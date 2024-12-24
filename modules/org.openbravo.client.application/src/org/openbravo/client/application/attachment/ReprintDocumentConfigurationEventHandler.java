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

package org.openbravo.client.application.attachment;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Invalidates the {@link ReprintableDocumentManager} cache that keeps the information about if
 * document reprinting is enabled for an {@link Organization} when that configuration is changed.
 */
class ReprintDocumentConfigurationEventHandler extends EntityPersistenceEventObserver {

  private static final Entity[] ENTITIES = {
      ModelProvider.getInstance().getEntity(Organization.ENTITY_NAME) };

  @Inject
  private ReprintableDocumentManager reprintableDocumentManager;

  @Override
  protected Entity[] getObservedEntities() {
    return ENTITIES;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    if (isConfigurationChanged(event)) {
      Organization org = ((Organization) event.getTargetInstance());
      reprintableDocumentManager.invalidateReprintDocumentConfigurationCache(org);
    }
  }

  private boolean isConfigurationChanged(EntityUpdateEvent event) {
    Property property = ENTITIES[0].getProperty(Organization.PROPERTY_REPRINTDOCUMENTS);
    String current = (String) event.getCurrentState(property);
    String previous = (String) event.getPreviousState(property);
    return current != null ? !current.equals(previous) : previous != null;
  }
}
