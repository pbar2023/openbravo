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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.openbravo.test.base.TestConstants.Orgs.MAIN;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.model.domaintype.StringEnumerateDomainType;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.Protocol;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.externalsystem.http.HttpExternalSystem;
import org.openbravo.test.base.TestConstants;

/**
 * Tests for the {@link ExternalSystemProvider} class
 */
public class ExternalSystemProviderTest extends WeldBaseTest {

  @Inject
  private ExternalSystemProvider externalSystemProvider;

  private ExternalSystemData externalSystemData;

  @Before
  public void createExternalSystem() {
    externalSystemData = OBProvider.getInstance().get(ExternalSystemData.class);
    externalSystemData.setOrganization(OBDal.getInstance().getProxy(Organization.class, MAIN));
    externalSystemData.setName("Test");
    externalSystemData.setSearchKey("Test");
    Protocol httpProtocol = OBDal.getInstance()
        .getProxy(Protocol.class, TestConstants.Protocols.HTTP);
    externalSystemData.setProtocol(httpProtocol);
    OBDal.getInstance().save(externalSystemData);

    HttpExternalSystemData httpExternalSystemData = OBProvider.getInstance()
        .get(HttpExternalSystemData.class);
    httpExternalSystemData.setOrganization(OBDal.getInstance().getProxy(Organization.class, MAIN));
    httpExternalSystemData.setURL("https://dummy");
    httpExternalSystemData.setAuthorizationType("NOAUTH");
    httpExternalSystemData.setExternalSystem(externalSystemData);
    httpExternalSystemData.setActive(true);
    externalSystemData.getExternalSystemHttpList().add(httpExternalSystemData);
    OBDal.getInstance().save(httpExternalSystemData);

    OBDal.getInstance().flush();

    // Add new authorization type testing values into its corresponding list reference
    StringEnumerateDomainType authTypeDomainType = (StringEnumerateDomainType) httpExternalSystemData
        .getEntity()
        .getProperty("authorizationType")
        .getDomainType();
    authTypeDomainType.addEnumerateValue("TEST");
  }

  @After
  public void cleanUp() {
    OBDal.getInstance().rollbackAndClose();
  }

  @Test
  public void getExternalSystemForHttpProtocol() {
    ExternalSystem externalSystem = externalSystemProvider.getExternalSystem(externalSystemData)
        .orElseThrow();

    assertThat(externalSystem, instanceOf(HttpExternalSystem.class));
  }

  @Test
  public void getExternalSystemForHttpProtocolById() {
    ExternalSystem externalSystem = externalSystemProvider
        .getExternalSystem(externalSystemData.getId())
        .orElseThrow();

    assertThat(externalSystem, instanceOf(HttpExternalSystem.class));
  }

  @Test
  public void getExternalSystemForHttpProtocolBySearchKey() {
    ExternalSystem externalSystem = externalSystemProvider
        .getExternalSystemBySearchKey(externalSystemData.getSearchKey())
        .orElseThrow();

    assertThat(externalSystem, instanceOf(HttpExternalSystem.class));
  }

  @Test
  public void cannotGetExternalSystemForInactiveConfiguration() {
    setConfigurationActive(false);
    assertThat(externalSystemProvider.getExternalSystem(externalSystemData).isEmpty(),
        equalTo(true));
  }

  @Test
  public void cannotGetUnknownExternalSystem() {
    assertThat(externalSystemProvider.getExternalSystem("UNKNOWN_ID").isEmpty(), equalTo(true));
  }

  @Test
  public void cannotGetExternalSystemForUnknownProtocol() {
    Protocol unknownProtocol = createTestProtocol();
    setConfigurationProtocol(unknownProtocol);

    assertThat(externalSystemProvider.getExternalSystem(externalSystemData).isEmpty(),
        equalTo(true));
  }

  @Test
  public void cannotGetHttpExternalSystemForUnknownAuthorizationType() {
    setHttpConfigurationAuthorizationType("TEST");

    assertThat(externalSystemProvider.getExternalSystem(externalSystemData).isEmpty(),
        equalTo(true));
  }

