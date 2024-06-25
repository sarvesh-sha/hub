/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.serialization;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;

public class SerializationHelper
{
    static final ConcurrentMap<Class<?>, List<SerializationSlotToFields>> s_classToTags = Maps.newConcurrentMap();

    public static List<SerializationSlotToFields> collectTags(Class<?> clz)
    {
        List<SerializationSlotToFields> res = s_classToTags.get(clz);
        if (res != null)
        {
            return res;
        }

        Multimap<Integer, SerializablePiece> group = HashMultimap.create();

        for (Field f : Reflection.collectFields(clz)
                                 .values())
        {
            SerializationTag t = f.getAnnotation(SerializationTag.class);
            if (t != null)
            {
                group.put(t.number(), new SerializablePiece(f, t));
            }
        }

        for (Integer number : group.keySet())
        {
            if (res == null)
            {
                res = Lists.newArrayList();
            }

            res.add(new SerializationSlotToFields(number, group.get(number)));
        }

        if (res == null)
        {
            res = Collections.emptyList();
        }
        else
        {
            res.sort(SerializationSlotToFields.MyComparator.c_instance);
            res = Collections.unmodifiableList(res);
        }

        List<SerializationSlotToFields> oldRes = s_classToTags.putIfAbsent(clz, res);
        return oldRes != null ? oldRes : res;
    }

    //--//

    public static void write(OutputBuffer buffer,
                             Object target)
    {
        List<SerializationSlotToFields> slots = SerializationHelper.collectTags(target.getClass());
        for (SerializationSlotToFields slot : slots)
        {
            writeSingleValue(buffer, target, slot);
        }
    }

    private static void writeSingleValue(OutputBuffer buffer,
                                         Object target,
                                         SerializationSlotToFields slot)
    {
        ConditionalFieldSelector selector = Reflection.as(target, ConditionalFieldSelector.class);

        if (slot.normalPiece != null)
        {
            SerializablePiece piece = slot.normalPiece;

            if (piece.shouldEncode(selector))
            {
                if (slot.isOptional())
                {
                    Optional<?> valueOpt = (Optional<?>) piece.getFieldDirect(target);
                    if (valueOpt == null || !valueOpt.isPresent())
                    {
                        return;
                    }
                }

                boolean isList  = piece.isList;
                boolean isArray = piece.isArray;
                Object  value   = piece.getField(target);

                if (!piece.encodeValue(selector, buffer, value))
                {
                    int fixedArraySize = piece.fixedArraySize;

                    if (isArray && piece.isSimplePrimitive && piece.actualType == byte.class)
                    {
                        byte[] array = (byte[]) value;

                        if (fixedArraySize > 0)
                        {
                            array = Arrays.copyOf(array, fixedArraySize);
                        }

                        buffer.emit(array);
                    }
                    else if (isList || isArray)
                    {
                        int length = getCollectionLength(value, isList, isArray);

                        if (fixedArraySize > 0)
                        {
                            length = fixedArraySize;
                        }

                        for (int i = 0; i < length; i++)
                        {
                            Object value2 = getCollectionItem(value, isList, isArray, fixedArraySize, i);
                            if (piece.typeDescriptor != null)
                            {
                                piece.writePrimitiveValue(buffer, value2);
                            }
                            else
                            {
                                write(buffer, value2);
                            }
                        }
                    }
                    else
                    {
                        if (piece.typeDescriptor != null)
                        {
                            piece.writePrimitiveValue(buffer, value);
                        }
                        else
                        {
                            write(buffer, value);
                        }
                    }
                }
            }
        }
        else
        {
            int maxBytes = -1;

            for (SerializablePiece piece : slot.bitfieldPieces)
            {
                maxBytes = Math.max(maxBytes, piece.computeStorageSize());
            }

            long v = 0;

            for (SerializablePiece piece : slot.bitfieldPieces)
            {
                if (piece.shouldEncode(selector))
                {
                    TypeDescriptor td = piece.typeDescriptor;

                    Object obj = piece.getField(target);
                    long   v2  = td.asLongValue(obj);

                    int maskWidth = 64 - piece.computeActualWidthInBits();
                    v2 <<= maskWidth;
                    v2 >>>= maskWidth;

                    int bitOffset = piece.bitOffset;
                    if (bitOffset >= 0)
                    {
                        v2 <<= bitOffset;
                    }

                    v |= v2;
                }
            }

            buffer.emitGenericInteger(v, maxBytes);
        }
    }

