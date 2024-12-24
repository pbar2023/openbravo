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
package org.openbravo.authentication;

/**
 * Used to provide information about the result of an authentication attempt against an external
 * authentication provider.
 * 
 * @see ExternalAuthenticationManager
 */
public class AuthenticatedUser {

  private String id;
  private String userName;

  public AuthenticatedUser(String id, String userName) {
    this.id = id;
    this.userName = userName;
  }

  /**
   * @return the value of AD_User_ID if the user is successfully authenticated or <b>null</b> if not
   */
  public String getId() {
    return id;
  }

  /**
   * @return the user name of the user if the user is successfully authenticated or <b>null</b> if
   *         not
   */
  public String getUserName() {
    return userName;
  }
}
