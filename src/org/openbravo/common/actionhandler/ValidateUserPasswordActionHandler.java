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
package org.openbravo.common.actionhandler;

import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.authentication.hashing.PasswordHash;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.application.process.ResponseActionsBuilder.MessageType;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.service.db.DbUtility;
import org.openbravo.service.password.PasswordStrengthChecker;

/**
 * Action handler that will given a password, will check if it complies with the password policies
 * defined in PasswordStrengthChecker. If it complies, the password is assigned to the user passed
 * as parameter. If not an error message will be displayed
 */
public class ValidateUserPasswordActionHandler extends BaseProcessActionHandler {

  private static final Logger log = Logger.getLogger(ValidateUserPasswordActionHandler.class);

  @Inject
  private PasswordStrengthChecker checker;

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    try {
      JSONObject request = new JSONObject(content);
      JSONObject params = request.getJSONObject("_params");

      String userId = request.getString("AD_User_ID");
      User user = OBDal.getInstance().get(User.class, userId);
      Client currentClient = OBContext.getOBContext().getCurrentClient();
      if (!currentClient.getId().equals(user.getClient().getId())) {
        throw new OBException(OBMessageUtils.messageBD("AD_UserCannotBeUpdated"));
      }

      String password = params.getString("new_pasword");
      if (!checker.isStrongPassword(password)) {
        throw new OBException(OBMessageUtils.messageBD("CPPasswordNotStrongEnough"));
      }

      user.setPassword(PasswordHash.generateHash(password));

      return getResponseBuilder()
          .showMsgInProcessView(MessageType.SUCCESS, OBMessageUtils.messageBD("OBUIAPP_Success"),
              OBMessageUtils.messageBD("AD_PasswordUpdated"))
          .build();
    } catch (JSONException e) {
      log.error(e.getMessage(), e);
      Throwable ex = DbUtility.getUnderlyingSQLException(e);
      String msgText = OBMessageUtils.translateError(ex.getMessage()).getMessage();
      return getResponseBuilder()
          .showMsgInProcessView(MessageType.ERROR, OBMessageUtils.messageBD("Error"), msgText)
          .build();
    }
  }

}
