// Observable related interfaces
export interface ISimpleCallback
{
    (...args: any[]): any
}

export interface ISimplePubSub
{
    subscribe: (event: string,
                callback: ISimpleCallback,
                priority?: number) => any,
    unsubscribe: (event: string,
                  callback: ISimpleCallback) => any
}

interface ISimplePrioritizedCallback
{
    callback: ISimpleCallback,
    priority: number
}

export abstract class SimplePubSub implements ISimplePubSub
{
    protected callbacks: Map<string, ISimplePrioritizedCallback[]> = new Map<string, ISimplePrioritizedCallback[]>();

    protected constructor() {}

    subscribe(event: string,
              callback: ISimpleCallback,
              priority: number = 0): void
    {

        let index = this.findCallbackIndex(event, callback);
        if (index < 0)
        {
            let entry = {
                callback: callback,
                priority: priority
            };

            this.getEventCallbacks(event)
                .push(entry);
            this.sortCallbacks(event);
        }
        else
        {
            let callback = this.getEventCallbacks(event)[index];
            if (callback.priority !== priority)
            {
                callback.priority = priority;
                this.sortCallbacks(event);
            }
        }
    }

    unsubscribe(event: string,
                callback: ISimpleCallback): void
    {
        let index = this.findCallbackIndex(event, callback);
        if (index >= 0)
        {
            this.getEventCallbacks(event)
                .splice(index, 1);
        }
    }

    unsubscribeAll(): void
    {
        this.callbacks.clear();
    }

    protected executeCallbacks(event: string,
                               ...args: any[]): void
    {
        let callbacks = this.getEventCallbacks(event);
        for (let entry of callbacks) entry.callback(...args);
    }

    protected getEventCallbacks(event: string): ISimplePrioritizedCallback[]
    {
        if (!this.callbacks.has(event)) this.callbacks.set(event, []);

        return this.callbacks.get(event);
    }

    private findCallbackIndex(event: string,
                              callback: ISimpleCallback): number
    {
        let callbacks = this.getEventCallbacks(event);
        return callbacks.findIndex((entry: ISimplePrioritizedCallback) =>
                                   {
                                       return entry.callback === callback;
                                   });
    }

    private sortCallbacks(event: string)
    {
        let callbacks = this.getEventCallbacks(event);
        callbacks.sort((a: ISimplePrioritizedCallback,
                        b: ISimplePrioritizedCallback) =>
                       {
                           return a.priority - b.priority;
                       });
    }
}
