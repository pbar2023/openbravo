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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openbravo.authentication.oauth2.OAuthTokenConfigProvider.Configuration;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.test.base.TestConstants;

/**
 * Tests the authentication management performed to make login with OAuth 2.0 with tokens managed by
 * {@link OAuth2TokenAuthenticationManager}
 */
public class OAuth2TokenAuthenticationManagerTest extends WeldBaseTest {

  @Mock
  private JWTDataProvider jwtDataProvider;

  @Mock
  private OAuthTokenConfigProvider oauthTokenConfigProvider;

  @InjectMocks
  private OAuth2TokenAuthenticationManager authManager;

  @Before
  public void setRequestContext() {
    updateUserWithTokenProperty();
  }

  @After
  public void cleanUp() {
    rollback();
  }

  @Test
  public void externalAuthenticationNotImplemented() {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

    assertThrows(UnsupportedOperationException.class, () -> {
      authManager.doExternalAuthentication(request, response);
    });
  }

  @Test
  public void doLogoutNotImplemented() {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

    assertThrows(UnsupportedOperationException.class, () -> {
      authManager.doLogout(request, response);
    });
  }

  @Test
  public void missingAuthorizationHeader() {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

    assertNull(authManager.doWebServiceAuthenticate(request));
  }

  @Test
  public void wrongFormatAuthorizationHeader() {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

    Mockito.when(request.getHeader("Authorization")).thenReturn("TOKEN_VALUE");

    assertNull(authManager.doWebServiceAuthenticate(request));
  }

  @Test
  public void userDoesNotMatchWithProvidedProperty() {
    HttpServletRequest request = mockRequest();
    mockConfiguration();
    mockJWTDataProvider("");

    assertNull(authManager.doWebServiceAuthenticate(request));
  }

  @Test
  public void userWithPropertyNotFound() {
    HttpServletRequest request = mockRequest();
    mockConfiguration();
    mockJWTDataProvider("OTHER_PROPERTY_VALUE");

    assertNull(authManager.doWebServiceAuthenticate(request));
  }

  @Test
  public void userWithPropertyFound() {
    HttpServletRequest request = mockRequest();
    mockConfiguration();
    mockJWTDataProvider("PROPERTY_VALUE");

    assertThat(authManager.doWebServiceAuthenticate(request), equalTo("100"));
  }

  private HttpServletRequest mockRequest() {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getHeader("Authorization")).thenReturn("Bearer TOKEN_VALUE");
    return request;
  }

  private void mockConfiguration() {
    Configuration configuration = Mockito.mock(Configuration.class);
    Mockito.when(configuration.getJwksURL()).thenReturn("CERTIFICATE_URL");
    Mockito.when(configuration.getTokenProperty()).thenReturn("TOKEN_PROPERTY");
    Mockito.when(oauthTokenConfigProvider.getConfiguration()).thenReturn(configuration);
  }

  private void mockJWTDataProvider(String propertyValue) {
    try {
      Map<String, Object> userProperties = Map.of("TOKEN_PROPERTY", propertyValue);
      Mockito.when(jwtDataProvider.getData("TOKEN_VALUE", "CERTIFICATE_URL", "TOKEN_PROPERTY"))
          .thenReturn(userProperties);
    } catch (OAuth2TokenVerificationException ex) {
      throw new OBException(ex);
    }
  }

  private void updateUserWithTokenProperty() {
    try {
      OBContext.setAdminMode(false);
      User user = OBDal.getInstance().get(User.class, TestConstants.Users.OPENBRAVO);
      user.setOauth2TokenValue("PROPERTY_VALUE");
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
