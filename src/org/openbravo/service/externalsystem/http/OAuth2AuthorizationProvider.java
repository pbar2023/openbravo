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
package org.openbravo.service.externalsystem.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.service.externalsystem.ExternalSystemConfigurationError;
import org.openbravo.service.externalsystem.HttpExternalSystemData;
import org.openbravo.utils.FormatUtilities;

/**
 * Used to authenticate an HTTP request using the OAuth 2.0 standard
 */
@HttpAuthorizationMethod("OAUTH2")
public class OAuth2AuthorizationProvider
    implements HttpAuthorizationProvider, HttpAuthorizationRequestHeaderProvider {

  private static final Logger log = LogManager.getLogger();
  private static final int TIMEOUT = 10;
  private static final int ONE_HOUR = 3600;

  private String clientId;
  private String clientSecret;
  private String authServerURL;
  private HttpClient httpClient;
  private OAuth2AccessToken accessToken;
  private boolean isCacheableToken;

  @Override
  public void init(HttpExternalSystemData configuration) {
    clientId = configuration.getOauth2ClientIdentifier();
    authServerURL = configuration.getOauth2AuthServerUrl();
    try {
      clientSecret = FormatUtilities.encryptDecrypt(configuration.getOauth2ClientSecret(), false);
    } catch (ServletException ex) {
      log.error("Error decrypting OAuth2 Client Secret of HTTP configuration {}",
          configuration.getId());
      throw new ExternalSystemConfigurationError(
          "Error decrypting OAuth2 Client Secret of HTTP configuration " + configuration.getId());
    }
    httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(TIMEOUT)).build();
    isCacheableToken = true;
    accessToken = null;
  }

  /**
   * Initializes the required information based on the provided configuration
   *
   * @param configuration
   *          A BaseOBObject containing the following properties: <br>
   *          - id: the ID of the external system. Used for logging purposes. <br>
   *          - oauth2AuthServerUrl: the URL of the authorization server that serves the tokens <br>
   *          - oauth2ClientIdentifier: the ID of the OAuth2 client <br>
   *          - oauth2ClientSecret: the OAuth2 client secret encrypted and decryptable using
   *          {@link FormatUtilities#encryptDecrypt(String, boolean)}
   *
   * @throws ExternalSystemConfigurationError
   *           if there is an error decrypting the provided client secret
   */
  @Override
  public void init(BaseOBObject configuration) {
    clientId = (String) configuration.get("oauth2ClientIdentifier");
    authServerURL = (String) configuration.get("oauth2AuthServerUrl");
    try {
      clientSecret = FormatUtilities
          .encryptDecrypt((String) configuration.get("oauth2ClientSecret"), false);
    } catch (ServletException ex) {
      log.error("Error decrypting OAuth2 Client Secret of HTTP configuration {}",
          configuration.get("id"));
      throw new ExternalSystemConfigurationError(
          "Error decrypting OAuth2 Client Secret of HTTP configuration " + configuration.get("id"));
    }
    httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(TIMEOUT)).build();
    isCacheableToken = false;
    accessToken = null;
  }

  @Override
  public Map<String, Object> getAuthorizationData() {
    return Map.of("expiresIn", accessToken.getExpiresIn());
  }

  @Override
  public Map<String, String> getHeaders() {
    if (!isCacheableToken || accessToken == null || accessToken.isExpired()) {
      accessToken = requestAccessToken();
    }
    return Map.of("Authorization", accessToken.getAuthorization());
  }

  /** Internal API, this method is not private only because of testing purposes */
  OAuth2AccessToken requestAccessToken() {
    String credentials = clientId + ":" + clientSecret;

    HttpRequest authRequest = HttpRequest.newBuilder()
        .uri(URI.create(authServerURL))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header("Authorization",
            "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes()))
        .timeout(Duration.ofSeconds(TIMEOUT))
        .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
        .build();

    HttpResponse<String> response;
    try {
      response = httpClient.send(authRequest, HttpResponse.BodyHandlers.ofString());
    } catch (IOException ex) {
      throw new OAuth2AuthorizationError("Request to retrieve the access token failed", ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new OAuth2AuthorizationError("Request to retrieve the access token interrupted", ex);
    }

    if (!isSuccessfulResponse(response)) {
      throw new OAuth2AuthorizationError(
          "Authorization server returned a " + response.statusCode() + " error response");
    }

    try {
      JSONObject tokenData = new JSONObject(response.body());
      String tokenType = tokenData.optString("token_type", OAuth2AccessToken.BEARER);
      if (!OAuth2AccessToken.BEARER.equalsIgnoreCase(tokenType)) {
        throw new OAuth2AuthorizationError("Unsupported access token type " + tokenType);
      }
      String tokenValue = tokenData.getString("access_token");
      int expiresIn = tokenData.optInt("expires_in", ONE_HOUR);
      return new OAuth2AccessToken(tokenValue, expiresIn);
    } catch (JSONException ex) {
      throw new OAuth2AuthorizationError("Could not extract access token data", ex);
    }
  }

  private boolean isSuccessfulResponse(HttpResponse<String> response) {
    return response.statusCode() >= 200 && response.statusCode() <= 299;
  }

  @Override
  public boolean handleRequestRetry(int responseStatusCode) {
    if (responseStatusCode == 401) {
      // Reset the access token, we'll retrieve a new one in the next request
      accessToken = null;
      return true;
    }
    return false;
  }
}
