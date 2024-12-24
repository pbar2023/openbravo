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
package org.openbravo.test.costing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.cost.CostAdjustment;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.test.costing.assertclass.CostAdjustmentAssert;
import org.openbravo.test.costing.assertclass.ProductCostingAssert;
import org.openbravo.test.costing.assertclass.ProductTransactionAssert;
import org.openbravo.test.costing.utils.TestCostingConstants;
import org.openbravo.test.costing.utils.TestCostingUtils;

public class TestIssue55837 extends TestCostingBase {

  @Test
  public void testIssue55837OneLineWithQty1() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("10.00");
    final BigDecimal price2 = new BigDecimal("50.00");
    final BigDecimal quantity1 = new BigDecimal("1");

    try {

      OBContext.setOBContext(TestCostingConstants.OPENBRAVO_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testIssue55837OneLineWithQty1", price1);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(Arrays.asList(product),
          Arrays.asList(price1), Arrays.asList(quantity1), day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1, null,
          day1);

      // Create purchase invoice, post it and assert it
      Invoice purchaseInvoice = TestCostingUtils.createPurchaseInvoice(purchaseOrder, price1,
          quantity1, day2);

      // Update purchase invoice line product price
      TestCostingUtils.updatePurchaseInvoice(purchaseInvoice, price2);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price2, price1, price2, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          new BigDecimal("40"), day2, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testIssue55837OneLineWithQty2() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("10.00");
    final BigDecimal price2 = new BigDecimal("50.00");
    final BigDecimal quantity1 = new BigDecimal("2");

    try {

      OBContext.setOBContext(TestCostingConstants.OPENBRAVO_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testIssue55837OneLineWithQty2", price1);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(Arrays.asList(product),
          Arrays.asList(price1), Arrays.asList(quantity1), day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1, null,
          day1);

      // Create purchase invoice, post it and assert it
      Invoice purchaseInvoice = TestCostingUtils.createPurchaseInvoice(purchaseOrder, price1,
          quantity1, day2);

      // Update purchase invoice line product price
      TestCostingUtils.updatePurchaseInvoice(purchaseInvoice, price2);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price2, price1, price2, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          new BigDecimal("80"), day2, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testIssue55837TwoLineWithQty1() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("10.00");
    final BigDecimal price2 = new BigDecimal("50.00");
    final BigDecimal quantity1 = new BigDecimal("1");

    try {

      OBContext.setOBContext(TestCostingConstants.OPENBRAVO_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = TestCostingUtils.createProduct("testIssue55837TwoLineWithQty1", price1);

      // Create purchase order and book it
      Order purchaseOrder = TestCostingUtils.createPurchaseOrder(Arrays.asList(product, product),
          Arrays.asList(price1, price1), Arrays.asList(quantity1, quantity1), day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = TestCostingUtils.createGoodsReceipt(purchaseOrder, price1, null,
          day1);

      // Create purchase invoice, post it and assert it
      Invoice purchaseInvoice = TestCostingUtils.createPurchaseInvoice(purchaseOrder, price1,
          quantity1, day2);

      // Update purchase invoice line product price
      TestCostingUtils.updatePurchaseInvoice(purchaseInvoice, price2);

      // Run price correction background
      TestCostingUtils.runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList()
          .get(1), price1, price2));
      TestCostingUtils.assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = TestCostingUtils
          .getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(0), price2, price1, price2, quantity1));
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(1), price2, price1, price2, quantity1));
      TestCostingUtils.assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = TestCostingUtils.getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1
          .add(new CostAdjustmentAssert(null, "PDC", new BigDecimal("40"), day2, true));
      costAdjustmentAssertLineList1
          .add(new CostAdjustmentAssert(null, "PDC", new BigDecimal("40"), day2, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      TestCostingUtils.assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

}
