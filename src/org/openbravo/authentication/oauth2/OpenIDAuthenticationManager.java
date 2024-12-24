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

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.Tuple;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.authentication.AuthenticatedUser;
import org.openbravo.authentication.AuthenticationException;
import org.openbravo.authentication.AuthenticationType;
import org.openbravo.authentication.ExternalAuthenticationManager;
import org.openbravo.authentication.LoginStateHandler;
import org.openbravo.base.HttpBaseUtils;
import org.openbravo.base.HttpClientManager;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.authentication.OAuth2AuthenticationProvider;
import org.openbravo.utils.FormatUtilities;

/**
 * Allows to authenticate with an external authentication provider using OpenID.
 */
@AuthenticationType("OPENID")
public class OpenIDAuthenticationManager extends ExternalAuthenticationManager {
  private static final Logger log = LogManager.getLogger();
  static final String DEFAULT_REDIRECT_PATH = "/secureApp/LoginHandler.html?loginMethod=OPENID";

  @Inject
  private LoginStateHandler authStateHandler;

  @Inject
  private JWTDataProvider jwtDataProvider;

  @Inject
  private HttpClientManager httpClientManager;

  @Override
  public AuthenticatedUser doExternalAuthentication(HttpServletRequest request,
      HttpServletResponse response) {
    if (request.getParameterMap().containsKey("code")) {
      if (!isValidAuthorizationResponse(request)) {
        log.error("The authorization response validation was not passed");
        throw new AuthenticationException(buildError());
      }
      return handleAuthorizationResponse(request);
    }
    JSONObject credential = getCredential(request)
        .orElseThrow(() -> new AuthenticationException(buildError()));
    return handleAuthorizationCredential(credential);
  }

  @Override
  public Optional<User> authenticate(String authProvider, JSONObject credential)
      throws JSONException {
    AuthenticatedUser user = handleAuthorizationCredential(credential);
    return Optional.of(OBDal.getInstance().get(User.class, user.getId()));
  }

  @Override
  protected void doLogout(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    throw new UnsupportedOperationException("doLogout is not implemented");
  }

  private boolean isValidAuthorizationResponse(HttpServletRequest request) {
    String code = request.getParameter("code");
    String state = request.getParameter("state");
    log.trace("Authorization response parameters: code = {}, state = {}", code, state);
    return code != null && authStateHandler.isValidKey(state);
  }

  private AuthenticatedUser handleAuthorizationResponse(HttpServletRequest request) {
    OAuth2AuthenticationProvider config = getConfig(request);
    String code = request.getParameter("code");
    String redirectURL = getRedirectURL();
    return handleAuthorizationResponse(code, redirectURL, config);
  }

  private Optional<JSONObject> getCredential(HttpServletRequest request) {
    try {
      String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
      return body.isBlank() ? Optional.empty()
          : Optional.of(new JSONObject(body).getJSONObject("credential"));
    } catch (IOException | JSONException ex) {
      log.error("Could not extract the credential data from the request body", ex);
      return Optional.empty();
    }
  }

  private AuthenticatedUser handleAuthorizationCredential(JSONObject credential) {
    if (credential.has("id_token")) {
      // PKCE flow: we only need to get the user information from the token data, the rest has
      // already being handled on the client side
      return findUserFromCredential(credential);
    }
    return handleAuthorizationResponse(credential);
  }

  private AuthenticatedUser handleAuthorizationResponse(JSONObject credential) {
    try {
      OAuth2AuthenticationProvider config = OBDal.getInstance()
          .get(OAuth2AuthenticationProvider.class, credential.getString("authProviderId"));
      String code = credential.getString("code");
      String redirectURL = credential.getString("redirectUri");
      return handleAuthorizationResponse(code, redirectURL, config);
    } catch (JSONException ex) {
      log.error("Unexpected authentication data: {}", credential, ex);
      throw new AuthenticationException(buildError());
    }
  }

