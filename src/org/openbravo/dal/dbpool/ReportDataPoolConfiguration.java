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
package org.openbravo.dal.dbpool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.query.Query;
import org.openbravo.dal.service.DataPoolConfiguration;
import org.openbravo.dal.service.OBDal;

/**
 * Retrieves the database pool configurations for the application reports, i.e., the information
 * about which database pool must be used to retrieve the information for each report.
 */
public class ReportDataPoolConfiguration implements DataPoolConfiguration {
  private static final int REPORT_ID = 0;
  private static final int DATA_POOL = 1;

  @Override
  public Map<String, String> getDataPoolSelection() {
    //@formatter:off
    String hql =
            "select dps.report.id, dps.dataPool " +
            "  from OBUIAPP_Data_Pool_Selection dps " +
            " where dps.dataType = 'REPORT' and dps.report is not null and dps.active = true";
    //@formatter:on
    Query<Object[]> query = OBDal.getInstance().getSession().createQuery(hql, Object[].class);
    List<Object[]> queryResults = query.list();

    Map<String, String> selection = new HashMap<>(queryResults.size());
    for (Object[] values : queryResults) {
      selection.put(values[REPORT_ID].toString(), values[DATA_POOL].toString());
    }
    return selection;
  }

  @Override
  public String getPreferenceName() {
    return "OBUIAPP_DefaultDBPoolForReports";
  }

  @Override
  public String getDataType() {
    return "REPORT";
  }

}
