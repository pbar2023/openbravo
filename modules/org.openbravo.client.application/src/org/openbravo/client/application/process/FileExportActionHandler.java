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
 * All portions are Copyright (C) 2023 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.application.process;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.attachment.AttachImplementationManager;
import org.openbravo.client.application.process.ResponseActionsBuilder.MessageType;
import org.openbravo.client.application.report.ReportingUtils;
import org.openbravo.client.application.report.ReportingUtils.ExportType;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.database.SessionInfo;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.db.DbUtility;
import org.openbravo.utils.FileUtility;

/**
 * Base action handler that provides the ability of generating and downloading a file.
 */
public abstract class FileExportActionHandler extends BaseProcessActionHandler {
  private static final Logger log = LogManager.getLogger();

  protected enum FileAction {
    BROWSE("OBUIAPP_browseReport"), DOWNLOAD("OBUIAPP_downloadReport");

    private final String actionName;

    private FileAction(String actionName) {
      this.actionName = actionName;
    }
  }

  /**
   * execute() method overridden to add the logic to download or display the file stored in the
   * temporary folder.
   */
  @Override
  public void execute() {
    final HttpServletRequest request = RequestContext.get().getRequest();
    String mode = request.getParameter("mode");
    if (mode == null) {
      mode = "Default";
    }
    if ("DOWNLOAD".equals(mode)) {
      try {
        doDownload(request);
      } catch (Exception e) {
        // Error downloading file
        log.error("Error downloading the file: {}", e.getMessage(), e);
      }
      return;
    } else if ("BROWSE".equals(mode)) {
      try {
        doBrowse(request);
      } catch (IOException e) {
        log.error("Error browsing the file: {}", e.getMessage(), e);
      }
      return;
    }
    SessionInfo.auditThisThread(false);
    super.execute();
  }

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    try {
      final JSONObject data = new JSONObject(content);
      return generateFile(parameters, data);
    } catch (Exception e) {
      log.error("Error generating file, request content: {}", content, e);
      Throwable uiException = DbUtility.getUnderlyingSQLException(e);
      return getResponseBuilder().retryExecution()
          .showResultsInProcessView()
          .showMsgInProcessView(MessageType.ERROR,
              OBMessageUtils.translateError(uiException.getMessage()).getMessage())
          .build();
    }
  }

  private JSONObject generateFile(Map<String, Object> parameters, JSONObject data)
      throws IOException, JSONException {
    doValidations(parameters, data);
    Path path = doGenerateFile(parameters, data);
    return createResponseForDownload(parameters, data, path);
  }

  protected Path doGenerateFile(Map<String, Object> parameters, JSONObject data)
      throws IOException, JSONException {
    Path path = generateFileToDownload(parameters, data);
    uploadAttachment(path, parameters, data);
    return path;
  }

  /**
   * Allows to do verifications in the data
   * 
   * @param parameters
   *          The map of parameters extracted from the request
   * @param data
   *          JSONObject with the request content
   */
  protected void doValidations(Map<String, Object> parameters, JSONObject data) {

  }

  /**
   * This method that allows possible verifications in the data
   * 
   * @param parameters
   *          The map of parameters extracted from the request
   * @param data
   *          JSONObject with the request content
   */
  protected abstract Path generateFileToDownload(Map<String, Object> parameters, JSONObject data)
      throws IOException, JSONException;

  /**
   * Retrieves the name of the file to be downloaded
   * 
   * @param parameters
   *          The map of parameters extracted from the request
   * @param data
   *          JSONObject with the request content
   * 
   * @return the name of the file to be downloaded
   */
  protected abstract String getDownloadFileName(Map<String, Object> parameters, JSONObject data)
      throws JSONException;

  /**
   * Returns the file output. The file containing the output is stored in a temporary folder with a
   * generated name. Its content is sent back to the browser to be shown in a new Openbravo tab.
   * Once the process is finished the file is removed from the server.
   *
   * @param request
   *          The HTTP request for this handler.
   */
  private void doBrowse(HttpServletRequest request) throws IOException {
    final Map<String, Object> parameters = getParameterMapFromRequest(request);
    final String strFileName = (String) parameters.get("fileName");
    final String tmpFileName = (String) parameters.get("tmpfileName");
    ExportType expType = ExportType.HTML;

    handleFileResponse(expType, strFileName, tmpFileName, false);
  }

  private void handleFileResponse(ExportType expType, String strFileName, String tmpFileName,
      boolean includeFileAsAttachment) throws IOException {
    if (!expType.isValidTemporaryFileName(tmpFileName)) {
      throw new IllegalArgumentException("Trying to download file " + strFileName
          + " using an invalid tmp file name: " + tmpFileName);
    }
    final String tmpDirectory = ReportingUtils.getTempFolder();
    final File file = new File(tmpDirectory, tmpFileName);
    final HttpServletResponse response = RequestContext.get().getResponse();
    response.setHeader("Content-Type", expType.getContentType());
    response.setContentType(expType.getContentType());
    response.setCharacterEncoding("UTF-8");
    if (includeFileAsAttachment) {
      response.setHeader("Content-Disposition",
          "attachment; filename=\"" + MimeUtility.encodeWord(strFileName, "utf-8", "Q") + "\"");
    }
    try (OutputStream os = response.getOutputStream()) {
      FileUtility fileUtil = new FileUtility(tmpDirectory, tmpFileName, false, true);
      fileUtil.dumpFile(os);
      response.getOutputStream().flush();
    } finally {
      Files.deleteIfExists(file.toPath());
    }
  }

  /**
   * Downloads the file with the generated result. The file is stored in a temporary folder with a
   * generated name. It is renamed and download as an attachment of the HTTP response. Once it is
   * finished the file is removed from the server.
   *
   * @param request
   *          The HTTP request for this handler.
   */
  private void doDownload(HttpServletRequest request) throws IOException {
    final Map<String, Object> parameters = getParameterMapFromRequest(request);
    final String strFileName = (String) parameters.get("fileName");
    final String tmpFileName = (String) parameters.get("tmpfileName");
    ExportType expType = Stream.of(ExportType.values())
        .filter(ext -> strFileName.endsWith("." + ext.getExtension()))
        .filter(this::isValidDownloadFileType)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Trying to download file with unsupported type " + strFileName));
    handleFileResponse(expType, strFileName, tmpFileName, true);
  }

  /**
   * @return true if the handler supports downloading files with the give type. Otherwise false is
   *         returned.
   */
  protected boolean isValidDownloadFileType(ExportType exportType) {
    return exportType != null;
  }

  private Map<String, Object> getParameterMapFromRequest(HttpServletRequest request) {
    Map<String, Object> parameterMap = new HashMap<>();
    for (Enumeration<?> keys = request.getParameterNames(); keys.hasMoreElements();) {
      String key = (String) keys.nextElement();
      if (request.getParameterValues(key) != null && request.getParameterValues(key).length > 1) {
        parameterMap.put(key, request.getParameterValues(key));
      } else {
        parameterMap.put(key, request.getParameter(key));
      }
    }
    parameterMap.put(KernelConstants.HTTP_SESSION, request.getSession(false));
    parameterMap.put(KernelConstants.HTTP_REQUEST, request);
    return parameterMap;
  }

  /**
   * Generates the response to call to the file action (OBUIAPP_downloadReport or
   * OBUIAPP_browseReport). This will generate a second call to finish the process by downloading or
   * browsing the file.
   * 
   * @param parameters
   *          The map of parameters extracted from the request
   * @param data
   *          JSONObject with the request content
   * @param path
   *          The path of the file to be downloaded
   * 
   * @return a JSONObject with the response containing the information to build the file download
   *         request from the client side
   * @throws JSONException
   */
  private JSONObject createResponseForDownload(Map<String, Object> parameters, JSONObject data,
      Path path) throws JSONException {
    JSONObject result = getAdditionalResponseActions();

    JSONObject processParams = data.getJSONObject("_params");
    processParams.put("processId", parameters.get("processId"));
    processParams.put("actionHandler", this.getClass().getName());
    addAditionalParameters(parameters, processParams);

    JSONObject recordInfo = new JSONObject();
    recordInfo.put("processParameters", processParams);
    recordInfo.put("tmpfileName", path.toFile().getName());
    recordInfo.put("fileName", getDownloadFileName(parameters, data));
    addAditionalRecordInfo(parameters, recordInfo);

    final JSONObject downloadAction = new JSONObject();
    downloadAction.put(getDownloadAction(data).actionName, recordInfo);

    JSONArray actions = new JSONArray();
    actions.put(0, downloadAction);

    result.put("responseActions", actions);
    return result;
  }

  /**
   * @return a JSONObject containing the additional response actions to be done together with the
   *         file action defined by the process
   */
  protected JSONObject getAdditionalResponseActions() {
    return new JSONObject();
  }

  /**
   * Allows to define the action to be done with the file when it is downloaded. It returns
   * {@link FileAction#DOWNLOAD} by default.
   * 
   * @param data
   *          JSONObject with the request content
   * 
   * @return the action to be done when the file is downloaded
   */
  protected FileAction getDownloadAction(JSONObject data) throws JSONException {
    return FileAction.DOWNLOAD;
  }

  /**
   * Allows to include additional record information in the result built with
   * {@link #createResponseForDownload}
   * 
   * @param parameters
   *          The map of parameters extracted from the request
   * @param recordInfo
   *          JSONObject with the record information. This method is allowed to mutate this object
   *          by adding properties into it.
   */
  protected void addAditionalRecordInfo(Map<String, Object> parameters, JSONObject recordInfo)
      throws JSONException {

  }

  /**
   * Allows to include additional process parameters in the result built with
   * {@link #createResponseForDownload}
   * 
   * @param parameters
   *          The map of parameters extracted from the request
   * @param processParameters
   *          JSONObject with information of the process itself. This method is allowed to mutate
   *          this object by adding properties into it.
   */
  protected void addAditionalParameters(Map<String, Object> parameters,
      JSONObject processParameters) throws JSONException {

  }

  /**
   * Uploads the downloaded file as an attachment. By default, it only attaches the file when the
   * process has been executed for a single record. So the attach is not done in a multiRecord
   * process definition or when process definition is executed directly from a menu entry.
   * Subclasses are allowed to override this method in case it is desired to change this default
   * behavior or not to attach any file at all.
   *
   * @param originalFile
   *          The file whose content is attached
   * @param parameters
   *          The map of parameters extracted from the request
   * @param data
   *          JSONObject with the request content. Used to extract required information for creating
   *          the attachment like the record ID.
   */
  protected void uploadAttachment(Path originalFile, Map<String, Object> parameters,
      JSONObject data) throws IOException, JSONException {
    String recordId = data.optString(data.optString("inpKeyName"));
    if (!StringUtils.isBlank(recordId)) {
      String attachFileName = getDownloadFileName(parameters, data);
      Path attachedFile = Paths.get(ReportingUtils.getTempFolder(), attachFileName);
      Files.copy(originalFile, attachedFile, StandardCopyOption.REPLACE_EXISTING);
      String tabId = data.optString("inpTabId");
      String orgId = data.optString("inpadOrgId");
      AttachImplementationManager aim = WeldUtils
          .getInstanceFromStaticBeanManager(AttachImplementationManager.class);
      aim.upload(Collections.emptyMap(), tabId, recordId, orgId, attachedFile.toFile());
    }
  }
}
