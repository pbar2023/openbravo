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
import static org.junit.Assert.assertThrows;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryStatusProcessor.ReferencedInventoryStatus;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;
import org.openbravo.synchronization.event.SynchronizationEventTestUtils;
import org.openbravo.test.referencedinventory.ReferencedInventoryTestUtils;

/**
 * Test cases to cover the handling unit status changing using the
 * {@link ReferencedInventoryStatusProcessor} class
 */
public class ReferencedInventoryStatusProcessorTest extends WeldBaseTest {

  private ReferencedInventory container;
  private ReferencedInventory pallet;
  private ReferencedInventory box;

  @Inject
  private ReferencedInventoryStatusProcessor statusProcessor;

  @Before
  public void prepareHandlingUnits() {
    container = ReferencedInventoryTestUtils.createReferencedInventory(null);
    pallet = ReferencedInventoryTestUtils.createReferencedInventory(container);
    box = ReferencedInventoryTestUtils.createReferencedInventory(pallet);
    OBDal.getInstance().flush();
  }

  @After
  public void cleanUp() {
    rollback();
  }

  @Test
  public void handlingUnitsAreOpenedByDefault() {
    assertThat(container.getStatus(), equalTo(ReferencedInventoryStatus.OPEN.name()));
    assertThat(pallet.getStatus(), equalTo(ReferencedInventoryStatus.OPEN.name()));
    assertThat(box.getStatus(), equalTo(ReferencedInventoryStatus.OPEN.name()));
  }

  @Test
  public void changeStatusInCascade() {
    ReferencedInventoryStatus newStatus = ReferencedInventoryStatus.CLOSED;
    statusProcessor.changeStatus(container, newStatus);
    assertThat(container.getStatus(), equalTo(newStatus.name()));
    assertThat(pallet.getStatus(), equalTo(newStatus.name()));
    assertThat(box.getStatus(), equalTo(newStatus.name()));

    newStatus = ReferencedInventoryStatus.OPEN;
    statusProcessor.changeStatus(container, newStatus);
    assertThat(container.getStatus(), equalTo(newStatus.name()));
    assertThat(pallet.getStatus(), equalTo(newStatus.name()));
    assertThat(box.getStatus(), equalTo(newStatus.name()));

    newStatus = ReferencedInventoryStatus.DESTROYED;
    statusProcessor.changeStatus(container, newStatus);
    assertThat(container.getStatus(), equalTo(newStatus.name()));
    assertThat(pallet.getStatus(), equalTo(newStatus.name()));
    assertThat(box.getStatus(), equalTo(newStatus.name()));
  }

  @Test
  public void changeIntermediateHandlingUnitStatus() {
    statusProcessor.changeStatus(pallet, ReferencedInventoryStatus.CLOSED);
    assertThat(container.getStatus(), equalTo(ReferencedInventoryStatus.OPEN.name()));
    assertThat(pallet.getStatus(), equalTo(ReferencedInventoryStatus.CLOSED.name()));
    assertThat(box.getStatus(), equalTo(ReferencedInventoryStatus.CLOSED.name()));
  }

  @Test
  public void changeSingleHandlingUnitStatus() {
    statusProcessor.changeStatus(box, ReferencedInventoryStatus.CLOSED);
    assertThat(container.getStatus(), equalTo(ReferencedInventoryStatus.OPEN.name()));
    assertThat(pallet.getStatus(), equalTo(ReferencedInventoryStatus.OPEN.name()));
    assertThat(box.getStatus(), equalTo(ReferencedInventoryStatus.CLOSED.name()));
  }

  @Test
  public void changeStatusAtDifferentLevels() {
    statusProcessor.changeStatus(container, ReferencedInventoryStatus.CLOSED);
    statusProcessor.changeStatus(container, ReferencedInventoryStatus.OPEN);
    statusProcessor.changeStatus(box, ReferencedInventoryStatus.DESTROYED);
    statusProcessor.changeStatus(pallet, ReferencedInventoryStatus.CLOSED);

    assertThat(container.getStatus(), equalTo(ReferencedInventoryStatus.OPEN.name()));
    assertThat(pallet.getStatus(), equalTo(ReferencedInventoryStatus.CLOSED.name()));
    assertThat(box.getStatus(), equalTo(ReferencedInventoryStatus.DESTROYED.name()));
  }

  @Test
  public void cannotChangeStatusOfADestroyedHandlingUnit() {
    statusProcessor.changeStatus(box, ReferencedInventoryStatus.DESTROYED);
    OBException exception = assertThrows(OBException.class,
        () -> statusProcessor.changeStatus(box, ReferencedInventoryStatus.OPEN));
    assertThat(exception.getMessage(),
        equalTo("Cannot change the status of a destroyed handling unit"));
  }

