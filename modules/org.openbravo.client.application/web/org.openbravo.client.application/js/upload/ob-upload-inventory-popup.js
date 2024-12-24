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

isc.defineClass('OBUploadInventoryView', isc.OBUploadView);

// == OBUploadInventoryView ==
//   OBUploadInventoryView is the view that represents the window to upload
// the file with Physical Inventory lines.
isc.OBUploadInventoryView.addProperties({
  action: './ApplicationDataUpload/ImportInventoryLines',
  popupHeight: '350',

  viewProperties: {
    additionalFields: [
      {
        selectOnFocus: true,
        width: '100%',
        canFocus: true,
        name: 'noStock',
        title: OB.I18N.getLabel('OBUIAPP_NoStock'),
        defaultValue: 'quantityCountZero',
        prompt: OB.I18N.getLabel('OBUIAPP_ExplainNoStock'),
        // is the list reference
        type: '_id_17',
        valueMap: {
          deleteLines: OB.I18N.getLabel('OBUIAPP_DeleteLines'),
          quantityCountZero: OB.I18N.getLabel('OBUIAPP_QuantityCountZero'),
          quantityCountOriginal: OB.I18N.getLabel(
            'OBUIAPP_QuantityCountOriginal'
          ),
          doNotModify: OB.I18N.getLabel('OBUIAPP_DoNotModify')
        }
      }
    ]
  }
});
