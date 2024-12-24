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
 * All portions are Copyright (C) 2015-2024 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.importprocess;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.jmx.MBeanRegistry;
import org.openbravo.service.importprocess.ImportEntryProcessor.ImportEntryProcessRunnable;
import org.openbravo.service.json.JsonUtils;

/**
 * This class is the main manager for performing multi-threaded and parallel import of data from the
 * {@link ImportEntry} entity/table. The {@link ImportEntryManager} is a singleton/ApplicationScoped
 * class.
 * 
 * @author mtaal
 */
@ApplicationScoped
public class ImportEntryManager implements ImportEntryManagerMBean {

  /*
   * For an overview of the technical layer, first view this presentation:
   * http://wiki.openbravo.com/
   * wiki/Projects:Retail_Operations_Buffer#Presentation_on_Technical_Structure (note: hyperlink
   * maybe cut by line-break)
   * 
   * {@link ImportEntry} records are created by for example data synchronization processes. For
   * creating a new {@link ImportEntry} preferably the {@link #createImportEntry(String, String,
   * String)} method should be used. This method also takes care of calling all the relevant {@link
   * ImportEntryPreProcessor} instances. As the {@link ImportEntryManager} is a
   * singleton/applicationscoped class it should preferably be obtained through Weld.
   * 
   * After creating a new {@link ImportEntry} and committing the transaction the creator of the
   * {@link ImportEntry} should preferably call the method {@link #notifyNewImportEntryCreated()}.
   * This to wake up the {@link ImportEntryManagerThread} to process the new entry.
   * 
   * The {@link ImportEntryManager} runs a thread (the {@link ImportEntryManagerThread}) which
   * periodically queries if there are {@link ImportEntry} records in state 'Initial'. Any {@link
   * ImportEntry} with status 'Initial' is to be processed. The {@link ImportEntryManagerThread} is
   * started when the application starts and is shutdown when the Tomcat application stops, see the
   * {@link #start()} and {@link #shutdown()} methods which are called from the {@link
   * ImportProcessContextListener}.
   * 
   * As mentioned above, the {@link ImportEntryManagerThread} periodically checks if there are
   * {@link ImportEntry} records in state 'Initial'. This thread is also notified when a new {@link
   * ImportEntry} is created. If there are no notifications or {@link ImportEntry} records in state
   * 'Initial', then the thread waits for a preset amount of time before querying the {@link
   * ImportEntry} table again. This notification and waiting is managed through the {@link
   * #notifyNewImportEntryCreated()} and {@link ImportEntryManagerThread#doNotify()} and {@link
   * ImportEntryManagerThread#doWait()} methods. This mechanism uses a monitor object. See here for
   * more information:
   * http://javarevisited.blogspot.nl/2011/05/wait-notify-and-notifyall-in-java.html
   * 
   * When the {@link ImportEntryManagerThread} retrieves an {@link ImportEntry} instance in state
   * 'Initial' then it tries to find an {@link ImportEntryProcessor} which can handle this instance.
   * The right {@link ImportEntryProcessor} is found by using the {@link ImportEntryQualifier} and
   * Weld selections.
   * 
   * The {@link ImportEntryProcessor#handleImportEntry(ImportEntry)} method gets the {@link
   * ImportEntry} and processes it.
   * 
   * As the {@link ImportEntryManagerThread} runs periodically and the processing of {@link
   * ImportEntry} instances can take a long it is possible that an ImportEntry is again 'offered' to
   * the {@link ImportEntryProcessor} for processing. The {@link ImportEntryProcessor} should handle
   * this case robustly.
   * 
   * For more information see the {@link ImportEntryProcessor}.
   * 
   * This class also provides methods for error handling and result processing: {@link
   * #setImportEntryProcessed(String)}, {@link #setImportEntryError(String, Throwable)}, {@link
   * #setImportEntryErrorIndependent(String, Throwable)}.
   */

  private static final Logger log = LogManager.getLogger();

  private static ImportEntryManager instance;

  public static ImportEntryManager getInstance() {
    return instance;
  }

