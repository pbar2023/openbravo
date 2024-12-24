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
 * All portions are Copyright (C) 2015-2021 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.base.session;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * <p>
 * Prints local host name as it will be used when looking for machine specific Openbravo.properties
 * files to override common properties. The name of the file should be
 * <code>hostName.Openbravo.properties</code> where <code>hostName</code> is the output of the
 * execution of this task.
 * </p>
 * <p>
 * It can be executed by <code>ant host.name</code> command.
 * </p>
 * 
 * @see org.openbravo.base.ConfigParameters#overrideProperties
 * 
 * @author alostale
 *
 */
public class HostNameTask extends Task {
  private static Logger log = LogManager.getLogger();

  @Override
  public void execute() throws BuildException {
    try {
      log.info("Machine name: {}", InetAddress.getLocalHost().getHostName());
    } catch (UnknownHostException e) {
      log.error("Could not determine machine name", e);
    }
  }
}
