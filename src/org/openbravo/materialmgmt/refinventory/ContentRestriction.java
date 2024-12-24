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

import java.util.Arrays;

import org.openbravo.base.exception.OBException;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventoryType;

/**
 * Available values for Referenced Inventory Type's Content Restrictions.
 * 
 * {@link ReferencedInventoryType#PROPERTY_CONTENTRESTRICTION}
 */
public enum ContentRestriction {
  ONLY_ITEMS("I"), ONLY_REFINVENTORIES("RI"), BOTH_ITEMS_OR_REFINVENTORIES("IRI");

  public final String value;

  private ContentRestriction(final String value) {
    this.value = value;
  }

  public static ContentRestriction getByValue(String searchKey) {
    return Arrays.stream(ContentRestriction.values())
        .filter(e -> searchKey != null && searchKey.equals(e.value))
        .findFirst()
        .orElseThrow(() -> new OBException(
            "Invalid Handling Unit Type Content Restriction searchKey: " + searchKey));
  }
}
