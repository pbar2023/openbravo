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
 * All portions are Copyright (C) 2009-2023 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.datasource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.SQLGrammarException;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.application.Process;
import org.openbravo.client.application.RefWindow;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.client.kernel.BaseKernelServlet;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.client.kernel.OBUserException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.SessionInfo;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.CsrfUtil;
import org.openbravo.erpCommon.utility.PropertyNotFoundException;
import org.openbravo.erpCommon.utility.StringCollectionUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.portal.PortalAccessible;
import org.openbravo.service.json.JSONWriterToCSV;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonUtils;
import org.openbravo.service.web.InvalidContentException;
import org.openbravo.service.web.InvalidRequestException;
import org.openbravo.service.web.ResourceNotFoundException;
import org.openbravo.service.web.WebServiceUtil;

/**
 * A web service which provides a JSON REST service using the {@link DataSourceService}
 * implementation. Retrieves the data source using the {@link DataSourceServiceProvider}.
 * 
 * @author mtaal
 */
public class DataSourceServlet extends BaseKernelServlet {
  private static final Logger log = LogManager.getLogger();

  private static final long serialVersionUID = 1L;

  private static String servletPathPart = "org.openbravo.service.datasource";

  public static String getServletPathPart() {
    return servletPathPart;
  }

  @Inject
  private DataSourceServiceProvider dataSourceServiceProvider;

  @Inject
  private ApplicationDictionaryCachedStructures cachedStructures;

  @Override
  public void init(ServletConfig config) {
    if (config.getInitParameter(DataSourceConstants.URL_NAME_PARAM) != null) {
      servletPathPart = config.getInitParameter(DataSourceConstants.URL_NAME_PARAM);
    }

    super.init(config);
  }

