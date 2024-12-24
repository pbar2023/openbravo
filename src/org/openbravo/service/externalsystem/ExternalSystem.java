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
 * All portions are Copyright (C) 2022-2023 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.externalsystem;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import javax.enterprise.context.Dependent;

/**
 * Used to define the communication with an external system. Classes extending this class must be
 * annotated with {@link Protocol} to declare the communication protocol it uses.
 * 
 * The {@ExternalSystemProvider} class must be used to retrieve instances of this class. It is
 * expected that the classes extending this one to be defined as {@link Dependent} scoped.
 */
@Dependent
public abstract class ExternalSystem implements AutoCloseable {

  private String name;
  private String searchKey;

  /**
   * Operations that can be applied in the external system when sending the information
   */
  public enum Operation {
    CREATE, READ, UPDATE, DELETE
  }

  /**
   * Sends information to the external system
   *
   * @param operation
   *          The operation to be applied in the external system with the sent information
   * @param payloadSupplier
   *          A supplier of the input stream with the data to be sent
   * @param path
   *          An optional sequence of segments separated by a slash (/) that be appended to the base
   *          external system URI where the information will be eventually sent. It can be null if
   *          not needed.
   * @param configuration
   *          Additional information used to configure the send operation
   *
   * @return a CompletableFuture<ExternalSystemResponse> containing the response data coming from
   *         the external system
   */
  public abstract CompletableFuture<ExternalSystemResponse> send(Operation operation,
      Supplier<? extends InputStream> payloadSupplier, String path,
      Map<String, Object> configuration);

  /**
   * Sends information to the external system with the default operation.
   * 
   * @see #getDefaultOperation
   *
   * @param payloadSupplier
   *          A supplier of the input stream with the data to be sent
   *
   * @return a CompletableFuture<ExternalSystemResponse> containing the response data coming from
   *         the external system
   */
  public final CompletableFuture<ExternalSystemResponse> send(
      Supplier<? extends InputStream> payloadSupplier) {
    return send(getDefaultOperation(), payloadSupplier, null, Collections.emptyMap());
  }

  /**
   * @return the operation used by default when invoking the {@link #send(Supplier)} method
   */
  protected Operation getDefaultOperation() {
    return Operation.CREATE;
  }

  /**
   * Sends information to the external system using the provided operation, payload supplier and
   * path but without using additional configuration.
   *
   * @see #send(Operation, Supplier, String, Map)
   */
  public final CompletableFuture<ExternalSystemResponse> send(Operation operation,
      Supplier<? extends InputStream> payloadSupplier, String path) {
    return send(operation, payloadSupplier, path, Collections.emptyMap());
  }

  /**
   * Sends information to the external system using the provided operation and payload supplier but
   * without using a path nor additional configuration.
   *
   * @see #send(Operation, Supplier, String)
   */
  public final CompletableFuture<ExternalSystemResponse> send(Operation operation,
      Supplier<? extends InputStream> payloadSupplier) {
    return send(operation, payloadSupplier, null);
  }

  /**
   * Sends information to the external system using the provided operation and path but without
   * using a payload supplier nor additional configuration.
   *
   * @see #send(Operation, String, Map)
   */
  public final CompletableFuture<ExternalSystemResponse> send(Operation operation, String path) {
    return send(operation, path, Collections.emptyMap());
  }

  /**
   * Sends information to the external system using the provided operation and configuration but
   * without using a payload supplier nor a path.
   *
   * @see #send(Operation, String, Map)
   */
  public final CompletableFuture<ExternalSystemResponse> send(Operation operation,
      Map<String, Object> configuration) {
    return send(operation, null, configuration);
  }

  /**
   * Sends information to the external system using the provided operation, path and configuration
   * but without using a payload supplier.
   *
   * @see #send(Operation, Supplier, String, Map)
   */
  public final CompletableFuture<ExternalSystemResponse> send(Operation operation, String path,
      Map<String, Object> configuration) {
    return send(operation, null, path, configuration);
  }

  /**
   * Configures the external system instance with the provided configuration. The extensions of this
   * class must use this method to initialize their own configuration fields.
   * 
   * @param configuration
   *          Provides the configuration data of the external system
   * @throws ExternalSystemConfigurationError
   *           in case the external system cannot be properly configured
   */
  protected void configure(ExternalSystemData configuration) {
    name = configuration.getName();
    searchKey = configuration.getSearchKey();
  }

  /**
   * @return the name of the external system
   */
  protected String getName() {
    return name;
  }

  /**
   * @return the search key of the external system
   */
  protected String getSearchKey() {
    return searchKey;
  }
}
