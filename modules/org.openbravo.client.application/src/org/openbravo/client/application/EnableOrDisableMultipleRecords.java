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
package org.openbravo.client.application;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.SessionInfo;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.service.json.JsonUtils;

/**
 * Action handler which can enable/disable multiple records in one transaction.
 */
@ApplicationScoped
public class EnableOrDisableMultipleRecords extends BaseActionHandler {

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    OBContext.setAdminMode(true);
    try {
      final JSONObject dataObject = new JSONObject(data);
      final String tabId = dataObject.getString("tabId");
      // action = true will result in selected records being deactivated, active = false will
      // deactivate them
      final boolean action = Boolean.parseBoolean(dataObject.getString("action"));
      final JSONArray jsonRecordIds = dataObject.getJSONArray("recordIds");
      HashSet<String> recordIds = new HashSet<>();
      for (int i = 0; i < jsonRecordIds.length(); i++) {
        recordIds.add((String) jsonRecordIds.get(i));
      }
      Tab tab = OBDal.getInstance().get(Tab.class, tabId);
      String tableId = tab.getTable().getId();
      Entity entity = ModelProvider.getInstance().getEntityByTableId(tableId);

      final JSONObject jsonResponse = new JSONObject();

      if (!OBContext.getOBContext().getEntityAccessChecker().isWritable(entity)) {
        throw new OBSecurityException("Entity " + entity + " is not writable by this user");
      }

      // Set information for audit trail
      SessionInfo.setProcessType("W");
      SessionInfo.setProcessId(tab.getId());
      SessionInfo.setUserId(OBContext.getOBContext().getUser().getId());
      SessionInfo.saveContextInfoIntoDB(OBDal.getInstance().getConnection(false));

      final String whereClause = " e where e.id in (:recordIds)" + " and e.active <> :active";

      final OBQuery<BaseOBObject> baseOBObjectQuery = OBDal.getInstance()
          .createQuery(entity.getName(), whereClause);
      baseOBObjectQuery.setNamedParameter("recordIds", recordIds);
      baseOBObjectQuery.setNamedParameter("active", action);
      baseOBObjectQuery.setFilterOnActive(false);

      final List<BaseOBObject> baseOBObjects = baseOBObjectQuery.list();

      for (BaseOBObject bo : baseOBObjects) {
        bo.set("active", action);
        // ideally we would prefer not to flush inside the loop and do it only once, outside
        // but some event handlers check the number of active records (i.e. to check number is
        // higher than a given amount), and if data is flushed only at the end, those event handlers
        // will not have the current information of the number of active records
        OBDal.getInstance().flush();
      }
      int updateCount = baseOBObjects.size();

      jsonResponse.put("updateCount", updateCount);
      return jsonResponse;
    } catch (Exception e) {
      try {
        JSONObject errorResponse = new JSONObject(JsonUtils.convertExceptionToJson(e));
        JSONObject response = errorResponse.getJSONObject("response");
        JSONObject error = response.getJSONObject("error");
        error.put("message",
            OBMessageUtils.getI18NMessage("AllUpdatesCanceled") + error.getString("message"));
        return errorResponse;
      } catch (JSONException t) {
        throw new OBException(t);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
