/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.db;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.persistence.Id;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.AbstractApplicationWithDatabase;
import com.optio3.cloud.AbstractConfigurationWithDatabase;
import com.optio3.cloud.annotation.Optio3KeyOrder;
import com.optio3.cloud.annotation.Optio3UpgradeValue;
import com.optio3.cloud.persistence.DbAction;
import com.optio3.cloud.persistence.Interceptor;
import com.optio3.cloud.search.HibernateSearch;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.SessionFactoryFactory;
import io.dropwizard.setup.Environment;
import liquibase.CatalogAndSchema;
import liquibase.change.AddColumnConfig;
import liquibase.change.Change;
import liquibase.change.ColumnConfig;
import liquibase.change.ConstraintsConfig;
import liquibase.change.core.AddColumnChange;
import liquibase.change.core.AddPrimaryKeyChange;
import liquibase.change.core.CreateTableChange;
import liquibase.change.core.DropColumnChange;
import liquibase.change.core.DropDefaultValueChange;
import liquibase.change.core.DropForeignKeyConstraintChange;
import liquibase.change.core.DropIndexChange;
import liquibase.change.core.DropTableChange;
import liquibase.change.core.ModifyDataTypeChange;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.DiffGeneratorFactory;
import liquibase.diff.DiffResult;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.DiffToChangeLog;
import liquibase.exception.DatabaseException;
import liquibase.resource.ResourceAccessor;
import liquibase.serializer.core.xml.XMLChangeLogSerializer;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.SnapshotControl;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.structure.core.Column;
import liquibase.structure.core.ForeignKey;
import liquibase.structure.core.Index;
import liquibase.structure.core.PrimaryKey;
import liquibase.structure.core.Table;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQL57Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;

/**
 * A wrapper for the Hibernate Bundle, to intercept the initialization steps and perform on-the-fly adjustments, like generating a new 'migrations.xml' file.
 *
 * @param <T> The type for the configuration.
 */
public class HibernateBundleWrapper<T extends AbstractConfigurationWithDatabase>
{
    @Provider
    public static class Mapper implements ExceptionMapper<HibernateException>
    {
        @Override
        public Response toResponse(HibernateException ex)
        {
            if (ex instanceof ObjectNotFoundException)
            {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity(ex.getMessage())
                               .type(MediaType.TEXT_PLAIN)
                               .build();
            }

            if (ex instanceof ConstraintViolationException)
            {
                return Response.status(Response.Status.CONFLICT)
                               .entity(ex.getMessage())
                               .type(MediaType.TEXT_PLAIN)
                               .build();
            }

            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(ex.getMessage())
                           .type(MediaType.TEXT_PLAIN)
                           .build();
        }
    }

    public static class H2DatabaseImpl extends liquibase.database.core.H2Database
    {
        @Override
        public int getMaxFractionalDigitsForTimestamp()
        {
            // We have to overwrite this method, because the official H2 database adapter changed the value from 23 to 9,
            // but we already generated all our diffs, everything would break...
            return 23;
        }
    }

    public static class MariaDBDatabaseImpl extends liquibase.database.core.MariaDBDatabase
    {
        @Override
        public boolean supportsSequences()
        {
            return false;
        }
    }

    //
    // Hibernate resolves to the MySQL 5 dialect, which does not support TIMESTAMPS.
    // This resolver checks the minor version and resolves to a dialect that supports TIMESTAMPS.
    //
    public static class DialectResolverImpl implements DialectResolver
    {
        private static final long serialVersionUID = 1L;

        @Override
        public Dialect resolveDialect(DialectResolutionInfo info)
        {
            final String databaseName = info.getDatabaseName()
                                            .toLowerCase();

            switch (databaseName)
            {
                case "mysql":
                case "mariadb":
                {
                    final int majorVersion = info.getDatabaseMajorVersion();

                    if (majorVersion >= 5)
                    {
                        final int minorVersion = info.getDatabaseMinorVersion();

                        if (minorVersion >= 7)
                        {
                            return new MySQL57Dialect();
                        }
                    }

                    break;
                }
            }

            return null;
        }
    }

    //
    // For accessing resources through a class, which allows us to find resources in other JARs.
    //
    static class ResourceAccessorImpl implements ResourceAccessor
    {
        private final Class<?> m_configClass;

        ResourceAccessorImpl(Class<?> configClass)
        {
            m_configClass = configClass;
        }

