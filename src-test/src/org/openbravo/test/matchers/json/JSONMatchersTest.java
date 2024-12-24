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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.openbravo.test.matchers.json.JSONMatchers.equal;
import static org.openbravo.test.matchers.json.JSONMatchers.hasItems;
import static org.openbravo.test.matchers.json.JSONMatchers.matchesObject;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

/**
 * Tests the correct behavior of the matchers exposed by {@link JSONMatchers}
 */
public class JSONMatchersTest {

  @Test
  public void areEqual() {
    JSONObject json1 = BaseJSON.base();
    JSONObject json2 = BaseJSON.base();

    assertThat("JSON objects are equal", json1, equal(json2));
  }

  @Test
  public void innerObjectsAreEqual() {
    JSONObject json1 = BaseJSON.base();
    JSONObject json2 = BaseJSON.withProp("key6", new JSONObject(
        Map.of("key6a", new JSONObject(Map.of("key6a2", 2, "key6a1", 1)), "key6b", 2)));
    assertThat("JSON objects are equal", json1, equal(json2));
  }

  @Test
  public void withPropsWithSameNumericalValueAreEqual() {
    JSONObject json1 = BaseJSON.base();
    JSONObject json2 = BaseJSON.withProp("key2", BigDecimal.valueOf(2.5));

    assertThat("JSON objects are equal", json1, equal(json2));
  }

  @Test
  public void withPropsWithSameTimestampValueAreEqual() {
    JSONObject json1 = BaseJSON.base();
    JSONObject json2 = BaseJSON.withProp("key5", "2015-08-24 00:00:00.0");

    assertThat("JSON objects are equal", json1, equal(json2));
  }

  @Test
  public void withMatherAreEqual() {
    JSONObject json1 = BaseJSON.base();
    JSONObject json2 = BaseJSON.withProp("key2", greaterThan(2.4));

    assertThat("JSON objects are equal", json1, equal(json2));
  }

  @Test
  public void withAdditionalPropsAreNotEqual() {
    JSONObject json1 = BaseJSON.base();
    JSONObject json2 = BaseJSON.withProp("newKey", "1234");

    assertThat("JSON objects are not equal", json1, not(equal(json2)));
  }

  @Test
  public void withMissingPropsAreNotEqual() {
    JSONObject json1 = BaseJSON.base();
    JSONObject json2 = BaseJSON.withoutProp("key3");

    assertThat("JSON objects are not equal", json1, not(equal(json2)));
  }

  @Test
  public void withDifferentPropValueAreNotEqual1() {
    JSONObject json1 = BaseJSON.base();
    JSONObject json2 = BaseJSON.withProp("key2", BigDecimal.valueOf(1.5));

    assertThat("JSON objects are not equal", json1, not(equal(json2)));
  }

  @Test
  public void withDifferentPropValueAreNotEqual2() {
    JSONObject json1 = BaseJSON.base();
    JSONObject json2 = BaseJSON.withProp("key4", "efgh");

    assertThat("JSON objects are not equal", json1, not(equal(json2)));
  }

  @Test
  public void withDifferentPropValueAreNotEqual3() throws JSONException {
    JSONObject json1 = BaseJSON.base();
    JSONArray array = new JSONArray();
    JSONObject inner = new JSONObject();
    inner.put("key4a", Integer.valueOf(3));
    array.put(inner);
    JSONObject json2 = BaseJSON.withProp("key4", array);

    assertThat("JSON objects are not equal", json1, not(equal(json2)));
  }

  @Test
  public void innerObjectsAreNotEqual() {
    JSONObject json1 = BaseJSON.base();
    JSONObject json2 = BaseJSON.withProp("key6",
        new JSONObject(Map.of("key6a", new JSONObject(Map.of("key6a1", 1)), "key6b", 2)));
    assertThat("JSON objects are not equal", json1, not(equal(json2)));
  }

  @Test
  public void withMatherAreNotEqual() {
    JSONObject json1 = BaseJSON.base();
    JSONObject json2 = BaseJSON.withProp("key2", greaterThan(2.6));

    assertThat("JSON objects are equal", json1, not(equal(json2)));
  }

  @Test
  public void match() {
    JSONObject json1 = BaseJSON.base();
    JSONObject json2 = BaseJSON.base();

    assertThat("JSON objects are matching", json1, matchesObject(json2));
  }

  @Test
  public void matchWithPropertyMatcher() {
    JSONObject json1 = BaseJSON.base();
    JSONObject json2 = BaseJSON.withProp("key1", startsWith("abc"));

    assertThat("JSON objects are matching", json1, matchesObject(json2));
  }

  @Test
  public void matchWithSubset() {
    JSONObject json1 = BaseJSON.base();
    JSONObject json2 = BaseJSON.withoutProp("key3");

    assertThat("JSON objects are matching", json1, matchesObject(json2));
  }

