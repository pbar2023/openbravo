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
 * All portions are Copyright (C) 2019-2024 Openbravo SLU
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;
import org.openbravo.model.pricing.priceadjustment.Product;
import org.openbravo.model.pricing.priceadjustment.PromotionType;

public class ImportProductInDiscount extends ProcessUploadedFile {
  private static final long serialVersionUID = 1L;
  private static final SimpleDateFormat date_format = new SimpleDateFormat("yyyy/MM/dd");

  @Override
  protected void clearBeforeImport(String ownerId, JSONObject paramValues) {
    @SuppressWarnings("unchecked")
    NativeQuery<String> qry = OBDal.getInstance()
        .getSession()
        .createNativeQuery(
            "update m_offer_product set updated=now(), updatedby=:userId, isactive='N' where m_offer_id = :m_offer_id");
    qry.setParameter("userId", OBContext.getOBContext().getUser().getId());
    qry.setParameter("m_offer_id", ownerId);
    qry.executeUpdate();
  }

  @Override
  protected UploadResult doProcessFile(JSONObject paramValues, File file) throws Exception {
    UploadResult uploadResult = new UploadResult();
    final String discountId = paramValues.getString("inpOwnerId");
    final PriceAdjustment discount = OBDal.getInstance().get(PriceAdjustment.class, discountId);
    try (BufferedReader br = Files.newBufferedReader(Paths.get(file.getAbsolutePath()))) {
      String line;
      while ((line = br.readLine()) != null) {
        int currentErrorCount = uploadResult.getErrorCount();
        final String[] productParams = line.trim().split(",");
        final String productKey = productParams[0];
        boolean isActive = true;
        if (productParams.length >= 2) {
          isActive = BooleanUtils.toBoolean(productParams[1]);
        }
        // ignore spaces
        if (productKey.length() == 0) {
          continue;
        }

        uploadResult.incTotalCount();

        final List<String> productIds = getProductIds(discount.getClient().getId(),
            discount.getOrganization().getId(), productKey);
        if (productIds.isEmpty()) {
          setErrorLine(uploadResult, productKey, "OBUIAPP_ProductNotFound");
        } else {
          for (String productId : productIds) {
            // check if the line already exists
            final OBQuery<Product> productDiscountQry = OBDal.getInstance()
                .createQuery(Product.class,
                    "m_offer_id=:m_offer_id and m_product_id=:m_product_id");
            productDiscountQry.setNamedParameter("m_offer_id", discountId);
            productDiscountQry.setNamedParameter("m_product_id", productId);
            productDiscountQry.setFilterOnReadableOrganization(false);
            productDiscountQry.setFilterOnActive(false);
            productDiscountQry.setMaxResult(1);
            Product productDiscount = productDiscountQry.uniqueResult();
            if (productDiscount == null) {
              // create a new one
              productDiscount = OBProvider.getInstance().get(Product.class);
              productDiscount.setClient(discount.getClient());
              productDiscount.setOrganization(discount.getOrganization());
              productDiscount.setPriceAdjustment(discount);
              productDiscount.setProduct(
                  OBDal.getInstance().get(org.openbravo.model.common.plm.Product.class, productId));
            }
            productDiscount.setActive(isActive);

            if (isPriceAdjustmentScopeByProduct(discount)) {
              // do all the formats and constraints checks
              if (productParams.length < 5) {
                setErrorLine(uploadResult, productKey, "OBUIAPP_PriceAdjustmentTypeNotSet");
                continue;
              }

              // ------------- STARTING DATE -------------
              // startingDate is not mandatory
              Date startingDate = getDateFromString(productParams[2], uploadResult, productKey);
              if (checkIfAnErrorHasOccurred(uploadResult, currentErrorCount)) {
                continue;
              }
              // ------------- ENDING DATE -------------
              // endingDate is not mandatory
              Date endingDate = getDateFromString(productParams[3], uploadResult, productKey);
              if (checkIfAnErrorHasOccurred(uploadResult, currentErrorCount)) {
                continue;
              }
              validateDateConstraints(uploadResult, productKey, startingDate, endingDate, discount);

              if (checkIfAnErrorHasOccurred(uploadResult, currentErrorCount)) {
                continue;
              }

              // ------------- PRICE ADJUSTMENT TYPE -------------

              final String priceAdjustmentType = productParams[4];

              // set the index for getting the value
              HashMap<String, Integer> priceAdjustmentValueIndex = new HashMap<>();
              priceAdjustmentValueIndex.put("P", 5);
              priceAdjustmentValueIndex.put("A", 6);
              priceAdjustmentValueIndex.put("F", 7);

              validatePriceAjustmentType(uploadResult, productKey, priceAdjustmentType);
              if (checkIfAnErrorHasOccurred(uploadResult, currentErrorCount)) {
                continue;
              }
              validatePriceAdjustmentValue(uploadResult, productKey,
                  priceAdjustmentValueIndex.get(priceAdjustmentType), productParams);
              if (checkIfAnErrorHasOccurred(uploadResult, currentErrorCount)) {
                continue;
              }

              // At this point, all validations have passed successfully. So the info is set to the
              // product discount
              productDiscount.setPriceAdjustmentType(priceAdjustmentType);
              setPriceAdjustmentTypeValue(
                  productParams[priceAdjustmentValueIndex.get(priceAdjustmentType)],
                  productDiscount, priceAdjustmentType);
              productDiscount.setStartingDate(startingDate);
              productDiscount.setEndingDate(endingDate);
            }
            OBDal.getInstance().save(productDiscount);
          }
        }
      }
    }
    OBDal.getInstance().flush();
    return uploadResult;

  }

