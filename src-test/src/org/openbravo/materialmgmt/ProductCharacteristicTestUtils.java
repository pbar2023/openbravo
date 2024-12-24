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

import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.domaintype.StringEnumerateDomainType;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Characteristic;
import org.openbravo.model.common.plm.CharacteristicValue;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCharacteristicValue;
import org.openbravo.test.base.TestConstants;

/**
 * Test utilities to deal with relevant characteristics
 */
public class ProductCharacteristicTestUtils {
  private static final String RELEVANT_CHARACTERISTICS_REFERENCE = "247C9B7EEFE1475EA322003B96E8B7AE";

  private ProductCharacteristicTestUtils() {
  }

  /**
   * Adds a new relevant characteristic into the "Relevant Characteristics" list reference. Note
   * that this method invokes {@link OBDal#flush()} to ensure that the changes are visible into the
   * current transaction.
   *
   * @param searchKey
   *          The search key of the new relevant characteristic
   * @param name
   *          The name of the new relevant characteristic
   * @param moduleId
   *          The ID of the module that the new relevant characteristic belongs to
   */
  public static void addRelevantCharacteristic(String searchKey, String name, String moduleId) {
    try {
      OBContext.setAdminMode(false);
      Module module = OBDal.getInstance().get(Module.class, moduleId);
      setModuleInDevelopment(module, true);

      org.openbravo.model.ad.domain.List listReference = OBProvider.getInstance()
          .get(org.openbravo.model.ad.domain.List.class);
      listReference
          .setClient(OBDal.getInstance().getProxy(Client.class, TestConstants.Clients.SYSTEM));
      listReference.setOrganization(
          OBDal.getInstance().getProxy(Organization.class, TestConstants.Orgs.MAIN));
      listReference.setModule(module);
      listReference.setReference(
          OBDal.getInstance().getProxy(Reference.class, RELEVANT_CHARACTERISTICS_REFERENCE));
      listReference.setSearchKey(searchKey);
      listReference.setName(name);
      listReference.setDescription(name + " relevant characteristic for testing purposes");
      OBDal.getInstance().save(listReference);
      OBDal.getInstance().flush();

      StringEnumerateDomainType relevantCharDomainType = (StringEnumerateDomainType) ModelProvider
          .getInstance()
          .getEntity(Characteristic.class)
          .getProperty("relevantCharacteristic")
          .getDomainType();
      relevantCharDomainType.addEnumerateValue(searchKey);

      setModuleInDevelopment(module, false);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private static void setModuleInDevelopment(Module module, boolean inDevelopment) {
    module.setInDevelopment(inDevelopment);
    OBDal.getInstance().flush();
  }

  /**
   * Creates a new {@link Characteristic} linked to the provided relevant characteristic
   *
   * @param name
   *          The name of the new characteristic
   * @param relevantCharacteristic
   *          The search key of the relevant characteristic
   *
   * @return the new characteristic
   */
  public static Characteristic createCharacteristicLinkedToRelevant(String name,
      String relevantCharacteristic) {
    Characteristic characteristic = OBProvider.getInstance().get(Characteristic.class);
    characteristic
        .setOrganization(OBDal.getInstance().getProxy(Organization.class, TestConstants.Orgs.MAIN));
    characteristic.setName(name);
    characteristic.setRelevantCharacteristic(relevantCharacteristic);
    OBDal.getInstance().save(characteristic);
    return characteristic;
  }

  /**
   * Creates a new {@link CharacteristicValue} for the given {@link Characteristic}
   *
   * @param name
   *          The name of the new characteristic value
   * @param code
   *          The code of the new characteristic value
   * @param characteristicId
   *          The ID of the characteristic that the new value belongs to
   *
   * @return the new characteristic value
   */
  public static CharacteristicValue createCharacteristicValue(String name, String code,
      String characteristicId) {
    Characteristic characteristic = OBDal.getInstance().get(Characteristic.class, characteristicId);
    CharacteristicValue characteristicValue = OBProvider.getInstance()
        .get(CharacteristicValue.class);
    characteristicValue.setOrganization(characteristic.getOrganization());
    characteristicValue.setName(name);
    characteristicValue.setCode(code);
    characteristicValue.setCharacteristic(characteristic);
    OBDal.getInstance().save(characteristicValue);
    return characteristicValue;
  }

  /**
   * Assigns a {@link CharacteristicValue} to a {@link Product}
   *
   * @param productId
   *          The ID of the product
   * @param characteristicValueId
   *          The ID of the characteristic value
   */
  public static void assignCharacteristicValueToProduct(String productId,
      String characteristicValueId) {
    ProductCharacteristicValue productCharacteristicValue = OBProvider.getInstance()
        .get(ProductCharacteristicValue.class);
    CharacteristicValue characteristicValue = OBDal.getInstance()
        .get(CharacteristicValue.class, characteristicValueId);
    productCharacteristicValue.setProduct(OBDal.getInstance().getProxy(Product.class, productId));
    productCharacteristicValue.setCharacteristic(characteristicValue.getCharacteristic());
    productCharacteristicValue.setCharacteristicValue(characteristicValue);
    OBDal.getInstance().save(productCharacteristicValue);
  }

  /**
   * Unlinks the relevant characteristic assigned to the {@link Characteristic} whose ID is passed
   * as parameter
   *
   * @param characteristicId
   *          The ID of the characteristic
   */
  public static void unlinkRelevantCharacteristic(String characteristicId) {
    Characteristic characteristic = OBDal.getInstance().get(Characteristic.class, characteristicId);
    characteristic.setRelevantCharacteristic(null);
  }

  /**
   * Reloads the cache of relevant characteristic properties
   */
  public static void reloadRelevantCharacteristicsCache() {
    RelevantCharacteristicProperty.reloadRelevantCharacteristicsCache();
  }

  /**
   * Creates a {@link Field} on a {@link Tab} with the given property path
   *
   * @param tabId
   *          The ID of the tab where the field is going to be placed
   * @param propertyPath
   *          The path of the property
   */
  public static Field createPropertyField(String tabId, String propertyPath, String moduleId) {
    try {
      OBContext.setAdminMode(false);
      Module module = OBDal.getInstance().get(Module.class, moduleId);
      setModuleInDevelopment(module, true);

      Field field = OBProvider.getInstance().get(Field.class);
      field.setClient(OBDal.getInstance().getProxy(Client.class, TestConstants.Clients.SYSTEM));
      field.setOrganization(
          OBDal.getInstance().getProxy(Organization.class, TestConstants.Orgs.MAIN));
      field.setTab(OBDal.getInstance().getProxy(Tab.class, tabId));
      field.setName(propertyPath);
      field.setProperty(propertyPath);
      OBDal.getInstance().save(field);

      setModuleInDevelopment(module, false);
      return field;
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
