import {Component, ElementRef, Injector, Input, Renderer2, SimpleChanges, ViewChild} from "@angular/core";
import * as chroma from "chroma-js";
import {ChartHelpers} from "framework/ui/charting/app-charting-utilities";
import {ChartTooltipComponent} from "framework/ui/charting/chart-tooltip.component";
import {Easing} from "framework/ui/charting/charting-animation";
import {Canvas, ITickInfo, Layer} from "framework/ui/charting/charting-framework";
import {PanZoom} from "framework/ui/charting/charting-interaction";
import {AxisAlignedBoundingBox, ChartingMath, Circle, Transform, Vector2} from "framework/ui/charting/charting-math";
import {BoxAnchor} from "framework/ui/charting/core/basics";
import {ChartColorUtilities, Fill} from "framework/ui/charting/core/colors";
import {PackingAlgorithms} from "framework/ui/charting/core/packing";
import {ChartFont, TextOrientation, TextPlacement} from "framework/ui/charting/core/text";
import {TilingAlgorithms} from "framework/ui/charting/core/tiling";
import {BaseComponent} from "framework/ui/components";
import {RelativeLocation} from "framework/ui/utils/relative-location-styles";

const WHITESPACE_MULTIPLIER = 1.05;

@Component({
               selector   : "o3-tree-chart",
               templateUrl: "./tree-chart.component.html",
               styleUrls  : ["./tree-chart.component.scss"]
           })
export class TreeChartComponent extends BaseComponent
{
    private static ids: number = 0;

    @Input() tree: TreeLike[];
    @Input() id: TreeStringAccessor<any>;
    @Input() weight: TreeNumericAccessor<any>;
    @Input() color: TreeStringAccessor<any>;
    @Input() label: TreeStringAccessor<any>;
    @Input() format: TreeStringAccessor<any>;
    @Input() description: TreeStringAccessor<any>;
    @Input() mode: TreeChartMode = TreeChartMode.BUBBLE;
    @Input() maxDepth: number    = Number.POSITIVE_INFINITY;
    @Input() viewport: Vector2   = null;

    @ViewChild("container", {static: true}) container: ElementRef;
    @ViewChild("tooltip", {static: true}) tooltip: ChartTooltipComponent;

    protected chartCanvas: Canvas;
    protected chartLayer: Layer;

    protected rootNode: TreeChartNode;

    private panZoom: PanZoom;
    private lastSelection: TreeChartNode;
    private lastHover: TreeChartNode;
    private lastCursor: Vector2;
    private font: ChartFont;

    public readonly tooltipPositions = [RelativeLocation.Top];
    public noData: boolean           = false;

    private showLegend: boolean;

    private ready = false;

    static nextId(): string
    {
        return `${this.ids++}`;
    }

    static depth(root: TreeLike,
                 level: number = 1): number
    {
        if (root.children?.length > 0)
        {
            let depth = level;

            for (let child of root.children) depth = Math.max(depth, this.depth(child, level + 1));

            return depth;
        }

        return level;
    }

    static nodes<T extends TreeLike>(roots: T[]): T[]
    {
        let nodes = roots;

        for (let root of roots)
        {
            if (root.children && root.children.length > 0)
            {
                for (let child of root.children)
                {
                    let childNodes = this.nodes([<T>child]);
                    nodes          = nodes.concat(childNodes);
                }
            }
        }

        return nodes;
    }

    static nodesInLevel(roots: TreeLike[],
                        level: number,
                        current: number = 0): TreeLike[]
    {
        let nodes: TreeLike[] = [];
        if (level === current) nodes = nodes.concat(roots);

        for (let root of roots)
        {
            if (root.children && root.children.length > 0)
            {
                for (let child of root.children)
                {
                    let childNodes = this.nodesInLevel([child], level, current + 1);
                    nodes          = nodes.concat(childNodes);
                }
            }
        }

        return nodes;
    }

    static nodesWithValue<T extends TreeLike>(nodes: T[],
                                              accessor: TreeNumericAccessor<T>): T[]
    {
        return nodes.filter((node) =>
                            {
                                return !isNaN(accessor(node));
                            });
    }

    static depthBasedColorScale<T extends TreeLike>(tree: T[],
                                                    start: string,
                                                    end: string): string[]
    {
        // Find the max depth
        let depth = 0;
        for (let root of tree)
        {
            depth = Math.max(depth, this.depth(root, 0));
        }

        // Build a color scale
        let scale = chroma.scale([
                                     start || "#696969",
                                     end || "#ffffff"
                                 ])
                          .mode("lch");

        // Generate colors based on number of levels
        return scale.colors(depth + 2);
    }

    constructor(inj: Injector,
                private renderer: Renderer2)
    {
        super(inj);

        // Set up chart font
        this.font = new ChartFont("#696969", undefined, 11);

        this.chartCanvas = new Canvas(200, 200);
        this.chartLayer  = this.chartCanvas.appendLayer();
        this.chartLayer.attachRenderCallback((ctx,
                                              info) => this.render(ctx, info));

        this.panZoom = new PanZoom(this.renderer, this.chartCanvas.interactionLayer.dom);
        this.panZoom.subscribe("change", (transform: Transform) =>
        {
            this.chartLayer.transform.reset();
            this.chartLayer.transform.translate(new Vector2(this.chartLayer.width / 2, this.chartLayer.height / 2));
            this.chartLayer.transform.append(transform);
            this.chartLayer.dirty = true;

            this.chartCanvas.interactionLayer.transform.setToMatch(this.chartLayer.transform);

            // Draw tooltip if it was previously rendered
            if (this.lastCursor) this.drawTooltip();
        });
        this.panZoom.subscribe("zoomToTick", () =>
        {
            this.rootNode.onFocusTick(Easing.easeInOutCubic(this.panZoom.zoomProgress()));
        });

        // Listen for mouse movement to render tooltips
        this.renderer.listen(this.chartCanvas.interactionLayer.dom, "mousemove", (event) => this.onMouseMove(event));
        this.renderer.listen(this.chartCanvas.interactionLayer.dom, "mouseleave", (event) => this.onMouseLeave(event));
        // Listen for double clicks to target bubbles
        this.renderer.listen(this.chartCanvas.interactionLayer.dom, "dblclick", (event) => this.onDoubleClick(event));
    }

    get renderContext(): CanvasRenderingContext2D
    {
        return this.chartLayer?.ctx;
    }

    ngOnInit()
    {
        super.ngOnInit();

        // Mount canvas elements in DOM
        this.chartCanvas.mount(this.container.nativeElement);
    }

