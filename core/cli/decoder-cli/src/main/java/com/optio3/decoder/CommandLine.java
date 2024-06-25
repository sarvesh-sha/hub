/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.decoder;

import java.io.File;
import java.lang.reflect.Array;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;

import com.fasterxml.jackson.databind.JsonNode;
import com.optio3.cloud.hub.persistence.asset.ResultStagingRecord;
import com.optio3.cloud.hub.persistence.asset.TimeSeries;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.serialization.ObjectMappers;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class CommandLine
{
    // @formatter:off
    @Option(name = "--input", aliases = { "-i" }, usage = "Input file", metaVar = "<input file>", required = true)
    private String inputFile;

    @Option(name = "--asMetadata", usage = "Decode As Metadata")
    private boolean asMetadata;

    @Option(name = "--asStagedResult", usage = "Decode As Staged Result")
    private boolean asStagedResult;

    @Option(name = "--asTimeSeries", usage = "Decode As Time Series")
    private boolean asTimeSeries;
    // @formatter:on

    //--//

    public static void main(String[] args) throws
                                           Exception
    {
        new CommandLine().doMain(args);
    }

    public void doMain(String[] args) throws
                                      Exception
    {
        CmdLineParser parser = new CmdLineParser(this);

        try
        {
            parser.parseArgument(args);
        }
        catch (CmdLineException e)
        {
            // if there's a problem in the command line, you'll get this exception. this will report an error message.
            System.err.println(e.getMessage());
            System.err.println();

            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();

            return;
        }

        byte[] data = FileUtils.readFileToByteArray(new File(inputFile));

        if (asMetadata)
        {
            var metadata = MetadataMap.decodeMetadata(data);

            for (String key : metadata.keySet())
            {
                System.out.printf("Key: %s\n", key);
                prettyPrint("   ", metadata.getObject(key, JsonNode.class));
                System.out.println();
            }
        }
        else if (asStagedResult)
        {
            var entities = ResultStagingRecord.getEntities(data);
            prettyPrint("   ", entities);
        }
        else if (asTimeSeries)
        {
            var ts         = TimeSeries.decode(data);
            var schemas    = ts.getSchema();
            var timestamps = ts.getTimeStampsAsEpochSeconds();

            schemas.sort(Comparator.comparing((a) -> a.identifier));

            System.out.printf("Version: %s\n\n", ts.getSourceVersion());
            System.out.printf("Number of timestamps: %,d\n\n", timestamps.size());
            System.out.printf("Number of properties: %,d\n", schemas.size());
            for (TimeSeries.SampleSchema schema : schemas)
            {
                int missing = 0;

                for (int i = 0; i < timestamps.size(); i++)
                {
                    if (schema.get(i, Object.class) == null)
                    {
                        missing++;
                    }
                }

                System.out.printf("   %s: %s %s (%,d missing)\n", schema.identifier, schema.type, schema.getResolution(), missing);
            }

            System.out.println();

            System.out.printf("Timestamp");

            for (TimeSeries.SampleSchema schema : schemas)
            {
                System.out.printf("\t%s", schema.identifier);
            }

            System.out.println();

            for (ZonedDateTime timestamp : ts.getTimeStamps())
            {
                System.out.printf("%s", timestamp);

                for (TimeSeries.SampleSchema schema : schemas)
                {
                    Object sample = ts.getSample(timestamp, schema.identifier, false, false, Object.class);
                    if (sample != null)
                    {
                        if (sample.getClass()
                                  .isArray())
                        {
                            StringBuilder sb = new StringBuilder();

                            sb.append("{");

                            for (int i = 0; i < Array.getLength(sample); i++)
                            {
                                if (sb.length() > 1)
                                {
                                    sb.append("|");
                                }

                                sb.append(Array.get(sample, i));
                            }

                            sb.append("}");

                            sample = sb.toString();
                        }
                        else if (sample instanceof Collection)
                        {
                            StringBuilder sb = new StringBuilder();

                            sb.append("{");

                            for (Object o : (Collection<?>) sample)
                            {
                                if (sb.length() > 1)
                                {
                                    sb.append("|");
                                }

                                sb.append(o);
                            }

                            sb.append("}");

                            sample = sb.toString();
                        }
                    }

                    System.out.printf("\t%s", sample);
                }

                System.out.println();
            }
        }
        else
        {
            throw new CmdLineException(parser, "No option specified", null);
        }
    }

    private void prettyPrint(String prefix,
                             Object object)
    {
        String text = ObjectMappers.prettyPrintAsJson(object);

        for (String line : StringUtils.split(text, '\n'))
        {
            System.out.printf("%s%s\n", prefix, line);
        }
    }
}
