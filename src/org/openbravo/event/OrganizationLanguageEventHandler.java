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
 * All portions are Copyright (C) 2024 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.OrganizationLanguage;

/**
 * Event Handler for AD_Org_Language.
 * 
 * Checks if there is more that one record defined as default per organization
 */
class OrganizationLanguageEventHandler extends EntityPersistenceEventObserver {
  private static final Entity[] entities = {
      ModelProvider.getInstance().getEntity(OrganizationLanguage.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    validateIsDefaultDefinition((OrganizationLanguage) event.getTargetInstance());
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    validateIsDefaultDefinition((OrganizationLanguage) event.getTargetInstance());
  }

  private void validateIsDefaultDefinition(OrganizationLanguage updatedOrgLan) {
    if (updatedOrgLan.isDefault()) {
      for (OrganizationLanguage orgLan : updatedOrgLan.getOrganization()
          .getOrganizationLanguageList()) {
        if (orgLan.isDefault() && orgLan.getId() != updatedOrgLan.getId()) {
          throw new OBException(
              OBMessageUtils.getI18NMessage("AD_DuplicatedIsDefaultForOrgLanNotAllowed"));
        }
      }
    }
  }

}
