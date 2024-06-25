/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.customer;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.annotation.Optio3UpgradeValue;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.model.customer.CustomerServiceBackup;
import com.optio3.cloud.builder.model.customer.RoleAndArchitectureWithImage;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.MetadataField;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RawQueryHelper;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.RecordWithMetadata;
import com.optio3.cloud.persistence.RecordWithMetadata_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.AwsHelper;
import com.optio3.infra.WellKnownSites;
import com.optio3.infra.directory.CredentialDirectory;
import com.optio3.util.Exceptions;
import com.optio3.util.function.BiConsumerWithException;
import com.optio3.util.function.BiFunctionWithException;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "CUSTOMER_SERVICE_BACKUP")
@Optio3TableInfo(externalId = "CustomerServiceBackup", model = CustomerServiceBackup.class, metamodel = CustomerServiceBackupRecord_.class,
                 metadata = CustomerServiceBackupRecord.WellKnownMetadata.class)
public class CustomerServiceBackupRecord extends RecordWithMetadata implements ModelMapperTarget<CustomerServiceBackup, CustomerServiceBackupRecord_>
{
    public static class WellKnownMetadata implements Optio3TableInfo.IMetadataDigest
    {
        private static final TypeReference<List<RoleAndArchitectureWithImage>> s_typeRef_listOfRoleImages = new TypeReference<>()
        {
        };

        public static final MetadataField<DatabaseMode> db_mode = new MetadataField<>("mode", DatabaseMode.class);

        public static final MetadataField<List<RoleAndArchitectureWithImage>> role_images = new MetadataField<>("role_images", s_typeRef_listOfRoleImages, Lists::newArrayList);
    }

    //--//

    /**
     * The context of this record.
     */
    @Optio3ControlNotifications(reason = "Only notify service", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getCustomerService")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "customer_service", nullable = false, foreignKey = @ForeignKey(name = "CUSTOMER_SERVICE_BACKUP__CUSTOMER_SERVICE__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private CustomerServiceRecord customerService;

    //--//

    @Column(name = "file_id", nullable = false)
    private String fileId;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "file_id_on_agent")
    private String fileIdOnAgent;

    @Column(name = "pending_transfer")
    private boolean pendingTransfer;

    @Optio3UpgradeValue("OnDemand")
    @Enumerated(EnumType.STRING)
    @Column(name = "backup_trigger", nullable = false) // trigger is a reserved keyword...
    private BackupKind trigger;

    @Lob
    @Column(name = "extra_config_lines")
    private String extraConfigLines;

    //--//

    public CustomerServiceBackupRecord()
    {
    }

    public static CustomerServiceBackupRecord newInstance(CustomerServiceRecord customerService,
                                                          BackupKind trigger,
                                                          String fileId,
                                                          long fileSize,
                                                          String filedIdOnAgent)
    {
        requireNonNull(customerService);

        CustomerServiceBackupRecord res = new CustomerServiceBackupRecord();
        res.customerService = customerService;
        res.trigger         = trigger;
        res.fileId          = fileId;
        res.fileSize        = fileSize;
        res.fileIdOnAgent   = filedIdOnAgent;
        res.pendingTransfer = filedIdOnAgent != null;
        return res;
    }

    //--//

    public CustomerServiceRecord getCustomerService()
    {
        return customerService;
    }

    public String getFileId()
    {
        return fileId;
    }

    public long getFileSize()
    {
        return fileSize;
    }

    public String getFileIdOnAgent()
    {
        return fileIdOnAgent;
    }

    public void setFileIdOnAgent(String fileIdOnAgent)
    {
        this.fileIdOnAgent = fileIdOnAgent;
    }

    public boolean isPendingTransfer()
    {
        return pendingTransfer;
    }

    public void setPendingTransfer(boolean pendingTransfer)
    {
        this.pendingTransfer = pendingTransfer;
    }

    public BackupKind getTrigger()
    {
        return trigger;
    }

    public String getExtraConfigLines()
    {
        return extraConfigLines;
    }

    public void setExtraConfigLines(String extraConfigLines)
    {
        this.extraConfigLines = extraConfigLines;
    }

    public List<RoleAndArchitectureWithImage> getRoleImages()
    {
        return getMetadata(WellKnownMetadata.role_images);
    }

    public boolean setRoleImages(List<RoleAndArchitectureWithImage> lst)
    {
        return putMetadata(WellKnownMetadata.role_images, lst);
    }

    //--//

    public static void streamAllRaw(SessionHolder sessionHolder,
                                    Consumer<RawQueryHelper<CustomerServiceBackupRecord, CustomerServiceBackup>> applyFilters,
                                    Consumer<CustomerServiceBackup> callback)
    {
        RawQueryHelper<CustomerServiceBackupRecord, CustomerServiceBackup> qh = new RawQueryHelper<>(sessionHolder, CustomerServiceBackupRecord.class);

        qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);
        qh.addDate(RecordWithCommonFields_.createdOn, (obj, val) -> obj.createdOn = val);
        qh.addDate(RecordWithCommonFields_.updatedOn, (obj, val) -> obj.updatedOn = val);

        qh.addObject(RecordWithMetadata_.metadataCompressed, byte[].class, (obj, val) -> obj.metadataCompressed = val);