  @Override
  public void service(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {

    try {
      if (OBContext.getOBContext() != null && OBContext.getOBContext().isPortalRole()) {
        if (!(getDataSource(request) instanceof PortalAccessible)) {
          log.error("Portal user " + OBContext.getOBContext().getUser() + " with role "
              + OBContext.getOBContext().getRole()
              + " is trying to access to non granted datasource " + request.getRequestURL());
          throw new OBSecurityException();
        }
      }

      SessionInfo.setModuleId(request.getParameter("moduleId"));
      SessionInfo.setCommand(request.getParameter(DataSourceConstants.OPERATION_TYPE_PARAM));
      SessionInfo.setProcessId(request.getParameter(JsonConstants.TAB_PARAMETER));
      SessionInfo.setProcessType("W");

      super.service(request, response);
    } catch (final InvalidRequestException e) {
      if (SessionHandler.isSessionHandlerPresent()) {
        SessionHandler.getInstance().setDoRollback(true);
      }
      response.setStatus(400);
      log.error(e.getMessage(), e);
      writeResult(response, JsonUtils.convertExceptionToJson(e));
    } catch (final InvalidContentException e) {
      if (SessionHandler.isSessionHandlerPresent()) {
        SessionHandler.getInstance().setDoRollback(true);
      }
      response.setStatus(409);
      log.error(e.getMessage(), e);
      writeResult(response, JsonUtils.convertExceptionToJson(e));
    } catch (final ResourceNotFoundException e) {
      if (SessionHandler.isSessionHandlerPresent()) {
        SessionHandler.getInstance().setDoRollback(true);
      }
      response.setStatus(404);
      log.error(e.getMessage(), e);
      writeResult(response, JsonUtils.convertExceptionToJson(e));
    } catch (final OBSecurityException e) {
      if (SessionHandler.isSessionHandlerPresent()) {
        SessionHandler.getInstance().setDoRollback(true);
      }
      response.setStatus(401);
      log.error(e.getMessage(), e);
      writeResult(response, JsonUtils.convertExceptionToJson(e));
    } catch (final Throwable t) {
      if (SessionHandler.isSessionHandlerPresent()) {
        SessionHandler.getInstance().setDoRollback(true);
      }
      response.setStatus(500);
      log.error(t.getMessage(), t);
      writeResult(response, JsonUtils.convertExceptionToJson(t));
    }
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    final Map<String, String> parameters = getParameterMap(request);
    doFetch(request, response, parameters);
  }

  private void doFetch(HttpServletRequest request, HttpServletResponse response,
      Map<String, String> parameters) throws IOException, ServletException {
    // checks and set parameters, if not valid then go away
    if (!checkSetParameters(request, response, parameters)) {
      return;
    }

    // The WHERE_AND_FILTER_CLAUSE parameter is initialized, it will be set after.
    parameters.put(JsonConstants.WHERE_AND_FILTER_CLAUSE, "");

    parameters.put(JsonConstants.WHERE_CLAUSE_HAS_BEEN_CHECKED, "false");

    if (log.isDebugEnabled()) {
      getRequestContent(request);
    }
    try {
      if (!hasAccess(request, parameters.get(JsonConstants.TAB_PARAMETER))) {
        throw new OBUserException("AccessTableNoView");
      }

      String filterClass = parameters.get(DataSourceConstants.DS_FILTERCLASS_PARAM);

      if (filterClass != null) {
        try {
          DataSourceFilter filter = (DataSourceFilter) WeldUtils
              .getInstanceFromStaticBeanManager(Class.forName(filterClass));
          filter.doFilter(parameters, request);
        } catch (OBSecurityException e) {
          throw e;
        } catch (Exception e) {
          log.error("Error trying to apply datasource filter with class: " + filterClass, e);
        }
      }
      // now do the action
      boolean isExport = "true".equals(parameters.get("exportToFile"));
      if (isExport) {
        String exportAs = parameters.get("exportAs");
        if (StringUtils.isEmpty(exportAs)) {
          exportAs = "csv";
        }
        if ("csv".equals(exportAs)) {
          try {
            OBContext.setAdminMode();
            try {
              Window window = JsonUtils.isValueEmpty(parameters.get(JsonConstants.TAB_PARAMETER))
                  ? null
                  : OBDal.getInstance()
                      .get(Tab.class, parameters.get(JsonConstants.TAB_PARAMETER))
                      .getWindow();
              String encoding = Preferences.getPreferenceValue("OBSERDS_CSVTextEncoding", true,
                  OBContext.getOBContext().getCurrentClient(),
                  OBContext.getOBContext().getCurrentOrganization(),
                  OBContext.getOBContext().getUser(), OBContext.getOBContext().getRole(), window);
              response.setContentType("text/csv; charset=" + encoding);
            } catch (PropertyNotFoundException e) {
              // There is no preference for encoding, using standard one which works on Excel
              response.setContentType("text/csv; charset=iso-8859-1");
            }
          } finally {
            OBContext.restorePreviousMode();
          }
          response.setHeader("Content-Disposition", "attachment; filename=ExportedData.csv");
          VariablesSecureApp vars = new VariablesSecureApp(request);
          JSONWriterToCSV writer;
          if (getDataSource(request) instanceof DefaultDataSourceService) {
            writer = new JSONWriterToCSV(response.getWriter(), vars, parameters,
                getDataSourceEntity(request, parameters));
            // when exporting a OB grid, the isActive filter should not be set
            parameters.put(JsonConstants.NO_ACTIVE_FILTER, "true");

            // set selected properties those that will finally be exported
            parameters.put(JsonConstants.SELECTEDPROPERTIES_PARAMETER,
                StringCollectionUtils.commaSeparated(writer.getFieldProperties(), false));
            ((DefaultDataSourceService) getDataSource(request)).fetch(parameters, writer);
          } else {
            String result = getDataSource(request).fetch(parameters);
            JSONObject jsonResult = new JSONObject(result);
            JSONArray data = jsonResult.getJSONObject("response").getJSONArray("data");
            writer = new JSONWriterToCSV(response.getWriter(), vars, parameters,
                getDataSourceEntity(request, parameters));
            for (int i = 0; i < data.length(); i++) {
              writer.write(data.getJSONObject(i));
            }
          }

          writer.writeCSVFooterNote(parameters);
        } else {
          log.error("Unsupported export format: " + exportAs);
        }
      } else {
        // Check security: continue only if the entity is accessible for current user/role.
        getDataSource(request).checkFetchDatasourceAccess(parameters);
        String result = getDataSource(request).fetch(parameters);
        writeResult(response, result);
      }
    } catch (Exception e) {
      handleException(e, response);
    }
  }

  private Entity getDataSourceEntity(HttpServletRequest request, Map<String, String> parameters) {
    Entity entity = getDataSource(request).getEntity();
    if (entity == null && !JsonUtils.isValueEmpty(parameters.get("tabId"))) {
      Tab tab = cachedStructures.getTab(parameters.get("tabId"));
      if (tab.getTable() != null) {
        return ModelProvider.getInstance().getEntityByTableId(tab.getTable().getId());
      } else {
        log.error("Could not find the table for the tab with id: " + tab.getId());
      }
    }
    return entity;
  }

  private void handleException(Exception e, HttpServletResponse response) throws IOException {
    if (!(e instanceof OBException && ((OBException) e).isLogExceptionNeeded())) {
      log4j.error(e.getMessage(), e);
    }
    if (e instanceof SQLGrammarException) {
      log.error(((SQLGrammarException) e).getSQL());
    }
    if (!response.isCommitted()) {
      final JSONObject jsonResult = new JSONObject();
      final JSONObject jsonResponse = new JSONObject();
      String result = "";
      try {
        jsonResponse.put(JsonConstants.RESPONSE_STATUS,
            JsonConstants.RPCREQUEST_STATUS_VALIDATION_ERROR);
        jsonResponse.put("error", KernelUtils.getInstance().createErrorJSON(e));
        jsonResult.put(JsonConstants.RESPONSE_RESPONSE, jsonResponse);
        result = jsonResult.toString();
      } catch (JSONException e1) {
        log.error("Error genearating JSON error", e1);
      }
      writeResult(response, result);
    }
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    final Map<String, String> parameters = getParameterMap(request);

    try {
      if (DataSourceConstants.FETCH_OPERATION
          .equals(parameters.get(DataSourceConstants.OPERATION_TYPE_PARAM))) {
        doFetch(request, response, parameters);
        return;
      }

      if (!hasAccess(request, parameters.get(JsonConstants.TAB_PARAMETER))) {
        throw new OBUserException("AccessTableNoView");
      }

      String content = getRequestContent(request);

      CsrfUtil.checkCsrfToken(CsrfUtil.getCsrfTokenFromRequestContent(content), request);

      // note if clause updates parameter map
      if (checkSetIDDataSourceName(request, response, parameters)) {
        parameters.put(DataSourceConstants.ADD_CONTENT_OPERATION, content);
        getDataSource(request).checkEditDatasourceAccess(parameters);
        final String result = getDataSource(request).add(parameters, content);
        writeResult(response, result);
      }
    } catch (Exception e) {
      handleException(e, response);
    }
  }

  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    final Map<String, String> parameters = getParameterMap(request);
    try {
      // checks and set parameters, if not valid then go away
      if (!checkSetParameters(request, response, parameters)) {
        return;
      }
      if (!hasAccess(request, parameters.get(JsonConstants.TAB_PARAMETER))) {
        throw new OBUserException("AccessTableNoView");
      }

      CsrfUtil.checkCsrfToken(parameters.get(JsonConstants.CSRF_TOKEN_PARAMETER), request);

      final String id = parameters.get(JsonConstants.ID);
      if (id == null) {
        throw new InvalidRequestException("No id parameter");
      }

      getDataSource(request).checkEditDatasourceAccess(parameters);
      final String result = getDataSource(request).remove(parameters);
      writeResult(response, result);
    } catch (Exception e) {
      handleException(e, response);
    }
  }

