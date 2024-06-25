/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.deployer;

import static com.optio3.util.Exceptions.getAndUnwrapException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.optio3.archive.TarBuilder;
import com.optio3.archive.TarWalker;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.client.deployer.model.FileStatus;
import com.optio3.cloud.client.deployer.proxy.DeployerControlApi;
import com.optio3.cloud.client.deployer.proxy.DeployerDockerApi;
import com.optio3.cloud.deployer.DeployerConfiguration;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.channel.RpcClient;
import com.optio3.logging.Severity;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import com.optio3.util.FileSystem;
import com.optio3.util.IdGenerator;
import org.apache.commons.io.FileUtils;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.ExternalResource;

public class DeployerFileCopyTest extends Optio3Test
{
    public static class MultiTestRule extends ExternalResource
    {
        public BuilderTestApplicationRule  builder;
        public DeployerTestApplicationRule deployer;

        public MultiTestRule()
        {
            builder = BuilderTestApplicationRule.newInstance((application) ->
                                                             {
                                                                 // Nothing to add.
                                                             });

            deployer = new DeployerTestApplicationRule((configuration) ->
                                                       {
                                                           configuration.connectionUrl = (builder.baseUri()
                                                                                                 .toString() + "api/v1/message-bus").replace("http:", "ws:");
                                                       }, (application) ->
                                                       {
                                                           // Nothing to add.
                                                       });
        }

        @Override
        protected void before() throws
                                Throwable
        {
            builder.invokeBefore();
            deployer.invokeBefore();
        }

        @Override
        protected void after()
        {
            deployer.invokeAfter();
            builder.invokeAfter();
        }
    }

    @ClassRule
    public static final MultiTestRule rule = new MultiTestRule();

    @Test
    @TestOrder(10)
    public void testFileIO() throws
                             Exception
    {
        MessageBusBroker.LoggerInstance.disable(Severity.Debug);
        MessageBusBroker.LoggerInstance.disable(Severity.DebugVerbose);

        BuilderApplication builder = rule.builder.getApplication();
        RpcClient          client  = getAndUnwrapException(builder.getRpcClient(10, TimeUnit.SECONDS));

        System.out.println("########### Waiting for Deployer...");
        String deployerId = rule.deployer.getEndpointId();

        boolean found = getAndUnwrapException(client.waitForDestination(deployerId, 2, TimeUnit.SECONDS));
        assertTrue(found);

        System.out.println("########### Found Deployer...");
        DeployerControlApi proxy = client.createProxy(deployerId, null, DeployerControlApi.class, 100, TimeUnit.SECONDS);

        DeployerConfiguration cfg = rule.deployer.getApplication()
                                                 .getServiceNonNull(DeployerConfiguration.class);

        for (int loop = 0; loop < 4; loop++)
        {
            final String testFileName = "deployed_file.txt";

            byte[] dataExpected = random(10240 + 100 * loop);

            try (ByteArrayInputStream stream = new ByteArrayInputStream(dataExpected))
            {
                Stopwatch sw = Stopwatch.createStarted();
                getAndUnwrapException(proxy.writeFile(testFileName, (len) ->
                {
                    byte[] buf  = new byte[len];
                    int    read = stream.read(buf);
                    //System.out.printf("Received request to read %d bytes, read %d%n", len, read);

                    return CompletableFuture.completedFuture(read < 0 ? null : Arrays.copyOf(buf, read));
                }));
                sw.stop();
                System.out.printf("Write time: %smsec%n", sw.elapsed(TimeUnit.MILLISECONDS));
            }

            byte[] dataGot = FileUtils.readFileToByteArray(cfg.getAgentFilesRoot()
                                                              .resolve(testFileName)
                                                              .toFile());

            assertArrayEquals(dataExpected, dataGot);

            List<FileStatus> files = getAndUnwrapException(proxy.listFiles("."));
            assertEquals(1, files.size());
            FileStatus fs = files.get(0);
            assertEquals(dataExpected.length, fs.length);

            //--//

            try (ByteArrayOutputStream stream = new ByteArrayOutputStream())
            {
                Stopwatch sw = Stopwatch.createStarted();
                int total = getAndUnwrapException(proxy.readFile(testFileName, (data) ->
                {
                    stream.write(data);

                    return AsyncRuntime.True;
                }));
                sw.stop();
                System.out.printf("Read time: %smsec%n", sw.elapsed(TimeUnit.MILLISECONDS));

                assertEquals(dataExpected.length, total);

                assertArrayEquals(dataExpected, stream.toByteArray());
            }

            //--//

            boolean deleted = getAndUnwrapException(proxy.deleteFile(testFileName));
            assertTrue(deleted);

            boolean deleted2 = getAndUnwrapException(proxy.deleteFile(testFileName));
            assertTrue(!deleted2);
        }
    }

