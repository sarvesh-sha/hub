/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.cli;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.qos.logback.classic.Logger;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.JCommander.Builder;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.DataType;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import com.optio3.metadata.normalization.BACnetImportExportData;
import com.optio3.metadata.normalization.ImportExportData;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.serialization.Reflection;
import com.optio3.util.FileSystem;
import io.dropwizard.logging.LoggingUtil;
import org.apache.commons.lang3.StringUtils;

public class AlertonImporter
{
    public static class Schema
    {
        public Set<Path> files = Sets.newHashSet();

        public String tableName;

        public Map<String, DataType> columns = Maps.newHashMap();

        static Schema merge(Collection<Schema> schemas)
        {
            Schema merged = new Schema();

            for (Schema schema : schemas)
            {
                merged.tableName = schema.tableName;

                merged.files.addAll(schema.files);

                for (String column : schema.columns.keySet())
                {
                    String   columnLower = column.toLowerCase();
                    DataType d1          = schema.columns.get(column);
                    DataType d2          = merged.columns.get(columnLower);

                    if (d2 == null)
                    {
                        merged.columns.put(columnLower, d1);
                    }
                    else if (d1 != d2)
                    {
                        System.err.printf("Two columns with same name but different type: %s/%s : %s <=> %s%n", merged.tableName, column, d1, d2);
                    }
                }
            }

            return merged;
        }
    }

    public static class DataImport<T>
    {
        public Path file;

        public Class<T> clz;

        public List<T> values = Lists.newArrayList();

        //--//

        public void dump(PrintStream out,
                         String indent) throws
                                        IllegalAccessException
        {
            final Map<String, Field> fields = Reflection.collectFields(clz);

            out.printf("%s%s - %s%n", indent, file, clz.getSimpleName());
            for (Object value : values)
            {
                out.printf("%s ", indent);
                for (Field field : fields.values())
                {
                    out.printf(" %s=%s", field.getName(), field.get(value));
                }
                out.println();
            }
        }

        //--//

        public static <T2> List<DataImport<T2>> create(Collection<Schema> tableSchemas,
                                                       Class<T2> clz,
                                                       Function<T2, Boolean> filter)
        {
            List<DataImport<T2>> results = Lists.newArrayList();

            for (Schema tableSchema : tableSchemas)
            {
                results.addAll(create(tableSchema, clz, filter));
            }

            return results;
        }

        public static <T2> List<DataImport<T2>> create(Schema tableSchema,
                                                       Class<T2> clz,
                                                       Function<T2, Boolean> filter)
        {
            Map<String, Field> fields        = Reflection.collectFields(clz);
            Map<String, Field> columnToField = Maps.newHashMap();

            for (String column : tableSchema.columns.keySet())
            {
                String column2 = column.toLowerCase()
                                       .replace(' ', '_');

                Field field = fields.get(column2);
                if (field != null)
                {
                    columnToField.put(column, field);
                }
            }

            List<DataImport<T2>> results = Lists.newArrayList();

            for (Path file : tableSchema.files)
            {
                try
                {
                    DataImport<T2> newImport = null;

                    Database db = new DatabaseBuilder(file.toFile()).setReadOnly(true)
                                                                    .open();
                    try
                    {
                        Table table = db.getTable(tableSchema.tableName);
                        for (Row row : table)
                        {
                            T2 instance = null;

                            for (String column : columnToField.keySet())
                            {
                                Object value = row.get(column);
                                if (value == null)
                                {
                                    continue;
                                }

                                final Field    field      = columnToField.get(column);
                                final Class<?> targetType = field.getType();

                                if (value instanceof String)
                                {
                                    String text = (String) value;

                                    text = text.trim();

                                    if (StringUtils.isEmpty(text) || text.equals("null") || text.equals("none") || text.equals("0"))
                                    {
                                        continue;
                                    }

                                    if (targetType != String.class)
                                    {
                                        if (targetType == int.class)
                                        {
                                            value = Integer.parseInt(text);
                                        }
                                        else if (targetType == long.class)
                                        {
                                            value = Long.parseLong(text);
                                        }
                                    }
                                }

                                if (value instanceof Number)
                                {
                                    Number num = (Number) value;

                                    if (num.longValue() == 0 && !column.equals("Instance"))
                                    {
                                        continue;
                                    }
                                }
                                else if (value.getClass() != targetType)
                                {
                                    if (targetType == String.class)
                                    {
                                        value = value.toString();
                                    }
                                }

                                if (instance == null)
                                {
                                    instance = Reflection.newInstance(clz);
                                }

                                try
                                {
                                    field.set(instance, value);
                                }
                                catch (Exception e)
                                {
                                    // Ignore failures.
                                }
                            }

                            if (instance != null)
                            {
                                if (filter == null || filter.apply(instance))
                                {
                                    if (newImport == null)
                                    {
                                        newImport = new DataImport<T2>();
                                        newImport.clz = clz;
                                        newImport.file = file;
                                        results.add(newImport);
                                    }

                                    newImport.values.add(instance);
                                }
                            }
                        }
                    }
                    finally
                    {
                        db.close();
                    }
                }
                catch (Exception e)
                {
                    // Ignore failures.
                }
            }

            return results;
        }
    }

