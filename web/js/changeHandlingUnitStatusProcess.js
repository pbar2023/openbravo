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
 ************************************************************************
 */

OB.ChangeHandlingUnitStatus = {};

/**
 * On load function of the Change Handling Unit Status process. It is used to prefilter the status
 * combo to show only the statuses that the selected handling unit can be changed to.
 *
 * @param {Object} view - The process view
 */
OB.ChangeHandlingUnitStatus.onLoad = view => {
  const statusList = view.theForm.getItem('Status');
  const currentStatus = view.parentWindow.view.viewGrid.getSelectedRecord()
    .status;

  statusList.valueMap = Object.keys(statusList.valueMap)
    .filter(key => key !== currentStatus)
    .reduce(
      (statuses, key) => ({ ...statuses, [key]: statusList.valueMap[key] }),
      {}
    );
  statusList.setValue(Object.keys(statusList.valueMap)[0]);
};
