import {Directive, InjectionToken, Injector, Type} from "@angular/core";
import {NavigationEnd} from "@angular/router";

import {AppContext} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";
import {DatabaseActivityService, DatabaseActivitySubscriber} from "app/services/domain/database-activity.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {Logger, LoggingService, LoggingSeverity} from "framework/services/logging.service";
import {UtilsService} from "framework/services/utils.service";
import {BaseComponent} from "framework/ui/components";
import {ControlOption} from "framework/ui/control-option";
import {ColumnConfiguration, DatatableManager, DatatableSort, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {Future} from "framework/utils/concurrency";
import {Debouncer} from "framework/utils/debouncers";
import {ResetMemoizers} from "framework/utils/memoizers";

import {Subscription} from "rxjs";

export interface ModelConstructor<T extends BaseModel>
{
    new(): T;
}

export interface BaseModel
{
    sysId?: string;
    updatedOn?: Date;
}

export interface ExtendedModelConstructor<M extends BaseModel, E extends ExtendedModel<M>, T extends BaseService<M, E>>
{
    (svc: T,
     model: M): E;
}

export abstract class ExtendedModel<T extends BaseModel>
{
    public constructor(private svc: BaseService<T, any>,
                       public model: T,
                       private targetTable: string)
    {
    }

    protected injectService<T>(token: Type<T> | InjectionToken<T>): T
    {
        return this.svc.injectService(token);
    }

    protected get domain(): AppDomainContext
    {
        return this.svc.domain;
    }

    public hasIdentity(): boolean
    {
        return this.model && !!this.model.sysId;
    }

    public sameIdentity(other: ExtendedModel<T>): boolean
    {
        return other?.model.sysId == this.model.sysId;
    }

    public getIdentity(): Models.RecordIdentity
    {
        return this.model?.sysId ? Models.RecordIdentity.newInstance({
                                                                         table     : this.targetTable,
                                                                         sysId     : this.model.sysId,
                                                                         lastUpdate: this.model.updatedOn
                                                                     }) : null;
    }

    public flush(): Promise<void>
    {
        return this.svc.flush(this);
    }

    public flushModel(model: T): Promise<void>
    {
        return this.svc.flushModel(model);
    }

    async refresh<T2 extends ExtendedModel<T>>(): Promise<T2>
    {
        await this.flush();

        return <Promise<T2>>this.svc.refreshExtended(this);
    }

    async refreshIfNeeded<T2 extends ExtendedModel<T>>(): Promise<T2>
    {
        let res;

        if (this.svc.cacheStillValid(this.model.sysId, this.model.updatedOn))
        {
            res = this;
        }
        else
        {
            res = await this.svc.refreshExtended(this);
        }

        return <T2>res;
    }

    protected removeFromCollection<S extends BaseModel>(target: ExtendedModel<S>,
                                                        ids: Models.RecordIdentity[],
                                                        values: ExtendedModel<S>[]): boolean
    {
        let pos = ids.findIndex((value: Models.RecordIdentity) =>
                                {
                                    return value.sysId == target.model.sysId;
                                });

        if (pos >= 0)
        {
            ids.splice(pos, 1);
            if (values != undefined) values.splice(pos, 1);
            return true;
        }

        if (values != undefined)
        {
            pos = values.indexOf(target);
            if (pos >= 0)
            {
                values.splice(pos, 1);
                return true;
            }
        }

        return false;
    }

    //--//

    public subscribe(fn: (ext: ExtendedModel<T>,
                          action: Models.DbAction) => Promise<void>): DbChangeSubscription<T>
    {
        return this.svc.subscribe(this, fn);
    }

    //--//

    @ResetMemoizers
    public async resetAllCachedValues()
    {
        // The side effect of this method is the flushing of the memoizer cache.
        await this.flush();
    }

    //--//

    isValid(): boolean
    {
        return this.validate().length == 0;
    }

    validate(): ModelValidationError[]
    {
        let errors: ModelValidationError[] = [];

        this.validateInner(errors);

        return errors;
    }

    protected validateInner(errors: ModelValidationError[])
    {
    }
}

export class ModelValidationError
{
    field: string;

    message: string;

    static add(errors: ModelValidationError[],
               field: string,
               message: string)
    {
        let error     = new ModelValidationError();
        error.field   = field;
        error.message = message;
        errors.push(error);
    }
}


/**
 * This class is only used as a tag for a logger common to all data services.
 */
class CommonServiceLog
{
}

/**
 * This class is only used as a tag for a logger used for cache interactions.
 */
class CacheServiceLog
{
}

export abstract class BaseService<M extends BaseModel, E extends ExtendedModel<M>>
{
    readonly logger: Logger;
    readonly loggerForCache: Logger;

    private m_domain: AppDomainContext;

    private mas: DatabaseActivityService;
    private dbActivitySub: DatabaseActivitySubscriberImpl<M>;

    constructor(public api: ApiService,
                public errors: ErrorService,
                private cache: CacheService,
                private modelBuilder: ModelConstructor<M>,
                private extBuilder: ExtendedModelConstructor<M, E, any>)
    {
        let logService      = api.injector.get(LoggingService) as LoggingService;
        let baseLogger      = logService.getLogger(CommonServiceLog);
        this.logger         = baseLogger.getLogger((<any>this).constructor);
        this.loggerForCache = logService.getLogger(CacheServiceLog);

        this.mas = api.injector.get(DatabaseActivityService);

        let table          = this.cachePrefix();
        this.dbActivitySub = new DatabaseActivitySubscriberImpl<M>(this, table);
        this.mas.register(this.dbActivitySub, table);
    }

    private autoBatch: Map<string, LazyRequest<M>> = new Map<string, LazyRequest<M>>();
    private flushBatchTimer: any;

    //--//

    injectService<T>(token: Type<T> | InjectionToken<T>): T
    {
        return this.api.injector.get(token);
    }

    get domain(): AppDomainContext
    {
        if (!this.m_domain)
        {
            this.m_domain = this.injectService(AppDomainContext);
        }

        return this.m_domain;
    }

    protected abstract cachePrefix(): string;

    protected abstract getRaw(id: string): Promise<M>;

    protected abstract getBatchRaw(ids: string[]): Promise<M[]>;

    protected getBatchRawSize(): number
    {
        return 100;
    }

    protected afterJsonParse(model: M): M
    {
        return UtilsService.afterJsonParse(model, this.modelBuilder);
    }

    protected canCacheValues(): boolean
    {
        return true;
    }

    public handleDbEvent(value: Models.DbEvent)
    {
        let key = BaseService.generateCacheKey(value.context.table, value.context.sysId);
        this.loggerForCache.debug(`Flushing ${key}, due to DB activity ${value.action} on ${value.context.lastUpdate}`);
        this.cache.flush(key);

        this.flushInFlight(value.context.sysId, value.context.lastUpdate);
    }

    public flushStale(lastUpdate: Date)
    {
        let table = this.cachePrefix();

        this.logger.debug(`Checking table ${table}`);

        let lastUpdateValue = lastUpdate.valueOf();

        for (let cacheItem of this.cache.getAll(BaseService.generateCacheKey(table, "")))
        {
            this.logger.debug(`Checking ${cacheItem.key}`);
            let cachedRec = CacheService.getValueAs<CachedRecord<any>>(cacheItem, null);
            if (cachedRec?.lastUpdate >= lastUpdateValue)
            {
                continue;
            }

            this.logger.debug(`Flushing ${cacheItem.key}`);
            this.cache.flush(cacheItem.key);
            this.flushInFlight(cachedRec.sysId, lastUpdate);
        }
    }

    //--//

    private getRawLazy(id: string): Future<M>
    {
        let lazyReq = this.autoBatch.get(id);
        if (!lazyReq)
        {
            lazyReq = new LazyRequest<M>();
            this.autoBatch.set(id, lazyReq);
        }

        this.setLazyFlushTimer();

        return lazyReq.value;
    }

    private async flushLazyBatch()
    {
        // Prepare for a new batch.
        this.clearLazyFlushTimer();

        let timestamp     = new Date();
        let ids: string[] = [];
        this.autoBatch.forEach((req,
                                id) =>
                               {
                                   if (!req.inFlight)
                                   {
                                       req.inFlight = timestamp;
                                       ids.push(id);
                                   }
                               });

        let offset             = 0;
        let remaining          = ids.length;
        let chunkSize          = this.getBatchRawSize();
        let pendingBatchChunks = [];

        while (remaining > 0)
        {
            let batchSize = Math.min(remaining, chunkSize);

            //
            // Queue the batch but don't wait for it.
            //
            pendingBatchChunks.push(this.fetchLazyBatchChunk(ids, offset, batchSize));

            remaining -= batchSize;
            offset += batchSize;
        }

        if (pendingBatchChunks.length > 0)
        {
            await Promise.all(pendingBatchChunks);
        }

        if (this.autoBatch.size > 0)
        {
            this.setLazyFlushTimer();
        }
    }

    private async fetchLazyBatchChunk(ids: string[],
                                      offset: number,
                                      batchSize: number)
    {
        let batch   = ids.slice(offset, offset + batchSize);
        let results = await AutoRetryRequest(3, () => this.getBatchRaw(batch));

        for (let i = 0; i < batch.length; i++)
        {
            let id  = batch[i];
            let req = this.autoBatch.get(id);

            if (req)
            {
                if (req.invalidated)
                {
                    // Leave in the queue for a new round.
                    req.inFlight    = undefined;
                    req.invalidated = undefined;
                }
                else
                {
                    this.autoBatch.delete(id);

                    req.value.resolve(results[i]);
                }
            }
        }
    }

    private setLazyFlushTimer()
    {
        if (!this.flushBatchTimer)
        {
            this.flushBatchTimer = setTimeout(() => this.flushLazyBatch(), 10);
        }
    }

    private clearLazyFlushTimer()
    {
        if (this.flushBatchTimer)
        {
            clearTimeout(this.flushBatchTimer);
            this.flushBatchTimer = undefined;
        }
    }

    private flushInFlight(id: string,
                          lastUpdate: Date)
    {
        let lazyReq = this.autoBatch.get(id);
        if (lazyReq && lazyReq.inFlight && lazyReq.inFlight < lastUpdate)
        {
            // In-flight request was snapped before the update, invalidate the request.
            lazyReq.invalidated = true;
        }
    }

    //--//

    public static generateCacheKey(table: string,
                                   id: string)
    {
        return `${table}:${id}`;
    }

    //--//

    public async getWithIdentity(ri: Models.RecordIdentity): Promise<M>
    {
        if (!ri)
        {
            return null;
        }

        return this.get(ri.sysId, ri.lastUpdate);
    }

    public cacheStillValid(id: string,
                           lastUpdate: Date): boolean
    {
        let lastUpdateValue = lastUpdate?.valueOf();

        let key       = BaseService.generateCacheKey(this.cachePrefix(), id);
        let cachedRec = this.cache.getValueAs<CachedRecord<M>>(key);
        if (cachedRec)
        {
            if (lastUpdateValue === undefined || cachedRec.lastUpdate >= lastUpdateValue)
            {
                return true;
            }
        }

        return false;
    }

    public async get(id: string,
                     lastUpdate: Date): Promise<M>
    {
        let lastUpdateValue = lastUpdate?.valueOf();

        let key       = BaseService.generateCacheKey(this.cachePrefix(), id);
        let cachedRec = this.cache.getValueAs<CachedRecord<M>>(key);
        if (cachedRec)
        {
            if (lastUpdateValue === undefined || cachedRec.lastUpdate >= lastUpdateValue)
            {
                this.loggerForCache.debug(`Cache hit for ${key}`);
                return this.afterJsonParse(cachedRec.value);
            }

            // Flush the cache.
            this.loggerForCache.debug(`Cache hit for ${key}, but value is stale (${new Date(lastUpdateValue)} > ${new Date(cachedRec.lastUpdate)})`);
            this.cache.flush(key);
        }

        //
        // Prepare timestamping it *before* the call, just in case there's no date in the results.
        //
        lastUpdate = new Date();

        let rawModel = await this.getRawLazy(id);
        if (!rawModel)
        {
            return null;
        }

        if (!rawModel.updatedOn)
        {
            rawModel.updatedOn = lastUpdate;
        }

        let newCachedRec        = new CachedRecord<M>();
        newCachedRec.sysId      = id;
        newCachedRec.lastUpdate = lastUpdate.valueOf();
        newCachedRec.value      = rawModel;

        // Update the cache.
        this.loggerForCache.debug(`Cache miss for ${key}, setting for ${lastUpdate}`);
        if (this.canCacheValues())
        {
            this.cache.set(key, newCachedRec);
        }

        this.dbActivitySub.raiseChange(id, lastUpdate, null);

        return newCachedRec.value;
    }

    public async getBatch(ids: Models.RecordIdentity[]): Promise<M[]>
    {
        if (!ids || ids.length == 0)
        {
            return [];
        }

        let promises = new Array<Promise<M>>(ids.length);
        for (let i = 0; i < ids.length; i++)
        {
            promises[i] = this.getWithIdentity(ids[i]);
        }

        await Promise.all(promises);

        let results = new Array<M>(ids.length);
        for (let i = 0; i < ids.length; i++)
        {
            results[i] = await promises[i];
        }

        return results;
    }

    public async flush(ext: E): Promise<void>
    {
        if (ext)
        {
            await this.flushModel(ext.model);
        }
    }

    public async flushModel(model: M): Promise<void>
    {
        if (model)
        {
            let key = BaseService.generateCacheKey(this.cachePrefix(), model.sysId);
            this.loggerForCache.debug(`Flushing model for ${key}`);

            await this.cache.flush(key);
        }
    }

    public subscribe(ext: ExtendedModel<M>,
                     fn: (ext: ExtendedModel<M>,
                          action: Models.DbAction) => Promise<void>): DbChangeSubscription<M>
    {
        return this.dbActivitySub.subscribe(ext, fn);
    }

    public subscribeAny(fn: (action: Models.DbAction) => Promise<void>): DbChangeSubscription<M>
    {
        return this.dbActivitySub.subscribe(null,
                                            (ext,
                                             action) => fn(action));
    }

    public listenToDatabase(callback: (from: DbChangeNotifier<M, E>,
                                       action: Models.DbAction) => Promise<void>,
                            debounceInserts: number = 100,
                            debounceUpdates: number = 200,
                            debounceDeletes: number = 100): DbChangeNotifier<M, E>
    {
        return new DbChangeNotifier(this, debounceInserts, debounceUpdates, debounceDeletes, callback);
    }

    //--//

    wrapModel(model: M): E
    {
        return model ? this.extBuilder(this, model) : null;
    }

    allocateInstance(): E
    {
        return this.wrapModel(new this.modelBuilder());
    }

    async refreshExtended(ext: E): Promise<E>
    {
        if (!ext || !ext.model.sysId)
        {
            return ext;
        }

        return this.getExtendedById(ext.model.sysId);
    }

    async getExtendedById(id: string): Promise<E>
    {
        if (!id)
        {
            return null;
        }

        return this.wrapModel(await this.get(id, null));
    }

    getExtendedByIdentity(ri: Models.RecordIdentity): Promise<E>
    {
        if (!ri)
        {
            return null;
        }

        return this.getExtendedById(ri.sysId);
    }

    async getExtendedBatch(ids: Models.RecordIdentity[]): Promise<E[]>
    {
        if (!ids || ids.length == 0)
        {
            return [];
        }

        let results = [];
        for (let model of await this.getBatch(ids))
        {
            let ext = this.wrapModel(model);
            if (ext) // Only include valid entries.
            {
                results.push(ext);
            }
        }

        return results;
    }

    async getPage(list: Models.RecordIdentity[],
                  offset: number,
                  limit: number): Promise<E[]>
    {
        if (!list || list.length == 0)
        {
            return [];
        }

        const start = offset * limit;
        const end   = start + limit;

        let ids: Models.RecordIdentity[] = [];

        for (let i = start; i < end; i++)
        {
            if (list[i]) ids.push(list[i]);
        }

        return await this.getExtendedBatch(ids);
    }

    async getPageFromTable(table: DatatableManager<Models.RecordIdentity, E, any>,
                           offset: number,
                           limit: number): Promise<E[]>
    {
        return table ? await this.getExtendedBatch(table.slicePage(offset, limit)) : [];
    }

    //--//

    asType<E2 extends E>(type: Type<E2>,
                         ext: E): E2
    {
        return ext instanceof type ? <E2>ext : null;
    }

    typedFilter<E2 extends E>(type: Type<E2>,
                              exts: E[],
                              removeNulls: boolean): E2[]
    {
        let exts2 = exts.map((ext) => ext instanceof type ? <E2>ext : null);

        if (removeNulls)
        {
            exts2 = exts2.filter((ext) => !!ext);
        }

        return exts2;
    }

    public static mapOptions<T, C>(inputs: T[],
                                   callback: (item: T) => ControlOption<C>): ControlOption<C>[]
    {
        let res = [];

        for (let v of inputs)
        {
            let option = callback(v);
            if (option) res.push(option);
        }

        return res;
    }

    public static async mapOptionsAsync<T, C>(inputs: T[],
                                              callback: (item: T) => Promise<ControlOption<C>>): Promise<ControlOption<C>[]>
    {
        let res = [];

        for (let v of inputs)
        {
            let option = await callback(v);
            if (option) res.push(option);
        }

        return res;
    }

    public static mapEnumOptions<T>(inputs: Models.EnumDescriptor[]): ControlOption<T>[]
    {
        return this.mapOptions(inputs, (desc) =>
        {
            let option = new ControlOption<T>();

            option.id    = <T><any>desc.id;
            option.label = desc.description ? `${desc.displayName} - ${desc.description}` : desc.displayName;

            return option;
        });
    }
}

class CachedRecord<M>
{
    sysId: string;
    lastUpdate: number;
    value: M;
}

class LazyRequest<M>
{
    inFlight: Date;
    invalidated: boolean;

    readonly value: Future<M> = new Future<M>();
}

class DatabaseActivitySubscriberImpl<M extends BaseModel> extends DatabaseActivitySubscriber
{
    subscribers: Map<string, Set<DbChangeSubscription<M>>> = new Map();

    constructor(private svc: BaseService<M, any>,
                private table: string)
    {
        super();
    }

    async onConnect(lastUpdate: Date): Promise<void>
    {
        this.svc.flushStale(lastUpdate);
    }

    async onDisconnect(): Promise<void>
    {
        this.svc.logger.debug(`onDisconnect: ${this.table}`);
    }

    onMessage(value: Models.DbEvent)
    {
        if (this.svc.logger.isEnabled(LoggingSeverity.DebugVerbose))
        {
            this.svc.logger.debugVerbose(`onMessage: ${this.table} => ${JSON.stringify(value)}`);
        }

        this.svc.handleDbEvent(value);

        this.raiseChange(value.context.sysId, value.context.lastUpdate, value.action);
    }

    //--//

    // Id used to subscribe to table changes.
    private readonly idForTableSubscription: string = "<table>";

    public subscribe(ext: ExtendedModel<M>,
                     fn: (ext: ExtendedModel<M>,
                          action: Models.DbAction) => Promise<void>): DbChangeSubscription<M>
    {
        let id  = this.resolveId(ext);
        let set = this.subscribers.get(id);
        if (!set)
        {
            set = new Set();
            this.subscribers.set(id, set);
            this.svc.logger.debug(`First subscription for ${this.table}/${id}`);
        }

        let res = new DbChangeSubscription(this, ext, fn);
        set.add(res);
        return res;
    }

    public unsubscribe(sub: DbChangeSubscription<M>)
    {
        let id  = this.resolveId(sub.ext);
        let set = this.subscribers.get(id);
        if (set)
        {
            set.delete(sub);
            if (set.size == 0)
            {
                this.svc.logger.debug(`Last subscription for ${this.table}/${id}`);
                this.subscribers.delete(id);
            }
        }
    }

    raiseChange(sysId: string,
                lastUpdate: Date,
                action: Models.DbAction)
    {
        if (this.svc.logger.isEnabled(LoggingSeverity.Debug))
        {
            this.svc.logger.debug(`Raising change for ${this.table}/${sysId} - ${lastUpdate} - ${action}`);
        }

        // Raise event for this specific record.
        this.raiseChangeInner(sysId, lastUpdate, action);

        // Raise event for any record in this table.
        this.raiseChangeInner(this.idForTableSubscription, lastUpdate, action);
    }

    private raiseChangeInner(id: string,
                             lastUpdate: Date,
                             action: Models.DbAction)
    {
        let set = this.subscribers.get(id);
        if (set)
        {
            let targets: DbChangeSubscription<M>[] = [];

            // Copy values, in case the subscription set gets modified by the callback.
            set.forEach((sub) => targets.push(sub));

            for (let sub of targets)
            {
                try
                {
                    sub.raise(lastUpdate, action);
                }
                catch (error)
                {
                    this.svc.logger.error(`DatabaseActivitySubscriber for ${this.table}:${id} threw an error: ${JSON.stringify(error)}`);
                }
            }
        }
    }

    private resolveId(ext: ExtendedModel<M>)
    {
        return ext && ext.model && ext.model.sysId ? ext.model.sysId : this.idForTableSubscription;
    }
}

export class DbChangeSubscription<T extends BaseModel>
{
    constructor(private src: DatabaseActivitySubscriberImpl<T>,
                public ext: ExtendedModel<T>,
                private fn: (ext: ExtendedModel<T>,
                             action: Models.DbAction) => Promise<void>)
    {
    }

    public unsubscribe()
    {
        this.fn = null;

        this.src.unsubscribe(this);
    }

    public raise(lastUpdate: Date,
                 action: Models.DbAction)
    {
        if (action == null || action == Models.DbAction.UPDATE_DIRECT || action == Models.DbAction.UPDATE_INDIRECT)
        {
            if (this.ext && this.ext.model.updatedOn == lastUpdate)
            {
                return;
            }
            else if (action == null && !this.ext)
            {
                // Prevent subscribeAny from triggering from getWithIdentity
                return;
            }
        }

        if (this.fn) this.fn(this.ext, action);
    }
}

export class DbChangeNotifier<M extends BaseModel, E extends ExtendedModel<M>>
{
    depChangeAny: DbChangeSubscription<M>;
    depChangeSubs: DbChangeSubscription<M>[];

    debouncerInserts: Debouncer;
    debouncerUpdates: Debouncer;
    debouncerUpdatesIndirect: Debouncer;
    debouncerDeletes: Debouncer;

    constructor(private svc: BaseService<M, E>,
                debounceInserts: number,
                debounceUpdates: number,
                debounceDeletes: number,
                private notify: (from: DbChangeNotifier<M, E>,
                                 action: Models.DbAction) => Promise<void>)
    {
        this.debouncerInserts = new Debouncer(debounceInserts, async () =>
        {
            await this.notify(this, Models.DbAction.INSERT);
        });

        this.debouncerUpdates = new Debouncer(debounceUpdates, async () =>
        {
            await this.notify(this, Models.DbAction.UPDATE_DIRECT);
        });

        this.debouncerUpdatesIndirect = new Debouncer(debounceUpdates, async () =>
        {
            await this.notify(this, Models.DbAction.UPDATE_INDIRECT);
        });

        this.debouncerDeletes = new Debouncer(debounceDeletes, async () =>
        {
            await this.notify(this, Models.DbAction.DELETE);
        });
    }

    public subscribe(rows: E[],
                     includeInsert: boolean,
                     includeUpdates: boolean,
                     includeDeletes: boolean)
    {
        this.unsubscribe();

        if (includeInsert || includeUpdates || includeDeletes)
        {
            this.depChangeAny = this.svc.subscribeAny(async (action) =>
                                                      {
                                                          switch (action)
                                                          {
                                                              case Models.DbAction.INSERT:
                                                                  if (includeInsert) this.debouncerInserts.invoke();
                                                                  break;

                                                              case Models.DbAction.UPDATE_DIRECT:
                                                                  if (includeUpdates) this.debouncerUpdates.invoke();
                                                                  break;

                                                              case Models.DbAction.UPDATE_INDIRECT:
                                                                  if (includeUpdates) this.debouncerUpdatesIndirect.invoke();
                                                                  break;

                                                              case Models.DbAction.DELETE:
                                                                  if (includeDeletes) this.debouncerDeletes.invoke();
                                                                  break;
                                                          }
                                                      });
        }

        let depChangeSubs = [];

        for (let row of (rows || []))
        {
            if (row)
            {
                this.svc.logger.debug(`subscribe for ${row.model.sysId}`);

                let sub = row.subscribe(async (ext,
                                               action) =>
                                        {
                                            switch (action)
                                            {
                                                case Models.DbAction.UPDATE_DIRECT:
                                                case Models.DbAction.UPDATE_INDIRECT:
                                                    this.debouncerUpdates.invoke();
                                                    break;
                                            }
                                        });

                depChangeSubs.push(sub);
            }
        }

        if (depChangeSubs.length > 0)
        {
            this.depChangeSubs = depChangeSubs;
        }
    }

    public unsubscribe()
    {
        if (this.depChangeSubs)
        {
            for (let sub of this.depChangeSubs)
            {
                this.svc.logger.debug(`unsubscribe for ${sub.ext.model.sysId}`);
                sub.unsubscribe();
            }

            this.depChangeSubs = null;
        }

        if (this.depChangeAny)
        {
            this.depChangeAny.unsubscribe();
            this.depChangeAny = null;
        }
    }
}

//--//

export class DeltaTracker<S extends BaseModel, E extends ExtendedModel<S>>
{
    added: E[] = [];

    removed: E[] = [];

    //--//

    constructor(private svc: BaseService<S, E>,
                public items: E[])
    {
    }

    newItem(): E
    {
        let target = this.svc.allocateInstance();

        this.added.push(target);
        this.items.push(target);

        return target;
    }

    removeItem(target: E)
    {
        this.removeFromCollection(this.items, target);

        if (this.removeFromCollection(this.added, target)) return;

        this.removed.push(target);
    }

    async flush(save: (item: E) => Promise<any>,
                remove: (item: E) => Promise<any>): Promise<void>
    {
        for (let e of this.items)
        {
            await save(e);
        }

        for (let e of this.removed)
        {
            await remove(e);
        }
    }

    private removeFromCollection(coll: E[],
                                 target: E): boolean
    {
        let pos = coll.indexOf(target);
        if (pos < 0) return false;

        coll.splice(pos, 1);
        return true;
    }
}

//--//

class DatatableDataProviderImpl<K, M extends BaseModel, E extends ExtendedModel<M>, T> implements IDatatableDataProvider<K, E, T>
{
    readonly table: DatatableManager<K, E, T>;
    private readonly notifier: DbChangeNotifier<M, E>;

    getTableConfigId(): string
    {
        return this.provider.getTableConfigId();
    }

    constructor(comp: BaseComponentWithTables,
                svc: BaseService<M, E>,
                private provider: IDatatableDataProvider<K, E, T>,
                debounceInserts: number,
                debounceUpdates: number,
                debounceDeletes: number)
    {
        this.table = new DatatableManager<K, E, T>(this, () => comp.getViewState());

        let callbackUpdates = async () =>
        {
            await comp.blockOnDelayedNotification();
            if (comp.wasDestroyed()) return;

            await this.table.refreshPage();
        };

        let callbackOther = async () =>
        {
            await comp.blockOnDelayedNotification();
            if (comp.wasDestroyed()) return;

            await this.table.refreshData();
        };

        this.notifier = comp.listenToDatabase(svc, callbackUpdates, callbackOther, debounceInserts, debounceUpdates, debounceDeletes);
    }

    getColumnConfigs(): Promise<ColumnConfiguration[]>
    {
        return this.provider.getColumnConfigs();
    }

    setColumnConfigs(columnConfigs: ColumnConfiguration[]): Promise<boolean>
    {
        return this.provider.setColumnConfigs(columnConfigs);
    }

    wasDestroyed(): boolean
    {
        return this.provider.wasDestroyed();
    }

    public getItemName(): string
    {
        return this.provider.getItemName();
    }

    public getList(): Promise<K[]>
    {
        return this.provider.getList();
    }

    public async getPage(offset: number,
                         limit: number): Promise<E[]>
    {
        this.notifier.unsubscribe();

        let rows = await this.provider.getPage(offset, limit);

        this.notifier.subscribe(rows, true, true, true);

        return rows;
    }

    public transform(rows: E[]): Promise<T[]>
    {
        return this.provider.transform(rows);
    }

    public itemClicked(columnId: string,
                       item: T)
    {
        this.provider.itemClicked(columnId, item);
    }
}

@Directive()
export abstract class BaseComponentWithTables extends BaseComponent
{
    private changeNotifiers: DbChangeNotifier<any, any>[] = [];

    private changeSubscriptions: DbChangeSubscription<any>[] = [];

    private delayedTablesUntilAfterViewInit: DatatableManager<any, any, any>[] = [];

    private m_delayNotifications: Future<void>;

    constructor(inj: Injector)
    {
        super(inj);
    }

    ngAfterViewInit(): void
    {
        super.ngAfterViewInit();

        if (this.delayedTablesUntilAfterViewInit)
        {
            for (let table of this.delayedTablesUntilAfterViewInit)
            {
                table.refreshData();
            }

            this.delayedTablesUntilAfterViewInit = null;
        }
    }

    private pushTableRefresh(table: DatatableManager<any, any, any>)
    {
        if (this.delayedTablesUntilAfterViewInit)
        {
            this.delayedTablesUntilAfterViewInit.push(table);
        }
        else
        {
            table.refreshData();
        }
    }

    protected mapSortBindings(sort: DatatableSort[]): Models.SortCriteria[]
    {
        if (!sort) return null;

        return sort.map((s) =>
                        {
                            let res       = new Models.SortCriteria();
                            res.column    = s.prop;
                            res.ascending = s.dir == "asc";
                            return res;
                        });
    }

    ngOnDestroy(): void
    {
        super.ngOnDestroy();

        for (let changeNotifier of this.changeNotifiers)
        {
            changeNotifier.unsubscribe();
        }

        for (let changeSubscription of this.changeSubscriptions)
        {
            changeSubscription.unsubscribe();
        }

        this.resumeNotifications(); // To unblock the clients.
    }

    //--//

    public detectChanges()
    {
        super.detectChanges();

        this.processDelayedNotifications();
    }

    public markForCheck()
    {
        super.markForCheck();

        this.processDelayedNotifications();
    }

    protected shouldDelayNotifications()
    {
        return false;
    }

    async blockOnDelayedNotification()
    {
        this.processDelayedNotifications();

        if (this.m_delayNotifications)
        {
            await this.m_delayNotifications;
        }
    }

    private processDelayedNotifications()
    {
        if (this.wasDestroyed()) return;

        if (this.shouldDelayNotifications())
        {
            this.blockNotifications();
        }
        else
        {
            this.resumeNotifications();
        }
    }

    private resumeNotifications()
    {
        if (this.m_delayNotifications)
        {
            this.m_delayNotifications.resolve();
            this.m_delayNotifications = undefined;
        }
    }

    private blockNotifications()
    {
        if (!this.m_delayNotifications)
        {
            this.m_delayNotifications = new Future<void>();
        }
    }

    //--//

    public listenToDatabase<M extends BaseModel, E extends ExtendedModel<M>>(svc: BaseService<M, E>,
                                                                             callbackUpdates: (from: DbChangeNotifier<M, E>) => Promise<void>,
                                                                             callbackOther: (from: DbChangeNotifier<M, E>) => Promise<void>,
                                                                             debounceInserts: number = 100,
                                                                             debounceUpdates: number = 200,
                                                                             debounceDeletes: number = 100): DbChangeNotifier<M, E>
    {
        let notifier = svc.listenToDatabase(async (from,
                                                   action) =>
                                            {
                                                await this.blockOnDelayedNotification();
                                                if (this.wasDestroyed()) return;

                                                switch (action)
                                                {
                                                    case Models.DbAction.UPDATE_DIRECT:
                                                    case Models.DbAction.UPDATE_INDIRECT:
                                                        if (callbackUpdates) await callbackUpdates(from);
                                                        break;

                                                    default:
                                                        if (callbackOther) await callbackOther(from);
                                                        break;
                                                }
                                            }, debounceInserts, debounceUpdates, debounceDeletes);

        this.changeNotifiers.push(notifier);

        return notifier;
    }

    protected newTableWithAutoRefresh<K, M extends BaseModel, E extends ExtendedModel<M>, T>(svc: BaseService<M, E>,
                                                                                             provider: IDatatableDataProvider<K, E, T>,
                                                                                             debounceInserts: number = 100,
                                                                                             debounceUpdates: number = 200,
                                                                                             debounceDeletes: number = 100): DatatableManager<K, E, T>
    {
        let wrapperTable = new DatatableDataProviderImpl<K, M, E, T>(this, svc, provider, debounceInserts, debounceUpdates, debounceDeletes);
        let table        = wrapperTable.table;

        this.pushTableRefresh(table);

        return table;
    }

    public subscribe<T extends BaseModel>(ext: ExtendedModel<T>,
                                          fn: (ext: ExtendedModel<T>,
                                               action: Models.DbAction) => Promise<void>): DbChangeSubscription<T>
    {
        if (this.wasDestroyed()) return null;

        if (ext && fn)
        {
            let sub = ext.subscribe(async (ext,
                                           action) =>
                                    {
                                        await this.blockOnDelayedNotification();
                                        if (this.wasDestroyed()) return;

                                        await fn(ext, action);
                                    });

            this.changeSubscriptions.push(sub);

            return sub;
        }

        return null;
    }

    protected subscribeAny<M extends BaseModel, E extends ExtendedModel<M>>(svc: BaseService<M, E>,
                                                                            fn: (action: Models.DbAction) => Promise<void>): DbChangeSubscription<M>
    {
        if (this.wasDestroyed()) return null;

        if (fn)
        {
            let sub = svc.subscribeAny(async (action) =>
                                       {
                                           await this.blockOnDelayedNotification();
                                           if (this.wasDestroyed()) return;

                                           await fn(action);
                                       });

            this.changeSubscriptions.push(sub);

            return sub;
        }

        return null;
    }

    public subscribeOneShot<T extends BaseModel>(ext: ExtendedModel<T>,
                                                 fn: (ext: ExtendedModel<T>,
                                                      action: Models.DbAction) => Promise<void>): DbChangeSubscription<T>
    {
        if (this.wasDestroyed()) return null;

        let sub = this.subscribe(ext,
                                 (extInner,
                                  actionInner) =>
                                 {
                                     this.removeSubscription(sub);
                                     return fn(extInner, actionInner);
                                 });
        return sub;
    }

    protected async waitForChange<E extends ExtendedModel<T>, T extends BaseModel>(ext: E): Promise<E>
    {
        if (this.wasDestroyed()) return null;

        let changeTriggered = new Future<void>();

        this.subscribeOneShot(ext, async () => changeTriggered.resolve());

        await changeTriggered;

        return ext.refresh<E>();
    }

    protected removeAllDbSubscriptions()
    {
        for (let changeSubscription of this.changeSubscriptions)
        {
            changeSubscription.unsubscribe();
        }

        this.changeSubscriptions = [];
    }

    public removeSubscription<T>(sub: DbChangeSubscription<T>)
    {
        if (sub)
        {
            sub.unsubscribe();

            let pos = this.changeSubscriptions.indexOf(sub);
            if (pos >= 0)
            {
                this.changeSubscriptions.splice(pos, 1);
            }
        }
    }

    //--//

    protected fromValidationToReason(heading: string,
                                     checks: Models.ValidationResult[]): string
    {
        if (checks && checks.length > 0)
        {
            let details = [
                heading,
                ""
            ];

            for (let validationError of checks)
            {
                details.push(`'${validationError.field}' : ${validationError.reason}`);
            }

            return details.join("\n");
        }
        else
        {
            return null;
        }
    }
}

@Directive()
export abstract class BaseApplicationComponent extends BaseComponentWithTables
{
    app: AppContext;

    private static readonly c_datatableConfig = "datatable-config";

    constructor(inj: Injector)
    {
        super(inj);

        this.app = new AppContext(inj);
    }

    get hasDemoData(): boolean
    {
        return this.app.domain.users.hasDemoData;
    }

    get isMaint(): boolean
    {
        return this.app.domain.users.hasMaintRole();
    }

    get isAdmin(): boolean
    {
        return this.app.domain.users.hasAdminRole();
    }

    get isPublisher(): boolean
    {
        return this.app.domain.users.hasPublisherRole();
    }

    getTableConfigId(): string
    {
        return null;
    }

    public async getColumnConfigs(): Promise<ColumnConfiguration[]>
    {
        let configId = this.getTableConfigId();
        if (configId)
        {
            let datatableConfig = await this.app.domain.users.getTypedPreference<Models.DatatableConfiguration>(BaseApplicationComponent.c_datatableConfig,
                                                                                                                configId,
                                                                                                                Models.DatatableConfiguration.fixupPrototype);
            return BaseApplicationComponent.toColumnInfo(datatableConfig);
        }
        return null;
    }

    public async setColumnConfigs(configs: ColumnConfiguration[]): Promise<boolean>
    {
        let configId = this.getTableConfigId();
        if (configId)
        {
            let datatableConfig = BaseApplicationComponent.toDatatableConfigModel(configs);
            return !!await this.app.domain.users.setTypedPreference<Models.DatatableConfiguration>(BaseApplicationComponent.c_datatableConfig, configId, datatableConfig);
        }
        return null;
    }

    private static toColumnInfo(config: Models.DatatableConfiguration): ColumnConfiguration[]
    {
        return !config ? null : config.columns.map((columnModel) => new ColumnConfiguration(columnModel.id, columnModel.enabled, columnModel.name));
    }

    private static toDatatableConfigModel(columnConfigs: ColumnConfiguration[]): Models.DatatableConfiguration
    {
        columnConfigs = columnConfigs || [];
        return Models.DatatableConfiguration.newInstance({
                                                             columns: columnConfigs.map((column) => Models.DatatableColumnConfiguration.newInstance({
                                                                                                                                                        id     : column.id,
                                                                                                                                                        name   : column.name,
                                                                                                                                                        enabled: column.enabled
                                                                                                                                                    }))
                                                         });
    }
}

@Directive()
export abstract class BaseComponentWithRouter extends BaseApplicationComponent
{
    private readonly m_eventsSub: Subscription;

    private m_afterViewCalled = false;
    private m_navigationEnded = false;

    constructor(inj: Injector)
    {
        super(inj);

        this.m_eventsSub = this.app.routing.router.events.subscribe((event) =>
                                                                    {
                                                                        if (event instanceof NavigationEnd)
                                                                        {
                                                                            this.m_navigationEnded = true;
                                                                            this.checkInitializationDone();
                                                                        }
                                                                    });

        if (!this.app.routing.isNavigating)
        {
            this.m_navigationEnded = true;
        }
    }

    public ngAfterViewInit()
    {
        super.ngAfterViewInit();
        this.m_afterViewCalled = true;
        this.checkInitializationDone();
    }

    public ngOnDestroy(): void
    {
        super.ngOnDestroy();

        this.m_eventsSub.unsubscribe();
    }

    private checkInitializationDone()
    {
        this.detectChanges();

        if (this.m_afterViewCalled && this.m_navigationEnded)
        {
            this.onNavigationComplete();
            this.detectChanges();
        }
    }

    protected getPathParameter(name: string): any
    {
        return this.app.routing.route.snapshot.params[name];
    }

    protected getQueryParameter(name: string): any
    {
        return this.app.routing.route.snapshot.queryParams[name];
    }

    protected getDataParameter(name: string): any
    {
        return this.app.routing.route.snapshot.data[name];
    }

    protected abstract onNavigationComplete(): void;
}

export class ActionDescriptor
{
    firstLevel: string;
    secondLevel: string;

    name: string;

    tooltip: string;

    execute: () => Promise<void>;

    static add(list: ActionDescriptor[],
               name: string,
               tooltip: string,
               execute: () => Promise<void>)
    {
        let ad     = new ActionDescriptor();
        ad.name    = name;
        ad.tooltip = tooltip;
        ad.execute = execute;

        list.push(ad);
    }

    static addAtSecondLevel(list: ActionDescriptor[],
                            firstLevel: string,
                            name: string,
                            tooltip: string,
                            execute: () => Promise<void>)
    {
        let ad        = new ActionDescriptor();
        ad.firstLevel = firstLevel;
        ad.name       = name;
        ad.tooltip    = tooltip;
        ad.execute    = execute;

        list.push(ad);
    }

    static addAtThirdLevel(list: ActionDescriptor[],
                           firstLevel: string,
                           secondLevel: string,
                           name: string,
                           tooltip: string,
                           execute: () => Promise<void>)
    {
        let ad         = new ActionDescriptor();
        ad.firstLevel  = firstLevel;
        ad.secondLevel = secondLevel;
        ad.name        = name;
        ad.tooltip     = tooltip;
        ad.execute     = execute;

        list.push(ad);
    }
}

export class AutoBatcher<I, O>
{
    private batchInput: I[]          = [];
    private batchOutput: Future<O>[] = [];
    private flushBatchTimer: any;

    constructor(private readonly maxBatchSize: number,
                private readonly delayInMillisec: number,
                private callback: (inputs: I[]) => Promise<O[]>)
    {
    }

    queue(input: I): Future<O>
    {
        let lazyOutput = new Future<O>();

        this.batchInput.push(input);
        this.batchOutput.push(lazyOutput);

        if (this.batchInput.length >= this.maxBatchSize)
        {
            this.flushLazyBatch();
        }
        else if (!this.flushBatchTimer)
        {
            this.flushBatchTimer = setTimeout(() => this.flushLazyBatch(), this.delayInMillisec);
        }

        return lazyOutput;
    }

    private async flushLazyBatch()
    {
        if (this.flushBatchTimer)
        {
            clearTimeout(this.flushBatchTimer);
            this.flushBatchTimer = undefined;
        }

        let batchInput  = this.batchInput;
        let batchOutput = this.batchOutput;

        // Prepare for a new batch.
        this.batchInput  = [];
        this.batchOutput = [];

        let results = await this.callback(batchInput);

        for (let i = 0; i < batchInput.length; i++)
        {
            batchOutput[i].resolve(results[i]);
        }
    }
}

export async function AutoRetryRequest<O>(maxRetries: number,
                                          callback: () => Promise<O>): Promise<O>
{
    while (true)
    {
        try
        {
            let results = await callback();
            return results;
        }
        catch (e)
        {
            if (maxRetries-- <= 0 || e.code != "UNEXPECTED_ERROR")
            {
                throw e;
            }

            // Wait a bit before retrying.
            await Future.delayed(200);
        }
    }
}
