/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.deployer.remoting.impl;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.io.File;
import java.io.IOException;
import java.lang.management.ThreadInfo;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.annotation.Optio3RemotableEndpoint;
import com.optio3.cloud.client.deployer.model.DeployerShutdownConfiguration;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.client.deployer.model.FileStatus;
import com.optio3.cloud.client.deployer.proxy.DeployerControlApi;
import com.optio3.cloud.deployer.DeployerApplication;
import com.optio3.cloud.deployer.DeployerConfiguration;
import com.optio3.cloud.deployer.logic.AdaptiveChunk;
import com.optio3.concurrency.Executors;
import com.optio3.infra.SshHelper;
import com.optio3.infra.docker.DockerHelper;
import com.optio3.infra.waypoint.BootConfig;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.interop.SystemTimeOfDay;
import com.optio3.logging.LoggerConfiguration;
import com.optio3.logging.LoggerFactory;
import com.optio3.util.FileSystem;
import com.optio3.util.StackTraceAnalyzer;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.FunctionWithException;

@Optio3RemotableEndpoint(itf = DeployerControlApi.class)
public class DeployerControlApiImpl extends CommonDeployerApiImpl implements DeployerControlApi
{
    @Override
    public CompletableFuture<ZonedDateTime> setSystemTime(ZonedDateTime time)
    {
        DockerImageArchitecture arch = FirmwareHelper.architecture();
        if (arch != null)
        {
            switch (arch)
            {
                case ARMv7:
                case ARMv8:
                    SystemTimeOfDay.setTimeOfDay(time);
                    break;
            }
        }

        return wrapAsync(TimeUtils.now());
    }

    @Override
    public CompletableFuture<List<String>> dumpThreads(boolean includeMemInfo)
    {
        Map<StackTraceAnalyzer, List<ThreadInfo>> uniqueStackTraces = StackTraceAnalyzer.allThreadInfos();
        List<String>                              lines             = StackTraceAnalyzer.printThreadInfos(includeMemInfo, uniqueStackTraces);

        return wrapAsync(lines);
    }

    @Override
    public CompletableFuture<String> setShutdownConfiguration(DeployerShutdownConfiguration cfg) throws
                                                                                                 Exception
    {
        FirmwareHelper helper = FirmwareHelper.get();

        FirmwareHelper.ShutdownConfiguration cfgLocal = new FirmwareHelper.ShutdownConfiguration();
        cfgLocal.turnOffVoltage      = cfg.turnOffVoltage;
        cfgLocal.turnOnVoltage       = cfg.turnOnVoltage;
        cfgLocal.turnOffDelaySeconds = cfg.turnOffDelaySeconds;
        cfgLocal.turnOnDelaySeconds  = cfg.turnOnDelaySeconds;

        String failure = helper.setShutdownConfiguration(cfgLocal);

        getApplication().flushHeartbeat(false);

        return wrapAsync(failure);
    }

    @Override
    public CompletableFuture<Void> flushHeartbeat()
    {
        getApplication().flushHeartbeat(true);

        return wrapAsync(null);
    }

    @Override
    public CompletableFuture<Void> restart()
    {
        Executors.scheduleOnDefaultPool(() -> Runtime.getRuntime()
                                                     .exit(0), 10, TimeUnit.SECONDS);

        return wrapAsync(null);
    }

    //--//

    @Override
    public CompletableFuture<List<LoggerConfiguration>> getLoggers()
    {
        return wrapAsync(LoggerFactory.getLoggersConfiguration());
    }

    @Override
    public CompletableFuture<LoggerConfiguration> configLogger(LoggerConfiguration cfg)
    {
        return wrapAsync(LoggerFactory.setLoggerConfiguration(cfg));
    }

    @Override
    public CompletableFuture<Map<String, String>> getBootParameters() throws
                                                                      Exception
    {
        DeployerConfiguration cfg = getConfiguration();

        try
        {
            BootConfig bc = BootConfig.parse(cfg.bootConfig);
            if (bc != null)
            {
                return wrapAsync(BootConfig.convertToPlain(bc.getAll()));
            }
        }
        catch (Throwable t)
        {
            DeployerApplication.LoggerInstance.error("Failed to parse boot file '%s': %s", cfg.bootConfig, t);
        }

        return wrapAsync(null);
    }

    @Override
    public CompletableFuture<String> setBootParameter(String key,
                                                      String value) throws
                                                                    Exception
    {
        DeployerConfiguration cfg = getConfiguration();

        try
        {
            BootConfig bc = BootConfig.parse(cfg.bootConfig);
            if (bc != null)
            {
                BootConfig.Options opt  = BootConfig.Options.parse(key);
                BootConfig.Line    line = bc.get(opt, key);

                if (value == null)
                {
                    bc.unset(opt, key);
                }
                else
                {
                    bc.set(opt, key, value);
                }

                bc.save(cfg.bootConfig);

                return wrapAsync(line != null ? line.value : null);
            }
        }
        catch (Throwable t)
        {
            DeployerApplication.LoggerInstance.error("Failed to set boot parameters: %s", t);
        }

        return wrapAsync(null);
    }

    //--//

    @Override
    public CompletableFuture<List<FileStatus>> listFiles(String path) throws
                                                                      Exception
    {
        Path          root = Paths.get(normalizeRoot());
        MonitoredFile file = normalizePath(path);

        if (!file.target.isDirectory())
        {
            return wrapAsync(null);
        }

        List<FileStatus> results = Lists.newArrayList();
        for (File sub : file.target.listFiles())
        {
            results.add(FileStatus.build(root, sub));
        }

        return wrapAsync(results);
    }

