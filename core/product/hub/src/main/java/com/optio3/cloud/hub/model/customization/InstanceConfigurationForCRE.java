/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization;

import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.hub.logic.normalizations.EquipmentClass;
import com.optio3.cloud.hub.logic.normalizations.NormalizationRules;
import com.optio3.cloud.hub.logic.normalizations.PointClass;
import com.optio3.cloud.hub.logic.normalizations.ValidationRules;
import com.optio3.cloud.hub.model.asset.DeviceElementSampling;
import com.optio3.cloud.hub.model.normalization.DeviceElementNormalizationRun;
import com.optio3.cloud.hub.model.workflow.IWorkflowHandler;
import com.optio3.cloud.hub.model.workflow.IWorkflowHandlerForCRE;
import com.optio3.cloud.hub.model.workflow.WorkflowDetails;
import com.optio3.cloud.hub.orchestration.tasks.TaskForDeviceElementNormalization;
import com.optio3.cloud.hub.orchestration.tasks.recurring.RecurringIoTHubPush;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.normalization.NormalizationRecord;
import com.optio3.cloud.hub.persistence.workflow.WorkflowRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.infra.integrations.azuredigitaltwins.AzureDigitalTwinsHelper;
import com.optio3.infra.integrations.azureiothub.AzureIotHubHelper;
import com.optio3.protocol.model.config.ProtocolConfigForIpn;
import com.optio3.serialization.Reflection;

@JsonTypeName("InstanceConfigurationForCRE")
public class InstanceConfigurationForCRE extends InstanceConfiguration
{
    public static final String c_credentialKeyForAzureDigitalTwin           = "AzureDigitalTwin";
    public static final String c_credentialKeyForAzureIotHub                = "AzureIotHub";
    public static final String c_credentialKeyForAzureIotHubConnectionState = "AzureIotHub-ConnectionState";

    @Override
    public void start()
    {
        // Nothing to do.
    }

    @Override
    public void stop()
    {
        // Nothing to do.
    }

    public CompletableFuture<Void> preprocessResults(List<GatewayDiscoveryEntity> entities)
    {
        // Nothing to do.
        return wrapAsync(null);
    }

    @Override
    public boolean hasRoamingAssets()
    {
        return false;
    }

    @Override
    public boolean shouldAutoConfig()
    {
        return false;
    }

    @Override
    public boolean shouldReportWhenUnreachable(DeviceRecord rec,
                                               ZonedDateTime unresponsiveSince)
    {
        return true;
    }

    @Override
    public boolean prepareSamplingConfiguration(SessionHolder sessionHolder,
                                                DeviceRecord rec_device,
                                                DeviceElementRecord rec_obj,
                                                boolean checkNonZeroValue,
                                                List<DeviceElementSampling> config)
    {
        // Nothing to do, auto config is not enabled.
        return false;
    }

    @Override
    public boolean fixupAutoConfig(ProtocolConfigForIpn cfg)
    {
        // Nothing to do, auto config is not enabled.
        return false;
    }

    @Override
    public NormalizationRules updateNormalizationRules(SessionHolder sessionHolder,
                                                       NormalizationRules rules) throws
                                                                                 Exception
    {
        if (rules == null)
        {
            rules = new NormalizationRules();
        }

        if (rules.pointClasses.isEmpty())
        {
            rules.pointClasses.addAll(PointClass.load());
        }

        if (rules.equipmentClasses.isEmpty())
        {
            rules.equipmentClasses.addAll(EquipmentClass.load());
        }

        if (rules.locationClasses.isEmpty())
        {
            rules.populateWellKnownLocationClasses();
        }

        if (rules.validation == null)
        {
            rules.validation = ValidationRules.load();
        }

        return rules;
    }

    @Override
    public void reclassify()
    {
        // Nothing to do, classification runs through a different process.
    }

    @Override
    public BackgroundActivityRecord scheduleClassification(SessionHolder sessionHolder,
                                                           NetworkAssetRecord rec_network) throws
                                                                                           Exception
    {
        // Nothing to do, classification runs through a different process.
        return null;
    }

    @Override
    public void executeClassification(SessionProvider sessionProvider,
                                      RecordLocator<NetworkAssetRecord> loc_network)
    {
        // Nothing to do, classification runs through a different process.
    }

