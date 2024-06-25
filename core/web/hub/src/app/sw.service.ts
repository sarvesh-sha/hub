import {Injectable} from "@angular/core";
import {SwUpdate} from "@angular/service-worker";
import {ErrorLevel, ErrorService, RefreshErrorAction} from "framework/services/error.service";

@Injectable()
export class SwService
{
    constructor(private sw: SwUpdate,
                private errors: ErrorService)
    {
        sw.unrecoverable.subscribe((event) =>
                                   {
                                       this.errors.notify("An unrecoverable error has occurred. Please refresh the page and try again.", [new RefreshErrorAction()], ErrorLevel.Fatal);
                                   });
    }

    public async forceUpdate(): Promise<boolean>
    {
        if (await this.sw.checkForUpdate())
        {
            return this.sw.activateUpdate();
        }

        return false;
    }
}
