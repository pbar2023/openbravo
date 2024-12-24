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
package org.openbravo.authentication.oauth2;

import org.openbravo.model.ad.utility.Image;
import org.openbravo.model.authentication.AuthenticationProvider;
import org.openbravo.model.authentication.OAuth2AuthenticationProvider;

/**
 * Allows to keep in cache the information of a login provider configuration based in OAuth 2.0
 * 
 * @see OpenIDSignInProvider
 */
class OAuth2Config {
  private static final String DEFAULT_SCOPE = "openid profile email";

  private String id;
  private String name;
  private String authorizationURL;
  private String clientID;
  private String scope;
  private byte[] imageData;
  private String imageMimeType;

  OAuth2Config(OAuth2AuthenticationProvider provider) {
    AuthenticationProvider authProvider = provider.getAuthProvider();
    Image image = authProvider.getIcon();

    id = provider.getId();
    name = authProvider.getName();
    clientID = provider.getClientID();
    authorizationURL = provider.getAuthorizationURL();
    scope = DEFAULT_SCOPE;
    imageData = image != null ? image.getBindaryData() : null;
    imageMimeType = image != null ? image.getMimetype() : null;
  }

  String getID() {
    return id;
  }

  String getName() {
    return name;
  }

  String getClientID() {
    return clientID;
  }

  String getAuthorizationURL() {
    return authorizationURL;
  }

  String getScope() {
    return scope;
  }

  byte[] getIconData() {
    return imageData;
  }

  String getIconMimeType() {
    return imageMimeType;
  }
}