    ngAfterViewInit()
    {
        super.ngAfterViewInit();

        // Flag as ready
        this.ready = true;

        // Always process after initializing
        this.process();
    }

    ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);

        // Re-process as long as chart is ready
        if (this.ready) this.process();
    }

    ngOnDestroy()
    {
        super.ngOnDestroy();

        // Remove and clean up canvas outputs
        this.chartCanvas.dispose();
    }

    protected afterLayoutChange(): void
    {
        this.resize();
    }

    resize()
    {
        if (!this.chartCanvas) return;

        let width  = this.container.nativeElement.clientWidth;
        let height = this.container.nativeElement.clientHeight;
        if (this.viewport)
        {
            width  = this.viewport.x;
            height = this.viewport.y;
        }

        this.chartCanvas.resize(width, height);

        // Update pan/zoom handler
        let chartViewport = this.viewport && new Vector2(width, height) || null;
        this.panZoom.targetChanged(false, chartViewport);
    }

    public process()
    {
        // Check if there is any valid data
        this.noData = !this.hasData();

        // If there is data, process and render
        if (!this.noData && !!this.mode)
        {
            // Set up based on mode
            let type: typeof TreeBubble | typeof TreeBox | typeof TreeArc;
            switch (this.mode)
            {
                case TreeChartMode.BUBBLE:
                    type = TreeBubble;
                    break;

                case TreeChartMode.BOX:
                    type = TreeBox;
                    break;

                case TreeChartMode.SUNBURST:
                case TreeChartMode.PIEBURST:
                    type = TreeArc;
                    break;
            }

            this.showLegend           = false;
            this.panZoom.interactable = type !== TreeArc;

            this.resize();

            // Wrap the tree nodes
            if (this.tree.length > 1)
            {
                // Make a fake root
                this.rootNode          = new type(this, null, null, null, null, null, null, null, null, null, null, -1);
                // Add the tree nodes as children
                this.rootNode.children = this.tree.map((node) => new type(this,
                                                                          this.rootNode,
                                                                          node,
                                                                          this.id,
                                                                          this.weight,
                                                                          this.color,
                                                                          this.label,
                                                                          this.format,
                                                                          this.description,
                                                                          true,
                                                                          this.maxDepth,
                                                                          0));
            }
            else if (this.tree.length === 1 && this.tree[0].children && this.maxDepth === 0)
            {
                // Make a fake root
                this.rootNode          = new type(this, null, null, null, null, null, null, null, null, null, null, -1);
                // Add the tree nodes as children
                this.rootNode.children = this.tree[0].children.map((node) => new type(this,
                                                                                      this.rootNode,
                                                                                      node,
                                                                                      this.id,
                                                                                      this.weight,
                                                                                      this.color,
                                                                                      this.label,
                                                                                      this.format,
                                                                                      this.description,
                                                                                      true,
                                                                                      this.maxDepth,
                                                                                      0));
            }
            else
            {
                // Use the first and only node as the root
                this.rootNode = new type(this, null, this.tree[0], this.id, this.weight, this.color, this.label, this.format, this.description, false, this.maxDepth, 0);
            }

            // Flag as pie if in a pie-type mode
            if (this.mode === TreeChartMode.PIEBURST) (<TreeArc>this.rootNode).setPieMode(true);

            // Update canvas debug flags to reflect state
            this.chartCanvas.setDebugState("mode", this.mode);
            this.chartCanvas.setDebugState("depth", `${this.maxDepth}`);

            // Reset the tree
            this.reset();
        }
        else
        {
            // If no data, clear bubbles
            this.rootNode = null;
        }

        // Mark as dirty always
        this.chartLayer.dirty = true;

        this.chartCanvas.ticker.start();
    }

    public markForRendering()
    {
        this.chartLayer.dirty = true;
    }

    public selectNode(node: TreeChartNode = null)
    {
        // Do nothing if no root or cannot select
        if (!this.rootNode || !node?.canSelect) return;

        // Clear any previous selection flag
        if (this.lastSelection)
        {
            this.lastSelection.setIsSelectedState(false, false);
        }

        // Select the given node or clear if none given
        if (node && node !== this.lastSelection)
        {
            this.lastSelection = node;
            this.lastSelection.setIsSelectedState(true, false);
            this.lastSelection.onFocus(this.rootNode);

            this.resetView(false, this.lastSelection);
        }
        else
        {
            this.lastSelection.onFocusClear(this.rootNode);
            this.lastSelection = null;

            this.resetView(false, this.rootNode);
        }
    }

    public highlightNode(node: TreeChartNode = null,
                         recursive: boolean  = false)
    {
        // Do nothing if no root
        if (!this.rootNode) return;

        // Highlight the given node
        if (node)
        {
            // Mute all nodes
            this.rootNode.setMutedState(true, true);
            this.rootNode.setHighlightState(false, true);

            // Unmute the branches under node and flag the given node as highlighted
            node.setMutedState(false, recursive);
            node.setHighlightState(true, recursive);
        }
        else
        {
            // Unmute and unhighlight all nodes
            this.rootNode.setMutedState(false, true);
            this.rootNode.setHighlightState(false, true);
        }

        // Trigger visual update rendering
        this.markForRendering();
    }

    public disableNode(node: TreeChartNode)
    {
        // Do nothing if no root
        if (!this.rootNode) return;

        // Disable node an re-process
        node.setIsDisabledState(true, false);
        this.reset();
    }

    public enableNode(node: TreeChartNode)
    {
        // Do nothing if no root
        if (!this.rootNode) return;

        // Enable node an re-process
        node.setIsDisabledState(false, false);
        this.reset();
    }

    private reset()
    {
        // Prune children
        this.rootNode.prune();

        // Prepare the nodes for rendering
        this.rootNode.prepare(this.chartLayer.ctx, this.chartCanvas.aabb);

        // Calulculate zoom bounds
        let bounds = this.rootNode.zoomBounds();

        // Special cases by visualization type
        switch (this.mode)
        {
            case TreeChartMode.BUBBLE:
                bounds.y *= 3;
                break;

            case TreeChartMode.BOX:
                // Make sure min zoom fits the entire box
                let minW = this.chartCanvas.aabb.width / (this.rootNode.box().width * WHITESPACE_MULTIPLIER);
                let minH = this.chartCanvas.aabb.height / (this.rootNode.box().height * WHITESPACE_MULTIPLIER);
                if (!isNaN(minW) && !isNaN(minH)) bounds.x = Math.min(bounds.x, minW, minH);
                break;
        }

        // Apply zoom bounds
        this.panZoom.options.minZoom = bounds.x;
        this.panZoom.options.maxZoom = bounds.y;

        // Re-center the chart
        this.resetView(true, this.rootNode);
    }

    private render(ctx: CanvasRenderingContext2D,
                   info: ITickInfo)
    {
        if (this.rootNode)
        {
            this.rootNode.render(ctx, this.panZoom, this.rootNode.level);
        }
    }

    private onMouseMove(event: MouseEvent)
    {
        // Abort if no data
        if (this.noData) return;

        // Save the last cursor position
        this.lastCursor = new Vector2(event.offsetX, event.offsetY);

        // Try to render a tooltip
        this.drawTooltip();
    }

    private onMouseLeave(event: MouseEvent)
    {
        // Abort if no data
        if (this.noData) return;

        // Close any open tooltip
        this.closeTooltip();
    }

    private onDoubleClick(event: MouseEvent)
    {
        // Abort if no data
        if (this.noData) return;

        // Find if any bubbles were clicked
        let position = this.chartCanvas.interactionLayer.unproject(new Vector2(event.offsetX, event.offsetY));
        let hits     = this.rootNode.hitList(position);
        if (hits.length > 0)
        {
            // Grab the deepest element in the tree that was hit
            let deepest = hits[0];

            // Try (toggle) targeting the deepest node
            this.selectNode(deepest);
        }
        else
        {
            this.selectNode();
        }
    }

    private resetView(hardReset: boolean,
                      node: TreeChartNode)
    {
        // Calculate a view box with margin
        let view = node.box();
        view.scale(WHITESPACE_MULTIPLIER);

        // If doing a hard reset, first center and zoom out camera
        if (hardReset)
        {
            let start = view.clone();
            start.scale(4);
            this.panZoom.resetTo(start);
        }

        // Animate to fit view box
        this.panZoom.zoomTo(view, 1000);
    }

    private drawTooltip()
    {
        let offset  = null;
        let content = null;
        let node    = null;

        // Unflag last hovered
        if (this.lastHover)
        {
            this.lastHover = null;
        }

        // Unproject and hit test
        let position = this.chartCanvas.interactionLayer.unproject(this.lastCursor);
        let hits     = this.rootNode.hitList(position);

        if (hits.length > 0)
        {
            // Grab the deepest hit
            node = hits[0];

            // Flag the hovered bubble
            this.lastHover = node;
            // Find the tooltip offset
            offset         = this.getTooltipOffset(node, position);
            // Find the tooltip label
            if (hits[0].label) content = this.getTooltipContent(node);
        }

        // If valid offset and valid label draw the tooltip, otherwise close any tooltip
        if (node && offset && content)
        {
            // Highlight the node
            this.highlightNode(node, false);

            // Draw the tooltip
            this.tooltip.render(offset.x, offset.y, content);
        }
        else
        {
            // Unhighlight the node
            this.highlightNode(null, true);

            // Close the tooltip
            this.closeTooltip();
        }
    }

    private getTooltipContent(node: TreeChartNode): string
    {
        let content = "";
        if (node.label)
        {
            content = `<div><span class="chart-prop">Label:</span><span> ${node.label(node.node)}</span></div>`;
            if (node.weight) content = `${content}<div><span class="chart-prop">Value:</span><span> ${node.format ? node.format(node.node) : node.weight(node.node)}</span></div>`;
            if (node instanceof TreeArc)
            {
                content = `${content}<div><span class="chart-prop">Percentage:</span><span> ${(node.percent * 100).toFixed(2)}%</span></div>`;
                if (node.level > 0) content = `${content}<div><span class="chart-prop">Relative Percentage:</span><span> ${(node.percentRelative * 100).toFixed(2)}%</span></div>`;
            }
            if (node.description) content = `${content}<div><span>${node.description(node.node)}</span></div>`;
            if (node.hiddenCount) content = `${content}<div><span class="chart-info">${node.hiddenCount} children without values are hidden</span></div>`;
        }

        return content;
    }

    private getTooltipOffset(node: TreeChartNode,
                             cursor: Vector2): Vector2
    {
        // Get the offset for the default node anchor point
        let offset = this.chartCanvas.interactionLayer.project(node.anchor());
        // If top of node off screen, use cursor position
        if (!this.chartCanvas.aabb.hit(offset)) offset = this.chartCanvas.interactionLayer.project(cursor);
        // If it is still off screen return null
        if (!this.chartCanvas.aabb.hit(offset)) offset = null;

        // Return the offset
        return offset;
    }

    private closeTooltip()
    {
        // Unhighlight the node
        this.highlightNode(null, true);

        this.lastCursor = null;
        this.tooltip.remove();
    }

    private hasData(): boolean
    {
        for (let root of this.tree)
        {
            if (this.weight && this.weight(root)) return true;
        }

        return false;
    }
}

