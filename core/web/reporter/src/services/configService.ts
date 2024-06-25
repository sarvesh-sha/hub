import * as fs from "fs";
import * as yaml from "js-yaml";
import {Service} from "@tsed/common";

@Service()
export class ConfigService
{
    public ssl: {
        port: number;
        keyPath: string;
        certPath: string;
        passPhrase: string;
    };

    public reports: {
        directory: string;
        storageTimeMinutes: number;
        maxConcurrentPages: number;
        allowedDomains: string[];
    };

    constructor()
    {
        const config = yaml.load(fs.readFileSync(process.argv[2] || "reporter.yml", "utf8"));
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
