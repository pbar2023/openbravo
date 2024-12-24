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
package org.openbravo.client.application.event;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Characteristic;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductByPriceAndWarehouse;
import org.openbravo.model.common.plm.ProductCharacteristicValue;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.service.db.CallStoredProcedure;

/**
 * Class that handles generating, updating and removing order lines in Purchase Model Mode process
 */
public class UpdateMultiVariantPurchaseOrderLines extends BaseActionHandler {

  public static final String NO_COLUMN_CHARACTERISTIC = "NO_COLUMN_CHARACTERISTIC";
  public static final String NO_ROW_CHARACTERISTIC = "NO_ROW_CHARACTERISTIC";
  int lastLineNo = 0;

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject result = new JSONObject();

    try {
      JSONObject jsonContent = new JSONObject(content);
      String orderId = jsonContent.getJSONArray("recordIdList").getString(0);
      JSONArray productRowIds = jsonContent.getJSONArray("productRowIds");
      JSONObject productRowsData = jsonContent.getJSONObject("productRowsData");

      Order order = OBDal.getInstance().get(Order.class, orderId);
      List<OrderLine> orderLines = order.getOrderLineList();

      List<String> productIdsFromProcess = new ArrayList<>();
      for (int i = 0; i < productRowIds.length(); i++) {
        productIdsFromProcess.add(productRowIds.getString(i));
      }

      List<String> removedProductIds = new ArrayList<>();
      for (OrderLine orderLine : orderLines) {
        Product genericProduct = orderLine.getProduct().getGenericProduct();
        if (genericProduct != null) {
          if (!productIdsFromProcess.contains(genericProduct.getId())) {
            removedProductIds.add(genericProduct.getId());
          }
        }
      }

      removeAllOrderLinesRelatedToGenericProduct(order, removedProductIds);

      updateOrderLinesForGenericProduct(order, productRowsData, productIdsFromProcess);

    } catch (JSONException | OBException e) {
      OBDal.getInstance().rollbackAndClose();
      return getErrorMessage(e);
    }

