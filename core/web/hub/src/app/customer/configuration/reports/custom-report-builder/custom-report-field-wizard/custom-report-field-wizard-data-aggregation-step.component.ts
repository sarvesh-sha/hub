import {Component, ViewChild} from "@angular/core";

import {CustomFieldData} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard.component";
import * as Models from "app/services/proxy/model/models";
import {DataAggregationExtended, DataAggregationType} from "app/shared/aggregation/data-aggregation.component";
import {AggregationNodeBindingConfigurerComponent} from "app/shared/assets/aggregation-node-binding/aggregation-node-binding-configurer.component";
import {ControlPointGroupingStepComponent} from "app/shared/assets/control-point-grouping-step/control-point-grouping-step.component";

import {UtilsService} from "framework/services/utils.service";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-custom-report-field-data-aggregation-step",
               templateUrl: "./custom-report-field-wizard-data-aggregation-step.component.html",
               providers  : [WizardStep.createProvider(CustomReportFieldWizardDataAggregationStepComponent)]
           })
export class CustomReportFieldWizardDataAggregationStepComponent extends WizardStep<CustomFieldData>
{
    get typedElement(): Models.CustomReportElementAggregationTable
    {
        return UtilsService.asTyped(this.data.element, Models.CustomReportElementAggregationTable);
    }

    graphConfigurerOverlayConfig: OverlayConfig = OverlayConfig.wizard({showCloseButton: true});

    @ViewChild(ControlPointGroupingStepComponent) groupings: ControlPointGroupingStepComponent;
    @ViewChild(AggregationNodeBindingConfigurerComponent) aggBindings: AggregationNodeBindingConfigurerComponent;
    @ViewChild(OverlayComponent) graphConfigurer: OverlayComponent;

    private m_hasGraphSub: boolean = false;

    dataAggregationExt: DataAggregationExtended;

    get compareBetweenGroups(): boolean
    {
        return !this.typedElement?.isolateGroupRanges;
    }

    set compareBetweenGroups(compare: boolean)
    {
        let dataAgg = this.typedElement;
        if (dataAgg) dataAgg.isolateGroupRanges = !compare;
    }

    public dataAggregationTypeChanged()
    {
        let dataAggregation = this.typedElement;
        if (dataAggregation)
        {
            if (this.dataAggregationExt.type === DataAggregationType.Bindings) this.attemptGraphSelection();

            dataAggregation.groups      = this.dataAggregationExt.model.groups;
            dataAggregation.columns     = this.dataAggregationExt.model.columns;
            dataAggregation.initialSort = this.dataAggregationExt.model.initialSort;
        }
    }

    public attemptGraphSelection()
    {
        let dataAggregation = this.typedElement;
        if (dataAggregation)
        {
            if (!this.data.graphs.get(dataAggregation.graphId))
            {
                let validGraphOption    = this.data.graphOptions.find((graphOption) =>
                                                                      {
                                                                          let graphExt = this.data.graphs.get(graphOption.id);
                                                                          return graphExt?.hasTreeNodes();
                                                                      });
                dataAggregation.graphId = validGraphOption?.id;
            }
            this.makeGraphSelection();
        }
    }

    public makeGraphSelection()
    {
        let dataAggregation = this.typedElement;
        if (dataAggregation?.graphId)
        {
            this.dataAggregationExt.updateGraph(this.data.graphs.get(dataAggregation.graphId).model);
            dataAggregation.columns = this.dataAggregationExt.model.columns;
        }
    }

    //--//

    public getLabel(): string
    {
        return "Data Aggregation";
    }

    public isEnabled(): boolean
    {
        return !!this.typedElement;
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public isValid(): boolean
    {
        if (!this.wizard.stepForm?.valid) return false;
        return this.dataAggregationExt?.isValid();
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
        let dataAggregation = this.typedElement;
        if (dataAggregation)
        {
            if (!this.m_hasGraphSub)
            {
                this.m_hasGraphSub = true;
                this.subscribeToObservable(this.data.graphsUpdated, () => this.markForCheck());
            }

            this.dataAggregationExt = new DataAggregationExtended({
                                                                      groups                 : dataAggregation.groups,
                                                                      columns                : dataAggregation.columns,
                                                                      graph                  : this.data.graphs.get(dataAggregation.graphId)?.model,
                                                                      filterableRanges       : [Models.FilterableTimeRange.newInstance({range: Models.RangeSelection.deepClone(this.data.range)})],
                                                                      controlPointDisplayType: dataAggregation.controlPointDisplayType,
                                                                      visualizationMode      : dataAggregation.visualizationMode,
                                                                      initialSort            : dataAggregation.initialSort,
                                                                      isolateGroupRanges     : dataAggregation.isolateGroupRanges,
                                                                      visualizationLegend    : true,
                                                                      visualizationRanges    : false
                                                                  }, true);
        }
    }
}
