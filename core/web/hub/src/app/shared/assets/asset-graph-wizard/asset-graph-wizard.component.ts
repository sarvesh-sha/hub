import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output, ViewChild} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {WizardComponent} from "framework/ui/wizards/wizard.component";

@Component({
               selector       : "o3-asset-graph-wizard",
               templateUrl    : "./asset-graph-wizard.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class AssetGraphWizardComponent extends SharedSvc.BaseApplicationComponent
{
    @Input() model: AssetGraphWizardState = new AssetGraphWizardState();

    @Output() cancelled   = new EventEmitter<void>();
    @Output() modelChange = new EventEmitter<AssetGraphWizardState>();

    @ViewChild(WizardComponent, {static: true}) wizard: WizardComponent<any>;

    public wizardCancel()
    {
        this.cancelled.emit();
    }

    public wizardCommit()
    {
        this.modelChange.emit(this.model);
        this.wizardCancel();
    }
}

export class AssetGraphWizardState
{
    graph: Models.TimeSeriesGraphConfiguration = new Models.TimeSeriesGraphConfiguration();
    graphSelections: string[];
}
