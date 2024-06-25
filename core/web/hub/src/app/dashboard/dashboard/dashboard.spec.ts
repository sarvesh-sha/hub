import {ComponentType} from "@angular/cdk/portal";
import {ElementRef, NgModule, Type} from "@angular/core";
import {UUID} from "angular2-uuid";

import {AssetStructureWizardDataStepComponent} from "app/customer/configuration/asset-structures/wizard/asset-structure-wizard-data-step.component";
import {AssetStructureWizardDialogComponent} from "app/customer/configuration/asset-structures/wizard/asset-structure-wizard-dialog.component";
import {UserListPageComponent} from "app/customer/configuration/users/user-list-page.component";
import {DashboardPageComponent} from "app/dashboard/dashboard/dashboard-page.component";
import {DashboardToolbarComponent} from "app/dashboard/dashboard/dashboard-toolbar.component";
import {WidgetBaseComponent} from "app/dashboard/dashboard/widgets/widget-base.component";
import {WidgetContainerComponent} from "app/dashboard/dashboard/widgets/widget-container.component";
import {WidgetManipulator} from "app/dashboard/dashboard/widgets/widget-manipulator";
import {WidgetEditorWizardAggregationNodeConfigurerComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-aggregation-node-configurer.component";
import {WidgetEditorWizardAggregationRangeStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-aggregation-range-step.component";
import {WidgetEditorWizardAlertSeverityStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-alert-severity-step.component";
import {WidgetEditorWizardAlertTableDisplayStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-alert-table-display-step.component";
import {WidgetEditorWizardAlertTypeStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-alert-type-step.component";
import {WidgetEditorWizardControlPointGroupingStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-control-point-grouping-step.component";
import {WidgetEditorWizardControlPointStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-control-point-step.component";
import {WidgetEditorWizardDataAggregationTypeStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-data-aggregation-type-step.component";
import {WidgetEditorWizardDialogComponent, WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import {WidgetEditorWizardGraphSelectorStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-graph-selector-step.component";
import {WidgetEditorWizardGraphsStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-graphs-step.component";
import {WidgetEditorWizardImageStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-image-step.component";
import {WidgetEditorWizardNameDescriptionStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-name-description-step.component";
import {WidgetEditorWizardPinConfigStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-pin-config-step.component";
import {WidgetEditorWizardTextStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-text-step.component";
import {WidgetEditorWizardTimeSeriesGraphsComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-time-series-graphs-step.component";
import {WidgetEditorWizardTimeSeriesStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-time-series-step.component";
import {WidgetEditorWizardTypeStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-type-step.component";
import {WidgetGraph} from "app/services/domain/dashboard-management.service";
import {WidgetConfigurationExtended} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {AggregationTableComponent, AggregationTableRow} from "app/shared/aggregation/aggregation-table.component";
import {DataAggregationType} from "app/shared/aggregation/data-aggregation.component";
import {ControlPointGroupingStepComponent} from "app/shared/assets/control-point-grouping-step/control-point-grouping-step.component";
import {ConditionNodeType} from "app/shared/assets/tag-condition-builder/tag-conditions";
import {DataSourceWizardDialogComponent, DataSourceWizardState} from "app/shared/charting/data-source-wizard/data-source-wizard-dialog.component";
import {DataSourceWizardGraphBindingsStepComponent} from "app/shared/charting/data-source-wizard/data-source-wizard-graph-bindings-step.component";
import {DataSourceWizardGraphsStepComponent} from "app/shared/charting/data-source-wizard/data-source-wizard-graphs-step.component";
import {ColorPickerGradientStopComponent} from "app/shared/colors/color-picker-gradient-stop.component";
import {AssetGraphTest} from "app/test/base-tests";
import {areEqual, assertIsDefined, assertTrue, getCenterPoint, TestCase, waitFor} from "app/test/driver";
import {ColorsDriver} from "app/test/drivers/colors-driver";
import {ConfirmationDriver} from "app/test/drivers/confirmation-driver";
import {DatatableDriver} from "app/test/drivers/datatable-driver";
import {airflowHeatingSpOptionLabel, datOptionLabel, DemoDataDriver} from "app/test/drivers/demo-data-driver";
import {DragScrollerDriver} from "app/test/drivers/drag-scroller-driver.service";
import {FormDriver} from "app/test/drivers/form-driver";
import {OverlayDriver} from "app/test/drivers/overlay-driver";
import {SelectionDriver} from "app/test/drivers/selection-driver";
import {TabGroupDriver} from "app/test/drivers/tab-group-driver";
import {ToastDriver} from "app/test/drivers/toast-driver";
import {WizardDriver} from "app/test/drivers/wizard-driver";
import {TestDataGenerator} from "app/test/test-data-generator";

import {UtilsService} from "framework/services/utils.service";
import {ChartColorUtilities} from "framework/ui/charting/core/colors";
import {TimeRange, TimeRanges} from "framework/ui/charting/core/time";
import {WizardComponent} from "framework/ui/wizards/wizard.component";
import {AggregationTableWidgetComponent} from "./widgets/aggregation-table-widget/widget.component";
import {AggregationTrendWidgetComponent} from "./widgets/aggregation-trend-widget/widget.component";
import {AggregationWidgetComponent} from "./widgets/aggregation-widget/widget.component";
import {AlertFeedWidgetComponent} from "./widgets/alert-feed-widget/widget.component";
import {AlertMapWidgetComponent} from "./widgets/alert-map-widget/widget.component";
import {AlertSummaryWidgetComponent} from "./widgets/alert-summary-widget/widget.component";
import {AlertTableWidgetComponent} from "./widgets/alert-table-widget/widget.component";
import {AssetGraphSelectorWidgetComponent} from "./widgets/asset-graph-selector-widget/widget.component";
import {ControlPointWidgetComponent} from "./widgets/control-point-widget/widget.component";
import {GroupingWidgetComponent} from "./widgets/grouping-widget/widget.component";
import {ImageWidgetComponent} from "./widgets/image-widget/widget.component";
import {TimeSeriesWidgetComponent} from "./widgets/time-series-widget/widget.component";

abstract class DashboardTest extends AssetGraphTest
{
    private m_onDashboard = false;
    protected m_page: DashboardPageComponent;
    protected m_toolbar: DashboardToolbarComponent;

    protected m_initialDashboardId: string;
    protected m_newDashboardId: string;

    private m_confirmationDriver: ConfirmationDriver   = this.m_driver.getDriver(ConfirmationDriver);
    private m_toastDriver: ToastDriver                 = this.m_driver.getDriver(ToastDriver);
    protected m_demoDataDriver: DemoDataDriver         = this.m_driver.getDriver(DemoDataDriver);
    protected m_colorsDriver: ColorsDriver             = this.m_driver.getDriver(ColorsDriver);
    protected m_datatableDriver: DatatableDriver       = this.m_driver.getDriver(DatatableDriver);
    protected m_formDriver: FormDriver                 = this.m_driver.getDriver(FormDriver);
    protected m_dragScrollerDriver: DragScrollerDriver = this.m_driver.getDriver(DragScrollerDriver);
    protected m_overlayDriver: OverlayDriver           = this.m_driver.getDriver(OverlayDriver);
    protected m_selectionDriver: SelectionDriver       = this.m_driver.getDriver(SelectionDriver);
    protected m_wizardDriver: WizardDriver             = this.m_driver.getDriver(WizardDriver);

    async init(): Promise<void>
    {
        await super.init();
        await this.initializeDashboard();

        if (!this.m_newDashboardId)
        {
            this.m_initialDashboardId = this.m_page.cfg.dashboardId;
            await this.makeToolbarMenuSelection("New");
            await waitFor(() => !UtilsService.equivalentStrings(this.m_initialDashboardId, this.m_page.cfg.dashboardId), "Failed to create new dashboard");

            this.m_newDashboardId = this.m_page.cfg.dashboardId;
        }
    }

    public async cleanup(): Promise<void>
    {
        if (this.m_newDashboardId)
        {
            await this.ensureParticularDashboard(this.m_newDashboardId, true);
            await waitFor(() => this.m_toolbar.test_menuTrigger, "Could not get toolbar's menu trigger");
            await this.m_selectionDriver.selectMenuOption("dashboard toolbar", this.m_toolbar.test_menuTrigger, "Delete");
            await this.m_confirmationDriver.handleConfirmationDialog();
            await this.ensureParticularDashboard(this.m_initialDashboardId);
        }
    }

    protected async initializeDashboard(): Promise<void>
    {
        this.m_page = await this.performNavigation(DashboardPageComponent, "/home");

        const standardLayout = await this.m_driver.getStandardLayoutComponent();
        await waitFor(() => this.m_page.cfg, "config never got set");
        this.m_toolbar = await waitFor(() => standardLayout.topnavComponentRef.instance instanceof DashboardToolbarComponent &&
                                             standardLayout.topnavComponentRef.instance, "Failed to get dashboard toolbar");
    }

    protected performNavigation<T>(type: ComponentType<T>,
                                   url: string): Promise<T>
    {
        this.m_onDashboard = url === "/home";
        return this.m_driver.navigate(type, url);
    }

    //--//

    protected async makeWidgetTypeSelection(wizard: WizardComponent<WidgetEditorWizardState>,
                                            type: Type<Models.WidgetConfiguration>): Promise<void>
    {
        const typeStep = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardTypeStepComponent);
        const typeName = WidgetConfigurationExtended.fromConfigModel(new type())
                                                    .getDescriptor().config.typeName;
        await this.m_wizardDriver.waitForWizard(wizard);
        if (typeStep.typeSelection != typeName)
        {
            await waitFor(() => typeStep.test_typeSelector, "Could not grab type selector");
            await this.m_selectionDriver.makeSelection("widget type selection", typeStep.test_typeSelector, [typeName]);
        }
    }

    protected async ensureParticularDashboard(dashboardId: string,
                                              force?: boolean): Promise<void>
    {
        try
        {
            if (!this.m_onDashboard || force) await this.initializeDashboard();

            await waitFor(() => this.m_page?.cfg.dashboardId === dashboardId, "", 500);
        }
        catch (e)
        {
            await this.m_selectionDriver.makeSelection("dashboard selection", this.m_toolbar.test_dashboardSelect, [dashboardId]);
            await waitFor(() => this.m_page.cfg.dashboardId === dashboardId, "Failed to reach particular dashboard");
        }
    }

    protected async navigateAwayAndBack(): Promise<Models.WidgetComposition[]>
    {
        let previousConfigs = this.m_page.cfg.model.widgets.map((cfg) => Models.WidgetComposition.deepClone(cfg));

        await this.performNavigation(UserListPageComponent, "/configuration/users");

        await this.initializeDashboard();
        await this.ensureParticularDashboard(this.m_newDashboardId);

        return previousConfigs;
    }

    protected async makeToolbarMenuSelection(option: string): Promise<void>
    {
        await waitFor(() => this.m_toolbar.test_menuTrigger, "Could not get toolbar's menu trigger");
        await this.m_selectionDriver.selectMenuOption("dashboard toolbar", this.m_toolbar.test_menuTrigger, option);
    }

    protected async getWidgetContainer(name: string): Promise<WidgetContainerComponent>
    {
        await waitFor(() => this.m_page.versionStateResolved, "Dashboard page did not stop loading");
        const widgetContainer = await waitFor(() => this.m_page.widgetManipulator.widgetContainers.find((widgetContainer) => widgetContainer.widget?.config.name === name),
                                              "Could not find widget");
        await this.m_driver.hover(widgetContainer.element, "widget");
        await waitFor(() => !widgetContainer.widget.isLoading, "Widget did not stop loading");
        return widgetContainer;
    }

    protected async ensureFocused(widget: WidgetContainerComponent | WidgetBaseComponent<any, any>): Promise<void>
    {
        assertIsDefined(widget, "widget component");
        if (!widget.focus) await this.m_driver.click(widget.element, "widget to focus");
    }

    protected async ensureNoFocus(): Promise<void>
    {
        if (this.m_page.widgetManipulator.focusId)
        {
            const firstWidget = this.m_page.cfg.model.widgets[0];
            assertIsDefined(firstWidget, "first widget");
            const firstWidgetContainer = await this.getWidgetContainer(firstWidget.config.name);
            const firstWidgetRect      = firstWidgetContainer.element.nativeElement.getBoundingClientRect();
            const outsideFirstWidget   = {
                x: firstWidgetRect.right + 2,
                y: (firstWidgetRect.bottom + firstWidgetRect.top) / 2 - 10
            };
            await this.m_driver.clickPoint(outsideFirstWidget, "to the right of the first widget");

            await waitFor(() => !this.m_page.widgetManipulator.focusId, "there should be no widget focused", 500, 1);
        }
    }

    protected async clearWidgets(): Promise<void>
    {
        await waitFor(() => this.m_page.editing, "Cannot clear widgets if not in edit mode");
        await this.m_toastDriver.clearAll();
        await this.m_selectionDriver.selectMenuOption("edit bar", this.m_page.test_editBarMenu, "Clear Widgets");
        await waitFor(() => this.m_page.cfg.model.widgets.length === 0, "Did not properly clear widgets");
        assertTrue(this.m_page.cfg.model.sharedSelectors.length === 0, "Did not properly clear selectors");
    }

    //--//

    protected async getWidgetWizard(): Promise<WizardComponent<WidgetEditorWizardState>>
    {
        return this.m_wizardDriver.getWizard(WidgetEditorWizardDialogComponent);
    }

    protected async enableDashboardEdits(): Promise<void>
    {
        await waitFor(() => !this.m_page.editing, "Cannot enable edit mode if already in edit mode");
        await this.makeToolbarMenuSelection("Edit");
        await waitFor(() => this.m_page.editing, "Enable edit failed");
    }

    protected async saveDashboardChanges(): Promise<void>
    {
        const saveRef    = await waitFor(() => this.m_page.editing && this.m_page.test_save, "Cannot save if not in edit mode");
        const saveButton = <HTMLElement>saveRef.nativeElement;
        await this.m_toastDriver.clearAll();

        await waitFor(() => this.m_page.versionStateResolved && !saveButton.classList.contains("mat-button-disabled"), "cannot save if disabled");
        await this.m_driver.click(this.m_page.test_save, "save dashboard button");
        await waitFor(() => !this.m_page.editing && this.m_page.versionStateResolved, "Failed to save");
    }

    protected async cancelDashboardChanges(manipulator?: WidgetManipulator,
                                           previousWidgets?: Models.WidgetComposition[]): Promise<void>
    {
        await waitFor(() => this.m_page.editing && this.m_page.test_cancel && this.m_page.versionStateResolved, "Cannot cancel edits if not in edit mode");
        await this.m_toastDriver.clearAll();
        await this.m_driver.click(this.m_page.test_cancel, "cancel dashboard button");
        await waitFor(() => !this.m_page.editing, "Did not successfully exit edit mode upon cancelling edits");
        if (manipulator && previousWidgets)
        {
            await waitFor(() => manipulator.widgetContainers.every((container,
                                                                    idx) => UtilsService.compareJson(container.outline.model, previousWidgets[idx].outline)),
                          "Some of the widget outlines did not revert correctly");
        }
    }

    protected async ensureAssetGraph(): Promise<void>
    {
        if (await this.ensureAhuVavDatGraph())
        {
            await this.initializeDashboard();
        }
    }
}

@TestCase({
              id        : "dashboard_clearWidgets",
              name      : "Clear widgets",
              timeout   : 50,
              categories: ["Dashboard"]
          })
class ClearWidgetsTest extends DashboardTest
{
    public async execute(): Promise<void>
    {
        await this.enableDashboardEdits();
        await this.clearWidgets();
        await this.saveDashboardChanges();
        await this.navigateAwayAndBack();
        await waitFor(() => this.m_page.cfg.model.widgets.length === 0, "Clear widgets did not persist");
    }
}

@TestCase({
              id        : "dashboard_removeWidget",
              name      : "Remove widget",
              categories: ["Dashboard"],
              timeout   : 40
          })
class DeleteWidgetTest extends DashboardTest
{
    public async execute(): Promise<void>
    {
        let startCfg        = Models.DashboardConfiguration.deepClone(this.m_page.cfg.model);
        let removableWidget = await waitFor(() => this.m_page.widgetManipulator.widgetContainers.find((widgetContainer) => widgetContainer.widget?.removable),
                                            "Could not find a widget to remove");
        await this.m_selectionDriver.selectMenuOption("widget container", removableWidget.test_menuTrigger, "Cut");

        await waitFor(() => this.m_page.cfg.model.widgets.length === startCfg.widgets.length - 1, "Widget removal failed");
        await this.saveDashboardChanges();
        await this.navigateAwayAndBack();
        await waitFor(() => this.m_page.cfg.model.widgets.length === startCfg.widgets.length - 1, "Widget removal did not persist");
    }
}

@TestCase({
              id        : "dashboard_manipulateWidgets",
              name      : "Widget manipulation",
              timeout   : 60,
              categories: ["Dashboard"]
          })
class ManipulateWidgetsTest extends DashboardTest
{
    public async execute(): Promise<void>
    {
        let manipulator: WidgetManipulator;
        let topContainers = await waitFor(() =>
                                          {
                                              manipulator       = this.m_page.widgetManipulator;
                                              let topContainers = manipulator.widgetContainers.filter((container) => container.outline.top === 0);
                                              return topContainers.length ? topContainers : null;
                                          }, "Could not find the top widgets");

        let mostLeft: WidgetContainerComponent;
        let mostRight: WidgetContainerComponent;
        for (let container of topContainers)
        {
            if (!mostLeft || container.outline.left < mostLeft.outline.left) mostLeft = container;
            if (!mostRight || container.outline.left > mostRight.outline.left) mostRight = container;
        }

        await this.enableDashboardEdits();

        let startLeft  = Models.WidgetOutline.deepClone(mostLeft.outline.model);
        let startRight = Models.WidgetOutline.deepClone(mostRight.outline.model);

        await this.m_driver.hover(mostLeft.element, "top left widget");

        await this.m_driver.clickAndDrag(mostLeft.element.nativeElement, {
            x: getCenterPoint(mostRight.element.nativeElement).x,
            y: getCenterPoint(mostLeft.element.nativeElement).y + manipulator.rowHeight
        }, "top left widget");
        await waitFor(() => mostLeft.outline.left === startRight.left && mostLeft.outline.top === startLeft.top + 1 &&
                            mostRight.outline.top === mostLeft.outline.top + mostLeft.outline.height && mostRight.outline.left === startRight.left,
                      "Widget drag didn't work as expected");

        await this.saveDashboardChanges();
        const previousWidgets = await this.navigateAwayAndBack();

        await waitFor(() =>
                      {
                          manipulator = this.m_page.widgetManipulator;
                          return manipulator.widgetContainers.every((container,
                                                                     idx) => UtilsService.compareJson(container.outline.model, previousWidgets[idx].outline));
                      }, "Some of the widget outlines changed");

        mostLeft  = await waitFor(() => manipulator.widgetContainers.find((widget) => widget.widget.id === mostLeft.widget.id), "Could not find original left widget");
        mostRight = await waitFor(() => manipulator.widgetContainers.find((widget) => widget.widget.id === mostRight.widget.id), "Could not find original right widget");

        await this.enableDashboardEdits();
        await this.resizeTopLeft(manipulator, mostRight, 1, 1);
        await waitFor(() => mostLeft.outline.top === 0 && mostLeft.outline.height === startLeft.height, "Resize failed to moved widgets out of the way");

        await this.resizeTopLeft(manipulator, mostRight, 1, 1);
        await waitFor(() => mostLeft.outline.top === 0 && mostLeft.outline.height === startLeft.height - 1, "Resize failed to resize nearby widgets");

        await this.resizeTopLeft(manipulator, mostRight, 0, 3);
        await this.resizeTopLeft(manipulator, mostRight, 0, 1, true);
        await waitFor(() => mostLeft.outline.top === 0 && mostLeft.outline.height === 1, "This resize shouldn't have done anything");

        await this.cancelDashboardChanges(manipulator, previousWidgets);
    }

    private async resizeTopLeft(manipulator: WidgetManipulator,
                                widgetContainer: WidgetContainerComponent,
                                dx: number,
                                dy: number,
                                skipResizeCheck?: boolean): Promise<void>
    {
        const resizeGrip       = await waitFor(() => widgetContainer.element.nativeElement.parentElement.querySelector(".top.left .resize-grip"), "Could not grab top left resize gripper");
        let topLeftResizePoint = getCenterPoint(resizeGrip);
        let startOutline       = Models.WidgetOutline.deepClone(widgetContainer.outline.model);
        await this.m_driver.clickAndDrag(topLeftResizePoint, {
            x: topLeftResizePoint.x - dx * manipulator.colWidth,
            y: topLeftResizePoint.y - dy * manipulator.rowHeight
        }, "top left resize point");

        if (!skipResizeCheck)
        {
            await waitFor(() => widgetContainer.outline.width - dx === startOutline.width && widgetContainer.outline.height - dy === startOutline.height,
                          `Widget resize didn't work correctly: should have changed width and height by ${dx} and ${dy} respectively`);
        }
    }
}

@TestCase({
              id        : "dashboard_undoRedoVersions",
              name      : "Undo/redo, versions",
              timeout   : 90,
              categories: ["Dashboard"]
          })
class UndoRedoVersionTest extends DashboardTest
{
    private async undoViaButton(): Promise<void>
    {
        const undoButton = await waitFor(() => this.m_page.test_undoRedo?.test_undo, "Unable to find undo button");
        await this.m_driver.click(undoButton, "undo button");
    }

    public async execute(): Promise<void>
    {
        await this.enableDashboardEdits();
        await this.clearWidgets();

        await this.undoViaButton();
        await waitFor(() => this.m_page.cfg.model.widgets.length > 0, "Undo didn't work");

        await waitFor(() => this.m_page.versionManager?.stateHistory?.canRedo(), "Was not ready to redo", 250);
        await this.m_driver.sendKeys(["z"],
                                     [
                                         "Shift",
                                         "MetaLeft"
                                     ]);
        await waitFor(() => this.m_page.versionStateResolved && this.m_page.cfg.model.widgets.length === 0, "Redo didn't trigger/work");

        const titleSuffix      = " with no widgets";
        const newTitle         = this.m_page.cfg.model.title + titleSuffix;
        let dashboardNameInput = await waitFor(() => this.m_page.test_nameInput, "Could not grab dashboard name input");
        await this.m_driver.sendText(dashboardNameInput, "dashboard name", titleSuffix);
        dashboardNameInput.nativeElement.blur();
        await waitFor(() => this.m_page.versionStateResolved && this.m_page.cfg.model.title === newTitle, "Failed to change dashboard name");

        await waitFor(() => this.m_page.versionStateResolved && this.m_page.versionManager?.stateHistory?.canUndo(), "Was not ready for undo");
        await this.m_driver.sendKeys(["z"], ["MetaRight"]);
        await waitFor(() => this.m_page.versionStateResolved && this.m_page.cfg.model.widgets.length === 0 && this.m_page.cfg.model.title !== newTitle, "Undo didn't trigger/work");

        await waitFor(() => this.m_page.versionManager?.stateHistory?.canRedo(), "Was not ready to redo 2");
        await this.m_driver.sendKeys(["y"], ["ControlLeft"]);
        await waitFor(() => this.m_page.versionStateResolved && this.m_page.cfg.model.widgets.length === 0 && this.m_page.cfg.model.title === newTitle, "Redo 2 didn't trigger/work");

        await this.saveDashboardChanges();
        await this.navigateAwayAndBack();
        await waitFor(() => this.m_page.versionStateResolved && this.m_page.cfg.model.widgets.length === 0 && this.m_page.cfg.model.title === newTitle, "Changes did not persist");

        await this.enableDashboardEdits();
        await this.undoViaButton();
        await waitFor(() => this.m_page.versionStateResolved && this.m_page.cfg.model.widgets.length > 0 && this.m_page.cfg.model.title !== newTitle, "Changing version did not work");
        const previousVersionId = this.m_page.activeVersionId;

        await this.navigateAwayAndBack();
        await waitFor(() => this.m_page.cfg.model.widgets.length === 0 && this.m_page.cfg.model.title === newTitle, "Changes did not persist in way expected");

        await this.enableDashboardEdits();
        await waitFor(() => this.m_page.test_versionSelect, "Could not grab version select component");
        await this.m_selectionDriver.makeSelection("dashboard version", this.m_page.test_versionSelect, [previousVersionId]);
        await waitFor(() => this.m_page.versionStateResolved && this.m_page.cfg.model.widgets.length > 0 && this.m_page.cfg.model.title !== newTitle, "Did not change version correctly");
        await this.saveDashboardChanges();

        await this.navigateAwayAndBack();
        await waitFor(() => this.m_page.cfg.model.widgets.length > 0 && this.m_page.cfg.model.title !== newTitle, "Version change did not persist");
    }
}

abstract class WidgetTest<T extends Models.WidgetConfiguration> extends DashboardTest
{
    protected abstract readonly m_widgetType: Type<T>;
    protected m_newWidgetName: string;

    protected m_numAhus: number;

    public async init(): Promise<void>
    {
        await super.init();

        await waitFor(async () => this.m_numAhus = await this.m_demoDataDriver.getNumAhus(), "Must have at least one AHU for this test to work: " + this.m_numAhus);
    }

    protected async getNewWidgetContainer(): Promise<WidgetContainerComponent>
    {
        return this.getWidgetContainer(this.m_newWidgetName);
    }

    protected async getWidgetMenuTrigger(name: string = this.m_newWidgetName): Promise<ElementRef<any>>
    {
        const widgetContainer = await this.getWidgetContainer(name);
        if (!widgetContainer.alwaysShowToolbar)
        {
            await this.m_driver.hover(widgetContainer.element, `widget container of '${name}'`);
            await waitFor(() => widgetContainer.test_menuTrigger, "toolbar did not appear");
        }

        return widgetContainer.test_menuTrigger;
    }

    protected async createNewWidget(widgetConfigureFn: (wizard: WizardComponent<WidgetEditorWizardState>) => Promise<void>,
                                    widgetType?: Type<Models.WidgetConfiguration>,
                                    doNotSave?: boolean,
                                    expectedNumNewWidgets: number = 1): Promise<string>
    {
        await waitFor(() => this.m_page.cfg, "page did not load");
        const initialWidgetCount = this.m_page.cfg.model.widgets.length;

        if (this.m_page.editing)
        {
            await this.m_driver.click(this.m_page.test_addWidget, "add widget button");
        }
        else
        {
            await this.makeToolbarMenuSelection("Add Widget");
        }
        const wizard = await this.getWidgetWizard();
        await this.makeWidgetTypeSelection(wizard, widgetType || this.m_widgetType);

        await widgetConfigureFn(wizard);

        const nameStep = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardNameDescriptionStepComponent);
        await waitFor(() => nameStep.test_nameInput, "Cannot grab widget name input");

        let newName = UUID.UUID();
        await this.m_driver.sendText(nameStep.test_nameInput, "widget name", newName);
        if (!widgetType) this.m_newWidgetName = newName;

        await this.m_wizardDriver.save(wizard, "Unable to save changes and create new widget");
        if (!doNotSave) await this.saveDashboardChanges();
        await waitFor(() => this.m_page.cfg?.model.widgets.length - expectedNumNewWidgets === initialWidgetCount,
                      `Failed to create new ${UtilsService.pluralize("widget", expectedNumNewWidgets)}`);

        return newName;
    }

    protected async editNewWidget(widgetConfigureFn: (wizard: WizardComponent<WidgetEditorWizardState>) => Promise<void>): Promise<void>
    {
        const widgetMenuTrigger = await this.getWidgetMenuTrigger();
        await this.m_selectionDriver.selectMenuOption("widget container", widgetMenuTrigger, "Settings");
        const wizard = await this.getWidgetWizard();

        await widgetConfigureFn(wizard);

        await this.m_wizardDriver.save(wizard, "Unable to save widget edits");
    }

    protected async getInnerGroupingStep(wizard: WizardComponent<WidgetEditorWizardState>): Promise<ControlPointGroupingStepComponent>
    {
        const wizardStep        = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardControlPointGroupingStepComponent);
        const innerGroupingStep = await waitFor(() => wizardStep.test_groupingStep, "Could not get underlying grouping component");
        await this.m_wizardDriver.waitForWizard(wizard);
        await waitFor(() => innerGroupingStep.test_configureSourceTriggers.length, "Inner grouping step didn't load in as expected");
        return innerGroupingStep;
    }

    //--//

    protected async getSelectorWidget(): Promise<WidgetContainerComponent>
    {
        const selectorWidget = await waitFor(() => this.m_page.widgetManipulator.widgetContainers.find(
                                                 (widgetContainer) => widgetContainer.widget?.config instanceof Models.AssetGraphSelectorWidgetConfiguration),
                                             "Could not find new selector widget");
        await this.m_driver.hover(selectorWidget.element, "widget");
        await waitFor(() => !selectorWidget.widget.isLoading, "Widget did not stop loading");
        return selectorWidget;
    }

    protected async selectNextContext(widget: AssetGraphSelectorWidgetComponent): Promise<string>
    {
        let id = widget.selectedAsset;
        if (widget.graphOptions.length > 1)
        {
            const currIdx    = widget.graphOptions.findIndex((option) => option.id === widget.selectedAsset);
            const nextOption = widget.graphOptions[(currIdx + 1) % widget.graphOptions.length];
            id               = nextOption.id;
            await this.m_selectionDriver.makeSelection("context select", widget.test_assets, [id]);
        }

        return id;
    }

    //--//

    protected async selectAlertType(wizard: WizardComponent<WidgetEditorWizardState>,
                                    type: Models.AlertType): Promise<void>
    {
        await this.m_wizardDriver.waitForWizard(wizard);
        const alertTypeStep = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardAlertTypeStepComponent);
        const alertOptions  = await waitFor(() => alertTypeStep.test_alerts?.nativeElement, "Could not get alert type control option list");
        const typeOption    = alertTypeStep.alertTypeOptions.find((option) => option.id === type);
        await this.m_datatableDriver.selectControlOption(alertOptions, typeOption.label);
    }

    protected async pickOnlyAvailableBinding(bindingsStep: DataSourceWizardGraphBindingsStepComponent,
                                             local: boolean): Promise<void>
    {
        const graphSelect = await this.m_driver.getComponentValue(bindingsStep,
                                                                  (bindingsStep) =>
                                                                  {
                                                                      let binding = local ? bindingsStep.test_bindingLocal : bindingsStep.test_bindingExternal;
                                                                      if (binding?.options?.length) return binding;
                                                                      return null;
                                                                  }, "graph select");

        await this.m_selectionDriver.makeOnlyAvailableSelection("asset graph binding", graphSelect, false);
    }

    protected async createAggregationSummaryWidget(dataWizardFn: (wizard: WizardComponent<DataSourceWizardState>) => Promise<void>,
                                                   usingSelectorWidget: boolean): Promise<string>
    {
        return this.createNewWidget(async (wizard) =>
                                    {
                                        await this.m_wizardDriver.stepNTimes(wizard, 1);

                                        const innerGroupingStep = await this.getInnerGroupingStep(wizard);
                                        await this.m_driver.click(innerGroupingStep.test_configureSourceTriggers.first, "group source configure button");

                                        const dataSourceWizard = await this.m_wizardDriver.getWizard<DataSourceWizardState, DataSourceWizardDialogComponent>(DataSourceWizardDialogComponent);
                                        await dataWizardFn(dataSourceWizard);
                                        await this.m_wizardDriver.save(dataSourceWizard, "Failed to save aggregation summary data source information");
                                        await waitFor(() => !innerGroupingStep.sourcesUpdating, "Sources never completed updating");

                                        const configureGroupTrigger = await waitFor(() => innerGroupingStep.test_configureGroupTriggers?.first, "Failed to grab group's config trigger button");
                                        await this.m_driver.click(configureGroupTrigger, "configure group trigger");
                                        const groupConfigurer = await waitFor(() => innerGroupingStep?.groupConfigurer, "Failed to grab Group Configurer Component");
                                        await this.m_overlayDriver.waitForOpen(groupConfigurer.overlayConfig.optio3TestId);
                                        await this.m_selectionDriver.makeMatSelection("group aggregation", groupConfigurer.test_groupAgg, "Max");
                                        await this.m_selectionDriver.makeMatSelection("control point aggregation", groupConfigurer.test_cpAgg, "Mean");
                                        await this.m_overlayDriver.closeOverlay(groupConfigurer.test_overlay);
                                        await this.m_wizardDriver.stepNTimes(wizard, 4);
                                    }, Models.AggregationWidgetConfiguration, false, usingSelectorWidget ? 2 : 1);
    }

    protected async verifyPastability(usingSelector: boolean,
                                      widgetName: string = this.m_newWidgetName)
    {
        let dashboardModel = this.m_page.cfg.model;
        const numWidgets   = dashboardModel.widgets.length;
        const numSelectors = dashboardModel.sharedSelectors.length;

        const widget = await this.getWidgetContainer(widgetName);
        await this.m_selectionDriver.selectMenuOption("widget container", widget.test_menuTrigger, "Copy");
        await this.m_driver.sendKeys(["v"], ["MetaLeft"]);
        await waitFor(() => this.m_page.editing && numWidgets + 1 === this.m_page.cfg.model.widgets.length,
                      "1st widget paste did not work correctly");

        await this.clearWidgets();
        await this.m_selectionDriver.selectMenuOption("dashboard edit bar", this.m_page.test_editBarMenu, "Paste Widget(s)");
        await waitFor(() =>
                      {
                          dashboardModel = this.m_page.cfg.model;
                          if (usingSelector)
                          {
                              return dashboardModel.widgets.length === 2 && dashboardModel.sharedSelectors.length === 1;
                          }
                          return dashboardModel.widgets.length === 1;
                      }, "2nd widget paste did not work correctly");
        await this.getWidgetContainer(widgetName);

        await this.m_driver.sendKeys(["z"], ["Control"]);
        await waitFor(() => this.m_page.versionStateResolved, "dashboard did not stabilize after paste undo");
        dashboardModel = this.m_page.cfg.model;
        assertTrue(dashboardModel.widgets.length === 0, "paste undo did not revert correctly");
        assertTrue(dashboardModel.sharedSelectors.length === 0, "paste undo did not revert shared selectors");

        const groupingName          = await this.createNewWidget((wizard) => this.m_wizardDriver.stepNTimes(wizard, 2),
                                                                 Models.GroupingWidgetConfiguration, true);
        let groupingWidgetContainer = await this.getWidgetContainer(groupingName);
        await this.m_driver.click(groupingWidgetContainer.element, "grouping widget");
        await this.m_driver.sendKeys(["v"], ["Control"]);
        await waitFor(() =>
                      {
                          let config = this.m_page.cfg.model.widgets[0].config;
                          if (config instanceof Models.GroupingWidgetConfiguration)
                          {
                              let expectedSubWidgetCt = usingSelector ? 2 : 1;
                              return config.widgets.length === expectedSubWidgetCt;
                          }
                          return false;
                      }, "Paste into grouping widget did not work");
        groupingWidgetContainer = await this.getWidgetContainer(groupingName);
        const groupingWidget    = await waitFor(() => groupingWidgetContainer.widget instanceof GroupingWidgetComponent && groupingWidgetContainer.widget,
                                                "could not get inner grouping widget");
        if (usingSelector)
        {
            const selectorWidgetContainer = await waitFor(() => groupingWidget.widgetManipulator.widgetContainers.find(
                                                              (widgetContainer) => widgetContainer.widget?.config instanceof Models.AssetGraphSelectorWidgetConfiguration),
                                                          "Could not find new selector widget");
            await waitFor(() => !selectorWidgetContainer.widget.isLoading, "Selector widget did not stop loading");

            const selectorWidget = await waitFor(() => selectorWidgetContainer.widget instanceof AssetGraphSelectorWidgetComponent && selectorWidgetContainer.widget,
                                                 "could not get inner selector sub-widget");
            assertIsDefined(selectorWidget.test_assets, "selector widget's select");
        }

        await this.cancelDashboardChanges();
        await waitFor(() => this.m_page.cfg.model.widgets.length === numWidgets, "widget pastes did not revert properly");
        assertTrue(this.m_page.cfg.model.sharedSelectors.length === numSelectors, "shared selectors did not revert properly");
    }
}

@TestCase({
              id        : "dashboard_widget_aggregationSummary",
              name      : "Aggregation Summary widget",
              timeout   : 100,
              categories: [
                  "Dashboard",
                  "Widgets"
              ]
          })
class AggregationSummaryWidgetTest extends WidgetTest<Models.AggregationWidgetConfiguration>
{
    protected readonly m_widgetType = Models.AggregationWidgetConfiguration;

    private m_numCps: number;

    private async getNewAggregationWidget(): Promise<AggregationWidgetComponent>
    {
        let widgetContainer = await this.getNewWidgetContainer();
        return waitFor(() => widgetContainer.widget instanceof AggregationWidgetComponent && widgetContainer.widget,
                       "Cannot find newly made aggregation summary");
    }

    public async execute(): Promise<void>
    {
        this.m_newWidgetName = await this.createAggregationSummaryWidget(
            async (dataSourceWizard) =>
            {
                await this.m_wizardDriver.stepNTimes(dataSourceWizard, 1);

                this.m_numCps = Math.min(this.m_numAhus * 6, 10);
                await this.m_wizardDriver.standardSelectControlPoints(dataSourceWizard, "discharge air temperature", this.m_numCps);
            }, false);
        await this.verifyNewWidget("Widget creation did not work as expected");

        await this.navigateAwayAndBack();
        await this.verifyNewWidget("Created widget's config did not persist correctly");

        const targetRange = TimeRanges.Last3Days;
        await this.editNewWidget(async (wizard) =>
                                 {
                                     await this.m_wizardDriver.stepNTimes(wizard, 1);
                                     const rangeStep = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardAggregationRangeStepComponent);
                                     await this.m_wizardDriver.waitForWizard(wizard);
                                     await this.m_driver.click(rangeStep.test_showRange, "show range toggle");
                                     let rangeSelector = await waitFor(() => rangeStep.test_rangeSelector?.test_rangeSelector, "Failed to grab pre-selected ranges select component");
                                     await this.m_selectionDriver.makeSelection("range selection", rangeSelector, [targetRange]);
                                 });

        await this.verifyNewWidget("Aggregation widget edit did not work correctly", true, targetRange);
        await this.saveDashboardChanges();

        await this.navigateAwayAndBack();
        await this.verifyNewWidget("Aggregation widget edits did not persist correctly", true, targetRange);
    }

    private async verifyNewWidget(errorMsg: string,
                                  hideRange?: boolean,
                                  expectedRange?: TimeRange): Promise<void>
    {
        expectedRange = expectedRange ?? TimeRanges.Last24Hours;
        hideRange     = hideRange ?? false;

        let aggWidgetConfig = (await this.getNewAggregationWidget()).config;
        await waitFor(() =>
                      {
                          if (aggWidgetConfig.controlPointGroup.groupAggregationType !== Models.AggregationTypeId.MAX) return false;
                          if (aggWidgetConfig.controlPointGroup.aggregationType !== Models.AggregationTypeId.MEAN) return false;
                          if (!aggWidgetConfig.hideRange !== !hideRange) return false;
                          if (aggWidgetConfig.filterableRange.range.range != <any>expectedRange.id) return false;
                          return aggWidgetConfig.controlPointGroup.selections.identities.length === this.m_numCps;
                      }, errorMsg);
    }
}

abstract class DataAggregationTest extends WidgetTest<Models.AggregationTableWidgetConfiguration>
{
    protected readonly m_widgetType = Models.AggregationTableWidgetConfiguration;

    protected async getNewDataAggregationWidget(): Promise<AggregationTableWidgetComponent>
    {
        let widgetContainer = await this.getNewWidgetContainer();
        return waitFor(() => widgetContainer.widget instanceof AggregationTableWidgetComponent && widgetContainer.widget,
                       "Cannot find newly made data aggregation widget");
    }
}

@TestCase({
              id        : "dashboard_widget_dataAggregation_groups",
              name      : "Data Aggregation widget - Groups",
              timeout   : 180,
              categories: [
                  "Dashboard",
                  "Widgets"
              ]
          })
export class DataAggregationGroupsTest extends DataAggregationTest
{
    protected readonly m_stopValue = 59 + Math.random() * 36;

    private m_tabGroupDriver: TabGroupDriver = this.m_driver.getDriver(TabGroupDriver);

    protected async buildBaseAggregationTable(wizard: WizardComponent<WidgetEditorWizardState>)
    {
        const innerGroupingStep = await this.getInnerGroupingStep(wizard);
        await this.m_driver.sendText(innerGroupingStep.test_groupNames.first, "group name", "Group 1");
        await this.m_driver.click(innerGroupingStep.test_configureSourceTriggers.first, "group source configure button");

        const dataSourceWizard = await this.m_wizardDriver.getWizard<DataSourceWizardState, DataSourceWizardDialogComponent>(DataSourceWizardDialogComponent);
        await this.m_wizardDriver.selectDataSourceType(dataSourceWizard, Models.TimeSeriesChartType.GRAPH);

        const graphsStep = await this.m_wizardDriver.getStep(dataSourceWizard, DataSourceWizardGraphsStepComponent);
        await this.m_assetGraphDriver.importAssetGraphFlow(graphsStep, AssetGraphTest.ahuVavDatGraphName, false);
        await this.m_wizardDriver.stepNTimes(dataSourceWizard, 1);
        await this.m_wizardDriver.waitForWizard(dataSourceWizard);

        const bindingsStep = await this.m_wizardDriver.getStep(dataSourceWizard, DataSourceWizardGraphBindingsStepComponent);
        await this.pickOnlyAvailableBinding(bindingsStep, true);
        await this.m_wizardDriver.save(dataSourceWizard, "Could not save hierarchically developed control point selection");
        await waitFor(() => !innerGroupingStep.sourcesUpdating, "Inner grouping step did not complete the source updating");

        await waitFor(() => innerGroupingStep.controlPointGroupExts?.[0].numControlPoints === this.m_numAhus * 5,
                      `Picked up ${innerGroupingStep.controlPointGroupExts?.[0].numControlPoints} control points instead of the expected ${this.m_numAhus * 5}. ` +
                      `Expectation comes from 5 VAVs per AHU with ${this.m_numAhus} AHUs found`);
        await this.m_driver.click(innerGroupingStep.test_configureGroupTriggers.first, "configure control point group");
        await this.m_overlayDriver.waitForOpen(innerGroupingStep.groupConfigurer.overlayConfig.optio3TestId);
        await this.m_selectionDriver.makeMatSelection("group agg", innerGroupingStep.groupConfigurer.test_groupAgg, "Mean");
    }

    public async init(): Promise<void>
    {
        await super.init();

        await this.ensureAssetGraph();
    }

    public async execute(): Promise<void>
    {
        await this.createNewWidget(async (wizard) =>
                                   {
                                       await this.m_wizardDriver.stepNTimes(wizard, 2);

                                       await this.buildBaseAggregationTable(wizard);

                                       await this.m_wizardDriver.stepNTimes(wizard, 4);
                                   });

        let aggTable               = await this.getNewDataAggregationWidget();
        const numRowsPerAhu        = 5 * 2 + 1; // 5 VAVs per AHU, 1 DAT per VAV, a row for each AHU, VAV, and DAT
        const expectedRowsPerGroup = this.m_numAhus * numRowsPerAhu + 1;
        await waitFor(() => aggTable.aggregationGroups.tableStructure?.rows.length === expectedRowsPerGroup * aggTable.config.groups.length,
                      "Did not get the expected number of results: " + aggTable.aggregationGroups.tableStructure?.rows.length);

        await this.editNewWidget(async (wizard) =>
                                 {
                                     const innerGroupingStep = await this.getInnerGroupingStep(wizard);
                                     await this.m_driver.click(innerGroupingStep.test_configureGroupTriggers.first, "configure control point group");
                                     await this.m_overlayDriver.waitForOpen(innerGroupingStep.groupConfigurer.overlayConfig.optio3TestId);
                                     const tabGroup = await waitFor(() => innerGroupingStep.groupConfigurer?.test_tabGroup, "Could not get group configurer's tab group");
                                     await this.m_tabGroupDriver.changeTab(tabGroup, "Display");
                                     const gradientStop = await waitFor(() => innerGroupingStep.groupConfigurer.test_gradientStop, "Could not get gradient stop");
                                     const firstChip    = await waitFor(() => gradientStop.test_stopChips?.first, "Could not get first gradient stop chip");
                                     await this.changeChipColor(gradientStop, firstChip, "red");
                                     await this.m_driver.click(gradientStop.test_splits.first, "split gradient stop");
                                     await waitFor(() => gradientStop.test_stopChips?.length === 3, "Chip split didn't work");
                                     await this.changeChipColor(gradientStop, gradientStop.test_stopChips.toArray()[1], "green", async () =>
                                     {
                                         const stopOption = gradientStop.editingStopModes.find((option) => option.id === Models.ColorStopPoint.CUSTOM);
                                         await this.m_selectionDriver.makeMatSelection("gradient stop mode selection", gradientStop.test_stopMode, stopOption.label);
                                         const stopValueElem = await waitFor(() => gradientStop.test_stopValue, "Could not find stop value input");
                                         await this.m_driver.sendText(stopValueElem, "custom stop value", `${this.m_stopValue}`, true);
                                     });
                                     await this.m_overlayDriver.closeOverlay(innerGroupingStep.groupConfigurer.test_overlay);

                                     await this.m_driver.click(innerGroupingStep.test_copyGroupTriggers.first, "copy control point group");
                                     await waitFor(() => !innerGroupingStep.sourcesUpdating, "sources did not stop updating");
                                     await this.m_driver.click(innerGroupingStep.test_configureGroupTriggers.first, "configure control point group");
                                     await this.m_overlayDriver.waitForOpen(innerGroupingStep.groupConfigurer.overlayConfig.optio3TestId);
                                     await this.m_selectionDriver.makeMatSelection("control point aggregation", innerGroupingStep.groupConfigurer.test_cpAgg, "Min");
                                 });
        await this.saveDashboardChanges();

        aggTable = await this.getNewDataAggregationWidget();
        await this.verifyEdits(aggTable.config, "Widget edits did not work");
        const redHex                = ChartColorUtilities.getDefaultColorById("red").hex;
        const greenHex              = ChartColorUtilities.getDefaultColorById("green").hex;
        const aggTableValueElements = <NodeListOf<HTMLElement>>aggTable.element.nativeElement.querySelectorAll("td > div.value > div");
        await waitFor(() =>
                      {
                          for (let valueElem of Array.from(aggTableValueElements))
                          {
                              let value = parseFloat(valueElem.innerText);
                              if (isNaN(value)) continue;

                              let cssColor = ChartColorUtilities.safeChroma(getComputedStyle(valueElem).color)
                                                                .hex();
                              if (value < this.m_stopValue)
                              {
                                  if (cssColor !== redHex) return false;
                              }
                              else
                              {
                                  if (cssColor !== greenHex) return false;
                              }
                          }
                          return true;
                      }, "Colors of the data aggregation table values are not correct", undefined, 1);

        await this.navigateAwayAndBack();
        aggTable = await this.getNewDataAggregationWidget();
        await this.verifyEdits(aggTable.config, "Widget edits did not persist");
    }

    protected async changeChipColor(gradientStop: ColorPickerGradientStopComponent,
                                    chip: ElementRef,
                                    colorId: string,
                                    extraChipManipulationFn?: () => Promise<void>): Promise<void>
    {
        await this.m_driver.click(chip, "gradient stop");
        await this.m_overlayDriver.waitForOpen(gradientStop.config.optio3TestId);
        const colorFlat = await waitFor(() => gradientStop.test_colorFlat, "Could not get color flat for changing chip color");
        await this.m_colorsDriver.selectColor(colorFlat, colorId);

        if (extraChipManipulationFn) await extraChipManipulationFn();

        await this.m_overlayDriver.closeOverlay(gradientStop.stopEditorOverlay);
    }

    private async verifyEdits(config: Models.AggregationTableWidgetConfiguration,
                              errorMsg: string): Promise<void>
    {
        await waitFor(() => config.groups.length === 2 &&
                            config.groups[0].aggregationType === Models.AggregationTypeId.MIN &&
                            config.groups[1].aggregationType === Models.AggregationTypeId.MAX, errorMsg);
    }
}

@TestCase({
              id        : "dashboard_widget_dataAggregation_bindings",
              name      : "Data Aggregation widget - Bindings",
              timeout   : 100,
              categories: [
                  "Dashboard",
                  "Widgets"
              ]
          })
export class DataAggregationBindingsTest extends DataAggregationTest
{
    public async execute(): Promise<void>
    {
        await this.createNewWidget(async (wizard) =>
                                   {
                                       await this.m_wizardDriver.stepNTimes(wizard, 1);
                                       let dataAggregationTypeStep = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardDataAggregationTypeStepComponent);
                                       let option                  = dataAggregationTypeStep.data.editor.dataAggregationExt.options.find((option) => option.id === DataAggregationType.Bindings);
                                       await this.m_datatableDriver.selectControlOption(dataAggregationTypeStep.test_types.nativeElement, option.label);

                                       await this.m_wizardDriver.stepNTimes(wizard, 1);
                                       let graphsStep = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardGraphsStepComponent);
                                       await this.m_assetGraphDriver.importAssetGraphFlow(graphsStep, AssetGraphTest.ahuVavDatGraphName, false);

                                       await this.m_wizardDriver.stepNTimes(wizard, 1);
                                       let aggNodeConfigurerStep = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardAggregationNodeConfigurerComponent);
                                       let nodeConfigurer        = await waitFor(() => aggNodeConfigurerStep.test_nodeBindingConfigurer, "could not get node configurer");
                                       const addBindingColumnBtn = await waitFor(() => nodeConfigurer.test_table.test_add, "could not get add binding column button");
                                       await this.m_driver.click(addBindingColumnBtn, "add binding column button");
                                       const assetGraphExtended = nodeConfigurer.graphExt;
                                       const datNode            = assetGraphExtended.model.nodes.find((node) => node.name === "DAT");
                                       const selectionHierarchy = assetGraphExtended.getAncestorHierarchy(datNode.id);
                                       await this.m_selectionDriver.makeSelection("agg node binding", nodeConfigurer.test_bindings.first, [datNode.id]);
                                       await waitFor(() => aggNodeConfigurerStep.isValid(), "node configurer should be valid after selecting your first binding");
                                       await this.m_driver.click(addBindingColumnBtn, "add binding column button");
                                       await waitFor(() => !aggNodeConfigurerStep.isValid(), "node configurer should be invalid after adding second column");
                                       await this.m_selectionDriver.makeSelection("agg node binding 2", nodeConfigurer.test_bindings.last, selectionHierarchy);
                                       await waitFor(() => nodeConfigurer.nodeExts[1].model.nodeId, "second binding should have a nodeId after the DAT selection");
                                       await waitFor(() => !aggNodeConfigurerStep.isValid(), "node configurer should be invalid after selecting second DAT column");
                                       const fieldElem = await this.m_formDriver.getFormFieldElement(nodeConfigurer.test_names.last, "last name");
                                       await waitFor(() => fieldElem.classList.contains("mat-form-field-invalid") && !aggNodeConfigurerStep.isValid(),
                                                     "names should be invalid because they're not unique");
                                       await this.m_driver.sendText(nodeConfigurer.test_names.first, "first node binding name input", " x");
                                       await waitFor(() => aggNodeConfigurerStep.isValid(), "node configurer should be valid after making the names different");

                                       await this.m_wizardDriver.stepNTimes(wizard, 4);
                                   });
        let widget = await this.getNewDataAggregationWidget();
        assertTrue(widget.config.filterableRanges[0].range.range === Models.TimeRangeId.Last24Hours, "range was not last 24 hours");
        await this.verifyItalics(widget, false);

        await this.editNewWidget(async (wizard) =>
                                 {
                                     const graphsStep        = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardGraphsStepComponent);
                                     const configureGraphBtn = await waitFor(() => graphsStep.test_localGraphs.test_configureGraphs.first, "could not get graph configure button");
                                     await this.m_driver.click(configureGraphBtn, "configure asset graph button");
                                     let ahuNodeId: string;
                                     await this.m_assetGraphDriver.assetStructureWizardFlow("", async (assetGraphStep) =>
                                     {
                                         const assetGraphEditor = await waitFor(() => assetGraphStep.test_graphEditor, "Could not get asset graph editor");
                                         const assetGraph       = await waitFor(() => assetGraphStep.graph?.nodes.length && assetGraphStep.graph.transforms.length && assetGraphStep.graph,
                                                                                "Could not get asset graph");
                                         ahuNodeId              = assetGraph.nodes.find((node) => node.name === "AHU").id;
                                         await this.m_assetGraphDriver.addAssetGraphNode(assetGraphEditor, ahuNodeId, "singular DAT", ConditionNodeType.POINT, false, datOptionLabel);
                                         await waitFor(() => assetGraph.nodes.length === 4, "Did not successfully add DAT node as child to AHU");
                                     }, false);

                                     await this.m_wizardDriver.stepNTimes(wizard, 1);
                                     let aggNodeConfigurerStep = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardAggregationNodeConfigurerComponent);
                                     let nodeConfigurer        = await waitFor(() => aggNodeConfigurerStep.test_nodeBindingConfigurer, "could not get node configurer");
                                     await this.m_driver.click(nodeConfigurer.test_table.test_add, "add agg node binding btn");
                                     await waitFor(() => !aggNodeConfigurerStep.isValid(), "node configurer should be invalid after adding 3rd column");
                                     const assetGraph = nodeConfigurer.graphExt.model;
                                     const vavNodeId  = assetGraph.nodes.find((node) => node.name === "VAV")?.id;
                                     assertIsDefined(vavNodeId, "vav node id");
                                     const datFromAhuId = assetGraph.transforms.find((transform) => transform.inputId === ahuNodeId && transform.outputId !== vavNodeId)?.outputId;
                                     assertIsDefined(datFromAhuId, "dat node from ahu");
                                     const selectionHierarchy = nodeConfigurer.graphExt.getAncestorHierarchy(datFromAhuId);
                                     await this.m_selectionDriver.makeSelection("3rd agg node binding", nodeConfigurer.test_bindings.last, selectionHierarchy);
                                     await waitFor(() => aggNodeConfigurerStep.isValid(), "node configurer should still be valid after adding a third node");
                                     await this.m_selectionDriver.makeMatSelection("3rd agg type select", nodeConfigurer.test_aggTypes.last, "Mean");
                                     await waitFor(() => nodeConfigurer.nodeExts[2].model.aggregationType === Models.AggregationTypeId.MEAN, "3rd agg type should now be mean");

                                     await this.m_wizardDriver.stepNTimes(wizard, 1);
                                     const rangeStep     = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardAggregationRangeStepComponent);
                                     const rangeSelector = await waitFor(() => rangeStep.test_rangeSelector?.test_rangeSelector, "could not get range selector");
                                     await this.m_selectionDriver.makeSelection("range selector", rangeSelector, [TimeRanges.Last3Days]);
                                 });
        await this.saveDashboardChanges();

        widget = await this.getNewDataAggregationWidget();
        assertTrue(widget.config.columns[widget.config.columns.length - 1].aggregationType === Models.AggregationTypeId.MEAN, "last column should have mean aggregation type");
        await this.verifyItalics(widget, true);
        assertTrue(widget.config.filterableRanges[0].range.range === Models.TimeRangeId.Last3Days, "range was not last 3 days");

        const aggTable = await waitFor(() => widget.dataAggregation.nodeBindingAggTable.aggregationTable, "could not get agg binding's agg table");
        await this.ensureFocused(widget);
        await this.verifySorted(aggTable, (row) => row.primaryLabel, (a,
                                                                      b) => UtilsService.compareStrings(a, b, true));
        await this.m_driver.click(aggTable.test_sorts.first.test_elemRef, "data aggregation's name col sort arrow");
        await this.verifySorted(aggTable, (row) => row.primaryLabel, (a,
                                                                      b) => UtilsService.compareStrings(a, b, false));
        await this.m_driver.click(aggTable.test_sorts.last.test_elemRef, "data aggregation's last col's sort arrow");
        await this.verifySorted(aggTable, (row) => row.cells[row.cells.length - 1].value, (a,
                                                                                           b) => UtilsService.compareNumbers(a, b, false));
    }

    private async verifyItalics(widget: AggregationTableWidgetComponent,
                                lastIsNormal: boolean)
    {
        const firstRow: HTMLElement = widget.element.nativeElement.querySelector(".o3-aggregation-table--top-of-group");
        assertIsDefined(firstRow, "first row of table");
        const firstRowCells = firstRow.querySelectorAll(".o3-aggregation-table--cell");
        assertTrue(!!firstRowCells?.length, "could not get first row's cells");

        assertTrue(Array.from(firstRowCells)
                        .every((cell,
                                idx) =>
                               {
                                   const shouldBeNormal    = idx === 0 || (idx === firstRowCells.length - 1 && lastIsNormal);
                                   const expectedFontStyle = shouldBeNormal ? "normal" : "italic";
                                   return getComputedStyle(cell).fontStyle === expectedFontStyle;
                               }), "should only be italic in cells that represent more than one control point");
    }

    private async verifySorted<T>(aggTable: AggregationTableComponent,
                                  getVal: (row: AggregationTableRow) => T,
                                  compare: (a: T,
                                            b: T) => number): Promise<void>
    {
        let rows = await waitFor(() => aggTable.rows.length && aggTable.rows, "could not get agg table's rows");

        let prev = rows[0];
        for (let i = 1; i < rows.length; i++)
        {
            let curr = rows[i];
            assertTrue(compare(getVal(prev), getVal(curr)) <= 0, "not sorted correctly");
            prev = curr;
        }
    }
}

@TestCase({
              id        : "dashboard_widget_aggregationTrend",
              name      : "Aggregation Trend widget",
              timeout   : 75,
              categories: [
                  "Dashboard",
                  "Widgets"
              ]
          })
class AggregationTrendWidgetTest extends WidgetTest<Models.AggregationTrendWidgetConfiguration>
{
    protected readonly m_widgetType = Models.AggregationTrendWidgetConfiguration;

    private async getNewAggregationTrend(): Promise<AggregationTrendWidgetComponent>
    {
        let widgetContainer = await this.getNewWidgetContainer();
        return waitFor(() => widgetContainer.widget instanceof AggregationTrendWidgetComponent && widgetContainer.widget,
                       "Widget picked up is not an aggregation trend");
    }

    public async execute(): Promise<void>
    {
        await this.createNewWidget(async (wizard) =>
                                   {
                                       await this.m_wizardDriver.stepNTimes(wizard, 1);
                                       const innerGroupingStep = await this.getInnerGroupingStep(wizard);
                                       await this.m_driver.sendText(innerGroupingStep.test_groupNames.first, "group name", "Group 1");
                                       await this.m_driver.click(innerGroupingStep.test_configureSourceTriggers.first, "group source configure button");
                                       const dataSourceWizard = await this.m_wizardDriver.getWizard<DataSourceWizardState, DataSourceWizardDialogComponent>(DataSourceWizardDialogComponent);
                                       await this.m_wizardDriver.stepNTimes(dataSourceWizard, 1);
                                       await this.m_wizardDriver.standardSelectControlPoints(dataSourceWizard, "outside air temperature", this.m_numAhus);
                                       await this.m_wizardDriver.save(dataSourceWizard, "Unable to save selected points");
                                       await waitFor(() => !innerGroupingStep.sourcesUpdating, "Sources never completed updating");
                                       await this.m_wizardDriver.stepNTimes(wizard, 3);
                                   });

        let aggregationTrend = await this.getNewAggregationTrend();
        await this.verifyConfig(aggregationTrend.config, Models.AggregationTrendVisualizationMode.Line, "New widget does not have expected config");
        await this.verifyValuePrecision(aggregationTrend);

        await this.editNewWidget(async (wizard) =>
                                 {
                                     const innerGroupingStep = await this.getInnerGroupingStep(wizard);
                                     await this.m_selectionDriver.makeSelection("agg trend chart type", innerGroupingStep.test_trendType, [Models.AggregationTrendVisualizationMode.Bar]);
                                 });
        await this.saveDashboardChanges();
        aggregationTrend = await this.getNewAggregationTrend();
        await this.verifyConfig(aggregationTrend.config, Models.AggregationTrendVisualizationMode.Bar, "Widget edit does not have expected config");

        await this.navigateAwayAndBack();
        aggregationTrend = await this.getNewAggregationTrend();
        await this.verifyConfig(aggregationTrend.config, Models.AggregationTrendVisualizationMode.Bar, "Widget edits did not persist");
        await this.verifyValuePrecision(aggregationTrend);
    }

    private async verifyConfig(config: Models.AggregationTrendWidgetConfiguration,
                               visualizationMode: Models.AggregationTrendVisualizationMode,
                               errorMsg: string): Promise<void>
    {
        await waitFor(() => config.groups.length === 1 &&
                            config.groups[0].selections.identities.length === this.m_numAhus &&
                            config.visualizationMode === visualizationMode, errorMsg);
    }

    private async verifyValuePrecision(aggregationTrend: AggregationTrendWidgetComponent)
    {
        const maxNumDecimals = aggregationTrend.config.groups[0].valuePrecision;
        const ticks          = await waitFor(() => aggregationTrend.test_chart.test_ticks.length && aggregationTrend.test_chart.test_ticks,
                                             "could not get ticks or ticks not yet generated");
        await assertTrue(ticks.every((ticks) => ticks.every((tick) =>
                                                            {
                                                                const tickVal    = "" + tick;
                                                                const decimalIdx = tickVal.indexOf(".");
                                                                if (decimalIdx === -1) return true;
                                                                return tickVal.length - decimalIdx - 1 <= maxNumDecimals;
                                                            })), "not all ticks have appropriate number of decimals");
    }
}

@TestCase({
              id        : "dashboard_widget_alertFeed",
              name      : "Alert Feed widget",
              timeout   : 75,
              categories: [
                  "Dashboard",
                  "Widgets"
              ]
          })
class AlertFeedWidgetTest extends WidgetTest<Models.AlertFeedWidgetConfiguration>
{
    protected readonly m_widgetType = Models.AlertFeedWidgetConfiguration;

    private async getNewAlertFeed(): Promise<AlertFeedWidgetComponent>
    {
        let widgetContainer = await this.getNewWidgetContainer();
        return waitFor(() => widgetContainer.widget instanceof AlertFeedWidgetComponent && widgetContainer.widget,
                       "Widget picked up is not an alert feed");
    }

    public async execute(): Promise<void>
    {
        await this.createNewWidget((wizard) => this.m_wizardDriver.stepNTimes(wizard, 5));
        await this.navigateAwayAndBack();

        let alertFeed = await this.getNewAlertFeed();
        await waitFor(() => alertFeed.config.alertTypes.length === 0, "New alert map widget does not have expected default settings");
        const numAlerts = await waitFor(() => alertFeed.alerts.length, "Could not get alerts");

        const targetAlertType = Models.AlertType.INFORMATIONAL;
        await this.editNewWidget(async (wizard) => this.selectAlertType(wizard, targetAlertType));
        await this.saveDashboardChanges();
        await this.navigateAwayAndBack();

        alertFeed = await this.getNewAlertFeed();
        await waitFor(() => UtilsService.compareArraysAsSets(alertFeed.config.alertTypes, [targetAlertType]), "Widget edits did not persist");
        await waitFor(() => alertFeed.alerts.length < numAlerts, "Alert count did not decrease with alert type subset selection");
    }
}

@TestCase({
              id        : "dashboard_widget_alertMap",
              name      : "Alert Map widget",
              timeout   : 100,
              categories: [
                  "Dashboard",
                  "Widgets"
              ]
          })
class AlertMapWidgetTest extends WidgetTest<Models.AlertMapWidgetConfiguration>
{
    protected readonly m_widgetType = Models.AlertMapWidgetConfiguration;

    private async getNewAlertMap(): Promise<AlertMapWidgetComponent>
    {
        let widgetContainer = await this.getNewWidgetContainer();
        return waitFor(() => widgetContainer.widget instanceof AlertMapWidgetComponent && widgetContainer.widget,
                       "Widget picked up is not an alert map");
    }

    public async execute(): Promise<void>
    {
        await this.createNewWidget((wizard) => this.m_wizardDriver.stepNTimes(wizard, 5));
        await this.navigateAwayAndBack();

        let alertMap = await this.getNewAlertMap();
        await waitFor(() => alertMap.config.pin.pinIcon !== Models.MapPinIcon.Circle, "New alert map widget does not have expected default settings");

        const iconType = Models.MapPinIcon.Circle;
        await this.editNewWidget(async (wizard) =>
                                 {
                                     await this.m_wizardDriver.stepNTimes(wizard, 1);
                                     await this.m_wizardDriver.waitForWizard(wizard);

                                     const pinStep        = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardPinConfigStepComponent);
                                     const radioButtonIdx = pinStep.icons.findIndex((iconOption) => iconOption.id === iconType);
                                     const radioButton    = await waitFor(() => pinStep.test_iconTypes.toArray()[radioButtonIdx], `Unable to pick up radio button ${radioButtonIdx + 1}: ${iconType}`);
                                     await this.m_driver.click(radioButton, "pin type: " + iconType);
                                 });

        alertMap = await this.getNewAlertMap();
        await waitFor(() => alertMap.config.pin.pinIcon === iconType, "Widget edit did not work: " + alertMap.config.pin.pinIcon);

        await this.saveDashboardChanges();
        await this.navigateAwayAndBack();

        alertMap = await this.getNewAlertMap();
        await waitFor(() => alertMap.config.pin.pinIcon === iconType, "Widget edit did not correctly persist");
    }
}

@TestCase({
              id        : "dashboard_widget_alertSummary",
              name      : "Alert Summary widget",
              timeout   : 60,
              categories: [
                  "Dashboard",
                  "Widgets"
              ]
          })
class AlertSummaryWidgetTest extends WidgetTest<Models.AlertSummaryWidgetConfiguration>
{
    protected readonly m_widgetType = Models.AlertSummaryWidgetConfiguration;

    private async getNewAlertSummary(): Promise<AlertSummaryWidgetComponent>
    {
        let widgetContainer = await this.getNewWidgetContainer();
        return waitFor(() => widgetContainer.widget instanceof AlertSummaryWidgetComponent && widgetContainer.widget,
                       "Widget picked up is not an alert feed");
    }

    public async execute(): Promise<void>
    {
        await this.createNewWidget((wizard) => this.m_wizardDriver.stepNTimes(wizard, 3));

        let alertSummary = await this.getNewAlertSummary();
        await waitFor(() => alertSummary.config.alertTypes.length === 0, "New alert summary does not have the expected default settings");
        const numAlerts = await waitFor(() => alertSummary.values.total, "Alert summary did not stop loading");

        const targetAlertType = Models.AlertType.INFORMATIONAL;
        await this.editNewWidget(async (wizard) => this.selectAlertType(wizard, targetAlertType));
        await this.saveDashboardChanges();

        alertSummary = await this.getNewAlertSummary();
        await waitFor(() => UtilsService.compareArraysAsSets(alertSummary.config.alertTypes, [targetAlertType]), "Widget edits did not work");
        const numInformationalAlerts = await waitFor(() => alertSummary.values.total, "Alert summary did not stop loading");
        await waitFor(() => numAlerts > numInformationalAlerts, "Widget count did not go down as expected", undefined, 1);
        await this.navigateAwayAndBack();
        await waitFor(() => UtilsService.compareArraysAsSets(alertSummary.config.alertTypes, [targetAlertType]), "Widget edits did not persist");
    }
}

@TestCase({
              id        : "dashboard_widget_alertTable",
              name      : "Alert Table widget",
              timeout   : 80,
              categories: [
                  "Dashboard",
                  "Widgets"
              ]
          })
class AlertTableWidgetTest extends WidgetTest<Models.AlertTableWidgetConfiguration>
{
    protected readonly m_widgetType = Models.AlertTableWidgetConfiguration;

    private async getNewAlertTable(): Promise<AlertTableWidgetComponent>
    {
        let widgetContainer = await this.getNewWidgetContainer();
        return waitFor(() => widgetContainer.widget instanceof AlertTableWidgetComponent && widgetContainer.widget,
                       "Widget picked up is not an alert table");
    }

    public async execute(): Promise<void>
    {
        const severityOfInterest        = Models.AlertSeverity.CRITICAL;
        const severityOfInterestColorId = "mapblue";
        await this.createNewWidget(async (wizard) =>
                                   {
                                       await this.m_wizardDriver.stepNTimes(wizard, 7);
                                       const alertTableDisplayStep = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardAlertTableDisplayStepComponent);
                                       await this.m_wizardDriver.waitForWizard(wizard);
                                       const severityConfigurer = await waitFor(() => alertTableDisplayStep.severityConfigurer, "Could not get alert severity configurer");
                                       await waitFor(() => severityConfigurer.alertSeverityOptions?.length && severityConfigurer.alertSeverityOptions.length === severityConfigurer.test_colors?.length,
                                                     "Severity step did not properly load");
                                       const colorPicker = await waitFor(() =>
                                                                         {
                                                                             const idx = severityConfigurer.severityColors.findIndex((severityColor) => severityColor.severity === severityOfInterest);
                                                                             return severityConfigurer.test_colors.toArray()[idx];
                                                                         }, "Could not get color picker");
                                       await this.m_colorsDriver.pickColor(colorPicker, severityOfInterestColorId, "severity");
                                       await this.m_wizardDriver.stepNTimes(wizard, 2);
                                   });

        let alertTable                = await this.getNewAlertTable();
        let severityColor             = alertTable.config.severityColors.find((sevColor) => sevColor.severity === severityOfInterest);
        const severityOfInterestColor = ChartColorUtilities.safeChroma(ChartColorUtilities.getColorById("Map Colors", severityOfInterestColorId).hex)
                                                           .hex();
        await waitFor(() => severityColor.color === severityOfInterestColor,
                      `New widget had "${severityColor.color}" associated with ${severityOfInterest} instead of "${severityOfInterestColor}"`,
                      undefined, 1);

        await this.editNewWidget(async (wizard) =>
                                 {
                                     await this.m_wizardDriver.stepNTimes(wizard, 5);
                                     const severityStep       = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardAlertSeverityStepComponent);
                                     const severityConfigurer = await waitFor(() => severityStep.severityConfigurer, "Could not get alert severity configurer");
                                     const severityOption     = await waitFor(() => severityConfigurer.alertSeverityOptions.find((option) => option.id == severityOfInterest),
                                                                              "Could not severity option with id " + severityOfInterest, undefined, 1);
                                     await waitFor(() => severityConfigurer.test_allSeverities, "Could not get alert severity toggle");
                                     await this.m_driver.click(severityConfigurer.test_allSeverities, "All severity toggle");
                                     await this.m_selectionDriver.makeMultiSelection("severity", severityConfigurer.test_severities, [[severityOption.id]]);
                                 });
        await this.saveDashboardChanges();

        alertTable = await this.getNewAlertTable();
        await waitFor(() => UtilsService.compareArraysAsSets(alertTable.config.alertSeverityIDs, [severityOfInterest]), "Widget edit did not work");
        await this.navigateAwayAndBack();

        alertTable = await this.getNewAlertTable();
        await waitFor(() =>
                      {
                          severityColor = alertTable.config.severityColors.find((sevColor) => sevColor.severity === severityOfInterest);
                          if (severityColor.color !== severityOfInterestColor) return false;
                          return UtilsService.compareArraysAsSets(alertTable.config.alertSeverityIDs, [severityOfInterest]);
                      }, "Widget edits did not persist", undefined, 1);

        await waitFor(() =>
                      {
                          for (let valueElem of alertTable.element.nativeElement.querySelectorAll(".o3-alert-table-widget--value"))
                          {
                              let color = ChartColorUtilities.safeChroma(getComputedStyle(valueElem).backgroundColor)
                                                             .hex();
                              if (color !== severityOfInterestColor) return false;
                          }
                          return true;
                      }, "Not all the alert table entries have the proper color", undefined, 1);
    }
}

@TestCase({
              id        : "dashboard_widget_selector",
              name      : "Selector widget",
              timeout   : 170,
              categories: [
                  "Dashboard",
                  "Widgets"
              ]
          })
class AssetGraphSelectorWidgetTest extends WidgetTest<Models.AssetGraphSelectorWidgetConfiguration>
{
    protected readonly m_widgetType = Models.AssetGraphSelectorWidgetConfiguration;

    private async getNewAssetGraphSelector(): Promise<AssetGraphSelectorWidgetComponent>
    {
        let widgetContainer = await this.getNewWidgetContainer();
        return waitFor(() => widgetContainer.widget instanceof AssetGraphSelectorWidgetComponent && widgetContainer.widget,
                       "Widget picked up is not an asset graph selector");
    }

    public async init(): Promise<void>
    {
        await super.init();

        await this.ensureAssetGraph();
    }

    public async execute(): Promise<void>
    {
        await this.createNewWidget(async (wizard) =>
                                   {
                                       await this.m_wizardDriver.stepNTimes(wizard, 1);
                                       const graphsStep = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardGraphsStepComponent);
                                       await this.m_wizardDriver.waitForWizard(wizard);

                                       await this.m_assetGraphDriver.addImportAssetGraphFlow(graphsStep, AssetGraphTest.ahuVavDatGraphName, true, true);

                                       await this.m_wizardDriver.stepNTimes(wizard, 1);
                                       await this.m_wizardDriver.waitForWizard(wizard);
                                       const selectorStep     = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardGraphSelectorStepComponent);
                                       const optionOfInterest = selectorStep.graphOptions.find((option) => option.label === AssetGraphTest.ahuVavDatGraphName);
                                       await this.m_selectionDriver.makeSelection("asset graph select", selectorStep.test_graphs, [optionOfInterest.id]);
                                       await this.m_wizardDriver.stepNTimes(wizard, 1);
                                   });

        let assetSelector = await this.getNewAssetGraphSelector();
        let selectedAsset = await this.selectNextContext(assetSelector);
        await this.navigateAwayAndBack();
        assetSelector = await this.getNewAssetGraphSelector();
        await waitFor(() => assetSelector.selectedAsset === selectedAsset, "Config and asset selection did not persist");

        const numWidgets     = this.m_page.cfg.model.widgets.length;
        const aggSummaryName = await this.createAggregationSummaryWidget(async (wizard) =>
                                                                         {
                                                                             await this.m_wizardDriver.selectDataSourceType(wizard, Models.TimeSeriesChartType.GRAPH);
                                                                             await this.m_wizardDriver.stepNTimes(wizard, 1);

                                                                             const bindingsStep = await this.m_wizardDriver.getStep(wizard, DataSourceWizardGraphBindingsStepComponent);
                                                                             await this.pickOnlyAvailableBinding(bindingsStep, false);
                                                                             const selectorSelect = await waitFor(() => bindingsStep.test_selector, "Could not get selector select component");
                                                                             await this.m_selectionDriver.makeSelection("selector select", selectorSelect, [bindingsStep.selectorOptions[0].id]);
                                                                         }, true);
        await waitFor(() => numWidgets + 2 === this.m_page.cfg.model.widgets.length, "Did not create two new widgets as was expected");

        let graph              = new WidgetGraph(this.m_page.widgetManipulator);
        const newAssetSelector = <AssetGraphSelectorWidgetComponent>await waitFor(() => graph.findNode((node) => node.widget.config instanceof Models.AssetGraphSelectorWidgetConfiguration
                                                                                                                 && node.widget.config.name !== this.m_newWidgetName)?.widget,
                                                                                  "Could not find widget created with the asset graph aggregation summary", undefined, 1);
        const assetBinding     = Models.AssetGraphBinding.newInstance({selectorId: newAssetSelector.config.selectorId});
        assetBinding.graphId   = this.m_page.cfg.getAssociatedGraphId(newAssetSelector.config.selectorId);
        const newAggSummary    = <AggregationWidgetComponent>(await this.getWidgetContainer(aggSummaryName)).widget;
        await this.verifySelectedAsset(newAggSummary, assetBinding, newAssetSelector.selectedAsset, "Aggregation summary is not reflecting the correct asset");

        selectedAsset = await this.selectNextContext(newAssetSelector);
        await waitFor(() => !newAggSummary.isLoading, "Aggregation summary never stopped loading");
        await this.verifySelectedAsset(newAggSummary, assetBinding, selectedAsset, "Aggregation summary did not update its selected asset correctly");

        await this.verifyPastability(false);
        await this.verifyPastability(true, aggSummaryName);
    }

    private async verifySelectedAsset(aggSummary: AggregationWidgetComponent,
                                      assetBinding: Models.AssetGraphBinding,
                                      assetId: string,
                                      errorMsg: string): Promise<void>
    {
        const selectedContext = <Models.AssetGraphContextAsset>await this.m_page.cfg.getGraphContext(assetBinding.selectorId);
        await waitFor(() => selectedContext.sysId === assetId, "Dashboard does not reflect the same selected asset", undefined, 1);
        await waitFor(() => aggSummary.test_contextId === assetId, errorMsg);
    }
}

@TestCase({
              id        : "dashboard_widget_controlPoint",
              name      : "Control Point widget",
              timeout   : 75,
              categories: [
                  "Dashboard",
                  "Widgets"
              ]
          })
class ControlPointWidgetTest extends WidgetTest<Models.ControlPointWidgetConfiguration>
{
    protected readonly m_widgetType = Models.ControlPointWidgetConfiguration;

    private async getNewControlPoint(): Promise<ControlPointWidgetComponent>
    {
        let widgetContainer = await this.getNewWidgetContainer();
        return waitFor(() => widgetContainer.widget instanceof ControlPointWidgetComponent && widgetContainer.widget,
                       "Widget picked up is not a control point summary");
    }

    public async execute(): Promise<void>
    {
        await this.createNewWidget(async (wizard) =>
                                   {
                                       await this.m_wizardDriver.stepNTimes(wizard, 1);
                                       await this.m_wizardDriver.waitForWizard(wizard);
                                       const cpStep = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardControlPointStepComponent);
                                       await this.m_driver.click(cpStep.test_cpTrigger, "control point wizard trigger");

                                       const dataSourceWizard = await this.m_wizardDriver.getWizard<DataSourceWizardState, DataSourceWizardDialogComponent>(DataSourceWizardDialogComponent);
                                       await this.m_wizardDriver.stepNTimes(dataSourceWizard, 1);
                                       await this.m_wizardDriver.standardSelectControlPoints(dataSourceWizard, "discharge air temperature", Math.min(this.m_numAhus * 6, 10), 1);
                                       await this.m_wizardDriver.save(dataSourceWizard, "Could not save the control point selection");
                                       await waitFor(() => !cpStep.updatingConfig, "Control point step did not stop updating");
                                       await this.m_wizardDriver.stepNTimes(wizard, 1);
                                   });

        let cpWidget = await this.getNewControlPoint();
        await waitFor(() => cpWidget.config.fontMultiplier === 1 && cpWidget.config.timestampEnabled, "Widget does not have expected default settings");
        const initialFont = parseInt(<string>cpWidget.entryStyling["font-size"]);

        await this.editNewWidget(async (wizard) =>
                                 {
                                     await this.m_wizardDriver.waitForWizard(wizard);
                                     const cpStep = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardControlPointStepComponent);
                                     await this.m_driver.click(cpStep.test_timestampToggle, "toggle timestamp");
                                     await this.m_driver.click(cpStep.test_fontScalingToggle, "toggle font scaling");
                                 });

        cpWidget = await this.getNewControlPoint();
        await waitFor(() => !cpWidget.config.fontMultiplier && !cpWidget.config.timestampEnabled, "Widget edits did not work");
        await waitFor(() => initialFont < parseInt(<string>cpWidget.entryStyling["font-size"]), "Font size did not get bigger upon turning on automatic font scaling", undefined, 1);
        await this.saveDashboardChanges();

        await this.navigateAwayAndBack();
        cpWidget = await this.getNewControlPoint();
        await waitFor(() => !cpWidget.config.fontMultiplier && !cpWidget.config.timestampEnabled, "Widget edits did not persist");

        await this.verifyPastability(false);
    }
}