export abstract class TreeChartNode implements TreeLike
{
    protected m_children: TreeChartNode[]         = [];
    protected m_filteredChildren: TreeChartNode[] = [];

    public get children(): TreeChartNode[]
    {
        return this.m_filteredChildren;
    }

    public set children(nodes: TreeChartNode[])
    {
        this.m_children         = nodes;
        this.m_filteredChildren = nodes;
    }

    public identifier: string;
    public hiddenCount: number = 0;

    // State flags
    public isZero: boolean     = false;
    public isInvalid: boolean  = false;
    public isDisabled: boolean = false;
    public isSelected: boolean = false;
    public isHovered: boolean  = false;

    // Visual state flags
    public selected: boolean    = false;
    public highlighted: boolean = false;
    public muted: boolean       = false;

    public get canSelect(): boolean
    {
        return true;
    }

    // Visual state colors
    protected baseColor: string;

    protected fillColor: string;
    protected selectedFillColor: string;
    protected highlightedFillColor: string;
    protected mutedFillColor: string;

    protected strokeColor: string;
    protected selectedStrokeColor: string;
    protected highlightedStrokeColor: string;
    protected mutedStrokeColor: string;

    constructor(public chart: TreeChartComponent,
                public parent: TreeChartNode,
                public node: TreeLike,
                public id: TreeStringAccessor<any>,
                public weight: TreeNumericAccessor<any>,
                public color: TreeStringAccessor<any>,
                public label: TreeStringAccessor<any>,
                public format: TreeStringAccessor<any>,
                public description: TreeStringAccessor<any>,
                public multiroot: boolean,
                public maxLevel: number,
                public level: number)
    {
        // Adopt a unique id
        this.identifier = this.id ? this.id(this.node) : null;
        if (this.identifier === null) this.identifier = TreeChartComponent.nextId();
    }

    public prune()
    {
        // Operate on unfiltered child list, reset hidden count
        this.hiddenCount = 0;

        // Update child isZero and isInvalid flags
        for (let child of this.m_children)
        {
            child.isInvalid = !child.weight || isNaN(child.weight(child.node));
            child.isZero    = !child.isInvalid && child.weight(child.node) === 0;
        }

        // Filter out invalid, zero and disabled children
        this.m_filteredChildren = [];
        for (let child of this.m_children)
        {
            if (!child.isInvalid && !child.isZero && !child.isDisabled)
            {
                this.m_filteredChildren.push(child);
            }
            else if (child.isZero || child.isInvalid)
            {
                this.hiddenCount++;
            }
        }

        // Recurse and prune filtered children
        for (let child of this.m_filteredChildren)
        {
            child.prune();
        }
    }

    public hitList(position: Vector2): TreeChartNode[]
    {
        if (this.hit(position))
        {
            // Initialize hit list for this level
            let list = [];
            // Add hit children
            for (let child of this.children) list.push(...child.hitList(position));
            // Add this node
            list.push(this);
            // Return all hit nodes
            return list;
        }

        // Return no hit nodes
        return [];
    }

    protected colorize()
    {
        // If no node data this must be a logical grouping node, style accordingly
        if (this.color && this.node)
        {
            // Calculate fill and stroke color
            this.fillColor   = this.color(this.node);
            this.strokeColor = ChartColorUtilities.safeChroma(this.fillColor)
                                                  .darken(0.25)
                                                  .hex();

            // Walk to find base color
            let current = this.parent;
            while (current)
            {
                if (current.baseColor)
                {
                    this.baseColor = current.baseColor;
                    break;
                }
                else if (current.fillColor)
                {
                    this.baseColor = current.fillColor;
                    break;
                }

                current = current.parent;
            }

            // If no base color was found, this is the first colored node so it is the base color
            if (!this.baseColor) this.baseColor = this.fillColor;

            // Use base color to get focus and hover colors
            if (this.baseColor)
            {
                this.highlightedFillColor   = ChartColorUtilities.safeChroma(this.baseColor)
                                                                 .saturate(0.5)
                                                                 .hex();
                this.selectedFillColor      = ChartColorUtilities.safeChroma(this.baseColor)
                                                                 .saturate(0.5)
                                                                 .darken(0.33)
                                                                 .hex();
                this.mutedFillColor         = ChartColorUtilities.safeChroma("#d8d8d8")
                                                                 .desaturate(2.9)
                                                                 .brighten(0.5)
                                                                 .hex();
                this.highlightedStrokeColor = ChartColorUtilities.safeChroma(this.highlightedFillColor)
                                                                 .darken(0.5)
                                                                 .hex();
                this.selectedStrokeColor    = ChartColorUtilities.safeChroma(this.selectedFillColor)
                                                                 .darken(0.5)
                                                                 .hex();
                this.mutedStrokeColor       = ChartColorUtilities.safeChroma(this.mutedFillColor)
                                                                 .darken(0.5)
                                                                 .hex();
            }

        }
    }

    public setSelectedState(value: boolean,
                            recursive: boolean)
    {
        this.selected = value;

        if (recursive)
        {
            for (let child of this.children) child.setSelectedState(value, recursive);
        }
    }

    public setHighlightState(value: boolean,
                             recursive: boolean)
    {
        this.highlighted = value;

        if (recursive)
        {
            for (let child of this.children) child.setHighlightState(value, recursive);
        }
    }

    public setMutedState(value: boolean,
                         recursive: boolean)
    {
        this.muted = value;

        if (recursive)
        {
            for (let child of this.children) child.setMutedState(value, recursive);
        }
    }

    public clearVisualStates(recursive: boolean)
    {
        this.setMutedState(false, false);
        this.setHighlightState(false, false);
        this.setSelectedState(false, false);

        if (recursive)
        {
            for (let child of this.children) child.clearVisualStates(recursive);
        }
    }

    public setIsSelectedState(value: boolean,
                              recursive: boolean)
    {
        this.isSelected = value;

        if (recursive)
        {
            for (let child of this.children) child.setIsSelectedState(value, recursive);
        }
    }

    public setisHoveredState(value: boolean,
                             recursive: boolean)
    {
        this.isHovered = value;

        if (recursive)
        {
            for (let child of this.children) child.setisHoveredState(value, recursive);
        }
    }

    public setIsDisabledState(value: boolean,
                              recursive: boolean)
    {
        this.isDisabled = value;

        if (recursive)
        {
            for (let child of this.children) child.setIsDisabledState(value, recursive);
        }
    }

    public clearStates(recursive: boolean)
    {
        this.setIsSelectedState(false, false);
        this.setisHoveredState(false, false);
        this.setIsDisabledState(false, false);

        if (recursive)
        {
            for (let child of this.children) child.clearStates(recursive);
        }
    }

    public abstract hit(position: Vector2): boolean;

    public abstract box(): AxisAlignedBoundingBox;

    public abstract anchor(): Vector2;

    public abstract prepare(ctx: CanvasRenderingContext2D,
                            hint: AxisAlignedBoundingBox): void;

    public abstract zoomBounds(bounds?: Vector2): Vector2;

    public abstract onFocus(root: TreeChartNode): void;

    public abstract onFocusTick(percent: number): void;

    public abstract onFocusClear(root: TreeChartNode): void;

    public abstract render(ctx: CanvasRenderingContext2D,
                           panZoom: PanZoom,
                           level: number): void;
}

class TreeBubble extends TreeChartNode
{
    public source: Circle    = new Circle(0, 0, 0);
    public enclosure: Circle = new Circle(0, 0, 0);
    public display: Circle   = new Circle(0, 0, 0);

    protected m_children: TreeBubble[]         = [];
    protected m_filteredChildren: TreeBubble[] = [];

    public get children(): TreeBubble[]
    {
        return this.m_filteredChildren;
    }

    public set children(nodes: TreeBubble[])
    {
        this.m_children         = nodes;
        this.m_filteredChildren = nodes;
    }

