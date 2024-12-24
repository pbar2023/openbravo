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
package org.openbravo.dal.security;

import org.openbravo.base.structure.OrganizationEnabled;

/**
 * Allows to provide extra security checks to the {@link SecurityChecker}
 */
public interface SecurityCheckerExtension {

  /**
   * Checks if the current context user has read access to the provided object based on its
   * organization. This method is used to include special read access rules on top of the standard
   * readable organization criteria.
   * 
   * @param organizationEnabledObject
   *          the object whose read access based on its organization is checked
   *
   * @return true if the current context user has read access to the provided object or false
   *         otherwise.
   */
  public boolean isReadable(OrganizationEnabled organizationEnabledObject);
}
