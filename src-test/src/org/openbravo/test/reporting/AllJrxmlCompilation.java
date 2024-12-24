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
package org.openbravo.test.reporting;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.report.ReportingUtils;

import net.sf.jasperreports.engine.JRException;

/**
 * Compiles all jrxml templates present in the sources directory ensuring they can be compiled with
 * the current combination of jdk + ejc.
 * 
 * @author alostale
 *
 */
public class AllJrxmlCompilation extends WeldBaseTest {
  // The following constant is used to exclude/skip those jrxml templates that have hwmanager logic
  private static final String HWM_TEST_PREFIX = "com.openbravo.pos";

  private static final Logger log = LogManager.getLogger();

  private static final List<Path> REPORTS = parameters();

  @Rule
  public ParameterCdiTestRule<Path> reportRule = new ParameterCdiTestRule<Path>(REPORTS);

  private @ParameterCdiTest Path report;

  @Override
  protected boolean shouldMockServletContext() {
    return true;
  }

  @Test
  public void jrxmlShouldCompile() throws JRException {
    ReportingUtils.compileReport(report.toString());
  }

  private static List<Path> parameters() {
    final List<Path> allJasperFiles = new ArrayList<>();
    try {
      allJasperFiles.addAll(getJrxmlTemplates("src"));
      allJasperFiles.addAll(getJrxmlTemplates("modules"));
    } catch (IOException ioex) {

    }
    return allJasperFiles;
  }

  private static Collection<Path> getJrxmlTemplates(String dir) throws IOException {
    final Collection<Path> allJasperFiles = new ArrayList<>();
    final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("regex:.*\\.jrxml");
    Path basePath = Paths.get(
        OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("source.path"),
        dir);
    Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (Files.exists(file) && matcher.matches(file)) {
          try (BufferedReader fileReader = Files.newBufferedReader(file)) {
            while (fileReader.ready()) {
              String line = fileReader.readLine();
              if (line.contains(HWM_TEST_PREFIX)) {
                // Ignore files that contain hwmanager logic as these are not intended to be
                // compiled in backoffice
                log.info("Skipped JRXML file(hwmanager report): " + file.toString());
                return FileVisitResult.CONTINUE;
              }
            }
          } catch (IOException ignore) {
            // Files that were not able to be read and parsed because of an IOException we will
            // try to compile
          }

          allJasperFiles.add(file);
        }
        return FileVisitResult.CONTINUE;
      }
    });
    return allJasperFiles;
  }
}
