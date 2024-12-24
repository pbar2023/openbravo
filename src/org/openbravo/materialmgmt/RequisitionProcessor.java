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
package org.openbravo.materialmgmt;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.service.db.CallProcess;
import org.openbravo.synchronization.event.SynchronizationEvent;

/**
 * Central place to process requisitions from Java. It calls the M_Requisition_Post procedure and
 * manage the Push API events. In case of error an {@link OBException} is thrown
 */
public class RequisitionProcessor {
  private static final String M_REQUISITION_POST_ID = "1004400003";

  /**
   * Valid actions when processing a requisition
   */
  public enum Action {
    COMPLETE("CO"), REACTIVATE("RE"), CLOSE("CL");

    private final String value;

    private Action(final String value) {
      this.value = value;
    }

    /**
     * Utility method to get the Action from a searchkey, like for example 'CO'
     */
    public static Action getByValue(String searchKey) {
      return Arrays.stream(Action.values())
          .filter(e -> searchKey != null && searchKey.equals(e.value))
          .findFirst()
          .orElseThrow(() -> new RuntimeException(
              "Action not valid for processing a requisition: " + searchKey));
    }
  }

  /**
   * Process the given requisition. After that the requisition is refreshed from database and
   * returned. Any Push API event is launched if processed successfully.
   * 
   * @param requisitionId
   *          requisition Id
   * @param action
   *          the action to perform when processing the requisition: complete, reactivate or close.
   * @throws OBException
   *           in case of error, a exception with the message is thrown
   */
  public static Requisition process(final String requisitionId, final Action action)
      throws OBException {
    OBContext.setAdminMode();
    try {
      final Requisition requisition = (Requisition) OBDal.getInstance()
          .getProxy(Requisition.ENTITY_NAME, requisitionId);
      requisition.setDocumentAction(action.value);
      OBDal.getInstance().flush(); // Necessary for the M_Requisition_Post

      callMRequisitionPost(requisitionId);
      OBDal.getInstance().refresh(requisition);

      SynchronizationEvent.getInstance().triggerEvent("API_Process_Requisition", requisitionId);

      return requisition;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private static void callMRequisitionPost(final String requisitionId) {
    final org.openbravo.model.ad.ui.Process process = OBDal.getInstance()
        .get(org.openbravo.model.ad.ui.Process.class, M_REQUISITION_POST_ID);
    final ProcessInstance pinstance = CallProcess.getInstance().call(process, requisitionId, null);
    final OBError result = OBMessageUtils.getProcessInstanceMessage(pinstance);
    if (StringUtils.equals("Error", result.getType())) {
      throw new OBException(result.getMessage());
    }
  }
}