    private m_fill: Fill;
    private m_fillHover: Fill;
    private m_fillFocus: Fill;
    private m_fillMuted: Fill;
    private m_stroke: string;
    private m_strokeHover: string;
    private m_strokeFocus: string;
    private m_strokeMuted: string;
    private m_width: number         = 1;
    private m_dash: number[]        = [];
    private m_value: string         = "";
    private m_label: string         = "";
    private m_labelIn: Vector2      = null;
    private m_labelOut: Vector2     = null;
    private m_font: ChartFont       = null;
    private m_helper: ChartHelpers  = new ChartHelpers();
    private m_helper2: ChartHelpers = null;

    constructor(public chart: TreeChartComponent,
                public parent: TreeChartNode,
                public node: TreeLike,
                public id: TreeStringAccessor<any>,
                public weight: TreeNumericAccessor<any>,
                public color: TreeStringAccessor<any>,
                public label: TreeStringAccessor<any>,
                public format: TreeStringAccessor<any>,
                public description: TreeStringAccessor<any>,
                public multiroot: boolean,
                public maxLevel: number,
                public level: number)
    {
        // Call parent constructor
        super(chart, parent, node, id, weight, color, label, format, description, multiroot, maxLevel, level);

        // Recurse and build child nodes
        if (this.node && this.node.children && this.level < this.maxLevel)
        {
            this.children = node.children.map((node) =>
                                              {
                                                  return new TreeBubble(chart, this, node, id, weight, color, label, format, description, multiroot, this.maxLevel, this.level + 1);
                                              });
        }
    }

    public hit(position: Vector2): boolean
    {
        return this.display.distanceToVectorSquared(position) <= this.display.radiusSquared;
    }

    public box()
    {
        return this.display.getBoundingBox();
    }

    public anchor(): Vector2
    {
        let box = this.box();
        return new Vector2(box.center().x, box.top);
    }

    public prepare()
    {
        // Compute tree layout
        this.compute();

        // Configure tree for display
        this.position(new Circle(0, 0, 1000), this.source);
        this.colorize();
        this.describe();
    }

    public zoomBounds(bounds?: Vector2): Vector2
    {
        if (!bounds)
        {
            bounds = new Vector2(this.m_labelOut.x, this.m_labelOut.y);
        }
        else
        {
            if (this.m_labelOut.x < bounds.x) bounds.x = this.m_labelOut.x;
            if (this.m_labelOut.x !== Infinity && this.m_labelOut.y > bounds.y) bounds.y = this.m_labelOut.y;
        }

        // Check all children as well
        this.children.forEach((bubble) => { bubble.zoomBounds(bounds); });

        // Return the final bounds
        return bounds;
    }

    public onFocus(root: TreeBubble)
    {
        // Nothing for now...
    }

    public onFocusTick(percent: number)
    {
        // Nothing for now...
    }

    public onFocusClear(root: TreeBubble)
    {
        // Nothing for now...
    }

    public render(ctx: CanvasRenderingContext2D,
                  panZoom: PanZoom,
                  level: number = 0)
    {
        // Calculate a zoom level factor for scaling some properties
        let factor = 1;
        if (panZoom.scale < this.m_labelIn.x)
        {
            factor = 0;
        }
        else if (panZoom.scale <= this.m_labelIn.y)
        {
            factor = (panZoom.scale - this.m_labelIn.x) / (this.m_labelIn.y - this.m_labelIn.x);
        }
        else if (panZoom.scale > this.m_labelOut.y)
        {
            factor = 0;
        }
        else if (panZoom.scale >= this.m_labelOut.x)
        {
            factor = 1 - ((panZoom.scale - this.m_labelOut.x) / (this.m_labelOut.y - this.m_labelOut.x));
        }

        // Render the base circle
        this.renderCircle(ctx, panZoom, factor, level);

        // Render all children
        this.children.forEach((bubble) => { bubble.render(ctx, panZoom, level + 1); });

        // Render the text labels
        this.renderText(ctx, panZoom, factor);
    }

    private compute()
    {
        // Update source circle
        this.source.x      = 0;
        this.source.y      = 0;
        this.source.radius = this.weight ? this.weight(this.node) : 1;

        // Compute for children first
        this.children.forEach((bubble) => { bubble.compute(); });

        // Pack the child circles
        let descendants = this.children.map((bubble) => { return bubble.source; });
        PackingAlgorithms.packSiblings(descendants);

        // Enclose the child circles
        this.enclosure = PackingAlgorithms.enclose(descendants);
    }

    private position(actual: Circle,
                     enclosure: Circle,
                     padding: number = 15)
    {
        // TODO: Compute and cache color and label in this method

        // Find the padding value to use (in px)
        let shrink = Math.min(actual.radius, padding);

        // Shrink the actual by the padding amount
        actual = new Circle(actual.x, actual.y, actual.radius - shrink);

        let delta = actual.differenceVector(enclosure);
        let scale = actual.radius / enclosure.radius;

        // Copy source circle to display circle
        this.display.copy(this.source);

        // Move the circle by the amount between the enclosure and the actual
        this.display.x += delta.x;
        this.display.y += delta.y;

        // Measure the difference between the actual circle and this circle
        let delta2 = actual.differenceVector(this.display);

        // Scale the difference by the scale factor
        delta2.scale(scale);

        // Move the display circle to the scaled offset from the actual circle
        this.display.x = actual.x + delta2.x;
        this.display.y = actual.y + delta2.y;

        // Scale the circle by the scale factor
        this.display.radius *= scale;

        // Configure all child bubbles
        this.children.forEach((bubble) =>
                              {
                                  bubble.position(this.display, this.enclosure);
                              });
    }

    protected colorize()
    {
        super.colorize();

        // Adopt main colors if fill color given
        if (this.fillColor)
        {
            this.m_fill   = this.fillColor;
            this.m_stroke = this.strokeColor;
        }
        else
        {
            this.m_fill   = "transparent";
            this.m_stroke = "transparent";
        }

        // Adopt additional colors if base color given
        if (this.baseColor)
        {
            this.m_fillFocus   = this.selectedFillColor;
            this.m_fillHover   = this.highlightedFillColor;
            this.m_fillMuted   = this.mutedFillColor;
            this.m_strokeHover = this.highlightedStrokeColor;
            this.m_strokeFocus = this.selectedStrokeColor;
            this.m_strokeMuted = this.mutedStrokeColor;
        }
        else
        {
            this.m_fillFocus   = "transparent";
            this.m_fillHover   = "transparent";
            this.m_fillMuted   = "transparent";
            this.m_strokeHover = "transparent";
            this.m_strokeFocus = "transparent";
            this.m_strokeMuted = "transparent";
        }

        // Colorize children as well
        for (let child of this.children) child.colorize();
    }

    private describe()
    {
        // Get label from label accessor
        this.m_label = this.label ? this.label(this.node) : "";

        // Make the value all pretty
        if (this.weight)
        {
            this.m_value = Number.parseFloat(`${this.weight(this.node)}`)
                                 .toPrecision(4);
        }
        else
        {
            this.m_value = "---";
        }

        // Make a font to use
        this.m_font       = new ChartFont();
        this.m_font.color = "black";
        this.m_font.size  = Math.max(Math.sqrt(this.display.radius) * 5, 7);

        // Measure the text
        let box = this.m_helper.placeTextInBox(this.m_font, TextPlacement.Center, TextOrientation.Horizontal, this.m_label, this.display.x, this.display.y, BoxAnchor.Center, 0);

        // Adjust font size to fit label to bubble
        this.m_font.size = Math.min(this.m_font.size, this.m_font.size / (box.width / ((this.display.radius * 2) - (this.display.radius * 0.2))));

        // Describe children as well
        for (let child of this.children) child.describe();

        // Configure label zoom bounds
        if (this.children && this.children.length > 0)
        {
            this.m_labelIn  = new Vector2(5 / this.m_font.size, 7 / this.m_font.size);
            this.m_labelOut = this.firstChildLabelIn();

            if (this.m_labelOut.x < this.m_labelIn.x)
            {
                let min = this.m_labelOut.x;
                let max = Math.max(this.m_labelIn.y, this.m_labelOut.y);
                let mid = min + ((max - min) / 2);

                this.m_labelIn.x  = min;
                this.m_labelIn.y  = mid;
                this.m_labelOut.x = mid;
                this.m_labelOut.y = max;
            }
        }
        else
        {
            this.m_labelIn  = new Vector2(5 / this.m_font.size, 7 / this.m_font.size);
            this.m_labelOut = new Vector2(Infinity, Infinity);
        }
    }

