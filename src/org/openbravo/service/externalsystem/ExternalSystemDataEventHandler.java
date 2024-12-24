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
 * All portions are Copyright (C) 2022-2023 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.externalsystem;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;

/**
 * Clears the cached information kept in the {@ExternalSystemProvider} when the configuration of an
 * external system information changes
 */
class ExternalSystemDataEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = getExternalSystemConfigurationEntities();
  private static final String EXTERNAL_SYSTEM_PROPERTY = "externalSystem";

  private static Entity[] getExternalSystemConfigurationEntities() {
    List<Entity> externalSystemEntities = ModelProvider.getInstance()
        .getModel()
        .stream()
        .filter(entity -> entity.hasProperty(EXTERNAL_SYSTEM_PROPERTY)
            && entity.getProperty(EXTERNAL_SYSTEM_PROPERTY).getEntity() != null
            && entity.getProperty(EXTERNAL_SYSTEM_PROPERTY).isParent()
            && ExternalSystemData.ENTITY_NAME
                .equals(entity.getProperty(EXTERNAL_SYSTEM_PROPERTY).getTargetEntity().getName()))
        .collect(Collectors.toList());
    externalSystemEntities
        .add(ModelProvider.getInstance().getEntity(ExternalSystemData.ENTITY_NAME));
    return externalSystemEntities.toArray(new Entity[externalSystemEntities.size()]);
  }

  @Inject
  @Any
  private ExternalSystemProvider externalSystemProvider;

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    clearCachedExternalSystemInstance(event);
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    clearCachedExternalSystemInstance(event);
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    clearCachedExternalSystemInstance(event);
  }

  private void clearCachedExternalSystemInstance(EntityPersistenceEvent event) {
    BaseOBObject bob = event.getTargetInstance();
    ExternalSystemData config = bob instanceof ExternalSystemData ? ((ExternalSystemData) bob)
        : ((ExternalSystemData) bob.get(EXTERNAL_SYSTEM_PROPERTY));
    if (config != null) {
      externalSystemProvider.invalidateExternalSystem(config.getId());
    }
  }
}
