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
 * All portions are Copyright (C) 2022 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.base;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Allows to set a priority between different objects
 */
public interface Prioritizable {

  /**
   * Sorts a list of {@code Prioritizable} objects by priority. Note that those with a lower
   * priority value are sorted first.
   * 
   * @param prioritizableObjects
   *          a list of {@code Prioritizable} objects
   * 
   * @return a list with the objects sorted by their priority
   */
  public static <P extends Prioritizable> List<P> sortByPriority(List<P> prioritizableObjects) {
    return prioritizableObjects.stream()
        .sorted(Comparator.comparing(Prioritizable::getPriority))
        .collect(Collectors.toList());
  }

  /**
   * @return an integer representing the priority of the object. It returns 100 by default.
   */
  public default int getPriority() {
    return 100;
  }
}
