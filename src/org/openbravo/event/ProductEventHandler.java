/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2013-2024 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.plm.CharacteristicValue;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCharacteristic;
import org.openbravo.model.common.plm.ProductCharacteristicConf;

/**
 * Handles generic products and also propagates characteristic dimensions configuration from the
 * generic product to the variant products
 */
class ProductEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Product.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    validateCharacteristicDimensionsConfiguration(event);
    propagateCharacteristicDimensionsConfigurationFromGenericProductToVariantProducts(event);
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final Entity productEntity = ModelProvider.getInstance().getEntity(Product.ENTITY_NAME);
    final Property genericProperty = productEntity.getProperty(Product.PROPERTY_ISGENERIC);
    boolean oldGeneric = (Boolean) event.getPreviousState(genericProperty);
    boolean newGeneric = (Boolean) event.getCurrentState(genericProperty);
    if (oldGeneric && !newGeneric) {
      // check that the generic does not have any variant created.
      final Product product = (Product) event.getTargetInstance();
      if (!product.getProductGenericProductList().isEmpty()) {
        throw new OBException(OBMessageUtils.messageBD("CannotUnSetGenericProduct"));
      }
    }

    if (newGeneric && !oldGeneric) {
      // check that whether the there are characteristic already define
      final Product product = (Product) event.getTargetInstance();
      if (!product.getProductCharacteristicList().isEmpty()) {
        for (ProductCharacteristic productCh : product.getProductCharacteristicList()) {
          if (productCh.getProductCharacteristicConfList().isEmpty()
              && productCh.getCharacteristicSubset() == null && productCh.isVariant()) {
            // Create configuration based on Characteristic Values.
            for (CharacteristicValue chValue : productCh.getCharacteristic()
                .getCharacteristicValueList()) {
              if (!chValue.isSummaryLevel()) {
                ProductCharacteristicConf charConf = OBProvider.getInstance()
                    .get(ProductCharacteristicConf.class);
                charConf.setCharacteristicOfProduct(productCh);
                charConf.setOrganization(productCh.getOrganization());
                charConf.setCharacteristicValue(chValue);
                charConf.setCode(chValue.getCode());
                OBDal.getInstance().save(charConf);
              }
            }
          }
          // Subset Value cannot be set unless Generic Flag = Yes.
          // so Create configuration based on Characteristic Subset Values is skipped
        }
      }
    }

    validateCharacteristicDimensionsConfiguration(event);
    propagateCharacteristicDimensionsConfigurationFromGenericProductToVariantProducts(event);
  }

  private Product getProduct(EntityPersistenceEvent event) {
    return (Product) event.getTargetInstance();
  }

  private void validateCharacteristicDimensionsConfiguration(EntityPersistenceEvent event) {
    final Product product = getProduct(event);
    if (product.isEnableCharacteristicDimensions()) {
      if (!product.isGeneric() && product.getGenericProduct() == null) {
        throw new OBException("@CharacteristicDimensionValidation_GenericProduct@");
      }
      if (product.getRowCharacteristic() == null
          || product.getRowCharacteristic().equals(product.getColumnCharacteristic())) {
        throw new OBException("@CharacteristicDimensionValidation_Enabled@");
      }
    } else if (product.getRowCharacteristic() != null
        || product.getColumnCharacteristic() != null) {
      throw new OBException("@CharacteristicDimensionValidation_Disabled@");
    }
  }

  private void propagateCharacteristicDimensionsConfigurationFromGenericProductToVariantProducts(
      EntityPersistenceEvent event) {
    final Product product = getProduct(event);

    // If generic product, copy characteristic dimensions configuration to variant products
    if (product.isGeneric()) {
      // @formatter:off
      final String hql =
      " update Product" +
      "  set enableCharacteristicDimensions = :enableCharacteristicDimensions" +
      "   , rowCharacteristic = :rowCharacteristic" +
      "   , columnCharacteristic = :columnCharacteristic" +
      "   , updated = now()" +
      "  where genericProduct.id = :genericProductId";
      // @formatter:on

      OBDal.getInstance()
          .getSession()
          .createQuery(hql)
          .setParameter("enableCharacteristicDimensions",
              product.isEnableCharacteristicDimensions())
          .setParameter("rowCharacteristic", product.getRowCharacteristic())
          .setParameter("columnCharacteristic", product.getColumnCharacteristic())
          .setParameter("genericProductId", product.getId())
          .executeUpdate();
    }

    // If variant product, copy characteristic dimensions configuration from generic product
    else if (product.getGenericProduct() != null) {
      final Product genericProduct = product.getGenericProduct();
      final Entity productEntity = ModelProvider.getInstance().getEntity(Product.ENTITY_NAME);
      event.setCurrentState(
          productEntity.getProperty(Product.PROPERTY_ENABLECHARACTERISTICDIMENSIONS),
          genericProduct.isEnableCharacteristicDimensions());
      event.setCurrentState(productEntity.getProperty(Product.PROPERTY_ROWCHARACTERISTIC),
          genericProduct.getRowCharacteristic());
      event.setCurrentState(productEntity.getProperty(Product.PROPERTY_COLUMNCHARACTERISTIC),
          genericProduct.getColumnCharacteristic());
    }

  }
}
