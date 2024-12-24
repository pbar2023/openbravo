/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
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
 *************************************************************************
 */

package org.openbravo.common.hooks;

import org.openbravo.base.Prioritizable;

/**
 * Classes implementing this hook gets the print template to be used for handling unit label
 * printing
 * 
 * Only the implementation with lowest priority value will be used (see
 * {@link Prioritizable#getPriority()})
 * 
 * The default implementation can be found at {@link GetDefaultPrintTemplate}
 */
public interface GetPrintTemplateHook extends Prioritizable {

  /**
   * Gets Print Template to be used for printing Handling Unit Label
   * 
   * @return the print template to print Handling Unit Label
   */
  public String getPrintTemplate();

}
