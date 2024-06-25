import {CdkVirtualScrollViewport} from "@angular/cdk/scrolling";
import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output, SimpleChanges, ViewChild} from "@angular/core";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {EventExtended} from "app/services/domain/events.service";

import * as Models from "app/services/proxy/model/models";
import {ExcelExporter} from "app/shared/utils/excel-exporter";
import {UtilsService} from "framework/services/utils.service";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {inParallel} from "framework/utils/concurrency";

@Component({
               selector       : "o3-timeline",
               templateUrl    : "./timeline.component.html",
               styleUrls      : ["./timeline.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class TimelineComponent extends BaseApplicationComponent
{
    private m_events: EventExtended[] = [];
    private m_eventToLocation         = new Map<string, string>();
    private m_eventToDefinition       = new Map<string, string>();
    private m_userToName              = new Map<string, string>();

    @Input() history: TimelineItem[] = [];

    @Input() showLinks: boolean = true;

    @Output() itemSelected = new EventEmitter<TimelineItem>();
    @Output() initialized  = new EventEmitter<boolean>();

    @ViewChild(CdkVirtualScrollViewport) scrollViewport: CdkVirtualScrollViewport;

    public async ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);
        if (changes.history && this.history)
        {
            this.initialized.emit(false);

            await this.initEvents();
            await this.initDefinitionTitles();
            await this.initLocations();
            await this.initUsers();
            this.markForCheck();

            if (this.history.length) this.initialized.emit(true);
        }
    }

    getLocation(item: TimelineItem): string
    {
        return this.m_eventToLocation.get(item.eventId) || "";
    }

    getDefinitionTitle(item: TimelineItem): string
    {
        return this.m_eventToDefinition.get(item.eventId) || "";
    }

    getUser(item: TimelineItem): string
    {
        return this.m_userToName.get(item.userId) || "";
    }

    refreshHeight(): boolean
    {
        if (this.scrollViewport)
        {
            this.scrollViewport.checkViewportSize();
            return true;
        }

        return false;
    }

    async exportToExcel()
    {
        let exporter = new ExcelExporter(this.app.domain.apis.exports, "Events");
        exporter.addColumnHeader("Id");
        exporter.addColumnHeader("EventID");
        exporter.addColumnHeader("UserID");
        exporter.addColumnHeader("Location");
        exporter.addColumnHeader("Definition");
        exporter.addColumnHeader("Text");
        exporter.addColumnHeader("Time", 'm/d/yy h:mm:ss "GMT"');

        let fileName = DownloadDialogComponent.fileName("timeline", ".xlsx");
        DownloadDialogComponent.openWithGenerator(this, "Events", fileName, exporter);

        for (let extraHeaderTitle of this.history[0]?.getExtraColumnHeaders() ?? [])
        {
            exporter.addColumnHeader(extraHeaderTitle);
        }

        for (let item of this.history)
        {
            let row = await exporter.addRow();
            row.push(item.id, item.eventId, item.userId, this.getLocation(item), this.getDefinitionTitle(item), item.text, item.date, ...item.getExtraColumns());
        }

        exporter.finish();
    }

    private async initEvents()
    {
        let uniqueEventIds = new Set(this.history.map((item) => item.eventId));
        let eventIds       = UtilsService.mapIterable(uniqueEventIds, (id) => EventExtended.newIdentityRaw(id));
        this.m_events      = await this.app.domain.events.getExtendedBatch(eventIds);
    }

    private async initDefinitionTitles()
    {
        await inParallel(this.m_events, async (event) =>
        {
            let definitionTitle = await event.getDefinitionTitle();
            if (definitionTitle)
            {
                this.m_eventToDefinition.set(event.model.sysId, definitionTitle);
            }
        });
    }

    private async initLocations()
    {
        await inParallel(this.m_events, async (event) =>
        {
            let location = await event.getLocation();
            if (location)
            {
                this.m_eventToLocation.set(event.model.sysId, location.model.name);
            }
        });
    }

    private async initUsers()
    {
        let uniqueUserIds = new Set(this.history.map((item) => item.userId)
                                        .filter((id) => !!id));

        for (let userId of uniqueUserIds)
        {
            let user = await this.app.domain.userManagement.getExtendedById(userId);
            this.m_userToName.set(user.model.sysId, user.fullName);
        }
    }
}

export abstract class TimelineItem
{
    constructor(public id: string,
                public eventId: string,
                public userId: string,
                public text: string,
                public date: Date)
    {
    }

    abstract getIcon(): string;

    abstract getExtraColumnHeaders(): string[];

    abstract getExtraColumns(): string[];
}

export class AlertTimelineItem extends TimelineItem
{
    static createList(history: Models.AlertHistory[]): AlertTimelineItem[]
    {
        return history.map((history) => new AlertTimelineItem(history));
    }

    constructor(private history: Models.AlertHistory)
    {
        super(history.sysId, history.alert.sysId, null, history.text, history.createdOn);
    }

    getIcon()
    {
        let iconClass = `o3 status-icon status-${this.history.level} `;
        switch (this.history.level)
        {
            case Models.AlertEventLevel.failure:
                return iconClass + "o3-failure";
            case Models.AlertEventLevel.info:
                return iconClass + "o3-information";
            case Models.AlertEventLevel.recall:
                return iconClass + "o3-warning";
            case Models.AlertEventLevel.success:
                return iconClass + "o3-ok";
            case Models.AlertEventLevel.certification:
                return iconClass + "o3-unknown";
        }

        return "";
    }

    public getExtraColumnHeaders(): string[]
    {
        return [
            "Alert Event Type",
            "Alert Level"
        ];
    }

    public getExtraColumns(): string[]
    {
        return [
            this.history.type,
            this.history.level
        ];
    }
}

export class WorkflowTimelineItem extends TimelineItem
{
    static createList(history: Models.WorkflowHistory[]): WorkflowTimelineItem[]
    {
        return history.map((history) => new WorkflowTimelineItem(history));
    }

    constructor(private history: Models.WorkflowHistory)
    {
        super(history.sysId, history.workflow.sysId, history.user?.sysId, history.text, history.createdOn);
    }

    getIcon()
    {
        let iconClass = `o3 status-icon `;
        switch (this.history.type)
        {
            case Models.WorkflowEventType.closed:
            case Models.WorkflowEventType.resolved:
                return iconClass + "o3-ok status-success";
            case Models.WorkflowEventType.created:
            case Models.WorkflowEventType.updatedWithNotes:
                return iconClass + "o3-information status-info";
            case Models.WorkflowEventType.reassigned:
            case Models.WorkflowEventType.reopened:
            case Models.WorkflowEventType.disabled:
            case Models.WorkflowEventType.disabling:
                return iconClass + "o3-warning status-recall";
        }

        return "";
    }

    public getExtraColumnHeaders(): string[]
    {
        return ["Workflow Event Type"];
    }

    public getExtraColumns(): string[]
    {
        return [this.history.type];
    }
}
