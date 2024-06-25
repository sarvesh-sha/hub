import {ChangeDetectionStrategy, Component} from "@angular/core";
import {UUID} from "angular2-uuid";

import {WidgetBaseComponent} from "app/dashboard/dashboard/widgets/widget-base.component";
import {WidgetGridManipulator} from "app/dashboard/dashboard/widgets/widget-grid-manipulator";
import {WidgetImportDialog} from "app/dashboard/dashboard/widgets/widget-import-dialog";
import {EditWidgetRequest} from "app/dashboard/dashboard/widgets/widget-manager.component";
import {WidgetLayoutConfig, WidgetManipulator} from "app/dashboard/dashboard/widgets/widget-manipulator";
import {ClipboardEntryData} from "app/services/domain/clipboard.service";
import {WidgetGraph, WidgetNode} from "app/services/domain/dashboard-management.service";
import {WidgetConfigurationExtended, WidgetDef} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {PackingAlgorithms} from "framework/ui/charting/core/packing";
import {HotkeyAction} from "framework/utils/keyboard-hotkeys";

@Component({
               selector       : "o3-grouping-widget",
               templateUrl    : "./widget.template.html",
               styleUrls      : ["./widget.styles.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class GroupingWidgetComponent extends WidgetBaseComponent<Models.GroupingWidgetConfiguration, GroupingWidgetConfigurationExtended>
{
    private static readonly INNER_PADDING     = 2;
    private static readonly SUBWIDGET_PADDING = 0;

    private m_widgetManipulator: WidgetManipulator;
    get widgetManipulator(): WidgetManipulator
    {
        return this.ensureWidgetManipulator();
    }

    minOutline: Models.WidgetOutline;

    get addWidgetTooltip(): string
    {
        return this.minOutline ? null : "Not enough space";
    }

    notPastableTooltip: string;

    public ngOnInit(): void
    {
        super.ngOnInit();

        this.ensureWidgetManipulator();
    }

    private ensureWidgetManipulator(): WidgetManipulator
    {
        if (!this.m_widgetManipulator)
        {
            let layoutConfig                 = new WidgetLayoutConfig(this.config.numCols, this.config.numRows);
            this.m_widgetManipulator         = new WidgetManipulator(this.injector, layoutConfig, null, this.config.id, this.dashboard.widgetManipulator);
            this.m_widgetManipulator.editing = this.editing;
        }

        return this.m_widgetManipulator;
    }

    //--//

    async bind()
    {
        await super.bind();

        if (!this.config.name) this.config.name = "Grouping";

        this.subscribeToObservable(this.clipboard.contentsChanged, () => this.updatePastable());
        this.updatePastable();
    }

    protected focusUpdated()
    {
        super.focusUpdated();

        if (!this.focus && this.widgetManipulator)
        {
            this.widgetManipulator.focusId = undefined;
        }
    }

    private updateMinOutline()
    {
        const baseOutline = Models.WidgetOutline.newInstance({
                                                                 width : 1,
                                                                 height: 1
                                                             });
        this.minOutline   = GroupingWidgetConfigurationExtended.getSubwidgetOutline(this.config, baseOutline, this.widgetManipulator.getMinOutline(), this.widgetManipulator.gridManipulator);
    }

    protected dimensionsUpdated()
    {
        this.widgetManipulator.markForCheck();

        super.dimensionsUpdated();
    }

    private updatePastable()
    {
        this.notPastableTooltip = "Nothing to paste";
        for (let data of this.clipboard.getAll())
        {
            let widget = data.getDashboardWidget();
            if (widget instanceof Models.GroupingWidgetConfiguration) continue;

            this.notPastableTooltip = "";
            break;
        }

        this.markForCheck();
    }

    protected editingUpdated()
    {
        if (this.widgetManipulator)
        {
            this.widgetManipulator.editing = this.editing;
        }
    }

    //--//

    protected handleHotkey(action: HotkeyAction)
    {
        switch (action)
        {
            case HotkeyAction.Copy:
                if (!this.widgetManipulator?.focusId)
                {
                    this.copy();
                }
                break;

            case HotkeyAction.Cut:
                if (!this.widgetManipulator?.focusId && this.removable)
                {
                    this.copy();
                    this.remove();
                }
                break;

            case HotkeyAction.Paste:
                if (!this.notPastableTooltip)
                {
                    this.paste();
                }
                break;

            case HotkeyAction.Delete:
                if (!this.widgetManipulator?.focusId && this.removable)
                {
                    this.remove();
                }
                break;
        }
    }

    public deleteMarkedChildren(node: WidgetNode)
    {
        let toDelete = new Set<string>();
        for (let childNode of node.children)
        {
            if (childNode.marked)
            {
                toDelete.add(childNode.widget.id);
            }
        }

        let widgets = this.config.widgets;
        for (let i = widgets.length - 1; i >= 0; i--)
        {
            let id = widgets[i].config.id;
            if (toDelete.has(id))
            {
                this.widgetManipulator.recordWidgetDeletion(id);
                this.config.widgets.splice(i, 1);
            }
        }

        this.widgetsChanged(false);
    }

    public editNestedWidget(widget?: Models.WidgetComposition)
    {
        this.widgetEdit.emit(new EditWidgetRequest(widget, this.config));
    }

    public isAssetGraphRelated(widget: WidgetBaseComponent<any, any>): boolean
    {
        return false;
    }

    async importWidget()
    {
        let widgetInfo = await WidgetImportDialog.open(this, GroupingWidgetComponent.widgetImportValidator);
        if (widgetInfo != null)
        {
            let selectorConfigsToAdd = await this.prepareSelectorConfigs(widgetInfo.config, widgetInfo.graphs, widgetInfo.selectors);
            if (!selectorConfigsToAdd)
            {
                this.insufficientSpaceError();
                return;
            }

            await this.executeEdit(async () => this.pasteImportWidget(widgetInfo.config, widgetInfo.outline, selectorConfigsToAdd));
        }
    }

    private insufficientSpaceError()
    {
        this.app.framework.errors.error("Insufficient space", "There isn't enough space for the widget and all its required asset selectors");
    }

    private static widgetImportValidator(config: Models.WidgetConfiguration): string
    {
        if (config instanceof Models.GroupingWidgetConfiguration)
        {
            return "Nested grouping widgets are not allowed";
        }

        return "";
    }

    async paste()
    {
        await this.executeEdit(async () =>
                               {
                                   for (let data of this.clipboard.getAll())
                                   {
                                       let config = data.getDashboardWidget();
                                       if (config)
                                       {
                                           if (config instanceof Models.GroupingWidgetConfiguration) continue;

                                           let selectorConfigsToAdd = await this.prepareSelectorConfigs(config, data.selectorGraphs, data.selectors);
                                           this.pasteImportWidget(config, data.widgetOutline, selectorConfigsToAdd);
                                       }
                                   }
                               });
    }

    private async prepareSelectorConfigs(config: Models.WidgetConfiguration,
                                         supportingGraphs: Models.SharedAssetGraph[],
                                         selectors: Models.SharedAssetSelector[]): Promise<Models.AssetGraphSelectorWidgetConfiguration[]>
    {
        let graphSelectorsToAdd: Models.AssetGraphSelectorWidgetConfiguration[] = [];
        if (supportingGraphs?.length)
        {
            await this.dashboard.addGraphs(supportingGraphs);

            let spacesAvailable = this.config.numCols * this.config.numRows -
                                  this.config.widgets.reduce((cum,
                                                              widget) => cum + widget.outline.width * widget.outline.height, 0) - 1;

            graphSelectorsToAdd = this.dashboard.getNewAssetGraphSelectors(config, undefined, spacesAvailable);

            for (let selector of selectors) this.dashboard.addSelector(selector);
        }

        return graphSelectorsToAdd;
    }

    private pasteImportWidget(config: Models.WidgetConfiguration,
                              outline: Models.WidgetOutline,
                              selectorConfigs: Models.AssetGraphSelectorWidgetConfiguration[])
    {
        if (!selectorConfigs) return;

        const selectorsAdded = selectorConfigs.every((selectorConfig) => this.addWidget(selectorConfig, this.widgetManipulator.getBaseOutline()));
        if (!selectorsAdded || !this.addWidget(config, outline)) this.insufficientSpaceError();

        this.updateMinOutline();
        this.widgetsChanged(true);
    }

    private addWidget(config: Models.WidgetConfiguration,
                      outline: Models.WidgetOutline): boolean
    {
        if (!outline || outline.width * outline.height < this.minOutline.width * this.minOutline.height)
        {
            outline = this.widgetManipulator.getBaseOutline();
        }

        outline = GroupingWidgetConfigurationExtended.getSubwidgetOutline(this.config, outline, this.widgetManipulator.getMinOutline(), this.widgetManipulator.gridManipulator);
        if (outline)
        {
            config.id = UUID.UUID();

            this.widgetManipulator.updateGridHelper(outline, (row,
                                                              col) => this.widgetManipulator.gridManipulator.setEntry(row, col, config.id));
            this.config.widgets.push(Models.WidgetComposition.newInstance({
                                                                              config : config,
                                                                              outline: outline
                                                                          }));
        }

        return !!outline;
    }

    private widgetsChanged(emitEvent: boolean)
    {
        this.config.widgets = UtilsService.arrayCopy(this.config.widgets);
        if (emitEvent)
        {
            this.contentUpdated.emit();
        }

        this.detectChanges();
    }

    private async executeEdit(action: () => Promise<void>)
    {
        if (this.editing)
        {
            await action();
        }
        else
        {
            this.app.domain.dashboard.triggerEdit(action);
        }
    }

    //--//

    public gridChanged()
    {
        this.updateMinOutline();
        this.markForCheck();
    }

    //--//

    public collectWidgetManipulators(manipulators: WidgetManipulator[])
    {
        super.collectWidgetManipulators(manipulators);

        manipulators.push(this.widgetManipulator);
    }

    protected widgetMarked(graph: WidgetGraph)
    {
        super.widgetMarked(graph);

        for (let widgetContainer of this.widgetManipulator.widgetContainers)
        {
            widgetContainer.widget.mark(graph);
        }
    }

    //--//

    public cannotRemoveTooltip(): string
    {
        return this.removable ? "" : "There are contained asset selectors that are relied on by other widgets";
    }

    private requiresColumnarViewHelper(columnarParent: boolean,
                                       paddingParent: number,
                                       overrideColWidthParent: number): boolean
    {
        const computedWidth    = this.computeWidth(columnarParent, paddingParent, overrideColWidthParent);
        const overrideColWidth = (computedWidth - GroupingWidgetComponent.INNER_PADDING) / this.widgetManipulator.numCols;

        return this.widgetManipulator.widgetContainers.some((widgetContainer) => widgetContainer.widget.requiresColumnarView(GroupingWidgetComponent.SUBWIDGET_PADDING, overrideColWidth));
    }

    public requiresColumnarView(widgetPadding: number,
                                overrideColWidth: number): boolean
    {
        return this.requiresColumnarViewHelper(false, widgetPadding, overrideColWidth);
    }

    public getUpdatedColumnarHeight(): number
    {
        this.m_widgetManipulator.columnar = this.requiresColumnarViewHelper(true, null, null);

        let columnarHeight = super.getUpdatedColumnarHeight();
        if (this.m_widgetManipulator.columnar) columnarHeight *= Math.max(1, this.config.widgets.length);
        return columnarHeight;
    }

    public refreshSize(): Promise<boolean>
    {
        let result = this.widgetManipulator.refresh(!this.loaded);

        let columnarHeight = this.getUpdatedColumnarHeight();
        if (columnarHeight !== this.container.outline.columnarHeight)
        {
            this.container.outline.columnarHeight = columnarHeight;
        }

        return result;
    }

    protected getClipboardData(): ClipboardEntryData<Models.GroupingWidgetConfiguration, null>
    {
        let model                                   = Models.GroupingWidgetConfiguration.deepClone(this.config);
        let selectors: Models.SharedAssetSelector[] = [];

        for (let binding of this.selectorBindings)
        {
            if (!binding.graphId)
            {
                selectors.push(Models.SharedAssetSelector.deepClone(this.dashboard.selectors[binding.selectorId]));
            }
        }

        return new class extends ClipboardEntryData<Models.GroupingWidgetConfiguration, null>
        {
            constructor()
            {
                super("grouping widget");

                this.selectors.push(...selectors);
            }

            public getDashboardWidget(): Models.GroupingWidgetConfiguration
            {
                let res = Models.GroupingWidgetConfiguration.deepClone(model);
                for (let widget of res.widgets)
                {
                    widget.config.id = UUID.UUID();
                }
                return res;
            }

            public getReportItem(): null
            {
                return null;
            }
        }();
    }
}

@WidgetDef({
               friendlyName      : "Grouping Widget",
               typeName          : "GROUPING_WIDGET",
               model             : Models.GroupingWidgetConfiguration,
               component         : GroupingWidgetComponent,
               dashboardCreatable: true,
               subgroupCreatable : false,
               maximizable       : true,
               defaultWidth      : 6,
               defaultHeight     : 4,
               hostScalableText  : false,
               needsProtector    : false,
               documentation     : {
                   description: "The Grouping Widget allows you to create a compact, concise view for your dashboard by allowing you to embed other widgets (except another grouping widget) inside of it without widget headers or borders. It has a configurable grid system to allow even finer display control.",
                   examples   : [
                       {
                           file       : "widgets/GROUPING_WIDGET/grouping.png",
                           label      : "Grouping Widget",
                           description: "Grouping Widget that shows multiple control point and aggregation widgets inside of it, displayed as a 6x6 widget."
                       }
                   ]
               }

           })
export class GroupingWidgetConfigurationExtended extends WidgetConfigurationExtended<Models.GroupingWidgetConfiguration>
{
    protected initializeForWizardInner()
    {
        this.model.numCols = 4;
        this.model.numRows = 4;
        this.model.widgets = [];
    }

    public getBindings(): Models.AssetGraphBinding[]
    {
        let bindings: Models.AssetGraphBinding[] = [];
        for (let widget of this.model.widgets)
        {
            let configExt = WidgetConfigurationExtended.fromConfigModel(widget.config);
            bindings.push(...configExt.getBindings());
        }

        return bindings;
    }

    public static getSubwidgetOutline(config: Models.GroupingWidgetConfiguration,
                                      outline: Models.WidgetOutline,
                                      minOutline: Models.WidgetOutline,
                                      gridManipulator: WidgetGridManipulator<any>): Models.WidgetOutline
    {
        if (!gridManipulator)
        {
            let outlines    = config.widgets.map((widget) => widget.outline);
            gridManipulator = WidgetManipulator.generateMinimalWidgetRepresentation(outlines, config.numRows, config.numCols);
        }

        if (!minOutline)
        {
            minOutline = Models.WidgetOutline.newInstance({
                                                              width : 1,
                                                              height: 1
                                                          });
        }

        if (outline)
        {
            outline.width  = Math.max(outline.width, minOutline.width);
            outline.height = Math.max(outline.height, minOutline.height);
        }

        let res = WidgetManipulator.getPositionedOutline(outline, gridManipulator);
        if (!res)
        {
            let histogram: number[] = [];
            for (let r = 0; r < gridManipulator.numRows; r++)
            {
                // update histogram to reflect this row
                for (let c = 0; c < gridManipulator.numCols; c++)
                {
                    if (gridManipulator.getEntry(r, c))
                    {
                        histogram[c] = 0;
                    }
                    else
                    {
                        histogram[c] = (histogram[c] || 0) + 1;
                    }
                }

                let rect = PackingAlgorithms.histogramMaxAreaRectangle(histogram, outline?.width, outline?.height);
                if (rect?.width >= minOutline.width && rect.height >= minOutline.height &&
                    (!res || rect.width * rect.height > res.width * res.height))
                {
                    res = Models.WidgetOutline.newInstance({
                                                               left  : rect.x,
                                                               top   : r - (rect.height - 1),
                                                               width : rect.width,
                                                               height: rect.height
                                                           });
                }
            }
        }

        return res;
    }
}