  private AuthenticatedUser handleAuthorizationResponse(String code, String redirectURL,
      OAuth2AuthenticationProvider config) {
    try {
      OBContext.setAdminMode(true);
      String tokenResponseBody = getAuthorizationBody(code, redirectURL, config);
      return getUser(tokenResponseBody, config);
    } catch (OAuth2TokenVerificationException ex) {
      log.error("The token verification failed", ex);
      throw new AuthenticationException(buildError("AUTHENTICATION_DATA_VERIFICATION_FAILURE"));
    } catch (Exception ex) {
      log.error("Error handling the authorization response", ex);
      throw new AuthenticationException(buildError());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private AuthenticatedUser findUserFromCredential(JSONObject credential) {
    try {
      return getUser(credential, credential.getString("authProviderId"));
    } catch (OAuth2TokenVerificationException ex) {
      log.error("The token verification failed", ex);
      throw new AuthenticationException(buildError("AUTHENTICATION_DATA_VERIFICATION_FAILURE"));
    } catch (Exception ex) {
      log.error("Error extracting user from credential data", ex);
      throw new AuthenticationException(buildError());
    }
  }

  /**
   * Retrieves the user's email from the request, doing an OAuth2 authentication and token request
   *
   * @param request
   *          - Request that should allow oauth2 authentication
   * @param config
   *          - OAuth2 authentication provider
   * @return Email of the user, provided by the OAuth2 provider
   */
  protected String getAuthenticatedUserEmail(HttpServletRequest request,
      OAuth2AuthenticationProvider config) {
    try {
      OBContext.setAdminMode(true);
      String code = request.getParameter("code");
      String redirectURL = getRedirectURL();
      String tokenResponseBody = getAuthorizationBody(code, redirectURL, config);
      JSONObject tokenData = new JSONObject(tokenResponseBody);
      return getEmail(tokenData, config);
    } catch (OAuth2TokenVerificationException ex) {
      log.error("The token verification failed", ex);
      throw new AuthenticationException(buildError("AUTHENTICATION_DATA_VERIFICATION_FAILURE"));
    } catch (Exception ex) {
      log.error("Error handling the authorization response", ex);
      throw new AuthenticationException(buildError());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private String getAuthorizationBody(String code, String redirectURL,
      OAuth2AuthenticationProvider config) {
    try {
      OBContext.setAdminMode(true);

      HttpRequest accessTokenRequest = buildAccessTokenRequest(code, redirectURL, config);
      HttpResponse<String> tokenResponse = httpClientManager.send(accessTokenRequest);
      int responseCode = tokenResponse.statusCode();
      if (responseCode >= 200 && responseCode < 300) {
        return tokenResponse.body();
      }
      log.error("The token request failed with a {} error {}", responseCode, tokenResponse.body());
      throw new AuthenticationException(buildError());
    } catch (AuthenticationException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error("Error handling the authorization response", ex);
      throw new AuthenticationException(buildError());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private OAuth2AuthenticationProvider getConfig(HttpServletRequest request)
      throws AuthenticationException {
    return authStateHandler.getConfiguration(OAuth2AuthenticationProvider.class, getState(request));
  }

  /**
   * Retrieves the state parameter from the request
   *
   * @param request
   *          Request that might contain the state
   */
  protected String getState(HttpServletRequest request) {
    return request.getParameter("state");
  }

  private HttpRequest buildAccessTokenRequest(String code, String redirectURL,
      OAuth2AuthenticationProvider config) throws ServletException {
    //@formatter:off
    Map<String, String> params = Map.of("grant_type", "authorization_code",
                                        "code", code,
                                        "redirect_uri", redirectURL,
                                        "client_id", config.getClientID(),
                                        "client_secret", FormatUtilities.encryptDecrypt(config.getClientSecret(), false));
    //@formatter:on
    String requestBody = params.entrySet()
        .stream()
        .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
            + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));

    log.trace("Access token request parameters: {}", requestBody);

    return HttpRequest.newBuilder()
        .uri(URI.create(config.getAccessTokenURL()))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header("Accept", "application/json")
        .timeout(Duration.ofSeconds(30))
        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
        .build();
  }

  /**
   * Retrieves the ID of the authenticated {@link User}. By default this method assumes that the
   * provided response data contains an OpenID token which includes an email which is used to find
   * the authenticated user.
   *
   * @param responseData
   *          The data obtained in the response of the access token request
   * @param configuration
   *          the OAuth 2.0 configuration with information that can be used to verify the token like
   *          the URL to get the public keys required by the algorithm used for encrypting the token
   *          data.
   *
   * @return the {@link AuthenticatedUser} with the information of the authenticated {@link User}
   *
   * @throws JSONException
   *           If it is not possible to parse the response data as JSON or if the "id_token"
   *           property is not present in the response
   * @throws OAuth2TokenVerificationException
   *           If it is not possible to verify the token or extract the authentication data
   * @throws AuthenticationException
   *           If there is no user linked to the retrieved email
   */
  protected AuthenticatedUser getUser(String responseData,
      OAuth2AuthenticationProvider configuration)
      throws JSONException, OAuth2TokenVerificationException {
    JSONObject tokenData = new JSONObject(responseData);
    return getUser(tokenData, configuration);
  }

  private AuthenticatedUser getUser(JSONObject tokenData, String configId)
      throws JSONException, OAuth2TokenVerificationException {
    try {
      OBContext.setAdminMode(true);
      OAuth2AuthenticationProvider config = OBDal.getInstance()
          .get(OAuth2AuthenticationProvider.class, configId);
      return getUser(tokenData, config);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private AuthenticatedUser getUser(JSONObject tokenData,
      OAuth2AuthenticationProvider configuration)
      throws JSONException, OAuth2TokenVerificationException {
    String email = getEmail(tokenData, configuration);

    if (StringUtils.isBlank(email)) {
      throw new OAuth2TokenVerificationException("The user e-mail was not found");
    }

    //@formatter:off
    String hql = "select u.id as id, u.username as userName" +
                 "  from ADUser u" +
                 " where email = :email";
    //@formatter:on
    Tuple user = OBDal.getInstance()
        .getSession()
        .createQuery(hql, Tuple.class)
        .setParameter("email", email)
        .setMaxResults(1)
        .uniqueResult();

    if (user == null) {
      throw new AuthenticationException(buildError("UNKNOWN_EMAIL_AUTHENTICATION_FAILURE"));
    }

    return new AuthenticatedUser((String) user.get("id"), (String) user.get("userName"));
  }

  protected String getEmail(JSONObject tokenData, OAuth2AuthenticationProvider configuration)
      throws JSONException, OAuth2TokenVerificationException {
    String idToken = tokenData.getString("id_token");
    Map<String, Object> authData = jwtDataProvider.getData(idToken,
        configuration.getCertificateURL(), "email");
    return (String) authData.get("email");
  }

  private OBError buildError() {
    return buildError("EXTERNAL_AUTHENTICATION_FAILURE");
  }

  private OBError buildError(String message) {
    OBError errorMsg = new OBError();
    errorMsg.setType("Error");
    errorMsg.setTitle("AUTHENTICATION_FAILURE");
    errorMsg.setMessage(message);
    return errorMsg;
  }

  /**
   * Retrieves the standard URL where OAuth 2.0 requests coming from the external provided should be
   * redirected by using the information of the request in the {@link RequestContext}.
   *
   * @see #getRedirectURL(HttpServletRequest)
   *
   * @return the standard URL where OAuth 2.0 requests coming from the external provided should be
   *         redirected
   */
  protected String getRedirectURL() {
    return getRedirectURL(RequestContext.get().getRequest());
  }

  /**
   * Retrieves the standard URL where OAuth 2.0 requests coming from the external provided should be
   * redirected by using the information in the provided request.
   *
   * @param request
   *          the HTTP request
   * @return the standard URL where OAuth 2.0 requests coming from the external provided should be
   *         redirected
   */
  private String getRedirectURL(HttpServletRequest request) {
    return HttpBaseUtils.getLocalAddress(request) + DEFAULT_REDIRECT_PATH;
  }
}
