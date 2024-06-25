import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {EquivalentUnitsSelectorComponent} from "app/shared/dropdowns/equivalent-units-selector/equivalent-units-selector.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
    declarations: [EquivalentUnitsSelectorComponent],
    imports: [CommonModule, FrameworkUIModule],
    exports: [EquivalentUnitsSelectorComponent]
          })
export class EquivalentUnitsSelectorModule {}