        @Override
        public Set<InputStream> getResourcesAsStream(String path)
        {
            Set<InputStream> res = Sets.newHashSet();

            InputStream stream = m_configClass.getResourceAsStream(path);
            if (stream != null)
            {
                res.add(stream);
            }

            return res;
        }

        @Override
        public Set<String> list(String relativeTo,
                                String path,
                                boolean includeFiles,
                                boolean includeDirectories,
                                boolean recursive)
        {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public ClassLoader toClassLoader()
        {
            return m_configClass.getClassLoader();
        }
    }

    static class KeyDetails
    {
        int    order;
        String name;
    }

    static class TableDetails
    {
        final Map<String, KeyDetails> ids     = Maps.newHashMap();
        final Map<String, Field>      columns = Maps.newHashMap();

        void addKey(Field field,
                    Id id,
                    String name)
        {
            name = name.toLowerCase();

            var keyDetails = new KeyDetails();
            keyDetails.name = name;

            Optio3KeyOrder order = field.getAnnotation(Optio3KeyOrder.class);
            if (order != null)
            {
                keyDetails.order = order.value();
            }

            ids.put(name, keyDetails);
        }

        int findColumnOrder(Column column)
        {
            return ids.get(column.getName()
                                 .toLowerCase()).order;
        }
    }

    //--//

    private final String                  m_databaseId;
    private final Optio3DataSourceFactory m_dataSourceFactory;
    private final ImmutableList<Class<?>> m_entities;
    private final HibernateBundle<T>      m_hibernate;

    private ProcessorForDatabaseActivity<?> m_processorForDatabaseActivity;
    private boolean                         m_resetDeferrable;

    public HibernateBundleWrapper(AbstractApplicationWithDatabase<?> app,
                                  T configuration,
                                  Environment environment,
                                  String databaseId,
                                  Optio3DataSourceFactory dataSourceFactory,
                                  ImmutableList<Class<?>> entities) throws
                                                                    Exception
    {
        m_databaseId        = databaseId;
        m_dataSourceFactory = dataSourceFactory;
        m_entities          = entities;

        m_hibernate = new HibernateBundle<T>(entities, new SessionFactoryFactory())
        {
            @Override
            public DataSourceFactory getDataSourceFactory(T configuration)
            {
                return m_dataSourceFactory;
            }

            @Override
            protected String name()
            {
                return m_databaseId != null ? m_databaseId : super.name();
            }

            @Override
            protected void configure(Configuration configuration)
            {
                configuration.setImplicitNamingStrategy(ImplicitNamingStrategyComponentPathImpl.INSTANCE);

                configuration.setInterceptor(new Interceptor()
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public SessionFactory getSessionFactory()
                    {
                        return HibernateBundleWrapper.this.getSessionFactory();
                    }
                });
            }
        };

        ManagedDataSource  dataSource = null;
        DatabaseConnection conn       = null;
        DatabaseMetaData   metadata   = null;
        Database           database   = null;

        // Always use UTC as the timezone for timestamps.
        System.setProperty(AvailableSettings.JDBC_TIME_ZONE, "UTC");

        String db = dataSourceFactory.extractDatabaseFlavor();
        switch (db)
        {
            case "mysql":
            case "mariadb":
                //
                // We want to use the INNODB engine with MySql/MariaDB.
                //
                System.setProperty(AvailableSettings.STORAGE_ENGINE, "innodb");
                m_resetDeferrable = true;
                database = new MariaDBDatabaseImpl();
                break;

            case "h2":
                // Keep connections open, otherwise the database will get unloaded.
                m_dataSourceFactory.setMinSize(8);
                database = new H2DatabaseImpl();
                break;
        }

        m_dataSourceFactory.finalizeConfiguration(app, m_databaseId);

        String targetDeltaFile;

        if (dataSourceFactory.saveMigrationDelta != null)
        {
            targetDeltaFile = Optio3Liquibase.generateMigrationPath(m_databaseId, db, dataSourceFactory.migrationsRevisionLevel, dataSourceFactory.migrationsVersionNumber);
        }
        else
        {
            targetDeltaFile = null;
        }

