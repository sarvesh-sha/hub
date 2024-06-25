/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.alert;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3Cascade.Flavor;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.persistence.CommonLogRecord;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "ALERT_DEFINITION_LOG")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "AlertDefinitionLog", model = BaseModel.class, metamodel = AlertDefinitionLogRecord_.class)
public class AlertDefinitionLogRecord extends CommonLogRecord
{
    /**
     * Bound to this job step.
     */
    @Optio3ControlNotifications(reason = "The alert definition is already touched when we change this record", direct = Notify.NEVER, reverse = Notify.NEVER)
    @Optio3Cascade(mode = Flavor.DELETE, getter = "getOwningDefinition")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "owning_definition", nullable = false, foreignKey = @ForeignKey(name = "OWNING_ALERT_DEFINITION_FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private AlertDefinitionRecord owningDefinition;

    public static AlertDefinitionLogRecord newInstance(AlertDefinitionRecord rec_def)
    {
        AlertDefinitionLogRecord rec_log = new AlertDefinitionLogRecord();
        rec_log.owningDefinition = rec_def;

        return rec_log;
    }

    //--//

    public AlertDefinitionRecord getOwningDefinition()
    {
        return owningDefinition;
    }
}
