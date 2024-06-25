/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.logic.build;

import static com.optio3.util.Exceptions.getAndUnwrapException;
import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

import javax.ws.rs.NotFoundException;

import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.builder.persistence.worker.ManagedDirectoryRecord;
import com.optio3.cloud.builder.remoting.RemoteFileSystemApi;
import com.optio3.infra.directory.RoleType;
import com.optio3.infra.directory.UserInfo;
import com.optio3.util.Exceptions;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;

public class BuildLogicForMaven extends BaseBuildLogicWithJob
{
    private static final String MAVEN_LOCAL_SETTINGS = "settings.xml";
    private static final String MAVEN_POM            = "pom.xml";

    public BuildLogicForMaven(BuilderConfiguration config,
                              HostRecord targetHost,
                              JobRecord job)
    {
        super(config, targetHost, job);
    }

    //--//

    public void createSettingsForNexus(ManagedDirectoryRecord root,
                                       String relativePath,
                                       String repoUrl) throws
                                                       Exception
    {
        UserInfo user = getCredentialForRepo(repoUrl);

        Settings settings = new Settings();

        Server server = new Server();
        server.setId("nexus");
        server.setUsername(user.user);
        server.setPassword(user.getEffectivePassword());
        settings.addServer(server);

        Mirror mirror = new Mirror();
        mirror.setId("nexus");
        mirror.setMirrorOf("*");
        mirror.setUrl(repoUrl);
        settings.addMirror(mirror);

        Profile profile = new Profile();
        profile.setId("nexus");
        settings.addProfile(profile);

        settings.addActiveProfile("nexus");

        saveSettings(root, relativePath, settings);
    }

    public void saveSettings(ManagedDirectoryRecord root,
                             String relativePath,
                             Settings settings) throws
                                                Exception
    {
        ensureAcquired(root);

        byte[] contents = serializeSettings(settings);

        //--//

        RemoteFileSystemApi proxy = getProxy(RemoteFileSystemApi.class);

        Path dir = root.getPath();
        if (relativePath != null)
        {
            dir = dir.resolve(relativePath);
        }

        Path file = dir.resolve(MAVEN_LOCAL_SETTINGS);
        getAndUnwrapException(proxy.writeFile(file, contents));
    }

    public Settings loadSettings(ManagedDirectoryRecord root,
                                 String relativePath) throws
                                                      Exception
    {
        ensureAcquired(root);

        RemoteFileSystemApi proxy = getProxy(RemoteFileSystemApi.class);

        Path dir = root.getPath();
        if (relativePath != null)
        {
            dir = dir.resolve(relativePath);
        }

        Path   file     = dir.resolve(MAVEN_LOCAL_SETTINGS);
        byte[] contents = getAndUnwrapException(proxy.readFile(file, 0, -1));

        return deserializeSettings(contents);
    }

    public void savePom(ManagedDirectoryRecord root,
                        String relativePath,
                        Model pom) throws
                                   Exception
    {
        ensureAcquired(root);

        byte[] contents = serializePom(pom);

        //--//

        RemoteFileSystemApi proxy = getProxy(RemoteFileSystemApi.class);

        Path dir = root.getPath();
        if (relativePath != null)
        {
            dir = dir.resolve(relativePath);
        }

        Path file = dir.resolve(MAVEN_POM);
        getAndUnwrapException(proxy.writeFile(file, contents));
    }

    public Model loadPom(ManagedDirectoryRecord root,
                         String relativePath) throws
                                              Exception
    {
        ensureAcquired(root);

        RemoteFileSystemApi proxy = getProxy(RemoteFileSystemApi.class);

        Path dir = root.getPath();
        if (relativePath != null)
        {
            dir = dir.resolve(relativePath);
        }

        Path   file     = dir.resolve(MAVEN_POM);
        byte[] contents = getAndUnwrapException(proxy.readFile(file, 0, -1));

        return deserializePom(contents);
    }

    //--//

    public UserInfo getCredentialForRepo(String repoUrl)
    {
        requireNonNull(repoUrl);

        String host;

        try
        {
            host = new URL(repoUrl).getHost();
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }

        try
        {
            return getCredentialForHost(host, RoleType.Publisher);
        }
        catch (NotFoundException e)
        {
            throw Exceptions.newRuntimeException("No credentials for Nexus Repository %s", repoUrl);
        }
    }

    //--//

    private Model deserializePom(byte[] contents) throws
                                                  Exception
    {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(contents))
        {
            return new MavenXpp3Reader().read(stream);
        }
    }

    private byte[] serializePom(Model pom) throws
                                           IOException
    {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream())
        {
            new MavenXpp3Writer().write(stream, pom);

            return stream.toByteArray();
        }
    }

    private Settings deserializeSettings(byte[] contents) throws
                                                          Exception
    {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(contents))
        {
            return new SettingsXpp3Reader().read(stream);
        }
    }

    private byte[] serializeSettings(Settings settings) throws
                                                        IOException
    {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream())
        {
            new SettingsXpp3Writer().write(stream, settings);

            return stream.toByteArray();
        }
    }
}
