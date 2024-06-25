import {Component, ViewChild} from "@angular/core";

import * as Models from "app/services/proxy/model/models";
import {DataSourceWizardPurpose, DataSourceWizardState} from "app/shared/charting/data-source-wizard/data-source-wizard-dialog.component";
import {ImagePreviewSelectorComponent} from "app/shared/dropdowns/image-preview-selector/image-preview-selector.component";
import {ImagePreviewTypeMeta} from "app/shared/image/image-preview.component";

import {ControlOption} from "framework/ui/control-option";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-data-source-wizard-type-step",
               templateUrl: "./data-source-wizard-type-step.component.html",
               providers  : [WizardStep.createProvider(DataSourceWizardTypeStepComponent)]
           })
export class DataSourceWizardTypeStepComponent extends WizardStep<DataSourceWizardState>
{
    public getMetaFn                                                = (type: Models.TimeSeriesChartType) => this.getMeta(type);
    public typeOptions: ControlOption<Models.TimeSeriesChartType>[] = [];
    public typeSelection: Models.TimeSeriesChartType                = Models.TimeSeriesChartType.STANDARD;

    @ViewChild("test_preview") test_preview: ImagePreviewSelectorComponent<Models.TimeSeriesChartType>;

    public getLabel()
    {
        return this.data.purpose === DataSourceWizardPurpose.visualization ? "Visualization Type" : "Search Method";
    }

    public async onData()
    {
        await super.onData();

        this.updateTypeOptions();
        this.processTypeChange(this.data.type || Models.TimeSeriesChartType.STANDARD);
    }

    private updateTypeOptions()
    {
        if (this.data.purpose === DataSourceWizardPurpose.visualization)
        {
            this.typeOptions = [
                new ControlOption(Models.TimeSeriesChartType.STANDARD, "Line chart"),
                new ControlOption(Models.TimeSeriesChartType.GRAPH, "Line chart with asset structure"),
                new ControlOption(Models.TimeSeriesChartType.HIERARCHICAL, "Hierarchical chart"),
                new ControlOption(Models.TimeSeriesChartType.SCATTER, "Scatter plot"),
                new ControlOption(Models.TimeSeriesChartType.GRAPH_SCATTER, "Scatter plot with asset structure")
            ];

            // If has GPS, add additional option
            if (this.data.hasGps)
            {
                this.typeOptions.push(new ControlOption(Models.TimeSeriesChartType.COORDINATE, "GPS Map"));
            }
        }
        else
        {
            this.typeOptions = [
                new ControlOption(Models.TimeSeriesChartType.STANDARD, "Standard search"),
                new ControlOption(Models.TimeSeriesChartType.GRAPH, "Asset Structure")
            ];
        }

        this.typeOptions = this.typeOptions.filter((option) => this.data.disabledTypes.every((disabled) => disabled !== option.id));
    }

    public isEnabled()
    {
        return this.data.isNew;
    }

    public isValid()
    {
        switch (this.data.type)
        {
            case Models.TimeSeriesChartType.STANDARD:
            case Models.TimeSeriesChartType.HIERARCHICAL:
            case Models.TimeSeriesChartType.COORDINATE:
            case Models.TimeSeriesChartType.GRAPH:
            case Models.TimeSeriesChartType.SCATTER:
            case Models.TimeSeriesChartType.GRAPH_SCATTER:
                return true;
        }
        return false;
    }

    public isNextJumpable()
    {
        return true;
    }

    public async onNext()
    {
        return false;
    }

    public async onStepSelected()
    {
    }

