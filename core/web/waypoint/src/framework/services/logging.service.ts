import {Injectable, Type} from "@angular/core";
import {ApiClient} from "framework/services/api.client";
import {UtilsService} from "framework/services/utils.service";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";

export abstract class LoggerLevelSettings
{
    private levels = new Map<LoggingSeverity, boolean>();

    constructor(public parent: LoggerLevelSettings)
    {
    }

    enable(severity: LoggingSeverity)
    {
        this.levels.set(severity, true);
    }

    disable(severity: LoggingSeverity)
    {
        this.levels.set(severity, false);
    }

    reset(severity: LoggingSeverity)
    {
        this.levels.delete(severity);
    }

    isEnabled(severity: LoggingSeverity): boolean
    {
        if (this.levels.has(severity))
        {
            return this.levels.get(severity) == true;
        }

        if (this.parent)
        {
            return this.parent.isEnabled(severity);
        }

        return false;
    }

    isInherited(severity: LoggingSeverity): boolean
    {
        return !this.levels.has(severity);
    }

    getLogger(svcType: Type<any>): Logger
    {
        let loggers = this.getLoggers();

        let logger = loggers.get(svcType);
        if (!logger)
        {
            logger = new Logger(svcType, this);
            loggers.set(svcType, logger);
        }

        return logger;
    }

    abstract getLoggers(): Map<Type<any>, Logger>;

    public abstract getService(): LoggingService;
}

@Injectable()
export class LoggingService extends LoggerLevelSettings
{
    appenders: ILogAppender[] = [];

    loggers: Map<Type<any>, Logger> = new Map<Type<any>, Logger>();

    /**
     * Constructor
     */
    constructor()
    {
        super(null);

        this.enable(LoggingSeverity.Info);
        this.enable(LoggingSeverity.Warn);
        this.enable(LoggingSeverity.Error);

        console.info("#######################################################################");
        console.info("");
        console.info("Use 'window.$$logger' to access the Optio3 logger in the console window");
        console.info("");
        console.info("   window.$$logger.dump() will show the loggers and their settings");
        console.info("");
        console.info("   window.$$logger.enable(<logger>, <level>)  will enable a level on a specific logger");
        console.info("   window.$$logger.disable(<logger>, <level>) will disable a level on a specific logger");
        console.info("");
        console.info("   window.$$logger.disableCBOR() to revert to JSON");
        console.info("   window.$$logger.enableCBOR() to revert to CBOR");
        console.info("");
        console.info("#######################################################################");

        (<any>window).$$logger = new LoggingHelper(this);
    }

    addAppender(appender: ILogAppender)
    {
        this.appenders.push(appender);
    }

    getLoggers(): Map<Type<any>, Logger>
    {
        return this.loggers;
    }

    getService(): LoggingService
    {
        return this;
    }

    //--//

    publish(entry: LogEntry)
    {
        if (this.appenders.length == 0)
        {
            const txt = entry.toString();

            switch (entry.severity)
            {
                case LoggingSeverity.Info:
                    console.info(txt);
                    break;

                case LoggingSeverity.Warn:
                    console.warn(txt);
                    break;

                case LoggingSeverity.Error:
                    console.error(txt);
                    break;

                case LoggingSeverity.Debug:
                case LoggingSeverity.DebugVerbose:
                case LoggingSeverity.DebugObnoxious:
                    console.debug(txt);
                    break;
            }
        }
        else
        {
            for (let appender of this.appenders)
            {
                appender.append(entry);
            }
        }
    }
}

class LoggingHelper
{
    constructor(private svc: LoggingService)
    {
    }

    disableCBOR()
    {
        ApiClient.disableCBOR = true;
    }

    enableCBOR()
    {
        ApiClient.disableCBOR = undefined;
    }

    dump()
    {
        let severities = UtilsService.getEnumValues<LoggingSeverity>(LoggingSeverity);

        console.info(`Available log levels: ${this.printLevels(severities)}`);
        console.info("");

        this.dumpInner("", this.svc, severities);
    }

    private dumpInner(indent: string,
                      root: LoggerLevelSettings,
                      severities: LoggingSeverity[])
    {
        this.svc.loggers.forEach((value,
                                  key) =>
                                 {
                                     if (value.parent != root)
                                     {
                                         return;
                                     }

                                     let active    = [];
                                     let inherited = [];

                                     for (let sev of severities)
                                     {
                                         if (value.isEnabled(sev))
                                         {
                                             if (value.isInherited(sev))
                                             {
                                                 inherited.push(sev);
                                             }
                                             else
                                             {
                                                 active.push(sev);
                                             }
                                         }
                                     }

                                     if (inherited.length == 0)
                                     {
                                         if (active.length == 0)
                                         {
                                             console.info(`${indent}Logger: ${key.name} => All disabled`);
                                         }
                                         else
                                         {
                                             console.info(`${indent}Logger: ${key.name} => ${this.printLevels(active)}`);
                                         }
                                     }
                                     else
                                     {
                                         if (active.length == 0)
                                         {
                                             console.info(`${indent}Logger: ${key.name} => Inherited(${this.printLevels(inherited)})`);
                                         }
                                         else
                                         {
                                             console.info(`${indent}Logger: ${key.name} => ${this.printLevels(active)} Inherited(${this.printLevels(inherited)})`);
                                         }
                                     }

                                     this.dumpInner(indent + "    ", value, severities);
                                 });
    }

