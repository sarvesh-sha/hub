import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";

import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {DeploymentHostExtended} from "app/services/domain/deployment-hosts.service";

import * as Models from "app/services/proxy/model/models";
import {convertLogFilters} from "app/shared/logging/application-log";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {ApplicationLogFilter, IApplicationLogRange, IConsoleLogEntry} from "framework/ui/consoles/console-log";
import {ConsoleLogComponent} from "framework/ui/consoles/console-log.component";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class DeploymentHostImagePullsService extends SharedSvc.BaseService<Models.DeploymentHostImagePull, DeploymentHostImagePullExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.DeploymentHostImagePull, DeploymentHostImagePullExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.DeploymentHostImagePull.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.DeploymentHostImagePull>
    {
        return this.api.deploymentHostImagePulls.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.DeploymentHostImagePull[]>
    {
        return this.api.deploymentHostImagePulls.getBatch(ids);
    }

    //--//

    @ReportError
    public getFiltered(filters: Models.DeploymentHostImagePullFilterRequest): Promise<Models.RecordIdentity[]>
    {
        return this.api.deploymentHostImagePulls.getFiltered(filters);
    }
}

export class DeploymentHostImagePullExtended extends SharedSvc.ExtendedModel<Models.DeploymentHostImagePull>
{
    static newInstance(svc: DeploymentHostImagePullsService,
                       model: Models.DeploymentHostImagePull): DeploymentHostImagePullExtended
    {
        return new DeploymentHostImagePullExtended(svc, model, Models.DeploymentHostImagePull.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.DeploymentHostImagePull.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    public static bindToLog(log: ConsoleLogComponent,
                            imageProvider: () => DeploymentHostImagePullExtended)
    {
        log.bind({
                     getLogCount(): number
                     {
                         let image = imageProvider();
                         return (image ? image.model.lastOffset : 0) + 1;
                     },
                     async getLogPage(start: number,
                                      end: number): Promise<IConsoleLogEntry[]>
                     {
                         let logEntries = [];

                         let image = imageProvider();
                         if (image)
                         {
                             if (start == 0)
                             {
                                 let hostName = await image.getHostDisplayName();

                                 let textPre = `Image '${image.model.image}' on host '${hostName}'`;
                                 let text    = `\x1b[33m${textPre}\x1b[0m`;

                                 logEntries.push(log.newLogEntry(Models.LogLine.newInstance({
                                                                                                lineNumber: 0,
                                                                                                timestamp : image.model.createdOn,
                                                                                                line      : text
                                                                                            }), false));

                                 start++;
                             }

                             if (start !== undefined) start--;
                             if (end !== undefined) end--;

                             let lines = await image.getLog(start, end, null) || [];
                             for (let line of lines)
                             {
                                 let text = line.line;
                                 let pos1 = text.indexOf("missing");
                                 if (pos1 >= 0)
                                 {
                                     let textPre  = text.substring(0, pos1);
                                     let textPost = text.substring(pos1);

                                     text = `${textPre}\x1b[35m${textPost}\x1b[0m`;
                                 }
                                 line.line = text;

                                 logEntries.push(log.newLogEntry(line));
                             }
                         }

                         return logEntries;
                     },
                     async performFilter(filter: ApplicationLogFilter): Promise<IApplicationLogRange[]>
                     {
                         let image = imageProvider();

                         // Always include header
                         let ranges: Models.LogRange[] = [
                             Models.LogRange.newInstance({
                                                             startOffset: 0,
                                                             endOffset  : 0
                                                         })
                         ];
                         if (image)
                         {
                             let realRanges = await image.filterLog(convertLogFilters(filter)) || [];
                             for (let range of realRanges)
                             {
                                 // Increment all offsets to account for header
                                 ranges.push(range);
                                 range.startOffset++;
                                 range.endOffset++;
                             }
                         }

                         return ranges;
                     }
                 });
    }

    //--//

    @Memoizer
    public getOwningDeployment(): Promise<DeploymentHostExtended>
    {
        return this.domain.deploymentHosts.getExtendedByIdentity(this.model.deployment);
    }

    @Memoizer
    public async getHostDisplayName(): Promise<string>
    {
        let host = await this.getOwningDeployment();
        return host?.displayName;
    }

    filterLog(filters: Models.LogEntryFilterRequest): Promise<Models.LogRange[]>
    {
        return this.domain.apis.deploymentHostImagePulls.filterLog(this.model.sysId, filters);
    }

    getLog(fromOffset?: number,
           toOffset?: number,
           limit?: number): Promise<Models.LogLine[]>
    {
        return this.domain.apis.deploymentHostImagePulls.getLog(this.model.sysId, fromOffset, toOffset, limit);
    }

    async forget(): Promise<void>
    {
        await this.domain.apis.deploymentHostImagePulls.remove(this.model.sysId);
    }
}

export type DeploymentHostImagePullChangeSubscription = SharedSvc.DbChangeSubscription<Models.DeploymentHostImagePull>;

//--//
