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
 * All portions are Copyright (C) 2009-2024 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.navigationbarcomponents;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.authentication.hashing.PasswordHash;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.ApplicationConstants;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.system.Language;
import org.openbravo.portal.PortalAccessible;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.password.PasswordStrengthChecker;

/**
 * Action handler used to save the default user information of the 'Profile' widget and the password
 * change using the 'Change Password' widget.
 * 
 * @author mtaal
 */
@ApplicationScoped
public class UserInfoWidgetActionHandler extends BaseActionHandler implements PortalAccessible {

  @Inject
  private PasswordStrengthChecker passwordStrengthChecker;

  @Inject
  @Any
  private Instance<UserInfoWidgetActionHandlerAdditionalCheck> additionalChecks;

  /*
   * (non-Javadoc)
   * 
   * @see org.openbravo.client.kernel.BaseActionHandler#execute(Map, String)
   */
  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    final String command = (String) parameters.get(ApplicationConstants.COMMAND);
    OBContext.setAdminMode();
    try {
      if (command == null) {
        throw new IllegalArgumentException("command parameter not specified");
      }
      if (command.equals(ApplicationConstants.SAVE_COMMAND)) {
        return executeSaveCommand(parameters, content);
      } else if (command.equals(ApplicationConstants.CHANGE_PWD_COMMAND)) {
        return executeChangePasswordCommand(parameters, content);
      }
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
    throw new IllegalArgumentException("Illegal command value: " + command);
  }

  protected JSONObject executeChangePasswordCommand(Map<String, Object> parameters, String content)
      throws Exception {
    // do some checking
    final User user = OBDal.getInstance()
        .get(User.class, OBContext.getOBContext().getUser().getId());
    final JSONObject json = new JSONObject(content);
    final String currentPwd = json.getString("currentPwd");
    final String newPwd = json.getString("newPwd");
    final String confirmPwd = json.getString("confirmPwd");

    if (!PasswordHash.matches(currentPwd, user.getPassword())) {
      return createErrorResponse("currentPwd", "UINAVBA_CurrentPwdIncorrect");
    }
    if (currentPwd.equals(newPwd)) {
      return createErrorResponse("newPwd", "CPDifferentPassword");
    }
    if (newPwd == null || newPwd.trim().length() == 0) {
      return createErrorResponse("currentPwd", "UINAVBA_IncorrectPwd");
    }
    if (!newPwd.equals(confirmPwd)) {
      return createErrorResponse("currentPwd", "UINAVBA_UnequalPwd");
    }
    if (!passwordStrengthChecker.isStrongPassword(newPwd)) {
      return createErrorResponse("newPwd", "CPPasswordNotStrongEnough");
    }
    user.setPassword(PasswordHash.generateHash(newPwd));
    OBDal.getInstance().flush();
    return ApplicationConstants.ACTION_RESULT_SUCCESS;
  }

  private JSONObject createErrorResponse(String fieldName, String messageKey) throws JSONException {
    final JSONObject response = new JSONObject();
    response.put("result", "error");
    final JSONArray fields = new JSONArray();
    final JSONObject field = new JSONObject();
    field.put("field", fieldName);
    field.put("messageCode", messageKey);
    fields.put(field);
    response.put("fields", fields);
    return response;
  }

  protected JSONObject executeSaveCommand(Map<String, Object> parameters, String content)
      throws Exception {
    final HttpServletRequest request = (HttpServletRequest) parameters
        .get(KernelConstants.HTTP_REQUEST);
    final JSONObject json = new JSONObject(content);

    String orgId = getStringValue(json, "organization");
    if (orgId == null) {
      orgId = OBContext.getOBContext().getCurrentOrganization().getId();
    }

    String roleId = getStringValue(json, "role");
    if (roleId == null) {
      roleId = OBContext.getOBContext().getRole().getId();
    }

    final Role role = OBDal.getInstance().get(Role.class, roleId);

    // In case the client is deleted using 'Delete Client' and immediately accessed from the profile
    // widget throw error. Refer https://issues.openbravo.com/view.php?id=24092
    if (role == null || role.getClient() == null) {
      String errorMessage = Utility.messageBD(new DalConnectionProvider(false), "ClientDeleted",
          OBContext.getOBContext().getLanguage().getLanguage());
      throw new OBException(errorMessage);
    }

    final String clientId = role.getClient().getId();

    String warehouseId = getStringValue(json, "warehouse");
    if (warehouseId == null && OBContext.getOBContext().getWarehouse() != null) {
      warehouseId = OBContext.getOBContext().getWarehouse().getId();
    }

    String languageId = getStringValue(json, "language");
    if (languageId == null) {
      // If the default language the user has is not a system language, then another language will
      // be automatically selected
      languageId = pickLanguage();
    }
    final boolean isDefault;
    String defaultRoleProperty = null;
    boolean setOnlyRole = false;
    if (json.has("default")) {
      isDefault = json.getBoolean("default");
      if (json.has("defaultRoleProperty")) {
        setOnlyRole = true;
        defaultRoleProperty = json.getString("defaultRoleProperty");
      }
    } else {
      isDefault = false;
    }

    if (StringUtils.isEmpty(defaultRoleProperty)) {
      defaultRoleProperty = User.PROPERTY_DEFAULTROLE;
    }

    String application = getStringValue(json, "appSearchKey");
    UserInfoWidgetActionHandlerAdditionalCheck checker = getRoleAdditionalChecker(application);
    if (checker != null) {
      try {
        checker.checkRole(role, application);
      } catch (Exception e) {
        return createErrorResponse(e.getMessage());
      }
    }

    new UserSessionSetter().resetSession(request, isDefault,
        OBContext.getOBContext().getUser().getId(), roleId, clientId, orgId, languageId,
        warehouseId, defaultRoleProperty, setOnlyRole);

    return ApplicationConstants.ACTION_RESULT_SUCCESS;
  }

  private JSONObject createErrorResponse(String messageKey) throws JSONException {
    final JSONObject response = new JSONObject();
    response.put("result", "error");
    response.put("message", messageKey);

    return response;
  }

  private String pickLanguage() {
    final OBQuery<Language> languages = OBDal.getInstance()
        .createQuery(Language.class, "(" + Language.PROPERTY_SYSTEMLANGUAGE + "=true or "
            + Language.PROPERTY_BASELANGUAGE + "=true)");
    languages.setFilterOnReadableClients(false);
    languages.setFilterOnReadableOrganization(false);
    List<Language> languagesList = languages.list();

    Client client = OBContext.getOBContext().getCurrentClient();
    Language clientLanguage = client.getLanguage();
    if (clientLanguage != null && languagesList.contains(clientLanguage)) {
      return clientLanguage.getId();
    } else {
      return languagesList.get(0).getId();
    }
  }

  private String getStringValue(JSONObject json, String name) throws JSONException {
    if (json.isNull(name)) {
      return null;
    }
    if (!json.has(name)) {
      return null;
    }
    return json.getString(name);
  }

  private UserInfoWidgetActionHandlerAdditionalCheck getRoleAdditionalChecker(String appName) {
    if (appName == null || appName.isEmpty()) {
      return null;
    }

    for (UserInfoWidgetActionHandlerAdditionalCheck checker : additionalChecks) {
      if (checker.getApplicableAppSearchKeyList().contains(appName)) {
        return checker;
      }
    }
    return null;
  }
}