  /**
   * 
   * @param discount
   *          {@link PriceAdjustment} - discount to check if its type is Price Adjustment by product
   * @return true if the type of the discount is Price Adjustment with by product scope
   */
  private boolean isPriceAdjustmentScopeByProduct(PriceAdjustment discount) {
    PromotionType promotionType = discount.getDiscountType();
    return promotionType.getId().equals("5D4BAF6BB86D4D2C9ED3D5A6FC051579")
        && discount.getPriceAdjustmentScope().equals("E");
  }

  /**
   * Checks if new errors have occurred in the provided {@link UploadResult} object since the last
   * update.
   *
   * Compares the current error count in the UploadResult with the specified currentErrorCount. If
   * the current error count is greater than the specified value, it indicates that new errors have
   * occurred since the last update.
   * 
   * @param uploadResult
   *          The {@link UploadResult} object to be checked for new errors.
   * @param currentErrorCount
   *          {@link Integer} that contains the error count value obtained from the previous check.
   * @return true if new errors have occurred since the last update, false otherwise.
   */
  private boolean checkIfAnErrorHasOccurred(UploadResult uploadResult, int currentErrorCount) {
    return uploadResult.getErrorCount() > currentErrorCount;
  }

  /**
   * Validates a price adjustment value for a product discount and updates the provided
   * {@link UploadResult} accordingly. Checks if the productParams array has enough elements to
   * access the parameter at the given index and if the value at that index is numeric. If any of
   * this validations are violated, an error message is added to the UploadResult.
   *
   * 
   * @param uploadResult
   *          The {@link UploadResult} object to be updated with error messages if validation fails.
   * @param productKey
   *          {@link String} that contains the product key associated with the price adjustment.
   * @param index
   *          {@link Integer} that contains the index of the product parameter to be validated.
   * @param productParams
   *          {@link String} array that contains the product parameters.
   */
  private void validatePriceAdjustmentValue(UploadResult uploadResult, String productKey, int index,
      String[] productParams) {
    if (productParams.length < index + 1) {
      String[] messageParams = { Integer.toString(index + 1),
          Integer.toString(productParams.length) };
      setErrorLine(uploadResult, productKey, "OBUIAPP_MissingParameters", messageParams);
    } else if (!isNumericString(productParams[index])) {
      String[] messageParams = { productParams[index] };
      setErrorLine(uploadResult, productKey, "OBUIAPP_IncorrectNumericParameter", messageParams);
    }
  }

