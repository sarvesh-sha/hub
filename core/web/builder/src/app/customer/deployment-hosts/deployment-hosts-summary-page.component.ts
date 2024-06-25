import {Component, Injector, ViewChild} from "@angular/core";
import {ReportError} from "app/app.service";
import {CustomerServiceSelectionDialogComponent} from "app/customer/customer-services/customer-service-selection-dialog.component";
import {CustomerSelectionDialogComponent} from "app/customer/customers/customer-selection-dialog.component";
import {DeploymentAgentUpgradeOverlay} from "app/customer/deployment-agents/deployment-agent-upgrade-overlay.component";

import {DeploymentHostsListComponent} from "app/customer/deployment-hosts/deployment-hosts-list.component";

import * as SharedSvc from "app/services/domain/base.service";
import {AgentsUpgradeSummary, DeploymentHostExtended} from "app/services/domain/deployment-hosts.service";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";

@Component({
               selector   : "o3-deployment-hosts-summary-page",
               templateUrl: "./deployment-hosts-summary-page.component.html"
           })
export class DeploymentHostsSummaryPageComponent extends SharedSvc.BaseApplicationComponent
{
    @ViewChild("childList", {static: true}) hostsList: DeploymentHostsListComponent;

    hasRC  = false;
    hasRTM = false;

    hasNonActiveAgents                    = false;
    hasNonActiveAgentsForReleaseCandidate = false;
    hasNonActiveAgentsForRelease          = false;
    hasTerminatedAgents                   = false;

    private m_notifierHosts: SharedSvc.DbChangeNotifier<Models.DeploymentHost, DeploymentHostExtended>;

    private upgradeResults: AgentsUpgradeSummary;
    @ViewChild(DeploymentAgentUpgradeOverlay, {static: true}) upgradeOverlay: DeploymentAgentUpgradeOverlay;

    constructor(inj: Injector)
    {
        super(inj);

        this.m_notifierHosts = this.listenToDatabase(this.app.domain.deploymentHosts, () => this.refreshData(), () => this.refreshData());
        this.m_notifierHosts.subscribe(null, true, true, true);
    }

    public ngAfterViewInit()
    {
        super.ngAfterViewInit();

        this.refreshData();
    }

    async refreshData()
    {
        let imagesForReleaseCandidate = await this.app.domain.registryTaggedImages.reportByReleaseStatus(Models.RegistryImageReleaseStatus.ReleaseCandidate, Models.DeploymentRole.deployer);
        this.hasRC                    = imagesForReleaseCandidate.entries.length > 0;

        let imagesForRelease = await this.app.domain.registryTaggedImages.reportByReleaseStatus(Models.RegistryImageReleaseStatus.Release, Models.DeploymentRole.deployer);
        this.hasRTM          = imagesForRelease.entries.length > 0;

        let hasNonActiveAgents                        = false;
        let hasNonActiveAgentsRunningRelease          = false;
        let hasNonActiveAgentsRunningReleaseCandidate = false;
        let hasTerminatedAgents                       = false;

        let descriptors = await this.app.domain.deploymentHosts.getList();
        for (let desc of descriptors)
        {
            if (desc.flags.indexOf(Models.DeploymentHostStatusDescriptorFlag.NonActiveAgents) >= 0) hasNonActiveAgents = true;
            if (desc.flags.indexOf(Models.DeploymentHostStatusDescriptorFlag.NonActiveAgentsRunningReleaseCandidate) >= 0) hasNonActiveAgentsRunningReleaseCandidate = true;
            if (desc.flags.indexOf(Models.DeploymentHostStatusDescriptorFlag.NonActiveAgentsRunningRelease) >= 0) hasNonActiveAgentsRunningRelease = true;
            if (desc.flags.indexOf(Models.DeploymentHostStatusDescriptorFlag.TerminatedAgents) >= 0) hasTerminatedAgents = true;
        }

        this.hasNonActiveAgents                    = hasNonActiveAgents;
        this.hasNonActiveAgentsForReleaseCandidate = hasNonActiveAgentsRunningReleaseCandidate;
        this.hasNonActiveAgentsForRelease          = hasNonActiveAgentsRunningRelease;
        this.hasTerminatedAgents                   = hasTerminatedAgents;
    }

