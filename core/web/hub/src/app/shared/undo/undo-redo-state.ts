import {UtilsService} from "framework/services/utils.service";

import {Subject} from "rxjs";

export class StateSnapshot<T>
{
    constructor(public state: T,
                public readonly isHistory: boolean,
                public readonly description: string = null)
    {}
}

export class StateHistory<T>
{
    get active(): boolean
    {
        return !!this.m_host;
    }

    private m_stepping: boolean = false;

    public changed = new Subject<void>();

    constructor(private m_host: StateRestorable<T>,
                private m_baseSnapshot?: StateSnapshot<T>,
                private m_history?: StateSnapshot<T>[],
                private m_pointer?: number,
                private m_predecessors?: StateSnapshot<T>[],
                private m_successors?: StateSnapshot<T>[],
                public readonly max: number = 25)
    {
        if (!this.m_history?.length) this.m_history = [];

        if (isNaN(this.m_pointer ?? NaN))
        {
            this.m_pointer = this.m_history.length - 1;
        }
        else
        {
            this.m_pointer = UtilsService.clamp(this.m_pointer, 0, this.m_history.length - 1);
        }

        if (this.m_host) this.init();
    }

    async init()
    {
        if (!this.m_baseSnapshot)
        {
            if (this.m_history.length)
            {
                this.m_baseSnapshot = new StateSnapshot(this.m_history.shift().state, false);
                this.m_pointer--;
            }
            else
            {
                let baseState       = await this.m_host.readState();
                this.m_baseSnapshot = new StateSnapshot(await this.m_host.cloneState(baseState), false);
            }
        }

        this.enforceMax();
    }

    async current(): Promise<T>
    {
        // Disallow action if not active
        if (!this.active) return null;

        let state = this.getSnapshot()?.state;
        if (state)
        {
            return this.m_host.cloneState(state);
        }

        return null;
    }

    onHistory(): boolean
    {
        return this.getSnapshot()?.isHistory;
    }

    private getSnapshot(pointer?: number): StateSnapshot<T>
    {
        if (pointer == null) pointer = this.m_pointer;

        if (pointer === -1)
        {
            return this.m_baseSnapshot;
        }
        else if (pointer < -1)
        {
            if (this.m_predecessors) return this.m_predecessors[this.m_predecessors.length + pointer + 1];
        }
        else if (this.m_history.length === 0)
        {
            if (this.m_successors) return this.m_successors[pointer];
        }

        return this.m_history[pointer];
    }

    undo(): Promise<T>
    {
        return this.performStateStep(false);
    }

    redo(): Promise<T>
    {
        return this.performStateStep(true);
    }

    private async performStateStep(forward: boolean): Promise<T>
    {
        // Disallow action if not active
        if (!this.active) return null;

        let canStep = forward ? this.canRedo() : this.canUndo();
        if (canStep)
        {
            let clonedState: T;
            try
            {
                this.m_stepping = true;
                if (forward)
                {
                    this.m_pointer++;
                }
                else
                {
                    this.m_pointer--;
                }

                clonedState = await this.m_host.cloneState(this.getSnapshot().state);
                await this.m_host.restoreToState(clonedState);
                this.changed.next();
                this.notify();
            }
            finally
            {
                this.m_stepping = false;
            }

            return clonedState;
        }

        return null;
    }

    clear(notify: boolean = false): StateHistoryState<T>
    {
        let inPredecessors = this.m_pointer < 0;
        let inSuccessors   = this.m_history.length === 0 && this.m_pointer >= 0;
        let state          = {
            pointer: inPredecessors || inSuccessors ? this.m_history.length - 1 : this.m_pointer,
            history: this.m_history
        };

        this.m_pointer = -1;
        this.m_history = [];

        this.changed.next();
        if (notify) this.notify();

        return state;
    }

    async record(description?: string)
    {
        // Disallow action if not active
        if (!this.active) return;

        if (this.shiftSuccPreds())
        {
            this.clear();
            await this.init();
        }

        await this.recordHelper(description);

        this.notify();
    }

