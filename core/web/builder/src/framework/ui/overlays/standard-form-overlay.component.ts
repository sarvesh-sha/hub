import {CdkOverlayOrigin} from "@angular/cdk/overlay";
import {CdkPortal} from "@angular/cdk/portal";
import {Component, ContentChild, ContentChildren, EventEmitter, Input, Output, QueryList, ViewChild} from "@angular/core";
import {NgModel} from "@angular/forms";
import {SafeHtml} from "@angular/platform-browser";

import {VerticalViewWindow} from "framework/ui/charting/vertical-view-window";
import {OverlayBase} from "framework/ui/overlays/overlay-base";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";
import {StandardFormComponent} from "framework/ui/overlays/standard-form.component";
import {TabActionDirective, TabActionPriority} from "framework/ui/shared/tab-action.directive";

@Component({
               selector   : "o3-standard-form-overlay[label]",
               templateUrl: "./standard-form-overlay.component.html"
           })
export class StandardFormOverlayComponent extends OverlayBase
{
    @Input() label: string;

    @Input() text: string | SafeHtml;

    @Input() actions: TabActionDirective[] = [];

    @Input() overlayConfig = OverlayConfig.newInstance({
                                                           showCloseButton    : true,
                                                           closableViaBackdrop: false
                                                       });

    @Input() overlayOrigin: CdkOverlayOrigin;

    private m_dialogClass: string;
    @Input() set dialogClass(dialogClass: string)
    {
        if (dialogClass === this.m_dialogClass) return;

        this.overlayConfig.containerClasses = [dialogClass];
    }

    @Input() internalCardClass: string;

    @Input() showPrimary: boolean           = true;
    @Input() primaryButtonText: string      = "Save";
    @Input() primaryButtonDisabled: boolean = false;
    @Input() primaryButtonTooltip: string;
    @Input() primaryButtonConfirmationMessage: string;
    @Input() closeOnPrimaryPress: boolean   = true;

    @Input() showSecondary: boolean                     = false;
    @Input() secondaryButtonText: string                = "Cancel";
    @Input() secondaryButtonDisabled: boolean           = false;
    @Input() confirmSecondaryButton: boolean            = false;
    @Input() secondaryButtonConfirmationMessage: string = "You will lose all your changes.";

    @Input() showExtraButton: boolean               = false;
    @Input() extraButtonText: string;
    @Input() extraButtonPriority: TabActionPriority = "secondary";
    @Input() extraButtonDisabled: boolean           = false;

    @Input() hideActions: boolean = false;

    @ViewChild(OverlayComponent, {static: true}) overlay: OverlayComponent;
    @ViewChild(StandardFormComponent, {static: true}) form: StandardFormComponent;

    get formIsValid(): boolean
    {
        return this.form.form?.valid;
    }

    @Output() submitted          = new EventEmitter<void>();
    @Output() cancelled          = new EventEmitter<void>();
    @Output() closed             = new EventEmitter<void>();
    @Output() extraButtonPressed = new EventEmitter<void>();
    @Output() viewWindowUpdated  = new EventEmitter<VerticalViewWindow>();

    @ContentChildren(NgModel, {descendants: true})
    public set formControls(formControls: QueryList<NgModel>)
    {
        this.m_formControls = formControls?.toArray() ?? [];
    }

    public get formControlsList(): NgModel[]
    {
        return this.m_formControls;
    }

    private m_formControls: NgModel[] = [];

    @ContentChild(CdkPortal, {static: true}) public contentPortal: CdkPortal;

    get portal(): CdkPortal
    {
        return this.isOpen() ? this.contentPortal : null;
    }

    async overlayClosed()
    {
        await this.formSubmitted(false, false);
        this.closed.emit();
    }

    async formSubmitted(fromPrimary: boolean,
                        fromSecondary: boolean)
    {
        if (fromPrimary)
        {
            let confirmed = true;
            if (this.primaryButtonConfirmationMessage)
            {
                confirmed = await this.confirmOperation(this.primaryButtonConfirmationMessage);
            }
            if (!confirmed) return;
            this.submitted.emit();
        }
        else
        {
            if (fromSecondary && this.confirmSecondaryButton)
            {
                let confirmed = await this.confirmOperation(this.secondaryButtonConfirmationMessage);
                if (!confirmed) return;
            }
            this.cancelled.emit();
        }

        if (this.closeOnPrimaryPress || !fromPrimary)
        {
            this.closeOverlay();
        }
    }
}
