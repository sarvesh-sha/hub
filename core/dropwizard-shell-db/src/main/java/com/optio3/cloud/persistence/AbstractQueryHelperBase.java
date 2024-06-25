/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import static java.util.Objects.requireNonNull;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.SingularAttribute;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;

public abstract class AbstractQueryHelperBase
{
    public static class ParsedLike
    {
        public String  query;
        public String  queryUnescaped;
        public boolean inverted;

        public static List<ParsedLike> decode(String likeFilter)
        {
            if (StringUtils.isBlank(likeFilter))
            {
                return null;
            }

            List<ParsedLike> lst = Lists.newArrayList();

            for (String s : StringUtils.split(likeFilter, ' '))
            {
                ParsedLike en = new ParsedLike();

                if (s.startsWith("!"))
                {
                    en.queryUnescaped = s.substring(1);
                    en.inverted       = true;
                }
                else
                {
                    en.queryUnescaped = s;
                }

                en.query = escape(en.queryUnescaped);

                lst.add(en);
            }

            return lst.isEmpty() ? null : lst;
        }

        private static String escape(String text)
        {
            return "%" + StringUtils.replace(text, "%", "\\%") + "%";
        }
    }

    public final CriteriaBuilder cb;

    private List<Predicate> m_whereClauses;
    private List<Order>     m_orders;
    private boolean         m_flushQueryPlanCache;

    protected AbstractQueryHelperBase(CriteriaBuilder cb)
    {
        this.cb = requireNonNull(cb);
    }

    protected void cleanupAfterQuery(SessionHolder sh)
    {
        if (m_flushQueryPlanCache)
        {
            sh.flushQueryPlanCache();
        }
    }

    protected abstract void cleanupAfterQuery();

    private void enforceCacheFlushIfNeeded(int size)
    {
        if (size > 400)
        {
            //
            // If the number of entries in the in clause is too large, Hibernate creates a monster Query Plan, leading to out of memory...
            // Flush the cache after the query.
            //
            m_flushQueryPlanCache = true;
        }
    }

    //--//

    public boolean hasOrderBy()
    {
        return m_orders != null;
    }

    public <X> void addOrderBy(Path<? extends X> path,
                               SingularAttribute<X, ?> attr,
                               boolean ascending)
    {
        Path<?> target = path.get(attr);

        if (m_orders == null)
        {
            m_orders = Lists.newArrayList();
        }

        m_orders.add(ascending ? cb.asc(target) : cb.desc(target));
    }

    protected Order[] getOrderPredicates()
    {
        if (m_orders == null)
        {
            return null;
        }

        Order[] array = new Order[m_orders.size()];
        m_orders.toArray(array);
        return array;
    }

    //--//

    public <X, Y> void addWhereClauseWithEqual(Path<? extends X> path,
                                               SingularAttribute<X, Y> attr,
                                               Y value)
    {
        addWhereClause(equal(path, attr, value));
    }

    public void addWhereClauseWithEqual(Expression<?> a,
                                        Expression<?> b)
    {
        addWhereClause(cb.equal(a, b));
    }

    public void addWhereClauseWithEqual(Expression<?> a,
                                        Object b)
    {
        addWhereClause(cb.equal(a, b));
    }

    public <X, Y> void addWhereClauseIsNull(Path<? extends X> path,
                                            SingularAttribute<X, Y> attr)
    {
        addWhereClause(isNull(path, attr));
    }

    public <X, Y> void addWhereClauseIn(Path<? extends X> path,
                                        SingularAttribute<X, Y> attr,
                                        Collection<Y> values)
    {
        if (values.isEmpty())
        {
            // Empty set means always false.
            addWhereClauseWithEqual(cb.literal(1), cb.literal(0));
        }
        else
        {
            enforceCacheFlushIfNeeded(values.size());

            addWhereClause(path.get(attr)
                               .in(values));
        }
    }

    public <X, Y> void addWhereClauseNotIn(Path<? extends X> path,
                                           SingularAttribute<X, Y> attr,
                                           Collection<Y> values)
    {
        if (values.isEmpty())
        {
            // Not of empty set is always true.
        }
        else
        {
            enforceCacheFlushIfNeeded(values.size());

            addWhereClause(path.get(attr)
                               .in(values)
                               .not());
        }
    }

    //--//

    public <X, Y extends RecordWithCommonFields> void addWhereReferencing(Path<? extends X> path,
                                                                          SingularAttribute<X, Y> attr,
                                                                          String sysId)
    {
        Path<Y> referencedRecord = path.get(attr);
        addWhereClause(cb.equal(referencedRecord.get(RecordWithCommonFields_.sysId), sysId));
    }

