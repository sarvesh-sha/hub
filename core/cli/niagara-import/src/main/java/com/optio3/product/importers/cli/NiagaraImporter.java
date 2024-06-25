/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import ch.qos.logback.classic.Logger;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.JCommander.Builder;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.optio3.archive.ZipWalker;
import com.optio3.metadata.normalization.BACnetImportExportData;
import com.optio3.metadata.normalization.ImportExportData;
import com.optio3.product.importers.niagara.EncodedString;
import com.optio3.product.importers.niagara.ParsedStation;
import com.optio3.product.importers.niagara.ParsedType;
import com.optio3.product.importers.niagara.bacnet.BBacnetDevice;
import com.optio3.product.importers.niagara.bacnet.BBacnetProxyExt;
import com.optio3.product.importers.niagara.baja.control.BControlPoint;
import com.optio3.product.importers.niagara.baja.driver.BPointFolder;
import com.optio3.product.importers.niagara.baja.sys.BFolder;
import com.optio3.product.importers.niagara.baja.sys.BStation;
import com.optio3.product.importers.niagara.niagaraDriver.BNiagaraProxyExt;
import com.optio3.product.importers.niagara.niagaraDriver.BNiagaraStation;
import com.optio3.util.CollectionUtils;
import com.optio3.util.FileSystem;
import io.dropwizard.logging.LoggingUtil;

public class NiagaraImporter
{

    //--//

    public static abstract class BaseCommand
    {
        public abstract void exec() throws
                                    Exception;

        protected Multimap<String, ParsedStation> scan(String path) throws
                                                                    IOException
        {
            Multimap<String, ParsedStation> tables = HashMultimap.create();

            File root = new File(path);
            if (root.isDirectory())
            {
                final Function<Path, Boolean> fileFilterLambda = (fileFilter) ->
                {
                    String name = fileFilter.toFile()
                                            .getName()
                                            .toLowerCase();

                    if (name.endsWith(".dist"))
                    {
                        return true;
                    }

                    return false;
                };

                FileSystem.walkDirectory(root.toPath(), (dir) -> true, fileFilterLambda, (file) -> visitOuterArchive(tables, file));
            }
            else
            {
                System.err.printf("Invalid path '%s'%n", path);
                Runtime.getRuntime()
                       .exit(10);
            }

            return tables;
        }

        protected void visitOuterArchive(Multimap<String, ParsedStation> tables,
                                         Path file)
        {
            final String suffix = "/config.bog";

            try
            {
                System.err.printf("Analyzing archive %s...\n", file);

                Stopwatch sw = Stopwatch.createStarted();
                ZipWalker.walk(file.toFile(), (subEntry) ->
                {
                    if (!subEntry.isDirectory())
                    {
                        String name = subEntry.getName();
                        if (name.endsWith(suffix))
                        {
                            name = name.substring(0, name.length() - suffix.length());

                            int slashPos = name.lastIndexOf('/');
                            if (slashPos > 0)
                            {
                                name = name.substring(slashPos + 1);
                            }

                            visitInnerArchive(tables, file, name, subEntry);
                        }
                    }

                    return true;
                });
                sw.stop();
                System.err.printf(" %smsec%n", sw.elapsed(TimeUnit.MILLISECONDS));
            }
            catch (Exception e)
            {
                // Ignore failures.
                System.err.printf("visitOuterArchive: failed analysis of %s:\n", file);
                e.printStackTrace();
            }
        }

        private void visitInnerArchive(Multimap<String, ParsedStation> tables,
                                       Path file,
                                       String stationName,
                                       ZipWalker.ArchiveEntry entry)
        {
            try
            {
                ZipWalker.walk(entry.getStream(), (subEntry) ->
                {
                    if (!subEntry.isDirectory())
                    {
                        String name = subEntry.getName();
                        if (name.equals("file.xml"))
                        {
                            try
                            {
                                ParsedStation station = new ParsedStation(stationName, file, subEntry.getLastModifiedTime(), subEntry.getStream());
                                tables.put(stationName, station);
                            }
                            catch (Exception e)
                            {
                                // Ignore failures.
                                System.err.println(e.getMessage());
                                e.printStackTrace(System.err);
                            }
                        }
                    }

                    return true;
                });
            }
            catch (Exception e)
            {
                // Ignore failures.
                System.err.printf("visitInnerArchive: failed analysis of %s:\n", entry.getName());
                e.printStackTrace();
            }
        }
    }

