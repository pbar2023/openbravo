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
 * All portions are Copyright (C) 2023 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.datasource;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.service.json.JsonUtils;

/**
 * This Datasource shows a list of Time zones according to Java's ZoneId class
 *
 * @see ZoneId
 */
public class TimezoneDatasource extends ReadOnlyDataSourceService {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected int getCount(Map<String, String> parameters) {
    return ZoneId.getAvailableZoneIds().size();
  }

  @Override
  protected List<Map<String, Object>> getData(Map<String, String> parameters, int startRow,
      int endRow) {
    int rowsToFetch = endRow - startRow + 1;
    Optional<JSONArray> criteria = getCriteria(parameters);
    boolean isCriteriaPresent = criteria.isPresent();

    return ZoneId.getAvailableZoneIds()
        .stream() //
        .filter(r -> !isCriteriaPresent || filterRow(r, criteria.get())) //
        .skip(startRow) //
        .limit(rowsToFetch) //
        .sorted()
        .map(l -> {
          Map<String, Object> r = new HashMap<>(2);
          r.put("id", l);
          r.put("_identifier", l);
          return r;
        }) //
        .collect(Collectors.toList());
  }

  private Optional<JSONArray> getCriteria(Map<String, String> parameters) {
    if (parameters.containsKey("criteria")) {
      try {
        return Optional.of((JSONArray) JsonUtils.buildCriteria(parameters).get("criteria"));
      } catch (JSONException e) {
        log.error("Failed to build criteria from parameters", e);
      }
    }
    return Optional.empty();
  }

  private static boolean filterRow(String row, JSONArray criteria) {
    boolean meetsCriteria = true;
    try {
      for (int i = 0; i < criteria.length(); i++) {
        JSONObject criterion = criteria.getJSONObject(i);
        String field = criterion.getString("fieldName");
        String value = criterion.getString("value").toLowerCase();

        if ("_identifier".equals(field)) {
          meetsCriteria = row.toLowerCase().contains(value);
        }
      }
    } catch (JSONException e) {
      log.error("Error matching criteria", e);
    }

    return meetsCriteria;
  }

  // This Datasource does not have any table to check access, so we override this function to avoid
  // this check
  @Override
  public void checkFetchDatasourceAccess(Map<String, String> parameters) {
  }
}