abstract class ChartWidgetTest extends WidgetTest<Models.TimeSeriesWidgetConfiguration>
{
    protected readonly m_widgetType = Models.TimeSeriesWidgetConfiguration;

    protected async getChartWidget(): Promise<TimeSeriesWidgetComponent>
    {
        let widgetContainer = await this.getNewWidgetContainer();
        return waitFor(() => widgetContainer.widget instanceof TimeSeriesWidgetComponent && widgetContainer.widget,
                       "Widget picked up is not a chart widget");
    }

    protected async waitForTimeSeriesStepValidity(timeSeriesStep: WidgetEditorWizardTimeSeriesStepComponent): Promise<void>
    {
        try
        {
            await waitFor(() => !timeSeriesStep.isValid(), "just waiting for this state for test case's reliability", 50, 20);
        }
        catch (e)
        {
            // not an indication of failure, saving was just faster than expected
        }
        await waitFor(() => timeSeriesStep.isValid(), "time series step did not validate after modifying sources");
    }
}

@TestCase({
              id        : "dashboard_widget_chart_standard",
              name      : "Chart widget - standard",
              timeout   : 90,
              categories: [
                  "Dashboard",
                  "Widgets"
              ]
          })
class StandardLineChartWidgetTest extends ChartWidgetTest
{
    public async execute(): Promise<void>
    {
        let numCps = 0;
        await this.createNewWidget(async (wizard) =>
                                   {
                                       await this.m_wizardDriver.stepNTimes(wizard, 1);
                                       const timeSeriesStep = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardTimeSeriesStepComponent);
                                       const addChart       = await waitFor(() => timeSeriesStep.test_chartSet.toolbar?.test_addChartButton, "Couldn't get add chart button");
                                       await this.m_driver.click(addChart, "add chart");

                                       const dataSourceWizard = await this.m_wizardDriver.getWizard<DataSourceWizardState, DataSourceWizardDialogComponent>(DataSourceWizardDialogComponent);
                                       await this.m_wizardDriver.stepNTimes(dataSourceWizard, 1);
                                       numCps += await this.m_wizardDriver.standardSelectControlPoints(dataSourceWizard, "discharge air temperature", Math.min(this.m_numAhus * 6, 10), 1);
                                       await this.m_wizardDriver.save(dataSourceWizard, "Failed to save control points selected");
                                       await this.waitForTimeSeriesStepValidity(timeSeriesStep);
                                       await this.m_wizardDriver.stepNTimes(wizard, 1);
                                   });

