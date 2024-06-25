import {Service} from "@tsed/common";
import * as fs from "fs";
import * as yaml from "js-yaml";

@Service()
export class ConfigService
{
    public tester: {
        record: boolean;
        headless: boolean;
        outputDirectory: string;
    };

    public ssl: {
        port: number;
        keyPath: string;
        certPath: string;
        passPhrase: string;
    };

    constructor()
    {
        const config = yaml.load(fs.readFileSync(process.argv[2] || "tester.yml", "utf8"));
        deepExtend(this, config);
    }
}

function deepExtend(destination: any,
                    source: any)
{
    for (const property in source)
    {
        if (source[property] && source[property].constructor && source[property].constructor === Object)
        {
            destination[property] = destination[property] || {};
            deepExtend(destination[property], source[property]);
        }
        else
        {
            destination[property] = source[property];
        }
    }
    return destination;
}
