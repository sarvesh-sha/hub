//
// Classes and functions to help manage concurrency.
//

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

export async function inParallel<T>(items: T[],
                                    callback: (item: T,
                                               index?: number) => Promise<void>): Promise<void>
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

//--//

class FutureInner<T> extends Promise<T>
{
    private timeoutId: any;
    private resolveCallback: (value?: T | PromiseLike<T>) => void;
    private rejectCallback: (reason?: any) => void;
    private resolved: boolean;

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
        if (!this.resolved)
        {
            this.cancelTimeout();
            this.timeoutId = setTimeout(() => this.reject(reason), timeout);
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
        if (this.resolved)
        {
            return false;
        }

        this.resolved = true;
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
}