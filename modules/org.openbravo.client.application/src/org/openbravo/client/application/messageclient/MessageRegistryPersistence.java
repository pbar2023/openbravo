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

import java.util.List;

/**
 * Interface that defines the methods that a Message persistence layer must implement to properly
 * work with MessageClientMsg objects
 */
public interface MessageRegistryPersistence {
  /**
   * Persists the MessageClientMsg in the corresponding storage
   * 
   * @param messageClientMsg
   *          Message to be persisted
   */
  void persistMessage(MessageClientMsg messageClientMsg);

  /**
   * Returns the pending messages to be sent from the persistence layer, should exclude expired
   * messages
   * 
   * @return Non-expired MessageClientMsg to be sent
   */
  List<MessageClientMsg> getPendingMessages();
}
