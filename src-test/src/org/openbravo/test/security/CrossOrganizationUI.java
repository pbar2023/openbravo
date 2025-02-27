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
 * All portions are Copyright (C) 2017-2022 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.security;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.test.base.OBBaseTest;
import org.openbravo.test.base.mock.HttpServletRequestMock;
import org.openbravo.test.base.mock.VariablesSecureAppMock;

/**
 * Tests to ensure different references don't apply organization filter when applied in a field that
 * allows cross organization references and apply them if it does not allow it.
 */
@RunWith(Parameterized.class)
public class CrossOrganizationUI extends OBBaseTest {
  private static final String ORDER_PRICELIST_FIELD = "1077";
  private static final String ORDER_ORGTRX_FIELD = "7038";

  private static final String ORDER_PRICELIST_COLUMN = "2204";
  private static final String ORDER_ORGTRX_COLUMN = "9331";

  private static final List<String> COLUMNS_TO_ALLOW_CROSS_ORG = Arrays
      .asList(ORDER_PRICELIST_COLUMN, ORDER_ORGTRX_COLUMN);

  private boolean useCrossOrgColumns;

  public CrossOrganizationUI(boolean useCrossOrgColumns) {
    this.useCrossOrgColumns = useCrossOrgColumns;
  }

  @Parameters(name = "Cross org refrence:{0}")
  public static Collection<Object[]> params() {
    return Arrays.asList(new Object[][] { //
        { true }, { false } });
  }

  @Test
  public void tableDirShouldAlwaysShowReferenceableOrgs() throws Exception {
    List<String> rows = getComboValues(OBDal.getInstance().get(Field.class, ORDER_PRICELIST_FIELD));
    assertThat(rows, hasItem("Tarifa de ventas"));
  }

  @Test
  public void tableDirShouldShowNonReferenceableOrgsIfAllowed() throws Exception {
    List<String> rows = getComboValues(OBDal.getInstance().get(Field.class, ORDER_PRICELIST_FIELD));
    if (useCrossOrgColumns) {
      assertThat(rows, hasItem("General Sales"));
    } else {
      assertThat(rows, not(hasItem("General Sales")));
    }
  }

  @Test
  public void tableShouldAlwaysShowReferenceableOrgs() throws Exception {
    List<String> rows = getComboValues(OBDal.getInstance().get(Field.class, ORDER_ORGTRX_FIELD));
    assertThat(rows, hasItem("F&B España - Región Norte - F&B España - Región Norte"));
  }

  @Test
  public void tableShouldShowNonReferenceableOrgsIfAllowed() throws Exception {
    List<String> rows = getComboValues(OBDal.getInstance().get(Field.class, ORDER_ORGTRX_FIELD));
    if (useCrossOrgColumns) {
      assertThat(rows, hasItem("F&B US West Coast - F&B US West Coast"));
    } else {
      assertThat(rows, not(hasItem("F&B US West Coast - F&B US West Coast")));
    }
  }

  @SuppressWarnings("serial")
  private List<String> getComboValues(Field field) throws Exception {
    DalConnectionProvider con = new DalConnectionProvider(false);
    HttpServletRequestMock
        .setRequestMockInRequestContext(new VariablesSecureAppMock(new HashMap<String, String>() {
          {
            // represents the organization of current record
            put("inpadOrgId", TEST_ORG_ID);
          }
        }));

    ComboTableData tableDirCombo = ComboTableData.getTableComboDataFor(field);
    List<String> rows = getRowsFromCombo(tableDirCombo.select(con, new HashMap<String, String>() {
      {
        put("ISSOTRX", "Y");
      }
    }, false, 0, 75));
    return rows;
  }

  private List<String> getRowsFromCombo(FieldProvider[] comboRows) {
    List<String> rows = new ArrayList<>();
    for (FieldProvider row : comboRows) {
      rows.add(row.getField("NAME"));
    }
    return rows;
  }

  @Before
  public void setUpAllowedCrossOrg() throws Exception {
    CrossOrganizationReference.setUpAllowedCrossOrg(COLUMNS_TO_ALLOW_CROSS_ORG, useCrossOrgColumns);
    setTestUserContext();
  }

  @AfterClass
  public static void resetAD() throws Exception {
    CrossOrganizationReference.setUpAllowedCrossOrg(COLUMNS_TO_ALLOW_CROSS_ORG, false);
    OBDal.getInstance().commitAndClose();
  }
}
