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
package org.openbravo.materialmgmt;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.common.plm.Characteristic;
import org.openbravo.model.common.plm.CharacteristicValue;
import org.openbravo.model.common.plm.Product;

/**
 * Keeps the information related to a relevant characteristic property.
 */
public class RelevantCharacteristicProperty {
  private static final String RELEVANT_CHARACTERISTICS_REFERENCE = "247C9B7EEFE1475EA322003B96E8B7AE";
  private static final List<RefListEntry> REF_LIST_ENTRIES = getRefListEntries();

  private Entity entity;
  private String basePath;
  private String name;

  private RelevantCharacteristicProperty(Entity entity, String basePath, String name) {
    this.entity = entity;
    this.basePath = basePath;
    this.name = name;
  }

  /**
   * @return The entity that references the characteristic through the base path
   */
  public Entity getEntity() {
    return entity;
  }

  /**
   * @return the base path the points to the relevant characteristic property, without including the
   *         relevant characteristic property name. It may be null, for example when the entity is
   *         the {@link Product} entity
   */
  public String getBasePath() {
    return basePath;
  }

  /**
   * @return the name of the relevant characteristic property
   */
  public String getName() {
    return name;
  }

  /**
   * @return the search key of the referenced relevant characteristic
   */
  public String getSearchKey() {
    return getRefListEntry().getSearchKey();
  }

  /**
   * @return the ID of the {@link org.openbravo.model.ad.domain.List} for the relevant
   *         characteristic in the list reference of relevant characteristics.
   */
  String getRefListId() {
    return getRefListEntry().getId();
  }

  /**
   * @return the name to be used in the {@Field} that points to this property. This is the name of
   *         the {@link org.openbravo.model.ad.domain.List} for the relevant characteristic in the
   *         list reference of relevant characteristics.
   */
  public String getFieldName() {
    return getRefListEntry().getName();
  }

  /**
   * @return the description of the property. This is the description of the
   *         {@link org.openbravo.model.ad.domain.List} for the relevant characteristic in the list
   *         reference of relevant characteristics.
   */
  String getDescription() {
    return getRefListEntry().getDescription();
  }

  /**
   * @return the id of the {@link Characteristic} linked to the relevant characteristic in the
   *         client of the current context
   */
  public String getCharacteristicId() {
    //@formatter:off
    String hql = "select ch.id" +
                 "  from Characteristic as ch" +
                 " where ch.relevantCharacteristic = :relevantCharacteristic" +
                 "  and ch.client.id = :clientId";
    //@formatter:on
    return OBDal.getInstance()
        .getSession()
        .createQuery(hql, String.class)
        .setParameter("relevantCharacteristic", getSearchKey())
        .setParameter("clientId", OBContext.getOBContext().getCurrentClient().getId())
        .uniqueResult();
  }

  /**
   * Retrieves the value of the characteristic linked to the relevant characteristic for the given
   * {@link BaseOBObject}
   * 
   * @param bob
   *          A {@link BaseOBObject}
   * 
   * @return the value of the characteristic linked to the relevant characteristic for the given
   *         {@link BaseOBObject}
   */
  public CharacteristicValue getCharacteristicValue(BaseOBObject bob) {
    Product product = Product.ENTITY_NAME.equals(bob.getEntity().getName()) ? (Product) bob
        : (Product) DalUtil.getValueFromPath(bob, basePath);

    //@formatter:off
    String hql = "select cv" +
                 "  from CharacteristicValue as cv" +
                 "  join cv.productCharacteristicValueList as pcv"+
                 " where pcv.product.id = :productId" +
                 "  and pcv.characteristic.id = :characteristicId";
    //@formatter:on

    return OBDal.getInstance()
        .getSession()
        .createQuery(hql, CharacteristicValue.class)
        .setParameter("productId", product.getId())
        .setParameter("characteristicId", getCharacteristicId())
        .uniqueResult();
  }

  private RefListEntry getRefListEntry() {
    return REF_LIST_ENTRIES.stream()
        .filter(rc -> rc.getPropertyName().equals(name))
        .findFirst()
        .orElseThrow();
  }

  /**
   * Retrieves a new {@code RelevantCharacteristicProperty} instance for the relevant characteristic
   * referenced by the given {@link Field}.
   *
   * @param field
   *          The AD field
   *
   * @return an Optional describing the {@code RelevantCharacteristicProperty} or an empty Optional
   *         in case the property cannot be resolved
   */
  public static Optional<RelevantCharacteristicProperty> from(Field field) {
    String propertyPath = field.getProperty();
    if (propertyPath == null) {
      return Optional.empty();
    }
    Entity entity = ModelProvider.getInstance()
        .getEntityByTableId(field.getTab().getTable().getId());
    return from(entity, propertyPath);
  }

  /**
   * Retrieves a new {@code RelevantCharacteristicProperty} instance for the relevant characteristic
   * pointed by the given {@link Entity} and property path.
   *
   * @param entity
   *          The {@link Entity} owner of the property path
   * @param propertyPath
   *          The path that references the relevant characteristic property
   *
   * @return an Optional describing the {@code RelevantCharacteristicProperty} or an empty Optional
   *         in case the property cannot be resolved
   */
  public static Optional<RelevantCharacteristicProperty> from(Entity entity, String propertyPath) {
    String[] parts = propertyPath.replace(DalUtil.FIELDSEPARATOR, DalUtil.DOT)
        .split("\\" + DalUtil.DOT);
    Entity currentEntity = entity;
    StringBuilder basePath = null;
    Set<String> relevantCharacteristicProperties = getRelevantCharateristicProperties();
    for (String part : parts) {
      if (!currentEntity.hasProperty(part)) {
        if (Product.ENTITY_NAME.equals(currentEntity.getName())
            && relevantCharacteristicProperties.contains(part)) {
          return Optional.of(new RelevantCharacteristicProperty(entity,
              basePath == null ? null : basePath.toString(), part));
        }
        return Optional.empty();
      }
      if (basePath == null) {
        basePath = new StringBuilder(part);
      } else {
        basePath.append(DalUtil.DOT);
        basePath.append(part);
      }
      Property currentProperty = currentEntity.getProperty(part);
      if (currentProperty.getTargetEntity() != null) {
        currentEntity = currentProperty.getTargetEntity();
      }
    }
    return Optional.empty();
  }

  /**
   * @return a set with the property name of the existing relevant characteristics
   */
  public static Set<String> getRelevantCharateristicProperties() {
    return REF_LIST_ENTRIES.stream().map(RefListEntry::getPropertyName).collect(Collectors.toSet());
  }

  private static List<RefListEntry> getRefListEntries() {
    try {
      OBContext.setAdminMode(true);
      return OBDal.getInstance()
          .createCriteria(org.openbravo.model.ad.domain.List.class)
          .add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_REFERENCE,
              OBDal.getInstance().getProxy(Reference.class, RELEVANT_CHARACTERISTICS_REFERENCE)))
          .setFilterOnReadableClients(false)
          .setFilterOnReadableOrganization(false)
          .list()
          .stream()
          .map(RefListEntry::new)
          .collect(Collectors.toList());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Internal API: do not use, used for testing purposes
   * 
   * Reloads the cache of relevant characteristic properties
   */
  static void reloadRelevantCharacteristicsCache() {
    REF_LIST_ENTRIES.clear();
    REF_LIST_ENTRIES.addAll(getRefListEntries());
  }
}
