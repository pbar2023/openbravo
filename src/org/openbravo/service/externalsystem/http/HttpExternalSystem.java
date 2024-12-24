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
package org.openbravo.service.externalsystem.http;

import java.io.InputStream;
import java.net.Authenticator;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.cache.Cacheable;
import org.openbravo.service.NonBlockingExecutorServiceProvider;
import org.openbravo.service.externalsystem.ExternalSystem;
import org.openbravo.service.externalsystem.ExternalSystemConfigurationError;
import org.openbravo.service.externalsystem.ExternalSystemData;
import org.openbravo.service.externalsystem.ExternalSystemResponse;
import org.openbravo.service.externalsystem.ExternalSystemResponse.Type;
import org.openbravo.service.externalsystem.ExternalSystemResponseBuilder;
import org.openbravo.service.externalsystem.HttpExternalSystemData;
import org.openbravo.service.externalsystem.Protocol;

/**
 * Allows to communicate with an external system through HTTP requests
 */
@Protocol("HTTP")
public class HttpExternalSystem extends ExternalSystem implements Cacheable {
  private static final Logger log = LogManager.getLogger();
  public static final int MAX_TIMEOUT = 30;
  private static final int MAX_RETRIES = 1;

  private String url;
  private String requestMethod;
  private int timeout;
  private HttpClient client;
  private HttpAuthorizationProvider authorizationProvider;

  @Inject
  @Any
  private Instance<HttpAuthorizationProvider> authorizationProviders;

  @Override
  public void configure(ExternalSystemData configuration) {
    super.configure(configuration);

    HttpExternalSystemData httpConfig = configuration.getExternalSystemHttpList()
        .stream()
        .filter(HttpExternalSystemData::isActive)
        .findFirst()
        .orElseThrow(() -> new ExternalSystemConfigurationError(
            "No HTTP configuration found for external system with ID " + configuration.getId()));

    url = httpConfig.getURL();
    requestMethod = httpConfig.getRequestMethod();
    timeout = getTimeoutValue(httpConfig);
    setAuthorizationProvider(newHttpAuthorizationProvider(httpConfig));
    client = buildClient();
  }

  private int getTimeoutValue(HttpExternalSystemData httpConfig) {
    Long configTimeout = httpConfig.getTimeout();
    if (configTimeout > MAX_TIMEOUT) {
      return MAX_TIMEOUT;
    }
    return configTimeout.intValue();
  }

  /** Internal API, this method is not private only because of testing purposes */
  void setAuthorizationProvider(HttpAuthorizationProvider authorizationProvider) {
    this.authorizationProvider = authorizationProvider;
  }

  private HttpClient buildClient() {
    HttpClient.Builder builder = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(timeout));

    builder.executor(NonBlockingExecutorServiceProvider.getExecutorService());

