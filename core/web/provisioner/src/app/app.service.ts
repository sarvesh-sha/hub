import {ErrorHandler, EventEmitter, Injectable, Injector} from "@angular/core";
import {MatDrawerMode} from "@angular/material/sidenav";
import {NavigationEnd, NavigationStart} from "@angular/router";

import {AppDomainContext} from "app/services/domain";
import {ApiClientConfiguration, ErrorResult} from "framework/services/api.client";
import {ErrorService} from "framework/services/error.service";
import {Logger, LoggingService} from "framework/services/logging.service";
import {ComponentContext} from "framework/ui/components";

/**
 * Environment configuration settings.
 */
@Injectable()
export class AppEnvironmentConfiguration
{
    version: string;
}

/**
 * App service
 */
@Injectable()
export class AppService extends ComponentContext
{
    public readonly stateChanged = new EventEmitter();

    constructor(private inj: Injector,
                public environment: AppEnvironmentConfiguration,
                public apiConfig: ApiClientConfiguration)
    {
        super(inj);

        this.apiConfig.isNotAuthenticatedError = (code) => true;
        this.apiConfig.errorHandler            = (result) => this.errorHandler(result);
        this.apiConfig.redirectToLogin         = (url) => this.redirectToLogin(url);

        // subscribe to route change
        this.routing.router.events.subscribe((val) =>
                                             {
                                                 // if it is a navigation end
                                                 if (val instanceof NavigationStart)
                                                 {
                                                     this.state.reportingCapture = false;
                                                     this.state.printTrigger     = false;

                                                     this.ui.navigation.logger.debug(`Nav starting: ${val.url}`);
                                                 }
                                                 if (val instanceof NavigationEnd)
                                                 {
                                                     let urlParts    = val.urlAfterRedirects.split("/");
                                                     this.state.area = urlParts[1].toLowerCase();

                                                     this.ui.navigation.logger.debug(`Nav complete: ${val.urlAfterRedirects}`);
                                                 }
                                             });
    }

    //#region State

    // Set your states default value.
    private state: InternalStateType = {
        appName         : "Optio3 - IoT under control",
        darkMode        : false,
        defaultLang     : "en",
        topnavTitle     : "Optio3",
        sidenavOpen     : true,
        sidenavMode     : "over",
        sidenavCollapse : false,
        pageFullscreen  : false,
        initial         : false,
        startPage       : false,
        reportingCapture: false,
        printTrigger    : false,
        area            : ""
    };

    /**
     * Get entire states.
     *
     * @return  {InternalStateType} State do not respond to changes.
     */
    public cloneState(): InternalStateType
    {
        return JSON.parse(JSON.stringify(this.state));
    }

    /**
     * Reload entire states.
     *
     * @param state
     */
    public reloadState(state: InternalStateType)
    {
        this.state = state;
    }

    /**
     * Get state by key.
     *
     * @param prop The field to read
     * @return The value of the field
     */
    public getState<K extends keyof InternalStateType>(prop: K): InternalStateType[K]
    {
        return this.state[prop];
    }

    /**
     * Set state by key and value.
     *
     * @param prop State key name.
     * @param value State value.
     */
    public setState<K extends keyof InternalStateType>(prop: K,
                                                       value: InternalStateType[K]): InternalStateType[K]
    {
        if (this.state[prop] != value)
        {
            this.state[prop] = value;

            this.stateChanged.emit();
        }

        return value;
    }

    /**
     * Returns the reporting capture status.
     *
     * @readonly
     */
    get reportingCaptureEnabled(): boolean
    {
        return this.getState("reportingCapture");
    }

    /**
     * Returns the print trigger status.
     *
     * @readonly
     */
    get printTriggerEnabled(): boolean
    {
        return this.getState("printTrigger");
    }

    //#endregion

    public redirectToLogin(url?: string)
    {
        let currentUrl = this.routing.router.url;

        if (!url) url = currentUrl;

        if (!currentUrl.startsWith("/start/login"))
        {
            setTimeout(() => this.routing.router.navigate(["/start/login"], {queryParams: {r: url || currentUrl}}));
        }
    }

    private errorHandler(result: ErrorResult)
    {
        if (result.validationErrors)
        {
            for (let validationError of result.validationErrors.entries)
            {
                console.error(`APP_ERROR: '${validationError.field}' : ${validationError.reason}`);
            }
        }
        else
        {
            console.error(`APP_ERROR: ${result.code} ${result.message}`);

            let notAuthenticated = this.apiConfig.isNotAuthenticatedError && this.apiConfig.isNotAuthenticatedError(result.code);
            if (notAuthenticated)
            {
                if (this.apiConfig.handleNotAuthenticatedError)
                {
                    this.apiConfig.handleNotAuthenticatedError();
                }
            }
        }
    }
}

@Injectable()
export class AppContext extends ComponentContext
{
    domain: AppDomainContext;

    /**
     * A general logger for component / ui logging.
     */
    logger: Logger;

    constructor(inj: Injector)
    {
        super(inj);
        this.domain = inj.get(AppDomainContext);
        this.logger = inj.get(LoggingService)
                         .getLogger(ComponentContext);
    }
}

/**
 * This decorator runs the target async method inside a try/catch and report any error messages coming from the server.
 */
export function ReportError(target: any,
                            propertyName: string,
                            descriptor: TypedPropertyDescriptor<any>)
{
    let method       = descriptor.value;
    descriptor.value = function (this: any)
    {
        let ctx = this;

        if (ctx.svc)
        {
            ctx = ctx.svc;
        }

        let handler: ErrorService;

        if (ctx.app instanceof ComponentContext)
        {
            handler = ctx.app.framework.errors;
        }
        else if (ctx.appService instanceof ComponentContext)
        {
            handler = ctx.appService.framework.errors;
        }
        else if (ctx.errorService instanceof ErrorService)
        {
            handler = ctx.errorService;
        }
        else if (ctx.errors instanceof ErrorService)
        {
            handler = ctx.errors;
        }
        else
        {
            handler = null;
        }

        if (handler)
        {
            return handler.execAndReportErrorFromDecorator(method, this, arguments);
        }
        else
        {
            return method.apply(this, arguments);
        }
    };
}

/**
 * Internal State
 *
 * Declare your custom state key any type here.
 */
export class InternalStateType
{
    [key: string]: any;

    appName: string;
    darkMode: boolean;
    defaultLang: string;
    topnavTitle: string;
    sidenavOpen: boolean;
    sidenavMode: MatDrawerMode;
    sidenavCollapse: boolean;
    pageFullscreen: boolean;
    initial: boolean;
    startPage: boolean;
    reportingCapture: boolean;
    printTrigger: boolean;
    area: string;
}

@Injectable()
export class AppErrorService extends ErrorHandler
{
    constructor()
    {
        super();
    }

    //#region Error

    handleError(error: any)
    {
        // Check if we have already handled this error.
        try
        {
            if (error.optio3ErrorHandled) return;

            let rejection = error.rejection;
            if (rejection?.optio3ErrorHandled) return;
        }
        catch (subError)
        {
        }

        super.handleError(error);
    }

    //#endregion Error
}
