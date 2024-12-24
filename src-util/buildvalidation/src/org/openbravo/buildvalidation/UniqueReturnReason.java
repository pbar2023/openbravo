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
 ************************************************************************
 */
package org.openbravo.buildvalidation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.openbravo.base.ExecutionLimits;
import org.openbravo.buildvalidation.BuildValidation;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.OpenbravoVersion;

/**
 * This build validation verifies duplicates in return reason table
 */
public class UniqueReturnReason extends BuildValidation {

  @Override
  public List<String> execute() {
    ConnectionProvider cp = getConnectionProvider();
    ArrayList<String> errors = new ArrayList<String>();
    try {
      UniqueReturnReasonData[] duplicateReturnReason = UniqueReturnReasonData.getDuplicateReturnReason(cp);
      
      if (duplicateReturnReason.length > 0) {
        errors.add("Due to a database constraint modification, it is no longer allowed to create two Return Reasons with the same search key and same Client. "
            + "There exists data in your database that do not fit this new constraint. "
            + "Please review following:- ");

        for (int i = 0; i < duplicateReturnReason.length; i++) {
          errors.add(" Search Key: " + duplicateReturnReason[i].searchkey+" Client "+duplicateReturnReason[i].client);
        }
      }

      return errors;
    } catch (Exception e) {
      return handleError(e);
    }
}

  @Override
  protected ExecutionLimits getBuildValidationLimits() {
    return new ExecutionLimits("0", null, new OpenbravoVersion(3, 0, 231000));
  }
}
