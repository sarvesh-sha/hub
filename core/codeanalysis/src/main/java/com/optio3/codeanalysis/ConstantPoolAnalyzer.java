/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis;

import java.nio.charset.Charset;
import java.util.function.Function;

import org.objectweb.asm.ClassReader;

public class ConstantPoolAnalyzer
{
    /**
     * The CONSTANT_Class_info structure is used to represent a class or an
     * interface:
     *
     * CONSTANT_Class_info { u1 tag; u2 name_index; }
     *
     * The value of the name_index item must be a valid index into the
     * constant_pool table. The constant_pool entry at that index must be a
     * CONSTANT_Utf8_info structure representing a valid binary class or
     * interface name encoded in internal form.
     */
    public static final int CONSTANT_Class = 7;

    /**
     * The type of CONSTANT_Fieldref constant pool items.
     *
     * CONSTANT_Fieldref_info { u1 tag; u2 class_index; u2 name_and_type_index;
     * }
     *
     * The value of the class_index item must be a valid index into the
     * constant_pool table. The constant_pool entry at that index must be a
     * CONSTANT_Class_info structure representing a class or interface type that
     * has the field as a member.
     *
     * The value of the name_and_type_index item must be a valid index into the
     * constant_pool table. The constant_pool entry at that index must be a
     * CONSTANT_NameAndType_info structure. This constant_pool entry indicates
     * the name and descriptor of the field.
     */
    public static final int CONSTANT_Fieldref = 9;

    /**
     * The type of CONSTANT_Methodref constant pool items.
     *
     * CONSTANT_Methodref_info { u1 tag; u2 class_index; u2 name_and_type_index;
     * }
     *
     * The value of the class_index item must be a valid index into the
     * constant_pool table. The constant_pool entry at that index must be a
     * CONSTANT_Class_info structure representing a class or interface type that
     * has the method as a member.
     *
     * The value of the name_and_type_index item must be a valid index into the
     * constant_pool table. The constant_pool entry at that index must be a
     * CONSTANT_NameAndType_info structure. This constant_pool entry indicates
     * the name and descriptor of the method.
     */
    public static final int CONSTANT_Methodref = 10;

    /**
     * The type of CONSTANT_InterfaceMethodref constant pool items.
     *
     * CONSTANT_InterfaceMethodref_info { u1 tag; u2 class_index; u2
     * name_and_type_index; }
     *
     * The value of the class_index item must be a valid index into the
     * constant_pool table. The constant_pool entry at that index must be a
     * CONSTANT_Class_info structure representing a class or interface type that
     * has the method as a member.
     *
     * The value of the name_and_type_index item must be a valid index into the
     * constant_pool table. The constant_pool entry at that index must be a
     * CONSTANT_NameAndType_info structure. This constant_pool entry indicates
     * the name and descriptor of the method.
     */
    public static final int CONSTANT_InterfaceMethodref = 11;

    /**
     * The CONSTANT_String_info structure is used to represent constant objects
     * of the type String:
     *
     * CONSTANT_String_info { u1 tag; u2 string_index; }
     *
     * The value of the string_index item must be a valid index into the
     * constant_pool table. The constant_pool entry at that index must be a
     * CONSTANT_Utf8_info structure representing the sequence of Unicode code
     * points to which the String object is to be initialized.
     */
    public static final int CONSTANT_String = 8;

    /**
     * The CONSTANT_Integer_info structure represents 4-byte numeric int
     * constants:
     *
     * CONSTANT_Integer_info { u1 tag; u4 bytes; }
     */
    public static final int CONSTANT_Integer = 3;

    /**
     * The CONSTANT_Float_info structure represents 4-byte numeric float
     * constants:
     *
     * CONSTANT_Float_info { u1 tag; u4 bytes; }
     */
    public static final int CONSTANT_Float = 4;

    /**
     * The CONSTANT_Long_info represents 8-byte numeric long constants:
     *
     * CONSTANT_Long_info { u1 tag; u4 high_bytes; u4 low_bytes; }
     */
    public static final int CONSTANT_Long = 5;

    /**
     * The CONSTANT_Double_info represents 8-byte numeric double constants:
     *
     * CONSTANT_Double_info { u1 tag; u4 high_bytes; u4 low_bytes; }
     */
    public static final int CONSTANT_Double = 6;

