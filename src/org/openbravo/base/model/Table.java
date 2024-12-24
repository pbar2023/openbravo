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
 * All portions are Copyright (C) 2008-2023 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.base.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Convert;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.session.BooleanYNConverter;

/**
 * Used by the {@link ModelProvider ModelProvider}, maps the AD_Table table in the application
 * dictionary. The {@link Entity Entity} is initialized from a Table.
 * 
 * @author iperdomo
 */
@javax.persistence.Table(name = "ad_table")
@javax.persistence.Entity
public class Table extends ModelObject {
  @Transient
  private static final Logger log = LogManager.getLogger();

  private Entity entity;
  private String dataOrigin;
  private String tableName;
  private boolean view;
  private boolean isDeletable;
  private List<Column> columns = new ArrayList<Column>();
  private List<Column> primaryKeyColumns = null;
  private List<Column> identifierColumns = null;
  private List<Column> parentColumns = null;
  private String className = null;
  private String accessLevel;
  private Package thePackage;
  private String treeType;

  @Override
  @Id
  @javax.persistence.Column(name = "ad_table_id")
  @GeneratedValue(generator = "DalUUIDGenerator")
  public String getId() {
    return super.getId();
  }

  @Override
  @javax.persistence.Column(name = "name", nullable = false)
  public String getName() {
    return super.getName();
  }

  @Override
  @javax.persistence.Column(name = "isactive", nullable = false)
  @Convert(converter = BooleanYNConverter.class)
  public boolean isActive() {
    return super.isActive();
  }

  @Override
  @javax.persistence.Column(name = "updated")
  public Date getUpdated() {
    return super.getUpdated();
  }

  @javax.persistence.Column(name = "treetype")
  public String getTreeType() {
    return treeType;
  }

  public void setTreeType(String treeType) {
    this.treeType = treeType;
  }

  @javax.persistence.Column(name = "dataorigintype", nullable = false)
  public String getDataOrigin() {
    return dataOrigin;
  }

  public void setDataOrigin(String dataOrigin) {
    this.dataOrigin = dataOrigin;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  /**
   * Note the columns are not set by hibernate or through a hibernate mapping. For performance
   * reasons they are set explicitly in the {@link ModelProvider}. See the assignColumnsToTable
   * method in that class.
   * 
   * This collection is only set and used within the {@link ModelProvider} initialize method. It
   * should not be used in other places. In other cases perform a direct database query to get the
   * columns of a table.
   * 
   * @return the list of Column instances of this table
   */
  @Transient
  public List<Column> getColumns() {
    return columns;
  }

  /**
   * Note the columns are not set by hibernate or through a hibernate mapping. For performance
   * reasons they are set explicitly in the {@link ModelProvider}. See the assignColumnsToTable
   * method in that class.
   */
  public void setColumns(List<Column> columns) {
    this.columns = columns;
  }

  @Transient
  public List<Column> getPrimaryKeyColumns() {
    if (primaryKeyColumns == null) {
      primaryKeyColumns = new ArrayList<Column>();

      for (final Column c : getColumns()) {
        if (c.isKey()) {
          primaryKeyColumns.add(c);
        }
      }
    }
    return primaryKeyColumns;
  }

  public void setPrimaryKeyColumns(List<Column> primaryKeyColumns) {
    this.primaryKeyColumns = primaryKeyColumns;
  }

  @Transient
  public List<Column> getIdentifierColumns() {
    if (identifierColumns == null) {
      identifierColumns = new ArrayList<Column>();
      for (final Column c : getColumns()) {
        if (c.isIdentifier()) {
          identifierColumns.add(c);
        }
      }
    }
    return identifierColumns;
  }

  public void setParentColumns(List<Column> parentColums) {
    this.parentColumns = parentColums;
  }

  @Transient
  public List<Column> getParentColumns() {
    if (parentColumns == null) {
      parentColumns = new ArrayList<Column>();
      for (final Column c : getColumns()) {
        if (c.isParent()) {
          parentColumns.add(c);
        }
      }
    }
    return parentColumns;
  }

  public void setIdentifierColumns(List<Column> identifierColumns) {
    this.identifierColumns = identifierColumns;
  }

  public void setView(boolean view) {
    this.view = view;
  }

  @javax.persistence.Column(name = "isview")
  @Convert(converter = BooleanYNConverter.class)
  public boolean isView() {
    return view;
  }

  @Transient
  public String getNotNullClassName() {
    if (getClassName() == null || getClassName().trim().length() == 0) {
      return getName();
    }
    return getClassName();
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public void setReferenceTypes(ModelProvider modelProvider) {
    for (final Column c : columns) {
      if (!c.isPrimitiveType()) {
        c.setReferenceType();
      }
    }
  }

  @Transient
  public String getPackageName() {
    if (getThePackage() != null) {
      return getThePackage().getJavaPackage();
    }
    log.error("Can not determine package name, no package defined for table " + getName());
    // ugly but effective
    return "no.package.defined.for.table." + getName();
  }

  @Transient
  public Entity getEntity() {
    return entity;
  }

  public void setEntity(Entity entity) {
    this.entity = entity;
  }

  @Override
  public String toString() {
    return getTableName();
  }

  @javax.persistence.Column(name = "isdeleteable")
  @Convert(converter = BooleanYNConverter.class)
  public boolean isDeletable() {
    return isDeletable;
  }

  public void setDeletable(boolean isDeletable) {
    this.isDeletable = isDeletable;
  }

  @javax.persistence.Column(name = "accesslevel")
  public String getAccessLevel() {
    return accessLevel;
  }

  public void setAccessLevel(String accessLevel) {
    this.accessLevel = accessLevel;
  }

  @ManyToOne
  @JoinColumn(name = "ad_package_id", nullable = false)
  public Package getThePackage() {
    return thePackage;
  }

  public void setThePackage(Package thePackage) {
    this.thePackage = thePackage;
  }
}
