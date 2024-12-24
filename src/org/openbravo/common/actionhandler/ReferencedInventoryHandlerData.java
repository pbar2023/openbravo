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
 * All portions are Copyright (C) 2024 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.common.actionhandler;

import java.util.Collection;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryProcessor.StorageDetailJS;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;

/**
 * Stores the data selected by the user for boxing/unboxing referenced inventories
 */
class ReferencedInventoryHandlerData {
  private static final String PARAMS = "_params";
  private static final String PARAM_GRID_STOCK = "stock";
  private static final String PARAM_GRID_SELECTION = "_selection";
  private static final String PARAM_GRID_REFINVENTORY = "referencedInventory";
  private static final String PARAM_UNBOXTOINDIVIDUALITEMS = "unboxToIndividualItems";
  private static final String PARAM_NEWSTORAGEBIN = "M_LocatorTo_ID";

  private JSONObject requestJson;
  private JSONObject paramsJson;
  private JSONArray storageDetailsToProcess;

  ReferencedInventoryHandlerData(final String content) throws JSONException {
    this.requestJson = new JSONObject(content);
    this.paramsJson = requestJson.getJSONObject(PARAMS);
    this.storageDetailsToProcess = paramsJson.getJSONObject(PARAM_GRID_STOCK)
        .getJSONArray(PARAM_GRID_SELECTION);
  }

  JSONArray getSelectedReferencedInventories() throws JSONException {
    return paramsJson.getJSONObject(PARAM_GRID_REFINVENTORY).getJSONArray(PARAM_GRID_SELECTION);
  }

  ReferencedInventory getReferencedInventory() throws JSONException {
    return OBDal.getInstance()
        .getProxy(ReferencedInventory.class, requestJson.getString("inpmRefinventoryId"));
  }

  JSONArray getSelectedStorageDetails() {
    return storageDetailsToProcess;
  }

  boolean getUnboxToIndividualItemsFlag() {
    try {
      return paramsJson.getBoolean(PARAM_UNBOXTOINDIVIDUALITEMS);
    } catch (JSONException noParameterFound) {
      return true;
    }
  }

  String getNewStorageBinId() {
    try {
      return paramsJson.getString(PARAM_NEWSTORAGEBIN);
    } catch (JSONException e) {
      return null;
    }
  }

  /**
   * Adds the nested storage details within the selected referenced inventories into the storage
   * details to process. The selected storage details will be later on boxed or unboxed
   * 
   */
  void appendNestedStorageDetailsFromSelectedRefInventories(
      final Collection<StorageDetailJS> generatedStorageDetailsJS) throws JSONException {
    generatedStorageDetailsJS.stream()
        .forEach(sd -> storageDetailsToProcess.put(sd.toJSONObject()));
  }

}
