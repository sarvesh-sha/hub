/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.cli;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.JCommander.Builder;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.optio3.concurrency.Executors;
import com.optio3.infra.LibUsbHelper;
import com.optio3.infra.directory.CredentialDirectory;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.CollectionUtils;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.usb4java.DeviceDescriptor;

public class Provisioner
{
    public abstract class BaseCommand
    {
        public abstract void exec() throws
                                    Exception;
    }

    @Parameters(commandNames = "pack", commandDescription = "pack firmware files into archive")
    public class CommandPack extends BaseCommand
    {
        @Parameter(names = "--root", description = "Root path for files", required = true)
        public String root;

        @Parameter(names = "--flashLayout", description = "Flash Layout file, relative to root", required = true)
        public String flashLayout; // Example: "flashlayout_optio3-image-core/trusted/FlashLayout_emmc_optio3-edge-v1-trusted.tsv";

        @Parameter(names = "--output", description = "Output archive", required = true)
        public String output;

        @Override
        public void exec() throws
                           Exception
        {
            List<LibUsbHelper.Dfu.FlashLayout> partitions = LibUsbHelper.Dfu.parseFlashLayout(root, flashLayout);

            LibUsbHelper.FirmwareArchive archive = LibUsbHelper.FirmwareArchive.convertFrom(partitions);

            if (verbose)
            {
                System.err.printf("%s\n", ObjectMappers.prettyPrintAsJson(archive));
            }

            archive.emit(output);
        }
    }

    @Parameters(commandNames = "flash", commandDescription = "flash firmware to eMMC")
    public class CommandFlash extends BaseCommand
    {
        @Parameter(names = "--firmware", description = "Firmware file")
        public String firmware;

        @Parameter(names = "--maxWait", description = "Max number of attempts to wait for USB")
        public int maxWait = 10;

        @Parameter(names = "--testOnly", description = "Just test, don't burn OTP flags")
        public boolean testOnly;

