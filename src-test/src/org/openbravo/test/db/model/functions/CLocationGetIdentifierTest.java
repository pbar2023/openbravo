/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.test.db.model.functions;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.test.base.OBBaseTest;

/**
 * C_Location_GetIdentifier function tests
 * 
 */
@RunWith(Parameterized.class)
public class CLocationGetIdentifierTest extends OBBaseTest {

  private String address1;
  private String address2;
  private String postal;
  private String city;
  private String regionId;
  private String countryId;
  private String result;

  public CLocationGetIdentifierTest(String address1, String address2, String postal, String city,
      String regionId, String countryId, String result) {
    this.address1 = address1;
    this.address2 = address2;
    this.postal = postal;
    this.city = city;
    this.regionId = regionId;
    this.countryId = countryId;
    this.result = result;
  }

  /** parameterized possible combinations for taxes computation */
  @Parameters(name = "{0}-{1}-{2}-{3}-{4}-{5}")
  public static Collection<String[]> params() {
    final String[][] params = new String[][] { //
        { null, null, null, null, null, null, " -  -  -  -  - " }, //
        { "", "", "", "", "", "", " -  -  -  -  - " }, //
        { "1", "", "3", "", "5", "", "1 -  - 3 -  - 5 - " }, //
        { "", "2", "", "4", "", "6", " - 2 -  - 4 -  - 6" }, //
        { "1", "2", "3", "4", "5", "6", "1 - 2 - 3 - 4 - 5 - 6" }, //
    };
    return Arrays.asList(params);
  }

  @Test
  public void test() {
    final List<Object> parameters = Arrays.asList(address1, address2, postal, city, regionId,
        countryId);
    final String identifier = ((String) CallStoredProcedure.getInstance()
        .call("c_location_getidentifier", parameters, null));
    assertEquals(result, identifier);
  }

}
