/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.infra.various;

import static org.junit.Assert.assertEquals;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.NotFoundException;

import com.optio3.archive.TarBuilder;
import com.optio3.asyncawait.CompileTime;
import com.optio3.concurrency.Executors;
import com.optio3.infra.WellKnownSites;
import com.optio3.infra.directory.RoleType;
import com.optio3.infra.directory.UserInfo;
import com.optio3.infra.docker.ContainerBuilder;
import com.optio3.infra.docker.DockerHelper;
import com.optio3.infra.docker.DockerImageDownloader;
import com.optio3.infra.docker.DockerImageIdentifier;
import com.optio3.infra.docker.model.ContainerInspection;
import com.optio3.infra.docker.model.Image;
import com.optio3.infra.docker.model.ImageSummary;
import com.optio3.infra.registry.ManifestV1Decoded;
import com.optio3.infra.registry.model.FsLayer;
import com.optio3.infra.registry.model.History;
import com.optio3.infra.registry.model.Layer;
import com.optio3.infra.registry.model.ManifestV1;
import com.optio3.infra.registry.model.ManifestV2;
import com.optio3.logging.Severity;
import com.optio3.serialization.ObjectMappers;
import com.optio3.test.infra.Optio3InfraTest;
import com.optio3.text.AnsiParser;
import com.optio3.util.FileSystem;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class DockerTest extends Optio3InfraTest
{
    @Before
    public void setup() throws
                        Exception
    {
        CompileTime.bootstrap();

        ensureCredentials(false, false);
    }

    @Ignore("Manually enable to test, since it requires access privileges to Docker.")
    @Test
    public void sniffDockerMessages() throws
                                      IOException,
                                      InterruptedException
    {
        DockerHelper.LoggerInstance.enable(Severity.Debug);
        DockerHelper.LoggerInstance.enable(Severity.DebugVerbose);

        try (DockerHelper helper = new DockerHelper(null, 4000))
        {
            System.out.println("Ready!");
            System.out.println();
            System.out.println("Launch docker with --host localhost:4000");

            Thread.sleep(10 * 60 * 1000);
        }
    }

    @Ignore("Manually enable to test, since it requires access privileges to Docker.")
    @Test
    public void listImages() throws
                             IOException
    {
        try (DockerHelper helper = new DockerHelper(null))
        {
            for (ImageSummary is : helper.listImages(false, null, false))
            {
                System.out.println(is.id);
            }
        }
    }

    @Ignore("Manually enable to test, since it requires access privileges to Docker.")
    @Test
    public void inspectImages() throws
                                IOException
    {
        try (DockerHelper helper = new DockerHelper(null))
        {
            for (ImageSummary is : helper.listImages(false, null, false))
            {
                System.out.println(is.id);
                Image image = helper.inspectImage(new DockerImageIdentifier(is.id));
                System.out.printf(">>>> %s%n", image.rootFS.layers);

                helper.exportImage(new DockerImageIdentifier(is.id));
            }
        }
    }

    @Ignore("Manually enable to test, since it requires access privileges to Docker.")
    @Test
    public void exportImage() throws
                              Exception
    {
        UserInfo user = credDir.findFirstAutomationUser(WellKnownSites.dockerRegistry(), RoleType.Subscriber);

        try (DockerImageDownloader dl = new DockerImageDownloader(Duration.of(1, ChronoUnit.DAYS)))
        {
            try (DockerImageDownloader.Reservation reservation = dl.acquire()
                                                                   .get())
            {
                reservation.analyzeAllImages("repo.dev.optio3.io:5000");

                DockerImageDownloader.LoggerInstance.enable(Severity.DebugVerbose);

                DockerImageIdentifier img = new DockerImageIdentifier("repo.dev.optio3.io:5000/optio3-deployer-armv7:bootstrap");

                DockerImageDownloader.PackageOfImages pkg1 = reservation.analyze(user, img);
                pkg1.processAllChunks();

                Set<String> diffIds = dl.enumerateDiffIDs();

                DockerImageDownloader.PackageOfImages pkg1_copy = reservation.createPackage(pkg1.manifestRaw, pkg1.repositoriesRaw);

                for (DockerImageDownloader.ImageDetails imageDetails : pkg1.images.values())
                {
                    System.out.printf("config: %s : %s%n", imageDetails.fileName, ObjectMappers.prettyPrintAsJson(imageDetails.contents));

                    for (String diff_id : imageDetails.diffIdToLayer.keySet())
                    {
                        System.out.printf("%s: %s%n", diff_id, diffIds.contains(diff_id) ? "Present" : "Missing");
                    }

                    DockerImageDownloader.ImageDetails imageDetails_copy = pkg1_copy.addImageDetails(imageDetails.fileName, imageDetails.raw);

                    for (String diff_id : imageDetails.diffIdToLayer.keySet())
                    {
                        if (!diffIds.contains(diff_id))
                        {
                            DockerImageDownloader.Layer layer = imageDetails.diffIdToLayer.get(diff_id);

                            pkg1_copy.linkLayer(layer);
                        }
                    }
                }

                try (FileSystem.TmpFileHolder tmp = FileSystem.createTempFile("docker", ".pkg.tar"))
                {
                    pkg1_copy.emit(tmp.get());
                }
            }
        }
    }

    @Ignore("Manually enable to test, since it requires access privileges to Docker.")
    @Test
    public void startContainer() throws
                                 IOException
    {
        try (DockerHelper helper = new DockerHelper(null))
        {
            ContainerBuilder builder = new ContainerBuilder();
            builder.setImage("busybox");
            builder.setCommandLine("ls -lR /");
            String id = helper.createContainer("test", builder);
            System.out.println(id);

            helper.startContainer(id, null);

            ZonedDateTime last  = null;
            int           round = 0;
            while (true)
            {
                List<DockerHelper.LogEntry> list = helper.getLogs(id, true, true, last, 0);
                System.out.printf("##### ROUND %d%n", round++);

                for (DockerHelper.LogEntry en : list)
                {
                    System.out.printf("%d: %s - %s", en.fd, en.timestamp, en.line);
                    last = en.timestamp;
                }

                ContainerInspection container = helper.inspectContainer(id);
                if (!DockerHelper.isRunning(container))
                {
                    break;
                }
            }

            helper.deleteContainer(id, true, true);
        }
    }

    @Ignore("Manually enable to test, since it requires access privileges to Docker.")
    @Test
    public void archiveContainer() throws
                                   Exception
    {
        try (DockerHelper helper = new DockerHelper(null))
        {
            ContainerBuilder builder = new ContainerBuilder();
            builder.setImage("debian:stretch");
            builder.setCommandLine("sleep 1000");
            String id = helper.createContainer("test", builder);
            System.out.println(id);

            helper.readArchiveFromContainer(id, "/etc/", (entry) ->
            {
                System.out.printf("%s : %s - %d %o%n", entry.name, entry.isDirectory(), entry.size, entry.permissions);
                return true;
            });

            helper.ensureDirectory(id, "/data///sub/child");

            try (FileSystem.TmpFileHolder tmpFile = FileSystem.createTempFile())
            {
                try (FileOutputStream stream = new FileOutputStream(tmpFile.get()))
                {
                    try (TarBuilder tarBuilder = new TarBuilder(stream, true))
                    {
                        tarBuilder.addAsString(null, "fileA.txt", "contentA", 0660);
                        tarBuilder.addAsString(null, "fileB.txt", "contentBB", 0600);
                        tarBuilder.addAsString(null, "fileC.txt", "contentCCC", 0400);
                    }
                }

                helper.writeArchiveToContainer(id, "/data/sub/child", tmpFile.get(), true);
            }

            helper.readArchiveFromContainer(id, "/data/sub", (entry) ->
            {
                System.out.printf("%s : %s - %d %o%n", entry.name, entry.isDirectory(), entry.size, entry.permissions);
                return true;
            });

            helper.readArchiveFromContainer(id, "/data/sub/child/fileB.txt", (entry) ->
            {
                System.out.printf("%s : %s - %d %o%n", entry.name, entry.isDirectory(), entry.size, entry.permissions);
                return true;
            });

            helper.deleteContainer(id, true, true);
        }
    }

    @Ignore("Manually enable to test, since it requires access privileges to Docker.")
    @Test
    public void buildCore() throws
                            IOException
    {
        //enableDockerHelperVerboseLog();

        Path repoDir = Paths.get(System.getenv("HOME"), "git/core");

        try (DockerHelper helper = new DockerHelper(null))
        {
            ContainerBuilder builder = new ContainerBuilder();
            builder.setImage("repo.dev.optio3.io:5000/optio3-maven:3.5.0-jre-8");
            // "maven:3.5.0-jdk-8"
            builder.setWorkingDir(Paths.get("/repo"));
            builder.setCommandLine("mvn compile -Dmaven.test.skip=true");
            // "mvn clean install"

            builder.addBind(repoDir, Paths.get("/repo"));
            builder.addBind(Paths.get(System.getenv("HOME"), ".m2"), Paths.get("/var/maven/.m2"));

            String id = helper.createContainer("test", builder);
            System.out.println(id);

            try
            {
                helper.startContainer(id, null);

                AnsiParser    ansiParser = new AnsiParser();
                StringBuilder sb         = new StringBuilder();

                ZonedDateTime last = null;
                while (true)
                {
                    Executors.safeSleep(1000);

                    List<DockerHelper.LogEntry> list = helper.getLogs(id, true, true, last, 0);

                    for (DockerHelper.LogEntry en : list)
                    {
                        List<Object> parsedLine = ansiParser.parse(en.line);
                        sb.setLength(0);
                        for (Object o : parsedLine)
                        {
                            if (o instanceof String)
                            {
                                sb.append((String) o);
                            }
                        }

                        System.out.printf("%d %s - %s", en.fd, en.timestamp, sb.toString());
                        last = en.timestamp;
                    }

                    ContainerInspection container = helper.inspectContainer(id);
                    if (!DockerHelper.isRunning(container))
                    {
                        break;
                    }
                }
            }
            finally
            {
                helper.deleteContainer(id, true, true);
            }
        }
    }

    @Ignore("Manually enable to test, since it requires access privileges to Docker.")
    @Test
    public void buildBadImage() throws
                                Exception
    {
        //enableDockerHelperVerboseLog();

        try (DockerHelper helper = new DockerHelper(null))
        {
            Path target = Paths.get(System.getenv("HOME"), "git/infra/docker-images/badImageForTesting");

            assertFailure(NotFoundException.class, () ->
            {
                @SuppressWarnings("unused") String imageTag = helper.buildImage(target, null, null, null, null, null, (bi) ->
                {
                    if (bi.stream != null)
                    {
                        System.out.print(bi.stream);
                    }

                    if (bi.status != null)
                    {
                        System.out.print("STATUS: " + bi.status + " " + bi.progress);
                    }

                    if (bi.errorDetail != null)
                    {
                        System.out.print("ERR: " + bi.errorDetail.message);
                    }
                });
            });
        }
    }

    @Ignore("Manually enable to test, since it requires access privileges to Docker and Nexus.")
    @Test
    public void pushImage() throws
                            Exception
    {
        //enableDockerHelperVerboseLog();

        UserInfo user = credDir.findFirstAutomationUser(WellKnownSites.dockerRegistry(), RoleType.Publisher);

        try (DockerHelper helper = new DockerHelper(user))
        {
            DockerImageIdentifier src = new DockerImageIdentifier("optio3-maven:3.5.0-jre-8");
            DockerImageIdentifier dst = new DockerImageIdentifier(WellKnownSites.makeDockerImageTagForPush("optio3-maven:test"));

            helper.tagImage(src, dst);

            String error = helper.pushImage(dst, (bi) ->
            {
                if (bi.stream != null)
                {
                    System.out.print(bi.stream);
                }

                if (bi.status != null)
                {
                    System.out.print("STATUS: " + bi.status + " " + bi.progress + "\n");
                }

                if (bi.aux != null)
                {
                    System.out.print("AUX: " + bi.aux + "\n");
                }

                if (bi.errorDetail != null)
                {
                    System.out.print("ERR: " + bi.errorDetail.message + "\n");
                }
            });
            assertEquals(null, error);

            helper.removeImage(dst, false);
        }
    }

    @Ignore("Manually enable to test, since it requires access privileges to Docker and Nexus.")
    @Test
    public void listRegistry() throws
                               Exception
    {
        //enableDockerHelperVerboseLog();

        UserInfo user = credDir.findFirstAutomationUser(WellKnownSites.dockerRegistry(), RoleType.Publisher);

        try (DockerHelper helper = new DockerHelper(user))
        {
            List<DockerImageIdentifier> repos = helper.listRegistryCatalog(WellKnownSites.dockerRegistryAddress(false));

            for (DockerImageIdentifier repo : repos)
            {
                System.out.printf("Repo: %s%n", repo);
                List<DockerImageIdentifier> tags = helper.listRegistryImageTags(repo);
                for (DockerImageIdentifier tag : tags)
                {
                    System.out.printf("    Tag: %s%n", tag);

                    ManifestV1 manifestV1 = helper.getManifestV1(tag);
                    for (FsLayer l : manifestV1.fsLayers)
                    {
                        System.out.printf("        FsLayer: %s%n", l.blobSum);
                    }

                    for (History h : manifestV1.history)
                    {
                        ManifestV1Decoded hDecoded = DockerHelper.decodeManifestHistory(h);
                        if (hDecoded.config != null && hDecoded.config.labels != null)
                        {
                            System.out.printf("        History: %s%n", hDecoded.id);
                            for (Entry<String, String> p : hDecoded.config.labels.entrySet())
                            {
                                System.out.printf("        Label: %s = %s%n", p.getKey(), p.getValue());
                            }
                        }
                    }

                    ManifestV2 manifestV2 = helper.getManifestV2(tag);
                    System.out.printf("    ManifestV2: %d / %s%n", manifestV2.config.size, manifestV2.config.digest);
                    for (Layer l : manifestV2.layers)
                    {
                        System.out.printf("        Layer: %d / %s / %s%n", l.size, l.digest, l.urls);
                    }

                    String digest = helper.getManifestDigest(tag);
                    System.out.printf("    Digest: %s%n", digest);

                    if (tag.name.equals("builder-test"))// && tag.tag.equals("latest"))
                    {
                        helper.deleteManifest(tag.getRegistryAddress(), tag);
                    }
                }
            }
        }
    }

    @Ignore("Manually enable to test, since it requires access privileges to Docker and Nexus.")
    @Test
    public void listRegistryForPull() throws
                                      Exception
    {
        //enableDockerHelperVerboseLog();

        UserInfo user = credDir.findFirstAutomationUser(WellKnownSites.dockerRegistry(), RoleType.Publisher);

        try (DockerHelper helper = new DockerHelper(user))
        {
            List<DockerImageIdentifier> repos = helper.listRegistryCatalog(WellKnownSites.dockerRegistryAddress(true));

            for (DockerImageIdentifier repo : repos)
            {
                List<DockerImageIdentifier> tags = helper.listRegistryImageTags(repo);
                for (DockerImageIdentifier tag : tags)
                {
                    System.out.printf("docker pull %s%n", tag);
                }
            }
        }
    }

    @Ignore("Manually enable to test, since it requires access privileges to Docker and Nexus.")
    @Test
    public void analyzeRegistry() throws
                                  Exception
    {
        //enableDockerHelperVerboseLog();

        UserInfo user = credDir.findFirstAutomationUser(WellKnownSites.dockerRegistry(), RoleType.Publisher);

        try (DockerHelper helper = new DockerHelper(user))
        {
            List<DockerHelper.RegistryImage> results = helper.analyzeRegistry(WellKnownSites.dockerRegistryAddress(false), false);

            for (DockerHelper.RegistryImage result : results)
            {
                System.out.printf("    Tag: %s%n", result.tag);
                System.out.printf("    Sha: %s - %s%n", result.imageSha, result.architecture);

                for (Entry<String, String> p : result.labels.entrySet())
                {
                    System.out.printf("        Label: %s = %s%n", p.getKey(), p.getValue());
                }
            }
        }
    }

    //--//

    public void enableDockerHelperVerboseLog()
    {
        DockerHelper.LoggerInstance.enable(Severity.Debug);
        DockerHelper.LoggerInstance.enable(Severity.DebugVerbose);
    }
}
