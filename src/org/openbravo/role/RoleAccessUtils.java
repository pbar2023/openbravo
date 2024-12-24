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
 * All portions are Copyright (C) 2023 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.role;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.query.Query;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.model.ad.access.Role;

/**
 * Utility class that contains method related to the access granted to roles
 */
public class RoleAccessUtils {

  // This map determines the access level granted for eaceh user level
  private static Map<String, List<String>> ACCESS_LEVEL_FOR_USER_LEVEL = Map.of( //
      "S", List.of("4", "7", "6"), //
      " CO", List.of("7", "6", "3", "1"), //
      " C", List.of("7", "6", "3", "1"), //
      "  O", List.of("3", "1", "7"));

  /**
   * Given a role id, returns a boolean the specifies whether the role is automatic
   * 
   * @return true if role is automatic, false if it is manual
   */
  public static boolean isAutoRole(String roleId) {
    // @formatter:off
    final String roleQryStr = "select r.manual" + 
                              "  from ADRole r" +
                              " where r.id= :targetRoleId" +
                              "   and r.active= 'Y'";
    // @formatter:on
    final Query<Boolean> qry = SessionHandler.getInstance()
        .createQuery(roleQryStr, Boolean.class)
        .setParameter("targetRoleId", roleId);
    return !qry.uniqueResult();
  }

  /**
   * @return String userLevel based on role ID.
   */
  public static String getUserLevel(String roleId) {
    // @formatter:off
    final String roleQryStr = "select r.userLevel" +
                              "  from ADRole r" +
                              " where r.id= :targetRoleId" +
                              "   and r.active= 'Y'";
    // @formatter:on
    final Query<String> qry = SessionHandler.getInstance()
        .createQuery(roleQryStr, String.class)
        .setParameter("targetRoleId", roleId);
    return qry.uniqueResult();
  }

  /**
   * Returns the expected table access levels for a given user level
   *
   * <pre>
   * Table Access Level:
   * "6" -> "System/Client"
   * "1" -> "Organization"
   * "3" -> "Client/Organization"
   * "4" -> "System only"
   * "7" -> "All"
   *
   * User level:
   * "S"    ->  "System"
   * " C"   ->  "Client"
   * "  O"   ->  "Organization"
   * " CO"  ->  "Client+Organization"
   * </pre>
   *
   * @param userLevel
   *          User Level ("S", " C", " O", " CO")
   * @return List of access levels corresponding to the user level
   */
  public static List<String> getAccessLevelForUserLevel(String userLevel) {
    return ACCESS_LEVEL_FOR_USER_LEVEL.get(userLevel);
  }

  /**
   * Get Organizations List for automatic Role based on "SCO" userLevel.
   * 
   * @param role
   *          the role in order to get client and provide Organization List.
   */
  public static List<String> getOrganizationsForAutoRoleByClient(Role role) {
    return getOrganizationsForAutoRoleByClient(role.getClient().getId(), role.getId());
  }

  /**
   * Get Organizations List for automatic Role based on "SCO" userLevel.
   * 
   * @param clientId
   *          the clientId reference to provide Organization list.
   * @param roleId
   *          the roleId which is checked to avoid inactive Manual Organization access inserts.
   * @return List<String> of Organizations IDs
   */
  public static List<String> getOrganizationsForAutoRoleByClient(String clientId, String roleId) {
    String userLevel = getUserLevel(roleId);
    List<String> organizations = new ArrayList<>();

    // " CO" Client/Organization level: *, other Orgs (but *)
    // " O" Organization level: Orgs (but *) [isOrgAdmin=Y]
    if (StringUtils.equals(userLevel, " CO") || StringUtils.equals(userLevel, "  O")) {
      // @formatter:off
      final String orgsQryStr = "select o.id" +
                                "  from Organization o" +
                                " where o.client.id= :clientId" +
                                "   and o.id <>'0'" +
                                "   and o.active= 'Y' " +
                                "   and not exists ( " +
                                "     select 1 " +
                                "       from ADRoleOrganization roa where (o.id=roa.organization.id)" +
                                "        and roa.role.id= :roleId" +
                                "        and roa.active= 'N')" +
                                " order by o.id desc";
      // @formatter:on
      final Query<String> qry = SessionHandler.getInstance()
          .createQuery(orgsQryStr, String.class)
          .setParameter("clientId", clientId)
          .setParameter("roleId", roleId);
      organizations.addAll(qry.list());
    }

    // Client or System level: Only *
    if (StringUtils.equals(userLevel, " C") || StringUtils.equals(userLevel, "S")
        || StringUtils.equals(userLevel, " CO")) {
      // @formatter:off
    	final String adminOrgQryStr = 
                 "select roa.id " +
                 "  from ADRoleOrganization roa "+
                 "  where roa.organization.id = '0'" +
                 "        and roa.role.id= :roleId" +
                 "        and roa.active= 'N'";
      // @formatter:on
      final Query<String> qry = SessionHandler.getInstance()
          .createQuery(adminOrgQryStr, String.class)
          .setParameter("roleId", roleId);
      if (qry.list().isEmpty()) {
        organizations.add("0");
      }
    }
    return organizations;
  }
}
