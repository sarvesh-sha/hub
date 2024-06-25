import {Component, ComponentFactoryResolver, ComponentRef, ContentChildren, Directive, ElementRef, Injector, Input, QueryList, Type, ViewChild, ViewChildren, ViewContainerRef} from "@angular/core";

import {AppService} from "app/app.service";

import * as SharedSvc from "app/services/domain/base.service";
import {MessageBusService} from "app/services/domain/message-bus.service";
import {SearchResult, SearchResultType} from "app/services/domain/search.service";

import * as Models from "app/services/proxy/model/models";
import {SearchBindingService} from "app/services/ui/search-binding.service";
//import {DeviceElementSearchFiltersComponent} from "app/shared/search/device-element-search-filters.component";
import {fromEvent} from "framework/ui/components";
import {ErrorListPaneComponent} from "framework/ui/errors/error-list-pane.component";
import {LogoService} from "framework/ui/logo/logo.service";

import {of, Subject} from "rxjs";
import {debounceTime, delay, flatMap} from "rxjs/operators";

export const NAVBAR_HEIGHT: number = 56;

@Directive({
               selector: "o3-standard-layout-menu[id] o3-standard-layout-menu[label]"
           })
export class StandardLayoutMenuDirective
{
    @ContentChildren(StandardLayoutMenuDirective) sub: QueryList<StandardLayoutMenuDirective>;

    @Input() id: string;

    @Input() label: string;

    @Input() faIcon: string;
    @Input() link: string;
    @Input() exact: boolean;
    @Input() roles: string[];
}

@Component({
               selector   : "o3-standard-layout",
               templateUrl: "./standard-layout.component.html",
               styleUrls  : ["./standard-layout.component.scss"]
           })
export class StandardLayoutComponent extends SharedSvc.BaseApplicationComponent
{
    // Menu
    @ContentChildren(StandardLayoutMenuDirective,
                     {
                         descendants: false,
                         read       : StandardLayoutMenuDirective
                     }) mainMenu: QueryList<StandardLayoutMenuDirective>;

    activeSubMenuName: string;

    topnavComponentRef: ComponentRef<Component>;
    topnavComponentType: Type<Component>;

    // Search
    showSearch: boolean;
    searchText: string;
    searchResults: SearchResult[]  = [];
    searchTrigger: Subject<string> = new Subject<string>();
    searching: boolean             = false;
    searchSubmitted: boolean       = false;

    // // Messages
    // maxMessageCount: number = 9;
    // newMessages: number     = 0;

    // Sidenav
    get sidenavOpen(): boolean
    {
        return this.appService.getState("sidenavOpen");
    }

    set sidenavOpen(open: boolean)
    {
        this.appService.setState("sidenavOpen", open);
    }

    @ViewChild("searchInput") searchInput: ElementRef;
    @ViewChild("topnavComponent", {read: ViewContainerRef}) componentContainer: ViewContainerRef;

    // private messageSubscription: SharedSvc.DbChangeSubscription<Models.UserMessage>;

    constructor(inj: Injector,
                public appService: AppService,
                public search: SearchBindingService,
                public logo: LogoService,
                private componentFactoryResolver: ComponentFactoryResolver,
                mb: MessageBusService)
    {
        super(inj);

        this.subscribeToObservable(this.appService.stateChanged, () => this.checkForChanges());

        this.subscribeToObservable(mb.maxReconnectExceeded, () => this.signOut());
    }

    ngOnInit()
    {
        this.onResize();
        // this.messageSubscription = this.subscribeAny(this.app.domain.userMessages, () => this.updateMessageCount());
        // this.updateMessageCount();
    }

    ngAfterViewInit()
    {
        // wire up search trigger
        this.searchTrigger
            .pipe(debounceTime(250), flatMap((search) =>
                                             {
                                                 return of(search)
                                                     .pipe(delay(100));
                                             }))
            .subscribe((searchText) =>
                       {
                           if (searchText) this.performSearch(searchText);
                       });

        // listen for window resize
        this.subscribeToObservable(fromEvent(window, "resize")
                                       .pipe(debounceTime(250)), () =>
                                   {
                                       this.reportLayoutChange();
                                       setTimeout(() => this.onResize());
                                   });

        this.checkForChanges();
    }

    public ngOnDestroy()
    {
        super.ngOnDestroy();
        // this.removeSubscription(this.messageSubscription);
        // this.messageSubscription = null;
        // this.newMessages         = 0;
    }

    private checkForChanges()
    {
        this.detectChanges();

        let topnavComponent = this.appService.getState("topnavComponent");

        if (this.topnavComponentType != topnavComponent)
        {
            this.topnavComponentRef = null;
            this.componentContainer.clear();

            if (topnavComponent)
            {
                let factory             = this.componentFactoryResolver.resolveComponentFactory(topnavComponent);
                this.topnavComponentRef = this.componentContainer.createComponent(factory);
            }

            this.topnavComponentType = topnavComponent;
            this.detectChanges();
        }
    }

    // async updateMessageCount()
    // {
    //     let filters = Models.UserMessageFilterRequest.newInstance({flagNew: true});
    //
    //     this.newMessages = await this.app.domain.userMessages.getCount(filters);
    // }

