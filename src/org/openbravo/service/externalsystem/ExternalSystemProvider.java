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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.cache.Cacheable;
import org.openbravo.cache.TimeInvalidatedCache;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

/**
 * Provides {@ExternalSystem} instances used to communicate with different external systems and that
 * are kept in an in memory cache to favor its reuse. Note that for a given
 * {@ExternalSystemData} configuration it always retrieves the same {@ExternalSystem} instance.
 */
@ApplicationScoped
public class ExternalSystemProvider {
  private static final Logger log = LogManager.getLogger();

  private TimeInvalidatedCache<String, ExternalSystem> configuredExternalSystems;
  private Map<String, Boolean> cacheableProtocols = new ConcurrentHashMap<>();

  @PostConstruct
  private void init() {
    configuredExternalSystems = TimeInvalidatedCache.newBuilder()
        .name("External System")
        .expireAfterDuration(Duration.ofMinutes(10))
        .removalListener((entry, reason) -> {
          try {
            ((ExternalSystem) entry.getValue()).close();
            log.trace("External system {} closed, reason: {}", entry.getKey(), reason);
          } catch (Exception ex) {
            log.error("Error closing external system {}", entry.getKey(), ex);
          }
        })
        .build(this::getConfiguredExternalSystemInstance);
  }

  private ExternalSystem getConfiguredExternalSystemInstance(String externalSystemDataId) {
    try {
      OBContext.setAdminMode(true);
      ExternalSystemData externalSystemData = OBDal.getInstance()
          .get(ExternalSystemData.class, externalSystemDataId);
      ExternalSystem externalSystem = getExternalSystemInstance(externalSystemData);
      if (externalSystem == null) {
        return null;
      }
      externalSystem.configure(externalSystemData);
      return externalSystem;
    } catch (Exception ex) {
      log.error("Could not configure an external system with configuration {}",
          externalSystemDataId, ex);
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private ExternalSystem getExternalSystemInstance(ExternalSystemData externalSystemData) {
    String protocol = externalSystemData.getProtocol().getSearchKey();
    List<ExternalSystem> externalSystems = WeldUtils.getInstances(ExternalSystem.class,
        new ProtocolSelector(protocol));

    if (externalSystems.size() > 1) {
      // For the moment it is only supported to have one ExternalSystem instance per
      // protocol
      throw new OBException("Found multiple external systems for protocol " + protocol);
    }

    if (externalSystems.isEmpty()) {
      return null;
    }

    return externalSystems.get(0);
  }

  /**
   * Retrieves the {@link ExternalSystem} instance configured with the {@link ExternalSystemData}
   * whose ID is received as parameter
   * 
   * @param externalSystemDataId
   *          The ID of the {@link ExternalSystemData} that contains the configuration data
   * 
   * @return an Optional with the external system instance or an empty Optional in case it is not
   *         possible to create it for example due to a configuration problem or because an external
   *         system configuration with the provided ID cannot be found or is not active
   */
  public Optional<ExternalSystem> getExternalSystem(String externalSystemDataId) {
    ExternalSystemData configuration = OBDal.getInstance()
        .get(ExternalSystemData.class, externalSystemDataId);
    return getExternalSystem(configuration);
  }

  /**
   * Retrieves the {@link ExternalSystem} instance configured with the {@link ExternalSystemData}
   * whose search key is received as parameter
   *
   * @param searchKey
   *          The search key of the {@link ExternalSystemData} that contains the configuration data
   *
   * @return an Optional with the external system instance or an empty Optional in case it is not
   *         possible to create it for example due to a configuration problem or because an external
   *         system configuration with the provided search key cannot be found or is not active
   */
  public Optional<ExternalSystem> getExternalSystemBySearchKey(String searchKey) {
    ExternalSystemData configuration = (ExternalSystemData) OBDal.getInstance()
        .createCriteria(ExternalSystemData.class)
        .add(Restrictions.eq(ExternalSystemData.PROPERTY_SEARCHKEY, searchKey))
        .uniqueResult();
    return getExternalSystem(configuration);
  }

  /**
   * Retrieves an {@link ExternalSystem} instance configured with the data included in the provided
   * {@link ExternalSystemData}
   * 
   * @param externalSystemData
   *          The {@link ExternalSystemData} instance that contains the configuration data
   * 
   * @return an Optional with the external system instance or an empty Optional in case it is not
   *         possible to create it for example due to a configuration problem or because the
   *         provided configuration cannot be found or is not active
   */
  public Optional<ExternalSystem> getExternalSystem(ExternalSystemData externalSystemData) {
    if (externalSystemData == null || !externalSystemData.isActive()) {
      return Optional.empty();
    }
    if (isCacheable(externalSystemData)) {
      return Optional.ofNullable(configuredExternalSystems.get(externalSystemData.getId()));
    }
    // external system cannot be kept in cache: retrieve a new instance
    ExternalSystem externalSystem = getConfiguredExternalSystemInstance(externalSystemData.getId());
    return Optional.ofNullable(externalSystem);
  }

  /**
   * Checks if {@link ExternalSystem} instance that is configured with the given
   * {@link ExternalSystemData} can be kept in the cache or not, i.e, if it implements the
   * {@link Cacheable} interface. This information is kept in a cache to avoid resolving the
   * dependency injection to get the external system instance every time this method is invoked.
   * Note that we can use the protocol as the key of the cache entries because for the moment it is
   * only supported to have one {@link ExternalSystem} class per protocol.
   *
   * @return true if the external system can be kept in the cache or false otherwise.
   */
  private boolean isCacheable(ExternalSystemData externalSystemData) {
    try {
      OBContext.setAdminMode(true);
      return cacheableProtocols.computeIfAbsent(externalSystemData.getProtocol().getId(),
          k -> getExternalSystemInstance(externalSystemData) instanceof Cacheable);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Removes from the provider the cached {@link ExternalSystem} instance that is configured with
   * the {@link ExternalSystemData} whose ID is received as parameter
   *
   * @param externalSystemId
   *          The ID of the {@link ExternalSystemData}
   */
  public void invalidateExternalSystem(String externalSystemId) {
    configuredExternalSystems.invalidate(externalSystemId);
  }
}
