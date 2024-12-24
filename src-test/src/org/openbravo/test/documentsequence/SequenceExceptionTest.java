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
package org.openbravo.test.documentsequence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceUtil;
import org.openbravo.erpCommon.utility.SequenceUtil.CalculationMethod;
import org.openbravo.erpCommon.utility.SequenceUtil.ControlDigit;
import org.openbravo.erpCommon.utility.SequenceUtil.SequenceNumberLength;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.DalConnectionProvider;

public class SequenceExceptionTest extends SequenceTest {

  /**
   * Test sequence with Calculation Method: Based On Sequence and Base Sequence: empty/null
   */
  @Test
  public void testSequenceExceptionWithCalculationMethod_Sequence() {
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.SEQUENCE, null, "0110491", null, null, null,
        null, ControlDigit.NONE, SequenceNumberLength.FIXED, 7L, false);
    OBDal.getInstance().save(sequence);
    Exception exception = assertThrows(Exception.class, () -> OBDal.getInstance().flush());
    assertThat(exception.getMessage(), containsString("ConstraintViolationException"));
  }

  /**
   * test Sequence with Calculation Method: Based On Sequence and update Base Sequence as
   * empty/null.
   */
  @Test
  public void testSequenceExceptionWithCalculationMethod_AutoNumbering() {
    final Sequence baseSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, "100", null, null, null,
        null, ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, false);
    OBDal.getInstance().save(baseSequence);
    final Sequence parentSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.SEQUENCE, baseSequence, "06", null, null,
        null, null, ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, false);
    OBDal.getInstance().save(parentSequence);
    OBDal.getInstance().flush();

    parentSequence.setBaseSequence(null);
    OBDal.getInstance().save(parentSequence);
    Exception exception = assertThrows(Exception.class, () -> OBDal.getInstance().flush());
    assertThat(exception.getMessage(), containsString("ConstraintViolationException"));
  }

  /**
   * test sequence with Sequence Number Length:Fixed and Sequence Length: 0L
   */

  @Test
  public void testSequenceExceptionWithSequenceNumberLength_Fixed_SequenceLength_0L() {
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, "0110491", null, null,
        null, null, ControlDigit.NONE, SequenceNumberLength.FIXED, 0L, false);
    OBDal.getInstance().save(sequence);
    Exception exception = assertThrows(Exception.class, () -> OBDal.getInstance().flush());
    assertThat(exception.getMessage(), containsString("ConstraintViolationException"));
  }

  /**
   * test sequence with Sequence Number Length:Fixed and Sequence Length: NULL
   */

  @Test
  public void testSequenceExceptionWithSequenceNumberLength_Fixed_SequenceLength_Empty() {
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, "0110491", null, null,
        null, null, ControlDigit.NONE, SequenceNumberLength.FIXED, null, false);
    OBDal.getInstance().save(sequence);
    Exception exception = assertThrows(Exception.class, () -> OBDal.getInstance().flush());
    assertThat(exception.getMessage(), containsString("ConstraintViolationException"));
  }

  /** Test sequence with alphanumeric Prefix and Control Digit: Module 10 */
  @Test
  public void testSequenceExceptionWithControlDigit_Module10_AlphanumericPrefix() {
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, "1A2", null, null, null,
        null, ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, false);
    OBException exception = assertThrows(OBException.class,
        () -> OBDal.getInstance().save(sequence));
    assertThat(exception.getMessage(),
        containsString(OBMessageUtils.messageBD("ValidateSequence")));
  }

  /** Test sequence with alphanumeric Suffix and Control Digit: Module 10 */
  @Test
  public void testSequenceExceptionWithControlDigit_Module10_AlphanumericSuffix() {
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, null, null, null, null,
        "1A2", ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, false);
    OBException exception = assertThrows(OBException.class,
        () -> OBDal.getInstance().save(sequence));
    assertThat(exception.getMessage(),
        containsString(OBMessageUtils.messageBD("ValidateSequence")));
  }

  /** Test parent sequence with Base Sequence having alphanumeric Prefix */
  @Test
  public void testSequenceExceptionWithControlDigit_Module10_BaseSequenceAlphanumericPrefix() {
    final Sequence baseSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, "1A2", null, null, null,
        null, ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, false);
    OBDal.getInstance().save(baseSequence);
    final Sequence parentSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.SEQUENCE, baseSequence, "06", null, null,
        null, null, ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, false);
    OBException exception = assertThrows(OBException.class,
        () -> OBDal.getInstance().save(parentSequence));
    assertThat(exception.getMessage(),
        containsString(OBMessageUtils.getI18NMessage("ValidateBaseSequenceMod10",
            new String[] { baseSequence.getIdentifier(), parentSequence.getIdentifier() })));
  }

  /** Test parent sequence with Base Sequence having alphanumeric Suffix */
  @Test
  public void testSequenceExceptionWithControlDigit_Module10_BaseSequenceAlphanumericSuffix() {
    final Sequence baseSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, null, null, null, null,
        "1A2", ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, false);
    OBDal.getInstance().save(baseSequence);
    final Sequence parentSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.SEQUENCE, baseSequence, "06", null, null,
        null, null, ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, false);
    OBException exception = assertThrows(OBException.class,
        () -> OBDal.getInstance().save(parentSequence));
    assertThat(exception.getMessage(),
        containsString(OBMessageUtils.getI18NMessage("ValidateBaseSequenceMod10",
            new String[] { baseSequence.getIdentifier(), parentSequence.getIdentifier() })));
  }

  /**
   * Test update base sequence with alphanumeric Suffix used in parent sequence with Control Digit:
   * Module 10
   */
  @Test
  public void testSequenceExceptionWithControlDigit_Module10_UpdateBaseSequenceAlphanumericSuffix() {
    final Sequence baseSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, null, null, null, null,
        "100", ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, false);
    OBDal.getInstance().save(baseSequence);
    final Sequence parentSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.SEQUENCE, baseSequence, "06", null, null,
        null, null, ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, false);
    OBDal.getInstance().save(parentSequence);
    OBDal.getInstance().flush();
    assertTrue(
        "Sequence with control digit Module 10 and base sequence with numeric suffix is not created.",
        parentSequence != null);

    baseSequence.setSuffix("1A2");
    OBDal.getInstance().save(baseSequence);
    OBException exception = assertThrows(OBException.class, () -> OBDal.getInstance().flush());
    assertThat(exception.getMessage(),
        containsString(OBMessageUtils.getI18NMessage("ValidateBaseSequenceMod10",
            new String[] { baseSequence.getIdentifier(), parentSequence.getIdentifier() })));
  }

  /**
   * Test update base sequence with alphanumeric Prefix used in parent sequence with Control Digit:
   * Module 10
   */
  @Test
  public void testSequenceExceptionWithControlDigit_Module10_UpdateBaseSequenceAlphanumericPrefix() {
    final Sequence baseSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, null, null, null, null,
        "100", ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, false);
    OBDal.getInstance().save(baseSequence);
    final Sequence parentSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.SEQUENCE, baseSequence, "06", null, null,
        null, null, ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, false);
    OBDal.getInstance().save(parentSequence);
    OBDal.getInstance().flush();
    assertTrue(
        "Sequence with control digit Module 10 and base sequence with numeric prefix is not created.",
        parentSequence != null);
    baseSequence.setPrefix("1A2");
    OBDal.getInstance().save(baseSequence);
    Exception exception = assertThrows(Exception.class, () -> OBDal.getInstance().flush());
    assertThat(exception.getMessage(),
        containsString(OBMessageUtils.getI18NMessage("ValidateBaseSequenceMod10",
            new String[] { baseSequence.getIdentifier(), parentSequence.getIdentifier() })));
  }

  /**
   * Update Parent or Base Sequence having None control digit to Module 10 with Parent or Base
   * sequence with alphanumeric suffix or prefix.
   */
  @Test
  public void testSequenceException_UpdateControlDigitModule10() {
    final Sequence baseSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, null, null, null, null,
        "1A2", ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, false);
    OBDal.getInstance().save(baseSequence);
    final Sequence parentSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.SEQUENCE, baseSequence, "100", null, null,
        null, null, ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, false);
    OBDal.getInstance().save(parentSequence);
    OBDal.getInstance().flush();

    parentSequence.setControlDigit(ControlDigit.MODULE10.value);
    OBDal.getInstance().save(parentSequence);

    OBException exception = assertThrows(OBException.class, () -> OBDal.getInstance().flush());
    assertThat(exception.getMessage(),
        containsString(OBMessageUtils.getI18NMessage("ValidateBaseSequenceMod10",
            new String[] { baseSequence.getIdentifier(), parentSequence.getIdentifier() })));

    baseSequence.setControlDigit(ControlDigit.MODULE10.value);
    OBDal.getInstance().save(baseSequence);

    exception = assertThrows(OBException.class, () -> OBDal.getInstance().flush());
    assertThat(exception.getMessage(),
        containsString(OBMessageUtils.messageBD("ValidateSequence")));
  }

  /**
   * Update Parent Sequence having Module 10 control digit with new Base sequence with alphanumeric
   * suffix or prefix.
   */
  @Test
  public void testSequenceException_ControlDigitModule10_UpdateBaseSequenceWithAlphanumericPrefixSuffix() {
    final Sequence baseSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, null, null, null, null,
        "100", ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, false);

    OBDal.getInstance().save(baseSequence);
    final Sequence parentSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.SEQUENCE, baseSequence, "06", null, null,
        null, "100", ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, false);

    OBDal.getInstance().save(parentSequence);
    OBDal.getInstance().flush();

    // new base sequence with alphanumeric suffix
    final Sequence newBaseSequenceAlphanumericSuffix = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, null, null, null, null,
        "1A2", ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, false);

    OBDal.getInstance().save(newBaseSequenceAlphanumericSuffix);

    // set parent sequence having Module10 control digit with new base sequence with alphanumeric
    // suffix.
    parentSequence.setBaseSequence(newBaseSequenceAlphanumericSuffix);
    OBDal.getInstance().save(parentSequence);

    Exception exception = assertThrows(Exception.class, () -> OBDal.getInstance().flush());
    assertThat(exception.getMessage(),
        containsString(OBMessageUtils.getI18NMessage("ValidateBaseSequenceMod10", new String[] {
            newBaseSequenceAlphanumericSuffix.getIdentifier(), parentSequence.getIdentifier() })));

    // new base sequence with alphanumeric prefix, suffix
    newBaseSequenceAlphanumericSuffix.setSuffix("102");
    newBaseSequenceAlphanumericSuffix.setPrefix("1A2");
    OBDal.getInstance().save(newBaseSequenceAlphanumericSuffix);

    // set parent sequence having Module10 control digit with new base sequence with alphanumeric
    // prefix.
    parentSequence.setBaseSequence(newBaseSequenceAlphanumericSuffix);
    OBDal.getInstance().save(parentSequence);

    exception = assertThrows(Exception.class, () -> OBDal.getInstance().flush());
    assertThat(exception.getMessage(),
        containsString(OBMessageUtils.getI18NMessage("ValidateBaseSequenceMod10", new String[] {
            newBaseSequenceAlphanumericSuffix.getIdentifier(), parentSequence.getIdentifier() })));

  }

  /**
   * Add Level 3 Sequences to check Base sequence with Alphanumeric prefix or suffix in first level
   * does not allow to create Sequence with its parent as base sequence and control digit module 10.
   */
  @Test
  public void testSequenceException_ControlDigitModule10_3Level() {
    final Sequence sequence1 = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, "A", null, null, null,
        null, ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, false);

    OBDal.getInstance().save(sequence1);

    final Sequence sequence2 = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.SEQUENCE, sequence1, null, null, null, null,
        null, ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, false);
    OBDal.getInstance().save(sequence2);

    final Sequence sequence3 = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.SEQUENCE, sequence2, null, null, null, null,
        null, ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, false);

    Exception exception = assertThrows(Exception.class, () -> OBDal.getInstance().save(sequence3));
    assertThat(exception.getMessage(),
        containsString(OBMessageUtils.getI18NMessage("ValidateBaseSequenceMod10",
            new String[] { sequence1.getIdentifier(), sequence3.getIdentifier() })));

  }

  /**
   * test to check whether updating base sequence of first sequence in a tree that creates an
   * infinite loop problem.
   */

  @Test
  public void testSequenceException_InfiniteLoopProblemBaseSequence() {

    final Sequence sequence1 = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, null, null, null, null,
        null, ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, false);
    OBDal.getInstance().save(sequence1);

    final Sequence sequence2 = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.SEQUENCE, sequence1, null, null, null, null,
        null, ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, false);
    OBDal.getInstance().save(sequence2);

    final Sequence sequence3 = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.SEQUENCE, sequence2, null, null, null, null,
        null, ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, false);
    OBDal.getInstance().save(sequence3);

    final Sequence sequence0 = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.SEQUENCE, sequence3, null, null, null, null,
        null, ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, false);

    OBDal.getInstance().save(sequence0);

    // Update calculation method and base sequence of first sequence in the tree
    sequence1.setCalculationMethod(CalculationMethod.SEQUENCE.value);
    sequence1.setBaseSequence(sequence0);
    OBDal.getInstance().save(sequence1);
    Exception exception = assertThrows(Exception.class, () -> OBDal.getInstance().flush());
    assertThat(exception.getMessage(),
        containsString(OBMessageUtils.getI18NMessage("NotValidBaseSequence")));

  }

  /**
   * test to check whether updating base sequence of intermediate sequence in a tree that creates an
   * infinite loop problem.
   */

  @Test
  public void testSequenceException_InfiniteLoopProblem_IntermediateBaseSequence() {

    final Sequence sequence1 = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, null, null, null, null,
        null, ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, false);

    OBDal.getInstance().save(sequence1);

    final Sequence sequence2 = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.SEQUENCE, sequence1, null, null, null, null,
        null, ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, false);

    OBDal.getInstance().save(sequence2);

    final Sequence sequence3 = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.SEQUENCE, sequence2, null, null, null, null,
        null, ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, false);

    OBDal.getInstance().save(sequence3);

    final Sequence sequence0 = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.SEQUENCE, sequence3, null, null, null, null,
        null, ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, false);

    OBDal.getInstance().save(sequence0);

    // Update base sequence of intermediate sequence in the tree
    sequence2.setBaseSequence(sequence0);
    OBDal.getInstance().save(sequence2);
    Exception exception = assertThrows(Exception.class, () -> OBDal.getInstance().flush());
    assertThat(exception.getMessage(),
        containsString(OBMessageUtils.getI18NMessage("NotValidBaseSequence")));

  }

  /**
   * Compute document no using SequenceUtil with invalid input
   */

  @Test
  public void utilitySequenceTest_SequenceUtil_InvalidSequenceId() {

    assertThat("Sequence with invalid information is not computed correctly using SequenceUtil",
        null, equalTo(SequenceUtil.getDocumentNo(false, null)));
  }

  /**
   * Utility: AD_SEQUENCE_DOC with invalid input
   * 
   */
  @Test
  public void utilitySequenceTest_AD_SEQUENCE_DOC_InvalidInput() {
    assertThat(
        "Sequence with invalid information is not computed correctly using Utility - AD_SEQUENCE_DOC",
        "", equalTo(Utility.getDocumentNo(new DalConnectionProvider(false),
            UUID.randomUUID().toString(), UUID.randomUUID().toString(), false)));
  }

  /**
   * Utility: AD_SEQUENCE_DOCTYPE with invalid input
   */

  @Test
  public void utilitySequenceTest_AD_SEQUENCE_DOCTYPE_InvalidInput() {
    assertThat(
        "Sequence with invalid information is not computed correctly using Utility - AD_SEQUENCE_DOCTYPE",
        "",
        equalTo(Utility.getDocumentNo(new DalConnectionProvider(false),
            RequestContext.get().getVariablesSecureApp(), "", SequenceTestUtils.C_ORDER_TABLE_NAME, "",
            UUID.randomUUID().toString(), false, false)));
  }
}
