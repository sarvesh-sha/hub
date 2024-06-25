/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.cli;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.JCommander.Builder;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.google.common.collect.Maps;
import com.optio3.infra.LdapHelper;
import com.optio3.infra.WellKnownSites;
import com.optio3.infra.directory.CredentialDirectory;
import com.optio3.infra.directory.RoleType;
import com.optio3.infra.directory.UserInfo;
import com.optio3.util.Exceptions;
import org.apache.commons.lang3.StringUtils;

public class Ldap
{
    static class EmailValidator implements IValueValidator<String>
    {
        @Override
        public void validate(String name,
                             String value) throws
                                           ParameterException
        {
            if (!value.contains("@"))
            {
                throw new ParameterException("Not a valid email address");
            }
        }
    }

    public abstract class BaseCommand
    {
        public abstract void exec() throws
                                    Exception;
    }

    public abstract class AdminBaseCommand extends BaseCommand
    {
        @Override
        public void exec() throws
                           Exception
        {
            try (LdapHelper helper = new LdapHelper(ensureAdminUser()))
            {
                exec(helper);
            }
        }

        protected abstract void exec(LdapHelper helper) throws
                                                        Exception;
    }

    @Parameters(commandNames = "list", commandDescription = "list all users and groups")
    public class CommandListAdmin extends AdminBaseCommand
    {
        @Override
        protected void exec(LdapHelper helper) throws
                                               Exception
        {
            Map<String, Attributes> users  = Maps.newHashMap();
            Map<String, Attributes> groups = Maps.newHashMap();

            NamingEnumeration<SearchResult> results = helper.searchSubtree(LdapHelper.LDAP_SUFFIX, "cn=*");
            while (results.hasMore())
            {
                SearchResult result = results.next();

                String name = result.getNameInNamespace();

                if (verbose)
                {
                    System.out.println();
                    System.out.printf("NAME = %s%n", name);
                }

                Attributes attrs = result.getAttributes();

                if (verbose)
                {
                    dumpAllAttributes(attrs, "  ");
                }

                String objectClass = helper.getValue(attrs, LdapHelper.OBJECT_CLASS);
                switch (objectClass)
                {
                    case LdapHelper.OBJECT_CLASS__GROUPOFNAMES:
                        groups.put(name, attrs);
                        break;

                    case LdapHelper.OBJECT_CLASS__INETORGPERSON:
                        users.put(name, attrs);
                        break;
                }
            }

            System.out.println("Users:");
            for (String name : users.keySet())
            {
                Attributes attrs = users.get(name);
                System.out.printf("  %s: %s %s%n", helper.getValue(attrs, LdapHelper.MAIL), helper.getValue(attrs, LdapHelper.GIVENNAME), helper.getValue(attrs, LdapHelper.SN));
            }
            System.out.println();

            System.out.println("Groups:");
            for (String name : groups.keySet())
            {
                Attributes attrs = groups.get(name);
                System.out.printf("  %s: %s%n", name, helper.getValue(attrs, LdapHelper.DESCRIPTION));

                for (String member : helper.getMembersOfGroup(name))
                    System.out.printf("     %s%n", member);
                System.out.println();
            }
        }
    }

    @Parameters(commandNames = "changePasswd", commandDescription = "change user password (use '-' to be asked for password)")
    public class CommandChangePassword extends BaseCommand
    {
        @Parameter(names = "--asAdmin", description = "Login as admin to change someone else's password")
        public boolean asAdmin;

        @Parameter(description = "<email address> <old password> <new password>", required = true)
        public List<String> args;

        @Override
        public void exec() throws
                           Exception
        {
            String emailAddress = args.get(0);
            String oldPassword  = args.get(1);
            String newPassword  = args.get(2);

            Console console = System.console();
            if (console != null)
            {
                if (StringUtils.equals(oldPassword, "-"))
                {
                    oldPassword = askForPassword(console, "old password", false);
                }

                if (StringUtils.equals(newPassword, "-"))
                {
                    newPassword = askForPassword(console, "new password", true);
                }
            }

            if (asAdmin)
            {
                try (LdapHelper helper = new LdapHelper(ensureAdminUser()))
                {
                    SearchResult result = helper.findByEmailAddress(LdapHelper.LDAP_SUFFIX, emailAddress);
                    if (result == null)
                    {
                        System.out.printf("User '%s' not found...%n", emailAddress);
                        return;
                    }

                    String name = result.getNameInNamespace();

                    String hashedPassword = LdapHelper.hashPassword(newPassword);
                    helper.changeUserPassword(name, hashedPassword);

                    System.out.printf("Password for user '%s' (%s) changed%n", name, emailAddress);
                }
            }
            else
            {
                String uid = LdapHelper.tryInferringDnfromEmail(emailAddress, LdapHelper.OPTIO3_DOMAIN, false);

                try (LdapHelper helper = new LdapHelper(WellKnownSites.ldapServer(), uid, oldPassword))
                {
                    SearchResult result = helper.findByEmailAddress(LdapHelper.LDAP_SUFFIX, emailAddress);
                    if (result == null)
                    {
                        System.out.printf("User '%s' not found...%n", emailAddress);
                        return;
                    }

                    String name = result.getNameInNamespace();

                    if (!helper.checkUserPassword(name, oldPassword))
                    {
                        System.out.printf("Password MISMATCH for user '%s' (%s)%n", name, emailAddress);
                        Runtime.getRuntime()
                               .exit(10);
                        return;
                    }

                    String hashedPassword = LdapHelper.hashPassword(newPassword);
                    helper.changeUserPassword(name, hashedPassword);

                    System.out.printf("Password for user '%s' (%s) changed%n", name, emailAddress);
                }
            }
        }
    }

