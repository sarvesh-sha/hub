// import dependencies
import {Injectable, Injector} from "@angular/core";

import * as Apis from "app/services/proxy/api/api";

import {ApiClient} from "framework/services/api.client";

@Injectable()
export class ApiService
{
    readonly basePath: string;

    adminTasks: Apis.AdminTasksApi;
    alertDefinitionVersions: Apis.AlertDefinitionVersionsApi;
    alertDefinitions: Apis.AlertDefinitionsApi;
    alerts: Apis.AlertsApi;
    assetRelationships: Apis.AssetRelationshipsApi;
    assetTimeSeries: Apis.AssetTimeSeriesApi;
    assets: Apis.AssetsApi;
    backgroundActivities: Apis.BackgroundActivitiesApi;
    dashboardDefinitionVersions: Apis.DashboardDefinitionVersionsApi;
    dashboardDefinitions: Apis.DashboardDefinitionsApi;
    dataConnections: Apis.DataConnectionApi;
    dataImports: Apis.DataImportsApi;
    demoTasks: Apis.DemoTasksApi;
    deviceElements: Apis.DeviceElementsApi;
    devices: Apis.DevicesApi;
    discovery: Apis.DiscoveryApi;
    enums: Apis.EnumsApi;
    events: Apis.EventsApi;
    exports: Apis.ExportsApi;
    gateways: Apis.GatewaysApi;
    gatewayProberOperations: Apis.GatewayProberOperationsApi;
    hosts: Apis.HostsApi;
    locations: Apis.LocationsApi;
    metricsDefinitionVersions: Apis.MetricsDefinitionVersionsApi;
    metricsDefinitions: Apis.MetricsDefinitionsApi;
    networks: Apis.NetworksApi;
    normalization: Apis.NormalizationsApi;
    reportDefinitionVersions: Apis.ReportDefinitionVersionsApi;
    reportDefinitions: Apis.ReportDefinitionsApi;
    reports: Apis.ReportsApi;
    roles: Apis.RolesApi;
    search: Apis.SearchApi;
    systemPreferences: Apis.SystemPreferencesApi;
    units: Apis.UnitsApi;
    userGroups: Apis.UserGroupsApi;
    userMessages: Apis.UserMessagesApi;
    userPreferences: Apis.UserPreferencesApi;
    users: Apis.UsersApi;
    workflows: Apis.WorkflowsApi;

    constructor(private client: ApiClient,
                public injector: Injector)
    {
        let origin   = this.client.configuration.apiDomain;
        let location = document.location;

        if (!origin || origin == "")
        {
            if (this.client.configuration.apiPort)
            {
                origin = `${location.protocol}//${location.hostname}:${this.client.configuration.apiPort}`;
            }
            else
            {
                origin = location.origin;
            }
        }

        this.basePath = origin + "/api/v1";

        //--//

        this.adminTasks                  = new Apis.AdminTasksApi(client, this.basePath);
        this.alertDefinitionVersions     = new Apis.AlertDefinitionVersionsApi(client, this.basePath);
        this.alertDefinitions            = new Apis.AlertDefinitionsApi(client, this.basePath);
        this.alerts                      = new Apis.AlertsApi(client, this.basePath);
        this.assetRelationships          = new Apis.AssetRelationshipsApi(client, this.basePath);
        this.assetTimeSeries             = new Apis.AssetTimeSeriesApi(client, this.basePath);
        this.assets                      = new Apis.AssetsApi(client, this.basePath);
        this.backgroundActivities        = new Apis.BackgroundActivitiesApi(client, this.basePath);
        this.dashboardDefinitionVersions = new Apis.DashboardDefinitionVersionsApi(client, this.basePath);
        this.dashboardDefinitions        = new Apis.DashboardDefinitionsApi(client, this.basePath);
        this.dataConnections             = new Apis.DataConnectionApi(client, this.basePath);
        this.dataImports                 = new Apis.DataImportsApi(client, this.basePath);
        this.demoTasks                   = new Apis.DemoTasksApi(client, this.basePath);
        this.deviceElements              = new Apis.DeviceElementsApi(client, this.basePath);
        this.devices                     = new Apis.DevicesApi(client, this.basePath);
        this.discovery                   = new Apis.DiscoveryApi(client, this.basePath);
        this.enums                       = new Apis.EnumsApi(client, this.basePath);
        this.events                      = new Apis.EventsApi(client, this.basePath);
        this.exports                     = new Apis.ExportsApi(client, this.basePath);
        this.gateways                    = new Apis.GatewaysApi(client, this.basePath);
        this.gatewayProberOperations     = new Apis.GatewayProberOperationsApi(client, this.basePath);
        this.hosts                       = new Apis.HostsApi(client, this.basePath);
        this.locations                   = new Apis.LocationsApi(client, this.basePath);
        this.metricsDefinitionVersions   = new Apis.MetricsDefinitionVersionsApi(client, this.basePath);
        this.metricsDefinitions          = new Apis.MetricsDefinitionsApi(client, this.basePath);
        this.networks                    = new Apis.NetworksApi(client, this.basePath);
        this.normalization               = new Apis.NormalizationsApi(client, this.basePath);
        this.reportDefinitionVersions    = new Apis.ReportDefinitionVersionsApi(client, this.basePath);
        this.reportDefinitions           = new Apis.ReportDefinitionsApi(client, this.basePath);
        this.reports                     = new Apis.ReportsApi(client, this.basePath);
        this.roles                       = new Apis.RolesApi(client, this.basePath);
        this.search                      = new Apis.SearchApi(client, this.basePath);
        this.systemPreferences           = new Apis.SystemPreferencesApi(client, this.basePath);
        this.units                       = new Apis.UnitsApi(client, this.basePath);
        this.userGroups                  = new Apis.UserGroupsApi(client, this.basePath);
        this.userMessages                = new Apis.UserMessagesApi(client, this.basePath);
        this.userPreferences             = new Apis.UserPreferencesApi(client, this.basePath);
        this.users                       = new Apis.UsersApi(client, this.basePath);
        this.workflows                   = new Apis.WorkflowsApi(client, this.basePath);
    }
}
