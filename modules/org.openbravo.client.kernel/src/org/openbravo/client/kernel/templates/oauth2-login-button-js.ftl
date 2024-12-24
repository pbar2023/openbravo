<#--
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
-->

<#list data.buttons as button>
<div id="${button.id}" class="signInButton signInButton-${button.id}" onclick="doExternalAuthentication({ authorizationURL: '${button.authorizationURL}', clientID: '${button.clientID}', state: '${button.state}', redirectURL: '${button.redirectURL}', scope: '${button.scope}' })">
  <span title="${button.name}"></span>
</div>
<#if button_index != data.buttons?size - 1>&nbsp;</#if>
</#list>

<#if data.buttons?has_content>
<style type="text/css">
  <#list data.buttons as button>
    <#if button.icon??>
  .signInButton-${button.id} > span {
    background: url(${button.icon});
    background-size: cover;
  }
    </#if>
  </#list>
</style>
</#if>