    private renderCircle(ctx: CanvasRenderingContext2D,
                         panZoom: PanZoom,
                         factor: number,
                         level: number = 0)
    {
        // Configure fill and stroke colors
        ctx.fillStyle   = this.m_fill;
        ctx.strokeStyle = this.m_stroke;
        if (this.node)
        {
            if (this.highlighted)
            {
                ctx.fillStyle   = this.m_fillHover;
                ctx.strokeStyle = this.m_strokeHover;
            }
            else if (this.selected)
            {
                ctx.fillStyle   = this.m_fillFocus;
                ctx.strokeStyle = this.m_strokeFocus;
            }
            else if (this.muted)
            {
                ctx.fillStyle   = this.m_fillMuted;
                ctx.strokeStyle = this.m_strokeMuted;
            }
        }

        // Compute stroke width
        ctx.lineWidth = (this.m_width / panZoom.scale) * (1 + (2 * factor / 3));
        if (this.selected)
        {
            ctx.lineWidth *= 2;
        }

        // Configure remaining properties
        ctx.setLineDash(this.m_dash);
        ctx.globalAlpha = 1;

        ctx.beginPath();
        ctx.arc(this.display.x, this.display.y, this.display.radius, 0, Math.PI * 2, true);
        ctx.closePath();
        ctx.fill();
        ctx.stroke();
        ctx.globalAlpha = 1;
    }

    private renderText(ctx: CanvasRenderingContext2D,
                       panZoom: PanZoom,
                       factor: number)
    {
        // Cache text rendering helper if not cached
        if (!this.m_helper2) this.m_helper2 = new ChartHelpers(ctx);

        // Only render text if visible
        if (panZoom.scale >= this.m_labelIn.x && panZoom.scale <= this.m_labelOut.y)
        {

            let opacity = factor * factor;
            let blur    = 0;

            if (panZoom.scale <= this.m_labelIn.y) blur = (1 - factor) * 2;
            if (panZoom.scale >= this.m_labelOut.x) blur = (1 - factor) * 2.25;

            ctx.globalAlpha = opacity;
            this.m_helper2.drawTextInBox(this.m_font,
                                         TextPlacement.Center,
                                         TextOrientation.Horizontal,
                                         this.m_label,
                                         this.m_font.color,
                                         this.display.x,
                                         this.display.y,
                                         BoxAnchor.Center,
                                         0,
                                         "transparent",
                                         blur);
            ctx.globalAlpha = 1;
        }
    }

    private firstChildLabelIn(): Vector2
    {
        if (this.children && this.children.length > 0)
        {
            let min = this.children[0].m_labelIn;
            for (let child of this.children)
            {
                if (child.m_labelIn.x < min.x) min = child.m_labelIn;
            }

            return min.clone();
        }

        return null;
    }
}

class TreeArc extends TreeChartNode
{
    private static size            = 120;
    private static fallOff         = 0.5;
    private static donutWhitespace = 0.5;

    protected m_children: TreeArc[]         = [];
    protected m_filteredChildren: TreeArc[] = [];

    public get children(): TreeArc[]
    {
        return this.m_filteredChildren;
    }

    public set children(nodes: TreeArc[])
    {
        this.m_children         = nodes;
        this.m_filteredChildren = nodes;
    }

    public focusBranch: boolean     = false;
    public focusLevel: number       = -1;
    public baseArc: Arc             = new Arc(0, Math.PI * 2, 0, 0);
    public displayArc: Arc          = new Arc(0, Math.PI * 2, 0, 0);
    public previousArc: Arc         = new Arc(0, Math.PI * 2, 0, 0);
    public currentArc: Arc          = new Arc(0, Math.PI * 2, 0, 0);
    public percent: number          = null;
    public percentRelative: number  = null;
    public displayCallout: Callout  = null;
    public previousCallout: Callout = null;
    public currentCallout: Callout  = null;
    public isPie: boolean           = false;

    public get canSelect(): boolean
    {
        return this.level < this.maxLevel;
    }

    private m_fill: Fill;
    private m_fillHover: Fill;
    private m_fillFocus: Fill;
    private m_fillMuted: Fill;
    private m_maxDepth                        = 0;
    private m_value: string                   = "";
    private m_label: string                   = "";
    private m_font: ChartFont                 = null;
    private m_subFont: ChartFont              = null;
    private m_helper: ChartHelpers            = null;
    private m_baseBox: AxisAlignedBoundingBox = null;
    private m_box: AxisAlignedBoundingBox     = null;

    private static radii(level: number,
                         startLevel: number,
                         maxLevel: number,
                         isPie: boolean): Vector2
    {
        // We will take from a fixed size circle for consistent zoom to ease in text space calculation

        // First find the effective number of levels displayed and the effective level
        let effectiveLevels = Math.max(0, (maxLevel - startLevel)) + 1;
        let effectiveLevel  = Math.max(0, (level - startLevel));

        // Get the empty space and non-empty space
        let whitespace = TreeArc.size * (isPie ? 0 : TreeArc.donutWhitespace);
        let space      = TreeArc.size - whitespace;

        // Use formula for finite geometric series to find min and max values
        let min = whitespace + space * ((1 - Math.pow(TreeArc.fallOff, effectiveLevel)) / (1 - Math.pow(TreeArc.fallOff, effectiveLevels)));
        let max = whitespace + space * ((1 - Math.pow(TreeArc.fallOff, effectiveLevel + 1)) / (1 - Math.pow(TreeArc.fallOff, effectiveLevels)));

        // Return a vector representing the radii span of the arc
        return new Vector2(min, max);
    }

    constructor(public chart: TreeChartComponent,
                public parent: TreeChartNode,
                public node: TreeLike,
                public id: TreeStringAccessor<any>,
                public weight: TreeNumericAccessor<any>,
                public color: TreeStringAccessor<any>,
                public label: TreeStringAccessor<any>,
                public format: TreeStringAccessor<any>,
                public description: TreeStringAccessor<any>,
                public multiroot: boolean,
                public maxLevel: number,
                public level: number)
    {
        // Call parent constructor
        super(chart, parent, node, id, weight, color, label, format, description, multiroot, maxLevel, level);

        // Capture a base bounding box
        let halfDimension = TreeArc.radii(this.m_maxDepth, 0, this.m_maxDepth, this.isPie).y * 1.25; // 25% padding
        let dimension     = halfDimension * 2;
        this.m_baseBox    = new AxisAlignedBoundingBox(-halfDimension, -halfDimension, dimension, dimension);
        this.m_box        = this.m_baseBox.clone();

        // Cache helper
        this.m_helper = new ChartHelpers(chart.renderContext);

        // Make fonts to use
        this.m_font       = new ChartFont();
        this.m_font.color = "black";
        this.m_font.size  = 11;

        this.m_subFont       = new ChartFont();
        this.m_subFont.color = "black";
        this.m_subFont.size  = 7;

        // Set up initial callout states
        this.displayCallout  = new Callout(this.m_font, this.m_subFont);
        this.previousCallout = new Callout(this.m_font, this.m_subFont);
        this.currentCallout  = new Callout(this.m_font, this.m_subFont);

        // Recurse and build child nodes
        if (this.node?.children && this.level < this.maxLevel)
        {
            this.children = node.children.map((node) =>
                                              {
                                                  return new TreeArc(chart, this, node, id, weight, color, label, format, description, multiroot, maxLevel, this.level + 1);
                                              });
        }
    }

    get effectiveFocusLevel(): number
    {
        return Math.max(0, this.focusLevel);
    }

    public setPieMode(isPie: boolean)
    {
        this.isPie = isPie;
        for (let child of this.children)
        {
            child.setPieMode(isPie);
        }
    }

    public hitList(position: Vector2): TreeChartNode[]
    {
        let flat = TreeChartComponent.nodes([this]);

        for (let node of flat)
        {
            if (node.hit(position)) return [node];
        }

        return [];
    }

    public hit(position: Vector2): boolean
    {
        // Do not allow hitting invalid/virtual nodes
        if (!this.node || !this.displayArc) return false;

        // Find angle from 0,0
        let angle    = this.angle(position);
        let distance = position.length();

        // Return false if outside angle range
        if (angle < this.displayArc.start || angle > this.displayArc.end) return false;
        // Return false if outside radius range
        if (distance < this.displayArc.rInner || distance > this.displayArc.rOuter) return false;

        // Return true otherwise
        return true;
    }

    public box()
    {
        let box = this.m_box.clone();
        if (this.children?.length > 0)
        {
            for (let child of this.children) box.updateToFit(child.box());
        }

        return box;
    }

