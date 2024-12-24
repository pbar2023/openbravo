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
import static org.hamcrest.Matchers.equalTo;

import java.util.UUID;

import org.junit.Test;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SequenceUtil;
import org.openbravo.erpCommon.utility.SequenceUtil.CalculationMethod;
import org.openbravo.erpCommon.utility.SequenceUtil.ControlDigit;
import org.openbravo.erpCommon.utility.SequenceUtil.SequenceNumberLength;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.DalConnectionProvider;

public class SequenceUtilAndUtilityTest extends SequenceTest {

  private static final String DOC_TYPE_ID = "FF8080812C2ABFC6012C2B3BDF4D005A";

  /**
   * Test documentNo from sequence with none control digit and no prefix, no suffix, no
   * nextAssignedNo, sequence number length variable
   * 
   * Compute and check documentNo using SequenceUtil, Utility - AD_SEQUENCE_DOC,
   * AD_SEQUENCE_DOCTYPE, AD_SEQUENCE_DOCUMENTNO
   */
  @Test
  public void sequenceUtilAndUtilityTest_AutoNumbering_ControlDigitNone() {
    String assertionMsg = "Sequence Number is not computed correctly for Sequence with AutoNumbering calculation method, "
        + " no Prefix, no Suffix, no nextAssignedNo, ControlDigit - None, Variable Sequence Number Length";

    final String tableName = UUID.randomUUID().toString();
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        "DocumentNo_" + tableName, CalculationMethod.AUTONUMERING, null, null, null, null, null,
        null, ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, true);

    // Run checks when updateNext is No
    checkAllUtilitiesWithoutUpdatingSequence(sequence, tableName, assertionMsg, "1");

