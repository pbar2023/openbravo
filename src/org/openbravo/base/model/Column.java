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

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.model.domaintype.ButtonDomainType;
import org.openbravo.base.model.domaintype.DomainType;
import org.openbravo.base.model.domaintype.EnumerateDomainType;
import org.openbravo.base.model.domaintype.ForeignKeyDomainType;
import org.openbravo.base.model.domaintype.PrimitiveDomainType;
import org.openbravo.base.model.domaintype.StringDomainType;
import org.openbravo.base.model.domaintype.StringEnumerateDomainType;
import org.openbravo.base.session.BooleanYNConverter;

/**
 * Used by the {@link ModelProvider ModelProvider}, maps the AD_Column table in the application
 * dictionary.
 * 
 * @author iperdomo
 * @author mtaal
 */
@Entity
@Table(name = "ad_column")
public class Column extends ModelObject {
  @Transient
  private static final Logger log = LogManager.getLogger();

  private Property property;
  private String columnName;
  private Module module;
  private org.openbravo.base.model.Table table;
  private Reference reference;
  private Reference referenceValue;
  private Column referenceType = null;
  private int fieldLength;
  private String defaultValue;
  private boolean key;
  private boolean secondaryKey;
  private boolean storedInSession;
  private boolean parent;
  private boolean mandatory;
  private boolean encrypted;
  private boolean decryptable;
  private boolean updatable;
  private boolean identifier;
  private String valueMin;
  private String valueMax;
  private String sqlLogic;
  private Boolean isTransient;
  private String isTransientCondition;
  private Integer position;
  private boolean translatable;
  private Integer seqno;
  private boolean usedSequence;
  private boolean isAllowedCrossOrgReference;
  private boolean childPropertyInParent;

  @Id
  @javax.persistence.Column(name = "ad_column_id")
  @GeneratedValue(generator = "DalUUIDGenerator")
  @Override
  public String getId() {
    return super.getId();
  }

  @javax.persistence.Column(name = "name", nullable = false)
  @Override
  public String getName() {
    return super.getName();
  }

  @Transient
  public DomainType getDomainType() {
    if (referenceValue != null) {
      final DomainType refValueDomainType = referenceValue.getDomainType();
      // always return a string or enumerate domain type for a button
      // this is to handle particular cases where the main reference
      // is a button and the sub reference is a list, there are one or
      // two exceptions on this also (other main reference)
      // but the common case is that the subreference is a list
      if (reference.getDomainType() instanceof ButtonDomainType
          && !(refValueDomainType instanceof EnumerateDomainType
              || refValueDomainType instanceof StringDomainType)) {
        final StringDomainType stringDomainType = new StringDomainType();
        stringDomainType.setModelProvider(refValueDomainType.getModelProvider());
        stringDomainType.setReference(referenceValue);
        return stringDomainType;
      } else {
        return refValueDomainType;
      }
    }

    return reference.getDomainType();
  }

  @Transient
  public boolean isBoolean() {
    return isPrimitiveType() && (getPrimitiveType().getName().compareTo("boolean") == 0
        || Boolean.class == getPrimitiveType());
  }

