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
 * All portions are Copyright (C) 2011-2023 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.userinterface.selector;

import static org.openbravo.userinterface.selector.SelectorConstants.includeOrgFilter;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Hibernate;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.domaintype.BigDecimalDomainType;
import org.openbravo.base.model.domaintype.BooleanDomainType;
import org.openbravo.base.model.domaintype.DateDomainType;
import org.openbravo.base.model.domaintype.DomainType;
import org.openbravo.base.model.domaintype.ForeignKeyDomainType;
import org.openbravo.base.model.domaintype.LongDomainType;
import org.openbravo.base.model.domaintype.StringEnumerateDomainType;
import org.openbravo.base.model.domaintype.UniqueIdDomainType;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.IdentifierProvider;
import org.openbravo.client.application.ParameterUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.model.ad.domain.ReferencedTable;
import org.openbravo.service.datasource.DataSourceUtils;
import org.openbravo.service.datasource.ReadOnlyDataSourceService;
import org.openbravo.service.datasource.hql.HqlQueryTransformer;
import org.openbravo.service.json.AdvancedQueryBuilder;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonUtils;

public class CustomQuerySelectorDatasource extends ReadOnlyDataSourceService {

  private static Logger log = LogManager.getLogger();
  private static final String ADDITIONAL_FILTERS = "@additional_filters@";
  public static final String ALIAS_PREFIX = "alias_";

  @Inject
  @Any
  private Instance<HqlQueryTransformer> hqlQueryTransformers;

  @Override
  protected int getCount(Map<String, String> parameters) {
    // we return -1, so that the super class calculates a valid count
    return -1;
  }

