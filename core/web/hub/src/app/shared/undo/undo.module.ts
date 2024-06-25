import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {RedoButtonDirective, UndoButtonDirective, UndoRedoComponent} from "app/shared/undo/undo-redo.component";

import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  RedoButtonDirective,
                  UndoButtonDirective,
                  UndoRedoComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [
                  RedoButtonDirective,
                  UndoButtonDirective,
                  UndoRedoComponent
              ]
          })
export class UndoModule {}