  @Test
  public void providedInstanceIsUpdated() {
    ExternalSystem externalSystem = externalSystemProvider.getExternalSystem(externalSystemData)
        .orElseThrow();
    assertThat(externalSystem.getName(), equalTo("Test"));
    assertThat(((HttpExternalSystem) externalSystem).getURL(), equalTo("https://dummy"));

    // get configuration again (this time from cache)
    externalSystem = externalSystemProvider.getExternalSystem(externalSystemData).orElseThrow();
    assertThat(externalSystem.getName(), equalTo("Test"));
    assertThat(((HttpExternalSystem) externalSystem).getURL(), equalTo("https://dummy"));

    // update configuration
    setConfigurationName("New Name");
    externalSystem = externalSystemProvider.getExternalSystem(externalSystemData).orElseThrow();
    assertThat(externalSystem.getName(), equalTo("New Name"));

    // update child HTTP configuration
    setHttpConfigurationURL("https://new");
    externalSystem = externalSystemProvider.getExternalSystem(externalSystemData).orElseThrow();
    assertThat(((HttpExternalSystem) externalSystem).getURL(), equalTo("https://new"));

    // deactivate child HTTP configuration
    setHttpConfigurationActive(false);
    assertThat(externalSystemProvider.getExternalSystem(externalSystemData).isPresent(),
        equalTo(false));

    // reactivate child HTTP configuration
    setHttpConfigurationActive(true);
    assertThat(externalSystemProvider.getExternalSystem(externalSystemData).isPresent(),
        equalTo(true));

    // remove child HTTP configuration
    deleteHttpConfiguration();
    assertThat(externalSystemProvider.getExternalSystem(externalSystemData).isPresent(),
        equalTo(false));
  }

  private void setConfigurationActive(boolean isActive) {
    externalSystemData.setActive(isActive);
    OBDal.getInstance().flush();
  }

  private void setConfigurationName(String name) {
    externalSystemData.setName(name);
    OBDal.getInstance().flush();
  }

  private void setConfigurationProtocol(Protocol protocol) {
    externalSystemData.setProtocol(protocol);
    OBDal.getInstance().flush();
  }

  private Protocol createTestProtocol() {
    try {
      OBContext.setAdminMode(false);
      setCoreInDevelopment(true);
      Protocol protocol = OBProvider.getInstance().get(Protocol.class);
      protocol.setClient(OBDal.getInstance().getProxy(Client.class, TestConstants.Clients.SYSTEM));
      protocol.setOrganization(
          OBDal.getInstance().getProxy(Organization.class, TestConstants.Orgs.MAIN));
      protocol.setModule(OBDal.getInstance().getProxy(Module.class, TestConstants.Modules.ID_CORE));
      protocol.setSearchKey("UNKNOWN");
      protocol.setName("UNKNOWN");
      protocol.setProtocolType("BE");
      OBDal.getInstance().save(protocol);
      OBDal.getInstance().flush();
      return protocol;
    } finally {
      setCoreInDevelopment(false);
      OBContext.restorePreviousMode();
    }
  }

  private void setCoreInDevelopment(boolean inDevelopment) {
    OBDal.getInstance()
        .get(Module.class, TestConstants.Clients.SYSTEM)
        .setInDevelopment(inDevelopment);
    OBDal.getInstance().flush();
  }

  private void setHttpConfigurationURL(String url) {
    externalSystemData.getExternalSystemHttpList().get(0).setURL(url);
    OBDal.getInstance().flush();
  }

  private void setHttpConfigurationAuthorizationType(String authType) {
    externalSystemData.getExternalSystemHttpList().get(0).setAuthorizationType(authType);
    OBDal.getInstance().flush();
  }

  private void setHttpConfigurationActive(boolean isActive) {
    externalSystemData.getExternalSystemHttpList().get(0).setActive(isActive);
    OBDal.getInstance().flush();
  }

  private void deleteHttpConfiguration() {
    OBDal.getInstance().remove(externalSystemData.getExternalSystemHttpList().get(0));
    externalSystemData.getExternalSystemHttpList().clear();
    OBDal.getInstance().flush();
  }
}
