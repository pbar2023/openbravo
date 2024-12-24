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
package org.openbravo.client.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.TreeNode;
import org.openbravo.model.common.plm.Characteristic;
import org.openbravo.model.common.plm.CharacteristicValue;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCharacteristic;
import org.openbravo.service.datasource.ReadOnlyDataSourceService;

/**
 * 
 * Datasource used by the Purchase Order view "Model Mode" process to retrieve the information about
 * the row/column characteristics of a given product
 */
public class ProductCharacteristicValuesRowColumnDataSource extends ReadOnlyDataSourceService {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected int getCount(Map<String, String> parameters) {
    return getData(parameters, 0, Integer.MAX_VALUE).size();
  }

  @Override
  protected List<Map<String, Object>> getData(Map<String, String> parameters, int startRow,
      int endRow) {
    List<Map<String, Object>> resultList = new ArrayList<>();

    String productId = parameters.get("productId");
    if (productId == null) {
      log.error("Missing productId for the datasource to fetch data");
      return resultList;
    }

    Product product = OBDal.getInstance().get(Product.class, productId);
    Characteristic prodRowCharacteristic = product.getRowCharacteristic();
    Characteristic prodColumnCharacteristic = product.getColumnCharacteristic();

    if (prodRowCharacteristic == null && prodColumnCharacteristic == null) {
      log.error("No product row or column dimension characteristics have been configured.");
      return resultList;
    }

    List<ProductCharacteristic> productCharacteristics = product.getProductCharacteristicList();
    JSONArray rowCharacteristics = new JSONArray();
    JSONArray columnCharacteristics = new JSONArray();

    ProductCharacteristic rowProductCharacteristic = productCharacteristics.stream()
        .filter(pc -> pc.getCharacteristic().getId() == prodRowCharacteristic.getId())
        .findAny()
        .get();

    rowProductCharacteristic.getProductCharacteristicConfList().forEach(chConf -> {
      rowCharacteristics.put(new JSONObject(Map.of("id", chConf.getCharacteristicValue().getId(),
          "_identifier", chConf.getCharacteristicValue().getIdentifier(), "seqNo",
          getProductCharacteristicSeqno(chConf.getCharacteristicValue()))));
    });

    if (prodColumnCharacteristic != null) {
      ProductCharacteristic colProductCharacteristic = productCharacteristics.stream()
          .filter(pc -> pc.getCharacteristic().getId() == prodColumnCharacteristic.getId())
          .findAny()
          .get();
      colProductCharacteristic.getProductCharacteristicConfList().forEach(chConf -> {
        columnCharacteristics
            .put(new JSONObject(Map.of("id", chConf.getCharacteristicValue().getId(), "_identifier",
                chConf.getCharacteristicValue().getIdentifier(), "seqNo",
                getProductCharacteristicSeqno(chConf.getCharacteristicValue()))));
      });
    }

    resultList.add(Map.of( //
        "rowCharacteristics", rowCharacteristics, //
        "columnCharacteristics", columnCharacteristics //
    ));

    return resultList;

  }

  private long getProductCharacteristicSeqno(CharacteristicValue characteristicValue) {
    OBCriteria<TreeNode> criteria = OBDal.getInstance().createCriteria(TreeNode.class);
    criteria.add(
        Restrictions.eq(TreeNode.PROPERTY_CLIENT, OBContext.getOBContext().getCurrentClient()));
    criteria.add(Restrictions.eq(TreeNode.PROPERTY_NODE, characteristicValue.getId()));
    criteria.setMaxResults(1);
    TreeNode treeNode = (TreeNode) criteria.uniqueResult();
    return treeNode == null ? 0 : treeNode.getSequenceNumber();
  }
}
