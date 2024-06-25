import {Component, Inject, Injector} from "@angular/core";

import {RegistryImageExtended} from "app/services/domain/registry-images.service";

import * as Models from "app/services/proxy/model/models";
import {BaseComponent} from "framework/ui/components";

import {BaseDialogComponentSingleSelect, BaseDialogConfig, BaseDialogSelection} from "framework/ui/dialogs/base.dialog";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";

@Component({
               templateUrl: "./test-configuration-selection-dialog.component.html"
           })
export class TestConfigurationDialogComponent extends BaseDialogComponentSingleSelect<TestConfigurationDialogComponent, TestConfiguration>
{
    result: TestConfiguration;

    constructor(dialogRef: OverlayDialogRef<TestConfiguration>,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: DialogConfig)
    {
        super(dialogRef, inj);

        this.result = new TestConfiguration();
    }

    public static open(comp: BaseComponent,
                       image: RegistryImageExtended,
                       purposeText: string,
                       okButton: string): Promise<TestConfiguration>
    {
        let cfg            = new DialogConfig();
        cfg.image          = image;
        cfg.dialogPurpose  = purposeText;
        cfg.dialogOkButton = okButton;

        return BaseDialogComponentSingleSelect.openInner(comp, TestConfigurationDialogComponent, cfg);
    }

    protected async loadItems()
    {
        this.addNewItem(this.result);

        this.selectItem(0);
    }

    protected async onEmptyFilterResults()
    {
    }

    protected shouldDisplay(pattern: string,
                            item: TestConfiguration): boolean
    {
        return false;
    }

}

class DialogConfig extends BaseDialogConfig
{
    image: RegistryImageExtended;
}

export class TestConfiguration extends BaseDialogSelection
{
    cfg: Models.DeploymentTaskConfiguration = new Models.DeploymentTaskConfiguration();
}
