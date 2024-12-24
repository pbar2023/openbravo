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
 * All portions are Copyright (C) 2023-2024 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.authentication;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SequenceIdData;

/**
 * Keeps in session the available login provider configurations linked to a random UUID. In the case
 * of OAuth 2.0, this UUID is used for preventing cross-site request forgery attacks by comparing
 * that UUID with the value received in the state parameter of the authorization requests.
 */
@ApplicationScoped
public class LoginStateHandler {
  public static final String LOGIN_STATE = "#LOGIN_STATE";

  /**
   * Saves the given configuration ID in the session, linked to a random generated UUID but only if
   * there is no value linked to the given configuration yet.
   *
   * @param configId
   *          The ID of the record where the configuration is kept
   *
   * @return the generated UUID
   */
  public String addNewConfiguration(String configId) {
    return addNewConfiguration(configId, SequenceIdData.getUUID());
  }

  /**
   * Saves the given configuration ID in the session, linked to given random value but only if there
   * is no value linked to the given configuration yet.
   *
   * @param configId
   *          The ID of the record where the configuration is kept
   * @param random
   *          A random value
   *
   * @return the saved random value
   */
  public String addNewConfiguration(String configId, String random) {
    Map<String, String> loginState = getLoginState();
    loginState.putIfAbsent(configId, random);
    return loginState.get(configId);
  }

  /**
   * Clears the value linked to the given configuration ID from the session
   *
   * @param configId
   *          The ID of the record where the configuration is kept
   */
  public void clearConfiguration(String configId) {
    getLoginState().remove(configId);
  }

  /**
   * Validates if the given key is valid in the current session.
   *
   * @param key
   *          The key to be validated
   *
   * @return true if the key is valid or false in any other case.
   */
  public boolean isValidKey(String key) {
    return getLoginState().containsValue(key);
  }

  /**
   * Gets the random value stored for a given configuration
   *
   * @param configId
   *          The configuration ID
   *
   * @return the value stored for the given configuration
   */
  public String getValue(String configId) {
    return getLoginState().get(configId);
  }

  /**
   * Gets the configuration linked to the given key.
   *
   * @param clz
   *          The BaseOBObject class of the record where the configuration is kept
   * @param key
   *          The key linked to the configuration
   *
   * @return true if the key is valid or false in any other case.
   */
  public <T extends BaseOBObject> T getConfiguration(Class<T> clz, String key) {
    return getLoginState().entrySet()
        .stream()
        .filter(e -> e.getValue().equals(key))
        .findFirst()
        .map(e -> OBDal.getInstance().get(clz, e.getKey()))
        .orElseThrow();
  }

  private Map<String, String> getLoginState() {
    @SuppressWarnings("unchecked")
    Map<String, String> loginState = (Map<String, String>) RequestContext.get()
        .getSessionAttribute(LOGIN_STATE);
    if (loginState == null) {
      loginState = new ConcurrentHashMap<>();
      RequestContext.get().setSessionAttribute(LOGIN_STATE, loginState);
    }
    return loginState;
  }
}
