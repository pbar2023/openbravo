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
 * All portions are Copyright (C) 2014-2023 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.scheduling;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.ui.ProcessGroup;
import org.openbravo.model.ad.ui.ProcessGroupList;
import org.openbravo.model.ad.ui.ProcessRequest;
import org.openbravo.model.ad.ui.ProcessRun;
import org.openbravo.scheduling.ProcessBundle.Channel;
import org.openbravo.service.db.DalConnectionProvider;
import org.quartz.SchedulerException;

public class GroupInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * String constant id for the Process Group process.
   */
  public static final String processGroupId = "5BD4D2B3313E4C708F0AE29095AF16AD";

  public static final String END = "END";

  private transient org.openbravo.model.ad.ui.ProcessGroup group;
  private String groupId;

  private transient ProcessRequest request;
  private String requestId;

  private transient ProcessRun processRun;
  private String processRunId;

  private transient List<ProcessGroupList> groupList;
  private List<String> groupListIds;

  private int currentposition;

  private VariablesSecureApp vars;

  private transient ConnectionProvider conn;

  private StringBuilder groupLog;

  private Date startGroupTime;

  private Date endGroupTime;

  private String status;

  private boolean stopWhenFails;

  /**
   * Creates a new GroupInfo object with the given parameters
   * 
   * @param group
   *          the process group
   * @param request
   *          the process request of the process group
   * @param processRun
   *          the process run of the process request of the process group
   * @param groupList
   *          the list of processes that are part of the group
   * @param stopWhenFails
   *          if true the process group will stop after a process fails
   * @param vars
   *          clients security/application context variables
   * @param conn
   *          connection provider
   */
  public GroupInfo(ProcessGroup group, ProcessRequest request, ProcessRun processRun,
      List<ProcessGroupList> groupList, boolean stopWhenFails, VariablesSecureApp vars,
      ConnectionProvider conn) {
    super();
    this.group = group;
    this.groupId = group.getId();
    this.request = request;
    this.requestId = request.getId();
    this.groupList = groupList;
    this.groupListIds = groupList.stream()
        .map(ProcessGroupList::getId)
        .collect(Collectors.toList());
    this.currentposition = 0;
    this.vars = vars;
    this.conn = conn;
    this.processRun = processRun;
    this.processRunId = processRun.getId();
    this.stopWhenFails = stopWhenFails;
    this.status = Process.SUCCESS;
  }

  /**
   * Returns the Process Group
   * 
   * @return ProcessGroup
   */
  public List<ProcessGroupList> getGroupList() {
    if (groupList == null && groupListIds != null) {
      groupList = groupListIds.stream()
          .map(groupListId -> OBDal.getInstance().get(ProcessGroupList.class, groupListId))
          .collect(Collectors.toList());
    }
    return groupList;
  }

  /**
   * Returns the Process Group
   * 
   * @return ProcessGroup
   */
  public ProcessGroup getGroup() {
    if (group == null && groupId != null) {
      group = OBDal.getInstance().get(ProcessGroup.class, groupId);
    }
    return group;
  }

  /**
   * Returns the Process Request of the group
   * 
   * @return ProcessRequest of the group
   */
  public ProcessRequest getRequest() {
    if (request == null && requestId != null) {
      request = OBDal.getInstance().get(ProcessRequest.class, requestId);
    }
    return request;
  }

  /**
   * Returns the Process Run of the group
   * 
   * @return ProcessRun of the group
   */
  public ProcessRun getProcessRun() {
    if (processRun == null && processRunId != null) {
      processRun = OBDal.getInstance().get(ProcessRun.class, processRunId);
    }
    return processRun;
  }

  /**
   * Returns the status of the group
   * 
   * @return String with the status of the group
   */
  public String getStatus() {
    return status;
  }

  /**
   * Returns the log of the group, this method should be call at the end of all process executions
   * 
   * @return String with the log of the group
   */
  public String getLog() {
    String groupLogMessage = this.groupLog.toString();
    groupLogMessage = groupLogMessage
        + OBMessageUtils.getI18NMessage("PROGROUP_End", new String[] { getGroup().getName() });
    return groupLogMessage;
  }

  /**
   * Returns the duration of the group
   * 
   * @return Long with the duration in milliseconds
   */
  public long getDuration() {
    return endGroupTime.getTime() - startGroupTime.getTime();
  }

  /**
   * Execute the next process of the group
   * 
   * @return String with the process id executed or END if there is no more processes to execute
   */
  public String executeNextProcess() throws SchedulerException, ServletException {
    if (currentposition == 0) {
      groupLog = new StringBuilder();
      groupLog.append(
          now() + OBMessageUtils.getI18NMessage("PROGROUP_Start", new String[] { group.getName() })
              + "\n\n");
      startGroupTime = new Date();
    }

    if (currentposition < getGroupList().size()
        && (status.equals(Process.SUCCESS) || (status.equals(Process.ERROR) && !stopWhenFails))) {

      ProcessGroupList processList = getGroupList().get(currentposition);
      String currentProcessId = processList.getProcess().getId();
      currentposition++;
      groupLog.append(now() + processList.getSequenceNumber() + OBMessageUtils.getI18NMessage(
          "PROGROUP_StartSuccess", new String[] { processList.getProcess().getName() }) + "\n");

      // Execute next process immediately
      final ProcessBundle firstProcess = new ProcessBundle(currentProcessId, vars,
          Channel.SCHEDULED, getRequest().getClient().getId(),
          getRequest().getOrganization().getId(), getRequest().isSecurityBasedOnRole(), this)
              .init(getConnectionProvider());
      OBScheduler.getInstance().schedule(firstProcess);
      return currentProcessId;
    } else {
      endGroupTime = new Date();
      return END;
    }
  }

  private ConnectionProvider getConnectionProvider() {
    if (conn == null) {
      conn = new DalConnectionProvider(true);
    }
    return conn;
  }

  /**
   * Log the result of a process in the process group log
   * 
   * @param result
   *          Process.SUCCESS or Process.ERROR
   */
  public void logProcess(String result) {
    String resultMessage = "";
    ProcessGroupList processList = getGroupList().get(currentposition - 1);
    if (result.equals(Process.SUCCESS)) {
      resultMessage = OBMessageUtils.getI18NMessage("PROGROUP_Success", null);
    } else if (result.equals(Process.ERROR)) {
      resultMessage = OBMessageUtils.getI18NMessage("PROGROUP_Fail", null);
      this.status = Process.ERROR;
    }
    groupLog.append(
        now() + processList.getSequenceNumber() + OBMessageUtils.getI18NMessage("PROGROUP_Process",
            new String[] { processList.getProcess().getName() }) + resultMessage + "\n");
    groupLog.append(OBMessageUtils.getI18NMessage("PROGROUP_Separator", null) + "\n");
  }

  private String now() {
    return new Timestamp(System.currentTimeMillis()).toString() + " - ";
  }
}
