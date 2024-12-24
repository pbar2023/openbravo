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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.attachment.AttachmentUtils.AttachmentType;
import org.openbravo.client.application.attachment.ReprintableDocumentManager.Format;
import org.openbravo.client.application.attachment.ReprintableSourceDocument.DocumentType;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.AttachmentConfig;
import org.openbravo.model.ad.utility.AttachmentMethod;
import org.openbravo.model.ad.utility.ReprintableDocument;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.test.base.Issue;
import org.openbravo.test.base.TestConstants;

/**
 * Test cases to cover the reprintable document management infrastructure
 */
public class ReprintableDocumentTest extends WeldBaseTest {

  private static final String DISABLED = "disabled";
  private static final String ENABLED = "enabled";
  private static final String DATA = "<output>hello<output/>";
  private static final String TRANSFORMED_DATA = "hello";
  private static final String INVALID_CONFIG_MSG = "The attachment type is not supported by the selected attachment method";

  @Inject
  private ReprintableDocumentManager reprintableDocumentManager;

  @After
  public void cleanUp() {
    resetCachedData();
    rollback();
  }

  private void resetCachedData() {
    reprintableDocumentManager.invalidateReprintDocumentConfigurationCache(
        OBDal.getInstance().get(Organization.class, TestConstants.Orgs.FB_GROUP));
  }

  @Test
  public void uploadAndDownloadReprintableDocumentOfOrder()
      throws IOException, DocumentNotFoundException {
    AttachmentConfig config = createAttachmentConfiguration(TestConstants.Clients.FB_GRP,
        createAttachmentMethod(TestAttachImplementation.SEARCH_KEY, true));

    InputStream inputStream = new ByteArrayInputStream(DATA.getBytes(StandardCharsets.UTF_8));
    ReprintableSourceDocument<?> sourceDocument = ReprintableSourceDocument
        .newSourceDocument(TEST_ORDER_ID, DocumentType.ORDER);
    Order order = OBDal.getInstance().get(Order.class, TEST_ORDER_ID);

    ReprintableDocument document = reprintableDocumentManager.upload(inputStream, Format.XML,
        sourceDocument);
    assertThat(document.getFormat(), equalTo(Format.XML.name()));
    assertThat(document.getOrder().getId(), equalTo(order.getId()));
    assertThat(document.getClient().getId(), equalTo(order.getClient().getId()));
    assertThat(document.getOrganization().getId(), equalTo(order.getOrganization().getId()));
    assertThat(document.getName(), equalTo("1000014.xml"));
    assertThat(document.getAttachmentConfiguration().getId(), equalTo(config.getId()));
    OBDal.getInstance().flush();

    String downloadedDocument = download(sourceDocument);
    assertThat(downloadedDocument, equalTo(DATA));
  }

  @Test
  public void uploadAndDownloadReprintableDocumentOfInvoice()
      throws IOException, DocumentNotFoundException {
    AttachmentConfig config = createAttachmentConfiguration(TestConstants.Clients.FB_GRP,
        createAttachmentMethod(TestAttachImplementation.SEARCH_KEY, true));

    InputStream inputStream = new ByteArrayInputStream(DATA.getBytes(StandardCharsets.UTF_8));
    ReprintableSourceDocument<?> sourceDocument = ReprintableSourceDocument
        .newSourceDocument(TEST_INVOICE_ID, DocumentType.INVOICE);
    Invoice invoice = OBDal.getInstance().get(Invoice.class, TEST_INVOICE_ID);

    ReprintableDocument document = reprintableDocumentManager.upload(inputStream, Format.XML,
        sourceDocument);
    assertThat(document.getFormat(), equalTo(Format.XML.name()));
    assertThat(document.getInvoice().getId(), equalTo(invoice.getId()));
    assertThat(document.getClient().getId(), equalTo(invoice.getClient().getId()));
    assertThat(document.getOrganization().getId(), equalTo(invoice.getOrganization().getId()));
    assertThat(document.getName(), equalTo("1000014.xml"));
    assertThat(document.getAttachmentConfiguration().getId(), equalTo(config.getId()));

    OBDal.getInstance().flush();

    String downloadedDocument = download(sourceDocument);
    assertThat(downloadedDocument, equalTo(DATA));
  }

