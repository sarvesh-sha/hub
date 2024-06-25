import {CdkPortalOutlet, ComponentPortal, ComponentType} from "@angular/cdk/portal";
import {Component, Inject, OnInit, ViewChild} from "@angular/core";
import {BaseComponent} from "framework/ui/components";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";
import {TabActionDirective} from "framework/ui/shared/tab-action.directive";

@Component({
               selector: "o3-blockly-dialog",
               template: `
                   <o3-standard-form [label]="data.title" [actions]="getActions()" (submitted)="onOkay()" (cancelled)="onCancel()">
                       <div style="position: relative">
                           <ng-template [cdkPortalOutlet]>
                           </ng-template>
                       </div>
                   </o3-standard-form>
               `
           })
export class BlocklyDialogComponent implements OnInit
{
    portal: ComponentPortal<any>;

    @ViewChild(CdkPortalOutlet, {static: true})
    portalOutlet: CdkPortalOutlet;

    private m_instance: any;

    constructor(private dialogRef: OverlayDialogRef<void>,
                @Inject(OVERLAY_DATA) public data: DialogData<any, any>)
    {
        this.portal = new ComponentPortal(this.data.componentType);
    }

    ngOnInit()
    {
        let componentRef = this.portal.attach(this.portalOutlet);
        this.m_instance  = componentRef.instance;
        this.data.manager.initComponent(this.m_instance);
    }

    onOkay()
    {
        this.data.manager.onAccept();
        this.dialogRef.close();
    }

    onCancel()
    {
        this.data.manager.onCancel();
        this.dialogRef.close();
    }

    getActions(): TabActionDirective[]
    {
        return this.data.manager.getActions(this.m_instance);
    }

    static open<S>(comp: BaseComponent,
                   title: string,
                   component: ComponentType<S>,
                   dialogManager: DialogManager<S>)
    {
        let data           = new DialogData();
        data.title         = title;
        data.componentType = component;
        data.manager       = dialogManager;

        return OverlayComponent.open(comp, BlocklyDialogComponent, {
            data  : data,
            config: OverlayConfig.newInstance({containerClasses: ["dialog-lg"]})
        });
    }
}

class DialogData<S, T extends ComponentType<S>>
{
    title: string;
    componentType: T;
    manager: DialogManager<S>;
}

export interface DialogManager<S>
{
    initComponent(component: S): void;

    getActions(component: S): TabActionDirective[];

    onAccept(): void;

    onCancel(): void;
}
