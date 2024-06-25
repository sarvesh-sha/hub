import {Component, Inject, Injector} from "@angular/core";

import {RepositoryExtended} from "app/services/domain/repositories.service";
import {RepositoryBranchExtended} from "app/services/domain/repository-branches.service";
import {BaseComponent} from "framework/ui/components";
import {BaseDialogComponentSingleSelect, BaseDialogConfig, BaseDialogSelection} from "framework/ui/dialogs/base.dialog";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";

@Component({
               templateUrl: "./repository-branch-selection-dialog.component.html"
           })
export class RepositoryBranchSelectionDialogComponent extends BaseDialogComponentSingleSelect<RepositoryBranchSelectionDialogComponent, BranchDescriptor>
{
    constructor(dialogRef: OverlayDialogRef<BranchDescriptor>,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: DialogConfig)
    {
        super(dialogRef, inj);
    }

    public static open(comp: BaseComponent,
                       repo: RepositoryExtended,
                       purpose: string,
                       okButton: string): Promise<BranchDescriptor>
    {
        let cfg            = new DialogConfig();
        cfg.repo           = repo;
        cfg.dialogPurpose  = purpose;
        cfg.dialogOkButton = okButton;

        return BaseDialogComponentSingleSelect.openInner(comp, RepositoryBranchSelectionDialogComponent, cfg, false);
    }

    async loadItems()
    {
        let branches = await this.data.repo.getBranches();
        for (let branch of branches)
        {
            let item    = new BranchDescriptor();
            item.branch = branch;

            this.addNewItem(item);
        }
    }

    protected async onEmptyFilterResults()
    {
    }

    protected shouldDisplay(pattern: string,
                            item: BranchDescriptor): boolean
    {
        if (this.containsPattern(pattern, item.branch.model.name)) return true;

        return false;
    }
}

class DialogConfig extends BaseDialogConfig
{
    repo: RepositoryExtended;
}

export class BranchDescriptor extends BaseDialogSelection
{
    branch: RepositoryBranchExtended;
}