        let chart = await this.getChartWidget();
        await waitFor(() => chart.config.charts[0].dataSources.length === numCps, `Chart should have ${numCps} data sources`);
        await waitFor(() => chart.activeVisualization?.set.timeSeriesContainers[0]?.test_consolidated?.sourceChips?.length === numCps,
                      `chart should have ${numCps} unconsolidated source chips`);

        await this.editNewWidget(async (wizard) =>
                                 {
                                     await this.m_wizardDriver.waitForWizard(wizard);
                                     const timeSeriesStep      = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardTimeSeriesStepComponent);
                                     const timeSeriesContainer = await waitFor(() => timeSeriesStep.test_chartSet.set.timeSeriesContainers[0], "Couldn't get time series container");
                                     await this.m_driver.click(timeSeriesContainer.test_addSources, "add sources");
                                     const dataSourceWizard = await this.m_wizardDriver.getWizard<DataSourceWizardState, DataSourceWizardDialogComponent>(DataSourceWizardDialogComponent);
                                     numCps += await this.m_wizardDriver.standardSelectControlPoints(dataSourceWizard, "outside air temperature", this.m_numAhus);
                                     await this.m_wizardDriver.save(dataSourceWizard, "Failed to add outside air temp control points");
                                     await this.waitForTimeSeriesStepValidity(timeSeriesStep);
                                 });
        await this.saveDashboardChanges();
        chart = await this.getChartWidget();
        await waitFor(() => chart.config.charts[0].dataSources.length === numCps, `Chart should have ${numCps} data sources`);

        await this.navigateAwayAndBack();
        chart                     = await this.getChartWidget();
        const timeSeriesContainer = await waitFor(() => chart.activeVisualization?.set.timeSeriesContainers[0], "Could not get chart widget's time series container");
        await waitFor(() => !timeSeriesContainer.showSources, "There should be no consolidated sources");

        await this.verifyCenterHovered();
        const chartContainer = await this.getNewWidgetContainer();
        await this.m_driver.click(chartContainer.element, "new widget");
        await waitFor(() => chartContainer.focus, "new widget did not get focused");
        const chartTimeline = timeSeriesContainer.chartElement.chartTimeline;
        await waitFor(() => chartTimeline.zoomSource, "chart timeline did not become ready for zooming", undefined, 1);
        assertTrue(chartTimeline.displayedRange.diffAsMs === chartTimeline.timeRange.diffAsMs, "the view should be equivalent to time range");
        await this.m_driver.mouseWheel(0, -200);
        await waitFor(() => chartTimeline.displayedRange.diffAsMs < chartTimeline.timeRange.diffAsMs, "chart did not zoom in", undefined, 1);

        const containerRect   = chartContainer.element.nativeElement.getBoundingClientRect();
        const scrollContainer = document.querySelector(".o3-standard-layout--scroll-container");
        const startScrollTop  = scrollContainer.scrollTop;
        await this.m_driver.clickPoint({
                                           x: containerRect.left + 10,
                                           y: (containerRect.top + containerRect.bottom) / 2
                                       }, "new widget");
        await this.m_driver.mouseWheel(0, -100);
        await waitFor(() => startScrollTop > scrollContainer.scrollTop,
                      "scroll did not escape chart widget to scroll greater dashboard", undefined, 1);

        await this.editNewWidget(async (wizard) =>
                                 {
                                     await this.m_wizardDriver.stepNTimes(wizard, 1);
                                     const nameDescriptionStep = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardNameDescriptionStepComponent);
                                     await this.m_selectionDriver.makeSelection("toolbar behavior", nameDescriptionStep.test_toolbarBehavior, [Models.WidgetToolbarBehavior.AutoHide]);
                                 });
        await this.saveDashboardChanges();
        await this.verifyCenterHovered();
        const menuTrigger = await this.getWidgetMenuTrigger();
        await this.m_driver.hover(menuTrigger, "chart container menu trigger");
        await this.verifyCenterHovered();

        await this.m_driver.click(menuTrigger, "chart container menu trigger");
        await waitFor(() => document.querySelector(".mat-menu-item o3-consolidated-source-chip"), "Could not find consolidated chip menu option");
    }

    private async verifyCenterHovered()
    {
        await this.ensureNoFocus();

        const chartContainer    = await this.getNewWidgetContainer();
        const widgetContentElem = chartContainer.widgetScrollContainer.nativeElement;
        const widgetContentRect = widgetContentElem.getBoundingClientRect();
        const widgetCenter      = getCenterPoint(widgetContentElem);

        await this.m_driver.hover(widgetContentElem, "widget content");

        assertTrue(areEqual(chartContainer.widget.test_mouseMove.offsetY, widgetCenter.y - widgetContentRect.top, 1),
                   `offset was ${chartContainer.widget.test_mouseMove.offsetY} instead of ${widgetCenter.y - widgetContentRect.top}`);
    }
}

