import {ComponentType} from "@angular/cdk/portal";
import {Directive, Inject, Injector, ViewChild} from "@angular/core";

import {BaseApplicationComponent} from "app/services/domain/base.service";

import {BaseComponent} from "framework/ui/components";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";
import {WizardComponent} from "framework/ui/wizards/wizard.component";

@Directive()
export abstract class WizardDialogComponent<T extends WizardDialogState> extends BaseComponent
{
    private m_wizard: WizardComponent<T>;
    @ViewChild(WizardComponent, {static: true}) set wizard(wizard: WizardComponent<T>)
    {
        this.m_wizard = wizard;
        this.m_wizard.stepsLoaded.then(() =>
                                       {
                                           if (this.data.step) this.m_wizard.goToStep(this.data.step);
                                       });
    }

    get wizard(): WizardComponent<T>
    {
        return this.m_wizard;
    }

    constructor(public dialogRef: OverlayDialogRef<boolean>,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: T)
    {
        super(inj);
    }

    public static async open<T extends WizardDialogState>(cfg: T,
                                                          base: BaseApplicationComponent,
                                                          ref: ComponentType<any>,
                                                          gotoOnCreate: boolean = true): Promise<boolean>
    {
        // Always load state before opening
        let loaded = await cfg.load(base);
        if (!loaded) return false;

        // Open a dialog with the loaded state
        return OverlayComponent.open<T, boolean>(base, ref, {
                                   data  : cfg,
                                   config: OverlayConfig.wizard()
                               })
                               .then(async (result: boolean) =>
                                     {
                                         // If committing, save or create
                                         let success = false;
                                         if (result)
                                         {
                                             success = cfg.isNew ? await cfg.create(base, gotoOnCreate) : await cfg.save(base);
                                         }

                                         // Pass through initial result plus save/create success state
                                         return result && success;
                                     });
    }

    wizardCancel()
    {
        this.dialogRef.close(false);
    }

    wizardCommit()
    {
        this.dialogRef.close(true);
    }

    public ngOnDestroy()
    {
        super.ngOnDestroy();

        this.data?.cleanUp();
    }
}


export abstract class WizardDialogState
{
    constructor(public readonly isNew?: boolean,
                public readonly step?: string)
    {}

    abstract create(comp: BaseApplicationComponent,
                    goto: boolean): Promise<boolean>;

    abstract save(comp: BaseApplicationComponent): Promise<boolean>;

    abstract load(comp: BaseApplicationComponent): Promise<boolean>;

    cleanUp(): void
    {
    }
}
