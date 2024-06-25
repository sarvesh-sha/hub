import {Injectable, Type} from "@angular/core";

import {AppContext, ReportError} from "app/app.service";
import {AlertDefinitionVersionExtended} from "app/services/domain/alert-definition-versions.service";

import {AlertHistoryExtended} from "app/services/domain/alert-history.service";
import {ApiService} from "app/services/domain/api.service";
import {AssetExtended, DeviceExtended, LocationExtended} from "app/services/domain/assets.service";

import * as SharedSvc from "app/services/domain/base.service";
import {WorkflowHistoryExtended} from "app/services/domain/workflow-history.service";
import * as Models from "app/services/proxy/model/models";

import {MarkerWithChartPointSource} from "app/shared/charting/markers";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {ChartPointStyle} from "framework/ui/charting/core/basics";
import {ChartColorUtilities} from "framework/ui/charting/core/colors";
import {ChartPointSource} from "framework/ui/charting/core/data-sources";
import {DatatableManager} from "framework/ui/datatables/datatable-manager";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {Memoizer} from "framework/utils/memoizers";
import moment from "framework/utils/moment";

@Injectable()
export class EventsService extends SharedSvc.BaseService<Models.Event, EventExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.Event, EventExtended.newInstance);
    }

    /**
     * Get the list of events.
     */
    @ReportError
    public getList(filters?: Models.EventFilterRequest): Promise<Models.PaginatedRecordIdentityList>
    {
        return this.api.events.getFiltered(filters);
    }

    /**
     * True if any filters are actually applied.
     */
    hasAppliedFilters(filters: Models.EventFilterRequest): boolean
    {
        if (filters)
        {
            if (filters.rangeEnd) return true;
            if (filters.rangeStart) return true;
            if (filters.likeDeviceManufacturerName) return true;
            if (filters.likeDeviceProductName) return true;
            if (filters.likeDeviceModelName) return true;
            if (filters.locationIDs && filters.locationIDs.length) return true;

            if (filters instanceof Models.AlertFilterRequest)
            {
                if (filters.alertSeverityIDs && filters.alertSeverityIDs.length) return true;
                if (filters.alertTypeIDs && filters.alertTypeIDs.length) return true;
            }

            if (filters instanceof Models.AuditFilterRequest)
            {
                if (filters.auditTypeIDs && filters.auditTypeIDs.length) return true;
            }
        }

        return false;
    }

    /**
     * Return the count of records when the filters are applied.
     * @param filters
     */
    @ReportError
    async getCount(filters: Models.EventFilterRequest): Promise<number>
    {
        let list = await this.api.events.getFiltered(filters);

        return list?.results?.length;
    }

    //--//

    protected cachePrefix(): string { return Models.Event.RECORD_IDENTITY; }

    protected getRaw(id: string): Promise<Models.Event>
    {
        return this.api.events.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.Event[]>
    {
        return this.api.events.getBatch(ids);
    }

    //--//

    wrapTypedModel<E extends EventExtended, M extends Models.Event>(type: Type<E>,
                                                                    model: M): E
    {
        let ext = super.wrapModel(model);
        return this.asType(type, ext);
    }

    async getTypedExtendedByIdentity<E extends EventExtended>(type: Type<E>,
                                                              id: Models.RecordIdentity): Promise<E>
    {
        let ext = await super.getExtendedByIdentity(id);
        return this.asType(type, ext);
    }

    async getTypedExtendedById<E extends EventExtended>(type: Type<E>,
                                                        id: string): Promise<E>
    {
        let ext = await super.getExtendedById(id);
        return this.asType(type, ext);
    }

    async getTypedExtendedBatch<E extends EventExtended>(type: Type<E>,
                                                         ids: Models.RecordIdentity[]): Promise<E[]>
    {
        let exts = await super.getExtendedBatch(ids);
        return this.typedFilter(type, exts, true);
    }

    async getTypedExtendedAll<E extends EventExtended, F extends Models.EventFilterRequest>(type: Type<E>,
                                                                                            filters: F): Promise<E[]>
    {
        let ids  = await this.getList(filters);
        let exts = await this.getExtendedBatch(ids.results);
        return this.typedFilter(type, exts, true);
    }

    async getTypedPage<E extends EventExtended>(type: Type<E>,
                                                list: Models.RecordIdentity[],
                                                offset: number,
                                                limit: number): Promise<E[]>
    {
        let exts = await super.getPage(list, offset, limit);
        return this.typedFilter(type, exts, false);
    }

    async getTypedPageFromTable<E extends EventExtended>(type: Type<E>,
                                                         table: DatatableManager<Models.RecordIdentity, EventExtended, any>,
                                                         offset: number,
                                                         limit: number): Promise<E[]>
    {
        let exts = await super.getPageFromTable(table, offset, limit);
        return this.typedFilter(type, exts, false);
    }

    //--//

    /**
     * Create or update the asset.
     */
    @ReportError
    async save(model: Models.Event): Promise<Models.Event>
    {
        this.flushModel(model);

        await this.api.events.update(model.sysId, undefined, model);
        return this.get(model.sysId, model.updatedOn);
    }
}

