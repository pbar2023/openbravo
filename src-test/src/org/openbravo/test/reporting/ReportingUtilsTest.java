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
 * All portions are Copyright (C) 2022-2024 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.reporting;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.report.ReportingUtils;
import org.openbravo.client.application.report.ReportingUtils.ExportType;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.test.base.Issue;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;

/**
 * Test cases covering report generation using {@link ReportingUtils}
 */
public class ReportingUtilsTest extends WeldBaseTest {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected boolean shouldMockServletContext() {
    return true;
  }

  @Before
  public void setCacheUsage() {
    OBDal.getInstance().getSession().createQuery("UPDATE ADModule set inDevelopment = false");
    OBDal.getInstance().flush();
  }

  @After
  public void cleanUp() throws IOException {
    rollback();
    Path report = getTmpFile().toPath();
    try {
      Files.deleteIfExists(report);
    } catch (IOException ex) {
      log.error("Could not delete file {}", report, ex);
    }
  }

  /**
   * Generates a report that uses {@link Utility#showImageLogo(String, String)} and a subreport that
   * executes a query
   */
  @Test
  @Issue("48465")
  public void generateReport() {
    File report = getTmpFile();
    generateReport(report, null);
    assertThat("report generated correctly", report.exists(), equalTo(true));
  }

  /**
   * Generates a report that uses {@link Utility#showImageLogo(String, String)} and a subreport that
   * executes a query. It provides an specific connection provider to generate the report.
   */
  @Test
  @Issue("48465")
  public void generateReportWithConnectionProvider() {
    File report = getTmpFile();
    generateReport(report, DalConnectionProvider.getReadOnlyConnectionProvider());
    assertThat("report generated correctly", report.exists(), equalTo(true));
  }

  @Test
  @Issue("54168")
  public void compileReportData() throws IOException, URISyntaxException, JRException {
    String reportData = new String(Files.readAllBytes(getReportPath()));
    JasperReport report = ReportingUtils.compileReportData(reportData);
    assertThat("report generated correctly", report, notNullValue());
    report = ReportingUtils.compileReportData(reportData);
    assertThat("report generated from cache correctly", report, notNullValue());
  }

  @Test
  @Issue("54168")
  public void compileReportDataToFile() throws IOException, URISyntaxException, JRException {
    String reportData = new String(Files.readAllBytes(getReportPath()));
    File result = getTmpFile();
    ReportingUtils.compileReportDataToFile(reportData, result.toPath());
    assertThat("report generated correctly", result.exists(), equalTo(true));
    ReportingUtils.compileReportDataToFile(reportData, result.toPath());
    assertThat("report generated from cache correctly", result.exists(), equalTo(true));
  }

  private void generateReport(File report, ConnectionProvider connectionProvider) {
    try {
      ReportingUtils.exportJR(getReportPath().toString(), ExportType.HTML, new HashMap<>(), report,
          true, connectionProvider, null, new HashMap<>());
    } catch (Exception ex) {
      log.error("Could not generate test report", ex);
    }
  }

  private File getTmpFile() {
    return new File(ReportingUtils.getTempFolder(), "tmp.html");
  }

  private Path getReportPath() throws URISyntaxException {
    return Paths.get(getClass().getResource("reports/Main.jrxml").toURI());
  }
}
