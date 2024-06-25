import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {AlertDefinitionDetailsExtended, AlertDefinitionExtended} from "app/services/domain/alert-definitions.service";
import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";
import {ControlBindingService} from "app/services/ui/control-binding.service";
import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {Memoizer, ResetMemoizers} from "framework/utils/memoizers";

@Injectable()
export class AlertDefinitionVersionsService extends SharedSvc.BaseService<Models.AlertDefinitionVersion, AlertDefinitionVersionExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.AlertDefinitionVersion, AlertDefinitionVersionExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string { return Models.AlertDefinitionVersion.RECORD_IDENTITY; }

    protected getRaw(id: string): Promise<Models.AlertDefinitionVersion>
    {
        return this.api.alertDefinitionVersions.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.AlertDefinitionVersion[]>
    {
        return this.api.alertDefinitionVersions.getBatch(ids);
    }

    /**
     * Create or update the alert definition.
     */
    @ReportError
    async save(model: Models.AlertDefinitionVersion): Promise<AlertDefinitionVersionExtended>
    {
        let newModel = await this.api.alertDefinitionVersions.create(model);
        let verExt   = this.wrapModel(newModel);

        // Flush the definition, since its head has been changed by the backend.
        let defExt = await verExt.getDefinition();
        await this.domain.alertDefinitions.flush(defExt);

        return verExt;
    }

    evaluate(model: Models.AlertDefinitionVersion,
             maxSteps?: number,
             when?: Date,
             trace?: boolean): Promise<Models.AlertEngineExecutionSteps>
    {
        return this.api.alertDefinitionVersions.evaluate(model.sysId, maxSteps, when, trace);
    }

    evaluateOverRange(model: Models.AlertDefinitionVersion,
                      rangeStart: Date,
                      rangeEnd: Date,
                      maxSteps?: number,
                      interval?: number): Promise<string>
    {
        return this.api.alertDefinitionVersions.evaluateOverRange(model.sysId, maxSteps, interval, rangeStart, rangeEnd);
    }

    checkEvaluation(progressId: string,
                    detailed?: boolean): Promise<Models.AlertTestProgress>
    {
        return this.api.alertDefinitionVersions.checkEvaluation(progressId, detailed);
    }

    async makeHead(id: string): Promise<AlertDefinitionVersionExtended>
    {
        let model = await this.api.alertDefinitionVersions.makeHead(id);
        return this.wrapModel(model);
    }

    async makeRelease(id: string): Promise<AlertDefinitionVersionExtended>
    {
        let model = await this.api.alertDefinitionVersions.makeRelease(id);
        return this.wrapModel(model);
    }

    async link(predId: string,
               succId: string): Promise<AlertDefinitionVersionExtended>
    {
        let model = await this.api.alertDefinitionVersions.link(predId, succId);
        return this.wrapModel(model);
    }

    async squash(baseId: string,
                 finalId: string): Promise<AlertDefinitionVersionExtended>
    {
        let model = await this.api.alertDefinitionVersions.squash(baseId, finalId);
        return this.wrapModel(model);
    }
}

