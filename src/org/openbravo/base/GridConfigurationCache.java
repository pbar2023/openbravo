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

package org.openbravo.base;

import static java.util.Comparator.comparing;

import java.time.Duration;
import java.util.Optional;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.provider.OBSingleton;
import org.openbravo.cache.TimeInvalidatedCache;
import org.openbravo.client.application.GCSystem;
import org.openbravo.client.application.GCTab;
import org.openbravo.client.application.window.OBViewUtil;
import org.openbravo.client.application.window.StandardWindowComponent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;

/**
 * Cache that stores the System Grid Configuration and Tab Grid Configuration. It uses a
 * TimeInvalidatedCache to avoid initializing this information too often. To avoid the potential
 * case where we store several times the same configuration (e.g. every tab that has no
 * configuration will use the system one), we cache separately the system and the tab ones and we
 * compose them into a JSONObject with the getGridConfigurationForTab method.
 *
 * If this information changes, some time will passed until the update is reflected on the cache
 * (see expireAfterDuration in cache builder)
 */
public class GridConfigurationCache implements OBSingleton {

  private static GridConfigurationCache instance;

  private static TimeInvalidatedCache<String, Optional<GCSystem>> systemGridConfigurationCache = TimeInvalidatedCache
      .newBuilder()
      .name("System_GCC")
      .expireAfterDuration(Duration.ofMinutes(5))
      .build(GridConfigurationCache::initializeSystemConfig);
  private static TimeInvalidatedCache<String, Optional<GCTab>> tabGridConfigurationCache = TimeInvalidatedCache
      .newBuilder()
      .name("Tab_GCC")
      .expireAfterDuration(Duration.ofMinutes(5))
      .build(GridConfigurationCache::initializeTabConfig);

  /**
   * @return the singleton instance of OrganizationNodeCache
   */
  public static GridConfigurationCache getInstance() {
    if (instance == null) {
      instance = OBProvider.getInstance().get(GridConfigurationCache.class);
    }
    return instance;
  }

  private static Optional<GCSystem> initializeSystemConfig(String key) {
    return StandardWindowComponent.getSystemGridConfig();
  }

  private static Optional<GCTab> initializeTabConfig(String tabId) {
    OBQuery<GCTab> qGCTab = OBDal.getInstance()
        .createQuery(GCTab.class, "as g where g.tab.id = :tabId");
    qGCTab.setNamedParameter("tabId", tabId);
    return qGCTab.stream()
        .sorted( //
            comparing(GCTab::getSeqno) //
                .thenComparing(GCTab::getId)) //
        .findFirst();
  }

  /**
   * Get the cached system and Tab configuration and compose them into a single JSON Object
   */
  public JSONObject getGridConfigurationForTab(String tabId) {
    try {
      OBContext.setAdminMode(true);
      Optional<GCSystem> sysConf = systemGridConfigurationCache.get("system");
      Optional<GCTab> tabConf = tabId != null ? tabGridConfigurationCache.get(tabId)
          : Optional.empty();
      return OBViewUtil.getGridConfigurationSettings(sysConf, tabConf);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Invalidates the cache for system Grid Configuration because the config has changed
   */
  public void clearSystemGridConfiguration() {
    systemGridConfigurationCache.invalidateAll();
  }

  /**
   * Invalidates the cache for Tab Grid Configuration because the config has changed for this Tab
   */
  public void clearTabGridConfiguration(String tabId) {
    tabGridConfigurationCache.invalidate(tabId);
  }
}
