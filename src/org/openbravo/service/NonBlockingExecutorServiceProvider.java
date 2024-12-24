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
package org.openbravo.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.service.importprocess.ImportEntryManager;

/**
 * Singleton that provides an ExecutorService for non-blocking tasks
 */
@ApplicationScoped
public class NonBlockingExecutorServiceProvider {
  private static ExecutorService executorService = null;

  private static final Integer DEFAULT_AMOUNT_OF_NON_BLOCKING_THREADS = 10;
  private static final String AMOUNT_OF_NON_BLOCKING_THREADS_PROPERTY = "amountOfNonBlockingThreads";
  private static final Logger log = LogManager.getLogger();

  private static synchronized void initializeExecutorService() {
    if (executorService == null) {
      int amountOfThreads;
      String amountOfThreadsProperty = OBPropertiesProvider.getInstance()
          .getOpenbravoProperties()
          .getProperty(AMOUNT_OF_NON_BLOCKING_THREADS_PROPERTY);
      if (amountOfThreadsProperty == null) {
        log.debug("Preference {} not found, defaulting to {}",
            AMOUNT_OF_NON_BLOCKING_THREADS_PROPERTY, DEFAULT_AMOUNT_OF_NON_BLOCKING_THREADS);
        amountOfThreads = DEFAULT_AMOUNT_OF_NON_BLOCKING_THREADS;
      } else {
        amountOfThreads = Integer.parseInt(amountOfThreadsProperty);
      }

      executorService = Executors.newFixedThreadPool(amountOfThreads,
          new ImportEntryManager.DaemonThreadFactory("NonBlocking"));
    }
  }

  public static ExecutorService getExecutorService() {
    if (executorService == null) {
      initializeExecutorService();
    }
    return executorService;
  }
}
