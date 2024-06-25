/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.visualization;

import com.optio3.util.CollectionUtils;

public class ColorOldConfiguration
{
    public ColorMode     mode;
    public ColorGradient gradient;
    public ColorList     single;
    public ColorList     palette;

    public ColorConfiguration upgrade()
    {
        if (mode == ColorMode.SINGLE && single != null)
        {
            ColorSegment seg1 = new ColorSegment();
            seg1.color = CollectionUtils.firstElement(single.selection);

            ColorConfiguration cfg = new ColorConfiguration();
            cfg.segments.add(seg1);
            return cfg;
        }

        if (mode == ColorMode.PALETTE && palette != null)
        {
            ColorSegment seg1 = new ColorSegment();
            seg1.color = CollectionUtils.firstElement(palette.selection);

            ColorConfiguration cfg = new ColorConfiguration();
            cfg.segments.add(seg1);
            return cfg;
        }

        if (mode == ColorMode.GRADIENT && gradient != null)
        {
            ColorSegment seg1 = new ColorSegment();
            seg1.color = gradient.startColor;
            ColorSegment seg2 = new ColorSegment();
            seg2.color = gradient.endColor;

            if (gradient.customRange)
            {
                seg1.stopPoint      = ColorStopPoint.CUSTOM;
                seg1.stopPointValue = gradient.startValue;

                seg2.stopPoint      = ColorStopPoint.CUSTOM;
                seg2.stopPointValue = gradient.endValue;
            }
            else
            {
                seg1.stopPoint = ColorStopPoint.MIN;
                seg2.stopPoint = ColorStopPoint.MAX;
            }

            ColorConfiguration cfg = new ColorConfiguration();
            cfg.segments.add(seg1);
            cfg.segments.add(seg2);
            return cfg;
        }

        return null;
    }
}
