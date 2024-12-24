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
package org.openbravo.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.materialmgmt.PurchaseOrderDocumentLine;
import org.openbravo.model.common.order.OrderLine;

/**
 * Event handler executed when creating or updating lines of purchase orders. It checks that the
 * lines of those documents met restrictions imposed by the configuration done in the purchasing
 * tab.
 * 
 */
class PurchaseOrderLineEventHandler extends EntityPersistenceEventObserver {
  private static final Entity[] PURCHASE_ORDER_LINE_ENTITIES = {
      ModelProvider.getInstance().getEntity(OrderLine.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return PURCHASE_ORDER_LINE_ENTITIES;
  }

  @Override
  protected boolean isValidEvent(EntityPersistenceEvent event) {
    boolean isCorrectEntity = super.isValidEvent(event);
    if (!isCorrectEntity) {
      return isCorrectEntity;
    }
    boolean isSalesDocument = ((OrderLine) event.getTargetInstance()).getSalesOrder()
        .isSalesTransaction();
    return !isSalesDocument;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    validate(event.getTargetInstance());
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    validate(event.getTargetInstance());
  }

  private void validate(BaseOBObject bob) {
    new PurchaseOrderDocumentLine((OrderLine) bob).validate();
  }
}
