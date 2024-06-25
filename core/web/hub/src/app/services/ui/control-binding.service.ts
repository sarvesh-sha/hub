import {Injectable} from "@angular/core";
import {AlertDefinitionsService} from "app/services/domain/alert-definitions.service";

import {AlertsService} from "app/services/domain/alerts.service";
import {AssetsService} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {LocationsService} from "app/services/domain/locations.service";
import {MetricsDefinitionDetailsExtended, MetricsDefinitionExtended, MetricsDefinitionsService} from "app/services/domain/metrics-definitions.service";
import {NormalizationService} from "app/services/domain/normalization.service";
import {PanesService} from "app/services/domain/panes.service";
import {RoleExtended} from "app/services/domain/roles.service";
import {SettingsService} from "app/services/domain/settings.service";
import {EngineeringUnitsDescriptorExtended, UnitsService} from "app/services/domain/units.service";
import {WorkflowsService} from "app/services/domain/workflows.service";
import * as Models from "app/services/proxy/model/models";
import {LocationHierarchy} from "app/services/proxy/model/models";
import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {mapInParallel} from "framework/utils/concurrency";

@Injectable()
export class ControlBindingService
{
    constructor(private alertsService: AlertsService,
                private alertDefinitionsService: AlertDefinitionsService,
                private locationsService: LocationsService,
                private settingsService: SettingsService,
                private assetsService: AssetsService,
                private metricsDefinitions: MetricsDefinitionsService,
                private normalizationService: NormalizationService,
                private panesService: PanesService,
                private unitsService: UnitsService,
                private workflowsService: WorkflowsService)
    {

    }

    async getTags(): Promise<ControlOption<string>[]>
    {
        let summary = await this.assetsService.getTagsSummary();
        let keys    = Object.keys(summary.tagFrequency);
        return keys.map((key) =>
                        {
                            return {
                                tag  : key,
                                count: summary.tagFrequency[key]
                            };
                        })
                   .sort((a,
                          b) =>
                         {
                             return b.count - a.count;
                         })
                   .map((entry) =>
                        {
                            let text = entry.tag;

                            if (text.startsWith("$manual."))
                            {
                                text = text.substring("$manual.".length) + " (Manual)";
                            }

                            return new ControlOption(entry.tag, `${UtilsService.capitalizeFirstLetterAllWords(text)} [${entry.count}]`);
                        });
    }

    async getPointClasses(onlyUsed: boolean,
                          rules: Models.NormalizationRules,
                          ignoreAliases = true): Promise<ControlOption<string>[]>
    {
        if (!rules)
        {
            rules = await this.getActiveNormalizationRules();
        }

        let summary = await this.assetsService.getTagsSummary();

        let classes = rules ? rules.pointClasses : [];

        if (onlyUsed)
        {
            classes = classes.filter((pc) =>
                                     {
                                         let id = pc.id + "";
                                         return summary.pointClassesFrequency[id] > 0;
                                     });
        }

        if (ignoreAliases)
        {
            classes = classes.filter((pc) => !pc.aliasPointClassId);
        }

        return classes.map((pc) =>
                           {
                               let id    = pc.id + "";
                               let label = `${pc.pointClassName} - ${pc.pointClassDescription}`;

                               let freq = summary.pointClassesFrequency[id];
                               if (freq > 0)
                               {
                                   label = `${label} (${freq})`;
                               }

                               return new ControlOption(id, label);
                           })
                      .sort((a,
                             b) => UtilsService.compareStrings(a.label, b.label, true));
    }

