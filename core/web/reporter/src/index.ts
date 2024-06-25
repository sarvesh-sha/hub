import {$log} from "@tsed/common";
import {PlatformExpress} from "@tsed/platform-express";
import {Server} from "./app";

async function bootstrap()
{
    try
    {
        $log.debug("Start server...");
        const platform = await PlatformExpress.bootstrap(Server, {
            // extra settings
        });

        await platform.listen();
        $log.debug("Server initialized");
    }
    catch (er)
    {
        $log.error(er);
    }
}


process.on("unhandledRejection",
           err =>
           {
               console.error("Unhandled Rejection: ", err);
           });

bootstrap();
