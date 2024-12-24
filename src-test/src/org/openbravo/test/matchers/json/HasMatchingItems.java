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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.codehaus.jettison.json.JSONArray;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * A matcher to check whether a JSONArray matches with a given list of matchers
 */
class HasMatchingItems extends TypeSafeMatcher<JSONArray> {

  private List<Matcher<?>> matchers;
  private List<Matcher<?>> nonMatching;

  HasMatchingItems(List<Matcher<?>> matchers) {
    this.matchers = matchers;
  }

  @Override
  protected boolean matchesSafely(JSONArray item) {
    nonMatching = matchers.stream()
        .filter(matcher -> !anyMatch(matcher, item))
        .collect(Collectors.toList());
    return nonMatching.isEmpty();
  }

  private boolean anyMatch(Matcher<?> matcher, JSONArray item) {
    for (int i = 0; i < item.length(); i++) {
      if (matcher.matches(item.opt(i))) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("expected items: ");
    addToDescription(matchers, description);
  }

  @Override
  public void describeMismatchSafely(JSONArray item, Description description) {
    description.appendText("missing items matching: ");
    addToDescription(nonMatching, description);
  }

  private void addToDescription(List<Matcher<?>> m, Description description) {
    IntStream.range(0, m.size()).forEach(idx -> {
      m.get(idx).describeTo(description);
      if (idx < m.size() - 1) {
        description.appendText(", ");
      }
    });
  }
}
