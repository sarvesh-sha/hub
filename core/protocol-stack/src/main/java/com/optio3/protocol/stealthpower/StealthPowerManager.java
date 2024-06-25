/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.stealthpower;

import java.nio.file.ClosedFileSystemException;
import java.util.Arrays;

import com.google.common.base.Charsets;
import com.optio3.concurrency.Executors;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.interop.mediaaccess.SerialAccess;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.common.ServiceWorker;
import com.optio3.protocol.common.ServiceWorkerWithWatchdog;
import com.optio3.protocol.model.ipn.objects.stealthpower.BaseStealthPowerModel;
import com.optio3.protocol.model.ipn.objects.stealthpower.StealthPowerSystemState;
import com.optio3.protocol.model.ipn.objects.stealthpower.StealthPowerSystemStateForAMR;
import com.optio3.protocol.model.ipn.objects.stealthpower.StealthPowerSystemStateForCAPMETRO;
import com.optio3.protocol.model.ipn.objects.stealthpower.StealthPowerSystemStateForMTA;
import com.optio3.protocol.model.ipn.objects.stealthpower.StealthPowerSystemStateForPEP;
import com.optio3.protocol.model.ipn.objects.stealthpower.StealthPowerSystemStateForPSEG;
import com.optio3.protocol.model.ipn.objects.stealthpower.StealthPower_AMR;
import com.optio3.protocol.model.ipn.objects.stealthpower.StealthPower_CAPMETRO;
import com.optio3.protocol.model.ipn.objects.stealthpower.StealthPower_FDNY;
import com.optio3.protocol.model.ipn.objects.stealthpower.StealthPower_MTA;
import com.optio3.protocol.model.ipn.objects.stealthpower.StealthPower_PEP;
import com.optio3.protocol.model.ipn.objects.stealthpower.StealthPower_PSEG;
import com.optio3.protocol.model.ipn.objects.stealthpower.StealthPower_PortAuthority;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.BufferUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class StealthPowerManager implements AutoCloseable
{
    public static final Logger LoggerInstance = new Logger(StealthPowerManager.class);

    public static final byte preamble_PortAuthority = 0x55;
    public static final byte preamble_FDNY          = 0x56;
    public static final byte preamble_MTA           = 0x57;
    public static final byte preamble_AMR           = 0x58;
    public static final byte preamble_PSEG          = 0x59;
    public static final byte preamble_PEP           = 0x5A;
    public static final byte preamble_CAPMETRO      = 0x5B;

    private enum State
    {
        Idle,
        GotBootloaderPreamble1,
        GotBootloaderPreamble2,
        GotBootloaderHardwareRevision,
        DownloadingFlowControlLow,
        DownloadingFlowControlHigh,
        DownloadingStatusCodeLow,
        DownloadingStatusCodeHigh,
        GotApplicationPreamble1,
        GotApplicationPreamble2
    }

    private static class PartsParser
    {
        private final String[] m_parts;
        private       int      m_cursor;

        PartsParser(String line,
                    int posChecksum)
        {
            m_parts = StringUtils.split(line.substring(0, posChecksum), ';');
        }

        boolean extractBool()
        {
            return extractInt() != 0;
        }

        byte extractByte()
        {
            String t = extractString();

            return t != null ? Byte.parseByte(t) : 0;
        }

        int extractInt()
        {
            String t = extractString();

            try
            {
                return StringUtils.isNotEmpty(t) ? Integer.parseInt(t) : 0;
            }
            catch (NumberFormatException e)
            {
                return -1;
            }
        }

        String extractString()
        {
            return m_parts != null && m_cursor < m_parts.length ? m_parts[m_cursor++] : "";
        }

        void skip()
        {
            m_cursor++;
        }
    }

    //--//

    private final String        m_serialPort;
    private final ServiceWorker m_serialWorker;
    private       SerialAccess  m_serialTransport;
    private       boolean       m_lastRxFailed;
    private       State         m_state = State.Idle;
    private final OutputBuffer  m_incomingBuffer;

    private byte   m_bootloadVersion;
    private byte   m_hardwareVersion;
    private byte   m_hardwareRevision;
    private byte[] m_payload;
    private int    m_payloadCursor;
    private short  m_bootloaderFlowControl;
    private int    m_bootloaderStatusCode;
    private int    m_applicationVersion;

    public StealthPowerManager(String port)
    {
        m_serialPort = port;

        m_serialWorker = new ServiceWorkerWithWatchdog(LoggerInstance, "StealthPower Serial", 60, 2000, 30)
        {
            @Override
            protected void shutdown()
            {
                closeTransport();
            }

            @Override
            protected void fireWatchdog()
            {
                reportError("StealthPower data flow stopped!");

                closeTransport();
            }

            @Override
            protected void worker()
            {
                byte[] input = new byte[512];

                m_state = State.Idle;

                while (canContinue())
                {
                    SerialAccess transport = m_serialTransport;
                    if (transport == null)
                    {
                        try
                        {
                            FirmwareHelper f = FirmwareHelper.get();
                            f.selectPort(m_serialPort, FirmwareHelper.PortFlavor.RS232, false, false);

                            // This is weird. If we open the serial port once and change the speed, it doesn't stick. We have to close it and reopen it...
                            m_serialTransport = SerialAccess.openMultipleTimes(4, f.mapPort(m_serialPort), 19200, 8, 'N', 1);

                            notifyTransport(m_serialPort, true, false);
                            resetWatchdog();
                        }
                        catch (Throwable t)
                        {
                            reportFailure("Failed to start StealthPower serial", t);

                            workerSleep(10000);
                        }
                    }
                    else
                    {
                        try
                        {
                            int len = transport.read(input, 1000);
                            if (len <= 0)
                            {
                                resetDecoding();

                                if (len < 0)
                                {
                                    closeTransport();
                                    workerSleep(500);
                                }
                            }
                            else
                            {
                                m_lastRxFailed = false;

                                if (LoggerInstance.isEnabled(Severity.DebugObnoxious))
                                {
                                    BufferUtils.convertToHex(input, 0, len, 32, true, (line) -> LoggerInstance.debugObnoxious("RX: %s", line));
                                }

                                boolean progress = false;

                                for (int i = 0; i < len; i++)
                                {
                                    progress |= decode(input[i]);
                                }

                                if (progress)
                                {
                                    resetWatchdog();

                                    reportErrorResolution("StealthPower data flow resumed!");
                                }
                            }
                        }
                        catch (Throwable t)
                        {
                            if (!canContinue())
                            {
                                // The manager has been stopped, exit.
                                return;
                            }

                            Severity level;

                            if (t instanceof ClosedFileSystemException)
                            {
                                // Expected, due to watchdog.
                                level = Severity.Debug;
                            }
                            else if (!m_lastRxFailed)
                            {
                                m_lastRxFailed = true;
                                level          = Severity.Info;
                            }
                            else
                            {
                                level = Severity.Debug;
                            }

                            LoggerInstance.log(null, level, null, null, "Got an exception trying to receive message: %s", t);

                            closeTransport();

                            workerSleep(10000);
                        }
                    }
                }
            }
        };

        m_incomingBuffer = new OutputBuffer();
    }

    //--//

    @Override
    public void close() throws
                        Exception
    {
        m_serialWorker.close();
    }

    public void start()
    {
        m_serialWorker.start();
    }

    public void stop()
    {
        m_serialWorker.stop();
    }

    //--//

    public void sendResetRequest()
    {
        sendStream((byte) 0x43, (byte) 'X', (byte) 'X', (byte) '\r', (byte) '\n');
    }

    public void sendVehicleMovingNotification()
    {
        LoggerInstance.debugVerbose("sendVehicleMovingNotification");

        sendStream((byte) 0x43, (byte) 'S', (byte) 'S', (byte) '\r', (byte) '\n');
    }

    public void sendObdStatus(boolean active)
    {
        LoggerInstance.debugVerbose("sendObdStatus: %s", active ? "ACTIVE" : "INACTIVE");

        if (active)
        {
            sendStream((byte) 0x43, (byte) 'O', (byte) 'O', (byte) '\r', (byte) '\n');
        }
        else
        {
            sendStream((byte) 0x43, (byte) 'N', (byte) 'N', (byte) '\r', (byte) '\n');
        }
    }

    private void sendStream(byte... msg)
    {
        SerialAccess serialTransport = m_serialTransport;
        if (serialTransport != null)
        {
            serialTransport.write(msg, msg.length);

            Executors.safeSleep(20);

            if (LoggerInstance.isEnabled(Severity.DebugObnoxious))
            {
                BufferUtils.convertToHex(msg, 0, msg.length, 32, true, (line) -> LoggerInstance.debugObnoxious("CMD: %s", line));
            }
        }
    }

    //--//

    private synchronized void closeTransport()
    {
        if (m_serialTransport != null)
        {
            try
            {
                m_serialTransport.close();
            }
            catch (Throwable t)
            {
                // Ignore failures.
            }

            m_serialTransport = null;
        }

        notifyTransport(m_serialPort, false, true);
    }

    private void resetDecoding()
    {
        m_state = State.Idle;
        m_incomingBuffer.reset();
    }

    protected boolean decode(byte c)
    {
        LoggerInstance.debugObnoxious("Decode: %s %02x", m_state, c & 0xFF);

        switch (m_state)
        {
            case Idle:
                switch (c)
                {
                    case (byte) 0xAA:
                        m_state = State.GotBootloaderPreamble1;
                        break;

                    case (byte) 0xAB:
                        m_state = State.GotApplicationPreamble1;
                        break;

                    default:
                        resetDecoding();
                        break;
                }
                break;

            case GotBootloaderPreamble1:
                m_bootloadVersion = c;

                switch (c)
                {
                    case preamble_PortAuthority:
                    case preamble_FDNY:
                    case preamble_MTA:
                    case preamble_AMR:
                    case preamble_PEP:
                    case preamble_PSEG:
                    case preamble_CAPMETRO:
                        m_state = State.GotBootloaderPreamble2;
                        break;

                    default:
                        resetDecoding();
                        break;
                }
                break;

            case GotBootloaderPreamble2:
                m_hardwareRevision = c;
                m_state = State.GotBootloaderHardwareRevision;
                break;

            case GotBootloaderHardwareRevision:
                m_hardwareVersion = c;

                m_payload = detectedBootloader(m_bootloadVersion, m_hardwareVersion, m_hardwareRevision);
                if (m_payload != null)
                {
                    m_payloadCursor         = 0;
                    m_bootloaderFlowControl = 0;
                    m_bootloaderStatusCode  = 0;

                    advanceDownload(64);

                    m_state = State.DownloadingFlowControlLow;
                }
                return true;

            case DownloadingFlowControlLow:
                m_bootloaderFlowControl = (short) (c & 0xFF);
                m_state = State.DownloadingFlowControlHigh;
                break;

            case DownloadingFlowControlHigh:
                m_bootloaderFlowControl |= (c & 0xFF) << 8;

                if (m_bootloaderFlowControl == 0)
                {
                    m_state = State.DownloadingStatusCodeLow;
                }
                else
                {
                    if (m_bootloaderFlowControl < 0)
                    {
                        LoggerInstance.debug("Stalled at %d, got %04x...", m_payloadCursor, m_bootloaderFlowControl);
                    }
                    else
                    {
                        advanceDownload(m_bootloaderFlowControl);
                    }

                    m_state = State.DownloadingFlowControlLow;
                }
                return true;

            case DownloadingStatusCodeLow:
                m_bootloaderStatusCode = c & 0xFF;
                m_state = State.DownloadingStatusCodeHigh;
                break;

            case DownloadingStatusCodeHigh:
                m_bootloaderStatusCode |= (c & 0xFF) << 8;

                reportDownloadResult(m_bootloaderStatusCode);
                m_state = State.Idle;
                return true;

            case GotApplicationPreamble1:
                m_applicationVersion = c & 0xFF;
                m_state = State.GotApplicationPreamble2;
                break;

            case GotApplicationPreamble2:
                switch (c)
                {
                    case 0:
                        resetDecoding();
                        break;

                    case '\n':
                    case '\r':
                        if (m_incomingBuffer.size() > 0)
                        {
                            String line = new String(m_incomingBuffer.toByteArray(), Charsets.US_ASCII);
                            LoggerInstance.debugVerbose("Line (%d): %s", m_applicationVersion, line);
                            resetDecoding();

                            int posChecksum = line.lastIndexOf(';');
                            if (posChecksum > 0)
                            {
                                int checksumExpected = Integer.parseInt(line.substring(posChecksum + 1), 16);
                                int checksumActual   = 0xAB + m_applicationVersion;

                                for (int i = 0; i < posChecksum + 1; i++)
                                {
                                    checksumActual += line.charAt(i);
                                }

                                if (((checksumActual + checksumExpected) & 0xFF) == 0xFF)
                                {
                                    PartsParser parser = new PartsParser(line, posChecksum);

                                    switch (m_applicationVersion)
                                    {
                                        case preamble_PortAuthority:
                                        {
                                            //    firmware_version
                                            //    event_type;
                                            //    System_State;
                                            //    SPData.Vsp;
                                            //    SPData.Voem;
                                            //    SPData.V3;  // park/neutral voltage
                                            //    SPData.V4; // parking brake voltage
                                            //    A1_curr; //alternator current
                                            //    SPData.A2; //not used
                                            //    Relays; //number of relays on
                                            //    tempC; // not used
                                            //    Timer_GetIgnitionON();   // ignition signal state
                                            //    parkNeutral;   //park neutral signal state
                                            //    Timer_GetSpare1Switch();  //hood closed state
                                            //    pBrake;  // parking brake state
                                            //    bReboot;        // reboot flag
                                            var obj = new StealthPower_PortAuthority();
                                            obj.firmware_version      = parser.extractString();
                                            obj.event_type            = parser.extractString();
                                            obj.system_state          = StealthPowerSystemState.parse(parser.extractByte());
                                            obj.supply_voltage        = limitRange(parser.extractInt() / 100.0f, 0, 24);
                                            obj.oem_voltage           = limitRange(parser.extractInt() / 100.0f, 0, 24);
                                            obj.park_neutral_voltage  = limitRange(parser.extractInt() / 100.0f, 0, 24);
                                            obj.parking_brake_voltage = limitRange(parser.extractInt() / 100.0f, 0, 24);
                                            obj.alternator_current    = limitRange(parser.extractInt() / 10.0f, -1000, 1000);
                                            parser.skip(); // SPData.A2
                                            obj.active_relays = parser.extractInt();
                                            parser.skip(); // tempC
                                            obj.ignition_signal      = parser.extractBool();
                                            obj.park_neutral_signal  = parser.extractBool();
                                            obj.hood_closed_signal   = parser.extractBool();
                                            obj.parking_brake_signal = parser.extractBool();
                                            obj.reboot_flag          = parser.extractBool();

                                            receivedMessage(obj);
                                            return true;
                                        }

                                        case preamble_FDNY:
                                        {
                                            //    event_type;
                                            //    System_State;
                                            //    SPData.Vsp;
                                            //    SPData.Voem;
                                            //    SPData.V3; //shoreline detection voltage
                                            //    SPData.V4; //Emergency Lights
                                            //    A1_curr; //Stealth Power Batter discharge current
                                            //    SPData.A2; //not used
                                            //    Relays; // number of relays on
                                            //    tempC; // not used
                                            //    Timer_GetIgnitionON();   // ignition signal
                                            //    Timer_GetParkNeutral();   // park signal
                                            //    Timer_GetSpare1Switch();  // foot brake signal
                                            //    bEmergLights;  // emergency lights signal
                                            //    bReboot;
                                            //    check sum
                                            var obj = new StealthPower_FDNY();
                                            obj.firmware_version            = parser.extractString();
                                            obj.event_type                  = parser.extractString();
                                            obj.system_state                = StealthPowerSystemState.parse(parser.extractByte());
                                            obj.supply_voltage              = limitRange(parser.extractInt() / 100.0f, 0, 24);
                                            obj.oem_voltage                 = limitRange(parser.extractInt() / 100.0f, 0, 24);
                                            obj.shoreline_detection_voltage = limitRange(parser.extractInt() / 100.0f, 0, 24);
                                            obj.emergency_lights_voltage    = limitRange(parser.extractInt() / 100.0f, 0, 24);
                                            obj.battery_discharge_current   = limitRange(parser.extractInt() / 10.0f, -1000, 1000);
                                            parser.skip(); // SPData.A2
                                            obj.active_relays = parser.extractInt();
                                            parser.skip(); // tempC
                                            obj.ignition_signal         = parser.extractBool();
                                            obj.park_signal             = parser.extractBool();
                                            obj.foot_brake_signal       = parser.extractBool();
                                            obj.emergency_lights_signal = parser.extractBool();
                                            obj.reboot_flag             = parser.extractBool();
                                            obj.activity_timer          = parser.extractInt();

                                            receivedMessage(obj);
                                            return true;
                                        }

                                        case preamble_MTA:
                                        {
                                            var obj = new StealthPower_MTA();
                                            obj.firmware_version = parser.extractString();

                                            switch (obj.firmware_version)
                                            {
                                                case "1.0.0":
                                                    //    event_type;
                                                    //    System_State;             (0 = all relays off, 1 = engine stop relay on, 2 = engine start relays on)
                                                    //    SPData.Vsp;               91 = .91V -> Vsp (hood closed signal, a value greater than 900 means the hood is open, less than 900 means the hood is closed)
                                                    //    SPData.Voem;              1512 = 15.12V -> Voem (voltage of the OEM battery)
                                                    //    SPData.V3;                393 = 3.93V -> V3 (anything greater than 200 (2V) means that the key is in the ignition, greater than 900 (9V) means the key has just been inserted, otherwise the key is not present)
                                                    //    SPData.V4;                535 = 5.35V-> V4 (PWM signal from fuel pump, will bounce up and down while the engine is running)
                                                    //    Temperature;              80 = 8.0C (current temperature in C inside the vehicle)
                                                    //    park_neutral_signal;      0 = park/neutral signal (0 means in park neutral, 1 = not in park neutral)
                                                    //    engine_running;           2 = engine running signal (anything greater than 0 means the engine is running)
                                                    //    max_discharge_time;       900 = max discharge time in seconds (rotary switch 1 setting)
                                                    //    max_temperature;          266 = 26.6C max temperature allowed inside the vehicle in C before the engine turns back on (rotary switch 2 setting)
                                                    //    min_temperature;          155 = 15.5C min temperature allowed inside the vehicle in C before the engine turns back on (rotary switch 3 setting)
                                                    //    cutoff_voltage;           1180 = 11.8V cutoff voltage setting to turn the engine back on (rotary switch 4 setting)
                                                    //    reboot_flag;              0 = reboot flag (1 means the system rebooted)
                                                    //    check sum
                                                    obj.event_type = parser.extractString();
                                                    obj.system_state = StealthPowerSystemStateForMTA.parse(parser.extractByte());
                                                    obj.hood_closed_signal = parser.extractInt() < 90;
                                                    obj.oem_voltage = limitRange(parser.extractInt() / 100.0f, 0, 24);
                                                    obj.key_inserted = parser.extractInt() > 200;
                                                    obj.fuel_pump = parser.extractInt() / 100.0f;
                                                    obj.temperature = limitRange(parser.extractInt() / 10.0f, -40, 80);
                                                    obj.park_signal = parser.extractInt() == 0;
                                                    obj.engine_running = parser.extractInt() > 0;
                                                    obj.max_discharge_time = parser.extractInt();
                                                    obj.max_temperature = parser.extractInt() / 10.0f;
                                                    obj.min_temperature = parser.extractInt() / 10.0f;
                                                    obj.cutoff_voltage = parser.extractInt() / 100.0f;
                                                    obj.reboot_flag = parser.extractBool();
                                                    break;

                                                case "2.0.1":
                                                case "2.0.2":
                                                    // I;       = message type (Interval or Event)
                                                    // 0;       = System State Enumeration (0 = OEM_OFF, 1 = SP_OFF, 2=SP_MODE, 3=ENG_STOP, 4=ENG_START)
                                                    // 1280;    = OEM Batt Voltage
                                                    // 261;     = Temperature in C (= 26.1C)
                                                    // 0;       = # of SP stops since reboot (unsigned 16-bit integer, rolls over at 65,535)
                                                    // 0;       = # of SP starts since reboot (unsigned 16-bit integer, rolls over at 65,535)
                                                    // 1;       = Ignition on signal (1 == on)
                                                    // 0        = Hood Open ( 1 == open)
                                                    // 1;       = Park/Neutral (1 == in park)
                                                    // 0;       = Engine Running (1 == running)
                                                    // 900;     = Rotary1 low battery charge timer setting (seconds)
                                                    // 355;     = Rotary2 max temp setting in C (=35.5), 0 setting = 48F/9C (each position +=4 degrees F)
                                                    // 199;     = Rotary3 min temp setting in C (=19.9), 0 setting = 0C/32F (each position +=4 degrees F)
                                                    // 1130;    = Rotary4 OEM battery cutoff Voltage setting (=11.3V)
                                                    // e3       = Checksum for the data string
                                                    obj.event_type = parser.extractString();
                                                    obj.system_state = StealthPowerSystemStateForMTA.parse(parser.extractByte());
                                                    obj.oem_voltage = limitRange(parser.extractInt() / 100.0f, 0, 24);
                                                    obj.temperature = limitRange(parser.extractInt() / 10.0f, -40, 80);
                                                    obj.engine_stop_counter = parser.extractInt();
                                                    obj.engine_start_counter = parser.extractInt();
                                                    obj.key_inserted = parser.extractBool();
                                                    obj.hood_closed_signal = !parser.extractBool(); // It's actually hood_open = 1.
                                                    obj.park_signal = parser.extractBool();
                                                    obj.engine_running = parser.extractBool();
                                                    obj.max_discharge_time = parser.extractInt();
                                                    obj.max_temperature = parser.extractInt() / 10.0f;
                                                    obj.min_temperature = parser.extractInt() / 10.0f;
                                                    obj.cutoff_voltage = parser.extractInt() / 100.0f;
                                                    break;
                                            }

                                            receivedMessage(obj);
                                            return true;
                                        }

                                        case preamble_AMR:
                                        {
                                            var obj = new StealthPower_AMR();
                                            obj.firmware_version = parser.extractString();

                                            switch (obj.firmware_version)
                                            {
                                                case "1.0.1": // LTE version
                                                case "1.0.2": // LTE version
                                                case "1.0.3": // LTE version
                                                    // "I;"    = message type (Interval or Event)
                                                    // "0;"    = System State Enumeration
                                                    // "1311;" = SP Batt Voltage
                                                    // "1180;" = OEM Batt Voltage
                                                    // "21;"   = Temperature in C (default value)
                                                    // "12;"   = number of system stops (0 on reboot)
                                                    // "10;    = number of system starts (0 on reboot)
                                                    // "1;"    = Ignition on signal (1 == on)
                                                    // "0;"    = Hood switch signal (1 == open)
                                                    // "1;"    = Park/neutral (1 == in park)
                                                    // "1;"    = Engine running (1== running)
                                                    // "0;"    = Emergency light signal (1 == on)
                                                    // "900;"  = Rotary1 low battery charge time setting (seconds)
                                                    // "355;"  = Rotary2 max temp setting in C (=35.5)
                                                    // "199;"  = Rotary3 min temp setting in C (=19.9)
                                                    // "1130;" = Rotary4 OEM battery cutoff Voltage setting (=11.3V)
                                                    obj.event_type = parser.extractString();
                                                    obj.system_state = StealthPowerSystemStateForAMR.parse(parser.extractByte());
                                                    obj.supply_voltage = limitRange(parser.extractInt() / 100.0f, 0, 24);
                                                    obj.oem_voltage = limitRange(parser.extractInt() / 100.0f, 0, 24);
                                                    obj.temperature = limitRange(parser.extractInt() / 10.0f, -40, 80);
                                                    obj.engine_stop_counter = parser.extractInt();
                                                    obj.engine_start_counter = parser.extractInt();
                                                    obj.key_inserted = parser.extractBool();
                                                    obj.hood_closed_signal = !parser.extractBool(); // It's actually hood_open = 1.
                                                    obj.park_signal = parser.extractBool();
                                                    obj.engine_running = parser.extractBool();
                                                    obj.emergency_light = parser.extractBool();
                                                    obj.max_discharge_time = parser.extractInt();
                                                    obj.max_temperature = parser.extractInt() / 10.0f;
                                                    obj.min_temperature = parser.extractInt() / 10.0f;
                                                    obj.cutoff_voltage = parser.extractInt() / 100.0f;
                                                    break;

                                                case "2.0.1": // SP3X version
                                                case "2.0.2": // SP3X version
                                                case "2.0.3": // SP3X version
                                                    // "I;"    = message type (Interval or Event)
                                                    // "0;"    = System State Enumeration
                                                    // "1311;" = SP Batt Voltage
                                                    // "1180;" = OEM Batt Voltage
                                                    // "21;"   = Temperature in C (default value)
                                                    // "12;"   = number of system stops (0 on reboot)
                                                    // "10;    = number of system starts (0 on reboot)
                                                    // "1;"    = Ignition on signal (1 == on)
                                                    // "0;"    = Hood switch signal (1 == open)
                                                    // "1;"    = Park/neutral (1 == in park)
                                                    // "1;"    = Engine running (1== running)
                                                    // "0;"    = Emergency light signal (1 == on)
                                                    // "900;"  = Rotary1 low battery charge time setting (seconds)
                                                    // "355;"  = Rotary2 max temp setting in C (=35.5)
                                                    // "199;"  = Rotary3 min temp setting in C (=19.9)
                                                    // "1130;" = Rotary4 OEM battery cutoff Voltage setting (=11.3V)
                                                    obj.event_type = parser.extractString();
                                                    obj.system_state = StealthPowerSystemStateForAMR.parse(parser.extractByte());
                                                    obj.supply_voltage = limitRange(parser.extractInt() / 100.0f, 0, 24);
                                                    obj.oem_voltage = limitRange(parser.extractInt() / 100.0f, 0, 24);
                                                    obj.temperature = limitRange(parser.extractInt() / 10.0f, -40, 80);
                                                    obj.temperature = Float.NaN; // Temperature sensor not actually installed.
                                                    obj.engine_stop_counter = parser.extractInt();
                                                    obj.engine_start_counter = parser.extractInt();
                                                    obj.key_inserted = parser.extractBool();
                                                    obj.hood_closed_signal = !parser.extractBool(); // It's actually hood_open = 1.
                                                    obj.park_signal = parser.extractBool();
                                                    obj.engine_running = parser.extractBool();
                                                    obj.emergency_light = parser.extractBool();
                                                    obj.max_discharge_time = parser.extractInt();
                                                    obj.max_temperature = parser.extractInt() / 10.0f;
                                                    obj.min_temperature = parser.extractInt() / 10.0f;
                                                    obj.cutoff_voltage = parser.extractInt() / 100.0f;
                                                    break;
                                            }

                                            receivedMessage(obj);
                                            return true;
                                        }

                                        case preamble_PEP:
                                        {
                                            var obj = new StealthPower_PEP();
                                            obj.firmware_version = parser.extractString();

                                            switch (obj.firmware_version)
                                            {
                                                case "1.0.1":
                                                case "1.0.2":
                                                    // I;     (interval message, E is an event)
                                                    // 1;      (this is the system state)
                                                    // 1288;  (Vsp – Stealth Battery voltage)
                                                    // 1270; (Voem – OEM battery voltage)
                                                    // 84;     (V3 = Shoreline voltage)
                                                    // -15;   ( current sensor reading)
                                                    // 244;  (temp sensor reading)
                                                    // 5;       (stop count since last reboot)
                                                    // 5;       (start count since last reboot)
                                                    // 0;       (key just inserted, so we don’t go into SP)
                                                    // 1;       (ignition on signal)
                                                    // 1;       (hood closed signal)
                                                    // 1;       (park/neutral signal)
                                                    // 1;       (engine run signal)
                                                    // 127;   (countdown timer to next state)
                                                    // b3;     (checksum, to make sure the data is not corrupted)
                                                    obj.event_type = parser.extractString();
                                                    obj.system_state = StealthPowerSystemStateForPEP.parse(parser.extractByte());
                                                    obj.supply_voltage = limitRange(parser.extractInt() / 100.0f, 0, 24);
                                                    obj.oem_voltage = limitRange(parser.extractInt() / 100.0f, 0, 24);
                                                    obj.shoreline_detection_voltage = limitRange(parser.extractInt() / 100.0f, 0, 24);
                                                    obj.battery_discharge_current = limitRange(parser.extractInt() / 10.0f, -1000, 1000);
                                                    obj.temperature = limitRange(parser.extractInt() / 10.0f, -40, 80);
                                                    obj.engine_stop_counter = parser.extractInt();
                                                    obj.engine_start_counter = parser.extractInt();
                                                    obj.key_inserted = parser.extractBool();
                                                    obj.ignition_signal = parser.extractBool();
                                                    obj.hood_closed_signal = parser.extractBool();
                                                    obj.park_signal = parser.extractBool();
                                                    obj.engine_running = parser.extractBool();
                                                    break;
                                            }

                                            receivedMessage(obj);
                                            return true;
                                        }

                                        case preamble_PSEG:
                                        {
                                            var obj = new StealthPower_PSEG();
                                            obj.firmware_version = parser.extractString();

                                            switch (obj.firmware_version)
                                            {
                                                case "1.0.1":
                                                case "1.0.2":
                                                    // I; (interval message type)
                                                    // 1; (system state enum value)
                                                    // 1551; (SP battery voltage)
                                                    // 1327; (ignition/OEM battery voltage)
                                                    // 1;  (shoreline Voltage)
                                                    // -290; (amps/current –290 = 29.0 amps going into the SP batteries, positive is amps going out)
                                                    // 181; (temperature in C = 18.1C)
                                                    // 0; (stop attempted count since last power on)
                                                    // 0; (start attempted count since last power on)
                                                    // 0; (key inserted but the driver has not started the vehicle, so the system is off)
                                                    // 1; (Ignition is on)
                                                    // 0; (vehicle is in park)
                                                    // 1; (engine is running)
                                                    // 60;(count down timer to next stealth state change)
                                                    // 1; (charge enable signal from BMS)
                                                    // 1; (discharge enable signal from BMS)
                                                    // 91 (string checksum)
                                                    obj.event_type = parser.extractString();
                                                    obj.system_state = StealthPowerSystemStateForPSEG.parse(parser.extractByte());
                                                    obj.supply_voltage = limitRange(parser.extractInt() / 100.0f, 0, 24);
                                                    obj.oem_voltage = limitRange(parser.extractInt() / 100.0f, 0, 24);
                                                    obj.shoreline_detection_voltage = limitRange(parser.extractInt() / 100.0f, 0, 24);
                                                    obj.battery_discharge_current = limitRange(parser.extractInt() / 10.0f, -1000, 1000);
                                                    obj.temperature = limitRange(parser.extractInt() / 10.0f, -40, 80);
                                                    obj.engine_stop_counter = parser.extractInt();
                                                    obj.engine_start_counter = parser.extractInt();
                                                    obj.key_inserted = parser.extractBool();
                                                    obj.ignition_signal = parser.extractBool();
                                                    obj.park_signal = parser.extractBool();
                                                    obj.engine_running = parser.extractBool();
                                                    parser.skip(); // Count down timer
                                                    obj.charge_enable = parser.extractBool();
                                                    obj.discharge_enable = parser.extractBool();
                                                    break;
                                            }

                                            receivedMessage(obj);
                                            return true;
                                        }

                                        case preamble_CAPMETRO:
                                        {
                                            var obj = new StealthPower_CAPMETRO();
                                            obj.firmware_version = parser.extractString();

                                            switch (obj.firmware_version)
                                            {
                                                case "1.0.1":
                                                case "1.0.2":
                                                    // 1.0.2; = FW version
                                                    // I; Interval Message
                                                    // 5; System State (Key Inserted)
                                                    // 1313; SP Voltage (13.13V)
                                                    // -31; Current (3.1A going into SP battery, but the readings are unreliable with little current)
                                                    // 196; Temperature in C (19.6C)
                                                    // 1; Stops
                                                    // 1; Starts
                                                    // 1; Key Inserted
                                                    // 0; Ignition On
                                                    // 1; Hood Closed
                                                    // 1; Park Neutral
                                                    // 0; Engine Run
                                                    // 0; Ramp Door Open
                                                    // 1; AC Request
                                                    // 60; Countdown Timer
                                                    // 24 Checksum
                                                    obj.event_type = parser.extractString();
                                                    obj.system_state = StealthPowerSystemStateForCAPMETRO.parse(parser.extractByte());
                                                    obj.supply_voltage = limitRange(parser.extractInt() / 100.0f, 0, 24);
                                                    obj.battery_discharge_current = limitRange(parser.extractInt() / 10.0f, -1000, 1000);
                                                    obj.temperature = limitRange(parser.extractInt() / 10.0f, -40, 80);
                                                    obj.engine_stop_counter = parser.extractInt();
                                                    obj.engine_start_counter = parser.extractInt();
                                                    obj.key_inserted = parser.extractBool();
                                                    obj.ignition_signal = parser.extractBool();
                                                    obj.hood_closed_signal = parser.extractBool();
                                                    obj.park_signal = parser.extractBool();
                                                    obj.engine_running = parser.extractBool();
                                                    obj.ramp_door_open = parser.extractBool();
                                                    obj.ac_request = parser.extractBool();
                                                    break;
                                            }

                                            receivedMessage(obj);
                                            return true;
                                        }
                                    }
                                }
                                else
                                {
                                    LoggerInstance.debugVerbose("Bad checksum, expected = %02x, actual = %02x", checksumExpected & 0xFF, (~checksumActual) & 0xFF);
                                }
                            }
                        }

                        resetDecoding();
                        break;

                    default:
                        m_incomingBuffer.emit1Byte(c);
                        break;
                }
                break;
        }

        return false;
    }

    private float limitRange(float v,
                             float min,
                             float max)
    {
        if (v < min)
        {
            return Float.NaN;
        }

        if (v > max)
        {
            return Float.NaN;
        }

        return v;
    }

    //--//

    private void advanceDownload(int chunkSize)
    {
        LoggerInstance.debug("advanceDownload at %d, got %d...", m_payloadCursor, chunkSize);

        int cursorEnd = Math.min(m_payload.length, m_payloadCursor + chunkSize);
        if (cursorEnd > m_payloadCursor)
        {
            SerialAccess transport = m_serialTransport;
            if (transport != null)
            {
                final byte[] buffer = Arrays.copyOfRange(m_payload, m_payloadCursor, cursorEnd);

                if (LoggerInstance.isEnabled(Severity.DebugObnoxious))
                {
                    BufferUtils.convertToHex(buffer, 0, buffer.length, 32, true, (line) -> LoggerInstance.debugObnoxious("TX: %s", line));
                }

                transport.write(buffer, buffer.length);
                m_payloadCursor = cursorEnd;
            }
        }
    }

    protected abstract void notifyTransport(String port,
                                            boolean opened,
                                            boolean closed);

    protected abstract byte[] detectedBootloader(byte bootloadVersion,
                                                 byte hardwareVersion,
                                                 byte hardwareRevision);

    protected abstract void reportDownloadResult(int statusCode);

    protected abstract void receivedMessage(BaseStealthPowerModel obj);
}