  @Inject
  @Any
  private Instance<ImportEntryProcessor> entryProcessors;

  @Inject
  @Any
  private ImportEntryArchiveManager importEntryArchiveManager;

  @Inject
  private ImportEntryClusterService clusterService;

  private ImportEntryManagerThread managerThread;
  private ThreadPoolExecutor executorService;

  private Map<String, ImportEntryProcessor> importEntryProcessors = new HashMap<String, ImportEntryProcessor>();

  private Map<String, ImportStatistics> stats = new HashMap<String, ImportEntryManager.ImportStatistics>();

  private boolean threadsStarted = false;

  private long initialWaitTime = 10000;
  private int managerWaitTime = 60; // seconds

  // default to number of processors plus some additionals for the main threads
  private int numberOfThreads = Runtime.getRuntime().availableProcessors() + 3;

  // used to determine the time to wait after each cycle before querying for new entries to be
  // processed
  private int processingCapacityPerSecond;

  // defines the batch size of reading and processing import entries by the
  // main thread, for each type of data the batch size is being read
  private int importBatchSize = 5000;

  // task queue limit in the executorservice, sufficiently large
  // to allow large sets of tasks but small enough to limit an implementing
  // subclass of ImportEntryProcessor going wild
  private int maxTaskQueueSize = 1000;

  private boolean isShutDown = false;

  /**
   * @return {@code true} if the ImportEntryManager is shut down. Otherwise {@code false} is
   *         returned. The ImportEntryManager can be shut down because of any of these reasons:
   * 
   *         1- The {@link #shutdown()} method has been invoked<br>
   *         2- In a clustered environment, the current node is not in charge of handling import
   *         entries
   */
  public boolean isShutDown() {
    return isShutDown || !isHandlingImportEntries();
  }

  /**
   * Keeps track of the current IEM's cycle. Each cycle queries for entries to process and schedules
   * them.
   */
  private static int currentCycle = 0;

  /*
   * During current cycle, new entries for the given type + key won't be accepted. This object is
   * accessed always from the main thread, there is not need it to be thread safe.
   */
  private static Set<String> blockedNewEntriesForKey = new HashSet<>();

  @PostConstruct
  private void init() {
    instance = this;
    importBatchSize = ImportProcessUtils.getCheckIntProperty(log, "import.batch.size",
        importBatchSize, 1000);
    numberOfThreads = ImportProcessUtils.getCheckIntProperty(log, "import.number.of.threads",
        numberOfThreads, 4);
    maxTaskQueueSize = ImportProcessUtils.getCheckIntProperty(log, "import.max.task.queue.size",
        maxTaskQueueSize, 50);
    managerWaitTime = ImportProcessUtils.getCheckIntProperty(log, "import.wait.time",
        managerWaitTime, 1);
    processingCapacityPerSecond = ImportProcessUtils.getCheckIntProperty(log,
        "import.processing.capacity.per.second", numberOfThreads * 30, 10);

    log.info("Import entry manager settings");
    log.info("  batch size: {}", importBatchSize);
    log.info("  number of threads: {}", numberOfThreads);
    log.info("  task queue size: {}", maxTaskQueueSize);
    log.info("  wait time: {} s", managerWaitTime);
    log.info("  processing capacity per second: {} entries", processingCapacityPerSecond);

    MBeanRegistry.registerMBean("ImportEntryManager", this);
  }

  public synchronized void start() {
    if (ImportProcessUtils.isImportProcessDisabled()) {
      log.debug("Import process disabled, not starting it");
      return;
    }

    if (threadsStarted) {
      return;
    }
    threadsStarted = true;

    log.debug("Starting Import Entry Framework");

    // same as fixed threadpool, will only stop accepting new tasks (throw an exception)
    // if there are maxTaskQueueSize in the queue, see the catch exception in submitRunnable.
    // http://www.nurkiewicz.com/2014/11/executorservice-10-tips-and-tricks.html
    final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(maxTaskQueueSize);
    executorService = new ThreadPoolExecutor(numberOfThreads, numberOfThreads, 0L,
        TimeUnit.MILLISECONDS, queue, new DaemonThreadFactory());

    // create, start the manager thread
    managerThread = new ImportEntryManagerThread(this);
    executorService.submit(managerThread);
    importEntryArchiveManager.start();
    isShutDown = false;
  }

