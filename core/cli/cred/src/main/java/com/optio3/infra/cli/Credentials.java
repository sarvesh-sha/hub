/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.JCommander.Builder;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.optio3.archive.ZipWalker;
import com.optio3.infra.WellKnownSites;
import com.optio3.infra.directory.CertificateInfo;
import com.optio3.infra.directory.CredentialDirectory;
import com.optio3.infra.directory.RoleType;
import com.optio3.infra.directory.UserInfo;
import com.optio3.text.TextSubstitution;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class Credentials
{
    public static abstract class BaseCommand
    {
        public abstract void exec() throws
                                    Exception;
    }

    //--//

    @Parameters(commandNames = "rand", commandDescription = "Generates a random key")
    public class CommandRandom extends BaseCommand
    {
        @Parameter(names = "--length", description = "The length of the key")
        public int length = 24;

        @Override
        public void exec() throws
                           Exception
        {
            System.out.println(CredentialDirectory.generateRandomKey(length));
            Runtime.getRuntime()
                   .exit(0);
        }
    }

    @Parameters(commandNames = "import-keys", commandDescription = "Loads all the certificates and keys into a fat credential file")
    public class CommandImportKeys extends BaseCommand
    {
        @Parameter(names = "--root", description = "Root directory for the import", required = true)
        public String rootDir;

        @Parameter(names = "--output", description = "Output location for fat credential file", required = true)
        public String outputFile;

        @Override
        public void exec() throws
                           Exception
        {
            File root = new File(rootDir);
            if (!root.isDirectory())
            {
                System.err.printf("Invalid directory '%s'%n", rootDir);
                Runtime.getRuntime()
                       .exit(10);
            }

            m_credDir.populateFileStore(root);

            m_credDir.save(new File(outputFile));

            Runtime.getRuntime()
                   .exit(0);
        }
    }

    @Parameters(commandNames = "keystore", commandDescription = "Generates a Java Key Store for a private certificate")
    public class CommandKeyStore extends BaseCommand
    {
        @Parameter(names = "--cert", description = "Certificate domain", required = true)
        public String cert;

        @Parameter(names = "--pass", description = "Passphrase for the key store", required = true)
        public String passPhrase;

        @Parameter(names = "--output", description = "Output file for key store", required = true)
        public String outputFile;

        @Parameter(names = "--root", description = "Root directory for the certificates")
        public String rootDir;

        @Override
        public void exec() throws
                           Exception
        {
            CertificateInfo certObj = m_credDir.certificates.get(cert);
            if (certObj == null)
            {
                System.err.printf("Invalid cert '%s'%n", cert);
                Runtime.getRuntime()
                       .exit(10);
            }

            if (rootDir != null)
            {
                File root = new File(rootDir);
                if (!root.isDirectory())
                {
                    System.err.printf("Invalid directory '%s'%n", rootDir);
                    Runtime.getRuntime()
                           .exit(10);
                }

                m_credDir.populateFileStore(root);
            }

            byte[] keyStore = certObj.generateJavaKeyStore(passPhrase);
            Files.write(keyStore, new File(outputFile));

            Runtime.getRuntime()
                   .exit(0);
        }
    }

    @Parameters(commandNames = "rawcerts", commandDescription = "Generates Raw Certificates for TLS")
    public class CommandRawCerts extends BaseCommand
    {
        @Parameter(names = "--cert", description = "Certificate domain", required = true)
        public String cert;

        @Parameter(names = "--ca", description = "Certificate Authority bundle", required = true)
        public String caBundle;

        @Parameter(names = "--public", description = "Public Certificate key", required = true)
        public String publicCert;

        @Parameter(names = "--private", description = "Private Certificate key", required = true)
        public String privateCert;

        @Parameter(names = "--root", description = "Root directory for the certificates")
        public String rootDir;

        @Override
        public void exec() throws
                           Exception
        {
            CertificateInfo certObj = m_credDir.certificates.get(cert);
            if (certObj == null)
            {
                System.err.printf("Invalid cert '%s'%n", cert);
                Runtime.getRuntime()
                       .exit(10);
            }

            if (rootDir != null)
            {
                File root = new File(rootDir);
                if (!root.isDirectory())
                {
                    System.err.printf("Invalid directory '%s'%n", rootDir);
                    Runtime.getRuntime()
                           .exit(10);
                }

                m_credDir.populateFileStore(root);
            }

            certObj.generateRawCert(caBundle, publicCert, privateCert);
            Runtime.getRuntime()
                   .exit(0);
        }
    }

    @Parameters(commandNames = "import-cert", commandDescription = "Generates Raw Certificates for TLS")
    public class CommandImportCert extends BaseCommand
    {
        @Parameter(names = "--cert", description = "Certificate domain", required = true)
        public String cert;

        @Parameter(names = "--file", description = "Archive to import", required = true)
        public String file;

        @Override
        public void exec() throws
                           Exception
        {
            CertificateInfo certObj = m_credDir.certificates.get(cert);
            if (certObj == null)
            {
                System.err.printf("Invalid cert '%s'%n", cert);
                Runtime.getRuntime()
                       .exit(10);
            }

            final String suffix = ".crt";

            ZipWalker.walk(new File(file), (subEntry) ->
            {
                if (!subEntry.isDirectory())
                {
                    String name = subEntry.getName();
                    System.err.printf("%s\n", name);
                    if (name.endsWith(".crt"))
                    {
                        name = name.substring(0, name.length() - suffix.length());

                        if (!StringUtils.equals(name, "gd_bundle-g2-g1"))
                        {
                            Path path = Path.of(master);
                            path = path.getParent();
                            path = path.getParent();
                            path = path.resolve(certObj.publicFile.fileName);

                            try (FileOutputStream output = new FileOutputStream(path.toFile()))
                            {
                                IOUtils.copyLarge(subEntry.getStream(), output);
                            }

                            Runtime.getRuntime()
                                   .exit(0);
                        }
                    }
                }

                return true;
            });

            System.err.println("Failed to find correct .crt entry!");
            Runtime.getRuntime()
                   .exit(10);
        }
    }

    @Parameters(commandNames = "account", commandDescription = "Get account name for a site or a role")
    public class CommandAccount extends BaseCommand
    {
        @Parameter(names = "--site", description = "Filter for site")
        public String site;

        @Parameter(names = "--role", description = "Filter for roles", required = true)
        public RoleType role;

        @Override
        public void exec()
        {
            boolean gotSite = (site == null);

            for (List<UserInfo> users : m_credDir.automationAccounts.values())
            {
                for (UserInfo user : users)
                {
                    if (site != null)
                    {
                        if (!site.equals(user.site))
                        {
                            continue;
                        }

                        gotSite = true;
                    }

                    if (role != null && !user.roles.contains(role))
                    {
                        continue;
                    }

                    System.out.println(user.user);
                    Runtime.getRuntime()
                           .exit(0);
                    return;
                }
            }

            if (!gotSite)
            {
                System.err.printf("Unknown site '%s'%n", site);
            }
            else
            {
                System.err.printf("No account matching search criteria%n");
            }

            Runtime.getRuntime()
                   .exit(10);
        }
    }

    @Parameters(commandNames = "password", commandDescription = "Get the password associated with an account")
    public class CommandPassword extends BaseCommand
    {
        @Parameter(names = "--site", description = "Filter for site")
        public String site;

        @Parameter(names = "--role", description = "Filter for roles")
        public RoleType role;

        @Parameter(names = "--account", description = "<account>")
        public String account;

        @Override
        public void exec()
        {
            boolean gotSite = (site == null);

            for (List<UserInfo> users : m_credDir.automationAccounts.values())
            {
                for (UserInfo user : users)
                {
                    if (site != null)
                    {
                        if (!site.equals(user.site))
                        {
                            continue;
                        }

                        gotSite = true;
                    }

                    if (role != null && !user.roles.contains(role))
                    {
                        continue;
                    }

                    if (account != null && !user.user.equals(account))
                    {
                        continue;
                    }

                    System.out.println(user.getEffectivePassword());
                    Runtime.getRuntime()
                           .exit(0);
                    return;
                }
            }

            if (!gotSite)
            {
                System.err.printf("Unknown site '%s'%n", site);
            }
            else
            {
                System.err.printf("No account matching search criteria%n");
            }

            Runtime.getRuntime()
                   .exit(10);
        }
    }

    @Parameters(commandNames = "file-substitution", commandDescription = "Substitute ${...} variables in a file")
    public class CommandFileSubstitution extends BaseCommand
    {
        @Parameter(names = "--in", description = "Input file", required = true)
        public String inputFile;

        @Parameter(names = "--out", description = "Output file", required = true)
        public String outputFile;

        @Override
        public void exec() throws
                           Exception
        {
            if (m_credDir == null)
            {
                throw new RuntimeException("No credentials available. Forgot to set '--master'?");
            }

            TextSubstitution fs = new TextSubstitution();

            fs.addHandler("cred.email.", (val) ->
            {
                UserInfo user = m_credDir.findFirstAutomationUser(WellKnownSites.builderServer(), RoleType.valueOf(val));

                return user.getEffectiveCredentials().emailAddress;
            });

            fs.addHandler("cred.user.", (val) ->
            {
                UserInfo user = m_credDir.findFirstAutomationUser(WellKnownSites.builderServer(), RoleType.valueOf(val));

                return user.getEffectiveCredentials().user;
            });

            fs.addHandler("cred.password.", (val) ->
            {
                UserInfo user = m_credDir.findFirstAutomationUser(WellKnownSites.builderServer(), RoleType.valueOf(val));

                return user.getEffectiveCredentials().password;
            });

            fs.transform(inputFile, outputFile);

            Runtime.getRuntime()
                   .exit(0);
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
        new Credentials().doMain(args);
    }

    public void doMain(String[] args) throws
                                      Exception
    {
        JCommander.Builder builder = JCommander.newBuilder();

        builder.addObject(this);

        addCommand(builder, new CommandAccount());
        addCommand(builder, new CommandPassword());
        addCommand(builder, new CommandFileSubstitution());
        addCommand(builder, new CommandImportKeys());
        addCommand(builder, new CommandRandom());
        addCommand(builder, new CommandKeyStore());
        addCommand(builder, new CommandRawCerts());
        addCommand(builder, new CommandImportCert());

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
            m_credDir = CredentialDirectory.load(new File(master));

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
}