    @Parameters(commandNames = "checkPasswd", commandDescription = "check user password (use '-' to be asked for password)")
    public class CommandCheckPasswordAdmin extends BaseCommand
    {
        @Parameter(names = "--asAdmin", description = "Login as admin to change someone else's password")
        public boolean asAdmin;

        @Parameter(arity = 2, description = "<email address> <password>", order = 1, required = true)
        public List<String> args;

        @Override
        public void exec() throws
                           Exception
        {
            String  emailAddress = args.get(0);
            String  password     = args.get(1);
            Console console      = System.console();
            if (console != null)
            {
                if (StringUtils.equals(password, "-"))
                {
                    password = askForPassword(console, "password", false);
                }
            }

            if (asAdmin)
            {
                try (LdapHelper helper = new LdapHelper(ensureAdminUser()))
                {
                    SearchResult result = helper.findByEmailAddress(LdapHelper.LDAP_SUFFIX, emailAddress);
                    if (result == null)
                    {
                        System.out.printf("User '%s' not found...%n", emailAddress);
                        return;
                    }

                    String name = result.getNameInNamespace();

                    if (helper.checkUserPassword(name, password))
                    {
                        System.out.printf("Password MATCH for user '%s' (%s)%n", name, emailAddress);
                    }
                    else
                    {
                        System.out.printf("Password MISMATCH for user '%s' (%s)%n", name, emailAddress);
                    }
                }
            }
            else
            {
                String uid = LdapHelper.tryInferringDnfromEmail(emailAddress, LdapHelper.OPTIO3_DOMAIN, false);

                try (LdapHelper helper = new LdapHelper(WellKnownSites.ldapServer(), uid, password))
                {
                    System.out.printf("Password MATCH for user '%s' (%s)%n", uid, emailAddress);
                }
                catch (Exception e)
                {
                    System.out.printf("Password MISMATCH for user '%s' (%s)%n", uid, emailAddress);
                }
            }
        }
    }

    @Parameters(commandNames = "groupAdd", commandDescription = "add group")
    public class CommandGroupAddAdmin extends AdminBaseCommand
    {
        @Parameter(arity = 3, description = "<parent node> <group name> <description>", order = 1, required = true)
        public List<String> args;

        @Override
        protected void exec(LdapHelper helper) throws
                                               Exception
        {
            String parentNode  = args.get(0);
            String groupName   = args.get(1);
            String description = args.get(2);

            Attributes attrs = helper.getAttributes(parentNode);
            if (!"organizationalUnit".equals(helper.getValue(attrs, LdapHelper.OBJECT_CLASS)))
            {
                System.out.println();
                System.out.printf("Can't find group %s...", parentNode);
                System.out.println();
                Runtime.getRuntime()
                       .exit(10);
                return;
            }

            helper.createGroup(parentNode, groupName, description);
        }
    }

    @Parameters(commandNames = "groupDelete", commandDescription = "delete group")
    public class CommandGroupDeleteAdmin extends AdminBaseCommand
    {
        @Parameter(description = "<group DN>", required = true)
        public String groupDn;

        @Override
        protected void exec(LdapHelper helper) throws
                                               Exception
        {
            Attributes attrs = helper.getAttributes(groupDn);
            if (!LdapHelper.OBJECT_CLASS__GROUPOFNAMES.equals(helper.getValue(attrs, LdapHelper.OBJECT_CLASS)))
            {
                System.out.println();
                System.out.printf("Can't find group %s...", groupDn);
                System.out.println();
                Runtime.getRuntime()
                       .exit(10);
                return;
            }

            helper.deleteGroup(groupDn);
        }
    }

    @Parameters(commandNames = "userAdd", commandDescription = "add user")
    public class CommandUserAddAdmin extends AdminBaseCommand
    {
        @Parameter(names = "--automation", description = "Set to create an automation account")
        public boolean automation;

