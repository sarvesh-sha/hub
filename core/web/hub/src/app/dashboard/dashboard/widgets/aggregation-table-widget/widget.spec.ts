import {NgModule} from "@angular/core";

import {DataAggregationGroupsTest} from "app/dashboard/dashboard/dashboard.spec";
import * as Models from "app/services/proxy/model/models";
import {TestCase, waitFor} from "app/test/driver";

import {TreeChartMode} from "framework/ui/charting/tree-chart.component";

@TestCase({
              id        : "dashboard_widget_dataAggregation_groups_interactivity",
              name      : "Data Aggregation widget - Groups Interactivity",
              timeout   : 130,
              categories: [
                  "Dashboard",
                  "Widgets"
              ]
          })
class DataAggregationGroupsInteractivityTest extends DataAggregationGroupsTest
{
    protected readonly m_widgetType = Models.AggregationTableWidgetConfiguration;

    private m_modeLabels: string[];

    private readonly m_rangeOptions                       = [
        "Last 24 Hours",
        "Last 7 Days",
        "Last 30 Days"
    ];
    private m_depthMap: [string, number][]                = [
        [
            "Table",
            null
        ],
        [
            "Bar Table",
            null
        ],
        [
            "Bubble Chart",
            Infinity
        ],
        [
            "Tree Chart",
            Infinity
        ],
        [
            "Donut Chart",
            0
        ],
        [
            "Sunburst Chart",
            Infinity
        ],
        [
            "Pie Chart",
            0
        ],
        [
            "Sunburst Chart - Pie",
            Infinity
        ]
    ];
    private m_visualizationMap: [string, TreeChartMode][] = [
        [
            "Table",
            null
        ],
        [
            "Bar Table",
            null
        ],
        [
            "Bubble Chart",
            TreeChartMode.BUBBLE
        ],
        [
            "Tree Chart",
            TreeChartMode.BOX
        ],
        [
            "Donut Chart",
            TreeChartMode.SUNBURST
        ],
        [
            "Sunburst Chart",
            TreeChartMode.SUNBURST
        ],
        [
            "Pie Chart",
            TreeChartMode.PIEBURST
        ],
        [
            "Sunburst Chart - Pie",
            TreeChartMode.PIEBURST
        ]
    ];

    private m_labelToMode: Map<string, Models.HierarchicalVisualizationType>;
    private readonly m_labelToDepth         = new Map<string, number>(this.m_depthMap);
    private readonly m_labelToVisualization = new Map<string, TreeChartMode>(this.m_visualizationMap);

    private async extractAggTableInfo(): Promise<void>
    {
        const aggTableWidget = await this.getNewDataAggregationWidget();

        const modeOptions = await waitFor(() => aggTableWidget.modeOptions, "could not get mode options");

        this.m_modeLabels                                             = [];
        let modeMap: [string, Models.HierarchicalVisualizationType][] = modeOptions.map((modeOption) =>
                                                                                        {
                                                                                            this.m_modeLabels.push(modeOption.label);
                                                                                            return [
                                                                                                modeOption.label,
                                                                                                modeOption.id
                                                                                            ];
                                                                                        });

        this.m_labelToMode = new Map(modeMap);
    }

    public async execute(): Promise<void>
    {
        // Configure widget
        await this.createNewWidget(async (wizard) =>
                                   {
                                       await this.m_wizardDriver.stepNTimes(wizard, 2);

                                       await this.buildBaseAggregationTable(wizard);

                                       await this.m_wizardDriver.stepNTimes(wizard, 4);
                                   });

        await this.extractAggTableInfo();

        // Validate all modes
        for (let mode of this.m_modeLabels)
        {
            await this.selectAndValidateMode(mode);
        }

        // Switch to Tree Chart
        const container = await this.getNewWidgetContainer();
        await this.m_selectionDriver.selectMenuOption("widget container", container.test_menuTrigger, "View as", "Tree Chart");

        // Validate all ranges
        for (let range of this.m_rangeOptions)
        {
            await this.selectAndValidateRange(range);
        }
    }

    private async selectAndValidateRange(rangeLabel: string)
    {
        let container = await this.getNewWidgetContainer();
        await this.m_selectionDriver.selectMenuOption("widget container", container.test_menuTrigger, "Time Range", rangeLabel);

        let aggregationGroups = (await this.getNewDataAggregationWidget()).aggregationGroups;
        let index             = aggregationGroups.rangeIdx;
        let range             = aggregationGroups.rangeOptions[index];

        if (range.label !== rangeLabel) throw new Error("Range does not match selection");
    }

    private async selectAndValidateMode(modeLabel: string)
    {
        let container = await this.getNewWidgetContainer();
        await this.m_selectionDriver.selectMenuOption("widget container", container.test_menuTrigger, "View as", modeLabel);

        let mode          = this.m_labelToMode.get(modeLabel);
        let depth         = this.m_labelToDepth.get(modeLabel);
        let visualization = this.m_labelToVisualization.get(modeLabel);

        // Validate the table DOM is updated
        if (await this.getAttributeValue("o3-debug-table-mode") !== mode) throw new Error("Table mode was not updated correctly");

        if (!!depth && !!visualization)
        {
            if (await this.getAttributeValue("o3-debug-mode") !== visualization) throw new Error("Tree chart mode was not updated correctly");
            if (Number.parseFloat(await this.getAttributeValue("o3-debug-depth")) !== depth) throw new Error("Tree chart depth was not updated correctly");
        }
    }

    private async getElementByAttribute(attribute: string,
                                        root: HTMLElement = document.body): Promise<HTMLElement>
    {
        return await waitFor(() => { return root.querySelector(`[${attribute}]`); },
                             `Could not find element with attribute ${attribute}`);
    }

    private async getAttributeValue(attribute: string,
                                    root: HTMLElement = document.body): Promise<string>
    {
        return (await this.getElementByAttribute(attribute, root))
            .getAttribute(attribute);
    }
}

@NgModule({imports: []})
export class AggregationTableTestModule {}
