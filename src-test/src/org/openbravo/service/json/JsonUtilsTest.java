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
package org.openbravo.service.json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.openbravo.test.matchers.json.JSONMatchers.equal;
import static org.openbravo.test.matchers.json.JSONMatchers.hasItems;

import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

/**
 * Test cases for the {@link JsonUtils} class
 */
public class JsonUtilsTest {

  @Test
  public void mergeJSONObjects() throws JSONException {
    JSONObject source = new JSONObject(Map.of(
    //@formatter:off
        "p1", "1",
        "p2", new JSONObject(Map.of("p22", "22", "p23", "23")),
        "p3", new JSONArray(List.of("p32", "p33")),
        "p4", "4"
    //@formatter:on
    ));
    JSONObject target = new JSONObject(Map.of(
    //@formatter:off
        "p0", "0",
        "p1", "",
        "p2", new JSONObject(Map.of("p21", "21")),
        "p3", new JSONArray(List.of("p31"))
    //@formatter:on
    ));
    JSONObject expected = new JSONObject(Map.of(
    //@formatter:off
        "p0", "0",
        "p1", "1",
        "p2", new JSONObject(Map.of("p21", "21", "p22", "22", "p23", "23")),
        "p3", new JSONArray(List.of("p31", "p32", "p33")),
        "p4", "4"
    //@formatter:on
    ));

    JsonUtils.merge(source, target);

    assertThat("Merge two JSON objects", target, equal(expected));
  }

  @Test
  public void mergeMapIntoJSONObject() throws JSONException {
    Map<String, Object> source = Map.of(
    //@formatter:off
        "p1", "1",
        "p2", new JSONObject(Map.of("p22", "22", "p23", "23")),
        "p3", new JSONArray(List.of("p32", "p33")),
        "p4", "4"
    //@formatter:on
    );
    JSONObject target = new JSONObject(Map.of(
    //@formatter:off
        "p0", "0",
        "p1", "",
        "p2", new JSONObject(Map.of("p21", "21")),
        "p3", new JSONArray(List.of("p31"))
    //@formatter:on
    ));
    JSONObject expected = new JSONObject(Map.of(
    //@formatter:off
        "p0", "0",
        "p1", "1",
        "p2", new JSONObject(Map.of("p21", "21", "p22", "22", "p23", "23")),
        "p3", new JSONArray(List.of("p31", "p32", "p33")),
        "p4", "4"
    //@formatter:on
    ));

    JsonUtils.merge(source, target);

    assertThat("Merge two JSON objects", target, equal(expected));
  }

  @Test
  public void mergeJSONArrays() throws JSONException {
    JSONArray source = new JSONArray(List.of("a", "c", 1));
    JSONArray target = new JSONArray(List.of("a", "b", 2));

    JsonUtils.merge(source, target);

    assertThat("Merged JSON array has the expected length", target.length(), equalTo(6));
    assertThat("Merge JSON array has the expected elements", target, hasItems("a", "b", "c", 1, 2));
  }
}
