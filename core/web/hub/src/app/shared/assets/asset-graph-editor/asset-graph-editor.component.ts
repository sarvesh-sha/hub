import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Injector, Input, Output, QueryList, SimpleChanges, ViewChild, ViewChildren} from "@angular/core";
import {UUID} from "angular2-uuid";

import {AssetGraphExtended, AssetGraphResponseHolder, AssetGraphTreeNode} from "app/services/domain/asset-graph.service";
import {AssetExtended, DeviceElementExtended, LogicalAssetExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {StateHistory, StateRestorable} from "app/shared/undo/undo-redo-state";
import * as Models from "app/services/proxy/model/models";
import {GraphConfigurationHost} from "app/shared/assets/configuration/graph-configuration-host";
import {TagConditionBuilderComponent} from "app/shared/assets/tag-condition-builder/tag-condition-builder.component";
import {ConditionNode, ConditionNodeType} from "app/shared/assets/tag-condition-builder/tag-conditions";

import {PanZoomDirective} from "framework/directives/pan-zoom.directive";
import {Lookup, UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";
import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector       : "o3-asset-graph-editor",
               templateUrl    : "./asset-graph-editor.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush,
               styleUrls      : ["./asset-graph-editor.component.scss"]
           })
export class AssetGraphEditorComponent extends BaseApplicationComponent implements StateRestorable<AssetGraphExtended>
{
    private static readonly classPrefix = "o3-asset-graph-editor--";

    private m_data: AssetGraphExtended;
    @Input() set data(data: AssetGraphExtended)
    {
        if (this.m_data != data)
        {
            this.m_data = data;

            if (this.m_stateHistory?.active)
            {
                this.m_stateHistory.record("import asset structure");
            }
            else
            {
                this.activateStateHistory();
            }
        }
    }

    get data(): AssetGraphExtended
    {
        return this.m_data;
    }

    @Output() dataChange = new EventEmitter<AssetGraphExtended>();

    private m_stateHistory: StateHistory<AssetGraphExtended>;
    @Input() set stateHistory(stateHistory: StateHistory<AssetGraphExtended>)
    {
        this.m_stateHistory = stateHistory;
        this.activateStateHistory();
    }

    @Input() host: GraphConfigurationHost;

    @Input() graphId: string;

    @Input() normalization: Models.NormalizationRules;

    @Input() allowMultipleRoots: boolean = false;

    @ViewChild(PanZoomDirective, {static: true}) panZoom: PanZoomDirective;

    @ViewChildren("test_newNode", {read: ElementRef}) test_newNodes: QueryList<ElementRef>;
    @ViewChildren("test_configureNode", {read: ElementRef}) test_configureNodes: QueryList<ElementRef>;
    @ViewChild("nodeDialog") test_nodeDialog: StandardFormOverlayComponent;
    @ViewChild("test_nodeName", {read: ElementRef}) test_nodeName: ElementRef;
    @ViewChild("test_tagBuilder") test_tagBuilder: TagConditionBuilderComponent;

    nodes: AssetGraphTreeNode[]                 = [];
    private m_cannotDeleteNode: Lookup<boolean> = {};

    holder: AssetGraphResponseHolder;

    //--//

    nodeValid = true;

    //--//

    existingNode: Models.AssetGraphNode;
    existingNodeNames: string[];
    existingHasChildren: boolean;
    editNode: Models.AssetGraphNode;
    parentNode: Models.AssetGraphNode;

    existingRelationship: Models.AssetRelationship;
    editTransform: Models.AssetGraphTransformRelationship;

    overlayConfig = OverlayConfig.newInstance({
                                                  showCloseButton : false,
                                                  containerClasses: ["dialog-xl"]
                                              });

