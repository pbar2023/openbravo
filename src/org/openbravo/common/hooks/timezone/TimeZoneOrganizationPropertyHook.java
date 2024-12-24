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
package org.openbravo.common.hooks.timezone;

import org.openbravo.base.Entity;
import org.openbravo.base.Prioritizable;
import org.openbravo.base.structure.BaseOBObject;

/**
 * Allows to implement exceptions to select the property that references the organization with the
 * time zone used to compute the organization time zone based properties of a BaseOBObject. The
 * classes implementing this interface must be annotated with the {@link Entity} annotation for
 * which the exception must be implemented.
 * 
 * @author Eugen Hamuraru
 *
 */
public interface TimeZoneOrganizationPropertyHook extends Prioritizable {
  /**
   * @return the name of the property that references the organization with the time zone used to
   *         compute the organization time zone based properties of the given BaseOBObject. The
   *         returned property must belong to the entity of the given BaseOBObject.
   * 
   * @param bob
   *          a BaseOBObject that contains the record information used to retrieve the organization
   *          property.
   * 
   */
  public String getOrganizationProperty(BaseOBObject bob);
}
