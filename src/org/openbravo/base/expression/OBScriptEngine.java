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
 * All portions are Copyright (C) 2018-2023 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.base.expression;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.ScriptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

/**
 * Class that wraps a ScriptEngine and that should be used to evaluate javascript scripts
 * 
 * It is a singleton, and it takes advantage of the thread safety of ScriptEngine
 * 
 */
public class OBScriptEngine {
  private static final Logger log = LogManager.getLogger();
  private Map<String, Script> scriptCache = new ConcurrentHashMap<>();
  private ScriptableObject sharedScope;

  private static OBScriptEngine instance = new OBScriptEngine();

  public static OBScriptEngine getInstance() {
    return instance;
  }

  private OBScriptEngine() {
    Context cx = Context.enter();
    try {
      sharedScope = cx.initStandardObjects(null, true);
    } finally {
      Context.exit();
    }
  }

  public Object eval(String script) throws ScriptException {
    return eval(script, Collections.emptyMap());
  }

  public Object eval(String script, Map<String, Object> properties) throws ScriptException {
    Object result = null;

    Context cx = Context.enter();
    try {
      Script compiledScript;
      try {
        compiledScript = scriptCache.computeIfAbsent(script, scriptDef -> {
          Script compileScriptToCache = cx.compileString(scriptDef, "js", 0, null);
          log.debug("Cached script: {}", scriptDef);
          return compileScriptToCache;
        });
      } catch (Exception e) {
        log.error("Error compiling script: {}", script, e);
        throw new ScriptException(e);
      }

      try {
        Scriptable threadScope = cx.newObject(sharedScope);
        threadScope.setPrototype(sharedScope);
        threadScope.setParentScope(null);

        for (Entry<String, Object> entry : properties.entrySet()) {
          threadScope.put(entry.getKey(), threadScope, entry.getValue());
        }

        result = compiledScript.exec(cx, threadScope);
        if (result instanceof NativeJavaObject) {
          result = ((NativeJavaObject) result).unwrap();
        }
      } catch (Exception e) {
        log.error("Error evaluating script: {}", script, e);
        throw new ScriptException(e);
      }
    } finally {
      Context.exit();
    }

    // Sometimes rhino evaluates to "undefined" when it should evaluate to null
    // This transforms all undefined results to null
    // Related issue: https://github.com/mozilla/rhino/issues/760
    if ("undefined".equals(result) || result instanceof Undefined) {
      return null;
    }
    return result;
  }
}
