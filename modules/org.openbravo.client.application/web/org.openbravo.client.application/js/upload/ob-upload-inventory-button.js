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
 * All portions are Copyright (C) 2023 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

(function() {
  // Import Inventory Lines in Physical Inventory window
  let ImportInventoryButtonProps = isc.addProperties(
    {},
    isc.OBUploadView.UPLOAD_BUTTON_PROPERTIES,
    {
      actionUrl: './ApplicationDataUpload/ImportInventoryLines',
      inChildTab: false,
      title: OB.I18N.getLabel('OBUIAPP_ImportInventoryLines'),
      prompt: OB.I18N.getLabel('OBUIAPP_ImportInventoryLinesPrompt'),
      buttonType: 'ob-upload-import-inventory-lines',
      updateTabState: function() {
        // execute default updateTabState
        this.updateTabState.bind(this);
        if (this.isDisabled() === false) {
          // disable button depends on preferece
          this.setDisabled(
            OB.PropertyStore.get(
              'OBUIAPP_ShowImportStoreStockButton',
              this.view.windowId
            ) === 'N'
          );
        }
      },
      getPopupType: function() {
        return isc.OBUploadInventoryView;
      }
    }
  );
  OB.ToolbarRegistry.registerButton(
    ImportInventoryButtonProps.buttonType,
    isc.OBToolbarIconButton,
    ImportInventoryButtonProps,
    500,
    '255'
  );
})();
