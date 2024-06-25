import {Component} from "@angular/core";
import {CacheItem, CacheService, ICacheStore, InMemoryCacheStore, LocalStorageCacheStore} from "framework/services/cache.service";

@Component({
               template: `
                   <o3-wdc></o3-wdc>`,
               selector: "o3-app"
           })
export class AppComponent
{
    constructor(private cacheService: CacheService)
    {
        let store: ICacheStore = new LocalStorageCacheStore("O3");
        try
        {
            let item = new CacheItem();
            item.key = "test";
            store.set("__test__", item);
            store.remove("__test__");
        }
        catch (error)
        {
            // This could happen on Safari in private mode.
            console.error("Failed to set entry in LocalStorage cache, switching to InMemory...");
            store = new InMemoryCacheStore();
        }
        this.cacheService.init(CacheService.createSessionID(), true, store);
    }
}
