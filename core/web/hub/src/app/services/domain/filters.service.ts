import {Injectable} from "@angular/core";
import {UUID} from "angular2-uuid";
import {AlertDefinitionExtended} from "app/services/domain/alert-definitions.service";
import {UsersService} from "app/services/domain/users.service";
import * as Models from "app/services/proxy/model/models";
import {UtilsService} from "framework/services/utils.service";
import {Future} from "framework/utils/concurrency";
import {AsyncDebouncer} from "framework/utils/debouncers";

@Injectable()
export class FiltersService
{
    private static readonly path: string           = "SAVED_FILTERS";
    private static readonly preferenceName: string = "filters";

    get locationIDs(): string[]
    {
        if (!this.filterPreferences) return undefined;
        return UtilsService.arrayCopy(this.filterPreferences.locationIDs);
    }

    set locationIDs(locationIDs: string[])
    {
        if (!this.filterPreferences || UtilsService.compareArraysAsSets(this.locationIDs, locationIDs)) return;
        this.filterPreferences.locationIDs = UtilsService.arrayCopy(locationIDs);
        this.refreshPreferences();
    }

    get equipmentClassIDs(): string[]
    {
        if (!this.filterPreferences) return undefined;
        return UtilsService.arrayCopy(this.filterPreferences.equipmentClassIDs);
    }

    set equipmentClassIDs(equipmentClassIDs: string[])
    {
        if (!this.filterPreferences || UtilsService.compareArraysAsSets(this.equipmentClassIDs, equipmentClassIDs)) return;
        this.filterPreferences.equipmentClassIDs = UtilsService.arrayCopy(equipmentClassIDs);
        this.refreshPreferences();
    }

    get equipmentIDs(): string[]
    {
        if (!this.filterPreferences) return undefined;
        return UtilsService.arrayCopy(this.filterPreferences.equipmentIDs);
    }

    set equipmentIDs(equipmentIDs: string[])
    {
        if (!this.filterPreferences || UtilsService.compareArraysAsSets(this.equipmentIDs, equipmentIDs)) return;
        this.filterPreferences.equipmentIDs = UtilsService.arrayCopy(equipmentIDs);
        this.refreshPreferences();
    }

    get deviceIDs(): string[]
    {
        if (!this.filterPreferences) return undefined;
        return UtilsService.arrayCopy(this.filterPreferences.deviceIDs);
    }

    set deviceIDs(deviceIDs: string[])
    {
        if (!this.filterPreferences || UtilsService.compareArraysAsSets(this.deviceIDs, deviceIDs)) return;
        this.filterPreferences.deviceIDs = UtilsService.arrayCopy(deviceIDs);
        this.refreshPreferences();
    }

    get pointClassIDs(): string[]
    {
        if (!this.filterPreferences) return undefined;
        return UtilsService.arrayCopy(this.filterPreferences.pointClassIDs);
    }

    set pointClassIDs(pointClassIDs: string[])
    {
        if (!this.filterPreferences || UtilsService.compareArraysAsSets(this.pointClassIDs, pointClassIDs)) return;
        this.filterPreferences.pointClassIDs = UtilsService.arrayCopy(pointClassIDs);
        this.refreshPreferences();
    }

    get likeDeviceManufacturerName(): string
    {
        return this.filterPreferences?.likeDeviceManufacturerName;
    }

    set likeDeviceManufacturerName(like: string)
    {
        if (!this.filterPreferences || this.likeDeviceManufacturerName == like) return;
        this.filterPreferences.likeDeviceManufacturerName = like;
        this.refreshPreferences();
    }

    get likeDeviceProductName(): string
    {
        return this.filterPreferences?.likeDeviceProductName;
    }

    set likeDeviceProductName(like: string)
    {
        if (!this.filterPreferences || this.likeDeviceProductName == like) return;
        this.filterPreferences.likeDeviceProductName = like;
        this.refreshPreferences();
    }

    get likeDeviceModelName(): string
    {
        return this.filterPreferences?.likeDeviceModelName;
    }

    set likeDeviceModelName(like: string)
    {
        if (!this.filterPreferences || this.filterPreferences.likeDeviceModelName == like) return;
        this.filterPreferences.likeDeviceModelName = like;
        this.refreshPreferences();
    }

    get sampling(): Models.FilterPreferenceBoolean
    {
        if (!this.filterPreferences) return undefined;
        return this.filterPreferences.isSampling || null;
    }

