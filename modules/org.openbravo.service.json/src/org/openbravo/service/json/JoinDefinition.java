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

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.util.CheckException;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.dal.core.DalUtil;

/**
 * Used to define entity joins for the queries built through the {@link AdvancedQueryBuilder}.
 */
public class JoinDefinition {
  private AdvancedQueryBuilder queryBuilder;
  private Property property;
  private String propertyPath;
  private String joinAlias;
  private String ownerAlias;
  private boolean fetchJoin;
  private String joinWithClause;
  private Property unrelatedProperty;

  /**
   * Builds a new entity join definition
   * 
   * @param queryBuilder
   *          The {@link AdvancedQueryBuilder} instance. It can be used to access to information
   *          related to the query being built
   */
  public JoinDefinition(AdvancedQueryBuilder queryBuilder) {
    this.queryBuilder = queryBuilder;
    this.joinAlias = queryBuilder.getNewUniqueJoinAlias();
    this.fetchJoin = queryBuilder.isJoinAssociatedEntities();
  }

  boolean appliesTo(String checkAlias, Property checkProperty) {
    return checkAlias.equals(ownerAlias) && checkProperty == property;
  }

  /**
   * @return the HQL statement built with the join definition
   */
  public String getJoinStatement() {
    String propName = getPropertyName();
    if (unrelatedProperty != null) {
      return " left join " + unrelatedProperty.getEntity().getName() + " as " + joinAlias + " on "
          + joinAlias + DalUtil.DOT + unrelatedProperty.getName() + " = " + ownerAlias + DalUtil.DOT
          + propName;
    }
    String joinType = null;
    if (queryBuilder.getOrNesting() > 0) {
      joinType = " left outer join ";
    }
    // if all the identifier properties of the target entity are mandatory, and if the joined
    // entity is used only in where clauses resulting from filtering the grid, an inner join can
    // be used
    else if (property == null || Entity.COMPUTED_COLUMNS_PROXY_PROPERTY.equals(property.getName())
        || KernelUtils.hasNullableIdentifierProperties(property.getTargetEntity())
        || !(queryBuilder.useInnerJoin(property))) {
      joinType = " left join ";
    } else {
      joinType = " inner join ";
    }
    return joinType + (fetchJoin ? "fetch " : "")
        + (ownerAlias != null ? ownerAlias + DalUtil.DOT : "") + propName + " as " + joinAlias
        + (joinWithClause != null ? " with " + joinAlias + DalUtil.DOT + joinWithClause : "");
  }

  private String getPropertyName() {
    if (propertyPath != null) {
      return propertyPath;
    } else if (property.isComputedColumn()) {
      return Entity.COMPUTED_COLUMNS_PROXY_PROPERTY + DalUtil.DOT + property.getName();
    }
    return property.getName();
  }

  /**
   * Sets the joining property
   * 
   * @param property
   *          The joining property
   *
   * @return the JoinDefinition instance, for method chaining
   */
  public JoinDefinition setProperty(Property property) {
    this.property = property;
    return this;
  }

  /**
   * @return the joining property
   */
  public Property getProperty() {
    return property;
  }

  /**
   * Sets the property path of the joining property. It may be used as an alternative for the
   * {@link #setProperty(Property)} method.
   * 
   * @param propertyPath
   *          The property path of the joining property
   *
   * @return the JoinDefinition instance, for method chaining
   */
  public JoinDefinition setPropertyPath(String propertyPath) {
    this.propertyPath = propertyPath;
    return this;
  }

  /**
   * @return the joining property path
   */
  public String getPropertyPath() {
    return propertyPath;
  }

  /**
   * @return the alias given to the join
   */
  public String getJoinAlias() {
    return joinAlias;
  }

  /**
   * Sets the alias of the owner of the joining property
   * 
   * @param ownerAlias
   *          The alias of the owner of the joining property
   *
   * @return the JoinDefinition instance, for method chaining
   */
  public JoinDefinition setOwnerAlias(String ownerAlias) {
    this.ownerAlias = ownerAlias;
    return this;
  }

  /**
   * Sets whether a fetch join must be used
   * 
   * @param fetchJoin
   *          True to use fetch when joining the entities or false to not use it
   *
   * @return the JoinDefinition instance, for method chaining
   */
  public JoinDefinition setFetchJoin(boolean fetchJoin) {
    this.fetchJoin = fetchJoin;
    return this;
  }

  /**
   * Sets a conditional clause to be considered when joining the entities
   * 
   * @param joinWithClause
   *          The conditional clause for the join
   *
   * @return the JoinDefinition instance, for method chaining
   */
  public JoinDefinition setJoinWithClause(String joinWithClause) {
    this.joinWithClause = joinWithClause;
    return this;
  }

  /**
   * Sets this join definition as a join with an unrelated entity, i.e., an entity which is not
   * referenced by the main entity of the query.
   * 
   * @param entityName
   *          The name of the unrelated entity
   * @param propertyName
   *          The name of the property of the unrelated entity to be used for the joining
   *
   * @return the JoinDefinition instance, for method chaining
   * @throws CheckException
   *           if there is no entity with the provided name or if it exists but there is no property
   *           with the given property name in that entity
   */
  public JoinDefinition setUnrelatedEntityJoin(String entityName, String propertyName) {
    this.unrelatedProperty = ModelProvider.getInstance()
        .getEntity(entityName)
        .getProperty(propertyName);
    return this;
  }
}
