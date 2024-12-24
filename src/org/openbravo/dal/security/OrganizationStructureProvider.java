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
 * All portions are Copyright (C) 2008-2023 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.dal.security;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.provider.OBNotSingleton;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.util.Check;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationNodeCache.OrgNode;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.StringCollectionUtils;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Builds a tree of organizations to compute the accessible organizations for the current
 * organizations of a user. Is used to check if references from one object to another are correct
 * from an organization structure perspective.
 * <p>
 * For example a city refers to a country then: an organization of the country (the refered object)
 * must be in the natural tree of the organization of the city (the referee).
 * 
 * @author mtaal
 */

public class OrganizationStructureProvider implements OBNotSingleton {

  final static Logger log = LogManager.getLogger();

  private String clientId;

  /**
   * Map orgId -> orgNode that contains the information of the organization nodes related to the
   * client of this OrganizationStructureProvider
   * 
   * This map is initialized in an instance of OrganizationStructureProvider the first time an org
   * node is retrieved, if there are later changes on the organization structure an
   * OrganizationStructureProvider will not see them unless the reinitialize method is invoked
   */
  private Map<String, OrgNode> orgNodes;

  /**
   * Forces the reinitialization of the organization cache. This method should not be used unless
   * the invoker just updated the organization structure and needs to retrieve the updated
   * information. The organization structure is seldom updated and the organization cache is
   * reinitialized periodically every few minutes, most users of OrganizationStructureProvider do
   * not need to reinitialize it each time the use it
   */
  public void reInitialize() {
    orgNodes = null;
    OrganizationNodeCache.getInstance().clear();
  }

  /**
   * Returns the natural tree of an organization.
   * 
   * @param orgId
   *          the id of the organization for which the natural tree is determined.
   * @return the natural tree of the organization.
   */
  public Set<String> getNaturalTree(String orgId) {
    long t = System.nanoTime();
    OrgNode node = getNode(orgId);
    if (node == null) {
      return new HashSet<>(Arrays.asList(orgId));
    } else {
      Set<String> result = new HashSet<>(getParentTree(orgId, true));
      result.addAll(getChildTree(orgId, false));
      if (log.isTraceEnabled()) {
        log.trace("getNaturalTree {} - {} ms", orgId,
            String.format("%.3f", (System.nanoTime() - t) / 1_000_000d));
      }
      return result;
    }
  }

  /**
   * Checks if an organization (org2) is in the natural tree of another organization (org1).
   * 
   * @param org1
   *          the natural tree of this organization is used to check if org2 is present
   * @param org2
   *          the organization checked in the natural tree of org1
   * @return true if org2 is in the natural tree of org1, false otherwise
   */
  public boolean isInNaturalTree(Organization org1, Organization org2) {
    final String id1 = org1.getId();
    final String id2 = org2.getId();

    // org 0 is in everyones natural tree, and the other way around
    if ("0".equals(id1) || "0".equals(id2)) {
      return true;
    }

    final Set<String> ids = getNaturalTree(id1);
    Check.isNotNull(ids, "Organization with id " + id1
        + " does not have a computed natural tree, does this organization exist?");
    return ids.contains(id2);
  }

  /**
   * Returns the parent organization tree of an organization.
   * 
   * @param orgId
   *          the id of the organization for which the parent organization tree is determined.
   * @param includeOrg
   *          if true, returns also the given organization as part of the tree
   * @return the parent organization tree of the organization.
   */
  public Set<String> getParentTree(String orgId, boolean includeOrg) {
    String parentOrg = getParentOrg(orgId);
    Set<String> result = new HashSet<String>();

    if (includeOrg) {
      result.add(orgId);
    }

    while (parentOrg != null) {
      result.add(parentOrg);
      parentOrg = getParentOrg(parentOrg);
    }
    return result;
  }

