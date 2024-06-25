/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.iso15765;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.obdii.Iso15765MessageType;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:ISO15765:SupportedPIDs")
public class SupportedPIDs extends BaseIso15765ObjectModel
{
    public static abstract class BaseReq extends BaseIso15765ObjectModel
    {
        @SerializationTag(number = 0, width = 32)
        public Unsigned32 mask;

        public List<String> pids = Lists.newArrayList();

        @Override
        public void postDecodeFixup()
        {
            long mask = Unsigned32.unboxUnsignedOrDefault(this.mask, 0);
            if (mask != 0)
            {
                decodeInner(mask);
            }
        }

        protected abstract void decodeInner(long mask);

        protected void recordPidIfSet(long mask,
                                      int base,
                                      int id,
                                      String desc)
        {
            final int offset = 32 - (id - base);

            if ((mask & (1L << offset)) != 0)
            {
                pids.add(String.format("%d - %s", id, desc));
            }
        }
    }

    @Iso15765MessageType(service = 1, pdu = 0)
    public static class Req00 extends BaseReq
    {
        @Override
        protected void decodeInner(long mask)
        {
            recordPidIfSet(mask, 0, 1, "Monitor status");
            recordPidIfSet(mask, 0, 2, "Freeze DTC");
            recordPidIfSet(mask, 0, 3, "Fuel system status");
            recordPidIfSet(mask, 0, 4, "Calculated engine load");
            recordPidIfSet(mask, 0, 5, "Engine coolant temperature");
            recordPidIfSet(mask, 0, 6, "Short term fuel trim—Bank 1");
            recordPidIfSet(mask, 0, 7, "Long term fuel trim—Bank 1");
            recordPidIfSet(mask, 0, 8, "Short term fuel trim—Bank 2");
            recordPidIfSet(mask, 0, 9, "Long term fuel trim—Bank 2");
            recordPidIfSet(mask, 0, 10, "Fuel pressure");
            recordPidIfSet(mask, 0, 11, "Intake manifold absolute pressure");
            recordPidIfSet(mask, 0, 12, "Engine RPM");
            recordPidIfSet(mask, 0, 13, "Vehicle speed");
            recordPidIfSet(mask, 0, 14, "Timing advance");
            recordPidIfSet(mask, 0, 15, "Intake air temperature");
            recordPidIfSet(mask, 0, 16, "MAF air flow rate");
            recordPidIfSet(mask, 0, 17, "Throttle position");
            recordPidIfSet(mask, 0, 18, "Commanded secondary air status");
            recordPidIfSet(mask, 0, 19, "Oxygen sensors present (in 2 banks)");
            recordPidIfSet(mask, 0, 20, "Oxygen Sensor 1");
            recordPidIfSet(mask, 0, 21, "Oxygen Sensor 2");
            recordPidIfSet(mask, 0, 22, "Oxygen Sensor 3");
            recordPidIfSet(mask, 0, 23, "Oxygen Sensor 4");
            recordPidIfSet(mask, 0, 24, "Oxygen Sensor 5");
            recordPidIfSet(mask, 0, 25, "Oxygen Sensor 6");
            recordPidIfSet(mask, 0, 26, "Oxygen Sensor 7");
            recordPidIfSet(mask, 0, 27, "Oxygen Sensor 8");
            recordPidIfSet(mask, 0, 28, "OBD standards this vehicle conforms to");
            recordPidIfSet(mask, 0, 29, "Oxygen sensors present (in 4 banks)");
            recordPidIfSet(mask, 0, 30, "Auxiliary input status");
            recordPidIfSet(mask, 0, 31, "Run time since engine start");
        }
    }

