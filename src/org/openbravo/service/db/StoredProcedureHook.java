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
 * All portions are Copyright (C) 2023 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.db;

import java.util.Map;

import org.openbravo.base.Prioritizable;
import org.openbravo.erpCommon.ad_process.ADProcessID;

/**
 * Hook executed when calling a process with the {@link CallProcess} class. The implementations of
 * this interface must be annotated with {@link ADProcessID} to indicate the related process that
 * triggers their execution.
 * 
 * @author Fermin Gascon
 *
 */
public interface StoredProcedureHook extends Prioritizable {
  /**
   * Executed when the process call ends successfully
   * 
   * @see CallProcess#callProcess(org.openbravo.model.ad.ui.Process, String, Map, Boolean)
   * 
   * @param recordId
   *          Id of the stored DB object in CallProces
   * @param processParameters
   *          The parameters used in the process call
   */
  public void onExecutionFinish(String recordId, Map<String, ?> processParameters);
}
