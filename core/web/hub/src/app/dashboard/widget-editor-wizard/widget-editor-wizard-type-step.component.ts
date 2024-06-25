import {Component, Injector, ViewChild} from "@angular/core";

import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import {WidgetConfigurationExtended, WidgetTypeConfig, WidgetTypeDefinition} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {ImagePreviewMeta, ImagePreviewTypeMeta} from "app/shared/image/image-preview.component";

import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {SelectComponent} from "framework/ui/forms/select.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-type-step",
               templateUrl: "./widget-editor-wizard-type-step.component.html",
               styleUrls  : [
                   "./widget-editor-wizard-dialog.component.scss",
                   "./widget-editor-wizard-type-step.component.scss"
               ],
               providers  : [WizardStep.createProvider(WidgetEditorWizardTypeStepComponent)]
           })
export class WidgetEditorWizardTypeStepComponent extends WizardStep<WidgetEditorWizardState>
{
    public typeOptions: ControlOption<string>[];
    public typeSelection: string = null;

    public previewMeta: ImagePreviewTypeMeta = null;
    public previews: ImagePreviewMeta[]      = [];

    @ViewChild("test_typeSelector") test_typeSelector: SelectComponent<string>;

    constructor(inj: Injector)
    {
        super(inj);
    }

    public async onData()
    {
        await super.onData();

        if (this.isEnabled())
        {
            const filterFn: (config: WidgetTypeConfig<Models.WidgetConfiguration, WidgetConfigurationExtended<Models.WidgetConfiguration>, WidgetTypeDefinition<Models.WidgetConfiguration>>) => boolean =
                      this.data.editor.forSubgroup ? (config) => config.subgroupCreatable : (config) => config.dashboardCreatable;

            this.typeOptions = WidgetConfigurationExtended.enumerateDescriptors()
                                                          .filter(filterFn)
                                                          .map((config) => new ControlOption(config.typeName, config.friendlyName))
                                                          .sort((a,
                                                                 b) => UtilsService.compareStrings(a.label, b.label, true));

            // Set initial type option
            this.typeSelection = this.data.editor.type;
            this.onSelectionChange(this.typeSelection);
        }
    }

    public getLabel() { return "Type"; }

    public isEnabled()
    {
        return this.data.isNew;
    }

    public isValid()
    {
        return !!this.data.editor;
    }

    public isReady()
    {
        return this.typeOptions && this.previews;
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

    public onSelectionChange(selection: string)
    {
        this.data.editor.type = selection;
        this.data.editor.configure();

        this.previewMeta = WidgetConfigurationExtended.getPreviewMeta(selection);
        this.previews    = this.previewMeta?.examples ? this.previewMeta.examples : [];
    }
}