    public anchor(): Vector2
    {
        if (this.displayArc)
        {
            let theta = (this.displayArc.start + this.displayArc.end) / 2;
            let span  = this.displayArc.rOuter - this.displayArc.rInner;
            let r     = this.displayArc.rInner + (span * (theta > Math.PI ? 0.8 : 0.2));
            return new Vector2(r * Math.cos(theta), r * Math.sin(theta));
        }

        return this.box()
                   .center();
    }

    public prepare(ctx: CanvasRenderingContext2D,
                   hint: AxisAlignedBoundingBox)
    {
        // Assign max depth
        this.setMaxDepth(TreeChartComponent.depth(this, this.level));
        this.setPercentages(1);

        // Compute tree layout
        this.compute();
        this.compute(false);
        this.interpolate();

        // Configure tree for display
        this.colorize();
        this.describe();
    }

    public zoomBounds(bounds?: Vector2): Vector2
    {
        return new Vector2(0.01, 100);
    }

    public onFocus(root: TreeArc)
    {
        // Set up focus flags
        root.setFocusLevel(this.level);
        root.clearFocusBranch();
        this.flagFocusBranch();

        root.compute(false);
        root.interpolate(0);
    }

    public onFocusTick(percent: number)
    {
        this.interpolate(percent);
    }

    public onFocusClear(root: TreeArc)
    {
        // Set up focus flags
        root.setFocusLevel(-1);
        root.clearFocusBranch();

        root.compute(false);
        root.interpolate(0);
    }

    public render(ctx: CanvasRenderingContext2D,
                  panZoom: PanZoom,
                  level: number = 0)
    {
        // Render the current arc, skip if no data
        if (this.node) this.renderArc(ctx);

        // Render all children
        this.children.forEach((childArc) => { childArc.render(ctx, panZoom, level + 1); });

        // Render the text labels
        this.renderText(ctx, panZoom);
    }

    private interpolate(percent: number = 1)
    {
        // Interpolate the arc
        let angle       = this.previousArc.angleAsVector()
                              .lerp(this.currentArc.angleAsVector(), percent);
        let radii       = this.previousArc.radiiAsVector()
                              .lerp(this.currentArc.radiiAsVector(), percent);
        this.displayArc = new Arc(angle.x, angle.y, radii.x, radii.y);

        // Interpolate the callout
        let calloutAngle = ChartingMath.lerp(this.previousCallout.angle, this.currentCallout.angle, percent);
        let r1           = ChartingMath.lerp(this.previousCallout.inner, this.currentCallout.inner, percent);
        let r2           = ChartingMath.lerp(this.previousCallout.outer, this.currentCallout.outer, percent);
        let o            = ChartingMath.lerp(this.previousCallout.opacity, this.currentCallout.opacity, percent);

        let start  = Vector2.fromPolar(calloutAngle, r1);
        let corner = Vector2.fromPolar(calloutAngle, r2);
        let side   = corner.x < start.x;
        let end    = corner.clone();
        end.x += (side ? -1 : 1) * this.currentCallout.tail;

        this.displayCallout = new Callout(this.m_font, this.m_subFont, start, corner, end, side, this.currentCallout.label, this.currentCallout.subLabel, o);

        // Interpolate all children
        for (let child of this.children) child.interpolate(percent);
    }

    private setMaxDepth(depth: number)
    {
        this.m_maxDepth = depth;
        for (let child of this.children) child.setMaxDepth(depth);
    }

    private setFocusLevel(level: number)
    {
        this.focusLevel = level;
        for (let child of this.children) child.setFocusLevel(level);
    }

    private setPercentages(parentPercent: number)
    {
        let sum = this.children.reduce((previous,
                                        current) =>
                                       {
                                           return previous + current.weight(current.node);
                                       }, 0);

        // Assign angles to children and recurse
        this.children.forEach((node,
                               index) =>
                              {
                                  let pct              = node.weight(node.node) / sum;
                                  node.percent         = pct * parentPercent;
                                  node.percentRelative = pct;
                                  node.setPercentages(node.percent);
                              });
    }

    private clearFocusBranch()
    {
        this.focusBranch = false;
        for (let child of this.children) child.clearFocusBranch();
    }

    private flagFocusBranch()
    {
        this.focusBranch = true;
        if (this.parent) (<TreeArc>this.parent).flagFocusBranch();
    }

    private compute(writeToBase: boolean = true,
                    startAngle: number   = 0,
                    endAngle: number     = Math.PI * 2)
    {
        let current = startAngle;
        let span    = endAngle - startAngle;
        let sum     = 0;
        for (let child of this.children) sum += child.weight(child.node);

        // Commit if not writing to base
        if (!writeToBase) this.commit();

        // Assign applicable values to self
        let radii = TreeArc.radii(this.level, this.effectiveFocusLevel, this.m_maxDepth, this.isPie);
        if (writeToBase)
        {
            this.baseArc.rInner = radii.x;
            this.baseArc.rOuter = radii.y;
            this.baseArc.start  = startAngle;
            this.baseArc.end    = endAngle;
        }
        else
        {
            this.currentArc.rInner = radii.x;
            this.currentArc.rOuter = radii.y;
            this.currentArc.start  = startAngle;
            this.currentArc.end    = endAngle;
            this.calculateCallout();
        }

        if (!this.percent) this.percent = 1;

        // Check if children has the a focused child
        let focusedChild = this.children.find((child) => { return child.selected; });
        let focusDivider = this.children.find((child) => { return child.focusBranch; });

        // If no focus, assign angles to children and recurse
        if (!focusedChild && !focusDivider)
        {
            this.children.forEach((node,
                                   index) =>
                                  {
                                      let pct   = node.weight(node.node) / sum;
                                      let start = current;
                                      let end   = current += pct * span;

                                      node.compute(writeToBase, start, end);
                                  });

        }
        else
        {
            let before = true;
            this.children.forEach((node,
                                   index) =>
                                  {
                                      if (node.selected || node.focusBranch)
                                      {
                                          node.compute(false);
                                          before = false;
                                      }
                                      else
                                      {
                                          if (before)
                                          {
                                              node.compute(false, 0, 0);
                                          }
                                          else
                                          {
                                              node.compute(false, 2 * Math.PI, 2 * Math.PI);
                                          }
                                      }
                                  });
        }
    }

    protected colorize()
    {
        super.colorize();

        // Adopt main colors if fill color given
        if (this.fillColor)
        {
            this.m_fill = this.fillColor;
        }
        else
        {
            this.m_fill = "transparent";
        }

        // Adopt additional colors if base color given
        if (this.baseColor)
        {
            this.m_fillFocus = this.selectedFillColor;
            this.m_fillHover = this.highlightedFillColor;
            this.m_fillMuted = this.mutedFillColor;
        }
        else
        {
            this.m_fillFocus = "transparent";
            this.m_fillHover = "transparent";
            this.m_fillMuted = "transparent";
        }

        // Colorize children as well
        for (let child of this.children) child.colorize();
    }

    private describe()
    {
        // Get label from label accessor
        this.m_label = this.label ? this.label(this.node) : "";

        // Make the value all pretty
        if (this.weight)
        {
            this.m_value = this.weight(this.node)
                               .toLocaleString(undefined,
                                               {
                                                   minimumFractionDigits: 0,
                                                   maximumFractionDigits: 2
                                               });
        }
        else
        {
            this.m_value = "---";
        }

        // Set the callout label content
        this.currentCallout.label    = `${(this.percent * 100).toFixed(1)}%`;
        this.currentCallout.subLabel = this.m_label;

        // Update bounds to include new label
        this.updateBounds();

        // Describe children as well
        for (let child of this.children) child.describe();
    }

    private renderArc(ctx: CanvasRenderingContext2D)
    {
        if (!this.displayArc) return;

        // Configure fill color
        let color = this.fillColor;
        if (this.node)
        {
            if (this.highlighted)
            {
                color = this.highlightedFillColor;
            }
            else if (this.selected)
            {
                color = this.selectedFillColor;
            }
            else if (this.muted)
            {
                color = this.mutedFillColor;
            }
        }

        // Render the arc
        this.displayArc.render(ctx, color);
    }

    private renderText(ctx: CanvasRenderingContext2D,
                       panZoom: PanZoom)
    {
        // Do not render if virtual of not focused level
        if (!this.node) return;

        // Render the callout
        this.displayCallout.render(ctx, this.m_helper);
    }

    private angle(point: Vector2): number
    {
        let axisAngle = Math.atan2(point.y, point.x);
        return point.y < 0 ? (Math.PI * 2) + axisAngle : axisAngle;
    }

