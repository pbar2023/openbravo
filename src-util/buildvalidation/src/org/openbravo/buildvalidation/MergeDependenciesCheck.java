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
 * All portions are Copyright (C) 2017-2022 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.buildvalidation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.beanutils.DynaBean;
import org.apache.ddlutils.io.DataReader;
import org.apache.ddlutils.io.DataToArraySink;
import org.apache.ddlutils.io.DatabaseDataIO;
import org.apache.ddlutils.model.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.ddlutils.task.DatabaseUtils;
import org.xml.sax.SAXException;

/**
 * Validation to check the instance does not have installed modules that depend on the merged
 * modules, like translation modules for example.
 */
public class MergeDependenciesCheck extends BuildValidation {
  private static final Logger log = LogManager.getLogger();

  private List<MergedModule> merges;
  private List<Module> allModules;

  private static final List<String> MODULE_INFO_FILES = Arrays.asList(
      "src-db/database/sourcedata/AD_MODULE.xml",
      "src-db/database/sourcedata/AD_MODULE_DEPENDENCY.xml",
      "src-db/database/sourcedata/AD_MODULE_MERGE.xml");

  @Override
  public List<String> execute() {
    try {
      initializeFromXml();
    } catch (Exception e) {
      log.warn("Could not read modules from {}. Skipping validation.", getOpenbravoDir());
      return Collections.emptyList();
    }

    try {
      List<String> errors = new ArrayList<>();
      errors.addAll(checkMergedModulesAreNotInModulesDir());
      errors.addAll(checkPossibleDependencies());
      return errors;
    } catch (Exception e) {
      return handleError(e);
    }
  }

  /** Reads modules, dependencies and merges from xml files */
  private void initializeFromXml() throws IOException, SAXException {
    if (!Files.exists(getOpenbravoDir())) {
      throw new RuntimeException("Could not read sources from " + getOpenbravoDir());
    }

    Database dbModel = DatabaseUtils.readDatabaseNoInit(new File[] {
        getOpenbravoDir().resolve("src-db/database/model/tables/AD_MODULE.xml").toFile(),
        getOpenbravoDir().resolve("src-db/database/model/tables/AD_MODULE_DEPENDENCY.xml").toFile(),
        getOpenbravoDir().resolve("src-db/database/model/tables/AD_MODULE_MERGE.xml").toFile(), });

    DatabaseDataIO io = new DatabaseDataIO();
    DataReader reader = io.getConfiguredCompareDataReader(dbModel);
    reader.getSink().start();

    readModule(reader, getOpenbravoDir());
    try (Stream<Path> stream = Files.list(getModulesDir())) {
      stream.forEach(moduleDir -> {
        try {
          readModule(reader, moduleDir);
        } catch (IOException | SAXException e) {
          throw new RuntimeException(e);
        }
      });
    }

    Vector<DynaBean> moduleInfo = ((DataToArraySink) reader.getSink()).getVector();

    merges = moduleInfo.stream()
        .filter(d -> "AD_MODULE_MERGE".equals(d.getDynaClass().getName()))
        .map(MergedModule::new)
        .collect(Collectors.toList());

    allModules = moduleInfo.stream()
        .filter(d -> "AD_MODULE".equals(d.getDynaClass().getName()))
        .map(moduleDef -> new Module(moduleDef,
            moduleInfo.stream()
                .filter(d -> "AD_MODULE_DEPENDENCY".equals(d.getDynaClass().getName()))))
        .collect(Collectors.toList());
  }

  private void readModule(DataReader dbReader, Path moduleDir) throws IOException, SAXException {
    for (String fileName : MODULE_INFO_FILES) {
      Path moduleFile = moduleDir.resolve(fileName);
      if (Files.exists(moduleFile)) {
        log.debug("reading {}", moduleFile);
        dbReader.parse(moduleFile.toFile());
      }
    }
  }

  /**
   * Checks the merged modules are not inside the modules folder. This is useful when the upgrade
   * process has been manually done by the user through the command line instead of using the Module
   * Management window that automatically removes the merged modules.
   * 
   * @return error message with a description of the problem
   * @throws SAXException
   * @throws IOException
   */
  private List<String> checkMergedModulesAreNotInModulesDir() {
    return merges.stream()
        .filter(merge -> findModuleById(merge.adModuleId).isPresent())
        .map(merge -> String.format(
            "The module %s has been merged into %s. You must uninstall it manually before proceeding with the update process.",
            findModuleById(merge.adModuleId).get(),
            findModuleById(merge.mergedIntoModuleId).orElse(null)))
        .collect(Collectors.toList());
  }

  /**
   * Checks the instance doesn't have any declared dependency to any of the merged modules
   * 
   * @return error message with a description of the problem
   * @throws SAXException
   * @throws IOException
   */
  private List<String> checkPossibleDependencies() {
    return allModules.stream()
        .filter(module -> module.dependencies.stream().anyMatch(dep -> merged(dep).isPresent()))
        .map(module -> {
          MergedModule mergedModule = merged(
              module.dependencies.stream().filter(dep -> merged(dep).isPresent()).findFirst().get())
                  .get();
          Module mergedInto = findModuleById(mergedModule.mergedIntoModuleId).orElse(null);

          return String.format(
              "The module %s has been merged into %s. However your instance has the module %s which declares a dependency on the merged module. You must uninstall it manually before proceeding with the update process.",
              mergedModule, mergedInto, module);
        })
        .collect(Collectors.toList());
  }

  /**
   * Return the openbravo directory for this instance
   */
  private Path getOpenbravoDir() {
    Path currentDir = Paths.get(System.getProperty("user.dir"));
    return currentDir.getParent().getParent();
  }

  /**
   * Return the openbravo/modules directory for this instance
   */
  private Path getModulesDir() {
    return getOpenbravoDir().resolve("modules");
  }

  private Optional<Module> findModuleById(String adModuleId) {
    return allModules.stream().filter(m -> adModuleId.equals(m.adModuleId)).findFirst();
  }

  private Optional<MergedModule> merged(String moduleId) {
    return merges.stream().filter(merge -> moduleId.equals(merge.adModuleId)).findFirst();
  }

  /**
   * Stores information about the merged modules
   * 
   */
  private class MergedModule {
    private String adModuleId;
    private String name;
    private String mergedIntoModuleId;

    private MergedModule(DynaBean mergedModule) {
      adModuleId = (String) mergedModule.get("MERGED_MODULE_UUID");
      name = (String) mergedModule.get("MERGED_MODULE_NAME");
      mergedIntoModuleId = (String) mergedModule.get("AD_MODULE_ID");
    }

    @Override
    public String toString() {
      return name;
    }
  }

  private class Module {
    private String adModuleId;
    private String javaPackage;
    private String name;
    private Set<String> dependencies;

    private Module(DynaBean mergedModule, Stream<DynaBean> allDependencies) {
      adModuleId = (String) mergedModule.get("AD_MODULE_ID");
      name = (String) mergedModule.get("NAME");
      javaPackage = (String) mergedModule.get("JAVAPACKAGE");

      dependencies = allDependencies.filter(dep -> adModuleId.equals(dep.get("AD_MODULE_ID")))
          .map(dep -> (String) dep.get("AD_DEPENDENT_MODULE_ID"))
          .collect(Collectors.toSet());
    }

    @Override
    public String toString() {
      return name + " (" + javaPackage + ")";
    }
  }
}
