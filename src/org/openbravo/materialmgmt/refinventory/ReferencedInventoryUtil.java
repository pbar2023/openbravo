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
 * All portions are Copyright (C) 2017-2024 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.materialmgmt.refinventory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.util.Check;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.utility.SequenceUtil;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventoryType;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventoryTypeOrgSequence;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;

/**
 * Utility class for Referenced Inventory feature
 *
 */
public class ReferencedInventoryUtil {
  public static final String REFERENCEDINVENTORYPREFIX = "[";
  public static final String REFERENCEDINVENTORYSUFFIX = "]";

  public enum SequenceType {
    GLOBAL("G"), NONE("N"), PER_ORGANIZATION("P");

    public final String value;

    SequenceType(String value) {
      this.value = value;
    }
  }

  /**
   * Create and return a new AttributeSetInstance from the given originalAttributeSetInstance and
   * link it to the given referencedInventory
   */
  public static final AttributeSetInstance cloneAttributeSetInstance(
      final AttributeSetInstance _originalAttributeSetInstance,
      final ReferencedInventory referencedInventory) {
    final AttributeSetInstance originalAttributeSetInstance = _originalAttributeSetInstance == null
        ? OBDal.getInstance().get(AttributeSetInstance.class, "0")
        : _originalAttributeSetInstance;

    final AttributeSetInstance newAttributeSetInstance = (AttributeSetInstance) DalUtil
        .copy(originalAttributeSetInstance, false);
    newAttributeSetInstance.setActive(true);
    newAttributeSetInstance.setClient(referencedInventory.getClient());
    newAttributeSetInstance.setOrganization(originalAttributeSetInstance.getOrganization());
    newAttributeSetInstance.setParentAttributeSetInstance(originalAttributeSetInstance);
    newAttributeSetInstance.setReferencedInventory(referencedInventory);
    newAttributeSetInstance.setDescription(getAttributeSetInstanceDescriptionForReferencedInventory(
        newAttributeSetInstance.getDescription(), referencedInventory));
    OBDal.getInstance().save(newAttributeSetInstance);
    return newAttributeSetInstance;
  }

