/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model;

import com.optio3.util.BoxingUtils;
import org.apache.commons.lang3.StringUtils;

public class DeployerCellularInfo
{
    public String modemIMSI;
    public String modemIMEI;

    private String m_modemICCID;

    public String getModemICCID()
    {
        return trimFiller(m_modemICCID);
    }

    public void setModemICCID(String modemICCID)
    {
        m_modemICCID = trimFiller(modemICCID);
    }

    public void update(String imsi,
                       String imei,
                       String iccid)
    {
        imsi = StringUtils.stripToNull(imsi);
        imei = StringUtils.stripToNull(imei);
        iccid = StringUtils.stripToNull(iccid);

        modemIMSI = BoxingUtils.get(imsi, modemIMSI);
        modemIMEI = BoxingUtils.get(imei, modemIMEI);
        m_modemICCID = trimFiller(BoxingUtils.get(iccid, m_modemICCID));
    }

    public boolean sameICCID(String iccid)
    {
        String modemICCID = getModemICCID();

        return modemICCID != null && modemICCID.equals(trimFiller(iccid));
    }

    public static String trimFiller(String iccid)
    {
        //
        // ICCID can be 19 or 20 character long. If the provider doesn't allocate all 20 characters, it add "F" to the end.
        //
        return StringUtils.removeEnd(iccid, "F");
    }
}
