/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

import com.google.common.collect.Maps;
import com.optio3.cloud.AbstractApplicationWithSwagger;
import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.client.builder.api.DeploymentHostProvisioningApi;
import com.optio3.cloud.client.builder.model.ProvisionFirmware;
import com.optio3.cloud.client.builder.model.ProvisionReport;
import com.optio3.cloud.provision.imaging.LabelerHelper;
import com.optio3.cloud.waypoint.model.FlashingProgress;
import com.optio3.cloud.waypoint.model.FlashingStatus;
import com.optio3.cloud.waypoint.model.ProvisionReportExt;
import com.optio3.concurrency.Executors;
import com.optio3.infra.waypoint.BootConfig;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.infra.waypoint.Led;
import com.optio3.util.CollectionUtils;
import com.optio3.util.FileSystem;
import com.optio3.util.TimeUtils;
import io.dropwizard.setup.Bootstrap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class WaypointApplication extends AbstractApplicationWithSwagger<WaypointConfiguration>
{
    private final Map<String, ProvisionReportExt> m_units = Maps.newHashMap();
    private       ScheduledFuture<?>              m_pendingFlash;
    private       FlashingProgress                m_flashingProgress;

    private final AtomicBoolean     m_checkingFirmware = new AtomicBoolean();
    private       ProvisionFirmware m_currentFirmware;
    private       ProvisionFirmware m_nextFirmware;

    public static void main(String[] args) throws
                                           Exception
    {
        new WaypointApplication().run(args);
    }

    public WaypointApplication()
    {
        enableVariableSubstition = true;
    }

    @Override
    public String getName()
    {
        return "Optio3 Waypoint";
    }

    @Override
    protected void initialize()
    {
        Bootstrap<?> bootstrap = getServiceNonNull(Bootstrap.class);
        bootstrap.addCommand(new LabelerCommand(this));
        bootstrap.addCommand(new ProgrammerCommand(this));
        bootstrap.addCommand(new ProvisionCommand(this));
        bootstrap.addCommand(new TesterCommand(this));
    }

    @Override
    protected boolean enablePeeringProtocol()
    {
        // Not routing RPC messages, no need for peering.
        return false;
    }

    @Override
    protected void run()
    {
        discoverResources("com.optio3.cloud.waypoint.api.");
        discoverExtraModels("com.optio3.cloud.waypoint.model.");

        serveAssets("/assets/website/dist", "/", "index.html", "WaypointUI", true, null);

        enableSwagger((config) ->
                      {
                          config.setVersion("1.0.0");
                          config.setTitle("Optio3 Waypoint APIs");
                          config.setDescription("APIs and Definitions for the Optio3 Waypoint product.");
                      }, (javaType, model) ->
                      {
                      }, "/api", "/api/v1", "com.optio3.cloud.waypoint.api");

        if (m_configuration.productionMode)
        {
            // No timed shutdown in production mode.
            queueFirmwareCheck(0, true);
        }
        else
        {
            queueShutdownTimer();
        }

        FirmwareHelper f = FirmwareHelper.get();
        f.rampLed(Led.Error, 1, Duration.ofMillis(4000), 0.5f);
    }

    @Override
    public void cleanupOnShutdown(long timeout,
                                  TimeUnit unit)
    {
        FirmwareHelper f = FirmwareHelper.get();
        f.turnLedOff(Led.Error);
    }

    //--//

    public void queueFirmwareCheck(int delayInMinutes,
                                   boolean reschedule)
    {
        Executors.scheduleOnDefaultPool(() ->
                                        {
                                            performFirmwareCheck();

                                            if (reschedule)
                                            {
                                                queueFirmwareCheck(15, true);
                                            }
                                        }, delayInMinutes, TimeUnit.MINUTES);
    }

    private void performFirmwareCheck()
    {
        if (!m_checkingFirmware.getAndSet(true))
        {
            try
            {
                com.optio3.cloud.client.builder.api.DeploymentHostProvisioningApi proxy = getProvisioningProxy();

                ProvisionFirmware latestFirmware = null;
                for (ProvisionFirmware firmware : proxy.listFirmwares("firmware_"))
                {
                    if (latestFirmware == null || TimeUtils.compare(latestFirmware.timestamp, firmware.timestamp) < 0)
                    {
                        latestFirmware = firmware;
                    }
                }

                if (latestFirmware != null)
                {
                    if (m_currentFirmware == null || TimeUtils.compare(m_currentFirmware.timestamp, latestFirmware.timestamp) < 0)
                    {
                        if (!isFlashingCard())
                        {
                            m_nextFirmware = latestFirmware;

                            LoggerInstance.info("Starting download of %,d bytes from %s", latestFirmware.size, latestFirmware.name);

                            try (FileSystem.TmpFileHolder tmp = FileSystem.createTempFile("firmware", "gz"))
                            {
                                final File file = tmp.get();

                                try (FileOutputStream output = new FileOutputStream(file))
                                {
                                    try (GZIPInputStream inputUncompressed = new GZIPInputStream(proxy.streamFirmware(latestFirmware.name)))
                                    {
                                        IOUtils.copy(inputUncompressed, output);
                                    }
                                }

                                final File fileTarget = new File(m_configuration.flashSource);
                                fileTarget.delete();
                                file.renameTo(fileTarget);
                            }

                            LoggerInstance.info("Completed download of %,d bytes from %s", latestFirmware.size, latestFirmware.name);

                            m_currentFirmware = latestFirmware;
                        }
                    }
                }
            }
            catch (Throwable t)
            {
                // Ignore failures...
            }

            m_nextFirmware = null;
            m_checkingFirmware.set(false);
        }
    }

    private void queueShutdownTimer()
    {
        Executors.scheduleOnDefaultPool(() ->
                                        {
                                            // If frontend has been disabled, exit.
                                            try
                                            {
                                                BootConfig bc = BootConfig.parse(m_configuration.bootConfig);
                                                if (bc != null)
                                                {
                                                    BootConfig.Line line = bc.get(BootConfig.Options.DisableFrontend, null);
                                                    if (line != null && line.value != null)
                                                    {
                                                        Runtime.getRuntime()
                                                               .exit(0);
                                                    }
                                                }
                                            }
                                            catch (Throwable t)
                                            {
                                                // Ignore failures...
                                            }

                                            queueShutdownTimer();
                                        }, 1, TimeUnit.MINUTES);
    }

    //--//

    public boolean recordCheckin(ProvisionReport report)
    {
        if (report == null || report.hostId == null)
        {
            return false;
        }

        LoggerInstance.info("Check-in by host %s", report.hostId);

        synchronized (m_units)
        {
            if (!m_units.containsKey(report.hostId))
            {
                ProvisionReportExt ext = new ProvisionReportExt();
                ext.info = report;

                m_units.put(report.hostId, ext);

                scheduleUploadToBuilder(ext);
            }
        }

        return true;
    }

    private void scheduleUploadToBuilder(ProvisionReportExt ext)
    {
        Executors.getDefaultThreadPool()
                 .execute(() -> uploadToBuilderInner(ext));
    }

    private void uploadToBuilderInner(ProvisionReportExt ext)
    {
        try
        {
            com.optio3.cloud.client.builder.api.DeploymentHostProvisioningApi proxy = getProvisioningProxy();

            if (proxy.checkin(ext.info))
            {
                LoggerInstance.info("Uploaded report for %s!", ext.info.hostId);
                ext.uploaded = true;
                ext.reportedError = false;
            }
            else
            {
                LoggerInstance.warn("Server rejected upload report for %s...", ext.info.hostId);
            }
        }
        catch (Throwable t)
        {
            if (!ext.reportedError)
            {
                ext.reportedError = true;
                LoggerInstance.error("Failed to upload report for %s, due to %s", ext.info.hostId, t);
            }

            Executors.safeSleep(30_000);
            scheduleUploadToBuilder(ext);
        }
    }

    public boolean printCheckin(String hostId)
    {
        WaypointConfiguration cfg = getServiceNonNull(WaypointConfiguration.class);

        synchronized (m_units)
        {
            for (ProvisionReportExt ext : m_units.values())
            {
                if (StringUtils.equals(ext.info.hostId, hostId))
                {
                    try
                    {
                        if (cfg.printerZD420t != null)
                        {
                            LabelerHelper helper = new LabelerHelper(ext.info);
                            helper.printZD420t(cfg.printerZD420t);
                            ext.printed = true;
                            return true;
                        }
                    }
                    catch (Throwable t)
                    {
                        return false;
                    }
                }
            }
        }

        return false;
    }

    public List<ProvisionReportExt> getNewCheckins(ZonedDateTime from)
    {
        synchronized (m_units)
        {
            return CollectionUtils.filter(m_units.values(), (report) -> TimeUtils.compare(from, report.info.timestamp) < 0);
        }
    }

    public long downloadingFirmware()
    {
        final ProvisionFirmware nextFirmware = m_nextFirmware;
        return nextFirmware != null ? nextFirmware.size : -1;
    }

    public long detectCard()
    {
        if (m_nextFirmware == null)
        {
            WaypointConfiguration cfg = getServiceNonNull(WaypointConfiguration.class);

            try (FileInputStream stream = new FileInputStream(cfg.flashInfo))
            {
                List<String> lines = IOUtils.readLines(stream, (Charset) null);
                if (lines.size() > 0)
                {
                    return Long.parseLong(lines.get(0)) * 512;
                }
            }
            catch (Throwable t)
            {
                // Ignore failures.
            }
        }

        return -1;
    }

    public boolean isFlashingCard()
    {
        synchronized (m_units)
        {
            if (m_pendingFlash != null)
            {
                if (!m_pendingFlash.isDone())
                {
                    return true;
                }

                m_pendingFlash = null;
            }

            return false;
        }
    }

    public FlashingProgress startCardFlashing()
    {
        FlashingProgress res = new FlashingProgress();

        synchronized (m_units)
        {
            if (isFlashingCard())
            {
                res.state = FlashingStatus.AlreadyFlashing;
            }
            else if (m_nextFirmware != null)
            {
                res.state = FlashingStatus.DownloadingFirmware;
            }
            else
            {
                WaypointConfiguration cfg = getServiceNonNull(WaypointConfiguration.class);

                File fileInput  = new File(cfg.flashSource);
                File fileOutput = new File(cfg.flashDevice);

                if (fileInput.isFile() && fileOutput.exists())
                {
                    res.state = FlashingStatus.Flashing;
                    res.imageSize = fileInput.length();
                    m_flashingProgress = res;

                    m_pendingFlash = Executors.scheduleOnDefaultPool(() ->
                                                                     {
                                                                         performCardFlashing(fileInput, fileOutput, res);
                                                                         System.gc();
                                                                     }, 0, TimeUnit.SECONDS);
                }
                else
                {
                    res.state = FlashingStatus.NoCard;
                }
            }
        }

        return res;
    }

    private void performCardFlashing(File fileInput,
                                     File fileOutput,
                                     FlashingProgress info)
    {
        try (FileChannel input = FileChannel.open(fileInput.toPath(), StandardOpenOption.READ))
        {
            try (FileChannel output = FileChannel.open(fileOutput.toPath(), StandardOpenOption.WRITE))
            {
                ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024 * 1024);

                while (true)
                {
                    buffer.clear();
                    int read = input.read(buffer);
                    if (read <= 0)
                    {
                        info.state = FlashingStatus.Failed;
                        break;
                    }

                    buffer.flip();
                    output.write(buffer);

                    info.imageOffset += read;

                    if (info.imageOffset >= info.imageSize)
                    {
                        info.state = FlashingStatus.Done;
                        break;
                    }
                }
            }
        }
        catch (Throwable t)
        {
            LoggerInstance.error("Failed copying from '%s' to '%s' at %d, due to %s", fileInput, fileOutput, m_flashingProgress, t);
            info.state = FlashingStatus.Failed;
        }
        finally
        {
            m_pendingFlash = null;
        }
    }

    public FlashingProgress checkCardFlashing()
    {
        return m_flashingProgress;
    }

    //--//

    private DeploymentHostProvisioningApi getProvisioningProxy()
    {
        String authVersion = "v1";

        String user = WellKnownRole.Machine.generateAuthPrincipal(authVersion, m_configuration.hostId);
        String pwd  = WellKnownRole.Machine.generateAuthCode(authVersion, m_configuration.hostId);

        return createProxyWithCredentials(m_configuration.connectionUrl + "/api/v1", DeploymentHostProvisioningApi.class, user, pwd);
    }
}
