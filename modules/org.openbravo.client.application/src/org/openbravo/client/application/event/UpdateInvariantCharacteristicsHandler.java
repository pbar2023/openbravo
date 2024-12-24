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
 * All portions are Copyright (C) 2013-2024 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.event;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.VariantChDescUpdateProcess;
import org.openbravo.model.common.plm.Characteristic;
import org.openbravo.model.common.plm.CharacteristicValue;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCharacteristic;
import org.openbravo.model.common.plm.ProductCharacteristicValue;
import org.openbravo.service.db.DbUtility;

/**
 * Process in charge of updating the product characteristics
 * 
 * When this process is called with the INITIALIZE action, it will provide the data needed to
 * initialize a popup that displays the invariant characteristics of the provided product
 * 
 * This process can also be called with the UPDATE action. In that case, it will update the value of
 * the product invariant characteristics using the provided data
 * 
 * 
 * @author augusto.mauch
 * 
 */
public class UpdateInvariantCharacteristicsHandler extends BaseActionHandler {
  final static private Logger log = LogManager.getLogger();
  private static final String NO_SUBSET = "-1";

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject response = new JSONObject();
    try {
      final JSONObject request = new JSONObject(content);

      // Possible actions:
      // - INITIALIZE: Populates the product characteristics pop up
      // - UPDATE: Updates the product characteristics based on the values selected in the popup
      final String action = request.getString("action");

      if ("INITIALIZE".equals(action)) {
        String productId = request.getString("productId");
        Product product = OBDal.getInstance().get(Product.class, productId);

        // Retrieves all the product invariant characteristics
        //@formatter:off
        final String hql = " as pch "
            + " where pch.product.id = :productId "
            + "   and pch.variant = :variant "
            + "   and pch.characteristic.isAssignValuesByOrg = :chValuesByOrg ";
        //@formatter:on
        OBQuery<ProductCharacteristic> query = OBDal.getInstance()
            .createQuery(ProductCharacteristic.class, hql)
            .setNamedParameter("productId", product.getId())
            .setNamedParameter("variant", false)
            .setNamedParameter("chValuesByOrg", false);

        JSONArray productCharArray = new JSONArray();

        final List<ProductCharacteristic> invariantCharacteristics = query.list();
        for (ProductCharacteristic characteristic : invariantCharacteristics) {
          JSONObject productChar = new JSONObject();
          // Retrieves the current selected value
          OBCriteria<ProductCharacteristicValue> criteriaSelectedValue = OBDal.getInstance()
              .createCriteria(ProductCharacteristicValue.class);
          criteriaSelectedValue
              .add(Restrictions.eq(ProductCharacteristicValue.PROPERTY_CHARACTERISTIC,
                  characteristic.getCharacteristic()));
          criteriaSelectedValue.add(Restrictions.eq(ProductCharacteristicValue.PROPERTY_PRODUCT,
              characteristic.getProduct()));
          ProductCharacteristicValue selectedValue = (ProductCharacteristicValue) criteriaSelectedValue
              .uniqueResult();
          productChar.put("id", characteristic.getCharacteristic().getId());
          if (selectedValue == null) {
            productChar.put("selectedValue", "");
            productChar.put("existingProdChValue", "");
          } else {
            productChar.put("selectedValue", selectedValue.getCharacteristicValue().getId());
            productChar.put("existingProdChValue", selectedValue.getId());
          }
          productChar.put("title", characteristic.getCharacteristic().getName());
          productChar.put("name", characteristic.getCharacteristic().getName().replace("/", "__"));

          // Retrieves all the possible values for the characteristic
          List<CharacteristicValue> values = characteristic.getCharacteristic()
              .getCharacteristicValueList();
          JSONObject productCharValuesValueMap = new JSONObject();
          // adding empty value to map to allow selecting empty value to delete the characteristic
          productCharValuesValueMap.put("", "");
          for (CharacteristicValue value : values) {
            productCharValuesValueMap.put(value.getId(), value.getIdentifier());
          }
          productChar.put("valueMap", productCharValuesValueMap);
          productCharArray.put(productChar);
          if (characteristic.getCharacteristicSubset() != null) {
            productChar.put("productCharSubsetId",
                characteristic.getCharacteristicSubset().getId());
          } else {
            productChar.put("productCharSubsetId", NO_SUBSET);
          }
        }
        response.put("productCharList", productCharArray);
        response.put("productId", productId);
        return response;
      } else {
        String productId = request.getString("productId");
        Product product = OBDal.getInstance().get(Product.class, productId);
        final JSONObject updatedValues = request.getJSONObject("updatedValues");
        final JSONObject existingProdChValues = request.getJSONObject("existingProdChValues");

        deleteNotSelectedProdCharValues(productId, existingProdChValues);

        @SuppressWarnings("unchecked")
        Iterator<String> keysIterator = updatedValues.keys();
        while (keysIterator.hasNext()) {
          String characteristicId = keysIterator.next();
          String updatedValueId = updatedValues.getString(characteristicId);
          String strProdChValueId = existingProdChValues.getString(characteristicId);
          Characteristic ch = OBDal.getInstance().get(Characteristic.class, characteristicId);
          CharacteristicValue charValue = OBDal.getInstance()
              .get(CharacteristicValue.class, updatedValueId);
          ProductCharacteristicValue prodCharValue = OBDal.getInstance()
              .get(ProductCharacteristicValue.class, strProdChValueId);
          if (prodCharValue == null && charValue != null) {
            prodCharValue = OBProvider.getInstance().get(ProductCharacteristicValue.class);
            prodCharValue.setCharacteristic(ch);
            prodCharValue.setProduct(product);
            prodCharValue.setOrganization(product.getOrganization());
            prodCharValue.setCharacteristicValue(charValue);
            OBDal.getInstance().save(prodCharValue);
          } else if (prodCharValue != null && charValue != null) {
            prodCharValue.setCharacteristicValue(charValue);
            OBDal.getInstance().save(prodCharValue);
          } else if (prodCharValue != null && charValue == null) {
            OBDal.getInstance().remove(prodCharValue);
          }

        }

        OBDal.getInstance().flush();
        new VariantChDescUpdateProcess().update(productId, null);
        JSONObject message = new JSONObject();
        message.put("severity", "success");
        message.put("text", OBMessageUtils.messageBD("UpdateCharacteristicsSuccess"));
        response.put("message", message);
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("UpdateInvariantCharacteristics error: " + e.getMessage(), e);
      Throwable ex = DbUtility.getUnderlyingSQLException(e);
      String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
      try {
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        response.put("message", errorMessage);
      } catch (JSONException ignore) {
      }
    }
    return response;
  }

  private void deleteNotSelectedProdCharValues(String productId, final JSONObject prodChValues) {
    @SuppressWarnings("unchecked")
    final Set<String> prodChValueIds = (Set<String>) StreamSupport
        .stream(Spliterators.spliteratorUnknownSize(prodChValues.keys(), 0), false)
        .map(key -> {
          try {
            return prodChValues.getString((String) key);
          } catch (JSONException e) {
            throw new OBException(e);
          }
        })
        .collect(Collectors.toSet());
    //@formatter:off
    final String hql = "delete from ProductCharacteristicValue pchv " +
                       "where pchv.product.id = :productId " + 
                       "  and pchv.id not in :prodChValueIds ";
    //@formatter:on

    OBDal.getInstance()
        .getSession()
        .createQuery(hql)
        .setParameter("productId", productId)
        .setParameterList("prodChValueIds", prodChValueIds)
        .executeUpdate();
  }
}
