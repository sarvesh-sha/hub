import {Component, Injector} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {BookmarkService} from "app/services/domain/bookmark.service";
import {AppNavigationService} from "framework/ui/navigation/app-navigation.service";

@Component({
               template: ""
           })
export class BookmarkRedirectComponent extends SharedSvc.BaseComponentWithRouter
{
    private bookmarkID: string;

    constructor(inj: Injector,
                private navigation: AppNavigationService,
                private bookmarks: BookmarkService)
    {
        super(inj);
    }

    protected async onNavigationComplete()
    {
        this.bookmarkID = this.getPathParameter("id");

        if (this.bookmarkID)
        {
            await this.evaluateBookmarkViewRequest();
        }
        else
        {
            await this.redirectToDefault();
        }
    }

    private async evaluateBookmarkViewRequest()
    {
        if (this.bookmarkID)
        {
            let bookmark = await this.bookmarks.getBookmarkByID(this.bookmarkID);
            if (bookmark)
            {
                await this.bookmarks.restoreBookmarkView(bookmark.id, bookmark.url);

                await this.navigation.go(bookmark.url, null, null, {replaceUrl: true});
            }
            else
            {
                await this.redirectToDefault();
            }
        }
    }

    private async redirectToDefault()
    {
        await this.navigation.go("home", null, null, {replaceUrl: true});
    }
}
