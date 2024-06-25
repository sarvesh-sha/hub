import {Injectable} from "@angular/core";
import {ApiService} from "app/services/domain/api.service";
import {DataConnectionService} from "app/services/domain/data-connection.service";

@Injectable({providedIn: "root"})
export class AppDomainContext
{
    constructor(public apis: ApiService,
                public dataConnection: DataConnectionService)
    {
    }
}