  /**
   * Returns an ordered list of parents of an organization. The parents are listed from the
   * organization and up (so parent before grand parent).
   * 
   * @param orgId
   *          the id of the organization for which the parent organization tree is determined.
   * @param includeOrg
   *          if true, returns also the given organization as part of the tree
   * @return the parent organization tree of the organization.
   */
  public List<String> getParentList(String orgId, boolean includeOrg) {
    long t = System.nanoTime();
    String parentOrg = getParentOrg(orgId);
    List<String> result = new ArrayList<String>();

    if (includeOrg) {
      result.add(orgId);
    }

    while (parentOrg != null) {
      result.add(parentOrg);
      parentOrg = getParentOrg(parentOrg);
    }
    if (log.isDebugEnabled()) {
      log.debug("getParentList {} - {} ms", orgId,
          String.format("%.3f", (System.nanoTime() - t) / 1_000_000d));
    }
    return result;
  }

  /**
   * Returns the parent organization of an organization.
   * 
   * @param orgId
   *          the id of the organization for which the parent organization is determined.
   * @return the parent organization.
   */
  public String getParentOrg(String orgId) {
    OrgNode node = getNode(orgId);
    return node == null ? null : node.getParentNodeId();
  }

  /**
   * Returns the parent organization of an organization.
   * 
   * @param org
   *          the organization for which the parent organization is determined.
   * @return the parent organization.
   */
  public Organization getParentOrg(Organization org) {
    String parentOrgId = getParentOrg(org.getId());
    return parentOrgId == null ? null : OBDal.getInstance().get(Organization.class, parentOrgId);
  }

  /**
   * Returns the child organization tree of an organization.
   * 
   * @param orgId
   *          the id of the organization for which the child organization tree is determined.
   * @param includeOrg
   *          if true, returns also the given organization as part of the tree
   * @return the child organization tree of the organization.
   */
  public Set<String> getChildTree(String orgId, boolean includeOrg) {
    OrgNode node = getNode(orgId);
    Set<String> result = new HashSet<>();
    if (includeOrg) {
      result.add(orgId);
    }

    if (node == null) {
      return result;
    }

    Set<String> childOrgs = getChildOrg(orgId);

    for (String co : childOrgs) {
      result.addAll(getChildTree(co, true));
    }
    return result;
  }

  /**
   * Returns the child organizations of an organization.
   * 
   * @param orgId
   *          the id of the organization for which the child organizations are determined.
   * @return the child organizations
   */
  public Set<String> getChildOrg(String orgId) {
    OrgNode node = getNode(orgId);

    if (node == null) {
      return new HashSet<>(0);
    }

    Set<String> os = new HashSet<>(node.getChildren().size());
    for (String child : node.getChildren()) {
      os.add(child);
    }
    return os;
  }

  public String getClientId() {
    if (clientId == null) {
      clientId = OBContext.getOBContext().getCurrentClient().getId();
    }
    return clientId;
  }

  public void setClientId(String clientId) {
    if (this.clientId != null && !this.clientId.equals(clientId)) {
      // if the client is changed, ensure the list of nodes is reinitialized
      orgNodes = null;
    }
    this.clientId = clientId;
  }

  /*
   * Returns the legal entities of the client.
   */
  public List<Organization> getLegalEntitiesList() {
    return getLegalEntitiesListForSelectedClient(clientId);
  }

  /*
   * Returns the legal entities of the selected client.
   */
  public List<Organization> getLegalEntitiesListForSelectedClient(String paramClientId) {
    //@formatter:off
    String where = 
            " as org " +
            "  join org.organizationType as orgType " +
            " where org.client.id = :client " +
            "   and orgType.legalEntity = true";
    //@formatter:on
    OBQuery<Organization> orgQry = OBDal.getInstance()
        .createQuery(Organization.class, where)
        .setFilterOnReadableClients(false)
        .setFilterOnReadableOrganization(false)
        .setNamedParameter("client", paramClientId);
    return orgQry.list();
  }

