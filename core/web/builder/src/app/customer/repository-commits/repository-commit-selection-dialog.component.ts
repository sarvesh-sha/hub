import {Component, Inject, Injector} from "@angular/core";

import {AppDomainContext} from "app/services/domain";
import {RepositoryBranchExtended} from "app/services/domain/repository-branches.service";
import {RepositoryCommitExtended} from "app/services/domain/repository-commits.service";
import {BaseComponent} from "framework/ui/components";
import {BaseDialogComponentSingleSelect, BaseDialogConfig, BaseDialogSelection} from "framework/ui/dialogs/base.dialog";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {Future} from "framework/utils/concurrency";

@Component({
               templateUrl: "./repository-commit-selection-dialog.component.html"
           })
export class RepositoryCommitSelectionDialogComponent extends BaseDialogComponentSingleSelect<RepositoryCommitSelectionDialogComponent, CommitDescriptor>
{
    private domain: AppDomainContext;

    refreshing: boolean;

    constructor(dialogRef: OverlayDialogRef<CommitDescriptor>,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: DialogConfig)
    {
        super(dialogRef, inj);

        this.domain = this.inject(AppDomainContext);
    }

    public static open(comp: BaseComponent,
                       branch: RepositoryBranchExtended,
                       purpose: string,
                       okButton: string): Promise<CommitDescriptor>
    {
        let cfg            = new DialogConfig();
        cfg.branch         = branch;
        cfg.dialogPurpose  = purpose;
        cfg.dialogOkButton = okButton;

        return BaseDialogComponentSingleSelect.openInner(comp, RepositoryCommitSelectionDialogComponent, cfg, false, "60%");
    }

    async loadItems()
    {
        this.startRefresh();

        let head = await this.data.branch.getHead();
        this.convert(head);

        await this.loadMoreItems();
    }

    private async startRefresh()
    {
        this.refreshing = true;
        this.detectChanges();

        try
        {
            let status = await this.domain.repositories.refresh();
            if (status && (status.branchesAdded || status.commitsAdded))
            {
                this.data.branch = await this.data.branch.refresh();
                this.selectedItemIndex = undefined;

                this.loadData();
            }
        }
        finally
        {
            this.refreshing = false;
            this.detectChanges();
        }
    }

    protected async onEmptyFilterResults()
    {
        await Future.delayed(200);
        await this.loadMoreItems();
    }

    protected shouldDisplay(pattern: string,
                            item: CommitDescriptor): boolean
    {
        if (this.containsPattern(pattern, item.commit.model.message)) return true;

        return false;
    }

    //--//

    async loadMoreItems()
    {
        let last = this.getLastItem();
        this.domain.repositoryCommits.logger.debug(`Expanding at ${last.commit.model.commitHash}`);

        for (let commit of await last.commit.getParents(20))
        {
            this.convert(commit);
        }
    }

    private convert(commit: RepositoryCommitExtended)
    {
        let item    = new CommitDescriptor();
        item.branch = this.data.branch;
        item.commit = commit;

        item.shortHash = commit.model.commitHash.substring(0, 7);

        let message = commit.model.message;
        if (message.length > 40)
        {
            message = message.substring(0, 40) + "...";
        }
        item.shortMessage = message;

        this.addNewItem(item);
    }
}

class DialogConfig extends BaseDialogConfig
{
    branch: RepositoryBranchExtended;
}

export class CommitDescriptor extends BaseDialogSelection
{
    branch: RepositoryBranchExtended;

    commit: RepositoryCommitExtended;

    shortHash: string;

    shortMessage: string;
}
