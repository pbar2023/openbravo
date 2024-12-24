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

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.weld.WeldUtils;

/**
 * Common registry for Messages that should be sent through the MessageClient infrastructure
 */
public class MessageRegistry {
  private static final Logger log = LogManager.getLogger();

  static MessageRegistry instance;

  @Inject
  MessageRegistryPersistence messageRegistryPersistence;

  @Inject
  @Any
  Instance<MessageHandler> messageHandlers;

  MessageRegistry() {
  }

  public static MessageRegistry getInstance() {
    if (instance == null) {
      instance = WeldUtils.getInstanceFromStaticBeanManager(MessageRegistry.class);
    }

    return instance;
  }

  /**
   * Registers a given message to then be sent to the corresponding MessageClients
   * 
   * @param message
   *          Message to be sent
   */
  public void sendMessage(MessageClientMsg message) {
    messageRegistryPersistence.persistMessage(message);
  }

  /**
   * Returns a list of messages that are pending to be sent, it must exclude expired messages.
   * 
   * @return non-expired messages
   */
  public List<MessageClientMsg> getPendingMessages() {
    List<MessageClientMsg> messages = messageRegistryPersistence.getPendingMessages();

    // Filter those messages that have no message handler implementation
    return messages.stream()
        .filter(this::checkMessageHandlerExistsForType)
        .collect(Collectors.toList());
  }

  private boolean checkMessageHandlerExistsForType(MessageClientMsg messageClientMsg) {
    Instance<MessageHandler> messageHandler = messageHandlers
        .select(new MessageHandler.Selector(messageClientMsg.getTopic()));

    if (messageHandler.isUnsatisfied()) {
      log.warn("No available message handler for type:" + messageClientMsg.getTopic());
      return false;
    }

    if (messageHandler.isAmbiguous()) {
      log.warn(
          "There are several message handlers for type {}. This is not supported, only the first one will be used, the rest are ignored. Review and remove them.",
          messageClientMsg.getTopic());
    }

    return true;
  }
}
