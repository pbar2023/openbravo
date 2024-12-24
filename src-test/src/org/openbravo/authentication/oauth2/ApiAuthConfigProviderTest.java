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
package org.openbravo.authentication.oauth2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.module.Application;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.authentication.AuthenticationProvider;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.test.base.TestConstants;

/**
 * Test to cover the configuration provider manage by {@link ApiAuthConfigProvider}
 */
public class ApiAuthConfigProviderTest extends WeldBaseTest {
  @Inject
  private ApiAuthConfigProvider apiAuthConfigProvider;

  @Before
  public void setRequestContext() {
    disableExistingConfigurations();
  }

  @After
  public void cleanUp() {
    rollback();
    apiAuthConfigProvider.invalidateCache();
  }

  @Test
  public void tokenConfigurationDoesNotExist() {
    assertFalse(apiAuthConfigProvider.existsApiAuthConfiguration());
  }

  @Test
  public void tokenConfigurationExist() {
    registerAuthorizationProvider();
    assertTrue(apiAuthConfigProvider.existsApiAuthConfiguration());
  }

  @Test
  public void getApiAuthType() {
    registerAuthorizationProvider();

    Optional<String> authType = apiAuthConfigProvider.getApiAuthType();
    assertTrue(authType.isPresent());
    assertEquals(authType.get(), "OAUTH2TOKEN");
  }

  private void registerAuthorizationProvider() {
    disableExistingConfigurations();
    try {
      OBContext.setAdminMode(false);
      AuthenticationProvider config = OBProvider.getInstance().get(AuthenticationProvider.class);
      config.setClient(OBDal.getInstance().getProxy(Client.class, TestConstants.Clients.SYSTEM));
      config.setOrganization(
          OBDal.getInstance().getProxy(Organization.class, TestConstants.Orgs.MAIN));
      config.setName("Test 1");
      config.setType("OAUTH2TOKEN");
      config.setApplication(
          OBDal.getInstance().getProxy(Application.class, TestConstants.Applications.API));
      config.setSequenceNumber(1L);
      OBDal.getInstance().save(config);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void disableExistingConfigurations() {
    OBDal.getInstance()
        .getSession()
        .createQuery("update AuthenticationProvider set active = false")
        .executeUpdate();
  }
}
