/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis;

import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public class ClassAnalyzer
{
    public final GenericTypeInfo genericTypeInfo;

    public final AnnotationsAnalyzer annotations;

    public final List<FieldAnalyzer>  fields  = Lists.newArrayList();
    public final List<MethodAnalyzer> methods = Lists.newArrayList();

    public final  File      m_fileLocation;
    private final ClassNode m_classNode;
    private       boolean   m_modified;

    public ClassAnalyzer(TypeResolver typeResolver,
                         TypeResolver.ClassLocator cl) throws
                                                       AnalyzerException
    {
        if (typeResolver == null)
        {
            typeResolver = new TypeResolver(null, null);
        }

        //--//

        m_fileLocation = cl.file;

        m_classNode = new ClassNode(Opcodes.ASM7);
        cl.classReader.accept(m_classNode, 0);

        Type classType = Type.getObjectType(m_classNode.name);
        genericTypeInfo = typeResolver.getGenericTypeInfo(classType);

        annotations = new AnnotationsAnalyzer(typeResolver,
                                              this,
                                              null,
                                              m_classNode.visibleAnnotations,
                                              m_classNode.invisibleAnnotations,
                                              m_classNode.visibleTypeAnnotations,
                                              m_classNode.invisibleTypeAnnotations);

        for (FieldNode field : m_classNode.fields)
        {
            GenericType type;
            if (field.signature != null)
            {
                type = typeResolver.parseGenericTypeReference(field.signature, genericTypeInfo.getSignature());
            }
            else
            {
                type = typeResolver.parseGenericTypeReference(field.desc, null);
            }

            GenericFieldInfo gfi = genericTypeInfo.findField(field.name, false);
            if (gfi == null || !gfi.getType()
                                   .equals(type))
            {
                throw TypeResolver.reportProblem("INTERNAL ERROR: mismatch between ClassReader and GenericFieldInfo for %s.%s", genericTypeInfo, field.name);
            }

            AnnotationsAnalyzer annotations = new AnnotationsAnalyzer(typeResolver,
                                                                      this,
                                                                      genericTypeInfo.getSignature(),
                                                                      field.visibleAnnotations,
                                                                      field.invisibleAnnotations,
                                                                      field.visibleTypeAnnotations,
                                                                      field.invisibleTypeAnnotations);

            fields.add(new FieldAnalyzer(this, gfi, field, annotations));
        }

        for (MethodNode method : m_classNode.methods)
        {
            GenericType.MethodDescriptor signature;

            if (method.signature != null)
            {
                signature = typeResolver.parseGenericMethodSignature(method.signature, genericTypeInfo.getSignature());
            }
            else
            {
                signature = typeResolver.parseGenericMethodSignature(method.desc, null);
            }

            GenericMethodInfo gmi = genericTypeInfo.addMethod(null, method.name, signature);
            if (gmi == null)
            {
                throw TypeResolver.reportProblem("INTERNAL ERROR: mismatch between ClassReader and GenericMethodInfo for %s.%s%s", genericTypeInfo, method.name, signature);
            }

            AnnotationsAnalyzer annotations = new AnnotationsAnalyzer(typeResolver,
                                                                      this,
                                                                      signature,
                                                                      method.visibleAnnotations,
                                                                      method.invisibleAnnotations,
                                                                      method.visibleTypeAnnotations,
                                                                      method.invisibleTypeAnnotations);
            methods.add(new MethodAnalyzer(this, gmi, method, annotations));
        }
    }

    public ClassAnalyzer(GenericTypeInfo genericTypeInfo)
    {
        Preconditions.checkNotNull(genericTypeInfo);

        this.genericTypeInfo = genericTypeInfo;
        this.annotations = new AnnotationsAnalyzer(this);

        //--/

        m_fileLocation = genericTypeInfo.getFileLocation();
        m_classNode = new ClassNode(Opcodes.ASM7);

        m_classNode.name = getInternalName();
        m_classNode.access = ClassAccess.toValue(genericTypeInfo.getAccess());

        GenericType.TypeDeclaration signature = genericTypeInfo.getSignature();
        if (signature.isGeneric())
        {
            m_classNode.signature = signature.toString();
        }

        if (signature.superclass != null)
        {
            m_classNode.superName = signature.superclass.asInternalName();
        }

        for (GenericType itf : signature.interfaces)
        {
            m_classNode.interfaces.add(itf.asInternalName());
        }
    }

    public byte[] encode()
    {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES)
        {
            @Override
            protected String getCommonSuperClass(String type1,
                                                 String type2)
            {
                Type t1 = Type.getObjectType(type1);
                Type t2 = Type.getObjectType(type2);

                try
                {
                    Type t3 = genericTypeInfo.typeResolver.findCommonSuperclass(t1, t2);

                    return t3 != null ? t3.getInternalName() : null;
                }
                catch (AnalyzerException e)
                {
                    return null;
                }
            }
        };

        m_classNode.accept(cw);
        return cw.toByteArray();
    }

    public boolean wasModified()
    {
        return m_modified;
    }

    void setModified()
    {
        m_modified = true;
    }

    //--//

    public ClassAnalyzer addNestedType(Set<ClassAccess> access,
                                       String innerName,
                                       GenericType.TypeReference superclass) throws
                                                                             AnalyzerException
    {
        GenericTypeInfo newTypeInfo = genericTypeInfo.createNestedType(access, innerName, superclass);

        ClassAnalyzer ca = new ClassAnalyzer(newTypeInfo);

        ca.m_classNode.version = m_classNode.version;
        ca.m_classNode.sourceFile = m_classNode.sourceFile;

        addInnerClass(this, access, getInternalName(), innerName);
        addInnerClass(ca, access, getInternalName(), innerName);

        return ca;
    }

    private void addInnerClass(ClassAnalyzer ca,
                               Set<ClassAccess> access,
                               String outerName,
                               String innerName) throws
                                                 AnalyzerException
    {

        int pos = outerName.lastIndexOf('$');
        if (pos >= 0)
        {
            String outerName2 = outerName.substring(0, pos);
            String innerName2 = outerName.substring(pos + 1);

            GenericTypeInfo outerTypeInfo = genericTypeInfo.typeResolver.getGenericTypeInfo(Type.getObjectType(outerName));
            addInnerClass(ca, outerTypeInfo.getAccess(), outerName2, innerName2);
        }

        String name = outerName + "$" + innerName;

        for (InnerClassNode icn : ca.m_classNode.innerClasses)
        {
            if (icn.name.equals(name))
            {
                return;
            }
        }

        EnumSet<ClassAccess> innerAccess = EnumSet.of(ClassAccess.Public, ClassAccess.Private, ClassAccess.Protected, ClassAccess.Static);
        innerAccess.retainAll(access);

        InnerClassNode icn = new InnerClassNode(name, outerName, innerName, ClassAccess.toValue(innerAccess));
        ca.m_classNode.innerClasses.add(icn);
        ca.setModified();
    }

    //--//

    public FieldAnalyzer findField(String name)
    {
        return findField(name, (Type) null);
    }

    public FieldAnalyzer findField(String name,
                                   GenericType signature)
    {
        for (FieldAnalyzer fa : fields)
        {
            if (name != null && !name.equals(fa.getName()))
            {
                continue;
            }

            if (signature != null && !signature.equalsRawTypes(fa.getType()))
            {
                continue;
            }

            return fa;
        }

        return null;
    }

    public FieldAnalyzer findField(String name,
                                   Type signature)
    {
        for (FieldAnalyzer fa : fields)
        {
            if (name != null && !name.equals(fa.getName()))
            {
                continue;
            }

            if (signature != null && !fa.getType()
                                        .equals(signature))
            {
                continue;
            }

            return fa;
        }

        return null;
    }

    public FieldAnalyzer addField(String name,
                                  GenericType type) throws
                                                    AnalyzerException
    {
        return addField(null, name, type);
    }

    public FieldAnalyzer addField(EnumSet<FieldAccess> access,
                                  String name,
                                  GenericType type) throws
                                                    AnalyzerException
    {
        if (access == null)
        {
            access = EnumSet.of(FieldAccess.Private);
        }

        FieldAnalyzer fa = findField(name);
        if (fa != null)
        {
            GenericType sig = fa.getType();
            if (sig.equalsRawTypes(type))
            {
                return fa;
            }

            throw TypeResolver.reportProblem("Field '%s' already exists with incompatible type: '%s' vs. new '%s'", name, sig, type);
        }

        //
        // Update GenericTypeNode state.
        //
        GenericFieldInfo gfi = genericTypeInfo.addField(access, name, type);

        //
        // Create FieldNode and update CloassNode state.
        //
        String desc = type.asRawType()
                          .getDescriptor();
        String sig = type.isGeneric() ? type.toString() : null;

        FieldNode fn = new FieldNode(FieldAccess.toValue(access), name, desc, sig, null);
        m_classNode.fields.add(fn);

        //
        // Create FieldAnalyzer and update ClassAnalyzer state.
        //
        FieldAnalyzer res = new FieldAnalyzer(this, gfi, fn, new AnnotationsAnalyzer(this));
        fields.add(res);
        res.setModified();

        return res;
    }

    //--//

    public MethodAnalyzer findMethod(String name)
    {
        return findMethod(name, (Type) null);
    }

    public MethodAnalyzer findMethod(GenericMethodInfo m)
    {
        return findMethod(m.getName(), m.getSignature());
    }

    public MethodAnalyzer findMethod(String name,
                                     GenericType.MethodDescriptor signature)
    {
        for (MethodAnalyzer ma : methods)
        {
            if (name != null && !name.equals(ma.getName()))
            {
                continue;
            }

            if (signature != null && !ma.getSignature()
                                        .equalsRawTypes(signature))
            {
                continue;
            }

            return ma;
        }

        return null;
    }

    public MethodAnalyzer findMethod(String name,
                                     Type signature)
    {
        for (MethodAnalyzer ma : methods)
        {
            if (name != null && !name.equals(ma.getName()))
            {
                continue;
            }

            if (signature != null && !ma.getSignature()
                                        .equals(signature))
            {
                continue;
            }

            return ma;
        }

        return null;
    }

    public MethodAnalyzer overrideMethod(GenericMethodInfo m,
                                         MethodAccess... accesses) throws
                                                                   AnalyzerException
    {
        String                       name      = m.getName();
        GenericType.MethodDescriptor signature = m.getSignature();

        EnumSet<MethodAccess> newAccess;

        if (accesses != null && accesses.length > 0)
        {
            newAccess = EnumSet.noneOf(MethodAccess.class);
            for (MethodAccess acc : accesses)
                newAccess.add(acc);
        }
        else
        {
            newAccess = EnumSet.copyOf(m.getModifiers());
            newAccess.remove(MethodAccess.Abstract);
            newAccess.add(MethodAccess.Final);
            newAccess.add(MethodAccess.Synthetic);
        }

        return addMethod(name,
                         signature,
                         newAccess.stream()
                                  .toArray((s) -> new MethodAccess[s]));
    }

    public MethodAnalyzer addMethod(String name,
                                    GenericType.MethodDescriptor signature,
                                    MethodAccess... accesses) throws
                                                              AnalyzerException
    {
        MethodAnalyzer ma = findMethod(name, signature);
        if (ma == null)
        {
            EnumSet<MethodAccess> newAccess;

            newAccess = EnumSet.noneOf(MethodAccess.class);
            for (MethodAccess acc : accesses)
                newAccess.add(acc);

            //
            // Update GenericTypeNode state.
            //
            GenericMethodInfo gmi = genericTypeInfo.addMethod(newAccess, name, signature);

            //
            // Create MethodNode and update CloassNode state.
            //
            String desc = signature.asRawType()
                                   .getDescriptor();
            String   sig = signature.isGeneric() ? signature.toString() : null;
            String[] exceptions;

            int numExceptions = signature.exceptionTypes.size();
            if (numExceptions > 0)
            {
                exceptions = new String[numExceptions];
                for (int i = 0; i < numExceptions; i++)
                    exceptions[i] = signature.exceptionTypes.get(i)
                                                            .asRawType()
                                                            .toString();
            }
            else
            {
                exceptions = null;
            }

            MethodNode mn = new MethodNode(Opcodes.ASM7, MethodAccess.toValue(gmi.getModifiers()), gmi.getName(), desc, sig, exceptions);
            m_classNode.methods.add(mn);

            //
            // Create MethodAnalyzer and update ClassAnalyzer state.
            //
            ma = new MethodAnalyzer(this, gmi, mn, new AnnotationsAnalyzer(this));
            methods.add(ma);
            ma.setModified();
        }

        return ma;
    }

    //--//

    public String getInternalName()
    {
        return genericTypeInfo.asType()
                              .getInternalName();
    }

    public ClassNode getClassNode()
    {
        return m_classNode;
    }

    //--//

    @Override
    public String toString()
    {
        return m_classNode.name;
    }
}
