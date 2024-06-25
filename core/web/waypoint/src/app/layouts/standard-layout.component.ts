import {Component, ContentChildren, Directive, Injector, Input, QueryList} from "@angular/core";

import {AppService} from "app/app.service";

import * as SharedSvc from "app/services/domain/base.service";

import {fromEvent} from "framework/ui/components";

import {debounceTime} from "rxjs/operators";

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
               templateUrl: "./standard-layout.component.html"
           })
export class StandardLayoutComponent extends SharedSvc.BaseApplicationComponent
{
    // Menu
    @ContentChildren(StandardLayoutMenuDirective, {
        descendants: false,
        read       : StandardLayoutMenuDirective
    }) mainMenu: QueryList<StandardLayoutMenuDirective>;

    activeSubMenuName: string;

    // Sidenav
    private m_sidenavOpen: boolean;
    get sidenavOpen(): boolean
    {
        return this.m_sidenavOpen;
    }

    set sidenavOpen(open: boolean)
    {
        this.appService.setState("sidenavOpen", open);
        this.m_sidenavOpen = open;
    }

    constructor(inj: Injector,
                public appService: AppService)

    {
        super(inj);

        this.appService.stateChanged.subscribe(() =>
                                               {
                                                   this.detectChanges();
                                               });
    }

    ngOnInit()
    {
        this.onResize();
    }

    ngAfterViewInit()
    {
        // listen for window resize
        this.subscribeToObservable(fromEvent(window, "resize")
                                       .pipe(debounceTime(250)), () =>
                                   {
                                       this.reportLayoutChange();
                                       setTimeout(() => { this.onResize(); }, 0);
                                   });
    }

    onResize(event?: UIEvent)
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
        this.appService.setState("sidenavOpen", opened);
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