    public <X, Y extends RecordWithCommonFields> void addWhereReferencingSysIds(Path<? extends X> path,
                                                                                SingularAttribute<X, Y> attr,
                                                                                Collection<String> values)
    {
        if (values.isEmpty())
        {
            // Empty set means always false.
            addWhereClauseWithEqual(cb.literal(1), cb.literal(0));
        }
        else
        {
            enforceCacheFlushIfNeeded(values.size());

            Set<String> uniqueIDs = Sets.newHashSet(values);

            Path<Y> referencedEntity = path.get(attr);
            addWhereClause(referencedEntity.get(RecordWithCommonFields_.sysId)
                                           .in(uniqueIDs));
        }
    }

    public <X, Y extends RecordWithCommonFields> void addWhereNotReferencingSysIds(Path<? extends X> path,
                                                                                   SingularAttribute<X, Y> attr,
                                                                                   Collection<String> values)
    {
        if (values.isEmpty())
        {
            // Not of empty set is always true.
        }
        else
        {
            enforceCacheFlushIfNeeded(values.size());

            Set<String> uniqueIDs = Sets.newHashSet(values);

            Path<Y> referencedEntity = path.get(attr);
            addWhereClause(referencedEntity.get(RecordWithCommonFields_.sysId)
                                           .in(uniqueIDs)
                                           .not());
        }
    }

    public void addWhereClause(Predicate pred)
    {
        if (m_whereClauses == null)
        {
            m_whereClauses = Lists.newArrayList();
        }

        m_whereClauses.add(pred);
    }

    protected Predicate[] getWherePredicates()
    {
        if (m_whereClauses == null)
        {
            return null;
        }

        Predicate[] array = new Predicate[m_whereClauses.size()];
        m_whereClauses.toArray(array);
        return array;
    }

    /**
     * Adds conditions for column 'root.field' to be between rangeStart and rangeEnd.
     *
     * @param root       Target entity
     * @param field      Target Column
     * @param rangeStart Lower bound of time range
     * @param rangeEnd   Upper bound of time range
     * @param <R>        Type of entity
     */
    public <R> void filterTimestampsCoveredByTargetRange(Path<? extends R> root,
                                                         SingularAttribute<R, ZonedDateTime> field,
                                                         ZonedDateTime rangeStart,
                                                         ZonedDateTime rangeEnd)
    {
        if (rangeStart != null)
        {
            addWhereClause(greaterThanOrEqualTo(root, field, rangeStart));
        }

        if (rangeEnd != null)
        {
            addWhereClause(lessThanOrEqualTo(root, field, rangeEnd));
        }
    }

    /**
     * Adds conditions for column 'root.fieldStart' and 'root.fieldEnd' to overlap the time range [targetStart,targetEnd].
     *
     * @param root                Target entity
     * @param fieldStart          Target Column for start time of entity
     * @param fieldEnd            Target Column for end time of entity
     * @param targetStart         Lower bound of time range
     * @param targetEnd           Upper bound of time range
     * @param startBoundCanBeOpen If true, the start column can be null
     * @param endBoundCanBeOpen   If true, the end column can be null
     * @param endBoundInclusive   If true, a value equal to the end time field will match the range
     * @param <R>                 Type of entity
     */
    public <R> void filterTimeRangesCoveredByTargetRange(Path<? extends R> root,
                                                         SingularAttribute<R, ZonedDateTime> fieldStart,
                                                         SingularAttribute<R, ZonedDateTime> fieldEnd,
                                                         ZonedDateTime targetStart,
                                                         ZonedDateTime targetEnd,
                                                         boolean startBoundCanBeOpen,
                                                         boolean endBoundCanBeOpen,
                                                         boolean endBoundInclusive)
    {
        if (targetStart != null)
        {
            //
            // The end field should *not* be less than targetStart.
            //
            Predicate pred = not(endBoundInclusive ? lessThanOrEqualTo(root, fieldEnd, targetStart) : lessThan(root, fieldEnd, targetStart));

            if (endBoundCanBeOpen)
            {
                pred = or(pred, isNull(root, fieldEnd));
            }

            addWhereClause(pred);
        }

        if (targetEnd != null)
        {
            //
            // The start field should *not* be more than targetEnd.
            //
            Predicate pred = not(greaterThan(root, fieldStart, targetEnd));

            if (startBoundCanBeOpen)
            {
                pred = or(pred, isNull(root, fieldStart));
            }

            addWhereClause(pred);
        }
    }

