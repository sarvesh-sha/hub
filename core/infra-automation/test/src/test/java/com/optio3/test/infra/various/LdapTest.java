/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.infra.various;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchResult;

import com.optio3.infra.LdapHelper;
import com.optio3.infra.WellKnownSites;
import com.optio3.infra.directory.RoleType;
import com.optio3.infra.directory.UserInfo;
import com.optio3.test.infra.Optio3InfraTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class LdapTest extends Optio3InfraTest
{
    @Before
    public void setup() throws
                        Exception
    {
        ensureCredentials(false, false);
    }

    @Ignore("Manually enable to test, since it requires access to LDAP.")
    @Test
    public void testLDAP()
    {
        if (credDir == null)
        {
            return; // Skip test if no credentials
        }

        UserInfo user = credDir.findFirstAutomationUser(WellKnownSites.ldapServer(), RoleType.Administrator);

        try
        {
            try (LdapHelper helper = new LdapHelper(user))
            {
                try
                {
                    System.out.println("getAttributes");
                    Attributes attrs = helper.getAttributes("uid=davide.m,ou=people,dc=optio3,dc=com");

                    dumpAllValues(attrs);
                    System.out.println();
                }
                catch (NamingException e)
                {
                    e.printStackTrace();
                }

                try
                {
                    System.out.println("getAttributes");
                    Attributes attrs = helper.getAttributes("cn=hub,ou=admin,ou=groups,dc=optio3,dc=com");

                    dumpAllValues(attrs);
                    System.out.println();
                }
                catch (NamingException e)
                {
                    e.printStackTrace();
                }

                try
                {
                    System.out.println("Simple search");
                    BasicAttributes matchAttrs = new BasicAttributes(true);
                    matchAttrs.put(new BasicAttribute("mail", "davide.m@optio3.com"));

                    NamingEnumeration<SearchResult> results = helper.search("dc=optio3,dc=com", matchAttrs);
                    while (results.hasMore())
                    {
                        SearchResult result = results.next();

                        Attributes attrs = result.getAttributes();
                        dumpAllValues(attrs);
                        System.out.println();
                    }
                    System.out.println();
                }
                catch (NamingException e)
                {
                    e.printStackTrace();
                }

                try
                {
                    System.out.println("Recursive search");
                    NamingEnumeration<SearchResult> results = helper.searchSubtree("dc=optio3,dc=com", "mail=davide.m@optio3.com");
                    while (results.hasMore())
                    {
                        SearchResult result = results.next();

                        System.out.printf("NAME = %s%n", result.getName());
                        Attributes attrs = result.getAttributes();

                        NamingEnumeration<String> ids = attrs.getIDs();
                        while (ids.hasMore())
                        {
                            String id = ids.next();

                            Attribute attr = attrs.get(id);
                            Object    val  = attr.get();
                            if (val instanceof byte[])
                            {
                                byte[] val2 = (byte[]) val;

                                System.out.printf("%s: ", attr.getID());
                                for (byte aVal2 : val2)
                                {
                                    System.out.printf("%c", aVal2);
                                }
                                System.out.println();
                            }
                            else
                            {
                                System.out.printf("%s: %s%n", attr.getID(), val);
                            }
                        }
                    }
                    System.out.println();
                }
                catch (NamingException e)
                {
                    e.printStackTrace();
                }
            }
        }
        catch (NamingException e)
        {
            e.printStackTrace();
        }
    }

    @Ignore("Manually enable to test, since it requires access to LDAP.")
    @Test
    public void createTestUser()
    {
        if (credDir == null)
        {
            return; // Skip test if no credentials
        }

        UserInfo user = credDir.findFirstAutomationUser(WellKnownSites.ldapServer(), RoleType.Administrator);

        try
        {
            try (LdapHelper helper = new LdapHelper(user))
            {
                String hash = LdapHelper.hashPassword("test");
                assertTrue(LdapHelper.checkPassword("test", hash));

                helper.createUser("ou=people,dc=optio3,dc=com", "testUser", "test", "user", "testUser@mail.com", hash);

                try
                {
                    System.out.println("createTestUser: AFTER");
                    Attributes attrs = helper.getAttributes("uid=testUser,ou=people,dc=optio3,dc=com");

                    dumpAllValues(attrs);
                    System.out.println();
                }
                catch (NamingException e)
                {
                    e.printStackTrace();
                }
            }
        }
        catch (NamingException e)
        {
            e.printStackTrace();
        }
    }

    @Ignore("Manually enable to test, since it requires access to LDAP.")
    @Test
    public void deleteTestUser()
    {
        if (credDir == null)
        {
            return; // Skip test if no credentials
        }

        UserInfo user = credDir.findFirstAutomationUser(WellKnownSites.ldapServer(), RoleType.Administrator);

        try
        {
            try (LdapHelper helper = new LdapHelper(user))
            {
                helper.deleteUser("uid=testUser,ou=people,dc=optio3,dc=com");

                System.out.println("createTestUser: AFTER");
                NamingEnumeration<SearchResult> results = helper.searchSubtree("ou=people,dc=optio3,dc=com", "uid=testUser");
                assertFalse(results.hasMore());
            }
        }
        catch (NamingException e)
        {
            e.printStackTrace();
        }
    }

    @Ignore("Manually enable to test, since it requires access to LDAP.")
    @Test
    public void addToGroup()
    {
        if (credDir == null)
        {
            return; // Skip test if no credentials
        }

        UserInfo user = credDir.findFirstAutomationUser(WellKnownSites.ldapServer(), RoleType.Administrator);

        try
        {
            try (LdapHelper helper = new LdapHelper(user))
            {
                try
                {
                    System.out.println("addToGroup: BEFORE");
                    Attributes attrs = helper.getAttributes("cn=build,ou=admin,ou=groups,dc=optio3,dc=com");

                    dumpAllValues(attrs);
                    System.out.println();
                }
                catch (NamingException e)
                {
                    e.printStackTrace();
                }

                helper.addMemberToGroup("cn=build,ou=admin,ou=groups,dc=optio3,dc=com", "uid=nexus-admin,ou=automation,dc=optio3,dc=com");

                try
                {
                    System.out.println("addToGroup: AFTER");
                    Attributes attrs = helper.getAttributes("cn=build,ou=admin,ou=groups,dc=optio3,dc=com");

                    dumpAllValues(attrs);
                    System.out.println();
                }
                catch (NamingException e)
                {
                    e.printStackTrace();
                }
            }
        }
        catch (NamingException e)
        {
            e.printStackTrace();
        }
    }

    @Ignore("Manually enable to test, since it requires access to LDAP.")
    @Test
    public void removeFromGroup()
    {
        if (credDir == null)
        {
            return; // Skip test if no credentials
        }

        UserInfo user = credDir.findFirstAutomationUser(WellKnownSites.ldapServer(), RoleType.Administrator);

        try
        {
            try (LdapHelper helper = new LdapHelper(user))
            {
                try
                {
                    System.out.println("removeFromGroup: BEFORE");
                    Attributes attrs = helper.getAttributes("cn=build,ou=admin,ou=groups,dc=optio3,dc=com");

                    dumpAllValues(attrs);
                    System.out.println();
                }
                catch (NamingException e)
                {
                    e.printStackTrace();
                }

                helper.removeMemberFromGroup("cn=build,ou=admin,ou=groups,dc=optio3,dc=com", "uid=nexus-admin,ou=automation,dc=optio3,dc=com");

                try
                {
                    System.out.println("removeFromGroup: AFTER");
                    Attributes attrs = helper.getAttributes("cn=build,ou=admin,ou=groups,dc=optio3,dc=com");

                    dumpAllValues(attrs);
                    System.out.println();
                }
                catch (NamingException e)
                {
                    e.printStackTrace();
                }
            }
        }
        catch (NamingException e)
        {
            e.printStackTrace();
        }
    }

    //--//

    private void dumpAllValues(Attributes attrs) throws
                                                 NamingException
    {
        NamingEnumeration<? extends Attribute> allAttrs = attrs.getAll();
        while (allAttrs.hasMore())
        {
            Attribute attr = allAttrs.next();

            NamingEnumeration<?> allValues = attr.getAll();
            while (allValues.hasMore())
            {
                Object val = allValues.next();
                if (val instanceof byte[])
                {
                    byte[] val2 = (byte[]) val;

                    System.out.printf("%s: ", attr.getID());
                    for (byte aVal2 : val2)
                    {
                        System.out.printf("%c", aVal2);
                    }
                    System.out.println();
                }
                else
                {
                    System.out.printf("%s: %s%n", attr.getID(), val);
                }
            }
        }
    }
}
