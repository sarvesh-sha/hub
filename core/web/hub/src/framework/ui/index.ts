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
import {MACAddressDirective, MACAddressRequiredDirective} from "framework/directives/mac-address.directive";
import {MinWidthDirective} from "framework/directives/min-width.directive";
import {Optio3TestId} from "framework/directives/optio3-test.directive";
import {PanZoomDirective} from "framework/directives/pan-zoom.directive";
import {PasswordMatchDirective, PasswordPolicyDirective} from "framework/directives/password-policy.directive";
import {PersistTabDirective} from "framework/directives/persist-tab.directive";
import {StickyDirective} from "framework/directives/sticky.directive";
import {MaterialModule} from "framework/material";
import {AddToScratchPadComponent} from "framework/ui/blockly/add-to-scratch-pad.component";
import {BlocklyDialogComponent} from "framework/ui/blockly/blockly-dialog.component";
import {ExtractVariableComponent} from "framework/ui/blockly/extract-variable.component";
import {VariableRenamePromptComponent} from "framework/ui/blockly/variable-rename-prompt.component";
import {BreadcrumbsComponent} from "framework/ui/breadcrumbs/breadcrumbs.component";
import {AggregationTrendChartComponent} from "framework/ui/charting/aggregation-trend-chart.component";
import {ChartTimelineComponent} from "framework/ui/charting/chart-timeline.component";
import {ChartTooltipComponent} from "framework/ui/charting/chart-tooltip.component";
import {ChartComponent} from "framework/ui/charting/chart.component";
import {HeatmapComponent} from "framework/ui/charting/heatmap.component";
import {ScatterPlotComponent} from "framework/ui/charting/scatter-plot.component";
import {TreeChartComponent} from "framework/ui/charting/tree-chart.component";
import {ColorBuilderComponent} from "framework/ui/colors/color-builder.component";
import {ConsoleInputComponent} from "framework/ui/consoles/console-input.component";
import {ConsoleLogColumnManagerComponent} from "framework/ui/consoles/console-log-column-manager.component";
import {ConsoleLogFilterComponent} from "framework/ui/consoles/console-log-filter.component";
import {ConsoleLogColumnComponent, ConsoleLogComponent, ConsoleLogScrollerDirective, ConsoleLogVirtualScrollComponent} from "framework/ui/consoles/console-log.component";
import {ContextMenuComponent} from "framework/ui/context-menu/context-menu.component";
import {DatatableCellButtonComponent} from "framework/ui/datatables/datatable-cell-button.component";
import {DatatableColumnManagerComponent} from "framework/ui/datatables/datatable-column-manager.component";
import {DatatableSubtextCellComponent} from "framework/ui/datatables/datatable-subtext-cell.component";
import {DatatableCellSuffixTemplateDirective, DatatableCellTemplateDirective, DatatableColumnDirective, DatatableComponent, DatatableContextMenuTriggerDirective, DatatableDetailsTemplateDirective, DatatableHeaderCellTemplateDirective} from "framework/ui/datatables/datatable.component";
import {DialogConfirmComponent} from "framework/ui/dialogs/dialog-confirm.component";
import {DialogPromptComponent} from "framework/ui/dialogs/dialog-prompt.component";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {ImportDialogComponent} from "framework/ui/dialogs/import-dialog.component";
import {OverlayDialogToggleDirective} from "framework/ui/dialogs/overlay-dialog-toggle.directive";
import {FilterableTreeComponent} from "framework/ui/dropdowns/filterable-tree.component";
import {ElapsedFormatPipe, LongDateFormatPipe, LongDateTimeFormatPipe, ShortDateFormatPipe, ShortTimeFormatPipe, TimeFormatPipe} from "framework/ui/formatting/date-format.pipe";
import {KeysOfPipe} from "framework/ui/formatting/keys-of.pipe";
import {PluralizePipe} from "framework/ui/formatting/pluralize.pipe";
import {CurrencyPipe, NumberWithSeparatorsPipe, TitleCaseFormatPipe} from "framework/ui/formatting/string-format.pipe";
import {BoundSelectComponent} from "framework/ui/forms/bound-select.component";
import {CollapsibleFilterButtonComponent} from "framework/ui/forms/collapsible-filter-button.component";
import {DatePickerComponent} from "framework/ui/forms/date-picker.component";
import {InteractiveErrorStateMatcherDirective} from "framework/ui/forms/interactive-error-state-matcher";
import {OverlayInputComponent} from "framework/ui/forms/overlay-input.component";
import {SelectComponent} from "framework/ui/forms/select.component";
import {StringSetComponent} from "framework/ui/forms/string-set.component";
import {EquivalentValuesValidatorDirective, UniqueValuesValidatorDirective, UniqueValuesWithCallbackValidatorDirective} from "framework/ui/forms/validators/unique-values-validator.directive";
import {ImageModule} from "framework/ui/image/image.module";
import {MenuComponent} from "framework/ui/menu/menu.component";
import {OverlayComponent} from "framework/ui/overlays/overlay.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";
import {StandardFormComponent} from "framework/ui/overlays/standard-form.component";
import {TooltipComponent, TooltipComponentDirective} from "framework/ui/overlays/tooltip.component";
import {ChipListOverlayComponent} from "framework/ui/shared/chip-list-overlay.component";
import {FilterChipsContainerComponent} from "framework/ui/shared/filter-chips-container.component";
import {GridBackgroundComponent} from "framework/ui/shared/grid-background.component";
import {KeyValueItem} from "framework/ui/shared/key-value-item.component";
import {ModifiableTableComponent, ModifiableTableRowDirective} from "framework/ui/shared/modifiable-table.component";
import {ProgressBarComponent} from "framework/ui/shared/progress-bar.component";
import {ProgressIconComponent} from "framework/ui/shared/progress-icon.component";
import {TabActionDirective} from "framework/ui/shared/tab-action.directive";
import {TooltipWhenTruncatedDirective} from "framework/ui/shared/tooltip-when-truncated.directive";
import {TabComponent, TabContentDirective, TabGroupComponent, TabMetaComponent, TabSection, TabSubsectionTitleComponent} from "framework/ui/tab-group/tab-group.component";
import {ClipboardCopyComponent} from "framework/ui/utils/clipboard-copy.component";
import {WizardStepContentTemplateDirective} from "framework/ui/wizards/wizard-step-content-template.directive";
import {WizardComponent} from "framework/ui/wizards/wizard.component";

