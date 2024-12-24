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
 * All portions are Copyright (C) 2013-2023 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.base.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Used by the {@link ModelProvider ModelProvider}, maps the AD_Ref_Table table in the application
 * dictionary.
 * 
 * @author AugustoMauch
 */
@Entity
@javax.persistence.Table(name = "ad_ref_tree")
public class RefTree extends ModelObject {

  private Table table;
  private Column column;
  private String referenceId;

  @Override
  @Id
  @javax.persistence.Column(name = "ad_ref_tree_id")
  @GeneratedValue(generator = "DalUUIDGenerator")
  public String getId() {
    return super.getId();
  }

  @ManyToOne
  @JoinColumn(name = "ad_column_id", nullable = false)
  public Column getColumn() {
    return column;
  }

  public void setColumn(Column column) {
    this.column = column;
  }

  @javax.persistence.Column(name = "ad_reference_id", nullable = false)
  public String getReferenceId() {
    return referenceId;
  }

  public void setReferenceId(String referenceId) {
    this.referenceId = referenceId;
  }

  @ManyToOne
  @JoinColumn(name = "ad_table_id", nullable = false)
  public Table getTable() {
    return table;
  }

  public void setTable(Table table) {
    this.table = table;
  }
}
