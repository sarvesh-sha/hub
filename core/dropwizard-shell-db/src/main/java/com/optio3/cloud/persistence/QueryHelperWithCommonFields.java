/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.persistence.Tuple;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.SingularAttribute;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.collection.Batcher;
import com.optio3.util.function.FunctionWithException;

public class QueryHelperWithCommonFields<T, R extends RecordWithCommonFields> extends AbstractSelectHelper<T, R>
{
    protected abstract static class PostQueryFilterExtension
    {
        public abstract boolean shouldInclude(RecordIdentity ri);
    }

    protected abstract static class SortExtension<T2>
    {
        private int offset;

        public abstract Path<T2> getPath();

        public abstract void processValue(RecordIdentity ri,
                                          T2 value);

        public abstract void processResults(List<RecordIdentity> results);

        private void processValueUntyped(RecordIdentity ri,
                                         Object value)
        {
            @SuppressWarnings("unchecked") T2 t = (T2) value;

            processValue(ri, t);
        }
    }

    public final Root<R> root;

    private List<PostQueryFilterExtension> m_postQueryFilterExtensions;

    private List<SortExtension<?>> m_sortExtensions = Collections.emptyList();

    public QueryHelperWithCommonFields(RecordHelper<R> helper,
                                       Class<T> clz)
    {
        super(helper, clz);

        root = cq.from(helper.getEntityClass());
    }

    //--//

    protected void addPostQueryFilterExtension(PostQueryFilterExtension ext)
    {
        if (m_postQueryFilterExtensions == null)
        {
            m_postQueryFilterExtensions = Lists.newArrayList();
        }

        m_postQueryFilterExtensions.add(ext);
    }

    protected <T2> void addSortExtension(SortExtension<T2> ext)
    {
        if (m_sortExtensions.isEmpty())
        {
            m_sortExtensions = Lists.newArrayList();
        }

        ext.offset = 2 + m_sortExtensions.size();
        m_sortExtensions.add(ext);
    }

    public static <C extends RecordWithCommonFields> List<RecordIdentity> returnFilterTuples(RecordHelper<C> helper,
                                                                                             QueryHelperWithCommonFields<Tuple, C> jh)
    {
        List<RecordIdentity> res = Lists.newArrayList();

        if (!jh.hasOrderBy())
        {
            jh.addOrderBy(jh.root, RecordWithCommonFields_.createdOn, false);
        }

        List<Selection<?>> columns = Lists.newArrayList();
        columns.add(jh.root.get(RecordWithCommonFields_.sysId));
        columns.add(jh.root.get(RecordWithCommonFields_.updatedOn));

        for (SortExtension<?> extension : jh.m_sortExtensions)
        {
            columns.add(extension.getPath());
        }

        jh.cq.multiselect(columns);

        for (Tuple t : jh.list())
        {
            RecordIdentity ri = RecordIdentity.newInstance(helper, t, 0, 1);

            boolean include = true;

            if (jh.m_postQueryFilterExtensions != null)
            {
                for (PostQueryFilterExtension extension : jh.m_postQueryFilterExtensions)
                {
                    if (!extension.shouldInclude(ri))
                    {
                        include = false;
                        break;
                    }
                }
            }

            if (include)
            {
                res.add(ri);

                for (SortExtension<?> extension : jh.m_sortExtensions)
                {
                    extension.processValueUntyped(ri, t.get(extension.offset));
                }
            }
        }

        for (SortExtension<?> extension : jh.m_sortExtensions)
        {
            extension.processResults(res);
        }

        return res;
    }

    //--//

    public static <R2 extends RecordWithCommonFields, Q extends QueryHelperWithCommonFields<Tuple, R2>> StreamHelperResult stream(boolean batchStream,
                                                                                                                                  Q jh,
                                                                                                                                  FunctionWithException<R2, StreamHelperNextAction> callbackResults) throws
                                                                                                                                                                                                     Exception
    {
        return stream(batchStream, -1, jh, callbackResults);
    }