  @Test
  public void matchWithSubset2() {
    JSONObject json1 = BaseJSON.base();
    JSONObject json2 = new JSONObject(Map.of("key6",
        new JSONObject(Map.of("key6a", new JSONObject(Map.of("key6a1", greaterThan(0)))))));

    assertThat("JSON objects are matching", json1, matchesObject(json2));
  }

  @Test
  public void emptyJSONObjectsMatch() {
    JSONObject json1 = new JSONObject();
    JSONObject json2 = new JSONObject();

    assertThat("JSON objects are matching", json1, matchesObject(json2));
  }

  @Test
  public void withMissingPropsNoMatch() {
    JSONObject json1 = BaseJSON.withoutProp("key3");
    JSONObject json2 = BaseJSON.base();

    assertThat("JSON objects are not matching", json1, not(matchesObject(json2)));
  }

  @Test
  public void withDifferentPropValueNoMatch1() {
    JSONObject json1 = BaseJSON.base();
    JSONObject json2 = BaseJSON.withProp("key4", Double.valueOf(3.5));

    assertThat("JSON objects are not matching", json1, not(matchesObject(json2)));
  }

  @Test
  public void withDifferentPropValueNoMatch2() throws JSONException {
    JSONObject json1 = BaseJSON.base();
    JSONArray array = new JSONArray();
    JSONObject inner = new JSONObject();
    inner.put("key4a", Integer.valueOf(3));
    array.put(inner);
    JSONObject json2 = BaseJSON.withProp("key4", array);

    assertThat("JSON objects are not matching", json1, not(matchesObject(json2)));
  }

  @Test
  public void withSameNumberOfPropsButDifferentShouldNotMatch() {
    JSONObject actual = new JSONObject(Map.of("p1", 1, "p2", 2, "p4", 4));
    JSONObject expected = new JSONObject(Map.of("p1", 1, "p2", 2, "p3", 3));

    assertThat("JSON objects are not matching", actual, not(matchesObject(expected)));
  }

  @Test
  public void compareMatchingArrayProperties() {
    JSONArray array1 = new JSONArray();
    array1.put(new JSONObject(Map.of("p1", "A", "p2", "A")));
    array1.put(new JSONObject(Map.of("p1", "B", "p2", "B")));
    JSONArray array2 = new JSONArray();
    array2.put(new JSONObject(Map.of("p1", "A")));
    array2.put(new JSONObject(Map.of("p2", "B")));

    JSONObject actual = new JSONObject(Map.of("array", array1));
    JSONObject expected = new JSONObject(Map.of("array", array2));

    assertThat("JSON objects are matching", actual, matchesObject(expected));
    assertThat("JSON objects are not equal", actual, not(equal(expected)));
  }

  @Test
  public void compareMatchingArrayPropertiesWithDifferenTypes() {
    JSONArray array1 = new JSONArray();
    array1.put(new JSONObject(Map.of("p1", "A", "p2", "A")));
    array1.put(1234);
    JSONArray array2 = new JSONArray();
    array2.put(1234);
    array2.put(new JSONObject(Map.of("p2", "A")));

    JSONObject actual = new JSONObject(Map.of("array", array1));
    JSONObject expected = new JSONObject(Map.of("array", array2));

    assertThat("JSON objects are matching", actual, matchesObject(expected));
    assertThat("JSON objects are not equal", actual, not(equal(expected)));
  }

  @Test
  public void compareMatchingArrayPropertiesWithMatchers() {
    JSONArray array1 = new JSONArray();
    array1.put(new JSONObject(Map.of("p1", "A", "p2", "A")));
    array1.put("abcde");
    array1.put(1234);
    JSONArray array2 = new JSONArray();
    array2.put(greaterThan(1000));
    array2.put(new JSONObject(Map.of("p2", "A")));
    array2.put(startsWith("abc"));

    JSONObject actual = new JSONObject(Map.of("array", array1));
    JSONObject expected = new JSONObject(Map.of("array", array2));

    assertThat("JSON objects are matching", actual, matchesObject(expected));
    assertThat("JSON objects are not equal", actual, not(equal(expected)));
  }

  @Test
  public void compareNonMatchingArrayProperties() {
    JSONArray array1 = new JSONArray();
    array1.put(new JSONObject(Map.of("p1", "A", "p2", "A")));
    array1.put(new JSONObject(Map.of("p1", "B", "p2", "B")));
    JSONArray array2 = new JSONArray();
    array2.put(new JSONObject(Map.of("p1", "A")));
    array2.put(new JSONObject(Map.of("p2", "C")));

    JSONObject actual = new JSONObject(Map.of("array", array1));
    JSONObject expected = new JSONObject(Map.of("array", array2));

    assertThat("JSON objects are not matching", actual, not(matchesObject(expected)));
    assertThat("JSON objects are not equal", actual, not(equal(expected)));
  }

