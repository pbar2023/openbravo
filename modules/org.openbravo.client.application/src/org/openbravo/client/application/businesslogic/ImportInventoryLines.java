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
 * Contributor(s):
 ************************************************************************
 */

package org.openbravo.client.application.businesslogic;

import java.io.BufferedReader;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.Tuple;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.attachment.AttachImplementationManager;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.PropertyNotFoundException;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.service.db.DbUtility;

public class ImportInventoryLines extends ProcessUploadedFile {
  private static final long serialVersionUID = 1L;
  private static final Logger log = LogManager.getLogger();

  @Inject
  @Any
  private Instance<InventoryCountUpdateHook> inventoryCountUpdateHooks;

  @Override
  protected void clearBeforeImport(String ownerId, JSONObject paramValues) {
    @SuppressWarnings("unchecked")
    NativeQuery<String> qry = OBDal.getInstance()
        .getSession()
        .createNativeQuery(
            "update m_inventoryline set updated=now(), updatedby=:userId, isactive='N' where m_inventory_id = :m_inventory_id");
    qry.setParameter("userId", OBContext.getOBContext().getUser().getId());
    qry.setParameter("m_inventory_id", ownerId);
    qry.executeUpdate();

  }

  @Override
  protected UploadResult doProcessFile(JSONObject paramValues, File file) throws Exception {
    final UploadResult uploadResult = new UploadResult();
    final String inventoryId = paramValues.getString("inpOwnerId");
    final String tabId = paramValues.getString("inpTabId");
    final String noStock = paramValues.getString("noStock");
    InventoryCount inventory = OBDal.getInstance().get(InventoryCount.class, inventoryId);
    String separator = getFieldSeparator();
    Set<String> alreadyProcessed = new HashSet<String>();
    try (BufferedReader br = Files.newBufferedReader(Paths.get(file.getAbsolutePath()))) {
      String line = br.readLine();
      int i = 0;
      while ((line = br.readLine()) != null) {

        String[] fields = line.split(separator, -1);
        String upc = fields[0];
        String productSearchkey = fields[1];
        String attributes = fields[2];
        String qtyCount = fields[4];
        String uomName = fields[5];
        String bin = fields[6];
        String description = fields[7];

        Product product = getProduct(upc, productSearchkey);
        if (product == null) {
          uploadResult.incErrorCount();
          uploadResult.addErrorMessage(upc + "/" + productSearchkey + " --> "
              + OBMessageUtils.getI18NMessage("OBUIAPP_ProductNotFound") + "\n");
          continue;
        }

        Locator locator = getLocator(bin, inventory.getWarehouse().getId());
        if (locator == null) {
          uploadResult.incErrorCount();
          uploadResult.addErrorMessage(
              bin + " --> " + OBMessageUtils.getI18NMessage("OBUIAPP_LocatorNotFound") + "\n");
          continue;
        }

        if (isNotValidProductForInventoryOrg(product, inventory)) {
          uploadResult.incErrorCount();
          uploadResult.addErrorMessage(upc + "/" + productSearchkey + " --> "
              + OBMessageUtils.getI18NMessage("OBUIAPP_NotValidProductForInventoryOrg") + "\n");
          continue;
        }

        UOM uom = getUOM(uomName);
        if (uom == null) {
          uploadResult.incErrorCount();
          uploadResult.addErrorMessage(
              uomName + " --> " + OBMessageUtils.getI18NMessage("OBUIAPP_UOMNotFound") + "\n");
          continue;
        }
        InventoryCountLine inventoryLine = null;
        // check if the line already exists
        List<InventoryCountLine> lines = checkIfInventoryLineExist(inventoryId, attributes, product,
            locator, uom);
        if (lines.size() == 0) {
          // create a new one
          inventoryLine = createInventoryLine(inventoryId, attributes, product, locator);
        } else {
          // get the line from the result
          inventoryLine = lines.get(0);
          BigDecimal updatedBookedQty = getUpdatedBookQty(inventoryLine);
          inventoryLine.setGapbookqty(inventoryLine.getBookQuantity().subtract(updatedBookedQty));
          inventoryLine.setBookQuantity(updatedBookedQty);
        }
        if (alreadyProcessed.contains(inventoryLine.getId())) {
          uploadResult.incErrorCount();
          String[] messageParams = { inventoryLine.getProduct().getName(),
              inventoryLine.getStorageBin().getSearchKey(),
              inventoryLine.getAttributeSetValue().getIdentifier(),
              inventoryLine.getUOM().getName() };
          String errorMsg = OBMessageUtils.getI18NMessage("OBUIAPP_DuplicateLineFound",
              messageParams);
          uploadResult.addErrorMessage(errorMsg);
          continue;
        }
        inventoryLine.setActive(true);
        inventoryLine
            .setQuantityCount(qtyCount.equals("") ? BigDecimal.ZERO : new BigDecimal(qtyCount));
        inventoryLine.setDescription(description);
        inventoryLine.setCsvimported(true);

        // Note: try-catch inside the loop required to allow for the process to continue on error
        try {
          if (paramValues.has("importErrorHandling")
              && paramValues.getString("importErrorHandling").equals("continue_at_error")) {
            // Note: OBDal.getInstance().commitAndClose() does not work
            // as it closes the session, doing the autocommit approach instead
            OBDal.getInstance().getConnection().setAutoCommit(true);
            OBDal.getInstance().save(inventoryLine);
            OBDal.getInstance().flush();
            OBDal.getInstance().getConnection().setAutoCommit(false);
          } else {
            OBDal.getInstance().save(inventoryLine);
          }
        } catch (Exception e) {
          uploadResult.incErrorCount();
          final Throwable ex = DbUtility
              .getUnderlyingSQLException(e.getCause() != null ? e.getCause() : e);
          final OBError errMsg = OBMessageUtils.translateError(ex.getMessage());
          uploadResult.addErrorMessage(errMsg.getMessage() + "\n");
        }

        alreadyProcessed.add(inventoryLine.getId());
        if ((i > 0) && (i % 100) == 0) {
          OBDal.getInstance().flush();
        }
        i++;
        uploadResult.incTotalCount();

        // evicting the unneeded objects at the end to help with performance.
        OBDal.getInstance().getSession().evict(product);
        OBDal.getInstance().getSession().evict(uom);
        OBDal.getInstance().getSession().evict(locator);
        OBDal.getInstance().getSession().evict(inventoryLine);
      }
      OBDal.getInstance().flush();

    } catch (Exception e) {
      uploadResult.incErrorCount();
      final Throwable ex = DbUtility
          .getUnderlyingSQLException(e.getCause() != null ? e.getCause() : e);
      final OBError errMsg = OBMessageUtils.translateError(ex.getMessage());
      uploadResult.addErrorMessage(errMsg.getMessage() + "\n");
    }

    try (ScrollableResults physicalInventorylines = getPhysicalInventoryLinesNotImported(
        inventoryId)) {
      inCaseNoStock(noStock, physicalInventorylines);
    } catch (Exception e) {
      uploadResult.incErrorCount();
      uploadResult.addErrorMessage(e.getMessage() + "\n");
    }

    // Attach csv in inventory attachments
    attachCSV(file, inventory, tabId);
    executeHooks(inventoryCountUpdateHooks, inventory);

    return uploadResult;
  }

