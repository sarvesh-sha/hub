/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.formatting;

import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.Lists;

public class TabularReportAsCSV<T> extends TabularReport<T>
{
    public List<String> lines = Lists.newArrayList();

    public TabularReportAsCSV(Class<T> clz)
    {
        super(clz);
    }

    public TabularReportAsCSV(List<ColumnDescriptor<T>> columnDescriptors)
    {
        super(columnDescriptors);
    }

    @Override
    public void emit(Consumer<RowHandler> rowEmitterCallback)
    {
        StringBuilder sb = new StringBuilder();

        lines.add("sep=\t");

        for (ColumnDescriptor<T> colDesc : m_columnDescriptors)
        {
            if (!colDesc.firstColumn)
            {
                sb.append('\t');
            }

            sb.append(escapeCharacters(colDesc.title));
        }

        addLine(sb);

        var rowHandler = new RowHandler()
        {
            @Override
            public void emitRow(T row)
            {
                for (ColumnDescriptor<T> colDesc : m_columnDescriptors)
                {
                    if (!colDesc.firstColumn)
                    {
                        sb.append('\t');
                    }

                    Object value = colDesc.accessor.apply(row, colDesc);
                    if (value != null)
                    {
                        String text;

                        if (colDesc.dateTimeFormatter != null)
                        {
                            TemporalAccessor t = (TemporalAccessor) value;
                            text = colDesc.dateTimeFormatter.format(t);
                        }
                        else if (colDesc.format != null)
                        {
                            text = String.format(colDesc.format, value);
                        }
                        else
                        {
                            text = value.toString();
                        }

                        sb.append(escapeCharacters(text));
                    }
                }

                addLine(sb);
            }
        };

        rowEmitterCallback.accept(rowHandler);
    }

    private void addLine(StringBuilder sb)
    {
        lines.add(sb.toString());
        sb.setLength(0);
    }
}