    /**
     * if (a recording needs to take place and active state is of a snapshot from m_predecessors or m_successors):
     * resets m_predecessors, m_successors, and m_baseSnapshot
     *
     * returns true if shifting/reset takes place
     */
    private shiftSuccPreds(): boolean
    {
        let shift = 0;
        if (this.m_pointer < -1 || this.m_history.length === 0) shift = -(this.m_pointer + 1);

        if (shift === 0) return false;

        let prevBaseSnapshot = this.m_baseSnapshot;
        if (shift > 0)
        {
            let translationStates = this.m_predecessors.slice(this.m_predecessors.length - shift);
            this.m_baseSnapshot   = translationStates.shift();
            translationStates.push(prevBaseSnapshot);
            this.m_predecessors = this.m_predecessors.slice(0, this.m_predecessors.length - shift);
            this.m_successors   = translationStates.concat(this.m_successors);
        }
        else
        {
            let shiftMagnitude = -shift;

            let translationStates = this.m_successors.slice(0, shiftMagnitude);
            this.m_baseSnapshot   = translationStates.pop();
            translationStates.unshift(prevBaseSnapshot);
            this.m_successors   = this.m_successors.slice(shiftMagnitude);
            this.m_predecessors = this.m_predecessors.concat(translationStates);
        }

        return true;
    }

    private async recordHelper(description?: string)
    {
        let state     = await this.m_host.readState();
        let stateCopy = await this.m_host.cloneState(state);
        let snapshot  = new StateSnapshot(stateCopy, true, description);

        // Replace any state after the current pointer with the new record
        this.m_history = this.m_history.slice(0, this.m_pointer + 1);
        this.m_history.push(snapshot);
        this.m_pointer++;

        this.enforceMax();
        this.changed.next();
    }

    deactivate()
    {
        this.m_host = null;
    }

    async activate(host: StateRestorable<T>)
    {
        this.m_host = host;
        if (!this.m_baseSnapshot)
        {
            await this.init();
        }
    }

    canUndo(): boolean
    {
        return !this.m_stepping && !!this.getSnapshot(this.m_pointer - 1);
    }

    canRedo(): boolean
    {
        return !this.m_stepping && !!this.getSnapshot(this.m_pointer + 1);
    }

    undoDescription(): string
    {
        if (!this.canUndo())
        {
            return "Nothing to undo";
        }
        else
        {
            let snapshot = this.getSnapshot();
            if (snapshot.isHistory)
            {
                return snapshot.description ? `Undo '${snapshot.description.toLowerCase()}'` : "Undo";
            }
            else
            {
                let prevSnapshot = this.getSnapshot(this.m_pointer - 1);
                return prevSnapshot.description ? `Jump to ${prevSnapshot.description.toLowerCase()}` : "Jump to previous version";
            }
        }
    }

    redoDescription(): string
    {
        if (!this.canRedo())
        {
            return "Nothing to redo";
        }
        else
        {
            let nextSnapshot = this.getSnapshot(this.m_pointer + 1);
            if (nextSnapshot.isHistory)
            {
                return nextSnapshot.description ? `Redo '${nextSnapshot.description.toLowerCase()}'` : "Redo";
            }
            else
            {
                return nextSnapshot.description ? `Jump to ${nextSnapshot.description.toLowerCase()}` : "Jump to next version";
            }
        }
    }

    private enforceMax()
    {
        let numExtra = this.m_history.length - this.max;
        if (numExtra > 0)
        {
            this.m_history = this.m_history.slice(numExtra);
            this.m_pointer -= numExtra;
        }
    }

    private notify()
    {
        if (this.active && this.m_host.onHistoryChange) this.m_host.onHistoryChange();
    }
}

export type StateHistoryState<T> = { pointer: number, history: StateSnapshot<T>[] }

export interface StateRestorable<T>
{
    onHistoryChange?(): void;

    restoreToState(state: T): Promise<void>;

    cloneState(state: T): Promise<T>;

    readState(): Promise<T>;
}
