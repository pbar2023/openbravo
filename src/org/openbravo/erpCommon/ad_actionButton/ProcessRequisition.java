/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
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
 *************************************************************************
 */
package org.openbravo.erpCommon.ad_actionButton;

import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.RequisitionProcessor;
import org.openbravo.materialmgmt.RequisitionProcessor.Action;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

/**
 * Handler to process a Requisition. It calls the {@link RequisitionProcessor} and prints the result
 * as a message in the UI
 */
public class ProcessRequisition extends DalBaseProcess {
  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    final String requisitionId = (String) bundle.getParams().get("M_Requisition_ID");
    final Action action = Action.getByValue((String) bundle.getParams().get("action"));
    OBError msg = new OBError();
    try {
      RequisitionProcessor.process(requisitionId, action);
      msg.setType("Success");
      msg.setMessage(OBMessageUtils.messageBD("Success"));
    } catch (Exception e) {
      msg.setType("Error");
      msg.setMessage(e.getMessage());
    }

    bundle.setResult(msg);
  }
}
