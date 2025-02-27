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
 * All portions are Copyright (C) 2011-2023 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

// == OBSearchItem ==
// Item used for Openbravo search fields.
isc.ClassFactory.defineClass('OBSearchItem', isc.StaticTextItem);

isc.ClassFactory.mixInInterface('OBSearchItem', 'OBLinkTitleItem');

// a global function as it is called from classic windows
(function(w) {
  w.closeSearch = function(action, value, display, parameters, wait) {
    var length,
      i,
      hiddenInputName,
      targetFld = isc.OBSearchItem.openSearchItem;

    if (action === 'SAVE') {
      if (!targetFld.valueMap) {
        targetFld.valueMap = {};
      }

      targetFld.storeValue(value);
      if (!targetFld.valueMap) {
        targetFld.valueMap = {};
      }
      targetFld.valueMap[targetFld.getValue()] = display;
      if (targetFld.displayField) {
        targetFld.form.setValue(targetFld.displayField, display);
        //setting the grid value so that it is not lost when there is another search field in the grid.
        //refer issue https://issues.openbravo.com/view.php?id=27243
        if (targetFld.form.grid && targetFld.form.grid.getEditForm()) {
          targetFld.form.grid.setEditValue(
            targetFld.form.grid.getEditRow(),
            targetFld.name,
            value
          );
          targetFld.form.grid.setEditValue(
            targetFld.form.grid.getEditRow(),
            targetFld.displayField,
            display
          );
        }
      }
      targetFld.updateValueMap(true);

      if (parameters && parameters.length > 0) {
        length = parameters.length;
        for (i = 0; i < length; i++) {
          hiddenInputName =
            (parameters[i].esRef ? targetFld.inpColumnName : '') +
            parameters[i].campo;
          // Revisit for grid editor, maybe setting the value in the form will set it
          // in the record to be kepped there
          targetFld.form.hiddenInputs[hiddenInputName] = parameters[i].valor;
        }
      }
      targetFld._hasChanged = true;
      if (targetFld.form.handleItemChange) {
        targetFld.form.handleItemChange(targetFld);
      }
      // fire with a delay otherwise results in strange errors
      targetFld.fireOnPause('validate', targetFld.validate, null, targetFld);

      if (targetFld.form.focusInNextItem) {
        targetFld.form.focusInNextItem(targetFld.name);
      }
    }
    if (isc.OBSearchItem.openedWindow.closeClick) {
      isc.OBSearchItem.openedWindow.closeClick();
    } else {
      isc.OBSearchItem.openedWindow.close();
    }
    isc.OBSearchItem.openSearchItem = null;
  };
})(this); // window
isc.OBSearchItem.addProperties({
  operator: 'iContains',
  showPickerIcon: true,
  canFocus: true,
  showFocused: true,
  wrap: false,
  clipValue: true,
  validateOnChange: true,

  // NOTE: FormItem don't have initWidget but use init
  init: function() {
    this.instanceClearIcon = isc.shallowClone(this.clearIcon);
    this.instanceClearIcon.formItem = this;
    this.valueMap = {};

    this.instanceClearIcon.showIf = function(form, item) {
      if (item.disabled) {
        return false;
      }
      if (item.required) {
        return false;
      }
      if (form && form.view && form.view.readOnly) {
        return false;
      }
      if (item.getValue()) {
        return true;
      }
      return false;
    };

    this.instanceClearIcon.click = function() {
      var targetFld = this.formItem;

      this.formItem.setValue(null);

      targetFld._hasChanged = true;
      targetFld.form.handleItemChange(targetFld);
      // fire with a delay otherwise results in strange errors
      targetFld.fireOnPause('validate', targetFld.validate, null, targetFld);
    };

    this.icons = [this.instanceClearIcon];

    return this.Super('init', arguments);
  },

  getDisplayValue: function(args) {
    var displayValue = this.Super('getDisplayValue', args),
      unescapedDisplayValue;
    // unescape before escaping to prevent escaping text that is already escaped
    unescapedDisplayValue = displayValue ? displayValue.unescapeHTML() : null;
    return unescapedDisplayValue && unescapedDisplayValue.asHTML();
  },

  // show the complete displayed value, handy when the display value got clipped
  itemHoverHTML: function(item, form) {
    return this.getDisplayValue(this.getValue());
  },

  click: function() {
    this.showPicker();
    return false;
  },

  keyPress: function(item, form, keyName, characterValue) {
    if (keyName === 'Enter') {
      this.showPicker();
      return false;
    }
    return true;
  },

  showPicker: function() {
    if (this.isDisabled()) {
      return;
    }
    var parameters = [],
      index = 0,
      i = 0,
      length,
      propDef,
      inpName,
      values;
    var form = this.form,
      view = form.view;
    if (this.isFocusable()) {
      this.focusInItem();
    }
    parameters[index++] = 'inpIDValue';
    if (this.getValue()) {
      parameters[index++] = this.getValue();
    } else {
      parameters[index++] = '';
    }
    parameters[index++] = 'WindowID';
    parameters[index++] = view.standardWindow.windowId;
    values = view.getContextInfo(false, true, true, true);
    length = this.inFields.length;
    for (i = 0; i < length; i++) {
      inpName = this.inFields[i].columnName;
      propDef = view.getPropertyDefinitionFromInpColumnName(inpName);
      if (propDef && values[inpName]) {
        parameters[index++] = this.inFields[i].parameterName;
        parameters[index++] = values[inpName];
        // and to be save also pass the value as the input name
        parameters[index++] = inpName;
        parameters[index++] = values[inpName];
      }
    }
    this.openSearchWindow(this.searchUrl, parameters, this.getValue());
  },

  openSearchWindow: function(url, parameters, strValueID) {
    var height, width;
    var auxField = '';
    var i;
    var displayedValue = '';

    if (this.valueMap && this.valueMap[this.getValue()]) {
      displayedValue = this.valueMap[this.getValue()];
    }

    if (url.indexOf('Location') !== -1) {
      height = 300;
      width = 600;
    } else {
      height = screen.height - 100;
      width = 900;
    }

    if (isc.OBSearchItem.openedWindow) {
      if (isc.OBSearchItem.openedWindow.closeClick) {
        isc.OBSearchItem.openedWindow.closeClick();
      } else {
        isc.OBSearchItem.openedWindow.close();
      }
      this.clearUnloadEventHandling();
    }
    isc.OBSearchItem.openedWindow = null;

    if (strValueID) {
      auxField = 'inpNameValue=' + encodeURIComponent(displayedValue);
    }
    if (parameters) {
      var total = parameters.length;
      for (i = 0; i < total; i++) {
        if (auxField !== '') {
          auxField += '&';
        }
        // TODO: check this
        //        if (parameters[i] === 'isMultiLine' && parameters[i + 1] == 'Y') {
        //          gIsMultiLineSearch = true;
        //        }
        auxField +=
          parameters[i] +
          '=' +
          (parameters[i + 1] !== null
            ? encodeURIComponent(parameters[i + 1])
            : '');
        i++;
      }
    }

    isc.OBSearchItem.openedWindow = OB.Layout.ClassicOBCompatibility.Popup.open(
      'SELECTOR',
      width,
      height,
      OB.Utilities.applicationUrl(url) +
        (auxField === '' ? '' : '?' + auxField),
      '',
      window,
      true,
      true,
      true,
      null,
      true
    );
    if (isc.OBSearchItem.openedWindow) {
      isc.OBSearchItem.openedWindow.name = 'SELECTOR';
      isc.OBSearchItem.openedWindow.focus();
      this.setUnloadEventHandling();
    }
    isc.OBSearchItem.openSearchItem = this;
  },

  setUnloadEventHandling: function() {
    var me = this;
    if (document.layers) {
      document.captureEvents(isc.Event.UNLOAD);
    }
    window.onunload = function() {
      if (isc.OBSearchItem.openedWindow) {
        isc.OBSearchItem.openedWindow.close();
      }
      isc.OBSearchItem.openedWindow = null;
      me.clearUnloadEventHandling();
    };
  },

  clearUnloadEventHandling: function() {
    if (document.layers) {
      window.releaseEvents(isc.Event.UNLOAD);
    }
    window.onunload = function() {};
  },

  mapValueToDisplay: function(value) {
    if (this.displayField) {
      return this.form.getValue(this.displayField);
    } else {
      return this.Super('mapValueToDisplay', arguments);
    }
  }
});
