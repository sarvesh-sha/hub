/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ForbiddenException;

import com.google.common.collect.Maps;
import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.client.hub.api.RolesApi;
import com.optio3.cloud.client.hub.api.UserGroupsApi;
import com.optio3.cloud.client.hub.api.UsersApi;
import com.optio3.cloud.client.hub.model.RecordIdentity;
import com.optio3.cloud.client.hub.model.Role;
import com.optio3.cloud.client.hub.model.User;
import com.optio3.cloud.client.hub.model.UserCreationRequest;
import com.optio3.cloud.client.hub.model.UserGroup;
import com.optio3.cloud.client.hub.model.UserGroupCreationRequest;
import com.optio3.cloud.client.hub.model.ValidationResults;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.exception.NotAuthenticatedException;
import com.optio3.cloud.exception.NotAuthorizedException;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.HubDataDefinition;
import com.optio3.cloud.hub.persistence.config.RoleRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord_;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.ClassRule;
import org.junit.Test;

public class UsersTest extends Optio3Test
{
    @ClassRule
    public static final TestApplicationWithDbRule<HubApplication, HubConfiguration> applicationRule = new TestApplicationWithDbRule<>(HubApplication.class, "hub-test.yml", (configuration) ->
    {
        configuration.data = new HubDataDefinition[] { new HubDataDefinition("demodata/defaultUsers.json", false, false) };
    }, null);

    private static Map<String, RecordIdentity> mapRoles = Maps.newHashMap();
    private static UserGroup                   group1;
    private static UserGroup                   group2;
    private static UserGroup                   group3;

    @Test
    @TestOrder(10)
    public void testUnauthorized()
    {
        UsersApi proxy = applicationRule.createProxy("api/v1", UsersApi.class);

        assertNonAuthorized(() ->
                            {
                                proxy.login(null, null);
                            });
    }

    @Test
    @TestOrder(20)
    public void testLogin()
    {
        UsersApi proxy = applicationRule.createProxy("api/v1", UsersApi.class);

        User user = proxy.login("admin@demo.optio3.com", "adminPwd");
        assertNotNull(user);

        RolesApi proxy_role = applicationRule.createProxy("api/v1", RolesApi.class);
        for (RecordIdentity ri : proxy_role.getAll())
        {
            Role role = proxy_role.get(ri.sysId);
            mapRoles.put(role.name, ri);
        }

        UserGroupsApi            proxy_usergroup = applicationRule.createProxy("api/v1", UserGroupsApi.class);
        UserGroupCreationRequest req             = new UserGroupCreationRequest();

        // Extra spaces to test auto-trim.
        req.name = " TestGroup1";
        group1   = proxy_usergroup.create(req);
        assertEquals("TestGroup1", group1.name);

        req.name = "TestGroup2  ";
        group2   = proxy_usergroup.create(req);
        assertEquals("TestGroup2", group2.name);

        req.name = " TestGroup3 ";
        group3   = proxy_usergroup.create(req);
        assertEquals("TestGroup3", group3.name);

        group1.roles.add(mapRoles.get("SYS.USER"));
        group1.subGroups.add(getRecordIdentity(group2.sysId));
        proxy_usergroup.update(group1.sysId, null, group1);
        group1 = proxy_usergroup.get(group1.sysId);

        {
            UserGroup bad = new UserGroup();
            bad.name  = "foo";
            bad.sysId = group2.sysId;
            bad.subGroups.add(getRecordIdentity(group1.sysId));
            ValidationResults badRes = proxy_usergroup.update(bad.sysId, true, bad);
            assertEquals(1, badRes.entries.size());
            assertInvalidArgument("Can't create loops between groups:", badRes.entries.get(0).reason);
        }

        {
            UserGroup bad = new UserGroup();
            bad.name  = "foo";
            bad.sysId = group2.sysId;
            bad.subGroups.add(getRecordIdentity(group2.sysId));
            ValidationResults badRes = proxy_usergroup.update(bad.sysId, true, bad);
            assertEquals(2, badRes.entries.size());
            assertInvalidArgument("Can't add group to itself:", badRes.entries.get(0).reason);
            assertInvalidArgument("Can't create loops between groups:", badRes.entries.get(1).reason);
        }
    }

    @Test
    @TestOrder(30)
    public void testCheckin()
    {
        UsersApi proxy = applicationRule.createProxy("api/v1", UsersApi.class);

        User user = proxy.login(null, null);
        assertNotNull(user);
        assertEquals("admin@demo.optio3.com", user.emailAddress);
    }