@TestCase({
              id        : "dashboard_widget_chart",
              name      : "Chart widget - external graph",
              timeout   : 135,
              categories: [
                  "Dashboard",
                  "Widgets"
              ]
          })
class GraphLineChartWidgetTest extends ChartWidgetTest
{
    private m_heatSpNodeId: string;
    private m_redColor: string;

    public async init(): Promise<void>
    {
        await super.init();

        await this.ensureAssetGraph();
    }

    public async execute(): Promise<void>
    {
        await this.createNewWidget(async (wizard) =>
                                   {
                                       await this.m_wizardDriver.stepNTimes(wizard, 1);
                                       const timeSeriesStep = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardTimeSeriesStepComponent);
                                       const addChart       = await waitFor(() => timeSeriesStep.test_chartSet.toolbar?.test_addChartButton, "Couldn't get add chart button");
                                       await this.m_driver.click(addChart, "add chart");

                                       const dataSourceWizard = await this.m_wizardDriver.getWizard<DataSourceWizardState, DataSourceWizardDialogComponent>(DataSourceWizardDialogComponent);
                                       await this.m_wizardDriver.selectDataSourceType(dataSourceWizard, Models.TimeSeriesChartType.GRAPH);

                                       const dataSourceGraphsStep = await this.m_wizardDriver.getStep(dataSourceWizard, DataSourceWizardGraphsStepComponent);
                                       await this.m_assetGraphDriver.addImportAssetGraphFlow(dataSourceGraphsStep, AssetGraphTest.ahuVavDatGraphName, true, false);
                                       await this.m_driver.click(dataSourceGraphsStep.test_externalGraphs.test_configureGraphs.last, "configure graph button");
                                       const assetStructureWizard = await this.m_driver.getComponent(AssetStructureWizardDialogComponent);
                                       const assetGraphStep       = await this.m_wizardDriver.getStep(assetStructureWizard.wizard, AssetStructureWizardDataStepComponent);
                                       const vavNode              = await waitFor(() => assetGraphStep.graph.nodes.find((node) => node.name === "VAV"), "node with name VAV not found");
                                       const graphEditor          = await waitFor(() => assetGraphStep.test_graphStep.test_graphEditor, "Could not get asset graph editor");
                                       this.m_heatSpNodeId        = await this.m_assetGraphDriver.addAssetGraphNode(graphEditor,
                                                                                                                    vavNode.id,
                                                                                                                    null,
                                                                                                                    ConditionNodeType.POINT,
                                                                                                                    false,
                                                                                                                    airflowHeatingSpOptionLabel);
                                       await waitFor(() => assetGraphStep.graph.nodes.length === 4, "Did not successfully add heat setpoint node");
                                       await this.m_wizardDriver.save(assetStructureWizard.wizard, "Failed to save asset structure change");
                                       await this.m_wizardDriver.stepNTimes(dataSourceWizard, 1);

                                       const dataSourceGraphBindingsStep = await this.m_wizardDriver.getStep(dataSourceWizard, DataSourceWizardGraphBindingsStepComponent);
                                       const externalBindingOptions      = await this.m_driver.getComponentValue(dataSourceGraphBindingsStep, (step) =>
                                       {
                                           if (step.test_bindingsExternal?.options?.length) return step.test_bindingsExternal.options;
                                           return null;
                                       }, "external bindings options");
                                       const ahuOption                   = externalBindingOptions[0].children[0];
                                       const vavOption                   = ahuOption.children[0];
                                       let selection                     = [
                                           ahuOption.id,
                                           vavOption.id
                                       ];
                                       let selections                    = [
                                           selection,
                                           UtilsService.arrayCopy(selection)
                                       ];
                                       selections[0].push(vavOption.children[0].id);
                                       selections[1].push(vavOption.children[1].id);
                                       await this.m_selectionDriver.makeMultiSelection("external bindings", dataSourceGraphBindingsStep.test_bindingsExternal, selections);
                                       await this.m_wizardDriver.save(dataSourceWizard, "Could not finish data source wizard to create chart");
                                       await waitFor(() => timeSeriesStep.isValid(), "time series step did not validate after adding sources");

                                       const timeSeriesContainer = await waitFor(() => timeSeriesStep.test_chartSet.set.timeSeriesContainers[0], "could not get time series container");
                                       await this.m_driver.click(timeSeriesContainer.test_settings, "configure chart button");
                                       const chartConfigurer = timeSeriesContainer.chartElement.chartConfigurer;
                                       await this.m_overlayDriver.waitForOpen(chartConfigurer.overlayConfig.optio3TestId);
                                       const sourceConfigurer   = await waitFor(() => chartConfigurer.test_sources, "Couldn't get source editor");
                                       const expectedNumSources = 10;
                                       await waitFor(() => sourceConfigurer.test_sources.length === expectedNumSources, "Should have 5 dats and 5 sps");
                                       assertTrue(Array.from(sourceConfigurer.test_sources)
                                                       .every((source) => source.primaryText), "Sources did not properly load");

                                       const htSpNameContains = "Heating Signal";
                                       this.m_redColor        = ChartColorUtilities.getDefaultColorById("red").hex;
                                       let numMoved           = 0;
                                       while (true)
                                       {
                                           if (numMoved)
                                           {
                                               const secondPanel = <ElementRef<HTMLElement>>sourceConfigurer.test_panels.last;
                                               await waitFor(() => secondPanel.nativeElement.querySelectorAll("o3-source-chip").length === numMoved,
                                                             "The second panel does not have the expected number of sources");

                                           }

                                           let sources                  = sourceConfigurer.test_sources.toArray()
                                                                                          .map((sourceChip) => sourceChip);
                                           let firstHeatSpIdx           = sources.findIndex((source) => source.primaryText.indexOf(htSpNameContains) >= 0);
                                           const allHeatSpOnSecondPanel = firstHeatSpIdx >= expectedNumSources - numMoved;
                                           if (allHeatSpOnSecondPanel) break;

                                           const source = sources[firstHeatSpIdx];
                                           if (source.source.color !== this.m_redColor)
                                           {
                                               const settings = await waitFor(() => source.test_actions.first, "could not get source settings button");
                                               await this.m_driver.click(settings, "source settings button");
                                               await this.m_overlayDriver.waitForOpen(chartConfigurer.sourceConfigurer.config.optio3TestId);
                                               await this.m_colorsDriver.pickColor(chartConfigurer.sourceConfigurer.test_color, "red", "source");
                                               await this.m_formDriver.submitOverlayForm(chartConfigurer.sourceConfigurer.overlay, "Apply");
                                           }

                                           let drag = await waitFor(() => sourceConfigurer.test_sourceDrags.toArray()[firstHeatSpIdx], "could not get corresponding source drag");
                                           if (numMoved++ === 0)
                                           {
                                               await this.m_dragScrollerDriver.dragScroll(sourceConfigurer.scrollContainer, drag, sourceConfigurer.test_newPanel,
                                                                                          "source", "new panel drop zone");
                                               await waitFor(() => sourceConfigurer.test_panels.length === 2 && sourceConfigurer.test_sources.length === 10,
                                                             "chart configurer did not update to have two panels correctly");
                                           }
                                           else
                                           {
                                               const secondPanel            = <HTMLElement>sourceConfigurer.test_panels.last.nativeElement;
                                               const firstSecondPanelSource = <HTMLElement>secondPanel.querySelector("o3-source-chip");
                                               await this.m_dragScrollerDriver.dragScroll(sourceConfigurer.scrollContainer, drag, firstSecondPanelSource,
                                                                                          "source", "second panel source",
                                                                                          () => secondPanel.classList.contains("cdk-drop-list-dragging"));
                                           }
                                       }

                                       assertTrue(numMoved === 5, `Moved ${numMoved} sources to the second panel instead`);
                                       await this.m_formDriver.submitOverlayForm(chartConfigurer.overlay, "Save");
                                       await this.waitForTimeSeriesStepValidity(timeSeriesStep);

                                       await this.m_wizardDriver.stepNTimes(wizard, 1);

                                       const timeSeriesGraphsStep = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardTimeSeriesGraphsComponent);
                                       const selectorNameInput    = await waitFor(() => timeSeriesGraphsStep.test_newSelectorNames.first, "could not get new selector name input");
                                       await this.m_driver.sendText(selectorNameInput, "new selector name input", "Selector");
                                       await waitFor(() => timeSeriesGraphsStep.isValid(), "graphs step did not become valid", 1000, 2);

                                       await this.m_wizardDriver.stepNTimes(wizard, 1);
                                   }, null, false, 2);

        await this.verifyHeatSpConfigs();

        const startSources            = await this.getCurrSources();
        const selectorWidgetContainer = await this.getSelectorWidget();
        await this.selectNextContext(<AssetGraphSelectorWidgetComponent>selectorWidgetContainer.widget);
        await this.verifyHeatSpConfigs();

        let chartWidget = await this.getChartWidget();
        assertTrue(chartWidget.config.charts[0].dataSources.every((source) => !startSources.has(source.uuid + source.id)), "Not all the sources changed");
        const nextSources = await this.getCurrSources();

        await this.navigateAwayAndBack();
        await this.getNewWidgetContainer();

        chartWidget = await this.getChartWidget();
        await assertTrue(chartWidget.config.charts[0].dataSources.every((source) => nextSources.has(source.uuid + source.id)), "chart sources remained the same");
        await this.verifyHeatSpConfigs();

        await this.verifyPastability(true);
    }

    private async getCurrSources(): Promise<Set<string>>
    {
        const chartWidget = await this.getChartWidget();
        return new Set(chartWidget.config.charts[0].dataSources.map((source) => source.uuid + source.id));
    }

    private async verifyHeatSpConfigs()
    {
        const chartWidget   = await this.getChartWidget();
        const dataSources   = await waitFor(() => chartWidget.config.charts[0].dataSources, "Could not get new chart's data sources");
        const heatSpSources = dataSources.filter((source) => source.pointBinding.nodeId === this.m_heatSpNodeId);
        assertTrue(heatSpSources.length === 5, "did not have 5 heat setpoint sources");
        assertTrue(heatSpSources.every((source) => source.panel === 1), "all heat setpoint sources were not on the second panel");
        assertTrue(heatSpSources.every((source) => source.color === this.m_redColor), "all heat setpoint sources were not red");
    }
}