  public long getNumberOfQueuedTasks() {
    return executorService.getQueue() == null ? 0 : executorService.getQueue().size();
  }

  public long getNumberOfActiveTasks() {
    return executorService.getActiveCount();
  }

  public boolean isExecutorRunning() {
    return executorService != null && !executorService.isShutdown()
        && !executorService.isTerminated() && managerThread.isRunning();
  }

  /**
   * Is called by the {@link ImportEntryProcessor} objects to submit a
   * {@link ImportEntryProcessor.ImportEntryProcessRunnable}. for execution.
   * 
   * Returns true if the runnable was properly submitted and false if the submission was rejected
   */
  boolean submitRunnable(Runnable runnable) {
    try {
      executorService.submit(runnable);
      return true;
    } catch (Exception e) {
      // except for logging we can ignore the exception
      // as the import entry will be offered again for reprocessing later anyway
      if (!log.isTraceEnabled()) {
        // Do not log stack trace unless log level is trace other way the log is too verbose, this
        // typically occurs when the queue limit is reached.
        log.warn("Cannot submit runnable. Queued: {} - Max queue: {}. Exception: {}. Runnable: {}",
            executorService.getQueue().size(), maxTaskQueueSize, e.getClass().getSimpleName(),
            runnable);
      } else {
        log.trace("Cannot submit runnable. Queued: {} - Max queue: {}. Runnable: {}",
            executorService.getQueue().size(), maxTaskQueueSize, runnable, e);
      }

      return false;
    }
  }

  /**
   * This method is used to set the cluster service into an state that indicates that it is
   * currently processing import entries. Note that if we are not in a clustered environment, this
   * method has no effect.
   */
  public void notifyStartProcessingInCluster() {
    clusterService.startProcessing();
  }

  /**
   * This method is used to set the cluster service into an state that indicates that it is
   * currently not processing import entries. Note that if we are not in a clustered environment,
   * this method has no effect.
   */
  public void notifyEndProcessingInCluster() {
    clusterService.endProcessing();
  }

  /**
   * @return {@code true} if the current cluster node is handling import entries, {@code false}
   *         otherwise. Note that if we are not in a clustered environment, this method is always
   *         returning {@code true}.
   */
  boolean isHandlingImportEntries() {
    return clusterService.isHandledInCurrentNode();
  }

  /**
   * Shutdown all the threads being used by the import framework
   */
  public void shutdown() {
    if (!threadsStarted) {
      return;
    }
    log.debug("Shutting down Import Entry Framework");

    isShutDown = true;

    if (executorService != null) {
      executorService.shutdownNow();
    }

    for (ImportEntryProcessor importEntryProcessor : importEntryProcessors.values()) {
      importEntryProcessor.shutdown();
    }
    importEntryArchiveManager.shutdown();
    executorService = null;
    threadsStarted = false;
    managerThread = null;
  }

  /**
   * Creates and saves the import entry, calls the
   * {@link ImportEntryPreProcessor#beforeCreate(ImportEntry)} on the
   * {@link ImportEntryPreProcessor} instances.
   * 
   * Note will commit the session/connection using {@link OBDal#commitAndClose()}
   */
  public void createImportEntry(String id, String typeOfData, String json) {
    createImportEntry(id, typeOfData, json, true);
  }

  /**
   * Creates and saves the import entry, calls the
   * {@link ImportEntryPreProcessor#beforeCreate(ImportEntry)} on the
   * {@link ImportEntryPreProcessor} instances.
   * 
   * Note will commit the session/connection using {@link OBDal#commitAndClose()}
   */
  public void createImportEntry(String id, String typeOfData, String json, boolean commitAndClose) {
    createImportEntry(id, typeOfData, json, commitAndClose, false);
  }