    @Parameters(commandNames = "discover", commandDescription = "Enumerate all files, looking for Niagara config.bog files")
    public class CommandDiscover extends BaseCommand
    {
        @Parameter(names = "--root", description = "Root directory for the import", required = true)
        public String rootDir;

        @Parameter(names = "--output", description = "Output directory for the dump", required = true)
        public String outputDir;

        @Override
        public void exec() throws
                           Exception
        {
            Multimap<String, ParsedStation> tables = scan(rootDir);

            for (String stationName : tables.keySet())
            {
                List<ParsedStation> parsedStations = Lists.newArrayList(tables.get(stationName));
                parsedStations.sort((a, b) -> b.timestamp.compareTo(a.timestamp));

                for (ParsedStation station : parsedStations)
                {
                    System.out.printf("Station: %s Date: %s File: %s%n", station.name, station.timestamp, station.file);
                }

                ParsedStation mostRecentStation = parsedStations.get(0);

                final BStation root = mostRecentStation.getRoot();

                System.out.printf("Dumping information for station: %s Date: %s File: %s%n", mostRecentStation.name, mostRecentStation.timestamp, mostRecentStation.file);
                try (FileOutputStream stream = new FileOutputStream(outputDir + "/" + mostRecentStation.name + ".txt"))
                {
                    final PrintStream printStream = new PrintStream(stream);

                    Set<ParsedType> types = Sets.newHashSet();
                    root.collectTypes(types);

                    List<ParsedType> sortedTypes = Lists.newArrayList(types);
                    sortedTypes.sort(Comparator.comparing((ParsedType a) -> a.module)
                                               .thenComparing(a -> a.type));

                    for (ParsedType type : sortedTypes)
                    {
                        printStream.printf("Type: %s%n", type);
                    }
                    printStream.println();

                    root.dump(printStream, false);
                }
                System.out.println();
            }

            Runtime.getRuntime()
                   .exit(0);
        }
    }

    @Parameters(commandNames = "import", commandDescription = "Import information from Niagara config.bog files")
    public class CommandImport extends BaseCommand
    {
        @Parameter(names = "--root", description = "Root directory for the import", required = true)
        public String rootDir;

        @Parameter(names = "--output", description = "Output file for the export", required = true)
        public String outputFile;

        private Map<BACnetImportExportData, Set<BACnetImportExportData>> m_devices    = Maps.newHashMap();
        private Map<String, BACnetImportExportData>                      m_pathLookup = Maps.newHashMap();

        @Override
        public void exec() throws
                           Exception
        {
            Multimap<String, ParsedStation> tables             = scan(rootDir);
            List<ParsedStation>             mostRecentStations = Lists.newArrayList();

            for (String stationName : tables.keySet())
            {
                List<ParsedStation> parsedStations = Lists.newArrayList(tables.get(stationName));
                parsedStations.sort((a, b) -> b.timestamp.compareTo(a.timestamp));

                ParsedStation mostRecentStation = parsedStations.get(0);

                final BStation root = mostRecentStation.getRoot();
                if (root.Drivers != null)
                {
                    mostRecentStations.add(mostRecentStation);
                }
            }

            //
            // First pass, just collect devices.
            //
            for (ParsedStation mostRecentStation : mostRecentStations)
            {
                if (verbose)
                {
                    System.out.printf("Exporting information for station: %s Date: %s File: %s%n", mostRecentStation.name, mostRecentStation.timestamp, mostRecentStation.file);
                }

                final BStation root = mostRecentStation.getRoot();
                root.Drivers.enumerateNetworks((network) ->
                                               {
                                                   if (verbose)
                                                   {
                                                       System.out.printf("  Network: %s%n", network);
                                                   }

                                                   network.enumerateDevices(BBacnetDevice.class, BFolder.class, this::analyzeDevice);
                                               });
            }

            //
            // Second pass, resolve references.
            //
            for (ParsedStation mostRecentStation : mostRecentStations)
            {
                final BStation root = mostRecentStation.getRoot();

                root.Drivers.enumerateNetworks((network) ->
                                               {
                                                   network.enumerateDevices(BNiagaraStation.class, BFolder.class, this::resolveDevice);
                                               });
            }

            List<BACnetImportExportData> results = Lists.newArrayList();

            for (BACnetImportExportData imEx : m_devices.keySet())
            {
                results.addAll(m_devices.get(imEx));
            }

            ImportExportData.sort(results);

            ImportExportData.save(new File(outputFile), results);

            Runtime.getRuntime()
                   .exit(0);
        }

