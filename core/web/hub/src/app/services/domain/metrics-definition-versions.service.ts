import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";

import {MetricsDefinitionDetailsExtended, MetricsDefinitionExtended} from "app/services/domain/metrics-definitions.service";
import * as Models from "app/services/proxy/model/models";
import {ControlBindingService} from "app/services/ui/control-binding.service";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {Memoizer, ResetMemoizers} from "framework/utils/memoizers";

@Injectable()
export class MetricsDefinitionVersionsService extends SharedSvc.BaseService<Models.MetricsDefinitionVersion, MetricsDefinitionVersionExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.MetricsDefinitionVersion, MetricsDefinitionVersionExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string { return Models.MetricsDefinitionVersion.RECORD_IDENTITY; }

    protected getRaw(id: string): Promise<Models.MetricsDefinitionVersion>
    {
        return this.api.metricsDefinitionVersions.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.MetricsDefinitionVersion[]>
    {
        return this.api.metricsDefinitionVersions.getBatch(ids);
    }

    /**
     * Create or update the metrics definition.
     */
    @ReportError
    async save(model: Models.MetricsDefinitionVersion): Promise<MetricsDefinitionVersionExtended>
    {
        let newModel = await this.api.metricsDefinitionVersions.create(model);
        let verExt   = this.wrapModel(newModel);

        // Flush the definition, since its head has been changed by the backend.
        let defExt = await verExt.getDefinition();
        await this.domain.metricsDefinitions.flush(defExt);

        return verExt;
    }

    async makeHead(id: string): Promise<MetricsDefinitionVersionExtended>
    {
        let model = await this.api.metricsDefinitionVersions.makeHead(id);
        return this.wrapModel(model);
    }

    async makeRelease(id: string): Promise<MetricsDefinitionVersionExtended>
    {
        let model = await this.api.metricsDefinitionVersions.makeRelease(id);
        return this.wrapModel(model);
    }

    async link(predId: string,
               succId: string): Promise<MetricsDefinitionVersionExtended>
    {
        let model = await this.api.metricsDefinitionVersions.link(predId, succId);
        return this.wrapModel(model);
    }

    async squash(baseId: string,
                 finalId: string): Promise<MetricsDefinitionVersionExtended>
    {
        let model = await this.api.metricsDefinitionVersions.squash(baseId, finalId);
        return this.wrapModel(model);
    }
}

export class MetricsDefinitionVersionExtended extends SharedSvc.ExtendedModel<Models.MetricsDefinitionVersion>
{
    static newInstance(svc: MetricsDefinitionVersionsService,
                       model: Models.MetricsDefinitionVersion): MetricsDefinitionVersionExtended
    {
        return new MetricsDefinitionVersionExtended(svc, model, Models.MetricsDefinitionVersion.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.MetricsDefinitionVersion.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    async save(): Promise<MetricsDefinitionVersionExtended>
    {
        let validations = this.validate();

        if (!validations.length)
        {
            // save entity
            return this.domain.metricsDefinitionVersions.save(this.model);
        }
        else
        {
            this.domain.events.errors.error("VALIDATION_ERRORS", "Metrics Definition version could not be saved.", validations.map((a) => a.message));
            throw new Error("VALIDATION_ERRORS");
        }
    }

    squash(base: MetricsDefinitionVersionExtended)
    {
        return this.domain.metricsDefinitionVersions.squash(base.model.sysId, this.model.sysId);
    }

    /**
     * Sets the head to this version. Used for tracking the current edit position.
     * @returns {Promise<MetricsDefinitionVersionExtended>}
     */
    async makeHead(): Promise<MetricsDefinitionVersionExtended>
    {
        return await this.domain.metricsDefinitionVersions.makeHead(this.model.sysId);
    }

    /**
     * Sets the version to the release version. Scheduled reports will use this version.
     * @returns {Promise<MetricsDefinitionVersionExtended>}
     */
    async makeRelease(): Promise<MetricsDefinitionVersionExtended>
    {
        return await this.domain.metricsDefinitionVersions.makeRelease(this.model.sysId);
    }

    /**
     * Will set the head to the predecessor of the current version and link the two. Returns the new head.
     * @returns {Promise<MetricsDefinitionVersionExtended>}
     */
    async undo(): Promise<MetricsDefinitionVersionExtended>
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
     * @returns {Promise<MetricsDefinitionVersionExtended>}
     */
    async redo(): Promise<MetricsDefinitionVersionExtended>
    {
        for (let successor of await this.getSuccessors())
        {
            return successor.makeHead();
        }

        return null;
    }

    /**
     * Link two versions together.
     * @param {MetricsDefinitionVersionExtended} succ
     * @returns {Promise<MetricsDefinitionVersionExtended>}
     */
    link(succ: MetricsDefinitionVersionExtended): Promise<MetricsDefinitionVersionExtended>
    {
        return this.domain.metricsDefinitionVersions.link(this.model.sysId, succ.model.sysId);
    }

    /**
     * Get a new version from the current version.
     * @returns {MetricsDefinitionVersionExtended}
     */
    getNewVersion(): MetricsDefinitionVersionExtended
    {
        let newModel = Models.MetricsDefinitionVersion.newInstance({
                                                                       ...this.model,
                                                                       predecessor: this.getIdentity(),
                                                                       sysId      : null
                                                                   });

        return this.domain.metricsDefinitionVersions.wrapModel(newModel);
    }

    getDetails<T extends Models.MetricsDefinitionDetails>()
    {
        return <T>this.model.details;
    }

    async getDetailsExtended<T extends MetricsDefinitionDetailsExtended>(): Promise<T>
    {
        let bindings = this.injectService(ControlBindingService);

        return <T>(MetricsDefinitionDetailsExtended.newInstance({
                                                                    tags            : await bindings.getTags(),
                                                                    pointClasses    : await bindings.getPointClasses(false, null),
                                                                    equipmentClasses: await bindings.getEquipmentClasses(false, null)
                                                                }, this.domain, this.model.details));
    }

    /**
     * Get the predecessor of this version
     * @returns {Promise<MetricsDefinitionVersionExtended>}
     */
    async getPredecessor(): Promise<MetricsDefinitionVersionExtended>
    {
        return this.domain.metricsDefinitionVersions.getExtendedByIdentity(this.model.predecessor);
    }

    /**
     * Get the successor of this version if any
     * @returns {Promise<MetricsDefinitionVersionExtended>}
     */
    async getSuccessors(): Promise<MetricsDefinitionVersionExtended[]>
    {
        return this.domain.metricsDefinitionVersions.getExtendedBatch(this.model.successors);
    }

    @Memoizer
    async getDefinition(): Promise<MetricsDefinitionExtended>
    {
        return await this.domain.metricsDefinitions.getExtendedByIdentity(this.model.definition);
    }

    @ResetMemoizers
    setDefinition(metricsDefinition: MetricsDefinitionExtended): void
    {
        this.model.definition = metricsDefinition ? metricsDefinition.getIdentity() : null;
    }
}

