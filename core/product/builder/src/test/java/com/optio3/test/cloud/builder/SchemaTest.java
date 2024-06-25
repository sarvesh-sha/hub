/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.persistence.LockTimeoutException;
import javax.persistence.PersistenceException;

import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.HostRemoter;
import com.optio3.cloud.builder.model.customer.CustomerService;
import com.optio3.cloud.builder.model.customer.CustomerServiceBackup;
import com.optio3.cloud.builder.model.customer.RoleAndArchitectureWithImage;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.model.jobs.output.RegistryTaggedImage;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.customer.BackupKind;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceBackupRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.customer.DatabaseMode;
import com.optio3.cloud.builder.persistence.customer.EmbeddedDatabaseConfiguration;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryImageRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.builder.persistence.worker.DockerContainerRecord;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.builder.persistence.worker.ManagedDirectoryRecord;
import com.optio3.cloud.builder.persistence.worker.MappedDockerVolumeRecord;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.db.Optio3DataSourceFactory;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.EncryptedPayload;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import com.optio3.util.TimeUtils;
import org.assertj.core.util.Lists;
import org.junit.ClassRule;
import org.junit.Test;

public class SchemaTest extends Optio3Test
{
    static RecordLocator<HostRecord>            loc_host;
    static RecordLocator<DockerContainerRecord> loc_container;

    @ClassRule
    public static final TestApplicationWithDbRule<BuilderApplication, BuilderConfiguration> applicationRule = new TestApplicationWithDbRule<>(BuilderApplication.class,
                                                                                                                                              "builder-test.yml",
                                                                                                                                              (configuration) ->
                                                                                                                                              {
                                                                                                                                                  Optio3DataSourceFactory dataSourceFactory = configuration.getDataSourceFactory();
                                                                                                                                                  dataSourceFactory.enableEvents  = true;
                                                                                                                                                  dataSourceFactory.skipMigration = false; // Test schema migration
                                                                                                                                              },
                                                                                                                                              null);

    @Test
    @TestOrder(10)
    public void testCreate()
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            HostRecord rec_host = applicationRule.getApplication()
                                                 .getCurrentHost(holder);

            RepositoryRecord rec_repo = new RepositoryRecord();
            rec_repo.setName("test");
            rec_repo.setGitUrl("https://github.com/optio3/core.git");
            holder.persistEntity(rec_repo);

            //--//

            ManagedDirectoryRecord rec_repoDb = ManagedDirectoryRecord.newInstance(rec_host, Paths.get("/data/db"));
            holder.persistEntity(rec_repoDb);

            ManagedDirectoryRecord rec_repoWork = ManagedDirectoryRecord.newInstance(rec_host, Paths.get("/data/work"));
            holder.persistEntity(rec_repoWork);

            //--//

            DockerContainerRecord rec_container = DockerContainerRecord.newInstance(rec_host);
            rec_container.setDockerId("foo");
            rec_container.setStartedOn(TimeUtils.now());
            holder.persistEntity(rec_container);

            MappedDockerVolumeRecord rec_mappedVolumeDb = MappedDockerVolumeRecord.newInstance(rec_container);
            rec_mappedVolumeDb.setPath("/var/data/db");
            rec_mappedVolumeDb.setDirectory(rec_repoDb);
            holder.persistEntity(rec_mappedVolumeDb);

            MappedDockerVolumeRecord rec_mappedVolumeWork = MappedDockerVolumeRecord.newInstance(rec_container);
            rec_mappedVolumeWork.setPath("/var/data/work");
            rec_mappedVolumeWork.setDirectory(rec_repoWork);
            holder.persistEntity(rec_mappedVolumeWork);

            holder.flush();

            holder.commit();

