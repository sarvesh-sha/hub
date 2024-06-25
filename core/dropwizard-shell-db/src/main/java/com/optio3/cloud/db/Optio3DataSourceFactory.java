/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.db;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.io.File;
import java.io.Serializable;
import java.lang.management.ThreadInfo;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.persistence.Table;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Stopwatch;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.AbstractApplication;
import com.optio3.cloud.AbstractApplicationWithDatabase;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.persistence.PostCommitNotificationReason;
import com.optio3.cloud.persistence.PostCommitNotificationState;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.search.HibernateSearch;
import com.optio3.cloud.search.NoOpWorker;
import com.optio3.concurrency.AsyncMutex;
import com.optio3.concurrency.AsyncSynchronization;
import com.optio3.concurrency.Executors;
import com.optio3.concurrency.LongRunningThreadPool;
import com.optio3.concurrency.RateLimiter;
import com.optio3.logging.Logger;
import com.optio3.util.Exceptions;
import com.optio3.util.FileSystem;
import com.optio3.util.MonotonousTime;
import com.optio3.util.StackTraceAnalyzer;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.BiConsumerWithException;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.util.Duration;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.common.util.ProxyHelper;
import org.hibernate.cfg.Environment;

public class Optio3DataSourceFactory extends DataSourceFactory
{
    public static final Logger LoggerInstance = AbstractApplication.LoggerInstance.createSubLogger(Optio3DataSourceFactory.class);

    static class JdbcUrlParser
    {
        private String m_url;

        JdbcUrlParser(String url)
        {
            m_url = url;

            String prefix = getNextPart(':', false);
            if (!StringUtils.equalsIgnoreCase(prefix, "jdbc"))
            {
                m_url = null;
            }
        }

        String getNextPart(char delimiter,
                           boolean consumeRestIfMissing)
        {
            if (m_url == null)
            {
                return null;
            }

            int pos = m_url.indexOf(delimiter);
            if (pos >= 0)
            {
                String res = m_url.substring(0, pos);

                if (m_url.length() <= pos + 1)
                {
                    m_url = null;
                }
                else
                {
                    m_url = m_url.substring(pos + 1);
                }

                return res;
            }

            if (consumeRestIfMissing)
            {
                String res = m_url;
                m_url = null;
                return res;
            }

            return null;
        }
    }

    class ProxyForManagedDataSource implements InvocationHandler
    {
        private final ManagedDataSource m_dataSource;

        ProxyForManagedDataSource(ManagedDataSource dataSource)
        {
            m_dataSource = dataSource;
        }

        @Override
        public Object invoke(Object proxy,
                             Method method,
                             Object[] args) throws
                                            Throwable
        {
            Class<?> declaringClass = method.getDeclaringClass();

            // Call methods on Object class directly.
            if (Object.class == declaringClass)
            {
                return method.invoke(this, args);
            }

            switch (method.getName())
            {
                // javax.sql.PooledConnection#getConnection()
                case "getConnection":
                {
                    for (int retries = 10; true; retries--)
                    {
                        try
                        {
                            notifyStartDataSource(m_dataSource);
                            Connection res = (Connection) method.invoke(m_dataSource, args);
                            return proxyForConnection(res);
                        }
                        catch (Throwable t)
                        {
                            // Failed to get the connection, revert notifications.
                            notifyStopDataSource(m_dataSource);

                            Throwable t2 = Exceptions.unwrapException(t);

                            boolean couldRetry = false;

                            if (t2 instanceof java.sql.SQLNonTransientConnectionException)
                            {
                                couldRetry = retries > 0;
                            }

                            if (couldRetry)
                            {
                                LoggerInstance.debug("getConnection failed: %s", t2);
                                dumpOpenConnections(LoggerInstance::debug);
                            }
                            else
                            {
                                LoggerInstance.warn("getConnection failed: %s", t2);
                                dumpOpenConnections(LoggerInstance::warn);
                            }

                            // On a database connection issue, sleep to avoid connection storm.
                            Executors.safeSleep(1000);

                            if (couldRetry)
                            {
                                continue;
                            }

                            throw t;
                        }
                    }
                }

                // io.dropwizard.lifecycle.Managed#start()
                case "start":
                {
                    try
                    {
                        notifyStartDataSource(m_dataSource);
                        return method.invoke(m_dataSource, args);
                    }
                    catch (Throwable t)
                    {
                        // Failed, revert notifications.
                        notifyStopDataSource(m_dataSource);
                        throw t;
                    }
                }

                // io.dropwizard.lifecycle.Managed#stop()
                case "stop":
                {
                    notifyStopDataSource(m_dataSource);
                    return method.invoke(m_dataSource, args);
                }

                default:
                    return method.invoke(m_dataSource, args);
            }
        }
    }

