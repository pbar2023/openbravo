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

package org.openbravo.test.sendemail;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.email.EmailUtils;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.test.base.OBBaseTest;

public class SendEmailTest extends OBBaseTest {

  private static final Logger log = LogManager.getLogger();

  // Organization *
  private static final String ORG_STAR_ID = "0";
  // Organization F&B España - Región Norte
  private static final String FB_SP_REGION_NORTE = "E443A31992CB4635AFCAEABE7183CE85";
  // Organization F&B España - Región Sur
  private static final String FB_SP_REGION_SUR = "DC206C91AA6A4897B44DA897936E0EC3";
  // Organization F&B International Group
  private static final String FB_IT_GROUP = "19404EAD144C49A0AF37D54377CF452D";

  @Test
  public void testSendEmailConfiguration() {
    setTestUserContext();

    Organization orgStar = OBDal.getInstance().get(Organization.class, ORG_STAR_ID);
    Organization orgSPRegionNorte = OBDal.getInstance().get(Organization.class, FB_SP_REGION_NORTE);
    Organization orgSPRegionSur = OBDal.getInstance().get(Organization.class, FB_SP_REGION_SUR);
    Organization orgItGroup = OBDal.getInstance().get(Organization.class, FB_IT_GROUP);
    List<Organization> orgList = Arrays.asList(orgStar, orgSPRegionNorte, orgSPRegionSur);

    // Populate tab Email Configuration
    List<EmailServerConfiguration> listEmailConfigs = insertEmailConfigurationTestByOrganization(
        orgList);
    assertTrue(listEmailConfigs.size() == orgList.size());
    log.info(
        "Populate tab Email Configuration result :" + listEmailConfigs.size() + " rows created");

    // Check if email configurations exist by organization
    log.info("Checking exist configurations by organization : " + orgSPRegionNorte.getName());
    final EmailServerConfiguration mailConfigSpRegionNorte = EmailUtils
        .getEmailConfiguration(orgSPRegionNorte);
    assertTrue(mailConfigSpRegionNorte.getOrganization().equals(orgSPRegionNorte));

    log.info("Checking exist configurations by organization : " + orgSPRegionSur.getName());
    final EmailServerConfiguration mailConfigSpRegionSur = EmailUtils
        .getEmailConfiguration(orgSPRegionSur);
    assertTrue(mailConfigSpRegionSur.getOrganization().equals(orgSPRegionSur));

    // Check if email configurations no exist and return start organization config
    log.info("Checking default configurations org star when organization : "
        + orgSPRegionSur.getName() + " no exist in email configuration");
    final EmailServerConfiguration mailConfigDefault = EmailUtils.getEmailConfiguration(orgItGroup);
    assertTrue(mailConfigDefault.getOrganization().equals(orgStar));

    // Clean data test inserted
    deleteEmailConfigurationTestByOrganization(listEmailConfigs);

    // Check data is clean
    OBCriteria<EmailServerConfiguration> critEmailConfigs = OBDal.getInstance()
        .createCriteria(EmailServerConfiguration.class);
    assertTrue(critEmailConfigs.list().isEmpty());
    log.info("Clean Populated tab Email Configuration result :" + critEmailConfigs.list().size()
        + " rows exist");
  }

  private List<EmailServerConfiguration> insertEmailConfigurationTestByOrganization(
      List<Organization> orgList) {
    List<EmailServerConfiguration> listEmailConfigs = new LinkedList<>();
    for (Organization org : orgList) {
      EmailServerConfiguration config = OBProvider.getInstance()
          .get(EmailServerConfiguration.class);
      config.setOrganization(org);
      config.setSmtpServer("smtp." + org.getName().replace(" ", "").trim() + ".test");
      listEmailConfigs.add(config);
      OBDal.getInstance().save(config);
    }
    OBDal.getInstance().flush();
    return listEmailConfigs;
  }

  private void deleteEmailConfigurationTestByOrganization(
      List<EmailServerConfiguration> listEmailConfigs) {
    for (EmailServerConfiguration config : listEmailConfigs) {
      OBDal.getInstance().remove(config);
    }
    OBDal.getInstance().flush();
  }
}
