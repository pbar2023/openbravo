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

/**
 * Main message client class, which keeps track of the different properties of the Message Client
 * and also allows sending messages, which must be implemented by specific implementations (for
 * example, websockets)
 */
public abstract class MessageClient {
  String searchKey;
  String clientId;
  String organizationId;
  String userId;
  String roleId;
  Date timestampLastMsgSent;
  List<String> subscribedTopics;

  public MessageClient(String searchKey, String clientId, String organizationId, String userId,
      String roleId) {
    this.searchKey = searchKey;
    this.clientId = clientId;
    this.organizationId = organizationId;
    this.userId = userId;
    this.roleId = roleId;
  }

  /**
   * Sends a given message to the MessageClient, should update the timestampLastMsgSent value
   * 
   * @param message
   *          Message to be sent
   * @param timestamp
   *          Date timestamp of message creation for further filtering
   */
  public abstract void sendMessage(String message, Date timestamp) throws IOException;

  /**
   * Sets the subscribed topics to the provided topics
   * 
   * @param topics
   *          List of topics, each must be a String
   */
  public void setSubscribedTopics(List<String> topics) {
    this.subscribedTopics = topics;
  }

  /**
   * Returns the MessageClient searchKey, it is useful to differentiate one MessageClient from
   * another
   * 
   * @return MessageClient searchKey
   */
  public String getSearchKey() {
    return searchKey;
  }

  /**
   * Returns the MessageClient client ID, this is a Client ID as in Openbravo
   * 
   * @return clientId of MessageClient
   */
  public String getClientId() {
    return clientId;
  }

  /**
   * Returns the MessageClient organization ID, this is an Organization ID as in Openbravo
   * 
   * @return organizationId of MessageClient
   */
  public String getOrganizationId() {
    return organizationId;
  }

  /**
   * Returns the MessageClient user ID, this is a User ID as in Openbravo
   * 
   * @return userId of MessageClient
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Returns the MessageClient role ID, this is a Role ID as in Openbravo
   * 
   * @return roleId of MessageClient
   */
  public String getRoleId() {
    return roleId;
  }

  /**
   * Returns the last timestamp message sent, which indicates the last message that was sent to the
   * client
   * 
   * @return timestamp of the last message successfully sent
   */
  public Date getTimestampLastMsgSent() {
    return timestampLastMsgSent;
  }

  /**
   * Sets the last timestamp message sent, which indicates the last message that was sent to the
   * client
   * 
   * @param timestampLastMsgSent
   *          timestamp new value to set it to
   */
  public void setTimestampLastMsgSent(Date timestampLastMsgSent) {
    this.timestampLastMsgSent = timestampLastMsgSent;
  }

  /**
   * Returns the topics that the MessageClient is subscribed to
   * 
   * @return List of subscribed topics
   */
  public List<String> getSubscribedTopics() {
    return subscribedTopics;
  }
}
