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

package org.openbravo.scheduling;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.ProcessGroup;
import org.openbravo.model.ad.ui.ProcessGroupList;
import org.openbravo.model.ad.ui.ProcessRequest;
import org.openbravo.model.ad.ui.ProcessRun;
import org.openbravo.test.base.OBBaseTest;

/**
 *
 */
public class ProcessBundleSerializationTest extends OBBaseTest {

  /**
   * Before the fix of its related issue, this test failed throwing an exception due to
   * ProcessRunner attempting to set process run status on an already closed connection.
   */
  @Test
  public void processBundleWithProcessGroupCanBeSerialized() throws Exception {

    ProcessRequest processRequest = OBDal.getInstance()
        .getProxy(ProcessRequest.class, "PROCESS_REQUEST_ID");
    ProcessBundle processBundle = createBundle(processRequest);
    try (MockedStatic<OBDal> mocked = Mockito.mockStatic(OBDal.class)) {
      mockDAL(processRequest, mocked);
      ByteArrayOutputStream bos = serializeProcessBundle(processBundle);
      try {
        ProcessBundle copy = deserializeProcessBundle(bos);
        GroupInfo groupInfoCopy = copy.getGroupInfo();
        ProcessRequest request = groupInfoCopy.getRequest();
        assertTrue(request == processRequest);
      } finally {
        try {
          bos.close();
        } catch (IOException ex) {
          // Ignore close exception
        }
      }
    }

  }

  private ProcessBundle createBundle(ProcessRequest processRequest) {
    String processId = "TEST";
    VariablesSecureApp vars = new VariablesSecureApp("USER", "CLIENT", "ORGANIZATION");
    ProcessBundle processBundle = new ProcessBundle(processId, vars);
    GroupInfo groupInfo = createGroupInfo(vars, processRequest);
    processBundle.setGroupInfo(groupInfo);
    return processBundle;
  }

  private void mockDAL(ProcessRequest processRequest, MockedStatic<OBDal> mocked) {
    OBDal obdalInstance = mock(OBDal.class);
    mocked.when(OBDal::getInstance).thenReturn(obdalInstance);
    Class<ProcessRequest> expectedTypeClass = ProcessRequest.class;
    when(obdalInstance.get(eq(expectedTypeClass), any())).thenReturn(processRequest);
  }

  private ProcessBundle deserializeProcessBundle(ByteArrayOutputStream bos)
      throws IOException, ClassNotFoundException {
    byte[] data = bos.toByteArray();
    ByteArrayInputStream bis = new ByteArrayInputStream(data);
    ObjectInput in = new ObjectInputStream(bis);
    ProcessBundle copy = (ProcessBundle) in.readObject();
    return copy;
  }

  private ByteArrayOutputStream serializeProcessBundle(ProcessBundle processBundle)
      throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream out = null;
    out = new ObjectOutputStream(bos);
    out.writeObject(processBundle);
    out.flush();
    return bos;
  }

  private GroupInfo createGroupInfo(VariablesSecureApp vars, ProcessRequest request) {
    ProcessGroup group = OBDal.getInstance().getProxy(ProcessGroup.class, "PROCESS_GROUP_ID");
    ProcessRun run = OBDal.getInstance().getProxy(ProcessRun.class, "PROCESS_RUN_ID");
    ProcessGroupList groupList = OBDal.getInstance()
        .getProxy(ProcessGroupList.class, "PROCESS_GROUPLIST_ID");
    boolean stopWhenFails = false;
    return new GroupInfo(group, request, run, Arrays.asList(groupList), stopWhenFails, vars, null);
  }

}
