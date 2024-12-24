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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.math.BigDecimal;

import org.codehaus.jettison.json.JSONArray;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.refinventory.BoxProcessor;
import org.openbravo.materialmgmt.refinventory.ContentRestriction;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryProcessor.StorageDetailJS;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryUtil;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryUtil.SequenceType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventoryType;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;

/**
 * This is Utility class used in Nested Referenced Inventory Tests.
 *
 */

public class NestedReferencedInventoryTestUtils {

  /**
   * validate referenced inventory after box transaction
   */
  static void boxAndValidateRefInventory(final ReferencedInventory refInv,
      JSONArray selectedStorageDetailsJS, String toBinId, Integer noOfLines, Long uniqueItemCount,
      Long nestedRefInvCount) throws Exception {
    InternalMovement boxMovement = new BoxProcessor(refInv, selectedStorageDetailsJS, toBinId)
        .createAndProcessGoodsMovement();

    OBDal.getInstance().refresh(boxMovement);
    OBDal.getInstance().refresh(refInv);

    assertThat("Box movement must be processed", boxMovement.isProcessed(), equalTo(true));

    if (noOfLines != null) {
      assertThat("Box Movement does not have " + noOfLines + "lines",
          boxMovement.getMaterialMgmtInternalMovementLineList().size(), equalTo(noOfLines));
    }
    if (uniqueItemCount != null) {
      assertThat("Nested Referenced Inventory does not have Unique Items Count equal to "
          + uniqueItemCount.intValue(), refInv.getUniqueItemsCount(), equalTo(uniqueItemCount));
    }

    if (nestedRefInvCount != null) {
      assertThat(
          "Nested Referenced Inventory Count is not equal to " + nestedRefInvCount.intValue(),
          refInv.getNestedReferencedInventoriesCount(), equalTo(nestedRefInvCount));
    }

    assertThat("Expected Referenced Inventory Locator", refInv.getStorageBin().getId(),
        equalTo(toBinId));
    assertThat("Expected Referenced Inventory Warehouse",
        refInv.getStorageBin().getWarehouse().getId(),
        equalTo(ReferencedInventoryTestUtils.QA_SPAIN_WAREHOUSE_ID));
  }

  /**
   * get Storage details for Nested RI
   */
  static JSONArray getStorageDetailsforNestedRI(final ReferencedInventory refInv, String toBinId) {
    final JSONArray storageDetailJS = new JSONArray();
    try (ScrollableResults sdScroll = ReferencedInventoryUtil.getStorageDetails(refInv.getId(),
        true)) {
      while (sdScroll.next()) {
        final StorageDetail sd = (StorageDetail) sdScroll.get(0);
        final StorageDetailJS sdJS = new StorageDetailJS(sd.getId(), sd.getQuantityOnHand(),
            toBinId);
        storageDetailJS.put(sdJS.toJSONObject());
      }
    }
    return storageDetailJS;
  }

  /**
   * Add items in referenced inventory during boxing transaction
   */
  static JSONArray addProductInBox(final Product product, final BigDecimal qty,
      final String attributeSetInstanceId) throws Exception {
    ReferencedInventoryTestUtils.receiveProduct(product, qty, attributeSetInstanceId);
    final StorageDetail storageDetail = ReferencedInventoryTestUtils
        .getUniqueStorageDetail(product);
    return ReferencedInventoryTestUtils.getStorageDetailsToBoxJSArray(storageDetail,
        BigDecimal.ONE);
  }

  /**
   * Validate product and attribute set instance value in the referenced inventory
   */
  public static void validateAttributeSetInstanceValue(final ReferencedInventory refInv,
      final Product product, final BigDecimal qtyOnHand,
      final String attributeSetInstanceDescription) {
    assertThat(
        "Product with Quantity On Hand and Attribute Set Instance does not exists in Referenced Inventory",
        storageDetailExists(refInv, product, qtyOnHand, attributeSetInstanceDescription),
        equalTo(true));
  }

  /**
   * Check whether storage detail exists for product and attribute set instance value in the
   * referenced inventory
   */
  static boolean storageDetailExists(final ReferencedInventory refInv, final Product product,
      final BigDecimal qtyOnHand, final String attributeSetInstanceDescription) {
    OBCriteria<StorageDetail> crit = OBDal.getInstance().createCriteria(StorageDetail.class);
    crit.createAlias(StorageDetail.PROPERTY_ATTRIBUTESETVALUE, "att");
    crit.add(Restrictions.eq(StorageDetail.PROPERTY_REFERENCEDINVENTORY, refInv));
    if (product != null) {
      crit.add(Restrictions.eq(StorageDetail.PROPERTY_PRODUCT, product));
    }
    if (qtyOnHand != null) {
      crit.add(Restrictions.eq(StorageDetail.PROPERTY_QUANTITYONHAND, qtyOnHand));
    }
    if (attributeSetInstanceDescription != null) {
      crit.add(Restrictions.eq("att." + AttributeSetInstance.PROPERTY_DESCRIPTION,
          attributeSetInstanceDescription));
    }
    crit.setMaxResults(1);
    return !crit.list().isEmpty();
  }

  /**
   * Session is cleared when box and unbox RI processes are executed, so to get updated information
   * about RI we need to re-initialize RI object and get refreshed RI.
   */
  public static ReferencedInventory getRefreshedReferencedInventory(String refInventoryId) {
    OBDal.getInstance().getSession().clear();
    ReferencedInventory refInventory = OBDal.getInstance()
        .get(ReferencedInventory.class, refInventoryId);
    OBDal.getInstance().refresh(refInventory);
    return refInventory;
  }

  /**
   * Creates a referenced inventory with referenced inventory type having contentRestriction
   */
  static ReferencedInventory createReferencedInventory(String orgId,
      ContentRestriction contentRestriction) {
    final ReferencedInventoryType refInvType = ReferencedInventoryTestUtils
        .createReferencedInventoryType(OBDal.getInstance().getProxy(Organization.class, "0"),
            SequenceType.NONE, null, contentRestriction);
    return ReferencedInventoryTestUtils.createReferencedInventory(orgId, refInvType);
  }
}
