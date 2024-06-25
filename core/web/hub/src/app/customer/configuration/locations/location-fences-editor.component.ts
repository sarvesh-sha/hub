import {Component, ElementRef, Inject, Injector, ViewChild} from "@angular/core";

import {ApiService} from "app/services/domain/api.service";
import {LocationExtended} from "app/services/domain/assets.service";
import {AzureMapsService, DistanceFromPolygon, FenceLayerExtended, FenceLayerFeature, MapExtended, PinLayerExtended, PolygonEditingLayerExtended, PolygonEditingLayerFeature} from "app/services/domain/azure-maps.service";
import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {UtilsService} from "framework/services/utils.service";
import {ChartColorUtilities} from "framework/ui/charting/core/colors";
import {BaseComponent} from "framework/ui/components";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               templateUrl: "./location-fences-editor.component.html",
               styleUrls  : ["./location-fences-editor.component.scss"]
           })
export class LocationFencesEditorComponent extends SharedSvc.BaseApplicationComponent
{
    @ViewChild("mapContainer", {static: true}) mapRef: ElementRef;

    hasChanges: boolean;
    editingMode: Action;
    editHelpText: string;

    private m_mapAzure: MapExtended;
    private m_mapAzureReady: Promise<void>;
    private m_pin: PinLayerExtended;
    private m_fence: FenceLayerExtended;
    private m_polygonEditing: PolygonEditingLayerExtended;

    private m_fencesExt: FenceLayerFeature[] = [];

    private m_fenceUnderCreation: FenceLayerFeature;
    private m_newFenceCommittedPoints: number;

    private m_extEdit: PolygonEditingLayerFeature;
    private m_polygonEdit: Models.LocationPolygon;

    constructor(public dialogRef: OverlayDialogRef<boolean>,
                public azureMaps: AzureMapsService,
                public apis: ApiService,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: LocationFencesEditorState)
    {
        super(inj);
    }

    public static open(comp: BaseComponent,
                       extended: LocationExtended,
                       mapLocation: Models.LongitudeLatitude): Promise<boolean>
    {
        let cfg: LocationFencesEditorState = {
            extended   : extended,
            mapLocation: mapLocation
        };

        let overlayConfig = OverlayConfig.newInstance({
                                                          width : "80%",
                                                          height: "80%"
                                                      });
        return OverlayComponent.open(comp, LocationFencesEditorComponent, {
            data  : cfg,
            config: overlayConfig
        });
    }

    async initializeMap()
    {
        this.m_mapAzure = await this.azureMaps.allocateMap(this.mapRef.nativeElement);

        this.m_pin            = new PinLayerExtended(this.m_mapAzure);
        this.m_fence          = new FenceLayerExtended(this.m_mapAzure);
        this.m_polygonEditing = new PolygonEditingLayerExtended(this.m_mapAzure);

        this.m_mapAzure.onEvent([
                                    "click",
                                    "mousemove",
                                    "contextmenu"
                                ], (e) => this.manageMapEvents(e.type, MapExtended.fromPoint(e.position)));

        // Configure map pin and location
        if (this.data.mapLocation)
        {
            this.m_pin.addPoint(this.data.mapLocation, ChartColorUtilities.getColorById("Map Colors", "mapred").hex);

            // Set the map view
            this.m_mapAzure.setCameraPosition(this.data.mapLocation, 15);
        }

        for (let fence of this.data.extended.typedModel.fences || [])
        {
            // make a local copy of the values
            let clonedFence = Models.GeoFence.deepClone(fence);

            if (clonedFence instanceof Models.GeoFenceByPolygon)
            {
                this.m_fencesExt.push(this.m_fence.addFence(clonedFence, ChartColorUtilities.getColorById("Map Colors", "mapyellow").hex));
            }
        }

        this.updateEditState(undefined, undefined);
    }

    async resizeMap()
    {
        if (!this.m_mapAzureReady)
        {
            this.m_mapAzureReady = this.initializeMap();
        }

        await this.m_mapAzureReady;
        this.m_mapAzure.resize();
    }

    wizardCancel()
    {
        this.dialogRef.close(null);
    }

    async wizardCommit()
    {
        this.data.extended.typedModel.fences = this.m_fencesExt.map((ext) => ext.source);
        if (this.data.extended.typedModel.fences.length == 0)
        {
            this.data.extended.typedModel.fences = null;
        }

        this.dialogRef.close(true);
    }

    addFence()
    {
        this.updateEditState(Action.AddFence, "Click to add a new fence or an exclusion to an existing fence");
    }

    removeFence()
    {
        this.updateEditState(Action.DeleteFence, "Click on a fence to delete it");
    }

    resetFences()
    {
        for (let ext of [...this.m_fencesExt])
        {
            this.m_fence.removeFeature(ext);

            let deleteIndex = this.m_fencesExt.indexOf(ext);
            if (deleteIndex >= 0)
            {
                this.m_fencesExt.splice(deleteIndex, 1);
            }

            this.updateEditState(undefined, undefined);
        }
    }

