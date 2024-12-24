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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Provides capabilities for comparing JSONObjects and JSONArrays
 */
abstract class JSONMatcher<T extends Object> extends TypeSafeMatcher<T> {

  /**
   * Finds the different properties between two JSONObjects. These are the properties that have a
   * different value in the both JSONObjects or those that are only present in just one of the
   * JSONObjects.
   * 
   * @param actual
   *          the actual object
   * @param expected
   *          the object that is expected to be equal to the actual one
   * 
   * @return the set of property keys that are different in the two JSONObjects
   */
  protected Set<String> getDifferentProperties(JSONObject actual, JSONObject expected) {
    Set<String> differentProperties;
    if (actual.length() > expected.length()) {
      differentProperties = getKeySet(actual);
      differentProperties.removeAll(getKeySet(expected));
    } else if (actual.length() < expected.length()) {
      differentProperties = getKeySet(expected);
      differentProperties.removeAll(getKeySet(actual));
    } else {
      differentProperties = getKeySet(actual).stream()
          .filter(key -> !propertiesAreEqual(actual.opt(key), expected.opt(key), true))
          .collect(Collectors.toSet());
    }
    return differentProperties;
  }

  /**
   * Finds the properties that do not match between the actual JSONObject and a subset of the
   * properties of a JSONObject
   * 
   * @param actual
   *          the actual object
   * @param subset
   *          the subset of properties that are expected to match in the actual object
   * 
   * @return the set of property keys that do not match between the actual JSONObject and the subset
   */
  protected Set<String> getNonMatchingProperties(JSONObject actual, JSONObject subset) {
    if (subset.length() == 0 && actual.length() == 0) {
      return Collections.emptySet();
    }
    Set<String> nonMatchingProperties;
    if (subset.length() > actual.length()) {
      nonMatchingProperties = getKeySet(subset);
      nonMatchingProperties.removeAll(getKeySet(actual));
    } else {
      JSONObject common = getCommonProperties(actual, subset);
      if (common.length() == 0) {
        // not any common property
        nonMatchingProperties = getKeySet(subset);
      } else if (subset.length() > common.length()) {
        // subset and actual have the same number of properties but there are missing properties in
        // actual that are in subset
        nonMatchingProperties = getKeySet(subset);
        nonMatchingProperties.removeAll(getKeySet(common));
      } else {
        nonMatchingProperties = getKeySet(common).stream()
            .filter(key -> !propertiesAreEqual(common.opt(key), subset.opt(key), false))
            .collect(Collectors.toSet());
      }
    }
    return nonMatchingProperties;
  }

  /**
   * Finds the object of a list that do not have an equal object in the given JSONArray
   * 
   * @param array
   *          the actual JSONArray
   * @param objects
   *          the list of objects that are expected to have an equal object in the actual array
   * 
   * @return a list of the objects that do not have an equal object in the JSONArray
   */
  protected List<Object> getMissingObjects(JSONArray array, List<Object> objects) {
    return objects.stream()
        .filter(json -> !hasEqualObject(array, json))
        .collect(Collectors.toList());
  }

  /**
   * Can be used to include into the test mismatch description the information about the differences
   * in the properties of two JSONObjects
   * 
   * @param properties
   *          the keys of the properties that are different
   * @param actual
   *          the actual JSONObject
   * @param expected
   *          the expected JSONObject
   * @param description
   *          The test mismatch description
   */
  protected void addDifferencesToDescription(Set<String> properties, JSONObject actual,
      JSONObject expected, Description description) {
    int i = 0;
    for (String key : properties) {
      addToDescription(key, actual, expected, description);
      if (i < properties.size() - 1) {
        description.appendText(", ");
      }
      i += 1;
    }
  }

  private boolean objectsAreEqual(Object actual, Object expected) {
    if (!(actual instanceof JSONObject) || !(expected instanceof JSONObject)) {
      return actual.equals(expected);
    }
    return getDifferentProperties((JSONObject) actual, (JSONObject) expected).isEmpty();
  }

  private boolean objectsMatch(JSONObject actual, JSONObject subset) {
    return getNonMatchingProperties(actual, subset).isEmpty();
  }