  /**
   * Creates and saves the import entry, calls the
   * {@link ImportEntryPreProcessor#beforeCreate(ImportEntry)} on the
   * {@link ImportEntryPreProcessor} instances.
   *
   * Note will commit the session/connection using {@link OBDal#commitAndClose()}
   */
  public void createImportEntry(String id, String typeOfData, String json, boolean commitAndClose,
      boolean isNonBlocking) {
    try {
      ImportEntryProcessor entryProcessor = getImportEntryProcessor(typeOfData);
      ImportEntryBuilder.newInstance(typeOfData, json) //
          .setId(id) //
          .setNotifyManager(commitAndClose) //
          .setIsNonBlocking(
              isNonBlocking || (entryProcessor != null && entryProcessor.isNonBlocking())) //
          .create();
    } catch (ImportEntryAlreadyExistsException e) {
      // Ignore exception when ImportEntry already exists either in ImportEntry or
      // ImportEntryArchive table
    }
  }

  public void reportStats(String typeOfData, long timeForEntry) {
    ImportStatistics importStatistics = stats.get(typeOfData);
    if (importStatistics == null) {
      createStatsEntry(typeOfData);
      importStatistics = stats.get(typeOfData);
    }
    importStatistics.addTiming(timeForEntry);
    if ((importStatistics.getCnt() % 100) == 0) {
      importStatistics.log();
    }
  }

  private void createStatsEntry(String typeOfData) {
    if (stats.containsKey(typeOfData)) {
      return;
    }
    ImportStatistics importStatistics = new ImportStatistics();
    importStatistics.setTypeOfData(typeOfData);
    stats.put(typeOfData, importStatistics);
  }

  /**
   * Is used to tell the import entry manager that a new entry was created in the import entry
   * table, so it can go process it immediately.
   */
  @Override
  public void notifyNewImportEntryCreated() {
    // make sure that the threads have started
    if (!threadsStarted) {
      start();
    }

    if (managerThread != null) {
      managerThread.doNotify();
    }
  }

  /**
   * Commits the current transaction if the current node is in charge of handling the import
   * entries. This method is intended to be used by those import entry processors which need to
   * commit their changes in the middle of the process. If processors don't use this method for
   * committing the changes they can leave an inconsistent state in the system if a subsequent call
   * to {@link #setImportEntryProcessed(String)} detects that we are not in the node that should
   * handle the import entries.
   *
   * @throws OBException
   *           if this method is invoked in a cluster node which is not handling the import entries
   */
  public void commitCurrentTransaction() throws SQLException {
    if (!isHandlingImportEntries() && Thread.currentThread() instanceof ImportEntryThread) {
      throw new OBException("Not allowed to commit in node " + clusterService.getNodeIdentifier()
          + " because active node is " + clusterService.getIdentifierOfNodeHandlingService());
    }
    OBDal.getInstance().getConnection().commit();
  }

  private boolean handleImportEntry(ImportEntry importEntry) {

    try {
      ImportEntryProcessor entryProcessor = getImportEntryProcessor(importEntry.getTypeofdata());
      if (entryProcessor == null) {
        log.warn("No import entry processor defined for type of data {}", importEntry);
        return false;
      } else {
        return entryProcessor.handleImportEntry(importEntry);
      }
    } catch (Throwable t) {
      log.error("Error while saving import entry {} ", importEntry, t);
      setImportEntryErrorIndependent(importEntry.getId(), t);
      return false;
    }
  }

  // somehow cache the import entry processors, Weld seems to create many instances
  // caching is probably also faster
  private ImportEntryProcessor getImportEntryProcessor(String qualifier) {
    ImportEntryProcessor importEntryProcessor = importEntryProcessors.get(qualifier);
    if (importEntryProcessor == null) {
      importEntryProcessor = entryProcessors.select(new ImportEntryProcessorSelector(qualifier))
          .get();
      if (importEntryProcessor != null) {
        importEntryProcessors.put(qualifier, importEntryProcessor);
      } else {
        // caller should handle it
        return null;
      }
    }
    return importEntryProcessor;
  }