    /**
     * Adds conditions for column 'root.fieldStart' and 'root.fieldEnd' to include the timestamp 'time'.
     *
     * @param root                Target entity
     * @param fieldStart          Target Column for start time of entity
     * @param fieldEnd            Target Column for end time of entity
     * @param time                Target timestamp
     * @param startBoundCanBeOpen If true, the start column can be null
     * @param endBoundCanBeOpen   If true, the end column can be null
     * @param endBoundInclusive   If true, a value equal to the end time field will match the range
     * @param <R>                 Type of entity
     */
    public <R> void filterTimeRangesCoveringTimestamp(Path<? extends R> root,
                                                      SingularAttribute<R, ZonedDateTime> fieldStart,
                                                      SingularAttribute<R, ZonedDateTime> fieldEnd,
                                                      ZonedDateTime time,
                                                      boolean startBoundCanBeOpen,
                                                      boolean endBoundCanBeOpen,
                                                      boolean endBoundInclusive)
    {
        if (time != null)
        {
            Predicate pred;

            //
            // Start field should be less than rangeStart or null.
            //
            pred = lessThanOrEqualTo(root, fieldStart, time);
            if (startBoundCanBeOpen)
            {
                pred = or(pred, isNull(root, fieldStart));
            }
            addWhereClause(pred);

            //
            // End field should be greater than rangeEnd or null.
            //
            pred = endBoundInclusive ? greaterThanOrEqualTo(root, fieldEnd, time) : greaterThan(root, fieldEnd, time);

            if (endBoundCanBeOpen)
            {
                pred = or(pred, isNull(root, fieldEnd));
            }

            addWhereClause(pred);
        }
    }

    //--//

    public <X, Y> Predicate isNull(Path<? extends X> path,
                                   SingularAttribute<X, Y> attr)
    {
        return cb.isNull(path.get(attr));
    }

    public <X, Y> Predicate isNotNull(Path<? extends X> path,
                                      SingularAttribute<X, Y> attr)
    {
        return cb.isNotNull(path.get(attr));
    }

    public <X, Y> Predicate equal(Path<? extends X> path,
                                  SingularAttribute<X, Y> attr,
                                  Y value)
    {
        return cb.equal(path.get(attr), value);
    }

    public <X, Y extends Comparable<? super Y>> Predicate greaterThan(Path<? extends X> path,
                                                                      SingularAttribute<X, Y> attr,
                                                                      Y value)
    {
        return cb.greaterThan(path.get(attr), value);
    }

    public <X, Y extends Comparable<? super Y>> Predicate greaterThanOrEqualTo(Path<? extends X> path,
                                                                               SingularAttribute<X, Y> attr,
                                                                               Y value)
    {
        return cb.greaterThanOrEqualTo(path.get(attr), value);
    }

    public <X, Y extends Comparable<? super Y>> Predicate lessThan(Path<? extends X> path,
                                                                   SingularAttribute<X, Y> attr,
                                                                   Y value)
    {
        return cb.lessThan(path.get(attr), value);
    }

    public <X, Y extends Comparable<? super Y>> Predicate lessThanOrEqualTo(Path<? extends X> path,
                                                                            SingularAttribute<X, Y> attr,
                                                                            Y value)
    {
        return cb.lessThanOrEqualTo(path.get(attr), value);
    }

    public <X> Predicate isLike(Path<? extends X> path,
                                SingularAttribute<X, String> attr,
                                String pattern)
    {
        return cb.like(path.get(attr), pattern);
    }

    public <X> Predicate isNotLike(Path<? extends X> path,
                                   SingularAttribute<X, String> attr,
                                   String pattern)
    {
        return cb.notLike(path.get(attr), pattern);
    }

    public <X> Predicate predicateForLike(Path<? extends X> path,
                                          SingularAttribute<X, String> attr,
                                          List<ParsedLike> likeFilters)
    {
        Predicate[] parts = new Predicate[likeFilters.size()];

        for (int i = 0; i < likeFilters.size(); i++)
        {
            ParsedLike likeFilter = likeFilters.get(i);

            if (likeFilter.inverted)
            {
                parts[i] = isNotLike(path, attr, likeFilter.query);
            }
            else
            {
                parts[i] = isLike(path, attr, likeFilter.query);
            }
        }

        return and(parts);
    }

    //--//

    public Predicate and(Predicate... restrictions)
    {
        return cb.and(restrictions);
    }

    public Predicate or(Predicate... restrictions)
    {
        return cb.or(restrictions);
    }

    public Predicate not(Predicate restriction)
    {
        return cb.not(restriction);
    }
}
