import {ApiService} from "app/services/domain/api.service";
import {AppDomainContext} from "app/services/domain/index";

import * as Models from "app/services/proxy/model/models";

import {CacheItem, CachePolicy, CacheService} from "framework/services/cache.service";
import {Future} from "framework/utils/concurrency";
import {ResetMemoizers} from "framework/utils/memoizers";

export interface ModelConstructor<T extends BaseModel>
{
    new(): T;
}

export interface BaseModel
{
    sysId?: string;
    updatedOn?: Date;
}

export interface ExtendedModelConstructor<M extends BaseModel, E extends ExtendedModel<M>>
{
    (svc: BaseService<M, E>,
     model: M): E;
}

export class ExtendedModel<T extends BaseModel>
{
    self: ExtendedModel<T>;

    protected constructor(protected svc: BaseService<T, ExtendedModel<T>>,
                          public model: T)
    {
        this.self = this;
    }

    public sameIdentity(other: ExtendedModel<T>): boolean
    {
        return other && other.model.sysId == this.model.sysId;
    }

    public getIdentity(): Models.RecordIdentity
    {
        return this.svc.identityFromExtended(this);
    }

    public get domain(): AppDomainContext
    {
        return this.svc.domain;
    }

    flush(): Promise<void>
    {
        return this.svc.flush(this);
    }

    refresh<T2 extends ExtendedModel<T>>(): Promise<T2>
    {
        return <Promise<T2>> this.svc.refreshExtended(this);
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

    protected addToCollection<S extends BaseModel>(target: ExtendedModel<S>,
                                                   ids: Models.RecordIdentity[],
                                                   values: ExtendedModel<S>[])
    {
        let ri = new Models.RecordIdentity;
        ids.push(ri);
        values.push(target);
    }

    //--//

    @ResetMemoizers
    public resetAllCachedValues()
    {
        // The only side effect of this method is the flushing of the memoizer cache.
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

export abstract class BaseService<M extends BaseModel, E extends ExtendedModel<M>>
{
    private m_domain: AppDomainContext;

    constructor(public api: ApiService,
                public cache: CacheService,
                private modelBuilder: ModelConstructor<M>,
                private extBuilder: ExtendedModelConstructor<M, E>)
    {
        let table = this.cachePrefix();
    }

    private autoBatch: Map<string, Future<M>> = new Map<string, Future<M>>();
    private flushBatchTimer: any;

    //--//

    get domain(): AppDomainContext
    {
        if (this.m_domain == undefined)
        {
            this.m_domain = this.api.injector.get(AppDomainContext);
        }

        return this.m_domain;
    }

    protected abstract cachePrefix(): string;

    protected abstract getRaw(id: string): Promise<M>;

    protected abstract getBatchRaw(ids: string[]): Promise<M[]>;

    protected abstract afterExtendedBatch(exts: E[]);

    protected afterJsonParse(model: M): M
    {
        return model;
    }

    //--//

    private getRawLazy(id: string): Future<M>
    {
        let lazyVal = this.autoBatch.get(id);
        if (!lazyVal)
        {
            lazyVal = new Future<M>();
            this.autoBatch.set(id, lazyVal);
        }

        if (!this.flushBatchTimer)
        {
            this.flushBatchTimer = setTimeout(() => this.flushLazyBatch(), 10);
        }

        return lazyVal;
    }

    private async flushLazyBatch()
    {
        let batch = this.autoBatch;

        // Prepare for a new batch.
        this.autoBatch       = new Map<string, Future<M>>();
        this.flushBatchTimer = null;

        let ids = [];
        batch.forEach((value,
                       id) => ids.push(id));

        let results = await this.getBatchRaw(ids);
        for (let i = 0; i < ids.length; i++)
        {
            let id  = ids[i];
            let val = results[i];

            batch.get(id)
                 .resolve(val);
        }
    }

    //--//

    public static generateCacheKey(table: string,
                                   id: string)
    {
        return `${table}:${id}`;
    }

    //--//

    public identityFromExtended(ext: E): Models.RecordIdentity
    {
        return ext ? this.identityFromSysId(ext.model.sysId) : null;
    }

    public identityFromSysId(id: string): Models.RecordIdentity
    {
        if (!id)
        {
            return null;
        }

        let ri   = new Models.RecordIdentity();
        ri.table = this.cachePrefix();
        ri.sysId = id;

        return ri;
    }

    public async get(id: string): Promise<M>
    {
        return this.getWithIdentity(this.identityFromSysId(id));
    }

    public async getWithIdentity(ri: Models.RecordIdentity): Promise<M>
    {
        if (!ri)
        {
            return null;
        }

        let key       = BaseService.generateCacheKey(this.cachePrefix(), ri.sysId);
        let cacheItem = this.cache.get(key);
        if (cacheItem)
        {
            let cachedRec = this.validateCacheResult(cacheItem, ri);
            if (cachedRec)
            {
                return this.afterJsonParse(cachedRec.value);
            }

            // Flush the cache.
            this.cache.flush(key);
        }

        //
        // Prepare the identity of the new record, timestamping it *before* the call.
        //
        let newCachedRec           = new CachedRecord<M>();
        newCachedRec.ri            = new Models.RecordIdentity();
        newCachedRec.ri.table      = ri.table;
        newCachedRec.ri.sysId      = ri.sysId;
        newCachedRec.ri.lastUpdate = new Date();

        let promise        = this.getRawLazy(ri.sysId);
        newCachedRec.value = this.afterJsonParse(await promise);

        // Update the cache.
        this.cache.set(key, CachePolicy.Session, newCachedRec);

        return newCachedRec.value;
    }

    private validateCacheResult(cacheItem: CacheItem,
                                ri: Models.RecordIdentity): CachedRecord<M>
    {
        let cachedRec = CacheService.getValueAs<CachedRecord<M>>(cacheItem, null);
        if (!cachedRec || !cachedRec.ri)
        {
            return null;
        }

        if (ri.lastUpdate && cachedRec.ri.lastUpdate < ri.lastUpdate)
        {
            return null;
        }

        return cachedRec;
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
            await this.cache.flush(BaseService.generateCacheKey(this.cachePrefix(), model.sysId));
        }
    }

    //--//

    newInstance(model?: M): E
    {
        model = model || new this.modelBuilder();

        return this.extBuilder(this, model);
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

        return this.newInstance(await this.get(id));
    }

    async getExtendedByIdentity(id: Models.RecordIdentity): Promise<E>
    {
        if (!id || !id.sysId)
        {
            return null;
        }

        return this.newInstance(await this.get(id.sysId));
    }

    async getExtendedBatch(ids: Models.RecordIdentity[]): Promise<E[]>
    {
        if (!ids || ids.length == 0)
        {
            return [];
        }

        let rawResults = await this.getBatch(ids);
        let data       = rawResults.map((rawResult) => this.newInstance(rawResult));

        this.afterExtendedBatch(data);

        return data;
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

}

class CachedRecord<M>
{
    ri: Models.RecordIdentity;
    value: M;
}
