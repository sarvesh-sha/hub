/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.message;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Tuple;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.hub.model.message.UserMessage;
import com.optio3.cloud.hub.model.message.UserMessageFilterRequest;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.SessionHolder;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.search.annotations.Field;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "USER_MESSAGE")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "UserMessage", model = UserMessage.class, metamodel = UserMessageRecord_.class)
public abstract class UserMessageRecord extends RecordWithCommonFields implements ModelMapperTarget<UserMessage, UserMessageRecord_>
{
    @Optio3ControlNotifications(reason = "Notify user of changes", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getUser")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getUser")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "user", nullable = false, foreignKey = @ForeignKey(name = "USER_MESSAGE__USER__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private UserRecord user;

    //--//

    @Column(name = "subject")
    @Field
    private String subject;

    @Column(name = "text", length = 8192)
    @Field
    private String body;

    @Column(name = "flag_new")
    private boolean flagNew;

    @Column(name = "flag_read")
    private boolean flagRead;

    @Column(name = "flag_active")
    private boolean flagActive;

    //--//

    public void populateFromDemo(SessionHolder holder,
                                 UserMessage msg)
    {
        this.user = holder.fromIdentity(msg.user);

        this.subject = msg.subject;
        this.body    = msg.body;
    }

    public void persist(SessionHolder sessionHolder)
    {
        persist(sessionHolder, this);
    }

    private static <T extends UserMessageRecord> void persist(SessionHolder sessionHolder,
                                                              T obj)
    {
        @SuppressWarnings("unchecked") final Class<T> aClass = (Class<T>) obj.getClass();

        RecordHelper<T> messageHelper = sessionHolder.createHelper(aClass);

        obj.setFlagActive(true);
        obj.setFlagNew(true);
        obj.setFlagRead(false);

        messageHelper.persist(obj);
    }
    //--//

    public UserRecord getUser()
    {
        return user;
    }

    protected void setUser(UserRecord user)
    {
        this.user = user;
    }

    public String getSubject()
    {
        return subject;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    public String getBody()
    {
        return body;
    }

    public void setBody(String body)
    {
        this.body = body;
    }

    public boolean getFlagNew()
    {
        return flagNew;
    }

    public void setFlagNew(boolean flagNew)
    {
        this.flagNew = flagNew;
    }

    public boolean getFlagRead()
    {
        return flagRead;
    }

    public void setFlagRead(boolean flagRead)
    {
        this.flagRead = flagRead;
    }

    public boolean getFlagActive()
    {
        return flagActive;
    }

    public void setFlagActive(boolean flagActive)
    {
        this.flagActive = flagActive;
    }

    //--//

    private static class JoinHelper<T> extends QueryHelperWithCommonFields<T, UserMessageRecord>
    {
        private final UserRecord m_rec_user;

        JoinHelper(RecordHelper<UserMessageRecord> helper,
                   UserRecord rec_user,
                   Class<T> clz)
        {
            super(helper, clz);

            m_rec_user = rec_user;
        }

        //--//

        void applyFilters(UserMessageFilterRequest filters)
        {
            addWhereClauseWithEqual(root, UserMessageRecord_.user, m_rec_user);

            if (filters.flagNew != null)
            {
                addWhereClauseWithEqual(root, UserMessageRecord_.flagNew, filters.flagNew);
            }

            if (filters.flagRead != null)
            {
                addWhereClauseWithEqual(root, UserMessageRecord_.flagRead, filters.flagRead);
            }

            if (filters.flagActive != null)
            {
                addWhereClauseWithEqual(root, UserMessageRecord_.flagActive, filters.flagActive);
            }
        }

        void sort()
        {
            addOrderBy(root, RecordWithCommonFields_.createdOn, false);
        }

        //--//

        public static List<RecordIdentity> returnFilterTuples(RecordHelper<UserMessageRecord> helper,
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

    //--//

    public static List<UserMessageRecord> getBatch(RecordHelper<UserMessageRecord> helper,
                                                   List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    public static List<RecordIdentity> filter(RecordHelper<UserMessageRecord> helper,
                                              UserRecord rec_user,
                                              UserMessageFilterRequest filters)
    {
        JoinHelper<Tuple> jh = new JoinHelper<>(helper, rec_user, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        jh.sort();

        //--//

        return JoinHelper.returnFilterTuples(helper, jh);
    }

    public static long count(RecordHelper<UserMessageRecord> helper,
                             UserRecord rec_user,
                             UserMessageFilterRequest filters)
    {
        JoinHelper<Tuple> jh = new JoinHelper<>(helper, rec_user, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        return jh.count();
    }
}
