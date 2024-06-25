// import dependencies
import {Injectable, Injector} from "@angular/core";

import * as Apis from "app/services/proxy/api/api";

import {ApiClient} from "framework/services/api.client";

@Injectable()
export class ApiService
{
    readonly basePath: string;

    adminTasks: Apis.AdminTasksApi;
    backgroundActivities: Apis.BackgroundActivitiesApi;
    customers: Apis.CustomersApi;
    customerServices: Apis.CustomerServicesApi;
    customerServiceBackups: Apis.CustomerServiceBackupsApi;
    customerServiceSecrets: Apis.CustomerServiceSecretsApi;
    customerSharedSecrets: Apis.CustomerSharedSecretsApi;
    customerSharedUsers: Apis.CustomerSharedUsersApi;
    deploymentAgents: Apis.DeploymentAgentsApi;
    deploymentHosts: Apis.DeploymentHostsApi;
    deploymentHostFiles: Apis.DeploymentHostFilesApi;
    deploymentHostImagePulls: Apis.DeploymentHostImagePullsApi;
    deploymentHostProvisioning: Apis.DeploymentHostProvisioningApi;
    deploymentTasks: Apis.DeploymentTasksApi;
    dockerContainers: Apis.DockerContainersApi;
    dockerVolumes: Apis.DockerVolumesApi;
    enums: Apis.EnumsApi;
    exports: Apis.ExportsApi;
    hosts: Apis.HostsApi;
    jobDefinitionSteps: Apis.JobDefinitionStepsApi;
    jobDefinitions: Apis.JobDefinitionsApi;
    jobSources: Apis.JobSourcesApi;
    jobSteps: Apis.JobStepsApi;
    jobs: Apis.JobsApi;
    managedDirectories: Apis.ManagedDirectoriesApi;
    registryImages: Apis.RegistryImagesApi;
    registryTaggedImages: Apis.RegistryTaggedImagesApi;
    repositories: Apis.RepositoriesApi;
    repositoryBranches: Apis.RepositoryBranchesApi;
    repositoryCheckouts: Apis.RepositoryCheckoutsApi;
    repositoryCommits: Apis.RepositoryCommitsApi;
    roles: Apis.RolesApi;
    search: Apis.SearchApi;
    systemPreferences: Apis.SystemPreferencesApi;
    userPreferences: Apis.UserPreferencesApi;
    users: Apis.UsersApi;

    constructor(private client: ApiClient,
                public injector: Injector)
    {
        let origin   = this.client.configuration.apiDomain;
        let location = document.location;

        if (!origin || origin == "")
        {
            if (this.client.configuration.apiPort)
            {
                origin = `${location.protocol}//${location.hostname}:${this.client.configuration.apiPort}`;
            }
            else
            {
                origin = location.origin;
            }
        }

        this.basePath = origin + "/api/v1";

        this.adminTasks                 = new Apis.AdminTasksApi(client, this.basePath);
        this.backgroundActivities       = new Apis.BackgroundActivitiesApi(client, this.basePath);
        this.customers                  = new Apis.CustomersApi(client, this.basePath);
        this.customerServices           = new Apis.CustomerServicesApi(client, this.basePath);
        this.customerServiceBackups     = new Apis.CustomerServiceBackupsApi(client, this.basePath);
        this.customerServiceSecrets     = new Apis.CustomerServiceSecretsApi(client, this.basePath);
        this.customerSharedSecrets      = new Apis.CustomerSharedSecretsApi(client, this.basePath);
        this.customerSharedUsers        = new Apis.CustomerSharedUsersApi(client, this.basePath);
        this.deploymentAgents           = new Apis.DeploymentAgentsApi(client, this.basePath);
        this.deploymentHosts            = new Apis.DeploymentHostsApi(client, this.basePath);
        this.deploymentHostFiles        = new Apis.DeploymentHostFilesApi(client, this.basePath);
        this.deploymentHostProvisioning = new Apis.DeploymentHostProvisioningApi(client, this.basePath);
        this.deploymentHostImagePulls   = new Apis.DeploymentHostImagePullsApi(client, this.basePath);
        this.deploymentTasks            = new Apis.DeploymentTasksApi(client, this.basePath);
        this.dockerContainers           = new Apis.DockerContainersApi(client, this.basePath);
        this.dockerVolumes              = new Apis.DockerVolumesApi(client, this.basePath);
        this.enums                      = new Apis.EnumsApi(client, this.basePath);
        this.exports                    = new Apis.ExportsApi(client, this.basePath);
        this.hosts                      = new Apis.HostsApi(client, this.basePath);
        this.jobDefinitionSteps         = new Apis.JobDefinitionStepsApi(client, this.basePath);
        this.jobDefinitions             = new Apis.JobDefinitionsApi(client, this.basePath);
        this.jobSources                 = new Apis.JobSourcesApi(client, this.basePath);
        this.jobSteps                   = new Apis.JobStepsApi(client, this.basePath);
        this.jobs                       = new Apis.JobsApi(client, this.basePath);
        this.managedDirectories         = new Apis.ManagedDirectoriesApi(client, this.basePath);
        this.registryImages             = new Apis.RegistryImagesApi(client, this.basePath);
        this.registryTaggedImages       = new Apis.RegistryTaggedImagesApi(client, this.basePath);
        this.repositories               = new Apis.RepositoriesApi(client, this.basePath);
        this.repositoryBranches         = new Apis.RepositoryBranchesApi(client, this.basePath);
        this.repositoryCheckouts        = new Apis.RepositoryCheckoutsApi(client, this.basePath);
        this.repositoryCommits          = new Apis.RepositoryCommitsApi(client, this.basePath);
        this.roles                      = new Apis.RolesApi(client, this.basePath);
        this.search                     = new Apis.SearchApi(client, this.basePath);
        this.systemPreferences          = new Apis.SystemPreferencesApi(client, this.basePath);
        this.userPreferences            = new Apis.UserPreferencesApi(client, this.basePath);
        this.users                      = new Apis.UsersApi(client, this.basePath);
    }
}
