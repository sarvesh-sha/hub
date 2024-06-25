import {Component} from "@angular/core";

import {AlertDefinitionExtended} from "app/services/domain/alert-definitions.service";
import * as Models from "app/services/proxy/model/models";

import {LocationFiltersAdapter} from "app/shared/filter/filters-adapter";
import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {FilterChip} from "framework/ui/shared/filter-chips-container.component";

@Component({
               selector   : "o3-alert-filters-adapter[request]",
               templateUrl: "./alert-filters-adapter.component.html"
           })
export class AlertFiltersAdapterComponent extends LocationFiltersAdapter<Models.AlertFilterRequest>
{
    optionsLoaded: boolean = false;

    alertStatusOptions: ControlOption<Models.AlertStatus>[];
    alertTypeOptions: ControlOption<Models.AlertType>[];
    alertRuleOptions: ControlOption<string>[];
    alertSeverityOptions: ControlOption<Models.AlertSeverity>[];

    private m_alertRules: string[];
    get alertRules(): string[]
    {
        return this.m_alertRules;
    }

    set alertRules(rules: string[])
    {
        this.m_alertRules           = rules;
        this.editRequest.alertRules = rules.map((rule) => AlertDefinitionExtended.newIdentity(rule));
    }

    public async ngOnInit()
    {
        super.ngOnInit();

        let alertRuleOptions: ControlOption<Models.RecordIdentity>[];
        [
            this.alertStatusOptions,
            this.alertTypeOptions,
            alertRuleOptions,
            this.alertSeverityOptions
        ]                     = await Promise.all([
                                                      await this.app.bindings.getAlertStates(),
                                                      await this.app.bindings.getAlertTypes(),
                                                      await this.app.bindings.getAlertRules(),
                                                      await this.app.bindings.getAlertSeverities()
                                                  ]);
        this.alertRuleOptions = alertRuleOptions.map((option) => new ControlOption(option.id.sysId, option.label));

        this.optionsLoaded = true;
    }

    protected editRequestUpdated()
    {
        this.m_alertRules = this.editRequest?.alertRules?.map((ruleRecord) => ruleRecord.sysId) || [];
        this.markForCheck();
    }

    protected updateGlobalFilters()
    {
        super.updateGlobalFilters();

        this.filtersSvc.likeDeviceManufacturerName = this.m_request.likeDeviceManufacturerName;
        this.filtersSvc.likeDeviceProductName      = this.m_request.likeDeviceProductName;
        this.filtersSvc.likeDeviceModelName        = this.m_request.likeDeviceModelName;
        this.filtersSvc.alertStatusIDs             = this.m_request.alertStatusIDs;
        this.filtersSvc.alertTypeIDs               = this.m_request.alertTypeIDs;
        this.filtersSvc.alertRules                 = this.m_request.alertRules?.map((ruleRecordId) => ruleRecordId.sysId);
        this.filtersSvc.alertSeverityIDs           = this.m_request.alertSeverityIDs;
    }

    protected syncWithGlobalFilters()
    {
        super.syncWithGlobalFilters();

        this.m_request.likeDeviceManufacturerName = this.filtersSvc.likeDeviceManufacturerName;
        this.m_request.likeDeviceProductName      = this.filtersSvc.likeDeviceProductName;
        this.m_request.likeDeviceModelName        = this.filtersSvc.likeDeviceModelName;
        this.m_request.alertStatusIDs             = this.filtersSvc.alertStatusIDs;
        this.m_request.alertTypeIDs               = this.filtersSvc.alertTypeIDs;
        this.m_request.alertRules                 = this.filtersSvc.alertRules.map((ruleId) => AlertDefinitionExtended.newIdentity(ruleId));
        this.m_request.alertSeverityIDs           = this.filtersSvc.alertSeverityIDs;
    }

    protected emptyRequestInstance(): Models.AlertFilterRequest
    {
        return new Models.AlertFilterRequest();
    }

