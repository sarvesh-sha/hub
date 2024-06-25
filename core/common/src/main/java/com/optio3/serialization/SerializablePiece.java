/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.serialization;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.Exceptions;

public class SerializablePiece
{
    static class MyComparator implements Comparator<SerializablePiece>
    {
        static final Comparator<SerializablePiece> c_instance = new MyComparator();

        @Override
        public int compare(SerializablePiece o1,
                           SerializablePiece o2)
        {
            int diff = o1.tagNumber - o2.tagNumber;
            if (diff == 0)
            {
                diff = o1.bitOffset - o2.bitOffset;
                if (diff == 0)
                {
                    diff = o1.getFieldName()
                             .compareTo(o2.getFieldName());
                }
            }

            return diff;
        }
    }

    private final Reflection.FieldAccessor    m_field;
    public final  Class<?>                    actualType;
    public final  Boolean                     customAsLittleEndian;
    public final  boolean                     isOptional;
    public final  boolean                     isList;
    public final  boolean                     isArray;
    public final  int                         fixedArraySize;
    public final  boolean                     isSimplePrimitive;
    public final  int                         tagNumber;
    public final  int                         bitWidth;
    public final  int                         bitOffset;
    public final  SerializationScaling        scaling;
    public final  int                         preProcessorLowerRange;
    public final  int                         preProcessorUpperRange;
    public final  SerializationValueProcessor preProcessor;
    public final  SerializationValueProcessor postProcessor;

    public final TypeDescriptor typeDescriptor;

    SerializablePiece(Field field,
                      SerializationTag tag)
    {
        m_field = new Reflection.FieldAccessor(field);
        this.tagNumber = tag.number();
        this.bitWidth = tag.width();
        this.bitOffset = tag.bitOffset();
        this.scaling = tag.scaling().length == 1 ? tag.scaling()[0] : null;
        this.preProcessorLowerRange = tag.preProcessorLowerRange();
        this.preProcessorUpperRange = tag.preProcessorUpperRange();
        this.preProcessor = instantiateIfNeeded(tag.preProcessor());
        this.postProcessor = instantiateIfNeeded(tag.postProcessor());

        if (tag.asBigEndian())
        {
            customAsLittleEndian = false;
        }
        else if (tag.asLittleEndian())
        {
            customAsLittleEndian = true;
        }
        else
        {
            customAsLittleEndian = null;
        }

        AnnotatedType annoType = field.getAnnotatedType();
        Type          type     = annoType.getType();
        isOptional = Reflection.isSubclassOf(Optional.class, type);
        if (isOptional)
        {
            type = Reflection.getTypeArgument(type, 0);
        }

        isList = Reflection.isSubclassOf(List.class, type);
        if (isList)
        {
            type = Reflection.getTypeArgument(type, 0);
        }

        if (type instanceof ParameterizedType)
        {
            throw Exceptions.newRuntimeException("Unsupported type for serialization: %s", type);
        }

        Class<?> clz = (Class<?>) type;

        isArray = clz.isArray();
        if (isArray)
        {
            clz = clz.getComponentType();
        }

        fixedArraySize = tag.fixedArraySize();

        actualType = clz;

        typeDescriptor = Reflection.getDescriptor(clz);

        boolean isSimplePrimitive;
        boolean isPrimitive = typeDescriptor != null && typeDescriptor.isPrimitive();
        if (isPrimitive && scaling == null && bitOffset <= 0 && (bitWidth <= 0 || bitWidth == typeDescriptor.size))
        {
            isSimplePrimitive = true;
        }
        else
        {
            isSimplePrimitive = false;
        }

        this.isSimplePrimitive = isSimplePrimitive;

        // We let reflection access private fields.
        field.setAccessible(true);
    }

    private static SerializationValueProcessor instantiateIfNeeded(Class<? extends SerializationValueProcessor> processorClz)
    {
        return processorClz != SerializationValueProcessor.class ? Reflection.newInstance(processorClz) : null;
    }

    public boolean shouldDecode(ConditionalFieldSelector selector)
    {
        if (selector == null)
        {
            return true;
        }

        return selector.shouldDecode(getFieldName());
    }

    public boolean shouldEncode(ConditionalFieldSelector selector)
    {
        if (selector == null)
        {
            return true;
        }

        return selector.shouldEncode(getFieldName());
    }

    public boolean encodeValue(ConditionalFieldSelector selector,
                               OutputBuffer buffer,
                               Object value)
    {
        if (selector == null)
        {
            return false;
        }

        return selector.encodeValue(getFieldName(), buffer, value);
    }

    public Optional<Object> provideValue(ConditionalFieldSelector selector,
                                         InputBuffer buffer)
    {
        if (selector == null)
        {
            return Optional.empty();
        }

        return selector.provideValue(getFieldName(), buffer);
    }

    public int computeActualWidthInBits()
    {
        if (bitWidth > 0)
        {
            return bitWidth;
        }

        if (typeDescriptor != null)
        {
            return typeDescriptor.size;
        }

        return -1;
    }

    public int computeStorageSize()
    {
        int width = computeActualWidthInBits();
        if (width < 0)
        {
            return -1;
        }

        int bitOffset = this.bitOffset;
        if (bitOffset >= 0)
        {
            width += bitOffset;
        }

        return (width + 7) / 8;
    }

    //--//

