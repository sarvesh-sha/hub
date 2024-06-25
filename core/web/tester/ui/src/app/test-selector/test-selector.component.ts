import {Component, Input, OnChanges, SimpleChanges} from "@angular/core";

import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-test-selector",
               templateUrl: "./test-selector.component.html"
           })
export class TestSelectorComponent implements OnChanges
{
    @Input()
    public tests: Models.TestCase[];

    @Input()
    public selectedTests: Set<string>;

    private m_testGroups: TestGroup[];
    @Input() set testGroups(groups: TestGroup[])
    {
        this.m_testGroups = groups;

        this.m_selectedCategories = new Set(this.m_testGroups?.map((group) => group.category));
    }

    get testGroups(): TestGroup[]
    {
        return this.m_testGroups;
    }

    private m_selectedCategories = new Set<string>();

    get selectAllEnabled(): boolean
    {
        return this.testGroups.length > 1;
    }

    private get numAvailableTests(): number
    {
        const availableTests = new Set<string>();
        for (let group of this.testGroups)
        {
            for (let test of group.tests) availableTests.add(test.id);
        }
        return availableTests.size;
    }

    get allSelected(): boolean
    {
        return this.numAvailableTests === this.selectedTests.size;
    }

    get allIndeterminate(): boolean
    {
        return this.selectedTests.size && this.selectedTests.size < this.numAvailableTests;
    }

    set allSelected(selected: boolean)
    {
        for (let group of this.testGroups)
        {
            for (let test of group.tests) this.select(test, selected);
        }
    }

    public readonly expanded = new Map<TestGroup, boolean>();

    //--//

    ngOnChanges(changes: SimpleChanges)
    {
        if (this.selectedTests && this.testGroups && changes.testGroups)
        {
            const prevGroups = changes.testGroups.previousValue || [];

            const newCategories = new Set(this.testGroups.map((group) => group.category));
            for (let group of prevGroups)
            {
                if (!newCategories.has(group.category))
                {
                    for (let test of group.tests) this.select(test, false);
                }
            }

            const oldCategories = new Set(prevGroups.map((group) => group.category));
            for (let group of this.testGroups)
            {
                if (!oldCategories.has(group.category))
                {
                    for (let test of group.tests) this.select(test, true);
                }
            }
        }
    }

    testEnabled(test: Models.TestCase): boolean
    {
        return test.categories.some((category) => this.m_selectedCategories.has(category));
    }

    select(test: Models.TestCase,
           selected: boolean): void
    {
        if (selected)
        {
            this.selectedTests.add(test.id);
        }
        else
        {
            this.selectedTests.delete(test.id);
        }
    }

    selectGroup(group: TestGroup,
                selected: boolean): void
    {
        for (let test of group.tests)
        {
            this.select(test, selected);
        }
    }

    groupSelected(group: TestGroup): boolean
    {
        return !group.tests.some((t) => !this.isSelected(t));
    }

    groupIndeterminate(group: TestGroup): boolean
    {
        return group.tests.some((t) => this.isSelected(t)) && group.tests.some((t) => !this.isSelected(t));
    }

    isSelected(test: Models.TestCase): boolean
    {
        return this.selectedTests.has(test.id);
    }
}

export class TestGroup
{
    constructor(public category: string,
                public tests: Models.TestCase[])
    {
    }
}
