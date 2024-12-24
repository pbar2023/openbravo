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

OB = OB || {};

OB.MultiVariantPurchaseGrid = {
  processPopup: null,
  execute: function(params, view) {
    const selection = params.button.contextView.viewGrid.getSelectedRecords(),
      recordIdList = [];

    for (let i = 0; i < selection.length; i++) {
      recordIdList.push(selection[i].id);
    }

    this.processPopup = isc.MultiVariantPurchaseGridProcessPopup.create({
      recordIdList: recordIdList,
      view: view,
      params: params
    });
    this.processPopup.show();
  },

  open: function(params, view) {
    params.adTabId = view.activeView.tabId;
    OB.MultiVariantPurchaseGrid.execute(params, view);
  },

  close: function() {
    this.processPopup.destroy();
  }
};

// 22803EBEEC804A648723B2B7070DBB7D is the id for the ProductVariantDataSource
const PRODUCT_VARIANT_DATA_SOURCE_ID = '22803EBEEC804A648723B2B7070DBB7D';
const PRODUCT_CHARACTERISTIC_VALUE_DATA_SOURCE_ID =
  'B87CFEF81AC347C28EF5C5BCECE0637C';

/**
 * Handles selection of different generic products. When a product is selected, it will properly show the bottom grid with the expected data.
 */
function handleSelection(grid, record, state) {
  if (!state) {
    return;
  }
  if (!record.product) {
    // No product set, reset the bottom grid using null
    OB.MultiVariantPurchaseGridItem.selectProduct(null);
    return;
  }
  if (record.newRow) {
    const requestProperties = {};
    requestProperties.params = {};
    requestProperties.params.productId = record.product;
    const o = {
      setDataSource: dataS => {
        this.dataSource = dataS;
        this.dataSource.fetchData(
          {},
          (response, data) => {
            const dataReceived = data[0];
            dataReceived.rowCharacteristics.sort((a, b) => a.seqNo - b.seqNo);
            const rowCharacteristics = dataReceived.rowCharacteristics.map(
              r => ({
                name: r.id,
                title: r._identifier
              })
            );
            dataReceived.columnCharacteristics.sort((a, b) => a.seqNo - b.seqNo);
            const columnCharacteristics = dataReceived.columnCharacteristics.map(
              r => ({
                name: r.id,
                title: r._identifier
              })
            );
            OB.MultiVariantPurchaseGridItem.selectProduct(
              record.product,
              rowCharacteristics,
              columnCharacteristics
            );
          },
          requestProperties
        );
      }
    };
    OB.Datasource.get(
      PRODUCT_CHARACTERISTIC_VALUE_DATA_SOURCE_ID,
      o,
      null,
      false
    );
  } else {
    const rowCharacteristics = record.rowCharacteristics.map(r => ({
      name: r.id,
      title: r._identifier
    }));
    const columnCharacteristics = record.columnCharacteristics.map(r => ({
      name: r.id,
      title: r._identifier
    }));
    // Record selected, comes from backend, so it should already contain rowCharacteristics and columnCharacteristics
    OB.MultiVariantPurchaseGridItem.selectProduct(
      record.product,
      rowCharacteristics,
      columnCharacteristics,
      record.initialValues
    );
  }
}

