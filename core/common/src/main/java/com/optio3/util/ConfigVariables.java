/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import com.google.common.collect.Maps;
import com.optio3.text.TextSubstitution;
import org.apache.commons.lang3.StringUtils;

public class ConfigVariables<V extends Enum<V> & IConfigVariable>
{
    public static class Template<V extends Enum<V> & IConfigVariable>
    {
        private final Validator<V> m_validator;

        public final String Contents;
        public final String Prefix;
        public final String Suffix;

        private Template(Validator<V> validator,
                         Class<?> clz,
                         String resource,
                         String prefix,
                         String suffix)
        {
            m_validator = validator;

            Prefix = prefix;
            Suffix = suffix;

            String contents = null;

            if (resource != null)
            {
                try
                {
                    contents = Resources.loadResourceAsText(clz, resource, false);

                    TextSubstitution sub = new TextSubstitution(prefix, suffix);
                    sub.addHandler("param.", (val) ->
                    {
                        if (!validator.isValidVariable(val))
                        {
                            throw Exceptions.newIllegalArgumentException("Invalid key '%s', not one of the values %s", val, validator.m_allowedVariables);
                        }

                        return val;
                    });

                    sub.transform(contents);
                }
                catch (Exception e)
                {
                    System.err.printf("Config Template verification failure for '%s/%s'%n", clz, resource);
                    System.err.println(e.getMessage());
                    e.printStackTrace(System.err);
                    Runtime.getRuntime()
                           .exit(10);
                }
            }

            Contents = contents;
        }

        public ConfigVariables<V> allocate()
        {
            return new ConfigVariables<V>(m_validator, this);
        }
    }

    public static class Validator<V extends Enum<V> & IConfigVariable>
    {
        private final Enum<V>[] m_allowedVariables;

        public Validator(Enum<V>[] allowedVariables)
        {
            m_allowedVariables = allowedVariables;
        }

        public Template<V> newTemplate(Class<?> clz,
                                       String resource,
                                       String prefix,
                                       String suffix)
        {
            return new Template<V>(this, clz, resource, prefix, suffix);
        }

        private boolean isValidVariable(String val)
        {
            for (Enum<V> allowedKey : m_allowedVariables)
            {
                IConfigVariable allowedKey2 = (IConfigVariable) allowedKey;

                if (allowedKey2.getVariable()
                               .equals(val))
                {
                    return true;
                }
            }

            return false;
        }
    }

    private final Validator<V>        m_validator;
    private final Template            m_template;
    private final Map<String, String> m_parameters = Maps.newHashMap();

    private ConfigVariables(Validator<V> validator,
                            Template template)
    {
        m_validator = validator;
        m_template = template;
    }

    public void setValue(V key,
                         String value)
    {
        m_parameters.put(key.getVariable(), value);
    }

    public void setValue(V key,
                         boolean value)
    {
        setValue(key, value ? "true" : "false");
    }

    public void setValue(V key,
                         ZonedDateTime value)
    {
        final ZonedDateTime localValue = value.withZoneSameInstant(ZoneId.systemDefault());
        setValue(key, TimeUtils.DEFAULT_FORMATTER_NO_MILLI.format(localValue));
    }

    public String convert()
    {
        return convert(m_template.Contents);
    }

    public String convert(String input)
    {
        TextSubstitution sub = new TextSubstitution(m_template.Prefix, m_template.Suffix);
        sub.addHandler("param.", (val) ->
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

            return BoxingUtils.get(resolve(val), defaultValue);
        });

        return sub.transform(input);
    }

    private String resolve(String val)
    {
        if (m_validator.isValidVariable(val))
        {
            return m_parameters.get(val);
        }

        throw Exceptions.newIllegalArgumentException("Invalid key '%s', not one of the values %s", val, m_validator.m_allowedVariables);
    }
}
