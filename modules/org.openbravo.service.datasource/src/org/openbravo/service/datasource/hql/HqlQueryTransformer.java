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
 * All portions are Copyright (C) 2014-2022 Openbravo SLU 
 * All Rights Reserved. 
 ************************************************************************
 */
package org.openbravo.service.datasource.hql;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.ComponentProvider;

/**
 * A HqlQueryTransformer is able to transform an HQL query. HqlTransformer are instantiated using
 * dependency injection.
 */

@ApplicationScoped
public abstract class HqlQueryTransformer extends HqlQueryPriorityHandler {
  private static Logger log = LogManager.getLogger();

  /**
   * Returns some code to be injected in a HQL query, and adds query named parameters when needed
   * 
   * @param requestParameters
   *          the parameters of the request. The injected code may vary depending on these
   *          parameters
   * @param queryNamedParameters
   *          the named parameters of the hql query that will be used to fetch the table data. If
   *          the injected code uses named parameters, the named parameters must be added to this
   *          map
   * @return the hql code to be injected
   */

  /**
   * Returns the transformed hql query
   * 
   * @param hqlQuery
   *          original hql query
   * @param requestParameters
   *          the parameters of the request
   * @param queryNamedParameters
   *          the named parameters of the hql query that will be used to fetch the table data. If
   *          the transformed hql query uses named parameters that did not exist in the original hql
   *          query, the named parameters must be added to this map
   * @return the transformed hql query
   */
  public abstract String transformHqlQuery(String hqlQuery, Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters);

  /**
   * Returns, if defined, an HQL Query Transformer for this selector. If the are several
   * transformers defined, the one with the lowest priority will be chosen
   * 
   * @param parameters
   *          the parameters of the request
   * @return the HQL Query transformer that will be used to transform the query
   */
  private static HqlQueryTransformer getTransformer(Map<String, String> parameters,
      BaseOBObject obj, Instance<HqlQueryTransformer> hqlQueryTransformers) {
    HqlQueryTransformer transformer = null;

    for (HqlQueryTransformer nextTransformer : hqlQueryTransformers
        .select(new ComponentProvider.Selector(obj.getId().toString()))) {
      if (transformer == null) {
        transformer = nextTransformer;
      } else if (nextTransformer.getPriority(parameters) < transformer.getPriority(parameters)) {
        transformer = nextTransformer;
      } else if (nextTransformer.getPriority(parameters) == transformer.getPriority(parameters)) {
        log.warn(
            "Trying to get hql query transformer for record id {}, there are more than one instance with same priority",
            obj.getId().toString());
      }
    }
    return transformer;
  }

  /**
   * If there is any HQL Query Transformer defined, uses its transformHqlQuery to transform the
   * query
   * 
   * @param hqlQuery
   *          the original HQL query
   * @param queryNamedParameters
   *          the named parameters that will be used in the query
   * @param parameters
   *          the parameters of the request
   * @return the transformed query
   */
  public static String transFormQuery(String hqlQuery, Map<String, Object> queryNamedParameters,
      Map<String, String> parameters, BaseOBObject obj,
      Instance<HqlQueryTransformer> hqlQueryTransformers) {
    String transformedHqlQuery = hqlQuery;
    HqlQueryTransformer hqlQueryTransformer = getTransformer(parameters, obj, hqlQueryTransformers);
    if (hqlQueryTransformer != null) {
      transformedHqlQuery = hqlQueryTransformer.transformHqlQuery(transformedHqlQuery, parameters,
          queryNamedParameters);
    }
    return transformedHqlQuery;
  }

}
