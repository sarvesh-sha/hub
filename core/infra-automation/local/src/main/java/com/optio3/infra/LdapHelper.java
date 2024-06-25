/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra;

import static java.util.Objects.requireNonNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Properties;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import com.google.common.collect.Sets;
import com.optio3.infra.directory.UserInfo;
import com.optio3.logging.Logger;
import org.apache.commons.lang3.StringUtils;

public class LdapHelper implements AutoCloseable
{
    public static final String DN            = "dn";
    public static final String UID           = "uid";
    public static final String OBJECT_CLASS  = "objectClass";
    public static final String DESCRIPTION   = "description";
    public static final String SN            = "sn";
    public static final String GN            = "gn";
    public static final String GIVENNAME     = "givenname";
    public static final String CN            = "cn";
    public static final String USER_PASSWORD = "userPassword";
    public static final String MAIL          = "mail";
    public static final String MEMBER        = "member";

    public static final String OBJECT_CLASS__GROUPOFNAMES  = "groupOfNames";
    public static final String OBJECT_CLASS__INETORGPERSON = "inetOrgPerson";

    //--//

    private static final String OPTIO3_PART1           = "optio3";
    private static final String OPTIO3_PART2           = "com";
    public static final  String LDAP_SUFFIX            = "dc=" + OPTIO3_PART1 + ",dc=" + OPTIO3_PART2;
    public static final  String LDAP_SUFFIX_USERS      = "ou=people," + LDAP_SUFFIX;
    public static final  String LDAP_SUFFIX_AUTOMATION = "ou=automation," + LDAP_SUFFIX;
    public static final  String OPTIO3_DOMAIN          = OPTIO3_PART1 + "." + OPTIO3_PART2;

    //--//

    private static final String c_prefixSaltedSHA1 = "{SSHA}";
    private static final int    c_saltLengthSHA1   = 4;

    public static final Logger LoggerInstance = new Logger(LdapHelper.class);

    private DirContext m_context;

    public LdapHelper(UserInfo ui) throws
                                   NamingException
    {
        m_context = ui.loginToLdap();
    }

    public LdapHelper(String server,
                      String ldapDn,
                      String password) throws
                                       NamingException
    {
        m_context = loginToLdap(server, ldapDn, password);
    }

    @Override
    public void close()
    {
        if (m_context != null)
        {
            try
            {
                m_context.close();
            }
            catch (NamingException e)
            {
            }

            m_context = null;
        }
    }

    //--//

    public static DirContext loginToLdap(String server,
                                         String ldapDn,
                                         String password) throws
                                                          NamingException
    {
        requireNonNull(server, "No LDAP server");
        requireNonNull(ldapDn, "No LDAP identifier");

        Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        properties.put(Context.PROVIDER_URL, "ldaps://" + server);
        properties.put(Context.SECURITY_PRINCIPAL, ldapDn);
        properties.put(Context.SECURITY_CREDENTIALS, password);

        return new InitialDirContext(properties);
    }

    public static String tryInferringDnfromEmail(String emailAddress,
                                                 String domain,
                                                 boolean automation)
    {
        final String c_domainAndAt = "@" + domain;

        int pos = emailAddress.lastIndexOf(c_domainAndAt);

        if (pos < 0 || (pos + c_domainAndAt.length()) != emailAddress.length())
        {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("uid=");
        sb.append(emailAddress.substring(0, pos));

        if (automation)
        {
            sb.append(",ou=automation");
        }
        else
        {
            sb.append(",ou=people");
        }

        for (String part : StringUtils.split(domain, '.'))
        {
            sb.append(",dc=");
            sb.append(part);
        }

        return sb.toString();
    }

    //--//

    public Attributes getAttributes(String name) throws
                                                 NamingException
    {
        return m_context.getAttributes(name);
    }

    public NamingEnumeration<SearchResult> search(String name,
                                                  Attributes matchingAttributes) throws
                                                                                 NamingException
    {
        return m_context.search(name, matchingAttributes);
    }

    public NamingEnumeration<SearchResult> searchSubtree(String name,
                                                         String filter) throws
                                                                        NamingException
    {
        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        return m_context.search(name, filter, sc);
    }

    public SearchResult findByEmailAddress(String root,
                                           String emailAddress) throws
                                                                NamingException
    {
        NamingEnumeration<SearchResult> results = searchSubtree(root, MAIL + "=" + emailAddress);
        if (!results.hasMore())
        {
            return null;
        }

        return results.next();
    }

    public String getValue(Attributes attrs,
                           String name) throws
                                        NamingException
    {
        Attribute attr = attrs.get(name);
        if (attr != null)
        {
            return String.valueOf(attr.get());
        }

        return null;
    }

    public String getValue(Attributes attrs,
                           String... names) throws
                                            NamingException
    {
        for (String name : names)
        {
            Attribute attr = attrs.get(name);
            if (attr != null)
            {
                return String.valueOf(attr.get());
            }
        }

        return null;
    }

    public Set<String> getMembersOfGroup(String groupName) throws
                                                           NamingException
    {
        Attributes attrs = getAttributes(groupName);
        Attribute  attr  = attrs.get(MEMBER);

        Set<String> res = Sets.newHashSet();
        for (int i = 0; i < attr.size(); i++)
            res.add((String) attr.get(i));

        return res;
    }

    //--//

    public boolean checkUserPassword(String userDN,
                                     String password) throws
                                                      NamingException
    {
        Attributes attrs = m_context.getAttributes(userDN);
        Attribute  attr  = attrs.get(USER_PASSWORD);
        if (attr == null)
        {
            return false;
        }

        Object val = attr.get();
        String hashedPassword;

        if (val instanceof byte[])
        {
            hashedPassword = new String((byte[]) val);
        }
        else
        {
            hashedPassword = (String) val;
        }

        return checkPassword(password, hashedPassword);
    }

    public void changeUserPassword(String userDN,
                                   String password) throws
                                                    NamingException
    {
        ModificationItem mi = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(USER_PASSWORD, password));

        applyModification(userDN, mi);
    }

