import {Injectable} from "@angular/core";
import {Router} from "@angular/router";
import {UUID} from "angular2-uuid";

import {UsersService} from "app/services/domain/users.service";
import * as Models from "app/services/proxy/model/models";

import {Logger, LoggingService} from "framework/services/logging.service";
import {Lookup, UtilsService} from "framework/services/utils.service";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {ViewState, ViewStateService} from "framework/ui/navigation/view-state.service";
import {inParallel, mapInParallel} from "framework/utils/concurrency";

@Injectable()
export class BookmarkService
{
    public static readonly bookmarkParamName: string = "sys_bookmark";

    private readonly logger: Logger;

    private cachedBookmarks: BookmarkCollection;
    private flushTimer: any;

    constructor(private route: Router,
                private vss: ViewStateService,
                private user: UsersService,
                private loggingSvc: LoggingService,
                private utilsService: UtilsService)
    {
        this.logger = this.loggingSvc.getLogger(BookmarkService);
    }

    /**
     * Navigate to the URL/state of this bookmark.
     * @param {BookmarkConfiguration} bookmark
     */
    public async navigateTo(bookmark: Models.BookmarkConfiguration)
    {
        await this.restoreBookmarkView(bookmark.id, bookmark.url);

        // If the current URL doesn't match the bookmark URL, navigate to the bookmark's URL.
        let currentUrl = this.route.routerState.snapshot.url;
        if (currentUrl != bookmark.url)
        {
            await this.vss.navigateToBookmark(bookmark.url);
        }
    }

    /**
     * Generate bookmark for the current page.
     * @param name
     * @param description
     * @param type
     * @param recordID
     * @param parentRecordID
     */
    async generateBookmark(name: string,
                           description: string,
                           type: Models.BookmarkType,
                           recordID: string,
                           parentRecordID?: string): Promise<Models.BookmarkConfiguration>
    {
        return this.getBuildBookmark(name, description, type, recordID, parentRecordID);
    }

    public async importBookmarks(bookmarkSets: BookmarkSet[]): Promise<number>
    {
        if (!Array.isArray(bookmarkSets) || bookmarkSets.length === 0) return 0;

        await this.getBookmarkCollection(); // cache this.cachedBookmarks

        let resolved = await mapInParallel(bookmarkSets, (set) => this.importBookmark(set, false));
        let numAdded = 0;
        for (let added of resolved)
        {
            if (added) numAdded++;
        }

        if (numAdded > 0) await this.saveBookmarks();
        return numAdded;
    }

    private async importBookmark(bookmark: BookmarkSet,
                                 save: boolean = true): Promise<boolean>
    {
        if (!bookmark) return false;

        let parentBookmark   = bookmark.bookmark;
        let importedBookmark = await this.getBuildBookmark(parentBookmark.name,
                                                           parentBookmark.description,
                                                           parentBookmark.type,
                                                           parentBookmark.recordID,
                                                           parentBookmark.parentRecordID,
                                                           parentBookmark.url,
                                                           false);

        if (importedBookmark)
        {
            await inParallel(bookmark.views, (view) =>
            {
                view           = Models.BookmarkConfiguration.newInstance(view);
                view.id        = UUID.UUID();
                view.parentID  = importedBookmark.id;
                view.createdOn = MomentHelper.now()
                                             .toDate();

                return this.updateBookmark(view, false, false);
            });

            if (save && bookmark.views.length > 0) await this.saveBookmarks();
        }

        return !!importedBookmark;
    }

    private async getBuildBookmark(name: string,
                                   description: string,
                                   type: Models.BookmarkType,
                                   recordID: string,
                                   parentRecordID?: string,
                                   url: string              = this.route.routerState.snapshot.url,
                                   saveImmediately: boolean = true): Promise<Models.BookmarkConfiguration>
    {
        let bookmark = await this.getBookmarkByRecordID(recordID);

        if (!bookmark)
        {
            bookmark                = new Models.BookmarkConfiguration();
            bookmark.id             = UUID.UUID();
            bookmark.name           = name;
            bookmark.description    = description;
            bookmark.createdOn      = MomentHelper.now()
                                                  .toDate();
            bookmark.type           = type;
            bookmark.recordID       = recordID;
            bookmark.parentRecordID = parentRecordID;
            bookmark.url            = url;

            // store the viewstate data
            await this.storeBookmark(bookmark, saveImmediately);
        }

        return bookmark;
    }

