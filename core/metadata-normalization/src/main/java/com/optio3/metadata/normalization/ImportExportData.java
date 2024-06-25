/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.metadata.normalization;

import java.io.File;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.CollectionUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = BACnetImportExportData.class) })
public abstract class ImportExportData
{
    public static final TypeReference<List<ImportExportData>> c_typeRef = new TypeReference<List<ImportExportData>>()
    {
    };

    public String sysId;

    public String       deviceName;
    public String       deviceDescription;
    public String       deviceLocation;
    public List<String> deviceStructure;
    public String       deviceVendor;
    public String       deviceModel;

    public String       dashboardName;
    public String       dashboardEquipmentName;
    public List<String> dashboardStructure;

    public String      normalizedName;
    public boolean     isSampled;
    public String      pointClassId;
    public String      pointClassAdt;
    public Set<String> pointTags;
    public String      locationName;
    public String      locationSysId;

    public EngineeringUnits units;
    public String           value;

    //--//

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    public abstract int compareTo(ImportExportData o,
                                  boolean fuzzy);

    //--//

    public static <T extends ImportExportData> void sort(List<T> results)
    {
        results.sort((a, b) -> a.compareTo(b, false));
    }

    public static <T extends ImportExportData> List<T> read(String contents,
                                                            Class<T> clz) throws
                                                                          Exception
    {
        List<ImportExportData> lst = ObjectMappers.SkipNulls.readValue(contents, c_typeRef);

        return CollectionUtils.transformToList(lst, clz::cast);
    }

    public static <T extends ImportExportData> String write(List<T> lst) throws
                                                                         Exception
    {
        ObjectWriter writer = ObjectMappers.SkipNulls.writerFor(c_typeRef);

        return writer.writeValueAsString(lst);
    }

    public static <T extends ImportExportData> List<T> load(File file,
                                                            Class<T> clz) throws
                                                                          Exception
    {
        List<ImportExportData> lst = ObjectMappers.SkipNulls.readValue(file, c_typeRef);

        return CollectionUtils.transformToList(lst, clz::cast);
    }

    public static <T extends ImportExportData> void save(File file,
                                                         List<T> lst) throws
                                                                      Exception
    {
        ObjectWriter writer = ObjectMappers.SkipNulls.writerFor(c_typeRef)
                                                     .with(new DefaultPrettyPrinter());

        writer.writeValue(file, lst);
    }
}
