/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.deployment;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.model.deployment.DeploymentAgent;
import com.optio3.cloud.builder.model.deployment.DeploymentAgentDetails;
import com.optio3.cloud.builder.model.deployment.DeploymentOperationalStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedAgentTermination;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryImageRecord;
import com.optio3.cloud.client.deployer.model.DeploymentAgentFeature;
import com.optio3.cloud.messagebus.channel.RpcConnectionInfo;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.PersistAsJsonHelper;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RawQueryHelper;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.RecordWithHeartbeat;
import com.optio3.cloud.persistence.RecordWithHeartbeat_;
import com.optio3.cloud.persistence.RecordWithMetadata_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.ObjectMappers;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "DEPLOYMENT_AGENT")
@Optio3TableInfo(externalId = "DeploymentAgent", model = DeploymentAgent.class, metamodel = DeploymentAgentRecord_.class)
public class DeploymentAgentRecord extends RecordWithHeartbeat implements ModelMapperTarget<DeploymentAgent, DeploymentAgentRecord_>
{
    /**
     * The deployment this agent is running on.
     */
    @Optio3ControlNotifications(reason = "Only notify host of agent's changes", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getDeployment")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "deployment", nullable = false, foreignKey = @ForeignKey(name = "AGENT__DEPLOYMENT__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private DeploymentHostRecord deployment;

    //--//

    @Column(name = "instance_id", nullable = false)
    private String instanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeploymentStatus status;

    @Column(name = "docker_id")
    private String dockerId;

    @Column(name = "rpc_id")
    private String rpcId;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Lob
    @Column(name = "details")
    private String details;

    @Transient
    private final PersistAsJsonHelper<String, DeploymentAgentDetails> m_detailsParser = new PersistAsJsonHelper<>(() -> details,
                                                                                                                  (val) -> details = val,
                                                                                                                  String.class,
                                                                                                                  DeploymentAgentDetails.class,
                                                                                                                  ObjectMappers.SkipNulls);

    //--//

    public DeploymentAgentRecord()
    {
    }

    public static DeploymentAgentRecord newInstance(DeploymentHostRecord deployer)
    {
        requireNonNull(deployer);

        DeploymentAgentRecord res = new DeploymentAgentRecord();
        res.deployment = deployer;
        res.status     = DeploymentStatus.Initialized;
        return res;
    }

    //--//

    public DeploymentHostRecord getDeployment()
    {
        return deployment;
    }

    public String getInstanceId()
    {
        return instanceId;
    }

    public void setInstanceId(String instanceId)
    {
        this.instanceId = instanceId;
    }

    public DeploymentStatus getStatus()
    {
        return status;
    }

    public void setStatus(DeploymentStatus status)
    {
        this.status = status;
    }

    public String getDockerId()
    {
        return dockerId;
    }

    public void setDockerId(String dockerId)
    {
        this.dockerId = dockerId;
    }

    public String getRpcId()
    {
        return gotHeartbeatRecently(1, TimeUnit.HOURS) ? rpcId : null;
    }

    public void setRpcId(String rpcId)
    {
        this.rpcId = rpcId;
    }

    public boolean isActive()
    {
        return active;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

    public boolean hasHeartbeat()
    {
        return gotHeartbeatRecently(30, TimeUnit.MINUTES);
    }

    //--//

    public RpcConnectionInfo extractConnectionInfo()
    {
        DeploymentHostRecord rec_host = getDeployment();

        var ci = new RpcConnectionInfo();
        ci.hostDisplayName = rec_host.getDisplayName();
        ci.hostId          = rec_host.getHostId();
        ci.instanceId      = getInstanceId();
        ci.rpcId           = getRpcId();
        return ci;
    }

    //--//

    public DeploymentAgentDetails getDetails()
    {
        return m_detailsParser.get();
    }

    public void setDetails(DeploymentAgentDetails details)
    {
        m_detailsParser.set(details);
    }

    public boolean canSupport(DeploymentAgentFeature... features)
    {
        DeploymentAgentDetails details = getDetails();
        return details != null && details.canSupport(features);
    }

    //--//

    public DeploymentTaskRecord findTask()
    {
        DeploymentHostRecord rec_host = getDeployment();
        if (rec_host != null)
        {
            for (DeploymentTaskRecord rec_task : rec_host.getTasks())
            {
                if (StringUtils.equals(rec_task.getDockerId(), getDockerId()))
                {
                    return rec_task;
                }
            }
        }

        return null;
    }

    public RegistryImageRecord findImage()
    {
        DeploymentTaskRecord rec_task = findTask();
        return rec_task != null ? rec_task.getImageReference() : null;
    }

    public boolean isDefaultAgent()
    {
        return StringUtils.equals(instanceId, "v1");
    }

    //--//

    public static void streamAllRaw(SessionHolder sessionHolder,
                                    Consumer<RawQueryHelper<DeploymentAgentRecord, DeploymentAgent>> applyFilters,
                                    Consumer<DeploymentAgent> callback)
    {
        RawQueryHelper<DeploymentAgentRecord, DeploymentAgent> qh = new RawQueryHelper<>(sessionHolder, DeploymentAgentRecord.class);

        qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);
        qh.addDate(RecordWithCommonFields_.createdOn, (obj, val) -> obj.createdOn = val);
        qh.addDate(RecordWithCommonFields_.updatedOn, (obj, val) -> obj.updatedOn = val);

        qh.addObject(RecordWithMetadata_.metadataCompressed, byte[].class, (obj, val) -> obj.metadataCompressed = val);
        qh.addDate(RecordWithHeartbeat_.lastHeartbeat, (obj, val) -> obj.lastHeartbeat = val);

        qh.addReference(DeploymentAgentRecord_.deployment, DeploymentHostRecord.class, (obj, val) -> obj.deployment = val);
        qh.addEnum(DeploymentAgentRecord_.status, DeploymentStatus.class, (obj, val) -> obj.status = val);
        qh.addBoolean(DeploymentAgentRecord_.active, (obj, val) -> obj.active = val);
        qh.addString(DeploymentAgentRecord_.dockerId, (obj, val) -> obj.dockerId = val);
        qh.addString(DeploymentAgentRecord_.instanceId, (obj, val) -> obj.instanceId = val);
        qh.addString(DeploymentAgentRecord_.rpcId, (obj, val) -> obj.rpcId = val);

        qh.addStringDeserializer(DeploymentAgentRecord_.details, DeploymentAgentDetails.class, (obj, val) -> obj.details = val);

        if (applyFilters != null)
        {
            applyFilters.accept(qh);
        }

        qh.stream(DeploymentAgent::new, callback);
    }

    public static List<DeploymentAgentRecord> getBatch(RecordHelper<DeploymentAgentRecord> helper,
                                                       List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    //--//

    public void checkTerminateConditions(ValidationResultsHolder validation)
    {
        if (isActive())
        {
            validation.addFailure("active", "Active agent '%s'", getInstanceId());
        }

        if (getDockerId() == null)
        {
            validation.addFailure("dockerId", "Agent '%s' is not running", getInstanceId());
        }
    }

    public void checkRemoveConditions(ValidationResultsHolder validation)
    {
        if (!hasHeartbeat())
        {
            // The agent hasn't been updated in forty minutes, it's okay to remove it.
            return;
        }

        DeploymentHostRecord rec_host = getDeployment();
        if (rec_host != null)
        {
            if (rec_host.getOperationalStatus() == DeploymentOperationalStatus.retired)
            {
                // Host has been retired, we can delete all the agents.
                return;
            }
        }

        if (isActive())
        {
            validation.addFailure("active", "Active agent '%s'", getInstanceId());
        }

        if (getStatus() != DeploymentStatus.Terminated)
        {
            validation.addFailure("status", "Running agent '%s'", getInstanceId());
        }
    }

    public void terminate(ValidationResultsHolder validation) throws
                                                              Exception
    {
        checkTerminateConditions(validation);

        if (validation.canProceed())
        {
            DelayedAgentTermination.queue(validation.sessionHolder, this);
        }
    }

    public void remove(ValidationResultsHolder validation,
                       RecordHelper<DeploymentAgentRecord> agentHelper)
    {
        checkRemoveConditions(validation);

        if (validation.canProceed())
        {
            agentHelper.delete(this);
        }
    }
}
