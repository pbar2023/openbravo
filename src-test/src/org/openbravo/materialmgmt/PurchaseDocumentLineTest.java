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
 * All portions are Copyright (C) 2024 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.materialmgmt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.ApprovedVendor;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.test.base.TestConstants.Orgs;
import org.openbravo.test.base.TestConstants.Users;

/**
 * Test cases to cover the purchase configuration validations done to the lines of purchase orders.
 */
public class PurchaseDocumentLineTest extends WeldBaseTest {

  private static final String FB_ESPANA_NORTE_ORGANIZATION_ID = "E443A31992CB4635AFCAEABE7183CE85";
  private static final String FB_CLIENT_ID = "23C59575B9CF467C9620760EB255B389";
  private static final String FB_ADMIN_ID = "42D0EEB1C66F497A90DD526DC597E6F0";
  private static final String PRICELIST_ID = "AEE66281A08F42B6BC509B8A80A33C29";
  private static final String PAYMENT_TERM_ID = "66BA1164A7394344BB9CD1A6ECEED05D";
  private static final String WAREHOUSE_ID = "B2D40D8A5D644DD89E329DC297309055";
  private static final String PRODUCT_ID = "DA7FC1BB3BA44EC48EC1AB9C74168CED";
  private static final String UOM_ID = "100";
  private static final String VENDOR_ID = "A6750F0D15334FB890C254369AC750A8";
  private static final String CURRENCY_ID = "102";
  private static final String ORDER_DOC_TYPE = "24C649C1402B4ABDA4C59046BB45B9FF";

  private static final List<Map<String, Object>> PARAMS = Arrays.asList(
  //@formatter:off
    Map.of("createConfig", true,  "minQty", 4, "stdQty", 5, "lineQty", 4, "qtyType", "A", "validationError", ""),
    Map.of("createConfig", true,  "minQty", 4, "stdQty", 5, "lineQty", 5, "qtyType", "E","validationError", ""),
    Map.of("createConfig", true,  "minQty", 4, "stdQty", 5, "lineQty", 10, "qtyType", "M", "validationError", ""),
    Map.of("createConfig", true,  "minQty", 4, "stdQty", 5, "lineQty", 1, "qtyType", "A", "validationError", ""),
    Map.of("createConfig", true,  "minQty", 4, "stdQty", 5, "lineQty", 6, "qtyType", "E", "validationError", "ValExactQty"),
    Map.of("createConfig", true,  "minQty", 4, "stdQty", 5, "lineQty", 9, "qtyType", "M", "validationError", "ValMultipleQty"),
    Map.of("createConfig", true,  "minQty", 4, "stdQty", 1, "lineQty", 1, "qtyType", "M", "validationError", "ValMinQty"),
    
    Map.of("createConfig", false,  "minQty", 4, "stdQty", 5, "lineQty", 4, "qtyType", "A", "validationError", "NoPurchaseConfiguration"),
    Map.of("createConfig", false,  "minQty", 4, "stdQty", 5, "lineQty", 5, "qtyType", "E","validationError", "NoPurchaseConfiguration"),
    Map.of("createConfig", false,  "minQty", 4, "stdQty", 5, "lineQty", 10, "qtyType", "M", "validationError", "NoPurchaseConfiguration"),
    Map.of("createConfig", false,  "minQty", 4, "stdQty", 5, "lineQty", 1, "qtyType", "A", "validationError", "NoPurchaseConfiguration"),
    Map.of("createConfig", false,  "minQty", 4, "stdQty", 5, "lineQty", 6, "qtyType", "E", "validationError", "NoPurchaseConfiguration"),
    Map.of("createConfig", false,  "minQty", 4, "stdQty", 5, "lineQty", 9, "qtyType", "M", "validationError", "NoPurchaseConfiguration")
           
    //@formatter:on
  );

  @Rule
  public final ParameterCdiTestRule<Map<String, Object>> parameterRule = new ParameterCdiTestRule<>(
      PARAMS);
  private @ParameterCdiTest Map<String, Object> testData;

  @Before
  public void init() {
    IncomingGoodsDocumentLine.invalidateCache();
    OBContext.setOBContext(Users.OPENBRAVO, FB_ADMIN_ID, FB_CLIENT_ID,
        FB_ESPANA_NORTE_ORGANIZATION_ID);
    addApprovedVendorConfiguration();
  }

  @After
  public void cleanUp() {
    rollback();
  }

  @Test
  public void validatePurchaseOrderLine() {
    enablePOQuantitiesValidation();

    int lineQty = (int) testData.get("lineQty");
    String validationError = (String) testData.get("validationError");
    Order purchaseOrder = createPurchaseOrder();

    if (validationError.isEmpty()) {
      OrderLine purchaseOrderLine = createPurchaseOrderLine(purchaseOrder, lineQty);
      assertThat("The purchase order line is created with the expected quantity",
          purchaseOrderLine.getOrderedQuantity(), equalTo(BigDecimal.valueOf(lineQty)));
    } else {
      PurchaseDocumentValidationError error = assertThrows(PurchaseDocumentValidationError.class,
          () -> createPurchaseOrderLine(purchaseOrder, lineQty));
      assertThat("The expected validation error is thrown", error.getMessage(),
          equalTo(getExpectedErrorMessage(validationError)));
    }
  }

