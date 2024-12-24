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
 * All portions are Copyright (C) 2023 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.event;

import javax.enterprise.event.Observes;

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
import org.openbravo.model.materialmgmt.transaction.InventoryCount;

/**
 * Event handler that ensures that the documentNo is a unique value in each organization.
 * 
 * Since the documentNo is not mandatory and has null values, it could not be implemented as a
 * unique constraint in the database as this behavior fails in Oracle.
 */

class PhysicalInventoryEventHandler extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(InventoryCount.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onNew(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    InventoryCount inventoryCount = (InventoryCount) event.getTargetInstance();
    if (inventoryCount.getDocumentNo() != null) {
      existRecordSameDocumentNoAndOrg(event);
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final Entity entity = ModelProvider.getInstance().getEntity(InventoryCount.class);
    final Property propertyDocumentNo = entity.getProperty(InventoryCount.PROPERTY_DOCUMENTNO);

    if (event.getCurrentState(propertyDocumentNo) != null
        && (!event.getCurrentState(propertyDocumentNo)
            .equals(event.getPreviousState(propertyDocumentNo)))) {
      existRecordSameDocumentNoAndOrg(event);
    }
  }

  private void existRecordSameDocumentNoAndOrg(EntityPersistenceEvent event) {
    if (event instanceof EntityNewEvent || event instanceof EntityUpdateEvent) {
      InventoryCount inventoryCount = (InventoryCount) event.getTargetInstance();
      OBCriteria<InventoryCount> criteria = OBDal.getInstance()
          .createCriteria(InventoryCount.class);
      criteria.add(Restrictions.eq("organization.id", inventoryCount.getOrganization().getId()));
      criteria.add(Restrictions.eq("documentNo", inventoryCount.getDocumentNo()));
      criteria.setMaxResults(1);
      InventoryCount inventoryCountResult = (InventoryCount) criteria.uniqueResult();
      if (inventoryCountResult != null) {
        throw new OBException(
            OBMessageUtils.getI18NMessage("PhysicalInventoryDocNoOrgCheck", null));
      }
    }
  }
}
