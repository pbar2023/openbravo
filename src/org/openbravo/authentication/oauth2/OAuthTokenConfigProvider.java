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

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.cache.TimeInvalidatedCache;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.authentication.AuthenticationProvider;
import org.openbravo.model.authentication.OAuth2TokenAuthenticationProvider;

/**
 * Provides the configuration properties defined to make authentication with OAuth 2.0 with tokens
 */
@ApplicationScoped
public class OAuthTokenConfigProvider {
  private static final String CONFIG_ID = "OAuth2TokenConfig";

  private TimeInvalidatedCache<String, Configuration> oauth2TokenConfig = TimeInvalidatedCache
      .newBuilder()
      .name("OAuth 2.0 Token Authentication Provider")
      .expireAfterDuration(Duration.ofMinutes(10))
      .build(this::getConfiguration);

  private Configuration getConfiguration(String configId) {
    try {
      OBContext.setAdminMode(true);
      AuthenticationProvider authProvider = OBDal.getInstance()
          .createQuery(AuthenticationProvider.class,
              "where type='OAUTH2TOKEN' and application.value = 'API'")
          .setFilterOnReadableClients(false)
          .setFilterOnReadableOrganization(false)
          .uniqueResult();
      return authProvider != null
          ? authProvider.getOAuth2TokenAuthenticationProviderList()
              .stream()
              .filter(l -> l.isActive())
              .findFirst()
              .map(Configuration::new)
              .orElse(null)
          : null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Return the configuration defined for oauth 2.0 with tokens
   * 
   * @return the Configuration instance
   * 
   */
  public Configuration getConfiguration() {
    return oauth2TokenConfig.get(CONFIG_ID);
  }

  /**
   * Removes the configuration record saved in cache
   */
  public void invalidateCache() {
    oauth2TokenConfig.invalidate(CONFIG_ID);
  }

  static class Configuration {
    private final String jwksURL;
    private final String tokenProperty;

    private Configuration(OAuth2TokenAuthenticationProvider config) {
      jwksURL = config.getJwksUrl();
      tokenProperty = config.getTokenProperty();
    }

    String getJwksURL() {
      return jwksURL;
    }

    String getTokenProperty() {
      return tokenProperty;
    }
  }
}
