/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.asset;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3KeyOrder;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.annotation.Optio3UpgradeValue;
import com.optio3.cloud.db.ICrossSessionIdentifier;
import com.optio3.cloud.hub.model.asset.AssetRelationship;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.persistence.AbstractSelectHelper;
import com.optio3.cloud.persistence.RawQueryHelper;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.collection.Memoizer;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.function.CallableWithoutException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.query.Query;

@Entity
@Table(name = "ASSET_RELATIONSHIP")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "Relationship", model = BaseModel.class, metamodel = RelationshipRecord_.class)
public class RelationshipRecord implements ICrossSessionIdentifier
{
    @Id
    @Optio3KeyOrder(1)
    @Optio3ControlNotifications(reason = "We want notifications to flow only from children to parent", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getParentAsset")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getParentAsset")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "parent", nullable = false, foreignKey = @ForeignKey(name = "ASSET_RELATIONSHIP__PARENT_ASSET__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private AssetRecord parentAsset;

    @Id
    @Optio3KeyOrder(3)
    @Optio3ControlNotifications(reason = "We want notifications to flow only from children to parent", direct = Notify.NEVER, reverse = Notify.NEVER, getter = "getChildAsset")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getChildAsset")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "child", nullable = false, foreignKey = @ForeignKey(name = "ASSET_RELATIONSHIP__CHILD_ASSET__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private AssetRecord childAsset;

    @Id
    @Optio3KeyOrder(2)
    @Optio3UpgradeValue("structural")
    @Enumerated(EnumType.STRING)
    @Column(name = "relation", nullable = false)
    private AssetRelationship relation = AssetRelationship.structural;

    public RelationshipRecord(AssetRecord parentAsset,
                              AssetRelationship relation,
                              AssetRecord childAsset)
    {
        this.parentAsset = parentAsset;
        this.relation    = relation;
        this.childAsset  = childAsset;
    }

    public RelationshipRecord()
    {
    }

    @Override
    public Serializable remapToSession(Session session)
    {
        AssetRecord parent = session.get(AssetRecord.class, parentAsset.getSysId());
        AssetRecord child  = session.get(AssetRecord.class, childAsset.getSysId());

        return new RelationshipRecord(parent, relation, child);
    }

    //--//

    public static class Raw
    {
        public String            parent;
        public String            child;
        public AssetRelationship relation;
    }

    public static RelationshipRecord addRelation(RecordHelper<?> helper,
                                                 AssetRecord rec_parent,
                                                 AssetRecord rec_child,
                                                 AssetRelationship relation)
    {
        return addRelation(helper, rec_parent, rec_child, relation, null);
    }

    public static RelationshipRecord addRelation(SessionHolder sessionHolder,
                                                 AssetRecord rec_parent,
                                                 AssetRecord rec_child,
                                                 AssetRelationship relation)
    {
        return addRelation(sessionHolder, rec_parent, rec_child, relation, null);
    }

    public static RelationshipRecord addRelation(RecordHelper<?> helper,
                                                 AssetRecord rec_parent,
                                                 AssetRecord rec_child,
                                                 AssetRelationship relation,
                                                 AtomicBoolean createdNew)
    {
        return addRelation(helper.currentSessionHolder(), rec_parent, rec_child, relation, createdNew);
    }

    public static RelationshipRecord addRelation(SessionHolder sessionHolder,
                                                 AssetRecord rec_parent,
                                                 AssetRecord rec_child,
                                                 AssetRelationship relation,
                                                 AtomicBoolean createdNew)
    {
        requireNonNull(rec_parent);
        requireNonNull(rec_child);
        requireNonNull(relation);

        if (SessionHolder.sameEntity(rec_parent, rec_child))
        {
            throw Exceptions.newIllegalArgumentException("Can't create relationship on self: %s", rec_parent.getSysId());
        }

        RelationshipRecord key = new RelationshipRecord(rec_parent, relation, rec_child);

        RecordHelper<RelationshipRecord> helper = sessionHolder.createHelper(RelationshipRecord.class);
        RelationshipRecord               rec    = helper.getOrNull(key);
        if (rec == null)
        {
            rec             = new RelationshipRecord();
            rec.parentAsset = requireNonNull(rec_parent);
            rec.childAsset  = requireNonNull(rec_child);
            rec.relation    = requireNonNull(relation);

            sessionHolder.persistEntity(rec);

            if (createdNew != null)
            {
                createdNew.set(true);
            }
        }

        return rec;
    }

    public static boolean removeRelation(SessionHolder sessionHolder,
                                         AssetRecord rec_parent,
                                         AssetRecord rec_child,
                                         AssetRelationship relation)
    {
        requireNonNull(rec_parent);
        requireNonNull(rec_child);
        requireNonNull(relation);

        RelationshipRecord key = new RelationshipRecord(rec_parent, relation, rec_child);

        RecordHelper<RelationshipRecord> helper = sessionHolder.createHelper(RelationshipRecord.class);
        RelationshipRecord               rec    = helper.getOrNull(key);
        if (rec == null)
        {
            return false;
        }

        helper.delete(rec);
        return true;
    }

    //--//

    public AssetRecord getParentAsset()
    {
        return parentAsset;
    }

    public AssetRecord getChildAsset()
    {
        return childAsset;
    }

    public AssetRelationship getRelation()
    {
        return relation;
    }

    //--//

    private static class JoinHelper extends AbstractSelectHelper<Tuple, AssetRecord>
    {
        JoinHelper(SessionHolder sessionHolder)
        {
            super(sessionHolder.createHelper(AssetRecord.class), Tuple.class);
        }
    }

    public static Map<AssetRelationship, Integer> count(SessionHolder sessionHolder)
    {
        JoinHelper               jh   = new JoinHelper(sessionHolder);
        Root<RelationshipRecord> root = jh.cq.from(RelationshipRecord.class);

        jh.cq.groupBy(root.get(RelationshipRecord_.relation));
        jh.cq.multiselect(root.get(RelationshipRecord_.relation), jh.cb.count(root));

        Map<AssetRelationship, Integer> map = Maps.newHashMap();

        for (Tuple t : jh.list())
        {
            AssetRelationship rel   = (AssetRelationship) t.get(0);
            Number            count = (Number) t.get(1);

            map.put(rel, count.intValue());
        }

        return map;
    }

    public static List<String> getParents(SessionHolder sessionHolder,
                                          String child,
                                          AssetRelationship relation)
    {
        JoinHelper               jh   = new JoinHelper(sessionHolder);
        Root<RelationshipRecord> root = jh.cq.from(RelationshipRecord.class);

        jh.addWhereReferencing(root, RelationshipRecord_.childAsset, child);

        jh.addWhereClauseWithEqual(root.get(RelationshipRecord_.relation), relation);

        jh.cq.multiselect(root.get(RelationshipRecord_.parentAsset)
                              .get(RecordWithCommonFields_.sysId));

        return CollectionUtils.transformToList(jh.list(), (t) -> (String) t.get(0));
    }

    public static List<String> getChildren(SessionHolder sessionHolder,
                                           String parent,
                                           AssetRelationship relation)
    {
        JoinHelper               jh   = new JoinHelper(sessionHolder);
        Root<RelationshipRecord> root = jh.cq.from(RelationshipRecord.class);

        jh.addWhereReferencing(root, RelationshipRecord_.parentAsset, parent);

        jh.addWhereClauseWithEqual(root.get(RelationshipRecord_.relation), relation);

        jh.cq.multiselect(root.get(RelationshipRecord_.childAsset)
                              .get(RecordWithCommonFields_.sysId));

        return CollectionUtils.transformToList(jh.list(), (t) -> (String) t.get(0));
    }

    public static Multimap<String, String> fetchAllMatchingRelations(SessionHolder sessionHolder,
                                                                     AssetRelationship relation)
    {

        CriteriaBuilder          cb   = sessionHolder.getCriteriaBuilder();
        CriteriaQuery<Tuple>     qdef = cb.createTupleQuery();
        Root<RelationshipRecord> root = qdef.from(RelationshipRecord.class);

        qdef.where(cb.equal(root.get(RelationshipRecord_.relation), relation));

        qdef.multiselect(root.get(RelationshipRecord_.parentAsset)
                             .get(RecordWithCommonFields_.sysId),
                         root.get(RelationshipRecord_.childAsset)
                             .get(RecordWithCommonFields_.sysId));

        Query<Tuple> query = sessionHolder.createQuery(qdef);
        query.setFetchSize(500);

        Multimap<String, String> map = ArrayListMultimap.create();

        try (ScrollableResults scroll = query.scroll(ScrollMode.FORWARD_ONLY))
        {
            while (scroll.next())
            {
                Tuple  row         = (Tuple) scroll.get(0);
                String parentSysId = row.get(0, String.class);
                String childSysId  = row.get(1, String.class);

                map.put(parentSysId, childSysId);
            }
        }

        return map;
    }

    public static List<Raw> fetchAllRelations(SessionHolder sessionHolder,
                                              Memoizer memoizer)
    {
        List<Raw> lst = Lists.newArrayList();

        streamAllRelations(sessionHolder, Raw::new, (model) ->
        {
            if (memoizer != null)
            {
                model.parent = memoizer.intern(model.parent);
                model.child  = memoizer.intern(model.child);
            }

            lst.add(model);
        });

        return lst;
    }

    public static void streamAllRelations(SessionHolder sessionHolder,
                                          CallableWithoutException<Raw> modelProducer,
                                          Consumer<Raw> modelConsumer)
    {
        RawQueryHelper<RelationshipRecord, Raw> qh = new RawQueryHelper<>(sessionHolder, RelationshipRecord.class);

        qh.addReferenceRaw(RelationshipRecord_.parentAsset, (obj, val) -> obj.parent = val);
        qh.addReferenceRaw(RelationshipRecord_.childAsset, (obj, val) -> obj.child = val);
        qh.addEnum(RelationshipRecord_.relation, AssetRelationship.class, (obj, val) -> obj.relation = val);

        qh.stream(modelProducer, modelConsumer);
    }
}
