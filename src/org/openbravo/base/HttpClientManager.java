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
package org.openbravo.base;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.cache.TimeInvalidatedCache;

/**
 * Provides efficient usage of HttpClient instances by keeping them in a cache so they can be reused
 * to execute multiple HTTP requests without the overhead of building a new client for each of them.
 * To avoid resource leaks due to having HttpClient references in memory, the cache is invalidated
 * after a certain period of time so the cached instances can be disposed by the garbage collector.
 * If an HttpClient is requested to the cache after it has been invalidated, a fresh new HttpClient
 * is created and returned.
 */
@ApplicationScoped
public class HttpClientManager {
  private static final TimeInvalidatedCache<String, HttpClient> HTTP_CLIENTS = TimeInvalidatedCache
      .newBuilder()
      .name("HTTP Client Cache")
      .expireAfterDuration(Duration.ofMinutes(10))
      .build(key -> null);
  private static final Supplier<HttpClient> DEFAULT_SUPPLIER = () -> HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(30))
      .build();
  private static final String DEFAULT = "DEFAULT";

  /**
   * Sends the given request using the default HttpClient instance
   * 
   * @see #get()
   * 
   * @param request
   *          The HTTP request
   * 
   * @return the HTTP response with the content completely written as a string
   */
  public HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
    return send(request, HttpResponse.BodyHandlers.ofString());
  }

  /**
   * Sends the given request using the default HttpClient instance
   * 
   * @see #get()
   * 
   * @param request
   *          The HTTP request
   * @param responseBodyHandler
   *          The body handler for the response
   * 
   * @return the HTTP response
   */
  private <T> HttpResponse<T> send(HttpRequest request,
      HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
    return get().send(request, responseBodyHandler);
  }

  /**
   * @return the default HttpClient instance
   *
   * @see #get(String, Supplier)
   */
  private HttpClient get() {
    return get(DEFAULT, DEFAULT_SUPPLIER);
  }

  /**
   * Returns the HttpClient instance linked to the given key. If not present in the cache, the
   * HttpClient instance is built using the provided supplier.
   *
   * @param key
   *          The key that identifies the requested HttpClient instance
   * @param httpClientBuilder
   *          A supplier used to build the HttpClient in case there is not an entry in the cache
   *          with the given key. This can happen when the client is requested for the first time or
   *          after the cache has been invalidated.
   * @return the HttpClient instance
   */
  private HttpClient get(String key, Supplier<HttpClient> httpClientBuilder) {
    return HTTP_CLIENTS.get(key, k -> httpClientBuilder.get());
  }
}
