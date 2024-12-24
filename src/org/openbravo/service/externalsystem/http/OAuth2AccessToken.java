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
package org.openbravo.service.externalsystem.http;

import java.time.Instant;

/**
 * Represents an OAuth 2.0 access token of type Bearer.
 * 
 * @see OAuth2AuthorizationProvider
 */
class OAuth2AccessToken {

  static final String BEARER = "Bearer";

  private final String value;
  private final int expiresIn;
  private final long validUntil;

  /**
   * Builds a new OAuth 2.0 access token that is valid since the moment it is created and until the
   * given expiration time passes.
   *
   * @param value
   *          The value of the token itself
   * @param expiresIn
   *          the number of seconds that this token is valid
   */
  OAuth2AccessToken(String value, int expiresIn) {
    this.value = value;
    this.expiresIn = expiresIn;
    validUntil = now() + expiresIn;
  }

  /**
   * @return the original token expiration time (in seconds) that was received from the
   *         authentication server
   */
  public int getExpiresIn() {
    return expiresIn;
  }

  /**
   * @return true if the token has expired, or false if it is still valid
   */
  boolean isExpired() {
    return validUntil < now();
  }

  /** Internal API: it is not a private method because of testing purposes */
  long now() {
    return Instant.now().getEpochSecond();
  }

  /**
   * @return a String with the value of the Authorization HTTP request header
   */
  String getAuthorization() {
    return BEARER + " " + value;
  }
}
