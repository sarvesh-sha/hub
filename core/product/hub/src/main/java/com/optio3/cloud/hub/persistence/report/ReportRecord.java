/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

import javax.persistence.Basic;
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
import javax.persistence.Transient;
import javax.persistence.Tuple;
import javax.persistence.criteria.Path;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.annotation.Optio3UpgradeValue;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.model.report.Report;
import com.optio3.cloud.hub.model.report.ReportFilterRequest;
import com.optio3.cloud.hub.model.report.ReportReason;
import com.optio3.cloud.hub.model.report.ReportStatus;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.SortCriteria;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import org.apache.commons.io.IOUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "REPORT")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "Report", model = Report.class, metamodel = ReportRecord_.class)
public class ReportRecord extends RecordWithCommonFields implements ModelMapperTarget<Report, ReportRecord_>
{
    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getReportDefinition")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.CLEAR, getter = "getReportDefinition", setter = "setReportDefinition")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "report_definition", nullable = false, foreignKey = @ForeignKey(name = "REPORT__REPORT_DEFINITION__FK"))
    private ReportDefinitionRecord reportDefinition;

    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getReportDefinitionVersion")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.CLEAR, getter = "getReportDefinitionVersion", setter = "setReportDefinitionVersion")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "report_definition_version", nullable = false, foreignKey = @ForeignKey(name = "REPORT__REPORT_DEFINITION_VERSION__FK"))
    private ReportDefinitionVersionRecord reportDefinitionVersion;

    @Column(name = "range_start")
    private ZonedDateTime rangeStart;

    @Column(name = "range_end")
    private ZonedDateTime rangeEnd;

