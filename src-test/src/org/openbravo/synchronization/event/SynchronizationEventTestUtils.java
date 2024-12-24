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
package org.openbravo.synchronization.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;

import org.mockito.verification.VerificationMode;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.test.base.MockableBaseTest;

/**
 * Test utilities for synchronization events
 */
public class SynchronizationEventTestUtils {

  private SynchronizationEventTestUtils() {
  }

  /**
   * Verifies whether a single record event is triggered after executing a method
   *
   * @param event
   *          The unique identifier of the synchronization event
   * @param bob
   *          The record triggered by the event
   * @param eventTrigger
   *          a consumer that accepts the given record and executes the method that is supposed to
   *          trigger the event
   */
  public static <T extends BaseOBObject> void verifyEventIsTriggered(String event, T bob,
      Consumer<T> eventTrigger) {
    verifyEventTriggering(event, bob, eventTrigger, times(1));
  }

  /**
   * Verifies that a single record event is not triggered after executing an action
   *
   * @param event
   *          The unique identifier of the synchronization event
   * @param bob
   *          The record triggered by the event
   * @param eventTrigger
   *          a consumer that accepts the given record and executes the method that is supposed not
   *          to trigger the event
   */
  public static <T extends BaseOBObject> void verifyEventIsNotTriggered(String event, T bob,
      Consumer<T> eventTrigger) {
    verifyEventTriggering(event, bob, eventTrigger, never());
  }

  private static <T extends BaseOBObject> void verifyEventTriggering(String event, T bob,
      Consumer<T> eventTrigger, VerificationMode mode) {
    MockableBaseTest.mockStatic(SynchronizationEvent.class, synchronizationEventMock -> {
      SynchronizationEvent instanceMock = mock(SynchronizationEvent.class);
      synchronizationEventMock.when(SynchronizationEvent::getInstance).thenReturn(instanceMock);
      eventTrigger.accept(bob);
      verify(instanceMock, mode).triggerEvent(event, (String) bob.getId());
    });
  }
}
