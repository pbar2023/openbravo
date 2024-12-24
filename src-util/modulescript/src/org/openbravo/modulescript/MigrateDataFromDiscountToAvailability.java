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
package org.openbravo.modulescript;

import org.openbravo.database.ConnectionProvider;

public class MigrateDataFromDiscountToAvailability extends ModuleScript {

  @Override
  public void execute() { 
    try {
      ConnectionProvider cp = getConnectionProvider(); 
      boolean isDataInOfferAvailabilityTableInserted = MigrateDataFromDiscountToAvailabilityData.isDataInOfferAvailabilityTableInserted(cp); 
      if (!isDataInOfferAvailabilityTableInserted) {
        MigrateDataFromDiscountToAvailabilityData[] results = MigrateDataFromDiscountToAvailabilityData.select(cp);
        
        for (int i = 0; i < results.length; i++) { 
          MigrateDataFromDiscountToAvailabilityData result = results[i];
          boolean updateDiscountData = false;
          if ("Y".equals(result.allweekdays) && (!"".equals(result.startingtime)|| !"".equals(result.endingtime))) {
            updateDiscountData = true;
            MigrateDataFromDiscountToAvailabilityData.insertAvailableDateTime(cp, result.adClientId, 
                result.adOrgId, result.mOfferId, "0", result.startingtime, result.endingtime);
          }else {
            if ("Y".equals(result.monday)) {
              updateDiscountData = true;
              MigrateDataFromDiscountToAvailabilityData.insertAvailableDateTime(cp, result.adClientId, 
                  result.adOrgId, result.mOfferId, "1", result.startingtimemonday, result.endingtimemonday);
            }
            
            if ("Y".equals(result.tuesday)) {
              updateDiscountData = true;
              MigrateDataFromDiscountToAvailabilityData.insertAvailableDateTime(cp, result.adClientId, 
                  result.adOrgId, result.mOfferId, "2", result.startingtimetuesday, result.endingtimetuesday);
            }
            
            if ("Y".equals(result.wednesday)) {
              updateDiscountData = true;
              MigrateDataFromDiscountToAvailabilityData.insertAvailableDateTime(cp, result.adClientId, 
                  result.adOrgId, result.mOfferId, "3", result.startingtimewednesday, result.endingtimewednesday);
            }
            
            if ("Y".equals(result.thursday)) {
              updateDiscountData = true;
              MigrateDataFromDiscountToAvailabilityData.insertAvailableDateTime(cp, result.adClientId, 
                  result.adOrgId, result.mOfferId, "4", result.startingtimethursday, result.endingtimethursday);
            }
            
            if ("Y".equals(result.friday)) {
              updateDiscountData = true;
              MigrateDataFromDiscountToAvailabilityData.insertAvailableDateTime(cp, result.adClientId, 
                  result.adOrgId, result.mOfferId, "5", result.startingtimefriday, result.endingtimefriday);
            }
            
            if ("Y".equals(result.saturday)) {
              updateDiscountData = true;
              MigrateDataFromDiscountToAvailabilityData.insertAvailableDateTime(cp, result.adClientId, 
                  result.adOrgId, result.mOfferId, "6", result.startingtimesaturday, result.endingtimesaturday);
            }
            
            if ("Y".equals(result.sunday)) {
              updateDiscountData = true;
              MigrateDataFromDiscountToAvailabilityData.insertAvailableDateTime(cp, result.adClientId,
                  result.adOrgId, result.mOfferId, "7", result.startingtimesunday, result.endingtimesunday);
            }
          }
          
          if (updateDiscountData) {
            MigrateDataFromDiscountToAvailabilityData.setWeekAndWeekDaysAsEmpty(cp, result.mOfferId);
          }
        }
      }
    } catch (Exception e) {
      handleError(e);
    }
  }
  
  
  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("0", null, 
        new OpenbravoVersion(3,0,222000));
  }
}
