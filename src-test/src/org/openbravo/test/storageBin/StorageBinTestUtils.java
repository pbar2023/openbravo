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
package org.openbravo.test.storageBin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.LocatorHandlingUnitType;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventoryType;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.service.db.CallStoredProcedure;

/**
 * This is a utility class for storage bin tests
 */

public class StorageBinTestUtils {

  // Client QA Testing
  public static final String CLIENT_ID = "4028E6C72959682B01295A070852010D";
  // Organization *
  public static final String ORG_STAR_ID = "0";
  // Organization USA
  public static final String ORG_ID = "5EFF95EB540740A3B10510D9814EFAD5";
  // User Openbravo
  public static final String USER_ID = "100";
  // Role QA Administrator
  public static final String ROLE_ID = "4028E6C72959682B01295A071429011E";
  // Language encoding English US
  public static final String LANGUAGE_CODE = "en_US";

  // StorageBin USA111
  public static final String LOCATOR_USA111_ID = "4028E6C72959682B01295ECFE4E50273";
  // Product Distribution Goods A
  public static final String PRODUCT_DGA_ID = "4028E6C72959682B01295ADC211E0237";
  // Original Goods Receipt
  public static final String GOODS_RECEIPT_ID = "AB9230E0C7974BC4A6E5429673FF9460";
  // Original Goods Shipment
  public static final String GOODS_SHIPMENT_ID = "8C43DC1BFB514C188BAC0246A36ED4A0";

  // Document Status and Actions
  private static final String COMPLETED_DOCUMENT = "CO";
  private static final String DRAFT_STATUS = "DR";
  private static final String COMPLETE_ACTION = "CO";

  // Process to complete Documents
  private static final String PROCESS_SHIPMENT_RECEIPT = "m_inout_post";

  // USA warehouse
  public static final String QA_USA_WAREHOUSE_ID = "4028E6C72959682B01295ECFE2E20270";

  // Selection MODE
  public static final String ALL_SELECTION_MODE = "A";
  public static final String ONLY_THOSE_DEFINED = "I";
  public static final String ALL_EXCLUDING_DEFINED = "E";

  /**
   * Create Storage Bin
   */

  public static Locator getNewStorageBinForTest(String key) {
    Locator storageBin = cloneStorageBin(LOCATOR_USA111_ID, key);
    return storageBin;
  }

  /**
   * Create Product
   */

  public static Product getNewProductForTest(String name) {
    Product product = cloneProduct(PRODUCT_DGA_ID, name);
    return product;
  }

  /**
   * Create Stock for product in particular locator
   */

  public static void createStockForProductInBinForTest(String oldTransactionId, String documentNo,
      Product product, Locator storageBin, BigDecimal quantity) {
    createShipmentInOut(oldTransactionId, product, storageBin, documentNo, quantity);
  }

  /***********************************************************************************************************************/
  /**********************************************
   * General methods for tests
   **********************************************/
  /***********************************************************************************************************************/

  /**
   * Returns a new StorageBin based on the given one.
   *
   * @param oldStorageBinID
   *          Id of original Locator to clone
   * @param name
   *          Name to be set to the new Storage Bin
   * @return a new StorageBin based on the original one
   */
  public static Locator cloneStorageBin(String oldStorageBinID, String name) {
    Locator oldStorageBin = OBDal.getInstance().get(Locator.class, oldStorageBinID);
    Locator newStorageBin = (Locator) DalUtil.copy(oldStorageBin, false);
    String suffix = getSuffixBasedOnNumberOfBinsWithSameName(name);

    setNewBinParameters(newStorageBin, name, suffix);

    return newStorageBin;
  }

  private static String getSuffixBasedOnNumberOfBinsWithSameName(String name) {
    return StringUtils.leftPad(String.valueOf(getNumberOfBinsWithSameName(name) + 1), 4, "0");
  }

