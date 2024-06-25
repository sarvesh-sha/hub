import {A11yModule} from "@angular/cdk/a11y";
import {ClipboardModule} from "@angular/cdk/clipboard";
import {DragDropModule} from "@angular/cdk/drag-drop";
import {OverlayModule} from "@angular/cdk/overlay";
import {PortalModule} from "@angular/cdk/portal";
import {NgModule} from "@angular/core";

@NgModule({
              exports: [
                  A11yModule,
                  DragDropModule,
                  ClipboardModule,
                  OverlayModule,
                  PortalModule
              ]
          })
export class CdkModule {}
