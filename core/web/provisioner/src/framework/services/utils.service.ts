import {Injectable, NgZone} from "@angular/core";
import {Future} from "framework/utils/concurrency";

@Injectable()
export class UtilsService
{
    /**
     * Constructor
     */
    constructor(private ngZone: NgZone)
    {
    }

    public static arrayCopy<T>(a: T[]): T[]
    {
        return [...(a || [])];
    }

    /**
     * returns (a and b are falsy) || (same lengths && all contents of a are also in b and vis versa)
     *
     * @param a
     * @param b
     */
    public static compareArraysAsSets<T>(a: T[],
                                         b: T[]): boolean
    {
        let emptyA = !a || a.length === 0;
        let emptyB = !b || b.length === 0;
        if (emptyA && emptyB) return true;
        if (emptyA || emptyB) return false;
        if (a.length !== b.length) return false;

        let aLookup = new Set<T>(a);
        for (let bItem of b)
        {
            if (!aLookup.has(bItem)) return false;
        }

        let bLookup = new Set<T>(b);
        for (let aItem of a)
        {
            if (!bLookup.has(aItem)) return false;
        }

        return true;
    }

    public static mapIterable<S, T>(set: Iterable<S>,
                                    callback: (item: S) => T): T[]
    {
        let result: T[] = [];
        for (let item of set)
        {
            result.push(callback(item));
        }

        return result;
    }

    public static isBlankString(a: string): boolean
    {
        return !a || a.length == 0 || a.trim().length == 0;
    }

    public static equivalentStrings(a: string,
                                    b: string): boolean
    {
        return !UtilsService.compareStringsHelper(a, b);
    }

    public static compareStrings(a: string,
                                 b: string,
                                 ascending: boolean = true): number
    {
        return ascending ? UtilsService.compareStringsHelper(a, b) : UtilsService.compareStringsHelper(b, a);
    }

    private static compareStringsHelper(a: string,
                                        b: string): number
    {
        a = a || "";
        b = b || "";

        return a.localeCompare(b);
    }

    public static compareNumbers(a: number,
                                 b: number,
                                 ascending: boolean = true): number
    {
        if (isNaN(a))
        {
            return isNaN(b) ? 0 : -1;
        }
        else if (isNaN(b))
        {
            return 1;
        }
        else
        {
            return ascending ? a - b : b - a;
        }
    }

    public static compareJson(a: any,
                              b: any): boolean
    {
        let binMatch = UtilsService.compareDeep(a, b);

        let jsonMatch = UtilsService.compareJsonTextual(a, b);
        if (jsonMatch != (binMatch == null))
        {
            console.debug(`compare mismatch between JSON and per-field:`);
            console.debug(`a = ${JSON.stringify(a || "")}`);
            console.debug(`b = ${JSON.stringify(b || "")}`);
            console.debug(`compareDeep = ${binMatch}`);
        }

        return !binMatch;
    }

    public static compareDeep(a: any,
                              b: any): string
    {
        let set = new Set<any>();

        return UtilsService.compareRecursively(set, a, b);
    }

    private static compareRecursively(set: Set<any>,
                                      a: any,
                                      b: any): string
    {
        if (a == b)
        {
            return null;
        }

        if (!a)
        {
            return "<no value on left>";
        }

        if (!b)
        {
            return "<no value on right>";
        }

        if (typeof a != typeof b)
        {
            return "<type mismatch>";
        }

        if (typeof a !== "object")
        {
            return "<value mismatch>";
        }

        if (Object.getPrototypeOf(a) !== Object.getPrototypeOf(b))
        {
            return "<class mismatch>";
        }

        if (typeof a.toJSON === "function")
        {
            a = a.toJSON();
            b = b.toJSON();
            return UtilsService.compareRecursively(set, a, b);
        }

        if (set.has(a) || set.has(b)) // Recursion detected, assume different!
        {
            return "<recursion detected>";
        }

        set.add(a);
        set.add(b);

        if (Array.isArray(a))
        {
            if (!Array.isArray(b))
            {
                return "<array vs object>";
            }

            let aLen = a.length;
            let bLen = b.length;

            if (aLen != bLen) return "<array length>";

            for (let i = 0; i < aLen; i++)
            {
                let mismatch = UtilsService.compareRecursively(set, a[i], b[i]);
                if (mismatch)
                {
                    return `[${i}] => ${mismatch}`;
                }
            }
        }
        else
        {
            let aKeys = Object.keys(a);
            let bKeys = Object.keys(b);

            let aKeyLookup = new Set<string>(aKeys);
            for (let key of bKeys)
            {
                if (!aKeyLookup.has(key))
                {
                    return `<left lacks ${key}>`;
                }
            }

            let bKeyLookup = new Set<string>(aKeys);
            for (let key of aKeys)
            {
                if (!bKeyLookup.has(key))
                {
                    return `<right lacks ${key}>`;
                }
            }

            for (let key of aKeys)
            {
                if (key == "scaling") continue;
                let mismatch = UtilsService.compareRecursively(set, a[key], b[key]);
                if (mismatch)
                {
                    return `${key} => ${mismatch}`;
                }
            }
        }

        return null;
    }