  public void handleImportError(ImportEntry importEntry, Throwable t) {
    importEntry.setImportStatus("Error");
    importEntry.setErrorinfo(ImportProcessUtils.getErrorMessage(t));
    importEntry.setResponseinfo(createErrorResponseContent(t));
    OBDal.getInstance().save(importEntry);
  }

  /**
   * Set the ImportEntry to status Processed in the same transaction as the caller.
   * 
   * @throws OBException
   *           if the import entry can't be set as processed because the current cluster node is not
   *           in charge of processing import entries
   */
  public void setImportEntryProcessed(String importEntryId) {
    ImportEntry importEntry = OBDal.getInstance().get(ImportEntry.class, importEntryId);
    // If the import entry is being processed through one of the threads of the ImportEntryManager
    // and we are in cluster but the current node currently is not in charge of managing import
    // entries, we don't allow to set the import entry status as processed to avoid potential
    // duplicated executions
    if (!isHandlingImportEntries() && Thread.currentThread() instanceof ImportEntryThread) {
      throw new OBException("Import entry " + importEntryId + " could not be processed in node "
          + clusterService.getNodeIdentifier() + " because active node is "
          + clusterService.getIdentifierOfNodeHandlingService());
    }
    if (importEntry != null && !"Processed".equals(importEntry.getImportStatus())) {
      importEntry.setImportStatus("Processed");
      importEntry.setImported(new Date());
      OBDal.getInstance().save(importEntry);
    }
  }

  /**
   * Set the ImportEntry to status Error in the same transaction as the caller.
   */
  public void setImportEntryError(String importEntryId, Throwable t) {
    ImportEntry importEntry = OBDal.getInstance().get(ImportEntry.class, importEntryId);
    if (importEntry != null && !"Processed".equals(importEntry.getImportStatus())) {
      importEntry.setImportStatus("Error");
      importEntry.setErrorinfo(ImportProcessUtils.getErrorMessage(t));
      importEntry.setResponseinfo(createErrorResponseContent(t));
      OBDal.getInstance().save(importEntry);
    }
  }

  /**
   * Returns whether the ImportEntry is in status Error in the same transaction as the caller.
   */
  public boolean isImportEntryError(String importEntryId) {
    ImportEntry importEntry = OBDal.getInstance().get(ImportEntry.class, importEntryId);
    return importEntry != null && "Error".equals(importEntry.getImportStatus());
  }

  /**
   * Sets an {@link ImportEntry} in status Error but does this in its own transaction so not
   * together with the original data. This is relevant when the previous transaction which tried to
   * import the data fails.
   */
  public void setImportEntryErrorIndependent(String importEntryId, Throwable t) {
    OBDal.getInstance().rollbackAndClose();
    final OBContext prevOBContext = OBContext.getOBContext();
    OBContext.setOBContext("0", "0", "0", "0");
    try {
      // do not do org/client check as the error can be related to org/client access
      // so prevent this check to be done to even be able to save org/client access
      // exceptions
      OBContext.setAdminMode(false);
      ImportEntry importEntry = OBDal.getInstance().get(ImportEntry.class, importEntryId);
      if (importEntry != null && !"Processed".equals(importEntry.getImportStatus())) {
        importEntry.setImportStatus("Error");
        importEntry.setErrorinfo(ImportProcessUtils.getErrorMessage(t));
        importEntry.setResponseinfo(createErrorResponseContent(t));
        OBDal.getInstance().save(importEntry);
        OBDal.getInstance().commitAndClose();
      }
    } catch (Throwable throwable) {
      try {
        OBDal.getInstance().rollbackAndClose();
      } catch (Throwable ignored) {
      }
      throw new OBException(throwable);
    } finally {
      OBContext.restorePreviousMode();
      OBContext.setOBContext(prevOBContext);
    }
  }

  private String createErrorResponseContent(Throwable t) {
    try {
      final String jsonStr = JsonUtils.convertExceptionToJson(t);
      final JSONObject json = new JSONObject(jsonStr);
      if (json.has("response")) {
        return json.getJSONObject("response").toString();
      }
      return json.toString();
    } catch (Exception logIt) {
      log.error(logIt.getMessage(), logIt);
      return "{error:\"" + logIt.getMessage() + "\"}";
    }
  }

