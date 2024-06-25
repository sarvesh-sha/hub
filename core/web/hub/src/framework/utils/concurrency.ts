//
// Classes and functions to help manage concurrency.
//

import {BehaviorSubject} from "rxjs";

export function callSafely(f: () => void)
{
    try
    {
        f();
    }
    catch (error)
    {
    }
}

export async function callAsyncSafely(f: () => Promise<void>)
{
    try
    {
        await f();
    }
    catch (error)
    {
    }
}

export async function inParallel<T>(items: T[],
                                    callback: (item: T,
                                               index?: number) => Promise<any>): Promise<void>
{
    if (items && items.length)
    {
        let promises = new Array<Promise<void>>(items.length);

        for (let i = 0; i < items.length; i++)
        {
            promises[i] = callback(items[i], i);
        }

        await Promise.all(promises);
    }
}

export async function mapInParallel<T, R>(items: T[],
                                          callback: (item: T,
                                                     index?: number) => Promise<R>): Promise<R[]>
{
    if (!items) return null;

    let results  = new Array<R>(items.length);
    let promises = new Array<Promise<R>>(items.length);

    for (let i = 0; i < items.length; i++)
    {
        promises[i] = callback(items[i], i);
    }

    for (let i = 0; i < items.length; i++)
    {
        results[i] = await promises[i];
    }

    return results;
}

export async function someInParallel<T>(items: T[],
                                        callback: (item: T,
                                                   index?: number) => Promise<boolean>): Promise<boolean>
{
    if (!items) return null;

    let completed = new Future<boolean>();
    await Promise.race([
                           completed,
                           inParallel(items, async (item: T,
                                                    index?: number) =>
                           {
                               if (await callback(item, index))
                               {
                                   completed.resolve(true);
                               }
                           })
                       ]);
    completed.resolve(false);

    return completed;
}

export async function mapInParallelNoNulls<T, R>(items: T[],
                                                 callback: (item: T,
                                                            index?: number) => Promise<R>): Promise<R[]>
{
    let results = await mapInParallel(items, callback) || [];

    return results.filter(r => !!r);
}

export async function mapAsync<T, R>(items: T[],
                                     callback: (item: T,
                                                index?: number) => Promise<R>): Promise<R[]>
{
    if (!items) return null;

    let results = [];

    for (let i = 0; i < items.length; i++)
    {
        results.push(await callback(items[i], i));
    }

    return results;
}

export async function filterAsync<T>(items: T[],
                                     callback: (item: T) => Promise<boolean>): Promise<T[]>
{
    if (!items) return null;

    let results = [];
    for (let item of items)
    {
        if (await callback(item))
        {
            results.push(item);
        }
    }

    return results;
}

export async function getSubjectValue<T>(subject: BehaviorSubject<T>): Promise<T>
{
    const result = new Future<T>();
    if (subject.getValue())
    {
        result.resolve(subject.getValue());
    }
    else
    {
        let sub = subject.subscribe((val) =>
                                    {
                                        if (val)
                                        {
                                            result.resolve(val);
                                            sub.unsubscribe();
                                        }
                                    });
    }

    return result;
}

//--//

class FutureInner<T> extends Promise<T>
{
    private timeoutId: any;
    private resolveCallback: (value?: T | PromiseLike<T>) => void;
    private rejectCallback: (reason?: any) => void;
    private m_resolved: boolean;

    constructor(state: any)
    {
        super((resolve,
               reject) =>
              {
                  state.resolveCallback = resolve;
                  state.rejectCallback  = reject;
              });

        this.resolveCallback = state.resolveCallback;
        this.rejectCallback  = state.rejectCallback;
    }

    isResolved(): boolean
    {
        return this.m_resolved;
    }

    resolve(value?: T)
    {
        if (this.canResolve())
        {
            this.cancelTimeout();

            this.resolveCallback(value);
        }
    }

    reject(reason?: any)
    {
        if (this.canResolve())
        {
            this.cancelTimeout();

            this.rejectCallback(reason);
        }
    }

    setCancellationTimeout(timeout: number,
                           reason?: any)
    {
        if (!this.m_resolved)
        {
            this.cancelTimeout();
            this.timeoutId = setTimeout(() => this.reject(reason), timeout);
        }
    }

    setResolveTimeout(value: T,
                      timeout: number)
    {
        if (!this.m_resolved)
        {
            this.cancelTimeout();
            this.timeoutId = setTimeout(() => this.resolve(), timeout);
        }
    }

    private cancelTimeout()
    {
        if (this.timeoutId)
        {
            clearTimeout(this.timeoutId);
            this.timeoutId = undefined;
        }
    }

    private canResolve()
    {
        if (this.m_resolved)
        {
            return false;
        }

        this.m_resolved = true;
        return true;
    }
}

export class Future<T> extends FutureInner<T>
{
    static guard: boolean;

    constructor()
    {
        super({});

        //
        // The "zone.js" module installs some extensions to Promises.
        // One of these extensions looks for Promises rejected *without* any listeners.
        // But we do want to create Futures that are waitable after the fact.
        // So the workaround is to create a fake listener.
        // The guard variable is to protect against infinite recursion, since .then allocates a new Promise...
        //
        if (!Future.guard)
        {
            Future.guard = true;
            this.then((v) => { }, (reason) => { });
            Future.guard = false;
        }
    }

    static delayed<T>(timeout: number,
                      value?: T): Future<T>
    {
        let res = new Future<T>();
        setTimeout(() => res.resolve(value), timeout);
        return res;
    }

    // Return a Promise for then/catch/finally
    static get [Symbol.species]()
    {
        return Promise;
    }

    get [Symbol.toStringTag]()
    {
        return "Future";
    }
}
