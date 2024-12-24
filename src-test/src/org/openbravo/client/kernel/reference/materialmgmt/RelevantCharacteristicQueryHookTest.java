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
package org.openbravo.client.kernel.reference.materialmgmt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.ProductCharacteristicTestUtils;
import org.openbravo.service.json.AdvancedQueryBuilder;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.test.base.TestConstants;

/**
 * Test cases for covering the query modifications done by the
 * {@link RelevantCharacteristicQueryHook} hook when building a query with the
 * {@link AdvancedQueryBuilder}
 */
public class RelevantCharacteristicQueryHookTest extends WeldBaseTest {
  private static final String PRODUCT_ENTITY = "Product";
  private static final String PRODUCT_PRICE_ENTITY = "PricingProductPrice";
  private static final String PRODUCT_CATEGORY_ID = "0C20B3F7AB234915B2239FCD8BE10CD1";
  private static final String CHARACTERISTIC_ID = "2A5B402016FE443F8F8C54722A69C77B";

  private String characteristicId1;
  private String characteristicId2;

  @Before
  public void initialize() {
    ProductCharacteristicTestUtils.addRelevantCharacteristic("M_Test1", "Test1",
        TestConstants.Modules.ID_CORE);
    ProductCharacteristicTestUtils.addRelevantCharacteristic("M_Test2", "Test2",
        TestConstants.Modules.ID_CORE);
    characteristicId1 = ProductCharacteristicTestUtils
        .createCharacteristicLinkedToRelevant("Char1", "M_Test1")
        .getId();
    characteristicId2 = ProductCharacteristicTestUtils
        .createCharacteristicLinkedToRelevant("Char2", "M_Test2")
        .getId();
    OBDal.getInstance().flush();
    ProductCharacteristicTestUtils.reloadRelevantCharacteristicsCache();
  }

  @After
  public void cleanUp() {
    OBDal.getInstance().rollbackAndClose();
  }

  @Test
  public void addQueryJoins() throws JSONException {
    AdvancedQueryBuilder queryBuilder = createAdvancedQueryBuilder(PRODUCT_ENTITY,
        getFilterByProductCategoryCriteria(), "searchKey,id", List.of("mTest1"));

    //@formatter:off
    assertThat(queryBuilder.getJoinClause(), equalTo(
        " as e " +
        " left join e.productCharacteristicValueList as join_0 with join_0.characteristic.id = :alias_0" +
        " left join join_0.characteristicValue as join_1" +
        " left join ADTreeNode as join_2 on join_2.node = join_1.id "));
    //@formatter:on
    assertThat(queryBuilder.getWhereClause(), equalTo(" where (e.productCategory.id = :alias_1) "));
    assertThat(queryBuilder.getOrderByClause(), equalTo(" order by e.searchKey,e.id"));
    Map<String, Object> namedParameters = queryBuilder.getNamedParameters();
    assertThat(namedParameters.get("alias_0"), equalTo(characteristicId1));
    assertThat(namedParameters.get("alias_1"), equalTo(PRODUCT_CATEGORY_ID));
  }

  @Test
  public void addQueryJoinsWithMultipleRelevantCharacteristics() throws JSONException {
    AdvancedQueryBuilder queryBuilder = createAdvancedQueryBuilder(PRODUCT_ENTITY,
        getFilterByProductCategoryCriteria(), "searchKey,id", List.of("mTest1", "mTest2"));

    //@formatter:off
    assertThat(queryBuilder.getJoinClause(), equalTo(
        " as e " +
        " left join e.productCharacteristicValueList as join_0 with join_0.characteristic.id = :alias_0" +
        " left join join_0.characteristicValue as join_1" +
        " left join ADTreeNode as join_2 on join_2.node = join_1.id" +
        " left join e.productCharacteristicValueList as join_3 with join_3.characteristic.id = :alias_1" +
        " left join join_3.characteristicValue as join_4" +
        " left join ADTreeNode as join_5 on join_5.node = join_4.id "));
    //@formatter:on
    assertThat(queryBuilder.getWhereClause(), equalTo(" where (e.productCategory.id = :alias_2) "));
    assertThat(queryBuilder.getOrderByClause(), equalTo(" order by e.searchKey,e.id"));
    Map<String, Object> namedParameters = queryBuilder.getNamedParameters();
    assertThat(namedParameters.get("alias_0"), equalTo(characteristicId1));
    assertThat(namedParameters.get("alias_1"), equalTo(characteristicId2));
    assertThat(namedParameters.get("alias_2"), equalTo(PRODUCT_CATEGORY_ID));
  }

