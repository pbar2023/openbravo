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

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.openbravo.authentication.LoginStateHandler;
import org.openbravo.base.HttpBaseUtils;
import org.openbravo.client.kernel.BaseTemplateComponent;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.Template;

/**
 * Generates the code for the buttons that are placed in the login page to log in with an external
 * authorization provider using an OAuth 2.0 based protocol.
 * 
 * @see OAuth2SignInProvider
 */
@RequestScoped
public class OAuth2LoginButtonGenerator extends BaseTemplateComponent {

  @Inject
  private OAuth2SignInProvider oauth2SignInProvider;

  @Inject
  private LoginStateHandler oauth2StateHandler;

  @Override
  protected Template getComponentTemplate() {
    return oauth2SignInProvider.getTemplate();
  }

  public List<OAuth2LoginButton> getButtons() {
    return oauth2SignInProvider.getAuthenticationProviderConfigs()
        .stream()
        .map(OAuth2LoginButton::new)
        .collect(Collectors.toList());
  }

  public class OAuth2LoginButton {
    private OAuth2Config config;

    public OAuth2LoginButton(OAuth2Config config) {
      this.config = config;
    }

    public String getId() {
      return config.getID();
    }

    public String getName() {
      return config.getName();
    }

    public String getClientID() {
      return config.getClientID();
    }

    public String getAuthorizationURL() {
      return config.getAuthorizationURL();
    }

    public String getRedirectURL() {
      return HttpBaseUtils.getLocalAddress(RequestContext.get().getRequest())
          + OpenIDAuthenticationManager.DEFAULT_REDIRECT_PATH;
    }

    public String getState() {
      return oauth2StateHandler.addNewConfiguration(config.getID());
    }

    public String getScope() {
      return config.getScope();
    }

    public String getIcon() {
      return config.getIconData() != null ? "data:" + config.getIconMimeType() + ";base64,"
          + Base64.getEncoder().encodeToString(config.getIconData()) : null;
    }
  }
}