const getViewProperties = recordId => ({
  allowAdd: true,
  allowDelete: true,
  showSelect: false,
  selectionType: 'S',
  arrowKeyAction: 'select',
  autoFetchData: true,
  handleItemChange: function(item) {
    if (item.getValue() == null) {
      return;
    }

    const rowsData = item.grid.getData().allRows;
    const rowNum = item.rowNum;
    if (rowsData) {
      if (
        !rowsData.some(
          (row, idx) => row.product === item.getValue() && idx !== rowNum
        )
      ) {
        item.grid.endEditing();
        item.grid.selectSingleRecord(item.rowNum);
      } else {
        item.setValue(undefined);
        isc.warn(
          OB.I18N.getLabel('GenericProductAlrdyAddedError'),
          function() {
            return true;
          },
          {
            icon: '[SKINIMG]Dialog/error.png',
            title: OB.I18N.getLabel('OBUIAPP_Error')
          }
        );
      }
    }
  },
  selectionFn: handleSelection,
  newFn: grid => {
    const returnObject = isc.addProperties({}, grid.data[0]);
    returnObject.newRow = true;
    returnObject.quantity = 0;
    return returnObject;
  },
  removeFn: function() {
    // Unselect any previously selected product
    OB.MultiVariantPurchaseGridItem.selectProduct(null);
    return true;
  },
  dataSourceProperties: {
    createClassName: '',
    dataURL: OB.Utilities.applicationUrl(
      `org.openbravo.service.datasource/${PRODUCT_VARIANT_DATA_SOURCE_ID}`
    ),
    requestProperties: {
      params: {
        Constants_IDENTIFIER: OB.Constants.IDENTIFIER,
        Constants_FIELDSEPARATOR: OB.Constants.FIELDSEPARATOR,
        recordId: recordId
      }
    },
    fields: [
      {
        name: 'product',
        type: '_id_AA08D5AFC89D4F6CB838064FC06EF5E4'
      }
    ]
  },
  fields: [
    {
      name: 'product',
      id: '1127',
      title: OB.I18N.getLabel('Product'),
      required: true,
      sessionProperty: true,
      columnName: 'M_Product_ID',
      inpColumnName: 'inpmProductId',
      refColumnName: 'M_Product_ID',
      targetEntity: 'Product',
      firstFocusedField: true,
      selectorDefinitionId: 'D55B3817C9CF4AC4AFEE7B0222C1453A',
      popupTextMatchStyle: 'substring',
      textMatchStyle: 'substring',
      defaultPopupFilterField: '_identifier',
      displayField: '_identifier',
      valueField: 'id',
      sortByField: '_identifier',
      pickListFields: [
        {
          title: OB.I18N.getLabel('Product'),
          name: '_identifier',
          type: 'text'
        },
        {
          title: OB.I18N.getLabel('RowCharacteristicLbl'),
          name: 'rowCharacteristic$_identifier',
          type: 'text'
        },
        {
          title: OB.I18N.getLabel('ColumnCharacteristicLbl'),
          name: 'columnCharacteristic$_identifier',
          type: 'text'
        }
      ],
      showSelectorGrid: true,
      selectorGridFields: [
        {
          title: OB.I18N.getLabel('ProductSearchKey'),
          name: 'searchKey',
          type: '_id_10',
          showHover: true
        },
        {
          title: OB.I18N.getLabel('NameLbl'),
          name: 'name',
          type: '_id_10',
          showHover: true
        },
        {
          title: OB.I18N.getLabel('CharacteristicDescriptionLbl'),
          name: 'characteristicDescription',
          type: '_id_C632F1CFF5A1453EB28BDF44A70478F8',
          showHover: true
        },
        {
          title: OB.I18N.getLabel('RowCharacteristicLbl'),
          name: 'rowCharacteristic',
          displayField: 'rowCharacteristic$_identifier',
          type: '_id_10',
          showHover: true
        },
        {
          title: OB.I18N.getLabel('ColumnCharacteristicLbl'),
          name: 'columnCharacteristic',
          displayField: 'columnCharacteristic$_identifier',
          type: '_id_10',
          showHover: true
        }
      ],
      extraSearchFields: [
        'product$searchKey',
        'product$name',
        'product$_identifier'
      ],
      init: function() {
        let existingGenericProductIds = null;
        const rowData = this.grid.getData().allRows;
        if (rowData && rowData.length > 0) {
          existingGenericProductIds = rowData.map(row => row.product).join();
        }
        this.optionDataSource = OB.Datasource.create({
          createClassName: '',
          dataURL: OB.Utilities.applicationUrl(
            'org.openbravo.service.datasource/Product'
          ),
          requestProperties: {
            params: {
              adTabId: '187',
              Constants_IDENTIFIER: '_identifier',
              Constants_FIELDSEPARATOR: '$',
              targetProperty: 'product',
              _extraProperties: '',
              columnName: 'M_Product_ID',
              IsSelectorItem: 'true',
              existingGenericProductIds: existingGenericProductIds
            }
          },
          fields: [
            { name: 'id', type: '_id_13', primaryKey: true },
            { name: 'client', type: '_id_19' },
            { name: 'client$_identifier' },
            { name: 'organization', type: '_id_19' },
            { name: 'organization$_identifier' },
            { name: 'active', type: '_id_20' },
            { name: 'updated', type: '_id_16' },
            { name: 'updatedBy', type: '_id_30' },
            { name: 'updatedBy$_identifier' },
            { name: 'creationDate', type: '_id_16' },
            { name: 'createdBy', type: '_id_30' },
            { name: 'createdBy$_identifier' },
            { name: '_identifier' },
            { name: 'rowCharacteristic', type: '_id_30' },
            { name: 'columnCharacteristic', type: '_id_30' }
          ]
        });
        this.Super('init', arguments);
      },
      outHiddenInputPrefix: 'inpmProductId',
      gridProps: {
        sort: 2,
        autoExpand: true,
        displaylength: 44,
        fkField: true,
        selectOnClick: true,
        canSort: true,
        canFilter: false,
        canGroupBy: false,
        showHover: true,
        filterEditorProperties: { keyProperty: 'id' }
      },
      type: '_id_AA08D5AFC89D4F6CB838064FC06EF5E4'
    },
    {
      name: 'quantity',
      id: 'quantity',
      title: OB.I18N.getLabel('QuantityLabel'),
      disabled: false,
      canEdit: false,
      updatable: false,
      columnName: 'quantity',
      inpColumnName: 'quantity',
      length: 16,
      gridProps: {
        sort: 3,
        autoExpand: true,
        length: 16,
        displaylength: 16,
        selectOnClick: false,
        canFilter: false,
        canGroupBy: false,
        showHover: true,
        canSort: false,
        width: 50
      },
      type: '_id_29'
    }
  ],
  gridProperties: {
    orderByClause: '',
    filterClause: false,
    allowSummaryFunctions: false,
    alias: 'e',
    lazyFiltering: false
  },
  standardProperties: {},
  statusTabFields: [],
  tabId: '294',
  handleReadOnlyLogic: (formValues, contextInfo, form) => {
    // Handles disabling the field if it has already been edited before
    const fields = form.getFields();
    for (let i = 0; i < fields.length; i++) {
      const field = form.getField(i);
      if (field.id === '1127' && formValues.product != null) {
        field.setDisabled(true);
      }
    }
    form.markForRedraw();
  }
});

