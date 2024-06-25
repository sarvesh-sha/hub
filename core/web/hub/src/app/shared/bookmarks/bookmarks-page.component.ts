import {Component} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {BookmarkGroups, BookmarkSet} from "app/services/domain/bookmark.service";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {ImportDialogComponent} from "framework/ui/dialogs/import-dialog.component";

@Component({
               selector   : "o3-bookmarks-page",
               templateUrl: "./bookmarks-page.component.html",
               styleUrls  : ["./bookmarks-page.component.scss"]
           })
export class BookmarksPageComponent extends SharedSvc.BaseApplicationComponent
{
    public loading: boolean;
    public bookmarkGroups: BookmarkGroups;

    filterText: string[];

    ngOnInit()
    {
        super.ngOnInit();

        this.reloadGroups();
    }

    async reloadGroups()
    {
        this.loading = true;

        this.bookmarkGroups = await this.app.domain.bookmarks.getBookmarkGroups();
        this.filterText     = [];

        this.loading = false;
    }

    getBookmarkTypeClass(type: Models.BookmarkType): string
    {
        switch (type)
        {
            case Models.BookmarkType.ALERT:
                return "o3-alert";

            case Models.BookmarkType.DEVICE:
            case Models.BookmarkType.DEVICE_ELEMENT:
                return "o3-device";

            case Models.BookmarkType.DATA_EXPLORER:
                return "o3-database-search";

            case Models.BookmarkType.EQUIPMENT:
                return "o3-equipment fa fa-plug";

            default:
                return "";
        }
    }

    exportBookmarks()
    {
        let bookmarkSets: BookmarkSet[] = [];
        for (let group of this.bookmarkGroups.groups)
        {
            for (let set of group.bookmarks)
            {
                bookmarkSets.push(set);
            }
        }

        DownloadDialogComponent.open(this, "Bookmarks Export", DownloadDialogComponent.fileName("bookmarks"), bookmarkSets);
    }

    async importBookmarks()
    {
        let result = await ImportDialogComponent.open(this, "Bookmarks Import", {
            returnRawBlobs: () => false,
            parseFile     : async (contents: string) =>
            {
                try
                {
                    return BookmarkSet.generateBookmarkSets(contents);
                }
                catch (e)
                {
                    return null;
                }
            }
        });

        let numAdded = await this.app.domain.bookmarks.importBookmarks(result);
        if (numAdded > 0)
        {
            await this.reloadGroups();
            this.app.framework.errors.success(UtilsService.pluralize("Imported Bookmark", numAdded) + ".", -1);
        }
    }
}
