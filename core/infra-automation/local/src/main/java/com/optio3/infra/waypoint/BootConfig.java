/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.waypoint;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.model.IEnumDescription;
import com.optio3.util.Resources;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class BootConfig
{
    public enum Options implements IEnumDescription
    {
        RebootTime("OPTIO3_REBOOT_TIME"),
        DisableFrontend("OPTIO3_DISABLE_FRONTEND"),
        DisableWifi("OPTIO3_DISABLE_WIFI"),
        ForceReboot("OPTIO3_FORCE_REBOOT"),
        StaticIp("OPTIO3_IP"),
        Gateway("OPTIO3_GATEWAY"),
        Firewall("OPTIO3_FIREWALL"),
        Swapfile("OPTIO3_SWAPFILE"),
        ModemReset("OPTIO3_MODEM_RESET"),
        ProductionSite("OPTIO3_PRODUCTION_SITE"),
        FactoryFloor("OPTIO3_FACTORY_FLOOR"),
        APN("OPTIO3_APN");

        public final String encoding;

        Options(String encoding)
        {
            this.encoding = encoding;
        }

        public static Options parse(String value)
        {
            for (Options t : values())
            {
                if (StringUtils.equals(t.encoding, value))
                {
                    return t;
                }
            }

            return null;
        }

        @Override
        public String getDisplayName()
        {
            return encoding;
        }

        @Override
        public String getDescription()
        {
            return null;
        }
    }

    public static class OptionAndValue
    {
        public Options key;
        public String  keyRaw;
        public String  value;
    }

    public static class Line
    {
        @JsonIgnore
        public String raw;

        public Options key;
        public String  keyRaw;
        public String  value;
        public boolean isCommented;

        public boolean sameKey(Options key,
                               String keyRaw)
        {
            if (key != null)
            {
                return this.key == key;
            }

            if (keyRaw != null)
            {
                return StringUtils.equals(this.keyRaw, keyRaw);
            }

            return false;
        }
    }

    public List<Line> lines = Lists.newArrayList();

    public static BootConfig parse(String file)
    {
        Pattern pattern = Pattern.compile("(#?)([A-Z0-9_]+)=(.*)");

        try
        {
            BootConfig res = new BootConfig();

            for (String str : Resources.loadLines(file, false))
            {
                Line line = new Line();
                line.raw = str;

                final Matcher matcher = pattern.matcher(str);
                if (matcher.matches())
                {
                    String comment = matcher.group(1);
                    String key     = matcher.group(2);
                    String value   = matcher.group(3);

                    line.key = Options.parse(key);
                    if (line.key == null)
                    {
                        // Not a recognized option, ignore line.
                        line.keyRaw = key;
                    }

                    line.value       = value;
                    line.isCommented = StringUtils.isNotEmpty(comment);
                }

                res.lines.add(line);
            }

            return res;
        }
        catch (Throwable t)
        {
            return null;
        }
    }

    public void save(String file) throws
                                  IOException
    {
        List<String> textLines = Lists.newArrayList();

        for (Line line : lines)
        {
            if (line.key != null)
            {
                textLines.add(String.format("%s%s=%s", line.isCommented ? "#" : "", line.key.encoding, line.value));
            }
            else if (line.keyRaw != null)
            {
                textLines.add(String.format("%s%s=%s", line.isCommented ? "#" : "", line.keyRaw, line.value));
            }
            else
            {
                textLines.add(line.raw);
            }
        }

        try (OutputStream stream = new FileOutputStream(file))
        {
            IOUtils.writeLines(textLines, null, stream, (Charset) null);
        }
    }

    //--//

    public static Map<String, String> convertToPlain(List<OptionAndValue> lst)
    {
        Map<String, String> res = Maps.newHashMap();

        for (OptionAndValue v : lst)
        {
            if (v.key != null)
            {
                res.put(v.key.encoding, v.value);
            }
            else
            {
                res.put(v.keyRaw, v.value);
            }
        }

        return res;
    }

    public static List<OptionAndValue> convertFromPlain(Map<String, String> map)
    {
        List<OptionAndValue> lst = Lists.newArrayList();

        map.forEach((k, v) ->
                    {
                        OptionAndValue ov = new OptionAndValue();
                        ov.value = v;
                        lst.add(ov);

                        Options opt = Options.parse(k);
                        if (opt != null)
                        {
                            ov.key = opt;
                        }
                        else
                        {
                            ov.keyRaw = k;
                        }
                    });

        return lst;
    }

    public List<OptionAndValue> getAll()
    {
        Map<String, OptionAndValue> map = Maps.newHashMap();

        for (Line line : lines)
        {
            if (line.isCommented)
            {
                continue;
            }

            if (line.key != null)
            {
                var ov = new OptionAndValue();
                ov.key   = line.key;
                ov.value = line.value;
                map.put(line.key.encoding, ov);
            }
            else if (line.keyRaw != null)
            {
                var ov = new OptionAndValue();
                ov.keyRaw = line.keyRaw;
                ;
                ov.value = line.value;
                map.put(line.keyRaw, ov);
            }
        }

        return Lists.newArrayList(map.values());
    }

    public Line set(Options key,
                    String keyRaw,
                    String value)
    {
        for (int i = lines.size() - 1; i >= 0; i--)
        {
            Line line = lines.get(i);
            if (line.sameKey(key, keyRaw))
            {
                line.value       = value;
                line.isCommented = false;
                return line;
            }
        }

        Line line = new Line();
        line.key    = key;
        line.keyRaw = keyRaw;
        line.value  = value;
        lines.add(line);
        return line;
    }

    public Line unset(Options key,
                      String keyRaw)
    {
        Line res = null;

        for (Line line : lines)
        {
            if (line.sameKey(key, keyRaw))
            {
                if (!line.isCommented)
                {
                    line.isCommented = true;
                    res              = line;
                }
            }
        }

        return res;
    }

    public Line get(Options key,
                    String keyRaw)
    {
        for (Line line : lines)
        {
            if (line.sameKey(key, keyRaw) && !line.isCommented)
            {
                return line;
            }
        }

        return null;
    }
}