  @Test
  public void cannotUploadReprintableDocumentOfNonWritableDocument() {
    createAttachmentConfiguration(TestConstants.Clients.SYSTEM,
        createAttachmentMethod(TestAttachImplementation.SEARCH_KEY, true));

    setQAAdminContext();

    InputStream inputStream = new ByteArrayInputStream(DATA.getBytes(StandardCharsets.UTF_8));
    ReprintableSourceDocument<?> sourceDocument = ReprintableSourceDocument
        .newSourceDocument(TEST_ORDER_ID, DocumentType.ORDER);

    assertThrows(OBSecurityException.class,
        () -> reprintableDocumentManager.upload(inputStream, Format.XML, sourceDocument));
  }

  @Test
  public void uploadReprintableDocumentWithDefaultConfig()
      throws IOException, DocumentNotFoundException {
    InputStream inputStream = new ByteArrayInputStream(DATA.getBytes(StandardCharsets.UTF_8));
    ReprintableSourceDocument<?> sourceDocument = ReprintableSourceDocument
        .newSourceDocument(TEST_ORDER_ID, DocumentType.ORDER);
    Order order = OBDal.getInstance().get(Order.class, TEST_ORDER_ID);

    ReprintableDocument document = reprintableDocumentManager.upload(inputStream, Format.XML,
        sourceDocument);
    assertThat(document.getFormat(), equalTo(Format.XML.name()));
    assertThat(document.getOrder().getId(), equalTo(order.getId()));
    assertThat(document.getClient().getId(), equalTo(order.getClient().getId()));
    assertThat(document.getOrganization().getId(), equalTo(order.getOrganization().getId()));
    assertThat(document.getName(), equalTo("1000014.xml"));
    assertNull(document.getAttachmentConfiguration());

    OBDal.getInstance().flush();

    String downloadedDocument = download(sourceDocument);
    assertThat(downloadedDocument, equalTo(DATA));
  }

  @Test
  public void cannotDownloadReprintableDocumentOfNonReadableDocument() {
    createAttachmentConfiguration(TestConstants.Clients.FB_GRP,
        createAttachmentMethod(TestAttachImplementation.SEARCH_KEY, true));

    InputStream inputStream = new ByteArrayInputStream(DATA.getBytes(StandardCharsets.UTF_8));
    ReprintableSourceDocument<?> sourceDocument = ReprintableSourceDocument
        .newSourceDocument(TEST_ORDER_ID, DocumentType.ORDER);
    reprintableDocumentManager.upload(inputStream, Format.XML, sourceDocument);
    OBDal.getInstance().flush();

    setQAAdminContext();

    assertThrows(OBSecurityException.class, () -> download(sourceDocument));
  }

  @Test
  public void cannotDownloadReprintableDocumentIfDoesNotExist() {
    createAttachmentConfiguration(TestConstants.Clients.FB_GRP,
        createAttachmentMethod(TestAttachImplementation.SEARCH_KEY, true));

    // Retrieve a source document that does not have a linked reprintable document yet
    ReprintableSourceDocument<?> sourceDocument = ReprintableSourceDocument
        .newSourceDocument(TEST_ORDER_ID, DocumentType.ORDER);

    assertThrows(DocumentNotFoundException.class, () -> download(sourceDocument));
  }

  @Test
  public void checkOrderSourceDocumentExists() {
    ReprintableSourceDocument<?> sourceDocument = ReprintableSourceDocument
        .newSourceDocument(TEST_ORDER_ID, DocumentType.ORDER);
    assertTrue(sourceDocument.exists());
  }

  @Test
  public void checkInvoiceSourceDocumentExists() {
    ReprintableSourceDocument<?> sourceDocument = ReprintableSourceDocument
        .newSourceDocument(TEST_INVOICE_ID, DocumentType.INVOICE);
    assertTrue(sourceDocument.exists());
  }