        try
        {
            if (dataSourceFactory.skipMigration)
            {
                dataSourceFactory.autoHibernateMode = true;
            }
            else
            {
                //
                // For in-memory databases, the database is garbage collected when its last connection is closed.
                //
                // To ensure we can perform the delta computation,
                // we need to keep a connection open while Hibernate initializes,
                // otherwise all its changes will be lost.
                //
                dataSource = dataSourceFactory.build(null, "Update Schema");
                Connection connRaw = dataSource.getConnection();
                metadata = connRaw.getMetaData();
                conn     = new JdbcConnection(connRaw);
                database.setConnection(conn);

                if (dataSourceFactory.migrationsVersionNumber == 1)
                {
                    // When we generate the first version of a level, we use a clean database.
                }
                else
                {
                    try (var liquibase = new Optio3Liquibase(new ResourceAccessorImpl(configuration.getClass()), database))
                    {
                        Optio3Liquibase.MigrationLevel topMigrationLevel = liquibase.collectMigrationFiles(m_databaseId, db);

                        liquibase.migrate(topMigrationLevel, dataSourceFactory.migrationsRevisionLevel, dataSourceFactory.migrationsVersionNumber);
                    }
                }
            }

            String hibernateMode = dataSourceFactory.autoHibernateMode ? "update" : "validate";

            DatabaseSnapshot referenceSnapshotBefore = null;

            if (targetDeltaFile != null)
            {
                referenceSnapshotBefore = getSnapshot(database, metadata);

                //
                // To properly track deletions, we need to delete the database.
                //
                dataSourceFactory.dropDatabase(dataSource);
            }

            {
                Map<String, String> props = dataSourceFactory.getProperties();
                props.put(AvailableSettings.HBM2DDL_AUTO, hibernateMode);

                // This is obsolete, but we keep it for future reference.
                if (false)
                {
                    props.put(AvailableSettings.DIALECT_RESOLVERS, DialectResolverImpl.class.getName());
                }

                m_hibernate.run(configuration, environment);
            }

            if (targetDeltaFile != null)
            {
                File file = new File(dataSourceFactory.saveMigrationDelta + targetDeltaFile);
                Files.createDirectories(file.getParentFile()
                                            .toPath());

                try (FileOutputStream output = new FileOutputStream(file))
                {
                    final DatabaseSnapshot referenceSnapshotAfter = getSnapshot(database, metadata);

                    //--//

                    Metamodel metamodel = getSessionFactory().getMetamodel();

                    //
                    // Build lookup map from <table>/<column> to the field of the Java Entity class.
                    // Also, make sure all the foreign keys have unique names.
                    //
                    Map<String, TableDetails> lookup      = Maps.newHashMap();
                    Map<String, Field>        foreignKeys = Maps.newHashMap();

                    for (EntityType<?> entity : metamodel.getEntities())
                    {
                        for (Class<?> clz = entity.getJavaType(); clz != null; clz = clz.getSuperclass())
                        {
                            javax.persistence.Table anno = clz.getAnnotation(javax.persistence.Table.class);
                            if (anno != null)
                            {
                                TableDetails tableDetails = lookup.computeIfAbsent(anno.name()
                                                                                       .toLowerCase(), (key) -> new TableDetails());
                                for (Field field : Reflection.collectFields(clz)
                                                             .values())
                                {
                                    javax.persistence.Id id = field.getAnnotation(javax.persistence.Id.class);

                                    javax.persistence.Column column = field.getAnnotation(javax.persistence.Column.class);
                                    if (column != null)
                                    {
                                        String name = column.name();
                                        tableDetails.columns.put(name.toLowerCase(), field);

                                        if (id != null)
                                        {
                                            tableDetails.addKey(field, id, name);
                                        }
                                    }

                                    javax.persistence.JoinColumn joinColumn = field.getAnnotation(javax.persistence.JoinColumn.class);
                                    if (joinColumn != null)
                                    {
                                        javax.persistence.ForeignKey foreignKey = joinColumn.foreignKey();
                                        String                       name       = foreignKey.name();
                                        Field                        oldField   = foreignKeys.get(name);
                                        if (oldField != null && !oldField.equals(field))
                                        {
                                            throw Exceptions.newIllegalArgumentException("Found two tables using the same foreign key name (%s): %s and %s", name, oldField, field);
                                        }
                                        else
                                        {
                                            foreignKeys.put(name, field);
                                        }

                                        if (id != null)
                                        {
                                            tableDetails.addKey(field, id, joinColumn.name());
                                        }
                                    }
                                }
                            }
                        }
                    }

                    //
                    // For multi-column primary keys, Hibernate doesn't support declaring the order.
                    // Fixup the PrimaryKey and Index objects to enforce the correct order.
                    //
                    for (PrimaryKey referenceObject : referenceSnapshotAfter.get(PrimaryKey.class))
                    {
                        TableDetails tableDetails = lookup.get(referenceObject.getTable()
                                                                              .getName()
                                                                              .toLowerCase());
                        if (tableDetails != null && tableDetails.ids.size() > 1)
                        {
                            var columns = referenceObject.getColumns();
                            columns.sort(Comparator.comparingInt(tableDetails::findColumnOrder));
                        }
                    }

                    for (Index referenceObject : referenceSnapshotAfter.get(Index.class))
                    {
                        TableDetails tableDetails = lookup.get(referenceObject.getRelation()
                                                                              .getName()
                                                                              .toLowerCase());
                        if (tableDetails != null && tableDetails.ids.size() > 1)
                        {
                            var columns = referenceObject.getColumns();
                            columns.sort(Comparator.comparingInt(tableDetails::findColumnOrder));
                        }
                    }

                    //--//

                    final List<ChangeSet> changeSetsAfter = generateDiff(referenceSnapshotBefore, referenceSnapshotAfter, dataSourceFactory.migrationsRoot);

                    //
                    // If we don't start from scratch, we need to perform some fixup.
                    //
                    if (referenceSnapshotBefore != null)
                    {
                        final List<ChangeSet> changeSetsBefore = generateDiff(null, referenceSnapshotBefore, dataSourceFactory.migrationsRoot);

                        //
                        // Because we use "create" mode for Hibernate, the two snapshots might have the same constraint name for different entities.
                        // We need to walk all the changes to make sure we don't have any conflicts.
                        //
                        {
                            Set<String> constraintNames = Sets.newHashSet();
                            enumerateChanges(changeSetsBefore, AddPrimaryKeyChange.class, (primaryKey) ->
                            {
                                final String constraintName = primaryKey.getConstraintName();
                                if (constraintName != null)
                                {
                                    constraintNames.add(constraintName);
                                }
                            });

                            enumerateChanges(changeSetsBefore, CreateTableChange.class, (table) ->
                            {
                                for (ColumnConfig column : table.getColumns())
                                {
                                    ConstraintsConfig constraints = column.getConstraints();
                                    if (constraints != null && constraints.isPrimaryKey() == Boolean.TRUE)
                                    {
                                        final String constraintName = constraints.getPrimaryKeyName();
                                        if (constraintName != null)
                                        {
                                            constraintNames.add(constraintName);
                                        }
                                    }
                                }
                            });

                            //--//

                            enumerateChanges(changeSetsAfter, AddPrimaryKeyChange.class, (primaryKey) ->
                            {
                                String constraintName = primaryKey.getConstraintName();
                                if (constraintName != null)
                                {
                                    if (constraintNames.contains(constraintName))
                                    {
                                        constraintName += "_" + dataSourceFactory.migrationsVersionNumber;
                                        primaryKey.setConstraintName(constraintName);
                                    }

                                    constraintNames.add(constraintName);
                                }
                            });

                            enumerateChanges(changeSetsAfter, CreateTableChange.class, (table) ->
                            {
                                for (ColumnConfig column : table.getColumns())
                                {
                                    ConstraintsConfig constraints = column.getConstraints();
                                    if (constraints != null && constraints.isPrimaryKey() == Boolean.TRUE)
                                    {
                                        String constraintName = constraints.getPrimaryKeyName();
                                        if (constraintName != null)
                                        {
                                            if (constraintNames.contains(constraintName))
                                            {
                                                constraintName += "_" + dataSourceFactory.migrationsVersionNumber;
                                                constraints.setPrimaryKeyName(constraintName);
                                            }

                                            constraintNames.add(constraintName);
                                        }
                                    }
                                }
                            });
                        }

                        //
                        // If a non-nullable column has been added, we need to ensure there's a default value
                        //
                        {
                            enumerateChanges(changeSetsAfter, AddColumnChange.class, (addColumn) ->
                            {
                                for (AddColumnConfig columnConfig : addColumn.getColumns())
                                {
                                    ConstraintsConfig constraints = columnConfig.getConstraints();
                                    if (constraints != null && !constraints.isNullable())
                                    {
                                        Field field = findFieldForColumn(lookup, addColumn.getTableName(), columnConfig.getName());
                                        if (field != null)
                                        {
                                            final Class<?> fieldType = field.getType();

                                            if (fieldType == boolean.class)
                                            {
                                                boolean value = false;

                                                Optio3UpgradeValue anno = field.getAnnotation(Optio3UpgradeValue.class);
                                                if (anno != null)
                                                {
                                                    value = Boolean.parseBoolean(anno.value());
                                                }

                                                columnConfig.setDefaultValueBoolean(value);
                                                continue;
                                            }

                                            if (fieldType == int.class)
                                            {
                                                int value = 0;

                                                Optio3UpgradeValue anno = field.getAnnotation(Optio3UpgradeValue.class);
                                                if (anno != null)
                                                {
                                                    value = Integer.parseInt(anno.value());
                                                }

                                                columnConfig.setDefaultValueNumeric(value);
                                                continue;
                                            }

                                            if (fieldType == long.class)
                                            {
                                                long value = 0;

                                                Optio3UpgradeValue anno = field.getAnnotation(Optio3UpgradeValue.class);
                                                if (anno != null)
                                                {
                                                    value = Long.parseLong(anno.value());
                                                }

                                                columnConfig.setDefaultValueNumeric(value);
                                                continue;
                                            }

                                            if (fieldType == float.class)
                                            {
                                                float value = 0;

                                                Optio3UpgradeValue anno = field.getAnnotation(Optio3UpgradeValue.class);
                                                if (anno != null)
                                                {
                                                    value = Float.parseFloat(anno.value());
                                                }

                                                columnConfig.setDefaultValueNumeric(value);
                                                continue;
                                            }

                                            if (fieldType == double.class)
                                            {
                                                double value = 0;

                                                Optio3UpgradeValue anno = field.getAnnotation(Optio3UpgradeValue.class);
                                                if (anno != null)
                                                {
                                                    value = Double.parseDouble(anno.value());
                                                }

                                                columnConfig.setDefaultValueNumeric(value);
                                                continue;
                                            }

                                            if (fieldType.isEnum())
                                            {
                                                Optio3UpgradeValue anno = field.getAnnotation(Optio3UpgradeValue.class);
                                                if (anno != null)
                                                {
                                                    setDefaultValueForEnum(columnConfig, anno, fieldType);
                                                    continue;
                                                }
                                            }

                                            throw Exceptions.newIllegalArgumentException("Can't generate default value for new column: %s/%s", addColumn.getTableName(), columnConfig.getName());
                                        }
                                    }
                                }
                            });

                            enumerateAndDeleteChanges(changeSetsAfter, ModifyDataTypeChange.class, (modify) ->
                            {
                                if (StringUtils.equals(modify.getNewDataType(), "datetime(19)"))
                                {
                                    return true; // For some reason, Liquibase wants to change the type of these columns.
                                }

                                return false;
                            });

                            enumerateAndDeleteChanges(changeSetsAfter, DropDefaultValueChange.class, (modify) ->
                            {
                                Field field = findFieldForColumn(lookup, modify.getTableName(), modify.getColumnName());
                                if (field != null)
                                {
                                    final Class<?> fieldType = field.getType();

                                    // For some reason, Liquibase wants to drop these default values.
                                    if (fieldType == boolean.class)
                                    {
                                        return true;
                                    }

                                    if (fieldType == int.class)
                                    {
                                        return true;
                                    }

                                    if (fieldType == long.class)
                                    {
                                        return true;
                                    }

                                    if (fieldType == float.class)
                                    {
                                        return true;
                                    }

                                    if (fieldType == double.class)
                                    {
                                        return true;
                                    }
                                }

                                return false;
                            });
                        }
                    }

                    emitChangeSet(output, changeSetsAfter);
                }
            }

            if (database != null && metadata != null)
            {
                Map<String, String> tables = extractTableNames(database, metadata);

                for (Class<?> entity : entities)
                {
                    javax.persistence.Table anno = entity.getAnnotation(javax.persistence.Table.class);
                    if (anno != null)
                    {
                        if (!tables.containsKey(anno.name()
                                                    .toLowerCase()))
                        {
                            throw Exceptions.newRuntimeException(
                                    "Table '%s' is missing, probably an issue with Hibernate silently failing to apply DDL (did you use a reserved word for a table/column/index?)",
                                    anno.name());
                        }
                    }
                }
            }
        }
        finally
        {
            if (database != null)
            {
                database.close();
            }

            if (dataSource != null)
            {
                dataSource.stop();
            }
        }
    }