    updateEditState(editingMode: Action,
                    editHelpText: string)
    {
        this.editingMode  = editingMode;
        this.editHelpText = editHelpText;

        this.updateHasChanges();
    }

    updateHasChanges()
    {
        let orig   = this.data.extended.typedModel.fences || [];
        let edited = this.m_fencesExt.map((ext) => ext.source);

        this.hasChanges = !UtilsService.compareJson(orig, edited);
        this.markForCheck();
    }

    //--//

    private manageMapEvents(type: string,
                            target: Models.LongitudeLatitude)
    {
        if (this.m_fenceUnderCreation)
        {
            this.clearPolygonEditing();

            switch (type)
            {
                case "contextmenu":
                    if (this.m_fenceUnderCreation.source.boundary != this.m_polygonEdit)
                    {
                        this.m_fenceUnderCreation.source.innerExclusions.pop();
                    }

                    this.clearEditingState();
                    return;

                case "mousemove":
                    if (this.isNewPointValid(target))
                    {
                        this.m_polygonEdit.points[this.m_newFenceCommittedPoints] = target;
                    }
                    break;

                case  "click":
                    let distance = this.m_mapAzure.distanceInPixels(this.m_polygonEdit.points[0], target);
                    if (distance < 32)
                    {
                        if (this.m_newFenceCommittedPoints > 2)
                        {
                            this.m_polygonEdit.points.pop();

                            if (this.m_fenceUnderCreation.source.boundary == this.m_polygonEdit)
                            {
                                this.m_fencesExt.push(this.m_fenceUnderCreation);
                            }
                        }

                        this.clearEditingState();
                        return;
                    }

                    if (this.isNewPointValid(target))
                    {
                        this.m_polygonEdit.points[this.m_newFenceCommittedPoints++] = target;
                        this.m_polygonEdit.points.push(target);
                    }
                    break;
            }

            this.m_fenceUnderCreation.refresh();

            this.m_extEdit = this.editPolygon(this.m_fenceUnderCreation.source, true, this.m_polygonEdit, (change) =>
            {
                this.m_fenceUnderCreation.refresh();
            });

            return;
        }

        if (type == "click")
        {
            let closest: DistanceFromPolygon = null;
            for (let fenceExt of this.m_fencesExt)
            {
                closest = DistanceFromPolygon.selectNearest(closest, this.m_mapAzure.closestPointToAFence(fenceExt.source, target));
            }

            if (closest)
            {
                if (!this.m_extEdit)
                {
                    switch (this.editingMode)
                    {
                        case Action.DeleteFence:
                            if (MapExtended.isPointInsidePolygon(closest.target, target))
                            {
                                let ext = this.findFenceExtended(closest.fence);
                                if (ext)
                                {
                                    let innerRemoved = false;

                                    let numInner = ext.source.innerExclusions?.length;
                                    for (let pos = 0; pos < numInner; pos++)
                                    {
                                        let inner = ext.source.innerExclusions[pos];
                                        if (MapExtended.isPointInsidePolygon(inner, target))
                                        {
                                            ext.source.innerExclusions.splice(pos, 1);
                                            innerRemoved = true;
                                            break;
                                        }
                                    }

                                    if (!innerRemoved)
                                    {
                                        this.m_fence.removeFeature(ext);

                                        let deleteIndex = this.m_fencesExt.indexOf(ext);
                                        if (deleteIndex >= 0)
                                        {
                                            this.m_fencesExt.splice(deleteIndex, 1);
                                        }
                                    }
                                }
                            }

                            this.updateEditState(undefined, undefined);
                            return;

                        case Action.AddFence:
                            if (MapExtended.isPointInsidePolygon(closest.target, target))
                            {
                                this.createNewExclusion(closest.fence, target);
                            }
                            else
                            {
                                this.createNewFence(target);
                            }

                            this.updateEditState(Action.AddFence, "Click on add points to the fence. To complete a fence, click on the initial point");
                            return;
                    }

                    if (MapExtended.isPointInsidePolygon(closest.target, target))
                    {
                        this.editExistingFence(closest.fence, closest.target);
                        this.updateEditState(Action.EditFence, "Move existing points by dragging them, click on segment to add new points, or right-click to remove them");
                    }
                }
                else
                {
                    this.clearPolygonEditing();

                    if (this.m_polygonEdit == closest.target && closest.distanceInPixels < 12)
                    {
                        this.m_polygonEdit.points.splice(closest.closestIndex + 1, 0, closest.interpolatedPoint);

                        let activeFenceExt = this.findFenceExtended(closest.fence);
                        activeFenceExt.refresh();

                        this.m_extEdit = this.editPolygon(closest.fence, false, this.m_polygonEdit, (change) =>
                        {
                            activeFenceExt.refresh();
                        });

                        this.updateEditState(Action.EditFence, "Move existing points by dragging them, click on segment to add new points, or right-click to remove them");
                    }
                    else
                    {
                        this.updateEditState(undefined, undefined);
                    }
                }
            }
            else
            {
                switch (this.editingMode)
                {
                    case Action.AddFence:
                        this.createNewFence(target);
                        return;
                }
            }
        }
    }

