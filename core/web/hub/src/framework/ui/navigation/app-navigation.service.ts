import {Location} from "@angular/common";
import {Injectable} from "@angular/core";
import {NavigationEnd, NavigationExtras, NavigationStart, Router} from "@angular/router";

import {Logger, LoggingService} from "framework/services/logging.service";
import {Future} from "framework/utils/concurrency";

import {Subject} from "rxjs";


@Injectable()
export class AppNavigationService
{
    readonly logger: Logger;

    private m_currentRouteParams: any;
    private m_ignoreRouteAsPrevious: boolean = false;

    private m_restoringHistory: NavigationState;
    private m_currentHistory: NavigationState;

    breadcrumbs: BreadcrumbItem[];
    private m_breadcrumbLabel: string;
    breadcrumbLabelChanged: Subject<string> = new Subject<string>();

    set breadcrumbCurrentLabel(val: string)
    {
        this.m_breadcrumbLabel = val;
        this.breadcrumbLabelChanged.next(val);
    }

    get breadcrumbCurrentLabel(): string
    {
        return this.m_breadcrumbLabel;
    }

    constructor(private router: Router,
                private location: Location,
                logService: LoggingService)
    {
        this.logger = logService.getLogger(AppNavigationService);

        // subscribe to route change
        router.events.subscribe((val) =>
                                {
                                    if (val instanceof NavigationStart)
                                    {
                                        if (this.m_restoringHistory === undefined)
                                        {
                                            if (val.restoredState)
                                            {
                                                this.logger.debug(`Back navigation to ${val.url} for ${val.navigationTrigger}`);

                                                //
                                                // This is a Back/Forward kind of navigation.
                                                // Remember the ID of the original navigation, we'll use it to update the history state.
                                                //
                                                let history = this.m_currentHistory?.findId(val.restoredState.navigationId, new Set<number>());
                                                if (!history)
                                                {
                                                    let prev = this.m_currentHistory?.previous;
                                                    let next = this.m_currentHistory?.next;

                                                    if (next?.url == val.url)
                                                    {
                                                        history = next;
                                                    }
                                                    else if (prev?.url == val.url)
                                                    {
                                                        history = prev;
                                                    }
                                                }

                                                this.m_restoringHistory = history;
                                            }
                                        }
                                    }

                                    if (val instanceof NavigationEnd)
                                    {
                                        let history: NavigationState;

                                        if (this.m_restoringHistory)
                                        {
                                            //
                                            // This is the result of a Pop navigation.
                                            // Instead of creating a new entry in the history, update the target one.
                                            //
                                            history     = this.m_restoringHistory;
                                            history.id  = val.id;
                                            history.url = val.urlAfterRedirects;
                                        }
                                        else
                                        {
                                            history                       = new NavigationState();
                                            history.id                    = val.id;
                                            history.url                   = val.urlAfterRedirects;
                                            history.ignoreRouteAsPrevious = this.m_ignoreRouteAsPrevious;

                                            history.previous = this.m_currentHistory;

                                            if (this.m_currentHistory) this.m_currentHistory.next = history;
                                        }

                                        let isBackNavigation    = this.m_currentHistory?.previous == history;
                                        let isForwardNavigation = this.m_currentHistory?.next == history;

                                        this.m_restoringHistory = undefined;
                                        this.m_currentHistory   = history;

                                        history.limitDepth(100);

                                        if (isBackNavigation && history.ignoreRouteAsPrevious && history.previous)
                                        {
                                            this.logger.debug(`Skipping Back navigation to ${history.id} ${history.url}`);

                                            this.location.back();
                                        }

                                        if (isForwardNavigation && history.ignoreRouteAsPrevious)
                                        {
                                            this.logger.debug(`Skipping Forward navigation to ${history.id} ${history.url}`);

                                            this.location.forward();
                                        }
                                    }
                                });
    }

    //#region Navigation

    /**
     * Navigate down the navigation hierarchy by appending url segments and params to the current route.
     * @param segments
     * @param params
     */
    public async push(segments: any[],
                      params?: { param: string, value: any }[])
    {
        await this.go(this.router.url, segments, params);
    }