    @Optio3UpgradeValue("Queued")
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    @Optio3UpgradeValue("OnDemand")
    @Column(name = "reason", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportReason reason;

    @Lob
    @Column(name = "bytes")
    @Basic(fetch = FetchType.LAZY)
    private byte[] bytes;

    @Column(name = "size")
    private int size;

    @Transient
    private String downloadUrl;

    public ReportRecord()
    {
    }

    //--//

    public static ReportRecord newInstance(ReportDefinitionRecord rec_reportDefinition,
                                           ReportDefinitionVersionRecord rec_reportDefinitionVersion)
    {
        ReportRecord rec_report = new ReportRecord();
        rec_report.reportDefinition        = rec_reportDefinition;
        rec_report.reportDefinitionVersion = rec_reportDefinitionVersion;
        rec_report.setStatus(ReportStatus.Queued);
        return rec_report;
    }

    //--//

    public ReportDefinitionVersionRecord getReportDefinitionVersion()
    {
        return reportDefinitionVersion;
    }

    public void setReportDefinitionVersion(ReportDefinitionVersionRecord reportDefinitionVersion)
    {
        if (this.reportDefinitionVersion != reportDefinitionVersion)
        {
            this.reportDefinitionVersion = reportDefinitionVersion;
        }
    }

    public ReportDefinitionRecord getReportDefinition()
    {
        return reportDefinition;
    }

    public void setReportDefinition(ReportDefinitionRecord reportDefinition)
    {
        if (this.reportDefinition != reportDefinition)
        {
            this.reportDefinition = reportDefinition;
        }
    }

    public ZonedDateTime getRangeStart()
    {
        return rangeStart;
    }

    public void setRangeStart(ZonedDateTime rangeStart)
    {
        this.rangeStart = rangeStart;
    }

    public ZonedDateTime getRangeEnd()
    {
        return rangeEnd;
    }

    public void setRangeEnd(ZonedDateTime rangeEnd)
    {
        this.rangeEnd = rangeEnd;
    }

    public ReportStatus getStatus()
    {
        return status;
    }

    public void setStatus(ReportStatus status)
    {
        this.status = status;
    }

    public byte[] getBytes()
    {
        try
        {
            ByteArrayOutputStream output       = new ByteArrayOutputStream();
            InflaterOutputStream  decompressor = new InflaterOutputStream(output);
            IOUtils.write(bytes, decompressor);
            return output.toByteArray();
        }
        catch (IOException ex)
        {
            return null;
        }
    }

    public void setBytes(byte[] bytes)
    {
        size = bytes.length;
        try
        {
            ByteArrayOutputStream output     = new ByteArrayOutputStream();
            DeflaterOutputStream  compressor = new DeflaterOutputStream(output);
            IOUtils.write(bytes, compressor);
            compressor.close();
            this.bytes = output.toByteArray();
        }
        catch (IOException ex)
        {
        }
    }

    public ReportReason getReason()
    {
        return reason;
    }

    public void setReason(ReportReason reason)
    {
        this.reason = reason;
    }

    public String getDownloadUrl()
    {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl)
    {
        this.downloadUrl = downloadUrl;
    }

    public int getSize()
    {
        return size;
    }

    //--//

    public static TypedRecordIdentityList<ReportRecord> getAllByUser(RecordHelper<ReportRecord> helper,
                                                                     UserRecord user)
    {
        return QueryHelperWithCommonFields.list(helper, (qh) ->
        {
            Path<ReportDefinitionRecord> join = qh.root.join(ReportRecord_.reportDefinition);
            qh.addWhereClauseWithEqual(join, ReportDefinitionRecord_.user, user);
        });
    }

    public static List<ReportRecord> getBatch(RecordHelper<ReportRecord> helper,
                                              List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    protected static class JoinHelper<T> extends QueryHelperWithCommonFields<T, ReportRecord>
    {
        JoinHelper(RecordHelper<ReportRecord> helper,
                   Class<T> clz)
        {
            super(helper, clz);
        }

        //--//

        protected void applyFilters(ReportFilterRequest filters)
        {
            if (filters.definitionIds != null)
            {
                filterByDefinitions(filters.definitionIds);
            }

            if (filters.sortBy != null)
            {
                for (SortCriteria sort : filters.sortBy)
                {
                    handleSortCriteria(sort);
                }
            }
        }

        private void filterByDefinitions(List<String> definitionIds)
        {
            addWhereReferencingSysIds(root, ReportRecord_.reportDefinition, definitionIds);
        }

        protected void handleSortCriteria(SortCriteria sort)
        {
            switch (sort.column)
            {
                case "createdOn":
                {
                    addOrderBy(root, RecordWithCommonFields_.createdOn, sort.ascending);
                    break;
                }

                case "version":
                {
                    addOrderBy(root, ReportRecord_.reportDefinitionVersion, sort.ascending);
                    break;
                }

                case "rangeStart":
                {
                    addOrderBy(root, ReportRecord_.rangeStart, sort.ascending);
                    break;
                }

                case "rangeEnd":
                {
                    addOrderBy(root, ReportRecord_.rangeEnd, sort.ascending);
                    break;
                }

                case "reason":
                {
                    addOrderBy(root, ReportRecord_.reason, sort.ascending);
                    break;
                }

                case "status":
                {
                    addOrderBy(root, ReportRecord_.status, sort.ascending);
                    break;
                }
            }
        }

        public static List<RecordIdentity> returnFilterTuples(RecordHelper<ReportRecord> helper,
                                                              JoinHelper<Tuple> jh)
        {
            List<RecordIdentity> res = Lists.newArrayList();

            jh.cq.multiselect(jh.root.get(RecordWithCommonFields_.sysId), jh.root.get(RecordWithCommonFields_.updatedOn));

            for (Tuple t : jh.list())
            {
                RecordIdentity ri = RecordIdentity.newInstance(helper, t, 0, 1);
                res.add(ri);
            }

            return res;
        }
    }

    public static List<RecordIdentity> filterReports(RecordHelper<ReportRecord> helper,
                                                     ReportFilterRequest filters)
    {
        JoinHelper<Tuple> jh = new JoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        //--//

        return JoinHelper.returnFilterTuples(helper, jh);
    }

    //--//

    public String generateDownloadUrl(String name,
                                      ZonedDateTime createdOn,
                                      HubConfiguration cfg)
    {
        return Report.getDownloadUrl(getSysId(), name, createdOn, cfg);
    }
}
