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
 * All portions are Copyright (C) 2018-2024 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.referencedinventory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.refinventory.BoxProcessor;
import org.openbravo.materialmgmt.refinventory.ContentRestriction;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryUtil.SequenceType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventoryType;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;

/**
 * Tests exceptions are thrown
 */
public class ReferencedInventoryExceptionTest extends ReferencedInventoryBoxTest {

  protected ReferencedInventory testBox(final String _toBinId, final String productId,
      final String attributeSetInstanceId, final BigDecimal qtyInBox) throws Exception {
    return testBox(_toBinId, productId, attributeSetInstanceId, qtyInBox, null, false);
  }

  @Test
  public void testNoStorageDetailsProvidedToBox() throws Exception {
    final ReferencedInventoryType refInvType = ReferencedInventoryTestUtils
        .createReferencedInventoryType(OBDal.getInstance().getProxy(Organization.class, "0"),
            SequenceType.NONE, null, ContentRestriction.ONLY_ITEMS);
    final ReferencedInventory refInv = ReferencedInventoryTestUtils
        .createReferencedInventory(ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID, refInvType);

    OBException thrown = assertThrows(OBException.class, () -> {
      new BoxProcessor(refInv, null, null).createAndProcessGoodsMovement();
    });
    assertThat(thrown.getMessage(), containsString(OBMessageUtils.messageBD("NotSelected")));
  }

  @Test
  public void testNegativeStorageDetailProvidedToBox() throws Exception {
    final ReferencedInventoryType refInvType = ReferencedInventoryTestUtils
        .createReferencedInventoryType(OBDal.getInstance().getProxy(Organization.class, "0"),
            SequenceType.NONE, null, ContentRestriction.ONLY_ITEMS);
    final ReferencedInventory refInv = ReferencedInventoryTestUtils
        .createReferencedInventory(ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID, refInvType);

    final JSONObject storageDetailJS = new JSONObject();
    storageDetailJS.put("id", ReferencedInventoryTestUtils.getAnyStorageDetail());
    storageDetailJS.put("quantityOnHand", BigDecimal.ONE.negate());
    final JSONArray storageDetailsJS = new JSONArray();
    storageDetailsJS.put(storageDetailJS);

    OBException thrown = assertThrows(OBException.class, () -> {
      new BoxProcessor(refInv, storageDetailsJS, null).createAndProcessGoodsMovement();
    });
    assertThat(thrown.getMessage(),
        containsString(String.format(OBMessageUtils.messageBD("RefInv_NegativeQty"), "")));
  }

  @Test
  public void testZeroQtyStorageDetailProvidedToBox() throws Exception {
    final ReferencedInventoryType refInvType = ReferencedInventoryTestUtils
        .createReferencedInventoryType(OBDal.getInstance().getProxy(Organization.class, "0"),
            SequenceType.NONE, null, ContentRestriction.ONLY_ITEMS);
    final ReferencedInventory refInv = ReferencedInventoryTestUtils
        .createReferencedInventory(ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID, refInvType);

    final JSONObject storageDetailJS = new JSONObject();
    storageDetailJS.put("id", ReferencedInventoryTestUtils.getAnyStorageDetail());
    storageDetailJS.put("quantityOnHand", BigDecimal.ZERO);
    final JSONArray storageDetailsJS = new JSONArray();
    storageDetailsJS.put(storageDetailJS);

    OBException thrown = assertThrows(OBException.class, () -> {
      new BoxProcessor(refInv, storageDetailsJS, null).createAndProcessGoodsMovement();
    });
    assertThat(thrown.getMessage(),
        containsString(String.format(OBMessageUtils.messageBD("RefInv_NegativeQty"), "")));
  }

  @Test
  public void testBoxQtyGreaterThanQtyOnHand() throws Exception {
    final BigDecimal TWO_HUNDRED = new BigDecimal("200");

    OBException thrown = assertThrows(OBException.class, () -> {
      testBox(null, ReferencedInventoryTestUtils.PRODUCT_TSHIRT_ID, null, TWO_HUNDRED);
    });
    assertThat(thrown.getMessage(), containsString("(" + TWO_HUNDRED + ")"));
  }

  @Test
  public void testSameReferencedInventoryInDifferentBinsNotPossible() throws Exception {
    final ReferencedInventory refInv = testBox(ReferencedInventoryTestUtils.BIN_SPAIN_L02,
        ReferencedInventoryTestUtils.PRODUCT_TSHIRT_ID, null, BigDecimal.ONE);
    final List<StorageDetail> storageDetails = ReferencedInventoryTestUtils
        .getAvailableStorageDetailsOrderByQtyOnHand(
            refInv.getMaterialMgmtStorageDetailList().get(0).getProduct());
    final JSONArray storageDetailsJS = ReferencedInventoryTestUtils
        .getStorageDetailsToBoxJSArray(storageDetails.get(1), new BigDecimal("3"));

    OBException thrown = assertThrows(OBException.class, () -> {
      new BoxProcessor(refInv, storageDetailsJS, ReferencedInventoryTestUtils.BIN_SPAIN_L03)
          .createAndProcessGoodsMovement();
    });
    assertThat(thrown.getMessage(), containsString(" handling unit is also located in bin: "));
  }

  @Test
  public void testBoxMandatoryNewStorageBinParameter() throws Exception {
    final ReferencedInventoryType refInvType = ReferencedInventoryTestUtils
        .createReferencedInventoryType(OBDal.getInstance().getProxy(Organization.class, "0"),
            SequenceType.NONE, null, ContentRestriction.ONLY_ITEMS);
    final ReferencedInventory refInv = ReferencedInventoryTestUtils
        .createReferencedInventory(ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID, refInvType);

    final Product product = ReferencedInventoryTestUtils
        .cloneProduct(ReferencedInventoryTestUtils.PRODUCT_TSHIRT_ID);
    final BigDecimal receivedQty = new BigDecimal("10");
    ReferencedInventoryTestUtils.receiveProduct(product, receivedQty, null);

    final StorageDetail storageDetail = ReferencedInventoryTestUtils
        .getUniqueStorageDetail(product);

    OBException thrown = assertThrows(OBException.class, () -> {
      new BoxProcessor(refInv,
          ReferencedInventoryTestUtils.getStorageDetailsToBoxJSArray(storageDetail, BigDecimal.ONE),
          null).createAndProcessGoodsMovement();
    });
    assertThat(thrown.getMessage(),
        containsString(OBMessageUtils.messageBD("NewStorageBinParameterMandatory")));
  }
}