    @Test
    @TestOrder(40)
    public void testLogout()
    {
        UsersApi proxy = applicationRule.createProxy("api/v1", UsersApi.class);

        proxy.logout();

        assertNonAuthorized(() ->
                            {
                                proxy.login(null, null);
                            });
    }

    @Test
    @TestOrder(50)
    public void testLoginWithToken()
    {
        CookiePrincipal principal = applicationRule.getApplication()
                                                   .buildPrincipal("synthetic@local");

        principal.setEmbeddedRolesEx(WellKnownRole.Machine);

        principal.disableAutoRefresh();
        principal.setExpirationDate(Duration.ofSeconds(60));
        UsersApi proxy1 = applicationRule.createProxyWithPrincipal("api/v1", UsersApi.class, principal);
        User     user1  = proxy1.login(null, null);
        assertNotNull(user1);
        assertEquals("synthetic@local", user1.emailAddress);
        assertEquals(1, user1.roles.size());

        proxy1.logout();

        assertNonAuthorized(() ->
                            {
                                proxy1.login(null, null);
                            });

        // Create an already-expired token.
        principal.setExpirationDate(Duration.ofSeconds(-1));
        UsersApi proxy2 = applicationRule.createProxyWithPrincipal("api/v1", UsersApi.class, principal);

        assertNonAuthorized(() ->
                            {
                                proxy2.login(null, null);
                            });
    }

