/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.proxy;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.optio3.cloud.client.Optio3RemotableProxy;
import com.optio3.cloud.client.gateway.model.GatewayAutoDiscovery;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayNetwork;
import com.optio3.cloud.client.gateway.model.GatewayOperationToken;

@Optio3RemotableProxy
public interface GatewayDiscoveryApi
{
    /**
     * Tells the Gateway to try all known protocol configurations, to discover connected sensors.
     *
     * @return A token that can be used to check the progress of the operation
     *
     * @throws Exception
     */
    CompletableFuture<GatewayOperationToken> performAutoDiscovery() throws
                                                                    Exception;

    /**
     * Gets the results for a previous AutoDiscovery.
     *
     * @return List of check configurations and their results
     *
     * @throws Exception
     */
    CompletableFuture<List<GatewayAutoDiscovery>> getAutoDiscoveryResults(GatewayOperationToken token) throws
                                                                                                       Exception;

    /**
     * Tells the Gateway to refresh its network list.
     *
     * @param networks The set of networks to manage
     *
     * @return List of networks that changed on the gateway.
     *
     * @throws Exception
     */
    CompletableFuture<List<String>> assignNetworks(List<GatewayNetwork> networks) throws
                                                                                  Exception;

    /**
     * Starts a discovery using various protocols.
     *
     * @param entities           The specification of the target for the discovery
     * @param broadcastIntervals Number of seconds to sleep after a discovery broadcast
     * @param rebroadcastCount   Number of times to repeat the discovery broadcast
     *
     * @return A token that can be used to check the progress of the operation
     *
     * @throws Exception
     */
    CompletableFuture<GatewayOperationToken> triggerDiscovery(List<GatewayDiscoveryEntity> entities,
                                                              int broadcastIntervals,
                                                              int rebroadcastCount) throws
                                                                                    Exception;

    /**
     * Lists objects in the networks.
     *
     * @param entities The specification of the target for the list operations
     *
     * @return A token that can be used to check the progress of the operation
     *
     * @throws Exception
     */
    CompletableFuture<GatewayOperationToken> listObjects(List<GatewayDiscoveryEntity> entities) throws
                                                                                                Exception;

    /**
     * Reads all the values from the target entities.
     *
     * @param entities The specification of the target for the read operations
     *
     * @return A token that can be used to check the progress of the operation
     *
     * @throws Exception
     */
    CompletableFuture<GatewayOperationToken> readAllValues(List<GatewayDiscoveryEntity> entities) throws
                                                                                                  Exception;

    /**
     * Writes values to the target entities.
     *
     * @param entities The specification of the target for the write operations
     *
     * @return A token that can be used to check the progress of the operation
     *
     * @throws Exception
     */
    CompletableFuture<GatewayOperationToken> writeValues(List<GatewayDiscoveryEntity> entities) throws
                                                                                                Exception;

    /**
     * Configures the sampling period for the target entities.
     *
     * @param entities If not null, only read these entities.
     *
     * @return A token that can be used to check the progress of the operation
     *
     * @throws Exception
     */
    CompletableFuture<GatewayOperationToken> startSamplingConfiguration(List<GatewayDiscoveryEntity> entities) throws
                                                                                                               Exception;

    /**
     * Configures the sampling period for the target entities.
     *
     * @param entities The configuration for various entities.
     *
     * @return A token that can be used to check the progress of the operation
     *
     * @throws Exception
     */
    CompletableFuture<GatewayOperationToken> updateSamplingConfiguration(List<GatewayDiscoveryEntity> entities) throws
                                                                                                                Exception;

    /**
     * Configures the sampling period for the target entities.
     *
     * @param entities If not null, only read these entities.
     *
     * @return A token that can be used to check the progress of the operation
     *
     * @throws Exception
     */
    CompletableFuture<GatewayOperationToken> completeSamplingConfiguration(List<GatewayDiscoveryEntity> entities) throws
                                                                                                                  Exception;

    /**
     * Flushes all the pending entities back to the Hub.
     *
     * @return A token that can be used to check the progress of the operation
     *
     * @throws Exception
     */
    CompletableFuture<GatewayOperationToken> flushEntities() throws
                                                             Exception;
}
