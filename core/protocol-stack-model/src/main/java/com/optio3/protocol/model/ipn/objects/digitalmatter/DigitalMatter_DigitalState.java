/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.digitalmatter;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.WellKnownPointClass;

@JsonTypeName("Ipn:DigitalMatter:DigitalState")
public class DigitalMatter_DigitalState extends BaseDigitalMatterObjectModel
{
    @FieldModelDescription(description = "In Trip", units = EngineeringUnits.enumerated, pointClass = WellKnownPointClass.TrackerInTrip)
    public DigitalMatter_Flag inTrip;

    @FieldModelDescription(description = "Tamper Alert", units = EngineeringUnits.enumerated, pointClass = WellKnownPointClass.TrackerTamperAlert)
    public DigitalMatter_Flag tamperAlert;

    @FieldModelDescription(description = "Recovery Mode Active", units = EngineeringUnits.enumerated, pointClass = WellKnownPointClass.TrackerRecoveryModeActive)
    public DigitalMatter_Flag recoveryModeActive;

    //--//

    @Override
    public String extractBaseId()
    {
        return "digitalState";
    }

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        detailsForParent.equipmentClass = WellKnownEquipmentClass.SensorDiagnostics.asWrapped();
    }
}