  @javax.persistence.Column(name = "columnName")
  public String getColumnName() {
    return columnName;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  @ManyToOne
  @JoinColumn(name = "ad_table_id", nullable = false)
  public org.openbravo.base.model.Table getTable() {
    return table;
  }

  public void setTable(org.openbravo.base.model.Table table) {
    this.table = table;
  }

  @ManyToOne
  @JoinColumn(name = "ad_reference_id", nullable = false)
  public Reference getReference() {
    return reference;
  }

  public void setReference(Reference reference) {
    this.reference = reference;
  }

  @ManyToOne
  @JoinColumn(name = "ad_reference_value_id")
  public Reference getReferenceValue() {
    return referenceValue;
  }

  public void setReferenceValue(Reference referenceValue) {
    this.referenceValue = referenceValue;
  }

  @javax.persistence.Column(name = "fieldLength")
  public int getFieldLength() {
    return fieldLength;
  }

  public void setFieldLength(int fieldLength) {
    this.fieldLength = fieldLength;
  }

  @javax.persistence.Column(name = "defaultValue")
  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  @Convert(converter = BooleanYNConverter.class)
  @javax.persistence.Column(name = "iskey")
  public boolean isKey() {
    return key;
  }

  public void setKey(Boolean key) {
    this.key = key;
  }

  @Convert(converter = BooleanYNConverter.class)
  @javax.persistence.Column(name = "issecondarykey")
  public boolean isSecondaryKey() {
    return secondaryKey;
  }

  public void setSecondaryKey(boolean secondaryKey) {
    this.secondaryKey = secondaryKey;
  }

  @Convert(converter = BooleanYNConverter.class)
  @javax.persistence.Column(name = "isparent")
  public boolean isParent() {
    return parent;
  }

  public void setParent(Boolean parent) {
    this.parent = parent;
  }

  @Convert(converter = BooleanYNConverter.class)
  @javax.persistence.Column(name = "ismandatory")
  public boolean isMandatory() {
    return mandatory;
  }

  public void setMandatory(Boolean mandatory) {
    this.mandatory = mandatory;
  }

  @Convert(converter = BooleanYNConverter.class)
  @javax.persistence.Column(name = "isupdateable")
  public boolean isUpdatable() {
    return updatable;
  }

  public void setUpdatable(Boolean updatable) {
    this.updatable = updatable;
  }

  @Convert(converter = BooleanYNConverter.class)
  @javax.persistence.Column(name = "isidentifier")
  public boolean isIdentifier() {
    return identifier;
  }

  public void setIdentifier(Boolean identifier) {
    this.identifier = identifier;
  }

  @javax.persistence.Column(name = "valueMin")
  public String getValueMin() {
    return valueMin;
  }

  public void setValueMin(String valueMin) {
    this.valueMin = valueMin;
  }

  @javax.persistence.Column(name = "valueMax")
  public String getValueMax() {
    return valueMax;
  }

  public void setValueMax(String valueMax) {
    this.valueMax = valueMax;
  }

  @Transient
  public boolean isPrimitiveType() {
    // anything else than foreign key is a primitive
    return getDomainType() instanceof PrimitiveDomainType;
  }

  @Transient
  public Class<?> getPrimitiveType() {
    if (isPrimitiveType()) {
      return ((PrimitiveDomainType) getDomainType()).getPrimitiveType();
      // final Class<?> clz = ((PrimitiveDomainType) getDomainType()).getPrimitiveType();
      // if (clz == Boolean.class && getReferenceValue() != null) {
      // // a string list
      // return String.class;
      // }
      // return clz;
    }
    return null;
  }

  @Transient
  public Column getReferenceType() {
    if (!isPrimitiveType()) {
      return referenceType;
    }
    return null;
  }

  public void setReferenceType(Column column) {
    this.referenceType = column;
  }

  @Convert(converter = BooleanYNConverter.class)
  @javax.persistence.Column(name = "isactive", nullable = false)
  @Override
  public boolean isActive() {
    if (super.isActive() && !isPrimitiveType()) {
      final Column thatColumn = getReferenceType();

      // note calls isSuperActive(), if it would call isActive there is a danger
      // for infinite looping, see issue:
      // https://issues.openbravo.com/view.php?id=8632
      if (thatColumn != null
          && (!thatColumn.isSuperActive() || !thatColumn.getTable().isActive())) {
        log.error(
            "Column " + this + " refers to a non active table or column or to a view" + thatColumn);
      }
    }
    return super.isActive();
  }

  // method to prevent infinite looping checking for exceptions. See this issue:
  // https://issues.openbravo.com/view.php?id=8632
  @Transient
  private boolean isSuperActive() {
    return super.isActive();
  }

  /**
   * Deprecated use {@link Column#setReferenceType()}
   */
  protected void setReferenceType(ModelProvider modelProvider) {
    setReferenceType();
  }

  protected void setReferenceType() {

    // reference type does not need to be set
    if (isPrimitiveType()) {
      return;
    }

    try {
      setReferenceType(
          ((ForeignKeyDomainType) getDomainType()).getForeignKeyColumn(getColumnName()));
    } catch (final Exception e) {
      log.error("No referenced column found: error >> tableName: " + table.getTableName()
          + " - columnName: " + getColumnName(), e);
    }
  }

  // returns the primitive type name or the class of the
  // referenced type
  @Transient
  public String getTypeName() {
    final String typeName;
    if (isPrimitiveType()) {
      typeName = getPrimitiveType().getName();
    } else if (getReferenceType() == null) {
      log.warn("ERROR NO REFERENCETYPE " + getTable().getName() + "." + getColumnName());
      return "java.lang.Object";
    } else {
      typeName = getReferenceType().getTable().getNotNullClassName();
    }
    return typeName;
  }

  // the last part of the class name
  @Transient
  public String getSimpleTypeName() {
    final String typeName = getTypeName();
    if (typeName.indexOf(".") == -1) {
      return typeName;
    }
    return typeName.substring(1 + typeName.lastIndexOf("."));
  }

  /**
   * Returns the classname of the object which maps to the type of this column. For example if this
   * column is an int then this method will return java.lang.Integer (the object version of the
   * int).
   * 
   * @return the name of the class of the type of this column
   */
  @Transient
  public String getObjectTypeName() {
    if (isPrimitiveType()) {
      final String typeName = getTypeName();
      if (typeName.indexOf('.') != -1) {
        return typeName;
      }
      if ("boolean".equals(typeName)) {
        return Boolean.class.getName();
      }
      if ("int".equals(typeName)) {
        return Integer.class.getName();
      }
      if ("long".equals(typeName)) {
        return Long.class.getName();
      }
      if ("byte".equals(typeName)) {
        return Byte.class.getName();
      }
      if ("float".equals(typeName)) {
        return Float.class.getName();
      }
      if ("double".equals(typeName)) {
        return Double.class.getName();
      }
      // TODO: maybe throw an exception
      return typeName;
    } else {
      return getTypeName();
    }
  }

  @Transient
  public Property getProperty() {
    return property;
  }

  public void setProperty(Property property) {
    this.property = property;
  }

  /**
   * Returns the concatenation of the table and column name.
   */
  @Override
  public String toString() {
    return getTable() + "." + getColumnName();
  }

  /**
   * Is used when this column denotes an enum. This method returns all allowed String values.
   * 
   * @return the set of allowed values for this Column.
   */
  @Transient
  @SuppressWarnings("unchecked")
  public Set<String> getAllowedValues() {
    // TODO: discrepancy with the application dictionary, solve this later
    if (getColumnName().equalsIgnoreCase("changeprojectstatus")) {
      return Collections.EMPTY_SET;
    }
    if (getDomainType() instanceof StringEnumerateDomainType) {
      return ((StringEnumerateDomainType) getDomainType()).getEnumerateValues();
    }
    return Collections.EMPTY_SET;
  }

  @Convert(converter = BooleanYNConverter.class)
  @javax.persistence.Column(name = "istransient")
  public Boolean isTransient() {
    return isTransient;
  }

  public void setTransient(Boolean isTransient) {
    this.isTransient = isTransient;
  }

  @javax.persistence.Column(name = "istransientcondition")
  public String getIsTransientCondition() {
    return isTransientCondition;
  }

  public void setIsTransientCondition(String isTransientCondition) {
    this.isTransientCondition = isTransientCondition;
  }

  @javax.persistence.Column(name = "position")
  public Integer getPosition() {
    return position;
  }

  public void setPosition(Integer position) {
    this.position = position;
  }

  @ManyToOne
  @JoinColumn(name = "ad_module_id", nullable = false)
  public Module getModule() {
    return module;
  }

  public void setModule(Module module) {
    this.module = module;
  }

  @Convert(converter = BooleanYNConverter.class)
  @javax.persistence.Column(name = "isencrypted")
  public boolean isEncrypted() {
    return encrypted;
  }

  public void setEncrypted(boolean encrypted) {
    this.encrypted = encrypted;
  }

  @Convert(converter = BooleanYNConverter.class)
  @javax.persistence.Column(name = "isdesencryptable")
  public boolean isDecryptable() {
    return decryptable;
  }

  public void setDecryptable(boolean decryptable) {
    this.decryptable = decryptable;
  }

  @Convert(converter = BooleanYNConverter.class)
  @javax.persistence.Column(name = "istranslated")
  public boolean isTranslatable() {
    return translatable;
  }

  public void setTranslatable(boolean translatable) {
    this.translatable = translatable;
  }

  @Convert(converter = BooleanYNConverter.class)
  @javax.persistence.Column(name = "issessionattr")
  public boolean isStoredInSession() {
    return storedInSession;
  }

  public void setStoredInSession(boolean storedInSession) {
    this.storedInSession = storedInSession;
  }

  @javax.persistence.Column(name = "seqno")
  public Integer getSeqno() {
    return seqno;
  }

  public void setSeqno(Integer seqno) {
    this.seqno = seqno;
  }

  @Convert(converter = BooleanYNConverter.class)
  @javax.persistence.Column(name = "isusedsequence")
  public boolean isUsedSequence() {
    return usedSequence;
  }

  public void setUsedSequence(boolean usedSequence) {
    this.usedSequence = usedSequence;
  }

  @javax.persistence.Column(name = "sqlLogic")
  public String getSqlLogic() {
    return sqlLogic;
  }

  public void setSqlLogic(String sqlLogic) {
    this.sqlLogic = sqlLogic;
  }

  /**
   * Defines whether the {@link Property} generated for this column will allow cross organization
   * references.
   * 
   * @see Property#isAllowedCrossOrgReference()
   */
  @Convert(converter = BooleanYNConverter.class)
  @javax.persistence.Column(name = "allowed_cross_org_link")
  public boolean isAllowedCrossOrgReference() {
    return isAllowedCrossOrgReference;
  }

  /** @see Property#isAllowedCrossOrgReference() */
  public void setAllowedCrossOrgReference(boolean allowedCrossOrgReference) {
    this.isAllowedCrossOrgReference = allowedCrossOrgReference;
  }

  @Convert(converter = BooleanYNConverter.class)
  @javax.persistence.Column(name = "is_child_property_in_parent")
  public boolean isChildPropertyInParent() {
    return childPropertyInParent;
  }

  public void setChildPropertyInParent(boolean isChildPropertyInParent) {
    this.childPropertyInParent = isChildPropertyInParent;
  }

  @javax.persistence.Column(name = "updated")
  @Override
  public Date getUpdated() {
    return super.getUpdated();
  }

}