  private boolean isNotValidProductForInventoryOrg(Product product, InventoryCount inventory) {
    return !OBContext.getOBContext()
        .getOrganizationStructureProvider()
        .isInNaturalTree(product.getOrganization(), inventory.getOrganization());
  }

  private void attachCSV(File file, InventoryCount inventory, String tabId) {
    AttachImplementationManager aim = WeldUtils
        .getInstanceFromStaticBeanManager(AttachImplementationManager.class);
    aim.upload(Collections.emptyMap(), tabId, inventory.getId(),
        inventory.getOrganization().getId(), file);
  }

  private List<InventoryCountLine> checkIfInventoryLineExist(final String inventoryId,
      String attributes, Product product, Locator locator, UOM uom) {
    AttributeSetInstance asi = getAttributeSetInstance(attributes);

    OBQuery<InventoryCountLine> qry = OBDal.getInstance()
        .createQuery(InventoryCountLine.class,
            "m_inventory_id=:m_inventory_id and m_product_id=:m_product_id and m_locator_id=:m_locator_id "
                + (asi == null || asi.getId().equals("0")
                    ? "and (m_attributesetinstance_id is null or m_attributesetinstance_id='0') "
                    : "and m_attributesetinstance_id=:m_attributesetinstance_id ")
                + "and c_uom_id=:c_uom_id");
    qry.setNamedParameter("m_inventory_id", inventoryId);
    qry.setNamedParameter("m_product_id", product.getId());
    qry.setNamedParameter("m_locator_id", locator.getId());
    if (asi != null && !asi.getId().equals("0")) {
      qry.setNamedParameter("m_attributesetinstance_id", asi.getId());
    }
    qry.setNamedParameter("c_uom_id", uom.getId());
    qry.setFilterOnActive(false);
    List<InventoryCountLine> lines = qry.list();

    return lines;
  }

