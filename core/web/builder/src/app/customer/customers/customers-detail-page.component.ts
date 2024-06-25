import {Component, Injector, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";
import {ReportError} from "app/app.service";
import {DeploymentAgentUpgradeOverlay} from "app/customer/deployment-agents/deployment-agent-upgrade-overlay.component";
import {HostDecoded, HostsListDownloader} from "app/customer/deployment-hosts/deployment-hosts-list.component";
import {DashboardManagementService} from "app/dashboard/dashboard-management.service";

import * as SharedSvc from "app/services/domain/base.service";
import {CustomerExtended} from "app/services/domain/customers.service";
import {AgentsUpgradeSummary} from "app/services/domain/deployment-hosts.service";
import * as Models from "app/services/proxy/model/models";
import {DeploymentOperationalStatus} from "app/services/proxy/model/models";
import {Logger, LoggingService} from "framework/services/logging.service";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {inParallel, mapInParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-customers-detail-page",
               templateUrl: "./customers-detail-page.component.html",
               styleUrls  : ["./customers-detail-page.component.scss"]
           })
export class CustomersDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    private readonly logger: Logger;

    id: string;
    extended: CustomerExtended;
    extendedRemoveChecks: Models.ValidationResult[];
    extendedNoRemoveReason: string;

    private upgradeResults: AgentsUpgradeSummary;
    @ViewChild(DeploymentAgentUpgradeOverlay, {static: true}) upgradeOverlay: DeploymentAgentUpgradeOverlay;

    charges: Models.DeploymentCellularChargesSummary;

    hasRC  = false;
    hasRTM = false;

    get isNew(): boolean
    {
        return !this.id;
    }

    get isDirty(): boolean
    {
        if (this.modelForm && this.modelForm.dirty)
        {
            return true;
        }

        return false;
    }

    get isValid(): boolean
    {
        if (this.modelForm && !this.modelForm.valid)
        {
            return false;
        }

        return true;
    }

    @ViewChild("modelForm") modelForm: NgForm;

    //--//

    checkUsagesDialogConfig = OverlayConfig.onTopDraggable({
                                                               width : "50vw",
                                                               height: "90vh"
                                                           });

    checkUsagesFilter: string;
    checkUsagesMaxResults: number;
    checkUsagesCaseInsensitive: boolean;
    checkUsagesResults: string = "";

    //--//

    constructor(inj: Injector,
                logService: LoggingService)
    {
        super(inj);

        this.logger = logService.getLogger(CustomersDetailPageComponent);

        this.extended = this.app.domain.customers.allocateInstance();
    }

    protected async onNavigationComplete()
    {
        this.resetForms();

        this.id = this.getPathParameter("custId");

        this.loadData();
    }

    //--//

    async loadData()
    {
        if (!this.isNew)
        {
            let customers = this.app.domain.customers;

            customers.logger.debug(`Loading customer: ${this.id}`);
            let extended = await customers.getExtendedById(this.id);
            if (!extended)
            {
                this.exit();
                return;
            }

            this.inject(DashboardManagementService)
                .recordCustomer(extended);

            this.extended = extended;

            this.extendedRemoveChecks   = await this.extended.checkRemove();
            this.extendedNoRemoveReason = this.fromValidationToReason("Remove is disabled because:", this.extendedRemoveChecks);

            this.app.ui.navigation.breadcrumbCurrentLabel = extended.model.name;
            customers.logger.debug(`customer Loaded: ${JSON.stringify(this.extended.model)}`);

            let imagesForReleaseCandidate = await this.app.domain.registryTaggedImages.reportByReleaseStatus(Models.RegistryImageReleaseStatus.ReleaseCandidate, Models.DeploymentRole.deployer);
            this.hasRC                    = imagesForReleaseCandidate.entries.length > 0;

            let imagesForRelease = await this.app.domain.registryTaggedImages.reportByReleaseStatus(Models.RegistryImageReleaseStatus.Release, Models.DeploymentRole.deployer);
            this.hasRTM          = imagesForRelease.entries.length > 0;

            //--//

            this.removeAllDbSubscriptions();

            this.subscribeOneShot(extended,
                                  async (ext,
                                         action) =>
                                  {
                                      this.loadData();
                                  });
        }
    }

    //--//

    newService()
    {
        this.app.ui.navigation.push([
                                        "service",
                                        "new"
                                    ]);
    }

    newSharedUser()
    {
        this.app.ui.navigation.push([
                                        "user",
                                        "new"
                                    ]);
    }

    newSharedSecret()
    {
        this.app.ui.navigation.push([
                                        "secret",
                                        "new"
                                    ]);
    }

    async fetchCharges()
    {
        await this.waitUntilTrue(10, () => !!this.extended);

        if (!this.charges)
        {
            this.charges = await this.extended.getCharges(100);
        }
    }

    getChargesReport()
    {
        let timestamp = MomentHelper.fileNameFormat();

        let url = this.app.domain.apis.customers.getChargesReport__generateUrl(this.extended.model.sysId, `charges__${timestamp}.csv`);
        window.open(url, "_blank");
    }

    @ReportError
    async compactTimeSeries()
    {
        let services = [];

        for (let svc of await this.extended.getServices())
        {
            if (svc.model.operationalStatus == DeploymentOperationalStatus.operational)
            {
                try
                {
                    await svc.compactTimeSeries();
                    services.push(svc);
                }
                catch (e)
                {
                    // Ignore failures.
                }
            }
        }

        this.app.framework.errors.success(`Started compaction on ${services.length} services`, -1);
    }

    //--//

    @ReportError
    async upgradeToRC()
    {
        await this.app.domain.customerServices.upgradeBulk(await this.extended.getServices(), true, false, false);
    }

    @ReportError
    async upgradeToRTM()
    {
        await this.app.domain.customerServices.upgradeBulk(await this.extended.getServices(), false, false, false);
    }

    //--//

    async pushSharedUsers()
    {
        let busy    = 0;
        let refresh = 0;

        for (let svc of await this.extended.getServices())
        {
            if (svc.model.operationalStatus != Models.DeploymentOperationalStatus.operational)
            {
                this.logger.info(`Skipped ${svc.model.name} because service is not operational...`);
                continue;
            }

            if (!(await svc.isReadyForChange()))
            {
                this.logger.info(`Skipped ${svc.model.name} because service is busy...`);
                busy++;
                continue;
            }

            let state = await svc.getState();
            if (!state.hasTasks() || !state.hasHosts())
            {
                this.logger.info(`Skipped ${svc.model.name} because service is not running...`);
                continue;
            }

            await svc.refreshUsers();
            refresh++;
        }

        let msg = `Refreshing shared users on ${refresh} services`;

        if (busy > 0)
        {
            msg = `${msg}, skipped ${busy} because already busy`;
        }

        this.app.framework.errors.success(`${msg}...`, -1);
    }

    async pushSharedSecrets()
    {
        let busy    = 0;
        let refresh = 0;

        for (let svc of await this.extended.getServices())
        {
            if (svc.model.operationalStatus != Models.DeploymentOperationalStatus.operational)
            {
                this.logger.info(`Skipped ${svc.model.name} because service is not operational...`);
                continue;
            }

            if (!(await svc.isReadyForChange()))
            {
                this.logger.info(`Skipped ${svc.model.name} because service is busy...`);
                busy++;
                continue;
            }

            let state = await svc.getState();
            if (!state.hasTasks() || !state.hasHosts())
            {
                this.logger.info(`Skipped ${svc.model.name} because service is not running...`);
                continue;
            }

            await svc.refreshSecrets();
            refresh++;
        }

        let msg = `Refreshing shared secrets on ${refresh} services`;

        if (busy > 0)
        {
            msg = `${msg}, skipped ${busy} because already busy`;
        }

        this.app.framework.errors.success(`${msg}...`, -1);
    }

    //--//

    async save()
    {
        let extended = await this.app.domain.customers.save(this.extended);

        if (this.isNew)
        {
            this.app.ui.navigation.go("/customers/item", [
                extended.model.sysId
            ]);
        }
        else
        {
            this.id       = extended.model.sysId;
            this.extended = extended;

            await this.cancel();
        }
    }

    @ReportError
    async remove()
    {
        await this.extended.remove();
        this.exit();
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }

    async cancel()
    {
        if (this.isNew)
        {
            this.exit();
        }
        else
        {
            let extended = await this.extended.refresh<CustomerExtended>();

            this.resetForms();

            this.detectChanges();
            this.extended = extended;
            this.detectChanges();
        }
    }

    resetForms()
    {
        if (this.modelForm)
        {
            this.modelForm.resetForm();
        }

        this.detectChanges();
    }

    @ReportError
    async startNewAgents(useRC: boolean)
    {
        let upgrade = Models.DeploymentAgentUpgrade.newInstance({
                                                                    action  : useRC ? Models.DeploymentAgentUpgradeAction.StartAgentsWithReleaseCandidate : Models.DeploymentAgentUpgradeAction.StartAgentsWithRelease,
                                                                    customer: this.extended.getIdentity()
                                                                });

        this.upgradeResults = await this.app.domain.deploymentHosts.startNewAgents(this, upgrade);
        this.upgradeOverlay.open(this.upgradeResults);
    }

    //--//

    isValidCheckUsages()
    {
        return !!this.checkUsagesFilter;
    }

    async checkUsages()
    {
        this.checkUsagesResults = "";

        let filters = Models.UsageFilterRequest.newInstance({
                                                                items          : this.checkUsagesFilter.split(" "),
                                                                maxResults     : this.checkUsagesMaxResults,
                                                                caseInsensitive: this.checkUsagesCaseInsensitive
                                                            });

        await mapInParallel(await this.extended.getServices(), async (svc) =>
        {
            let response = await svc.checkUsages(filters);
            let lines    = svc.formatUsages(response, true);

            if (lines)
            {
                let append = `${svc.model.name}:\n`;

                for (let line of lines)
                {
                    append += `    ${line}\n`;
                }

                append += "\n";

                this.checkUsagesResults += append;
            }
        });

        if (this.checkUsagesResults == "")
        {
            this.checkUsagesResults = "No hits!";
        }
        else
        {
            this.checkUsagesResults += "--------------\n";
            this.checkUsagesResults += "Search done!";
        }
    }

    async exportToExcel()
    {
        let exts: HostDecoded[] = [];

        await inParallel(await this.extended.getServices(), async (svc) =>
        {
            let filter          = new Models.DeploymentHostFilterRequest();
            filter.serviceSysid = svc.model.sysId;

            let descriptors = await this.app.domain.deploymentHosts.getList(filter);
            for (let desc of descriptors)
            {
                exts.push(new HostDecoded(this.app.domain.deploymentHosts, desc));
            }
        });

        let fileName      = DownloadDialogComponent.fileName(`${this.extended.model.name}__hosts`, ".xlsx");
        const sheetName   = "Host List";
        let dataGenerator = new HostsListDownloader(this.app.domain.apis.exports, exts, fileName, sheetName);

        return DownloadDialogComponent.openWithGenerator(this, sheetName, fileName, dataGenerator);
    }
}
