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
package org.openbravo.service.json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.model.domaintype.OrganizationDateTimeDomainType;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.test.base.Issue;
import org.openbravo.test.base.TestConstants;

/**
 * Tests to cover the generation of organization time zone based fields done by the
 * {@link DataToJsonConverter}
 */
public class OrganizationTimeZoneTest extends WeldBaseTest {

  @Before
  public void init() {
    setOrganizationTimeZone(TestConstants.Orgs.FB_GROUP, "America/Chicago");
    setOrganizationTimeZone(TestConstants.Orgs.ESP_NORTE, "Europe/Madrid");
    OBDal.getInstance().flush();
    OBContext.getOBContext().getOrganizationStructureProvider().reInitialize();
  }

  private void setOrganizationTimeZone(String orgId, String timeZone) {
    OBDal.getInstance().get(Organization.class, orgId).setTimezone(timeZone);
  }

  @After
  public void cleanUp() {
    OBDal.getInstance().rollbackAndClose();
  }

  @Test
  public void calculateFields() throws JSONException {
    Invoice invoice = getInvoice(TestConstants.Orgs.FB_GROUP);

    JSONObject json = getDataToJsonConverter().toJsonObject(invoice, DataResolvingMode.FULL);

    assertThat("Expected orgCreationDate", json.getString("orgCreationDate"),
        equalTo("10-01-2024 05:30:20 (America/Chicago)"));
    assertThat("Expected orgUpdatedDate", json.getString("orgUpdatedDate"),
        equalTo("12-01-2024 10:40:30 (America/Chicago)"));
  }

  @Test
  public void fieldsAreEmptyIfOrgTimezoneIsNotSet() throws JSONException {
    Invoice invoice = getInvoice(TestConstants.Orgs.US);

    JSONObject json = getDataToJsonConverter().toJsonObject(invoice, DataResolvingMode.FULL);

    assertThat("Empty orgCreationDate", json.getString("orgCreationDate"), equalTo(""));
    assertThat("Empty orgUpdatedDate", json.getString("orgUpdatedDate"), equalTo(""));
  }

  @Test
  public void doNotCalculateFieldsIfConverterShouldNotDisplayThem() {
    Invoice invoice = getInvoice(TestConstants.Orgs.FB_GROUP);

    DataToJsonConverter toJsonConverter = getDataToJsonConverter();
    toJsonConverter.setShouldDisplayOrgDate(false);
    JSONObject json = toJsonConverter.toJsonObject(invoice, DataResolvingMode.FULL);

    assertThat("orgCreationDate is not calculated", json.has("orgCreationDate"), equalTo(false));
    assertThat("orgUpdatedDate is not calculated", json.has("orgUpdatedDate"), equalTo(false));
  }

  @Test
  @Issue("54285")
  public void calculateFieldsWithException() throws JSONException {
    Order order = getOrder(TestConstants.Orgs.ESP, TestConstants.Orgs.ESP_NORTE);

    JSONObject json = getDataToJsonConverter().toJsonObject(order, DataResolvingMode.FULL);

    assertThat("Expected orgCreationDate", json.getString("orgCreationDate"),
        equalTo("10-01-2024 12:30:20 (Europe/Madrid)"));
    assertThat("Expected orgUpdatedDate", json.getString("orgUpdatedDate"),
        equalTo("12-01-2024 17:40:30 (Europe/Madrid)"));
  }

  @Test
  @Issue("54339")
  public void calculateOrgTimeZoneReferenceField() throws JSONException {
    Invoice invoice = getInvoice(TestConstants.Orgs.FB_GROUP);
    String propertyName = "computedOrgTimeZoneCreatedColumn";
    BaseOBObject bob = prepareBOBWithOrgDateTimeProperty(invoice, propertyName);

    JSONObject json = getDataToJsonConverter().toJsonObject(bob, DataResolvingMode.FULL);

    assertThat("Expected " + propertyName, json.getString(propertyName),
        equalTo("10-01-2024 05:30:20 (America/Chicago)"));
  }

  @Test
  @Issue("54339")
  public void calculateOrgTimeZoneReferenceFieldWithException() throws JSONException {
    Order order = getOrder(TestConstants.Orgs.ESP, TestConstants.Orgs.ESP_NORTE);
    String propertyName = "computedOrgTimeZoneCreatedColumn";
    BaseOBObject bob = prepareBOBWithOrgDateTimeProperty(order, propertyName);

    JSONObject json = getDataToJsonConverter().toJsonObject(bob, DataResolvingMode.FULL);

    assertThat("Expected " + propertyName, json.getString(propertyName),
        equalTo("10-01-2024 12:30:20 (Europe/Madrid)"));
  }

