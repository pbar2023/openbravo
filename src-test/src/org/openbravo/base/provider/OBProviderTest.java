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
package org.openbravo.base.provider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.openbravo.base.util.OBClassLoader;

public class OBProviderTest {

  @Test
  public void assertProviderIsRegistered() throws ClassNotFoundException {

    OBProvider.getInstance()
        .register("org.openbravo.api.ExportService",
            OBClassLoader.getInstance().loadClass("org.openbravo.api.service.ApiExportWebService"),
            true);

    Object o = OBProvider.getInstance()
        .getMostSpecificService("org.openbravo.api.ExportService/User");
    assertThat(o, notNullValue());

  }

  @Test
  public void assertSpecificProviderIsRegistered() throws ClassNotFoundException {

    OBProvider.getInstance()
        .register("org.openbravo.api.ExportService/ReprintableReport", OBClassLoader.getInstance()
            .loadClass("org.openbravo.api.service.ReprintableDocumentWebService"), true);

    Object o = OBProvider.getInstance()
        .getMostSpecificService("/org.openbravo.api.ExportService/ReprintableReport/invoice/1234/");
    assertThat(o, notNullValue());

  }

  @Test
  public void assertProviderIsNotRegistered() throws ClassNotFoundException {
    OBProvider.getInstance()
        .register("org.openbravo.api.ExportService",
            OBClassLoader.getInstance().loadClass("org.openbravo.api.service.ApiExportWebService"),
            true);

    OBProviderException thrown = assertThrows(OBProviderException.class,
        () -> OBProvider.getInstance()
            .getMostSpecificService("org.openbravo.api.ExportService1/User"));

    assertThat(thrown.getMessage(),
        containsString("No registration for name org.openbravo.api.ExportService1/User"));

  }

}
