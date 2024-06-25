import {BreakpointObserver, Breakpoints} from "@angular/cdk/layout";
import {ChangeDetectionStrategy, Component, ElementRef, Injector, ViewChild} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {DashboardConfigurationExtended, DashboardManagementService} from "app/services/domain/dashboard-management.service";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {ImportDialogComponent} from "framework/ui/dialogs/import-dialog.component";
import {SelectComponent} from "framework/ui/forms/select.component";
import {mapInParallel} from "framework/utils/concurrency";

@Component({
               templateUrl    : "./dashboard-toolbar.component.html",
               styleUrls      : ["./dashboard-toolbar.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class DashboardToolbarComponent extends SharedSvc.BaseApplicationComponent
{
    activeExt: DashboardConfigurationExtended;
    dashboardOptions: ControlOption<string>[];

    allowExportAll: boolean   = true;
    dashboardEditing: boolean = this.app.domain.dashboard.isEditing.getValue();

    @ViewChild("test_dashboardSelect") test_dashboardSelect: SelectComponent<string>;
    @ViewChild("test_menuTrigger", {read: ElementRef}) test_menuTrigger: ElementRef<HTMLElement>;

    private m_loading: boolean = false;

    private get ready(): boolean
    {
        return this.activeExt && this.dashboardOptions && !this.m_loading;
    }

    private m_mobileView: boolean;
    get mobileView(): boolean
    {
        return this.m_mobileView;
    }

    constructor(inj: Injector,
                breakpointObserver: BreakpointObserver)
    {
        super(inj);

        this.subscribeToObservable(breakpointObserver.observe(Breakpoints.XSmall), (breakpoint) =>
        {
            this.m_mobileView = breakpoint.matches;
            this.markForCheck();
        });
    }

    public async ngOnInit()
    {
        super.ngOnInit();

        this.subscribeToObservable(this.app.domain.dashboard.isEditing, (editing) =>
        {
            this.dashboardEditing = editing;
            this.markForCheck();
        });

        this.subscribeToObservable(this.app.domain.dashboard.dashboardsUpdated, () => this.update());
        this.subscribeToObservable(this.app.domain.dashboard.currentDashboardConfig, (cfg) => this.updateOption(cfg));
        await UtilsService.executeWithRetries(() => this.update(), 5, 750, undefined, 1.25, true);
        this.markForCheck();
    }

    private async update(): Promise<boolean>
    {
        this.activeExt = await this.app.domain.dashboard.getActive();

        let dashboardModels   = this.app.domain.dashboard.getDashboardModels(true, true);
        this.dashboardOptions = dashboardModels.map((cfg) => new ControlOption(cfg.dashboardId, cfg.title));

        this.allowExportAll = this.dashboardOptions.length <= DashboardManagementService.MaxImportExportCount;

        this.markForCheck();

        return this.dashboardOptions.length > 0;
    }

    private updateOption(cfg: DashboardConfigurationExtended)
    {
        if (this.dashboardOptions)
        {
            this.dashboardOptions = this.dashboardOptions.map((old) => new ControlOption(old.id, cfg?.dashboardId === old.id ? cfg.title : old.label));
            this.markForCheck();
        }
    }

    private async handleInteraction(dashboardManagementInteraction: () => Promise<void>)
    {
        if (this.ready)
        {
            try
            {
                this.m_loading = true;

                await dashboardManagementInteraction();
            }
            finally
            {
                this.m_loading = false;
                this.markForCheck();
            }
        }
    }

    editDashboard()
    {
        this.app.domain.dashboard.enableEdit();
    }

    addWidget()
    {
        this.app.domain.dashboard.triggerAddWidget();
    }

    newDashboard()
    {
        let cfgExt = DashboardConfigurationExtended.getNewConfig(this.app.domain.dashboard);
        this.handleInteraction(() => this.app.domain.dashboard.makeNewActive(cfgExt, true, true));
    }

    cloneDashboard()
    {
        let cfgExt = this.activeExt.getCopyConfig();
        this.handleInteraction(() => this.app.domain.dashboard.makeNewActive(cfgExt, true, false));
    }

    deleteDashboard()
    {
        this.handleInteraction(async () =>
                               {
                                   let confirmed = await this.confirmOperation("Deleting this dashboard cannot be undone.");
                                   if (!confirmed)
                                   {
                                       return;
                                   }

                                   await this.app.domain.dashboard.deleteDashboard(this.activeExt);
                               });
    }

    importDashboard()
    {
        this.handleInteraction(async () =>
                               {
                                   let result = await ImportDialogComponent.open(this, "Import Dashboard(s)", {
                                       returnRawBlobs: () => false,
                                       parseFile     : (contents: string) => this.parseDashboards(contents)
                                   });

                                   if (!result?.length)
                                   {
                                       return;
                                   }

                                   await this.app.domain.dashboard.importDashboards(result);

                                   this.app.framework.errors.success(`${result.length} ${UtilsService.pluralize("dashboard", result.length)} uploaded.`, -1);
                               });
    }

    private async parseDashboards(dashboardsJson: string): Promise<Models.DashboardConfiguration[]>
    {
        let parsedJson = JSON.parse(dashboardsJson);
        if (!Array.isArray(parsedJson))
        {
            parsedJson = [parsedJson];
        }
        if (parsedJson.length > DashboardManagementService.MaxImportExportCount)
        {
            throw Error("Too many dashboards: load fewer at once");
        }

        let models = await mapInParallel(parsedJson, async (model) =>
        {
            try
            {
                let rawImport   = Models.RawImport.newInstance({contentsAsJSON: JSON.stringify(model)});
                let parsedModel = await this.app.domain.apis.dashboardDefinitionVersions.parseImport(rawImport);
                return DashboardConfigurationExtended.isValid(parsedModel) ? parsedModel : null;
            }
            catch (e)
            {
                return null;
            }
        });

        return models.filter((model) => !!model);
    }

    exportDashboard()
    {
        this.handleInteraction(() =>
                               {
                                   let model    = this.activeExt.model;
                                   let fileName = DownloadDialogComponent.fileName(`dashboard${model.title ? "__" + model.title : ""}`);
                                   return DownloadDialogComponent.open(this, "Export this Dashboard", fileName, model);
                               });
    }

    exportAllDashboards()
    {
        this.handleInteraction(() =>
                               {
                                   let models = this.app.domain.dashboard.getDashboardModels();
                                   if (!models?.length)
                                   {
                                       return null;
                                   }

                                   let fileName = DownloadDialogComponent.fileName(`all_${models.length}_dashboards`);
                                   return DownloadDialogComponent.open(this, "Export all Dashboards", fileName, models.map((m) => m.model));
                               });
    }

    async pushDashboardToUsers(create: boolean): Promise<void>
    {
        this.app.framework.errors.success("Pushing dashboard to all users.", -1);
        await this.app.domain.dashboard.push(create);
        this.app.framework.errors.success("Successfully pushed dashboard to all users.", -1);
    }

    changeActiveDashboard(id: string)
    {
        if (this.ready && this.activeExt.dashboardId !== id)
        {
            this.handleInteraction(async () =>
                                   {
                                       await this.app.domain.dashboard.changeActiveDashboard(id);
                                       await this.update();
                                   });
        }
        else
        {
            this.markForCheck();
        }
    }
}
