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
 * All portions are Copyright (C) 2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.window;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openbravo.client.application.window.OBViewFieldHandler.FieldSorter;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.FieldGroup;

/**
 * Test the behaviour of OBViewFieldHandler.FieldSorter
 */
public class OBViewFieldHandlerFieldSorterTest {

  @Test
  public void canSortEmptyFieldList() throws Exception {
    List<Field> fields = new ArrayList<>();
    List<Field> sortedFields = FieldSorter.sortAccordingToFieldGroup(fields);
    assertTrue(sortedFields.isEmpty());
  }

  @Test
  public void maintainsOrderIfNoFieldGroupDefined() throws Exception {
    List<Field> fields = new ArrayList<>();
    fields.add(createField("0", null));
    fields.add(createField("1", null));
    fields.add(createField("2", null));

    List<Field> sortedFields = FieldSorter.sortAccordingToFieldGroup(fields);

    assertEquals(sortedFields.size(), fields.size());
    for (int i = 0; i < fields.size(); i++) {
      assertEquals(sortedFields.get(i).getId(), fields.get(i).getId());
    }
  }

  @Test
  public void maintainsOrderWithFieldGroupsButNoSplit() throws Exception {
    List<Field> fields = new ArrayList<>();
    FieldGroup fg0 = createFieldGroup("FG0");
    FieldGroup fg1 = createFieldGroup("FG1");
    fields.add(createField("0", null));
    fields.add(createField("1", fg0));
    fields.add(createField("2", fg1));

    List<Field> sortedFields = FieldSorter.sortAccordingToFieldGroup(fields);

    assertEquals(sortedFields.size(), fields.size());
    for (int i = 0; i < fields.size(); i++) {
      assertEquals(sortedFields.get(i).getId(), fields.get(i).getId());
    }
  }

  @Test
  public void maintainsOrderWithFieldGroupsButNoSplitWithNulls() throws Exception {
    List<Field> fields = new ArrayList<>();
    FieldGroup fg0 = createFieldGroup("FG0");
    FieldGroup fg1 = createFieldGroup("FG1");
    fields.add(createField("0", null));
    fields.add(createField("1", fg0));
    fields.add(createField("2", null));
    fields.add(createField("3", null));
    fields.add(createField("4", fg1));
    fields.add(createField("5", null));

    List<Field> sortedFields = FieldSorter.sortAccordingToFieldGroup(fields);

    assertEquals(sortedFields.size(), fields.size());
    for (int i = 0; i < fields.size(); i++) {
      assertEquals(sortedFields.get(i).getId(), fields.get(i).getId());
    }
  }

  @Test
  public void adjustOrderIfFieldGroupsAreSplit() throws Exception {
    List<Field> fields = new ArrayList<>();
    FieldGroup fg0 = createFieldGroup("FG0");
    FieldGroup fg1 = createFieldGroup("FG1");
    fields.add(createField("0", null));
    fields.add(createField("1", fg0));
    fields.add(createField("2", fg1));
    fields.add(createField("3", fg0));

    List<Field> sortedFields = FieldSorter.sortAccordingToFieldGroup(fields);

    assertEquals(sortedFields.size(), fields.size());
    // no field group
    assertEquals(sortedFields.get(0).getId(), "0");
    // fieldgroup FG0
    assertEquals(sortedFields.get(1).getId(), "1");
    assertEquals(sortedFields.get(2).getId(), "3");
    // fieldgroup FG1
    assertEquals(sortedFields.get(3).getId(), "2");
  }

  @Test
  public void adjustOrderIfFieldGroupsAreSplitWithNull() throws Exception {
    List<Field> fields = new ArrayList<>();
    FieldGroup fg0 = createFieldGroup("FG0");
    FieldGroup fg1 = createFieldGroup("FG1");
    fields.add(createField("0", null));
    fields.add(createField("1", fg0));
    fields.add(createField("2", null));
    fields.add(createField("3", fg1));
    fields.add(createField("4", null));
    fields.add(createField("5", null));
    fields.add(createField("6", fg0));
    fields.add(createField("7", null));

    List<Field> sortedFields = FieldSorter.sortAccordingToFieldGroup(fields);

    assertEquals(sortedFields.size(), fields.size());
    // no fieldgroup
    assertEquals(sortedFields.get(0).getId(), "0");
    // fieldgroup FG0
    assertEquals(sortedFields.get(1).getId(), "1");
    assertEquals(sortedFields.get(2).getId(), "2");
    assertEquals(sortedFields.get(3).getId(), "6");
    assertEquals(sortedFields.get(4).getId(), "7");
    // fieldgroup FG1
    assertEquals(sortedFields.get(5).getId(), "3");
    assertEquals(sortedFields.get(6).getId(), "4");
    assertEquals(sortedFields.get(7).getId(), "5");
  }

  private Field createField(String id, FieldGroup fieldGroup) {
    Field f = new Field();
    f.setId(id);
    f.setFieldGroup(fieldGroup);
    return f;
  }

  private FieldGroup createFieldGroup(String id) {
    FieldGroup fg = new FieldGroup();
    fg.setId(id);
    return fg;
  }
}