    public void addMemberToGroup(String groupDN,
                                 String memberDN) throws
                                                  NamingException
    {
        Attributes attrs = m_context.getAttributes(groupDN);
        Attribute  attr  = attrs.get(MEMBER);

        if (!attr.contains(memberDN))
        {
            if (attr.size() == 1 && "".equals(attr.get()))
            {
                // Since the only entry is the empty one, replace it.

                ModificationItem mi = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(MEMBER, memberDN));

                applyModification(groupDN, mi);
            }
            else
            {
                ModificationItem mi = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute(MEMBER, memberDN));

                applyModification(groupDN, mi);
            }
        }
    }

    public void removeMemberFromGroup(String groupDN,
                                      String memberDN) throws
                                                       NamingException
    {
        Attributes attrs = m_context.getAttributes(groupDN);
        Attribute  attr  = attrs.get(MEMBER);

        if (attr.contains(memberDN))
        {
            if (attr.size() == 1)
            {
                // We need to leave at least one "member" entry.
                ModificationItem mi = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(MEMBER, ""));

                applyModification(groupDN, mi);
            }
            else
            {
                ModificationItem mi = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(MEMBER, memberDN));

                applyModification(groupDN, mi);
            }
        }
    }

    public void createUser(String root,
                           String uid,
                           String firstName,
                           String lastName,
                           String email,
                           String password) throws
                                            NamingException
    {
        Attributes attrs = new BasicAttributes();

        attrs.put(UID, uid);
        attrs.put(OBJECT_CLASS, "inetOrgPerson");
        attrs.put(CN, firstName + " " + lastName);
        attrs.put(GN, firstName);
        attrs.put(SN, lastName);
        attrs.put(MAIL, email);
        attrs.put(USER_PASSWORD, password);

        m_context.createSubcontext(getUidInNamespace(root, uid), attrs);
    }

    public void deleteUser(String dn) throws
                                      NamingException
    {
        m_context.destroySubcontext(dn);
    }

    //--//

    public void createGroup(String root,
                            String cn,
                            String description) throws
                                                NamingException
    {
        Attributes attrs = new BasicAttributes();

        attrs.put(CN, cn);
        attrs.put(OBJECT_CLASS, "groupofnames");
        attrs.put(DESCRIPTION, description);
        attrs.put(MEMBER, "");

        m_context.createSubcontext(getCnInNamespace(root, cn), attrs);
    }

    public void deleteGroup(String dn) throws
                                       NamingException
    {
        m_context.destroySubcontext(dn);
    }

    //--//

    public static String getUidInNamespace(String root,
                                           String uid)
    {
        return UID + "=" + uid + "," + root;
    }

    public static String getCnInNamespace(String root,
                                          String cn)
    {
        return CN + "=" + cn + "," + root;
    }

    public static String hashPassword(String password)
    {
        byte[] salt = new byte[c_saltLengthSHA1];
        new SecureRandom().nextBytes(salt);

        return hashPassword(password, salt);
    }

    public static boolean checkPassword(String input,
                                        String hashedPassword)
    {
        try
        {
            if (hashedPassword.startsWith(c_prefixSaltedSHA1))
            {
                Decoder base64 = Base64.getDecoder();

                byte[] merged = base64.decode(hashedPassword.substring(c_prefixSaltedSHA1.length()));

                if (merged.length == 20 + c_saltLengthSHA1)
                {
                    byte[] salt = Arrays.copyOfRange(merged, 20, 20 + c_saltLengthSHA1);

                    return hashPassword(input, salt).equals(hashedPassword);
                }
            }
        }
        catch (Exception e)
        {
            // Any exception is a failure.
        }

        return false;
    }

    private static String hashPassword(String password,
                                       byte[] salt)
    {
        MessageDigest md;

        try
        {
            md = MessageDigest.getInstance("SHA1");
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException(e);
        }

        md.update(password.getBytes());
        md.update(salt);
        byte[] hash = md.digest();

        Encoder base64 = Base64.getEncoder();

        byte[] merge = new byte[hash.length + salt.length];
        System.arraycopy(hash, 0, merge, 0, hash.length);
        System.arraycopy(salt, 0, merge, hash.length, salt.length);

        StringBuilder sb = new StringBuilder();
        sb.append(c_prefixSaltedSHA1);
        sb.append(base64.encodeToString(merge));
        return sb.toString();
    }

    //--//

    private void applyModification(String name,
                                   ModificationItem mi) throws
                                                        NamingException
    {
        m_context.modifyAttributes(name, new ModificationItem[] { mi });
    }
}
