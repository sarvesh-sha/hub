export enum SortDirection
{
    ASCENDING  = 0,
    DESCENDING = 1
}

export function numericSortBy(property: string,
                              order: SortDirection)
{
    let dir = order === SortDirection.ASCENDING ? 1 : -1;
    return function (a: any,
                     b: any): number
    {
        return dir * a[property] - dir * b[property];
    };
}
