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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;

import javax.persistence.Column;
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
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.domaintype.DomainType;
import org.openbravo.base.model.domaintype.PrimitiveDomainType;
import org.openbravo.base.model.domaintype.StringDomainType;
import org.openbravo.base.session.BooleanYNConverter;
import org.openbravo.base.util.OBClassLoader;

/**
 * Used by the {@link ModelProvider ModelProvider}, maps the AD_Reference table in the in-memory
 * model.
 * 
 * @author iperdomo
 */
@Entity
@Table(name = "ad_reference")
public class Reference extends ModelObject {
  @Transient
  private static final Logger log = LogManager.getLogger();

  // Ids of ReferenceTypes
  @Transient
  public static final String TABLE = "18";
  @Transient
  public static final String TABLEDIR = "19";
  @Transient
  public static final String SEARCH = "30";
  @Transient
  public static final String IMAGE = "32";
  @Transient
  public static final String IMAGE_BLOB = "4AA6C3BE9D3B4D84A3B80489505A23E5";
  @Transient
  public static final String RESOURCE_ASSIGNMENT = "33";
  @Transient
  public static final String PRODUCT_ATTRIBUTE = "35";
  @Transient
  public static final String NO_REFERENCE = "-1";

  @Transient
  private static HashMap<String, Class<?>> primitiveTypes;

  static {
    // Mapping reference id with a Java type
    primitiveTypes = new HashMap<String, Class<?>>();

    primitiveTypes.put("10", String.class);
    primitiveTypes.put("11", Long.class);
    primitiveTypes.put("12", BigDecimal.class);
    primitiveTypes.put("13", String.class);
    primitiveTypes.put("14", String.class);
    primitiveTypes.put("15", Date.class);
    primitiveTypes.put("16", Date.class);
    primitiveTypes.put("17", String.class);
    primitiveTypes.put("20", Boolean.class);
    primitiveTypes.put("22", BigDecimal.class);
    primitiveTypes.put("23", byte[].class); // Binary/Blob Data
    primitiveTypes.put("24", Timestamp.class);
    primitiveTypes.put("26", Object.class); // RowID is not used
    primitiveTypes.put("27", Object.class); // Color is not used
    primitiveTypes.put("28", Boolean.class);
    primitiveTypes.put("29", BigDecimal.class);
    primitiveTypes.put("34", String.class);
    primitiveTypes.put("800008", BigDecimal.class);
    primitiveTypes.put("800019", BigDecimal.class);
    primitiveTypes.put("800101", String.class);
  }

  private String modelImpl;
  private DomainType domainType;
  private Reference parentReference;
  private boolean baseReference;

  @Id
  @Column(name = "ad_reference_id")
  @GeneratedValue(generator = "DalUUIDGenerator")
  @Override
  public String getId() {
    return super.getId();
  }

  @Convert(converter = BooleanYNConverter.class)
  @Column(name = "isactive", nullable = false)
  @Override
  public boolean isActive() {
    return super.isActive();
  }

  @Column(name = "name", nullable = false)
  @Override
  public String getName() {
    return super.getName();
  }

  @Column(name = "model_impl")
  public String getModelImpl() {
    return modelImpl;
  }

  public void setModelImpl(String modelImpl) {
    this.modelImpl = modelImpl;
  }

  @ManyToOne
  @JoinColumn(name = "parentreference_id")
  public Reference getParentReference() {
    return parentReference;
  }

  public void setParentReference(Reference parentReference) {
    this.parentReference = parentReference;
  }

  @Convert(converter = BooleanYNConverter.class)
  @Column(name = "isbasereference", nullable = false)
  public boolean isBaseReference() {
    return baseReference;
  }

  public void setBaseReference(boolean baseReference) {
    this.baseReference = baseReference;
  }

  @Transient
  public boolean isPrimitive() {
    return getDomainType() instanceof PrimitiveDomainType;
  }

  @Column(name = "updated", nullable = false)
  @Override
  public Date getUpdated() {
    return super.getUpdated();
  }

  @Transient
  public DomainType getDomainType() {
    if (domainType != null) {
      return domainType;
    }
    String modelImplementationClass = getModelImplementationClassName();
    if (modelImplementationClass == null) {
      log.error(
          "Reference " + this + " has a modelImpl which is null, using String as the default");
      modelImplementationClass = StringDomainType.class.getName();
    }
    try {
      final Class<?> clz = OBClassLoader.getInstance().loadClass(modelImplementationClass);
      domainType = (DomainType) clz.getDeclaredConstructor().newInstance();
      domainType.setReference(this);
    } catch (Exception e) {
      throw new OBException(
          "Not able to create domain type " + getModelImpl() + " for reference " + this, e);
    }
    return domainType;
  }

  /**
   * Also calls the parent reference ({@link #getParentReference()}) to find the modelImpl (
   * {@link #getModelImpl()}).
   * 
   * @return the modelImpl or if not set, the value set in the parent.
   */
  @Transient
  public String getModelImplementationClassName() {
    // only call the parent if the parent is a base reference and this is not a basereference
    if (getModelImpl() == null && !isBaseReference() && getParentReference() != null
        && getParentReference().isBaseReference()) {
      return getParentReference().getModelImplementationClassName();
    }
    return getModelImpl();
  }
}
