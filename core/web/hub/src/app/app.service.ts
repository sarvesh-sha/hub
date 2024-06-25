import {Component, ErrorHandler, EventEmitter, Injectable, Injector, Type} from "@angular/core";
import {MatDrawerMode} from "@angular/material/sidenav";
import {NavigationEnd, NavigationStart} from "@angular/router";
import {ApiService} from "app/services/domain/api.service";

import {AppDomainContext} from "app/services/domain/domain.module";
import {DetailedApplicationExceptionCode} from "app/services/proxy/model/DetailedApplicationExceptionCode";
import {ControlBindingService} from "app/services/ui/control-binding.service";
import {CssConstantsService} from "app/services/ui/css-constants";
import {ApiClientConfiguration, ErrorResult} from "framework/services/api.client";
import {ErrorLevel, ErrorService, RefreshErrorAction} from "framework/services/error.service";
import {Logger, LoggingService} from "framework/services/logging.service";
import {UtilsService} from "framework/services/utils.service";
import {ComponentContext} from "framework/ui/components";

/**
 * Environment configuration settings.
 */
@Injectable()
export class AppEnvironmentConfiguration
{
    version: string;
    mapsApiKey: string;
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

        this.apiConfig.isNotAuthenticatedError = (code) => code == <any>DetailedApplicationExceptionCode.NOT_AUTHENTICATED;
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
        topnavComponent : null,
        topnavTitle     : "Optio3",
        sidenavOpen     : true,
        sidenavMode     : "over",
        sidenavCollapse : false,
        pageFullscreen  : true,
        initial         : false,
        startPage       : true,
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

    /**
     * Triggers the environment-appropriate report export feature.
     */
    public triggerReport()
    {
        if (this.reportingCaptureEnabled) this.ui.puppeteer.triggerCapture();
        if (this.printTriggerEnabled) this.ui.printing.print();
    }

    public triggerReportFailure()
    {
        if (this.reportingCaptureEnabled) this.ui.puppeteer.triggerFailure();
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
    bindings: ControlBindingService;
    css: CssConstantsService;

    /**
     * A general logger for component / ui logging.
     */
    logger: Logger;

    constructor(inj: Injector)
    {
        super(inj);
        this.domain   = inj.get(AppDomainContext);
        this.bindings = inj.get(ControlBindingService);
        this.css      = inj.get(CssConstantsService);
        this.logger   = inj.get(LoggingService)
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
    topnavComponent: Type<Component>;
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
    constructor(private m_app: AppService)
    {
        super();
    }

    handleError(error: any)
    {
        // Check if we have already handled this error.
        try
        {
            if (error.optio3ErrorHandled) return;

            switch (error.code)
            {
                case "INVALID_ARGUMENT":
                case "NOT_AUTHENTICATED":
                case "NOT_AUTHORIZED":
                case "PERMISSION_ERROR":
                case "UNEXPECTED_ERROR":
                    // Not really a crash.
                    return;
            }

            let rejection = error.rejection;
            if (rejection?.optio3ErrorHandled) return;

            switch (rejection?.code)
            {
                case "INVALID_ARGUMENT":
                case "NOT_AUTHENTICATED":
                case "NOT_AUTHORIZED":
                case "PERMISSION_ERROR":
                case "UNEXPECTED_ERROR":
                    // Not really a crash.
                    return;
            }

            if (rejection?.name == "ChunkLoadError")
            {
                let error = new RefreshErrorAction();
                this.m_app.framework.errors.notify("Failed to load page. Refresh and try again", [error], ErrorLevel.Error);
            }

            let message = error.message || rejection?.message;
            let stack   = error.stack || rejection?.stack;
            if (message || stack)
            {
                let loc  = window.location;
                let page = loc.href;
                let root = `${loc.protocol}//${loc.host}`;
                page     = UtilsService.replaceAll(page, root, "");
                message  = UtilsService.replaceAll(message, root, "<root>");
                stack    = UtilsService.replaceAll(stack, root, "<root>");

                this.reportCrash(page, message, stack);
            }
        }
        catch (subError)
        {
        }

        super.handleError(error);
    }

    readonly m_crashes = new Set<String>();

    private async reportCrash(page: string,
                              message: string,
                              stack: string)
    {
        if (!this.m_crashes.has(stack))
        {
            this.m_crashes.add(stack);

            try
            {
                let api = this.m_app.inject(ApiService);
                await api.adminTasks.reportCrash(page, `Message: ${message}\n\nStack: ${stack}`);
            }
            catch (e)
            {
                // Ignore reporting failures.
            }
        }
    }
}
