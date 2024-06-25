/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.codeanalysis.logging.CodeAnalysisLogger;
import com.optio3.codeanalysis.logging.WellKnownContexts;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public class TypeResolver
{
    public interface Loader
    {
        URL locateResource(String internalClassName) throws
                                                     IOException;
    }

    private static Loader s_defaultLoader = new Loader()
    {
        @Override
        public URL locateResource(String internalClassName) throws
                                                            IOException
        {
            return ClassLoader.getSystemResource(internalClassName + ".class");
        }
    };

    //--//

    public static class ClassLocator
    {
        public final File        file;
        public final String      resource;
        public final ClassReader classReader;

        public ClassLocator(byte[] bytecode)
        {
            this.file = null;
            this.resource = null;
            this.classReader = new ClassReader(bytecode);
        }

        public ClassLocator(String internalClassName,
                            URL url,
                            ClassReader classReader)
        {
            final String protocol = url.getProtocol();
            final String filePart = url.getFile();

            File   file     = null;
            String resource = null;

            switch (protocol)
            {
                case "jar":
                    try
                    {
                        String   filePart2 = new URL(filePart).getFile();
                        String[] parts     = StringUtils.splitByWholeSeparator(filePart2, "!/");

                        if (parts.length == 2)
                        {
                            file = new File(parts[0]);
                            resource = parts[1];
                        }
                        else
                        {
                            file = new File(filePart2);
                        }
                    }
                    catch (Exception e)
                    {
                        // Ignore failures.
                    }
                    break;

                case "file":
                    int pos = filePart.indexOf(internalClassName);
                    if (pos >= 0)
                    {
                        file = new File(filePart.substring(0,pos));
                    }
                    else
                    {
                        file = new File(filePart);
                    }
                    break;
            }

            this.file = file;
            this.resource = resource;
            this.classReader = classReader;
        }
    }

    private static class BytecodeCache
    {
        final Loader                                             loader;
        final ConcurrentMap<String, SoftReference<ClassLocator>> readers = Maps.newConcurrentMap();

        BytecodeCache(Loader loader)
        {
            this.loader = loader;
        }

        ClassLocator get(String internalClassName) throws
                                                   IOException
        {
            while (true)
            {
                ClassLocator                cl;
                SoftReference<ClassLocator> ref = readers.get(internalClassName);
                if (ref != null)
                {
                    cl = ref.get();
                    if (cl != null)
                    {
                        return cl;
                    }

                    readers.remove(internalClassName, ref);
                }

                URL url = loader.locateResource(internalClassName);
                if (url != null)
                {
                    try (InputStream stream = url.openStream())
                    {
                        cl = new ClassLocator(internalClassName, url, new ClassReader(stream));
                    }
                }
                else
                {
                    cl = null;
                }

                ref = new SoftReference<>(cl);

                // Try to insert the cache into the map.
                // But if we lose the race, loop again.
                if (readers.putIfAbsent(internalClassName, ref) == null)
                {
                    return cl;
                }
            }
        }
    }

    private static ConcurrentMap<Loader, SoftReference<BytecodeCache>> s_perLoaderReader = Maps.newConcurrentMap();

    private static BytecodeCache getCache(Loader loader)
    {
        while (true)
        {
            BytecodeCache                cache;
            SoftReference<BytecodeCache> ref = s_perLoaderReader.get(loader);
            if (ref != null)
            {
                cache = ref.get();
                if (cache != null)
                {
                    return cache;
                }

                s_perLoaderReader.remove(loader, ref);
            }

            cache = new BytecodeCache(loader);
            ref = new SoftReference<BytecodeCache>(cache);

            // Try to insert the cache into the map.
            // But if we lose the race, loop again.
            if (s_perLoaderReader.putIfAbsent(loader, ref) == null)
            {
                return cache;
            }
        }
    }

    //--//

    private static final Type[] s_empty = new Type[0];

    public static final Type TypeForNull                    = Type.getObjectType("null");
    public static final Type PlaceholderTypeForArray        = Type.getObjectType("java/lang/reflect/Array");
    public static final Type TypeForObject                  = Type.getObjectType("java/lang/Object");
    public static final Type TypeForString                  = Type.getObjectType("java/lang/String");
    public static final Type TypeForClass                   = Type.getObjectType("java/lang/Class");
    public static final Type TypeForThrowable               = Type.getObjectType("java/lang/Throwable");
    public static final Type TypeForMethod                  = Type.getObjectType("java/lang/invoke/MethodType");
    public static final Type TypeForMethodHandle            = Type.getObjectType("java/lang/invoke/MethodHandle");
    public static final Type TypeForPrimitiveArrayOfBoolean = Type.getType("[Z");
    public static final Type TypeForPrimitiveArrayOfChar    = Type.getType("[C");
    public static final Type TypeForPrimitiveArrayOfByte    = Type.getType("[B");
    public static final Type TypeForPrimitiveArrayOfShort   = Type.getType("[S");
    public static final Type TypeForPrimitiveArrayOfInteger = Type.getType("[I");
    public static final Type TypeForPrimitiveArrayOfFloat   = Type.getType("[F");
    public static final Type TypeForPrimitiveArrayOfDouble  = Type.getType("[D");
    public static final Type TypeForPrimitiveArrayOfLong    = Type.getType("[J");

    //--//

    protected final CodeAnalysisLogger logger;

    private final Loader m_loader;

    private final Map<Type, Type>   m_superClasses = Maps.newHashMap();
    private final Map<Type, Type[]> m_interfaces   = Maps.newHashMap();

    private final Map<String, GenericType> m_signatures = Maps.newHashMap();

    private final Map<Type, GenericTypeInfo> m_descriptors = Maps.newHashMap();

    public TypeResolver(CodeAnalysisLogger logger,
                        Loader loader)
    {
        this.logger = logger != null ? logger : CodeAnalysisLogger.nullLogger;

        m_loader = loader != null ? loader : s_defaultLoader;
    }

    //--//

    public final Type getSuperclass(Type type) throws
                                               AnalyzerException
    {
        if (TypeResolver.TypeForNull.equals(type))
        {
            return null;
        }

        if (TypeResolver.PlaceholderTypeForArray.equals(type))
        {
            return TypeResolver.TypeForObject;
        }

        if (isArray(type))
        {
            return TypeResolver.TypeForObject;
        }

        if (!isObject(type))
        {
            return null;
        }

        Type typeSuper = m_superClasses.get(type);
        if (typeSuper == null)
        {
            typeSuper = getSuperclassSlow(type);
            m_superClasses.put(type, typeSuper);
        }
        return typeSuper;
    }

    private Type getSuperclassSlow(Type type) throws
                                              AnalyzerException
    {
        GenericTypeInfo gti        = getGenericTypeInfo(type);
        GenericType     superClass = gti.getSuperclass();
        if (superClass == null)
        {
            return null;
        }

        return superClass.asRawType();
    }

    //--//

    public final Type[] getInterfaces(Type type) throws
                                                 AnalyzerException
    {
        if (TypeResolver.TypeForNull.equals(type))
        {
            return s_empty;
        }

        Type[] itf = m_interfaces.get(type);
        if (itf == null)
        {
            itf = getInterfacesSlow(type);
            m_interfaces.put(type, itf);
        }
        return itf;
    }

    private Type[] getInterfacesSlow(Type type) throws
                                                AnalyzerException
    {
        Set<Type> res = Sets.newHashSet();
        while (type != null)
        {
            GenericTypeInfo gti = getGenericTypeInfo(type);
            for (GenericType itf : gti.getInterfaces())
                res.add(itf.asRawType());

            type = getSuperclass(type);
        }

        Type[] resArray = new Type[res.size()];
        res.toArray(resArray);
        return resArray;
    }

    //--//

    public static Type getElementType(Type type)
    {
        return type == PlaceholderTypeForArray ? TypeForObject : type.getElementType();
    }

    public boolean canCastTo(final GenericTypeInfo target,
                             final GenericType source) throws
                                                       AnalyzerException
    {
        return canCastTo(target.asType(), source.asRawType());
    }

    public boolean canCastTo(final Type target,
                             final Type source) throws
                                                AnalyzerException
    {
        logger.trace(WellKnownContexts.TypeResolver, "canCastTo: %s <= %s", target, source);

        if (target == source)
        {
            return true;
        }

        if (TypeResolver.TypeForObject.equals(target))
        {
            if (!isReference(source))
            {
                logger.debug(WellKnownContexts.TypeResolver, "canCastTo: %s <= %s, failed because source not a reference", target, source);
                return false;
            }

            return true;
        }

        if (TypeResolver.PlaceholderTypeForArray.equals(target))
        {
            if (isNull(source))
            {
                return true;
            }

            if (!isArray(source))
            {
                logger.debug(WellKnownContexts.TypeResolver, "canCastTo: %s <= %s, failed because source not an array", target, source);
                return false;
            }

            return true;
        }

        if (isObject(target))
        {
            if (isNull(source))
            {
                return true;
            }

            if (!isObject(source))
            {
                logger.debug(WellKnownContexts.TypeResolver, "canCastTo2: %s <= %s, failed because source not a class", target, source);
                return false;
            }

            Type ptr = source;
            while (ptr != null)
            {
                if (target.equals(ptr))
                {
                    return true;
                }

                ptr = getSuperclass(ptr);
            }

            // Just in case 'target' is an interface, let's try all the implemented interfaces.
            for (Type itf : getInterfaces(source))
            {
                if (canCastTo(target, itf))
                {
                    return true;
                }
            }

            return false;
        }

        if (isArray(target))
        {
            if (isNull(source))
            {
                return true;
            }

            if (!isArray(source))
            {
                logger.debug(WellKnownContexts.TypeResolver, "canCastTo: %s <= %s, failed because source not an array", target, source);
                return false;
            }

            Type targetElement = TypeResolver.getElementType(target);
            Type sourceElement = TypeResolver.getElementType(source);

            return canCastTo(targetElement, sourceElement);
        }

        Type target2 = coerceToInt(target);
        Type source2 = coerceToInt(source);

        if (target2.equals(source2))
        {
            return true;
        }

        return false;
    }

    public Type findCommonSuperclass(final Type t1,
                                     final Type t2) throws
                                                    AnalyzerException
    {
        if (t1 == null || t2 == null)
        {
            return null;
        }

        if (t1.equals(t2))
        {
            return t1;
        }

        if (canCastTo(t1, t2))
        {
            return t1;
        }

        if (canCastTo(t2, t1))
        {
            return t2;
        }

        Type superT1 = findCommonSuperclass(getSuperclass(t1), t2);
        if (superT1 != null)
        {
            return superT1;
        }

        Type superT2 = findCommonSuperclass(getSuperclass(t2), t1);
        if (superT2 != null)
        {
            return superT2;
        }

        return null;
    }

    public static boolean isNull(Type type)
    {
        return type == TypeResolver.TypeForNull;
    }

    public static boolean isArray(Type type)
    {
        return type.getSort() == Type.ARRAY;
    }

    public static boolean isObject(Type type)
    {
        return type.getSort() == Type.OBJECT;
    }

    public static boolean isReference(Type type)
    {
        return isObject(type) || isArray(type) || isNull(type);
    }

    public static Type coerceToInt(Type type)
    {
        //
        // Extend values to INT when pushing into the stack.
        //
        switch (type.getSort())
        {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
                return Type.INT_TYPE;
        }

        return type;
    }

    //--//

    public GenericType.TypeDeclaration parseGenericTypeDeclaration(String signature,
                                                                   GenericType.GenericMethodOrType context)
    {
        return (GenericType.TypeDeclaration) parse(signature, context, false);
    }

    public GenericType.MethodDescriptor parseGenericMethodSignature(String signature,
                                                                    GenericType.GenericMethodOrType context)
    {
        return (GenericType.MethodDescriptor) parse(signature, context, false);
    }

    public GenericType parseGenericTypeReference(String signature,
                                                 GenericType.GenericMethodOrType context)
    {
        return parse(signature, context, true);
    }

    public GenericType getGenericTypeReference(Type type)
    {
        return parseGenericTypeReference(type.getDescriptor(), null);
    }

    private GenericType parse(String signature,
                              GenericType.GenericMethodOrType context,
                              boolean asType)
    {
        if (context != null)
        {
            if (asType)
            {
                return context.parseAsType(signature);
            }
            else
            {
                return context.parse(signature);
            }
        }

        GenericType gt = m_signatures.get(signature);
        if (gt == null)
        {
            if (asType)
            {
                gt = GenericType.parseType(signature, null);
            }
            else
            {
                gt = GenericType.parse(signature, null);
            }

            m_signatures.put(signature, gt);
        }

        return gt;
    }

    //--//

    synchronized void putGenericTypeInfo(GenericTypeInfo gti) throws
                                                              AnalyzerException
    {
        Type type = gti.asType();

        GenericTypeInfo gtiOld = m_descriptors.get(type);
        if (gtiOld != null)
        {
            throw TypeResolver.reportProblem("Attempt to redefine '%s'", type);
        }

        m_descriptors.put(type, gti);
    }

    public synchronized GenericTypeInfo getGenericTypeInfo(Type type) throws
                                                                      AnalyzerException
    {
        GenericTypeInfo gti = m_descriptors.get(type);
        if (gti == null)
        {
            gti = getGenericTypeInfoSlow(type);
            m_descriptors.put(type, gti);
        }

        return gti;
    }

    protected GenericTypeInfo getGenericTypeInfoSlow(Type type) throws
                                                                AnalyzerException
    {
        String internalName = type.getInternalName();

        GenericTypeInfo                 enclosingType;
        GenericType.GenericMethodOrType context;

        String outerClassName;
        String innerClassName;

        int innerClassPos = internalName.lastIndexOf('$');
        if (innerClassPos >= 0)
        {
            outerClassName = internalName.substring(0, innerClassPos);
            innerClassName = internalName.substring(innerClassPos + 1);

            Type outerType = Type.getObjectType(outerClassName);

            enclosingType = getGenericTypeInfo(outerType);
            context = enclosingType.getSignature();
        }
        else
        {
            outerClassName = null;
            innerClassName = internalName;
            enclosingType = null;
            context = null;
        }

        ClassLocator cl;

        try
        {
            cl = getClassReader(internalName);
            if (cl == null)
            {
                throw TypeResolver.reportProblem("No class reader for '%s'", internalName);
            }
        }
        catch (IOException e)
        {
            throw TypeResolver.reportProblem(e, "Failed to load type '%s'", type);
        }

        GenericTypeInfoImpl gti = new GenericTypeInfoImpl(this, enclosingType, type, innerClassName, cl.file);

        ClassVisitor cv = new ClassVisitor(Opcodes.ASM7)
        {
            @Override
            public void visit(int version,
                              int access,
                              String name,
                              String signature,
                              String superName,
                              String[] interfaces)
            {
                gti.setAccess(ClassAccess.fromValue(access));

                if (signature != null)
                {
                    gti.m_classSignature = parseGenericTypeDeclaration(signature, context);
                }
                else
                {
                    GenericType.TypeDeclaration td = new GenericType.TypeDeclaration();
                    td.outerContext = context;

                    gti.m_classSignature = td;

                    if (superName != null)
                    {
                        td.superclass = new GenericType.TypeReference(superName, null);
                    }

                    if (interfaces != null)
                    {
                        for (String itf : interfaces)
                            td.addInterface(new GenericType.TypeReference(itf, null));
                    }
                }
            }

            @Override
            public FieldVisitor visitField(int access,
                                           String name,
                                           String desc,
                                           String signature,
                                           Object value)
            {
                GenericType type;

                if (signature != null)
                {
                    type = parseGenericTypeReference(signature, gti.m_classSignature);
                }
                else
                {
                    type = parseGenericTypeReference(desc, null);
                }

                Set<FieldAccess> modifiers = FieldAccess.fromValue(access);

                gti.addField(new GenericFieldInfoImpl(gti, name, modifiers, type));

                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access,
                                             String name,
                                             String desc,
                                             String signature,
                                             String[] exceptions)
            {
                GenericType.MethodDescriptor type;

                if (signature != null)
                {
                    type = parseGenericMethodSignature(signature, gti.m_classSignature);
                }
                else
                {
                    type = parseGenericMethodSignature(desc, null);
                }

                Set<MethodAccess> modifiers = MethodAccess.fromValue(access);

                gti.addMethod(new GenericMethodInfoImpl(gti, name, modifiers, type));

                return null;
            }
        };

        cl.classReader.accept(cv, ClassReader.SKIP_CODE + ClassReader.SKIP_DEBUG + ClassReader.SKIP_FRAMES);

        return gti;
    }

    public GenericTypeInfo getGenericTypeInfo(Class<?> clz) throws
                                                            AnalyzerException
    {
        return getGenericTypeInfo(Type.getType(clz));
    }

    public GenericFieldInfo getGenericFieldInfo(Field f) throws
                                                         AnalyzerException
    {
        GenericTypeInfo gti = getGenericTypeInfo(f.getDeclaringClass());
        return gti.get(f);
    }

    public GenericMethodInfo getGenericConstructorInfo(Constructor<?> c) throws
                                                                         AnalyzerException
    {
        GenericTypeInfo td = getGenericTypeInfo(c.getDeclaringClass());
        return td.get(c);
    }

    public GenericMethodInfo getGenericMethodInfo(Method m) throws
                                                            AnalyzerException
    {
        GenericTypeInfo td = getGenericTypeInfo(m.getDeclaringClass());
        return td.get(m);
    }

    //--//

    public ClassLocator getClassReader(String internalClassName) throws
                                                                 IOException
    {
        BytecodeCache cache = getCache(m_loader);

        return cache.get(internalClassName);
    }

    public ClassLocator getClassReader(Class<?> clz) throws
                                                     IOException
    {
        String className = clz.getName();

        return getClassReader(className.replace('.', '/'));
    }

    //--//

    public static AnalyzerException reportProblem(Throwable e,
                                                  String format,
                                                  Object... args)
    {
        return new AnalyzerException(null, String.format(format, args), e);
    }

    public static AnalyzerException reportProblem(AbstractInsnNode context,
                                                  String format,
                                                  Object... args)
    {
        return new AnalyzerException(context, String.format(format, args));
    }

    public static AnalyzerException reportProblem(String format,
                                                  Object... args)
    {
        return reportProblem((AbstractInsnNode) null, format, args);
    }
}