    public static compareJsonTextual(a: any,
                                     b: any): boolean
    {
        let aJson = a ? JSON.stringify(a) : "";
        let bJson = b ? JSON.stringify(b) : "";

        return aJson == bJson;
    }

    public static valuesAreUnique<T>(values: T[]): boolean
    {
        return new Set<T>(values).size === values?.length;
    }

    /**
     * Pluralize a given string, if able.
     * @param text {string} - The text to pluralize.
     * @param count {number} - The number of objects, used to determine if pluralization should occur.
     */
    public static pluralize(text: string,
                            count?: number)
    {
        if (text)
        {
            if (count && Math.abs(count) == 1) return text;

            let last = text[text.length - 1];
            if (last == "s")
            {
                return text + "es";
            }
            else if (last == "y")
            {
                // If the character preceding the 'y' is a vowel, only add an s.
                if (text.length == 1 || UtilsService.isVowel(text[text.length - 2]))
                {
                    return text + "s";
                }
                // Otherwise, replace the 'y' with 'ies'.
                else
                {
                    return text.substr(0, text.length - 1) + "ies";
                }
            }
            else
            {
                return text + "s";
            }
        }

        return text;
    }

    private static isVowel(char: string): boolean
    {
        return /[aeiouAEIOU]/.test(char);
    }

    /**
     *
     * Generates unique name given other names already used
     *
     * @param title
     * @param otherTitles
     */
    public static getUniqueTitle(title: string,
                                 otherTitles: string[]): string
    {
        if (!otherTitles) return null;
        if (!otherTitles.length) return title;
        let iterationNum = 0;

        if (/ copy [0-9]{1,2}$/.test(title))
        {
            let numStartIdx = title.lastIndexOf(" ") + 1;
            iterationNum    = eval(title.substr(numStartIdx, title.length - numStartIdx));
            title           = iterationNum ? title.substr(0, numStartIdx - " copy ".length) : title;
        }
        else if (title.substr(title.length - " copy".length, " copy".length) === " copy")
        {
            iterationNum = 1;
            title        = title.substr(0, title.length - " copy".length);
        }

        for (let i = iterationNum; i < iterationNum + otherTitles.length + 1; i++)
        {
            let substTitle = title;
            if (i) substTitle = i > 1 ? `${title} copy ${i}` : `${title} copy`;
            let otherTitle = otherTitles.find((otherTitle) => otherTitle === substTitle);
            if (!otherTitle) return substTitle;
        }

        return null;
    }

    /**
     * Replace all occurrences of find in str.
     * @param str
     * @param find
     * @param replace
     */
    public static replaceAll(str: string,
                             find: string,
                             replace: string): string
    {
        let result = "";
        let pos    = 0;

        while (pos < str.length)
        {
            let nextMatch = str.indexOf(find, pos);
            if (nextMatch < 0)
            {
                break;
            }

            result += str.substr(pos, nextMatch - pos);
            result += replace;
            pos = nextMatch + find.length;
        }

        result += str.substr(pos);
        return result;
    }

    /**
     * Capitalize the first letter of all words
     * @param str
     */
    public static capitalizeFirstLetterAllWords(str: string)
    {
        if (str)
        {
            let pieces = str.split(" ");
            for (let i = 0; i < pieces.length; i++)
            {
                let j     = pieces[i].charAt(0)
                                     .toUpperCase();
                pieces[i] = j + pieces[i].substr(1)
                                         .toLowerCase();
            }
            return pieces.join(" ");
        }

        return null;
    }

    public static async executeWithRetries(fn: () => Promise<boolean>,
                                           numRetries: number,
                                           delay: number           = 200,
                                           failureFn?: () => void,
                                           delayMultiplier: number = 1)
    {
        while (numRetries-- >= 0)
        {
            await Future.delayed(delay);

            if (await fn()) return;

            delay *= delayMultiplier;
        }

        if (failureFn) failureFn();
    }

    /**
     * returns index of target or bitwise complement of closest idx in case it's not found
     *
     * @param sortedArray
     * @param target
     * @param getValueFn
     */
    public static binarySearch<T>(sortedArray: T[],
                                  target: number,
                                  getValueFn: (item: T) => number): number
    {
        let low  = 0;
        let high = sortedArray.length - 1;

        while (low <= high)
        {
            let mid    = Math.floor((low + high) / 2);
            let midVal = getValueFn(sortedArray[mid]);

            if (midVal < target)
            {
                low = mid + 1;
            }
            else if (midVal > target)
            {
                high = mid - 1;
            }
            else
            {
                return mid;
            }
        }

        return -(low + 1);
    }

