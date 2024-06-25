/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.protocol.serialization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializablePiece;
import com.optio3.serialization.SerializationHelper;
import com.optio3.serialization.SerializationSlotToFields;
import com.optio3.serialization.SerializationTag;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;

public class SerializationTest
{
    public static class Source
    {
        public int f1;

        public String f2;

        public int getF1()
        {
            return f1;
        }

        public void setF1(int f1)
        {
            this.f1 = f1;
        }

        public String getF2()
        {
            return f2;
        }

        public void setF2(String f2)
        {
            this.f2 = f2;
        }
    }

    @Test
    public void testUsingOptio3Reflection()
    {
        assertNotNull(Reflection.findGetter(Source.class, "f1"));
        assertNotNull(Reflection.findGetter(Source.class, "f2"));

        assertNotNull(Reflection.findSetter(Source.class, "f1", int.class));
        assertNull(Reflection.findSetter(Source.class, "f1", double.class));

        assertNotNull(Reflection.findSetter(Source.class, "f2", String.class));
        assertNull(Reflection.findSetter(Source.class, "f2", Object.class));

        assertNull(Reflection.findGetter(Source.class, "foo"));
        assertNull(Reflection.findSetter(Source.class, "foo", null));
    }

    @Test
    public void testUsingReflectionsLibrary()
    {
        Reflections   reflections = new Reflections("com.optio3.", new FieldAnnotationsScanner());
        Set<Class<?>> classes     = Sets.newHashSet();

        for (Field f : reflections.getFieldsAnnotatedWith(SerializationTag.class))
        {
            classes.add(f.getDeclaringClass());
        }

        for (Class<?> clz : classes)
        {
            List<SerializationSlotToFields> group = SerializationHelper.collectTags(clz);
            for (SerializationSlotToFields offset : group)
            {
                System.out.println(String.format("Class %s : %d", clz.getName(), offset.sequence));
                if (offset.normalPiece != null)
                {
                    System.out.println(String.format("  Field %s : width=%d", offset.normalPiece.getFieldName(), offset.normalPiece.computeActualWidthInBits()));
                }
                else
                {
                    for (SerializablePiece piece : offset.bitfieldPieces)
                    {
                        int firstBit = piece.bitOffset;
                        if (firstBit < 0)
                        {
                            System.out.println(String.format("  Field %s : width=%d", piece.getFieldName(), piece.computeActualWidthInBits()));
                        }
                        else
                        {
                            System.out.println(String.format("  Field %s : width=%d bitOffset=%d", piece.getFieldName(), piece.computeActualWidthInBits(), firstBit));
                        }
                    }
                }
            }
        }
    }

    //--//

    static class GenericBase<T1, T2>
    {
    }

    // Generic class that swaps the type arguments
    static class GenericMid<T1, T2> extends GenericBase<T2, T1>
    {

    }

    static class UseT1 extends GenericMid<String, Long>
    {
    }

    static class UseT2 extends GenericMid<Long, String>
    {
    }

    @Test
    public void testTypeInference()
    {
        assertEquals(Long.class, Reflection.searchTypeArgument(GenericBase.class, new UseT1(), 0));
        assertEquals(String.class, Reflection.searchTypeArgument(GenericBase.class, new UseT1(), 1));

        assertEquals(String.class, Reflection.searchTypeArgument(GenericBase.class, new UseT2(), 0));
        assertEquals(Long.class, Reflection.searchTypeArgument(GenericBase.class, new UseT2(), 1));
    }
}
