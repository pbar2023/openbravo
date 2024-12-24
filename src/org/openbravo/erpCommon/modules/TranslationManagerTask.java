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

package org.openbravo.erpCommon.modules;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.openbravo.database.CPStandAlone;
import org.openbravo.erpCommon.ad_forms.TranslationManager;

/**
 * Ant task for Export Translation
 * 
 */
public class TranslationManagerTask extends Task {

  private static final String STR_CLIENT = "0";
  private static final String UI_LANGUAGE = "en_US";

  private String exportDirectory;
  private String strLang;
  private String propertiesFile;

  public void setExportDirectory(String exportDirectory) {
    this.exportDirectory = exportDirectory;
  }

  public void setStrLang(String strLang) {
    this.strLang = strLang;
  }

  public void setPropertiesFile(String propertiesFile) {
    this.propertiesFile = propertiesFile;
  }

  @Override
  public void execute() {
    if (strLang.isBlank()) {
      throw new BuildException(
          "Parameter strLang cannot be empty. Example: ant export.translation -DstrLang=\"es_ES\"");
    }
    try {
      TranslationManager.exportTrl(new CPStandAlone(propertiesFile), exportDirectory, strLang,
          STR_CLIENT, UI_LANGUAGE);
    } catch (final Exception e) {
      throw new BuildException(e);
    }
  }

}
