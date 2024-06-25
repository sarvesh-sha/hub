/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.infra.various;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import com.optio3.infra.directory.CertificateInfo;
import com.optio3.infra.directory.CredentialDirectory;
import com.optio3.infra.directory.RoleType;
import com.optio3.infra.directory.SshKey;
import com.optio3.infra.directory.UserInfo;
import com.optio3.test.infra.Optio3InfraTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class InfraDirectoryTest extends Optio3InfraTest
{
    File baseDir;

    @Before
    public void setup() throws
                        Exception
    {
        ensureCredentials(false, true);

        File baseDir = new File(System.getenv("HOME") + "/git/infra");
        credDir.populateFileStore(baseDir);
        System.out.println(credDir);
    }

    @Ignore("Manually enable to test, since it requires access to credentials.")
    @Test
    public void verify() throws
                         Exception
    {
        for (CertificateInfo cert : credDir.certificates.values())
        {
            byte[] clearText = cert.readAndDecryptPrivateFile();

            String s = new String(clearText, StandardCharsets.UTF_8);
            System.out.println(s);
        }

        for (List<SshKey> keys : credDir.sshKeys.values())
        {
            for (SshKey key : keys)
            {
                byte[] clearText = key.getPrivateKey();

                String s = new String(clearText, StandardCharsets.UTF_8);
                System.out.println(s);
            }
        }

        for (UserInfo ui : credDir.getLdapUsers())
        {
            checkUser(ui);
        }

        credDir.save(new File(baseDir, "identity/master2.key"));
    }

    private void checkUser(UserInfo ui) throws
                                        NamingException
    {
        DirContext context = ui.loginToLdap();

        if (ui.roles.contains(RoleType.User))
        {
            System.out.println("getAttributes");
            Attributes attrs = context.getAttributes(ui.ldapDn);

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
            System.out.println();
        }
    }

    @Ignore("Manually enable to test, since it requires access to credentials.")
    @Test
    public void testJavaKeyStore() throws
                                   Exception
    {
        String key = CredentialDirectory.generateRandomKey(24);

        for (CertificateInfo cert : credDir.certificates.values())
        {
            byte[] keyStore = cert.generateJavaKeyStore(key);

            String s = new String(keyStore, StandardCharsets.UTF_8);
            System.out.println(s);
        }
    }
}
