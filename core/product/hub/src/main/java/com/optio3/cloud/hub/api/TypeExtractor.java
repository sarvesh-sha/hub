package com.optio3.cloud.hub.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.poi.hpsf.Decimal;

import com.optio3.cloud.hub.logic.protocol.IProtocolDecoder;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesEnumeratedValue;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyType;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.TimeSeries;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.enums.BACnetEngineeringUnits;
import com.google.common.collect.Maps;

public class TypeExtractor extends AssetRecord.PropertyTypeExtractor {
	@Override
	public Map<String, TimeSeriesPropertyType> classifyRecord(DeviceElementRecord rec, boolean handlePresentationType) {
		
		System.out.println("classifyRecord ZZZ");
		Map<String, TimeSeriesPropertyType> res = Maps.newHashMap();

		TimeSeriesPropertyType a = new TimeSeriesPropertyType();
		a.expectedType = Decimal.class;
		a.name = "present_value";
		// a.name = "Temprature";
		// a.displayName = "Evet Type1";
		a.type = TimeSeries.SampleType.Decimal;
		// a.isBoolean=true;
		// 0a.unitsFactors.scaling.multiplier;

		TimeSeriesEnumeratedValue t = new TimeSeriesEnumeratedValue();
		// EngineeringUnits eb =
		// EngineeringUnits.latitude;
		// List<EngineeringUnits> el = new ArrayList<EngineeringUnits>();
		// el.add(eb);
		// List<EngineeringUnits> el1 = new ArrayList<EngineeringUnits>();
		// EngineeringUnitsFactors ef =
		// EngineeringUnitsFactors.fromValues(EngineeringUnitsFactors.Scaling.Identity,
		// el, el1, EngineeringUnits.latitude);
		// a.setUnitsFactors(ef);
		// a.setPrimaryUnits(EngineeringUnits.latitude);
		EngineeringUnitsFactors ef = getUnitsFactors(rec);
		if(ef != null)
		{
			a.unitsFactors = ef;
		}
		else
			
		a.unitsFactors = EngineeringUnitsFactors.get(EngineeringUnits.kilo_btus_per_hour);

		List<TimeSeriesEnumeratedValue> list = new ArrayList<TimeSeriesEnumeratedValue>();
		// t.name = "Evet Type";
		t.value = 11;
		list.add(t);
		a.values = list;
		// map.put("Temprature",a);
		res.put("present_value", a);
		return res;
	}

	@Override
	protected void classifyInstance(Map<String, TimeSeriesPropertyType> map, BaseObjectModel obj,
			boolean handlePresentationType) {
		// TODO Auto-generated method stub

	}

	@Override
	public IProtocolDecoder getProtocolDecoder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EngineeringUnitsFactors getUnitsFactors(DeviceElementRecord rec) {
		// TODO Auto-generated method stub
		System.out.println("getUnitsFactors1 ***" );
		EngineeringUnits units = null;
        try
        {System.out.println("getUnitsFactors2 ***" );
            MetadataMap metadata = rec.getMetadata();
            units = AssetRecord.WellKnownMetadata.assignedUnits.get(metadata);
            System.out.println("units 222" + units);
            if(units != null)
            {
            	units.getDisplayName();
            }
           
        }
        catch (Throwable t)
        {
        	
        	System.out.println("units ***" + units);
        	t.printStackTrace();
            // Ignore failures.
        }
		System.out.println("getUnitsFactors  22222");
	return	EngineeringUnitsFactors.get(EngineeringUnits.kilometers_per_hour);
	}

	@Override
	public String getIndexedValue(DeviceElementRecord rec) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BaseObjectModel getContentsAsObject(DeviceElementRecord rec, boolean desiredState) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
}