    protected newRequestInstance(request?: Models.AlertFilterRequest): Models.AlertFilterRequest
    {
        request = Models.AlertFilterRequest.newInstance(request);

        if (!request.alertStatusIDs) request.alertStatusIDs = [Models.AlertStatus.active];

        return request;
    }

    protected async appendChips(chips: FilterChip[]): Promise<void>
    {
        await super.appendChips(chips);

        let alertChips = await Promise.all(
            [
                new FilterChip("Model #",
                               () =>
                               {
                                   this.resetEditRequest();
                                   this.m_editRequest.likeDeviceModelName = undefined;
                                   this.applyFilterEdits();
                               },
                               () => this.m_request.likeDeviceModelName ? [this.m_request.likeDeviceModelName] : []),
                new FilterChip("Product Name",
                               () =>
                               {
                                   this.resetEditRequest();
                                   this.m_editRequest.likeDeviceProductName = undefined;
                                   this.applyFilterEdits();
                               },
                               () => this.m_request.likeDeviceProductName ? [this.m_request.likeDeviceProductName] : []),
                new FilterChip("Manufacturer",
                               () =>
                               {
                                   this.resetEditRequest();
                                   this.m_editRequest.likeDeviceManufacturerName = undefined;
                                   this.applyFilterEdits();
                               },
                               () => this.m_request.likeDeviceManufacturerName ? [this.m_request.likeDeviceManufacturerName] : []),
                new FilterChip("Alert State",
                               () =>
                               {
                                   this.resetEditRequest();
                                   this.m_editRequest.alertStatusIDs = [];
                                   this.applyFilterEdits();
                               },
                               () => this.m_request.alertStatusIDs,
                               await this.app.bindings.getAlertStates()),
                new FilterChip("Alert Type",
                               () =>
                               {
                                   this.resetEditRequest();
                                   this.m_editRequest.alertTypeIDs = [];
                                   this.applyFilterEdits();
                               },
                               () => this.m_request.alertTypeIDs,
                               await this.app.bindings.getAlertTypes()),
                new FilterChip("Alert Severity",
                               () =>
                               {
                                   this.resetEditRequest();
                                   this.m_editRequest.alertSeverityIDs = [];
                                   this.applyFilterEdits();
                               },
                               () => this.m_request.alertSeverityIDs,
                               await this.app.bindings.getAlertSeverities())
            ]);

        for (let chip of alertChips) chips.push(chip);

        let alertRules = await this.app.bindings.getAlertRules();
        chips.push(new FilterChip("Alert Rule",
                                  () =>
                                  {
                                      this.resetEditRequest();
                                      this.alertRules = [];
                                      this.applyFilterEdits();
                                  },
                                  () => this.m_request.alertRules?.map((recordId) => recordId.sysId),
                                  alertRules.map((ruleOption) => new ControlOption<string>(ruleOption.id.sysId, ruleOption.label))));
    }

    protected areEquivalent(requestA: Models.AlertFilterRequest,
                            requestB: Models.AlertFilterRequest): boolean
    {
        if (!super.areEquivalent(requestA, requestB)) return false;

        if (requestA.likeDeviceManufacturerName != requestB.likeDeviceManufacturerName) return false;
        if (requestA.likeDeviceProductName != requestB.likeDeviceProductName) return false;
        if (requestA.likeDeviceModelName != requestB.likeDeviceModelName) return false;

        if (!UtilsService.compareArraysAsSets(requestA.alertStatusIDs, requestB.alertStatusIDs)) return false;
        if (!UtilsService.compareArraysAsSets(requestA.alertTypeIDs, requestB.alertTypeIDs)) return false;
        if (!UtilsService.compareArraysAsSets(requestA.alertSeverityIDs, requestB.alertSeverityIDs)) return false;

        let alertRulesA = requestA.alertRules?.map((recordId) => recordId.sysId) || [];
        let alertRulesB = requestB.alertRules?.map((recordId) => recordId.sysId) || [];
        return UtilsService.compareArraysAsSets(alertRulesA, alertRulesB);
    }
}
