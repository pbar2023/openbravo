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

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

/**
 * An {@link EventTrigger} for testing purposes
 */
@ApplicationScoped
class TestEventTrigger implements EventTrigger {
  private int triggeredEvents = 0;
  private int triggeredMultipleRecordEvents = 0;

  @Override
  public void triggerEvent(String event, String recordId) {
    triggeredEvents += 1;
  }

  @Override
  public void triggerEvent(String event, Map<String, Object> params) {
    triggeredMultipleRecordEvents += 1;
  }

  @Override
  public boolean handlesEvent(String event) {
    return "EVENT_A".equals(event);
  }

  void reset() {
    triggeredEvents = 0;
    triggeredMultipleRecordEvents = 0;
  }

  int getTriggeredEvents() {
    return triggeredEvents;
  }

  int getTriggeredMultiRecordEvents() {
    return triggeredMultipleRecordEvents;
  }
}
