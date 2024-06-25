/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.logic.deploy;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.optio3.cloud.ProxyFactory;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.model.customer.CustomerVertical;
import com.optio3.cloud.builder.model.deployment.DeploymentHost;
import com.optio3.cloud.builder.model.deployment.DeploymentHostServiceDetails;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.customer.BackupKind;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceBackupRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceSecretRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerSharedSecretRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerSharedUserRecord;
import com.optio3.cloud.builder.persistence.customer.EmbeddedDatabaseConfiguration;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.logic.BackgroundActivityHandler;
import com.optio3.cloud.persistence.EncryptedPayload;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.infra.WellKnownSites;
import com.optio3.infra.directory.RoleType;
import com.optio3.infra.directory.UserInfo;
import com.optio3.logging.ILogger;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.FileSystem;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public class DeployLogicForHub
{
    public static final String H2_DATABASE_LOCATION = "db";

    private static final String c_pref_instanceConfiguration = "sys_instanceConfiguration";
    private static final String c_pref_privateValues         = "sys_privateValues";
    private static final String c_pref_disableServiceWorker  = "sys_disableServiceWorker";

    //--//

    public final SessionProvider      sessionProvider;
    public final BuilderApplication   app;
    public final BuilderConfiguration appConfig;

    public final RecordLocator<CustomerServiceRecord> loc_service;
    public final String                               service_name;
    public final MetadataMap                          service_metadata;
    public final String                               service_url;
    public final EncryptedPayload                     service_pwd;
    public final CustomerVertical                     service_vertical;

    public final RecordLocator<CustomerRecord> loc_customer;
    public final String                        customer_name;

    private ProxyFactory m_proxyFactory;
    private boolean      m_loggedIn;

    private DeployLogicForAgent m_logic;

    private DeployLogicForHub(SessionHolder sessionHolder,
                              CustomerServiceRecord rec_svc)
    {
        sessionProvider = sessionHolder.getSessionProvider();
        app             = sessionHolder.getServiceNonNull(BuilderApplication.class);
        appConfig       = sessionHolder.getService(BuilderConfiguration.class);

        loc_service      = sessionHolder.createLocator(rec_svc);
        service_name     = rec_svc.getName();
        service_metadata = rec_svc.getMetadata();
        service_url      = rec_svc.getUrl();
        service_pwd      = rec_svc.getMaintPassword();
        service_vertical = rec_svc.getVertical();

        CustomerRecord rec_cust = rec_svc.getCustomer();
        loc_customer  = sessionHolder.createLocator(rec_cust);
        customer_name = rec_cust.getName();
    }

    public static DeployLogicForHub fromRecord(SessionHolder sessionHolder,
                                               CustomerServiceRecord rec_service)
    {
        return new DeployLogicForHub(sessionHolder, rec_service);
    }

    public static DeployLogicForHub fromLocator(SessionProvider sessionProvider,
                                                RecordLocator<CustomerServiceRecord> loc_service) throws
                                                                                                  Exception
    {
        return sessionProvider.computeInReadOnlySession(sessionHolder -> fromRecord(sessionHolder, sessionHolder.fromLocator(loc_service)));
    }

    //--//

    public int getServicePort() throws
                                MalformedURLException
    {
        URL url = new URL(service_url);

        int port = url.getPort();
        if (port > 0)
        {
            return port;
        }

        switch (url.getProtocol())
        {
            case "http":
                return 8080;

            case "https":
                return 443;

            default:
                throw Exceptions.newIllegalArgumentException("Unrecognized format for URL: %s", url);
        }
    }

    //--//

    private DeployLogicForAgent getLogic(boolean onlyRunning) throws
                                                              Exception
    {
        if (m_logic == null)
        {
            m_logic = sessionProvider.computeInReadOnlySession((sessionHolder) ->
                                                               {
                                                                   CustomerServiceRecord rec_svc = sessionHolder.fromLocator(loc_service);

                                                                   DeploymentHostRecord rec_host = null;

                                                                   DeploymentTaskRecord task = rec_svc.findAnyTaskForRole(sessionHolder, DeploymentStatus.Ready, DeploymentRole.hub);
                                                                   if (task != null)
                                                                   {
                                                                       rec_host = task.getDeployment();
                                                                   }

                                                                   if (rec_host == null && !onlyRunning)
                                                                   {
                                                                       for (DeploymentHost host : rec_svc.getInstancesForRole(sessionHolder, DeploymentRole.hub, true))
                                                                       {
                                                                           rec_host = sessionHolder.getEntityOrNull(DeploymentHostRecord.class, host.sysId);
                                                                           if (rec_host != null)
                                                                           {
                                                                               break;
                                                                           }
                                                                       }
                                                                   }

                                                                   if (rec_host == null)
                                                                   {
                                                                       throw Exceptions.newRuntimeException("No Hub running on service '%s'", rec_svc.getDisplayName());
                                                                   }

                                                                   return new DeployLogicForAgent(sessionHolder, rec_host);
                                                               });
        }

        return m_logic;
    }

    //--//

    public <T> T createHubProxy(Class<T> clz)
    {
        if (m_proxyFactory == null)
        {
            m_proxyFactory = new ProxyFactory(app.getService(ObjectMapper.class));
        }

        return m_proxyFactory.createProxy(service_url + "/api/v1", clz);
    }

    public void login(boolean useLDAP) throws
                                       Exception
    {
        if (!m_loggedIn)
        {
            com.optio3.cloud.client.hub.api.UsersApi userProxy = createHubProxy(com.optio3.cloud.client.hub.api.UsersApi.class);

            if (useLDAP)
            {
                UserInfo user = appConfig.getCredentialForHost(WellKnownSites.builderServer(), false, RoleType.Machine);
                userProxy.login(user.getEffectiveEmailAddress(), user.getEffectivePassword());
            }
            else
            {
                userProxy.login(BaseDeployLogic.MACHINE_ACCOUNT, appConfig.decrypt(service_pwd));
            }

            m_loggedIn = true;
        }
    }

    public void refreshAccounts() throws
                                  Exception
    {
        com.optio3.cloud.client.hub.api.RolesApi rolesProxy = createHubProxy(com.optio3.cloud.client.hub.api.RolesApi.class);

        Map<String, com.optio3.cloud.client.hub.model.RecordIdentity> mapRoles = Maps.newHashMap();

        for (com.optio3.cloud.client.hub.model.RecordIdentity ri : rolesProxy.getAll())
        {
            com.optio3.cloud.client.hub.model.Role role = rolesProxy.get(ri.sysId);
            mapRoles.put(role.name, ri);
        }

        com.optio3.cloud.client.hub.api.UsersApi usersProxy = createHubProxy(com.optio3.cloud.client.hub.api.UsersApi.class);

        Map<String, com.optio3.cloud.client.hub.model.User> existingUsers = Maps.newHashMap();
        for (com.optio3.cloud.client.hub.model.User oldUser : usersProxy.getAll())
        {
            existingUsers.put(oldUser.emailAddress, oldUser);
        }

        //--//

        ensureAccount(usersProxy,
                      existingUsers,
                      mapRoles,
                      false,
                      true,
                      BaseDeployLogic.MACHINE_ACCOUNT__FIRSTNAME,
                      BaseDeployLogic.MACHINE_ACCOUNT__LASTNAME,
                      BaseDeployLogic.MACHINE_ACCOUNT,
                      null,
                      appConfig.decrypt(service_pwd),
                      Lists.newArrayList(WellKnownRoleIds.Maintenance, WellKnownRoleIds.Machine));

        sessionProvider.callWithReadOnlySession(sessionHolder ->
                                                {
                                                    CustomerServiceRecord rec_svc  = sessionHolder.fromLocator(loc_service);
                                                    CustomerRecord        rec_cust = rec_svc.getCustomer();
                                                    for (CustomerSharedUserRecord rec_sharedUser : rec_cust.getSharedUsers())
                                                    {
                                                        ensureAccount(usersProxy,
                                                                      existingUsers,
                                                                      mapRoles,
                                                                      true,
                                                                      false,
                                                                      rec_sharedUser.getFirstName(),
                                                                      rec_sharedUser.getLastName(),
                                                                      rec_sharedUser.getEmailAddress(),
                                                                      rec_sharedUser.getPhoneNumber(),
                                                                      appConfig.decrypt(rec_sharedUser.getPassword()),
                                                                      rec_sharedUser.getRoles());
                                                    }
                                                });
    }

    private void ensureAccount(com.optio3.cloud.client.hub.api.UsersApi usersProxy,
                               Map<String, com.optio3.cloud.client.hub.model.User> existingUsers,
                               Map<String, com.optio3.cloud.client.hub.model.RecordIdentity> mapRoles,
                               boolean overwriteRoles,
                               boolean resetPassword,
                               String firstname,
                               String lastname,
                               String emailAddress,
                               String phoneNumber,
                               String password,
                               List<String> roles)
    {
        com.optio3.cloud.client.hub.model.User oldUser = existingUsers.get(emailAddress);
        if (oldUser != null)
        {
            if (overwriteRoles)
            {
                oldUser.roles = Lists.newArrayList();

                for (String role : roles)
                {
                    com.optio3.cloud.client.hub.model.RecordIdentity roleId = mapRoles.get(role);
                    if (roleId != null)
                    {
                        oldUser.roles.add(roleId);
                    }
                }

                usersProxy.update(oldUser.sysId, null, oldUser);
            }

            if (resetPassword)
            {
                usersProxy.changePassword(oldUser.sysId, password, password);
            }
        }
        else
        {
            com.optio3.cloud.client.hub.model.UserCreationRequest newUserRequest = new com.optio3.cloud.client.hub.model.UserCreationRequest();
            newUserRequest.firstName    = firstname;
            newUserRequest.lastName     = lastname;
            newUserRequest.emailAddress = emailAddress;
            newUserRequest.phoneNumber  = phoneNumber;
            newUserRequest.password     = password;

            for (String role : roles)
            {
                com.optio3.cloud.client.hub.model.RecordIdentity roleId = mapRoles.get(role);
                if (roleId != null)
                {
                    newUserRequest.roles.add(roleId);
                }
            }

            com.optio3.cloud.client.hub.model.User newUser = usersProxy.create(newUserRequest);
            existingUsers.put(newUser.sysId, newUser);
        }
    }

    //--//

    public void configureServiceWorker(Boolean disable)
    {
        com.optio3.cloud.client.hub.api.SystemPreferencesApi proxy = createHubProxy(com.optio3.cloud.client.hub.api.SystemPreferencesApi.class);

        if (disable == null)
        {
            proxy.removeValue(null, c_pref_disableServiceWorker);
        }
        else
        {
            proxy.setValue(null, c_pref_disableServiceWorker, disable.toString());
        }
    }

    //--//

    public void refreshSecrets() throws
                                 Exception
    {
        com.optio3.cloud.client.hub.api.SystemPreferencesApi proxy = createHubProxy(com.optio3.cloud.client.hub.api.SystemPreferencesApi.class);

        sessionProvider.callWithReadOnlySession(sessionHolder ->
                                                {
                                                    CustomerServiceRecord rec_svc  = sessionHolder.fromLocator(loc_service);
                                                    CustomerRecord        rec_cust = rec_svc.getCustomer();

                                                    proxy.removeSubKeys(c_pref_privateValues);

                                                    Multimap<String, com.optio3.cloud.client.hub.model.PrivateValue> map = HashMultimap.create();

                                                    for (CustomerSharedSecretRecord rec_sharedSecret : rec_cust.getSharedSecrets())
                                                    {
                                                        com.optio3.cloud.client.hub.model.PrivateValue pair = new com.optio3.cloud.client.hub.model.PrivateValue();
                                                        pair.key   = rec_sharedSecret.getKey();
                                                        pair.value = rec_svc.encryptForService(appConfig, appConfig.decrypt(rec_sharedSecret.getValue()));

                                                        map.put(rec_sharedSecret.getContext(), pair);
                                                    }

                                                    for (CustomerServiceSecretRecord rec_secret : rec_svc.getSecrets())
                                                    {
                                                        com.optio3.cloud.client.hub.model.PrivateValue pair = new com.optio3.cloud.client.hub.model.PrivateValue();
                                                        pair.key   = rec_secret.getKey();
                                                        pair.value = rec_svc.encryptForService(appConfig, appConfig.decrypt(rec_secret.getValue()));

                                                        map.put(rec_secret.getContext(), pair);
                                                    }

                                                    for (String context : map.keySet())
                                                    {
                                                        com.optio3.cloud.client.hub.model.PrivateValues payload = new com.optio3.cloud.client.hub.model.PrivateValues();
                                                        payload.values.addAll(map.get(context));

                                                        proxy.setValue(c_pref_privateValues, context, ObjectMappers.RestDefaults.writeValueAsString(payload));
                                                    }
                                                });
    }

    //--//

    public void applyVertical(ILogger logger)
    {
        if (service_vertical == null || !service_vertical.shouldApplyToInstance())
        {
            return;
        }

        final Class<? extends com.optio3.cloud.client.hub.model.InstanceConfiguration> handler = service_vertical.getHandler();

        try
        {
            com.optio3.cloud.client.hub.api.SystemPreferencesApi prefProxy = createHubProxy(com.optio3.cloud.client.hub.api.SystemPreferencesApi.class);

            com.optio3.cloud.client.hub.model.InstanceConfiguration cfg;

            final com.optio3.cloud.client.hub.model.SystemPreference prefValue = prefProxy.getValue(null, c_pref_instanceConfiguration);
            if (prefValue != null)
            {
                cfg = ObjectMappers.SkipNulls.readValue(prefValue.value, com.optio3.cloud.client.hub.model.InstanceConfiguration.class);
                if (cfg != null)
                {
                    Class<? extends com.optio3.cloud.client.hub.model.InstanceConfiguration> clz = cfg.getClass();
                    if (clz != handler)
                    {
                        logger.warn("Service '%s' already configured as %s", service_name, clz.getSimpleName());
                    }

                    return;
                }
            }

            cfg = Reflection.newInstance(handler);

            prefProxy.setValue(null, c_pref_instanceConfiguration, ObjectMappers.SkipNulls.writeValueAsString(cfg));

            logger.info("Service '%s' configured as %s", service_name, handler.getSimpleName());
        }
        catch (Throwable t)
        {
            logger.error("Failed to configure Vertical on '%s': %s", service_name, t);
        }
    }

    //--//

    public CompletableFuture<Boolean> transferBackupFromHub(String dockerId,
                                                            Path file,
                                                            BackupKind trigger,
                                                            DeployLogicForAgent.TransferProgress<RecordLocator<CustomerServiceBackupRecord>> transferProgress) throws
                                                                                                                                                               Exception
    {
        String backupFileName = file.getFileName()
                                    .toString();

        DeployLogicForAgent logic = getLogic(true);
        return logic.enumerateContainerFileSystem(dockerId, file, false, 20, transferProgress, (entry) ->
        {
            if (StringUtils.equals(entry.name, backupFileName))
            {
                try (FileSystem.TmpFileHolder backupOnDisk = entry.saveToDiskAsTempFile())
                {
                    sessionProvider.callWithSessionWithAutoCommit((sessionHolder) ->
                                                                  {
                                                                      CustomerServiceRecord rec_svc = sessionHolder.fromLocator(loc_service);
                                                                      CustomerServiceBackupRecord rec_backup = CustomerServiceBackupRecord.newInstance(rec_svc,
                                                                                                                                                       trigger,
                                                                                                                                                       BackgroundActivityHandler.DEFAULT_TIMESTAMP.format(
                                                                                                                                                               TimeUtils.now()),
                                                                                                                                                       entry.size,
                                                                                                                                                       null);

                                                                      EmbeddedDatabaseConfiguration db = rec_svc.getDbConfiguration();
                                                                      rec_backup.putMetadata(CustomerServiceBackupRecord.WellKnownMetadata.db_mode, db.getMode());

                                                                      rec_backup.saveFileToCloud(appConfig.credentials, backupOnDisk.get());

                                                                      rec_backup.saveSettings();

                                                                      sessionHolder.persistEntity(rec_backup);

                                                                      transferProgress.context = sessionHolder.createLocator(rec_backup);
                                                                  });
                }

                return false;
            }

            return true;
        });
    }

    public CompletableFuture<Boolean> transferBackupToHub(RecordLocator<CustomerServiceBackupRecord> loc_backup,
                                                          String containerId,
                                                          Path targetPath,
                                                          DeployLogicForAgent.TransferProgress<RecordLocator<CustomerServiceBackupRecord>> fileCopyProgress) throws
                                                                                                                                                             Exception
    {
        DeployLogicForAgent logic = getLogic(false);
        return logic.restoreFileSystem(containerId, targetPath, 20, (file) ->
        {
            sessionProvider.callWithReadOnlySession((sessionHolder ->
            {
                CustomerServiceBackupRecord rec_backup = sessionHolder.fromLocator(loc_backup);
                rec_backup.loadFileFromCloud(appConfig.credentials, file);
            }));
        }, fileCopyProgress);
    }

    //--//

    public DeploymentHostServiceDetails getGatewayDetails(DeploymentHostRecord rec_host,
                                                          boolean force) throws
                                                                         Exception
    {
        var details = rec_host.getRemoteDetails();
        if (details != null && TimeUtils.wasUpdatedRecently(details.lastFetch, 1, TimeUnit.DAYS))
        {
            if (!force)
            {
                return details;
            }
        }

        if (service_url == null)
        {
            return null;
        }

        login(false);

        com.optio3.cloud.client.hub.api.GatewaysApi    adminProxy = createHubProxy(com.optio3.cloud.client.hub.api.GatewaysApi.class);
        com.optio3.cloud.client.hub.model.GatewayAsset asset      = adminProxy.lookup(rec_host.getHostId());
        if (asset == null)
        {
            return null;
        }

        details                  = new DeploymentHostServiceDetails();
        details.lastFetch        = TimeUtils.nowUtc();
        details.name             = asset.name;
        details.remoteSysId      = asset.sysId;
        details.lastUpdatedDate  = asset.lastUpdatedDate;
        details.url              = service_url + "/#/gateways/gateway/" + asset.sysId;
        details.warningThreshold = asset.warningThreshold;
        details.alertThreshold   = asset.alertThreshold;

        if (asset.details != null)
        {
            details.lastRefresh = asset.details.lastRefresh;
            details.queueStatus = asset.details.queueStatus;
        }

        rec_host.setRemoteDetails(details);

        return details;
    }

    public String changeThresholds(DeploymentHostRecord rec_host,
                                   Integer warningThreshold,
                                   Integer alertThreshold) throws
                                                           Exception
    {
        if (warningThreshold != null)
        {
            rec_host.setWarningThreshold(warningThreshold);
        }

        var details = getGatewayDetails(rec_host, false);
        if (details == null)
        {
            return String.format("Cannot resolve HostId '%s' on %s", rec_host.getHostId(), rec_host.getDisplayName());
        }

        int newWarningThreshold = BoxingUtils.get(warningThreshold, details.warningThreshold);
        int newAlertThreshold   = BoxingUtils.get(alertThreshold, details.alertThreshold);

        if (newWarningThreshold != details.warningThreshold || newAlertThreshold != details.alertThreshold)
        {
            try
            {
                com.optio3.cloud.client.hub.api.AssetsApi      assetProxy   = createHubProxy(com.optio3.cloud.client.hub.api.AssetsApi.class);
                com.optio3.cloud.client.hub.model.Asset        asset        = assetProxy.get(details.remoteSysId);
                com.optio3.cloud.client.hub.model.GatewayAsset gatewayAsset = Reflection.as(asset, com.optio3.cloud.client.hub.model.GatewayAsset.class);
                if (gatewayAsset == null)
                {
                    return String.format("Cannot resolve Gateway '%s' on %s", rec_host.getHostId(), rec_host.getDisplayName());
                }

                if (warningThreshold != null)
                {
                    gatewayAsset.warningThreshold = warningThreshold;
                }

                if (alertThreshold != null)
                {
                    gatewayAsset.alertThreshold = alertThreshold;
                }

                assetProxy.update(details.remoteSysId, null, gatewayAsset);

                details.warningThreshold = newWarningThreshold;
                details.alertThreshold   = newAlertThreshold;
            }
            catch (javax.ws.rs.NotFoundException e)
            {
                details = null;
            }

            rec_host.setRemoteDetails(details);
        }

        return null;
    }
}