  /**
   * Validates the price adjustment type for a product discount and updates the provided
   * {@link UploadResult} accordingly. Checks if the price adjustment type is one of the accepted
   * values ("P" for discount percentage, "A" for discount amount, and "F" for fixed price). If the
   * provided price adjustment type is not one of the accepted values, an error message is added to
   * the UploadResult.
   * 
   * @param uploadResult
   *          The {@link UploadResult} object to be updated with error messages if the price
   *          adjustment type is invalid.
   * @param productKey
   *          {@link String} that contains the product key associated with the price adjustment.
   * @param priceAdjustmentType
   *          {@link String} that contains the price adjustment type to be validated.
   */
  private void validatePriceAjustmentType(UploadResult uploadResult, String productKey,
      String priceAdjustmentType) {
    if (!priceAdjustmentType.equals("P") && !priceAdjustmentType.equals("A")
        && !priceAdjustmentType.equals("F")) {
      String[] messageParams = { priceAdjustmentType };
      setErrorLine(uploadResult, productKey, "OBUIAPP_UnexistingPriceAdjustmentType",
          messageParams);
    }
  }

  /**
   * Validates date constraints for a product discount and updates the provided {@link UploadResult}
   * accordingly. Checks if the starting date is before the ending date, and if the starting and
   * ending dates fall within the valid date range specified by the discount. If any constraint is
   * violated, an error message is added to the UploadResult.
   * 
   * @param uploadResult
   *          The {@link UploadResult} object to be updated with error messages if constraints are
   *          violated.
   * @param productKey
   *          {@link String} that contains the product key associated with the discount.
   * @param startingDate
   *          {@link Date} that contains the starting date of the discount period.
   * @param endingDate
   *          {@link Date} that contains the ending date of the discount period.
   * @param discount
   *          The {@link PriceAdjustment} object representing the discount and its date range.
   */
  private void validateDateConstraints(UploadResult uploadResult, String productKey,
      Date startingDate, Date endingDate, PriceAdjustment discount) {
    Date startingDateDiscount = discount.getStartingDate();
    Date endingDateDiscount = discount.getEndingDate();
    if (startingDate != null && endingDate != null && (startingDate.after(endingDate))) {
      setErrorLine(uploadResult, productKey, "M_OFFER_DATE_CHK");
    } else if (startingDate != null && (startingDate.before(startingDateDiscount))) {
      setErrorLine(uploadResult, productKey, "PriceAdjustmentProductDateError");
    } else if (endingDate != null && endingDateDiscount != null
        && (endingDate.after(endingDateDiscount))) {
      setErrorLine(uploadResult, productKey, "PriceAdjustmentProductDateError");
    }
  }

  /**
   * Sets the price adjustment type value for a given product discount based on the provided
   * parameters.
   * 
   * @param param
   *          {@link String} that contains the value of the price adjustment type to be set on the
   *          product discount
   * @param productDiscount
   *          The {@link Product} object that contains the product discount to be modified.
   * @param priceAdjustmentType
   *          {@link String} that contains the type of price adjustment to be applied ("P" for
   *          discount percentage, "A" for discount amount, and "F" for fixed price).
   */
  private void setPriceAdjustmentTypeValue(String param, Product productDiscount,
      String priceAdjustmentType) {
    if (priceAdjustmentType.equals("P")) {
      productDiscount.setDiscount(new BigDecimal(param));
    } else if (priceAdjustmentType.equals("A")) {
      productDiscount.setDiscountAmount(new BigDecimal(param));
    } else if (priceAdjustmentType.equals("F")) {
      productDiscount.setFixedPrice(new BigDecimal(param));
    }
  }

  /**
   * Increments the error count in the provided {@link UploadResult} object and adds an error
   * message to it, constructed using the given product key and message name.
   * 
   * @param uploadResult
   *          The {@link UploadResult} object in which the error message will be added
   * @param productKey
   *          {@link String} that contains the product key associated with the line where the error
   *          occured
   * @param msgName
   *          A {@link String} that contains the name of the message used to express the error.
   */
  private void setErrorLine(UploadResult uploadResult, String productKey, String msgName) {
    uploadResult.incErrorCount();
    uploadResult.addErrorMessage(
        productKey + " --> " + OBMessageUtils.getI18NMessage(msgName, new String[0]) + "\n");
  }

