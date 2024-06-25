import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {EnumsService} from "app/services/domain/enums.service";
import {AlertExtended, EventsService} from "app/services/domain/events.service";

import * as Models from "app/services/proxy/model/models";
import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class AlertsService
{
    constructor(private api: ApiService,
                private enums: EnumsService,
                private events: EventsService)
    {
    }

    /**
     * Get the summary of alerts.
     */
    @ReportError
    async getSummary(filters: Models.AlertFilterRequest,
                     rollupType?: Models.LocationType,
                     groupBy?: Models.SummaryFlavor): Promise<Models.SummaryResult[]>
    {
        let response = await this.api.alerts.getSummary(groupBy, rollupType, filters);
        let result   = response.results;

        // Wait and sort the results, to make it nicer for the caller.
        result.sort((a,
                     b) => UtilsService.compareStrings(a.label, b.label, true));

        return result;
    }

    /**
     * Get the list of states.
     */
    describeStates(): Promise<Models.EnumDescriptor[]>
    {
        return this.enums.getInfos("AlertStatus", false);
    }

    @Memoizer
    public async getStates(): Promise<ControlOption<Models.AlertStatus>[]>
    {
        let types = await this.describeStates();

        return SharedSvc.BaseService.mapEnumOptions(types);
    }

    /**
     * Get the list of types.
     */
    describeTypes(): Promise<Models.EnumDescriptor[]>
    {
        return this.enums.getInfos("AlertType", true);
    }

    public async describeType(type: Models.AlertType): Promise<string>
    {
        let types = await this.getTypes();
        return types.find((entry) => { return entry.id === type; })?.label;
    }

    @Memoizer
    public async getTypes(): Promise<ControlOption<Models.AlertType>[]>
    {
        let types = await this.describeTypes();

        return SharedSvc.BaseService.mapEnumOptions(types);
    }

    /**
     * Get the list of severities.
     */
    describeSeverities(): Promise<Models.EnumDescriptor[]>
    {
        return this.enums.getInfos("AlertSeverity", false);
    }

    public async describeSeverity(severity: Models.AlertSeverity): Promise<string>
    {
        let severities = await this.getSeverities();
        return severities.find((entry) => { return entry.id === severity; })?.label;
    }

    @Memoizer
    public async getSeverities(): Promise<ControlOption<Models.AlertSeverity>[]>
    {
        let types = await this.describeSeverities();

        return SharedSvc.BaseService.mapEnumOptions(types);
    }

    //--//

    async getExtendedAll(filters: Models.AlertFilterRequest): Promise<AlertExtended[]>
    {
        return this.events.getTypedExtendedAll(AlertExtended, filters);
    }

    async getExtendedBatch(ids: Models.RecordIdentity[]): Promise<AlertExtended[]>
    {
        return this.events.getTypedExtendedBatch(AlertExtended, ids);
    }
}
