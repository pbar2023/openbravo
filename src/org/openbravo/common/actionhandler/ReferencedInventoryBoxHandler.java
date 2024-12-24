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

package org.openbravo.common.actionhandler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.refinventory.BoxProcessor;
import org.openbravo.materialmgmt.refinventory.ContentRestriction;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryProcessor.GridJS;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;

/**
 * Action handler for boxing storage details (already contained into a Referenced Inventory or not)
 * into a Referenced Inventory
 *
 */
public class ReferencedInventoryBoxHandler extends ReferencedInventoryHandler {

  @Override
  protected void validateSelectionOrThrowException(final ReferencedInventoryHandlerData data)
      throws Exception {
    throwExceptionIfInnerRIsOrEmptyRIsAreSelected(data.getSelectedReferencedInventories());
    throwExceptionIfContentRestrictionsAreNotRespected(data);
  }

  public static void throwExceptionIfInnerRIsOrEmptyRIsAreSelected(
      final JSONArray selectedReferencedInventories) throws JSONException {
    final List<String> alreadyNestedRIsSelected = new ArrayList<>();
    final List<String> emptyRIsSelected = new ArrayList<>();
    for (int i = 0; selectedReferencedInventories != null
        && i < selectedReferencedInventories.length(); i++) {
      final String riId = selectedReferencedInventories.getJSONObject(i).getString(GridJS.ID);
      final ReferencedInventory ri = OBDal.getInstance().getProxy(ReferencedInventory.class, riId);
      if (ri.getParentRefInventory() != null) {
        alreadyNestedRIsSelected.add(ri.getIdentifier());
      }
      if (ri.getUniqueItemsCount() == 0) {
        emptyRIsSelected.add(ri.getIdentifier());
      }
    }
    if (!alreadyNestedRIsSelected.isEmpty()) {
      throw new OBException(String.format(OBMessageUtils.messageBD("BoxNestedRISelectionError"),
          alreadyNestedRIsSelected.stream().collect(Collectors.joining(", "))));
    }
    if (!emptyRIsSelected.isEmpty()) {
      throw new OBException(String.format(OBMessageUtils.messageBD("BoxEmptyRISelectionError"),
          emptyRIsSelected.stream().collect(Collectors.joining(", "))));
    }
  }

  private void throwExceptionIfContentRestrictionsAreNotRespected(
      final ReferencedInventoryHandlerData data) throws JSONException {
    throwExceptionIfContentRestrictionsAreNotRespected(data.getReferencedInventory(),
        data.getSelectedStorageDetails().length(),
        data.getSelectedReferencedInventories().length());
  }

  public static void throwExceptionIfContentRestrictionsAreNotRespected(
      final ReferencedInventory referencedInventory, int selectedStorageDetailsCount,
      int selectedReferencedInventoriesCount) {
    final ContentRestriction contentRestriction = ContentRestriction
        .getByValue(referencedInventory.getReferencedInventoryType().getContentRestriction());
    if (ContentRestriction.ONLY_ITEMS.value.equals(contentRestriction.value)
        && selectedReferencedInventoriesCount > 0) {
      throw new OBException("@RIBoxRIsContentRestrictionError@");
    }
    if (ContentRestriction.ONLY_REFINVENTORIES.value.equals(contentRestriction.value)
        && selectedStorageDetailsCount > 0) {
      throw new OBException("@RIBoxItemsContentRestrictionError@");
    }
  }

  @Override
  protected void run(final ReferencedInventoryHandlerData data) throws Exception {
    new BoxProcessor(data.getReferencedInventory(), data.getSelectedStorageDetails(),
        getNewStorageBin(data, null)).createAndProcessGoodsMovement();
  }

  /**
   * In Box, the new storage bin is a common parameter for all the storage details / referenced
   * inventories, so the selectedRefInventoryJS param is totally ignored by this method
   */
  @Override
  protected String getNewStorageBin(final ReferencedInventoryHandlerData data,
      final JSONObject selectedRefInventoryJS) throws JSONException {
    final String newStorageBinId = data.getNewStorageBinId();
    return StringUtils.isBlank(newStorageBinId) || StringUtils.equals(newStorageBinId, "null")
        ? null
        : newStorageBinId;
  }
}
