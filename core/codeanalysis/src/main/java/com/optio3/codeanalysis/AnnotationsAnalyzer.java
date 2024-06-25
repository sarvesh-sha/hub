/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

public class AnnotationsAnalyzer
{
    public final ClassAnalyzer declaringClass;

    public List<AnnotationAnalyzer> visibleAnnotations;
    public List<AnnotationAnalyzer> invisibleAnnotations;

    public List<AnnotationAnalyzer> visibleTypeAnnotations;
    public List<AnnotationAnalyzer> invisibleTypeAnnotations;

    public AnnotationsAnalyzer(ClassAnalyzer ca)
    {
        this.declaringClass = ca;
    }

    public AnnotationsAnalyzer(TypeResolver typeResolver,
                               ClassAnalyzer ca,
                               GenericType.GenericMethodOrType context,
                               List<AnnotationNode> visibleAnnotations,
                               List<AnnotationNode> invisibleAnnotations,
                               List<TypeAnnotationNode> visibleTypeAnnotations,
                               List<TypeAnnotationNode> invisibleTypeAnnotations)

    {
        this.declaringClass = ca;

        this.visibleAnnotations = parseAnnotation(typeResolver, context, visibleAnnotations);
        this.invisibleAnnotations = parseAnnotation(typeResolver, context, invisibleAnnotations);

        this.visibleTypeAnnotations = parseAnnotation(typeResolver, context, visibleTypeAnnotations);
        this.invisibleTypeAnnotations = parseAnnotation(typeResolver, context, invisibleTypeAnnotations);
    }

    //--//

    private <T extends AnnotationNode> List<AnnotationAnalyzer> parseAnnotation(TypeResolver typeResolver,
                                                                                GenericType.GenericMethodOrType context,
                                                                                List<T> lst)
    {
        if (lst == null)
        {
            return Collections.emptyList();
        }

        List<AnnotationAnalyzer> res = Lists.newArrayList();

        for (AnnotationNode anno : lst)
        {
            GenericType type = typeResolver.parseGenericTypeReference(anno.desc, context);

            res.add(new AnnotationAnalyzer(declaringClass, context, type, anno));
        }

        return res;
    }
}
