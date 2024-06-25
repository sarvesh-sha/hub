import {Component, Injector, ViewChild} from "@angular/core";
import {DeploymentHostImagePullsListComponent, ImageSelection} from "app/customer/deployment-hosts/deployment-host-image-pulls-list.component";

import * as SharedSvc from "app/services/domain/base.service";
import {DeploymentHostImagePullExtended} from "app/services/domain/deployment-host-image-pulls.service";
import * as Models from "app/services/proxy/model/models";
import {ConsoleLogComponent} from "framework/ui/consoles/console-log.component";
import {DatatableContextMenuEvent} from "framework/ui/datatables/datatable.component";

@Component({
               selector   : "o3-statistics-image-pulls-summary-page",
               templateUrl: "./statistics-image-pulls-summary-page.component.html"
           })
export class StatisticsImagePullsSummaryPageComponent extends SharedSvc.BaseApplicationComponent
{
    private imagePullSelected: DeploymentHostImagePullExtended;
    private imagePullSub: SharedSvc.DbChangeSubscription<Models.DeploymentHostImagePull>;

    @ViewChild("imagePulls", {static: true}) imagePullsList: DeploymentHostImagePullsListComponent;
    @ViewChild("imagePullLog", {static: true}) imagePullLog: ConsoleLogComponent;

    imagePullLogLockScroll: boolean;

    constructor(inj: Injector)
    {
        super(inj);
    }

    public ngAfterViewInit(): void
    {
        super.ngAfterViewInit();

        DeploymentHostImagePullExtended.bindToLog(this.imagePullLog, () => this.imagePullSelected);

        this.fetchImagePulls();

        this.subscribeAny(this.app.domain.deploymentHostImagePulls, async (action) =>
        {
            await this.fetchImagePulls();
        });
    }

    async fetchImagePulls()
    {
        if (this.imagePullSelected)
        {
            this.selectImagePull(await this.imagePullSelected.refreshIfNeeded());
            this.detectChanges();
        }
    }

    selectImagePull(imageExt: DeploymentHostImagePullExtended)
    {
        let currentSysId = this.imagePullSelected?.model?.sysId;
        let newSysId     = imageExt?.model?.sysId;

        this.imagePullSelected = imageExt;

        if (currentSysId != newSysId)
        {
            this.imagePullLog.reset();
        }

        this.refreshImagePullLog();
    }

    private refreshImagePullLog()
    {
        this.imagePullLog.refresh(this.imagePullSelected?.model?.status == Models.JobStatus.EXECUTING);

        if (this.imagePullSub)
        {
            this.imagePullSub.unsubscribe();
            this.imagePullSub = undefined;
        }

        if (this.imagePullSelected)
        {
            this.imagePullSub = this.subscribeOneShot(this.imagePullSelected, async () =>
            {
                this.imagePullSelected = await this.imagePullSelected.refreshIfNeeded();
                this.refreshImagePullLog();
            });
        }
    }

    selectImagePullMenu(event: DatatableContextMenuEvent<ImageSelection>)
    {
        let imageExt = event.row?.singleSelect;
        if (imageExt)
        {
            event.root.addItem("Discard", async () =>
            {
                if (await this.confirmOperation("Click Yes to forget this image pull."))
                {
                    await imageExt.forget();

                    if (this.imagePullSelected == imageExt)
                    {
                        this.selectImagePull(null);
                    }

                    this.imagePullsList.table.refreshData();
                }
            });
        }

        let imageExts = event.row?.multiSelect;
        if (imageExts)
        {
            event.root.addItem("Discard Selected", async () =>
            {
                if (await this.confirmOperation("Click Yes to forget all selected image pulls."))
                {
                    for (let imageExt of imageExts)
                    {
                        await imageExt.forget();

                        if (this.imagePullSelected == imageExt)
                        {
                            this.selectImagePull(null);
                        }

                        this.imagePullsList.table.refreshData();
                    }
                }
            });
        }
    }
}