  @Test
  public void compareNonMatchingArrayPropertiesWithDifferentTypes() {
    JSONArray array1 = new JSONArray();
    array1.put(new JSONObject(Map.of("p1", "A", "p2", "A")));
    array1.put(1234);
    JSONArray array2 = new JSONArray();
    array2.put(5678);
    array2.put(new JSONObject(Map.of("p2", "A")));

    JSONObject actual = new JSONObject(Map.of("array", array1));
    JSONObject expected = new JSONObject(Map.of("array", array2));

    assertThat("JSON objects are not matching", actual, not(matchesObject(expected)));
    assertThat("JSON objects are not equal", actual, not(equal(expected)));
  }

  @Test
  public void compareNonMatchingArrayPropertiesWithMatchers() {
    JSONArray array1 = new JSONArray();
    array1.put(new JSONObject(Map.of("p1", "A", "p2", "A")));
    array1.put("abcde");
    array1.put(1234);
    JSONArray array2 = new JSONArray();
    array2.put(greaterThan(1000));
    array2.put(new JSONObject(Map.of("p2", "A")));
    array2.put(startsWith("efg"));

    JSONObject actual = new JSONObject(Map.of("array", array1));
    JSONObject expected = new JSONObject(Map.of("array", array2));

    assertThat("JSON objects are not matching", actual, not(matchesObject(expected)));
    assertThat("JSON objects are not equal", actual, not(equal(expected)));
  }

  @Test
  public void arrayHasItems() {
    JSONArray array = Items.baseArray();
    List<JSONObject> items = Items.base();
    JSONObject item1 = items.get(0);
    JSONObject item2 = items.get(2);

    assertThat("JSON array has all the items", array, hasItems(item1, item2));
  }

  @Test
  public void arrayHasItemsWithPropertyMatcher() throws JSONException {
    JSONArray array = Items.baseArray();
    JSONObject inner = new JSONObject();
    inner.put("key2a", greaterThan(Double.valueOf(2)));
    inner.put("key2b", startsWith("abc"));

    assertThat("JSON array has all the items", array, hasItems(inner));
  }

  @Test
  public void arrayHasItemsWithDifferenTypes() {
    JSONArray array = Items.withObject(1);
    List<JSONObject> items = Items.base();
    JSONObject item1 = items.get(0);
    JSONObject item2 = items.get(2);
    int item3 = 1;

    assertThat("JSON array has all the items", array, hasItems(item1, item2, item3));
  }

  @Test
  public void arrayDoesNotHaveItems() {
    JSONArray array = Items.withoutObjectAt(0);
    List<JSONObject> items = Items.base();
    JSONObject item1 = items.get(0);
    JSONObject item2 = items.get(2);

    assertThat("JSON array has all the items", array, not(hasItems(item1, item2)));
  }

  @Test
  public void arrayDoesNotHaveItemsWithDifferenTypes() {
    JSONArray array = Items.withObject(1);
    List<JSONObject> items = Items.base();
    JSONObject item1 = items.get(0);
    JSONObject item2 = items.get(2);
    int item3 = 5;

    assertThat("JSON array has all the items", array, not(hasItems(item1, item2, item3)));
  }

  @Test
  public void arrayDoesNotHaveItemsWithPropertyMatcher() throws JSONException {
    JSONArray array = Items.baseArray();
    JSONObject inner = new JSONObject();
    inner.put("key2a", greaterThan(Double.valueOf(2)));
    inner.put("key2b", startsWith("efg"));

    assertThat("JSON array has all the items", array, not(hasItems(inner)));
  }

  @Test
  public void matchItems() throws JSONException {
    JSONArray array = Items.baseArray();
    JSONObject item1 = Items.base().get(0);
    JSONObject item2 = new JSONObject();
    item2.put("key2a", greaterThan(Double.valueOf(2)));
    item2.put("key2b", startsWith("abc"));

    assertThat("JSON array match all items", array, hasItems(equal(item1), matchesObject(item2)));
  }

  @Test
  public void matchItemsRegardlessTheOrder() throws JSONException {
    JSONArray array = Items.withReverseOrder();
    JSONObject item1 = Items.base().get(0);
    JSONObject item2 = new JSONObject();
    item2.put("key2a", greaterThan(Double.valueOf(2)));
    item2.put("key2b", startsWith("abc"));

    assertThat("JSON array match all items", array, hasItems(equal(item1), matchesObject(item2)));
  }