  @Test
  public void doNotAllowOrgTimeZoneReferenceColumnsWithouSqllogic() {
    OBException exceptionRule = assertThrows(OBException.class,
        () -> createColumnWithOrgDateTimeReference());
    assertThat(exceptionRule.getMessage(), equalTo(
        "The reference Organization DateTime must be used with a computed column (Sqllogic cannot be empty)."));
  }

  private Column createColumnWithOrgDateTimeReference() {
    OBContext.setAdminMode(false);
    try {
      Module module = OBDal.getInstance().get(Module.class, "0");
      module.setInDevelopment(true);
      Column column = OBProvider.getInstance().get(Column.class);
      column.setActive(true);
      column.setClient(OBDal.getInstance().getProxy(Client.class, TestConstants.Clients.SYSTEM));
      column.setOrganization(
          OBDal.getInstance().getProxy(Organization.class, TestConstants.Orgs.MAIN));
      column.setTable(OBDal.getInstance().getProxy(Table.class, TestConstants.Tables.C_ORDER));
      column.setName("testingColumn");
      column.setDBColumnName("testingColumns");
      column.setModule(module);
      column.setReference((Reference) OBDal.getInstance()
          .getProxy(Reference.ENTITY_NAME, "F8428F177B6146D3A13C4830FB87DE49"));
      OBDal.getInstance().save(column);
      return column;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private BaseOBObject prepareBOBWithOrgDateTimeProperty(BaseOBObject bob, String propertyName) {
    Property property = mock(Property.class);
    when(property.isPrimitive()).thenReturn(true);
    when(property.getDomainType()).thenReturn(new OrganizationDateTimeDomainType());
    when(property.getName()).thenReturn(propertyName);
    doReturn(Date.class).when(property).getPrimitiveObjectType();

    Entity entity = spy(ModelProvider.getInstance().getEntity(bob.getEntityName()));
    List<Property> properties = new ArrayList<>(entity.getProperties());
    properties.add(property);
    when(entity.getProperties()).thenReturn(properties);

    BaseOBObject bobSpy = spy(bob);
    when(bobSpy.getEntity()).thenReturn(entity);
    doReturn(getCreationDate()).when(bobSpy).get(propertyName);

    return bobSpy;
  }

  private DataToJsonConverter getDataToJsonConverter() {
    DataToJsonConverter toJsonConverter = OBProvider.getInstance().get(DataToJsonConverter.class);
    toJsonConverter.setShouldDisplayOrgDate(true);
    toJsonConverter.setOrganizationStructureProvider(
        OBContext.getOBContext().getOrganizationStructureProvider());
    return toJsonConverter;
  }

  private Invoice getInvoice(String orgId) {
    Invoice invoice = OBProvider.getInstance().get(Invoice.class);
    invoice.setOrganization(OBDal.getInstance().getProxy(Organization.class, orgId));
    invoice.setCreationDate(getCreationDate());
    invoice.setUpdated(getUpdationDate());
    return invoice;
  }

  private Order getOrder(String orgId, String trxOrgId) {
    Order order = OBProvider.getInstance().get(Order.class);
    order.setOrganization(OBDal.getInstance().getProxy(Organization.class, orgId));
    order.setTrxOrganization(OBDal.getInstance().getProxy(Organization.class, trxOrgId));
    order.setCreationDate(getCreationDate());
    order.setUpdated(getUpdationDate());
    return order;
  }

  private Date getCreationDate() {
    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    calendar.set(Calendar.YEAR, 2024);
    calendar.set(Calendar.MONTH, 0);
    calendar.set(Calendar.DAY_OF_MONTH, 10);
    calendar.set(Calendar.HOUR_OF_DAY, 11);
    calendar.set(Calendar.MINUTE, 30);
    calendar.set(Calendar.SECOND, 20);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTime();
  }

  private Date getUpdationDate() {
    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    calendar.set(Calendar.YEAR, 2024);
    calendar.set(Calendar.MONTH, 0);
    calendar.set(Calendar.DAY_OF_MONTH, 12);
    calendar.set(Calendar.HOUR_OF_DAY, 16);
    calendar.set(Calendar.MINUTE, 40);
    calendar.set(Calendar.SECOND, 30);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTime();
  }
}
