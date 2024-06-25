/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import com.google.common.collect.Lists;

public class Resources
{
    public static InputStream openResourceAsStream(Class<?> context,
                                                   String resource)
    {
        if (context == null)
        {
            context = Resources.class;
        }

        ClassLoader classLoader = context.getClassLoader();
        return classLoader.getResourceAsStream(resource);
    }

    public static BufferedReader openResourceAsBufferedReader(Class<?> context,
                                                              String resource)
    {
        InputStream       stream = openResourceAsStream(context, resource);
        InputStreamReader reader = new InputStreamReader(stream);
        return new BufferedReader(reader);
    }

    public static URL openResource(Class<?> context,
                                   String resource)
    {
        if (context == null)
        {
            context = Resources.class;
        }

        ClassLoader classLoader = context.getClassLoader();
        return classLoader.getResource(resource);
    }

    public static List<String> loadLines(String file,
                                         boolean skipLinesStartingWithComments) throws
                                                                                IOException
    {
        try (InputStream stream = new FileInputStream(file))
        {
            try (InputStreamReader reader = new InputStreamReader(stream))
            {
                try (BufferedReader reader2 = new BufferedReader(reader))
                {
                    return loadLines(reader2, skipLinesStartingWithComments);
                }
            }
        }
    }

    public static List<String> loadLines(BufferedReader reader,
                                         boolean skipLinesStartingWithComments) throws
                                                                                IOException
    {
        List<String> res = Lists.newArrayList();

        while (true)
        {
            String line = reader.readLine();
            if (line == null)
            {
                return res;
            }

            if (skipLinesStartingWithComments && line.startsWith("//"))
            {
                continue;
            }

            res.add(line);
        }
    }

    public static String loadResourceAsText(Class<?> context,
                                            String resource,
                                            boolean skipLinesStartingWithComments) throws
                                                                                   IOException
    {
        return String.join("\n", loadResourceAsLines(context, resource, skipLinesStartingWithComments));
    }

    public static List<String> loadResourceAsLines(Class<?> context,
                                                   String resource,
                                                   boolean skipLinesStartingWithComments) throws
                                                                                          IOException
    {
        try (BufferedReader reader = openResourceAsBufferedReader(context, resource))
        {
            return loadLines(reader, skipLinesStartingWithComments);
        }
    }
}