  @Test
  public void enableReprintDocumentsInOrg() {
    setReprintDocuments(TestConstants.Orgs.ESP_NORTE, ENABLED);
    assertTrue(reprintableDocumentManager.isReprintDocumentsEnabled(TestConstants.Orgs.ESP_NORTE));
  }

  @Test
  public void disableReprintDocumentsInOrg() {
    setReprintDocuments(TestConstants.Orgs.ESP_NORTE, DISABLED);
    assertFalse(reprintableDocumentManager.isReprintDocumentsEnabled(TestConstants.Orgs.ESP_NORTE));
  }

  @Test
  public void reprintDocumentsIsDisabledIfNotSet() {
    setReprintDocuments(TestConstants.Orgs.ESP_NORTE, null);
    assertFalse(reprintableDocumentManager.isReprintDocumentsEnabled(TestConstants.Orgs.ESP_NORTE));
  }

  @Test
  public void enableReprintDocumentsInParent() {
    setReprintDocuments(TestConstants.Orgs.ESP, ENABLED);
    assertTrue(reprintableDocumentManager.isReprintDocumentsEnabled(TestConstants.Orgs.ESP_NORTE));
    assertTrue(reprintableDocumentManager.isReprintDocumentsEnabled(TestConstants.Orgs.ESP_SUR));
    assertFalse(reprintableDocumentManager.isReprintDocumentsEnabled(TestConstants.Orgs.US_EST));
    assertFalse(reprintableDocumentManager.isReprintDocumentsEnabled(TestConstants.Orgs.US_WEST));
  }

  @Test
  public void enableReprintDocumentsInAncestor() {
    setReprintDocuments(TestConstants.Orgs.FB_GROUP, ENABLED);
    assertTrue(reprintableDocumentManager.isReprintDocumentsEnabled(TestConstants.Orgs.ESP_NORTE));
    assertTrue(reprintableDocumentManager.isReprintDocumentsEnabled(TestConstants.Orgs.ESP_SUR));
    assertTrue(reprintableDocumentManager.isReprintDocumentsEnabled(TestConstants.Orgs.US_EST));
    assertTrue(reprintableDocumentManager.isReprintDocumentsEnabled(TestConstants.Orgs.US_WEST));
  }

  @Test
  public void configureReprintDocumentsAtDifferentLevels() {
    setReprintDocuments(TestConstants.Orgs.FB_GROUP, ENABLED);
    setReprintDocuments(TestConstants.Orgs.US, DISABLED);
    setReprintDocuments(TestConstants.Orgs.ESP_SUR, DISABLED);
    setReprintDocuments(TestConstants.Orgs.US_WEST, ENABLED);
    assertTrue(reprintableDocumentManager.isReprintDocumentsEnabled(TestConstants.Orgs.ESP_NORTE));
    assertFalse(reprintableDocumentManager.isReprintDocumentsEnabled(TestConstants.Orgs.ESP_SUR));
    assertFalse(reprintableDocumentManager.isReprintDocumentsEnabled(TestConstants.Orgs.US_EST));
    assertTrue(reprintableDocumentManager.isReprintDocumentsEnabled(TestConstants.Orgs.US_WEST));
  }

  @Test
  public void cannotSaveInvalidAttachmentConfiguration() {
    AttachmentMethod attachmentMethod = createAttachmentMethod(TestAttachImplementation.SEARCH_KEY,
        false);

    OBException thrown = assertThrows(OBException.class,
        () -> createAttachmentConfiguration(TestConstants.Clients.FB_GRP, attachmentMethod));
    assertThat(thrown.getMessage(), equalTo(INVALID_CONFIG_MSG));
  }

  @Test
  public void cannotUpdateInvalidAttachmentConfiguration() {
    AttachmentMethod reprintableDocAttachmentMethod = createAttachmentMethod(
        TestAttachImplementation.SEARCH_KEY, true);
    AttachmentMethod standardAttachmentMethod = createAttachmentMethod("ANOTHER_METHOD", false);
    AttachmentConfig config = createAttachmentConfiguration(TestConstants.Clients.FB_GRP,
        reprintableDocAttachmentMethod);

    OBException thrown = assertThrows(OBException.class, () -> {
      config.setAttachmentMethod(standardAttachmentMethod);
      OBDal.getInstance().flush();
    });
    assertThat(thrown.getMessage(), equalTo(INVALID_CONFIG_MSG));
  }

