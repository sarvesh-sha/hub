import {Directive, Injector} from "@angular/core";
import {SafeHtml} from "@angular/platform-browser";
import {NavigationEnd} from "@angular/router";
import {AppContext} from "app/app.service";
import {BaseComponent} from "framework/ui/components";
import {DialogConfirmComponent} from "framework/ui/dialogs/dialog-confirm.component";
import {Subscription} from "rxjs";

/**
 * This class is only used as a tag for a logger common to all data services.
 */
class CommonServiceLog
{
}

/**
 * This class is only used as a tag for a logger used for cache interactions.
 */
class CacheServiceLog
{
}

@Directive()
export abstract class BaseApplicationComponent extends BaseComponent
{
    app: AppContext;

    private static readonly c_datatableConfig = "datatable-config";

    constructor(inj: Injector)
    {
        super(inj);

        this.app = new AppContext(inj);
    }

    //--//

    protected async confirmOperation(text: string | SafeHtml): Promise<boolean>
    {
        return DialogConfirmComponent.execute(this, "Are you Sure?", text);
    }
}

@Directive()
export abstract class BaseComponentWithRouter extends BaseApplicationComponent
{
    private readonly m_eventsSub: Subscription;

    private m_afterViewCalled = false;
    private m_navigationEnded = false;

    constructor(inj: Injector)
    {
        super(inj);

        this.m_eventsSub = this.app.routing.router.events.subscribe((event) =>
                                                                    {
                                                                        if (event instanceof NavigationEnd)
                                                                        {
                                                                            this.m_navigationEnded = true;
                                                                            this.checkInitializationDone();
                                                                        }
                                                                    });
    }

    public ngAfterViewInit()
    {
        super.ngAfterViewInit();

        this.m_afterViewCalled = true;
        this.checkInitializationDone();
    }

    public ngOnDestroy(): void
    {
        super.ngOnDestroy();

        this.m_eventsSub.unsubscribe();
    }

    private checkInitializationDone()
    {
        this.detectChanges();

        if (this.m_afterViewCalled && this.m_navigationEnded)
        {
            this.onNavigationComplete();
            this.detectChanges();
        }
    }

    protected getPathParameter(name: string): any
    {
        return this.app.routing.route.snapshot.params[name];
    }

    protected getQueryParameter(name: string): any
    {
        return this.app.routing.route.snapshot.queryParams[name];
    }

    protected getDataParameter(name: string): any
    {
        return this.app.routing.route.snapshot.data[name];
    }

    protected abstract onNavigationComplete(): void;
}

export class ActionDescriptor
{
    name: string;

    tooltip: string;

    execute: () => Promise<void>;

    static add(list: ActionDescriptor[],
               name: string,
               tooltip: string,
               execute: () => Promise<void>)
    {
        let ad     = new ActionDescriptor();
        ad.name    = name;
        ad.tooltip = tooltip;
        ad.execute = execute;

        list.push(ad);
    }
}
