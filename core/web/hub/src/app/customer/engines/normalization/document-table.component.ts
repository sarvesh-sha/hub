import {Component, Injector, Input} from "@angular/core";
import {BlockTableComponent} from "app/customer/engines/shared/block-table.component";
import * as Models from "app/services/proxy/model/models";
import {ProviderForMappableList} from "app/shared/tables/provider-for-map";
import {ControlOption} from "framework/ui/control-option";

@Component({
               selector   : "o3-document-table",
               templateUrl: "./document-table.component.html"
           })
export class DocumentTableComponent extends BlockTableComponent<Models.NormalizationEngineValueDocument, DocumentProvider>
{
    provider: DocumentProvider;
    pointClassOptions: ControlOption<string>[] = [];
    private m_rules: Models.NormalizationRules;

    constructor(inj: Injector)
    {
        super(inj);
        this.provider = new DocumentProvider(this, "Document", "Documents", "ID", "Text");
    }

    @Input()
    public set rules(rules: Models.NormalizationRules)
    {
        this.m_rules = rules;
    }

    protected getProvider(): DocumentProvider
    {
        return this.provider;
    }

    protected async initExtra()
    {
        this.pointClassOptions = await this.app.bindings.getPointClasses(false, this.m_rules);
    }

    protected getItemName(): string
    {
        return "Documents";
    }

    protected parseContents(contents: string): Models.NormalizationEngineValueDocument[]
    {
        let data: Models.NormalizationEngineValueDocument[] = JSON.parse(contents);
        for (let item of data)
        {
            Models.NormalizationEngineValueDocument.fixupPrototype(item);
        }

        return data;
    }

    getPointClassOption(id: string): ControlOption<string>
    {
        return this.pointClassOptions.find((opt) => opt.id === id);
    }
}

class DocumentProvider extends ProviderForMappableList<Models.NormalizationEngineValueDocument>
{
    constructor(private m_host: DocumentTableComponent,
                subViewPrefix: string,
                displayName: string,
                labelForKey: string,
                labelForValue: string)
    {
        super(m_host, subViewPrefix, displayName, labelForKey, labelForValue);
    }

    public getKey(item: Models.NormalizationEngineValueDocument): string
    {
        return item.id;
    }

    public setKey(item: Models.NormalizationEngineValueDocument,
                  key: string): void
    {
        item.id = key;
    }

    public isOrdered(): boolean
    {
        return true;
    }

    protected allocate(): Models.NormalizationEngineValueDocument
    {
        return Models.NormalizationEngineValueDocument.newInstance({
                                                                       id  : "",
                                                                       text: ""
                                                                   });
    }

    protected getText(data: Models.NormalizationEngineValueDocument): string
    {
        return data.text;
    }

    protected shouldInclude(data: Models.NormalizationEngineValueDocument,
                            filterLowercase: string): boolean
    {
        let pc = this.m_host.getPointClassOption(data.id);
        return this.contains(pc ? pc.label : "", filterLowercase) || this.contains(data.text, filterLowercase);
    }

    public sort(a: Models.NormalizationEngineValueDocument,
                b: Models.NormalizationEngineValueDocument): number
    {
        return 0;
    }
}
