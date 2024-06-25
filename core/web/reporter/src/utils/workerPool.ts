class Worker
{
    public isAvailable: boolean = true;

    public async run(task: () => Promise<void>)
    {
        this.isAvailable = false;
        try
        {
            await task();
        }
        catch (err)
        {
        }
        finally
        {
            this.isAvailable = true;
        }
    }
}

interface Task {
    id: number;
    task: () => Promise<void>;
}

export class WorkerPool
{
    private workers: Worker[];
    private tasks: Task[];
    private taskId = 0;

    constructor(numWorkers: number)
    {
        this.workers = [];
        this.tasks   = [];

        for (let i = 0; i < numWorkers; i++)
        {
            this.workers.push(new Worker());
        }

        setInterval(() =>
                    {
                        if (this.tasks.length)
                        {
                            const [availableWorker] = this.workers.filter(
                                w => w.isAvailable);
                            if (availableWorker)
                            {
                                availableWorker.run(this.tasks.shift().task);
                            }
                        }
                    }, 100);
    }

    public addTask(task: () => Promise<void>): number
    {
        this.tasks.push({ id: this.taskId++, task });
        return this.taskId;
    }

    public removeTask(taskId: number) {
        this.tasks = this.tasks.filter(t => t.id !== taskId);
    }
}
