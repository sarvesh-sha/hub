import {HttpClient, HttpEvent, HttpHeaders, HttpParams, HttpRequest, HttpResponse, HttpUrlEncodingCodec} from "@angular/common/http";
import {Injectable} from "@angular/core";

import {ArrayBufferReader, CborDecoder} from "framework/services/cbor";

@Injectable()
export class ApiClientConfiguration
{
    apiDomain: string;

    apiPort: number;

    isNotAuthenticatedError: (code: string) => boolean;

    handleNotAuthenticatedError: () => void;

    errorHandler: (error: ErrorResult) => void;

    redirectToLogin: (url?: string) => void;
}

class CustomHttpUrlEncodingCodec extends HttpUrlEncodingCodec
{
    encodeKey(k: string): string
    {
        k = super.encodeKey(k);
        return k.replace(/\+/gi, "%2B");
    }

    encodeValue(v: string): string
    {
        v = super.encodeValue(v);
        return v.replace(/\+/gi, "%2B");
    }
}

@Injectable()
export class ApiClient
{
    public static disableCBOR: boolean;

    private readonly m_formEncoder = new CustomHttpUrlEncodingCodec();

    constructor(private http: HttpClient,
                public configuration: ApiClientConfiguration)
    {
        console.info(`Creating api client. Domain: ${this.configuration.apiDomain}...`);
    }

    async callWithOptions<T>(path: string,
                             requestOptions: ApiRequest): Promise<T>
    {
        let responseType: "arraybuffer" | "blob" | "json" | "text" = "text";

        // Bypass service workers.
        requestOptions.setHeader("ngsw-bypass", "true");

        let body = this.setConsumeContentType(requestOptions);

        if (requestOptions.produces)
        {
            if (!ApiClient.disableCBOR && requestOptions.produces.indexOf("application/cbor") !== -1)
            {
                requestOptions.setHeader("Accept", "application/cbor");
                responseType = "arraybuffer";
            }
            else if (requestOptions.produces.indexOf("application/json") !== -1)
            {
                requestOptions.setHeader("Accept", "application/json");
                responseType = "json";
            }
            else if (requestOptions.produces.indexOf("application/octet-stream") !== -1)
            {
                requestOptions.setHeader("Accept", "application/octet-stream");
                responseType = "blob";
            }
        }

        // -------------------------

        let request = new HttpRequest<any>(requestOptions.method, path, body, {
            headers        : requestOptions.headers,
            params         : new HttpParams({fromObject: requestOptions.queryParams}),
            responseType   : responseType,
            withCredentials: true // enable credentials (allows app to keep session cookies)
        });

        let response: HttpEvent<T>;

        try
        {
            // create request
            response = await this.http.request<T>(request)
                                 .toPromise();
        }
        catch (error)
        {
            switch (error.status)
            {
                case 412: // handle "application" errors identify during handleHttpSuccess
                    if (error.error)
                    {
                        let result: ErrorResult;

                        if (typeof error.error == "string")
                        {
                            result = <ErrorResult>JSON.parse(error.error);
                        }
                        else
                        {
                            result = <ErrorResult>error.error;
                        }

                        if (this.configuration.errorHandler)
                        {
                            this.configuration.errorHandler(result);
                        }

                        throw result;
                    }
                    break;

                case 401:
                    console.error(`HTTP_ERROR: Status code ${error.status} on url ${error.url}`);

                    throw {
                        code   : "AUTHENTICATION_ERROR",
                        message: "You must be logged in to use the requested feature",
                        context: error.url
                    };

                case 403: // else handle the http errors
                    console.error(`HTTP_ERROR: Status code ${error.status} on url ${error.url}`);

                    throw {
                        code   : "PERMISSION_ERROR",
                        message: "You don't have access to the requested feature",
                        context: error.url
                    };
            }

            // else handle the http errors
            if (error.status)
            {
                console.error(`HTTP_ERROR: Status code ${error.status} on url ${error.url}`);

                throw {
                    code   : "UNEXPECTED_ERROR",
                    message: "An unexpected system error occurred",
                    context: `Status code ${error.status} on url ${error.url}`
                };
            }

            // handle any other errors errors
            console.error(`ERROR: A non-HTTP error occurred: ${error.toString()}`);

            throw {
                code   : "UNEXPECTED_ERROR",
                message: "An unexpected system error occurred",
                context: error.url
            };
        }

        if (response instanceof HttpResponse)
        {
            if (response.status === 204)
            {
                return undefined;
            }
            else
            {
                let contentType = response.headers.get("content-type");
                switch (contentType)
                {
                    case "application/cbor":
                        let reader      = new ArrayBufferReader(<ArrayBuffer><any>response.body);
                        let cborDecoder = new CborDecoder(reader);
                        return <T>cborDecoder.decode();

                    case "application/json":
                        return <T>response.body;

                    case "application/octet-stream":
                        return <T>response.body;

                    case "text/plain":
                        return <T>response.body;

                    default:
                        throw TypeError(`Unsupported response content type (${contentType}).`);
                }
            }
        }

        return undefined;
    }

