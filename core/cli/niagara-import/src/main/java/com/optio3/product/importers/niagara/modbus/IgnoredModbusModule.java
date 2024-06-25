/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.modbus;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.baja.sys.BValue;

@ModuleTypeAnnotation(module = "modbusCore", type = "AddressFormatEnum")
@ModuleTypeAnnotation(module = "modbusCore", type = "DataTypeEnum")
@ModuleTypeAnnotation(module = "modbusCore", type = "DevicePollConfigTable")
@ModuleTypeAnnotation(module = "modbusCore", type = "FlexAddress")
@ModuleTypeAnnotation(module = "modbusCore", type = "FlexAddress")
@ModuleTypeAnnotation(module = "modbusTcp", type = "ModbusTcpDevice")
@ModuleTypeAnnotation(module = "modbusTcp", type = "ModbusTcpNetwork")
public class IgnoredModbusModule extends BValue
{
}

