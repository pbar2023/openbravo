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
 * All portions are Copyright (C) 2024 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.application.attachment;

import java.io.IOException;
import java.nio.file.Path;

import org.openbravo.base.Prioritizable;
import org.openbravo.model.ad.utility.ReprintableDocument;

/**
 * Transforms the {@link ReprintableDocument} data linked to a {@link ReprintableSourceDocument}
 * into the format specified by the implementation with the {@link ReprintableSourceDocument}
 * annotation.
 */
public interface ReprintableDocumentTransformer extends Prioritizable {

  /**
   * Transforms a reprintable document into a different format.
   *
   * @param sourceDocument
   *          The source document linked to the document to be transformed
   * @param originalDocument
   *          The path of the file containing the original document data
   *
   * @return The path of the resulting file with the transformed document. Note that this file will
   *         be removed automatically by the infrastructure once it is no longer needed.
   * @throws IOException
   *           if an I/O error occurs during the transformation process.
   */
  public Path transform(ReprintableSourceDocument<?> sourceDocument, Path originalDocument)
      throws IOException;
}
