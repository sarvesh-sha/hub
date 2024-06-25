import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {DocumentTableComponent} from "app/customer/engines/normalization/document-table.component";
import {SubnetTableComponent} from "app/customer/engines/normalization/subnet-table.component";
import {TermFrequencyInverseDocumentFrequencyConfigurationComponent} from "app/customer/engines/normalization/term-frequency-inverse-document-frequency-configuration.component";
import {EngineTestResultsComponent} from "app/customer/engines/shared/engine-test-results.component";
import {EquipmentClassAssignmentTableComponent} from "app/customer/engines/shared/equipment-class-assignment-table.component";
import {ExportableStringSetComponent} from "app/customer/engines/shared/exportable-string-set.component";
import {LookupTableComponent} from "app/customer/engines/shared/lookup-table.component";
import {PointClassAssignmentTableComponent} from "app/customer/engines/shared/point-class-assignment-table.component";
import {RegexReplaceTableComponent} from "app/customer/engines/shared/regex-replace-table.component";
import {UnitEditorWrapperComponent} from "app/customer/engines/shared/unit-editor-wrapper.component";
import {AppBlocklyWorkspaceComponent} from "app/customer/engines/shared/workspace.component";
import {TablesModule} from "app/shared/tables/tables.module";
import {UnitEditorModule} from "app/shared/units/unit-editor.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  AppBlocklyWorkspaceComponent,
                  DocumentTableComponent,
                  EngineTestResultsComponent,
                  EquipmentClassAssignmentTableComponent,
                  ExportableStringSetComponent,
                  LookupTableComponent,
                  PointClassAssignmentTableComponent,
                  RegexReplaceTableComponent,
                  SubnetTableComponent,
                  TermFrequencyInverseDocumentFrequencyConfigurationComponent,
                  UnitEditorWrapperComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  TablesModule,
                  UnitEditorModule
              ],
              exports     : [
                  AppBlocklyWorkspaceComponent,

                  EngineTestResultsComponent
              ]
          })
export class EnginesModule {}
