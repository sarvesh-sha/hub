/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.formatting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.google.common.collect.Maps;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.BiConsumerWithException;
import com.optio3.util.function.ConsumerWithException;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class TabularReportAsExcel<T> extends TabularReport<T>
{
    public static class Holder implements Closeable
    {
        final XSSFWorkbook  workbook;
        final SXSSFWorkbook workbookWriter;

        public Holder()
        {
            workbook       = new XSSFWorkbook();
            workbookWriter = new SXSSFWorkbook(workbook, 500, true);
        }

        @Override
        public void close()
        {
            workbookWriter.dispose();
        }

        public SXSSFSheet createSheet(String sheetName)
        {
            sheetName = WorkbookUtil.createSafeSheetName(BoxingUtils.get(sheetName, "Data"));
            return workbookWriter.createSheet(sheetName);
        }

        public InputStream asStream() throws
                                      IOException
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            toStream(outputStream);

            return new ByteArrayInputStream(outputStream.toByteArray());
        }

        public void toStream(OutputStream stream) throws
                                                  IOException
        {
            workbookWriter.write(stream);
        }
    }

    private static final DateTimeFormatter c_pattern1 = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
    private static final DateTimeFormatter c_pattern2 = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public final String m_sheetName;
    public final Holder m_holder;

    public TabularReportAsExcel(Class<T> clz,
                                String sheetName,
                                Holder holder)
    {
        super(clz);

        m_sheetName = sheetName;
        m_holder    = holder;
    }

    public TabularReportAsExcel(List<ColumnDescriptor<T>> columnDescriptors,
                                String sheetName,
                                Holder holder)
    {
        super(columnDescriptors);

        m_sheetName = sheetName;
        m_holder    = holder;
    }

    @Override
    public void emit(Consumer<RowHandler> rowEmitterCallback)
    {
        SXSSFSheet sheetWriter = m_holder.createSheet(m_sheetName);

        // Unfortunately, we can't use trackAllColumnsForAutoSizing(), because it tries to render all the cells with AWT, making it super slow.
        // sheetWriter.trackAllColumnsForAutoSizing();

        StylesTable          stylesTable  = m_holder.workbook.getStylesSource();
        XSSFCellStyle[]      cellStyles   = new XSSFCellStyle[m_columnDescriptors.size()];
        Map<String, Integer> formatLookup = Maps.newHashMap();
        AtomicInteger        rowCounter   = new AtomicInteger();

        SXSSFRow headerRow = sheetWriter.createRow(rowCounter.getAndIncrement());
        for (int col = 0; col < m_columnDescriptors.size(); col++)
        {
            ColumnDescriptor<T> colDesc = m_columnDescriptors.get(col);

            SXSSFCell cell = headerRow.createCell(col);
            cell.setCellValue(colDesc.title);

            XSSFCellStyle cellStyle = m_holder.workbook.createCellStyle();
            cellStyles[col] = cellStyle;

            if (colDesc.format != null)
            {
                int dateFormat = formatLookup.computeIfAbsent(colDesc.format, stylesTable::putNumberFormat);
                cellStyle.setDataFormat(dateFormat);
            }
        }

        var rowHandler = new RowHandler()
        {
            @Override
            public void emitRow(T row)
            {
                SXSSFRow rowOutput = sheetWriter.createRow(rowCounter.getAndIncrement());

                for (int col = 0; col < m_columnDescriptors.size(); col++)
                {
                    ColumnDescriptor<T> colDesc = m_columnDescriptors.get(col);
                    Object              value   = colDesc.accessor.apply(row, colDesc);
                    if (value != null)
                    {
                        SXSSFCell cell = rowOutput.createCell(col);
                        cell.setCellStyle(cellStyles[col]);

                        ZonedDateTime value_ZonedDateTime = Reflection.as(value, ZonedDateTime.class);
                        if (value_ZonedDateTime != null)
                        {
                            cell.setCellValue(value_ZonedDateTime.toLocalDateTime());
                            continue;
                        }

                        if (value instanceof Enum)
                        {
                            value = value.toString();
                        }

                        String value_String = Reflection.as(value, String.class);
                        if (value_String != null)
                        {
                            cell.setCellValue(value_String);
                            continue;
                        }

                        Boolean value_Boolean = Reflection.as(value, Boolean.class);
                        if (value_Boolean != null)
                        {
                            cell.setCellValue(value_Boolean);
                            continue;
                        }

                        Number value_Number = Reflection.as(value, Number.class);
                        if (value_Number != null)
                        {
                            cell.setCellValue(value_Number.doubleValue());
                            continue;
                        }
                    }
                }
            }
        };

        rowEmitterCallback.accept(rowHandler);
    }

    //--//

    public static void forEachRow(XSSFWorkbook workbook,
                                  String name,
                                  ConsumerWithException<XSSFRow> callback) throws
                                                                           Exception
    {
        for (Sheet sheet : workbook)
        {
            if (StringUtils.equalsIgnoreCase(sheet.getSheetName(), name))
            {
                for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++)
                {
                    XSSFRow row = (XSSFRow) sheet.getRow(rowIndex);
                    callback.accept(row);
                }

                return;
            }
        }
    }

    public static void forEachRowWithHeader(XSSFWorkbook workbook,
                                            String name,
                                            BiConsumerWithException<Map<String, Integer>, XSSFRow> callback) throws
                                                                                                             Exception
    {
        AtomicReference<Map<String, Integer>> lookupRef = new AtomicReference<>();

        forEachRow(workbook, name, (row) ->
        {
            var lookup = lookupRef.get();
            if (lookup == null)
            {
                lookup = Maps.newHashMap();
                lookupRef.set(lookup);

                for (int colIndex = row.getFirstCellNum(); colIndex < row.getLastCellNum(); colIndex++)
                {
                    String val = getCellStringValue(row, colIndex);
                    if (val != null)
                    {
                        lookup.put(val.toLowerCase(), colIndex);
                    }
                }
            }
            else
            {
                callback.accept(lookup, row);
            }
        });
    }

    public static String getCellStringValue(Map<String, Integer> lookup,
                                            XSSFRow row,
                                            String column)
    {
        Integer k = lookup.get(column);
        return k != null ? getCellStringValue(row, k) : null;
    }

    public static String getCellStringValue(XSSFRow row,
                                            int k)
    {
        XSSFCell cell = row.getCell(k);
        if (cell == null)
        {
            return null;
        }

        switch (cell.getCellType())
        {
            case NUMERIC:
                return Double.toString(cell.getNumericCellValue());

            case STRING:
                return cell.getStringCellValue();

            default:
                return null;
        }
    }

    public static ZonedDateTime getCellTimeValue(Map<String, Integer> lookup,
                                                 XSSFRow row,
                                                 String column)
    {
        Integer k = lookup.get(column);
        return k != null ? getCellTimeValue(row, k) : null;
    }

    public static ZonedDateTime getCellTimeValue(XSSFRow row,
                                                 int k)
    {
        XSSFCell cell = row.getCell(k);
        if (cell == null)
        {
            return null;
        }

        switch (cell.getCellType())
        {
            case NUMERIC:
                final double daysBetweenJanuary1900AndJanuary1970 = 25567;

                return TimeUtils.fromTimestampToUtcTime((cell.getNumericCellValue() - daysBetweenJanuary1900AndJanuary1970) * 86400);

            case STRING:
                String text = cell.getStringCellValue();

                try
                {
                    return ZonedDateTime.parse(text, c_pattern1);
                }
                catch (Throwable t)
                {
                    // Ignore failure.
                }

                try
                {
                    return ZonedDateTime.parse(text, c_pattern2);
                }
                catch (Throwable t)
                {
                    // Ignore failure.
                }

                try
                {
                    return ZonedDateTime.parse(text);
                }
                catch (Throwable t)
                {
                    // Ignore failure.
                }

                return null;

            default:
                return null;
        }
    }

    public static double getCellNumericValue(Map<String, Integer> lookup,
                                             XSSFRow row,
                                             String column)
    {
        Integer k = lookup.get(column);
        return k != null ? getCellNumericValue(row, k) : Double.NaN;
    }

    public static double getCellNumericValue(XSSFRow row,
                                             int k)
    {
        XSSFCell cell = row.getCell(k);
        if (cell == null)
        {
            return Double.NaN;
        }

        switch (cell.getCellType())
        {
            case NUMERIC:
                return cell.getNumericCellValue();

            case STRING:
                String value = cell.getStringCellValue();
                if (StringUtils.isBlank(value))
                {
                    return Double.NaN;
                }

                return Double.parseDouble(value);

            default:
                return Double.NaN;
        }
    }
}
