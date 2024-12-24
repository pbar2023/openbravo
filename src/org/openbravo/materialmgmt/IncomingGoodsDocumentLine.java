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
package org.openbravo.materialmgmt;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.OrganizationEnabled;
import org.openbravo.cache.TimeInvalidatedCache;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.PropertyNotFoundException;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.plm.Product;

/**
 * Used to validate lines of incoming goods documents (like purchase order) according to the
 * purchasing configuration.
 */

public abstract class IncomingGoodsDocumentLine<T extends BaseOBObject> {
  private static final Logger log = LogManager.getLogger();

  private static final String ANY_QTY_TYPE = "Any";
  private static final String EXACT_QTY_TYPE = "Exact";
  private static final String MULTIPLE_QTY_TYPE = "Multiple";
  protected static final String ENABLE_PO_VALIDATION_PREFERENCE = "EnableCheckPurchaseOrderLineQty";

  protected static final TimeInvalidatedCache<String, Boolean> PREFERENCE_VALUES = TimeInvalidatedCache
      .newBuilder()
      .name("Enable Purchase Assortment Validation Preference Values")
      .expireAfterDuration(Duration.ofMinutes(10))
      .build(IncomingGoodsDocumentLine::getPreferenceValue);

  protected static boolean getPreferenceValue(String key) {
    String[] keyParts = key.split("-");
    try {
      OBContext.setAdminMode(true);

      String value = Preferences.getPreferenceValue(keyParts[5], true, keyParts[0], keyParts[1],
          keyParts[2], keyParts[3], keyParts[4]);
      return Preferences.YES.equals(value);
    } catch (PropertyNotFoundException ex) {
      log.debug("Property " + keyParts[5] + " not found");
      return false;
    } catch (PropertyException ex) {
      log.error("Detected a conflict resolving the " + keyParts[5] + " preference value");
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Internal API: Do not use. Clears the cache with the values of the "Enable Purchase Assortment
   * Validation" preference.
   */
  public static void invalidateCache() {
    PREFERENCE_VALUES.invalidateAll();
  }

  private T line;

  /**
   * Creates a new PurchaseDocumentLine from the given {@link BaseOBObject}
   * 
   * @param line
   *          line of a purchase document
   * 
   * @throws IllegalArgumentException
   *           in case the line does not belong to a purchase document
   */
  protected IncomingGoodsDocumentLine(T line) {
    this.line = line;
    if (!isPurchaseDocument()) {
      throw new IllegalArgumentException("Not a purchase document line: " + line);
    }
  }

  /**
   * @return true if the line belongs to a purchase document or false otherwise
   */
  protected abstract boolean isPurchaseDocument();

  /**
   * @return the record with the information of the purchase document that the line belongs to
   */
  protected abstract OrganizationEnabled getDocumentHeader();

  /**
   * @return an optional describing the business partner of the document
   */
  protected abstract Optional<BusinessPartner> getBusinessPartner();

  /**
   * @return the product quantity of the line
   */
  protected abstract BigDecimal getLineQuantity();

  /**
   * @return the ID of the {@link Window} used to create the line. This is used to find the correct
   *         value for the preference that allows to skip the validation, in case it is defined at
   *         window level.
   */
  protected abstract String getWindowId();

  /**
   * @return the purchase document line
   */
  protected final T getLine() {
    return line;
  }

  /**
   * Validates the line
   * 
   * @throws PurchaseDocumentValidationError
   *           if the validation of the line fails
   */
  public abstract void validate();

  protected boolean isValidationEnabled(String preference) {
    OBContext ctx = OBContext.getOBContext();
    String key = ctx.getCurrentClient().getId() + "-" + ctx.getCurrentOrganization().getId() + "-"
        + ctx.getUser().getId() + "-" + ctx.getRole().getId() + "-" + getWindowId() + "-"
        + preference;

    return getPreferenceValue(key);
  }

  protected Product getLineProduct() {
    return (Product) getLine().get("product");
  }

  protected void validateLineQuantity(BigDecimal minQty, BigDecimal stdQty, String qtyType) {
    BigDecimal lineQty = getLineQuantity();
    if (isAnyType(qtyType)) {
      return;
    }
    if (isExactQtyType(qtyType) && lineQty.compareTo(stdQty) != 0) {
      throw new PurchaseDocumentValidationError("ValExactQty", new String[] { stdQty.toString() });
    } else if (isMultipleQtyType(qtyType) && lineQty.compareTo(minQty) < 0) {
      throw new PurchaseDocumentValidationError("ValMinQty", new String[] { minQty.toString() });
    } else if (isMultipleQtyType(qtyType)
        && lineQty.remainder(stdQty).compareTo(BigDecimal.ZERO) != 0) {
      throw new PurchaseDocumentValidationError("ValMultipleQty",
          new String[] { stdQty.toString() });
    }
  }

  protected boolean isAnyType(String qtyType) {
    return ANY_QTY_TYPE.equals(qtyType) || "A".equals(qtyType);
  }

  protected boolean isExactQtyType(String qtyType) {
    return EXACT_QTY_TYPE.equals(qtyType) || "E".equals(qtyType);
  }

  protected boolean isMultipleQtyType(String qtyType) {
    return MULTIPLE_QTY_TYPE.equals(qtyType) || "M".equals(qtyType);
  }
}