    private setConsumeContentType(requestOptions: ApiRequest): any
    {
        if (requestOptions.hasBodyParam)
        {
            if (!requestOptions.consumes)
            {
                return this.setConsumeContentType_JSON(requestOptions);
            }

            for (let consume of requestOptions.consumes)
            {
                switch (consume)
                {
                    case "application/json":
                        return this.setConsumeContentType_JSON(requestOptions);

                    case "application/octet-stream":
                        return this.setConsumeContentType_OctetStream(requestOptions);
                }
            }
        }
        else if (requestOptions.hasFormParams)
        {
            requestOptions.setHeader("Content-Type", "application/x-www-form-urlencoded");

            return new HttpParams({
                                      encoder   : this.m_formEncoder,
                                      fromObject: requestOptions.formParams
                                  }).toString();
        }

        if (requestOptions.consumes?.length > 0)
        {
            requestOptions.setHeader("Content-Type", requestOptions.consumes[0]);
        }

        return undefined;
    }

    private setConsumeContentType_JSON(requestOptions: ApiRequest): any
    {
        requestOptions.setHeader("Content-Type", "application/json");
        return requestOptions.body == null ? "" : JSON.stringify(requestOptions.body); // https://github.com/angular/angular/issues/10612
    }

    private setConsumeContentType_OctetStream(requestOptions: ApiRequest): any
    {
        requestOptions.setHeader("Content-Type", "application/octet-stream");
        return requestOptions.body;
    }
}

export class ApiRequest
{
    method: string = "GET";

    formParams: { [param: string]: string | string[] };
    queryParams: { [param: string]: string | string[] };

    headers: HttpHeaders = new HttpHeaders();

    body: any;

    consumes: string[];
    produces: string[];

    hasFormParams: boolean;
    hasBodyParam: boolean;
    // responseType: "arraybuffer" | "blob" | "json" | "text" = "json";

    //--//

    public setConsume(type: string)
    {
        if (!this.consumes) this.consumes = [];

        this.consumes.push(type);
    }

    public setProduce(type: string)
    {
        if (!this.produces) this.produces = [];

        this.produces.push(type);
    }

    public setHeader(name: string,
                     value: string | string[]): void
    {
        this.headers = this.headers.set(name, value);
    }

    public setFormParam(name: string,
                        value: string): void
    {
        if (!this.formParams) this.formParams = {};

        ApiRequest.setParam(this.formParams, name, value);
    }

    public setQueryParam(name: string,
                         value: string): void
    {
        if (!this.queryParams) this.queryParams = {};

        ApiRequest.setParam(this.queryParams, name, value);
    }

    private static setParam(params: { [param: string]: string | string[] },
                            name: string,
                            value: string): void
    {
        if (value === null || value === undefined) return;

        let oldValue = params[name];
        if (oldValue)
        {
            if (Array.isArray(oldValue))
            {
                let array = <string[]>oldValue;
                array.push(value);
            }
            else
            {
                let array = [<string>oldValue];
                array.push(value);
                params[name] = array;
            }
        }
        else
        {
            params[name] = value;
        }
    }
}

export class ErrorResult
{
    code: string;

    message: string;

    exceptionTrace: string;

    validationErrors: ValidationErrors;
}

export class ValidationErrors
{
    entries: ValidationError[];

}

export class ValidationError
{
    field: string;

    reason: string;
}
