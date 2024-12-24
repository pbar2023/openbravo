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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.WeldUtils;

/**
 * Defines a common API for Connection Handlers to trigger connection calls(established/closed) and
 * handle messages and errors
 */
public class MessageClientConnectionHandler {
  private static final Logger log = LogManager.getLogger();

  /**
   * Handles connection establishment by registering the message client
   * 
   * @param messageClient
   *          Message Client to be registered
   * @return true if connection was properly established, false otherwise. On non-proper connection,
   *         the connection should be closed by the caller.
   */
  static boolean connectionEstablished(MessageClient messageClient) {
    return MessageClientRegistry.getInstance().registerClient(messageClient);
  }

  /**
   * Handles connection being closed, in particular it un-registers the message client
   * 
   * @param searchKey
   *          SearchKey of the message client
   */
  static void connectionClosed(String searchKey) {
    MessageClientRegistry.getInstance().removeClient(searchKey);
  }

  /**
   * Handles a message sent by the client and received by this function. Allows to send a message
   * back by providing a String as a return value.
   *
   * The message is handled by a MessageHandler of the topic of the message through
   * "handleReceivedMessage" method. If there is no existing MessageHandler, there will be a warning
   * being logged.
   * 
   * @param message
   *          Message received from the client
   * @param messageClientSearchKey
   *          Search Key of the message client, to identify who sent the message
   * @return Optional message that will be sent to the user, return null to not send anything back
   */
  static String handleMessage(String message, String messageClientSearchKey) {
    log.debug("[Message Client] Message received from client {}. Message: {}",
        messageClientSearchKey, message);

    try {
      JSONObject jsonMessage = new JSONObject(message);
      String topic = jsonMessage.getString("topic");
      String messageToBeHandled = jsonMessage.getString("data");
      List<MessageHandler> messageHandlers = WeldUtils.getInstances(MessageHandler.class,
          new MessageHandler.Selector(topic));
      MessageClient messageClient = MessageClientRegistry.getInstance()
          .getBySearchKey(messageClientSearchKey);

      if (messageHandlers.isEmpty()) {
        log.error("[Message Client] No handler for message received with topic ({})", topic);
        return null;
      }

      if (messageClient == null) {
        log.error(
            "[Message Client] No message client is registered with search key ({}), can't handle message received.",
            messageClientSearchKey);
        return null;
      }

      Optional<String> messageToSendBack = messageHandlers.get(0)
          .handleReceivedMessage(messageToBeHandled, messageClient);

      if (messageToSendBack.isEmpty()) {
        return null;
      }

      JSONObject jsonMessageToSendBack = new JSONObject(
          Map.of("data", messageToSendBack.get(), "topic", topic));
      return jsonMessageToSendBack.toString();
    } catch (JSONException e) {
      throw new OBException("Could not handle non-json message.", e);
    }
  }

  /**
   * Handles an error received from the MessageClient
   * 
   * @param searchKey
   *          SearchKey of the MessageClient that originated the error
   * @param e
   *          Throwable, usually an exception representing the error
   */
  static void handleError(String searchKey, Throwable e) {
    log.error("Message Client with search key({}) - Received error: {}", searchKey, e);
    // Error always triggers a connection closed event, which is properly handled in the
    // connectionClosed method
  }
}