    //--//

    public static abstract class BaseCommand
    {
        public abstract void exec() throws
                                    Exception;

        protected Multimap<String, Schema> scan(String path) throws
                                                             IOException
        {
            Multimap<String, Schema> tables = HashMultimap.create();

            File root = new File(path);
            if (root.isFile())
            {
                visit(tables, root.toPath());
            }
            else if (root.isDirectory())
            {
                final Function<Path, Boolean> dirFilterLambda = (fileFilter) ->
                {
                    String name = fileFilter.toFile()
                                            .getName()
                                            .toLowerCase();

                    switch (name)
                    {
                        case "tempmdb":
                            return false;
                    }

                    return true;
                };

                final Function<Path, Boolean> fileFilterLambda = (fileFilter) ->
                {
                    String name = fileFilter.toFile()
                                            .getName()
                                            .toLowerCase();

                    if (name.startsWith("trendlog"))
                    {
                        return false;
                    }

                    if (name.endsWith(".mdb"))
                    {
                        return true;
                    }
                    if (name.endsWith(".asc"))
                    {
                        return true;
                    }
                    if (name.endsWith(".gbl"))
                    {
                        return true;
                    }

                    return false;
                };

                FileSystem.walkDirectory(root.toPath(), dirFilterLambda, fileFilterLambda, (file) -> visit(tables, file));
            }
            else
            {
                System.err.printf("Invalid path '%s'%n", path);
                Runtime.getRuntime()
                       .exit(10);
            }

            return tables;
        }

        protected void visit(Multimap<String, Schema> tables,
                             Path file)
        {
            try
            {
                Database db = new DatabaseBuilder(file.toFile()).setReadOnly(true)
                                                                .open();
                try
                {
                    loop:
                    for (String tableName : db.getTableNames())
                    {
                        Schema tableSchema = new Schema();
                        tableSchema.tableName = tableName;
                        tableSchema.files.add(file);

                        Table table = db.getTable(tableName);
                        for (Column column : table.getColumns())
                        {
                            tableSchema.columns.put(column.getName(), column.getType());
                        }

                        for (Schema tableSchemaOld : tables.get(tableName))
                        {
                            if (tableSchemaOld.columns.equals(tableSchema.columns))
                            {
                                tableSchemaOld.files.add(file);
                                continue loop;
                            }
                        }

                        tables.put(tableName, tableSchema);
                    }
                }
                finally
                {
                    db.close();
                }
            }
            catch (Exception e)
            {
                // Ignore failures.
            }
        }
    }

