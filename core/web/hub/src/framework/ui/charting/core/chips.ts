export class ChartChip
{
    displayValue: string;
    numericValue: number;

    tooltip: string;

    click: () => void;

    possibleActions: ChartChipAction[] = [];

    addAction(name: string,
              tooltip: string,
              execute: (ctx: ChartChip) => Promise<void>)
    {
        let ad     = new ChartChipAction();
        ad.name    = name;
        ad.tooltip = tooltip;
        ad.execute = execute;

        this.possibleActions.push(ad);
    }
}

export class ChartChipAction
{
    name: string;

    tooltip: string;

    execute: (ctx: ChartChip) => Promise<void>;
}
