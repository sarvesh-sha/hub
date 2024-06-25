import {Component, ContentChildren, EventEmitter, Input, Output, QueryList, ViewChild} from "@angular/core";
import {NgForm, NgModel} from "@angular/forms";

import {SafeHtml} from "@angular/platform-browser";
import {VerticalViewWindow} from "framework/ui/charting/vertical-view-window";

import {TabActionComponent, TabActionPriority} from "framework/ui/tab-group/tab-group.component";

@Component({
               selector   : "o3-standard-form[label]",
               templateUrl: "./standard-form.component.html"
           })
export class StandardFormComponent
{
    @Input() label: string;

    @Input() text: string | SafeHtml;

    @Input() cardClass: string;

    @Input() hideActions: boolean = false;

    @Input() actions: TabActionComponent[] = [];

    @Input() showPrimary: boolean           = true;
    @Input() primaryButtonText: string      = "Save";
    @Input() primaryButtonTooltip: string;
    @Input() primaryButtonDisabled: boolean = false;
    @Input() closeOnPrimaryPress: boolean   = true;

    @Input() secondaryButtonText: string      = "Cancel";
    @Input() showSecondary: boolean           = true;
    @Input() secondaryButtonDisabled: boolean = false;

    @Input() extraText: string      = "";
    @Input() showExtraText: boolean = false;

    @Input() extraButtonText: string                = "";
    @Input() showExtraButton: boolean               = false;
    @Input() extraButtonPriority: TabActionPriority = "secondary";
    @Input() extraButtonDisabled: boolean           = false;

    @Input() maxHeight: number = 90;
    @Input() maxWidth: number  = 90;

    @Input() overflow: "auto" | "hidden" = "auto";

    @Output() submitted   = new EventEmitter<void>();
    @Output() cancelled   = new EventEmitter<void>();
    @Output() extraButton = new EventEmitter<void>();
    @Output() scrolled = new EventEmitter<VerticalViewWindow>();

    @ViewChild(NgForm, {static: true}) public form: NgForm;

    @ContentChildren(NgModel, {descendants: true})
    public set contentFormControls(contentFormControls: QueryList<NgModel>)
    {
        if (this.m_contentFormControls)
        {
            this.removeControls(this.m_contentFormControls);
            this.m_contentFormControls = null;
        }

        if (contentFormControls)
        {
            this.m_contentFormControls = contentFormControls.toArray();
            this.addControls(this.m_contentFormControls);
        }
    }

    private m_contentFormControls: NgModel[];


    @Input()
    public set formControls(formControls: NgModel[])
    {
        if (this.m_formControls)
        {
            this.removeControls(this.m_formControls);
            this.m_formControls = null;
        }

        if (formControls)
        {
            this.m_formControls = formControls;
            this.addControls(this.m_formControls);
        }
    }

    private m_formControls: NgModel[];

    markAsPristine()
    {
        this.form.form.markAsPristine();
    }

    private addControls(controls: NgModel[])
    {
        for (let control of controls)
        {
            this.form.addControl(control);
        }
    }

    private removeControls(controls: NgModel[])
    {
        for (let control of controls)
        {
            this.form.removeControl(control);
        }
    }
}
