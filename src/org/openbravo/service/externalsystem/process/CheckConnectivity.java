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
 * All portions are Copyright (C) 2022-2023 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.externalsystem.process;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.cache.Cacheable;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.application.process.ResponseActionsBuilder.MessageType;
import org.openbravo.dal.core.DalThreadCleaner;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.externalsystem.ExternalSystem;
import org.openbravo.service.externalsystem.ExternalSystem.Operation;
import org.openbravo.service.externalsystem.ExternalSystemProvider;
import org.openbravo.service.externalsystem.ExternalSystemResponse;
import org.openbravo.service.externalsystem.ExternalSystemResponse.Type;

/**
 * Process that checks the connectivity of an external system with a given configuration
 */
public class CheckConnectivity extends BaseProcessActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Inject
  private ExternalSystemProvider externalSystemProvider;

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    String id = null;
    try {
      JSONObject data = new JSONObject(content);
      id = data.getString("inpcExternalSystemId");
    } catch (JSONException ex) {
      log.error("Could not retrieve external system ID", ex);
      return buildError("C_ConnCheckProcessError");
    }

    JSONObject dataToSend = new JSONObject();
    return externalSystemProvider.getExternalSystem(id)
        .map(externalSystem -> externalSystem.send(Operation.CREATE,
            () -> new ByteArrayInputStream(dataToSend.toString().getBytes(StandardCharsets.UTF_8)),
            null,
            Map.of("event", "ConnectivityTest", "id", "1", "timestamp", System.currentTimeMillis()))
            .thenApply(response -> {
              try {
                close(externalSystem);
                return handleResponse(response);
              } finally {
                // close the DAL session used to get the messages of the response
                DalThreadCleaner.getInstance().cleanWithRollback();
              }
            })
            .exceptionally(t -> {
              close(externalSystem);
              throw new OBException("Connectity test failed", t);
            })
            .join())
        .orElse(buildError("C_ConnCheckMissingConfig"));
  }

  private void close(ExternalSystem externalSystem) {
    // cacheable external systems are closed when they are invalidated in the ExternalSystem
    // provider cache, so we need just to close the external system if it is not cacheable
    if (!(externalSystem instanceof Cacheable)) {
      try {
        externalSystem.close();
      } catch (Exception ex) {
        log.error("Error closing external system ", ex);
      }
    }
  }

  private JSONObject handleResponse(ExternalSystemResponse response) {
    if (Type.ERROR.equals(response.getType())) {
      String error = response.getError() != null ? response.getError().toString() : "";
      String statusCode = response.getStatusCode() + "";
      if ("0".equals(statusCode)) {
        return buildError("C_ConnCheckCouldNotConnect", error);
      }
      return buildError("C_ConnCheckFailed", statusCode, error);
    }
    return getResponseBuilder()
        .showMsgInProcessView(MessageType.SUCCESS, OBMessageUtils.getI18NMessage("OBUIAPP_Success"),
            OBMessageUtils.getI18NMessage("C_ConnCheckSuccess"))
        .build();
  }

  private JSONObject buildError(String message, String... messageParams) {
    return getResponseBuilder()
        .showMsgInProcessView(MessageType.ERROR, OBMessageUtils.getI18NMessage("OBUIAPP_Error"),
            OBMessageUtils.getI18NMessage(message, messageParams))
        .build();
  }
}
