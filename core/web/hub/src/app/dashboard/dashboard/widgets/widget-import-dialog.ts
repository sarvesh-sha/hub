import {AppContext} from "app/app.service";
import {WidgetExportInfo} from "app/dashboard/dashboard/widgets/widget-export-info";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {WidgetConfigurationExtended} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";

import {ImportDialogComponent} from "framework/ui/dialogs/import-dialog.component";

export class WidgetImportDialog
{
    public static async open(component: BaseApplicationComponent,
                             innerValidator?: (result: Models.WidgetConfiguration) => string): Promise<WidgetExportInfo<any>>
    {
        const validator = (widgetInfo: WidgetExportInfo<any>) =>
        {
            if (widgetInfo.config instanceof Models.WidgetConfiguration)
            {
                if (innerValidator)
                {
                    return innerValidator(widgetInfo.config);
                }

                return "";
            }
            else
            {
                return "No widget found";
            }
        };

        return ImportDialogComponent.open(component, "Widget Import", {
            returnRawBlobs: () => false,
            parseFile     : (contents: string) => this.parseWidget(component.app, contents),
            validator     : validator
        });
    }

    private static async parseWidget(app: AppContext,
                                     widgetJSON: string): Promise<WidgetExportInfo<any>>
    {
        try
        {
            let parsedJson = JSON.parse(widgetJSON);
            if (parsedJson?.config)
            {
                let exportInfo      = <WidgetExportInfo<any>>parsedJson;
                let dashboard       = Models.DashboardConfiguration.newInstance({widgets: [Models.WidgetComposition.newInstance({config: exportInfo.config})]});
                let rawImport       = Models.RawImport.newInstance({contentsAsJSON: JSON.stringify(dashboard)});
                let parsedModel     = await app.domain.apis.dashboardDefinitionVersions.parseImport(rawImport);
                let widgetConfigExt = WidgetConfigurationExtended.fromConfigModel(parsedModel.widgets[0].config);

                let uniqueGraphs = new Map<string, Models.SharedAssetGraph>();
                for (let graph of exportInfo.graphs || [])
                {
                    uniqueGraphs.set(graph.id, graph);
                }

                return new WidgetExportInfo(widgetConfigExt.model, exportInfo.outline, exportInfo.selectors, Array.from(uniqueGraphs.values()));
            }

            return null;
        }
        catch (e)
        {
            return null;
        }
    }
}
