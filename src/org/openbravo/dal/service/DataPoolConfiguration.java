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
 * All portions are Copyright (C) 2022 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.dal.service;

import java.util.Map;

/**
 * Used by the {@link DataPoolChecker} to retrieve the database pool configuration for a specific
 * type of data
 */
public interface DataPoolConfiguration {

  /**
   * Provides a map that contains the data pool to be used for each entry of this configuration. The
   * entries (keys of the map) should be built according to the format expected by the
   * {@DataPoolChecker} to ensure that it is able to find the configuration correctly.
   *
   * @see DataPoolChecker#shouldUseDefaultPool
   *
   * @return configured values defined on a map with the IDs of the different entries as keys and
   *         the database pool name to be used by each key as values.
   */
  public Map<String, String> getDataPoolSelection();

  /**
   * Provides the name of the preference that specifies the default database pool to be used by the
   * elements of this configuration when there is no an specific configuration form them
   *
   * @return The preference name
   */
  public String getPreferenceName();

  /**
   * Provides the name of the type of data for this configuration
   *
   * @return The name of the data type
   */
  public String getDataType();

}
