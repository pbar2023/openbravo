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
 * All portions are Copyright (C) 2001-2023 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.utility;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.Sqlc;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.ui.Window;

public class UsedByLink extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private static final String DISABLE_LINKED_ITEMS_SECTION_PREFERENCE = "OBUIAPP_DisableLinkedItemsSection";

  @Override
  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    final VariablesSecureApp vars = new VariablesSecureApp(request);

    String output;
    if (isLinkedItemsSectionDisabled(vars)) {
      output = getDisabledLinkedItemsSectionResponse(vars.getLanguage());
    } else if (vars.commandIn("JSONCategory")) {
      output = getJSONCategories(vars);
    } else if (vars.commandIn("JSONLinkedItem")) {
      output = getJSONLinkedItems(vars);
    } else {
      throw new ServletException("2.50 commands are no longer supported: " + vars.getCommand());
    }

    try (PrintWriter out = response.getWriter()) {
      response.setContentType("application/json;charset=UTF-8");
      out.print(output);
    }
  }

  private boolean isLinkedItemsSectionDisabled(VariablesSecureApp vars) {
    String windowId = vars.getStringParameter("windowId");
    try {
      OBContext currentContext = OBContext.getOBContext();
      String preferenceValue = Preferences.getPreferenceValue(
          DISABLE_LINKED_ITEMS_SECTION_PREFERENCE, true, currentContext.getCurrentClient(),
          currentContext.getCurrentOrganization(), currentContext.getUser(),
          currentContext.getRole(), OBDal.getInstance().getProxy(Window.class, windowId));

      if (Preferences.YES.equals(preferenceValue)) {
        log4j.error("Linked Items section is disabled for window with id " + windowId);
        return true;
      }
    } catch (PropertyException e) {
      // preference not found, Linked Items Section is not disabled
    }
    return false;
  }

  private String getDisabledLinkedItemsSectionResponse(String language) {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("msg", Utility.messageBD(this, "LinkedItemsSectionIsDisabled", language));
    } catch (JSONException e) {
      log4j.error("Error trying to generate message for Disabled Linked Items Section");
    }
    return jsonObject.toString();
  }

  /**
   * Returns a JSON object containing linked item categories.
   */
  private String getJSONCategories(VariablesSecureApp vars) throws ServletException, IOException {

    final String windowId = vars.getStringParameter("windowId");
    final String entityName = vars.getStringParameter("entityName");

    JSONObject jsonObject = new JSONObject();

    try {

      final Entity entity = ModelProvider.getInstance().getEntity(entityName);
      final String tableId = entity.getTableId();

      final List<Property> properties = entity.getIdProperties();

      // we expect to have only one key property for the entity
      if (properties == null || properties.size() != 1) {
        throw new OBException(String.format(
            "Exactly one key property was expected for entity [%s]. Actual key properties are %s",
            entityName, properties));
      }

      final Property property = properties.get(0);
      final String columnId = property.getColumnId();
      final String columnName = property.getColumnName();
      final String keyId = vars.getGlobalVariable("inp" + Sqlc.TransformaNombreColumna(columnName),
          windowId + "|" + columnName);

      // get categories
      final SearchResult searchResult = getLinkedItemCategories(vars, windowId, columnId, keyId,
          tableId);

      jsonObject = buildJsonObject(jsonObject, searchResult);

    } catch (Exception e) {
      log4j.error("Error getting Categories for windowId: " + windowId + " - entity: " + entityName,
          e);
      try {
        jsonObject.put("msg", "" + e.getMessage());
      } catch (JSONException jex) {
        log4j.error("Error trying to generate message: " + jex.getMessage(), jex);
      }
    }

    return jsonObject.toString();
  }

  private String getJSONLinkedItems(VariablesSecureApp vars) throws ServletException, IOException {

    // read request parameters
    final String windowId = vars.getStringParameter("windowId");
    final String entityName = vars.getStringParameter("entityName");
    final String adTabId = vars.getStringParameter("adTabId");
    final String tableName = vars.getStringParameter("tableName");
    // XXX isn't this the same as columnName declared later?
    final String inpcolumnName = vars.getStringParameter("columnName");

    JSONObject jsonObject = new JSONObject();

    try {

      final Entity entity = ModelProvider.getInstance().getEntity(entityName);
      final String tableId = entity.getTableId();

      final List<Property> properties = entity.getIdProperties();

      // we expect to have only one key property for the entity
      if (properties == null || properties.size() != 1) {
        throw new OBException(String.format(
            "Exactly one key property was expected for entity [%s]. Actual key properties are %s",
            entityName, properties));
      }

      final Property property = properties.get(0);
      final String columnName = property.getColumnName();
      final String keyId = vars.getGlobalVariable("inp" + Sqlc.TransformaNombreColumna(columnName),
          windowId + "|" + columnName);

      final UsedByLinkData[] data = UsedByLinkData.keyColumns(this, adTabId);
      if (data != null && data.length > 0) {
        final SearchResult searchResult = getLinkedItems(vars, data, windowId, columnName, keyId,
            adTabId, tableName, inpcolumnName, tableId);
        // jsonObject
        jsonObject = buildJsonObject(jsonObject, searchResult);
      }

    } catch (Exception e) {
      log4j.error("Error getting linkend items for tab " + adTabId, e);
      try {
        jsonObject.put("msg", "" + e.getMessage());
      } catch (JSONException jex) {
        log4j.error("Error trying to generate message: " + jex.getMessage(), jex);
      }
    }
    return jsonObject.toString();
  }

  /**
   * Gets linked items categories. If some of the data can't be accessed (i.g. lack of rights) then
   * a corresponding message is generated and put into result.
   */
  private SearchResult getLinkedItemCategories(VariablesSecureApp vars, String strWindow,
      String keyColumn, String keyId, String tableId) throws IOException, ServletException {

    final String keyColumnId = UsedByLinkData.selectKeyColumnId(this, tableId);
    int numOfRelatedColumns = 0;
    boolean nonAccessible = false;

    UsedByLinkData[] data = null;

    // Obtain the list of columns that are linked to the current one using DAL, this list will
    // return
    // any reference including user defined ones. It will be joined with the old ones because
    // currently it doesn't support views.
    StringBuffer linkedColumnsQuery = new StringBuffer();
    for (LinkedTable linkedTable : getLinkedTables(tableId)) {
      if (linkedColumnsQuery.length() != 0) {
        linkedColumnsQuery.append(", ");
      }
      linkedColumnsQuery.append(linkedTable.toQueryString());
    }
    if (linkedColumnsQuery.length() == 0) {
      linkedColumnsQuery.append("'--'");
      numOfRelatedColumns = 0;
    } else {
      numOfRelatedColumns = linkedColumnsQuery.toString().split(",").length;
    }

    if (numOfRelatedColumns < 1000) {
      data = UsedByLinkData.select(this, vars.getClient(), vars.getLanguage(), vars.getRole(),
          keyColumnId, keyColumn, tableId, linkedColumnsQuery.toString());

      if (data != null && data.length > 0) {
        final Vector<UsedByLinkData> vecTotal = new Vector<UsedByLinkData>();
        Role role = OBContext.getOBContext().getRole();
        for (int i = 0; i < data.length; i++) {
          if (log4j.isDebugEnabled()) {
            log4j.debug("***Referenced tab: " + data[i].adTabId);
          }
          try {
            final UsedByLinkData[] dataRef = UsedByLinkData.windowRef(this, data[i].adTabId);
            if (dataRef == null || dataRef.length == 0) {
              continue;
            }
            String strWhereClause = getWhereClause(vars, strWindow, dataRef[0].whereclause);
            if (log4j.isDebugEnabled()) {
              log4j.debug("***   Referenced where clause (1): " + strWhereClause);
            }
            strWhereClause += getAditionalWhereClause(vars, strWindow, data[i].adTabId,
                data[i].tablename, keyColumn, data[i].columnname,
                UsedByLinkData.getTabTableName(this, tableId));
            if (log4j.isDebugEnabled()) {
              log4j.debug("***   Referenced where clause (2): " + strWhereClause);
            }
            if (!nonAccessible) {
              final String strNonAccessibleWhere = strWhereClause + " AND AD_ORG_ID NOT IN ("
                  + vars.getUserOrg() + ")";
              if (!UsedByLinkData
                  .countLinks(this, data[i].tablename, data[i].columnname, keyId,
                      strNonAccessibleWhere)
                  .equals("0")) {
                nonAccessible = true;
              }
            }
            strWhereClause += " AND AD_ORG_ID IN (" + vars.getUserOrg() + ") AND AD_CLIENT_ID IN ("
                + vars.getUserClient() + ")";
            int total = Integer
                .valueOf(UsedByLinkData.countLinks(this, data[i].tablename, data[i].columnname,
                    keyId, strWhereClause))
                .intValue();

            if (log4j.isDebugEnabled()) {
              log4j.debug("***   Count: " + total);
            }

            data[i].total = Integer.toString(total);

            if (data[i].accessible.equals("N") && role.isManual() && total > 0) {
              nonAccessible = true;
            } else if (total > 0 && !existsInVector(data[i], vecTotal)) {
              vecTotal.addElement(data[i]);
            }
          } catch (Exception ignore) {
            log4j.warn(
                "Coulnd't get linked items for " + data[i].tablename + "." + data[i].columnname,
                ignore);
          }
        }
        data = new UsedByLinkData[vecTotal.size()];
        vecTotal.copyInto(data);
      }

      if (nonAccessible) {
        final OBError myMessage = new OBError();
        myMessage.setType("Info");
        myMessage.setMessage(Utility.messageBD(this, "NonAccessibleRecords", vars.getLanguage()));
        myMessage.setTitle(Utility.messageBD(this, "Info", vars.getLanguage()));
        return new SearchResult(data, myMessage);
      } else {
        return new SearchResult(data);
      }
    } else {
      final OBError myMessage = new OBError();
      myMessage.setType("Info");
      myMessage.setMessage(Utility.messageBD(this, "TooManyColumnsLinked", vars.getLanguage()));
      myMessage.setTitle(Utility.messageBD(this, "Info", vars.getLanguage()));
      return new SearchResult(data, myMessage);
    }
  }

  private boolean existsInVector(UsedByLinkData elem, Vector<UsedByLinkData> vec) {

    for (UsedByLinkData i : vec) {
      if (elem.adTabId.equals(i.adTabId)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gets the linked items for the given category.
   */
  private SearchResult getLinkedItems(VariablesSecureApp vars, UsedByLinkData[] data,
      String strWindow, String keyColumn, String keyId, String adTabId, String adTableName,
      String strCOLUMNNAME, String adTableId) throws ServletException, IOException {

    final UsedByLinkData[] dataRef = UsedByLinkData.windowRef(this, adTabId);
    if (dataRef == null || dataRef.length == 0) {
      final OBError message = new OBError();
      message.setType("Error");
      message.setMessage(Utility.messageBD(this, "RecordError", vars.getLanguage()));
      message.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
      return new SearchResult(message);
    }

    String whereClause = getWhereClause(vars, strWindow, dataRef[0].whereclause);
    whereClause += getAditionalWhereClause(vars, strWindow, adTabId, adTableName, keyColumn,
        strCOLUMNNAME, UsedByLinkData.getTabTableName(this, adTabId));
    whereClause += " AND " + adTableName + ".AD_ORG_ID IN (" + vars.getUserOrg() + ") AND "
        + adTableName + ".AD_CLIENT_ID IN (" + vars.getUserClient() + ")";

    final StringBuffer strSQL = new StringBuffer();
    for (int i = 0; i < data.length; i++) {
      if (i > 0) {
        strSQL.append(" || ', ' || ");
      }
      strSQL.append("").append(adTableName).append(".").append(data[i].name).append("");
    }

    UsedByLinkData[] usedByLinkData = UsedByLinkData.selectLinks(this, strSQL.toString(),
        adTableName, adTableName + "." + data[0].name, vars.getLanguage(), adTabId,
        adTableName + "." + strCOLUMNNAME, keyId, whereClause);

    return new SearchResult(usedByLinkData);
  }

  private static JSONObject buildJsonObject(JSONObject jsonObject, SearchResult searchResult)
      throws JSONException {
    final UsedByLinkData[] usedByLinkData = searchResult.getUsedByLinkData();
    final OBError msg = searchResult.getMessage();

    final List<JSONObject> usedByLinkDataJsonObjects = new ArrayList<JSONObject>();
    if (usedByLinkData != null) {
      for (UsedByLinkData data : usedByLinkData) {
        final JSONObject usedByLinkDataJsonObj = new JSONObject();
        usedByLinkDataJsonObj.put("accessible", data.accessible);
        usedByLinkDataJsonObj.put("adMenuName", data.adMenuName);
        usedByLinkDataJsonObj.put("adTabId", data.adTabId);
        usedByLinkDataJsonObj.put("adWindowId", data.adWindowId);
        usedByLinkDataJsonObj.put("columnName", data.columnname);
        usedByLinkDataJsonObj.put("elementName", data.elementName);
        usedByLinkDataJsonObj.put("fullElementName",
            data.elementName + " - " + data.tabname + " (" + data.total + ")");
        usedByLinkDataJsonObj.put("hasTree", data.hastree);
        usedByLinkDataJsonObj.put("id", data.id);
        usedByLinkDataJsonObj.put("name", data.name);
        usedByLinkDataJsonObj.put("referencedColumnId", data.referencedColumnId);
        usedByLinkDataJsonObj.put("tableName", data.tablename);
        usedByLinkDataJsonObj.put("tabName", data.tabname);
        usedByLinkDataJsonObj.put("total", data.total);
        usedByLinkDataJsonObj.put("whereClause", data.whereclause);
        usedByLinkDataJsonObj.put("windowName", data.windowname);
        usedByLinkDataJsonObj.put("singleRecord", "SR".equals(data.uipattern));
        usedByLinkDataJsonObj.put("readOnly", "RO".equals(data.uipattern));
        usedByLinkDataJsonObj.put("editOrDeleteOnly", "ED".equals(data.uipattern));
        usedByLinkDataJsonObjects.add(usedByLinkDataJsonObj);
      }
      jsonObject.put("usedByLinkData", usedByLinkDataJsonObjects);
    }
    if (msg != null) {
      jsonObject.put("msg", msg.getMessage());
    }
    return jsonObject;
  }

  public String getWhereClause(VariablesSecureApp vars, String window, String strWhereClause)
      throws ServletException {
    String strWhere = strWhereClause;
    if (strWhere.equals("") || strWhere.indexOf("@") == -1) {
      return ((strWhere.equals("") ? "" : " AND ") + strWhere);
    }
    if (log4j.isDebugEnabled()) {
      log4j.debug("WHERE CLAUSE: " + strWhere);
    }
    final StringBuffer where = new StringBuffer();
    String token = "";
    int i = 0;
    i = strWhere.indexOf("@");
    while (i != -1) {
      where.append(strWhere.substring(0, i));
      if (log4j.isDebugEnabled()) {
        log4j.debug("WHERE ACTUAL: " + where.toString());
      }
      strWhere = strWhere.substring(i + 1);
      if (log4j.isDebugEnabled()) {
        log4j.debug("WHERE COMPARATION: " + strWhere);
      }
      if (strWhere.startsWith("SQL")) {
        strWhere.substring(4);
        where.append("(");
      } else {
        i = strWhere.indexOf("@");
        if (i == -1) {
          log4j.error("Unable to parse the following string: " + strWhereClause + "\nNow parsing: "
              + where.toString());
          throw new ServletException("Unable zo parse the following string: " + strWhereClause
              + "\nNow parsing: " + where.toString());
        }
        token = strWhere.substring(0, i);
        strWhere = (i == strWhere.length()) ? "" : strWhere.substring(i + 1);
        if (log4j.isDebugEnabled()) {
          log4j.debug("TOKEN: " + token);
        }
        final String tokenResult = "'" + Utility.getContext(this, vars, token, window) + "'";
        if (log4j.isDebugEnabled()) {
          log4j.debug("TOKEN PARSED: " + tokenResult);
        }
        if (tokenResult.equalsIgnoreCase(token)) {
          log4j.error("Unable to parse the String " + strWhereClause + "\nNow parsing: "
              + where.toString());
          throw new ServletException("Unable to parse the string: " + strWhereClause
              + "\nNow parsing: " + where.toString());
        }
        where.append(tokenResult);
      }
      i = strWhere.indexOf("@");
    }
    ;
    where.append(strWhere);
    return " AND " + where.toString();
  }

  private String getAditionalWhereClause(VariablesSecureApp vars, String strWindow, String adTabId,
      String tableName, String keyColumn, String columnName, String parentTableName)
      throws ServletException {
    String result = "";
    if (log4j.isDebugEnabled()) {
      log4j.debug("getAditionalWhereClause - ad_Tab_ID: " + adTabId);
    }
    final UsedByLinkData[] data = UsedByLinkData.parentTabTableName(this, adTabId);
    if (data != null && data.length > 0) {
      if (log4j.isDebugEnabled()) {
        log4j.debug("getAditionalWhereClause - parent tab: " + data[0].adTabId);
      }
      UsedByLinkData[] dataColumn = UsedByLinkData.parentsColumnName(this, adTabId,
          data[0].adTabId);
      if (dataColumn == null || dataColumn.length == 0) {
        if (log4j.isDebugEnabled()) {
          log4j.debug("getAditionalWhereClause - searching parent Columns Real");
        }
        dataColumn = UsedByLinkData.parentsColumnReal(this, adTabId, data[0].adTabId);
      }
      if (dataColumn == null || dataColumn.length == 0) {
        if (log4j.isDebugEnabled()) {
          log4j.debug("getAditionalWhereClause - no parent columns found");
        }
        return result;
      }
      result += " AND EXISTS (SELECT 1 FROM " + data[0].tablename + " WHERE " + data[0].tablename
          + "." + ((!dataColumn[0].name.equals("")) ? dataColumn[0].name : keyColumn) + " = "
          + tableName + "." + ((!dataColumn[0].name.equals("")) ? dataColumn[0].name : columnName);
      final UsedByLinkData[] dataRef = UsedByLinkData.windowRef(this, data[0].adTabId);
      String strAux = "";
      if (dataRef != null && dataRef.length > 0) {
        strAux = getWhereClause(vars, strWindow, dataRef[0].whereclause);
      }
      result += strAux;
      // Check where clause for parent tabs
      result += getAditionalWhereClause(vars, strWindow, data[0].adTabId, data[0].tablename, "", "",
          parentTableName);
      result += ")";
    }
    if (log4j.isDebugEnabled()) {
      log4j.debug("getAditionalWhereClause - result: " + result);
    }
    return result;
  }

  /**
   * Obtains a list of all tables that link to the tableId
   * 
   */
  private List<LinkedTable> getLinkedTables(String tableId) {
    OBContext.setAdminMode();
    try {
      Table table = OBDal.getInstance().get(Table.class, tableId), linkedTableObject = null;
      String tableName = table.getDBTableName(), dataOriginType = null, linkedTableId = null;

      final List<LinkedTable> linkedTables = new ArrayList<LinkedTable>();
      for (Entity entity : ModelProvider.getInstance().getModel()) {
        if (entity.isVirtualEntity()) {
          continue;
        }
        linkedTableId = entity.getTableId();
        linkedTableObject = OBDal.getInstance().get(Table.class, linkedTableId);
        dataOriginType = linkedTableObject.getDataOriginType();
        if (!"TABLE".equalsIgnoreCase(dataOriginType)) {
          // Datasource tables are skipped
          continue;
        }
        for (Property property : entity.getProperties()) {
          // ignore one-to-many (a list of children)
          if (!property.isOneToMany() && property.getColumnName() != null
              && property.getTargetEntity() != null
              && property.getTargetEntity().getTableName().equalsIgnoreCase(tableName)
              && !property.isComputedColumn()) {
            final LinkedTable linkedTable = new LinkedTable();
            log4j.debug("p:" + property.getColumnName());
            linkedTable.setColumnId(property.getColumnId());
            linkedTables.add(linkedTable);
          }
        }
      }
      return linkedTables;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private static class LinkedTable {
    private String columnId;

    public void setColumnId(String columnId) {
      this.columnId = columnId;
    }

    public String toQueryString() {
      return "'" + columnId + "'";
    }
  }

  @Override
  public String getServletInfo() {
    return "Servlet that presents the usedBy links";
  } // end of getServletInfo() method

  /**
   * Contains found linked items along with a message if some of the items are not accessible.
   * 
   * @author Valery Lezhebokov
   */
  private static class SearchResult {
    private final UsedByLinkData[] usedByLinkData;
    private final OBError message;

    private SearchResult(UsedByLinkData[] usedByLinkData) {
      this(usedByLinkData, null);
    }

    private SearchResult(OBError message) {
      this(null, message);
    }

    private SearchResult(UsedByLinkData[] usedByLinkData, OBError message) {
      this.usedByLinkData = usedByLinkData;
      this.message = message;
    }

    private UsedByLinkData[] getUsedByLinkData() {
      return usedByLinkData;
    }

    private OBError getMessage() {
      return message;
    }
  }
}
