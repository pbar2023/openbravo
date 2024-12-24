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
package org.openbravo.service.externalsystem.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Spy;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.test.base.MockableBaseTest;

/**
 * Test cases for the {@link OAuth2AccessToken} class
 */
public class OAuth2AccessTokenTest extends MockableBaseTest {

  private static final String VALUE = SequenceIdData.getUUID();
  private static final int EXPIRES_IN = 100;

  @Spy
  private OAuth2AccessToken token = new OAuth2AccessToken(VALUE, EXPIRES_IN);

  @Test
  public void getTokenAuthentication() {
    assertThat("Expected authorization", token.getAuthorization(), equalTo("Bearer " + VALUE));
  }

  @Test
  public void getTokenOriginalExpiresIn() {
    assertThat("Expected expiration time", token.getExpiresIn(), equalTo(EXPIRES_IN));
  }

  @Test
  public void checkTokenExpiration() {
    assertThat("Token is not expired", token.isExpired(), equalTo(false));
    long now = token.now();
    when(token.now()).thenReturn(now + 50);
    assertThat("Token is not expired yet", token.isExpired(), equalTo(false));
    when(token.now()).thenReturn(now + 200);
    assertThat("Token is expired", token.isExpired(), equalTo(true));
  }
}