    @Test
    @TestOrder(60)
    public void testListAllUsers()
    {
        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            RecordHelper<UserRecord> helper = holder.createHelper(UserRecord.class);

            for (UserRecord rec_user : helper.listAll())
            {
                System.out.printf("ID: %s%n", rec_user.getSysId());
                System.out.printf("EMAIL: %s%n", rec_user.getEmailAddress());

                for (RoleRecord role : rec_user.getRoles())
                {
                    System.out.printf("  ID: %s%n", role.getSysId());
                    System.out.printf("  NAME: %s%n", role.getName());
                    System.out.printf("  DISPLAYNAME: %s%n", role.getDisplayName());
                }
            }
        }
    }

    @Test
    @TestOrder(70)
    public void testListSelectUser()
    {
        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            RecordHelper<UserRecord> helper = holder.createHelper(UserRecord.class);

            List<UserRecord> list = QueryHelperWithCommonFields.filter(helper, (jh) ->
            {
                jh.addWhereClauseWithEqual(jh.root, UserRecord_.emailAddress, "admin@demo.optio3.com");
            });

            for (UserRecord rec_user : list)
            {
                System.out.printf("ID: %s%n", rec_user.getSysId());
                System.out.printf("EMAIL: %s%n", rec_user.getEmailAddress());
            }
        }
    }

    @Test
    @TestOrder(80)
    public void testChangePassword()
    {
        UsersApi proxy = applicationRule.createProxy("api/v1", UsersApi.class);

        proxy.logout();
        User admin = proxy.login("admin@demo.optio3.com", "adminPwd");

        proxy.logout();
        User user = proxy.login("user@demo.optio3.com", "userPwd");

        try
        {
            proxy.changePassword(admin.sysId, "userPwd", "userPwd2");
            fail();
        }
        catch (NotAuthorizedException e)
        {
            assertEquals("Can't reset password of another user", e.getMessage());
        }

        proxy.changePassword(user.sysId, "userPwd", "userPwd2");

        proxy.logout();
        proxy.login("user@demo.optio3.com", "userPwd2");

        proxy.logout();
        proxy.login("admin@demo.optio3.com", "adminPwd");

        proxy.changePassword(user.sysId, null, "userPwd");
    }

    @Test
    @TestOrder(90)
    public void testNewUser()
    {
        UsersApi proxy = applicationRule.createProxy("api/v1", UsersApi.class);

        proxy.logout();
        User user = proxy.login("user@demo.optio3.com", "userPwd");

        try
        {
            UserCreationRequest newUser = createTestUser();
            proxy.create(newUser);
            fail();
        }
        catch (ForbiddenException ex)
        {
            // Success.
        }

        proxy.logout();
        User admin = proxy.login("admin@demo.optio3.com", "adminPwd");

        UserCreationRequest newUser     = createTestUser();
        User                createdUser = proxy.create(newUser);

        assertEquals(newUser.firstName, createdUser.firstName);
        assertEquals(newUser.lastName, createdUser.lastName);
        assertEquals(newUser.phoneNumber, createdUser.phoneNumber);
        assertEquals(newUser.emailAddress, createdUser.emailAddress);
        assertEquals(0, newUser.roles.size());

        //
        // Test assigning new role.
        //
        RecordIdentity       riRole = new RecordIdentity();
        final RecordIdentity riUser = mapRoles.get(WellKnownRole.User.getId());
        assertNotNull(riUser);
        createdUser.roles.add(riUser);
        proxy.update(createdUser.sysId, null, createdUser);
        createdUser = proxy.get(createdUser.sysId);

        RolesApi   proxy2    = applicationRule.createProxy("api/v1", RolesApi.class);
        List<Role> roleLists = proxy2.getBatch(CollectionUtils.transformToList(createdUser.roles, (ri) -> ri.sysId));
        assertEquals(1, roleLists.size());
        assertEquals(WellKnownRole.User.getId(), roleLists.get(0).name);

        //
        // Test logging in.
        //
        proxy.logout();
        User user2 = proxy.login(newUser.emailAddress, newUser.password);
        assertEquals(createdUser.sysId, user2.sysId);
    }

    @Test
    @TestOrder(100)
    public void testUserUpdate()
    {
        UsersApi proxy = applicationRule.createProxy("api/v1", UsersApi.class);

        proxy.logout();
        User user = proxy.login("user@demo.optio3.com", "userPwd");

        List<User> users = proxy.getAll();
        for (User otherUser : users)
        {
            if (StringUtils.equals(otherUser.sysId, user.sysId))
            {
                otherUser.groups.add(getRecordIdentity(group1.sysId));
                otherUser.phoneNumber = "555-555-4444";
                proxy.update(otherUser.sysId, null, otherUser);
                User updatedUser = proxy.get(otherUser.sysId);
                assertEquals(0, updatedUser.groups.size()); // Users can't update their groups
                assertEquals(otherUser.phoneNumber, updatedUser.phoneNumber);
            }
            else
            {
                ValidationResults validation = proxy.update(otherUser.sysId, true, otherUser);
                assertEquals(1, validation.entries.size());
                assertInvalidArgument("Not authorized to change another user's details", validation.entries.get(0).reason);
            }
        }

        proxy.logout();
        User admin = proxy.login("admin@demo.optio3.com", "adminPwd");

        for (User otherUser : users)
        {
            if (StringUtils.equals(otherUser.sysId, admin.sysId))
            {
                otherUser.roles.clear();

                ValidationResults validation = proxy.update(otherUser.sysId, true, otherUser);
                assertEquals(1, validation.entries.size());
                assertInvalidArgument("Can't remove ADMIN role from yourself", validation.entries.get(0).reason);
            }
            else
            {
                otherUser.groups.add(getRecordIdentity(group1.sysId));
                otherUser.phoneNumber = "555-555-5555";
                proxy.update(otherUser.sysId, null, otherUser);
                User updatedUser = proxy.get(otherUser.sysId);
                assertEquals(1, updatedUser.groups.size()); // Admins can update groups
                assertEquals(otherUser.phoneNumber, updatedUser.phoneNumber);
            }
        }
    }

    @Test
    @TestOrder(110)
    public void testUserDelete()
    {
        UsersApi proxy = applicationRule.createProxy("api/v1", UsersApi.class);

        proxy.logout();
        User user = proxy.login("user@demo.optio3.com", "userPwd");

        try
        {
            proxy.remove(user.sysId, null);
            fail();
        }
        catch (ForbiddenException ex)
        {
            // Success.
        }

        proxy.logout();
        User admin = proxy.login("admin@demo.optio3.com", "adminPwd");

        try
        {
            proxy.remove(admin.sysId, null);
            fail();
        }
        catch (InvalidStateException e)
        {
            assertEquals(1, e.validationErrors.entries.size());
            assertEquals("Can't delete yourself", e.validationErrors.entries.get(0).reason);
        }

        proxy.remove(user.sysId, null);
    }

    //--//

    private UserCreationRequest createTestUser()
    {
        UserCreationRequest newUser = new UserCreationRequest();
        newUser.firstName    = "firstName";
        newUser.lastName     = "lastName";
        newUser.emailAddress = "emailAddress@test.com";
        newUser.phoneNumber  = "555-555-5555";
        newUser.password     = "testPassword";
        return newUser;
    }

    //--//

    private static RecordIdentity getRecordIdentity(String sysId)
    {
        RecordIdentity ri = new RecordIdentity();
        ri.sysId = sysId;
        return ri;
    }

    private void assertNonAuthorized(Runnable task)
    {
        try
        {
            task.run();
            fail();
        }
        catch (NotAuthenticatedException ex)
        {
            System.out.printf("Got expected exception: %s%n", ex.getMessage());
        }
    }

    private void assertInvalidArgument(String expectedMessage,
                                       String message)
    {
        if (expectedMessage != null && !message.startsWith(expectedMessage))
        {
            fail(String.format("Incorrect message for expected exception: '%s' instead of '%s...'", message, expectedMessage));
        }
    }
}
