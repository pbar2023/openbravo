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
 * All portions are Copyright (C) 2023 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.base;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Properties;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.weld.test.WeldBaseTest;

/**
 * Covers the usage of the {@link HttpClientManager}. Note: this class executes HTTP requests so it
 * is expected to have the server running when executing this test.
 */
public class HttpClientManagerTest extends WeldBaseTest {

  @Inject
  private HttpClientManager httpClientManager;

  @Test
  public void executeHttpRequest() throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(getURL()))
        .header("Content-Type", "application/json")
        .header("Authorization",
            "Basic " + Base64.getEncoder()
                .encodeToString(("Openbravo:openbravo").getBytes(StandardCharsets.ISO_8859_1)))
        .timeout(Duration.ofSeconds(30))
        .GET()
        .build();
    HttpResponse<String> response = httpClientManager.send(request);

    assertThat("A response is received", response.statusCode(),
        anyOf(equalTo(HttpServletResponse.SC_OK), equalTo(HttpServletResponse.SC_UNAUTHORIZED)));
  }

  private String getURL() {
    Properties props = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    String obURL = props.getProperty("context.url");
    if (StringUtils.isEmpty(obURL)) {
      throw new OBException("context.url is not set in Openbravo.properties");
    }
    return obURL + "/org.openbravo.service.json.jsonrest/Country";
  }
}
