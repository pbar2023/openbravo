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

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.PersistedMessage;

/**
 * Handles the persistence of MessageClientMsg objects in the database and their retrieval
 */
@ApplicationScoped
public class DatabaseMessageRegistryPersistence implements MessageRegistryPersistence {
  private static final Logger log = LogManager.getLogger();

  // Max expiration duration in days, a min of 1 and maximum of 7 days has been set
  private static final int MAX_EXPIRATION_DURATION = MessageClientUtils
      .getOBProperty("messageclient.max.expiration", 1, 1, 7);

  @Override
  public void persistMessage(MessageClientMsg messageClientMsg) {
    OBContext.setAdminMode();
    try {
      PersistedMessage persistedMessage = OBProvider.getInstance().get(PersistedMessage.class);
      persistedMessage.setNewOBObject(true);
      persistedMessage.setPayload(messageClientMsg.getPayload());
      persistedMessage.setType(messageClientMsg.getTopic());
      persistedMessage.setExpirationdate(getExpirationDate(messageClientMsg));
      persistedMessage.setContext(getContext(messageClientMsg));
      OBDal.getInstance().save(persistedMessage);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Override
  public List<MessageClientMsg> getPendingMessages() {
    List<PersistedMessage> persistedMsgs = getPendingPersistedMessages();
    return mapPersistedMessageToMessageClientMsg(persistedMsgs);
  }

  private String getContext(MessageClientMsg messageClientMsg) {
    JSONObject contextJson = new JSONObject(messageClientMsg.getContext());
    return contextJson.toString();
  }

  private List<PersistedMessage> getPendingPersistedMessages() {
    List<PersistedMessage> persistedMsgs = new ArrayList<>();
    try {
      OBContext.setAdminMode();
      OBCriteria<PersistedMessage> criteria = OBDal.getInstance()
          .createCriteria(PersistedMessage.class);
      criteria.setFilterOnReadableOrganization(false);
      criteria.setFilterOnReadableClients(false);
      criteria.setFilterOnActive(true);
      criteria.add(Restrictions.ge(PersistedMessage.PROPERTY_EXPIRATIONDATE, new Date()));
      criteria.addOrder(Order.asc(PersistedMessage.PROPERTY_CREATIONDATE));
      persistedMsgs = criteria.list();
    } finally {
      OBContext.restorePreviousMode();
    }
    return persistedMsgs;
  }

  private List<MessageClientMsg> mapPersistedMessageToMessageClientMsg(
      List<PersistedMessage> persistedMsgs) {
    return persistedMsgs.stream().map(msg -> {
      try {
        JSONObject contextJson = new JSONObject(msg.getContext());
        Map<String, String> mappedContext = jsonToStringMap(contextJson);
        return new MessageClientMsg(msg.getId(), msg.getType(), mappedContext, msg.getPayload(),
            msg.getExpirationdate(), msg.getCreationDate());
      } catch (JSONException e) {
        log.error(
            "Failed to extract context from PersistedMessage with ID ({}). Skipping the message, it must be fixed.",
            msg.getId(), e);
        return null;
      }
    }).filter(Objects::nonNull).collect(Collectors.toList());
  }

  private Map<String, String> jsonToStringMap(JSONObject json) throws JSONException {
    Map<String, String> result = new HashMap<>();
    Iterator<?> keys = json.keys();
    while (json.keys().hasNext()) {
      String key = (String) keys.next();
      String value = json.getString(key);
      result.put(key, value);
    }
    return result;
  }

  /**
   * Retrieves the expiration date, it is the minimum between the provided expiration date and the
   * hard minimum
   * 
   * @param messageClientMsg
   *          Message with or without expiration date
   */
  private Date getExpirationDate(MessageClientMsg messageClientMsg) {
    Date currentMaxExpirationDate = DateUtils.addDays(messageClientMsg.getCreationDate(),
        MAX_EXPIRATION_DURATION);
    if (messageClientMsg.getExpirationDate() == null) {
      return currentMaxExpirationDate;
    }

    if (messageClientMsg.getExpirationDate().after(currentMaxExpirationDate)) {
      return currentMaxExpirationDate;
    }

    return messageClientMsg.getExpirationDate();
  }
}
