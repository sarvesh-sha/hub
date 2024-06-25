/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.customer;

import static java.util.Objects.requireNonNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.annotation.Optio3UpgradeValue;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.logic.deploy.BaseDeployLogic;
import com.optio3.cloud.builder.model.admin.HubHeapAndThreads;
import com.optio3.cloud.builder.model.customer.CustomerService;
import com.optio3.cloud.builder.model.customer.CustomerServiceDesiredState;
import com.optio3.cloud.builder.model.customer.CustomerServiceDesiredStateRole;
import com.optio3.cloud.builder.model.customer.CustomerServiceUpgradeBlocker;
import com.optio3.cloud.builder.model.customer.CustomerServiceUpgradeBlockers;
import com.optio3.cloud.builder.model.customer.CustomerVertical;
import com.optio3.cloud.builder.model.customer.RoleAndArchitectureWithImage;
import com.optio3.cloud.builder.model.deployment.DeploymentCellularCharges;
import com.optio3.cloud.builder.model.deployment.DeploymentGlobalDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentHost;
import com.optio3.cloud.builder.model.deployment.DeploymentOperationalStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.orchestration.tasks.bookkeeping.TaskForAlertThresholds;
import com.optio3.cloud.builder.orchestration.tasks.bookkeeping.TaskForServiceBatteryThresholds;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentInstance;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForDesiredState;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForHubAccountsRefresh;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForHubCheckUsages;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForHubCompactTimeSeries;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForHubSecretsRefresh;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.config.UserRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.client.deployer.model.DeployerShutdownConfiguration;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.logic.BackgroundActivityStatus;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.EncryptedPayload;
import com.optio3.cloud.persistence.LogHandler;
import com.optio3.cloud.persistence.MetadataField;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RawQueryHelper;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.RecordWithMetadata;
import com.optio3.cloud.persistence.RecordWithMetadata_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.search.Optio3QueryAnalyzerOverride;
import com.optio3.infra.deploy.CommonDeployer;
import com.optio3.logging.ILogger;
import com.optio3.logging.RedirectingLogger;
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
@Table(name = "CUSTOMER_SERVICE")
@DynamicUpdate // Due to HHH-11506
@Indexed
@Analyzer(definition = "fuzzy")
@Optio3QueryAnalyzerOverride("fuzzy_query")
@Optio3TableInfo(externalId = "CustomerService", model = CustomerService.class, metamodel = CustomerServiceRecord_.class, metadata = CustomerServiceRecord.WellKnownMetadata.class)
public class CustomerServiceRecord extends RecordWithMetadata implements LogHandler.ILogHost<CustomerServiceLogRecord>,
                                                                         ModelMapperTarget<CustomerService, CustomerServiceRecord_>
{
    public static class WellKnownMetadata implements Optio3TableInfo.IMetadataDigest
    {
        private static final TypeReference<Set<DeploymentRole>> s_typeRef_Purposes = new TypeReference<>()
        {
        };

        private static final TypeReference<List<RoleAndArchitectureWithImage>> s_typeRef_listOfRoleImages = new TypeReference<>()
        {
        };

        private static final TypeReference<List<HubHeapAndThreads>> s_typeRef_HeapStatus = new TypeReference<>()
        {
        };

        //--//

        public static final MetadataField<Set<DeploymentRole>> purposes = new MetadataField<>("purposes", s_typeRef_Purposes, Sets::newHashSet);

        public static final MetadataField<List<RoleAndArchitectureWithImage>> role_images = new MetadataField<>("role_images", s_typeRef_listOfRoleImages, Lists::newArrayList);
        public static final MetadataField<String>                             role_origin = new MetadataField<>("role_origin", String.class);

        public static final MetadataField<Boolean> disableServiceWorker = new MetadataField<>("disableServiceWorker", Boolean.class, () -> false);
        public static final MetadataField<Boolean> disableEmails        = new MetadataField<>("disableEmails", Boolean.class, () -> false);
        public static final MetadataField<Boolean> disableTexts         = new MetadataField<>("disableTexts", Boolean.class, () -> false);
        public static final MetadataField<Boolean> useTestReporter      = new MetadataField<>("useTestReporter", Boolean.class, () -> false);

        //--//

        public static final MetadataField<ZonedDateTime> backupFailureHourly = new MetadataField<>("backupFailureHourly", ZonedDateTime.class);
        public static final MetadataField<ZonedDateTime> backupFailureDaily  = new MetadataField<>("backupFailureDaily", ZonedDateTime.class);

        public static final MetadataField<Boolean> emailFailureHourly = new MetadataField<>("emailFailureHourly", Boolean.class);
        public static final MetadataField<Boolean> emailFailureDaily  = new MetadataField<>("emailFailureDaily", Boolean.class);

        public static final MetadataField<ZonedDateTime>           hubFailure         = new MetadataField<>("hubFailure", ZonedDateTime.class);
        public static final MetadataField<Boolean>                 hubFailureNotified = new MetadataField<>("hubFailureNotified", Boolean.class);
        public static final MetadataField<List<HubHeapAndThreads>> hubHeapStatus      = new MetadataField<>("hubHeapStatus", s_typeRef_HeapStatus, Lists::newArrayList);

        public static final MetadataField<ZonedDateTime> certificateFailure         = new MetadataField<>("certificateFailure", ZonedDateTime.class);
        public static final MetadataField<Boolean>       certificateFailureNotified = new MetadataField<>("certificateFailureNotified", Boolean.class);

        //--//

        public static final MetadataField<ZonedDateTime> backupDelay = new MetadataField<>("backupDelay", ZonedDateTime.class);

        public static final MetadataField<Boolean> relaunchAlways = new MetadataField<>("relaunchAlways", Boolean.class);

        public static final MetadataField<CustomerServiceUpgradeBlockers> upgradeBlockers = new MetadataField<>("upgradeBlockers", CustomerServiceUpgradeBlockers.class);

        public static final MetadataField<List<CustomerService.AlertThresholds>> alertThresholds = new MetadataField<>("alertThresholds",
                                                                                                                       CustomerService.AlertThresholds.s_typeRef,
                                                                                                                       Lists::newArrayList);

        public static final MetadataField<DeployerShutdownConfiguration> batteryThresholds = new MetadataField<>("batteryThresholds", DeployerShutdownConfiguration.class);

        public static final MetadataField<Map<String, RestartHistory>> taskRestartHistory = new MetadataField<>("taskRestartHistory", RestartHistory.s_typeRef, Maps::newHashMap);
    }

    public static class RestartHistory
    {
        private static final TypeReference<Map<String, RestartHistory>> s_typeRef = new TypeReference<>()
        {
        };

        public ZonedDateTime lastAttempt;

        public boolean shouldProceed()
        {
            if (!TimeUtils.wasUpdatedRecently(lastAttempt, 1, TimeUnit.HOURS))
            {
                lastAttempt = TimeUtils.now();
                return true;
            }

            return false;
        }

        public boolean shouldRemoveStale()
        {
            return !TimeUtils.wasUpdatedRecently(lastAttempt, 1, TimeUnit.DAYS);
        }
    }

    public interface LoggerContext
    {
        RecordLocator<CustomerServiceRecord> getServiceLocator();
    }

    public static ILogger buildContextualLogger(ILogger inner,
                                                RecordLocator<CustomerServiceRecord> loc)
    {
        if (loc == null)
        {
            return inner;
        }

        class SubLogger extends RedirectingLogger implements CustomerServiceRecord.LoggerContext
        {
            public SubLogger(ILogger outerLoggerInstance)
            {
                super(outerLoggerInstance);
            }

            @Override
            public RecordLocator<CustomerServiceRecord> getServiceLocator()
            {
                return loc;
            }
        }

        return new SubLogger(inner);
    }

    //--//

    /**
     * The context of this record.
     */
    @Optio3ControlNotifications(reason = "Only notify customer", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getCustomer")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "customer", nullable = false, foreignKey = @ForeignKey(name = "CUSTOMER__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private CustomerRecord customer;

    //--//

    @Field
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * The URL this service is published at.
     */
    @Field
    @Column(name = "url", nullable = false)
    private String url;

    @Optio3UpgradeValue("operational")
    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false)
    private DeploymentOperationalStatus operationalStatus = DeploymentOperationalStatus.operational;

    /**
     * The account used to access this instance.
     */
    @Column(name = "instance_account")
    private String instanceAccount;

    /**
     * The type of compute node to instantiate.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "instance_type")
    private DeploymentInstance instanceType;

    /**
     * Where to instantiate a compute node.
     */
    @Column(name = "instance_region")
    private String instanceRegion;

    @Enumerated(EnumType.STRING)
    @Column(name = "vertical")
    private CustomerVertical vertical;

    /**
     * The type of compute node to instantiate.
     */
    @Column(name = "disk_size")
    private Integer diskSize;

    /**
     * If true, demo data will be loaded as boot time.
     */
    @Column(name = "use_demo_data", nullable = false)
    private boolean useDemoData;

    @Lob
    @Column(name = "extra_config_lines")
    private String extraConfigLines;

    @Lob
    @Column(name = "extra_config_lines_active")
    private String extraConfigLinesActive;

    //--//

    /**
     * If the service has to store credentials in the database, this should be the key used to encrypt/decrypt them.
     */
    @Embedded
    private EncryptedPayload masterKey;

    @Embedded
    private EmbeddedDatabaseConfiguration dbConfiguration;

    /**
     * When the service is initialized, this is the password for the maint user.
     */
    @Embedded
    private EncryptedPayload maintPassword;

    //--//

    @Optio3ControlNotifications(reason = "Notify service of activity changes", direct = Notify.NEVER, reverse = Notify.ALWAYS)
    @Optio3Cascade(mode = Optio3Cascade.Flavor.CLEAR, getter = "getCurrentActivity", setter = "setCurrentActivity")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "current_activity", foreignKey = @ForeignKey(name = "CURRENT_ACTIVITY__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private BackgroundActivityRecord currentActivity;

    /**
     * List of all the various backups belonging to this customer's service.
     */
    @OneToMany(mappedBy = "customerService", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    @OrderBy("fileId DESC")
    private List<CustomerServiceBackupRecord> backups;

    /**
     * List of all the various secrets belonging to this customer.
     */
    @OneToMany(mappedBy = "service", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    @OrderBy("context")
    private List<CustomerServiceSecretRecord> secrets;

    //--//

    @Column(name = "last_output")
    private ZonedDateTime lastOutput;

    @Column(name = "last_offset", nullable = false)
    private int lastOffset;

    @Lob
    @Column(name = "log_ranges")
    private byte[] logRanges;

    //--//

    public CustomerServiceRecord()
    {
    }

    public static CustomerServiceRecord newInstance(CustomerRecord customer)
    {
        requireNonNull(customer);

        CustomerServiceRecord res = new CustomerServiceRecord();
        res.customer = customer;
        return res;
    }

    //--//

    public CustomerRecord getCustomer()
    {
        return customer;
    }

    public String getDisplayName()
    {
        return String.format("%s / %s", getCustomer().getName(), getName());
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public DeploymentOperationalStatus getOperationalStatus()
    {
        return operationalStatus;
    }

    public void setOperationalStatus(DeploymentOperationalStatus status)
    {
        this.operationalStatus = status;
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

    //--//

    public String getInstanceAccount()
    {
        return instanceAccount;
    }

    public void setInstanceAccount(String instanceAccount)
    {
        this.instanceAccount = instanceAccount;
    }

    public DeploymentInstance getInstanceType()
    {
        return instanceType != null ? instanceType : DeploymentInstance.Edge;
    }

    public void setInstanceType(DeploymentInstance instanceType)
    {
        this.instanceType = instanceType;
    }

    public String getInstanceRegion()
    {
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

    public void setInstanceRegion(String instanceRegion)
    {
        DeploymentInstance instanceType = getInstanceType();

        if (instanceType.isDeployable && instanceType.deployerClass != null && instanceType.parseTypedInstanceRegion(instanceRegion) == null)
        {
            throw Exceptions.newIllegalArgumentException("Invalid region '%s' for '%s'", instanceRegion, getInstanceType());
        }

        this.instanceRegion = instanceRegion;
    }

    public CustomerVertical getVertical()
    {
        return vertical != null ? vertical : CustomerVertical.CRE;
    }

    public void setVertical(CustomerVertical vertical)
    {
        this.vertical = vertical;
    }

    public Integer getDiskSize()
    {
        return diskSize;
    }

    public void setDiskSize(Integer diskSize)
    {
        this.diskSize = diskSize;
    }

    public Set<DeploymentRole> getPurposes()
    {
        return getMetadata(WellKnownMetadata.purposes);
    }

    public void setPurposes(Set<DeploymentRole> purposes)
    {
        putMetadata(WellKnownMetadata.purposes, purposes);
    }

    public List<RoleAndArchitectureWithImage> getRoleImages()
    {
        return getMetadata(WellKnownMetadata.role_images);
    }

    public boolean setRoleImages(List<RoleAndArchitectureWithImage> lst)
    {
        return putMetadata(WellKnownMetadata.role_images, lst);
    }

    public String getRoleOrigin()
    {
        return getMetadata(WellKnownMetadata.role_origin);
    }

    public boolean setRoleOrigin(String origin)
    {
        return putMetadata(WellKnownMetadata.role_origin, origin);
    }

    public boolean getUseDemoData()
    {
        return useDemoData;
    }

    public void setUseDemoData(boolean useDemoData)
    {
        this.useDemoData = useDemoData;
    }

    public boolean getRelaunchAlways()
    {
        return getMetadata(WellKnownMetadata.relaunchAlways);
    }

    public void setRelaunchAlways(boolean relaunchAlways)
    {
        putMetadata(WellKnownMetadata.relaunchAlways, relaunchAlways);
    }

    public String getExtraConfigLines()
    {
        return extraConfigLines;
    }

    public void setExtraConfigLines(String extraConfigLines)
    {
        this.extraConfigLines = extraConfigLines;
    }

    public String getExtraConfigLinesActive()
    {
        return extraConfigLinesActive;
    }

    public void setExtraConfigLinesActive(String extraConfigLinesActive)
    {
        this.extraConfigLinesActive = extraConfigLinesActive;
    }

    public boolean getCertificateWarning()
    {
        return getMetadata(WellKnownMetadata.certificateFailure) != null;
    }

    public EncryptedPayload getMasterKey()
    {
        return masterKey;
    }

    public void setMasterKey(EncryptedPayload masterKey)
    {
        this.masterKey = masterKey;
    }

    public String getAccessKey(BuilderConfiguration cfg) throws
                                                         Exception
    {
        final EncryptedPayload payload = cfg.encrypt(getSysId());

        return payload.encodeAsBase64();
    }

    public boolean checkAccessKey(BuilderConfiguration cfg,
                                  String accessKey) throws
                                                    Exception
    {
        EncryptedPayload ep = EncryptedPayload.decodeFromBase64(accessKey);

        return StringUtils.equals(getSysId(), cfg.decrypt(ep));
    }

    public String encryptForService(BuilderConfiguration cfg,
                                    String value) throws
                                                  Exception
    {
        String masterKey = cfg.decrypt(getMasterKey());

        return EncryptedPayload.build(masterKey, value)
                               .encodeAsBase64();
    }

    public DatabaseMode getDbMode()
    {
        return getDbConfiguration().getMode();
    }

    public EmbeddedDatabaseConfiguration getDbConfiguration()
    {
        return dbConfiguration;
    }

    public void setDbConfiguration(EmbeddedDatabaseConfiguration dbConfiguration)
    {
        this.dbConfiguration = dbConfiguration;
    }

    public EncryptedPayload getMaintPassword()
    {
        return maintPassword;
    }

    public void setMaintPassword(EncryptedPayload maintPassword)
    {
        this.maintPassword = maintPassword;
    }

    //--//

    public boolean isHeapStatusAbnormal()
    {
        return CollectionUtils.findFirst(getHeapStatusHistory(), item -> item.heapWarning && TimeUtils.wasUpdatedRecently(item.timestamp, 2, TimeUnit.HOURS)) != null;
    }

    public List<HubHeapAndThreads> getHeapStatusHistory()
    {
        return getMetadata(WellKnownMetadata.hubHeapStatus);
    }

    //--//

    public BackgroundActivityRecord getCurrentActivity()
    {
        return currentActivity;
    }

    public BackgroundActivityRecord getCurrentActivityIfNotDone()
    {
        BackgroundActivityRecord activity = currentActivity;
        if (activity != null && activity.getStatus()
                                        .isDone())
        {
            this.currentActivity = null;
            activity             = null;
        }

        return activity;
    }

    public void setCurrentActivity(BackgroundActivityRecord currentActivity)
    {
        // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
        if (this.currentActivity != currentActivity)
        {
            this.currentActivity = currentActivity;
        }
    }

    //--//

    public CustomerServiceUpgradeBlockers getUpgradeBlockers()
    {
        CustomerServiceUpgradeBlockers blockers = getMetadata(WellKnownMetadata.upgradeBlockers);
        if (blockers != null)
        {
            ZonedDateTime now = TimeUtils.now();

            blockers.requests.removeIf((request) -> request.until.isBefore(now));

            if (blockers.requests.isEmpty())
            {
                blockers = null;
            }
        }

        return blockers;
    }

    public void updateUpgradeBlockers(UserRecord user,
                                      ZonedDateTime until)
    {
        CustomerServiceUpgradeBlockers blockers = getUpgradeBlockers();
        if (blockers == null)
        {
            blockers = new CustomerServiceUpgradeBlockers();
        }

        if (until != null)
        {
            blockers.add(user, until);
        }
        else
        {
            blockers.remove(user);
        }

        if (blockers.requests.isEmpty())
        {
            blockers = null;
        }

        putMetadata(WellKnownMetadata.upgradeBlockers, blockers);
    }

    //--//

    public boolean getDisableServiceWorker()
    {
        return getMetadata(WellKnownMetadata.disableServiceWorker);
    }

    public boolean setDisableServiceWorker(boolean disableServiceWorker)
    {
        return putMetadata(WellKnownMetadata.disableServiceWorker, disableServiceWorker);
    }

    //--//

    public boolean getDisableEmails()
    {
        return getMetadata(WellKnownMetadata.disableEmails);
    }

    public boolean setDisableEmails(boolean disableEmails)
    {
        return putMetadata(WellKnownMetadata.disableEmails, disableEmails);
    }

    public boolean getDisableTexts()
    {
        return getMetadata(WellKnownMetadata.disableTexts);
    }

    public boolean setDisableTexts(boolean disableTexts)
    {
        return putMetadata(WellKnownMetadata.disableTexts, disableTexts);
    }

    public boolean getUseTestReporter()
    {
        return getMetadata(WellKnownMetadata.useTestReporter);
    }

    public boolean setUseTestReporter(boolean useTestReporter)
    {
        return putMetadata(WellKnownMetadata.useTestReporter, useTestReporter);
    }

    //--//

    public List<CustomerService.AlertThresholds> getAlertThresholds()
    {
        return getMetadata(WellKnownMetadata.alertThresholds);
    }

    public void setAlertThresholds(SessionHolder sessionHolder,
                                   DeploymentRole role,
                                   int warningThreshold,
                                   int alertThreshold) throws
                                                       Exception
    {
        List<CustomerService.AlertThresholds> settings = getAlertThresholds();

        for (CustomerService.AlertThresholds settingsForRole : settings)
        {
            if (settingsForRole.role == role)
            {
                settingsForRole.warningThreshold = warningThreshold;
                settingsForRole.alertThreshold   = alertThreshold;
                role                             = null;
                break;
            }
        }

        if (role != null)
        {
            CustomerService.AlertThresholds settingsForRole = new CustomerService.AlertThresholds();
            settingsForRole.role             = role;
            settingsForRole.warningThreshold = warningThreshold;
            settingsForRole.alertThreshold   = alertThreshold;
            settings.add(settingsForRole);
        }

        putMetadata(WellKnownMetadata.alertThresholds, settings);

        TaskForAlertThresholds.scheduleTask(sessionHolder, this, null);
    }

    public DeployerShutdownConfiguration getBatteryThresholds()
    {
        return getMetadata(WellKnownMetadata.batteryThresholds);
    }

    public void setBatteryThresholds(SessionHolder sessionHolder,
                                     DeployerShutdownConfiguration cfg) throws
                                                                        Exception
    {
        putMetadata(WellKnownMetadata.batteryThresholds, cfg);

        TaskForServiceBatteryThresholds.scheduleTask(sessionHolder, this, null);
    }

    //--//

    public List<CustomerServiceBackupRecord> getBackups()
    {
        return CollectionUtils.asEmptyCollectionIfNull(backups);
    }

    public boolean hasBackups()
    {
        return !getBackups().isEmpty();
    }

    public CustomerServiceBackupRecord findLatestBackup()
    {
        CustomerServiceBackupRecord rec = null;

        for (CustomerServiceBackupRecord rec_backup : getBackups())
        {
            if (rec == null || TimeUtils.isAfterOrNull(rec_backup.getCreatedOn(), rec.getCreatedOn()))
            {
                rec = rec_backup;
            }
        }

        return rec;
    }

    public List<CustomerServiceSecretRecord> getSecrets()
    {
        return CollectionUtils.asEmptyCollectionIfNull(secrets);
    }

    //--//

    public static void streamAllRaw(SessionHolder sessionHolder,
                                    Consumer<RawQueryHelper<CustomerServiceRecord, CustomerService>> applyFilters,
                                    Consumer<CustomerService> callback)
    {
        RawQueryHelper<CustomerServiceRecord, CustomerService> qh = new RawQueryHelper<>(sessionHolder, CustomerServiceRecord.class);

        qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);
        qh.addDate(RecordWithCommonFields_.createdOn, (obj, val) -> obj.createdOn = val);
        qh.addDate(RecordWithCommonFields_.updatedOn, (obj, val) -> obj.updatedOn = val);

        qh.addObject(RecordWithMetadata_.metadataCompressed, byte[].class, (obj, val) -> obj.metadataCompressed = val);

        qh.addReference(CustomerServiceRecord_.customer, CustomerRecord.class, (obj, val) -> obj.customer = val);
        qh.addString(CustomerServiceRecord_.name, (obj, val) -> obj.name = val);
        qh.addString(CustomerServiceRecord_.url, (obj, val) -> obj.url = val);
        qh.addEnum(CustomerServiceRecord_.operationalStatus, DeploymentOperationalStatus.class, (obj, val) -> obj.operationalStatus = val);
        qh.addEnum(CustomerServiceRecord_.instanceType, DeploymentInstance.class, (obj, val) -> obj.instanceType = val);
        qh.addEnum(CustomerServiceRecord_.vertical, CustomerVertical.class, (obj, val) -> obj.vertical = val);
        qh.addInteger(CustomerServiceRecord_.diskSize, (obj, val) -> obj.diskSize = val);
        qh.addBoolean(CustomerServiceRecord_.useDemoData, (obj, val) -> obj.useDemoData = val);
        qh.addString(CustomerServiceRecord_.extraConfigLines, (obj, val) -> obj.extraConfigLines = val);
        qh.addReference(CustomerServiceRecord_.currentActivity, BackgroundActivityRecord.class, (obj, val) -> obj.currentActivity = val);

        qh.addDate(CustomerServiceRecord_.lastOutput, (obj, val) -> obj.lastOutput = val);
        qh.addInteger(CustomerServiceRecord_.lastOffset, (obj, val) -> obj.lastOffset = val);

        if (applyFilters != null)
        {
            applyFilters.accept(qh);
        }

        qh.stream(CustomerService::new, callback);
    }

    public static List<CustomerServiceRecord> getBatch(RecordHelper<CustomerServiceRecord> helper,
                                                       List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    //--//

    public void collectCharges(DeploymentGlobalDescriptor globalDescriptor,
                               Map<TypedRecordIdentity<DeploymentHostRecord>, DeploymentCellularCharges> map)
    {
        CustomerService svc = globalDescriptor.getService(this);
        if (svc != null)
        {
            for (DeploymentHost host : svc.rawHosts)
            {
                DeploymentCellularCharges res = DeploymentHostRecord.WellKnownMetadata.cellularCharges.get(host.decodeMetadata());
                if (res != null)
                {
                    map.put(RecordIdentity.newTypedInstance(DeploymentHostRecord.class, host.sysId), res);
                }
            }
        }
    }

    //--//

    public boolean hasRunningCloudHosts(DeploymentGlobalDescriptor globalDescriptor)
    {
        CustomerService svc = globalDescriptor.getService(this);
        if (svc != null)
        {
            for (DeploymentHost host : svc.findHostsForRole(DeploymentStatus.Ready, null))
            {
                for (DeploymentRole role : host.roles)
                {
                    if (role.cloudBased)
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    //--//

    public List<DeploymentHost> getInstancesForRole(SessionHolder sessionHolder,
                                                    DeploymentRole role,
                                                    boolean onlyWithAgents)
    {
        List<DeploymentHost> hosts = Lists.newArrayList();

        for (DeploymentHost host : DeploymentHostRecord.getHostsInService(sessionHolder, getSysId()))
        {
            if (!host.hasRole(role))
            {
                continue;
            }

            if (onlyWithAgents && !host.ensureInstanceType().hasAgent)
            {
                continue;
            }

            hosts.add(host);
        }

        return hosts;
    }

    public DeploymentTaskRecord findAnyTaskForRole(SessionHolder sessionHolder,
                                                   DeploymentStatus desiredState,
                                                   DeploymentRole desiredRole)
    {
        return CollectionUtils.firstElement(findAllTasksForRole(sessionHolder, desiredState, desiredRole));
    }

    public List<DeploymentTaskRecord> findAllTasksForRole(SessionHolder sessionHolder,
                                                          DeploymentStatus desiredState,
                                                          DeploymentRole desiredRole)
    {
        List<DeploymentTaskRecord> tasks = Lists.newArrayList();

        List<DeploymentHost> hosts = getInstancesForRole(sessionHolder, desiredRole, false);
        for (DeploymentHost host : hosts)
        {
            if (host.status == desiredState)
            {
                DeploymentHostRecord rec_host = sessionHolder.getEntity(DeploymentHostRecord.class, host.sysId);
                tasks.addAll(rec_host.findTasksForPurpose(desiredState, desiredRole, null, true));
            }
        }

        return tasks;
    }

    //--//

    public void lockForStateChange(SessionHolder sessionHolder,
                                   boolean noUpgrade)
    {
        if (currentActivity != null)
        {
            final BackgroundActivityStatus status = currentActivity.getStatus();

            if (status.isDone())
            {
                currentActivity = null;
            }
            else
            {
                throw Exceptions.newGenericException(InvalidStateException.class, "Customer service '%s / %s' already processing a state change", getCustomer().getName(), getName());
            }
        }

        if (!noUpgrade)
        {
            CustomerServiceUpgradeBlockers blockers = getUpgradeBlockers();
            if (blockers != null)
            {
                for (CustomerServiceUpgradeBlocker request : blockers.requests)
                {
                    UserRecord rec = sessionHolder.getEntityOrNull(UserRecord.class, request.user.sysId);
                    if (rec != null)
                    {
                        throw Exceptions.newGenericException(InvalidStateException.class,
                                                             "Upgrade for service '%s / %s' blocked by user '%s' until %s",
                                                             getCustomer().getName(),
                                                             getName(),
                                                             rec.getEmailAddress(),
                                                             request.until);
                    }
                }
            }
        }
    }

    //--//

    public boolean isReadyForBackup(SessionHolder sessionHolder)
    {
        if (getRelaunchAlways())
        {
            // This instance doesn't have persistent state.
            return false;
        }

        if (getOperationalStatus() != DeploymentOperationalStatus.operational)
        {
            // Only care about production instances.
            return false;
        }

        switch (getDbMode())
        {
            case H2OnDisk:
                return findAnyTaskForRole(sessionHolder, DeploymentStatus.Ready, DeploymentRole.hub) != null;

            case MariaDB:
                return findAnyTaskForRole(sessionHolder, DeploymentStatus.Ready, DeploymentRole.database) != null;

            default:
                return false;
        }
    }

    public CustomerServiceDesiredState prepareDesiredState()
    {
        var state = new CustomerServiceDesiredState();

        for (RoleAndArchitectureWithImage roleImage : getRoleImages())
        {
            state.roles.add(CustomerServiceDesiredStateRole.from(roleImage));
        }

        return state;
    }

    public BackgroundActivityRecord refreshCertificate(RecordLocked<CustomerServiceRecord> lock_svc) throws
                                                                                                     Exception
    {
        var state = prepareDesiredState();

        for (CustomerServiceDesiredStateRole role : state.roles)
        {
            switch (role.role)
            {
                case hub:
                case reporter:
                    role.shutdown = true;
                    role.launchIfMissingAndIdle = true;
                    break;

                default:
                    role.launchIfMissingAndIdle = true;
                    break;
            }
        }

        return TaskForDesiredState.scheduleTask(lock_svc, state, Duration.of(10, ChronoUnit.HOURS));
    }

    public BackgroundActivityRecord startBackup(RecordLocked<CustomerServiceRecord> lock_svc,
                                                BackupKind trigger,
                                                boolean startMissingTasks) throws
                                                                           Exception
    {
        var state = prepareDesiredState();
        state.createBackup = trigger;

        if (startMissingTasks)
        {
            for (CustomerServiceDesiredStateRole role : state.roles)
            {
                role.launchIfMissingAndIdle = true;
            }
        }

        return TaskForDesiredState.scheduleTask(lock_svc, state, Duration.of(10, ChronoUnit.HOURS));
    }

    public BackgroundActivityRecord refreshAccounts(SessionHolder sessionHolder) throws
                                                                                 Exception
    {
        return TaskForHubAccountsRefresh.scheduleTask(sessionHolder, this, Duration.of(10, ChronoUnit.MINUTES));
    }

    public BackgroundActivityRecord refreshSecrets(SessionHolder sessionHolder) throws
                                                                                Exception
    {
        return TaskForHubSecretsRefresh.scheduleTask(sessionHolder, this, Duration.of(10, ChronoUnit.MINUTES));
    }

    public BackgroundActivityRecord compactTimeSeries(SessionHolder sessionHolder) throws
                                                                                   Exception
    {
        return TaskForHubCompactTimeSeries.scheduleTask(sessionHolder, this, Duration.of(10, ChronoUnit.MINUTES));
    }

    public BackgroundActivityRecord checkUsages(SessionHolder sessionHolder,
                                                com.optio3.cloud.client.hub.model.UsageFilterRequest filters) throws
                                                                                                              Exception
    {
        return TaskForHubCheckUsages.scheduleTask(sessionHolder, this, filters, Duration.of(10, ChronoUnit.MINUTES));
    }

    //--//

    public void checkRemoveConditions(ValidationResultsHolder validation)
    {
        List<DeploymentHost> hosts = DeploymentHostRecord.getHostsInService(validation.sessionHolder, getSysId());
        if (!hosts.isEmpty())
        {
            CustomerRecord rec_cust = getCustomer();

            validation.addFailure("instances", "Can't remove customer service '%s / %s' because it has resources associated with it", rec_cust.getName(), getName());
        }

        if (!getBackups().isEmpty())
        {
            validation.addFailure("backups", "Can't remove customer service '%s / %s' because it has backups", getCustomer().getName(), getName());
        }
    }

    public void remove(ValidationResultsHolder validation,
                       RecordHelper<CustomerServiceRecord> helper) throws
                                                                   Exception
    {
        checkRemoveConditions(validation);

        if (validation.canProceed())
        {
            RecordHelper<CustomerServiceBackupRecord> backupHelper = helper.wrapFor(CustomerServiceBackupRecord.class);
            for (CustomerServiceBackupRecord rec_backup : Lists.newArrayList(getBackups()))
            {
                rec_backup.remove(validation, backupHelper);
            }

            cleanupCloudResources(validation.sessionHolder);

            helper.delete(this);
        }
    }

    private void cleanupCloudResources(SessionHolder sessionHolder)
    {
        BuilderConfiguration cfg = sessionHolder.getServiceNonNull(BuilderConfiguration.class);

        CustomerRecord rec_cust = getCustomer();

        Map<CustomerServiceRecord, CommonDeployer> lookup = Maps.newHashMap();

        for (CustomerServiceRecord rec_svc : rec_cust.getServices())
        {
            DeploymentInstance instanceType = rec_svc.getInstanceType();
            if (instanceType.deployerClass != null)
            {
                CommonDeployer deployer = BaseDeployLogic.allocateDeployer(rec_svc.getInstanceAccount(), instanceType, rec_svc.getInstanceRegion(), cfg, rec_cust, rec_svc, null);

                lookup.put(rec_svc, deployer);
            }
        }

        CommonDeployer deployer = lookup.get(this);
        if (deployer != null)
        {
            lookup.remove(this);

            deployer.cleanupService();

            boolean foundSameRegion   = false;
            boolean foundSameProvider = false;

            for (CustomerServiceRecord rec_svc : lookup.keySet())
            {
                CommonDeployer deployer2 = lookup.get(rec_svc);
                if (deployer.getClass() == deployer2.getClass())
                {
                    foundSameProvider = true;

                    if (StringUtils.equals(rec_svc.getInstanceRegion(), getInstanceRegion()))
                    {
                        foundSameRegion = true;
                    }
                }
            }

            if (!foundSameRegion)
            {
                deployer.cleanupCustomerInRegion();
            }

            if (!foundSameProvider)
            {
                deployer.cleanupCustomer();
            }
        }
    }

    //--//

    public static CustomerServiceRecord findSimilarURL(RecordHelper<CustomerServiceRecord> helper,
                                                       String url)
    {
        try
        {
            URL    parsedUrl = new URL(url);
            String host      = parsedUrl.getHost();

            for (CustomerServiceRecord rec_svc : helper.listAll())
            {
                URL    url2         = new URL(rec_svc.getUrl());
                String hostExisting = url2.getHost();

                if (StringUtils.equals(host, hostExisting))
                {
                    return rec_svc;
                }
            }
        }
        catch (MalformedURLException e)
        {
            throw new InvalidStateException(e.getMessage());
        }

        return null;
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
    public void refineLogQuery(LogHandler.JoinHelper<?, CustomerServiceLogRecord> jh)
    {
        jh.addWhereClauseWithEqual(jh.rootLog, CustomerServiceLogRecord_.owningService, this);
    }

    @Override
    public CustomerServiceLogRecord allocateNewLogInstance()
    {
        return CustomerServiceLogRecord.newInstance(this);
    }

    public static LogHandler<CustomerServiceRecord, CustomerServiceLogRecord> allocateLogHandler(RecordLocked<CustomerServiceRecord> lock)
    {
        return new LogHandler<>(lock, CustomerServiceLogRecord.class);
    }

    public static LogHandler<CustomerServiceRecord, CustomerServiceLogRecord> allocateLogHandler(SessionHolder sessionHolder,
                                                                                                 CustomerServiceRecord rec)
    {
        return new LogHandler<>(sessionHolder, rec, CustomerServiceLogRecord.class);
    }
}