    pristine: boolean = false;

    ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);

        if (this.m_data && this.normalization)
        {
            this.initFromData(true);
        }
    }

    private async activateStateHistory()
    {
        if (this.m_data && this.m_stateHistory && !this.m_stateHistory.active)
        {
            await this.m_stateHistory.activate(this);
            this.markForCheck();
        }
    }

    private initFromData(resetZoom: boolean)
    {
        this.nodes = this.data.getTreeNodes();

        let rootId   = this.data.getRootNodes()[0]?.id;
        let rootNode = this.nodes.find((node) => node.id === rootId);
        if (rootNode && this.host && this.graphId)
        {
            this.m_cannotDeleteNode = {};
            this.evaluateNonDeletable(rootNode);
        }

        this.evaluate(resetZoom);
    }

    private evaluateNonDeletable(node: AssetGraphTreeNode): boolean
    {
        let nodeId       = node.id;
        let cannotDelete = !this.host.canRemoveNode(this.graphId, nodeId);
        for (let child of node.children || [])
        {
            if (this.evaluateNonDeletable(child)) cannotDelete = true;
        }
        this.m_cannotDeleteNode[nodeId] = cannotDelete;

        return cannotDelete;
    }

    newNodeClass(nodeId: string)
    {
        return `${AssetGraphEditorComponent.classPrefix}new--${nodeId}`;
    }

    editNodeClass(nodeId: string)
    {
        return `${AssetGraphEditorComponent.classPrefix}edit--${nodeId}`;
    }

    public async createNew(parentId?: string)
    {
        let newNode = Models.AssetGraphNode.newInstance({
                                                            id           : UUID.UUID(),
                                                            condition    : null,
                                                            optional     : false,
                                                            allowMultiple: false
                                                        });

        this.existingNodeNames = this.data.getNodeNames();
        this.data.addNode(newNode);
        if (parentId)
        {
            this.editTransform = this.data.addRelationshipTransform(parentId, newNode.id, Models.AssetRelationship.controls);
            this.parentNode    = this.data.getNodeById(parentId);
        }

        await this.evaluate();
        this.editNode = newNode;
        this.markForCheck();
    }

    async edit(id: string)
    {
        let node    = this.data.getNodeById(id);
        let tr      = this.data.findTransformByOutputId(id);
        let newNode = Models.AssetGraphNode.deepClone(node);

        this.data.updateNode(newNode);
        await this.evaluate();

        this.existingNode        = node;
        this.existingHasChildren = this.data.getNodeChildren(id)?.length > 0;
        this.editNode            = newNode;
        this.pristine            = true;
        this.existingNodeNames   = this.data.getNodeNames();

        if (tr instanceof Models.AssetGraphTransformRelationship)
        {
            this.editTransform        = tr;
            this.existingRelationship = tr.relationship;
            this.parentNode           = this.data.getNodeById(tr.inputId);
        }

        this.nodeValid = true;
        this.markForCheck();
    }

    async saveNode()
    {
        this.reset();
        this.initFromData(true);
        await this.m_stateHistory.record(this.existingNode ? "edit node" : "create node");
        this.dataChange.emit(this.data);
    }

    cancelNode()
    {
        if (!this.editNode) return;

        if (this.existingNode)
        {
            this.data.updateNode(this.existingNode);
            if (this.editTransform)
            {
                this.editTransform.relationship = this.existingRelationship;
            }
        }
        else
        {
            this.data.deleteNode(this.editNode.id);
        }

        this.reset();
        this.initFromData(false);
    }

    cannotDeleteNode(node: AssetGraphTreeNode): boolean
    {
        return this.m_cannotDeleteNode[node.id];
    }

    async deleteNode(node: AssetGraphTreeNode)
    {
        let nodeCt = this.data.model.nodes.length;
        this.data.deleteNode(node.id);

        this.initFromData(false);
        await this.m_stateHistory.record(`delete ${UtilsService.pluralize("node", nodeCt - this.data.model.nodes.length)}`);
        this.dataChange.emit(this.data);
    }

    reset()
    {
        this.editNode             = null;
        this.existingNode         = null;
        this.existingHasChildren  = false;
        this.parentNode           = null;
        this.editTransform        = null;
        this.existingRelationship = null;
        this.nodeValid            = true;
        this.existingNodeNames    = [];
    }

    private m_generatedName: string;

    async conditionUpdated(tagBuild: TagConditionBuilderComponent)
    {
        let text          = "";
        let canUpdateText = !this.editNode.name || (!this.existingNode && this.editNode.name == this.m_generatedName);
        if (canUpdateText && tagBuild.root instanceof ConditionNode)
        {
            let value = tagBuild.root.value;
            switch (tagBuild.root.type)
            {
                case ConditionNodeType.POINT:
                    text = await this.findPointClassName(value);
                    break;

                case ConditionNodeType.EQUIPMENT:
                    text = await this.findEquipClassName(value);
                    break;

                case ConditionNodeType.LOCATION:
                    text = await this.findLocationName(value);
                    break;

                case ConditionNodeType.METRICS:
                    text = await this.findMetricName(value);
                    break;

                case ConditionNodeType.TAG:
                    text = value;
                    break;
            }

            if (text)
            {
                if (tagBuild.root.negate)
                {
                    text = `Not ${text}`;
                }

                this.m_generatedName = text;
                this.editNode.name   = text;
            }
        }

        await this.evaluate();
    }

    private async findPointClassName(id: string): Promise<string>
    {
        const rules = await this.app.domain.normalization.getActiveRules();
        if (rules && rules.rules?.pointClasses)
        {
            const pc = rules.rules.pointClasses.find((pc) => pc.id + "" === id);
            if (pc)
            {
                return pc.pointClassName;
            }
        }

        return "";
    }

    private async findEquipClassName(id: string): Promise<string>
    {
        const rules = await this.app.domain.normalization.getActiveRules();
        if (rules && rules.rules?.equipmentClasses)
        {
            const ec = rules.rules.equipmentClasses.find((pc) => pc.id + "" === id);
            if (ec)
            {
                return ec.equipClassName;
            }
        }

        return "";
    }

    private async findLocationName(id: string): Promise<string>
    {
        const location = await this.app.domain.assets.getExtendedById(id);

        return location?.model.name || "";
    }

    private async findMetricName(id: string): Promise<string>
    {
        const metricDefinition = await this.app.domain.metricsDefinitions.getExtendedById(id);

        return metricDefinition?.model.title || "";
    }

    async evaluate(resetZoom?: boolean)
    {
        this.updatePristine();
        if (!this.nodeValid) return;

        try
        {
            this.holder = await this.data.resolveWithContext();
        }
        catch (e)
        {
        }

        if (resetZoom) this.panZoom.autoZoom();
        this.markForCheck();
    }

    updateValid(valid: boolean)
    {
        this.nodeValid = valid;
        if (!this.nodeValid)
        {
            this.holder = new AssetGraphResponseHolder([]);
            this.markForCheck();
        }
    }

    updatePristine()
    {
        if (this.editNode && this.existingNode)
        {
            this.pristine = UtilsService.compareJson(this.editNode, this.existingNode);
            if (this.editTransform)
            {
                this.pristine = this.pristine && this.editTransform.relationship === this.existingRelationship;
            }
        }
        else
        {
            this.pristine = false;
        }
    }

    //--//

    async restoreToState(state: AssetGraphExtended): Promise<void>
    {
        this.m_data = state;
        this.initFromData(false);
        this.dataChange.emit(this.data);
    }

    async cloneState(state: AssetGraphExtended): Promise<AssetGraphExtended>
    {
        return new AssetGraphExtended(this.app.domain, Models.AssetGraph.deepClone(state.model));
    }

    async readState(): Promise<AssetGraphExtended>
    {
        return this.data;
    }
}

