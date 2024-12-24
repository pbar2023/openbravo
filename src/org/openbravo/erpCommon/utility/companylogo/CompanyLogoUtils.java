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
 * All portions are Copyright (C) 2022-2024 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.utility.companylogo;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.system.ClientInformation;
import org.openbravo.model.ad.system.SystemInformation;
import org.openbravo.model.ad.utility.Image;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationInformation;

/**
 * Utility functions used to retrieve the proper company logo image All of them looks first for the
 * given Organization, if no matching image was found, we try looking at ClientInfo and finally at
 * SystemInfo
 */
public class CompanyLogoUtils {
  public static Image getCompanyLogo(Organization org) {
    return getCompanyLogoByProperties(org, "companyLogo", "companyLogoDark", false);
  }

  public static Image getCompanyLogoForClient(Client client) {
    return getCompanyLogoByProperties(client, "companyLogo", "companyLogoDark", false);
  }

  public static Image getCompanyLogoDarkMode(Organization org) {
    return getCompanyLogoByProperties(org, "companyLogo", "companyLogoDark", true);
  }

  public static Image getCompanyLogoSubmark(Organization org) {
    return getCompanyLogoByProperties(org, "companyLogoSubmark", "companyLogoSubmarkDark", false);
  }

  public static Image getCompanyLogoSubmarkDarkMode(Organization org) {
    return getCompanyLogoByProperties(org, "companyLogoSubmark", "companyLogoSubmarkDark", true);
  }

  public static Image getCompanyLogoForDocuments(Organization org) {
    return getCompanyLogoByProperties(org, "companyLogoForDocs", null, false);
  }

  public static Image getCompanyLogoForReceipts(Organization org) {
    return getCompanyLogoByProperties(org, "companyLogoForReceipts", null, false);
  }

  public static Image getAdditionalDocumentImage(Organization org) {
    return getCompanyLogoByOrg(org, "additionalDocumentImage", null, false);
  }

  private static Image getCompanyLogoByOrg(Organization org, String propertyLight,
      String propertyDark, boolean isDarkMode) {
    Image img = null;
    // first look for the logo in org
    if (org != null) {
      OrganizationInformation orgInfo = org.getOrganizationInformationList().get(0);
      img = getLogoImageFromEntity(orgInfo, propertyLight, propertyDark, isDarkMode);
    }
    // Does not try in Client info from the current client
    // or in SystemInfo if no logo is found for this organization
    return img;
  }

  private static Image getCompanyLogoByProperties(Organization org, String propertyLight,
      String propertyDark, boolean isDarkMode) {
    Image img = null;
    // first look for the logo in org
    if (org != null) {
      OrganizationInformation orgInfo = org.getOrganizationInformationList().get(0);
      img = getLogoImageFromEntity(orgInfo, propertyLight, propertyDark, isDarkMode);
    }
    // then try in Client info from the current client
    // or in SystemInfo if no logo is found for this client
    if (img == null) {
      Client client = OBContext.getOBContext().getCurrentClient();
      if (org != null) {
        client = org.getClient();
      }
      img = getCompanyLogoByProperties(client, propertyLight, propertyDark, isDarkMode);
    }

    // If everything fails, return an empty image
    return img;
  }

  private static Image getCompanyLogoByProperties(Client client, String propertyLight,
      String propertyDark, boolean isDarkMode) {
    Image img = null;
    // try in Client info
    ClientInformation clientInfo = OBDal.getReadOnlyInstance()
        .get(ClientInformation.class, client.getId());
    img = getLogoImageFromEntity(clientInfo, propertyLight, propertyDark, isDarkMode);

    // Finally try the system info
    if (img == null) {
      SystemInformation systemInfo = OBDal.getReadOnlyInstance().get(SystemInformation.class, "0");
      img = getLogoImageFromEntity(systemInfo, propertyLight, propertyDark, isDarkMode);
    }
    // If everything fails, return an empty image
    return img;
  }

  private static Image getLogoImageFromEntity(BaseOBObject entity, String propertyLight,
      String propertyDark, boolean isDarkMode) {
    if (StringUtils.isNotEmpty(propertyDark)) {
      Image img = null;
      if (isDarkMode) {
        img = (Image) entity.get(propertyDark);
        if (img == null) {
          img = (Image) entity.get(propertyLight);
        }
      } else {
        img = (Image) entity.get(propertyLight);
        if (img == null) {
          img = (Image) entity.get(propertyDark);
        }
      }

      return img;
    }

    return (Image) entity.get(propertyLight);
  }
}
