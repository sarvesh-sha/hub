/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.text;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.Maps;
import com.optio3.util.BoxingUtils;
import com.optio3.util.Exceptions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StrLookup;
import org.apache.commons.text.StrSubstitutor;

public class TextSubstitution
{
    private final StrSubstitutor m_substitutor;

    private Map<String, Function<String, String>> m_handlers = Maps.newHashMap();

    public TextSubstitution()
    {
        this(null, null);
    }

    public TextSubstitution(String prefix,
                            String suffix)
    {
        m_substitutor = new StrSubstitutor(new StrLookup<String>()
        {
            @Override
            public String lookup(String key)
            {
                String val = substituteVariable(key);
                if (val == null)
                {
                    throw Exceptions.newRuntimeException("Failed to substitute '%s'", key);
                }

                return val;
            }
        });

        if (prefix != null)
        {
            m_substitutor.setVariablePrefix(prefix);
        }

        if (suffix != null)
        {
            m_substitutor.setVariableSuffix(suffix);
        }

        addHandler("env.", (val) ->
        {
            String defaultValue;
            int    defaultValuePos = val.indexOf('?');
            if (defaultValuePos >= 0)
            {
                defaultValue = val.substring(defaultValuePos + 1);
                val = val.substring(0, defaultValuePos);

                if (StringUtils.isEmpty(defaultValue))
                {
                    defaultValue = null;
                }
            }
            else
            {
                defaultValue = null;
            }

            return BoxingUtils.get(System.getenv(val), defaultValue);
        });
    }

    public void transform(String inputFile,
                          String outputFile) throws
                                             IOException
    {
        String contents = FileUtils.readFileToString(new File(inputFile), (Charset) null);

        String substituted = this.m_substitutor.replace(contents);

        FileUtils.writeStringToFile(new File(outputFile), substituted, (Charset) null);
    }

    public void transform(InputStream inputStream,
                          OutputStream outputStream) throws
                                                     IOException
    {
        String contents = IOUtils.toString(inputStream, (Charset) null);

        String substituted = this.m_substitutor.replace(contents);

        IOUtils.write(substituted, outputStream, (Charset) null);
    }

    public String transform(String inputText)
    {
        return this.m_substitutor.replace(inputText);
    }

    //--//

    public void addHandler(String prefix,
                           Function<String, String> callback)
    {
        m_handlers.put(prefix, callback);
    }

    //--//

    private String substituteVariable(String key)
    {
        try
        {
            for (String prefix : m_handlers.keySet())
            {
                String val = matchVariablePrefix(key, prefix);
                if (val != null)
                {
                    val = m_handlers.get(prefix)
                                    .apply(val);
                    if (val != null)
                    {
                        return val;
                    }
                }
            }

            return null;
        }
        catch (SecurityException scex)
        {
            // Suppressed, just return null.
            return null;
        }
    }

    private String matchVariablePrefix(String key,
                                       String prefix)
    {
        if (key.startsWith(prefix))
        {
            return key.substring(prefix.length());
        }

        return null;
    }
}