            loc_host      = holder.createLocator(rec_host);
            loc_container = holder.createLocator(rec_container);
        }

        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            HostRecord rec_host = holder.fromLocator(loc_host);

            for (ManagedDirectoryRecord rec_child : Lists.newArrayList(rec_host.getDirectories()))
            {
                System.out.printf("ManagedDirectoryRecord: %s%n", rec_child.getSysId());

                for (MappedDockerVolumeRecord rec_child2 : rec_child.getMappedIn())
                {
                    System.out.printf("MappedDockerVolumeRecord: %s%n", rec_child2.getSysId());
                }
            }
        }
    }

    @Test
    @TestOrder(20)
    public void testFailedDelete1()
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            HostRecord rec_host = holder.fromLocator(loc_host);

            holder.deleteEntity(rec_host);

            assertFailure(PersistenceException.class, () ->
            {
                holder.flush();
            });
        }
    }

    @Test
    @TestOrder(21)
    public void testFailedDelete2()
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            HostRecord rec_host = holder.fromLocator(loc_host);

            for (ManagedDirectoryRecord rec_child : Lists.newArrayList(rec_host.getDirectories()))
            {
                assertFailure(InvalidStateException.class, () ->
                {
                    try (ValidationResultsHolder validation = new ValidationResultsHolder(holder, null, false))
                    {
                        rec_child.deleteRecursively(getHostRemoter(), validation);
                    }
                });
            }
        }
    }

    @Test
    @TestOrder(30)
    public void testDelete1() throws
                              Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            HostRecord rec_host = holder.fromLocator(loc_host);

            try (ValidationResultsHolder validation = new ValidationResultsHolder(holder, null, true))
            {
                rec_host.deleteRecursively(getHostRemoter(), validation);
            }

            holder.flush();
        }
    }

    @Test
    @TestOrder(31)
    public void testDelete2() throws
                              Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            DockerContainerRecord rec_container = holder.fromLocator(loc_container);
            try (ValidationResultsHolder validation = new ValidationResultsHolder(holder, null, false))
            {
                rec_container.deleteRecursively(getHostRemoter(), validation);
            }

            HostRecord rec_host = holder.fromLocator(loc_host);
            for (ManagedDirectoryRecord rec_child : Lists.newArrayList(rec_host.getDirectories()))
            {
                try (ValidationResultsHolder validationChild = new ValidationResultsHolder(holder, null, false))
                {
                    rec_child.deleteRecursively(getHostRemoter(), validationChild);
                }
            }

            holder.deleteEntity(rec_host);

            holder.flush();
        }
    }

    @Test
    @TestOrder(100)
    public void testCustomer1() throws
                                Exception
    {
        RecordLocator<RegistryTaggedImageRecord>   loc_tag;
        RecordLocator<CustomerServiceRecord>       loc_svc;
        RecordLocator<CustomerServiceBackupRecord> loc_backup;

        System.out.println("testCustomer1 - create Service");
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            CustomerRecord rec_cust = new CustomerRecord();
            rec_cust.setCloudId("cloudId");
            rec_cust.setName("cust");
            holder.persistEntity(rec_cust);

            CustomerServiceRecord rec_svc = CustomerServiceRecord.newInstance(rec_cust);
            rec_svc.setName("test1");
            rec_svc.setUrl("http://localhost:8080");

            EmbeddedDatabaseConfiguration dbConfig = new EmbeddedDatabaseConfiguration();
            dbConfig.setMode(DatabaseMode.H2OnDisk);
            dbConfig.setDatabaseName("hub_db");
            dbConfig.setDatabaseUser("sa");
            dbConfig.setDatabasePassword(EncryptedPayload.build("12345678", "test"));
            rec_svc.setDbConfiguration(dbConfig);

            holder.persistEntity(rec_svc);
            loc_svc = holder.createLocator(rec_svc);

            holder.commit();
        }

        System.out.println("testCustomer1 - update DB configuration");
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            CustomerServiceRecord rec_svc = holder.fromLocator(loc_svc);

            EmbeddedDatabaseConfiguration dbConfig = rec_svc.getDbConfiguration();
            dbConfig.setMode(DatabaseMode.H2OnDisk);
            dbConfig.setDatabaseName("hub_db2");
            dbConfig.setDatabaseUser("sa2");
            dbConfig.setDatabasePassword(EncryptedPayload.build("12345678", "test"));

            holder.commit();
        }

        System.out.println("testCustomer1 - check DB configuration");
        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            CustomerServiceRecord rec_svc = holder.fromLocator(loc_svc);

            EmbeddedDatabaseConfiguration dbConfig = rec_svc.getDbConfiguration();
            assertEquals(DatabaseMode.H2OnDisk, dbConfig.getMode());
            assertEquals("hub_db2", dbConfig.getDatabaseName());
            assertEquals("sa2", dbConfig.getDatabaseUser());
            assertEquals("test",
                         dbConfig.getDatabasePassword()
                                 .decrypt("12345678"));
        }

        System.out.println("testCustomer1 - create Image");
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RegistryImageRecord rec_img = RegistryImageRecord.newInstance("imageSha", null, DockerImageArchitecture.X86);
            holder.persistEntity(rec_img);

            RegistryTaggedImageRecord rec_taggedImage = RegistryTaggedImageRecord.newInstance(null, rec_img, "imageTag");
            holder.persistEntity(rec_taggedImage);
            loc_tag = holder.createLocator(rec_taggedImage);

            holder.commit();
        }

        System.out.println("testCustomer1 - bind Service to Image");
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RegistryTaggedImageRecord rec_taggedImage = holder.fromLocator(loc_tag);
            CustomerServiceRecord     rec_svc         = holder.fromLocator(loc_svc);

            List<RoleAndArchitectureWithImage> roleImages = rec_svc.getRoleImages();

            RoleAndArchitectureWithImage.add(roleImages, DeploymentRole.test, DockerImageArchitecture.X86, TypedRecordIdentity.newTypedInstance(rec_taggedImage));

            rec_svc.setRoleImages(roleImages);

            holder.commit();
        }

        System.out.println("testCustomer1 - list Images for Service");
        try (SessionHolder sessionHolder = applicationRule.openSessionWithoutTransaction())
        {
            CustomerServiceRecord rec_svc = sessionHolder.fromLocator(loc_svc);

            List<RoleAndArchitectureWithImage> roleImages = rec_svc.getRoleImages();
            assertEquals(1, roleImages.size());

            for (RoleAndArchitectureWithImage roleImage : roleImages)
            {
                assertEquals(DeploymentRole.test, roleImage.role);
                assertEquals(DockerImageArchitecture.X86, roleImage.architecture);
                assertEquals("imageTag",
                             sessionHolder.fromIdentity(roleImage.image)
                                          .getTag());
            }

            CustomerService model = ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_svc);
            assertNotNull(model);

            CustomerServiceRecord rec_new = new CustomerServiceRecord();
            ModelMapper.fromModel(sessionHolder, ModelMapperPolicy.Default, model, rec_new);
            assertNotNull(rec_new);
        }

        System.out.println("testCustomer1 - list Services for Image");
        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            RegistryTaggedImageRecord rec_taggedImage = holder.fromLocator(loc_tag);

            List<CustomerService> services = rec_taggedImage.findServices(holder);
            assertEquals(1, services.size());

            CustomerService svc = services.get(0);

            assertEquals("test1", svc.name);
        }

        System.out.println("testCustomer1 - create Backup");
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            CustomerServiceRecord rec_svc = holder.fromLocator(loc_svc);

            CustomerServiceBackupRecord rec_backup = CustomerServiceBackupRecord.newInstance(rec_svc, BackupKind.OnDemand, "fileId", 1024, null);
            rec_backup.setRoleImages(rec_svc.getRoleImages());

            holder.persistEntity(rec_backup);
            loc_backup = holder.createLocator(rec_backup);

            holder.commit();
        }

        System.out.println("testCustomer1 - list Images for Backup");
        try (SessionHolder sessionHolder = applicationRule.openSessionWithoutTransaction())
        {
            CustomerServiceBackupRecord rec_backup = sessionHolder.fromLocator(loc_backup);

            List<RoleAndArchitectureWithImage> roleImages = rec_backup.getRoleImages();
            assertEquals(1, roleImages.size());

            for (RoleAndArchitectureWithImage roleImage : roleImages)
            {
                assertEquals(DeploymentRole.test, roleImage.role);
                assertEquals(DockerImageArchitecture.X86, roleImage.architecture);
                assertEquals("imageTag",
                             sessionHolder.fromIdentity(roleImage.image)
                                          .getTag());
            }

            CustomerServiceBackup model = ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_backup);
            assertNotNull(model);

            CustomerServiceBackupRecord rec_new = new CustomerServiceBackupRecord();
            ModelMapper.fromModel(sessionHolder, ModelMapperPolicy.Default, model, rec_new);
            assertNotNull(rec_new);
        }

        System.out.println("testCustomer1 - list Backups for Image");
        try (SessionHolder sessionHolder = applicationRule.openSessionWithoutTransaction())
        {
            RegistryTaggedImageRecord rec_taggedImage = sessionHolder.fromLocator(loc_tag);

            List<CustomerServiceBackup> backups = rec_taggedImage.findBackups(sessionHolder);
            assertEquals(1, backups.size());

            CustomerServiceBackup backup = backups.get(0);

            assertEquals("fileId", backup.fileId);

            RegistryTaggedImage model = ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_taggedImage);
            assertNotNull(model);

            RegistryTaggedImageRecord rec_new = new RegistryTaggedImageRecord();
            ModelMapper.fromModel(sessionHolder, ModelMapperPolicy.Default, model, rec_new);
            assertNotNull(rec_new);
        }
    }

    @Test
    @TestOrder(200)
    public void testImage1()
    {
        RecordLocator<RegistryImageRecord>  loc_image1;
        RecordLocator<RegistryImageRecord>  loc_image2;
        RecordLocator<DeploymentHostRecord> loc_host;
        RecordLocator<DeploymentTaskRecord> loc_task;

        System.out.println("testImage1 - create Host/Task/Image");
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RegistryImageRecord rec_image1 = RegistryImageRecord.newInstance("abc", null, DockerImageArchitecture.X86);
            holder.persistEntity(rec_image1);
            loc_image1 = holder.createLocator(rec_image1);
            System.out.println(loc_image1);

            RegistryImageRecord rec_image2 = RegistryImageRecord.newInstance("def", null, DockerImageArchitecture.X86);
            holder.persistEntity(rec_image2);
            loc_image2 = holder.createLocator(rec_image2);
            System.out.println(loc_image2);

            DeploymentHostRecord rec_host = new DeploymentHostRecord();
            rec_host.setHostId("test");
            rec_host.setStatus(DeploymentStatus.Ready);
            rec_host.setArchitecture(DockerImageArchitecture.X86);
            holder.persistEntity(rec_host);
            loc_host = holder.createLocator(rec_host);

            holder.commitAndBeginNewTransaction();

            DeploymentTaskRecord rec_task = DeploymentTaskRecord.newInstance(rec_host);
            rec_task.setImage("image");
            rec_task.setImageReference(rec_image1);
            holder.persistEntity(rec_task);
            loc_task = holder.createLocator(rec_task);

            holder.commit();
        }

        System.out.println("testImage1 - update name");
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            DeploymentTaskRecord rec_task = holder.fromLocator(loc_task);
            rec_task.setName("Foo");

            holder.commit();
        }

        System.out.println("testImage1 - swap image");
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            DeploymentTaskRecord rec_task  = holder.fromLocator(loc_task);
            RegistryImageRecord  rec_image = holder.fromLocator(loc_image2);
            rec_task.setImageReference(rec_image);

            holder.commit();
        }
    }

    @Test
    @TestOrder(300)
    public void testAgent1()
    {
        RecordLocator<DeploymentHostRecord>  loc_host;
        RecordLocator<DeploymentAgentRecord> loc_agent1;
        RecordLocator<DeploymentAgentRecord> loc_agent2;

        System.out.println("testAgent1 - create Host/Agents");
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            DeploymentHostRecord rec_host = new DeploymentHostRecord();
            rec_host.setHostId("test");
            rec_host.setStatus(DeploymentStatus.Ready);
            rec_host.setArchitecture(DockerImageArchitecture.X86);
            holder.persistEntity(rec_host);
            loc_host = holder.createLocator(rec_host);

            holder.commitAndBeginNewTransaction();

            DeploymentAgentRecord rec_agent1 = DeploymentAgentRecord.newInstance(rec_host);
            rec_agent1.setInstanceId("v1");
            holder.persistEntity(rec_agent1);
            loc_agent1 = holder.createLocator(rec_agent1);

            DeploymentAgentRecord rec_agent2 = DeploymentAgentRecord.newInstance(rec_host);
            rec_agent2.setInstanceId("v2");
            holder.persistEntity(rec_agent2);
            loc_agent2 = holder.createLocator(rec_agent2);

            holder.commit();
        }

        System.out.println("testAgent1 - update name");
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            DeploymentAgentRecord rec_agent1 = holder.fromLocator(loc_agent1);
            rec_agent1.setInstanceId("Foo");

            holder.commit();
        }

        System.out.println("testImage1 - delete agent");
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            DeploymentHostRecord rec_host = holder.fromLocator(loc_host);
            assertEquals(2,
                         rec_host.getAgents()
                                 .size());

            DeploymentAgentRecord rec_agent2 = holder.fromLocator(loc_agent2);
            holder.deleteEntity(rec_agent2);

            holder.commit();

            assertEquals(1,
                         rec_host.getAgents()
                                 .size());
        }
    }

    @Test
    @TestOrder(400)
    public void testLocks()
    {
        RecordHelper<DeploymentHostRecord>  helper_host;
        RecordHelper<DeploymentAgentRecord> helper_agent;

        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            helper_host  = holder.createHelper(DeploymentHostRecord.class);
            helper_agent = holder.createHelper(DeploymentAgentRecord.class);

            try (var lock_host = helper_host.lockTable(10, TimeUnit.MILLISECONDS))
            {
                assertFailure(LockTimeoutException.class, () ->
                {
                    try (var lock_host2 = helper_host.lockTable(10, TimeUnit.MILLISECONDS))
                    {
                        fail();
                    }
                });
            }

            try (var lock_host = helper_host.lockTable(10, TimeUnit.MILLISECONDS))
            {
                try (var lock_agent = helper_agent.lockTable(10, TimeUnit.MILLISECONDS))
                {
                    System.out.println("Locked Host and Agent tables");
                }
            }

            try (var lock_host = helper_host.lockTableAndRecord("test", 10, TimeUnit.MILLISECONDS))
            {
                assertFailure(LockTimeoutException.class, () ->
                {
                    try (var lock_host2 = helper_host.lockTableAndRecord("test", 10, TimeUnit.MILLISECONDS))
                    {
                        fail();
                    }
                });
            }

            try (var lock_host = helper_host.lockTableAndRecord("test", 10, TimeUnit.MILLISECONDS))
            {
                try (var lock_host2 = helper_host.lockTableAndRecord("test2", 10, TimeUnit.MILLISECONDS))
                {
                    System.out.println("Locked two sections of Host table");
                }
            }
        }
    }

    //--//

    private HostRemoter getHostRemoter()
    {
        return applicationRule.getSupport()
                              .getConfiguration().hostRemoter;
    }
}
