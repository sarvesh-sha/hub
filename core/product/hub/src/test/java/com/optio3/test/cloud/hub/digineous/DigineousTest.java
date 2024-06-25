/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub.digineous;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.core.type.TypeReference;
import com.optio3.cloud.client.hub.api.DataConnectionApi;
import com.optio3.cloud.client.hub.api.UsersApi;
import com.optio3.cloud.client.hub.model.User;
import com.optio3.cloud.client.hub.util.DataConnectionHelper;
import com.optio3.cloud.exception.NotAuthenticatedException;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.HubDataDefinition;
import com.optio3.cloud.hub.model.customization.digineous.InstanceConfigurationForDigineous;
import com.optio3.cloud.hub.model.customization.digineous.model.DigineousDeviceConfig;
import com.optio3.cloud.hub.model.customization.digineous.model.DigineousDeviceLibrary;
import com.optio3.cloud.hub.model.customization.digineous.model.DigineousMachineConfig;
import com.optio3.cloud.hub.model.customization.digineous.model.DigineousMachineLibrary;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.serialization.ObjectMappers;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import org.junit.ClassRule;
import org.junit.Test;

public class DigineousTest extends Optio3Test
{
    @ClassRule
    public static final TestApplicationWithDbRule<HubApplication, HubConfiguration> applicationRule = new TestApplicationWithDbRule<>(HubApplication.class, "hub-test.yml", (configuration) ->
    {
        configuration.data = new HubDataDefinition[] { new HubDataDefinition("demodata/defaultUsers.json", false, false) };

        configuration.instanceConfigurationForUnitTest = new InstanceConfigurationForDigineous();
    }, null);

    //--//

    @Test
    @TestOrder(10)
    public void testNotAuthorized()
    {
        UsersApi proxy = applicationRule.createProxy("api/v1", UsersApi.class);

        User user = proxy.login("user@demo.optio3.com", "userPwd");
        assertNotNull(user);

        assertNonAuthorized(() ->
                            {
                                var proxy2 = applicationRule.createProxy("api/v1", DataConnectionApi.class);

                                DigineousMachineConfig req = new DigineousMachineConfig();
                                req.machineId   = "1";
                                req.machineName = "test1";

                                DataConnectionHelper.callEndpoint(proxy2, InstanceConfigurationForDigineous.endpoint__MACHINE_CREATE, null, req, TypedRecordIdentity.class);
                            });

        proxy.logout();

        user = proxy.login("admin@demo.optio3.com", "adminPwd");
        assertNotNull(user);
    }

    @Test
    @TestOrder(20)
    public void testTemplate()
    {
        var proxy = applicationRule.createProxy("api/v1", DataConnectionApi.class);

        DigineousDeviceLibrary deviceTemplate = DataConnectionHelper.callEndpoint(proxy,
                                                                                  InstanceConfigurationForDigineous.endpoint__DEVICE_TEMPLATE_GET,
                                                                                  "DeviceTemplateV1",
                                                                                  null,
                                                                                  DigineousDeviceLibrary.class);
        System.out.println(ObjectMappers.prettyPrintAsJson(deviceTemplate));

        {
            var val = deviceTemplate.locatePoint("AI1");
            val.pointClass = WellKnownPointClass.ArrayVoltage.asWrapped();
            val.enabled    = true;
        }
        {
            var val = deviceTemplate.locatePoint("DI1");
            val.pointClass = WellKnownPointClass.CommandOpen.asWrapped();
            val.enabled    = true;
        }

        //--//

        DigineousDeviceLibrary deviceTemplateB = DataConnectionHelper.callEndpoint(proxy,
                                                                                   InstanceConfigurationForDigineous.endpoint__DEVICE_TEMPLATE_SET,
                                                                                   "DeviceTemplateV1",
                                                                                   deviceTemplate,
                                                                                   DigineousDeviceLibrary.class);

        {
            var valB = deviceTemplateB.locatePoint("AI1");
            assertEquals(WellKnownPointClass.ArrayVoltage, valB.pointClass.known);
            assertTrue(valB.enabled);
        }
        {
            var valB = deviceTemplateB.locatePoint("DI1");
            assertEquals(WellKnownPointClass.CommandOpen, valB.pointClass.known);
            assertTrue(valB.enabled);
        }

        //--//

        DigineousMachineLibrary machineTemplate = new DigineousMachineLibrary();
        machineTemplate.equipmentClass = WellKnownEquipmentClass.Machine.asWrapped();
        machineTemplate.deviceTemplates.add(deviceTemplateB.id);

        DigineousMachineLibrary machineDeviceTemplate1res = DataConnectionHelper.callEndpoint(proxy,
                                                                                              InstanceConfigurationForDigineous.endpoint__MACHINE_TEMPLATE_SET,
                                                                                              "MachineTemplateV1",
                                                                                              machineTemplate,
                                                                                              DigineousMachineLibrary.class);
        System.out.println(ObjectMappers.prettyPrintAsJson(machineDeviceTemplate1res));

        {
            var deviceA = machineDeviceTemplate1res.deviceTemplates.get(0);
            assertEquals(deviceTemplateB.id, deviceA);
        }
    }

    @Test
    @TestOrder(30)
    public void testCreation()
    {
        var proxy = applicationRule.createProxy("api/v1", DataConnectionApi.class);

        var deviceConfig1 = new DigineousDeviceConfig();
        deviceConfig1.deviceId = 1;

        var machineConfig = new DigineousMachineConfig();
        machineConfig.machineId       = "1";
        machineConfig.machineName     = "Test";
        machineConfig.machineTemplate = "MachineTemplateV1";
        machineConfig.devices.add(deviceConfig1);

        TypedRecordIdentity<NetworkAssetRecord> ri_network = DataConnectionHelper.callEndpoint(proxy,
                                                                                               InstanceConfigurationForDigineous.endpoint__MACHINE_CREATE,
                                                                                               null,
                                                                                               machineConfig,
                                                                                               new TypeReference<>()
                                                                                               {
                                                                                               });
        assertNotNull(ri_network.sysId);

        DigineousMachineConfig machineConfigB = DataConnectionHelper.callEndpoint(proxy, InstanceConfigurationForDigineous.endpoint__MACHINE_GET, ri_network.sysId, null, DigineousMachineConfig.class);
        System.out.println(ObjectMappers.prettyPrintAsJson(machineConfigB));

        assertEquals(machineConfig.machineTemplate, machineConfigB.machineTemplate);
    }

    //--//

    private void assertNonAuthorized(Runnable task)
    {
        try
        {
            task.run();
            fail();
        }
        catch (NotAuthenticatedException ex)
        {
            System.out.printf("Got expected exception: %s%n", ex.getMessage());
        }
    }
}
