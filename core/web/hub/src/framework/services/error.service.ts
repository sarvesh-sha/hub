import {Injectable} from "@angular/core";
import {ThemePalette} from "@angular/material/core";
import {Router} from "@angular/router";
import {ApiClientConfiguration} from "framework/services/api.client";

import {UtilsService} from "framework/services/utils.service";

@Injectable()
export class ErrorService
{
    constructor(private utilSvc: UtilsService,
                private configuration: ApiClientConfiguration)
    {

    }

    errors: Array<Error> = [];

    error(code: string,
          message: string,
          details?: Array<string>): Error
    {
        // set the message to the code if no message is provided
        if (!message && code) message = code;

        return this.push(ErrorLevel.Error, 10_000, code, message, details);
    }

    warn(message: string,
         dismissTime?: number): Error
    {
        if (dismissTime === -1) dismissTime = 6_000;

        return this.push(ErrorLevel.Warn, dismissTime, "WARNING", message, []);
    }

    success(message: string,
            dismissTime?: number,
            actions?: Array<ErrorAction>): Error
    {
        if (dismissTime === -1) dismissTime = 3_000;

        return this.push(ErrorLevel.Success, dismissTime, "SUCCESS", message, [], actions);
    }

    notify(message: string,
           actions: Array<ErrorAction> = [],
           level: ErrorLevel           = ErrorLevel.Success): Error
    {
        return this.push(level, null, "SUCCESS", message, [], actions);
    }

    private push(level: ErrorLevel,
                 dismissTime: number,
                 code: string,
                 message: string,
                 details: Array<string>,
                 actions: Array<ErrorAction> = []): Error
    {
        let error     = new Error();
        error.level   = level;
        error.code    = code;
        error.message = message;
        error.details = details;
        error.actions = actions;
        this.errors.push(error);

        if (dismissTime > 0)
        {
            this.utilSvc.setTimeoutOutsideAngular(() => this.dismiss(error), dismissTime);
        }

        return error;
    }

    hasAny()
    {
        return this.errors.length > 0;
    }

    dismiss(error?: Error)
    {
        if (error && this.errors)
        {
            let index: number = this.errors.indexOf(error);
            if (index > -1) this.errors.splice(index, 1);
        }
        else
        {
            this.errors = new Array<Error>();
        }
    }

    async execAndReportError(callback: () => Promise<void>)
    {
        try
        {
            await callback();
        }
        catch (error)
        {
            if (error.code)
            {
                this.error(error.code, error.message);
            }
            else
            {
                this.error("Error", `${JSON.stringify(error)}`);
            }

            error.optio3ErrorHandled = true;
            throw error;
        }
    }

    async execAndReportErrorFromDecorator(method: Function,
                                          target: any,
                                          args: IArguments): Promise<any>
    {
        try
        {
            return await method.apply(target, args);
        }
        catch (error)
        {
            if (error.validationErrors)
            {
                let details = [];

                for (let validationError of error.validationErrors.entries)
                {
                    details.push(`'${validationError.field}' : ${validationError.reason}`);
                }

                this.error(error.code, error.message, details);
            }
            else if (error.code)
            {
                let notAuthenticated = this.configuration.isNotAuthenticatedError && this.configuration.isNotAuthenticatedError(error.code);
                if (notAuthenticated)
                {
                    if (this.configuration.handleNotAuthenticatedError)
                    {
                        this.configuration.handleNotAuthenticatedError();
                    }
                }
                else
                {
                    this.error(error.code, error.message);
                }
            }
            else
            {
                this.error("Error", `${error}`);
            }

            error.optio3ErrorHandled = true;
            throw error;
        }
    }
}

export class Error
{
    level: ErrorLevel = ErrorLevel.Error;

    code: string;

    message: string;

    details: Array<string>;

    actions: Array<ErrorAction> = [];
}

export enum ErrorLevel
{

    Fatal,

    Error,

    Warn,

    Success
}

export type ErrorActionFunction = (e: Error) => boolean;

export class ErrorAction
{
    label: string;

    func: ErrorActionFunction;

    color: ThemePalette;

    constructor(label: string             = "Ok",
                func: ErrorActionFunction = null,
                color: ThemePalette       = "primary")
    {
        this.label = label;
        this.func  = func;
        this.color = color;
    }
}

export class NavigationErrorAction extends ErrorAction
{
    constructor(private router: Router,
                label: string       = "View",
                public route?: string,
                color: ThemePalette = "primary")
    {
        super(label, (e: Error) =>
        {

            if (route)
            {
                router.navigateByUrl(route);
                return true;
            }

            return false;
        }, color);
    }
}

export class RefreshErrorAction extends ErrorAction
{
    constructor(label: string = "Refresh")
    {
        super(label, (e: Error) =>
        {
            document.location.reload();
            return true;
        });
    }
}

export class DismissErrorAction extends ErrorAction
{
    constructor(label: string = "Dismiss")
    {
        super(label, (e: Error) =>
        {
            return true;
        }, "accent");
    }
}

export class DownloadErrorAction extends ErrorAction
{
    constructor(label: string,
                downloadUrl: string,
                fileName: string)
    {
        super(label, () =>
        {
            const link    = window.document.createElement("a");
            link.download = fileName;
            link.href     = downloadUrl;
            window.document.body.appendChild(link);
            link.click();
            window.document.body.removeChild(link);
            return true;
        });
    }
}
