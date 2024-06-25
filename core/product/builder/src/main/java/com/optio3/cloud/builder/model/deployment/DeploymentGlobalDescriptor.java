/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.model.customer.Customer;
import com.optio3.cloud.builder.model.customer.CustomerService;
import com.optio3.cloud.builder.model.customer.CustomerServiceBackup;
import com.optio3.cloud.builder.model.customer.RoleAndArchitectureWithImage;
import com.optio3.cloud.builder.model.jobs.output.RegistryImage;
import com.optio3.cloud.builder.model.jobs.output.RegistryImageReleaseStatus;
import com.optio3.cloud.builder.model.jobs.output.RegistryTaggedImage;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceBackupRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryImageRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.channel.DatabaseActivity;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.DbEvent;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class DeploymentGlobalDescriptor
{
    public static final Logger LoggerInstance = new Logger(DeploymentGlobalDescriptor.class);

    public static class Settings
    {
        public boolean loadImages;
        public boolean loadDeployments;
        public boolean loadServices;
        public boolean loadBackups;
        public boolean linkImages;
    }

    public static class Spooler
    {
        class CachePerTable<T, R extends BaseModel>
        {
            private final Class<T>                               m_clzTable;
            private final Class<R>                               m_clzRaw;
            private final BiConsumer<SessionHolder, Consumer<R>> m_streamCallback;
            private       ConcurrentMap<String, R>               lookup;

            CachePerTable(Class<T> clzTable,
                          Class<R> clzRaw,
                          BiConsumer<SessionHolder, Consumer<R>> streamCallback)
            {
                m_clzTable       = clzTable;
                m_clzRaw         = clzRaw;
                m_streamCallback = streamCallback;
                m_regDbActivity.subscribeToTable(clzTable, this::flush);
            }

            ConcurrentMap<String, R> fetch(SessionHolder sessionHolder)
            {
                synchronized (m_streamCallback)
                {
                    ConcurrentMap<String, R> map = lookup;
                    if (map == null)
                    {
                        map = Maps.newConcurrentMap();

                        ConcurrentMap<String, R> finalMap = map;

                        m_streamCallback.accept(sessionHolder, (raw) ->
                        {
                            finalMap.put(raw.sysId, raw);
                        });

                        lookup = map;
                    }

                    return map;
                }
            }

            private void flush(DbEvent dbEvent)
            {
                synchronized (m_streamCallback)
                {
                    ConcurrentMap<String, R> map = lookup;

                    if (map != null)
                    {
                        switch (dbEvent.action)
                        {
                            case UPDATE_DIRECT:
                            case INSERT:
                                lookup = null;
                                break;

                            case DELETE:
                                map.remove(dbEvent.context.sysId);
                                break;
                        }
                    }
                }

                Spooler.this.flush(dbEvent);
            }
        }

        private final BuilderApplication                      m_app;
        private final Map<String, DeploymentGlobalDescriptor> m_cache = Maps.newHashMap();

        private DatabaseActivity.LocalSubscriber m_regDbActivity;

        private CachePerTable<RegistryImageRecord, RegistryImage>             m_cacheRegistryImages;
        private CachePerTable<RegistryTaggedImageRecord, RegistryTaggedImage> m_cacheRegistryTaggedImages;

        private CachePerTable<DeploymentHostRecord, DeploymentHost>   m_cacheDeploymentHosts;
        private CachePerTable<DeploymentTaskRecord, DeploymentTask>   m_cacheDeploymentTasks;
        private CachePerTable<DeploymentAgentRecord, DeploymentAgent> m_cacheDeploymentAgents;

        private CachePerTable<CustomerRecord, Customer>                           m_cacheCustomers;
        private CachePerTable<CustomerServiceRecord, CustomerService>             m_cacheCustomerServices;
        private CachePerTable<CustomerServiceBackupRecord, CustomerServiceBackup> m_cacheCustomerServiceBackups;

        public Spooler(BuilderApplication app)
        {
            m_app = app;

            app.registerService(Spooler.class, () -> this);
        }

        public void initialize()
        {
            m_regDbActivity = DatabaseActivity.LocalSubscriber.create(m_app.getServiceNonNull(MessageBusBroker.class));

            m_cacheRegistryImages = new CachePerTable<>(RegistryImageRecord.class, RegistryImage.class, (sessionHolder, callback) ->
            {
                RegistryImageRecord.streamAllRaw(sessionHolder, null, callback);
            });

            m_cacheRegistryTaggedImages = new CachePerTable<>(RegistryTaggedImageRecord.class, RegistryTaggedImage.class, (sessionHolder, callback) ->
            {
                RegistryTaggedImageRecord.streamAllRaw(sessionHolder, null, callback);
            });

            //--//

            m_cacheDeploymentHosts = new CachePerTable<>(DeploymentHostRecord.class, DeploymentHost.class, (sessionHolder, callback) ->
            {
                DeploymentHostRecord.streamAllRaw(sessionHolder, null, callback);
            });

            m_cacheDeploymentTasks = new CachePerTable<>(DeploymentTaskRecord.class, DeploymentTask.class, (sessionHolder, callback) ->
            {
                DeploymentTaskRecord.streamAllRaw(sessionHolder, null, callback);
            });

            m_cacheDeploymentAgents = new CachePerTable<>(DeploymentAgentRecord.class, DeploymentAgent.class, (sessionHolder, callback) ->
            {
                DeploymentAgentRecord.streamAllRaw(sessionHolder, null, callback);
            });

            //--//

            m_cacheCustomers = new CachePerTable<>(CustomerRecord.class, Customer.class, (sessionHolder, callback) ->
            {
                CustomerRecord.streamAllRaw(sessionHolder, null, callback);
            });

            m_cacheCustomerServices = new CachePerTable<>(CustomerServiceRecord.class, CustomerService.class, (sessionHolder, callback) ->
            {
                CustomerServiceRecord.streamAllRaw(sessionHolder, null, callback);
            });

            m_cacheCustomerServiceBackups = new CachePerTable<>(CustomerServiceBackupRecord.class, CustomerServiceBackup.class, (sessionHolder, callback) ->
            {
                CustomerServiceBackupRecord.streamAllRaw(sessionHolder, null, callback);
            });
        }

        public void close() throws
                            Exception
        {
            if (m_regDbActivity != null)
            {
                m_regDbActivity.close();
                m_regDbActivity = null;
            }
        }

        private void flush(DbEvent dbEvent)
        {
            switch (dbEvent.action)
            {
                case UPDATE_DIRECT:
                case INSERT:
                case DELETE:
                    synchronized (m_cache)
                    {
                        m_cache.clear();
                    }

                    if (LoggerInstance.isEnabled(Severity.Debug))
                    {
                        LoggerInstance.debug("Invalidated due to %s", ObjectMappers.toJsonNoThrow(null, dbEvent));
                    }
                    break;
            }
        }

        DeploymentGlobalDescriptor get(SessionHolder sessionHolder,
                                       Settings settings)
        {
            Stopwatch                  sw  = null;
            String                     key = ObjectMappers.toJsonNoThrow(null, settings);
            DeploymentGlobalDescriptor desc;
            synchronized (m_cache)
            {
                desc = m_cache.get(key);
                if (desc == null)
                {
                    sw   = Stopwatch.createStarted();
                    desc = new DeploymentGlobalDescriptor(this, sessionHolder, settings);
                    m_cache.put(key, desc);
                    sw.stop();
                }
            }

            if (sw != null)
            {
                LoggerInstance.debug("Refresh: %,d msec for %s", sw.elapsed(TimeUnit.MILLISECONDS), key);
            }

            return desc;
        }

        DeploymentGlobalDescriptor get(SessionProvider sessionProvider,
                                       Settings settings) throws
                                                          Exception
        {
            Stopwatch                  sw  = null;
            String                     key = ObjectMappers.toJsonNoThrow(null, settings);
            DeploymentGlobalDescriptor desc;
            synchronized (m_cache)
            {
                desc = m_cache.get(key);
                if (desc == null)
                {
                    sw   = Stopwatch.createStarted();
                    desc = sessionProvider.computeInReadOnlySession(sessionHolder -> new DeploymentGlobalDescriptor(this, sessionHolder, settings));
                    m_cache.put(key, desc);
                    sw.stop();
                }
            }

            if (sw != null)
            {
                LoggerInstance.debug("Refresh: %,d msec for %s", sw.elapsed(TimeUnit.MILLISECONDS), key);
            }

            return desc;
        }
    }

    public final Map<String, RegistryImage>       images       = Maps.newHashMap();
    public final Map<String, RegistryTaggedImage> taggedImages = Maps.newHashMap();

    public final Map<String, DeploymentHost>  hosts  = Maps.newHashMap();
    public final Map<String, DeploymentTask>  tasks  = Maps.newHashMap();
    public final Map<String, DeploymentAgent> agents = Maps.newHashMap();

    public final Map<String, Customer>              customers = Maps.newHashMap();
    public final Map<String, CustomerService>       services  = Maps.newHashMap();
    public final Map<String, CustomerServiceBackup> backups   = Maps.newHashMap();

    public final Multimap<String, DeploymentTask>        imageToTasks    = HashMultimap.create();
    public final Multimap<String, CustomerService>       imageToServices = HashMultimap.create();
    public final Multimap<String, CustomerServiceBackup> imageToBackups  = HashMultimap.create();

    private DeploymentGlobalDescriptor(Spooler spooler,
                                       SessionHolder sessionHolder,
                                       Settings settings)
    {
        Stopwatch sw = Stopwatch.createStarted();

        if (settings.linkImages)
        {
            settings.loadImages   = true;
            settings.loadServices = true;
            settings.loadBackups  = true;
        }

        if (settings.loadImages)
        {
            populateMap("images", images, () -> spooler.m_cacheRegistryImages.fetch(sessionHolder));
            populateMap("taggedImages", taggedImages, () -> spooler.m_cacheRegistryTaggedImages.fetch(sessionHolder));

            for (RegistryTaggedImage rawTaggedImage : taggedImages.values())
            {
                if (rawTaggedImage.image != null)
                {
                    RegistryImage rawImage = images.get(rawTaggedImage.image.sysId);
                    if (rawImage != null)
                    {
                        rawTaggedImage.rawImage = rawImage;

                        switch (rawTaggedImage.releaseStatus)
                        {
                            case Release:
                                rawImage.isRelease = true;
                                break;

                            case ReleaseCandidate:
                                rawImage.isReleaseCandidate = true;
                                break;
                        }
                    }
                }
            }
        }

        if (settings.loadDeployments)
        {
            populateMap("hosts", hosts, () -> spooler.m_cacheDeploymentHosts.fetch(sessionHolder));
            populateMap("tasks", tasks, () -> spooler.m_cacheDeploymentTasks.fetch(sessionHolder));
            populateMap("agents", agents, () -> spooler.m_cacheDeploymentAgents.fetch(sessionHolder));

            {
                Multimap<String, DeploymentTask> lookup = ArrayListMultimap.create();

                for (DeploymentTask task : tasks.values())
                {
                    if (task.imageReference != null)
                    {
                        task.rawImage = images.get(task.imageReference.sysId);
                    }

                    if (task.deployment != null)
                    {
                        lookup.put(task.deployment.sysId, task);
                    }
                }

                for (DeploymentHost host : hosts.values())
                {
                    Collection<DeploymentTask> entries = lookup.get(host.sysId);

                    entries.toArray(host.rawTasks = new DeploymentTask[entries.size()]);
                }
            }

            {
                Multimap<String, DeploymentAgent> lookup = ArrayListMultimap.create();

                for (DeploymentAgent agent : agents.values())
                {
                    if (agent.deployment != null)
                    {
                        lookup.put(agent.deployment.sysId, agent);
                    }
                }

                for (DeploymentHost host : hosts.values())
                {
                    Collection<DeploymentAgent> entries = lookup.get(host.sysId);

                    entries.toArray(host.rawAgents = new DeploymentAgent[entries.size()]);

                    for (DeploymentAgent rawAgent : host.rawAgents)
                    {
                        if (rawAgent.dockerId != null)
                        {
                            for (DeploymentTask rawTask : host.rawTasks)
                            {
                                if (StringUtils.equals(rawTask.dockerId, rawAgent.dockerId))
                                {
                                    rawAgent.rawTask = rawTask;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (settings.loadServices)
        {
            populateMap("customers", customers, () -> spooler.m_cacheCustomers.fetch(sessionHolder));
            populateMap("services", services, () -> spooler.m_cacheCustomerServices.fetch(sessionHolder));

            {
                Multimap<String, CustomerService> lookup = ArrayListMultimap.create();

                for (CustomerService svc : services.values())
                {
                    if (svc.customer != null)
                    {
                        lookup.put(svc.customer.sysId, svc);
                    }
                }

                for (Customer cust : customers.values())
                {
                    Collection<CustomerService> entries = lookup.get(cust.sysId);

                    entries.toArray(cust.rawServices = new CustomerService[entries.size()]);

                    for (CustomerService rawService : cust.rawServices)
                    {
                        rawService.rawCustomer = cust;
                    }
                }
            }

            if (settings.loadDeployments)
            {
                Multimap<String, DeploymentHost> lookup = ArrayListMultimap.create();

                for (DeploymentHost host : hosts.values())
                {
                    if (host.customerService != null)
                    {
                        lookup.put(host.customerService.sysId, host);
                    }
                }

                for (CustomerService svc : services.values())
                {
                    Collection<DeploymentHost> entries = lookup.get(svc.sysId);

                    entries.toArray(svc.rawHosts = new DeploymentHost[entries.size()]);

                    for (DeploymentHost rawHost : svc.rawHosts)
                    {
                        rawHost.rawService = svc;
                    }
                }
            }
        }

        if (settings.loadBackups)
        {
            populateMap("backups", backups, () -> spooler.m_cacheCustomerServiceBackups.fetch(sessionHolder));

            if (settings.loadServices)
            {
                Multimap<String, CustomerServiceBackup> lookup = ArrayListMultimap.create();

                for (CustomerServiceBackup backup : backups.values())
                {
                    if (backup.customerService != null)
                    {
                        lookup.put(backup.customerService.sysId, backup);
                    }
                }

                for (CustomerService svc : services.values())
                {
                    Collection<CustomerServiceBackup> entries = lookup.get(svc.sysId);

                    entries.toArray(svc.rawBackups = new CustomerServiceBackup[entries.size()]);
                }
            }
        }

        if (settings.linkImages)
        {
            for (DeploymentTask task : tasks.values())
            {
                RegistryImage image = task.rawImage;
                if (image != null)
                {
                    imageToTasks.put(image.sysId, task);
                }
            }

            for (CustomerService svc : services.values())
            {
                svc.roleImages = CustomerServiceRecord.WellKnownMetadata.role_images.get(svc.decodeMetadata());
                for (RoleAndArchitectureWithImage roleImage : svc.roleImages)
                {
                    RegistryTaggedImage taggedImage = resolveTaggedImage(roleImage.image);
                    if (taggedImage != null)
                    {
                        RegistryImage image = taggedImage.rawImage;
                        if (image != null)
                        {
                            imageToServices.put(image.sysId, svc);
                        }
                    }
                }
            }

            for (CustomerServiceBackup backup : backups.values())
            {
                backup.roleImages = CustomerServiceBackupRecord.WellKnownMetadata.role_images.get(backup.decodeMetadata());
                for (RoleAndArchitectureWithImage roleImage : backup.roleImages)
                {
                    RegistryTaggedImage taggedImage = resolveTaggedImage(roleImage.image);
                    if (taggedImage != null)
                    {
                        RegistryImage image = taggedImage.rawImage;
                        if (image != null)
                        {
                            imageToBackups.put(image.sysId, backup);
                        }
                    }
                }
            }
        }

        sw.stop();

        LoggerInstance.debugVerbose("Summary: %,d msec", sw.elapsed(TimeUnit.MILLISECONDS));
    }

    public static DeploymentGlobalDescriptor get(SessionHolder sessionHolder,
                                                 Settings settings)
    {
        Spooler spooler = sessionHolder.getServiceNonNull(Spooler.class);

        return spooler.get(sessionHolder, settings);
    }

    public static DeploymentGlobalDescriptor get(SessionProvider sessionProvider,
                                                 Settings settings) throws
                                                                    Exception
    {
        Spooler spooler = sessionProvider.getServiceNonNull(Spooler.class);

        return spooler.get(sessionProvider, settings);
    }

    //--//

    public CustomerService getService(TypedRecordIdentity<CustomerServiceRecord> ri)
    {
        return ri != null ? services.get(ri.sysId) : null;
    }

    public CustomerService getService(RecordLocator<CustomerServiceRecord> loc)
    {
        return loc != null ? services.get(loc.getIdRaw()) : null;
    }

    public CustomerService getService(CustomerServiceRecord rec)
    {
        return rec != null ? services.get(rec.getSysId()) : null;
    }

    public DeploymentHost getHost(TypedRecordIdentity<DeploymentHostRecord> ri)
    {
        return ri != null ? hosts.get(ri.sysId) : null;
    }

    //--//

    public RegistryTaggedImage resolveTaggedImage(TypedRecordIdentity<RegistryTaggedImageRecord> ri)
    {
        return ri != null ? taggedImages.get(ri.sysId) : null;
    }

    public RegistryImage resolveImage(TypedRecordIdentity<RegistryImageRecord> ri)
    {
        return ri != null ? images.get(ri.sysId) : null;
    }

    public List<RegistryTaggedImage> findImagesForService(CustomerService svc)
    {
        return findImages(svc != null ? svc.roleImages : null);
    }

    public List<RegistryTaggedImage> findImagesForBackup(CustomerServiceBackup backup)
    {
        return findImages(backup != null ? backup.roleImages : null);
    }

    private List<RegistryTaggedImage> findImages(List<RoleAndArchitectureWithImage> lst)
    {
        Map<String, RegistryTaggedImage> found = Maps.newHashMap();

        if (lst != null)
        {
            for (RoleAndArchitectureWithImage roleImage : lst)
            {
                RegistryTaggedImage taggedImage = resolveTaggedImage(roleImage.image);
                if (taggedImage != null)
                {
                    found.put(taggedImage.sysId, taggedImage);
                }
            }
        }

        return Lists.newArrayList(found.values());
    }

    public List<RegistryTaggedImage> getReferencingTags(String imageSysId)
    {
        return CollectionUtils.filter(taggedImages.values(), (o) -> sameSysId(o.image, imageSysId));
    }

    public RegistryTaggedImage findCompatibleImage(RegistryImageReleaseStatus releaseStatus,
                                                   DeploymentRole role,
                                                   DockerImageArchitecture architecture)
    {
        for (RegistryTaggedImage taggedImage : taggedImages.values())
        {
            if (taggedImage.releaseStatus == releaseStatus)
            {
                RegistryImage image = images.get(taggedImage.image.sysId);
                if (image.architecture == architecture && image.targetService == role)
                {
                    return taggedImage;
                }
            }
        }

        return null;
    }

    //--//

    @FunctionalInterface
    interface Generator<T>
    {
        ConcurrentMap<String, T> generate();
    }

    private static boolean sameSysId(TypedRecordIdentity<?> ri,
                                     String sysId)
    {
        String sysId2 = ri != null ? ri.sysId : null;
        return StringUtils.equals(sysId, sysId2);
    }

    private static <T extends BaseModel> void populateMap(String tag,
                                                          Map<String, T> map,
                                                          Generator<T> generator)
    {
        Stopwatch sw = Stopwatch.createStarted();

        map.putAll(generator.generate());

        sw.stop();

        LoggerInstance.debugObnoxious("%s: %,d msec for %d entries", tag, sw.elapsed(TimeUnit.MILLISECONDS), map.size());
    }
}
