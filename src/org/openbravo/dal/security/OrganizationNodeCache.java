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
 * All portions are Copyright (C) 2022 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.dal.security;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.query.NativeQuery;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.provider.OBSingleton;
import org.openbravo.cache.TimeInvalidatedCache;
import org.openbravo.dal.service.OBDal;

/**
 * Cache that stores the hierarchical information of Openbravo organizations. It uses a
 * TimeInvalidatedCache to avoid initializing this information too often. If this information
 * changes (i.e. a new organization is added to Openbravo or an existing organization is moved in
 * the hierarchy, some time will passed until the update is reflected on the cache (see
 * expireAfterDuration in cache builder)
 */
public class OrganizationNodeCache implements OBSingleton {

  private static OrganizationNodeCache instance;

  private static TimeInvalidatedCache<String, Map<String, OrgNode>> orgNodesCache = TimeInvalidatedCache
      .newBuilder()
      .name("OSP_OrgCache")
      .expireAfterDuration(Duration.ofMinutes(5))
      .build(OrganizationNodeCache::initialize);

  private static final Logger log = LogManager.getLogger();

  /**
   * @return the singleton instance of OrganizationNodeCache
   */
  public static OrganizationNodeCache getInstance() {
    if (instance == null) {
      instance = OBProvider.getInstance().get(OrganizationNodeCache.class);
    }
    return instance;
  }

  private static Map<String, OrgNode> initialize(String clientId) {

    long t = System.nanoTime();

    // Read all org tree of any client: bypass DAL to prevent security checks and Hibernate to make
    // it in a single query. Using direct SQL managed by Hibernate as in this point SQLC is not
    // allowed because this code is used while generating entities.
    //@formatter:off
    String sql = 
            "select n.node_id, n.parent_id, o.isready, ot.islegalentity, ot.isbusinessunit, ot.istransactionsallowed, o.isperiodcontrolallowed, o.timezone" +
            "  from ad_tree t, ad_treenode n, ad_org o, ad_orgtype ot" +
            " where n.node_id = o.ad_org_id " +
            "   and o.ad_orgtype_id = ot.ad_orgtype_id" +
            "   and n.ad_tree_id = t.ad_tree_id" +
            "   and t.ad_table_id = '155'" +
            "   and t.ad_client_id = :clientId";
    //@formatter:on

    @SuppressWarnings("rawtypes")
    NativeQuery qry = OBDal.getInstance()
        .getSession()
        .createNativeQuery(sql)
        .setParameter("clientId", clientId);

    @SuppressWarnings("unchecked")
    List<Object[]> treeNodes = qry.list();

    Map<String, OrgNode> orgNodes = new HashMap<>(treeNodes.size());
    for (Object[] nodeDef : treeNodes) {
      final OrgNode on = new OrgNode(nodeDef);
      String nodeId = (String) nodeDef[0];
      orgNodes.put(nodeId, on);
    }

    for (Entry<String, OrgNode> nodeEntry : orgNodes.entrySet()) {
      nodeEntry.getValue().resolve(orgNodes, nodeEntry.getKey());
    }

    log.debug("Client {} initialized in {} ms", clientId,
        String.format("%.3f", (System.nanoTime() - t) / 1_000_000d));
    return orgNodes;
  }

  /**
   * Returns the organization nodes of a given client
   * 
   * @param clientId
   *          the client id
   * @return a map with the stored organization nodes for the given client
   */
  Map<String, OrgNode> getNodes(String clientId) {
    return orgNodesCache.get(clientId);
  }

  /**
   * Clears the cache, so that next time org data for a client is requested, it will be
   * reinitialized
   */
  void clear() {
    orgNodesCache.invalidateAll();
  }

  static class OrgNode {
    private String parentNodeId;
    boolean isReady;
    boolean isLegalEntity;
    boolean isBusinessUnit;
    boolean isTransactionsAllowed;
    boolean isPeriodControlAllowed;
    String timeZoneId;

    private List<String> children = new ArrayList<>();

    void addChild(String childId) {
      children.add(childId);
    }

    OrgNode(Object[] nodeDef) {
      parentNodeId = (String) nodeDef[1];
      isReady = Objects.equals('Y', nodeDef[2]);
      isLegalEntity = Objects.equals('Y', nodeDef[3]);
      isBusinessUnit = Objects.equals('Y', nodeDef[4]);
      isTransactionsAllowed = Objects.equals('Y', nodeDef[5]);
      isPeriodControlAllowed = Objects.equals('Y', nodeDef[6]);
      timeZoneId = (String) nodeDef[7];
    }

    void resolve(Map<String, OrgNode> orgNodes, String nodeId) {
      OrgNode parentNode = parentNodeId != null ? orgNodes.get(parentNodeId) : null;
      if (parentNode != null) {
        parentNode.addChild(nodeId);
      }
    }

    String getParentNodeId() {
      return parentNodeId;
    }

    List<String> getChildren() {
      return children;
    }
  }
}