@NgModule({
              declarations: [
                  AddToScratchPadComponent,
                  AggregationTrendChartComponent,
                  AutofocusDirective,
                  BlocklyDialogComponent,
                  BoundSelectComponent,
                  BreadcrumbsComponent,
                  ChartComponent,
                  ChartTimelineComponent,
                  ChartTooltipComponent,
                  ChipListOverlayComponent,
                  CidrDirective,
                  CidrRequiredDirective,
                  ClipboardCopyComponent,
                  CollapsibleFilterButtonComponent,
                  ColorBuilderComponent,
                  ConsoleInputComponent,
                  ConsoleLogColumnComponent,
                  ConsoleLogColumnManagerComponent,
                  ConsoleLogComponent,
                  ConsoleLogFilterComponent,
                  ConsoleLogScrollerDirective,
                  ConsoleLogVirtualScrollComponent,
                  ContextMenuComponent,
                  CurrencyPipe,
                  DatatableCellButtonComponent,
                  DatatableCellSuffixTemplateDirective,
                  DatatableCellTemplateDirective,
                  DatatableColumnDirective,
                  DatatableColumnManagerComponent,
                  DatatableComponent,
                  DatatableContextMenuTriggerDirective,
                  DatatableDetailsTemplateDirective,
                  DatatableHeaderCellTemplateDirective,
                  DatatableSubtextCellComponent,
                  DatePickerComponent,
                  DialogConfirmComponent,
                  DialogPromptComponent,
                  DownloadDialogComponent,
                  ElapsedFormatPipe,
                  EquivalentValuesValidatorDirective,
                  ExpandDirective,
                  ExtractVariableComponent,
                  FilterChipsContainerComponent,
                  FilterableTreeComponent,
                  GridBackgroundComponent,
                  HeatmapComponent,
                  HeightBreakpointDirective,
                  ImportDialogComponent,
                  InteractiveErrorStateMatcherDirective,
                  IpAddressDirective,
                  IpAddressRequiredDirective,
                  IpAddressWithPortDirective,
                  IpAddressWithPortRequiredDirective,
                  KeyValueItem,
                  KeysOfPipe,
                  LongDateFormatPipe,
                  LongDateTimeFormatPipe,
                  MACAddressDirective,
                  MACAddressRequiredDirective,
                  MenuComponent,
                  MinWidthDirective,
                  ModifiableTableComponent,
                  ModifiableTableRowDirective,
                  Optio3TestId,
                  NumberWithSeparatorsPipe,
                  OverlayComponent,
                  OverlayDialogToggleDirective,
                  OverlayInputComponent,
                  PanZoomDirective,
                  PasswordMatchDirective,
                  PasswordPolicyDirective,
                  PersistTabDirective,
                  PluralizePipe,
                  ProgressBarComponent,
                  ProgressIconComponent,
                  ScatterPlotComponent,
                  SelectComponent,
                  ShortDateFormatPipe,
                  ShortTimeFormatPipe,
                  StandardFormComponent,
                  StandardFormOverlayComponent,
                  StickyDirective,
                  StringSetComponent,
                  TabActionDirective,
                  TabComponent,
                  TabContentDirective,
                  TabGroupComponent,
                  TabMetaComponent,
                  TabSection,
                  TabSubsectionTitleComponent,
                  TimeFormatPipe,
                  TitleCaseFormatPipe,
                  TooltipComponent,
                  TooltipComponentDirective,
                  TooltipWhenTruncatedDirective,
                  TreeChartComponent,
                  UniqueValuesValidatorDirective,
                  UniqueValuesWithCallbackValidatorDirective,
                  VariableRenamePromptComponent,
                  WizardComponent,
                  WizardStepContentTemplateDirective
              ],
              imports     : [
                  CommonModule,
                  FormsModule,
                  ImageModule,
                  MaterialModule,
                  CdkModule,
                  TreeModule
              ],
              providers   : [],
              exports     : [
                  AggregationTrendChartComponent,
                  AutofocusDirective,
                  BoundSelectComponent,
                  BreadcrumbsComponent,
                  CdkModule,
                  ChartComponent,
                  ChartTimelineComponent,
                  ChartTooltipComponent,
                  ChipListOverlayComponent,
                  CidrDirective,
                  CidrRequiredDirective,
                  ClipboardCopyComponent,
                  CollapsibleFilterButtonComponent,
                  ColorBuilderComponent,
                  ConsoleInputComponent,
                  ConsoleLogColumnComponent,
                  ConsoleLogComponent,
                  ConsoleLogScrollerDirective,
                  ConsoleLogVirtualScrollComponent,
                  ContextMenuComponent,
                  CurrencyPipe,
                  DatatableCellButtonComponent,
                  DatatableCellSuffixTemplateDirective,
                  DatatableCellTemplateDirective,
                  DatatableColumnDirective,
                  DatatableColumnManagerComponent,
                  DatatableComponent,
                  DatatableContextMenuTriggerDirective,
                  DatatableDetailsTemplateDirective,
                  DatatableHeaderCellTemplateDirective,
                  DatatableSubtextCellComponent,
                  DatePickerComponent,
                  DialogConfirmComponent,
                  DialogPromptComponent,
                  DownloadDialogComponent,
                  ElapsedFormatPipe,
                  EquivalentValuesValidatorDirective,
                  ExpandDirective,
                  FilterChipsContainerComponent,
                  FilterableTreeComponent,
                  FormsModule,
                  GridBackgroundComponent,
                  HeatmapComponent,
                  HeightBreakpointDirective,
                  ImageModule,
                  ImportDialogComponent,
                  InteractiveErrorStateMatcherDirective,
                  IpAddressDirective,
                  IpAddressRequiredDirective,
                  IpAddressWithPortDirective,
                  IpAddressWithPortRequiredDirective,
                  KeyValueItem,
                  KeysOfPipe,
                  LongDateFormatPipe,
                  LongDateTimeFormatPipe,
                  MACAddressDirective,
                  MACAddressRequiredDirective,
                  MaterialModule,
                  MenuComponent,
                  MinWidthDirective,
                  ModifiableTableComponent,
                  ModifiableTableRowDirective,
                  Optio3TestId,
                  NumberWithSeparatorsPipe,
                  OverlayComponent,
                  OverlayDialogToggleDirective,
                  OverlayInputComponent,
                  PanZoomDirective,
                  PasswordMatchDirective,
                  PasswordPolicyDirective,
                  PersistTabDirective,
                  PluralizePipe,
                  ProgressBarComponent,
                  ProgressIconComponent,
                  ScatterPlotComponent,
                  SelectComponent,
                  ShortDateFormatPipe,
                  ShortTimeFormatPipe,
                  StandardFormComponent,
                  StandardFormOverlayComponent,
                  StickyDirective,
                  StringSetComponent,
                  TabActionDirective,
                  TabComponent,
                  TabContentDirective,
                  TabGroupComponent,
                  TabMetaComponent,
                  TabSection,
                  TabSubsectionTitleComponent,
                  TimeFormatPipe,
                  TitleCaseFormatPipe,
                  TooltipComponentDirective,
                  TooltipWhenTruncatedDirective,
                  TreeChartComponent,
                  UniqueValuesValidatorDirective,
                  UniqueValuesWithCallbackValidatorDirective,
                  VariableRenamePromptComponent,
                  WizardComponent,
                  WizardStepContentTemplateDirective
              ]
          })
export class FrameworkUIModule {}
