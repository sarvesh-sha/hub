/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import static java.util.Objects.requireNonNull;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.SingularAttribute;

import com.google.common.collect.Lists;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.CollectionUtils;
import com.optio3.util.function.BiConsumerWithException;
import com.optio3.util.function.CallableWithoutException;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.query.Query;

public class RawQueryHelper<TEntity, TModel> extends AbstractQueryHelperBase
{
    private static abstract class ColumnInfo<TModel>
    {
        int          index;
        Selection<?> handle;

        abstract void handle(Tuple row,
                             TModel obj);
    }

    private static class ColumnInfoForScalar<TScalar, TModel> extends ColumnInfo<TModel>
    {
        Class<TScalar>              clz;
        BiConsumer<TModel, TScalar> setter;

        @Override
        void handle(Tuple row,
                    TModel obj)
        {
            TScalar scalar = row.get(index, clz);

            setter.accept(obj, scalar);
        }
    }

    private static class ColumnInfoForEntityIdentityReference<T extends RecordWithCommonFields, TModel> extends ColumnInfo<TModel>
    {
        Class<T>                                   clz;
        BiConsumer<TModel, TypedRecordIdentity<T>> setter;

        @Override
        void handle(Tuple row,
                    TModel obj)
        {
            String sysId = row.get(index, String.class);

            setter.accept(obj, TypedRecordIdentity.newTypedInstance(clz, sysId));
        }
    }

    private static class ColumnInfoForEntityIdentityReferenceRaw<T extends RecordWithCommonFields, TModel> extends ColumnInfo<TModel>
    {
        BiConsumer<TModel, String> setter;

        @Override
        void handle(Tuple row,
                    TModel obj)
        {
            String sysId = row.get(index, String.class);

            setter.accept(obj, sysId);
        }
    }

    private static class ColumnInfoForEntityReference<T, TModel> extends ColumnInfo<TModel>
    {
        Class<T>              clz;
        BiConsumer<TModel, T> setter;

        @Override
        void handle(Tuple row,
                    TModel obj)
        {
            T value = row.get(index, clz);

            setter.accept(obj, value);
        }
    }

    private final SessionHolder        m_sessionHolder;
    private final CriteriaQuery<Tuple> m_qdef;
    public final  Root<TEntity>        root;

    private final Class<TEntity>           m_clzEntity;
    private final List<ColumnInfo<TModel>> m_columns = Lists.newArrayList();

    public RawQueryHelper(SessionHolder sessionHolder,
                          Class<TEntity> clzEntity)
    {
        super(requireNonNull(sessionHolder).getCriteriaBuilder());

        m_sessionHolder = sessionHolder;
        m_qdef          = cb.createTupleQuery();
        root            = m_qdef.from(clzEntity);

        m_clzEntity = clzEntity;
    }

    @Override
    protected void cleanupAfterQuery()
    {
        cleanupAfterQuery(m_sessionHolder);
    }

    public <T extends RecordWithCommonFields> void addReference(SingularAttribute<? super TEntity, T> attr,
                                                                Class<T> clz,
                                                                BiConsumer<TModel, TypedRecordIdentity<T>> setter)
    {
        ColumnInfoForEntityIdentityReference<T, TModel> ci = allocateColumn(new ColumnInfoForEntityIdentityReference<>());

        ci.handle = root.get(attr)
                        .get(RecordWithCommonFields_.sysId);

        ci.clz    = clz;
        ci.setter = setter;
    }

    public <T extends RecordWithCommonFields> void addReferenceRaw(SingularAttribute<? super TEntity, T> attr,
                                                                   BiConsumer<TModel, String> setter)
    {
        ColumnInfoForEntityIdentityReferenceRaw<T, TModel> ci = allocateColumn(new ColumnInfoForEntityIdentityReferenceRaw<>());

        ci.handle = root.get(attr)
                        .get(RecordWithCommonFields_.sysId);

        ci.setter = setter;
    }

    public <T extends RecordWithCommonFields, S> void addExpensiveDereference(SingularAttribute<? super TEntity, T> attr,
                                                                              SingularAttribute<? super T, S> attr2,
                                                                              Class<S> clz,
                                                                              BiConsumer<TModel, S> setter)
    {
        ColumnInfoForEntityReference<S, TModel> ci = allocateColumn(new ColumnInfoForEntityReference<>());

        ci.handle = root.join(attr, JoinType.LEFT)
                        .get(attr2);

        ci.clz    = clz;
        ci.setter = setter;
    }

