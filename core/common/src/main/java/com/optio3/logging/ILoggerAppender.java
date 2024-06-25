/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.logging;

import java.time.ZonedDateTime;

public interface ILoggerAppender
{
    boolean append(ILogger context,
                   ZonedDateTime timestamp,
                   Severity level,
                   String thread,
                   String selector,
                   String msg) throws
                               Exception;

    public class ColumnFormat
    {
        private final int m_limitWidth;

        private int m_maxWidth = 0;

        public ColumnFormat(int limitWidth)
        {
            m_limitWidth = limitWidth;
        }

        public ColumnFormat(int limitWidth,
                            int maxWidth)
        {
            m_limitWidth = limitWidth;
            m_maxWidth   = maxWidth;
        }

        public String format(Object input)
        {
            String str = input != null ? input.toString() : "";
            int    len = Math.min(m_limitWidth, str.length());
            m_maxWidth = Math.max(m_maxWidth, len);

            StringBuilder sb = new StringBuilder(m_maxWidth);
            sb.append(str);
            while (sb.length() < m_maxWidth)
            {
                sb.append(' ');
            }

            return sb.toString();
        }
    }
}
