import {Injectable, Type} from "@angular/core";

import {ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";
import {DeviceExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {AlertExtended, WorkflowExtended} from "app/services/domain/events.service";
import {ReportDefinitionExtended} from "app/services/domain/report-definitions.service";
import {ReportExtended} from "app/services/domain/reports.service";

import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {DatatableManager} from "framework/ui/datatables/datatable-manager";
import {AppNavigationService} from "framework/ui/navigation/app-navigation.service";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class UserMessagesService extends SharedSvc.BaseService<Models.UserMessage, UserMessageExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.UserMessage, UserMessageExtended.newInstance);
    }

    /**
     * Return the count of records when the filters are applied.
     * @param filters
     */
    @ReportError
    async getCount(filters: Models.UserMessageFilterRequest): Promise<number>
    {
        return await this.api.userMessages.getFilteredCount(filters);
    }

    /**
     * Get the list of messages, applying optional filters.
     */
    @ReportError
    public getList(filters: Models.UserMessageFilterRequest): Promise<Models.RecordIdentity[]>
    {
        return this.api.userMessages.getFiltered(filters);
    }

    /**
     * True if any filters are actually applied.
     */
    static hasAppliedFilters(filters: Models.UserMessageFilterRequest): boolean
    {
        if (filters)
        {
            if (filters.flagActive !== undefined) return true;
            if (filters.flagNew !== undefined) return true;
            if (filters.flagRead !== undefined) return true;
        }

        return false;
    }

    //--//

    protected cachePrefix(): string { return Models.UserMessage.RECORD_IDENTITY; }

    protected getRaw(id: string): Promise<Models.UserMessage>
    {
        return this.api.userMessages.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.UserMessage[]>
    {
        return this.api.userMessages.getBatch(ids);
    }

    //--//

    wrapTypedModel<E extends UserMessageExtended, M extends Models.UserMessage>(type: Type<E>,
                                                                                model: M): E
    {
        let ext = super.wrapModel(model);
        return this.asType(type, ext);
    }

    async getTypedExtendedByIdentity<E extends UserMessageExtended>(type: Type<E>,
                                                                    id: Models.RecordIdentity): Promise<E>
    {
        let ext = await super.getExtendedByIdentity(id);
        return this.asType(type, ext);
    }

    async getTypedExtendedById<E extends UserMessageExtended>(type: Type<E>,
                                                              id: string): Promise<E>
    {
        let ext = await super.getExtendedById(id);
        return this.asType(type, ext);
    }

    async getTypedExtendedBatch<E extends UserMessageExtended>(type: Type<E>,
                                                               ids: Models.RecordIdentity[]): Promise<E[]>
    {
        let exts = await super.getExtendedBatch(ids);
        return this.typedFilter(type, exts, true);
    }

    async getTypedExtendedAll<F extends Models.UserMessageFilterRequest>(filters: F): Promise<UserMessageExtended[]>
    {
        let ids  = await this.getList(filters);
        return this.getExtendedBatch(ids);
    }

    async getTypedPage<E extends UserMessageExtended>(type: Type<E>,
                                                      list: Models.RecordIdentity[],
                                                      offset: number,
                                                      limit: number): Promise<E[]>
    {
        let exts = await super.getPage(list, offset, limit);
        return this.typedFilter(type, exts, false);
    }

    async getTypedPageFromTable<E extends UserMessageExtended>(type: Type<E>,
                                                               table: DatatableManager<Models.RecordIdentity, UserMessageExtended, any>,
                                                               offset: number,
                                                               limit: number): Promise<E[]>
    {
        let exts = await super.getPageFromTable(table, offset, limit);
        return this.typedFilter(type, exts, false);
    }

    //--//

    /**
     * Create or update the userMessage.
     */
    @ReportError
    async save<T extends Models.UserMessage>(model: T): Promise<T>
    {
        if (model.sysId)
        {
            this.flushModel(model);

            await this.api.userMessages.update(model.sysId, undefined, model);
            return <Promise<T>>this.get(model.sysId, model.updatedOn);
        }
        else
        {
            // return <Promise<T>> this.api.userMessages.create(model);
            return null;
        }
    }
}

