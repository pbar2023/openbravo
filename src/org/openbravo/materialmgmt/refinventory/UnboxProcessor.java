/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2017-2024 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.materialmgmt.refinventory;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.util.Check;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;

/**
 * Process of unboxing storage details or referenced inventories out of a referenced inventory
 */
public class UnboxProcessor extends ReferencedInventoryProcessor {

  private boolean unboxToIndividualItems;

  // Every RIs selected and, if unboxToIndividualItems, any RI in the selected storage details
  private Set<String> affectedRefInventoryIds = new HashSet<>();

  /**
   * Calculates all the affected referenced inventories by the user selection.
   * 
   * In Unbox To Individual Items all the parent RIs of the selected storage details might be
   * potentially affected by the unbox process.
   * 
   * In Unbox to HU then only the selected RIs are affected or, exceptionally and alternatively, any
   * immediate nested RIs if outermost RI is selected
   */
  public static Collection<String> calculateAffectedRefInventoryIds(
      final JSONArray selectedStorageDetails, final boolean unboxToIndividualItems,
      JSONArray selectedRIs) throws JSONException {
    final Set<String> affectedRefInventoryIds = new HashSet<>();
    // Selected RI or, exceptionally and alternatively, immediate nested RIs if outermost RI
    for (int i = 0; selectedRIs != null && i < selectedRIs.length(); i++) {
      final String riId = selectedRIs.getJSONObject(i).getString(GridJS.ID);
      final ReferencedInventory ri = OBDal.getInstance().getProxy(ReferencedInventory.class, riId);
      if (ri.getParentRefInventory() == null) {
        // Add immediate inner RIs if RI is the outermost.
        // This is for the special case of selecting the outermost HU, in order to unbox the
        // immediate inner HUs outside of it. Without this hack the user would be forced to select
        // the n immediate inner HUs to do the same stuff.
        affectedRefInventoryIds.addAll(getImmediateNestedRefInventories(riId));
      } else {
        // Selected RI
        affectedRefInventoryIds.add(riId);
      }
    }

    // Add all parent RIs (including the innermost) of the selected storage details
    if (unboxToIndividualItems) {
      for (int i = 0; i < selectedStorageDetails.length(); i++) {
        final JSONObject storageDetailJS = selectedStorageDetails.getJSONObject(i);
        final StorageDetail storageDetail = getStorageDetail(storageDetailJS);
        final ReferencedInventory storageDetailRefInventory = storageDetail
            .getReferencedInventory();
        if (storageDetailRefInventory != null) {
          affectedRefInventoryIds.addAll(ReferencedInventoryUtil
              .getParentReferencedInventories(storageDetailRefInventory, true)
              .stream()
              .map(ReferencedInventory::getId)
              .collect(Collectors.toList()));
        }
      }
    }

    return affectedRefInventoryIds;
  }

  private static Collection<String> getImmediateNestedRefInventories(final String refInventoryId) {
    //@formatter:off
    final String hql = "select ri.id "
                     + "from MaterialMgmtReferencedInventory ri "
                     + "where ri.parentRefInventory.id = :thisRefInventoryId ";
    //@formatter:on
    final Query<String> query = OBDal.getInstance().getSession().createQuery(hql, String.class);
    query.setParameter("thisRefInventoryId", refInventoryId);
    return query.list();
  }

  /**
   * Returns the first parent in the RI tree that has been selected by the user.
   * 
   * If unboxToIndividualItems, returns null (so the stock will be extracted outside any RI)
   */
  public static final ReferencedInventory getSelectedReferencedInventory(
      final StorageDetail storageDetail, boolean unboxToIndividualItems,
      final Collection<String> affectedRefInventoryIds) {
    if (unboxToIndividualItems) {
      return null; // Extract the stock outside any RI
    }

    // Get the first parent in the RI tree that has been selected by the user
    ReferencedInventory outerMostRI = storageDetail.getReferencedInventory();
    while (outerMostRI.getParentRefInventory() != null
        && !affectedRefInventoryIds.contains(outerMostRI.getId())) {
      outerMostRI = outerMostRI.getParentRefInventory();
    }
    return outerMostRI;
  }