    @Iso15765MessageType(service = 1, pdu = 0x20)
    public static class Req20 extends BaseReq
    {
        @Override
        protected void decodeInner(long mask)
        {
            recordPidIfSet(mask, 32, 33, "Distance traveled with MIL on");
            recordPidIfSet(mask, 32, 34, "Fuel Rail Pressure");
            recordPidIfSet(mask, 32, 35, "Fuel Rail Gauge Pressure");
            recordPidIfSet(mask, 32, 36, "Oxygen Sensor 1");
            recordPidIfSet(mask, 32, 37, "Oxygen Sensor 2");
            recordPidIfSet(mask, 32, 38, "Oxygen Sensor 3");
            recordPidIfSet(mask, 32, 39, "Oxygen Sensor 4");
            recordPidIfSet(mask, 32, 40, "Oxygen Sensor 5");
            recordPidIfSet(mask, 32, 41, "Oxygen Sensor 6");
            recordPidIfSet(mask, 32, 42, "Oxygen Sensor 7");
            recordPidIfSet(mask, 32, 43, "Oxygen Sensor 8");
            recordPidIfSet(mask, 32, 44, "Commanded EGR");
            recordPidIfSet(mask, 32, 45, "EGR Error");
            recordPidIfSet(mask, 32, 46, "Commanded evaporative purge");
            recordPidIfSet(mask, 32, 47, "Fuel Tank Level Input");
            recordPidIfSet(mask, 32, 48, "Warm-ups since codes cleared");
            recordPidIfSet(mask, 32, 49, "Distance traveled since codes cleared");
            recordPidIfSet(mask, 32, 50, "Evap. System Vapor Pressure");
            recordPidIfSet(mask, 32, 51, "Absolute Barometric Pressure");
            recordPidIfSet(mask, 32, 52, "Oxygen Sensor 1");
            recordPidIfSet(mask, 32, 53, "Oxygen Sensor 2");
            recordPidIfSet(mask, 32, 54, "Oxygen Sensor 3");
            recordPidIfSet(mask, 32, 55, "Oxygen Sensor 4");
            recordPidIfSet(mask, 32, 56, "Oxygen Sensor 5");
            recordPidIfSet(mask, 32, 57, "Oxygen Sensor 6");
            recordPidIfSet(mask, 32, 58, "Oxygen Sensor 7");
            recordPidIfSet(mask, 32, 59, "Oxygen Sensor 8");
            recordPidIfSet(mask, 32, 60, "Catalyst Temperature: Bank 1, Sensor 1");
            recordPidIfSet(mask, 32, 61, "Catalyst Temperature: Bank 2, Sensor 1");
            recordPidIfSet(mask, 32, 62, "Catalyst Temperature: Bank 1, Sensor 2");
            recordPidIfSet(mask, 32, 63, "Catalyst Temperature: Bank 2, Sensor 2");
        }
    }

    @Iso15765MessageType(service = 1, pdu = 0x40)
    public static class Req40 extends BaseReq
    {
        @Override
        protected void decodeInner(long mask)
        {
            recordPidIfSet(mask, 64, 65, "Monitor status this drive cycle");
            recordPidIfSet(mask, 64, 66, "Control module voltage");
            recordPidIfSet(mask, 64, 67, "Absolute load value");
            recordPidIfSet(mask, 64, 68, "Fuel–Air commanded equivalence ratio");
            recordPidIfSet(mask, 64, 69, "Relative throttle position");
            recordPidIfSet(mask, 64, 70, "Ambient air temperature");
            recordPidIfSet(mask, 64, 71, "Absolute throttle position B");
            recordPidIfSet(mask, 64, 72, "Absolute throttle position C");
            recordPidIfSet(mask, 64, 73, "Accelerator pedal position D");
            recordPidIfSet(mask, 64, 74, "Accelerator pedal position E");
            recordPidIfSet(mask, 64, 75, "Accelerator pedal position F");
            recordPidIfSet(mask, 64, 76, "Commanded throttle actuator");
            recordPidIfSet(mask, 64, 77, "Time run with MIL on");
            recordPidIfSet(mask, 64, 78, "Time since trouble codes cleared");
            recordPidIfSet(mask, 64, 79, "Maximum value for Fuel–Air equivalence ratio, oxygen sensor voltage, oxygen sensor current, and intake manifold absolute pressure");
            recordPidIfSet(mask, 64, 80, "Maximum value for air flow rate from mass air flow sensor");
            recordPidIfSet(mask, 64, 81, "Fuel Type");
            recordPidIfSet(mask, 64, 82, "Ethanol fuel %");
            recordPidIfSet(mask, 64, 83, "Absolute Evap system Vapor Pressure");
            recordPidIfSet(mask, 64, 84, "Evap system vapor pressure");
            recordPidIfSet(mask, 64, 85, "Short term secondary oxygen sensor trim, A: bank 1, B: bank 3");
            recordPidIfSet(mask, 64, 86, "Long term secondary oxygen sensor trim, A: bank 1, B: bank 3");
            recordPidIfSet(mask, 64, 87, "Short term secondary oxygen sensor trim, A: bank 2, B: bank 4");
            recordPidIfSet(mask, 64, 88, "Long term secondary oxygen sensor trim, A: bank 2, B: bank 4");
            recordPidIfSet(mask, 64, 89, "Fuel rail absolute pressure");
            recordPidIfSet(mask, 64, 90, "Relative accelerator pedal position");
            recordPidIfSet(mask, 64, 91, "Hybrid battery pack remaining life");
            recordPidIfSet(mask, 64, 92, "Engine oil temperature");
            recordPidIfSet(mask, 64, 93, "Fuel injection timing");
            recordPidIfSet(mask, 64, 94, "Engine fuel rate");
            recordPidIfSet(mask, 64, 95, "Emission requirements to which vehicle is designed");
        }
    }