@TestCase({
              id        : "dashboard_widget_groupingWidget",
              name      : "Grouping Widget",
              timeout   : 115,
              categories: [
                  "Dashboard",
                  "Widgets"
              ]
          })
class GroupingWidgetTest extends WidgetTest<Models.GroupingWidgetConfiguration>
{
    protected readonly m_widgetType = Models.GroupingWidgetConfiguration;

    private async getNewGroupingWidget(): Promise<GroupingWidgetComponent>
    {
        let widgetContainer = await this.getNewWidgetContainer();
        return waitFor(() => widgetContainer.widget instanceof GroupingWidgetComponent && widgetContainer.widget,
                       "Widget picked up is not a grouping widget");
    }

    private async checkNewTextWidgets(expectedTextChildren: number): Promise<WidgetBaseComponent<any, any>[]>
    {
        return waitFor(async () =>
                       {
                           if (!this.m_page.versionStateResolved) return null;

                           let graph = new WidgetGraph(this.m_page.widgetManipulator);
                           if (graph.numNodes !== this.m_page.widgetManipulator.widgetContainers.length + expectedTextChildren) return null;

                           let textWidgets    = [];
                           let groupingWidget = await this.getNewGroupingWidget();
                           for (let childWidget of graph.getNode(groupingWidget)?.children || [])
                           {
                               if (childWidget.widget.config instanceof Models.TextWidgetConfiguration && childWidget.widget.config.text === "child of " + this.m_newWidgetName)
                               {
                                   textWidgets.push(childWidget.widget);
                               }
                               else
                               {
                                   return null;
                               }
                           }

                           return textWidgets.length === expectedTextChildren ? textWidgets : null;
                       }, "Widget graph is not picking up the correct number of new text widgets: " + expectedTextChildren);
    }

