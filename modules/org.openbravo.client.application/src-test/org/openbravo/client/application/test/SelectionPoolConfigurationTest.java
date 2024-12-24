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
package org.openbravo.client.application.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.openbravo.test.base.TestConstants.Clients.SYSTEM;
import static org.openbravo.test.base.TestConstants.Orgs.MAIN;

import javax.persistence.PersistenceException;

import org.junit.After;
import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.DataPoolReport;
import org.openbravo.client.application.DataPoolSelection;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Tests Data Pool Selection Configuration window on creation and update
 */
public class SelectionPoolConfigurationTest extends WeldBaseTest {

  private static final String REPORT_AGILE_ID = "EB4C4053F3B94A17A08D1DD7E89CEB7E";
  private static final String REPORT_CASHFLOW_ID = "1B0BF927933A4F41A73739CB6E4A9AD0";

  private static final String MESSAGE_EXCEPTION = "org.hibernate.exception.ConstraintViolationException: could not execute batch";

  @After
  public void cleanUp() {
    OBDal.getInstance().rollbackAndClose();
  }

  @Test
  public void creatingSelectionPoolConfigurationWithAllRequiredFieldsShouldNotFail() {
    try {
      OBContext.setAdminMode(false);
      DataPoolSelection dataPoolConf = newDataPoolSelectionConf();
      OBDal.getInstance().save(dataPoolConf);
      OBDal.getInstance().flush();

      assertNotNull(dataPoolConf.getId());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void updatingSelectionPoolConfigurationWithAllRequiredFieldsShouldNotFail() {
    try {
      OBContext.setAdminMode(false);
      DataPoolSelection dataPoolConf = newDataPoolSelectionConf();
      OBDal.getInstance().save(dataPoolConf);
      dataPoolConf.setDataPool("DEFAULT");
      OBDal.getInstance().flush();

      assertThat(dataPoolConf.getDataPool(), equalTo("DEFAULT"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void creatingSelectionPoolConfigurationWithoutReportShouldFail() {
    PersistenceException exceptionRule = assertThrows(PersistenceException.class, () -> {
      try {
        OBContext.setAdminMode(false);
        DataPoolSelection dataPoolConf = newDataPoolSelectionConf();
        dataPoolConf.setReport(null);
        OBDal.getInstance().save(dataPoolConf);
        OBDal.getInstance().flush();
      } finally {
        OBContext.restorePreviousMode();
      }
    });

    assertThat(exceptionRule.getMessage(), containsString(MESSAGE_EXCEPTION));
  }

  @Test
  public void updatingSelectionPoolConfigurationWithoutReportShouldFail() {
    PersistenceException exceptionRule = assertThrows(PersistenceException.class, () -> {
      try {
        OBContext.setAdminMode(false);
        DataPoolSelection dataPoolConf = newDataPoolSelectionConf();
        OBDal.getInstance().save(dataPoolConf);
        dataPoolConf.setReport(null);
        OBDal.getInstance().flush();
      } finally {
        OBContext.restorePreviousMode();
      }
    });

    assertThat(exceptionRule.getMessage(), containsString(MESSAGE_EXCEPTION));
  }

  @Test
  public void creatingSelectionPoolConfigurationNotUniqueShouldFail() {
    PersistenceException exceptionRule = assertThrows(PersistenceException.class, () -> {
      try {
        OBContext.setAdminMode(false);
        DataPoolSelection dataPoolConf1 = newDataPoolSelectionConf();
        OBDal.getInstance().save(dataPoolConf1);
        OBDal.getInstance().flush();
        DataPoolSelection dataPoolConf2 = newDataPoolSelectionConf();
        OBDal.getInstance().save(dataPoolConf2);
        OBDal.getInstance().flush();
      } finally {
        OBContext.restorePreviousMode();
      }
    });

    assertThat(exceptionRule.getMessage(), containsString(MESSAGE_EXCEPTION));
  }

  @Test
  public void updatingSelectionPoolConfigurationNotUniqueShouldFail() {
    PersistenceException exceptionRule = assertThrows(PersistenceException.class, () -> {
      try {
        OBContext.setAdminMode(false);
        DataPoolSelection dataPoolConf1 = newDataPoolSelectionConf();
        OBDal.getInstance().save(dataPoolConf1);
        DataPoolSelection dataPoolConf2 = newDataPoolSelectionConf();
        dataPoolConf2
            .setReport(OBDal.getInstance().getProxy(DataPoolReport.class, REPORT_CASHFLOW_ID));
        OBDal.getInstance().save(dataPoolConf2);
        OBDal.getInstance().flush();
        dataPoolConf2
            .setReport(OBDal.getInstance().getProxy(DataPoolReport.class, REPORT_AGILE_ID));
        OBDal.getInstance().flush();
      } finally {
        OBContext.restorePreviousMode();
      }
    });

    assertThat(exceptionRule.getMessage(), containsString(MESSAGE_EXCEPTION));
  }

  private DataPoolSelection newDataPoolSelectionConf() {
    DataPoolSelection conf = OBProvider.getInstance().get(DataPoolSelection.class);
    conf.setClient(OBDal.getInstance().getProxy(Client.class, SYSTEM));
    conf.setOrganization(OBDal.getInstance().getProxy(Organization.class, MAIN));
    conf.setDataType("REPORT");
    conf.setDataPool("RO");
    conf.setReport(OBDal.getInstance().getProxy(DataPoolReport.class, REPORT_AGILE_ID));
    return conf;
  }

}
