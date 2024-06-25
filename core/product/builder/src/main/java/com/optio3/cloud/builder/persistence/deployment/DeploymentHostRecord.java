/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.deployment;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ProcessingException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.AbstractApplicationWithDatabase;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.annotation.Optio3UpgradeValue;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.model.customer.Customer;
import com.optio3.cloud.builder.model.customer.CustomerService;
import com.optio3.cloud.builder.model.customer.CustomerVertical;
import com.optio3.cloud.builder.model.deployment.BootOptions;
import com.optio3.cloud.builder.model.deployment.DeploymentAgentUpgrade;
import com.optio3.cloud.builder.model.deployment.DeploymentAgentUpgradeDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentCellularCharge;
import com.optio3.cloud.builder.model.deployment.DeploymentCellularCharges;
import com.optio3.cloud.builder.model.deployment.DeploymentCellularCommunications;
import com.optio3.cloud.builder.model.deployment.DeploymentCellularSession;
import com.optio3.cloud.builder.model.deployment.DeploymentCellularSessions;
import com.optio3.cloud.builder.model.deployment.DeploymentCellularStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentGlobalDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentHost;
import com.optio3.cloud.builder.model.deployment.DeploymentHostDetails;
import com.optio3.cloud.builder.model.deployment.DeploymentHostFilterRequest;
import com.optio3.cloud.builder.model.deployment.DeploymentHostFilterStatusPair;
import com.optio3.cloud.builder.model.deployment.DeploymentHostFlavor;
import com.optio3.cloud.builder.model.deployment.DeploymentHostImage;
import com.optio3.cloud.builder.model.deployment.DeploymentHostOnlineSession;
import com.optio3.cloud.builder.model.deployment.DeploymentHostOnlineSessions;
import com.optio3.cloud.builder.model.deployment.DeploymentHostProvisioningInfo;
import com.optio3.cloud.builder.model.deployment.DeploymentHostProvisioningNotes;
import com.optio3.cloud.builder.model.deployment.DeploymentHostServiceDetails;
import com.optio3.cloud.builder.model.deployment.DeploymentHostStatusDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentHostStatusDescriptorFlag;
import com.optio3.cloud.builder.model.deployment.DeploymentOperationalResponsiveness;
import com.optio3.cloud.builder.model.deployment.DeploymentOperationalStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.model.jobs.output.RegistryImageReleaseStatus;
import com.optio3.cloud.builder.model.provision.ProvisionReport;
import com.optio3.cloud.builder.orchestration.tasks.bookkeeping.TaskForServiceBatteryThresholds;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentInstance;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForDelayedOperations;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForHostTermination;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.config.SystemPreferenceRecord;
import com.optio3.cloud.builder.persistence.config.SystemPreferenceTypedValue;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedAgentCreation;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryImageRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.client.deployer.model.DeployerCellularInfo;
import com.optio3.cloud.client.deployer.model.DeployerShutdownConfiguration;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.client.deployer.model.ImageStatus;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.formatting.TabularField;
import com.optio3.cloud.formatting.TabularReportAsExcel;
import com.optio3.cloud.messagebus.channel.RpcConnectionInfo;
import com.optio3.cloud.model.SortCriteria;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.AbstractQueryHelperBase;
import com.optio3.cloud.persistence.LogHandler;
import com.optio3.cloud.persistence.MetadataField;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.PersistAsJsonHelper;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RawQueryHelper;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.RecordWithMetadata;
import com.optio3.cloud.persistence.RecordWithMetadata_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.search.Optio3QueryAnalyzerOverride;
import com.optio3.concurrency.Executors;
import com.optio3.infra.WellKnownDockerImageLabel;
import com.optio3.infra.cellular.CellularProvider;
import com.optio3.infra.cellular.ICellularProviderHandler;
import com.optio3.infra.waypoint.BootConfig;
import com.optio3.logging.ILogger;
import com.optio3.logging.Logger;
import com.optio3.logging.RedirectingLogger;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.service.IServiceProvider;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

