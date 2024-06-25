/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.bcsv3;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.baja.sys.BValue;

@ModuleTypeAnnotation(module = "bcsv3", type = "BcpBacnetHistoryDeviceExt")
@ModuleTypeAnnotation(module = "bcsv3", type = "BcpBacnetScheduleDeviceExt")
@ModuleTypeAnnotation(module = "bcsv3", type = "BcpBacnetTuningPolicy")
@ModuleTypeAnnotation(module = "bcsv3", type = "BcpConnectionInfo")
@ModuleTypeAnnotation(module = "bcsv3", type = "BcpServerConnections")
@ModuleTypeAnnotation(module = "bcsv3", type = "BcpService")
@ModuleTypeAnnotation(module = "bcsv3", type = "BcpServiceBacnetSettings")
@ModuleTypeAnnotation(module = "bcsv3", type = "BcpServiceLonworksSettings")
@ModuleTypeAnnotation(module = "bcsv3", type = "BcpSupportBacnetDevice")
@ModuleTypeAnnotation(module = "bcsv3", type = "BcpTunnelingSettings")
@ModuleTypeAnnotation(module = "bcsv3", type = "BcpUserConfig")
@ModuleTypeAnnotation(module = "bcsv3", type = "BcpWizardSettings")
@ModuleTypeAnnotation(module = "bcsv3", type = "CommunicationConfig")
@ModuleTypeAnnotation(module = "bcsv3", type = "LoadManager")
public class IgnoredBcsv3Module extends BValue
{
}

