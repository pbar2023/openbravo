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

/**
 * Keeps the information of a response from an external system
 */
public class ExternalSystemResponse {

  /**
   * Defines the supported external system response types after a communication with an external
   * system:<br>
   * - SUCCESS: when the communication was correct<br>
   * - ERROR: to indicate that there were errors during the communication with the external system
   */
  public enum Type {
    SUCCESS, ERROR
  }

  private Object data;
  private int statusCode;
  private Type type;
  private Object error;
  private Throwable errorCause;

  /**
   * Creates a new ExternalSystemResponse. Instance of this class must be created with a
   * {@link ExternalSystemResponseBuilder}.
   */
  ExternalSystemResponse() {
  }

  /**
   * @return the data received in the external system response
   */
  public Object getData() {
    return data;
  }

  /**
   * Sets the data received from the external system response
   * 
   * @param data
   *          the data received in the external system response
   */
  void setData(Object data) {
    this.data = data;
  }

  /**
   * @return the type of the external system response
   * @see Type
   */
  public Type getType() {
    return type;
  }

  /**
   * Sets the type of the external system response
   * 
   * @param type
   *          the response type
   * @see Type
   */
  void setType(Type type) {
    this.type = type;
  }

  /**
   * @return the status code of the external system response
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * Sets the status code of the external system response
   * 
   * @param statusCode
   *          the status code of the response
   */
  void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  /**
   * @return the error information of the external system response
   */
  public Object getError() {
    return error;
  }

  /**
   * @return the cause of error of the external system response
   */
  public Throwable getErrorCause() {
    return errorCause;
  }

  /**
   * Sets the error information of the external system response
   * 
   * @param error
   *          the error information
   */
  void setError(Object error) {
    this.error = error;
  }

  /**
   * Sets the error cause of the external system response
   *
   * @param errorCause
   *          the error cause
   */
  void setErrorCause(Throwable errorCause) {
    this.errorCause = errorCause;
  }
}
