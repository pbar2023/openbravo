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

package org.openbravo.modulescript;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.database.ConnectionProvider;

public class UpdateDocTypeDocumentSequence extends ModuleScript {

  private static final Logger log4j = LogManager.getLogger();

  /**
   * Updates DocType with IsDocNoControlled as N and DocNoSequence_ID as NULL
   * when the DocumentSequence associated with DocType has Calculation
   * Method as N (System DocumentNo_<tableName>) and IsDocNoControlled is Y.
   *
   * This way document sequence having CalculationMethod as N i.e
   * (System DocumentNo_<tableName>) are not used in any of the
   * existing document types as this calculation method is deprecated.
   */

  @Override
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      boolean isDocTypeDocumentSequenceUpdated = UpdateDocTypeDocumentSequenceData
          .isDocTypeDocumentSequenceUpdated(cp);
      if (!isDocTypeDocumentSequenceUpdated) {
        long init = System.currentTimeMillis();
        int docTypeDocumentSequenceUpdated = UpdateDocTypeDocumentSequenceData
            .updateDocTypeDocumentSequence(cp);
        if (docTypeDocumentSequenceUpdated > 0) {
          log4j.info("Updated " + docTypeDocumentSequenceUpdated + " Document Types in "
              + (System.currentTimeMillis() - init) + " ms.");
        }
        UpdateDocTypeDocumentSequenceData.createPreference(cp);
      }
    } catch (Exception e) {
      handleError(e);
    }
  }

  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("0", null, new OpenbravoVersion(3, 0, 242000));
  }
}
