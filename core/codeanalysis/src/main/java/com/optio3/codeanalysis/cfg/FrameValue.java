/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg;

import java.util.Collections;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.instruction.NewObjectInstruction;
import com.optio3.serialization.Reflection;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public class FrameValue
{
    public FrameValue coerceToInt()
    {
        Type type        = getType();
        Type typeCoerced = TypeResolver.coerceToInt(type);

        if (type != typeCoerced)
        {
            //
            // Extend values to INT when pushing into the stack.
            //
            return create(Type.INT_TYPE, this);
        }

        return this;
    }

    //--//

    private final Type m_type;

    private boolean                  m_uninitialized;
    private AbstractInstruction      m_singleGenerator;
    private Set<AbstractInstruction> m_multipleGenerators;

    private FrameValue(Type type)
    {
        Preconditions.checkNotNull(type);

        m_type = type;
    }

    public static FrameValue create(Type type,
                                    FrameValue forwardedFrom)
    {
        FrameValue res = new FrameValue(type);

        if (forwardedFrom.m_multipleGenerators != null)
        {
            //
            // Need to make a copy of the set.
            //
            res.m_multipleGenerators = Sets.newHashSet(forwardedFrom.m_multipleGenerators);
        }
        else
        {
            res.m_singleGenerator = forwardedFrom.m_singleGenerator;
        }

        return res;
    }

    public static FrameValue create(Type type,
                                    AbstractInstruction generator)
    {
        Preconditions.checkNotNull(generator);

        FrameValue res = new FrameValue(type);
        res.m_singleGenerator = generator;
        res.m_uninitialized = (generator instanceof NewObjectInstruction);
        return res;
    }

    public static FrameValue createUninitialized(Type type)
    {
        FrameValue res = new FrameValue(type);
        res.m_uninitialized = true;
        return res;
    }

    public static FrameValue createWithUnknownOrigin(Type type)
    {
        FrameValue res = new FrameValue(type);
        return res;
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        FrameValue that = Reflection.as(o, FrameValue.class);
        if (that == null)
        {
            return false;
        }

        return equals(that);
    }

    public boolean equals(FrameValue other)
    {
        if (this == other)
        {
            return true;
        }

        if (!m_type.equals(other.getType()))
        {
            return false;
        }

        boolean thisHasSingleGen  = this.hasSingleGenerator();
        boolean otherHasSingleGen = other.hasSingleGenerator();

        if (thisHasSingleGen && otherHasSingleGen)
        {
            return m_singleGenerator == other.m_singleGenerator;
        }

        return Sets.symmetricDifference(getGenerators(), other.getGenerators())
                   .isEmpty();
    }

    //--//

    public int getSize()
    {
        if (isUninitialized())
        {
            return 1; // Uninitialized are always object references.
        }

        return m_type.getSize();
    }

    public Type getType()
    {
        return m_type;
    }

    //--//

    public boolean isNull()
    {
        return m_type == TypeResolver.TypeForNull;
    }

    public boolean isUninitialized()
    {
        return m_uninitialized;
    }

    public boolean hasSingleGenerator()
    {
        return m_multipleGenerators == null || m_multipleGenerators.size() == 1;
    }

    public AbstractInstruction getSingleGenerator() throws
                                                    AnalyzerException
    {
        if (m_multipleGenerators != null)
        {
            if (m_multipleGenerators.size() != 1)
            {
                throw TypeResolver.reportProblem("Frame value has multiple generators");
            }
        }

        return m_singleGenerator;
    }

    public Set<AbstractInstruction> getGenerators()
    {
        if (m_multipleGenerators == null)
        {
            m_multipleGenerators = Sets.newHashSet();
            m_multipleGenerators.add(m_singleGenerator);
        }

        return Collections.unmodifiableSet(m_multipleGenerators);
    }

    private void copyGenerators(Set<AbstractInstruction> set)
    {
        if (m_multipleGenerators != null)
        {
            set.addAll(m_multipleGenerators);
        }
        else
        {
            set.add(m_singleGenerator);
        }
    }

    //--//

    public FrameValue merge(TypeResolver typeResolver,
                            FrameValue other) throws
                                              AnalyzerException
    {
        if (this == other)
        {
            return this;
        }

        if (this.isUninitialized() != other.isUninitialized())
        {
            throw TypeResolver.reportProblem("Multiple incompatible paths (one initialized, the other un-initialized) lead to same frame value: %s <-> %s", this, other);
        }

        Type mergedType = merge(typeResolver, getType(), other.getType());
        if (mergedType == null)
        {
            return null;
        }

        FrameValue res = new FrameValue(mergedType);
        res.m_uninitialized = this.isUninitialized();

        Set<AbstractInstruction> set = Sets.newHashSet();

        copyGenerators(set);
        other.copyGenerators(set);

        if (set.size() == 1)
        {
            res.m_singleGenerator = set.iterator()
                                       .next();
        }
        else
        {
            res.m_multipleGenerators = set;
        }

        return res.equals(this) ? this : res;
    }

    private static Type merge(TypeResolver typeResolver,
                              Type leftType,
                              Type rightType) throws
                                              AnalyzerException
    {
        if (leftType.equals(rightType))
        {
            return leftType;
        }

        if (TypeResolver.isReference(leftType) && TypeResolver.isReference(rightType))
        {
            Type commonType = typeResolver.findCommonSuperclass(leftType, rightType);
            if (commonType != null)
            {
                return commonType;
            }
        }

        return null;
    }

    //--//

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getType());

        boolean emitted = false;

        for (AbstractInstruction origin : getGenerators())
        {
            if (emitted)
            {
                sb.append(", ");
            }
            else
            {
                sb.append(" generators: {");
                emitted = true;
            }

            sb.append(origin);
        }

        if (emitted)
        {
            sb.append("}");
        }

        return sb.toString();
    }

    public static void dumpType(StringBuilder sb,
                                FrameValue v)
    {
        if (v == null)
        {
            sb.append(" .");
        }
        else if (v.isUninitialized())
        {
            sb.append(" U");
        }
        else
        {
            switch (v.m_type.getSort())
            {
                case Type.ARRAY:
                    sb.append(" A");
                    break;

                case Type.OBJECT:
                case Type.METHOD:
                    sb.append(" R");
                    break;

                default:
                    sb.append(" ");
                    sb.append(v.m_type);
                    break;
            }
        }
    }
}
