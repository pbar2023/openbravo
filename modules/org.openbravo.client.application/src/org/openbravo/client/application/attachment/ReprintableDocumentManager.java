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
 * All portions are Copyright (C) 2023-2024 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.attachment;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.OrganizationEnabled;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.cache.TimeInvalidatedCache;
import org.openbravo.client.application.attachment.AttachmentUtils.AttachmentType;
import org.openbravo.client.application.attachment.ReprintableSourceDocument.DocumentType;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.SecurityChecker;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.utility.AttachmentConfig;
import org.openbravo.model.ad.utility.ReprintableDocument;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.synchronization.event.SynchronizationEvent;

/**
 * Centralizes the {@link ReprintableDocument} Management. Any action to manage reprintable
 * documents in Openbravo should be done through this class.
 */
@ApplicationScoped
public class ReprintableDocumentManager {
  private static final Logger log = LogManager.getLogger();
  // Sales Order, Sales Invoice, Purchase Order, Purchase Invoice, Substitute Invoice and Return
  // from Customer windows
  private static final Set<String> WINDOWS_WITH_REPRINT = Set.of("143", "167", "181", "183",
      "6916326417DB4A6FBD07870C0884E569", "FF808081330213E60133021822E40007");

  private TimeInvalidatedCache<String, String> methodsOfAttachmentConfigs;
  private TimeInvalidatedCache<String, Boolean> reprintDocumentsConfiguration;

  /**
   * Supported formats for a {@link ReprintableDocument}
   */
  public enum Format {
    XML, PDF;
  }

  @PostConstruct
  private void init() {
    methodsOfAttachmentConfigs = TimeInvalidatedCache.newBuilder()
        .name("Methods of Attachment Configs")
        .expireAfterDuration(Duration.ofMinutes(10))
        .build(this::getAttachmentMethod);
    reprintDocumentsConfiguration = TimeInvalidatedCache.newBuilder()
        .name("Reprint Document Org Configs")
        .expireAfterDuration(Duration.ofMinutes(10))
        .build(this::getIsReprintDocumentEnabled);
  }

  private String getAttachmentMethod(String attachmentConfigurationId) {
    //@formatter:off
    String hql = "select c.attachmentMethod.value" +
                 "  from AttachmentConfig c" +
                 " where c.id = :id";
    //@formatter:on
    return OBDal.getInstance()
        .getSession()
        .createQuery(hql, String.class)
        .setParameter("id", attachmentConfigurationId)
        .uniqueResult();
  }

  private Boolean getIsReprintDocumentEnabled(String orgId) {
    return OBContext.getOBContext()
        .getOrganizationStructureProvider(OBContext.getOBContext().getCurrentClient().getId())
        .getParentList(orgId, true)
        .stream()
        .map(org -> OBDal.getInstance().get(Organization.class, org).getReprintDocuments())
        .filter(setting -> !StringUtils.isBlank(setting))
        .findFirst() // note that getParentList returns the parent organizations ordered by distance
                     // from the orgId node, from closest to farthest. So following that order, we
                     // retrieve the configuration status from the first organization that enables
                     // or disables the feature (if any).
        .map("enabled"::equals)
        .orElse(false);
  }

  /**
   * Creates a new ReprintableDocument and uploads its data as an attachment using the attachment
   * method of the configuration for "Reprintable Documents" defined for the current session client.
   * If such configuration does not exist for the current client then the one defined at system
   * level is used. And if no attachment configuration is found at all, then the {#link
   * AttachmentUtils#DEFAULT_METHOD} method is used by default.
   *
   * Important Note: this method flushes the current transaction when creating the
   * ReprintableDocument record in order to trigger the unique constraint checks because in case the
   * record cannot be saved, this method should not continue doing the attachment upload in order to
   * avoid creating attachments not linked to any record.
   *
   * @param document
   *          A JSONObject containing the properties documentData (string containing the report),
   *          documentId (the ticket id), documentType (ORDER or INVOICE) and documentFormat (XML or
   *          PDF).
   *
   * @return the newly created ReprintableDocument
   *
   * @throws JSONException
   *           if it is not possible to find a required property inside the document parameter
   */
  public ReprintableDocument upload(JSONObject document) throws JSONException {
    String documentData = document.getString("documentData");
    String documentId = document.getString("documentId");
    String documentType = document.getString("documentType");
    String documentFormat = document.getString("documentFormat");

    Format format = Format.valueOf(documentFormat.toUpperCase());
    InputStream inputStream = Format.PDF.equals(format)
        ? new ByteArrayInputStream(Base64.getDecoder().decode(documentData))
        : new ByteArrayInputStream(documentData.getBytes(StandardCharsets.UTF_8));
    DocumentType type = DocumentType.valueOf(documentType.toUpperCase());
    ReprintableSourceDocument<?> sourceDocument = ReprintableSourceDocument
        .newSourceDocument(documentId, type);

    return upload(inputStream, format, sourceDocument);
  }

