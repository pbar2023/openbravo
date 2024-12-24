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
 * All portions are Copyright (C) 2023 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.Location;

public class SE_BPartner_Utils {

  public static Location getDefaultLocation(String bpartnerId, boolean isShipping) {
    final OBCriteria<?> criteria = OBDal.getInstance().createCriteria(Location.class);
    criteria.add(Restrictions.eq(Location.PROPERTY_BUSINESSPARTNER + ".id", bpartnerId));
    criteria.add(Restrictions.eq(Location.PROPERTY_ACTIVE, true));
    if (isShipping) {
      criteria.add(Restrictions.eq(Location.PROPERTY_DEFAULTSHIPTOADDRESS, true));
    } else {
      criteria.add(Restrictions.eq(Location.PROPERTY_DEFAULTINVOICETOADDRESS, true));
    }
    criteria.setMaxResults(1);
    return (Location) criteria.uniqueResult();
  }
}