  @Test
  public void matchItemsWithDifferentMatcherTypes() throws JSONException {
    JSONArray array = Items.withObject(5);
    JSONObject item1 = Items.base().get(0);
    JSONObject item2 = new JSONObject();
    item2.put("key2a", greaterThan(Double.valueOf(2)));
    item2.put("key2b", startsWith("abc"));
    int item3 = 4;

    assertThat("JSON array match all items", array,
        hasItems(equal(item1), matchesObject(item2), greaterThan(item3)));
  }

  @Test
  public void itemsDoNotMatch() throws JSONException {
    JSONArray array = Items.baseArray();
    JSONObject item1 = Items.base().get(0);
    JSONObject item2 = new JSONObject();
    item2.put("key2a", greaterThan(Double.valueOf(2)));
    item2.put("key2b", startsWith("ccc"));

    assertThat("JSON array does not match all items", array,
        not(hasItems(equal(item1), matchesObject(item2))));
  }

  @Test
  public void itemsDoNotMatchWithDifferentMatcherTypes() throws JSONException {
    JSONArray array = Items.withObject(5);
    JSONObject item1 = Items.base().get(0);
    JSONObject item2 = new JSONObject();
    item2.put("key2a", greaterThan(Double.valueOf(2)));
    item2.put("key2b", startsWith("abc"));
    int item3 = 7;

    assertThat("JSON array does not match all items", array,
        not(hasItems(equal(item1), matchesObject(item2), greaterThan(item3))));
  }

  @Test
  public void nonEmptyJSONMatchesWithEmptyJSON() {
    assertThat("non empty JSON matches with empty JSON", BaseJSON.base(),
        matchesObject(BaseJSON.empty()));
  }

  @Test
  public void emptyJSONDoesNotMatchWithNonEmptyJSON() {
    assertThat("empty JSON does not match with non empty JSON", BaseJSON.empty(),
        not(matchesObject(BaseJSON.base())));
  }

  @Test
  public void nonEmptyJSONNotEqualToEmptyJSON() {
    assertThat("non empty JSON not equal to empty JSON", BaseJSON.base(),
        not(equal(BaseJSON.empty())));
  }

  private static class BaseJSON {
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private JSONObject json;

    private BaseJSON() {
      json = new JSONObject();

      try {
        json.put("key1", "abcd");
        json.put("key2", Double.valueOf(2.5));
        json.put("key3", true);
        json.put("key4", new JSONArray());
        JSONObject inner = new JSONObject();
        inner.put("key4a", Integer.valueOf(2));
        json.getJSONArray("key4").put(inner);
        json.put("key5", new Timestamp(formatter.parse("2015-08-24 00:00:00.0").getTime()));
        json.put("key6", new JSONObject(
            Map.of("key6a", new JSONObject(Map.of("key6a1", 1, "key6a2", 2)), "key6b", 2)));
      } catch (JSONException | ParseException ignore) {
        // should not fail
      }
    }

    private JSONObject getJSON() {
      return json;
    }

    static JSONObject base() {
      return new BaseJSON().getJSON();
    }

    static JSONObject withProp(String property, Object value) {
      JSONObject json = base();
      try {
        json.put(property, value);
      } catch (JSONException ignore) {
        // should not fail
      }
      return json;
    }

    static JSONObject withoutProp(String property) {
      JSONObject json = base();
      if (json.has(property)) {
        json.remove(property);
      }
      return json;
    }

    static JSONObject empty() {
      return new JSONObject();
    }
  }

  private static class Items {
    List<JSONObject> list;
    JSONArray array;

    private Items() {
      list = new ArrayList<>();
      array = new JSONArray();
      try {
        JSONObject inner1 = new JSONObject();
        inner1.put("key1a", Integer.valueOf(2));
        inner1.put("key1b", true);
        JSONObject inner2 = new JSONObject();
        inner2.put("key2a", Double.valueOf(2.5));
        inner2.put("key2b", "abcd");
        JSONObject inner3 = new JSONObject();
        inner3.put("key3a", new JSONObject());
        inner3.put("key3b", "abcd");

        addItem(inner1);
        addItem(inner2);
        addItem(inner3);
      } catch (JSONException ignore) {
        // should not fail
      }
    }

    private void addItem(JSONObject item) {
      list.add(item);
      array.put(item);
    }

    private List<JSONObject> getList() {
      return list;
    }

    private JSONArray getArray() {
      return array;
    }

    static List<JSONObject> base() {
      return new Items().getList();
    }

    static JSONArray baseArray() {
      return new Items().getArray();
    }

    static JSONArray withReverseOrder() {
      List<JSONObject> list = base();
      Collections.reverse(list);
      return new JSONArray(list);
    }

    static JSONArray withObject(Object object) {
      JSONArray array = baseArray();
      array.put(object);
      return array;
    }

    static JSONArray withoutObjectAt(int index) {
      List<JSONObject> list = base();
      list.remove(index);
      return new JSONArray(list);
    }
  }
}
