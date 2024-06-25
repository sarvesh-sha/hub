import {Component, ViewChild} from "@angular/core";

import {WidgetEditorWizardWidgetPreviewStep} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-widget-preview-step.directive";
import * as Models from "app/services/proxy/model/models";
import {CenteredVerticalHorizontalPlacement, getVerticalHorizontalPlacement, PlacementOptions, VerticalHorizontalPlacement} from "app/shared/options/placement-options";

import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {FileReadType, ImportDialogComponent} from "framework/ui/dialogs/import-dialog.component";
import {SelectComponent} from "framework/ui/forms/select.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-image-step",
               templateUrl: "./widget-editor-wizard-image-step.component.html",
               styleUrls  : [
                   "./widget-editor-wizard-dialog.component.scss",
                   "./widget-editor-wizard-image-step.component.scss"
               ],
               providers  : [WizardStep.createProvider(WidgetEditorWizardImageStepComponent)]
           })
export class WidgetEditorWizardImageStepComponent extends WidgetEditorWizardWidgetPreviewStep<Models.ImageWidgetConfiguration>
{
    readonly placementOptions: ControlOption<VerticalHorizontalPlacement>[] = PlacementOptions;

    get typedWidget(): Models.ImageWidgetConfiguration
    {
        return UtilsService.asTyped(this.data.editor.widget, Models.ImageWidgetConfiguration);
    }

    get imgBase64(): string
    {
        return this.typedWidget.image.logoBase64;
    }

    set imgBase64(img: string)
    {
        this.updateWidget((widget) => widget.image.logoBase64 = img);
    }

    get backgroundColor(): string
    {
        return this.typedWidget.image.primaryColor || "#ffffff";
    }

    set backgroundColor(color: string)
    {
        this.updateWidget((widget) => widget.image.primaryColor = color);
    }

    private m_placement: VerticalHorizontalPlacement;
    get placement(): VerticalHorizontalPlacement
    {
        if (!this.m_placement)
        {
            this.m_placement = getVerticalHorizontalPlacement(this.verticalPlacement, this.horizontalPlacement) || CenteredVerticalHorizontalPlacement;
        }
        return this.m_placement;
    }

    set placement(placement: VerticalHorizontalPlacement)
    {
        this.m_placement = placement;

        const verticalAlignment   = this.placement.vertical;
        const horizontalAlignment = this.placement.horizontal;

        this.updateWidget((widget) => widget.image.verticalPlacement = verticalAlignment);
        this.updateWidget((widget) => widget.image.horizontalPlacement = horizontalAlignment);
    }

    private get verticalPlacement(): Models.VerticalAlignment
    {
        return this.typedWidget.image.verticalPlacement;
    }

    private get horizontalPlacement(): Models.HorizontalAlignment
    {
        return this.typedWidget.image.horizontalPlacement;
    }

    @ViewChild("test_placement") test_placement: SelectComponent<VerticalHorizontalPlacement>;

    protected updatePreviewConfig()
    {
        this.previewConfig = Models.ImageWidgetConfiguration.newInstance(this.previewConfig);
    }

    public getLabel(): string
    {
        return "Image";
    }

    public isEnabled(): boolean
    {
        return !!this.typedWidget;
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public isValid(): boolean
    {
        let widget = this.typedWidget;
        if (!widget?.image) return false;
        return !!widget.image.logoBase64;
    }

    public onNext(): Promise<boolean>
    {
        return undefined;
    }

    public async onStepSelected()
    {
        this.initialize();

        this.widgetPreview?.refresh();
    }

    public async onData(): Promise<any>
    {
        await super.onData();

        this.initialize();
    }

    async uploadLogo()
    {
        this.imgBase64 = await ImportDialogComponent.open(this, "Import Logo", {
            returnRawBlobs: () => false,
            parseFile     : async (image: string) => typeof image === "string" ? image : null
        }, FileReadType.asDataURL);
    }

    private initialize()
    {
        let widget = this.typedWidget;
        if (widget && !this.previewConfig)
        {
            this.previewConfig = Models.ImageWidgetConfiguration.deepClone(widget);
        }
    }
}
