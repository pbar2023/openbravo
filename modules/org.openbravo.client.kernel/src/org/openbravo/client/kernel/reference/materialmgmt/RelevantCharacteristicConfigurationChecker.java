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
package org.openbravo.client.kernel.reference.materialmgmt;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.ExtraWindowSettingsInjector;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.RelevantCharacteristicProperty;

/**
 * Checks if there are relevant characteristic property fields on a window which make reference to
 * relevant characteristics that are not linked to a product characteristic. If detected, it shows a
 * warning message when the window is opened in order to inform the user about the missing
 * configuration.
 */
public class RelevantCharacteristicConfigurationChecker implements ExtraWindowSettingsInjector {
  private static final Logger log = LogManager.getLogger();
  private static final String MESSAGE = "RelevantCharacteristicNotLinked";
  private static final String WARNING_TYPE = "warning";
  private static final List<String> EXTRA_CALLBACKS = List.of("OB.Utilities.showWindowMessage");

  @Inject
  private ApplicationDictionaryCachedStructures adcs;

  @Override
  public Map<String, Object> doAddSetting(Map<String, Object> parameters, JSONObject json)
      throws OBException {

    String windowId = (String) parameters.get("windowId");
    Map<String, Set<String>> nonConfiguredFields = getWindowFieldsWithNonConfiguredRelevantCharacteristics(
        windowId);

    if (nonConfiguredFields.isEmpty()) {
      return Collections.emptyMap();
    }

    JSONArray messageParams = new JSONArray();
    messageParams.put(nonConfiguredFields.entrySet()
        .stream()
        .map(this::stringify)
        .collect(Collectors.joining(", ")));
    //@formatter:off
    return Map.of(
        "message", MESSAGE, 
        "messageType", WARNING_TYPE,
        "messageParams", messageParams, 
        "extraCallbacks", EXTRA_CALLBACKS);
   //@formatter:on
  }

  private Map<String, Set<String>> getWindowFieldsWithNonConfiguredRelevantCharacteristics(
      String windowId) {
    long t1 = System.currentTimeMillis();
    try {
      return adcs.getWindow(windowId)
          .getADTabList()
          .stream()
          .map(tab -> Map.entry(tab.getId(),
              getTabFieldsWithNonConfiguredRelevantCharacteristics(tab.getId())))
          .filter(entry -> !entry.getValue().isEmpty())
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    } finally {
      log.trace("Relevant characteristic field search for window {} completed in {} ms.", windowId,
          (System.currentTimeMillis() - t1));
    }
  }

  private Set<String> getTabFieldsWithNonConfiguredRelevantCharacteristics(String tabId) {
    return adcs.getFieldsOfTab(tabId)
        .stream()
        .filter(f -> f.getProperty() != null && f.getColumn() == null && f.getClientclass() == null)
        .map(f -> RelevantCharacteristicProperty.from(f).orElse(null))
        .filter(p -> p != null && p.getCharacteristicId() == null)
        .map(RelevantCharacteristicProperty::getFieldName)
        .collect(Collectors.toSet());
  }

  private String stringify(Entry<String, Set<String>> entry) {
    String result = entry.getValue().stream().collect(Collectors.joining(", "));
    result += " (" + adcs.getTab(entry.getKey()).getName() + " "
        + OBMessageUtils.getI18NMessage("OBUIAPP_Tab") + ")";
    return result;
  }

}