  /**
   * Creates a new ReprintableDocument and uploads its data as an attachment using the attachment
   * method of the configuration for "Reprintable Documents" defined for the current session client.
   * If such configuration does not exist for the current client then the one defined at system
   * level is used. And if no attachment configuration is found at all, then the {#link
   * AttachmentUtils#DEFAULT_METHOD} method is used by default.
   *
   * Important Note: this method flushes the current transaction when creating the
   * ReprintableDocument record in order to trigger the unique constraint checks because in case the
   * record cannot be saved, this method should not continue doing the attachment upload in order to
   * avoid creating attachments not linked to any record.
   *
   * @param documentData
   *          An InputStream with the document data. This method is in charge of closing it when
   *          finish its execution.
   * @param format
   *          The format of document
   * @param sourceDocument
   *          The document used as source data for the ReprintableDocument
   *
   * @return the newly created ReprintableDocument
   *
   * @throws OBSecurityException
   *           if the write access to the source document is not granted in the current context
   *           because in such case is not allowed to create a ReprintableDocument linked to the
   *           source document.
   * @throws OBException
   *           if it is not possible to find a handler for the selected attachment method
   */
  public ReprintableDocument upload(InputStream documentData, Format format,
      ReprintableSourceDocument<?> sourceDocument) {
    long init = System.currentTimeMillis();
    ReprintableDocument document = createReprintableDocument(format, sourceDocument);

    ReprintableDocumentAttachHandler handler = getHandler(document);
    try (documentData) {
      handler.upload(document, documentData);
    } catch (Exception ex) {
      throw new OBException("Error uploading reprintable document", ex);
    }

    log.trace("Reprintable document {} uploaded in {} ms", document.getId(),
        System.currentTimeMillis() - init);

    triggerUploadEvent(sourceDocument);

    return document;
  }

  private void triggerUploadEvent(ReprintableSourceDocument<?> sourceDocument) {
    sourceDocument.getUploadEvent()
        .ifPresent(event -> SynchronizationEvent.getInstance()
            .triggerEvent(event, sourceDocument.getId()));
  }

  /**
   * Retrieves the data of a ReprintableDocument linked to the provided document
   *
   * @param sourceDocument
   *          The document used as source data for the ReprintableDocument
   *
   * @param outputStream
   *          outputStream where document data is provided. Code invoking this method is also
   *          responsible of closing it.
   *
   * @return the ReprintableDocument linked to the source document
   *
   * @throws DocumentNotFoundException
   *           if it is not possible to find the ReprintableDocument linked to the provided source
   *           document
   * @throws OBSecurityException
   *           if the read access to the source document is not granted in the current context
   *           because in such case is not allowed to access to the ReprintableDocument linked to
   *           the source document.
   * @throws OBException
   *           if it is not possible to find a handler for the attachment method defined in the
   *           ReprintableDocument attachment configuration
   */
  public ReprintableDocument download(ReprintableSourceDocument<?> sourceDocument,
      OutputStream outputStream) throws IOException, DocumentNotFoundException {
    ReprintableDocument reprintableDocument = findReprintableDocument(sourceDocument);
    download(reprintableDocument, outputStream);
    return reprintableDocument;
  }

  /**
   * Retrieves the data of a ReprintableDocument linked to the provided document in the specified
   * format. If the format of the document linked to the given source document is different from the
   * given format, then the document is tried to be transformed using a
   * {@link ReprintableDocumentTransformer}. Otherwise the document is directly retrieved with its
   * actual format.
   * 
   * @param sourceDocument
   *          The document used as source data for the ReprintableDocument
   * @param outputStream
   *          outputStream where document data is provided. Code invoking this method is also
   *          responsible of closing it.
   * @param format
   *          the format of the document to be downloaded
   * 
   * @return the ReprintableDocument linked to the source document
   *
   * @throws DocumentNotFoundException
   *           if it is not possible to find the ReprintableDocument linked to the provided source
   *           document
   * @throws OBSecurityException
   *           if the read access to the source document is not granted in the current context
   *           because in such case is not allowed to access to the ReprintableDocument linked to
   *           the source document.
   * @throws OBException
   *           if it is not possible to find a handler for the attachment method defined in the
   *           ReprintableDocument attachment configuration or if the document transformation fails
   * @throws TransformerNotFoundException
   *           if the document needs to be transformed and there is no
   *           {@link ReprintableDocumentTransformer} instance that can be used to do the
   *           transformation into the given format
   */
  public ReprintableDocument download(ReprintableSourceDocument<?> sourceDocument,
      OutputStream outputStream, Format format)
      throws IOException, DocumentNotFoundException, TransformerNotFoundException {
    ReprintableDocument reprintableDocument = findReprintableDocument(sourceDocument);
    return download(reprintableDocument, outputStream, format);
  }

