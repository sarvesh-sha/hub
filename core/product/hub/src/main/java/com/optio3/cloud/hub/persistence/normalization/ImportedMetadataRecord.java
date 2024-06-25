/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.normalization;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.metamodel.SingularAttribute;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.hub.model.dataImports.DataImportRun;
import com.optio3.cloud.hub.model.normalization.ImportedMetadata;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.PersistAsJsonHelper;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithSequenceNumber;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.metadata.normalization.ImportExportData;
import com.optio3.serialization.ObjectMappers;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "IMPORTED_METADATA")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "ImportedMetadata", model = ImportedMetadata.class, metamodel = ImportedMetadataRecord_.class)
public class ImportedMetadataRecord extends RecordWithSequenceNumber<ImportedMetadataRecord> implements ModelMapperTarget<ImportedMetadata, ImportedMetadataRecord_>
{
    private static final TypeReference<List<ImportExportData>> c_typeRef = new TypeReference<List<ImportExportData>>()
    {
    };

    //--//

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "active", nullable = false)
    private boolean active;

    // TODO: UPGRADE PATCH: Remove column on next schema change
    @Lob
    @Column(name = "metadata")
    @Basic(fetch = FetchType.LAZY)
    private String metadata;

    @Lob
    @Column(name = "metadata_compressed")
    @Basic(fetch = FetchType.LAZY)
    private byte[] metadataCompressed;

    @Transient
    private final PersistAsJsonHelper<byte[], List<ImportExportData>> m_metadataCompressedParser = new PersistAsJsonHelper<>(() -> metadataCompressed,
                                                                                                                             (val) -> metadataCompressed = val,
                                                                                                                             byte[].class,
                                                                                                                             c_typeRef,
                                                                                                                             ObjectMappers.SkipNulls);

    //--//

    public static ImportedMetadataRecord newInstance(RecordHelper<ImportedMetadataRecord> helper,
                                                     List<ImportExportData> metadata,
                                                     Integer version)
    {
        helper.lockTableUntilEndOfTransaction(30, TimeUnit.SECONDS);

        ImportedMetadataRecord rec_new = new ImportedMetadataRecord();
        rec_new.setMetadata(metadata);

        rec_new.version = rec_new.assignUniqueNumber(helper, version, null);

        helper.persist(rec_new);

        return rec_new;
    }

    @Override
    protected SingularAttribute<ImportedMetadataRecord, Integer> fetchSequenceNumberField()
    {
        return ImportedMetadataRecord_.version;
    }

    @Override
    protected int fetchSequenceNumberValue()
    {
        return getVersion();
    }

    //--//

    public int getVersion()
    {
        return version;
    }

    public boolean isActive()
    {
        return active;
    }

    public List<ImportExportData> getMetadata()
    {
        return m_metadataCompressedParser.get();
    }

    public void setMetadata(List<ImportExportData> metadata)
    {
        if (metadata != null && metadata.isEmpty())
        {
            metadata = null;
        }

        m_metadataCompressedParser.set(metadata);
    }

    //--//

    public static TypedRecordIdentityList<ImportedMetadataRecord> list(RecordHelper<ImportedMetadataRecord> helper)
    {
        return QueryHelperWithCommonFields.list(helper, (jh) ->
        {
            jh.addOrderBy(jh.root, ImportedMetadataRecord_.version, false);
        });
    }

    public static List<ImportedMetadataRecord> getBatch(RecordHelper<ImportedMetadataRecord> helper,
                                                        List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    public void makeActive(RecordHelper<ImportedMetadataRecord> helper)
    {
        helper.lockTableUntilEndOfTransaction(10, TimeUnit.SECONDS);

        ImportedMetadataRecord rec_active = findActive(helper);
        if (rec_active != this)
        {
            if (rec_active != null)
            {
                rec_active.active = false;
            }

            this.active = true;
        }
    }

    public static ImportedMetadataRecord findActive(RecordHelper<ImportedMetadataRecord> helper)
    {
        return QueryHelperWithCommonFields.getFirstMatch(helper, (jh) ->
        {
            jh.addWhereClauseWithEqual(jh.root, ImportedMetadataRecord_.active, true);
        });
    }

    public static ImportedMetadataRecord findVersion(RecordHelper<ImportedMetadataRecord> helper,
                                                     int version)
    {
        return QueryHelperWithCommonFields.getFirstMatch(helper, (jh) ->
        {
            jh.addWhereClauseWithEqual(jh.root, ImportedMetadataRecord_.version, version);
        });
    }

    //--//

    public static List<RecordLocator<DeviceRecord>> extractDevices(RecordHelper<DeviceRecord> helper,
                                                                   DataImportRun run) throws
                                                                                      Exception
    {
        List<RecordLocator<DeviceRecord>> locators = Lists.newArrayList();

        if (run.devices == null)
        {
            RecordHelper<NetworkAssetRecord> helperNetwork = helper.wrapFor(NetworkAssetRecord.class);
            for (NetworkAssetRecord rec_network : helperNetwork.listAll())
            {
                rec_network.enumerateChildren(helper.wrapFor(DeviceRecord.class), true, -1, null, (rec_device) ->
                {
                    locators.add(helper.asLocator(rec_device));

                    return StreamHelperNextAction.Continue_Evict;
                });
            }
        }
        else
        {
            for (TypedRecordIdentity<DeviceRecord> ri : run.devices)
            {
                locators.add(RecordLocator.create(helper, ri));
            }
        }

        return locators;
    }
}