  private String getDataSourceNameFromRequest(HttpServletRequest request) {
    final String url = request.getRequestURI();
    if (url.indexOf(getServletPathPart()) == -1) {
      throw new OBException("Request url " + url + " is not valid");
    }
    final int startIndex = 1 + url.indexOf(getServletPathPart()) + getServletPathPart().length();
    final int endIndex = url.indexOf("/", startIndex + 1);
    final String dsName = (endIndex == -1 ? url.substring(startIndex)
        : url.substring(startIndex, endIndex));

    if (dsName.length() == 0) {
      throw new ResourceNotFoundException("Data source not found using url " + url);
    }
    return dsName;
  }

  @Override
  public void doPut(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    final Map<String, String> parameters = getParameterMap(request);
    try {
      if (!hasAccess(request, parameters.get(JsonConstants.TAB_PARAMETER))) {
        throw new OBUserException("AccessTableNoView");
      }

      String requestContent = getRequestContent(request);

      CsrfUtil.checkCsrfToken(CsrfUtil.getCsrfTokenFromRequestContent(requestContent), request);

      // note if clause updates parameter map
      if (checkSetIDDataSourceName(request, response, parameters)) {
        getDataSource(request).checkEditDatasourceAccess(parameters);
        final String result = getDataSource(request).update(parameters, requestContent);
        writeResult(response, result);
      }

    } catch (Exception e) {
      handleException(e, response);
    }
  }

