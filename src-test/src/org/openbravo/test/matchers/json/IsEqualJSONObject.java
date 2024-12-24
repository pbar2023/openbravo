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
package org.openbravo.test.matchers.json;

import java.util.HashSet;
import java.util.Set;

import org.codehaus.jettison.json.JSONObject;
import org.hamcrest.Description;

/**
 * A matcher to check whether two JSONObjects are equal. Two JSONObjects will be considered equal if
 * both have exactly the same number of properties with the same value for each of them.
 */
class IsEqualJSONObject extends JSONMatcher<JSONObject> {

  private JSONObject expected;
  private Set<String> differentProperties;

  IsEqualJSONObject(JSONObject expected) {
    this.expected = expected;
    differentProperties = new HashSet<>();
  }

  @Override
  protected boolean matchesSafely(JSONObject item) {
    differentProperties = getDifferentProperties(item, expected);
    return differentProperties.isEmpty();
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("equal to ").appendValue(expected);
  }

  @Override
  public void describeMismatchSafely(JSONObject item, Description description) {
    addDifferencesToDescription(differentProperties, item, expected, description);
    description.appendText(", actual is ").appendValue(item);
  }
}