    public <T> void addObject(SingularAttribute<? super TEntity, T> attr,
                              Class<T> clz,
                              BiConsumer<TModel, T> setter)
    {
        addScalar(attr, clz, setter);
    }

    public void addString(SingularAttribute<? super TEntity, String> attr,
                          BiConsumer<TModel, String> setter)
    {
        addScalar(attr, String.class, setter);
    }

    public <T> void addStringDeserializer(SingularAttribute<? super TEntity, String> attr,
                                          Class<T> clz,
                                          BiConsumerWithException<TModel, T> setter)
    {
        addString(attr, (obj, val) ->
        {
            if (StringUtils.isNotBlank(val))
            {
                try
                {
                    setter.accept(obj, ObjectMappers.SkipNulls.readValue(val, clz));
                }
                catch (Exception e)
                {
                    // Ignore failures.
                }
            }
        });
    }

    public void addBoolean(SingularAttribute<? super TEntity, Boolean> attr,
                           BiConsumer<TModel, Boolean> setter)
    {
        addScalar(attr, Boolean.class, setter);
    }

    public void addInteger(SingularAttribute<? super TEntity, Integer> attr,
                           BiConsumer<TModel, Integer> setter)
    {
        addScalar(attr, Integer.class, setter);
    }

    public void addLong(SingularAttribute<? super TEntity, Long> attr,
                        BiConsumer<TModel, Long> setter)
    {
        addScalar(attr, Long.class, setter);
    }

    public void addFloat(SingularAttribute<? super TEntity, Float> attr,
                         BiConsumer<TModel, Float> setter)
    {
        addScalar(attr, Float.class, setter);
    }

    public void addDouble(SingularAttribute<? super TEntity, Double> attr,
                          BiConsumer<TModel, Double> setter)
    {
        addScalar(attr, Double.class, setter);
    }

    public void addDate(SingularAttribute<? super TEntity, ZonedDateTime> attr,
                        BiConsumer<TModel, ZonedDateTime> setter)
    {
        addScalar(attr, ZonedDateTime.class, setter);
    }

    public <T extends Enum<T>> void addEnum(SingularAttribute<? super TEntity, T> attr,
                                            Class<T> clz,
                                            BiConsumer<TModel, T> setter)
    {
        addScalar(attr, clz, setter);
    }

    private <T> void addScalar(SingularAttribute<? super TEntity, T> attr,
                               Class<T> clz,
                               BiConsumer<TModel, T> setter)
    {
        ColumnInfoForScalar<T, TModel> ci = allocateColumn(new ColumnInfoForScalar<>());

        ci.handle = root.get(attr);
        ci.clz    = clz;
        ci.setter = setter;
    }

    public List<TModel> execute(CallableWithoutException<TModel> modelProducer)
    {
        List<TModel> res = Lists.newArrayList();

        stream(modelProducer, res::add);

        return res;
    }

    public void stream(CallableWithoutException<TModel> modelProducer,
                       Consumer<TModel> callback)
    {
        Predicate[] arrayWhere = getWherePredicates();
        if (arrayWhere != null)
        {
            m_qdef.where(arrayWhere);
        }

        Order[] arrayOrder = getOrderPredicates();
        if (arrayOrder != null)
        {
            m_qdef.orderBy(arrayOrder);
        }

        m_qdef.multiselect(CollectionUtils.transformToList(m_columns, (ci) -> ci.handle));

        Query<Tuple> query = m_sessionHolder.createQuery(m_qdef);
        query.setFetchSize(500);

        try (ScrollableResults scroll = query.scroll(ScrollMode.FORWARD_ONLY))
        {
            while (scroll.next())
            {
                Tuple  row = (Tuple) scroll.get(0);
                TModel obj = modelProducer.call();

                for (ColumnInfo<TModel> columnInfo : m_columns)
                {
                    columnInfo.handle(row, obj);
                }

                callback.accept(obj);
            }
        }
    }

    private <T extends ColumnInfo<TModel>> T allocateColumn(T ci)
    {
        ci.index = m_columns.size();
        m_columns.add(ci);
        return ci;
    }
}