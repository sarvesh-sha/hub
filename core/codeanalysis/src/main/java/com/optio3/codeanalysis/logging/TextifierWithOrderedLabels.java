/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.logging;

import java.util.ListIterator;
import java.util.Map;

import com.google.common.collect.Maps;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Textifier;

public class TextifierWithOrderedLabels extends Textifier
{
    private final Map<String, MethodNode> m_methodLookup = Maps.newHashMap();

    private MethodNode m_currentContext;

    private TextifierWithOrderedLabels()
    {
        super(Opcodes.ASM7);
    }

    public TextifierWithOrderedLabels(ClassNode cn)
    {
        super(Opcodes.ASM7);

        for (MethodNode mn : cn.methods)
        {
            m_methodLookup.put(mn.name, mn);
        }
    }

    public TextifierWithOrderedLabels(MethodNode mn)
    {
        super(Opcodes.ASM7);

        setLabels(mn);
    }

    //--//

    @Override
    public Textifier visitMethod(int access,
                                 String name,
                                 String descriptor,
                                 String signature,
                                 String[] exceptions)
    {
        m_currentContext = m_methodLookup.get(name);

        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    protected Textifier createTextifier()
    {
        TextifierWithOrderedLabels res = new TextifierWithOrderedLabels();

        if (m_currentContext != null)
        {
            res.setLabels(m_currentContext);
        }

        return res;
    }

    private void setLabels(MethodNode mn)
    {
        labelNames = Maps.newHashMap();

        for (ListIterator<AbstractInsnNode> it = mn.instructions.iterator(); it.hasNext(); )
        {
            AbstractInsnNode insn = it.next();
            if (insn instanceof LabelNode)
            {
                LabelNode labelNode = (LabelNode) insn;

                labelNames.put(labelNode.getLabel(), "L" + labelNames.size());
            }
        }
    }
}
