import {Component, ViewChild} from "@angular/core";
import {AgentsUpgradeSummary} from "app/services/domain/deployment-hosts.service";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               selector   : "o3-deployment-agent-upgrade",
               templateUrl: "./deployment-agent-upgrade-overlay.component.html"
           })
export class DeploymentAgentUpgradeOverlay
{
    public dialogConfig = OverlayConfig.onTopDraggable({
                                                           minWidth : 600,
                                                           maxHeight: "90vh"
                                                       });

    @ViewChild(OverlayComponent, {static: true}) overlay: OverlayComponent;

    public data: AgentsUpgradeSummary;

    get isOpen(): boolean
    {
        return this.overlay && this.overlay.isOpen;
    }

    public open(data: AgentsUpgradeSummary): void
    {
        if (this.overlay && !this.overlay.isOpen)
        {
            this.data = data;

            this.overlay.toggleOverlay();
        }
    }

    public close(): void
    {
        if (this.isOpen)
        {
            this.overlay.toggleOverlay();
        }
    }
}
