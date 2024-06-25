/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import com.optio3.codeanalysis.cfg.TypeVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public class DependencyAnalyzer
{
    public final ClassAnalyzer declaringClass;

    private TypeResolver m_typeResolver;

    public Map<Type, GenericTypeInfo> seen     = Maps.newHashMap();
    public List<Exception>            failures = Lists.newArrayList();

    public DependencyAnalyzer(ClassAnalyzer ca) throws
                                                AnalyzerException
    {
        declaringClass = ca;

        GenericTypeInfo gti = ca.genericTypeInfo;
        m_typeResolver = gti.typeResolver;

        analyze(gti.getGenericSuperType());

        for (GenericType gtiInterface : gti.getInterfaces())
        {
            analyze(gtiInterface);
        }

        for (FieldAnalyzer field : ca.fields)
        {
            analyze(field.getType());
        }

        for (MethodAnalyzer method : ca.methods)
        {
            GenericType.MethodDescriptor sig = method.getSignature();
            analyze(sig.getRawReturnType());
            for (Type rawParameterType : sig.getRawParameterTypes())
            {
                analyze(rawParameterType);
            }

            if (method.hasCode())
            {
                ControlFlowGraph cfg = method.parseControlFlowGraph(m_typeResolver.logger);

                TypeVisitor typeVisitor = new TypeVisitor()
                {

                    @Override
                    public void visitType(Type type)
                    {
                        analyze(type);
                    }
                };

                cfg.accept(typeVisitor);
            }
        }
    }

    public Multimap<File, GenericTypeInfo> generateSummary()
    {
        Multimap<File, GenericTypeInfo> lookup = ArrayListMultimap.create();

        final File source = declaringClass.genericTypeInfo.getFileLocation();

        for (GenericTypeInfo value : seen.values())
        {
            File file = value.getFileLocation();
            if (file != null && !file.equals(source))
            {
                lookup.put(file, value);
            }
        }

        return lookup;
    }

    private void analyze(GenericTypeInfo gti)
    {
        analyze(gti.asType());
    }

    private void analyze(GenericType genericType)
    {
        analyze(genericType.asRawType());
    }

    private void analyze(Type type)
    {
        while (true)
        {
            if (type == null || TypeResolver.isNull(type) || type.equals(TypeResolver.TypeForObject))
            {
                return;
            }

            if (TypeResolver.isArray(type))
            {
                type = TypeResolver.getElementType(type);
                continue;
            }

            if (TypeResolver.isObject(type))
            {
                if (!seen.containsKey(type))
                {
                    try
                    {
                        GenericTypeInfo gti = m_typeResolver.getGenericTypeInfo(type);

                        seen.put(type, gti);

                        GenericType.TypeDeclaration sig = gti.getSignature();
                        for (GenericType.FormalParameter formalParameter : sig.formalParameters)
                        {
                            analyze(formalParameter);
                        }
                    }
                    catch (AnalyzerException e)
                    {
                        failures.add(e);
                    }
                }
            }

            break;
        }
    }
}