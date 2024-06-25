/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.db;

import java.net.URL;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.RuntimeEnvironment;
import liquibase.changelog.ChangeLogHistoryServiceFactory;
import liquibase.changelog.ChangeLogIterator;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.RanChangeSet;
import liquibase.changelog.filter.ContextChangeSetFilter;
import liquibase.changelog.filter.DbmsChangeSetFilter;
import liquibase.changelog.filter.IgnoreChangeSetFilter;
import liquibase.changelog.filter.LabelChangeSetFilter;
import liquibase.changelog.filter.NotRanChangeSetFilter;
import liquibase.changelog.visitor.ChangeLogSyncVisitor;
import liquibase.database.Database;
import liquibase.database.ObjectQuotingStrategy;
import liquibase.exception.LiquibaseException;
import liquibase.exception.LockException;
import liquibase.lockservice.LockService;
import liquibase.lockservice.LockServiceFactory;
import liquibase.parser.ChangeLogParser;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ResourceAccessor;

class Optio3Liquibase extends Liquibase implements AutoCloseable
{
    private final Contexts           m_contexts;
    private final LabelExpression    m_labelExpression;
    private final RuntimeEnvironment m_runtimeEnvironment;
    private final LockService        m_lockService;

    Optio3Liquibase(ResourceAccessor resourceAccessor,
                    Database database) throws
                                       LockException
    {
        super((DatabaseChangeLog) null, resourceAccessor, database);

        m_contexts = new Contexts();
        m_labelExpression = new LabelExpression();
        m_runtimeEnvironment = new RuntimeEnvironment(database, m_contexts, m_labelExpression);

        ChangeLogParameters changeLogParameters = getChangeLogParameters();
        changeLogParameters.setContexts(m_contexts);
        changeLogParameters.setLabels(m_labelExpression);

        m_lockService = LockServiceFactory.getInstance()
                                          .getLockService(database);

        m_lockService.waitForLock();
    }

    @Override
    public void close() throws
                        Exception
    {
        m_lockService.releaseLock();
        resetServices();
    }

    public static String generateMigrationPath(String resourcePrefix,
                                               String db,
                                               int revisionLevel,
                                               int versionNumber)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("/migrations");

        if (resourcePrefix != null)
        {
            sb.append("_");
            sb.append(resourcePrefix);
        }

        // Skip legacy level.
        if (revisionLevel > 1)
        {
            sb.append("-rev");
            sb.append(revisionLevel);
        }

        sb.append("/");
        sb.append(db);
        sb.append("_v");
        sb.append(versionNumber);
        sb.append(".xml");

