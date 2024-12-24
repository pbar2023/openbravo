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
 * All portions are Copyright (C) 2022 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.event;

import javax.enterprise.event.Observes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.pricing.priceadjustment.OrganizationFilter;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;

/**
 * Event handler that ensures that the organization filter in discount are correctly created.
 */
class PricingAdjustmentOrganizationEventHandler extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(OrganizationFilter.ENTITY_NAME) };
  protected Logger logger = LogManager.getLogger();

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    validateOrganizationFilter((OrganizationFilter) event.getTargetInstance());
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    validateOrganizationFilter((OrganizationFilter) event.getTargetInstance());
  }

  private void validateOrganizationFilter(OrganizationFilter organizationFilter) {
    PriceAdjustment discount = organizationFilter.getPriceAdjustment();
    if (organizationFilter.getEndingDate() != null) {
      if (organizationFilter.getStartingDate() == null) {
        throw new OBException(OBMessageUtils.getI18NMessage("StartingDateMandatoryWithEndingDate"));
      } else if (organizationFilter.getEndingDate().compareTo(discount.getStartingDate()) < 0
          || (discount.getEndingDate() != null
              && organizationFilter.getEndingDate().compareTo(discount.getEndingDate()) > 0)) {
        String[] messageParam = { (OBDateUtils.formatDate(discount.getStartingDate()) + " < "
            + (discount.getEndingDate() != null ? OBDateUtils.formatDate(discount.getEndingDate())
                : "?")) };
        throw new OBException(
            OBMessageUtils.getI18NMessage("InvalidEndingDateWithHeader", messageParam));
      }
    }
    if (organizationFilter.getStartingDate() != null
        && (organizationFilter.getStartingDate().compareTo(discount.getStartingDate()) < 0
            || (discount.getEndingDate() != null
                && organizationFilter.getStartingDate().compareTo(discount.getEndingDate()) > 0))) {
      String[] messageParam = { (OBDateUtils.formatDate(discount.getStartingDate()) + " < "
          + (discount.getEndingDate() != null ? OBDateUtils.formatDate(discount.getEndingDate())
              : "?")) };
      throw new OBException(
          OBMessageUtils.getI18NMessage("InvalidStartingDateWithHeader", messageParam));
    }
  }
}
