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
package org.openbravo.client.application.messageclient;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.model.ad.domain.Preference;

/**
 * Used to stop the MessageClientManager when the preference to enable it is set to false
 */
public class MessageClientManagerEnabledPreferenceChangeEventHandler
    extends EntityPersistenceEventObserver {

  private static final Entity[] ENTITIES = {
      ModelProvider.getInstance().getEntity(Preference.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return ENTITIES;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    MessageClientManager.getInstance().shutdown();
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    MessageClientManager.getInstance().shutdown();
  }

  @Override
  protected boolean isValidEvent(EntityPersistenceEvent event) {
    if (!super.isValidEvent(event)) {
      return false;
    }

    Preference preference = (Preference) event.getTargetInstance();
    if (!MessageClientManager.MESSAGE_MANAGER_ENABLED_PREFERENCE.equals(preference.getProperty())) {
      return false;
    }

    return preference.getClient().getId().equals("0") && preference.isActive()
        && "N".equals(preference.getSearchKey());
  }
}
