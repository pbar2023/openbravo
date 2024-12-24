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
 * All portions are Copyright (C) 2014-2024 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.datasource;

import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.service.datasource.DataSourceServiceProvider;

import junit.framework.TestCase;

/**
 * Utility methods to deal with datasource calls.
 *
 * @author alostale
 *
 */
public class DatasourceTestUtil {
  private static final Logger log = LogManager.getLogger();
  private static final String CONTEXT_PROPERTY = "context.url";

  /** Creates a connection to a given URL without processing it. */
  public static HttpURLConnection createConnection(String url, String wsPart, String method,
      String cookie) throws Exception {

    String completeUrl = url + wsPart;
    log.debug("Create conntection URL: {}, method {}", completeUrl, method);
    final URL connUrl = new URI(completeUrl).toURL();
    final HttpURLConnection hc = (HttpURLConnection) connUrl.openConnection();
    hc.setRequestMethod(method);
    hc.setAllowUserInteraction(false);
    hc.setDefaultUseCaches(false);
    hc.setDoOutput(true);
    hc.setDoInput(true);
    hc.setInstanceFollowRedirects(true);
    hc.setUseCaches(false);
    if (cookie != null) {
      hc.setRequestProperty("Cookie", cookie);
    }
    return hc;
  }

  /**
   * Fetch data with an HQL data source
   *
   * @see #fetch(String, Map)
   */
  public static JSONObject fetchWithHQLDataSource(Map<String, String> additionalParams)
      throws JSONException {
    String result = fetch("3C1148C0AB604DE1B51B7EA4112C325F", additionalParams);
    return new JSONObject(result);
  }

  /**
   * Fetch data with a data source
   *
   * @param dataSourceId
   *          The data source identifier
   * @param additionalParams
   *          Parameters to include in the data source fetch call
   *
   * @return the result of fetching data with the data source
   */
  public static String fetch(String dataSourceId, Map<String, String> additionalParams) {
    Map<String, String> params = new HashMap<>();
    params.put("dataSourceName", dataSourceId);
    params.put("_startRow", "0");
    params.put("_endRow", "99");
    params.put("_noCount", "true");
    params.putAll(additionalParams);

    String result = WeldUtils.getInstanceFromStaticBeanManager(DataSourceServiceProvider.class)
        .getDataSource(dataSourceId)
        .fetch(params);
    log.debug("Fetch result: {}", result);

    return result;
  }

  static String authenticate(String openbravoURL, String user, String password) throws Exception {
    final HttpURLConnection hc = DatasourceTestUtil.createConnection(openbravoURL,
        "/secureApp/LoginHandler.html", "POST", null);
    final OutputStream os = hc.getOutputStream();
    String content = "user=" + user + "&password=" + password;
    os.write(content.getBytes("UTF-8"));
    os.flush();
    os.close();
    hc.connect();

    return hc.getHeaderField("Set-Cookie");
  }

  static String request(String openbravoURL, String wsPart, String method, String content,
      String cookie, int expectedResponse) throws Exception {
    return request(openbravoURL, wsPart, method, content, cookie, expectedResponse, null);
  }

  public static String request(String openbravoURL, String wsPart, String method, String content,
      String cookie, int expectedResponse, String contentType) throws Exception {
    final HttpURLConnection hc = createConnection(openbravoURL, wsPart, method, cookie);
    if (contentType != null) {
      hc.setRequestProperty("Content-Type", contentType);
    }
    if (!"DELETE".equals(method)) {
      final OutputStream os = hc.getOutputStream();
      os.write(content.getBytes("UTF-8"));
      os.flush();
      os.close();
    }
    hc.connect();
    TestCase.assertEquals(expectedResponse, hc.getResponseCode());

    if (expectedResponse == 500) {
      // no content available anyway
      return "";
    }

    StringWriter writer = new StringWriter();
    IOUtils.copy(hc.getInputStream(), writer, "utf-8");
    return writer.toString();
  }

  static String getParamsContent(Map<String, String> params) {
    String content = "";
    for (String key : params.keySet()) {
      if (!content.isEmpty()) {
        content += "&";
      }
      content += key + "=" + params.get(key);
    }
    return content;
  }

  static String getOpenbravoURL() {
    Properties props = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    String obURL = props.getProperty(CONTEXT_PROPERTY);
    if (StringUtils.isEmpty(obURL)) {
      throw new OBException(CONTEXT_PROPERTY + " is not set in Openbravo.properties");
    }
    log.debug("got OB context: {}", obURL);

    return obURL;
  }

  static void changeProfile(String openbravoURL, String cookie, String csrfToken, String roleId,
      String langId, String orgId, String warehouseId) throws Exception {
    JSONObject newProfile = new JSONObject();
    newProfile.put("language", langId);
    newProfile.put("organization", orgId);
    newProfile.put("role", roleId);
    newProfile.put("warehouse", warehouseId);
    newProfile.put("csrfToken", csrfToken);
    request(openbravoURL,
        "/org.openbravo.client.kernel?command=save&_action=org.openbravo.client.application.navigationbarcomponents.UserInfoWidgetActionHandler",
        "POST", newProfile.toString(), cookie, 200, "application/json");
  }
}
