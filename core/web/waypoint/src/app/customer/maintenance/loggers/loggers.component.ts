import {Component, Injector, Input} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {UtilsService} from "framework/services/utils.service";

import {ControlOption} from "framework/ui/control-option";

@Component({
               selector   : "o3-loggers",
               templateUrl: "./loggers.component.html"
           })
export class LoggersComponent extends SharedSvc.BaseApplicationComponent
{
    private m_loggers: Array<Models.LoggerConfiguration>         = [];
    private m_loggersFiltered: Array<Models.LoggerConfiguration> = [];

    @Input() set loggers(value: Array<Models.LoggerConfiguration>)
    {
        this.m_loggers         = [];
        this.m_loggersFiltered = [];
        this.loggerLookup      = {};
        this.loggersMap        = {};
        this.m_loggersUpdated  = null;

        for (let logger of value)
        {
            this.m_loggers.push(logger);
            this.loggerLookup[logger.name] = logger;

            let map: { [key: string]: number } = {};

            for (let v of this.severities)
            {
                let level = logger.levels[v];

                if (level == undefined)
                {
                    map[v] = 0;
                }
                else if (level)
                {
                    map[v] = 1;
                }
                else
                {
                    map[v] = -1;
                }
            }

            this.loggersMap[logger.name] = map;
        }

        this.m_loggers.sort((a,
                             b) =>
                            {
                                let fullNameA = this.getFullName(a);
                                let fullNameB = this.getFullName(b);

                                return UtilsService.compareStrings(fullNameA, fullNameB);
                            });

        this.filterLoggers();
    }

    get loggers(): Array<Models.LoggerConfiguration>
    {
        return this.m_loggers;
    }

    get loggersFiltered(): Array<Models.LoggerConfiguration>
    {
        return this.m_loggersFiltered;
    }

    //--//

    private m_filterText: string = null;

    public get filterText(): string
    {
        return this.m_filterText;
    }

    public set filterText(value: string)
    {
        this.m_filterText = value;
        this.filterLoggers();
    }

    //--//

    loggerLookup: { [key: string]: Models.LoggerConfiguration };

    loggersMap: { [key: string]: { [key: string]: number } };

    private m_loggersUpdated: { [key: string]: boolean } = null;

    statusOptions: ControlOption<number>[];

    severities = [
        "Error",
        "Warn",
        "Info",
        "Debug",
        "DebugVerbose",
        "DebugObnoxious"
    ];

    //--//

    constructor(inj: Injector)
    {
        super(inj);

        this.statusOptions = [];

        this.addOption("Inherit", 0);
        this.addOption("Enabled", 1);
        this.addOption("Disabled", -1);
    }

    getLabel(logger: Models.LoggerConfiguration,
             sev: string,
             opt: ControlOption<number>): string
    {
        if (opt.id == 0)
        {
            while (logger.parent)
            {
                let parent = this.loggerLookup[logger.parent];

                let level = parent.levels[sev];
                if (level != undefined)
                {
                    return level ? "Inherit (Enabled)" : "Inherit (Disabled)";
                }

                logger = parent;
            }
        }

        return opt.label;
    }

    updateLogger(logger: Models.LoggerConfiguration,
                 sev: string)
    {
        if (!this.m_loggersUpdated)
        {
            this.m_loggersUpdated = {};
        }

        this.m_loggersUpdated[logger.name] = true;

        let val = this.loggersMap[logger.name][sev];

        if (val == 0)
        {
            delete logger.levels[sev];
        }
        else
        {
            logger.levels[sev] = (val == 1);
        }

        this.detectChanges();
    }

    public get isDirty(): boolean
    {
        return this.m_loggersUpdated != null;
    }

    public wasUpdated(logger: Models.LoggerConfiguration): boolean
    {
        return this.m_loggersUpdated && this.m_loggersUpdated[logger.name];
    }

    public filterLoggers()
    {
        if (this.m_filterText)
        {
            this.m_loggersFiltered = [];

            let filterTarget = this.m_filterText.toLocaleLowerCase();

            for (let logger of this.m_loggers)
            {
                if (this.matchFilter(filterTarget, logger.name))
                {
                    this.m_loggersFiltered.push(logger);
                }
            }
        }
        else
        {
            this.m_loggersFiltered = this.m_loggers;
        }

    }

    //--//

    private addOption(label: string,
                      id: number)
    {
        let res   = new ControlOption<number>();
        res.label = label;
        res.id    = id;

        this.statusOptions.push(res);
    }

    private getFullName(logger: Models.LoggerConfiguration): string
    {
        let name = logger.name;
        while (logger.parent)
        {
            let parent = this.loggerLookup[logger.parent];

            name   = `${parent.name}/${name}`;
            logger = parent;
        }

        return name;
    }
}
