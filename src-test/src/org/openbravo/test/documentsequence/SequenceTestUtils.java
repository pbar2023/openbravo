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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SequenceUtil.CalculationMethod;
import org.openbravo.erpCommon.utility.SequenceUtil.ControlDigit;
import org.openbravo.erpCommon.utility.SequenceUtil.SequenceNumberLength;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.CallStoredProcedure;

public class SequenceTestUtils {
  public static final String C_ORDER_TABLE_NAME = "C_Order";
  private static final String ANY_EXISTING_SEQUENCE_ID = "FF8080812C2ABFC6012C2B3BE4970094";

  /*
   * Creates Document Sequence to be used
   */
  public static Sequence createDocumentSequence(Organization org, String sequenceName,
      CalculationMethod calculationMethod, Sequence baseSequence, String prefix, Long startingNo,
      Long nextAssignedNumber, Long incrementBy, String suffix, ControlDigit controlDigit,
      SequenceNumberLength sequenceNoLength, Long sequenceLength, boolean saveAndflush) {
    final Sequence sequence = (Sequence) DalUtil
        .copy(OBDal.getInstance().getProxy(Sequence.class, ANY_EXISTING_SEQUENCE_ID));
    sequence.setOrganization(org);
    sequence.setName(sequenceName);
    sequence.setControlDigit(controlDigit.value);
    sequence.setCalculationMethod(calculationMethod.value);
    sequence.setPrefix(prefix);
    sequence.setStartingNo(startingNo == null ? 1L : startingNo);
    sequence.setNextAssignedNumber(nextAssignedNumber == null ? 1L : nextAssignedNumber);
    sequence.setIncrementBy(incrementBy == null ? 1L : incrementBy);
    sequence.setSuffix(suffix);
    sequence.setBaseSequence(baseSequence);
    sequence.setSequenceNumberLength(sequenceNoLength.value);
    sequence.setSequenceLength(sequenceLength);
    if (saveAndflush) {
      OBDal.getInstance().save(sequence);
      OBDal.getInstance().flush(); // Required to lock sequence at db level later on
    }
    return sequence;
  }

  /**
   * Create Document Type with Document Sequence
   */
  static DocumentType createDocumentType(String docTypeId, Sequence sequence) {
    final DocumentType anyExistingDocType = OBDal.getInstance()
        .getProxy(DocumentType.class, docTypeId);
    // Create a document Type, but do not copy its children's
    final DocumentType docType = (DocumentType) DalUtil.copy(anyExistingDocType, false);
    docType.setName(UUID.randomUUID().toString());
    docType.setCreationDate(new Date());
    docType.setSequencedDocument(true);
    docType.setDocumentSequence(sequence);
    OBDal.getInstance().save(docType);
    OBDal.getInstance().flush();
    return docType;
  }

  /**
   * 
   * Call AD_SEQUENCE_DOCUMENTNO - get documentNo using computation of sequence and control digit in
   * PL
   *
   * @param sequenceId
   *          Document Sequence configuration used to compute document No.
   * @param updateNext
   *          flag to update current next in AD_Sequence
   * @return computed documentNo using computation of sequence and control digit in PL
   */
  public static String callADSequenceDocumentNo(String sequenceId, boolean updateNext) {
    try {
      final List<Object> parameters = new ArrayList<>();
      parameters.add(sequenceId);
      parameters.add(updateNext);
      return ((String) CallStoredProcedure.getInstance()
          .call("AD_SEQUENCE_DOCUMENTNO", parameters, null));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
