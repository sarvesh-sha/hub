import {Component, Injector, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";

import {ReportError} from "app/app.service";
import {CustomerServiceSelectionDialogComponent} from "app/customer/customer-services/customer-service-selection-dialog.component";

import {ServiceRoleSelectionDialogComponent} from "app/customer/customer-services/service-role-selection-dialog.component";
import {DeploymentAgentUpgradeOverlay} from "app/customer/deployment-agents/deployment-agent-upgrade-overlay.component";
import {HostDecoded, HostsListDownloader} from "app/customer/deployment-hosts/deployment-hosts-list.component";
import {RegistryImageSelectionDialogComponent} from "app/customer/registry-images/registry-image-selection-dialog.component";

import {DashboardManagementService} from "app/dashboard/dashboard-management.service";

import {BackgroundActivityExtended} from "app/services/domain/background-activities.service";
import * as SharedSvc from "app/services/domain/base.service";
import {CustomerServiceExtended, UpgradeStatus} from "app/services/domain/customer-services.service";
import {CustomerExtended} from "app/services/domain/customers.service";
import {AgentsUpgradeSummary, DeploymentHostExtended} from "app/services/domain/deployment-hosts.service";
import {RegistryTaggedImageExtended, ReleaseStatusDetails} from "app/services/domain/registry-tagged-images.service";
import {UserExtended} from "app/services/domain/user-management.service";
import * as Models from "app/services/proxy/model/models";

import {Logger} from "framework/services/logging.service";
import {UtilsService} from "framework/services/utils.service";

import {ControlOption} from "framework/ui/control-option";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {NumberWithSeparatorsPipe} from "framework/ui/formatting/string-format.pipe";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";

import moment from "framework/utils/moment";

@Component({
               selector   : "o3-customer-services-detail-page",
               templateUrl: "./customer-services-detail-page.component.html"
           })
export class CustomerServicesDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    private readonly logger: Logger;

    id: string;
    extended: CustomerServiceExtended;
    extendedRemoveChecks: Models.ValidationResult[];
    extendedNoRemoveReason: string;
    extendedReady: boolean;

    private upgradeResults: AgentsUpgradeSummary;
    @ViewChild(DeploymentAgentUpgradeOverlay, {static: true}) upgradeOverlay: DeploymentAgentUpgradeOverlay;

    charges: Models.DeploymentCellularChargesSummary;

    upgradeBlockers: UpgradeBlocker[];
    blockUntil: Date;

    stalenessText: string[] = [];

    warningThreshold: number;
    alertThreshold: number;
    imagePruningThreshold: number;

    checkUsagesDialogConfig = OverlayConfig.onTopDraggable({
                                                               minWidth : 600,
                                                               maxHeight: "90vh"
                                                           });

    checkUsagesFilter: string;
    checkUsagesMaxResults: number;
    checkUsagesCaseInsensitive: boolean;
    checkUsagesResults: string = "";

    batteryThreshold = new Models.DeployerShutdownConfiguration();

    private upgradeStatus: UpgradeStatus;

    heapStatusHistoryOptions: ControlOption<string>[] = [];
    heapStatusHistorySelected: string;

    //--//

    get isNew(): boolean
    {
        return !this.id;
    }

    get isDirty(): boolean
    {
        if (this.modelForm?.dirty)
        {
            return true;
        }

        if (this.cloudForm?.dirty)
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

        if (this.cloudForm && !this.cloudForm.valid)
        {
            return false;
        }

        let model = this.extended?.model;
        if (!model.vertical || !model.instanceType)
        {
            return false;
        }

        switch (model.instanceType)
        {
            case Models.DeploymentInstance.Edge:
            case Models.DeploymentInstance.AZURE_EDGE:
            case Models.DeploymentInstance.DigitalMatter:
                break;

            default:
                if (!model.instanceRegion)
                {
                    return false;
                }
                break;
        }

        return true;
    }

    images: { [key: string]: RegistryTaggedImageExtended } = {};

    private m_pendingActivity: BackgroundActivityExtended;
    private m_pendingActivitySubscriptions: SharedSvc.DbChangeSubscription<Models.BackgroundActivity>[] = [];
    public pendingActivityInfo: string[];

    @ViewChild("modelForm") modelForm: NgForm;
    @ViewChild("cloudForm") cloudForm: NgForm;

    possibleActions: SharedSvc.ActionDescriptor[]                          = [];
    dbModes: ControlOption<Models.DatabaseMode>[]                          = [];
    instanceTypes: ControlOption<Models.DeploymentInstance>[]              = [];
    instanceAccounts: ControlOption<string>[]                              = [];
    instanceRegions: ControlOption<string>[]                               = [];
    customerVerticals: ControlOption<Models.CustomerVertical>[]            = [];
    operationalStates: ControlOption<Models.DeploymentOperationalStatus>[] = [];

    navigationComplete = false;

    constructor(inj: Injector)
    {
        super(inj);

        this.logger                           = this.app.domain.customerServices.logger.getLogger(CustomerServicesDetailPageComponent);
        this.extended                         = this.app.domain.customerServices.allocateInstance();
        this.extended.model.operationalStatus = Models.DeploymentOperationalStatus.operational;
    }

    protected async onNavigationComplete()
    {
        this.resetForms();

        this.id = this.getPathParameter("svcId");

        if (this.isNew)
        {
            this.extended.model.customer          = CustomerExtended.newIdentity(this.getPathParameter("custId"));
            this.extended.model.dbMode            = Models.DatabaseMode.MariaDB;
            this.extended.model.operationalStatus = Models.DeploymentOperationalStatus.operational;
        }

        this.navigationComplete = true;

        this.dbModes           = await this.app.domain.customerServices.getDbModes();
        this.instanceTypes     = await this.app.domain.customerServices.getInstanceTypes();
        this.customerVerticals = await this.app.domain.customerServices.getCustomerVerticals();
        this.operationalStates = await this.app.domain.deploymentHosts.getOperationalStates();

        this.loadData();
    }

    //--//

    async loadData()
    {
        let stalenessText: string[] = [];

        if (!this.isNew)
        {
            let extended = await this.app.domain.customerServices.getExtendedById(this.id);
            if (!extended)
            {
                this.exit();
                return;
            }

            this.inject(DashboardManagementService)
                .recordService(extended);

            this.extended = extended;

            this.extendedRemoveChecks   = await this.extended.checkRemove();
            this.extendedNoRemoveReason = this.fromValidationToReason("Remove is disabled because:", this.extendedRemoveChecks);

            this.upgradeBlockers = null;
            if (this.extended.model.upgradeBlockers && this.extended.model.upgradeBlockers.requests && this.extended.model.upgradeBlockers.requests.length > 0)
            {
                let upgradeBlockers = [];

                for (let req of this.extended.model.upgradeBlockers.requests)
                {
                    let user      = await this.app.domain.userManagement.getExtendedByIdentity(req.user);
                    let blocker   = new UpgradeBlocker();
                    blocker.user  = user;
                    blocker.until = req.until;
                    upgradeBlockers.push(blocker);
                }

                this.upgradeBlockers = upgradeBlockers;
            }

            this.updateRegions();

            let images  = await this.extended.getImages();
            this.images = images;

            for (let alert of this.extended.model.alertThresholds || [])
            {
                if (alert.role == Models.DeploymentRole.gateway)
                {
                    this.warningThreshold = alert.warningThreshold;
                    this.alertThreshold   = alert.alertThreshold;
                    break;
                }
            }

            if (this.extended.model.batteryThresholds)
            {
                this.batteryThreshold = this.extended.model.batteryThresholds;
            }

            //--//

            let imagesForReleaseCandidate = await this.app.domain.registryTaggedImages.reportByReleaseStatus(Models.RegistryImageReleaseStatus.ReleaseCandidate);
            let imagesForRelease          = await this.app.domain.registryTaggedImages.reportByReleaseStatus(Models.RegistryImageReleaseStatus.Release);

            let state = await this.extended.getState();

            let upgradeStatus = state.checkUpgrade(imagesForReleaseCandidate, imagesForRelease);

            let possibleActions: SharedSvc.ActionDescriptor[] = [];

            this.extendedReady = await this.extended.isReadyForChange();
            if (this.extendedReady)
            {
                let agentCreations   = 0;
                let imagePulls       = 0;
                let stale            = 0;
                let highestStaleness = 0;

                state.hostsPerRole.forEach((hosts,
                                            role) =>
                                           {
                                               let taskCreation = 0;

                                               for (let host of hosts)
                                               {
                                                   for (let ops of host.delayedOps)
                                                   {
                                                       if (ops instanceof Models.DelayedAgentCreation)
                                                       {
                                                           agentCreations++;
                                                       }
                                                       else if (ops instanceof Models.DelayedTaskCreation && ops.role == role)
                                                       {
                                                           taskCreation++;
                                                       }
                                                       else if (ops instanceof Models.DelayedImagePull)
                                                       {
                                                           imagePulls++;
                                                       }
                                                   }

                                                   switch (host.responsiveness)
                                                   {
                                                       case Models.DeploymentOperationalResponsiveness.UnresponsiveFullThreshold:
                                                       case Models.DeploymentOperationalResponsiveness.Unresponsive:
                                                           switch (host.operationalStatus)
                                                           {
                                                               case Models.DeploymentOperationalStatus.offline:
                                                               case Models.DeploymentOperationalStatus.idle:
                                                               case Models.DeploymentOperationalStatus.operational:
                                                               case Models.DeploymentOperationalStatus.maintenance:
                                                               case Models.DeploymentOperationalStatus.lostConnectivity:
                                                                   stale++;

                                                                   let stalenessInMilli   = DeploymentHostExtended.computeStalenessFromLastHeartbeat(host.lastHeartbeat);
                                                                   let stalenessInMinutes = Math.trunc(stalenessInMilli / (60 * 1000));
                                                                   highestStaleness       = Math.max(highestStaleness, stalenessInMinutes);
                                                                   break;
                                                           }
                                                           break;
                                                   }
                                               }

                                               if (taskCreation > 0)
                                               {
                                                   stalenessText.push(`${taskCreation} pending ${role} tasks`);
                                               }
                                           });

                if (agentCreations > 0)
                {
                    stalenessText.push(`${agentCreations} pending agents`);
                }

                if (imagePulls > 0)
                {
                    stalenessText.push(`${imagePulls} pending image pulls`);
                }

                if (stale > 0)
                {
                    let d = moment.duration(highestStaleness, "minutes")
                                  .humanize();

                    stalenessText.push(`Warning: ${stale} resources offline, some for more than ${d}...`);
                }

                if (!state.hasTasks())
                {
                    if (state.hasHosts())
                    {
                        SharedSvc.ActionDescriptor.addAtSecondLevel(possibleActions, "Manage State", "Launch...", "Start a service fresh", async () =>
                        {
                            let desiredState = Models.CustomerServiceDesiredState.newInstance({
                                                                                                  roles: []
                                                                                              });

                            if (!await state.enumerateHostsAsync(false,
                                                                 async (role,
                                                                        host) =>
                                                                 {
                                                                     let arch = host.architecture;
                                                                     if (!extended.findDesiredState(desiredState, role, arch))
                                                                     {
                                                                         let image = await this.selectImageForRole(role, host);
                                                                         if (image == null) return false;

                                                                         let roleSpec      = extended.addImageToDesiredState(desiredState, role, arch, image);
                                                                         roleSpec.shutdown = true;
                                                                         roleSpec.launch   = true;
                                                                     }

                                                                     return true;
                                                                 }))
                            {
                                return;
                            }

                            await this.confirmChanges(extended, desiredState, "Click Yes to confirm launch.");
                        });

                        if (upgradeStatus.hasAllImagesForRelease)
                        {
                            SharedSvc.ActionDescriptor.addAtSecondLevel(possibleActions, "Manage State", "Launch Release Build", "Start a service fresh", async () =>
                            {
                                let desiredState = Models.CustomerServiceDesiredState.newInstance({
                                                                                                      roles: []
                                                                                                  });

                                if (!await state.enumerateHostsAsync(false,
                                                                     async (role,
                                                                            host) =>
                                                                     {
                                                                         let arch = host.architecture;
                                                                         if (!extended.findDesiredState(desiredState, role, arch))
                                                                         {
                                                                             let targetRelease = upgradeStatus.imagesForRelease.findMatch(role, arch);
                                                                             if (targetRelease == null) return false;

                                                                             let image = targetRelease.image;
                                                                             if (!image) return false;

                                                                             let roleSpec      = extended.addImageToDesiredState(desiredState, role, arch, image);
                                                                             roleSpec.shutdown = true;
                                                                             roleSpec.launch   = true;
                                                                         }

                                                                         return true;
                                                                     }))
                                {
                                    return;
                                }

                                await this.confirmChanges(extended, desiredState, "Click Yes to confirm launch.");
                            });
                        }

                        if (upgradeStatus.hasAnyImagesForReleaseCandidate && upgradeStatus.hasAllImagesForReleaseCandidate)
                        {
                            SharedSvc.ActionDescriptor.addAtSecondLevel(possibleActions, "Manage State", "Launch Release Candidate Build", "Start a service fresh", async () =>
                            {
                                let desiredState = Models.CustomerServiceDesiredState.newInstance({
                                                                                                      roles: []
                                                                                                  });

                                if (!await state.enumerateHostsAsync(false,
                                                                     async (role,
                                                                            host) =>
                                                                     {
                                                                         let arch = host.architecture;
                                                                         if (!extended.findDesiredState(desiredState, role, arch))
                                                                         {
                                                                             let targetRelease = upgradeStatus.imagesForReleaseCandidate.findMatch(role, arch);
                                                                             if (!targetRelease)
                                                                             {
                                                                                 targetRelease = upgradeStatus.imagesForRelease.findMatch(role, arch);
                                                                                 if (!targetRelease) return false;
                                                                             }

                                                                             let image = targetRelease.image;
                                                                             if (!image) return false;

                                                                             let roleSpec      = extended.addImageToDesiredState(desiredState, role, arch, image);
                                                                             roleSpec.shutdown = true;
                                                                             roleSpec.launch   = true;
                                                                         }

                                                                         return true;
                                                                     }))
                                {
                                    return;
                                }

                                await this.confirmChanges(extended, desiredState, "Click Yes to confirm launch.");
                            });
                        }
                    }
                }
                else
                {
                    let noTasksOnHost = state.getRolesWithNoTasksOnHost();
                    if (noTasksOnHost.size > 0)
                    {
                        SharedSvc.ActionDescriptor.addAtSecondLevel(possibleActions, "Manage State", "Launch remaining", "Launch roles with no tasks", async () =>
                        {
                            let desiredState = Models.CustomerServiceDesiredState.newInstance({
                                                                                                  roles: []
                                                                                              });

                            if (!await state.enumerateHostsAsync(false,
                                                                 async (role,
                                                                        host) =>
                                                                 {
                                                                     let arch = host.architecture;
                                                                     if (!extended.findDesiredState(desiredState, role, arch))
                                                                     {
                                                                         let image = extended.findExistingImage(images, role, arch);
                                                                         if (image == null)
                                                                         {
                                                                             image = await this.selectImageForRole(role, host);
                                                                             if (image == null) return false;
                                                                         }

                                                                         let roleSpec                 = extended.addImageToDesiredState(desiredState, role, arch, image);
                                                                         roleSpec.shutdownIfDifferent = true;
                                                                         roleSpec.launchIfMissing     = true;
                                                                     }

                                                                     return true;
                                                                 }))
                            {
                                return;
                            }

                            await this.confirmChanges(extended, desiredState, "Click Yes to confirm refresh.");
                        });
                    }

                    if (!extended.model.relaunchAlways)
                    {
                        SharedSvc.ActionDescriptor.addAtSecondLevel(possibleActions, "Manage State", "Refresh certificate", "Redeploy Web frontend to refresh SSL certificate", async () =>
                        {
                            await extended.refreshCertificate();
                            await this.loadData();
                        });

                        SharedSvc.ActionDescriptor.addAtSecondLevel(possibleActions, "Manage State", "Upgrade...", "Take a backup and upgrade the service", async () =>
                        {
                            let desiredState = Models.CustomerServiceDesiredState.newInstance({
                                                                                                  roles       : [],
                                                                                                  createBackup: Models.BackupKind.Upgrade
                                                                                              });

                            if (!await state.enumerateHostsAsync(false,
                                                                 async (role,
                                                                        host) =>
                                                                 {
                                                                     let arch = host.architecture;
                                                                     if (!extended.findDesiredState(desiredState, role, arch))
                                                                     {
                                                                         let image = await this.selectImageForRole(role, host);
                                                                         if (image == null) return false;

                                                                         let roleSpec      = extended.addImageToDesiredState(desiredState, role, arch, image);
                                                                         roleSpec.shutdown = true;
                                                                         roleSpec.launch   = true;
                                                                     }

                                                                     return true;
                                                                 }))
                            {
                                return;
                            }

                            await this.confirmChanges(extended, desiredState, "Click Yes to confirm upgrade.");
                        });

                        if (upgradeStatus.hasAllImagesForRelease)
                        {
                            SharedSvc.ActionDescriptor.addAtSecondLevel(possibleActions, "Manage State", "Upgrade To Release", "Take a backup and upgrade the service", async () =>
                            {
                                let desiredState = Models.CustomerServiceDesiredState.newInstance({
                                                                                                      roles       : [],
                                                                                                      createBackup: Models.BackupKind.Upgrade
                                                                                                  });

                                if (!await state.enumerateHostsAsync(false,
                                                                     async (role,
                                                                            host) =>
                                                                     {
                                                                         let arch = host.architecture;
                                                                         if (!extended.findDesiredState(desiredState, role, arch))
                                                                         {
                                                                             let targetRelease = upgradeStatus.imagesForRelease.findMatch(role, arch);
                                                                             if (targetRelease == null) return false;

                                                                             let image = targetRelease.image;
                                                                             if (!image) return false;

                                                                             let roleSpec      = extended.addImageToDesiredState(desiredState, role, arch, image);
                                                                             roleSpec.shutdown = true;
                                                                             roleSpec.launch   = true;
                                                                         }

                                                                         return true;
                                                                     }))
                                {
                                    return;
                                }

                                await this.confirmChanges(extended, desiredState, "Click Yes to confirm upgrade.");
                            });
                        }

                        if (upgradeStatus.hasAnyImagesForReleaseCandidate && upgradeStatus.hasAllImagesForReleaseCandidate)
                        {
                            SharedSvc.ActionDescriptor.addAtSecondLevel(possibleActions, "Manage State", "Upgrade To Release Candidate", "Take a backup and upgrade the service", async () =>
                            {
                                let desiredState = Models.CustomerServiceDesiredState.newInstance({
                                                                                                      roles       : [],
                                                                                                      createBackup: Models.BackupKind.Upgrade
                                                                                                  });

                                if (!await state.enumerateHostsAsync(false,
                                                                     async (role,
                                                                            host) =>
                                                                     {
                                                                         let arch = host.architecture;
                                                                         if (!extended.findDesiredState(desiredState, role, arch))
                                                                         {
                                                                             let targetRelease = upgradeStatus.imagesForReleaseCandidate.findMatch(role, arch);
                                                                             if (!targetRelease)
                                                                             {
                                                                                 targetRelease = upgradeStatus.imagesForRelease.findMatch(role, arch);
                                                                                 if (!targetRelease) return false;
                                                                             }

                                                                             let image = targetRelease.image;
                                                                             if (!image) return false;

                                                                             let roleSpec      = extended.addImageToDesiredState(desiredState, role, arch, image);
                                                                             roleSpec.shutdown = true;
                                                                             roleSpec.launch   = true;
                                                                         }

                                                                         return true;
                                                                     }))
                                {
                                    return;
                                }

                                await this.confirmChanges(extended, desiredState, "Click Yes to confirm upgrade.");
                            });
                        }

                        SharedSvc.ActionDescriptor.addAtSecondLevel(possibleActions, "Manage State", "Hub Refresh...", "Take a backup and relaunch the hub, leaving gateways untouched", async () =>
                        {
                            let desiredState = Models.CustomerServiceDesiredState.newInstance({
                                                                                                  roles       : [],
                                                                                                  createBackup: Models.BackupKind.Upgrade
                                                                                              });

                            if (!await state.enumerateHostsAsync(false,
                                                                 async (role,
                                                                        host) =>
                                                                 {
                                                                     let arch = host.architecture;
                                                                     if (!extended.findDesiredState(desiredState, role, arch))
                                                                     {
                                                                         if (role == Models.DeploymentRole.hub)
                                                                         {
                                                                             let image = await this.selectImageForRole(role, host);
                                                                             if (image == null) return false;

                                                                             let roleSpec      = extended.addImageToDesiredState(desiredState, role, arch, image);
                                                                             roleSpec.shutdown = true;
                                                                             roleSpec.launch   = true;
                                                                         }
                                                                         else
                                                                         {
                                                                             let image = extended.findExistingImage(images, role, arch);
                                                                             if (image == null) return false;

                                                                             let roleSpec = extended.addImageToDesiredState(desiredState, role, arch, image);
                                                                             // Don't start or stop, just create the role.
                                                                         }
                                                                     }

                                                                     return true;
                                                                 }))
                            {
                                return;
                            }

                            await this.confirmChanges(extended, desiredState, "Click Yes to confirm Hub refresh.");
                        });

                        SharedSvc.ActionDescriptor.addAtSecondLevel(possibleActions, "Manage State", "Gateway Refresh...", "Relaunch just the gateways, leaving the rest untouched", async () =>
                        {
                            let desiredState = Models.CustomerServiceDesiredState.newInstance({
                                                                                                  roles: []
                                                                                              });

                            if (!await state.enumerateHostsAsync(false,
                                                                 async (role,
                                                                        host) =>
                                                                 {
                                                                     let arch = host.architecture;
                                                                     if (!extended.findDesiredState(desiredState, role, arch))
                                                                     {
                                                                         if (role == Models.DeploymentRole.gateway)
                                                                         {
                                                                             let image = await this.selectImageForRole(role, host);
                                                                             if (image == null) return false;

                                                                             let roleSpec                 = extended.addImageToDesiredState(desiredState, role, arch, image);
                                                                             roleSpec.shutdownIfDifferent = true;
                                                                             roleSpec.launch              = true;
                                                                         }
                                                                         else
                                                                         {
                                                                             let image = extended.findExistingImage(images, role, arch);
                                                                             if (image == null) return false;

                                                                             let roleSpec = extended.addImageToDesiredState(desiredState, role, arch, image);
                                                                             // Don't start or stop, just create the role.
                                                                         }
                                                                     }

                                                                     return true;
                                                                 }))
                            {
                                return;
                            }

                            await this.confirmChanges(extended, desiredState, "Click Yes to confirm Gateway refresh.");
                        });

                        SharedSvc.ActionDescriptor.addAtSecondLevel(possibleActions,
                                                                    "Manage State",
                                                                    "Prepare For Host Migration...",
                                                                    "Take a backup and terminate the hub, leaving gateways untouched",
                                                                    async () =>
                                                                    {
                                                                        let desiredState = Models.CustomerServiceDesiredState.newInstance({
                                                                                                                                              roles       : [],
                                                                                                                                              createBackup: Models.BackupKind.HostMigration
                                                                                                                                          });

                                                                        if (!await state.enumerateHostsAsync(false,
                                                                                                             async (role,
                                                                                                                    host) =>
                                                                                                             {
                                                                                                                 let arch = host.architecture;
                                                                                                                 if (!extended.findDesiredState(desiredState, role, arch))
                                                                                                                 {
                                                                                                                     let image = extended.findExistingImage(images, role, arch);
                                                                                                                     if (image == null) return false;

                                                                                                                     let roleSpec = extended.addImageToDesiredState(desiredState, role, arch, image);

                                                                                                                     if (role == Models.DeploymentRole.gateway)
                                                                                                                     {
                                                                                                                         // Don't start or stop, just create the role.
                                                                                                                     }
                                                                                                                     else
                                                                                                                     {
                                                                                                                         roleSpec.shutdown = true;
                                                                                                                     }
                                                                                                                 }

                                                                                                                 return true;
                                                                                                             }))
                                                                        {
                                                                            return;
                                                                        }

                                                                        await this.confirmChanges(extended, desiredState, "Click Yes to confirm Host Migration.");
                                                                    });

                        SharedSvc.ActionDescriptor.addAtSecondLevel(possibleActions, "Manage State", "Migrate host", "Move instance between clouds, leaving gateways untouched", async () =>
                        {
                            if (await this.confirmOperation(`Click Yes to confirm cloud migration.`))
                            {
                                await extended.migrate();
                            }
                        });
                    }

                    SharedSvc.ActionDescriptor.addAtSecondLevel(possibleActions, "Manage State", "Relaunch...", "Restart a service fresh, all data erased", async () =>
                    {
                        let desiredState = Models.CustomerServiceDesiredState.newInstance({
                                                                                              roles: []
                                                                                          });

                        if (!await state.enumerateHostsAsync(false,
                                                             async (role,
                                                                    host) =>
                                                             {
                                                                 let arch = host.architecture;
                                                                 if (!extended.findDesiredState(desiredState, role, arch))
                                                                 {
                                                                     let image = await this.selectImageForRole(role, host);
                                                                     if (image == null) return false;

                                                                     let roleSpec      = extended.addImageToDesiredState(desiredState, role, arch, image);
                                                                     roleSpec.shutdown = true;
                                                                     roleSpec.launch   = true;
                                                                 }

                                                                 return true;
                                                             }))
                        {
                            return;
                        }

                        await this.confirmChanges(extended, desiredState, "Click Yes to confirm relaunch (all current data will be lost).");
                    });

                    if (upgradeStatus.hasAllImagesForRelease)
                    {
                        SharedSvc.ActionDescriptor.addAtSecondLevel(possibleActions, "Manage State", "Relaunch With Release", "Restart a service fresh, all data erased", async () =>
                        {
                            let desiredState = Models.CustomerServiceDesiredState.newInstance({
                                                                                                  roles: []
                                                                                              });

                            if (!await state.enumerateHostsAsync(false,
                                                                 async (role,
                                                                        host) =>
                                                                 {
                                                                     let arch = host.architecture;
                                                                     if (!extended.findDesiredState(desiredState, role, arch))
                                                                     {
                                                                         let targetRelease = upgradeStatus.imagesForRelease.findMatch(role, arch);
                                                                         if (targetRelease == null) return false;

                                                                         let image = targetRelease.image;
                                                                         if (!image) return false;

                                                                         let roleSpec      = extended.addImageToDesiredState(desiredState, role, arch, image);
                                                                         roleSpec.shutdown = true;
                                                                         roleSpec.launch   = true;
                                                                     }

                                                                     return true;
                                                                 }))
                            {
                                return;
                            }

                            await this.confirmChanges(extended, desiredState, "Click Yes to confirm relaunch (all current data will be lost).");
                        });
                    }

                    if (upgradeStatus.hasAnyImagesForReleaseCandidate && upgradeStatus.hasAllImagesForReleaseCandidate)
                    {
                        SharedSvc.ActionDescriptor.addAtSecondLevel(possibleActions, "Manage State", "Relaunch With Release Candidate", "Restart a service fresh, all data erased", async () =>
                        {
                            let desiredState = Models.CustomerServiceDesiredState.newInstance({
                                                                                                  roles: []
                                                                                              });

                            if (!await state.enumerateHostsAsync(false,
                                                                 async (role,
                                                                        host) =>
                                                                 {
                                                                     let arch = host.architecture;
                                                                     if (!extended.findDesiredState(desiredState, role, arch))
                                                                     {
                                                                         let targetRelease = upgradeStatus.imagesForReleaseCandidate.findMatch(role, arch);
                                                                         if (!targetRelease)
                                                                         {
                                                                             targetRelease = upgradeStatus.imagesForRelease.findMatch(role, arch);
                                                                             if (!targetRelease) return false;
                                                                         }

                                                                         let image = targetRelease.image;
                                                                         if (!image) return false;

                                                                         let roleSpec      = extended.addImageToDesiredState(desiredState, role, arch, image);
                                                                         roleSpec.shutdown = true;
                                                                         roleSpec.launch   = true;
                                                                     }

                                                                     return true;
                                                                 }))
                            {
                                return;
                            }

                            await this.confirmChanges(extended, desiredState, "Click Yes to confirm relaunch (all current data will be lost).");
                        });
                    }

                    SharedSvc.ActionDescriptor.addAtSecondLevel(possibleActions, "Manage State", "Shutdown", "Kill a service, without a backup", async () =>
                    {
                        let desiredState = Models.CustomerServiceDesiredState.newInstance({
                                                                                              roles: []
                                                                                          });

                        await this.confirmChanges(extended, desiredState, "Click Yes to confirm shutdown.");
                    });
                }
            }

            if (this.isAdmin && imagesForReleaseCandidate.entries.length > 0)
            {
                SharedSvc.ActionDescriptor.addAtSecondLevel(possibleActions, "Manage State", "New Agents From RC Build", "Start new Agent from RC build", async () =>
                {
                    let upgrade = Models.DeploymentAgentUpgrade.newInstance({
                                                                                action : Models.DeploymentAgentUpgradeAction.StartAgentsWithReleaseCandidate,
                                                                                service: this.extended.getIdentity()
                                                                            });

                    this.upgradeResults = await this.app.domain.deploymentHosts.startNewAgents(this, upgrade);
                    this.upgradeOverlay.open(this.upgradeResults);
                });
            }

            if (this.isAdmin && imagesForRelease.entries.length > 0)
            {
                SharedSvc.ActionDescriptor.addAtSecondLevel(possibleActions, "Manage State", "New Agents From RTM Build", "Start new Agent from RTM build", async () =>
                {
                    let upgrade = Models.DeploymentAgentUpgrade.newInstance({
                                                                                action : Models.DeploymentAgentUpgradeAction.StartAgentsWithRelease,
                                                                                service: this.extended.getIdentity()
                                                                            });

                    this.upgradeResults = await this.app.domain.deploymentHosts.startNewAgents(this, upgrade);
                    this.upgradeOverlay.open(this.upgradeResults);
                });
            }

            this.possibleActions = possibleActions;
            this.upgradeStatus   = upgradeStatus;

            this.app.ui.navigation.breadcrumbCurrentLabel = extended.model.name;
            this.logger.debug(`Loaded: ${JSON.stringify(this.extended.model)}`);

            //--//

            this.removeAllDbSubscriptions();

            this.subscribeOneShot(this.extended,
                                  async (ext,
                                         action) =>
                                  {
                                      this.loadData();
                                  });

            this.monitorActivity();
        }

        this.stalenessText = stalenessText;
    }

    @ReportError
    async rollbackForeign()
    {
        let backup = await this.extended.getLatestBackup();
        if (!backup)
        {
            return;
        }

        let dialogService = await CustomerServiceSelectionDialogComponent.open(this, `rollback to backup '${backup.model.fileId}'`, "Select", Models.DeploymentOperationalStatus.idle);
        if (dialogService == null)
        {
            return;
        }

        let svc = dialogService.svc;
        if (svc == null)
        {
            return;
        }

        await backup.rollbackToService(this, svc);
    }

    rollbackAndUpgradeForeign()
    {
        this.rollbackAndUpgradeForeignImpl();
    }

    rollbackAndUpgradeForeignToRC()
    {
        this.rollbackAndUpgradeForeignImpl([
                                               this.upgradeStatus?.imagesForReleaseCandidate,
                                               this.upgradeStatus?.imagesForRelease
                                           ]);
    }

    rollbackAndUpgradeForeignToRTM()
    {
        this.rollbackAndUpgradeForeignImpl([this.upgradeStatus?.imagesForRelease]);
    }

    @ReportError
    private async rollbackAndUpgradeForeignImpl(images?: ReleaseStatusDetails[])
    {
        let backup = await this.extended.getLatestBackup();
        if (!backup)
        {
            return;
        }

        let dialogService = await CustomerServiceSelectionDialogComponent.open(this, `rollback to backup '${backup.model.fileId}' and upgrade`, "Select", Models.DeploymentOperationalStatus.idle);
        if (dialogService == null)
        {
            return;
        }

        let svc = dialogService.svc;
        if (svc == null)
        {
            return;
        }

        await backup.rollbackAndUpgradeToService(this, svc, images);
    }

    //--//

    compactTimeSeries()
    {
        this.extended.compactTimeSeries();
    }

    //--//

    isValidBlocker()
    {
        return this.blockUntil && this.blockUntil > new Date();
    }

    updateBlocker(blockUntil?: Date)
    {
        this.extended.updateUpgradeBlocker(blockUntil);
    }

    //--//

    areValidAlertThresholds()
    {
        return this.warningThreshold > 0 && this.alertThreshold > 0;
    }

    updateAlertThreshold()
    {
        this.extended.updateAlertThreshold(Models.DeploymentRole.gateway, this.warningThreshold, this.alertThreshold);
    }

    //--//

    areValidBatteryThresholds()
    {
        return this.batteryThreshold.turnOnVoltage >= this.batteryThreshold.turnOffVoltage;
    }

    updateBatteryThreshold()
    {
        this.extended.updateBatteryThreshold(this.batteryThreshold);
    }

    //--//

    isValidPruningThreshold()
    {
        return this.imagePruningThreshold > 2;
    }

    async pruneImages()
    {
        let state = await this.extended.getState();

        let seen = new Set<String>();

        await state.enumerateHostsAsync(false,
                                        async (role,
                                               host) =>
                                        {
                                            if (!seen.has(host.ri.sysId))
                                            {
                                                seen.add(host.ri.sysId);

                                                let hostExt = await this.app.domain.deploymentHosts.getExtendedById(host.ri.sysId);
                                                hostExt.pruneImages(this.imagePruningThreshold);
                                            }
                                            return true;
                                        });

        this.app.framework.errors.success(`Started pruning on ${seen.size} hosts...`, -1);
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

        let response            = await this.extended.checkUsages(filters);
        let lines               = this.extended.formatUsages(response, false);
        this.checkUsagesResults = lines.join("\n");
    }

    //--//

    @ReportError
    private async confirmChanges(extended: CustomerServiceExtended,
                                 desiredState: Models.CustomerServiceDesiredState,
                                 reason: string)
    {
        let html = reason;

        if (UtilsService.countKeysInMap(desiredState.roles) > 0)
        {
            html += "<div class=\"row\">";
            html += "   <div class=\"clearfix\">&nbsp;</div>";
            html += "   <table class='table table-striped table-bordered selectable'>";
            html += "       <thead>";
            html += "       <tr>";
            html += "           <th>Role</th>";
            html += "           <th>Architecture</th>";
            html += "           <th>Image</th>";
            html += "       </tr>";
            html += "       </thead>";
            html += "       <tbody>";

            for (let imageSpec of desiredState.roles)
            {
                let image = await this.app.domain.registryTaggedImages.getExtendedByIdentity(imageSpec.image);

                html += "<tr>";
                html += `   <td>${imageSpec.role}</td>`;
                html += `   <td>${imageSpec.architecture}</td>`;
                html += `   <td>${image.model.tag}</td>`;
                html += "</tr>";
            }

            html += "       </tbody>";
            html += "   </table>";
            html += "</div>";
        }

        if (await this.confirmOperation(this.sanitizeHtml(html)))
        {
            await extended.applyDesiredState(desiredState);
            this.loadData();
        }
    }

    @ReportError
    async newResource()
    {
        let arch = Models.DockerImageArchitecture.X86;

        switch (this.extended.model.instanceType)
        {
            case Models.DeploymentInstance.AWS_T4G_Small:
            case Models.DeploymentInstance.AWS_T4G_Medium:
            case Models.DeploymentInstance.AWS_T4G_Large:
            case Models.DeploymentInstance.AWS_T4G_XLarge:
                arch = Models.DockerImageArchitecture.ARM64v8;
                break;
        }

        let map = new Map<Models.DeploymentRole, boolean>();
        for (let role of this.extended.model.purposes)
        {
            map.set(role, false);
        }

        let roles = await ServiceRoleSelectionDialogComponent.open(this, map, "resource", "Select");
        if (roles == null) return;

        let image = await RegistryImageSelectionDialogComponent.open(this, arch, [Models.DeploymentRole.deployer], "deploy for the agent", "Deploy");
        if (image == null) return;

        this.logger.debug(`Selected image ${JSON.stringify(image.taggedImage.model)}`);

        let cfg = Models.DeploymentHostConfig.newInstance({
                                                              imageId: image.taggedImage.model.sysId,
                                                              roles  : roles.map((item) => item.role)
                                                          });

        let host = await this.extended.deploy(cfg);
        this.logger.debug(`Preparing to deploy a new host: ${host.model.hostId}`);
        this.app.ui.navigation.go("/deployments/item", [host.model.sysId]);
    }

    @ReportError
    async newVirtualGateway()
    {
        if (await this.confirmOperation(`Are you sure you want to create a new virtual gateway?`))
        {
            let cfg = Models.DeploymentHostConfig.newInstance({
                                                                  instanceType: Models.DeploymentInstance.AZURE_EDGE,
                                                                  roles       : [Models.DeploymentRole.gateway]
                                                              });

            let host = await this.extended.deploy(cfg);
            this.logger.debug(`Preparing to deploy a new host: ${host.model.hostId}`);
            this.app.ui.navigation.go("/deployments/item", [host.model.sysId]);
        }
    }

    async newBackup()
    {
        await this.extended.startBackup();
        this.loadData();
    }

    newSecret()
    {
        this.app.ui.navigation.push([
                                        "secret",
                                        "new"
                                    ]);
    }

    async showLog()
    {
        let cust = await this.extended.getOwningCustomer();

        this.app.ui.navigation.go("/customers/item", [
            cust.model.sysId,
            "service",
            this.extended.model.sysId,
            "log"
        ]);
    }

    navigateToSite(url: string)
    {
        window.open(url, "_blank");
    }

    async updateRegions()
    {
        let accounts: string[];
        let regions: string[];

        if (this.extended?.model?.instanceType)
        {
            accounts = await this.app.domain.apis.customerServices.getAvailableAccounts(this.extended.model.instanceType);
            regions  = await this.app.domain.apis.customerServices.getAvailableRegions(this.extended.model.instanceType);
        }
        else
        {
            accounts = [];
            regions  = [];
        }

        this.instanceAccounts = accounts.map((name) =>
                                             {
                                                 let option = new ControlOption<string>();

                                                 option.id    = name;
                                                 option.label = name;

                                                 return option;
                                             });

        this.instanceRegions = regions.map((name) =>
                                           {
                                               let option = new ControlOption<string>();

                                               option.id    = name;
                                               option.label = name;

                                               return option;
                                           });

    }

    async pushSecrets()
    {
        let busy    = 0;
        let refresh = 0;

        let svc = this.extended;

        if (svc.model.operationalStatus != Models.DeploymentOperationalStatus.operational)
        {
            this.logger.info(`Skipped ${svc.model.name} because service is not operational...`);
            return;
        }

        if (!(await svc.isReadyForChange()))
        {
            this.logger.info(`Skipped ${svc.model.name} because service is busy...`);
            busy++;
            return;
        }

        let state = await svc.getState();
        if (!state.hasTasks() || !state.hasHosts())
        {
            this.logger.info(`Skipped ${svc.model.name} because service is not running...`);
            return;
        }

        await svc.refreshSecrets();
        refresh++;

        let msg = `Refreshing secrets on ${refresh} services`;

        if (busy > 0)
        {
            msg = `${msg}, skipped ${busy} because already busy`;
        }

        this.app.framework.errors.success(`${msg}...`, -1);
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

        let url = this.app.domain.apis.customerServices.getChargesReport__generateUrl(this.extended.model.sysId, `charges__${timestamp}.csv`);
        window.open(url, "_blank");
    }

    async exportToExcel()
    {
        let filter          = new Models.DeploymentHostFilterRequest();
        filter.serviceSysid = this.extended.model.sysId;

        let descriptors = await this.app.domain.deploymentHosts.getList(filter);
        let exts        = descriptors.map(desc => new HostDecoded(this.app.domain.deploymentHosts, desc));

        let fileName      = DownloadDialogComponent.fileName(`${this.extended.model.name}__hosts`, ".xlsx");
        const sheetName   = "Host List";
        let dataGenerator = new HostsListDownloader(this.app.domain.apis.exports, exts, fileName, sheetName);

        return DownloadDialogComponent.openWithGenerator(this, sheetName, fileName, dataGenerator);
    }

    //--//

    async fetchHeapStatusHistory()
    {
        await this.waitUntilTrue(10, () => !!this.extended);

        let entries = await this.extended.getHeapStatusHistory();

        this.heapStatusHistorySelected = null;
        this.heapStatusHistoryOptions  = entries.map((entry) =>
                                                     {
                                                         let text = "";

                                                         text += `Memory Max  : ${entry.memoryMax}\n`;
                                                         text += `Memory Total: ${entry.memoryTotal}\n`;
                                                         text += `Memory Free : ${entry.memoryFree}\n`;
                                                         text += `Memory Used : ${entry.memoryUsed}\n`;

                                                         for (let uniqueStackTrace of entry.uniqueStackTraces)
                                                         {
                                                             text += `\nFound ${uniqueStackTrace.threads.length} unique stack traces:\n`;
                                                             for (let thread of uniqueStackTrace.threads)
                                                             {
                                                                 text += `  ${thread}\n`;
                                                             }

                                                             for (let frame of uniqueStackTrace.frames)
                                                             {
                                                                 text += `    ${frame}\n`;
                                                             }
                                                         }

                                                         let timestamp  = MomentHelper.parse(entry.timestamp)
                                                                                      .format("L LTS");
                                                         let memoryUsed = NumberWithSeparatorsPipe.format(entry.memoryUsed);
                                                         return new ControlOption<string>(text, `${timestamp} / ${memoryUsed} bytes`);
                                                     });
    }

    @ReportError
    async save()
    {
        if (this.isNew)
        {
            let model = this.extended.model;
            if (model.vertical == Models.CustomerVertical.None)
            {
                model.operationalStatus = Models.DeploymentOperationalStatus.idle;
                model.disableEmails     = true;
                model.disableTexts      = true;
            }
        }

        let extended = await this.app.domain.customerServices.save(this.extended);

        if (this.isNew)
        {
            let cust = await extended.getOwningCustomer();

            this.app.ui.navigation.go("/customers/item", [
                cust.model.sysId,
                "service",
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
            await this.extended.refresh<CustomerServiceExtended>();

            this.resetForms();

            await this.loadData();
            this.detectChanges();
        }
    }

    resetForms()
    {
        if (this.modelForm)
        {
            this.modelForm.resetForm();
        }

        if (this.cloudForm)
        {
            this.cloudForm.resetForm();
        }

        this.detectChanges();
    }

    //--//

    private async selectImageForRole(role: Models.DeploymentRole,
                                     host: Models.DeploymentHostStatusDescriptor): Promise<RegistryTaggedImageExtended>
    {
        let dialogRes = await RegistryImageSelectionDialogComponent.open(this, host.architecture, [role], `deploy for ${role}`, "Deploy");

        return dialogRes && dialogRes.taggedImage ? dialogRes.taggedImage : null;
    }

    private async monitorActivity()
    {
        this.unsubscribeActivities();

        this.extended.resetAllCachedValues();

        let newPendingActivityInfo = null;

        this.m_pendingActivity = await this.extended.getCurrentActivity();
        if (this.m_pendingActivity)
        {
            switch (this.m_pendingActivity.model.status)
            {
                case Models.BackgroundActivityStatus.COMPLETED:
                case Models.BackgroundActivityStatus.FAILED:
                    break;

                default:
                    this.subscribeActivity(this.m_pendingActivity);

                    for (let sub of await this.m_pendingActivity.getSubActivities())
                    {
                        this.subscribeActivity(sub);
                    }

                    newPendingActivityInfo = await this.m_pendingActivity.activityInfo();

                    setTimeout(() => this.monitorActivity(), 10_000);
                    break;
            }
        }

        //--//

        let newInfo = (newPendingActivityInfo || []).join("/");
        let oldInfo = (this.pendingActivityInfo || []).join("/");

        if (newInfo != oldInfo)
        {
            this.pendingActivityInfo = newPendingActivityInfo;
            this.detectChanges();
        }
    }

    private subscribeActivity(pendingActivity: BackgroundActivityExtended)
    {
        if (!pendingActivity) return;

        for (let sub of this.m_pendingActivitySubscriptions)
        {
            if (sub.ext.sameIdentity(pendingActivity))
            {
                // Don't create a new subscription for the same target.
                return;
            }
        }

        pendingActivity.resetAllCachedValues();

        this.m_pendingActivitySubscriptions.push(this.subscribe(pendingActivity,
                                                                async (ext,
                                                                       action) =>
                                                                {
                                                                    ext.resetAllCachedValues();

                                                                    if (this.m_pendingActivity)
                                                                    {
                                                                        this.m_pendingActivity.resetAllCachedValues();
                                                                    }

                                                                    this.monitorActivity();
                                                                }));
    }

    private unsubscribeActivities()
    {
        for (let sub of this.m_pendingActivitySubscriptions)
        {
            this.removeSubscription(sub);
        }

        this.m_pendingActivitySubscriptions = [];
    }
}

class UpgradeBlocker
{
    user: UserExtended;
    until: Date;
}