    private async saveBookmarks()
    {
        await this.storeBookmarkCollection(await this.getBookmarkCollection());
    }


    /**
     * Generate bookmark view for the current page.
     * @param bookmark
     * @param name
     * @param description
     */
    async generateBookmarkView(bookmark: Models.BookmarkConfiguration,
                               name: string,
                               description: string): Promise<Models.BookmarkConfiguration>
    {
        let viewBookmark         = new Models.BookmarkConfiguration();
        viewBookmark.id          = UUID.UUID();
        viewBookmark.parentID    = bookmark.id;
        viewBookmark.name        = name;
        viewBookmark.description = description;

        viewBookmark.url = this.route.routerState.snapshot.url;

        await this.updateBookmark(viewBookmark, true);

        return viewBookmark;
    }

    /**
     * Update a given bookmark view.
     * @param bookmark
     * @param resetViewState
     */
    async updateBookmark(bookmark: Models.BookmarkConfiguration,
                         resetViewState: boolean  = false,
                         saveImmediately: boolean = true): Promise<void>
    {
        if (resetViewState)
        {
            bookmark.createdOn       = MomentHelper.now()
                                                   .toDate();
            bookmark.stateSerialized = this.serialize(this.vss.restore());
        }

        await this.storeBookmark(bookmark, saveImmediately);
    }

    /**
     * Restore a bookmark on given url.
     * @param bookmarkID
     * @param url
     */
    async restoreBookmarkView(bookmarkID: string,
                              url: string): Promise<boolean>
    {
        this.logger.debug(`Restoring bookmark view ${bookmarkID}: ${url}`);

        let bookmark = await this.getBookmarkByID(bookmarkID);
        bookmark     = Models.BookmarkConfiguration.deepClone(bookmark);

        // inject in to view state service
        if (bookmark && bookmark.stateSerialized)
        {
            let viewState = this.deserialize(bookmark.stateSerialized);

            this.vss.setForUrl(viewState, url);
            return true;
        }

        return false;
    }

    private async storeBookmark(bookmark: Models.BookmarkConfiguration,
                                save: boolean = true): Promise<void>
    {
        let collection = await this.getBookmarkCollection();

        collection.add(bookmark);

        if (save) await this.storeBookmarkCollection(collection);
    }

    /**
     * Save the bookmark collection to the user preferences.
     * @param collection
     */
    async storeBookmarkCollection(collection: BookmarkCollection): Promise<void>
    {
        if (collection)
        {
            await this.user.setTypedPreference(null, BookmarkService.bookmarkParamName, collection.filter((bookmark) => true));
        }
    }

    /**
     * Get bookmark by ID.
     * @param id Bookmark to get.
     * @returns Bookmark.
     */
    async getBookmarkByID(id: string): Promise<Models.BookmarkConfiguration>
    {
        let collection = await this.getBookmarkCollection();

        return collection.bookmarks[id];
    }

    /**
     * Get bookmark by record ID.
     * @param recordID Bookmark to get, relative to associated record.
     * @returns Bookmark.
     */
    async getBookmarkByRecordID(recordID: string): Promise<Models.BookmarkConfiguration>
    {
        let collection = await this.getBookmarkCollection();

        let match = collection.filter((b) => b.recordID == recordID && !b.stateSerialized);
        return match.length == 1 ? match[0] : null;
    }

    /**
     * Get bookmarks by parent record ID.
     * @param recordID Bookmark to get, relative to associated record.
     * @returns Bookmarks.
     */
    async getBookmarksByParentRecordID(recordID: string): Promise<Models.BookmarkConfiguration[]>
    {
        let collection = await this.getBookmarkCollection();

        return collection.filter((b) => b.parentRecordID == recordID && !b.stateSerialized);
    }

    /**
     * Get all bookmarks of a given type.
     * @param type The bookmark type.
     * @param skipSort
     * @returns Bookmarks of a given type.
     */
    async getBookmarksOfType(type: Models.BookmarkType,
                             skipSort?: boolean): Promise<Models.BookmarkConfiguration[]>
    {
        let collection = await this.getBookmarkCollection();
        let bookmarks  = collection.filter((b) => b.url && b.type == type && !b.stateSerialized);

        // Sort bookmarks by bookmark name.
        if (!skipSort)
        {
            bookmarks.sort((a,
                            b) => UtilsService.compareStrings(a.name.toUpperCase(), b.name.toUpperCase(), true));
        }

        return bookmarks;
    }

