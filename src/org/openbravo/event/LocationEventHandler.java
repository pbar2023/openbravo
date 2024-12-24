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
 * All portions are Copyright (C) 2023 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.event;

import javax.enterprise.event.Observes;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.Location;

class LocationEventHandler extends EntityPersistenceEventObserver {
  private static final Entity[] entities = {
      ModelProvider.getInstance().getEntity(Location.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkSingleDefaultAddress(event);
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkSingleDefaultAddress(event);
  }

  private void checkSingleDefaultAddress(EntityPersistenceEvent event) {
    final Location loc = (Location) event.getTargetInstance();
    // Check default ship to address
    if (loc.isDefaultShipToAddress()) {
      if (!loc.isShipToAddress()) {
        throw new OBException(OBMessageUtils.messageBD("ValidDefaultShipLocation"));
      }
      checkSingleDefaultAddress(loc, Location.PROPERTY_DEFAULTSHIPTOADDRESS,
          "DuplicatedDefaultShipLocation");
    }
    // Check default invoice to address
    if (loc.isDefaultInvoiceToAddress()) {
      if (!loc.isInvoiceToAddress()) {
        throw new OBException(OBMessageUtils.messageBD("ValidDefaultInvoiceLocation"));
      }
      checkSingleDefaultAddress(loc, Location.PROPERTY_DEFAULTINVOICETOADDRESS,
          "DuplicatedDefaultInvoiceLocation");
    }
  }

  private void checkSingleDefaultAddress(Location loc, String field, String error) {
    final OBCriteria<?> criteria = OBDal.getInstance().createCriteria(Location.ENTITY_NAME);
    criteria.add(Restrictions.ne(Location.PROPERTY_ID, loc.getId()));
    criteria.add(Restrictions.eq(Location.PROPERTY_ACTIVE, true));
    criteria.add(Restrictions.eq(field, true));
    criteria.add(Restrictions.eq(Location.PROPERTY_BUSINESSPARTNER, loc.getBusinessPartner()));
    criteria.setMaxResults(1);
    if (criteria.uniqueResult() != null) {
      throw new OBException(OBMessageUtils.messageBD(error));
    }
  }
}
