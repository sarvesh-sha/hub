/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.directory;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.FileSystem;
import com.optio3.util.ProcessUtils;
import com.sun.jna.Platform;
import org.apache.commons.lang3.StringUtils;

public class CredentialDirectory
{
    /**
     * A map from a domain to the certificates for that domain: public/private/passphrase.
     */
    public Map<String, CertificateInfo> certificates = Maps.newHashMap();

    /**
     * A map from a service to the user/password used to login into that service.
     */
    public Map<String, UserInfo> managementConsoles = Maps.newHashMap();

    /**
     * Users' credentials for various services, tagged with target site and user.
     */
    public Map<String, List<UserInfo>> automationAccounts = Maps.newHashMap();

    /**
     * A map from a service to the user/accessKey/secretKey used to access the REST api for that service.
     */
    public Map<String, List<ApiInfo>> apiCredentials = Maps.newHashMap();

    /**
     * A map from a service to the SSH private key with corresponding passphrase.
     */
    public Map<String, List<SshKey>> sshKeys = Maps.newHashMap();

    /**
     * A map from a service to the set of id/secretKey used to access other credentials.
     */
    public Map<String, List<SecretInfo>> secrets = Maps.newHashMap();

    /**
     * A map from a file name to its contents.
     */
    @JsonDeserialize(keyUsing = FileInfo.KeyDeserializerImpl.class)
    public Map<FileInfo, byte[]> fileStore = Maps.newHashMap();

    //--//

    private static final ObjectMapper s_mapper;