        qh.addReference(CustomerServiceBackupRecord_.customerService, CustomerServiceRecord.class, (obj, val) -> obj.customerService = val);
        qh.addString(CustomerServiceBackupRecord_.fileId, (obj, val) -> obj.fileId = val);
        qh.addLong(CustomerServiceBackupRecord_.fileSize, (obj, val) -> obj.fileSize = val);
        qh.addString(CustomerServiceBackupRecord_.fileIdOnAgent, (obj, val) -> obj.fileIdOnAgent = val);
        qh.addBoolean(CustomerServiceBackupRecord_.pendingTransfer, (obj, val) -> obj.pendingTransfer = val);
        qh.addEnum(CustomerServiceBackupRecord_.trigger, BackupKind.class, (obj, val) -> obj.trigger = val);
        qh.addString(CustomerServiceBackupRecord_.extraConfigLines, (obj, val) -> obj.extraConfigLines = val);

        if (applyFilters != null)
        {
            applyFilters.accept(qh);
        }

        qh.stream(CustomerServiceBackup::new, callback);
    }

    public static List<CustomerServiceBackupRecord> getBatch(RecordHelper<CustomerServiceBackupRecord> helper,
                                                             List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    //--//

    public void saveSettings()
    {
        CustomerServiceRecord rec_svc = getCustomerService();

        setExtraConfigLines(rec_svc.getExtraConfigLinesActive());

        setRoleImages(rec_svc.getRoleImages());
    }

    public void restoreSettings(CustomerServiceRecord rec_svc)
    {
        rec_svc.setExtraConfigLines(getExtraConfigLines());
    }

    //--//

    public void saveFileToCloud(CredentialDirectory credentials,
                                File source) throws
                                             Exception
    {
        prepareForCloudAccess(credentials, (aws, fileOnS3) ->
        {
            aws.saveFileToS3(fileOnS3, source);
        });
    }

    public void loadFileFromCloud(CredentialDirectory credentials,
                                  File destination) throws
                                                    Exception
    {
        prepareForCloudAccess(credentials, (aws, fileOnS3) ->
        {
            aws.loadFileFromS3(fileOnS3, destination);
        });
    }

    public InputStream streamFileFromCloud(CredentialDirectory credentials) throws
                                                                            Exception
    {
        return prepareForCloudAccess(credentials, (aws, fileOnS3) ->
        {
            return aws.loadStreamFromS3(fileOnS3);
        });
    }

    public void deleteFileFromCloud(CredentialDirectory credentials) throws
                                                                     Exception
    {
        if (fileId != null)
        {
            prepareForCloudAccess(credentials, (aws, fileOnS3) ->
            {
                aws.deleteFileFromS3(fileOnS3);
            });

            fileId = null;
        }
    }

    private void prepareForCloudAccess(CredentialDirectory credentials,
                                       BiConsumerWithException<AwsHelper, String> callback) throws
                                                                                            Exception
    {
        prepareForCloudAccess(credentials, (helper, fileOnS3) ->
        {
            callback.accept(helper, fileOnS3);
            return null;
        });
    }

    private <T> T prepareForCloudAccess(CredentialDirectory credentials,
                                        BiFunctionWithException<AwsHelper, String, T> callback) throws
                                                                                                Exception
    {
        if (fileId == null)
        {
            throw Exceptions.newGenericException(InvalidStateException.class, "No backup file");
        }

        CustomerServiceRecord rec_svc      = getCustomerService();
        CustomerRecord        rec_customer = rec_svc.getCustomer();
        String                cloudId      = rec_customer.getCloudId();
        String                instanceId   = AwsHelper.sanitizeId(rec_svc.getName());

        try (AwsHelper aws = AwsHelper.buildWithDirectoryLookup(credentials, WellKnownSites.optio3DomainName(), Regions.US_WEST_2))
        {
            String backupPath = String.format("%s/%s.tgz", instanceId, getFileId());
            String fileOnS3   = aws.formatBackupPath(cloudId, backupPath);

            return callback.apply(aws, fileOnS3);
        }
    }

    //--//

    public void checkRemoveConditions(ValidationResultsHolder validation)
    {
        CustomerServiceRecord rec_svc = getCustomerService();
        if (rec_svc != null)
        {
            CustomerRecord rec_cust = rec_svc.getCustomer();

            if (rec_svc.getCurrentActivityIfNotDone() != null)
            {
                validation.addFailure("customerService",
                                      "Can't delete backup '%s' for customer service '%s / %s' because the service is undergoing changes",
                                      this.getSysId(),
                                      rec_cust.getName(),
                                      rec_svc.getName());
            }

            if (isPendingTransfer())
            {
                validation.addFailure("transferPending",
                                      "Can't delete backup '%s' for customer service '%s / %s' because it's uploading to the cloud",
                                      this.getSysId(),
                                      rec_cust.getName(),
                                      rec_svc.getName());
            }
        }
    }

    public void remove(ValidationResultsHolder validation,
                       RecordHelper<CustomerServiceBackupRecord> helper) throws
                                                                         Exception
    {
        checkRemoveConditions(validation);

        if (validation.canProceed())
        {
            BuilderConfiguration cfg = helper.getServiceNonNull(BuilderConfiguration.class);

            deleteFileFromCloud(cfg.credentials);

            helper.delete(this);
        }
    }
}
