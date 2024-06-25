/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.formatting;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class TabularReport<T>
{
    public static class ColumnDescriptor<T>
    {
        public final BiFunction<T, ColumnDescriptor<T>, Object> accessor;
        public final int                                        order;
        public final String                                     title;
        public final String                                     format;
        public       boolean                                    firstColumn;
        public       boolean                                    lastColumn;
        public       DateTimeFormatter                          dateTimeFormatter;

        public ColumnDescriptor(Field field,
                                TabularField desc)
        {
            Reflection.FieldAccessor accessor = new Reflection.FieldAccessor(field);

            this.accessor = (ctx, colDesc) -> accessor.get(ctx);
            this.order    = desc.order();
            this.title    = desc.title();
            this.format   = StringUtils.isNotBlank(desc.format()) ? desc.format() : null;

            Class<?> clz = accessor.getNativeType();

            if (clz == ZonedDateTime.class || clz == OffsetDateTime.class || clz == LocalDateTime.class)
            {
                if (StringUtils.isNotBlank(desc.format()))
                {
                    dateTimeFormatter = DateTimeFormatter.ofPattern(desc.format());
                }
                else
                {
                    dateTimeFormatter = TimeUtils.DEFAULT_FORMATTER_NO_MILLI;
                }
            }
            else if (clz == LocalDate.class)
            {
                if (StringUtils.isNotBlank(desc.format()))
                {
                    dateTimeFormatter = DateTimeFormatter.ofPattern(desc.format());
                }
                else
                {
                    dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                }
            }
            else if (clz == OffsetTime.class || clz == LocalTime.class)
            {
                if (StringUtils.isNotBlank(desc.format()))
                {
                    dateTimeFormatter = DateTimeFormatter.ofPattern(desc.format());
                }
                else
                {
                    dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                }
            }
        }

        public ColumnDescriptor(BiFunction<T, ColumnDescriptor<T>, Object> accessor,
                                int order,
                                String title,
                                String format)
        {
            this.accessor = accessor;
            this.order    = order;
            this.title    = title;
            this.format   = format;
        }
    }

    public abstract class RowHandler
    {
        public abstract void emitRow(T row);
    }

    protected final List<ColumnDescriptor<T>> m_columnDescriptors = Lists.newArrayList();

    public TabularReport(Class<T> clz)
    {
        this(extractDescriptors(clz));
    }

    public TabularReport(List<ColumnDescriptor<T>> columnDescriptors)
    {
        m_columnDescriptors.addAll(columnDescriptors);
        m_columnDescriptors.sort(Comparator.comparing((a) -> a.order));

        ColumnDescriptor<T> firstDesc = CollectionUtils.firstElement(m_columnDescriptors);
        if (firstDesc != null)
        {
            firstDesc.firstColumn = true;
        }

        ColumnDescriptor<T> lastDesc = CollectionUtils.lastElement(m_columnDescriptors);
        if (lastDesc != null)
        {
            lastDesc.lastColumn = true;
        }
    }

    private static <T> List<ColumnDescriptor<T>> extractDescriptors(Class<T> clz)
    {
        List<ColumnDescriptor<T>> columnDescriptors = Lists.newArrayList();
        for (Field f : Reflection.collectFields(clz)
                                 .values())
        {
            TabularField t = f.getAnnotation(TabularField.class);
            if (t != null)
            {
                columnDescriptors.add(new ColumnDescriptor<T>(f, t));
            }
        }

        return columnDescriptors;
    }

    //--//

    public abstract void emit(Consumer<RowHandler> rowEmitterCallback);

    protected static String escapeCharacters(String val)
    {
        val = StringUtils.replace(val, "\t", "\\t");
        val = StringUtils.replace(val, "\r", "\\r");
        val = StringUtils.replace(val, "\n", "\\n");
        return val;
    }
}
