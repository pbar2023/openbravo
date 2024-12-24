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

import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.window.OBTreeReferenceComponent;
import org.openbravo.client.kernel.reference.UIDefinition;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.RelevantCharacteristicProperty;
import org.openbravo.model.ad.domain.ReferencedTree;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.service.json.JsonConstants;

/**
 * UI definition for displaying the values of the characteristics which are linked to a relevant
 * characteristic
 */
public class RelevantCharacteristicUIDefinition extends UIDefinition {

  private static final String CH_VALUE_TREE_FILTER_REFERENCE = "39891DBE132640E98C15D090959DCC29";

  @Override
  public String getFormEditorType() {
    return "OBMappedTextItem";
  }

  @Override
  public String getFilterEditorType() {
    return "OBCharacteristicValueTreeFilterItem";
  }

  @Override
  public String getGridFieldProperties(Field field) {
    String gridFieldProperties = super.getGridFieldProperties(field);
    gridFieldProperties += ", fkField: true";
    gridFieldProperties += ", length: 100";
    gridFieldProperties += ", displaylength: 100";
    return gridFieldProperties;
  }

  @Override
  public String getGridEditorFieldProperties(Field field) {
    String gridFieldEditorProperties = "displayField: '" + getDisplayField(field) + "'";
    gridFieldEditorProperties += ", valueField: '" + getProperty(field) + "'";
    return gridFieldEditorProperties;
  }

  @Override
  public String getFilterEditorPropertiesProperty(Field field) {
    return RelevantCharacteristicProperty.from(field)
        .map(p -> ", filterType: 'id', filterOnKeypress: false, criteriaField: '"
            + getProperty(field) + "', characteristicId: '" + p.getCharacteristicId() + "'")
        .orElse("");
  }

  @Override
  public String getFieldProperties(Field field) {
    if (field == null) {
      return "";
    }
    String displayField = getDisplayField(field);
    String valueField = getProperty(field);
    String fieldProperties = getOBTreeReferenceComponent().generate();
    fieldProperties += " displayField: '" + displayField + "'";
    fieldProperties += ", valueField: '" + valueField + "'";
    fieldProperties += ", doMapValueToDisplay: (value, form) => form.getValue('" + displayField
        + "') || ''";
    return fieldProperties;
  }

  @Override
  public String getTypeProperties() {
    return "sortNormalizer: function (item, field, context){ return OB.Utilities.bySeqNoSortNormalizer(item, field, context); },";
  }

  private String getDisplayField(Field field) {
    return getProperty(field) + DalUtil.FIELDSEPARATOR + JsonConstants.IDENTIFIER;
  }

  private String getProperty(Field field) {
    return field.getProperty().replace(DalUtil.DOT, DalUtil.FIELDSEPARATOR);
  }

  private OBTreeReferenceComponent getOBTreeReferenceComponent() {
    OBTreeReferenceComponent treeReferenceComponent = WeldUtils
        .getInstanceFromStaticBeanManager(OBTreeReferenceComponent.class);
    ReferencedTree referencedTree = OBDal.getInstance()
        .get(ReferencedTree.class, CH_VALUE_TREE_FILTER_REFERENCE);
    treeReferenceComponent.setId(referencedTree.getId());
    treeReferenceComponent.setParameters(new HashMap<>());
    treeReferenceComponent.setReferencedTree(referencedTree);
    return treeReferenceComponent;
  }
}
