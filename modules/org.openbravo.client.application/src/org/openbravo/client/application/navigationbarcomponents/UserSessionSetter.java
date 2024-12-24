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
package org.openbravo.client.application.navigationbarcomponents;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.LoginUtils;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.KernelServlet;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.Session;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * This class was originally a private extension in UserInfoWidgetActionHandler. It should only be
 * used in places where we want to reset the Session because of a change of Role
 */
public class UserSessionSetter extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private static final String TEXT_DIRECTION = "#TextDirection";
  private static final String SESSION_ID = "#AD_Session_ID";
  private static final String AUTHENTICATED_USER = "#Authenticated_user";
  private static final String CURRENT_APPLICATION = "#APPLICATION_SEARCH_KEY";
  // Application Mode is a concept introduced in Core2, but as this is a private static class
  // there is no other way to extend this class to include additional values
  // stored in Session
  private static final String CURRENT_APPLICATION_MODE = "#APPLICATION_MODE";

  /**
   * Reset the current Session and updates its data with the given parameters. This call is only
   * intended for requests where the Role changes
   */
  public void resetSession(HttpServletRequest request, boolean isDefault, String userId,
      String roleId, String clientId, String organizationId, String languageId, String warehouseId,
      String defaultRoleProperty, boolean setOnlyRole, boolean resetWebSession) throws Exception {
    final VariablesSecureApp vars = new VariablesSecureApp(request); // refresh
    final Language language = OBDal.getInstance().get(Language.class, languageId);
    if (language.isRTLLanguage()) {
      vars.setSessionValue(TEXT_DIRECTION, "RTL");
    } else {
      vars.setSessionValue(TEXT_DIRECTION, "LTR");
    }

    if (isDefault) {
      final User user = OBDal.getInstance().get(User.class, userId);
      user.set(defaultRoleProperty, OBDal.getInstance().get(Role.class, roleId));
      user.setDefaultLanguage(OBDal.getInstance().get(Language.class, languageId));
      if (!setOnlyRole) {
        user.setDefaultClient(OBDal.getInstance().get(Client.class, clientId));
        user.setDefaultOrganization(OBDal.getInstance().get(Organization.class, organizationId));
      }

      if (warehouseId != null) {
        user.setDefaultWarehouse(OBDal.getInstance().get(Warehouse.class, warehouseId));
      }
      OBDal.getInstance().save(user);
      OBDal.getInstance().flush();
    }

    if (clientId == null || organizationId == null || roleId == null) {
      throw new IllegalArgumentException("Illegal values for client/org or role " + clientId + "/"
          + organizationId + "/" + roleId);
    }

    HttpSession oldSession = request.getSession(false);

    // Clear session variables maintaining session and user
    String sessionID = vars.getSessionValue(SESSION_ID);
    String currentApp = vars.getSessionValue(CURRENT_APPLICATION, "");
    String currentAppMode = vars.getSessionValue(CURRENT_APPLICATION_MODE, "");
    String sessionUser = (String) request.getSession(true).getAttribute(AUTHENTICATED_USER);
    vars.clearSession(false);

    if (resetWebSession) {
      oldSession.invalidate();

      HttpSession newSession = request.getSession(true);
      vars.refreshSession();

      updateDBSession(sessionID, newSession.getId());
    }

    vars.setSessionValue(SESSION_ID, sessionID);
    request.getSession(true).setAttribute(AUTHENTICATED_USER, sessionUser);
    vars.setSessionValue(CURRENT_APPLICATION, currentApp);
    vars.setSessionValue(CURRENT_APPLICATION_MODE, currentAppMode);

    OBDal.getInstance().flush();
    boolean result = LoginUtils.fillSessionArguments(new DalConnectionProvider(false), vars, userId,
        toSaveStr(language.getLanguage()), (language.isRTLLanguage() ? "Y" : "N"),
        toSaveStr(roleId), toSaveStr(clientId), toSaveStr(organizationId), toSaveStr(warehouseId));
    if (!result) {
      throw new IllegalArgumentException("Error when saving default values");
    }
    readProperties(vars);
    readNumberFormat(vars, KernelServlet.getGlobalParameters().getFormatPath());
  }

  public void resetSession(HttpServletRequest request, boolean isDefault, String userId,
      String roleId, String clientId, String organizationId, String languageId, String warehouseId,
      String defaultRoleProperty, boolean setOnlyRole) throws Exception {
    this.resetSession(request, isDefault, userId, roleId, clientId, organizationId, languageId,
        warehouseId, defaultRoleProperty, setOnlyRole, false);
  }

  private void updateDBSession(String sessionId, String webSessionId) {
    try {
      OBContext.setAdminMode();
      Session session = OBDal.getInstance().get(Session.class, sessionId);
      session.setWebSession(webSessionId);
      session.setSessionActive(true);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log4j.error("Error updating session in DB", e);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private String toSaveStr(String value) {
    if (value == null) {
      return "";
    }
    return value;
  }
}
