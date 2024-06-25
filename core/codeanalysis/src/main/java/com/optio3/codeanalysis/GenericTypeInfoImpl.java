/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.optio3.codeanalysis.GenericType.MethodDescriptor;
import com.optio3.codeanalysis.GenericType.TypeReference;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.AnalyzerException;

class GenericTypeInfoImpl extends GenericTypeInfo
{
    private final GenericTypeInfo m_outerType;
    private final Type            m_type;
    private final String          m_name;
    private final File            m_fileLocation;

    private Set<ClassAccess> m_access;
    GenericType.TypeDeclaration m_classSignature;

    private final List<GenericFieldInfoImpl>  m_fields  = Lists.newArrayList();
    private final List<GenericMethodInfoImpl> m_methods = Lists.newArrayList();

    GenericTypeInfoImpl(TypeResolver typeResolver,
                        GenericTypeInfo outerType,
                        Type type,
                        String name,
                        File fileLocation)
    {
        super(typeResolver);

        m_outerType = outerType;
        m_type = type;
        m_name = name;
        m_fileLocation = fileLocation;
    }

    @Override
    public File getFileLocation()
    {
        return m_fileLocation;
    }

    @Override
    public GenericTypeInfo getEnclosingGenericType()
    {
        return m_outerType;
    }

    @Override
    public GenericTypeInfo getGenericSuperType() throws
                                                 AnalyzerException
    {
        TypeReference superRef = getSuperclass();
        if (superRef == null)
        {
            return null;
        }

        return typeResolver.getGenericTypeInfo(superRef.asRawType());
    }

    @Override
    public String getInnerName()
    {
        return m_name;
    }

    @Override
    public Type asType()
    {
        return m_type;
    }

    @Override
    public Set<ClassAccess> getAccess()
    {
        return m_access;
    }

    @Override
    public GenericType.TypeDeclaration getSignature()
    {
        return m_classSignature;
    }

    @Override
    public GenericType.TypeReference getSuperclass()
    {
        return m_classSignature.superclass;
    }

    @Override
    public List<GenericType> getInterfaces()
    {
        return Collections.unmodifiableList(m_classSignature.interfaces);
    }

    @Override
    public List<GenericFieldInfo> getFields()
    {
        return Collections.unmodifiableList(m_fields);
    }

    @Override
    public List<GenericMethodInfo> getMethods()
    {
        return Collections.unmodifiableList(m_methods);
    }

    @Override
    GenericTypeInfo createNestedType(Set<ClassAccess> access,
                                     String name,
                                     TypeReference superclass) throws
                                                               AnalyzerException
    {
        Type newType = Type.getObjectType(asType().getInternalName() + "$" + name);

        GenericTypeInfoImpl newTypeInfo = new GenericTypeInfoImpl(typeResolver, this, newType, name, m_fileLocation);
        newTypeInfo.setAccess(access);

        GenericType.TypeDeclaration td = new GenericType.TypeDeclaration();
        td.superclass = superclass;
        td.outerContext = m_classSignature;

        newTypeInfo.m_classSignature = td;

        typeResolver.putGenericTypeInfo(newTypeInfo);

        return newTypeInfo;
    }

    @Override
    GenericFieldInfo addField(Set<FieldAccess> access,
                              String name,
                              GenericType signature) throws
                                                     AnalyzerException
    {
        GenericFieldInfo fi = findField(name, true);
        if (fi != null)
        {
            if (GenericType.equals(fi.getType(), signature))
            {
                return fi;
            }

            throw TypeResolver.reportProblem("Field '%s' already exists with incompatible type: '%s' vs. new '%s'", name, fi.getType(), signature);
        }

        GenericFieldInfoImpl fiImpl = new GenericFieldInfoImpl(this, name, access, signature);
        m_fields.add(fiImpl);
        return fiImpl;
    }

    @Override
    GenericMethodInfo addMethod(Set<MethodAccess> access,
                                String name,
                                MethodDescriptor signature) throws
                                                            AnalyzerException
    {
        GenericMethodInfo mi = findMethod(name, signature, false);
        if (mi != null)
        {
            if (GenericType.equals(mi.getSignature(), signature))
            {
                return mi;
            }

            throw TypeResolver.reportProblem("Field '%s' already exists with incompatible type: '%s' vs. new '%s'", name, mi.getSignature(), signature);
        }

        GenericMethodInfoImpl miImpl = new GenericMethodInfoImpl(this, name, access, signature);
        m_methods.add(miImpl);
        return miImpl;
    }

    //--//

    void addField(GenericFieldInfoImpl fi)
    {
        m_fields.add(fi);
    }

    void addMethod(GenericMethodInfoImpl mi)
    {
        m_methods.add(mi);
    }

    void setAccess(Set<ClassAccess> access)
    {
        m_access = Collections.unmodifiableSet(access);
    }
}
