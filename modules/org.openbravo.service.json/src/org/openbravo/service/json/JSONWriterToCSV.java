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

package org.openbravo.service.json;

import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.Property;
import org.openbravo.base.model.domaintype.EnumerateDomainType;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.window.OBViewUtil;
import org.openbravo.client.kernel.reference.DateTimeUIDefinition;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.PropertyNotFoundException;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.ui.Element;
import org.openbravo.model.ad.ui.FieldTrl;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.utils.Replace;

/**
 * Helper class to write JSON objects to generate a CSV file using the standard preferences.
 * 
 * This class will be commonly used by first creating a new instance and that will write the header
 * in the writer, then invoking the write method for each of the json objects that need to be
 * written and then by invoking the writeCSVFooterNote method to write the fotter, if any.
 *
 */
public class JSONWriterToCSV extends DefaultJsonDataService.QueryResultWriter {

  private static final Logger log = LogManager.getLogger();

  private static final String[] CSV_FORMULA_PREFIXES = new String[] { "=", "+", "-", "@" };
  private static final String YES_NO_REFERENCE_ID = "20";

  private Writer writer;
  private String fieldSeparator;
  private String decimalSeparator;
  private List<String> fieldProperties;
  private Map<String, String> niceFieldProperties = new HashMap<>();
  private boolean propertiesWritten = false;
  private Map<String, Map<String, String>> refLists = new HashMap<>();
  private List<String> refListCols = new ArrayList<>();
  private List<String> dateCols = new ArrayList<>();
  private List<String> dateTimeCols = new ArrayList<>();
  private List<String> timeCols = new ArrayList<>();
  private List<String> numericCols = new ArrayList<>();
  private List<String> yesNoCols = new ArrayList<>();
  private int clientUTCOffsetMiliseconds;
  private TimeZone clientTimeZone;
  private String translatedLabelYes;
  private String translatedLabelNo;

