import {ManipulationNode} from "app/dashboard/dashboard/widgets/widget-manipulation";
import {WidgetManipulator, WidgetOutlineExtended} from "app/dashboard/dashboard/widgets/widget-manipulator";
import * as Models from "app/services/proxy/model/models";

import {Direction} from "framework/ui/charting/app-charting-utilities";
import {MaxPriorityQueue} from "framework/utils/queues";

export class ReactiveManipulationNode implements ManipulationNode
{
    parents: ReactiveManipulationNode[]  = [];
    children: ReactiveManipulationNode[] = [];

    private empties: number[] = [];

    get numIndexesWithoutCollision(): number
    {
        return this.empties?.length || 0;
    }

    public get shrinkable(): number
    {
        return this.length - this.minLength;
    }

    projectedOutline: Models.WidgetOutline;

    private m_position: number;

    public set position(position: number)
    {
        this.hostManipulation.updateModel(this.projectedOutline, this.m_position = position, this.m_length);
    }

    public get position(): number
    {
        return this.m_position;
    }

    private m_length: number;

    public set length(length: number)
    {
        this.hostManipulation.updateModel(this.projectedOutline, this.m_position, this.m_length = length);
    }

    public get length(): number
    {
        return this.m_length;
    }

    minLength: number = 1;

    pressureAfterShrink: number = 0;

    cumAncestorShrinkability: number = 0;

    constructor(private readonly hostManipulation: ReactiveManipulation,
                public widgetId: string,
                public pressureBeforeShrink: number,
                manipulator: WidgetManipulator,
                direction: Direction,
                public readonly visibleOutline?: Models.WidgetOutline)
    {
        if (!this.visibleOutline) this.visibleOutline = manipulator.widgetOutlineExtLookup[widgetId].model;
        this.projectedOutline = Models.WidgetOutline.newInstance(this.visibleOutline);

        switch (direction)
        {
            case Direction.Up:
                this.m_position = this.visibleOutline.top;
            // fallthrough
            case Direction.Down:
                if (isNaN(this.m_position)) this.m_position = this.visibleOutline.top + this.visibleOutline.height;
                this.m_length = this.visibleOutline.height;

                let endCol = this.visibleOutline.left + this.visibleOutline.width;
                for (let col = this.visibleOutline.left; col < endCol; col++) this.empties.push(col);
                break;

            case Direction.Left:
                this.m_position = this.visibleOutline.left;
            // fallthrough
            case Direction.Right:
                if (isNaN(this.m_position)) this.m_position = this.visibleOutline.left + this.visibleOutline.width;
                this.m_length = this.visibleOutline.width;

                let endRow = this.visibleOutline.top + this.visibleOutline.height;
                for (let row = this.visibleOutline.top; row < endRow; row++) this.empties.push(row);
                break;
        }
    }

    getIndexesWithoutCollision(): number[]
    {
        return this.empties.map((rowCol) => rowCol);
    }

    widgetCollision(collisionIdx: number)
    {
        let idx = this.empties.findIndex((rowCol) => rowCol === collisionIdx);
        if (idx >= 0) this.empties.splice(idx, 1);
    }
}

export abstract class ReactiveManipulation implements Manipulation
{
    public readonly valid: boolean;

    public readonly reactiveManipulationDAG: ReactiveManipulationNode;

    public readonly nodeLookup = new Map<string, ReactiveManipulationNode>();

    protected constructor(protected readonly m_manipulator: WidgetManipulator,
                          public readonly direction: Direction,
                          pressure: number,
                          public readonly forResize: boolean,
                          rootId: string,
                          rootOutline?: Models.WidgetOutline,
                          private readonly m_previousManipulations?: Manipulation[])
    {
        this.reactiveManipulationDAG = new ReactiveManipulationNode(this, rootId, pressure, this.m_manipulator, this.direction, rootOutline);
        this.nodeLookup.set(rootId, this.reactiveManipulationDAG);

        let explored: Map<string, ReactiveManipulationNode> = new Map();
        let unallocatedPressure                             = this.exploreWithoutShrink(explored);

        if (unallocatedPressure === 0)
        {
            this.pushWithoutShrink(explored);
            this.valid = true;
        }
        else if (this.forResize)
        {
            this.exploreShrinkPotential([...explored.values()]);
            this.valid = this.pushWithShrink();
        }
        else
        {
            this.valid = false;
        }
    }

