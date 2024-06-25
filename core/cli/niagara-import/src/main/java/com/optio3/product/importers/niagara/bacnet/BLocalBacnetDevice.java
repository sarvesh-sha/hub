/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.bacnet;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.baja.driver.BDevice;
import com.optio3.product.importers.niagara.baja.driver.BPointDeviceExt;
import com.optio3.product.importers.niagara.baja.sys.BValue;

@ModuleTypeAnnotation(module = "bacnet", type = "LocalBacnetDevice")
public class BLocalBacnetDevice extends BDevice
{
    public BValue                  status;
    public BValue                  faultCause;
    public BBacnetObjectIdentifier objectId;
    public BBacnetDeviceStatus     systemStatus;
    public BValue                  vendorName;
    public BValue                  vendorId;
    public BValue                  modelName;
    public BValue                  firmwareRevision;
    public BValue                  applicationSoftwareVersion;
    public BValue                  location;
    public BValue                  description;
    public BValue                  protocolVersion;
    public BValue                  protocolRevision;
    public BValue                  protocolConformanceClass;
    public BBacnetBitString        protocolServicesSupported;
    public BBacnetBitString        protocolObjectTypesSupported;
    public BValue                  maxAPDULengthAccepted;
    public BValue                  segmentationSupported;
    public BValue                  maxSegmentsAccepted;
    public BValue                  apduSegmentTimeout;
    public BValue                  apduTimeout;
    public BValue                  numberOfApduRetries;
    public BBacnetListOf           deviceAddressBinding;
    public BValue                  databaseRevision;
    public BBacnetArray            configurationFiles;
    public BValue                  lastRestoreTime;
    public BValue                  backupFailureTimeout;
    public BValue                  backupPreparationTime;
    public BValue                  restorePreparationTime;
    public BValue                  restoreCompletionTime;
    public BValue                  backupAndRestoreState;
    public BBacnetListOf           activeCovSubscriptions;
    public BValue                  characterSet;
    public BValue                  enumerationList;
    public BValue                  exportTable;
    public BValue                  virtual;
    public BValue                  covPropertyPollRate;
    public BValue                  timeSynchronizationRecipients;
    public BValue                  timeSynchronizationInterval;
    public BValue                  alignIntervals;
    public BValue                  intervalOffset;
    public BBacnetListOf           utcTimeSynchronizationRecipients;

    //--//

    @Override
    protected BPointDeviceExt getPoints()
    {
        return null;
    }
}