    private dumpSettings(indent: string,
                         logger: Logger,
                         level: LoggingSeverity)
    {
        console.info(`${indent}    ${LoggingSeverity[level]}: ${logger.isEnabled(level) ? "On" : "Off"}`);
    }

    private printLevels(severities: LoggingSeverity[])
    {
        let texts = [];

        for (let sev of severities)
        {
            texts.push(LoggingSeverity[sev]);
        }

        return texts.join(", ");
    }

    //--//

    enable(logger: string,
           level: string)
    {
        let severity: LoggingSeverity = <any>LoggingSeverity[<any>level];
        this.svc.loggers.forEach((value,
                                  key) =>
                                 {
                                     if (key.name == logger)
                                     {
                                         value.enable(severity);
                                     }
                                 });
    }

    disable(logger: string,
            level: string)
    {
        let severity: LoggingSeverity = UtilsService.getEnumValue(LoggingSeverity, level);
        this.svc.loggers.forEach((value,
                                  key) =>
                                 {
                                     if (key.name == logger)
                                     {
                                         value.disable(severity);
                                     }
                                 });
    }
}

export enum LoggingSeverity
{
    Info,
    Warn,
    Error,
    Debug,
    DebugVerbose,
    DebugObnoxious
}

export class LogEntry
{
    timestamp: Date;

    source: Type<any>;

    severity: LoggingSeverity;

    msg: string;

    parameters: any[];

    //--//

    toString(): string
    {
        const id        = this.source.name;
        const timestamp = MomentHelper.parse(this.timestamp)
                                      .format("HH:mm:ss");

        switch (this.severity)
        {
            case LoggingSeverity.Info:
                return `${timestamp} : ${id} : INFO  : ${this.msg}`;

            case LoggingSeverity.Warn:
                return `${timestamp} : ${id} : WARN  : ${this.msg}`;

            case LoggingSeverity.Error:
                return `${timestamp} : ${id} : ERROR : ${this.msg}`;

            case LoggingSeverity.Debug:
                return `${timestamp} : ${id} : DEBUG : ${this.msg}`;

            case LoggingSeverity.DebugVerbose:
                return `${timestamp} : ${id} : DBG_V : ${this.msg}`;

            case LoggingSeverity.DebugObnoxious:
                return `${timestamp} : ${id} : DBG_O : ${this.msg}`;

            default:
                return `${timestamp} : ${id} : ----- : ${this.msg}`;
        }
    }

}

export interface ILogAppender
{
    append(entry: LogEntry): void;
}

export class Logger extends LoggerLevelSettings
{
    constructor(private id: Type<any>,
                parent: LoggerLevelSettings)
    {
        super(parent);
    }

    getLoggers(): Map<Type<any>, Logger>
    {
        return this.parent.getLoggers();
    }

    getService(): LoggingService
    {
        return this.parent.getService();
    }

    //--//

    log(severity: LoggingSeverity,
        msg: string,
        ...parameters: any[])
    {
        if (!this.isEnabled(severity))
        {
            return;
        }

        let entry        = new LogEntry();
        entry.timestamp  = new Date();
        entry.source     = this.id;
        entry.severity   = severity;
        entry.msg        = msg;
        entry.parameters = parameters;

        this.getService()
            .publish(entry);
    }

    info(msg: string,
         ...parameters: any[])
    {
        this.log(LoggingSeverity.Info, msg, parameters);
    }

    warn(msg: string,
         ...parameters: any[])
    {
        this.log(LoggingSeverity.Warn, msg, parameters);
    }

    error(msg: string,
          ...parameters: any[])
    {
        this.log(LoggingSeverity.Error, msg, parameters);
    }

    debug(msg: string,
          ...parameters: any[])
    {
        this.log(LoggingSeverity.Debug, msg, parameters);
    }

    debugVerbose(msg: string,
                 ...parameters: any[])
    {
        this.log(LoggingSeverity.DebugVerbose, msg, parameters);
    }

    debugObnoxious(msg: string,
                   ...parameters: any[])
    {
        this.log(LoggingSeverity.DebugObnoxious, msg, parameters);
    }
}