    class ProxyForConnection implements InvocationHandler
    {
        private final Connection          m_conn;
        private       boolean             m_resetAutoCommit;
        private       StackTraceElement[] m_openStackTrace;

        ProxyForConnection(Connection conn) throws
                                            SQLException
        {
            m_conn = conn;
            conn.setAutoCommit(false);

            if (trackConnectionClients)
            {
                m_openStackTrace = Thread.currentThread()
                                         .getStackTrace();
            }

            notifyOpenConnection(this);
        }

        @Override
        public Object invoke(Object proxy,
                             Method method,
                             Object[] args) throws
                                            Throwable
        {
            Class<?> declaringClass = method.getDeclaringClass();

            // Call methods on Object class directly.
            if (Object.class == declaringClass)
            {
                return method.invoke(this, args);
            }

            switch (method.getName())
            {
                // java.sql.Connection#close()
                case "close":
                    if (m_resetAutoCommit)
                    {
                        m_conn.setAutoCommit(false);
                        m_resetAutoCommit = false;
                    }

                    m_conn.rollback();
                    break;

                // java.sql.Connection#setAutoCommit(boolean autoCommit)
                case "setAutoCommit":
                    m_resetAutoCommit = true;
                    break;
            }

            Object res = method.invoke(m_conn, args);

            switch (method.getName())
            {
                // java.sql.Connection#close()
                case "close":
                    notifyCloseConnection(this);
                    return res;

                default:
                    return res;
            }
        }
    }

    //
    // We have to segregate the code in a separate class because HibernateSearch is in the startup path for the app,
    // which causes non-instrumented build (i.e. under IntelliJ or plain Maven) to fail with this error:
    //
    // Failed to transform class com.optio3.cloud.search.HibernateSearch due to exception class redefinition failed: attempted to add a method
    // java.lang.UnsupportedOperationException: class redefinition failed: attempted to add a method
    //
    class IndexingHelper
    {
        private final AsyncSynchronization m_wakeup = new AsyncSynchronization();

        private CompletableFuture<HibernateSearch>                         m_hibernateSearchReady = new CompletableFuture<>();
        private HibernateSearch                                            m_hibernateSearch;
        private boolean                                                    m_shutdown;
        private int                                                        m_rebuildVersion;
        private int                                                        m_rebuildVersionActual;
        private CompletableFuture<Void>                                    m_worker;
        private AbstractApplicationWithDatabase.DatabaseChangeRegistration m_changeRegistration;

        private IndexingHelper()
        {
        }

        void start() throws
                     Exception
        {
            synchronized (this)
            {
                if (m_worker == null && StringUtils.isNotEmpty(hibernateSearchIndexLocation))
                {
                    m_changeRegistration = m_app.registerLocalDatabaseChangeNotification(m_databaseId, null, this::handleDatabaseChange);

                    m_worker = worker();
                }
            }
        }

        void stop() throws
                    Exception
        {
            synchronized (this)
            {
                if (m_changeRegistration != null)
                {
                    m_changeRegistration.close();
                    m_changeRegistration = null;
                }

                if (m_worker != null)
                {
                    m_shutdown = true;
                    m_wakeup.signal();

                    m_worker.get();
                    m_worker = null;
                }
            }
        }

