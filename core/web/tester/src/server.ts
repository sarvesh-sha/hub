import * as bodyParser from "body-parser";
import * as compression from "compression"; // compresses requests
import * as cors from "cors";
import * as fs from "fs";
import * as logger from "morgan";
import {$log, BeforeRoutesInit, Configuration, Inject, OnReady, PlatformApplication} from "@tsed/common";
import "@tsed/swagger";
import {ConfigService} from "./services/config";
import Path = require("path");

const config = new ConfigService();
const httpPort: any = config.ssl ? false : process.env.PORT || 3100;
const httpsPort: any = config.ssl ? config.ssl.port : false;
const httpsOptions = config.ssl ? {
    key: fs.readFileSync(config.ssl.keyPath),
    cert: fs.readFileSync(config.ssl.certPath),
    passphrase: config.ssl.passPhrase
} : undefined;

const rootDir = Path.resolve(__dirname);


console.error(rootDir)

@Configuration({
    rootDir: rootDir,
    acceptMimes: ["application/json"],
    debug: false,
    port: httpPort,
    httpsPort: httpsPort,
    httpsOptions: httpsOptions,
    endpointUrl: "/api",
    mount: {"/api": "${rootDir}/controllers/**/*.js"},
    componentsScan: ["${rootDir}/services/**/*.js"],
    swagger: [{
        path: "/swagger-ui"
    }],
    statics: {
        "/": [
            {
                root: `${rootDir}/../public`
            }
        ]
    }
})
export class Server implements BeforeRoutesInit, OnReady
{
    @Inject()
    app: PlatformApplication;

    @Configuration()
    settings: Configuration;

    public $beforeRoutesInit(): void | Promise<any>
    {
        this.app
            .use(cors())
            .use(compression())
            .use(bodyParser.json())
            .use(bodyParser.urlencoded({
                extended: true
            }))
            .use(logger("combined"));

        return null;
    }

    public $onReady()
    {
        $log.level = "OFF";
        console.log("Server started...");
    }
}


