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
package org.openbravo.materialmgmt.refinventory;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;
import org.openbravo.synchronization.event.SynchronizationEvent;

/**
 * In charge of updating the status of a handling unit and triggering the
 * API_HandlingUnitStatusChange push API event.
 */
@ApplicationScoped
public class ReferencedInventoryStatusProcessor {
  private static final Logger log = LogManager.getLogger();

  public enum ReferencedInventoryStatus {
    OPEN, CLOSED, DESTROYED;

    private boolean isStatusOf(ReferencedInventory handlingUnit) {
      return name().equals(handlingUnit.getStatus());
    }

    private static boolean isClosed(ReferencedInventory handlingUnit) {
      return CLOSED.isStatusOf(handlingUnit);
    }

    private static boolean isNotDestroyed(ReferencedInventory handlingUnit) {
      return !isDestroyed(handlingUnit);
    }

    private static boolean isDestroyed(ReferencedInventory handlingUnit) {
      return DESTROYED.isStatusOf(handlingUnit);
    }
  }

  /**
   * Sets the status of a handling unit and to its child handling units in cascade
   * 
   * @param handlingUnit
   *          the handling unit
   * @param newStatus
   *          the new status to be set
   * @throws OBException
   *           if the handling unit is destroyed or if the parent of the handling unit is closed
   */
  public void changeStatus(ReferencedInventory handlingUnit,
      ReferencedInventoryStatus newStatus) {
    if (newStatus.isStatusOf(handlingUnit)) {
      log.warn("Skipping status change. The current status of the handling unit {} is already {}",
          handlingUnit.getSearchKey(), newStatus);
      return;
    }
    checkIsDestroyed(handlingUnit);
    checkIsAnyAncestorClosed(handlingUnit);
    changeStatusInCascade(handlingUnit, newStatus);
    triggerHandlingUnitStatusChangeEvent(handlingUnit);
  }

  private void checkIsDestroyed(ReferencedInventory handlingUnit) {
    if (ReferencedInventoryStatus.isDestroyed(handlingUnit)) {
      log.error("Cannot change the status of the handling unit {} because it is destroyed",
          handlingUnit.getSearchKey());
      throw new OBException(OBMessageUtils.getI18NMessage("HandlingUnitIsDestroyed"));
    }
  }

  private void checkIsAnyAncestorClosed(ReferencedInventory handlingUnit) {
    findClosedAncestor(handlingUnit).ifPresent(ancestor -> {
      throw new OBException(OBMessageUtils.getI18NMessage("ParentHandlingUnitIsClosed",
          new String[] { handlingUnit.getSearchKey(), ancestor.getSearchKey() }));
    });
  }

  private Optional<ReferencedInventory> findClosedAncestor(ReferencedInventory handlingUnit) {
    ReferencedInventory parent = handlingUnit.getParentRefInventory();
    if (parent == null) {
      return Optional.empty();
    }
    if (ReferencedInventoryStatus.isClosed(parent)) {
      return Optional.of(parent);
    } else {
      return findClosedAncestor(parent);
    }
  }

  private void changeStatusInCascade(ReferencedInventory handlingUnit,
      ReferencedInventoryStatus status) {
    handlingUnit.setStatus(status.name());
    ReferencedInventoryUtil.getDirectChildReferencedInventories(handlingUnit)
        .filter(ReferencedInventoryStatus::isNotDestroyed)
        .forEach(child -> changeStatusInCascade(child, status));
  }

  private void triggerHandlingUnitStatusChangeEvent(ReferencedInventory handlingUnit) {
    SynchronizationEvent.getInstance()
        .triggerEvent("API_HandlingUnitStatusChange", handlingUnit.getId());
  }
}