    @Override
    public void handleWorkflowCreated(SessionHolder sessionHolder,
                                      WorkflowRecord rec,
                                      UserRecord rec_user)
    {
        WorkflowDetails details = rec.getDetails();

        boolean                closed        = false;
        IWorkflowHandlerForCRE handlerForCRE = Reflection.as(details, IWorkflowHandlerForCRE.class);
        if (handlerForCRE != null)
        {
            closed = handlerForCRE.postWorkflowCreationForCRE(sessionHolder);
        }
        else
        {
            IWorkflowHandler handler = Reflection.as(details, IWorkflowHandler.class);
            if (handler != null)
            {
                closed = handler.postWorkflowCreation(sessionHolder);
            }
        }

        if (closed)
        {
            rec.markAsProcessed(sessionHolder, rec_user);
        }
    }

    @Override
    public void handleWorkflowUpdated(SessionHolder sessionHolder,
                                      WorkflowRecord rec,
                                      UserRecord rec_user)
    {
        WorkflowDetails details = rec.getDetails();

        IWorkflowHandlerForCRE handler = Reflection.as(details, IWorkflowHandlerForCRE.class);
        if (handler != null)
        {
            try
            {
                DeviceElementNormalizationRun run = new DeviceElementNormalizationRun();
                run.rulesId = "active";

                if (!run.ensureRules(sessionHolder.createHelper(NormalizationRecord.class)))
                {
                    return;
                }

                RecordHelper<DeviceRecord>        helper      = sessionHolder.createHelper(DeviceRecord.class);
                RecordHelper<UserRecord>          helper_user = sessionHolder.createHelper(UserRecord.class);
                List<RecordLocator<DeviceRecord>> locators    = NormalizationRecord.extractDevices(helper, run);

                // Queue normalization
                TaskForDeviceElementNormalization.scheduleTask(sessionHolder, locators, run.rules, helper_user.asLocator(rec_user), false);
            }
            catch (Throwable t)
            {
                LoggerInstance.error("Failed to start normalization/classification. %s", t);
            }
        }
    }

    @Override
    protected boolean shouldNotifyNewGateway(String instanceId)
    {
        return true;
    }

    @Override
    protected NetworkAssetRecord createInstanceId(SessionHolder sessionHolder,
                                                  String instanceId,
                                                  GatewayAssetRecord rec_gateway) throws
                                                                                  Exception
    {
        return createInstanceIdImpl(sessionHolder, instanceId, rec_gateway);
    }

    @Override
    protected void afterNetworkCreation(SessionHolder sessionHolder,
                                        GatewayAssetRecord rec_gateway,
                                        NetworkAssetRecord rec_network) throws
                                                                        Exception
    {
        // No automatic configuration, nothing to do.
    }

    //--//

    @JsonIgnore
    public AzureDigitalTwinsHelper.Credentials getAzureDigitalTwinCredentials() throws
                                                                                Exception
    {
        return m_app.getTypedPrivateValue(c_credentialKeyForAzureDigitalTwin, AzureDigitalTwinsHelper.Credentials.class);
    }

    @JsonIgnore
    public void setAzureDigitalTwinCredentials(AzureDigitalTwinsHelper.Credentials cred) throws
                                                                                         Exception
    {
        m_app.setTypedPrivateValue(c_credentialKeyForAzureDigitalTwin, cred);
    }

    @JsonIgnore
    public AzureIotHubHelper.Credentials getAzureIotHubCredentials() throws
                                                                     Exception
    {
        return m_app.getTypedPrivateValue(c_credentialKeyForAzureIotHub, AzureIotHubHelper.Credentials.class);
    }

    @JsonIgnore
    public void setAzureIotHubCredentials(AzureIotHubHelper.Credentials cred) throws
                                                                              Exception
    {
        m_app.setTypedPrivateValue(c_credentialKeyForAzureIotHub, cred);
    }

    @JsonIgnore
    public RecurringIoTHubPush.History getAzureIotHubConnectionHistory() throws
                                                                         Exception
    {
        return m_app.getTypedPrivateValue(c_credentialKeyForAzureIotHubConnectionState, RecurringIoTHubPush.History.class);
    }

    @JsonIgnore
    public void setAzureIotHubConnectionHistory(RecurringIoTHubPush.History history) throws
                                                                                     Exception
    {
        m_app.setTypedPrivateValue(c_credentialKeyForAzureIotHubConnectionState, history);
    }
}