    private static Field findFieldForColumn(Map<String, TableDetails> lookup,
                                            String tableName,
                                            String columnName)
    {
        TableDetails tableDetails = lookup.get(tableName.toLowerCase());
        if (tableDetails == null)
        {
            return null;
        }

        return tableDetails.columns.get(columnName.toLowerCase());
    }

    private static <T extends Enum<T>> void setDefaultValueForEnum(AddColumnConfig columnConfig,
                                                                   Optio3UpgradeValue anno,
                                                                   Class<?> clz)
    {
        @SuppressWarnings("unchecked") Class<T> clz2 = (Class<T>) clz;

        T val = Enum.valueOf(clz2, anno.value());
        columnConfig.setDefaultValue(val.toString());
    }

    //--//

    public Optio3DataSourceFactory getDataSourceFactory()
    {
        return m_dataSourceFactory;
    }

    public ImmutableList<Class<?>> getEntities()
    {
        return m_entities;
    }

    public SessionFactory getSessionFactory()
    {
        return m_hibernate.getSessionFactory();
    }

    public CompletableFuture<HibernateSearch> getHibernateSearch()
    {
        return m_dataSourceFactory.getHibernateSearch();
    }

    private DatabaseSnapshot getSnapshot(Database database,
                                         DatabaseMetaData metadata)
    {
        try
        {
            DatabaseFactory instance = DatabaseFactory.getInstance();

            final SnapshotControl snapshotControl = new SnapshotControl(database);
            DatabaseSnapshot snapshot = SnapshotGeneratorFactory.getInstance()
                                                                .createSnapshot(CatalogAndSchema.DEFAULT, database, snapshotControl);

            if (metadata != null)
            {
                //
                // Due to https://bugs.mysql.com/bug.php?id=57830, MySQL/MariaDB sometimes return the wrong table name.
                // This happens when you query the metadata for a specific table.
                //
                // To work around the bug, we query the metadata for all the tables
                // and backpatch the Liquibase snapshot to have the correct spelling.
                //
                Map<String, String> fixTableName = extractTableNames(database, metadata);

                for (Table table : snapshot.get(Table.class))
                {
                    String name    = table.getName();
                    String fixName = fixTableName.get(name.toLowerCase());
                    if (fixName != null && !fixName.equals(name))
                    {
                        // Found a problem, fix it!
                        table.setName(fixName);
                    }
                }
            }

            if (m_resetDeferrable)
            {
                //
                // With MySQL, Liquibase sees "deferrable=true" during snapshot creation, even if it's not supported.
                // We have to reset it, otherwise Liquibase will fail on migration.
                //
                for (ForeignKey fk : snapshot.get(ForeignKey.class))
                {
                    fk.setDeferrable(false);
                }
            }

            return snapshot;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private Map<String, String> extractTableNames(Database database,
                                                  DatabaseMetaData metadata) throws
                                                                             SQLException
    {
        Map<String, String> tableNames = Maps.newHashMap();

        ResultSet rs = metadata.getTables(database.getDefaultCatalogName(), database.getDefaultSchemaName(), null, new String[] { "TABLE" });
        while (rs.next())
        {
            String tableName = rs.getString("TABLE_NAME");
            tableNames.put(tableName.toLowerCase(), tableName);
        }
        rs.close();

        return tableNames;
    }

    private List<ChangeSet> generateDiff(final DatabaseSnapshot referenceSnapshot,
                                         final DatabaseSnapshot referenceSnapshot2,
                                         String migrationsRoot) throws
                                                                DatabaseException
    {
        if (migrationsRoot == null)
        {
            migrationsRoot = LocalDateTime.now()
                                          .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        }

        final DiffResult diffResult = DiffGeneratorFactory.getInstance()
                                                          .compare(referenceSnapshot2, referenceSnapshot, CompareControl.STANDARD);

        final DiffToChangeLog diffToChangeLog = new DiffToChangeLog(new DiffOutputControl(false, false, true, null));
        diffToChangeLog.setChangeSetAuthor("Optio3 Dropwizard Shell");
        diffToChangeLog.setDiffResult(diffResult);
        diffToChangeLog.setIdRoot(migrationsRoot);

        List<ChangeSet> res = diffToChangeLog.generateChangeSets();

        //
        // Liquibase emit "DROP INDEX" after "DROP COLUMN" and "DROP TABLE", but DBs delete the indices when you delete the elements of the index.
        // So we have to rearrange the results to avoid errors.
        //
        List<ChangeSet> dropIndexLst      = extractChanges(res, DropIndexChange.class);
        List<ChangeSet> dropColumnLst     = extractChanges(res, DropColumnChange.class);
        List<ChangeSet> dropTableLst      = extractChanges(res, DropTableChange.class);
        List<ChangeSet> dropForeignKeyLst = extractChanges(res, DropForeignKeyConstraintChange.class);

        List<ChangeSet> resRearranged = Lists.newArrayList();
        resRearranged.addAll(res);
        resRearranged.addAll(dropIndexLst);
        resRearranged.addAll(dropForeignKeyLst);
        resRearranged.addAll(dropColumnLst);
        resRearranged.addAll(dropTableLst);

        return resRearranged;
    }

    private void emitChangeSet(OutputStream output,
                               List<ChangeSet> changeSetsAfter) throws
                                                                IOException
    {
        //
        // XMLChangeLogSerializer closes the stream, so we have to wrap System.out to avoid that.
        //
        XMLChangeLogSerializer serializer = new XMLChangeLogSerializer();
        serializer.write(changeSetsAfter, new PrintStream(output)
        {
            @Override
            public void close()
            {
            }
        });
    }

    private <T> void enumerateChanges(List<ChangeSet> changeSets,
                                      Class<T> clz,
                                      Consumer<T> callback)
    {
        for (ChangeSet changeSet : changeSets)
        {
            for (Change change : changeSet.getChanges())
            {
                if (clz.isInstance(change))
                {
                    callback.accept(clz.cast(change));
                }
            }
        }
    }

    private <T> List<ChangeSet> extractChanges(List<ChangeSet> changeSets,
                                               Class<T> clz)
    {
        List<ChangeSet> extracted = Lists.newArrayList();

        Iterator<ChangeSet> it = changeSets.iterator();
        while (it.hasNext())
        {
            ChangeSet changeSet = it.next();
            boolean   extract   = false;

            for (Change change : changeSet.getChanges())
            {
                if (clz.isInstance(change))
                {
                    extract = true;
                    break;
                }
            }

            if (extract)
            {
                it.remove();
                extracted.add(changeSet);
            }
        }

        return extracted;
    }

    private <T> void enumerateAndDeleteChanges(List<ChangeSet> changeSets,
                                               Class<T> clz,
                                               Function<T, Boolean> callback)
    {
        Iterator<ChangeSet> it = changeSets.iterator();
        while (it.hasNext())
        {
            ChangeSet changeSet = it.next();
            boolean   delete    = false;

            for (Change change : changeSet.getChanges())
            {
                if (clz.isInstance(change))
                {
                    delete |= callback.apply(clz.cast(change));
                    if (delete)
                    {
                        break;
                    }
                }
            }

            if (delete)
            {
                it.remove();
            }
        }
    }

    //--//

    public void registerForEvents(AbstractApplicationWithDatabase<?> app)
    {
        SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) getSessionFactory();
        if (sessionFactory != null)
        {
            EntityReferenceLookup lookup = new EntityReferenceLookup(sessionFactory.getMetamodel());
            lookup.dump();

            new ProcessorForCascadeDelete(m_databaseId, sessionFactory, lookup);

            if (m_dataSourceFactory.enableEvents)
            {
                new ProcessorForPostCommitNotification(app, m_databaseId, sessionFactory, lookup);
                m_processorForDatabaseActivity = new ProcessorForDatabaseActivity<>(app, m_databaseId, sessionFactory, lookup);
            }
        }
    }

    public void postEvent(Class<?> entityClass,
                          Serializable id,
                          DbAction action,
                          ZonedDateTime lastUpdate)
    {
        if (m_processorForDatabaseActivity != null)
        {
            m_processorForDatabaseActivity.postHierarchicalEvent(entityClass, id, action, lastUpdate);
        }
    }

    public void drainDatabaseEvents()
    {
        if (m_processorForDatabaseActivity != null)
        {
            m_processorForDatabaseActivity.drainDatabaseEvents();
        }
    }
}
