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
 * All portions are Copyright (C) 2022-2023 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.cache;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.exception.OBException;

import com.github.benmanes.caffeine.cache.Ticker;

/**
 * Tests for TimeInvalidatedCache and TimeInvalidatedCacheBuilder
 */
public class TimeInvalidatedCacheTest {

  @Before
  public void setUp() {
    ValueTest.value = null;
  }

  @Test
  public void cacheShouldBeCorrectlyInitialized() {
    TimeInvalidatedCache<String, String> cache = initializeCache(key -> "TestValue");
    assertEquals("TestValue", cache.get("testKey"));
  }

  @Test
  public void cacheShouldBeInvalidatedAfterTimeAndValueChange() {
    FakeTicker ticker = new FakeTicker();
    ValueTest.value = "oldValue";

    TimeInvalidatedCache<String, String> cache = initializeCache(key -> ValueTest.value, ticker);
    assertEquals("oldValue", cache.get("testKey"));
    ValueTest.value = "newValue";
    // Make sure second all to .get still gets the same value
    assertEquals("oldValue", cache.get("testKey"));

    ticker.advance(Duration.ofSeconds(5));
    assertEquals("newValue", cache.get("testKey"));
  }

  @Test
  public void cacheShouldBeInvalidatedDirectlyAndValueChange() {
    ValueTest.value = "oldValue";

    TimeInvalidatedCache<String, String> cache = initializeCache(key -> ValueTest.value);
    assertEquals("oldValue", cache.get("testKey"));
    ValueTest.value = "newValue";
    // Make sure second all to .get still gets the same value
    assertEquals("oldValue", cache.get("testKey"));

    cache.invalidateAll();
    assertEquals("newValue", cache.get("testKey"));
  }

  @Test
  public void cacheShouldBeAbleToInvalidateSingleKeyDirectlyAndValueChange() {
    ValueTest.value = "oldValue";

    TimeInvalidatedCache<String, String> cache = initializeCache(key -> ValueTest.value);
    assertEquals("oldValue", cache.get("testKey"));
    assertEquals("oldValue", cache.get("testKey2"));
    ValueTest.value = "newValue";
    // Make sure second all to .get still gets the same value
    assertEquals("oldValue", cache.get("testKey"));

    cache.invalidate("testKey");
    assertEquals("newValue", cache.get("testKey"));
    // Only testKey should have changed its value, because it was invalidated
    assertEquals("oldValue", cache.get("testKey2"));
  }

  @Test
  public void cacheShouldGetNullIfNotComputableValue() {
    TimeInvalidatedCache<String, String> cache = initializeCache(key -> null);
    assertNull(cache.get("testKey"));
  }

  @Test
  public void cacheShouldGetNullIfNotComputableAndNoDefaultValue() {
    TimeInvalidatedCache<String, String> cache = initializeCache(key -> null);
    assertNull(cache.get("testKey", key -> null));
  }

  @Test
  public void cacheShouldBeAbleToRetrieveSeveralValues() {
    TimeInvalidatedCache<String, String> cache = initializeCache(key -> key + "Value");
    assertEquals("oldKeyValue", cache.get("oldKey"));
    assertEquals("testKeyValue", cache.get("testKey"));

    Map<String, String> expectedValues = Map.of( //
        "testKey", "testKeyValue", //
        "someKey", "someKeyValue");
    assertEquals(expectedValues, cache.getAll(List.of("testKey", "someKey")));
  }

  @Test
  public void cacheShouldBeAbleToRetrieveDefaultValue() {
    TimeInvalidatedCache<String, String> cache = initializeCache(key -> null);
    assertNull(cache.get("testKey"));
    assertEquals("testDefaultValue", cache.get("testKey", key -> "testDefaultValue"));
  }

  @Test
  public void cacheShouldRetrieveCachedValueInsteadOfDefaultValueIfExists() {
    TimeInvalidatedCache<String, String> cache = initializeCache(key -> "testValue");
    assertEquals("testValue", cache.get("testKey", key -> "testDefaultValue"));
  }

  @Test
  public void cacheShouldBeAbleToRetrieveDefaultAndCachedValues() {
    TimeInvalidatedCache<String, String> cache = initializeCache(key -> {
      if (key.equals("testKey2")) {
        return "testKey2CachedValue";
      }
      return null;
    });
    List<String> testKeys = List.of("testKey", "testKey2", "testKey3");

    Map<String, String> expectedValues = Map.of( //
        "testKey", "testKeyValue", //
        "testKey2", "testKey2CachedValue", //
        "testKey3", "testKey3Value");
    assertEquals(expectedValues, cache.getAll(testKeys,
        keys -> keys.stream().collect(Collectors.toMap(key -> key, key -> key + "Value"))));
  }

  @Test
  public void cacheShouldBeAbleToRetrieveCachedValuesIfAlreadyComputable() {
    TimeInvalidatedCache<String, String> cache = initializeCache(key -> key + "Value");
    List<String> testKeys = List.of("testKey", "testKey2", "testKey3");

    Map<String, String> expectedValues = Map.of( //
        "testKey", "testKeyValue", //
        "testKey2", "testKey2Value", //
        "testKey3", "testKey3Value");
    assertEquals(expectedValues, cache.getAll(testKeys, keys -> Collections.emptyMap()));
  }