    set sampling(state: Models.FilterPreferenceBoolean)
    {
        if (!this.filterPreferences || this.sampling === state) return;
        this.filterPreferences.isSampling = state || null;
        this.refreshPreferences();
    }

    get classified(): Models.FilterPreferenceBoolean
    {
        if (!this.filterPreferences) return undefined;
        return this.filterPreferences.isClassified || null;
    }

    set classified(state: Models.FilterPreferenceBoolean)
    {
        if (!this.filterPreferences || this.classified === state) return;
        this.filterPreferences.isClassified = state || null;
        this.refreshPreferences();
    }

    get alertStatusIDs(): Models.AlertStatus[]
    {
        if (!this.filterPreferences) return undefined;
        let alertStatusIDs = this.filterPreferences.alertStatusIDs;
        return alertStatusIDs ? UtilsService.arrayCopy(alertStatusIDs) : alertStatusIDs;
    }

    set alertStatusIDs(alertStatusIDs: Models.AlertStatus[])
    {
        if (!this.filterPreferences || UtilsService.compareArraysAsSets(this.alertStatusIDs, alertStatusIDs)) return;
        this.filterPreferences.alertStatusIDs = alertStatusIDs ? UtilsService.arrayCopy(alertStatusIDs) : alertStatusIDs;
        this.refreshPreferences();
    }

    get alertTypeIDs(): Models.AlertType[]
    {
        if (!this.filterPreferences) return undefined;
        return UtilsService.arrayCopy(<Models.AlertType[]>this.filterPreferences.alertTypeIDs);
    }

    set alertTypeIDs(alertTypeIDs: Models.AlertType[])
    {
        if (!this.filterPreferences || UtilsService.compareArraysAsSets(this.alertTypeIDs, alertTypeIDs)) return;
        this.filterPreferences.alertTypeIDs = UtilsService.arrayCopy(alertTypeIDs);
        this.refreshPreferences();
    }

    get alertRules(): string[]
    {
        if (!this.filterPreferences) return undefined;
        let rules = this.filterPreferences.alertRules ?? [];
        return rules.map((recordId) => recordId.sysId);
    }

    set alertRules(alertRules: string[])
    {
        if (!this.filterPreferences || UtilsService.compareArraysAsSets(this.alertRules, alertRules)) return;
        this.filterPreferences.alertRules = alertRules.map((id) => AlertDefinitionExtended.newIdentity(id));
        this.refreshPreferences();
    }

    get alertSeverityIDs(): Models.AlertSeverity[]
    {
        if (!this.filterPreferences) return undefined;
        return UtilsService.arrayCopy(this.filterPreferences.alertSeverityIDs);
    }

    set alertSeverityIDs(alertSeverityIDs: Models.AlertSeverity[])
    {
        if (!this.filterPreferences || UtilsService.compareArraysAsSets(this.alertSeverityIDs, alertSeverityIDs)) return;
        this.filterPreferences.alertSeverityIDs = UtilsService.arrayCopy(alertSeverityIDs);
        this.refreshPreferences();
    }

    get workflowPriorityIDs(): Models.WorkflowPriority[]
    {
        if (!this.filterPreferences) return undefined;
        return UtilsService.arrayCopy(this.filterPreferences.workflowPriorityIDs);
    }

    set workflowPriorityIDs(workflowPriorityIDs: Models.WorkflowPriority[])
    {
        if (!this.filterPreferences || UtilsService.compareArraysAsSets(this.workflowPriorityIDs, workflowPriorityIDs)) return;
        this.filterPreferences.workflowPriorityIDs = UtilsService.arrayCopy(workflowPriorityIDs);
        this.refreshPreferences();
    }

    get workflowStatusIDs(): Models.WorkflowStatus[]
    {
        if (!this.filterPreferences) return undefined;
        return UtilsService.arrayCopy(this.filterPreferences.workflowStatusIDs);
    }

    set workflowStatusIDs(workflowStatusIDs: Models.WorkflowStatus[])
    {
        if (!this.filterPreferences || UtilsService.compareArraysAsSets(this.workflowStatusIDs, workflowStatusIDs)) return;
        this.filterPreferences.workflowStatusIDs = UtilsService.arrayCopy(workflowStatusIDs);
        this.refreshPreferences();
    }

    get workflowTypeIDs(): Models.WorkflowType[]
    {
        if (!this.filterPreferences) return undefined;
        return UtilsService.arrayCopy(this.filterPreferences.workflowTypeIDs);
    }

