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
 * All portions are Copyright (C) 2022 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.synchronization.event;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collections;
import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.base.weld.test.WeldBaseTest;

/**
 * Integration tests for the {@link SynchronizationEvent} API. It uses a testing
 * {@link EventTrigger} to check if the event has been handled.
 * 
 * @see TestEventTrigger
 */
public class EventTriggeringTest extends WeldBaseTest {

  private static final String HANDLED_EVENT = "EVENT_A";
  private static final String UNHANDLED_EVENT = "EVENT_B";
  private static final String RECORD_ID = "1";
  private static final Map<String, Object> PARAMS = Collections.emptyMap();
  private static final int DEFAULT_PRIORITY = 100;

  @After
  public void cleanUp() {
    WeldUtils.getInstanceFromStaticBeanManager(TestEventTrigger.class).reset();
  }

  @Test
  public void triggerHandledEvent() {
    SynchronizationEvent.getInstance().triggerEvent(HANDLED_EVENT, RECORD_ID);

    assertThat(
        WeldUtils.getInstanceFromStaticBeanManager(TestEventTrigger.class).getTriggeredEvents(),
        equalTo(1));
    assertThat(WeldUtils.getInstanceFromStaticBeanManager(TestEventTrigger.class)
        .getTriggeredMultiRecordEvents(), equalTo(0));
  }

  @Test
  public void triggerUnhandledEvent() {
    SynchronizationEvent.getInstance().triggerEvent(UNHANDLED_EVENT, RECORD_ID);

    assertThat(
        WeldUtils.getInstanceFromStaticBeanManager(TestEventTrigger.class).getTriggeredEvents(),
        equalTo(0));
    assertThat(WeldUtils.getInstanceFromStaticBeanManager(TestEventTrigger.class)
        .getTriggeredMultiRecordEvents(), equalTo(0));
  }

  @Test
  public void triggerHandledMultiRecordEvent() {
    SynchronizationEvent.getInstance().triggerEvent(HANDLED_EVENT, PARAMS);

    assertThat(
        WeldUtils.getInstanceFromStaticBeanManager(TestEventTrigger.class).getTriggeredEvents(),
        equalTo(0));
    assertThat(WeldUtils.getInstanceFromStaticBeanManager(TestEventTrigger.class)
        .getTriggeredMultiRecordEvents(), equalTo(1));
  }

  @Test
  public void hasDefaultPriority() {
    assertThat(WeldUtils.getInstanceFromStaticBeanManager(TestEventTrigger.class).getPriority(),
        equalTo(DEFAULT_PRIORITY));
  }
}
