/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg;

import java.io.IOException;

import org.objectweb.asm.tree.analysis.AnalyzerException;

@FunctionalInterface
public interface CheckedPredicate<T>
{
    boolean test(T t) throws
                      AnalyzerException,
                      IOException;
}
