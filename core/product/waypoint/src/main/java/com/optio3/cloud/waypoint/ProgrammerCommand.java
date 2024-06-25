/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.optio3.concurrency.Executors;
import com.optio3.infra.waypoint.ProgrammerCommands;
import com.optio3.infra.waypoint.ProgrammerStatus;
import com.optio3.interop.mediaaccess.I2cAccess;
import com.optio3.logging.Logger;
import com.optio3.util.BufferUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.Resources;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class ProgrammerCommand extends Command
{
    public static final Logger LoggerInstance = new Logger(ProgrammerCommand.class);

    private final WaypointApplication m_app;

    private int m_i2cAddress = 0x66;

    private boolean m_erase;
    private boolean m_program;

    private boolean m_dumpMemory;
    private int     m_memoryAddress = 0x8000;
    private int     m_memorySize    = 8192;

    public ProgrammerCommand(WaypointApplication app)
    {
        super("programmer", "Runs Programmer logic for ATtiny microcontroller");

        m_app = app;
    }

    @Override
    public void run(Bootstrap<?> bootstrap,
                    Namespace namespace) throws
                                         Exception
    {
        if (m_dumpMemory)
        {
            try (I2cAccess i2cHandler = new I2cAccess(1))
            {
                try
                {
                    LoggerInstance.info("Connecting...");
                    connect(i2cHandler);
                    LoggerInstance.info("Connected!");

                    byte[] buf = readMemory(i2cHandler, m_memoryAddress, m_memorySize);
                    BufferUtils.convertToHex(buf, 0, buf.length, 32, true, (line) -> LoggerInstance.info("%s", line));
                }
                catch (Throwable t)
                {
                    LoggerInstance.error("Failed due to %s", t);

                    // Not responding?
                    exit(2);
                }
            }
        }
        else if (m_program)
        {
            Map<Integer, byte[]> map = loadFirmware();

            try (I2cAccess i2cHandler = new I2cAccess(1))
            {
                try
                {
                    LoggerInstance.info("Connecting...");
                    connect(i2cHandler);
                    LoggerInstance.info("Connected!");

                    if (m_erase)
                    {
                        LoggerInstance.info("Erasing...");
                        eraseChip(i2cHandler);
                        LoggerInstance.info("Erased!");

                        LoggerInstance.info("Reconnecting...");
                        connect(i2cHandler);
                        LoggerInstance.info("Reconnected!");
                    }

                    for (Map.Entry<Integer, byte[]> pair : map.entrySet())
                    {
                        int    address = pair.getKey();
                        byte[] data    = pair.getValue();

                        writeMemory(i2cHandler, address + m_memoryAddress, data, false);
                    }

                    resetCpu(i2cHandler);
                }
                catch (Throwable t)
                {
                    LoggerInstance.error("Failed due to %s", t);

                    // Not responding?
                    exit(2);
                }
            }
        }
        else if (m_erase)
        {
            try (I2cAccess i2cHandler = new I2cAccess(1))
            {
                try
                {
                    LoggerInstance.info("Connecting...");
                    connect(i2cHandler);
                    LoggerInstance.info("Connected!");

                    LoggerInstance.info("Erasing...");
                    eraseChip(i2cHandler);
                    LoggerInstance.info("Erased!");
                }
                catch (Throwable t)
                {
                    LoggerInstance.error("Failed due to %s", t);

                    // Not responding?
                    exit(2);
                }
            }
        }
    }

    @Override
    public void configure(Subparser subparser)
    {
        m_app.addArgumentToCommand(subparser, "--enableLog", "Enable logging", false, WaypointConfiguration::enableLogLevel);

        m_app.addActionToCommand(subparser, "--erase", "Erase Flash memory", false, () ->
        {
            m_erase = true;
        });

        m_app.addActionToCommand(subparser, "--program", "Flash new Firmware version", false, () ->
        {
            m_program = true;
        });

        m_app.addActionToCommand(subparser, "--dumpMemory", "Dump the FLASH contents", false, () ->
        {
            m_dumpMemory = true;
        });

        m_app.addArgumentToCommand(subparser, "--memoryAddress", "Address of memory to dump", false, (value) ->
        {
            m_memoryAddress = parseHex(value);
        });

        m_app.addArgumentToCommand(subparser, "--memorySize", "Size of memory to dump", false, (value) ->
        {
            m_memorySize = parseHex(value);
        });

        m_app.addArgumentToCommand(subparser, "--i2c-address", "I2C Slave address", false, (value) ->
        {
            m_i2cAddress = parseHex(value);
        });
    }

    private int parseHex(String value)
    {
        if (value.startsWith("0x") || value.startsWith("0X"))
        {
            return Integer.parseInt(value.substring(2), 16);
        }

        return Integer.parseInt(value);
    }

    //--//

    private void connect(I2cAccess i2cHandler)
    {
        ProgrammerCommands.StartConnection.writeByte(i2cHandler, m_i2cAddress, 0);

        String failure = waitForCommandExecution(i2cHandler, ProgrammerStatus.Connecting);
        if (failure != null)
        {
            throw Exceptions.newRuntimeException("Failed to connect to device 0x%2x through UPDI (%s)", m_i2cAddress, failure);
        }
    }

    private void eraseChip(I2cAccess i2cHandler)
    {
        ProgrammerCommands.EraseChip.writeByte(i2cHandler, m_i2cAddress, 0);

        String failure = waitForCommandExecution(i2cHandler, ProgrammerStatus.Erasing);
        if (failure != null)
        {
            throw Exceptions.newRuntimeException("Failed to erase FLASH on device 0x%2x (%s)", m_i2cAddress, failure);
        }
    }

    private void resetCpu(I2cAccess i2cHandler)
    {
        ProgrammerCommands.ResetCpu.writeByte(i2cHandler, m_i2cAddress, 0);

        String failure = waitForCommandExecution(i2cHandler, ProgrammerStatus.Resetting);
        if (failure != null)
        {
            throw Exceptions.newRuntimeException("Failed to erase FLASH on device 0x%2x (%s)", m_i2cAddress, failure);
        }
    }

    private byte[] readMemory(I2cAccess i2cHandler,
                              int address,
                              int len)
    {
        byte[] res    = new byte[len];
        int    offset = 0;

        while (len > 0)
        {
            ProgrammerCommands.AddressCursor.writeWord(i2cHandler, m_i2cAddress, address);

            int chunk = Math.min(len, 16);

            ProgrammerCommands.MemoryRead.writeByte(i2cHandler, m_i2cAddress, chunk);

            String failure = waitForCommandExecution(i2cHandler, ProgrammerStatus.Reading);
            if (failure != null)
            {
                throw Exceptions.newRuntimeException("Failed to read memory on device 0x%2x at address 0x%04x (%s)", m_i2cAddress, address, failure);
            }

            for (int i = 0; i < chunk; i++)
            {
                res[offset++] = (byte) ProgrammerCommands.MemoryBuffer.readWord(i2cHandler, m_i2cAddress);
            }

            LoggerInstance.info("readMemory: %04x...", address);

            address += chunk;
            len -= chunk;
        }

        return res;
    }

    private void writeMemory(I2cAccess i2cHandler,
                             int address,
                             byte[] data,
                             boolean isEEPROM)
    {
        int alignment = isEEPROM ? 32 : 64;
        int mask      = ~(alignment - 1);
        int remaining = data.length;
        int offset    = 0;

        while (remaining > 0)
        {
            int addressThisPage = address & mask;
            int addressNextPage = addressThisPage + alignment;
            int addressMax      = address + remaining;

            int chunk = Math.min(addressMax, addressNextPage) - address;

            ProgrammerCommands.AddressCursor.writeWord(i2cHandler, m_i2cAddress, address);

            for (int i = 0; i < chunk; i++)
            {
                ProgrammerCommands.MemoryBuffer.writeByte(i2cHandler, m_i2cAddress, data[offset++]);
            }

            ProgrammerCommands.MemoryWrite.writeByte(i2cHandler, m_i2cAddress, chunk);

            String failure = waitForCommandExecution(i2cHandler, ProgrammerStatus.Writing);
            if (failure != null)
            {
                throw Exceptions.newRuntimeException("Failed to write memory on device 0x%2x at address 0x%04x (%s)", m_i2cAddress, address, failure);
            }

            ProgrammerCommands.WritePage.writeByte(i2cHandler, m_i2cAddress, chunk);

            failure = waitForCommandExecution(i2cHandler, ProgrammerStatus.WritingPage);
            if (failure != null)
            {
                throw Exceptions.newRuntimeException("Failed to program page on device 0x%2x at address 0x%04x (%s)", m_i2cAddress, address, failure);
            }

            LoggerInstance.info("writeMemory: %04x...", address);

            address += chunk;
            remaining -= chunk;
        }
    }

    private String waitForCommandExecution(I2cAccess i2cHandler,
                                           ProgrammerStatus target)
    {
        for (int wait = 0; wait < 500; wait++)
        {
            int status = ProgrammerCommands.ConnectionStatus.readWord(i2cHandler, m_i2cAddress);
            if (ProgrammerStatus.Failure.isSet(status))
            {
                return "Failure";
            }

            if (!target.isSet(status))
            {
                return null;
            }

            Executors.safeSleep(1);
        }

        return "Timeout";
    }

    private void exit(int code)
    {
        Runtime.getRuntime()
               .exit(code);
    }

    private Map<Integer, byte[]> loadFirmware()
    {
        List<String> lines;

        try
        {
            lines = Resources.loadResourceAsLines(ProgrammerCommand.class, "WaypointFirmware/waypoint-firmware-0x0103.hex", false);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        IntelHexParser parser = new IntelHexParser();
        parser.parse(lines);

        return parser.toMap();
    }
}
