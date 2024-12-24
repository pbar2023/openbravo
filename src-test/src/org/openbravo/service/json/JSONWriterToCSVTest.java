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
 * All portions are Copyright (C) 2022-2023 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.service.json;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.report.ReportingUtils.ExportType;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.test.base.mock.HttpServletRequestMock;

/**
 * Tests the {@link JSONWriterToCSV} class
 */

public class JSONWriterToCSVTest extends WeldBaseTest {

  private static final String TMP_DIR = System.getProperty("java.io.tmpdir");
  private static final String CSV_SEPARATOR = "\",\"";

  private Path tmpFileAbsolutePath;

  @Before
  public void initialize() {
    setTestUserContext();
    RequestContext.get().setRequest(new HttpServletRequestMock());
    RequestContext.get().setSessionAttribute("#DecimalSeparator|generalQtyEdition", ".");
  }

  @After
  public void cleanUp() throws IOException {
    if (tmpFileAbsolutePath != null && Files.exists(tmpFileAbsolutePath)) {
      Files.delete(tmpFileAbsolutePath);
    }
  }

  @Test
  public void writeJsonObjectWithCustomFields()
      throws JSONException, IOException, PropertyException {
    final HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    final Map<String, String> params = getRequestParameters();

    final DataToJsonConverter toJsonConverter = OBProvider.getInstance()
        .get(DataToJsonConverter.class);
    toJsonConverter.setAdditionalProperties(JsonUtils.getAdditionalProperties(params));
    toJsonConverter.setSelectedProperties(params.get(JsonConstants.SELECTEDPROPERTIES_PARAMETER));

    Entity entity = ModelProvider.getInstance()
        .getEntity(params.get(JsonConstants.DATASOURCE_NAME), false);
    File fileReport = new File(TMP_DIR,
        UUID.randomUUID().toString() + "." + ExportType.CSV.getExtension());

    Organization org = OBDal.getInstance().get(Organization.class, TEST_ORG_ID);
    final JSONObject json = toJsonConverter.toJsonObject(org, DataResolvingMode.FULL);
    json.put("Custom Field 1", "Test 1");
    json.put("Custom Field 2", "Test 2");

    try (PrintWriter printWriter = new PrintWriter(fileReport)) {
      JSONWriterToCSV writer = new JSONWriterToCSV(printWriter, vars, params, entity);
      writer.write(json);
      writer.writeCSVFooterNote(params);
    }

    tmpFileAbsolutePath = Paths.get(fileReport.getAbsolutePath());

    final String fileContent = Files.readString(tmpFileAbsolutePath);

    assertThat("Expected content of the CSV file", fileContent, equalTo(getExpectedResult(org)));
  }

  private Map<String, String> getRequestParameters() {
    Map<String, String> params = new HashMap<>();
    int offset = Calendar.getInstance().get(Calendar.ZONE_OFFSET)
        + Calendar.getInstance().get(Calendar.DST_OFFSET);
    params.put(JsonConstants.UTCOFFSETMILISECONDS_PARAMETER, offset + "");
    params.put(JsonConstants.DATASOURCE_NAME, "Organization");
    params.put(JsonConstants.SELECTEDPROPERTIES_PARAMETER,
        "searchKey,name,summaryLevel,organizationType,legalEntityOrganization,creationDate,createdBy");
    params.put(JsonConstants.FIELDNAMES_PARAMETER,
        new JSONArray().put("searchKey")
            .put("name")
            .put("summaryLevel")
            .put("organizationType")
            .put("legalEntityOrganization")
            .put("creationDate")
            .put("createdBy")
            .put("Custom Field 1")
            .put("Custom Field 2")
            .toString());
    return params;
  }

  private String getExpectedResult(Organization org) {
    return "\"Search Key\",\"Name\",\"Summary Level\",\"Organization Type\",\"Legal Entity Organization\",\"Creation Date\",\"Created By\",\"Custom Field 1\",\"Custom Field 2\"\n"
        + "\"" + org.getSearchKey() + CSV_SEPARATOR + org.getName() + CSV_SEPARATOR
        + org.isSummaryLevel() + CSV_SEPARATOR + org.getOrganizationType().getIdentifier()
        + CSV_SEPARATOR + org.getLegalEntityOrganization().getIdentifier() + CSV_SEPARATOR
        + formatDateInDefaultTimeZone(org.getCreationDate()) + CSV_SEPARATOR
        + org.getCreatedBy().getIdentifier() + "\",\"Test 1\",\"Test 2\"";
  }

  private String formatDateInDefaultTimeZone(Date date) {
    ZonedDateTime zonedDateTime = date.toInstant().atZone(ZoneId.systemDefault());
    return OBDateUtils.formatDateTime(Date.from(zonedDateTime.toInstant()));
  }
}