    @Iso15765MessageType(service = 1, pdu = 0x60)
    public static class Req60 extends BaseReq
    {
        @Override
        protected void decodeInner(long mask)
        {
            recordPidIfSet(mask, 96, 97, "Driver's demand engine - percent torque");
            recordPidIfSet(mask, 96, 98, "Actual engine - percent torque");
            recordPidIfSet(mask, 96, 99, "Engine reference torque");
            recordPidIfSet(mask, 96, 100, "Engine percent torque data");
            recordPidIfSet(mask, 96, 101, "Auxiliary input / output supported");
            recordPidIfSet(mask, 96, 102, "Mass air flow sensor");
            recordPidIfSet(mask, 96, 103, "Engine coolant temperature");
            recordPidIfSet(mask, 96, 104, "Intake air temperature sensor");
            recordPidIfSet(mask, 96, 105, "Commanded EGR and EGR Error");
            recordPidIfSet(mask, 96, 106, "Commanded Diesel intake air flow control and relative intake air flow position");
            recordPidIfSet(mask, 96, 107, "Exhaust gas recirculation temperature");
            recordPidIfSet(mask, 96, 108, "Commanded throttle actuator control and relative throttle position");
            recordPidIfSet(mask, 96, 109, "Fuel pressure control system");
            recordPidIfSet(mask, 96, 110, "Injection pressure control system");
            recordPidIfSet(mask, 96, 111, "Turbocharger compressor inlet pressure");
            recordPidIfSet(mask, 96, 112, "Boost pressure control");
            recordPidIfSet(mask, 96, 113, "Variable Geometry turbo (VGT) control");
            recordPidIfSet(mask, 96, 114, "Wastegate control");
            recordPidIfSet(mask, 96, 115, "Exhaust pressure");
            recordPidIfSet(mask, 96, 116, "Turbocharger RPM");
            recordPidIfSet(mask, 96, 117, "Turbocharger temperature");
            recordPidIfSet(mask, 96, 118, "Turbocharger temperature");
            recordPidIfSet(mask, 96, 119, "Charge air cooler temperature");
            recordPidIfSet(mask, 96, 120, "Exhaust Gas temperature (EGT) Bank 1");
            recordPidIfSet(mask, 96, 121, "Exhaust Gas temperature (EGT) Bank 2");
            recordPidIfSet(mask, 96, 122, "Diesel particulate filter (DPF)");
            recordPidIfSet(mask, 96, 123, "Diesel particulate filter (DPF)");
            recordPidIfSet(mask, 96, 124, "Diesel Particulate filter (DPF) temperature");
            recordPidIfSet(mask, 96, 125, "NOx NTE (Not-To-Exceed) control area status");
            recordPidIfSet(mask, 96, 126, "PM NTE (Not-To-Exceed) control area status");
            recordPidIfSet(mask, 96, 127, "Engine run time");
        }
    }

