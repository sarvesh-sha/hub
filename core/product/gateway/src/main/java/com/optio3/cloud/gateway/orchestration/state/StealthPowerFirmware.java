/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.gateway.orchestration.state;

import com.optio3.protocol.model.ipn.objects.stealthpower.BaseStealthPowerModel;
import com.optio3.protocol.model.ipn.objects.stealthpower.StealthPower_AMR;
import com.optio3.protocol.model.ipn.objects.stealthpower.StealthPower_CAPMETRO;
import com.optio3.protocol.model.ipn.objects.stealthpower.StealthPower_FDNY;
import com.optio3.protocol.model.ipn.objects.stealthpower.StealthPower_MTA;
import com.optio3.protocol.model.ipn.objects.stealthpower.StealthPower_PEP;
import com.optio3.protocol.model.ipn.objects.stealthpower.StealthPower_PSEG;
import com.optio3.protocol.stealthpower.StealthPowerManager;
import org.apache.commons.lang3.StringUtils;

//
// To flash the bootloader, use MPLAB IPE to erase the flash and program the .hex.
//
//  Device: PIC24FJ128GA110
//  Tool: PICkit3
//
enum StealthPowerFirmware
{
    FDNY_56_old(StealthPowerManager.preamble_FDNY, 2, 1, false, StealthPower_FDNY.class, "1.0.0", "StealthPower/FDNY_old/MCU8b_Optio_FDNY_Test_Feb2020_v100.X.production.bl2"),

    //--//

    FDNY_56_new(StealthPowerManager.preamble_FDNY, 2, 2, true, StealthPower_FDNY.class, "2.0.4", "StealthPower/FDNY_new/MCU8b_Optio_FDNY_March2020.X.production_v204.bl2"),

    //--//

    FDNY_76(StealthPowerManager.preamble_FDNY, 3, 1, true, StealthPower_FDNY.class, "3.0.2", "StealthPower/FDNY_76/MCU8b_Optio_FDNY_76_Mar2020.X.production_v302.bl2"),

    //--//

    MTA(StealthPowerManager.preamble_MTA, 2, 1, true, StealthPower_MTA.class, "2.0.2", "StealthPower/MTA/MCU8b_MTA_North.v202.production.bl2"),

    //--//

    AMR_LTE(StealthPowerManager.preamble_AMR, 1, 1, true, StealthPower_AMR.class, "1.0.3", "StealthPower/AMR/MCU8b_AMR_LTE.v103.production.bl2"),

    //--//

    AMR_SP3X(StealthPowerManager.preamble_AMR, 2, 1, true, StealthPower_AMR.class, "2.0.3", "StealthPower/AMR/MCU8b_AMR_SP3X.v203.production.bl2"),

    //--//

    AMR_PSEG(StealthPowerManager.preamble_PSEG, 2, 1, true, StealthPower_PSEG.class, "1.0.2", "StealthPower/PSEG/MCU8b_PSEG.x.production_v102.bl2"),

    //--//

    AMR_PEP(StealthPowerManager.preamble_PEP, 2, 1, true, StealthPower_PEP.class, "1.0.2", "StealthPower/PEP/MCU8b_PEPCO.x.production_v102.bl2"),

    //--//

    AMR_CAPMETRO(StealthPowerManager.preamble_CAPMETRO, 2, 1, true, StealthPower_CAPMETRO.class, "1.0.2", "StealthPower/CAPMETRO/MCU8b_capmetro.X.production.v102.bl2");

    //--//

    public final int                                    bootloadId;
    public final int                                    hardwareVersion;
    public final int                                    hardwareRevision;
    public final boolean                                supportsRestart;
    public final Class<? extends BaseStealthPowerModel> modelClass;
    public final String                                 appVersion;
    public final String                                 resource;

    StealthPowerFirmware(int bootloadId,
                         int hardwareVersion,
                         int hardwareRevision,
                         boolean supportsRestart,
                         Class<? extends BaseStealthPowerModel> modelClass,
                         String appVersion,
                         String resource)
    {
        this.bootloadId       = bootloadId;
        this.hardwareVersion  = hardwareVersion;
        this.hardwareRevision = hardwareRevision;
        this.supportsRestart  = supportsRestart;
        this.modelClass       = modelClass;
        this.appVersion       = appVersion;
        this.resource         = resource;
    }

    public static StealthPowerFirmware matchBootloader(int bootloadId,
                                                       int hardwareVersion,
                                                       int hardwareRevision)
    {
        for (StealthPowerFirmware value : values())
        {
            if (value.bootloadId == bootloadId && value.hardwareVersion == hardwareVersion && value.hardwareRevision == hardwareRevision)
            {
                return value;
            }
        }

        return null;
    }

    public static StealthPowerFirmware matchApplication(Class<? extends BaseStealthPowerModel> modelClass,
                                                        String appVersion)
    {
        for (StealthPowerFirmware value : values())
        {
            if (value.modelClass == modelClass && StringUtils.equals(value.appVersion, appVersion))
            {
                return value;
            }
        }

        return null;
    }
}