  /**
   * Retrieves the data of a given ReprintableDocument in the specified format. If the format of the
   * provided ReprintableDocument is different from the given format, the method attempts to
   * transform the document using a {@link ReprintableDocumentTransformer}. If the formats are the
   * same, the document is directly retrieved in its actual format.
   *
   * @param reprintableDocument
   *          The ReprintableDocument to download or to transform if needed
   * @param outputStream
   *          The outputStream where the document data is written. The code invoking this method is
   *          also responsible for closing it.
   * @param format
   *          The format in which the document is to be downloaded
   *
   * @return The provided ReprintableDocument
   *
   * @throws DocumentNotFoundException
   *           If it is not possible to find the ReprintableDocument linked to the provided source
   *           document
   * @throws OBSecurityException
   *           If the read access to the source document is not granted in the current context
   *           because in such case it is not allowed to access the ReprintableDocument linked to
   *           the source document.
   * @throws OBException
   *           If it is not possible to find a handler for the attachment method defined in the
   *           ReprintableDocument attachment configuration or if the document transformation fails
   * @throws TransformerNotFoundException
   *           If the document needs to be transformed and there is no
   *           {@link ReprintableDocumentTransformer} instance that can be used to perform the
   *           transformation into the given format
   */
  public ReprintableDocument download(ReprintableDocument reprintableDocument,
      OutputStream outputStream, Format format)
      throws IOException, DocumentNotFoundException, TransformerNotFoundException {
    return processDownload(reprintableDocument, outputStream, format);
  }

  private ReprintableDocument processDownload(ReprintableDocument reprintableDocument,
      OutputStream outputStream, Format format)
      throws IOException, DocumentNotFoundException, TransformerNotFoundException {
    if (reprintableDocument.getFormat().equalsIgnoreCase(format.name())) {
      download(reprintableDocument, outputStream);
    } else {
      Optional<ReprintableDocumentTransformer> transformer = getReprintableDocumentTransformer(
          format);
      if (transformer.isEmpty()) {
        throw new TransformerNotFoundException(
            "No ReprintableDocumentTransformer instance found to transform into " + format.name()
                + " format");
      }
      Path transformedDocument = null;
      Path originalDocument = null;
      try {
        originalDocument = download(reprintableDocument);
        transformedDocument = transformer.get()
            .transform(ReprintableSourceDocument.newSourceDocument(reprintableDocument),
                originalDocument);
        Files.copy(transformedDocument, outputStream);
      } finally {
        delete(originalDocument);
        delete(transformedDocument);
      }
    }
    return reprintableDocument;
  }

  private Path download(ReprintableDocument reprintableDocument) throws IOException {
    File document = File.createTempFile("obrd-",
        "." + reprintableDocument.getFormat().toLowerCase());
    try (OutputStream os = new FileOutputStream(document)) {
      download(reprintableDocument, os);
    }
    return document.toPath();
  }

  private void download(ReprintableDocument reprintableDocument, OutputStream outputStream)
      throws IOException {
    long init = System.currentTimeMillis();
    ReprintableDocumentAttachHandler handler = getHandler(reprintableDocument);
    handler.download(reprintableDocument, outputStream);
    log.trace("Reprintable document {} downloaded in {} ms", reprintableDocument.getId(),
        System.currentTimeMillis() - init);
  }

  private void delete(Path path) throws IOException {
    if (path != null) {
      Files.deleteIfExists(path);
    }
  }

  /**
   * Gets the {@link ReprintableDocumentTransformer} with more priority for the given format
   *
   * @param format
   *          The format to which the document is transformed
   *
   * @return the transformed with more priority for the given format
   */
  Optional<ReprintableDocumentTransformer> getReprintableDocumentTransformer(Format format) {
    List<ReprintableDocumentTransformer> hooks = WeldUtils.getInstancesSortedByPriority(
        ReprintableDocumentTransformer.class, new ReprintableDocumentFormatSelector(format));
    return hooks.isEmpty() ? Optional.empty() : Optional.of(hooks.get(0));
  }

  /**
   * @return true if it is possible to transform reprintable documents into the given format or
   *         false in any other case.
   */
  public boolean canTransformIntoFormat(Format format) {
    return getReprintableDocumentTransformer(format).isPresent();
  }