  private InventoryCountLine createInventoryLine(final String inventoryId, String attributes,
      Product product, Locator locator) {
    InventoryCountLine inventoryLine;
    InventoryCount inventoryCount = OBDal.getInstance().get(InventoryCount.class, inventoryId);

    inventoryLine = OBProvider.getInstance().get(InventoryCountLine.class);
    inventoryLine.setClient(inventoryCount.getClient());
    inventoryLine.setOrganization(inventoryCount.getOrganization());
    inventoryLine.setPhysInventory(inventoryCount);
    inventoryLine.setLineNo(getMaxLineNo(inventoryId) + 10);
    inventoryLine.setProduct(product);
    inventoryLine.setAttributeSetValue(
        getAttributeSetInstance(attributes) != null ? getAttributeSetInstance(attributes)
            : OBDal.getInstance().get(AttributeSetInstance.class, "0"));
    inventoryLine.setBookQuantity(BigDecimal.ZERO);
    inventoryLine.setGapbookqty(BigDecimal.ZERO);
    inventoryLine.setUOM(product.getUOM());
    inventoryLine.setStorageBin(locator);
    return inventoryLine;
  }

  private BigDecimal getUpdatedBookQty(InventoryCountLine inventoryLine) {
    AttributeSetInstance asi = inventoryLine.getAttributeSetValue();

    // @formatter:off 
    String hql = "select sd.quantityOnHand from MaterialMgmtStorageDetail as sd "
        + "where sd.product.id = :productId "
        + "and sd.storageBin.id = :locatorId "
        + (asi == null || asi.getId().equals("0") ?
            "and (sd.attributeSetValue is null or sd.attributeSetValue.id = '0') "
            : "and sd.attributeSetValue.id = :attributeSetValueId "
            )
        + "and sd.uOM.id = :uomId "
        + "and coalesce(sd.orderUOM.id, '-1') = :productUOMId ";
    // @formatter:on
    Query<BigDecimal> query = OBDal.getInstance().getSession().createQuery(hql, BigDecimal.class);
    query.setParameter("productId", inventoryLine.getProduct().getId());
    query.setParameter("locatorId", inventoryLine.getStorageBin().getId());
    if (asi != null && !asi.getId().equals("0")) {
      query.setParameter("attributeSetValueId", inventoryLine.getAttributeSetValue().getId());
    }
    query.setParameter("uomId", inventoryLine.getUOM().getId());
    query.setParameter("productUOMId",
        inventoryLine.getOrderUOM() != null ? inventoryLine.getOrderUOM() : "-1");

    BigDecimal bookqty = query.uniqueResult();
    if (bookqty != null) {
      return bookqty;
    }
    return BigDecimal.ZERO;
  }

  private void inCaseNoStock(String noStock, ScrollableResults physicalInventorylines) {
    while (physicalInventorylines.next()) {
      Tuple physicalInventoryline = (Tuple) physicalInventorylines.get()[0];
      InventoryCountLine inventoryCountLine = OBDal.getInstance()
          .get(InventoryCountLine.class, physicalInventoryline.get("inventoryLineId"));

      if (noStock.equals("deleteLines")) {
        OBDal.getInstance().remove(inventoryCountLine);
      } else if (noStock.equals("quantityCountZero")) {
        inventoryCountLine.setQuantityCount(BigDecimal.ZERO);
      } else if (noStock.equals("quantityCountOriginal")) {
        inventoryCountLine.setQuantityCount(inventoryCountLine.getBookQuantity());
      } else if (noStock.equals("doNotModify")) {
        // Do nothing
      }
    }
  }

