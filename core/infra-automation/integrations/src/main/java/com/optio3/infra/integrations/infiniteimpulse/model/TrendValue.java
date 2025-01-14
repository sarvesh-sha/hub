/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Subscriber Actions
 * <p>Perform troubleshooting actions for your subscribers.</p><p>You can use these endpoints to&#58;</p><ul> <li>View a list of available actions</li> <li>Refresh the subscriber services that are currently running (SIM refresh) </li> <li>Disconnect/reconnect a subscriber (forces a new connection)</li> <li>Perform a SIM swap between two subscribers to transfer products, services and tariff details from one SIM to another</li></ul><p>The subscriber actions you can perform depend on the subscriber type and network.</p><p><strong>Note -</strong> Some endpoints require you to pass the subscriber <em>physicalId</em> parameter in the request. This value varies depending on the subscriber type&#58;</p><ul><li><em>Cellular</em> - use the ICCID</li><li><em>Non-IP</em> - use the Device EUI</li><li><em>Satellite</em> - use the IMEI</li></ul>
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.infra.integrations.infiniteimpulse.model;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.optio3.infra.integrations.infiniteimpulse.InfiniteImpulseHelper;

public class TrendValue
{
    public Double totalAcceleration;
    public Double velocityX;
    public Double velocityY;
    public Double velocityZ;
    public Double temperature;
    public Double audio;

    public static TrendValue decode(String val) throws
                                                JsonProcessingException
    {
        Map<String, Double> map = InfiniteImpulseHelper.getObjectMapper()
                                                       .readValue(val, new TypeReference<Map<String, Double>>()
                                                       {
                                                       });

        TrendValue res = new TrendValue();

        map.forEach((k, v) ->
                    {
                        switch (k)
                        {
                            case "0001":
                                res.totalAcceleration = v;
                                break;

                            case "0002":
                                res.velocityX = v;
                                break;

                            case "0003":
                                res.velocityY = v;
                                break;

                            case "0004":
                                res.velocityZ = v;
                                break;

                            case "0005":
                                res.temperature = v;
                                break;

                            case "0006":
                                res.audio = v;
                                break;
                        }
                    });

        return res;
    }
}