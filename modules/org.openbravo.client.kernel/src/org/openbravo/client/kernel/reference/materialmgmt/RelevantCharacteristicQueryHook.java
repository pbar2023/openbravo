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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.materialmgmt.RelevantCharacteristicProperty;
import org.openbravo.model.ad.utility.TreeNode;
import org.openbravo.model.common.plm.CharacteristicValue;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCharacteristicValue;
import org.openbravo.service.json.AdvancedQueryBuilder;
import org.openbravo.service.json.AdvancedQueryBuilderHook;
import org.openbravo.service.json.JoinDefinition;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonUtils;

/**
 * This hooks allows to build the joins, filters and order by clauses for those queries that use a
 * property path that points to a relevant characteristic property.
 */
@Dependent
public class RelevantCharacteristicQueryHook implements AdvancedQueryBuilderHook {
  private static final Logger log = LogManager.getLogger();

  private Map<String, String> joinsWithProductCharacteristicValue = new HashMap<>();
  private Map<String, String> joinsWithTreeNode = new HashMap<>();

  @Override
  public List<JoinDefinition> getJoinDefinitions(AdvancedQueryBuilder queryBuilder,
      List<JoinDefinition> joinDefinitions) {
    for (RelevantCharacteristicProperty property : getRelevantCharacteristicProperties(
        queryBuilder)) {

      String characteristicId = property.getCharacteristicId();
      if (characteristicId == null) {
        continue;
      }

      // add the join with M_Product, if needed
      String productAlias;
      if (isProductEntity(queryBuilder.getEntity())) {
        productAlias = queryBuilder.getMainAlias();
      } else {
        JoinDefinition productJoin = getJoinDefinition(joinDefinitions, property.getBasePath());
        if (productJoin == null) {
          productJoin = new JoinDefinition(queryBuilder).setOwnerAlias(queryBuilder.getMainAlias())
              .setFetchJoin(false)
              .setPropertyPath(property.getBasePath());
          joinDefinitions.add(productJoin);
        }
        productAlias = productJoin.getJoinAlias();
      }

      // join with M_Product_Ch_Value
      JoinDefinition relevantCharJoin = new JoinDefinition(queryBuilder).setOwnerAlias(productAlias)
          .setFetchJoin(false)
          .setProperty(ModelProvider.getInstance()
              .getEntity(Product.class)
              .getProperty(Product.PROPERTY_PRODUCTCHARACTERISTICVALUELIST))
          .setJoinWithClause(
              "characteristic.id = :" + queryBuilder.addNamedParameter(characteristicId));
      joinDefinitions.add(relevantCharJoin);

      // join with M_Ch_Value
      JoinDefinition charValueJoin = new JoinDefinition(queryBuilder)
          .setOwnerAlias(relevantCharJoin.getJoinAlias())
          .setFetchJoin(false)
          .setProperty(ModelProvider.getInstance()
              .getEntity(ProductCharacteristicValue.class)
              .getProperty(ProductCharacteristicValue.PROPERTY_CHARACTERISTICVALUE));
      joinDefinitions.add(charValueJoin);
      joinsWithProductCharacteristicValue.put(property.getName(), charValueJoin.getJoinAlias());

      // join with AD_TreeNode so we can sort by sequence number
      JoinDefinition adTreeNodeJoin = new JoinDefinition(queryBuilder)
          .setOwnerAlias(charValueJoin.getJoinAlias())
          .setFetchJoin(false)
          .setProperty(ModelProvider.getInstance()
              .getEntity(CharacteristicValue.class)
              .getProperty(CharacteristicValue.PROPERTY_ID))
          .setUnrelatedEntityJoin("ADTreeNode", "node");
      joinDefinitions.add(adTreeNodeJoin);
      joinsWithTreeNode.put(property.getName(), adTreeNodeJoin.getJoinAlias());
    }
    return joinDefinitions;
  }

