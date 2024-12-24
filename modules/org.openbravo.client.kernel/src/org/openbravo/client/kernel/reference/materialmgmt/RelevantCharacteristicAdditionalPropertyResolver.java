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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openbravo.base.model.Entity;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.RelevantCharacteristicProperty;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.common.plm.CharacteristicValue;
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.datasource.DataSourceProperty;
import org.openbravo.service.json.AdditionalPropertyResolver;
import org.openbravo.service.json.JsonConstants;

/**
 * Resolves additional properties that reference to relevant characteristics and provides the data
 * source properties required to filter and sort by the characteristic values linked to the relevant
 * characteristic in the client side.
 */
public class RelevantCharacteristicAdditionalPropertyResolver
    implements AdditionalPropertyResolver {

  private static final String INTEGER_REFERENCE_ID = "11";
  private static final String STRING_REFERENCE_ID = "10";
  private static final String PROPERTY_SEQUENCENUMBER = "sequenceNumber";

  @Override
  public boolean canResolve(Entity entity, String additionalProperty) {
    return RelevantCharacteristicProperty.from(entity, additionalProperty).isPresent();
  }

  @Override
  public Map<String, Object> resolve(BaseOBObject bob, String additionalProperty) {
    return RelevantCharacteristicProperty.from(bob.getEntity(), additionalProperty).map(o -> {
      Map<String, Object> result = new HashMap<>();
      CharacteristicValue chv = o.getCharacteristicValue(bob);
      result.put(additionalProperty, chv != null ? chv.getId() : null);
      result.put(additionalProperty + DalUtil.DOT + JsonConstants.IDENTIFIER,
          chv != null ? chv.getIdentifier() : null);
      result.put(additionalProperty + DalUtil.DOT + PROPERTY_SEQUENCENUMBER,
          chv != null ? getSequenceNumber(chv.getId()) : null);
      return result;
    }).orElse(Collections.emptyMap());
  }

  @Override
  public List<DataSourceProperty> getDataSourceProperties(Entity entity,
      String additionalProperty) {
    return RelevantCharacteristicProperty.from(entity, additionalProperty)
        .map(rcp -> getRelevantCharacteristicDataSourceProperties(additionalProperty))
        .orElse(Collections.emptyList());
  }

  private Long getSequenceNumber(String characteristicValueId) {
    //@formatter:off
    String hql = "select tn.sequenceNumber" +
                 "  from ADTreeNode as tn" +
                 " where tn.node = :characteristicValueId";
    //@formatter:on
    return OBDal.getInstance()
        .getSession()
        .createQuery(hql, Long.class)
        .setParameter("characteristicValueId", characteristicValueId)
        .uniqueResult();
  }

  private List<DataSourceProperty> getRelevantCharacteristicDataSourceProperties(
      String additionalProperty) {
    DataSourceProperty chValueId = new DataSourceProperty();
    chValueId.setName(additionalProperty.replace(DalUtil.DOT, DalUtil.FIELDSEPARATOR));
    chValueId.setId(false);
    chValueId.setMandatory(false);
    chValueId.setAuditInfo(false);
    chValueId.setUpdatable(false);
    chValueId.setBoolean(false);
    chValueId.setAllowedValues(null);
    chValueId.setPrimitive(true);
    chValueId.setFieldLength(100);
    chValueId.setUIDefinition(UIDefinitionController.getInstance()
        .getUIDefinition(OBDal.getInstance().getProxy(Reference.class, STRING_REFERENCE_ID)));
    chValueId.setPrimitiveObjectType(String.class);
    chValueId.setNumericType(false);
    chValueId.setAdditional(true);

    DataSourceProperty sequenceNumber = new DataSourceProperty();
    sequenceNumber.setName(additionalProperty.replace(DalUtil.DOT, DalUtil.FIELDSEPARATOR)
        + DalUtil.FIELDSEPARATOR + PROPERTY_SEQUENCENUMBER);
    sequenceNumber.setId(false);
    sequenceNumber.setMandatory(false);
    sequenceNumber.setAuditInfo(false);
    sequenceNumber.setUpdatable(false);
    sequenceNumber.setBoolean(false);
    sequenceNumber.setAllowedValues(null);
    sequenceNumber.setPrimitive(true);
    sequenceNumber.setFieldLength(12);
    sequenceNumber.setUIDefinition(UIDefinitionController.getInstance()
        .getUIDefinition(OBDal.getInstance().getProxy(Reference.class, INTEGER_REFERENCE_ID)));
    sequenceNumber.setPrimitiveObjectType(Long.class);
    sequenceNumber.setNumericType(true);

    return List.of(chValueId, sequenceNumber);
  }

  @Override
  public Set<String> getPropertyNames(Entity entity) {
    return entity != null && Product.ENTITY_NAME.equals(entity.getName())
        ? RelevantCharacteristicProperty.getRelevantCharateristicProperties()
        : Collections.emptySet();
  }
}