@Component({
               selector       : "o3-asset-graph-matches-list",
               template       : `
                   <p *ngIf="helpMessage">{{helpMessage}}</p>
                   <o3-datatable *ngIf="!helpMessage" [table]="table">
                       <o3-datatable-column id="name" prop="name" name="Name"></o3-datatable-column>
                       <o3-datatable-column id="assetClass" prop="assetClass" name="Asset Class"></o3-datatable-column>
                       <o3-datatable-column id="type" prop="type" name="Type"></o3-datatable-column>
                       <o3-datatable-column id="location" prop="location" name="Location"></o3-datatable-column>
                       <o3-datatable-column id="parentName" prop="parentName" name="Parent Name"></o3-datatable-column>
                   </o3-datatable>
               `,
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class AssetGraphMatchesListComponent extends BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, AssetExtended, AssetFlat>
{
    @Input() holder: AssetGraphResponseHolder;
    @Input() node: Models.AssetGraphNode;
    @Input() parentNode: Models.AssetGraphNode;
    @Input() relationship: Models.AssetRelationship;

    table: DatatableManager<Models.RecordIdentity, AssetExtended, AssetFlat>;

    pointClasses: ControlOption<string>[];
    equipClasses: ControlOption<string>[];

    helpMessage: string;

    constructor(inj: Injector)
    {
        super(inj);
        this.table = new DatatableManager<Models.RecordIdentity, AssetExtended, AssetFlat>(this, () => this.getViewState());
    }

    public async ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);
        this.table.resetPagination();
        await this.table.refreshData();
        this.markForCheck();
    }

    public getItemName(): string
    {
        return "assets";
    }

    public async getList(): Promise<Models.RecordIdentity[]>
    {
        if (!this.holder || !this.node) return [];

        this.helpMessage = "";
        if (this.holder.invalidResponses.length)
        {
            this.helpMessage = "Too many assets matched. Use 'Allow multiple' to allow multiple matches for each parent.";
            return [];
        }
        else if (!this.holder.responses.length)
        {
            this.helpMessage = "No assets match this structure. Perhaps the condition is too strict.";
            return [];
        }

        let ids = this.holder.resolveIdentities(Models.AssetGraphBinding.newInstance({nodeId: this.node.id}));

        if (!ids.length)
        {
            this.helpMessage = "Condition matches no assets.";
            return [];
        }
        else if (ids.length > 2000)
        {
            this.helpMessage = `Condition matches ${ids.length.toLocaleString()} assets.`;
            return [];
        }

        return ids;
    }

    public async getPage(offset: number,
                         limit: number): Promise<AssetExtended[]>
    {
        return this.app.domain.assets.getPageFromTable(this.table, offset, limit);
    }

    public itemClicked(columnId: string,
                       item: AssetFlat): void
    {
    }

    public async transform(assets: AssetExtended[]): Promise<AssetFlat[]>
    {
        let flat: AssetFlat[]        = [];
        let locations                = await mapInParallel(assets, (row) => row.getLocation());
        let parents: AssetExtended[] = [];
        if (this.relationship)
        {
            let parentsArrs = await mapInParallel(assets, (row) => row.getExtendedParentsOfRelation(this.relationship));
            parents         = parentsArrs.map((arr) => arr[0]);
        }

        if (!this.pointClasses || !this.equipClasses)
        {
            this.equipClasses = await this.app.bindings.getEquipmentClasses(false, null);
            this.pointClasses = await this.app.bindings.getPointClasses(false, null);
        }

        for (let i = 0; i < assets.length; i++)
        {
            let asset              = assets[i];
            let parent             = parents[i];
            let loc                = locations[i];
            let locName            = loc ? await loc.getRecursiveName() : "";
            let type               = "Asset";
            let assetClass: string = "";
            if (asset instanceof DeviceElementExtended)
            {
                type             = "Control Point";
                let pointClassId = asset.typedModel.pointClassId;
                if (pointClassId)
                {
                    assetClass = this.pointClasses.find((cls) => cls.id === pointClassId).label;
                }
            }
            else if (asset instanceof LogicalAssetExtended)
            {
                type             = "Equipment";
                let equipClassId = asset.typedModel.equipmentClassId;
                if (equipClassId)
                {
                    assetClass = this.equipClasses.find((cls) => cls.id === equipClassId).label;
                }
            }

            flat.push(new AssetFlat(asset.model.name, locName, parent?.model.name || "", type, assetClass));
        }

        return flat;
    }

    //--//
}

class AssetFlat
{
    constructor(public name: string,
                public location: string,
                public parentName: string,
                public type: string,
                public assetClass: string)
    {}
}
