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

import java.util.List;

import org.openbravo.base.Prioritizable;

/**
 * Allows to modify the queries built by the {@link AdvancedQueryBuilder} by adding custom logic on
 * some key points of the query building.
 */
public interface AdvancedQueryBuilderHook extends Prioritizable {

  /**
   * This method can be used to modify the join clauses of the query.
   * 
   * @param queryBuilder
   *          The {@link AdvancedQueryBuilder} instance. It can be used to access to information
   *          related to the query being built
   * @param joinDefinitins
   *          The current list of {@link JoinDefinition} for the query being built
   * 
   * @return the new list of {@link JoinDefinition} of the query
   */
  public List<JoinDefinition> getJoinDefinitions(AdvancedQueryBuilder queryBuilder,
      List<JoinDefinition> joinDefinitins);

  /**
   * This method is used to provide an alternative way of parsing a simple filter clause. Note that
   * this method may return null which means that the filter clause may be parsed by another
   * {@code AdvancedQueryBuilderHook} with less priority, if exists. In case there is no hook
   * returning a not null value, then the filter clause will be parsed with the standard logic of
   * the {@link AdvancedQueryBuilder}.
   * 
   * @param queryBuilder
   *          The {@link AdvancedQueryBuilder} instance. It can be used to access to information
   *          related to the query being built
   * @param fieldName
   *          The name (it can be a path) of the property being filtered
   * @param operator
   *          The filtering operator
   * @param value
   *          The value to filter the property
   * 
   * @return a String containing the parsed filter clause or null in case this hook is not able to
   *         parse the filter clause
   */
  public String parseSimpleFilterClause(AdvancedQueryBuilder queryBuilder, String fieldName,
      String operator, Object value);

  /**
   * This method is used to provide an alternative way of parsing a part of the order by clause.
   * Note that this method may return null which means that the part of the order by may be parsed
   * by another {@code AdvancedQueryBuilderHook} with less priority, if exists. In case there is no
   * hook returning a not null value, then the order by clause will be parsed with the standard
   * logic of the {@link AdvancedQueryBuilder}.
   * 
   * @param queryBuilder
   *          The {@link AdvancedQueryBuilder} instance. It can be used to access to information
   *          related to the query being built
   * @param orderByPart
   *          One of the parts of order by clause of the query being built
   * 
   * @return a String with the parsed filter clause or null in case this hook is not able to parse
   *         the filter clause
   */
  public String parseOrderByClausePart(AdvancedQueryBuilder queryBuilder, String orderByPart);
}