    /**
     * The CONSTANT_NameAndType_info structure is used to represent a field or
     * method, without indicating which class or interface type it belongs to:
     *
     * CONSTANT_NameAndType_info { u1 tag; u2 name_index; u2 descriptor_index; }
     *
     * The value of the name_index item must be a valid index into the
     * constant_pool table. The constant_pool entry at that index must be a
     * CONSTANT_Utf8_info structure representing either the special method name
     * <init> or a valid unqualified name denoting a field or method.
     *
     * The value of the descriptor_index item must be a valid index into the
     * constant_pool table. The constant_pool entry at that index must be a
     * CONSTANT_Utf8_info structure representing a valid field descriptor or
     * method descriptor.
     */
    public static final int CONSTANT_NameAndType = 12;

    /**
     * The CONSTANT_Utf8_info structure is used to represent constant string
     * values:
     *
     * CONSTANT_Utf8_info { u1 tag; u2 length; u1 bytes[ length]; }
     */
    public static final int CONSTANT_Utf8 = 1;

    /**
     * The CONSTANT_MethodHandle_info structure is used to represent a method
     * handle:
     *
     * CONSTANT_MethodHandle_info { u1 tag; u1 reference_kind; u2
     * reference_index; }
     *
     * The value of the reference_index item must be a valid index into the
     * constant_pool table. The constant_pool entry at that index must be as
     * follows:
     *
     * - If the value of the reference_kind item is 1 (REF_getField), 2
     * (REF_getStatic), 3 (REF_putField), or 4 (REF_putStatic), then the
     * constant_pool entry at that index must be a CONSTANT_Fieldref_info
     * structure representing a field for which a method handle is to be
     * created.
     *
     * - If the value of the reference_kind item is 5 (REF_invokeVirtual) or 8
     * (REF_newInvokeSpecial), then the constant_pool entry at that index must
     * be a CONSTANT_Methodref_info structure representing a class’s method or
     * constructor for which a method handle is to be created.
     *
     * - If the value of the reference_kind item is 6 (REF_invokeStatic) or 7
     * (REF_invokeSpecial), then if the class file version number is less than
     * 52.0, the constant_pool entry at that index must be a
     * CONSTANT_Methodref_info structure representing a class’s method for which
     * a method handle is to be created; if the class file version number is
     * 52.0 or above, the constant_pool entry at that index must be either a
     * CONSTANT_Methodref_info structure or a CONSTANT_InterfaceMethodref_info
     * structure representing a class’s or interface’s method for which a method
     * handle is to be created.
     *
     * - If the value of the reference_kind item is 9 (REF_invokeInterface),
     * then the constant_pool entry at that index must be a
     * CONSTANT_InterfaceMethodref_info structure representing an interface’s
     * method for which a method handle is to be created.
     *
     *
     * - If the value of the reference_kind item is 5 (REF_invokeVirtual), 6
     * (REF_invokeStatic), 7 (REF_invokeSpecial), or 9 (REF_invokeInterface),
     * the name of the method represented by a CONSTANT_Methodref_info structure
     * or a CONSTANT_InterfaceMethodref_info structure must not be < init > or <
     * clinit >. - If the value is 8 (REF_newInvokeSpecial), the name of the
     * method represented by a CONSTANT_Methodref_info structure must be < init
     * >.
     */
    public static final int CONSTANT_MethodHandle = 15;

    /**
     * The CONSTANT_MethodType_info structure is used to represent a method
     * type:
     *
     * CONSTANT_MethodType_info { u1 tag; u2 descriptor_index; }
     *
     * The value of the descriptor_index item must be a valid index into the
     * constant_pool table. The constant_pool entry at that index must be a
     * CONSTANT_Utf8_info structure (§ 4.4.7) representing a method descriptor.
     */
    public static final int CONSTANT_MethodType = 16;

    /**
     * The CONSTANT_InvokeDynamic_info structure is used by an invokedynamic
     * instruction to specify a bootstrap method, the dynamic invocation name,
     * the argument and return types of the call, and optionally, a sequence of
     * additional constants called static arguments to the bootstrap method.
     *
     * CONSTANT_InvokeDynamic_info { u1 tag; u2 bootstrap_method_attr_index; u2 name_and_type_index; }
     *
     * The value of the bootstrap_method_attr_index item must be a valid index
     * into the bootstrap_methods array of the bootstrap method table of this
     * class file.
     *
     * The value of the name_and_type_index item must be a valid index into the
     * constant_pool table. The constant_pool entry at that index must be a
     * CONSTANT_NameAndType_info structure representing a method name and method
     * descriptor.
     */
    public static final int CONSTANT_InvokeDynamic = 18;

    //--//

    public static class Entry
    {
        private static final Charset s_utf8 = Charset.forName("UTF-8");

        public int tag;

        public String className;
        public String memberName;
        public String descriptor;

        public Object value;

