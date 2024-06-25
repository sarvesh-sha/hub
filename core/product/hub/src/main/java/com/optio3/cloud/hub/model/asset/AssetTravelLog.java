/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import java.time.ZonedDateTime;
import java.util.List;

import com.google.common.collect.Lists;
import com.mapbox.geojson.Point;
import com.mapbox.turf.TurfMeasurement;
import com.optio3.cloud.hub.logic.samples.SamplesCache;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesMultiPropertyRequest;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesMultiPropertyResponse;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyRequest;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.collection.ExpandableArrayOfDoubles;

public class AssetTravelLog
{
    public static class Raw
    {
        public double[] timestamps;
        public double[] longitude;
        public double[] latitude;
    }

    public List<AssetTravelLogSegment> segments = Lists.newArrayList();

    //--//

    public static Raw collect(SamplesCache samplesCache,
                              TypedRecordIdentity<DeviceElementRecord> ri_longitude,
                              TypedRecordIdentity<DeviceElementRecord> ri_latitude,
                              ZonedDateTime rangeStart,
                              ZonedDateTime rangeEnd)
    {
        TimeSeriesPropertyRequest req_lng = new TimeSeriesPropertyRequest();
        TimeSeriesPropertyRequest req_lat = new TimeSeriesPropertyRequest();

        req_lng.sysId = ri_longitude.sysId;
        req_lng.prop  = DeviceElementRecord.DEFAULT_PROP_NAME;

        req_lat.sysId = ri_latitude.sysId;
        req_lat.prop  = DeviceElementRecord.DEFAULT_PROP_NAME;

        TimeSeriesMultiPropertyRequest req = new TimeSeriesMultiPropertyRequest();
        req.rangeStart = rangeStart;
        req.rangeEnd   = rangeEnd;
        req.specs      = Lists.newArrayList(req_lng, req_lat);

        TimeSeriesMultiPropertyResponse data = req.fetch(samplesCache);

        var res = new Raw();
        res.timestamps = data.timestamps;
        res.longitude  = data.results[0].values;
        res.latitude   = data.results[1].values;

        return res;
    }

    public void collect(SamplesCache samplesCache,
                        TypedRecordIdentity<DeviceElementRecord> ri_longitude,
                        TypedRecordIdentity<DeviceElementRecord> ri_latitude,
                        ZonedDateTime rangeStart,
                        ZonedDateTime rangeEnd,
                        double maxGapForSegmentInMeters,
                        int maxDurationPerSegmentInSeconds)
    {
        var raw = collect(samplesCache, ri_longitude, ri_latitude, rangeStart, rangeEnd);

        try (ExpandableArrayOfDoubles res_timestamps = ExpandableArrayOfDoubles.create())
        {
            try (ExpandableArrayOfDoubles res_longitudes = ExpandableArrayOfDoubles.create())
            {
                try (ExpandableArrayOfDoubles res_latitudes = ExpandableArrayOfDoubles.create())
                {
                    Point  previousPoint    = null;
                    double currentTimestamp = 0;

                    for (int i = 0; i < raw.timestamps.length; i++)
                    {
                        double timestamp = raw.timestamps[i];
                        double lngValue  = raw.longitude[i];
                        double latValue  = raw.latitude[i];

                        if (!Double.isNaN(lngValue) && !Double.isNaN(latValue) && Math.abs(latValue) >= 0.1 && Math.abs(lngValue) >= 0.1)
                        {
                            Point point = Point.fromLngLat(lngValue, latValue);

                            if (previousPoint != null)
                            {
                                double distance = TurfMeasurement.distance(previousPoint, point, "meters");
                                if (distance > maxGapForSegmentInMeters)
                                {
                                    flush(res_timestamps, res_longitudes, res_latitudes);
                                    currentTimestamp = timestamp;
                                }
                            }

                            if (timestamp - currentTimestamp > maxDurationPerSegmentInSeconds)
                            {
                                flush(res_timestamps, res_longitudes, res_latitudes);
                                currentTimestamp = timestamp;
                            }

                            res_timestamps.add(timestamp);
                            res_longitudes.add(lngValue);
                            res_latitudes.add(latValue);

                            previousPoint = point;
                        }
                    }

                    flush(res_timestamps, res_longitudes, res_latitudes);
                }
            }
        }
    }

    private void flush(ExpandableArrayOfDoubles res_timestamps,
                       ExpandableArrayOfDoubles res_longitudes,
                       ExpandableArrayOfDoubles res_latitudes)
    {
        if (res_timestamps.size() > 0)
        {
            AssetTravelLogSegment segment = new AssetTravelLogSegment();
            segment.timestamps = res_timestamps.toArray();
            segment.longitudes = res_longitudes.toArray();
            segment.latitudes  = res_latitudes.toArray();
            segments.add(segment);

            res_timestamps.clear();
            res_longitudes.clear();
            res_latitudes.clear();
        }
    }
}
