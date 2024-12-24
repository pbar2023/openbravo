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
 * All portions are Copyright (C) 2023-2024 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Characteristic;
import org.openbravo.model.common.plm.Product;

public class ProductEventHandlerTest extends WeldBaseTest {

  private static final String FB_CLIENT = "23C59575B9CF467C9620760EB255B389";
  private static final String FB_ADMIN_ROLE = "42D0EEB1C66F497A90DD526DC597E6F0";
  private static final String FB_ADMIN_USER = "A530AAE22C864702B7E1C22D58E7B17B";
  private static final String FB_ES_ORG = "B843C30461EA4501935CB1D125C9C25A";
  private static final String ORANGE_JUICE_PRODUCT = "61047A6B06B3452B85260C7BCF08E78D";
  private static final String CUSTOM_WATER_BOTTLE_PRODUCT = "78CACC94B1FE4AE6A9A7CF6D9F0AD25F";
  private static final String COLOR_CHARACTERISTIC = "4F993B818D3548FFA70DE633E4E69AC2";
  private static final String SIZE_CHARACTERISTIC = "7B619A9E101F4F9B9E664B6CD877FC0D";

  @Rule
  public ParameterCdiTestRule<TestData> parameterValuesRule = new ParameterCdiTestRule<>(
      Arrays.asList(//
          new TestData("No generic product with characteristic dimensions disabled", //
              false, false, false, null, null, null), //
          new TestData("No generic product with characteristic dimensions enabled", //
              false, false, true, null, null, "@CharacteristicDimensionValidation_GenericProduct@"), //

          new TestData("Generic product with characteristic dimensions disabled", //
              true, false, false, null, null, null), //

          new TestData(
              "Generic product with characteristic dimensions disabled and row characteristic defined", //
              true, false, false, COLOR_CHARACTERISTIC, null,
              "@CharacteristicDimensionValidation_Disabled@"), //
          new TestData(
              "Generic product with characteristic dimensions disabled and column characteristic defined", //
              true, false, false, null, SIZE_CHARACTERISTIC,
              "@CharacteristicDimensionValidation_Disabled@"), //
          new TestData(
              "Generic product with characteristic dimensions disabled and row and column characteristic defined", //
              true, false, false, COLOR_CHARACTERISTIC, SIZE_CHARACTERISTIC,
              "@CharacteristicDimensionValidation_Disabled@"), //

          new TestData("Variant product with characteristic dimensions disabled", //
              false, true, false, null, null, null), //

          new TestData(
              "Variant product with characteristic dimensions disabled and row characteristic defined", //
              false, true, false, COLOR_CHARACTERISTIC, null,
              "@CharacteristicDimensionValidation_Disabled@"), //
          new TestData(
              "Variant product with characteristic dimensions disabled and column characteristic defined", //
              false, true, false, null, SIZE_CHARACTERISTIC,
              "@CharacteristicDimensionValidation_Disabled@"), //
          new TestData(
              "Variant product with characteristic dimensions disabled and row and column characteristic defined", //
              false, true, false, COLOR_CHARACTERISTIC, SIZE_CHARACTERISTIC,
              "@CharacteristicDimensionValidation_Disabled@"), //

          new TestData(
              "Generic product with characteristic dimensions enabled and row and column characteristics undefined", //
              true, false, true, null, null, "@CharacteristicDimensionValidation_Enabled@"), //
          new TestData(
              "Generic product with characteristic dimensions enabled and row characteristic defined", //
              true, false, true, COLOR_CHARACTERISTIC, null, null), //
          new TestData(
              "Generic product with characteristic dimensions enabled and column characteristic defined", //
              true, false, true, null, SIZE_CHARACTERISTIC,
              "@CharacteristicDimensionValidation_Enabled@"), //
          new TestData(
              "Generic product with characteristic dimensions enabled and row and column characteristics defined", //
              true, false, true, COLOR_CHARACTERISTIC, SIZE_CHARACTERISTIC, null), //

          new TestData(
              "Variant product with characteristic dimensions enabled and row and column characteristics undefined", //
              false, true, true, null, null, "@CharacteristicDimensionValidation_Enabled@"), //
          new TestData(
              "Variant product with characteristic dimensions enabled and row characteristic defined", //
              false, true, true, COLOR_CHARACTERISTIC, null, null), //
          new TestData(
              "Variant product with characteristic dimensions enabled and column characteristic defined", //
              false, true, true, null, SIZE_CHARACTERISTIC,
              "@CharacteristicDimensionValidation_Enabled@"), //
          new TestData(
              "Variant product with characteristic dimensions enabled and row and column characteristics defined", //
              false, true, true, COLOR_CHARACTERISTIC, SIZE_CHARACTERISTIC, null) //
      ));