    public processTypeChange(selectedType: Models.TimeSeriesChartType): void
    {
        let previousType = this.data.type;
        let currentType  = selectedType;

        let wasStandardOrScatter    = previousType === Models.TimeSeriesChartType.STANDARD || previousType === Models.TimeSeriesChartType.SCATTER;
        let willBeStandardOrScatter = currentType === Models.TimeSeriesChartType.STANDARD || currentType === Models.TimeSeriesChartType.SCATTER;
        let wasCoordinate           = previousType === Models.TimeSeriesChartType.COORDINATE;
        let willBeCoordinate        = currentType === Models.TimeSeriesChartType.COORDINATE;

        if (wasStandardOrScatter && willBeCoordinate || wasCoordinate && willBeStandardOrScatter)
        {
            this.data.ids = [];
        }

        this.wizard?.markForCheck();
        this.data.type     = currentType;
        this.typeSelection = currentType;
    }

    public getMeta(type: Models.TimeSeriesChartType): ImagePreviewTypeMeta
    {
        return this.data?.purpose === DataSourceWizardPurpose.visualization ? this.getVisualizationMeta(type) : this.getSearchMeta(type);
    }

    private getVisualizationMeta(type: Models.TimeSeriesChartType): ImagePreviewTypeMeta
    {
        switch (type)
        {
            case Models.TimeSeriesChartType.STANDARD:
                return {
                    description: "Allows you to create a line chart using control points found via a text search.",
                    examples   : [
                        {
                            file       : "charts/standard/result.png",
                            label      : "Line Chart",
                            description: "Viewing selected control points as a line chart"
                        },
                        {
                            file       : "charts/standard/select.png",
                            label      : "Search and Select",
                            description: "Searching for individual control points by name"
                        }
                    ]
                };

            case Models.TimeSeriesChartType.HIERARCHICAL:
                return {
                    description: "Allows you to create a hierarchical set of heatmaps or line charts using a control point set defined by an asset structure query.",
                    examples   : [
                        {
                            file       : "charts/hierarchical/result.png",
                            label      : "Hierarchical Charts",
                            description: "View your data as a set of line charts or heatmaps constructed from an asset structure defined hierarchy"
                        },
                        {
                            file       : "charts/hierarchical/create.png",
                            label      : "Asset Structure Selection",
                            description: "Create or select an asset structure to define sets of control points"
                        },
                        {
                            file       : "charts/hierarchical/asset_structure.png",
                            label      : "Configure Asset Structure",
                            description: "Configure your asset structure query as needed"
                        },
                        {
                            file       : "charts/hierarchical/select.png",
                            label      : "Select Data Source",
                            description: "Select your data source from the results of your asset structure query"
                        },
                        {
                            file       : "charts/hierarchical/configure.png",
                            label      : "Configure Hierarchy",
                            description: "Configure how your hierarchy will be ordered and displayed"
                        }
                    ]
                };

            case Models.TimeSeriesChartType.GRAPH:
                return {
                    description: "Allows you to create line charts using a control point set defined by an asset structure query.",
                    examples   : [
                        {
                            file       : "charts/asset_structure/result.gif",
                            label      : "Asset Structure Chart",
                            description: "View your data as a set of line charts or heatmaps constructed from an asset structure"
                        },
                        {
                            file       : "charts/asset_structure/create.png",
                            label      : "Asset Structure Selection",
                            description: "Create or select an asset structure to define sets of control points"
                        },
                        {
                            file       : "charts/asset_structure/asset_structure.png",
                            label      : "Configure Asset Structure",
                            description: "Configure your asset structure query as needed"
                        },
                        {
                            file       : "charts/asset_structure/select.png",
                            label      : "Select Data Source",
                            description: "Select your data source from the results of your asset structure query"
                        }
                    ]
                };

            case Models.TimeSeriesChartType.SCATTER:
                return {
                    description: "Allows you to create configurable scatter plots using control points found via a text search.",
                    examples   : [
                        {
                            file       : "charts/scatter/result_color.png",
                            label      : "Scatter Plot With Colors",
                            description: "View your data as a scatter plot with an extra color encoded dimension"
                        },
                        {
                            file       : "charts/scatter/select.png",
                            label      : "Search and Select",
                            description: "Searching for and selecting individual control points by name"
                        },
                        {
                            file       : "charts/scatter/configure.png",
                            label      : "Configure Panels and Axes",
                            description: "Assigning selected sources to tuples and panels"
                        },
                        {
                            file       : "charts/scatter/result.png",
                            label      : "Scatter Plot",
                            description: "View your data as a scatter plot constructed using manually selected control points"
                        },
                        {
                            file       : "charts/scatter/select_color.png",
                            label      : "Add a Third Control Point",
                            description: "Select 3 or more control points to enable use of a color axis"
                        },
                        {
                            file       : "charts/scatter/configure_color.png",
                            label      : "Configure Panels and Axes",
                            description: "Assigning an additional control point to the color axis varies the color by data"
                        }
                    ]
                };

            case Models.TimeSeriesChartType.GRAPH_SCATTER:
                return {
                    description: "Allows you to create configurable scatter plots using control point sets defined by an asset structure query.",
                    examples   : [
                        {
                            file       : "charts/scatter_asset_structure/result.gif",
                            label      : "Asset Structure Scatter Plot",
                            description: "View your data as a scatter plot constructed from an asset structure using selectable tuples"
                        },
                        {
                            file       : "charts/scatter_asset_structure/create.png",
                            label      : "Asset Structure Selection",
                            description: "Create or select an asset structure to define sets of control points"
                        },
                        {
                            file       : "charts/scatter_asset_structure/asset_structure.png",
                            label      : "Configure Asset Structure",
                            description: "Configure your asset structure query as needed"
                        },
                        {
                            file       : "charts/scatter_asset_structure/configure.png",
                            label      : "Configure Panels and Axes",
                            description: "Assigning selected sources to tuples and panels"
                        }
                    ]
                };

            case Models.TimeSeriesChartType.COORDINATE:
                return {
                    description: "Construct a map to visualize GPS positions over time.",
                    examples   : [
                        {
                            file       : "charts/gps/result.gif",
                            label      : "GPS History Map",
                            description: "View one or multiple GPS histories on a map for a selected time range"
                        }
                    ]
                };
        }

        return null;
    }