    @Test
    @TestOrder(20)
    public void testFilePath() throws
                               Exception
    {
        BuilderApplication builder = rule.builder.getApplication();
        RpcClient          client  = getAndUnwrapException(builder.getRpcClient(10, TimeUnit.SECONDS));

        System.out.println("########### Waiting for Deployer...");
        String deployerId = rule.deployer.getEndpointId();

        boolean found = getAndUnwrapException(client.waitForDestination(deployerId, 2, TimeUnit.SECONDS));
        assertTrue(found);

        System.out.println("########### Found Deployer...");
        DeployerControlApi proxy = client.createProxy(deployerId, null, DeployerControlApi.class, 100, TimeUnit.SECONDS);

        assertFailure(RuntimeException.class, () ->
        {
            getAndUnwrapException(proxy.deleteFile("../unreachableFile.txt"));
        });
    }

    @Ignore("Manually enable to test, since this test creates and destroys Docker volumes, it could be a bit disruptive, especially if it fails.")
    @Test
    @TestOrder(30)
    public void testVolumeBackupAndRestore() throws
                                             Exception
    {
        BuilderApplication builder = rule.builder.getApplication();
        RpcClient          client  = getAndUnwrapException(builder.getRpcClient(10, TimeUnit.SECONDS));

        System.out.println("########### Waiting for Deployer...");
        String deployerId = rule.deployer.getEndpointId();

        boolean found = getAndUnwrapException(client.waitForDestination(deployerId, 2, TimeUnit.SECONDS));
        assertTrue(found);

        System.out.println("########### Found Deployer...");
        DeployerControlApi proxy  = client.createProxy(deployerId, null, DeployerControlApi.class, 100, TimeUnit.SECONDS);
        DeployerDockerApi  proxy2 = client.createProxy(deployerId, null, DeployerDockerApi.class, 100, TimeUnit.SECONDS);

        try (FileSystem.TmpFileHolder tarHolder = FileSystem.createTempFile())
        {
            try (FileOutputStream stream = new FileOutputStream(tarHolder.get()))
            {
                try (TarBuilder tarBuilder = new TarBuilder(stream, true))
                {
                    tarBuilder.addAsString(null, "fileA.txt", "contentA");
                    tarBuilder.addAsString("dir1", "fileB.txt", "contentBB");
                    tarBuilder.addAsString("dir2", "fileC.txt", "contentCCC");
                }
            }

            //--//

            String volumeName = IdGenerator.newGuid();

            getAndUnwrapException(proxy2.createVolume(volumeName, null, null, null));
            try
            {
                saveFile(proxy, tarHolder.get(), "volume.tgz");

                getAndUnwrapException(proxy2.restoreVolume(volumeName, null, "volume.tgz", 100, TimeUnit.SECONDS));

                getAndUnwrapException(proxy.deleteFile("volume.tgz"));

                String file = getAndUnwrapException(proxy2.backupVolume(volumeName, ".", 100, TimeUnit.SECONDS));

                List<FileStatus> files = getAndUnwrapException(proxy.listFiles("."));
                for (FileStatus fs : files)
                {
                    System.out.printf("%s: %d%n", fs.name, fs.length);
                }

                loadFile(proxy, file, tarHolder.get());

                getAndUnwrapException(proxy.deleteFile(file));

                Map<String, Long> filesInTar = Maps.newHashMap();

                TarWalker.walk(tarHolder.get(), true, (entry) ->
                {
                    if (!entry.isDirectory())
                    {
                        filesInTar.put(entry.name, entry.size);
                    }

                    return true;
                });

                assertEquals(3, filesInTar.size());
                assertTrue(filesInTar.containsKey("fileA.txt"));
                assertTrue(filesInTar.containsKey("dir1/fileB.txt"));
                assertTrue(filesInTar.containsKey("dir2/fileC.txt"));
                assertEquals("contentA".length(), (long) filesInTar.get("fileA.txt"));
                assertEquals("contentBB".length(), (long) filesInTar.get("dir1/fileB.txt"));
                assertEquals("contentCCC".length(), (long) filesInTar.get("dir2/fileC.txt"));
            }
            finally
            {
                proxy2.deleteVolume(volumeName, true);
            }
        }
    }

    private static void loadFile(DeployerControlApi proxy,
                                 String remoteSource,
                                 File localDestination) throws
                                                        Exception
    {
        try (FileOutputStream stream = new FileOutputStream(localDestination))
        {
            getAndUnwrapException(proxy.readFile(remoteSource, (buf) ->
            {
                stream.write(buf);

                return AsyncRuntime.True;
            }));
        }
    }

    private static void saveFile(DeployerControlApi proxy,
                                 File localSource,
                                 String remoteDestination) throws
                                                           Exception
    {
        try (FileInputStream stream = new FileInputStream(localSource))
        {
            getAndUnwrapException(proxy.writeFile(remoteDestination, (len) ->
            {
                byte[] buf  = new byte[len];
                int    read = stream.read(buf);

                return CompletableFuture.completedFuture(read < 0 ? null : Arrays.copyOf(buf, read));
            }));
        }
    }

    byte[] random(int len)
    {
        byte[] res = new byte[len];

        new Random().nextBytes(res);

        return res;
    }
}
