import {CollectionOf} from "@tsed/schema";
import {TestsInitializeRequest} from "./testsInitializeRequest";

export class TestsRunRequest extends TestsInitializeRequest
{
    @CollectionOf(String)
    public ids: string[];

    @CollectionOf(String)
    public categories?: string[];
}
