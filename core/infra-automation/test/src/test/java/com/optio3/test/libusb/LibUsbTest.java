/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.libusb;

import com.optio3.infra.LibUsbHelper;
import com.optio3.test.infra.Optio3InfraTest;
import org.junit.Ignore;
import org.junit.Test;
import org.usb4java.ConfigDescriptor;
import org.usb4java.DeviceDescriptor;
import org.usb4java.Interface;
import org.usb4java.InterfaceDescriptor;

public class LibUsbTest extends Optio3InfraTest
{
    @Ignore("Manually enable to test, since it requires access to USB devices.")
    @Test
    public void listDevices() throws
                              Exception
    {
        try (LibUsbHelper usbHelper = new LibUsbHelper())
        {
            try (LibUsbHelper.DeviceListHelper deviceList = usbHelper.getDeviceList())
            {
                for (LibUsbHelper.DeviceHelper device : deviceList.getDevices())
                {
                    DeviceDescriptor descriptor = device.getDeviceDescriptor();
//                System.out.println("####### DeviceDescriptor");
//                System.out.println(descriptor.dump());

                    String product      = device.getString(descriptor.iProduct());
                    String manufacturer = device.getString(descriptor.iManufacturer());

                    System.out.printf("iProduct: %s / %s\n", product, manufacturer);

                    for (ConfigDescriptor cfgDescriptor : device.getConfigDescriptors())
                    {
                        for (Interface anInterface : cfgDescriptor.iface())
                        {
                            boolean found = false;

                            for (InterfaceDescriptor interfaceDescriptor : anInterface.altsetting())
                            {
                                if (!found)
                                {
                                    found = true;

                                    System.out.println("####### Interface");
                                    System.out.println(anInterface.dump());
                                    System.out.println();
                                }

                                System.out.println("####### InterfaceDescriptor");
                                System.out.printf("iInterface: %s\n", device.getString(interfaceDescriptor.iInterface()));
                                System.out.println(interfaceDescriptor.dump());
                                System.out.println();
                            }
                        }
                    }
                }
            }
        }
    }
}
