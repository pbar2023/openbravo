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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.openbravo.base.structure.OrganizationEnabled;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.ApprovedVendor;
import org.openbravo.model.common.plm.Product;

/**
 * Used to validate purchase order lines according to the purchasing configuration.
 */
public class PurchaseOrderDocumentLine extends IncomingGoodsDocumentLine<OrderLine> {

  private static final String PURCHASE_ORDER_WINDOW_ID = "181";

  public PurchaseOrderDocumentLine(OrderLine bob) {
    super(bob);
  }

  @Override
  protected boolean isPurchaseDocument() {
    return !getLine().getSalesOrder().isSalesTransaction();
  }

  @Override
  protected OrganizationEnabled getDocumentHeader() {
    return getLine().getSalesOrder();
  }

  @Override
  protected Optional<BusinessPartner> getBusinessPartner() {
    return Optional.of(getLine().getSalesOrder().getBusinessPartner());
  }

  @Override
  protected BigDecimal getLineQuantity() {
    return getLine().getOrderedQuantity();
  }

  @Override
  public void validate() {
    if (!isValidationEnabled(ENABLE_PO_VALIDATION_PREFERENCE)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      Optional<ApprovedVendor> optAppV = getApprovedVendor();
      optAppV.ifPresentOrElse(this::validate, () -> {
        String orgName = getDocumentHeader().getOrganization().getName();
        Product product = getLine().getProduct();
        BusinessPartner vendor = getLine().getBusinessPartner();
        String businessPartnerIdentifier = vendor != null ? vendor.getIdentifier() : "";
        String productIdentifier = product != null ? product.getIdentifier() : "";
        throw new PurchaseDocumentValidationError("NoPurchaseConfiguration",
            new String[] { orgName, businessPartnerIdentifier, productIdentifier });
      });
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void validate(ApprovedVendor approvedVendor) {
    validateLineQuantity(approvedVendor.getMinimumOrderQty(), approvedVendor.getStandardQuantity(),
        approvedVendor.getQuantityType());
  }

  private Optional<ApprovedVendor> getApprovedVendor() {
    OrderLine orderLine = getLine();
    Product p = orderLine.getProduct();
    List<String> naturalTree = OBContext.getOBContext()
        .getOrganizationStructureProvider()
        .getParentList(orderLine.getOrganization().getId(), true);

    String hql = "as prpo where prpo.product.id = :productID and prpo.organization.id in :orgList";
    List<ApprovedVendor> approvedVendorList = OBDal.getInstance()
        .createQuery(ApprovedVendor.class, hql)
        .setNamedParameter("orgList", naturalTree)
        .setNamedParameter("productID", p.getId())
        .list();

    ApprovedVendor closestApprovedVendor = (ApprovedVendor) OBContext.getOBContext()
        .getOrganizationStructureProvider()
        // Using the organization set at the order line level instead of the user's
        .getBOBInClosestOrg(approvedVendorList, orderLine.getOrganization().getId());

    return Optional.ofNullable(closestApprovedVendor);
  }

  @Override
  protected String getWindowId() {
    return PURCHASE_ORDER_WINDOW_ID;
  }
}
