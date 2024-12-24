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

import org.openbravo.model.ad.utility.ReprintableDocument;

/**
 * Allows to handle re-printable documents as attachments through an attachment provider in
 * particular.
 * 
 * The {@link ReprintableDocumentManager} is the class in charge of instantiating and use the
 * correct instance of this class depending on the attachment provider to be used on each case.
 */
public interface ReprintableDocumentAttachHandler {

  /**
   * Uploads the document the data of the ReprintableDocument as an attachment
   *
   * @param document
   *          The ReprintableDocument whose data is uploaded
   * @param documentData
   *          The InputStream with the document data
   */
  public void upload(ReprintableDocument document, InputStream documentData) throws IOException;

  /**
   * Retrieves ReprintableDocument data which was saved as an attachment
   *
   * @param document
   *          The ReprintableDocument whose data is retrieved
   *
   * @param outputStream
   *          the OutputStream where the document data should be provided
   */
  public void download(ReprintableDocument document, OutputStream outputStream) throws IOException;
}