    private getSearchMeta(type: Models.TimeSeriesChartType): ImagePreviewTypeMeta
    {
        switch (type)
        {
            case Models.TimeSeriesChartType.STANDARD:
                return {
                    description: "Select control points by searching for them by name.",
                    examples   : [
                        {
                            file       : "sources/standard/select.png",
                            label      : "Search And Select",
                            description: "Search for and select individual control points from your search results"
                        }
                    ]
                };

            case Models.TimeSeriesChartType.HIERARCHICAL:
                return {
                    description: "Select grouped control points via an asset structure.",
                    examples   : [
                        {
                            file       : "sources/hierarchical/create.png",
                            label      : "Create An Asset Structure",
                            description: "Select or create an asset structure to pick control points"
                        },
                        {
                            file       : "sources/hierarchical/asset_structure.png",
                            label      : "Configure Asset Structure",
                            description: "Configure your asset structure query as needed"
                        },
                        {
                            file       : "sources/hierarchical/select.png",
                            label      : "Select Data Source",
                            description: "Select your data source from the results of your asset structure query"
                        }
                    ]
                };

            case Models.TimeSeriesChartType.GRAPH:
                return {
                    description: "Select control points using an asset structure defined in the containing dashboard or chart.",
                    examples   : [
                        {
                            file       : "sources/asset_structure/create.png",
                            label      : "Create An Asset Structure",
                            description: "Select or create an asset structure to pick control points"
                        },
                        {
                            file       : "sources/asset_structure/asset_structure.png",
                            label      : "Configure Asset Structure",
                            description: "Configure your asset structure query as needed"
                        },
                        {
                            file       : "sources/asset_structure/selector.png",
                            label      : "Select Data Source",
                            description: "Select your data source from the results of your asset structure query and select or create a selector control"
                        }
                    ]
                };
        }

        return null;
    }
}