    public async execute(): Promise<void>
    {
        await this.createNewWidget((wizard) => this.m_wizardDriver.stepNTimes(wizard, 2));
        await this.navigateAwayAndBack();

        await this.getNewGroupingWidget();
        let groupingWidgetContainer = await this.getNewWidgetContainer();
        let widgetMenuTrigger       = await this.getWidgetMenuTrigger();
        await this.m_selectionDriver.selectMenuOption("grouping widget", widgetMenuTrigger, "Add widget", "New");
        const wizard = await this.getWidgetWizard();
        await this.makeWidgetTypeSelection(wizard, Models.TextWidgetConfiguration);
        await this.m_wizardDriver.stepNTimes(wizard, 1);
        const textStep = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardTextStepComponent);

        await waitFor(() => textStep.test_text, "Unable to pick up textarea input element");
        await this.m_driver.sendText(textStep.test_text, "text config", "child of " + this.m_newWidgetName);
        await this.m_wizardDriver.save(wizard, "Unable to finish");

        await this.checkNewTextWidgets(1);
        await this.saveDashboardChanges();
        await this.navigateAwayAndBack();

        let textWidgets = await this.checkNewTextWidgets(1);
        await this.m_driver.click(textWidgets[0].element, "text widget");
        await this.m_driver.sendKeys(["x"], ["ControlRight"]);
        await this.checkNewTextWidgets(0);
        await this.m_driver.sendKeys(["v"], ["MetaLeft"]);
        await this.checkNewTextWidgets(1);

