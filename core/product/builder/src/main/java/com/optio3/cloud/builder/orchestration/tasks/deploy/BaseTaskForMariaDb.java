/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;

import com.optio3.archive.TarBuilder;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.persistence.customer.BackupKind;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceBackupRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.customer.EmbeddedDatabaseConfiguration;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.infra.WellKnownDockerImageLabel;
import com.optio3.util.Base64EncodedValue;
import com.optio3.util.ConfigVariables;
import com.optio3.util.IConfigVariable;
import com.optio3.util.TimeUtils;

public abstract class BaseTaskForMariaDb extends BaseDeployTask
{
    public enum ConfigVariable implements IConfigVariable
    {
        RootPassword("ROOT_PASSWORD"),
        DatabaseName("DATABASE_NAME"),
        DbHost("DB_HOST");

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
    private static final ConfigVariables.Template<ConfigVariable>  s_template_YamlFromBuild = s_configValidator.newTemplate(BaseTaskForMariaDb.class, null, "${", "}");

    //--//

    public static final String MARIADB_DATABASE_LOCATION = "mysql";

    public RecordLocator<RegistryTaggedImageRecord> loc_image;
    public String                                   tag_image;

    //--//

    protected CompletableFuture<RecordLocator<CustomerServiceBackupRecord>> beginTransferBackupFromMariaDb(String dockerId,
                                                                                                           Path dir,
                                                                                                           BackupKind trigger) throws
                                                                                                                               Exception
    {
        DeployLogicForAgent agentLogic  = getLogicForAgent();
        String              fileOnAgent = await(agentLogic.exportContainerFileSystemToTar(dockerId, dir, true, true, 60));

        try
        {
            long length = await(agentLogic.fileSizeOnAgent(fileOnAgent));

            String finalFileOnAgent = fileOnAgent;

            var loc_backup = withLocator(getTargetServiceLocator(), (sessionHolder, rec_svc) ->
            {
                CustomerServiceBackupRecord rec_backup = CustomerServiceBackupRecord.newInstance(rec_svc, trigger, DEFAULT_TIMESTAMP.format(TimeUtils.now()), length, finalFileOnAgent);

                EmbeddedDatabaseConfiguration db = rec_svc.getDbConfiguration();
                rec_backup.putMetadata(CustomerServiceBackupRecord.WellKnownMetadata.db_mode, db.getMode());

                rec_backup.saveSettings();

                sessionHolder.persistEntity(rec_backup);

                TaskForBackupTransfer.scheduleTask(sessionHolder, getTargetHostNoLock(sessionHolder), rec_backup, Duration.of(2, ChronoUnit.HOURS));

                return sessionHolder.createLocator(rec_backup);
            });

            // Download is pending, don't delete file.
            fileOnAgent = null;

            return wrapAsync(loc_backup);
        }
        finally
        {
            await(agentLogic.deleteFileOnAgent(fileOnAgent));
        }
    }

    protected CompletableFuture<Boolean> transferBackupFromMariaDb(String dockerId,
                                                                   Path dir,
                                                                   BackupKind trigger,
                                                                   DeployLogicForAgent.TransferProgress<RecordLocator<CustomerServiceBackupRecord>> transferProgress) throws
                                                                                                                                                                      Exception
    {
        DeployLogicForAgent agentLogic = getLogicForAgent();
        return agentLogic.readContainerFileSystemToTar(dockerId, dir, true, true, 60, transferProgress, (tmpFile) ->
        {
            transferProgress.context = withLocator(getTargetServiceLocator(), (sessionHolder, rec_svc) ->
            {
                CustomerServiceBackupRecord rec_backup = CustomerServiceBackupRecord.newInstance(rec_svc, trigger, DEFAULT_TIMESTAMP.format(TimeUtils.now()), tmpFile.length(), null);

                EmbeddedDatabaseConfiguration db = rec_svc.getDbConfiguration();
                rec_backup.putMetadata(CustomerServiceBackupRecord.WellKnownMetadata.db_mode, db.getMode());

                rec_backup.saveFileToCloud(appConfig.credentials, tmpFile);

                rec_backup.saveSettings();

                sessionHolder.persistEntity(rec_backup);

                return sessionHolder.createLocator(rec_backup);
            });

            return true;
        });
    }

    protected CompletableFuture<Boolean> transferBackupToMariaDb(RecordLocator<CustomerServiceBackupRecord> loc_backup,
                                                                 String containerId,
                                                                 Path targetPath,
                                                                 DeployLogicForAgent.TransferProgress<RecordLocator<CustomerServiceBackupRecord>> transferProgress) throws
                                                                                                                                                                    Exception
    {
        var model_backup = withLocatorReadonly(loc_backup, (sessionHolder, rec_backup) ->
        {
            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_backup);
        });

        DeployLogicForAgent agentLogic = getLogicForAgent();

        if (model_backup != null && model_backup.fileIdOnAgent != null)
        {
            long length = await(agentLogic.fileSizeOnAgent(model_backup.fileIdOnAgent));
            if (length == model_backup.fileSize)
            {
                return agentLogic.importContainerFileSystemFromTar(containerId, targetPath, model_backup.fileIdOnAgent, true, 60);
            }

            // Either the file is not longer there, or it's not the correct one.
            withLocator(loc_backup, (sessionHolder, rec_backup) ->
            {
                rec_backup.setFileIdOnAgent(null);
                rec_backup.setPendingTransfer(false);
            });
        }

        return agentLogic.restoreFileSystem(containerId, targetPath, 60, (file) ->
        {
            withLocatorReadonly(loc_backup, (sessionHolder, rec_backup) ->
            {
                rec_backup.loadFileFromCloud(appConfig.credentials, file);
            });
        }, transferProgress);
    }

    //--//

    protected void generateConfiguration(File file) throws
                                                    Exception
    {
        callInReadOnlySession(sessionHolder ->
                              {
                                  try (FileOutputStream stream = new FileOutputStream(file))
                                  {
                                      try (TarBuilder builder = new TarBuilder(stream, true))
                                      {
                                          CustomerServiceRecord         rec_svc = getTargetService(sessionHolder);
                                          EmbeddedDatabaseConfiguration db      = rec_svc.getDbConfiguration();

                                          ConfigVariables<ConfigVariable> parameters = s_template_YamlFromBuild.allocate();

                                          parameters.setValue(ConfigVariable.RootPassword, appConfig.decryptDatabasePassword(db.getDatabasePassword()));
                                          parameters.setValue(ConfigVariable.DatabaseName, db.getDatabaseName());
                                          parameters.setValue(ConfigVariable.DbHost, db.getServerName());

                                          RegistryTaggedImageRecord rec_taggedImage = sessionHolder.fromLocator(loc_image);
                                          Base64EncodedValue        config          = rec_taggedImage.findLabelOrDefault(WellKnownDockerImageLabel.ConfigTemplate, null);
                                          String                    input           = new String(config.getValue());

                                          String scriptFile = parameters.convert(input);

                                          builder.addAsString(null, "setup.sh", scriptFile, 0444);
                                      }
                                  }
                              });
    }
}
