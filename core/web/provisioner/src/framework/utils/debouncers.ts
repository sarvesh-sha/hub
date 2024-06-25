import {BehaviorSubject} from "rxjs";

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
    private m_task: number    = null;
    private m_taskArgs: any[] = null;

    private m_return = new BehaviorSubject<T>(null);
    public return$   = this.m_return.asObservable();

    constructor(private m_callback: ISyncFunction<T>,
                private m_interval: number = 0)
    {}

    invoke(...args: any[])
    {
        // If a task is scheduled, cancel to re-schedule
        if (this.m_task !== null) clearTimeout(this.m_task);

        // Schedule the execution
        this.m_task     = setTimeout(this.process.bind(this), this.m_interval);
        this.m_taskArgs = args;
    }

    forceProcessing()
    {
        if (this.m_task)
        {
            clearTimeout(this.m_task);
            this.process();
        }
    }

    protected process(): void
    {
        let args = this.m_taskArgs;

        // Clear previous scheduling
        this.m_task     = null;
        this.m_taskArgs = null;

        // Execute the callback
        this.m_return.next(this.m_callback(args));
    }
}

export class AsyncDebouncer<T>
{
    private m_task: number        = null;
    private m_taskArgs: any[]     = null;
    private m_nextTaskArgs: any[] = null;
    private m_processing: boolean = false;

    constructor(private m_callback: IAsyncFunction<T>,
                private m_interval: number = 0)
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
        if (this.m_task !== null) clearTimeout(this.m_task);

        // Schedule the execution
        this.m_task     = setTimeout(this.process.bind(this), this.m_interval);
        this.m_taskArgs = args;
    }

    private async process(): Promise<void>
    {
        let args = this.m_taskArgs;

        // Set up for processing
        this.m_processing = true;
        this.m_task       = null;
        this.m_taskArgs   = null;

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
    private m_taskArgs: any;
    private m_pending: number = 0;

    constructor(private delay: number,
                private readonly callback: (data?: any) => Promise<void>)
    {
    }

    invoke(data?: any)
    {
        this.m_pending++;
        this.m_taskArgs = data;

        this.schedule();
    }

    cancel()
    {
        if (this.m_task)
        {
            clearTimeout(this.m_task);
            this.m_task = undefined;
        }
    }

    private schedule()
    {
        if (!this.m_task)
        {
            this.m_task = setTimeout(async () =>
                                     {
                                         this.m_pending = 0;

                                         try
                                         {
                                             await this.callback(this.m_taskArgs);
                                         }
                                         catch (error)
                                         {
                                         }

                                         this.m_task = undefined;

                                         // If there were new events, reschedule ourselves.
                                         if (this.m_pending)
                                         {
                                             this.schedule();
                                         }
                                     }, this.delay);
        }
    }
}
