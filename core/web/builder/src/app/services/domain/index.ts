import {Injectable, NgModule} from "@angular/core";

import {ApiService} from "app/services/domain/api.service";
import {AuthGuard} from "app/services/domain/auth.guard";
import {BackgroundActivitiesService} from "app/services/domain/background-activities.service";
import {CustomerServiceBackupsService} from "app/services/domain/customer-service-backups.service";
import {CustomerServiceSecretsService} from "app/services/domain/customer-service-secrets.service";
import {CustomerServicesService} from "app/services/domain/customer-services.service";
import {CustomerSharedSecretsService} from "app/services/domain/customer-shared-secrets.service";
import {CustomerSharedUsersService} from "app/services/domain/customer-shared-users.service";
import {CustomersService} from "app/services/domain/customers.service";
import {DatabaseActivityService} from "app/services/domain/database-activity.service";
import {DeploymentAgentsService} from "app/services/domain/deployment-agents.service";
import {DeploymentHostFilesService} from "app/services/domain/deployment-host-files.service";
import {DeploymentHostImagePullsService} from "app/services/domain/deployment-host-image-pulls.service";
import {DeploymentHostsService} from "app/services/domain/deployment-hosts.service";
import {DeploymentTasksService} from "app/services/domain/deployment-tasks.service";
import {EnumsService} from "app/services/domain/enums.service";
import {HostsService} from "app/services/domain/hosts.service";
import {JobDefinitionStepsService} from "app/services/domain/job-definition-steps.service";
import {JobDefinitionsService} from "app/services/domain/job-definitions.service";
import {JobSourcesService} from "app/services/domain/job-sources.service";
import {JobStepsService} from "app/services/domain/job-steps.service";
import {JobsService} from "app/services/domain/jobs.service";
import {MessageBusService} from "app/services/domain/message-bus.service";
import {RegistryImagesService} from "app/services/domain/registry-images.service";
import {RegistryTaggedImagesService} from "app/services/domain/registry-tagged-images.service";
import {RepositoriesService} from "app/services/domain/repositories.service";
import {RepositoryBranchesService} from "app/services/domain/repository-branches.service";
import {RepositoryCheckoutsService} from "app/services/domain/repository-checkouts.service";
import {RepositoryCommitsService} from "app/services/domain/repository-commits.service";
import {RolesService} from "app/services/domain/roles.service";
import {SearchService} from "app/services/domain/search.service";
import {SettingsService} from "app/services/domain/settings.service";
import {UserManagementService} from "app/services/domain/user-management.service";
import {UsersService} from "app/services/domain/users.service";

@Injectable()
export class AppDomainContext
{
    constructor(public apis: ApiService,
                public backgroundActivities: BackgroundActivitiesService,
                public customers: CustomersService,
                public customerServices: CustomerServicesService,
                public customerServiceBackups: CustomerServiceBackupsService,
                public customerServiceSecrets: CustomerServiceSecretsService,
                public customerSharedSecrets: CustomerSharedSecretsService,
                public customerSharedUsers: CustomerSharedUsersService,
                public deploymentAgents: DeploymentAgentsService,
                public deploymentHosts: DeploymentHostsService,
                public deploymentHostFiles: DeploymentHostFilesService,
                public deploymentHostImagePulls: DeploymentHostImagePullsService,
                public deploymentTasks: DeploymentTasksService,
                public enums: EnumsService,
                public hosts: HostsService,
                public jobDefinitions: JobDefinitionsService,
                public jobDefinitionSteps: JobDefinitionStepsService,
                public jobSources: JobSourcesService,
                public jobSteps: JobStepsService,
                public jobs: JobsService,
                public registryImages: RegistryImagesService,
                public registryTaggedImages: RegistryTaggedImagesService,
                public repositories: RepositoriesService,
                public repositoryBranches: RepositoryBranchesService,
                public repositoryCheckouts: RepositoryCheckoutsService,
                public repositoryCommits: RepositoryCommitsService,
                public roles: RolesService,
                public search: SearchService,
                public settings: SettingsService,
                public users: UsersService,
                public userManagement: UserManagementService)
    {
    }
}

@NgModule({
              providers: [
                  ApiService,
                  AuthGuard,
                  BackgroundActivitiesService,
                  CustomersService,
                  CustomerServicesService,
                  CustomerServiceBackupsService,
                  CustomerServiceSecretsService,
                  CustomerSharedSecretsService,
                  CustomerSharedUsersService,
                  DatabaseActivityService,
                  DeploymentAgentsService,
                  DeploymentHostsService,
                  DeploymentHostImagePullsService,
                  DeploymentHostFilesService,
                  DeploymentTasksService,
                  EnumsService,
                  HostsService,
                  JobDefinitionsService,
                  JobDefinitionStepsService,
                  JobSourcesService,
                  JobStepsService,
                  JobsService,
                  MessageBusService,
                  RegistryImagesService,
                  RegistryTaggedImagesService,
                  RepositoriesService,
                  RepositoryBranchesService,
                  RepositoryCheckoutsService,
                  RepositoryCommitsService,
                  RolesService,
                  SearchService,
                  SettingsService,
                  UsersService,
                  UserManagementService,
                  AppDomainContext
              ]
          })
export class DomainModule {}