    public static <R2 extends RecordWithCommonFields, Q extends QueryHelperWithCommonFields<Tuple, R2>> StreamHelperResult stream(boolean batchStream,
                                                                                                                                  int maxResults,
                                                                                                                                  Q jh,
                                                                                                                                  FunctionWithException<R2, StreamHelperNextAction> callbackResults) throws
                                                                                                                                                                                                     Exception
    {
        List<String> res = Lists.newArrayList();

        jh.cq.multiselect(jh.root.get(RecordWithCommonFields_.sysId));

        for (Tuple t : jh.list(maxResults))
            res.add((String) t.get(0));

        int batchSize = batchStream ? 100 : 1;

        for (int offset = 0; offset < res.size(); offset += batchSize)
        {
            int count = Math.min(batchSize, res.size() - offset);

            List<R2>               entities        = getBatch(jh.helper, res, offset, count);
            List<R2>               entitiesToEvict = Lists.newArrayList();
            AtomicBoolean          mustFlush       = new AtomicBoolean(false);
            StreamHelperNextAction nextAction      = null;

            for (int index = 0; index < entities.size(); index++)
            {
                R2 entity = entities.get(index);
                if (entity == null)
                {
                    continue;
                }

                if (nextAction != null)
                {
                    // If we have to stop, we better evict the pre-cached entities...
                    entitiesToEvict.add(entity);
                }
                else
                {
                    nextAction = callbackResults.apply(entity);
                    switch (nextAction)
                    {
                        case Continue:
                            nextAction = null;
                            break;

                        case Continue_Evict:
                            entitiesToEvict.add(entity);
                            nextAction = null;
                            break;

                        case Continue_Flush_Evict:
                            mustFlush.set(true);
                            entitiesToEvict.add(entity);
                            nextAction = null;
                            break;

                        case Continue_Flush_Evict_Commit:
                            mustFlush.set(true);
                            entitiesToEvict.add(entity);

                            jh.flushState(entitiesToEvict, mustFlush, true);
                            nextAction = null;
                            break;

                        case Stop:
                            break;

                        case Stop_Evict:
                            entitiesToEvict.add(entity);
                            break;
                    }
                }
            }

            jh.flushState(entitiesToEvict, mustFlush, false);

            if (nextAction != null)
            {
                return StreamHelperResult.Stopped;
            }
        }

        return StreamHelperResult.ProcessedAllRecords;
    }

    //--//

    public static <R2 extends RecordWithCommonFields, Q extends QueryHelperWithCommonFields<R2, R2>> StreamHelperResult streamNoNesting(int maxResults,
                                                                                                                                        Q jh,
                                                                                                                                        FunctionWithException<R2, StreamHelperNextAction> callbackResults) throws
                                                                                                                                                                                                           Exception
    {
        jh.cq.select(jh.root);

        try (var scroll = jh.scroll(maxResults))
        {
            List<R2>      entitiesToEvict = Lists.newArrayList();
            AtomicBoolean mustFlush       = new AtomicBoolean(false);

            try
            {
                while (scroll.next())
                {
                    R2 entity = scroll.get(0);
                    if (entity == null)
                    {
                        continue;
                    }

                    StreamHelperNextAction nextAction = callbackResults.apply(entity);
                    switch (nextAction)
                    {
                        case Continue:
                            break;

                        case Continue_Evict:
                            entitiesToEvict.add(entity);
                            break;

                        case Continue_Flush_Evict:
                            mustFlush.set(true);
                            entitiesToEvict.add(entity);
                            break;

                        case Stop:
                            return StreamHelperResult.Stopped;

                        case Stop_Evict:
                            entitiesToEvict.add(entity);
                            return StreamHelperResult.Stopped;

                        case Continue_Flush_Evict_Commit:
                            throw new IllegalArgumentException("FlushEvictAndCommit not supported for streamNoNesting!");
                    }

                    if (entitiesToEvict.size() > 100)
                    {
                        jh.flushState(entitiesToEvict, mustFlush, false);
                    }
                }

                return StreamHelperResult.ProcessedAllRecords;
            }
            finally
            {
                jh.flushState(entitiesToEvict, mustFlush, false);
            }
        }
    }

    protected void flushState(List<R> entitiesToEvict,
                              AtomicBoolean mustFlush,
                              boolean commit)
    {
        if (mustFlush.getAndSet(false))
        {
            helper.flush();
        }
        else
        {
            commit = false;
        }

        for (R entityToEvict : entitiesToEvict)
        {
            helper.evict(entityToEvict);
        }

        entitiesToEvict.clear();

        if (commit)
        {
            helper.currentSessionHolder()
                  .commitAndBeginNewTransaction();
        }
    }

    //--//