        @Parameter(names = "--password", password = true, description = "<password> [use 'lookup' to search Master file for password]", order = 0)
        public String password;

        @Parameter(arity = 3, description = "<firstName> <lastName> <email address>", order = 1, required = true)
        public List<String> args;

        @Override
        protected void exec(LdapHelper helper) throws
                                               Exception
        {
            String firstName    = args.get(0);
            String lastName     = args.get(1);
            String emailAddress = args.get(2);

            new EmailValidator().validate("email", emailAddress);

            if (helper.findByEmailAddress(LdapHelper.LDAP_SUFFIX, emailAddress) != null)
            {
                System.out.println();
                System.out.println("User with same email already exists!");
                System.out.println();
                Runtime.getRuntime()
                       .exit(10);
                return;
            }

            String suffix = automation ? LdapHelper.LDAP_SUFFIX_AUTOMATION : LdapHelper.LDAP_SUFFIX_USERS;
            String uid    = emailAddress.split("@")[0];
            String dn     = helper.getUidInNamespace(suffix, uid);

            try
            {
                helper.getAttributes(dn);
                System.out.println();
                System.out.println("User already exists!");
                System.out.println();
                Runtime.getRuntime()
                       .exit(10);
                return;
            }
            catch (NameNotFoundException ex)
            {
            }

            if ("lookup".equals(password))
            {
                password = null;
                for (UserInfo ui : m_credDir.getLdapUsers())
                {
                    if (dn.equals(ui.ldapDn))
                    {
                        password = ui.password;
                        break;
                    }
                }

                if (password == null)
                {
                    System.out.println();
                    System.out.printf("Can't find user %s in Master file...", dn);
                    System.out.println();
                    Runtime.getRuntime()
                           .exit(10);
                    return;
                }
            }

            System.out.println("Creating user:");
            System.out.println("--------------");
            System.out.printf("  First Name: %s%n", firstName);
            System.out.printf("  Last Name : %s%n", lastName);
            System.out.printf("  Email     : %s%n", emailAddress);

            Console console = System.console();
            if (password == null)
            {
                if (console != null)
                {
                    password = askForPassword(console, "password", true);

                    if (password == null)
                    {
                        System.out.println();
                        System.out.println("Exiting without creating user...");
                        System.out.println();
                        Runtime.getRuntime()
                               .exit(10);
                        return;
                    }
                }
            }

            if (console != null)
            {
                System.out.println();
                System.out.println("Press ENTER to continue...");
                console.readLine();
            }

            String hashedPassword = LdapHelper.hashPassword(password);

            if (automation)
            {
                helper.createUser(LdapHelper.LDAP_SUFFIX_AUTOMATION, uid, firstName, lastName, emailAddress, hashedPassword);
            }
            else
            {
                helper.createUser(LdapHelper.LDAP_SUFFIX_USERS, uid, firstName, lastName, emailAddress, hashedPassword);
            }
        }
    }

    @Parameters(commandNames = "userDelete", commandDescription = "delete user")
    public class CommandUserDeleteAdmin extends AdminBaseCommand
    {
        @Parameter(description = "<email address>", required = true, validateValueWith = EmailValidator.class)
        public String emailAddress;

        @Override
        protected void exec(LdapHelper helper) throws
                                               Exception
        {
            SearchResult result = helper.findByEmailAddress(LdapHelper.LDAP_SUFFIX, emailAddress);
            if (result == null)
            {
                System.out.printf("User '%s' not found...%n", emailAddress);
                return;
            }

            String name = result.getNameInNamespace();
            System.out.println();
            System.out.printf("NAME = %s%n", name);
            Attributes attrs = result.getAttributes();

            if (verbose)
            {
                dumpAllAttributes(attrs, "  ");
            }

            helper.deleteUser(name);
            System.out.printf("Deleted user '%s' (%s)%n", name, emailAddress);
        }
    }

    @Parameters(commandNames = "userAddToGroup", commandDescription = "add user to group")
    public class CommandUserAddToGroupAdmin extends AdminBaseCommand
    {
        @Parameter(arity = 2, description = "<email address> <group>", required = true)
        public List<String> args;

        @Override
        protected void exec(LdapHelper helper) throws
                                               Exception
        {
            String emailAddress = args.get(0);
            String group        = args.get(1);

            new EmailValidator().validate("email", emailAddress);

            SearchResult result = helper.findByEmailAddress(LdapHelper.LDAP_SUFFIX, emailAddress);
            if (result == null)
            {
                System.out.printf("User '%s' not found...%n", emailAddress);
                return;
            }

            String name = result.getNameInNamespace();

            helper.addMemberToGroup(group, name);

            System.out.printf("Added user '%s' (%s) to group %s%n", name, emailAddress, group);
        }
    }

