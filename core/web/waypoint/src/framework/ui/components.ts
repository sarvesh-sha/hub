import {Overlay} from "@angular/cdk/overlay";
import {AfterViewInit, ChangeDetectorRef, Directive, EventEmitter, Injectable, Injector, Input, OnDestroy, OnInit, SecurityContext, SimpleChanges, Type} from "@angular/core";
import {DomSanitizer, SafeHtml, SafeResourceUrl} from "@angular/platform-browser";
import {ActivatedRoute, NavigationEnd, NavigationError, NavigationExtras, NavigationStart, Router} from "@angular/router";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {UtilsService} from "framework/services/utils.service";

import {AppFabsService} from "framework/ui/fabs/app-fabs.service";
import {CommonLayout} from "framework/ui/layout";
import {AppNavigationService} from "framework/ui/navigation/app-navigation.service";
import {ViewState, ViewStateService} from "framework/ui/navigation/view-state.service";
import {PrintingService} from "framework/ui/reporting/printing.service";
import {PuppeteerService} from "framework/ui/reporting/puppeteer.service";

import * as rx from "rxjs";
import {FromEventTarget} from "rxjs/internal/observable/fromEvent";
import {mergeMap, takeUntil} from "rxjs/operators";

@Injectable()
export class ComponentCoreContext
{
    constructor(public injector: Injector)
    {

    }
}

@Injectable()
export class ComponentRoutingContext
{
    route: ActivatedRoute;

    router: Router;

    private m_isNavigating = false;

    constructor(public injector: Injector)
    {
        this.route  = injector.get(ActivatedRoute);
        this.router = injector.get(Router);

        this.router.events.subscribe((event) =>
                                     {
                                         if (event instanceof NavigationStart)
                                         {
                                             this.m_isNavigating = true;
                                         }
                                         else if (event instanceof NavigationEnd || event instanceof NavigationError)
                                         {
                                             this.m_isNavigating = false;
                                         }
                                     });
    }

    navigate(commands: any[],
             extras?: NavigationExtras): Promise<boolean>
    {
        return this.router.navigate(commands, extras);
    }

    get isNavigating(): boolean
    {
        return this.m_isNavigating;
    }
}

@Injectable()
export class ComponentFrameworkContext
{
    constructor(public cache: CacheService,
                public errors: ErrorService,
                public utils: UtilsService)
    {
    }
}

@Injectable()
export class ComponentUIFrameworkContext
{
    constructor(public navigation: AppNavigationService,
                public viewstate: ViewStateService,
                public fabs: AppFabsService,
                public puppeteer: PuppeteerService,
                public printing: PrintingService,
                public overlay: Overlay)
    {

    }
}

@Injectable()
export class ComponentContext
{
    core: ComponentCoreContext;

    routing: ComponentRoutingContext;

    framework: ComponentFrameworkContext;

    ui: ComponentUIFrameworkContext;

    constructor(public injector: Injector)
    {
        this.core      = new ComponentCoreContext(injector);
        this.routing   = new ComponentRoutingContext(injector);
        this.framework = injector.get(ComponentFrameworkContext);
        this.ui        = injector.get(ComponentUIFrameworkContext);
    }

    inject<T>(token: Type<T>): T
    {
        return this.injector.get(token);
    }
}

@Directive()
export abstract class BaseComponent implements OnInit,
                                               AfterViewInit,
                                               OnDestroy