  @Override
  public String parseSimpleFilterClause(AdvancedQueryBuilder queryBuilder, String fieldName,
      String operator, Object value) {
    String relevantCharacteristic = getRelevantCharacteristic(queryBuilder, fieldName);
    if (relevantCharacteristic == null
        || joinsWithProductCharacteristicValue.get(relevantCharacteristic) == null) {
      return null;
    }
    if (!"equals".equals(operator)) {
      log.error("Cannot filter using operator {} the field {} with value {}", operator, fieldName,
          value);
      return null;
    }
    String filterProperty = joinsWithProductCharacteristicValue.get(relevantCharacteristic)
        + DalUtil.DOT + CharacteristicValue.PROPERTY_ID;
    return filterProperty + " " + AdvancedQueryBuilder.getHqlOperator(operator) + " :"
        + queryBuilder.addNamedParameter(value);
  }

  @Override
  public String parseOrderByClausePart(AdvancedQueryBuilder queryBuilder, String orderByPart) {
    boolean desc = orderByPart.startsWith("-");
    String path = desc ? orderByPart.substring(1) : orderByPart;
    String identifierPart = JsonConstants.FIELD_SEPARATOR + JsonConstants.IDENTIFIER;
    if (path.endsWith(identifierPart)) {
      path = path.substring(0, path.length() - identifierPart.length());
    }
    String relevantCharacteristic = getRelevantCharacteristic(queryBuilder, path);
    if (relevantCharacteristic == null) {
      return null;
    }
    String treeNodeJoin = joinsWithTreeNode.get(relevantCharacteristic);
    String chValueJoin = joinsWithProductCharacteristicValue.get(relevantCharacteristic);
    if (treeNodeJoin == null || chValueJoin == null) {
      // we cannot sort by the referenced relevant characteristic because it is not linked to a
      // product characteristic, so just return an empty order by clause part to avoid breaking the
      // complete order by clause being built by the AdvancedQueryBuilder
      return "";
    }
    String direction = (desc ? " desc " : "");
    return treeNodeJoin + DalUtil.DOT + TreeNode.PROPERTY_SEQUENCENUMBER + direction + ","
        + chValueJoin + DalUtil.DOT + CharacteristicValue.PROPERTY_NAME + direction;
  }

  private List<RelevantCharacteristicProperty> getRelevantCharacteristicProperties(
      AdvancedQueryBuilder queryBuilder) {
    Set<String> properties = new LinkedHashSet<>(queryBuilder.getAdditionalProperties());
    return properties.stream()
        .map(p -> RelevantCharacteristicProperty.from(queryBuilder.getEntity(), p).orElse(null))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private JoinDefinition getJoinDefinition(List<JoinDefinition> list, String path) {
    return list.stream()
        .filter(join -> path.equals(join.getPropertyPath()))
        .findFirst()
        .orElse(null);
  }

  private boolean isProductEntity(Entity entity) {
    return entity != null && Product.ENTITY_NAME.equals(entity.getName());
  }

  private String getRelevantCharacteristic(AdvancedQueryBuilder queryBuilder, String fieldName) {
    if (isProductEntity(queryBuilder.getEntity())) {
      return RelevantCharacteristicProperty.getRelevantCharateristicProperties()
          .stream()
          .filter(fieldName::equals)
          .findFirst()
          .orElse(null);
    }
    List<Property> properties = JsonUtils.getPropertiesOnPath(queryBuilder.getEntity(), fieldName);
    if (properties.isEmpty()) {
      return null;
    }
    Property property = properties.get(properties.size() - 1);
    if (!isProductEntity(property.getTargetEntity())) {
      return null;
    }
    return RelevantCharacteristicProperty.getRelevantCharateristicProperties()
        .stream()
        .filter(p -> fieldName.endsWith(DalUtil.FIELDSEPARATOR + p))
        .findFirst()
        .orElse(null);
  }
}
