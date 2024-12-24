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

package org.openbravo.common.hooks;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.application.window.FICExtension;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * This FICExtension is used to set "Is Active" field as false in Discounts and promotions window,
 * when any NEW record is created. It also displays a message informing about it. The purpose is the
 * record to be inactive until all the configuration has been finished, so records only are suitable
 * to be sent to the Web POS once they are fully configured.
 */
@ApplicationScoped
public class DiscountsDeactivedByDefaultFICExtension implements FICExtension {
  private static final Logger log = LogManager.getLogger();

  private final static String NEW_MODE = "NEW";

  private static final String DISCOUNTS_AND_PROMOTIONS_TAB = "800079";

  @Override
  public void execute(String mode, Tab tab, Map<String, JSONObject> columnValues, BaseOBObject row,
      List<String> changeEventCols, List<JSONObject> calloutMessages, List<JSONObject> attachments,
      List<String> jsExcuteCode, Map<String, Object> hiddenInputs, int noteCount,
      List<String> overwrittenAuxiliaryInputs) {
    long t = System.nanoTime();
    if (!isValidEvent(mode, tab)) {
      log.debug("took {} ns", (System.nanoTime() - t));
      return;
    }
    try {
      columnValues.get("inpisactive").put("value", false);
      JSONObject msg = new JSONObject();
      msg.put("text", OBMessageUtils.messageBD(new DalConnectionProvider(false),
          "DiscountsDeactivedByDefault", OBContext.getOBContext().getLanguage().getLanguage()));
      msg.put("severity", "TYPE_INFO");
      calloutMessages.add(msg);
    } catch (Exception e) {
      log.error("Error when generating warn message for POS Terminals", e);
    }

  }

  private boolean isValidEvent(String mode, Tab tab) {
    return NEW_MODE.equals(mode) && DISCOUNTS_AND_PROMOTIONS_TAB.equals(tab.getId());

  }

}
