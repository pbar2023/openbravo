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
 * All portions are Copyright (C) 2024 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.messageclient;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.openbravo.dal.core.DalThreadCleaner;

/**
 * Context listener for the MessageClientManager, it starts and stops it in the ServletContext
 * pipeline
 */
public class MessageClientContextListener implements ServletContextListener {
  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    try {
      MessageClientManager.getInstance().start();
    } finally {
      DalThreadCleaner.getInstance().cleanWithCommit();
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent servletContextEvent) {
    MessageClientManager.getInstance().shutdown();
  }
}
