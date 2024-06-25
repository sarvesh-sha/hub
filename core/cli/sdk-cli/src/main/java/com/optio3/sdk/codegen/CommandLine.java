/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.sdk.codegen;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class CommandLine
{
    public enum Language
    {
        Java,
        Typescript,
        Python
    }

    // @formatter:off
    @Option(name = "--output", aliases = { "-o" }, usage = "Output directory", metaVar = "<output dir>", required = true)
    private String outputDir;

    @Option(name = "--spec", aliases = { "-s" }, usage = "URL to fetch the Swagger specification from", metaVar = "<URL for Swagger spec>", required = true)
    private String spec;

    @Option(name = "--package", aliases = { "-p" }, usage = "Java package to generated the code for", metaVar = "<java package spec>")
    private String pkg;

    @Option(name = "--lang", aliases = { "-l" }, usage = "Target language", metaVar = "<Java | Typescript>", required = true)
    private Language lang;

    @Option(name = "--dumpSpec", usage = "Print Swagger spec")
    private boolean dumpSpec;

    @Option(name = "--deleteOldFiles", usage = "Delete all the files in the target directories before generating the new code")
    private boolean deleteOldFiles;

    @Option(name = "--noOptio3Enum", usage = "Disable processing of Optio3 Enum directives")
    private boolean noOptio3Enum;
    // @formatter:on

    //--//

    public static void main(String[] args) throws
                                           Exception
    {
        new CommandLine().doMain(args);
    }

    public void doMain(String[] args)
    {
        CmdLineParser parser = new CmdLineParser(this);

        try
        {
            parser.parseArgument(args);

            if (lang == Language.Java && StringUtils.isEmpty(pkg))
            {
                throw new CmdLineException(parser, "No package specified, required when target is Java.", null);
            }
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

        Optio3Coordinator coord = new Optio3Coordinator(spec, outputDir);

        if (noOptio3Enum)
        {
            coord.setProcessEnumDirectives(false);
        }

        Optio3Generator gen;

        if (lang == Language.Java)
        {
            gen = coord.configureForJava(pkg);
        }
        else if (lang == Language.Typescript)
        {
            gen = coord.configureForTypescript();
        }
        else if (lang == Language.Python)
        {
            gen = coord.configureForPython();
        }
        else
        {
            gen = null;
        }

        if (deleteOldFiles)
        {
            coord.deleteOldFiles();
        }

        if (dumpSpec)
        {
            gen.dumpSwagger();
        }

        gen.generate();

        gen.emitProcessedFiles();
    }
}
