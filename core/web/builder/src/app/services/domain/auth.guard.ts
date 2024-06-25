import {Injectable, Injector, Type} from "@angular/core";
import {ActivatedRouteSnapshot, CanActivate, CanActivateChild, CanLoad, Route, RouterStateSnapshot, UrlSegment} from "@angular/router";

import {AppService} from "app/app.service";
import {AppDomainContext} from "app/services/domain/index";
import {SingleSignOnCredentials} from "app/services/domain/users.service";
import {ApiClient} from "framework/services/api.client";
import {BaseComponent} from "framework/ui/components";


@Injectable()
export class AuthGuard implements CanActivate,
                                  CanActivateChild,
                                  CanLoad
{
    private appService: AppService;

    constructor(private domain: AppDomainContext,
                private apiClient: ApiClient,
                inj: Injector)
    {
        this.appService = inj.get(AppService);
    }

    canActivate(route: ActivatedRouteSnapshot,
                state: RouterStateSnapshot): Promise<boolean>
    {
        return this.canProceed(route, state.url);
    }

    canActivateChild(route: ActivatedRouteSnapshot,
                     state: RouterStateSnapshot): Promise<boolean>
    {
        return this.canProceed(route, state.url);
    }

    public async canLoad(route: Route,
                         segments: UrlSegment[])
    {
        let username: string;
        let password: string;
        let sessionToken: string;

        for (let segment of segments)
        {
            username     = username || segment.parameters["sys_username"];
            password     = password || segment.parameters["sys_password"];
            sessionToken = sessionToken || segment.parameters["sys_sessiontoken"];
        }

        await this.handleAutoLogin(username, password, sessionToken);

        if (segments.length > 0 && segments[0].path === "start")
        {
            return true;
        }

        if (this.domain.users.isAuthenticated)
        {
            return true;
        }
        else
        {
            this.queueNavigationToLogin("/" + segments.join("/"));
            return false;
        }
    }

    private async canProceed(route: ActivatedRouteSnapshot,
                             url: string): Promise<boolean>
    {
        try
        {
            let nextAction = await this.computeNextAction(route, url);
            switch (nextAction)
            {
                case NextAction.ProceedAndResetState:
                    this.appService.setState("startPage", false);
                    this.appService.setState("pageFullscreen", false);
                    return true;

                case NextAction.Proceed:
                    return true;

                default:
                    this.queueNavigationToLogin(url);
                    return false;
            }
        }
        catch (e)
        {
            this.queueNavigationToLogin(url);
            return false;
        }
    }

    private queueNavigationToLogin(url: string)
    {
        this.appService.redirectToLogin(url);
    }

    private async computeNextAction(route: ActivatedRouteSnapshot,
                                    url: string): Promise<NextAction>
    {
        let username: string;
        let password: string;
        let sessionToken: string;

        let routeLeaf = route;
        while (true)
        {
            username     = username || routeLeaf.params["sys_username"];
            password     = password || routeLeaf.params["sys_password"];
            sessionToken = sessionToken || routeLeaf.params["sys_sessiontoken"];

            if (routeLeaf.children && routeLeaf.children.length == 1)
            {
                routeLeaf = routeLeaf.children[0];
                continue;
            }

            break;
        }

        await this.handleAutoLogin(username, password, sessionToken);

        if (!this.domain.users.isAuthenticated)
        {
            let cfg = AuthorizationContext.lookup(routeLeaf.component);
            if (cfg && cfg.noAuth)
            {
                // It was a request to a page that doesn't require authentication, let it go.
                this.appService.setState("startPage", false);
                return NextAction.Proceed;
            }

            return NextAction.GoToLogin;
        }

        // if they are authenticated, allow the navigation
        // disable start page / fullscreen settings that are set by default
        return NextAction.ProceedAndResetState;
    }

    private async handleAutoLogin(username: string,
                                  password: string,
                                  sessionToken: string)
    {
        if (!this.domain.users.isInitialized)
        {
            try
            {
                let credentials: SingleSignOnCredentials;
                if (username && password)
                {
                    credentials          = new SingleSignOnCredentials();
                    credentials.username = username;
                    credentials.password = password;
                }
                else if (sessionToken)
                {
                    document.cookie = `sessionToken=${sessionToken}`;
                }

                await this.domain.users.init(credentials);
            }
            catch (e)
            {
            }
        }
    }
}

export interface AuthorizationDefinition<T extends BaseComponent | void> extends Type<T>
{
}

export interface AuthorizationConfig
{
    noAuth?: boolean;
}

export abstract class AuthorizationContext
{
    private static s_modelPrototypeToDescriptor: Map<Type<any>, AuthorizationConfig> = new Map();

    static AuthorizationDef<S extends BaseComponent, T extends AuthorizationDefinition<S>>(config: AuthorizationConfig): any
    {
        return function (definition: T)
        {
            AuthorizationContext.s_modelPrototypeToDescriptor.set(definition, config);

            // Link the descriptor to the block's prototype.
            let blockDef                    = definition.prototype;
            blockDef.$$AuthorizationContext = config;

            return definition;
        };
    }

    static lookup(comp: Type<any> | string | null): AuthorizationConfig
    {
        if (!comp || typeof comp == "string") return null;

        return AuthorizationContext.s_modelPrototypeToDescriptor.get(comp);
    }
}

export const AuthorizationDef = AuthorizationContext.AuthorizationDef;

enum NextAction
{
    Proceed, ProceedAndResetState, GoToLogin
}
