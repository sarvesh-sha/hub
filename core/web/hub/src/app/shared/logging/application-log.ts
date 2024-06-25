import * as Models from "app/services/proxy/model/models";
import {ApplicationLogFilter} from "framework/ui/consoles/console-log";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";

export function convertLogFilters(filters: ApplicationLogFilter): Models.LogEntryFilterRequest
{
    return Models.LogEntryFilterRequest.newInstance({
                                                        fromOffset: filters.startOffset,
                                                        toOffset  : null,
                                                        limit     : null,
                                                        filter    : filters.filter,
                                                        threads   : filters.threads,
                                                        hosts     : filters.hosts,
                                                        selectors : filters.selectors,
                                                        levels    : filters.levels
                                                    });
}

export class LogFormatter
{
    public static formatLines(logEntries: Models.LogLine[]): string[]
    {
        let len_level    = 0;
        let len_thread   = 0;
        let len_selector = 0;

        for (let line of logEntries)
        {
            len_level    = Math.max(len_level, line.level?.length);
            len_thread   = Math.max(len_thread, line.thread?.length);
            len_selector = Math.max(len_selector, line.selector?.length);
        }

        let res: string[] = [];

        for (let line of logEntries)
        {
            let timestamp = MomentHelper.parse(line.timestamp);
            let log       = `${timestamp.format("YYYY-MM-DD HH:mm:ss.SSS")}: `;

            log = LogFormatter.append(log, line.level?.toString(), len_level);
            log = LogFormatter.append(log, line.thread, len_thread);
            log = LogFormatter.append(log, line.selector, len_selector);
            log += line.line.replace("\n", "");

            res.push(log);
        }

        return res;
    }

    private static append(line: string,
                          text: string,
                          len: number)
    {
        if (!text) text = "";

        return `${line}${text.padEnd(len)} | `;
    }
}
