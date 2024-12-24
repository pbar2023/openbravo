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
 * All portions are Copyright (C) 2024 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.event;

import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceUtil.CalculationMethod;
import org.openbravo.erpCommon.utility.SequenceUtil.ControlDigit;
import org.openbravo.erpCommon.utility.SequenceUtil.SequenceNumberLength;
import org.openbravo.model.ad.utility.Sequence;

/**
 * This class validates recursively that sequence being set as base sequence should not have
 * alphanumeric prefix/suffix values if it is configured in parent sequence having control digit as
 * Module 10.
 * 
 * Alternatively sequence that is already used as base sequence should not have alphanumeric prefix
 * and suffix values when the control digit is calculated using Module 10 algorithm in its parent
 * sequence.
 * 
 * Clear some fields based on the status of another field
 * 
 */
class ADSequenceEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Sequence.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    validateModule10Configuration(event);
    clearSequenceLengthIfNotFixedLength(event);
    clearBaseSequenceIfCalculationMethodIsNotBasedOnSequence(event);
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    // Skip validations when sequence is being consumed to improve performance
    if (isConsumingSequence(event)) {
      return;
    }

    validateNoInfiniteLoopsInBaseSequence(event);
    validateModule10Configuration(event);
    clearSequenceLengthIfNotFixedLength(event);
    clearBaseSequenceIfCalculationMethodIsNotBasedOnSequence(event);
  }

  /**
   * Returns true when the sequence is being updated because it is being consumed.
   * 
   * To check if we are consuming the sequence, it checks whether currentNumber == previousNumber +
   * incrementBy
   * 
   * Note that this is not accurate, as someone manually can do the same. In this case, if the user
   * bypasses the configuration checks, the sequence calculation might fail when consumed
   */
  private boolean isConsumingSequence(EntityUpdateEvent event) {
    if (CalculationMethod.AUTONUMERING.value.equals(
        event.getCurrentState(entities[0].getProperty(Sequence.PROPERTY_CALCULATIONMETHOD)))) {
      final Property sequenceNumberProperty = entities[0]
          .getProperty(Sequence.PROPERTY_NEXTASSIGNEDNUMBER);
      final Long previousNumber = (Long) event.getPreviousState(sequenceNumberProperty);
      final Long currentNumber = (Long) event.getCurrentState(sequenceNumberProperty);
      final Long incrementBy = (Long) event
          .getCurrentState(entities[0].getProperty(Sequence.PROPERTY_INCREMENTBY));

      return currentNumber == (previousNumber + incrementBy);
    }

    return false;
  }

  /**
   * Check whether sequence being used as current base sequence does not have sequence as its base
   * sequence to avoid infinite loop in the recursive check
   */
  private void validateNoInfiniteLoopsInBaseSequence(EntityUpdateEvent event) {
    final Property baseSequenceProperty = entities[0].getProperty(Sequence.PROPERTY_BASESEQUENCE);
    final Sequence currentBaseSequence = (Sequence) event.getCurrentState(baseSequenceProperty);
    if (currentBaseSequence != null) {
      checkForValidBaseSequence(currentBaseSequence.getId(),
          ((Sequence) event.getTargetInstance()).getId());
    }
  }

  private void checkForValidBaseSequence(String currentBaseSequenceId, String sequenceId) {
    if (StringUtils.equals(currentBaseSequenceId, sequenceId)) {
      throw new OBException(OBMessageUtils.messageBD("NotValidBaseSequence"));
    }
    Sequence sequence = OBDal.getInstance().get(Sequence.class, currentBaseSequenceId);
    // Base case: sequence is null or it doesn't have a base sequence
    if (sequence == null || sequence.getBaseSequence() == null) {
      return;
    }
    // Recursive case: sequence has a base sequence
    checkForValidBaseSequence(sequence.getBaseSequence().getId(), sequenceId);
  }

  /**
   * Ensure configuration allows to perform Module 10 control digit.
   * 
   * It recursively validates the MOD10 configuration only if this sequence or any base sequence in
   * the child tree defines alphanumeric prefix/suffix. In this case it validates recursively that
   * this sequence, or any other sequence whose base parent is this sequence, is not configured to
   * work with MOD10 control digit.
   */
  private void validateModule10Configuration(final EntityPersistenceEvent event) {
    if (isMod10ControlDigit(event) && isAlphanumericPrefixOrSuffix(event)) {
      // Check this sequence
      throw new OBException(OBMessageUtils.messageBD("ValidateSequence"));
    }

    // Check any sequence in the natural tree
    final Sequence thisSequence = (Sequence) event.getTargetInstance();
    final Sequence anySequenceWithAlphanumericPrefixOrSuffix = anyBaseSequenceHasAlphanumericPrefixOrSuffix(
        thisSequence);
    if (anySequenceWithAlphanumericPrefixOrSuffix != null) {
      checkNoParentSequenceHasMod10(thisSequence,
          anySequenceWithAlphanumericPrefixOrSuffix.getIdentifier());
    }
  }

  private boolean isMod10ControlDigit(final EntityPersistenceEvent event) {
    final Property controlDigitProperty = entities[0].getProperty(Sequence.PROPERTY_CONTROLDIGIT);
    final String currentControlDigit = (String) event.getCurrentState(controlDigitProperty);
    return isMod10ControlDigit(currentControlDigit);
  }

  private boolean isMod10ControlDigit(final String controlDigit) {
    return ControlDigit.MODULE10.value.equals(controlDigit);
  }

  private boolean isAlphanumericPrefixOrSuffix(EntityPersistenceEvent event) {
    return isAlphaNumeric(
        (String) event.getCurrentState(entities[0].getProperty(Sequence.PROPERTY_PREFIX)))
        || isAlphaNumeric(
            (String) event.getCurrentState(entities[0].getProperty(Sequence.PROPERTY_SUFFIX)));
  }

  private Sequence anyBaseSequenceHasAlphanumericPrefixOrSuffix(final Sequence sequence) {
    if (sequence == null) {
      return null;
    }
    if (isAlphaNumeric(sequence.getPrefix()) || isAlphaNumeric(sequence.getSuffix())) {
      return sequence;
    }
    // Recursively check the base sequences
    return anyBaseSequenceHasAlphanumericPrefixOrSuffix(sequence.getBaseSequence());
  }

  /**
   * check recursively all parent sequences does not have Module 10 control digit
   */
  private void checkNoParentSequenceHasMod10(final Sequence thisSequence,
      String baseSequenceIdentifier) {
    if (isMod10ControlDigit(thisSequence.getControlDigit())) {
      throw new OBException(OBMessageUtils.getI18NMessage("ValidateBaseSequenceMod10",
          new String[] { baseSequenceIdentifier, thisSequence.getIdentifier() }));
    }

    getAllParentSequence(thisSequence).stream()
        .forEach(parentSequence -> checkNoParentSequenceHasMod10(parentSequence,
            baseSequenceIdentifier));
  }

  /**
   * This method gives the list of sequence that has input parameter sequence as base sequence
   *
   * @param sequence
   *          Input Sequence
   * @return list of sequences that have sequence as its base sequence
   */
  private List<Sequence> getAllParentSequence(Sequence sequence) {
    OBCriteria<Sequence> seqCriteria = OBDal.getInstance().createCriteria(Sequence.class);
    seqCriteria.add(Restrictions.eq(Sequence.PROPERTY_BASESEQUENCE + ".id", sequence.getId()));
    seqCriteria.add(Restrictions.ne(Sequence.PROPERTY_ID, sequence.getId()));
    seqCriteria.addOrderBy(Sequence.PROPERTY_ID, true); // To get same error message afterwards
    return seqCriteria.list();
  }

  private boolean isAlphaNumeric(final String str) {
    return str != null && !StringUtils.isNumeric(str);
  }

  /**
   * This method clears the Sequence Length in Sequence if Sequence Type is not Fix Length
   */
  private void clearSequenceLengthIfNotFixedLength(EntityPersistenceEvent event) {
    final Entity sequenceEntity = ModelProvider.getInstance().getEntity(Sequence.ENTITY_NAME);
    final Property sequenceNumberLengthProperty = sequenceEntity
        .getProperty(Sequence.PROPERTY_SEQUENCENUMBERLENGTH);
    final Property sequenceLengthProperty = sequenceEntity
        .getProperty(Sequence.PROPERTY_SEQUENCELENGTH);
    String currentSequenceNumberLength = (String) event
        .getCurrentState(sequenceNumberLengthProperty);
    Long currentSequenceLength = (Long) event.getCurrentState(sequenceLengthProperty);

    if (!StringUtils.equals(currentSequenceNumberLength, SequenceNumberLength.FIXED.value)
        && currentSequenceLength != null) {
      event.setCurrentState(sequenceLengthProperty, null);
    }
  }

  /**
   * This method clears the Base Sequence in Sequence if Calculation Method is not Based on
   * Sequence.
   */
  private void clearBaseSequenceIfCalculationMethodIsNotBasedOnSequence(
      EntityPersistenceEvent event) {
    final Entity sequenceEntity = ModelProvider.getInstance().getEntity(Sequence.ENTITY_NAME);
    final Property baseSequenceProperty = sequenceEntity
        .getProperty(Sequence.PROPERTY_BASESEQUENCE);
    final Property calculationMethodProperty = sequenceEntity
        .getProperty(Sequence.PROPERTY_CALCULATIONMETHOD);
    Sequence currentBaseSequence = (Sequence) event.getCurrentState(baseSequenceProperty);
    String currentSequenceType = (String) event.getCurrentState(calculationMethodProperty);

    if (!StringUtils.equals(currentSequenceType, CalculationMethod.SEQUENCE.value)
        && currentBaseSequence != null) {
      event.setCurrentState(baseSequenceProperty, null);
    }
  }
}
