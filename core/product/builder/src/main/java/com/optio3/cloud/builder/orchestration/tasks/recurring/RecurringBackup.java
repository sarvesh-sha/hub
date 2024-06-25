/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.recurring;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.sleep;
import static com.optio3.asyncawait.CompileTime.wasComputationCancelled;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.annotation.Optio3RecurringProcessor;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.model.customer.CustomerService;
import com.optio3.cloud.builder.model.customer.CustomerServiceBackup;
import com.optio3.cloud.builder.model.deployment.DeploymentGlobalDescriptor;
import com.optio3.cloud.builder.persistence.customer.BackupKind;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceBackupRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.logic.BackgroundActivityScheduler;
import com.optio3.cloud.logic.RecurringActivityHandler;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.DbEvent;
import com.optio3.cloud.persistence.MetadataField;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.ILogger;
import com.optio3.logging.Logger;
import com.optio3.util.ConfigVariables;
import com.optio3.util.IConfigVariable;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.RandomUtils;

@Optio3RecurringProcessor
public class RecurringBackup extends RecurringActivityHandler implements RecurringActivityHandler.ForTable
{
    public static final Logger LoggerInstance = BackgroundActivityScheduler.LoggerInstance.createSubLogger(RecurringBackup.class);

    enum ConfigVariable implements IConfigVariable
    {
        CustomerSysId("CUSTOMER_SYSID"),
        Customer("CUSTOMER"),
        ServiceSysId("SERVICE_SYSID"),
        Service("SERVICE"),
        Timestamp("TIMESTAMP");

        private final String m_variable;

        ConfigVariable(String variable)
        {
            m_variable = variable;
        }

        public String getVariable()
        {
            return m_variable;
        }
    }

