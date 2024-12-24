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

package org.openbravo.base.weld.test.testinfrastructure;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.base.weld.test.WeldBaseTest;

/**
 * Test cases checking test case parameterization with cdi and parameter skipping.
 */
public class ParameterizedCdiWithSkip extends WeldBaseTest {
  private static final List<String> PARAMS = Arrays.asList("param1", "param2", "param3", "param4");
  private static final List<String> SKIP = Arrays.asList("param2", "param4");

  /** defines the values the parameter will take. */
  @Rule
  public ParameterCdiTestRule<String> parameterValuesRule = new ParameterCdiTestRule<>(PARAMS,
      SKIP::contains);

  /** this field will take the values defined by parameterValuesRule field. */
  private @ParameterCdiTest String parameter;

  private static int counterTest = 0;
  private static String testExecution = "";

  private static List<String> getNonSkippedParams() {
    return PARAMS.stream().filter(p -> !SKIP.contains(p)).collect(Collectors.toList());
  }

  /** Test case to be executed once per non skipped parameter value */
  @Test
  public void test() {
    assertThat("parameter value", parameter, equalTo(getNonSkippedParams().get(counterTest)));
    counterTest++;
    testExecution += parameter;
  }

  /**
   * Checks the previous test cases were executed as many times as non skipped parameter values in
   * the list.
   */
  @AfterClass
  public static void testsShouldBeExecutedOncePerNonSkippedParameter() {
    List<String> nonSkippedParams = getNonSkippedParams();
    String expectedValue = nonSkippedParams.stream().collect(Collectors.joining(""));

    assertThat("# of executions for test", nonSkippedParams.size(), is(counterTest));
    assertThat("test result", testExecution, equalTo(expectedValue));
  }
}
