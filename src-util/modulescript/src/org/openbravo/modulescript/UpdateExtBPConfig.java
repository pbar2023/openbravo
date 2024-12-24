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
package org.openbravo.modulescript;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.database.ConnectionProvider;

import java.sql.PreparedStatement;

public class UpdateExtBPConfig extends ModuleScript {

  private static final Logger log4j = LogManager.getLogger();

  @Override
  public void execute() {
    try {
      log4j.debug("Updating CRM Connector properties to keep configuration for 3 forms");
      ConnectionProvider cp = getConnectionProvider();
      PreparedStatement ps = cp.getPreparedStatement("UPDATE c_extbp_config_property "
          + "SET isdisplayedincreate = isdisplayedindetail, isdisplayedinedit = isdisplayedindetail, "
          + "create_seqno = detail_seqno, edit_seqno = detail_seqno, "
          + "sectionmessagecreate_id = sectionmessage_id , sectionmessageedit_id = sectionmessage_id, "
          + "create_colspan = detail_colspan, edit_colspan = detail_colspan");
      ps.executeUpdate();
    } catch (Exception e) {
      handleError(e);
    }
  }

  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("0", null, new OpenbravoVersion(3, 0, 223900));
  }

  @Override
  protected boolean executeOnInstall() {
    return false;
  }

}