        groupingWidgetContainer = await this.getNewWidgetContainer();
        widgetMenuTrigger       = await this.getWidgetMenuTrigger();
        await this.m_selectionDriver.selectMenuOption("grouping widget container", widgetMenuTrigger, "Add widget", "Paste");
        await this.checkNewTextWidgets(2);
        await this.m_driver.sendKeys(["v"]);
        await this.checkNewTextWidgets(2);

        await this.saveDashboardChanges();
        await this.navigateAwayAndBack();
        await this.checkNewTextWidgets(2);
    }
}

@TestCase({
              id        : "dashboard_widget_image",
              name      : "Image widget",
              timeout   : 90,
              categories: [
                  "Dashboard",
                  "Widgets"
              ]
          })
class ImageTest extends WidgetTest<Models.ImageWidgetConfiguration>
{
    protected readonly m_widgetType = Models.ImageWidgetConfiguration;

    private m_generator = new TestDataGenerator();

    private async getImageWidget(): Promise<ImageWidgetComponent>
    {
        let widgetContainer = await this.getNewWidgetContainer();
        return waitFor(() => widgetContainer.widget instanceof ImageWidgetComponent && widgetContainer.widget,
                       "Widget picked up is not an image widget");
    }

    public async execute(): Promise<void>
    {
        await this.createNewWidget(async (wizard) =>
                                   {
                                       await this.m_wizardDriver.stepNTimes(wizard, 1);
                                       await this.m_wizardDriver.waitForWizard(wizard);
                                       const imgStep     = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardImageStepComponent);
                                       imgStep.imgBase64 = this.m_generator.generateCanvasImage((context) =>
                                                                                                {
                                                                                                    context.fillStyle = "red";
                                                                                                    context.fillRect(25, 25, 350, 50);
                                                                                                }, 400, 100);
                                       await this.m_wizardDriver.stepNTimes(wizard, 1);
                                   });