    public async getBookmarkViews(bookmark: Models.BookmarkConfiguration): Promise<Models.BookmarkConfiguration[]>
    {
        let collection = await this.getBookmarkCollection();

        return collection.filter((b) => b.parentID == bookmark.id && !!b.stateSerialized);
    }

    /**
     * Get bookmarks grouped by type.
     */
    async getBookmarkGroups(): Promise<BookmarkGroups>
    {
        let groups = new BookmarkGroups();
        groups.push(Models.BookmarkType.ALERT, "Alert");
        groups.push(Models.BookmarkType.DEVICE_ELEMENT, "Control Point");
        let dataExplorerGroup = groups.push(Models.BookmarkType.DATA_EXPLORER, "Data Explorer");
        groups.push(Models.BookmarkType.DEVICE, "Device");
        groups.push(Models.BookmarkType.EQUIPMENT, "Equipment");

        for (let group of groups.groups)
        {
            let bookmarks = await this.getBookmarksOfType(group.type);
            for (let bookmark of bookmarks)
            {
                let set = group.push(bookmark);

                let views    = await this.getBookmarkViews(bookmark);
                let numViews = views.length;
                for (let i = 0; i < numViews; i++)
                {
                    set.addView(views[i], i === numViews - 1);
                }
            }
        }

        let dataExplorerBookmark = dataExplorerGroup.bookmarks[0];
        if (dataExplorerBookmark?.views.length === 0) dataExplorerGroup.bookmarks = [];

        return groups;
    }

    /**
     * Delete a bookmark for a given ID.
     */
    async deleteBookmark(bookmark: Models.BookmarkConfiguration,
                         save: boolean = true): Promise<void>
    {
        let collection = await this.getBookmarkCollection();

        collection.remove(bookmark);

        if (save) await this.storeBookmarkCollection(collection);
    }

    /**
     * Delete all bookmarks of a given type.
     * @param type
     */
    async deleteBookmarksOfType(type: Models.BookmarkType): Promise<void>
    {
        let bookmarks = await this.getBookmarksOfType(type, true);
        for (let bookmark of bookmarks)
        {
            await this.deleteBookmark(bookmark, false);
        }

        if (bookmarks.length > 0) return await this.storeBookmarkCollection(await this.getBookmarkCollection());
    }

    /**
     * Delete all bookmarks.
     */
    async deleteAllBookmarks(): Promise<void>
    {
        await this.storeBookmarkCollection(new BookmarkCollection());
    }

    //--//

    /**
     * Get all bookmarks in the form of a bookmark collection.
     * @returns Bookmark Collection.
     */
    private async getBookmarkCollection(): Promise<BookmarkCollection>
    {
        if (!this.cachedBookmarks)
        {
            let bookmarks = await this.user.getTypedPreference<Models.BookmarkConfiguration[]>(null, BookmarkService.bookmarkParamName, (v) => v.forEach(Models.BookmarkConfiguration.fixupPrototype));

            this.cachedBookmarks = new BookmarkCollection();

            for (let bookmark of bookmarks || [])
            {
                this.cachedBookmarks.add(bookmark);
            }
        }

        if (this.flushTimer)
        {
            clearTimeout(this.flushTimer);
        }


        this.flushTimer = this.utilsService.setTimeoutOutsideAngular(() =>
                                                                     {
                                                                         this.cachedBookmarks = null;
                                                                         this.flushTimer      = null;
                                                                     }, 10000);

        return this.cachedBookmarks;
    }

    //--//

    public serialize(viewState: ViewState): Models.ViewStateSerialized
    {
        let res = Models.ViewStateSerialized.newInstance(
            {
                state    : {},
                subStates: {}
            }
        );

        for (let key in viewState.state)
        {
            res.state[key] = Models.ViewStateItem.newInstance(viewState.state[key]);
        }

        for (let key in viewState.subStates)
        {
            let subState       = viewState.subStates[key];
            res.subStates[key] = this.serialize(subState);
        }

        return res;
    }