  /**
   * @param printWriter
   *          Writer that will be used to write the results of the export process.
   * @param vars
   *          VariablseSecureApp with the session data.
   * @param parameters
   *          It contains the parameters required to build properly the CSV file.
   * @param entity
   *          Entity to load the properties.
   */
  public JSONWriterToCSV(Writer printWriter, VariablesSecureApp vars,
      Map<String, String> parameters, Entity entity) {
    try {
      OBContext.setAdminMode();
      writer = printWriter;
      Window window = JsonUtils.isValueEmpty(parameters.get(JsonConstants.TAB_PARAMETER)) ? null
          : OBDal.getInstance()
              .get(Tab.class, parameters.get(JsonConstants.TAB_PARAMETER))
              .getWindow();

      loadSeparatorValues(vars, window);
      loadClientUTCOffsetMiliseconds(parameters);
      loadClientTimeZone();
      loadFieldProperties(parameters);
      loadNiceFieldProperties(parameters, entity, window);

      writeCSVHeaderNote(parameters);
      writeCSVHeader();
    } catch (Exception e) {
      throw new OBException("Error while exporting a CSV file", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void loadSeparatorValues(VariablesSecureApp vars, Window window)
      throws PropertyException {
    try {
      decimalSeparator = Preferences.getPreferenceValue("OBSERDS_CSVDecimalSeparator", true,
          OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
          OBContext.getOBContext().getRole(), window);
    } catch (PropertyNotFoundException e) {
      // There is no preference for the decimal separator.
      decimalSeparator = vars.getSessionValue("#DecimalSeparator|generalQtyEdition")
          .substring(0, 1);
    }
    try {
      fieldSeparator = Preferences.getPreferenceValue("OBSERDS_CSVFieldSeparator", true,
          OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
          OBContext.getOBContext().getRole(), window);
    } catch (PropertyNotFoundException e) {
      // There is no preference for the field separator. Using the default one.
      fieldSeparator = ",";
    }
    if (decimalSeparator.equals(fieldSeparator)) {
      if (!fieldSeparator.equals(";")) {
        fieldSeparator = ";";
      } else {
        fieldSeparator = ",";
      }
      log.warn(
          "Warning: CSV Field separator is identical to the decimal separator. Changing the field separator to {} to avoid generating a wrong CSV file",
          fieldSeparator);
    }
  }

  private void loadClientUTCOffsetMiliseconds(Map<String, String> parameters) {
    if (parameters.get(JsonConstants.UTCOFFSETMILISECONDS_PARAMETER).length() > 0) {
      clientUTCOffsetMiliseconds = Integer
          .parseInt(parameters.get(JsonConstants.UTCOFFSETMILISECONDS_PARAMETER));
    } else {
      clientUTCOffsetMiliseconds = 0;
    }
  }

  private void loadClientTimeZone() {
    clientTimeZone = null;
    try {
      String clientTimeZoneId = Preferences.getPreferenceValue("localTimeZoneID", true,
          OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
          OBContext.getOBContext().getRole(), null);
      List<String> validTimeZoneIDs = Arrays.asList(TimeZone.getAvailableIDs());
      if (validTimeZoneIDs.contains(clientTimeZoneId)) {
        clientTimeZone = TimeZone.getTimeZone(clientTimeZoneId);
      } else {
        log.error(
            "{} is not a valid time zone identifier. For a list of all accepted identifiers check http://www.java2s.com/Tutorial/Java/0120__Development/GettingallthetimezonesIDs.htm",
            clientTimeZoneId);
      }
    } catch (PropertyException pe) {
      log.warn(
          "The local Local Timezone ID property is not defined. It can be defined in a preference. For a list of all accepted values check http://www.java2s.com/Tutorial/Java/0120__Development/GettingallthetimezonesIDs.htm");
    }
  }

  private void loadFieldProperties(Map<String, String> parameters) throws JSONException {
    fieldProperties = new ArrayList<>();
    if (!JsonUtils.isValueEmpty(parameters.get("viewState"))) {
      String viewStateO = parameters.get("viewState");
      String viewStateWithoutParenthesis = viewStateO.substring(1, viewStateO.length() - 1);
      JSONObject viewState = new JSONObject(viewStateWithoutParenthesis);
      String fieldA = viewState.getString("field");
      JSONArray fields = new JSONArray(fieldA);
      for (int i = 0; i < fields.length(); i++) {
        JSONObject field = fields.getJSONObject(i);
        if ((field.has("visible") && !field.getBoolean("visible"))
            || field.getString("name").equals("_checkboxField")
            || field.getString("name").equals("_editLink")) {
          // The field is not visible. We should not export it
          continue;
        }
        fieldProperties.add(field.getString("name"));
      }
    } else if (!JsonUtils.isValueEmpty(parameters.get(JsonConstants.FIELDNAMES_PARAMETER))) {
      JSONArray fields = new JSONArray(parameters.get(JsonConstants.FIELDNAMES_PARAMETER));
      for (int i = 0; i < fields.length(); i++) {
        fieldProperties.add(fields.getString(i));
      }
    }
  }

  private void loadNiceFieldProperties(Map<String, String> parameters, Entity entity,
      Window window) {
    if (entity == null) {
      return;
    }
    // Now we calculate ref lists and nice property names
    final String userLanguageId = OBContext.getOBContext().getLanguage().getId();
    final Map<String, Property> properties = loadEntityProperties(entity);
    boolean translateYesNoReferences = shouldTranslateYesNoReferencesInCsv(window);
    for (Entry<String, Property> propEntry : properties.entrySet()) {
      final String propKey = propEntry.getKey();
      final Property prop = propEntry.getValue();
      if (prop == null) {
        loadTabNiceFieldProperty(propKey, null, userLanguageId, parameters);
      } else {
        Column col = OBDal.getInstance().get(Column.class, prop.getColumnId());
        if (prop.isAuditInfo()) {
          loadAuditInfoNiceFieldProperty(propKey, col);
        } else {
          loadTabNiceFieldProperty(propKey, col, userLanguageId, parameters);
        }

        loadTypesCols(propKey, prop, translateYesNoReferences);

        if (prop.getDomainType() instanceof EnumerateDomainType) {
          loadRefListCols(propKey, col, userLanguageId);
        }
      }
    }
    loadOrgAuditNiceFieldProperty();
  }

  /**
   * Load Organization audit (orgCreation and orgUpdated) properties into niceFieldProperties to
   * display proper labels for those field's header when they are visible in the View
   */
  private void loadOrgAuditNiceFieldProperty() {
    niceFieldProperties.put("orgCreationDate", OBMessageUtils.getI18NMessage("OrgCreated"));
    niceFieldProperties.put("orgUpdatedDate", OBMessageUtils.getI18NMessage("OrgUpdated"));
  }

  private Map<String, Property> loadEntityProperties(Entity entity) {
    final Map<String, Property> properties = new HashMap<>();
    for (Property prop : entity.getProperties()) {
      if (!fieldProperties.contains(prop.getName())) {
        continue;
      }
      properties.put(prop.getName(), prop);
    }
    for (String fieldProperty : fieldProperties) {
      if (fieldProperty.contains(DalUtil.FIELDSEPARATOR)) {
        properties.put(fieldProperty, DalUtil.getPropertyFromPath(entity, fieldProperty));
      } else if (canBeResolvedAsAdditionalProperty(entity, fieldProperty)) {
        properties.put(fieldProperty, null);
      }
    }
    return properties;
  }

  private boolean canBeResolvedAsAdditionalProperty(Entity entity, String property) {
    return WeldUtils.getInstances(AdditionalPropertyResolver.class)
        .stream()
        .anyMatch(r -> r.canResolve(entity, property));
  }

  private void loadAuditInfoNiceFieldProperty(String propKey, Column col) {
    Element element = null;
    if ("creationDate".equals(propKey)) {
      element = OBViewUtil.createdElement;
    } else if ("createdBy".equals(propKey)) {
      element = OBViewUtil.createdByElement;
    } else if ("updated".equals(propKey)) {
      element = OBViewUtil.updatedElement;
    } else if ("updatedBy".equals(propKey)) {
      element = OBViewUtil.updatedByElement;
    }
    if (element != null) {
      niceFieldProperties.put(propKey, OBViewUtil.getLabel(element, element.getADElementTrlList()));
    } else {
      niceFieldProperties.put(propKey, col.getName());
    }
  }

  private void loadTabNiceFieldProperty(String propKey, Column column, String userLanguageId,
      Map<String, String> parameters) {
    String tabId = parameters.get(JsonConstants.TAB_PARAMETER);
    if (StringUtils.isBlank(tabId)) {
      niceFieldProperties.put(propKey, column != null ? column.getName() : "");
      return;
    }
    Tab tab = OBDal.getInstance().get(Tab.class, tabId);
    String formattedPropKey = propKey.replace("$", ".");
    String columnId = column != null ? column.getId() : null;
    tab.getADFieldList()
        .stream()
        .filter(f -> (f.getProperty() != null && formattedPropKey.equals(f.getProperty()))
            || (f.getColumn() != null && f.getColumn().getId().equals(columnId)))
        .findFirst()
        .map(f -> f.getADFieldTrlList()
            .stream()
            .filter(trl -> userLanguageId.equals(trl.getLanguage().getId()))
            .findFirst()
            .map(FieldTrl::getName)
            .orElse(f.getName()))
        .ifPresent(name -> niceFieldProperties.put(propKey, name));
  }

  private void loadTypesCols(String propKey, Property prop, boolean translateYesNoReferences) {
    // We also store the date properties
    if (prop.isDate()) {
      dateCols.add(propKey);
    } else if (prop.isDatetime()) {
      dateTimeCols.add(propKey);
    } else if (prop.isTime()) {
      timeCols.add(propKey);
    } else if (prop.isPrimitive() && prop.isNumericType()) {
      numericCols.add(propKey);
    } else if (isYesNoReference(prop) && translateYesNoReferences) {
      yesNoCols.add(propKey);
    }
  }

  private void loadRefListCols(String propKey, Column col, String userLanguageId) {
    String referenceId = col.getReferenceSearchKey().getId();
    Map<String, String> reflists = new HashMap<>();
    final String hql = "select al.searchKey, al.name from ADList al where "
        + " al.reference.id=:referenceId and al.active=true";
    final Query<Object[]> qry = OBDal.getInstance().getSession().createQuery(hql, Object[].class);
    qry.setParameter("referenceId", referenceId);
    for (Object[] row : qry.list()) {
      reflists.put(row[0].toString(), row[1].toString());
    }
    final String hqltrl = "select al.searchKey, trl.name from ADList al, ADListTrl trl where "
        + " al.reference.id=:referenceId and trl.listReference=al and trl.language.id=:languageId"
        + " and al.active=true and trl.active=true";
    final Query<Object[]> qrytrl = OBDal.getInstance()
        .getSession()
        .createQuery(hqltrl, Object[].class);
    qrytrl.setParameter("referenceId", referenceId);
    qrytrl.setParameter("languageId", userLanguageId);
    for (Object[] row : qrytrl.list()) {
      reflists.put(row[0].toString(), row[1].toString());
    }
    refListCols.add(propKey);
    refLists.put(propKey, reflists);
  }

  private boolean isYesNoReference(Property prop) {
    final Column column = OBDal.getInstance().get(Column.class, prop.getColumnId());
    return YES_NO_REFERENCE_ID.equals(column.getReference().getId());
  }

  private boolean shouldTranslateYesNoReferencesInCsv(Window windowToCsv) {
    boolean shouldCheck = false;
    try {
      shouldCheck = Preferences.YES
          .equals(Preferences.getPreferenceValue("OBSERDS_CSVExportTranslateYesNoReference", true,
              OBContext.getOBContext().getCurrentClient(),
              OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
              OBContext.getOBContext().getRole(), windowToCsv));
    } catch (PropertyException prefNotDefined) {
      // There is no preference defined
    }
    return shouldCheck;
  }

  /**
   * Get selected properties those that will finally be exported.
   *
   */
  public List<String> getFieldProperties() {
    return fieldProperties;
  }

  /**
   * Writes the JSON object as a record in CSV format based on the class parameters settings.
   *
   * @param json
   *          JSON object to convert to CSV record.
   *
   */
  @Override
  public void write(JSONObject json) {
    try {
      if (!propertiesWritten) {
        writeJSONProperties(json);
      }
      writer.append("\n");
      final Iterator<?> itKeys = getIteratorKeys(json);
      boolean isFirst = true;
      while (itKeys.hasNext()) {
        String key = (String) itKeys.next();
        if (!key.endsWith(JsonConstants.IDENTIFIER)
            // Field is not visible. We don't show it
            && (fieldProperties.isEmpty() || fieldProperties.contains(key))) {
          if (isFirst) {
            isFirst = false;
          } else {
            writer.append(fieldSeparator);
          }
          if (!json.has(key)) {
            continue;
          }
          writer.append(getStringValue(key, json));
        }
      }
    } catch (Exception e) {
      throw new OBException("Error while exporting CSV information", e);
    }
  }

  private void writeJSONProperties(JSONObject row) {
    final Iterator<?> itKeysF = row.keys();
    ArrayList<String> keys = new ArrayList<>();
    boolean isFirst = true;
    try {
      while (itKeysF.hasNext()) {
        String key = (String) itKeysF.next();
        // Field is not visible. We don't show it
        if (!key.endsWith(JsonConstants.IDENTIFIER)
            // Field is not visible. We don't show it
            && (fieldProperties.isEmpty() || fieldProperties.contains(key))) {
          if (isFirst) {
            isFirst = false;
          } else {
            writer.append(fieldSeparator);
          }
          keys.add(key);
          writer.append("\"").append(key).append("\"");
        }
      }
      propertiesWritten = true;
    } catch (Exception e) {
      throw new OBException("Error while writing column names when exporting a CSV file", e);
    }
  }

  private Iterator<?> getIteratorKeys(JSONObject json) {
    final Iterator<?> itKeys;
    if (!fieldProperties.isEmpty()) {
      itKeys = fieldProperties.iterator();
    } else {
      itKeys = json.keys();
    }
    return itKeys;
  }

  private String getStringValue(String key, JSONObject json) throws JSONException, ParseException {
    Object keyValue = getKeyValue(key, json);
    boolean isNumeric = false;
    if (refListCols.contains(key)) {
      keyValue = refLists.get(key).get(keyValue);
    } else if (keyValue instanceof Number) {
      // if the CSV decimal separator property is defined, used it over the character
      // defined in Format.xml
      isNumeric = true;
      keyValue = keyValue.toString().replace(".", decimalSeparator);
    } else if (yesNoCols.contains(key) && keyValue != null) {
      keyValue = (boolean) keyValue ? getTranslatedLabelYes() : getTranslatedLabelNo();
    } else {
      keyValue = checkKeyValueDate(key, keyValue);
    }
    return getOutputValue(key, keyValue, isNumeric);
  }

  private Object getKeyValue(String key, JSONObject json) throws JSONException {
    return json.has(key + DalUtil.FIELDSEPARATOR + JsonConstants.IDENTIFIER)
        ? json.get(key + DalUtil.FIELDSEPARATOR + JsonConstants.IDENTIFIER)
        : json.get(key);
  }

  private Object checkKeyValueDate(String key, Object keyValue) throws ParseException {
    Object keyValueDate = keyValue;
    if (dateCols.contains(key) && keyValue != null && !keyValue.toString().equals("null")) {
      Date date = JsonUtils.createDateFormat().parse(keyValue.toString());
      keyValueDate = UIDefinitionController.DATE_UI_DEFINITION.convertToClassicString(date);
    } else if (dateTimeCols.contains(key) && keyValue != null
        && !keyValue.toString().equals("null")) {
      final String repairedString = JsonUtils.convertFromXSDToJavaFormat(keyValue.toString());
      Date localDate = JsonUtils.createDateTimeFormat().parse(repairedString);
      Date clientTimezoneDate = null;
      clientTimezoneDate = convertFromLocalToClientTimezone(localDate);
      keyValueDate = ((DateTimeUIDefinition) UIDefinitionController.DATETIME_UI_DEFINITION)
          .convertToClassicStringInLocalTime(clientTimezoneDate);
    } else if (timeCols.contains(key) && keyValue != null && !keyValue.toString().equals("null")) {
      Date dateUTC = JsonUtils.createTimeFormatWithoutGMTOffset().parse(keyValue.toString());
      Date clientTimezoneDate = null;
      clientTimezoneDate = convertFromUTCToClientTimezone(dateUTC);
      SimpleDateFormat timeFormat = JsonUtils.createTimeFormatWithoutGMTOffset();
      timeFormat.setLenient(true);
      keyValueDate = timeFormat.format(clientTimezoneDate);
    }
    return keyValueDate;
  }

  private String getOutputValue(String key, Object keyValue, boolean isNumeric) {
    String outputValue;
    if (keyValue != null && !keyValue.toString().equals("null")) {
      outputValue = keyValue.toString().replace("\"", "\"\"");
      if (!isNumeric && StringUtils.startsWithAny(outputValue, CSV_FORMULA_PREFIXES)) {
        // escape formulas
        outputValue = "\t" + outputValue;
      }
    } else {
      outputValue = "";
    }
    if (!numericCols.contains(key)) {
      outputValue = "\"" + outputValue + "\"";
    }
    return outputValue;
  }

  private String getTranslatedLabelYes() {
    if (translatedLabelYes == null) {
      translatedLabelYes = getTranslatedLabel("OBUISC_Yes");
    }
    return translatedLabelYes;
  }

  private String getTranslatedLabelNo() {
    if (translatedLabelNo == null) {
      translatedLabelNo = getTranslatedLabel("OBUISC_No");
    }
    return translatedLabelNo;
  }

  private String getTranslatedLabel(String label) {
    String userLanguage = OBContext.getOBContext().getLanguage().getLanguage();
    return Utility.messageBD(new DalConnectionProvider(false), label, userLanguage);
  }

  private Date convertFromLocalToClientTimezone(Date localDate) {

    Date dateUTC = convertFromLocalToUTCTimezone(localDate);

    return convertFromUTCToClientTimezone(dateUTC);
  }

  private Date convertFromUTCToClientTimezone(Date dateUTC) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(dateUTC);
    if (clientTimeZone != null) {
      calendar = Calendar.getInstance(clientTimeZone);
      calendar.setTime(dateUTC);
      int gmtMillisecondOffset = (calendar.get(Calendar.ZONE_OFFSET)
          + calendar.get(Calendar.DST_OFFSET));
      calendar.add(Calendar.MILLISECOND, gmtMillisecondOffset);
    } else {
      calendar = Calendar.getInstance();
      calendar.setTime(dateUTC);
      calendar.add(Calendar.MILLISECOND, clientUTCOffsetMiliseconds);
    }
    return calendar.getTime();
  }

  private Date convertFromLocalToUTCTimezone(Date localDate) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(localDate);

    int gmtMillisecondOffset = (calendar.get(Calendar.ZONE_OFFSET)
        + calendar.get(Calendar.DST_OFFSET));
    calendar.add(Calendar.MILLISECOND, -gmtMillisecondOffset);

    return calendar.getTime();
  }

  private void writeCSVHeaderNote(Map<String, String> parameters)
      throws IOException, PropertyException {
    final String csvHeaderMsg = getMessage(parameters, "OBSERDS_CSVHeaderMessage");

    if (StringUtils.isNotBlank(csvHeaderMsg)) {
      writer.append("\"").append(csvHeaderMsg).append("\"");
      fillEmptyColumns();
      writer.append("\n");
    }
  }

  private void writeCSVHeader() throws IOException {
    if (!fieldProperties.isEmpty()) {
      // If the request came with the view state information, we get the properties from there
      for (int i = 0; i < fieldProperties.size(); i++) {
        if (i > 0) {
          writer.append(fieldSeparator);
        }
        if (niceFieldProperties.get(fieldProperties.get(i)) != null) {
          writer.append("\"").append(niceFieldProperties.get(fieldProperties.get(i))).append("\"");
        } else {
          writer.append("\"").append(fieldProperties.get(i)).append("\"");
        }
      }
      propertiesWritten = true;
    }
  }

  /**
   * Writes the footer message of the CSV configured in the preference.
   *
   * @param parameters
   *          It contains the parameters required to build properly the CSV file.
   *
   */
  public void writeCSVFooterNote(Map<String, String> parameters)
      throws IOException, PropertyException {
    final String csvFooterMsg = getMessage(parameters, "OBSERDS_CSVFooterMessage");

    if (StringUtils.isNotBlank(csvFooterMsg)) {
      writer.append("\n").append("\"").append(csvFooterMsg).append("\"");
      fillEmptyColumns();
    }
  }

  private String getMessage(final Map<String, String> parameters, final String property)
      throws PropertyException {
    OBContext.setAdminMode(true);
    try {
      String csvMessage = null;
      try {
        Window window = JsonUtils.isValueEmpty(parameters.get(JsonConstants.TAB_PARAMETER)) ? null
            : OBDal.getInstance()
                .get(Tab.class, parameters.get(JsonConstants.TAB_PARAMETER))
                .getWindow();
        csvMessage = Preferences.getPreferenceValue(property, true,
            OBContext.getOBContext().getCurrentClient(),
            OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
            OBContext.getOBContext().getRole(), window);
      } catch (PropertyNotFoundException e) {
        // There is no preference defined
        csvMessage = null;
      }

      if (StringUtils.isNotBlank(csvMessage)) {
        csvMessage = Replace.replace(
            Replace.replace(Replace.replace(OBMessageUtils.messageBD(csvMessage), "\\n", "\n"),
                "&quot;", "\""),
            "\"", "\"\"");
      }
      return csvMessage;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void fillEmptyColumns() throws IOException {
    for (int i = 1; i < fieldProperties.size(); i++) {
      writer.append(fieldSeparator);
    }
  }
}
