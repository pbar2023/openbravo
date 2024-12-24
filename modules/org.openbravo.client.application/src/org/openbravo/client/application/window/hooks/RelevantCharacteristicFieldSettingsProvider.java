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
package org.openbravo.client.application.window.hooks;

import org.openbravo.client.application.window.FieldSettingsProvider;
import org.openbravo.client.kernel.reference.UIDefinition;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.RelevantCharacteristicProperty;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.ui.Field;

/**
 * Provides the view settings of the fields displaying properties which make reference to relevant
 * characteristics
 */
public class RelevantCharacteristicFieldSettingsProvider implements FieldSettingsProvider {
  private static final String RELEVANT_CHARACTERISTIC_REFERENCE = "243FB7EE87FD477E9DF1F14E098C645F";

  @Override
  public boolean accepts(Field field) {
    return RelevantCharacteristicProperty.from(field).isPresent();
  }

  @Override
  public UIDefinition getUIDefinition(Field field) {
    try {
      OBContext.setAdminMode(true);
      Reference reference = OBDal.getInstance()
          .getProxy(Reference.class, RELEVANT_CHARACTERISTIC_REFERENCE);
      return UIDefinitionController.getInstance().getUIDefinition(reference);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Override
  public boolean isReadOnly(Field field) {
    return true;
  }
}
