/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.provision.imaging;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.EnumMap;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class BarCodeHelper
{
    public static void insertQRCode(Graphics2D graphics2D,
                                    String text,
                                    int x,
                                    int y,
                                    int width,
                                    int height,
                                    int degree)
    {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        Map<EncodeHintType, Object> hintMap = new EnumMap<>(EncodeHintType.class);
        hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hintMap.put(EncodeHintType.MARGIN, 1); /* default = 4 */
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);

        AffineTransform transformOrig = graphics2D.getTransform();
        AffineTransform transform     = new AffineTransform(transformOrig);
        transform.translate(x, y);
        if (degree != 0)
        {
            transform.rotate(degree / 180.0 * Math.PI);
        }
        graphics2D.setTransform(transform);

        try
        {
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hintMap);

            for (int xCode = 0; xCode < bitMatrix.getWidth(); xCode++)
            {
                for (int yCode = 0; yCode < bitMatrix.getHeight(); yCode++)
                {
                    if (bitMatrix.get(xCode, yCode))
                    {
                        graphics2D.fillRect(xCode, yCode, 1, 1);
                    }
                }
            }
        }
        catch (WriterException e)
        {
            throw new RuntimeException(e);
        }

        graphics2D.setTransform(transformOrig);
    }
}
