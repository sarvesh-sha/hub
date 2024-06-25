import {Component, Injector, Input} from "@angular/core";

import {AssetExtended, DeviceExtended, LocationExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {AlertExtended, EventExtended} from "app/services/domain/events.service";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {DatatableManager, DatatableSelectionManager, FilterDebouncer, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {DatatableContextMenuEvent} from "framework/ui/datatables/datatable.component";
import {inParallel, mapInParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-alerts-list",
               templateUrl: "./alerts-list.component.html"
           })
export class AlertsListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, AlertExtended, AlertFlat>
{
    private m_filterDebouncer: FilterDebouncer<AlertFilter, Models.AlertFilterRequest>;

    @Input()
    public set filters(value: Models.AlertFilterRequest)
    {
        this.m_filterDebouncer.setExternalFilter(value);
    }

    public get filters(): Models.AlertFilterRequest
    {
        return this.m_filterDebouncer.getExternalFilter();
    }

    private readonly m_bulkEditOptions: ControlOption<Models.AlertStatus>[] = [
        new ControlOption(Models.AlertStatus.muted, "Mute Alert"),
        new ControlOption(Models.AlertStatus.active, "Unmute Alert"),
        new ControlOption(Models.AlertStatus.resolved, "Resolve Alert"),
        new ControlOption(Models.AlertStatus.closed, "Close Alert")
    ];

    private m_selectedAlerts: AlertExtended[] = [];

    table: DatatableManager<Models.RecordIdentity, EventExtended, AlertFlat>;

    constructor(inj: Injector)
    {
        super(inj);

        this.m_filterDebouncer = new FilterDebouncer(() =>
                                                     {
                                                         return new AlertFilter();
                                                     },
                                                     () =>
                                                     {
                                                         return this.getViewStateValue<AlertFilter>("TABLE_FILTERS");
                                                     },
                                                     (state) =>
                                                     {
                                                         this.setViewStateValue("TABLE_FILTERS", state);
                                                     },
                                                     (state,
                                                      baseFilters) =>
                                                     {
                                                         let filters        = Models.AlertFilterRequest.deepClone(baseFilters) || new Models.AlertFilterRequest();
                                                         let alertStatusIds = state.alertStatusIDs || baseFilters.alertStatusIDs;
                                                         if (alertStatusIds)
                                                         {
                                                             filters.alertStatusIDs = alertStatusIds;
                                                         }
                                                         else
                                                         {
                                                             filters.alertStatusIDs = [Models.AlertStatus.active];
                                                         }

                                                         return filters;
                                                     },
                                                     (filtersChanged: boolean) =>
                                                     {
                                                         if (filtersChanged)
                                                         {
                                                             this.table.resetPagination();
                                                         }

                                                         this.table.refreshData();
                                                     });

        this.table           = this.newTableWithAutoRefresh(this.app.domain.events, this);
        let selectionManager = this.table.enableSimpleSelection((key) => key.sysId, (eq) => eq.extended.typedModel.sysId);
        this.subscribeToObservable(selectionManager.selectionChange, async (selection) =>
        {
            this.m_selectedAlerts = null;
            let records           = Array.from(selection)
                                         .map((id) => Models.RecordIdentity.newInstance({sysId: id}));
            this.m_selectedAlerts = await this.app.domain.events.getTypedExtendedBatch(AlertExtended, records);
        });
    }

    getTableConfigId(): string { return "alerts"; }

    getItemName(): string { return "Alerts"; }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        let filters    = this.m_filterDebouncer.generateFilter();
        filters.sortBy = this.mapSortBindings(this.table.sort);

        let ids = await this.app.domain.events.getList(filters);

        if (this.app.domain.events.hasAppliedFilters(filters))
        {
            let count                               = await this.app.domain.events.getCount(new Models.AlertFilterRequest());
            this.table.config.messages.totalMessage = `of ${count} Alerts`;
        }

        return ids.results;
    }

    getPage(offset: number,
            limit: number): Promise<AlertExtended[]>
    {
        return this.app.domain.events.getTypedPageFromTable(AlertExtended, this.table, offset, limit);
    }

    async transform(rows: AlertExtended[]): Promise<AlertFlat[]>
    {
        return await mapInParallel(rows, async (row) =>
        {
            let result      = new AlertFlat();
            result.extended = row;

            result.asset = await row.getAsset();
            if (result.asset)
            {
                result.contextName = result.asset.model.name;
                if (result.asset instanceof DeviceExtended)
                {
                    result.manufacturerName = result.asset.typedModel.manufacturerName;
                    result.productName      = result.asset.typedModel.productName;
                    result.modelNumber      = result.asset.typedModel.modelName;
                }
            }

            result.location = await row.getLocation();
            if (result.location)
            {
                result.locationPath = await result.location.getRecursivePath();
            }

            result.typeLabel     = await row.getDisplayType();
            result.severityLabel = await this.app.domain.alerts.describeSeverity(row.typedModel.severity);

            return result;
        });
    }

    itemClicked(columnId: string,
                item: AlertFlat)
    {
        this.app.ui.navigation.go("/alerts/alert", [item.extended.model.sysId]);
    }

    contextMenu(event: DatatableContextMenuEvent<AlertFlat>)
    {
        let clickedId = event.row?.extended?.model.sysId;
        if (clickedId && event.columnProperty === "status" && this.m_selectedAlerts)
        {
            if (this.m_selectedAlerts.every((alertExt) => alertExt.model.sysId !== clickedId))
            {
                // add directly to m_selectedAlerts because programmatically changing selection does not trigger a change event
                // but will continue to be reflected in underlying selection
                this.m_selectedAlerts.push(event.row.extended);
                DatatableSelectionManager.ensureSelection(this.table.selectionManager, clickedId);
            }

            let someNotClosed    = false;
            let validEditOptions = new Set(this.m_bulkEditOptions.map((option) => option.id));
            for (let alertExt of this.m_selectedAlerts)
            {
                switch (alertExt.typedModel.status)
                {
                    case Models.AlertStatus.active:
                        someNotClosed = true;
                        validEditOptions.delete(Models.AlertStatus.active);
                        validEditOptions.delete(Models.AlertStatus.closed);
                        break;

                    case Models.AlertStatus.muted:
                        someNotClosed = true;
                        validEditOptions.delete(Models.AlertStatus.muted);
                        validEditOptions.delete(Models.AlertStatus.closed);
                        break;

                    case Models.AlertStatus.resolved:
                        someNotClosed = true;
                        validEditOptions.delete(Models.AlertStatus.muted);
                        validEditOptions.delete(Models.AlertStatus.active);
                        validEditOptions.delete(Models.AlertStatus.resolved);
                        break;

                    case Models.AlertStatus.closed:
                        validEditOptions.delete(Models.AlertStatus.muted);
                        validEditOptions.delete(Models.AlertStatus.active);
                        validEditOptions.delete(Models.AlertStatus.resolved);
                        validEditOptions.delete(Models.AlertStatus.closed);
                        break;
                }
            }

            if (validEditOptions.size)
            {
                for (let option of this.m_bulkEditOptions)
                {
                    if (validEditOptions.has(option.id))
                    {
                        event.root.addItem(UtilsService.pluralize(option.label, this.m_selectedAlerts.length), async () =>
                        {
                            let confirmationMessage = "Are you sure you want to ";
                            switch (option.id)
                            {
                                case Models.AlertStatus.active:
                                    confirmationMessage += "unmute ";
                                    break;

                                case Models.AlertStatus.muted:
                                    confirmationMessage += "mute ";
                                    break;

                                case Models.AlertStatus.resolved:
                                    confirmationMessage += "resolve ";
                                    break;

                                case Models.AlertStatus.closed:
                                    confirmationMessage += "close ";
                                    break;
                            }
                            if (this.m_selectedAlerts.length > 1)
                            {
                                confirmationMessage += `these ${this.m_selectedAlerts.length} alerts?`;
                            }
                            else
                            {
                                confirmationMessage += "this alert?";
                            }

                            if (await this.confirmOperation(confirmationMessage))
                            {
                                await inParallel(this.m_selectedAlerts, (alertExt) =>
                                {
                                    alertExt.typedModel.status = option.id;
                                    return alertExt.save();
                                });

                                this.table.selectionManager.checkAllItems(false);
                            }
                        });
                    }
                }
            }
            else
            {
                let message = someNotClosed ? "No single action can be taken" : "Nothing to do";
                event.root.addItem(message, undefined, true);
            }
        }
    }
}

class AlertFilter
{
    filterText: string;
    alertStatusIDs: Models.AlertStatus[];
}

class AlertFlat
{
    extended: AlertExtended;
    asset: AssetExtended;

    manufacturerName: string;
    productName: string;
    modelNumber: string;
    contextName: string;

    location: LocationExtended;
    locationPath: string;

    typeLabel: string;
    severityLabel: string;
}