  @Test
  public void addQueryJoinsWithNonProductAsMainEntity() throws JSONException {
    AdvancedQueryBuilder queryBuilder = createAdvancedQueryBuilder(PRODUCT_PRICE_ENTITY,
        getEmptyCriteria(), "standardPrice,id", List.of("product.mTest1"));

    //@formatter:off
    assertThat(queryBuilder.getJoinClause(), equalTo(
        " as e " +
        " left join e.product as join_0" +
        " left join join_0.productCharacteristicValueList as join_1 with join_1.characteristic.id = :alias_0" +
        " left join join_1.characteristicValue as join_2" +
        " left join ADTreeNode as join_3 on join_3.node = join_2.id "));
    //@formatter:on
    assertThat(queryBuilder.getWhereClause(), equalTo(" "));
    assertThat(queryBuilder.getOrderByClause(), equalTo(" order by e.standardPrice,e.id"));
    assertThat(queryBuilder.getNamedParameters().get("alias_0"), equalTo(characteristicId1));
  }

  @Test
  public void setAllQueryClauses() throws JSONException {
    AdvancedQueryBuilder queryBuilder = createAdvancedQueryBuilder(PRODUCT_ENTITY,
        getFilterByRelevantCharacteristicCriteria("mTest1"), "mTest1$_identifier,id",
        List.of("mTest1"));

    //@formatter:off
    assertThat(queryBuilder.getJoinClause(), equalTo(
        " as e " +
        " left join e.productCharacteristicValueList as join_0 with join_0.characteristic.id = :alias_0" +
        " left join join_0.characteristicValue as join_1" +
        " left join ADTreeNode as join_2 on join_2.node = join_1.id "));
    //@formatter:on
    assertThat(queryBuilder.getWhereClause(), equalTo(" where (( join_1.id = :alias_1 )) "));
    assertThat(queryBuilder.getOrderByClause(), equalTo(" order by join_2.sequenceNumber,join_1.name,e.id"));
    Map<String, Object> namedParameters = queryBuilder.getNamedParameters();
    assertThat(namedParameters.get("alias_0"), equalTo(characteristicId1));
    assertThat(namedParameters.get("alias_1"), equalTo(CHARACTERISTIC_ID));
  }

  @Test
  public void buildQueryDoNotFailIfRelevantCharacteristicIsNotLinked() throws JSONException {
    ProductCharacteristicTestUtils.unlinkRelevantCharacteristic(characteristicId1);
    OBDal.getInstance().flush();

    AdvancedQueryBuilder queryBuilder = createAdvancedQueryBuilder(PRODUCT_ENTITY,
        getEmptyCriteria(), "mTest1$_identifier,id", List.of("mTest1"));

    assertThat(queryBuilder.getJoinClause(), equalTo(" as e  "));
    assertThat(queryBuilder.getWhereClause(), equalTo(" "));
    assertThat(queryBuilder.getOrderByClause(), equalTo(" order by e.id"));
  }

  private AdvancedQueryBuilder createAdvancedQueryBuilder(String entity, JSONObject criteria,
      String orderBy, List<String> additionalProperties) {
    AdvancedQueryBuilder queryBuilder = new AdvancedQueryBuilder();
    queryBuilder.setEntity(entity);
    queryBuilder.setMainAlias(JsonConstants.MAIN_ALIAS);
    queryBuilder.setCriteria(criteria);
    queryBuilder.setOrderBy(orderBy);
    queryBuilder.setAdditionalProperties(additionalProperties);
    return queryBuilder;
  }

  private JSONObject getEmptyCriteria() throws JSONException {
    JSONObject criteria = new JSONObject();
    criteria.put("operator", "and");
    criteria.put("_constructor", "AdvancedCriteria");
    criteria.put("criteria", new JSONArray());
    return criteria;
  }

  private JSONObject getFilterByProductCategoryCriteria() throws JSONException {
    JSONObject criteria = new JSONObject();
    criteria.put("fieldName", "productCategory");
    criteria.put("operator", "equals");
    criteria.put("value", PRODUCT_CATEGORY_ID);
    criteria.put("_constructor", "AdvancedCriteria");
    criteria.put("criteria", new JSONArray());
    return criteria;
  }

  private JSONObject getFilterByRelevantCharacteristicCriteria(String fieldName)
      throws JSONException {
    JSONObject criterion = new JSONObject();
    criterion.put("fieldName", fieldName);
    criterion.put("operator", "equals");
    criterion.put("value", CHARACTERISTIC_ID);
    criterion.put("_constructor", "AdvancedCriteria");
    JSONArray criteriaDefinition = new JSONArray();
    criteriaDefinition.put(criterion);
    JSONObject criteria = new JSONObject();
    criteria.put("operator", "and");
    criteria.put("_constructor", "AdvancedCriteria");
    criteria.put("criteria", criteriaDefinition);
    return criteria;
  }
}
