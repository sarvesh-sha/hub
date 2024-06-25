/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.hub.model.asset.DeviceElementFilterRequest;
import com.optio3.cloud.hub.persistence.asset.BACnetDeviceRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.metadata.normalization.BACnetBulkRenamingData;
import com.optio3.metadata.normalization.ImportExportData;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.bacnet.BACnetDeviceDescriptor;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.serialization.ObjectMappers;
import com.optio3.service.IServiceProvider;

public class BulkRenaming
{
    public static void generate(IServiceProvider serviceProvider,
                                String bulkRenamingInput,
                                String bulkRenamingOutput) throws
                                                           Exception
    {
        try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(serviceProvider, null, Optio3DbRateLimiter.Normal))
        {
            DeploymentProbersExport exportState = ObjectMappers.SkipNulls.readValue(new File(bulkRenamingInput), DeploymentProbersExport.class);

            RecordHelper<NetworkAssetRecord>  helperNetwork = holder.createHelper(NetworkAssetRecord.class);
            RecordHelper<BACnetDeviceRecord>  helperDevice  = holder.createHelper(BACnetDeviceRecord.class);
            RecordHelper<DeviceElementRecord> helperObject  = holder.createHelper(DeviceElementRecord.class);

            List<ImportExportData> metadata = Lists.newArrayList();

            for (NetworkAssetRecord rec_network : helperNetwork.listAll())
            {
                for (DeploymentProbersDeviceExport device : exportState.devices)
                {
                    BACnetDeviceDescriptor desc       = (BACnetDeviceDescriptor) device.descriptor;
                    BACnetDeviceRecord     rec_device = BACnetDeviceRecord.findByDescriptor(helperDevice, rec_network, desc);
                    if (rec_device != null)
                    {
                        Map<String, BACnetObjectIdentifier> map = Maps.newHashMap();

                        for (DeploymentProbersObjectExport objectExport : device.objects.values())
                        {
                            String name = (String) objectExport.properties.getValue(BACnetPropertyIdentifier.object_name, null);
                            map.put(name, new BACnetObjectIdentifier(objectExport.objectId));
                        }

                        final DeviceElementFilterRequest filters = DeviceElementFilterRequest.createFilterForParent(rec_device);
                        DeviceElementRecord.enumerateNoNesting(helperObject, filters, (rec_object) ->
                        {
                            BACnetObjectModel obj  = rec_object.getTypedContents(BACnetObjectModel.getObjectMapper(), BACnetObjectModel.class);
                            String            name = (String) obj.getValue(BACnetPropertyIdentifier.object_name, null);

                            BACnetObjectIdentifier objId = map.get(name);
                            if (objId != null)
                            {
                                BACnetBulkRenamingData res = new BACnetBulkRenamingData();
                                res.networkId = desc.address.networkNumber;
                                res.instanceId = desc.address.instanceNumber;
                                res.objectId = new BACnetObjectIdentifier(rec_object.getIdentifier());
                                res.objectIdNew = objId;
                                HubApplication.LoggerInstance.info("Found: %s => %s : %s # %s", rec_object.getSysId(), rec_object.getIdentifier(), objId.toJsonValue(), name);

                                metadata.add(res);
                            }

                            return StreamHelperNextAction.Continue_Evict;
                        });
                    }
                }
            }

            ImportExportData.save(new File(bulkRenamingOutput), metadata);
        }
    }

    public static class DeploymentProbersExport
    {
        public String version;

        public List<DeploymentProbersDeviceExport> devices;

        public JsonNode frames;
    }

    public static class DeploymentProbersDeviceExport
    {
        public BaseAssetDescriptor descriptor;

        public boolean       isBBMD;
        public boolean       isRouter;
        public List<Integer> routedNetworks;

        public boolean foundInMstpScan;
        public boolean foundInSubnetScan;

        public boolean manual;

        public Map<String, DeploymentProbersObjectExport> objects;
    }

    public static class DeploymentProbersObjectExport
    {
        public String objectId;

        public BACnetObjectModel properties;
    }
}
