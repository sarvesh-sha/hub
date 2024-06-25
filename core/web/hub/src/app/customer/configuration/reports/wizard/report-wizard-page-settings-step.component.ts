import {Component} from "@angular/core";
import {ReportWizardState} from "app/customer/configuration/reports/wizard/report-wizard-dialog.component";
import * as Models from "app/services/proxy/model/models";
import {ControlOption} from "framework/ui/control-option";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-report-page-settings-step",
               templateUrl: "./report-wizard-page-settings-step.component.html",
               providers  : [
                   WizardStep.createProvider(ReportWizardPageSettingsStepComponent)
               ]
           })
export class ReportWizardPageSettingsStepComponent extends WizardStep<ReportWizardState>
{
    public readonly sizeOptions: ControlOption<Models.PaperFormat>[] = [
        new ControlOption<Models.PaperFormat>(Models.PaperFormat.letter, "Letter"),
        new ControlOption<Models.PaperFormat>(Models.PaperFormat.legal, "Legal"),
        new ControlOption<Models.PaperFormat>(Models.PaperFormat.tabloid, "Tabloid"),
        new ControlOption<Models.PaperFormat>(Models.PaperFormat.ledger, "Ledger"),
        new ControlOption<Models.PaperFormat>(Models.PaperFormat.a0, "A0"),
        new ControlOption<Models.PaperFormat>(Models.PaperFormat.a1, "A1"),
        new ControlOption<Models.PaperFormat>(Models.PaperFormat.a2, "A2"),
        new ControlOption<Models.PaperFormat>(Models.PaperFormat.a3, "A3"),
        new ControlOption<Models.PaperFormat>(Models.PaperFormat.a4, "A4"),
        new ControlOption<Models.PaperFormat>(Models.PaperFormat.a5, "A5"),
        new ControlOption<Models.PaperFormat>(Models.PaperFormat.a6, "A6")
    ];

    public getLabel() { return "Page Settings"; }

    public isEnabled(): boolean
    {
        return true;
    }

    public isValid(): boolean
    {
        return true;
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
    }
}