        return sb.toString();
    }

    //--//

    public static class MigrationEntry
    {
        public int    versionNumber;
        public String resource;
    }

    public static class MigrationLevel
    {
        public       int                  revisionLevel;
        public final List<MigrationEntry> entries = Lists.newArrayList();
        public       MigrationLevel       previousLevel;

        public boolean hasAlreadyRun(List<RanChangeSet> ranSets)
        {
            Set<String> files = Sets.newHashSet();
            for (MigrationEntry entry : entries)
            {
                files.add(entry.resource);
            }

            for (RanChangeSet ranSet : ranSets)
            {
                if (files.contains(ranSet.getChangeLog()))
                {
                    return true;
                }
            }

            return false;
        }
    }

    public MigrationLevel collectMigrationFiles(String resourcePrefix,
                                                String db)
    {
        MigrationLevel chain = null;

        for (int revisionLevel = 1; ; revisionLevel++)
        {
            var migrationLevel = new MigrationLevel();
            migrationLevel.revisionLevel = revisionLevel;

            for (int versionNumber = 1; ; versionNumber++)
            {
                // Select the correct migration file based on database flavor.
                String resource = generateMigrationPath(resourcePrefix, db, revisionLevel, versionNumber);

                URL url = getClass().getResource(resource);
                if (url == null)
                {
                    break;
                }

                var entry = new MigrationEntry();
                entry.versionNumber = versionNumber;
                entry.resource = resource;

                migrationLevel.entries.add(entry);
            }

            if (migrationLevel.entries.isEmpty())
            {
                break;
            }

            if (chain != null)
            {
                migrationLevel.previousLevel = chain;
            }

            chain = migrationLevel;
        }

        return chain;
    }

    public boolean migrate(MigrationLevel migrationLevel,
                           int targetRevisionLevel,
                           int targetVersionNumber) throws
                                                    Exception
    {
        MigrationLevel previousLevel = migrationLevel.previousLevel;
        if (previousLevel == null)
        {
            //
            // No previous level, just apply the migration files.
            //
            return migrateNoChecks(migrationLevel, targetRevisionLevel, targetVersionNumber, false);
        }

        if (targetRevisionLevel == migrationLevel.revisionLevel)
        {
            //
            // This is the target level, only run our migrations.
            //
            migrateNoChecks(migrationLevel, targetRevisionLevel, targetVersionNumber, false);
            return true;
        }

        List<RanChangeSet> ranSets = database.getRanChangeSetList();
        if (migrationLevel.hasAlreadyRun(ranSets))
        {
            //
            // There's a previous level, but we already applied some of the entries in this level, just process this level.
            //
            return migrateNoChecks(migrationLevel, targetRevisionLevel, targetVersionNumber, false);
        }

        if (ranSets.isEmpty())
        {
            //
            // Empty DB, just process this level.
            //
            return migrateNoChecks(migrationLevel, targetRevisionLevel, targetVersionNumber, false);
        }

        //
        // Make sure the previous level is all up-to-date.
        //
        if (migrate(previousLevel, targetRevisionLevel, targetVersionNumber))
        {
            // Hit the target migration, exit.
            return true;
        }

        //
        // Delete all the previous change logs.
        //
        for (MigrationEntry migrationEntry : previousLevel.entries)
        {
            DatabaseChangeLog changeLog = loadChangeLog(migrationEntry.resource);
            remove(changeLog);
        }

        //
        // Apply the entries in this level, but just marking the first one, since it's the roll-up from the previous level.
        //
        return migrateNoChecks(migrationLevel, targetRevisionLevel, targetVersionNumber, true);
    }

    private boolean migrateNoChecks(MigrationLevel migrationLevel,
                                    int targetRevisionLevel,
                                    int targetVersionNumber,
                                    boolean justMarkFirstEntry) throws
                                                                Exception
    {
        for (MigrationEntry migrationEntry : migrationLevel.entries)
        {
            if (targetRevisionLevel == migrationLevel.revisionLevel && targetVersionNumber == migrationEntry.versionNumber)
            {
                // If we are trying to generate a new migration file, stop at the target version.
                return true;
            }

            DatabaseChangeLog changeLog = loadChangeLog(migrationEntry.resource);
            if (justMarkFirstEntry)
            {
                justMarkFirstEntry = false;
                updateNoRun(changeLog);
            }
            else
            {
                update(changeLog);
            }
        }

        return false;
    }

    public DatabaseChangeLog loadChangeLog(String migration) throws
                                                             LiquibaseException
    {
        ResourceAccessor resourceAccessor = getResourceAccessor();

        ChangeLogParserFactory changeLogParserFactory = ChangeLogParserFactory.getInstance();
        ChangeLogParser        parser                 = changeLogParserFactory.getParser(migration, resourceAccessor);

        return parser.parse(migration, getChangeLogParameters(), resourceAccessor);
    }

    public DatabaseChangeLog collectChangeLogs(List<String> migrations) throws
                                                                        LiquibaseException
    {
        DatabaseChangeLog changeLogRollup = null;

        for (String migration : migrations)
        {
            DatabaseChangeLog databaseChangeLog = loadChangeLog(migration);

            if (changeLogRollup == null)
            {
                changeLogRollup = databaseChangeLog;
            }
            else
            {
                for (ChangeSet changeSet : databaseChangeLog.getChangeSets())
                {
                    changeLogRollup.addChangeSet(changeSet);
                }
            }
        }

        return changeLogRollup;
    }

    public void update(DatabaseChangeLog changeLog) throws
                                                    Exception
    {
        try
        {
            checkLiquibaseTables(true, changeLog, m_contexts, m_labelExpression);

            ChangeLogHistoryServiceFactory.getInstance()
                                          .getChangeLogService(database)
                                          .generateDeploymentId();

            changeLog.validate(database, m_contexts, m_labelExpression);

            ChangeLogIterator changeLogIterator = getStandardChangelogIterator(m_contexts, m_labelExpression, changeLog);

            changeLogIterator.run(createUpdateVisitor(), m_runtimeEnvironment);
        }
        finally
        {
            database.setObjectQuotingStrategy(ObjectQuotingStrategy.LEGACY);
        }
    }

    public void updateNoRun(DatabaseChangeLog changeLog) throws
                                                         Exception
    {
        try
        {
            checkLiquibaseTables(true, changeLog, m_contexts, m_labelExpression);

            ChangeLogHistoryServiceFactory.getInstance()
                                          .getChangeLogService(database)
                                          .generateDeploymentId();

            changeLog.validate(database, m_contexts, m_labelExpression);

            ChangeLogIterator logIterator = new ChangeLogIterator(changeLog,
                                                                  new NotRanChangeSetFilter(database.getRanChangeSetList()),
                                                                  new ContextChangeSetFilter(m_contexts),
                                                                  new LabelChangeSetFilter(m_labelExpression),
                                                                  new DbmsChangeSetFilter(database),
                                                                  new IgnoreChangeSetFilter());

            logIterator.run(new ChangeLogSyncVisitor(database), m_runtimeEnvironment);
        }
        finally
        {
            database.setObjectQuotingStrategy(ObjectQuotingStrategy.LEGACY);
        }
    }

    public void remove(DatabaseChangeLog changeLog) throws
                                                    Exception
    {
        for (ChangeSet changeSet : changeLog.getChangeSets())
        {
            database.removeRanStatus(changeSet);
        }
    }
}
