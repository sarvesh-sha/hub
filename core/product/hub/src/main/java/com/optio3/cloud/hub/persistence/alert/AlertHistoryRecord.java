/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.alert;

import static java.util.Objects.requireNonNull;

import java.time.ZonedDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.annotation.Optio3UpgradeValue;
import com.optio3.cloud.hub.model.alert.AlertEventLevel;
import com.optio3.cloud.hub.model.alert.AlertEventType;
import com.optio3.cloud.hub.model.alert.AlertHistory;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.SessionHolder;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "ALERT_HISTORY", indexes = { @Index(name = "ALERT_HISTORY__OCCURRED", columnList = "sys_created_on") })
@Optio3TableInfo(externalId = "AlertHistory", model = AlertHistory.class, metamodel = AlertHistoryRecord_.class)
public class AlertHistoryRecord extends RecordWithCommonFields implements ModelMapperTarget<AlertHistory, AlertHistoryRecord_>
{
    @Optio3ControlNotifications(reason = "Only notify alert of history's changes", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getAlert")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getAlert")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "alert", nullable = false, foreignKey = @ForeignKey(name = "ALERT__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private AlertRecord alert;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private AlertEventLevel level;

    @Optio3UpgradeValue("created")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AlertEventType type;

    @Lob
    @Column(name = "text", length = 8192)
    private String text;

    public AlertHistoryRecord()
    {
    }

    public static AlertHistoryRecord newInstance(AlertRecord rec_alert,
                                                 AlertEventLevel level,
                                                 AlertEventType type)

    {
        requireNonNull(rec_alert);

        AlertHistoryRecord res = new AlertHistoryRecord();
        res.alert = rec_alert;
        res.level = level;
        res.type  = type;

        return res;
    }

    //--//

    public AlertRecord getAlert()
    {
        return alert;
    }

    public boolean setAlert(AlertRecord alert)
    {
        // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
        if (SessionHolder.sameEntity(this.alert, alert))
        {
            return false; // Nothing changed.
        }

        this.alert = alert;
        return true;
    }

    public AlertEventLevel getLevel()
    {
        return level;
    }

    public void setLevel(AlertEventLevel level)
    {
        this.level = level;
    }

    public AlertEventType getType()
    {
        return type;
    }

    public void setType(AlertEventType type)
    {
        this.type = type;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    //--//

    public static TypedRecordIdentityList<AlertHistoryRecord> listSorted(RecordHelper<AlertHistoryRecord> helper,
                                                                         AlertRecord alert,
                                                                         ZonedDateTime rangeStart,
                                                                         ZonedDateTime rangeEnd,
                                                                         int maxResults)
    {
        return QueryHelperWithCommonFields.list(helper, maxResults, (jh) ->
        {
            Root<AlertHistoryRecord> rootAlertHistory = jh.root;

            if (alert != null)
            {
                jh.addWhereClauseWithEqual(rootAlertHistory, AlertHistoryRecord_.alert, alert);
            }

            jh.filterTimestampsCoveredByTargetRange(rootAlertHistory, RecordWithCommonFields_.createdOn, rangeStart, rangeEnd);

            jh.addOrderBy(rootAlertHistory, RecordWithCommonFields_.createdOn, false);
        });
    }

    public static TypedRecordIdentityList<AlertHistoryRecord> listSorted(RecordHelper<AlertHistoryRecord> helper,
                                                                         AssetRecord rec_asset,
                                                                         ZonedDateTime rangeStart,
                                                                         ZonedDateTime rangeEnd,
                                                                         int maxResults)
    {
        return QueryHelperWithCommonFields.list(helper, maxResults, (jh) ->
        {
            Root<AlertHistoryRecord>              rootAlertHistory = jh.root;
            Join<AlertHistoryRecord, AlertRecord> rootAlert        = rootAlertHistory.join(AlertHistoryRecord_.alert);

            jh.addWhereClauseWithEqual(rootAlert, AlertRecord_.asset, rec_asset);

            jh.filterTimestampsCoveredByTargetRange(rootAlertHistory, RecordWithCommonFields_.createdOn, rangeStart, rangeEnd);

            jh.addOrderBy(rootAlertHistory, RecordWithCommonFields_.createdOn, false);
        });
    }

    public static List<AlertHistoryRecord> getBatch(RecordHelper<AlertHistoryRecord> helper,
                                                    List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }
}
