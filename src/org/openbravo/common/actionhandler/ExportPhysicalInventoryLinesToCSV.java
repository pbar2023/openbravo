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

package org.openbravo.common.actionhandler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Tuple;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.process.FileExportActionHandler;
import org.openbravo.client.application.report.ReportingUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.PropertyNotFoundException;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.service.db.CallProcess;

/**
 * Action handler to Export Physical Inventory Lines to a CSV file.
 * 
 * Also this process gives the possibility of generating the inventory lines automatically, with the
 * default values parameters when the inventory type is "total".
 */

public class ExportPhysicalInventoryLinesToCSV extends FileExportActionHandler {
  private static final Logger log = LogManager.getLogger();
  private static final String FILE_PREFIX = "STOCK_LEVEL_";
  private static final String FILE_EXTENSION = ".csv";

  @Override
  protected Path generateFileToDownload(Map<String, Object> parameters, JSONObject data)
      throws IOException, JSONException {
    JSONObject params;
    try {
      params = data.getJSONObject("_params");
      boolean generateLines = params.optBoolean("CreateInventoryLinesAutomatically");

      boolean blindCount = params.optBoolean("BlindCount");

      String inventoryId = data.optString("M_Inventory_ID");

      boolean exportLinesWithZeroQty = params.optBoolean("exportLinesWithZeroQty");

      boolean alreadyImported = checkIfThereAreLinesImported(inventoryId);

      generateInventoryLines(generateLines && !alreadyImported, inventoryId);
      if (hasLines(inventoryId)) {
        return createCSVFile(inventoryId, blindCount, exportLinesWithZeroQty);
      } else {
        throw new OBException(OBMessageUtils.messageBD("NoPhysicalInventoryLines"));
      }
    } catch (Exception e) {
      log.error("Error generating tmp file: " + e.getMessage(), e);
    }
    return null;
  }

  private boolean checkIfThereAreLinesImported(String inventoryId) {
    InventoryCount inventory = OBDal.getInstance().get(InventoryCount.class, inventoryId);
    List<InventoryCountLine> lines = inventory.getMaterialMgmtInventoryCountLineList();
    return lines.stream().anyMatch(line -> line.isCsvimported());
  }

  @Override
  protected String getDownloadFileName(Map<String, Object> parameters, JSONObject data) {
    String inventoryId = data.optString("M_Inventory_ID");
    InventoryCount inventory = OBDal.getInstance().getProxy(InventoryCount.class, inventoryId);
    return FILE_PREFIX + inventory.getOrganization().getSearchKey() + "_" + new Date()
        + FILE_EXTENSION;
  }

  private void generateInventoryLines(boolean generateLines, String inventoryId) {
    InventoryCount inventory = OBDal.getInstance().get(InventoryCount.class, inventoryId);
    if (generateLines && inventory.getInventoryType().equals("T")) {
      // Delete lines, in order to replace and create again
      deleteInventoryLines(inventory);
      generateInventoryLinesProcess(inventoryId);
    }
    OBDal.getInstance().refresh(inventory);
  }

  private boolean hasLines(String inventoryId) {
    OBCriteria<InventoryCountLine> criteria = OBDal.getInstance()
        .createCriteria(InventoryCountLine.class);
    criteria.add(Restrictions.eq("physInventory.id", inventoryId));
    criteria.setMaxResults(1);
    InventoryCountLine inventoryCountLine = (InventoryCountLine) criteria.uniqueResult();
    return inventoryCountLine != null;
  }

  private void deleteInventoryLines(InventoryCount inventory) {

    // clear will also remove the objects from the database
    inventory.getMaterialMgmtInventoryCountLineList().clear();
    OBDal.getInstance().save(inventory);
    OBDal.getInstance().flush();
  }

  private void generateInventoryLinesProcess(String inventoryId) {
    final String PROCESS_M_INVENTORY_LISTCREATE = "105";
    final org.openbravo.model.ad.ui.Process process = OBDal.getInstance()
        .get(org.openbravo.model.ad.ui.Process.class, PROCESS_M_INVENTORY_LISTCREATE);
    Map<String, String> parameters = inventoryLinesProcessParameters();

    final ProcessInstance pinstance = CallProcess.getInstance()
        .call(process, inventoryId, parameters);
    final OBError result = OBMessageUtils.getProcessInstanceMessage(pinstance);
    if (StringUtils.equals("Error", result.getType())) {
      throw new OBException(result.getMessage());
    }
  }

  /**
   * Generates the map of parameters to launch the Create Inventory Count List process
   */
  protected Map<String, String> inventoryLinesProcessParameters() {
    // Default values parameters, generate inventory lines for all products regardless of the
    // quantity count
    Map<String, String> parameters = new HashMap<>();
    parameters.put("M_Locator_ID", null);
    parameters.put("ProductValue", "%");
    parameters.put("M_Product_Category_ID", null);
    parameters.put("QtyRange", null);
    parameters.put("regularization", "N");
    parameters.put("ABC", null);
    return parameters;
  }

