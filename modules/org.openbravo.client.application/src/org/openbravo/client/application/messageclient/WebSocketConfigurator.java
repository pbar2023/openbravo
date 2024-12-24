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
package org.openbravo.client.application.messageclient;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import org.openbravo.base.exception.OBException;

/**
 * Configurator that handles proper authentication on WebSocket handshake request
 */
public class WebSocketConfigurator extends ServerEndpointConfig.Configurator {
  @Override
  public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request,
      HandshakeResponse response) {
    HttpSession session = (HttpSession) request.getHttpSession();
    if (!validateSession(session)) {
      throw new OBException("WebSocket authentication failed, not authenticated");
    }

    // Add sessionId to the userProperties
    sec.getUserProperties().put("sessionId", session.getId());
    sec.getUserProperties().put("user_id", session.getAttribute("#AD_USER_ID"));
    sec.getUserProperties().put("client_id", session.getAttribute("#AD_CLIENT_ID"));
    sec.getUserProperties().put("org_id", session.getAttribute("#AD_ORG_ID"));
    sec.getUserProperties().put("role_id", session.getAttribute("#AD_ROLE_ID"));
  }

  private boolean validateSession(HttpSession session) {
    return session != null && session.getAttribute("#Authenticated_user") != null;
  }
}
