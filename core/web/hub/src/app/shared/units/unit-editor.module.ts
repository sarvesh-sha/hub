import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {UnitEditorComponent} from "app/shared/units/unit-editor.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [UnitEditorComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [UnitEditorComponent]
          })
export class UnitEditorModule {}
