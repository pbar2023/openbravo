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

import java.util.Collection;
import java.util.HashSet;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryProcessor.GridJS;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryUtil;
import org.openbravo.materialmgmt.refinventory.UnboxProcessor;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;

/**
 * Action handler for unboxing storage details or referenced inventories from a Referenced Inventory
 *
 */
public class ReferencedInventoryUnBoxHandler extends ReferencedInventoryHandler {
  @Override
  protected void validateSelectionOrThrowException(final ReferencedInventoryHandlerData data)
      throws Exception {
    final JSONArray selectedRIsJS = data.getSelectedReferencedInventories();
    validateRIsAndStockAreNotSelectedAtTheSameTime(data.getSelectedStorageDetails(), selectedRIsJS);
    validateNotNestedRIsAreSelected(selectedRIsJS);
  }

  private void validateRIsAndStockAreNotSelectedAtTheSameTime(
      final JSONArray selectedStorageDetailsJS, final JSONArray selectedRIsJS) {
    if (selectedRIsJS.length() > 0 && selectedStorageDetailsJS.length() > 0) {
      throw new OBException("@UnboxRIsAndStockAtTheSameTimeError@");
    }
  }

  /**
   * Validates it is not possible to select at the same time a handling unit and any of its parents
   */
  public static void validateNotNestedRIsAreSelected(final JSONArray selectedRIsJS)
      throws JSONException {
    final Collection<String> selectedRIs = new HashSet<>();
    for (int i = 0; i < selectedRIsJS.length(); i++) {
      selectedRIs.add(selectedRIsJS.getJSONObject(i).getString(GridJS.ID));
    }

    for (final String selectedRIId : selectedRIs) {
      final ReferencedInventory selectedRI = OBDal.getInstance()
          .getProxy(ReferencedInventory.class, selectedRIId);

      ReferencedInventoryUtil.getParentReferencedInventories(selectedRI, false)
          .stream()
          .map(ReferencedInventory::getId)
          .filter(selectedRIs::contains)
          .findAny()
          .ifPresent(match -> {
            throw new OBException(
                OBMessageUtils.getI18NMessage("UnboxToRIParentTreeSelectionError", new String[] {
                    OBDal.getInstance().get(ReferencedInventory.class, match).getIdentifier() }));
          });
    }
  }

  @Override
  protected void run(final ReferencedInventoryHandlerData data) throws Exception {
    new UnboxProcessor(data.getReferencedInventory(), data.getSelectedStorageDetails(),
        data.getSelectedReferencedInventories(), data.getUnboxToIndividualItemsFlag())
            .createAndProcessGoodsMovement();
  }

  /**
   * In Unbox, each selected referenced inventory contains the new storage bin. The data parameter
   * is totally ignored
   */
  @Override
  protected String getNewStorageBin(final ReferencedInventoryHandlerData data,
      final JSONObject selectedRefInventoryJS) throws JSONException {
    return selectedRefInventoryJS.getString(GridJS.STORAGEBIN_ID);
  }
}
