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

import javax.enterprise.util.AnnotationLiteral;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Handles to whom a message should be sent, it is expected to be extended from by each
 * MessageHandler by type
 */
public abstract class MessageHandler {
  /**
   * Returns the list of recipients filtered first by client, then by subscribed topic, timestamp
   * from last message sent and finally by the isValidReceipt function, which is to be implemented
   * by each MessageHandler
   * 
   * @param messageClientMsg
   *          Message to be sent and retrieve the expected recipients for that message
   * @return List of MessageClient representing each recipient
   */
  protected final List<MessageClient> getRecipients(MessageClientMsg messageClientMsg) {
    String clientId = messageClientMsg.getContext().get("client");
    List<MessageClient> connectedClients = MessageClientRegistry.getInstance()
        .getRegisteredClientsOfClientId(clientId);

    // Filters those connectedClients who already received the message
    List<MessageClient> relevantClients = connectedClients.stream().filter(messageClient -> {
      if (!messageClient.getSubscribedTopics().contains(messageClientMsg.getTopic())) {
        // Filter non-subscribed-for topics
        return false;
      }
      if (messageClient.getTimestampLastMsgSent() == null) {
        return true;
      }
      return messageClient.getTimestampLastMsgSent().before(messageClientMsg.getCreationDate());
    }).collect(Collectors.toList());

    return getRecipients(messageClientMsg, relevantClients);
  }

  /**
   * Defines if a message client is allowed to subscribe to the current MessageHandler topic, each
   * message handler must implement this method and define rules to identify when a message client
   * can subscribe to its specific topic, by ideally using the message client context.
   * 
   * @param messageClient
   *          Message client that wants to subscribe to the topic
   * @return true if it can subscribe, false otherwise
   */
  protected abstract boolean isAllowedToSubscribeToTopic(MessageClient messageClient);

  /**
   * Must return the recipients of the messageClientMsg by using the provided context in that same
   * object. It should only check the provided connected message clients, as those are the relevant
   * ones
   * 
   * @param messageClientMsg
   *          Message that also contains the context
   * @param connectedClients
   *          Relevant connected clients, function should filter those
   * @return List of MessageClient that should receive the message
   */
  protected final List<MessageClient> getRecipients(MessageClientMsg messageClientMsg,
      List<MessageClient> connectedClients) {
    return connectedClients.stream()
        .filter(messageClient -> this.isValidRecipient(messageClientMsg, messageClient))
        .collect(Collectors.toList());
  }

  /**
   * Must return if the recipient is valid for the messageClientMsg by using the provided context in
   * that same object.
   *
   * @param messageClientMsg
   *          Message that also contains the context
   * @param messageClient
   *          Message client to be validated
   * @return true if the message client should receive the message, false otherwise
   */
  protected abstract boolean isValidRecipient(MessageClientMsg messageClientMsg,
      MessageClient messageClient);

  /**
   * Handles a message received from the connected message client with the annotated topic
   * 
   * @param message
   *          Message received from the client
   * @param messageClient
   *          Connected message client that sent the message
   * @return An optional string that will be sent to the client
   */
  protected abstract Optional<String> handleReceivedMessage(String message,
      MessageClient messageClient);

  /**
   * Defines the qualifier used to register a message handler type.
   */
  @javax.inject.Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
  public @interface Qualifier {
    String value();
  }

  /**
   * A class used to select the correct message handler type.
   */
  @SuppressWarnings("all")
  public static class Selector extends AnnotationLiteral<MessageHandler.Qualifier>
      implements MessageHandler.Qualifier {
    private static final long serialVersionUID = 1L;

    final String value;

    public Selector(String value) {
      this.value = value;
    }

    @Override
    public String value() {
      return value;
    }
  }
}
