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
package org.openbravo.service.json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.openbravo.base.weld.test.WeldBaseTest;

/**
 * Test cases for the {@link AdvancedQueryBuilder} class
 */
public class AdvancedQueryBuilderTest extends WeldBaseTest {
  private static final String PRODUCT_ENTITY = "Product";

  @Test
  public void buildSimpleQuery() throws JSONException {
    AdvancedQueryBuilder queryBuilder = createAdvancedQueryBuilder(getEmptyCriteria(),
        "searchKey,id");

    assertThat(queryBuilder.getJoinClause(), equalTo(" as e  "));
    assertThat(queryBuilder.getWhereClause(), equalTo(" "));
    assertThat(queryBuilder.getOrderByClause(), equalTo(" order by e.searchKey,e.id"));
  }

  @Test
  public void buildQueryWithFilter() throws JSONException {
    String productCategoryId = "0C20B3F7AB234915B2239FCD8BE10CD1";
    AdvancedQueryBuilder queryBuilder = createAdvancedQueryBuilder(
        getFilterBySingleFieldCriteria("productCategory", productCategoryId), "searchKey,id");

    assertThat(queryBuilder.getJoinClause(), equalTo(" as e  "));
    assertThat(queryBuilder.getWhereClause(), equalTo(" where (e.productCategory.id = :alias_0) "));
    assertThat(queryBuilder.getOrderByClause(), equalTo(" order by e.searchKey,e.id"));
    assertThat(queryBuilder.getNamedParameters().keySet(), contains("alias_0"));
    assertThat(queryBuilder.getNamedParameters().values(), contains(productCategoryId));
  }

  private AdvancedQueryBuilder createAdvancedQueryBuilder(JSONObject criteria, String orderBy) {
    AdvancedQueryBuilder queryBuilder = new AdvancedQueryBuilder();
    queryBuilder.setEntity(PRODUCT_ENTITY);
    queryBuilder.setMainAlias(JsonConstants.MAIN_ALIAS);
    queryBuilder.setCriteria(criteria);
    queryBuilder.setOrderBy(orderBy);
    return queryBuilder;
  }

  private JSONObject getEmptyCriteria() throws JSONException {
    JSONObject criteria = new JSONObject();
    criteria.put("operator", "and");
    criteria.put("_constructor", "AdvancedCriteria");
    criteria.put("criteria", new JSONArray());
    return criteria;
  }

  private JSONObject getFilterBySingleFieldCriteria(String fieldName, String value)
      throws JSONException {
    JSONObject criteria = new JSONObject();
    criteria.put("fieldName", fieldName);
    criteria.put("operator", "equals");
    criteria.put("value", value);
    criteria.put("_constructor", "AdvancedCriteria");
    criteria.put("criteria", new JSONArray());
    return criteria;
  }
}
