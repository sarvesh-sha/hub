import {Injectable} from "@angular/core";
import {ThemePalette} from "@angular/material/core";
import {NavigationEnd, Router} from "@angular/router";
import {Subject} from "rxjs";

/**
 * App service
 */
@Injectable()
export class AppFabsService
{
    /**
     * Collection of active fabs.
     */
    public fabs: Fab[] = [];

    private dragging: boolean = false;

    get fabsHidden(): boolean
    {
        return this.dragging;
    }

    /**
     * Raised when a fab changes occur.
     */
    fabsChanged: Subject<void> = new Subject<void>();

    /**
     * Raised when num fabs to show changes
     */
    maxFabsChanged: Subject<number> = new Subject<number>();

    /**
     * The current url we are on.
     */
    private currentUrl: string;

    constructor(private router: Router)
    {
        // subscribe to route change
        router.events.subscribe((val) =>
                                {
                                    if (val instanceof NavigationEnd)
                                    {
                                        if (val.urlAfterRedirects != this.currentUrl) this.removeAll();
                                        this.currentUrl = val.urlAfterRedirects;
                                    }
                                });
    }

    public add(id: string,
               callback: () => void)
    {
        let fabIcon: string;
        let fabTooltip: string;
        switch (id)
        {
            case "filters":
                fabIcon    = "filter_list";
                fabTooltip = "Configure Table Filters";
                break;

            case "bookmarks":
                fabIcon    = "bookmarks";
                fabTooltip = "Bookmarks";
                break;

            case "dashboard":
                fabIcon    = "dashboard";
                fabTooltip = "";
                break;

            default:
                return;
        }

        this.customAdd(id, fabIcon, fabTooltip, "primary", callback);
    }

    public customAdd(id: string,
                     icon: string,
                     tooltip: string,
                     theme: ThemePalette,
                     callback: Function,
                     toEnd: boolean   = true,
                     enabled: boolean = true)
    {
        if (!id || !callback) return;

        this.remove(id, false);

        let fab = new Fab(id, icon, tooltip, theme, callback, enabled);
        if (toEnd)
        {
            this.fabs.push(fab);
        }
        else
        {
            this.fabs.unshift(fab);
        }

        this.fabsChanged.next();
    }

    public remove(id: string,
                  emitChange: boolean = true)
    {
        for (let i = 0; i < this.fabs.length; i++)
        {
            let fab = this.fabs[i];
            if (fab.id === id)
            {
                this.fabs.splice(i, 1);
                if (emitChange) this.fabsChanged.next();
                break;
            }
        }
    }

    public removeAll()
    {
        this.fabs = [];

        this.fabsChanged.next();
    }

    public enable(id: string)
    {
        let fab = this.find(id);
        if (fab && !fab.isEnabled)
        {
            fab.isEnabled = true;
            this.fabsChanged.next();
        }
    }

    public disable(id: string)
    {
        let fab = this.find(id);
        if (fab)
        {
            fab.isEnabled = false;
            this.fabsChanged.next();
        }
    }

    public disableAll(): Fab[]
    {
        let justDisabled: Fab[] = [];
        for (let fab of this.fabs)
        {
            if (fab.isEnabled)
            {
                fab.isEnabled = false;
                justDisabled.push(fab);
            }
        }

        if (justDisabled.length > 0) this.fabsChanged.next();

        return justDisabled;
    }

    public enableAll(): Fab[]
    {
        let justEnabled: Fab[] = [];
        for (let fab of this.fabs)
        {
            if (!fab.isEnabled)
            {
                fab.isEnabled = true;
                justEnabled.push(fab);
            }
        }

        if (justEnabled.length > 0) this.fabsChanged.next();

        return justEnabled;
    }

    public find(id: string): Fab
    {
        return this.fabs.find((f) => f.id === id);
    }

    public activate(id: string)
    {
        let fab = this.find(id);
        if (fab && !fab.isActive)
        {
            fab.isActive = true;
            this.fabsChanged.next();
        }
    }

    public deactivate(id: string)
    {
        let fab = this.find(id);
        if (fab)
        {
            fab.isActive = false;
            this.fabsChanged.next();
        }
    }

    public deactivateAll(): Fab[]
    {
        let justDeactivated: Fab[] = [];
        for (let fab of this.fabs)
        {
            if (fab.isActive)
            {
                fab.isActive = false;
                justDeactivated.push(fab);
            }
        }

        if (justDeactivated.length > 0) this.fabsChanged.next();

        return justDeactivated;
    }

    public activateAll(): Fab[]
    {
        let justActivated: Fab[] = [];
        for (let fab of this.fabs)
        {
            if (!fab.isActive)
            {
                fab.isActive = true;
                justActivated.push(fab);
            }
        }

        if (justActivated.length > 0) this.fabsChanged.next();

        return justActivated;
    }

    public startingDrag()
    {
        this.dragging = true;
    }

    public endingDrag()
    {
        this.dragging = false;
    }

    public updateMaxNumVisible(maxNumVisible: number = 2)
    {
        this.maxFabsChanged.next(maxNumVisible);
    }

    public get(enabledOnly: boolean = true)
    {
        return this.fabs.filter((f) => !enabledOnly || f.isEnabled);
    }
}

export class Fab
{
    constructor(public id: string,
                public icon: string        = "add",
                public tooltip: string     = "",
                public theme: ThemePalette = "primary",
                public callback: Function,
                public isEnabled: boolean  = true,
                public isActive: boolean   = true)
    { }
}