  @Override
  public void logImportEntryManager() {
    log.info(this);
  }

  @Override
  public String toString() {
    return "Import Entry Manager\n" + //
        "* Active threads: " + getNumberOfActiveTasks() + "/" + numberOfThreads + "\n" + //
        "* Processor queue size: " + getNumberOfQueuedTasks() + "/" + maxTaskQueueSize + "\n" + //
        "* Current cycle: " + currentCycle + "\n" + //
        "* Blocked type keys in this cycle (" + blockedNewEntriesForKey.size() + "): "
        + blockedNewEntriesForKey + "\n" + //
        "* Processors:\n" + //
        importEntryProcessors.entrySet()
            .stream()
            .map(e -> " " + e.getKey() + " - " + e.getValue())
            .collect(Collectors.joining("\n"))
        + "\n" + //
        "* Shut Down: " + isShutDown + "\n" //
        + clusterService;
  }

  private static class ImportEntryManagerThread implements Runnable {
    private final ImportEntryManager manager;

    // @formatter:off
    private static final String IMPORT_ENTRY_QRY =
          " from C_IMPORT_ENTRY "
        + "where typeofdata = :typeOfData "
        + "  and importStatus = 'Initial' "
        + "order by creationDate, createdtimestamp";
    // @formatter:on

    private boolean isRunning = false;
    private Object monitorObject = new Object();
    private boolean wasNotifiedInParallel = false;

    ImportEntryManagerThread(ImportEntryManager manager) {
      this.manager = manager;
    }

    // http://javarevisited.blogspot.nl/2011/05/wait-notify-and-notifyall-in-java.html
    // note the doNotify and doWait methods should not be synchronized themselves
    // the synchronization should happen on the monitorObject
    private void doNotify() {
      synchronized (monitorObject) {
        wasNotifiedInParallel = true;
        monitorObject.notifyAll();
      }
    }

    private void doWait() {
      synchronized (monitorObject) {
        try {
          if (!wasNotifiedInParallel) {
            log.debug("Waiting for next cycle or new import entries for {} s",
                manager.managerWaitTime);
            monitorObject.wait(manager.managerWaitTime * 1_000L);
            log.debug("Woken");
          }
          wasNotifiedInParallel = false;
        } catch (InterruptedException ignore) {
        }
      }
    }