  /**
   * Checks whether documents reprinting is enabled for a given organization which is determined by
   * the value returned with {@link Organization#getReprintDocuments()}. Note that in case that
   * value is not defined (null) for the given organization, it is taken from the closest
   * organization in the parent organization tree that has a value defined (not null).
   *
   * @param orgId
   *          The ID of the organization to check
   *
   * @return true if document reprinting is enabled for the given organization or false otherwise
   */
  public boolean isReprintDocumentsEnabled(String orgId) {
    return reprintDocumentsConfiguration.get(orgId);
  }

  private ReprintableDocument createReprintableDocument(Format format,
      ReprintableSourceDocument<?> sourceDocument) {
    var sourceDocumentBOB = sourceDocument.getBaseDocument();

    SecurityChecker.getInstance().checkWriteAccess(sourceDocumentBOB);

    // If we have write access to the source document, the ReprintableDocument can be saved but we
    // need to do it in admin mode because the ReprintableDocument entity is not writable by default
    try {
      OBContext.setAdminMode(true);
      ReprintableDocument reprintableDocument = OBProvider.getInstance()
          .get(ReprintableDocument.class);
      reprintableDocument.setClient(sourceDocumentBOB.getClient());
      reprintableDocument.setOrganization(sourceDocumentBOB.getOrganization());
      reprintableDocument.setName(sourceDocument.getSafeName() + "." + format.name().toLowerCase());
      reprintableDocument.setFormat(format.name());
      reprintableDocument
          .setAttachmentConfiguration(AttachmentUtils.getAttachmentConfig(AttachmentType.RD));
      reprintableDocument.set(sourceDocument.getProperty(), sourceDocument.getBaseDocument());
      OBDal.getInstance().save(reprintableDocument);
      OBDal.getInstance().flush();
      return reprintableDocument;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Looks for a persisted reprintable document. If it exists it returns it, if not it throws a
   * DocumentNotFoundException exception.
   *
   * @param sourceDocument
   *          The reprintable document to be found
   *
   * @return the document if found or a DocumentNotFoundException otherwise
   */
  public ReprintableDocument findReprintableDocument(ReprintableSourceDocument<?> sourceDocument)
      throws DocumentNotFoundException {
    BaseOBObject bob = sourceDocument.getBaseDocument();

    SecurityChecker.getInstance().checkReadableAccess((OrganizationEnabled) bob);

    return sourceDocument.getReprintableDocument().orElseThrow(DocumentNotFoundException::new);
  }

  private ReprintableDocumentAttachHandler getHandler(ReprintableDocument reprintableDocument) {
    AttachmentConfig config = reprintableDocument.getAttachmentConfiguration();
    String attachMethod = config != null ? methodsOfAttachmentConfigs.get(config.getId())
        : AttachmentUtils.DEFAULT_METHOD;
    return WeldUtils
        .getInstances(ReprintableDocumentAttachHandler.class,
            new ComponentProvider.Selector(attachMethod))
        .stream()
        .findFirst()
        .orElseThrow(() -> new OBException(OBMessageUtils.messageBD("OBUIAPP_NoMethod")));
  }

  /**
   * Checks if reprintable documents can be generated from a given window. For the moment it is only
   * supported to generate reprintable documents from these windows: Sales Order, Sales Invoice,
   * Purchase Order, Substitute Invoice and Purchase Invoice.
   *
   * @param windowId
   *          The ID of the AD window
   *
   * @return true if reprintable documents can be generated from the given window or false in other
   *         case
   */
  public boolean isReprintableDocumentsWindow(String windowId) {
    return WINDOWS_WITH_REPRINT.contains(windowId);
  }

  /**
   * Clears the cached information of the attachment configuration passed as parameter. For internal
   * use only.
   *
   * @param attachmentConfigurationId
   *          The attachment configuration ID
   */
  void invalidateAttachmentConfigurationCache(String attachmentConfigurationId) {
    methodsOfAttachmentConfigs.invalidate(attachmentConfigurationId);
  }

  /**
   * Clears the cached information of the document reprinting configuration for a given organization
   * and for all the organizations on its natural tree. Note that when working in a cluster it is
   * only cleared the cache in the node processing the request. In the rest of the nodes, the cache
   * will be automatically invalidated some time later because the cache is a time invalidated one.
   *
   * @param organization
   *          The organization whose configuration is invalidated. The configuration of the
   *          organizations included on its natural tree is also invalidated.
   */
  void invalidateReprintDocumentConfigurationCache(Organization organization) {
    try {
      OBContext.setAdminMode(true);
      OBContext.getOBContext()
          .getOrganizationStructureProvider(organization.getClient().getId())
          .getNaturalTree(organization.getId())
          .forEach(o -> reprintDocumentsConfiguration.invalidate(o));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
