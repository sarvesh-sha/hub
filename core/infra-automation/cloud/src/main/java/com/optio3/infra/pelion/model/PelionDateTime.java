/*
 * Copyright (C) 2017-2020, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.pelion.model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class PelionDateTime
{
    private final ZonedDateTime m_value;

    private PelionDateTime(ZonedDateTime value)
    {
        m_value = value;
    }

    public static PelionDateTime wrap(ZonedDateTime value)
    {
        return value != null ? new PelionDateTime(value) : null;
    }

    @Override
    public String toString()
    {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(m_value);
    }
}
