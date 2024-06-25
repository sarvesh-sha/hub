import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {DayPickerComponent} from "app/shared/forms/day-picker/day-picker.component";

@NgModule({
              declarations: [DayPickerComponent],
              imports     : [CommonModule],
              exports     : [DayPickerComponent]
          })
export class DayPickerModule {}