isc.ClassFactory.defineClass('ProductSelectionGridItem', isc.CanvasItem);
isc.ProductSelectionGridItem.addProperties({
  height: 300,
  showTitle: false,
  isPickAndExecuteWindow: true,
  alternateRecordStyles: true,
  showFilterEditor: false,
  canReorderFields: false,
  canFreezeFields: false,
  canGroupBy: false,
  canAutoFitFields: false,
  dataPageSize: 100,
  init: function() {
    const modifiedView = isc.addProperties(this.view, {
      getUnderLyingRecordContext: () => ({}),
      messageBar: {
        setMessage: function(type, message) {
          // There was an error, handle it
          if (message != null && message !== '') {
            isc.warn(
              message,
              function() {
                OB.MultiVariantPurchaseGrid.close();
                return true;
              },
              {
                icon: '[SKINIMG]Dialog/error.png',
                title: OB.I18N.getLabel('OBUIAPP_Error')
              }
            );
          }
        }
      }
    });

    this.canvas = isc.OBPickAndExecuteView.create({
      height: 300,
      view: modifiedView,
      viewProperties: getViewProperties(this.recordId),
      onTotalChange: function(newTotal) {
        const selectedRecord = this.viewGrid.getSelectedRecord();
        if (selectedRecord) {
          this.viewGrid.setRawCellValue(selectedRecord, null, 2, newTotal);
        }
      },
      init: function() {
        this.Super('init', arguments);

        // Initially do a fetchData to show order line related products
        this.viewGrid.fetchData();
      }
    });
    this.Super('init', arguments);
    this.selectionLayout = this.canvas;
  }
});