  private boolean checkSetParameters(HttpServletRequest request, HttpServletResponse response,
      Map<String, String> parameters) throws IOException {
    if (!request.getRequestURI().contains("/" + servletPathPart)) {
      writeResult(response, JsonUtils.convertExceptionToJson(new InvalidRequestException(
          "Invalid url, the path should contain the service name: " + servletPathPart)));
      return false;
    }
    final int nameIndex = request.getRequestURI().indexOf(servletPathPart);
    final String servicePart = request.getRequestURI().substring(nameIndex);
    final String[] pathParts = WebServiceUtil.getInstance().getSegments(servicePart);
    if (pathParts.length == 0 || !pathParts[0].equals(servletPathPart)) {
      writeResult(response, JsonUtils.convertExceptionToJson(
          new InvalidRequestException("Invalid url: " + request.getRequestURI())));
      return false;
    }
    if (pathParts.length == 1) {
      writeResult(response, JsonUtils.convertExceptionToJson(new InvalidRequestException(
          "Invalid url, no datasource name: " + request.getRequestURI())));
      return false;
    }
    final String dsName = pathParts[1];
    parameters.put(DataSourceConstants.DS_NAME_PARAM, dsName);
    if (pathParts.length > 2) {
      // search on the exact id
      parameters.put(JsonConstants.ID, pathParts[2]);
      if (!parameters.containsKey(JsonConstants.TEXTMATCH_PARAMETER)) {
        parameters.put(JsonConstants.TEXTMATCH_PARAMETER, JsonConstants.TEXTMATCH_EXACT);
        parameters.put(JsonConstants.TEXTMATCH_PARAMETER_OVERRIDE, JsonConstants.TEXTMATCH_EXACT);
      }
    }
    return true;
  }

  private Map<String, String> getParameterMap(HttpServletRequest request) {
    final Map<String, String> parameterMap = new HashMap<String, String>();
    for (@SuppressWarnings("rawtypes")
    Enumeration keys = request.getParameterNames(); keys.hasMoreElements();) {
      final String key = (String) keys.nextElement();

      // do simple conversion of array of values to a string
      // TODO: replace when advancedcriteria are supported
      final String[] values = request.getParameterValues(key);
      if (values.length == 1) {
        parameterMap.put(key, values[0]);
      } else {
        final StringBuilder sb = new StringBuilder();
        for (String value : values) {
          if (sb.length() > 0) {
            sb.append(JsonConstants.IN_PARAMETER_SEPARATOR);
          }
          sb.append(value);
        }
        parameterMap.put(key, sb.toString());
      }
    }
    return parameterMap;
  }

  // NOTE: parameters parameter is updated inside this method
  private boolean checkSetIDDataSourceName(HttpServletRequest request, HttpServletResponse response,
      Map<String, String> parameters) throws IOException {
    if (!request.getRequestURI().contains("/" + servletPathPart)) {
      writeResult(response, JsonUtils.convertExceptionToJson(new InvalidRequestException(
          "Invalid url, the path should contain the service name: " + servletPathPart)));
      return false;
    }
    final int nameIndex = request.getRequestURI().indexOf(servletPathPart);
    final String servicePart = request.getRequestURI().substring(nameIndex);
    final String[] pathParts = WebServiceUtil.getInstance().getSegments(servicePart);
    if (pathParts.length == 0 || !pathParts[0].equals(servletPathPart)) {
      writeResult(response, JsonUtils.convertExceptionToJson(
          new InvalidRequestException("Invalid url: " + request.getRequestURI())));
      return false;
    }
    if (pathParts.length == 1) {
      return true;
    }

    final String dsName = pathParts[1];
    parameters.put(DataSourceConstants.DS_NAME_PARAM, dsName);

    if (pathParts.length > 2) {
      // search on the exact id
      parameters.put(JsonConstants.ID, pathParts[2]);
      if (!parameters.containsKey(JsonConstants.TEXTMATCH_PARAMETER)) {
        parameters.put(JsonConstants.TEXTMATCH_PARAMETER, JsonConstants.TEXTMATCH_EXACT);
        parameters.put(JsonConstants.TEXTMATCH_PARAMETER_OVERRIDE, JsonConstants.TEXTMATCH_EXACT);
      }
    }
    return true;
  }

  private DataSourceService getDataSource(HttpServletRequest request) {
    final String dsName = getDataSourceNameFromRequest(request);
    final DataSourceService dataSource = dataSourceServiceProvider.getDataSource(dsName);
    return dataSource;
  }

  private void writeResult(HttpServletResponse response, String result) throws IOException {
    response.setContentType(JsonConstants.JSON_CONTENT_TYPE);
    response.setHeader("Content-Type", JsonConstants.JSON_CONTENT_TYPE);

    final Writer w = response.getWriter();
    w.write(result);
    w.close();
  }