        private CompletableFuture<Void> worker() throws
                                                 Exception
        {
            m_rebuildVersion++;

            await(m_wakeup.condition(hibernateSearchIndexingDelay, TimeUnit.SECONDS));

            boolean firstTime = true;

            LoggerInstance.debug("Indexing worker startup");
            while (!m_shutdown)
            {
                try
                {
                    MonotonousTime forcedRebuild = TimeUtils.computeTimeoutExpiration(hibernateSearchForceRebuildingDelay, TimeUnit.SECONDS);

                    while (true)
                    {
                        int rebuildVersion = m_rebuildVersion;

                        boolean detectedActivity = await(m_wakeup.condition(hibernateSearchIdleRebuildingDelay, TimeUnit.SECONDS));

                        if (rebuildVersion != m_rebuildVersion)
                        {
                            detectedActivity = true;
                        }

                        if (m_shutdown)
                        {
                            break;
                        }

                        if (!firstTime) // Don't honor the suspension request on the first indexing.
                        {
                            if (m_app.shouldSuspendSearchIndexer())
                            {
                                // The indexer has been suspended, wait.
                                continue;
                            }
                        }

                        if (!detectedActivity)
                        {
                            break;
                        }

                        if (TimeUtils.isTimeoutExpired(forcedRebuild))
                        {
                            LoggerInstance.debug("Detected some DB activity but past forced deadline, reindexing...");
                            break;
                        }

                        LoggerInstance.debug("Detected some DB activity, sleeping...");
                    }

                    if (m_shutdown)
                    {
                        break;
                    }

                    LoggerInstance.debug("Checking gate...");

                    // Don't index until the gate opens.
                    await(m_app.getGate(HibernateSearch.Gate.class)
                               .getWaiter());

                    LoggerInstance.debug("Gate was open");

                    firstTime = false;

                    if (m_hibernateSearchReady.isDone())
                    {
                        m_hibernateSearchReady = new CompletableFuture<>();
                    }

                    int rebuildVersionActual = m_rebuildVersion;

                    try (AsyncMutex.Holder ignored = await(HibernateSearch.acquireLock()))
                    {
                        HibernateSearch.LoggerInstance.info("Starting indexing for Hibernate Search...");

                        try (HibernateSearch.IndexingContext ctx = new HibernateSearch.IndexingContext(m_app, m_databaseId))
                        {
                            CompletableFuture<HibernateSearch> newSearch;

                            HibernateSearch.LoggerInstance.info("Completed context pre-fetching...");

                            try (SessionHolder holder = SessionHolder.createWithNewReadOnlySession(m_app, m_databaseId, Optio3DbRateLimiter.System))
                            {
                                newSearch = HibernateSearch.startIndexing(holder);
                            }

                            m_hibernateSearch = await(newSearch);
                            m_hibernateSearchReady.complete(m_hibernateSearch);
                            m_rebuildVersionActual = rebuildVersionActual;
                        }
                    }

                    LoggerInstance.debug("Indexing done (version %d)", rebuildVersionActual);

                    if (m_shutdown)
                    {
                        break;
                    }

                    await(m_wakeup.condition());
                    LoggerInstance.debug("Indexing worker was woken up");
                }
                catch (Throwable t)
                {
                    LoggerInstance.error("Indexing failed due to %s", t);

                    await(m_wakeup.condition(10, TimeUnit.SECONDS));
                }
            }

            return wrapAsync(null);
        }

        private void handleDatabaseChange(String databaseId,
                                          Object entity,
                                          Serializable id,
                                          PostCommitNotificationReason action,
                                          PostCommitNotificationState state)
        {
            HibernateSearch search = m_hibernateSearch;
            if (search != null)
            {
                Class<?> clz = SessionHolder.getClassOfEntity(entity);
                switch (action)
                {
                    case INSERT:
                    case DELETE:
                        if (search.changeIncludesIndexedClass(clz))
                        {
                            LoggerInstance.debug("Wake up Indexer due to '%s:%s:%s'", action, RecordHelper.getEntityTable(clz), id);
                            triggerRebuild();
                        }
                        break;

                    case UPDATE:
                        if (search.changeIncludesIndexedField(clz, state))
                        {
                            LoggerInstance.debug("Wake up Indexer due to '%s:%s:%s'", action, RecordHelper.getEntityTable(clz), id);
                            triggerRebuild();
                        }
                        break;
                }
            }
        }

        private void triggerRebuild()
        {
            synchronized (this)
            {
                m_rebuildVersion++;
                m_wakeup.signal();
            }
        }
    }

    //--//

    /**
     * If true, the system will register for database events.
     */
    @JsonProperty
    public boolean enableEvents;

    //--//

    /**
     * If true, the system will keep track of the stack traces of all connection clients.
     */
    @JsonProperty
    public boolean trackConnectionClients;

    /**
     * If true, Hibernate will be configured to automatically update the schema.
     */
    public boolean autoHibernateMode;

    /**
     * If true, Liquibase will not migrate the database schema.
     */
    public boolean skipMigration;

    /**
     * If set, use Liquibase to generate new changesets and save it to this directory.
     */
    @JsonIgnore
    public String saveMigrationDelta;

