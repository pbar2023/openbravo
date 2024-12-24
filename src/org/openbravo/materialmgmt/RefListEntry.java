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

import org.openbravo.base.model.NamingUtil;
import org.openbravo.model.ad.domain.List;

/**
 * Used by the {@link RelevantCharacteristicProperty} to keep the information of the entries of the
 * relevant characteristic list reference
 */
class RefListEntry {
  private String id;
  private String searchKey;
  private String name;
  private String description;
  private String propertyName;

  RefListEntry(List refListEntry) {
    this.id = refListEntry.getId();
    this.searchKey = refListEntry.getSearchKey();
    this.name = refListEntry.getName();
    this.description = refListEntry.getDescription();
    propertyName = NamingUtil.formatAsPropertyName(refListEntry.getSearchKey());
  }

  /**
   * @return the entry ID
   */
  String getId() {
    return id;
  }

  /**
   * @return the entry search key
   */
  String getSearchKey() {
    return searchKey;
  }

  /**
   * @return the entry name
   */
  String getName() {
    return name;
  }

  /**
   * @return the entry description
   */
  String getDescription() {
    return description;
  }

  /**
   * @return the result of formatting the entry search key as a property name
   */
  String getPropertyName() {
    return propertyName;
  }
}
