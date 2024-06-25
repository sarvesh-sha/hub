/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.value;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Sets;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.model.DeliveryOptions;

@JsonTypeName("AlertEngineValueDeliveryOptions")
public class AlertEngineValueDeliveryOptions extends EngineValue
{
    public Set<String> resolvedUsers;

    public static AlertEngineValueDeliveryOptions create(EngineExecutionContext<?, ?> ctx,
                                                         DeliveryOptions val)
    {
        if (val == null)
        {
            return null;
        }

        AlertEngineValueDeliveryOptions res = new AlertEngineValueDeliveryOptions();
        res.resolvedUsers = ctx.deliveryOptionsResolver.resolve(val);
        return res;
    }

    //--//

    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        throw stack.unexpected();
    }

    @Override
    public String format(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         Map<String, String> modifiers)
    {
        return null;
    }

    public static AlertEngineValueDeliveryOptions add(AlertEngineValueDeliveryOptions a,
                                                      AlertEngineValueDeliveryOptions b)
    {
        AlertEngineValueDeliveryOptions c = new AlertEngineValueDeliveryOptions();
        c.resolvedUsers = Sets.newHashSet();

        if (a != null)
        {
            c.resolvedUsers.addAll(a.resolvedUsers);
        }

        if (b != null)
        {
            c.resolvedUsers.addAll(b.resolvedUsers);
        }

        return c;
    }

    public static AlertEngineValueDeliveryOptions subtract(AlertEngineValueDeliveryOptions a,
                                                           AlertEngineValueDeliveryOptions b)
    {
        AlertEngineValueDeliveryOptions c = new AlertEngineValueDeliveryOptions();
        c.resolvedUsers = Sets.newHashSet();

        if (a != null)
        {
            c.resolvedUsers.addAll(a.resolvedUsers);
        }

        if (b != null)
        {
            c.resolvedUsers.removeAll(b.resolvedUsers);
        }

        return c;
    }

    public static AlertEngineValueDeliveryOptions intersect(AlertEngineValueDeliveryOptions a,
                                                            AlertEngineValueDeliveryOptions b)
    {
        AlertEngineValueDeliveryOptions c = new AlertEngineValueDeliveryOptions();

        if (a != null && b != null)
        {
            c.resolvedUsers = Sets.intersection(a.resolvedUsers, b.resolvedUsers);
        }
        else
        {
            c.resolvedUsers = Sets.newHashSet();
        }

        return c;
    }
}
