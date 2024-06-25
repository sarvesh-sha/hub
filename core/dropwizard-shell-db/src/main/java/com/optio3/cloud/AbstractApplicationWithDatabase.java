/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.ws.rs.core.GenericType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.client.SwaggerExtensions;
import com.optio3.cloud.db.HibernateBundleWrapper;
import com.optio3.cloud.db.Optio3DataSourceFactory;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.db.RefreshLiquibaseCommand;
import com.optio3.cloud.formatting.TabularField;
import com.optio3.cloud.formatting.TabularReportAsCSV;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.channel.RpcChannel;
import com.optio3.cloud.messagebus.transport.StableIdentity;
import com.optio3.cloud.persistence.DbAction;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.PostCommitNotification;
import com.optio3.cloud.persistence.PostCommitNotificationReason;
import com.optio3.cloud.persistence.PostCommitNotificationState;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionResolver;
import com.optio3.cloud.search.HibernateSearch;
import com.optio3.concurrency.AsyncGate;
import com.optio3.concurrency.Executors;
import com.optio3.lang.RunnableWithException;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.swagger.models.Model;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionResolver;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;

public abstract class AbstractApplicationWithDatabase<T extends AbstractConfigurationWithDatabase> extends AbstractApplicationWithSwagger<T>
{
    static class RowForRequestStatistics
    {
        @TabularField(order = 0, title = "URL")
        public String col_url;

        @TabularField(order = 1, title = "Hits")
        public int col_hits;

        @TabularField(order = 2, title = "Execution time")
        public long col_execTime;

        @TabularField(order = 3, title = "Bytes written")
        public long col_bytesWritten;

        @TabularField(order = 4, title = "Success")
        public int col_success;

        @TabularField(order = 5, title = "Client Errors")
        public int col_errorsClient;

        @TabularField(order = 6, title = "Server Errors")
        public int col_errorsServer;
    }

    static class RowForMessageBusStatistics
    {
        @TabularField(order = 0, title = "Id")
        public String col_id;

        @TabularField(order = 1, title = "Last Activity")
        public ZonedDateTime col_lastTimestamp;

        @TabularField(order = 2, title = "Display Name")
        public String col_displayName;

        @TabularField(order = 3, title = "Connections")
        public long col_total_connections;

        @TabularField(order = 4, title = "RX messages")
        public long col_total_rx_messages;

        @TabularField(order = 5, title = "TX messages")
        public long col_total_tx_messages;

        @TabularField(order = 6, title = "RX bytes")
        public long col_total_rx_bytes;

        @TabularField(order = 7, title = "TX bytes")
        public long col_total_tx_bytes;

        @TabularField(order = 8, title = "Connections (3 hours)")
        public long col_3hours_connections;

        @TabularField(order = 9, title = "RX messages (3 hours)")
        public long col_3hours_rx_messages;

        @TabularField(order = 10, title = "TX messages (3 hours)")
        public long col_3hours_tx_messages;

        @TabularField(order = 11, title = "RX bytes (3 hours)")
        public long col_3hours_rx_bytes;

        @TabularField(order = 12, title = "TX bytes (3 hours)")
        public long col_3hours_tx_bytes;

        @TabularField(order = 13, title = "Connections (24 hours)")
        public long col_24hours_connections;

        @TabularField(order = 14, title = "RX messages (24 hours)")
        public long col_24hours_rx_messages;

        @TabularField(order = 15, title = "TX messages (24 hours)")
        public long col_24hours_tx_messages;

        @TabularField(order = 16, title = "RX bytes (24 hours)")
        public long col_24hours_rx_bytes;

        @TabularField(order = 17, title = "TX bytes (24 hours)")
        public long col_24hours_tx_bytes;

        @TabularField(order = 18, title = "Last Connection")
        public String col_connection;

        @TabularField(order = 19, title = "RPC ID")
        public String col_rpcId;
    }

    //--//

    public static final Semaphore GlobalRateLimiter = Executors.allocateSemaphore(1.5);

    private final Map<String, HibernateBundleWrapper<T>> m_hibernateWrappers = Maps.newHashMap();

