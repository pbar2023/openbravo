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

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.server.ServerEndpoint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * WebSocket specific connection handler, it triggers the corresponding calls in
 * MessageClientConnectionHandler class.
 */
@ServerEndpoint(value = "/websocket", configurator = WebSocketConfigurator.class)
public class WebSocketConnectionHandler {
  private static final Logger log = LogManager.getLogger();

  /**
   * Handles websocket connection creation, it is executed after the connection has already been
   * authenticated
   * 
   * @param session
   *          WebSocket session which should include as part of user properties the context and
   *          sessionId
   */
  @OnOpen
  public void onOpen(javax.websocket.Session session) throws IOException {
    String sessionId = (String) session.getUserProperties().get("sessionId");
    log.debug("Websocket - Connection accepted. Session: " + sessionId);
    String userId = (String) session.getUserProperties().get("user_id");
    String roleId = (String) session.getUserProperties().get("role_id");
    String orgId = (String) session.getUserProperties().get("org_id");
    String clientId = (String) session.getUserProperties().get("client_id");

    List<String> supportedMessageTypes = session.getRequestParameterMap().get("supportedTopics");
    List<String> lastMessageTimestampParams = session.getRequestParameterMap()
        .get("lastMessageTimestamp");

    WebSocketClient webSocketClient = new WebSocketClient(sessionId, clientId, orgId, userId,
        roleId, session);
    webSocketClient.setSubscribedTopics(supportedMessageTypes);

    if (lastMessageTimestampParams != null && !lastMessageTimestampParams.isEmpty()) {
      long lastMessageTimestamp = Long
          .parseLong(session.getRequestParameterMap().get("lastMessageTimestamp").get(0));
      webSocketClient.setTimestampLastMsgSent(new Date(lastMessageTimestamp));
    }

    boolean registrationSuccessful = MessageClientConnectionHandler
        .connectionEstablished(webSocketClient);
    if (!registrationSuccessful) {
      session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT,
          "Cannot establish connection, registration failed. Check subscribed topics."));
    }
  }

  /**
   * Handles connection being closed, this is executed both on connection being closed intentionally
   * and also on an error
   * 
   * @param session
   *          WebSocket session, should include the sessionId as part of its user properties
   */
  @OnClose
  public void onClose(javax.websocket.Session session) {
    String sessionId = (String) session.getUserProperties().get("sessionId");
    log.debug("Websocket - Connection terminated. Session: " + sessionId);

    MessageClientConnectionHandler.connectionClosed(sessionId);
  }

  /**
   * Handles message being received from a WebSocket session, in some cases we might want to send a
   * message in response to the received one
   * 
   * @param message
   *          Message received from the WebSocket client
   * @param session
   *          WebSocket session, includes sessionId and other context info as part of its user
   *          properties
   */
  @OnMessage
  public void onMessage(String message, javax.websocket.Session session) {
    String sessionId = (String) session.getUserProperties().get("sessionId");
    String messageToSendBack = MessageClientConnectionHandler.handleMessage(message, sessionId);
    if (messageToSendBack != null) {
      session.getAsyncRemote().sendText(messageToSendBack);
    }
  }

  /**
   * Handles errors on the WebSocket session connection, it is executed when an error happens on the
   * connection, message sending process or the message handling infrastructure on the frontend
   * 
   * @param session
   *          WebSocket session, include the sessionId and other context info as part of its user
   *          properties
   * @param t
   *          Throwable, usually an exception that explains why the error has happened
   */
  @OnError
  public void onError(javax.websocket.Session session, Throwable t) {
    String sessionId = (String) session.getUserProperties().getOrDefault("sessionId", null);
    MessageClientConnectionHandler.handleError(sessionId, t);
  }
}
