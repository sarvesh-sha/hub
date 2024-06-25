/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.text;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.util.Exceptions;

public class CommandLineTokenizer
{
    public static List<String> translate(String line)
    {
        List<String> tokens = Lists.newArrayList();

        if (line != null)
        {
            StringBuilder current   = new StringBuilder();
            int           quoteChar = -1;

            for (int pos = 0; pos < line.length(); pos++)
            {
                char c = line.charAt(pos);

                switch (c)
                {
                    case '\'':
                    case '\"':
                        if (quoteChar == c)
                        {
                            quoteChar = -1;
                            continue;
                        }

                        if (quoteChar == -1)
                        {
                            quoteChar = c;
                            continue;
                        }
                        break;

                    case ' ':
                        if (quoteChar == -1)
                        {
                            flush(tokens, current);
                            continue;
                        }

                        break;
                }

                current.append(c);
            }

            flush(tokens, current);

            if (quoteChar != -1)
            {
                throw Exceptions.newIllegalArgumentException("unbalanced quotes in %s", line);
            }
        }

        return tokens;
    }

    private static void flush(List<String> res,
                              StringBuilder current)
    {
        if (current.length() > 0)
        {
            res.add(current.toString());
            current.setLength(0);
        }
    }
}
