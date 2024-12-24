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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.openbravo.base.session.BooleanYNConverter;

/**
 * Models the ad_package table. Is used for generating correct java classnames.
 * 
 * @author mtaal
 */
@Entity
@Table(name = "ad_package")
public class Package extends ModelObject {
  private String javaPackage;
  private Module module;

  @Id
  @Column(name = "ad_package_id")
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

  @Column(name = "javapackage")
  public String getJavaPackage() {
    return javaPackage;
  }

  public void setJavaPackage(String javaPackage) {
    this.javaPackage = javaPackage;
  }

  @ManyToOne
  @JoinColumn(name = "ad_module_id", nullable = false)
  public Module getModule() {
    return module;
  }

  public void setModule(Module module) {
    this.module = module;
  }

  @Column(name = "updated")
  @Override
  public Date getUpdated() {
    return super.getUpdated();
  }
}
