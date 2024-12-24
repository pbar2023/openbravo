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

package org.openbravo.erpCommon.utility;

import java.util.Arrays;
import java.util.function.Function;

import org.apache.commons.lang.StringUtils;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.Sequence;

/**
 * Central place for sequence management. It is a pure Java implementation not based on the
 * AD_SEQUENCE_DOC and AD_SEQUENCE_DOCTYPE PL/SQL procedures.
 *
 */
public class SequenceUtil {

  public enum CalculationMethod {
    DOCUMENTNO_TABLENAME("N"), // Deprecated and not supported by this class
    AUTONUMERING("A"),
    SEQUENCE("S");

    public final String value;

    private CalculationMethod(String value) {
      this.value = value;
    }
  }

  public enum SequenceNumberLength {
    FIXED("F", "0"), VARIABLE("V", null);

    public final String value;
    public final String padLeftString;

    private SequenceNumberLength(String value, String padLeftString) {
      this.value = value;
      this.padLeftString = padLeftString;
    }
  }

  public enum ControlDigit {
    NONE(null, documentNo -> ""),

    MODULE10("M10", documentNo -> {
      if (documentNo == null || documentNo.trim().isEmpty()
          || !StringUtils.isNumeric((documentNo))) {
        throw new OBException(OBMessageUtils.messageBD("ValidateSequenceForControlDigit"));
      }

      int sum = 0;
      boolean isOddIndexedDigit = true;
      String reverserSequence = new StringBuilder(documentNo).reverse().toString();
      for (int i = 0; i <= reverserSequence.length() - 1; i++) {
        char digitChar = reverserSequence.charAt(i);
        int digit = Character.getNumericValue(digitChar);
        if (isOddIndexedDigit) {
          sum += digit * 3;
        } else {
          sum += digit;
        }
        isOddIndexedDigit = !isOddIndexedDigit;
      }
      return (10 - (sum % 10)) % 10;
    });

    public final String value;
    public final Function<String, Object> calculator;

    ControlDigit(String value, Function<String, Object> calculator) {
      this.value = value;
      this.calculator = calculator;
    }

    private static ControlDigit getByValue(String searchKey) {
      return Arrays.stream(ControlDigit.values())
          .filter(e -> searchKey != null && searchKey.equals(e.value))
          .findFirst()
          .orElse(NONE);
    }
  }

  /**
   * Retrieves the next document number from the specified sequence.
   * 
   * @param updatedNext
   *          When set to true, it updates and persists the next assigned number in the database.
   *          Use false when you want to retrieve the next document number without updating the
   *          sequence in the database, for instance, to preview the document number before saving
   *          the document.
   * @param seq
   *          The sequence from which to generate the document number.
   * 
   * @return The next document number ready to be used, or null if no sequence is found.
   */
  public static String getDocumentNo(boolean updatedNext, final Sequence seq) {
    if (seq == null) {
      return null;
    }
    Sequence sequence = getSequenceAndLockIfUpdateNext(updatedNext, seq);
    final String calculationMethod = sequence.getCalculationMethod();

    final StringBuilder documentNo = new StringBuilder();

    if (CalculationMethod.AUTONUMERING.value.equals(calculationMethod)) {
      documentNo.append(getNextDocNumberWithoutPrefixSuffix(updatedNext, sequence));
    } else if (CalculationMethod.SEQUENCE.value.equals(calculationMethod)) {
      documentNo.append(getDocumentNo(updatedNext, sequence.getBaseSequence()));
    } else if (CalculationMethod.DOCUMENTNO_TABLENAME.value.equals(calculationMethod)) {
      // Deprecated Calculation Method (former AutoNumbering=N)
      throw new OBException("Calculation Method not supported: " + calculationMethod
          + " by this method." + OBMessageUtils.messageBD("DeprecatedCalculationMethod"), true);
    } else {
      throw new OBException("Calculation Method not supported: " + calculationMethod, true);
    }

    if (SequenceNumberLength.FIXED.value.equals(sequence.getSequenceNumberLength())) {
      documentNo.replace(0, documentNo.length(), StringUtils.leftPad(documentNo.toString(),
          sequence.getSequenceLength().intValue(), SequenceNumberLength.FIXED.padLeftString));
    }

    addPrefixAndSuffix(sequence.getPrefix(), documentNo, sequence.getSuffix());

    final ControlDigit controlDigit = ControlDigit.getByValue(sequence.getControlDigit());
    documentNo.append(controlDigit.calculator.apply(documentNo.toString()));

    return documentNo.toString();
  }

  private static String getNextDocNumberWithoutPrefixSuffix(final boolean updateNext,
      final Sequence seq) {
    final Long nextDocNumber = seq.getNextAssignedNumber();
    incrementSeqIfUpdateNext(updateNext, seq);
    return nextDocNumber.toString();
  }

  private static Sequence getSequenceAndLockIfUpdateNext(final boolean updateNext,
      final Sequence seqParam) {
    if (updateNext) {
      // We lock the sequence with a select for update to avoid duplicates
      return lockSequence(seqParam.getId());
    }
    return seqParam;
  }

  private static Sequence lockSequence(String sequenceId) {
    // @formatter:off
    final String where = ""
        + "select s "
        + "from ADSequence s "
        + "where id = :id";
    // @formatter:on
    final Session session = OBDal.getInstance().getSession();
    final Query<Sequence> query = session.createQuery(where, Sequence.class);
    query.setParameter("id", sequenceId);
    query.setMaxResults(1);
    query.setLockOptions(LockOptions.UPGRADE);
    return query.uniqueResult();
  }

  private static void incrementSeqIfUpdateNext(final boolean updateNext, final Sequence seq) {
    if (updateNext) {
      seq.setNextAssignedNumber(seq.getNextAssignedNumber() + seq.getIncrementBy());
      OBDal.getInstance().save(seq);
    }
  }

  private static void addPrefixAndSuffix(final String prefix, final StringBuilder documentNo,
      final String suffix) {
    if (StringUtils.isNotEmpty(prefix)) {
      documentNo.insert(0, prefix);
    }
    if (StringUtils.isNotEmpty(suffix)) {
      documentNo.append(suffix);
    }
  }

}