  /**
   * Returns the legal entity of the given organization
   * 
   * @param org
   *          organization to get its legal entity
   * @return legal entity (with or without accounting) organization or null if not found
   */
  public Organization getLegalEntity(final Organization org) {
    // Admin mode needed to get the Organization type.
    OBContext.setAdminMode(true);
    try {
      for (final String orgId : getParentList(org.getId(), true)) {
        OrgNode node = getNode(orgId);
        if (node != null && node.isLegalEntity) {
          return OBDal.getInstance().get(Organization.class, orgId);
        }
      }
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Returns the list of legal entities that are children of the given organization
   * 
   * @param org
   *          organization to get its child legal entities
   * @return legal entity (with or without accounting) organization or null if not found
   */
  public List<Organization> getChildLegalEntitesList(final Organization org) {
    // Admin mode needed to get the Organization type.
    OBContext.setAdminMode(true);
    List<Organization> childLegalEntitiesList = new ArrayList<Organization>();
    try {
      for (final String orgId : getChildTree(org.getId(), false)) {
        OrgNode node = getNode(orgId);
        if (node != null && node.isLegalEntity) {
          childLegalEntitiesList.add(OBDal.getInstance().get(Organization.class, orgId));
        }
      }
      return childLegalEntitiesList;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Returns the legal entity or Business Unit of the given organization
   * 
   * @param org
   *          organization to get its legal entity or business unit
   * @return legal entity (with or without accounting) organization or null if not found
   */
  public Organization getLegalEntityOrBusinessUnit(final Organization org) {
    // Admin mode needed to get the Organization type.
    OBContext.setAdminMode(true);
    try {
      for (final String orgId : getParentList(org.getId(), true)) {
        OrgNode node = getNode(orgId);
        if (node != null && (node.isLegalEntity || node.isBusinessUnit)) {
          return OBDal.getInstance().get(Organization.class, orgId);
        }
      }
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Returns the organization that is period control allowed for the org Organization. If no
   * organization is found, it returns NULL.
   * 
   * @param org
   *          Organization to get its period control allowed organization.
   */
  public Organization getPeriodControlAllowedOrganization(final Organization org) {
    // Admin mode needed to get the Organization type.
    OBContext.setAdminMode(true);
    try {
      for (final String orgId : getParentList(org.getId(), true)) {
        OrgNode node = getNode(orgId);
        if (node != null && node.isPeriodControlAllowed) {
          return OBDal.getInstance().get(Organization.class, orgId);
        }
      }
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Checks a list of organizations filtering out those ones that do not allow transactions.
   * 
   * @param orgIds
   *          List of organizations to check represented as a comma separated String, elements can
   *          be quoted.
   * @return The list of organization from {@code orgIds} that allow transactions represented a
   *         String of comma separated quoted elements.
   */
  public String getTransactionAllowedOrgs(String orgIds) {
    long t = System.nanoTime();
    try {
      String[] orgs = orgIds.split(",");
      List<String> orgsToCheck = new ArrayList<>(orgs.length);
      for (String orgId : orgs) {
        String fixedOrgId = orgId.startsWith("'") ? orgId.substring(1, orgId.length() - 1) : orgId;
        orgsToCheck.add(fixedOrgId);
      }

      return StringCollectionUtils.commaSeparated(getTransactionAllowedOrgs(orgsToCheck));
    } finally {
      if (log.isDebugEnabled()) {
        log.debug("getTransactionAllowedOrgs - {} ms",
            String.format("%.3f", (System.nanoTime() - t) / 1_000_000d));
      }
    }
  }

  private OrgNode getNode(String nodeId) {
    if (clientId == null) {
      setClientId(OBContext.getOBContext().getCurrentClient().getId());
    }
    if (orgNodes == null) {
      orgNodes = OrganizationNodeCache.getInstance().getNodes(clientId);
    }
    return orgNodes.get(nodeId);
  }

  /**
   * Retrieves the property value from the specified Organization nodeId. It can only retrieve those
   * values that are cached using the OrgNode object.
   *
   * @see OrgNode
   * 
   * @param nodeId - The ID of the Organization to retrieve the info from
   * @param property - The property as defined in OrgNode to retrieve
   * @return An Object containing the value from OrgNode. Null if this value is empty or either the
   *         OrgNode or the property does not exist
   */
  public Object getPropertyFromNode(String nodeId, String property) {
    OrgNode node = getNode(nodeId);
    if (node == null) {
      return null;
    }

    try {
      Field field = OrgNode.class.getDeclaredField(property);
      return field.get(node);
    } catch (Exception e) {
      log.error("Cannot access to property {} in OrgNode", property, e);
      return null;
    }
  }

  private List<String> getTransactionAllowedOrgs(List<String> orgIds) {
    List<String> trxAllowedOrgs = new ArrayList<>(orgIds.size());
    for (String orgId : orgIds) {
      OrgNode node = getNode(orgId);
      if (node != null && node.isReady && node.isTransactionsAllowed) {
        trxAllowedOrgs.add(orgId);
      }
    }
    return trxAllowedOrgs;
  }

  /**
   * Retrieves from a list of {@link BaseOBObject} the one whose organization is the closest in the
   * tree structure to the provided organization. To criteria to determine the closest one is the
   * following:
   * <ol>
   * <li>First we take the closest record in the parent list, if multiple records are found then we
   * sort them by ID and return the first one.</li>
   * <li>If no records are found in the fist step, then we select the record whose organization is
   * present in the child tree. Here it does not make sense to consider the distance, because we can
   * have multiple records with the same distance due to the ramifications that the child tree may
   * have. If multiple records are found, then we sort them by ID and return the first one.</li>
   * <li>If still we have not selected any record after these two steps, which may be the case of
   * the cross organization references, then we just sort the candidate records by its ID and return
   * the first one.</li>
   * </ol>
   * 
   * @param bobs
   *          a collection of objects extending {@link BaseOBObject}
   * @param orgId
   *          the organization ID
   * @return an object extending {@link BaseOBObject} whose organization is closest to the provided
   *         organization. It may be null in case the provided list is empty or if there is no BOB
   *         in that list whose organization is in the natural tree of the given organization or if
   *         the given organization ID is null
   */
  public <T extends BaseOBObject> BaseOBObject getBOBInClosestOrg(Collection<T> bobs,
      String orgId) {
    if (bobs.isEmpty() || orgId == null) {
      return null;
    }
    return getBOBInClosestOrgInParentList(bobs, orgId)
        .orElse(getBOBInClosestOrgInChildTree(bobs, orgId).orElse(null));
  }

  private <T extends BaseOBObject> Optional<BaseOBObject> getBOBInClosestOrgInParentList(
      Collection<T> bobs, String orgId) {
    List<String> parentList = getParentList(orgId, true);

    int min = Integer.MAX_VALUE;
    List<BaseOBObject> closest = new ArrayList<>();
    for (BaseOBObject bob : bobs) {
      String bobOrgId = getOrgId(bob);
      if (bobOrgId == null) {
        continue;
      }
      int position = parentList.indexOf(bobOrgId);
      if (position == -1) {
        continue;
      } else if (min > position) {
        min = position;
        closest.clear();
        closest.add(bob);
      } else if (min == position) {
        closest.add(bob);
      }
    }

    if (closest.isEmpty()) {
      return Optional.empty();
    }

    if (closest.size() == 1) {
      return Optional.of(closest.get(0));
    }

    log.warn("Found multiple records in the parent list of the organization {}: {}", orgId,
        closest.stream().map(bob -> (String) bob.getId()).collect(Collectors.joining(", ")));
    return getFirstRecordOrderedById(closest);
  }

  private <T extends BaseOBObject> Optional<BaseOBObject> getBOBInClosestOrgInChildTree(
      Collection<T> bobs, String orgId) {
    Set<String> childTree = getChildTree(orgId, false);
    List<BaseOBObject> closest = bobs.stream()
        .filter(bob -> getOrgId(bob) != null && childTree.contains(getOrgId(bob)))
        .collect(Collectors.toList());

    if (closest.isEmpty()) {
      return Optional.empty();
    }

    if (closest.size() == 1) {
      return Optional.of(closest.get(0));
    }

    log.warn("Found multiple records in the child tree of the organization {}: {}", orgId,
        closest.stream().map(bob -> (String) bob.getId()).collect(Collectors.joining(", ")));
    return getFirstRecordOrderedById(closest);
  }

  private <T extends BaseOBObject> Optional<T> getFirstRecordOrderedById(Collection<T> bobs) {
    return bobs.stream().sorted(Comparator.comparing(bob -> (String) bob.getId())).findFirst();
  }

  private String getOrgId(BaseOBObject bob) {
    if (Organization.ENTITY_NAME.equals(bob.getEntity().getName())) {
      return (String) bob.getId();
    }
    if (!bob.getEntity().isOrganizationEnabled()) {
      return null;
    }
    Organization org = (Organization) bob.get("organization");
    return org != null ? org.getId() : OBContext.getOBContext().getCurrentOrganization().getId();
  }
}