    @Override
    public CompletableFuture<Boolean> deleteFile(String path) throws
                                                              Exception
    {
        MonitoredFile file = normalizePath(path);

        return wrapAsync(file.delete());
    }

    @Override
    public CompletableFuture<Boolean> deleteDirectory(String path) throws
                                                                   Exception
    {
        MonitoredFile file = normalizePath(path);

        if (!file.target.isDirectory())
        {
            return wrapAsync(false);
        }

        Map<Path, IOException> errors = FileSystem.deleteDirectory(Paths.get(path));
        return wrapAsync(errors.isEmpty());
    }

    @Override
    public CompletableFuture<Long> getFileSize(String path) throws
                                                            Exception
    {
        MonitoredFile file = normalizePath(path);

        if (file.target.isFile())
        {
            return wrapAsync(file.target.length());
        }

        return wrapAsync(-1L);
    }

    @Override
    public CompletableFuture<Void> writeFile(String path,
                                             FunctionWithException<Integer, CompletableFuture<byte[]>> getChunk) throws
                                                                                                                 Exception
    {
        MonitoredFile file        = normalizePath(path);
        AdaptiveChunk chunkHelper = new AdaptiveChunk();
        long          offset      = 0;

        while (true)
        {
            int chunkSize = chunkHelper.size();

            chunkHelper.start();

            byte[] buf = await(getChunk.apply(chunkSize));
            if (buf == null || buf.length == 0)
            {
                break;
            }

            chunkHelper.stop();

            file.write(offset, buf, 0, buf.length);
            offset += buf.length;
        }

        file.closeHandles();

        return wrapAsync(null);
    }

    @Override
    public CompletableFuture<Integer> readFile(String path,
                                               FunctionWithException<byte[], CompletableFuture<Boolean>> putChunk) throws
                                                                                                                   Exception
    {
        MonitoredFile file        = normalizePath(path);
        AdaptiveChunk chunkHelper = new AdaptiveChunk();
        byte[]        buf         = null;
        int           tot         = 0;

        while (true)
        {
            int chunkSize = chunkHelper.size();

            chunkHelper.start();

            if (buf == null || buf.length != chunkSize)
            {
                buf = new byte[chunkSize];
            }

            int read = file.read(tot, buf);
            if (read <= 0)
            {
                break;
            }

            chunkHelper.stop();

            byte[]  bufOut      = read == buf.length ? buf : Arrays.copyOf(buf, read);
            boolean keepReading = await(putChunk.apply(bufOut));
            if (!keepReading)
            {
                break;
            }

            tot += read;
        }

        file.closeHandles();

        return wrapAsync(tot);
    }

    //--//

    @Override
    public CompletableFuture<Void> writeFileChunk(String path,
                                                  long offset,
                                                  byte[] chunk) throws
                                                                Exception
    {
        MonitoredFile file = normalizePath(path);

        file.write(offset, chunk, 0, chunk.length);

        return AsyncRuntime.asNull();
    }

    @Override
    public CompletableFuture<byte[]> readFileChunk(String path,
                                                   long offset,
                                                   int length) throws
                                                               Exception
    {
        MonitoredFile file = normalizePath(path);

        byte[] buf  = new byte[length];
        int    read = file.read(offset, buf);
        if (read <= 0)
        {
            return AsyncRuntime.asNull();
        }

        byte[] bufOut = read == buf.length ? buf : Arrays.copyOf(buf, read);

        return wrapAsync(bufOut);
    }

    @Override
    public CompletableFuture<Void> closeFile(String path) throws
                                                          Exception
    {
        MonitoredFile file = normalizePath(path);

        file.closeHandles();

        return AsyncRuntime.asNull();
    }

    //--//

    @Override
    public CompletableFuture<Integer> copyFileToHost(String user,
                                                     byte[] privateKey,
                                                     byte[] publicKey,
                                                     byte[] passphrase,
                                                     String path,
                                                     String hostPath) throws
                                                                      Exception
    {
        MonitoredFile file            = normalizePath(path);
        String        localPathOnHost = DockerHelper.mapBindingToHost(file.path);
        SshHelper     sshHelper       = new SshHelper(privateKey, publicKey, passphrase, "127.0.0.1", user);

        String cmd     = String.format("/usr/bin/sudo cp %s %s", localPathOnHost, hostPath);
        int    success = sshHelper.exec(cmd, 5, TimeUnit.MINUTES, null, null);

        return wrapAsync(success);
    }

    @Override
    public CompletableFuture<Integer> copyFileFromHost(String user,
                                                       byte[] privateKey,
                                                       byte[] publicKey,
                                                       byte[] passphrase,
                                                       String path,
                                                       String hostPath) throws
                                                                        Exception
    {
        MonitoredFile file            = normalizePath(path);
        String        localPathOnHost = DockerHelper.mapBindingToHost(file.path);
        SshHelper     sshHelper       = new SshHelper(privateKey, publicKey, passphrase, "127.0.0.1", user);

        int success = -1;

        try
        {
            String cmd = String.format("/usr/bin/sudo cp %s %s", hostPath, localPathOnHost);
            success = sshHelper.exec(cmd, 5, TimeUnit.MINUTES, null, null);
        }
        finally
        {
            if (success != 0)
            {
                File f = new File(path);
                if (f.exists())
                {
                    f.delete();
                }
            }
        }

        return wrapAsync(success);
    }
}
