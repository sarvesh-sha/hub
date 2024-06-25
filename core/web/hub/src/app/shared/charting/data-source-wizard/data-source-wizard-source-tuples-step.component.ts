import {animate, style, transition, trigger} from "@angular/animations";
import {CdkDragDrop, moveItemInArray, transferArrayItem} from "@angular/cdk/drag-drop";
import {Component, DoCheck, Injector, QueryList, ViewChildren} from "@angular/core";
import {UUID} from "angular2-uuid";

import {AppContext} from "app/app.service";
import {ControlPointMetadata} from "app/customer/visualization/time-series-utils";
import {AssetGraphTreeNode, SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import {UnitsService} from "app/services/domain/units.service";
import * as Models from "app/services/proxy/model/models";
import {DataSourceWizardState} from "app/shared/charting/data-source-wizard/data-source-wizard-dialog.component";
import {ScatterPlotContainerComponent, ScatterPlotSourceTupleExtended} from "app/shared/charting/scatter-plot/scatter-plot-container.component";

import {Lookup} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {SelectComponent} from "framework/ui/forms/select.component";
import {EquivalentValuesValidatorDirective, UniqueValuesWithCallbackValidatorDirective} from "framework/ui/forms/validators/unique-values-validator.directive";
import {WizardStep} from "framework/ui/wizards/wizard-step";
import {inParallel, mapInParallelNoNulls} from "framework/utils/concurrency";

@Component({
               selector   : "o3-data-source-wizard-source-tuples-step",
               templateUrl: "./data-source-wizard-source-tuples-step.component.html",
               styleUrls  : ["./data-source-wizard-source-tuples-step.component.scss"],
               providers  : [WizardStep.createProvider(DataSourceWizardSourceTuplesStepComponent)],
               animations : [
                   trigger("expand", [
                       transition(":enter", [
                           style({height: 0}),
                           animate(".2s ease-out", style({height: "*"}))
                       ]),
                       transition(":leave", [
                           animate(".2s ease-out", style({height: "0"}))
                       ])
                   ])
               ]
           })
export class DataSourceWizardSourceTuplesStepComponent extends WizardStep<DataSourceWizardState> implements DoCheck
{
    private static readonly DragTransitionDurationMs: number = 200;

    tupleCollections: TupleCollection[];

    // id for use as name in form; axis/tuple/panel does not work as name because the tuples are reorderable
    tupleIdMap: Map<Models.ScatterPlotSourceTuple, string>;

    private idToUnitsHash: Lookup<string>;
    graphOptions: AssetGraphTreeNode[];
    standardOptions: ControlOption<string>[];

    private m_graphs: Map<string, SharedAssetGraphExtended>;

    idToUnits = (id: string) => this.idToUnitsHash[id];

    showZOptions: boolean;

    dragInProgress: boolean = false;
    overNewPanel: boolean   = false;

    formErrors: Lookup<string>;

    private formNeedsEvaluation: boolean = false;

    get ready(): boolean
    {
        if (!this.tupleCollections) return false;
        if (!this.idToUnitsHash) return false;

        return !!this.standardOptions || !!this.graphOptions;
    }

    readonly tupleListIdPrefix: string = "tuple-list-";
    readonly addPanelId: string        = "add-panel";

    @ViewChildren("test_xStandard") test_xStandards: QueryList<SelectComponent<string>>;
    @ViewChildren("test_yStandard") test_yStandards: QueryList<SelectComponent<string>>;
    @ViewChildren("test_zStandard") test_zStandards: QueryList<SelectComponent<string>>;

    @ViewChildren("test_xGraph") test_xGraphs: QueryList<SelectComponent<string>>;
    @ViewChildren("test_yGraph") test_yGraphs: QueryList<SelectComponent<string>>;
    @ViewChildren("test_zGraph") test_zGraphs: QueryList<SelectComponent<string>>;

    constructor(inj: Injector,
                private m_app: AppContext)
    {
        super(inj);
    }

    public ngDoCheck()
    {
        if (this.formNeedsEvaluation)
        {
            // form controls seem to be doing some asynchronous work: wait for it to complete before reevaluating
            setTimeout(() => this.evaluateTupleSelections(), 25);
            this.formNeedsEvaluation = false;
        }
    }

    public uniqueFn(tuple: Models.ScatterPlotSourceTuple,
                    formPart: TupleValue): () => boolean
    {
        return () => this.isUnique(tuple, formPart);
    }

    private isUnique(tuple: Models.ScatterPlotSourceTuple,
                     formPart: TupleValue): boolean
    {
        const getId = this.getIdFn();
        switch (formPart)
        {
            case "x":
                let xId = getId(tuple.sourceX);
                if (!xId) return true;
                return xId !== getId(tuple.sourceY) && xId !== getId(tuple.sourceZ);

            case "y":
                let yId = getId(tuple.sourceY);
                if (!yId) return true;
                return yId !== getId(tuple.sourceX) && yId !== getId(tuple.sourceZ);

            case "z":
                let zId = getId(tuple.sourceZ);
                if (!zId) return true;
                return zId !== getId(tuple.sourceX) && zId !== getId(tuple.sourceY);
        }

        return true;
    }

    public getXIds(tupleCollection: TupleCollection,
                   excludeTuple: Models.ScatterPlotSourceTuple): string[]
    {
        const getId = this.getIdFn();
        let tuples  = tupleCollection.tuples.filter((tuple) => tuple !== excludeTuple);
        return tuples.map((tuple) => getId(tuple.sourceX));
    }

    public getYIds(tupleCollection: TupleCollection,
                   excludeTuple: Models.ScatterPlotSourceTuple): string[]
    {
        const getId = this.getIdFn();
        let tuples  = tupleCollection.tuples.filter((tuple) => tuple !== excludeTuple);
        return tuples.map((tuple) => getId(tuple.sourceY));
    }

    public getZIds(tupleCollection: TupleCollection,
                   excludeTuple: Models.ScatterPlotSourceTuple): string[]
    {
        const getId = this.getIdFn();
        let tuples  = tupleCollection.tuples.filter((tuple) => tuple !== excludeTuple);
        return tuples.map((tuple) => getId(tuple.sourceZ));
    }

    public runCheck()
    {
        this.formNeedsEvaluation = true;
        this.detectChanges();
    }

    private evaluateTupleSelections()
    {
        let form = this.wizard.stepForm;

        this.formErrors = {};
        for (let controlName in form.controls)
        {
            if (controlName.startsWith("name")) continue;

            form.controls[controlName].updateValueAndValidity();

            let errors = form.controls[controlName].errors;
            if (!errors) continue;

            if (errors["required"])
            {
                this.formErrors[controlName] = "Needs selection";
            }
            else if (UniqueValuesWithCallbackValidatorDirective.isInvalid(errors))
            {
                this.formErrors[controlName] = "Same source in tuple";
            }
            else if (EquivalentValuesValidatorDirective.isInvalid(errors))
            {
                this.formErrors[controlName] = "Different units on axis";
            }
        }

        this.detectChanges();
    }

    public getName(tuple: Models.ScatterPlotSourceTuple,
                   formPart: TupleValue): string
    {
        return `${formPart}-${this.tupleIdMap.get(tuple)}`;
    }

    public getError(tuple: Models.ScatterPlotSourceTuple,
                    formPart: TupleValue)
    {
        let name = this.getName(tuple, formPart);
        return this.formErrors && this.formErrors[name] || "";
    }

    public getLabel(): string
    {
        return "Select Source Couplings";
    }

    public isEnabled(): boolean
    {
        if (!this.data) return false;
        return this.data.type === Models.TimeSeriesChartType.SCATTER || this.data.type === Models.TimeSeriesChartType.GRAPH_SCATTER;
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public isValid(): boolean
    {
        if (!this.data.sourceTuples?.length) return false;
        if (!this.ready) return false;

        return !!this.wizard.stepForm?.valid;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
        // clean up tuples
        if (this.data.type === Models.TimeSeriesChartType.SCATTER)
        {
            let controlPointMetadata = await mapInParallelNoNulls(this.data.ids, (id) => ControlPointMetadata.fromId(this.m_app, id.sysId));
            this.standardOptions     = controlPointMetadata.map((meta) => new ControlOption<string>(meta.point.model.sysId, meta.name + " - " + meta.standardDescription()));

            const idIsAvaible = (source: Models.ScatterPlotSource) =>
            {
                let sourceId = ScatterPlotSourceTupleExtended.getDeviceElementId(source);
                return !sourceId || this.data.ids.findIndex((recordId) => recordId.sysId === sourceId) >= 0;
            };

            this.data.sourceTuples = this.data.sourceTuples.filter((sourceTuple) =>
                                                                       idIsAvaible(sourceTuple.sourceX) && idIsAvaible(sourceTuple.sourceY) && idIsAvaible(sourceTuple.sourceZ));

            this.graphOptions = null;
        }
        else
        {
            this.m_graphs           = await this.data.graphsHost.resolveGraphs();
            this.graphOptions       = [];
            const availableBindings = new Set<string>();

            for (let [id, graph] of this.m_graphs.entries())
            {
                this.graphOptions.push(...graph.getTreeNodes(true, id, graph.name, availableBindings));
            }

            this.data.sourceTuples = this.data.sourceTuples.filter(
                (sourceTuple) =>
                {
                    let valid = true;
                    if (!availableBindings.has(AssetGraphTreeNode.getIdFromBinding(sourceTuple.sourceX?.binding))) valid = false;
                    if (valid && !availableBindings.has(AssetGraphTreeNode.getIdFromBinding(sourceTuple.sourceY?.binding))) valid = false;

                    if (valid)
                    {
                        let zId = AssetGraphTreeNode.getIdFromBinding(sourceTuple.sourceZ?.binding);
                        if (zId && !availableBindings.has(zId)) valid = false;
                    }

                    return valid;
                });

            this.standardOptions = null;
        }

        // clean up panels
        let validPanels: boolean[] = [];
        for (let tuple of this.data.sourceTuples) validPanels[tuple.panel] = true;
        this.data.panels = this.data.panels.filter((panel,
                                                    panelIdx) => validPanels[panelIdx]);

        await this.init();

        setTimeout(() => this.evaluateTupleSelections());
    }

    private async init()
    {
        // create local structures to represent tuples
        this.tupleCollections = [];
        let tuples            = this.data.sourceTuples;
        if (tuples.length === 0) tuples.push(ScatterPlotSourceTupleExtended.newModel(0));

        this.tupleIdMap = new Map();
        for (let tuple of tuples)
        {
            let tupleCollection = this.tupleCollections[tuple.panel];
            if (!tupleCollection) this.tupleCollections[tuple.panel] = tupleCollection = new TupleCollection(tuple.panel);
            tupleCollection.tuples.push(tuple);
            this.tupleIdMap.set(tuple, UUID.UUID());
        }

        let numOptions    = this.data.type === Models.TimeSeriesChartType.SCATTER ? this.standardOptions.length :
            this.graphOptions.reduce((cum,
                                      option) => cum + option.numOptions, 0);
        this.showZOptions = numOptions > 2;

        // build out id to units mapping for ensuring like-unit tuples on every panel
        let deviceElemRecords: Models.RecordIdentity[];
        let recordIdToBindingId: Lookup<string>;
        if (this.standardOptions)
        {
            deviceElemRecords = this.standardOptions.map((standardOption) => DeviceElementExtended.newIdentity(standardOption.id));
        }
        else
        {
            recordIdToBindingId = {};
            deviceElemRecords   = [];
            await inParallel(this.graphOptions, async (graphOption) =>
            {
                await this.assetStructureUnitsMappingHelper(deviceElemRecords, recordIdToBindingId, graphOption);
            });
        }
        let deviceElemExts = await this.m_app.domain.assets.getTypedExtendedBatch(DeviceElementExtended, deviceElemRecords);

        this.idToUnitsHash = {};
        await inParallel(deviceElemExts, async (deviceElemExt) =>
        {
            if (deviceElemExt)
            {
                let id = deviceElemExt.model.sysId;
                if (recordIdToBindingId) id = recordIdToBindingId[id];

                if (id)
                {
                    let schema             = await deviceElemExt.getSchemaProperty(DeviceElementExtended.PRESENT_VALUE);
                    this.idToUnitsHash[id] = UnitsService.computeHash(schema?.unitsFactors, false);
                }
            }
        });
    }

    private async assetStructureUnitsMappingHelper(deviceElemRecords: Models.RecordIdentity[],
                                                   recordIdToBindingId: Lookup<string>,
                                                   graphOption: AssetGraphTreeNode)
    {
        if (graphOption.hasChildren)
        {
            await inParallel(graphOption.children, (option) => this.assetStructureUnitsMappingHelper(deviceElemRecords, recordIdToBindingId, option));
        }
        else
        {
            const id       = graphOption.id;
            const binding  = AssetGraphTreeNode.getBinding(id);
            const graph    = this.m_graphs.get(binding.graphId);
            const resolved = await graph.resolve();
            const graphIds = await resolved.resolveIdentities(binding);
            let recordId   = graphIds && graphIds[0];
            if (recordId)
            {
                recordIdToBindingId[recordId.sysId] = id;
                deviceElemRecords.push(recordId);
            }
        }
    }

    public getBindingId(source: Models.ScatterPlotSource): string
    {
        return AssetGraphTreeNode.getIdFromBinding(source.binding);
    }

    public setBindingId(source: Models.ScatterPlotSource,
                        id: string)
    {
        source.binding = AssetGraphTreeNode.getBinding(id);
        this.runCheck();
    }

    public async onData()
    {
        await super.onData();
    }

    private getIdFn(): (source: Models.ScatterPlotSource) => string
    {
        return this.data.type === Models.TimeSeriesChartType.SCATTER ?
            ScatterPlotSourceTupleExtended.getDeviceElementId : (source) => AssetGraphTreeNode.getIdFromBinding(source?.binding);
    }

    public tupleListsConnectedTo(sourcePanel: number): string[]
    {
        let connectedTo = [this.addPanelId];
        for (let i = 0; i < this.tupleCollections.length; i++)
        {
            if (i !== sourcePanel) connectedTo.push(`${this.tupleListIdPrefix}${i}`);
        }

        return connectedTo;
    }

    public panelsReordered(event: CdkDragDrop<TupleCollection[]>)
    {
        moveItemInArray(this.tupleCollections, event.previousIndex, event.currentIndex);

        for (let i = event.previousIndex; i < event.currentIndex; i++) this.tupleCollections[i].previousPanelRemoved();
        for (let tuple of this.tupleCollections[event.currentIndex].tuples) tuple.panel = event.currentIndex;

        this.updateSourceTuplesOrder();
    }

    public addTuple(panelIdx: number)
    {
        let tuple = ScatterPlotSourceTupleExtended.newModel(panelIdx);
        this.tupleCollections[panelIdx].tuples.push(tuple);
        this.data.sourceTuples.push(tuple);
        this.tupleIdMap.set(tuple, UUID.UUID());

        this.updateSourceTuplesOrder();
    }

    public removeTuple(panelIndex: number,
                       tuple: Models.ScatterPlotSourceTuple)
    {
        let collection = this.tupleCollections[panelIndex];
        let tupleIndex = collection.tuples.findIndex((curr) => curr === tuple);
        if (tupleIndex >= 0)
        {
            collection.tuples.splice(tupleIndex, 1);
            if (collection.tuples.length === 0) this.cleanEmptyPanel(panelIndex);

            let sourceTuples = this.data.sourceTuples;
            sourceTuples.splice(sourceTuples.indexOf(tuple), 1);
            this.tupleIdMap.delete(tuple);
        }

        this.runCheck();
    }

    public moveTuple(tuple: Models.ScatterPlotSourceTuple,
                     sourcePanelIndex: number,
                     targetPanelIndex: number,
                     targetTupleIndex?: number)
    {
        let sourceTuples = this.tupleCollections[sourcePanelIndex].tuples;
        let targetTuples = this.tupleCollections[targetPanelIndex]?.tuples;
        if (!targetTuples)
        {
            let collection = new TupleCollection(this.tupleCollections.length);
            this.tupleCollections.push(collection);
            targetTuples = collection.tuples;
            this.data.panels.push(ScatterPlotContainerComponent.newPanel());
        }

        let sourceTupleIndex = sourceTuples.indexOf(tuple);
        if (sourceTuples === targetTuples)
        {
            moveItemInArray(sourceTuples, sourceTupleIndex, targetTupleIndex);
        }
        else
        {
            transferArrayItem(sourceTuples, targetTuples, sourceTupleIndex, targetTupleIndex);
            tuple.panel = targetPanelIndex;
            if (sourceTuples.length === 0) this.cleanEmptyPanel(sourcePanelIndex);
        }

        this.updateSourceTuplesOrder();
    }

    public setAnimationDelay()
    {
        // avoiding *cdkDragPreview to keep styling from drag element
        // conditionally applying classes/styles to the cdkDrag element does not affect preview animation if drag has already begun
        let animationElement: HTMLElement = document.querySelector(".cdk-drag-preview");
        if (!animationElement) return;

        let delay                                 = this.overNewPanel ? 0 : DataSourceWizardSourceTuplesStepComponent.DragTransitionDurationMs;
        animationElement.style.transitionDuration = delay + "ms";
    }

    private updateSourceTuplesOrder()
    {
        let i = 0;
        for (let tupleCollection of this.tupleCollections)
        {
            for (let tuple of tupleCollection.tuples) this.data.sourceTuples[i++] = tuple;
        }
    }

    private cleanEmptyPanel(panelIndex: number)
    {
        this.tupleCollections.splice(panelIndex, 1);
        for (let i = panelIndex; i < this.tupleCollections.length; i++) this.tupleCollections[i].previousPanelRemoved();
    }
}

type TupleValue = "name" | "x" | "y" | "z";

class TupleCollection
{
    constructor(private panelIdx: number,
                public tuples: Models.ScatterPlotSourceTuple[] = [])
    {
    }

    previousPanelRemoved()
    {
        this.panelIdx--;
        for (let tuple of this.tuples) tuple.panel = this.panelIdx;
    }
}
