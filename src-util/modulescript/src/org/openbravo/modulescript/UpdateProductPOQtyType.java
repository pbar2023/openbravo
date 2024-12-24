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

import java.sql.PreparedStatement;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;

/**
 * The qtytype column of M_Product_PO now accepts a new value ('A' for Any), which should also be the default one.
 * This modulescript updates the column for those records that have it at null.
 * Related Jira: https://openbravo.atlassian.net/browse/RM-15921
 */
public class UpdateProductPOQtyType extends ModuleScript {

  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      UpdateProductPOQtyTypeData.update(cp);  
    } catch (Exception e) {
      handleError(e);
    }
  }
  
  @Override
  protected boolean executeOnInstall() {
    return false;
  }
  
  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("0", null, new OpenbravoVersion(3, 0, 244000));
  }
}