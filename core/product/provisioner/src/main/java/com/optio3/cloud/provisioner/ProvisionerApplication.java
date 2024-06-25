/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.provisioner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
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
import com.optio3.cloud.provisioner.model.FlashingProgress;
import com.optio3.cloud.provisioner.model.FlashingStatus;
import com.optio3.cloud.provisioner.model.ProvisionReportExt;
import com.optio3.concurrency.Executors;
import com.optio3.infra.GpioHelper;
import com.optio3.infra.LibUsbHelper;
import com.optio3.interop.mediaaccess.I2cAccess;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.CollectionUtils;
import com.optio3.util.FileSystem;
import com.optio3.util.IdGenerator;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class ProvisionerApplication extends AbstractApplicationWithSwagger<ProvisionerConfiguration>
{
    private final Map<String, ProvisionReportExt> m_units = Maps.newHashMap();
    private       boolean                         m_fetchedRecentUnits;
    private       ScheduledFuture<?>              m_pendingFlash;
    private       FlashingProgress                m_flashingProgress;

    private final AtomicBoolean     m_checkingFirmware = new AtomicBoolean();
    private       ProvisionFirmware m_currentFirmware;
    private       ProvisionFirmware m_nextFirmware;

    private Boolean    m_relayDetected;
    private GpioHelper m_relay;
    private String     m_uniqueSeed = IdGenerator.newGuid();

    public static void main(String[] args) throws
                                           Exception
    {
        new ProvisionerApplication().run(args);
    }

    public ProvisionerApplication()
    {
        enableVariableSubstition = true;
    }

    @Override
    public String getName()
    {
        return "Optio3 Provisioner";
    }

    @Override
    protected void initialize()
    {
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
        discoverResources("com.optio3.cloud.provisioner.api.");
        discoverExtraModels("com.optio3.cloud.provisioner.model.");

        serveAssets("/assets/website/dist", "/", "index.html", "ProvisionerUI", true, null);

        enableSwagger((config) ->
                      {
                          config.setVersion("1.0.0");
                          config.setTitle("Optio3 Provisioner APIs");
                          config.setDescription("APIs and Definitions for the Optio3 Provisioner product.");
                      }, (javaType, model) ->
                      {
                      }, "/api", "/api/v1", "com.optio3.cloud.provisioner.api");

        queueFirmwareCheck(0, true);

        isPowerSupported();
    }

    @Override
    public void cleanupOnShutdown(long timeout,
                                  TimeUnit unit)
    {
        if (m_relay != null)
        {
            m_relay.deinit(26);
        }
    }

    @Override
    public String getAppVersion()
    {
        return m_uniqueSeed; // Every run gets a new version, to force a UI refresh.
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
        if (m_configuration.getLocalArchiveLocation() != null)
        {
            return;
        }

        if (!m_checkingFirmware.getAndSet(true))
        {
            try
            {
                com.optio3.cloud.client.builder.api.DeploymentHostProvisioningApi proxy = getProvisioningProxy();

                ProvisionFirmware latestFirmware = null;
                for (ProvisionFirmware firmware : proxy.listFirmwares(m_configuration.factoryFloorMode ? "firmwareEdgeV1Test_" : "firmwareEdgeV1Prod_"))
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

                            Executors.safeSleep(5000);

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
                LoggerInstance.error("%s", t);
            }

            m_nextFirmware = null;
            m_checkingFirmware.set(false);
        }
    }

    //--//

    public boolean recordCheckin(ProvisionReport report)
    {
        if (report == null || report.hostId == null)
        {
            return false;
        }

        LoggerInstance.info("Check-in by host %s", report.hostId);
        LoggerInstance.info("%s", ObjectMappers.prettyPrintAsJson(report));

        synchronized (m_units)
        {
            ProvisionReportExt ext = m_units.get(report.hostId);
            if (ext == null)
            {
                ext = new ProvisionReportExt();

                m_units.put(report.hostId, ext);
            }

            ext.info     = report;
            ext.uploaded = false;
            scheduleUploadToBuilder(ext);
        }

        return true;
    }

    private void scheduleUploadToBuilder(ProvisionReportExt ext)
    {
        if (!ext.uploaded)
        {
            Executors.getDefaultThreadPool()
                     .execute(() -> uploadToBuilderInner(ext));
        }
    }

    private void uploadToBuilderInner(ProvisionReportExt ext)
    {
        try
        {
            com.optio3.cloud.client.builder.api.DeploymentHostProvisioningApi proxy = getProvisioningProxy();

            if (proxy.checkin(ext.info))
            {
                LoggerInstance.info("Uploaded report for %s!", ext.info.hostId);
                ext.uploaded      = true;
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
        synchronized (m_units)
        {
            for (ProvisionReportExt ext : m_units.values())
            {
                if (StringUtils.equals(ext.info.hostId, hostId))
                {
                    try
                    {
                        if (m_configuration.printerZD420t != null)
                        {
                            LabelerHelper helper = new LabelerHelper(ext.info);
                            helper.printZD420t(m_configuration.printerZD420t);
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
            if (!m_fetchedRecentUnits)
            {
                try
                {
                    ProvisionerConfiguration cfg = getServiceNonNull(ProvisionerConfiguration.class);

                    com.optio3.cloud.client.builder.api.DeploymentHostProvisioningApi proxy = getProvisioningProxy();

                    for (ProvisionReport report : proxy.recentCheckins(cfg.hostId))
                    {
                        if (!m_units.containsKey(report.hostId))
                        {
                            var ext = new ProvisionReportExt();

                            ext.info     = report;
                            ext.uploaded = true;

                            m_units.put(report.hostId, ext);
                        }
                    }

                    m_fetchedRecentUnits = true;
                }
                catch (Throwable t)
                {
                    // Retry later...
                }
            }

            return CollectionUtils.filter(m_units.values(), (report) -> TimeUtils.compare(from, report.info.timestamp) < 0);
        }
    }

    public long downloadingFirmware()
    {
        final ProvisionFirmware nextFirmware = m_nextFirmware;
        if (nextFirmware != null)
        {
            return nextFirmware.size;
        }

        return m_currentFirmware != null ? -1 : 0;
    }

    public boolean isPowerSupported()
    {
        if (m_relayDetected == null)
        {
            boolean detected = true;

//            detected &= isI2cPresent(1, 0x60); // ATECC608A, Disabled, a bit faulty.
            detected &= isI2cPresent(1, 0x57); // MCP79410 EEPROM
            detected &= isI2cPresent(1, 0x6F); // MCP79410 RTC

            m_relayDetected = detected;
            if (detected)
            {
                // Found Strato PI!
                m_relay = GpioHelper.get();

                m_relay.setDirection(26, true);
                powerBoard(false);
            }
        }

        return m_relayDetected;
    }

    private boolean isI2cPresent(int port,
                                 int address)
    {
        try (I2cAccess i2cHandler = new I2cAccess(port))
        {
            byte c = i2cHandler.readCommandByte(address, (byte) 0);
            LoggerInstance.info("Found I2C %d/%x", port, address);
            return true;
        }
        catch (Throwable t)
        {
            LoggerInstance.error("Not found I2C %d/%x, due to %s", port, address, t.getMessage());
            return false;
        }
    }

    public String powerBoard(boolean on)
    {
        if (m_relay == null)
        {
            return "No Relay";
        }

        LoggerInstance.info("Power: %s", on ? "on" : "off");

        return m_relay.setOutput(26, on) ? "Success" : "Failed";
    }

    public String detectBoard()
    {
        if (m_nextFirmware == null)
        {
            synchronized (m_units)
            {
                if (!isFlashingCard())
                {
                    try (LibUsbHelper usbHelper = new LibUsbHelper())
                    {
                        try (LibUsbHelper.DeviceListHelper deviceList = usbHelper.getDeviceList())
                        {
                            for (LibUsbHelper.Dfu dfu : deviceList.getDfuDevices())
                            {
                                LoggerInstance.debug("Found %s!", dfu.serialNumber);
                                return dfu.serialNumber;
                            }
                        }
                    }
                    catch (Throwable t)
                    {
                        // Ignore failures.
                    }
                }
            }
        }

        return null;
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

    public FlashingProgress startBoardFlashing(String serialNumber)
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
                String board = detectBoard();
                if (!StringUtils.equals(board, serialNumber))
                {
                    res.state = FlashingStatus.NoBoard;
                }
                else
                {
                    String archiveLocation = m_configuration.getArchiveLocation();

                    LibUsbHelper.FirmwareArchive archive = parseFirmware(archiveLocation);
                    if (archive == null)
                    {
                        res.state = FlashingStatus.NoBoard;
                    }
                    else
                    {
                        res.state          = FlashingStatus.Flashing;
                        res.imageSize      = archive.getTotalSize();
                        m_flashingProgress = res;

                        m_pendingFlash = Executors.scheduleOnDefaultPool(() ->
                                                                         {
                                                                             performBoardFlashing(serialNumber, archiveLocation, archive, res, 30);
                                                                             System.gc();
                                                                         }, 0, TimeUnit.SECONDS);
                    }
                }
            }
        }

        return res;
    }

    private LibUsbHelper.FirmwareArchive parseFirmware(String file)
    {
        try
        {
            return LibUsbHelper.FirmwareArchive.open(file);
        }
        catch (Throwable t)
        {
            return null;
        }
    }

    private void performBoardFlashing(String expectedSerialNumber,
                                      String archiveLocation,
                                      LibUsbHelper.FirmwareArchive archive,
                                      FlashingProgress info,
                                      int maxWait)
    {
        try
        {
            MonotonousTime timeout = TimeUtils.computeTimeoutExpiration(maxWait, TimeUnit.SECONDS);
            while (!TimeUtils.isTimeoutExpired(timeout))
            {
                try (LibUsbHelper usbHelper = new LibUsbHelper())
                {
                    try (LibUsbHelper.DeviceListHelper deviceList = usbHelper.getDeviceList())
                    {
                        for (LibUsbHelper.Dfu dfu : deviceList.getDfuDevices())
                        {
                            if (!StringUtils.equals(expectedSerialNumber, dfu.serialNumber))
                            {
                                info.state = FlashingStatus.NoBoard;
                                return;
                            }

                            while (!TimeUtils.isTimeoutExpired(timeout))
                            {
                                LibUsbHelper.Dfu.Status status = dfu.getStatus();
                                if (status == null)
                                {
                                    continue;
                                }

                                LoggerInstance.debug("Status %s", status.bState);

                                LibUsbHelper.Dfu.Phase phase = dfu.getPhase();
                                if (phase == null)
                                {
                                    continue;
                                }
                                LoggerInstance.debug("Phase %d", phase.phaseId);

                                if (phase.phaseId == 0xFE)// || phase.phaseId == 0x21)
                                {
                                    LoggerInstance.debug("Done");

                                    status = dfu.getStatus();
                                    LoggerInstance.debug("Status %s", status.bState);

                                    if (m_configuration.factoryFloorMode)
                                    {
                                        LoggerInstance.info("Board ready, booting for testing!!");

                                        dfu.sendBOOT();
                                    }
                                    else
                                    {
                                        dfu.configureMMC();
                                        status = dfu.getStatus();
                                        LoggerInstance.debug("Status %s", status.bState);

                                        LoggerInstance.info("Board ready, booting for registration!!");
                                        dfu.sendBOOT();
                                    }

                                    info.state = FlashingStatus.Done;
                                    return;
                                }

                                if (phase.detachRequested)
                                {
                                    dfu.sendDETACH();
                                    LoggerInstance.debug("Detached...");

                                    timeout = TimeUtils.computeTimeoutExpiration(maxWait, TimeUnit.SECONDS);
                                    break;
                                }

                                info.phase = phase.phaseId;

                                if (phase.phaseId == 0)
                                {
                                    List<LibUsbHelper.Dfu.FlashLayout> partitions = CollectionUtils.transformToList(archive.partitions, (p) -> p.descriptor);

                                    byte[] buf = LibUsbHelper.Dfu.prepareFlashLayout(partitions);

                                    ByteBuffer buffer = ByteBuffer.allocateDirect(buf.length);
                                    buffer.put(buf);
                                    buffer.flip();

                                    dfu.sendToDevice(phase.phaseId, buffer, null);

                                    info.phaseName = "Flash Layout";
                                }
                                else
                                {
                                    LibUsbHelper.FirmwareArchive.Part partition = CollectionUtils.findFirst(archive.partitions, (p) -> p.descriptor.phaseId == phase.phaseId);

                                    info.phaseName = partition.descriptor.name;

                                    var baseOffset = info.imageOffset;

                                    try (RandomAccessFile file = new RandomAccessFile(archiveLocation, "r"))
                                    {
                                        MappedByteBuffer buffer = file.getChannel()
                                                                      .map(FileChannel.MapMode.READ_ONLY, partition.offset, partition.length);

                                        dfu.sendToDevice(phase.phaseId, buffer, (chunks, offset, last) ->
                                        {
                                            info.imageOffset = baseOffset + offset;
                                        });

                                        timeout = TimeUtils.computeTimeoutExpiration(maxWait, TimeUnit.SECONDS);
                                    }
                                }

                                dfu.getPhase();
                            }
                        }
                    }
                }

                Executors.safeSleep(1000);
            }

            info.state = FlashingStatus.NoBoard;
        }
        catch (Throwable t)
        {
            LoggerInstance.error("Failed flashing firmware, due to %s", t);
            info.state         = FlashingStatus.Failed;
            info.failureReason = t.getMessage();
        }
    }

    public FlashingProgress checkBoardFlashing()
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