    return result;
  }

  /**
   * Finds each order line that contains a given generic product id, and removes it
   * 
   * @param genericProductIds
   *          List of generic product ids, related orderlines will be removed
   */
  private void removeAllOrderLinesRelatedToGenericProduct(Order order,
      List<String> genericProductIds) {
    if (genericProductIds.isEmpty()) {
      return;
    }
    List<OrderLine> orderLines = order.getOrderLineList();
    List<OrderLine> orderLinesToRemove = new ArrayList<>();
    for (OrderLine orderLine : orderLines) {
      Product orderLineGenericProduct = orderLine.getProduct().getGenericProduct();
      if (orderLineGenericProduct != null
          && genericProductIds.contains(orderLineGenericProduct.getId())) {
        // Remove product from both list and DAL
        orderLinesToRemove.add(orderLine);
      }
    }

    orderLinesToRemove.forEach(orderLine -> removeOrderLine(order, orderLine));
  }

  private void removeOrderLine(Order order, OrderLine orderLine) {
    List<OrderLine> orderLines = order.getOrderLineList();
    orderLines.remove(orderLine);
    OBDal.getInstance().remove(orderLine);
  }

  /**
   * Generates, removes or updates order lines based on productRowsData
   * 
   * @param order
   *          Order to be updated
   * @param productRowsData
   *          Product row data with values for each combination of relevant characteristics
   * @param genericProductsToUpdate
   *          Generic products to be updated
   */
  private void updateOrderLinesForGenericProduct(Order order, JSONObject productRowsData,
      List<String> genericProductsToUpdate) throws JSONException {
    for (String genericProductId : genericProductsToUpdate) {
      if (!productRowsData.has(genericProductId)) {
        // Skip generic products that have not been modified
        continue;
      }

      Map<String, Map<String, Integer>> mappedProductRowData = getProductRowDataMapped(
          productRowsData.getJSONArray(genericProductId));
      Product genericProductToUpdate = OBDal.getInstance().get(Product.class, genericProductId);
      Characteristic genericProductToUpdateRowCharacteristic = genericProductToUpdate
          .getRowCharacteristic();
      Characteristic genericProductToUpdateColumnCharacteristic = genericProductToUpdate
          .getColumnCharacteristic();
      List<Product> variantProducts = getAllVariantsOfGenericProduct(genericProductId);
      for (Product variantProduct : variantProducts) {
        // Check product characteristic value against mappedProductRowData row/column
        // characteristic values
        String productRowCharacteristicValue = "";
        String productColumnCharacteristicValue = "";

        for (ProductCharacteristicValue productCharacteristicValue : variantProduct
            .getProductCharacteristicValueList()) {
          if (genericProductToUpdateRowCharacteristic != null
              && productCharacteristicValue.getCharacteristic()
                  .getId()
                  .equals(genericProductToUpdateRowCharacteristic.getId())) {
            productRowCharacteristicValue = productCharacteristicValue.getCharacteristicValue()
                .getId();
          } else if (genericProductToUpdateColumnCharacteristic != null
              && productCharacteristicValue.getCharacteristic()
                  .getId()
                  .equals(genericProductToUpdateColumnCharacteristic.getId())) {
            productColumnCharacteristicValue = productCharacteristicValue.getCharacteristicValue()
                .getId();
          }
        }

        int quantity = getQuantityFromProductRowData(genericProductToUpdate, mappedProductRowData,
            productRowCharacteristicValue, productColumnCharacteristicValue);
        // Find if OrderLine already exists for this variant product
        List<OrderLine> existingOrderLines = order.getOrderLineList()
            .stream()
            .filter(orderLine -> orderLine.getProduct().getId().equals(variantProduct.getId()))
            .collect(Collectors.toList());
        if (existingOrderLines.isEmpty()) {
          // OrderLine for this variant does not exist, one should be created if quantity > 0
          if (quantity > 0) {
            createOrderLine(order, variantProduct, quantity);
          }
        } else {
          // Assumed that there is only one existing orderLine for a given variant product
          OrderLine orderLineOfVariantProduct = existingOrderLines.get(0);
          if (quantity == 0) {
            // OrderLine must be removed
            removeOrderLine(order, orderLineOfVariantProduct);
          } else if (quantity != orderLineOfVariantProduct.getOrderedQuantity().intValue()) {
            // OrderLine should be update with the new quantity if it is different
            updateOrderLineQuantity(order, orderLineOfVariantProduct, quantity);
          }
        }
      }
    }
  }

  private static void updateOrderLineQuantity(Order order, OrderLine orderLineOfVariantProduct,
      int quantity) {
    orderLineOfVariantProduct.setOrderedQuantity(BigDecimal.valueOf(quantity));
    orderLineOfVariantProduct.setOperativeQuantity(BigDecimal.valueOf(quantity));
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(order);
  }

  private Map<String, Map<String, Integer>> getProductRowDataMapped(JSONArray productRowData)
      throws JSONException {
    Map<String, Map<String, Integer>> mappedProductRowData = new HashMap<>();
    for (int i = 0; i < productRowData.length(); i++) {
      JSONObject productRow = productRowData.getJSONObject(i);
      String rowCharacteristic = productRow.getString("rowCharacteristicValue");
      String columnCharacteristic = productRow.getString("columnCharacteristicValue");
      if (!NO_ROW_CHARACTERISTIC.equals(rowCharacteristic)) {
        Map<String, Integer> row = mappedProductRowData.getOrDefault(rowCharacteristic,
            new HashMap<>());
        if (NO_COLUMN_CHARACTERISTIC.equals(columnCharacteristic)) {
          row.put(NO_COLUMN_CHARACTERISTIC, productRow.getInt("quantity"));
        } else {
          row.put(columnCharacteristic, productRow.getInt("quantity"));
        }
        mappedProductRowData.put(rowCharacteristic, row);
      } else if (!NO_COLUMN_CHARACTERISTIC.equals(columnCharacteristic)) {
        Map<String, Integer> column = mappedProductRowData.getOrDefault(columnCharacteristic,
            new HashMap<>());
        column.put(NO_ROW_CHARACTERISTIC, productRow.getInt("quantity"));
        mappedProductRowData.put(columnCharacteristic, column);
      }
    }

    return mappedProductRowData;
  }

  private void createOrderLine(Order order, Product variantProduct, Integer quantity) {
    List<OrderLine> orderLines = order.getOrderLineList();
    OrderLine newOrderLine = OBProvider.getInstance().get(OrderLine.class);
    newOrderLine.setLineNo((long) getNextLineNo(order));
    newOrderLine.setProduct(variantProduct);
    newOrderLine.setOrderedQuantity(BigDecimal.valueOf(quantity));
    newOrderLine.setOrganization(order.getOrganization());
    newOrderLine.setSalesOrder(order);
    newOrderLine.setUOM(variantProduct.getUOM());
    newOrderLine.setOperativeQuantity(BigDecimal.valueOf(quantity));
    newOrderLine.setOperativeUOM(variantProduct.getUOM());
    newOrderLine.setOrderDate(new Date());
    newOrderLine.setWarehouse(order.getWarehouse());
    newOrderLine.setCurrency(order.getCurrency());

    addTaxToOrderLine(order, newOrderLine);
    addPriceToOrderLine(order, BigDecimal.valueOf(quantity), newOrderLine);

    newOrderLine.setNewOBObject(true);
    orderLines.add(newOrderLine);
    OBDal.getInstance().save(newOrderLine);
  }

  private void addPriceToOrderLine(Order order, BigDecimal qtyOrdered, OrderLine orderLine) {
    // Price
    BigDecimal unitPrice, netPrice, grossPrice, stdPrice, limitPrice, grossAmt, netListPrice,
        grossListPrice, grossStdPrice;
    stdPrice = BigDecimal.ZERO;
    final int stdPrecision = order.getCurrency().getStandardPrecision().intValue();

    OBCriteria<ProductByPriceAndWarehouse> criteria = OBDal.getInstance()
        .createCriteria(ProductByPriceAndWarehouse.class);
    criteria.add(Restrictions.eq(ProductByPriceAndWarehouse.PROPERTY_PRODUCT + ".id",
        orderLine.getProduct().getId()));
    criteria.add(Restrictions.eq(ProductByPriceAndWarehouse.PROPERTY_WAREHOUSE + ".id",
        orderLine.getWarehouse().getId()));
    criteria.createAlias(ProductByPriceAndWarehouse.PROPERTY_PRODUCTPRICE, "p");
    criteria.add(Restrictions.in("p." + ProductPrice.PROPERTY_PRICELISTVERSION,
        order.getPriceList().getPricingPriceListVersionList()));
    criteria.add(Restrictions.eq(ProductByPriceAndWarehouse.PROPERTY_ACTIVE, true));
    // criteria.setMaxResults(1);
    List<ProductByPriceAndWarehouse> productByPriceAndWarehouse = criteria.list();
    if (productByPriceAndWarehouse.isEmpty()) {
      throw new OBException(OBMessageUtils.getI18NMessage("VariantNoProductPrice",
          new String[] { orderLine.getProduct().getSearchKey(), order.getPriceList().getName() }));
    }

    unitPrice = productByPriceAndWarehouse.get(0).getStandardPrice();
    limitPrice = netListPrice = grossListPrice = stdPrice = unitPrice;

    if (order.getPriceList().isPriceIncludesTax()) {
      grossPrice = unitPrice;
      grossAmt = grossPrice.multiply(qtyOrdered).setScale(stdPrecision, RoundingMode.HALF_UP);
      netPrice = limitPrice = netListPrice = BigDecimal.ZERO;
      // selected line standard price is Gross Std Price in this case
      grossStdPrice = unitPrice;
    } else {
      netPrice = unitPrice;
      grossListPrice = grossAmt = grossPrice = grossStdPrice = BigDecimal.ZERO;
    }

    orderLine.setUnitPrice(netPrice);
    orderLine.setGrossUnitPrice(grossPrice);
    orderLine.setListPrice(netListPrice);
    orderLine.setGrossListPrice(grossListPrice);
    orderLine.setPriceLimit(limitPrice);
    orderLine.setStandardPrice(stdPrice);
    // Set Base Gross Unit Price that is Gross Standard Price
    orderLine.setBaseGrossUnitPrice(grossStdPrice);
    orderLine.setLineNetAmount(
        netPrice.multiply(qtyOrdered).setScale(stdPrecision, RoundingMode.HALF_UP));
    orderLine.setLineGrossAmount(grossAmt);
  }

  private void addTaxToOrderLine(Order order, OrderLine orderLine) {
    List<Object> parameters = new ArrayList<Object>();
    parameters.add(orderLine.getProduct().getId());
    parameters.add(order.getOrderDate());
    parameters.add(order.getOrganization().getId());
    parameters.add(order.getWarehouse().getId());
    parameters.add(order.getInvoiceAddress().getId());
    parameters.add(order.getPartnerAddress().getId());

    if (order.getProject() != null) {
      parameters.add(order.getProject().getId());
    } else {
      parameters.add(null);
    }
    parameters.add("N");

    String taxId = (String) CallStoredProcedure.getInstance().call("C_Gettax", parameters, null);
    if (taxId == null || taxId.isEmpty()) {
      Map<String, String> errorParameters = new HashMap<String, String>();
      errorParameters.put("product", orderLine.getProduct().getName());
      String message = OBMessageUtils.messageBD("NoTaxFoundForProduct");
      throw new OBException(OBMessageUtils.parseTranslation(message, errorParameters));
    }
    TaxRate tax = OBDal.getInstance().get(TaxRate.class, taxId);
    orderLine.setTax(tax);
  }

  private JSONObject getErrorMessage(Exception e) {
    JSONObject errorMessage = new JSONObject( //
        Map.of("severity", "error", //
            "title", "Error", //
            "text", e.getMessage())); //
    return new JSONObject(Map.of("message", errorMessage));
  }

  private static int getQuantityFromProductRowData(Product genericProduct,
      Map<String, Map<String, Integer>> mappedProductRowData, String productRowCharacteristicValue,
      String productColumnCharacteristicValue) {
    boolean hasRowCharacteristic = genericProduct.getRowCharacteristic() != null;
    boolean hasColumnCharacteristic = genericProduct.getColumnCharacteristic() != null;
    int quantity = 0;
    if (hasRowCharacteristic) {
      if (hasColumnCharacteristic) {
        quantity = getQuantity(mappedProductRowData, productRowCharacteristicValue,
            productColumnCharacteristicValue);
      } else {
        quantity = getQuantity(mappedProductRowData, productRowCharacteristicValue,
            NO_COLUMN_CHARACTERISTIC);
      }
    } else if (hasColumnCharacteristic) {
      quantity = getQuantity(mappedProductRowData, productColumnCharacteristicValue,
          NO_ROW_CHARACTERISTIC);
    }
    return quantity;
  }

  private static Integer getQuantity(Map<String, Map<String, Integer>> mappedProductRowData,
      String productRowCharacteristicValue, String productColumnCharacteristicValue) {
    if (mappedProductRowData.get(productRowCharacteristicValue) == null
        || mappedProductRowData.get(productRowCharacteristicValue)
            .get(productColumnCharacteristicValue) == null) {
      return 0;
    }
    return mappedProductRowData.get(productRowCharacteristicValue)
        .get(productColumnCharacteristicValue);
  }

  private static List<Product> getAllVariantsOfGenericProduct(String newGenericProductId) {
    OBCriteria<Product> obc = OBDal.getInstance().createCriteria(Product.class);
    obc.setFilterOnActive(true);
    obc.setFilterOnReadableClients(true);
    obc.setFilterOnReadableOrganization(true);

    obc.add(Restrictions.eq(Product.PROPERTY_GENERICPRODUCT + ".id", newGenericProductId));
    obc.add(Restrictions.eq(Product.PROPERTY_ISGENERIC, false));
    return obc.list();
  }

  private int getNextLineNo(Order order) {
    if (lastLineNo == 0) {
      order.getOrderLineList().forEach(orderLine -> {
        if (orderLine.getLineNo().intValue() > lastLineNo) {
          lastLineNo = orderLine.getLineNo().intValue();
        }
      });
    }
    lastLineNo += 10;
    return lastLineNo;
  }
}