    @Override
    public void run() {
      isRunning = true;

      Thread.currentThread().setName("Import Entry Manager Main");

      boolean isTest = OBPropertiesProvider.getInstance().getBooleanProperty("test.environment");

      // don't start right away at startup, give the system time to
      // really start
      log.debug("Started, first sleep " + manager.initialWaitTime);
      try {
        Thread.sleep(manager.initialWaitTime);
      } catch (Exception ignored) {
      }
      if (manager.isShutDown) {
        return;
      }
      log.debug("Run loop started");
      try {
        List<String> typesOfData = null;
        while (true) {
          try {

            // system is shutting down, bail out
            if (manager.isShutDown) {
              return;
            }

            if (shouldWait()) {
              doWait();
              // woken, re-start from beginning of loop
              continue;
            }

            currentCycle += 1;

            // obcontext cleared or wrong obcontext, repair
            if (OBContext.getOBContext() == null
                || !"0".equals(OBContext.getOBContext().getUser().getId())) {
              // make ourselves an admin
              OBContext.setOBContext("0", "0", "0", "0");
            }

            if (typesOfData == null) {
              typesOfData = ImportProcessUtils.getOrderedTypesOfData();
            }

            int entryCount = 0;
            int skippedEntries = 0;
            try {
              // start processing, so ignore any notifications happening before
              wasNotifiedInParallel = false;

              // read the types of data one by one in a specific order, so that they
              // don't block eachother with the limited batch size
              // being read
              for (String typeOfData : typesOfData) {
                log.debug("Reading import entries for type of data {}", typeOfData);

                final Query<ImportEntry> entriesQry = OBDal.getInstance()
                    .getSession()
                    .createQuery(IMPORT_ENTRY_QRY, ImportEntry.class)
                    .setParameter("typeOfData", typeOfData)
                    .setFirstResult(0)
                    .setFetchSize(100)
                    .setMaxResults(manager.importBatchSize);

                int typeOfDataEntryCount = 0;
                try (ScrollableResults entries = entriesQry.scroll(ScrollMode.FORWARD_ONLY)) {
                  while (entries.next() && isHandlingImportEntries()) {
                    final ImportEntry entry = (ImportEntry) entries.get(0);

                    log.trace("Handle import entry {}", entry::getIdentifier);

                    try {
                      if (manager.handleImportEntry(entry)) {
                        entryCount++;
                        typeOfDataEntryCount++;
                      } else {
                        skippedEntries++;
                      }
                      // remove it from the internal cache to keep it small
                      OBDal.getInstance().getSession().evict(entry);
                    } catch (Throwable t) {
                      ImportProcessUtils.logError(log, t);

                      // ImportEntryProcessors are custom implementations which can cause
                      // errors, so always catch them to prevent other import entries
                      // from not getting processed
                      manager.setImportEntryError(entry.getId(), t);
                    }
                  }
                }

                if (typeOfDataEntryCount > 0) {
                  log.debug("Handled {} entries for {}", typeOfDataEntryCount, typeOfData);
                }
              }
            } catch (Throwable t) {
              ImportProcessUtils.logError(log, t);
            } finally {
              OBDal.getInstance().commitAndClose();

              log.debug("cycle {} completed {}", currentCycle, manager);

              // we're done with current cycle, remove any blocked entry there might be
              blockedNewEntriesForKey.clear();
            }

            if (entryCount > 0) {
              // if there was data then just wait some time
              // give the threads time to process it all before trying
              // a next batch of entries to prevent retrieving from DB the same records we have just
              // handled in this cycle
              try {
                // wait a time based on the number of processed entries and
                // processingCapacityPerSecond (which is the expected number of entries that can be
                // processed per second), it defaults to one second per 30 records per
                // thread, somewhat arbitrary but high enough for most cases, also always wait 300
                // milliseconds additional to start up threads etc. note computation of timing
                // ensures that int rounding is done on 1000* entrycount

                // wait minimal 2 seconds or based on entry count, no minimal wait in case of test
                int minWait = isTest ? 0 : 2_000;
                long t = Math.max(minWait,
                    300 + ((1_000 * entryCount) / manager.processingCapacityPerSecond));

                log.debug(
                    "{} entries have been handled, {} skipped. Wait {} ms, and try again to capture new entries which have been added",
                    entryCount, skippedEntries, t);
                Thread.sleep(t);
              } catch (Exception ignored) {
              }
            } else {
              // else wait for new ones to arrive or check after a certain
              // amount of time
              doWait();
            }

          } catch (Throwable t) {
            ImportProcessUtils.logError(log, t);

            // wait for 5 min otherwise the loop goes wild in case of really severe
            // system errors like full disk
            try {
              Thread.sleep(300_000L);
            } catch (Exception ignored) {
            }
          }
        }
      } finally {
        isRunning = false;
      }
    }

    private boolean shouldWait() {
      if (manager.executorService.getQueue() != null
          && manager.executorService.getQueue().size() > (manager.maxTaskQueueSize - 1)) {
        // too busy, don't process, but wait
        return true;
      }
      // - in cluster: process if we are in the node in charge of handling the import entries,
      // otherwise just wait
      // - not in cluster: do not wait
      return !isHandlingImportEntries();
    }

    private boolean isHandlingImportEntries() {
      return manager.isHandlingImportEntries();
    }

    public boolean isRunning() {
      return isRunning;
    }
  }