        Entry(int tag,
              ClassReader cr,
              int index,
              char[] bufChars,
              byte[] bufBytes)
        {
            this.tag = tag;

            switch (tag)
            {
                case CONSTANT_Class:
                    className = cr.readUTF8(index, bufChars);
                    break;

                case CONSTANT_Fieldref:
                case CONSTANT_Methodref:
                case CONSTANT_InterfaceMethodref:
                    parseMemberRef(cr, index, bufChars);
                    break;

                case CONSTANT_Integer:
                    value = cr.readInt(index);
                    break;

                case CONSTANT_Float:
                    value = Float.intBitsToFloat(cr.readInt(index));
                    break;

                case CONSTANT_Long:
                    value = cr.readLong(index);
                    break;

                case CONSTANT_Double:
                    value = Double.longBitsToDouble(cr.readLong(index));
                    break;

                case CONSTANT_NameAndType:
                    parseNameType(cr, index, bufChars);
                    break;

                case CONSTANT_Utf8:
                {
                    int len = cr.readUnsignedShort(index);
                    if (len > 0)
                    {
                        for (int i = 0; i < len; i++)
                        {
                            bufBytes[i] = (byte) cr.readByte(index + 2 + i);
                        }

                        value = new String(bufBytes, 0, len, s_utf8);
                    }

                    break;
                }

                case CONSTANT_MethodHandle:
                    value = cr.readByte(index);

                    int fieldOrMethodRef = readIndex(cr, index + 1);
                    parseMemberRef(cr, fieldOrMethodRef, bufChars);
                    break;

                case CONSTANT_InvokeDynamic:
                    value = cr.readUnsignedShort(index);

                    parseNameTypeRef(cr, index + 2, bufChars);
                    break;

                case CONSTANT_String:
                    value = cr.readUTF8(index, bufChars);
                    break;
            }
        }

        private void parseMemberRef(ClassReader cr,
                                    int index,
                                    char[] buf)
        {
            className = cr.readClass(index, buf);
            parseNameTypeRef(cr, index + 2, buf);
        }

        private void parseNameTypeRef(ClassReader cr,
                                      int index,
                                      char[] buf)
        {
            int nameTypeIdx = readIndex(cr, index);
            parseNameType(cr, nameTypeIdx, buf);
        }

        private void parseNameType(ClassReader cr,
                                   int index,
                                   char[] buf)
        {
            memberName = cr.readUTF8(index, buf);
            descriptor = cr.readUTF8(index + 2, buf);
        }

        private static int readIndex(ClassReader cr,
                                     int index)
        {
            return cr.getItem(cr.readUnsignedShort(index));
        }
    }

    //--//

    public static <T> T searchClassNames(ClassReader cr,
                                         Function<Entry, T> callback)
    {
        int    maxStringLength = cr.getMaxStringLength();
        char[] bufChars        = new char[maxStringLength];
        byte[] bufBytes        = new byte[4 * maxStringLength];
        int    ll              = cr.getItemCount();
        for (int itemNumber = 1; itemNumber < ll; itemNumber++)
        {
            int index = cr.getItem(itemNumber);
            if (index > 0)
            {
                int tag = cr.readByte(index - 1);
                switch (tag)
                {
                    //
                    // Type names could hide in annotations, which are tracked as method_info.
                    // We don't want to parse method_infos, so we also look for any string.
                    //
                    case CONSTANT_Utf8:

                    case CONSTANT_Class:
                    case CONSTANT_Fieldref:
                    case CONSTANT_Methodref:
                    case CONSTANT_InterfaceMethodref:
                    case CONSTANT_MethodHandle:
                    case CONSTANT_InvokeDynamic:
                        Entry v = new Entry(tag, cr, index, bufChars, bufBytes);
                        T res = callback.apply(v);
                        if (res != null)
                        {
                            return res;
                        }
                }
            }
        }

        return null;
    }

    public static <T> T searchMethodReferences(ClassReader cr,
                                               Function<Entry, T> callback)
    {
        int    maxStringLength = cr.getMaxStringLength();
        char[] bufChars        = new char[maxStringLength];
        byte[] bufBytes        = new byte[4 * maxStringLength];
        int    ll              = cr.getItemCount();
        for (int itemNumber = 1; itemNumber < ll; itemNumber++)
        {
            int index = cr.getItem(itemNumber);
            if (index > 0)
            {
                int tag = cr.readByte(index - 1);
                switch (tag)
                {
                    case CONSTANT_Methodref:
                    case CONSTANT_InterfaceMethodref:
                    case CONSTANT_MethodHandle:
                    case CONSTANT_InvokeDynamic:
                        Entry v = new Entry(tag, cr, index, bufChars, bufBytes);
                        T res = callback.apply(v);
                        if (res != null)
                        {
                            return res;
                        }
                }
            }
        }

        return null;
    }
}
