/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.dashboard;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.hub.model.AccessControlListPolicy;
import com.optio3.cloud.hub.model.dashboard.DashboardDefinition;
import com.optio3.cloud.hub.model.dashboard.DashboardDefinitionVersion;
import com.optio3.cloud.hub.persistence.acl.RecordWithAccessControlList;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RawQueryHelper;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.logging.Logger;
import com.optio3.logging.LoggerFactory;
import com.optio3.util.TimeUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "DASHBOARD_DEFINITION")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "DashboardDefinition", model = DashboardDefinition.class, metamodel = DashboardDefinitionRecord_.class)
public class DashboardDefinitionRecord extends RecordWithAccessControlList implements ModelMapperTarget<DashboardDefinition, DashboardDefinitionRecord_>
{
    public static class PruneHistory
    {
    }

    public static final Logger LoggerInstance = new Logger(PruneHistory.class);

    //--//

    @Optio3ControlNotifications(reason = "Notify user of changes", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getUser")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getUser")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "user", foreignKey = @ForeignKey(name = "DASHBOARD_DEFINITION__USER__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private UserRecord user;

    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getHeadVersion")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "head_version", foreignKey = @ForeignKey(name = "DASHBOARD_DEFINITION__HEAD_VERSION__FK"))
    private DashboardDefinitionVersionRecord headVersion;

    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getReleaseVersion")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "release_version", foreignKey = @ForeignKey(name = "DASHBOARD_DEFINITION__RELEASE_VERSION__FK"))
    private DashboardDefinitionVersionRecord releaseVersion;

    @OneToMany(mappedBy = "definition", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<DashboardDefinitionVersionRecord> versions;

    public DashboardDefinitionRecord()
    {
    }

    //--//

    public UserRecord getUser()
    {
        return user;
    }

    public boolean setUser(UserRecord user)
    {
        // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
        if (SessionHolder.sameEntity(this.user, user))
        {
            return false; // Nothing changed.
        }

        this.user = user;
        return true;
    }

    public DashboardDefinitionVersionRecord getHeadVersion()
    {
        return headVersion;
    }

    public boolean setHeadVersion(DashboardDefinitionVersionRecord headVersion)
    {
        if (SessionHolder.sameEntity(this.headVersion, headVersion))
        {
            return false; // Nothing changed.
        }

        this.headVersion = headVersion;
        return true;
    }

    public DashboardDefinitionVersionRecord getReleaseVersion()
    {
        return releaseVersion;
    }

    public boolean setReleaseVersion(DashboardDefinitionVersionRecord releaseVersion)
    {
        if (SessionHolder.sameEntity(this.releaseVersion, releaseVersion))
        {
            return false; // Nothing changed.
        }

        this.releaseVersion = releaseVersion;
        return true;
    }

    public List<DashboardDefinitionVersionRecord> getVersions()
    {
        return versions;
    }

    //--//

    public static TypedRecordIdentityList<DashboardDefinitionRecord> getAllByUser(RecordHelper<DashboardDefinitionRecord> helper,
                                                                                  UserRecord user)
    {
        return QueryHelperWithCommonFields.list(helper, (qh) ->
        {
            qh.addWhereClauseWithEqual(qh.root, DashboardDefinitionRecord_.user, user);
        });
    }

    public static List<DashboardDefinitionRecord> getBatch(RecordHelper<DashboardDefinitionRecord> helper,
                                                           List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    //--//

    public List<DashboardDefinitionVersion> getHistory(SessionHolder sessionHolder)
    {
        RawQueryHelper<DashboardDefinitionVersionRecord, DashboardDefinitionVersion> qh = new RawQueryHelper<>(sessionHolder, DashboardDefinitionVersionRecord.class);

        qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);
        qh.addDate(RecordWithCommonFields_.createdOn, (obj, val) -> obj.createdOn = val);
        qh.addInteger(DashboardDefinitionVersionRecord_.version, (obj, val) -> obj.version = val);
        qh.addReference(DashboardDefinitionVersionRecord_.predecessor, DashboardDefinitionVersionRecord.class, (obj, val) -> obj.predecessor = val);

        qh.addWhereClauseWithEqual(qh.root.get(DashboardDefinitionVersionRecord_.definition), this);

        List<DashboardDefinitionVersion> versions = qh.execute(DashboardDefinitionVersion::new);
        versions.sort(Comparator.comparing(a -> a.version));
        return versions;
    }

    public boolean pruneHistory(SessionHolder sessionHolder,
                                List<DashboardDefinitionVersion> lst,
                                int max,
                                Duration minGap)
    {
        boolean pruned = false;

        if (lst.size() >= max)
        {
            Map<String, DashboardDefinitionVersion> lookup = Maps.newHashMap();
            Set<String>                             keep   = Sets.newHashSet();

            // Sort from newest to oldest.
            lst = Lists.newArrayList(lst);
            lst.sort(Comparator.comparing(a -> -a.version));

            for (DashboardDefinitionVersion model : lst)
            {
                lookup.put(model.sysId, model);

                //
                // Keep all the recent edits.
                //
                if (TimeUtils.wasUpdatedRecently(model.createdOn, 2, TimeUnit.DAYS))
                {
                    keep.add(model.sysId);
                }
            }

            // Increase the limit by the number of versions we need to keep.
            max += keep.size();

            //
            // Keep the roots.
            //
            markAsKeep(keep, getReleaseVersion());
            markAsKeep(keep, getHeadVersion());

            while (true)
            {
                int keepCount = keep.size();

                for (String sysId : Lists.newArrayList(keep))
                {
                    //
                    // For each version to keep, keep the predecessors that are older than the minimum gap, to skip interim edits.
                    //
                    DashboardDefinitionVersion start = lookup.get(sysId);
                    prunePredecessorChain(lookup, keep, start, start, minGap);
                }

                if (keep.size() == keepCount)
                {
                    // No progress, exit.
                    break;
                }

                if (keep.size() >= max)
                {
                    // Collected enough versions, exit.
                    break;
                }
            }

            if (keep.size() != lst.size())
            {
                try (var ignored = LoggerFactory.indent(String.format("[%s - %s] ", getUser().getEmailAddress(), getHeadVersion().getDetails().title)))
                {
                    RecordHelper<DashboardDefinitionVersionRecord> helper = sessionHolder.createHelper(DashboardDefinitionVersionRecord.class);
                    for (DashboardDefinitionVersion model : lst)
                    {
                        if (!keep.contains(model.sysId))
                        {
                            redirectToPredecessor(helper, lookup, keep, model);
                            pruned = true;

                            LoggerInstance.info("Deleting  v%d (%s)", model.version, model.createdOn);

                            DashboardDefinitionVersionRecord rec_toDelete = helper.get(model.sysId);
                            helper.delete(rec_toDelete);
                        }
                        else
                        {
                            LoggerInstance.info("Keeping   v%d (%s)", model.version, model.createdOn);
                        }
                    }
                }
            }
        }

        return pruned;
    }

    private static void markAsKeep(Set<String> keep,
                                   DashboardDefinitionVersionRecord rec)
    {
        if (rec != null)
        {
            keep.add(rec.getSysId());
        }
    }

    private static void prunePredecessorChain(Map<String, DashboardDefinitionVersion> lookup,
                                              Set<String> keep,
                                              DashboardDefinitionVersion root,
                                              DashboardDefinitionVersion current,
                                              Duration minGap)
    {
        var pred = getPredecessor(lookup, current);
        if (pred != null)
        {
            Duration diff = Duration.between(pred.createdOn, root.createdOn);
            if (Math.abs(diff.toMinutes()) > minGap.toMinutes())
            {
                keep.add(pred.sysId);
            }
            else if (!keep.contains(pred.sysId))
            {
                prunePredecessorChain(lookup, keep, root, pred, minGap);
            }
        }
    }

    private static void redirectToPredecessor(RecordHelper<DashboardDefinitionVersionRecord> helper,
                                              Map<String, DashboardDefinitionVersion> lookup,
                                              Set<String> keep,
                                              DashboardDefinitionVersion toDelete)
    {
        //
        // Find the first predecessor to keep.
        //
        DashboardDefinitionVersion pred = getPredecessor(lookup, toDelete);
        while (pred != null && !keep.contains(pred.sysId))
        {
            pred = getPredecessor(lookup, pred);
        }

        DashboardDefinitionVersionRecord rec_pred = pred != null ? helper.get(pred.sysId) : null;

        for (DashboardDefinitionVersion succ : lookup.values())
        {
            if (keep.contains(succ.sysId) && getPredecessor(lookup, succ) == toDelete)
            {
                if (pred != null)
                {
                    LoggerInstance.info("Relinking v%d (%s) to v%d (%s)", succ.version, succ.createdOn, pred.version, pred.createdOn);
                }
                else
                {
                    LoggerInstance.info("Relinking v%d (%s) to nothing", succ.version, succ.createdOn);
                }

                DashboardDefinitionVersionRecord rec_succ = helper.get(succ.sysId);
                rec_succ.setPredecessor(rec_pred);
            }
        }
    }

    private static DashboardDefinitionVersion getPredecessor(Map<String, DashboardDefinitionVersion> lookup,
                                                             DashboardDefinitionVersion obj)
    {
        return obj.predecessor != null ? lookup.get(obj.predecessor.sysId) : null;
    }

    //--//

    public void remove(ValidationResultsHolder validation,
                       RecordHelper<DashboardDefinitionRecord> helper) throws
                                                                       Exception
    {
        if (validation.canProceed())
        {
            removeInner(validation, helper);
        }
    }

    private void removeInner(ValidationResultsHolder validation,
                             RecordHelper<DashboardDefinitionRecord> helper)
    {
        setReleaseVersion(null);
        setHeadVersion(null);

        for (DashboardDefinitionVersionRecord rec_child : Lists.newArrayList(versions))
        {
            rec_child.remove(validation, helper.wrapFor(DashboardDefinitionVersionRecord.class));
        }

        helper.delete(this);
    }

    @Override
    protected AccessControlListPolicy computeEffectiveAccessControlList(SessionHolder sessionHolder,
                                                                        AccessControlListPolicy policy)
    {
        // Nothing to do.
        return policy;
    }
}
