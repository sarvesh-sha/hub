import {Injectable} from "@angular/core";
import {UUID} from "angular2-uuid";

@Injectable()
export class CacheService
{
    private store: ICacheStore = null;

    private sessionID: string = null;

    private userID: string = null;

    /**
     * Constructor
     */
    constructor()
    {
        this.store = new InMemoryCacheStore();
    }

    /**
     * Set the Session ID and perform startup logic.
     * @param sessionID
     * @param purgeStaleSessionItems
     * @param store
     */
    async init(sessionID: string,
               purgeStaleSessionItems?: boolean,
               store?: ICacheStore): Promise<boolean>
    {
        this.sessionID = sessionID;
        if (store)
        {
            this.store = store;
        }
        else
        {
            store = this.store;
        }

        // if init supported, initialize the store
        if (this.canInitStore(store))
        {

            try
            {
                await store.init();

                // then validate each item
                this.processNewSession(purgeStaleSessionItems);
            }
            catch (e)
            {
            }
        }
        else
        {
            // otherwise, immediately validate them
            this.processNewSession(purgeStaleSessionItems);
        }

        // then notify anyone waiting
        return true;
    }

    /**
     * Check if the specified store is initializable
     * @param store
     */
    private canInitStore(store: ICacheStore): store is IInitializableCacheStore
    {
        return (store as IInitializableCacheStore).init !== undefined;
    }

