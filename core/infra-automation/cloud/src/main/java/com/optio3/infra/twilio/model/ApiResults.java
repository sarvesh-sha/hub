/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * wireless.twilio.com
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: 2.4.9
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.infra.twilio.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;

public class ApiResults
{
    public List<Sim>         sims;
    public List<RatePlan>    rate_plans;
    public List<UsageRecord> usage_records;
    public List<DataSession> data_sessions;
    public JsonNode          meta;

    //--//

    public String extractNextPage(String queryPart) throws
                                                    URISyntaxException
    {
        if (meta != null)
        {
            JsonNode nextUrlNode = meta.get("next_page_url");
            if (nextUrlNode != null)
            {
                String nextUrl = nextUrlNode.asText();
                if (nextUrl != null)
                {
                    URI    uri   = new URI(nextUrl);
                    String query = uri.getQuery();
                    for (String queryItem : StringUtils.split(query, "&"))
                    {
                        String[] parts = StringUtils.split(queryItem, "=");
                        if (StringUtils.equals(parts[0], queryPart))
                        {
                            return parts[1];
                        }
                    }
                }
            }
        }

        return null;
    }
}