  /**
   * Updates the parent referenced inventory of the affected referenced inventories.
   * 
   * Warning: This method internally executes an update query on the database the Referenced
   * Inventory objects. It is crucial to avoid any subsequent updates to these objects using
   * Hibernate, as doing so may overwrite these changes made directly to the database.
   */
  public static int clearParentReferenceInventory(boolean isUnboxToIndividualItems,
      Collection<String> affectedRefInventoryIdCollection) {
    return clearParentRefInventoryIfEmpty(isUnboxToIndividualItems,
        affectedRefInventoryIdCollection);
  }

  /**
   * For the affected RIs, clear the parent reference inventory, i.e. move the reference inventory
   * outside of the outermost reference inventory.
   * 
   * If unboxToIndividualItems do it only for referenced inventories that are empty after the
   * unboxing.
   */
  private static int clearParentRefInventoryIfEmpty(boolean isUnboxToIndividualItems,
      Collection<String> affectedRefInventoryIdCollection) {
    //@formatter:off
    final String hql = "update MaterialMgmtReferencedInventory ri "
                     + "set ri.parentRefInventory.id = null "
                     + "where ri.parentRefInventory.id is not null "
                     // Affected referenced inventories..
                     + "and ri.id in (:affectedRefInventoryIds) "
                     // ...that have no stock remaining in unboxToIndividualItems
                     + (isUnboxToIndividualItems 
                         ? " and not exists (select 1 "
                            + "             from MaterialMgmtStorageDetail sd "
                            + "             where sd.referencedInventory.id = ri.id) "
                         : "");
    //@formatter:on
    return OBDal.getInstance()
        .getSession()
        .createQuery(hql)
        .setParameterList("affectedRefInventoryIds", affectedRefInventoryIdCollection)
        .executeUpdate();
  }

  @Deprecated
  public UnboxProcessor(final ReferencedInventory referencedInventory,
      final JSONArray selectedStorageDetails) throws JSONException {
    this(referencedInventory, selectedStorageDetails, null, true);
  }

  public UnboxProcessor(final ReferencedInventory referencedInventory,
      final JSONArray selectedStorageDetails, final JSONArray selectedRefInventories,
      boolean unboxToIndividualItems) throws JSONException {
    super(referencedInventory, selectedStorageDetails);
    this.unboxToIndividualItems = unboxToIndividualItems;
    checkStorageDetailsHaveReferencedInventory(selectedStorageDetails);
    setAffectedRefInventoryIds(selectedStorageDetails, selectedRefInventories);
  }

  private void checkStorageDetailsHaveReferencedInventory(final JSONArray selectedStorageDetails)
      throws JSONException {
    for (int i = 0; i < selectedStorageDetails.length(); i++) {
      final JSONObject storageDetailJS = selectedStorageDetails.getJSONObject(i);
      final StorageDetail storageDetail = getStorageDetail(storageDetailJS);
      Check.isNotNull(storageDetail.getReferencedInventory(),
          String.format(OBMessageUtils.messageBD("StorageDetailNotLinkedToReferencedInventory"),
              storageDetail.getIdentifier()));
    }
  }

  private void setAffectedRefInventoryIds(final JSONArray selectedStorageDetails,
      JSONArray selectedRIs) throws JSONException {
    this.affectedRefInventoryIds = (Set<String>) calculateAffectedRefInventoryIds(
        selectedStorageDetails, unboxToIndividualItems, selectedRIs);
  }

  @Override
  protected AttributeSetInstance getAttributeSetInstanceTo(StorageDetail storageDetail) {
    return ReferencedInventoryUtil.getAttributeSetInstanceTo(storageDetail,
        getSelectedReferencedInventory(storageDetail));
  }

  private ReferencedInventory getSelectedReferencedInventory(final StorageDetail storageDetail) {
    return getSelectedReferencedInventory(storageDetail, unboxToIndividualItems,
        affectedRefInventoryIds);
  }

  @Override
  protected String generateInternalMovementName() {
    return OBDateUtils.formatDateTime(new Date()) + "_" + OBMessageUtils.messageBD("UNBOX");
  }

  @Override
  protected String getNewStorageBinId(JSONObject storageDetailJS) {
    try {
      return storageDetailJS.getString(GridJS.STORAGEBIN_ID);
    } catch (JSONException e) {
      throw new OBException("Error getting new storage bin for storage detail: " + storageDetailJS,
          e);
    }
  }

  @Override
  protected int updateParentReferenceInventory() {
    return clearParentRefInventoryIfEmpty(unboxToIndividualItems, affectedRefInventoryIds);
  }

}
