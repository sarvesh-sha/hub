/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import java.net.UnknownHostException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveBoolean;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;
import com.optio3.infra.NetworkHelper;
import com.optio3.protocol.model.config.FilteredSubnet;
import com.optio3.protocol.model.transport.UdpTransportAddress;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;

@JsonTypeName("NormalizationEngineExpressionGetControllerInSubnet")
public class NormalizationEngineExpressionGetControllerInSubnet extends EngineExpressionFromNormalization<EngineValuePrimitiveBoolean>
{
    public List<FilteredSubnet> subnets = Lists.newArrayList();

    public NormalizationEngineExpressionGetControllerInSubnet()
    {
        super(EngineValuePrimitiveBoolean.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        NormalizationEngineExecutionContext ctx2 = (NormalizationEngineExecutionContext) ctx;

        boolean result = false;

        UdpTransportAddress controllerAddress = Reflection.as(ctx2.state.controllerTransportAddress, UdpTransportAddress.class);

        if (controllerAddress != null)
        {
            for (FilteredSubnet subnet : subnets)
            {
                try
                {
                    NetworkHelper.InetAddressWithPrefix parsedSubnet = NetworkHelper.InetAddressWithPrefix.parse(subnet.cidr);
                    if (parsedSubnet.isInSubnet(controllerAddress.socketAddress.getAddress()))
                    {
                        result = true;
                        break;
                    }
                }
                catch (UnknownHostException t)
                {
                    throw Exceptions.newRuntimeException("Failed to determine host for %s", subnet.cidr);
                }
            }
        }

        ctx.popBlock(EngineValuePrimitiveBoolean.create(result));
    }
}
