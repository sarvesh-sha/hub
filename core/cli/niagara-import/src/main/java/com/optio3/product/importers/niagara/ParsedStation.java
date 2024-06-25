/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.optio3.product.importers.niagara.baja.sys.BStation;
import com.optio3.product.importers.niagara.baja.sys.BValue;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;

public class ParsedStation
{
    public final String        name;
    public final Path          file;
    public final LocalDateTime timestamp;

    private final byte[]   m_bytes;
    private       BStation m_root;

    public ParsedStation(String name,
                         Path file,
                         LocalDateTime timestamp,
                         InputStream stream) throws
                                             Exception
    {
        ByteArrayOutputStream mem = new ByteArrayOutputStream();
        IOUtils.copyLarge(stream, mem);

        String text = new String(mem.toByteArray(), StandardCharsets.UTF_8);
        text = text.replace("&#x0;", "-");
        m_bytes = text.getBytes();

        this.name = name;
        this.file = file;
        this.timestamp = timestamp;
    }

    public BStation getRoot() throws
                              Exception
    {
        if (m_root == null)
        {
            try
            {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder        dBuilder  = dbFactory.newDocumentBuilder();
                Document               parser    = dBuilder.parse(new ByteArrayInputStream(m_bytes));

                Parser mr   = new Parser();
                BValue root = mr.load(null, parser.getDocumentElement());

                m_root = root.findByType(BStation.class);
                if (m_root != null)
                {
                    m_root.parent = null;
                }
            }
            catch (Exception e)
            {
                System.out.printf("Failed to parse file %s:%s%n", file, name);
                throw e;
            }
        }

        return m_root;
    }
}