  @Test
  public void cacheShouldNotRetrieveValueIfNotComputableInGetAll() {
    TimeInvalidatedCache<String, String> cache = initializeCache(key -> {
      if ("testKey2".equals(key)) {
        return null;
      }
      return key + "Value";
    });
    List<String> testKeys = List.of("testKey", "testKey2", "testKey3");

    Map<String, String> expectedValues = Map.of( //
        "testKey", "testKeyValue", //
        "testKey3", "testKey3Value");
    assertEquals(expectedValues, cache.getAll(testKeys, keys -> Collections.emptyMap()));
  }

  @Test
  public void cacheShouldHasNameSet() {
    TimeInvalidatedCache<String, String> cache = TimeInvalidatedCache.newBuilder()
        .name("TestCache")
        .build(key -> null);
    assertEquals("TestCache", cache.getName());
  }

  @Test
  public void cacheShouldThrowExceptionIfNameNotSet() {
    OBException thrown = assertThrows(OBException.class, () -> {
      TimeInvalidatedCache.newBuilder().build(key -> null);
    });

    assertThat(thrown.getMessage(), containsString(
        "Name must be set prior to executing TimeInvalidatedCacheBuilder build function."));
  }

  @Test
  public void removalListenerIsInvokedOnEntryExpiration()
      throws ExecutionException, InterruptedException, TimeoutException {
    FakeTicker ticker = new FakeTicker();
    CompletableFuture<Boolean> future = new CompletableFuture<>();

    TimeInvalidatedCache<String, String> cache = initializeCache(key -> "TestValue", ticker,
        (entry, reason) -> future.complete(true));
    assertEquals("TestValue", cache.get("testKey"));

    ticker.advance(Duration.ofSeconds(5));
    cache.get("testKey");

    Boolean executed = future.get(500, TimeUnit.MILLISECONDS);

    assertThat("Removal listener is executed", executed, equalTo(true));
  }

  @Test
  public void removalListenerIsNotInvokedIfEntryIsNotExpired()
      throws ExecutionException, InterruptedException, TimeoutException {
    FakeTicker ticker = new FakeTicker();
    CompletableFuture<Boolean> future = new CompletableFuture<>();

    TimeInvalidatedCache<String, String> cache = initializeCache(key -> "TestValue", ticker,
        (entry, reason) -> future.complete(true));
    assertEquals("TestValue", cache.get("testKey"));

    ticker.advance(Duration.ofSeconds(3));
    cache.get("testKey");

    Boolean executed;
    try {
      executed = future.get(500, TimeUnit.MILLISECONDS);
    } catch (TimeoutException ex) {
      executed = false;
    }

    assertThat("Removal listener is not executed", executed, equalTo(false));
  }

  @Test
  public void removalListenerIsInvokedOnEntryInvalidation()
      throws ExecutionException, InterruptedException, TimeoutException {
    CompletableFuture<Boolean> future = new CompletableFuture<>();

    TimeInvalidatedCache<String, String> cache = initializeCache(key -> "TestValue",
        (entry, reason) -> future.complete(true));
    assertEquals("TestValue", cache.get("testKey"));

    cache.invalidate("testKey");

    Boolean executed = future.get(500, TimeUnit.MILLISECONDS);

    assertThat("Removal listener is executed", executed, equalTo(true));
  }

  private TimeInvalidatedCache<String, String> initializeCache(UnaryOperator<String> buildMethod) {
    return TimeInvalidatedCache.newBuilder()
        .name("TestCache")
        .expireAfterDuration(Duration.ofSeconds(5))
        .build(buildMethod);
  }

  private TimeInvalidatedCache<String, String> initializeCache(UnaryOperator<String> buildMethod,
      Ticker ticker) {
    return TimeInvalidatedCache.newBuilder()
        .name("TestCache")
        .expireAfterDuration(Duration.ofSeconds(5))
        .ticker(ticker)
        .build(buildMethod);
  }

  private TimeInvalidatedCache<String, String> initializeCache(UnaryOperator<String> buildMethod,
      Ticker ticker, BiConsumer<Map.Entry<Object, Object>, String> listener) {
    return TimeInvalidatedCache.newBuilder()
        .name("TestCache")
        .expireAfterDuration(Duration.ofSeconds(5))
        .removalListener(listener)
        .ticker(ticker)
        .build(buildMethod);
  }

  private TimeInvalidatedCache<String, String> initializeCache(UnaryOperator<String> buildMethod,
      BiConsumer<Map.Entry<Object, Object>, String> listener) {
    return TimeInvalidatedCache.newBuilder()
        .name("TestCache")
        .expireAfterDuration(Duration.ofSeconds(5))
        .removalListener(listener)
        .build(buildMethod);
  }

  private static class ValueTest {
    private static String value;
  }
}