    public static <R2 extends RecordWithCommonFields> R2 getFirstMatch(RecordHelper<R2> helper,
                                                                       Consumer<QueryHelperWithCommonFields<R2, R2>> callback)
    {
        final QueryHelperWithCommonFields<R2, R2> jh = prepareQueryForEntity(helper);
        if (callback != null)
        {
            callback.accept(jh);
        }

        return jh.getFirstResultOrNull();
    }

    public static <R2 extends RecordWithCommonFields> List<R2> filter(RecordHelper<R2> helper,
                                                                      Consumer<QueryHelperWithCommonFields<R2, R2>> callback)
    {
        QueryHelperWithCommonFields<R2, R2> jh = prepareQueryForEntity(helper);
        if (callback != null)
        {
            callback.accept(jh);
        }

        return jh.list();
    }

    public static <R2 extends RecordWithCommonFields> QueryHelperWithCommonFields<R2, R2> prepareQueryForEntity(RecordHelper<R2> helper)
    {
        requireNonNull(helper);

        QueryHelperWithCommonFields<R2, R2> jh = new QueryHelperWithCommonFields<>(helper, helper.getEntityClass());

        //--//

        jh.cq.select(jh.root);

        return jh;
    }

    //--//

    public static <R2 extends RecordWithCommonFields> List<String> listRaw(RecordHelper<R2> helper)
    {
        return listRaw(helper, null);
    }

    public static <R2 extends RecordWithCommonFields> List<String> listRaw(RecordHelper<R2> helper,
                                                                           Consumer<QueryHelperWithCommonFields<Tuple, R2>> callback)
    {
        requireNonNull(helper);

        QueryHelperWithCommonFields<Tuple, R2> jh = prepareQueryForSysId(helper);

        //--//

        List<String> res = Lists.newArrayList();

        if (callback != null)
        {
            callback.accept(jh);
        }

        for (Tuple t : jh.list())
            res.add((String) t.get(0));

        return res;
    }

    public static <R2 extends RecordWithCommonFields> QueryHelperWithCommonFields<Tuple, R2> prepareQueryForSysId(RecordHelper<R2> helper)
    {
        requireNonNull(helper);

        QueryHelperWithCommonFields<Tuple, R2> jh = new QueryHelperWithCommonFields<>(helper, Tuple.class);

        //--//

        jh.cq.multiselect(jh.root.get(RecordWithCommonFields_.sysId));

        return jh;
    }

    //--//

    public static <R2 extends RecordWithCommonFields> TypedRecordIdentityList<R2> list(RecordHelper<R2> helper)
    {
        return list(helper, null);
    }

    public static <R2 extends RecordWithCommonFields> TypedRecordIdentityList<R2> list(RecordHelper<R2> helper,
                                                                                       Consumer<QueryHelperWithCommonFields<Tuple, R2>> callback)
    {
        return list(helper, 0, callback);
    }

    public static <R2 extends RecordWithCommonFields> TypedRecordIdentityList<R2> list(RecordHelper<R2> helper,
                                                                                       int maxResults,
                                                                                       Consumer<QueryHelperWithCommonFields<Tuple, R2>> callback)
    {
        requireNonNull(helper);

        QueryHelperWithCommonFields<Tuple, R2> jh = new QueryHelperWithCommonFields<>(helper, Tuple.class);

        //--//

        TypedRecordIdentityList<R2> res = new TypedRecordIdentityList<>();

        jh.cq.multiselect(jh.root.get(RecordWithCommonFields_.sysId), jh.root.get(RecordWithCommonFields_.updatedOn));

        if (callback != null)
        {
            callback.accept(jh);
        }

        for (Tuple t : jh.list(maxResults))
        {
            res.add(RecordIdentity.newInstance(helper, t, 0, 1));
        }

        return res;
    }

    public static <R2 extends RecordWithCommonFields> TypedRecordIdentity<R2> single(RecordHelper<R2> helper,
                                                                                     Consumer<QueryHelperWithCommonFields<Tuple, R2>> callback)
    {
        requireNonNull(helper);

        QueryHelperWithCommonFields<Tuple, R2> jh = new QueryHelperWithCommonFields<>(helper, Tuple.class);

        //--//

        jh.cq.multiselect(jh.root.get(RecordWithCommonFields_.sysId), jh.root.get(RecordWithCommonFields_.updatedOn));

        if (callback != null)
        {
            callback.accept(jh);
        }

        for (Tuple t : jh.list(1))
        {
            return RecordIdentity.newInstance(helper, t, 0, 1);
        }

        return null;
    }

