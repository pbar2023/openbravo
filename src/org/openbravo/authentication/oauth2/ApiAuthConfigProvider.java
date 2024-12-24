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
package org.openbravo.authentication.oauth2;

import java.time.Duration;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.cache.TimeInvalidatedCache;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.authentication.AuthenticationProvider;

/**
 * Provides the configuration properties defined to make openbravo api
 */
@ApplicationScoped
public class ApiAuthConfigProvider {
  private static final String CONFIG_ID = "ApiAuthConfig";
  private static final String DEFAULT = "DEFAULT";

  private TimeInvalidatedCache<String, String> oauthTokenConfig = TimeInvalidatedCache.newBuilder()
      .name("API Authentication Configuration")
      .expireAfterDuration(Duration.ofMinutes(10))
      .build(this::getConfiguration);

  private String getConfiguration(String configId) {
    try {
      OBContext.setAdminMode(true);
      AuthenticationProvider authProvider = OBDal.getInstance()
          .createQuery(AuthenticationProvider.class, "where application.value = 'API'")
          .setFilterOnReadableClients(false)
          .setFilterOnReadableOrganization(false)
          .uniqueResult();
      return authProvider != null ? authProvider.getType() : DEFAULT;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Return the authentication type defined for the API
   * 
   * @return an Optional describing the authentication type defined for the API. An empty Optional
   *         is defined in case there is no authentication configuration defined for the API.
   * 
   */
  public Optional<String> getApiAuthType() {
    String authType = oauthTokenConfig.get(CONFIG_ID);
    return DEFAULT.equals(authType) ? Optional.empty() : Optional.of(authType);
  }

  /**
   * @return true if there is a configuration record for the API
   */
  public boolean existsApiAuthConfiguration() {
    String authType = oauthTokenConfig.get(CONFIG_ID);
    return !DEFAULT.equals(authType);
  }

  /**
   * Removes the configuration record saved in cache
   */
  public void invalidateCache() {
    oauthTokenConfig.invalidate(CONFIG_ID);
  }
}
