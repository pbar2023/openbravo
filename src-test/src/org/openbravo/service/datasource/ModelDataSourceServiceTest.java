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
 * All portions are Copyright (C) 2022 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.datasource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.ProductCharacteristicTestUtils;
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.json.AdditionalPropertyResolver;
import org.openbravo.test.base.TestConstants;

/**
 * Test cases for the {@link ModelDataSourceService} class
 */
public class ModelDataSourceServiceTest extends WeldBaseTest {
  @Before
  public void initialize() {
    // create an special property that will be retrieved through an AdditionalPropertyResolver
    ProductCharacteristicTestUtils.addRelevantCharacteristic("M_Test", "Test",
        TestConstants.Modules.ID_CORE);
    ProductCharacteristicTestUtils.reloadRelevantCharacteristicsCache();
    setSystemAdministratorContext();
  }

  @After
  public void cleanUp() {
    OBDal.getInstance().rollbackAndClose();
  }

  @Test
  public void fetchAllProperties() throws JSONException {
    ModelDataSourceService datasource = WeldUtils
        .getInstanceFromStaticBeanManager(ModelDataSourceService.class);
    Map<String, String> parameters = getRequestParameters(TestConstants.Tables.M_PRODUCT, null);

    String result = datasource.fetch(parameters);

    String[] expectedProperties = getProductProperties(p -> true, p -> p);
    assertThat(getPropertiesFromResponse(result), contains(expectedProperties));
  }

  @Test
  public void fetchAllPropertiesNavigatingFromOtherEntity() throws JSONException {
    ModelDataSourceService datasource = WeldUtils
        .getInstanceFromStaticBeanManager(ModelDataSourceService.class);
    Map<String, String> parameters = getRequestParameters(TestConstants.Tables.C_ORDERLINE,
        "product.");

    String result = datasource.fetch(parameters);

    String[] expectedProperties = getProductProperties(p -> true, p -> "product." + p);
    assertThat(getPropertiesFromResponse(result), contains(expectedProperties));
  }

  @Test
  public void fetchMatchingProperties() throws JSONException {
    ModelDataSourceService datasource = WeldUtils
        .getInstanceFromStaticBeanManager(ModelDataSourceService.class);
    Map<String, String> parameters = getRequestParameters(TestConstants.Tables.M_PRODUCT, "a");

    String result = datasource.fetch(parameters);

    String[] expectedProperties = getProductProperties(p -> p.startsWith("a"), p -> p);
    assertThat(getPropertiesFromResponse(result), contains(expectedProperties));
  }

  @Test
  public void fetchMatchingPropertiesNavigatingFromOtherEntity() throws JSONException {
    ModelDataSourceService datasource = WeldUtils
        .getInstanceFromStaticBeanManager(ModelDataSourceService.class);
    Map<String, String> parameters = getRequestParameters(TestConstants.Tables.C_ORDERLINE,
        "product.a");

    String result = datasource.fetch(parameters);

    String[] expectedProperties = getProductProperties(p -> p.startsWith("a"), p -> "product." + p);
    assertThat(getPropertiesFromResponse(result), contains(expectedProperties));
  }

  @Test
  public void fetchProductPropertyNavigatingFromOtherEntity() throws JSONException {
    ModelDataSourceService datasource = WeldUtils
        .getInstanceFromStaticBeanManager(ModelDataSourceService.class);
    Map<String, String> parameters = getRequestParameters(TestConstants.Tables.C_ORDERLINE,
        "product");

    String result = datasource.fetch(parameters);

    assertThat(getPropertiesFromResponse(result), contains("product"));
  }

  @Test
  public void fetchAdditionalProperty() throws JSONException {
    ModelDataSourceService datasource = WeldUtils
        .getInstanceFromStaticBeanManager(ModelDataSourceService.class);
    Map<String, String> parameters = getRequestParameters(TestConstants.Tables.M_PRODUCT, "mtes");

    String result = datasource.fetch(parameters);

    assertThat(getPropertiesFromResponse(result), contains("mTest"));
  }

  @Test
  public void fetchAdditionalPropertyNavigatingFromOtherEntity() throws JSONException {
    ModelDataSourceService datasource = WeldUtils
        .getInstanceFromStaticBeanManager(ModelDataSourceService.class);
    Map<String, String> parameters = getRequestParameters(TestConstants.Tables.C_ORDERLINE,
        "product.mtes");

    String result = datasource.fetch(parameters);

    assertThat(getPropertiesFromResponse(result), contains("product.mTest"));
  }

  private Map<String, String> getRequestParameters(String tableId, String filterValue) {
    Map<String, String> parameters = new HashMap<>();
    parameters.put("inpadTableId", tableId);
    parameters.put("_constructor", "AdvancedCriteria");
    parameters.put("_OrExpression", "true");
    //@formatter:off
    parameters.put("criteria",
        "{\"fieldName\":\"_dummy\",\"operator\":\"equals\",\"value\":1663665066321}" +
        (filterValue == null ? "" : "__;__{\"fieldName\":\"property\",\"operator\":\"iContains\",\"value\":\""+ filterValue +"\"}"));
    //@formatter:on
    return parameters;
  }

  private List<String> getPropertiesFromResponse(String response) throws JSONException {
    JSONArray data = new JSONObject(response).getJSONObject("response").getJSONArray("data");
    return IntStream.range(0, data.length()).mapToObj(i -> {
      try {
        return data.getJSONObject(i).getString("property");
      } catch (JSONException ex) {
        throw new OBException("Unexpected data received from datasource", ex);
      }
    }).collect(Collectors.toList());
  }

  private String[] getProductProperties(Predicate<String> filter, UnaryOperator<String> mapper) {
    Entity product = ModelProvider.getInstance().getEntity(Product.class);
    List<String> properties = product.getProperties()
        .stream()
        .map(Property::getName)
        .collect(Collectors.toList());
    Set<String> additionalProperties = WeldUtils
        .getInstancesSortedByPriority(AdditionalPropertyResolver.class)
        .stream()
        .map(resolver -> resolver.getPropertyNames(product))
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
    properties.add("_identifier");
    properties.addAll(additionalProperties);
    return properties.stream().filter(filter).map(mapper).sorted().toArray(String[]::new);
  }
}
