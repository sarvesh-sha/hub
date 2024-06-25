import {Component, ElementRef, ViewChild} from "@angular/core";

import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import * as Models from "app/services/proxy/model/models";

import {ControlOption} from "framework/ui/control-option";
import {SelectComponent} from "framework/ui/forms/select.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-name-description-step",
               templateUrl: "./widget-editor-wizard-name-description-step.component.html",
               providers  : [WizardStep.createProvider(WidgetEditorWizardNameDescriptionStepComponent)]
           })
export class WidgetEditorWizardNameDescriptionStepComponent extends WizardStep<WidgetEditorWizardState>
{
    get toolbarBehavior(): Models.WidgetToolbarBehavior
    {
        if (!this.data.editor.widget.toolbarBehavior)
        {
            return this.data.editor.forSubgroup ? Models.WidgetToolbarBehavior.AutoHide : Models.WidgetToolbarBehavior.AlwaysShow;
        }
        return this.data.editor.widget.toolbarBehavior;
    }

    set toolbarBehavior(behavior: Models.WidgetToolbarBehavior)
    {
        this.data.editor.widget.toolbarBehavior = behavior;
    }

    @ViewChild("test_nameInput", {read: ElementRef}) test_nameInput: ElementRef;
    @ViewChild("test_toolbarBehavior") test_toolbarBehavior: SelectComponent<Models.WidgetToolbarBehavior>;

    public toolbarBehaviorOptions = [
        new ControlOption(Models.WidgetToolbarBehavior.AlwaysShow, "Always show toolbar"),
        new ControlOption(Models.WidgetToolbarBehavior.AutoHide, "Auto hide toolbar"),
        new ControlOption(Models.WidgetToolbarBehavior.Collapsible, "Collapsible widget"),
        new ControlOption(Models.WidgetToolbarBehavior.Hide, "Always hide toolbar")
    ];

    public getLabel() { return "Name & Description"; }

    public isEnabled()
    {
        return true;
    }

    public isValid()
    {
        return this.data.editor.forSubgroup || !!this.data.editor.widget.name;
    }

    public isNextJumpable()
    {
        return true;
    }

    public async onNext()
    {
        return false;
    }

    public async onStepSelected()
    {

    }

    public async onData()
    {
    }
}
