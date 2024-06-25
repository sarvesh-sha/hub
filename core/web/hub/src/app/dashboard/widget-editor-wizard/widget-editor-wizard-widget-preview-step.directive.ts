import {Directive, ViewChild} from "@angular/core";

import {WidgetManipulator} from "app/dashboard/dashboard/widgets/widget-manipulator";
import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import {WidgetEditorWizardWidgetPreviewComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-widget-preview.component";
import {WidgetConfigurationExtended} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Directive()
export abstract class WidgetEditorWizardWidgetPreviewStep<T extends Models.WidgetConfiguration> extends WizardStep<WidgetEditorWizardState>
{
    previewConfig: T;

    abstract get typedWidget(): T;

    get fontScalar(): number
    {
        return this.data.editor.parentManipulator.widgetGridConfig.baseFontScalar;
    }

    get colWidth(): number
    {
        return this.data.editor.parentManipulator.colWidth;
    }

    get widgetWidth(): number
    {
        if (this.data.widgetOutline.width) return this.colWidth * this.data.widgetOutline.width;
        if (this.data.editor.parentManipulator.hasParentManipulator) return WidgetManipulator.BASE_SUBWIDGET_WIDTH;

        const width = WidgetConfigurationExtended.fromConfigModel(this.data.editor.widget)
                                                 .getDescriptor().config.defaultWidth;
        return this.colWidth * width;
    }

    get widgetHeight(): number
    {
        if (this.data.widgetOutline.height) return this.data.editor.parentManipulator.rowHeight * this.data.widgetOutline.height;
        if (this.data.editor.parentManipulator.hasParentManipulator) return WidgetManipulator.BASE_SUBWIDGET_HEIGHT;

        return null;
    }

    protected m_widgetPreview: WidgetEditorWizardWidgetPreviewComponent;
    @ViewChild(WidgetEditorWizardWidgetPreviewComponent) set widgetPreview(preview: WidgetEditorWizardWidgetPreviewComponent)
    {
        this.m_widgetPreview = preview;
        this.m_widgetPreview?.refresh();
    }

    protected updateWidget(updateFn: (widget: T) => void)
    {
        let widget = this.typedWidget;
        if (widget && this.previewConfig)
        {
            updateFn(this.previewConfig);
            updateFn(widget);

            this.updatePreviewConfig();
        }
    }

    protected abstract updatePreviewConfig(): void;
}
