/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;

import com.google.common.base.Charsets;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import com.optio3.logging.Logger;
import com.optio3.util.Resources;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class GpioHelper
{
    public static final Logger LoggerInstance = new Logger(GpioHelper.class);

    private static final Supplier<GpioHelper> s_instance = Suppliers.memoize(() ->
                                                                             {
                                                                                 GpioHelper fh;

                                                                                 LoggerInstance.info("Detecting hardware...");

                                                                                 fh = Linux.probe();

                                                                                 if (fh == null)
                                                                                 {
                                                                                     LoggerInstance.info("No custom hardware detected...");
                                                                                     fh = new Noop();
                                                                                 }

                                                                                 return fh;
                                                                             });

    static class Noop extends GpioHelper
    {
        @Override
        public void deinit(int gpio)
        {
        }

        @Override
        public boolean setDirection(int gpio,
                                    boolean output)
        {
            return true;
        }

        @Override
        public boolean setOutput(int gpio,
                                 boolean value)
        {
            return true;
        }

        @Override
        public Boolean getInput(int gpio)
        {
            return false;
        }
    }

    static abstract class Linux extends GpioHelper
    {
        static final Path s_root = Path.of("/sys/class/gpio");

        private final Map<Integer, Path> m_enabled = Maps.newHashMap();

        static GpioHelper probe()
        {
            File export = s_root.resolve("export")
                                .toFile();
            if (export.isFile())
            {
                try
                {
                    for (String line : Resources.loadLines("/proc/cpuinfo", false))
                    {
                        int pos = line.indexOf(':');
                        if (pos > 0)
                        {
                            String key   = line.substring(0, pos);
                            String value = line.substring(pos + 1);

                            key   = key.trim();
                            value = value.trim();

                            if ("Hardware".equals(key))
                            {
                                if (value.startsWith("STM32"))
                                {
                                    LoggerInstance.info("Detected EdgeV1");
                                    return new Linux_EdgeV1();
                                }

                                if (value.startsWith("BCM27") || value.startsWith("BCM28"))
                                {
                                    LoggerInstance.info("Detected Raspberry PI");
                                    return new Linux_RaspberryPi();
                                }
                            }
                        }
                    }
                }
                catch (Throwable t)
                {
                    return null;
                }
            }

            return null;
        }

        @Override
        public void deinit(int gpio)
        {
            ensureDisabled(gpio);
        }

        @Override
        public boolean setDirection(int gpio,
                                    boolean output)
        {
            Path dir = ensureEnabled(gpio);
            if (dir == null)
            {
                return false;
            }

            return write(dir, "direction", output ? "out" : "in");
        }

        @Override
        public boolean setOutput(int gpio,
                                 boolean value)
        {
            Path dir = ensureEnabled(gpio);
            if (dir == null)
            {
                return false;
            }

            return write(dir, "value", value ? "1" : "0");
        }

        @Override
        public Boolean getInput(int gpio)
        {
            Path dir = ensureEnabled(gpio);
            if (dir == null)
            {
                return null;
            }

            return read(dir, "value");
        }

        private Path ensureEnabled(int gpio)
        {
            Path dir = m_enabled.get(gpio);
            if (dir == null)
            {
                dir = s_root.resolve("gpio" + gpio);

                if (!dir.toFile()
                        .isDirectory())
                {
                    if (!write(s_root, "export", Integer.toString(gpio)))
                    {
                        dir = s_root; // Sentinel.
                    }
                }

                m_enabled.put(gpio, dir);
            }

            return dir == s_root ? null : dir;
        }

        private void ensureDisabled(int gpio)
        {
            write(s_root, "unexport", Integer.toString(gpio));
            m_enabled.remove(gpio);
        }

        private boolean write(Path dir,
                              String fileName,
                              String value)
        {
            Path file = dir.resolve(fileName);

            try
            {
                FileUtils.writeStringToFile(file.toFile(), value, Charsets.US_ASCII);
                return true;
            }
            catch (IOException e)
            {
                return false;
            }
        }

        private boolean read(Path dir,
                             String fileName)
        {
            Path file = dir.resolve(fileName);

            try
            {
                String value = FileUtils.readFileToString(file.toFile(), Charsets.US_ASCII);
                return StringUtils.equals(value, "1");
            }
            catch (IOException e)
            {
                return false;
            }
        }
    }

    static class Linux_RaspberryPi extends Linux
    {
    }

    static class Linux_EdgeV1 extends Linux
    {
    }

    //--//

    public static GpioHelper get()
    {
        return s_instance.get();
    }

    public abstract void deinit(int gpio);

    public abstract boolean setDirection(int gpio,
                                         boolean output);

    public abstract boolean setOutput(int gpio,
                                      boolean value);

    public abstract Boolean getInput(int gpio);
}
