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

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.weld.WeldUtils;

/**
 * The entry point to launch synchronization events
 */
@ApplicationScoped
public class SynchronizationEvent {

  private static final Logger log = LogManager.getLogger();

  @Inject
  @Any
  private Instance<EventTrigger> eventTriggers;

  /**
   * @return the SynchronizationEvent instance
   */
  public static SynchronizationEvent getInstance() {
    return WeldUtils.getInstanceFromStaticBeanManager(SynchronizationEvent.class);
  }

  /**
   * Triggers a single record synchronization event
   * 
   * @param event
   *          The unique identifier of the synchronization event
   * @param recordId
   *          the ID that identifies the record related to the event. This is used to generate the
   *          event payload.
   */
  public void triggerEvent(String event, String recordId) {
    log.trace("Triggering event {} for record ID {}", event, recordId);
    Optional<EventTrigger> optTrigger = getEventTrigger(event);
    if (optTrigger.isPresent()) {
      optTrigger.get().triggerEvent(event, recordId);
      log.trace("Triggered event {} for record ID {}", event, recordId);
    } else {
      log.trace("No trigger found for event {}, record ID {}", event, recordId);
    }
  }

  /**
   * Triggers a multiple record synchronization event
   * 
   * @param event
   *          The unique identifier of the multiple record synchronization event
   * @param params
   *          The map of parameters used to obtain the records that will be related to the event.
   *          The keys are the parameter name and the map values are the values for each parameter.
   */
  public void triggerEvent(String event, Map<String, Object> params) {
    log.trace("Triggering multiple record event {} with params {}", event, params);
    Optional<EventTrigger> optTrigger = getEventTrigger(event);
    if (optTrigger.isPresent()) {
      optTrigger.get().triggerEvent(event, params);
      log.trace("Triggered multiple record event {} with params {}", event, params);
    } else {
      log.trace("No trigger found for multiple record event {}, params {}", event, params);
    }
  }

  /**
   * Selects the EvenTrigger instance with most priority that is able to trigger the provided event.
   * 
   * @param event
   *          The unique identifier of a synchronization event
   */
  private Optional<EventTrigger> getEventTrigger(String event) {
    return eventTriggers.stream()
        .filter(trigger -> trigger.handlesEvent(event))
        .sorted(Comparator.comparingInt(EventTrigger::getPriority))
        .findFirst();
  }
}
