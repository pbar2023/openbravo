/*
 ************************************************************************************
 * Copyright (C) 2019-2022 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.common.actionhandler;

import java.util.HashSet;
import java.util.Set;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.TreeUtility;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.ProductCategory;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;

public class OfferPickProductCategoryActionHandler extends OfferPickAndExecBaseActionHandler {

  @Override
  protected void doPickAndExecute(String offerId, PriceAdjustment priceAdjustment, Client client,
      Organization org, JSONArray selectedLines) throws JSONException {
    Set<String> childTreePc = getAllChildProductCategories(selectedLines);
    int i = 0;
    for (String productCategoryId : childTreePc) {
      ProductCategory prdCat = (ProductCategory) OBDal.getInstance()
          .getProxy(ProductCategory.ENTITY_NAME, productCategoryId);
      if (!prdCat.isSummaryLevel().booleanValue()) {
        org.openbravo.model.pricing.priceadjustment.ProductCategory item = OBProvider.getInstance()
            .get(org.openbravo.model.pricing.priceadjustment.ProductCategory.class);
        item.setActive(true);
        item.setClient(client);
        item.setOrganization(org);
        item.setPriceAdjustment(priceAdjustment);
        item.setProductCategory(prdCat);
        OBDal.getInstance().save(item);
        i++;
        if ((i % 100) == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
        }
      }
    }
    if (i > 0) {
      OBDal.getInstance().flush();
    }
  }

  /**
   * Return all child tree of selected product categories
   * 
   * @param selectedLines
   *          The product categories objects in JSON format. Each JSON object must have at least the
   *          property "id"
   * @return List of the product category IDs
   * @throws JSONException
   */

  public static Set<String> getAllChildProductCategories(JSONArray selectedLines)
      throws JSONException {
    Set<String> result = new HashSet<>();
    TreeUtility treeUtility = new TreeUtility();
    for (int i = 0; i < selectedLines.length(); i++) {
      JSONObject productCat = selectedLines.getJSONObject(i);
      result.addAll(treeUtility.getChildTree(productCat.getString("id"), "PC"));
    }
    return result;
  }

  @Override
  protected String getJSONName() {
    return "Confprodcatprocess";
  }

}
