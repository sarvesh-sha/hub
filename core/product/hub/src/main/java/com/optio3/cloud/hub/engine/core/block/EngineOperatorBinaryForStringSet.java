/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Sets;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueList;
import com.optio3.cloud.hub.engine.core.value.EngineValueListConcrete;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.model.shared.program.CommonEngineSetOperation;
import com.optio3.util.CollectionUtils;

@JsonTypeName("EngineOperatorBinaryForStringSet")
public class EngineOperatorBinaryForStringSet extends EngineOperatorBinaryFromCore<EngineValueList<EngineValuePrimitiveString>, EngineValueList<EngineValuePrimitiveString>, EngineValueList<EngineValuePrimitiveString>>
{
    public CommonEngineSetOperation operation;

    public EngineOperatorBinaryForStringSet()
    {
        super(EngineValueList.typeRef_ListOfStrings);
    }

    @Override
    protected EngineValueList<EngineValuePrimitiveString> computeResult(EngineExecutionContext<?, ?> ctx,
                                                                        EngineExecutionStack stack,
                                                                        EngineValueList<EngineValuePrimitiveString> optionsA,
                                                                        EngineValueList<EngineValuePrimitiveString> optionsB)
    {
        Set<String> a = createSet(optionsA);
        Set<String> b = createSet(optionsB);

        switch (operation)
        {
            case Add:
                return add(a, b);

            case Subtract:
                return subtract(a, b);

            case Intersect:
                return intersect(a, b);

            default:
                throw stack.unexpected();
        }
    }

    private static Set<String> createSet(EngineValueList<EngineValuePrimitiveString> list)
    {
        Set<String> result = Sets.newHashSet();
        if (list != null)
        {
            for (int i = 0; i < list.getLength(); i++)
            {
                EngineValuePrimitiveString nthElement = list.getNthElement(i);
                if (nthElement != null)
                {
                    result.add(nthElement.value);
                }
            }
        }

        return result;
    }

    private static EngineValueList<EngineValuePrimitiveString> add(Set<String> a,
                                                                   Set<String> b)
    {
        EngineValueListConcrete<EngineValuePrimitiveString> list = new EngineValueListConcrete<>();

        Sets.SetView<String> diff = Sets.union(a, b);
        list.elements.addAll(CollectionUtils.transformToList(diff, EngineValuePrimitiveString::create));

        return list;
    }

    private static EngineValueList<EngineValuePrimitiveString> subtract(Set<String> a,
                                                                        Set<String> b)
    {
        EngineValueListConcrete<EngineValuePrimitiveString> list = new EngineValueListConcrete<>();

        Sets.SetView<String> diff = Sets.difference(a, b);
        list.elements.addAll(CollectionUtils.transformToList(diff, EngineValuePrimitiveString::create));

        return list;
    }

    private static EngineValueList<EngineValuePrimitiveString> intersect(Set<String> a,
                                                                         Set<String> b)
    {
        EngineValueListConcrete<EngineValuePrimitiveString> list = new EngineValueListConcrete<>();

        Sets.SetView<String> diff = Sets.intersection(a, b);
        list.elements.addAll(CollectionUtils.transformToList(diff, EngineValuePrimitiveString::create));

        return list;
    }
}
