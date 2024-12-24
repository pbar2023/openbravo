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
 * All portions are Copyright (C) 2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_forms;

/**
 * Allows to extend the standard behavior of the {@link TranslationManager} when handling
 * translations
 */
public interface TranslationManagerHook {

  /**
   * Executed after importing a translation file
   * 
   * @param fileName
   *          the name of the translation file
   * @param tableName
   *          the upper case name of the trl table where records in the translation file are
   *          imported
   * @param updateCount
   *          the number of updated records
   */
  public void onTrlFileImport(String fileName, String tableName, int updateCount);
}
