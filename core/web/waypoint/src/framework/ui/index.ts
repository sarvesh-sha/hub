// libraries
import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {FormsModule} from "@angular/forms";
import {TreeModule} from "@circlon/angular-tree-component";

import {CdkModule} from "framework/cdk";

import {AutofocusDirective} from "framework/directives/autofocus.directive";
import {CidrDirective, CidrRequiredDirective} from "framework/directives/cidr.directive";
import {ExpandDirective} from "framework/directives/expand.directive";
import {HeightBreakpointDirective} from "framework/directives/height-breakpoint.directive";
import {IpAddressWithPortDirective, IpAddressWithPortRequiredDirective} from "framework/directives/ip-address-with-port.directive";
import {IpAddressDirective, IpAddressRequiredDirective} from "framework/directives/ip-address.directive";
import {MinWidthDirective} from "framework/directives/min-width.directive";
//import {PanZoomDirective} from "framework/directives/pan-zoom.directive";
import {PersistTabDirective} from "framework/directives/persist-tab.directive";
import {StickyDirective} from "framework/directives/sticky.directive";
import {MaterialModule} from "framework/material";

//import {BlocklyDialogComponent} from "framework/ui/blockly/blockly-dialog.component";
//import {VariableRenamePromptComponent} from "framework/ui/blockly/variable-rename-prompt.component";
import {BreadcrumbsComponent} from "framework/ui/breadcrumbs/breadcrumbs.component";
//import {AggregationTrendChartComponent} from "framework/ui/charting/aggregation-trend-chart.component";
//import {ChartChipComponent} from "framework/ui/charting/chart-chip.component";
//import {ChartTooltipComponent} from "framework/ui/charting/chart-tooltip.component";
//import {ChartComponent} from "framework/ui/charting/chart.component";
//import {HeatmapComponent} from "framework/ui/charting/heatmap.component";
//import {ScatterPlotComponent} from "framework/ui/charting/scatter-plot.component";
//import {TreeChartComponent} from "framework/ui/charting/tree-chart.component";
import {ColorBuilderComponent} from "framework/ui/colors/color-builder.component";

import {ConsoleInputComponent} from "framework/ui/consoles/console-input.component";
import {ConsoleLogColumnManagerComponent} from "framework/ui/consoles/console-log-column-manager.component";
import {ConsoleLogFilterComponent} from "framework/ui/consoles/console-log-filter.component";
import {ConsoleLogColumnComponent, ConsoleLogComponent, ConsoleLogScrollerDirective, ConsoleLogVirtualScrollComponent} from "framework/ui/consoles/console-log.component";

import {ContextMenu} from "framework/ui/context-menu/context-menu.component";
import {DatatableCellButtonComponent} from "framework/ui/datatables/datatable-cell-button.component";
import {DatatableColumnManagerComponent} from "framework/ui/datatables/datatable-column-manager.component";
// datatables
import {DatatableCellSuffixTemplateDirective, DatatableCellTemplateDirective, DatatableColumnDirective, DatatableComponent, DatatableContextMenuTriggerDirective, DatatableDetailsTemplateDirective, DatatableHeaderCellTemplateDirective} from "framework/ui/datatables/datatable.component";

import {DialogConfirmComponent} from "framework/ui/dialogs/dialog-confirm.component";
import {DialogPromptComponent} from "framework/ui/dialogs/dialog-prompt.component";
import {ImportDialogComponent} from "framework/ui/dialogs/import-dialog.component";
import {OverlayDialogToggleDirective} from "framework/ui/dialogs/overlay-dialog-toggle.directive";
import {FilterableTreeComponent} from "framework/ui/dropdowns/filterable-tree.component";

import {ElapsedFormatPipe, LongDateFormatPipe, LongDateTimeFormatPipe, ShortDateFormatPipe, ShortTimeFormatPipe, TimeFormatPipe} from "framework/ui/formatting/date-format.pipe";
import {KeysOfPipe} from "framework/ui/formatting/keys-of.pipe";
import {PluralizePipe} from "framework/ui/formatting/pluralize.pipe";
import {TitleCaseFormatPipe} from "framework/ui/formatting/string-format.pipe";

