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
package org.openbravo.client.application.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.model.ad.utility.ReprintableDocument;

/**
 * Attach implementation for testing purposes
 */
@ApplicationScoped
@ComponentProvider.Qualifier(TestAttachImplementation.SEARCH_KEY)
public class TestAttachImplementation implements ReprintableDocumentAttachHandler {

  private static final String TMP_PATH = System.getProperty("java.io.tmpdir");
  static final String SEARCH_KEY = "TEST";

  @Override
  public void upload(ReprintableDocument document, InputStream documentData) throws IOException {
    Path path = Paths.get(TMP_PATH, document.getName());
    Files.copy(documentData, path, StandardCopyOption.REPLACE_EXISTING);
  }

  @Override
  public void download(ReprintableDocument document, OutputStream outputStream) throws IOException {
    Path path = Paths.get(TMP_PATH, document.getName());
    Files.copy(path, outputStream);
  }
}