  private Path createCSVFile(String inventoryId, boolean blindCount, boolean exportLinesWithZeroQty)
      throws IOException {
    String tmpFileName = UUID.randomUUID().toString() + ".csv";
    File file = new File(ReportingUtils.getTempFolder(), tmpFileName);
    try (FileWriter outputfile = new FileWriter(file)) {
      // Comma as a separator and No Quote Character
      final Writer writer = new StringWriter();
      addPhysicalInventoryLinesToCsv(writer, inventoryId, blindCount, exportLinesWithZeroQty);
      writer.close();
      outputfile.write(writer.toString());
      InventoryCount inventoryCount = OBDal.getInstance().get(InventoryCount.class, inventoryId);
      inventoryCount.setExportedCsvDate(new Date());
    }
    return file.toPath();
  }

  private static ScrollableResults getPhysicalInventoryLines(String inventoryId,
      boolean exportLinesWithZeroQty) {
    //@formatter:off
    String hql =  "select coalesce(pil.product.uPCEAN,'') as productUPC , " +
                  "  pil.product.searchKey as productSearchKey," +
                  "  coalesce(att.description,'') as attributeSet, "+
                  "  pil.bookQuantity as bookQuantity," +
                  "  pil.quantityCount as quantityCount," +
                  "  pil.uOM.name as UOM, pil.storageBin.searchKey as storageBin," +
                  "  coalesce(pil.description,'') as description " +
                  " from MaterialMgmtInventoryCountLine as pil" +
                  " left join pil.attributeSetValue as att" +
                  " where pil.physInventory.id = :inventoryId " +
(!exportLinesWithZeroQty ? " and pil.bookQuantity <> 0 " : "") +
                  " order by pil.lineNo, pil.id ";
    //@formatter:on  

    final Query<Tuple> query = OBDal.getInstance().getSession().createQuery(hql, Tuple.class);
    query.setParameter("inventoryId", inventoryId);
    return query.scroll(ScrollMode.FORWARD_ONLY);
  }

  private void addPhysicalInventoryLinesToCsv(final Writer writer, String inventoryId,
      boolean blindCount, boolean exportLinesWithZeroQty) throws IOException {
    String fieldSeparator = getFieldSeparator();

    createCSVHeader(writer, fieldSeparator);

    try (ScrollableResults physicalInventorylines = getPhysicalInventoryLines(inventoryId,
        exportLinesWithZeroQty)) {
      addCSVLines(writer, blindCount, fieldSeparator, physicalInventorylines);
    }
  }

  protected void addCSVLines(final Writer writer, boolean blindCount, String fieldSeparator,
      ScrollableResults physicalInventorylines) throws IOException {
    while (physicalInventorylines.next()) {
      Tuple physicalInventoryline = (Tuple) physicalInventorylines.get()[0];
      BigDecimal bookedQty = (BigDecimal) physicalInventoryline.get("bookQuantity");
      BigDecimal qtyCount = (BigDecimal) physicalInventoryline.get("quantityCount");
      writer.append((String) physicalInventoryline.get("productUPC") + fieldSeparator);
      writer.append((String) physicalInventoryline.get("productSearchKey") + fieldSeparator);
      writer.append(((String) physicalInventoryline.get("attributeSet") != null
          ? (String) physicalInventoryline.get("attributeSet")
          : "") + fieldSeparator);
      writer.append((blindCount ? ""
          : bookedQty.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0
              ? bookedQty.toBigInteger()
              : bookedQty)
          + fieldSeparator);
      writer.append((blindCount ? ""
          : qtyCount.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0
              ? qtyCount.toBigInteger()
              : qtyCount)
          + fieldSeparator);
      writer.append((String) physicalInventoryline.get("UOM") + fieldSeparator);
      writer.append((String) physicalInventoryline.get("storageBin") + fieldSeparator);
      writer.append((String) physicalInventoryline.get("description") + fieldSeparator);
      writer.append("\n");
    }
  }

  protected void createCSVHeader(final Writer writer, String fieldSeparator) throws IOException {
    writer.append(OBMessageUtils.messageBD("ProductUPCEAN") + fieldSeparator);
    writer.append(OBMessageUtils.messageBD("ProductSearchKey") + fieldSeparator);
    writer.append(OBMessageUtils.messageBD("Attributes") + fieldSeparator);
    writer.append(OBMessageUtils.messageBD("BookQty") + fieldSeparator);
    writer.append(OBMessageUtils.messageBD("QuantityCount") + fieldSeparator);
    writer.append(OBMessageUtils.messageBD("UOM") + fieldSeparator);
    writer.append(OBMessageUtils.messageBD("StorageBinSearchKey") + fieldSeparator);
    writer.append(OBMessageUtils.messageBD("Description") + fieldSeparator);
    writer.append("\n");
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

}
