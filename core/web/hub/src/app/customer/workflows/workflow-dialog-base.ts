import {ComponentType} from "@angular/cdk/overlay";
import {Directive, Injector, Type} from "@angular/core";
import {AssetExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {DatatableManager, DatatableSelectionManager} from "framework/ui/datatables/datatable-manager";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

@Directive()
export class WorkflowDialogBase extends BaseApplicationComponent
{
    constructor(inj: Injector)
    {
        super(inj);
    }

    protected static async openDialog<T extends AssetExtended>(type: Type<T>,
                                                               comp: BaseApplicationComponent,
                                                               dialogClass: ComponentType<any>,
                                                               clickedId: string,
                                                               table: DatatableManager<Models.RecordIdentity, any, any>): Promise<boolean>
    {
        let cfg           = await this.getConfig<T>(type, comp, clickedId, table);
        let overlayConfig = this.getOverlayConfig();

        let success = await OverlayComponent.open<any, boolean>(comp, dialogClass,
                                                                {
                                                                    data  : cfg,
                                                                    config: overlayConfig
                                                                });

        if (success && table.selectionManager)
        {
            table.selectionManager.checkAllItems(false);
        }

        return success;
    }

    public static async getConfig<T extends AssetExtended>(type: Type<T>,
                                                           comp: BaseApplicationComponent,
                                                           clickedId: string,
                                                           table: DatatableManager<Models.RecordIdentity, any, string>)
    {
        let selectionManager = table.selectionManager;
        if (!selectionManager) return null;

        DatatableSelectionManager.ensureSelection(selectionManager, clickedId);

        let selectionIds = selectionManager.selection;
        let identities   = table.slice(0, table.count)
                                .filter((r) => selectionIds.has(r.sysId));
        let assets       = await comp.app.domain.assets.getTypedExtendedBatch(type, identities);
        let config       = new WorkflowDialogConfig<T>();
        config.assets    = assets;

        return config;
    }

    public static getOverlayConfig(): OverlayConfig
    {
        return OverlayConfig.newInstance({width: "400px"});
    }
}

export class WorkflowDialogConfig<T extends AssetExtended>
{
    assets: T[];
}
