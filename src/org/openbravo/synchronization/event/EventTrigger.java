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

/**
 * Provides the ability of triggering synchronization events
 */
public interface EventTrigger {

  /**
   * Triggers a single record synchronization event
   * 
   * @param event
   *          The unique identifier of the synchronization event
   * @param recordId
   *          the ID that identifies the record related to the event. This is used to generate the
   *          event payload.
   */
  void triggerEvent(String event, String recordId);

  /**
   * Triggers a multiple record synchronization event
   * 
   * @param event
   *          The unique identifier of the multiple record synchronization event
   * @param params
   *          The map of parameters used to obtain the records that will be related to the event.
   *          The keys are the parameter name and the map values are the values for each parameter.
   */
  void triggerEvent(String event, Map<String, Object> params);

  /**
   * @return true if the EventTrigger is able to handle the provided event. Otherwise, false is
   *         returned.
   */
  boolean handlesEvent(String event);

  /**
   * @return an integer representing the priority of this EventTrigger. It is used to select the
   *         EventTrigger with most priority in case there exists several instances which can handle
   *         the same kind of synchronization event. In that case, the one with the lowest priority
   *         will be used to trigger those events. By default this method returns 100.
   * 
   * @see #handlesEvent(String)
   */
  default int getPriority() {
    return 100;
  }
}
