/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
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
 *************************************************************************
 */
package org.openbravo.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThrows;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.domaintype.StringEnumerateDomainType;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Characteristic;
import org.openbravo.test.base.TestConstants;

/**
 * Tests the {@link CharacteristicEventHandler} class
 */
public class CharacteristicEventHandlerTest extends WeldBaseTest {

  private static final String RELEVANT_CHARACTERISTIC_1 = "Test_Color";
  private static final String RELEVANT_CHARACTERISTIC_2 = "Test_Size";
  private static final String NON_ZERO_ORG_MSG = "Characteristics linked to a relevant characteristic must be defined at * organization";
  private static final String RELEVANT_CHAR_IN_USE_MSG = "It already exists a characteristic linked to the selected relevant characteristic";

  @Before
  public void addRelevantCharacteristicEnumeratedValue() {
    StringEnumerateDomainType domain = (StringEnumerateDomainType) ModelProvider.getInstance()
        .getEntity("Characteristic")
        .getProperty("relevantCharacteristic")
        .getDomainType();
    if (!domain.getEnumerateValues().contains(RELEVANT_CHARACTERISTIC_1)) {
      domain.addEnumerateValue(RELEVANT_CHARACTERISTIC_1);
      domain.addEnumerateValue(RELEVANT_CHARACTERISTIC_2);
    }
  }

  @After
  public void cleanUp() {
    OBDal.getInstance().rollbackAndClose();
  }

  @Test
  public void cannotLinkRelevantCharacteristicToCharacteristicAtNonZeroOrg() {
    Characteristic characteristic = newCharacteristic("Color", RELEVANT_CHARACTERISTIC_1);
    characteristic
        .setOrganization(OBDal.getInstance().getProxy(Organization.class, TestConstants.Orgs.ESP));

    OBException exception = assertThrows(OBException.class,
        () -> OBDal.getInstance().save(characteristic));

    assertThat(exception.getMessage(), equalTo(NON_ZERO_ORG_MSG));
  }

  @Test
  public void cannotLinkRelevantCharacteristicToCharacteristicAtNonZeroOrgOnUpdate() {
    Characteristic characteristic = newCharacteristic("Color", RELEVANT_CHARACTERISTIC_1);
    OBDal.getInstance().save(characteristic);
    OBDal.getInstance().flush();

    characteristic
        .setOrganization(OBDal.getInstance().getProxy(Organization.class, TestConstants.Orgs.ESP));
    OBException exception = assertThrows(OBException.class, () -> OBDal.getInstance().flush());

    assertThat(exception.getMessage(), equalTo(NON_ZERO_ORG_MSG));
  }

  @Test
  public void cannotLinkRelevantCharacteristicAlreadyInUse() {
    Characteristic characteristic1 = newCharacteristic("Color1", RELEVANT_CHARACTERISTIC_1);
    OBDal.getInstance().save(characteristic1);
    OBDal.getInstance().flush();

    Characteristic characteristic2 = newCharacteristic("Color2", RELEVANT_CHARACTERISTIC_1);
    OBException exception = assertThrows(OBException.class,
        () -> OBDal.getInstance().save(characteristic2));

    assertThat(exception.getMessage(), equalTo(RELEVANT_CHAR_IN_USE_MSG));
  }

  @Test
  public void cannotLinkRelevantCharacteristicAlreadyInUseOnUpdate() {
    Characteristic characteristic1 = newCharacteristic("Color", RELEVANT_CHARACTERISTIC_1);
    OBDal.getInstance().save(characteristic1);
    Characteristic characteristic2 = newCharacteristic("Size", RELEVANT_CHARACTERISTIC_2);
    OBDal.getInstance().save(characteristic2);
    OBDal.getInstance().flush();

    characteristic2.setRelevantCharacteristic(RELEVANT_CHARACTERISTIC_1);
    OBException exception = assertThrows(OBException.class, () -> OBDal.getInstance().flush());

    assertThat(exception.getMessage(), equalTo(RELEVANT_CHAR_IN_USE_MSG));
  }

  private Characteristic newCharacteristic(String name, String relevantCharacteristic) {
    Characteristic characteristic = OBProvider.getInstance().get(Characteristic.class);
    characteristic
        .setOrganization(OBDal.getInstance().getProxy(Organization.class, TestConstants.Orgs.MAIN));
    characteristic.setName(name);
    characteristic.setRelevantCharacteristic(relevantCharacteristic);
    return characteristic;
  }
}