    /**
     * Perform operations on start of a new session.
     * @param purgeStaleSessionItems
     */
    private processNewSession(purgeStaleSessionItems: boolean)
    {
        // ensure all items are valid for the current user
        let allItems = this.getAll();

        for (let item of allItems)
        {
            let stale = false;

            // if not associated to current session, mark stale
            if (this.sessionID && item.sessionID && item.sessionID != this.sessionID)
            {
                stale = true;
            }

            // if no session and has session id, mark stale
            else if (!this.sessionID && item.sessionID)
            {
                stale = true;
            }

            // if stale, take approriate policy action
            if (stale)
            {

                if (item.policy == CachePolicy.SessionOnly)
                {
                    this.store.remove(item.key);
                }
                else if (item.policy == CachePolicy.Session && purgeStaleSessionItems)
                {
                    this.store.remove(item.key);
                }
                else
                {
                    item.stale = true;
                    this.store.set(item.key, item);
                }
            }
        }
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
     * Get the value of item in the store.
     * @param key Null if not in the store.
     */
    getValue(key: string): any
    {
        let item = this.get(key);
        return CacheService.getValueAs<any>(item, null);
    }

    /**
     * Get the value as a specified type.
     * @param key
     * @param defaultValue
     */
    getValueAs<T>(key: string,
                  defaultValue: T): T
    {
        let item = this.get(key);
        return CacheService.getValueAs<T>(item, defaultValue);
    }

    /**
     * Wrap an operation in a cache.
     * @param key
     */
    async flush(key: string): Promise<void>
    {
        this.store.remove(key);
    }

    /**
     * Set the item in to the store with the specified cache behavior.
     * @param key
     * @param policy
     * @param value
     */
    set(key: string,
        policy: CachePolicy,
        value: any): CacheItem
    {
        let item      = new CacheItem();
        item.key      = key;
        item.policy   = policy;
        item.stale    = false;
        item.dirty    = false;
        item.value    = value;
        item.cachedOn = new Date();

        if (policy == CachePolicy.Session) item.sessionID = this.sessionID;
        if (policy == CachePolicy.SessionOnly) item.sessionID = this.sessionID;
        if (policy == CachePolicy.User)
        {
            item.userID    = this.userID;
            item.sessionID = this.sessionID;
        }

        this.store.set(key, item);

        return item;
    }

    /**
     * Set an item for the current session (will be available but stale on subsequent sessions).
     * @param key
     * @param value
     */
    setForSession(key: string,
                  value: any): CacheItem
    {
        return this.set(key, CachePolicy.Session, value);
    }

    /**
     * Set an item for the current session only (will not be avialble on subsequent sessions).
     * @param key
     * @param value
     */
    setForSessionOnly(key: string,
                      value: any): CacheItem
    {
        return this.set(key, CachePolicy.SessionOnly, value);
    }

    /**
     * Set an item for the current the current user only.
     * @param key
     * @param value
     */
    setForUser(key: string,
               value: any): CacheItem
    {
        return this.set(key, CachePolicy.User, value);
    }

    /**
     * Set an item permanently.
     * @param key
     * @param value
     */
    setPermanently(key: string,
                   value: any): CacheItem
    {
        return this.set(key, CachePolicy.Permanent, value);
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
     * Mark an item dirty.
     * @param key
     */
    markDirty(key: string)
    {
        let item = this.store.get(key);
        if (item)
        {
            item.dirty = true;
            this.store.set(key, item);
        }
    }

    /**
     * Mark an item stale.
     * @param key
     */
    markStale(key: string)
    {
        let item = this.store.get(key);
        if (item)
        {
            item.stale = true;
            this.store.set(key, item);
        }
    };

    /**
     *
     * @param userID
     */
    setUser(userID: string)
    {

        this.userID = userID;

        // ensure all items are valid for the current user
        let allItems = this.getAll();

        for (let item of allItems)
        {

            // if not associated to current user, remove
            if (this.userID && item.userID && item.userID != this.userID)
            {
                this.store.remove(item.key);
            }

            // if no current user and has user, remove
            else if (!this.userID && item.userID)
            {
                this.store.remove(item.key);
            }
        }
    }

    /**
     * True if an item is valid (not stale or dirty)
     * @param key
     */
    isValid(key: string): boolean
    {
        let item = this.get(key);
        return CacheService.isValid(item);
    }

    /**
     * Get the value as a specified type.
     */
    static getValueAs<T>(item: CacheItem,
                         defaultValue: T): T
    {
        if (item && (item.value as T) != null) return item.value as T;
        return defaultValue;
    }

    private static isValid(item: CacheItem): boolean
    {
        if (!item) return false;
        if (item.stale) return false;
        if (item.dirty) return false;
        return true;
    }

    private static isValidAndNotNull(item: CacheItem): boolean
    {
        return CacheService.isValid(item) && item.value != null;
    }

    /**
     * Create a Unique ID (GUID) for use as a session key.
     */
    static createSessionID(): string
    {
        return UUID.UUID();
    }
}

export interface ICacheStore
{
    get(key: string): CacheItem;

    getAll(filter?: string): CacheItem[];

    set(key: string,
        item: CacheItem);

    remove(key: string);

    removeAll();
}

export interface IInitializableCacheStore extends ICacheStore
{
    init(): Promise<boolean>;
}

export class CacheItem
{

    key: string;

    userID: string;

    sessionID: string;

    policy: CachePolicy;

    dirty: boolean;

    stale: boolean;

    value: any;

    cachedOn: Date;

    lastAccessed: Date;
}

export enum CachePolicy
{

    None,

    Session,

    User,

    SessionOnly,

    Permanent
}

export class InMemoryCacheStore implements ICacheStore
{
    private store: any = {};

    get(key: string): CacheItem
    {
        if (this.store[key])
        {
            let item          = this.store[key] as CacheItem;
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

            if (this.store.hasOwnProperty(prop))
            {
                result.push(this.get(prop));
            }
        }
        return result;
    }

    set(key: string,
        item: CacheItem)
    {
        this.store[key] = item;
    }

    remove(key: string)
    {
        if (this.store[key])
        {
            delete this.store[key];
        }
    }

    removeAll()
    {
        this.store = {};
    }
}

export class LocalStorageCacheStore implements ICacheStore
{
    constructor(private cachePrefix: string)
    {
    }

    private key(key): string
    {
        return this.cachePrefix + "_" + key;
    }

    get(key: string): CacheItem
    {
        let item = this.performGet(key);
        if (item)
        {
            item.lastAccessed = new Date();
            this.set(key, item);
        }

        return item;
    }

    private performGet(key: string): CacheItem
    {
        let itemJson = localStorage[this.key(key)];
        if (itemJson)
        {
            return JSON.parse(itemJson) as CacheItem;
        }

        return null;
    }

    getAll(filter?: string): CacheItem[]
    {
        let result: CacheItem[] = [];

        for (let cacheKey of this.getAllKeys(filter))
        {
            let itemJson = localStorage.getItem(cacheKey);
            if (itemJson) result.push((JSON.parse(itemJson) as CacheItem));
        }

        return result;
    }

    private getAllKeys(filter?: string): string[]
    {
        let result: string[] = [];

        let prefix = this.key(filter || "");

        for (let i = 0; i < localStorage.length; i++)
        {
            let cacheKey = localStorage.key(i);
            if (cacheKey.startsWith(prefix))
            {
                result.push(cacheKey);
            }
        }

        return result;
    }

    set(key: string,
        item: CacheItem)
    {
        this.performSet(key, item, 0);
    }

    private performSet(key: string,
                       item: CacheItem,
                       attempt: number)
    {
        if (attempt < 5)
        {
            try
            {
                localStorage.setItem(this.key(key), JSON.stringify(item));
            }
            catch (e)
            {
                if (this.isQuotaExceeded(e))
                {
                    // age off the least accesed and reattempt performSet
                    this.ageOffLeastAccessed(15);
                    this.performSet(key, item, attempt + 1);
                }
            }
        }
    }

    private isQuotaExceeded(e): boolean
    {
        let quotaExceeded = false;

        if (e)
        {
            if (e.code)
            {
                switch (e.code)
                {
                    case 22:
                        quotaExceeded = true;
                        break;
                    case 1014:
                        // Firefox
                        if (e.name === "NS_ERROR_DOM_QUOTA_REACHED")
                        {
                            quotaExceeded = true;
                        }
                        break;
                }
            }
            else if (e.number === -2147024882)
            {
                // Internet Explorer 8
                quotaExceeded = true;
            }
        }

        return quotaExceeded;
    }

    private ageOffLeastAccessed(percentageToRemove: number)
    {
        // find all with session only or session data and order by last access
        let lastAccessedList = this.getAll()
                                   .filter((item) =>
                                           {
                                               if (item.policy == CachePolicy.Session || item.policy == CachePolicy.SessionOnly) return true;
                                               return false;
                                           })
                                   .sort((itemA,
                                          itemB) =>
                                         {
                                             return this.getLastAccessed(itemA) - this.getLastAccessed(itemB);
                                         });

        // clear X%
        if (lastAccessedList && lastAccessedList.length)
        {
            let countToRemove = Math.floor(lastAccessedList.length * percentageToRemove / 100);

            let removalCandidates = lastAccessedList.slice(0, countToRemove);

            for (let candidate of removalCandidates)
            {
                this.remove(candidate.key);
            }
        }
    }

    private getLastAccessed(item: CacheItem): number
    {
        if (item.lastAccessed) return new Date(item.lastAccessed).getTime();
        if (item.cachedOn) return new Date(item.cachedOn).getTime();
        return 0;
    }

    remove(key: string)
    {
        localStorage.removeItem(this.key(key));
    }

    removeAll()
    {
        for (let key of this.getAllKeys())
        {
            localStorage.removeItem(key);
        }
    }
}

export class DisabledCacheStore implements ICacheStore
{
    constructor(private cachePrefix: string)
    {
    }

    private key(key): string
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

    set(key: string,
        item: CacheItem)
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