export class EventExtended extends SharedSvc.ExtendedModel<Models.Event>
{
    static newInstance(svc: EventsService,
                       model: Models.Event): EventExtended
    {
        if (model instanceof Models.Alert)
        {
            return AlertExtended.newInstance(svc, model);
        }

        if (model instanceof Models.Audit)
        {
            return AuditExtended.newInstance(svc, model);
        }

        if (model instanceof Models.Workflow)
        {
            return WorkflowExtended.newInstance(svc, model);
        }

        return new EventExtended(svc, model, Models.Event.RECORD_IDENTITY);
    }

    public static newIdentityRaw(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.Event.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    async getAll()
    {
        let asset = await this.getDevice();
        if (asset)
        {
            await asset.getAll();
        }

        let location = await this.getLocation();
        if (location)
        {
            await location.getRecursiveName();
        }
    }

    //--//

    @Memoizer
    public async getAsset(): Promise<AssetExtended>
    {
        if (!this.model.asset) return null;

        return await this.domain.assets.getTypedExtendedById(AssetExtended, this.model.asset.sysId);
    }

    @Memoizer
    public async getDevice(): Promise<DeviceExtended>
    {
        let asset = await this.getAsset();
        return asset instanceof DeviceExtended ? asset : null;
    }

    @Memoizer
    public async getLocation(): Promise<LocationExtended>
    {
        let asset = await this.getAsset();
        if (!asset) return null;

        return await asset.getLocation();
    }

    async save(): Promise<EventExtended>
    {
        let validations = this.validate();

        if (!validations.length)
        {
            // save entity
            this.model = await this.domain.events.save(this.model);

            return this;
        }
        else
        {
            this.domain.events.errors.error("VALIDATION_ERRORS",
                                            "Event could not be saved.",
                                            validations.map(
                                                a => a.message));
            throw new Error("VALIDATION_ERRORS");
        }
    }

    public async getDefinitionTitle(): Promise<string>
    {
        return "";
    }
}

export class EventMarker extends MarkerWithChartPointSource<Models.Event>
{
    get navigable(): boolean
    {
        return false;
    }

    constructor(event: Models.Event,
                parent: ChartPointSource<any>)
    {
        super(event, parent);
    }

    get x(): number
    {
        return this.source.createdOn.valueOf();
    }

    get y(): number
    {
        let time   = this.sourceTime;
        let bounds = this.parent.findBoundingPoints(time);

        if (bounds) return bounds.interpolate(time);

        // Return null if value could not be interpolated
        return null;
    }

    get label(): string
    {
        return this.source.description;
    }

    get tooltip(): string
    {
        return ChartPointSource.generateTooltipEntry("Timestamp", this.sourceTime.toLocaleString(), false);
    }

    get shape(): ChartPointStyle
    {
        return ChartPointStyle.rectRot;
    }

    get size(): number
    {
        return 10;
    }

    private get sourceTime(): moment.Moment
    {
        return MomentHelper.parse(this.source.createdOn);
    }

