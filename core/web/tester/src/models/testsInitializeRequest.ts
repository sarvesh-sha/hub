import {Required} from "@tsed/schema";

export class TestsInitializeRequest
{
    @Required()
    public url: string;
}
