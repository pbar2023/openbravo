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
package org.openbravo.dal.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.DataPoolReport;
import org.openbravo.client.application.DataPoolSelection;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.SessionInfo;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.test.base.TestConstants;

/**
 * Tests to cover the usage of the {@link DataPoolChecker} to retrieve the data pool configuration
 * for the application reports
 */
public class DataPoolCheckerTest extends WeldBaseTest {
  private static final String REPORT_USING_RO_POOL = "800000";
  private static final String REPORT_USING_DEFAULT_POOL = "800039";
  private static final String REPORT_NOT_CONFIGURED = "800040";

  @Before
  public void configure() {
    createDataPoolConfiguration();
    DataPoolChecker.getInstance().refreshDataPoolProcesses();
  }

  @After
  public void cleanUp() {
    SessionInfo.setProcessId(null);
    SessionInfo.setProcessType(null);
    OBDal.getInstance().rollbackAndClose();
  }

  @Test
  public void usePoolFromPreferenceIfConfigurationIsNotSpecified() {
    assertPoolFromPreferenceIsUsed();
  }

  @Test
  public void useReadOnlyPoolByConfiguration() {
    SessionInfo.setProcessId(REPORT_USING_RO_POOL);
    SessionInfo.setProcessType("REPORT");
    assertTrue(isReadOnlyPoolUsed());
  }

  @Test
  public void useDefaultPoolByConfiguration() {
    SessionInfo.setProcessId(REPORT_USING_DEFAULT_POOL);
    SessionInfo.setProcessType("REPORT");
    assertFalse(isReadOnlyPoolUsed());
  }

  @Test
  public void reportDataTypeIsUsedByDefault() {
    SessionInfo.setProcessId(REPORT_USING_DEFAULT_POOL);
    assertFalse(isReadOnlyPoolUsed());
  }

  @Test
  public void usePoolFromPreferenceWithUnknownDataType() {
    SessionInfo.setProcessId(REPORT_USING_DEFAULT_POOL);
    // reports executed from process definitions set "PD" process type
    SessionInfo.setProcessType("PD");
    assertFalse(isReadOnlyPoolUsed());
  }

  @Test
  public void usePoolFromPreferenceIfReportIsNotConfigured() {
    SessionInfo.setProcessId(REPORT_NOT_CONFIGURED);
    SessionInfo.setProcessType("REPORT");
    assertPoolFromPreferenceIsUsed();
  }

  private void createDataPoolConfiguration() {
    try {
      OBContext.setAdminMode(false);
      DataPoolSelection config1 = newDataPoolSelection(REPORT_USING_RO_POOL, "RO");
      DataPoolSelection config2 = newDataPoolSelection(REPORT_USING_DEFAULT_POOL, "DEFAULT");
      OBDal.getInstance().save(config1);
      OBDal.getInstance().save(config2);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private DataPoolSelection newDataPoolSelection(String reportId, String poolName) {
    DataPoolSelection config = OBProvider.getInstance().get(DataPoolSelection.class);
    config.setClient(OBDal.getInstance().getProxy(Client.class, TestConstants.Clients.SYSTEM));
    config
        .setOrganization(OBDal.getInstance().getProxy(Organization.class, TestConstants.Orgs.MAIN));
    config.setDataType("REPORT");
    config.setReport(OBDal.getInstance().getProxy(DataPoolReport.class, reportId));
    config.setDataPool(poolName);
    return config;
  }

  private String getDefaultPoolForReports() {
    //@formatter:off
    String hql = 
            "select p.searchKey " +
            "  from ADPreference p " +
            " where p.property = 'OBUIAPP_DefaultDBPoolForReports' " +
            "   and p.active = true " +
            "   and p.visibleAtClient.id = '0' " +
            "   and p.visibleAtOrganization.id = '0' ";
    //@formatter:on
    return OBDal.getInstance()
        .getSession()
        .createQuery(hql, String.class)
        .setMaxResults(1)
        .uniqueResult();
  }

  private void assertPoolFromPreferenceIsUsed() {
    boolean useReadOnlyPoolByDefault = "RO".equals(getDefaultPoolForReports());
    assertThat(isReadOnlyPoolUsed(), equalTo(useReadOnlyPoolByDefault));
  }

  private boolean isReadOnlyPoolUsed() {
    return !DataPoolChecker.getInstance().shouldUseDefaultPool();
  }
}
