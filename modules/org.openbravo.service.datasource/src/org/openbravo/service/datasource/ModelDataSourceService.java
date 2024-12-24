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
 * All portions are Copyright (C) 2010-2022 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.datasource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.service.json.AdditionalPropertyResolver;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonUtils;

/**
 * A data source which provides the data for a field which refers to properties in the data model.
 * 
 * @author mtaal
 * @author iperdomo
 */
public class ModelDataSourceService extends BaseDataSourceService {

  private static final String PROPERTY_FIELD = "inpproperty";
  private static final String DATASOURCE_FIELD = "property";
  private static final String FORM_FIELD = "inpadTableId";
  private static final String UNSUPPORTED_OPERATION_MESSAGE = "This operation is not supported by this data source implementation";

  private static final Logger log = LogManager.getLogger();
  private static final Property identifier = new Property();

  static {
    // Setting identifier property name
    identifier.setName(JsonConstants.IDENTIFIER);
  }

  @Override
  public String add(Map<String, String> parameters, String content) {
    throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
  }

  @Override
  public String fetch(Map<String, String> parameters) {

    final Entity baseEntity = getBaseEntity(parameters);
    String propertyPath;

    // filter based on criteria
    Map<String, String> criteria = getCriteria(parameters);
    if (!criteria.isEmpty() && criteria.containsKey(DATASOURCE_FIELD)) {
      propertyPath = criteria.get(DATASOURCE_FIELD);
    } else {
      // when there is no criteria present, filter based on field's value
      propertyPath = parameters.get(PROPERTY_FIELD);
      if ("null".equals(propertyPath) || propertyPath == null) {
        propertyPath = "";
      }
    }

    if (baseEntity == null) {
      // The first request doesn't contain the adTableId
      // that's why baseEntity is null
      final List<Property> baseEntityProperties = new ArrayList<>();
      if (propertyPath != null) {
        final Property savedPath = new Property();
        savedPath.setName(propertyPath);
        baseEntityProperties.add(savedPath);
      }
      try {
        return getJSONResponse(baseEntityProperties, Collections.emptySet(), "", 0);
      } catch (JSONException e) {
        log.error("Error building JSON response: {}", e.getMessage(), e);
        return JsonUtils.getEmptyResult();
      }
    }

    int startRow = 0;

    if (propertyPath == null || propertyPath.equals("")) {
      try {
        return getJSONResponse(getEntityProperties(baseEntity), getAdditionalProperties(baseEntity),
            "", 0);
      } catch (JSONException e) {
        log.error("Error building JSON response: {}", e.getMessage(), e);
        return JsonUtils.getEmptyResult();
      }
    }

    if (propertyPath.lastIndexOf("..") != -1) {
      return JsonUtils.getEmptyResult();
    }

    final boolean endsWithDot = propertyPath.endsWith(".");

    try {
      final String[] parts = propertyPath.split("\\.");
      Entity currentEntity = baseEntity;
      Property currentProperty = null;
      List<Property> props = new ArrayList<>();
      Set<String> additionalProps = Collections.emptySet();
      int currentDepth = 0;
      int pathDepth = parts.length;

      boolean getAllProperties = (propertyPath.lastIndexOf('.') == propertyPath.length() - 1);
      int index = 0;
      for (String part : parts) {

        final boolean lastPart = index == (parts.length - 1);
        currentDepth++;

        boolean propNotFound = true;
        final List<Property> currentEntityProperties;
        if (currentProperty != null
            && Entity.COMPUTED_COLUMNS_PROXY_PROPERTY.equals(currentProperty.getName())) {
          // for computed columns get computed properties
          currentEntity = currentProperty.getEntity();
          currentEntityProperties = currentEntity.getComputedColumnProperties();
        } else {
          // other case get properties from entity
          currentEntityProperties = getEntityProperties(currentEntity);
        }

        for (Property prop : currentEntityProperties) {
          boolean tryProperty = false;
          if (lastPart && endsWithDot) {
            tryProperty = prop.getName().equalsIgnoreCase(part.toLowerCase());
          } else {
            tryProperty = prop.getName().toLowerCase().startsWith(part.toLowerCase());
          }
          if (tryProperty) {
            if (prop.getName().equals(JsonConstants.IDENTIFIER)) {
              currentProperty = identifier;
            } else {
              currentProperty = currentEntity.getProperty(prop.getName());
            }
            propNotFound = false;
            if (currentDepth != pathDepth) {
              // Breaking loop to continue with next property in the path
              break;
            }
            props.add(prop);
          }
        }

        if (currentDepth == pathDepth) {
          additionalProps = getAdditionalProperties(currentEntity,
              p -> p.toLowerCase().startsWith(part.toLowerCase()));
        }

        if (propNotFound && additionalProps.isEmpty()) {
          return JsonUtils.getEmptyResult();
        }

        List<Property> computedColProperties = null;
        if (getAllProperties && currentProperty != null
            && Entity.COMPUTED_COLUMNS_PROXY_PROPERTY.equals(currentProperty.getName())) {
          computedColProperties = currentEntity != null
              ? currentEntity.getComputedColumnProperties()
              : Collections.emptyList();
        } else if (currentProperty != null) {
          currentEntity = currentProperty.getTargetEntity();
        }

        if (currentDepth == pathDepth && getAllProperties) {
          // User just pressed a final dot (.) key - getting all properties
          // of current Entity
          if (computedColProperties != null) {
            return getJSONResponse(computedColProperties, Collections.emptySet(), propertyPath, 0);
          } else if (currentEntity != null) {
            return getJSONResponse(getEntityProperties(currentEntity),
                getAdditionalProperties(currentEntity), propertyPath, 0);
          }
        }
        index++;
      }

      if (getAllProperties && !props.isEmpty() && props.get(0).getTargetEntity() == null) {
        return JsonUtils.getEmptyResult();
      }

      return getJSONResponse(props, additionalProps, propertyPath, startRow);

    } catch (JSONException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public String remove(Map<String, String> parameters) {
    throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
  }

  @Override
  public String update(Map<String, String> parameters, String content) {
    throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
  }

  /**
   * Returns an entity based on the table record id. Returns null if the ipadTableId input is not
   * present or no Entity is found for a given table id.
   * 
   * @param parameters
   *          Map of the parameters from the request
   * @return the Entity or null if not found
   */
  protected Entity getBaseEntity(Map<String, String> parameters) {
    final String tableId = parameters.get(FORM_FIELD);
    if (tableId == null) {
      return null;
    }
    return ModelProvider.getInstance().getEntityByTableId(tableId);
  }

  /**
   * Returns the list of properties sorted alphabetically and with a extra _identifier property
   * 
   * @param entity
   *          the parent Entity from which the Property will be extracted
   * @return a list of properties plus an extra _identifier property
   */
  protected List<Property> getEntityProperties(Entity entity) {

    final List<Property> entityProperties = new ArrayList<>();
    // Appending identifier property
    entityProperties.add(identifier);
    entityProperties.addAll(entity.getProperties());

    return entityProperties;
  }

  private Set<String> getAdditionalProperties(Entity entity) {
    return getAdditionalProperties(entity, p -> true);
  }

  private Set<String> getAdditionalProperties(Entity entity, Predicate<String> filter) {
    return WeldUtils.getInstancesSortedByPriority(AdditionalPropertyResolver.class)
        .stream()
        .map(resolver -> resolver.getPropertyNames(entity))
        .flatMap(Collection::stream)
        .filter(filter)
        .collect(Collectors.toSet());
  }

  /**
   * Returns a JSON string representation of the properties matched based on user input
   * 
   * @param entityProperties
   *          the list of entity properties to be transformed into JSON representation
   * @param additionalProperties
   *          a set of additional property names to be included also as part of the result
   * @param propertyPath
   *          the user's request input string
   * @param startRow
   *          the start index. Used for paging
   * @return a JSON string response for the client
   * @throws JSONException
   */
  private String getJSONResponse(List<Property> entityProperties, Set<String> additionalProperties,
      String propertyPath, int startRow) throws JSONException {
    final JSONObject jsonResult = new JSONObject();
    final JSONObject jsonResponse = new JSONObject();

    List<JSONObject> properties = convertToJSONObjects(entityProperties, propertyPath);
    properties.addAll(convertToJSONObjects(additionalProperties, propertyPath));
    Collections.sort(properties, new PropertyNameComparator());

    jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
    jsonResponse.put(JsonConstants.RESPONSE_STARTROW, startRow);
    jsonResponse.put(JsonConstants.RESPONSE_ENDROW, properties.size() + startRow - 1);
    jsonResponse.put(JsonConstants.RESPONSE_TOTALROWS, properties.size());
    jsonResponse.put(JsonConstants.RESPONSE_DATA, new JSONArray(properties));
    jsonResult.put(JsonConstants.RESPONSE_RESPONSE, jsonResponse);

    return jsonResult.toString();
  }

  /**
   * Converts a List of {@link org.openbravo.base.model.Property properties} into JSON objects
   * 
   * @param properties
   *          the list of properties
   * @param propertyPath
   *          the user request input
   * @return a list of JSONObjects
   */
  private List<JSONObject> convertToJSONObjects(List<Property> properties, String propertyPath) {
    final int pos = propertyPath.lastIndexOf('.');
    String propertyPrefix = pos != -1 ? propertyPath.substring(0, pos + 1) : "";

    return properties.stream()
        .map(property -> convertToJSONObject(property, propertyPrefix))
        .collect(Collectors.toList());
  }

  private List<JSONObject> convertToJSONObjects(Set<String> propertyNames, String propertyPath) {
    final int pos = propertyPath.lastIndexOf('.');
    String propertyPrefix = pos != -1 ? propertyPath.substring(0, pos + 1) : "";

    return propertyNames.stream()
        .map(property -> convertToJSONObject(property, propertyPrefix))
        .collect(Collectors.toList());
  }

  /**
   * Converts a Property into its JSONObject representation.<br>
   * Note: The JSONObject representation takes only the property name
   * 
   * @param property
   *          the {@link org.openbravo.base.model.Property Property} to convert
   * @param propertyPrefix
   *          the prefix that will be appended to the property name
   * @return a JSONObject representation of the Property
   */
  private JSONObject convertToJSONObject(Property property, String propertyPrefix) {
    return convertToJSONObject(property.getName(), propertyPrefix);
  }

  private JSONObject convertToJSONObject(String propertyName, String propertyPrefix) {
    final JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put(DATASOURCE_FIELD, propertyPrefix + propertyName);
    } catch (JSONException e) {
      throw new IllegalStateException(e);
    }
    return jsonObject;
  }

  private Map<String, String> getCriteria(Map<String, String> parameters) {
    if (!"AdvancedCriteria".equals(parameters.get("_constructor"))) {
      return Collections.emptyMap();
    }
    Map<String, String> criteriaValues = new HashMap<>();
    try {
      JSONArray criterias = (JSONArray) JsonUtils.buildCriteria(parameters).get("criteria");
      for (int i = 0; i < criterias.length(); i++) {
        final JSONObject criteria = criterias.getJSONObject(i);
        criteriaValues.put(criteria.getString("fieldName"), criteria.getString("value"));
      }
    } catch (JSONException e) {
      // Ignore exception.
    }
    if (criteriaValues.isEmpty()) {
      return Collections.emptyMap();
    }
    return criteriaValues;
  }

  /**
   * Compares 2 JSONObjects based on the value of their property named "property"
   * 
   * @author iperdomo
   */
  private static class PropertyNameComparator implements Comparator<JSONObject> {

    @Override
    public int compare(JSONObject o1, JSONObject o2) {
      try {
        return o1.getString(DATASOURCE_FIELD).compareTo(o2.getString(DATASOURCE_FIELD));
      } catch (JSONException ex) {
        log.error("Could not compare {} with {}", o1, o2);
        throw new IllegalArgumentException("JSONObject comparison failed", ex);
      }
    }
  }
}
