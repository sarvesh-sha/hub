import {Injectable} from "@angular/core";
import {UUID} from "angular2-uuid";

import {ReportError} from "app/app.service";
import {AssetGraphSelectorWidgetConfigurationExtended} from "app/dashboard/dashboard/widgets/asset-graph-selector-widget/widget.component";
import {GroupingWidgetConfigurationExtended} from "app/dashboard/dashboard/widgets/grouping-widget/widget.component";
import {WidgetBaseComponent} from "app/dashboard/dashboard/widgets/widget-base.component";
import {WidgetManipulator} from "app/dashboard/dashboard/widgets/widget-manipulator";
import {ApiService} from "app/services/domain/api.service";
import {SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {DashboardDefinitionVersionExtended} from "app/services/domain/dashboard-definition-versions.service";
import {DashboardDefinitionExtended, DashboardDefinitionsService} from "app/services/domain/dashboard-definitions.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import {UsersService} from "app/services/domain/users.service";
import {WidgetConfigurationExtended} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {GraphConfigurationHost} from "app/shared/assets/configuration/graph-configuration-host";
import {VersionService} from "app/shared/undo/version-manager";

import {ErrorService} from "framework/services/error.service";
import {Lookup, UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {Future, inParallel, mapInParallelNoNulls} from "framework/utils/concurrency";

import {BehaviorSubject, Observable, Subject, Subscription} from "rxjs";

@Injectable()
export class DashboardManagementService implements VersionService<DashboardDefinitionVersionExtended>
{
    public static readonly ActiveDashboard: string       = "DASHBOARD_ACTIVE";
    public static readonly DashboardGraphContext: string = "DASHBOARD_GRAPH_CONTEXT";

    public static readonly MaxImportExportCount: number = 50;

    private m_dashboards: Lookup<DashboardConfigurationExtended>;
    private m_loadingActives: Future<void>;
    private m_active: DashboardConfigurationExtended;

    private m_enqueuedEdit: () => Promise<void>;
    get enqueuedEdit(): () => Promise<void>
    {
        let action          = this.m_enqueuedEdit;
        this.m_enqueuedEdit = null;
        return action;
    }

    private m_widgetWizardOpen: boolean = false;
    get widgetWizardOpen(): boolean
    {
        return this.m_widgetWizardOpen;
    }

    public readonly isEditing              = new BehaviorSubject<boolean>(false);
    public readonly addWidget              = new Subject<void>();
    public readonly dashboardsUpdated      = new Subject<void>();
    public readonly activeDashboardChanged = new Subject<void>();
    public readonly currentDashboardConfig = new BehaviorSubject<DashboardConfigurationExtended>(null);

    private m_selectorContexts = new Future<Map<string, Map<string, string>>>();

    //--//

    constructor(public users: UsersService,
                public errors: ErrorService,
                public definitions: DashboardDefinitionsService,
                public apis: ApiService)
    {
        this.users.loggedIn.subscribe(() =>
                                      {
                                          this.m_active     = null;
                                          this.m_dashboards = null;
                                          this.initializeGraphContexts();
                                      });

        this.users.loggedOut.subscribe(() =>
                                       {
                                           this.m_selectorContexts.reject("loggedOut");

                                           this.m_active           = null;
                                           this.m_dashboards       = null;
                                           this.m_selectorContexts = new Future<Map<string, Map<string, string>>>();
                                       });

    }

    private async initializeGraphContexts()
    {
        let dashboardContexts = new Map<string, Map<string, string>>();
        const graphContexts   = await this.users.getTypedPreference(null, DashboardManagementService.DashboardGraphContext, Models.DashboardGraphContextPreference.fixupPrototype);
        if (graphContexts)
        {
            const contextByDashboard = graphContexts.graphContexts || {};
            for (let dashboardId in contextByDashboard)
            {
                const dashboardContext = contextByDashboard[dashboardId];
                const contexts         = new Map<string, string>();
                for (let graphId in dashboardContext)
                {
                    contexts.set(graphId, dashboardContext[graphId]);
                }
                dashboardContexts.set(dashboardId, contexts);
            }
        }

        this.m_selectorContexts.resolve(dashboardContexts);
    }

    //--//

    public getHeadIdentity(): Models.RecordIdentity
    {
        return this.m_active?.defExt.model.headVersion;
    }

    public getVersion(id: Models.RecordIdentity): Promise<DashboardDefinitionVersionExtended>
    {
        return this.m_active?.defExt.getVersion(id);
    }

    public async getAllVersions(): Promise<DashboardDefinitionVersionExtended[]>
    {
        return this.m_active?.defExt.getAllVersions() || [];
    }

    public async getConfigExtended(verExt: DashboardDefinitionVersionExtended): Promise<DashboardConfigurationExtended>
    {
        let defExt = await verExt.getDefinition();
        return new DashboardConfigurationExtended(this, defExt, verExt, await verExt.getDetails());
    }

    //--//

    public triggerEdit(editAction: () => Promise<void>)
    {
        if (!this.isEditing.getValue() && editAction)
        {
            this.m_enqueuedEdit = editAction;
            this.enableEdit();
        }
    }

    public triggerAddWidget()
    {
        this.addWidget.next();
    }

    public enableEdit()
    {
        this.isEditing.next(true);
    }

    public disableEdit()
    {
        this.isEditing.next(false);
    }

    //--//

    public widgetWizardOpened()
    {
        this.m_widgetWizardOpen = true;
    }

    public widgetWizardClosed()
    {
        this.m_widgetWizardOpen = false;
    }

    //--//

    public async getActive(force: boolean = false): Promise<DashboardConfigurationExtended>
    {
        if (!force)
        {
            while (this.m_loadingActives) await this.m_loadingActives;
        }

        if (!this.m_active || force)
        {
            let loadingActives    = new Future<void>();
            this.m_loadingActives = loadingActives;

            let cfgExt: DashboardConfigurationExtended;

            let activeId = await this.users.getPreference(null, DashboardManagementService.ActiveDashboard);
            if (activeId)
            {
                try
                {
                    let defExt = await this.definitions.getExtendedById(activeId);
                    cfgExt     = await this.getConfigExtendedForHead(defExt);
                }
                catch (e)
                {
                    // Dashboard deleted, ignore.
                    cfgExt = null;
                }
            }

            if (!cfgExt)
            {
                activeId = null;

                // Search for existing dashboard, select first one.
                await this.reloadDashboardsInner();
                for (let id in this.m_dashboards)
                {
                    cfgExt = this.m_dashboards[id];
                    if (cfgExt) break;
                }
            }

            if (!cfgExt)
            {
                await this.makeNewActive(undefined, true, false);
            }
            else
            {
                if (activeId)
                {
                    this.m_active = cfgExt;
                }
                else
                {
                    await this.makeActive(cfgExt, false);
                }
            }

            loadingActives.resolve();
            if (this.m_loadingActives !== loadingActives) return this.getActive();
            this.m_loadingActives = null;
        }

        return this.m_active;
    }

    public async makeNewActive(config: Models.DashboardConfiguration,
                               useNewDashboard: boolean,
                               withWidgets: boolean)
    {
        if (!config)
        {
            config      = DashboardConfigurationExtended.getNewConfig(this);
            withWidgets = true;
        }

        let defExt = this.m_active?.defExt;
        if (!defExt || useNewDashboard)
        {
            defExt = this.definitions.allocateInstance();
            defExt = await defExt.save();
        }

        if (withWidgets)
        {
            await this.initWidgets(config);
        }
        let verExt = await defExt.linkNewToHead(config);

        let cfgExt = new DashboardConfigurationExtended(this, defExt, verExt, config);
        await cfgExt.save(undefined);

        await this.makeActive(cfgExt, true);
    }

    public async makeActive(cfg: DashboardConfigurationExtended,
                            emitEvent: boolean): Promise<boolean>
    {
        if (await this.users.setPreference(null, DashboardManagementService.ActiveDashboard, cfg.dashboardId))
        {
            await this.reloadDashboards(true, true);

            if (emitEvent) this.activeDashboardChanged.next();

            return true;
        }

        return false;
    }

    public async makeHead(verExt: DashboardDefinitionVersionExtended)
    {
        await verExt.makeHead();
        await this.m_active.defExt.flush();
        await this.getActive(true);
    }

    public async changeActiveDashboard(id: string): Promise<void>
    {
        try
        {
            let defExt = await this.definitions.getExtendedById(id);
            let cfgExt = await this.getConfigExtendedForHead(defExt);
            if (cfgExt)
            {
                await this.makeActive(cfgExt, true);
            }
        }
        catch (e)
        {
            // Dashboard deleted, ignore.
        }
    }

    public async importDashboards(models: Models.DashboardConfiguration[])
    {
        let currTitles = this.getDashboardModels()
                             .map((cfg) => cfg.title);

        let newActiveId: string;

        for (let model of models)
        {
            model.title = UtilsService.getUniqueTitle(model.title, currTitles);
            currTitles.push(model.title);

            let defExt = this.definitions.allocateInstance();
            defExt     = await defExt.save();

            await defExt.linkNewToHead(model);

            if (!newActiveId)
            {
                newActiveId = defExt.model.sysId;
            }
        }

        if (newActiveId)
        {
            await this.changeActiveDashboard(newActiveId);
        }
    }

    public async deleteDashboard(dashboard: DashboardConfigurationExtended)
    {
        let defExt = await this.definitions.getExtendedById(dashboard.dashboardId);
        if (defExt)
        {
            delete this.m_dashboards[dashboard.dashboardId];
            this.m_active = null;

            await defExt.remove();

            let models        = this.getDashboardModels(false, true);
            let nextDashboard = models.find((cfg) => cfg.title > dashboard.title) || models[0];
            if (nextDashboard)
            {
                await this.changeActiveDashboard(nextDashboard.dashboardId);
            }
            else
            {
                await this.makeNewActive(DashboardConfigurationExtended.getNewConfig(this), true, true);
            }
        }
    }

    public getUniqueDashboardTitle(desiredTitle: string = "Dashboard",
                                   dashboardId?: string): string
    {
        return UtilsService.getUniqueTitle(desiredTitle, this.getDashboardModels()
                                                             .filter((cfg) => !dashboardId || cfg.dashboardId !== dashboardId)
                                                             .map((cfg) => cfg.title));
    }

    public getDashboardModels(withActiveDashboard: boolean = true,
                              sorted: boolean              = false): DashboardConfigurationExtended[]
    {
        let models = [];
        for (const id in this.m_dashboards || {})
        {
            let cfgExt = this.m_dashboards[id];
            if (withActiveDashboard || !cfgExt.defExt.sameIdentity(this.m_active?.defExt)) models.push(cfgExt);
        }

        if (sorted)
        {
            models.sort((cfg1,
                         cfg2) => UtilsService.compareStrings(cfg1.model.title, cfg2.model.title, true));
        }

        return models;
    }

    @ReportError
    public push(create: boolean)
    {
        return this.apis.dashboardDefinitions.push(false, create, this.m_active.model);
    }

    public async reloadDashboards(force: boolean,
                                  emitChange: boolean)
    {
        if (!force && this.m_dashboards) return;

        await this.reloadDashboardsInner();

        await this.getActive(force);

        if (emitChange) this.dashboardsUpdated.next();
    }

    private async reloadDashboardsInner()
    {
        let defExts    = await this.definitions.getExtendedAll();
        let dashboards = await mapInParallelNoNulls(defExts, (defExt) => this.getConfigExtendedForHead(defExt));

        this.m_dashboards = {};
        for (let dashboard of dashboards)
        {
            this.m_dashboards[dashboard.dashboardId] = dashboard;
        }
    }

    private async getConfigExtendedForHead(defExt: DashboardDefinitionExtended): Promise<DashboardConfigurationExtended>
    {
        let verExt = await defExt?.getHead();
        if (!verExt)
        {
            // Bad dashboard, get rid of it.
            try
            {
                await defExt?.remove();
            }
            catch (e)
            {
                // Ignore failures.
            }

            return null;
        }

        return new DashboardConfigurationExtended(this, defExt, verExt, await verExt.getDetails());
    }

    private async getContexts(dashboardId: string): Promise<Map<string, string>>
    {
        let dashboardContexts = await this.m_selectorContexts;
        if (!dashboardContexts.has(dashboardId))
        {
            dashboardContexts.set(dashboardId, new Map<string, string>());
        }

        return dashboardContexts.get(dashboardId);
    }

    public async getSelectorContext(dashboardId: string,
                                    selectorId: string): Promise<string>
    {
        let dashboardContexts = await this.getContexts(dashboardId);
        return dashboardContexts.get(selectorId);
    }

    public async setGraphContext(dashboardId: string,
                                 selectorId: string,
                                 sysId: string): Promise<boolean>
    {
        let contexts = await this.getContexts(dashboardId);
        if (contexts.get(selectorId) !== sysId)
        {
            contexts.set(selectorId, sysId);

            const allContexts = await this.m_selectorContexts;
            const model       = Models.DashboardGraphContextPreference.newInstance({graphContexts: {}});
            for (let [dashboardId, contexts] of allContexts.entries())
            {
                // Dashboard has been deleted, don't save context
                if (this.m_dashboards && !this.m_dashboards[dashboardId])
                {
                    continue;
                }

                const dashboardContext: Lookup<string> = {};
                model.graphContexts[dashboardId]       = dashboardContext;
                for (let [selectorId, asset] of contexts.entries())
                {
                    dashboardContext[selectorId] = asset;
                }
            }

            await this.users.setTypedPreference(null, DashboardManagementService.DashboardGraphContext, model);

            return true;
        }
        return false;
    }

    //--//

    /**
     * Initialize the user's widget dashboard.
     */
    public initWidgets(config: Models.DashboardConfiguration)
    {
        if (this.users.hasDemoData)
        {
            if (this.users.hasAdminRole())
            {
                // render admin role widgets
                this.initAdminWidgetLayout(config);
            }
            else
            {
                // render user role widgets
                this.initUserWidgetLayout(config);
            }
        }
        else
        {
            this.initIntroductionWidget(config);
        }
    }

    /**
     * Initialize the default "User" layout.
     */
    private async initUserWidgetLayout(config: Models.DashboardConfiguration)
    {
        let baseOutline: Models.WidgetOutline = Models.WidgetOutline.newInstance({
                                                                                     width: 6,
                                                                                     top  : 0
                                                                                 });

        {
            baseOutline.left = 0;

            let widget1       = new Models.AlertSummaryWidgetConfiguration();
            widget1.name      = "Lincoln Square Summary";
            widget1.locations = ["e0a7a980-13b0-4fe3-93b3-ff19a84f70b5"];
  //          this.initWidget(config, baseOutline, widget1);

            let widget2       = new Models.AlertFeedWidgetConfiguration();
            widget2.name      = "Lincoln Square Feed";
            widget2.locations = ["e0a7a980-13b0-4fe3-93b3-ff19a84f70b5"];
//            this.initWidget(config, baseOutline, widget2);

            let widget3       = new Models.DeviceSummaryWidgetConfiguration();
            widget3.name      = "Lincoln Square & Holyoke Devices";
            widget3.locations = [
                "e0a7a980-13b0-4fe3-93b3-ff19a84f70b5",
                "fa210561-5f3c-4005-8822-8e68601332a2"
            ];
    //        this.initWidget(config, baseOutline, widget3);
        }

        {
            baseOutline.left = 6;
            baseOutline.top  = 0;

            let widget1       = new Models.AlertSummaryWidgetConfiguration();
            widget1.name      = "Holyoke Building Summary";
            widget1.locations = ["fa210561-5f3c-4005-8822-8e68601332a2"];
      //      this.initWidget(config, baseOutline, widget1);

            let widget2       = new Models.AlertFeedWidgetConfiguration(); // units-expand-mobile
            widget2.name      = "Holyoke Building Feed";
            widget2.locations = ["fa210561-5f3c-4005-8822-8e68601332a2"];
        //    this.initWidget(config, baseOutline, widget2);

            let widget3          = new Models.AlertMapWidgetConfiguration();
            widget3.center       = "Seattle, WA";
            widget3.options      = new Models.AlertMapOptions();
            widget3.options.zoom = 2;
          //  this.initWidget(config, baseOutline, widget3);
        }
    }

    /**
     * Initialize the default "Admin" layout.
     */
    private initAdminWidgetLayout(config: Models.DashboardConfiguration)
    {
        let baseOutline: Models.WidgetOutline = Models.WidgetOutline.newInstance({
                                                                                     width: 6,
                                                                                     top  : 0
                                                                                 });

        {
            baseOutline.left = 0;

            let widget1          = new Models.AlertMapWidgetConfiguration();
            widget1.name         = "Seattle Alerts";
            widget1.center       = "Seattle, WA";
            widget1.options      = new Models.AlertMapOptions();
            widget1.options.zoom = 2;
            widget1.locations    = [
                "fa210561-5f3c-4005-8822-8e68601332a2",
                "c1452194-6aa2-43e6-99e1-c878196bbacf",
                "3f37e299-92b6-48ae-9479-28bee4f4f64c",
                "4ad67f14-1058-49c3-b04a-3329863a9c09",
                "cb043364-0478-465d-b94c-d514d74f503e"
            ];
            //this.initWidget(config, baseOutline, widget1);

            let widget2          = new Models.AlertMapWidgetConfiguration();
            widget2.name         = "Bellevue Alerts";
            widget2.center       = "Bellevue, WA";
            widget2.options      = new Models.AlertMapOptions();
            widget2.options.zoom = 4;
            widget2.locations    = ["e0a7a980-13b0-4fe3-93b3-ff19a84f70b5"];
           // this.initWidget(config, baseOutline, widget2);

            let widget3  = new Models.AlertFeedWidgetConfiguration();
            widget3.name = "All Location Alert Feed";
          //  this.initWidget(config, baseOutline, widget3);
        }

        {
            baseOutline.left = 6;
            baseOutline.top  = 0;

            let widget1       = new Models.AlertSummaryWidgetConfiguration();
            widget1.name      = "Seattle Alert Summary";
            widget1.locations = [
                "fa210561-5f3c-4005-8822-8e68601332a2",
                "c1452194-6aa2-43e6-99e1-c878196bbacf",
                "3f37e299-92b6-48ae-9479-28bee4f4f64c",
                "4ad67f14-1058-49c3-b04a-3329863a9c09",
                "cb043364-0478-465d-b94c-d514d74f503e"
            ];
//            this.initWidget(config, baseOutline, widget1);

            let widget2       = new Models.DeviceSummaryWidgetConfiguration();
            widget2.name      = "Seattle Device Summary";
            widget2.locations = [
                "fa210561-5f3c-4005-8822-8e68601332a2",
                "c1452194-6aa2-43e6-99e1-c878196bbacf",
                "3f37e299-92b6-48ae-9479-28bee4f4f64c",
                "4ad67f14-1058-49c3-b04a-3329863a9c09",
                "cb043364-0478-465d-b94c-d514d74f503e"
            ];
  //          this.initWidget(config, baseOutline, widget2);

            let widget3       = new Models.AlertSummaryWidgetConfiguration();
            widget3.name      = "Bellevue Alert Summary";
            widget3.locations = ["e0a7a980-13b0-4fe3-93b3-ff19a84f70b5"];
    //        this.initWidget(config, baseOutline, widget3);

            let widget4       = new Models.DeviceSummaryWidgetConfiguration();
            widget4.name      = "Bellevue Device Summary";
            widget4.locations = ["e0a7a980-13b0-4fe3-93b3-ff19a84f70b5"];
      //      this.initWidget(config, baseOutline, widget4);

            let widget5  = new Models.AlertSummaryWidgetConfiguration();
            widget5.name = "All Location Alert Summary";
        //    this.initWidget(config, baseOutline, widget5);

            let widget6  = new Models.DeviceSummaryWidgetConfiguration();
            widget6.name = "All Location Device Summary";
          //  this.initWidget(config, baseOutline, widget6);
        }
    }

    private initIntroductionWidget(config: Models.DashboardConfiguration)
    {
        // todo: implement
    }

    private initWidget(config: Models.DashboardConfiguration,
                       baseOutline: Models.WidgetOutline,
                       widget: Models.WidgetConfiguration)
    {
        widget.id          = UUID.UUID();
        let defaultOutline = WidgetConfigurationExtended.fromConfigModel(widget)
                                                        .getDescriptor().config;
        let composition    = Models.WidgetComposition.newInstance(
            {
                config : widget,
                outline: Models.WidgetOutline.newInstance({
                                                              top   : baseOutline.top,
                                                              left  : baseOutline.left,
                                                              height: defaultOutline.defaultHeight,
                                                              width : defaultOutline.defaultWidth
                                                          })
            });
        config.widgets.push(composition);
        baseOutline.top += composition.outline.height;
    }
}

export class DashboardConfigurationExtended
{
    public static readonly numDashboardColumns: number   = 12;
    public static readonly defaultPrimaryColor: string   = "#FFFFFF";
    public static readonly defaultSecondaryColor: string = "#2196F3";

    public readonly selectors: Lookup<Models.SharedAssetSelector> = {};

    public widgetManipulator: WidgetManipulator;

    private readonly m_graphConfigurationHost: GraphConfigurationHost;
    get graphConfigurationHost(): GraphConfigurationHost
    {
        return this.m_graphConfigurationHost;
    }

    get sharedGraphs(): Models.SharedAssetGraph[]
    {
        return this.model.sharedGraphs;
    }

    get dashboardId(): string
    {
        return this.defExt.model.sysId;
    }

    get title(): string
    {
        return this.model.title;
    }

    constructor(public readonly svc: DashboardManagementService,
                public readonly defExt: DashboardDefinitionExtended,
                public readonly verExt: DashboardDefinitionVersionExtended,
                public readonly model: Models.DashboardConfiguration)
    {
        if (!this.model.sharedGraphs)
        {
            this.model.sharedGraphs = [];
        }

        if (this.model.sharedSelectors)
        {
            this.selectors = UtilsService.extractLookup(this.model.sharedSelectors, this.selectors);
        }
        else
        {
            this.model.sharedSelectors = [];
        }

        this.m_graphConfigurationHost = {
            hostContext  : "Dashboard's Selector Widgets",
            graphsChanged: new Subject(),
            getGraphs    : () => this.sharedGraphs,
            resolveGraphs: () => this.resolveGraphs(),
            canRemove    : (graphId: string) => this.canRemoveGraph(graphId),
            canRemoveNode: (graphId: string,
                            nodeId: string) =>
            {
                for (let widget of this.model.widgets)
                {
                    for (let binding of WidgetConfigurationExtended.fromConfigModel(widget.config)
                                                                   .getBindings())
                    {
                        if (binding.graphId === graphId && binding.nodeId === nodeId) return false;
                    }
                }

                return true;
            }
        };
    }

    public cloneForEdit(): DashboardConfigurationExtended
    {
        const clone             = new DashboardConfigurationExtended(this.svc, this.defExt, this.verExt, Models.DashboardConfiguration.deepClone(this.model));
        clone.widgetManipulator = this.widgetManipulator;
        return clone;
    }

    public static getNewConfig(svc: DashboardManagementService): Models.DashboardConfiguration
    {
        return Models.DashboardConfiguration.newInstance(
            {
                title    : svc.getUniqueDashboardTitle() || "Dashboard",
                showTitle: false,
                widgets  : []
            });
    }

    public static isValid(model: Models.DashboardConfiguration): boolean
    {
        if (!model?.widgets) return false;
        return model.widgets.every((composition) => composition?.config && composition.outline);
    }

    public canRemoveGraph(graphId: string): boolean
    {
        for (let widget of this.model.widgets)
        {
            let widgetExt = WidgetConfigurationExtended.fromConfigModel(widget.config);
            for (let binding of widgetExt.getBindings())
            {
                if (this.getAssociatedGraphId(binding.selectorId) === graphId)
                {
                    return false;
                }
            }
        }

        return true;
    }

    public getNewAssetGraphSelectors(newConfig: Models.WidgetConfiguration,
                                     newNameLookup: Lookup<Models.SharedAssetSelector>,
                                     maxNumSelectors: number = -1): Models.AssetGraphSelectorWidgetConfiguration[]
    {
        let selectorConfigs = [];

        let graphIdToSelectorNames = this.graphToSelectorNames();
        let bindings               = WidgetConfigurationExtended.fromConfigModel(newConfig)
                                                                .getBindings();
        if (maxNumSelectors >= 0)
        {
            let newSelectors    = new Set<string>(); // duplicates are allowed in selectorIds
            let numNewSelectors = bindings.reduce((cum,
                                                   binding) =>
                                                  {
                                                      let selectorId = binding.selectorId;
                                                      if (!this.selectors[selectorId] && !newSelectors.has(selectorId))
                                                      {
                                                          newSelectors.add(selectorId);
                                                          return cum + 1;
                                                      }

                                                      return cum;
                                                  }, 0);
            if (numNewSelectors > maxNumSelectors)
            {
                return null;
            }
        }

        for (let binding of bindings)
        {
            if (!this.selectors[binding.selectorId] && binding.graphId)
            {
                const graph = this.model.sharedGraphs.find((g) => g.id === binding.graphId);
                selectorConfigs.push(Models.AssetGraphSelectorWidgetConfiguration.newInstance(
                    {
                        id        : UUID.UUID(),
                        name      : graph?.name,
                        selectorId: binding.selectorId
                    }));

                let selectorName = newNameLookup?.[binding.selectorId].name ||
                                   UtilsService.getUniqueTitle(AssetGraphSelectorWidgetConfigurationExtended.defaultSelectorName, graphIdToSelectorNames[binding.graphId]);

                this.addSelector(Models.SharedAssetSelector.newInstance({
                                                                            id     : binding.selectorId,
                                                                            graphId: binding.graphId,
                                                                            name   : selectorName
                                                                        }));

                if (!graphIdToSelectorNames[binding.graphId])
                {
                    graphIdToSelectorNames[binding.graphId] = [];
                }
                graphIdToSelectorNames[binding.graphId].push(selectorName);
            }
        }

        return selectorConfigs;
    }

    public removeSelector(selectorId: string)
    {
        let selector = this.selectors[selectorId];
        if (selector)
        {
            this.model.sharedSelectors.splice(this.model.sharedSelectors.indexOf(selector), 1);
            delete this.selectors[selectorId];
        }
    }

    private graphToSelectorNames(): Lookup<string[]>
    {
        let graphIdToName: Lookup<string[]> = {};

        for (let selectorId in this.selectors)
        {
            let selector = this.selectors[selectorId];
            let graphId  = selector.graphId;

            let selectorNames = graphIdToName[graphId] || [];
            selectorNames.push(selector.name);
            graphIdToName[graphId] = selectorNames;
        }

        return graphIdToName;
    }

    public getUniqueSelectorName(graphId: string): string
    {
        let allSelectorNames = this.graphToSelectorNames();
        let selectorNames    = allSelectorNames[graphId]?.map((selectorName) => selectorName || AssetGraphSelectorWidgetConfigurationExtended.defaultSelectorName) || [];
        return UtilsService.getUniqueTitle(AssetGraphSelectorWidgetConfigurationExtended.defaultSelectorName, selectorNames);
    }

    public addGraphs(graphs: Models.SharedAssetGraph[]): Promise<Map<string, SharedAssetGraphExtended>>
    {
        let added      = false;
        const graphIds = new Set<string>(this.model.sharedGraphs.map((g) => g.id));
        for (let graph of graphs)
        {
            if (!graphIds.has(graph.id))
            {
                graphIds.add(graph.id);
                graph = Models.SharedAssetGraph.deepClone(graph);
                this.model.sharedGraphs.push(graph);
                added = true;
            }
        }

        if (added && this.m_graphsResolved)
        {
            return this.resolveGraphs();
        }
        return null;
    }

    private m_graphsResolved: Map<string, SharedAssetGraphExtended>;
    private readonly m_selectorContextSubs    = new Map<string, BehaviorSubject<Models.AssetGraphContextAsset>>();
    private readonly m_graphContextOptionSubs = new Map<string, BehaviorSubject<ControlOption<string>[]>>();
    private readonly m_graphInitializing      = new Map<string, Promise<void>>();

    public getAssociatedGraphId(selectorId: string): string
    {
        return this.selectors[selectorId]?.graphId;
    }

    public async getContextUpdater(selectorId: string): Promise<GraphContextUpdater>
    {
        let graphId = this.getAssociatedGraphId(selectorId);
        if (graphId)
        {
            return new GraphContextUpdater(this,
                                           this.m_graphsResolved.get(graphId).name,
                                           selectorId,
                                           await this.getGraphOptions(graphId),
                                           await this.getGraphContextObservable(selectorId),
                                           await this.svc.getSelectorContext(this.dashboardId, selectorId));
        }

        return null;
    }

    public async getResolvedGraph(graphId: string): Promise<SharedAssetGraphExtended>
    {
        if (!this.m_graphsResolved)
        {
            await this.resolveGraphs();
        }

        return this.m_graphsResolved.get(graphId);
    }

    public async resolveGraphs(): Promise<Map<string, SharedAssetGraphExtended>>
    {
        let domain = this.svc.apis.injector.get(AppDomainContext);

        this.m_graphsResolved = await SharedAssetGraphExtended.loadGraphs(domain, this.model.sharedGraphs);

        return this.m_graphsResolved;
    }

    public async getGraphContextObservable(selectorId: string): Promise<Observable<Models.AssetGraphContextAsset>>
    {
        let subject = await this.getGraphContextSubject(selectorId);
        return subject?.asObservable();
    }

    private async getGraphContextSubject(selectorId: string): Promise<BehaviorSubject<Models.AssetGraphContextAsset>>
    {
        await this.ensureInitialized(this.getAssociatedGraphId(selectorId));

        return this.m_selectorContextSubs.get(selectorId);
    }

    public async getGraphOptionsObservable(graphId: string): Promise<Observable<ControlOption<string>[]>>
    {
        await this.ensureInitialized(graphId);

        let subject = this.m_graphContextOptionSubs.get(graphId);
        return subject?.asObservable();
    }

    public async setGraphContext(selectorId: string,
                                 sysId: string): Promise<void>
    {
        let subject = this.m_selectorContextSubs.get(selectorId);
        if (!subject) return;

        if (await this.svc.setGraphContext(this.dashboardId, selectorId, sysId) || subject.getValue()?.sysId !== sysId)
        {
            subject.next(Models.AssetGraphContextAsset.newInstance({sysId: sysId}));
        }
    }

    public async getSelectorOptions(graphId: string,
                                    includeContexts?: boolean): Promise<ControlOption<string>[]>
    {
        let options: ControlOption<string>[] = [];
        for (let selectorId in this.selectors)
        {
            let selector = this.selectors[selectorId];
            if (selector.graphId === graphId)
            {
                options.push(new ControlOption(selector.id, selector.name));
            }
        }

        if (includeContexts)
        {
            let contextOptions = await this.getGraphOptions(graphId);
            await inParallel(options, async (option) =>
            {
                let currContext = <Models.AssetGraphContextAsset>await this.getGraphContext(option.id);
                if (currContext)
                {
                    let currContextOption = contextOptions.find((contextOption) => contextOption.id === currContext.sysId);
                    if (currContextOption)
                    {
                        option.label += " - " + currContextOption.label;
                    }
                }
            });
        }

        return options;
    }

    public async getGraphContext(selectorId: string): Promise<Models.AssetGraphContext>
    {
        let subject = await this.getGraphContextSubject(selectorId);
        return subject?.getValue();
    }

    public async getGraphOptions(graphId: string): Promise<ControlOption<string>[]>
    {
        await this.ensureInitialized(graphId);

        let subject = this.m_graphContextOptionSubs.get(graphId);
        return subject?.getValue();
    }

    public async getContextName(selectorId: string): Promise<string>
    {
        let graphId = this.getAssociatedGraphId(selectorId);
        await this.ensureInitialized(graphId);

        let optionsSubject = this.m_graphContextOptionSubs.get(graphId);
        let options        = optionsSubject?.getValue();
        if (options)
        {
            let selectedSubject = this.m_selectorContextSubs.get(selectorId);
            let selected        = selectedSubject?.getValue();
            if (selected)
            {
                let option = options.find((o) => o.id === selected.sysId);
                return option?.label;
            }
        }

        return undefined;
    }

    private async ensureInitialized(graphId: string): Promise<void>
    {
        if (!graphId) return;

        let initializing = this.m_graphInitializing.get(graphId);
        if (!initializing)
        {
            initializing = this.initializeGraphInner(graphId);
            this.m_graphInitializing.set(graphId, initializing);
            initializing.then(() => this.m_graphInitializing.delete(graphId));
        }

        await initializing;
    }

    private async initializeGraphInner(graphId: string): Promise<void>
    {
        let graph = await this.getResolvedGraph(graphId);
        if (!graph) return;

        let responses = await graph.resolve();
        if (!responses) return;

        let allOptions = await responses.getControlOptions();
        // Only consider single roots
        let root       = graph.getRootNodes()[0]?.id;
        let options    = allOptions.get(root) || [];

        let subject = this.m_graphContextOptionSubs.get(graphId);
        if (!subject)
        {
            subject = new BehaviorSubject<ControlOption<string>[]>(options);
            this.m_graphContextOptionSubs.set(graphId, subject);

            if (options.length)
            {
                let defaultOption = options[0]?.id;
                await inParallel(Object.keys(this.selectors), async (selectorId) =>
                {
                    if (this.getAssociatedGraphId(selectorId) === graphId)
                    {
                        let selectedContext = await this.svc.getSelectorContext(this.dashboardId, selectorId);
                        if (!selectedContext || !options.find((o) => o.id === selectedContext))
                        {
                            selectedContext = defaultOption;
                            await this.svc.setGraphContext(this.dashboardId, selectorId, selectedContext);
                        }

                        let context = selectedContext ? Models.AssetGraphContextAsset.newInstance({sysId: selectedContext}) : null;
                        this.m_selectorContextSubs.set(selectorId, new BehaviorSubject<Models.AssetGraphContextAsset>(context));
                    }
                });
            }
        }
        else
        {
            subject.next(options);
        }
    }

    public addSelector(selector: Models.SharedAssetSelector)
    {
        if (selector.graphId && !this.selectors[selector.id])
        {
            selector          = Models.SharedAssetSelector.deepClone(selector);
            let existingNames = this.graphToSelectorNames()[selector.graphId] || [];
            selector.name     = UtilsService.getUniqueTitle(selector.name || "Selector", existingNames);

            this.model.sharedSelectors.push(selector);
            this.selectors[selector.id] = selector;
        }
    }

    public getCopyConfig(): Models.DashboardConfiguration
    {
        let copy   = Models.DashboardConfiguration.deepClone(this.model);
        copy.title = this.svc.getUniqueDashboardTitle(copy.title);
        return copy;
    }

    public async save(predecessorId: Models.RecordIdentity): Promise<boolean>
    {
        let verExt = await this.defExt.linkNewToHead(this.model, predecessorId);

        // Force a reload of dashboards to get latest versions if save successful
        if (verExt)
        {
            await this.svc.reloadDashboards(true, true);

            return true;
        }

        return false;
    }

    public async saveWithNotifications(predecessorExt?: DashboardDefinitionVersionExtended,
                                       successMessage?: string,
                                       failMessage?: string): Promise<boolean>
    {
        let saved = await this.save(predecessorExt?.getIdentity());
        if (saved)
        {
            if (successMessage) this.svc.errors.success(successMessage, -1);
        }
        else if (failMessage)
        {
            this.svc.errors.warn(failMessage, -1);
        }
        return saved;
    }

    //--//

    /**
     * Get a top-level widget's idx from an id
     * @param id
     */
    public getWidgetIdx(id: string): number
    {
        if (this.model.widgets)
        {
            for (let i = 0; i < this.model.widgets.length; i++)
            {
                if (this.model.widgets[i].config.id === id) return i;
            }
        }

        return -1;
    }

    public async addTimeSeriesWidget(config: Models.TimeSeriesWidgetConfiguration)
    {
        let outline = Models.WidgetOutline.newInstance({
                                                           width : 6,
                                                           height: 7
                                                       });

        let updatedCfg = this.updateWidget(config, outline);
        if (updatedCfg)
        {
            // successfully added: save a new version of the dashboard
            await updatedCfg.save(undefined);
        }
    }

    public updateSubWidget(parentId: string,
                           cfg: Models.WidgetConfiguration,
                           newSelectorNameLookup?: Lookup<Models.SharedAssetSelector>): DashboardConfigurationExtended
    {
        let cloned = this.cloneForEdit();

        return cloned.updateSubWidgetInPlace(parentId, cfg, newSelectorNameLookup) ? cloned : null;
    }

    private updateSubWidgetInPlace(parentId: string,
                                   cfg: Models.WidgetConfiguration,
                                   newSelectorNameLookup?: Lookup<Models.SharedAssetSelector>): boolean
    {
        let parentCfg = this.model.widgets[this.getWidgetIdx(parentId)]?.config;
        if (parentCfg instanceof Models.GroupingWidgetConfiguration && cfg)
        {
            let subWidget = parentCfg.widgets.find((subWidget) => subWidget.config.id === cfg.id);
            if (subWidget)
            {
                subWidget.config = cfg;
            }
            else
            {
                let composition = this.getWidgetComposition(parentCfg, cfg);
                if (!composition) return false;

                parentCfg.widgets.push(composition);
            }

            for (let selectorConfig of this.getNewAssetGraphSelectors(cfg, newSelectorNameLookup))
            {
                let selectorComposition = this.getWidgetComposition(parentCfg, selectorConfig);
                if (selectorComposition)
                {
                    parentCfg.widgets.push(selectorComposition);
                }
                else
                {
                    this.model.widgets.push(this.getWidgetComposition(selectorConfig));
                }
            }

            return true;
        }

        return false;
    }

    public addWidget(config: Models.WidgetConfiguration,
                     selectors: Models.SharedAssetSelector[],
                     graphs: Models.SharedAssetGraph[],
                     outline?: Models.WidgetOutline): DashboardConfigurationExtended
    {
        config.id = null;

        let cloned = this.cloneForEdit();

        cloned.addGraphs(graphs || []);
        for (let selector of selectors || [])
        {
            cloned.addSelector(selector);
        }

        if (outline?.width > this.widgetManipulator.numCols)
        {
            const scaleFactor = this.widgetManipulator.numCols / outline.width;
            outline.width     = Math.round(outline.width * scaleFactor);
            outline.height    = Math.round(outline.height * scaleFactor);
        }

        return cloned.updateWidgetInPlace(config, outline) ? cloned : null;
    }

    public updateWidget(config: Models.WidgetConfiguration,
                        outline?: Models.WidgetOutline,
                        newSelectorLookup?: Lookup<Models.SharedAssetSelector>): DashboardConfigurationExtended
    {
        let cloned = this.cloneForEdit();

        return cloned.updateWidgetInPlace(config, outline, newSelectorLookup) ? cloned : null;
    }

    private updateWidgetInPlace(config: Models.WidgetConfiguration,
                                outline?: Models.WidgetOutline,
                                newSelectorLookup?: Lookup<Models.SharedAssetSelector>): boolean
    {
        let widgetIdx = this.getWidgetIdx(config.id);
        if (widgetIdx >= 0)
        {
            this.model.widgets[widgetIdx].config = config;
        }
        else
        {
            let composition = this.getWidgetComposition(config, undefined, outline);
            if (!composition) return false;

            this.model.widgets.push(composition);
        }

        for (let selectorConfig of this.getNewAssetGraphSelectors(config, newSelectorLookup))
        {
            this.model.widgets.push(this.getWidgetComposition(selectorConfig));
        }

        return true;
    }

    private getWidgetComposition(config: Models.WidgetConfiguration,
                                 subConfig?: Models.WidgetConfiguration,
                                 outline?: Models.WidgetOutline): Models.WidgetComposition
    {
        let newConfig: Models.WidgetConfiguration;
        if (subConfig && config instanceof Models.GroupingWidgetConfiguration)
        {
            newConfig = subConfig;

            let minOutline: Models.WidgetOutline;
            if (!outline)
            {
                const parentNode     = new WidgetGraph(this.widgetManipulator).findNode((node) => node.widget.id === config.id);
                const subManipulator = WidgetManipulator.getInnerManipulator(parentNode?.widget?.container);
                if (subManipulator)
                {
                    outline    = subManipulator.getBaseOutline();
                    minOutline = subManipulator.getMinOutline();
                }
            }

            outline = GroupingWidgetConfigurationExtended.getSubwidgetOutline(config, outline, minOutline, null);
            if (!outline) return null;
        }
        else
        {
            newConfig = config;
            if (!outline)
            {
                let widgetDescriptorConfig = WidgetConfigurationExtended.fromConfigModel(config)
                                                                        .getDescriptor().config;

                outline = Models.WidgetOutline.newInstance({
                                                               width : widgetDescriptorConfig.defaultWidth,
                                                               height: widgetDescriptorConfig.defaultHeight
                                                           });
            }

            let outlines = this.model.widgets.map((widget) => widget.outline);
            let grid     = WidgetManipulator.generateMinimalWidgetRepresentation(outlines, -1, DashboardConfigurationExtended.numDashboardColumns);

            outline = WidgetManipulator.getPositionedOutline(outline, grid);
        }

        newConfig.id = UUID.UUID();
        return Models.WidgetComposition.newInstance({
                                                        config : newConfig,
                                                        outline: outline
                                                    });
    }
}

export class GraphContextUpdater
{
    get contextId(): string
    {
        return this.m_contextId;
    }

    set contextId(id: string)
    {
        if (this.m_contextId !== id)
        {
            this.m_contextId = id;
            this.updateGraphContext();
        }
    }

    private readonly m_activeDashboard: DashboardConfigurationExtended;

    private m_selectionChangedSub: Subscription;
    selectionChanged = new Subject<void>();

    constructor(private readonly m_dashboard: DashboardConfigurationExtended,
                public readonly name: string,
                public readonly selectorId: string,
                public readonly options: ControlOption<string>[],
                contextChanged: Observable<Models.AssetGraphContextAsset>,
                private m_contextId: string)
    {
        let activeDashboard = this.m_dashboard.svc.currentDashboardConfig.getValue();
        if (activeDashboard !== this.m_dashboard)
        {
            this.m_activeDashboard = activeDashboard;
        }

        this.m_selectionChangedSub = contextChanged.subscribe((context) => this.m_contextId = context.sysId);
    }

    private async updateGraphContext()
    {
        if (this.m_activeDashboard)
        {
            await this.m_activeDashboard.setGraphContext(this.selectorId, this.m_contextId);
        }

        await this.m_dashboard.setGraphContext(this.selectorId, this.m_contextId);

        this.selectionChanged.next();
    }

    destroy()
    {
        this.m_selectionChangedSub.unsubscribe();
    }
}

export class AssetContextSubscriptionPayload
{
    constructor(public readonly selectorId: string,
                public readonly fn: (context: Models.AssetGraphContextAsset) => Promise<void>)
    {}
}

export class WidgetGraph
{
    private readonly m_nodeLookup = new Map<string, WidgetNode>();

    get numNodes(): number
    {
        return this.m_nodeLookup.size;
    }

    constructor(widgetManipulator: WidgetManipulator)
    {
        for (let widgetContainer of widgetManipulator.widgetContainers)
        {
            this.addNode(widgetContainer.widget, null);
        }
    }

    private addNode(widget: WidgetBaseComponent<any, any>,
                    parent: WidgetNode): WidgetNode
    {
        let node = this.getNode(widget);
        if (!node)
        {
            node = new WidgetNode(this, widget, parent);
            this.m_nodeLookup.set(widget.id, node);

            let subManipulators: WidgetManipulator[] = [];
            widget.collectWidgetManipulators(subManipulators);
            for (let manipulator of subManipulators)
            {
                for (let childContainer of manipulator.widgetContainers)
                {
                    node.children.push(this.addNode(childContainer.widget, node));
                }
            }
        }

        return node;
    }

    getNode(widget: WidgetBaseComponent<any, any>): WidgetNode
    {
        return this.m_nodeLookup.get(widget?.id);
    }

    markNode(widget: WidgetBaseComponent<any, any>): boolean
    {
        let node = this.getNode(widget);
        if (node && !node.marked)
        {
            node.marked = true;
            return true;
        }

        return false;
    }

    canRemoveMarked(): boolean
    {
        return !this.findNode((node) => node.marked && !node.widget.canRemove(this));
    }

    findNode(fn: (node: WidgetNode) => boolean): WidgetNode
    {
        for (let node of this.m_nodeLookup.values())
        {
            if (fn(node)) return node;
        }

        return null;
    }

    findNodes(fn: (node: WidgetNode) => boolean,
              foundNodes?: WidgetNode[]): WidgetNode[]
    {
        if (!foundNodes) foundNodes = [];

        for (let node of this.m_nodeLookup.values())
        {
            if (fn(node)) foundNodes.push(node);
        }

        return foundNodes;
    }

    forEachNode(fn: (node: WidgetNode) => void)
    {
        for (let node of this.m_nodeLookup.values())
        {
            fn(node);
        }
    }
}

export class WidgetNode
{
    public readonly selectorIds: string[]  = [];
    public readonly children: WidgetNode[] = [];

    public marked: boolean = false;

    constructor(public readonly graph: WidgetGraph,
                public readonly widget: WidgetBaseComponent<Models.WidgetConfiguration, WidgetConfigurationExtended<any>>,
                public readonly parent: WidgetNode)
    {
        this.selectorIds = widget.configExt.getBindings()
                                 .map((binding) => binding.selectorId);
    }
}