{
    private static s_uniqueId              = 0;
    protected readonly uniqueComponentId = BaseComponent.s_uniqueId++;

    private m_cdr: ChangeDetectorRef;

    get cdr(): ChangeDetectorRef
    {
        return this.m_cdr;
    }

    get injector(): Injector
    {
        return this.inj;
    }

    private readonly m_sanitizer: DomSanitizer;

    private m_vss: ViewStateService;

    private viewStateDefaultCache: { [key: string]: any } = {};
    private viewStateCacheReady                           = false;

    @Input() viewStatePrefix: string;

    private eventSubscriptions: rx.Subscription[] = [];

    private observableSubscriptions: rx.Subscription[] = [];

    private m_destroyed: boolean;

    private m_reportChanges: number;

    private m_layout: CommonLayout;

    constructor(private inj: Injector)
    {
        this.m_cdr       = inj.get(ChangeDetectorRef);
        this.m_vss       = inj.get(ViewStateService);
        this.m_sanitizer = inj.get(DomSanitizer);
        this.m_layout    = inj.get(CommonLayout);

        this.subscribeToObservable(this.m_layout.contentSizeChanged, () => this.afterLayoutChange());
    }

    ngOnInit(): void
    {
    }

    ngAfterViewInit(): void
    {
    }

    ngOnDestroy(): void
    {
        this.cancelConfigurationChangesReport();

        this.m_destroyed = true;
        this.m_cdr       = null;

        for (let eventSubscription of this.eventSubscriptions)
        {
            eventSubscription.unsubscribe();
        }

        for (let observableSubscription of this.observableSubscriptions)
        {
            observableSubscription.unsubscribe();
        }
    }

    ngOnChanges(changes: SimpleChanges)
    {
        this.reportConfigurationChanges();
    }

    wasDestroyed(): boolean
    {
        return this.m_destroyed;
    }

    triggerLayoutChangeEvent()
    {
        this.m_layout.contentSizeChanged.next();
    }

    detectChanges()
    {
        this.m_cdr?.detectChanges();
    }

    markForCheck()
    {
        this.m_cdr?.markForCheck();
    }

    private cancelConfigurationChangesReport()
    {
        if (this.m_reportChanges)
        {
            clearTimeout(this.m_reportChanges);
            this.m_reportChanges = undefined;
        }
    }

    protected reportConfigurationChanges()
    {
        this.cancelConfigurationChangesReport();

        this.m_reportChanges = setTimeout(() =>
                                          {
                                              if (!this.m_destroyed) this.afterConfigurationChanges();
                                          }, 10);
    }

    protected afterConfigurationChanges(): void
    {
        // Subclasses should override this method to get notified after configuration changes.
    }

    protected afterLayoutChange(): void
    {
        // Subclasses should override this method to get notified after layout changes.
    }

    protected reportLayoutChange()
    {
        this.m_layout.contentSizeChanged.next();
    }

    //--//

    protected inject<T>(token: Type<T>): T
    {
        return this.inj.get(token);
    }

    //--//

    protected persistViewStateReady()
    {
        this.viewStateCacheReady = true;
    }

    public handleViewStateGetter(key: string)
    {
        if (!this.viewStateCacheReady)
        {
            throw `PersistViewState not properly initialized, did you forget to call "persistViewStateReady()?`;
        }

        return this.getViewStateValue(key, this.viewStateDefaultCache[key]);
    }

    public handleViewStateSetter(key: string,
                                 newVal: any,
                                 saveInBookmark: boolean)
    {
        // If we are still executing the constructor, we don't want to update the view state.
        if (!this.viewStateCacheReady)
        {
            this.viewStateDefaultCache[key] = newVal;
        }
        else
        {
            this.setViewStateValue(key, newVal, saveInBookmark);
        }
    }

    //--//

    public getViewState(): ViewState
    {
        let viewstate = this.m_vss.restore();
        if (this.viewStatePrefix)
        {
            return viewstate.getSubView(this.viewStatePrefix, true);
        }
        else
        {
            return viewstate;
        }
    }

    protected setViewStateValue(key: string,
                                value: any,
                                saveInBookmark: boolean = true)
    {
        let viewState = this.getViewState();
        viewState.set(key, value, saveInBookmark);
    }

    protected getViewStateValue<T>(key: string,
                                   defaultValue: T = null): T
    {
        let viewState = this.getViewState();
        return viewState.get(key, defaultValue);
    }

    protected removeViewStateKey(key: string)
    {
        let viewState = this.getViewState();
        viewState.remove(key);
    }

    protected removeViewStateKeys(prefix: string)
    {
        let viewState = this.getViewState();
        viewState.removeKeys(prefix);
    }

    //--//

    protected subscribeToEvent<T>(emitter: EventEmitter<T>,
                                  generatorOrNext?: (val: T) => void,
                                  error?: (err: any) => void,
                                  complete?: () => void): any
    {
        if (this.m_destroyed) return null;

        if (!emitter || !generatorOrNext) return null;

        let sub = emitter.subscribe(generatorOrNext, error, complete);
        this.eventSubscriptions.push(sub);
        return sub;
    }

    protected subscribeToObservable<T>(emitter: rx.Observable<T>,
                                       generatorOrNext?: (val: T) => void,
                                       error?: (err: any) => void,
                                       complete?: () => void): rx.Subscription
    {
        if (this.m_destroyed) return null;

        if (!emitter || !generatorOrNext) return null;

        let sub = emitter.subscribe(generatorOrNext, error, complete);
        this.observableSubscriptions.push(sub);
        return sub;
    }

    //--//

    protected subscribeToMouseDrag(target: HTMLElement,
                                   callback: (e: MouseEvent,
                                              mouseDown: boolean,
                                              mouseUp: boolean) => void)
    {
        const mousedown = fromEvent(target, "mousedown");

        const mousemove = fromEvent(document.documentElement, "mousemove");

        const mouseup = fromEvent(document.documentElement, "mouseup");

        const mousedrag = mousedown.pipe(mergeMap((e: MouseEvent) =>
                                                  {
                                                      callback(e, true, false);

                                                      return mousemove.pipe(takeUntil(mouseup));
                                                  }));

        this.subscribeToObservable(mousedrag, (e: MouseEvent) =>
        {
            callback(e, false, false);
        });

        this.subscribeToObservable(mouseup, (e: MouseEvent) =>
        {
            callback(e, false, true);
        });
    }

    protected subscribeToTouchDrag(target: HTMLElement,
                                   callback: (e: TouchEvent,
                                              touchStat: boolean,
                                              touchEnd: boolean) => void)
    {
        const touchstart = fromEvent(target, "touchstart");

        const touchmove = fromEvent(document.documentElement, "touchmove");

        const touchend = fromEvent(document.documentElement, "touchend");

        const touchdrag = touchstart.pipe(mergeMap((e: TouchEvent) =>
                                                   {
                                                       callback(e, true, false);

                                                       return touchmove.pipe(takeUntil(touchend));
                                                   }));

        this.subscribeToObservable(touchdrag, (e: TouchEvent) =>
        {
            callback(e, false, false);
        });

        this.subscribeToObservable(touchend, (e: TouchEvent) =>
        {
            callback(e, false, true);
        });
    }

    //--//

    protected sanitizeHtml(html: string): SafeHtml
    {
        return this.m_sanitizer.sanitize(SecurityContext.HTML, html);
    }

    protected bypassSecurityTrustHtml(html: string): SafeHtml
    {
        return this.m_sanitizer.bypassSecurityTrustHtml(html);
    }

    protected bypassSecurityTrustResourceUrl(html: string): SafeResourceUrl
    {
        return this.m_sanitizer.bypassSecurityTrustResourceUrl(html);
    }

    //--//

    protected matchFilter(target: string,
                          value: string): boolean
    {
        if (!target || !value) return false;

        value = value.toLocaleLowerCase();

        return value ? value.indexOf(target) >= 0 : false;
    }
}

