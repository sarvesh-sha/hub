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

import java.time.ZonedDateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.optio3.infra.integrations.infiniteimpulse.InfiniteImpulseHelper;
import com.optio3.serialization.ObjectMappers;

public class TrendEntry
{
    public ZonedDateTime time;
    public int monitorId;

    public TrendValue avg;
    public TrendValue min;
    public TrendValue max;

    public void setJsonavg(String val) throws
                                       JsonProcessingException
    {
        avg = TrendValue.decode(val);
    }

    public void setJsonmin(String val) throws
                                       JsonProcessingException
    {
        min = TrendValue.decode(val);
    }

    public void setJsonmax(String val) throws
                                       JsonProcessingException
    {
        max = TrendValue.decode(val);
    }
}