    public executeManipulation(outline: Models.WidgetOutline,
                               node: ReactiveManipulationNode)
    {
        if (this.forResize)
        {
            this.executeResize(outline, node);
        }
        else
        {
            this.executeDrag(outline, node);
        }
    }

    protected abstract executeDrag(outline: Models.WidgetOutline,
                                   node: ReactiveManipulationNode): void;

    protected abstract executeResize(outline: Models.WidgetOutline,
                                     node: ReactiveManipulationNode): void;

    public abstract distToWall(node: ReactiveManipulationNode): number;

    public abstract visibleLength(node: ReactiveManipulationNode): number;

    public abstract pushNode(node: ReactiveManipulationNode,
                             distance: number): void;

    public abstract distanceBetween(ancestor: ReactiveManipulationNode,
                                    descendant: ReactiveManipulationNode): number;

    public abstract updateModel(model: Models.WidgetOutline,
                                position: number,
                                length: number): void;

    public abstract sortByPosition(nodes: ReactiveManipulationNode[],
                                   ancestorToDescendant: boolean): void;

    private exploreWithoutShrink(explored: Map<string, ReactiveManipulationNode>): number
    {
        let workList = new MaxPriorityQueue<ReactiveManipulationNode>();
        workList.enqueue(this.reactiveManipulationDAG.pressureBeforeShrink, this.reactiveManipulationDAG);
        let unallocatedPressure = 0;
        while (!workList.isEmpty())
        {
            let node = workList.dequeue();
            if (!explored.has(node.widgetId))
            {
                unallocatedPressure = node.pressureBeforeShrink - this.exploreNodeWithoutShrink(workList, node);
                explored.set(node.widgetId, node);
            }
        }

        return unallocatedPressure;
    }

    private exploreNodeWithoutShrink(prioQueue: MaxPriorityQueue<ReactiveManipulationNode>,
                                     node: ReactiveManipulationNode): number
    {
        let initialEmpties = node.numIndexesWithoutCollision;
        let currPressure   = 1;
        for (; currPressure <= node.pressureBeforeShrink; currPressure++)
        {
            let currEmpties = node.numIndexesWithoutCollision;
            if (currEmpties === 0) return node.pressureBeforeShrink;

            let collisions = this.processNextDistance(node, currPressure);
            if (collisions === null) // wall collision
            {
                if (initialEmpties === currEmpties) node.children = null; // no widgets between this one and the wall
                break;
            }

            let pressure = node.pressureBeforeShrink - currPressure + 1;
            for (let widgetId of collisions)
            {
                let collisionNode = this.nodeLookup.get(widgetId);
                if (!collisionNode) this.nodeLookup.set(widgetId, collisionNode = new ReactiveManipulationNode(this, widgetId, pressure, this.m_manipulator, this.direction));

                node.children.push(collisionNode);
                collisionNode.parents.push(node);

                if (pressure >= collisionNode.pressureBeforeShrink)
                {
                    collisionNode.pressureBeforeShrink = pressure;
                    prioQueue.enqueue(pressure, collisionNode);
                }
            }
        }

        return currPressure - 1;
    }

    private pushWithoutShrink(explored: Map<string, ReactiveManipulationNode>,
                              reducedPressure: number = 0)
    {
        if (this.forResize) this.reactiveManipulationDAG.length += this.reactiveManipulationDAG.pressureBeforeShrink;

        for (let node of explored.values()) this.pushNode(node, node.pressureBeforeShrink - reducedPressure);
    }

