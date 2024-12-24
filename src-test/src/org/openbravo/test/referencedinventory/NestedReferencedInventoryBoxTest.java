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
 * All portions are Copyright (C) 2024 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.referencedinventory;

import java.math.BigDecimal;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import org.codehaus.jettison.json.JSONArray;
import org.junit.After;
import org.junit.Test;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.refinventory.ContentRestriction;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;

/**
 * This is class to test Nested Referenced Inventory Functionalities.
 *
 */

public class NestedReferencedInventoryBoxTest extends ReferencedInventoryTest {

  private final String toBinId = BINS[0];

  @Test
  public void testIndividualBox() throws Exception {
    executeIndividualBoxTest();
  }

  /**
   *
   * Individual Box Test: new box
   *
   * Create small box with 1 stock product without attribute set instance and 1 product with
   * attribute set instance. Process the box movement. Verify the number of lines in the box
   * movement, unique item count, nested referenced inventories count in referenced inventory.
   * Verify storage details for Stock quantity, attribute set instance value for the product with
   * and without attribute set instance.
   */
  private Entry<ReferencedInventory, JSONArray> executeIndividualBoxTest() throws Exception {
    final ReferencedInventory refInv = NestedReferencedInventoryTestUtils.createReferencedInventory(
        ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID,
        ContentRestriction.BOTH_ITEMS_OR_REFINVENTORIES);

    JSONArray selectedStorageDetailsJS = new JSONArray();

    final Product firstProduct = ReferencedInventoryTestUtils.cloneProduct(PRODUCTS[0][0]);
    selectedStorageDetailsJS.put(NestedReferencedInventoryTestUtils
        .addProductInBox(firstProduct, BigDecimal.TEN, PRODUCTS[0][1])
        .get(0));

    final Product secondProduct = ReferencedInventoryTestUtils.cloneProduct(PRODUCTS[1][0]);
    selectedStorageDetailsJS.put(NestedReferencedInventoryTestUtils
        .addProductInBox(secondProduct, BigDecimal.TEN, PRODUCTS[1][1])
        .get(0));

    NestedReferencedInventoryTestUtils.boxAndValidateRefInventory(refInv, selectedStorageDetailsJS,
        toBinId, 2, 2L, 0L);

    NestedReferencedInventoryTestUtils.validateAttributeSetInstanceValue(refInv, firstProduct,
        BigDecimal.ONE, "[" + refInv.getSearchKey() + "]");
    NestedReferencedInventoryTestUtils.validateAttributeSetInstanceValue(refInv, secondProduct,
        BigDecimal.ONE, "Yellow[" + refInv.getSearchKey() + "]");

    return new SimpleEntry<>(refInv, selectedStorageDetailsJS);
  }

  /**
   * Individual Box Test: existing box
   *
   * Add product in existing Individual Box that contains 1 stock product without attribute set
   * instance and 1 product with attribute set instance.
   *
   * 1. Add product without attribute set instance in existing small box.
   *
   * 2. Add product with attribute set instance in existing small box.
   *
   * Process the new box movement. Verify the number of lines in the box movement. Verify unique
   * item count, nested referenced inventories count in referenced inventory. Verify Stock quantity
   * and attribute set instance value for storage detail of the product.
   */

  @Test
  public void testBoxSameProductInExistingRefInventory() throws Exception {
    final Entry<ReferencedInventory, JSONArray> refInvAndSelectedStorageDetails = executeIndividualBoxTest();
    final ReferencedInventory refInv = refInvAndSelectedStorageDetails.getKey();
    final JSONArray previousSelectedStorageDetailsJS = refInvAndSelectedStorageDetails.getValue();

    // Add product in without attribute set instance which is already present in Box, added during
    // previous Box transaction in the referenced inventory
    JSONArray selectedStorageDetailsJS = new JSONArray();
    selectedStorageDetailsJS.put(previousSelectedStorageDetailsJS.getJSONObject(0));
    NestedReferencedInventoryTestUtils.boxAndValidateRefInventory(refInv, selectedStorageDetailsJS,
        toBinId, 1, 2L, 0L);
    NestedReferencedInventoryTestUtils.validateAttributeSetInstanceValue(refInv, OBDal.getInstance()
        .getProxy(Product.class, selectedStorageDetailsJS.getJSONObject(0).getString("productId")),
        new BigDecimal(2), "[" + refInv.getSearchKey() + "]");

    // Add product in with attribute set instance which is already present in Box, added during
    // previous Box transaction in the referenced inventory
    selectedStorageDetailsJS = new JSONArray();
    selectedStorageDetailsJS.put(previousSelectedStorageDetailsJS.getJSONObject(1));
    NestedReferencedInventoryTestUtils.boxAndValidateRefInventory(refInv, selectedStorageDetailsJS,
        toBinId, 1, 2L, 0L);
    NestedReferencedInventoryTestUtils.validateAttributeSetInstanceValue(refInv, OBDal.getInstance()
        .getProxy(Product.class, selectedStorageDetailsJS.getJSONObject(0).getString("productId")),
        new BigDecimal(2), "Yellow[" + refInv.getSearchKey() + "]");
  }

