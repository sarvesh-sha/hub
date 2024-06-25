export interface PriorityQueueEntry<T>
{
    key: number,
    value: T
}

export abstract class PriorityQueue<T>
{
    protected m_data: PriorityQueueEntry<T>[] = [];

    isEmpty(): boolean
    {
        return this.m_data.length === 0;
    }

    peek(): T
    {
        return this.isEmpty() ? null : this.m_data[this.m_data.length - 1].value;
    }

    peekKey(): number
    {
        return this.isEmpty() ? null : this.m_data[this.m_data.length - 1].key;
    }

    peekEntry(): PriorityQueueEntry<T>
    {
        return this.isEmpty() ? null : this.m_data[this.m_data.length - 1];
    }

    dequeue(): T
    {
        return this.isEmpty() ? null : this.m_data.pop().value;
    }

    enqueue(key: number,
            value: T): void
    {
        this.m_data.splice(this.findIndex(key), 0, {
            key  : key,
            value: value
        });
    }

    protected abstract findIndex(key: number): number;
}

export class CustomPriorityQueue<T> extends PriorityQueue<T>
{
    constructor(private m_comparator: (a: number,
                                       b: number) => number)
    {
        super();
    }

    protected findIndex(key: number): number
    {
        let l = 0;
        let r = this.m_data.length;
        let m: number;
        let result: number;

        while (l < r)
        {
            m      = Math.floor((l + r) / 2);
            result = this.m_comparator(this.m_data[m].key, key);
            if (result < 0)
            {
                l = m + 1;
            }
            else
            {
                r = m;
            }
        }

        return l;
    }
}

export class MinPriorityQueue<T> extends CustomPriorityQueue<T>
{
    constructor()
    {
        super((a: number,
               b: number) => b - a);
    }
}

export class MaxPriorityQueue<T> extends CustomPriorityQueue<T>
{
    constructor()
    {
        super((a: number,
               b: number) => a - b);
    }
}
