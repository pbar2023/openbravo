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

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryUtil.SequenceType;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventoryType;

/**
 * This event handler clears the Sequence in Handling Unit Type if Sequence Type is not Global
 */

class ReferencedInventoryEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ReferencedInventoryType.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    clearSequenceInRefInvType(event);
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    clearSequenceInRefInvType(event);
  }

  /**
   * This method clears the Sequence in Handling Unit Type if Sequence Type is not Global
   */
  private void clearSequenceInRefInvType(EntityPersistenceEvent event) {
    final Entity refInvType = ModelProvider.getInstance()
        .getEntity(ReferencedInventoryType.ENTITY_NAME);
    final Property sequenceTypeProperty = refInvType
        .getProperty(ReferencedInventoryType.PROPERTY_SEQUENCETYPE);
    final Property sequenceProperty = refInvType
        .getProperty(ReferencedInventoryType.PROPERTY_SEQUENCE);
    String currentSequenceType = (String) event.getCurrentState(sequenceTypeProperty);
    Sequence sequence = (Sequence) event.getCurrentState(sequenceProperty);

    if (!StringUtils.equals(currentSequenceType, SequenceType.GLOBAL.value) && sequence != null) {
      event.setCurrentState(sequenceProperty, null);
    }
  }

}