export class AlertDefinitionVersionExtended extends SharedSvc.ExtendedModel<Models.AlertDefinitionVersion>
{
    static newInstance(svc: AlertDefinitionVersionsService,
                       model: Models.AlertDefinitionVersion): AlertDefinitionVersionExtended
    {
        return new AlertDefinitionVersionExtended(svc, model, Models.AlertDefinitionVersion.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.AlertDefinitionVersion.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    async save(): Promise<AlertDefinitionVersionExtended>
    {
        let validations = this.validate();

        if (!validations.length)
        {
            // save entity
            return this.domain.alertDefinitionVersions.save(this.model);
        }
        else
        {
            this.domain.events.errors.error("VALIDATION_ERRORS", "Alert Definition version could not be saved.", validations.map((a) => a.message));
            throw new Error("VALIDATION_ERRORS");
        }
    }

    evaluate(maxSteps?: number,
             when?: Date,
             trace?: boolean): Promise<Models.AlertEngineExecutionSteps>
    {
        return this.domain.alertDefinitionVersions.evaluate(this.model, maxSteps, when, trace);
    }

    evaluateOverRange(rangeStart: Date,
                      rangeEnd: Date,
                      maxSteps?: number,
                      interval?: number): Promise<string>
    {
        return this.domain.alertDefinitionVersions.evaluateOverRange(this.model, rangeStart, rangeEnd, maxSteps, interval);
    }

    checkEvaluation(progressId: string,
                    detailed?: boolean): Promise<Models.AlertTestProgress>
    {
        return this.domain.alertDefinitionVersions.checkEvaluation(progressId, detailed);
    }


    squash(base: AlertDefinitionVersionExtended)
    {
        return this.domain.alertDefinitionVersions.squash(base.model.sysId, this.model.sysId);
    }

    /**
     * Sets the head to this version. Used for tracking the current edit position.
     * @returns {Promise<AlertDefinitionVersionExtended>}
     */
    async makeHead(): Promise<AlertDefinitionVersionExtended>
    {
        return await this.domain.alertDefinitionVersions.makeHead(this.model.sysId);
    }

    /**
     * Sets the version to the release version. Scheduled reports will use this version.
     * @returns {Promise<AlertDefinitionVersionExtended>}
     */
    async makeRelease(): Promise<AlertDefinitionVersionExtended>
    {
        return await this.domain.alertDefinitionVersions.makeRelease(this.model.sysId);
    }

    /**
     * Will set the head to the predecessor of the current version and link the two. Returns the new head.
     * @returns {Promise<AlertDefinitionVersionExtended>}
     */
    async undo(): Promise<AlertDefinitionVersionExtended>
    {
        let predecessor = await this.getPredecessor();
        if (predecessor)
        {
            predecessor = await predecessor.makeHead();
        }

        return predecessor;
    }

    /**
     * Will set the head to the successor of the current version. Returns the new head.
     *
     * @returns {Promise<AlertDefinitionVersionExtended>}
     */
    async redo(): Promise<AlertDefinitionVersionExtended>
    {
        for (let successor of await this.getSuccessors())
        {
            return successor.makeHead();
        }

        return null;
    }

    /**
     * Link two versions together.
     * @param {AlertDefinitionVersionExtended} succ
     * @returns {Promise<AlertDefinitionVersionExtended>}
     */
    link(succ: AlertDefinitionVersionExtended): Promise<AlertDefinitionVersionExtended>
    {
        return this.domain.alertDefinitionVersions.link(this.model.sysId, succ.model.sysId);
    }

    /**
     * Get a new version from the current version.
     * @returns {AlertDefinitionVersionExtended}
     */
    getNewVersion(): AlertDefinitionVersionExtended
    {
        let newModel = Models.AlertDefinitionVersion.newInstance({
                                                                     ...this.model,
                                                                     predecessor: this.getIdentity(),
                                                                     sysId      : null
                                                                 });

        return this.domain.alertDefinitionVersions.wrapModel(newModel);
    }

    getDetails<T extends Models.AlertDefinitionDetails>()
    {
        return <T>this.model.details;
    }

    async getDetailsExtended<T extends AlertDefinitionDetailsExtended>(): Promise<T>
    {
        let bindings = this.injectService(ControlBindingService);

        return <T>(AlertDefinitionDetailsExtended.newInstance({
                                                                  tags            : await bindings.getTags(),
                                                                  pointClasses    : await bindings.getPointClasses(false, null),
                                                                  equipmentClasses: await bindings.getEquipmentClasses(false, null)
                                                              }, this.injectService(AppDomainContext), this.model.details));
    }

    /**
     * Get the predecessor of this version
     * @returns {Promise<AlertDefinitionVersionExtended>}
     */
    async getPredecessor(): Promise<AlertDefinitionVersionExtended>
    {
        return this.domain.alertDefinitionVersions.getExtendedByIdentity(this.model.predecessor);
    }

    /**
     * Get the successor of this version if any
     * @returns {Promise<AlertDefinitionVersionExtended>}
     */
    async getSuccessors(): Promise<AlertDefinitionVersionExtended[]>
    {
        return this.domain.alertDefinitionVersions.getExtendedBatch(this.model.successors);
    }

    @Memoizer
    async getDefinition(): Promise<AlertDefinitionExtended>
    {
        return await this.domain.alertDefinitions.getExtendedByIdentity(this.model.definition);
    }

    @ResetMemoizers
    setDefinition(alertDefinition: AlertDefinitionExtended): void
    {
        this.model.definition = alertDefinition ? alertDefinition.getIdentity() : null;
    }
}
