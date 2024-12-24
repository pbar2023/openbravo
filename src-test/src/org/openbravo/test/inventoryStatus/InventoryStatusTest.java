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
 * All portions are Copyright (C) 2017-2024 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.inventoryStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.InventoryStatusUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductAUM;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.test.storageBin.StorageBinTestUtils;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class InventoryStatusTest extends WeldBaseTest {

  final static private Logger log = LogManager.getLogger();

  // Original Reservation
  private static final String RESERVATION_ID = "82E3163AD14F4182B9F1292D5EACF7D4";

  // Inventory Status options
  private static final String AVAILABLE_STATUS = "2";
  private static final String BLOCKED_STATUS = "1";
  private static final String BACKFLUSH_STATUS = "00";
  private static final String UNDEF_OVERISSUE_STATUS = "0";

  // Document Status and Actions
  private static final String COMPLETED_DOCUMENT = "CO";
  private static final String DRAFT_STATUS = "DR";
  private static final String RES_PROCESS_ACTION = "PR";

  // Process to complete Documents
  private static final String PROCESS_RESERVATION = "m_reservation_post";

  // Reservations preference
  private static final String RESERVATIONS_PREFERENCE = "StockReservations";

  // Error messages
  private static final String ERROR_MESSAGE_NEGATIVESTOCK = "There is negative Stock for Product:";
  private static final String LOCATOR_WITH_NOT_AVAILABLE_STATUS_ERROR = "@LocatorWithNotAvailableStatus@";
  private static final String NOT_ENOUGH_STOCKED_ERROR = "@NotEnoughStocked@";

  // Global variables needed to share data between IS003 Tests
  private static String binIS003ID;
  private static String productIS003ID;

  // Global variables needed to share data between IS004 Tests
  private static String binIS004ID;
  private static String productIS004ID;

  @Before
  public void initialize() {
    log.info("Initializing Inventory Status Test ...");
    OBContext.setOBContext(StorageBinTestUtils.USER_ID, StorageBinTestUtils.ROLE_ID,
        StorageBinTestUtils.CLIENT_ID, StorageBinTestUtils.ORG_ID,
        StorageBinTestUtils.LANGUAGE_CODE);
    initializeReservationsPreference();
  }

  private void initializeReservationsPreference() {
    if (!existsReservationsPreference()) {
      createReservationsPreference();
    }
  }

  private void createReservationsPreference() {
    Client client = OBDal.getInstance().get(Client.class, StorageBinTestUtils.CLIENT_ID);
    Organization organization = OBDal.getInstance()
        .get(Organization.class, StorageBinTestUtils.ORG_STAR_ID);

    Preference reservationsPreference = OBProvider.getInstance().get(Preference.class);
    reservationsPreference.setClient(client);
    reservationsPreference.setOrganization(organization);
    reservationsPreference.setPropertyList(true);
    reservationsPreference.setProperty(RESERVATIONS_PREFERENCE);
    reservationsPreference.setSearchKey("Y");
    reservationsPreference.setVisibleAtClient(null);
    reservationsPreference.setVisibleAtOrganization(null);
    reservationsPreference.setVisibleAtRole(null);
    reservationsPreference.setUserContact(null);
    reservationsPreference.setWindow(null);
    OBDal.getInstance().save(reservationsPreference);

  }

  private boolean existsReservationsPreference() {
    Client client = OBDal.getInstance().get(Client.class, StorageBinTestUtils.CLIENT_ID);
    Organization organization = OBDal.getInstance()
        .get(Organization.class, StorageBinTestUtils.ORG_STAR_ID);
    // Value property is defined as CLOB in Oracle, this is why it is needed this expression to
    // convert it to a char first
    String valueISYes = " to_char(value) = 'Y' ";

    OBCriteria<Preference> criteria = OBDal.getInstance().createCriteria(Preference.class);
    criteria.add(Restrictions.eq(Preference.PROPERTY_PROPERTY, RESERVATIONS_PREFERENCE));
    criteria.add(Restrictions.sqlRestriction(valueISYes));
    criteria.add(Restrictions.eq(Preference.PROPERTY_CLIENT, client));
    criteria.add(Restrictions.eq(Preference.PROPERTY_ORGANIZATION, organization));
    return !criteria.list().isEmpty();
  }

  /**
   * IS001IS001SeveralStatusChanges: Basic Inventory Status changes over the same Storage Bin with
   * existing Stock, without any particular scenario
   * 
   * <ul>
   * <li>Clone a Product and a Storage Bin</li>
   * <li>Change Status of the Storage Bin to Available</li>
   * <li>Change Status of the Storage Bin to Blocked</li>
   * <li>Change Status of the Storage Bin to Available</li>
   * <li>Change Status of the Storage Bin to Undefined Over Issue</li>
   * </ul>
   */
  @Test
  public void IS001SeveralStatusChanges() {
    OBContext.setAdminMode();
    try {

      Locator storageBin = getNewStorageBinForTest001();
      Product product = getNewProductForTest001();

      createStockForProductInBinForTest001(product, storageBin);

      changeBinToAvailableStatus(storageBin);
      assertThatBinHasAvailableStatus(storageBin);

      changeBinToBlockedStatus(storageBin);
      assertThatBinHasBlockedStatus(storageBin);

      changeBinToAvailableStatus(storageBin);
      assertThatBinHasAvailableStatus(storageBin);

      changeBinToUndefOverIssueStatus(storageBin);
      assertThatBinHasUndefOverIssueStatus(storageBin);

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private Locator getNewStorageBinForTest001() {
    Locator storageBin = StorageBinTestUtils.cloneStorageBin(StorageBinTestUtils.LOCATOR_USA111_ID,
        "IS001");
    return storageBin;
  }

  private Product getNewProductForTest001() {
    Product product = cloneProduct(StorageBinTestUtils.PRODUCT_DGA_ID, "IS001");
    return product;
  }

  private void createStockForProductInBinForTest001(Product product, Locator storageBin) {
    String documentNo = "IS001";
    BigDecimal quantity = BigDecimal.ONE;
    StorageBinTestUtils.createShipmentInOut(StorageBinTestUtils.GOODS_RECEIPT_ID, product,
        storageBin, documentNo, quantity);
  }

  /**
   * IS002IS002NotPossibleToSetAvailableWhenNegativeStock : Test that it is not possible to change
   * the Inventory Status of a Storage Bin from a Status that allows to go Over Issue to a Status
   * that does not allow it, if there is negative Stock in the Storage Bin
   * <ul>
   * <li>Clone Product and Storage Bin and create a Goods Shipment to create negative Stock</li>
   * <li>Change Inventory Status to BackFlush, it allows it</li>
   * <li>Change Inventory Status to Available, it fails because there is negative Stock</li>
   * </ul>
   */
  @Test
  public void IS002NotPossibleToSetAvailableWhenNegativeStock() {
    OBContext.setAdminMode();
    try {
      Locator storageBin = getNewStorageBinForTest002();
      Product product = getNewProductForTest002();

      createNegativeStockForProductInBinForTest002(product, storageBin);

      changeBinToBackflushStatus(storageBin);
      assertThatBinHasBackflushStatus(storageBin);

      // Expect to throw an exception in the next Status change
      Exception thrown = assertThrows(Exception.class,
          () -> changeBinToAvailableStatus(storageBin));
      assertThat(thrown.getMessage(), containsString(ERROR_MESSAGE_NEGATIVESTOCK));
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private Locator getNewStorageBinForTest002() {
    return StorageBinTestUtils.cloneStorageBin(StorageBinTestUtils.LOCATOR_USA111_ID, "IS002");
  }

  private Product getNewProductForTest002() {
    return cloneProduct(StorageBinTestUtils.PRODUCT_DGA_ID, "IS002");
  }

  private void createNegativeStockForProductInBinForTest002(Product product, Locator storageBin) {
    String documentNo = "IS002";
    BigDecimal quantity = BigDecimal.ONE;
    StorageBinTestUtils.createShipmentInOut(StorageBinTestUtils.GOODS_SHIPMENT_ID, product,
        storageBin, documentNo, quantity);
  }

  /**
   * IS003: Test that it is not possible to create a Goods Shipment for Stock that is in Blocked
   * Inventory Status, but if the Status is changed to Available, this Goods Shipment can be
   * completed
   * <ul>
   * <li>IS003A: Clone Product and Storage Bin and create a Goods Receipt to create Stock. Change
   * Inventory Status to Blocked</li>
   * <li>IS003B: Try to Complete the Goods Shipment, it is not possible</li>
   * <li>IS003C: Change the Inventory Status to Available. Complete the Goods Shipment
   * successfully</li>
   * </ul>
   */
  @Test
  public void IS003ACreateStockAndSetBlockedStatus() {
    OBContext.setAdminMode();
    try {
      Locator storageBin = getNewStorageBinForTestAndSaveSharedBinID003();
      Product product = getNewProductAndSaveSharedIDForTest003();

      createStockForProductInBinForTest003(storageBin, product);

      changeBinToBlockedStatus(storageBin);
      assertThatBinHasBlockedStatus(storageBin);

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void IS003BNotPossibleToCompleteShipment() {
    OBContext.setAdminMode();
    try {
      // Needs to be done to reload them into hibernate
      Locator storageBin = reloadStorageBinForTestIS003();
      Product product = reloadProductForTestIS003();

      // This should throw an exception since the target Locator is in Blocked Status
      createGoodsShipmentForTest003(storageBin, product);

    } catch (Exception e) {
      // Check correct exception is thrown
      assertThatNotPossibleToCompleteShipmentDueToNotAvailableBin(e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void IS003CSetAvailableStatusAndCompleteShipment() {
    OBContext.setAdminMode();
    try {
      // Needs to be done to reload them into hibernate
      Locator storageBin = reloadStorageBinForTestIS003();
      Product product = reloadProductForTestIS003();

      changeBinToAvailableStatus(storageBin);
      assertThatBinHasAvailableStatus(storageBin);

      createGoodsShipmentForTest003(storageBin, product);

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private Locator getNewStorageBinForTestAndSaveSharedBinID003() {
    Locator storageBin = StorageBinTestUtils.cloneStorageBin(StorageBinTestUtils.LOCATOR_USA111_ID,
        "IS003");
    saveSharedBinIDForTest003(storageBin);
    return storageBin;
  }

  private Product getNewProductAndSaveSharedIDForTest003() {
    Product product = cloneProduct(StorageBinTestUtils.PRODUCT_DGA_ID, "IS003");
    saveSharedProductIDForTest003(product);
    return product;
  }

  private void saveSharedBinIDForTest003(Locator storageBin) {
    binIS003ID = storageBin.getId();
  }

  private void saveSharedProductIDForTest003(Product product) {
    productIS003ID = product.getId();
  }

  private Locator reloadStorageBinForTestIS003() {
    return OBDal.getInstance().get(Locator.class, binIS003ID);
  }

  private Product reloadProductForTestIS003() {
    return OBDal.getInstance().get(Product.class, productIS003ID);
  }

  private void createStockForProductInBinForTest003(Locator storageBin, Product product) {
    String documentNo = "IS003";
    BigDecimal quantity = new BigDecimal(100);
    StorageBinTestUtils.createShipmentInOut(StorageBinTestUtils.GOODS_RECEIPT_ID, product,
        storageBin, documentNo, quantity);
  }

  private void createGoodsShipmentForTest003(Locator storageBin, Product product) {
    String documentNo = "IS003";
    BigDecimal quantity = BigDecimal.ONE;
    StorageBinTestUtils.createShipmentInOut(StorageBinTestUtils.GOODS_SHIPMENT_ID, product,
        storageBin, documentNo, quantity);
  }

  /**
   * IS004: Test that it is not possible to complete a Goods Shipment if there is not Stock and
   * there are no Bins that allows to go Over Issue. Change the status of one Bin to one that allows
   * to go Over Issue and check that the Goods Shipment can be completed creating negative Stock
   * <ul>
   * <li>IS004A: Clone Product and Storage Bin and check that the Inventory Status is Available</li>
   * <li>IS004B: Try to complete a Goods Shipment, it is not possible because there is not Stock
   * available</li>
   * <li>IS004C: Change Inventory Status to Backflush. Complete the Goods Shipment successfully</li>
   * </ul>
   */
  @Test
  public void IS004ASetBinStatusToAvailable() {
    OBContext.setAdminMode();
    try {
      Locator storageBin = getNewStorageBinForTestAndSaveSharedBinID004();
      getNewProductAndSaveSharedIDForTest004();

      changeBinToAvailableStatus(storageBin);
      assertThatBinHasAvailableStatus(storageBin);

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void IS004BNotPossibleToCompleShipment() {
    OBContext.setAdminMode();
    try {

      // Needs to be done to reload them into hibernate
      Locator storageBin = reloadStorageBinForTestIS004();
      Product product = reloadProductForTestIS004();

      // This should throw an exception since there is no sufficient stock
      createGoodsShipmentForTest004(storageBin, product);
    } catch (Exception e) {
      // Check that correct exception is thrown
      assertThatNotPossibleToCompleteShipmentDueToNoStock(e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void IS004CSetBinStatusToBackflushAndCompleteShipment() {
    OBContext.setAdminMode();
    try {
      // Needs to be done to reload them into hibernate
      Locator storageBin = reloadStorageBinForTestIS004();
      Product product = reloadProductForTestIS004();

      changeBinToBackflushStatus(storageBin);
      assertThatBinHasBackflushStatus(storageBin);

      createGoodsShipmentForTest004(storageBin, product);

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private Locator getNewStorageBinForTestAndSaveSharedBinID004() {
    Locator storageBin = StorageBinTestUtils.cloneStorageBin(StorageBinTestUtils.LOCATOR_USA111_ID,
        "IS004");
    saveSharedBinIDForTest004(storageBin);
    return storageBin;
  }

  private Product getNewProductAndSaveSharedIDForTest004() {
    Product product = cloneProduct(StorageBinTestUtils.PRODUCT_DGA_ID, "IS004");
    saveSharedProductIDForTest004(product);
    return product;
  }

  private void saveSharedBinIDForTest004(Locator storageBin) {
    binIS004ID = storageBin.getId();
  }

  private void saveSharedProductIDForTest004(Product product) {
    productIS004ID = product.getId();
  }

  private Locator reloadStorageBinForTestIS004() {
    return OBDal.getInstance().get(Locator.class, binIS004ID);
  }

  private Product reloadProductForTestIS004() {
    return OBDal.getInstance().get(Product.class, productIS004ID);
  }

  private void createGoodsShipmentForTest004(Locator storageBin, Product product) {
    String documentNo = "IS004";
    BigDecimal quantity = BigDecimal.ONE;
    StorageBinTestUtils.createShipmentInOut(StorageBinTestUtils.GOODS_SHIPMENT_ID, product,
        storageBin, documentNo, quantity);
  }

  /**
   * IS005IS005NotPossibleToChangeStatusToBlockedIfThereAreReservations: Test that it is not
   * possible to change the Status of a Bin to a Status that is not Avilable if there are
   * Reservations related to it.
   * <ul>
   * <li>Clone Product and Storage Bin and create a Goods Receipt to create Stock</li>
   * <li>Clone an existing Reservation</li>
   * <li>Change Inventory Status to Available</li>
   * <li>Try to change Inventory Status to Blocked and it fails because there is a related
   * Reservation</li>
   * </ul>
   */
  @Test
  public void IS005NotPossibleToChangeStatusToBlockedIfThereAreReservations() {
    OBContext.setAdminMode();
    try {
      Locator storageBin = getNewStorageBinForTest005();
      Product product = getNewProductForTest005();

      createStockForProductInBinForTest005(storageBin, product);
      createStockReservationForProductInBinForTest0005(product, storageBin);

      changeBinToAvailableStatus(storageBin);
      assertThatBinHasAvailableStatus(storageBin);

      try {
        changeBinToBlockedStatus(storageBin);
      } catch (Exception e) {
        assertThatNotPossibleToChangeStatusDueToExistingReservation(e, product);
      }

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private Locator getNewStorageBinForTest005() {
    return StorageBinTestUtils.cloneStorageBin(StorageBinTestUtils.LOCATOR_USA111_ID, "IS005");
  }

  private Product getNewProductForTest005() {
    return cloneProduct(StorageBinTestUtils.PRODUCT_DGA_ID, "IS005");
  }

  private void createStockForProductInBinForTest005(Locator storageBin, Product product) {
    String documentNo = "IS005";
    BigDecimal quantity = new BigDecimal(1);
    StorageBinTestUtils.createShipmentInOut(StorageBinTestUtils.GOODS_RECEIPT_ID, product,
        storageBin, documentNo, quantity);
  }

  private void createStockReservationForProductInBinForTest0005(Product product,
      Locator storageBin) {
    BigDecimal reservedQty = BigDecimal.ONE;
    createStockReservationForProductInBin(product, storageBin, reservedQty);
  }

  /***********************************************************************************************************************/
  /**********************************************
   * General methods for tests
   **********************************************/
  /***********************************************************************************************************************/

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
    String suffix = StorageBinTestUtils.getSuffixBasedOnNumberOfProductsWithSameName(name);

    StorageBinTestUtils.setProductParameters(newProduct, name, suffix);

    StorageBinTestUtils.cloneProductPrices(oldProduct, newProduct);
    cloneProductAUMs(oldProduct, newProduct);

    return newProduct;
  }

  private static void cloneProductAUMs(Product oldProduct, Product newProduct) {
    List<ProductAUM> oldAUMList = oldProduct.getProductAUMList();
    for (ProductAUM oldAum : oldAUMList) {
      ProductAUM newAUM = createNewAUMForProduct(oldAum, newProduct);
      newProduct.getProductAUMList().add(newAUM);
    }
    OBDal.getInstance().save(newProduct);
  }

  private static ProductAUM createNewAUMForProduct(ProductAUM oldAum, Product newProduct) {
    ProductAUM newAUM = (ProductAUM) DalUtil.copy(oldAum, false);
    newAUM.setNewOBObject(true);
    newAUM.setId(SequenceIdData.getUUID());
    newAUM.setProduct(newProduct);
    OBDal.getInstance().save(newAUM);
    return newAUM;
  }

  private static void createStockReservationForProductInBin(Product product, Locator storageBin,
      BigDecimal reservedQty) {
    Reservation reservation = cloneReservation(RESERVATION_ID);
    ReservationStock reservationStock = reservation.getMaterialMgmtReservationStockList().get(0);

    modifyClonedReservationStock(reservationStock, storageBin, reservedQty);
    modifyClonedReservation(reservation, product, reservedQty);

    processAndRefreshReservation(reservation);
    assertThatReservationHasBeenCompleted(reservation);
  }

  /**
   * Returns a new Reservation based on the given one. It is a clone of the first one but in a not
   * completed status
   * 
   * @param oldReservationID
   *          Id of original Reservation to clone
   * @return a Reservation not completed
   */
  private static Reservation cloneReservation(String oldReservationID) {
    Reservation oldReservation = OBDal.getInstance().get(Reservation.class, oldReservationID);
    Reservation newReservation = (Reservation) DalUtil.copy(oldReservation, false);

    setReservationParameters(newReservation);
    for (ReservationStock reservationStock : oldReservation.getMaterialMgmtReservationStockList()) {
      cloneReservationStock(reservationStock, newReservation);
      newReservation.getMaterialMgmtReservationStockList().add(reservationStock);
    }

    OBDal.getInstance().save(newReservation);
    return newReservation;
  }

  private static void setReservationParameters(Reservation reservation) {
    reservation.setId(SequenceIdData.getUUID());
    reservation.setRESStatus(DRAFT_STATUS);
    reservation.setRESProcess(RES_PROCESS_ACTION);
    reservation.setNewOBObject(true);
    OBDal.getInstance().save(reservation);
  }

  private static void modifyClonedReservationStock(ReservationStock line, Locator storageBin,
      BigDecimal reservedQty) {
    line.setAllocated(true);
    line.setStorageBin(storageBin);
    line.setQuantity(reservedQty);
    OBDal.getInstance().save(line);
  }

  private static void modifyClonedReservation(Reservation reservation, Product product,
      BigDecimal reservedQty) {
    reservation.setProduct(product);
    reservation.setReservedQty(reservedQty);
    OBDal.getInstance().save(reservation);
  }

  /**
   * Returns a new ReservationStock based on the given one. It is a clone of the first one
   * 
   * @param oldReservationStock
   *          Reservation Stock to be cloned
   * @param newReservation
   *          new Reservation (a clone of the original one)
   * @return A new Reservation Stock clone based on the original one
   */
  private static ReservationStock cloneReservationStock(ReservationStock oldReservationStock,
      Reservation newReservation) {
    ReservationStock newReservationStock = (ReservationStock) DalUtil.copy(oldReservationStock,
        false);
    setReservationStockParameters(newReservation, newReservationStock);
    return newReservationStock;
  }

  private static void setReservationStockParameters(Reservation newReservation,
      ReservationStock newReservationStock) {
    newReservationStock.setId(SequenceIdData.getUUID());
    newReservationStock.setReservation(newReservation);
    newReservationStock.setNewOBObject(true);
    OBDal.getInstance().save(newReservationStock);
  }

  private static void processAndRefreshReservation(Reservation reservation) {
    processReservationInDB(reservation, StorageBinTestUtils.USER_ID);
    OBDal.getInstance().refresh(reservation);
  }

  /**
   * Calls M_Reservation_Post Database Function to complete the given Reservation
   * 
   * @param reservation
   *          Reservation to be completed
   * @throws OBException
   */
  private static void processReservationInDB(Reservation reservation, String userId)
      throws OBException {
    final List<Object> parameters = new ArrayList<Object>();
    parameters.add(null);
    parameters.add(reservation.getId());
    parameters.add(RES_PROCESS_ACTION);
    parameters.add(userId);
    final String procedureName = PROCESS_RESERVATION;
    CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);
  }

  private static void changeBinToUndefOverIssueStatus(Locator storageBin) {
    InventoryStatusUtils.changeStatusOfStorageBin(storageBin, UNDEF_OVERISSUE_STATUS);
  }

  private static void changeBinToBlockedStatus(Locator storageBin) {
    InventoryStatusUtils.changeStatusOfStorageBin(storageBin, BLOCKED_STATUS);
  }

  private static void changeBinToAvailableStatus(Locator storageBin) {
    InventoryStatusUtils.changeStatusOfStorageBin(storageBin, AVAILABLE_STATUS);
  }

  private static void changeBinToBackflushStatus(Locator storageBin) {
    InventoryStatusUtils.changeStatusOfStorageBin(storageBin, BACKFLUSH_STATUS);
  }

  private static void assertThatBinHasUndefOverIssueStatus(Locator storageBin) {
    assertThat("Inventory Status of Storage Bin must be 'Undefined Over-Issue': ",
        storageBin.getInventoryStatus().getId(), equalTo(UNDEF_OVERISSUE_STATUS));
  }

  private static void assertThatBinHasBlockedStatus(Locator storageBin) {
    assertThat("Inventory Status of Storage Bin must be 'Blocked': ",
        storageBin.getInventoryStatus().getId(), equalTo(BLOCKED_STATUS));
  }

  private static void assertThatBinHasAvailableStatus(Locator storageBin) {
    assertThat("Inventory Status of Storage Bin must be 'Available': ",
        storageBin.getInventoryStatus().getId(), equalTo(AVAILABLE_STATUS));
  }

  private static void assertThatBinHasBackflushStatus(Locator storageBin) {
    assertThat("Inventory Status of Storage Bin must be 'Backflush': ",
        storageBin.getInventoryStatus().getId(), equalTo(BACKFLUSH_STATUS));
  }

  private static void assertThatReservationHasBeenCompleted(Reservation reservation) {
    assertThat("Document must be completed: ", reservation.getRESStatus(),
        equalTo(COMPLETED_DOCUMENT));
  }

  private static void assertThatNotPossibleToCompleteShipmentDueToNoStock(Exception e) {
    assertThat("Expected exception is: Error: Insufficient stock",
        StringUtils.contains(e.getMessage(), NOT_ENOUGH_STOCKED_ERROR), equalTo(true));
  }

  private static void assertThatNotPossibleToChangeStatusDueToExistingReservation(Exception e,
      Product product) {
    assertThat("The expected exception is 'There are Reservations created for Product: '" + product,
        StringUtils.contains(e.getMessage(),
            "There are Reservations created for Product: " + product.getName()),
        equalTo(true));
  }

  private static void assertThatNotPossibleToCompleteShipmentDueToNotAvailableBin(Exception e) {
    assertThat(
        "The expected exception is : Error: In line 10, the Storage Bin of the Line has a Not Available Inventory Status. It is not possible to issue a Product from a Not Available Storage Bin",
        StringUtils.contains(e.getMessage(), LOCATOR_WITH_NOT_AVAILABLE_STATUS_ERROR),
        equalTo(true));
  }

}