  @Test
  public void checkReprintableDocumentsWindows() {
    for (String windowId : Set.of("143", "167", "181", "183", "6916326417DB4A6FBD07870C0884E569",
        "FF808081330213E60133021822E40007")) {
      assertThat("Window " + windowId + " supports reprintable documents",
          reprintableDocumentManager.isReprintableDocumentsWindow(windowId), equalTo(true));
    }
  }

  @Test
  public void checkSomeNonReprintableDocumentsWindows() {
    for (String windowId : Set.of("6CB5B67ED33F47DFA334079D3EA2340E", "169", "184",
        "E547CE89D4C04429B6340FFA44E70716", "6F8F913FA60F4CBD93DC1D3AA696E76E")) {
      assertThat("Window " + windowId + " does not support reprintable documents",
          reprintableDocumentManager.isReprintableDocumentsWindow(windowId), equalTo(false));
    }
  }

  @Test
  @Issue("54971")
  public void uploadAndDownloadTransformedReprintableDocument()
      throws IOException, DocumentNotFoundException, TransformerNotFoundException {
    InputStream inputStream = new ByteArrayInputStream(DATA.getBytes(StandardCharsets.UTF_8));
    ReprintableSourceDocument<?> sourceDocument = ReprintableSourceDocument
        .newSourceDocument(TEST_ORDER_ID, DocumentType.ORDER);
    Order order = OBDal.getInstance().get(Order.class, TEST_ORDER_ID);

    ReprintableDocument document = reprintableDocumentManager.upload(inputStream, Format.XML,
        sourceDocument);
    assertThat(document.getFormat(), equalTo(Format.XML.name()));
    assertThat(document.getOrder().getId(), equalTo(order.getId()));
    assertThat(document.getClient().getId(), equalTo(order.getClient().getId()));
    assertThat(document.getOrganization().getId(), equalTo(order.getOrganization().getId()));
    assertThat(document.getName(), equalTo("1000014.xml"));
    OBDal.getInstance().flush();

    String downloadedDocument = download(sourceDocument, Format.PDF, getTransformer());
    assertThat(downloadedDocument, equalTo(TRANSFORMED_DATA));
  }

  @Test
  @Issue("54971")
  public void cannotTransformWhithoutATransformer() {
    InputStream inputStream = new ByteArrayInputStream(DATA.getBytes(StandardCharsets.UTF_8));
    ReprintableSourceDocument<?> sourceDocument = ReprintableSourceDocument
        .newSourceDocument(TEST_ORDER_ID, DocumentType.ORDER);
    Order order = OBDal.getInstance().get(Order.class, TEST_ORDER_ID);

    ReprintableDocument document = reprintableDocumentManager.upload(inputStream, Format.XML,
        sourceDocument);
    assertThat(document.getFormat(), equalTo(Format.XML.name()));
    assertThat(document.getOrder().getId(), equalTo(order.getId()));
    assertThat(document.getClient().getId(), equalTo(order.getClient().getId()));
    assertThat(document.getOrganization().getId(), equalTo(order.getOrganization().getId()));
    assertThat(document.getName(), equalTo("1000014.xml"));
    OBDal.getInstance().flush();

    assertThrows(TransformerNotFoundException.class,
        () -> download(sourceDocument, Format.PDF, null));
  }

  @Test
  @Issue("54971")
  public void checkIfCanTransform() {
    ReprintableDocumentManager rdm = spy(ReprintableDocumentManager.class);
    ReprintableDocumentTransformer transformer = getTransformer();

    when(rdm.getReprintableDocumentTransformer(Format.PDF)).thenReturn(Optional.of(transformer));
    when(rdm.getReprintableDocumentTransformer(Format.XML)).thenReturn(Optional.empty());

    assertThat(rdm.canTransformIntoFormat(Format.PDF), equalTo(true));
    assertThat(rdm.canTransformIntoFormat(Format.XML), equalTo(false));
  }

