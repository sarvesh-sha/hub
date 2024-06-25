/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.search;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.optio3.cloud.AbstractApplicationWithDatabase;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.PostCommitNotificationState;
import com.optio3.cloud.persistence.PostCommitNotificationStateField;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.collection.Memoizer;
import com.optio3.concurrency.AsyncMutex;
import com.optio3.logging.Logger;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.ngram.EdgeNGramFilterFactory;
import org.apache.lucene.analysis.ngram.NGramFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.standard.UAX29URLEmailTokenizerFactory;
import org.apache.lucene.search.Query;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.analyzer.definition.LuceneAnalysisDefinitionProvider;
import org.hibernate.search.analyzer.definition.LuceneAnalysisDefinitionRegistryBuilder;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.query.dsl.EntityContext;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.SimpleQueryStringMatchingContext;

public class HibernateSearch
{
    public static class Gate extends AbstractApplicationWithDatabase.GateClass
    {
    }

    public static class Provider
    {
        @Factory
        public LuceneAnalysisDefinitionProvider getAnalyzers()
        {
            return (LuceneAnalysisDefinitionRegistryBuilder builder) ->
            {
                builder.analyzer("prefix")
                       .tokenizer(StandardTokenizerFactory.class)
                       .tokenFilter(LowerCaseFilterFactory.class)
                       .tokenFilter(EdgeNGramFilterFactory.class)
                       .param("minGramSize", "1")
                       .param("maxGramSize", "128");

                builder.analyzer("prefix_email")
                       .tokenizer(UAX29URLEmailTokenizerFactory.class)
                       .tokenFilter(LowerCaseFilterFactory.class)
                       .tokenFilter(EdgeNGramFilterFactory.class)
                       .param("minGramSize", "1")
                       .param("maxGramSize", "128");

                builder.analyzer("prefix_query")
                       .tokenizer(StandardTokenizerFactory.class)
                       .tokenFilter(LowerCaseFilterFactory.class);

                builder.analyzer("fuzzy")
                       .tokenizer(StandardTokenizerFactory.class)
                       .tokenFilter(LowerCaseFilterFactory.class)
                       .tokenFilter(NGramFilterFactory.class)
                       .param("minGramSize", "1")
                       .param("maxGramSize", "128");

                builder.analyzer("fuzzy_query")
                       .tokenizer(StandardTokenizerFactory.class)
                       .tokenFilter(LowerCaseFilterFactory.class);
            };
        }
    }

    public static class ResultSet
    {
        private final Map<Class<?>, Results<?>> m_results = Maps.newHashMap();

        void setResults(Class<?> clz,
                        Results<?> results)
        {
            m_results.put(clz, results);
        }

        public <T extends RecordWithCommonFields> Results<T> getResults(Class<T> clz)
        {
            @SuppressWarnings("unchecked") Results<T> result = (Results<T>) m_results.get(clz);

            if (result == null)
            {
                return new Results<>();
            }

            return result;
        }
    }

    public static class Results<T extends RecordWithCommonFields> extends TypedRecordIdentityList<T>
    {
        public int totalResults;
    }

    public static class IndexingContext implements AutoCloseable
    {
        public IndexingContext(AbstractApplicationWithDatabase<?> app,
                               String databaseId)
        {
            final Map<Class<? extends HibernateIndexingContext>, HibernateIndexingContext> contextLookup = Maps.newHashMap();

            Set<Class<?>> processed = Sets.newHashSet();

            Memoizer memoizer = new Memoizer();

            for (Class<?> entityClass : app.getDataSourceEntities(databaseId))
            {
                processAnnotation(app, databaseId, memoizer, processed, contextLookup, entityClass);
            }

            s_indexingContext = contextLookup;
        }

        private void processAnnotation(AbstractApplicationWithDatabase<?> app,
                                       String databaseId,
                                       Memoizer memoizer,
                                       Set<Class<?>> processed,
                                       Map<Class<? extends HibernateIndexingContext>, HibernateIndexingContext> contextLookup,
                                       Class<?> entityClass)
        {
            Class<?> superClass = entityClass.getSuperclass();
            if (superClass != null)
            {
                processAnnotation(app, databaseId, memoizer, processed, contextLookup, superClass);
            }

            if (processed.add(entityClass))
            {
                Optio3HibernateSearchContext anno = entityClass.getAnnotation(Optio3HibernateSearchContext.class);
                if (anno != null)
                {
                    HibernateIndexingContext handler = Reflection.newInstance(anno.handler());
                    handler.initialize(app, databaseId, memoizer);
                    contextLookup.put(anno.handler(), handler);
                }
            }
        }

        @Override
        public void close()
        {
            s_indexingContext = null;
        }

        //--//

        public static <T extends HibernateIndexingContext> T get(Class<T> clz)
        {
            Map<Class<? extends HibernateIndexingContext>, HibernateIndexingContext> map = s_indexingContext;
            if (map != null)
            {
                return clz.cast(map.get(clz));
            }

            return null;
        }
    }

    //--//

    public static final Logger LoggerInstance = new Logger(HibernateSearch.class);

