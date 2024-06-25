import {CollectionOf, Property} from "@tsed/schema";

export class TestCase
{
    @Property()
    public id: string;

    @Property()
    public name: string;

    @Property()
    public timeout?: number;

    @CollectionOf(String)
    public categories: string[];
}