        let imageWidget     = await this.getImageWidget();
        const imageBranding = imageWidget.config.image;
        await waitFor(() => (imageBranding.horizontalPlacement === Models.HorizontalAlignment.Center || !imageBranding.horizontalPlacement) &&
                            (imageBranding.verticalPlacement === Models.VerticalAlignment.Middle || !imageBranding.horizontalPlacement),
                      "Image widget did not take on expected default settings", undefined, 1);

        await this.checkAligments(imageWidget,
                                  (rects) => areEqual(rects.imgFrameRect.x + (rects.imgFrameRect.width - rects.imgRect.width) / 2, rects.imgRect.x, 0.5),
                                  "image is not correctly horizontally centered");
        await this.checkAligments(imageWidget,
                                  (rects) => areEqual(rects.imgFrameRect.y + (rects.imgFrameRect.height - rects.imgRect.height) / 2, rects.imgRect.y, 0.5),
                                  "image is not correctly vertically centered");

        await this.editNewWidget(async (wizard) =>
                                 {
                                     await this.m_wizardDriver.waitForWizard(wizard);
                                     const imgStep        = await this.m_wizardDriver.getStep(wizard, WidgetEditorWizardImageStepComponent);
                                     const botRightOption = imgStep.placementOptions.find((option) => option.label === "Bottom-right");
                                     await this.m_selectionDriver.makeSelection("image placement", imgStep.test_placement, [botRightOption.id]);
                                 });
        await this.saveDashboardChanges();
        imageWidget = await this.getImageWidget();
        await waitFor(() => imageWidget.config.image.horizontalPlacement === Models.HorizontalAlignment.Right &&
                            imageWidget.config.image.verticalPlacement === Models.VerticalAlignment.Bottom, "Edits did not work");


        await this.navigateAwayAndBack();
        imageWidget = await this.getImageWidget();
        await waitFor(() => imageWidget.config.image.horizontalPlacement === Models.HorizontalAlignment.Right &&
                            imageWidget.config.image.verticalPlacement === Models.VerticalAlignment.Bottom, "Edits did not persist");

        await this.checkAligments(imageWidget,
                                  (rects) => areEqual(rects.imgFrameRect.y + rects.imgFrameRect.height - rects.imgRect.height, rects.imgRect.y, 0.5),
                                  "Vertical alignment is incorrect");
        await this.checkAligments(imageWidget,
                                  (rects) => areEqual(rects.imgFrameRect.x + rects.imgFrameRect.width - rects.imgRect.width, rects.imgRect.x, 0.5),
                                  "Horizontal alignment is incorrect");
    }

    private async checkAligments(imageWidget: ImageWidgetComponent,
                                 fn: (rects: { imgFrameRect: DOMRect, imgRect: DOMRect }) => boolean,
                                 failureMessage: string)
    {
        const widgetElem = await waitFor(() => imageWidget.element?.nativeElement, "Could not get image widget elem");

        await waitFor(() =>
                      {
                          let imgFrameContainer = widgetElem.querySelector(".o3-image-frame--outer-container");
                          let imgElem           = imgFrameContainer.querySelector("img");
                          return fn({
                                        imgFrameRect: imgFrameContainer.getBoundingClientRect(),
                                        imgRect     : imgElem.getBoundingClientRect()
                                    });
                      }, failureMessage, undefined, 1);
    }
}

@NgModule({imports: []})
export class DashboardTestsModule {}