  @Override
  protected List<Map<String, Object>> getData(Map<String, String> parameters, int startRow,
      int endRow) {
    // Creation of formats is done here because they are not thread safe
    final SimpleDateFormat xmlDateFormat = JsonUtils.createDateFormat();
    final SimpleDateFormat xmlDateTimeFormat = JsonUtils.createDateTimeFormat();
    final List<Object> typedParameters = new ArrayList<>();
    final Map<String, Object> namedParameters = new HashMap<>();

    // Defaulted to endRow + 2 to check for more records while scrolling.
    int totalRows = endRow + 2;
    int rowCount = 0;

    String selectorId = parameters.get(SelectorConstants.DS_REQUEST_SELECTOR_ID_PARAMETER);

    if (StringUtils.isEmpty(selectorId)) {
      return new ArrayList<>();
    }

    OBContext.setAdminMode();

    List<Map<String, Object>> result = null;
    try {
      Selector selector = OBDal.getInstance().get(Selector.class, selectorId);
      List<SelectorField> fields = OBDao.getActiveOBObjectList(selector,
          Selector.PROPERTY_OBUISELSELECTORFIELDLIST);

      // Forcing object initialization to prevent LazyInitializationException in case session is
      // cleared when number of records is big enough
      Hibernate.initialize(fields);

      // Parse the hql in case that optional filters are required
      String hql = parseOptionalFilters(parameters, selector, xmlDateFormat, typedParameters,
          namedParameters);

      String sortBy = parameters.get("_sortBy");
      String distinct = parameters.get(JsonConstants.DISTINCT_PARAMETER);

      if (distinct != null) {
        SelectorField selectorField = getSelectorFieldFromColumnAlias(selector, distinct);
        if (selectorField != null && selectorField.getReference().getParentReference() != null
            && selectorField.getReference().getParentReference().getName().equals("Table")) {
          String[] distinctCriteria = getDistinctCriteria(parameters, distinct);
          result = filterByDistinctEntity(hql, namedParameters, typedParameters, selectorField,
              startRow, endRow, distinctCriteria);
        } else {
          result = getSelectorResults(hql, sortBy, selector, namedParameters, typedParameters,
              startRow, endRow, rowCount, fields, xmlDateFormat, xmlDateTimeFormat);
        }
      } else {
        String criteriaParameter = parameters.get("criteria");
        if (criteriaParameter != null) {
          JSONObject criteria;
          try {
            criteria = new JSONObject(criteriaParameter);
            String fieldName = criteria.getString("fieldName").split("[^a-zA-Z]")[0];

            SelectorField selectorField = getSelectorFieldFromColumnAlias(selector, fieldName);
            if (selectorField != null && selectorField.getReference().getParentReference() != null
                && selectorField.getReference().getParentReference().getName().equals("Table")) {
              hql = getFilteredHQL(hql, sortBy, criteria, selector, selectorField);
            }
          } catch (JSONException e) {
            // Ignore this case
          }
        }
        result = getSelectorResults(hql, sortBy, selector, namedParameters, typedParameters,
            startRow, endRow, rowCount, fields, xmlDateFormat, xmlDateTimeFormat);
      }

      if ("true".equals(parameters.get(JsonConstants.NOCOUNT_PARAMETER)) && startRow < endRow) {
        if (rowCount < endRow) {
          totalRows = rowCount;
        }
        parameters.put(JsonConstants.RESPONSE_TOTALROWS, String.valueOf(totalRows));
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  @SuppressWarnings("all")
  private List<Map<String, Object>> getSelectorResults(String hql, String sortBy, Selector selector,
      Map<String, Object> namedParameters, List<Object> typedParameters, int startRow, int endRow,
      int rowCount, List<SelectorField> fields, SimpleDateFormat xmlDateFormat,
      SimpleDateFormat xmlDateTimeFormat) {
    hql += getSortClause(sortBy, selector, hql);
    List<Map<String, Object>> result = new ArrayList<>();
    Query<Tuple> selQuery = OBDal.getInstance().getSession().createQuery(hql, Tuple.class);

    setQueryParameters(selQuery, namedParameters, typedParameters, startRow, endRow);
    for (Tuple tuple : selQuery.list()) {
      rowCount++;
      final Map<String, Object> data = new LinkedHashMap<>();
      for (SelectorField field : fields) {
        // TODO: throw an exception if the display expression doesn't match any returned alias.
        for (TupleElement<?> tupleElement : tuple.getElements()) {
          String alias = tupleElement.getAlias();
          if (alias != null && alias.equals(field.getDisplayColumnAlias())) {
            Object value = tuple.get(alias);
            if (value instanceof Date) {
              value = xmlDateFormat.format(value);
            }
            if (value instanceof Timestamp) {
              value = xmlDateTimeFormat.format(value);
              value = JsonUtils.convertToCorrectXSDFormat((String) value);
            }
            data.put(alias, value);
          }
        }
      }
      result.add(data);
    }
    return result;
  }

  private void setQueryParameters(Query<Tuple> selQuery, Map<String, Object> namedParameters,
      List<Object> typedParameters, int startRow, int endRow) {
    selQuery.setParameterList("clients", (String[]) namedParameters.get("clients"));
    if (namedParameters.containsKey("orgs")) {
      selQuery.setParameterList("orgs", (String[]) namedParameters.get("orgs"));
    }
    if (namedParameters.containsKey("distinctCriteriaValue")) {
      selQuery.setParameter("distinctCriteriaValue",
          (String) namedParameters.get("distinctCriteriaValue"));
    }

    for (int i = 0; i < typedParameters.size(); i++) {
      selQuery.setParameter(ALIAS_PREFIX + Integer.toString(i), typedParameters.get(i));
    }

    if (startRow > 0) {
      selQuery.setFirstResult(startRow);
    }
    if (endRow > startRow) {
      selQuery.setMaxResults(endRow - startRow + 1);
    }
  }

  private String getFilteredHQL(String hql, String sortBy, JSONObject criteria, Selector selector,
      SelectorField selectorField) {
    String[] clauseLeftParts = selectorField.getClauseLeftPart().split("\\.", 2);
    String entityAlias = clauseLeftParts[0];
    String property = getPropertyFromClauseLeftParts(clauseLeftParts);

    String filter = null;
    try {
      if (criteria.getString("operator").equals("equals")) {
        filter = " and " + entityAlias + property + "='" + criteria.optString("value") + "'";
      } else if (criteria.getString("operator").equals("or")) {
        JSONArray criterias = criteria.getJSONArray("criteria");
        String values = "'" + criterias.getJSONObject(0).optString("value") + "'";
        for (int i = 1; i < criterias.length(); i++) {
          values += ", '" + criterias.getJSONObject(i).optString("value") + "'";
        }
        filter = " and " + entityAlias + property + " in (" + values + ")";
      } else {
        Entity entity = getEntityFromSelectorField(selectorField);
        String identifierPropertyName = "." + entity.getIdentifierProperties().get(0).getName();
        filter = " and lower(" + entityAlias + property + identifierPropertyName + ") like '%"
            + criteria.optString("value").toLowerCase() + "%'";
      }
    } catch (JSONException e) {
      // Ignore this case
    }

    return hql + filter;
  }

  private List<Map<String, Object>> filterByDistinctEntity(String hql,
      Map<String, Object> namedParameters, List<Object> typedParameters,
      SelectorField selectorField, int startRow, int endRow, String[] distinctCriteria) {
    Entity entity = getEntityFromSelectorField(selectorField);
    String entityName = entity.getName();
    String[] clauseLeftParts = selectorField.getClauseLeftPart().split("\\.", 2);
    String entityAlias = clauseLeftParts[0];
    String property = getPropertyFromClauseLeftParts(clauseLeftParts);

    String[] hqlParts = hql.split("(?i)from", 2);
    String hqlSelect = "select " + entityAlias + property;
    String fromAndWhereClause = hqlParts[1];
    String hqlSubquery = hqlSelect + " from " + fromAndWhereClause;

    List<Map<String, Object>> result = new ArrayList<>();

    //@formatter:off
    String hqlDistinctQuery =
        "select distinct e " +
        "from " + entityName + " e " +
        "where e in (" + hqlSubquery + ") " + getFilteredDistinctWhereClause(entity, selectorField, distinctCriteria, typedParameters);
    //@formatter:on
    Query<Tuple> distinctQuery = OBDal.getInstance()
        .getSession()
        .createQuery(hqlDistinctQuery, Tuple.class);

    setQueryParameters(distinctQuery, namedParameters, typedParameters, startRow, endRow);

    for (Tuple tuple : distinctQuery.list()) {
      Map<String, Object> record = new HashMap<>();
      BaseOBObject tupleObject = (BaseOBObject) tuple.get(0);
      if (tupleObject == null) {
        break;
      }
      record.put(JsonConstants.ID, tupleObject.getId());
      record.put(JsonConstants.IDENTIFIER,
          IdentifierProvider.getInstance().getIdentifier(tupleObject));
      result.add(record);
    }
    return result;
  }

  // if the user is including some text in the selector filter, take it into account
  private String getFilteredDistinctWhereClause(Entity entity, SelectorField selectorField,
      String[] distinctCriteria, List<Object> typedParameters) {
    if (distinctCriteria == null || distinctCriteria.length == 0) {
      return "";
    }
    String firstIdentifierProperty = getNameOfFirstIdentifierProperty(entity);
    String operator = distinctCriteria[0];
    String value = distinctCriteria[1];
    return " AND "
        + getTextWhereClause(operator, value, "e." + firstIdentifierProperty, typedParameters);
  }

  private String getNameOfFirstIdentifierProperty(Entity entity) {
    String propertyName = "";
    if (!entity.getIdentifierProperties().isEmpty()) {
      propertyName = entity.getIdentifierProperties().get(0).getName();
    }
    return propertyName;
  }

  private SelectorField getSelectorFieldFromColumnAlias(Selector selector, String columnAlias) {
    return selector.getOBUISELSelectorFieldList()
        .stream()
        .filter(selectorField -> selectorField.getDisplayColumnAlias().equals(columnAlias))
        .findFirst()
        .orElse(null);
  }

  private Entity getEntityFromSelectorField(SelectorField selectorField) {
    List<ReferencedTable> referencedTableList = selectorField.getReference()
        .getADReferencedTableList();
    if (!referencedTableList.isEmpty()) {
      return ModelProvider.getInstance()
          .getEntityByTableId(referencedTableList.get(0).getTable().getId());
    } else {
      throw new OBException("Error while getting entity from selector field: "
          + "This functionality is only available when using Table references.");
    }

  }

  private String getPropertyFromClauseLeftParts(String[] clauseLeftParts) {
    String property = "";

    if (clauseLeftParts.length > 1) {
      property = "." + clauseLeftParts[1];
    }

    return property;
  }

  /**
   * Returns the selectors HQL query. In case that it contains the '@additional_filters@' String it
   * is replaced by a set of filter clauses.
   * 
   * These include a filter clause:
   * <ul>
   * <li>for the main entity's client by the context's client.</li>
   * <li>for the main entity's organization by an organization list see
   * {@link DataSourceUtils#getOrgs(String)}</li>
   * <li>with Selector's default filter expression.</li>
   * <li>for each default expression defined on the selector fields.</li>
   * <li>for each selector field in case exists a value for it on the parameters param.</li>
   * </ul>
   * 
   * @param parameters
   *          Map of String values with the request parameters.
   * @param selector
   *          the selector that it is being retrieved the data.
   * @param xmlDateFormat
   *          SimpleDataFormat to be used to parse date Strings.
   * @return a String with the HQL to be executed.
   */

  public String parseOptionalFilters(Map<String, String> parameters, Selector selector,
      SimpleDateFormat xmlDateFormat, List<Object> typedParameters) {
    return parseOptionalFilters(parameters, selector, xmlDateFormat, typedParameters,
        new HashMap<>());
  }

  /**
   * Returns the selectors HQL query. In case that it contains the '@additional_filters@' String it
   * is replaced by a set of filter clauses.
   *
   * These include a filter clause:
   * <ul>
   * <li>for the main entity's client by the context's client.</li>
   * <li>for the main entity's organization by an organization list see
   * {@link DataSourceUtils#getOrgs(String)}</li>
   * <li>with Selector's default filter expression.</li>
   * <li>for each default expression defined on the selector fields.</li>
   * <li>for each selector field in case exists a value for it on the parameters param.</li>
   * </ul>
   *
   * @param parameters
   *          Map of String values with the request parameters.
   * @param selector
   *          the selector that it is being retrieved the data.
   * @param xmlDateFormat
   *          SimpleDataFormat to be used to parse date Strings.
   * @param typedParameters
   *          Typed parameters to be used in the query
   * @param namedParameters
   *          Named parameters to be used in the query
   * @return a String with the HQL to be executed.
   */

  public String parseOptionalFilters(Map<String, String> parameters, Selector selector,
      SimpleDateFormat xmlDateFormat, List<Object> typedParameters,
      Map<String, Object> namedParameters) {
    String hql = selector.getHQL();

    if (hql.contains(ADDITIONAL_FILTERS)) {
      final String requestType = parameters.get(SelectorConstants.DS_REQUEST_TYPE_PARAMETER);
      final String entityAlias = selector.getEntityAlias();
      // Client filter
      String additionalFilter = entityAlias + ".client.id in :clients";
      final String[] clients = { "0", OBContext.getOBContext().getCurrentClient().getId() };
      namedParameters.put("clients", clients);
      if (includeOrgFilter(parameters)) {
        // Organization filter
        boolean isOrgSelector = selector.getTable().getName().equals("Organization");
        String orgs;
        if (isOrgSelector) {
          // Just retrieve the list of readable organizations in the current context
          orgs = DataSourceUtils.getOrgs(parameters.get(""));
        } else {
          orgs = DataSourceUtils.getOrgs(parameters.get(JsonConstants.ORG_PARAMETER));
        }
        if (StringUtils.isNotEmpty(orgs)) {
          additionalFilter += " and " + entityAlias;
          if (isOrgSelector) {
            additionalFilter += ".id in :orgs";
          } else {
            additionalFilter += ".organization.id in :orgs";
          }
          namedParameters.put("orgs", orgs.replaceAll("'", "").split(","));
        }
      }
      additionalFilter += getDefaultFilterExpression(selector, parameters);

      String defaultExpressionsFilter = "";
      boolean hasFilter = false;
      List<SelectorField> fields = OBDao.getActiveOBObjectList(selector,
          Selector.PROPERTY_OBUISELSELECTORFIELDLIST);
      HashMap<String, String[]> criteria = getCriteria(parameters);
      for (SelectorField field : fields) {
        if (StringUtils.isEmpty(field.getClauseLeftPart())) {
          continue;
        }
        String operator = null;
        String value = null;
        String[] operatorvalue = null;
        if (criteria != null) {
          operatorvalue = criteria.get(field.getDisplayColumnAlias());
          if (operatorvalue != null) {
            operator = operatorvalue[0];
            value = operatorvalue[1];
          }
        }
        if (StringUtils.isEmpty(value)) {
          value = parameters.get(field.getDisplayColumnAlias());
        }
        // Add field default expression on picklist if it is not already filtered. Default
        // expressions
        // on selector popup are already evaluated and their values came in the parameters object.
        if (field.getDefaultExpression() != null && !"Window".equals(requestType)
            && StringUtils.isEmpty(value)) {
          try {
            String defaultValue = "";
            Object defaultValueObject = ParameterUtils.getJSExpressionResult(parameters,
                RequestContext.get().getSession(), field.getDefaultExpression());
            if (defaultValueObject != null) {
              defaultValue = defaultValueObject.toString();
            }
            if (StringUtils.isNotEmpty(defaultValue)) {
              defaultExpressionsFilter += " and " + getWhereClause(operator, defaultValue, field,
                  xmlDateFormat, operatorvalue, typedParameters);
            }
          } catch (Exception e) {
            log.error("Error evaluating filter expression: " + e.getMessage(), e);
          }
        }
        if (field.isFilterable() && StringUtils.isNotEmpty(value)) {
          String whereClause = getWhereClause(operator, value, field, xmlDateFormat, operatorvalue,
              typedParameters);
          if (!hasFilter) {
            additionalFilter += " and (";
            hasFilter = true;
          } else {
            if ("Window".equals(requestType)) {
              additionalFilter += " and ";
            } else {
              additionalFilter += " or ";
            }
          }
          additionalFilter += whereClause;
        }
      }
      if (hasFilter) {
        additionalFilter += ")";
      }
      if (defaultExpressionsFilter.length() > 0) {
        additionalFilter += defaultExpressionsFilter;
      }
      hql = hql.replace(ADDITIONAL_FILTERS, additionalFilter);
    }
    // if there is any HQL Query transformer defined for this selector, use it to transform the
    // query
    hql = HqlQueryTransformer.transFormQuery(hql, namedParameters, parameters, selector,
        hqlQueryTransformers);

    return hql;
  }

  /**
   * Returns the where clause of a selector's field based on the given value.
   * 
   * This method based on the DomainType of the selector field returns the filter clause using the
   * clause left part defined on the selector field.
   * <ul>
   * <li>Numeric Domain Type: Returns an equals clause <i>field.clauseLeftPart = value</i></li>
   * <li>Date Domain Type: Returns a multiple clause comparing separately value's day, month and
   * year.</li>
   * <li>Boolean Domain Type: Returns an equals clause <i>field.clauseLeftPart = value</i></li>
   * <li>Foreign Key Domain Type: Returns an equals clause <i>field.clauseLeftPart.id =
   * value</i></li>
   * <li>Unique Id Domain Type: Returns an equals clause <i>field.clauseLeftPart = value</i></li>
   * <li>String Domain Type: Compares the clause left part with the value using the upper database
   * function which to make comparison case insensitive.
   * </ul>
   * 
   * @param operator
   *          String with the operator of the filter expression to be applied on the selector
   *          field's column.
   * @param value
   *          String with the value that the selector field's column is filtered by.
   * @param field
   *          The SelectorField that is filtered.
   * @param xmlDateFormat
   *          SimpleDateFormat to parse the value in case the field is a Date field.
   * @param operatorvalue
   *          a String array containing the filtering criteria.
   * @param typedParameters
   *          a list of Strings that will be populated with the parameters of the query.
   * @return a String with the HQL where clause to filter the field by the given value.
   */
  private String getWhereClause(String operator, String value, SelectorField field,
      SimpleDateFormat xmlDateFormat, String[] operatorvalue, List<Object> typedParameters) {
    String whereClause = "";

    if (operator != null && operator.equals(AdvancedQueryBuilder.EXISTS_QUERY_KEY)) {
      String val = "";
      for (int i = 1; i < operatorvalue.length; i++) {
        val += i > 1 ? " and " : "";
        val += operatorvalue[i];
      }
      return val;
    }

    DomainType domainType = ModelProvider.getInstance()
        .getReference(field.getReference().getId())
        .getDomainType();
    if (domainType.getClass().getSuperclass().equals(BigDecimalDomainType.class)
        || domainType.getClass().equals(LongDomainType.class)) {
      whereClause = field.getClauseLeftPart() + AdvancedQueryBuilder.getHqlOperator(operator)
          + getTypedParameterAlias(typedParameters, new BigDecimal(value));
    } else if (domainType.getClass().equals(DateDomainType.class)) {
      try {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(xmlDateFormat.parse(value));
        whereClause = " (day(" + field.getClauseLeftPart() + ") = "
            + getTypedParameterAlias(typedParameters, cal.get(Calendar.DATE));
        whereClause += "\n and month(" + field.getClauseLeftPart() + ") = "
            + getTypedParameterAlias(typedParameters, cal.get(Calendar.MONTH) + 1);
        whereClause += "\n and year(" + field.getClauseLeftPart() + ") = "
            + getTypedParameterAlias(typedParameters, cal.get(Calendar.YEAR)) + ") ";
      } catch (Exception e) {
        // ignore these errors, just don't filter then
        // add a dummy whereclause to make the query format correct
        whereClause = "1 = 1";
      }
    } else if (domainType instanceof BooleanDomainType) {
      whereClause = field.getClauseLeftPart() + " = "
          + getTypedParameterAlias(typedParameters, Boolean.valueOf(value));
    } else if (domainType instanceof UniqueIdDomainType) {
      whereClause = field.getClauseLeftPart() + " = "
          + getTypedParameterAlias(typedParameters, value);
    } else if (domainType instanceof ForeignKeyDomainType) {
      // Assume left part definition is full object reference from HQL select
      whereClause = field.getClauseLeftPart() + ".id = "
          + getTypedParameterAlias(typedParameters, value);
    } else if (domainType instanceof StringEnumerateDomainType) {
      // For enumerations value can be in two formats:
      // 1- VAL: in this case the expression should be property='VAL'
      // 2- ["VAL1", "VAL2"] (JSONArray): the expression should be property in ('VAL1', 'VAL2')
      JSONArray values = null;
      if (value.startsWith("[")) {
        try {
          values = new JSONArray(value);
        } catch (JSONException ignore) {
          // It is not a JSONArray: assuming format 1
        }
      }

      if (values == null) {
        // format 1
        whereClause = field.getClauseLeftPart() + " = "
            + getTypedParameterAlias(typedParameters, value);
      } else {
        // format 2
        whereClause = field.getClauseLeftPart() + " IN (";
        for (int i = 0; i < values.length(); i++) {
          if (i > 0) {
            whereClause += ", ";
          }
          try {
            whereClause += getTypedParameterAlias(typedParameters, values.getString(i));
          } catch (JSONException e) {
            log.error("Error parsing values as JSONArray:" + value, e);
          }
        }
        whereClause += ")";
      }
    } else {
      whereClause = getTextWhereClause(operator, value, field.getClauseLeftPart(), typedParameters);
    }
    return whereClause;
  }

  private String getTextWhereClause(String operator, String value, String leftPart,
      List<Object> typedParameters) {
    String whereClause;
    if ("iStartsWith".equals(operator)) {
      whereClause = "upper(" + leftPart + ") LIKE upper("
          + getTypedParameterAlias(typedParameters, value.replaceAll(" ", "%") + "%") + ")";
    } else if ("iEquals".equals(operator)) {
      whereClause = "upper(" + leftPart + ") = upper("
          + getTypedParameterAlias(typedParameters, value) + ")";
    } else {
      whereClause = "upper(" + leftPart + ") LIKE upper("
          + getTypedParameterAlias(typedParameters, "%" + value.replaceAll(" ", "%") + "%") + ")";
    }
    return whereClause;
  }

  /**
   * Generates the HQL Sort By Clause to append to the query being executed. If no sort options is
   * set on the sortBy parameter the result is ordered by the first shown grid's column.
   * 
   * @param sortBy
   *          String of grid's field names concatenated by JsonConstants.IN_PARAMETER_SEPARATOR.
   * @param selector
   *          the selector that it is being displayed.
   * @return a String with the HQL Sort By clause.
   */
  private String getSortClause(String sortBy, Selector selector, String hql) {
    StringBuffer sortByClause = new StringBuffer();
    boolean sortByDesc = false;
    if (sortBy != null && sortBy.startsWith("-")) {
      sortByDesc = true;
    }
    // If grid is manually filtered sortBy is not empty
    if (StringUtils.isNotEmpty(sortBy)) {
      if (sortBy.contains(JsonConstants.IN_PARAMETER_SEPARATOR)) {
        final String[] fieldNames = sortBy.split(JsonConstants.IN_PARAMETER_SEPARATOR);
        for (String fieldName : fieldNames) {
          if (sortByDesc) {
            fieldName = fieldName.substring(1, fieldName.length());
          }
          int fieldSortIndex = getFieldSortIndex(fieldName, hql);
          if (fieldSortIndex > 0) {
            if (sortByClause.length() > 0) {
              sortByClause.append(", ");
            }
            if (sortByDesc) {
              sortByClause.append(fieldSortIndex + " desc");
            } else {
              sortByClause.append(fieldSortIndex);
            }
          }
        }
      } else {
        String fieldName = null;
        if (sortByDesc) {
          fieldName = sortBy.substring(1, sortBy.length());
        } else {
          fieldName = sortBy;
        }
        int fieldSortIndex = getFieldSortIndex(fieldName, hql);
        if (fieldSortIndex > 0) {
          if (sortByDesc) {
            sortByClause.append(fieldSortIndex + " desc");
          } else {
            sortByClause.append(fieldSortIndex);
          }
        }
      }
    }

    // If sortByClause is empty set default sort options.
    if (sortByClause.length() == 0) {
      OBCriteria<SelectorField> selFieldsCrit = OBDao.getFilteredCriteria(SelectorField.class,
          Restrictions.eq(SelectorField.PROPERTY_OBUISELSELECTOR, selector),
          Restrictions.eq(SelectorField.PROPERTY_SHOWINGRID, true));
      selFieldsCrit.addOrderBy(SelectorField.PROPERTY_SORTNO, true);
      for (SelectorField selField : selFieldsCrit.list()) {
        int fieldSortIndex = getFieldSortIndex(selField.getDisplayColumnAlias(), hql);
        if (fieldSortIndex > 0) {
          sortByClause.append(fieldSortIndex + ", ");
        }
      }
      // Delete last 2 characters: ", "
      if (sortByClause.length() > 0) {
        sortByClause.delete(sortByClause.length() - 2, sortByClause.length() - 1);
      }
    }
    String result = "";
    if (sortByClause.length() > 0) {
      result = "\n ORDER BY " + sortByClause.toString();
    }

    return result;
  }

  /**
   * Given a Selector object and the request parameters it evaluates the Filter Expression in case
   * that it is defined and returns the result.
   * 
   * @param selector
   *          The Selector that it is being used.
   * @param parameters
   *          parameters used for this request.
   * @return a String with the evaluated JavaScript filter expression in case it is defined.
   */
  private String getDefaultFilterExpression(Selector selector, Map<String, String> parameters) {
    if ((selector.getFilterExpression() == null || selector.getFilterExpression().equals(""))) {
      // Nothing to filter
      return "";
    }

    Object result = null;
    try {
      result = ParameterUtils.getJSExpressionResult(parameters, RequestContext.get().getSession(),
          selector.getFilterExpression());
    } catch (Exception e) {
      log.error("Error evaluating filter expression: " + e.getMessage(), e);
    }
    if (result != null && !result.toString().equals("")) {
      return " and " + "(" + result.toString() + ")";
    }

    return "";
  }

  /**
   * Based on the given field name it gets the HQL query column related to it and returns its index.
   * 
   * @param fieldName
   *          Grid's field name or display alias of the related selector field it is desired to
   *          order by.
   * @param hql
   *          A String with the HQL query that is being used.
   * @return The index of the query column related to the field. Note that 0 will be returned if
   *         there is no query column with an alias equal to the provided field name.
   */
  private int getFieldSortIndex(String fieldName, String hql) {
    @SuppressWarnings("deprecation")
    final String[] queryAliases = OBDal.getInstance()
        .getSession()
        .createQuery(hql.replace(ADDITIONAL_FILTERS, "1=1"))
        .getReturnAliases();

    for (int i = 0; i < queryAliases.length; i++) {
      if (queryAliases[i] != null && queryAliases[i].equals(fieldName)) {
        return i + 1;
      }
    }
    return 0;
  }

  private HashMap<String, String[]> getCriteria(JSONArray criterias) {
    HashMap<String, String[]> criteriaValues = new HashMap<String, String[]>();
    try {

      for (int i = 0; i < criterias.length(); i++) {
        JSONObject criteria = criterias.getJSONObject(i);
        if (!criteria.has("fieldName") && criteria.has("criteria")
            && criteria.has("_constructor")) {
          // nested criteria, eval it recursively
          JSONArray cs = criteria.getJSONArray("criteria");
          HashMap<String, String[]> c = getCriteria(cs);
          for (String k : c.keySet()) {
            criteriaValues.put(k, c.get(k));
          }
          continue;
        }
        final String operator = criteria.getString("operator");
        final String fieldName = criteria.getString("fieldName");
        String[] criterion;
        if (operator.equals(AdvancedQueryBuilder.OPERATOR_EXISTS)
            && criteria.has(AdvancedQueryBuilder.EXISTS_QUERY_KEY)) {
          String value = "";
          JSONArray values = criteria.getJSONArray("value");
          for (int v = 0; v < values.length(); v++) {
            value += value.length() > 0 ? ", " : "";
            value += "'" + values.getString(v) + "'";
          }
          String qry = criteria.getString(AdvancedQueryBuilder.EXISTS_QUERY_KEY)
              .replace(AdvancedQueryBuilder.EXISTS_VALUE_HOLDER, value);

          if (criteriaValues.containsKey(fieldName)) {
            // assuming it is possible to have more than one query for exists in same field, storing
            // them as array
            String[] originalCriteria = criteriaValues.get(fieldName);
            List<String> newCriteria = new ArrayList<String>(Arrays.asList(originalCriteria));
            newCriteria.add(qry);
            criteriaValues.put(fieldName, newCriteria.toArray(new String[newCriteria.size()]));
          } else {
            criteriaValues.put(fieldName,
                new String[] { AdvancedQueryBuilder.EXISTS_QUERY_KEY, qry });
          }
        } else {
          criterion = new String[] { operator, criteria.getString("value") };
          criteriaValues.put(fieldName, criterion);
        }
      }
    } catch (JSONException e) {
      log.error("Error getting criteria for custom query selector", e);
    }
    if (criteriaValues.isEmpty()) {
      return null;
    }
    return criteriaValues;

  }

  private String[] getDistinctCriteria(Map<String, String> parameters, String fieldName) {
    try {
      JSONArray criterias = (JSONArray) JsonUtils.buildCriteria(parameters).get("criteria");
      HashMap<String, String[]> criteria = getCriteria(criterias);
      String key = fieldName + "$_identifier";
      if (criteria == null || !criteria.containsKey(key)) {
        return null;
      }
      return criteria.get(key);
    } catch (JSONException e) {
      return null;
    }
  }

  private HashMap<String, String[]> getCriteria(Map<String, String> parameters) {
    if (!"AdvancedCriteria".equals(parameters.get("_constructor"))) {
      return null;
    }
    try {
      JSONArray criterias = (JSONArray) JsonUtils.buildCriteria(parameters).get("criteria");
      return getCriteria(criterias);
    } catch (JSONException e) {
      return null;
    }
  }

  private String getTypedParameterAlias(List<Object> typedParameters, Object value) {
    String alias = ":" + ALIAS_PREFIX + (typedParameters.size());
    typedParameters.add(value);
    return alias;
  }

}
