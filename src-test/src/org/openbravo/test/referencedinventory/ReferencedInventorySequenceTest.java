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
 * All portions are Copyright (C) 2018-2024 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.referencedinventory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.UUID;

import org.junit.Test;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SequenceUtil.CalculationMethod;
import org.openbravo.erpCommon.utility.SequenceUtil.ControlDigit;
import org.openbravo.erpCommon.utility.SequenceUtil.SequenceNumberLength;
import org.openbravo.materialmgmt.refinventory.ContentRestriction;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryUtil;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryUtil.SequenceType;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventoryType;
import org.openbravo.test.documentsequence.SequenceTestUtils;

/**
 * Test referenced inventory type sequence is properly used
 */
public class ReferencedInventorySequenceTest extends ReferencedInventoryTest {

  @Test
  public void testReferencedInventorySequenceIsUsed() {
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance()
            .getProxy(Organization.class, ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, null, 1L, 1000000L, 1L,
        null, ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, true);

    final ReferencedInventoryType refInvType = ReferencedInventoryTestUtils
        .createReferencedInventoryType(sequence.getOrganization(), SequenceType.GLOBAL, sequence,
            ContentRestriction.ONLY_ITEMS);
    OBDal.getInstance().save(refInvType);
    Long currentSequenceNumber = sequence.getNextAssignedNumber();

    final ReferencedInventory refInv = ReferencedInventoryTestUtils
        .createReferencedInventory(ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID, refInvType);
    assertThat("Referenced Inventory Search Key is taken from sequence", refInv.getSearchKey(),
        equalTo(Long.toString(currentSequenceNumber)));

    final ReferencedInventory refInv2 = ReferencedInventoryTestUtils
        .createReferencedInventory(ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID, refInvType);
    assertThat("Referenced Inventory Search Key is updated from sequence", refInv2.getSearchKey(),
        equalTo(Long.toString(currentSequenceNumber + 1)));
  }

  @Test
  public void testReferenceInventorySequenceUsingModule10_a() {
    testModule10(2821L, "6", "000", "0110491", null, SequenceNumberLength.FIXED, 5L,
        SequenceNumberLength.VARIABLE, null, false, "601104910282130002");
  }

  @Test
  public void testReferenceInventorySequenceUsingModule10_b() {
    testModule10(2821L, "6", "001", "0110491", null, SequenceNumberLength.FIXED, 5L,
        SequenceNumberLength.VARIABLE, null, false, "601104910282130019");
  }

  @Test
  public void testReferenceInventorySequenceUsingModule10_c() {
    testModule10(2822L, "6", "248", "0110491", null, SequenceNumberLength.FIXED, 5L,
        SequenceNumberLength.VARIABLE, null, false, "601104910282202488");
  }

  @Test
  public void testReferenceInventorySequenceUsingModule10_d() {
    // Parent sequence prefix is empty or null, Child Sequence suffix is empty or null
    testModule10(2822L, null, "248", "0110491", null, SequenceNumberLength.FIXED, 5L,
        SequenceNumberLength.VARIABLE, null, false, "01104910282202486");
  }

  @Test
  public void testReferenceInventorySequenceUsingModule10_e() {
    // Parent sequence prefix & suffix is empty or null, Child Sequence suffix is empty or null
    testModule10(2822L, null, null, "0110491", null, SequenceNumberLength.FIXED, 5L,
        SequenceNumberLength.VARIABLE, null, false, "01104910282200");
  }

  @Test
  public void testReferenceInventorySequenceUsingModule10_f() {
    // Parent sequence prefix & suffix is empty or null, Child Sequence prefix & suffix is empty
    // or null
    testModule10(2822L, null, null, null, null, SequenceNumberLength.FIXED, 5L,
        SequenceNumberLength.VARIABLE, null, false, "0282260");
  }

  @Test
  public void testReferenceInventorySequenceUsingModule10_g() {
    // Create a child sequence with base sequence
    testModule10(2821L, "6", "000", "0110491", null, SequenceNumberLength.FIXED, 5L,
        SequenceNumberLength.FIXED, 15L, true, "60001104910282130002");
  }

  @Test
  public void testReferenceInventorySequenceUsingModule10_h() {
    // Create a child sequence with base sequence
    testModule10(2821L, "6", "000", "0110491", null, SequenceNumberLength.FIXED, 7L,
        SequenceNumberLength.FIXED, 15L, true, "60110491000282130002");
  }

