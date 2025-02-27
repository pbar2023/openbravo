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

import org.openbravo.base.exception.OBException;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * Used to throw validation errors related with the purchase assortment or purchasing configuration
 */
@SuppressWarnings("serial")
public class PurchaseDocumentValidationError extends OBException {

  /**
   * Creates a new PurchaseDocumentValidationError instance
   * 
   * @param adMessage
   *          the search key of the AD message with the error text
   */
  PurchaseDocumentValidationError(String adMessage) {
    super(OBMessageUtils.getI18NMessage(adMessage));
  }

  /**
   * Creates a new PurchaseDocumentValidationError instance
   * 
   * @param adMessage
   *          the search key of the AD message with the error text
   * @param parameters
   *          the message parameters
   */
  public PurchaseDocumentValidationError(String adMessage, String[] parameters) {
    super(OBMessageUtils.getI18NMessage(adMessage, parameters));
  }
}
