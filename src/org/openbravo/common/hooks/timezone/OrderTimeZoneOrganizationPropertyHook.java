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
package org.openbravo.common.hooks.timezone;

import org.openbravo.base.Entity;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.model.common.order.Order;

/**
 * Allows to implement the exception to get the organization property used to calculate the time
 * zone based fields of the orders: if the order has the transaction organization property defined
 * the organization to be used is the one referenced by the {@link Order#PROPERTY_TRXORGANIZATION}
 * property.
 */
@Entity(Order.class)
public class OrderTimeZoneOrganizationPropertyHook implements TimeZoneOrganizationPropertyHook {

  @Override
  public String getOrganizationProperty(BaseOBObject bob) {
    return bob.get(Order.PROPERTY_TRXORGANIZATION) != null ? Order.PROPERTY_TRXORGANIZATION
        : Order.PROPERTY_ORGANIZATION;
  }
}
