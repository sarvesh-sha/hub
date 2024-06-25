/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.gateway.remoting.impl;

import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.optio3.cloud.annotation.Optio3RemotableEndpoint;
import com.optio3.cloud.client.gateway.model.LogEntry;
import com.optio3.cloud.client.gateway.model.prober.ProberNetworkStatus;
import com.optio3.cloud.client.gateway.model.prober.ProberOperation;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationForBACnet;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationForCANbus;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationForIpn;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationStatus;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationToken;
import com.optio3.cloud.client.gateway.proxy.GatewayProberControlApi;
import com.optio3.cloud.gateway.logic.ProberForBACnet;
import com.optio3.cloud.gateway.logic.ProberForCANbus;
import com.optio3.cloud.gateway.logic.ProberForIpn;
import com.optio3.cloud.gateway.logic.ProberOperationTracker;
import com.optio3.infra.NetworkHelper;
import com.optio3.logging.LoggerConfiguration;
import com.optio3.logging.LoggerFactory;
import com.optio3.logging.Severity;
import com.optio3.protocol.bacnet.BACnetManager;
import com.optio3.protocol.bacnet.DeviceIdentity;
import com.optio3.protocol.can.CanManager;
import com.optio3.protocol.ipn.IpnManager;
import com.optio3.protocol.modbus.ModbusManager;
import com.optio3.protocol.obdii.J1939Manager;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import com.optio3.util.function.FunctionWithException;

@Optio3RemotableEndpoint(itf = GatewayProberControlApi.class)
public class GatewayProberControlApiImpl extends CommonGatewayApiImpl implements GatewayProberControlApi
{
    @Override
    public CompletableFuture<ProberNetworkStatus> checkNetwork() throws
                                                                 Exception
    {
        ProberNetworkStatus res = new ProberNetworkStatus();

        for (NetworkHelper.InterfaceAddressDetails itfDetails : NetworkHelper.listNetworkAddresses(false, false, false, true, null))
        {
            res.networkInterfaces.put(itfDetails.networkInterface.getName(), itfDetails.cidr.toString());
        }

        return wrapAsync(res);
    }

    @Override
    public CompletableFuture<ProberOperationToken> executeOperation(ProberOperation input) throws
                                                                                           Exception
    {
        return getTrackerForProber().trackOperation(input, (operationHolder) -> executeInner(input));
    }

    @Override
    public CompletableFuture<ProberOperationStatus> checkOperation(ProberOperationToken token,
                                                                   FunctionWithException<List<LogEntry>, CompletableFuture<Void>> output) throws
                                                                                                                                          Exception
    {
        return getTrackerForProber().checkOperationStatus(token, output);
    }

    @Override
    public CompletableFuture<Void> cancelOperation(ProberOperationToken token)
    {
        getTrackerForProber().unregister(token);

        return wrapAsync(null);
    }

    @Override
    public CompletableFuture<ProberOperation.BaseResults> getOperationResults(ProberOperationToken token)
    {
        ProberOperationTracker.State state = getTrackerForProber().get(token);

        return wrapAsync(state != null ? state.getOutput() : null);
    }

    //--//

    private CompletableFuture<ProberOperation.BaseResults> executeInner(ProberOperation input) throws
                                                                                               Exception
    {
        ProberOperationForBACnet input_bacnet = Reflection.as(input, ProberOperationForBACnet.class);
        if (input_bacnet != null)
        {
            ProberForBACnet worker = new ProberForBACnet(getApplication(), input_bacnet);
            return worker.execute();
        }

        ProberOperationForCANbus input_can = Reflection.as(input, ProberOperationForCANbus.class);
        if (input_can != null)
        {
            ProberForCANbus worker = new ProberForCANbus(getApplication(), input_can);
            return worker.execute();
        }

        ProberOperationForIpn input_ipn = Reflection.as(input, ProberOperationForIpn.class);
        if (input_ipn != null)
        {
            ProberForIpn worker = new ProberForIpn(getApplication(), input_ipn);
            return worker.execute();
        }

        throw Exceptions.newIllegalArgumentException("Unknown operation: %s", input != null ? input.getClass() : "<none>");
    }
}