        private void analyzeDevice(BBacnetDevice device,
                                   List<BFolder> devicePath) throws
                                                             Exception
        {
            if (device.address == null)
            {
                return;
            }

            if (device.config == null || device.config.deviceObject == null)
            {
                return;
            }

            BACnetImportExportData imEx = new BACnetImportExportData();

            imEx.networkId = device.address.networkNumber.numericValue;
            imEx.instanceId = device.config.deviceObject.objectId.id.instance_number.unbox();

            if (verbose)
            {
                System.out.printf("    Device %s: %s/%s %s%n", device.name, imEx.networkId, imEx.instanceId, CollectionUtils.transformToList(devicePath, (folder) -> folder.name.rawValue));
            }

            if (!m_devices.containsKey(imEx))
            {
                final Set<BACnetImportExportData> points = Sets.newHashSet();

                BACnetImportExportData imExDevice = new BACnetImportExportData();

                imExDevice.networkId = imEx.networkId;
                imExDevice.instanceId = imEx.instanceId;
                imExDevice.objectId = device.config.deviceObject.objectId.id;
                imExDevice.deviceName = device.name.getDecodedValue();
                imExDevice.deviceStructure = CollectionUtils.transformToList(devicePath, (folder) -> folder.name.getDecodedValue());

                points.add(imExDevice);
                m_pathLookup.put(device.getPath(), imExDevice);

                device.enumeratePoints(BControlPoint.class, BBacnetProxyExt.class, BPointFolder.class, (point, pointExt, pointPath) ->
                {
                    final List<String> pathText = CollectionUtils.transformToList(pointPath, (folder) -> folder.name.getDecodedValue());

                    BACnetImportExportData imExPoint = new BACnetImportExportData();

                    imExPoint.networkId = imEx.networkId;
                    imExPoint.instanceId = imEx.instanceId;
                    imExPoint.objectId = pointExt.objectId.id;
                    imExPoint.deviceName = point.name.getDecodedValue();
                    imExPoint.deviceStructure = pathText;

                    if (verbose)
                    {
                        System.out.printf("      Point %-30s: %s %s %s%n", imExPoint.deviceName, point.getPath(), imExPoint.objectId, imExPoint.deviceStructure);
                    }

                    points.add(imExPoint);
                    m_pathLookup.put(point.getPath(), imExPoint);
                });

                m_devices.put(imEx, points);
            }
        }

        private void resolveDevice(BNiagaraStation device,
                                   List<BFolder> devicePath) throws
                                                             Exception
        {
            device.enumeratePoints(BControlPoint.class, BNiagaraProxyExt.class, BPointFolder.class, (point, pointExt, pointPath) ->
            {
                final List<String> pathText = CollectionUtils.transformToList(pointPath, (folder) -> folder.name.getDecodedValue());

                if (pointExt.pointId != null)
                {
                    final EncodedString slot = pointExt.pointId.parts.get("slot");
                    if (slot != null)
                    {
                        final BACnetImportExportData imEx = m_pathLookup.get(slot.getDecodedValue());
                        if (imEx != null)
                        {
                            imEx.dashboardName = point.name.getDecodedValue();
                            imEx.dashboardStructure = pathText;
                            return;
                        }
                    }

                    System.err.printf("Unable to resolve Point %-30s: %s %s %s%n", point.name, point.getPath(), pointExt.pointId.value, pathText);
                }
                else
                {
                    System.err.printf("Unable to resolve Point %-30s: %s %s%n", point.name, point.getPath(), pathText);
                }
            });
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

    //--//

    public static void main(String[] args) throws
                                           Exception
    {
        disableSlf4jLogger();

        new NiagaraImporter().doMain(args);
    }

    private static void disableSlf4jLogger()
    {
        final Logger root = LoggingUtil.getLoggerContext()
                                       .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.detachAndStopAllAppenders();
    }

    public void doMain(String[] args) throws
                                      Exception
    {
        JCommander.Builder builder = JCommander.newBuilder();

        builder.addObject(this);

        addCommand(builder, new CommandDiscover());
        addCommand(builder, new CommandImport());

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

