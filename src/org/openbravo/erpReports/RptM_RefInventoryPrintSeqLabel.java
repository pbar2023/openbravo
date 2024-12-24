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
package org.openbravo.erpReports;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.common.hooks.GetPrintTemplateHook;

/*
 * Report and Process class configured in Reference Inventory tab print button to print Reference Inventory Label
 */

public class RptM_RefInventoryPrintSeqLabel extends HttpSecureAppServlet {

  private static final long serialVersionUID = 1L;
  private static final String DEFAULT_OUTPUT_NAME = "HULabel";

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    if (vars.commandIn("DEFAULT")) {
      String strHandlingUnitId = vars
          .getSessionValue("RptM_RefInventoryPrintSeqLabel.inpmRefinventoryId_R");
      if (strHandlingUnitId.equals("")) {
        strHandlingUnitId = vars
            .getSessionValue("RptM_RefInventoryPrintSeqLabel.inpmRefinventoryId");
      }
      printPageDataPDF(request, response, vars, strHandlingUnitId);
    }
  }

  private void printPageDataPDF(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strHandlingUnitId) throws IOException, ServletException {
    log4j.debug("Output: PDF report");
    final HashMap<String, Object> parameters = new HashMap<>(1);
    parameters.put("M_REFINVENTORY_ID", strHandlingUnitId);
    renderJR(vars, response, getPrintTemplate(), getOutputReportFilename(), "pdf", parameters, null,
        null, true);
  }

  /*
   * Gets the print template for Handling Unit Label printing from highest priority hook either
   * Custom template from external module that implements GetPrintTemplateHook or default template
   * from core module GetDefaultPrintTemplate
   */

  private String getPrintTemplate() {
    return WeldUtils.getInstancesSortedByPriority(GetPrintTemplateHook.class)
        .get(0) // Only get the highest priority hook
        .getPrintTemplate();
  }

  /*
   * This method computes the suffix to be added for output report file name based on current date
   * i.e ddMMyyyhhmmss format
   */

  private String getOutputReportFilename() {
    SimpleDateFormat dateFormatter = new SimpleDateFormat("ddMMyyyhhmmss");
    String dateSuffix = dateFormatter.format(new Date());
    return DEFAULT_OUTPUT_NAME + dateSuffix;
  }
}
