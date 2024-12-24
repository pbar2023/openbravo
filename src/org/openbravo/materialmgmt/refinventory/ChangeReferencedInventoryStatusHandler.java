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
package org.openbravo.materialmgmt.refinventory;

import java.util.Map;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.application.process.ResponseActionsBuilder;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryStatusProcessor.ReferencedInventoryStatus;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;

/**
 * A process that changes the status of a handling unit
 */
public class ChangeReferencedInventoryStatusHandler extends BaseProcessActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Inject
  private ReferencedInventoryStatusProcessor statusProcessor;

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    try {
      JSONObject request = new JSONObject(content);
      ReferencedInventory handlingUnit = OBDal.getInstance()
          .get(ReferencedInventory.class, request.getString("M_RefInventory_ID"));
      ReferencedInventoryStatus status = ReferencedInventoryStatus
          .valueOf(request.getJSONObject("_params").getString("Status"));

      statusProcessor.changeStatus(handlingUnit, status);

      return getResponseBuilder()
          .showMsgInProcessView(ResponseActionsBuilder.MessageType.SUCCESS,
              OBMessageUtils.messageBD("HandlingUnitStatusChanged"))
          .build();
    } catch (JSONException ex) {
      log.error("Error changing handling unit status", ex);
      return getResponseBuilder()
          .showMsgInProcessView(ResponseActionsBuilder.MessageType.ERROR,
              OBMessageUtils.messageBD("OBUIAPP_Error"))
          .build();
    }
  }
}
