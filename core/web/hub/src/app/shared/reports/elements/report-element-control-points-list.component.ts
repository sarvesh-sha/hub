import {Component, Injector} from "@angular/core";
import {DeviceElementExtended, DeviceExtended} from "app/services/domain/assets.service";
import {ReportElementBaseComponent, ReportElementConfigurationBase, ReportElementDataBase, ReportElementModel, ReportElementType} from "app/shared/reports/elements/report-element-base.component";
import moment from "framework/utils/moment";

@Component({
               selector   : "o3-report-element-control-points-list",
               templateUrl: "./report-element-control-points-list.component.html"
           })
export class ReportElementControlPointsListComponent extends ReportElementBaseComponent<ReportElementControlPointsListData, ReportElementControlPointsListConfiguration>
{
    controlPoints: ControlPointFlat[];

    constructor(inj: Injector)
    {
        super(inj);
    }

    async afterConfigurationChanges()
    {
        if (this.data && this.data.points)
        {
            await this.initControlPoints(this.data.points);
        }

        this.markAsComplete();
    }

    async initControlPoints(points: DeviceElementExtended[])
    {
        let controlPoints = [];
        for (let point of points)
        {
            let device = <DeviceExtended>await point.getParent();
            let loc    = await device.getLocation();
            await loc.getRecursiveName();
            let locationName = loc ? loc.recursiveName : "No Location Assigned";

            let controlPoint        = new ControlPointFlat();
            controlPoint.self       = point;
            controlPoint.name       = point.model.name;
            controlPoint.deviceName = device.model.name;
            controlPoint.location   = locationName;

            controlPoints.push(controlPoint);
        }

        this.controlPoints = controlPoints;
    }
}

class ControlPointFlat
{
    self: DeviceElementExtended;
    name: string;
    deviceName: string;
    location: string;
}

export class ReportElementControlPointsListConfiguration extends ReportElementConfigurationBase
{
    public static newReportModel()
    {
        let model           = new ReportElementModel();
        model.type          = ReportElementType.ControlPointList;
        model.configuration = new ReportElementControlPointsListConfiguration();
        return model;
    }
}

export class ReportElementControlPointsListData extends ReportElementDataBase
{
    static newInstance(controlPoints: DeviceElementExtended[],
                       rangeStart: moment.Moment,
                       rangeEnd: moment.Moment): ReportElementControlPointsListData
    {
        let data        = new ReportElementControlPointsListData();
        data.points     = controlPoints;
        data.rangeStart = rangeStart;
        data.rangeEnd   = rangeEnd;
        return data;
    }

    points: DeviceElementExtended[];

    rangeStart: moment.Moment;

    rangeEnd: moment.Moment;
}
