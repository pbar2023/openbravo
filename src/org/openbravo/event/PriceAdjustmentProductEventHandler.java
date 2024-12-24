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

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;
import org.openbravo.model.pricing.priceadjustment.Product;

/**
 * Validates the Price Adjustment Products on events
 */
public class PriceAdjustmentProductEventHandler extends EntityPersistenceEventObserver {
  private static final Entity[] ENTITIES = {
      ModelProvider.getInstance().getEntity(Product.ENTITY_NAME) };
  private static final String PRICE_ADJUSTMENT_ID = "5D4BAF6BB86D4D2C9ED3D5A6FC051579";

  @Override
  protected Entity[] getObservedEntities() {
    return ENTITIES;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final Product discountProduct = (Product) event.getTargetInstance();
    validatePriceAdjustmentType(discountProduct);
    validateDates(discountProduct);
    validateOverlappingDates(discountProduct);
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final Product discountProduct = (Product) event.getTargetInstance();
    validatePriceAdjustmentType(discountProduct);
    validateDates(discountProduct);
    validateOverlappingDates(discountProduct);
  }

  /**
   * When a Price Adjustment is set for each product, the Price Adjustment Type field will decide
   * which kind of discount to apply, so the corresponding field needs to be filled.
   * 
   * @param discountProduct
   *          The Product where a discount is applied that we need to check
   */
  private void validatePriceAdjustmentType(Product discountProduct) {
    final PriceAdjustment discount = discountProduct.getPriceAdjustment();
    if (!discount.getDiscountType().getId().equals(PRICE_ADJUSTMENT_ID)
        || !discount.getPriceAdjustmentScope().equals("E")) {
      return;
    }
    if (discountProduct.getPriceAdjustmentType().equals("A")
        && discountProduct.getDiscountAmount() == null) {
      throw new OBException(OBMessageUtils.messageBD("PriceAdjustmentEmptyField@"));
    }
    if (discountProduct.getPriceAdjustmentType().equals("P")
        && discountProduct.getDiscount() == null) {
      throw new OBException(OBMessageUtils.messageBD("PriceAdjustmentEmptyField"));
    }
    if (discountProduct.getPriceAdjustmentType().equals("F")
        && discountProduct.getFixedPrice() == null) {
      throw new OBException(OBMessageUtils.messageBD("PriceAdjustmentEmptyField"));
    }
  }

  /**
   * For price adjustments that are set for each product, checks that the dates in the Product tab
   * are between the ones in the discount header
   * 
   * @param discountProduct
   *          The Product where a discount is applied that we need to check
   */
  private void validateDates(Product discountProduct) {
    final PriceAdjustment discount = discountProduct.getPriceAdjustment();

    // Only check price adjustments that are set for each product
    if (!discount.getDiscountType().getId().equals(PRICE_ADJUSTMENT_ID)
        || !discount.getPriceAdjustmentScope().equals("E")) {
      return;
    }
    if (discountProduct.getStartingDate() != null
        && discountProduct.getStartingDate().before(discount.getStartingDate())) {
      throw new OBException(OBMessageUtils.messageBD("PriceAdjustmentProductDateError"));
    }
    if (discountProduct.getEndingDate() != null && discount.getEndingDate() != null
        && discountProduct.getEndingDate().after(discount.getEndingDate())) {
      throw new OBException(OBMessageUtils.messageBD("PriceAdjustmentProductDateError"));
    }
  }

  /**
   * For price adjustments that are set for each product, checks that the dates in the Product tab
   * do not overlap between them
   *
   * @param discountProduct
   *          The Product where a discount is applied that we need to check
   */
  private void validateOverlappingDates(Product discountProduct) {
    final PriceAdjustment discount = discountProduct.getPriceAdjustment();

    // check if we have 2 times or more the current product in current discount

    // @formatter:off
      final String whereClause =
              " where id != :id " +
              "   and priceAdjustment.id = :discountId " +
              "   and product.id = :productId ";
      // @formatter:on
    final OBQuery<Product> criteria = OBDal.getInstance().createQuery(Product.class, whereClause);
    criteria.setNamedParameter("id", discountProduct.getId());
    criteria.setNamedParameter("discountId", discount.getId());
    criteria.setNamedParameter("productId", discountProduct.getProduct().getId());
    criteria.setMaxResult(1);
    if (criteria.count() > 0) {
      if (!discount.getDiscountType().getId().equals(PRICE_ADJUSTMENT_ID)
          || !discount.getPriceAdjustmentScope().equals("E")) {
        // Discounts that are not price adjustment or doesn't have the scope "each product"
        // Should work like before, since we have remove the unique constraint, we have to implement
        // it here
        throw new OBException(OBMessageUtils.messageBD("M_OFFER_PRODUCT_UNIQUE"));
      } else {
        // For price adjustment discount with each product scope with more than one instance of
        // current product in current discount,
        // we need to ensure that start and end dates are not null
        // and that the dates doesn't overlap

        // @formatter:off
          final String whereClause2 =
                  " where  id != :id " +
                  "   and priceAdjustment.id = :discountId " +
                  "   and  product.id = :productId" +
                  "   and ( startingDate is null or endingDate is null) ";
          // @formatter:on
        final OBQuery<Product> criteria2 = OBDal.getInstance()
            .createQuery(Product.class, whereClause2);
        criteria2.setNamedParameter("id", discountProduct.getId());
        criteria2.setNamedParameter("discountId", discount.getId());
        criteria2.setNamedParameter("productId", discountProduct.getProduct().getId());
        criteria2.setMaxResult(1);
        if (criteria2.count() > 0 || discountProduct.getStartingDate() == null
            || discountProduct.getEndingDate() == null) {
          throw new OBException(OBMessageUtils.messageBD("StartAndEndDatesShouldBeFilled"));
        }

        // @formatter:off
        final String whereClause3 =
                " where id != :id "+
                "   and priceAdjustment.id = :discountId " +
                "   and product.id = :productId" +
                "   and (" +
                "        ( startingDate <= :fromDate and :fromDate <= endingDate ) " +
                "        or " +
                "        ( startingDate <= :toDate and :toDate <= endingDate ) " +
                "        or " +
                "        ( :fromDate <= startingDate and endingDate <= :toDate  ) " +
                "     )";
        // @formatter:on
        final OBQuery<Product> criteria3 = OBDal.getInstance()
            .createQuery(Product.class, whereClause3);
        criteria3.setNamedParameter("id", discountProduct.getId());
        criteria3.setNamedParameter("discountId", discount.getId());
        criteria3.setNamedParameter("productId", discountProduct.getProduct().getId());
        criteria3.setNamedParameter("fromDate", discountProduct.getStartingDate());
        criteria3.setNamedParameter("toDate", discountProduct.getEndingDate());
        criteria3.setMaxResult(1);
        if (criteria3.count() > 0) {
          throw new OBException(OBMessageUtils.messageBD("ThereIsOverlapOfDates"));
        }
      }
    }
  }
}
