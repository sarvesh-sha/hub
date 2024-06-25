import {Injectable, NgModule} from "@angular/core";
import {AlertDefinitionVersionsService} from "app/services/domain/alert-definition-versions.service";
import {AlertDefinitionsService} from "app/services/domain/alert-definitions.service";

import {AlertsHistoryService} from "app/services/domain/alert-history.service";
import {AlertsService} from "app/services/domain/alerts.service";
import {ApiService} from "app/services/domain/api.service";
import {AssetGraphService} from "app/services/domain/asset-graph.service";
import {AssetsService} from "app/services/domain/assets.service";
import {AuthGuard} from "app/services/domain/auth.guard";
import {BackgroundActivitiesService} from "app/services/domain/background-activities.service";
import {BookmarkGuard} from "app/services/domain/bookmark.guard";
import {BookmarkService} from "app/services/domain/bookmark.service";
import {ClipboardService} from "app/services/domain/clipboard.service";
import {DashboardDefinitionVersionsService} from "app/services/domain/dashboard-definition-versions.service";
import {DashboardDefinitionsService} from "app/services/domain/dashboard-definitions.service";
import {DashboardManagementService} from "app/services/domain/dashboard-management.service";
import {DataImportsService} from "app/services/domain/data-imports.service";
import {DatabaseActivityService} from "app/services/domain/database-activity.service";
import {DevicesService} from "app/services/domain/devices.service";
import {DigineousService} from "app/services/domain/digineous.service";
import {EnumsService} from "app/services/domain/enums.service";
import {EventsService} from "app/services/domain/events.service";
import {FiltersService} from "app/services/domain/filters.service";
import {GatewayProberOperationsService} from "app/services/domain/gateway-prober-operations.service";
import {LocationsService} from "app/services/domain/locations.service";
import {MessageBusService} from "app/services/domain/message-bus.service";
import {MetricsDefinitionVersionsService} from "app/services/domain/metrics-definition-versions.service";
import {MetricsDefinitionsService} from "app/services/domain/metrics-definitions.service";
import {NormalizationService} from "app/services/domain/normalization.service";
import {PanesService} from "app/services/domain/panes.service";
import {ReportDefinitionVersionsService} from "app/services/domain/report-definition-versions.service";
import {ReportDefinitionsService} from "app/services/domain/report-definitions.service";
import {ReportingService} from "app/services/domain/reporting.service";
import {ReportsService} from "app/services/domain/reports.service";
import {RolesService} from "app/services/domain/roles.service";
import {SearchService} from "app/services/domain/search.service";
import {SettingsService} from "app/services/domain/settings.service";
import {UnitsService} from "app/services/domain/units.service";
import {UserGroupsService} from "app/services/domain/user-groups.service";
import {UserMessagesService} from "app/services/domain/user-messages.service";
import {UsersService} from "app/services/domain/users.service";
import {WidgetDataService} from "app/services/domain/widget-data.service";
import {WorkflowsHistoryService} from "app/services/domain/workflow-history.service";
import {WorkflowsService} from "app/services/domain/workflows.service";
import {ApiClientConfiguration} from "framework/services/api.client";
import {UserManagementService} from "./user-management.service";

@Injectable({providedIn: "root"})
export class AppDomainContext
{
    constructor(public alertDefinitions: AlertDefinitionsService,
                public alertDefinitionVersions: AlertDefinitionVersionsService,
                public alerts: AlertsService,
                public alertsHistory: AlertsHistoryService,
                public apiConfiguration: ApiClientConfiguration,
                public apis: ApiService,
                public assetGraphs: AssetGraphService,
                public assets: AssetsService,
                public bookmarks: BookmarkService,
                public backgroundActivities: BackgroundActivitiesService,
                public dashboard: DashboardManagementService,
                public dashboardDefinitions: DashboardDefinitionsService,
                public dashboardDefinitionVersions: DashboardDefinitionVersionsService,
                public dataImports: DataImportsService,
                public devices: DevicesService,
                public digineous: DigineousService,
                public enums: EnumsService,
                public events: EventsService,
                public filters: FiltersService,
                public gatewayProberOperations: GatewayProberOperationsService,
                public locations: LocationsService,
                public metricsDefinitions: MetricsDefinitionsService,
                public metricsDefinitionVersions: MetricsDefinitionVersionsService,
                public normalization: NormalizationService,
                public panes: PanesService,
                public reportDefinitions: ReportDefinitionsService,
                public reportDefinitionVersions: ReportDefinitionVersionsService,
                public reports: ReportsService,
                public reporting: ReportingService,
                public search: SearchService,
                public settings: SettingsService,
                public units: UnitsService,
                public userMessages: UserMessagesService,
                public userGroups: UserGroupsService,
                public users: UsersService,
                public userManagement: UserManagementService,
                public widgetData: WidgetDataService,
                public workflows: WorkflowsService,
                public workflowsHistory: WorkflowsHistoryService)
    {
    }
}

@NgModule({
              providers: [
                  AlertDefinitionsService,
                  AlertDefinitionVersionsService,
                  AlertsHistoryService,
                  AlertsService,
                  ApiService,
                  AssetsService,
                  AuthGuard,
                  BookmarkGuard,
                  BookmarkService,
                  ClipboardService,
                  BackgroundActivitiesService,
                  DashboardManagementService,
                  DashboardDefinitionsService,
                  DashboardDefinitionVersionsService,
                  DatabaseActivityService,
                  DataImportsService,
                  DevicesService,
                  DigineousService,
                  EnumsService,
                  EventsService,
                  FiltersService,
                  GatewayProberOperationsService,
                  LocationsService,
                  MessageBusService,
                  MetricsDefinitionsService,
                  MetricsDefinitionVersionsService,
                  NormalizationService,
                  PanesService,
                  ReportDefinitionsService,
                  ReportDefinitionVersionsService,
                  ReportingService,
                  ReportsService,
                  RolesService,
                  SearchService,
                  SettingsService,
                  UserMessagesService,
                  UserGroupsService,
                  UsersService,
                  UserManagementService,
                  WidgetDataService,
                  WorkflowsService,
                  WorkflowsHistoryService
              ]
          })
export class DomainModule {}
