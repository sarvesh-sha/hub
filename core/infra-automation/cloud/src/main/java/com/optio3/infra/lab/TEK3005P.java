/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.lab;

import java.util.Objects;

import com.optio3.concurrency.Executors;
import com.optio3.interop.mediaaccess.SerialAccess;
import com.optio3.util.BoxingUtils;

public class TEK3005P implements AutoCloseable
{
    private final SerialAccess m_transport;

    public TEK3005P(String port)
    {
        // This is weird. If we open the serial port once and change the speed, it doesn't stick. We have to close it and reopen it...
        m_transport = SerialAccess.openMultipleTimes(4, port, 9600, 8, 'N', 1);

        if (getVoltage() == null)
        {
            throw new RuntimeException("Can't connect to TEK3005P");
        }
    }

    public void close()
    {
        m_transport.close();
    }

    public void setOutput(boolean on)
    {
        sendWithDelay(on ? "OUTPUT1" : "OUTPUT0");
        sendWithDelay("\\n");
    }

    public boolean setVoltage(double v)
    {
        int vDecimal = (int) (v * 100);

        sendWithDelay("VSET1:");
        sendWithDelay("%02d.%02d", vDecimal / 100, vDecimal % 100);
        sendWithDelay("\\n");

        return Objects.equals(getVoltage(), v);
    }

    public Double getVoltage()
    {
        String result = executeWithResult("VSET1?");
        return result != null ? Double.valueOf(result) : null;
    }

    public Double getVoltageActual()
    {
        String result = executeWithResult("VOUT1?");
        return result != null ? Double.valueOf(result) : null;
    }

    public double getVoltageActual(double defaultValue)
    {
        return BoxingUtils.get(getVoltageActual(), defaultValue);
    }

    public double getPowerActual()
    {
        double vActual = getVoltageActual(0);
        double iActual = getCurrentActual(0);

        return vActual * iActual;
    }

    public boolean setCurrent(double v)
    {
        int vDecimal = (int) (v * 100);

        sendWithDelay("ISET1:");
        sendWithDelay("%02d.%02d", vDecimal / 100, vDecimal % 100);
        sendWithDelay("\\n");

        return Objects.equals(getCurrent(), v);
    }

    public Double getCurrent()
    {
        String result = executeWithResult("ISET1?");
        return result != null ? Double.valueOf(result) : null;
    }

    public Double getCurrentActual()
    {
        String result = executeWithResult("IOUT1?");
        return result != null ? Double.valueOf(result) : null;
    }

    public double getCurrentActual(double defaultValue)
    {
        return BoxingUtils.get(getCurrentActual(), defaultValue);
    }

    private String executeWithResult(String fmt,
                                     Object... args)
    {
        sendWithDelay(fmt, args);
        sendWithDelay("\\n");

        StringBuilder sb  = new StringBuilder();
        byte[]        buf = new byte[1];

        while (true)
        {
            if (m_transport.read(buf, 1000) != 1)
            {
                return null;
            }

            char c = (char) buf[0];
            if (c == '\n')
            {
                break;
            }

            sb.append(c);
        }

        return sb.toString();
    }

    private void sendWithDelay(String fmt,
                               Object... args)
    {
        String cmd = String.format(fmt, args);
        m_transport.write(cmd);
        Executors.safeSleep(100);
    }
}
