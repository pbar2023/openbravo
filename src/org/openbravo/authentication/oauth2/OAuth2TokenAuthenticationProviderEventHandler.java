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

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.model.authentication.OAuth2TokenAuthenticationProvider;

/**
 * Used to invalidate the cache of public keys kept by {@link JWTDataProvider} and configuration
 * save by {@link ApiAuthConfigProvider} and {@link OAuthTokenConfigProvider} when changes regarding
 * an OAuth 2.0 authentication provider with tokens configuration are detected. Note that in case of
 * working in a clustered environment, this mechanism will only invalidate the cache in the node
 * were the changes occurred. For the rest of the nodes in the cluster it will be necessary to wait
 * for the expiration of the cache entry.
 *
 * @see JWTDataProvider#invalidateCache()
 * @see ApiAuthConfigProvider#invalidateCache()
 * @see OAuthTokenConfigProvider#invalidateCache()
 */
class OAuth2TokenAuthenticationProviderEventHandler extends EntityPersistenceEventObserver {
  private static final Entity[] ENTITIES = {
      ModelProvider.getInstance().getEntity(OAuth2TokenAuthenticationProvider.ENTITY_NAME) };

  @Inject
  private JWTDataProvider jwtDataProvider;

  @Inject
  private ApiAuthConfigProvider apiAuthConfigProvider;

  @Inject
  private OAuthTokenConfigProvider oathTokenConfigProvider;

  @Override
  protected Entity[] getObservedEntities() {
    return ENTITIES;
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    OAuth2TokenAuthenticationProvider oAuth2Provider = (OAuth2TokenAuthenticationProvider) event
        .getTargetInstance();
    invalidateCaches(oAuth2Provider.getJwksUrl());
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    Property certificateURLProperty = ENTITIES[0]
        .getProperty(OAuth2TokenAuthenticationProvider.PROPERTY_JWKSURL);
    String certificateURL = (String) event.getPreviousState(certificateURLProperty);
    invalidateCaches(certificateURL);
  }

  private void invalidateCaches(String certificateURL) {
    if (certificateURL != null) {
      jwtDataProvider.invalidateCache(certificateURL);
    }
    apiAuthConfigProvider.invalidateCache();
    oathTokenConfigProvider.invalidateCache();
  }
}
