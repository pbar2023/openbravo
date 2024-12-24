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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.session.OBPropertiesProvider;

/**
 * Contains common utility methods used by several MessageClient classes
 */
public class MessageClientUtils {
  private static final Logger log = LogManager.getLogger();

  /**
   * Returns an integer property or its min/default value if not set
   *
   * @param property
   *          Property to be retrieved
   * @param defaultValue
   *          Default value if not set
   * @param minValue
   *          Minimum value, to avoid user setting it too low
   * @return value of the property
   */
  public static int getOBProperty(String property, int defaultValue, int minValue) {
    int value = getOpenbravoProperty(property, defaultValue);
    if (value < minValue) {
      log.warn("Value of property " + property + " is set too low (" + value
          + "), using valid minValue instead " + minValue);
      return minValue;
    }
    return value;
  }

  /**
   * Returns an integer property or its min/max/default value if not set
   *
   * @param property
   *          Property to be retrieved
   * @param defaultValue
   *          Default value if not set
   * @param minValue
   *          Minimum value, to avoid user setting it too low
   * @param maxValue
   *          Maximum value, to avoid user setting a value too high
   * @return value of the property
   */
  public static int getOBProperty(String property, int defaultValue, int minValue, int maxValue) {
    int value = getOpenbravoProperty(property, defaultValue);
    if (value < minValue) {
      log.warn("Value of property " + property + " is set too low (" + value
          + "), using valid minValue instead " + minValue);
      return minValue;
    }
    if (value > maxValue) {
      log.warn("Value of property " + property + " is set too high (" + value
          + "), using valid maxValue instead " + minValue);
      return maxValue;
    }
    return value;
  }

  /**
   * Retrieves an integer property from OBProperties Provider, if it is not properly set, it will
   * fallback to a provided default value
   *
   * @param propName
   *          Property name to be retrieved
   * @param defaultValue
   *          Default value in case it is not properly set
   * @return value of the property
   */
  private static int getOpenbravoProperty(String propName, int defaultValue) {
    final String val = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty(propName);
    if (val == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(val);
    } catch (NumberFormatException ignore) {
      return defaultValue;
    }
  }
}
