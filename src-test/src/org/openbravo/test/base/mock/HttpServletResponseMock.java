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
package org.openbravo.test.base.mock;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * HttpServletResponse mock implementation
 */
public class HttpServletResponseMock implements HttpServletResponse {

  private PrintWriter writer;

  /**
   * Create a mock for HttpServletResponse with the given StringWriter
   * 
   * @param writer
   *          the StringWriter where the response data is written
   */
  public HttpServletResponseMock(StringWriter writer) {
    this.writer = new PrintWriter(writer);
  }

  @Override
  public void flushBuffer() throws IOException {
  }

  @Override
  public int getBufferSize() {
    return 0;
  }

  @Override
  public String getCharacterEncoding() {
    return null;
  }

  @Override
  public String getContentType() {
    return null;
  }

  @Override
  public Locale getLocale() {
    return null;
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    return null;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    return writer;
  }

  @Override
  public boolean isCommitted() {
    return false;
  }

  @Override
  public void reset() {
  }

  @Override
  public void resetBuffer() {
  }

  @Override
  public void setBufferSize(int arg0) {
  }

  @Override
  public void setCharacterEncoding(String arg0) {
  }

  @Override
  public void setContentLength(int arg0) {
  }

  @Override
  public void setContentLengthLong(long arg0) {
  }

  @Override
  public void setContentType(String arg0) {
  }

  @Override
  public void setLocale(Locale arg0) {
  }

  @Override
  public void addCookie(Cookie arg0) {
  }

  @Override
  public void addDateHeader(String arg0, long arg1) {
  }

  @Override
  public void addHeader(String arg0, String arg1) {
  }

  @Override
  public void addIntHeader(String arg0, int arg1) {
  }

  @Override
  public boolean containsHeader(String arg0) {
    return false;
  }

  @Override
  public String encodeRedirectURL(String arg0) {
    return null;
  }

  @SuppressWarnings("deprecation")
  @Override
  public String encodeRedirectUrl(String arg0) {
    return null;
  }

  @Override
  public String encodeURL(String arg0) {
    return null;
  }

  @SuppressWarnings("deprecation")
  @Override
  public String encodeUrl(String arg0) {
    return null;
  }

  @Override
  public String getHeader(String arg0) {
    return null;
  }

  @Override
  public Collection<String> getHeaderNames() {
    return null;
  }

  @Override
  public Collection<String> getHeaders(String arg0) {
    return null;
  }

  @Override
  public int getStatus() {
    return 0;
  }

  @Override
  public void sendError(int arg0) throws IOException {
  }

  @Override
  public void sendError(int arg0, String arg1) throws IOException {
  }

  @Override
  public void sendRedirect(String arg0) throws IOException {
  }

  @Override
  public void setDateHeader(String arg0, long arg1) {
  }

  @Override
  public void setHeader(String arg0, String arg1) {
  }

  @Override
  public void setIntHeader(String arg0, int arg1) {
  }

  @Override
  public void setStatus(int arg0) {
  }

  @SuppressWarnings("deprecation")
  @Override
  public void setStatus(int arg0, String arg1) {
  }
}