    @Iso15765MessageType(service = 1, pdu = 0x80)
    public static class Req80 extends BaseReq
    {
        @Override
        protected void decodeInner(long mask)
        {
            recordPidIfSet(mask, 128, 129, "Engine run time for Auxiliary Emissions Control Device");
            recordPidIfSet(mask, 128, 130, "Engine run time for Auxiliary Emissions Control Device");
            recordPidIfSet(mask, 128, 131, "NOx sensor");
            recordPidIfSet(mask, 128, 132, "Manifold surface temperature");
            recordPidIfSet(mask, 128, 133, "NOx reagent system");
            recordPidIfSet(mask, 128, 134, "Particulate matter (PM) sensor");
            recordPidIfSet(mask, 128, 135, "Intake manifold absolute pressure");
            recordPidIfSet(mask, 128, 136, "SCR Induce System");
            recordPidIfSet(mask, 128, 137, "Run Time for AECD #11-#15");
            recordPidIfSet(mask, 128, 138, "Run Time for AECD #16-#20");
            recordPidIfSet(mask, 128, 139, "Diesel Aftertreatment");
            recordPidIfSet(mask, 128, 140, "O2 Sensor (Wide Range)");
            recordPidIfSet(mask, 128, 141, "Throttle Position");
            recordPidIfSet(mask, 128, 142, "Engine Friction - Percent Torque");
            recordPidIfSet(mask, 128, 143, "PM Sensor Bank 1 & 2");
            recordPidIfSet(mask, 128, 144, "WWH-OBD Vehicle OBD System Information");
            recordPidIfSet(mask, 128, 145, "WWH-OBD Vehicle OBD System Information");
            recordPidIfSet(mask, 128, 146, "Fuel System Control");
            recordPidIfSet(mask, 128, 147, "WWH-OBD Vehicle OBD Counters support");
            recordPidIfSet(mask, 128, 148, "NOx Warning And Inducement System");
            recordPidIfSet(mask, 128, 152, "Exhaust Gas Temperature Sensor");
            recordPidIfSet(mask, 128, 153, "Exhaust Gas Temperature Sensor");
            recordPidIfSet(mask, 128, 154, "Hybrid/EV Vehicle System Data, Battery, Voltage");
            recordPidIfSet(mask, 128, 155, "Diesel Exhaust Fluid Sensor Data");
            recordPidIfSet(mask, 128, 156, "O2 Sensor Data");
            recordPidIfSet(mask, 128, 157, "Engine Fuel Rate");
            recordPidIfSet(mask, 128, 158, "Engine Exhaust Flow Rate");
            recordPidIfSet(mask, 128, 159, "Fuel System Percentage Use");
        }
    }

    @Iso15765MessageType(service = 1, pdu = 0xA0)
    public static class ReqA0 extends BaseReq
    {
        @Override
        protected void decodeInner(long mask)
        {
            recordPidIfSet(mask, 160, 161, "NOx Sensor Corrected Data");
            recordPidIfSet(mask, 160, 162, "Cylinder Fuel Rate");
            recordPidIfSet(mask, 160, 163, "Evap System Vapor Pressure");
            recordPidIfSet(mask, 160, 164, "Transmission Actual Gear");
            recordPidIfSet(mask, 160, 165, "Diesel Exhaust Fluid Dosing");
            recordPidIfSet(mask, 160, 166, "Odometer");
        }
    }

    @FieldModelDescription(description = "Supported PIDs", units = EngineeringUnits.enumerated, pointClass = WellKnownPointClass.ObdiiSupportedPIDs, debounceSeconds = 5)
    public List<String> pids;

    //--//

    @Override
    public String extractBaseId()
    {
        return "ISO15765_SupportedPIDs";
    }
}
