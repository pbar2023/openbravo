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
 * All portions are Copyright (C) 2015-2022 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.base.weld.test.testinfrastructure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import org.jboss.arquillian.junit.InSequence;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.test.event.ObserverBaseTest;
import org.openbravo.client.application.test.event.OrderLineTestObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.geography.Country;
import org.openbravo.test.base.Issue;
import org.openbravo.test.base.TestConstants;

/**
 * Persistance observers require of cdi. Test cases covering observers are executed when using
 * WeldBaseTest.
 * 
 * @author alostale
 *
 */
public class DalPersistanceEventTest extends ObserverBaseTest {

  @Test
  @InSequence(1)
  public void beginTrxObserversShouldBeExecutedOnFirstTest() {
    assertThat("begin transaction observer executions",
        OrderLineTestObserver.getNumberOfStartedTrxs(), is(1));
  }

  @Test
  @InSequence(2)
  public void beginTrxObserversShouldBeExecutedOnSubsequentTests() {
    assertThat("begin transaction observer executions",
        OrderLineTestObserver.getNumberOfStartedTrxs(), is(1));
  }

  @Test
  @InSequence(3)
  public void endTrxObserversShouldBeExecuted() {
    int initiallyClosedTrxs = OrderLineTestObserver.getNumberOfClosedTrxs();

    OBDal.getInstance().commitAndClose();

    assertThat("initial end transaction observer executions", initiallyClosedTrxs, is(0));
    assertThat("end transaction observer executions", OrderLineTestObserver.getNumberOfClosedTrxs(),
        is(1));
  }

  @Test
  @InSequence(4)
  public void persistanceObserversShouldBeExecuted() {
    try {
      setSystemAdministratorContext();
      Country newCountry = OBProvider.getInstance().get(Country.class);
      newCountry.setName("Wonderland");
      newCountry.setISOCountryCode("WL");
      newCountry.setAddressPrintFormat("-");

      newCountry.setDateformat("invalid date format");

      // expecting exception thrown by by persistance observer, it will be thrown only if it is
      // executed
      OBException thrown = assertThrows(OBException.class, () -> {
        OBDal.getInstance().save(newCountry);
        OBDal.getInstance().flush();
      });
      assertThat(thrown.getMessage(),
          containsString(OBMessageUtils.messageBD("InvalidDateFormat")));
    } finally {
      OBDal.getInstance().rollbackAndClose();
    }
  }

  @Test
  @InSequence(5)
  @Issue("45341")
  public void persistenceObserversShouldBeExecutedOnModifiedCreatedBy() {
    try {
      setSystemAdministratorContext();
      Country newCountry = OBProvider.getInstance().get(Country.class);
      newCountry.setName("Wonderland");
      newCountry.setISOCountryCode("WL");
      newCountry.setAddressPrintFormat("-");

      // Set createdBy user id manually
      User user = OBDal.getInstance().get(User.class, TestConstants.Users.OPENBRAVO);
      newCountry.setCreatedBy(user);
      OBInterceptor.setPreventUpdateInfoChange(true);
      OBDal.getInstance().save(newCountry);
      OBDal.getInstance().flush();

      // createdBy user should be persisted and should be different than updatedBy value
      String createdById = newCountry.getCreatedBy().getId();
      String updatedById = newCountry.getUpdatedBy().getId();
      assertEquals("createdBy value is not the one assigned manually.", user.getId(), createdById);
      assertEquals("updatedBy value has not been assigned using OBContext user.",
          OBContext.getOBContext().getUser().getId(), updatedById);
      assertNotEquals("createdBy and updatedBy are equal, those should be different.", createdById,
          updatedById);
    } finally {
      OBDal.getInstance().rollbackAndClose();
    }
  }
}