  /**
   * Returns an AttributeSetInstance previously created from the given _originalAttributeSetInstance
   * and referenced inventory. If not found returns null.
   */
  public static final AttributeSetInstance getAlreadyClonedAttributeSetInstance(
      final AttributeSetInstance _originalAttributeSetInstance,
      final ReferencedInventory referencedInventory) {
    try {
      OBContext.setAdminMode(true);
      final AttributeSetInstance originalAttributeSetInstance = _originalAttributeSetInstance == null
          ? OBDal.getInstance().getProxy(AttributeSetInstance.class, "0")
          : _originalAttributeSetInstance;

      final OBCriteria<AttributeSetInstance> criteria = OBDao.getFilteredCriteria(
          AttributeSetInstance.class,
          Restrictions.eq(AttributeSetInstance.PROPERTY_PARENTATTRIBUTESETINSTANCE + ".id",
              originalAttributeSetInstance.getId()),
          Restrictions.eq(AttributeSetInstance.PROPERTY_REFERENCEDINVENTORY + ".id",
              referencedInventory.getId()));
      criteria.setMaxResults(1);
      return criteria.list().get(0);
    } catch (final Exception notFound) {
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Generates a description with the originalDesc +
   * {@value org.openbravo.materialmgmt.refinventory.ReferencedInventoryUtil#REFERENCEDINVENTORYPREFIX}
   * + referenced Inventory search key +
   * {@value org.openbravo.materialmgmt.refinventory.ReferencedInventoryUtil#REFERENCEDINVENTORYSUFFIX}
   */
  public static final String getAttributeSetInstanceDescriptionForReferencedInventory(
      final String originalDesc, final ReferencedInventory referencedInventory) {
    return StringUtils
        .left((StringUtils.isBlank(originalDesc) ? "" : originalDesc) + REFERENCEDINVENTORYPREFIX
            + referencedInventory.getSearchKey() + REFERENCEDINVENTORYSUFFIX, 255);
  }

  /**
   * Returns the parent attribute set instance for the given storage detail. If not found it returns
   * null
   */
  @Deprecated
  public static final AttributeSetInstance getParentAttributeSetInstance(
      final StorageDetail storageDetail) {
    try {
      return storageDetail.getAttributeSetValue().getParentAttributeSetInstance();
    } catch (NullPointerException noParentFound) {
      return null;
    }
  }

  /**
   * Gets the outermost referenced inventory for the given storage detail. It might be null when the
   * stock is not inside a referenced inventory
   * 
   * Note that this information is actually stored in the storage detail's attribute set instance
   * itself.
   */
  public static final ReferencedInventory getOutermostRefInventory(
      final StorageDetail storageDetail) {
    return storageDetail.getAttributeSetValue().getReferencedInventory();
  }

  /**
   * Gets the innermost referenced inventory for the given storage detail. It might be null when the
   * stock is not inside a referenced inventory
   * 
   * Note that this information is actually stored in the storage detail's referenced inventory
   * directly.
   */
  public static final ReferencedInventory getInnermostRefInventory(
      final StorageDetail storageDetail) {
    return storageDetail.getReferencedInventory();
  }

  /**
   * Gets the innermost referenced inventory for the attribute set instance. It might be null when
   * the stock is not inside a referenced inventory
   * 
   * This method uses the tree structure available in {@link AttributeSetInstance} instead of taking
   * it directly from the storage detail, as it does the
   * {@link #getInnermostRefInventory(StorageDetail)} method.
   * 
   * Use this method if the referenced inventory linked to the storage detail might not be accurate
   * during the transaction (for example during an unboxing activity).
   * 
   */
  public static final ReferencedInventory getInnermostRefInventory(
      final AttributeSetInstance attributeSetInstance) {
    try {
      AttributeSetInstance innerMostASI = attributeSetInstance;
      while (innerMostASI.getParentAttributeSetInstance().getReferencedInventory() != null) {
        innerMostASI = innerMostASI.getParentAttributeSetInstance();
      }
      return innerMostASI.getReferencedInventory();
    } catch (NullPointerException notPartOfAReferencedInventory) {
      return null;
    }
  }

  /**
   * This method is used only for unboxing activities. It returns the right attribute set instance
   * ready to associate with the storage detail.
   * 
   * @param storageDetail
   *          this is the storage detail being unboxed
   * @param referencedInventoryToKeep
   *          If the parameter is null, it returns the inner most attribute set instance, i.e. the
   *          original attribute set instance cloned before the very first box. It is used when we
   *          want to fully unbox the storage detail outside any referenced inventory.
   * 
   *          If it is not null, it returns the attribute set instance, for the given storage
   *          detail, linked to the given referenced inventory. It is used when we want to keep the
   *          storage detail inside a referenced inventory tree, but the referencedInventoryToKeep
   *          is being unboxed from its parent(s) referenced inventories.
   */
  private static final AttributeSetInstance getInnerAttributeSetInstance(
      final StorageDetail storageDetail, final ReferencedInventory referencedInventoryToKeep) {
    AttributeSetInstance innerMostASI = storageDetail.getAttributeSetValue();
    while (innerMostASI.getParentAttributeSetInstance() != null //
        && (referencedInventoryToKeep == null || !innerMostASI.getReferencedInventory()
            .getId()
            .equals(referencedInventoryToKeep.getId()))) {
      innerMostASI = innerMostASI.getParentAttributeSetInstance();
    }
    return innerMostASI;
  }

  /**
   * Gets the right attribute set instance to associate to the storage detail when unboxing.
   * 
   * @param storageDetail
   *          storage detail to unbox
   * @param referencedInventoryToKeep
   *          there are two possibilities:
   *          <ul>
   *          <li>referencedInventoryToKeep is not null: the storage detail must be kept in the
   *          given referenced inventory (and any inner referenced inventory), but it is being
   *          unboxed from any outer referenced inventory.
   * 
   *          For example, we have a stock inside a [SmallBox], which is inside
   *          [MediumBox][BigBox][Pallet]. We want to unbox [BigBox] as a box outside [Pallet]. In
   *          this case the stock will remain inside [SmallBox][MediumBox][BigBox], but not anymore
   *          inside [Pallet].
   * 
   *          The parameter in this example would be [BigBox].</li>
   * 
   *          <li>referencedInventoryToKeep is null: the storage detail is unboxed outside of any
   *          referenced inventory.
   * 
   *          In the above example it means that the stock originally in [SmallBox] will be unboxed
   *          outside [Pallet]</li>
   * 
   *          <li>Exceptionally, if the referencedInventoryToKeep is not null, but it is already the
   *          outermost referenced inventory, then the attributed set instance returned is the same
   *          as when this parameter is null.
   * 
   *          For example, if we have a stock inside [Pallet], and [Pallet] is the outermost
   *          referenced inventory, then the unbox process should always extract the stock from
   *          [Pallet]</li>
   *          </ul>
   */
  public static final AttributeSetInstance getAttributeSetInstanceTo(
      final StorageDetail storageDetail, final ReferencedInventory referencedInventoryToKeep) {
    Check.isNotNull(storageDetail.getReferencedInventory(),
        "Storage detail is not linked to a referenced inventory. "
            + "This method only accepts storage details within a referenced inventory.");

    final boolean isStorageDetailInTheOuterMostRI = storageDetail.getReferencedInventory()
        .getParentRefInventory() == null;

    if (referencedInventoryToKeep == null || isStorageDetailInTheOuterMostRI) {
      // Unbox from all the boxes
      return ReferencedInventoryUtil.getInnerAttributeSetInstance(storageDetail, null);
    } else {
      // Unbox the selected box, but keep the stock inside
      return ReferencedInventoryUtil.getInnerAttributeSetInstance(storageDetail,
          referencedInventoryToKeep);
    }
  }

  /**
   * Returns the parent referenced inventories of the given one. If includeRefInventory, the given
   * one is included in the returned collection
   */
  public static final Collection<ReferencedInventory> getParentReferencedInventories(
      final ReferencedInventory refInventory, boolean includeRefInventory) {
    final Collection<ReferencedInventory> parentList = new ArrayList<>();
    if (includeRefInventory) {
      parentList.add(refInventory);
    }

    ReferencedInventory parentRI = refInventory.getParentRefInventory();
    while (parentRI != null) {
      parentList.add(parentRI);
      parentRI = parentRI.getParentRefInventory();
    }
    return parentList;
  }

  /**
   * Retrieves the IDs of the referenced inventories that are direct children of the given
   * referenced inventory
   * 
   * @param refInventory
   *          the referenced inventory
   * 
   * @return a Stream containing the referenced inventories that are direct children of the given
   *         referenced inventory
   */
  public static final Stream<ReferencedInventory> getDirectChildReferencedInventories(
      ReferencedInventory refInventory) {
    return OBDal.getInstance()
        .createQuery(ReferencedInventory.class, "where parentRefInventory.id = :parent")
        .setNamedParameter("parent", refInventory.getId())
        .stream();
  }

  /**
   * If the given referenced inventory type id is associated to a sequence, it then return the next
   * value in that sequence. Otherwise returns null.
   * 
   * @param referencedInventoryTypeId
   *          Referenced Inventory Type Id used to get its sequence
   * @param updateNext
   *          if true updates the sequence's next value in database
   * 
   * @deprecated this method doesn't support the Sequence Type combo, so the new "Per Organization"
   *             feature is not supported. Please use instead
   *             {@link #getProposedValueFromSequence(String, String, boolean)}
   */
  @Deprecated
  public static String getProposedValueFromSequenceOrNull(final String referencedInventoryTypeId,
      final boolean updateNext) {
    if (StringUtils.isBlank(referencedInventoryTypeId)) {
      return null;
    } else {
      return SequenceUtil.getDocumentNo(updateNext,
          OBDal.getInstance()
              .getProxy(ReferencedInventoryType.class, referencedInventoryTypeId)
              .getSequence());
    }
  }

  /**
   * If the given referenced inventory type id is associated to a sequence (either with a Sequence
   * Type Global or Per Organization), it then return the next value in that sequence. If sequence
   * type is None, it returns null.
   * 
   * @param referencedInventoryTypeId
   *          Referenced Inventory Type Id used to get its sequence
   * @param orgId
   *          Organization Id of the referenced inventory. It is used when Sequence Type is Per
   *          Organization
   * @param updateNext
   *          if true updates the sequence's next value in database
   */
  public static String getProposedValueFromSequence(final String referencedInventoryTypeId,
      final String orgId, final boolean updateNext) {
    if (StringUtils.isBlank(referencedInventoryTypeId)) {
      return null;
    }
    return SequenceUtil.getDocumentNo(updateNext, getSequence(referencedInventoryTypeId, orgId));
  }

  /**
   * Returns the sequence associated to the given referenced inventory type id if Sequence Type is
   * Global or from Organization Sequence in case Sequence Type is Per Organization
   */
  private static Sequence getSequence(final String referencedInventoryTypeId, final String orgId) {
    final ReferencedInventoryType refInventoryType = OBDal.getInstance()
        .getProxy(ReferencedInventoryType.class, referencedInventoryTypeId);
    final String riSequenceType = refInventoryType.getSequenceType();

    if (SequenceType.GLOBAL.value.equals(riSequenceType)) {
      return refInventoryType.getSequence();
    } else if (SequenceType.PER_ORGANIZATION.value.equals(riSequenceType)) {
      return ReferencedInventoryUtil.getPerOrganizationSequence(referencedInventoryTypeId, orgId);
    } else if (SequenceType.NONE.value.equals(riSequenceType)) {
      return null;
    } else {
      throw new OBException("Sequence Type not supported: " + riSequenceType, true);
    }
  }

  private static Sequence getPerOrganizationSequence(final String referencedInventoryTypeId,
      String orgId) {
    try {
      OBContext.setAdminMode(true);
      final OBCriteria<ReferencedInventoryTypeOrgSequence> criteria = OBDao.getFilteredCriteria(
          ReferencedInventoryTypeOrgSequence.class,
          Restrictions.eq(
              ReferencedInventoryTypeOrgSequence.PROPERTY_REFERENCEDINVENTORYTYPE + ".id",
              referencedInventoryTypeId),
          Restrictions.eq(ReferencedInventoryTypeOrgSequence.PROPERTY_ORGANIZATION + ".id", orgId));
      ReferencedInventoryTypeOrgSequence orgSeq = (ReferencedInventoryTypeOrgSequence) criteria
          .uniqueResult();
      return orgSeq != null ? orgSeq.getSequence() : null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Throw an exception if the given attribute set instance is linked to a referenced inventory
   */
  public static void avoidUpdatingIfLinkedToReferencedInventory(
      final String attributeSetInstanceId) {
    try {
      OBContext.setAdminMode(true);
      final AttributeSetInstance attributeSetInstance = OBDal.getInstance()
          .getProxy(AttributeSetInstance.class, attributeSetInstanceId);
      if (attributeSetInstance.getParentAttributeSetInstance() != null
          || !attributeSetInstance.getAttributeSetInstanceParentAttributeSetInstanceIDList()
              .isEmpty()) {
        throw new OBException("@RefInventoryAvoidUpdatingAttribute@");
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  static InternalMovement createAndSaveGoodsMovementHeader(final Organization organization,
      final String name) {
    final InternalMovement header = OBProvider.getInstance().get(InternalMovement.class);
    header.setClient(OBContext.getOBContext().getCurrentClient());
    header.setOrganization(organization);
    header.setName(name);
    header.setMovementDate(DateUtils.truncate(new Date(), Calendar.DATE));
    OBDal.getInstance().save(header);
    return header;
  }

  static InternalMovementLine createAndSaveMovementLine(final InternalMovement internalMovement,
      final BigDecimal movementQty, final Locator newStorageBin,
      final AttributeSetInstance newAttributeSetInstance, final long lineNo,
      final StorageDetail storageDetail, final Reservation reservation) {
    final InternalMovementLine line = OBProvider.getInstance().get(InternalMovementLine.class);
    line.setClient(internalMovement.getClient());
    line.setOrganization(internalMovement.getOrganization());
    line.setLineNo(lineNo);
    line.setProduct(storageDetail.getProduct());
    line.setMovementQuantity(movementQty);
    line.setUOM(storageDetail.getProduct().getUOM());
    line.setAttributeSetValue(storageDetail.getAttributeSetValue());
    line.setStorageBin(storageDetail.getStorageBin());
    line.setNewStorageBin(newStorageBin);
    line.setMovement(internalMovement);
    line.setAttributeSetInstanceTo(newAttributeSetInstance);
    line.setStockReservation(reservation);
    internalMovement.getMaterialMgmtInternalMovementLineList().add(line);
    OBDal.getInstance().save(line);
    return line;
  }

  static boolean isGreaterThanZero(final BigDecimal qty) {
    return qty.compareTo(BigDecimal.ZERO) > 0;
  }

  /**
   * Returns a ScrollableResults with the available stock reservations for the given storage detail
   * that can be boxed to the given newStorageBin. They are ordered by first non-allocated, without
   * a defined attribute set instance at reservation header first and with the lower reserved
   * quantity
   */
  public static ScrollableResults getAvailableStockReservations(final StorageDetail storageDetail,
      final Locator newStorageBin) {
    Check.isNotNull(storageDetail, "storageDetail parameter can't be null");
    final String olHql = "select sr, sr.quantity - sr.released " + //
        "from MaterialMgmtReservationStock sr " + //
        "join sr.reservation res " + //
        "where coalesce(sr.storageBin.id, res.storageBin.id) = :sdBinId " + //
        // Skip reservations forced to a bin different from the destination bin
        "and (res.storageBin.id is null or res.storageBin.id = :toBindId) " + //
        "and coalesce(sr.attributeSetValue.id, res.attributeSetValue.id) = :sdAttributeSetId " + //
        "and sr.quantity - sr.released > 0 " + //
        "and res.product.id = :productId " + //
        "and res.uOM.id = :uomId " + //
        "and res.rESStatus = 'CO' " + //
        "order by case when sr.allocated = 'Y' then 1 else 0 end, " + //
        "      case when res.attributeSetValue.id is not null then 1 else 0 end, " + //
        "      sr.quantity - sr.released asc  ";
    final Session session = OBDal.getInstance().getSession();
    final Query<Object[]> sdQuery = session.createQuery(olHql, Object[].class);
    sdQuery.setParameter("sdBinId", storageDetail.getStorageBin().getId());
    sdQuery.setParameter("toBindId",
        newStorageBin != null ? newStorageBin.getId() : "noStorageBinToIDShouldMatch");
    sdQuery.setParameter("sdAttributeSetId", storageDetail.getAttributeSetValue().getId());
    sdQuery.setParameter("productId", storageDetail.getProduct().getId());
    sdQuery.setParameter("uomId", storageDetail.getUOM().getId());
    sdQuery.setFetchSize(1000);
    return sdQuery.scroll(ScrollMode.FORWARD_ONLY);
  }

  /**
   * Returns a ScrollableResults of storage details included in the given referenced inventory id
   * ordered from the innermost RIs to the outermost.
   * 
   * @param refInventoryId
   *          return the stock (items) included in this referenced inventory id
   * @param includeNestedRefInventories
   *          if true returns also the stock (items) in the nested referenced inventories
   */
  public static ScrollableResults getStorageDetails(final String refInventoryId,
      boolean includeNestedRefInventories) {
    Check.isNotNull(refInventoryId, "refInventoryId parameter can't be null");
    // @formatter:off
    final String hql = "select sd "
        + "from MaterialMgmtStorageDetail sd "
        + "where sd.referencedInventory.id = "
        + (includeNestedRefInventories
            ? " any(select unnest(m_refinventory_nested(ri.id)) "
                + " from MaterialMgmtReferencedInventory ri "
                + " where ri.id = :refInventoryId "
                + ") "
            : " :refInventoryId ") 
        + "order by sd.referencedInventory._computedColumns.nestedReferencedInventoriesCount asc ";
    // @formatter:on
    final Session session = OBDal.getInstance().getSession();
    final Query<StorageDetail> sdQuery = session.createQuery(hql, StorageDetail.class);
    sdQuery.setParameter("refInventoryId", refInventoryId);
    sdQuery.setFetchSize(1000);
    return sdQuery.scroll(ScrollMode.FORWARD_ONLY);
  }
}