    public deserialize(input: Models.ViewStateSerialized): ViewState
    {
        let res = new ViewState(this.vss);

        for (let key in input.state)
        {
            let srcState = input.state[key];

            res.set(key, srcState.value, srcState.saveInBookmark, false);
        }

        for (let key in input.subStates)
        {
            let subState       = input.subStates[key];
            res.subStates[key] = this.deserialize(subState);
        }

        return res;
    }
}

export class BookmarkCollection
{
    bookmarks: Lookup<Models.BookmarkConfiguration> = {};

    filter(callback: (bookmark: Models.BookmarkConfiguration) => boolean): Models.BookmarkConfiguration[]
    {
        let res = [];

        for (let id in this.bookmarks)
        {
            let bookmark = this.bookmarks[id];

            if (callback(bookmark)) res.push(bookmark);
        }

        return res;
    }

    add(bookmark: Models.BookmarkConfiguration)
    {
        this.bookmarks[bookmark.id] = bookmark;
    }

    remove(bookmark: Models.BookmarkConfiguration)
    {
        delete this.bookmarks[bookmark.id];

        let children = this.filter((b) => b.parentID == bookmark.id);
        for (let child of children)
        {
            this.remove(child);
        }
    }
}

export class BookmarkGroups
{
    groups: BookmarkGroup[] = [];

    get hasAnyBookmarks(): boolean
    {
        return this.groups.some((group) => group.hasBookmarks);
    }

    push(type: Models.BookmarkType,
         name: string): BookmarkGroup
    {
        let group = new BookmarkGroup(type, name);
        this.groups.push(group);

        return group;
    }
}

export class BookmarkGroup
{
    bookmarks: BookmarkSet[] = [];

    constructor(public type: Models.BookmarkType,
                public name: string)
    {
    }

    get hasBookmarks(): boolean
    {
        return this.numBookmarks > 0;
    }

    get numBookmarks(): number
    {
        return this.bookmarks.reduce((cum,
                                      bookmarkSet) => cum + bookmarkSet.numBookmarks, 0);
    }

    push(bookmark: Models.BookmarkConfiguration): BookmarkSet
    {
        let set = new BookmarkSet(bookmark);

        this.bookmarks.push(set);

        return set;
    }
}

export class BookmarkSet
{
    views: Models.BookmarkConfiguration[] = [];

    private m_deleted: boolean = false;
    get deleted(): boolean
    {
        return this.m_deleted;
    }

    get numBookmarks(): number
    {
        if (this.views.length) return this.views.length;
        return this.deleted ? 0 : 1;
    }

    get hasViews(): boolean
    {
        return this.views.length > 0;
    }

    constructor(public readonly bookmark: Models.BookmarkConfiguration)
    {
    }

    static generateBookmarkSets(bookmarksJSON: string): BookmarkSet[]
    {
        let sets: { bookmark: Models.BookmarkConfiguration, views: Models.BookmarkConfiguration[] }[] = JSON.parse(bookmarksJSON);
        if (!sets || !(sets instanceof Array)) return null;

        return sets.filter((set) => set.hasOwnProperty("bookmark") && set.views instanceof Array)
                   .map((set) =>
                        {
                            let typedSet   = new BookmarkSet(Models.BookmarkConfiguration.newInstance(set.bookmark));
                            typedSet.views = set.views.map((view) => Models.BookmarkConfiguration.newInstance(view));
                            return typedSet;
                        });

    }

    addView(bookmark: Models.BookmarkConfiguration,
            sort: boolean = true)
    {
        this.views.push(bookmark);

        if (sort)
        {
            this.views.sort((viewA,
                             viewB) => UtilsService.compareStrings(viewA.name, viewB.name, true) || UtilsService.compareStrings(viewA.description, viewB.description, true));
        }
    }

    bookmarkDeleted(bookmark: Models.BookmarkConfiguration)
    {
        if (this.bookmark === bookmark)
        {
            this.m_deleted = true;
        }
        else
        {
            let viewIdx = this.views.indexOf(bookmark);
            if (viewIdx >= 0)
            {
                this.views.splice(viewIdx, 1);
                this.views = UtilsService.arrayCopy(this.views);
                if (this.views.length === 0) this.m_deleted = true;
            }
        }
    }
}
