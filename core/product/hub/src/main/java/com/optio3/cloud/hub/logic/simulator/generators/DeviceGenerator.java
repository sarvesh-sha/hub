/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.simulator.generators;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayState;
import com.optio3.cloud.hub.model.normalization.DeviceElementClassificationOverrides;
import com.optio3.cloud.hub.model.normalization.NormalizationEquipment;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.BACnetDeviceRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.protocol.model.bacnet.BACnetDeviceAddress;
import com.optio3.protocol.model.bacnet.BACnetDeviceDescriptor;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.enums.BACnetEngineeringUnits;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.objects.device;
import com.optio3.util.CollectionUtils;
import com.optio3.util.TimeUtils;

public abstract class DeviceGenerator extends ObjectGenerator<device>
{
    public class DeviceReader
    {
        private Map<BACnetObjectIdentifier, ObjectGenerator<?>.ObjectReader> m_objectReaders = Maps.newHashMap();

        public void addObjectReader(BACnetObjectIdentifier objId,
                                    BACnetPropertyIdentifierOrUnknown prop,
                                    Function<BACnetObjectIdentifier, ObjectGenerator<?>.ObjectReader> creator)
        {
            ObjectGenerator<?>.ObjectReader sampler = m_objectReaders.computeIfAbsent(objId, creator);
            sampler.add(prop);
        }

        public CompletableFuture<Void> read(GatewayState.ResultHolder holder_device,
                                            Callable<CompletableFuture<Void>> progressCallback)
        {
            final ZonedDateTime timestamp = TimeUtils.fromTimestampToUtcTime(holder_device.getTimestamp());

            for (BACnetObjectIdentifier objId : m_objectReaders.keySet())
            {
                ObjectGenerator<?>.ObjectReader objReader = m_objectReaders.get(objId);

                GatewayState.ResultHolder holder_object = holder_device.prepareResult(GatewayDiscoveryEntitySelector.BACnet_Object, objId.toJsonValue(), false);

                try
                {
                    BACnetObjectModel obj = objReader.read(getPSTTime(timestamp), true);

                    // When reading fails, just create a placeholder for the timestamp, so we know something was not right.
                    String contents = obj != null ? obj.serializeToJson() : null;

                    GatewayState.ResultHolder holder_sample = holder_object.prepareResult(GatewayDiscoveryEntitySelector.BACnet_ObjectSample, (String) null, true);
                    holder_sample.queueContents(contents);

                    await(progressCallback.call());
                }
                catch (Exception ex)
                {
                    // Ignore failures.
                }
            }

            return wrapAsync(null);
        }
    }

    private BACnetDeviceAddress m_address;
    private String              m_modelName;
    private String              m_vendorName;
    private int                 m_vendorIdentifier;

    private Multimap<NormalizationEquipment, ObjectGenerator<?>> m_equipmentObjectGenerators = ArrayListMultimap.create();

    protected DeviceGenerator(int networkNumber,
                              int instanceNumber,
                              String name,
                              String modelName,
                              String vendorName,
                              int vendorIdentifier)
    {
        super(name, instanceNumber, BACnetObjectType.device, BACnetEngineeringUnits.no_units, device.class);
        m_address          = new BACnetDeviceAddress(networkNumber, instanceNumber);
        m_modelName        = modelName;
        m_vendorName       = vendorName;
        m_vendorIdentifier = vendorIdentifier;
    }

    public <D extends ObjectGenerator<?>> D registerGenerator(NormalizationEquipment equipment,
                                                              D generator)
    {
        m_equipmentObjectGenerators.put(equipment, generator);
        return generator;
    }

    public void persistDevice(RecordHelper<AssetRecord> helper,
                              NetworkAssetRecord rec_network,
                              NormalizationEquipment parentEquipment,
                              Map<String, DeviceElementClassificationOverrides> overrides) throws
                                                                                           Exception
    {
        BACnetDeviceRecord rec_device = new BACnetDeviceRecord();
        rec_device.setPhysicalName(getName());

        BACnetDeviceDescriptor desc = new BACnetDeviceDescriptor();
        desc.address = m_address;
        rec_device.setIdentityDescriptor(desc);

        helper.persist(rec_device);
        helper.flush();

        rec_device.linkToParent(helper, rec_network);

        for (NormalizationEquipment equipment : m_equipmentObjectGenerators.keySet())
        {
            for (ObjectGenerator<?> generator : m_equipmentObjectGenerators.get(equipment))
            {
                DeviceElementRecord rec_object = generator.persistObject(helper, rec_device);

                if (equipment != null)
                {
                    DeviceElementClassificationOverrides override = new DeviceElementClassificationOverrides();
                    override.equipments = Lists.newArrayList();

                    if (parentEquipment != null)
                    {
                        override.equipments.add(parentEquipment);
                    }

                    override.equipments.add(equipment);

                    overrides.put(rec_object.getSysId(), override);
                }
            }
        }
    }

    public List<NormalizationEquipment> getEquipments()
    {
        return CollectionUtils.transformToListNoNulls(m_equipmentObjectGenerators.keySet(), eq -> eq);
    }

    public BACnetDeviceAddress getDeviceAddress()
    {
        return m_address;
    }

    public Collection<ObjectGenerator<?>> getObjectGenerators()
    {
        return m_equipmentObjectGenerators.values();
    }

    public ObjectGenerator<?> getObjectGenerator(BACnetObjectIdentifier objId)
    {
        for (ObjectGenerator<?> gen : m_equipmentObjectGenerators.values())
        {
            if (objId.equals(gen.getIdentifier()))
            {
                return gen;
            }
        }

        return null;
    }

    @Override
    public void readSamples(ZonedDateTime now,
                            device d)
    {
        d.setValue(BACnetPropertyIdentifier.model_name, m_modelName);
        d.setValue(BACnetPropertyIdentifier.vendor_name, m_vendorName);
        d.setValue(BACnetPropertyIdentifier.vendor_identifier, m_vendorIdentifier);
    }

    public DeviceReader newDeviceReader()
    {
        return new DeviceReader();
    }
}
