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

import java.util.Date;
import java.util.Map;

import org.openbravo.erpCommon.utility.SequenceIdData;

/**
 * Structure that messages for message client infrastructure use. It is used to send, receive and
 * register messages.
 */
public class MessageClientMsg {
  private final String id;
  private final String topic;
  private final Map<String, String> context;
  private final String payload;
  private final Date expirationDate;
  private final Date creationDate;

  public MessageClientMsg(String topic, Map<String, String> context, String payload,
      Date expirationTime, Date creationDate) {
    this(SequenceIdData.getUUID(), topic, context, payload, expirationTime, creationDate);
  }

  public MessageClientMsg(String id, String topic, Map<String, String> context, String payload,
      Date expirationTime, Date creationDate) {
    this.id = id;
    this.topic = topic;
    this.context = context;
    this.payload = payload;
    this.expirationDate = expirationTime;
    this.creationDate = creationDate;
  }

  public String getId() {
    return id;
  }

  public String getTopic() {
    return topic;
  }

  public Map<String, String> getContext() {
    return context;
  }

  public String getPayload() {
    return payload;
  }

  public Date getExpirationDate() {
    return expirationDate;
  }

  public Date getCreationDate() {
    return creationDate;
  }
}
