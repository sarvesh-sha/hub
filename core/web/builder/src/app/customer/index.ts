import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {FormsModule} from "@angular/forms";
import {BrowserModule} from "@angular/platform-browser";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {RouterModule} from "@angular/router";
import {TreeModule} from "@circlon/angular-tree-component";
import {AngularDraggableModule} from "angular2-draggable";

import {UsersCreationPageComponent} from "app/customer/configuration/users/users-creation-page.component";
import {UsersDetailPageComponent} from "app/customer/configuration/users/users-detail-page.component";
import {UsersListPageComponent} from "app/customer/configuration/users/users-list-page.component";
import {ConsumerServiceBackupsDetailPageComponent} from "app/customer/customer-services/customer-service-backups-detail-page.component";
import {CustomerServiceBackupsListComponent} from "app/customer/customer-services/customer-service-backups-list.component";
import {CustomerServiceInstancesListComponent} from "app/customer/customer-services/customer-service-instances-list.component";

import {CustomerServiceSelectionDialogComponent} from "app/customer/customer-services/customer-service-selection-dialog.component";
import {CustomerServicesDetailPageLogComponent} from "app/customer/customer-services/customer-services-detail-page-log.component";
import {CustomerServicesDetailPageComponent} from "app/customer/customer-services/customer-services-detail-page.component";

import {CustomerServicesListComponent} from "app/customer/customer-services/customer-services-list.component";
import {CustomerServiceSecretsCreationPageComponent} from "app/customer/customer-services/secrets/customer-service-secrets-creation-page.component";
import {CustomerServiceSecretsDetailPageComponent} from "app/customer/customer-services/secrets/customer-service-secrets-detail-page.component";
import {CustomerServiceSecretsListComponent} from "app/customer/customer-services/secrets/customer-service-secrets-list.component";
import {ServiceRoleSelectionDialogComponent} from "app/customer/customer-services/service-role-selection-dialog.component";
import {CustomerSelectionDialogComponent} from "app/customer/customers/customer-selection-dialog.component";
import {CustomersDetailPageComponent} from "app/customer/customers/customers-detail-page.component";
import {CustomersListComponent} from "app/customer/customers/customers-list.component";

import {CustomersSummaryPageComponent} from "app/customer/customers/customers-summary-page.component";
import {CustomerSharedSecretsCreationPageComponent} from "app/customer/customers/shared-secrets/customer-shared-secrets-creation-page.component";
import {CustomerSharedSecretsDetailPageComponent} from "app/customer/customers/shared-secrets/customer-shared-secrets-detail-page.component";
import {CustomerSharedSecretsListComponent} from "app/customer/customers/shared-secrets/customer-shared-secrets-list.component";
import {CustomerSharedUsersCreationPageComponent} from "app/customer/customers/shared-users/customer-shared-users-creation-page.component";
import {CustomerSharedUsersDetailPageComponent} from "app/customer/customers/shared-users/customer-shared-users-detail-page.component";
import {CustomerSharedUsersListComponent} from "app/customer/customers/shared-users/customer-shared-users-list.component";
import {DeploymentAgentUpgradeOverlay} from "app/customer/deployment-agents/deployment-agent-upgrade-overlay.component";
import {DeploymentAgentsDetailPageComponent} from "app/customer/deployment-agents/deployment-agents-detail-page.component";

import {DeploymentAgentsListComponent} from "app/customer/deployment-agents/deployment-agents-list.component";
import {DeploymentChargesSummaryComponent} from "app/customer/deployment-charges/deployment-charges-summary.component";
import {DeploymentHostDelayedOpsListComponent} from "app/customer/deployment-hosts/deployment-host-delayedops-list.component";
import {DeploymentHostFilesListComponent} from "app/customer/deployment-hosts/deployment-host-files-list.component";
import {DeploymentHostImagePullsListComponent} from "app/customer/deployment-hosts/deployment-host-image-pulls-list.component";
import {DeploymentHostImagesListComponent} from "app/customer/deployment-hosts/deployment-host-images-list.component";
import {DeploymentHostProvisioningListComponent} from "app/customer/deployment-hosts/deployment-host-provisioning-list.component";
import {DeploymentHostsDetailPageLogComponent} from "app/customer/deployment-hosts/deployment-hosts-detail-page-log.component";
import {DeploymentHostsDetailPageComponent} from "app/customer/deployment-hosts/deployment-hosts-detail-page.component";
import {DeploymentHostsListComponent} from "app/customer/deployment-hosts/deployment-hosts-list.component";

import {DeploymentHostsSummaryPageComponent} from "app/customer/deployment-hosts/deployment-hosts-summary-page.component";
import {TestConfigurationDialogComponent} from "app/customer/deployment-hosts/test-configuration-selection-dialog.component";
import {DeploymentTasksDetailPageComponent} from "app/customer/deployment-tasks/deployment-tasks-detail-page.component";