    private commit()
    {
        this.previousArc.start  = this.currentArc.start;
        this.previousArc.end    = this.currentArc.end;
        this.previousArc.rInner = this.currentArc.rInner;
        this.previousArc.rOuter = this.currentArc.rOuter;

        this.previousCallout.start   = this.currentCallout.start;
        this.previousCallout.corner  = this.currentCallout.corner;
        this.previousCallout.end     = this.currentCallout.end;
        this.previousCallout.opacity = this.currentCallout.opacity;
        this.previousCallout.onUpdate();
    }

    private updateBounds()
    {
        // Depending on callout visibility, update the bounding box
        if (this.currentCallout.opacity !== 0)
        {
            this.m_box = this.m_baseBox.clone()
                             .updateToFit(this.currentCallout.box(this.m_helper));
        }
        else
        {
            // No callout, use base box
            this.m_box = this.m_baseBox.clone();
        }
    }

    private calculateCallout()
    {
        // Compute percentage label parameters
        let theta                  = (this.currentArc.start + this.currentArc.end) / 2;
        let lineStart              = (this.currentArc.rInner + this.currentArc.rOuter) / 2;
        let lineCorner             = TreeArc.radii(this.m_maxDepth, this.effectiveFocusLevel, this.m_maxDepth, this.isPie).y * WHITESPACE_MULTIPLIER;
        this.currentCallout.start  = new Vector2(lineStart * Math.cos(theta), lineStart * Math.sin(theta));
        this.currentCallout.corner = new Vector2(lineCorner * Math.cos(theta), lineCorner * Math.sin(theta));
        this.currentCallout.toLeft = theta > Math.PI * (1 / 2) && theta < Math.PI * (3 / 2);
        this.currentCallout.end    = new Vector2(this.currentCallout.corner.x + (this.currentCallout.toLeft ? -15 : 15), this.currentCallout.corner.y);

        // Calculate visibility based on level, focus level and multiroot state
        if (this.focusLevel !== -1)
        {
            this.currentCallout.opacity = this.level === this.focusLevel + 1 && this.currentArc.isVisible() ? 1 : 0;
        }
        else
        {
            this.currentCallout.opacity = this.level === (this.multiroot ? 0 : 1) ? 1 : 0;
        }

        // Override visibility if too skinny
        if (this.currentArc.end - this.currentArc.start < ((Math.PI * 2) / 100) * (Math.abs(Math.sin(theta)) + 1)) this.currentCallout.opacity = 0;

        // Trigger update
        this.currentCallout.onUpdate();

        // Update bounds
        this.updateBounds();
    }
}

class TreeBox extends TreeChartNode
{
    public display: AxisAlignedBoundingBox = new AxisAlignedBoundingBox(0, 0, 0, 0);

    protected m_children: TreeBox[]         = [];
    protected m_filteredChildren: TreeBox[] = [];

    public get children(): TreeBox[]
    {
        return this.m_filteredChildren;
    }

    public set children(nodes: TreeBox[])
    {
        this.m_children         = nodes;
        this.m_filteredChildren = nodes;
    }

    private m_labelSpace: number    = 64;
    private m_padding: number       = 10;
    private m_margin: number        = 5;
    private m_value: string         = "";
    private m_label: string         = "";
    private m_font: ChartFont       = null;
    private m_helper: ChartHelpers  = new ChartHelpers();
    private m_helper2: ChartHelpers = null;

    private m_labelBox: AxisAlignedBoundingBox = new AxisAlignedBoundingBox(0, 0, 0, 0);

    constructor(public chart: TreeChartComponent,
                public parent: TreeChartNode,
                public node: TreeLike,
                public id: TreeStringAccessor<any>,
                public weight: TreeNumericAccessor<any>,
                public color: TreeStringAccessor<any>,
                public label: TreeStringAccessor<any>,
                public format: TreeStringAccessor<any>,
                public description: TreeStringAccessor<any>,
                public multiroot: boolean,
                public maxLevel: number,
                public level: number)
    {
        // Call parent constructor
        super(chart, parent, node, id, weight, color, label, format, description, multiroot, maxLevel, level);

        // Recurse and build child nodes
        if (this.node && this.node.children && this.level < this.maxLevel)
        {
            this.children = node.children.map((node) =>
                                              {
                                                  return new TreeBox(chart, this, node, id, weight, color, label, format, description, multiroot, maxLevel, this.level + 1);
                                              });
        }
    }

    public hit(position: Vector2): boolean
    {
        return this.box()
                   .hit(position);
    }

    public box()
    {
        return this.display.clone();
    }

    public anchor(): Vector2
    {
        let box = this.box();
        return new Vector2(box.center().x, box.top);
    }

    public prepare(ctx: CanvasRenderingContext2D,
                   hint: AxisAlignedBoundingBox)
    {
        // Use the hint box to create a target box
        let scale    = hint.width < hint.height ? hint.height / 2000 : hint.width / 2000;
        let width    = scale && hint.width / scale || 1;
        let height   = scale && hint.height / scale || 1;
        this.display = new AxisAlignedBoundingBox(width / -2, height / -2, width, height);

        // Compute tree layout
        this.compute();

        // Configure tree for display
        this.colorize();
        this.describe();
    }

    public zoomBounds(bounds?: Vector2): Vector2
    {
        let small = 10 / this.m_font.size;
        let large = 12 / this.m_font.size;

        if (!bounds)
        {
            bounds = new Vector2(small, large);
        }
        else
        {
            if (small < bounds.x) bounds.x = small;
            if (large > bounds.y) bounds.y = large;
        }

        // Check all children as well
        this.children.forEach((node) => { node.zoomBounds(bounds); });

        // Return the final bounds
        return bounds;
    }

    public onFocus(root: TreeBox)
    {
        // Nothing for now..
    }

    public onFocusTick(percent: number)
    {
        // Nothing for now...
    }

    public onFocusClear(root: TreeBox)
    {
        // Nothing for now..
    }

    public render(ctx: CanvasRenderingContext2D,
                  panZoom: PanZoom,
                  level: number = 0)
    {
        // Render the base box
        this.renderBox(ctx);

        // Render all children
        this.children.forEach((bubble) => { bubble.render(ctx, panZoom, level + 1); });

        // Render the text labels
        this.renderText(ctx, panZoom);
    }

    private compute()
    {
        let divisor = Math.max(1, this.level + 1);
        let padding = this.m_padding / divisor;
        let margin  = this.m_margin / divisor;

        // Only set up label box if no children
        if (!this.children || this.children.length === 0)
        {
            this.m_labelBox = this.makeLabelBox(this.display, this.level);
            return;
        }

        // Sort children by weight
        this.children.sort((a,
                            b) =>
                           {
                               let aValue = a.weight ? a.weight(a.node) : 0;
                               let bValue = b.weight ? b.weight(b.node) : 0;
                               return bValue - aValue;
                           });

        // Get only the values for the children
        let values = this.children.map((node) => { return node.weight ? node.weight(node.node) : 0; });

        // Apply padding to this child nodes
        let bounds = this.display.clone();
        bounds.height -= padding * 2;
        bounds.width -= padding * 2;
        bounds.x += padding;
        bounds.y += padding;

        // Fix up box
        bounds.unflip();

        // Reserve space for label
        this.m_labelBox = this.makeLabelBox(bounds, this.level);

        // Subtract reserved space
        if (this.level > -1)
        {
            bounds.y += this.m_labelBox.height;
            bounds.height -= this.m_labelBox.height;
        }

        // Get boxes for all children
        let boxes = TilingAlgorithms.squarify(1, values, bounds);

        // Assign boxes to children and recurse
        this.children.forEach((node,
                               index) =>
                              {
                                  // Set display box
                                  node.display = boxes[index];

                                  // Apply margin
                                  node.display.height -= margin * 2;
                                  node.display.width -= margin * 2;
                                  node.display.x += margin;
                                  node.display.y += margin;

                                  // Fix up box
                                  node.display.unflip();

                                  // Recursively compute
                                  node.compute();
                              });
    }

    protected colorize()
    {
        super.colorize();

        // Colorize children as well
        for (let child of this.children) child.colorize();

        // If no fill was given, make it transparent
        if (!this.fillColor)
        {
            this.fillColor            = "rgba(0,0,0,0)";
            this.highlightedFillColor = "rgba(0,0,0,0)";
            this.mutedFillColor       = "rgba(0,0,0,0)";
            this.selectedFillColor    = "rgba(0,0,0,0)";
        }
    }