@Entity
@Table(name = "DEPLOYMENT_HOST")
@DynamicUpdate // Due to HHH-11506
@Indexed
@Analyzer(definition = "fuzzy")
@Optio3QueryAnalyzerOverride("fuzzy_query")
@Optio3TableInfo(externalId = "DeploymentHost", model = DeploymentHost.class, metamodel = DeploymentHostRecord_.class, metadata = DeploymentHostRecord.WellKnownMetadata.class)
public class DeploymentHostRecord extends RecordWithMetadata implements LogHandler.ILogHost<DeploymentHostLogRecord>,
                                                                        ModelMapperTarget<DeploymentHost, DeploymentHostRecord_>
{
    public static class WellKnownMetadata implements Optio3TableInfo.IMetadataDigest
    {
        public static final MetadataField<AgentFailures>   agentFailures   = new MetadataField<>("agentFailures", AgentFailures.class);
        public static final MetadataField<ZonedDateTime>   nextImagesPrune = new MetadataField<>("nextImagesPrune", ZonedDateTime.class);
        public static final MetadataField<AgentStatistics> imagesHistory   = new MetadataField<>("imagesHistory", AgentStatistics.class);

        public static final MetadataField<DelayedOperations> delayedOperations = new MetadataField<>("delayedOperations", DelayedOperations.class);

        public static final MetadataField<ZonedDateTime> activeAgentUnresponsive = new MetadataField<>("activeAgentUnresponsive", ZonedDateTime.class);
        public static final MetadataField<ZonedDateTime> duplicateAgentsShutdown = new MetadataField<>("duplicateAgentsShutdown", ZonedDateTime.class);
        public static final MetadataField<ZonedDateTime> lastTaskRestart         = new MetadataField<>("lastTaskRestart", ZonedDateTime.class);
        public static final MetadataField<Integer>       countTaskRestart        = new MetadataField<>("countTaskRestart", Integer.class);

        public static final MetadataField<UsersNotification> notifyWhenOnline = new MetadataField<>("notifyWhenOnline", UsersNotification.class);
        public static final MetadataField<Boolean>           logRPC           = new MetadataField<>("logRPC", Boolean.class, () -> false);

        public static final MetadataField<String>             instanceAccount   = new MetadataField<>("instanceAccount", String.class);
        public static final MetadataField<DeploymentInstance> instanceType      = new MetadataField<>("instanceType", DeploymentInstance.class, () -> DeploymentInstance.Edge);
        public static final MetadataField<String>             instanceRegion    = new MetadataField<>("instanceRegion", String.class);
        public static final MetadataField<String>             instanceIp        = new MetadataField<>("instanceIp", String.class);
        public static final MetadataField<ZonedDateTime>      warningLowCredits = new MetadataField<>("warningLowCredits", ZonedDateTime.class);
        public static final MetadataField<ZonedDateTime>      warningHighCpu    = new MetadataField<>("warningHighCpu", ZonedDateTime.class);

        public static final MetadataField<BootOptions>   bootOptions     = new MetadataField<>("bootOptions", BootOptions.class);
        public static final MetadataField<ZonedDateTime> frontendTimeout = new MetadataField<>("frontendTimeout", ZonedDateTime.class);

        public static final MetadataField<DeploymentHostDetails>         cellularDetails   = new MetadataField<>("cellularDetails", DeploymentHostDetails.class);
        public static final MetadataField<DeploymentCellularCharges>     cellularCharges   = new MetadataField<>("cellularCharges", DeploymentCellularCharges.class);
        public static final MetadataField<List<DeploymentHostImage>>     imageDetails      = new MetadataField<>("imageDetails", s_imagesTypeRef, Lists::newArrayList);
        public static final MetadataField<DeployerShutdownConfiguration> batteryThresholds = new MetadataField<>("batteryThresholds", DeployerShutdownConfiguration.class);

        public static final MetadataField<DeploymentHostProvisioningInfo> provisioningInfo    = new MetadataField<>("provisioningInfo", DeploymentHostProvisioningInfo.class);
        public static final MetadataField<ZonedDateTime>                  provisioningCheckin = new MetadataField<>("provisioningCheckin", ZonedDateTime.class);
        public static final MetadataField<String>                         preparedForCustomer = new MetadataField<>("preparedForCustomer", String.class);
        public static final MetadataField<String>                         preparedForService  = new MetadataField<>("preparedForService", String.class);

        public static final MetadataField<DeploymentHostOnlineSessions> onlineSessions = new MetadataField<>("onlineSessions", DeploymentHostOnlineSessions.class);

        public static final MetadataField<DeploymentHostServiceDetails> remoteDetails = new MetadataField<>("remoteDetails", DeploymentHostServiceDetails.class);
    }

    public static class AgentStatistics
    {
        public final Map<String, ZonedDateTime> imageTimestamp = Maps.newHashMap();

        //--//

        public void pruneDeletedImages(List<ImageStatus> images)
        {
            Set<String> foundImages = Sets.newHashSet();

            for (ImageStatus image : images)
            {
                foundImages.add(image.id);
            }

            imageTimestamp.keySet()
                          .removeIf(img -> !foundImages.contains(img));
        }

        public boolean isStale(ZonedDateTime now,
                               ZonedDateTime threshold,
                               String id)
        {
            ZonedDateTime lastInUse = imageTimestamp.get(id);
            if (lastInUse == null)
            {
                // First time through, set a delay to the deletion.
                markAsInUse(now, id);

                return false;
            }

            if (lastInUse.isAfter(threshold))
            {
                return false;
            }

            return true;
        }

        public void markAsInUse(ZonedDateTime now,
                                String id)
        {
            imageTimestamp.put(id, now);
        }
    }

    public static class AgentFailures
    {
        public final Map<String, ZonedDateTime> lookup = Maps.newHashMap();
    }

    public static class UsersNotification
    {
        public List<String> users = Lists.newArrayList();
    }

    public static class SequenceNumbers
    {
        public final Map<String, Integer> lookupByPrefix = Maps.newHashMap();
    }

    private static final TypeReference<List<DeploymentHostImage>> s_imagesTypeRef = new TypeReference<List<DeploymentHostImage>>()
    {
    };

    //--//

    public interface LoggerContext
    {
        RecordLocator<DeploymentHostRecord> getHostLocator();
    }

    public static ILogger buildContextualLogger(ILogger inner,
                                                RecordLocator<DeploymentHostRecord> loc)
    {
        if (loc == null)
        {
            return inner;
        }

        class SubLogger extends RedirectingLogger implements LoggerContext
        {
            public SubLogger(ILogger outerLoggerInstance)
            {
                super(outerLoggerInstance);
            }

            @Override
            public RecordLocator<DeploymentHostRecord> getHostLocator()
            {
                return loc;
            }
        }

        return new SubLogger(inner);
    }

    //--//

    static class RowForReport
    {
        @TabularField(order = 0, title = "HostId")
        public String col_hostId;

        @TabularField(order = 1, title = "HostName")
        public String col_hostName;

        @TabularField(order = 2, title = "DisplayName")
        public String col_remoteName;

        @TabularField(order = 3, title = "Customer")
        public String col_customer;

        @TabularField(order = 4, title = "Service")
        public String col_service;

        @TabularField(order = 5, title = "Vertical")
        public CustomerVertical col_vertical;

        @TabularField(order = 6, title = "State")
        public DeploymentOperationalStatus col_state;

        @TabularField(order = 7, title = "IMSI")
        public String col_imsi;

        @TabularField(order = 8, title = "IMEI")
        public String col_imei;

        @TabularField(order = 9, title = "ICCID")
        public String col_iccid;

        @TabularField(order = 10, title = "Cellular Provider")
        public CellularProvider col_cellularProvider;

        @TabularField(order = 11, title = "Last Heartbeat", format = "yyyy-MM-dd")
        public ZonedDateTime col_lastHeartbeat;

        @TabularField(order = 12, title = "Manufacturing Site")
        public String col_manufacturingSite;

        @TabularField(order = 13, title = "Manufacturing Date", format = "yyyy-MM-dd")
        public ZonedDateTime col_manufacturingDate;

        @TabularField(order = 14, title = "Shipping Date", format = "yyyy-MM-dd")
        public ZonedDateTime col_shipping;

        @TabularField(order = 15, title = "Deployment Date", format = "yyyy-MM-dd")
        public ZonedDateTime col_deployed;

        @TabularField(order = 16, title = "Billing Start", format = "yyyy-MM-dd")
        public ZonedDateTime col_billing;
    }

    //--//

    public static class ForCellular
    {
    }

    public static final Logger LoggerInstance            = new Logger(DeploymentHostRecord.class);
    public static final Logger LoggerInstanceForCellular = LoggerInstance.createSubLogger(ForCellular.class);

    //--//

    @Optio3ControlNotifications(reason = "Notify both ways", direct = Optio3ControlNotifications.Notify.ALWAYS, reverse = Optio3ControlNotifications.Notify.ALWAYS, getter = "getCustomerService")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "customer_service", foreignKey = @ForeignKey(name = "CUSTOMER_SERVICE__DEPLOYMENT_HOST__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private CustomerServiceRecord customerService;

    @Optio3UpgradeValue("0")
    @Column(name = "customer_service_roles", nullable = false)
    private long roleIds;

    //--//

    @Field
    @Column(name = "host_id", nullable = false)
    private String hostId;

    @Field
    @Column(name = "host_name")
    private String hostName;

    @Transient
    private String m_hostDisplayName;

    @Field
    @Column(name = "dns_name")
    private String dnsName;

    @Column(name = "warning_threshold", nullable = false)
    private int warningThreshold = 40;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeploymentStatus status;

    @Optio3UpgradeValue("operational")
    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false)
    private DeploymentOperationalStatus operationalStatus = DeploymentOperationalStatus.provisioned;

    @Optio3UpgradeValue("false")
    @Column(name = "has_delayed_ops", nullable = false)
    private boolean hasDelayedOps;

    @Column(name = "last_heartbeat")
    private ZonedDateTime lastHeartbeat;

    //--//

    @Enumerated(EnumType.STRING)
    @Column(name = "architecture", nullable = false)
    private DockerImageArchitecture architecture;

    //--//

    @Lob
    @Column(name = "details")
    private String details;

    @Transient
    private final PersistAsJsonHelper<String, DeploymentHostDetails> m_detailsParser = new PersistAsJsonHelper<>(() -> details,
                                                                                                                 (val) -> details = val,
                                                                                                                 String.class,
                                                                                                                 DeploymentHostDetails.class,
                                                                                                                 ObjectMappers.SkipNulls);

    //--//

    /**
     * List of all the agents running on the deployer.
     */
    @OneToMany(mappedBy = "deployment", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    @OrderBy("instance_id")
    private List<DeploymentAgentRecord> agents;

    /**
     * List of all the tasks running on the deployer.
     */
    @OneToMany(mappedBy = "deployment", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    @OrderBy("sys_created_on DESC")
    private List<DeploymentTaskRecord> tasks;

    /**
     * List of all the tasks running on the deployer.
     */
    @OneToMany(mappedBy = "deployment", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    @OrderBy("sys_created_on DESC")
    private List<DeploymentHostImagePullRecord> imagePulls;

    /**
     * List of all the files uploaded to/downloaded from host.
     */
    @OneToMany(mappedBy = "deployment", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    @OrderBy("sys_created_on DESC")
    private List<DeploymentHostFileRecord> files;

    //--//

    @Column(name = "last_output")
    private ZonedDateTime lastOutput;

    @Column(name = "last_offset", nullable = false)
    private int lastOffset;

    @Lob
    @Column(name = "log_ranges")
    private byte[] logRanges;

    //--//

    public DeploymentHostRecord()
    {
    }

    public static DeploymentHostRecord buildNewHost(String hostId,
                                                    String hostName,
                                                    DockerImageArchitecture architecture,
                                                    String instanceAccount,
                                                    DeploymentInstance instanceType,
                                                    String instanceRegion)

    {
        DeploymentHostRecord rec_host = new DeploymentHostRecord();
        rec_host.setStatus(DeploymentStatus.Initialized);
        rec_host.setArchitecture(architecture);
        rec_host.setHostId(hostId);
        rec_host.setHostName(hostName);

        if (instanceType != null)
        {
            rec_host.putMetadata(WellKnownMetadata.instanceAccount, instanceAccount);
            rec_host.putMetadata(WellKnownMetadata.instanceType, instanceType);

            if (instanceType.deployerClass != null)
            {
                if (instanceType.parseTypedInstanceRegion(instanceRegion) == null)
                {
                    throw Exceptions.newIllegalArgumentException("Invalid region '%s' for '%s'", instanceRegion, instanceType);
                }

                rec_host.putMetadata(WellKnownMetadata.instanceRegion, instanceRegion);
            }
        }

        return rec_host;
    }

    public DeploymentHostFlavor classifyHost(DeploymentHostFlavor defaultFlavor)
    {
        DeploymentHostFlavor hostFlavor = DeploymentHostFlavor.classifyHost(getHostId(), getArchitecture(), getCellularSimIdForSearch() != null);
        if (hostFlavor == null)
        {
            hostFlavor = defaultFlavor;
        }

        return hostFlavor;
    }

    public static String findUniqueName(SessionHolder sessionHolder,
                                        String hostId,
                                        DockerImageArchitecture arch,
                                        boolean hasCellular)
    {
        DeploymentHostFlavor hostFlavor = DeploymentHostFlavor.classifyHost(hostId, arch, hasCellular);
        if (hostFlavor != null)
        {
            return hostFlavor.findUniqueName(sessionHolder, hostId, hasCellular);
        }

        return findUniqueName(sessionHolder, "Unknown");
    }

    public static String findUniqueName(SessionHolder sessionHolder,
                                        String prefix)
    {
        int sequence = findUniqueSequence(sessionHolder, prefix + "_");

        return String.format("%s_%05d", prefix, sequence);
    }

    //--//

    public String getInstanceAccount()
    {
        return getMetadata(WellKnownMetadata.instanceAccount);
    }

    public DeploymentInstance getInstanceType()
    {
        return getMetadata(WellKnownMetadata.instanceType);
    }

    public String getInstanceRegion()
    {
        String instanceRegion = getMetadata(WellKnownMetadata.instanceRegion);
        if (instanceRegion == null)
        {
            DeploymentInstance instanceType = getInstanceType();
            instanceRegion = instanceType.deployerDefaultRegion;
        }

        return instanceRegion;
    }

    public Object getTypedInstanceRegion()
    {
        DeploymentInstance instanceType   = getInstanceType();
        String             instanceRegion = getInstanceRegion();

        return instanceType.parseTypedInstanceRegion(instanceRegion);
    }

    public void setInstanceIp(String publicIp)
    {
        putMetadata(WellKnownMetadata.instanceIp, publicIp);
    }

    public String getInstanceIp()
    {
        return getMetadata(WellKnownMetadata.instanceIp);
    }

    //--//

    public CustomerServiceRecord getCustomerService()
    {
        return customerService;
    }

    public Set<DeploymentRole> getRoles()
    {
        return DeploymentRole.mapFrom(roleIds);
    }

    public boolean hasRole(DeploymentRole role)
    {
        return role != null && role.isActive(roleIds);
    }

    public boolean bindRole(CustomerServiceRecord rec_svc,
                            DeploymentRole role)
    {
        if (role != null)
        {
            if (customerService != null && !SessionHolder.sameEntity(customerService, rec_svc))
            {
                throw Exceptions.newIllegalArgumentException("Can't bind host '%s' to service '%s', already bound to service '%s'",
                                                             getDisplayName(),
                                                             rec_svc.getDisplayName(),
                                                             customerService.getDisplayName());
            }

            long newRoleIDs = role.add(roleIds);
            if (newRoleIDs != roleIds)
            {
                roleIds = newRoleIDs;

                if (customerService == null)
                {
                    customerService = rec_svc;
                }

                return true;
            }
        }

        return false;
    }

    public boolean unbindRole(DeploymentRole role)
    {
        if (role != null)
        {
            long newRoleIDs = role.remove(roleIds);
            if (newRoleIDs != roleIds)
            {
                roleIds = newRoleIDs;

                if (roleIds == 0)
                {
                    customerService = null;
                }

                return true;
            }
        }

        return false;
    }

    public DeploymentRole findRoleCompatibleWithImage(RegistryTaggedImageRecord rec_taggedImage)
    {
        Optional<String> purposeOpt = rec_taggedImage.findLabel(WellKnownDockerImageLabel.Service);
        if (purposeOpt.isPresent())
        {
            String         purposeText = purposeOpt.get();
            DeploymentRole purpose     = DeploymentRole.parse(purposeText);

            if (hasRole(purpose))
            {
                return purpose;
            }
        }

        return null;
    }

    //--//

    public static List<DeploymentHost> getHostsInService(SessionHolder sessionHolder,
                                                         String serviceSysId)
    {
        List<DeploymentHost> hosts = Lists.newArrayList();

        DeploymentHostRecord.streamAllRaw(sessionHolder, (qh) ->
        {
            qh.addWhereReferencing(qh.root, DeploymentHostRecord_.customerService, serviceSysId);
        }, hosts::add);

        return hosts;
    }

    //--//

    public String getHostId()
    {
        return hostId;
    }

    public void setHostId(String hostId)
    {
        if (!StringUtils.equals(this.hostId, hostId))
        {
            this.hostId = hostId;
        }

        m_hostDisplayName = null;
    }

    public String getHostName()
    {
        return hostName;
    }

    public void setHostName(String hostName)
    {
        if (!StringUtils.equals(this.hostName, hostName))
        {
            this.hostName = hostName;
        }

        m_hostDisplayName = null;
    }

    public String getDisplayName()
    {
        if (m_hostDisplayName == null)
        {
            StringBuilder sb = new StringBuilder();

            sb.append(hostId);

            boolean opened = false;

            if (StringUtils.isNotBlank(hostName))
            {
                sb.append(" [");
                sb.append(hostName);
                opened = true;
            }

            String remoteName = getRemoteName();
            if (StringUtils.isNotBlank(remoteName))
            {
                sb.append(opened ? ", " : " [");
                sb.append(remoteName);
                opened = true;
            }

            if (opened)
            {
                sb.append("]");
            }

            m_hostDisplayName = sb.toString();
        }

        return m_hostDisplayName;
    }

    public String prepareEmailSubject(String text)
    {
        if (StringUtils.isNotBlank(hostName))
        {
            return String.format("%s [%s] - %s - %s", hostName, hostId, getOperationalStatus(), text);
        }
        else
        {
            return String.format("%s - %s - %s", hostId, getOperationalStatus(), text);
        }
    }

    public String getDnsName()
    {
        return dnsName;
    }

    public void setDnsName(String dnsName)
    {
        if (!StringUtils.equals(this.dnsName, dnsName))
        {
            this.dnsName = dnsName;
        }
    }

    public void setDnsName(CustomerServiceRecord rec_svc) throws
                                                          MalformedURLException
    {
        String urlText = rec_svc.getUrl();
        if (urlText != null)
        {
            URL url = new URL(urlText);

            setDnsName(url.getHost());
        }
        else
        {
            setDnsName((String) null);
        }
    }

    public int getWarningThreshold()
    {
        return warningThreshold;
    }

    public void setWarningThreshold(int warningThreshold)
    {
        this.warningThreshold = warningThreshold;
    }

    public DeploymentStatus getStatus()
    {
        return status;
    }

    public void setStatus(DeploymentStatus status)
    {
        this.status = status;
    }

    public boolean conditionallyChangeStatus(DeploymentStatus expectedStatus,
                                             DeploymentStatus status)
    {
        if (this.status == expectedStatus)
        {
            this.status = status;
            return true;
        }

        return false;
    }

    public DeploymentOperationalStatus getOperationalStatus()
    {
        return operationalStatus;
    }

    public void setOperationalStatus(DeploymentOperationalStatus status)
    {
        this.operationalStatus = status;

        if (status == DeploymentOperationalStatus.operational && isArm32())
        {
            DeploymentHostProvisioningInfo info = getProvisioningInfo(true);

            if (CollectionUtils.findFirst(info.notes, (note) -> note.deployed) == null)
            {
                DeploymentHostProvisioningNotes note = new DeploymentHostProvisioningNotes();
                note.deployed = true;
                info.addNote(note);

                setProvisioningInfo(info);
            }

            // Start the timer to turn off the WiFi frontend.
            ZonedDateTime timeout = getMetadata(WellKnownMetadata.frontendTimeout);
            if (timeout == null)
            {
                putMetadata(WellKnownMetadata.frontendTimeout, TimeUtils.future(2, TimeUnit.HOURS));
            }
        }
    }

    public boolean conditionallyChangeOperationalStatus(DeploymentOperationalStatus expectedStatus,
                                                        DeploymentOperationalStatus status)
    {
        if (this.operationalStatus == expectedStatus)
        {
            this.operationalStatus = status;
            return true;
        }

        return false;
    }

    public DeploymentOperationalResponsiveness getOperationalResponsiveness()
    {
        return computeOperationalResponsiveness(lastHeartbeat, warningThreshold);
    }

    public static DeploymentOperationalResponsiveness computeOperationalResponsiveness(ZonedDateTime lastHeartbeat,
                                                                                       int warningThreshold)
    {
        ZonedDateTime now = TimeUtils.now();

        if (lastHeartbeat == null || lastHeartbeat.isBefore(now.minus(2 * warningThreshold, ChronoUnit.MINUTES)))
        {
            return DeploymentOperationalResponsiveness.Unresponsive;
        }

        if (lastHeartbeat.isBefore(now.minus(warningThreshold, ChronoUnit.MINUTES)))
        {
            return DeploymentOperationalResponsiveness.UnresponsiveFullThreshold;
        }

        if (lastHeartbeat.isBefore(now.minus(Math.max(40, warningThreshold - 120), ChronoUnit.MINUTES)))
        {
            return DeploymentOperationalResponsiveness.UnresponsiveHalfThreshold;
        }

        return DeploymentOperationalResponsiveness.Responsive;
    }

    //--//

    public DeploymentHostDetails getDetails()
    {
        return getMetadata(WellKnownMetadata.cellularDetails);
    }

    public void setDetails(DeploymentHostDetails details)
    {
        putMetadata(WellKnownMetadata.cellularDetails, details);
    }

    //--//

    public DeployerShutdownConfiguration getBatteryThresholds()
    {
        return getMetadata(WellKnownMetadata.batteryThresholds);
    }

    public void setBatteryThresholds(SessionHolder sessionHolder,
                                     DeployerShutdownConfiguration cfg) throws
                                                                        Exception
    {
        putMetadata(WellKnownMetadata.batteryThresholds, cfg);

        TaskForServiceBatteryThresholds.scheduleTask(sessionHolder, null, this);
    }

    public boolean shouldUpdateTaskThresholds()
    {
        var rec_svc = getCustomerService();
        if (rec_svc == null)
        {
            // Not bound, nothing to do.
            return false;
        }

        var details = getRemoteDetails();

        for (CustomerService.AlertThresholds thresholds : rec_svc.getAlertThresholds())
        {
            if (hasRole(thresholds.role))
            {
                if (details == null)
                {
                    if (findTaskForPurpose(DeploymentStatus.Ready, thresholds.role, null, true) != null)
                    {
                        return true;
                    }
                }
                else
                {
                    if (thresholds.warningThreshold != details.warningThreshold || thresholds.alertThreshold != details.alertThreshold)
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    //--//

    public String computeTargetTag(RegistryTaggedImageRecord image)
    {
        if (isArm32() && image.getReleaseStatus() == RegistryImageReleaseStatus.Release)
        {
            switch (image.getTargetService())
            {
                case deployer:
                    return DeploymentHostImage.DEPLOYER_BOOTSTRAP_ARM;

                case waypoint:
                    return DeploymentHostImage.WAYPOINT_BOOTSTRAP_ARM;

                case provisioner:
                    return DeploymentHostImage.PROVISIONER_BOOTSTRAP_ARM;
            }
        }

        return null;
    }

    public DeploymentHostImage locateCachedImage(String imageTag)
    {
        return CollectionUtils.findFirst(getImages(), (image) -> StringUtils.equals(imageTag, image.tag));
    }

    public List<DeploymentHostImage> getImages()
    {
        List<DeploymentHostImage> images        = getMetadata(WellKnownMetadata.imageDetails);
        AgentStatistics           imagesHistory = getMetadata(WellKnownMetadata.imagesHistory);

        if (imagesHistory != null)
        {
            for (String id : imagesHistory.imageTimestamp.keySet())
            {
                for (DeploymentHostImage image : images)
                {
                    if (StringUtils.equals(image.id, id))
                    {
                        image.lastUsed = imagesHistory.imageTimestamp.get(id);
                    }
                }
            }
        }

        return images;
    }

    public void setImages(List<ImageStatus> images)
    {
        if (images != null)
        {
            List<DeploymentHostImage> res = Lists.newArrayList();
            ZonedDateTime             now = TimeUtils.now();

            for (ImageStatus imageStatus : images)
            {
                if (imageStatus.repoTags == null || imageStatus.repoTags.isEmpty())
                {
                    DeploymentHostImage img = new DeploymentHostImage();
                    img.id            = imageStatus.id;
                    img.size          = imageStatus.size;
                    img.created       = imageStatus.created;
                    img.lastRefreshed = now;

                    res.add(img);
                }
                else
                {
                    for (String repoTag : imageStatus.repoTags)
                    {
                        DeploymentHostImage img = new DeploymentHostImage();
                        img.id            = imageStatus.id;
                        img.tag           = repoTag;
                        img.size          = imageStatus.size;
                        img.created       = imageStatus.created;
                        img.lastRefreshed = now;

                        res.add(img);
                    }
                }
            }

            putMetadata(WellKnownMetadata.imageDetails, res.isEmpty() ? null : res);
        }
    }

    //--//

    public DeploymentHostProvisioningInfo getProvisioningInfo(boolean allocateIfMissing)
    {
        return DeploymentHostProvisioningInfo.sanitize(getMetadata(WellKnownMetadata.provisioningInfo), allocateIfMissing);
    }

    public void setProvisioningInfo(DeploymentHostProvisioningInfo info)
    {
        putMetadata(WellKnownMetadata.provisioningInfo, DeploymentHostProvisioningInfo.sanitize(info, false));
    }

    //--//

    @Field
    public String getCellularEquipmentIdForSearch()
    {
        DeploymentHostDetails details = getDetails();
        if (details != null && details.cellular != null)
        {
            return details.cellular.modemIMEI;
        }

        return null;
    }

    @Field
    public String getCellularSimIdForSearch()
    {
        DeploymentHostDetails details = getDetails();
        if (details != null && details.cellular != null)
        {
            return details.cellular.getModemICCID();
        }

        return null;
    }

    public String getRemoteName()
    {
        DeploymentHostServiceDetails details = getRemoteDetails();
        return details != null ? details.name : null;
    }

    public DeploymentHostServiceDetails getRemoteDetails()
    {
        return getMetadata(DeploymentHostRecord.WellKnownMetadata.remoteDetails);
    }

    public boolean setRemoteDetails(DeploymentHostServiceDetails details)
    {
        return putMetadata(DeploymentHostRecord.WellKnownMetadata.remoteDetails, details);
    }

    //--//

    public DockerImageArchitecture getArchitecture()
    {
        return architecture;
    }

    public boolean isArm32()
    {
        return architecture.isArm32();
    }

    public boolean isArm64()
    {
        return architecture.isArm64();
    }

    public void setArchitecture(DockerImageArchitecture architecture)
    {
        this.architecture = architecture;
    }

    public ZonedDateTime getLastHeartbeat()
    {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(ZonedDateTime lastHeartbeat)
    {
        this.lastHeartbeat = lastHeartbeat;
    }

    public boolean hasHeartbeat()
    {
        for (DeploymentAgentRecord agent : getAgents())
        {
            if (agent.hasHeartbeat())
            {
                return true;
            }
        }

        return false;
    }

    public DeploymentHostOnlineSessions getOnlineSessions()
    {
        DeploymentHostOnlineSessions sessions = getMetadata(WellKnownMetadata.onlineSessions);
        return sessions != null ? sessions : new DeploymentHostOnlineSessions();
    }

    public void updateOnlineOfflineState(SessionHolder sessionHolder,
                                         ILogger logger,
                                         MetadataMap metadata)
    {
        DeploymentHostOnlineSessions sessions = WellKnownMetadata.onlineSessions.get(metadata);
        if (sessions == null)
        {
            sessions = new DeploymentHostOnlineSessions();
        }

        DeploymentHostOnlineSession session = sessions.accessLastSession();

        if (TimeUtils.wasUpdatedRecently(lastHeartbeat, 3, TimeUnit.HOURS))
        {
            if (session == null || session.end != null)
            {
                if (session == null)
                {
                    logger.info("Marking host '%s' as back online at %s", getDisplayName(), TimeUtils.DEFAULT_FORMATTER_NO_MILLI.format(lastHeartbeat));
                }
                else
                {
                    Duration diff = Duration.between(session.end, lastHeartbeat);

                    logger.info("Marking host '%s' as back online at %s (after %s)", getDisplayName(), TimeUtils.DEFAULT_FORMATTER_NO_MILLI.format(lastHeartbeat), diff);
                }

                // Mark the start of the online period.
                session       = new DeploymentHostOnlineSession();
                session.start = lastHeartbeat;

                sessions.entries.add(session);
            }
        }
        else
        {
            if (session != null && session.end == null)
            {
                logger.info("Marking host '%s' as offline from %s", getDisplayName(), TimeUtils.DEFAULT_FORMATTER_NO_MILLI.format(lastHeartbeat));

                // Mark the start of the offline period.
                session.end = lastHeartbeat;
            }

            //
            // Delete Waypoint tasks for hosts that have not been online for one day.
            // They should only run for a few hours anyways.
            //
            if (!TimeUtils.wasUpdatedRecently(lastHeartbeat, 1, TimeUnit.DAYS))
            {
                List<DeploymentTaskRecord> existingTasks = Lists.newArrayList(getTasks());

                for (DeploymentTaskRecord rec_task : existingTasks)
                {
                    if (rec_task.getPurpose() == DeploymentRole.waypoint)
                    {
                        try (ValidationResultsHolder validation = new ValidationResultsHolder(sessionHolder, false, false))
                        {
                            rec_task.remove(validation, sessionHolder.createHelper(DeploymentTaskRecord.class));
                        }
                    }
                }
            }
        }

        sessions.prune(TimeUtils.now()
                                .minus(1, ChronoUnit.YEARS));

        if (sessions.entries.isEmpty())
        {
            sessions = null;
        }

        WellKnownMetadata.onlineSessions.put(metadata, sessions);
    }

    //--//

    public List<DeploymentAgentRecord> getAgents()
    {
        return CollectionUtils.asEmptyCollectionIfNull(agents);
    }

    public List<DeploymentTaskRecord> getTasks()
    {
        return CollectionUtils.asEmptyCollectionIfNull(tasks);
    }

    public List<DeploymentHostImagePullRecord> getImagePulls()
    {
        return CollectionUtils.asEmptyCollectionIfNull(imagePulls);
    }

    public List<DeploymentHostFileRecord> getFiles()
    {
        return CollectionUtils.asEmptyCollectionIfNull(files);
    }

    //--//

    public void prepareHostForCustomer(SessionHolder sessionHolder,
                                       CustomerRecord rec_customer)
    {
        putMetadata(WellKnownMetadata.preparedForCustomer, RecordWithCommonFields.getSysIdSafe(rec_customer));

        renameForCustomer(sessionHolder, rec_customer);
    }

    public void prepareHostForService(SessionHolder sessionHolder,
                                      CustomerServiceRecord rec_svc,
                                      boolean ready)
    {
        putMetadata(WellKnownMetadata.preparedForService, RecordWithCommonFields.getSysIdSafe(rec_svc));

        if (rec_svc != null)
        {
            CustomerRecord rec_customer = rec_svc.getCustomer();
            putMetadata(WellKnownMetadata.preparedForCustomer, RecordWithCommonFields.getSysIdSafe(rec_customer));
        }

        renameForService(sessionHolder, rec_svc, ready ? null : "prep");
    }

    public void renameForCustomer(SessionHolder sessionHolder,
                                  CustomerRecord rec_customer)
    {
        if (rec_customer != null)
        {
            String prefix = String.format("%s_", compactName(rec_customer.getName()));

            int sequence = findUniqueSequence(sessionHolder, prefix);

            setHostName(String.format("%s%05d", prefix, sequence));
        }
    }

    public void renameForService(SessionHolder sessionHolder,
                                 CustomerServiceRecord rec_svc,
                                 String suffix)
    {
        if (rec_svc != null)
        {
            CustomerRecord rec_customer = rec_svc.getCustomer();

            String prefix = String.format("%s__%s_%s", compactName(rec_customer.getName()), compactName(rec_svc.getName()), suffix != null ? "_" + suffix : "");

            int sequence = findUniqueSequence(sessionHolder, prefix);

            setHostName(String.format("%s%05d", prefix, sequence));
        }
    }

    public void renameBasedOnRole(SessionHolder sessionHolder)
    {
        CustomerServiceRecord rec_svc = customerService;
        if (rec_svc != null)
        {
            if (hasRole(DeploymentRole.hub))
            {
                renameForService(sessionHolder, rec_svc, "Hub");
                return;
            }

            if (hasRole(DeploymentRole.database))
            {
                renameForService(sessionHolder, rec_svc, "Database");
                return;
            }

            renameForService(sessionHolder, rec_svc, null);
        }
    }

    private static String compactName(String name)
    {
        name = StringUtils.remove(name, ' ');
        name = StringUtils.remove(name, '.');
        name = StringUtils.remove(name, ',');
        name = StringUtils.remove(name, '_');
        return name;
    }

    //--//

    public static int findUniqueSequence(SessionHolder sessionHolder,
                                         String prefix)
    {
        SequenceNumbers lookup;

        try
        {
            lookup = SystemPreferenceRecord.getTypedValue(sessionHolder, SystemPreferenceTypedValue.HostSequenceNumber, SequenceNumbers.class);
        }
        catch (Exception e)
        {
            lookup = null;
        }

        if (lookup == null)
        {
            lookup = new SequenceNumbers();
        }

        int sequence = lookup.lookupByPrefix.getOrDefault(prefix, 0) + 1;

        DeploymentGlobalDescriptor.Settings settings = new DeploymentGlobalDescriptor.Settings();
        settings.loadDeployments = true;
        DeploymentGlobalDescriptor globalDescriptor = DeploymentGlobalDescriptor.get(sessionHolder, settings);

        int newSequence = AbstractApplicationWithDatabase.findUniqueSequence(prefix, sequence, globalDescriptor.hosts.values(), (host) -> host.hostName);

        lookup.lookupByPrefix.put(prefix, newSequence);

        try
        {
            SystemPreferenceRecord.setTypedValue(sessionHolder, SystemPreferenceTypedValue.HostSequenceNumber, lookup);
        }
        catch (Exception e)
        {
            // Ignore failures.
        }

        return newSequence;
    }

    public DeploymentAgentRecord getActiveAgent()
    {
        for (DeploymentAgentRecord rec_agent : getAgents())
        {
            if (rec_agent.isActive())
            {
                return rec_agent;
            }
        }

        return null;
    }

    //--//

    public BootOptions getBootOptions()
    {
        return getMetadata(WellKnownMetadata.bootOptions);
    }

    public void updateBootOptions(Map<String, String> map)
    {
        BootOptions val = new BootOptions();
        val.lastUpdated = TimeUtils.now();
        val.options     = BootConfig.convertFromPlain(map);

        putMetadata(DeploymentHostRecord.WellKnownMetadata.bootOptions, val);
    }

    //--//

    public DelayedOperations getDelayedOperations(SessionHolder sessionHolder,
                                                  boolean allocateIfMissing)
    {
        MetadataMap metadata = getMetadata();

        DelayedOperations state = WellKnownMetadata.delayedOperations.get(metadata);
        if (state == null)
        {
            if (!allocateIfMissing)
            {
                return null;
            }

            state = new DelayedOperations();
        }

        state.ops.removeIf((op) -> !op.validate(sessionHolder));

        setDelayedOperations(state, metadata);

        return state;
    }

    public boolean hasDelayedOperations()
    {
        return hasDelayedOps;
    }

    public static DelayedOperation.NextAction processDelayedOperations(RecordLocked<DeploymentHostRecord> lock_host) throws
                                                                                                                     Exception
    {
        DeploymentHostRecord rec_host = lock_host.get();

        SessionHolder     sessionHolder = lock_host.getSessionHolder();
        DelayedOperations state         = rec_host.getDelayedOperations(sessionHolder, false);
        if (state != null)
        {
            if (CollectionUtils.findFirst(state.ops, DelayedOperation::mightRequireImagePull) != null)
            {
                //
                // If any of the delayed operations require a download and only the "default" agent is running,
                // we want to launch another one and make it the active one.
                // The reason is that the "default" agent gets killed whenever there's a network issue and we don't want to kill a long download...
                //
                boolean defaultActiveAgent = false;
                boolean otherAgent         = false;

                for (DeploymentAgentRecord rec_agent : rec_host.getAgents())
                {
                    if (rec_agent.getStatus() == DeploymentStatus.Ready)
                    {
                        if (rec_agent.isDefaultAgent())
                        {
                            if (rec_agent.isActive())
                            {
                                defaultActiveAgent = true;
                            }
                        }
                        else
                        {
                            otherAgent = true;
                        }
                    }
                }

                if (defaultActiveAgent && !otherAgent)
                {
                    DeploymentHostFlavor hostFlavor = rec_host.classifyHost(DeploymentHostFlavor.RaspberryPI);
                    if (hostFlavor.shouldCloneDefaultAgent())
                    {
                        if (DelayedAgentCreation.queue(lock_host, DeploymentHostImage.DEPLOYER_BOOTSTRAP_ARM, true))
                        {
                            // Reload operations, since queueing changed them.
                            state = rec_host.getDelayedOperations(sessionHolder, false);
                        }
                    }
                }
            }

            Iterator<DelayedOperation> it = state.ops.iterator();
            while (it.hasNext())
            {
                DelayedOperation op = it.next();
                op.prepare(lock_host);

                DelayedOperation.NextAction nextAction = op.process();
                if (nextAction == null || nextAction instanceof DelayedOperation.NextAction.Done)
                {
                    it.remove();
                }
                else
                {
                    DelayedOperation.NextAction.WaitForActivity waitFor = Reflection.as(nextAction, DelayedOperation.NextAction.WaitForActivity.class);
                    if (waitFor != null)
                    {
                        op.setActivity(waitFor.activity);
                    }

                    rec_host.setDelayedOperations(state);
                    return nextAction;
                }
            }

            rec_host.setDelayedOperations(state);
        }

        return null;
    }

    public DelayedOperations removeDelayedOperation(SessionHolder sessionHolder,
                                                    DelayedOperation op)
    {

        DelayedOperations res = getDelayedOperations(sessionHolder, false);
        if (res != null)
        {
            res.ops.removeIf((op2) ->
                             {
                                 if (!op2.equals(op))
                                 {
                                     return false;
                                 }

                                 BackgroundActivityRecord rec_activity = sessionHolder.fromLocatorOrNull(op2.loc_activity);
                                 if (rec_activity != null)
                                 {
                                     rec_activity.transitionToCancelling();
                                 }

                                 return true;
                             });

            setDelayedOperations(res);
        }

        return res;
    }

    public static boolean queueUnique(RecordLocked<DeploymentHostRecord> lock_target,
                                      DelayedOperation op) throws
                                                           Exception
    {
        SessionHolder        sessionHolder = lock_target.getSessionHolder();
        DeploymentHostRecord rec_host      = lock_target.get();

        DelayedOperations state = rec_host.getDelayedOperations(sessionHolder, true);
        if (state.ops.contains(op))
        {
            return false;
        }

        op.prepare(lock_target);
        op.loggerInstance.info("Creating delayed operation for host '%s'", rec_host.getDisplayName());
        op.createdOn = TimeUtils.now();

        DelayedOperation.IPreprocessState itf = Reflection.as(op, DelayedOperation.IPreprocessState.class);
        if (itf != null)
        {
            itf.preprocessState(sessionHolder, state.ops);
        }

        int insertionPos = 0;
        while (insertionPos < state.ops.size())
        {
            DelayedOperation opExisting = state.ops.get(insertionPos);
            if (opExisting.priority < op.priority)
            {
                break;
            }

            insertionPos++;
        }

        state.ops.add(insertionPos, op);

        rec_host.setDelayedOperations(state);

        TaskForDelayedOperations.scheduleTask(lock_target);

        return true;
    }

    private void setDelayedOperations(DelayedOperations state)
    {
        setDelayedOperations(state, getMetadata());
    }

    private void setDelayedOperations(DelayedOperations state,
                                      MetadataMap metadata)
    {
        if (state == null || state.ops.isEmpty())
        {
            WellKnownMetadata.delayedOperations.remove(metadata);
            setDelayedOperationsFlag(false);
        }
        else
        {
            WellKnownMetadata.delayedOperations.put(metadata, state);
            setDelayedOperationsFlag(true);
        }

        setMetadata(metadata);
    }

    private void setDelayedOperationsFlag(boolean hasDelayedOps)
    {
        if (this.hasDelayedOps != hasDelayedOps)
        {
            this.hasDelayedOps = hasDelayedOps;
        }
    }

    //--//

    public static List<DeploymentAgentUpgradeDescriptor> upgradeAgents(SessionHolder sessionHolder,
                                                                       DeploymentAgentUpgrade upgrade) throws
                                                                                                       Exception
    {
        DeploymentGlobalDescriptor.Settings settings = new DeploymentGlobalDescriptor.Settings();
        settings.loadImages      = true;
        settings.loadDeployments = true;
        settings.loadServices    = true;

        DeploymentGlobalDescriptor globalDescriptor = DeploymentGlobalDescriptor.get(sessionHolder, settings);

        List<DeploymentAgentUpgradeDescriptor> res = Lists.newArrayList();

        if (upgrade.customer != null)
        {
            Customer cust = globalDescriptor.customers.get(upgrade.customer.sysId);
            if (cust != null)
            {
                for (CustomerService service : cust.rawServices)
                {
                    for (DeploymentHost host : service.rawHosts)
                    {
                        res.add(DeploymentAgentUpgradeDescriptor.process(sessionHolder, globalDescriptor, host, upgrade.action));
                    }
                }
            }
        }
        else if (upgrade.service != null)
        {
            CustomerService svc = globalDescriptor.getService(upgrade.service);
            if (svc != null)
            {
                for (DeploymentHost host : svc.rawHosts)
                {
                    res.add(DeploymentAgentUpgradeDescriptor.process(sessionHolder, globalDescriptor, host, upgrade.action));
                }
            }
        }
        else if (upgrade.hosts != null)
        {
            for (TypedRecordIdentity<DeploymentHostRecord> ri_host : upgrade.hosts)
            {
                DeploymentHost host = globalDescriptor.getHost(ri_host);

                res.add(DeploymentAgentUpgradeDescriptor.process(sessionHolder, globalDescriptor, host, upgrade.action));
            }
        }
        else
        {
            for (DeploymentHost host : globalDescriptor.hosts.values())
            {
                res.add(DeploymentAgentUpgradeDescriptor.process(sessionHolder, globalDescriptor, host, upgrade.action));
            }
        }

        return res;
    }

    public static List<DeploymentHostStatusDescriptor> describe(RecordHelper<DeploymentHostRecord> helper,
                                                                boolean includeFullDetails,
                                                                String onlyForServiceSysId)
    {
        DeploymentGlobalDescriptor.Settings settings = new DeploymentGlobalDescriptor.Settings();
        settings.loadImages      = true;
        settings.loadDeployments = true;
        settings.loadServices    = true;

        DeploymentGlobalDescriptor globalDescriptor = DeploymentGlobalDescriptor.get(helper.currentSessionHolder(), settings);

        List<DeploymentHostStatusDescriptor> res = Lists.newArrayList();

        if (onlyForServiceSysId != null)
        {
            CustomerService svc = globalDescriptor.services.get(onlyForServiceSysId);
            if (svc != null)
            {
                for (DeploymentHost host : svc.rawHosts)
                {
                    DeploymentHostStatusDescriptor desc = new DeploymentHostStatusDescriptor(host, includeFullDetails);
                    desc.lookupPreparedFor(globalDescriptor, host);
                    res.add(desc);
                }
            }
        }
        else
        {
            for (DeploymentHost host : globalDescriptor.hosts.values())
            {
                DeploymentHostStatusDescriptor desc = new DeploymentHostStatusDescriptor(host, includeFullDetails);
                desc.lookupPreparedFor(globalDescriptor, host);
                res.add(desc);
            }
        }

        return res;
    }

    public static List<DeploymentHostStatusDescriptor> describeFiltered(RecordHelper<DeploymentHostRecord> helper,
                                                                        DeploymentHostFilterRequest filters)
    {
        if (filters == null)
        {
            SortCriteria sc = new SortCriteria();
            sc.column = "hostId";

            filters        = new DeploymentHostFilterRequest();
            filters.sortBy = Lists.newArrayList();
            filters.sortBy.add(sc);
        }

        List<DeploymentHostStatusDescriptor> res = describe(helper, filters.includeFullDetails, filters.serviceSysid);

        List<AbstractQueryHelperBase.ParsedLike> parsedLikeLst = AbstractQueryHelperBase.ParsedLike.decode(filters.likeFilter);
        if (parsedLikeLst != null)
        {
            for (AbstractQueryHelperBase.ParsedLike parsedLike : parsedLikeLst)
            {
                if (parsedLike.inverted)
                {
                    res = CollectionUtils.filter(res, s -> !s.matchFilter(parsedLike.queryUnescaped));
                }
                else
                {
                    res = CollectionUtils.filter(res, s -> s.matchFilter(parsedLike.queryUnescaped));
                }
            }
        }

        final DeploymentHostFilterStatusPair statusFilter = filters.statusFilter;
        if (statusFilter != null && statusFilter.filter != null)
        {
            switch (statusFilter.filter)
            {
                case all:
                    break;

                case recentlyCreated:
                    res = CollectionUtils.filter(res, s -> TimeUtils.wasUpdatedRecently(s.createdOn, 30, TimeUnit.DAYS));
                    break;

                case delayedOps:
                    res = CollectionUtils.filter(res, s -> s.delayedOps.size() > 0);
                    break;

                case needAttention:
                    res = CollectionUtils.filter(res, s ->
                    {
                        if (!s.instanceType.hasAgent)
                        {
                            return false;
                        }

                        boolean isResponsive       = s.responsiveness == DeploymentOperationalResponsiveness.Responsive;
                        Boolean shouldBeResponsive = s.operationalStatus.shouldBeResponsive();
                        if (shouldBeResponsive == null)
                        {
                            return false;
                        }

                        return isResponsive != shouldBeResponsive;
                    });
                    break;

                case onlyWaypoint:
                    res = CollectionUtils.filter(res, s -> s.hasFlag(DeploymentHostStatusDescriptorFlag.Waypoints));
                    break;

                case onlyBroken:
                    res = CollectionUtils.filter(res, s -> s.operationalStatus.isMalfunctioning());
                    break;

                case stoppedTasks:
                    res = CollectionUtils.filter(res, s -> s.hasFlag(DeploymentHostStatusDescriptorFlag.StoppedTasks));
                    break;

                case nonActiveAgents:
                    res = CollectionUtils.filter(res, s -> s.hasFlag(DeploymentHostStatusDescriptorFlag.NonActiveAgents));
                    break;

                case matchingStatus:
                    res = CollectionUtils.filter(res, s -> s.operationalStatus == statusFilter.target);
                    break;
            }
        }

        if (filters.sortBy != null)
        {
            for (SortCriteria sort : filters.sortBy)
            {
                switch (sort.column)
                {
                    case "hostId":
                        res.sort((a, b) -> StringUtils.compareIgnoreCase(a.hostId, b.hostId));
                        break;

                    case "hostName":
                        res.sort((a, b) -> StringUtils.compareIgnoreCase(a.hostName, b.hostName));
                        break;

                    case "status":
                        res.sort((a, b) ->
                                 {
                                     int diff = StringUtils.compareIgnoreCase(a.status.name(), b.status.name());
                                     if (diff == 0)
                                     {
                                         diff = StringUtils.compareIgnoreCase(a.operationalStatus.name(), b.operationalStatus.name());
                                         if (diff == 0)
                                         {
                                             diff = StringUtils.compareIgnoreCase(a.responsiveness.name(), b.responsiveness.name());
                                             if (diff == 0)
                                             {
                                                 diff = TimeUtils.compare(a.lastHeartbeat, b.lastHeartbeat);
                                             }
                                         }
                                     }
                                     return diff;
                                 });
                        break;

                    case "roleInfo":
                        res.sort((a, b) ->
                                 {
                                     int diff = StringUtils.compareIgnoreCase(a.rolesSummary, b.rolesSummary);
                                     if (diff == 0)
                                     {
                                         diff = StringUtils.compareIgnoreCase(a.hostName, b.hostName);
                                     }
                                     return diff;
                                 });
                        break;

                    case "serviceInfo":
                        res.sort((a, b) ->
                                 {
                                     int diff = StringUtils.compareIgnoreCase(a.serviceName, b.serviceName);
                                     if (diff == 0)
                                     {
                                         diff = StringUtils.compareIgnoreCase(a.hostName, b.hostName);
                                     }
                                     return diff;
                                 });
                        break;

                    case "customerInfo":
                        res.sort((a, b) ->
                                 {
                                     int diff = StringUtils.compareIgnoreCase(a.customerName, b.customerName);
                                     if (diff == 0)
                                     {
                                         diff = StringUtils.compareIgnoreCase(a.serviceName, b.serviceName);
                                         if (diff == 0)
                                         {
                                             diff = StringUtils.compareIgnoreCase(a.hostName, b.hostName);
                                         }
                                     }
                                     return diff;
                                 });
                        break;

                    case "batteryVoltage":
                        res.sort((a, b) -> Float.compare(a.batteryVoltage, b.batteryVoltage));
                        break;

                    case "buildTime":
                        res.sort((a, b) ->
                                 {
                                     int diff = TimeUtils.compare(a.agentBuildTime, b.agentBuildTime);
                                     if (diff == 0)
                                     {
                                         diff = StringUtils.compareIgnoreCase(a.hostName, b.hostName);
                                     }
                                     return diff;
                                 });
                        break;

                    case "createdOn":
                        res.sort((a, b) -> TimeUtils.compare(a.createdOn, b.createdOn));
                        break;

                    case "numDelayedOps":
                        res.sort((a, b) ->
                                 {
                                     int diff = Integer.compare(a.delayedOps.size(), b.delayedOps.size());
                                     if (diff == 0)
                                     {
                                         diff = StringUtils.compareIgnoreCase(a.hostName, b.hostName);
                                     }
                                     return diff;
                                 });
                        break;

                    case "lastHeartbeat":
                        res.sort((a, b) -> TimeUtils.compare(a.lastHeartbeat, b.lastHeartbeat));
                        break;

                    case "instanceType":
                        res.sort(Comparator.comparing(a -> a.instanceType.getDisplayName()));
                        break;

                    case "diskTotal":
                        res.sort(Comparator.comparingLong(a -> a.diskTotal));
                        break;

                    case "diskFree":
                        res.sort(Comparator.comparingLong(a -> a.diskFree));
                        break;

                    case "diskFreePercent":
                        res.sort(Comparator.comparingDouble(a -> (double) a.diskFree / Math.max(a.diskTotal, 1)));
                        break;
                }

                if (!sort.ascending)
                {
                    res = Lists.reverse(res);
                }
            }
        }

        return res;
    }

    public static void streamAllRaw(SessionHolder sessionHolder,
                                    Consumer<RawQueryHelper<DeploymentHostRecord, DeploymentHost>> applyFilters,
                                    Consumer<DeploymentHost> callback)
    {
        RawQueryHelper<DeploymentHostRecord, DeploymentHost> qh = new RawQueryHelper<>(sessionHolder, DeploymentHostRecord.class);

        qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);
        qh.addDate(RecordWithCommonFields_.createdOn, (obj, val) -> obj.createdOn = val);
        qh.addDate(RecordWithCommonFields_.updatedOn, (obj, val) -> obj.updatedOn = val);

        qh.addObject(RecordWithMetadata_.metadataCompressed, byte[].class, (obj, val) -> obj.metadataCompressed = val);

        qh.addReference(DeploymentHostRecord_.customerService, CustomerServiceRecord.class, (obj, val) -> obj.customerService = val);
        qh.addLong(DeploymentHostRecord_.roleIds, (obj, val) -> obj.roles = DeploymentRole.mapFrom(val));

        qh.addString(DeploymentHostRecord_.hostId, (obj, val) -> obj.hostId = val);
        qh.addString(DeploymentHostRecord_.hostName, (obj, val) -> obj.hostName = val);
        qh.addString(DeploymentHostRecord_.dnsName, (obj, val) -> obj.dnsName = val);
        qh.addInteger(DeploymentHostRecord_.warningThreshold, (obj, val) -> obj.warningThreshold = val);
        qh.addEnum(DeploymentHostRecord_.status, DeploymentStatus.class, (obj, val) -> obj.status = val);
        qh.addEnum(DeploymentHostRecord_.operationalStatus, DeploymentOperationalStatus.class, (obj, val) -> obj.operationalStatus = val);
        qh.addEnum(DeploymentHostRecord_.architecture, DockerImageArchitecture.class, (obj, val) -> obj.architecture = val);

        qh.addDate(DeploymentHostRecord_.lastHeartbeat, (obj, val) -> obj.lastHeartbeat = val);
        qh.addBoolean(DeploymentHostRecord_.hasDelayedOps, (obj, val) -> obj.delayedOperations = val);

        qh.addDate(DeploymentHostRecord_.lastOutput, (obj, val) -> obj.lastOutput = val);
        qh.addInteger(DeploymentHostRecord_.lastOffset, (obj, val) -> obj.lastOffset = val);

        if (applyFilters != null)
        {
            applyFilters.accept(qh);
        }

        qh.stream(DeploymentHost::new, callback);
    }

    public static TypedRecordIdentityList<DeploymentHostRecord> list(RecordHelper<DeploymentHostRecord> helper,
                                                                     Boolean withDelayedOps)
    {
        return QueryHelperWithCommonFields.list(helper, (jh) ->
        {
            if (withDelayedOps != null)
            {
                jh.addWhereClauseWithEqual(jh.root.get(DeploymentHostRecord_.hasDelayedOps), withDelayedOps);
            }

            jh.addOrderBy(jh.root, DeploymentHostRecord_.hostId, true);
        });
    }

    public static List<DeploymentHostRecord> getBatch(RecordHelper<DeploymentHostRecord> helper,
                                                      List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    public static TypedRecordIdentity<DeploymentHostRecord> findByHostId(RecordHelper<DeploymentHostRecord> helper,
                                                                         String hostId) throws
                                                                                        NoResultException
    {
        for (DeploymentHostFlavor hostFlavor : DeploymentHostFlavor.values())
        {
            String effectiveHostId = hostFlavor.addPrefix(hostId);

            TypedRecordIdentity<DeploymentHostRecord> ri_host = QueryHelperWithCommonFields.single(helper, (jh) ->
            {
                jh.addWhereClauseWithEqual(jh.root, DeploymentHostRecord_.hostId, effectiveHostId);
            });
            if (ri_host != null)
            {
                return ri_host;
            }
        }

        return null;
    }

    //--//

    public DeploymentAgentRecord findAgent(String id)
    {
        for (DeploymentAgentRecord rec_agent : getAgents())
        {
            if (StringUtils.equals(rec_agent.getInstanceId(), id))
            {
                return rec_agent;
            }
        }

        return null;
    }

    public DeploymentAgentRecord findAgentByDockerId(String id)
    {
        for (DeploymentAgentRecord rec_agent : getAgents())
        {
            if (StringUtils.equals(rec_agent.getDockerId(), id))
            {
                return rec_agent;
            }
        }

        return null;
    }

    public DeploymentAgentRecord findActiveAgent()
    {
        for (DeploymentAgentRecord rec_agent : getAgents())
        {
            if (rec_agent.isActive())
            {
                return rec_agent;
            }
        }

        return null;
    }

    public void activateAgent(DeploymentAgentRecord rec)
    {
        rec.setActive(true);

        for (DeploymentAgentRecord rec_agent : getAgents())
        {
            if (rec_agent.isActive() && rec_agent != rec)
            {
                rec_agent.setActive(false);
            }
        }
    }

    //--//

    public DeploymentTaskRecord findTaskForPurpose(DeploymentStatus desiredState,
                                                   DeploymentRole desiredPurpose,
                                                   RegistryTaggedImageRecord rec_image,
                                                   Boolean withContainer)
    {
        return CollectionUtils.firstElement(findTasksForPurpose(desiredState, desiredPurpose, rec_image, withContainer));
    }

    public List<DeploymentTaskRecord> findTasksForPurpose(DeploymentStatus desiredState,
                                                          DeploymentRole desiredPurpose,
                                                          RegistryTaggedImageRecord rec_image,
                                                          Boolean withContainer)
    {
        List<DeploymentTaskRecord> res = Lists.newArrayList();

        for (DeploymentTaskRecord rec_task : getTasks())
        {
            boolean hasContainer = rec_task.getDockerId() != null;

            if (withContainer != null && withContainer != hasContainer)
            {
                continue;
            }

            if (desiredState != null && rec_task.getStatus() != desiredState)
            {
                continue;
            }

            if (rec_task.getPurpose() != desiredPurpose)
            {
                continue;
            }

            if (rec_image != null && rec_image.getImage() != rec_task.getImageReference())
            {
                continue;
            }

            res.add(rec_task);
        }

        return res;
    }

    //--//

    public RpcConnectionInfo extractConnectionInfo()
    {
        DeploymentAgentRecord rec_agent = findActiveAgent();
        if (rec_agent == null)
        {
            throw Exceptions.newRuntimeException("No Active Agent for '%s'", getDisplayName());
        }

        return rec_agent.extractConnectionInfo();
    }

    public <T> CompletableFuture<T> getProxyOrNull(IServiceProvider serviceProvider,
                                                   Class<T> clz,
                                                   int proxyTimeoutInSeconds)
    {
        try
        {
            T proxy = await(getProxy(serviceProvider, clz, proxyTimeoutInSeconds));
            return wrapAsync(proxy);
        }
        catch (Throwable t)
        {
            return wrapAsync(null);
        }
    }

    @Nonnull
    public <T> CompletableFuture<T> getProxy(IServiceProvider serviceProvider,
                                             Class<T> clz,
                                             int proxyTimeoutInSeconds) throws
                                                                        Exception
    {
        var ci = extractConnectionInfo();

        return ci.getProxy(serviceProvider, clz, proxyTimeoutInSeconds);
    }

    //--//

    public void checkRemoveConditions(ValidationResultsHolder validation)
    {
        for (DeploymentAgentRecord rec_agent : getAgents())
        {
            rec_agent.checkRemoveConditions(validation);
        }

        for (DeploymentTaskRecord rec_task : Lists.newArrayList(getTasks()))
        {
            rec_task.checkRemoveConditions(validation);
        }

        switch (getStatus())
        {
            case Cancelled:
            case BootFailed:
            case Terminated:
                break;

            default:
                if (hasHeartbeat())
                {
                    validation.addFailure("status", "Host '%s' in state '%s'", getHostId(), getStatus());
                }
                break;
        }
    }

    public void remove(ValidationResultsHolder validation,
                       RecordHelper<DeploymentHostRecord> hostHelper) throws
                                                                      Exception
    {
        checkRemoveConditions(validation);

        if (validation.canProceed())
        {
            setOperationalStatus(DeploymentOperationalStatus.retired);
            cleanupState(validation, hostHelper);

            hostHelper.delete(this);
        }
    }

    public void cleanupState(ValidationResultsHolder validation,
                             RecordHelper<DeploymentHostRecord> helper)
    {
        RecordHelper<DeploymentAgentRecord> agentHelper = helper.wrapFor(DeploymentAgentRecord.class);
        RecordHelper<DeploymentTaskRecord>  taskHelper  = helper.wrapFor(DeploymentTaskRecord.class);

        DeploymentOperationalStatus operationalStatus = getOperationalStatus();
        switch (operationalStatus)
        {
            case lostConnectivity:
            case retired:
                for (DeploymentAgentRecord rec_agent : Lists.newArrayList(getAgents()))
                {
                    rec_agent.remove(validation, agentHelper);
                }

                for (DeploymentTaskRecord rec_task : Lists.newArrayList(getTasks()))
                {
                    rec_task.remove(validation, taskHelper);
                }
                break;
        }

        switch (operationalStatus)
        {
            case retired:
                setDelayedOperations(null);

                if (hasRole(DeploymentRole.gateway))
                {
                    for (var role : getRoles())
                    {
                        unbindRole(role);
                    }
                }

                DeploymentHostDetails details = getDetails();
                if (details != null && details.provider != null)
                {
                    try
                    {
                        BuilderConfiguration cfg = validation.getServiceNonNull(BuilderConfiguration.class);

                        ICellularProviderHandler handler = details.provider.getHandler(cfg.credentials);
                        if (handler != null)
                        {
                            ICellularProviderHandler.SimInfo si = handler.lookupSim(details.providerId);
                            if (si == null)
                            {
                                si = handler.lookupSim(details.cellular.getModemICCID());
                            }
                            if (si != null)
                            {
                                si.status = ICellularProviderHandler.Status.DEACTIVATED;
                                handler.updateStatus(si, null);
                                LoggerInstanceForCellular.info("Deactivated %s SIM '%s' for host '%s'", details.provider, details.providerId, hostId);
                            }
                        }
                    }
                    catch (Throwable t)
                    {
                        LoggerInstanceForCellular.error("Failed to deactivate %s SIM for host '%s', due to %s", details.provider, hostId);
                    }
                }
                break;
        }
    }

    public boolean canStartNewAgent(RegistryImageRecord rec_targetImage)
    {
        int count = 0;

        for (DeploymentTaskRecord task : getTasks())
        {
            if (task.getImageReference() == rec_targetImage && task.getDockerId() != null)
            {
                count++;
            }
        }

        int limit;

        if (isArm32())
        {
            limit = 1;
        }
        else
        {
            // Allow pairs of agents on Intel.
            limit = 2;
        }

        if (getOperationalStatus() == DeploymentOperationalStatus.maintenance)
        {
            // Allow an extra agent if we are in maintenance mode.
            limit++;
        }

        return count < limit;
    }

    public void terminate(ValidationResultsHolder validation) throws
                                                              Exception
    {
        DeploymentInstance instanceType = BoxingUtils.get(getInstanceType(), DeploymentInstance.Edge);
        if (!instanceType.canTerminate)
        {
            validation.addFailure("architecture", "%s cannot be terminated", instanceType);
        }

        if (validation.canProceed())
        {
            RecordLocked<DeploymentHostRecord> lock_host = validation.sessionHolder.optimisticallyUpgradeToLocked(this, 2, TimeUnit.MINUTES);
            TaskForHostTermination.scheduleTask(lock_host, Duration.of(2, ChronoUnit.HOURS));
        }
    }

    //--//

    public DeploymentCellularCharges getCharges()
    {
        DeploymentCellularCharges charges = getMetadata(WellKnownMetadata.cellularCharges);
        if (charges != null)
        {
            var details = getDetails();
            if (details != null && details.provider != null)
            {
                switch (details.provider)
                {
                    case Pelion:
                        switch (BoxingUtils.get(details.providerPlan, "Pelion7_001_50MB_12"))
                        {
                            case "Pelion7_001_50MB_12":
                                charges.monthlyFees = 3.14;
                                charges.monthlyQuotaIncludedInFees = 50 * 1024 * 1024;
                                charges.extraCostPerMB = 0.07;
                                break;
                        }
                        break;

                    case Twilio:
                        switch (BoxingUtils.get(details.providerPlan, "5CentsPerMB"))
                        {
                            case "5CentsPerMB":
                                charges.monthlyFees = 2;
                                charges.monthlyQuotaIncludedInFees = 0;
                                charges.extraCostPerMB = 0.05;
                                break;
                        }
                        break;
                }
            }
        }

        return charges;
    }

    public void setCharges(DeploymentCellularCharges charges)
    {
        putMetadata(WellKnownMetadata.cellularCharges, charges);
    }

    public void refreshCharges(BuilderConfiguration cfg,
                               ZonedDateTime now,
                               int refreshPeriodInHours)
    {
        DeploymentHostDetails details = getDetails();
        if (details != null && details.provider != null)
        {
            boolean writeBack = false;

            ZonedDateTime thresholdForRefresh = now.minusHours(refreshPeriodInHours);

            DeploymentCellularCharges charges = getCharges();
            if (charges == null)
            {
                charges   = new DeploymentCellularCharges();
                writeBack = true;
            }

            if (TimeUtils.isBeforeOrNull(charges.lastRefresh, thresholdForRefresh))
            {
                try
                {
                    ZonedDateTime start;
                    ZonedDateTime oldest = now.minus(30, ChronoUnit.DAYS);

                    DeploymentCellularCharge lastPeriod = CollectionUtils.lastElement(charges.charges);
                    if (lastPeriod == null)
                    {
                        start = oldest;
                    }
                    else
                    {
                        start = TimeUtils.max(oldest, lastPeriod.timestamp.minus(24, ChronoUnit.HOURS)); // Allow some overlap, for late billing arrivals.
                    }

                    ICellularProviderHandler handler = details.provider.getHandler(cfg.credentials);
                    if (handler != null)
                    {
                        for (int retry = 0; retry < 3; retry++)
                        {
                            try
                            {
                                for (ICellularProviderHandler.SimCharges charge : handler.getCharges(details.providerId, start, null))
                                {
                                    DeploymentCellularCharge val = charges.ensureTimestamp(charge.timestamp);
                                    val.upload   = charge.upload;
                                    val.download = charge.download;
                                    val.total    = charge.total;
                                    val.billed   = charge.billed;
                                }
                                break;
                            }
                            catch (ProcessingException e)
                            {
                                Throwable e2 = e.getCause();
                                if (e2 instanceof SocketTimeoutException || e2 instanceof TimeoutException)
                                {
                                    continue;
                                }

                                throw e;
                            }
                        }
                    }
                }
                catch (ClientErrorException e1)
                {
                    LoggerInstanceForCellular.warn("Failed to refresh charges for %s, due to %s", getDisplayName(), e1.getMessage());
                }
                catch (Throwable t)
                {
                    LoggerInstanceForCellular.warn("Failed to refresh charges for %s, due to %s", getDisplayName(), t);
                }

                // Keep twelve months of samples.
                ZonedDateTime oldestPeriodToKeep = now.minus(365, ChronoUnit.DAYS);
                charges.cleanup(oldestPeriodToKeep);

                charges.lastRefresh = now;
                writeBack           = true;
            }

            if (writeBack)
            {
                setCharges(charges);
            }
        }
    }

    public DeploymentCellularStatus getCellularConnectionStatus(BuilderConfiguration cfg)
    {
        DeploymentHostDetails details = getDetails();
        if (details != null)
        {
            try
            {
                ICellularProviderHandler handler = details.provider.getHandler(cfg.credentials);
                if (handler != null)
                {
                    ICellularProviderHandler.SimConnectionStatus status = handler.getConnectionStatus(details.providerId);
                    if (status != null)
                    {
                        DeploymentCellularStatus res = new DeploymentCellularStatus();
                        res.isOnline       = status.isOnline;
                        res.isTransferring = status.isTransferring;
                        return res;
                    }
                }
            }
            catch (Throwable t)
            {
                LoggerInstanceForCellular.debug("Failed to fetch connection status for %s, due to %s", getDisplayName(), t.getMessage());
            }
        }

        return null;
    }

    public DeploymentCellularSessions getCellularSessions(BuilderConfiguration cfg)
    {
        DeploymentHostDetails details = getDetails();
        if (details != null)
        {
            try
            {
                final DeploymentCellularSessions sessions = new DeploymentCellularSessions();

                ICellularProviderHandler handler = details.provider.getHandler(cfg.credentials);
                if (handler != null)
                {
                    for (ICellularProviderHandler.SimDataSession result : handler.getDataSessions(details.providerId, null, null))
                    {
                        DeploymentCellularSession val = sessions.ensureTimestamp(result.start);
                        if (val != null)
                        {
                            val.end         = result.end;
                            val.lastUpdated = result.lastUpdated;

                            val.packetsDownloaded = result.packetsDownloaded;
                            val.packetsUploaded   = result.packetsUploaded;

                            val.cellId             = result.cellId;
                            val.operator           = result.operator;
                            val.operatorCountry    = result.operatorCountry;
                            val.radioLink          = result.radioLink;
                            val.estimatedLatitude  = result.estimatedLatitude;
                            val.estimatedLongitude = result.estimatedLongitude;
                        }
                    }
                }

                return sessions;
            }
            catch (Throwable t)
            {
                LoggerInstanceForCellular.debug("Failed to fetch data sessions for %s, due to %s", getDisplayName(), t.getMessage());
            }
        }

        return null;
    }

    public DeploymentCellularCommunications getCellularCommunications(BuilderConfiguration cfg,
                                                                      Function<String, String> ipResolver,
                                                                      int days)
    {
        DeploymentHostDetails details = getDetails();
        if (details != null)
        {
            try
            {
                final DeploymentCellularCommunications exchanges = new DeploymentCellularCommunications();

                ICellularProviderHandler handler = details.provider.getHandler(cfg.credentials);
                if (handler != null)
                {
                    Semaphore rateLimiter = Executors.allocateSemaphore(2);

                    List<ICellularProviderHandler.SimDataExchange> results = handler.getDataExchanges(rateLimiter, details.providerId, days);

                    Set<String> addresses = Sets.newHashSet();

                    for (ICellularProviderHandler.SimDataExchange result : results)
                    {
                        addresses.add(result.ip);
                    }

                    CollectionUtils.transformInParallel(addresses, rateLimiter, (address) ->
                    {
                        int[] bytesPerDay = new int[days];

                        for (ICellularProviderHandler.SimDataExchange result : results)
                        {
                            if (StringUtils.equals(result.ip, address))
                            {
                                bytesPerDay[result.daysAgo] = result.bytes;
                            }
                        }

                        exchanges.addEntries(ipResolver.apply(address), bytesPerDay);
                        return null;
                    });
                }

                return exchanges;
            }
            catch (Throwable t)
            {
                LoggerInstanceForCellular.debug("Failed to fetch data exchanges for %s, due to %s", getDisplayName(), t.getMessage());
            }
        }

        return null;
    }

    public boolean tryLinkingToCellular(BuilderConfiguration cfg,
                                        String iccid,
                                        String imsi,
                                        String productId)
    {
        // Link host to cellular network, if possible.
        if (CellularProvider.isValidICCID(iccid))
        {
            LoggerInstanceForCellular.debug("tryLinkingToCellular: ICCID=%s IMSI=%s", iccid, imsi);

            for (CellularProvider provider : CellularProvider.values())
            {
                if (provider.mightBeCompatible(imsi))
                {
                    LoggerInstanceForCellular.debug("tryLinkingToCellular: checking provider '%s'...", provider);

                    ICellularProviderHandler handler = provider.getHandler(cfg.credentials);
                    if (handler != null)
                    {
                        try
                        {
                            ICellularProviderHandler.SimInfo si = handler.lookupSim(iccid);
                            if (si != null)
                            {
                                linkToCellular(provider, si);

                                if (si.status == ICellularProviderHandler.Status.NEW)
                                {
                                    si.status = ICellularProviderHandler.Status.READY;
                                }

                                handler.updateStatus(si, productId);
                                return true;
                            }
                        }
                        catch (Throwable t)
                        {
                            LoggerInstanceForCellular.error("Failed to link host '%s' to SIM '%s', due to %s", getHostId(), iccid, t);
                        }
                    }
                }
            }
        }

        return false;
    }

    public void linkToCellular(CellularProvider provider,
                               ICellularProviderHandler.SimInfo si)
    {
        DeploymentHostDetails details = getDetails();
        if (details == null)
        {
            details = new DeploymentHostDetails();
        }

        details.provider     = provider;
        details.providerId   = si.id;
        details.providerPlan = si.plan;

        var cellular = new DeployerCellularInfo();
        cellular.setModemICCID(si.iccid);

        details.update(cellular);

        LoggerInstanceForCellular.info("Associating host '%s' with '%s' (account %s/%s)", hostName, si.iccid, provider.name(), si.id);
        setDetails(details);
    }

    //--//

    public static InputStream generateUnitsReport(IServiceProvider serviceProvider,
                                                  DeploymentHostFilterRequest filters) throws
                                                                                       IOException
    {
        try (var holder = new TabularReportAsExcel.Holder())
        {
            TabularReportAsExcel<RowForReport> tr     = new TabularReportAsExcel<>(RowForReport.class, "Units", holder);
            ZoneId                             zoneId = ZoneId.of("America/Los_Angeles");

            tr.emit(rowHandler ->
                    {
                        try (SessionHolder sessionHolder = SessionHolder.createWithNewReadOnlySession(serviceProvider, null, Optio3DbRateLimiter.Normal))
                        {
                            RecordHelper<DeploymentHostRecord> helper_host = sessionHolder.createHelper(DeploymentHostRecord.class);

                            for (DeploymentHostStatusDescriptor desc : describeFiltered(helper_host, filters))
                            {
                                if (desc.architecture.isIntel())
                                {
                                    continue;
                                }

                                RowForReport row = new RowForReport();

                                row.col_hostId     = desc.hostId;
                                row.col_hostName   = desc.hostName;
                                row.col_remoteName = desc.remoteName;
                                row.col_state      = desc.operationalStatus;

                                row.col_customer = desc.customerName;
                                row.col_service  = desc.serviceName;
                                row.col_vertical = desc.serviceVertical;

                                if (row.col_customer == null)
                                {
                                    if (desc.preparedForCustomer != null)
                                    {
                                        row.col_customer = desc.preparedForCustomer + " (prepared)";
                                    }
                                }

                                if (row.col_service == null && desc.preparedForService != null)
                                {
                                    row.col_service = desc.preparedForService + " (prepared)";
                                }

                                DeploymentHostDetails hostDetails = desc.hostDetails;
                                if (hostDetails != null)
                                {
                                    row.col_cellularProvider = hostDetails.provider;

                                    DeployerCellularInfo cellular = hostDetails.cellular;
                                    if (cellular != null)
                                    {
                                        row.col_imsi  = cellular.modemIMSI;
                                        row.col_imei  = cellular.modemIMEI;
                                        row.col_iccid = cellular.getModemICCID();
                                    }
                                }

                                row.col_lastHeartbeat = desc.lastHeartbeat;

                                row.col_manufacturingDate = desc.createdOn;

                                DeploymentHostProvisioningInfo info = desc.provisioningInfo;
                                if (info != null)
                                {
                                    ProvisionReport provisionReport = info.manufacturingInfo;
                                    if (provisionReport != null)
                                    {
                                        TypedRecordIdentity<DeploymentHostRecord> ri_host2 = findByHostId(helper_host, provisionReport.manufacturingLocation);
                                        if (ri_host2 != null)
                                        {
                                            DeploymentHostRecord rec_host2 = sessionHolder.getEntityOrNull(DeploymentHostRecord.class, ri_host2.sysId);
                                            if (rec_host2 != null)
                                            {
                                                row.col_manufacturingSite = rec_host2.getHostName();
                                            }
                                        }

                                        if (provisionReport.timestamp != null)
                                        {
                                            row.col_manufacturingDate = provisionReport.timestamp;
                                        }
                                    }

                                    for (DeploymentHostProvisioningNotes note : info.notes)
                                    {
                                        if (note.readyForShipping)
                                        {
                                            row.col_shipping = note.timestamp;
                                        }

                                        if (note.deployed)
                                        {
                                            row.col_deployed = note.timestamp;
                                        }
                                    }
                                }

                                switch (desc.operationalStatus)
                                {
                                    case offline:
                                    case operational:
                                    case maintenance:
                                    case lostConnectivity:
                                    case storageCorruption:
                                        if (row.col_shipping == null)
                                        {
                                            row.col_shipping = row.col_manufacturingDate;
                                        }

                                        if (row.col_deployed == null)
                                        {
                                            row.col_deployed = row.col_shipping;
                                        }
                                        break;
                                }

                                ZonedDateTime billing = null;

                                if (row.col_shipping != null)
                                {
                                    billing = row.col_shipping;
                                }

                                if (row.col_deployed != null)
                                {
                                    billing = TimeUtils.updateIfBefore(billing, row.col_deployed);
                                }

                                if (billing != null)
                                {
                                    row.col_billing = TimeUtils.truncateToMonths(billing.plus(30, ChronoUnit.DAYS)
                                                                                        .withZoneSameInstant(zoneId));
                                }

                                rowHandler.emitRow(row);
                            }
                        }
                    });

            return holder.asStream();
        }
    }

    //--//

    public ZonedDateTime getLastOutput()
    {
        return lastOutput;
    }

    public int getLastOffset()
    {
        return lastOffset;
    }

    @Override
    public byte[] getLogRanges()
    {
        return logRanges;
    }

    @Override
    public void setLogRanges(byte[] logRanges,
                             ZonedDateTime lastOutput,
                             int lastOffset)
    {
        if (!Arrays.equals(this.logRanges, logRanges))
        {
            this.logRanges  = logRanges;
            this.lastOutput = lastOutput;
            this.lastOffset = lastOffset;
        }
    }

    @Override
    public void refineLogQuery(LogHandler.JoinHelper<?, DeploymentHostLogRecord> jh)
    {
        jh.addWhereClauseWithEqual(jh.rootLog, DeploymentHostLogRecord_.owningHost, this);
    }

    @Override
    public DeploymentHostLogRecord allocateNewLogInstance()
    {
        return DeploymentHostLogRecord.newInstance(this);
    }

    public static LogHandler<DeploymentHostRecord, DeploymentHostLogRecord> allocateLogHandler(RecordLocked<DeploymentHostRecord> lock)
    {
        return new LogHandler<>(lock, DeploymentHostLogRecord.class);
    }

    public static LogHandler<DeploymentHostRecord, DeploymentHostLogRecord> allocateLogHandler(SessionHolder sessionHolder,
                                                                                               DeploymentHostRecord rec)
    {
        return new LogHandler<>(sessionHolder, rec, DeploymentHostLogRecord.class);
    }
}