    public static int getCollectionLength(Object value,
                                          boolean isList,
                                          boolean isArray)
    {
        if (isList)
        {
            List<?> list = (List<?>) value;
            return list.size();
        }

        if (isArray)
        {
            return Array.getLength(value);
        }

        return 0;
    }

    public static Object getCollectionItem(Object value,
                                           boolean isList,
                                           boolean isArray,
                                           int fixedArraySize,
                                           int index)
    {
        if (fixedArraySize > 0 && getCollectionLength(value, isList, isArray) < fixedArraySize)
        {
            return null;
        }

        if (isList)
        {
            List<?> list = (List<?>) value;
            return list.get(index);
        }

        if (isArray)
        {
            return Array.get(value, index);
        }

        return null;
    }

    //--//

    public static void read(InputBuffer buffer,
                            Object target)
    {
        List<SerializationSlotToFields> slots = SerializationHelper.collectTags(target.getClass());
        for (SerializationSlotToFields slot : slots)
        {
            readSingleValue(buffer, target, slot);
        }
    }

    private static void readSingleValue(InputBuffer buffer,
                                        Object target,
                                        SerializationSlotToFields slot)
    {
        ConditionalFieldSelector selector = Reflection.as(target, ConditionalFieldSelector.class);

        if (slot.normalPiece != null)
        {
            SerializablePiece piece = slot.normalPiece;

            if (piece.shouldDecode(selector))
            {
                Object value;

                Optional<Object> opt = piece.provideValue(selector, buffer);
                if (opt.isPresent())
                {
                    value = opt.get();
                }
                else
                {
                    boolean isList  = piece.isList;
                    boolean isArray = piece.isArray;

                    if (isArray && piece.isSimplePrimitive && piece.actualType == byte.class)
                    {
                        value = buffer.readByteArray(buffer.remainingLength());
                    }
                    else if (isList || isArray)
                    {
                        List<Object> list           = Lists.newArrayList();
                        int          fixedArraySize = piece.fixedArraySize;

                        while (!buffer.isEOF())
                        {
                            Object valueSub;

                            if (Reflection.getDescriptor(piece.actualType) != null)
                            {
                                valueSub = piece.readPrimitiveValue(buffer);
                            }
                            else
                            {
                                valueSub = Reflection.newInstance(piece.actualType);
                                read(buffer, valueSub);
                            }
                            list.add(valueSub);

                            if (fixedArraySize > 0)
                            {
                                fixedArraySize--;

                                if (fixedArraySize == 0)
                                {
                                    break;
                                }
                            }
                        }

                        while (fixedArraySize-- > 0)
                        {
                            list.add(null);
                        }

                        if (isArray)
                        {
                            Object array = Array.newInstance(piece.actualType, list.size());
                            for (int i = 0; i < list.size(); i++)
                                Array.set(array, i, list.get(i));

                            value = array;
                        }
                        else
                        {
                            value = list;
                        }
                    }
                    else
                    {
                        if (slot.isOptional() && buffer.isEOF())
                        {
                            piece.setFieldDirect(target, Optional.empty());
                            return;
                        }

                        value = piece.readPrimitiveValue(buffer);
                    }
                }

                piece.setField(target, value);
            }
        }
        else
        {
            int maxBytes = -1;

            for (SerializablePiece piece : slot.bitfieldPieces)
            {
                maxBytes = Math.max(maxBytes, piece.computeStorageSize());
            }

            final long v = buffer.readGenericInteger(maxBytes, TypeDescriptorKind.integerUnsigned);

            for (SerializablePiece piece : slot.bitfieldPieces)
            {
                if (piece.shouldDecode(selector))
                {
                    Object value = piece.readBitfieldValue(v);

                    piece.setField(target, value);
                }
            }
        }
    }
}
