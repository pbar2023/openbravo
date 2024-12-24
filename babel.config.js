/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at https://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2020-2024 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

/* eslint-disable no-undef */
module.exports = app => {
  const isTest = app.env('test');
  const plugins = [];
  if (isTest) {
    plugins.push('babel-plugin-transform-vite-meta-env');
  }

  return {
    presets: [
      [
        '@babel/preset-env',
        {
          // keep this list in sync with userinterface.react
          targets: [
            'last 2 Chrome versions',
            'last 2 Firefox versions',
            'last 2 Safari versions',
            'current node'
          ],
          include: []
        }
      ],
      '@babel/preset-react'
    ],
    plugins
  };
};