  /**
   * Test sequence computation in Referenced Inventory using Module 10 algorithm with child sequence
   * having a base sequence or having no base sequence
   */
  private void testModule10(Long nextAssignedNumberChild, String parentPrefix, String parentSuffix,
      String childPrefix, String childSuffix, SequenceNumberLength childSequenceNumberLength,
      Long childSequenceLength, SequenceNumberLength parentSequenceNumberLength,
      Long parentSequenceLength, Boolean childSequenceHasBaseSequence, String expectedOutput) {
    Organization org = OBDal.getInstance()
        .getProxy(Organization.class, ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID);
    // Create child sequence with or without base sequence
    Sequence childSequence = setUpChildSequence(org, ControlDigit.MODULE10,
        CalculationMethod.AUTONUMERING, childPrefix, 1L, nextAssignedNumberChild, 1L, childSuffix,
        childSequenceNumberLength, childSequenceLength, childSequenceHasBaseSequence);

    // Create Parent Sequence with Child Sequence created above as its Base Sequence
    final Sequence parentSequence = SequenceTestUtils.createDocumentSequence(org,
        UUID.randomUUID().toString(), CalculationMethod.SEQUENCE, childSequence, parentPrefix, null,
        null, null, parentSuffix, ControlDigit.MODULE10, parentSequenceNumberLength,
        parentSequenceLength, true);

    // Create Referenced Inventory Type with Sequence Type as Per Organization
    final ReferencedInventoryType refInvType = ReferencedInventoryTestUtils
        .createReferencedInventoryType(org, SequenceType.PER_ORGANIZATION, null,
            ContentRestriction.ONLY_ITEMS);

    // Create Referenced Inventory Type Organization Sequence with Parent Sequence created Above.
    ReferencedInventoryTestUtils.createReferencedInventoryTypeOrgSeq(refInvType, org,
        parentSequence);

    String proposedSequenceUsingDal = ReferencedInventoryUtil.getProposedValueFromSequence(
        refInvType.getId(), ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID, false);
    assertThat("Referenced Inventory Search Key is computed from sequence",
        proposedSequenceUsingDal, equalTo(expectedOutput));

    // get proposed sequence using control digit & sequence computation in PL
    String proposedSequenceUsingPL = SequenceTestUtils
        .callADSequenceDocumentNo(parentSequence.getId(), false);

    assertThat("Referenced Inventory Search Key is computed from sequence using PL",
        proposedSequenceUsingPL, equalTo(expectedOutput));
  }

  /**
   * This method sets up a child sequence with or without base sequence as per input parameters
   *
   * @param org
   *          Organization in which sequence is defined
   * @param controlDigit
   *          Control Digit for the child sequence
   * @param calculationMethod
   *          Calculation Method for the child sequence
   * @param prefix
   *          Prefix for the child sequence
   * @param startingNo
   *          Starting Number for the child sequence
   * @param nextAssignedNumber
   *          Next Assigned Number for the child sequence
   * @param incrementBy
   *          Increment Child Sequence By
   * @param suffix
   *          Suffix to be appended for the child sequence
   * @param sequenceNoLength
   *          Sequence Number Length for the child Sequence - Variable or Fix Length
   * @param sequenceLength
   *          Sequence Length for child sequence in case of Fix Length
   * @param childSequenceHasBaseSequence
   *          flag to define a base sequence for the child sequence
   * @return Document sequence to be used as base sequence in the parent sequence for referenced
   *         inventory type
   */

  private static Sequence setUpChildSequence(Organization org, ControlDigit controlDigit,
      CalculationMethod calculationMethod, String prefix, Long startingNo, Long nextAssignedNumber,
      Long incrementBy, String suffix, SequenceNumberLength sequenceNoLength, Long sequenceLength,
      boolean childSequenceHasBaseSequence) {

    if (childSequenceHasBaseSequence) {
      Sequence childSequence = SequenceTestUtils.createDocumentSequence(org,
          UUID.randomUUID().toString(), calculationMethod, null, prefix, startingNo,
          nextAssignedNumber, incrementBy, suffix, controlDigit, sequenceNoLength, sequenceLength,
          true);
      return SequenceTestUtils.createDocumentSequence(org, UUID.randomUUID().toString(),
          CalculationMethod.SEQUENCE, childSequence, null, null, null, null, null,
          ControlDigit.NONE, sequenceNoLength, sequenceLength, true);
    }
    return SequenceTestUtils.createDocumentSequence(org, UUID.randomUUID().toString(),
        calculationMethod, null, prefix, startingNo, nextAssignedNumber, incrementBy, suffix,
        controlDigit, sequenceNoLength, sequenceLength, true);
  }

}
