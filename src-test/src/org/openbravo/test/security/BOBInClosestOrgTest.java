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
package org.openbravo.test.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.openbravo.test.base.TestConstants.Orgs.ESP;
import static org.openbravo.test.base.TestConstants.Orgs.ESP_NORTE;
import static org.openbravo.test.base.TestConstants.Orgs.ESP_SUR;
import static org.openbravo.test.base.TestConstants.Orgs.FB_GROUP;
import static org.openbravo.test.base.TestConstants.Orgs.MAIN;
import static org.openbravo.test.base.TestConstants.Orgs.US;
import static org.openbravo.test.base.TestConstants.Orgs.US_EST;
import static org.openbravo.test.base.TestConstants.Orgs.US_WEST;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test cases to cover the usage of the
 * {@link OrganizationStructureProvider#getBOBInClosestOrg(java.util.Collection, String)} method
 */
@RunWith(Parameterized.class)
public class BOBInClosestOrgTest extends OBBaseTest {

  private Map<String, String> bobs;
  private String orgId;
  private String closestBOBId;

  public BOBInClosestOrgTest(Map<String, String> bobs, String orgId, String closestBOBId) {
    this.bobs = bobs;
    this.orgId = orgId;
    this.closestBOBId = closestBOBId;
  }

  @Parameters(name = "bobs={0}, orgId={1}, closestBOBId={2}")
  public static Collection<Object[]> parameters() {
    Object[][] params = {
        //@formatter:off
        { Collections.emptyMap(), ESP, null },
        { Map.of("A", ESP), null, null },
        { Map.of("A", ESP), ESP, "A" },
        { Map.of("A", MAIN, "B", ESP, "C", ESP), ESP, "B" },
        { Map.of("A", MAIN, "B", MAIN, "C", ESP), ESP, "C" },
        { Map.of("A", MAIN, "B", FB_GROUP, "C", ESP), ESP, "C" },
        { Map.of("A", MAIN, "B", FB_GROUP, "C", ESP_NORTE), ESP, "B" },
        { Map.of("A", ESP_NORTE, "B", ESP_SUR), ESP, "A" },
        { Map.of("A", ESP_NORTE, "B", ESP_SUR, "C", US_WEST), ESP, "A" },
        { Map.of("A", ESP_NORTE, "B", ESP_SUR, "C", US_WEST), US, "C" },
        { Map.of("A", US_EST, "B", ESP_SUR), ESP, "B" },
        { Map.of("A", US_EST, "B", US_WEST), ESP, null }
        //@formatter:on
    };
    return Arrays.asList(params);
  }

  @Test
  public void getBOBInClosestOrg() {
    BaseOBObject bob = OBContext.getOBContext()
        .getOrganizationStructureProvider()
        .getBOBInClosestOrg(bobs.entrySet()
            .stream()
            .map(entry -> getBOB(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList()), orgId);

    String bobId = bob != null ? (String) bob.getId() : null;
    assertThat("Is the expected closest BOB", bobId, equalTo(closestBOBId));
  }

  private BaseOBObject getBOB(String bobId, String bobOrgId) {
    BaseOBObject bob = OBProvider.getInstance().get(UOM.class);
    bob.setId(bobId);
    bob.set("organization", OBDal.getInstance().getProxy(Organization.class, bobOrgId));
    return bob;
  }
}