  @Test
  public void checkValidationForPurchaseOrderLineIsDisabledByDefault() {
    int lineQty = (int) testData.get("lineQty");
    Order purchaseOrder = createPurchaseOrder();

    OrderLine purchaseOrderLine = createPurchaseOrderLine(purchaseOrder, lineQty);
    assertThat("The purchase order line is created with the expected quantity",
        purchaseOrderLine.getOrderedQuantity(), equalTo(BigDecimal.valueOf(lineQty)));
  }

  private void addApprovedVendorConfiguration() {
    if (!(boolean) testData.get("createConfig")) {
      return;
    }
    String qtyType = (String) testData.get("qtyType");
    int minQty = (int) testData.get("minQty");
    int stdQty = (int) testData.get("stdQty");

    ApprovedVendor appVendor = OBProvider.getInstance().get(ApprovedVendor.class);

    Product product = OBDal.getInstance().get(Product.class, PRODUCT_ID);

    appVendor.setOrganization(
        OBDal.getInstance().getProxy(Organization.class, FB_ESPANA_NORTE_ORGANIZATION_ID));
    appVendor.setProduct(product);
    appVendor.setMinimumOrderQty(BigDecimal.valueOf(minQty));
    appVendor.setStandardQuantity(BigDecimal.valueOf(stdQty));
    appVendor.setQuantityType(qtyType);
    appVendor.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, VENDOR_ID));

    OBDal.getInstance().save(appVendor);
    OBDal.getInstance().flush();
  }

  private Order createPurchaseOrder() {
    Order order = OBProvider.getInstance().get(Order.class);
    order.setOrganization(
        OBDal.getInstance().getProxy(Organization.class, FB_ESPANA_NORTE_ORGANIZATION_ID));
    order.setSalesTransaction(false);
    order.setDocumentType(OBDal.getInstance().getProxy(DocumentType.class, ORDER_DOC_TYPE));
    order.setTransactionDocument(OBDal.getInstance().getProxy(DocumentType.class, ORDER_DOC_TYPE));
    order.setDocumentNo("3000000");
    order.setOrderDate(new Date());
    BusinessPartner bp = OBDal.getInstance().get(BusinessPartner.class, VENDOR_ID);
    order.setBusinessPartner(bp);
    Location location = bp.getBusinessPartnerLocationList()
        .stream()
        .filter(l -> l.isInvoiceToAddress())
        .findFirst()
        .orElseThrow();
    order.setPartnerAddress(location);
    order.setInvoiceAddress(location);
    order.setWarehouse(OBDal.getInstance().getProxy(Warehouse.class, WAREHOUSE_ID));
    order.setScheduledDeliveryDate(new Date());
    order.setPaymentTerms(OBDal.getInstance().getProxy(PaymentTerm.class, PAYMENT_TERM_ID));
    order.setPriceList(OBDal.getInstance().getProxy(PriceList.class, PRICELIST_ID));
    order.setAccountingDate(new Date());
    order.setCurrency(OBDal.getInstance().getProxy(Currency.class, CURRENCY_ID));
    OBDal.getInstance().save(order);
    return order;
  }

  private OrderLine createPurchaseOrderLine(Order order, int qty) {
    OrderLine orderLine = OBProvider.getInstance().get(OrderLine.class);
    orderLine.setSalesOrder(order);
    orderLine.setLineNo(10L);
    orderLine.setProduct(OBDal.getInstance().getProxy(Product.class, PRODUCT_ID));
    orderLine.setOrderedQuantity(BigDecimal.valueOf(qty));
    orderLine.setOrderDate(new Date());
    orderLine.setBusinessPartner(order.getBusinessPartner());
    orderLine.setWarehouse(order.getWarehouse());
    orderLine.setUOM(OBDal.getInstance().getProxy(UOM.class, UOM_ID));
    orderLine.setCurrency(order.getCurrency());
    OBDal.getInstance().save(orderLine);
    OBDal.getInstance().flush();
    return orderLine;
  }

  private void enablePOQuantitiesValidation() {
    try {
      OBContext.setAdminMode(true);
      Preference preference = OBProvider.getInstance().get(Preference.class);
      preference.setOrganization(OBDal.getInstance().getProxy(Organization.class, Orgs.MAIN));
      preference.setProperty("EnableCheckPurchaseOrderLineQty");
      preference.setSearchKey("Y");
      preference.setVisibleAtRole(OBContext.getOBContext().getRole());
      OBDal.getInstance().save(preference);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private String getExpectedErrorMessage(String validationError) {
    String[] params;
    switch (validationError) {
      case "ValMinQty":
        params = new String[] { testData.get("minQty").toString() };
        break;
      case "ValExactQty":
        params = new String[] { testData.get("stdQty").toString() };
        break;
      case "ValMultipleQty":
        params = new String[] { testData.get("stdQty").toString() };
        break;
      case "NoPurchaseConfiguration":
        Organization organization = OBDal.getInstance()
            .get(Organization.class, FB_ESPANA_NORTE_ORGANIZATION_ID);
        BusinessPartner businessPartner = OBDal.getInstance().get(BusinessPartner.class, VENDOR_ID);
        Product product = OBDal.getInstance().get(Product.class, PRODUCT_ID);
        params = new String[] { organization.getName(), businessPartner.getIdentifier(),
            product.getIdentifier() };
        break;
      default:
        params = null;
        break;
    }
    return OBMessageUtils.getI18NMessage(validationError, params);
  }
}