import {BoundSelectComponent} from "framework/ui/forms/bound-select.component";
import {CollapsibleFilterButtonComponent} from "framework/ui/forms/collapsible-filter-button.component";
import {DatePickerComponent} from "framework/ui/forms/date-picker.component";
import {OverlayInputComponent} from "framework/ui/forms/overlay-input.component";
import {SelectComponent} from "framework/ui/forms/select.component";
import {StringSetComponent} from "framework/ui/forms/string-set.component";
import {AllEquivalentValuesValidatorDirective, UniqueValuesValidatorDirective} from "framework/ui/forms/validators/unique-values-validator.directive";

import {OverlayComponent} from "framework/ui/overlays/overlay.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";
import {StandardFormComponent} from "framework/ui/overlays/standard-form.component";
import {ChipListOverlayComponent} from "framework/ui/shared/chip-list-overlay.component";
import {FilterChipsContainerComponent} from "framework/ui/shared/filter-chips-container.component";
import {KeyValueItem} from "framework/ui/shared/key-value-item.component";
import {ModifiableTableComponent, ModifiableTableRowDirective} from "framework/ui/shared/modifiable-table.component";
import {SafeImageComponent} from "framework/ui/shared/safe-image.component";
import {SortArrowComponent} from "framework/ui/shared/sort-arrow.component";
import {TooltipWhenTruncatedDirective} from "framework/ui/shared/tooltip-when-truncated.directive";

import {TabActionComponent, TabComponent, TabContentDirective, TabGroupComponent, TabMetaComponent, TabSection, TabSubsectionTitleComponent} from "framework/ui/tab-group/tab-group.component";
import {TestingModule} from "framework/ui/testing/testing.module";
import {ClipboardCopyComponent} from "framework/ui/utils/clipboard-copy.component";

import {WizardSelectionTreeComponent} from "framework/ui/wizards/wizard-selection-tree.component";
import {WizardStepContentTemplateDirective} from "framework/ui/wizards/wizard-step-content-template.directive";
import {WizardStepGroupDirective} from "framework/ui/wizards/wizard-step-group.directive";
import {WizardComponent} from "framework/ui/wizards/wizard.component";

