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

package org.openbravo.buildvalidation;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.base.ExecutionLimits;
import org.openbravo.buildvalidation.BuildValidation;
import org.openbravo.modulescript.OpenbravoVersion;

/**
 * This build validation will be executed when updating from a version lower than 16Q1
 * 3.0PR16Q1 version is: 3.0.28207
 *
 * If it runs it just blocks the update giving a message that "direct" updates
 * of such an old release is not possible.
 * 
 * Update is still possible by doing 2 smaller updates:
 * - First to an older intermediate version (before adding this check)
 * - Then a 2nd one updating to the latest.
 * Wiki page explaining this and which has a table of such restrictions:
 * http://wiki.openbravo.com/wiki/RestrictOldDirectUpdates
 * 
 */
public class RestrictOldDirectUpdates extends BuildValidation {

  private static String errorMsg = "" +
	  "Since version 23Q1 direct updates from releases older than 16Q1 are no longer possible.\n" +
	  "More information on how to still update in this case: http://wiki.openbravo.com/wiki/RestrictOldDirectUpdates";

  @Override
  public List<String> execute() {
    List<String> errors = new ArrayList<>();
    errors.add(errorMsg);
    return errors;
  }

  @Override
  protected ExecutionLimits getBuildValidationLimits() {
    return new ExecutionLimits("0", null, new OpenbravoVersion(3, 0, 28207));
  }
}
