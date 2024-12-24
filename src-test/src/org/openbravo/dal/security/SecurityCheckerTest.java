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

package org.openbravo.dal.security;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.OrganizationEnabled;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.test.base.Issue;
import org.openbravo.test.base.TestConstants.Clients;
import org.openbravo.test.base.TestConstants.Orgs;
import org.openbravo.test.base.TestConstants.Roles;

/**
 * Test cases for the {@link SecurityChecker}
 */
public class SecurityCheckerTest extends WeldBaseTest {

  @Before
  public void init() {
    OBContext.setOBContext(TEST_USER_ID, Roles.ESP_ADMIN, TEST_CLIENT_ID, Orgs.ESP);
  }

  @Test
  public void hasReadAccess() {
    OrganizationEnabled readableObject = getOrganizationEnabledObject(Orgs.ESP_NORTE);
    SecurityChecker.getInstance().checkReadableAccess(readableObject);
  }

  @Test
  public void hasNotReadAccess() {
    OrganizationEnabled nonReadableObject = getOrganizationEnabledObject(Orgs.US_EST);
    assertThrows(OBSecurityException.class,
        () -> SecurityChecker.getInstance().checkReadableAccess(nonReadableObject));
  }

  @Test
  @Issue("55124")
  public void hasSpecialReadAccess() {
    OrganizationEnabled specialReadableObject = getOrganizationEnabledObject(Orgs.US_EST);
    SecurityChecker securityChecker = spy(SecurityChecker.getInstance());
    when(securityChecker.hasSpecialReadAccess(specialReadableObject)).thenReturn(true);
    securityChecker.checkReadableAccess(specialReadableObject);
  }

  private OrganizationEnabled getOrganizationEnabledObject(String orgId) {
    Order object = OBProvider.getInstance().get(Order.class);
    object.setClient(OBDal.getInstance().getProxy(Client.class, Clients.FB_GRP));
    object.setOrganization(OBDal.getInstance().getProxy(Organization.class, orgId));
    return object;
  }
}
