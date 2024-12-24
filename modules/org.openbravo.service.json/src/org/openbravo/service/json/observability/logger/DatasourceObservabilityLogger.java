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

package org.openbravo.service.json.observability.logger;

import java.util.Date;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.service.datasource.DatasourceObservabilityConfig;
import org.openbravo.service.json.DataEntityQueryService;

@ApplicationScoped
public class DatasourceObservabilityLogger {

  private static final Logger log = LogManager.getLogger("DatasourceEvents");

  public void logIfNeeded(Map<String, String> parameters, DataEntityQueryService queryService,
      long queryTime) {
    try {
      OBContext.setAdminMode(true);
      String tabId = parameters.get("tabId");
      if (StringUtils.isBlank(tabId)) {
        return;
      }
      Tab tab = OBDal.getInstance().get(Tab.class, tabId);
      DatasourceObservabilityConfig config = getConfig(tab);
      if (config == null || (config.getMinMs() != null && config.getMinMs() > queryTime)) {
        return;
      }
      String queryString = queryService.buildOBQuery().createQuery().getQueryString();
      JSONObject event = new JSONObject();
      event.put("type", "dsEvent");
      event.put("tabIdentifier", tab.getIdentifier());
      event.put("logTime", new Date());
      event.put("duration", queryTime);
      event.put("query", queryString);
      event.put("params", parameters);
      log.info(event);
    } catch (JSONException e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private DatasourceObservabilityConfig getConfig(Tab tab) {
    OBCriteria<DatasourceObservabilityConfig> criteria = OBDal.getInstance()
        .createCriteria(DatasourceObservabilityConfig.class);
    criteria.add(Restrictions.eq(DatasourceObservabilityConfig.PROPERTY_TAB, tab));
    criteria.setFilterOnReadableClients(false);
    criteria.setFilterOnReadableOrganization(false);
    return (DatasourceObservabilityConfig) criteria.uniqueResult();
  }

}
