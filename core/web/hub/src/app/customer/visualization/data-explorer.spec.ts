import {ElementRef, NgModule} from "@angular/core";
import {UUID} from "angular2-uuid";

import {UserListPageComponent} from "app/customer/configuration/users/user-list-page.component";
import {DataExplorerPageComponent} from "app/customer/visualization/data-explorer-page.component";
import {SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import * as Models from "app/services/proxy/model/models";
import {ConditionNodeType} from "app/shared/assets/tag-condition-builder/tag-conditions";
import {DataSourceWizardDialogComponent, DataSourceWizardState} from "app/shared/charting/data-source-wizard/data-source-wizard-dialog.component";
import {DataSourceWizardGraphBindingsStepComponent} from "app/shared/charting/data-source-wizard/data-source-wizard-graph-bindings-step.component";
import {DataSourceWizardGraphsStepComponent} from "app/shared/charting/data-source-wizard/data-source-wizard-graphs-step.component";
import {DataSourceWizardSourceTuplesStepComponent} from "app/shared/charting/data-source-wizard/data-source-wizard-source-tuples-step.component";
import {HierarchicalVisualizationConfigurationComponent} from "app/shared/charting/hierarchical-visualization/hierarchical-visualization-configuration.component";
import {HierarchicalVisualizationComponent} from "app/shared/charting/hierarchical-visualization/hierarchical-visualization.component";
import {TimeSeriesChartConfigurationComponent} from "app/shared/charting/time-series-chart/time-series-chart-configuration.component";
import {TimeSeriesContainerComponent} from "app/shared/charting/time-series-container/time-series-container.component";
import {AssetGraphTest} from "app/test/base-tests";
import {assertIsDefined, assertTrue, getCenterPoint, TestCase, waitFor} from "app/test/driver";
import {AssetGraphDriver} from "app/test/drivers/asset-graph-driver";
import {ColorsDriver} from "app/test/drivers/colors-driver";
import {ConfirmationDriver} from "app/test/drivers/confirmation-driver";
import {airflowHeatingSpOptionLabel, co2OptionLabel, DemoDataDriver} from "app/test/drivers/demo-data-driver";
import {FormDriver} from "app/test/drivers/form-driver";
import {OverlayDriver} from "app/test/drivers/overlay-driver";
import {SelectionDriver} from "app/test/drivers/selection-driver";
import {SidebarDriver} from "app/test/drivers/sidebar-driver";
import {TabGroupDriver} from "app/test/drivers/tab-group-driver";
import {WizardDriver} from "app/test/drivers/wizard-driver";

import {CanvasZoneSelectionType} from "framework/ui/charting/app-charting-utilities";
import {ProcessedGroup} from "framework/ui/charting/chart.component";
import {ChartColorUtilities} from "framework/ui/charting/core/colors";
import {VisualizationDataSourceState} from "framework/ui/charting/core/data-sources";
import {DialogConfirmComponent} from "framework/ui/dialogs/dialog-confirm.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {SelectComponent} from "framework/ui/forms/select.component";
import {WizardComponent} from "framework/ui/wizards/wizard.component";

abstract class DataExplorerTest extends AssetGraphTest
{
    protected m_page: DataExplorerPageComponent;

    protected m_numAhus: number;

    protected m_assetGraphDriver: AssetGraphDriver = this.m_driver.getDriver(AssetGraphDriver);
    protected m_colorsDriver: ColorsDriver         = this.m_driver.getDriver(ColorsDriver);
    protected m_demoDataDriver: DemoDataDriver     = this.m_driver.getDriver(DemoDataDriver);
    protected m_formDriver: FormDriver             = this.m_driver.getDriver(FormDriver);
    protected m_overlayDriver: OverlayDriver       = this.m_driver.getDriver(OverlayDriver);
    protected m_selectionDriver: SelectionDriver   = this.m_driver.getDriver(SelectionDriver);
    protected m_sidebarDriver: SidebarDriver       = this.m_driver.getDriver(SidebarDriver);
    protected m_tabGroupDriver: TabGroupDriver     = this.m_driver.getDriver(TabGroupDriver);
    protected m_wizardDriver: WizardDriver         = this.m_driver.getDriver(WizardDriver);

    protected async navigateToDataExplorer()
    {
        this.m_page = await this.m_driver.navigate(DataExplorerPageComponent, "/visualization/explorer");
    }

    public async init(): Promise<void>
    {
        await super.init();

        await this.navigateToDataExplorer();

        this.m_numAhus = await this.m_demoDataDriver.getNumAhus();
    }

    public async cleanup(): Promise<void>
    {
        // delete all entries from data explorer page
        await this.deleteAll();
    }

    public async deleteAll(): Promise<void>
    {
        await this.m_page.chartSetComponent.toolbar.deleteHandler(true);
    }

    private async saveWizard(wizard: WizardComponent<DataSourceWizardState>,
                             timeSeriesContainer: TimeSeriesContainerComponent): Promise<void>
    {
        await this.m_wizardDriver.save(wizard, "Could not save chart source changes");
        await waitFor(() => !timeSeriesContainer.updating, "chart did not stop loading");
    }

    protected async createNewChart(wizardFn: (wizard: WizardComponent<DataSourceWizardState>) => Promise<void>): Promise<TimeSeriesContainerComponent>
    {
        const timeSeriesSet = this.m_page.chartSetComponent.set;
        const numCharts     = timeSeriesSet.timeSeriesContainers.length;

        const addChartBtn = await waitFor(() => this.m_page.chartSetComponent.toolbar?.test_addChartButton, "Could not get add chart button");
        await this.m_driver.click(addChartBtn, "Add chart button");
        await waitFor(() => timeSeriesSet.timeSeriesContainers.length === numCharts + 1, "Chart wasn't added");

        const timeSeriesContainer = await this.getTimeSeriesContainer(numCharts);
        const wizard              = await this.getDataSourceWizard();
        await wizardFn(wizard);
        await this.saveWizard(wizard, timeSeriesContainer);

        return timeSeriesContainer;
    }

    protected async configureChartSources(timeSeriesContainer: TimeSeriesContainerComponent,
                                          wizardFn: (wizard: WizardComponent<DataSourceWizardState>) => Promise<void>,
                                          add: boolean): Promise<void>
    {
        if (add)
        {
            await this.selectTimeSeriesContainerButton(timeSeriesContainer.test_addSources, "add sources button");
        }
        else
        {
            await this.selectTimeSeriesContainerButton(timeSeriesContainer.test_editSources, "edit sources button");
        }
        const wizard = await this.getDataSourceWizard();

        await wizardFn(wizard);
        await this.saveWizard(wizard, timeSeriesContainer);
    }

    protected async selectTimeSeriesContainerButton(button: ElementRef<HTMLElement> | HTMLElement,
                                                    buttonName: string): Promise<void>
    {
        if (button instanceof ElementRef) button = button.nativeElement;

        await waitFor(() => parseInt(getComputedStyle(<HTMLElement>button).width) === 40, "button never got to its final width");
        await this.m_driver.click(button, buttonName);
    }

    protected getTimeSeriesContainer(idx: number = 0): Promise<TimeSeriesContainerComponent>
    {
        return waitFor(() => this.m_page.chartSetComponent.set.timeSeriesContainers[idx], "Could not get time series container at idx " + idx);
    }

    protected getDataSourceWizard(): Promise<WizardComponent<DataSourceWizardState>>
    {
        return this.m_wizardDriver.getWizard<DataSourceWizardState, DataSourceWizardDialogComponent>(DataSourceWizardDialogComponent);
    }

    protected getLocalBindingsSelect(bindingsStep: DataSourceWizardGraphBindingsStepComponent): Promise<SelectComponent<string>>
    {
        return waitFor(() =>
                       {
                           if (!bindingsStep.test_bindingsLocal?.options.length) bindingsStep.markForCheck();
                           return bindingsStep.test_bindingsLocal;
                       }, "Could not get local bindings select");
    }
}

@TestCase({
              id        : "dataExplorer_loads",
              name      : "Data Explorer Page loads",
              categories: ["Data Explorer"]
          })
class DataExplorerLoadsTest extends DataExplorerTest
{
    public async execute(): Promise<void>
    {
        // validate the + button on the data explorer page shows up
        await waitFor(() => this.m_page.chartSetComponent.toolbar?.test_addChartButton.nativeElement.textContent === "add_circle", "No 'Add a chart' button found in toolbar");
    }
}

@TestCase({
              id        : "dataExplorer_addChart",
              name      : "Add a Chart",
              categories: ["Data Explorer"]
          })
class AddChartTest extends DataExplorerTest
{
    public async execute(): Promise<void>
    {
        await this.createNewChart(async (wizard) =>
                                  {
                                      await this.m_wizardDriver.stepNTimes(wizard, 1);
                                      await this.m_wizardDriver.standardSelectControlPoints(wizard, "outside air temp", this.m_numAhus, 1);
                                  });

        // validate there is at least 1 chart added to the explorer page and the point drawn matches it
        const chartSet = this.m_page.chartSetComponent.set;
        await waitFor(() => chartSet.configExts.length > 0 && chartSet.configExts[0].sourcesExt[0].name === "Outside Air Temperature",
                      "No chart added to the explorer page or no control point displayed");
    }
}

abstract class HierarchicalChartTest extends DataExplorerTest
{
    public async init(): Promise<void>
    {
        await super.init();

        if (await this.ensureVavDatGraph())
        {
            await this.navigateToDataExplorer();
        }
    }

    protected async createHierarchicalChart(): Promise<TimeSeriesContainerComponent>
    {
        return this.createNewChart(async (wizard) =>
                                   {
                                       await this.m_wizardDriver.selectDataSourceType(wizard, Models.TimeSeriesChartType.HIERARCHICAL);

                                       const graphsStep = await this.m_wizardDriver.getStep(wizard, DataSourceWizardGraphsStepComponent);
                                       await this.m_wizardDriver.waitForWizard(wizard);
                                       await this.m_assetGraphDriver.addImportAssetGraphFlow(graphsStep, AssetGraphTest.vavDatGraphName, true, true);

                                       await this.m_wizardDriver.stepNTimes(wizard, 1);
                                       const bindingsStep = await this.m_wizardDriver.getStep(wizard, DataSourceWizardGraphBindingsStepComponent);

                                       let vavDatGraph     = wizard.data.graphsHost.getGraphs()
                                                                   .find((sharedGraph) => sharedGraph.name === AssetGraphTest.vavDatGraphName);
                                       const localBindings = await this.getLocalBindingsSelect(bindingsStep);
                                       await this.m_selectionDriver.makeBindingSelections(localBindings,
                                                                                          new SharedAssetGraphExtended(this.m_driver.app.domain, vavDatGraph),
                                                                                          [vavDatGraph.graph.nodes[1].id]);

                                       await this.m_wizardDriver.stepNTimes(wizard, 1);
                                   });
    }

    protected async configureChart(timeSeriesContainer: TimeSeriesContainerComponent,
                                   editFn: (configurer: HierarchicalVisualizationConfigurationComponent) => Promise<void>): Promise<void>
    {
        const hierarchicalChart = await waitFor(() => timeSeriesContainer.hierarchyElement, "Could not get hierarchical chart");
        await this.selectTimeSeriesContainerButton(timeSeriesContainer.test_settings, "chart settings button");
        await this.m_overlayDriver.waitForOpen(hierarchicalChart.chartOptionsConfig.optio3TestId);

        await editFn(hierarchicalChart.test_configurer);

        await this.m_overlayDriver.closeOverlay(hierarchicalChart.optionsDialog);
    }
}

@TestCase({
              id        : "dataExplorer_hierarchical_multipleBindings",
              name      : "Hierarchical Multiple Bindings test",
              timeout   : 100,
              categories: ["Data Explorer"]
          })
class HierarchicalMultipleBindingsTest extends HierarchicalChartTest
{
    public async execute(): Promise<void>
    {
        const timeSeriesContainer = await this.createHierarchicalChart();
        const hierarchicalChart   = await waitFor(() => timeSeriesContainer.hierarchyElement, "Could not get hierarchical chart");

        await this.configureChart(timeSeriesContainer, async (configurer) =>
        {
            const visualizationTypeSelect = await waitFor(() => configurer.test_type, "Could not get visualization type select");
            const heatmapOption           = configurer.types.find((option) => option.id === Models.HierarchicalVisualizationType.HEATMAP);
            await this.m_selectionDriver.makeSelection("hierarchical visualization type", visualizationTypeSelect, [heatmapOption.id]);
        });
        await waitFor(() => hierarchicalChart.numHeatmaps === this.m_numAhus * 5, `Should have ${this.m_numAhus * 5} heatmaps in view instead of ${hierarchicalChart.numHeatmaps}`);

        await this.configureChartSources(timeSeriesContainer, async (wizard) =>
        {
            await this.m_wizardDriver.waitForWizard(wizard);
            const graphsStep           = await this.m_wizardDriver.getStep(wizard, DataSourceWizardGraphsStepComponent);
            const localGraphs          = await this.m_assetGraphDriver.getMultipleGraphsConfig(graphsStep, true);
            const graphConfigureButton = await waitFor(() => localGraphs.test_configureGraphs?.last, "Could not get local graph configure button");
            await this.m_driver.click(graphConfigureButton, "configure graph button");

            const htSpName = "Heat Setpoint";
            await this.m_assetGraphDriver.assetStructureWizardFlow("", async (assetGraphStep) =>
            {
                const assetGraph       = await waitFor(() => assetGraphStep.graph?.nodes.length && assetGraphStep.graph.transforms.length && assetGraphStep.graph, "Could not get asset graph");
                const assetGraphEditor = await waitFor(() => assetGraphStep.test_graphEditor, "Could not get asset graph editor");
                await this.m_assetGraphDriver.addAssetGraphNode(assetGraphEditor, assetGraph.nodes[0].id, htSpName, ConditionNodeType.POINT, false, airflowHeatingSpOptionLabel);
                await waitFor(() => assetGraph.nodes.length === 3, "Did not successfully add heat setpoint node");
            }, true);
            await waitFor(() => graphsStep.isValid(), "Graph step did not validate after completing hierarchical query");

            await this.m_wizardDriver.stepNTimes(wizard, 1);

            const assetGraph    = wizard.data.graphsHost.getGraphs()[0];
            const graphExt      = new SharedAssetGraphExtended(this.m_driver.app.domain, assetGraph);
            const bindingsStep  = await this.m_wizardDriver.getStep(wizard, DataSourceWizardGraphBindingsStepComponent);
            const binding       = assetGraph.graph.nodes.find((node) => node.name === htSpName);
            const localBindings = await this.getLocalBindingsSelect(bindingsStep);
            await this.m_selectionDriver.makeBindingSelections(localBindings, graphExt, [binding.id]);
        }, false);
        const expectedCpCt = this.m_numAhus * 5;
        await waitFor(() => hierarchicalChart.numHeatmaps === expectedCpCt && hierarchicalChart.numTrendlines === expectedCpCt,
                      `Expected ${expectedCpCt * 2} control points from ${this.m_numAhus} AHUs: 5 VAVs per AHU, 1 DAT and HtSp per VAV;
                      Got ${hierarchicalChart.numHeatmaps} heatmaps and ${hierarchicalChart.numTrendlines} trend lines instead`);

        const prevRows = hierarchicalChart.rows;
        await this.configureChart(timeSeriesContainer, async (configurer) =>
        {
            const sizingSelect = await waitFor(() => configurer.test_rowSizing, "Could not get row sizing select");
            const fixedOption  = configurer.sizings.find((option) => option.id === Models.HierarchicalVisualizationSizing.FIXED);
            await this.m_selectionDriver.makeSelection("row sizing", sizingSelect, [fixedOption.id]);

            let otherOption = hierarchicalChart.leafNodeOptions.find((option) => option.id !== hierarchicalChart.selectedLeafNodeId);
            await this.m_selectionDriver.makeSelection("edit binding", hierarchicalChart.test_editBinding, [otherOption.id]);
            await waitFor(() => hierarchicalChart.selectedLeafNodeId === otherOption.id, "Did not successfully start editing other binding");
            await this.m_selectionDriver.makeSelection("row sizing", sizingSelect, [fixedOption.id]);
        });
        const rowHeight = await waitFor(() =>
                                        {
                                            let heights: number;
                                            for (let leafNodeId in hierarchicalChart.bindingInfoLookup)
                                            {
                                                let bindingInfo = hierarchicalChart.bindingInfoLookup[leafNodeId];
                                                if (bindingInfo.options.sizing !== Models.HierarchicalVisualizationSizing.FIXED)
                                                {
                                                    return null;
                                                }
                                                else
                                                {
                                                    heights = bindingInfo.options.size;
                                                }
                                            }
                                            return heights;
                                        }, "Hierarchical chart's rows did not all become a fixed height");
        await waitFor(() => prevRows !== hierarchicalChart.rows, "Hierarchical chart's rows never updated");

        let numInView          = await waitFor(() => this.numRowsInView(hierarchicalChart), "New rows never got marked as in view");
        const numRowsPerScroll = 5;
        while (numInView < hierarchicalChart.rows.length)
        {
            await this.m_driver.scroll(numRowsPerScroll * rowHeight);
            let inViewCt;
            numInView = await waitFor(() =>
                                      {
                                          inViewCt = this.numRowsInView(hierarchicalChart);
                                          return inViewCt - numRowsPerScroll === numInView || inViewCt === hierarchicalChart.rows.length ? inViewCt : null;
                                      }, `Found ${inViewCt} rows instead of the expected ${numRowsPerScroll + numInView} or all ${hierarchicalChart.rows.length}`);
        }
        await waitFor(() => hierarchicalChart.numHeatmaps + hierarchicalChart.numTrendlines === numInView, `Only got ${numInView} rows instead of the expected ${hierarchicalChart.numHeatmaps}`);
    }

    private numRowsInView(hierarchicalChart: HierarchicalVisualizationComponent): number
    {
        return hierarchicalChart.rows.reduce((cum,
                                              row) => row.inView ? cum + 1 : cum, 0);
    }
}

abstract class LineChartTest extends DataExplorerTest
{
    private m_confirmationDriver: ConfirmationDriver = this.m_driver.getDriver(ConfirmationDriver);

    protected async createChart(): Promise<TimeSeriesContainerComponent>
    {
        return this.createNewChart(async (wizard) =>
                                   {
                                       await this.m_wizardDriver.stepNTimes(wizard, 1);
                                       await this.m_wizardDriver.standardSelectControlPoints(wizard, "outside air temperature", this.m_numAhus);
                                   });
    }

    protected async configureChart(timeSeriesContainer: TimeSeriesContainerComponent,
                                   editFn: (configurer: TimeSeriesChartConfigurationComponent) => Promise<void>): Promise<void>
    {
        const lineChart = await waitFor(() => timeSeriesContainer.chartElement, "Could not get line chart");
        await this.selectTimeSeriesContainerButton(timeSeriesContainer.test_settings, "chart settings button");
        await this.m_overlayDriver.waitForOpen(lineChart.chartConfigurer.overlayConfig.optio3TestId);

        await editFn(lineChart.chartConfigurer);

        await waitFor(() => !lineChart.chartConfigurer.pristine, "Changes did not make the config dirty");
        await this.m_formDriver.submitForm(lineChart.chartConfigurer.overlay.form, "Save");
        if (await this.waitForConfirmationDialog()) await this.m_confirmationDriver.handleConfirmationDialog();
        await this.m_overlayDriver.waitForClose(lineChart.chartConfigurer.overlayConfig.optio3TestId);
    }

    private async waitForConfirmationDialog(): Promise<DialogConfirmComponent>
    {
        let dialog: DialogConfirmComponent;
        try
        {
            dialog = await this.m_driver.getComponent(DialogConfirmComponent);
        }
        catch (e)
        {
            // ignore: just return null
            dialog = null;
        }

        return dialog;
    }
}

@TestCase({
              id        : "dataExplorer_annotation",
              name      : "Annotation test",
              categories: ["Data Explorer"],
              timeout   : 50
          })
class AnnotationTest extends LineChartTest
{
    public async execute(): Promise<void>
    {
        const timeSeriesContainer = await this.createChart();
        await waitFor(() => timeSeriesContainer.configExt.sourcesExt.length === this.m_numAhus, "chart did not get the expected number of OATs");

        await this.newAnnotation(async (canvas) =>
                                 {
                                     const canvasCenter = getCenterPoint(canvas);
                                     canvasCenter.y -= 50;
                                     const lowerPoint   = {...canvasCenter};
                                     lowerPoint.y += 100;
                                     await this.m_driver.clickAndDrag(canvasCenter, lowerPoint);
                                 }, timeSeriesContainer, CanvasZoneSelectionType.YRange, "Less Stable", "Will invalidate when all sources are moved to right axis");
        await this.newAnnotation(async (canvas) =>
                                 {
                                     const dataSourceGroup = await waitFor(() => timeSeriesContainer.chartElement.optio3Chart?.test_panels[0]?.groups[0], "Could not get chart group");
                                     const dataSource      = await waitFor(() => dataSourceGroup.sources[0]?.dataSource, "Could not get first data source");
                                     const middlePoint     = dataSource.findNearestPoint(MomentHelper.subtract(MomentHelper.now(), 12, "hour"));
                                     const canvasRect      = canvas.getBoundingClientRect();
                                     const point           = {
                                         x: canvasRect.x + dataSourceGroup.transform.fromMillisecondToXCoordinate(middlePoint.timestampInMillisec),
                                         y: canvasRect.y + dataSourceGroup.transform.fromValueToYCoordinate(middlePoint.numberValue)
                                     };
                                     await this.m_driver.clickPoint(point, "central chart point");
                                 }, timeSeriesContainer, CanvasZoneSelectionType.Point, "Stable");
        await this.m_overlayDriver.closeOverlay(timeSeriesContainer.chartElement.annotationDialog.overlay);
        await waitFor(() => timeSeriesContainer.configExt.model.annotations.length === 2, "Config didn't update to reflect two annotations");
        await waitFor(() => timeSeriesContainer.configExt.sourcesExt.every((sourceExt) => sourceExt.model.axis === 0), "All sources were not on left axis");

        await this.configureChart(timeSeriesContainer, async (configurer) =>
        {
            await this.m_tabGroupDriver.changeTab(configurer.test_tabGroup, "Sources");
            const sourceConfigurer = await waitFor(() => configurer.test_sources, "could not get source configurer");
            const toRightButtons   = await waitFor(() => sourceConfigurer.test_toRightAxis?.length === 3 && sourceConfigurer.test_toRightAxis,
                                                   "Could not get buttons to move sources to right axis");
            for (let button of toRightButtons) await this.m_driver.click(button, "to right axis button");
        });
        await waitFor(() => timeSeriesContainer.configExt.sourcesExt.every((sourceExt) => sourceExt.model.axis === 1), "All sources were not on right axis");
        await waitFor(() => timeSeriesContainer.configExt.model.annotations.length === 1 && timeSeriesContainer.configExt.model.annotations[0].type === Models.TimeSeriesAnnotationType.Point,
                      "1 annotation should have been invalidated to leave 1 annotation left");
    }

    private async newAnnotation(buildFn: (canvas: HTMLCanvasElement) => Promise<void>,
                                timeSeriesContainer: TimeSeriesContainerComponent,
                                annotationType: CanvasZoneSelectionType,
                                annotationName: string,
                                annotationDescription?: string): Promise<void>
    {
        const annotationDialog = await waitFor(() => timeSeriesContainer.chartElement.annotationDialog, "Could not get annotation dialog");
        if (!this.m_overlayDriver.overlayPresent(annotationDialog.overlayConfig.optio3TestId))
        {
            await this.selectTimeSeriesContainerButton(timeSeriesContainer.test_annotations, "annotation overlay button");
            await this.m_overlayDriver.waitForOpen(annotationDialog.overlayConfig.optio3TestId);
        }

        const annotationOption = annotationDialog.annotationTypeOptions.find((option) => option.id === annotationType);
        await this.m_selectionDriver.selectMenuOption("add annotation", annotationDialog.test_table.test_add, annotationOption.label);
        await this.m_overlayDriver.waitForClose(annotationDialog.overlayConfig.optio3TestId);
        const canvas = timeSeriesContainer.element.nativeElement.querySelector("canvas");

        await buildFn(canvas);

        await this.m_overlayDriver.waitForOpen(annotationDialog.editOverlayConfig.optio3TestId);
        const nameInput = await waitFor(() => annotationDialog.test_name, "Could not get annotation name input");
        await this.m_driver.sendText(nameInput, "new annotation name input", annotationName || UUID.UUID());
        if (annotationDescription)
        {
            await this.m_driver.sendText(annotationDialog.test_description, "new annotation description input", annotationDescription);
        }
        await this.m_formDriver.submitOverlayForm(annotationDialog.editOverlay, "Save");
    }
}

@TestCase(
    {
        id        : "dataExplorer_sourceChipInteractions",
        name      : "Source Chip Interactions test",
        categories: ["Data Explorer"]
    }
)
class SourceChipInteractionsTest extends LineChartTest
{
    public async execute(): Promise<void>
    {
        const timeSeriesContainer = await this.createChart();
        const standardLayout      = await this.m_driver.getStandardLayoutComponent();
        await this.m_sidebarDriver.ensureSidebarOpen(standardLayout);
        const consolidatedChipComponent = await waitFor(() => timeSeriesContainer.test_consolidated, "Could not find consolidated chip");
        await this.m_driver.click(consolidatedChipComponent.test_consolidated, "consolidated source chip");
        await this.m_overlayDriver.waitForOpen(consolidatedChipComponent.sourcesOverlayConfig.optio3TestId);
        const chartComponent    = await waitFor(() => timeSeriesContainer.chartElement?.optio3Chart, "Could not get chart component");
        const chartGroup        = await waitFor(() => chartComponent.test_panels[0]?.groups[0], "Could not get chart's source group");
        let firstOverlayChip    = await waitFor(() => consolidatedChipComponent.test_overlayChips.first, "could not get first overlay chip");
        let firstChipIdentifier = firstOverlayChip.source.identifier;
        await this.m_driver.hover(firstOverlayChip.element, "first overlay chip");
        await this.checkSourceStates(timeSeriesContainer, chartGroup, firstChipIdentifier, VisualizationDataSourceState.Target, VisualizationDataSourceState.Muted);
        await this.m_driver.hover(consolidatedChipComponent.sourcesOverlay.test_close, "source overlay close button");
        await this.checkAllSourceStates(chartGroup);
        await this.m_driver.click(firstOverlayChip.element, "first overlay chip");
        await this.checkSourceStates(timeSeriesContainer, chartGroup, firstChipIdentifier, VisualizationDataSourceState.Target, VisualizationDataSourceState.Muted);
        await this.m_driver.click(firstOverlayChip.test_delete, "first overlay chip's delete button");
        await waitFor(() => firstOverlayChip.isDeleting, "First overlay chip never got marked for deletion");
        await this.checkSourceStates(timeSeriesContainer, chartGroup, firstChipIdentifier, VisualizationDataSourceState.Deleted, VisualizationDataSourceState.Active);
        await this.m_driver.click(firstOverlayChip.test_delete, "first overlay chip's cancel delete button");
        await waitFor(() => !firstOverlayChip.isDeleting, "first overlay chip is still marked for deletion");
        await this.checkAllSourceStates(chartGroup, VisualizationDataSourceState.Active);
        await this.m_driver.click(firstOverlayChip.test_delete, "first overlay chip's delete button");
        await waitFor(() => firstOverlayChip.isDeleting, "first overlay chip didn't get marked for deletion");
        await this.m_overlayDriver.closeOverlay(consolidatedChipComponent.sourcesOverlay);
        await waitFor(() => timeSeriesContainer.configExt.sourcesExt.length === 2, "Source did not delete");

        await waitFor(() => consolidatedChipComponent.sourceChips.length === 2, "Did not get two unconsolidated source chips");
        firstOverlayChip    = consolidatedChipComponent.sourceChips.first;
        firstChipIdentifier = firstOverlayChip.source.identifier;
        await this.m_driver.click(firstOverlayChip.test_disable, "first chip's disable button", 2);
        await this.checkSourceStates(timeSeriesContainer, chartGroup, firstChipIdentifier, VisualizationDataSourceState.Active, VisualizationDataSourceState.Disabled, 1);
        await this.m_driver.click(firstOverlayChip.test_disable, "first chip's disable button");
        await this.checkAllSourceStates(chartGroup, VisualizationDataSourceState.Disabled, 1);
        await this.m_driver.click(firstOverlayChip.test_disable, "first chip's disable button");
        await this.checkSourceStates(timeSeriesContainer, chartGroup, firstChipIdentifier, VisualizationDataSourceState.Active, VisualizationDataSourceState.Disabled, 1);
    }

    private async checkAllSourceStates(group: ProcessedGroup,
                                       type: VisualizationDataSourceState = VisualizationDataSourceState.Active,
                                       retries: number                    = 1): Promise<void>
    {
        for (let source of group.sources)
        {
            await waitFor(() => source.state === type || source.state === VisualizationDataSourceState.Deleted, "All sources should be " + type, undefined, retries);
        }
    }

    private async checkSourceStates(timeSeriesContainer: TimeSeriesContainerComponent,
                                    group: ProcessedGroup,
                                    primaryIdentifier: string,
                                    primaryType: VisualizationDataSourceState,
                                    secondaryType: VisualizationDataSourceState,
                                    retries: number = 0): Promise<void>
    {
        let sourceExt = timeSeriesContainer.configExt.sourcesExt.find((sourceExt) => sourceExt.identifier === primaryIdentifier);
        assertIsDefined(sourceExt, `sourceExt with identifier === ${primaryIdentifier}`);

        let primarySource = sourceExt.getChartData();
        for (let source of group.sources)
        {
            await waitFor(() =>
                          {
                              if (source.dataSource === primarySource)
                              {
                                  return source.state === primaryType;
                              }
                              return source.state === secondaryType || source.state === VisualizationDataSourceState.Deleted;
                          },
                          `${source.dataSource === primarySource ? `source is ${source.state} instead of ${primaryType}` : `source is ${source.state} instead of ${secondaryType}`}`,
                          undefined,
                          retries);
        }
    }
}

@TestCase(
    {
        id        : "dataExplorer_assetGraphConfigurability",
        name      : "Asset Graph Configurability test",
        timeout   : 80,
        categories: ["Data Explorer"]
    }
)
class AssetGraphConfigurabilityTest extends LineChartTest
{
    public async init(): Promise<void>
    {
        await this.ensureLoggedIn();
        await this.ensureVavDatGraph();
        await super.init();
    }

    public async execute(): Promise<void>
    {
        let timeSeriesContainer = await this.createNewChart(async (wizard) =>
                                                            {
                                                                await this.m_wizardDriver.selectDataSourceType(wizard, Models.TimeSeriesChartType.GRAPH);

                                                                const graphsStep = await this.m_wizardDriver.getStep(wizard, DataSourceWizardGraphsStepComponent);
                                                                await this.m_assetGraphDriver.addImportAssetGraphFlow(graphsStep, AssetGraphTest.vavDatGraphName, true, true);
                                                                await this.m_wizardDriver.stepNTimes(wizard, 1);

                                                                const bindingsStep   = await this.m_wizardDriver.getStep(wizard, DataSourceWizardGraphBindingsStepComponent);
                                                                const bindingsSelect = await this.getLocalBindingsSelect(bindingsStep);
                                                                await this.m_selectionDriver.makeOnlyAvailableSelection("multiple bindings", bindingsSelect, true);
                                                            });

        await this.configureChart(timeSeriesContainer, async (configurer) =>
        {
            await this.m_tabGroupDriver.changeTab(configurer.test_tabGroup, "Sources");
            const sourceConfigurer = await waitFor(() => configurer.test_sources, "Could not get source configurer");
            const toRight          = await waitFor(() => sourceConfigurer.test_toRightAxis?.first, "Could not get to right button");
            await this.m_driver.click(toRight, "to right axis button");
        });
        await waitFor(() => timeSeriesContainer.configExt.model.dataSources[0].axis === 1, "Source did not move to right axis");

        const contextOptions = timeSeriesContainer.configExt.assetSelectionHelper.currentAssetOptions[0];
        await this.m_selectionDriver.makeSelection("Context select", timeSeriesContainer.test_contexts, [contextOptions.list[1].id]);
        await waitFor(() => timeSeriesContainer.configExt.model.dataSources[0].axis === 1, "Source did not stay on right axis");

        await this.m_selectionDriver.makeMultiSelection("Context select", timeSeriesContainer.test_contexts, [[contextOptions.list[0].id]]);
        await waitFor(() => timeSeriesContainer.configExt.model.dataSources.length === 2, "Did not add another source via new context selection");
        await waitFor(() => timeSeriesContainer.configExt.model.dataSources.every((source) => source.axis === 1),
                      "The newly added source was not correctly placed on the right axis");

        await this.configureChart(timeSeriesContainer, async (configurer) =>
        {
            await this.m_tabGroupDriver.changeTab(configurer.test_tabGroup, "Sources");
            const sourceConfigurer = await waitFor(() => configurer.test_sources, "Could not get source configurer");
            await waitFor(() => sourceConfigurer.test_sourceDrags.length === 2, "configurer did not load in both sources");
            await this.m_driver.clickAndDrag(sourceConfigurer.test_sourceDrags.last.nativeElement,
                                             sourceConfigurer.test_newPanel.nativeElement, "source drag icon");
        });
        await waitFor(() => timeSeriesContainer.configExt.panelsExt.length === 2, "Don't have 2 panels as is expected");

        let selection = [
            [contextOptions.list[0].id],
            [contextOptions.list[1].id],
            [contextOptions.list[2].id],
            [contextOptions.list[3].id]
        ];
        await this.m_selectionDriver.makeMultiSelection("Context select", timeSeriesContainer.test_contexts, selection);
        await this.finalCheck(timeSeriesContainer);

        await this.m_driver.navigate(UserListPageComponent, "/configuration/users");
        await this.navigateToDataExplorer();
        timeSeriesContainer = await this.getTimeSeriesContainer();
        await this.finalCheck(timeSeriesContainer);
    }

    private async finalCheck(timeSeriesContainer: TimeSeriesContainerComponent): Promise<void>
    {
        await waitFor(() => timeSeriesContainer.configExt.sourcesExt.length === 2,
                      "Should have 2 sources instead of " + timeSeriesContainer.configExt.sourcesExt.length);

        let numOnRight  = 0;
        let numOnFirst  = 0;
        let numOnSecond = 0;
        for (let sourceExt of timeSeriesContainer.configExt.sourcesExt)
        {
            if (sourceExt.model.axis === 1) numOnRight++;
            if (sourceExt.model.panel === 0) numOnFirst++;
            if (sourceExt.model.panel === 1) numOnSecond++;
        }
        assertTrue(numOnRight === 2, "both sources should be on right axis");
        assertTrue(numOnFirst === 1 && numOnSecond === 1, "there should be a source on each of the two panels");
    }
}

@TestCase({
              id        : "dataExplorer_standardScatter",
              name      : "Standard Scatter Plot test",
              timeout   : 45,
              categories: ["Data Explorer"]
          })
class StandardScatterPlotTest extends DataExplorerTest
{
    public async execute(): Promise<void>
    {
        const timeSeriesContainer = await this.createNewChart(async (wizard) =>
                                                              {
                                                                  await this.m_wizardDriver.selectDataSourceType(wizard, Models.TimeSeriesChartType.SCATTER);
                                                                  await this.m_wizardDriver.standardSelectControlPoints(wizard, "discharge air temperature", Math.min(this.m_numAhus * 5, 10), 3);
                                                                  await this.m_wizardDriver.stepNTimes(wizard, 1);
                                                                  const tupleStep    = await this.m_wizardDriver.getStep(wizard, DataSourceWizardSourceTuplesStepComponent);
                                                                  const tupleOptions = await waitFor(() => tupleStep.standardOptions, "Could not get standard tuple options");
                                                                  const xStandard    = await waitFor(() => tupleStep.test_xStandards.first, "Could not get x select");
                                                                  await this.m_selectionDriver.makeSelection("tuple's x select", xStandard, [tupleOptions[0].id]);
                                                                  await this.m_selectionDriver.makeSelection("tuple's y select", tupleStep.test_yStandards.first, [tupleOptions[1].id]);
                                                                  await waitFor(() => tupleStep.isValid(), "tuple step should be valid");
                                                                  await this.m_selectionDriver.makeSelection("tuple's y select", tupleStep.test_yStandards.first, [tupleOptions[0].id]);
                                                                  await waitFor(() => !tupleStep.isValid(), "tuple step should not be valid: two of the same control point");
                                                                  await this.m_selectionDriver.makeSelection("tuple's x select", xStandard, [tupleOptions[1].id]);
                                                                  await waitFor(() => tupleStep.isValid(), "tuple step should be valid once again");
                                                              });

        const consolidated = timeSeriesContainer.test_consolidated;
        const tupleChip    = await waitFor(() => consolidated.sourceChips.first, "Could not get source chip");
        const configureId  = tupleChip.actions[0].optio3TestId(consolidated.sourceExts[0].source.identifier);
        await this.m_driver.clickO3Element(configureId, "tuple chip's configure button");
        const scatterChart = timeSeriesContainer.scatterElement;
        await this.m_overlayDriver.waitForOpen(scatterChart.tupleEditConfig.optio3TestId);
        await this.m_colorsDriver.pickColor(scatterChart.test_color, "red", "tuple");
        await this.m_formDriver.submitOverlayForm(scatterChart.tupleConfigurerContainer, "Save");
        await waitFor(() => scatterChart.scatterPlot.test_panels[0].sourceTuples[0].sourceTuple.color === ChartColorUtilities.getDefaultColorById("red").hex,
                      "tuple color is not red as expected");

        await this.configureChartSources(timeSeriesContainer, async (wizard) =>
        {
            await this.m_wizardDriver.standardSelectControlPoints(wizard, "outside air temperature", this.m_numAhus, 1);
            await this.m_wizardDriver.stepNTimes(wizard, 1);
            const tupleStep     = await this.m_wizardDriver.getStep(wizard, DataSourceWizardSourceTuplesStepComponent);
            const tupleOptions  = await waitFor(() => tupleStep.standardOptions, "Could not get standard tuple options");
            const colorStandard = await waitFor(() => tupleStep.test_zStandards?.first, "Could not get color tuple dropdown");
            await this.m_selectionDriver.makeSelection("color tuple standard selection", colorStandard, [tupleOptions[2].id]);
            await waitFor(() => tupleStep.isValid(), "Tuple step should remain valid after selecting a third source for tuple");
        }, true);

        const scatterContainer = await waitFor(() => timeSeriesContainer.scatterElement, "could not get scatter container");
        let scatterPanels      = await waitFor(() => scatterContainer.scatterPlot?.test_panels?.length && scatterContainer.scatterPlot.test_panels,
                                               "could not get scatter chart computed panels");
        const gradientStops    = await waitFor(() =>
                                               {
                                                   const stops = scatterPanels[0].rawPanel?.gradientStops;
                                                   return stops?.length && stops;
                                               }, "Could not get scatter plot gradient stops");
        await waitFor(() => gradientStops[0].color === ChartColorUtilities.getDefaultColorById("green").hex && gradientStops[1].color === ChartColorUtilities.getDefaultColorById("red").hex,
                      "Did not have the expected colors: green, red");

        await this.selectTimeSeriesContainerButton(timeSeriesContainer.test_colors, "configure color button");

        await this.m_overlayDriver.waitForOpen(scatterContainer.editColorConfig.optio3TestId);
        const gradientSelect = await waitFor(() => scatterContainer.test_colorConfig?.test_gradient?.test_gradients, "could not get gradient select");
        const gradientPreset = "Purples";
        await this.m_selectionDriver.makeSelection("gradient select", gradientSelect, [gradientPreset]);
        await this.m_formDriver.submitOverlayForm(scatterContainer.colorConfigurerContainer, "Save");

        const purplePreset = ChartColorUtilities.gradientPresets[gradientPreset];
        scatterPanels      = await waitFor(() => scatterContainer.scatterPlot?.test_panels, "could not get scatter chart computed panels");
        await waitFor(() =>
                      {
                          let stops = scatterPanels[0].rawPanel.gradientStops;
                          return stops[0].color === purplePreset.colors[0] && stops[1].color === purplePreset.colors[1];
                      }, "Did not have the expected colors: light purple, darker purple");
    }
}

@TestCase({
              id        : "dataExplorer_assetGraphScatter",
              name      : "Asset Graph Scatter Plot test",
              categories: ["Data Explorer"]
          })
class AssetGraphScatterPlotTest extends DataExplorerTest
{
    public async init(): Promise<void>
    {
        await this.ensureLoggedIn();
        await this.ensureVavDatGraph();
        await super.init();
    }

    public async execute(): Promise<void>
    {
        const timeSeriesContainer = await this.createNewChart(
            async (wizard) =>
            {
                await this.m_wizardDriver.selectDataSourceType(wizard, Models.TimeSeriesChartType.GRAPH_SCATTER);

                const graphsStep = await this.m_wizardDriver.getStep(wizard, DataSourceWizardGraphsStepComponent);
                await this.m_assetGraphDriver.addImportAssetGraphFlow(graphsStep, AssetGraphTest.vavDatGraphName, true, true);
                await this.m_driver.click(graphsStep.test_localGraphs.test_configureGraphs.first, "Configure asset graph button");

                await this.m_assetGraphDriver.assetStructureWizardFlow("", async (assetGraphStep) =>
                {
                    const graphEditor = await waitFor(() => assetGraphStep.test_graphEditor,
                                                      "could not get asset graph editor");
                    await this.m_assetGraphDriver.addAssetGraphNode(graphEditor, graphEditor.nodes[0].id, "CO2", ConditionNodeType.POINT, false, co2OptionLabel);
                }, true);

                await this.m_wizardDriver.stepNTimes(wizard, 1);

                const tupleStep  = await this.m_wizardDriver.getStep(wizard, DataSourceWizardSourceTuplesStepComponent);
                let tupleOptions = await waitFor(() => tupleStep.graphOptions, "Could not get asset graph tuple options");
                const ids        = [];
                while (tupleOptions[0].children.length)
                {
                    ids.push(tupleOptions[0].id);
                    tupleOptions = tupleOptions[0].children;
                }
                await this.m_selectionDriver.makeSelection("asset graph x-tuple selection", tupleStep.test_xGraphs.first, ids.concat(tupleOptions[0].id));
                await this.m_selectionDriver.makeSelection("asset graph y-tuple selection", tupleStep.test_yGraphs.first, ids.concat(tupleOptions[1].id));
                await waitFor(() => tupleStep.isValid(), "Tuple step should be valid");
            });

        let color = ChartColorUtilities.nextBestColor([]);
        await waitFor(() => timeSeriesContainer.scatterElement?.scatterPlot.test_panels[0]?.sourceTuples?.[0]?.sourceTuple.color === color,
                      `tuple color is not the default first color that was expected (${color})`);

        const consolidated = timeSeriesContainer.test_consolidated;
        const tupleChip    = await waitFor(() => consolidated.sourceChips.first?.element?.nativeElement, "Could not get source chip");
        await waitFor(() => !tupleChip.querySelector(".custom-action"), "There should be no custom actions present");
    }
}

@NgModule({imports: []})
export class DataExplorerTestsModule {}