  /**
   * Returns the number of locators with same Locators value
   */
  private static int getNumberOfBinsWithSameName(String searchKey) {
    try {
      final OBCriteria<Locator> criteria = OBDal.getInstance().createCriteria(Locator.class);
      criteria.add(Restrictions.like(Locator.PROPERTY_SEARCHKEY, searchKey + "-%"));
      return criteria.count();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * sets the storage bin properties Id, search key, RowX, StackY, LevelZ for new storage bin
   */

  private static void setNewBinParameters(Locator storageBin, String name, String suffix) {
    storageBin.setId(SequenceIdData.getUUID());
    storageBin.setSearchKey(name + "-" + suffix);
    storageBin.setRowX(storageBin.getRowX() + "-" + suffix + " - " + name);
    storageBin.setStackY(storageBin.getStackY() + "-" + suffix + " - " + name);
    storageBin.setLevelZ(storageBin.getLevelZ() + "-" + suffix + " - " + name);
    storageBin.setNewOBObject(true);
    OBDal.getInstance().save(storageBin);
  }

  /**
   * Refresh storage bin
   */

  public static void refreshStorageBin(Locator storageBin) {
    OBDal.getInstance().refresh(storageBin);
  }

  /**
   * Returns a new Product based on the given one. It is a clone of the first one but with different
   * name and value
   *
   * @param productId
   *          Id of the original Product
   * @param name
   *          Name of the original Product
   * @return A new Product clone based on the original one
   */
  private static Product cloneProduct(String productId, String name) {
    Product oldProduct = OBDal.getInstance().get(Product.class, productId);
    Product newProduct = (Product) DalUtil.copy(oldProduct, false);
    String suffix = getSuffixBasedOnNumberOfProductsWithSameName(name);

    setProductParameters(newProduct, name, suffix);

    cloneProductPrices(oldProduct, newProduct);

    return newProduct;
  }

  /**
   * gets a suffix based on number of products with same name
   */
  public static String getSuffixBasedOnNumberOfProductsWithSameName(String name) {
    return StringUtils.leftPad(String.valueOf(getNumberOfProductsWithSameName(name)) + 1, 4, "0");
  }

  /**
   * Returns the number of products with same Product name
   */
  private static int getNumberOfProductsWithSameName(String name) {
    try {
      final OBCriteria<Product> criteria = OBDal.getInstance().createCriteria(Product.class);
      criteria.add(Restrictions.like(Product.PROPERTY_NAME, name + "-%"));
      return criteria.count();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * Sets product properties search key, name, upc, id for new Product
   */
  public static void setProductParameters(Product product, String name, String suffix) {
    product.setSearchKey(name + "-" + suffix);
    product.setName(name + "-" + suffix);
    product.setUPCEAN(null);
    product.setId(SequenceIdData.getUUID());
    product.setNewOBObject(true);
    OBDal.getInstance().save(product);
  }

  /**
   * Creates a copy of product price for the newly created product from old product
   */
  public static void cloneProductPrices(Product oldProduct, Product newProduct) {
    List<ProductPrice> oldPriceList = oldProduct.getPricingProductPriceList();
    for (ProductPrice oldPrice : oldPriceList) {
      ProductPrice newProductPrice = createNewPriceForProduct(oldPrice, newProduct);
      newProduct.getPricingProductPriceList().add(newProductPrice);
    }
    OBDal.getInstance().save(newProduct);
  }

  /**
   * Creates a copy of product price for the newly created product from old product
   */
  private static ProductPrice createNewPriceForProduct(ProductPrice price, Product product) {
    ProductPrice newProductPrice = (ProductPrice) DalUtil.copy(price, false);
    newProductPrice.setNewOBObject(true);
    newProductPrice.setId(SequenceIdData.getUUID());
    newProductPrice.setProduct(product);
    OBDal.getInstance().save(newProductPrice);
    return newProductPrice;
  }

  /**
   * Creates Receipt Shipment from old transaction id for new product and new locator
   */

  public static void createShipmentInOut(String oldShipmentInOutID, Product product,
      Locator storageBin, String documentNo, BigDecimal quantity) {
    ShipmentInOut shipmentInOut = cloneReceiptShipment(oldShipmentInOutID, documentNo);
    ShipmentInOutLine line = getFisrtLineOfShipmentInOut(shipmentInOut);

    modifyClonedInOutLine(product, storageBin, line, quantity);

    processAndRefreshShipmentInOut(shipmentInOut);
    assertThatDocumentHasBeenCompleted(shipmentInOut);
    // Needs to refresh Storage Bin to take into account new Stock
    refreshStorageBin(storageBin);
  }

  /**
   * asserts that receipt/shipment document is completed
   */

  private static void assertThatDocumentHasBeenCompleted(ShipmentInOut shipment) {
    assertThat("Document must be completed: ", shipment.getDocumentStatus(),
        equalTo(COMPLETED_DOCUMENT));
  }

  /**
   * Gets the suffix based on number of shipments/receipts with same documentNo
   */

  public static String getSuffixBasedOnNumberOfShipmentsWithSameDocNo(String docNo) {
    return StringUtils.leftPad(String.valueOf(getNumberOfShipmentsWithSameName(docNo)) + 1, 4, "0");
  }

  /**
   * Returns the number of Goods Receipts/Shipments with same Document Number
   */
  public static int getNumberOfShipmentsWithSameName(String docNo) {
    try {
      final OBCriteria<ShipmentInOut> criteria = OBDal.getInstance()
          .createCriteria(ShipmentInOut.class);
      criteria.add(Restrictions.like(ShipmentInOut.PROPERTY_DOCUMENTNO, docNo + "-%"));
      return criteria.count();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * Returns a new Goods Receipt/Shipment based on the given one. It is a clone of the first one but
   * in a not completed status
   *
   * @param oldInOutID
   *          Id of original Goods Receipt/Shipment to clone
   * @param docNo
   *          docNo to set to the new Goods Receipt/Shipment
   * @return a Goods Receipt/Shipment not completed
   */
  public static ShipmentInOut cloneReceiptShipment(String oldInOutID, String docNo) {
    ShipmentInOut oldInOut = OBDal.getInstance().get(ShipmentInOut.class, oldInOutID);
    ShipmentInOut newInOut = (ShipmentInOut) DalUtil.copy(oldInOut, false);
    String suffix = getSuffixBasedOnNumberOfShipmentsWithSameDocNo(docNo);

    setInOutParameters(newInOut, docNo, suffix);
    for (ShipmentInOutLine oldLine : oldInOut.getMaterialMgmtShipmentInOutLineList()) {
      ShipmentInOutLine newLine = cloneReceiptShipmentLine(oldLine, newInOut);
      newInOut.getMaterialMgmtShipmentInOutLineList().add(newLine);
    }

    OBDal.getInstance().save(newInOut);
    return newInOut;
  }

  /**
   * Gets the first receipt/shipment line
   */

  private static ShipmentInOutLine getFisrtLineOfShipmentInOut(ShipmentInOut receipt) {
    return receipt.getMaterialMgmtShipmentInOutLineList().get(0);
  }

  /**
   * Set the receipt/shipment properties id, documentNo, documentStatus,documentAction, Processed
   * flag, movementDate, orderDate for newly created receipt/shipment
   */

  private static void setInOutParameters(ShipmentInOut inOut, String docNo, String suffix) {
    inOut.setId(SequenceIdData.getUUID());
    inOut.setDocumentNo(docNo + "-" + suffix);
    inOut.setDocumentStatus(DRAFT_STATUS);
    inOut.setDocumentAction(COMPLETE_ACTION);
    inOut.setProcessed(false);
    inOut.setMovementDate(new Date());
    inOut.setOrderDate(new Date());
    inOut.setNewOBObject(true);
    inOut.setSalesOrder(null);
    OBDal.getInstance().save(inOut);
  }

  /**
   * Returns a new Goods Receipt/Shipment Line based on the given one. It is a clone of the first
   * one but with different product
   *
   * @param line
   *          Original Goods Receipt/Shipment
   * @param newInOut
   *          new Goods Receipt/Shipment (a clone of the original one)
   * @return A new Goods Receipt/Shipment Line clone based on the original one
   */
  private static ShipmentInOutLine cloneReceiptShipmentLine(ShipmentInOutLine oldLine,
      ShipmentInOut newInOut) {
    ShipmentInOutLine newLine = (ShipmentInOutLine) DalUtil.copy(oldLine, false);
    setInOutLineParameters(newInOut, newLine);
    OBDal.getInstance().save(newLine);
    return newLine;
  }

  /**
   * Sets receipt/shipment line properties id, receipt/shipment
   */
  private static void setInOutLineParameters(ShipmentInOut inOut, ShipmentInOutLine inOutLIne) {
    inOutLIne.setId(SequenceIdData.getUUID());
    inOutLIne.setShipmentReceipt(inOut);
    inOutLIne.setNewOBObject(true);
    inOutLIne.setSalesOrderLine(null);
  }

  /**
   * Set receipt/shipment properties product, locator, attributeSetValue, movementQuantity
   */
  public static void modifyClonedInOutLine(Product product, Locator storageBin,
      ShipmentInOutLine line, BigDecimal movementQty) {
    line.setProduct(product);
    line.setStorageBin(storageBin);
    line.setAttributeSetValue(null);
    line.setMovementQuantity(movementQty);
    OBDal.getInstance().save(line);
  }

  /**
   * Process and Refresh receipt/shipment document.
   */
  public static void processAndRefreshShipmentInOut(ShipmentInOut receipt) {
    processShipmentInOutInDB(receipt);
    OBDal.getInstance().refresh(receipt);
  }

  /**
   * Calls M_Inout_Post Database Function to complete the given Shipment/Receipt
   *
   * @param shipmentReceipt
   *          Shipment or Receipt to be completed
   * @throws OBException
   */
  private static void processShipmentInOutInDB(ShipmentInOut shipmentReceipt) throws OBException {
    final List<Object> parameters = new ArrayList<Object>();
    parameters.add(null);
    parameters.add(shipmentReceipt.getId());
    final String procedureName = PROCESS_SHIPMENT_RECEIPT;
    CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);
    OBDal.getInstance().flush();
  }

  /**
   * Defines a handling unit type for Storage Bin
   */

  public static LocatorHandlingUnitType createStorageBinHUType(Locator storageBin,
      ReferencedInventoryType handlingUnitType) {
    LocatorHandlingUnitType locatorHUType = OBProvider.getInstance()
        .get(LocatorHandlingUnitType.class);
    locatorHUType.setStorageBin(storageBin);
    locatorHUType.setHandlingUnitType(handlingUnitType);
    OBDal.getInstance().save(locatorHUType);
    return locatorHUType;
  }
}
