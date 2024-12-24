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
package org.openbravo.service.externalsystem.http;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openbravo.test.base.TestConstants.Orgs.MAIN;
import static org.openbravo.test.matchers.json.JSONMatchers.equal;
import static org.openbravo.test.matchers.json.JSONMatchers.matchesObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpRequest;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.domaintype.StringEnumerateDomainType;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.utility.Protocol;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.geography.Country;
import org.openbravo.service.externalsystem.ExternalSystem;
import org.openbravo.service.externalsystem.ExternalSystem.Operation;
import org.openbravo.service.externalsystem.ExternalSystemData;
import org.openbravo.service.externalsystem.ExternalSystemProvider;
import org.openbravo.service.externalsystem.ExternalSystemResponse;
import org.openbravo.service.externalsystem.ExternalSystemResponse.Type;
import org.openbravo.service.externalsystem.ExternalSystemResponseBuilder;
import org.openbravo.service.externalsystem.HttpExternalSystemData;
import org.openbravo.test.base.Issue;
import org.openbravo.test.base.TestConstants;
import org.openbravo.utils.FormatUtilities;

/**
 * Tests to cover the sending of data with {@link HttpExternalSystem}. Note: these tests expect to
 * have the server running as they execute HTTP requests and evaluate the responses.
 */
public class HttpExternalSystemCommunicationTest extends WeldBaseTest {

  @Inject
  private ExternalSystemProvider externalSystemProvider;

  private ExternalSystemData externalSystemData;
  private HttpExternalSystemData httpExternalSystemData;
  private ArgumentCaptor<Supplier<HttpRequest>> requestCaptor;

  @Before
  @SuppressWarnings("unchecked")
  public void init() {
    setTestAdminContext();
    setDefaultContext();
    createTestData();
    OBDal.getInstance().commitAndClose();

    externalSystemData = OBProvider.getInstance().get(ExternalSystemData.class);
    externalSystemData.setOrganization(OBDal.getInstance().getProxy(Organization.class, MAIN));
    externalSystemData.setName("Test");
    Protocol httpProtocol = OBDal.getInstance()
        .getProxy(Protocol.class, TestConstants.Protocols.HTTP);
    externalSystemData.setProtocol(httpProtocol);
    OBDal.getInstance().save(externalSystemData);
    httpExternalSystemData = OBProvider.getInstance().get(HttpExternalSystemData.class);
    httpExternalSystemData.setOrganization(OBDal.getInstance().getProxy(Organization.class, MAIN));
    httpExternalSystemData.setURL(getURL());
    httpExternalSystemData.setExternalSystem(externalSystemData);
    httpExternalSystemData.setActive(true);
    externalSystemData.getExternalSystemHttpList().add(httpExternalSystemData);
    OBDal.getInstance().save(httpExternalSystemData);

    // Add a new request method into the list reference, to avoid failing when checking if the
    // property value is valid after setting the "requestMethod" property value with it
    StringEnumerateDomainType domainType = (StringEnumerateDomainType) httpExternalSystemData
        .getEntity()
        .getProperty("requestMethod")
        .getDomainType();
    domainType.addEnumerateValue("TEST");

    requestCaptor = ArgumentCaptor.forClass(Supplier.class);
  }