export abstract class UserMessageExtended extends SharedSvc.ExtendedModel<Models.UserMessage>
{
    static newInstance(svc: UserMessagesService,
                       model: Models.UserMessage): UserMessageExtended
    {
        if (model instanceof Models.UserMessageAlert)
        {
            return UserMessageAlertExtended.newInstance(svc, model);
        }

        if (model instanceof Models.UserMessageDevice)
        {
            return UserMessageDeviceExtended.newInstance(svc, model);
        }

        if (model instanceof Models.UserMessageReport)
        {
            return UserMessageReportExtended.newInstance(svc, model);
        }

        if (model instanceof Models.UserMessageRoleManagement)
        {
            return UserMessageRoleManagementExtended.newInstance(svc, model);
        }

        if (model instanceof Models.UserMessageGeneric)
        {
            return UserMessageGenericExtended.newInstance(svc, model);
        }

        if (model instanceof Models.UserMessageWorkflow)
        {
            return UserMessageWorkflowExtended.newInstance(svc, model);
        }

        throw `Unknown type: ${model.__type}`;
    }

    get typedModel(): Models.UserMessage
    {
        return this.model;
    }

    @ReportError
    async remove(): Promise<Models.ValidationResults>
    {
        await this.flush();
        return await this.domain.apis.userMessages.remove(this.model.sysId);
    }

    @ReportError
    async save(): Promise<UserMessageExtended>
    {
        let validations = this.validate();

        if (!validations.length)
        {
            // save entity
            this.model = await this.domain.userMessages.save(this.model);

            return this;
        }
        else
        {
            this.domain.userMessages.errors.error("VALIDATION_ERRORS",
                                                  "UserMessage could not be saved.",
                                                  validations.map(
                                                      a => a.message));
            throw new Error("VALIDATION_ERRORS");
        }
    }

    abstract prepareForNavigation(): Promise<void>;

    abstract navigateTo(navigation: AppNavigationService): void;
}

export type UserMessageChangeSubscription = SharedSvc.DbChangeSubscription<Models.UserMessage>;

