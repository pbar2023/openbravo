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

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;
import org.openbravo.model.pricing.priceadjustment.Product;

/**
 * Validates mandatory to choose Priority Rule if there is max quantity
 */
public class PriceAdjustmentHandler extends EntityPersistenceEventObserver {
  private static final Entity[] ENTITIES = {
      ModelProvider.getInstance().getEntity(PriceAdjustment.ENTITY_NAME) };
  private static final String PRICE_ADJUSTMENT_ID = "5D4BAF6BB86D4D2C9ED3D5A6FC051579";
  private static final String FIXED_PERCENTAGE_ID = "697A7AB9FD9C4EE0A3E891D3D3CCA0A7";

  private final Entity priceAdjustmentEntity = ModelProvider.getInstance()
      .getEntity(PriceAdjustment.ENTITY_NAME);
  private final Property startingDateProperty = priceAdjustmentEntity
      .getProperty(PriceAdjustment.PROPERTY_STARTINGDATE);
  private final Property endingDateProperty = priceAdjustmentEntity
      .getProperty(PriceAdjustment.PROPERTY_ENDINGDATE);

  @Override
  protected Entity[] getObservedEntities() {
    return ENTITIES;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    PriceAdjustment discount = (PriceAdjustment) event.getTargetInstance();

    validateData(event);
    validatePriceAdjustmentScope(discount);
    validatePriceAdjustmentType(discount);
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    PriceAdjustment discount = (PriceAdjustment) event.getTargetInstance();

    validateData(event);
    updateOrganizationDates(discount, (Date) event.getPreviousState(startingDateProperty),
        (Date) event.getPreviousState(endingDateProperty));
    validatePriceAdjustmentScope(discount);
    validatePriceAdjustmentType(discount);
    validatePriceAdjustmentDates(discount);
  }

  private void validateData(EntityPersistenceEvent event) {
    final Entity offerEntity = ModelProvider.getInstance()
        .getEntity(event.getTargetInstance().getEntityName());
    final BigDecimal maxQty = (BigDecimal) event
        .getCurrentState(offerEntity.getProperty(PriceAdjustment.PROPERTY_MAXQUANTITY));
    final Boolean applyToProduct = (Boolean) event.getCurrentState(
        offerEntity.getProperty(PriceAdjustment.PROPERTY_APPLYTOPRODUCTSUPTOTHEMAXQUANTITY));
    if (maxQty != null && maxQty.compareTo(BigDecimal.ZERO) > 0 && applyToProduct) {
      final String priorityRule = (String) event
          .getCurrentState(offerEntity.getProperty(PriceAdjustment.PROPERTY_PRIORITYRULE));
      if (StringUtils.isBlank(priorityRule)) {
        throw new OBException("@SelectPriorityRule@");
      }
    }
  }

  private void updateOrganizationDates(PriceAdjustment discount, Date previousStartingDate,
      Date previousEndingDate) {
    final boolean isStartingDateChanged = !Objects.equals(previousStartingDate,
        discount.getStartingDate());
    final boolean isEndingDateChanged = !Objects.equals(previousEndingDate,
        discount.getEndingDate());
    if (isStartingDateChanged || isEndingDateChanged) {
    //@formatter:off
      final String hql =
          "update from PricingAdjustmentOrganization e" +
          "  set e.startingDate = :startingDate," +
          "    e.endingDate = :endingDate," +
          "    e.updated = :updated," +
          "    e.updatedBy = :updatedBy" +
          " where e.priceAdjustment.id = :discountId";
      //@formatter:on
      OBDal.getInstance()
          .getSession()
          .createQuery(hql)
          .setParameter("discountId", discount.getId())
          .setParameter("startingDate", discount.getStartingDate())
          .setParameter("endingDate", discount.getEndingDate())
          .setParameter("updated", discount.getUpdated())
          .setParameter("updatedBy", discount.getUpdatedBy())
          .executeUpdate();
    }
  }

  /**
   * Checks that a discounts Included Products field is 'Only those defined' if the Price Adjustment
   * Scope is 'Set a specific price adjustment for each product'.
   *
   * @param discount
   *          The discount that is being created or updated
   */
  private void validatePriceAdjustmentScope(PriceAdjustment discount) {
    if (discount.getDiscountType().getId().equals(PRICE_ADJUSTMENT_ID)
        && discount.getPriceAdjustmentScope().equals("E")
        && !discount.getIncludedProducts().equals("N")) {
      throw new OBException("@PriceAdjustmentScopeError@");
    }
  }

  /**
   * When a Price Adjustment is set for all the products, the Price Adjustment Type field will
   * decide which kind of discount to apply, so the corresponding field needs to be filled. Also,
   * when a Fixed Percentage Discount is set, the percentage needs to be filled.
   * 
   * @param discount
   *          The discount that is being created or updated
   */
  private void validatePriceAdjustmentType(PriceAdjustment discount) {
    if (discount.getDiscountType().getId().equals(PRICE_ADJUSTMENT_ID)
        && discount.getPriceAdjustmentScope().equals("A")) {
      if (discount.getPriceAdjustmentType().equals("A") && discount.getDiscountAmount() == null) {
        throw new OBException("@PriceAdjustmentEmptyField@");
      }
      if (discount.getPriceAdjustmentType().equals("P") && discount.getDiscount() == null) {
        throw new OBException("@PriceAdjustmentEmptyField@");
      }
      if (discount.getPriceAdjustmentType().equals("F") && discount.getFixedPrice() == null) {
        throw new OBException("@PriceAdjustmentEmptyField@");
      }
    }
    if (discount.getDiscountType().getId().equals(FIXED_PERCENTAGE_ID)
        && discount.getDiscount() == null) {
      throw new OBException("@PriceAdjustmentEmptyField@");
    }
  }

  /**
   * For price adjustments that are set for each product, checks that the dates in the Product tab
   * are between the ones in the discount header
   * 
   * @param discount
   *          The discount that is being created or updated
   */
  private void validatePriceAdjustmentDates(PriceAdjustment discount) {
    // Only check price adjustments that are set for each product
    if (!discount.getDiscountType().getId().equals(PRICE_ADJUSTMENT_ID)
        || !discount.getPriceAdjustmentScope().equals("E")) {
      return;
    }

    OBCriteria<Product> productDateCriteria = OBDal.getInstance().createCriteria(Product.class);
    productDateCriteria
        .add(Restrictions.eq(Product.PROPERTY_PRICEADJUSTMENT + "." + PriceAdjustment.PROPERTY_ID,
            discount.getId()))
        .add(Restrictions.or(
            Restrictions.lt(Product.PROPERTY_STARTINGDATE, discount.getStartingDate()),
            Restrictions.gt(Product.PROPERTY_ENDINGDATE, discount.getEndingDate())))
        .setMaxResults(1);

    Product wrongProduct = (Product) productDateCriteria.uniqueResult();
    if (wrongProduct != null) {
      throw new OBException(
          String.format(OBMessageUtils.messageBD("PriceAdjustmentProductDateErrorWithProduct"),
              wrongProduct.getIdentifier()));
    }
  }
}
