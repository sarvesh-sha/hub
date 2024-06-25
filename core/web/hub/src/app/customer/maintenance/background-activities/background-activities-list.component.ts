import {Component, Injector} from "@angular/core";

import {BackgroundActivityExtended} from "app/services/domain/background-activities.service";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {PersistViewState} from "framework/ui/components";
import {ControlOption} from "framework/ui/control-option";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";

@Component({
               selector   : "o3-background-activities-list",
               templateUrl: "./background-activities-list.component.html"
           })
export class BackgroundActivitiesListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, BackgroundActivityExtended, BackgroundActivityExtended>
{
    table: DatatableManager<Models.RecordIdentity, BackgroundActivityExtended, BackgroundActivityExtended>;

    private m_notifier: SharedSvc.DbChangeNotifier<Models.BackgroundActivity, BackgroundActivityExtended>;

    //--//

    @PersistViewState() private m_filterText: string = null;

    public get filterText(): string
    {
        return this.m_filterText;
    }

    public set filterText(value: string)
    {
        this.m_filterText = value;
        this.table.refreshData();
    }

    //--//

    @PersistViewState() private m_filterStatus: Models.BackgroundActivityFilterPair;
    public states: ControlOption<string>[];

    public get filterStatus(): string
    {
        return JSON.stringify(this.m_filterStatus);
    }

    public set filterStatus(value: string)
    {
        if (value)
        {
            this.m_filterStatus = Models.BackgroundActivityFilterPair.newInstance(JSON.parse(value));
        }
        else
        {
            this.m_filterStatus = undefined;
        }

        this.table.refreshData();
    }

    //--//

    constructor(inj: Injector)
    {
        super(inj);

        this.persistViewStateReady(); // Needed to properly enable @PersistViewState

        if (!this.m_filterStatus)
        {
            this.m_filterStatus        = new Models.BackgroundActivityFilterPair();
            this.m_filterStatus.filter = Models.BackgroundActivityFilter.hideCompleted;
        }

        this.table = this.newTableWithAutoRefresh(this.app.domain.backgroundActivities, this);

        this.m_notifier = this.listenToDatabase(this.app.domain.backgroundActivities, () => this.invalidateData(), () => this.invalidateData());
        this.m_notifier.subscribe(null, true, true, true);
    }

    public getTableConfigId(): string { return "background-activities"; }

    async ngAfterViewInit()
    {
        super.ngAfterViewInit();

        this.states = await this.app.domain.backgroundActivities.prepareFilterControlOptions();
    }

    async invalidateData()
    {
        this.table.refreshData();
    }

    getItemName(): string
    {
        return "Background Activities";
    }

    getList(): Promise<Models.RecordIdentity[]>
    {
        let filter          = new Models.BackgroundActivityFilterRequest();
        filter.sortBy       = this.mapSortBindings(this.table.sort);
        filter.likeFilter   = this.m_filterText;
        filter.statusFilter = this.m_filterStatus;

        return this.app.domain.backgroundActivities.getList(filter);
    }

    getPage(offset: number,
            limit: number): Promise<BackgroundActivityExtended[]>
    {
        return this.app.domain.backgroundActivities.getPageFromTable(this.table, offset, limit);
    }

    async transform(rows: BackgroundActivityExtended[]): Promise<BackgroundActivityExtended[]>
    {
        return rows; // Nothing to do.
    }

    itemClicked(columnId: string,
                item: BackgroundActivityExtended)
    {
        this.app.ui.navigation.go("/background-activities/item", [item.model.sysId]);
    }
}
