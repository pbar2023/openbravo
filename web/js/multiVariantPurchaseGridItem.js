/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use. this
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

isc.ClassFactory.defineClass('MultiVariantPurchaseGridItem', isc.CanvasItem);

isc.MultiVariantPurchaseGridItem.addProperties({
  completeValue: null,
  showTitle: false,
  productData: {}, // Stores data for different products
  onTotalChange: null, // Function to be overwritten that will be called when total quantities change
  init: function() {
    this.colSpan = 4;
    this.disabled = false;
    this.grid = this.createGrid();
    // Clear product data on init
    this.productData = {};
    this.Super('init', arguments);
    OB.MultiVariantPurchaseGridItem = this;
  },
  createGrid: function(rows = [], columns = [], initialValues = []) {
    // Create or recreate ListGrid based on new or existing configuration
    if (this.grid) {
      this.grid.destroy(); // If grid exists, destroy before recreating
    }

    const hasRowCharacteristic = rows.length > 0;
    const hasColumnCharacteristic = columns.length > 0;
    const me = this;

    // Function that handles saving the productData info on each cell modification
    const changedFn = function(currentRow) {
      const currentDataFromGrid = currentRow.grid.getData();
      const currentRowData = currentRow.getData();
      const dataFromGridWithEditedValue = currentDataFromGrid.map(row => {
        if (currentRowData.characteristicValue === row.characteristicValue) {
          return currentRowData;
        }
        return row;
      });
      me.productData[me.currentProductId] = dataFromGridWithEditedValue.flatMap(
        row => me.transformObjectToArray(row)
      );

      // Recalculating totals
      this.record.total = me
        .transformObjectToArray(currentRowData)
        .reduce((sum, record) => sum + record.quantity, 0);
      currentRow.grid.refreshCell(
        this.rowNum,
        currentRow.grid.getColNum('total')
      );
      currentRow.grid.summaryRow.recalculateSummaries(
        dataFromGridWithEditedValue
      );

      this.Super('changed', arguments);
    };

    const cols = columns.map(col => ({
      name: col.name,
      title: col.title,
      type: '_id_29',
      defaultValue: 0,
      canGroupBy: false,
      canSort: false,
      changed: changedFn
    }));

    if (!hasColumnCharacteristic) {
      // There's only row characteristic,
      // an extra field is required to allow user
      // to enter values for each row characteristic value
      cols.push({
        name: 'NO_COLUMN_CHARACTERISTIC',
        title: OB.I18N.getLabel('QuantityLabel'),
        align: 'center',
        canEdit: true,
        canSort: false,
        canGroupBy: false,
        type: '_id_29',
        defaultValue: 0,
        changed: changedFn
      });
    }

    let rowsData = rows.map(row => ({
      characteristicName: row.title,
      characteristicValue: row.name,
      total: 0,
      ...cols.reduce((acc, col) => ({ ...acc, [col.name]: 0 }), {})
    }));

    if (!hasRowCharacteristic) {
      rowsData = [
        {
          characteristicName: '',
          characteristicValue: 'NO_ROW_CHARACTERISTIC',
          total: 0,
          ...columns.reduce((acc, col) => ({ ...acc, [col.name]: 0 }), {})
        }
      ];
    }

    // initialValues: [{ rowCharacteristicValue: 'X', columnCharacteristicValue: 'Y', quantity: 2 }]
    initialValues.forEach(item => {
      let row;
      if (rowsData.length === 1) {
        // NO_ROW_CHARACTERISTIC or only one column characteristic
        row = rowsData[0];
      } else {
        row = rowsData.find(
          r =>
            r.characteristicValue.toLowerCase() ===
            item.rowCharacteristicValue.toLowerCase()
        );
      }
      if (row && row.hasOwnProperty(item.columnCharacteristicValue)) {
        row[item.columnCharacteristicValue] = item.quantity;
        row.total += item.quantity;
      } else {
        row['NO_COLUMN_CHARACTERISTIC'] = item.quantity;
        row.total += item.quantity;
      }
    });

    const fields = [
      {
        name: 'characteristicValue',
        title: ' ',
        displayField: 'characteristicName',
        canEdit: false,
        width: 100,
        canSort: false,
        canGroupBy: false
      },
      ...cols
    ];

    if (hasColumnCharacteristic) {
      fields.push({
        name: 'total',
        title: OB.I18N.getLabel('TotalLabel'),
        type: '_id_29',
        align: 'right',
        canEdit: false,
        canSort: false,
        canGroupBy: false,
        showGridSummary: true
      });
    }

    this.grid = isc.OBGrid.create({
      width: '100%',
      height: 50,
      canEdit: true,
      editEvent: 'click',
      editByCell: true,
      showGridSummary: hasRowCharacteristic, // Shows total summary
      showHeaderContextMenu: false, // Disables the default context menu for headers
      headerContextMenu: null, // Ensure no custom menus are applied
      autoFitData: 'vertical', // Fit grid vertically based on number of rows
      autoFitMaxRecords: 10, // Maximum number of records to fit without scrolling
      canReorderFields: false, // Disables dragging to reorder columns
      canResizeFields: false, // Optionally disable resizing columns
      leaveScrollbarGap: false, // If you expect to never exceed 10, setting to false can remove unnecessary scrollbar space
      bodyOverflow: 'visible',
      overflow: 'auto', // Use hidden to cut off any excess but consider "auto" if exceeding
      cellHeight: OB.Styles.Process.PickAndExecute.gridCellHeight,
      fields: fields,
      data: rowsData,
      autoDraw: false,
      onRecordClick: function(
        viewer,
        record,
        recordNum,
        field,
        fieldNum,
        value,
        rawValue
      ) {
        const editRowNum = this.getEditRow();
        const editColNum = this.getEditCol();

        if (editRowNum != null && editColNum != null) {
          // If clicked cell is different from the editing cell
          if (editRowNum !== recordNum || editColNum !== fieldNum) {
            // Recalculate totals, this is due to this.endEditing not triggering cellEditEnd
            const allRows = this.getData();
            const editedRecord = this.getEditedRecord(editRowNum);
            const newTotal = allRows.reduce((sum, row) => {
              if (
                row.characteristicValue === editedRecord.characteristicValue
              ) {
                // Use the edited record values
                return sum + me.getRowTotal(editedRecord);
              }

              return sum + me.getRowTotal(row);
            }, 0);

            me.onTotalChange(newTotal);

            // End editing on the current cell
            viewer.endEditing();
          }
        }
        // Proceed with the default recordClick behavior
        this.Super('onRecordClick', arguments);
      },
      cellEditEnd: function(
        editCompletionEvent,
        newValue,
        ficCallDone,
        autoSaveDone
      ) {
        const rowNum = this.getEditRow();

        if (this.getRecord(rowNum)) {
          const allRows = this.getData();
          const editedRecord = this.getEditedRecord(rowNum);
          const newTotal = allRows.reduce((sum, row) => {
            if (row.characteristicValue === editedRecord.characteristicValue) {
              // Use the edited record values
              return sum + me.getRowTotal(editedRecord);
            }

            return sum + me.getRowTotal(row);
          }, 0);

          me.onTotalChange(newTotal);
        }

        this.Super('cellEditEnd', arguments);
      }
    });

    this.setCanvas(this.grid); // Assign the grid as the canvas of the CanvasItem
    this.grid.draw(); // Manually draw the grid
    return this.grid;
  },

  selectProduct: function(productId, columns, rows, initialValues) {
    if (productId === null) {
      // If no productId is provided, assume no grid should appear
      this.grid = null;
      this.currentProductId = null;
      this.setCanvas(null);
      return;
    }

    // Check if product data already exists
    if (this.productData[productId]) {
      const savedData = this.productData[productId];
      // Create grid with saved data
      this.createGrid(columns, rows, savedData);
    } else {
      // Create new grid with initial values
      this.createGrid(columns, rows, initialValues);
    }

    this.currentProductId = productId; // Update current product id
  },

  transformObjectToArray: function(obj) {
    // Extract the color value first
    const rowCh = obj.characteristicValue;
    const excludedKeys = ['characteristicValue', 'characteristicName', 'total'];
    // Use Object.keys() to get all keys, filter out excluded keys, and map to new array format
    return Object.keys(obj)
      .filter(key => !excludedKeys.includes(key) && !key.startsWith('_')) // Ignore the 'characteristic' keys
      .map(colCh => ({
        rowCharacteristicValue: rowCh,
        columnCharacteristicValue: colCh,
        quantity: Number(obj[colCh])
      }));
  },

  // Sums row quantities into a total
  getRowTotal: function(row) {
    const transformedRows = this.transformObjectToArray(row);
    return transformedRows.reduce(
      (sum, item) => sum + Number(item.quantity),
      0
    );
  },

  destroy: function() {
    if (this.canvas && typeof this.canvas.destroy === 'function') {
      this.canvas.destroy();
      this.canvas = null;
    }
    return this.Super('destroy', arguments);
  }
});
