/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.optio3.cloud.builder.model.provision.ProvisionReport;
import com.optio3.util.CollectionUtils;
import com.optio3.util.IdGenerator;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public class DeploymentHostProvisioningInfo
{
    public List<DeploymentHostProvisioningNotes> notes = Lists.newArrayList();

    public ProvisionReport manufacturingInfo;

    //--//

    public static String fixupNA(String s)
    {
        return StringUtils.equals(s, "N/A") ? null : s;
    }

    public boolean updateManufacturingInfo(ProvisionReport info)
    {
        if (manufacturingInfo == null)
        {
            manufacturingInfo = info;
            return true;
        }

        boolean updated = false;

        updated |= checkChanges(manufacturingInfo.manufacturingLocation, info.manufacturingLocation, (value) -> manufacturingInfo.manufacturingLocation = value);

        updated |= checkChanges(manufacturingInfo.imei, info.imei, (value) -> manufacturingInfo.imei = value);
        updated |= checkChanges(manufacturingInfo.imsi, info.imsi, (value) -> manufacturingInfo.imsi = value);
        updated |= checkChanges(manufacturingInfo.iccid, info.iccid, (value) -> manufacturingInfo.iccid = value);

        updated |= checkChanges(manufacturingInfo.boardSerialNumber, info.boardSerialNumber, (value) -> manufacturingInfo.boardSerialNumber = value);
        updated |= checkChanges(manufacturingInfo.firmwareVersion, info.firmwareVersion, (value) -> manufacturingInfo.firmwareVersion = value);

        updated |= checkChanges(manufacturingInfo.modemModule, info.modemModule, (value) -> manufacturingInfo.modemModule = value);
        updated |= checkChanges(manufacturingInfo.modemRevision, info.modemRevision, (value) -> manufacturingInfo.modemRevision = value);

        return updated;
    }

    private static boolean checkChanges(String orig,
                                        String updated,
                                        Consumer<String> setter)
    {
        orig    = fixupNA(orig);
        updated = fixupNA(updated);

        if (updated != null && !StringUtils.equals(orig, updated))
        {
            setter.accept(updated);
            return true;
        }

        return false;
    }

    public void addNote(DeploymentHostProvisioningNotes note)
    {
        note.sysId     = IdGenerator.newGuid();
        note.timestamp = TimeUtils.now();

        notes.add(note);
    }

    public static DeploymentHostProvisioningInfo sanitize(DeploymentHostProvisioningInfo val,
                                                          boolean allocateIfMissing)
    {
        if (val != null)
        {
            if (CollectionUtils.isNotEmpty(val.notes))
            {
                return val;
            }

            if (val.manufacturingInfo != null)
            {
                return val;
            }
        }

        return allocateIfMissing ? new DeploymentHostProvisioningInfo() : null;
    }
}