    @Parameters(commandNames = "discover", commandDescription = "Enumerate all files, looking for MS Access databases")
    public class CommandDiscover extends BaseCommand
    {
        @Parameter(names = "--root", description = "Root directory for the import", required = true)
        public String rootDir;

        @Parameter(names = "--filter", description = "Only show rows with data in this column", required = false)
        public String columnFilter;

        @Parameter(names = "--dumpRows", description = "Show rows", required = false)
        public boolean dumpRows;

        @Override
        public void exec() throws
                           Exception
        {
            Multimap<String, Schema> tables = scan(rootDir);

            final List<String> tableNames = Lists.newArrayList(tables.keySet());
            tableNames.sort(String::compareTo);
            for (String tableName : tableNames)
            {
                System.out.printf("Table: %s%n", tableName);

                {
                    Schema mergedSchema = Schema.merge(tables.get(tableName));

                    List<String> columns = Lists.newArrayList(mergedSchema.columns.keySet());
                    columns.sort(String::compareTo);
                    for (String column : columns)
                    {
                        System.out.printf("  Column %s: %s%n", column, mergedSchema.columns.get(column));
                    }
                }

                for (Schema tableSchema : tables.get(tableName))
                {
                    if (dumpRows)
                    {
                        List<String> columns = Lists.newArrayList(tableSchema.columns.keySet());
                        columns.sort(String::compareTo);

                        if (columnFilter == null || columns.contains(columnFilter))
                        {
                            for (Path file : tableSchema.files)
                            {
                                try
                                {
                                    boolean emitFile = true;
                                    Database db = new DatabaseBuilder(file.toFile()).setReadOnly(true)
                                                                                    .open();
                                    try
                                    {
                                        Table table = db.getTable(tableName);
                                        for (Row row : table)
                                        {
                                            if (columnFilter != null)
                                            {
                                                String text = row.getString(columnFilter);
                                                if (text == null)
                                                {
                                                    continue;
                                                }

                                                text = text.trim();
                                                if (StringUtils.isEmpty(text))
                                                {
                                                    continue;
                                                }
                                            }

                                            StringBuilder sb = new StringBuilder();

                                            for (String column : columns)
                                            {
                                                Object value = row.get(column);
                                                if (value == null)
                                                {
                                                    continue;
                                                }

                                                if (value instanceof String)
                                                {
                                                    String text = (String) value;

                                                    if (StringUtils.isEmpty(text) || text.equals("null") || text.equals("0"))
                                                    {
                                                        continue;
                                                    }
                                                }

                                                if (value instanceof Number)
                                                {
                                                    Number num = (Number) value;

                                                    if (num.longValue() == 0 && !column.equals("Instance"))
                                                    {
                                                        continue;
                                                    }
                                                }

                                                if (sb.length() > 0)
                                                {
                                                    sb.append(", ");
                                                }

                                                sb.append(column);
                                                sb.append("=");
                                                sb.append(value);
                                            }

                                            if (emitFile)
                                            {
                                                System.out.printf("    File %s%n",
                                                                  file.toAbsolutePath()
                                                                      .toString());

                                                emitFile = false;
                                            }

                                            System.out.printf("      Row: %s [%s]%n",
                                                              tableName,
                                                              sb.toString()
                                                                .replace("\n", "\\n"));
                                        }
                                    }
                                    finally
                                    {
                                        db.close();
                                    }
                                }
                                catch (Exception e)
                                {
                                }
                            }
                        }
                    }

                    System.out.println();
                }
            }

            Runtime.getRuntime()
                   .exit(0);
        }
    }

    @Parameters(commandNames = "import", commandDescription = "Enumerate all files, importing data from MS Access databases")
    public class CommandImport extends BaseCommand
    {
        @Parameter(names = "--root", description = "Root directory for the import", required = true)
        public String rootDir;

        @Parameter(names = "--output", description = "Output file", required = true)
        public String outputFile;