    /**
     * When generating new changesets, this will be the target revision level.
     */
    @JsonIgnore
    public int migrationsRevisionLevel = -1;

    /**
     * When generating new changesets, this will be the target version.
     */
    @JsonIgnore
    public int migrationsVersionNumber = -1;

    /**
     * When generating new changesets, this will be the common ID for them.
     */
    @JsonIgnore
    public String migrationsRoot;

    //--//

    /**
     * If true, the system will attempt to compact the database if it grows too large.
     */
    @JsonProperty
    public boolean enableCompaction = true;

    /**
     * For file-based databases, this is the size threshold before attempting a compaction.
     */
    @JsonProperty
    public long compactionThreshold = 100 * 1024 * 1024;

    /**
     * The number of seconds the database has to be idle before attempting a compaction.
     */
    @JsonProperty
    public int idleTimeBeforeCompaction = 30; // seconds

    /**
     * Whether to enable hibernate search or not
     */
    @JsonProperty
    public String hibernateSearchIndexLocation;

    /**
     * The number of seconds to wait before creating the search index.
     */
    @JsonProperty
    public int hibernateSearchIndexingDelay = 30; // seconds

    /**
     * The number of seconds to wait while idle before rebuilding the search index.
     */
    @JsonProperty
    public int hibernateSearchIdleRebuildingDelay = 10; // seconds

    /**
     * The number of minutes to wait before forcing a rebuild of the search index.
     */
    @JsonProperty
    public int hibernateSearchForceRebuildingDelay = 2 * 60; // seconds

    //--//

    /**
     * If set to true, the system will print SQL statements to the console.
     */
    @JsonProperty
    public boolean showSql;

    /**
     * If set to true, the system will format the SQL statements in the console.
     */
    @JsonProperty
    public boolean formatSql;

    /**
     * If set to true, the system will count the number of records in each entity on startup.
     */
    @JsonProperty
    public boolean countRecords;

    //--//

    private final Object                  m_lock                   = new Object();
    private final Set<ProxyForConnection> m_outstandingConnections = Sets.newHashSet();

    private final Set<ManagedDataSource> m_activeManagedDataSources = Sets.newHashSet();

    private AbstractApplicationWithDatabase<?> m_app;
    private String                             m_databaseId;
    private String                             m_databaseFlavor;
    private File                               m_databaseFile;
    private CompletableFuture<Void>            m_compactionActive;
    private ZonedDateTime                      m_compactionTriggered;

    private IndexingHelper m_indexingHelper;

    private       ScheduledFuture<?> m_defragmentHandler;
    private final Set<Class<?>>      m_defragmentQueue = Sets.newHashSet();

    private final Supplier<RateLimiter> m_limiter = Suppliers.memoize(() -> new RateLimiter(getMaxSize()));
    private       MonotonousTime        m_poolExhaustedReport;

    public Optio3DataSourceFactory()
    {
        // the maximum amount of time to wait on an empty pool before throwing an exception
        setMaxWaitForConnection(Duration.seconds(30));

        // the SQL query to run when validating a connection's liveness
        setValidationQuery("/* MyService Health Check */ SELECT 1");

        // the timeout before a connection validation queries fail
        setValidationQueryTimeout(Duration.seconds(3));

        // the number of connections to open at startup
        setInitialSize(2);

        // the minimum number of connections to keep open
        setMinSize(0);

        // the maximum number of connections to keep open
        setMaxSize(32);

        // whether or not idle connections should be validated
        setCheckConnectionWhileIdle(true);

        // the amount of time to sleep between runs of the idle connection validation, abandoned cleaner and idle pool resizing
        setEvictionInterval(Duration.seconds(5));

        // the minimum amount of time an connection must sit idle in the pool before it is eligible for eviction
        setMinIdleTime(Duration.seconds(10));
    }

    @Override
    public ManagedDataSource build(MetricRegistry metricRegistry,
                                   String name)
    {
        return proxyForManagedDataSource(super.build(metricRegistry, name));
    }