    private describe()
    {
        // Get label from label accessor
        this.m_label = this.label ? this.label(this.node) : "";

        // Make the value all pretty
        if (this.weight)
        {
            this.m_value = Number.parseFloat(`${this.weight(this.node)}`)
                                 .toPrecision(4);
        }
        else
        {
            this.m_value = "---";
        }

        // Make a font to use
        this.m_font       = new ChartFont();
        this.m_font.color = "black";
        this.m_font.size  = this.m_labelBox.height;

        // Measure the text
        let box          = this.m_helper.placeTextInBox(this.m_font, TextPlacement.Center, TextOrientation.Horizontal, this.m_label, this.display.x, this.display.y, BoxAnchor.Center, 0);
        // Adjust font size to fit label to label box
        this.m_font.size = Math.min(this.m_font.size, this.m_font.size / (box.height / this.m_labelBox.height), this.m_font.size / (box.width / this.m_labelBox.width));

        // Describe children as well
        for (let child of this.children) child.describe();
    }

    private makeLabelBox(container: AxisAlignedBoundingBox,
                         depth: number): AxisAlignedBoundingBox
    {
        let divisor = Math.max(depth + 1, 1);
        let margin  = this.m_margin / divisor;
        let height  = Math.min(container.height / 2, this.m_labelSpace / divisor);
        let width   = container.width - (2 * margin);
        let x       = container.x + margin;
        let y       = container.y;

        return new AxisAlignedBoundingBox(x, y, width, height);
    }

    private renderBox(ctx: CanvasRenderingContext2D)
    {
        // Use given fill color
        ctx.fillStyle = this.fillColor;

        // If a special state is applied, use its fill color
        if (this.node)
        {
            // State priority: highlighted > muted > selected
            if (this.highlighted)
            {
                ctx.fillStyle = this.highlightedFillColor;
            }
            else if (this.muted)
            {
                ctx.fillStyle = this.mutedFillColor;
            }
            else if (this.selected)
            {
                ctx.fillStyle = this.selectedFillColor;
            }
        }

        // Draw the box
        ctx.fillRect(this.display.x, this.display.y, this.display.width, this.display.height);
    }

    private renderText(ctx: CanvasRenderingContext2D,
                       panZoom: PanZoom)
    {
        // Cache text rendering helper if not cached
        if (!this.m_helper2) this.m_helper2 = new ChartHelpers(ctx);

        // Handle fading if needed
        let effective = panZoom.scale * this.m_font.size;

        // Only render if large enough to read
        if (effective > 6)
        {
            // Save canvas state
            ctx.save();

            // Calculate alpha if needed
            if (effective < 8)
            {
                ctx.globalAlpha = (effective - 6) / 2;
            }

            // Set up clipping
            ctx.beginPath();
            ctx.rect(this.m_labelBox.x, this.m_labelBox.y, this.m_labelBox.width, this.m_labelBox.height);
            ctx.clip();

            // Draw the text
            this.m_helper2.drawTextInBox(this.m_font,
                                         TextPlacement.Left,
                                         TextOrientation.Horizontal,
                                         this.m_label,
                                         this.m_font.color,
                                         this.m_labelBox.x,
                                         this.m_labelBox.y,
                                         BoxAnchor.TopLeft,
                                         0,
                                         "transparent",
                                         0);

            // Restore canvas state
            ctx.restore();
        }
    }
}

export interface TreeLike
{
    children: TreeLike[];
}

export interface TreeNumericAccessor<T extends TreeLike>
{
    (node: T): number;
}

export interface TreeStringAccessor<T extends TreeLike>
{
    (node: T): string;
}

export interface TreeFillAccessor<T extends TreeLike>
{
    (node: T): Fill;
}

export class LegendOption
{
    constructor(public label: string,
                public color: string)
    {}
}

export enum TreeChartMode
{
    BUBBLE   = "bubble",
    BOX      = "box",
    SUNBURST = "sunburst",
    PIEBURST = "pieburst"
}

class Arc
{
    constructor(public start: number,
                public end: number,
                public rInner: number,
                public rOuter: number)
    {}

    public angleAsVector(): Vector2
    {
        return new Vector2(this.start, this.end);
    }

    public radiiAsVector(): Vector2
    {
        return new Vector2(this.rInner, this.rOuter);
    }

    public isVisible(): boolean
    {
        return this.start !== this.end && this.rInner !== this.rOuter;
    }

    public render(ctx: CanvasRenderingContext2D,
                  color: string)
    {
        // Abort if invisible
        if (!this.isVisible()) return;

        // Save canvas state
        ctx.save();

        // Configure rendering parameters
        ctx.fillStyle   = color;
        ctx.lineWidth   = 1;
        ctx.strokeStyle = "white";

        // Draw the arc
        ctx.beginPath();
        ctx.arc(0, 0, this.rOuter, this.start, this.end, false);
        ctx.arc(0, 0, this.rInner, this.end, this.start, true);
        ctx.closePath();
        ctx.fill();
        ctx.stroke();

        // Restore canvas state
        ctx.restore();
    }
}

class Callout
{
    public angle: number;
    public inner: number;
    public outer: number;
    public tail: number;

    constructor(public font: ChartFont,
                public subFont: ChartFont,
                public start: Vector2   = new Vector2(0, 0),
                public corner: Vector2  = new Vector2(0, 0),
                public end: Vector2     = new Vector2(0, 0),
                public toLeft: boolean  = false,
                public label: string    = null,
                public subLabel: string = null,
                public opacity: number  = 0)
    {
        this.onUpdate();
    }

    public onUpdate()
    {
        this.angle = this.start.angleOf();
        this.inner = this.start.length();
        this.outer = this.corner.length();
        this.tail  = this.corner.distanceToVector(this.end);
    }

    public box(helper: ChartHelpers): AxisAlignedBoundingBox
    {
        let labelBox    = AxisAlignedBoundingBox.fromChartTextBox(helper.placeTextInBox(this.font,
                                                                                        this.toLeft ? TextPlacement.Right : TextPlacement.Left,
                                                                                        TextOrientation.Horizontal,
                                                                                        this.label ? this.label : "",
                                                                                        this.end.x + (this.toLeft ? -5 : 5),
                                                                                        this.end.y,
                                                                                        this.toLeft ? BoxAnchor.BottomRight : BoxAnchor.BottomLeft,
                                                                                        0));
        let subLabelBox = AxisAlignedBoundingBox.fromChartTextBox(helper.placeTextInBox(this.subFont,
                                                                                        this.toLeft ? TextPlacement.Right : TextPlacement.Left,
                                                                                        TextOrientation.Horizontal,
                                                                                        this.subLabel ? this.subLabel : "",
                                                                                        this.end.x + (this.toLeft ? -5 : 5),
                                                                                        this.end.y,
                                                                                        this.toLeft ? BoxAnchor.TopRight : BoxAnchor.TopLeft,
                                                                                        0));

        // Calculate a box containing text and callout lines
        let box = labelBox.clone();
        box.updateToFit(subLabelBox);
        box.updateToFit(new Vector2(this.start.x, this.start.y));
        box.updateToFit(new Vector2(this.corner.x, this.corner.y));
        box.updateToFit(new Vector2(this.end.x, this.end.y));

        return box;
    }

    public render(ctx: CanvasRenderingContext2D,
                  helper: ChartHelpers)
    {
        // Abort if invisible or empty
        if (this.opacity <= 0 || !this.label) return;

        // Save canvas state
        ctx.save();

        // Configure rendering parameters
        ctx.lineWidth   = 0.75;
        ctx.strokeStyle = "black";
        ctx.globalAlpha = this.opacity;

        // Draw line
        ctx.beginPath();
        ctx.moveTo(this.start.x, this.start.y);
        ctx.lineTo(this.corner.x, this.corner.y);
        ctx.lineTo(this.end.x, this.end.y);
        ctx.stroke();

        // Draw label
        helper.drawTextInBox(this.font,
                             this.toLeft ? TextPlacement.Right : TextPlacement.Left,
                             TextOrientation.Horizontal,
                             this.label,
                             this.font.color,
                             this.end.x + (this.toLeft ? -5 : 5),
                             this.end.y,
                             this.toLeft ? BoxAnchor.BottomRight : BoxAnchor.BottomLeft,
                             0,
                             "transparent",
                             0);
        helper.drawTextInBox(this.subFont,
                             this.toLeft ? TextPlacement.Right : TextPlacement.Left,
                             TextOrientation.Horizontal,
                             this.subLabel,
                             this.subFont.color,
                             this.end.x + (this.toLeft ? -5 : 5),
                             this.end.y,
                             this.toLeft ? BoxAnchor.TopRight : BoxAnchor.TopLeft,
                             0,
                             "transparent",
                             0);

        // Retore canvas state
        ctx.restore();
    }
}
