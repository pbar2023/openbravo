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

import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SequenceUtil.CalculationMethod;
import org.openbravo.erpCommon.utility.SequenceUtil.ControlDigit;
import org.openbravo.erpCommon.utility.SequenceUtil.SequenceNumberLength;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.Organization;

public class SequenceCalculationMethodTest extends SequenceTest {

  /**
   * test Sequence with Calculation Method as Auto Numbering and null Base Sequence
   */
  @Test
  public void testSequenceWithCalculationMethod_AutoNumbering() {
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, "0110491", null, null,
        null, null, ControlDigit.NONE, SequenceNumberLength.FIXED, 7L, false);
    OBDal.getInstance().save(sequence);
    OBDal.getInstance().flush();
    assertTrue("Sequence with calculation method - AutoNumbering is not created", sequence != null);
  }

  /**
   * test Sequence with Calculation Method as DocumentNo_TablName and null Base Sequence
   */

  @Test
  public void testSequenceWithCalculationMethod_DocumentNoTableName() {
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance().getProxy(Organization.class, QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.DOCUMENTNO_TABLENAME, null, "0110491", null,
        null, null, null, ControlDigit.NONE, SequenceNumberLength.FIXED, 7L, false);
    OBDal.getInstance().save(sequence);
    OBDal.getInstance().flush();
    assertTrue("Sequence with calculation method - DocumentNo_TableName is not created",
        sequence != null);
  }

  /**
   * test Sequence with Calculation Method as Based On Sequence with Sequence and its valid Base
   * Sequence
   */
  @Test
  public void testSequenceWithCalculationMethod_Sequence() {
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
    assertTrue(
        "Sequence with calculation method - Sequence with Sequence and its valid base sequence is not created",
        parentSequence != null && parentSequence.getBaseSequence() != null);

  }
}
