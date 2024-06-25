import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {MatButtonModule} from "@angular/material/button";
import {MatCardModule} from "@angular/material/card";
import {ErrorListPaneComponent} from "framework/ui/errors/error-list-pane.component";

@NgModule({
              declarations: [ErrorListPaneComponent],
              imports     : [
                  CommonModule,
                  MatCardModule,
                  MatButtonModule
              ],
              exports     : [ErrorListPaneComponent]
          })
export class ErrorListPaneModule {}
