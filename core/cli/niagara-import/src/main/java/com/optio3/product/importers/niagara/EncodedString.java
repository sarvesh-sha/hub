/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara;

public class EncodedString
{
    public final String rawValue;

    private String m_decodedValue;

    public EncodedString(String rawValue)
    {
        this.rawValue = rawValue;
    }

    public String getDecodedValue()
    {
        if (m_decodedValue == null)
        {
            StringBuilder sb  = new StringBuilder();
            final int     end = rawValue.length();
            int           pos = 0;

            while (pos < end)
            {
                int pos2 = rawValue.indexOf('$', pos);
                if (pos2 < 0)
                {
                    sb.append(rawValue.substring(pos));
                    break;
                }

                sb.append(rawValue.substring(pos, pos2));

                if (pos2 + 3 <= end)
                {
                    char c = (char) Integer.parseInt(rawValue.substring(pos2 + 1, pos2 + 3), 16);
                    sb.append(c);

                    pos2 += 3;
                }
                else
                {
                    sb.append('$');
                    pos2 += 1;
                }

                pos = pos2;
            }

            m_decodedValue = sb.toString();
        }

        return m_decodedValue;
    }

    @Override
    public String toString()
    {
        return getDecodedValue();
    }
}
