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
 * All portions are Copyright (C) 2010-2022 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.dal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.Category;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.test.base.Issue;
import org.openbravo.test.base.OBBaseTest;
import org.openbravo.test.base.TestConstants;

/**
 * Tests the usage of the {@link OBContext#setAdminMode()} and
 * {@link OBContext#restorePreviousMode()} and how this affects to the write and org/client access
 * checks when saving/updating {@link BaseOBObject} instances.
 * 
 * @author mtaal
 */

public class AdminContextTest extends OBBaseTest {

  private static final String NO_ORG_ACCESS = "is not present in OrganizationList";
  private static final String CURRENCY_NOT_WRITABLE = "Entity Currency is not writable by this user";

  @After
  public void cleanUp() {
    OBDal.getInstance().rollbackAndClose();
  }

  /**
   * Test a single call to the admin context setting.
   */
  @Test
  @Issue("12594")
  @Issue("12660")
  public void testSingleAdminContextCall() {
    setTestUserContext();
    assertFalse(OBContext.getOBContext().isInAdministratorMode());
    OBContext.setAdminMode();
    assertTrue(OBContext.getOBContext().isInAdministratorMode());
    OBContext.restorePreviousMode();
    assertFalse(OBContext.getOBContext().isInAdministratorMode());
  }

  /**
   * Test multiple nested calls to setting and reseting admin context.
   */
  @Test
  @Issue("12594")
  @Issue("12660")
  public void testMultipleAdminContextCall() {
    setTestUserContext();
    assertFalse(OBContext.getOBContext().isInAdministratorMode());
    OBContext.setAdminMode();
    assertTrue(OBContext.getOBContext().isInAdministratorMode());

    {
      OBContext.setAdminMode();
      assertTrue(OBContext.getOBContext().isInAdministratorMode());

      {
        OBContext.setAdminMode();
        assertTrue(OBContext.getOBContext().isInAdministratorMode());

        OBContext.restorePreviousMode();
        assertTrue(OBContext.getOBContext().isInAdministratorMode());
      }

      OBContext.restorePreviousMode();
      assertTrue(OBContext.getOBContext().isInAdministratorMode());
    }

    OBContext.restorePreviousMode();
    assertFalse(OBContext.getOBContext().isInAdministratorMode());
  }

  @Test
  @Issue("50160")
  public void doNotSkipWriteAccessCheckIfBobWasNotSavedInAdminMode() {
    setUserContext(TEST2_USER_ID);

    Currency currency;
    try {
      OBContext.setAdminMode(true);
      currency = newCurrency();
    } finally {
      OBContext.restorePreviousMode();
    }

    OBSecurityException exception = assertThrows(OBSecurityException.class,
        () -> OBDal.getInstance().save(currency));
    assertThat(exception.getMessage(), equalTo(CURRENCY_NOT_WRITABLE));
  }

  @Test
  @Issue("50160")
  public void skipWriteAccessCheckIfBobWasSavedInAdminMode() {
    setUserContext(TEST2_USER_ID);

    Currency currency;
    try {
      OBContext.setAdminMode(false);
      currency = newCurrency();
      OBDal.getInstance().save(currency);
    } finally {
      OBContext.restorePreviousMode();
    }

    OBDal.getInstance().flush();

    assertNotNull(currency.getId());
  }

  @Test
  @Issue("50160")
  public void skipWriteAccessCheckOnFlushDirtyIfBobWasSavedInAdminMode() {
    setUserContext(TEST2_USER_ID);

    Currency currency;
    try {
      OBContext.setAdminMode(false);
      currency = newCurrency();
      OBDal.getInstance().save(currency);
      currency.setDescription("Testing Currency");
    } finally {
      OBContext.restorePreviousMode();
    }

    // this flush should detect dirty changes (we've updated currency after save)
    OBDal.getInstance().flush();

    String description = "";
    try {
      OBContext.setAdminMode(true);
      description = currency.getDescription();
    } finally {
      OBContext.restorePreviousMode();
    }

    assertThat(description, equalTo("Testing Currency"));
  }

  @Test
  @Issue("50160")
  public void doNotSkipOrgClientAccessCheckIfCheckWasEnabledOnSave() {
    setUserContext(TEST2_USER_ID);

    Category category;
    try {
      OBContext.setAdminMode(true);
      category = newCategory(TestConstants.Orgs.ESP);
    } finally {
      OBContext.restorePreviousMode();
    }

    OBSecurityException exception = assertThrows(OBSecurityException.class,
        () -> OBDal.getInstance().save(category));
    assertThat(exception.getMessage(), containsString(NO_ORG_ACCESS));
  }

  @Test
  @Issue("50160")
  public void skipOrgClientAccessCheckIfCheckWasDisabledOnSave() {
    setUserContext(TEST2_USER_ID);

    Category category;
    try {
      OBContext.setAdminMode(false);
      category = newCategory(TestConstants.Orgs.ESP);
      OBDal.getInstance().save(category);
      category.setDescription("Testing Category");
    } finally {
      OBContext.restorePreviousMode();
    }

    OBDal.getInstance().flush();

    assertThat(category.getDescription(), equalTo("Testing Category"));
  }

  private Currency newCurrency() {
    Currency currency = OBProvider.getInstance().get(Currency.class);
    currency.setClient(OBDal.getInstance().getProxy(Client.class, TestConstants.Clients.FB_GRP));
    currency
        .setOrganization(OBDal.getInstance().getProxy(Organization.class, TestConstants.Orgs.MAIN));
    currency.setISOCode("T");
    currency.setDescription("A currency for testing purposes");
    currency.setStandardPrecision(2L);
    currency.setCostingPrecision(4L);
    currency.setPricePrecision(0L);
    return currency;
  }

  private Category newCategory(String orgId) {
    Category category = OBProvider.getInstance().get(Category.class);
    category.setClient(OBDal.getInstance().getProxy(Client.class, TestConstants.Clients.FB_GRP));
    category.setOrganization(OBDal.getInstance().getProxy(Organization.class, orgId));
    category.setSearchKey("Test");
    category.setName("Test");
    return category;
  }
}