    /**
     * Navigate back up the navigation hierarchy.
     */
    public async pop()
    {
        // Add a little delay, to allow database change notifications to trickle through.
        await Future.delayed(150);

        let targetHistory = this.m_currentHistory?.previous;
        while (targetHistory && targetHistory.ignoreRouteAsPrevious)
        {
            targetHistory = targetHistory.previous;
        }

        if (targetHistory)
        {
            this.m_restoringHistory = targetHistory;
            await this.router.navigateByUrl(targetHistory.url);
        }
        else
        {
            await this.router.navigateByUrl("/home");
        }
    }

    /**
     * Go to the specific url.
     * @param url
     * @param segments
     * @param params
     * @param extras
     */
    public async go(url: string,
                    segments?: any[],
                    params?: { param: string, value: any }[],
                    extras?: NavigationExtras)
    {
        let formattedUrl = this.formatUrl(url, segments, params);
        this.logger.info(`Navigating to ${formattedUrl}`);

        await this.router.navigateByUrl(formattedUrl, extras);
    }

    /**
     * Format a url given the rool url, additional url segments, and query params
     * @param rootUrl
     * @param segments
     * @param params
     */
    public formatUrl(rootUrl: string,
                     segments?: any[],
                     params?: { param: string, value: any }[])
    {
        let urlSegments = rootUrl.split("/");

        if (segments)
        {
            let encodedSegments: any[] = segments.map((segment) =>
                                                      {
                                                          return encodeURIComponent(<string>segment);
                                                      });

            for (let encodedSegment of encodedSegments)
            {
                if (encodedSegment == "..")
                {
                    urlSegments.pop();
                }
                else
                {
                    urlSegments.push(encodedSegment);
                }
            }

            rootUrl = urlSegments.join("/");
        }
        if (params)
        {
            let encodedParams: string[] = params.filter((param) => !!param.value)
                                                .map((param) =>
                                                     {
                                                         return encodeURIComponent(param.param) + "=" + encodeURIComponent(param.value);
                                                     });
            rootUrl += ";" + encodedParams.join(";");
        }
        return rootUrl;
    }

    //#endregion

    //#region Breadcrumb Management

    /**
     * Set the breadcrumbs to display.
     * @param breadcrumbs
     * @param currentRouteParams
     * @param ignoreRouteAsPrevious
     */
    public setBreadcrumbs(breadcrumbs: BreadcrumbItem[],
                          currentRouteParams: any,
                          ignoreRouteAsPrevious: boolean)
    {
        this.breadcrumbs             = breadcrumbs;
        this.m_currentRouteParams    = currentRouteParams;
        this.m_ignoreRouteAsPrevious = ignoreRouteAsPrevious;
    }

    /**
     * Dynamically add a new breadcrumb to display.
     * @param title
     * @param url
     */
    public addBreadcrumb(title: string,
                         url: string)
    {
        if (title && url) this.breadcrumbs.push(new BreadcrumbItem(title, url));
    }

    /**
     * Navigate to the associated breadcrum url
     * @param item
     * @param params
     */
    public async breadcrumb(item: BreadcrumbItem,
                            params?: { param: string, value: any }[])
    {

        // process url values
        let finalUrl = item.url;
        let segments = item.url.split("/");
        for (let segment of segments)
        {
            if (segment[0] == ":")
            {
                let paramName  = segment.replace(":", "");
                let paramValue = this.m_currentRouteParams[paramName];
                finalUrl       = finalUrl.replace(segment, paramValue);
            }
        }

        // go to breadcrumb url
        await this.go(finalUrl, [], params);
    }

    //#endregion
}

/**
 * Holds information for items in the breadcrumbs
 */
export class BreadcrumbItem
{
    constructor(public title: string,
                public url: string)
    { }
}

class NavigationState
{
    public id: number;
    public url: string;
    public ignoreRouteAsPrevious: boolean;

    public previous: NavigationState;
    public next: NavigationState;

    findId(id: number,
           visited: Set<number>): NavigationState
    {
        if (visited.has(this.id))
        {
            return null;
        }

        if (this.id === id) return this;

        visited.add(this.id);

        if (this.previous)
        {
            let res = this.previous.findId(id, visited);
            if (res) return res;
        }

        if (this.next)
        {
            let res = this.next.findId(id, visited);
            if (res) return res;
        }

        return null;
    }

    limitDepth(depth: number)
    {
        if (depth <= 0)
        {
            this.previous = null;
            return;
        }

        if (this.previous)
        {
            this.previous.limitDepth(depth - 1);
        }
    }
}
