import {Directive, Injector, Input} from "@angular/core";

import {BaseApplicationComponent} from "app/services/domain/base.service";
import {IProviderForMapHost, ProviderForMappableList} from "app/shared/tables/provider-for-map";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {ImportDialogComponent} from "framework/ui/dialogs/import-dialog.component";

@Directive()
export abstract class BlockTableComponent<T, S extends ProviderForMappableList<T>> extends BaseApplicationComponent implements IProviderForMapHost
{
    constructor(inj: Injector)
    {
        super(inj);
    }

    protected abstract getProvider(): S;

    protected abstract initExtra(): Promise<void>;

    protected abstract parseContents(contents: string,
                                     fileName: string): T[];

    protected abstract getItemName(): string;

    private m_data: T[];

    @Input()
    public set data(data: T[])
    {
        this.m_data = data;
        if (this.m_data)
        {
            this.bindList();
        }
    }

    private async bindList()
    {
        await this.initExtra();
        this.getProvider()
            .bindList(this.m_data);
    }

    public setDirty()
    {
    }

    export()
    {
        let fileItemName = this.getItemName()
                               .toLowerCase()
                               .replace(/ /g, "-");
        DownloadDialogComponent.open(this, `Export ${this.getItemName()}`, DownloadDialogComponent.fileName("blockly" + fileItemName), this.m_data);
    }

    async import()
    {
        let data = await ImportDialogComponent.open(this, `Import ${this.getItemName()}`, {
            returnRawBlobs: () => false,
            parseFile     : async (contents: string,
                                   fileName: string): Promise<T[]> =>
            {
                try
                {
                    return this.parseContents(contents, fileName);
                }
                catch (err)
                {
                    throw Error("Invalid import format");
                }
            }
        });

        if (data)
        {
            this.m_data.length = 0;
            this.m_data.push(...data);
            this.bindList();
        }
    }
}
