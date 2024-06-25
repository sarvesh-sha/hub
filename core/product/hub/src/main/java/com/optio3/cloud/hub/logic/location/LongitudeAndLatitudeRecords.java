/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.location;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

import com.google.common.collect.Sets;
import com.optio3.cloud.hub.model.asset.DeviceElementFilterRequest;
import com.optio3.cloud.hub.model.customization.InstanceConfiguration;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementSampleRecord;
import com.optio3.cloud.hub.persistence.asset.IpnDeviceRecord;
import com.optio3.cloud.hub.persistence.asset.TimeSeries;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.protocol.model.FieldModel;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.WellKnownPointClassOrCustom;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.protocol.model.ipn.objects.IpnLocation;
import org.apache.commons.lang3.StringUtils;

public class LongitudeAndLatitudeRecords
{
    private static final String c_idLongitude;
    private static final String c_idLatitude;

    static
    {
        String      idLongitude = null;
        String      idLatitude  = null;
        IpnLocation obj         = new IpnLocation();

        for (FieldModel fieldModel : obj.getDescriptors())
        {
            final WellKnownPointClassOrCustom pt = fieldModel.getPointClass(obj);

            if (pt == WellKnownPointClass.LocationLongitude.asWrapped())
            {
                idLongitude = fieldModel.name;
            }
            else if (pt == WellKnownPointClass.LocationLatitude.asWrapped())
            {
                idLatitude = fieldModel.name;
            }
        }

        c_idLongitude = idLongitude;
        c_idLatitude  = idLatitude;
    }

    //--//

    public TypedRecordIdentity<DeviceElementRecord> longitude;
    public TypedRecordIdentity<DeviceElementRecord> latitude;

    public void locate(SessionHolder sessionHolder,
                       AssetRecord rec)
    {
        Set<String> seen = Sets.newHashSet();

        locate(sessionHolder, rec, seen);
    }
	static final ZonedDateTime baseTime = ZonedDateTime.of(2023, 10, 12, 0, 0, 0, 1238, ZoneId.systemDefault());

    private boolean locate(SessionHolder sessionHolder,
                           AssetRecord rec,
                           Set<String> seen)
    {
        if (rec == null)
        {
            return false;
        }
		RecordHelper<AssetRecord> assetHelper = sessionHolder.createHelper(AssetRecord.class);
        RecordHelper<DeviceElementRecord> helper_element = sessionHolder.createHelper(DeviceElementRecord.class);

   
			
        System.out.println("LongitudeAndLatitudeRecords locate 2");
        IpnDeviceRecord rec_ipn = SessionHolder.asEntityOfClassOrNull(rec, IpnDeviceRecord.class);
        System.out.println("LongitudeAndLatitudeRecords locate 3"+rec_ipn);
     //  if (rec_ipn != null)
        {
            DeviceElementFilterRequest filters = DeviceElementFilterRequest.createFilterForParent(rec);
            System.out.println("LongitudeAndLatitudeRecords locate 2");
            try
            {
                System.out.println("LongitudeAndLatitudeRecords locate 4");
                DeviceElementRecord.enumerate(helper_element, true, filters, (rec_object) ->
                { System.out.println("LongitudeAndLatitudeRecords locate 5");
                    String         elementId = rec_object.getIdentifier();
                    IpnObjectModel obj       = rec_object.getTypedContents(IpnObjectModel.getObjectMapper(), IpnObjectModel.class);
                    System.out.println("LongitudeAndLatitudeRecords locate 6"+elementId) ;
                    if (obj instanceof IpnLocation)
                    { System.out.println("LongitudeAndLatitudeRecords locate 6.5");
                        if (StringUtils.equals(elementId, c_idLongitude))
                        {
                        	System.out.println("LongitudeAndLatitudeRecords locate 6.75");
                            longitude = TypedRecordIdentity.newTypedInstance(helper_element, rec_object);
                        }

                        if (StringUtils.equals(elementId, c_idLatitude))
                        {
                        	System.out.println("LongitudeAndLatitudeRecords locate 6.85");

                            latitude = TypedRecordIdentity.newTypedInstance(helper_element, rec_object);
                        }

                        if (isValid())
                        {
                            return StreamHelperNextAction.Stop_Evict;
                        }
                    }
                    System.out.println("LongitudeAndLatitudeRecords locate 7");

                    seen.add(rec_object.getSysId());

                    return StreamHelperNextAction.Continue_Evict;
                });
            }
            catch (Exception e)
            {
            	
            	System.out.println("*******Exceptio******");
            	e.printStackTrace();
            	// Ignore failures...
            }

            if (isValid())
            {
                return true;
            }
        }

        if (!seen.add(rec.getSysId()))
        {
            // Avoid recursions...
        	System.out.println("*******4******");
        	return false;
        }

        DeviceElementRecord rec_element = SessionHolder.asEntityOfClassOrNull(rec, DeviceElementRecord.class);
        if (rec_element != null)
        {
            if (locate(sessionHolder, rec.getParentAsset(), seen))
            {
                return true;
            }
        }

        LocationRecord rec_loc = SessionHolder.asEntityOfClassOrNull(rec, LocationRecord.class);
        if (rec_loc != null)
        {
            InstanceConfiguration cfg = sessionHolder.getServiceNonNull(InstanceConfiguration.class);
            if (cfg.hasRoamingAssets())
            {
                for (AssetRecord rec_locAsset : rec_loc.getAssets())
                {
                    if (locate(sessionHolder, rec_locAsset, seen))
                    {
                        return true;
                    }
                }
            }
        }

        if (locate(sessionHolder, rec.getLocation(), seen))
        {
            return true;
        }

        return isValid();
    }

    public boolean isValid()
    {
        return longitude != null && latitude != null;
    }
}
