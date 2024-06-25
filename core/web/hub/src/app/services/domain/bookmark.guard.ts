import {Injectable, Injector} from "@angular/core";
import {ActivatedRouteSnapshot, CanActivate, CanActivateChild, RouterStateSnapshot} from "@angular/router";
import {BookmarkService} from "app/services/domain/bookmark.service";
import {ViewStateService} from "framework/ui/navigation/view-state.service";
import {Observable, Subscriber} from "rxjs";

@Injectable()
export class BookmarkGuard implements CanActivate,
                                      CanActivateChild
{
    constructor(private inj: Injector,
                private vss: ViewStateService,
                private bookmarks: BookmarkService)
    {

    }

    canActivate(route: ActivatedRouteSnapshot,
                state: RouterStateSnapshot): Observable<boolean>
    {
        return Observable.create((observer: Subscriber<boolean>) => this.evaluateBookmarkViewRequest(observer, route, state.url));
    }

    canActivateChild(route: ActivatedRouteSnapshot,
                     state: RouterStateSnapshot): Observable<boolean>
    {
        return Observable.create((observer: Subscriber<boolean>) => this.evaluateBookmarkViewRequest(observer, route, state.url));
    }

    private async evaluateBookmarkViewRequest(observer: Subscriber<boolean>,
                                              route: ActivatedRouteSnapshot,
                                              url: string)
    {
        try
        {
            let bookmarkID = route.params[BookmarkService.bookmarkParamName];

            if (bookmarkID)
            {
                await this.bookmarks.restoreBookmarkView(bookmarkID, url);
            }
        }
        catch (e)
        {
            // Ignore failures.
        }

        observer.next(true);
    }
}