    static
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);

        s_mapper = objectMapper;
    }

    public static CredentialDirectory load(File file) throws
                                                      IOException
    {
        CredentialDirectory cred = s_mapper.readValue(file, CredentialDirectory.class);

        cred.postLoad();

        return cred;
    }

    public void save(File file) throws
                                IOException
    {
        ObjectWriter writer = s_mapper.writer(new DefaultPrettyPrinter());
        writer.writeValue(file, this);
    }

    public String saveAsString() throws
                                 IOException
    {
        ObjectWriter writer = s_mapper.writer(new DefaultPrettyPrinter());
        return writer.writeValueAsString(this);
    }

    public void remapSite(String oldSite,
                          String newSite)
    {
        List<UserInfo> usersFromOldSite = automationAccounts.remove(oldSite);
        if (usersFromOldSite != null)
        {
            List<UserInfo> usersFromNewSite = automationAccounts.computeIfAbsent(newSite, k -> Lists.newArrayList());

            for (UserInfo user : usersFromOldSite)
            {
                user.site = newSite;

                usersFromNewSite.add(user);
            }
        }
    }

    //--//

    public void populateFileStore(File contentRoot) throws
                                                    IOException
    {
        for (CertificateInfo obj : certificates.values())
        {
            loadContents(contentRoot, obj.publicFile);
            loadContents(contentRoot, obj.privateFile);

            for (FileInfo chain : obj.trustChain)
                loadContents(contentRoot, chain);
        }

        for (List<SshKey> list : sshKeys.values())
        {
            for (SshKey obj : list)
            {
                loadContents(contentRoot, obj.publicKeyFile);
                loadContents(contentRoot, obj.privateKeyFile);
            }
        }
    }

    private void loadContents(File root,
                              FileInfo fi) throws
                                           IOException
    {
        if (fi != null)
        {
            byte[] content = Files.toByteArray(new File(root, fi.fileName));

            fileStore.put(fi, content);
        }
    }

    public byte[] getContents(FileInfo fi)
    {
        return fileStore.get(fi);
    }

    public byte[] decrypt(FileInfo encrypted,
                          String passphrase) throws
                                             Exception
    {
        try (FileSystem.TmpFileHolder holder = FileSystem.createTempFile())
        {
            Files.write(getContents(encrypted), holder.get());

            // We have to use an explicit version on Mac, since the built-in version is too old!
            String openSSL = Platform.isMac() ? "/opt/homebrew/opt/openssl@1.1/bin/openssl" : "openssl";
            return ProcessUtils.execAndCaptureOutput(holder.get(), 10, TimeUnit.SECONDS, openSSL, "aes-256-cbc", "-d", "-salt", "-pbkdf2", "-k", passphrase);
        }
    }

    public static String generateRandomKey(int length)
    {
        byte[]       output = new byte[length];
        SecureRandom rnd    = new SecureRandom();

        while (true)
        {
            rnd.nextBytes(output);
            String key = Base64.getEncoder()
                               .encodeToString(output);

            // These characters can cause problems in files and scripts, run again.
            if (key.indexOf('+') >= 0 || key.indexOf('/') >= 0)
            {
                continue;
            }

            return key;
        }
    }

    //--//

    public Stream<UserInfo> filterAutomationUser(String site,
                                                 RoleType... roles)
    {
        List<UserInfo> list = CollectionUtils.asEmptyCollectionIfNull(automationAccounts.get(site));

        return list.stream()
                   .filter((ui) -> ui.hasRoles(roles));
    }

    public UserInfo findFirstAutomationUser(String site,
                                            RoleType... roles)
    {
        Optional<UserInfo> res = filterAutomationUser(site, roles).findFirst();
        if (res.isPresent())
        {
            return res.get();
        }

        throw Exceptions.newIllegalArgumentException("No Automation account on '%s' with roles '%s'", site, roles);
    }

    @JsonIgnore
    public List<UserInfo> getLdapUsers()
    {
        List<UserInfo> lst = Lists.newArrayList();

        for (List<UserInfo> lst2 : automationAccounts.values())
        {
            for (UserInfo ui : lst2)
            {
                if (ui.ldapDn != null)
                {
                    lst.add(ui);
                }
            }
        }

        return lst;
    }

    //--//

    public Stream<ApiInfo> filterApiCredentials(String site,
                                                String key)
    {
        List<ApiInfo> list = CollectionUtils.asEmptyCollectionIfNull(apiCredentials.get(site));

        return list.stream()
                   .filter((ai) -> key == null || StringUtils.equals(ai.key, key));
    }

    public ApiInfo findFirstApiCredential(String site,
                                          String key)
    {
        ApiInfo ai = findFirstApiCredentialOrNull(site, key);
        if (ai != null)
        {
            return ai;
        }

        if (key == null)
        {
            throw Exceptions.newIllegalArgumentException("No API accounts for '%s'", site);
        }

        throw Exceptions.newIllegalArgumentException("No API account for '%s' as '%s'", site, key);
    }

    public ApiInfo findFirstApiCredentialOrNull(String site,
                                                String key)
    {
        Optional<ApiInfo> res = filterApiCredentials(site, key).findFirst();
        if (res.isPresent())
        {
            return res.get();
        }

        return null;
    }

    //--//

    public Stream<SshKey> filterSshKeys(String site,
                                        String user)
    {
        List<SshKey> list = CollectionUtils.asEmptyCollectionIfNull(sshKeys.get(site));

        return list.stream()
                   .filter((ai) -> user == null || ai.user.equals(user));
    }

    public SshKey findFirstSshKey(String site,
                                  String user)
    {
        Optional<SshKey> res = filterSshKeys(site, user).findFirst();
        if (res.isPresent())
        {
            return res.get();
        }

        if (user == null)
        {
            throw Exceptions.newIllegalArgumentException("No SSH keys for '%s'", site);
        }

        throw Exceptions.newIllegalArgumentException("No SSH user '%s' for '%s'", user, site);
    }

    //--//

    public Stream<SecretInfo> filterSecrets(String site,
                                            String id)
    {
        List<SecretInfo> list = CollectionUtils.asEmptyCollectionIfNull(secrets.get(site));

        return list.stream()
                   .filter((v) -> id == null || v.id.equals(id));
    }

    public SecretInfo findFirstSecret(String site,
                                      String id)
    {
        Optional<SecretInfo> res = filterSecrets(site, id).findFirst();
        if (res.isPresent())
        {
            return res.get();
        }

        if (id == null)
        {
            throw Exceptions.newIllegalArgumentException("No Secrets for '%s'", site);
        }

        throw Exceptions.newIllegalArgumentException("No Secret '%s' for '%s'", id, site);
    }

    //--//

    public CertificateInfo findCertificate(String url)
    {
        String certDomain = null;

        for (String domain : certificates.keySet())
        {
            if (url.contains("." + domain))
            {
                if (certDomain == null || certDomain.length() < domain.length())
                {
                    certDomain = domain;
                }
            }
        }

        if (certDomain == null)
        {
            throw Exceptions.newIllegalArgumentException("No certificate for domain '%s'", url);
        }

        return certificates.get(certDomain);
    }

    //--//

    private void postLoad()
    {
        for (CertificateInfo obj : certificates.values())
            obj.credDir = this;

        for (List<SshKey> list : sshKeys.values())
        {
            for (SshKey obj : list)
                obj.credDir = this;
        }

        //
        // Then collect all LDAP users, for effective credential lookup.
        //
        Map<String, UserInfo> userToLdap = Maps.newHashMap();
        for (List<UserInfo> lst : automationAccounts.values())
        {
            for (UserInfo ui : lst)
            {
                if (ui.ldapDn != null)
                {
                    userToLdap.put(ui.user, ui);
                }
            }
        }

        //
        // Finally, link users to sites and their effective credentials.
        //
        for (String site : automationAccounts.keySet())
        {
            for (UserInfo ui : automationAccounts.get(site))
            {
                ui.site = site;

                ui.setEffectiveCredentials(ui.referToLdap ? userToLdap.get(ui.user) : ui);
            }
        }
    }
}