  @Test
  public void cannotOpenAHandlingUnitWithParentClosed() {
    statusProcessor.changeStatus(pallet, ReferencedInventoryStatus.CLOSED);

    OBException exception = assertThrows(OBException.class,
        () -> statusProcessor.changeStatus(box, ReferencedInventoryStatus.OPEN));
    assertThat(exception.getMessage(),
        equalTo("Cannot change the status of the handling unit " + box.getSearchKey()
            + " because its parent handling unit " + pallet.getSearchKey() + " is closed"));
  }

  @Test
  public void cannotDestroyAHandlingUnitWithParentClosed() {
    statusProcessor.changeStatus(pallet, ReferencedInventoryStatus.CLOSED);

    OBException exception = assertThrows(OBException.class,
        () -> statusProcessor.changeStatus(box, ReferencedInventoryStatus.DESTROYED));
    assertThat(exception.getMessage(),
        equalTo("Cannot change the status of the handling unit " + box.getSearchKey()
            + " because its parent handling unit " + pallet.getSearchKey() + " is closed"));
  }

  @Test
  public void cannotCloseAHandlingUnitWithOutermostParentClosed() {
    container.setStatus(ReferencedInventoryStatus.CLOSED.name());

    OBException exception = assertThrows(OBException.class,
        () -> statusProcessor.changeStatus(box, ReferencedInventoryStatus.CLOSED));
    assertThat(exception.getMessage(),
        equalTo("Cannot change the status of the handling unit " + box.getSearchKey()
            + " because its parent handling unit " + container.getSearchKey() + " is closed"));
  }

  @Test
  public void cannotDestroyAHandlingUnitWithOutermostParentClosed() {
    container.setStatus(ReferencedInventoryStatus.CLOSED.name());

    OBException exception = assertThrows(OBException.class,
        () -> statusProcessor.changeStatus(box, ReferencedInventoryStatus.DESTROYED));
    assertThat(exception.getMessage(),
        equalTo("Cannot change the status of the handling unit " + box.getSearchKey()
            + " because its parent handling unit " + container.getSearchKey() + " is closed"));
  }

  @Test
  public void cannotOpenAHandlingUnitWithAllParentsClosed() {
    statusProcessor.changeStatus(container, ReferencedInventoryStatus.CLOSED);

    OBException exception = assertThrows(OBException.class,
        () -> statusProcessor.changeStatus(box, ReferencedInventoryStatus.OPEN));
    assertThat(exception.getMessage(),
        equalTo("Cannot change the status of the handling unit " + box.getSearchKey()
            + " because its parent handling unit " + pallet.getSearchKey() + " is closed"));
  }

  @Test
  public void cannotDestroyAHandlingUnitWithAllParentsClosed() {
    statusProcessor.changeStatus(container, ReferencedInventoryStatus.CLOSED);

    OBException exception = assertThrows(OBException.class,
        () -> statusProcessor.changeStatus(box, ReferencedInventoryStatus.DESTROYED));
    assertThat(exception.getMessage(),
        equalTo("Cannot change the status of the handling unit " + box.getSearchKey()
            + " because its parent handling unit " + pallet.getSearchKey() + " is closed"));
  }

  @Test
  public void eventIsTriggeredWhenExpected() {
    changeStatusAndVerifyEventIsTriggered(container, ReferencedInventoryStatus.CLOSED);
    changeStatusAndVerifyEventIsNotTriggered(container, ReferencedInventoryStatus.CLOSED);
    changeStatusAndVerifyEventIsTriggered(container, ReferencedInventoryStatus.OPEN);
    changeStatusAndVerifyEventIsNotTriggered(container, ReferencedInventoryStatus.OPEN);
    changeStatusAndVerifyEventIsNotTriggered(pallet, ReferencedInventoryStatus.OPEN);
    changeStatusAndVerifyEventIsNotTriggered(box, ReferencedInventoryStatus.OPEN);
    changeStatusAndVerifyEventIsTriggered(box, ReferencedInventoryStatus.DESTROYED);
    changeStatusAndVerifyEventIsTriggered(pallet, ReferencedInventoryStatus.CLOSED);
    changeStatusAndVerifyEventIsTriggered(container, ReferencedInventoryStatus.CLOSED);
  }

  private void changeStatusAndVerifyEventIsTriggered(ReferencedInventory handlingUnit,
      ReferencedInventoryStatus status) {
    SynchronizationEventTestUtils.verifyEventIsTriggered("API_HandlingUnitStatusChange",
        handlingUnit, hu -> statusProcessor.changeStatus(hu, status));
  }

  private void changeStatusAndVerifyEventIsNotTriggered(ReferencedInventory handlingUnit,
      ReferencedInventoryStatus status) {
    SynchronizationEventTestUtils.verifyEventIsNotTriggered("API_HandlingUnitStatusChange",
        handlingUnit, hu -> statusProcessor.changeStatus(hu, status));
  }
}
