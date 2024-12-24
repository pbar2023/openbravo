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

import java.util.Arrays;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hamcrest.Matcher;

/**
 * Provides different matchers for asserting JSONObjects and JSONArrays
 */
public class JSONMatchers {

  private JSONMatchers() {
  }

  /**
   * Creates a matcher for a JSONObject matching when the examined JSONObject has exactly the same
   * number of properties with the same values as the expected one. The order of the keys is not
   * taken into account. It accepts matcher properties.
   * 
   * For example assertThat(actual, equal(expected));:
   * 
   * - Passes if actual = {p1: "hello", p2: 2} and expected = {p1: "hello", p2: 2} <br>
   * - Passes if actual = {p1: "hello", p2: 2} and expected = {p1: "hello", p2: greaterThan(1)} <br>
   * - Does not pass if actual = {p1: "hello", p2: 2} and expected = {p1: "hello", p2: 1}
   * 
   * @param expected
   *          the JSONObject that is expected to be equal to the actual one
   * 
   * @return the equal matcher
   * 
   */
  public static Matcher<JSONObject> equal(JSONObject expected) {
    return new IsEqualJSONObject(expected);
  }

  /**
   * Creates a matcher for a JSONObject matching when the examined JSONObject contains the
   * properties with the same values of the expected one. The order of the keys is not taken into
   * account. It accepts matcher properties.
   * 
   * For example assertThat(actual, matchesObject(expected));:
   * 
   * - Passes if actual = {p1: "hello", p2: 2} and expected = {p1: "hello", p2: 2} <br>
   * - Passes if actual = {p1: "hello", p2: 2} and expected = {p1: "hello", p2: greaterThan(1)} <br>
   * - Passes if actual = {p1: "hello", p2: 2} and expected = {p1: "hello"} <br>
   * - Passes if actual = {p1: "hello", p2: 2} and expected = {p2: greaterThan(1)} <br>
   * - Does not pass if actual = {p1: "hello", p2: 2} and expected = {p1: "hello", p2: 1} <br>
   * - Does not pass if actual = {p1: "hello", p2: 2} and expected = {p1: "hello", p2: 2, p3: "bye"}
   * 
   * @param expected
   *          the JSON object that is expected to match all its properties with the actual one
   * 
   * @return the matchesObject matcher
   */
  public static Matcher<JSONObject> matchesObject(JSONObject expected) {
    return new IsMatchingJSONObject(expected);
  }

  /**
   * Creates a matcher for a JSONArray matching when the examined JSONArray contains all the
   * expected objects. The order of the objects is not taken into account.
   * 
   * For example assertThat(actual, hasItems(item1, item2));:
   * 
   * - Passes if actual = [{p1: "hello", p2: 2}, 1], item = {p1: "hello", p2: 2} and item2 = 1 <br>
   * - Passes if actual = [{p1: "hello", p2: 2}, 1], item = {p1: "hello", p2: greaterThan(1)} and
   * item2 = 1 <br>
   * - Does not pass if actual = [{p1: "hello", p2: 2}, 1], item = {p1: "hello", p2: 2} and item2 =
   * 3
   * 
   * @param expected
   *          the objects that are expected to be included in the actual JSONArray
   * 
   * @return the hasItems for objects matcher
   */
  public static Matcher<JSONArray> hasItems(Object... expected) {
    return new HasItems(Arrays.asList(expected));
  }

  /**
   * Creates a matcher for a JSONArray matching when the examined JSONArray matches with all the
   * expected matchers.
   *
   * For example assertThat(actual, hasItems(matcher1, matcher2));:
   * 
   * - Passes if actual = [{p1: "hello", p2: 2}, 1], matcher1 = matchesObject{p1: "hello"} and
   * matcher2 = greaterThan(1) <br>
   * - Passes if actual = [{p1: "hello", p2: 2}, 1], matcher1 = matchesObject{p1: startsWith("he")}
   * and matcher2 = greaterThan(1) <br>
   * - Does not pass if actual = [{p1: "hello", p2: 2}, 1], matcher1 = matchesObject{p1: "hello"}
   * and matcher2 = greaterThan(10)
   * 
   * @param expected
   *          a list of matchers that are expected to match with the actual JSONArray
   * 
   * @return the hasItems for matchers matcher
   */
  @SafeVarargs
  public static Matcher<JSONArray> hasItems(Matcher<?>... expected) {
    return new HasMatchingItems(Arrays.asList(expected));
  }
}
