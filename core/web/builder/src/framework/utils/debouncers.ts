import {callAsyncSafely} from "framework/utils/concurrency";
import {BehaviorSubject, merge, MonoTypeOperatorFunction, Observable} from "rxjs";
import {debounceTime, distinctUntilChanged, skip, take, throttleTime} from "rxjs/operators";

interface ISyncFunction<T>
{
    (...args: any[]): T;
}

interface IAsyncFunction<T>
{
    (...args: any[]): Promise<T>;
}

export class SyncDebouncer<T>
{
    private m_task: number;
    private m_taskArgs: any[];

    private m_return = new BehaviorSubject<T>(null);
    public return$   = this.m_return.asObservable();

    get scheduled(): boolean
    {
        return !!this.m_task;
    }

    constructor(private m_delay: number,
                private m_callback: ISyncFunction<T>)
    {}

    invoke(...args: any[])
    {
        // If a task is scheduled, cancel to re-schedule
        this.cancelInvocation();

        // Schedule the execution
        this.m_taskArgs = args;
        this.m_task     = setTimeout(() => this.process(), this.m_delay);
    }

    forceProcessing()
    {
        if (this.scheduled)
        {
            this.cancelInvocation();
            this.process();
        }
    }

    cancelInvocation()
    {
        if (this.scheduled)
        {
            clearTimeout(this.m_task);
            this.m_task     = undefined;
            this.m_taskArgs = undefined;
        }
    }

    protected process(): void
    {
        let args = this.m_taskArgs;

        // Clear previous scheduling
        this.m_task     = undefined;
        this.m_taskArgs = undefined;

        // Execute the callback
        this.m_return.next(this.m_callback(args));
    }
}

export class AsyncDebouncer<T>
{
    private m_task: number;
    private m_taskArgs: any[];
    private m_nextTaskArgs: any[];

    private m_processing: boolean = false;
    get processing(): boolean
    {
        return this.m_processing;
    }

    get scheduled(): boolean
    {
        return !!this.m_task;
    }

    constructor(private m_delay: number,
                private m_callback: IAsyncFunction<T>)
    {}

    invoke(...args: any[])
    {
        // If processing, queue invocation
        if (this.m_processing)
        {
            this.m_nextTaskArgs = args;
            return;
        }

        // If a task is scheduled, cancel and re-schedule
        this.cancelInvocation();

        // Schedule the execution
        this.m_taskArgs = args;
        this.m_task     = setTimeout(() => this.process(), this.m_delay);
    }

    cancelInvocation()
    {
        if (this.scheduled)
        {
            clearTimeout(this.m_task);
            this.m_task = undefined;
        }
    }

    private async process(): Promise<void>
    {
        let args = this.m_taskArgs;

        // Set up for processing
        this.m_processing = true;
        this.m_task       = undefined;
        this.m_taskArgs   = undefined;

        try
        {
            // Execute the callback
            await this.m_callback(args);
        }
        catch (e)
        {
            // Ignore failures.
        }

        // Flag as finished
        this.m_processing = false;

        // Invoke queued call if needed
        if (this.m_nextTaskArgs !== null)
        {
            let args            = this.m_nextTaskArgs;
            this.m_nextTaskArgs = null;
            this.invoke(args);
        }
    }
}

export class Debouncer
{
    private m_task: number;
    private m_taskFn: () => Promise<void>;
    private m_taskArgs: any;
    private m_pending: number = 0;

    get scheduled(): boolean
    {
        return !!this.m_task;
    }

    constructor(private readonly m_delay: number,
                private readonly m_callback: (data?: any) => Promise<void>)
    {
    }

    async forceExecution()
    {
        if (this.scheduled)
        {
            let fn = this.m_taskFn;
            this.cancel();
            await fn();
        }
    }

    invoke(data?: any)
    {
        this.m_pending++;
        this.m_taskArgs = data;

        this.schedule();
    }

    cancel()
    {
        if (this.scheduled)
        {
            this.cancelInvocation();
        }
    }

    cancelInvocation()
    {
        if (this.scheduled)
        {
            clearTimeout(this.m_task);
            this.m_task   = undefined;
            this.m_taskFn = undefined;
        }

        this.m_pending = 0;
    }

    private schedule()
    {
        if (!this.scheduled)
        {
            this.m_taskFn = async () =>
            {
                this.m_pending = 0;

                await callAsyncSafely(() => this.m_callback(this.m_taskArgs));

                this.m_task   = undefined;
                this.m_taskFn = undefined;

                // If there were new events, reschedule ourselves.
                if (this.m_pending)
                {
                    this.schedule();
                }
            };

            this.m_task = setTimeout(this.m_taskFn, this.m_delay);
        }
    }
}

/*
    Given a stream of events, this will immediately emit the first event, then
    emit the most recent value not more than once every 'duration', always
    emitting the last received input.
 */
export function throttledDebounceTime<T>(duration: number): MonoTypeOperatorFunction<T>
{
    return (source: Observable<T>) =>
    {
        return merge(
            source.pipe(throttleTime(duration)),
            source.pipe(debounceTime(duration))
        )
            .pipe(throttleTime(0, undefined, {
                leading : true,
                trailing: false
            }))
            .pipe(distinctUntilChanged());
    };
}

/*
    Behaves just like debounceTime, except the very first value is emitted immediately
 */
export function debounceTimeAfterFirst<T>(duration: number): MonoTypeOperatorFunction<T>
{
    return (source: Observable<T>) =>
    {
        return merge(
            source.pipe(take(1)),
            source.pipe(
                skip(1),
                debounceTime(duration)
            )
        );
    };
}
