import {ChangeDetectionStrategy, Component, EventEmitter, Injector, Input, Output} from "@angular/core";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {Block} from "framework/ui/blockly/block";
import {ModalDialogField} from "framework/ui/blockly/modal-dialog-field";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {ImportDialogComponent} from "framework/ui/dialogs/import-dialog.component";
import {TabActionDirective} from "framework/ui/shared/tab-action.directive";

@Component({
               selector       : "o3-exportable-string-set",
               templateUrl    : "./exportable-string-set.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ExportableStringSetComponent extends BaseApplicationComponent
{
    @Input() label: string;

    @Input() model: string[] = [];

    @Output() modelChange = new EventEmitter<string[]>();

    private m_actions: TabActionDirective[] = [];

    constructor(inj: Injector)
    {
        super(inj);

        const importAction    = new TabActionDirective();
        importAction.label    = "Import";
        importAction.callback = () => this.import();

        const exportAction    = new TabActionDirective();
        exportAction.label    = "Export";
        exportAction.callback = () => this.export();

        this.m_actions = [
            importAction,
            exportAction
        ];
    }

    public getActions(): TabActionDirective[]
    {
        return this.m_actions;
    }

    private async import()
    {
        const terms = await ImportDialogComponent.open(this, `Import ${this.label}`, {
            returnRawBlobs: () => false,
            parseFile     : async (contents: string) =>
            {
                let array            = JSON.parse(contents);
                let result: string[] = [];
                if (Array.isArray(array))
                {
                    for (let element of array)
                    {
                        if (typeof element === "string")
                        {
                            result.push(element);
                        }
                    }
                }
                return result;
            }
        });

        if (terms != null)
        {
            this.model = terms;
            this.modelChange.emit(this.model);
        }

        this.detectChanges();
    }

    private export()
    {
        DownloadDialogComponent.open(this,
                                     `Export ${this.label}`,
                                     DownloadDialogComponent.fileName(this.label.split(" ")
                                                                          .join("_")),
                                     this.model);
    }
}

export class ExportableStringSetField extends ModalDialogField<ExportableStringSetComponent, typeof ExportableStringSetComponent, string[]>
{
    private m_model: string[];

    constructor(block: Block<any>,
                private m_label: string)
    {
        super(block, ExportableStringSetComponent, m_label);
    }

    protected initData(data: string[]): void
    {
        if (data)
        {
            this.m_model = data;
        }
        else if (!this.m_model)
        {
            this.m_model = [];
        }
    }

    protected initComponent(component: ExportableStringSetComponent): void
    {
        component.label = this.m_label;
        component.model = this.m_model;
        component.modelChange.subscribe((model) => this.m_model = model);
    }

    public getModel(): string[]
    {
        return [...this.m_model];
    }

    protected getText(): string
    {
        return `${this.m_model.length} ${(this.m_label || "").toLowerCase()}`;
    }

    protected getActions(component: ExportableStringSetComponent): TabActionDirective[]
    {
        return component.getActions();
    }
}
