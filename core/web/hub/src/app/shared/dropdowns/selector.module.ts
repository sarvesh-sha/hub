import {NgModule} from "@angular/core";
import {DeviceSelectorDirective} from "app/shared/dropdowns/device-selector.directive";
import {DigineousDeviceTemplateSelectorDirective} from "app/shared/dropdowns/digineous-device-template-selector.directive";
import {DigineousFlavorSelectorDirective} from "app/shared/dropdowns/digineous-flavor-selector.directive";
import {DigineousMachineSelectorDirective} from "app/shared/dropdowns/digineous-machine-selector.directive";
import {DigineousMachineTemplateSelectorDirective} from "app/shared/dropdowns/digineous-machine-template-selector.directive";
import {DigineousVibrationMonitorSelectorDirective} from "app/shared/dropdowns/digineous-vibration-monitor-selector.directive";
import {EquipmentClassSelectorDirective} from "app/shared/dropdowns/equipment-class-selector.directive";
import {EquipmentSelectorDirective} from "app/shared/dropdowns/equipment-selector.directive";
import {LocationSelectorDirective} from "app/shared/dropdowns/location-selector.directive";
import {MetricsOutputSelectorDirective} from "app/shared/dropdowns/metrics-output-selector.directive";
import {MetricsSelectorDirective} from "app/shared/dropdowns/metrics-selector.directive";
import {PointClassSelectorDirective} from "app/shared/dropdowns/point-class-selector.directive";
import {RelationshipSelectorDirective} from "app/shared/dropdowns/relationship-selector.directive";
import {TagSelectorDirective} from "app/shared/dropdowns/tag-selector.directive";

@NgModule({
              declarations: [
                  DeviceSelectorDirective,
                  DigineousDeviceTemplateSelectorDirective,
                  DigineousFlavorSelectorDirective,
                  DigineousMachineSelectorDirective,
                  DigineousMachineTemplateSelectorDirective,
                  DigineousVibrationMonitorSelectorDirective,
                  EquipmentClassSelectorDirective,
                  EquipmentSelectorDirective,
                  LocationSelectorDirective,
                  MetricsOutputSelectorDirective,
                  MetricsSelectorDirective,
                  PointClassSelectorDirective,
                  RelationshipSelectorDirective,
                  TagSelectorDirective
              ],
              imports     : [],
              exports     : [
                  DeviceSelectorDirective,
                  DigineousDeviceTemplateSelectorDirective,
                  DigineousFlavorSelectorDirective,
                  DigineousMachineSelectorDirective,
                  DigineousMachineTemplateSelectorDirective,
                  DigineousVibrationMonitorSelectorDirective,
                  EquipmentClassSelectorDirective,
                  EquipmentSelectorDirective,
                  LocationSelectorDirective,
                  MetricsOutputSelectorDirective,
                  MetricsSelectorDirective,
                  PointClassSelectorDirective,
                  RelationshipSelectorDirective,
                  TagSelectorDirective
              ]
          })
export class SelectorModule {}