    private clearPolygonEditing()
    {
        this.m_polygonEditing.clear();
        this.m_extEdit = undefined;
    }

    private clearEditingState()
    {
        this.m_fenceUnderCreation = undefined;
        this.m_polygonEdit        = undefined;

        this.updateEditState(undefined, undefined);
    }

    private findFenceExtended(fence: Models.GeoFenceByPolygon): FenceLayerFeature
    {
        return this.m_fencesExt.find((ext) => ext.source == fence);
    }

    private isNewPointValid(target: Models.LongitudeLatitude): boolean
    {
        //
        // We already have a slot for the point, we just need to make sure it creates a valid fence.
        //
        return this.canMovePointOnFence(this.m_fenceUnderCreation.source, this.m_polygonEdit, target, this.m_newFenceCommittedPoints);
    }

    private canMovePointOnFence(fence: Models.GeoFenceByPolygon,
                                targetPolygon: Models.LocationPolygon,
                                newPoint: Models.LongitudeLatitude,
                                index: number): boolean
    {
        let newPolygon           = new Models.LocationPolygon();
        newPolygon.points        = [...targetPolygon.points];
        newPolygon.points[index] = newPoint;

        return MapExtended.checkValidFence(fence, targetPolygon, newPolygon);
    }

    private canRemovePointFromFence(fence: Models.GeoFenceByPolygon,
                                    targetPolygon: Models.LocationPolygon,
                                    index: number): boolean
    {
        if (targetPolygon.points.length <= 3) return false;

        let newPolygon    = new Models.LocationPolygon();
        newPolygon.points = [...targetPolygon.points];
        newPolygon.points.splice(index, 1);

        return MapExtended.checkValidFence(fence, targetPolygon, newPolygon);
    }

    private editExistingFence(fence: Models.GeoFenceByPolygon,
                              target: Models.LocationPolygon)
    {
        this.m_polygonEdit = target;

        let activeFenceExt = this.findFenceExtended(fence);

        this.m_extEdit = this.editPolygon(fence, false, this.m_polygonEdit, (change) =>
        {
            activeFenceExt.refresh();
        });
    }

    private createNewExclusion(fence: Models.GeoFenceByPolygon,
                               target: Models.LongitudeLatitude)
    {
        let polygon    = new Models.LocationPolygon();
        polygon.points = [
            target,
            target
        ];

        if (!fence.innerExclusions) fence.innerExclusions = [];
        fence.innerExclusions.push(polygon);

        this.m_fenceUnderCreation      = this.findFenceExtended(fence);
        this.m_polygonEdit             = polygon;
        this.m_newFenceCommittedPoints = 1; // We start with one point in the polygon.

        this.m_extEdit = this.editPolygon(fence, true, this.m_polygonEdit, (change) =>
        {
            this.m_fenceUnderCreation.refresh();
        });
    }

    private createNewFence(target: Models.LongitudeLatitude)
    {
        let polygon    = new Models.LocationPolygon();
        polygon.points = [
            target,
            target
        ];

        let fence      = new Models.GeoFenceByPolygon();
        fence.boundary = polygon;

        this.m_fenceUnderCreation = this.m_fence.addFence(fence, ChartColorUtilities.getColorById("Map Colors", "mapyellow").hex);

        this.m_polygonEdit             = polygon;
        this.m_newFenceCommittedPoints = 1; // We start with one point in the polygon.

        this.m_extEdit = this.editPolygon(fence, true, this.m_polygonEdit, (change) =>
        {
            this.m_fenceUnderCreation.refresh();
        });
    }

    private editPolygon(fence: Models.GeoFenceByPolygon,
                        isNew: boolean,
                        targetPolygon: Models.LocationPolygon,
                        callback: (change: "added" | "deleted" | "moved") => void): PolygonEditingLayerFeature
    {
        let extEdit = this.m_polygonEditing.setPolygon(targetPolygon, !isNew, isNew, ChartColorUtilities.getColorById("Map Colors", "mapred").hex);

        extEdit.checkMove = (index,
                             newPoint) =>
        {
            return this.canMovePointOnFence(fence, targetPolygon, newPoint, index);
        };

        extEdit.checkDelete = (index) =>
        {
            return this.canRemovePointFromFence(fence, targetPolygon, index);
        };

        extEdit.onChange = (change) =>
        {
            callback(change);

            this.updateHasChanges();
        };

        return extEdit;
    }
}

class LocationFencesEditorState
{
    extended: LocationExtended;
    mapLocation: Models.LongitudeLatitude;
}

enum Action
{
    DeleteFence,
    AddFence,
    AddRadius,
    EditFence,
    EditRadius,
}