        @Override
        public void exec() throws
                           Exception
        {
            LibUsbHelper.FirmwareArchive archive = LibUsbHelper.FirmwareArchive.open(firmware);

            MonotonousTime timeout = TimeUtils.computeTimeoutExpiration(maxWait, TimeUnit.SECONDS);
            while (!TimeUtils.isTimeoutExpired(timeout))
            {
                System.out.println("Looking for USB...");

                try (LibUsbHelper usbHelper = new LibUsbHelper())
                {
                    try (LibUsbHelper.DeviceListHelper deviceList = usbHelper.getDeviceList())
                    {
                        for (LibUsbHelper.Dfu dfu : deviceList.getDfuDevices())
                        {
                            DeviceDescriptor descriptor   = dfu.device.getDeviceDescriptor();
                            String           product      = dfu.device.getString(descriptor.iProduct());
                            String           manufacturer = dfu.device.getString(descriptor.iManufacturer());
                            String           serialNumber = dfu.device.getString(descriptor.iSerialNumber());

                            System.out.printf("iProduct: %s / %s / %s\n", product, manufacturer, serialNumber);

                            while (!TimeUtils.isTimeoutExpired(timeout))
                            {
                                LibUsbHelper.Dfu.Status status = dfu.getStatus();
                                if (status == null)
                                {
                                    continue;
                                }

                                if (verbose)
                                {
                                    System.out.printf("Status %s\n", status.bState);
                                }

                                LibUsbHelper.Dfu.Phase phase = dfu.getPhase();
                                if (phase == null)
                                {
                                    continue;
                                }
                                System.out.printf("Phase %d\n", phase.phaseId);

                                if (phase.phaseId == 0xFE)// || phase.phaseId == 0x21)
                                {
                                    System.out.println("Done");

                                    status = dfu.getStatus();
                                    if (verbose)
                                    {
                                        System.out.printf("Status %s\n", status.bState);
                                    }

                                    if (testOnly)
                                    {
                                        System.out.println("Board ready, booting for testing!!");

                                        dfu.sendBOOT();
                                        return;
                                    }
                                    else
                                    {

                                        dfu.configureMMC();

                                        System.out.println("Board ready, resetting!!\n");

                                        status = dfu.getStatus();
                                        if (verbose)
                                        {
                                            System.out.printf("Status %s\n", status.bState);
                                        }
                                        dfu.sendRESET();
                                        return;
                                    }
                                }

                                if (phase.detachRequested)
                                {
                                    dfu.sendDETACH();
                                    System.out.printf("Detached...\n");

                                    timeout = TimeUtils.computeTimeoutExpiration(maxWait, TimeUnit.SECONDS);
                                    break;
                                }

                                if (phase.phaseId == 0)
                                {
                                    List<LibUsbHelper.Dfu.FlashLayout> partitions = CollectionUtils.transformToList(archive.partitions, (p) -> p.descriptor);

                                    byte[] buf = LibUsbHelper.Dfu.prepareFlashLayout(partitions);

                                    ByteBuffer buffer = ByteBuffer.allocateDirect(buf.length);
                                    buffer.put(buf);
                                    buffer.flip();

                                    dfu.sendToDevice(phase.phaseId, buffer, null);
                                }
                                else
                                {
                                    LibUsbHelper.FirmwareArchive.Part partition = CollectionUtils.findFirst(archive.partitions, (p) -> p.descriptor.phaseId == phase.phaseId);

                                    Stopwatch st = Stopwatch.createStarted();

                                    try (RandomAccessFile file = new RandomAccessFile(new File(firmware), "r"))
                                    {
                                        MappedByteBuffer buffer = file.getChannel()
                                                                      .map(FileChannel.MapMode.READ_ONLY, partition.offset, partition.length);

                                        final int     reportDelta = 1024 * 1024;
                                        AtomicInteger nextUpdate  = new AtomicInteger(reportDelta);

                                        dfu.sendToDevice(phase.phaseId, buffer, (chunks, offset, last) ->
                                        {
                                            if (last || offset > nextUpdate.get())
                                            {
                                                System.out.printf("%s: %,d / %d\n", partition.descriptor.binary.getName(), offset, st.elapsed(TimeUnit.SECONDS));

                                                nextUpdate.addAndGet(reportDelta);
                                            }
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

            throw new TimeoutException("No USB handshake!");
        }
    }

    // @formatter:off
    @Parameter(names = "--help", help = true)
    private boolean help;

    @Parameter(names = "--verbose", description = "Verbose output")
    private boolean verbose;
    // @formatter:on

    //--//

    private Map<String, BaseCommand> m_register = Maps.newHashMap();

    private CredentialDirectory m_credDir;

    //--//

    public static void main(String[] args) throws
                                           Exception
    {
        new Provisioner().doMain(args);
    }

    public void doMain(String[] args) throws
                                      Exception
    {
        JCommander.Builder builder = JCommander.newBuilder();

        builder.addObject(this);

        addCommand(builder, new CommandPack());
        addCommand(builder, new CommandFlash());

        JCommander parser = builder.build();

        try
        {
            parser.parse(args);
        }
        catch (ParameterException e)
        {
            System.err.println(e.getMessage());
            System.err.println();

            StringBuilder sb = new StringBuilder();
            parser.usage(sb);
            System.err.println(sb);
            System.err.println();
            return;
        }

        String      parsedCmdName = parser.getParsedCommand();
        BaseCommand parsedCmd     = m_register.get(parsedCmdName);
        if (parsedCmd != null)
        {
            parsedCmd.exec();
        }
        else
        {
            StringBuilder sb = new StringBuilder();
            parser.usage(sb);
            System.err.println(sb);
            System.err.println();
        }
    }

    private void addCommand(Builder builder,
                            BaseCommand cmd)
    {
        Parameters anno = cmd.getClass()
                             .getAnnotation(Parameters.class);

        builder.addCommand(cmd);
        for (String name : anno.commandNames())
            m_register.put(name, cmd);
    }
}
