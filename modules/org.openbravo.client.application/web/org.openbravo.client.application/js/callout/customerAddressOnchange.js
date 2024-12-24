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

OB.CORE = window.OB.CORE || {};
OB.CORE.OnChangeFunctions = {};

OB.CORE.OnChangeFunctions.Set_Default_IsAddrProperty = function(
  item,
  view,
  form,
  grid
) {
  if (form.isNew) {
    // Ensure that if a property is created from address property tab it is marked as isAddressProperty
    form.setItemValue('isAddressProperty', true);
  }
};

OB.OnChangeRegistry.register(
  'EE99B25298D14C67AC7674E560F24BCB',
  'apiKey',
  OB.CORE.OnChangeFunctions.Set_Default_IsAddrProperty,
  'OBMOBC_Set_Default_IsAddrProperty'
);