    public Object readPrimitiveValue(InputBuffer buffer)
    {
        TypeDescriptorKind kind = computeTypeKindForRead();

        int  numBytes = Math.min(buffer.remainingLength(), computeStorageSize());
        long v;

        if (customAsLittleEndian != null)
        {
            boolean previous = buffer.littleEndian;
            buffer.littleEndian = customAsLittleEndian;
            v = buffer.readGenericInteger(numBytes, kind);
            buffer.littleEndian = previous;
        }
        else
        {
            v = buffer.readGenericInteger(numBytes, kind);
        }

        int maskWidth = computeActualWidthInBits();
        v = kind.signAwareMasking(v, maskWidth);

        return processAndScaleIfNeeded(v);
    }

    public Object readBitfieldValue(long v)
    {
        TypeDescriptorKind kind = computeTypeKindForRead();

        int bitOffset = this.bitOffset;
        if (bitOffset >= 0)
        {
            v = kind.signAwareRightShift(v, bitOffset);
        }

        int maskWidth = 64 - computeActualWidthInBits();
        v <<= maskWidth;

        v = kind.signAwareRightShift(v, maskWidth);

        return processAndScaleIfNeeded(v);
    }

    private TypeDescriptorKind computeTypeKindForRead()
    {
        if (scaling != null)
        {
            return scaling.assumeUnsigned() ? TypeDescriptorKind.integerUnsigned : TypeDescriptorKind.integerSigned;
        }
        else
        {
            return typeDescriptor.kind;
        }
    }

    private Object processAndScaleIfNeeded(long v)
    {
        if (preProcessor != null)
        {
            Optional<Object> res = preProcessor.handle(this, v);
            if (res != null)
            {
                return res.isPresent() ? res.get() : null;
            }
        }

        Object valueOutput = scaleIfNeeded(v);

        if (postProcessor != null)
        {
            Optional<Object> res = postProcessor.handle(this, valueOutput);
            if (res != null)
            {
                return res.isPresent() ? res.get() : null;
            }
        }

        return valueOutput;
    }

    private Object scaleIfNeeded(long v)
    {
        if (scaling == null)
        {
            return typeDescriptor.fromLongValue(v);
        }
        else
        {
            double val = v + scaling.preScalingOffset();

            val *= scaling.scalingFactor();

            val += scaling.postScalingOffset();

            return val;
        }
    }

    public void writePrimitiveValue(OutputBuffer buffer,
                                    Object value)
    {
        long v;

        if (scaling != null)
        {
            Number num = (Number) value;
            double val = num != null ? num.doubleValue() : 0;

            val -= scaling.postScalingOffset();
            val /= scaling.scalingFactor();
            val -= scaling.preScalingOffset();

            v = Math.round(val);
        }
        else
        {
            v = value != null ? typeDescriptor.asLongValue(value) : 0;
        }

        int maskWidth = computeActualWidthInBits();
        v <<= maskWidth;
        v >>>= maskWidth;

        if (customAsLittleEndian != null)
        {
            boolean previous = buffer.littleEndian;
            buffer.littleEndian = customAsLittleEndian;
            buffer.emitGenericInteger(v, computeStorageSize());
            buffer.littleEndian = previous;
        }
        else
        {
            buffer.emitGenericInteger(v, computeStorageSize());
        }
    }

    //--//

    public Optional<?> getFieldRaw(Object target)
    {
        Object value = m_field.get(target);

        if (value instanceof Optional)
        {
            return (Optional<?>) value;
        }

        if (value == null)
        {
            return Optional.empty();
        }

        return Optional.of(value);
    }

    public Object getField(Object target)
    {
        Object value = getFieldDirect(target);

        if (value instanceof Optional)
        {
            Optional<?> opt = (Optional<?>) value;

            if (opt.isPresent())
            {
                value = opt.get();
            }
            else
            {
                value = null;
            }
        }

        return value;
    }

    public Object getFieldDirect(Object target)
    {
        return m_field.get(target);
    }

    public void setField(Object target,
                         Object value)
    {
        if (value == null && m_field.getNativeType() == Object.class)
        {
            value = Null.instance;
        }

        if (isOptional)
        {
            if (value == null)
            {
                value = Optional.empty();
            }
            else
            {
                value = Optional.of(value);
            }
        }

        setFieldDirect(target, value);
    }

    public void setFieldDirect(Object target,
                               Object value)
    {
        m_field.set(target, value);
    }

    //--//

    public String getFieldName()
    {
        return m_field.getName();
    }

    public <T extends Annotation> T getFieldAnnotation(Class<T> clz)
    {
        return m_field.getAnnotation(clz);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("[Field='%s.%s', ActualType='%s'",
                                m_field.getDeclaringClass()
                                       .getName(),
                                m_field.getName(),
                                actualType.getName()));

        if (isList)
        {
            sb.append(", IsList");
        }

        if (isArray)
        {
            sb.append(", IsArray");
        }

        if ((isList || isArray) && fixedArraySize > 0)
        {
            sb.append(String.format("(%d slots)", fixedArraySize));
        }

        if (isOptional)
        {
            sb.append(", IsOptional");
        }

        sb.append("]");
        return sb.toString();
    }

    //--//

    void validateForBitfield()
    {
        if (typeDescriptor == null)
        {
            throw Exceptions.newRuntimeException("Invalid member of a bit field, it's not a primitive or enum: %s", m_field.getNative());
        }
    }
}
