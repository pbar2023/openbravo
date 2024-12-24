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
package org.openbravo.materialmgmt.refinventory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collections;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryStatusProcessor.ReferencedInventoryStatus;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;
import org.openbravo.test.referencedinventory.ReferencedInventoryTestUtils;

/**
 * Test cases to cover the execution of the {@link ChangeReferencedInventoryStatusHandler} process
 * action handler.
 */
public class ChangeReferencedInventoryStatusHandlerTest extends WeldBaseTest {

  private ReferencedInventory handlingUnit;

  @Before
  public void prepareHandlingUnits() {
    handlingUnit = ReferencedInventoryTestUtils.createReferencedInventory(null);
  }

  @After
  public void cleanUp() {
    rollback();
  }

  @Test
  public void changeHandlingUnitStatus() {
    assertThat("Handling unit status is initally open", handlingUnit.getStatus(),
        equalTo(ReferencedInventoryStatus.OPEN.name()));

    JSONObject requestData = new JSONObject(Map.of("M_RefInventory_ID", handlingUnit.getId(),
        "_params", new JSONObject(Map.of("Status", "CLOSED"))));
    WeldUtils.getInstanceFromStaticBeanManager(ChangeReferencedInventoryStatusHandler.class)
        .doExecute(Collections.emptyMap(), requestData.toString());

    assertThat("Handling unit status is changed", handlingUnit.getStatus(),
        equalTo(ReferencedInventoryStatus.CLOSED.name()));
  }
}