import {DeploymentTasksListComponent} from "app/customer/deployment-tasks/deployment-tasks-list.component";
import {HostsDetailPageLogComponent} from "app/customer/hosts/hosts-detail-page-log.component";
import {HostsDetailPageComponent} from "app/customer/hosts/hosts-detail-page.component";
import {HostsListComponent} from "app/customer/hosts/hosts-list/hosts-list.component";
import {HostsSummaryPageComponent} from "app/customer/hosts/hosts-summary-page.component";
import {JobDefinitionStepsDetailPageComponent} from "app/customer/job-definition-steps/job-definition-steps-detail-page.component";

import {JobDefinitionStepsListComponent} from "app/customer/job-definition-steps/job-definition-steps-list.component";
import {JobDefinitionsDetailPageComponent} from "app/customer/job-definitions/job-definitions-detail-page.component";
import {JobDefinitionsListComponent} from "app/customer/job-definitions/job-definitions-list.component";

import {JobDefinitionsSummaryPageComponent} from "app/customer/job-definitions/job-definitions-summary-page.component";
import {JobImagesDetailPageComponent} from "app/customer/job-images/job-images-detail-page.component";

import {JobImagesListComponent} from "app/customer/job-images/job-images-list.component";
import {JobStepsListComponent} from "app/customer/job-steps/job-steps-list.component";
import {JobsDetailPageComponent} from "app/customer/jobs/jobs-detail-page.component";
import {JobsListComponent} from "app/customer/jobs/jobs-list.component";
import {JobsSummaryPageComponent} from "app/customer/jobs/jobs-summary-page.component";

import {BackgroundActivitiesListComponent} from "app/customer/maintenance/background-activities/background-activities-list.component";
import {BackgroundActivitiesSummaryPageComponent} from "app/customer/maintenance/background-activities/background-activities-summary-page.component";
import {BackgroundActivityDetailPageComponent} from "app/customer/maintenance/background-activities/background-activity-detail-page.component";
import {LoggersPageComponent} from "app/customer/maintenance/loggers/loggers-page.component";
import {LoggersComponent} from "app/customer/maintenance/loggers/loggers.component";
import {ProvisioningLabelsComponent} from "app/customer/maintenance/provisioning/provisioning-labels.component";
import {ProvisioningPageComponent} from "app/customer/maintenance/provisioning/provisioning-page.component";
import {ThreadsPageComponent} from "app/customer/maintenance/threads/threads-page.component";

import {RegistryImageSelectionDialogComponent} from "app/customer/registry-images/registry-image-selection-dialog.component";

import {RegistryTaggedImagesDetailPageComponent} from "app/customer/registry-tagged-images/registry-tagged-images-detail-page.component";
import {RegistryTaggedImagesListComponent} from "app/customer/registry-tagged-images/registry-tagged-images-list.component";
import {RegistryTaggedImagesSummaryPageComponent} from "app/customer/registry-tagged-images/registry-tagged-images-summary-page.component";

import {RepositoriesDetailPageComponent} from "app/customer/repositories/repositories-detail-page.component";
import {RepositoriesListComponent} from "app/customer/repositories/repositories-list.component";

import {RepositoriesSummaryPageComponent} from "app/customer/repositories/repositories-summary-page.component";
import {RepositoryBranchSelectionDialogComponent} from "app/customer/repository-branches/repository-branch-selection-dialog.component";
import {RepositoryCheckoutsDetailPageComponent} from "app/customer/repository-checkouts/repository-checkouts-detail-page.component";

import {RepositoryCheckoutsListComponent} from "app/customer/repository-checkouts/repository-checkouts-list.component";
import {RepositoryCommitSelectionDialogComponent} from "app/customer/repository-commits/repository-commit-selection-dialog.component";
import {SearchResultsPageComponent} from "app/customer/search/search-results-page.component";
import {StatisticsBackupsUsagePageComponent} from "app/customer/statistics/statistics-backups-usage-page.component";
import {StatisticsCellularChargesPageComponent} from "app/customer/statistics/statistics-cellular-charges-page.component";
import {StatisticsDatagramSessionsPageComponent} from "app/customer/statistics/statistics-datagram-sessions-page.component";
import {StatisticsDeployersUsagePageComponent} from "app/customer/statistics/statistics-deployers-usage-page.component";
import {StatisticsImagePullsSummaryPageComponent} from "app/customer/statistics/statistics-image-pulls-summary-page.component";
import {StatisticsImagesSummaryPageComponent} from "app/customer/statistics/statistics-images-summary-page.component";
import {StatisticsJobsSummaryPageComponent} from "app/customer/statistics/statistics-jobs-summary-page.component";
import {StatisticsJobsUsagePageComponent} from "app/customer/statistics/statistics-jobs-usage-page.component";
import {StatisticsUpgradeSummaryPageComponent} from "app/customer/statistics/statistics-upgrade-summary-page.component";
//import {ReportsModule} from "app/reports";
import {DomainModule} from "app/services/domain";
import {SharedModule} from "app/shared";
import {CdkModule} from "framework/cdk";