    private exploreShrinkPotential(nodes: ReactiveManipulationNode[])
    {
        this.sortByPosition(nodes, true);
        for (let node of nodes)
        {
            if (!node.children) continue;

            let cumPressure = node.shrinkable + node.cumAncestorShrinkability;
            for (let child of node.children)
            {
                if (cumPressure > child.cumAncestorShrinkability) child.cumAncestorShrinkability = cumPressure;
            }
        }
    }

    private exploreAfterShrink(prioQueue: MaxPriorityQueue<ReactiveManipulationNode>,
                               parentNode: ReactiveManipulationNode)
    {
        if (!parentNode.children) return;

        for (let node of parentNode.children)
        {
            node.length  = node.minLength;
            let pressure = Math.max(0, parentNode.pressureAfterShrink - this.distanceBetween(parentNode, node));
            if (pressure > node.pressureAfterShrink)
            {
                prioQueue.enqueue(node.pressureAfterShrink = pressure, node);
            }
            else if (pressure === 0 && pressure === node.pressureAfterShrink)
            {
                prioQueue.enqueue(node.pressureAfterShrink, node);
            }
        }
    }

    private pushWithShrink(): boolean
    {
        let workList                                     = new MaxPriorityQueue<ReactiveManipulationNode>();
        let explored                                     = new Map<string, ReactiveManipulationNode>();
        this.reactiveManipulationDAG.pressureAfterShrink = this.reactiveManipulationDAG.pressureBeforeShrink;
        this.exploreAfterShrink(workList, this.reactiveManipulationDAG);
        this.pushNode(this.reactiveManipulationDAG, this.reactiveManipulationDAG.pressureAfterShrink);
        if (this.forResize) this.reactiveManipulationDAG.length += this.reactiveManipulationDAG.pressureAfterShrink;
        while (!workList.isEmpty())
        {
            let node = workList.dequeue();
            if (!explored.has(node.widgetId))
            {
                this.exploreAfterShrink(workList, node);
                this.pushNode(node, node.pressureAfterShrink);
                if (this.distToWall(node) < 0) return false;

                explored.set(node.widgetId, node);
            }
        }

        let shiftedNodes = this.shiftAndStretch(Array.from(explored.values()));
        this.fixShifts(shiftedNodes);
        return true;
    }

    /**
     * returns nodes that were shifted
     *
     * @param nodes
     */
    private shiftAndStretch(nodes: ReactiveManipulationNode[]): ReactiveManipulationNode[]
    {
        this.sortByPosition(nodes, false);
        return nodes.filter((node) => this.shiftAndStretchNode(node) > 0);
    }

    /**
     * returns amount shifted
     *
     * @param node
     */
    private shiftAndStretchNode(node: ReactiveManipulationNode): number
    {
        // shift amount = min(dist to closest child, original expand amount, cum shrinkable)
        let firstObstacleDist = Number.MAX_SAFE_INTEGER;
        if (node.children)
        {
            for (let child of node.children) firstObstacleDist = Math.min(this.distanceBetween(node, child), firstObstacleDist);
        }
        else
        {
            firstObstacleDist = this.distToWall(node);
        }
        let localShrinkAmount = this.visibleLength(node) - node.length;
        let shiftAmount       = Math.max(0, Math.min(firstObstacleDist, this.reactiveManipulationDAG.pressureBeforeShrink, localShrinkAmount + node.cumAncestorShrinkability));
        if (shiftAmount > 0) this.pushNode(node, shiftAmount);

        // get stretch amount: then stretch
        let closestParentDist = Number.MAX_SAFE_INTEGER;
        for (let parent of node.parents) closestParentDist = Math.min(this.distanceBetween(parent, node), closestParentDist);
        node.length += Math.min(localShrinkAmount, closestParentDist);

        return shiftAmount;
    }

    private fixShifts(shiftedNodes: ReactiveManipulationNode[]): void
    {
        this.sortByPosition(shiftedNodes, true);
        for (let shiftedNode of shiftedNodes)
        {
            let distToNearestAncestor = Number.MAX_SAFE_INTEGER;
            for (let parent of shiftedNode.parents) distToNearestAncestor = Math.min(this.distanceBetween(parent, shiftedNode), distToNearestAncestor);

            if (distToNearestAncestor > 0)
            {
                this.pushNode(shiftedNode, -distToNearestAncestor);
                if (!shiftedNode.children) continue;
                for (let child of shiftedNode.children) shiftedNodes.push(shiftedNode);
            }
        }
    }

