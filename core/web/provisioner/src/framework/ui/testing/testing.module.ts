import {NgModule} from "@angular/core";
import {Optio3TestColor} from "framework/ui/testing/optio3-test-color";
import {Optio3TestId} from "framework/ui/testing/optio3-test-id";
import {Optio3TestValue} from "framework/ui/testing/optio3-test-value";

@NgModule({
              declarations: [
                  Optio3TestColor,
                  Optio3TestId,
                  Optio3TestValue
              ],
              exports     : [
                  Optio3TestColor,
                  Optio3TestId,
                  Optio3TestValue
              ]
          })
export class TestingModule {}
