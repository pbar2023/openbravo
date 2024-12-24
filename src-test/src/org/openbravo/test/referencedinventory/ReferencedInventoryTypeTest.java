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
package org.openbravo.test.referencedinventory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SequenceUtil.CalculationMethod;
import org.openbravo.erpCommon.utility.SequenceUtil.ControlDigit;
import org.openbravo.erpCommon.utility.SequenceUtil.SequenceNumberLength;
import org.openbravo.materialmgmt.refinventory.ContentRestriction;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryUtil;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryUtil.SequenceType;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventoryType;
import org.openbravo.test.documentsequence.SequenceTestUtils;

public class ReferencedInventoryTypeTest extends ReferencedInventoryTest {

  /**
   * compute Referenced Inventory Type with Sequence Type : None
   */

  @Test
  public void testReferencedInventoryType_None_ComputeSequence() {
    final ReferencedInventoryType refInvType = ReferencedInventoryTestUtils
        .createReferencedInventoryType(OBDal.getInstance().getProxy(Organization.class, "0"),
            SequenceType.NONE, null, ContentRestriction.ONLY_ITEMS);
    OBDal.getInstance().save(refInvType);
    OBDal.getInstance().flush();

    assertTrue("Referenced Inventory Type with Sequence Type - None is not created",
        refInvType != null);

    String strSequence = ReferencedInventoryUtil.getProposedValueFromSequence(refInvType.getId(),
        OBDal.getInstance()
            .getProxy(Organization.class, ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID)
            .getId(),
        true);
    assertThat(
        "Proposed Referenced Inventory Search Key with Sequence Type None is not computed properly using DAL",
        null, equalTo(strSequence));
  }

  /**
   * test Referenced Inventory Type with Sequence Type : Global
   */

  @Test
  public void testReferencedInventoryType_Global() {
    final ReferencedInventoryType referencedInventoryType = ReferencedInventoryTestUtils
        .createReferencedInventoryType(OBDal.getInstance().getProxy(Organization.class, "0"),
            SequenceType.GLOBAL, null, ContentRestriction.ONLY_ITEMS);
    OBDal.getInstance().save(referencedInventoryType);
    Exception exception = assertThrows(Exception.class, () -> OBDal.getInstance().flush());
    assertThat(exception.getMessage(), containsString("ConstraintViolationException"));
  }

  /**
   * test Referenced Inventory Type with Sequence Type : Per Organization
   */

  @Test
  public void testReferencedInventoryType_PerOrganization() {
    final ReferencedInventoryType referencedInventoryType = ReferencedInventoryTestUtils
        .createReferencedInventoryType(OBDal.getInstance().getProxy(Organization.class, "0"),
            SequenceType.PER_ORGANIZATION, null, ContentRestriction.ONLY_ITEMS);
    OBDal.getInstance().save(referencedInventoryType);
    OBDal.getInstance().flush();
    assertTrue("Referenced Inventory Type with Sequence Type - None is not created",
        referencedInventoryType != null);
  }

  /**
   * test Referenced Inventory Type with Sequence Type : None and Sequence not empty
   */

  @Test
  public void testReferencedInventoryType_None_Sequence() {
    final ReferencedInventoryType referencedInventoryType = ReferencedInventoryTestUtils
        .createReferencedInventoryType(OBDal.getInstance().getProxy(Organization.class, "0"),
            SequenceType.NONE, createSequence(), ContentRestriction.ONLY_ITEMS);
    OBDal.getInstance().save(referencedInventoryType);
    OBDal.getInstance().flush();
    assertTrue("Referenced Inventory Type with Sequence Type - None is set with Sequence",
        referencedInventoryType.getSequence() == null);
  }

  /**
   * test Referenced Inventory Type with Sequence Type : Per Organization and Sequence not empty
   */

