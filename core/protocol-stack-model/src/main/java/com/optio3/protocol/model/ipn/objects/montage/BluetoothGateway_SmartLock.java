/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.montage;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.WellKnownPointClass;

@JsonTypeName("Ipn:BluetoothGateway:SmartLock")
public class BluetoothGateway_SmartLock extends BaseBluetoothGatewayObjectModel
{
    @FieldModelDescription(description = "Locking / Unlocking Counter", units = EngineeringUnits.counts)
    public int lockingCounter;

    @FieldModelDescription(description = "Status", units = EngineeringUnits.enumerated, pointClass = WellKnownPointClass.FaultCode, debounceSeconds = 15)
    public SmartLockState status;

    //--//

    @Override
    public String extractBaseId()
    {
        return "smartLock";
    }

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        detailsForParent.equipmentName    = unitId;
        detailsForParent.equipmentClass   = WellKnownEquipmentClass.SmartLock.asWrapped();
        detailsForParent.instanceSelector = unitId;
    }
}
