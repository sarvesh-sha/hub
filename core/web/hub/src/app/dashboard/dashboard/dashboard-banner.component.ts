import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output, ViewChild} from "@angular/core";

import {BrandingConfigurationExtended} from "app/dashboard/dashboard/branding-configuration-extended";
import * as SharedSvc from "app/services/domain/base.service";
import {DashboardConfigurationExtended} from "app/services/domain/dashboard-management.service";
import * as Models from "app/services/proxy/model/models";
import {CenteredVerticalHorizontalPlacement, getVerticalHorizontalPlacement, horizontalAlignmentToRelativeLocation, PlacementOptions, verticalAlignmentToRelativeLocation, VerticalHorizontalPlacement} from "app/shared/options/placement-options";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {FileReadType, ImportDialogComponent} from "framework/ui/dialogs/import-dialog.component";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";
import {RelativeLocation, relativeLocationToAlignItemsCss, relativeLocationToJustifyContentCss} from "framework/ui/utils/relative-location-styles";

@Component({
               selector       : "o3-dashboard-banner[cfgExt]",
               templateUrl    : "./dashboard-banner.component.html",
               styleUrls      : ["./dashboard-banner.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class DashboardBannerComponent extends SharedSvc.BaseApplicationComponent
{
    DashboardBannerSegmentExtended = DashboardBannerSegmentExtended;

    private m_cfgExt: DashboardConfigurationExtended;
    @Input() set cfgExt(cfgExt: DashboardConfigurationExtended)
    {
        if (cfgExt)
        {
            let differentBanners = !this.m_cfgExt || !DashboardBannerComponent.equivalentBanners(this.m_cfgExt.model.bannerSegments, cfgExt.model.bannerSegments);
            this.m_cfgExt        = cfgExt;
            if (differentBanners)
            {
                this.resetEditRows();
            }
        }
    }

    private m_editable: boolean;
    @Input() set editable(editable: boolean)
    {
        this.m_editable = editable;
        if (!editable && this.editOverlay.isOpen())
        {
            this.editOverlay.toggleOverlay();
        }
    }

    get editable(): boolean
    {
        return this.m_editable;
    }

    public readonly placementOptions: ControlOption<VerticalHorizontalPlacement>[] = PlacementOptions;

    editRows: DashboardBannerSegmentExtended[];
    pristine: boolean;

    overlayConfig = OverlayConfig.onTopDraggable({
                                                     showCloseButton: false,
                                                     width          : 1000,
                                                     maxWidth       : "95vw"
                                                 });

    @ViewChild(StandardFormOverlayComponent, {static: true}) editOverlay: StandardFormOverlayComponent;

    @Output() updated = new EventEmitter<void>();

    async uploadLogo(segment: DashboardBannerSegmentExtended)
    {
        let logo = await ImportDialogComponent.open(this, "Import Image", {
            returnRawBlobs: () => false,
            parseFile     : async (base64Logo: string) => typeof base64Logo === "string" ? base64Logo : null
        }, FileReadType.asDataURL);
        if (logo && logo !== segment.model.branding.logoBase64)
        {
            segment.logoBase64 = logo;
            this.updatePristineState();
            this.markForCheck();
        }
    }

    editBanner()
    {
        if (this.m_editable)
        {
            this.editOverlay.openOverlay();
        }
    }

    addNewSegment(forText: boolean)
    {
        this.editRows.push(new DashboardBannerSegmentExtended(forText, this.m_cfgExt.model));
        this.editRows = UtilsService.arrayCopy(this.editRows);

        this.updatePristineState();
    }

    updatePristineState()
    {
        this.pristine = DashboardBannerComponent.equivalentBanners(this.editRows.map((edit) => edit.model), this.m_cfgExt.model.bannerSegments);
    }

    private static equivalentBanners(bannerA: Models.DashboardBannerSegment[],
                                     bannerB: Models.DashboardBannerSegment[]): boolean
    {
        if (!bannerA || !bannerB) return false;
        if (bannerA.length !== bannerB.length) return false;
        for (let i = 0; i < bannerA.length; i++)
        {
            let bannerSegmentA = bannerA[i];
            let bannerSegmentB = bannerB[i];

            if (bannerSegmentA.widthRatio !== bannerSegmentB.widthRatio) return false;
            if (!BrandingConfigurationExtended.areEquivalent(bannerSegmentA.branding, bannerSegmentB.branding)) return false;
        }

        return true;
    }

    updateCfgExt()
    {
        this.m_cfgExt.model.bannerSegments = this.editRows.map((editRow) => editRow.model);
        this.updated.emit();
    }

    resetEditRows()
    {
        this.editRows = this.m_cfgExt.model.bannerSegments.map((branding) => DashboardBannerSegmentExtended.fromSegment(branding));
        this.pristine = true;
    }
}

class DashboardBannerSegmentExtended
{
    private static ct: number = 0;

    public readonly id: number = DashboardBannerSegmentExtended.ct++;
    public readonly model: Models.DashboardBannerSegment;

    private m_placement: VerticalHorizontalPlacement;
    set placement(placement: VerticalHorizontalPlacement)
    {
        this.m_placement                        = placement || CenteredVerticalHorizontalPlacement;
        this.model.branding.verticalPlacement   = this.m_placement.vertical;
        this.model.branding.horizontalPlacement = this.m_placement.horizontal;
    }

    get placement(): VerticalHorizontalPlacement
    {
        return this.m_placement;
    }

    get horizontalAlignment(): RelativeLocation
    {
        return horizontalAlignmentToRelativeLocation(this.model.branding.horizontalPlacement);
    }

    get verticalAlignment(): RelativeLocation
    {
        return verticalAlignmentToRelativeLocation(this.model.branding.verticalPlacement);
    }

    get widthRatio(): number
    {
        return this.model.widthRatio;
    }

    set widthRatio(ratio: number)
    {
        this.model.widthRatio = UtilsService.clamp(1, 99, Math.round(ratio));
    }

    get logoBase64(): string
    {
        return this.model.branding.logoBase64;
    }

    set logoBase64(image: string)
    {
        this.model.branding.logoBase64 = image;
    }

    get styles(): Lookup<number | string>
    {
        return {
            "background-color": this.model.branding.primaryColor,
            "color"           : this.model.branding.secondaryColor,
            "justify-content" : relativeLocationToJustifyContentCss(this.horizontalAlignment, "center"),
            "align-items"     : relativeLocationToAlignItemsCss(this.verticalAlignment, "center"),
            "flex-grow"       : this.model.widthRatio
        };
    }

    constructor(public readonly forText: boolean,
                containingDashboardModel?: Models.DashboardConfiguration)
    {
        this.model     = Models.DashboardBannerSegment.newInstance({branding: new Models.BrandingConfiguration()});
        this.placement = getVerticalHorizontalPlacement(Models.VerticalAlignment.Middle, Models.HorizontalAlignment.Center);

        if (containingDashboardModel)
        {
            this.model.branding.primaryColor = containingDashboardModel.widgetPrimaryColor;
            if (forText) this.model.branding.secondaryColor = containingDashboardModel.widgetSecondaryColor;

            let cumWidth          = containingDashboardModel.bannerSegments.reduce((cum,
                                                                                    segment) => cum + segment.widthRatio, 0);
            this.model.widthRatio = Math.floor(cumWidth / containingDashboardModel.bannerSegments.length) || 1;
        }
    }

    public static fromSegment(segment: Models.DashboardBannerSegment): DashboardBannerSegmentExtended
    {
        let branding   = segment.branding;
        let segmentExt = new DashboardBannerSegmentExtended(!branding.logoBase64);

        segmentExt.model.widthRatio            = segment.widthRatio;
        segmentExt.model.branding.primaryColor = branding.primaryColor;
        if (segmentExt.forText)
        {
            segmentExt.model.branding.text           = branding.text;
            segmentExt.model.branding.secondaryColor = branding.secondaryColor;
        }
        else
        {
            segmentExt.model.branding.logoBase64 = branding.logoBase64;
        }
        segmentExt.placement = getVerticalHorizontalPlacement(branding.verticalPlacement || Models.VerticalAlignment.Middle,
                                                              branding.horizontalPlacement || Models.HorizontalAlignment.Center);

        return segmentExt;
    }
}
