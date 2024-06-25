import {ChangeDetectionStrategy, Component, EventEmitter, Injector, Input, Output, Renderer2, SimpleChanges} from "@angular/core";

import {InteractableSource, InteractableSourcesChart} from "app/customer/visualization/time-series-utils";
import {TimeDuration} from "app/services/proxy/model/TimeDuration";
import {Lookup} from "framework/services/utils.service";

import {Vector2} from "framework/ui/charting/charting-math";
import {ColorGradientStop} from "framework/ui/charting/core/colors";
import {ChartPointSource, VisualizationDataSourceState} from "framework/ui/charting/core/data-sources";
import {TreeChartComponent, TreeChartNode, TreeLike} from "framework/ui/charting/tree-chart.component";

@Component({
               selector       : "o3-aggregation-tree",
               templateUrl    : "aggregation-tree.component.html",
               styleUrls      : ["aggregation-tree.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class AggregationTreeComponent extends TreeChartComponent implements InteractableSourcesChart
{
    private m_viewport: Vector2;
    private m_viewportWithLegend: Vector2;
    private m_sourcesMap: Map<string, AggregationTreeSource> = new Map<string, AggregationTreeSource>();

    public sources: AggregationTreeSource[] = [];

    @Input()
    set dynamicViewport(vec: Vector2)
    {
        if (vec)
        {
            this.m_viewport           = vec;
            this.m_viewportWithLegend = vec.clone();
            this.m_viewportWithLegend.y -= 56;
            this.viewport             = this.dynamicViewport;
        }
    }

    get dynamicViewport(): Vector2
    {
        return this.legend ? this.m_viewportWithLegend : this.m_viewport;
    }

    @Input() legend: boolean = true;

    @Output() sourcesUpdated = new EventEmitter<AggregationTreeSource[]>();

    public chart: AggregationTreeComponent;
    public chartUpdated        = new EventEmitter<boolean>();
    public sourceStatesUpdated = new EventEmitter<Lookup<VisualizationDataSourceState>>();

    constructor(inj: Injector,
                renderer: Renderer2)
    {
        super(inj, renderer);
        this.chart = this;
    }

    ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);

        // Needed to fix bug with 'legend' input not set before 'dynamicViewport' input is set
        if (changes.legend) this.dynamicViewport = this.m_viewport;
    }

    public process()
    {
        super.process();
        this.extractSources();
    }

    public getNumSources(): number
    {
        return this.sources.length;
    }

    public isDeletable(sourceId: string): boolean
    {
        return false;
    }

    public getSource(sourceId: string): AggregationTreeSource
    {
        return this.m_sourcesMap.get(sourceId);
    }

    public getSourcesExcept(sourceId: string): AggregationTreeSource[]
    {
        return this.sources.filter((source) => source.identifier !== sourceId);
    }

    public getSources(panelIdx: number     = null,
                      onlyVisible: boolean = null): AggregationTreeSource[]
    {
        return this.sources;
    }

    public getSourceState(sourceId: string): VisualizationDataSourceState
    {
        let source = this.getSource(sourceId);
        return source ? source.state : VisualizationDataSourceState.Disabled;
    }

    public isReady(): boolean
    {
        return true;
    }

    public onChange(): void
    {
        // Noop
    }

    public configureSource(sourceId: string): void
    {
        // Noop
    }

    public multiToggleEnabled(sourceId: string): void
    {
        let source = this.getSource(sourceId);
        if (!source) return;

        // Get all other sources
        let others = this.getSourcesExcept(sourceId);

        // Enable the source
        source.enable();
        this.enableNode(source.data);

        // Disable all others
        others.forEach((source) =>
                       {
                           source.disable();
                           this.disableNode(source.data);
                       });

        // Unselect and unhighlight any nodes as well
        this.selectNode(null);
        this.highlightNode(null);

        // Emit state updates
        this.emitSourceStates();
    }

    public toggleEnabled(sourceId: string): void
    {
        let source = this.getSource(sourceId);
        if (!source) return;

        if (source.isDisabled())
        {
            source.enable();
            this.enableNode(source.data);
        }
        else
        {
            source.disable();
            this.disableNode(source.data);
        }

        // Unselect and unhighlight any nodes as well
        this.selectNode(null);
        this.highlightNode(null);
        this.getSources()
            .forEach((source) => source.clear());

        // Emit state updates
        this.emitSourceStates();
    }

    public toggleTarget(sourceId: string,
                        fromMouseover: boolean): void
    {
        let source = this.getSource(sourceId);
        if (!source) return;

        if (fromMouseover)
        {
            if (!source.isTargeted())
            {
                this.highlightNode(source.data, true);
                this.targetSource(source);
            }
            else
            {
                this.highlightNode(null, true);
                this.untargetSource();
            }
        }
        else
        {
            this.selectNode(source.data);
        }
    }

    private targetSource(source: AggregationTreeSource)
    {
        // Mute all other sources
        this.getSourcesExcept(source.identifier)
            .forEach((source) => source.mute());

        // Target the given source
        source.target();

        // Emit state updates
        this.emitSourceStates();
    }

    private untargetSource()
    {
        // Clear all sources
        this.getSources()
            .forEach((source) => source.clear());

        // Emit state updates
        this.emitSourceStates();
    }

    private emitSourceStates()
    {
        // Find sources that changed
        let changed = this.sources.filter((source) => source.stateChanged);

        // Clear changed flags and compile states
        let states: Lookup<VisualizationDataSourceState> = {};
        for (let source of changed)
        {
            source.stateChanged       = false;
            states[source.identifier] = source.state;
        }

        // Emit changes source states
        this.sourceStatesUpdated.emit(states);
    }

    private extractSources()
    {
        // Extract data source node and tree nodes separately
        let treeNodes: TreeChartNode[] = [];
        let sourceNodes: TreeLike[]    = [];
        if (this.rootNode)
        {
            treeNodes   = this.isMultiGroup() || this.isSingleGroupFlat() ? this.rootNode.children : [this.rootNode];
            sourceNodes = treeNodes.map((treeNode) => treeNode.node);
        }
        else
        {
            sourceNodes = this.tree;
            treeNodes   = new Array(this.tree.length).fill(null);
            // Null is okay here, we won't ever be interacting with the tree nodes since there is no data
        }

        // Make each tree/source node combo into an interactable source
        this.sources = [];
        for (let i = 0; i < treeNodes.length; i++) this.sources.push(new AggregationTreeSource(sourceNodes[i], treeNodes[i], this));
        // Map sources by identifier
        this.m_sourcesMap = new Map<string, AggregationTreeSource>();
        for (let source of this.sources) this.m_sourcesMap.set(source.identifier, source);

        // Emit update event
        this.sourcesUpdated.emit(this.sources);
    }

    private isMultiGroup(): boolean
    {
        return this.tree.length > 1;
    }

    private isSingleGroupFlat(): boolean
    {
        return this.tree.length === 1 && this.tree[0].children && this.maxDepth === 0;
    }
}


export class AggregationTreeSource implements InteractableSource
{
    public identifier: string;
    public name: string;
    public description: string;
    public color: string;
    public colorStops: ColorGradientStop[];

    public deviceElementId: string  = null;
    public panel: number            = 0;
    public timeOffset: TimeDuration = null;
    public valid: boolean           = true;

    public state: VisualizationDataSourceState = VisualizationDataSourceState.Active;
    public stateChanged: boolean               = true;

    constructor(public source: TreeLike,
                public data: TreeChartNode,
                public chart: AggregationTreeComponent)
    {
        // Adopt id, if none available, generate a dummy one
        this.identifier = data ? data.identifier : `${AggregationTreeComponent.nextId()}`;

        this.color       = this.chart.color(this.source);
        this.colorStops  = null;
        this.name        = this.chart.label(this.source);
        this.description = this.chart.description(this.source);
    }

    public getChartData(): ChartPointSource<any>
    {
        // Not applicable for now
        return null;
    }

    public mute()
    {
        // Mute branch if not muted or disabled
        if (!this.isMuted() && !this.isDisabled())
        {
            this.state        = VisualizationDataSourceState.Muted;
            this.stateChanged = true;
        }
    }

    public target()
    {
        // Target branch if not targeted
        if (!this.isTargeted() && !this.isDisabled())
        {
            this.state        = VisualizationDataSourceState.Target;
            this.stateChanged = true;
        }
    }

    public clear()
    {
        if (!this.isDisabled())
        {
            this.state        = VisualizationDataSourceState.Active;
            this.stateChanged = true;
        }
    }

    public enable()
    {
        this.state        = VisualizationDataSourceState.Active;
        this.stateChanged = true;
    }

    public disable()
    {
        this.state        = VisualizationDataSourceState.Disabled;
        this.stateChanged = true;
    }

    public isDisabled()
    {
        return this.state === VisualizationDataSourceState.Disabled;
    }

    public isMuted()
    {
        return this.state === VisualizationDataSourceState.Disabled;
    }

    public isTargeted()
    {
        return this.state === VisualizationDataSourceState.Target;
    }
}