  private ReprintableDocumentTransformer getTransformer() {
    ReprintableDocumentTransformer transformer = mock(ReprintableDocumentTransformer.class);
    try {
      Path transformedFile = createTmpFile();
      try (FileWriter writer = new FileWriter(transformedFile.toFile())) {
        writer.write(TRANSFORMED_DATA);
      }
      when(transformer.transform(any(), any())).thenReturn(transformedFile);
    } catch (IOException ex) {
      throw new OBException(ex);
    }
    return transformer;
  }

  private AttachmentConfig createAttachmentConfiguration(String clientId,
      AttachmentMethod attachmentMethod) {
    try {
      OBContext.setAdminMode(false);
      AttachmentConfig config = OBProvider.getInstance().get(AttachmentConfig.class);
      config.setClient(OBDal.getInstance().getProxy(Client.class, clientId));
      config.setOrganization(
          OBDal.getInstance().getProxy(Organization.class, TestConstants.Orgs.MAIN));
      config.setAttachmentType(AttachmentType.RD.name());
      config.setAttachmentMethod(attachmentMethod);

      OBDal.getInstance().save(config);
      OBDal.getInstance().flush();

      return config;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private AttachmentMethod createAttachmentMethod(String searchKey,
      boolean supportReprintableDocuments) {
    try {
      OBContext.setAdminMode(false);
      setCoreInDevelopment(true);
      AttachmentMethod attachmentMethod = OBProvider.getInstance().get(AttachmentMethod.class);
      attachmentMethod
          .setClient(OBDal.getInstance().getProxy(Client.class, TestConstants.Clients.SYSTEM));
      attachmentMethod.setOrganization(
          OBDal.getInstance().getProxy(Organization.class, TestConstants.Orgs.MAIN));
      attachmentMethod
          .setModule(OBDal.getInstance().getProxy(Module.class, TestConstants.Modules.ID_CORE));
      attachmentMethod.setName(searchKey);
      attachmentMethod.setValue(searchKey);
      attachmentMethod.setSupportReprintableDocuments(supportReprintableDocuments);
      OBDal.getInstance().save(attachmentMethod);
      OBDal.getInstance().flush();
      setCoreInDevelopment(false);
      return attachmentMethod;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void setCoreInDevelopment(boolean inDevelopment) {
    try {
      OBContext.setAdminMode(false);
      OBDal.getInstance()
          .get(Module.class, TestConstants.Modules.ID_CORE)
          .setInDevelopment(inDevelopment);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private String download(ReprintableSourceDocument<?> sourceDocument)
      throws IOException, DocumentNotFoundException {
    Path file = createTmpFile();
    try (OutputStream os = new FileOutputStream(file.toFile())) {
      reprintableDocumentManager.download(sourceDocument, os);
    }
    try (FileInputStream is = new FileInputStream(file.toFile())) {
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private String download(ReprintableSourceDocument<?> sourceDocument, Format format,
      ReprintableDocumentTransformer transformer)
      throws IOException, DocumentNotFoundException, TransformerNotFoundException {
    ReprintableDocumentManager rdm = spy(ReprintableDocumentManager.class);
    when(rdm.getReprintableDocumentTransformer(format))
        .thenReturn(Optional.ofNullable(transformer));
    Path file = createTmpFile();
    try (OutputStream os = new FileOutputStream(file.toFile())) {
      rdm.download(sourceDocument, os, format);
    }
    try (FileInputStream is = new FileInputStream(file.toFile())) {
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private Path createTmpFile() throws IOException {
    Path file = Files.createTempFile(null, null);
    file.toFile().deleteOnExit();
    return file;
  }

  private void setReprintDocuments(String orgId, String status) {
    OBDal.getInstance().get(Organization.class, orgId).setReprintDocuments(status);
    OBDal.getInstance().flush();
  }
}
