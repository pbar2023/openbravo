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
 * All portions are Copyright (C) 2022 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.materialmgmt;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_process.ADProcessID;
import org.openbravo.model.ad.domain.ListTrl;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.FieldTrl;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessRunnerHook;

/**
 * Synchronizes the terminology of the fields based in relevant characteristic properties. This kind
 * of fields are not backed by a DB physical column and therefore their terminology cannot be
 * synchronized through the AD_Synchronize DB procedure. Note that the synchronization logic used by
 * this class is a simplification respect of the DB procedure, as this class assumes that the
 * language of the module of each of the relevant characteristics whose translation is synchronized
 * is the same as the language of the module of the fields.
 */
@ADProcessID("172")
public class RelevantCharacteristicFieldTerminologySynchronizer implements ProcessRunnerHook {

  private static final Logger log = LogManager.getLogger();

  @Override
  public void onExecutionFinish(ProcessBundle bundle) {
    for (Field field : getNonSynchronizableFieldsByDB()) {
      synchronizeField(field);
    }
  }

  private List<Field> getNonSynchronizableFieldsByDB() {
    //@formatter:off
    final String hql = " as f" +
                       " where f.column is null" +
                       "  and f.clientclass is null" +
                       "  and f.property is not null" +
                       "  and f.centralMaintenance = true" +
                       "  and f.module.inDevelopment = true";
    //@formatter:on
    return OBDal.getInstance().createQuery(Field.class, hql).list();
  }

  private void synchronizeField(Field field) {
    RelevantCharacteristicProperty.from(field).ifPresent(p -> {
      field.setName(p.getFieldName());
      field.setHelpComment(p.getDescription());

      // synchronize field translations
      for (FieldTrl fieldTrl : field.getADFieldTrlList()) {
        ListTrl listTrl = getTranslation(p.getRefListId(), fieldTrl.getLanguage());
        if (listTrl != null) {
          fieldTrl.setName(listTrl.getName());
          fieldTrl.setHelpComment(listTrl.getDescription());
        } else {
          log.warn("Translation for relevant characteristic {} in {} language not found",
              p.getSearchKey(), fieldTrl.getLanguage().getName());
        }
      }
    });
  }

  private ListTrl getTranslation(String refListId, Language language) {
    //@formatter:off
    String hql = " as trl" +
                 " where trl.listReference.id = :refListId" +
                 "  and language.id = :languageId";
    //@formatter:on
    return OBDal.getInstance()
        .createQuery(ListTrl.class, hql)
        .setNamedParameter("refListId", refListId)
        .setNamedParameter("languageId", language.getId())
        .uniqueResult();
  }

  /**
   * Detects if the terminology of any of the centrally maintained fields based in relevant
   * characteristic properties is not synchronized. If this validation is not passed it means that
   * it is missing to execute the synchronize terminology process.
   * 
   * @throws OBException
   *           in case there are fields without its terminology not properly synchronized
   */
  void validate() {
    List<Field> nonSynchronizedFields = getNonSynchronizableFieldsByDB().stream()
        .filter(f -> !this.isSynchronized(f))
        .collect(Collectors.toList());
    if (!nonSynchronizedFields.isEmpty()) {
      log.error("The terminology is not synchronized for these fields: {}",
          () -> nonSynchronizedFields.stream().map(Field::getId).collect(Collectors.joining(", ")));
      throw new OBException("Synchronize terminology pending execution found");
    }
  }

  private boolean isSynchronized(Field field) {
    return RelevantCharacteristicProperty.from(field)
        .map(p -> field.getName().equals(p.getFieldName())
            && field.getHelpComment().equals(p.getDescription()))
        .orElse(true);
  }
}