export class UserMessageGenericExtended extends UserMessageExtended
{
    static newInstance(svc: UserMessagesService,
                       model: Models.UserMessageGeneric): UserMessageGenericExtended
    {
        return new UserMessageGenericExtended(svc, model, Models.UserMessageGeneric.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.UserMessageGeneric.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    get typedModel(): Models.UserMessageGeneric
    {
        return <Models.UserMessageGeneric>this.model;
    }

    async save(): Promise<UserMessageGenericExtended>
    {
        return <UserMessageGenericExtended>await super.save();
    }

    async prepareForNavigation()
    {
    }

    navigateTo(navigation: AppNavigationService)
    {
    }
}

export class UserMessageAlertExtended extends UserMessageExtended
{
    static newInstance(svc: UserMessagesService,
                       model: Models.UserMessageAlert): UserMessageAlertExtended
    {
        return new UserMessageAlertExtended(svc, model, Models.UserMessageAlert.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.UserMessageAlert.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    get typedModel(): Models.UserMessageAlert
    {
        return <Models.UserMessageAlert>this.model;
    }

    @Memoizer
    public getAlert(): Promise<AlertExtended>
    {
        return this.domain.events.getTypedExtendedByIdentity(AlertExtended, this.typedModel.alert);
    }

    async save(): Promise<UserMessageAlertExtended>
    {
        return <UserMessageAlertExtended>await super.save();
    }

    async prepareForNavigation()
    {
    }

    navigateTo(navigation: AppNavigationService)
    {
        navigation.go("/alerts/alert", [this.typedModel.alert.sysId]);
    }
}

export class UserMessageWorkflowExtended extends UserMessageExtended
{
    static newInstance(svc: UserMessagesService,
                       model: Models.UserMessageWorkflow): UserMessageWorkflowExtended
    {
        return new UserMessageWorkflowExtended(svc, model, Models.UserMessageWorkflow.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.UserMessageWorkflow.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    get typedModel(): Models.UserMessageWorkflow
    {
        return <Models.UserMessageWorkflow>this.model;
    }

    @Memoizer
    public getWorfklow(): Promise<WorkflowExtended>
    {
        return this.domain.events.getTypedExtendedByIdentity(WorkflowExtended, this.typedModel.workflow);
    }

    async save(): Promise<UserMessageWorkflowExtended>
    {
        return <UserMessageWorkflowExtended>await super.save();
    }

    async prepareForNavigation()
    {
    }

    navigateTo(navigation: AppNavigationService)
    {
        navigation.go("/workflows/workflow", [this.typedModel.workflow.sysId]);
    }
}

export class UserMessageDeviceExtended extends UserMessageExtended
{
    static newInstance(svc: UserMessagesService,
                       model: Models.UserMessageDevice): UserMessageDeviceExtended
    {
        return new UserMessageDeviceExtended(svc, model, Models.UserMessageDevice.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.UserMessageDevice.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    get typedModel(): Models.UserMessageDevice
    {
        return <Models.UserMessageDevice>this.model;
    }

    @Memoizer
    public getDevice(): Promise<DeviceExtended>
    {
        return this.domain.assets.getTypedExtendedByIdentity(DeviceExtended, this.typedModel.device);
    }

    async save(): Promise<UserMessageDeviceExtended>
    {
        return <UserMessageDeviceExtended>await super.save();
    }

    async prepareForNavigation()
    {
    }

    navigateTo(navigation: AppNavigationService)
    {
        navigation.go("/devices/device", [this.typedModel.device.sysId]);
    }
}

export class UserMessageReportExtended extends UserMessageExtended
{
    static newInstance(svc: UserMessagesService,
                       model: Models.UserMessageReport): UserMessageReportExtended
    {
        return new UserMessageReportExtended(svc, model, Models.UserMessageReport.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.UserMessageReport.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    private m_report: ReportExtended;
    private m_reportDefinition: ReportDefinitionExtended;

    get typedModel(): Models.UserMessageReport
    {
        return <Models.UserMessageReport>this.model;
    }

    @Memoizer
    public getReport(): Promise<ReportExtended>
    {
        return this.domain.reports.getExtendedByIdentity(this.typedModel.report);
    }

    async save(): Promise<UserMessageReportExtended>
    {
        return <UserMessageReportExtended>await super.save();
    }

    async prepareForNavigation()
    {
        this.m_report           = await this.getReport();
        this.m_reportDefinition = await this.m_report.getDefinition();
    }

    navigateTo(navigation: AppNavigationService)
    {
        let model = this.m_reportDefinition?.model;
        if (!model) return;

        if (this.m_report?.isFinished)
        {
            let url = this.m_report.getDownloadUrl(model.title);
            window.open(url, "_blank");
        }
        else
        {
            navigation.go("/configuration/reports/report", [model.sysId]);
        }
    }
}

export class UserMessageRoleManagementExtended extends UserMessageExtended
{
    static newInstance(svc: UserMessagesService,
                       model: Models.UserMessageRoleManagement): UserMessageRoleManagementExtended
    {
        return new UserMessageRoleManagementExtended(svc, model, Models.UserMessageRoleManagement.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.UserMessageRoleManagement.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    get typedModel(): Models.UserMessageRoleManagement
    {
        return <Models.UserMessageRoleManagement>this.model;
    }

    async save(): Promise<UserMessageRoleManagementExtended>
    {
        return <UserMessageRoleManagementExtended>await super.save();
    }

    async prepareForNavigation()
    {
    }

    navigateTo(navigation: AppNavigationService)
    {
        navigation.go("/configuration/users/user", [this.typedModel.userTarget.sysId]);
    }
}