    /**
     * returns an array of newly collided widget indexes or null in the case that it reached a wall
     *
     * @param node
     * @param dist
     */
    private processNextDistance(node: ReactiveManipulationNode,
                                dist: number): string[]
    {
        let widgets: string[]  = [];
        let prevWidget: string = undefined;
        let outline            = node.visibleOutline;

        const nextSquareHasCollision = (row: number,
                                        col: number) =>
        {
            let widgetId = this.m_manipulator.gridManipulator.getEntry(row, col);
            if (widgetId === undefined) return false;
            if (widgetId === this.m_manipulator.manipulationId) return false;

            let prevManipulation = ReactiveManipulation.getPreviousManipulationNode(this.m_previousManipulations, widgetId);
            if (prevManipulation && !WidgetOutlineExtended.isIntersection(prevManipulation.projectedOutline, row, col)) return false;

            if (widgetId !== prevWidget)
            {
                widgets.push(widgetId);
                prevWidget = widgetId;
            }

            return true;
        };

        switch (this.direction)
        {
            case Direction.Down:
            case Direction.Up:
                let row = this.direction === Direction.Up ? outline.top - dist : outline.top + outline.height + dist - 1;
                if (row < 0) return null; // running off top of grid
                if (row >= this.m_manipulator.numRows) // running off bottom of grid
                {
                    if (this.m_manipulator.widgetGridConfig.numRows) return null; // static grid height: fail
                    this.m_manipulator.updateLayoutHeight(row + 1);
                }

                for (let noCollisionIdx of node.getIndexesWithoutCollision())
                {
                    if (nextSquareHasCollision(row, noCollisionIdx)) node.widgetCollision(noCollisionIdx);
                }
                break;

            case Direction.Left:
            case Direction.Right:
                let col = this.direction === Direction.Left ? outline.left - dist : outline.left + outline.width + dist - 1;
                if (col < 0 || col >= this.m_manipulator.widgetGridConfig.numCols) return null;

                for (let noCollisionIdx of node.getIndexesWithoutCollision())
                {
                    if (nextSquareHasCollision(noCollisionIdx, col)) node.widgetCollision(noCollisionIdx);
                }
                break;
        }

        return widgets;
    }

    private static getPreviousManipulationNode(previousManipulations: Manipulation[],
                                               widgetId: string): ManipulationNode
    {
        if (!previousManipulations || previousManipulations.length === 0) return null;

        for (let i = previousManipulations.length - 1; i >= 0; i--)
        {
            let prevManipulation = previousManipulations[i];
            if (prevManipulation.nodeLookup.has(widgetId)) return prevManipulation.nodeLookup.get(widgetId);
        }

        return null;
    }

    public static generate(manipulator: WidgetManipulator,
                           direction: Direction,
                           pressure: number,
                           forResize: boolean,
                           previousManipulations?: Manipulation[],
                           rootId: string = manipulator.manipulationId): ReactiveManipulation
    {
        let previousRoot                      = ReactiveManipulation.getPreviousManipulationNode(previousManipulations, rootId);
        let rootOutline: Models.WidgetOutline = previousRoot && previousRoot.projectedOutline;

        switch (direction)
        {
            case Direction.Up:
                return new ReactiveUpManipulation(manipulator, direction, pressure, forResize, rootId, rootOutline, previousManipulations);

            case Direction.Down:
                if (isNaN(manipulator.widgetGridConfig.numRows))
                {
                    return new LimitlessReactiveDownManipulation(manipulator, direction, pressure, forResize, rootId, rootOutline, previousManipulations);
                }
                return new ReactiveDownManipulation(manipulator, direction, pressure, forResize, rootId, rootOutline, previousManipulations);

            case Direction.Left:
                return new ReactiveLeftManipulation(manipulator, direction, pressure, forResize, rootId, rootOutline, previousManipulations);

            case Direction.Right:
                return new ReactiveRightManipulation(manipulator, direction, pressure, forResize, rootId, rootOutline, previousManipulations);
        }
    }
}

