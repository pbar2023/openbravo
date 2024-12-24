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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.plm.CharacteristicValue;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.test.base.TestConstants;

/**
 * Test cases for {@link RelevantCharacteristicProperty}
 */
public class RelevantCharacteristicPropertyTest extends WeldBaseTest {
  private static final String PRODUCT_ID = "DA7FC1BB3BA44EC48EC1AB9C74168CED";
  private static final String PRODUCT_PRICE_ID = "316F95A165914A538D923F3CA815E4D4";

  private String characteristicId;

  @Before
  public void initialize() {
    ProductCharacteristicTestUtils.addRelevantCharacteristic("M_Color", "Color",
        TestConstants.Modules.ID_CORE);
    characteristicId = ProductCharacteristicTestUtils
        .createCharacteristicLinkedToRelevant("Color", "M_Color")
        .getId();
    CharacteristicValue chValue = ProductCharacteristicTestUtils.createCharacteristicValue("Red",
        "R", characteristicId);
    ProductCharacteristicTestUtils.assignCharacteristicValueToProduct(PRODUCT_ID, chValue.getId());
    OBDal.getInstance().flush();
    ProductCharacteristicTestUtils.reloadRelevantCharacteristicsCache();
  }

  @After
  public void cleanUp() {
    OBDal.getInstance().rollbackAndClose();
  }

  @Test
  public void newRelevantCharacteristicPropertyIsAvailable() {
    assertThat(
        RelevantCharacteristicProperty.getRelevantCharateristicProperties().contains("mColor"),
        equalTo(true));
  }

  @Test
  public void getProperty() {
    Entity product = ModelProvider.getInstance().getEntity(Product.class);
    RelevantCharacteristicProperty property = RelevantCharacteristicProperty.from(product, "mColor")
        .orElseThrow();

    assertThat(property.getName(), equalTo("mColor"));
    assertThat(property.getEntity().getName(), equalTo("Product"));
    assertThat(property.getSearchKey(), equalTo("M_Color"));
    assertThat(property.getCharacteristicId(), equalTo(characteristicId));
    assertThat(property.getFieldName(), equalTo("Color"));
    assertThat(property.getDescription(),
        equalTo("Color relevant characteristic for testing purposes"));
  }

  @Test
  public void getPropertyFromProductRelatedEntity() {
    Entity productPrice = ModelProvider.getInstance().getEntity(ProductPrice.class);
    RelevantCharacteristicProperty property = RelevantCharacteristicProperty
        .from(productPrice, "product.mColor")
        .orElseThrow();

    assertThat(property.getName(), equalTo("mColor"));
    assertThat(property.getEntity().getName(), equalTo("PricingProductPrice"));
    assertThat(property.getSearchKey(), equalTo("M_Color"));
    assertThat(property.getCharacteristicId(), equalTo(characteristicId));
    assertThat(property.getFieldName(), equalTo("Color"));
    assertThat(property.getDescription(),
        equalTo("Color relevant characteristic for testing purposes"));
  }

  @Test
  public void getPropertyFromField() {
    Field field = ProductCharacteristicTestUtils.createPropertyField(
        TestConstants.Tabs.PRODUCT_HEADER, "mColor", TestConstants.Modules.ID_CORE);
    try {
      OBContext.setAdminMode(true);
      assertThat(RelevantCharacteristicProperty.from(field).isPresent(), equalTo(true));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void getPropertyFromNonPropertyField() {
    Field nonPropertyField = OBProvider.getInstance().get(Field.class);
    try {
      OBContext.setAdminMode(true);
      assertThat(RelevantCharacteristicProperty.from(nonPropertyField).isPresent(), equalTo(false));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void cannotGetPropertyFromNonProductEntity() {
    Entity businessPartner = ModelProvider.getInstance().getEntity(BusinessPartner.class);
    assertThat(RelevantCharacteristicProperty.from(businessPartner, "mColor").isPresent(),
        equalTo(false));
  }

  @Test
  public void cannotGetUnknownProperty() {
    Entity product = ModelProvider.getInstance().getEntity(Product.class);
    assertThat(RelevantCharacteristicProperty.from(product, "mUnknown").isPresent(),
        equalTo(false));
  }

  @Test
  public void getPropertyValue() {
    Entity productEntity = ModelProvider.getInstance().getEntity(Product.class);
    RelevantCharacteristicProperty property = RelevantCharacteristicProperty
        .from(productEntity, "mColor")
        .orElseThrow();
    Product product = OBDal.getInstance().get(Product.class, PRODUCT_ID);
    assertThat(property.getCharacteristicValue(product).getName(), equalTo("Red"));
  }

  @Test
  public void getPropertyValueFromProductRelatedBOB() {
    Entity productPriceEntity = ModelProvider.getInstance().getEntity(ProductPrice.class);
    RelevantCharacteristicProperty property = RelevantCharacteristicProperty
        .from(productPriceEntity, "product.mColor")
        .orElseThrow();
    ProductPrice productPrice = OBDal.getInstance().get(ProductPrice.class, PRODUCT_PRICE_ID);
    assertThat(property.getCharacteristicValue(productPrice).getName(), equalTo("Red"));
  }
}
