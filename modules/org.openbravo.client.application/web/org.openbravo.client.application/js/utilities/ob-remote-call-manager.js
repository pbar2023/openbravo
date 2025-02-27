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
 * All portions are Copyright (C) 2010-2024 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

// = Remote Call Manager =
//
// The Remote Call Manager provides support for calling java from the client
// and retrieving the result.
//
(function(OB, isc) {
  if (!OB || !isc) {
    throw {
      name: 'ReferenceError',
      message: 'openbravo and isc objects are required'
    };
  }

  // cache object references locally
  var ISC = isc; // Local reference to RemoveCallManager instance

  function RemoteCallManager() {}

  RemoteCallManager.prototype = {
    // ** {{{ RemoteCallManager.call(actionName, params, content, callBack)
    // }}} **
    //
    // Calls the remote action on the server. The response is processed by
    // calling the callBack function.
    // The callback function gets three parameters: rpcResponse,
    // data (the resulting content), rpcRequest.
    // See the Smartclient RPCCallback type for more information.
    //
    // Parameters:
    // * {{{actionName}}}: is the unique identifier of the action on the
    // server
    // * {{{data}}}: is an Object with the data passed to the action.
    // * {{{requestParams}}}: request parameters send to the action.
    // * {{{callback}}}: is the function which is called after receiving the
    // result.
    // * {{{callerContext}}}: a context object which is available
    // (as rpcRequest.clientContext) when the callback gets called
    // * {{{errorCallback}}}: a function that is executed when the call returns with an error status
    //
    call: function(
      actionName,
      data,
      requestParams,
      callback,
      callerContext,
      errorCallback
    ) {
      var requestParameters = {};
      ISC.addProperties(requestParameters, requestParams);
      requestParameters._action = actionName;
      var rpcRequest = {};

      // support overriding of the http method through a request param
      if (requestParameters.httpMethod) {
        rpcRequest.httpMethod = requestParameters.httpMethod;
      }

      rpcRequest.actionURL =
        OB.Application.contextUrl + 'org.openbravo.client.kernel';
      rpcRequest.callback = callback;
      if (isc.isA.Function(errorCallback)) {
        rpcRequest.errorCallback = errorCallback;
      }
      if (data) {
        let requestData;
        if (Array.isArray(data)) {
          // There are cases where request data is just an array. In those cases CSRF Token is not inserted
          // See issue 54801 for more info
          requestData = [...data];
        } else {
          requestData = { ...data, csrfToken: OB.User.csrfToken };
        }

        rpcRequest.data = ISC.JSON.encode(requestData);
        rpcRequest.httpMethod = 'POST';
      } else if (!rpcRequest.httpMethod) {
        rpcRequest.httpMethod = 'GET';
      }
      rpcRequest.contentType = 'application/json;charset=UTF-8';
      rpcRequest.useSimpleHttp = true;
      rpcRequest.evalResult = true;
      rpcRequest.params = requestParameters;
      rpcRequest.clientContext = callerContext;
      ISC.RPCManager.sendRequest(rpcRequest);
    }
  };

  // Initialize RemoteCallManager object
  OB.RemoteCallManager = new RemoteCallManager();
})(OB, isc);