class ReactiveUpManipulation extends ReactiveManipulation
{
    protected executeDrag(outline: Models.WidgetOutline,
                          node: ReactiveManipulationNode)
    {
        outline.top = node.position;
    }

    protected executeResize(outline: Models.WidgetOutline,
                            node: ReactiveManipulationNode)
    {
        outline.top    = node.position;
        outline.height = node.length;
    }

    public visibleLength(node: ReactiveManipulationNode): number
    {
        return node.visibleOutline.height;
    }

    public distanceBetween(ancestor: ReactiveManipulationNode,
                           descendant: ReactiveManipulationNode): number
    {
        return ancestor.position - (descendant.position + descendant.length);
    }

    public distToWall(node: ReactiveManipulationNode): number
    {
        return node.position;
    }

    public pushNode(node: ReactiveManipulationNode,
                    distance: number): void
    {
        node.position -= distance;
    }

    public updateModel(model: Models.WidgetOutline,
                       position: number,
                       length: number): void
    {
        model.top    = position;
        model.height = length;
    }

    public sortByPosition(nodes: ReactiveManipulationNode[],
                          ancestorToDescendant: boolean)
    {
        nodes.sort(ancestorToDescendant ?
                       (nodeA: ReactiveManipulationNode,
                        nodeB: ReactiveManipulationNode) => nodeB.position - nodeA.position :
                       (nodeA: ReactiveManipulationNode,
                        nodeB: ReactiveManipulationNode) => nodeA.position - nodeB.position);
    }
}

class LimitlessReactiveDownManipulation extends ReactiveManipulation
{
    protected executeDrag(outline: Models.WidgetOutline,
                          node: ReactiveManipulationNode)
    {
        outline.top = node.position - node.length;
    }

    protected executeResize(outline: Models.WidgetOutline,
                            node: ReactiveManipulationNode)
    {
        outline.top    = node.position - node.length;
        outline.height = node.length;
    }

    public visibleLength(node: ReactiveManipulationNode): number
    {
        return node.visibleOutline.height;
    }

    public distanceBetween(ancestor: ReactiveManipulationNode,
                           descendant: ReactiveManipulationNode): number
    {
        return descendant.position - descendant.length - ancestor.position;
    }

    public distToWall(node: ReactiveManipulationNode): number
    {
        return Number.MAX_SAFE_INTEGER;
    }

    public pushNode(node: ReactiveManipulationNode,
                    distance: number): void
    {
        node.position += distance;
    }

    public updateModel(model: Models.WidgetOutline,
                       position: number,
                       length: number): void
    {
        model.top    = position - length;
        model.height = length;
    }

    public sortByPosition(nodes: ReactiveManipulationNode[],
                          ancestorToDescendant: boolean)
    {
        nodes.sort(ancestorToDescendant ?
                       (nodeA: ReactiveManipulationNode,
                        nodeB: ReactiveManipulationNode) => nodeA.position - nodeB.position :
                       (nodeA: ReactiveManipulationNode,
                        nodeB: ReactiveManipulationNode) => nodeB.position - nodeA.position);
    }
}

class ReactiveDownManipulation extends ReactiveManipulation
{
    protected executeDrag(outline: Models.WidgetOutline,
                          node: ReactiveManipulationNode)
    {
        outline.top = node.position - node.length;
    }

    protected executeResize(outline: Models.WidgetOutline,
                            node: ReactiveManipulationNode)
    {
        outline.top    = node.position - node.length;
        outline.height = node.length;
    }

    public visibleLength(node: ReactiveManipulationNode): number
    {
        return node.visibleOutline.height;
    }

    public distanceBetween(ancestor: ReactiveManipulationNode,
                           descendant: ReactiveManipulationNode): number
    {
        return descendant.position - descendant.length - ancestor.position;
    }