@NgModule({
              declarations: [
                  //AggregationTrendChartComponent,
                  AutofocusDirective,
                  //BlocklyDialogComponent,
                  BoundSelectComponent,
                  DatatableColumnManagerComponent,
                  BreadcrumbsComponent,
                  //ChartChipComponent,
                  //ChartComponent,
                  //ChartTooltipComponent,
                  ChipListOverlayComponent,
                  CidrDirective,
                  CidrRequiredDirective,
                  ClipboardCopyComponent,
                  ColorBuilderComponent,
                  CollapsibleFilterButtonComponent,
                  ConsoleInputComponent,
                  ConsoleLogComponent,
                  ConsoleLogColumnComponent,
                  ConsoleLogColumnManagerComponent,
                  ConsoleLogFilterComponent,
                  ConsoleLogScrollerDirective,
                  ConsoleLogVirtualScrollComponent,
                  ContextMenu,
                  DatatableCellSuffixTemplateDirective,
                  DatatableCellTemplateDirective,
                  DatatableColumnDirective,
                  DatatableComponent,
                  DatatableCellButtonComponent,
                  DatatableContextMenuTriggerDirective,
                  DatatableDetailsTemplateDirective,
                  DatatableHeaderCellTemplateDirective,
                  DatePickerComponent,
                  DialogConfirmComponent,
                  DialogPromptComponent,
                  ElapsedFormatPipe,
                  ExpandDirective,
                  FilterableTreeComponent,
                  FilterChipsContainerComponent,
                  //HeatmapComponent,
                  HeightBreakpointDirective,
                  ImportDialogComponent,
                  IpAddressDirective,
                  IpAddressRequiredDirective,
                  IpAddressWithPortDirective,
                  IpAddressWithPortRequiredDirective,
                  KeysOfPipe,
                  KeyValueItem,
                  LongDateFormatPipe,
                  LongDateTimeFormatPipe,
                  MinWidthDirective,
                  ModifiableTableComponent,
                  ModifiableTableRowDirective,
                  AllEquivalentValuesValidatorDirective,
                  OverlayComponent,
                  OverlayDialogToggleDirective,
                  OverlayInputComponent,
                  //PanZoomDirective,
                  PersistTabDirective,
                  PluralizePipe,
                  SafeImageComponent,
                  SelectComponent,
                  //ScatterPlotComponent,
                  ShortDateFormatPipe,
                  ShortTimeFormatPipe,
                  SortArrowComponent,
                  StandardFormComponent,
                  StandardFormOverlayComponent,
                  StickyDirective,
                  StringSetComponent,
                  TabActionComponent,
                  TabComponent,
                  TabContentDirective,
                  TabGroupComponent,
                  TabMetaComponent,
                  TabSection,
                  TabSubsectionTitleComponent,
                  TimeFormatPipe,
                  TitleCaseFormatPipe,
                  TooltipWhenTruncatedDirective,
                  //TreeChartComponent,
                  UniqueValuesValidatorDirective,
                  //VariableRenamePromptComponent,
                  WizardComponent,
                  WizardSelectionTreeComponent,
                  WizardStepContentTemplateDirective,
                  WizardStepGroupDirective
              ],
              imports     : [
                  CommonModule,
                  FormsModule,
                  MaterialModule,
                  CdkModule,
                  TestingModule,
                  TreeModule
              ],
              providers   : [],
              exports     : [
                  MaterialModule,
                  CdkModule,
                  FormsModule,
                  //AggregationTrendChartComponent,
                  AutofocusDirective,
                  BoundSelectComponent,
                  DatatableColumnManagerComponent,
                  BreadcrumbsComponent,
                  //ChartChipComponent,
                  //ChartComponent,
                  //ChartTooltipComponent,
                  ChipListOverlayComponent,
                  CidrDirective,
                  CidrRequiredDirective,
                  ClipboardCopyComponent,
                  CollapsibleFilterButtonComponent,
                  ColorBuilderComponent,
                  ConsoleInputComponent,
                  ConsoleLogComponent,
                  ConsoleLogColumnComponent,
                  ConsoleLogScrollerDirective,
                  ConsoleLogVirtualScrollComponent,
                  ContextMenu,
                  DatatableCellSuffixTemplateDirective,
                  DatatableCellTemplateDirective,
                  DatatableColumnDirective,
                  DatatableComponent,
                  DatatableCellButtonComponent,
                  DatatableContextMenuTriggerDirective,
                  DatatableDetailsTemplateDirective,
                  DatatableHeaderCellTemplateDirective,
                  DatePickerComponent,
                  DialogConfirmComponent,
                  DialogPromptComponent,
                  ElapsedFormatPipe,
                  ExpandDirective,
                  FilterableTreeComponent,
                  FilterChipsContainerComponent,
                  //HeatmapComponent,
                  HeightBreakpointDirective,
                  ImportDialogComponent,
                  IpAddressDirective,
                  IpAddressRequiredDirective,
                  IpAddressWithPortDirective,
                  IpAddressWithPortRequiredDirective,
                  KeysOfPipe,
                  KeyValueItem,
                  LongDateFormatPipe,
                  LongDateTimeFormatPipe,
                  //PanZoomDirective,
                  MinWidthDirective,
                  ModifiableTableComponent,
                  ModifiableTableRowDirective,
                  AllEquivalentValuesValidatorDirective,
                  OverlayComponent,
                  OverlayDialogToggleDirective,
                  OverlayInputComponent,
                  PersistTabDirective,
                  PluralizePipe,
                  SafeImageComponent,
                  //ScatterPlotComponent,
                  SelectComponent,
                  ShortDateFormatPipe,
                  ShortTimeFormatPipe,
                  SortArrowComponent,
                  StandardFormComponent,
                  StandardFormOverlayComponent,
                  StickyDirective,
                  StringSetComponent,
                  TabActionComponent,
                  TabComponent,
                  TabContentDirective,
                  TabGroupComponent,
                  TabMetaComponent,
                  TabSection,
                  TabSubsectionTitleComponent,
                  TestingModule,
                  TimeFormatPipe,
                  TitleCaseFormatPipe,
                  TooltipWhenTruncatedDirective,
                  //TreeChartComponent,
                  UniqueValuesValidatorDirective,
                  //VariableRenamePromptComponent,
                  WizardComponent,
                  WizardSelectionTreeComponent,
                  WizardStepContentTemplateDirective,
                  WizardStepGroupDirective
              ]
          })
export class FrameworkUIModule {}
