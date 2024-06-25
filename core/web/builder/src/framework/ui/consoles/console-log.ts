import {SafeHtml} from "@angular/platform-browser";
import {UtilsService} from "framework/services/utils.service";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";

/**
 * Provides the lines to show in the console.
 */
export interface IConsoleLogProvider
{
    getLogCount(): number;

    getLogPage(start: number,
               end: number): Promise<IConsoleLogEntry[]>;

    performFilter(filter: ApplicationLogFilter): Promise<IApplicationLogRange[]>;
}

/**
 * Holds information for an entry in the console.
 */
export interface IConsoleLogEntry
{
    lineNumber: number;

    /**
     * Returns the item as html.
     */
    columns: Map<LogColumn, string | SafeHtml>;
}

//--//

export class AnsiRendererOptions
{
    fg: string       = "#FFF";
    bg: string       = "#000";
    newline: boolean = false;
    colors: any      = AnsiRendererOptions.getDefaultColors();

    private static getDefaultColors(): any
    {
        const colors: { [index: number]: string } = {
            0 : "#000",
            1 : "#A00",
            2 : "#0A0",
            3 : "#A50",
            4 : "#00A",
            5 : "#A0A",
            6 : "#0AA",
            7 : "#AAA",
            8 : "#555",
            9 : "#F55",
            10: "#5F5",
            11: "#FF5",
            12: "#55F",
            13: "#F5F",
            14: "#5FF",
            15: "#FFF"
        };

        for (let red = 0; red <= 5; red++)
        {
            for (let green = 0; green <= 5; green++)
            {
                for (let blue = 0; blue <= 5; blue++)
                {
                    const c = 16 + (red * 36) + (green * 6) + blue;
                    const r = AnsiRendererOptions.toHexString(red > 0 ? red * 40 + 55 : 0);
                    const g = AnsiRendererOptions.toHexString(green > 0 ? green * 40 + 55 : 0);
                    const b = AnsiRendererOptions.toHexString(blue > 0 ? blue * 40 + 55 : 0);

                    colors[c] = `#${r}${g}${b}`;
                }
            }
        }

        for (let gray = 0; gray <= 23; gray++)
        {
            const c = gray + 232;
            const l = AnsiRendererOptions.toHexString(gray * 10 + 8);

            colors[c] = `#${l}${l}${l}`;
        }

        return colors;
    }

    private static toHexString(num: number): string
    {
        let high = (num / 16) & 0xF;
        let low  = (num & 0xF);

        return high.toString(16) + low.toString(16);
    }
}

class AnsiOutput
{
    text: string;
    tag: string;
}

export class AnsiRenderer
{
    options = new AnsiRendererOptions();

    stack: string[]      = [];
    output: AnsiOutput[] = [];
    lastText: AnsiOutput;
    textInsertionPoint: number;

    appendOutput(token: string,
                 data: string)
    {
        switch (token)
        {
            case "text":
                if (!this.lastText)
                {
                    this.textInsertionPoint = 0;
                    this.lastText           = this.pushOutput("", null);
                }

                let newTextLen     = data.length;
                let currentText    = this.lastText.text;
                let currentTextLen = currentText.length;

                if (this.textInsertionPoint == currentTextLen)
                {
                    this.lastText.text = currentText + data;
                }
                else
                {
                    let textBefore = currentText.substring(0, this.textInsertionPoint);

                    let remaining = this.textInsertionPoint + newTextLen;
                    if (remaining < currentTextLen)
                    {
                        let textAfter      = currentText.substring(remaining);
                        this.lastText.text = textBefore + data + textAfter;
                    }
                    else
                    {
                        this.lastText.text = textBefore + data;
                    }
                }

                this.textInsertionPoint += newTextLen;
                break;

            case "display":
                this.lastText = null;
                this.pushOutput(null, this.handleDisplay(data));
                break;

            case "xterm256":
                this.lastText = null;
                this.pushOutput(null, this.pushForegroundColor(this.options.colors[data]));
                break;
        }
    }

