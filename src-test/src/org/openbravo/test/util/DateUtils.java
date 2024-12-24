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
package org.openbravo.test.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Testing utilities to work with dates
 */
public class DateUtils {

  private DateUtils() {
  }

  /**
   * Gets today's date
   * 
   * @return a {@link Date} with the date of today
   */
  public static Date today() {
    return Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  /**
   * Gets yesterday's date
   * 
   * @return a {@link Date} with the day of yesterday
   */
  public static Date yesterday() {
    return daysAgo(1);
  }

  /**
   * Gets tomorrow's date
   * 
   * @return a {@link Date} with the date of tomorrow
   */
  public static Date tomorrow() {
    return daysAfter(1);
  }

  /**
   * Gets the date of n days ago starting from today
   *
   * @param days
   *          The number of days to be subtracted to the current date
   * @return a {@link Date} with the date of n days ago, where n is the provided value.
   */
  public static Date daysAgo(int days) {
    LocalDate date = LocalDate.now().minusDays(days);
    return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  /**
   * Gets the date of n days after today
   *
   * @param days
   *          The number of days to be added to the current date
   * @return a {@link Date} with the date of n days after, where n is the provided value.
   */
  public static Date daysAfter(int days) {
    LocalDate date = LocalDate.now().plusDays(days);
    return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  /**
   * Gets the Instant of n hours ago starting from now
   * 
   * @param hoursAgo
   *          The number of hours to be subtracted
   * @return the current {@link Instant} minus the specified number of hours
   */
  public static Instant hoursAgo(int hoursAgo) {
    return LocalDateTime.now().minusHours(hoursAgo).atZone(ZoneId.systemDefault()).toInstant();
  }
}
