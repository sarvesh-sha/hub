/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.visualization;

import java.util.List;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

public class ColorConfiguration
{
    public  List<ColorSegment> segments = Lists.newArrayList();
    private String             m_paletteName;

    public void setPaletteName(String paletteName)
    {
        if (StringUtils.equals(paletteName, "Default"))
        {
            paletteName = null;
        }

        m_paletteName = paletteName;
    }

    public String getPaletteName()
    {
        return m_paletteName;
    }
}