        @Parameter(names = "--dumpRows", description = "Show rows", required = false)
        public boolean dumpRows;

        @Override
        public void exec() throws
                           Exception
        {
            Multimap<String, Schema> tables = scan(rootDir);

            Multimap<Class<?>, DataImport<?>> imports = HashMultimap.create();

            final List<String> tableNames = Lists.newArrayList(tables.keySet());
            tableNames.sort(String::compareTo);
            for (String tableName : tableNames)
            {
                final Collection<Schema> tableSchemas = tables.get(tableName);

                switch (tableName)
                {
                    case "Analog Inputs":
                        for (DataImport<AnalogInput> newData : DataImport.create(tableSchemas, AnalogInput.class, (v) -> v.name != null || v.description != null))
                        {
                            imports.put(AnalogInput.class, newData);
                        }
                        break;

                    case "Analog Outputs":
                        for (DataImport<AnalogOutput> newData : DataImport.create(tableSchemas, AnalogOutput.class, (v) -> v.name != null || v.description != null))
                        {
                            imports.put(AnalogOutput.class, newData);
                        }
                        break;

                    case "Analog Values":
                        for (DataImport<AnalogValue> newData : DataImport.create(tableSchemas, AnalogValue.class, (v) -> v.name != null || v.description != null))
                        {
                            imports.put(AnalogValue.class, newData);
                        }
                        break;

                    case "Binary Inputs":
                        for (DataImport<BinaryInput> newData : DataImport.create(tableSchemas, BinaryInput.class, (v) -> v.name != null || v.description != null))
                        {
                            imports.put(BinaryInput.class, newData);
                        }
                        break;

                    case "Binary Outputs":
                        for (DataImport<BinaryOutput> newData : DataImport.create(tableSchemas, BinaryOutput.class, (v) -> v.name != null || v.description != null))
                        {
                            imports.put(BinaryOutput.class, newData);
                        }
                        break;

                    case "Binary Values":
                        for (DataImport<BinaryValue> newData : DataImport.create(tableSchemas, BinaryValue.class, (v) -> v.name != null || v.description != null))
                        {
                            imports.put(BinaryValue.class, newData);
                        }
                        break;

                    case "Multistate Inputs":
                        for (DataImport<MultiStateInput> newData : DataImport.create(tableSchemas, MultiStateInput.class, (v) -> v.name != null || v.description != null))
                        {
                            imports.put(MultiStateInput.class, newData);
                        }
                        break;

                    case "Multistate Outputs":
                        for (DataImport<MultiStateOutput> newData : DataImport.create(tableSchemas, MultiStateOutput.class, (v) -> v.name != null || v.description != null))
                        {
                            imports.put(MultiStateOutput.class, newData);
                        }
                        break;

                    case "Multistate Values":
                        for (DataImport<MultiStateValue> newData : DataImport.create(tableSchemas, MultiStateValue.class, (v) -> v.name != null || v.description != null))
                        {
                            imports.put(MultiStateValue.class, newData);
                        }
                        break;

                    case "tblDevices":
                        for (DataImport<tblDevices> newData : DataImport.create(tableSchemas, tblDevices.class, null))
                        {
                            imports.put(tblDevices.class, newData);
                        }
                        break;

                    case "tblDevCapabilities":
                        for (DataImport<tblDevCapabilities> newData : DataImport.create(tableSchemas, tblDevCapabilities.class, null))
                        {
                            imports.put(tblDevCapabilities.class, newData);
                        }
                        break;

                    case "tblTrendlogList":
                        for (DataImport<tblTrendlogList> newData : DataImport.create(tableSchemas, tblTrendlogList.class, null))
                        {
                            imports.put(tblTrendlogList.class, newData);
                        }
                        break;

//
//                    default:
//                        System.out.printf("Skipping %s%n", tableName);
                }
            }

            Multimap<Integer, DataImport<?>> deviceToImports = HashMultimap.create();
            Multimap<String, DataImport<?>>  ddcToImports    = HashMultimap.create();

            Pattern ddcRegex  = Pattern.compile("/ddc/([a-zA-Z0-9_]+)\\.asc$");
            Pattern dev1Regex = Pattern.compile("/dev([0-9]+)/pointdata\\.mdb$");
            Pattern dev2Regex = Pattern.compile("/de([0-9]+)/pointdata\\.mdb$");
            Pattern dev3Regex = Pattern.compile("/d([0-9]+)/pointdata\\.mdb$");

            for (DataImport<?> dataImport : imports.values())
            {
                String path = dataImport.file.toString()
                                             .toLowerCase();

                final Matcher ddcMatcher = ddcRegex.matcher(path);
                if (ddcMatcher.find())
                {
                    String ddcName = ddcMatcher.group(1);
                    ddcToImports.put(ddcName, dataImport);
                }

                final Matcher dev1Matcher = dev1Regex.matcher(path);
                if (dev1Matcher.find())
                {
                    int dev = Integer.parseInt(dev1Matcher.group(1));
                    deviceToImports.put(dev, dataImport);
                }

                final Matcher dev2Matcher = dev2Regex.matcher(path);
                if (dev2Matcher.find())
                {
                    int dev = Integer.parseInt(dev2Matcher.group(1));
                    deviceToImports.put(dev, dataImport);
                }

                final Matcher dev3Matcher = dev3Regex.matcher(path);
                if (dev3Matcher.find())
                {
                    int dev = Integer.parseInt(dev3Matcher.group(1));
                    deviceToImports.put(dev, dataImport);
                }
            }

            Map<tblDevices, Multimap<Class<?>, DataImport<?>>> deviceDetails = Maps.newHashMap();

            for (DataImport<?> dataImport : imports.get(tblDevices.class))
            {
                for (Object value : dataImport.values)
                {
                    tblDevices desc = (tblDevices) value;

                    if (desc.devinst > 0)
                    {
                        Collection<DataImport<?>> deviceImports = deviceToImports.get(desc.devinst);
                        if (deviceImports != null)
                        {
                            if (verbose)
                            {
                                System.out.printf("Found device %d through number%n", desc.devinst);
                            }

                            for (DataImport<?> deviceImport : deviceImports)
                            {
                                Multimap<Class<?>, DataImport<?>> byClass = deviceDetails.computeIfAbsent(desc, (key) -> HashMultimap.create());
                                byClass.put(deviceImport.clz, deviceImport);
                            }
                        }
                    }

                    if (desc.ddc_app != null)
                    {
                        Collection<DataImport<?>> deviceImports = ddcToImports.get(desc.ddc_app.toLowerCase());
                        if (deviceImports != null)
                        {
                            if (verbose)
                            {
                                System.out.printf("Found device %d through DDC%n", desc.devinst);
                            }

                            for (DataImport<?> deviceImport : deviceImports)
                            {
                                Multimap<Class<?>, DataImport<?>> byClass = deviceDetails.computeIfAbsent(desc, (key) -> HashMultimap.create());
                                byClass.put(deviceImport.clz, deviceImport);
                            }
                        }
                    }
                }
            }

            Map<BACnetImportExportData, BACnetImportExportData> resultsLookup = Maps.newHashMap();

            List<tblDevices> devices = Lists.newArrayList(deviceDetails.keySet());
            devices.sort((a, b) ->
                         {
                             int diff = Integer.compare(a.network, b.network);
                             if (diff == 0)
                             {
                                 diff = Integer.compare(a.devinst, b.devinst);
                             }
                             return diff;
                         });

            for (tblDevices desc : devices)
            {
                final Multimap<Class<?>, DataImport<?>> byClass = deviceDetails.get(desc);

                if (!isStandardName(desc.objname))
                {
                    BACnetImportExportData item = new BACnetImportExportData();
                    item.networkId = desc.network;
                    item.instanceId = desc.devinst;
                    item.objectId = new BACnetObjectIdentifier(BACnetObjectType.device, desc.devinst);
                    item.deviceName = desc.objname;
                    item.dashboardName = desc.objname;

                    merge(resultsLookup, item);
                }

                for (DataImport<AnalogInput> dataImport : enumerate(byClass, AnalogInput.class))
                {
                    for (AnalogInput value : dataImport.values)
                    {
                        if (isStandardName(value.name) && isStandardDescription(value.description))
                        {
                            continue;
                        }

                        BACnetImportExportData item = new BACnetImportExportData();
                        item.networkId = desc.network;
                        item.instanceId = desc.devinst;
                        item.objectId = new BACnetObjectIdentifier(BACnetObjectType.analog_input, value.instance);
                        item.deviceName = value.name;
                        item.dashboardName = value.description;

                        merge(resultsLookup, item);
                    }
                }

                for (DataImport<AnalogOutput> dataImport : enumerate(byClass, AnalogOutput.class))
                {
                    for (AnalogOutput value : dataImport.values)
                    {
                        if (isStandardName(value.name) && isStandardDescription(value.description))
                        {
                            continue;
                        }

                        BACnetImportExportData item = new BACnetImportExportData();
                        item.networkId = desc.network;
                        item.instanceId = desc.devinst;
                        item.objectId = new BACnetObjectIdentifier(BACnetObjectType.analog_output, value.instance);
                        item.deviceName = value.name;
                        item.dashboardName = value.description;

                        merge(resultsLookup, item);
                    }
                }

                for (DataImport<AnalogValue> dataImport : enumerate(byClass, AnalogValue.class))
                {
                    for (AnalogValue value : dataImport.values)
                    {
                        if (isStandardName(value.name) && isStandardDescription(value.description))
                        {
                            continue;
                        }

                        BACnetImportExportData item = new BACnetImportExportData();
                        item.networkId = desc.network;
                        item.instanceId = desc.devinst;
                        item.objectId = new BACnetObjectIdentifier(BACnetObjectType.analog_value, value.instance);
                        item.deviceName = value.name;
                        item.dashboardName = value.description;

                        merge(resultsLookup, item);
                    }
                }

                for (DataImport<BinaryInput> dataImport : enumerate(byClass, BinaryInput.class))
                {
                    for (BinaryInput value : dataImport.values)
                    {
                        if (isStandardName(value.name) && isStandardDescription(value.description))
                        {
                            continue;
                        }

                        BACnetImportExportData item = new BACnetImportExportData();
                        item.networkId = desc.network;
                        item.instanceId = desc.devinst;
                        item.objectId = new BACnetObjectIdentifier(BACnetObjectType.binary_input, value.instance);
                        item.deviceName = value.name;
                        item.dashboardName = value.description;

                        merge(resultsLookup, item);
                    }
                }

                for (DataImport<BinaryOutput> dataImport : enumerate(byClass, BinaryOutput.class))
                {
                    for (BinaryOutput value : dataImport.values)
                    {
                        if (isStandardName(value.name) && isStandardDescription(value.description))
                        {
                            continue;
                        }

                        BACnetImportExportData item = new BACnetImportExportData();
                        item.networkId = desc.network;
                        item.instanceId = desc.devinst;
                        item.objectId = new BACnetObjectIdentifier(BACnetObjectType.binary_output, value.instance);
                        item.deviceName = value.name;
                        item.dashboardName = value.description;

                        merge(resultsLookup, item);
                    }
                }

                for (DataImport<BinaryValue> dataImport : enumerate(byClass, BinaryValue.class))
                {
                    for (BinaryValue value : dataImport.values)
                    {
                        if (isStandardName(value.name) && isStandardDescription(value.description))
                        {
                            continue;
                        }

                        BACnetImportExportData item = new BACnetImportExportData();
                        item.networkId = desc.network;
                        item.instanceId = desc.devinst;
                        item.objectId = new BACnetObjectIdentifier(BACnetObjectType.binary_value, value.instance);
                        item.deviceName = value.name;
                        item.dashboardName = value.description;

                        merge(resultsLookup, item);
                    }
                }

                for (DataImport<MultiStateInput> dataImport : enumerate(byClass, MultiStateInput.class))
                {
                    for (MultiStateInput value : dataImport.values)
                    {
                        if (isStandardName(value.name) && isStandardDescription(value.description))
                        {
                            continue;
                        }

                        BACnetImportExportData item = new BACnetImportExportData();
                        item.networkId = desc.network;
                        item.instanceId = desc.devinst;
                        item.objectId = new BACnetObjectIdentifier(BACnetObjectType.multi_state_input, value.instance);
                        item.deviceName = value.name;
                        item.dashboardName = value.description;

                        merge(resultsLookup, item);
                    }
                }

                for (DataImport<MultiStateOutput> dataImport : enumerate(byClass, MultiStateOutput.class))
                {
                    for (MultiStateOutput value : dataImport.values)
                    {
                        if (isStandardName(value.name) && isStandardDescription(value.description))
                        {
                            continue;
                        }

                        BACnetImportExportData item = new BACnetImportExportData();
                        item.networkId = desc.network;
                        item.instanceId = desc.devinst;
                        item.objectId = new BACnetObjectIdentifier(BACnetObjectType.multi_state_output, value.instance);
                        item.deviceName = value.name;
                        item.dashboardName = value.description;

                        merge(resultsLookup, item);
                    }
                }

                for (DataImport<MultiStateValue> dataImport : enumerate(byClass, MultiStateValue.class))
                {
                    for (MultiStateValue value : dataImport.values)
                    {
                        if (isStandardName(value.name) && isStandardDescription(value.description))
                        {
                            continue;
                        }

                        BACnetImportExportData item = new BACnetImportExportData();
                        item.networkId = desc.network;
                        item.instanceId = desc.devinst;
                        item.objectId = new BACnetObjectIdentifier(BACnetObjectType.multi_state_value, value.instance);
                        item.deviceName = value.name;
                        item.dashboardName = value.description;

                        merge(resultsLookup, item);
                    }
                }
            }

            for (DataImport<?> dataImport : imports.get(tblTrendlogList.class))
            {
                for (Object value : dataImport.values)
                {
                    tblTrendlogList trendLog = (tblTrendlogList) value;

                    final BACnetObjectType objectType = BACnetObjectType.parse((byte) trendLog.objtype);
                    if (objectType == null)
                    {
                        System.out.printf("No objectType for tblTrendlogList: %d: %s/%d %s : %s : %s%n",
                                          trendLog.devnum,
                                          trendLog.objtype,
                                          trendLog.objinst,
                                          trendLog.propid,
                                          trendLog.objname,
                                          trendLog.logdescription);
                        continue;
                    }

                    final BACnetObjectIdentifier objId = new BACnetObjectIdentifier(objectType, trendLog.objinst);

                    BACnetImportExportData item = new BACnetImportExportData();
                    item.instanceId = trendLog.devnum;
                    item.objectId = objId;
                    item.deviceName = trendLog.objname;
                    item.dashboardName = trendLog.logdescription;

//                    final BACnetPropertyIdentifier propId = BACnetPropertyIdentifier.parse((short) trendLog.propid);
//                    merge(resultsLookup, item);
//                    System.out.printf("%d: %s/%d %s : %s : %s%n", trendLog.devnum, objectType, trendLog.objinst, propId, trendLog.objname, trendLog.logdescription);
                }
            }

            List<BACnetImportExportData> results = Lists.newArrayList(resultsLookup.values());
            ImportExportData.sort(results);

            //--//

            if (dumpRows)
            {
                for (BACnetImportExportData item : results)
                {
                    System.out.printf("%d/%d %s %s %s%n", item.networkId, item.instanceId, item.objectId, item.deviceName, item.dashboardName);
                }
            }

            final long maxFileSize = 10 * 1024 * 1024;

            File file = new File(outputFile);
            ImportExportData.save(file, results);
            if (file.length() > maxFileSize)
            {
                long bytesPerEntry   = file.length() / results.size();
                int  maxPartsPerFile = (int) (maxFileSize / bytesPerEntry);

                int pos = outputFile.lastIndexOf('.');
                if (pos < 0)
                {
                    pos = outputFile.length();
                }

                int partSeq = 0;
                int partStart = 0;
                while (partStart < results.size())
                {
                    List<BACnetImportExportData> resultsInPart = Lists.newArrayListWithCapacity(maxPartsPerFile);

                    int partEnd = partStart;
                    while (resultsInPart.size() < maxPartsPerFile && partEnd < results.size())
                    {
                        resultsInPart.add(results.get(partEnd++));
                    }

                    String outputFilePart = String.format("%s.part%d%s", outputFile.substring(0, pos), ++partSeq, outputFile.substring(pos));
                    ImportExportData.save(new File(outputFilePart), resultsInPart);

                    partStart = partEnd;
                }
            }

            Runtime.getRuntime()
                   .exit(0);
        }