    if (authorizationProvider instanceof Authenticator) {
      builder.authenticator((Authenticator) authorizationProvider);
    }
    return builder.build();
  }

  private HttpAuthorizationProvider newHttpAuthorizationProvider(
      HttpExternalSystemData httpConfig) {
    String authorizationType = httpConfig.getAuthorizationType();
    HttpAuthorizationProvider provider = authorizationProviders
        .select(new HttpAuthorizationMethodSelector(authorizationType))
        .stream()
        .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
          if (list.isEmpty()) {
            throw new ExternalSystemConfigurationError(
                "No HTTP authorization provider found for method " + authorizationType);
          }
          if (list.size() > 1) {
            // For the moment it is only supported to have one HttpAuthorizationProvider instance
            // per authorization type
            throw new ExternalSystemConfigurationError(
                "Found multiple HTTP authorization providers for method " + authorizationType);
          }
          return list.get(0);
        }));
    provider.init(httpConfig);
    return provider;
  }

  @Override
  protected Operation getDefaultOperation() {
    switch (requestMethod.toUpperCase()) {
      case "DELETE":
        return Operation.DELETE;
      case "GET":
        return Operation.READ;
      case "POST":
        return Operation.CREATE;
      case "PUT":
        return Operation.UPDATE;
      default:
        throw new OBException("Unsupported HTTP request method " + requestMethod);
    }
  }

  @Override
  public CompletableFuture<ExternalSystemResponse> send(Operation operation,
      Supplier<? extends InputStream> payloadSupplier, String path,
      Map<String, Object> configuration) {
    log.trace("Sending {} request to URL {} of external system {}", operation, url, getName());
    switch (operation) {
      case DELETE:
        if (payloadSupplier != null) {
          throw new OBException("DELETE requests do not accept a payload");
        }
        return sendRequest(getDeleteRequestSupplier(path, configuration));
      case READ:
        if (payloadSupplier != null) {
          throw new OBException("GET requests do not accept a payload");
        }
        return sendRequest(getGetRequestSupplier(path, configuration));
      case CREATE:
        return sendRequest(getPostRequestSupplier(payloadSupplier, path, configuration));
      case UPDATE:
        return sendRequest(getPutRequestSupplier(payloadSupplier, path, configuration));
      default:
        throw new OBException("Unsupported operation " + operation);
    }
  }

  /** Internal API, this method is not private only because of testing purposes */
  CompletableFuture<ExternalSystemResponse> sendRequest(Supplier<HttpRequest> requestSupplier) {
    return sendRequestWithRetry(requestSupplier, MAX_RETRIES);
  }

  private CompletableFuture<ExternalSystemResponse> sendRequestWithRetry(
      Supplier<HttpRequest> requestSupplier, int remainingRetries) {
    HttpRequest request;
    try {
      request = requestSupplier.get();
    } catch (Exception ex) {
      log.error("Error building the HTTP request to {}", url, ex);
      return CompletableFuture.failedFuture(ex);
    }
    long requestStartTime = System.currentTimeMillis();
    return client.sendAsync(request, BodyHandlers.ofString()).thenComposeAsync(response -> {
      boolean retry = false;
      if (!isSuccessfulResponse(response) && remainingRetries > 0) {
        retry = authorizationProvider.handleRequestRetry(response.statusCode());
      }
      if (retry) {
        return sendRequestWithRetry(requestSupplier, remainingRetries - 1);
      }
      return CompletableFuture.completedFuture(response)
          .thenApply(this::buildResponse)
          .orTimeout(timeout, TimeUnit.SECONDS);

      // Executor is required to be provided here because of how the HttpClient is implemented, it
      // allows running the request on the provided executor service, but it doesn't return a
      // CompletableFuture with the same executor service, so we need to provide it again here in
      // the thenComposeAsync.
    }, NonBlockingExecutorServiceProvider.getExecutorService())
        .exceptionally(this::buildErrorResponse)
        .whenComplete((response, action) -> log.debug("{} request to {} completed in {} ms",
            request.method(), url, System.currentTimeMillis() - requestStartTime));
  }

  private Supplier<HttpRequest> getDeleteRequestSupplier(String path,
      Map<String, Object> configuration) {
    return () -> getHttpRequestBuilder(path, configuration).DELETE().build();
  }

  private Supplier<HttpRequest> getGetRequestSupplier(String path,
      Map<String, Object> configuration) {
    return () -> getHttpRequestBuilder(path, configuration).GET().build();
  }

  private Supplier<HttpRequest> getPostRequestSupplier(
      Supplier<? extends InputStream> inputStreamSupplier, String path,
      Map<String, Object> configuration) {
    return () -> getHttpRequestBuilder(path, configuration)
        .POST(BodyPublishers.ofInputStream(inputStreamSupplier))
        .build();
  }

  private Supplier<HttpRequest> getPutRequestSupplier(
      Supplier<? extends InputStream> inputStreamSupplier, String path,
      Map<String, Object> configuration) {
    return () -> getHttpRequestBuilder(path, configuration)
        .PUT(BodyPublishers.ofInputStream(inputStreamSupplier))
        .build();
  }

  private HttpRequest.Builder getHttpRequestBuilder(String path,
      Map<String, Object> configuration) {
    String urlPart = path != null ? path : "";
    if (StringUtils.isNotBlank(urlPart) && !urlPart.startsWith("/")) {
      urlPart = "/" + path;
    }

    @SuppressWarnings("unchecked")
    Map<String, String> queryParams = (Map<String, String>) configuration
        .getOrDefault("queryParameters", Collections.emptyMap());

    String queryString = queryParams.entrySet()
        .stream()
        .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
            + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));

    if (StringUtils.isNotBlank(queryString)) {
      queryString = "?" + queryString;
    }

    HttpRequest.Builder builder = HttpRequest.newBuilder()
        .uri(URI.create(url + urlPart + queryString))
        .timeout(Duration.ofSeconds(timeout))
        .header("Content-Type",
            (String) configuration.getOrDefault("Content-Type", "application/json"));

    if (authorizationProvider instanceof HttpAuthorizationRequestHeaderProvider) {
      ((HttpAuthorizationRequestHeaderProvider) authorizationProvider).getHeaders()
          .entrySet()
          .stream()
          .forEach(entry -> builder.header(entry.getKey(), entry.getValue()));
    }
    return builder;
  }

  private ExternalSystemResponse buildResponse(HttpResponse<String> response) {
    long buildResponseStartTime = System.currentTimeMillis();
    if (isSuccessfulResponse(response)) {
      ExternalSystemResponse externalSystemResponse = ExternalSystemResponseBuilder.newBuilder()
          .withData(parseBody(response.body()))
          .withStatusCode(response.statusCode())
          .withType(Type.SUCCESS)
          .build();
      log.trace("HTTP successful response processed in {} ms",
          () -> (System.currentTimeMillis() - buildResponseStartTime));
      return externalSystemResponse;
    }
    Object error = parseBody(response.body());
    ExternalSystemResponse externalSystemResponse = ExternalSystemResponseBuilder.newBuilder()
        .withError(error != null ? error : "Response Status Code: " + response.statusCode())
        .withStatusCode(response.statusCode())
        .withType(Type.ERROR)
        .build();
    log.trace("HTTP error response processed in {} ms",
        () -> (System.currentTimeMillis() - buildResponseStartTime));
    return externalSystemResponse;
  }

  private boolean isSuccessfulResponse(HttpResponse<String> response) {
    return response.statusCode() >= 200 && response.statusCode() <= 299;
  }

  private Object parseBody(String body) {
    try {
      return new JSONObject(body);
    } catch (JSONException ex) {
      return body;
    }
  }

  private ExternalSystemResponse buildErrorResponse(Throwable error) {
    String errorMessage = error.getMessage();
    if (errorMessage == null && error instanceof TimeoutException) {
      log.warn("Operation exceeded the maximum {} seconds allowed", timeout, error);
      errorMessage = "Operation exceeded the maximum " + timeout + " seconds allowed";
    }
    return ExternalSystemResponseBuilder.newBuilder().withError(errorMessage, error).build();
  }

  /**
   * @return the URL that this HTTP external system communicates with
   */
  public String getURL() {
    return url;
  }

  @Override
  public void close() throws Exception {
    // We do not need to manually close anything here because the resources are automatically
    // released when the HttpClient is no longer referenced. This should happen when this instance
    // is invalidated from the ExternalSystemProvider cache.
  }
}
