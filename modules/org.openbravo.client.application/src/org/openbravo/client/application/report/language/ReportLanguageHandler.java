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

package org.openbravo.client.application.report.language;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import javax.enterprise.util.AnnotationLiteral;

import org.openbravo.base.Prioritizable;
import org.openbravo.base.exception.OBException;
import org.openbravo.erpCommon.utility.reporting.DocumentType;

/**
 * Provides a way for specific reports to overwrite this behavior making possible to select a
 * different language based on some ad-hoc logic
 * 
 * Classes that implement this interface should declare the {@ReportLanguageQualifier} qualifier to
 * define the file extension that they can handle
 */
public interface ReportLanguageHandler extends Prioritizable {

  /**
   * Returns the language value based on an ad-hoc logic.
   * 
   * @param parameters
   *          Report parameters with the required information to to get the language based on the
   *          implemented logic
   * 
   * @return Language to use in the desired report
   */
  public String getLanguage(Map<?, ?> parameters) throws OBException;

  /**
   * Defines the qualifier used to provide a report's language
   * 
   * The Qualifier will be composed of two fields: Type and Value.
   */
  @javax.inject.Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE })
  @Inherited
  @Repeatable(Qualifiers.class)
  public @interface Qualifier {

    /**
     * Returns the qualifier type associated to the report.Having the value of one element of a new
     * enum that will have the 4 values: (PROCESS_DEFINITION, AD_PROCESS, DOCUMENT, MANUAL)
     * 
     * @return type
     */
    ReportType type();

    /**
     * Returns the qualifier value associated to the report. Depending on the type it can be: For
     * process definition and ad_process, the ID of the report being invoked. For Document reports
     * it will be the document type as defined in {@link DocumentType}.
     * 
     * @return value
     */
    String value();
  }

  /** Containing annotation type to support repeating {@link Qualifier}. */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE })
  @Inherited
  public @interface Qualifiers {
    Qualifier[] value();
  }

  /**
   * A class used to select the correct language implementation to overwrite the specific report
   * language
   */
  @SuppressWarnings("all")
  public static class Selector extends AnnotationLiteral<Qualifier> implements Qualifier {
    private static final long serialVersionUID = 1L;

    final ReportType type;
    final String value;

    public Selector(ReportType type, String value) {
      this.type = type;
      this.value = value;
    }

    @Override
    public ReportType type() {
      return type;
    }

    @Override
    public String value() {
      return value;
    }

  }

  /**
   * Enum to define the report types supported (PROCESS_DEFINITION, AD_PROCESS, DOCUMENT, MANUAL)
   */
  public enum ReportType {
    PROCESS_DEFINITION("PD"), AD_PROCESS("AP"), DOCUMENT("D"), MANUAL("M");

    private String code;

    private ReportType(final String code) {
      this.code = code;
    }

    @Override
    public String toString() {
      return code;
    }
  }

}
