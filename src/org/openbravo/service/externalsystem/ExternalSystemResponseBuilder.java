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
 * All portions are Copyright (C) 2022-2024 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.externalsystem;

import java.util.concurrent.CompletionException;

import org.openbravo.service.externalsystem.ExternalSystemResponse.Type;

/**
 * A builder of {@ExternalSystemResponse} instances
 */
public class ExternalSystemResponseBuilder {

  private ExternalSystemResponse response;

  /**
   * @return a new ExternalSystemResponseBuilder
   */
  public static ExternalSystemResponseBuilder newBuilder() {
    return new ExternalSystemResponseBuilder();
  }

  private ExternalSystemResponseBuilder() {
    response = new ExternalSystemResponse();
  }

  /**
   * Sets the data received from the external system response
   * 
   * @param data
   *          the data received in the external system response
   */
  public ExternalSystemResponseBuilder withData(Object data) {
    response.setData(data);
    return this;
  }

  /**
   * Sets the status code of the external system response
   * 
   * @param statusCode
   *          the status code of the response
   */
  public ExternalSystemResponseBuilder withStatusCode(int statusCode) {
    response.setStatusCode(statusCode);
    return this;
  }

  /**
   * Sets the type of the external system response
   * 
   * @param type
   *          the response type
   * @see Type
   */
  public ExternalSystemResponseBuilder withType(Type type) {
    response.setType(type);
    return this;
  }

  /**
   * Sets the error information of the external system response. It also sets the response type as
   * {@link Type#ERROR}.
   * 
   * @param error
   *          the error information
   */
  public ExternalSystemResponseBuilder withError(Object error) {
    response.setType(Type.ERROR);
    response.setError(error);
    return this;
  }

  /**
   * Sets the error information and cause of the external system response. It also sets the response
   * type as {@link Type#ERROR}.
   *
   * @param error
   *          the error information
   * @param cause
   *          the error cause
   */
  public ExternalSystemResponseBuilder withError(Object error, Throwable cause) {
    withError(error);
    if (cause instanceof CompletionException) {
      response.setErrorCause(cause.getCause());
    } else {
      response.setErrorCause(cause);
    }
    return this;
  }

  /**
   * @return the built {@link ExternalSystemResponse}
   */
  public ExternalSystemResponse build() {
    return response;
  }
}