  /**
   *
   * Nested Box Test: new parent box
   *
   * 1. Create child box using Individual Box Test.
   *
   * 2. Add child box inside a parent box. Process the new parent box movement. Verify the number of
   * lines in the parent box movement. Verify unique item count, nested referenced inventories count
   * in parent referenced inventory. Verify Stock quantity, attribute set instance value for storage
   * detail of the product with and without attribute set instance.
   */

  @Test
  public void testNestedReferencedInventory() throws Exception {
    executeNestedBoxTest();
  }

  /**
   * Return the outermost RI and the originally selected Storage Details
   */
  private SimpleEntry<ReferencedInventory, JSONArray> executeNestedBoxTest() throws Exception {
    Entry<ReferencedInventory, JSONArray> innerRIInfo = executeIndividualBoxTest();
    final ReferencedInventory refInv = innerRIInfo.getKey();
    final JSONArray storageDetailsForNestedRI = NestedReferencedInventoryTestUtils
        .getStorageDetailsforNestedRI(refInv, toBinId);

    // Create a parent Referenced Inventory
    final ReferencedInventory refInvParent = ReferencedInventoryTestUtils.createReferencedInventory(
        ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID, refInv.getReferencedInventoryType());

    NestedReferencedInventoryTestUtils.boxAndValidateRefInventory(refInvParent,
        storageDetailsForNestedRI, toBinId, 2, 2L, 1L);
    NestedReferencedInventoryTestUtils.validateAttributeSetInstanceValue(refInv, null, null,
        "Yellow[" + refInv.getSearchKey() + "]" + "[" + refInvParent.getSearchKey() + "]");
    NestedReferencedInventoryTestUtils.validateAttributeSetInstanceValue(refInv, null, null,
        "[" + refInv.getSearchKey() + "]" + "[" + refInvParent.getSearchKey() + "]");

    return new SimpleEntry<>(refInvParent, innerRIInfo.getValue());
  }

  /**
   * Nested Box Test: existing parent box
   *
   * Add product in existing Nested Box that contains a small box with 1 stock product without
   * attribute set instance and 1 product with attribute set instance.
   *
   * 1. Add product without attribute set instance in existing nested box.
   *
   * 2. Add product with attribute set instance in existing nested box.
   *
   * Process the new box movement. Verify the number of lines in the box movement. Verify unique
   * item count, nested referenced inventories count in parent referenced inventory. Verify Stock
   * quantity and attribute set instance value for storage detail of the product.
   */

  @Test
  public void testAddProductInNestedReferencedInventory() throws Exception {
    final Entry<ReferencedInventory, JSONArray> refInvAndSelectedStorageDetails = executeNestedBoxTest();
    final ReferencedInventory refInvParent = refInvAndSelectedStorageDetails.getKey();
    final JSONArray previousSelectedStorageDetailsJS = refInvAndSelectedStorageDetails.getValue();

    // Add product without attribute set instance which is already present in Box, added during
    // previous Box transaction in nested referenced inventory
    JSONArray selectedStorageDetailsJS = new JSONArray();
    selectedStorageDetailsJS.put(previousSelectedStorageDetailsJS.getJSONObject(0));
    NestedReferencedInventoryTestUtils.boxAndValidateRefInventory(refInvParent,
        selectedStorageDetailsJS, toBinId, 1, 2L, 1L);
    NestedReferencedInventoryTestUtils.validateAttributeSetInstanceValue(refInvParent,
        OBDal.getInstance()
            .getProxy(Product.class,
                previousSelectedStorageDetailsJS.getJSONObject(0).getString("productId")),
        BigDecimal.ONE, "[" + refInvParent.getSearchKey() + "]");

    // Add product with attribute set instance which is already present in Box, added during
    // previous Box transaction in nested referenced inventory
    selectedStorageDetailsJS = new JSONArray();
    selectedStorageDetailsJS.put(previousSelectedStorageDetailsJS.getJSONObject(1));
    NestedReferencedInventoryTestUtils.boxAndValidateRefInventory(refInvParent,
        selectedStorageDetailsJS, toBinId, 1, 2L, 1L);
    NestedReferencedInventoryTestUtils.validateAttributeSetInstanceValue(refInvParent,
        OBDal.getInstance()
            .getProxy(Product.class,
                previousSelectedStorageDetailsJS.getJSONObject(1).getString("productId")),
        BigDecimal.ONE, "Yellow[" + refInvParent.getSearchKey() + "]");
  }

