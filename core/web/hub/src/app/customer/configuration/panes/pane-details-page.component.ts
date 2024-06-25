import {Component} from "@angular/core";
import {PaneWizardDialogComponent, PaneWizardState} from "app/customer/configuration/panes/wizard/pane-wizard-dialog.component";
import {BaseComponentWithRouter} from "app/services/domain/base.service";
import {PaneConfigurationExtended} from "app/services/domain/panes.service";
import * as Models from "app/services/proxy/model/models";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";

@Component({
               selector   : "o3-pane-details",
               templateUrl: "./pane-details-page.component.html"
           })
export class PaneDetailsPageComponent extends BaseComponentWithRouter
{
    pane: Models.PaneConfiguration;

    async onNavigationComplete()
    {
        let id                                        = this.getPathParameter("id");
        this.pane                                     = await this.app.domain.panes.getConfig(id);
        this.app.ui.navigation.breadcrumbCurrentLabel = this.pane.name;
    }

    export()
    {
        DownloadDialogComponent.open(this, "Export Pane Configuration", DownloadDialogComponent.fileName("pane"), this.pane);
    }

    async edit()
    {
        let cfg = new PaneWizardState(this.app.domain, await PaneConfigurationExtended.load(this.app.domain, this.pane.id));
        if (await PaneWizardDialogComponent.open(cfg, this))
        {
            this.pane = await this.app.domain.panes.getConfig(cfg.pane.model.id);
        }
    }

    async exit()
    {
        await this.app.ui.navigation.pop();
    }

    async remove()
    {
        await this.app.domain.panes.remove(this.pane.id);
        await this.exit();
    }
}
