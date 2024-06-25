import {Component, ElementRef, Injector, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";

import {ReportError} from "app/app.service";

import {CustomerServiceSelectionDialogComponent} from "app/customer/customer-services/customer-service-selection-dialog.component";
import {ServiceRoleSelectionDialogComponent} from "app/customer/customer-services/service-role-selection-dialog.component";
import {DeploymentHostImagePullsListComponent, ImageSelection} from "app/customer/deployment-hosts/deployment-host-image-pulls-list.component";
import {TestConfigurationDialogComponent} from "app/customer/deployment-hosts/test-configuration-selection-dialog.component";
import {RegistryImageSelectionDialogComponent} from "app/customer/registry-images/registry-image-selection-dialog.component";
import {DashboardManagementService} from "app/dashboard/dashboard-management.service";

import * as SharedSvc from "app/services/domain/base.service";
import {DeploymentHostFileExtended} from "app/services/domain/deployment-host-files.service";
import {DeploymentHostImagePullExtended} from "app/services/domain/deployment-host-image-pulls.service";
import {DeploymentHostExtended} from "app/services/domain/deployment-hosts.service";
import {DeploymentTaskExtended} from "app/services/domain/deployment-tasks.service";
import {RegistryTaggedImageExtended} from "app/services/domain/registry-tagged-images.service";
import * as Models from "app/services/proxy/model/models";
import {UtilsService} from "framework/services/utils.service";
import {ConsoleLogComponent} from "framework/ui/consoles/console-log.component";

import {ControlOption} from "framework/ui/control-option";
import {ColumnConfiguration, DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {DatatableContextMenuEvent} from "framework/ui/datatables/datatable.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {NumberWithSeparatorsPipe} from "framework/ui/formatting/string-format.pipe";
import {OverlayComponent} from "framework/ui/overlays/overlay.component";
import {Future} from "framework/utils/concurrency";

import moment from "framework/utils/moment";

@Component({
               selector   : "o3-deployment-hosts-detail-page",
               templateUrl: "./deployment-hosts-detail-page.component.html"
           })
export class DeploymentHostsDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    @ViewChild("modelForm") modelForm: NgForm;

    id: string;
    extended: DeploymentHostExtended;
    extendedRemoveChecks: Models.ValidationResult[];
    extendedNoRemoveReason: string;
    extendedCheckingConnection = "<checking...>";

    extendedTerminateChecks: Models.ValidationResult[];
    extendedNoTerminateReason: string;

    onlineSessions: OnlineSessionDetails[];
    onlineSessionsProvider: ProviderForOnlineSession;

    isOnline: boolean;
    isTransferring: boolean;

    possibleActions: SharedSvc.ActionDescriptor[]                          = [];
    instanceTypes: ControlOption<Models.DeploymentInstance>[]              = [];
    operationalStates: ControlOption<Models.DeploymentOperationalStatus>[] = [];

    images: Models.DeploymentHostImage[];

    charges: Models.DeploymentCellularChargesSummary;

    remoteInfo: Models.DeploymentHostServiceDetails;

    warningThreshold: number;
    alertThreshold: number;
    imagePruningThreshold: number;

    batteryThreshold = new Models.DeployerShutdownConfiguration();

    offlineDeployment: Models.DeploymentHostOffline;

    //--//

    @ViewChild("addNewFileDialog") addNewFileDialog: OverlayComponent;
    @ViewChild("uploadFileDialog") uploadFileDialog: OverlayComponent;
    @ViewChild("filesToUpload") filesToUpload: ElementRef;
    @ViewChild("fileTextContentsForm") fileTextContentsForm: NgForm;
    taskOptions: ControlOption<string>[] = [];
    tasks: DeploymentTaskExtended[]      = [];
    taskSelected: string;

    filesFetched: boolean;
    filesFetchedOnce: boolean;
    files: DeploymentHostFileExtended[] = [];
    fileSelected: DeploymentHostFileExtended;
    fileToUpload: DeploymentHostFileExtended;
    enableFileEdit: boolean;
    fileTextContents: string;

    filePath: string;

    //--//

    private imagePullSelected: DeploymentHostImagePullExtended;
    private imagePullSub: SharedSvc.DbChangeSubscription<Models.DeploymentHostImagePull>;

    @ViewChild("imagePulls", {static: true}) imagePullsList: DeploymentHostImagePullsListComponent;
    @ViewChild("imagePullLog", {static: true}) imagePullLog: ConsoleLogComponent;

    imagePullLogLockScroll: boolean;

    //--//

    operations: Models.DelayedOperation[];
    operationsSelected: Models.DelayedOperation[] = [];

    //--//

    provisioning: Models.DeploymentHostProvisioningInfo;

    @ViewChild("noteTextContentsForm") noteTextContentsForm: NgForm;
    notes: Models.DeploymentHostProvisioningNotes[];
    noteSelected: Models.DeploymentHostProvisioningNotes;
    noteCustomerInfo: string;
    noteText: string;

    //--//

    bootOptionsDef: Models.EnumDescriptor[];
    bootOptionsList: OptionValue[];

    fetchingDataSessions: boolean;
    dataSessions: DataSessionDetails[];
    dataSessionsProvider: ProviderForDataSession;

    dataExchangeOptions: ControlOption<Models.DeploymentCellularCommunicationsDetails>[];
    dataExchangeSelected: Models.DeploymentCellularCommunicationsDetails;
    dataExchangeTotal: number;

    //--//

    constructor(inj: Injector)
    {
        super(inj);

        this.extended = this.app.domain.deploymentHosts.allocateInstance();

        this.onlineSessionsProvider = new ProviderForOnlineSession(this);

        this.dataSessionsProvider = new ProviderForDataSession(this);
    }

    protected async onNavigationComplete()
    {
        this.id = this.getPathParameter("id");

        this.instanceTypes     = await this.app.domain.customerServices.getInstanceTypes();
        this.operationalStates = await this.app.domain.deploymentHosts.getOperationalStates();

        this.loadData();

        DeploymentHostImagePullExtended.bindToLog(this.imagePullLog, () => this.imagePullSelected);
    }

    protected shouldDelayNotifications(): boolean
    {
        return !this.modelForm.pristine;
    }

    //--//

    async loadData()
    {
        if (this.id)
        {
            let deploymentHosts = this.app.domain.deploymentHosts;

            deploymentHosts.logger.debug(`Loading Host: ${this.id}`);
            let extended = await deploymentHosts.getExtendedById(this.id);
            if (!extended)
            {
                this.exit();
                return;
            }

            this.inject(DashboardManagementService)
                .recordHost(extended);

            this.extended = extended;

            let onlineSessions: OnlineSessionDetails[];

            if (extended.model.onlineSessions && extended.model.onlineSessions.entries)
            {
                onlineSessions                              = extended.model.onlineSessions.entries.map((s) => new OnlineSessionDetails(s));
                let nextOnlineSession: OnlineSessionDetails = null;

                onlineSessions.sort((a,
                                     b) => -MomentHelper.compareDates(a.model.start, b.model.start));

                for (let onlineSession of onlineSessions)
                {
                    onlineSession.computeOffline(nextOnlineSession);

                    nextOnlineSession = onlineSession;
                }
            }
            else
            {
                onlineSessions = null;
            }

            this.onlineSessions = onlineSessions;
            this.onlineSessionsProvider.bind(onlineSessions);

            //--//

            this.app.ui.navigation.breadcrumbCurrentLabel = extended.model.hostId + (extended.model.hostName ? ` - ${extended.model.hostName}` : "");
            deploymentHosts.logger.debug(`Loaded Host: ${JSON.stringify(this.extended.model)}`);

            let steps = [
                async () =>
                {
                    this.extendedRemoveChecks   = await this.extended.checkRemove();
                    this.extendedNoRemoveReason = this.fromValidationToReason("Remove is disabled because:", this.extendedRemoveChecks);
                    this.detectChanges();
                },
                async () =>
                {
                    this.extendedTerminateChecks   = await this.extended.checkTerminate();
                    this.extendedNoTerminateReason = this.fromValidationToReason("Terminate is disabled because:", this.extendedTerminateChecks);
                    this.detectChanges();
                },
                async () =>
                {
                    await this.updateActions();
                    this.detectChanges();
                },
                async () =>
                {
                    this.charges = await extended.getCharges();
                    this.detectChanges();
                },
                async () =>
                {
                    this.bootOptionsDef = await deploymentHosts.describeBootConfigOptions();
                    this.updateBootOptions(await extended.getBootOptions());
                    this.detectChanges();
                },
                async () =>
                {
                    let ops = await extended.describeDelayedOperations();
                    if (ops?.ops?.length > 0)
                    {
                        this.operations = ops.ops;
                    }
                    else
                    {
                        this.operations = undefined;
                    }
                    this.detectChanges();
                },
                async () =>
                {
                    this.filesFetched = false;
                    this.fileToUpload = null;

                    if (this.filesFetchedOnce)
                    {
                        await this.fetchFiles();
                    }

                    if (this.fileSelected)
                    {
                        let sysId         = this.fileSelected.model.sysId;
                        this.fileSelected = null;

                        await this.fetchFiles();

                        for (let file of this.files)
                        {
                            if (file.model.sysId == sysId)
                            {
                                this.fileSelected = file;
                                break;
                            }
                        }
                    }

                    this.selectFile(this.fileSelected);
                    this.detectChanges();
                },
                async () =>
                {
                    if (this.imagePullSelected)
                    {
                        this.selectImagePull(await this.imagePullSelected.refreshIfNeeded());
                        this.detectChanges();
                    }
                },
                async () =>
                {
                    await this.loadProvisioningInfo();
                    this.detectChanges();
                },
                async () =>
                {
                    this.remoteInfo = await extended.getRemoteInfo();
                    this.detectChanges();
                },
                async () =>
                {
                    await this.checkIfOnline();
                    this.detectChanges();
                }
            ];

            await Promise.all(steps.map((step) => step()));

            await this.updateActions();
            this.detectChanges();

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

    async updateActions()
    {
        let deploymentHosts = this.app.domain.deploymentHosts;
        let extended        = this.extended;

        let possibleActions: SharedSvc.ActionDescriptor[] = [];

        if (extended.model.details?.provider)
        {
            SharedSvc.ActionDescriptor.add(possibleActions, "Refresh Charges", "Refresh Cellular Charges", async () =>
            {
                this.charges = await extended.getCharges(true);
            });
        }

        if (extended.model.instanceType == Models.DeploymentInstance.AZURE_EDGE)
        {
            SharedSvc.ActionDescriptor.add(possibleActions, "Deploy Offline", "Generate IoT Edge configuration", async () =>
            {
                return this.bindOffline();
            });

            SharedSvc.ActionDescriptor.add(possibleActions, "Unbind", "Remove the host from a service", async () =>
            {
                return this.unbind();
            });
        }
        else
        {
            let custSvc = await extended.getCustomerService();
            if (custSvc != null)
            {
                SharedSvc.ActionDescriptor.add(possibleActions, "Unbind", "Remove the host from a service", async () =>
                {
                    return this.unbind();
                });
            }
            else
            {
                SharedSvc.ActionDescriptor.add(possibleActions, "Bind", "Associate the host with a service", async () =>
                {
                    return this.bind();
                });
            }
        }

        if (extended.model.status == Models.DeploymentStatus.Ready)
        {
            SharedSvc.ActionDescriptor.add(possibleActions, "New Agent...", "Start a new agent on the host", async () =>
            {
                let image = await RegistryImageSelectionDialogComponent.open(this, extended.model.architecture, [Models.DeploymentRole.deployer], "deploy", "Deploy");
                if (image != null)
                {
                    deploymentHosts.logger.debug(`Selected image for new agent: ${JSON.stringify(image.taggedImage.model)}`);
                    await extended.startAgent(image.taggedImage);
                }
            });

            for (let agent of await extended.getAgents())
            {
                if (agent.model.active)
                {
                    SharedSvc.ActionDescriptor.add(possibleActions, "Login", "Log into the active agent of the host", async () =>
                    {
                        this.app.ui.navigation.push([
                                                        "agent",
                                                        agent.model.sysId
                                                    ], [
                                                        {
                                                            param: "login",
                                                            value: "true"
                                                        }
                                                    ]);
                    });

                    break;
                }
            }

            if (extended.model.instanceType == Models.DeploymentInstance.Edge)
            {
                SharedSvc.ActionDescriptor.add(possibleActions, "Update Waypoint...", "Update the Waypoint app on the host", async () =>
                {
                    const purposes = [
                        Models.DeploymentRole.provisioner,
                        Models.DeploymentRole.waypoint
                    ];

                    let image = await RegistryImageSelectionDialogComponent.open(this, extended.model.architecture, purposes, "deploy", "Deploy");
                    if (image != null)
                    {
                        await extended.updateWaypoint(image.taggedImage);
                    }
                });
            }

            SharedSvc.ActionDescriptor.add(possibleActions, "Load Image...", "Load image on the host", async () =>
            {
                let image = await RegistryImageSelectionDialogComponent.open(this, extended.model.architecture, null, "distribute", "Send");
                if (image == null)
                {
                    return;
                }

                let count = await image.taggedImage.distribute(undefined, this.extended.model.sysId);
                if (count > 0)
                {
                    this.app.framework.errors.success(`Starting download of ${image.taggedImage.model.tag}...`, -1);
                }
                else
                {
                    this.app.framework.errors.warn(`Unable to start download of ${image.taggedImage.model.tag}...`, -1);
                }
            });

            SharedSvc.ActionDescriptor.add(possibleActions, "Run Test...", "Run a test task on the host", async () =>
            {
                let image = await RegistryImageSelectionDialogComponent.open(this, extended.model.architecture, null, "deploy", "Deploy");
                if (image == null)
                {
                    return;
                }

                let result = await TestConfigurationDialogComponent.open(this, image.image, "test", "Launch Test");
                if (result == null)
                {
                    return;
                }

                console.info(`privileged: ${result.cfg.privileged}`);
                console.info(`hostNetwork: ${result.cfg.useHostNetwork}`);
                console.info(`commandLine: ${result.cfg.commandLine}`);
                console.info(`entrypoint: ${result.cfg.entrypoint}`);

                deploymentHosts.logger.debug(`Selected image for new test: ${JSON.stringify(image.taggedImage.model)}`);
                await extended.startTask(image.taggedImage, result.cfg);
            });
        }

        if (this.extendedNoTerminateReason == null)
        {
            SharedSvc.ActionDescriptor.add(possibleActions, "Terminate", "Retire the cloud instance for this host", async () =>
            {
                if (await this.confirmOperation("Click Yes to confirm termination (all data on this host will be lost)."))
                {
                    deploymentHosts.logger.debug(`Terminate host ${extended.model.hostId}`);

                    await extended.terminate();
                }
            });
        }

        SharedSvc.ActionDescriptor.add(possibleActions, "Notify Me On Agent Checkin", "Send an email when any agent for this host contacts the Builder", async () =>
        {
            await extended.notifyMe();
        });

        if (await extended.getLogRpc())
        {
            SharedSvc.ActionDescriptor.add(possibleActions, "Turn Off RPC Logging", "Disable detailed logging of RPC calls", async () =>
            {
                await extended.setLogRpc(false);
            });
        }
        else
        {
            SharedSvc.ActionDescriptor.add(possibleActions, "Turn On RPC Logging", "Enable detailed logging of RPC calls", async () =>
            {
                await extended.setLogRpc(true);
            });
        }

        this.possibleActions = possibleActions;

        this.detectChanges();
    }

    //--//

    @ReportError
    async save()
    {
        await this.extended.save();

        await this.cancel();
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }

    async cancel()
    {
        let extended = await this.extended.refresh<DeploymentHostExtended>();

        if (this.modelForm)
        {
            this.modelForm.resetForm();
        }

        if (this.fileTextContentsForm)
        {
            this.fileTextContentsForm.resetForm();
        }

        this.detectChanges();
        this.extended = extended;
        this.detectChanges();
    }

    //--//

    async fetchFiles()
    {
        await this.waitUntilTrue(10, () => !!this.extended);

        if (this.extended && !this.filesFetched)
        {
            this.files            = await this.extended.getFiles();
            this.filesFetched     = true;
            this.filesFetchedOnce = true;

            for (let op of this.operations || [])
            {
                if (op instanceof Models.DelayedFileTransfer)
                {
                    for (let file of this.files)
                    {
                        if (file.model.sysId == op.loc_file.id)
                        {
                            file.transferDisplay = "<pending transfer>";
                        }
                    }
                }
            }
        }
    }

    selectFile(fileExt: DeploymentHostFileExtended)
    {
        this.fileSelected = fileExt;

        this.restoreFile();
    }

    selectFileMenu(event: DatatableContextMenuEvent<DeploymentHostFileExtended>)
    {
        let fileExt = event.row;
        if (!fileExt) return;

        let noTransfers = !fileExt.transferDisplay;

        if (noTransfers)
        {
            if (fileExt.model.length != -1)
            {
                event.root.addItem("Download...", async () =>
                {
                    let fileUrl = await fileExt.getUrlForDownload();

                    window.open(fileUrl, "_blank");
                });
            }

            event.root.addItem("Upload...", async () =>
            {
                this.fileToUpload = fileExt;
                this.uploadFileDialog.toggleOverlay();
            });

            event.root.addItem("Fetch from Host", async () =>
            {
                if (await this.confirmOperation("Click Yes to fetch this file from the host."))
                {
                    await fileExt.startDownload();
                }
            });

            if (fileExt.model.length != -1)
            {
                event.root.addItem("Push To Host", async () =>
                {
                    if (await this.confirmOperation("Click Yes to push this file to the host."))
                    {
                        await fileExt.startUpload();
                    }
                });
            }

            event.root.addItem("Discard", async () =>
            {
                if (await this.confirmOperation("Click Yes to forget this file."))
                {
                    await fileExt.forget();

                    if (this.fileSelected == fileExt)
                    {
                        this.selectFile(null);
                    }
                }
            });
        }
    }

    async newFileTrigger()
    {
        this.taskSelected = null;
        this.taskOptions  = [new ControlOption<string>("<none>", "From Host")];

        this.tasks = await this.extended.getTasks();
        for (let task of this.tasks)
        {
            if (task.model.dockerId)
            {
                this.taskOptions.push(new ControlOption<string>(task.model.sysId, `From Container '${task.model.name}'`));
            }
        }

        this.addNewFileDialog.toggleOverlay();
    }

    async newFile(path: string)
    {
        this.addNewFileDialog.closeOverlay();

        let task = this.tasks.find((task) => task.model.sysId == this.taskSelected);
        await this.extended.addFile(path, task);
    }

    async saveFile()
    {
        let upload  = new Models.DeploymentHostFileContents();
        upload.text = this.fileTextContents;

        this.fileSelected = await this.fileSelected.setContents(upload);
        this.files        = await this.extended.getFiles();

        this.restoreFile();
    }

    async restoreFile()
    {
        this.fileTextContentsForm.resetForm();

        let fileExt = this.fileSelected;
        if (fileExt)
        {
            let noTransfers = !fileExt.transferDisplay;

            if (fileExt.model.length == -1)
            {
                this.enableFileEdit   = true;
                this.fileTextContents = "";
            }
            else if (fileExt.model.length < 500000 && !fileExt.mayBeBinary)
            {
                this.enableFileEdit   = true;
                this.fileTextContents = await fileExt.getContents();
            }
            else
            {
                this.enableFileEdit   = false;
                this.fileTextContents = null;
            }
        }
        else
        {
            this.enableFileEdit = false;
        }
    }

    async uploadFile()
    {
        this.uploadFileDialog.closeOverlay();

        if (this.filesToUpload && this.filesToUpload.nativeElement)
        {
            let files = <FileList>this.filesToUpload.nativeElement.files;
            if (files.length > 0)
            {
                let file = files[0];

                this.app.framework.errors.success(`Uploading ${file.name}...`, -1);

                try
                {
                    await this.fileToUpload.uploadContents(file);

                    this.app.framework.errors.success(`Uploaded ${file.name}!`, -1);

                    if (this.fileToUpload == this.fileSelected)
                    {
                        this.selectFile(this.fileSelected);
                    }

                    this.fileToUpload = null;

                    await Future.delayed(300);
                }
                catch (e)
                {
                    this.app.framework.errors.error("FAILURE",`Failed to upload ${file.name}`);
                }

                this.filesFetched = null;
                await this.fetchFiles();
            }
        }
    }

    //--//

    async fetchImages()
    {
        await this.waitUntilTrue(10, () => !!this.extended);

        if (!this.images)
        {
            this.images = await this.extended.getImages();
        }
    }

    selectImagePull(imageExt: DeploymentHostImagePullExtended)
    {
        let currentSysId = this.imagePullSelected?.model?.sysId;
        let newSysId     = imageExt?.model?.sysId;

        this.imagePullSelected = imageExt;

        if (currentSysId != newSysId)
        {
            this.imagePullLog.reset();
        }

        this.refreshImagePullLog();
    }

    private refreshImagePullLog()
    {
        this.imagePullLog.refresh(this.imagePullSelected?.model?.status == Models.JobStatus.EXECUTING);

        if (this.imagePullSub)
        {
            this.imagePullSub.unsubscribe();
            this.imagePullSub = undefined;
        }

        if (this.imagePullSelected)
        {
            this.imagePullSub = this.subscribeOneShot(this.imagePullSelected, async () =>
            {
                this.imagePullSelected = await this.imagePullSelected.refreshIfNeeded();
                this.refreshImagePullLog();
            });
        }
    }

    selectImagePullMenu(event: DatatableContextMenuEvent<ImageSelection>)
    {
        let imageExt = event.row?.singleSelect;
        if (imageExt)
        {
            event.root.addItem("Discard", async () =>
            {
                if (await this.confirmOperation("Click Yes to forget this image pull."))
                {
                    await imageExt.forget();

                    if (this.imagePullSelected == imageExt)
                    {
                        this.selectImagePull(null);
                    }

                    this.imagePullsList.table.refreshData();
                }
            });
        }

        let imageExts = event.row?.multiSelect;
        if (imageExts)
        {
            event.root.addItem("Discard Selected", async () =>
            {
                if (await this.confirmOperation("Click Yes to forget all selected image pulls."))
                {
                    for (let imageExt of imageExts)
                    {
                        await imageExt.forget();

                        if (this.imagePullSelected == imageExt)
                        {
                            this.selectImagePull(null);
                        }
                    }

                    this.imagePullsList.table.refreshData();
                }
            });
        }
    }

    //--//

    async selectOperations(ops: Models.DelayedOperation[])
    {
        this.operationsSelected = ops;
    }

    async cancelOperation()
    {
        let ops                 = this.operationsSelected;
        this.operationsSelected = [];

        let opsAfter = this.operations;
        for (let op of ops)
        {
            let res = await this.extended.cancelDelayedOperation(op);
            if (res)
            {
                opsAfter = res.ops;
            }
        }

        this.operations = opsAfter ? opsAfter : null;
    }

    //--//

    async selectNote(note: Models.DeploymentHostProvisioningNotes)
    {
        this.noteTextContentsForm.resetForm();
        this.detectChanges();
        this.noteSelected     = note;
        this.noteCustomerInfo = note?.customerInfo;
        this.noteText         = note?.text;
        this.detectChanges();
    }

    async resetNoteEditing()
    {
        this.noteTextContentsForm.resetForm();
        this.detectChanges();
        this.noteSelected     = null;
        this.noteCustomerInfo = null;
        this.noteText         = null;
        this.detectChanges();
    }

    async deleteNote()
    {
        if (await this.confirmOperation("Click Yes to forget this note."))
        {
            this.extended.removeProvisioningNote(this.noteSelected.sysId);

            await this.loadProvisioningInfo();
            this.detectChanges();
        }
    }

    async loadProvisioningInfo()
    {
        let provisioning = await this.extended.getProvisioningInfo();
        if (provisioning)
        {
            let manufacturingInfo = provisioning.manufacturingInfo;
            if (manufacturingInfo)
            {
                let manufacturingLocation = await this.extended.getManufacturingLocation();
                if (manufacturingLocation)
                {
                    manufacturingInfo.stationNumber = manufacturingLocation.model.hostName;
                }
            }
        }

        this.provisioning = provisioning;
        this.notes        = provisioning && provisioning.notes || [];
    }

    async saveNote()
    {
        this.extended.addProvisioningNote(this.noteCustomerInfo, this.noteText);

        this.resetNoteEditing();
    }

    async newNote()
    {
        this.selectNote(new Models.DeploymentHostProvisioningNotes());
    }

    //--//

    async refreshImages()
    {
        await this.extended.refreshImages();
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

    pruneImages()
    {
        this.extended.pruneImages(this.imagePruningThreshold);
    }

    //--//

    async fetchBootOptions()
    {
        this.updateBootOptions(await this.extended.fetchBootOptions());
    }

    async setBootOption(opt: OptionValue)
    {
        if (opt.valueNew === undefined || opt.valueNew == "") opt.valueNew = null;

        this.updateBootOptions(await this.extended.setBootOption(opt.key, opt.keyRaw, opt.valueNew));
    }

    //--//

    async refreshConnectionStatus()
    {
        await this.waitUntilTrue(10, () => !!this.extended);

        let status          = await this.extended.getDataConnectionStatus();
        this.isOnline       = status?.isOnline;
        this.isTransferring = status?.isTransferring;
        this.detectChanges();
    }

    async fetchDataSessions()
    {
        try
        {
            this.fetchingDataSessions = true;
            let dataSessionsRaw       = await this.extended.getDataSessions();
            let dataSessions: DataSessionDetails[];
            let dataExchangeOptions: ControlOption<Models.DeploymentCellularCommunicationsDetails>[];
            let totalBytes            = 0;

            if (dataSessionsRaw?.length > 0)
            {
                dataSessions                            = dataSessionsRaw.map((s) => new DataSessionDetails(s));
                let nextDataSession: DataSessionDetails = null;

                for (let dataSession of dataSessions)
                {
                    dataSession.computeOffline(nextDataSession);

                    nextDataSession = dataSession;
                }

                dataExchangeOptions = null;
            }
            else
            {
                dataSessions = null;

                let dataExchangesRaw = await this.extended.getDataExchanges(30);

                let addresses = UtilsService.extractKeysFromMap(dataExchangesRaw.sessions);

                addresses.sort((a,
                                b) =>
                               {
                                   let aVal = dataExchangesRaw.sessions[a];
                                   let bVal = dataExchangesRaw.sessions[b];

                                   let res = -UtilsService.compareNumbers(aVal.totalBytes, bVal.totalBytes, true);
                                   if (res == 0)
                                   {
                                       res = UtilsService.compareStrings(a, b, true);
                                   }
                                   return res;
                               });

                dataExchangeOptions = SharedSvc.BaseService.mapOptions(addresses, (address) =>
                {
                    let val = dataExchangesRaw.sessions[address];
                    if (!val) return null;

                    totalBytes += val.totalBytes;

                    let option   = new ControlOption<Models.DeploymentCellularCommunicationsDetails>();
                    option.id    = val;
                    option.label = `${address} / ${NumberWithSeparatorsPipe.format(val.totalBytes)}`;

                    return option;
                });
            }

            this.dataSessions = dataSessions;
            if (dataSessions)
            {
                this.dataSessionsProvider.bind(this.dataSessions);
            }

            this.dataExchangeOptions  = dataExchangeOptions;
            this.dataExchangeSelected = null;
            this.dataExchangeTotal    = totalBytes;
        }
        finally
        {
            this.fetchingDataSessions = false;
        }
    }

    updateDataExchanges()
    {
    }

    //--//

    async goToService()
    {
        let svc  = await this.extended.getCustomerService();
        let cust = await svc.getOwningCustomer();

        this.app.ui.navigation.go("/customers/item", [
            cust.model.sysId,
            "service",
            svc.model.sysId
        ]);
    }

    showLog()
    {
        this.app.ui.navigation.go("/deployments/item", [
            this.extended.model.sysId,
            "log"
        ]);
    }

    goToRemoteService()
    {
        window.open(this.remoteInfo.url, "_blank");
    }

    @ReportError
    async remove()
    {
        if (await this.confirmOperation("Click Yes to confirm deletion of this host."))
        {
            this.removeAllDbSubscriptions();

            await this.extended.remove();

            this.exit();
        }
    }

    @ReportError
    async bind()
    {
        let deploymentHosts = this.app.domain.deploymentHosts;

        let dialogService = await CustomerServiceSelectionDialogComponent.open(this, "select", "Select");
        if (dialogService == null)
        {
            deploymentHosts.logger.debug(`CustomerServiceSelectionDialogComponent cancelled`);
            return;
        }

        let map = new Map<Models.DeploymentRole, boolean>();
        for (let role of dialogService.svc.model.purposes)
        {
            let status = false;

            switch (role)
            {
                case Models.DeploymentRole.gateway:
                    switch (this.extended.model.architecture)
                    {
                        case Models.DockerImageArchitecture.ARMv7:
                        case Models.DockerImageArchitecture.ARMv6:
                            status = true;
                            break;
                    }
                    break;
            }

            map.set(role, status);
        }

        let dialogRoles = await ServiceRoleSelectionDialogComponent.open(this, map, "host", "Select");
        if (dialogRoles == null)
        {
            deploymentHosts.logger.debug(`ServiceRoleSelectionDialogComponent cancelled`);
            return;
        }

        deploymentHosts.logger.debug(`Selected customer ${JSON.stringify(dialogService.service)}`);
        let svc = dialogService.svc;
        for (let dialogRole of dialogRoles)
        {
            let success = await this.extended.bindToService(svc, dialogRole.role);
            deploymentHosts.logger.debug(`Bind result: ${success}`);

            if (dialogRole.role == Models.DeploymentRole.gateway)
            {
                svc = await svc.refresh();

                let desiredState = Models.CustomerServiceDesiredState.newInstance({
                                                                                      roles: []
                                                                                  });

                let state  = await svc.getState();
                let images = await svc.getImages();

                if (await state.enumerateHostsAsync(false,
                                                    async (role,
                                                           host) =>
                                                    {
                                                        let arch = host.architecture;
                                                        if (!svc.findDesiredState(desiredState, role, arch))
                                                        {
                                                            let image = svc.findExistingImage(images, role, arch);
                                                            if (image == null)
                                                            {
                                                                image = await this.selectImageForRole(role, host.architecture);
                                                                if (image == null) return false;
                                                            }

                                                            let roleSpec                 = svc.addImageToDesiredState(desiredState, role, arch, image);
                                                            roleSpec.shutdownIfDifferent = true;
                                                            roleSpec.launchIfMissing     = true;
                                                        }

                                                        return true;
                                                    }))
                {
                    await svc.applyDesiredState(desiredState);
                }
            }
        }

        await this.loadData();
    }

    private async selectImageForRole(role: Models.DeploymentRole,
                                     architecture: Models.DockerImageArchitecture): Promise<RegistryTaggedImageExtended>
    {
        let dialogRes = await RegistryImageSelectionDialogComponent.open(this, architecture, [role], `deploy for ${role}`, "Deploy");

        return dialogRes && dialogRes.taggedImage ? dialogRes.taggedImage : null;
    }

    @ReportError
    async unbind()
    {
        if (await this.confirmOperation("Click Yes to confirm unbinding host from service."))
        {
            for (let role of this.extended.model.roles)
            {
                let success = await this.extended.unbindFromService(role);
                this.app.domain.deploymentHosts.logger.debug(`Unbind result: ${success}`);
            }

            if (this.extended.model.instanceType == Models.DeploymentInstance.AZURE_EDGE)
            {
                this.exit();
            }
            else
            {
                await this.loadData();
            }
        }
    }

    @ReportError
    async bindOffline()
    {
        this.offlineDeployment = undefined;

        let svc = await this.extended.getCustomerService();

        let images = await svc.getImages();

        let image = svc.findExistingImage(images, Models.DeploymentRole.gateway, this.extended.model.architecture);
        if (image == null)
        {
            image = await this.selectImageForRole(Models.DeploymentRole.gateway, this.extended.model.architecture);
            if (image == null) return;

            if (!await svc.addImage(Models.DeploymentRole.gateway, this.extended.model.architecture, image))
            {
                this.app.framework.errors.warn(`Failed to add image ${image.model.tag} to service...`);
                return;
            }

            await this.loadData();
        }

        for (let part of await this.extended.prepareForOfflineDeployment())
        {
            this.offlineDeployment = part;
            break;
        }
    }

    //--//

    private async checkIfOnline()
    {
        try
        {
            let agents = await this.extended.getAgents();
            for (let agent of agents)
            {
                if (await agent.isOnline())
                {
                    this.extendedCheckingConnection = "Online!";
                    return;
                }
            }
        }
        catch (e)
        {
        }

        this.extendedCheckingConnection = "Offline!";
    }

    navigateToCellularProvider()
    {
        switch (this.extended.model.details?.provider)
        {
            case Models.CellularProvider.Twilio:
                window.open("https://www.twilio.com/console/wireless/sims/" + this.extended.model.details.providerId, "_blank");
                break;

            case Models.CellularProvider.TwilioSuperSim:
                window.open("https://www.twilio.com/console/iot/supersim/sims/" + this.extended.model.details.providerId, "_blank");
                break;

            case Models.CellularProvider.Pelion:
                window.open("https://connectivity-us.pelion.com/subscribers/" + this.extended.model.details.providerId, "_blank");
                break;
        }
    }

    private updateBootOptions(bootOptions: Models.BootOptions)
    {
        this.bootOptionsList = [];

        for (let def of this.bootOptionsDef)
        {
            let opt         = new OptionValue();
            opt.displayName = def.displayName;
            opt.key         = <Models.BootConfigOptions>def.id;
            this.bootOptionsList.push(opt);
        }

        for (let option of bootOptions?.options || [])
        {
            if (option.key)
            {
                let opt = this.bootOptionsList.find((v) => v.key == option.key);
                if (!opt)
                {
                    opt             = new OptionValue();
                    opt.displayName = option.key;
                    opt.key         = option.key;
                    this.bootOptionsList.push(opt);
                }

                opt.value = option.value;
            }
            else if (option.keyRaw)
            {
                let opt = this.bootOptionsList.find((v) => v.keyRaw == option.keyRaw);
                if (!opt)
                {
                    opt             = new OptionValue();
                    opt.displayName = option.keyRaw;
                    opt.keyRaw      = option.keyRaw;
                    this.bootOptionsList.push(opt);
                }

                opt.value = option.value;
            }
        }
    }

    public handleDataSessionContextMenu(event: DatatableContextMenuEvent<DataSessionDetails>)
    {
        let details = event.row;
        if (!details) return;

        switch (event.columnProperty)
        {
            case "location":
                this.dataSessionsProvider.handleFiltering(event, "filterLocation", details.location);
                break;

            case "cellId":
                this.dataSessionsProvider.handleFiltering(event, "filterCellId", details.model.cellId);
                break;
        }
    }
}

class DataSessionDetails
{
    location: string;

    duration: string;
    offline: string;

    constructor(public readonly model: Models.DeploymentCellularSession)
    {
        if (isNaN(model.estimatedLongitude) || isNaN(model.estimatedLatitude))
        {
            this.location = "<unknown>";
        }
        else
        {
            this.location = `${Math.abs(model.estimatedLongitude)}${model.estimatedLongitude < 0 ? "W" : "E"} ${Math.abs(model.estimatedLatitude)}${model.estimatedLatitude < 0 ? "S" : "N"}`;
        }

        let momentStart = MomentHelper.parse(model.start);
        let momentEnd: moment.Moment;

        if (model.end)
        {
            momentEnd = MomentHelper.parse(model.end);
        }
        else
        {
            momentEnd = MomentHelper.now();
        }

        this.duration = `${momentEnd.from(momentStart, true)}${!model.end ? " (active)" : ""}`;
    }

    computeOffline(nextDataSession: DataSessionDetails)
    {
        if (nextDataSession)
        {
            let momentStart = MomentHelper.parse(nextDataSession.model.start);
            let momentEnd   = MomentHelper.parse(this.model.end);

            this.offline = momentStart.from(momentEnd, true);
        }
        else if (this.model.end)
        {
            let momentStart = MomentHelper.parse(this.model.end);
            let momentEnd   = MomentHelper.now();

            this.offline = momentStart.from(momentEnd, true);
        }
    }
}

class ProviderForDataSession implements IDatatableDataProvider<DataSessionDetails, DataSessionDetails, DataSessionDetails>
{
    table: DatatableManager<DataSessionDetails, DataSessionDetails, DataSessionDetails>;

    data: DataSessionDetails[];
    private dataFiltered: DataSessionDetails[];

    filterLocation: string;
    filterCellId: string;

    constructor(private host: DeploymentHostsDetailPageComponent)
    {
        this.table = new DatatableManager<DataSessionDetails, DataSessionDetails, DataSessionDetails>(this, () =>
        {
            return this.host.getViewState()
                       .getSubView("DataSessions", true);
        });
    }

    //--//

    public bind(data: DataSessionDetails[])
    {
        this.filterLocation = undefined;
        this.filterCellId   = undefined;

        this.data = data;
        this.table.refreshData();
        this.table.resetPagination();
    }

    public handleFiltering<K extends keyof ProviderForDataSession>(event: DatatableContextMenuEvent<DataSessionDetails>,
                                                                   key: K,
                                                                   value: ProviderForDataSession[K])
    {
        let thisLambda: ProviderForDataSession = this;

        if (thisLambda[key])
        {
            event.root.addItem("Reset filter", async () =>
            {
                thisLambda[key] = undefined;
                this.table.refreshData();
            });

        }
        else
        {
            event.root.addItem("Show Only Matching", async () =>
            {
                thisLambda[key] = value;
                this.table.refreshData();
            });
        }
    }

    public async getList(): Promise<DataSessionDetails[]>
    {
        let data = this.data;

        if (this.filterLocation)
        {
            data = data.filter((a) => a.location == this.filterLocation);
        }

        if (this.filterCellId)
        {
            data = data.filter((a) => a.model.cellId == this.filterCellId);
        }

        if (this.table.sort && this.table.sort.length)
        {
            let property  = this.table.sort[0].prop;
            let direction = this.table.sort[0].dir == "asc" ? 1 : -1;

            data.sort((a,
                       b) =>
                      {
                          let diff: number;

                          switch (property)
                          {
                              default:
                              case "start":
                                  return direction * MomentHelper.compareDates(a.model.start, b.model.start);

                              case "end":
                                  return direction * MomentHelper.compareDates(a.model.end, b.model.end);

                              case "location":
                                  diff = UtilsService.compareStrings(a.location, b.location, true);
                                  if (!diff)
                                  {
                                      diff = MomentHelper.compareDates(a.model.start, b.model.start);
                                  }
                                  return direction * diff;

                              case "cellId":
                                  diff = UtilsService.compareStrings(a.model.cellId, b.model.cellId, true);
                                  if (!diff)
                                  {
                                      diff = MomentHelper.compareDates(a.model.start, b.model.start);
                                  }
                                  return direction * diff;
                          }
                      });
        }

        this.dataFiltered = data;

        return data;
    }

    public async getPage(offset: number,
                         limit: number): Promise<DataSessionDetails[]>
    {
        if (!this.dataFiltered || this.dataFiltered.length == 0)
        {
            return [];
        }

        const start = offset * limit;
        const end   = start + limit;

        let res: DataSessionDetails[] = [];

        for (let i = start; i < end; i++)
        {
            let key = this.dataFiltered[i];
            if (key) res.push(key);
        }

        return res;
    }

    public async transform(rows: DataSessionDetails[]): Promise<DataSessionDetails[]>
    {
        return rows;
    }

    public itemClicked(columnId: string,
                       item: DataSessionDetails)
    {
        this.host.detectChanges();
    }

    //--//

    getTableConfigId(): string
    {
        return "Data-Sessions";
    }

    getColumnConfigs(): Promise<ColumnConfiguration[]>
    {
        return null;
    }

    setColumnConfigs(columnConfigs: ColumnConfiguration[]): Promise<boolean>
    {
        return null;
    }

    public getItemName(): string
    {
        return "Data Sessions";
    }

    public wasDestroyed(): boolean
    {
        return false;
    }
}

//--//


class OnlineSessionDetails
{
    duration: string;
    offline: string;

    constructor(public readonly model: Models.DeploymentHostOnlineSession)
    {
        if (model.start)
        {
            let momentStart = MomentHelper.parse(model.start);

            if (model.end)
            {
                let momentEnd = MomentHelper.parse(model.end);

                this.duration = momentEnd.from(momentStart, true);
            }
            else
            {
                let momentNow = MomentHelper.now();

                this.duration = momentNow.from(momentStart, true) + " (active)";
            }
        }
    }

    computeOffline(nextSession: OnlineSessionDetails)
    {
        if (nextSession && nextSession.model.start)
        {
            let momentStart = MomentHelper.parse(nextSession.model.start);
            let momentEnd   = MomentHelper.parse(this.model.end);

            this.offline = momentStart.from(momentEnd, true);
        }
        else if (this.model.end)
        {
            let momentStart = MomentHelper.parse(this.model.end);
            let momentEnd   = MomentHelper.now();

            this.offline = momentStart.from(momentEnd, true);
        }
    }
}


class ProviderForOnlineSession implements IDatatableDataProvider<OnlineSessionDetails, OnlineSessionDetails, OnlineSessionDetails>
{
    table: DatatableManager<OnlineSessionDetails, OnlineSessionDetails, OnlineSessionDetails>;

    data: OnlineSessionDetails[];

    constructor(private host: DeploymentHostsDetailPageComponent)
    {
        this.table = new DatatableManager<OnlineSessionDetails, OnlineSessionDetails, OnlineSessionDetails>(this, () =>
        {
            return this.host.getViewState()
                       .getSubView("OnlineSessions", true);
        });
    }

    //--//

    public bind(data: OnlineSessionDetails[])
    {
        this.data = data;
        this.table.refreshData();
        this.table.resetPagination();
    }

    public async getList(): Promise<OnlineSessionDetails[]>
    {
        return this.data;
    }

    public async getPage(offset: number,
                         limit: number): Promise<OnlineSessionDetails[]>
    {
        if (!this.data || this.data.length == 0)
        {
            return [];
        }

        const start = offset * limit;
        const end   = start + limit;

        let res: OnlineSessionDetails[] = [];

        for (let i = start; i < end; i++)
        {
            let key = this.data[i];
            if (key) res.push(key);
        }

        return res;
    }

    public async transform(rows: OnlineSessionDetails[]): Promise<OnlineSessionDetails[]>
    {
        return rows;
    }

    public itemClicked(columnId: string,
                       item: OnlineSessionDetails)
    {
        this.host.detectChanges();
    }

    //--//

    getTableConfigId(): string
    {
        return "Online-Sessions";
    }

    getColumnConfigs(): Promise<ColumnConfiguration[]>
    {
        return null;
    }

    setColumnConfigs(columnConfigs: ColumnConfiguration[]): Promise<boolean>
    {
        return null;
    }

    public getItemName(): string
    {
        return "Online Sessions";
    }

    public wasDestroyed(): boolean
    {
        return false;
    }
}

class OptionValue
{
    displayName: string;
    key: Models.BootConfigOptions;
    keyRaw: string;

    value: string;
    valueNew: string;
    present: boolean;
}
