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
package org.openbravo.authentication.oauth2;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.authentication.AuthenticationProvider;
import org.openbravo.model.authentication.OAuth2AuthenticationProvider;

/**
 * Used to invalidate both the cache of public keys kept by {@link OpenIDTokenDataProvider} and the
 * cache of configurations kept by {@link OpenIDSignInProvider} when changes regarding an OAuth 2.0
 * authentication provider configuration are detected. Note that in case of working in a clustered
 * environment, this mechanism will only invalidate the cache in the node were the changes occurred.
 * For the rest of the nodes in the cluster it will be necessary to wait for the expiration of the
 * cache entry.
 *
 * @see JWTDataProvider#invalidateCache()
 * @see OpenIDSignInProvider#invalidateCache()
 */
class OAuth2AuthenticationProviderEventHandler extends EntityPersistenceEventObserver {
  private static final Entity[] ENTITIES = {
      ModelProvider.getInstance().getEntity(OAuth2AuthenticationProvider.ENTITY_NAME) };

  @Inject
  private JWTDataProvider jwtDataProvider;

  @Inject
  private OAuth2SignInProvider oauth2SignInProvider;

  @Override
  protected Entity[] getObservedEntities() {
    return ENTITIES;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    OAuth2AuthenticationProvider oAuth2Provider = (OAuth2AuthenticationProvider) event
        .getTargetInstance();
    checkSupportedAuthenticationFlow(oAuth2Provider);
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    OAuth2AuthenticationProvider oAuth2Provider = (OAuth2AuthenticationProvider) event
        .getTargetInstance();
    invalidateOpenIDPublicKeyCache(oAuth2Provider.getCertificateURL());
    invalidateOAuth2ConfigurationCache(oAuth2Provider.getAuthProvider().getType());
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    Property certificateURLProperty = ENTITIES[0]
        .getProperty(OAuth2AuthenticationProvider.PROPERTY_CERTIFICATEURL);
    String certificateURL = (String) event.getPreviousState(certificateURLProperty);
    invalidateOpenIDPublicKeyCache(certificateURL);
    OAuth2AuthenticationProvider oAuth2Provider = (OAuth2AuthenticationProvider) event
        .getTargetInstance();
    invalidateOAuth2ConfigurationCache(oAuth2Provider.getAuthProvider().getType());
    checkSupportedAuthenticationFlow(oAuth2Provider);
  }

  private void invalidateOpenIDPublicKeyCache(String certificateURL) {
    if (certificateURL != null) {
      jwtDataProvider.invalidateCache(certificateURL);
    }
  }

  private void invalidateOAuth2ConfigurationCache(String type) {
    oauth2SignInProvider.invalidateCache(type);
  }

  private void checkSupportedAuthenticationFlow(OAuth2AuthenticationProvider oAuth2Provider) {
    AuthenticationProvider authProvider = oAuth2Provider.getAuthProvider();
    if (OAuth2SignInProvider.BACKOFFICE_APP.equals(authProvider.getApplication().getId())
        && oAuth2Provider.getAuthFlow().equals("PKCE")) {
      throw new OBException(OBMessageUtils.messageBD("BackOfficeAppUnsupportedAuthFlow"));
    }
  }
}
