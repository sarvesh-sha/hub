/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.bacnet;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.baja.sys.BValue;

@ModuleTypeAnnotation(module = "bacnet", type = "BacnetDeviceObject")
public class BBacnetDeviceObject extends BBacnetObject
{
    public BBacnetObjectIdentifier objectId;
    public BBacnetObjectType       objectType;
    public BValue                  systemStatus;
    public BValue                  vendorName;
    public BValue                  vendorIdentifier;
    public BValue                  modelName;
    public BValue                  firmwareRevision;
    public BValue                  applicationSoftwareVersion;
    public BValue                  protocolVersion;
    public BValue                  protocolRevision;
    public BValue                  protocolServicesSupported;
    public BValue                  protocolObjectTypesSupported;
    public BValue                  objectList;
    public BValue                  maxAPDULengthAccepted;
    public BValue                  segmentationSupported;
    public BValue                  apduTimeout;
    public BValue                  numberOfAPDURetries;
    public BValue                  deviceAddressBinding;
    public BValue                  databaseRevision;
}