  private void setDefaultContext() {
    try {
      OBContext.setAdminMode(false);
      User user = OBDal.getInstance().get(User.class, OBContext.getOBContext().getUser().getId());
      user.setDefaultRole(OBContext.getOBContext().getRole());
      user.setDefaultClient(OBContext.getOBContext().getCurrentClient());
      user.setDefaultOrganization(OBContext.getOBContext().getCurrentOrganization());
      user.setDefaultWarehouse(OBContext.getOBContext().getWarehouse());
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void createTestData() {
    Country newCountry = OBProvider.getInstance().get(Country.class);
    newCountry.setName("Wonderland");
    newCountry.setISOCountryCode("WL");
    newCountry.setAddressPrintFormat("-");
    OBDal.getInstance().save(newCountry);
  }

  private String getURL() {
    Properties props = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    String obURL = props.getProperty("context.url");
    if (StringUtils.isEmpty(obURL)) {
      throw new OBException("context.url is not set in Openbravo.properties");
    }
    return obURL + "/org.openbravo.service.json.jsonrest/Country";
  }

  @After
  public void cleanUp() {
    OBDal.getInstance().rollbackAndClose();
    deleteTestData();
  }

  private void deleteTestData() {
    OBDal.getInstance()
        .getSession()
        .createQuery("DELETE FROM Country WHERE iSOCountryCode IN ('WL', 'TE')")
        .executeUpdate();
    OBDal.getInstance().commitAndClose();
  }

  @Test
  public void sendWithBasicAuth() throws JSONException, ServletException {
    ExternalSystemResponse response = sendWithAuthorization("BASIC");

    assertResponse(response, ExternalSystemResponse.Type.SUCCESS, HttpServletResponse.SC_OK);
    assertThat("Expected Response Data", (JSONObject) response.getData(),
        equal(getExpectedResponseData()));
  }

  @Test
  @Issue("49159")
  public void sendWithBasicAuthAlwaysInHeader() throws JSONException, ServletException {
    ExternalSystemResponse response = sendWithAuthorization("BASIC_ALWAYS_HEADER");

    assertResponse(response, ExternalSystemResponse.Type.SUCCESS, HttpServletResponse.SC_OK);
    assertThat("Expected Response Data", (JSONObject) response.getData(),
        equal(getExpectedResponseData()));
  }

  private ExternalSystemResponse sendWithAuthorization(String authorizationType)
      throws JSONException, ServletException {
    return getExternalSystem(authorizationType).send(getRequestDataSupplier()).join();
  }

  @Test
  public void checkOAuth2TokenRetrieval() throws JSONException, ServletException {
    HttpExternalSystem externalSystem = getExternalSystem("OAUTH2");
    OAuth2AccessToken token1 = createAccessToken();
    OAuth2AccessToken token2 = createAccessToken();
    OAuth2AuthorizationProvider authProvider = getOAuth2AuthorizationProvider();
    doReturn(token1, token2).when(authProvider).requestAccessToken();
    externalSystem.setAuthorizationProvider(authProvider);

    doReturn(CompletableFuture.completedFuture(ExternalSystemResponseBuilder.newBuilder()
        .withData("")
        .withStatusCode(200)
        .withType(Type.SUCCESS)
        .build())).when(externalSystem).sendRequest(any());

    // first call, a new token is requested
    externalSystem.send(getRequestDataSupplier()).join();
    verifyRequestAuthorizationHeader(externalSystem, 1, token1.getAuthorization());

    // while the token is not expired it is reused
    externalSystem.send(getRequestDataSupplier()).join();
    verifyRequestAuthorizationHeader(externalSystem, 2, token1.getAuthorization());
    externalSystem.send(getRequestDataSupplier()).join();
    verifyRequestAuthorizationHeader(externalSystem, 3, token1.getAuthorization());

    // when the token expires, a new one is requested
    when(token1.isExpired()).thenReturn(true);
    externalSystem.send(getRequestDataSupplier()).join();
    verifyRequestAuthorizationHeader(externalSystem, 4, token2.getAuthorization());

    // two token requests have been done
    verify(authProvider, Mockito.times(2)).requestAccessToken();
  }

  @Test
  public void retryOnceWithOAuth2OnUnauthorizedError() throws JSONException, ServletException {
    HttpExternalSystem externalSystem = getExternalSystem("OAUTH2");
    OAuth2AccessToken token = createAccessToken();
    OAuth2AuthorizationProvider authProvider = getOAuth2AuthorizationProvider();
    doReturn(token).when(authProvider).requestAccessToken();
    externalSystem.setAuthorizationProvider(authProvider);

    // this request to the backoffice is going to fail with a unauthorized (401) error due to the
    // authorization that we are using. We take advantage of it to test that a request retry is
    // done when receiving that error status from the external system when using OAuth 2.0
    externalSystem.send(getRequestDataSupplier()).join();

    verify(authProvider, Mockito.times(1)).handleRequestRetry(401);
    verify(authProvider, Mockito.times(2)).requestAccessToken();
  }

  private OAuth2AuthorizationProvider getOAuth2AuthorizationProvider() {
    OAuth2AuthorizationProvider authProvider = new OAuth2AuthorizationProvider();
    authProvider.init(httpExternalSystemData);
    return spy(authProvider);
  }

  private OAuth2AccessToken createAccessToken() {
    return spy(new OAuth2AccessToken(SequenceIdData.getUUID(), 100));
  }

  @Test
  public void sendUnauthorized() throws JSONException {
    httpExternalSystemData.setAuthorizationType("NOAUTH");

    ExternalSystem externalSystem = externalSystemProvider.getExternalSystem(externalSystemData)
        .orElseThrow();
    ExternalSystemResponse response = externalSystem.send(getRequestDataSupplier()).join();

    assertResponse(response, ExternalSystemResponse.Type.ERROR,
        HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Test
  public void cannotSendWithUnsupportedRequestMethod() {
    OBException exceptionRule = assertThrows(OBException.class, () -> {
      httpExternalSystemData.setAuthorizationType("NOAUTH");
      httpExternalSystemData.setRequestMethod("TEST");
      ExternalSystem externalSystem = externalSystemProvider.getExternalSystem(externalSystemData)
          .orElseThrow();
      externalSystem.send(getRequestDataSupplier()).join();
    });

    assertThat(exceptionRule.getMessage(), containsString("Unsupported HTTP request method TEST"));
  }

  @Test
  public void sendRequestToUnknownResource() throws JSONException {
    httpExternalSystemData.setURL("http://localhost:8000/dummy");
    httpExternalSystemData.setAuthorizationType("NOAUTH");

    ExternalSystem externalSystem = externalSystemProvider.getExternalSystem(externalSystemData)
        .orElseThrow();
    ExternalSystemResponse response = externalSystem.send(getRequestDataSupplier()).join();

    assertResponse(response, ExternalSystemResponse.Type.ERROR, 0);
    assertThat("Expected Response Error", response.getError().toString(),
        startsWith("java.net.ConnectException"));
  }

  @Test
  @Issue("53077")
  public void sendWithDeleteOperation() throws ServletException {
    HttpExternalSystem externalSystem = getExternalSystem();
    String path = getCountry("WL").getId();

    ExternalSystemResponse response = externalSystem.send(Operation.DELETE, path).join();

    assertResponse(response, ExternalSystemResponse.Type.SUCCESS, HttpServletResponse.SC_OK);
    verifyRequestMethod(externalSystem, "DELETE");
  }

  @Test
  @Issue("53077")
  public void sendWithReadOperation() throws ServletException {
    HttpExternalSystem externalSystem = getExternalSystem();
    String path = getCountry("WL").getId();

    ExternalSystemResponse response = externalSystem.send(Operation.READ, path).join();

    assertResponse(response, ExternalSystemResponse.Type.SUCCESS, HttpServletResponse.SC_OK);
    verifyRequestMethod(externalSystem, "GET");
    assertThat("Expected Response Data", (JSONObject) response.getData(),
        matchesObject(new JSONObject(Map.of("iSOCountryCode", "WL"))));
  }

  @Test
  @Issue("53077")
  public void sendWithQueryParams() throws ServletException, JSONException {
    HttpExternalSystem externalSystem = getExternalSystem();

    ExternalSystemResponse response = externalSystem
        .send(Operation.READ, Map.of("queryParameters", Map.of("_where", "iSOCountryCode='WL'")))
        .join();

    assertResponse(response, ExternalSystemResponse.Type.SUCCESS, HttpServletResponse.SC_OK);
    verifyRequestMethod(externalSystem, "GET");
    JSONArray data = ((JSONObject) response.getData()).getJSONObject("response")
        .getJSONArray("data");
    assertThat("Expected Response Data", data.getJSONObject(0),
        matchesObject(new JSONObject(Map.of("iSOCountryCode", "WL"))));
  }

  @Test
  @Issue("53077")
  public void sendWithCreateOperation() throws ServletException, JSONException {
    HttpExternalSystem externalSystem = getExternalSystem();
    JSONObject requestData = new JSONObject();
    requestData.put("data",
        new JSONObject(Map.of("name", "Test", "iSOCountryCode", "TE", "addressPrintFormat", "-")));

    ExternalSystemResponse response = externalSystem
        .send(Operation.CREATE, getRequestDataSupplier(requestData))
        .join();

    assertResponse(response, ExternalSystemResponse.Type.SUCCESS, HttpServletResponse.SC_OK);
    verifyRequestMethod(externalSystem, "POST");
  }

  @Test
  @Issue("53077")
  public void sendWithUpdateOperation() throws ServletException, JSONException {
    HttpExternalSystem externalSystem = getExternalSystem();
    JSONObject requestData = new JSONObject();
    requestData.put("data",
        new JSONObject(Map.of("id", getCountry("WL").getId(), "description", "hi")));

    ExternalSystemResponse response = externalSystem
        .send(Operation.UPDATE, getRequestDataSupplier(requestData))
        .join();

    assertResponse(response, ExternalSystemResponse.Type.SUCCESS, HttpServletResponse.SC_OK);
    verifyRequestMethod(externalSystem, "PUT");
  }

  @Test
  @Issue("53077")
  public void cannotSendPayloadInGetRequests() {
    OBException exceptionRule = assertThrows(OBException.class,
        () -> getExternalSystem().send(Operation.READ, getRequestDataSupplier(new JSONObject()))
            .join());

    assertThat(exceptionRule.getMessage(), equalTo("GET requests do not accept a payload"));
  }

  @Test
  @Issue("53077")
  public void cannotSendPayloadInDeleteRequests() {
    OBException exceptionRule = assertThrows(OBException.class,
        () -> getExternalSystem().send(Operation.DELETE, getRequestDataSupplier(new JSONObject()))
            .join());

    assertThat(exceptionRule.getMessage(), equalTo("DELETE requests do not accept a payload"));
  }

  private HttpExternalSystem getExternalSystem() throws ServletException {
    return getExternalSystem("BASIC");
  }

  private HttpExternalSystem getExternalSystem(String authorizationType) throws ServletException {
    httpExternalSystemData.setAuthorizationType(authorizationType);
    if ("OAUTH2".equals(authorizationType)) {
      httpExternalSystemData.setOauth2AuthServerUrl("https://authServer.com");
      httpExternalSystemData.setOauth2ClientIdentifier("1234");
      httpExternalSystemData.setOauth2ClientSecret(FormatUtilities.encryptDecrypt("abcd", true));
    } else {
      httpExternalSystemData.setUsername("Openbravo");
      httpExternalSystemData.setPassword(FormatUtilities.encryptDecrypt("openbravo", true));
    }

    ExternalSystem externalSystem = externalSystemProvider.getExternalSystem(externalSystemData)
        .orElseThrow();
    return Mockito.spy((HttpExternalSystem) externalSystem);
  }

  private Country getCountry(String isoCode) {
    return (Country) OBDal.getInstance()
        .createCriteria(Country.class)
        .add(Restrictions.eq(Country.PROPERTY_ISOCOUNTRYCODE, isoCode))
        .setFilterOnActive(false)
        .setMaxResults(1)
        .uniqueResult();
  }

  private Supplier<InputStream> getRequestDataSupplier() throws JSONException {
    JSONObject requestData = new JSONObject();
    requestData.put("data", new JSONArray());
    return getRequestDataSupplier(requestData);
  }

  private Supplier<InputStream> getRequestDataSupplier(JSONObject requestData) {
    return () -> new ByteArrayInputStream(requestData.toString().getBytes());
  }

  private void assertResponse(ExternalSystemResponse response, ExternalSystemResponse.Type type,
      int statusCode) {
    assertThat("Has Expected Response Type", response.getType(), equalTo(type));
    assertThat("Expected Response Status Code", response.getStatusCode(), equalTo(statusCode));
  }

  private void verifyRequestMethod(HttpExternalSystem externalSystemSpy, String method) {
    verify(externalSystemSpy, Mockito.times(1)).sendRequest(requestCaptor.capture());
    HttpRequest request = requestCaptor.getValue().get();

    assertThat("Expected Request Method", request.method(), equalTo(method));
  }

  private void verifyRequestAuthorizationHeader(HttpExternalSystem externalSystemSpy, int times,
      String authorization) {
    verify(externalSystemSpy, Mockito.times(times)).sendRequest(requestCaptor.capture());
    HttpRequest request = requestCaptor.getValue().get();

    assertThat("Expected Request Authorization Header",
        request.headers().map().get("Authorization").get(0), equalTo(authorization));
  }

  private JSONObject getExpectedResponseData() throws JSONException {
    JSONObject expectedResponse = new JSONObject();
    JSONObject response = new JSONObject();
    response.put("status", 0);
    response.put("data", new JSONArray());
    expectedResponse.put("response", response);
    return expectedResponse;
  }
}
