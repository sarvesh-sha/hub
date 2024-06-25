import {Injectable} from "@angular/core";

import {Logger, LoggingService} from "framework/services/logging.service";
import {Lookup} from "framework/services/utils.service";

@Injectable()
export class CacheService
{
    private readonly logger: Logger;

    private readonly store: ICacheStore;

    /**
     * Constructor
     */
    constructor(private logService: LoggingService,
                private loggingService: LoggingService)
    {
        this.logger = logService.getLogger(CacheService);

        this.store = new InMemoryCacheStore(loggingService);
    }

    /**
     * Perform startup logic.
     */
    init()
    {
        this.logger.info("Initializing Cache");

        this.store.removeAll();
    }

    /**
     * Get an item from the store.
     * @param key
     */
    get(key: string): CacheItem
    {
        return this.store.get(key);
    }

    /**
     * Get all items in store.
     */
    getAll(filter?: string): CacheItem[]
    {
        return this.store.getAll(filter);
    }

    /**
     * Get the value as a specified type.
     * @param key
     * @param defaultValue
     */
    getValueAs<T>(key: string,
                  defaultValue?: T): T
    {
        let item  = this.get(key);
        let value = item?.getValue<T>();
        return value ?? defaultValue;
    }

    /**
     * Wrap an operation in a cache.
     * @param key
     */
    flush(key: string): void
    {
        this.store.remove(key);
    }

    /**
     * Set the item in to the store with the specified cache behavior.
     * @param key
     * @param value
     */
    set(key: string,
        value: any)
    {
        if (!value)
        {
            this.remove(key);
        }
        else
        {
            let item = new CacheItem(key, value);

            this.logger.debug(`Caching Item (${item.key})`);

            this.store.set(item);
        }
    }

    /**
     * Remove the specified item from the store.
     * @param key
     */
    remove(key: string)
    {
        this.store.remove(key);
    }

    /**
     * Remove all items from the store.
     */
    removeAll()
    {
        this.store.removeAll();
    }

    /**
     * True if an item is valid (not stale or dirty)
     * @param key
     */
    isValid(key: string): boolean
    {
        return this.get(key) != null;
    }

    /**
     * Get the value as a specified type.
     */
    static getValueAs<T>(item: CacheItem,
                         defaultValue: T): T
    {
        return item ? <T>item.getValue() : defaultValue;
    }
}

export interface ICacheStore
{
    get(key: string): CacheItem;

    getAll(filter?: string): CacheItem[];

    set(item: CacheItem): void;

    remove(key: string): void;

    removeAll(): void;
}

export class CacheItem
{
    private readonly valueJson: string;

    readonly cachedOn = new Date();

    lastAccessed: Date;

    constructor(readonly key: string,
                value: any)
    {
        this.lastAccessed = this.cachedOn;

        this.valueJson = JSON.stringify(value);
    }

    get size(): number
    {
        return this.valueJson.length;
    }

    public getValue<T>(): T
    {
        return <T>JSON.parse(this.valueJson);
    }
}

export class InMemoryCacheStore implements ICacheStore
{
    private static cacheThreshold: number = 512 * 1024; // Maximum size of an item in the cache.
    private static cacheMaxSize: number   = 64 * 1024 * 1024; // Maximum size of the cache.

    private store: Lookup<CacheItem> = {};
    private currentSize              = 0;

    private readonly logger: Logger;

    constructor(private loggingService: LoggingService)
    {
        this.logger = loggingService.getLogger(InMemoryCacheStore);
    }

    get(key: string): CacheItem
    {
        let item = this.store[key];
        if (item)
        {
            item.lastAccessed = new Date();
            return item;
        }

        return null;
    }

    getAll(filter?: string): CacheItem[]
    {
        let result: CacheItem[] = [];
        for (let prop in this.store)
        {
            if (filter && !prop.startsWith(filter))
            {
                continue;
            }

            result.push(this.get(prop));
        }
        return result;
    }

    set(item: CacheItem)
    {
        this.remove(item.key);

        let size = item.size;
        if (size > InMemoryCacheStore.cacheThreshold)
        {
            this.logger.debug(`Skipping caching item (${item.key}) due to size: ${size}`);
            return;
        }

        item.lastAccessed    = new Date();
        this.store[item.key] = item;
        this.currentSize += size;

        if (this.currentSize > InMemoryCacheStore.cacheMaxSize)
        {
            this.ageOffLeastAccessed(80);
        }
    }

    remove(key: string)
    {
        let oldItem = this.store[key];
        if (oldItem)
        {
            delete this.store[key];

            this.currentSize -= oldItem.size;
        }
    }

    removeAll()
    {
        this.store       = {};
        this.currentSize = 0;
    }

    private ageOffLeastAccessed(percentageToKeep: number)
    {
        // find all with session only or session data and order by last access
        let lastAccessedList = this.getAll()
                                   .sort((itemA,
                                          itemB) => itemA.lastAccessed.getTime() - itemB.lastAccessed.getTime());

        // clear X%
        let targetSize = InMemoryCacheStore.cacheMaxSize * percentageToKeep / 100;
        let removed    = 0;
        let kept       = 0;

        for (let candidate of lastAccessedList)
        {
            if (this.currentSize > targetSize)
            {
                this.remove(candidate.key);
                removed++;
            }
            else
            {
                kept++;
            }
        }

        this.logger.debug(`Removed cached items: ${removed}`);
        this.logger.debug(`Remaining cached items: ${kept}`);
    }
}

export class DisabledCacheStore implements ICacheStore
{
    constructor(private cachePrefix: string)
    {
    }

    private key(key: string): string
    {
        return this.cachePrefix + "_" + key;
    }

    get(key: string): CacheItem
    {
        return null;
    }

    getAll(filter?: string): CacheItem[]
    {
        return [];
    }

    set(item: CacheItem)
    {
        // does nothing
    }

    remove(key: string)
    {
        // does nothing
    }

    removeAll()
    {
        // does nothing
    }
}