    @Parameters(commandNames = "userRemoveFromGroup", commandDescription = "add user to group")
    public class CommandUserRemoveFromGroupAdmin extends AdminBaseCommand
    {
        @Parameter(arity = 2, description = "<email address> <group>", required = true)
        public List<String> args;

        @Override
        protected void exec(LdapHelper helper) throws
                                               Exception
        {
            String emailAddress = args.get(0);
            String group        = args.get(1);

            new EmailValidator().validate("email", emailAddress);

            SearchResult result = helper.findByEmailAddress(LdapHelper.LDAP_SUFFIX, emailAddress);
            if (result == null)
            {
                System.out.printf("User '%s' not found...%n", emailAddress);
                return;
            }

            String name = result.getNameInNamespace();

            helper.removeMemberFromGroup(group, name);

            System.out.printf("Removed user '%s' (%s) to group %s%n", name, emailAddress, group);
        }
    }

    // @formatter:off
    @Parameter(names = "--help", help = true)
    private boolean help;

    @Parameter(names = "--master", description = "master identity file")
    private String master = System.getenv("HOME") + "/git/infra/identity/master.key";
    
    @Parameter(names = "--verbose", description = "Verbose output")
    private boolean verbose;
    // @formatter:on

    //--//

    private Map<String, BaseCommand> m_register = Maps.newHashMap();

    private CredentialDirectory m_credDir;

    //--//

    public static void main(String[] args) throws
                                           Exception
    {
        new Ldap().doMain(args);
    }

    public void doMain(String[] args) throws
                                      Exception
    {
        JCommander.Builder builder = JCommander.newBuilder();

        builder.addObject(this);

        addCommand(builder, new CommandListAdmin());
        addCommand(builder, new CommandCheckPasswordAdmin());
        addCommand(builder, new CommandChangePassword());
        addCommand(builder, new CommandGroupAddAdmin());
        addCommand(builder, new CommandGroupDeleteAdmin());
        addCommand(builder, new CommandUserAddAdmin());
        addCommand(builder, new CommandUserDeleteAdmin());
        addCommand(builder, new CommandUserAddToGroupAdmin());
        addCommand(builder, new CommandUserRemoveFromGroupAdmin());

        JCommander parser = builder.build();

        try
        {
            parser.parse(args);
        }
        catch (ParameterException e)
        {
            System.err.println(e.getMessage());
            System.err.println();

            StringBuilder sb = new StringBuilder();
            parser.usage(sb);
            System.err.println(sb);
            System.err.println();
            return;
        }

        String      parsedCmdName = parser.getParsedCommand();
        BaseCommand parsedCmd     = m_register.get(parsedCmdName);
        if (parsedCmd != null)
        {
            parsedCmd.exec();
        }
        else
        {
            StringBuilder sb = new StringBuilder();
            parser.usage(sb);
            System.err.println(sb);
            System.err.println();
        }
    }

    private void addCommand(Builder builder,
                            BaseCommand cmd)
    {
        Parameters anno = cmd.getClass()
                             .getAnnotation(Parameters.class);

        builder.addCommand(cmd);
        for (String name : anno.commandNames())
            m_register.put(name, cmd);
    }

    private UserInfo ensureAdminUser() throws
                                       IOException
    {
        if (m_credDir == null)
        {
            m_credDir = CredentialDirectory.load(new File(master));
        }

        Optional<UserInfo> optUser = m_credDir.filterAutomationUser(WellKnownSites.ldapServer(), RoleType.Administrator)
                                              .findFirst();
        if (!optUser.isPresent())
        {
            throw Exceptions.newRuntimeException("No administrator account for LDAP");
        }

        return optUser.get();
    }

    //--//

    private void dumpAllAttributes(Attributes attrs,
                                   String prefix) throws
                                                  NamingException
    {
        NamingEnumeration<String> ids = attrs.getIDs();
        while (ids.hasMore())
        {
            String id = ids.next();

            Attribute attr = attrs.get(id);
            Object    val  = attr.get();
            if (val instanceof byte[])
            {
                byte[] val2 = (byte[]) val;

                System.out.printf("%s%s: ", prefix, attr.getID());
                for (byte aVal2 : val2)
                {
                    System.out.printf("%c", aVal2);
                }
                System.out.println();
            }
            else
            {
                System.out.printf("%s%s: %s%n", prefix, attr.getID(), val);
            }
        }
    }

    private static String askForPassword(Console console,
                                         String text,
                                         boolean check)
    {
        while (true)
        {
            System.out.println();

            if (!check)
            {
                return new String(console.readPassword(String.format("Enter %s:", text)));
            }

            String password  = new String(console.readPassword(String.format("Enter %s      :", text)));
            String password2 = new String(console.readPassword(String.format("Enter %s again:", text)));
            if (StringUtils.equals(password, password2))
            {
                return password;
            }

            System.out.println("Passwords mismatch!");
        }
    }
}