  /**
   * 3 Level Boxing: add product & box at the same time in nested box.
   *
   * 1. Add product and individual box in medium box at the same time
   *
   * 2. Add medium box, 1 product without attribute and 1 product with attribute in Pallet
   *
   * Process the box movement at each step. Verify the number of lines in the box movement. Verify
   * unique item count, nested referenced inventories count in outer most referenced inventory.
   * Verify Stock quantity and attribute set instance value for storage detail of all stock product
   * in the referenced inventory.
   */

  @Test
  public void testAddProductAndNestedReferencedInventoryAtSameTime() throws Exception {
    final Entry<ReferencedInventory, JSONArray> smallBoxAndSelectedStorageDetails = executeIndividualBoxTest();
    final ReferencedInventory smallBox = smallBoxAndSelectedStorageDetails.getKey();
    final JSONArray previousSelectedStorageDetailsJS = smallBoxAndSelectedStorageDetails.getValue();

    // Create Medium Box with smallBox and a new Product in one step
    JSONArray storageDetailsForMediumBox = NestedReferencedInventoryTestUtils
        .getStorageDetailsforNestedRI(smallBox, toBinId);
    final Product thirdProduct = ReferencedInventoryTestUtils.cloneProduct(PRODUCTS[2][0]);
    JSONArray thirdProductSD = new JSONArray();
    thirdProductSD.put(NestedReferencedInventoryTestUtils
        .addProductInBox(thirdProduct, BigDecimal.TEN, PRODUCTS[2][1])
        .get(0));
    storageDetailsForMediumBox.put(thirdProductSD.get(0));
    final ReferencedInventory mediumBox = NestedReferencedInventoryTestUtils
        .createReferencedInventory(ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID,
            ContentRestriction.BOTH_ITEMS_OR_REFINVENTORIES);
    NestedReferencedInventoryTestUtils.boxAndValidateRefInventory(mediumBox,
        storageDetailsForMediumBox, toBinId, 3, 3L, 1L);

    // 3-level box: Create a Pallet and box in one step the Medium Box and two products (with and
    // without attributes) which exist before in the smallBox
    final JSONArray storageDetailsForPallet = NestedReferencedInventoryTestUtils
        .getStorageDetailsforNestedRI(mediumBox, toBinId);
    storageDetailsForPallet.put(previousSelectedStorageDetailsJS.getJSONObject(0));
    storageDetailsForPallet.put(previousSelectedStorageDetailsJS.getJSONObject(1));
    final ReferencedInventory pallet = NestedReferencedInventoryTestUtils.createReferencedInventory(
        ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID,
        ContentRestriction.BOTH_ITEMS_OR_REFINVENTORIES);
    NestedReferencedInventoryTestUtils.boxAndValidateRefInventory(pallet, storageDetailsForPallet,
        toBinId, 5, 3L, 2L);

    NestedReferencedInventoryTestUtils.validateAttributeSetInstanceValue(pallet,
        OBDal.getInstance()
            .getProxy(Product.class,
                previousSelectedStorageDetailsJS.getJSONObject(0).getString("productId")),
        BigDecimal.ONE, "[" + pallet.getSearchKey() + "]");

    NestedReferencedInventoryTestUtils.validateAttributeSetInstanceValue(pallet,
        OBDal.getInstance()
            .getProxy(Product.class,
                previousSelectedStorageDetailsJS.getJSONObject(1).getString("productId")),
        BigDecimal.ONE, "Yellow[" + pallet.getSearchKey() + "]");

    NestedReferencedInventoryTestUtils.validateAttributeSetInstanceValue(mediumBox, thirdProduct,
        BigDecimal.ONE, "#015[" + mediumBox.getSearchKey() + "][" + pallet.getSearchKey() + "]");

    NestedReferencedInventoryTestUtils.validateAttributeSetInstanceValue(smallBox,
        OBDal.getInstance()
            .getProxy(Product.class,
                previousSelectedStorageDetailsJS.getJSONObject(0).getString("productId")),
        BigDecimal.ONE, "[" + smallBox.getSearchKey() + "][" + mediumBox.getSearchKey() + "]["
            + pallet.getSearchKey() + "]");

    NestedReferencedInventoryTestUtils.validateAttributeSetInstanceValue(smallBox,
        OBDal.getInstance()
            .getProxy(Product.class,
                previousSelectedStorageDetailsJS.getJSONObject(1).getString("productId")),
        BigDecimal.ONE, "Yellow[" + smallBox.getSearchKey() + "][" + mediumBox.getSearchKey() + "]["
            + pallet.getSearchKey() + "]");
  }

  @Override
  @After
  public void clearSession() {
    OBDal.getInstance().rollbackAndClose();
  }
}
