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

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.Tuple;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.authentication.AuthenticatedUser;
import org.openbravo.authentication.AuthenticationType;
import org.openbravo.authentication.ExternalAuthenticationManager;
import org.openbravo.authentication.oauth2.OAuthTokenConfigProvider.Configuration;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.authentication.OAuth2TokenAuthenticationProvider;

/**
 * Allows to authenticate with a JWT token provided by an external authentication provider. This
 * token is validated using the keys provided by the configured JWKS URL. If the validation is
 * passed, then the value of the property indicated by
 * {@link OAuth2TokenAuthenticationProvider#PROPERTY_TOKENPROPERTY} is extracted of the token, and
 * the user matching that value on its {@link User#PROPERTY_OAUTH2TOKENVALUE} is the one identified
 * as the authenticated user.
 */
@AuthenticationType("OAUTH2TOKEN")
public class OAuth2TokenAuthenticationManager extends ExternalAuthenticationManager {
  private static final Logger log = LogManager.getLogger();
  private static final String BEARER = "Bearer ";

  @Inject
  private JWTDataProvider jwtDataProvider;

  @Inject
  private OAuthTokenConfigProvider oauthTokenConfigProvider;

  @Override
  public AuthenticatedUser doExternalAuthentication(HttpServletRequest request,
      HttpServletResponse response) {
    throw new UnsupportedOperationException("doExternalAuthentication is not implemented");
  }

  @Override
  protected void doLogout(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    throw new UnsupportedOperationException("doLogout is not implemented");
  }

  @Override
  public String doWebServiceAuthenticate(HttpServletRequest request) {
    return handleAuthorizationResponse(request);
  }

  private String handleAuthorizationResponse(HttpServletRequest request) {

    try {
      OBContext.setAdminMode(true);
      String authorizationHeader = request.getHeader("Authorization");

      if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER)) {
        log.error("The authentication header token has not been received");
        return null;
      }

      Configuration config = oauthTokenConfigProvider.getConfiguration();
      return config != null ? getUser(authorizationHeader.substring(BEARER.length()), config)
          : null;

    } catch (OAuth2TokenVerificationException ex) {
      log.error("The token verification failed", ex);
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Based on the provided token retrieves the ID of the authenticated {@link User}.
   *
   * @param tokenID
   *          Access token received in the request to be authenticated
   * @param configuration
   *          the OAuth 2.0 Token configuration with the information that will be used to verify the
   *          token like the URL to get the public keys required by the algorithm used for
   *          encrypting the token data.
   *
   * @return the identifier of the authenticated {@link User}
   *
   * @throws OAuth2TokenVerificationException
   *           If it is not possible to verify the token or extract the authentication data
   */
  private String getUser(String tokenID, Configuration configuration)
      throws OAuth2TokenVerificationException {

    Map<String, Object> authData = jwtDataProvider.getData(tokenID, configuration.getJwksURL(),
        configuration.getTokenProperty());
    String userIdentifierValue = (String) authData.get(configuration.getTokenProperty());

    if (StringUtils.isBlank(userIdentifierValue)) {
      throw new OAuth2TokenVerificationException(
          "The user " + configuration.getTokenProperty() + " was not found");
    }

    //@formatter:off
    String hql = "select u.id as id, u.username as userName" +
                 "  from ADUser u" +
                 " where oauth2TokenValue = :tokenValue";
    //@formatter:on
    Tuple user = OBDal.getInstance()
        .getSession()
        .createQuery(hql, Tuple.class)
        .setParameter("tokenValue", userIdentifierValue)
        .setMaxResults(1)
        .uniqueResult();

    return user != null ? (String) user.get("id") : null;
  }
}