    private static final ConfigVariables.Validator<ConfigVariable> s_configValidator        = new ConfigVariables.Validator<>(ConfigVariable.values());
    private static final ConfigVariables.Template<ConfigVariable>  s_template_dailyFailed   = s_configValidator.newTemplate(RecurringBackup.class, "emails/backup/failed_daily.txt", "${", "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_hourlyFailed  = s_configValidator.newTemplate(RecurringBackup.class, "emails/backup/failed_hourly.txt", "${", "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_dailySuccess  = s_configValidator.newTemplate(RecurringBackup.class, "emails/backup/success_daily.txt", "${", "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_hourlySuccess = s_configValidator.newTemplate(RecurringBackup.class, "emails/backup/success_hourly.txt", "${", "}");

    //--//

    @Override
    public Class<?> getEntityClass()
    {
        return CustomerServiceBackupRecord.class;
    }

    @Override
    public boolean shouldTrigger(DbEvent event)
    {
        return true;
    }

    //--//

    @Override
    public Duration startupDelay()
    {
        // Delay first backup 10 minutes after startup.
        return Duration.of(10, ChronoUnit.MINUTES);
    }

    @Override
    public CompletableFuture<ZonedDateTime> process(SessionProvider sessionProvider) throws
                                                                                     Exception
    {
        final ZonedDateTime now                  = TimeUtils.now();
        final ZonedDateTime nowTruncated         = now.truncatedTo(ChronoUnit.HOURS);
        final ZonedDateTime purgeHourlyOlderThan = nowTruncated.minus(24, ChronoUnit.HOURS);
        final ZonedDateTime purgeDailyOlderThan  = nowTruncated.minus(7, ChronoUnit.DAYS);

        BuilderConfiguration cfg = sessionProvider.getServiceNonNull(BuilderConfiguration.class);
        if (!cfg.developerSettings.disableAutomaticBackups)
        {
            DeploymentGlobalDescriptor.Settings settings = new DeploymentGlobalDescriptor.Settings();
            settings.loadServices = true;
            settings.loadBackups  = true;

            DeploymentGlobalDescriptor globalDescriptor = sessionProvider.computeInReadOnlySession((sessionHolder) -> DeploymentGlobalDescriptor.get(sessionHolder, settings));

            for (CustomerService svc : globalDescriptor.services.values())
            {
                try (SessionHolder holder = sessionProvider.newSessionWithTransaction())
                {
                    processService(holder, globalDescriptor, svc, nowTruncated, purgeHourlyOlderThan, purgeDailyOlderThan);

                    holder.commit();
                }
                catch (Throwable t)
                {
                    LoggerInstance.error("Encountered a problem processing backups for '%s': %s", svc.name, t);
                }

                // Yield processor.
                await(sleep(1, TimeUnit.MILLISECONDS));

                if (wasComputationCancelled())
                {
                    break;
                }
            }
        }

        return wrapAsync(now.plus(7, ChronoUnit.MINUTES)); // Prime number, to avoid hitting the check at the same time every hour.
    }

    @Override
    public void shutdown()
    {
        // Nothing to do.
    }

    //--//

    private void processService(SessionHolder sessionHolder,
                                DeploymentGlobalDescriptor globalDescriptor,
                                CustomerService svc,
                                ZonedDateTime nowTruncated,
                                ZonedDateTime purgeHourlyOlderThan,
                                ZonedDateTime purgeDailyOlderThan)
    {
        RecordLocked<CustomerServiceRecord> lock_svc = sessionHolder.getEntityWithLock(CustomerServiceRecord.class, svc.sysId, 2, TimeUnit.MINUTES);
        CustomerServiceRecord               rec_svc  = lock_svc.get();

        if (!rec_svc.isReadyForBackup(sessionHolder))
        {
            return;
        }

        if (rec_svc.getCurrentActivityIfNotDone() != null)
        {
            // Service is busy, we'll try again later.
            return;
        }

        ILogger loggerInstance = CustomerServiceRecord.buildContextualLogger(LoggerInstance, sessionHolder.createLocator(rec_svc));

        ZonedDateTime lastHourly = null;
        ZonedDateTime lastDaily  = null;

        for (CustomerServiceBackup backup : svc.rawBackups)
        {
            ZonedDateTime time_backup = backup.createdOn;

            switch (backup.trigger)
            {
                case Upgrade:
                    if (purgeDailyOlderThan.isAfter(time_backup))
                    {
                        removeStaleBackup(loggerInstance, sessionHolder, svc, backup);
                        continue;
                    }

                    break;

                case Hourly:
                    if (purgeHourlyOlderThan.isAfter(time_backup))
                    {
                        removeStaleBackup(loggerInstance, sessionHolder, svc, backup);
                        continue;
                    }
                    break;

                case Daily:
                    if (purgeDailyOlderThan.isAfter(time_backup))
                    {
                        removeStaleBackup(loggerInstance, sessionHolder, svc, backup);
                        continue;
                    }

                    lastDaily = TimeUtils.updateIfAfter(lastDaily, time_backup);
                    break;
            }

            lastHourly = TimeUtils.updateIfAfter(lastHourly, time_backup);
        }

        MetadataMap metadata = rec_svc.getMetadata();

        if (shouldCreateBackup(lastDaily, nowTruncated, 1, ChronoUnit.DAYS))
        {
            queueBackup(loggerInstance, lock_svc, metadata, BackupKind.Daily);
        }
        else if (shouldCreateBackup(lastHourly, nowTruncated, 6, ChronoUnit.HOURS))
        {
            queueBackup(loggerInstance, lock_svc, metadata, BackupKind.Hourly);
        }

        if (shouldSendNotification(metadata, lastDaily, nowTruncated, CustomerServiceRecord.WellKnownMetadata.backupFailureDaily, 1, ChronoUnit.DAYS))
        {
            if (!CustomerServiceRecord.WellKnownMetadata.emailFailureDaily.get(metadata))
            {
                sendEmail(sessionHolder, rec_svc, BuilderApplication.EmailFlavor.Warning, "Backup failure", s_template_dailyFailed);

                CustomerServiceRecord.WellKnownMetadata.emailFailureDaily.put(metadata, true);
            }
        }
        else
        {
            if (CustomerServiceRecord.WellKnownMetadata.emailFailureDaily.get(metadata))
            {
                sendEmail(sessionHolder, rec_svc, BuilderApplication.EmailFlavor.Info, "Backup success", s_template_dailySuccess);

                CustomerServiceRecord.WellKnownMetadata.emailFailureDaily.put(metadata, false);
            }
        }

        if (shouldSendNotification(metadata, lastHourly, nowTruncated, CustomerServiceRecord.WellKnownMetadata.backupFailureHourly, 6, ChronoUnit.HOURS))
        {
            if (!CustomerServiceRecord.WellKnownMetadata.emailFailureHourly.get(metadata))
            {
                sendEmail(sessionHolder, rec_svc, BuilderApplication.EmailFlavor.Warning, "Backup failure", s_template_hourlyFailed);

                CustomerServiceRecord.WellKnownMetadata.emailFailureHourly.put(metadata, true);
            }
        }
        else
        {
            if (CustomerServiceRecord.WellKnownMetadata.emailFailureHourly.get(metadata))
            {
                sendEmail(sessionHolder, rec_svc, BuilderApplication.EmailFlavor.Info, "Backup success", s_template_hourlySuccess);

                CustomerServiceRecord.WellKnownMetadata.emailFailureHourly.put(metadata, false);
            }
        }

        rec_svc.setMetadata(metadata);
    }

    private static boolean shouldCreateBackup(ZonedDateTime time_backup,
                                              ZonedDateTime now,
                                              int amount,
                                              ChronoUnit unit)
    {
        if (time_backup == null)
        {
            return true;
        }

        ZonedDateTime next = time_backup.plus(amount, unit);
        return !now.isBefore(next);
    }

    private static boolean shouldSendNotification(MetadataMap metadata,
                                                  ZonedDateTime lastBackup,
                                                  ZonedDateTime now,
                                                  MetadataField<ZonedDateTime> backupFlavor,
                                                  int amount,
                                                  ChronoUnit unit)
    {
        ZonedDateTime previousFailureWarning = backupFlavor.get(metadata);
        if (previousFailureWarning == null)
        {
            // If we just started, give it as least one hour before alerting.
            previousFailureWarning = now.plus(1, ChronoUnit.HOURS);
        }

        ZonedDateTime nextFailureWarning;

        if (lastBackup == null)
        {
            nextFailureWarning = previousFailureWarning;
        }
        else
        {
            // Push the notification 2N time units from last success.
            nextFailureWarning = lastBackup.plus(amount * 2, unit);

            if (nextFailureWarning.isBefore(previousFailureWarning))
            {
                nextFailureWarning = previousFailureWarning;
            }
        }

        backupFlavor.put(metadata, nextFailureWarning);

        return nextFailureWarning.isBefore(now);
    }

    private void queueBackup(ILogger loggerInstance,
                             RecordLocked<CustomerServiceRecord> lock_svc,
                             MetadataMap metadata,
                             BackupKind trigger)
    {
        CustomerServiceRecord rec_svc  = lock_svc.get();
        CustomerRecord        rec_cust = rec_svc.getCustomer();

        try
        {
            if (rec_svc.getCurrentActivityIfNotDone() == null)
            {
                final ZonedDateTime now = TimeUtils.now();

                // Use a random delay to decide when to schedule the backup, to avoid starting all backups at the same time.
                ZonedDateTime backupDelay = CustomerServiceRecord.WellKnownMetadata.backupDelay.get(metadata);
                if (backupDelay == null)
                {
                    backupDelay = now.plus(RandomUtils.nextInt(10, 40), ChronoUnit.MINUTES);

                    loggerInstance.info("Delaying %s backup on '%s # %s' until %s", trigger, rec_cust.getName(), rec_svc.getName(), backupDelay);
                }

                if (now.isAfter(backupDelay))
                {
                    CustomerServiceRecord.WellKnownMetadata.backupDelay.remove(metadata);

                    loggerInstance.info("Starting %s backup on '%s # %s'", trigger, rec_cust.getName(), rec_svc.getName());

                    rec_svc.startBackup(lock_svc, trigger, true);
                }
                else
                {
                    CustomerServiceRecord.WellKnownMetadata.backupDelay.put(metadata, backupDelay);
                }
            }
        }
        catch (InvalidStateException e1)
        {
            // We get this when something else is working on the service, ignore...
        }
        catch (Exception e2)
        {
            loggerInstance.error("Failed to start %s backup on '%s # %s': %s", trigger, rec_cust.getName(), rec_svc.getName(), e2);
        }
    }

    private void removeStaleBackup(ILogger loggerInstance,
                                   SessionHolder sessionHolder,
                                   CustomerService svc,
                                   CustomerServiceBackup backup)
    {
        try
        {
            CustomerServiceBackupRecord rec_backup = sessionHolder.getEntityOrNull(CustomerServiceBackupRecord.class, backup.sysId);
            if (rec_backup == null)
            {
                // Already gone.
                return;
            }

            if (rec_backup.isPendingTransfer())
            {
                return;
            }

            loggerInstance.info("Removing stale %s backup '%s/%s'", backup.trigger, svc.name, backup.fileId);

            final RecordHelper<CustomerServiceBackupRecord> helper_backup = sessionHolder.createHelper(CustomerServiceBackupRecord.class);

            try (ValidationResultsHolder validation = new ValidationResultsHolder(sessionHolder, false, false))
            {
                rec_backup.remove(validation, helper_backup);
            }
        }
        catch (Exception e)
        {
            loggerInstance.error("Failed to delete stale %s backup '%s': %s", backup.trigger, backup.fileId, e);
        }
    }

    private void sendEmail(SessionHolder sessionHolder,
                           CustomerServiceRecord rec_svc,
                           BuilderApplication.EmailFlavor flavor,
                           String emailSubject,
                           ConfigVariables.Template<ConfigVariable> emailTemplate)
    {
        BuilderApplication app = sessionHolder.getServiceNonNull(BuilderApplication.class);

        ConfigVariables<ConfigVariable> parameters = emailTemplate.allocate();

        CustomerRecord rec_cust = rec_svc.getCustomer();

        parameters.setValue(ConfigVariable.CustomerSysId, rec_cust.getSysId());
        parameters.setValue(ConfigVariable.Customer, rec_cust.getName());
        parameters.setValue(ConfigVariable.ServiceSysId, rec_svc.getSysId());
        parameters.setValue(ConfigVariable.Service, rec_svc.getName());
        parameters.setValue(ConfigVariable.Timestamp, TimeUtils.now());

        app.sendEmailNotification(flavor, String.format("%s - %s", rec_svc.getName(), emailSubject), parameters);
    }
}