  private @ParameterCdiTest TestData testData;

  @Before
  public void beforeTest() {
    OBContext.setOBContext(FB_ADMIN_USER, FB_ADMIN_ROLE, FB_CLIENT, FB_ES_ORG);
  }

  @After
  public void afterTest() {
    rollback();
  }

  @Test
  public void productEventHandlerTest() {
    final Product product = OBDal.getInstance()
        .get(Product.class,
            testData.isGenericProduct() ? CUSTOM_WATER_BOTTLE_PRODUCT : ORANGE_JUICE_PRODUCT);
    assertThat(
        "Product event handler should throw the correct exception: " + testData.getDescription(),
        updateProduct(product), equalTo(testData.getError()));

    if (testData.getError() == null) {
      if (testData.isGenericProduct()) {
        product.getProductGenericProductList()
            .forEach(variantProduct -> assertProduct(product, variantProduct));
      } else if (testData.isVariantProduct()) {
        assertProduct(product.getGenericProduct(), product);
      }
    }
  }

  private void assertProduct(final Product genericProduct, Product variantProduct) {
    assertThat(
        "Variant product should have the same characteristic dimensions configuration than generic product: "
            + testData.getDescription(),
        variantProduct.isEnableCharacteristicDimensions(),
        equalTo(genericProduct.isEnableCharacteristicDimensions()));
    assertThat(
        "Variant product should have the same row characteristic than generic product: "
            + testData.getDescription(),
        variantProduct.isEnableCharacteristicDimensions(),
        equalTo(genericProduct.isEnableCharacteristicDimensions()));
    assertThat(
        "Variant product should have the same column characteristic than generic product: "
            + testData.getDescription(),
        variantProduct.isEnableCharacteristicDimensions(),
        equalTo(genericProduct.isEnableCharacteristicDimensions()));
  }

  private String updateProduct(Product product) {
    try {
      product.setGeneric(testData.isGenericProduct());
      product.setGenericProduct(testData.isVariantProduct()
          ? OBDal.getInstance().get(Product.class, CUSTOM_WATER_BOTTLE_PRODUCT)
          : null);
      product.setEnableCharacteristicDimensions(testData.hasCharacteristicDimensions());
      product.setRowCharacteristic(Optional.ofNullable(testData.getRowCharacteristic())
          .map(id -> OBDal.getInstance().get(Characteristic.class, id))
          .orElse(null));
      product.setColumnCharacteristic(Optional.ofNullable(testData.getColumnCharacteristic())
          .map(id -> OBDal.getInstance().get(Characteristic.class, id))
          .orElse(null));
      OBDal.getInstance().save(product);
      OBDal.getInstance().flush();
      return null;
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  static class TestData {
    private final String description;
    private final boolean isGenericProduct;
    private final boolean isVariantProduct;
    private final boolean hasCharacteristicDimensions;
    private final String rowCharacteristic;
    private final String columnCharacteristic;
    private final String error;

    public TestData(String description, boolean isGenericProduct, boolean isVariantProduct,
        boolean hasCharacteristicDimensions, String rowCharacteristic, String columnCharacteristic,
        String error) {
      this.description = description;
      this.isGenericProduct = isGenericProduct;
      this.isVariantProduct = isVariantProduct;
      this.hasCharacteristicDimensions = hasCharacteristicDimensions;
      this.rowCharacteristic = rowCharacteristic;
      this.columnCharacteristic = columnCharacteristic;
      this.error = error;
    }

    String getDescription() {
      return description;
    }

    boolean isGenericProduct() {
      return isGenericProduct;
    }

    boolean isVariantProduct() {
      return isVariantProduct;
    }

    boolean hasCharacteristicDimensions() {
      return hasCharacteristicDimensions;
    }

    String getRowCharacteristic() {
      return rowCharacteristic;
    }

    String getColumnCharacteristic() {
      return columnCharacteristic;
    }

    String getError() {
      return error;
    }
  }
}