  private Long getMaxLineNo(String inventoryId) {
    // @formatter:off 
    String hql = "select coalesce(max(il.lineNo), 0) from MaterialMgmtInventoryCountLine as il"
        + " where il.physInventory.id = :inventoryId";
    // @formatter:on
    Query<Long> query = OBDal.getInstance().getSession().createQuery(hql, Long.class);
    query.setParameter("inventoryId", inventoryId);
    Long maxLineNo = query.uniqueResult();
    if (maxLineNo != null) {
      return maxLineNo;
    }
    return 0L;
  }

  private Product getProduct(String upc, String productSearchkey) {
    if (upc.isEmpty() && productSearchkey.isEmpty()) {
      return null;
    }

    String whereClause;
    if (upc.isEmpty()) {
      whereClause = "value=:value";
    } else if (productSearchkey.isEmpty()) {
      whereClause = "upc=:upc";
    } else {
      whereClause = "value=:value or upc=:upc";
    }

    final OBQuery<Product> qry = OBDal.getInstance().createQuery(Product.class, whereClause);
    if (!productSearchkey.isEmpty()) {
      qry.setNamedParameter("value", productSearchkey);
    }
    if (!upc.isEmpty()) {
      qry.setNamedParameter("upc", upc);
    }
    qry.setMaxResult(1);
    return qry.uniqueResult();
  }

  private Locator getLocator(String bin, String warehouseId) {
    final OBQuery<Locator> qry = OBDal.getInstance()
        .createQuery(Locator.class, "value=:value and m_warehouse_id= :warehouseId");
    qry.setNamedParameter("value", bin);
    qry.setNamedParameter("warehouseId", warehouseId);
    qry.setMaxResult(1);
    return qry.uniqueResult();
  }

  private UOM getUOM(String uom) {
    final OBQuery<UOM> qry = OBDal.getInstance().createQuery(UOM.class, "name=:name");
    qry.setNamedParameter("name", uom);
    qry.setMaxResult(1);
    return qry.uniqueResult();
  }

  private AttributeSetInstance getAttributeSetInstance(String attributeSetInstance) {
    final OBQuery<AttributeSetInstance> qry = OBDal.getInstance()
        .createQuery(AttributeSetInstance.class, "description=:description");
    qry.setNamedParameter("description", attributeSetInstance);
    qry.setMaxResult(1);
    return qry.uniqueResult();
  }

  private String getFieldSeparator() {
    String fieldSeparator = "";
    try {
      fieldSeparator = Preferences.getPreferenceValue("OBSERDS_CSVFieldSeparator", true,
          OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
          OBContext.getOBContext().getRole(), null);
    } catch (PropertyNotFoundException e) {
      // There is no preference for the field separator. Using the default one.
      fieldSeparator = ",";
    } catch (PropertyException e1) {
      log.error("Error getting Preference: " + e1.getMessage(), e1);
    }
    return fieldSeparator;
  }

  private static ScrollableResults getPhysicalInventoryLinesNotImported(String inventoryId) {
    //@formatter:off
    String hql =  " select pil.id as inventoryLineId " +
                  " from MaterialMgmtInventoryCountLine as pil" +
                  " where pil.physInventory.id = :inventoryId " +
                  " and iscsvimported = 'N'" +
                  " order by pil.lineNo, pil.id ";
    //@formatter:on  

    final Query<Tuple> query = OBDal.getInstance().getSession().createQuery(hql, Tuple.class);
    query.setParameter("inventoryId", inventoryId);
    return query.scroll(ScrollMode.FORWARD_ONLY);
  }

  private void executeHooks(final Instance<? extends Object> hooks, final InventoryCount inventory)
      throws Exception {
    if (hooks != null) {
      for (final Iterator<? extends Object> procIter = hooks.iterator(); procIter.hasNext();) {
        final Object proc = procIter.next();
        if (proc instanceof InventoryCountUpdateHook) {
          ((InventoryCountUpdateHook) proc).exec(inventory);
        }
      }
    }
  }

}