    async signOut()
    {
        try
        {
            await this.app.domain.users.logout();
        }
        catch (e)
        {
            // Ignore failures.
        }

        this.app.routing.navigate(["/start/login"]);
    }

    // messageCenter()
    // {
    //     this.app.routing.navigate(["./message-center"]);
    // }

    profile()
    {
        this.app.routing.navigate(["./user/profile"]);
    }

    // bookmarks()
    // {
    //     this.app.routing.navigate(["./bookmarks"]);
    // }

    changePassword()
    {
        this.app.routing.navigate(["./user/change-password"]);
    }

    swagger()
    {
        window.open("/swagger-ui/", "_blank");
    }

    onResize()
    {
        let bodyWidth: number = document.body.clientWidth;
        if (bodyWidth > 960)
        {
            if (this.appService.getState("sidenavMode") !== "side")
            {
                this.appService.setState("sidenavMode", "side");
                this.sidenavOpen = true;
            }
        }
        else if (bodyWidth <= 960 && bodyWidth > 600)
        {
            this.appService.setState("sidenavMode", "push");
            this.sidenavOpen = false;
        }
        else if (bodyWidth <= 600)
        {
            this.appService.setState("sidenavMode", "over");
            this.sidenavOpen = false;
        }
    }

    toggleTopnavSearch()
    {
        this.showSearch = !this.showSearch;

        if (this.showSearch) setTimeout(() => this.searchInput.nativeElement.focus(), 50);
    }

    onSearchTextBlur()
    {
        this.showSearch = false;

        // delay slightly more so that model change event has time
        // to process and search result can be navigated to
        setTimeout(() =>
                   {
                       this.searchText    = null;
                       this.searchResults = [];
                   }, 250);
    }

    async onSearchTextChanged(searchText: string)
    {
        this.searchSubmitted = false;

        this.searchTrigger.next(searchText);
    }

    async navigateToSearchResult(url: string)
    {
        if (!url) return;

        // clear the search text so the url doesn't show in the box after selected
        this.searchText    = null;
        this.searchResults = [];
        this.showSearch    = false;
        await this.app.routing.navigate([url]);
    }

    async submitSearch()
    {
        // start the search
        let searchArea = this.appService.getState("area");
        await this.search.searchAndGroup(Models.SearchRequest.newInstance({
                                                                              query  : this.searchText,
//                                                                              filters: [DeviceElementSearchFiltersComponent.getDefaultFilters()]
                                                                          }), searchArea);

        // hide/clear the top nav search bar
        this.searchSubmitted = true;
        this.searchText      = null;
        this.searchResults   = [];
        this.showSearch      = false;

        // go to the results page
        this.app.ui.navigation.go("/search");
    }

    async performSearch(searchText: string)
    {
        if (!this.searchSubmitted)
        {
            let searchArea                 = this.appService.getState("area");
            let resultsPerCategory: number = 3;

            this.searching = true;

            let results = await this.search.searchAndLimit(Models.SearchRequest.newInstance({
                                                                                                query  : searchText,
//                                                                                                filters: [DeviceElementSearchFiltersComponent.getDefaultFilters()]
                                                                                            }), resultsPerCategory, searchArea);
            if (!this.searchSubmitted)
            {
                this.searchResults = results;
                if (results.length > 0)
                {
                    this.searchResults.unshift({
                                                   type: SearchResultType.SEARCHALL,
                                                   text: "See All Results",
                                                   url : "/search",
                                                   id  : "SEARCHALL"
                                               });
                }
                else
                {
                    this.searchResults.unshift({
                                                   type: SearchResultType.SEARCHALL,
                                                   text: "No Results Returned",
                                                   url : null,
                                                   id  : "SEARCHALL"
                                               });
                }
            }
            this.searching = false;
        }
    }

    toggleSidenavCollapse()
    {
        if (this.appService.getState("sidenavCollapse"))
        {
            this.resizeSidenav();
        }
    }

    toggleSidenav()
    {
        this.sidenavOpen = !this.appService.getState("sidenavOpen");
    }

    updateSidenavOpenState(opened: boolean)
    {
        this.reportLayoutChange();
        this.sidenavOpen = opened;
    }

    resizeSidenav()
    {
        if (this.appService.getState("sidenavMode") === "side")
        {
            let resizeEvent = document.createEvent("HTMLEvents");
            resizeEvent.initEvent("resize", true, true);
            document.dispatchEvent(resizeEvent);
        }
    }

    toggleSidenavMenu(menuName: string,
                      isSub: boolean,
                      isParent: boolean)
    {
        if (isParent)
        {
            if (this.activeSubMenuName === menuName)
            {
                this.activeSubMenuName = null;
            }
            else
            {
                this.activeSubMenuName = menuName;
            }
            return;
        }

        if (isSub)
        {
            if (this.appService.getState("sidenavMode") === "push" || this.appService.getState("sidenavMode") === "over")
            {
                this.toggleSidenav();
            }
            return;
        }

        // bp 092717 - only reset active sub menu if manually closed
        //this.activeSubMenuName = null;

        if (this.appService.getState("sidenavMode") === "push" || this.appService.getState("sidenavMode") === "over")
        {
            this.toggleSidenav();
        }
    }
}
