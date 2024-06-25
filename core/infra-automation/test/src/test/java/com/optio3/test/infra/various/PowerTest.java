/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.infra.various;

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.optio3.concurrency.Executors;
import com.optio3.infra.lab.TEK3005P;
import com.optio3.test.infra.Optio3InfraTest;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;

public class PowerTest extends Optio3InfraTest
{
    @Ignore("Manually enable to test, since it requires hardware setup.")
    @Test(timeout = 2000 * 1000)
    public void testShutdownAndRestart()
    {
        try (TEK3005P powerSupply = new TEK3005P("/dev/cu.SLAB_USBtoUART"))
        {
            Stopwatch st = Stopwatch.createStarted();

            powerSupply.setVoltage(12);

            long shutdownTime = waitForShutdown(powerSupply, st);

            double v = 12;
            powerSupply.setVoltage(v);

            st.reset();
            st.start();
            while (true)
            {
                Executors.safeSleep(10000);

                long time = st.elapsed(TimeUnit.SECONDS);

                double vActual = powerSupply.getVoltageActual(0);
                double iActual = powerSupply.getCurrentActual(0);
                System.out.printf("[%3d] Waiting for restart at %fV %fA\n", st.elapsed(TimeUnit.SECONDS), vActual, iActual);

                double power = vActual * iActual;
                if (power > 0.1)
                {
                    System.out.printf("System restart at %fV in %d seconds\n", powerSupply.getVoltageActual(), time);
                    break;
                }

                if (time > 2 * shutdownTime)
                {
                    v += 0.1;
                    System.out.printf("No system restart after %d seconds, increasing voltage to %fV\n", time, v);
                    powerSupply.setVoltage(v);
                    st.reset();
                    st.start();
                }
            }
        }
    }

    @Ignore("Manually enable to test, since it requires hardware setup.")
    @Test(timeout = 2000 * 1000)
    public void testShutdownAndPingpong()
    {
        try (TEK3005P powerSupply = new TEK3005P("/dev/cu.SLAB_USBtoUART"))
        {
            Stopwatch st = Stopwatch.createStarted();

            powerSupply.setVoltage(11);

            long shutdownTime = waitForShutdown(powerSupply, st);

            powerSupply.setVoltage(14);

            // Measure how long it takes to restart => that's two ticks of the slow clock.
            long restartTime = waitForRestart(powerSupply, st, 200);

            // Drop below threshold.
            powerSupply.setVoltage(12);

            waitForShutdown(powerSupply, st);

            // Raise above threshold.
            powerSupply.setVoltage(14);

            // Wait for one tick of the slow clock.
            Executors.safeSleep((restartTime * 3 / 4) * 1000);

            // Drop below threshold.
            powerSupply.setVoltage(12);

            st.reset();
            st.start();
            for (int i = 0; i < 100; i++)
            {
                Executors.safeSleep(10000);

                long time = st.elapsed(TimeUnit.SECONDS);

                double vActual = powerSupply.getVoltageActual(0);
                double iActual = powerSupply.getCurrentActual(0);
                System.out.printf("[%3d] Waiting for ping-pong at %fV %fA\n", st.elapsed(TimeUnit.SECONDS), vActual, iActual);
            }
        }
    }

    private long waitForRestart(TEK3005P powerSupply,
                                Stopwatch st,
                                int maxWait)
    {
        st.reset();
        st.start();
        long report = 10;
        while (true)
        {
            Executors.safeSleep(500);

            double vActual = powerSupply.getVoltageActual(0);
            double iActual = powerSupply.getCurrentActual(0);

            long time = st.elapsed(TimeUnit.SECONDS);
            if (time >= report)
            {
                System.out.printf("[%3d] Waiting for restart at %fV %fA\n", time, vActual, iActual);

                report += 10;
            }

            if (time > maxWait)
            {
                return -1;
            }

            double power = vActual * iActual;
            if (power > 0.1)
            {
                break;
            }
        }

        long restartTime = st.elapsed(TimeUnit.SECONDS);
        System.out.printf("Restarted after %d seconds\n", restartTime);
        return restartTime;
    }

    private long waitForShutdown(TEK3005P powerSupply,
                                 Stopwatch st)
    {
        if (powerSupply.getPowerActual() < 0.1)
        {
            return -1;
        }

        st.reset();
        st.start();

        System.out.println("Initiating shutdown...");
        while (!initiateShutdown())
        {
            Executors.safeSleep(1000);
        }
        System.out.println("Initiated shutdown...");

        long report = 10;
        while (true)
        {
            Executors.safeSleep(500);

            double vActual = powerSupply.getVoltageActual(0);
            double iActual = powerSupply.getCurrentActual(0);

            long time = st.elapsed(TimeUnit.SECONDS);
            if (time >= report)
            {
                System.out.printf("[%3d] Waiting for shutdown at %fV %fA\n", time, vActual, iActual);

                report += 10;
            }

            double power = vActual * iActual;
            if (power < 0.1)
            {
                break;
            }
        }

        long shutdownTime = st.elapsed(TimeUnit.SECONDS);
        System.out.printf("System shutdown at %fV in %d seconds\n", powerSupply.getVoltageActual(), shutdownTime);
        return shutdownTime;
    }

    static boolean initiateShutdown()
    {
        if (!execRemoteCommand(0, "gpio write 21 0", null)) // SHDN_12v
        {
            return false;
        }

        if (!execRemoteCommand(0, "gpio write 22 0", null)) // SHDN_24v
        {
            return false;
        }

        if (!execRemoteCommand(0, "gpio mode 21 out", null)) // SHDN_12v
        {
            return false;
        }

        if (!execRemoteCommand(0, "gpio mode 22 out", null)) // SHDN_24v
        {
            return false;
        }

        if (!execRemoteCommand(0, "gpio write 21 1", null)) // SHDN_12v
        {
            return false;
        }

        if (!execRemoteCommand(0, "gpio write 21 0", null)) // SHDN_12v
        {
            return false;
        }

        return true;
    }

    public static boolean execRemoteCommand(int expectedExitCode,
                                            String cmd,
                                            List<String> lines)
    {
        try
        {
            ProcessBuilder pb = new ProcessBuilder(StringUtils.split("/usr/bin/ssh -i /Users/davidem/git/infra/identity/gateway/remoteOptio3.key pi@192.168.1.118 " + cmd, " "));

            Process p = pb.start();

            boolean exited;

            try
            {
                exited = p.waitFor(2, TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                exited = false;
            }

            if (!exited)
            {
                p.destroyForcibly();
            }

            if (lines != null)
            {
                lines.clear();
                lines.addAll(IOUtils.readLines(p.getInputStream(), Charset.defaultCharset()));
            }

            return p.exitValue() == expectedExitCode;
        }
        catch (Throwable t)
        {
            return false;//Integer.MIN_VALUE;
        }
    }
}