    /**
     * returns index of target or bitwise complement of closest idx in case it's not found
     *
     * @param sortedArray
     * @param target
     * @param sortedArrayLen
     */
    public static binarySearchForFloat64Array(sortedArray: Float64Array,
                                              sortedArrayLen: number,
                                              target: number): number
    {
        let low  = 0;
        let high = sortedArrayLen - 1;

        while (low <= high)
        {
            let mid    = Math.floor((low + high) / 2);
            let midVal = sortedArray[mid];

            if (midVal < target)
            {
                low = mid + 1;
            }
            else if (midVal > target)
            {
                high = mid - 1;
            }
            else
            {
                return mid;
            }
        }

        return -(low + 1);
    }

    public static mergeSortedArrays<T>(sortedArrays: T[][],
                                       compareFn: (a: T,
                                                   b: T) => number): T[]
    {
        switch (sortedArrays.length)
        {
            case 0:
                return [];

            case 1:
                return sortedArrays[0];

            case 2:
                return UtilsService.mergeSortedHelper(sortedArrays[0], sortedArrays[1], compareFn);

            default:
                let nextLevelLen = Math.floor((sortedArrays.length + 1) / 2);
                let nextLevel    = new Array<T[]>(nextLevelLen);
                for (let i = 0; i < nextLevelLen; i++)
                {
                    let a = sortedArrays[2 * i];
                    let b = sortedArrays[2 * i + 1];

                    nextLevel[i] = !b ? a : UtilsService.mergeSortedHelper(a, b, compareFn);
                }

                return UtilsService.mergeSortedArrays(nextLevel, compareFn);
        }
    }

    private static mergeSortedHelper<T>(a: T[],
                                        b: T[],
                                        compareFn: (a: T,
                                                    b: T) => number): T[]
    {
        a = a || [];
        b = b || [];

        let aLen = a.length;
        let bLen = b.length;
        let out  = new Array<T>(aLen + bLen);
        let aIdx = 0;
        let bIdx = 0;
        while (aIdx < aLen && bIdx < bLen)
        {
            let aVal   = a[aIdx];
            let bVal   = b[bIdx];
            let result = compareFn(aVal, bVal);
            if (result <= 0)
            {
                out[aIdx++ + bIdx] = aVal;
            }
            else
            {
                out[aIdx + bIdx++] = bVal;
            }
        }

        while (aIdx < aLen)
        {
            // Don't inline, otherwise we get the wrong index.
            let aVal           = a[aIdx];
            out[aIdx++ + bIdx] = aVal;
        }

        while (bIdx < bLen)
        {
            // Don't inline, otherwise we get the wrong index.
            let bVal           = b[bIdx];
            out[aIdx + bIdx++] = bVal;
        }

        return out;
    }

    /**
     * Get the portions of an href, broken down by the regex found at https://stackoverflow.com/a/21553982.
     * @param href {string} - The URL to parse.
     */
    public static parseUrl(href: string): ParsedUrl
    {
        return new ParsedUrl(href);
    }

    public static addClass(element: Element,
                           className: string)
    {
        let classList: DOMTokenList = element.classList;

        if (!classList.contains(className))
        {
            classList.add(className);
            element.className = classList.value;
        }
    }

    public static removeClass(element: Element,
                              className: string)
    {
        let classList: DOMTokenList = element.classList;

        if (classList.contains(className))
        {
            classList.remove(className);
            element.className = classList.value;
        }
    }

    public setTimeoutOutsideAngular(callback: () => void,
                                    ms: number): any
    {
        return this.ngZone.runOutsideAngular(() =>
                                             {
                                                 return setTimeout(() =>
                                                                   {
                                                                       this.ngZone.run(() =>
                                                                                       {
                                                                                           callback();
                                                                                       });
                                                                   }, ms);
                                             });
    }

    public setIntervalOutsideAngular(callback: () => void,
                                     ms: number): any
    {
        return this.ngZone.runOutsideAngular(() =>
                                             {
                                                 return setInterval(() =>
                                                                    {
                                                                        this.ngZone.run(() =>
                                                                                        {
                                                                                            callback();
                                                                                        });
                                                                    }, ms);
                                             });
    }

    public static extractKeysFromMap(data: { [key: string]: any },
                                     skipNulls: boolean = false,
                                     ...skipKey: string[]): string[]
    {
        let res: string[] = [];

        if (data)
        {
            for (let key in data)
            {
                if (skipKey != null && skipKey.indexOf(key) >= 0) continue;

                if (skipNulls && data[key] == null) continue;

                res.push(key);
            }
        }

        return res;
    }