        private void merge(Map<BACnetImportExportData, BACnetImportExportData> results,
                           BACnetImportExportData item)
        {
            BACnetImportExportData oldItem = results.get(item);
            if (oldItem == null)
            {
                results.put(item, item);
                return;
            }

            String item_deviceName    = StringUtils.defaultIfEmpty(item.deviceName, "");
            String item_dashboardName = StringUtils.defaultIfEmpty(item.dashboardName, "");

            String oldItem_deviceName    = StringUtils.defaultIfEmpty(oldItem.deviceName, "");
            String oldItem_dashboardName = StringUtils.defaultIfEmpty(oldItem.dashboardName, "");

            if (StringUtils.equalsIgnoreCase(item_dashboardName, oldItem_dashboardName))
            {
                return;
            }

            if (item_deviceName.length() < oldItem_deviceName.length()) // Prefer shorter names.
            {
                System.out.printf("Updating deviceName of '%s/%s %s' : %s => %s%n", oldItem.networkId, oldItem.instanceId, oldItem.objectId, oldItem_deviceName, item_deviceName);

                oldItem.deviceName = item.deviceName;
            }

            if (item_dashboardName.length() > oldItem_dashboardName.length()) // Prefer longer descriptions.
            {
                System.out.printf("Updating dashboardName of '%s/%s %s' : %s => %s%n", oldItem.networkId, oldItem.instanceId, oldItem.objectId, oldItem_dashboardName, item_dashboardName);

                oldItem.dashboardName = item.dashboardName;
            }
        }

        private <T> Collection<DataImport<T>> enumerate(Multimap<Class<?>, DataImport<?>> byClass,
                                                        Class<T> clz)
        {
            List<DataImport<T>> res = Lists.newArrayList();

            for (DataImport<?> dataImport : byClass.get(clz))
            {
                @SuppressWarnings("unchecked") DataImport<T> dataImport2 = (DataImport<T>) dataImport;

                res.add(dataImport2);
            }

            return res;
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

        new AlertonImporter().doMain(args);
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

    //--//

    private static final Pattern s_standardName = Pattern.compile("^(ai|ao|av|bi|bo|bv) +([0-9]+)$");
    private static final Pattern s_standardDesc = Pattern.compile("^(ai|ao|av|bi|bo|bv) +([0-9]+) desc.*$");

    private static boolean isStandardName(String name)
    {
        return name == null || s_standardName.matcher(name.toLowerCase())
                                             .matches();
    }

    private static boolean isStandardDescription(String description)
    {
        return description == null || s_standardDesc.matcher(description.toLowerCase())
                                                    .matches();
    }
}
