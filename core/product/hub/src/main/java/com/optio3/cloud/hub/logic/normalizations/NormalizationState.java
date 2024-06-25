/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.normalizations;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueEquipment;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueLocation;
import com.optio3.cloud.hub.model.normalization.ClassificationPointInput;
import com.optio3.cloud.hub.model.normalization.ClassificationReason;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.transport.TransportAddress;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class NormalizationState
{
    public String           controlPointIdentifier;
    public String           controlPointName;
    public String           controlPointNameRaw;
    public String           controlPointWorkflowOverrideName;
    public String           controlPointDescription;
    public String           controlPointLocation;
    public EngineeringUnits controlPointUnits;
    public String           controlPointType;

    public String           controllerIdentifier;
    public String           controllerName;
    public String           controllerBackupName;
    public String           controllerDescription;
    public String           controllerLocation;
    public String           controllerModel;
    public String           controllerVendor;
    public TransportAddress controllerTransportAddress;

    //--//

    public String nameFromLegacyImport;
    public String equipmentNameFromLegacyImport;

    public List<String> structureFromLegacyImport = Lists.newArrayList();

    //--//

    public final List<NormalizationEngineValueEquipment> equipments = Lists.newArrayList();

    public final List<NormalizationEngineValueLocation> locations = Lists.newArrayList();

    public String pointClassId;

    public boolean setUnclassified;

    public ClassificationReason classificationReason;

    public double positiveScore;
    public double negativeScore;

    public int samplingPeriod;

    public boolean noSampling;

    //--//

    public final Set<String> tags = Sets.newHashSet();

    public boolean tagsSet;

    //--//

    @JsonIgnore
    public MetadataMap metadata = MetadataMap.empty();

    //--//

    public final List<NormalizationMatchHistory> history = Lists.newArrayList();

    //--//

    @Override
    public boolean equals(Object o)
    {
        NormalizationState that = Reflection.as(o, NormalizationState.class);
        if (that == null)
        {
            return false;
        }

        String jsonThis = ObjectMappers.prettyPrintAsJson(this);
        String jsonThat = ObjectMappers.prettyPrintAsJson(o);

        return StringUtils.equals(jsonThis, jsonThat);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(controlPointIdentifier,
                            controlPointName,
                            controlPointWorkflowOverrideName,
                            controlPointDescription,
                            controlPointLocation,
                            controlPointUnits,
                            controlPointType,
                            controllerIdentifier,
                            controllerName,
                            controllerDescription,
                            controllerLocation,
                            controllerModel,
                            controllerVendor,
                            controllerTransportAddress,
                            nameFromLegacyImport,
                            equipmentNameFromLegacyImport,
                            structureFromLegacyImport,
                            tags);
    }

    //--//

    public NormalizationState copy()
    {
        NormalizationState copy = ObjectMappers.cloneThroughJson(ObjectMappers.SkipNulls, this);
        copy.metadata = metadata.copy();
        return copy;
    }

    public String extractEquipmentThroughLegacyHeuristics()
    {
        String                            equip     = "";
        NormalizationEngineValueEquipment equipment = CollectionUtils.firstElement(equipments);
        if (equipment != null)
        {
            equip = equipment.name;
        }

        if (StringUtils.isEmpty(equip))
        {
            equip = BACnetObjectModel.extractName(controllerName, controllerDescription, true);
        }

        return equip;
    }

    public String extractInputThroughLegacyHeuristics()
    {
        // Give precedence to the name imported from the BAS system.
        String input = nameFromLegacyImport;
        if (input == null)
        {
            input = BACnetObjectModel.extractName(controlPointName, controlPointDescription, true);
        }

        return input;
    }

    public void setPointClassId(String pointClassId,
                                double posScore,
                                double negScore,
                                ClassificationReason reason)
    {
        this.pointClassId    = pointClassId;
        setUnclassified      = pointClassId == null;
        positiveScore        = posScore;
        negativeScore        = negScore;
        classificationReason = reason;
    }

    public static NormalizationState fromClassificationInput(ClassificationPointInput input)
    {
        NormalizationState stateIn = new NormalizationState();

        stateIn.controlPointIdentifier           = input.details.objectIdentifier;
        stateIn.controlPointName                 = input.details.objectName;
        stateIn.controlPointWorkflowOverrideName = input.details.objectWorkflowOverrideName;
        stateIn.controlPointDescription          = input.details.objectDescription;
        stateIn.controlPointLocation             = input.details.objectLocation;
        stateIn.controlPointUnits                = input.objectUnits;
        stateIn.controlPointType                 = input.objectType != null ? input.objectType.toString() : "";
        stateIn.nameFromLegacyImport             = input.details.objectBackupName;
        stateIn.equipmentNameFromLegacyImport    = input.details.objectBackupEquipmentName;
        if (input.details.objectBackupStructure != null)
        {
            stateIn.structureFromLegacyImport.addAll(input.details.objectBackupStructure);
        }

        stateIn.controllerIdentifier       = input.details.controllerIdentifier;
        stateIn.controllerName             = input.details.controllerName;
        stateIn.controllerBackupName       = input.details.controllerBackupName;
        stateIn.controllerDescription      = input.details.controllerDescription;
        stateIn.controllerLocation         = input.details.controllerLocation;
        stateIn.controllerModel            = input.details.controllerModelName;
        stateIn.controllerVendor           = input.details.controllerVendorName;
        stateIn.controllerTransportAddress = input.details.controllerTransportAddress;

        return stateIn;
    }
}