    public navigate(): void
    {
    }
}

//--//

export class AlertExtended extends EventExtended
{
    static newInstance(svc: EventsService,
                       model: Models.Alert): AlertExtended
    {
        return new AlertExtended(svc, model, Models.Alert.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.Alert.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    get typedModel(): Models.Alert
    {
        return <Models.Alert>this.model;
    }

    //--//

    public async extractDetails(): Promise<AlertDetails>
    {
        let details        = new AlertDetails();
        details.alertSysId = this.model.sysId;

        let asset = await this.getAsset();
        if (asset)
        {
            details.assetSysId = asset.model.sysId;

            let location = await asset.getLocation();
            if (location)
            {
                details.assetLocation = await location.getRecursiveName();
            }
        }

        let alert             = this.typedModel;
        details.description   = alert.description;
        details.status        = alert.status;
        details.type          = alert.type;
        details.processedType = await this.getDisplayType();
        details.severity      = alert.severity;

        return details;
    }

    @Memoizer
    public async getAlertDefinitionVersion(): Promise<AlertDefinitionVersionExtended>
    {
        return await this.domain.alertDefinitionVersions.getExtendedByIdentity(this.typedModel.alertDefinitionVersion);
    }

    @Memoizer
    public async getHistory(): Promise<AlertHistoryExtended[]>
    {
        return this.filterHistory();
    }

    public async filterHistory(rangeStart?: Date,
                               rangeEnd?: Date): Promise<AlertHistoryExtended[]>
    {
        let historyIds = await this.domain.apis.alerts.listAlertHistoryByID(this.model.sysId, rangeStart, rangeEnd);
        return await this.domain.alertsHistory.getExtendedBatch(historyIds);
    }

    //--//

    @Memoizer
    async getDisplayType(): Promise<string>
    {
        let typeLabel = await this.domain.alerts.describeType(this.typedModel.type);

        let alertVer = await this.getAlertDefinitionVersion();
        if (alertVer)
        {
            let alertDef = await alertVer.getDefinition();
            if (alertDef?.model?.title)
            {
                return `${typeLabel} - ${alertDef.model.title}`;
            }
        }

        return typeLabel;
    }

    @Memoizer
    public async getDefinitionTitle(): Promise<string>
    {
        let alert = await this.getAlertDefinitionVersion();
        if (alert)
        {
            let rule = await alert.getDefinition();
            return rule?.model.title;
        }
        return "";
    }
}

export class AlertDetails
{
    assetSysId: string;
    alertSysId: string;
    historySysId: string;

    title: string;
    description: string;
    processedType: string;

    status: Models.AlertStatus;
    type: Models.AlertType;
    severity: Models.AlertSeverity;

    eventTimestamp: moment.Moment;
    eventLevel: Models.AlertEventLevel;
    eventType: Models.AlertEventType;
    eventText: string;

    assetLocation: string;
}

export class AlertMarker extends MarkerWithChartPointSource<AlertDetails>
{
    public sizeStrategy: AlertSizeStrategy   = null;
    public colorStrategy: AlertColorStrategy = null;
    public shapeStrategy: AlertShapeStrategy = null;

    constructor(private app: AppContext,
                source: AlertDetails,
                parent: ChartPointSource<any>,
                private readonly synthetic: boolean = false)
    {
        super(source, parent);
    }

    get x(): number
    {
        return this.source.eventTimestamp.valueOf();
    }

    get y(): number
    {
        let time   = this.source.eventTimestamp;
        let bounds = this.parent.findBoundingPoints(time);

        if (bounds) return bounds.interpolate(time);

        // Return null if value could not be interpolated
        return null;
    }

    get label(): string
    {
        return this.source.description;
    }

    get tooltip(): string
    {
        return ChartPointSource.generateTooltipEntry("Timestamp", this.source.eventTimestamp?.toLocaleString()) +
               ChartPointSource.generateTooltipEntry("Location", this.source.assetLocation) +
               ChartPointSource.generateTooltipEntry("Type", this.source.processedType) +
               ChartPointSource.generateTooltipEntry("Severity", this.source.severity?.toString()) +
               ChartPointSource.generateTooltipEntry("Status", this.source.status?.toString()) +
               ChartPointSource.generateTooltipEntry("Event", this.source.eventType?.toString(), false);
    }

    get size(): number
    {
        if (this.sizeStrategy === AlertSizeStrategy.ALERT_REPORT)
        {
            return 12;
        }

        return 10;
    }

    get shape(): ChartPointStyle
    {
        if (this.shapeStrategy === AlertShapeStrategy.TYPE)
        {
            switch (this.source.type)
            {
                case Models.AlertType.THRESHOLD_EXCEEDED:
                    return ChartPointStyle.rectRot;

                case Models.AlertType.WARNING:
                    return ChartPointStyle.triangle;

                case Models.AlertType.INFORMATIONAL:
                    return ChartPointStyle.circle;

                default:
                    return ChartPointStyle.crossRot;
            }
        }
        else if (this.shapeStrategy === AlertShapeStrategy.ALERT_REPORT)
        {
            switch (this.source.eventType)
            {
                case Models.AlertEventType.created:
                case Models.AlertEventType.updatedWithNotes:
                case Models.AlertEventType.reassigned:
                case Models.AlertEventType.unmuted:
                case Models.AlertEventType.reopened:
                case Models.AlertEventType.muted:
                    return ChartPointStyle.pinExclamation;

                case Models.AlertEventType.resolved:
                case Models.AlertEventType.closed:
                    return ChartPointStyle.pinCheck;

                default:
                    return ChartPointStyle.rect;
            }
        }

        return ChartPointStyle.rectRot;
    }

    protected computeColor(): string
    {
        if (this.colorStrategy === AlertColorStrategy.SEVERITY)
        {
            switch (this.source.eventType)
            {
                case Models.AlertEventType.created:
                case Models.AlertEventType.updatedWithNotes:
                case Models.AlertEventType.reassigned:
                case Models.AlertEventType.unmuted:
                case Models.AlertEventType.reopened:
                    break;

                case Models.AlertEventType.muted:
                    return "#888888";

                case Models.AlertEventType.resolved:
                    return ChartColorUtilities.getDefaultColorById("green").hex;

                case Models.AlertEventType.closed:
                    return "#444444";
            }

            switch (this.source.severity)
            {
                case Models.AlertSeverity.CRITICAL:
                    return ChartColorUtilities.getDefaultColorById("red").hex;

                case Models.AlertSeverity.SIGNIFICANT:
                    return ChartColorUtilities.getDefaultColorById("orange").hex;

                case Models.AlertSeverity.NORMAL:
                    return ChartColorUtilities.getDefaultColorById("yellow").hex;

                case Models.AlertSeverity.LOW:
                    return ChartColorUtilities.getDefaultColorById("green").hex;

                default:
                    return "#222222";
            }
        }
        else if (this.colorStrategy === AlertColorStrategy.ALERT_REPORT)
        {
            switch (this.source.eventType)
            {
                case Models.AlertEventType.created:
                case Models.AlertEventType.updatedWithNotes:
                case Models.AlertEventType.reassigned:
                case Models.AlertEventType.unmuted:
                case Models.AlertEventType.muted:
                case Models.AlertEventType.reopened:
                    return ChartColorUtilities.getDefaultColorById("red").hex;

                case Models.AlertEventType.resolved:
                    return ChartColorUtilities.getDefaultColorById("lavender").hex;

                case Models.AlertEventType.closed:
                    return ChartColorUtilities.getDefaultColorById("green").hex;
            }

            return "#444444";
        }

        return null;
    }

    get navigable(): boolean
    {
        return !this.synthetic;
    }

    public navigate(): void
    {
        if (this.navigable) this.app.ui.navigation.go("/alerts/alert", [this.source.alertSysId]);
    }
}

export enum AlertShapeStrategy
{
    TYPE,
    ALERT_REPORT
}

export enum AlertColorStrategy
{
    SEVERITY,
    ALERT_REPORT
}

export enum AlertSizeStrategy
{
    ALERT_REPORT
}

//--//

export class AuditExtended extends EventExtended
{
    static newInstance(svc: EventsService,
                       model: Models.Audit): AuditExtended
    {
        return new AuditExtended(svc, model, Models.Audit.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.Audit.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    get typedModel(): Models.Audit
    {
        return <Models.Audit>this.model;
    }
}

//--//

export class WorkflowDetails
{
    assetSysId: string;
    alertSysId: string;
    historySysId: string;

    title: string;
    description: string;
    status: Models.WorkflowStatus;
    type: Models.WorkflowType;
    priority: Models.WorkflowPriority;

    eventTimestamp: moment.Moment;
    eventType: Models.WorkflowEventType;
    eventText: string;
}

export class WorkflowExtended extends EventExtended
{
    static newInstance(svc: EventsService,
                       model: Models.Workflow): WorkflowExtended
    {
        return new WorkflowExtended(svc, model, Models.Workflow.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.Workflow.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    static create(svc: EventsService,
                  type: Models.WorkflowType,
                  details: Models.WorkflowDetails,
                  description?: string)
    {
        let model = Models.Workflow.newInstance({
                                                    type       : type,
                                                    details    : details,
                                                    status     : Models.WorkflowStatus.Active,
                                                    priority   : Models.WorkflowPriority.Normal,
                                                    description: description
                                                });

        return WorkflowExtended.newInstance(svc, model);
    }

    get typedModel(): Models.Workflow
    {
        return <Models.Workflow>this.model;
    }

    //--//

    public async assignToUser(id: string): Promise<void>
    {
        await this.domain.apis.workflows.assign(this.typedModel.sysId, id);
    }

    //--//

    public async extractDetails(): Promise<WorkflowDetails>
    {
        let details        = new WorkflowDetails();
        details.alertSysId = this.model.sysId;

        let asset = await this.getAsset();
        if (asset)
        {
            details.assetSysId = asset.model.sysId;
        }

        details.description = this.typedModel.description;
        details.status      = this.typedModel.status;
        details.type        = this.typedModel.type;
        details.priority    = this.typedModel.priority;

        return details;
    }

    @Memoizer
    public async getHistory(): Promise<WorkflowHistoryExtended[]>
    {
        return this.filterHistory();
    }

    public async filterHistory(rangeStart?: Date,
                               rangeEnd?: Date): Promise<WorkflowHistoryExtended[]>
    {
        let historyIds = await this.domain.apis.workflows.listWorkflowHistoryByID(this.model.sysId, rangeStart, rangeEnd);
        return await this.domain.workflowsHistory.getExtendedBatch(historyIds);
    }

    public getDetails<T extends Models.WorkflowDetails>(type: Type<T>): T
    {
        if (this.typedModel.details instanceof type)
        {
            return <T>this.typedModel.details;
        }

        return null;
    }
}