  @Test
  public void testReferencedInventoryType_PerOrganization_Sequence() {
    final ReferencedInventoryType referencedInventoryType = ReferencedInventoryTestUtils
        .createReferencedInventoryType(OBDal.getInstance().getProxy(Organization.class, "0"),
            SequenceType.PER_ORGANIZATION, createSequence(), ContentRestriction.ONLY_ITEMS);
    OBDal.getInstance().save(referencedInventoryType);
    OBDal.getInstance().flush();
    assertTrue(
        "Referenced Inventory Type with Sequence Type - Per Organization is set with Sequence",
        referencedInventoryType.getSequence() == null);
  }

  /**
   * test Referenced Inventory Type with Sequence Type : Global and Sequence not empty
   */

  @Test
  public void testReferencedInventoryType_Global_WithSequence() {
    final ReferencedInventoryType referencedInventoryType = ReferencedInventoryTestUtils
        .createReferencedInventoryType(OBDal.getInstance().getProxy(Organization.class, "0"),
            SequenceType.GLOBAL, createSequence(), ContentRestriction.ONLY_ITEMS);
    OBDal.getInstance().save(referencedInventoryType);
    OBDal.getInstance().flush();
    assertTrue("Referenced Inventory Type with Sequence Type - Global is not set with Sequence",
        referencedInventoryType.getSequence() != null);
  }

  /**
   * compute sequence for Referenced Inventory Type with Sequence Type : Global and Sequence not
   * empty
   */

  @Test
  public void testReferencedInventoryType_Global_ComputeSequence() {

    final Sequence baseSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance()
            .getProxy(Organization.class, ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, "0110491", null, 2821L,
        null, null, ControlDigit.MODULE10, SequenceNumberLength.FIXED, 5L, false);
    OBDal.getInstance().save(baseSequence);
    final Sequence parentSequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance()
            .getProxy(Organization.class, ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.SEQUENCE, baseSequence, "6", null, null,
        null, "000", ControlDigit.MODULE10, SequenceNumberLength.VARIABLE, null, false);
    OBDal.getInstance().save(parentSequence);

    final ReferencedInventoryType refInvType = ReferencedInventoryTestUtils
        .createReferencedInventoryType(OBDal.getInstance().getProxy(Organization.class, "0"),
            SequenceType.GLOBAL, parentSequence, ContentRestriction.ONLY_ITEMS);
    OBDal.getInstance().save(refInvType);
    OBDal.getInstance().flush();
    final ReferencedInventory refInv = ReferencedInventoryTestUtils
        .createReferencedInventory(ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID, refInvType);
    assertThat("Referenced Inventory Search Key is not computed properly using DAL",
        "601104910282130002", equalTo(refInv.getSearchKey()));
  }

  /**
   * test Referenced Inventory Type with Sequence Type : Global and Update Sequence as empty
   */

  @Test
  public void testReferencedInventoryType_Global_WithoutSequence() {
    final ReferencedInventoryType referencedInventoryType = ReferencedInventoryTestUtils
        .createReferencedInventoryType(OBDal.getInstance().getProxy(Organization.class, "0"),
            SequenceType.GLOBAL, createSequence(), ContentRestriction.ONLY_ITEMS);
    OBDal.getInstance().save(referencedInventoryType);
    OBDal.getInstance().flush();

    referencedInventoryType.setSequence(null);
    OBDal.getInstance().save(referencedInventoryType);
    Exception exception = assertThrows(Exception.class, () -> OBDal.getInstance().flush());
    assertThat(exception.getMessage(), containsString("ConstraintViolationException"));
  }

  /**
   * Create sequence with calculation method auto numbering
   * 
   * @return Sequence to be used for defined Referenced Inventory Type
   */

  private Sequence createSequence() {
    final Sequence sequence = SequenceTestUtils.createDocumentSequence(
        OBDal.getInstance()
            .getProxy(Organization.class, ReferencedInventoryTestUtils.QA_SPAIN_ORG_ID),
        UUID.randomUUID().toString(), CalculationMethod.AUTONUMERING, null, null, null, null, null,
        null, ControlDigit.NONE, SequenceNumberLength.VARIABLE, null, false);
    OBDal.getInstance().save(sequence);
    return sequence;
  }
}
