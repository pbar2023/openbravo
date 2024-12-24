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
package org.openbravo.client.application.window;

import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.model.ad.ui.Tab;

/**
 * Classes implementing this interface are injected in the {@link FormInitializationComponent}
 * class. Using this interface it is possible to customize the return object of that class.
 */
public interface FICFinalObjectExtension {

  /**
   * This method is executed in the {@link FormInitializationComponent#execute(Map, String) execute}
   * method after the response JSONObject is built. It receives some of the objects used to build
   * the response, the initial JSON content received as a parameter and the final JSONObject in his
   * actual state.
   * 
   * @param mode
   *          The execution mode.
   * @param tab
   *          The Tab owner of the Form that it is being executed.
   * @param columnValues
   *          Map with the values of forms columns.
   * @param row
   *          The BaseOBObject that it is being edited in the form.
   * @param jsContent
   *          JSON content received as a parameter in the FormInitializationComponent.
   * 
   * @return The changes to the properties of the final JSON Object.
   */
  public JSONObject execute(String mode, Tab tab, Map<String, JSONObject> columnValues,
      BaseOBObject row, JSONObject jsContent) throws JSONException;
}
