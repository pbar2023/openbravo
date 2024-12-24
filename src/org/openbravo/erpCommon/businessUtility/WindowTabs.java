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
 * All portions are Copyright (C) 2001-2021 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.businessUtility;

import java.util.Hashtable;
import java.util.Stack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.ConnectionProvider;

/**
 * @author Fernando
 * 
 *         Class in charge of building the application's tabs for each window type.
 */
public class WindowTabs {
  static Logger log4j = LogManager.getLogger();
  private VariablesSecureApp vars;
  private ConnectionProvider conn;
  private String className = "";
  private String TabID = "";
  private String Title = "";
  private String ID = "";
  private String action = "";
  private int level = 0;
  private Hashtable<String, Stack<WindowTabsData>> tabs = new Hashtable<String, Stack<WindowTabsData>>();
  private Stack<WindowTabsData> breadcrumb = new Stack<WindowTabsData>();

  /**
   * Constructor Used by manual windows.
   * 
   * @param _conn
   *          Object with the database connection methods.
   * @param _vars
   *          Object with the session information.
   * @param _className
   *          String with the form's classname.
   * @throws Exception
   */
  public WindowTabs(ConnectionProvider _conn, VariablesSecureApp _vars, String _className)
      throws Exception {
    if (_conn == null || _vars == null || _className == null || _className.equals("")) {
      throw new Exception("Missing parameters");
    }
    this.conn = _conn;
    this.vars = _vars;
    this.className = _className;
    getWindowInfo();
    getTabs();
  }

  /**
   * Obtains all the window information from database. (For manual windows)
   * 
   * @throws Exception
   */
  private void getWindowInfo() throws Exception {
    WindowTabsData[] windowInfo = WindowTabsData.selectJavaInfo(this.conn, this.className);
    if (windowInfo == null || windowInfo.length == 0) {
      log4j.debug("Error while trying to obtain window info for class: " + this.className);
      return;
    }
    this.TabID = windowInfo[0].adTabId;
    this.action = windowInfo[0].action;
    windowInfo = WindowTabsData.selectMenuInfo(this.conn, this.vars.getLanguage(), this.action,
        this.TabID);
    if (windowInfo == null || windowInfo.length == 0) {
      log4j.debug("Error while trying to obtain window info for class: " + this.className);
      return;
    }
    this.ID = windowInfo[0].id;
    this.Title = windowInfo[0].name;
  }

  /**
   * Gets the menu root element for the selected window.
   * 
   * @return String with the menu root element.
   * @throws Exception
   */
  private String getMenuInfo() throws Exception {
    return WindowTabsData.selectMenuManual(this.conn, this.vars.getLanguage(), this.ID);
  }

  /**
   * Build the internal structure of all the tabs defined for the window.
   * 
   * @throws Exception
   */
  private void getTabs() throws Exception {
    WindowTabsData[] tabsAux = null;
    // find menu entry via ad_menu_id will always be <= 1 rows
    tabsAux = WindowTabsData.selectManual(this.conn, this.TabID, this.vars.getLanguage(), this.ID);
    if (tabsAux == null || tabsAux.length == 0) {
      log4j.debug("Error while trying to obtain tabs for id: " + this.TabID);
      return;
    }
    this.level = 0;
    getTabsByLevel(tabsAux[0]);
  }

  private void getTabsByLevel(WindowTabsData tabsAux) throws Exception {
    this.breadcrumb.push(tabsAux);
    final Stack<WindowTabsData> result = new Stack<WindowTabsData>();
    result.push(tabsAux);
    this.tabs.put("0", result);
  }

  /**
   * Method to get the parent's tabs of the actual (If exists).
   * 
   * @return String with the HTML text for the tabs.
   */
  public String parentTabs() {
    if (this.tabs == null) {
      return "";
    }
    return "<td class=\"tabBackGroundInit\"></td>";
  }

  /**
   * Method to get the tabs of the same level as the actual.
   * 
   * @return String with the HTML of the tabs.
   */
  public String mainTabs() {
    final StringBuffer text = new StringBuffer();
    if (this.tabs == null) {
      return text.toString();
    }
    final Stack<WindowTabsData> aux = this.tabs.get(Integer.toString(this.level));
    if (aux == null) {
      return text.toString();
    }

    text.append("<td class=\"tabBackGroundInit\">\n");
    text.append("  <div>\n");
    text.append("  <span class=\"tabTitle\">\n");
    text.append("    <div class=\"tabTitle_background\">\n");
    text.append("      <span class=\"tabTitle_elements_container\">\n");
    text.append("        <span class=\"tabTitle_elements_text\" id=\"tabTitle_text\">")
        .append(this.Title)
        .append("</span>\n");
    text.append(
        "        <span class=\"tabTitle_elements_separator\"><div class=\"tabTitle_elements_separator_icon\"></div></span>\n");
    text.append(
        "        <span class=\"tabTitle_elements_image\"><div class=\"tabTitle_elements_image_normal_icon\" id=\"TabStatusIcon\"></div></span>\n");
    text.append("      </span>\n");
    text.append("    </div>\n");
    text.append("  </span>\n");
    text.append("</div>\n");
    text.append("</td></tr><tr>");
    text.append("<td class=\"tabBackGround\">\n");
    text.append("  <div class=\"marginLeft\">\n");

    text.append("  </div>\n");
    text.append("</td>\n");
    return text.toString();
  }

  /**
   * Method to get the child tabs from the actual.
   * 
   * @return String with the HTML of the tabs.
   */
  public String childTabs(boolean readOnly) {
    return "<td class=\"tabTabbarBackGround\"></td>";
  }

  public String childTabs() {
    return childTabs(false);
  }

  /**
   * Method to obtain the breadcrumb for this tab.
   * 
   * @return String with the HTML of the breadcrumb.
   */
  public String breadcrumb() {
    final StringBuffer text = new StringBuffer();
    if (this.breadcrumb == null || this.breadcrumb.empty()) {
      return text.toString();
    }
    try {
      text.append("<span>").append(getMenuInfo()).append("</span>\n");
      text.append("&nbsp;||&nbsp;\n");
    } catch (final Exception ex) {
      ex.printStackTrace();
      log4j.error("Failed when trying to get parent menu element for breadcrumb");
    }
    final WindowTabsData data = this.breadcrumb.pop();
    text.append(data.tabname).append("\n");
    return text.toString();
  }

}
