/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.provision.imaging;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import javax.imageio.ImageIO;

import com.google.common.collect.Lists;
import com.optio3.infra.WellKnownSites;

public class LegacyLabelerHelper
{
    private final String m_hostId;
    private final String m_imsi;
    private final String m_imei;
    private final String m_iccid;

    public LegacyLabelerHelper(String hostId,
                               String imsi,
                               String imei,
                               String iccid)
    {
        m_hostId = hostId;
        m_imsi = imsi;
        m_imei = imei;
        m_iccid = iccid;
    }

    public void printQL800(String driverFile) throws
                                              Exception
    {
        final boolean twoColors = false;
        final boolean highDpi   = true;

        BrotherQlPrinterDriver driver = new BrotherQlPrinterDriver(driverFile, 90 * 8, 500, twoColors, highDpi);
        generateLabel(driver);
        driver.print();
    }

    public void printImage(String imageFile) throws
                                             Exception
    {
        AbstractPrinterDriver driver = new AbstractPrinterDriver(90 * 8, 500);
        generateLabel(driver);

        ImageIO.write(driver.getImage(), "JPEG", new File(imageFile));
    }

    public byte[] getImage() throws
                             Exception
    {
        AbstractPrinterDriver driver = new AbstractPrinterDriver(90 * 8, 500);
        generateLabel(driver);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(driver.getImage(), "JPEG", outputStream);

        return outputStream.toByteArray();
    }

    //--//

    private void generateLabel(AbstractPrinterDriver driver) throws
                                                             UnsupportedEncodingException
    {
        Graphics2D graphics2D = driver.getGraphics();

        graphics2D.setColor(Color.BLACK);

        Font font = new Font("Dialog", Font.PLAIN, 40);
        graphics2D.setFont(font);

        List<List<String>> table = Lists.newArrayList();

        addRow(table, "S/N", m_hostId);

        if (m_imsi != null)
        {
            addRow(table, "IMSI", m_imsi);
        }

        if (m_imei != null)
        {
            addRow(table, "IMEI", m_imei);
        }

        if (m_iccid != null)
        {
            addRow(table, "ICCID", m_iccid);
        }

        Point tableCorner = renderTable(graphics2D, 10, 10, 10, 10, 4, table);

        Font fontSmall = new Font("Dialog", Font.PLAIN, 18);
        graphics2D.setFont(fontSmall);

        int textHeight = fontSmall.getSize();
        int width      = driver.getWidth();
        int height     = driver.getHeight();
        int qrTop      = tableCorner.y + 10;
        int qrSize     = height - qrTop;

        BarCodeHelper.insertQRCode(graphics2D, "https://" + WellKnownSites.builderServer() + "/#/provision?hostId=" + URLEncoder.encode(m_hostId, "UTF-8"), 0, qrTop, qrSize, qrSize, 0);
        drawString(graphics2D, qrSize + 5, qrTop + 40, "<-- Scan For Provisioning");

//        graphics2D.setColor(Color.RED);
//        BarCodeHelper.insertQRCode(graphics2D, "http://waypoint.localnet", width - qrSize, qrTop, qrSize, qrSize, 0);
        BarCodeHelper.insertQRCode(graphics2D, "http://192.170.4.1", width - qrSize, qrTop, qrSize, qrSize, 0);
        drawStringRightJustified(graphics2D, width - qrSize, height - 40, "Scan For Diagnostics -->");

        drawString(graphics2D, qrSize + 25, qrTop + 100, "Connect to Wi-Fi:");
        drawString(graphics2D, qrSize + 25, qrTop + 130, "Optio3 %s", m_hostId);
    }

    private void addRow(List<List<String>> table,
                        String... cells)
    {
        List<String> row = Lists.newArrayList(cells);
        table.add(row);
    }

    private Point renderTable(Graphics2D graphics2D,
                              int x,
                              int y,
                              int xMargin,
                              int yMargin,
                              int borderSize,
                              List<List<String>> table)
    {
        final Font font = graphics2D.getFont();

        List<Integer> columns = Lists.newArrayList();
        for (List<String> row : table)
        {
            for (int i = 0; i < row.size(); i++)
            {
                final String cell = row.get(i);

                if (i >= columns.size())
                {
                    columns.add(0);
                }

                Integer column = columns.get(i);
                columns.set(i, Math.max(column, measureString(font, cell)));
            }
        }

        int[] offsetX = new int[columns.size() + 1];
        int[] offsetY = new int[table.size() + 1];

        for (int colIdx = 0; colIdx < columns.size(); colIdx++)
        {
            offsetX[colIdx] = x;

            x += 2 * xMargin + borderSize + columns.get(colIdx);

            offsetX[colIdx + 1] = x;
        }

        for (int rowIdx = 0; rowIdx < table.size(); rowIdx++)
        {
            offsetY[rowIdx] = y;

            y += 2 * yMargin + borderSize + font.getSize();

            offsetY[rowIdx + 1] = y;
        }

        for (int rowIdx = 0; rowIdx < table.size(); rowIdx++)
        {
            List<String> row = table.get(rowIdx);

            for (int colIdx = 0; colIdx < columns.size(); colIdx++)
            {
                final String cell = row.get(colIdx);

                drawString(graphics2D, offsetX[colIdx] + xMargin, offsetY[rowIdx + 1] - yMargin - borderSize, cell);

                Stroke stroke = graphics2D.getStroke();
                graphics2D.setStroke(new BasicStroke(borderSize));
                graphics2D.drawRect(offsetX[colIdx], offsetY[rowIdx], offsetX[colIdx + 1] - offsetX[colIdx], offsetY[rowIdx + 1] - offsetY[rowIdx]);
                graphics2D.setStroke(stroke);
            }
        }

        return new Point(offsetX[offsetX.length - 1], offsetY[offsetY.length - 1]);
    }

    private static int measureString(Font font,
                                     String text)
    {
        Rectangle2D bounds = font.getStringBounds(text, new FontRenderContext(null, false, false));
        return (int) Math.ceil(bounds.getWidth());
    }

    private static void drawStringRightJustified(Graphics2D graphics2D,
                                                 int x,
                                                 int y,
                                                 String fmt,
                                                 Object... args)
    {
        String text = String.format(fmt, args);
        graphics2D.drawString(text, x - measureString(graphics2D.getFont(), text), y);
    }

    private static void drawString(Graphics2D graphics2D,
                                   int x,
                                   int y,
                                   String fmt,
                                   Object... args)
    {
        graphics2D.drawString(String.format(fmt, args), x, y);
    }
}
