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

import java.util.Optional;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.ReprintableDocument;
import org.openbravo.model.common.invoice.Invoice;

/**
 * Used to generate the data of a {@link ReprintableDocument} based in an {@link Invoice}
 */
public class ReprintableInvoice extends ReprintableSourceDocument<Invoice> {

  public ReprintableInvoice(String id) {
    super(id);
  }

  @Override
  Invoice getBaseDocument() {
    return OBDal.getInstance().getProxy(Invoice.class, getId());
  }

  @Override
  String getProperty() {
    return ReprintableDocument.PROPERTY_INVOICE;
  }

  @Override
  protected String getName() {
    return getBaseDocument().getDocumentNo();
  }

  @Override
  protected Optional<String> getUploadEvent() {
    // Although this code creates a dependency with the Business API module due to the string with
    // the name of the event triggered and this is not totally clean, with it we do not have to
    // overcomplicate by adding an abstraction layer with hooks to be implemented by modules.
    return Optional.of("API_ReprintableInvoiceCreated");
  }
}