  /**
   * Increments the error count in the provided {@link UploadResult} object and adds an error
   * message to it, constructed using the given product key, message name, and message parameters.
   * 
   * @param uploadResult
   *          The {@link UploadResult} object in which the error message will be added
   * @param productKey
   *          {@link String} that contains the product key associated with the line where the error
   *          occured
   * @param msgName
   *          A {@link String} that contains the name of the message used to express the error.
   * @param messageParams
   *          An {@link String} array that contains the parameters to be substituted in the error
   *          message.
   */
  private void setErrorLine(UploadResult uploadResult, String productKey, String msgName,
      String[] messageParams) {
    uploadResult.incErrorCount();
    uploadResult.addErrorMessage(
        productKey + " --> " + OBMessageUtils.getI18NMessage(msgName, messageParams) + "\n");
  }

  /**
   * Increments the error count in the provided {@link UploadResult} object and adds an error
   * message to it, constructed using the given product key, message name, and additional
   * information.
   * 
   * @param uploadResult
   *          The {@link UploadResult} object in which the error message will be added
   * @param productKey
   *          {@link String} that contains the product key associated with the line where the error
   *          occured
   * @param msgName
   *          {@link String} that contains the name of the message used to express the error
   * @param extraInfo
   *          {@link String} that contains additional information to be appended to the error
   *          message
   */
  private void setErrorLine(UploadResult uploadResult, String productKey, String msgName,
      String extraInfo) {
    uploadResult.incErrorCount();
    uploadResult.addErrorMessage(productKey + " --> "
        + OBMessageUtils.getI18NMessage(msgName, new String[0]) + extraInfo + "\n");
  }

  /**
   * Returns the {@link Date} object parsed from the provided string parameter or add an error to
   * the {@link UploadResult} object in case the parse fails.
   * 
   * @param dateString
   *          {@link String} object that contains the date to be parsed.
   * @param The
   *          {@link UploadResult} object in which the error message will be added
   * @param productKey
   *          {@link String} that contains the product key associated with the line where the error
   *          occured
   * @return the {@link Date} object corresponding to the parsed string, or null if the dateString
   *         is blank or cannot be parsed.
   */
  private Date getDateFromString(String dateString, UploadResult uploadResult, String productKey) {
    Date date;
    if (!StringUtils.isBlank(dateString)) {
      try {
        date = date_format.parse(dateString);
      } catch (ParseException e) {
        setErrorLine(uploadResult, productKey, "InvalidDateFormat", " : " + dateString);
        return null;
      }
      return date;
    }
    return null;
  }

  /**
   * Checks if a string is numeric. If the parseDouble method does not throw any
   * NumberFormatException exception it means that the string is numeric. If a NumberFormatException
   * exception is thrown or the string is empty it means that the string is not numeric.
   * 
   * @param param
   *          - String to check
   * @return true if the string is numeric false if not
   */
  private boolean isNumericString(String param) {
    if (StringUtils.isBlank(param)) {
      return false;
    }
    try {
      Double.parseDouble(param);
    } catch (NumberFormatException e) {
      return false;
    }
    return true;
  }

  @SuppressWarnings("unchecked")
  protected List<String> getProductIds(String clientId, String orgId, String productKey) {
    String sql = "SELECT p.m_product_id from m_product p "
        + "where p.ad_client_id=:clientId and p.value=:value and "
        + "((ad_isorgincluded(:orgId, p.ad_org_id, :clientId) <> -1) or "
        + "(ad_isorgincluded( p.ad_org_id,:orgId, :clientId) <> -1))";
    Session session = OBDal.getInstance().getSession();
    @SuppressWarnings("rawtypes")
    NativeQuery qry = session.createNativeQuery(sql);
    qry.setParameter("clientId", clientId);
    qry.setParameter("orgId", orgId);
    qry.setParameter("value", productKey);
    return (List<String>) qry.list();
  }
}