  private String getRequestContent(HttpServletRequest request) throws IOException {
    final BufferedReader reader = request.getReader();
    if (reader == null) {
      return "";
    }
    String line;
    final StringBuilder sb = new StringBuilder();
    while ((line = reader.readLine()) != null) {
      if (sb.length() > 0) {
        sb.append("\n");
      }
      sb.append(line);
    }
    log.debug("REQUEST CONTENT>>>>");
    for (Enumeration<?> enumeration = request.getParameterNames(); enumeration.hasMoreElements();) {
      final Object key = enumeration.nextElement();
      log.debug(key + ": " + request.getParameter((String) key));
    }
    return sb.toString();
  }

  /**
   * Checks access to the current tab, it reuses infrastructure in HttpSecureAppServlet
   */
  private boolean hasAccess(HttpServletRequest req, String tabId) {
    try {
      if (JsonUtils.isValueEmpty(tabId)) {
        return true;
      }
      VariablesSecureApp vars = new VariablesSecureApp(req);
      boolean hasAccess = hasGeneralAccess(vars, "W", tabId);
      if (hasAccess) {
        return true;
      }
      // Here is checked process definition that containing parameters defined as "window",
      // automatically inherit permissions. See issue #29035
      try {
        OBContext.setAdminMode(true);
        Tab tab = OBDal.getInstance().get(Tab.class, tabId);
        if (tab == null) {
          return false;
        }
        // Search window references that linked with the window of the provided tab.
        OBCriteria<RefWindow> obcRefWindow = OBDal.getInstance().createCriteria(RefWindow.class);
        obcRefWindow.add(Restrictions.eq(RefWindow.PROPERTY_WINDOW, tab.getWindow()));
        if (obcRefWindow.list().size() == 0) {
          return false;
        }
        final List<Reference> references = new ArrayList<Reference>();
        for (RefWindow refWindow : obcRefWindow.list()) {
          references.add(refWindow.getReference());
        }

        // Then search parameters that linked with references and get theirs processes.
        OBCriteria<Parameter> obParameters = OBDal.getInstance().createCriteria(Parameter.class);
        obParameters.add(Restrictions.in(Parameter.PROPERTY_REFERENCESEARCHKEY, references));
        if (obParameters.list().size() == 0) {
          return false;
        }
        final List<Process> obuiapProcesses = new ArrayList<Process>();
        for (Parameter parameter : obParameters.list()) {
          obuiapProcesses.add(parameter.getObuiappProcess());

        }

        // If access to process are granted for current user/role return directly true.
        boolean isAccessGranted = true;
        final OBContext obContext = OBContext.getOBContext();
        for (Process process : obuiapProcesses) {
          if (!obContext.getEntityAccessChecker().checkProcessAccess(process.getId())) {
            isAccessGranted = false;
          }
        }
        if (isAccessGranted) {
          return true;
        }
        Role role = OBContext.getOBContext().getRole();
        if (!role.isManual()) {
          return true;
        }
        // Finally select all columns that linked with selected processes and get their fields.
        OBCriteria<Column> columns = OBDal.getInstance().createCriteria(Column.class);
        columns.add(Restrictions.in(Column.PROPERTY_OBUIAPPPROCESS, obuiapProcesses));
        if (columns.list().size() == 0) {
          return false;
        }
        final List<Field> fields = new ArrayList<Field>();
        for (Column col : columns.list()) {
          for (Field field : col.getADFieldList()) {
            fields.add(field);
          }
        }

        Map<String, Object> parameters = new HashMap<String, Object>();
        // Avoid to invoke hasAccess() method with same window.
        final List<String> windowCheck = new ArrayList<String>();
        for (Field f : fields) {
          // Check access with OBUIAPPProcess & windowId for each field.
          String windowId = f.getTab().getWindow().getId();
          if (!windowCheck.contains(windowId)) {
            windowCheck.add(windowId);
            parameters.put("windowId", windowId);
            boolean hasAccessEntity = BaseProcessActionHandler
                .hasAccess(f.getColumn().getOBUIAPPProcess(), parameters);
            if (hasAccessEntity) {
              return true;
            }
          }
        }
        // here there is no access to current tabId
        return false;
      } catch (final Exception e) {
        log4j.error("Error checking access: ", e);
        return false;
      } finally {
        OBContext.restorePreviousMode();
      }
    } catch (final Exception e) {
      log4j.error("Error checking access: ", e);
      return false;
    }
  }
}