    //--//

    public static <R2 extends RecordWithCommonFields> List<R2> getBatch(RecordHelper<R2> helper,
                                                                        TypedRecordIdentityList<R2> ids)
    {
        return getBatch(helper, ids, 0, ids.size());
    }

    public static <R2 extends RecordWithCommonFields> List<R2> getBatch(RecordHelper<R2> helper,
                                                                        TypedRecordIdentityList<R2> ids,
                                                                        int offset,
                                                                        int count)
    {
        int offsetEnd = Math.min(offset + count, ids.size());

        List<String> idsRaw = Lists.newArrayList();

        for (int cursor = offset; cursor < offsetEnd; cursor++)
        {
            idsRaw.add(ids.get(cursor).sysId);
        }

        return getBatch(helper, idsRaw, 0, idsRaw.size());
    }

    public static <R2 extends RecordWithCommonFields> List<R2> getBatch(RecordHelper<R2> helper,
                                                                        List<String> ids)
    {
        return getBatch(helper, ids, 0, ids.size());
    }

    public static <R2 extends RecordWithCommonFields> List<R2> getBatch(RecordHelper<R2> helper,
                                                                        List<String> ids,
                                                                        int offset,
                                                                        int count)
    {
        int offsetEnd = Math.min(offset + count, ids.size());

        Map<String, R2> mapResults = Maps.newHashMap();

        for (R2 res : getBatchOutOfOrder(helper, ids, offset, count))
            mapResults.put(res.getSysId(), res);

        List<R2> outputList = Lists.newArrayList();

        for (int cursor = offset; cursor < offsetEnd; cursor++)
        {
            String id  = ids.get(cursor);
            R2     val = mapResults.get(id);
            outputList.add(val);
        }

        return outputList;
    }

    public static <R2 extends RecordWithCommonFields> List<R2> getBatchOutOfOrder(RecordHelper<R2> helper,
                                                                                  List<String> ids,
                                                                                  int offset,
                                                                                  int count)
    {
        requireNonNull(helper);

        return Batcher.splitInBatches(ids, offset, count, 100, (slice) ->
        {
            QueryHelperWithCommonFields<R2, R2> jh = new QueryHelperWithCommonFields<>(helper, helper.getEntityClass());

            jh.cq.select(jh.root);
            jh.cq.where(jh.root.get(RecordWithCommonFields_.sysId)
                               .in(slice));

            return jh.list();
        });
    }

    //--//

    public static <R2 extends RecordWithCommonFields, X, K> Map<K, Number> countByField(RecordHelper<R2> helper,
                                                                                        Path<X> keyPath,
                                                                                        SingularAttribute<X, K> keyAttr,
                                                                                        Consumer<QueryHelperWithCommonFields<Tuple, R2>> callback)
    {
        QueryHelperWithCommonFields<Tuple, R2> jh = new QueryHelperWithCommonFields<>(helper, Tuple.class);

        //--//

        if (callback != null)
        {
            callback.accept(jh);
        }

        return jh.countByField(keyPath, keyAttr);
    }

    public <X, K> Map<K, Number> countByField(Path<X> keyPath,
                                              SingularAttribute<? super X, K> keyAttr)
    {
        Expression<?> path = keyPath.get(keyAttr);

        cq.multiselect(path, cb.count(root));
        cq.groupBy(path);

        Map<K, Number> res = Maps.newHashMap();

        for (T row : list())
        {
            Tuple t = (Tuple) row;

            @SuppressWarnings("unchecked") K key = (K) t.get(0);

            res.put(key, (Number) t.get(1));
        }

        return res;
    }

    //--//

    public static <R2 extends RecordWithCommonFields> long count(RecordHelper<R2> helper,
                                                                 Consumer<QueryHelperWithCommonFields<Tuple, R2>> callback)
    {
        QueryHelperWithCommonFields<Tuple, R2> jh = new QueryHelperWithCommonFields<>(helper, Tuple.class);

        //--//

        if (callback != null)
        {
            callback.accept(jh);
        }

        return jh.count();
    }

    public long count()
    {
        cq.multiselect(cb.count(root));

        Tuple t = (Tuple) getSingleResult();

        cleanupAfterQuery(helper.currentSessionHolder());

        Number res = (Number) t.get(0);

        return res != null ? res.longValue() : -1;
    }
}