    public distToWall(node: ReactiveManipulationNode): number
    {
        return this.m_manipulator.widgetGridConfig.numRows - node.position;
    }

    public pushNode(node: ReactiveManipulationNode,
                    distance: number): void
    {
        node.position += distance;
    }

    public updateModel(model: Models.WidgetOutline,
                       position: number,
                       length: number): void
    {
        model.top    = position - length;
        model.height = length;
    }

    public sortByPosition(nodes: ReactiveManipulationNode[],
                          ancestorToDescendant: boolean)
    {
        nodes.sort(ancestorToDescendant ?
                       (nodeA: ReactiveManipulationNode,
                        nodeB: ReactiveManipulationNode) => nodeA.position - nodeB.position :
                       (nodeA: ReactiveManipulationNode,
                        nodeB: ReactiveManipulationNode) => nodeB.position - nodeA.position);
    }
}

class ReactiveLeftManipulation extends ReactiveManipulation
{
    protected executeDrag(outline: Models.WidgetOutline,
                          node: ReactiveManipulationNode)
    {
        outline.left = node.position;
    }

    protected executeResize(outline: Models.WidgetOutline,
                            node: ReactiveManipulationNode)
    {
        outline.left  = node.position;
        outline.width = node.length;
    }

    public visibleLength(node: ReactiveManipulationNode): number
    {
        return node.visibleOutline.width;
    }

    public distanceBetween(ancestor: ReactiveManipulationNode,
                           descendant: ReactiveManipulationNode): number
    {
        return ancestor.position - (descendant.position + descendant.length);
    }

    public distToWall(node: ReactiveManipulationNode): number
    {
        return node.position;
    }

    public pushNode(node: ReactiveManipulationNode,
                    distance: number): void
    {
        node.position -= distance;
    }

    public updateModel(model: Models.WidgetOutline,
                       position: number,
                       length: number): void
    {
        model.left  = position;
        model.width = length;
    }

    public sortByPosition(nodes: ReactiveManipulationNode[],
                          ancestorToDescendant: boolean)
    {
        nodes.sort(ancestorToDescendant ?
                       (nodeA: ReactiveManipulationNode,
                        nodeB: ReactiveManipulationNode) => nodeB.position - nodeA.position :
                       (nodeA: ReactiveManipulationNode,
                        nodeB: ReactiveManipulationNode) => nodeA.position - nodeB.position);
    }
}

class ReactiveRightManipulation extends ReactiveManipulation
{
    protected executeDrag(outline: Models.WidgetOutline,
                          node: ReactiveManipulationNode)
    {
        outline.left = node.position - node.length;
    }

    protected executeResize(outline: Models.WidgetOutline,
                            node: ReactiveManipulationNode)
    {
        outline.left  = node.position - node.length;
        outline.width = node.length;
    }

    public visibleLength(node: ReactiveManipulationNode): number
    {
        return node.visibleOutline.width;
    }

    public distanceBetween(ancestor: ReactiveManipulationNode,
                           descendant: ReactiveManipulationNode): number
    {
        return descendant.position - descendant.length - ancestor.position;
    }

    public distToWall(node: ReactiveManipulationNode): number
    {
        return this.m_manipulator.widgetGridConfig.numCols - node.position;
    }

    public pushNode(node: ReactiveManipulationNode,
                    distance: number): void
    {
        node.position += distance;
    }

    public updateModel(model: Models.WidgetOutline,
                       position: number,
                       length: number): void
    {
        model.left  = position - length;
        model.width = length;
    }

    public sortByPosition(nodes: ReactiveManipulationNode[],
                          ancestorToDescendant: boolean)
    {
        nodes.sort(ancestorToDescendant ?
                       (nodeA: ReactiveManipulationNode,
                        nodeB: ReactiveManipulationNode) => nodeA.position - nodeB.position :
                       (nodeA: ReactiveManipulationNode,
                        nodeB: ReactiveManipulationNode) => nodeB.position - nodeA.position);
    }
}

interface Manipulation
{
    valid: boolean;

    nodeLookup: Map<string, ManipulationNode>;
}
