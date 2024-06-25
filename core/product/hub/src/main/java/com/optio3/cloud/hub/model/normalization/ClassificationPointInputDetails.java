/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.normalization;

import java.util.List;

import com.optio3.protocol.model.transport.TransportAddress;

public class ClassificationPointInputDetails
{
    public String       objectName;
    public String       objectBackupName;
    public String       objectBackupEquipmentName;
    public String       objectWorkflowOverrideName;
    public String       objectDescription;
    public String       objectIdentifier;
    public String       objectType;
    public String       objectUnits;
    public String       objectLocation;
    public List<String> objectBackupStructure;

    public String           controllerName;
    public String           controllerBackupName;
    public String           controllerDescription;
    public String           controllerIdentifier;
    public String           controllerLocation;
    public String           controllerModelName;
    public String           controllerVendorName;
    public TransportAddress controllerTransportAddress;

    //--//

    public void copy(ClassificationPointInputDetails src)
    {
        objectName                 = src.objectName;
        objectBackupName           = src.objectBackupName;
        objectBackupEquipmentName  = src.objectBackupEquipmentName;
        objectWorkflowOverrideName = src.objectWorkflowOverrideName;
        objectDescription          = src.objectDescription;
        objectIdentifier           = src.objectIdentifier;
        objectType                 = src.objectType;
        objectUnits                = src.objectUnits;
        objectLocation             = src.objectLocation;
        objectBackupStructure      = src.objectBackupStructure;

        controllerName             = src.controllerName;
        controllerBackupName       = src.controllerBackupName;
        controllerDescription      = src.controllerDescription;
        controllerIdentifier       = src.controllerIdentifier;
        controllerLocation         = src.controllerLocation;
        controllerModelName        = src.controllerModelName;
        controllerVendorName       = src.controllerVendorName;
        controllerTransportAddress = src.controllerTransportAddress;
    }
}