import {MaterialModule} from "framework/material";
import {FrameworkServicesModule} from "framework/services";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  BackgroundActivitiesSummaryPageComponent,
                  BackgroundActivitiesListComponent,
                  BackgroundActivityDetailPageComponent,

                  CustomersSummaryPageComponent,
                  CustomersListComponent,
                  CustomersDetailPageComponent,
                  CustomerSelectionDialogComponent,

                  CustomerServicesListComponent,
                  CustomerServicesDetailPageComponent,
                  CustomerServicesDetailPageLogComponent,
                  CustomerServiceInstancesListComponent,
                  CustomerServiceBackupsListComponent,
                  ConsumerServiceBackupsDetailPageComponent,

                  CustomerServiceSecretsCreationPageComponent,
                  CustomerServiceSecretsDetailPageComponent,
                  CustomerServiceSecretsListComponent,

                  CustomerSharedSecretsCreationPageComponent,
                  CustomerSharedSecretsDetailPageComponent,
                  CustomerSharedSecretsListComponent,

                  CustomerSharedUsersCreationPageComponent,
                  CustomerSharedUsersDetailPageComponent,
                  CustomerSharedUsersListComponent,

                  CustomerServiceSelectionDialogComponent,
                  ServiceRoleSelectionDialogComponent,

                  DeploymentChargesSummaryComponent,

                  DeploymentHostsSummaryPageComponent,
                  DeploymentHostsListComponent,
                  DeploymentHostsDetailPageComponent,
                  DeploymentHostsDetailPageLogComponent,
                  DeploymentHostDelayedOpsListComponent,
                  DeploymentHostFilesListComponent,
                  DeploymentHostImagePullsListComponent,
                  DeploymentHostImagesListComponent,
                  DeploymentHostProvisioningListComponent,

                  DeploymentAgentsListComponent,
                  DeploymentAgentsDetailPageComponent,
                  DeploymentAgentUpgradeOverlay,

                  DeploymentTasksListComponent,
                  DeploymentTasksDetailPageComponent,

                  JobsListComponent,
                  JobsSummaryPageComponent,
                  JobsDetailPageComponent,

                  HostsDetailPageComponent,
                  HostsDetailPageLogComponent,
                  HostsListComponent,
                  HostsSummaryPageComponent,

                  StatisticsBackupsUsagePageComponent,
                  StatisticsCellularChargesPageComponent,
                  StatisticsDatagramSessionsPageComponent,
                  StatisticsDeployersUsagePageComponent,
                  StatisticsImagePullsSummaryPageComponent,
                  StatisticsImagesSummaryPageComponent,
                  StatisticsJobsSummaryPageComponent,
                  StatisticsJobsUsagePageComponent,
                  StatisticsUpgradeSummaryPageComponent,

                  JobImagesListComponent,
                  JobImagesDetailPageComponent,
                  JobStepsListComponent,

                  JobDefinitionsSummaryPageComponent,
                  JobDefinitionsListComponent,
                  JobDefinitionsDetailPageComponent,

                  JobDefinitionStepsListComponent,
                  JobDefinitionStepsDetailPageComponent,

                  RegistryTaggedImagesSummaryPageComponent,
                  RegistryTaggedImagesDetailPageComponent,
                  RegistryTaggedImagesListComponent,
                  RegistryImageSelectionDialogComponent,

                  RepositoriesSummaryPageComponent,
                  RepositoriesListComponent,
                  RepositoriesDetailPageComponent,

                  RepositoryBranchSelectionDialogComponent,
                  RepositoryCommitSelectionDialogComponent,

                  RepositoryCheckoutsListComponent,
                  RepositoryCheckoutsDetailPageComponent,

                  TestConfigurationDialogComponent,

                  UsersListPageComponent,
                  UsersDetailPageComponent,
                  UsersCreationPageComponent,

                  LoggersComponent,
                  LoggersPageComponent,
                  ThreadsPageComponent,

                  ProvisioningPageComponent,
                  ProvisioningLabelsComponent,

                  SearchResultsPageComponent
              ],
              imports     : [
                  BrowserModule,
                  CommonModule,
                  FormsModule,
                  RouterModule,
//                  ReportsModule,
                  CdkModule,
                  MaterialModule,
                  FrameworkServicesModule,
                  FrameworkUIModule,
                  DomainModule,
                  SharedModule,
                  TreeModule,
                  BrowserAnimationsModule,
                  AngularDraggableModule
              ]
          })
export class CustomerModule {}