    public RateLimiter.Holder acquirePermit(Optio3DbRateLimiter limiter,
                                            int wait,
                                            TimeUnit unit)
    {
        RateLimiter rateLimiter     = m_limiter.get();
        int         requiredPermits = limiter.getRequiredPermits(rateLimiter.getMaxPermits());

        RateLimiter.Holder holder = rateLimiter.acquire(requiredPermits, wait, unit);
        if (!holder.wasAcquired())
        {
            if (TimeUtils.isTimeoutExpired(m_poolExhaustedReport))
            {
                m_poolExhaustedReport = TimeUtils.computeTimeoutExpiration(1, TimeUnit.HOURS);

                Map<StackTraceAnalyzer, List<ThreadInfo>> uniqueStackTraces = StackTraceAnalyzer.allThreadInfos();
                List<String>                              lines             = StackTraceAnalyzer.printThreadInfos(true, uniqueStackTraces);

                LoggerInstance.warn("Failed to acquire %s DB connection (%,d in use, %,d waiting)...", limiter, rateLimiter.getConsumedPermits(), rateLimiter.getQueueDepth());
                for (String line : lines)
                {
                    LoggerInstance.warn(line);
                }
            }
        }

        return holder;
    }

    //--//

    public CompletableFuture<HibernateSearch> getHibernateSearch()
    {
        return m_indexingHelper.m_hibernateSearchReady;
    }

    public int getHibernateSearchVersion()
    {
        return m_indexingHelper.m_rebuildVersionActual;
    }

    public void startIndexing() throws
                                Exception
    {
        if (m_indexingHelper == null)
        {
            m_indexingHelper = new IndexingHelper();

            m_indexingHelper.start();
        }
    }

    public void stopIndexing() throws
                               Exception
    {
        if (m_indexingHelper != null)
        {
            m_indexingHelper.stop();
            m_indexingHelper = null;
        }
    }

