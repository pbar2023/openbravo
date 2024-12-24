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
package org.openbravo.authentication;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.Prioritizable;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.authentication.AuthenticationProvider;

/**
 * Provides authentication using an external authentication provider. Classes extending this one
 * must be annotated with the {@link AuthenticationType} annotation so they can be properly selected
 * before starting with the authentication process.
 */
public abstract class ExternalAuthenticationManager extends AuthenticationManager
    implements Prioritizable {
  private static final Logger log = LogManager.getLogger();

  /**
   * Get a ExternalAuthenticationManager instance
   * 
   * @param authMethod
   *          The identifier of the authentication method
   *
   * @return an optional describing the ExternalAuthenticationManager instance for authentication
   *         using the given method or an empty optional if no instance can be retrieved for the
   *         given method
   */
  public static Optional<ExternalAuthenticationManager> newInstance(String authMethod) {
    List<ExternalAuthenticationManager> externalAuthManagers = WeldUtils
        .getInstancesSortedByPriority(ExternalAuthenticationManager.class,
            new AuthenticationTypeSelector(authMethod));
    if (externalAuthManagers.isEmpty()) {
      log.error("Could not find an ExternalAuthenticationManager instance for method {}",
          authMethod);
      return Optional.empty();
    }
    return Optional.of(externalAuthManagers.get(0));
  }

  @Override
  public String doAuthenticate(HttpServletRequest request, HttpServletResponse response) {
    AuthenticatedUser user = doExternalAuthentication(request, response);
    if (user.getUserName() != null) {
      loginName.set(user.getUserName());
      if (request.getParameter("authProvider") != null) {
        RequestContext.get()
            .setSessionAttribute("#AUTH_PROVIDER", request.getParameter("authProvider"));
      }
    }
    return user.getId();
  }

  /**
   * To be implemented with the logic of the external authentication for the login flow.
   *
   * @param request
   *          HTTP request object to handle parameters and session attributes
   * @param response
   *          HTTP response object to handle possible redirects
   * @return the information of the successfully authenticated user
   */
  public abstract AuthenticatedUser doExternalAuthentication(HttpServletRequest request,
      HttpServletResponse response);

  /**
   * Method to perform the external authentication from a given object containing the user
   * credentials. This method is invoked when trying to perform an authentication in flows different
   * from the login flow.
   *
   * @param authProvider
   *          The name of the {@link AuthenticationProvider} used to authenticate
   * @param credential
   *          The credentials needed to perform the external authentication
   *
   * @return an Optional describing the authenticated user or an empty optional if the
   *         authentication fails or cannot be done with the given credentials
   *
   * @throws JSONException
   *           if there is an error retrieving the information from the credential
   * @throws AuthenticationException
   *           if there is an error during the external authentication process
   */
  public Optional<User> authenticate(String authProvider, JSONObject credential)
      throws JSONException {
    throw new AuthenticationException("Not implemented");
  }
}