    // Run checks when updateNext is Yes
    runADSequenceDocTypeCheck(sequence, assertionMsg, "1", true);
    runSequenceUtilCheck(sequence, assertionMsg, "2", true);
    runADSequencDocCheck(sequence, tableName, assertionMsg, "3", true);
    runADSequenceDocumentNoCheck(sequence, assertionMsg, "4", true);
  }

  /**
   * Test documentNo from sequence with none control digit and no prefix, no suffix, nextAssignedNo:
   * 1000L, sequence number length variable
   * 
   * Compute and check documentNo using SequenceUtil, Utility - AD_SEQUENCE_DOC,
   * AD_SEQUENCE_DOCTYPE, AD_SEQUENCE_DOCUMENTNO
   */
  @Test
  public void sequenceUtilAndUtilityTest_AutoNumbering_ControlDigitNone_NextAssignedNo() {
    String assertionMsg = "Sequence Number is not computed correctly for Sequence with AutoNumbering calculation method, "
        + " no Prefix, no Suffix, valid nextAssignedNo, ControlDigit - None, Variable Sequence Number Length";

    final String tableName = UUID.randomUUID().toString();
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        "DocumentNo_" + tableName, CalculationMethod.AUTONUMERING, null, null, null, 1000L, null,
        null, ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, true);

    // Run checks with updateNext as No
    checkAllUtilitiesWithoutUpdatingSequence(sequence, tableName, assertionMsg, "1000");

    // Run checks when updateNext is Yes
    runADSequenceDocTypeCheck(sequence, assertionMsg, "1000", true);
    runSequenceUtilCheck(sequence, assertionMsg, "1001", true);
    runADSequencDocCheck(sequence, tableName, assertionMsg, "1002", true);
    runADSequenceDocumentNoCheck(sequence, assertionMsg, "1003", true);
  }

  /**
   * Test documentNo from sequence with none control digit and prefix: A, no suffix, nextAssignedNo:
   * 1000L, sequence number length variable
   * 
   * Compute and check documentNo using SequenceUtil, Utility - AD_SEQUENCE_DOC,
   * AD_SEQUENCE_DOCTYPE, AD_SEQUENCE_DOCUMENTNO
   */
  @Test
  public void sequenceUtilAndUtilityTest_AutoNumbering_ControlDigitNone_NextAssignedNo_Prefix() {
    String assertionMsg = "Sequence Number is not computed correctly for Sequence with AutoNumbering calculation method, "
        + " valid Prefix, no Suffix, valid nextAssignedNo, ControlDigit - None, Variable Sequence Number Length";

    final String tableName = UUID.randomUUID().toString();
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        "DocumentNo_" + tableName, CalculationMethod.AUTONUMERING, null, "A", null, 1000L, null,
        null, ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, true);

    // Run checks when updateNext is No
    checkAllUtilitiesWithoutUpdatingSequence(sequence, tableName, assertionMsg, "A1000");

    // Run checks when updateNext is Yes
    runADSequenceDocTypeCheck(sequence, assertionMsg, "A1000", true);
    runSequenceUtilCheck(sequence, assertionMsg, "A1001", true);
    runADSequencDocCheck(sequence, tableName, assertionMsg, "A1002", true);
    runADSequenceDocumentNoCheck(sequence, assertionMsg, "A1003", true);
  }

  /**
   * Test documentNo from sequence with none control digit, no prefix, suffix: 000, nextAssignedNo:
   * 1000L, sequence number length variable
   * 
   * Compute and check documentNo using SequenceUtil, Utility - AD_SEQUENCE_DOC,
   * AD_SEQUENCE_DOCTYPE, AD_SEQUENCE_DOCUMENTNO
   */
  @Test
  public void sequenceUtilAndUtilityTest_AutoNumbering_ControlDigitNone_NextAssignedNo_Suffix() {
    String assertionMsg = "Sequence Number is not computed correctly for Sequence with AutoNumbering calculation method, "
        + " no Prefix, valid Suffix, valid nextAssignedNo, ControlDigit - None, Variable Sequence Number Length";

    final String tableName = UUID.randomUUID().toString();
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        "DocumentNo_" + tableName, CalculationMethod.AUTONUMERING, null, null, null, 1000L, null,
        "000", ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, true);

    // Run checks when updateNext is No
    checkAllUtilitiesWithoutUpdatingSequence(sequence, tableName, assertionMsg, "1000000");

    // Run checks when updateNext is Yes
    runADSequenceDocTypeCheck(sequence, assertionMsg, "1000000", true);
    runSequenceUtilCheck(sequence, assertionMsg, "1001000", true);
    runADSequencDocCheck(sequence, tableName, assertionMsg, "1002000", true);
    runADSequenceDocumentNoCheck(sequence, assertionMsg, "1003000", true);
  }

  /**
   * Test documentNo from sequence with none control digit, prefix: A, suffix: 000, nextAssignedNo:
   * 1000L, sequence number length variable
   * 
   * Compute and check documentNo using SequenceUtil, Utility - AD_SEQUENCE_DOC,
   * AD_SEQUENCE_DOCTYPE, AD_SEQUENCE_DOCUMENTNO
   */
  @Test
  public void sequenceUtilAndUtilityTest_AutoNumbering_ControlDigitNone_NextAssignedNo_PrefixSuffix() {
    String assertionMsg = "Sequence Number is not computed correctly for Sequence with AutoNumbering calculation method, "
        + " valid Prefix, valid Suffix, valid nextAssignedNo, ControlDigit - None, Variable Sequence Number Length";

    final String tableName = UUID.randomUUID().toString();
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        "DocumentNo_" + tableName, CalculationMethod.AUTONUMERING, null, "A", null, 1000L, null,
        "000", ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, true);

    // Run checks when updateNext is No
    checkAllUtilitiesWithoutUpdatingSequence(sequence, tableName, assertionMsg, "A1000000");

    // Run checks when updateNext is Yes
    runADSequenceDocTypeCheck(sequence, assertionMsg, "A1000000", true);
    runSequenceUtilCheck(sequence, assertionMsg, "A1001000", true);
    runADSequencDocCheck(sequence, tableName, assertionMsg, "A1002000", true);
    runADSequenceDocumentNoCheck(sequence, assertionMsg, "A1003000", true);
  }

  /**
   * Test documentNo from sequence with none control digit, prefix: A, suffix: 000, nextAssignedNo:
   * 1000L, sequence number length fixed: 7L
   * 
   * Compute and check documentNo using SequenceUtil, Utility - AD_SEQUENCE_DOC,
   * AD_SEQUENCE_DOCTYPE, AD_SEQUENCE_DOCUMENTNO
   */
  @Test
  public void sequenceUtilAndUtilityTest_AutoNumbering_ControlDigitNone_NextAssignedNo_PrefixSuffix_FixedLength_GreaterThan_ComputedSequenceLength() {
    String assertionMsg = "Sequence Number is not computed correctly for Sequence with AutoNumbering calculation method, "
        + " valid Prefix, valid Suffix, valid nextAssignedNo, ControlDigit - None, Fixed Length 7L";

    final String tableName = UUID.randomUUID().toString();
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        "DocumentNo_" + tableName, CalculationMethod.AUTONUMERING, null, "A", null, 1000L, null,
        "000", ControlDigit.NONE, SequenceNumberLength.FIXED, 7L, true);

    // Run checks when updateNext is No
    checkAllUtilitiesWithoutUpdatingSequence(sequence, tableName, assertionMsg, "A0001000000");

    // Run checks when updateNext is Yes
    runADSequenceDocTypeCheck(sequence, assertionMsg, "A0001000000", true);
    runSequenceUtilCheck(sequence, assertionMsg, "A0001001000", true);
    runADSequencDocCheck(sequence, tableName, assertionMsg, "A0001002000", true);
    runADSequenceDocumentNoCheck(sequence, assertionMsg, "A0001003000", true);
  }

  /**
   * Test documentNo from sequence with none control digit, prefix: A, suffix: 000, nextAssignedNo:
   * 1000L, sequence number length fixed: 4L
   * 
   * Compute and check documentNo using SequenceUtil, Utility - AD_SEQUENCE_DOC,
   * AD_SEQUENCE_DOCTYPE, AD_SEQUENCE_DOCUMENTNO
   */
  @Test
  public void sequenceUtilAndUtilityTest_AutoNumbering_ControlDigitNone_NextAssignedNo_PrefixSuffix_FixedLength_LessThan_ComputedSequenceLength() {
    String assertionMsg = "Sequence Number is not computed correctly for Sequence with AutoNumbering calculation method, "
        + " valid Prefix, valid Suffix, valid nextAssignedNo, ControlDigit - None, Fixed Length 4L";

    final String tableName = UUID.randomUUID().toString();
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        "DocumentNo_" + tableName, CalculationMethod.AUTONUMERING, null, "A", null, 1000L, null,
        "000", ControlDigit.NONE, SequenceNumberLength.FIXED, 4L, true);

    // Run checks when updateNext is No
    checkAllUtilitiesWithoutUpdatingSequence(sequence, tableName, assertionMsg, "A1000000");

    // Run checks when updateNext is Yes
    runADSequenceDocTypeCheck(sequence, assertionMsg, "A1000000", true);
    runSequenceUtilCheck(sequence, assertionMsg, "A1001000", true);
    runADSequencDocCheck(sequence, tableName, assertionMsg, "A1002000", true);
    runADSequenceDocumentNoCheck(sequence, assertionMsg, "A1003000", true);
  }

  /**
   * Test documentNo from sequence with module 10 control digit, no prefix, no suffix, no
   * nextAssignedNo, sequence number length variable
   * 
   * Compute and check documentNo using SequenceUtil, Utility - AD_SEQUENCE_DOC,
   * AD_SEQUENCE_DOCTYPE, AD_SEQUENCE_DOCUMENTNO
   */
  @Test
  public void sequenceUtilAndUtilityTest_ControlDigitMod10_AutoNumbering() {
    String assertionMsg = "Sequence Number is not computed correctly for Sequence with AutoNumbering calculation method, "
        + " no Prefix, no Suffix, no nextAssignedNo, ControlDigit - Module 10, Variable Sequence Length";

    final String tableName = UUID.randomUUID().toString();
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        "DocumentNo_" + tableName, CalculationMethod.AUTONUMERING, null, null, null, null, null,
        null, ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, true);

    // Run checks when updateNext is No
    checkAllUtilitiesWithoutUpdatingSequence(sequence, tableName, assertionMsg, "17");

    // Run checks when updateNext is Yes
    runADSequenceDocTypeCheck(sequence, assertionMsg, "17", true);
    runSequenceUtilCheck(sequence, assertionMsg, "24", true);
    runADSequencDocCheck(sequence, tableName, assertionMsg, "31", true);
    runADSequenceDocumentNoCheck(sequence, assertionMsg, "48", true);
  }

  /**
   * Test documentNo from sequence with module 10 control digit, no prefix, no suffix,
   * nextAssignedNo:1000L, sequence number length variable
   * 
   * Compute and check documentNo using SequenceUtil, Utility - AD_SEQUENCE_DOC,
   * AD_SEQUENCE_DOCTYPE, AD_SEQUENCE_DOCUMENTNO
   */
  @Test
  public void sequenceUtilAndUtilityTest_AutoNumbering_ControlDigitMod10_NextAssignedNo() {
    String assertionMsg = "Sequence Number is not computed correctly for Sequence with AutoNumbering calculation method, "
        + " no Prefix, no Suffix, valid nextAssignedNo, ControlDigit - Module 10, Variable Sequence Length";

    final String tableName = UUID.randomUUID().toString();
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        "DocumentNo_" + tableName, CalculationMethod.AUTONUMERING, null, null, null, 1000L, null,
        null, ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, true);

    // Run checks when updateNext is No
    checkAllUtilitiesWithoutUpdatingSequence(sequence, tableName, assertionMsg, "10009");

    // Run checks when updateNext is Yes
    runADSequenceDocTypeCheck(sequence, assertionMsg, "10009", true);
    runSequenceUtilCheck(sequence, assertionMsg, "10016", true);
    runADSequencDocCheck(sequence, tableName, assertionMsg, "10023", true);
    runADSequenceDocumentNoCheck(sequence, assertionMsg, "10030", true);
  }

  /**
   * Test documentNo from sequence with module 10 control digit and prefix: 6, no suffix,
   * nextAssignedNo:1000L, sequence number length variable
   * 
   * Compute and check documentNo using SequenceUtil, Utility - AD_SEQUENCE_DOC,
   * AD_SEQUENCE_DOCTYPE, AD_SEQUENCE_DOCUMENTNO
   */
  @Test
  public void sequenceUtilAndUtilityTest_AutoNumbering_ControlDigitMod10_NextAssignedNo_Prefix() {
    String assertionMsg = "Sequence Number is not computed correctly for Sequence with AutoNumbering calculation method, "
        + " valid Prefix, no Suffix, valid nextAssignedNo, ControlDigit - Module 10, Variable Sequence Length";

    final String tableName = UUID.randomUUID().toString();
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        "DocumentNo_" + tableName, CalculationMethod.AUTONUMERING, null, "6", null, 1000L, null,
        null, ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, true);

    // Run checks when updateNext is No
    checkAllUtilitiesWithoutUpdatingSequence(sequence, tableName, assertionMsg, "610001");

    // Run checks when updateNext is Yes
    runADSequenceDocTypeCheck(sequence, assertionMsg, "610001", true);
    runSequenceUtilCheck(sequence, assertionMsg, "610018", true);
    runADSequencDocCheck(sequence, tableName, assertionMsg, "610025", true);
    runADSequenceDocumentNoCheck(sequence, assertionMsg, "610032", true);
  }

  /**
   * Test documentNo from sequence with module 10 control digit and no prefix, suffix 000,
   * nextAssignedNo:1000L, sequence number length variable
   * 
   * Compute and check documentNo using SequenceUtil, Utility - AD_SEQUENCE_DOC,
   * AD_SEQUENCE_DOCTYPE, AD_SEQUENCE_DOCUMENTNO
   */
  @Test
  public void sequenceUtilAndUtilityTest_AutoNumbering_ControlDigitMod10_NextAssignedNo_Suffix() {
    String assertionMsg = "Sequence Number is not computed correctly for Sequence with AutoNumbering calculation method, "
        + " no Prefix, valid Suffix, valid nextAssignedNo, ControlDigit - Module 10, Variable Sequence Length";

    final String tableName = UUID.randomUUID().toString();
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        "DocumentNo_" + tableName, CalculationMethod.AUTONUMERING, null, null, null, 1000L, null,
        "000", ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, true);

    // Run checks when updateNext is No
    checkAllUtilitiesWithoutUpdatingSequence(sequence, tableName, assertionMsg, "10000007");

    // Run checks when updateNext is Yes
    runADSequenceDocTypeCheck(sequence, assertionMsg, "10000007", true);
    runSequenceUtilCheck(sequence, assertionMsg, "10010006", true);
    runADSequencDocCheck(sequence, tableName, assertionMsg, "10020005", true);
    runADSequenceDocumentNoCheck(sequence, assertionMsg, "10030004", true);
  }

  /**
   * Test documentNo from sequence with module 10 control digit, prefix 6, suffix 000,
   * nextAssignedNo:1000L, sequence number length variable
   * 
   * Compute and check documentNo using SequenceUtil, Utility - AD_SEQUENCE_DOC,
   * AD_SEQUENCE_DOCTYPE, AD_SEQUENCE_DOCUMENTNO
   */
  @Test
  public void sequenceUtilAndUtilityTest_AutoNumbering_ControlDigitMod10_NextAssignedNo_PrefixSuffix() {
    String assertionMsg = "Sequence Number is not computed correctly for Sequence with AutoNumbering calculation method, "
        + " valid Prefix, valid Suffix, valid nextAssignedNo, ControlDigit - Module 10, Variable Sequence Length";

    final String tableName = UUID.randomUUID().toString();
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        "DocumentNo_" + tableName, CalculationMethod.AUTONUMERING, null, "6", null, 1000L, null,
        "000", ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, true);

    // Run checks when updateNext is No
    checkAllUtilitiesWithoutUpdatingSequence(sequence, tableName, assertionMsg, "610000001");

    // Run checks when updateNext is Yes
    runADSequenceDocTypeCheck(sequence, assertionMsg, "610000001", true);
    runSequenceUtilCheck(sequence, assertionMsg, "610010000", true);
    runADSequencDocCheck(sequence, tableName, assertionMsg, "610020009", true);
    runADSequenceDocumentNoCheck(sequence, assertionMsg, "610030008", true);
  }

  /**
   * Test documentNo from sequence with module 10 control digit, prefix 7, suffix 000,
   * nextAssignedNo:1001L, sequence number length fixed, sequence length 7L
   * 
   * Compute and check documentNo using SequenceUtil, Utility - AD_SEQUENCE_DOC,
   * AD_SEQUENCE_DOCTYPE, AD_SEQUENCE_DOCUMENTNO
   */
  @Test
  public void sequenceUtilAndUtilityTest_AutoNumbering_ControlDigitMod10_NextAssignedNo_PrefixSuffix_FixedLength_GreaterThan_ComputedSequenceLength() {
    String assertionMsg = "Sequence Number is not computed correctly for Sequence with AutoNumbering calculation method, "
        + " valid Prefix, valid Suffix, valid nextAssignedNo, ControlDigit - Module 10, Fixed Sequence Number Length: 7L";

    final String tableName = UUID.randomUUID().toString();
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        "DocumentNo_" + tableName, CalculationMethod.AUTONUMERING, null, "7", null, 1001L, null,
        "000", ControlDigit.MODULE10, SequenceNumberLength.FIXED, 7L, true);

    // Run checks when updateNext is No
    checkAllUtilitiesWithoutUpdatingSequence(sequence, tableName, assertionMsg, "700010010005");

    // Run checks when updateNext is Yes
    runADSequenceDocTypeCheck(sequence, assertionMsg, "700010010005", true);
    runSequenceUtilCheck(sequence, assertionMsg, "700010020004", true);
    runADSequencDocCheck(sequence, tableName, assertionMsg, "700010030003", true);
    runADSequenceDocumentNoCheck(sequence, assertionMsg, "700010040002", true);
  }

  /**
   * Test documentNo from sequence with module 10 control digit, prefix 8, suffix 000,
   * nextAssignedNo:1001L, sequence number length fixed, sequence length 3L
   * 
   * Compute and check documentNo using SequenceUtil, Utility - AD_SEQUENCE_DOC,
   * AD_SEQUENCE_DOCTYPE, AD_SEQUENCE_DOCUMENTNO
   * 
   */
  @Test
  public void sequenceUtilAndUtilityTest_AutoNumbering_ControlDigitMod10_NextAssignedNo_PrefixSuffix_FixedLength_LessThan_ComputedSequenceLength() {
    String assertionMsg = "Sequence Number is not computed correctly for Sequence with AutoNumbering calculation method, "
        + " valid Prefix, valid Suffix, valid nextAssignedNo, ControlDigit - Module 10, Fixed Sequence Number Length: 7L";

    final String tableName = UUID.randomUUID().toString();
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        "DocumentNo_" + tableName, CalculationMethod.AUTONUMERING, null, "8", null, 1001L, null,
        "000", ControlDigit.MODULE10, SequenceNumberLength.FIXED, 3L, true);

    // Run checks when updateNext is No
    checkAllUtilitiesWithoutUpdatingSequence(sequence, tableName, assertionMsg, "810010008");

    // Run checks when updateNext is Yes
    runADSequenceDocTypeCheck(sequence, assertionMsg, "810010008", true);
    runSequenceUtilCheck(sequence, assertionMsg, "810020007", true);
    runADSequencDocCheck(sequence, tableName, assertionMsg, "810030006", true);
    runADSequenceDocumentNoCheck(sequence, assertionMsg, "810040005", true);
  }

  /**
   * 2 Level Sequences
   * 
   * Test with Base Sequence having CalculationMethod: Auto numbering, Control Digit : Module10,
   * NextAssignedNo: 2821L, SequenceNumberLength: Fixed, Sequence Length: 5L, Prefix: 0110491.
   * 
   * Parent Sequence having CalculationMethod: Based On Sequence, Base Sequence as above, Control
   * Digit : Module10, SequenceNumberLength: Variable, Prefix: 6, Suffix: 000
   * 
   * Compute and check documentNo using SequenceUtil, Utility - AD_SEQUENCE_DOC,
   * AD_SEQUENCE_DOCTYPE, AD_SEQUENCE_DOCUMENTNO
   */
  @Test
  public void sequenceUtilAndUtilityTest_BaseSequence_AutoNumbering_NextAssignedNo_Level2() {
    String assertionMsg = "Sequence Number is not computed correctly for Parent Sequence "
        + " with Based On Sequence calculation method, 2 Level Base Sequence";

    final String tableName = UUID.randomUUID().toString();
    final Sequence baseSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, "0110491", null, 2821L,
        null, null, ControlDigit.MODULE10, SequenceNumberLength.FIXED, 5L, true);

    final Sequence parentSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        "DocumentNo_" + tableName, CalculationMethod.SEQUENCE, baseSequence, "6", null, null, null,
        "000", ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, true);

    // Run checks when updateNext is No
    checkAllUtilitiesWithoutUpdatingSequence(parentSequence, tableName, assertionMsg,
        "601104910282130002");

    // Run checks when updateNext is Yes
    runADSequenceDocTypeCheck(parentSequence, assertionMsg, "601104910282130002", true);
    OBDal.getInstance().refresh(baseSequence); // Refresh to get updated sequence
    runSequenceUtilCheck(parentSequence, assertionMsg, "601104910282200002", true);
    OBDal.getInstance().refresh(baseSequence); // Refresh to get updated sequence
    runADSequencDocCheck(parentSequence, tableName, assertionMsg, "601104910282370002", true);
    runADSequenceDocumentNoCheck(parentSequence, assertionMsg, "601104910282440002", true);
  }

  /**
   * 
   * 3 Level Sequences
   * 
   * Test with Sequence having CalculationMethod: Auto numbering, Control Digit : Module10,
   * NextAssignedNo: 5000L, SequenceNumberLength: Variable, Prefix: 3, Suffix: 7
   * 
   * Base Sequence having CalculationMethod: Based On Sequence, Sequence as above, Control Digit :
   * Module10, SequenceNumberLength: Variable, Prefix: 8, Suffix: 2
   * 
   * Parent Sequence having CalculationMethod: Based On Sequence, Base Sequence as above, Control
   * Digit : Module10, SequenceNumberLength: Variable, Prefix: 9, Suffix: 1
   * 
   * Compute and check documentNo using SequenceUtil, Utility - AD_SEQUENCE_DOC,
   * AD_SEQUENCE_DOCTYPE, AD_SEQUENCE_DOCUMENTNO
   */
  @Test
  public void sequenceUtilAndUtilityTest_BaseSequence_AutoNumbering_NextAssignedNo_PrefixSuffix_Level3() {
    String assertionMsg = "Sequence Number is not computed correctly for Parent Sequence "
        + " with Based On Sequence calculation method, 3 Level Base Sequence";

    final String tableName = UUID.randomUUID().toString();
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, "3", null, 5000L, null,
        "7", ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, true);

    final Sequence baseSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.SEQUENCE, sequence, "8", null, null, null,
        "2", ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, true);

    final Sequence parentSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        "DocumentNo_" + tableName, CalculationMethod.SEQUENCE, baseSequence, "9", null, null, null,
        "1", ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, true);

    // Run checks when updateNext is No
    checkAllUtilitiesWithoutUpdatingSequence(parentSequence, tableName, assertionMsg,
        "983500072510");

    // Run checks when updateNext is Yes
    runADSequenceDocTypeCheck(parentSequence, assertionMsg, "983500072510", true);
    OBDal.getInstance().refresh(sequence); // Refresh to get updated sequence
    runSequenceUtilCheck(parentSequence, assertionMsg, "983500172210", true);
    OBDal.getInstance().refresh(sequence); // Refresh to get updated sequence
    runADSequencDocCheck(parentSequence, tableName, assertionMsg, "983500272910", true);
    runADSequenceDocumentNoCheck(parentSequence, assertionMsg, "983500372610", true);
  }

  /**
   * Compute documentNo using SequenceUtil, Utility - AD_SEQUENCE_DOC, AD_SEQUENCE_DOCTYPE,
   * AD_SEQUENCE_DOCUMENTNO and compare it with expectedSequenceNumber when updateNext is No
   */
  private void checkAllUtilitiesWithoutUpdatingSequence(Sequence sequence, String tableName,
      String assertionMsg, String expectedSequenceNumber) {
    // Test SequenceUtil.getDocumentNo()
    runSequenceUtilCheck(sequence, assertionMsg, expectedSequenceNumber, false);

    // Tests AD_Sequence_Doc
    runADSequencDocCheck(sequence, tableName, assertionMsg, expectedSequenceNumber, false);

    // Tests AD_Sequence_DocType
    runADSequenceDocTypeCheck(sequence, assertionMsg, expectedSequenceNumber, false);

    // Tests AD_Sequence_DocumentNo
    runADSequenceDocumentNoCheck(sequence, assertionMsg, expectedSequenceNumber, false);
  }

  /**
   * Compute documentNo using SequenceUtil and compare it with expectedSequenceNumber
   */
  private void runSequenceUtilCheck(Sequence sequence, String assertionMsg,
      String expectedSequenceNumber, boolean updateNext) {
    String documentNo = SequenceUtil.getDocumentNo(updateNext, sequence);
    if (updateNext) {
      // Flush is necessary to update the sequence when using SequenceUtil
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(sequence); // Refresh to get updated sequence
    }
    assertThat(assertionMsg + ", updateNext " + updateNext + " using SequenceUtil", documentNo,
        equalTo(expectedSequenceNumber));
  }

  /**
   * Compute documentNo using Utility - AD_SEQUENCE_DOC and compare it with expectedSequenceNumber
   */
  private void runADSequencDocCheck(Sequence sequence, String tableName, String assertionMsg,
      String expectedSequenceNumber, boolean updateNext) {
    assertThat(assertionMsg + ", updateNext " + updateNext + " using AD_SEQUENCE_DOC",
        Utility.getDocumentNo(new DalConnectionProvider(false), sequence.getClient().getId(),
            tableName, updateNext),
        equalTo(expectedSequenceNumber));
    if (updateNext) {
      OBDal.getInstance().refresh(sequence); // Refresh to get updated sequence
    }
  }

  /**
   * Compute documentNo using Utility - AD_SEQUENCE_DOCTYPE and compare it with
   * expectedSequenceNumber
   */
  private void runADSequenceDocTypeCheck(Sequence sequence, String assertionMsg,
      String expectedSequenceNumber, boolean updateNext) {
    // C_ORDER_TABLE_NAME is a dummy value to be able to use AD_SEQUENCE_DOCTYPE
    assertThat(assertionMsg + ", updateNext " + updateNext + " using AD_SEQUENCE_DOCTYPE",
        Utility.getDocumentNo(new DalConnectionProvider(false),
            RequestContext.get().getVariablesSecureApp(), "", SequenceTestUtils.C_ORDER_TABLE_NAME,
            "", SequenceTestUtils.createDocumentType(DOC_TYPE_ID, sequence).getId(), false,
            updateNext),
        equalTo(expectedSequenceNumber));
    if (updateNext) {
      OBDal.getInstance().refresh(sequence); // Refresh to get updated sequence
    }
  }

  /**
   * Compute documentNo using AD_SEQUENCE_DOCUMENTNO and compare it with expectedSequenceNumber
   */
  private void runADSequenceDocumentNoCheck(Sequence sequence, String assertionMsg,
      String expectedSequenceNumber, boolean updateNext) {
    assertThat(assertionMsg + ", updateNext " + updateNext + " using AD_SEQUENCE_DOCUMENTNO",
        SequenceTestUtils.callADSequenceDocumentNo(sequence.getId(), updateNext),
        equalTo(expectedSequenceNumber));
    if (updateNext) {
      OBDal.getInstance().refresh(sequence); // Refresh to get updated sequence
    }
  }

}
