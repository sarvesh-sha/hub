/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionProgram;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueController;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueEquipment;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValuePoint;
import com.optio3.cloud.hub.logic.normalizations.EquipmentClass;
import com.optio3.cloud.hub.logic.normalizations.NormalizationEngine;
import com.optio3.cloud.hub.logic.normalizations.NormalizationState;
import com.optio3.cloud.hub.logic.normalizations.PointClass;
import com.optio3.cloud.hub.logic.normalizations.TermFrequencyInverseDocumentFrequencyVectorizer;
import com.optio3.cloud.hub.model.asset.AssetState;
import com.optio3.cloud.hub.model.common.LogLine;
import com.optio3.cloud.hub.model.normalization.NormalizationDefinitionDetails;
import com.optio3.cloud.hub.persistence.asset.BACnetDeviceRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.collection.Memoizer;
import com.optio3.metadata.normalization.BACnetImportExportData;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;

public class NormalizationEngineExecutionContext extends EngineExecutionContext<NormalizationDefinitionDetails, NormalizationEngineExecutionStep>
{
    public NormalizationEngine normalizationEngine;

    public NormalizationState state;

    public List<PointClass> pointClasses;

    public List<EquipmentClass> equipmentClasses;

    public final Memoizer memoizer = new Memoizer();

    private final Map<TermFrequencyInverseDocumentFrequencyVectorizer, TermFrequencyInverseDocumentFrequencyVectorizer> m_vectorizers = Maps.newHashMap();

    public final List<LogLine> logEntries = Lists.newArrayList();

    private       List<NormalizationEngineValueController>         m_controllers;
    private final Map<String, List<NormalizationEngineValuePoint>> m_points = Maps.newHashMap();

    //--//

    public NormalizationEngineExecutionContext(SessionProvider sessionProvider,
                                               NormalizationEngine engine,
                                               EngineExecutionProgram<NormalizationDefinitionDetails> program)
    {
        super(null, sessionProvider, program);
        normalizationEngine = engine;
    }

    public NormalizationEngineValueEquipment pushEquipment(EngineValuePrimitiveString nameRaw,
                                                           String equipmentClassId,
                                                           boolean rootLevel)
    {
        String name = EngineValuePrimitiveString.extract(nameRaw);

        NormalizationEngineValueEquipment newEquip = NormalizationEngineValueEquipment.create(name, equipmentClassId, null);

        return pushEquipment(newEquip, rootLevel);
    }

    public NormalizationEngineValueEquipment pushEquipment(NormalizationEngineValueEquipment newEquip,
                                                           boolean rootLevel)
    {
        NormalizationEngineValueEquipment firstEquip = CollectionUtils.firstElement(this.state.equipments);

        NormalizationEngineExecutionStepPushEquipment step = new NormalizationEngineExecutionStepPushEquipment();
        step.equipment = newEquip.copy();

        if (firstEquip == null || rootLevel)
        {
            this.state.equipments.add(newEquip);
        }
        else
        {
            NormalizationEngineValueEquipment leaf = firstEquip.getFirstLeaf();
            leaf.addChild(newEquip);
            step.parentEquipment = leaf.copy();
        }

        pushStep(step);

        return newEquip;
    }

    public List<NormalizationEngineValueController> getControllers() throws
                                                                     Exception
    {
        if (m_controllers == null)
        {
            try (SessionHolder subSessionHolder = sessionProvider.newReadOnlySession())
            {
                RecordHelper<NetworkAssetRecord>  helperNetwork       = subSessionHolder.createHelper(NetworkAssetRecord.class);
                RecordHelper<DeviceRecord>        helperDevice        = subSessionHolder.createHelper(DeviceRecord.class);
                RecordHelper<DeviceElementRecord> helperDeviceElement = subSessionHolder.createHelper(DeviceElementRecord.class);

                List<NormalizationEngineValueController> controllers = Lists.newArrayList();

                for (NetworkAssetRecord rec_network : helperNetwork.listAll())
                {
                    rec_network.enumerateChildrenNoNesting(helperDevice, -1, (filters) -> filters.addState(AssetState.operational), (rec_device) ->
                    {
                        BACnetDeviceRecord rec_device_bacnet = Reflection.as(rec_device, BACnetDeviceRecord.class);
                        if (rec_device_bacnet != null)
                        {
                            BACnetImportExportData device_info;

                            DeviceElementRecord rec_object_device = rec_device_bacnet.findDeviceObject(helperDeviceElement);
                            if (rec_object_device != null)
                            {
                                device_info = rec_device_bacnet.extractImportExportData(null, rec_object_device);
                            }
                            else
                            {
                                device_info = null;
                            }

                            controllers.add(NormalizationEngineValueController.create(helperDevice.asLocator(rec_device),
                                                                                      device_info.objectId.toJsonValue(),
                                                                                      device_info.deviceName,
                                                                                      device_info.dashboardName,
                                                                                      device_info.deviceDescription,
                                                                                      device_info.deviceLocation,
                                                                                      device_info.deviceVendor,
                                                                                      device_info.deviceModel,
                                                                                      device_info.transport));
                        }

                        return StreamHelperNextAction.Continue_Evict;
                    });
                }

                m_controllers = controllers;
            }
        }

        return Lists.newArrayList(m_controllers);
    }

    public List<NormalizationEngineValuePoint> getControllerPoints(NormalizationEngineValueController controller) throws
                                                                                                                  Exception
    {
        String                              controllerSysId = controller.locator.getIdRaw();
        List<NormalizationEngineValuePoint> points          = m_points.get(controllerSysId);

        if (points == null)
        {
            points = Lists.newArrayList();

            try (SessionHolder subSessionHolder = sessionProvider.newReadOnlySession())
            {
                List<NormalizationEngineValuePoint> points2           = points;
                RecordHelper<DeviceElementRecord>   helper            = subSessionHolder.createHelper(DeviceElementRecord.class);
                DeviceRecord                        rec_device        = subSessionHolder.fromLocator(controller.locator);
                BACnetDeviceRecord                  rec_device_bacnet = Reflection.as(rec_device, BACnetDeviceRecord.class);
                if (rec_device_bacnet != null)
                {
                    rec_device.enumerateChildrenNoNesting(helper, -1, (filters) -> filters.addState(AssetState.operational), (rec_object) ->
                    {
                        BACnetImportExportData device_info = rec_device_bacnet.extractImportExportData(null, rec_object);

                        points2.add(NormalizationEngineValuePoint.create(helper.asLocator(rec_object),
                                                                         device_info.objectId.toJsonValue(),
                                                                         device_info.deviceName,
                                                                         device_info.dashboardName,
                                                                         device_info.deviceDescription,
                                                                         device_info.deviceLocation));

                        return StreamHelperNextAction.Continue_Evict;
                    });
                }
            }

            m_points.put(controllerSysId, points);
        }

        return Lists.newArrayList(points);
    }

    @Override
    public void reset(ZonedDateTime when)
    {
        super.reset(when);

        logEntries.clear();
    }

    @Override
    protected NormalizationEngineExecutionStep allocateStep()
    {
        return new NormalizationEngineExecutionStep();
    }

    public TermFrequencyInverseDocumentFrequencyVectorizer getVectorizer(int minNgram,
                                                                         int maxNgram)
    {
        var vectorizer = new TermFrequencyInverseDocumentFrequencyVectorizer(memoizer, minNgram, maxNgram);
        return m_vectorizers.computeIfAbsent(vectorizer, (key) -> key);
    }
}
