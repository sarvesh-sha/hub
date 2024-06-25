package com.optio3.cloud.hub.engine.metrics.value;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class AzimuthZenithAngle
{
    public final double azimuth;
    public final double zenithAngle;

    public AzimuthZenithAngle(final ZonedDateTime date,
                              final double latitude,
                              final double longitude)
    {
        ZonedDateTime dateUtc = date.withZoneSameInstant(ZoneOffset.UTC);

        final double pi = Math.PI;
        int          m  = dateUtc.getMonthValue() + 1;
        int          y  = dateUtc.getYear();
        final int    d  = dateUtc.getDayOfMonth();
        final double h  = dateUtc.getHour() + dateUtc.getMinute() / 60d + dateUtc.getSecond() / (60d * 60);

        if (m <= 2)
        {
            m += 12;
            y -= 1;
        }

        final double tE = (int) (365.25 * (y - 2000)) + (int) (30.6001 * (m + 1)) - (int) (0.01 * y) + d + 0.0416667 * h - 21958;

        final double omegaAtE = 0.0172019715 * tE;

        final double lambda = -1.388803 + 1.720279216e-2 * tE + 3.3366e-2 * Math.sin(omegaAtE - 0.06172) + 3.53e-4 * Math.sin(2.0 * omegaAtE - 0.1163);

        final double epsilon = 4.089567e-1 - 6.19e-9 * tE;

        final double sLambda  = Math.sin(lambda);
        final double cLambda  = Math.cos(lambda);
        final double sEpsilon = Math.sin(epsilon);
        final double cEpsilon = Math.sqrt(1 - sEpsilon * sEpsilon);

        double alpha = Math.atan2(sLambda * cEpsilon, cLambda);
        if (alpha < 0)
        {
            alpha += 2 * pi;
        }

        final double delta = Math.asin(sLambda * sEpsilon);

        double H = 1.7528311 + 6.300388099 * tE + Math.toRadians(longitude) - alpha;
        H = ((H + pi) % (2 * pi)) - pi;
        if (H < -pi)
        {
            H += 2 * pi;
        }

        // end of "short procedure"
        final double sPhi   = Math.sin(Math.toRadians(latitude));
        final double cPhi   = Math.sqrt((1 - sPhi * sPhi));
        final double sDelta = Math.sin(delta);
        final double cDelta = Math.sqrt(1 - sDelta * sDelta);
        final double sH     = Math.sin(H);
        final double cH     = Math.cos(H);

        final double sEpsilon0 = sPhi * sDelta + cPhi * cDelta * cH;
        final double eP        = Math.asin(sEpsilon0) - 4.26e-5 * Math.sqrt(1.0 - sEpsilon0 * sEpsilon0);
        final double gamma     = Math.atan2(sH, cH * sPhi - sDelta * cPhi / cDelta);

        final double z = pi / 2 - eP;

        this.azimuth     = Math.toDegrees(gamma + pi) % 360.0;
        this.zenithAngle = Math.toDegrees(z);
    }
}
