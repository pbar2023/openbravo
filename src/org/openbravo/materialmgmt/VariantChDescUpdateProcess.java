/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
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
 *************************************************************************
 */
package org.openbravo.materialmgmt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Tuple;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.GenericJDBCException;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Characteristic;
import org.openbravo.model.common.plm.CharacteristicTrl;
import org.openbravo.model.common.plm.CharacteristicValue;
import org.openbravo.model.common.plm.CharacteristicValueTrl;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCharacteristic;
import org.openbravo.model.common.plm.ProductCharacteristicDescriptionOrg;
import org.openbravo.model.common.plm.ProductCharacteristicValue;
import org.openbravo.model.common.plm.ProductTrl;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

public class VariantChDescUpdateProcess extends DalBaseProcess {
  private static final Logger log4j = LogManager.getLogger();
  public static final String AD_PROCESS_ID = "58591E3E0F7648E4A09058E037CE49FC";
  private static final String ERROR_MSG_TYPE = "Error";

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    OBError msg = new OBError();
    msg.setType("Success");
    msg.setTitle(OBMessageUtils.messageBD("Success"));

    try {
      // retrieve standard params
      String strProductId = (String) bundle.getParams().get("mProductId");
      String strChValueId = (String) bundle.getParams().get("mChValueId");

      update(strProductId, strChValueId);

      bundle.setResult(msg);

      // Postgres wraps the exception into a GenericJDBCException
    } catch (GenericJDBCException ge) {
      log4j.error("Exception processing variant generation", ge);
      msg.setType(ERROR_MSG_TYPE);
      msg.setTitle(OBMessageUtils.messageBD(bundle.getConnection(), ERROR_MSG_TYPE,
          bundle.getContext().getLanguage()));
      msg.setMessage(ge.getSQLException().getMessage().split("\n")[0]);
      bundle.setResult(msg);
      OBDal.getInstance().rollbackAndClose();
    } catch (final Exception e) {
      log4j.error("Exception processing variant generation", e);
      msg.setType(ERROR_MSG_TYPE);
      msg.setTitle(OBMessageUtils.messageBD(bundle.getConnection(), ERROR_MSG_TYPE,
          bundle.getContext().getLanguage()));
      msg.setMessage(FIN_Utility.getExceptionMessage(e));
      bundle.setResult(msg);
      OBDal.getInstance().rollbackAndClose();
    }

  }

  /**
   * Method to update the Characteristics Description.
   * 
   * @param strProductId
   *          Optional parameter, when given updates only the description of this product.
   * @param strChValueId
   *          Optional parameter, when given updates only products with this characteristic value
   *          assigned.
   */
  public void update(String strProductId, String strChValueId) {
    OBContext.setAdminMode(true);
    try {

      boolean translationRequired = false;

      // @formatter:off
      final String trlQryStr = " as l"
                             + " where l.systemLanguage = true ";
      // @formatter:on
      final OBQuery<Language> languagesHql = OBDal.getInstance()
          .createQuery(Language.class, trlQryStr);
      List<Language> languages = languagesHql.list();
      if (languages.size() > 1) {
        translationRequired = true;
      }

      if (StringUtils.isNotBlank(strProductId)) {
        Product product = OBDal.getInstance().get(Product.class, strProductId);
        // In some cases product might have been deleted.
        if (product != null) {
          updateProduct(product);
          updateProductWithCharValuesByOrg(product);
          if (translationRequired) {
            updateProductTrl(product, languages);
          }
        }
        return;
      }
      //@formatter:off
      String hql = " as p"
                 + " where p.productCharacteristicList is not empty ";
      if (StringUtils.isNotBlank(strChValueId)) {
        hql += " and exists (select 1 "
             + "              from p.productCharacteristicValueList as chv "
             + "              where chv.characteristicValue.id = :chvid) ";
      }
      //@formatter:on
      OBQuery<Product> productQuery = OBDal.getInstance()
          .createQuery(Product.class, hql)
          .setFilterOnReadableOrganization(false)
          .setFilterOnActive(false);
      if (StringUtils.isNotBlank(strChValueId)) {
        productQuery.setNamedParameter("chvid", strChValueId);
      }

      ScrollableResults products = productQuery.scroll(ScrollMode.FORWARD_ONLY);
      int i = 0;
      try {
        while (products.next()) {
          Product product = (Product) products.get(0);
          updateProduct(product);
          updateProductWithCharValuesByOrg(product);
          if (translationRequired) {
            updateProductTrl(product, languages);
          }

          if ((i % 100) == 0) {
            OBDal.getInstance().flush();
            OBDal.getInstance().getSession().clear();
          }
          i++;
        }
      } finally {
        products.close();
      }

    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void updateProduct(Product product) {
    updateProduct(product, false);
  }

  private void updateProductWithCharValuesByOrg(Product product) {
    updateProduct(product, true);
  }

  private void updateProduct(Product product, boolean assignCharValuesByOrg) {
    //@formatter:off
    final String hql = " as pch "
               + " where pch.product.id = :productId "
               + "   and pch.characteristic.isAssignValuesByOrg = :assignCharValuesByOrg "
               + " order by pch.sequenceNumber ";
    //@formatter:on
    OBQuery<ProductCharacteristic> pchQuery = OBDal.getInstance()
        .createQuery(ProductCharacteristic.class, hql)
        .setFilterOnActive(false)
        .setFilterOnReadableOrganization(false)
        .setNamedParameter("productId", product.getId())
        .setNamedParameter("assignCharValuesByOrg", assignCharValuesByOrg);

    if (assignCharValuesByOrg) {
      setCharDescriptionByOrg(product, pchQuery.list());
    } else {
      setCharDescription(product, pchQuery.list());
    }
  }

  private void setCharDescriptionByOrg(Product product, List<ProductCharacteristic> pchList) {
    // Mapa para agrupar resultados por orgId
    Map<String, StringBuilder> orgToCharValuesMap = new HashMap<>();

    // Construcción del strChDesc inicial
    pchList.forEach(pch -> {
      final String characteristicName = pch.getCharacteristic().getName();

      //@formatter:off
      final String hql = "select org.id as orgId, chValue.name as chValueName "
          + "from ProductCharacteristicOrg pco " 
          + "  join pco.organization org "
          + "  join pco.characteristicValue chValue "
          + "where pco.characteristicOfProduct.id = :pchId ";
      //@formatter:on

      OBDal.getInstance()
          .getSession()
          .createQuery(hql, Tuple.class)
          .setParameter("pchId", pch.getId())
          .stream()
          .forEach(tuple -> {
            String orgId = (String) tuple.get("orgId");
            String chValueName = (String) tuple.get("chValueName");
            orgToCharValuesMap.computeIfAbsent(orgId, k -> new StringBuilder())
                .append(orgToCharValuesMap.get(orgId).length() > 0 ? ", " : "")
                .append(characteristicName)
                .append(": ")
                .append(chValueName);
          });
    });

    orgToCharValuesMap.forEach((orgId, strChDescBuilder) -> {
      String strChDesc = strChDescBuilder.toString();

      Organization organization = OBDal.getInstance().get(Organization.class, orgId);
      OBCriteria<ProductCharacteristicDescriptionOrg> pcvOrgCriteria = OBDal.getInstance()
          .createCriteria(ProductCharacteristicDescriptionOrg.class)
          .add(Restrictions.eq(ProductCharacteristicDescriptionOrg.PROPERTY_PRODUCT, product))
          .add(Restrictions.eq(ProductCharacteristicDescriptionOrg.PROPERTY_ORGANIZATION,
              organization))
          .setMaxResults(1);

      ProductCharacteristicDescriptionOrg pcvOrg = (ProductCharacteristicDescriptionOrg) pcvOrgCriteria
          .uniqueResult();

      if (pcvOrg == null) {
        pcvOrg = OBProvider.getInstance().get(ProductCharacteristicDescriptionOrg.class);
        pcvOrg.setProduct(product);
        pcvOrg.setOrganization(organization);
      }
      pcvOrg.setCharacteristicDescription(strChDesc);
      OBDal.getInstance().save(pcvOrg);
    });
  }

  private void setCharDescription(Product product, List<ProductCharacteristic> pchList) {
    StringBuilder strChDesc = new StringBuilder();
    for (ProductCharacteristic pch : pchList) {
      if (StringUtils.isNotBlank(strChDesc.toString())) {
        strChDesc.append(", ");
      }
      strChDesc.append(pch.getCharacteristic().getName() + ":");
      //@formatter:off
      String hql = " as pchv "
          + " where pchv.characteristic.id = :chId "
          + " and pchv.product.id = :productId ";
      //@formatter:on
      OBQuery<ProductCharacteristicValue> pchvQuery = OBDal.getInstance()
          .createQuery(ProductCharacteristicValue.class, hql)
          .setFilterOnActive(false)
          .setFilterOnReadableOrganization(false)
          .setNamedParameter("chId", pch.getCharacteristic().getId())
          .setNamedParameter("productId", product.getId());

      for (ProductCharacteristicValue pchv : pchvQuery.list()) {
        strChDesc.append(" " + pchv.getCharacteristicValue().getName());
      }
    }
    product.setCharacteristicDescription(strChDesc.toString());
  }

  private void updateProductTrl(Product product, List<Language> languages) {

    for (Language language : languages) {
      // In case it is the language of the Client skip the process as it is supposed to use the
      // standard M_Product
      if (language == OBContext.getOBContext().getCurrentClient().getLanguage()) {
        continue;
      }

      StringBuilder strChDesc = new StringBuilder();
      //@formatter:off
      String hql = " as pch "
                 + " where pch.product.id = :productId "
                 + " order by pch.sequenceNumber ";
      //@formatter:on
      OBQuery<ProductCharacteristic> pchQuery = OBDal.getInstance()
          .createQuery(ProductCharacteristic.class, hql)
          .setFilterOnActive(false)
          .setFilterOnReadableOrganization(false)
          .setNamedParameter("productId", product.getId());

      for (ProductCharacteristic pch : pchQuery.list()) {
        Characteristic characteristic = pch.getCharacteristic();
        String charName = characteristic.getName();
        //@formatter:off
        String pchTrlHql = " as pchtrl "
                         + " where pchtrl.characteristic.id = :characteristicId "
                         + " and pchtrl.language.id = :languageId ";
        //@formatter:on
        OBQuery<CharacteristicTrl> pchTrlQuery = OBDal.getInstance()
            .createQuery(CharacteristicTrl.class, pchTrlHql)
            .setFilterOnActive(false)
            .setFilterOnReadableOrganization(false)
            .setNamedParameter("characteristicId", characteristic.getId())
            .setNamedParameter("languageId", language.getId())
            .setMaxResult(1);
        CharacteristicTrl characteristicTrl = pchTrlQuery.uniqueResult();
        if (characteristicTrl != null) {
          charName = characteristicTrl.getName();
        }

        if (StringUtils.isNotBlank(strChDesc.toString())) {
          strChDesc.append(", ");
        }
        strChDesc.append(charName + ":");

        //@formatter:off
        hql = " as pchv "
            + " where pchv.characteristic.id = :chId "
            + " and pchv.product.id = :productId ";
        //@formatter:on
        OBQuery<ProductCharacteristicValue> pchvQuery = OBDal.getInstance()
            .createQuery(ProductCharacteristicValue.class, hql)
            .setFilterOnActive(false)
            .setFilterOnReadableOrganization(false)
            .setNamedParameter("chId", pch.getCharacteristic().getId())
            .setNamedParameter("productId", product.getId());

        for (ProductCharacteristicValue pchv : pchvQuery.list()) {
          CharacteristicValue chValue = pchv.getCharacteristicValue();
          String chValueName = chValue.getName();
          //@formatter:off
          String pchValueTrlHql = " as pchvaluetrl "
                                + " where pchvaluetrl.characteristicValue.id = :characteristicValueId "
                                + " and pchvaluetrl.language.id = :languageId ";
          //@formatter:on
          OBQuery<CharacteristicValueTrl> pchValueTrlQuery = OBDal.getInstance()
              .createQuery(CharacteristicValueTrl.class, pchValueTrlHql)
              .setFilterOnActive(false)
              .setFilterOnReadableOrganization(false)
              .setNamedParameter("characteristicValueId", chValue.getId())
              .setNamedParameter("languageId", language.getId())
              .setMaxResult(1);
          CharacteristicValueTrl characteristicValueTrl = pchValueTrlQuery.uniqueResult();
          if (characteristicValueTrl != null) {
            chValueName = characteristicValueTrl.getName();
          }
          strChDesc.append(" " + chValueName);
        }
      }

      //@formatter:off
      String hqlProductTrl = " as pt "
                           + " where pt.language.id = :languageId "
                           + " and pt.product.id = :productId ";
      //@formatter:on
      OBQuery<ProductTrl> productTrlQuery = OBDal.getInstance()
          .createQuery(ProductTrl.class, hqlProductTrl)
          .setFilterOnActive(false)
          .setFilterOnReadableOrganization(false)
          .setNamedParameter("languageId", language.getId())
          .setNamedParameter("productId", product.getId())
          .setMaxResult(1);

      ProductTrl productTrl = productTrlQuery.uniqueResult();

      if (productTrl == null) {
        productTrl = OBProvider.getInstance().get(ProductTrl.class);
        productTrl.setLanguage(language);
        productTrl.setName(product.getName());
        productTrl.setProduct(product);
      }
      productTrl.setCharacteristicDescription(strChDesc.toString());
      OBDal.getInstance().save(productTrl);

    }

  }
}
