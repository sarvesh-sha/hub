/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.provision.imaging;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;

public class AbstractPrinterDriver
{
    private final BufferedImage m_image;

    static
    {
        System.setProperty("java.awt.headless", "true");
    }

    public AbstractPrinterDriver(int width,
                                 int height)
    {
        m_image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D gfx = m_image.createGraphics();
        gfx.setBackground(Color.WHITE);
        gfx.clearRect(0, 0, width, height);
    }

    public int getWidth()
    {
        return m_image.getWidth();
    }

    public int getHeight()
    {
        return m_image.getHeight();
    }

    public BufferedImage getImage()
    {
        return m_image;
    }

    public Raster getRaster()
    {
        return m_image.getData();
    }

    public Graphics2D getGraphics()
    {
        return m_image.createGraphics();
    }
}
