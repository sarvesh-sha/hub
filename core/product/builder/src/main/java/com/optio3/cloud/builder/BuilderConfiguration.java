/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder;

import java.lang.reflect.Field;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.optio3.cloud.AbstractConfigurationWithDatabase;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.builder.model.worker.Host;
import com.optio3.cloud.builder.persistence.config.UserRecord;
import com.optio3.cloud.persistence.EncryptedPayload;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.WellKnownSites;
import com.optio3.infra.directory.CredentialDirectory;
import com.optio3.infra.directory.RoleType;
import com.optio3.infra.directory.UserInfo;
import com.optio3.util.Exceptions;

public class BuilderConfiguration extends AbstractConfigurationWithDatabase
{
    public static class DeveloperSettings
    {
        public boolean developerMode;
        public boolean unitTestMode;
        public boolean forceLogToConsole;

        public boolean useLocalMaven;
        public boolean useLocalhostAsNexus;

        public String sourceRepo;

        public boolean dontPushImages;

        public boolean disableImagePruning;
        public boolean disableAutomaticBackups;

        public String databasePasswordOverride;

        public boolean disableEmails;

        public boolean dumpBuildLogs;
    }

    public String   credentialFile;
    public UserInfo selfhost;

    public String managedDirectoriesRoot;

    public boolean startBackgroundProcessing = true;
    public boolean noBackgroundProcessingDefragmentation;

    public boolean enableMessageBusOverUDP = true;
    public boolean enableMessageBusOverUDPforIntel;

    public boolean loadDemoJobs;

    //--//

    // To generate Docker images that are more consistent from build to build, we want to hardcode the time of files and directories.
    public ZonedDateTime overrideBuildTime = ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

    //--//

    public String emailForInfo         = "prod.info@optio3.com";
    public String emailForWarnings     = "prod.warn@optio3.com";
    public String emailForAlerts       = "prod.alert@optio3.com";
    public String emailForProvisioning = "prod.provisioning@optio3.com";

    //--//

    public final DeveloperSettings developerSettings = new DeveloperSettings();

    public String gitHubSignatureKey;

    public BuilderConnectionMode deployerConnectionMode = BuilderConnectionMode.Production;
    public String                ngrokTunnelUrl;

    //--//

    public Host host;

    //--//

    @JsonIgnore
    private String m_cloudConnectionUrl = "wss://" + WellKnownSites.builderServer();

    @JsonIgnore
    public CredentialDirectory credentials;

    @JsonIgnore
    public UserInfo ldapRoot;

    @JsonIgnore
    public BuilderUserLogic userLogic;

    @JsonIgnore
    public HostRemoter hostRemoter;

    @JsonIgnore
    public String getCloudConnectionUrl()
    {
        return m_cloudConnectionUrl;
    }

    @JsonIgnore
    public void setCloudConnectionUrl(String url)
    {
        m_cloudConnectionUrl = url;
    }

    //--//

    public UserRecord getUserFromAccessor(SessionHolder sessionHolder,
                                          CookiePrincipalAccessor accessor)
    {
        CookiePrincipal principal = CookiePrincipalAccessor.get(accessor);
        principal.ensureAuthenticated();

        return userLogic.findUser(sessionHolder, principal, true);
    }

    public UserInfo getCredentialForHost(String host,
                                         boolean returnEffectiveVersion,
                                         RoleType... roles)
    {
        UserInfo ui = getCredentialForHostOrNull(host, returnEffectiveVersion, roles);
        if (ui != null)
        {
            return ui;
        }

        throw Exceptions.newIllegalArgumentException("No Automation account on '%s' with roles '%s'", host, roles);
    }

    public UserInfo getCredentialForHostOrNull(String host,
                                               boolean returnEffectiveVersion,
                                               RoleType... roles)
    {
        Optional<UserInfo> res = credentials.filterAutomationUser(host, roles)
                                            .filter((u) -> u.getEffectivePassword() != null)
                                            .findFirst();
        if (res.isPresent())
        {
            UserInfo ui = res.get();

            return returnEffectiveVersion ? ui.getEffectiveCredentials() : ui;
        }

        return null;
    }

    public String decryptDatabasePassword(EncryptedPayload ep) throws
                                                               Exception
    {
        return developerSettings.databasePasswordOverride != null ? developerSettings.databasePasswordOverride : decrypt(ep);
    }

    public ModelMapperPolicy getPolicyWithDecryptionForAdministratorUser(SessionHolder sessionHolder,
                                                                         CookiePrincipalAccessor accessor)
    {
        UserRecord rec_user = getUserFromAccessor(sessionHolder, accessor);

        if (rec_user.hasAnyRoles(WellKnownRoleIds.Administrator))
        {
            return new ModelMapperPolicy()
            {
                @Override
                public boolean canOverrideReadOnlyField(Field modelField)
                {
                    return false;
                }

                @Override
                public boolean canReadField(Field modelField)
                {
                    return true;
                }

                @Override
                public boolean canWriteField(Field modelFieldf)
                {
                    return true;
                }

                @Override
                public EncryptedPayload encryptField(Field modelField,
                                                     String value) throws
                                                                   Exception
                {
                    return encrypt(value);
                }

                @Override
                public String decryptField(Field modelField,
                                           EncryptedPayload value) throws
                                                                   Exception
                {
                    return decrypt(value);
                }
            };
        }

        return ModelMapperPolicy.Default;
    }
}