    set workflowTypeIDs(workflowTypeIDs: Models.WorkflowType[])
    {
        if (!this.filterPreferences || UtilsService.compareArraysAsSets(this.workflowTypeIDs, workflowTypeIDs)) return;
        this.filterPreferences.workflowTypeIDs = UtilsService.arrayCopy(workflowTypeIDs);
        this.refreshPreferences();
    }

    get createdByIDs(): string[]
    {
        if (!this.filterPreferences) return undefined;
        return UtilsService.arrayCopy(this.filterPreferences.createdByIDs);
    }

    set createdByIDs(createdByIDs: string[])
    {
        if (!this.filterPreferences || UtilsService.compareArraysAsSets(this.createdByIDs, createdByIDs)) return;
        this.filterPreferences.createdByIDs = UtilsService.arrayCopy(createdByIDs);
        this.refreshPreferences();
    }

    get assignedToIDs(): string[]
    {
        if (!this.filterPreferences) return undefined;
        return UtilsService.arrayCopy(this.filterPreferences.assignedToIDs);
    }

    set assignedToIDs(assignedToIDs: string[])
    {
        if (!this.filterPreferences || UtilsService.compareArraysAsSets(this.assignedToIDs, assignedToIDs)) return;
        this.filterPreferences.assignedToIDs = UtilsService.arrayCopy(assignedToIDs);
        this.refreshPreferences();
    }

    get savedFilters(): Models.FilterPreferences[]
    {
        return this.m_savedFilterPreferences;
    }

    readonly initialized = new Future<void>();

    private readonly m_filterPreferencesRefresher = new AsyncDebouncer(1000,
                                                                       () => this.users.setTypedPreference<Models.FilterPreferences>(null, FiltersService.preferenceName, this.filterPreferences));

    private filterPreferences: Models.FilterPreferences;
    private m_savedFilterPreferences: Models.FilterPreferences[] = [];

    constructor(private users: UsersService)
    {
        this.users.loggedIn.subscribe(() => this.initialize());
        this.users.loggedOut.subscribe(() =>
                                       {
                                           this.filterPreferences        = null;
                                           this.m_savedFilterPreferences = [];
                                       });
    }

    async initialize()
    {
        // Restore global filter
        try
        {
            this.filterPreferences = await this.users.getTypedPreference<Models.FilterPreferences>(null, FiltersService.preferenceName, Models.FilterPreferences.fixupPrototype);
        }
        catch (e)
        {
        }

        if (!this.filterPreferences) this.filterPreferences = new Models.FilterPreferences();

        // Restore saved filters
        await this.refreshSavedFilters();

        // Mark as initialized
        this.initialized.resolve();
    }

    private refreshPreferences()
    {
        this.m_filterPreferencesRefresher.invoke();
    }

    private async refreshSavedFilters()
    {
        // Load existing saved filters
        let ids     = await this.users.getPreferenceValues(FiltersService.path);
        let filters = [];
        for (let id of ids)
        {
            // Get the full model
            filters.push(await this.users.getTypedPreference<Models.FilterPreferences>(FiltersService.path,
                                                                                       id,
                                                                                       Models.FilterPreferences.fixupPrototype));
        }

        // Assign the filters to service
        this.m_savedFilterPreferences = filters;
    }

    public async saveFilter(filter: Models.FilterPreferences,
                            name: string)
    {
        // Assign new id and use name given
        filter.id   = UUID.UUID();
        filter.name = name;

        // Save the new filter
        await this.users.setTypedPreference<Models.FilterPreferences>(FiltersService.path, filter.id, filter);

        // Reload filters
        await this.refreshSavedFilters();
    }

    public async updateFilter(oldFilter: Models.FilterPreferences,
                              newFilter: Models.FilterPreferences)
    {
        // Assign the old name and id
        newFilter.id   = oldFilter.id;
        newFilter.name = oldFilter.name;

        // Re-save the filter
        await this.users.setTypedPreference<Models.FilterPreferences>(FiltersService.path, newFilter.id, newFilter);

        // Reload filters
        await this.refreshSavedFilters();
    }

    public async deleteFilter(filter: Models.FilterPreferences)
    {
        // Delete the filter
        await this.users.removePreference(FiltersService.path, filter.id);

        // Reload filters
        await this.refreshSavedFilters();
    }
}