    handleDisplay(text: string): string
    {
        let code = parseInt(text, 10);

        switch (code)
        {
            case -1:
                return "<br/>";

            case 0:
                return this.resetStyles();

            case 1:
                return this.pushTag("b");

            case 3:
                return this.pushTag("i");

            case 4:
                return this.pushTag("u");

            case 5:
            case 6:
            case 7:
                return this.pushTag("blink");

            case 8:
                return this.pushStyle("display:none");

            case 9:
                return this.pushTag("strike");

            case 22:
                return this.closeTag("b");

            case 23:
                return this.closeTag("i");

            case 24:
                return this.closeTag("u");

            case 39:
                return this.pushForegroundColor(this.options.fg);

            case 49:
                return this.pushBackgroundColor(this.options.bg);
        }

        if (30 <= code && code <= 39) return this.pushForegroundColor(this.options.colors[code - 30]);
        if (90 <= code && code <= 97) return this.pushForegroundColor(this.options.colors[code - 90]);

        if (40 <= code && code <= 49) return this.pushBackgroundColor(this.options.colors[8 + (code - 40)]);
        if (100 <= code && code <= 107) return this.pushBackgroundColor(this.options.colors[8 + (code - 100)]);

        return "";
    }

    resetStyles(): string
    {
        let oldStack = this.stack;
        this.stack   = [];

        return oldStack.reverse()
                       .map((tag) => "</" + tag + ">")
                       .join("");
    }

    pushTag(tag: string,
            style?: string): string
    {
        this.stack.push(tag);

        return `<${tag}${style ? " style=\"" + style + "\"" : ""}>`;
    }

    pushStyle(style?: string): string
    {
        return this.pushTag("span", style);
    }

    pushForegroundColor(color: string): string
    {
        return this.pushTag("span", "color:" + color);
    }

    pushBackgroundColor(color: string)
    {
        return this.pushTag("span", "background-color:" + color);
    }

    closeTag(style: string): string
    {
        if (this.stack.slice(-1)[0] == style)
        {
            this.stack.pop();
            return "</" + style + ">";
        }

        return "";
    }