    private final Map<String, Map<Class<?>, CopyOnWriteArrayList<DatabaseChangeRegistration>>> m_hibernateNotification = Maps.newHashMap();
    private       AsyncGate                                                                    m_hibernateIndexingGate;

    //--//

    /**
     * Marker class used to detect Gates.
     */
    public static abstract class GateClass
    {
    }

    protected AbstractApplicationWithDatabase()
    {
        registerService(AbstractApplicationWithDatabase.class, () -> this);
        registerService(AbstractConfigurationWithDatabase.class, () -> m_configuration);

        //--//

        registerService(SessionFactory.class, () -> getSessionFactory(null));

        registerService(Session.class, () ->
        {
            SessionFactory factory = getService(SessionFactory.class);
            if (factory == null)
            {
                return null;
            }

            try
            {
                return factory.getCurrentSession();
            }
            catch (HibernateException ex)
            {
                return null;
            }
        });
    }

    @Override
    protected void initialize()
    {
        Bootstrap<?> bootstrap = getServiceNonNull(Bootstrap.class);
        bootstrap.addCommand(new RefreshLiquibaseCommand<T>(this));
    }

    @Override
    protected void registerWithJersey(JerseyEnvironment jersey) throws
                                                                Exception
    {
        super.registerWithJersey(jersey);

        m_hibernateIndexingGate = new AsyncGate(m_configuration.isRunningUnitTests() ? 0 : m_configuration.hibernateIndexingGateDelay, TimeUnit.SECONDS);

        try
        {
            initializeDatabase(m_configuration, m_environment);
        }
        catch (Exception e)
        {
            e.printStackTrace();

            throw e;
        }

        jersey.register(new HibernateBundleWrapper.Mapper());

        for (HibernateBundleWrapper<T> hibernateWrapper : m_hibernateWrappers.values())
        {
            hibernateWrapper.registerForEvents(this);
        }

        HibernateBundleWrapper<T> defaultHibernateWrapper = m_hibernateWrappers.get(null);
        if (defaultHibernateWrapper != null)
        {
            //
            // Make Session Factory available to Resources through "@Inject SessionFactory var".
            //
            jersey.register(new AbstractBinder()
            {
                @Override
                protected void configure()
                {
                    SessionFactory sessionFactory = defaultHibernateWrapper.getSessionFactory();

                    bind(sessionFactory).to(SessionFactory.class);
                }
            });
        }

        jersey.register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bind(new SessionResolver(AbstractApplicationWithDatabase.this)).to(new GenericType<InjectionResolver<Optio3Dao>>()
                {
                });
            }
        });
    }

    @Override
    protected void onServerStarted()
    {
        super.onServerStarted();

        for (HibernateBundleWrapper<T> hibernateWrapper : m_hibernateWrappers.values())
        {
            try
            {
                hibernateWrapper.getDataSourceFactory()
                                .startIndexing();
            }
            catch (Throwable t)
            {
                LoggerInstance.error("Failed to start indexing, due to %s", t);
            }
        }
    }

    @Override
    protected void onServerStopping()
    {
        try
        {
            LoggerInstance.info("Stopping indexing...");

            for (HibernateBundleWrapper<T> hibernateWrapper : m_hibernateWrappers.values())
            {
                hibernateWrapper.getDataSourceFactory()
                                .stopIndexing();
            }

            LoggerInstance.info("Stopped indexing");
        }
        catch (Exception e)
        {
            LoggerInstance.error("Caught exception during shutdown: %s", e);
        }

        super.onServerStopping();
    }

    public void refreshDatabase(T cfg,
                                Environment environment) throws
                                                         Exception
    {
        m_configuration = cfg;
        m_environment   = environment;

        initializeDatabase(cfg, environment);
    }

    protected void initializeDatabase(T cfg,
                                      Environment environment) throws
                                                               Exception
    {
    }

    //--//

    protected void fixupRecordId(List<Class<?>> entities,
                                 Type type,
                                 Model model)
    {
        if (entities != null)
        {
            Class<?> clz = Reflection.getRawType(type);

            for (Class<?> entity : entities)
            {
                Optio3TableInfo anno = entity.getAnnotation(Optio3TableInfo.class);
                if (anno != null && anno.model() == clz)
                {
                    model.getVendorExtensions()
                         .put(SwaggerExtensions.TYPE_TABLE.getText(), anno.externalId());

                    return;
                }
            }
        }
    }

    protected List<Class<?>> enableHibernate(T configuration,
                                             Environment environment,
                                             String database,
                                             Optio3DataSourceFactory dataSourceFactory,
                                             String packagePrefix) throws
                                                                   Exception
    {
        if (dataSourceFactory == null)
        {
            return null;
        }

        final String driverClass = dataSourceFactory.getDriverClass();
        if (driverClass == null || driverClass.equals("invalid"))
        {
            return null;
        }

        ImmutableList.Builder<Class<?>> builder = new ImmutableList.Builder<>();

        Reflections reflections = new Reflections(packagePrefix, new TypeAnnotationsScanner());

        Set<Class<?>> noDynamicUpdate = Sets.newHashSet();

        for (Class<?> t : reflections.getTypesAnnotatedWith(Entity.class, true))
        {
            ensureEnumAsStrings(t);
            checkCollectionAnnotations(noDynamicUpdate, t);
            builder.add(t);

            if (!Reflection.isAbstractClass(t) && ModelMapperTarget.class.isAssignableFrom(t))
            {
                @SuppressWarnings("unchecked") Class<? extends ModelMapperTarget<?, ?>> t2 = (Class<? extends ModelMapperTarget<?, ?>>) t;

                ModelMapper.validateModel(t2);
            }
        }

        if (noDynamicUpdate.size() > 0)
        {
            for (Class<?> t : noDynamicUpdate)
            {
                System.out.println(t);
            }

            throw Exceptions.newRuntimeException("Due to Hibernate bug HHH-11506, entities with multiple lazy properties have to be annotated with @DynamicUpdate");
        }

        ImmutableList<Class<?>> entities = builder.build();
        if (!entities.isEmpty())
        {
            //
            // Ensure all the table names have been resolved.
            //
            for (Class<?> entity : entities)
            {
                RecordHelper.registerEntityTable(entity);
            }

            if (m_hibernateWrappers.size() == 0)
            {
                Hibernate5Module module = new Hibernate5Module();
                module.enable(Hibernate5Module.Feature.FORCE_LAZY_LOADING);

                ObjectMapper objectMapper = getServiceNonNull(ObjectMapper.class);
                objectMapper.registerModules(module);
            }

            m_hibernateWrappers.put(database, new HibernateBundleWrapper<>(this, configuration, environment, database, dataSourceFactory, entities));

            if (dataSourceFactory.countRecords)
            {
                try (SessionHolder sessionHolder = SessionHolder.createWithNewReadOnlySession(this, database, Optio3DbRateLimiter.System))
                {
                    List<Class<?>> sortedEntities = Lists.newArrayList(entities);

                    sortedEntities.sort((a, b) ->
                                        {
                                            String aName = a.getSimpleName();
                                            String bName = b.getSimpleName();

                                            return aName.compareTo(bName);
                                        });

                    for (Class<?> entity : sortedEntities)
                    {
                        if (Reflection.isSubclassOf(RecordWithCommonFields.class, entity))
                        {
                            try
                            {
                                @SuppressWarnings("unchecked") Class<? extends RecordWithCommonFields> entity2 = (Class<? extends RecordWithCommonFields>) entity;
                                long                                                                   num     = QueryHelperWithCommonFields.count(sessionHolder.createHelper(entity2), null);

                                LoggerInstance.info("%-50s: %d\n", entity.getSimpleName(), num);
                            }
                            catch (Exception e)
                            {
                                LoggerInstance.info("Failed to count records in table %s: %s", entity.getSimpleName(), e);
                            }
                        }
                    }
                }
            }
        }

        return entities;
    }

    private static void ensureEnumAsStrings(Class<?> t)
    {
        for (Field field : Reflection.collectFields(t)
                                     .values())
        {
            if (field.isAnnotationPresent(Transient.class))
            {
                continue;
            }

            if (field.getType()
                     .isEnum())
            {
                Enumerated anno = field.getAnnotation(Enumerated.class);
                if (anno == null)
                {
                    throw Exceptions.newRuntimeException("Field %s is not annotated with @Enumerated", field);
                }

                if (anno.value() != EnumType.STRING)
                {
                    throw Exceptions.newRuntimeException("Field %s is not annotated with @Enumerated(EnumType.STRING)", field);
                }
            }
        }
    }

    private static void checkCollectionAnnotations(Set<Class<?>> noDynamicUpdate,
                                                   Class<?> t)
    {
        int lazyFields = 0;

        for (Field field : Reflection.collectFields(t)
                                     .values())
        {
            boolean checkJoinColumn = false;

            LazyToOne annoLazyToOne = field.getAnnotation(LazyToOne.class);

            Optio3ControlNotifications annoControl = field.getAnnotation(Optio3ControlNotifications.class);
            Optio3Cascade              annoDelete  = field.getAnnotation(Optio3Cascade.class);

            OneToOne annoOneToOne = field.getAnnotation(OneToOne.class);
            if (annoOneToOne != null)
            {
                if (annoOneToOne.fetch() == FetchType.LAZY)
                {
                    lazyFields++;

                    if (annoLazyToOne == null)
                    {
                        throw Exceptions.newRuntimeException("Lazy @OneToOne field %s is not annotated with @LazyToOne", field);
                    }
                    else
                    {
                        LazyToOneOption value = annoLazyToOne.value();
                        if (value != LazyToOneOption.PROXY)
                        {
                            throw Exceptions.newRuntimeException("@LazyToOne field %s is not using PROXY", field);
                        }
                    }
                }

                if (annoControl == null)
                {
                    throw Exceptions.newRuntimeException("No @Optio3ControlNotifications annotation on field %s", field);
                }

                checkJoinColumn = true;
            }

            ManyToOne annoManyToOne = field.getAnnotation(ManyToOne.class);
            if (annoManyToOne != null)
            {
                if (annoManyToOne.fetch() == FetchType.LAZY)
                {
                    lazyFields++;

                    if (annoLazyToOne == null)
                    {
                        throw Exceptions.newRuntimeException("Lazy @ManyToOne field %s is not annotated with @LazyToOne", field);
                    }
                    else
                    {
                        LazyToOneOption value = annoLazyToOne.value();
                        if (value != LazyToOneOption.PROXY)
                        {
                            throw Exceptions.newRuntimeException("@LazyToOne field %s is not using PROXY", field);
                        }
                    }
                }

                if (annoControl == null)
                {
                    throw Exceptions.newRuntimeException("Missing @Optio3ControlNotifications annotation on field %s", field);
                }

                checkJoinColumn = true;
            }

            OneToMany annoOneToMany = field.getAnnotation(OneToMany.class);
            if (annoOneToMany != null)
            {
                if (annoControl != null && !annoControl.markerForLeftJoin())
                {
                    throw Exceptions.newRuntimeException("Wrong @Optio3ControlNotifications annotation for @OneToMany field %s", field);
                }

                if (annoDelete != null)
                {
                    throw Exceptions.newRuntimeException("Wrong @Optio3Cascade annotation for @OneToMany field %s", field);
                }
            }

            ManyToMany annoManyToMany = field.getAnnotation(ManyToMany.class);
            if (annoManyToMany != null)
            {
                if (StringUtils.isEmpty(annoManyToMany.mappedBy()))
                {
                    if (annoControl == null)
                    {
                        throw Exceptions.newRuntimeException("No @Optio3ControlNotifications annotation on field %s", field);
                    }

                    if (field.getAnnotation(JoinTable.class) == null)
                    {
                        throw Exceptions.newRuntimeException("No @JoinTable annotation on @ManyToMany field %s", field);
                    }
                }
                else
                {
                    if (annoControl != null)
                    {
                        throw Exceptions.newRuntimeException("Wrong @Optio3ControlNotifications annotation for @ManyToMany field %s", field);
                    }
                }
            }

            Basic annoBasic = field.getAnnotation(Basic.class);
            if (annoBasic != null && annoBasic.fetch() == FetchType.LAZY)
            {
                lazyFields++;
            }

            if (checkJoinColumn)
            {
                JoinColumn annoJoinColumn = field.getAnnotation(JoinColumn.class);
                if (annoJoinColumn == null)
                {
                    throw Exceptions.newRuntimeException("Collection field %s is not annotated with @JoinColumn", field);
                }

                if (StringUtils.isEmpty(annoJoinColumn.name()))
                {
                    throw Exceptions.newRuntimeException("@JoinColumn for field %s does not specify a name", field);
                }

                if (StringUtils.isEmpty(annoJoinColumn.foreignKey()
                                                      .name()))
                {
                    throw Exceptions.newRuntimeException("@JoinColumn for field %s does not specify a @ForeignKey", field);
                }
            }
        }

        if (lazyFields > 1 && !t.isAnnotationPresent(DynamicUpdate.class))
        {
            noDynamicUpdate.add(t);
        }
    }

    public Optio3DataSourceFactory getDataSourceFactory(String databaseName)
    {
        HibernateBundleWrapper<T> hibernateWrapper = m_hibernateWrappers.get(databaseName);
        return hibernateWrapper != null ? hibernateWrapper.getDataSourceFactory() : null;
    }

    public ImmutableList<Class<?>> getDataSourceEntities(String databaseName)
    {
        HibernateBundleWrapper<T> hibernateWrapper = m_hibernateWrappers.get(databaseName);
        return hibernateWrapper != null ? hibernateWrapper.getEntities() : null;
    }

    public SessionFactory getSessionFactory(String databaseName)
    {
        HibernateBundleWrapper<T> hibernateWrapper = m_hibernateWrappers.get(databaseName);
        return hibernateWrapper != null ? hibernateWrapper.getSessionFactory() : null;
    }

    //--//

    public List<String> dumpRequestStatistics()
    {
        TabularReportAsCSV<RowForRequestStatistics> tr = new TabularReportAsCSV<>(RowForRequestStatistics.class);

        var stats = Lists.newArrayList(getRequestStatistics().values());

        stats.sort(Comparator.comparing((a) -> a.path));

        tr.emit(rowHandler ->
                {
                    for (RequestStatistics rs : stats)
                    {
                        if (rs.count > 0)
                        {
                            RowForRequestStatistics row = new RowForRequestStatistics();

                            row.col_url          = rs.path;
                            row.col_hits         = rs.count;
                            row.col_execTime     = rs.executionTime;
                            row.col_bytesWritten = rs.bytesWritten;
                            row.col_success      = rs.statusSuccess;
                            row.col_errorsClient = rs.statusClientError;
                            row.col_errorsServer = rs.statusServerError;

                            rowHandler.emitRow(row);
                        }
                    }
                });

        return tr.lines;
    }

    public List<String> dumpMessageBusStatistics()
    {
        TabularReportAsCSV<RowForMessageBusStatistics> tr = new TabularReportAsCSV<>(RowForMessageBusStatistics.class);

        Map<String, StableIdentity> stats = getMessageBusStatistics();
        List<String>                keys  = Lists.newArrayList(stats.keySet());

        keys.sort((a, b) ->
                  {
                      StableIdentity idA = stats.get(a);
                      StableIdentity idB = stats.get(b);

                      int diff = StringUtils.compare(idA.displayName, idB.displayName);
                      if (diff == 0)
                      {
                          diff = StringUtils.compare(a, b);
                      }

                      return diff;
                  });

        tr.emit(rowHandler ->
                {
                    for (String id : keys)
                    {
                        StableIdentity            si    = stats.get(id);
                        StableIdentity.Statistics total = si.totalStatistics;

                        if (total.messagesRx != 0 || total.messagesTx != 0)
                        {
                            StableIdentity.Statistics last3Hours  = si.report(1, 3);
                            StableIdentity.Statistics last24Hours = si.report(1, 24);

                            RowForMessageBusStatistics row = new RowForMessageBusStatistics();

                            row.col_id                  = id;
                            row.col_lastTimestamp       = TimeUtils.fromMilliToLocalTime(si.lastTimestampUTC);
                            row.col_displayName         = si.displayName;
                            row.col_total_connections   = total.connections;
                            row.col_total_rx_messages   = total.messagesRx;
                            row.col_total_tx_messages   = total.messagesTx;
                            row.col_total_rx_bytes      = total.bytesRx;
                            row.col_total_tx_bytes      = total.bytesTx;
                            row.col_3hours_connections  = last3Hours.connections;
                            row.col_3hours_rx_messages  = last3Hours.messagesRx;
                            row.col_3hours_tx_messages  = last3Hours.messagesTx;
                            row.col_3hours_rx_bytes     = last3Hours.bytesRx;
                            row.col_3hours_tx_bytes     = last3Hours.bytesTx;
                            row.col_24hours_connections = last24Hours.connections;
                            row.col_24hours_rx_messages = last24Hours.messagesRx;
                            row.col_24hours_tx_messages = last24Hours.messagesTx;
                            row.col_24hours_rx_bytes    = last24Hours.bytesRx;
                            row.col_24hours_tx_bytes    = last24Hours.bytesTx;

                            if (si.lastKnownConnection != null)
                            {
                                InetAddress address = si.lastKnownConnection.getAddress();
                                row.col_connection = String.format("%s:%d", address.getHostAddress(), si.lastKnownConnection.getPort());
                            }

                            row.col_rpcId = si.rpcId;

                            rowHandler.emitRow(row);
                        }
                    }
                });

        return tr.lines;
    }

    //--//

    static class WaitForAllGates
    {
        private final AbstractApplicationWithDatabase<?> m_app;
        private final Class<? extends GateClass>[]       m_gateClasses;

        WaitForAllGates(AbstractApplicationWithDatabase<?> app,
                        Class<? extends GateClass>[] gateClasses)
        {
            m_app         = app;
            m_gateClasses = gateClasses;
        }

        public CompletableFuture<Void> waitForAll()
        {
            while (true)
            {
                boolean hadToWait = false;

                for (Class<? extends GateClass> gateClass : m_gateClasses)
                {
                    AsyncGate gate = m_app.getGate(gateClass);

                    CompletableFuture<Void> waiter = gate.getWaiter();
                    if (!waiter.isDone())
                    {
                        hadToWait = true;

                        try
                        {
                            await(waiter);
                        }
                        catch (Exception e)
                        {
                            // Waiters don't really throw, just an artifact of the signature of await().
                        }
                    }
                }

                if (!hadToWait)
                {
                    // At least one gate was closed, we have to go around again.
                    break;
                }
            }

            return wrapAsync(null);
        }

        public CompletableFuture<Void> waitForAllAndExecute(RunnableWithException worker)
        {
            try
            {
                await(waitForAll());
            }
            catch (Exception e)
            {
                // Waiters don't really throw, just an artifact of the signature of await().
            }

            return Executors.getDefaultLongRunningThreadPool()
                            .queue(worker);
        }
    }

    public AsyncGate getGate(Class<? extends GateClass> gateClass)
    {
        if (gateClass == HibernateSearch.Gate.class)
        {
            return m_hibernateIndexingGate;
        }

        throw Exceptions.newIllegalArgumentException("Unknown gate '%s'", gateClass.getSimpleName());
    }

    @SafeVarargs
    public final CompletableFuture<Void> waitForAllGatesToOpenThenExecuteLongRunningTask(RunnableWithException worker,
                                                                                         Class<? extends GateClass>... gateClasses)
    {
        WaitForAllGates w = new WaitForAllGates(this, gateClasses);

        return w.waitForAllAndExecute(worker);
    }

    @SafeVarargs
    public final void waitForAllGatesToOpen(Class<? extends GateClass>... gateClasses) throws
                                                                                       Exception
    {
        WaitForAllGates w = new WaitForAllGates(this, gateClasses);

        w.waitForAll()
         .get();
    }

    public void waitForGateToOpen(Class<? extends GateClass> gateClass) throws
                                                                        Exception
    {
        AsyncGate gate = getGate(gateClass);

        CompletableFuture<Void> waiter = gate.getWaiter();
        waiter.get();
    }

    public boolean waitForGateToOpen(Class<? extends GateClass> gateClass,
                                     Duration timeout)
    {
        AsyncGate gate = getGate(gateClass);
        try
        {
            CompletableFuture<Void> waiter = gate.getWaiter();
            waiter.get(timeout.toNanos(), TimeUnit.NANOSECONDS);

            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public AsyncGate.Holder closeGate(Class<? extends GateClass> gateClass)
    {
        AsyncGate gate = getGate(gateClass);

        return gate.blockConsumers();
    }

    //--//

    public class DatabaseChangeRegistration implements AutoCloseable
    {
        private final CopyOnWriteArrayList<DatabaseChangeRegistration> m_list;
        private final PostCommitNotification                           m_callback;

        public DatabaseChangeRegistration(CopyOnWriteArrayList<DatabaseChangeRegistration> list,
                                          PostCommitNotification callback)
        {
            m_list     = list;
            m_callback = callback;
        }

        @Override
        public void close()
        {
            m_list.remove(this);
        }

        void accept(String databaseId,
                    Object entity,
                    Serializable id,
                    PostCommitNotificationReason action,
                    PostCommitNotificationState state)
        {
            try
            {
                m_callback.accept(databaseId, entity, id, action, state);
            }
            catch (Throwable ex)
            {
                // Ignore notification failures.
                LoggerInstance.error("PostCommit Notification for %s:%s.%s %s failed with: %s",
                                     BoxingUtils.get(databaseId, "<default>"),
                                     entity.getClass()
                                           .getName(),
                                     id,
                                     action,
                                     ex);
            }
        }
    }

    public DatabaseChangeRegistration registerLocalDatabaseChangeNotification(String databaseId,
                                                                              Class<?> clz,
                                                                              PostCommitNotification callback)
    {
        Map<Class<?>, CopyOnWriteArrayList<DatabaseChangeRegistration>> map = accessNotificationMap(databaseId, true);

        CopyOnWriteArrayList<DatabaseChangeRegistration> list;

        synchronized (map)
        {
            list = map.get(clz);
            if (list == null)
            {
                list = Lists.newCopyOnWriteArrayList();
                map.put(clz, list);
            }
        }

        DatabaseChangeRegistration reg = new DatabaseChangeRegistration(list, callback);
        list.add(reg);

        return reg;
    }

    public void postDatabaseEvent(String databaseId,
                                  Class<?> entityClass,
                                  Serializable id,
                                  DbAction action,
                                  ZonedDateTime lastUpdate)
    {
        HibernateBundleWrapper<T> wrapper = m_hibernateWrappers.get(databaseId);
        if (wrapper != null)
        {
            wrapper.postEvent(entityClass, id, action, lastUpdate);
        }
    }

    public void drainDatabaseEvents(String databaseId)
    {
        HibernateBundleWrapper<T> wrapper = m_hibernateWrappers.get(databaseId);
        if (wrapper != null)
        {
            wrapper.drainDatabaseEvents();
        }
    }

    //--//

    private Map<Class<?>, CopyOnWriteArrayList<DatabaseChangeRegistration>> accessNotificationMap(String databaseId,
                                                                                                  boolean createIfMissing)
    {
        synchronized (m_hibernateNotification)
        {
            Map<Class<?>, CopyOnWriteArrayList<DatabaseChangeRegistration>> map = m_hibernateNotification.get(databaseId);
            if (map == null && createIfMissing)
            {
                map = Maps.newHashMap();
                m_hibernateNotification.put(databaseId, map);
            }

            return map;
        }
    }

    public void notifyPostCommit(String databaseId,
                                 Object entity,
                                 Serializable id,
                                 PostCommitNotificationReason action,
                                 PostCommitNotificationState state)
    {

        Map<Class<?>, CopyOnWriteArrayList<DatabaseChangeRegistration>> map = accessNotificationMap(databaseId, false);
        if (map == null)
        {
            return;
        }

        Class<?> clz = entity.getClass();
        while (true)
        {
            CopyOnWriteArrayList<DatabaseChangeRegistration> regs;

            synchronized (map)
            {
                regs = map.get(clz);
            }

            if (regs != null)
            {
                for (DatabaseChangeRegistration reg : regs)
                    reg.accept(databaseId, entity, id, action, state);
            }

            //
            // Check for null *after* we send notifications, since null acts as a wildcard.
            //
            if (clz == null)
            {
                break;
            }

            clz = clz.getSuperclass();
        }
    }

    //--//

    public static <T> int findUniqueSequence(String prefix,
                                             int sequence,
                                             Collection<T> items,
                                             Function<T, String> callback)
    {
        Pattern pattern = Pattern.compile(prefix + "(\\d+)");

        for (T item : items)
        {
            String name = callback.apply(item);
            if (name != null)
            {
                Matcher matcher = pattern.matcher(name);
                if (matcher.matches())
                {
                    sequence = Math.max(sequence, Integer.parseInt(matcher.group(1)) + 1);
                }
            }
        }

        return sequence;
    }

    //--//

    private MonotonousTime m_suspendSearchIndexerUntil;
    private boolean        m_suspendSearchIndexerUntilReported;

    public void suspendSearchIndexer(int amount,
                                     TimeUnit unit)
    {
        MonotonousTime newDeadline = TimeUtils.computeTimeoutExpiration(amount, unit);
        m_suspendSearchIndexerUntil = TimeUtils.updateIfAfter(m_suspendSearchIndexerUntil, newDeadline);
    }

    public boolean shouldSuspendSearchIndexer()
    {
        if (TimeUtils.isTimeoutExpired(m_suspendSearchIndexerUntil))
        {
            m_suspendSearchIndexerUntil         = null;
            m_suspendSearchIndexerUntilReported = false;
        }

        if (m_suspendSearchIndexerUntil == null)
        {
            return false;
        }

        if (!m_suspendSearchIndexerUntilReported)
        {
            m_suspendSearchIndexerUntilReported = true;
            LoggerInstance.debug("Indexer suspended until %s...", m_suspendSearchIndexerUntil);
        }

        return true;
    }

    //--//

    public List<String> getRpcStatistics()
    {
        MessageBusBroker broker  = getServiceNonNull(MessageBusBroker.class);
        RpcChannel       channel = broker.getChannelProvider(RpcChannel.class);
        return channel.reportStatistics();
    }

    //--//

    static class Patch
    {
    }

    public static final  Logger                        LoggerInstanceForPatch = new Logger(Patch.class);
    private static final ConcurrentMap<String, String> s_traces               = Maps.newConcurrentMap();

    public static void reportPatchCall(Object val)
    {
        if (LoggerInstanceForPatch.isEnabled(Severity.Debug))
        {
            StackTraceElement[] stackTrace = new Exception().getStackTrace();

            for (int i = 0; i < stackTrace.length; i++)
            {
                StackTraceElement ste = stackTrace[i];

                if (ste.getClassName()
                       .contains("AbstractApplicationWithDatabase"))
                {
                    continue;
                }

                String line = ste.toString();
                if (s_traces.putIfAbsent(line, line) == null)
                {
                    if (val != null)
                    {
                        try
                        {
                            val = ObjectMappers.SkipNulls.writeValueAsString(val);
                        }
                        catch (Throwable t)
                        {
                            val = val.getClass();
                        }
                    }

                    synchronized (s_traces)
                    {
                        LoggerInstanceForPatch.debug("Detected invocation of patch code: %s", val);
                        for (int j = 0; i < stackTrace.length && j < 10; i++, j++)
                        {
                            LoggerInstanceForPatch.debug("   at %s", stackTrace[i].toString());
                        }
                        LoggerInstanceForPatch.debug("   ...");
                    }
                }

                break;
            }
        }
    }
}
