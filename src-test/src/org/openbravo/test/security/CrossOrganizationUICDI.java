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
 * All portions are Copyright (C) 2017-2022 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.security;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.assetmgmt.Asset;
import org.openbravo.service.datasource.BaseDataSourceService;
import org.openbravo.service.datasource.DataSourceService;
import org.openbravo.service.datasource.DataSourceServiceProvider;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.test.base.mock.HttpServletRequestMock;
import org.openbravo.userinterface.selector.CustomQuerySelectorDatasource;
import org.openbravo.userinterface.selector.SelectorConstants;

/**
 * Tests to ensure references that require of CDI (Standard Selector) don't apply organization
 * filter when applied in a field that allows cross organization references and apply them if it
 * does not allow it.
 */
public class CrossOrganizationUICDI extends WeldBaseTest {

  private static final String ORDER_ASSET_COLUMN = "1E2CDC6A59BF4277B0E0A5EA45332EE9";
  private static final String ORDER_BP_COLUMN = "2762";

  private static final List<String> COLUMNS_TO_ALLOW_CROSS_ORG = Arrays.asList(ORDER_ASSET_COLUMN,
      ORDER_BP_COLUMN);

  @Inject
  private DataSourceServiceProvider dataSourceServiceProvider;

  @Inject
  private CustomQuerySelectorDatasource customQuerySelectorDatasource;

  /** defines the values the parameter will take. */
  @Rule
  public ParameterCdiTestRule<Boolean> parameterValuesRule = new ParameterCdiTestRule<>(
      Arrays.asList(true, false));

  /** this field will take the values defined by parameterValuesRule field. */
  private @ParameterCdiTest Boolean useCrossOrgColumns;

  @Test
  public void stdSelectorShouldShowNonReferenceableOrgsIfAllowed() throws Exception {
    HttpServletRequestMock.setRequestMockInRequestContext();
    final DataSourceService dataSource = dataSourceServiceProvider.getDataSource(Asset.ENTITY_NAME);

    @SuppressWarnings("serial")
    String r = dataSource.fetch(new HashMap<String, String>() {
      {
        put(JsonConstants.STARTROW_PARAMETER, "0");
        put(JsonConstants.ENDROW_PARAMETER, "75");
        put(JsonConstants.NOCOUNT_PARAMETER, "true");
        put(SelectorConstants.DS_REQUEST_SELECTOR_ID_PARAMETER, "862F54CB1B074513BD791C6789F4AA42");
        put(JsonConstants.ORG_PARAMETER, TEST_ORG_ID);
        put("inpTableId", "259");
        put("targetProperty", "asset");
      }
    });

    List<String> values = new ArrayList<>();
    JSONObject o = new JSONObject(r);
    JSONArray data = o.getJSONObject("response").getJSONArray("data");
    for (int i = 0; i < data.length(); i++) {
      JSONObject row = data.getJSONObject(i);
      values.add(row.getString("_identifier"));
    }

    if (useCrossOrgColumns) {
      assertThat("Asset selector allowing cross org", values, hasItems("Coche", "Car"));
    } else {
      assertThat("Asset selector not allowing cross org", values,
          allOf(hasItem("Coche"), not(hasItem("Car"))));
    }
  }

  @Test
  public void customQuerySelectorAlwaysShowReferenceableOrgs() throws Exception {
    List<String> rows = getSelectorValues();
    assertThat(rows, hasItem("Bebidas Alegres, S.L."));
  }

  @Test
  public void customQuerySelectorShouldShowNonReferenceableOrgsIfAllowed() throws Exception {
    List<String> rows = getSelectorValues();
    if (useCrossOrgColumns) {
      assertThat(rows, hasItem("Be Soft Drinker, Inc."));
    } else {
      assertThat(rows, not(hasItem("Be Soft Drinker, Inc.")));
    }
  }

  private List<String> getSelectorValues() throws JSONException {
    HttpServletRequestMock.setRequestMockInRequestContext();
    BaseDataSourceService selectorDatasorce = customQuerySelectorDatasource;
    @SuppressWarnings("serial")
    String r = selectorDatasorce.fetch(new HashMap<String, String>() {
      {
        put(JsonConstants.STARTROW_PARAMETER, "0");
        put(JsonConstants.ENDROW_PARAMETER, "75");
        put(JsonConstants.NOCOUNT_PARAMETER, "true");
        put(SelectorConstants.DS_REQUEST_SELECTOR_ID_PARAMETER, "F132874BE0954A9B8C1301BE20704730");
        put(JsonConstants.ORG_PARAMETER, TEST_ORG_ID);
        put("inpTableId", "259");
        put("targetProperty", "businessPartner");
      }
    });

    List<String> values = new ArrayList<>();
    JSONObject o = new JSONObject(r);
    JSONArray data = o.getJSONObject("response").getJSONArray("data");
    for (int i = 0; i < data.length(); i++) {
      JSONObject row = data.getJSONObject(i);
      values.add(row.getString("name"));
    }
    return values;
  }

  @Before
  public void setUpAllowedCrossOrg() throws Exception {
    CrossOrganizationReference.setUpAllowedCrossOrg(COLUMNS_TO_ALLOW_CROSS_ORG, useCrossOrgColumns);
    setTestUserContext();
  }

  @AfterClass
  public static void resetAllowedCrossOrg() throws Exception {
    CrossOrganizationReference.setUpAllowedCrossOrg(COLUMNS_TO_ALLOW_CROSS_ORG, false);
    OBDal.getInstance().commitAndClose();
  }
}
