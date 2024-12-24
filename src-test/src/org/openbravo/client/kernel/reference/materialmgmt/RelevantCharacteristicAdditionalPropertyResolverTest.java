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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.ProductCharacteristicTestUtils;
import org.openbravo.model.common.plm.Characteristic;
import org.openbravo.model.common.plm.CharacteristicValue;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.service.datasource.DataSourceProperty;
import org.openbravo.service.datasource.DataSourceServiceProvider;
import org.openbravo.service.datasource.DefaultDataSourceService;
import org.openbravo.service.json.AdditionalPropertyResolver;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.DataToJsonConverter;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.test.base.TestConstants;

/**
 * Test cases for the {@link RelevantCharacteristicAdditionalPropertyResolver} hook
 */
public class RelevantCharacteristicAdditionalPropertyResolverTest extends WeldBaseTest {
  private static final String PRODUCT_ID = "DA7FC1BB3BA44EC48EC1AB9C74168CED";
  private static final String PRODUCT_PRICE_ID = "316F95A165914A538D923F3CA815E4D4";

  @Inject
  private DataSourceServiceProvider dataSourceServiceProvider;

  @Inject
  @Any
  private Instance<AdditionalPropertyResolver> resolvers;

  private String characteristicValueId;

  @Before
  public void initialize() {
    try {
      OBContext.setAdminMode(false);
      setProductCharacteristicConfiguration();
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @After
  public void cleanUp() {
    OBDal.getInstance().rollbackAndClose();
  }

  @Test
  public void characteristicPropertyCanBeResolved() {
    Entity product = ModelProvider.getInstance().getEntity(Product.class);
    assertThat(canBeResolvedAsAdditionalProperty(product, "mColor"), equalTo(true));
  }

  @Test
  public void productCharacteristicPropertyCanBeResolved() {
    Entity productPrice = ModelProvider.getInstance().getEntity(ProductPrice.class);
    assertThat(canBeResolvedAsAdditionalProperty(productPrice, "product.mColor"), equalTo(true));
  }

  @Test
  public void unknownPropertyCanBeResolved() {
    Entity product = ModelProvider.getInstance().getEntity(Product.class);
    assertThat(canBeResolvedAsAdditionalProperty(product, "mUnknown"), equalTo(false));
  }

  @Test
  public void unknownProductCharacteristicPropertyCanBeResolved() {
    Entity productPrice = ModelProvider.getInstance().getEntity(ProductPrice.class);
    assertThat(canBeResolvedAsAdditionalProperty(productPrice, "product.mUnknown"), equalTo(false));
  }

  @Test
  public void propertyIsResolvedByDataToJsonConverter() throws JSONException {
    DataToJsonConverter converter = new DataToJsonConverter();
    converter.setAdditionalProperties(List.of("mColor"));
    Product product = OBDal.getInstance().get(Product.class, PRODUCT_ID);

    JSONObject json = converter.toJsonObject(product, DataResolvingMode.FULL);

    CharacteristicValue chValue = OBDal.getInstance()
        .get(CharacteristicValue.class, characteristicValueId);
    assertThat(json.getString("mColor"), equalTo(chValue.getId()));
    assertThat(json.getString("mColor$_identifier"), equalTo(chValue.getIdentifier()));
    assertThat(json.has("mColor$sequenceNumber"), equalTo(true));
  }

  @Test
  public void productPropertyIsResolvedByDataToJsonConverter() throws JSONException {
    DataToJsonConverter converter = new DataToJsonConverter();
    converter.setAdditionalProperties(List.of("product.mColor"));
    ProductPrice product = OBDal.getInstance().get(ProductPrice.class, PRODUCT_PRICE_ID);

    JSONObject json = converter.toJsonObject(product, DataResolvingMode.FULL);

    CharacteristicValue chValue = OBDal.getInstance()
        .get(CharacteristicValue.class, characteristicValueId);
    assertThat(json.getString("product$mColor"), equalTo(chValue.getId()));
    assertThat(json.getString("product$mColor$_identifier"), equalTo(chValue.getIdentifier()));
    assertThat(json.has("product$mColor$sequenceNumber"), equalTo(true));
  }

  @Test
  public void getDataSourcePropertiesForProduct() {
    Map<String, Object> parameters = Map.of(JsonConstants.ADDITIONAL_PROPERTIES_PARAMETER,
        "mColor");
    Entity product = ModelProvider.getInstance().getEntity(Product.class);

    List<DataSourceProperty> properties = getDataSourceProperties(product, parameters);

    assertThat(properties.stream().anyMatch(p -> p.getName().equals("mColor")), equalTo(true));
    assertThat(properties.stream().anyMatch(p -> p.getName().equals("mColor$sequenceNumber")),
        equalTo(true));
  }

  @Test
  public void getDataSourcePropertiesForProductRelatedEntity() {
    Map<String, Object> parameters = Map.of(JsonConstants.ADDITIONAL_PROPERTIES_PARAMETER,
        "product.mColor");
    Entity productPrice = ModelProvider.getInstance().getEntity(ProductPrice.class);

    List<DataSourceProperty> properties = getDataSourceProperties(productPrice, parameters);

    assertThat(properties.stream().anyMatch(p -> p.getName().equals("product$mColor")),
        equalTo(true));
    assertThat(
        properties.stream().anyMatch(p -> p.getName().equals("product$mColor$sequenceNumber")),
        equalTo(true));
  }

  @Test
  public void doNotGetDataSourcePropertiesForProductIfNotRequested() {
    Entity product = ModelProvider.getInstance().getEntity(Product.class);

    List<DataSourceProperty> properties = getDataSourceProperties(product, Collections.emptyMap());

    assertThat(properties.stream().anyMatch(p -> p.getName().equals("mColor")), equalTo(false));
    assertThat(properties.stream().anyMatch(p -> p.getName().equals("mColor$sequenceNumber")),
        equalTo(false));
  }

  @Test
  public void provideCharacteristicPropertyNameForProductEntity() {
    Entity product = ModelProvider.getInstance().getEntity(Product.class);
    assertThat(getNamesOfAdditionalProperties(product).anyMatch(p -> p.equals("mColor")),
        equalTo(true));
  }

  @Test
  public void doNotProvideCharacteristicPropertyNameForNonProductEntity() {
    Entity productPrice = ModelProvider.getInstance().getEntity(ProductPrice.class);
    assertThat(getNamesOfAdditionalProperties(productPrice).anyMatch(p -> p.equals("mColor")),
        equalTo(false));
  }

  private void setProductCharacteristicConfiguration() {
    ProductCharacteristicTestUtils.addRelevantCharacteristic("M_Color", "Color",
        TestConstants.Modules.ID_CORE);
    Characteristic characteristic = ProductCharacteristicTestUtils
        .createCharacteristicLinkedToRelevant("Color", "M_Color");
    CharacteristicValue chValue = ProductCharacteristicTestUtils.createCharacteristicValue("Blue",
        "B", characteristic.getId());
    characteristicValueId = chValue.getId();
    ProductCharacteristicTestUtils.assignCharacteristicValueToProduct(PRODUCT_ID,
        characteristicValueId);
    ProductCharacteristicTestUtils.reloadRelevantCharacteristicsCache();
  }

  private boolean canBeResolvedAsAdditionalProperty(Entity entity, String property) {
    return resolvers.stream().anyMatch(r -> r.canResolve(entity, property));
  }

  private List<DataSourceProperty> getDataSourceProperties(Entity entity,
      Map<String, Object> parameters) {
    DefaultDataSourceService dataSource = (DefaultDataSourceService) dataSourceServiceProvider
        .getDataSource(entity.getName());
    List<DataSourceProperty> properties;
    try {
      OBContext.setAdminMode(true);
      properties = dataSource.getDataSourceProperties(parameters);
    } finally {
      OBContext.restorePreviousMode();
    }
    return properties;
  }

  private Stream<String> getNamesOfAdditionalProperties(Entity entity) {
    return resolvers.stream()
        .map(resolver -> resolver.getPropertyNames(entity))
        .flatMap(Collection::stream);
  }
}