  private boolean arrayContains(JSONArray array, List<Object> objects) {
    return getMissingObjects(array, objects).isEmpty();
  }

  private void addToDescription(String key, JSONObject item, JSONObject expected,
      Description description) {
    Object property = expected.opt(key);
    description.appendText(key).appendText(" is ");
    if (item.has(key)) {
      description.appendValue(item.opt(key));
    } else {
      description.appendText("not present");
    }

    description.appendText(" but expected ");
    if (!expected.has(key)) {
      description.appendText("not present");
    } else if (property instanceof Matcher) {
      @SuppressWarnings("rawtypes")
      Matcher matcher = (Matcher) property;
      matcher.describeTo(description);
    } else {
      description.appendValue(property);
    }
  }

  private boolean propertiesAreEqual(Object actual, Object expected, boolean strict) {
    if (actual == null || expected == null) {
      return actual == null && expected == null;
    }
    if (actual.getClass() != expected.getClass()) {
      if (actual instanceof Number && expected instanceof Number) {
        return areEqualNumericValues((Number) actual, (Number) expected);
      } else if (canCompareTimestampValues(actual, expected)) {
        return areEqualStringValues(actual, expected);
      } else if (expected instanceof Matcher<?>) {
        try {
          return ((Matcher<?>) expected).matches(actual);
        } catch (ClassCastException ex) {
          // trying to match an actual of unexpected type
          return false;
        }
      }
      return false;
    }
    if (!strict && actual instanceof JSONObject && expected instanceof JSONObject) {
      return objectsMatch((JSONObject) actual, (JSONObject) expected);
    }
    if (!strict && actual instanceof JSONArray && expected instanceof JSONArray) {
      return arraysMatch((JSONArray) actual, (JSONArray) expected);
    }
    if (actual instanceof JSONObject) {
      return objectsAreEqual(actual, expected);
    }
    if (actual instanceof JSONArray) {
      return arraysAreEquivalent((JSONArray) actual, (JSONArray) expected);
    }
    return actual.equals(expected);
  }

  private boolean arraysAreEquivalent(JSONArray array1, JSONArray array2) {
    if (array1.length() != array2.length()) {
      return false;
    }
    return arrayContains(array1, asStream(array2).collect(Collectors.toList()));
  }

  private boolean arraysMatch(JSONArray array1, JSONArray array2) {
    return asStream(array2)
        .filter(obj -> asStream(array1).noneMatch(o -> propertiesAreEqual(o, obj, false)))
        .count() == 0;
  }

  private boolean areEqualNumericValues(Number number1, Number number2) {
    return new BigDecimal(number1.toString()).compareTo(new BigDecimal(number2.toString())) == 0;
  }

  private boolean canCompareTimestampValues(Object object1, Object object2) {
    if (object1 instanceof Timestamp) {
      return object2 instanceof Timestamp || object2 instanceof String;
    }
    return object1 instanceof String && object2 instanceof Timestamp;
  }

  private boolean areEqualStringValues(Object object1, Object object2) {
    return object1.toString().equals(object2.toString());
  }

  private boolean hasEqualObject(JSONArray array, Object object) {
    return asStream(array).anyMatch(json -> objectsAreEqual(json, object));
  }

  private JSONObject getCommonProperties(JSONObject json1, JSONObject json2) {
    @SuppressWarnings("unchecked")
    Stream<String> stream = asStream(json1.keys());
    return stream.filter(json2::has).collect(Collector.of(JSONObject::new, (result, key) -> {
      try {
        result.put(key, json1.get(key));
      } catch (JSONException ignore) {
        // should not fail
      }
    }, (object1, object2) -> {
      throw new UnsupportedOperationException(
          "This JSONObject collector does not support combine operation");
    }));
  }

  private Stream<String> asStream(Iterator<String> sourceIterator) {
    Iterable<String> iterable = () -> sourceIterator;
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  private Stream<Object> asStream(JSONArray array) {
    return IntStream.range(0, array.length()).mapToObj(array::opt);
  }

  private Set<String> getKeySet(JSONObject json) {
    @SuppressWarnings("unchecked")
    Stream<String> keys = asStream(json.keys());
    return keys.collect(Collectors.toSet());
  }
}