    public void finalizeConfiguration(AbstractApplicationWithDatabase<?> app,
                                      String databaseName)
    {
        m_app        = app;
        m_databaseId = databaseName;

        Map<String, String> props = getProperties();

        props.put("charSet", "UTF-8");
        props.putIfAbsent(org.hibernate.cfg.AvailableSettings.GENERATE_STATISTICS, "false");
        props.put(org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, "true");
        props.put(org.hibernate.cfg.AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true");
        props.put(org.hibernate.cfg.AvailableSettings.SHOW_SQL, Boolean.toString(showSql));
        props.put(org.hibernate.cfg.AvailableSettings.FORMAT_SQL, Boolean.toString(formatSql));

        // When running sessions without transaction, by default Hibernate tries to close the JDBC connection, which causes lots of unnecessary ROLLBACKs.
        props.put(org.hibernate.cfg.AvailableSettings.CONNECTION_HANDLING, org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode.IMMEDIATE_ACQUISITION_AND_HOLD.name());

        props.put(Environment.STATEMENT_BATCH_SIZE, "100");

        final boolean enableSearch = hibernateSearchIndexLocation != null;
        props.put(org.hibernate.search.cfg.Environment.AUTOREGISTER_LISTENERS, enableSearch ? "true" : "false");

        if (enableSearch)
        {
            props.put(org.hibernate.search.cfg.Environment.WORKER_SCOPE, NoOpWorker.class.getTypeName());
            props.put(org.hibernate.search.cfg.Environment.ANALYSIS_DEFINITION_PROVIDER, HibernateSearch.Provider.class.getTypeName());
            props.put("hibernate.search.default.indexBase", hibernateSearchIndexLocation);
        }
    }

    public void queueDefragmentationAtBoot(Collection<Class<?>> entityClasses)
    {
        if (entityClasses != null)
        {
            boolean shouldWait = false;

            for (Class<?> clz : entityClasses)
            {
                Optio3TableInfo anno = clz.getAnnotation(Optio3TableInfo.class);
                if (anno != null && anno.defragmentOnBoot())
                {
                    queueDefragmentation(clz);
                    shouldWait = true;
                }
            }

            if (shouldWait)
            {
                try
                {
                    waitForDefragmentation().get();
                }
                catch (Throwable t)
                {
                    // Ignore failures.
                }
            }
        }
    }

    public void queueDefragmentation(Class<?> clz)
    {
        if (skipMigration)
        {
            // We skip migration during testing, no need to defragment database.
            return;
        }

        synchronized (m_defragmentQueue)
        {
            while (clz != null)
            {
                m_defragmentQueue.add(clz);

                clz = clz.getSuperclass();
            }

            if (m_defragmentHandler == null)
            {
                m_defragmentHandler = Executors.scheduleOnDefaultPool((Runnable) this::processDefragmentation, 5, TimeUnit.SECONDS);
            }
        }
    }

    public Future<?> waitForDefragmentation()
    {
        synchronized (m_defragmentQueue)
        {
            return m_defragmentHandler != null ? m_defragmentHandler : AsyncRuntime.NullResult;
        }
    }

    private void processDefragmentation()
    {
        while (true)
        {
            List<Class<?>> workList;

            synchronized (m_defragmentQueue)
            {
                if (m_defragmentQueue.isEmpty())
                {
                    m_defragmentHandler = null;
                    return;
                }

                workList = Lists.newArrayList(m_defragmentQueue);
                m_defragmentQueue.clear();
            }

            try
            {
                ManagedDataSource dataSource = build(null, "Defragmentation Worker");
                try
                {
                    LoggerInstance.info("Starting defragmentation...");
                    try (Connection connRaw = dataSource.getConnection())
                    {
                        for (Class<?> clz : workList)
                        {
                            processDefragmentation(connRaw, clz);
                        }
                    }
                    LoggerInstance.info("Completed defragmentation.");
                }
                finally
                {
                    dataSource.stop();
                }
            }
            catch (Exception e)
            {
                // Ignore exceptions.
                LoggerInstance.error("Defragment failed with error: %s", e);
            }
        }
    }

    private void processDefragmentation(Connection conn,
                                        Class<?> clz) throws
                                                      Exception
    {
        Table tbl = clz.getAnnotation(Table.class);
        if (tbl != null)
        {
            switch (extractDatabaseFlavor())
            {
                case "mysql":
                case "mariadb":
                    try (Statement stmt = conn.createStatement())
                    {
                        Stopwatch st = Stopwatch.createStarted();
                        stmt.executeUpdate("OPTIMIZE TABLE " + tbl.name());
                        st.stop();
                        LoggerInstance.info("Optimized table %s in %smsec", tbl.name(), st.elapsed(TimeUnit.MILLISECONDS));
                    }
                    break;
            }
        }
    }

    //--//

    public String extractDatabaseFlavor()
    {
        if (m_databaseFlavor != null)
        {
            return m_databaseFlavor;
        }

        //
        // Extract the database flavor from JDBC string.
        //
        JdbcUrlParser parser   = new JdbcUrlParser(getUrl());
        String        database = parser.getNextPart(':', false);

        m_databaseFlavor = database != null ? database.toLowerCase() : null;
        return m_databaseFlavor;
    }

    public File extractDatabaseFile()
    {
        if (m_databaseFile != null)
        {
            return m_databaseFile;
        }

        //
        // Extract the database file from JDBC string.
        //
        JdbcUrlParser parser   = new JdbcUrlParser(getUrl());
        String        database = parser.getNextPart(':', false);

        switch (database)
        {
            case "h2":
                String prefix = parser.getNextPart(':', false);
                if (StringUtils.equalsIgnoreCase(prefix, "mem"))
                {
                    return null;
                }

                String file = parser.getNextPart(';', true);
                m_databaseFile = new File(file + ".mv.db");
                return m_databaseFile;

            default:
                return null;
        }
    }

    public void dropDatabase() throws
                               Exception
    {
        ManagedDataSource dataSource = build(null, "Drop Schema");
        try
        {
            dropDatabase(dataSource);
        }
        finally
        {
            dataSource.stop();
        }
    }

    public void dropDatabase(ManagedDataSource dataSource) throws
                                                           SQLException
    {
        try (Connection connRaw = dataSource.getConnection())
        {
            try (Statement stmt = connRaw.createStatement())
            {
                switch (extractDatabaseFlavor())
                {
                    case "mysql":
                    case "mariadb":
                        stmt.execute("DROP DATABASE " + connRaw.getCatalog());
                        break;

                    case "h2":
                        stmt.execute("DROP ALL OBJECTS");
                        break;
                }
            }
        }
    }

    //--//

    public boolean waitForNoConnections(long timeout,
                                        TimeUnit unit)
    {
        MonotonousTime timeoutExpiration = TimeUtils.computeTimeoutExpiration(timeout, unit);

        synchronized (m_lock)
        {
            while (m_outstandingConnections.size() > 0)
            {
                if (!waitOnLock(timeoutExpiration))
                {
                    return false;
                }
            }

            return true;
        }
    }

    public boolean waitForConnections(long timeout,
                                      TimeUnit unit)
    {
        MonotonousTime timeoutExpiration = TimeUtils.computeTimeoutExpiration(timeout, unit);

        synchronized (m_lock)
        {
            while (m_outstandingConnections.size() == 0)
            {
                if (!waitOnLock(timeoutExpiration))
                {
                    return false;
                }
            }

            return true;
        }
    }

    public boolean acquireIdleLock(long timeout,
                                   TimeUnit unit,
                                   BiConsumerWithException<Optio3DataSourceFactory, Set<ManagedDataSource>> callback) throws
                                                                                                                      Exception
    {
        MonotonousTime timeoutExpiration = TimeUtils.computeTimeoutExpiration(timeout, unit);

        synchronized (m_lock)
        {
            while (m_outstandingConnections.size() > 0)
            {
                if (!waitOnLock(timeoutExpiration))
                {
                    return false;
                }
            }

            callback.accept(this, Sets.newHashSet(m_activeManagedDataSources));

            return true;
        }
    }

    private boolean waitOnLock(MonotonousTime timeoutExpiration)
    {
        return TimeUtils.waitOnLock(m_lock, timeoutExpiration);
    }

    public ManagedDataSource buildRawDataSource()
    {
        return super.build(null, "Idle Data Source");
    }

    //--//

    private void notifyStartDataSource(ManagedDataSource dataSource)
    {
        synchronized (m_lock)
        {
            m_activeManagedDataSources.add(dataSource);
        }
    }

    private void notifyStopDataSource(ManagedDataSource dataSource)
    {
        synchronized (m_lock)
        {
            m_activeManagedDataSources.remove(dataSource);
        }
    }

    public void dumpOpenConnections(Consumer<String> callback)
    {
        if (trackConnectionClients)
        {
            synchronized (m_lock)
            {
                addLine(callback, "");
                addLine(callback, "Currently %d open connection(s)", m_outstandingConnections.size());

                Map<StackTraceAnalyzer, List<ProxyForConnection>> uniqueStackTraces = StackTraceAnalyzer.summarize(m_outstandingConnections, (conn) -> conn.m_openStackTrace);

                for (StackTraceAnalyzer st : uniqueStackTraces.keySet())
                {
                    List<ProxyForConnection> sameStackTrace = uniqueStackTraces.get(st);

                    addLine(callback, "");
                    addLine(callback, "Found %d unique stack traces: ", sameStackTrace.size());
                    for (StackTraceElement ste : st.elements)
                    {
                        addLine(callback, "   at %s", ste);
                    }
                }

                addLine(callback, "");
            }
        }
    }

    private static void addLine(Consumer<String> callback,
                                String fmt,
                                Object... args)
    {
        callback.accept(String.format(fmt, args));
    }

    private void notifyOpenConnection(ProxyForConnection proxy)
    {
        synchronized (m_lock)
        {
            if (m_outstandingConnections.size() == 0)
            {
                m_lock.notifyAll();
            }

            m_outstandingConnections.add(proxy);
        }
    }

    private void notifyCloseConnection(ProxyForConnection proxy)
    {
        synchronized (m_lock)
        {
            m_outstandingConnections.remove(proxy);

            if (m_outstandingConnections.size() == 0)
            {
                m_lock.notifyAll();

                if (enableCompaction && (m_compactionActive == null || m_compactionActive.isDone()))
                {
                    File file = extractDatabaseFile();
                    if (file == null)
                    {
                        enableCompaction = false;
                    }
                    else if (file.exists() && file.length() > compactionThreshold)
                    {
                        LoggerInstance.debug("Database file size exceeded threshold (%,d vs. %,d), queueing compaction...", file.length(), compactionThreshold);

                        LongRunningThreadPool executor = Executors.getDefaultLongRunningThreadPool();
                        m_compactionActive = executor.queue(this::tryCompaction);
                    }
                }
            }
        }
    }

    private void tryCompaction()
    {
        try
        {
            if (m_compactionTriggered == null)
            {
                m_compactionTriggered = TimeUtils.now();
            }

            LoggerInstance.debug("Waiting for idle database before compacting...");

            // Wait for the database to be idle.
            if (waitForNoConnections(idleTimeBeforeCompaction, TimeUnit.SECONDS))
            {
                // Make sure the database stays idle for N seconds.
                if (!waitForConnections(idleTimeBeforeCompaction, TimeUnit.SECONDS))
                {
                    // Finally try to compact, no wait.
                    if (acquireIdleLock(0, TimeUnit.SECONDS, this::doCompaction))
                    {
                        m_compactionTriggered = null;
                        return;
                    }
                }
            }

            LoggerInstance.debug("Database compaction not run, database not idle for %d seconds", idleTimeBeforeCompaction);

            if (m_compactionTriggered != null)
            {
                ZonedDateTime now = TimeUtils.now();
                if (m_compactionTriggered.plus(60, ChronoUnit.SECONDS)
                                         .isBefore(now))
                {
                    dumpOpenConnections(LoggerInstance::debug);
                    m_compactionTriggered = now;
                }
            }
        }
        catch (Exception e)
        {
            LoggerInstance.error("Database compaction failed: %s", e);
        }
    }

    private void doCompaction(Optio3DataSourceFactory factory,
                              Set<ManagedDataSource> dataSources) throws
                                                                  Exception
    {
        //
        // Stop all the data sources, this will disconnect from the database.
        //
        for (ManagedDataSource dataSource : dataSources)
        {
            dataSource.stop();
        }

        File dbFile = extractDatabaseFile();

        LoggerInstance.info("Starting Database compaction (%,d bytes)...", dbFile.length());

        try (FileSystem.TmpFileHolder tempDb = FileSystem.createTempFile("backup", "sql"))
        {
            final File   tempDbFile     = tempDb.get();
            final String tempDbFilePath = tempDbFile.getAbsolutePath();

            //
            // First, export the database as a SQL dump.
            //
            {
                ManagedDataSource rawDataSource = factory.buildRawDataSource();
                try (Connection connRaw = rawDataSource.getConnection())
                {
                    try (Statement stmt = connRaw.createStatement())
                    {
                        stmt.setQueryTimeout(60 * 60); // One Hour

                        stmt.execute(String.format("SCRIPT BLOCKSIZE 128000 TO '%s'", tempDbFilePath));
                        stmt.execute("SHUTDOWN");
                    }
                }
                finally
                {
                    rawDataSource.stop();
                }
            }

            LoggerInstance.info("Database backed up...");

            //
            // Second, rename the old database, for recovery purposes.
            //
            File dbFileBackup = new File(dbFile.getAbsolutePath() + ".bak");
            if (dbFileBackup.exists())
            {
                dbFileBackup.delete();
            }

            dbFile.renameTo(dbFileBackup);

            LoggerInstance.info("Database backup moved...");

            //
            // Finally, rebuild the database from the SQL dump.
            //
            try
            {
                ManagedDataSource rawDataSource = factory.buildRawDataSource();
                try (Connection connRaw = rawDataSource.getConnection())
                {
                    try (Statement stmt = connRaw.createStatement())
                    {
                        stmt.setQueryTimeout(60 * 60); // One Hour

                        stmt.execute(String.format("RUNSCRIPT FROM '%s'", tempDbFilePath));
                    }

                    // Restore successful, remove backup.
                    dbFileBackup.delete();
                }
                finally
                {
                    rawDataSource.stop();
                }
            }
            finally
            {
                //
                // If the backup still exists, something went wrong.
                // Move the backup back to the original place.
                //
                if (dbFileBackup.exists())
                {
                    LoggerInstance.error("Database compaction failed, restoring backup...");

                    dbFile.delete();
                    dbFileBackup.renameTo(dbFile);
                }
            }
        }

        long fileSize = dbFile.length();

        //
        // Let the database grow to three times the size after the compaction, to avoid unnecessary churn.
        //
        if (compactionThreshold < fileSize * 3)
        {
            compactionThreshold = fileSize * 3;
        }

        LoggerInstance.info("Database compaction succeeded: new file size %,d (new threshold: %,d)", fileSize, compactionThreshold);
    }

    private ManagedDataSource proxyForManagedDataSource(ManagedDataSource dataSource)
    {
        return newProxy(ManagedDataSource.class, new ProxyForManagedDataSource(dataSource));
    }

    private Connection proxyForConnection(Connection conn) throws
                                                           SQLException
    {
        return newProxy(Connection.class, new ProxyForConnection(conn));
    }

    private static <T> T newProxy(Class<T> itf,
                                  InvocationHandler proxyImpl)
    {
        return itf.cast(ProxyHelper.getProxy(itf.getClassLoader(), new Class[] { itf }, proxyImpl));
    }
}

