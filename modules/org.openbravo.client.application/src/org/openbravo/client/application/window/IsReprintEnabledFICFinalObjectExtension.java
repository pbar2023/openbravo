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
 * All portions are Copyright (C) 2023-2024 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.application.window;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.OrganizationEnabled;
import org.openbravo.client.application.attachment.ReprintableDocumentManager;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Checks if we have any selected record relative to an organization with the reprintable documents
 * feature enabled or not.
 */
@ApplicationScoped
public class IsReprintEnabledFICFinalObjectExtension implements FICFinalObjectExtension {

  @Inject
  private ReprintableDocumentManager reprintableDocumentManager;

  @Override
  public JSONObject execute(String mode, Tab tab, Map<String, JSONObject> columnValues,
      BaseOBObject row, JSONObject jsContent) throws JSONException {
    JSONObject newObject = new JSONObject();
    if (!((mode.equals("NEW") || mode.equals("EDIT") || mode.equals("SETSESSION"))
        && (jsContent.has("C_Order_ID") || jsContent.has("C_Invoice_ID")
            || jsContent.has("C_Substitutiveinvoice_ID")))
        || !isReprintableWindow(tab)) {
      newObject.put("isReprintEnabled", false);
      return newObject;
    }

    if (row instanceof OrganizationEnabled) {
      boolean isReprintEnabled = false;
      if (jsContent.has("MULTIPLE_ROW_IDS")) {
        JSONArray selectedRecordIds = jsContent.getJSONArray("MULTIPLE_ROW_IDS");
        for (int i = 0; i < selectedRecordIds.length(); i++) {
          String selectedRecordId = selectedRecordIds.getString(i);
          OrganizationEnabled selectedRecord = (OrganizationEnabled) OBDal.getInstance()
              .get(row.getClass(), selectedRecordId);
          if (reprintableDocumentManager
              .isReprintDocumentsEnabled(selectedRecord.getOrganization().getId())) {
            isReprintEnabled = true;
            break;
          }
        }
      } else {
        Organization organization = ((OrganizationEnabled) row).getOrganization();
        if (organization != null
            && reprintableDocumentManager.isReprintDocumentsEnabled(organization.getId())) {
          isReprintEnabled = true;
        }
      }
      newObject.put("isReprintEnabled", isReprintEnabled);
    }

    return newObject;
  }

  private boolean isReprintableWindow(Tab tab) {
    return reprintableDocumentManager.isReprintableDocumentsWindow(tab.getWindow().getId());
  }
}
