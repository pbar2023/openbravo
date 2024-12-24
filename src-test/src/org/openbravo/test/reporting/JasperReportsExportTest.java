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
package org.openbravo.test.reporting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.report.ReportingUtils;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.JRFieldProviderDataSource;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Test that checks a jasper report is correctly exported to all the available export types If no
 * exception is launched, it works correctly, also necessary to check the new possible warnings
 * generated
 */
public class JasperReportsExportTest extends WeldBaseTest {

  @Override
  protected boolean shouldMockServletContext() {
    return true;
  }

  public static final List<String> PARAMS = Arrays.asList("PDF", "HTML", "TXT", "XML", "XLS",
      "XLSX", "CSV");

  /** defines the values the parameter will take. */
  @Rule
  public ParameterCdiTestRule<String> parameterValuesRule = new ParameterCdiTestRule<>(PARAMS);

  private @ParameterCdiTest String exportType;

  private static FieldProvider[] reportData;

  @BeforeClass
  public static void initializeData() throws ClassNotFoundException, InvocationTargetException,
      NoSuchMethodException, IllegalAccessException {
    ConnectionProvider readOnlyCP = new DalConnectionProvider(false);
    reportData = getReportData(readOnlyCP);
  }

  @Test
  public void shouldExportJasperReport() {
    setTestUserContext();
    setTestLogAppenderLevel(Level.WARN);
    String reportPath = getReportPath();
    ConnectionProvider readOnlyCP = new DalConnectionProvider(false);

    JRFieldProviderDataSource jrFieldProviderDataSource = new JRFieldProviderDataSource(reportData,
        "dd-mm-yyyy");

    ReportingUtils.exportJR(reportPath, ReportingUtils.ExportType.getExportType(this.exportType),
        new HashMap<>(), OutputStream.nullOutputStream(), false, readOnlyCP,
        jrFieldProviderDataSource, new HashMap<>());

    // There should be only 2 warnings, related to fonts
    assertThat(getTestLogAppender().getMessages(Level.WARN), hasSize(2));
  }

  /**
   * This function returns the data required to fill the report accordingly. It uses reflection to
   * obtain the data from OrderEditionData class
   *
   * @param readOnlyCP
   *          - Read only ConnectionProvider
   */
  private static FieldProvider[] getReportData(ConnectionProvider readOnlyCP)
      throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
      IllegalAccessException {
    String strCurrencyConv = "100";
    String adUserOrg = "'0','E443A31992CB4635AFCAEABE7183CE85','B843C30461EA4501935CB1D125C9C25A','BAE22373FEBE4CCCA24517E23F0C8A48',"
        + "'DC206C91AA6A4897B44DA897936E0EC3','19404EAD144C49A0AF37D54377CF452D','2E60544D37534C0B89E765FE29BC0B43','7BABA5FF80494CAFA54DEBD22EC46F01'";
    String adUserClient = "'0','23C59575B9CF467C9620760EB255B389'";
    String dateFrom = "20-10-2010";
    String dateTo = "20-10-2021";

    // Using reflection on OrderEditionData.select function, because it is a sqlc generated private
    // class
    Class<?> orderEditionDataClass = Class
        .forName("org.openbravo.erpCommon.ad_reports.OrderEditionData");
    Method orderEditionDataSelect = orderEditionDataClass.getMethod("select",
        ConnectionProvider.class, String.class, String.class, String.class, String.class,
        String.class, String.class, String.class, String.class, String.class, String.class,
        String.class);
    orderEditionDataSelect.setAccessible(true);

    return (FieldProvider[]) orderEditionDataSelect.invoke(null, readOnlyCP, strCurrencyConv,
        adUserOrg, adUserClient, dateFrom, dateTo, "", "", "", "", "", "");
  }

  private String getReportPath() {
    Path basePath = Paths.get(
        OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("source.path"),
        "src/org/openbravo/erpCommon/ad_reports/ReportSalesOrderJR.jrxml");
    return basePath.toString();
  }
}
