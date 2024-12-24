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
 * All portions are Copyright (C) 2022 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.materialmgmt;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.service.OBDal;

/**
 * A check to detect if there are centrally maintained fields based in relevant characteristic
 * properties whose terminology has not been synchronized.
 */
public class RelevantCharacteristicFieldTerminologyChecker extends WeldBaseTest {

  @Before
  public void init() {
    setSystemAdministratorContext();
    setModulesInDevelopment();
  }

  @After
  public void reset() {
    OBDal.getInstance().rollbackAndClose();
  }

  @Test
  public void checkMissingTerminologySynchronization() {
    WeldUtils
        .getInstanceFromStaticBeanManager(RelevantCharacteristicFieldTerminologySynchronizer.class)
        .validate();
  }

  private void setModulesInDevelopment() {
    String hql = "update ADModule set inDevelopment = true";
    OBDal.getInstance().getSession().createQuery(hql).executeUpdate();
    OBDal.getInstance().flush();
  }
}