    private static final AsyncMutex                                                               s_indexingLock = new AsyncMutex();
    private static       Map<Class<? extends HibernateIndexingContext>, HibernateIndexingContext> s_indexingContext;

    private Map<String, String>        m_fieldAnalyzers  = Maps.newHashMap();
    private Set<Class<?>>              m_indexedEntities = Sets.newHashSet();
    private Multimap<Class<?>, String> m_indexedFields   = HashMultimap.create();

    //--//

    private HibernateSearch()
    {
    }

    //--//

    public static CompletableFuture<AsyncMutex.Holder> acquireLock()
    {
        return s_indexingLock.acquire();
    }

    public static CompletableFuture<HibernateSearch> startIndexing(SessionHolder sessionHolder)
    {
        CompletableFuture<HibernateSearch> indexingCompleted = new CompletableFuture<>();
        HibernateSearch                    search            = new HibernateSearch();

        FullTextSession fullTextSession = HibernateSearch.getCurrentFullTextSession(sessionHolder);

        MassIndexerProgressMonitor progressMonitor = new MassIndexerProgressMonitor()
        {
            private Stopwatch m_st = Stopwatch.createStarted();
            private MonotonousTime m_nextUpdate = TimeUtils.computeTimeoutExpiration(1, TimeUnit.SECONDS);
            private long m_documentsDoneCounter;
            private long m_totalCounter;

            @Override
            public void entitiesLoaded(int size)
            {
            }

            @Override
            public synchronized void documentsAdded(long increment)
            {
                m_documentsDoneCounter += increment;

                if (TimeUtils.isTimeoutExpired(m_nextUpdate))
                {
                    long elapsedMs = m_st.elapsed(TimeUnit.MILLISECONDS);

                    LoggerInstance.info("%d documents indexed in %d ms", m_documentsDoneCounter, elapsedMs);
                    int estimateSpeed              = (int) (m_documentsDoneCounter * 1000.0F / elapsedMs);
                    int estimatePercentileComplete = (int) (m_documentsDoneCounter * 100.0F / m_totalCounter);

                    LoggerInstance.info("Indexing speed: %d documents/second; progress: %d%%", estimateSpeed, estimatePercentileComplete);

                    m_nextUpdate = TimeUtils.computeTimeoutExpiration(m_documentsDoneCounter < 2000 ? 1 : 5, TimeUnit.SECONDS);
                }
            }

            @Override
            public void documentsBuilt(int number)
            {
            }

            @Override
            public synchronized void addToTotalCount(long count)
            {
                m_totalCounter += count;

                LoggerInstance.info("Going to reindex %d entities", count);
            }

            @Override
            public synchronized void indexingCompleted()
            {
                LoggerInstance.info("Completed indexing for Hibernate Search.");
                search.configure(fullTextSession);
                indexingCompleted.complete(search);

                LoggerInstance.info("%d documents indexed in %d ms", m_totalCounter, m_st.elapsed(TimeUnit.MILLISECONDS));
            }
        };

        fullTextSession.createIndexer()
                       .progressMonitor(progressMonitor)
                       .start();

        return indexingCompleted;
    }

    //--//

    public ResultSet queryAll(SessionHolder sessionHolder,
                              String searchText,
                              int offset,
                              int limit)
    {
        return query(sessionHolder, RecordWithCommonFields.class, searchText, offset, limit);
    }

    public <T extends RecordWithCommonFields> ResultSet query(SessionHolder sessionHolder,
                                                              Class<T> clz,
                                                              String searchText,
                                                              int offset,
                                                              int limit)
    {
        return query(sessionHolder, clz, searchText, offset, limit, null);
    }

    public <T extends RecordWithCommonFields> ResultSet query(SessionHolder sessionHolder,
                                                              Class<T> clz,
                                                              String searchText,
                                                              int offset,
                                                              int limit,
                                                              Function<QueryBuilder, Query> subQueryBuilder)
    {
        List<String>    fields          = Lists.newArrayList(m_fieldAnalyzers.keySet());
        FullTextSession fullTextSession = HibernateSearch.getCurrentFullTextSession(sessionHolder);
        EntityContext entityContext = fullTextSession.getSearchFactory()
                                                     .buildQueryBuilder()
                                                     .forEntity(clz);

        for (String field : m_fieldAnalyzers.keySet())
        {
            if (!m_fieldAnalyzers.get(field)
                                 .isEmpty())
            {
                entityContext.overridesForField(field, m_fieldAnalyzers.get(field));
            }
        }

        QueryBuilder qb = entityContext.get();

        SimpleQueryStringMatchingContext matchingContext = qb.simpleQueryString()
                                                             .onFields(CollectionUtils.firstElement(fields));
        for (int i = 1; i < fields.size(); i++)
        {
            matchingContext.andField(fields.get(i));
        }

        Query query = matchingContext.withAndAsDefaultOperator()
                                     .matching(searchText)
                                     .createQuery();

        if (subQueryBuilder != null)
        {
            Query subQuery = subQueryBuilder.apply(qb);

            if (subQuery != null)
            {
                query = qb.bool()
                          .must(query)
                          .must(subQuery)
                          .createQuery();
            }
        }

        Set<Class<?>> classesToQuery;
        if (clz == RecordWithCommonFields.class)
        {
            classesToQuery = m_indexedEntities;
        }
        else
        {
            classesToQuery = Sets.newHashSet(clz);
        }

        ResultSet resultSet = new ResultSet();

        for (Class<?> classToQuery : classesToQuery)
        {
            Results<?> searchResults = getSearchResults(fullTextSession, query, offset, limit, classToQuery);
            resultSet.setResults(classToQuery, searchResults);
        }

        return resultSet;
    }

