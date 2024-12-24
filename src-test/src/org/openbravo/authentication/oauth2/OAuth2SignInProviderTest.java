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
 * All portions are Copyright (C) 2023-2024 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.authentication.oauth2;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.authentication.LoginStateHandler;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.module.Application;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.Image;
import org.openbravo.model.authentication.AuthenticationProvider;
import org.openbravo.model.authentication.OAuth2AuthenticationProvider;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.test.base.TestConstants;
import org.openbravo.test.base.mock.HttpServletRequestMock;

/**
 * Test to cover the button generation done by the {@link OAuth2SignInProvider}
 */
public class OAuth2SignInProviderTest extends WeldBaseTest {
  private static final Logger log = LogManager.getLogger();

  @Inject
  private OAuth2SignInProvider oauth2SignInProvider;

  @Before
  public void setRequestContext() {
    HttpServletRequestMock.setRequestMockInRequestContext();
    prepareData();
  }

  @After
  public void cleanUp() {
    rollback();
  }

  @Test
  public void generateButtons() {
    String html = oauth2SignInProvider.getLoginPageSignInHTMLCode();
    String expected = getExpectedResult();

    log.debug("Generated result: {}, expected result: {}", html, expected);
    assertThat("Buttons are correctly generated", html, equalTo(expected));
  }

  private String getExpectedResult() {
    try (InputStream inputStream = getClass().getResourceAsStream("buttons.html")) {
      String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      // replace the random values of the state stored in the session
      @SuppressWarnings("unchecked")
      Map<String, String> loginState = (Map<String, String>) RequestContext.get()
          .getSessionAttribute(LoginStateHandler.LOGIN_STATE);
      for (Entry<String, String> entry : loginState.entrySet()) {
        html = html.replaceAll("state: '" + entry.getKey() + "'",
            "state: '" + entry.getValue() + "'");
      }
      return html;
    } catch (IOException ex) {
      throw new OBException("Could not read the expected result", ex);
    }
  }

  private void prepareData() {
    disableExistingConfigurations();
    try {
      OBContext.setAdminMode(false);
      createOAuth2Configuration("Test 1", 1L, "650E72E5682A410F9DE4166FBA59A101",
          "B188EDFE47BA4B62B63038E3203A4D48");
      createOAuth2Configuration("Test 2", 2L, "CFB0455F982C4262AF3808BF0D468EDB", null);
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

  private void createOAuth2Configuration(String name, Long sequenceNumber, String oAuth2ConfigId,
      String imageId) {
    AuthenticationProvider config = OBProvider.getInstance().get(AuthenticationProvider.class);
    config.setClient(OBDal.getInstance().getProxy(Client.class, TestConstants.Clients.SYSTEM));
    config
        .setOrganization(OBDal.getInstance().getProxy(Organization.class, TestConstants.Orgs.MAIN));
    config.setName(name);
    config.setType("OPENID");
    config.setApplication(
        OBDal.getInstance().getProxy(Application.class, TestConstants.Applications.BACKOFFICE));
    config.setSequenceNumber(sequenceNumber);
    if (imageId != null) {
      config.setIcon(OBDal.getInstance().getProxy(Image.class, imageId));
    }
    OBDal.getInstance().save(config);

    OAuth2AuthenticationProvider oAuth2Config = OBProvider.getInstance()
        .get(OAuth2AuthenticationProvider.class);
    oAuth2Config
        .setClient(OBDal.getInstance().getProxy(Client.class, TestConstants.Clients.SYSTEM));
    oAuth2Config
        .setOrganization(OBDal.getInstance().getProxy(Organization.class, TestConstants.Orgs.MAIN));
    oAuth2Config.setNewOBObject(true);
    oAuth2Config.setId(oAuth2ConfigId);
    oAuth2Config.setClientID("1234");
    oAuth2Config.setClientSecret("secret");
    oAuth2Config.setAuthorizationURL("https://auth-server.com");
    oAuth2Config.setAccessTokenURL("https://token-server.com");
    oAuth2Config.setCertificateURL("https://certificate-server.com");
    oAuth2Config.setAuthProvider(config);
    config.getOAuth2AuthenticationProviderList().add(oAuth2Config);
    OBDal.getInstance().save(oAuth2Config);
  }
}
