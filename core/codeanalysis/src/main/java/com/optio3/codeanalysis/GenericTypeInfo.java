/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.optio3.codeanalysis.GenericType.TypeArgument;
import com.optio3.codeanalysis.GenericType.TypeReference;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public abstract class GenericTypeInfo extends GenericMemberInfo
{
    protected GenericTypeInfo(TypeResolver typeResolver)
    {
        super(typeResolver);
    }

    //--//

    public abstract File getFileLocation();

    public abstract GenericTypeInfo getEnclosingGenericType();

    public abstract GenericTypeInfo getGenericSuperType() throws
                                                          AnalyzerException;

    public abstract String getInnerName();

    public abstract Type asType();

    public abstract Set<ClassAccess> getAccess();

    public abstract GenericType.TypeDeclaration getSignature();

    public abstract GenericType.TypeReference getSuperclass();

    public abstract List<GenericType> getInterfaces();

    public abstract List<GenericFieldInfo> getFields();

    public abstract List<GenericMethodInfo> getMethods();

    //--//

    abstract GenericTypeInfo createNestedType(Set<ClassAccess> access,
                                              String name,
                                              TypeReference superclass) throws
                                                                        AnalyzerException;

    abstract GenericFieldInfo addField(Set<FieldAccess> access,
                                       String name,
                                       GenericType type) throws
                                                         AnalyzerException;

    abstract GenericMethodInfo addMethod(Set<MethodAccess> access,
                                         String name,
                                         GenericType.MethodDescriptor signature) throws
                                                                                 AnalyzerException;

    //--//

    public GenericFieldInfo findField(String name,
                                      boolean includeParentFields) throws
                                                                   AnalyzerException
    {
        for (GenericFieldInfo fi : getFields())
        {
            if (fi.getName()
                  .equals(name))
            {
                return fi;
            }
        }

        if (includeParentFields)
        {
            GenericTypeInfo tiSuper = getGenericSuperType();
            if (tiSuper != null)
            {
                return tiSuper.findField(name, true);
            }
        }

        return null;
    }

    public GenericMethodInfo findMethod(String name,
                                        GenericType.MethodDescriptor signature,
                                        boolean includeParentFields) throws
                                                                     AnalyzerException
    {
        for (GenericMethodInfo mi : getMethods())
        {
            if (!mi.getName()
                   .equals(name))
            {
                continue;
            }

            if (!mi.getSignature()
                   .equalsRawTypes(signature))
            {
                continue;
            }

            return mi;
        }

        if (includeParentFields)
        {
            GenericTypeInfo tiSuper = getGenericSuperType();
            if (tiSuper != null)
            {
                return tiSuper.findMethod(name, signature, true);
            }
        }

        return null;
    }

    //--//

    public GenericType.TypeReference createReferenceForSubclass(TypeArgument... typeArgs) throws
                                                                                          AnalyzerException
    {
        List<TypeArgument> list = Lists.newArrayList(typeArgs);

        return createReferenceForSubclassInner(list, typeArgs);
    }

    public GenericType.TypeReference createReferenceForSubclass(GenericType... typeArgs) throws
                                                                                         AnalyzerException
    {
        List<TypeArgument> list = Lists.newArrayList();

        for (GenericType t : typeArgs)
        {
            list.add(new GenericType.TypeArgument(t));
        }

        return createReferenceForSubclassInner(list, typeArgs);
    }

    private GenericType.TypeReference createReferenceForSubclassInner(List<TypeArgument> list,
                                                                      GenericType... typeArgs) throws
                                                                                               AnalyzerException
    {
        GenericType.TypeReference res = createReferenceForSubclassRecurse(list);

        if (!list.isEmpty())
        {
            throw TypeResolver.reportProblem("Incorrect number of type arguments, expecting %d, got %d", typeArgs.length - list.size(), typeArgs.length);
        }

        return res;
    }

    private GenericType.TypeReference createReferenceForSubclassRecurse(List<GenericType.TypeArgument> typeArgs) throws
                                                                                                                 AnalyzerException
    {
        GenericTypeInfo           outerType = getEnclosingGenericType();
        GenericType.TypeReference outerRef;
        GenericType.TypeReference ref;

        if (outerType != null)
        {
            outerRef = outerType.createReferenceForSubclassRecurse(typeArgs);
        }
        else
        {
            outerRef = null;
        }

        ref = new GenericType.TypeReference(getInnerName(), outerRef);

        int requiredNumberOfParams = getSignature().formalParameters.size();
        int gotNumberOfParams      = typeArgs.size();
        if (gotNumberOfParams < requiredNumberOfParams)
        {
            throw TypeResolver.reportProblem("Incorrect number of type arguments, expecting %d, got %d", requiredNumberOfParams, gotNumberOfParams);
        }

        while (requiredNumberOfParams-- > 0)
        {
            ref.addTypeArgument(typeArgs.get(0));

            typeArgs.remove(0);
        }

        return ref;
    }

    //--//

    public GenericFieldInfo get(Field f)
    {
        String  fieldName = f.getName();
        boolean isStatic  = Modifier.isStatic(f.getModifiers());

        for (GenericFieldInfo gfi : getFields())
        {
            if (!gfi.getName()
                    .equals(fieldName))
            {
                continue;
            }

            if (gfi.isStatic() != isStatic)
            {
                continue;
            }

            return gfi;
        }

        return null;
    }

    public GenericMethodInfo get(Constructor<?> c)
    {
        return getInner("<init>", false, Type.getConstructorDescriptor(c));
    }

    public GenericMethodInfo get(Method m)
    {
        return getInner(m.getName(), Modifier.isStatic(m.getModifiers()), Type.getMethodDescriptor(m));
    }

    private GenericMethodInfo getInner(String methodName,
                                       boolean isStatic,
                                       String descriptor)
    {
        Type signature = Type.getMethodType(descriptor);

        for (GenericMethodInfo gmi : getMethods())
        {
            if (!gmi.getName()
                    .equals(methodName))
            {
                continue;
            }

            if (gmi.isStatic() != isStatic)
            {
                continue;
            }

            if (!gmi.getSignature()
                    .equals(signature))
            {
                continue;
            }

            return gmi;
        }

        return null;
    }

    //--//

    @Override
    public String toString()
    {
        return asType().toString();
    }
}