  @javax.inject.Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE })
  public @interface ImportEntryQualifier {
    String entity();
  }

  @SuppressWarnings("all")
  public static class ImportEntryProcessorSelector extends AnnotationLiteral<ImportEntryQualifier>
      implements ImportEntryQualifier {
    private static final long serialVersionUID = 1L;

    final String entity;

    public ImportEntryProcessorSelector(String entity) {
      this.entity = entity;
    }

    @Override
    public String entity() {
      return entity;
    }
  }

  private static class ImportStatistics {
    private String typeOfData;
    private long cnt;
    private long cntPartial;
    private long totalTime;
    private long totalTimePartial;

    public void setTypeOfData(String typeOfData) {
      this.typeOfData = typeOfData;
    }

    public long getCnt() {
      return cnt;
    }

    public synchronized void addTiming(long timeForEntry) {
      cnt++;
      cntPartial++;
      totalTime += timeForEntry;
      totalTimePartial += timeForEntry;
    }

    public synchronized void log() {
      log.info("Timings for {}. Partial [cnt: {}, avg: {} ms] - Total [cnt: {}, avg: {} ms]",
          typeOfData, cntPartial, totalTimePartial / cntPartial, cnt, totalTime / cnt);
      cntPartial = 0;
      totalTimePartial = 0;
    }
  }

  /**
   * Creates threads which have deamon set to true.
   */
  public static class DaemonThreadFactory implements ThreadFactory {
    private AtomicInteger threadNumber = new AtomicInteger(0);
    private String threadNamePrefix;

    public DaemonThreadFactory() {
      this("Import Entry");
    }

    public DaemonThreadFactory(String threadNamePrefix) {
      this.threadNamePrefix = threadNamePrefix;
    }

    @Override
    public Thread newThread(Runnable runnable) {
      return new ImportEntryThread(runnable, threadNumber.getAndIncrement(), threadNamePrefix);
    }
  }

  /**
   * A Thread wrapper used to identify the threads used by the ImportEntryManager for processing the
   * import entries
   */
  private static class ImportEntryThread extends Thread {
    private ImportEntryThread(Runnable runnable, int threadNumber) {
      this(runnable, threadNumber, "Import Entry");
    }

    private ImportEntryThread(Runnable runnable, int threadNumber, String threadNamePrefix) {
      super(runnable, threadNamePrefix + " - " + threadNumber);
      if (getPriority() != Thread.NORM_PRIORITY) {
        setPriority(Thread.NORM_PRIORITY);
      }
      setDaemon(true);
    }
  }

  /**
   * {@link ImportEntryManager} runs in cycles. On each cycle it queries in batches the import
   * entries pending to be processed and schedules them to be handled by an
   * {@link ImportEntryProcessRunnable}.
   * 
   * @return The identifier of the currently running cycle.
   */
  int getCurrentCycle() {
    return currentCycle;
  }

  /**
   * Determines whether new entries for the given {@code typeOfData} and {@code key} are accepted in
   * the current cycle.
   * 
   * @param typeOfData
   *          Import Entry's type of data
   * @param key
   *          Import Entry's key
   * @return {@code true} in case new entries are accepted in the current cycle
   * 
   * @see #getCurrentCycle
   * @see #stopAcceptingEntries
   * @see ImportEntryProcessor#getProcessSelectionKey
   */
  boolean isAcceptingEntries(String typeOfData, String key) {
    return !blockedNewEntriesForKey.contains(typeOfData + "_" + key);
  }

  /**
   * After this method is invoked, new import entries for the given {@code typeOfData} and
   * {@code key} will not be accepted during the current running cycle. This method is invoked from
   * {@link ImportEntryProcessor#assignEntryToThread} when it is detected there are still queued
   * entries pending to be processed for the same {@code typeOfData} and {@code key}. Preventing, in
   * this way, to schedule new entries in an {@link ImportEntryProcessRunnable} that was created in
   * a previous cycle.
   * 
   * @param typeOfData
   *          Import Entry's type of data
   * @param key
   *          Import Entry's key
   * 
   * @see #isAcceptingEntries
   * @see ImportEntryProcessor#assignEntryToThread
   */
  void stopAcceptingEntries(String typeOfData, String key) {
    blockedNewEntriesForKey.add(typeOfData + "_" + key);
  }
}