    async getEquipmentClasses(onlyUsed: boolean,
                              rules: Models.NormalizationRules): Promise<ControlOption<string>[]>
    {
        if (!rules)
        {
            rules = await this.getActiveNormalizationRules();
        }

        let summary = await this.assetsService.getTagsSummary();

        let classes = rules ? rules.equipmentClasses : [];

        if (onlyUsed)
        {
            classes = classes.filter((ec) =>
                                     {
                                         let id = ec.id + "";
                                         return summary.equipmentClassesFrequency[id] > 0;
                                     });
        }

        return classes.map((ec) =>
                           {
                               let id    = ec.id + "";
                               let label = `${ec.equipClassName} - ${ec.description}`;

                               let freq = summary.equipmentClassesFrequency[id];
                               if (freq > 0)
                               {
                                   label = `${label} (${freq})`;
                               }

                               return new ControlOption(id, label);
                           })
                      .sort((a,
                             b) => UtilsService.compareStrings(a.label, b.label, true));
    }

    //--//

    async getLocations(): Promise<LocationHierarchy[]>
    {
        return this.locationsService.getLocationHierarchy();
    }

    async getLocationsOptions(): Promise<ControlOption<string>[]>
    {
        let hierarchy = await this.getLocations();

        return this.collectLocationsOptions(hierarchy);
    }

    private collectLocationsOptions(locationHierarchy: LocationHierarchy[]): ControlOption<string>[]
    {
        return SharedSvc.BaseService.mapOptions(locationHierarchy, (location) =>
        {
            let option   = new ControlOption<string>();
            option.id    = location.ri.sysId;
            option.label = location.name;

            let children = this.collectLocationsOptions(location.subLocations);
            if (children && children.length) option.children = children;

            return option;
        });
    }

    async getLocationTypeOptions(): Promise<ControlOption<string>[]>
    {
        let types = await this.locationsService.describeLocationTypes();
        return SharedSvc.BaseService.mapEnumOptions(types);
    }

    async getUsedLocationTypeOptions(): Promise<ControlOption<string>[]>
    {
        let locations   = await this.locationsService.getLocationHierarchy();
        let types       = await this.getLocationTypeOptions();
        const usedTypes = new Set<Models.LocationType>();
        this.collectLocationTypeOptions(usedTypes, locations);
        return types.filter((t) => usedTypes.has(<any>t.id));
    }

    private collectLocationTypeOptions(usedTypes: Set<Models.LocationType>,
                                       locations: Models.LocationHierarchy[])
    {
        if (!locations) return;

        for (let loc of locations)
        {
            usedTypes.add(loc.type);
            this.collectLocationTypeOptions(usedTypes, loc.subLocations);
        }
    }

    //--//

    async getMetrics(): Promise<MetricsDefinitionExtended[]>
    {
        return this.metricsDefinitions.getExtendedList();
    }

    async getMetricsOptions(): Promise<ControlOption<string>[]>
    {
        let metrics = await this.getMetrics();

        return this.collectMetricsOptions(metrics);
    }

    collectMetricsOptions(metrics: MetricsDefinitionExtended[]): ControlOption<string>[]
    {
        return SharedSvc.BaseService.mapOptions(metrics, (metric) =>
        {
            let option   = new ControlOption<string>();
            option.id    = metric.model.sysId;
            option.label = metric.model.title;
            return option;
        });
    }

    async getMetricsNamedOutput(): Promise<ControlOption<string>[]>
    {
        let metrics         = await this.getMetrics();
        let versions        = await mapInParallel(metrics, (metric) => metric.getRelease());
        let details         = versions.map((v) => v.getDetails());
        let detailsExtended = details.map((d) => MetricsDefinitionDetailsExtended.newInstance(null, this.metricsDefinitions.domain, d));
        let namedOutputs    = new Set<string>();
        for (let detail of detailsExtended)
        {
            detail.data
                  .filterBlocks<Models.MetricsEngineStatementSetOutputToSeriesWithName>((b) => b instanceof Models.MetricsEngineStatementSetOutputToSeriesWithName)
                  .forEach((b) => namedOutputs.add(b.name));
        }

        return UtilsService.mapIterable(namedOutputs, (n) => new ControlOption<string>(n, n))
                           .sort((a,
                                  b) => UtilsService.compareStrings(a.label, b.label, true));
    }

    //--//

