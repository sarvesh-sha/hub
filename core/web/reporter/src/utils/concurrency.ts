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
                                    callback: (item: T) => Promise<void>): Promise<void>
{
    if (items && items.length)
    {
        let promises = new Array<Promise<void>>(items.length);

        for (let i = 0; i < items.length; i++)
        {
            promises[i] = callback(items[i]);
        }

        await Promise.all(promises);
    }
}

class FutureInner<T> extends Promise<T>
{
    private timeoutId: any;
    private resolveCallback: (value?: T | PromiseLike<T>) => void;
    private rejectCallback: (reason?: any) => void;
    private resolved: boolean;

    constructor(state: any)
    {
        if (state instanceof Function) {
            super(state);

            return this;
        }

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

    static delayed<T>(timeout: number,
                      value?: T): Future<T>
    {
        let res = new Future<T>();
        setTimeout(() => res.resolve(value), timeout);
        return res;
    }

    constructor(options?: any)
    {
        if (options instanceof Function) {
            super(options);

            return this;
        }

        super({});
    }
}
