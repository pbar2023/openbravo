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
package org.openbravo.service.importprocess;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.NonBlockingExecutorServiceProvider;
import org.openbravo.service.importprocess.ImportEntryProcessor.ImportEntryProcessRunnable;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * An import entry process runnable that handles non-blocking execution. It provides a processAsync
 * function which other classes that extend this one should implement, by returning a
 * CompletableFuture. Once the CompletableFuture has been completed,
 * {@link NonBlockingImportEntryProcessRunnable#completed} method is executed and handles marking
 * the Import Entry as processed.
 * 
 * Exceptions and errors are handled through
 * {@link NonBlockingImportEntryProcessRunnable#cleanUpAndLogOnException} and
 * {@link NonBlockingImportEntryProcessRunnable#markImportEntryWithError} methods.
 */
public abstract class NonBlockingImportEntryProcessRunnable extends ImportEntryProcessRunnable {

  @Inject
  @Any
  private Instance<ImportEntryPostProcessor> importEntryPostProcessors;

  private final Logger log = LogManager.getLogger();

  /**
   * Method that handles asynchronous task execution. It should return a CompletableFuture. When the
   * CompletableFuture is completed, the Import Entry will be marked as processed if it was properly
   * processed. Otherwise, if the CompletableFuture thrown some exception or there was an error, it
   * will be marked with Error.
   * 
   * Exceptions should be thrown when the processing of the ImportEntry must fail. This way it is
   * properly handled by the processEntry method and the ImportEntry is properly marked with an
   * Error state and an Error message
   * 
   * @param importEntry
   *          Import Entry to process
   * @return a CompletableFuture
   */
  public abstract CompletableFuture<?> processAsync(ImportEntry importEntry) throws Exception;

  @Override
  protected void processEntry(ImportEntry importEntry) throws Exception {
    processAsync(importEntry).handle((result, ex) -> {
      if (ex != null) {
        cleanUpAndLogOnException(ex);
        markImportEntryWithError(importEntry.getId(), ex);
        return CompletableFuture.failedFuture(ex);
      }

      completed(importEntry);
      return CompletableFuture.completedFuture(null);
    });
  }

  @Override
  protected void postProcessEntry(String importEntryId, long t0, ImportEntry localImportEntry,
      String typeOfData) {
    OBContext.setAdminMode(true);
    try {
      if (!"Initial".equals(localImportEntry.getImportStatus())) {
        super.postProcessEntry(importEntryId, t0, localImportEntry, typeOfData);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Override
  public void cleanUp(Set<String> importEntriesInExecution) {
    if (!importEntryIds.isEmpty()) {
      // In case of non-blocking import entry, we save it in the list of import entries to keep
      importEntriesInExecution.addAll(importEntryIds);
    }
  }

  private void completed(ImportEntry importEntry) {
    setImportEntryQueuedEntryContext(importEntry);
    OBContext.setAdminMode(true);
    try {
      log.debug("Completed non-blocking import entry {}", importEntry);
      ImportEntryManager.getInstance().setImportEntryProcessed(importEntry.getId());
      importEntry.setImportStatus("Processed");
      postProcessEntry(importEntry.getId(), 0L, importEntry, importEntry.getTypeofdata());
    } finally {
      OBDal.getInstance().commitAndClose();
      OBContext.restorePreviousMode();
    }
    executePostProcessors(importEntry);
  }

  /**
   * Executes post proceessor hooks, provided by ImportEntryPostProcessor
   *
   * @param importEntry
   *          ImportEntry that will be post-processed
   */
  private void executePostProcessors(ImportEntry importEntry) {
    for (ImportEntryPostProcessor importEntryPostProcessor : importEntryPostProcessors
        .select(new ImportEntryManager.ImportEntryProcessorSelector(importEntry.getTypeofdata()))) {
      importEntryPostProcessor.afterProcessing(importEntry);
    }
  }

  /**
   * Marks the import entry with an error status in an independent transaction
   * 
   * @param importEntryId
   *          Import entry id of the import entry to change its status
   * @param t
   *          Throwable that caused the error
   */
  private void markImportEntryWithError(String importEntryId, Throwable t) {
    try {
      ImportEntryManager.getInstance().setImportEntryErrorIndependent(importEntryId, t);
    } catch (Throwable ex) {
      ImportProcessUtils.logError(log, ex);
    }
  }

  /**
   * Retrieves the executor service where non-blocking execution is expected to be executed
   *
   * @return An executor service with non-blocking threads
   */
  protected ExecutorService getExecutorService() {
    return NonBlockingExecutorServiceProvider.getExecutorService();
  }
}
