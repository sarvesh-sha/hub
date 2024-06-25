/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.directory;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.io.Files;
import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

@JsonSerialize(using = FileInfo.Serializer.class)
@JsonDeserialize(using = FileInfo.Deserializer.class)
public class FileInfo
{
    public static class Serializer extends StdSerializer<FileInfo>
    {
        private static final long serialVersionUID = 1L;

        public Serializer()
        {
            this(null);
        }

        public Serializer(Class<FileInfo> t)
        {
            super(t);
        }

        @Override
        public void serialize(FileInfo value,
                              JsonGenerator gen,
                              SerializerProvider provider) throws
                                                           IOException
        {
            gen.writeString(value.fileName);
        }
    }

    public static class Deserializer extends StdDeserializer<FileInfo>
    {
        private static final long serialVersionUID = 1L;

        public Deserializer()
        {
            this(null);
        }

        public Deserializer(Class<FileInfo> t)
        {
            super(t);
        }

        @Override
        public FileInfo deserialize(JsonParser p,
                                    DeserializationContext ctxt) throws
                                                                 IOException,
                                                                 JsonProcessingException
        {
            FileInfo value = new FileInfo();
            value.fileName = p.readValueAs(String.class);
            return value;
        }
    }

    public static class KeyDeserializerImpl extends KeyDeserializer
    {
        @Override
        public Object deserializeKey(String key,
                                     DeserializationContext ctxt)
        {
            FileInfo value = new FileInfo();
            value.fileName = key;
            return value;
        }
    }

    //--//

    public String fileName;

    //--//

    @Override
    public int hashCode()
    {
        return fileName != null ? fileName.hashCode() : 0;
    }

    @Override
    public boolean equals(Object o)
    {
        FileInfo that = Reflection.as(o, FileInfo.class);
        if (that == null)
        {
            return false;
        }

        return StringUtils.contains(fileName, that.fileName);
    }

    @Override
    public String toString()
    {
        return fileName;
    }

    //--//

    public byte[] load(File root) throws
                                  IOException
    {
        return Files.toByteArray(new File(root, fileName));
    }
}
