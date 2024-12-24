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
package org.openbravo.test.referencedinventory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThrows;

import java.util.UUID;

import org.junit.After;
import org.junit.Test;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SequenceUtil.CalculationMethod;
import org.openbravo.erpCommon.utility.SequenceUtil.ControlDigit;
import org.openbravo.erpCommon.utility.SequenceUtil.SequenceNumberLength;
import org.openbravo.materialmgmt.refinventory.ContentRestriction;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryUtil.SequenceType;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventoryType;
import org.openbravo.test.documentsequence.SequenceTestUtils;

public class ReferencedInventoryTypeOrgSequenceTest extends ReferencedInventoryTest {

  /**
   * test unique sequence defined in Organization Sequence Tab for Referenced Inventory Type with
   * Sequence Type as Per Organization
   */
  @Test
  public void testReferencedInventoryTypeOrgSequenceTest() {
    final Sequence baseSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance()
            .getProxy(Organization.class, ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, null, null, null, null,
        "100", ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, false);
    OBDal.getInstance().save(baseSequence);
    final Sequence parentSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance()
            .getProxy(Organization.class, ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.SEQUENCE, baseSequence, "06", null, null,
        null, null, ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, false);
    OBDal.getInstance().save(parentSequence);

    // Create Referenced Inventory Type with Sequence Type as Per Organization
    final ReferencedInventoryType refInvType = ReferencedInventoryTestUtils
        .createReferencedInventoryType(OBDal.getInstance().getProxy(Organization.class, "0"),
            SequenceType.PER_ORGANIZATION, parentSequence, ContentRestriction.ONLY_ITEMS);
    OBDal.getInstance().save(refInvType);

    ReferencedInventoryTestUtils.createReferencedInventoryTypeOrgSeq(refInvType,
        OBDal.getInstance()
            .getProxy(Organization.class, ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID),
        parentSequence);

    Exception exception = assertThrows(Exception.class,
        () -> ReferencedInventoryTestUtils.createReferencedInventoryTypeOrgSeq(refInvType,
            OBDal.getInstance()
                .getProxy(Organization.class, ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID),
            baseSequence));
    assertThat(exception.getMessage(), containsString("ConstraintViolationException"));
  }

  /**
   * compute sequence for Referenced Inventory Type with Sequence Type : Per Organization and
   * Sequence exists with base sequence
   */
  @Test
  public void testReferencedInventoryTypeOrgSequence_DocumentSequence() {

    // create a base sequence in Spain Organization
    final Sequence baseSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance()
            .getProxy(Organization.class, ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, "0110491", null, 3821L,
        null, null, ControlDigit.MODULE10, SequenceNumberLength.FIXED, 5L, true);
    OBDal.getInstance().save(baseSequence);

    // create a parent sequence with above base sequence in Spain Organization
    final Sequence parentSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance()
            .getProxy(Organization.class, ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.SEQUENCE, baseSequence, "6", null, null,
        null, "000", ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, false);
    OBDal.getInstance().save(parentSequence);

    // Create a document sequence in USA Organization
    final Sequence usaSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance()
            .getProxy(Organization.class, ReferencedInventoryTestUtils.QA_USA_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, "6", null, 1000L, null,
        "000", ControlDigit.MODULE10, SequenceNumberLength.FIXED, 7L, true);

    // Create Referenced Inventory Type with Sequence Type as Per Organization in Main Organization
    final ReferencedInventoryType refInvType = ReferencedInventoryTestUtils
        .createReferencedInventoryType(
            OBDal.getInstance()
                .getProxy(Organization.class, ReferencedInventoryTestUtils.QA_MAIN_ORG_ID),
            SequenceType.PER_ORGANIZATION, parentSequence, ContentRestriction.ONLY_ITEMS);
    OBDal.getInstance().save(refInvType);

    // Create Referenced Inventory Type Organization Sequence with Parent Sequence created above in
    // Spain Organization
    ReferencedInventoryTestUtils.createReferencedInventoryTypeOrgSeq(refInvType,
        OBDal.getInstance()
            .getProxy(Organization.class, ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID),
        parentSequence);

    // Create Referenced Inventory Type Organization Sequence with Sequence created above in USA
    // Organization
    ReferencedInventoryTestUtils.createReferencedInventoryTypeOrgSeq(refInvType, OBDal.getInstance()
        .getProxy(Organization.class, ReferencedInventoryTestUtils.QA_USA_ORG_ID), usaSequence);

    // Create Referenced Inventory in Spain Organization
    final ReferencedInventory refInvSpain = ReferencedInventoryTestUtils
        .createReferencedInventory(ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID, refInvType);
    assertThat("Referenced Inventory Search Key is not computed properly using DAL",
        "601104910382120002", equalTo(refInvSpain.getSearchKey()));

    // Create Referenced Inventory in USA Organization
    final ReferencedInventory refInvUSA = ReferencedInventoryTestUtils
        .createReferencedInventory(ReferencedInventoryTestUtils.QA_USA_ORG_ID, refInvType);
    assertThat("Referenced Inventory Search Key is not computed properly using DAL", "600010000009",
        equalTo(refInvUSA.getSearchKey()));

  }

  /**
   * roll back after the tests are executed in order to cleanup the data created by tests, this way
   * failing unique constraint for ad_client_id, search key in referenced inventory could be
   * avoided.
   */
  @After
  public void cleanup() {
    OBDal.getInstance().rollbackAndClose();
  }
}