    @ReportError
    async startNewAgents(useRC: boolean,
                         onlyOperational: boolean)
    {
        let action: Models.DeploymentAgentUpgradeAction;

        if (onlyOperational)
        {
            action = useRC ? Models.DeploymentAgentUpgradeAction.StartOperationalAgentsWithReleaseCandidate : Models.DeploymentAgentUpgradeAction.StartOperationalAgentsWithRelease;
        }
        else
        {
            action = useRC ? Models.DeploymentAgentUpgradeAction.StartAgentsWithReleaseCandidate : Models.DeploymentAgentUpgradeAction.StartAgentsWithRelease;
        }

        let upgrade = Models.DeploymentAgentUpgrade.newInstance({action: action});

        this.upgradeResults = await this.app.domain.deploymentHosts.startNewAgents(this, upgrade);
        this.upgradeOverlay.open(this.upgradeResults);
    }

    @ReportError
    async activateNewAgents(useRC: boolean)
    {
        await this.app.domain.deploymentHosts.activateNewAgents(this.app, useRC);
    }

    async stopOldAgents()
    {
        await this.app.domain.deploymentHosts.stopOldAgents(this.app);
    }

    async removeOldAgents()
    {
        await this.app.domain.deploymentHosts.removeOldAgents(this.app);
    }

    //--//

    get canPrintLabels(): boolean
    {
        for (let host of this.hostsList.getAllSelected())
        {
            return true;
        }

        return false;
    }

    async printLabels()
    {
        let selected = [...this.hostsList.getAllSelected()];
        selected.sort((a,
                       b) => UtilsService.compareStrings(a.hostId, b.hostId, true));

        let hostIds = [];

        for (let host of selected)
        {
            hostIds.push(host.ri.sysId);
        }

        window.open(`#/provision-labels?hostIds=${hostIds.join(",")}`, "_blank");
    }

    get canRenameForCustomer(): boolean
    {
        for (let host of this.hostsList.getAllSelected())
        {
            if (!host.customerName)
            {
                return true;
            }
        }

        return false;
    }

    async renameForCustomer()
    {
        let dialogService = await CustomerSelectionDialogComponent.open(this, "associate the hosts with", "Assign");
        if (dialogService != null)
        {
            let count = 0;

            for (let host of this.hostsList.getAllSelected())
            {
                if (!host.customerName)
                {
                    await this.app.domain.apis.deploymentHosts.prepareForCustomer(host.ri.sysId, dialogService.ext.model.sysId);

                    this.hostsList.changeSelection(host.ri.sysId, false);
                    count++;
                }
            }

            this.app.framework.errors.success(`Updated ${count} hosts...`, -1);
        }
    }

    get canRenameForService(): boolean
    {
        for (let host of this.hostsList.getAllSelected())
        {
            if (!host.serviceName)
            {
                return true;
            }
        }

        return false;
    }

    async renameForService()
    {
        let dialogService = await CustomerServiceSelectionDialogComponent.open(this, "associate the hosts with", "Assign");
        if (dialogService != null)
        {
            let count = 0;

            for (let host of this.hostsList.getAllSelected())
            {
                if (!host.serviceName)
                {
                    await this.app.domain.apis.deploymentHosts.prepareForService(host.ri.sysId, dialogService.svc.model.sysId);

                    this.hostsList.changeSelection(host.ri.sysId, false);
                    count++;
                }
            }

            this.app.framework.errors.success(`Updated ${count} hosts...`, -1);
        }
    }

    async exportToExcel()
    {
        this.hostsList.exportToExcel();
    }
}
