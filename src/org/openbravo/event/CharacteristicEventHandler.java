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
 * All portions are Copyright (C) 2022 Openbravo SLU
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
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.plm.Characteristic;

/**
 * Event handler for the {@link Characteristic} entity. It ensures that the relevant characteristic
 * configuration is correct.
 */
class CharacteristicEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Characteristic.ENTITY_NAME) };
  private static final String MAIN = "0";

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkRelevantCharacteristicConfig((Characteristic) event.getTargetInstance());
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkRelevantCharacteristicConfig((Characteristic) event.getTargetInstance());
  }

  private void checkRelevantCharacteristicConfig(Characteristic characteristic) {
    if (characteristic.getRelevantCharacteristic() == null) {
      return;
    }
    if (!MAIN.equals(characteristic.getOrganization().getId())) {
      throw new OBException(OBMessageUtils.getI18NMessage("RelevantCharacteristicIncorrectOrg"));
    }
    boolean isRelevantCharacteristicAssigned = OBDal.getInstance()
        .createCriteria(Characteristic.class)
        .setFilterOnReadableOrganization(false)
        .add(Restrictions.ne(Characteristic.PROPERTY_ID, characteristic.getId()))
        .add(Restrictions.eq(Characteristic.PROPERTY_RELEVANTCHARACTERISTIC,
            characteristic.getRelevantCharacteristic()))
        .count() > 0;
    if (isRelevantCharacteristicAssigned) {
      throw new OBException(OBMessageUtils.getI18NMessage("RelevantCharacteristicInUse"));
    }
  }
}
