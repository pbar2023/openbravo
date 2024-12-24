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
 * All portions are Copyright (C) 2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.modulescript;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This module script will restore quartz.properties when updating to a version older than
 * 3.0.203901
 */
public class CreateMissingQuartzProperties extends ModuleScript {

  private static final Logger logger = LogManager.getLogger();

  private static final String CONFIG_DIR = "/config/";
  private static final String QUARTZ_CONF_FILE = "quartz.properties";

  public void execute() {
    try {
      final String sourcePath = getSourcePath();
      copyFromTemplateFile(sourcePath + CONFIG_DIR + QUARTZ_CONF_FILE);
    } catch (IOException e) {
      logger.error("Couldn't copy config/quartz.properties.template to config/quartz.properties",
        e);
    }
  }

  /**
   * Copies from template to target file, only when target files doesn't already exist.
   *
   * @param targetPath
   *          Target path, .template will be added at the end for source file
   * @throws IOException
   *           In case of error while copying
   */
  private void copyFromTemplateFile(String targetPath) throws IOException {
    Path source = Paths.get(targetPath + ".template");
    Path target = Paths.get(targetPath);

    if (Files.notExists(target)) {
      Files.copy(source, target);
    }
  }

  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits("0", null, new OpenbravoVersion(3, 0, 203901));
  }
}
