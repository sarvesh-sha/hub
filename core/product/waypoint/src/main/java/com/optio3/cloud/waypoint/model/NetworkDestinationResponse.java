/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.model;

import java.net.HttpURLConnection;
import java.net.URL;

import com.optio3.cloud.waypoint.WaypointApplication;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class NetworkDestinationResponse
{
    public String result;

    public void checkDestination(String target)
    {
        for (int pass = 0; pass < 3; pass++)
        {
            try
            {
                URL               url           = new URL(target);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setConnectTimeout(2 * 1000);
                urlConnection.setReadTimeout(2 * 1000);
                urlConnection.setRequestMethod("HEAD");

                try
                {
                    String type = urlConnection.getContentType();
                    if (StringUtils.isNotEmpty(type))
                    {
                        result = "reachable";
                        return;
                    }
                }
                finally
                {
                    IOUtils.close(urlConnection);
                }
            }
            catch (Exception e)
            {
                // Failure means no connection.
                WaypointApplication.LoggerInstance.error("checkDestination for '%s' failed due to: %s", target, e);
            }
        }

        result = "unreachable";
    }
}