    public static isMapEmpty(data: { [key: string]: any })
    {
        if (data)
        {
            for (let key in data)
            {
                return false;
            }
        }

        return true;
    }

    public static countKeysInMap(data: { [key: string]: any },
                                 skipNulls: boolean = false,
                                 ...skipKey: string[]): number
    {
        let res = 0;

        if (data)
        {
            for (let key in data)
            {
                if (skipKey != null && skipKey.indexOf(key) >= 0) continue;

                if (skipNulls && data[key] == null) continue;

                res++;
            }
        }

        return res;
    }

    //--//

    /**
     * Split a line into two parts, based on position of white space.
     * @param s
     */
    public static splitTextIntoTwoLines(s: string): string[]
    {
        s = s.replace(/\s/g, " ");

        let words = s.split(" ");
        if (words.length > 1)
        {
            let lengthMidpoint = s.length / 2;
            let lengthLeft     = -1;
            let whole          = words.join(" ");

            for (let word of words)
            {
                lengthLeft += word.length + 1;

                if (lengthLeft >= lengthMidpoint)
                {
                    let left  = whole.substr(0, lengthLeft);
                    let right = whole.substr(lengthLeft + 1);

                    return [
                        left,
                        right
                    ];
                }
            }
        }

        return [];
    }

    /**
     * Converts the rest of the arguments to a comma-separated line in the 'output' array.
     *
     * @param {string[]} output Target array
     * @param args
     */
    public static appendAsCommaSeparatedLine(output: string[],
                                             ...args: any[])
    {
        let line = "";

        for (let i in args)
        {
            let val = args[i] || "";
            let txt = val.toString();

            txt = UtilsService.replaceAll(txt, "\r", " ");
            txt = UtilsService.replaceAll(txt, "\n", " ");

            if (line.length > 0)
            {
                line += ",";
            }

            line += UtilsService.replaceAll(txt, ",", ";");
        }

        output.push(line);
    }

    public static afterJsonParse<T>(obj: T,
                                    constructor: { new(): T }): T
    {
        // console.debug(`Looking for fixup function for '${constructor.name}'`);

        //
        // We have an instance method on each class that requires fixups.
        // We'll walk the prototype hierarchy looking for it.
        // The method then returns the static function that will perform the actual fixup.
        //
        let proto = constructor.prototype;
        while (proto)
        {
            let fixupFunction = proto.getFixupPrototypeFunction;
            if (fixupFunction instanceof Function)
            {
                let func = fixupFunction();
                func(obj);
                // console.debug(`Found fixup function '${constructor.name}' -> ${func}`);
                break;
            }

            let parentProto = Object.getPrototypeOf(proto);
            if (parentProto === Object.prototype)
            {
                break;
            }

            proto = parentProto;
        }

        return obj;
    }

    public static getEnumNames(e: any): string[]
    {
        return Object.keys(e)
                     .filter((key) => isNaN(+key));
    }

    public static getEnumValues<T>(e: any): T[]
    {
        let names = UtilsService.getEnumNames(e);
        return names.map((n) => e[n]);
    }

    public static getEnumValue<T>(e: any,
                                  key: string): T
    {
        return <T>e[key];
    }

    public static parseEnumValue<T>(e: any,
                                    value: string): T
    {
        return <T>e[e[value]];
    }

    public static parseComputedStyle(computedStyle: string,
                                     unit: string): number
    {
        let unitIdx = computedStyle.indexOf(unit);
        if (unitIdx === -1) return 0;

        computedStyle = computedStyle.substring(0, unitIdx) + computedStyle.substring(unitIdx + unit.length);
        return parseFloat(computedStyle.trim());
    }

    public static formatCssValue(val: number | string): string
    {
        if (typeof val === "number")
        {
            return `${val}px`;
        }

        return val;
    }

    public static clamp(min: number,
                        max: number,
                        value: number): number
    {
        return Math.max(Math.min(value, max), min);
    }
}

export class ParsedUrl
{
    public protocol: string;
    public host: string;
    public hostname: string;
    public port: string;
    public pathname: string;
    public search: string;
    public hash: string;

    constructor(href: string)
    {
        if (href)
        {
            let match = href.match(/^(https?\:)\/\/(([^:\/?#]*)(?:\:([0-9]+))?)([\/]{0,1}[^?#]*)(\?[^#]*|)(#.*|)$/);
            if (match)
            {
                this.protocol = match[1];
                this.host     = match[2];
                this.hostname = match[3];
                this.port     = match[4];
                this.pathname = match[5];
                this.search   = match[6];
                this.hash     = match[7];
            }
        }
    }
}
