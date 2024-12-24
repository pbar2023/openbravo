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
package org.openbravo.client.application.window;

import org.openbravo.base.Prioritizable;
import org.openbravo.client.kernel.reference.UIDefinition;
import org.openbravo.model.ad.ui.Field;

/**
 * Allows to provide to the {@link OBViewFieldHandler} the settings of the fields that are not
 * backed by a column and therefore cannot be retrieved from the application dictionary.
 */
public interface FieldSettingsProvider extends Prioritizable {

  /**
   * Determines if the {@code FieldSettingsProvider} is able to retrieve the field view properties
   * of the given field
   * 
   * @param field
   *          The AD field
   * 
   * @return {@code true} if the field view properties can be retrieved with this
   *         {@code FieldSettingsProvider} or {@false} in any other case
   */
  public boolean accepts(Field field);

  /**
   * Gets the {@link UIDefinition} for the given field
   * 
   * @param field
   *          The AD field
   * 
   * @return {@code true} if the field view properties can be retrieved with this
   *         {@code FieldSettingsProvider} or {@false} in any other case
   */
  public UIDefinition getUIDefinition(Field field);

  /**
   * Determines if the given field must be displayed in read only mode
   * 
   * @param field
   *          The AD field
   * 
   * @return {@code true} if the given field must be displayed in read only mode or {@false} in any
   *         other case
   */
  public boolean isReadOnly(Field field);
}
