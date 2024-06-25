/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra;

import java.util.Base64;
import java.util.Map;

@SuppressWarnings("unchecked")
public enum WellKnownEnvironmentVariable
{
    BuildBranch("OPTIO3_BUILD_BRANCH", String.class)
            {
                @Override
                public String parseValue(String value)
                {
                    return value;
                }

                @Override
                protected <T> String encodeValue(T value)
                {
                    return (String) value;
                }
            },
    BuildCommit("OPTIO3_BUILD_COMMIT", String.class)
            {
                @Override
                public String parseValue(String value)
                {
                    return value;
                }

                @Override
                protected <T> String encodeValue(T value)
                {
                    return (String) value;
                }
            },
    BuildDeployUrl("OPTIO3_BUILD_DEPLOYURL", String.class)
            {
                @Override
                public String parseValue(String value)
                {
                    return value;
                }

                @Override
                protected <T> String encodeValue(T value)
                {
                    return (String) value;
                }
            },
    BuildProd("OPTIO3_BUILD_PROD", Boolean.class)
            {
                @Override
                public Boolean parseValue(String value)
                {
                    return Boolean.parseBoolean(value);
                }

                @Override
                protected <T> String encodeValue(T value)
                {
                    return Boolean.toString((boolean) value);
                }
            },
    Timezone("OPTIO3_TIMEZONE", String.class)
            {
                @Override
                public String parseValue(String value)
                {
                    return value;
                }

                @Override
                protected <T> String encodeValue(T value)
                {
                    return (String) value;
                }
            },
    MaxMemory("OPTIO3_MAX_MEM", Integer.class)
            {
                @Override
                public Integer parseValue(String value)
                {
                    return Integer.parseInt(value);
                }

                @Override
                protected <T> String encodeValue(T value)
                {
                    return Integer.toString((int) value);
                }
            },
    SoftReferenceKeepAliveTime("OPTIO3_SOFT_REF_TIME", Integer.class) // Milliseconds to keep a soft reference, which will be multiplied by the # of free MB at GC time.
            {
                @Override
                public Integer parseValue(String value)
                {
                    return Integer.parseInt(value);
                }

                @Override
                protected <T> String encodeValue(T value)
                {
                    return Integer.toString((int) value);
                }
            },
    FileSystemPatch("OPTIO3_FILESYSTEM_PATCH", byte[].class)
            {
                @Override
                public byte[] parseValue(String value)
                {
                    return Base64.getDecoder()
                                 .decode(value);
                }

                @Override
                protected <T> String encodeValue(T value)
                {
                    return Base64.getEncoder()
                                 .encodeToString((byte[]) value);
                }
            };

    private final String   name;
    private final Class<?> valueClass;

    WellKnownEnvironmentVariable(String name,
                                 Class<?> valueClass)
    {
        this.name       = name;
        this.valueClass = valueClass;
    }

    public static WellKnownEnvironmentVariable parse(String name)
    {
        for (WellKnownEnvironmentVariable l : values())
        {
            if (l.name == null || l.name.equals(name))
            {
                return l;
            }
        }

        return null;
    }

    public String getName()
    {
        return name;
    }

    public Class<?> getValueClass()
    {
        return valueClass;
    }

    public abstract <T> T parseValue(String value);

    public <T> void setValue(Map<String, String> envs,
                             T value)
    {
        envs.put(name, encodeValue(value));
    }

    public String getValue(Map<String, String> envs)
    {
        return envs.get(name);
    }

    protected abstract <T> String encodeValue(T value);
}