isc.defineClass('MultiVariantPurchaseGridProcessPopup', isc.OBPopup);

isc.MultiVariantPurchaseGridProcessPopup.addProperties({
  width: 600,
  height: 600,
  title: OB.I18N.getLabel('ModelModeTitle'),
  showMinimizeButton: false,
  showMaximizeButton: false,

  //Form
  mainform: null,

  //Button
  okButton: null,
  cancelButton: null,

  getProductGrid: function(view, recordId) {
    return {
      type: 'ProductSelectionGridItem',
      title: 'Product grid',
      name: 'product-grid',
      editorType: 'ProductSelectionGridItem',
      view: view,
      recordId: recordId
    };
  },

  getGrid: function() {
    const me = this;
    return {
      type: 'MultiVariantPurchaseGridItem',
      title: 'Characteristics grid',
      name: 'ch-grid',
      editorType: 'MultiVariantPurchaseGridItem',
      onTotalChange: newTotal => {
        me.mainform.items[0].canvas.onTotalChange(newTotal);
      }
    };
  },

  initWidget: function() {
    var recordIdList = this.recordIdList,
      originalView = this.view;

    this.mainform = isc.DynamicForm.create({
      view: originalView,
      numCols: 1,
      colWidths: ['100%'],
      fields: [
        this.getProductGrid(originalView, recordIdList[0]),
        this.getGrid()
      ]
    });

    this.okButton = isc.OBFormButton.create({
      title: OB.I18N.getLabel('OK'),
      popup: this,
      action: function() {
        var callback, productRowIds, productRowsData;

        callback = function(rpcResponse, data, rpcRequest) {
          if (data.message) {
            // Error when applying the process
            isc.warn(
              data.message.text,
              function() {
                return true;
              },
              {
                icon: '[SKINIMG]Dialog/error.png',
                title: OB.I18N.getLabel('OBUIAPP_Error')
              }
            );
          } else {
            rpcRequest.clientContext.popup.close();
            rpcRequest.clientContext.originalView.view.refresh(false, false);
          }
        };

        productRowIds = this.popup.mainform
          .getItem('product-grid')
          .canvas.viewGrid.getData()
          .allRows.map(
            row => row.product // Ignore other data that is not being used, only product id is being used in the request
          );
        productRowsData = this.popup.mainform.getItem('ch-grid').productData;

        OB.RemoteCallManager.call(
          'org.openbravo.client.application.event.UpdateMultiVariantPurchaseOrderLines',
          {
            recordIdList: recordIdList,
            productRowIds: productRowIds,
            productRowsData: productRowsData
          },
          {},
          callback,
          {
            originalView: this.popup.view,
            popup: this.popup
          }
        );
      }
    });

    this.cancelButton = isc.OBFormButton.create({
      title: OB.I18N.getLabel('Cancel'),
      popup: this,
      action: function() {
        this.popup.closeClick();
      }
    });

    this.items = [
      isc.VLayout.create({
        defaultLayoutAlign: 'center',
        align: 'center',
        width: '100%',
        layoutMargin: 10,
        membersMargin: 6,
        members: [
          isc.HLayout.create({
            defaultLayoutAlign: 'center',
            height: '75%',
            align: 'center',
            layoutMargin: 15,
            membersMargin: 6,
            members: this.mainform
          }),
          isc.HLayout.create({
            defaultLayoutAlign: 'center',
            height: '25%',
            align: 'center',
            membersMargin: 10,
            members: [this.okButton, this.cancelButton]
          })
        ]
      })
    ];

    this.Super('initWidget', arguments);
  }
});