    private <T extends RecordWithCommonFields> Results<T> getSearchResults(FullTextSession session,
                                                                           Query query,
                                                                           int offset,
                                                                           int limit,
                                                                           Class<?> clz)
    {
        FullTextQuery queryForClass = session.createFullTextQuery(query, clz);
        int           resultSize    = queryForClass.getResultSize();
        Results<T>    searchResults = new Results<>();
        searchResults.totalResults = resultSize;

        if (offset < resultSize)
        {
            queryForClass.setFirstResult(offset);
            if (limit > 0)
            {
                queryForClass.setMaxResults(limit);
            }

            queryForClass.setProjection(ProjectionConstants.ID, ProjectionConstants.SCORE);

            @SuppressWarnings("unchecked") List<Object[]> rawResults = queryForClass.getResultList();
            @SuppressWarnings("unchecked") Class<T>       clzT       = (Class<T>) clz;

            for (Object[] rawResult : rawResults)
            {
                TypedRecordIdentity<T> ri = TypedRecordIdentity.newTypedInstance(clzT, (String) rawResult[0]);
                searchResults.add(ri);
            }
        }

        return searchResults;
    }

    private void configure(FullTextSession session)
    {
        m_indexedEntities = session.getSearchFactory()
                                   .getIndexedTypes();

        for (Class<?> clz : m_indexedEntities)
        {
            collectFields(clz);
        }
    }

    public boolean changeIncludesIndexedClass(Class<?> clz)
    {
        return !getIndexedFields(clz).isEmpty();
    }

    public boolean changeIncludesIndexedField(Class<?> clz,
                                              PostCommitNotificationState state)
    {
        Collection<String> indexedFields = getIndexedFields(clz);

        for (PostCommitNotificationStateField field : state.fields)
        {
            if (field.dirty)
            {
                if (indexedFields.contains(field.name))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private Collection<String> getIndexedFields(Class<?> clz)
    {
        while (clz != null)
        {
            Collection<String> indexedFields = m_indexedFields.get(clz);
            if (!indexedFields.isEmpty())
            {
                return indexedFields;
            }

            clz = clz.getSuperclass();
        }

        return Collections.emptySet();
    }

    private static FullTextSession getCurrentFullTextSession(SessionHolder sessionHolder)
    {
        return sessionHolder.getFullTextSession();
    }

    private void collectFields(Class<?> clz)
    {
        if (m_indexedFields.containsKey(clz))
        {
            // already configured
            return;
        }

        Optio3QueryAnalyzerOverride queryAnalyzerOverride = clz.getAnnotation(Optio3QueryAnalyzerOverride.class);
        String                      analyzerOverride      = queryAnalyzerOverride != null ? queryAnalyzerOverride.value() : "";

        for (Field f : Reflection.collectFields(clz)
                                 .values())
        {
            org.hibernate.search.annotations.Field searchField = f.getAnnotation(org.hibernate.search.annotations.Field.class);
            if (searchField != null)
            {
                String annotationName = searchField.name();
                String fieldName      = !annotationName.isEmpty() ? annotationName : f.getName();
                m_fieldAnalyzers.put(fieldName, analyzerOverride);
                m_indexedFields.put(clz, f.getName());
            }

            IndexedEmbedded embedded = f.getAnnotation(IndexedEmbedded.class);
            if (embedded != null)
            {
                throw Exceptions.newIllegalArgumentException("Discovered usage of @IndexedEmbedded on %s, use @Field on a synthetic property!", f);
            }
        }

        for (Method m : Reflection.collectMethods(clz)
                                  .values())
        {
            org.hibernate.search.annotations.Field searchField = m.getAnnotation(org.hibernate.search.annotations.Field.class);
            if (searchField != null)
            {
                String annotationName = searchField.name();
                String fieldName      = !annotationName.isEmpty() ? annotationName : getFieldName(m.getName());
                m_fieldAnalyzers.put(fieldName, analyzerOverride);
            }

            IndexedEmbedded embedded = m.getAnnotation(IndexedEmbedded.class);
            if (embedded != null)
            {
                throw Exceptions.newIllegalArgumentException("Discovered usage of @IndexedEmbedded on %s, use @Field on a synthetic property!", m);
            }
        }
    }

    private static String getFieldName(String methodName)
    {
        if (methodName.startsWith("get"))
        {
            String name      = methodName.substring(3);
            String firstChar = name.charAt(0) + "";
            return name.replaceFirst(firstChar, firstChar.toLowerCase());
        }
        return methodName;
    }
}
