import {Injectable} from "@angular/core";

import {AppEnvironmentConfiguration, ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";
import {MessageBusService} from "app/services/domain/message-bus.service";
import {RoleExtended, RolesService} from "app/services/domain/roles.service";
import {UserGroupExtended, UserGroupsService} from "app/services/domain/user-groups.service";
import * as Models from "app/services/proxy/model/models";
import {SwService} from "app/sw.service";
import {environment} from "environments/environment";
import {ApiClientConfiguration} from "framework/services/api.client";

import {CacheService} from "framework/services/cache.service";
import {ErrorLevel, ErrorService, RefreshErrorAction} from "framework/services/error.service";
import {Logger, LoggingService} from "framework/services/logging.service";
import {Lookup} from "framework/services/utils.service";

import {Subject} from "rxjs";

//--//

const c_keyForUser = "USER";

@Injectable()
export class UsersService
{
    readonly logger: Logger;

    isInitializing: boolean  = false;
    isInitialized: boolean   = false;
    isAuthenticated: boolean = false;

    /**
     * The current user
     */
    user: Models.User;

    /**
     * The current user's roles.
     */
    userRoles: RoleExtended[];

    /**
     * The current user's groups.
     */
    userGroups: UserGroupExtended[];

    /**
     * True if the instance is using demo data.
     */
    public hasDemoData: boolean;

    /**
     * The type of instance configuration, which defines the vertical we are in.
     */
    public instanceConfiguration: Models.InstanceConfiguration;

    // events
    loggedIn: Subject<void>  = new Subject<void>();
    loggedOut: Subject<void> = new Subject<void>();

    constructor(private api: ApiService,
                private appConfig: AppEnvironmentConfiguration,
                private errors: ErrorService,
                private cache: CacheService,
                private roles: RolesService,
                private groups: UserGroupsService,
                private logService: LoggingService,
                private mb: MessageBusService,
                private sw: SwService,
                configuration: ApiClientConfiguration)
    {
        this.logger = logService.getLogger(UsersService);
        this.loggedIn.subscribe({next: () => this.mb.onLogin()});
        this.loggedOut.subscribe({next: () => this.mb.onLogout()});

        configuration.handleNotAuthenticatedError = () =>
        {
            if (this.isAuthenticated)
            {
                configuration.redirectToLogin();
            }
        };
    }

    /**
     * Returns true if a user has logged in previously but not yet logged out.
     */
    wasPreviouslyAuthenticated(): boolean
    {
        if (this.cache.getValueAs<Models.User>(c_keyForUser)) return true;
        return false;
    }

    /**
     * Initialized the user service.
     */
    async init(credentials?: SingleSignOnCredentials)
    {
        if (!this.isInitializing)
        {
            this.logger.info("Initializing User services...");

            this.isInitializing = true;

            await this.checkin(credentials);
        }
    }

    /**
     * Perform a checkin.
     */
    async checkin(credentials?: SingleSignOnCredentials): Promise<Models.User>
    {
        // attempt a login, if this fails, our session has expired / never been established
        try
        {
            await this.validateAppVersion();

            let user: Models.User;

            if (credentials != null)
            {
                user = await this.api.users.login(credentials.username, credentials.password);
            }
            else
            {
                user = await this.api.users.login();
            }

            this.logger.info("User logged in with valid session.");

            // if successful, make sure we have the latest user info stored
            await this.processLogin(user);

            // flag that initializing has completed
            this.isInitializing = false;
            this.isInitialized  = true;

            return user;
        }
        catch (error)
        {
            this.logger.error("User not logged in or session expired.");

            // log the user out
            this.flushUser();

            // flag that initializing has completed
            this.isInitializing = false;
            this.isInitialized  = true;

            throw error;
        }
    }

    /**
     * Verify user is logged in.
     */
    checkLoggedIn(): Promise<Models.User>
    {
        // attempt a login, if this fails, our session has expired / never been established
        return this.api.users.login();
    }

    /**
     * Log in to the application.
     * @param username
     * @param password
     */
    async login(username: string,
                password: string): Promise<Models.User>
    {
        this.user            = null;
        this.isAuthenticated = false;
        this.errors.dismiss();

        this.logger.info("Logging in...");

        try
        {
            // attempt the login
            let user = await this.api.users.login(username, password);

            // if success, process the returned user
            if (user) await this.processLogin(user);

            return user;
        }
        catch (err)
        {
            // Nope, we couldn't log in.
            return null;
        }
    }

    /**
     * Impersonate another user.
     * @param userSysId
     */
    @ReportError
    async impersonate(userSysId: string): Promise<Models.User>
    {
        this.flushUser();

        let user = await this.api.users.impersonate(userSysId);

        // if success, process the returned user
        if (user) await this.processLogin(user);

        return user;
    }

    async validateAppVersion()
    {
        let appVersion     = await this.api.adminTasks.getAppVersion();
        let currentVersion = this.appConfig.version;
        if (environment.production && currentVersion && currentVersion != appVersion)
        {
            try
            {
                setTimeout(() => this.reportVersionError(), 30000);
                if (!await this.sw.forceUpdate())
                {
                    this.reportVersionError();
                    return;
                }
            }
            catch (e)
            {
                // We failed to fully load the new service worker.
                this.reportVersionError();
                return;
            }

            console.log("###### RELOADING ######");
            window.location.reload();
            return;
        }

        this.instanceConfiguration = await this.api.adminTasks.getInstanceConfiguration();
    }

    private reportVersionError(): void
    {
        this.errors.notify("Error downloading the latest version. Please refresh the page and try again.", [new RefreshErrorAction()], ErrorLevel.Fatal);
    }

    /**
     * Process a user login operation.
     * @param user
     */
    private async processLogin(user: Models.User): Promise<void>
    {
        // capture our user info
        this.user = user;

        if (this.user)
        {
            this.isAuthenticated = true;

            this.hasDemoData = await this.api.demoTasks.hasDemoData();

            // get the user's roles
            this.userRoles = await this.roles.getExtendedBatch(this.user.roles);

            // get the user's roles
            this.userGroups = await this.groups.getExtendedBatch(this.user.groups);

            // set our cache user context
            this.cache.init();
            this.cache.set(c_keyForUser, user);

            // raise the login event.
            this.loggedIn.next();
        }
        else
        {
            // we aren't authed any more
            this.isAuthenticated = false;
            this.cache.remove(c_keyForUser);
        }
    }

    /**
     * Create a new user profile
     */
    @ReportError
    async createUser(user: Models.UserCreationRequest): Promise<Models.User>
    {
        return await this.api.users.create(user);
    }

    /**
     * Update the user profile
     */
    @ReportError
    async saveUser(user: Models.User): Promise<Models.User>
    {
        await this.api.users.update(user.sysId, undefined, user);

        return this.api.users.get(user.sysId);
    }

    /**
     * Refresh the current user profile
     */
    @ReportError
    async refreshCurrentUser(): Promise<void>
    {
        this.user = await this.api.users.get(this.user.sysId);
    }

    /**
     * Delete a user.
     */
    @ReportError
    async deleteUser(user: Models.User): Promise<Models.ValidationResults>
    {
        return await this.api.users.remove(user.sysId);
    }

    /**
     * Request password reset.
     * @param emailAddress
     */
    @ReportError
    async forgotPassword(emailAddress: string): Promise<Models.User>
    {
        if (emailAddress)
        {
            let user = await this.api.users.forgotPassword(emailAddress);
            this.errors.success("Your request has been submitted. Check your email for further instructions", -1);
            return user;
        }
        else
        {
            throw this.errors.error("MISSING_INFO", "You must provide an email address.");
        }
    }

    /**
     * Reset the current user's password.
     * @param emailAddress
     * @param token
     * @param password
     * @param passwordConfirmation
     */
    @ReportError
    async resetPassword(emailAddress: string,
                        token: string,
                        password: string,
                        passwordConfirmation: string): Promise<Models.User>
    {
        if (!token)
        {
            throw this.errors.error("MISSING_INFO", "You must reset your password through the link sent to your inbox.");
        }

        if (password != passwordConfirmation)
        {
            throw this.errors.error("PASSWORDS_DONT_MATCH", "Your password confirmation does not match.");
        }

        return this.api.users.resetPassword(emailAddress, token, password);
    }

    /**
     * Change the current user's password.
     * @param currentPassword
     * @param password
     */
    @ReportError
    async changePassword(currentPassword: string,
                         password: string): Promise<Models.User>
    {
        let user = await this.api.users.changePassword(this.user.sysId, currentPassword, password);
        await this.processLogin(user);
        this.errors.success("Password updated successfully", -1);
        return user;
    }

    /**
     * Log out of the application.
     */
    async logout(): Promise<string>
    {
        this.logger.info("Logging out...");

        try
        {
            return await this.api.users.logout();
        }
        catch (error)
        {
            this.logger.error(`Error performing server log out: ${error}`);
            return null;
        }
        finally
        {
            this.flushUser();
        }
    }

    /**
     * Flush user information.
     */
    private flushUser()
    {
        this.isAuthenticated = false;
        this.user            = null;
        this.cache.remove(c_keyForUser);

        // raise the logout event.
        this.loggedOut.next();
    }

    /**
     * True if the current user has the "User" role.
     */
    hasUserRole(): boolean
    {
        return this.hasRole("SYS.USER");
    }

    /**
     * True if the current user has the "Admin" role.
     */
    hasAdminRole(): boolean
    {
        return this.hasRole("SYS.ADMIN");
    }

    /**
     * True if the current user has the "Maint" role.
     */
    hasMaintRole(): boolean
    {
        return this.hasRole("SYS.MAINT");
    }

    /**
     * True if the current user has the "Publisher" role.
     */
    hasPublisherRole(): boolean
    {
        return this.hasRole("SYS.PUBLISHER");
    }

    /**
     * True if the current user has the specified role.
     */
    hasRole(roleName: string): boolean
    {
        for (let role of (this.userRoles || []))
        {
            if (role.model.name == roleName) return true;
        }
        return false;
    }

    mapRoles(selections: Lookup<boolean>): Models.RecordIdentity[]
    {
        let array: Models.RecordIdentity[] = [];

        for (let sysId in selections)
        {
            if (selections.hasOwnProperty(sysId) && selections[sysId])
            {
                array.push(RoleExtended.newIdentity(sysId));
            }
        }

        return array;
    }

    mapRolesFromName(selections: Lookup<boolean>): string[]
    {
        let array: string[] = [];

        for (let name in selections)
        {
            if (selections.hasOwnProperty(name) && selections[name])
            {
                array.push(name);
            }
        }

        return array;
    }

    /**
     * True if the current user has any of the specified role.
     */
    hasAnyRole(roleNames: string[]): boolean
    {
        if (roleNames && roleNames.length)
        {
            for (let roleName of roleNames)
            {
                if (this.hasRole(roleName)) return true;
            }
        }

        return false;
    }

    /**
     * Get the preference nested under the target path.
     * @param path
     */
    async getPreferenceSubkeys(path: string): Promise<string[]>
    {
        if (this.user?.sysId)
        {
            return this.api.userPreferences.listSubKeys(this.user.sysId, path);
        }

        return [];
    }

    /**
     * Get the preference values by their path.
     * @param path
     */
    async getPreferenceValues(path: string): Promise<string[]>
    {
        if (this.user?.sysId)
        {
            return this.api.userPreferences.listValues(this.user.sysId, path);
        }

        return [];
    }

    /**
     * Get a preference by its key.
     * @param path
     * @param name
     */
    async getPreference(path: string,
                        name: string): Promise<string>
    {
        if (this.user?.sysId)
        {
            let res = await this.api.userPreferences.getValue(this.user.sysId, path, name);
            if (res)
            {
                return res.value;
            }
        }

        return null;
    }

    /**
     * Get a preference value by its key.
     * @param path
     * @param name
     * @param fixup
     */
    async getTypedPreference<T>(path: string,
                                name: string,
                                fixup: (value: T) => void): Promise<T>
    {
        let value = await this.getPreference(path, name);
        if (value)
        {
            let valueTyped = <T>JSON.parse(value);
            if (valueTyped) fixup(valueTyped);
            return valueTyped;
        }

        return null;
    };

    /**
     * Set a preference value.
     * @param path
     * @param name
     * @param value
     */
    setPreference(path: string,
                  name: string,
                  value: string): Promise<string>
    {
        if (this.user?.sysId)
        {
            return this.api.userPreferences.setValue(this.user.sysId, path, name, value);
        }

        return null;
    }

    /**
     * Set a preference value.
     * @param path
     * @param name
     * @param value
     */
    setTypedPreference<T>(path: string,
                          name: string,
                          value: T): Promise<string>
    {
        if (this.user?.sysId)
        {
            return this.api.userPreferences.setValue(this.user.sysId, path, name, JSON.stringify(value));
        }

        return null;
    }

    /**
     * Checks the value of a typed preference entry.
     * @param path
     * @param name
     * @param value
     * @param fixup
     */
    async checkTypedPreferenceValue<T>(path: string,
                                       name: string,
                                       value: string,
                                       fixup: (value: T) => void): Promise<T>
    {
        if (this.user?.sysId)
        {
            if (await this.api.userPreferences.checkValueFormat(this.user.sysId, path, name, value))
            {
                let valueTyped = <T>JSON.parse(value);
                if (valueTyped) fixup(valueTyped);
                return valueTyped;
            }
        }

        return null;
    }

    /**
     * Delete a preference value.
     * @param path
     * @param name
     */
    removePreference(path: string,
                     name: string): Promise<boolean>
    {
        if (this.user?.sysId)
        {
            return this.api.userPreferences.removeValue(this.user.sysId, path, name);
        }

        return null;
    }
}

export class SingleSignOnCredentials
{
    username: string;
    password: string;
}
