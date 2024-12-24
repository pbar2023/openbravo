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
 * All portions are Copyright (C) 2022 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.externalsystem.http;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.service.externalsystem.ExternalSystemConfigurationError;
import org.openbravo.service.externalsystem.HttpExternalSystemData;
import org.openbravo.utils.FormatUtilities;

/**
 * Used to authenticate an HTTP request with the standard Basic HTTP authorization method
 */
@HttpAuthorizationMethod("BASIC")
public class BasicHttpAuthorizationProvider extends Authenticator
    implements HttpAuthorizationProvider {
  private static final Logger log = LogManager.getLogger();

  private String userName;
  private String password;

  @Override
  public void init(HttpExternalSystemData configuration) {
    userName = configuration.getUsername();
    try {
      password = FormatUtilities.encryptDecrypt(configuration.getPassword(), false);
    } catch (ServletException ex) {
      log.error("Error decrypting password of HTTP configuration {}", configuration.getId());
      throw new ExternalSystemConfigurationError(
          "Error decrypting password of HTTP configuration " + configuration.getId());
    }
  }

  @Override
  protected PasswordAuthentication getPasswordAuthentication() {
    return new PasswordAuthentication(userName, password.toCharArray());
  }
}
