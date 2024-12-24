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

package org.openbravo.service.externalsystem.http;

import javax.enterprise.util.AnnotationLiteral;

/**
 * Selector for the {@link HttpAuthorizationMethod} annotation
 */
@SuppressWarnings("all")
public class HttpAuthorizationMethodSelector extends AnnotationLiteral<HttpAuthorizationMethod>
    implements HttpAuthorizationMethod {
  private static final long serialVersionUID = 1L;

  private final String value;

  /**
   * Builds a new HttpAuthorizationMethodSelector
   *
   * @param value
   *          The identifier of an authorization method. This is a value used to annotate
   *          {@link HttpAuthorizationProvider} instances with the {@link HttpAuthorizationMethod}
   *          annotation.
   */
  public HttpAuthorizationMethodSelector(String value) {
    this.value = value;
  }

  @Override
  public String value() {
    return value;
  }
}
