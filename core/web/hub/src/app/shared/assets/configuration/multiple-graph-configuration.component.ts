import {Component, ElementRef, EventEmitter, Input, Output, QueryList, ViewChild, ViewChildren} from "@angular/core";
import {UUID} from "angular2-uuid";
import {AssetStructureWizardDialogComponent, AssetStructureWizardState} from "app/customer/configuration/asset-structures/wizard/asset-structure-wizard-dialog.component";

import {SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {GraphConfigurationHost} from "app/shared/assets/configuration/graph-configuration-host";

import {UtilsService} from "framework/services/utils.service";
import {ModifiableTableComponent} from "framework/ui/shared/modifiable-table.component";

@Component({
               selector   : "o3-multiple-graph-configuration",
               templateUrl: "./multiple-graph-configuration.component.html"
           })
export class MultipleGraphConfigurationComponent extends BaseApplicationComponent
{
    private m_host: GraphConfigurationHost;
    @Input() set host(host: GraphConfigurationHost)
    {
        if (host)
        {
            this.m_host = host;
            this.bind();
        }
    }

    get host(): GraphConfigurationHost
    {
        return this.m_host;
    }

    @Input() singularGraph      = false;
    @Input() readonlyGraphNames = false;

    @Output() graphsUpdated = new EventEmitter<void>();

    @ViewChildren("test_graphName", {read: ElementRef}) test_graphNames: QueryList<ElementRef>;
    @ViewChildren("test_configureGraph", {read: ElementRef}) test_configureGraphs: QueryList<ElementRef>;
    @ViewChild("test_table") test_table: ModifiableTableComponent<Models.SharedAssetGraph>;

    rows: Models.SharedAssetGraph[];

    public static isValid(host: GraphConfigurationHost): boolean
    {
        if (!host) return false;
        const graphs = host.getGraphs();
        return graphs.length > 0 && UtilsService.valuesAreUnique(graphs.map((g) => g.name));
    }

    async addRow()
    {
        let data = new MultipleAssetStructureWizardState(this, undefined, this.host);
        if (await AssetStructureWizardDialogComponent.open(data, this))
        {
            this.rows.push(SharedAssetGraphExtended.newModel(data.graph.model, UUID.UUID(), data.graph.name));

            this.graphsChanged();
            this.detectChanges();
            this.graphsUpdated.emit();
        }
    }

    async editRow(row: Models.SharedAssetGraph)
    {
        let graph = Models.SharedAssetGraph.deepClone(row);
        let data  = new MultipleAssetStructureWizardState(this, new SharedAssetGraphExtended(this.app.domain, graph), this.host, !this.readonlyGraphNames);
        if (await AssetStructureWizardDialogComponent.open(data, this))
        {
            row.name  = data.graph.name;
            row.graph = data.graph.model;
            this.graphsChanged();
            this.detectChanges();
            this.graphsUpdated.emit();
        }
    }

    rowRemoved()
    {
        this.graphsChanged();
    }

    canRemove(row: Models.SharedAssetGraph): boolean
    {
        return this.m_host.canRemove(row.id);
    }

    graphsChanged()
    {
        this.m_host.graphsChanged.next();
    }

    private bind(): void
    {
        this.rows = this.m_host.getGraphs();
    }
}

export class MultipleAssetStructureWizardState extends AssetStructureWizardState
{
    constructor(private m_host: MultipleGraphConfigurationComponent,
                model?: SharedAssetGraphExtended,
                host?: GraphConfigurationHost,
                editableGraphName: boolean = true)
    {
        super(m_host.app.domain, model, host, editableGraphName);
    }

    public async create(comp: BaseApplicationComponent,
                        goto: boolean): Promise<boolean>
    {
        return true;
    }

    public async save(comp: BaseApplicationComponent): Promise<boolean>
    {
        return true;
    }
}

