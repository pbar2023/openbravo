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
package org.openbravo.test.base;

import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * Enables the use of Mockito annotations by default and provides some mocking utilities.
 */
public class MockableBaseTest {

  private AutoCloseable closeable;

  @Before
  public void setUp() throws Exception {
    closeable = MockitoAnnotations.openMocks(this);
  }

  @After
  public void testDone() throws Exception {
    closeable.close();
  }

  /**
   * Utility method to enable the mocking of static invocations of the given class within the
   * current thread.
   *
   * @param clz
   *          The class whose static invocations can be mocked
   * @param test
   *          The testing logic to be executed with the static mock which is the input argument of
   *          the {@link Consumer}. This way it can be used to stub the mock methods required by the
   *          test.
   */
  public static <T> void mockStatic(Class<T> clz, Consumer<MockedStatic<T>> test) {
    try (MockedStatic<T> mock = Mockito.mockStatic(clz)) {
      test.accept(mock);
    }
  }
}
