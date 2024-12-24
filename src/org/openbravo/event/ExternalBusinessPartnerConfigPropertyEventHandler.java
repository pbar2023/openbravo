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
 * All portions are Copyright (C) 2021-2024 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.event;

import java.util.List;

import javax.enterprise.event.Observes;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.financial.paymentreport.erpCommon.ad_reports.PaymentReportDao;
import org.openbravo.model.externalbpartner.ExternalBusinessPartnerConfig;
import org.openbravo.model.externalbpartner.ExternalBusinessPartnerConfigLocation;
import org.openbravo.model.externalbpartner.ExternalBusinessPartnerConfigProperty;

/**
 * Checks the validity of the saved/updated external business partner configuration property
 */
public class ExternalBusinessPartnerConfigPropertyEventHandler
    extends EntityPersistenceEventObserver {
  private static final Entity[] ENTITIES = {
      ModelProvider.getInstance().getEntity(ExternalBusinessPartnerConfigProperty.ENTITY_NAME) };
  private static final String MULTI_INTEGRATION_TYPE = "MI";

  @Override
  protected Entity[] getObservedEntities() {
    return ENTITIES;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    resetValuesWhenReferenceIsInvoiceOrShipping(event);
    checkCRMBusinessPropertyUniqueness(event);
    checkKeyColumnsAndAddress(event);
    checkTranslatableFields(event);
    checkIsUniqueFields(event);
    checkSuggestionsMandatory(event);
    checkValidationsMandatory(event);
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    resetValuesWhenReferenceIsInvoiceOrShipping(event);
    checkCRMBusinessPropertyUniqueness(event);
    checkMandatoryRemovalIfMultiIntegration(event);
    checkIdentifierScanningActionDuplicates(event);
    checkKeyColumnsAndAddress(event);
    checkTranslatableFields(event);
    checkIsUniqueFields(event);
    checkSuggestionsMandatory(event);
    checkValidationsMandatory(event);
  }

  private void resetValuesWhenReferenceIsInvoiceOrShipping(final EntityPersistenceEvent event) {
    BaseOBObject targetInstance = event.getTargetInstance();
    final Entity entity = targetInstance.getEntity();
    final Entity transactionEntity = ModelProvider.getInstance()
        .getEntity(targetInstance.getEntityName());
    final String currentReferenceProperty = (String) event.getCurrentState(
        transactionEntity.getProperty(ExternalBusinessPartnerConfigProperty.PROPERTY_REFERENCE));
    if ("ShippingAddress".equals(currentReferenceProperty)
        || "InvoiceAddress".equals(currentReferenceProperty)) {
      event.setCurrentState(
          entity.getProperty(ExternalBusinessPartnerConfigProperty.PROPERTY_TRANSLATABLE), false);
      event.setCurrentState(entity.getProperty(ExternalBusinessPartnerConfigProperty.PROPERTY_TEXT),
          currentReferenceProperty);
    }
  }

  private void checkMandatoryRemovalIfMultiIntegration(EntityUpdateEvent event) {
    final ExternalBusinessPartnerConfigProperty extBPConfigProperty = (ExternalBusinessPartnerConfigProperty) event
        .getTargetInstance();
    final ExternalBusinessPartnerConfig extBPConfiguration = extBPConfigProperty
        .getExternalBusinessPartnerIntegrationConfiguration();
    if (MULTI_INTEGRATION_TYPE.equals(extBPConfiguration.getTypeOfIntegration())) {
      final Property mandatoryCreateProp = ENTITIES[0]
          .getProperty(ExternalBusinessPartnerConfigProperty.PROPERTY_ISMANDATORYCREATE);
      final Property mandatoryEditProp = ENTITIES[0]
          .getProperty(ExternalBusinessPartnerConfigProperty.PROPERTY_ISMANDATORYEDIT);

      Object previousMandatoryCreatePropObject = event.getPreviousState(mandatoryCreateProp);
      boolean previousMandatoryCreatePropValue = previousMandatoryCreatePropObject == null ? false
          : (Boolean) previousMandatoryCreatePropObject;

      boolean changeMandatoryCreate = previousMandatoryCreatePropValue
          && !((Boolean) event.getCurrentState(mandatoryCreateProp));

      Object previousMandatoryEditPropObject = event.getPreviousState(mandatoryEditProp);
      boolean previousMandatoryEditPropValue = previousMandatoryEditPropObject == null ? false
          : (Boolean) previousMandatoryEditPropObject;

      boolean changeMandatoryEdit = previousMandatoryEditPropValue
          && !((Boolean) event.getCurrentState(mandatoryEditProp));
      if (changeMandatoryCreate || changeMandatoryEdit) {
        // Query to check if the property being managed exists in address mapping
        //@formatter:off
        String hql = " cRMConnectorConfiguration.id = :crmConfigurationId "
            + " and ("
            + "         addressLine1.id = :propertyId "
            + "      or addressLine2.id = :propertyId "
            + "      or cityName.id = :propertyId "
            + "      or postalCode.id = :propertyId "
            + "      or country.id = :propertyId "
            + "      or region.id = :propertyId"
            + "     )";
        //@formatter:on

        OBQuery<ExternalBusinessPartnerConfigLocation> hqlCriteria = OBDal.getInstance()
            .createQuery(ExternalBusinessPartnerConfigLocation.class, hql)
            .setNamedParameter("crmConfigurationId", extBPConfiguration.getId())
            .setNamedParameter("propertyId", extBPConfigProperty.getId());
        hqlCriteria.setMaxResult(1);
        if (hqlCriteria.uniqueResult() != null) {
          throw new OBException(OBMessageUtils.messageBD("UnnasignExtBPAddressPropertyMandatory"));
        }
      }
    }
  }

  private void checkIdentifierScanningActionDuplicates(EntityPersistenceEvent event) {
    final String id = event.getId();
    final ExternalBusinessPartnerConfigProperty property = (ExternalBusinessPartnerConfigProperty) event
        .getTargetInstance();
    final ExternalBusinessPartnerConfig currentExtBPConfig = property
        .getExternalBusinessPartnerIntegrationConfiguration();

    if (!property.isIdentifierscanningaction() || !property.isActive()) {
      return;
    }

    if (!property.getReference().equals("B")) {
      throw new OBException("@NotBooleanTypeCRMIdentifierScanningAction@");
    }

    final OBCriteria<?> criteria = OBDal.getInstance()
        .createCriteria(event.getTargetInstance().getClass());
    criteria.add(Restrictions.eq(
        ExternalBusinessPartnerConfigProperty.PROPERTY_EXTERNALBUSINESSPARTNERINTEGRATIONCONFIGURATION,
        currentExtBPConfig));
    criteria.add(Restrictions
        .eq(ExternalBusinessPartnerConfigProperty.PROPERTY_IDENTIFIERSCANNINGACTION, true));
    criteria.add(Restrictions.eq(ExternalBusinessPartnerConfigProperty.PROPERTY_ACTIVE, true));
    criteria.add(Restrictions.ne(ExternalBusinessPartnerConfigProperty.PROPERTY_ID, id));

    criteria.setMaxResults(1);
    if (criteria.uniqueResult() != null) {
      throw new OBException("@DuplicatedCRMIdentifierScanningAction@");
    }
  }

  private void checkKeyColumnsAndAddress(EntityPersistenceEvent event) {
    final Entity transactionEntity = ModelProvider.getInstance()
        .getEntity(event.getTargetInstance().getEntityName());
    final Boolean currentIsAddressProperty = (Boolean) event.getCurrentState(transactionEntity
        .getProperty(ExternalBusinessPartnerConfigProperty.PROPERTY_ISADDRESSPROPERTY));

    // Check Key Column unique constrain
    final Boolean currentKeyColumn = (Boolean) event.getCurrentState(
        transactionEntity.getProperty(ExternalBusinessPartnerConfigProperty.PROPERTY_KEYCOLUMN));
    if (currentKeyColumn) {
      final String id = event.getId();
      OBCriteria<ExternalBusinessPartnerConfigProperty> criteria = getUniqueCriteria(event,
          transactionEntity);
      criteria.add(Restrictions.eq(ExternalBusinessPartnerConfigProperty.PROPERTY_KEYCOLUMN, true));
      criteria.add(Restrictions.ne(ExternalBusinessPartnerConfigProperty.PROPERTY_ID, id));
      List<ExternalBusinessPartnerConfigProperty> keyColumns = criteria.list();
      if (currentIsAddressProperty) {
        long countAddressKey = keyColumns.stream()
            .filter(ExternalBusinessPartnerConfigProperty::isAddressProperty)
            .count();
        if (countAddressKey > 0) {
          throw new OBException("@DuplicatedCRMAddressKeyColumn@");
        }
        return;
      }
      long countKey = keyColumns.stream().filter(col -> !col.isAddressProperty()).count();
      if (countKey > 0) {
        throw new OBException("@DuplicatedCRMKeyColumn@");
      }
    }

    // Check Address reference constrains
    final String currentReference = (String) event.getCurrentState(
        transactionEntity.getProperty(ExternalBusinessPartnerConfigProperty.PROPERTY_REFERENCE));
    if ("ShippingAddress".equals(currentReference) || "InvoiceAddress".equals(currentReference)) {
      if (currentIsAddressProperty) {
        throw new OBException("@AddressReferenceCRMNotAllowAtAddress@");
      }
      OBCriteria<ExternalBusinessPartnerConfigProperty> criteria = getUniqueCriteria(event,
          transactionEntity);
      criteria.add(Restrictions.eq(ExternalBusinessPartnerConfigProperty.PROPERTY_REFERENCE,
          currentReference));
      if (criteria.count() > 0) {
        throw new OBException("@DuplicatedCRMRefenceAddress@");
      }
    }
  }

  private OBCriteria<ExternalBusinessPartnerConfigProperty> getUniqueCriteria(
      EntityPersistenceEvent event, Entity transactionEntity) {
    final ExternalBusinessPartnerConfig currentConfig = (ExternalBusinessPartnerConfig) event
        .getCurrentState(transactionEntity.getProperty(
            ExternalBusinessPartnerConfigProperty.PROPERTY_EXTERNALBUSINESSPARTNERINTEGRATIONCONFIGURATION));
    final String currentApiKey = (String) event.getCurrentState(
        transactionEntity.getProperty(ExternalBusinessPartnerConfigProperty.PROPERTY_APIKEY));

    final OBCriteria<ExternalBusinessPartnerConfigProperty> criteria = OBDal.getInstance()
        .createCriteria(ExternalBusinessPartnerConfigProperty.class);
    criteria.add(Restrictions.eq(
        ExternalBusinessPartnerConfigProperty.PROPERTY_EXTERNALBUSINESSPARTNERINTEGRATIONCONFIGURATION,
        currentConfig));
    criteria
        .add(Restrictions.ne(ExternalBusinessPartnerConfigProperty.PROPERTY_APIKEY, currentApiKey));
    return criteria;
  }

  private void checkCRMBusinessPropertyUniqueness(EntityPersistenceEvent event) {
    final ExternalBusinessPartnerConfigProperty property = (ExternalBusinessPartnerConfigProperty) event
        .getTargetInstance();

    if (!property.isActive()) {
      return;
    }

    String propertyValue = property.getCrmBusinessProperty();

    final OBCriteria<?> criteria = OBDal.getInstance()
        .createCriteria(event.getTargetInstance().getClass());

    criteria.add(Restrictions.eq(
        ExternalBusinessPartnerConfigProperty.PROPERTY_EXTERNALBUSINESSPARTNERINTEGRATIONCONFIGURATION,
        property.getExternalBusinessPartnerIntegrationConfiguration()));
    criteria.add(Restrictions.eq(ExternalBusinessPartnerConfigProperty.PROPERTY_CRMBUSINESSPROPERTY,
        propertyValue));
    criteria.add(Restrictions.eq(ExternalBusinessPartnerConfigProperty.PROPERTY_ACTIVE, true));
    criteria.add(Restrictions.ne(ExternalBusinessPartnerConfigProperty.PROPERTY_ID, event.getId()));

    criteria.setMaxResults(1);

    if (criteria.uniqueResult() != null) {
      String propertyTranslated = PaymentReportDao.translateRefList(propertyValue);
      String msg = OBMessageUtils.getI18NMessage("DuplicatedCRMBusinessProperty",
          new String[] { propertyTranslated });
      throw new OBException(msg);
    }
  }

  private void checkTranslatableFields(EntityPersistenceEvent event) {
    final ExternalBusinessPartnerConfigProperty extBPConfigProperty = (ExternalBusinessPartnerConfigProperty) event
        .getTargetInstance();
    if (extBPConfigProperty.isTranslatable()) {
      final Entity extBPConfigEntity = ModelProvider.getInstance()
          .getEntity(ExternalBusinessPartnerConfigProperty.ENTITY_NAME);
      final Property textProperty = extBPConfigEntity
          .getProperty(ExternalBusinessPartnerConfigProperty.PROPERTY_TEXT);
      event.setCurrentState(textProperty, "");
    }
  }

  private void checkIsUniqueFields(EntityPersistenceEvent event) {
    final ExternalBusinessPartnerConfigProperty extBPConfigProperty = (ExternalBusinessPartnerConfigProperty) event
        .getTargetInstance();
    if (extBPConfigProperty.isUnique() && extBPConfigProperty.getFilterForUnique() == null) {
      throw new OBException("@NotNullCRMFilterForUnique@");
    }
  }

  private void checkSuggestionsMandatory(EntityPersistenceEvent event) {
    final ExternalBusinessPartnerConfigProperty extBPConfigProperty = (ExternalBusinessPartnerConfigProperty) event
        .getTargetInstance();
    if ("SUGGEST_DQM".equals(extBPConfigProperty.getSuggestions())) {
      if (extBPConfigProperty.getSuggestionsExtSystem() == null) {
        throw new OBException("@NotNullCRMSuggestionsExtSystem@");
      }
      if (extBPConfigProperty.getSuggestionsMinChars() == null) {
        throw new OBException("@NotNullCRMSuggestionsMinChars@");
      }
    }
  }

  private void checkValidationsMandatory(EntityPersistenceEvent event) {
    final ExternalBusinessPartnerConfigProperty extBPConfigProperty = (ExternalBusinessPartnerConfigProperty) event
        .getTargetInstance();
    if (!"no".equals(extBPConfigProperty.getValidation())
        && "dqm".equals(extBPConfigProperty.getValidationtype())) {
      if (extBPConfigProperty.getValidationExtSystem() == null) {
        throw new OBException("@NotNullCRMValidationExtSystem@");
      }
    }
  }
}
