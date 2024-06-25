/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.optio3.cloud.AbstractConfigurationWithDatabase;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.hub.model.customization.InstanceConfiguration;
import com.optio3.cloud.hub.model.customization.InstanceConfigurationDoNothing;
import com.optio3.cloud.hub.persistence.config.SystemPreferenceRecord;
import com.optio3.cloud.hub.persistence.config.SystemPreferenceTypedValue;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.persistence.EncryptedPayload;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.SessionHolder;

public class HubConfiguration extends AbstractConfigurationWithDatabase
{
    public static class DeveloperSettings
    {
        public boolean developerMode;
        public boolean unitTestMode;
        public boolean forceLogToConsole;

        public boolean disableEmails;
        public boolean disableSMSs;

        public int restCounterFrequency = 5 * 60;

        /**
         * If true, demo data gets loaded
         */
        public boolean includeDemoData = true;

        /**
         * If true, dump demo data as it gets loaded
         */
        public boolean verboseDemoData;

        public boolean dumpStagingStatisticsRaw;
        public boolean dumpStagingStatistics;

        public String  dumpSamplingStatistics;
        public boolean dumpSamplingStatisticsOnlyGateways;
        public boolean dumpSamplingStatisticsOnlyNetworks;
        public boolean dumpSamplingStatisticsPerDevice;

        public boolean flushResultsOnStartup;

        //--//

        public boolean autoConfigureSampling = true;

        //--//

        public String bulkRenamingInput;
        public String bulkRenamingOutput;
    }

    public HubDataDefinition[] data;

    public HubExportDefinition[] exports;

    public boolean startBackgroundProcessing = true;
    public boolean noBackgroundProcessingDefragmentation;

    public boolean enableMessageBusOverUDP = true;
    public boolean enableMessageBusOverUDPforIntel;

    public String hostId;
    public String buildId;

    public String cloudConnectionUrl = "http://localhost:8080";

    public String backupLocation;

    public String reporterConnectionUrl    = "https://reporter.optio3.io";
    public String localReportConnectionUrl = null;

    //--//

    public boolean disableServiceWorker;

    public String emailForInfo     = "prod.info@optio3.com";
    public String emailForWarnings = "prod.warn@optio3.com";
    public String emailForAlerts   = "prod.alert@optio3.com";

    public String communicatorConnectionUrl = "https://builder.dev.optio3.io";
    public String communicatorId;
    public String communicatorAccessKey;

    //--//

    @JsonIgnore
    public HubUserLogic userLogic;

    @JsonIgnore
    public InstanceConfiguration instanceConfigurationForUnitTest;

    public final DeveloperSettings developerSettings = new DeveloperSettings();

    //--//

    public URI parseConnectionUrl(String extraPath)
    {
        if (extraPath != null)
        {
            return URI.create(cloudConnectionUrl + "/")
                      .resolve(extraPath);
        }

        return URI.create(cloudConnectionUrl);
    }

    public UserRecord getUserFromAccessor(SessionHolder sessionHolder,
                                          CookiePrincipalAccessor accessor)
    {
        CookiePrincipal principal = CookiePrincipalAccessor.get(accessor);
        principal.ensureAuthenticated();

        return userLogic.findUser(sessionHolder, principal, true);
    }

    public ModelMapperPolicy getPolicyWithOverrideForMaintenanceUser(SessionHolder sessionHolder,
                                                                     CookiePrincipalAccessor accessor)
    {
        UserRecord rec_user = getUserFromAccessor(sessionHolder, accessor);

        if (rec_user.hasAnyRoles(WellKnownRoleIds.Maintenance))
        {
            return new ModelMapperPolicy()
            {
                @Override
                public boolean canOverrideReadOnlyField(Field modelField)
                {
                    if (modelField.getDeclaringClass() == BaseModel.class)
                    {
                        switch (modelField.getName())
                        {
                            case "sysId":
                            case "createdOn":
                            case "updatedOn":
                                return true;
                        }
                    }

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

    //--//

    public static InstanceConfiguration getInstanceConfigurationNonNull(SessionHolder sessionHolder)
    {
        InstanceConfiguration cfg;

        try
        {
            cfg = SystemPreferenceRecord.getTypedValue(sessionHolder, SystemPreferenceTypedValue.InstanceConfiguration, InstanceConfiguration.class);
        }
        catch (IOException e)
        {
            HubApplication.LoggerInstance.error("Failed to access instance configuration: %s", e);
            cfg = null;
        }

        if (cfg == null)
        {
            cfg = new InstanceConfigurationDoNothing();
        }

        return cfg;
    }

    public static void setInstanceConfiguration(SessionHolder sessionHolder,
                                                InstanceConfiguration cfg)
    {
        try
        {
            SystemPreferenceRecord.setTypedValue(sessionHolder, SystemPreferenceTypedValue.InstanceConfiguration, cfg);
        }
        catch (Exception e)
        {
            HubApplication.LoggerInstance.error("Failed to set instance configuration: %s", e);
        }
    }
}
