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

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;
import org.openbravo.test.base.OBBaseTest;
import org.openbravo.test.referencedinventory.ReferencedInventoryTestUtils;

/**
 * Tests the utilities provided by {@link ReferencedInventoryUtil}
 */
public class ReferencedInventoryUtilTest extends OBBaseTest {

  private ReferencedInventory container;
  private ReferencedInventory pallet;
  private ReferencedInventory box1;
  private ReferencedInventory box2;

  @Before
  public void prepareHandlingUnits() {
    container = ReferencedInventoryTestUtils.createReferencedInventory(null);
    pallet = ReferencedInventoryTestUtils.createReferencedInventory(container);
    box1 = ReferencedInventoryTestUtils.createReferencedInventory(pallet);
    box2 = ReferencedInventoryTestUtils.createReferencedInventory(pallet);
    OBDal.getInstance().flush();
  }

  @After
  public void cleanUp() {
    rollback();
  }

  @Test
  public void getDirectChildReferencedInventories() {
    assertThat(getDirectChildReferencedInventories(container), hasItems(pallet.getSearchKey()));
    assertThat(getDirectChildReferencedInventories(pallet),
        hasItems(box1.getSearchKey(), box2.getSearchKey()));
    assertThat(getDirectChildReferencedInventories(box1), empty());
    assertThat(getDirectChildReferencedInventories(box2), empty());
  }

  private List<String> getDirectChildReferencedInventories(ReferencedInventory handlingUnit) {
    return ReferencedInventoryUtil.getDirectChildReferencedInventories(handlingUnit)
        .map(ReferencedInventory::getSearchKey)
        .collect(Collectors.toList());
  }
}
