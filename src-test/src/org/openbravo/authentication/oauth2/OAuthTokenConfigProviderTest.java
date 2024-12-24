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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Test;
import org.openbravo.authentication.oauth2.OAuthTokenConfigProvider.Configuration;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.module.Application;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.authentication.AuthenticationProvider;
import org.openbravo.model.authentication.OAuth2TokenAuthenticationProvider;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.test.base.TestConstants;

/**
 * Test to cover the configuration provider manage by {@link OAuthTokenConfigProvider}
 */
public class OAuthTokenConfigProviderTest extends WeldBaseTest {
  @Inject
  private OAuthTokenConfigProvider oauthTokenConfigProvider;

  @After
  public void cleanUp() {
    rollback();
    oauthTokenConfigProvider.invalidateCache();
  }

  @Test
  public void missingTokenConfiguration() {
    disableExistingConfigurations();
    Configuration config = oauthTokenConfigProvider.getConfiguration();
    assertNull(config);
  }

  @Test
  public void getTokenConfiguration() {
    registerAuthorizationProvider();

    Configuration authType = oauthTokenConfigProvider.getConfiguration();
    assertNotNull(authType);
    assertEquals(authType.getJwksURL(), "CERTIFICATE_URL");
    assertEquals(authType.getTokenProperty(), "TOKEN_PROPERTY");
  }

  private void registerAuthorizationProvider() {
    disableExistingConfigurations();
    try {
      OBContext.setAdminMode(false);
      AuthenticationProvider provider = createOAuth2Configuration("Test 1", 1L);
      createOAuth2TokenConfiguration("123456", provider);
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

  private AuthenticationProvider createOAuth2Configuration(String name, Long sequenceNumber) {
    AuthenticationProvider config = OBProvider.getInstance().get(AuthenticationProvider.class);
    config.setClient(OBDal.getInstance().getProxy(Client.class, TestConstants.Clients.SYSTEM));
    config
        .setOrganization(OBDal.getInstance().getProxy(Organization.class, TestConstants.Orgs.MAIN));
    config.setName(name);
    config.setType("OAUTH2TOKEN");
    config.setApplication(
        OBDal.getInstance().getProxy(Application.class, TestConstants.Applications.API));
    config.setSequenceNumber(sequenceNumber);
    OBDal.getInstance().save(config);

    return config;
  }

  private void createOAuth2TokenConfiguration(String oAuth2ConfigId,
      AuthenticationProvider config) {
    OAuth2TokenAuthenticationProvider oAuth2Config = OBProvider.getInstance()
        .get(OAuth2TokenAuthenticationProvider.class);
    oAuth2Config
        .setClient(OBDal.getInstance().getProxy(Client.class, TestConstants.Clients.SYSTEM));
    oAuth2Config
        .setOrganization(OBDal.getInstance().getProxy(Organization.class, TestConstants.Orgs.MAIN));
    oAuth2Config.setNewOBObject(true);
    oAuth2Config.setId(oAuth2ConfigId);
    oAuth2Config.setJwksUrl("CERTIFICATE_URL");
    oAuth2Config.setTokenProperty("TOKEN_PROPERTY");
    oAuth2Config.setAuthProvider(config);
    config.getOAuth2TokenAuthenticationProviderList().add(oAuth2Config);
    OBDal.getInstance().save(oAuth2Config);
  }

}