    toHtml(text: string): string
    {
        const tokens = [
            {
                pattern: /^\</,
                sub    : (match: string,
                          group: string) =>
                {
                    this.appendOutput("text", "&lt;");
                    return "";
                } // remove text.
            },
            {
                pattern: /^\>/,
                sub    : (match: string,
                          group: string) =>
                {
                    this.appendOutput("text", "&gt;");
                    return "";
                } // remove text.
            },
            {
                pattern: /^ /,
                sub    : (match: string,
                          group: string) =>
                {
                    this.appendOutput("text", " ");
                    return "";
                } // remove text.
            },
            {
                pattern: /^\x08+/,
                sub    : (match: string,
                          group: string) =>
                {
                    this.textInsertionPoint = Math.max(0, this.textInsertionPoint - match.length);
                    return "";
                } // remove text.
            },
            {
                pattern: /^\x1b\[[012]?K/,
                sub    : (match: string,
                          group: string) => "" // remove text.
            },
            {
                pattern: /^\x1b\[38;5;(\d+)m/,
                sub    : (match: string,
                          group: string) =>
                {
                    this.appendOutput("xterm256", group);
                    return "";
                }
            },
            {
                pattern: /^\n/,
                sub    : (match: string,
                          group: string) =>
                {
                    if (this.options.newline)
                    {
                        this.appendOutput("display", "-1");
                    }
                    else
                    {
                        this.appendOutput("text", match);
                    }

                    return "";

                }
            },
            {
                pattern: /^\x1b\[((?:\d{1,3};?)+|)m/,
                sub    : (match: string,
                          group: string) =>
                {
                    if (group.trim().length === 0)
                    {
                        group = "0";
                    }

                    for (let val of this.trimEnd(group)
                                        .split(";"))
                    {
                        this.appendOutput("display", val);
                    }

                    return "";
                }
            },
            {
                pattern: /^\x1b\[?[\d;]{0,3}/,
                sub    : (match: string,
                          group: string) => "" // remove text.
            },
            {
                pattern: /^([^\x1b\x08\n <>]+)/,
                sub    : (match: string,
                          group: string) =>
                {
                    this.appendOutput("text", match);
                    return "";
                }
            }
        ];

        while (true)
        {
            let length = text.length;
            if (length == 0) break;

            for (let handler of tokens)
            {
                let newText = text.replace(handler.pattern, handler.sub);

                if (newText != text)
                {
                    text = newText;
                    break;
                }
            }
        }

        let result = "";
        for (let output of this.output)
        {
            if (output.text)
            {
                result += UtilsService.replaceAll(output.text, " ", "&nbsp;");
            }

            if (output.tag)
            {
                result += output.tag;
            }

        }
        return result;
    }

    private trimEnd(str: string): string
    {
        return str.replace(/\s+$/, "");
    }

    private pushOutput(text: string,
                       tag: string): AnsiOutput
    {
        let newOutput  = new AnsiOutput();
        newOutput.text = text;
        newOutput.tag  = tag;
        this.output.push(newOutput);
        return newOutput;
    }
}

export type LogColumn = "lineNumber" | "timestamp" | "level" | "host" | "thread" | "selector" | "line";

export class ColumnConfig
{
    constructor(public readonly type: LogColumn,
                public readonly name: string,
                public enabled: boolean,
                public width: number)
    {}

    public clone(): ColumnConfig { return new ColumnConfig(this.type, this.name, this.enabled, this.width); }

    public equals(other: ColumnConfig): boolean
    {
        return this.type === other.type && this.name === other.name && this.enabled === other.enabled && this.width == other.width;
    }

    public get columnStyle()
    {
        return this.width ? {width: `${this.width}px`} : {};
    }
}

export const DefaultColumns: ColumnConfig[] = [
    new ColumnConfig("lineNumber", "#", true, 40),
    new ColumnConfig("timestamp", "Timestamp", true, 140),
    new ColumnConfig("level", "Level", true, 40),
    new ColumnConfig("host", "Host", false, 40),
    new ColumnConfig("thread", "Thread ID", true, 80),
    new ColumnConfig("selector", "Selector", true, 110)
];

export interface IApplicationLogRange
{
    startOffset: number;
    endOffset: number;
}

export interface IApplicationLog
{
    lineNumber: number;
    timestamp: Date;
    thread: string;
    fd: number;
    host: string;
    level: string;
    selector: string;
    line: string;
}

export class ApplicationLogFilter
{
    startOffset: number = null;

    levels: string[] = [];

    threads: string[] = [];

    hosts: string[] = [];

    selectors: string[] = [];

    filter: string = "";

    public clone(): ApplicationLogFilter
    {
        let clone       = new ApplicationLogFilter();
        clone.levels    = UtilsService.arrayCopy(this.levels);
        clone.threads   = UtilsService.arrayCopy(this.threads);
        clone.hosts     = UtilsService.arrayCopy(this.hosts);
        clone.selectors = UtilsService.arrayCopy(this.selectors);
        clone.filter    = this.filter;

        return clone;
    }

    public equals(other: ApplicationLogFilter): boolean
    {
        if (!UtilsService.compareArraysAsSets(this.levels, other.levels)) return false;
        if (!UtilsService.compareArraysAsSets(this.threads, other.threads)) return false;
        if (!UtilsService.compareArraysAsSets(this.hosts, other.hosts)) return false;
        if (!UtilsService.compareArraysAsSets(this.selectors, other.selectors)) return false;
        if (!UtilsService.equivalentStrings(this.filter, other.filter)) return false;

        return true;
    }
}

export class ApplicationLogEntry implements IConsoleLogEntry
{
    public readonly lineNumber: number;

    public readonly columns: Map<LogColumn, SafeHtml>;

    private static readonly StandardColumns: LogColumn[] = [
        "thread",
        "host",
        "level",
        "line",
        "selector"
    ];

    /**
     * Construct the log entry.
     * @param sanitizer
     * @param item
     * @param incrementLine
     */
    constructor(sanitizer: (html: string) => SafeHtml,
                item: IApplicationLog,
                incrementLine: boolean)
    {
        this.lineNumber = item.lineNumber;
        this.columns    = new Map<LogColumn, SafeHtml>();

        this.columns.set("lineNumber", this.getHtml(sanitizer, (incrementLine ? item.lineNumber + 1 : item.lineNumber) + "", "console-ln"));
        this.columns.set("timestamp", this.getHtml(sanitizer, MomentHelper.parse(item.timestamp)
                                                                          .format("YYYY-MM-DD HH:mm:ss.SSS"), "console-ts"));

        for (let column of ApplicationLogEntry.StandardColumns)
        {
            this.columns.set(column, this.getHtml(sanitizer, "" + item[column], "console-msg"));
        }
    }

    private getHtml(sanitizer: (html: string) => SafeHtml,
                    line: string,
                    className: string)
    {
        let renderer = new AnsiRenderer();
        return sanitizer(`<span class="${className}">${renderer.toHtml(line)}</span>`);
    }
}

