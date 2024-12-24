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

package org.openbravo.erpCommon.utility;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.junit.Test;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.json.JsonUtils;
import org.openbravo.test.base.OBBaseTest;

/**
 * Tests the {@link OBDateUtils} class
 */
public class OBDateUtilsTest extends OBBaseTest {

  /**
   * Test truncate method without specified unit.
   * 
   * @throws ParseException
   */
  @Test
  public void truncateTimeInDate() throws ParseException {

    String strDateWithTime = "2022-07-01T19:55:55+0200";
    String strDateWithoutTime = "2022-07-01";

    Date dateWithTime = JsonUtils.createDateTimeFormat().parse(strDateWithTime);
    Date dateWithoutTime = JsonUtils.createDateFormat().parse(strDateWithoutTime);
    Date dateTrucanted = OBDateUtils.truncate(dateWithTime);

    assertThat(dateTrucanted, equalTo(dateWithoutTime));
  }

  /**
   * Test truncate method wit specified unit.
   * 
   * @throws ParseException
   */
  @Test
  public void truncateMinutesInDate() throws ParseException {

    String strDateWithMinutes = "2022-07-01T19:55:55+0200";
    String strDateWithoutMinutes = "2022-07-01T19:00:00+0200";

    Date dateWithMinutes = JsonUtils.createDateTimeFormat().parse(strDateWithMinutes);
    Date dateWithoutMinutes = JsonUtils.createDateTimeFormat().parse(strDateWithoutMinutes);
    Date dateTrucanted = OBDateUtils.truncate(dateWithMinutes, ChronoUnit.HOURS);

    assertThat(dateTrucanted, equalTo(dateWithoutMinutes));
  }

  @Test
  public void convertServerDateToTimezoneProvidedByOrganization() {
    Date serverDate = Date.from(Instant.parse("2023-09-18T10:00:00Z"));
    Organization organization = mock(Organization.class);
    when(organization.getTimezone()).thenReturn("Asia/Tokyo");
    ZonedDateTime tokyoDate = ZonedDateTime.of(2023, 9, 18, 19, 0, 0, 0, ZoneId.of("Asia/Tokyo"));

    ZonedDateTime convertedDate = OBDateUtils.convertFromServerToOrgDateTime(serverDate,
        organization);

    assertNotNull(convertedDate);
    assertThat(convertedDate.toString(), equalTo(tokyoDate.toString()));
  }

  @Test
  public void convertServerDateToDefinedTimezone() {
    Date serverDate = Date.from(Instant.parse("2023-09-18T10:00:00Z"));
    ZonedDateTime tokyoDate = ZonedDateTime.of(2023, 9, 18, 19, 0, 0, 0, ZoneId.of("Asia/Tokyo"));

    ZonedDateTime convertedDate = OBDateUtils.convertFromServerToOrgDateTime(serverDate,
        "Asia/Tokyo");

    assertNotNull(convertedDate);
    assertThat(convertedDate.toString(), equalTo(tokyoDate.toString()));
  }

  @Test
  public void dateConverterWillReturnNullIfTheTimezoneDoesNotExist() {
    Date serverDate = Date.from(Instant.parse("2023-09-18T10:00:00Z"));

    ZonedDateTime convertedDate = OBDateUtils.convertFromServerToOrgDateTime(serverDate,
        "Europe/Pamplona");

    assertNull(convertedDate);
  }

  @Test
  public void zonedDateTimeIsProperlyFormatted() {
    ZonedDateTime tokyoDate = ZonedDateTime.of(2023, 9, 18, 19, 0, 0, 0, ZoneId.of("Asia/Tokyo"));

    assertThat(OBDateUtils.formatZonedDateTime(tokyoDate, "dd-MM-yyyy HH:mm:ss"),
        equalTo("18-09-2023 19:00:00"));
  }

  @Test
  public void convertServerDateToTimeWillReturnNullWhenTimezoneIsNotAvailable() {
    Date serverDate = Date.from(Instant.parse("2023-09-18T10:00:00Z"));
    Organization organization = mock(Organization.class);
    when(organization.getTimezone()).thenReturn(null); // Org has no timezone defined

    ZonedDateTime convertedDate = OBDateUtils.convertFromServerToOrgDateTime(serverDate,
        organization);

    assertNull(convertedDate);
  }

  @Test
  public void formatZonedDateTimeWillReturnEmptyStringWhenDateIsNull() {
    assertThat(OBDateUtils.formatZonedDateTime(null, "dd-MM-yyyy HH:mm:ss"), equalTo(""));
  }
}
