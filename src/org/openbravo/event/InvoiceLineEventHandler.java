/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2013-2023 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.event;

import javax.enterprise.event.Observes;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

class InvoiceLineEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(InvoiceLine.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkInvoiceLineRelation((InvoiceLine) event.getTargetInstance());
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkProductDefinedIfNoFinancialInvoiceLine(event);
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkProductDefinedIfNoFinancialInvoiceLine(event);
  }

  private void checkProductDefinedIfNoFinancialInvoiceLine(EntityPersistenceEvent event) {
    final InvoiceLine invoiceLine = (InvoiceLine) event.getTargetInstance();
    if (!invoiceLine.isFinancialInvoiceLine() && invoiceLine.getProduct() == null) {
      throw new OBException("@NoProductSelected@");
    }
  }

  private void checkInvoiceLineRelation(InvoiceLine invoiceLine) {
    OBCriteria<InvoiceLine> criteria = OBDal.getInstance().createCriteria(InvoiceLine.class);
    criteria.add(Restrictions.eq(InvoiceLine.PROPERTY_INVOICE, invoiceLine.getInvoice()));

    if (criteria.count() == 1) {
      Invoice invoice = OBDal.getInstance().get(Invoice.class, invoiceLine.getInvoice().getId());

      if (invoice != null) {
        invoice.setSalesOrder(null);
        OBDal.getInstance().save(invoice);
        unlinkInvoiceFromGoodsReceipt(invoice);
        OBDal.getInstance().flush();
      }
    }
  }

  private void unlinkInvoiceFromGoodsReceipt(Invoice objInvoice) {
    OBCriteria<ShipmentInOut> criteria = OBDal.getInstance().createCriteria(ShipmentInOut.class);
    criteria.add(Restrictions.eq(ShipmentInOut.PROPERTY_SALESTRANSACTION, Boolean.FALSE));
    criteria.add(Restrictions.eq(ShipmentInOut.PROPERTY_INVOICE, objInvoice));

    ShipmentInOut goodsReceipt = (ShipmentInOut) criteria.uniqueResult();
    if (goodsReceipt != null) {
      goodsReceipt.setInvoice(null);
      OBDal.getInstance().save(goodsReceipt);
    }
  }
}