    async getRoles(): Promise<ControlOption<string>[]>
    {
        let roles = await this.settingsService.getRolesList();
        return SharedSvc.BaseService.mapOptions<RoleExtended, string>(roles, (role) =>
        {
            if (!role) return null;

            let option   = new ControlOption<string>();
            option.id    = role.model.sysId;
            option.label = role.model.displayName;
            return option;
        });
    }

    async getUsers(includeLdap: boolean): Promise<ControlOption<string>[]>
    {
        let users = await this.settingsService.getUsersList(includeLdap);
        return SharedSvc.BaseService.mapOptions(users, (user) =>
        {
            if (!user) return null;

            let option   = new ControlOption<string>();
            option.id    = user.sysId;
            option.label = `${user.firstName} ${user.lastName}`;
            return option;
        });
    }

    async getGroups(): Promise<ControlOption<string>[]>
    {
        let groups = await this.settingsService.getGroupsList();
        return SharedSvc.BaseService.mapOptions(groups, (group) =>
        {
            if (!group) return null;

            let option   = new ControlOption<string>();
            option.id    = group.model.sysId;
            option.label = group.model.name;
            return option;
        });
    }

    async getActiveNormalizationRules(): Promise<Models.NormalizationRules>
    {
        let normalization = await this.normalizationService.getActiveRules();
        return normalization.rules;
    }

    async getUnits(): Promise<ControlOption<EngineeringUnitsDescriptorExtended>[]>
    {
        let units = await this.unitsService.describeEngineeringUnits();
        return units.map((unit) => unit.controlPointWithDescription);
    }

    getAlertStates(): Promise<ControlOption<Models.AlertStatus>[]>
    {
        return this.alertsService.getStates();
    }

    getAlertTypes(): Promise<ControlOption<Models.AlertType>[]>
    {
        return this.alertsService.getTypes();
    }

    getAlertSeverities(): Promise<ControlOption<Models.AlertSeverity>[]>
    {
        return this.alertsService.getSeverities();
    }

    async getAlertRules(): Promise<ControlOption<Models.RecordIdentity>[]>
    {
        let allRules = await this.alertDefinitionsService.getExtendedList();

        return allRules.map((rule) => new ControlOption(rule.getIdentity(), rule.model.title));
    }

    getWorkflowPriorities(): Promise<ControlOption<Models.WorkflowPriority>[]>
    {
        return this.workflowsService.getPriorities();
    }

    getWorkflowStatuses(): Promise<ControlOption<Models.WorkflowStatus>[]>
    {
        return this.workflowsService.getStatuses();
    }

    getWorkflowTypes(): Promise<ControlOption<Models.WorkflowType>[]>
    {
        return this.workflowsService.getTypes();
    }


    async getPaneConfigurations(): Promise<ControlOption<string>[]>
    {
        let allPaneIds = await this.panesService.getPaneIds();
        let allPanes   = await this.panesService.getConfigBatch(allPaneIds);

        return allPanes.map((pane) => new ControlOption(pane.id, pane.name));
    }

    getBacnetObjectTypes(): ControlOption<string>[]
    {
        return [
            new ControlOption("analog_input", "analog_input"),
            new ControlOption("analog_output", "analog_output"),
            new ControlOption("analog_value", "analog_value"),
            new ControlOption("large_analog_value", "large_analog_value"),
            new ControlOption("binary_input", "binary_input"),
            new ControlOption("binary_output", "binary_output"),
            new ControlOption("binary_value", "binary_value"),
            new ControlOption("multi_state_input", "multi_state_input"),
            new ControlOption("multi_state_output", "multi_state_output"),
            new ControlOption("multi_state_value", "multi_state_value"),
            new ControlOption("integer_value", "integer_value"),
            new ControlOption("positive_integer_value", "positive_integer_value"),
            new ControlOption("accumulator", "accumulator"),
            new ControlOption("device", "device"),
            new ControlOption("file", "file"),
            new ControlOption("trend_log", "trend_log")
        ];
    }

}
