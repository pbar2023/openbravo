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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.provider.OBSingleton;
import org.openbravo.base.weld.WeldUtils;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

/**
 * Provides a common Registry for MessageClients
 */
public class MessageClientRegistry implements OBSingleton {
  private static final Logger logger = LogManager.getLogger();
  Map<String, MessageClient> messageClientsBySessionId;
  Map<String, MessageClient> messageClientsByUserId;

  @Inject
  @Any
  Instance<MessageHandler> messageHandlers;

  private static MessageClientRegistry instance;

  public static MessageClientRegistry getInstance() {
    if (instance == null) {
      instance = WeldUtils.getInstanceFromStaticBeanManager(MessageClientRegistry.class);
      instance.messageClientsBySessionId = new HashMap<>();
      instance.messageClientsByUserId = new HashMap<>();
    }
    return instance;
  }

  /**
   * Registers a connected message client in the MessageClientRegistry
   *
   * @param messageClient
   *          - Message client to be registered
   * @return true if the registration was done, false if it failed
   */
  public boolean registerClient(MessageClient messageClient) {
    String messageClientSearchKey = messageClient.getSearchKey();
    if (messageClientsBySessionId.containsKey(messageClientSearchKey)) {
      logger.warn(
          "A message client already registered for searchKey: {}, overwriting it with the new one.",
          messageClientSearchKey);
    }

    boolean isAllowedToSubscribe = areAllSubscribedTopicsAllowed(messageClient);
    if (!isAllowedToSubscribe) {
      // Some topic is not allowed to be subscribed to, aborting message client registration
      logger.warn(
          "Message Client will not be registered, as some subscribed topics can't be handled.");
      return false;
    }
    messageClientsBySessionId.put(messageClientSearchKey, messageClient);
    messageClientsByUserId.put(messageClient.getUserId(), messageClient);
    return true;
  }

  /**
   * Removes a message client from the Registry
   *
   * @param searchKey
   *          - searchKey of the message client to be removed
   */
  public void removeClient(String searchKey) {
    if (!messageClientsBySessionId.containsKey(searchKey)) {
      logger.warn("Trying to remove a non registered message client: {}. Ignoring.", searchKey);
      return;
    }

    MessageClient messageClientRemoved = messageClientsBySessionId.get(searchKey);
    messageClientsByUserId.remove(messageClientRemoved.getUserId());
    messageClientsBySessionId.remove(searchKey);
  }

  /**
   * Internal use, returns all the connected MessageClients.
   *
   * @return all the connected MessageClients.
   */
  protected List<MessageClient> getAllClients() {
    return new ArrayList<>(messageClientsBySessionId.values());
  }

  /**
   * Returns the connected MessageClients of a given client id.
   *
   * @param clientId
   *          Client ID to filter message clients by
   * @return all the connected MessageClients that are of that client
   */
  protected List<MessageClient> getRegisteredClientsOfClientId(String clientId) {
    if (clientId == null || "0".equals(clientId)) {
      // ClientID 0 indicates that all clients should be returned
      return getAllClients();
    }

    return messageClientsBySessionId.values()
        .stream()
        .filter(messageClient -> clientId.equals(messageClient.getClientId()))
        .collect(Collectors.toList());
  }

  /**
   * Returns a Message Client that was registered with the given Search Key
   * 
   * @param searchKey
   *          Search Key of the message client to search
   * @return Message Client corresponding to that Search Key
   */
  public MessageClient getBySearchKey(String searchKey) {
    return messageClientsBySessionId.get(searchKey);
  }

  private boolean areAllSubscribedTopicsAllowed(MessageClient messageClient) {
    List<String> subscribedTopics = messageClient.getSubscribedTopics();
    if (subscribedTopics.isEmpty()) {
      return true;
    }
    return subscribedTopics.stream().allMatch(topic -> isSubscriptionAllowed(messageClient, topic));
  }

  private boolean isSubscriptionAllowed(MessageClient messageClient, String topic) {
    Instance<MessageHandler> messageHandler = messageHandlers
        .select(new MessageHandler.Selector(topic));
    if (messageHandler.isUnsatisfied()) {
      logger
          .warn("No available message handler for subscribed topic, connection aborted: " + topic);
      return false;
    }
    return messageHandler.get().isAllowedToSubscribeToTopic(messageClient);
  }
}