/**
 * This decorator persists and restores the property value from the component viewstate. Property must
 * be associated with a class deriving BaseComponent or exposing a "viewstate" property.
 */
export function PersistViewState(saveInBookmark: boolean = true,
                                 fixupFn?: (instance: BaseComponent,
                                            item: any) => void)
{
    return function (target: any,
                     key: string)
    {
        // Took inspiration from the implementation documented here
        // http://blog.wolksoftware.com/decorators-metadata-reflection-in-typescript-from-novice-to-expert-part-ii

        if (!(target instanceof BaseComponent))
        {
            throw `PersistViewState can only be applied to BaseComponent objects: found on property ${key} of ${target.constructor}`;
        }

        let prev: any;

        // property getter
        let getter = function (this: BaseComponent)
        {
            let value = this.handleViewStateGetter(key);

            if (fixupFn && prev !== value)
            {
                prev = value;
                fixupFn(this, value);
            }

            return value;
        };

        // property setter
        let setter = function (this: BaseComponent,
                               newVal: any)
        {
            return this.handleViewStateSetter(key, newVal, saveInBookmark);
        };

        // Delete property.
        if (delete (<any>target)[key])
        {
            // Create new property with getter and setter
            Object.defineProperty(target, key, {
                get         : getter,
                set         : setter,
                enumerable  : true,
                configurable: true
            });
        }
    };
}

export function fromEvent<K extends keyof WindowEventMap>(target: FromEventTarget<any>,
                                                          type: K): rx.Observable<WindowEventMap[K]>
{
    return rx.fromEvent(target, type);
}

