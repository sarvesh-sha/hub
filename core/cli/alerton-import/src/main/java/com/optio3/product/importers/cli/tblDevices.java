/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.cli;

public class tblDevices
{
    public int     alarmhostinst;
    public boolean commalarmenabled;
    public int     commalarmfreq;
    public int     commalarmhostinst;
    public int     commalarmnotclass;
    public String  commissionedby;
    public String  commissioningdate;
    public String  ddc_app;
    public boolean ddc_autodown;
    public String  ddc_job;
    public String  ddc_rep;
    public String  devdescription;
    public String  devfirmware;
    public int     devinst;
    public String  devlocation;
    public String  devmodel;
    public String  devsoftware;
    public boolean enablemstpproxy;
    public String  macaddr;
    public int     max_trends;
    public int     network;
    public String  objname;
    public boolean omf_autodown;
    public int     schedhostinst;
    public int     trendhostinst;
    public int     unit_type;
    public int     utcoffset;

    @Override
    public String toString()
    {
        return "tblDevices{" + "devinst=" + devinst + ", network=" + network + ", objname='" + objname + '\'' + '}';
    }
}
