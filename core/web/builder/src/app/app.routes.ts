import {Routes} from "@angular/router";

import {AppNavigationResolver} from "app/app-navigation.resolver";
import {UsersCreationPageComponent} from "app/customer/configuration/users/users-creation-page.component";
import {UsersDetailPageComponent} from "app/customer/configuration/users/users-detail-page.component";
import {UsersListPageComponent} from "app/customer/configuration/users/users-list-page.component";
import {ConsumerServiceBackupsDetailPageComponent} from "app/customer/customer-services/customer-service-backups-detail-page.component";
import {CustomerServicesDetailPageLogComponent} from "app/customer/customer-services/customer-services-detail-page-log.component";
import {CustomerServicesDetailPageComponent} from "app/customer/customer-services/customer-services-detail-page.component";
import {CustomerServiceSecretsCreationPageComponent} from "app/customer/customer-services/secrets/customer-service-secrets-creation-page.component";
import {CustomerServiceSecretsDetailPageComponent} from "app/customer/customer-services/secrets/customer-service-secrets-detail-page.component";
import {CustomersDetailPageComponent} from "app/customer/customers/customers-detail-page.component";
import {CustomersSummaryPageComponent} from "app/customer/customers/customers-summary-page.component";
import {CustomerSharedSecretsCreationPageComponent} from "app/customer/customers/shared-secrets/customer-shared-secrets-creation-page.component";
import {CustomerSharedSecretsDetailPageComponent} from "app/customer/customers/shared-secrets/customer-shared-secrets-detail-page.component";
import {CustomerSharedUsersCreationPageComponent} from "app/customer/customers/shared-users/customer-shared-users-creation-page.component";
import {CustomerSharedUsersDetailPageComponent} from "app/customer/customers/shared-users/customer-shared-users-detail-page.component";
import {DeploymentAgentsDetailPageComponent} from "app/customer/deployment-agents/deployment-agents-detail-page.component";
import {DeploymentHostsDetailPageLogComponent} from "app/customer/deployment-hosts/deployment-hosts-detail-page-log.component";
import {DeploymentHostsDetailPageComponent} from "app/customer/deployment-hosts/deployment-hosts-detail-page.component";
import {DeploymentHostsSummaryPageComponent} from "app/customer/deployment-hosts/deployment-hosts-summary-page.component";
import {DeploymentTasksDetailPageComponent} from "app/customer/deployment-tasks/deployment-tasks-detail-page.component";
import {HostsDetailPageLogComponent} from "app/customer/hosts/hosts-detail-page-log.component";
import {HostsDetailPageComponent} from "app/customer/hosts/hosts-detail-page.component";
import {HostsSummaryPageComponent} from "app/customer/hosts/hosts-summary-page.component";
import {JobDefinitionStepsDetailPageComponent} from "app/customer/job-definition-steps/job-definition-steps-detail-page.component";
import {JobDefinitionsDetailPageComponent} from "app/customer/job-definitions/job-definitions-detail-page.component";
import {JobDefinitionsSummaryPageComponent} from "app/customer/job-definitions/job-definitions-summary-page.component";
import {JobsDetailPageComponent} from "app/customer/jobs/jobs-detail-page.component";
import {JobsSummaryPageComponent} from "app/customer/jobs/jobs-summary-page.component";
import {BackgroundActivitiesSummaryPageComponent} from "app/customer/maintenance/background-activities/background-activities-summary-page.component";
import {BackgroundActivityDetailPageComponent} from "app/customer/maintenance/background-activities/background-activity-detail-page.component";
import {LoggersPageComponent} from "app/customer/maintenance/loggers/loggers-page.component";
import {ProvisioningLabelsComponent} from "app/customer/maintenance/provisioning/provisioning-labels.component";
import {ProvisioningPageComponent} from "app/customer/maintenance/provisioning/provisioning-page.component";
import {ThreadsPageComponent} from "app/customer/maintenance/threads/threads-page.component";
import {RegistryTaggedImagesDetailPageComponent} from "app/customer/registry-tagged-images/registry-tagged-images-detail-page.component";
import {RegistryTaggedImagesSummaryPageComponent} from "app/customer/registry-tagged-images/registry-tagged-images-summary-page.component";
import {RepositoriesDetailPageComponent} from "app/customer/repositories/repositories-detail-page.component";
import {RepositoriesSummaryPageComponent} from "app/customer/repositories/repositories-summary-page.component";
import {RepositoryCheckoutsDetailPageComponent} from "app/customer/repository-checkouts/repository-checkouts-detail-page.component";
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
import {DashboardPageComponent} from "app/dashboard/dashboard-page.component";
import {UserPasswordChangePageComponent} from "app/dashboard/user-password-change-page.component";
import {UserProfilePageComponent} from "app/dashboard/user-profile-page.component";

// optio3 error page components
import {ErrorPageComponent} from "app/error/error-page.component";
import {NotFoundPageComponent} from "app/error/not-found-page.component";
// domain services
import {AuthGuard} from "app/services/domain/auth.guard";
import {ForgotPasswordPageComponent} from "app/start/forgot-password-page.component";
// optio3 start page components
import {LoginPageComponent} from "app/start/login-page.component";
import {ResetPasswordPageComponent} from "app/start/reset-password-page.component";

/**
 * Router Setting
 *
 * Write your component (Page) here to load.
 */
export const ROUTES: Routes = [

    // application routes
    {
        path            : "",
        canActivateChild: [AuthGuard],
        children        : [
            {
                path      : "",
                redirectTo: "home",
                pathMatch : "full"
            },
            {
                path     : "home",
                component: DashboardPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Home",
                    breadcrumbs: []
                }
            },
            {
                path     : "search/:area",
                component: SearchResultsPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title           : "Search",
                    ignoreAsPrevious: true,
                    breadcrumbs     : [
                        {
                            title: "Home",
                            url  : "/home"
                        }
                    ]
                }
            },
            {
                path     : "search",
                component: SearchResultsPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title           : "Search",
                    ignoreAsPrevious: true,
                    breadcrumbs     : [
                        {
                            title: "Home",
                            url  : "/home"
                        }
                    ]
                }
            },
            {
                path    : "user",
                children: [
                    {
                        path      : "",
                        redirectTo: "profile",
                        pathMatch : "full"
                    },
                    {
                        path     : "profile",
                        component: UserProfilePageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title           : "User",
                            ignoreAsPrevious: true,
                            breadcrumbs     : []
                        }
                    },
                    {
                        path     : "change-password",
                        component: UserPasswordChangePageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title           : "User",
                            ignoreAsPrevious: true,
                            breadcrumbs     : []
                        }
                    }
                ]
            },
            {
                path    : "job-definitions",
                children: [
                    {
                        path      : "",
                        redirectTo: "summary",
                        pathMatch : "full"
                    },
                    {
                        path     : "summary",
                        component: JobDefinitionsSummaryPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Job Definitions",
                            breadcrumbs: []
                        }
                    },
                    {
                        path     : "item/:jobId",
                        component: JobDefinitionsDetailPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Job Definition",
                            breadcrumbs: [
                                {
                                    title: "Job Definitions",
                                    url  : "/job-definitions/summary"
                                }
                            ]
                        }
                    },
                    {
                        path     : "item/:jobId/step/:stepId",
                        component: JobDefinitionStepsDetailPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Job Definition Step",
                            breadcrumbs: [
                                {
                                    title: "Job Definitions",
                                    url  : "/job-definitions/summary"
                                },
                                {
                                    title: "Definition",
                                    url  : "/job-definitions/item/:jobId"
                                }
                            ]
                        }
                    }
                ]
            },
            {
                path    : "jobs",
                children: [
                    {
                        path      : "",
                        redirectTo: "summary",
                        pathMatch : "full"
                    },
                    {
                        path     : "summary",
                        component: JobsSummaryPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Jobs",
                            breadcrumbs: []
                        }
                    },
                    {
                        path     : "item/:id",
                        component: JobsDetailPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Jobs",
                            breadcrumbs: [
                                {
                                    title: "Jobs Summary",
                                    url  : "/jobs/summary"
                                }
                            ]
                        }
                    },
                    {
                        path     : "item/:jobId/job-definition/:id",
                        component: JobDefinitionsDetailPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Jobs",
                            breadcrumbs: [
                                {
                                    title: "Jobs Summary",
                                    url  : "/jobs/summary"
                                },
                                {
                                    title: "Job Details",
                                    url  : "/jobs/item/:jobId"
                                }
                            ]
                        }
                    },
                    {
                        path     : "item/:jobId/image/:imageId",
                        component: RegistryTaggedImagesDetailPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Images",
                            breadcrumbs: [
                                {
                                    title: "Jobs Summary",
                                    url  : "/jobs/summary"
                                },
                                {
                                    title: "Job Details",
                                    url  : "/jobs/item/:jobId"
                                }
                            ]
                        }
                    }
                ]
            },
            {
                path    : "hosts",
                children: [
                    {
                        path      : "",
                        redirectTo: "summary",
                        pathMatch : "full"
                    },
                    {
                        path     : "summary",
                        component: HostsSummaryPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Hosts",
                            breadcrumbs: []
                        }
                    },
                    {
                        path     : "host/:id",
                        component: HostsDetailPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Hosts",
                            breadcrumbs: [
                                {
                                    title: "Host Summary",
                                    url  : "/hosts/summary"
                                }
                            ]
                        }
                    },
                    {
                        path     : "host/:id/log",
                        component: HostsDetailPageLogComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Hosts",
                            breadcrumbs: [
                                {
                                    title: "Host Summary",
                                    url  : "/hosts/summary"
                                },
                                {
                                    title: "Host Details",
                                    url  : "/hosts/host/:id"
                                }
                            ]
                        }
                    }
                ]
            },
            {
                path    : "repositories",
                children: [
                    {
                        path      : "",
                        redirectTo: "summary",
                        pathMatch : "full"
                    },
                    {
                        path     : "summary",
                        component: RepositoriesSummaryPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Repositories",
                            breadcrumbs: []
                        }
                    },
                    {
                        path     : "item/:id",
                        component: RepositoriesDetailPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Repositories",
                            breadcrumbs: [
                                {
                                    title: "Repositories",
                                    url  : "/repositories/summary"
                                }
                            ]
                        }
                    },
                    {
                        path     : "item/:id/checkout/:checkoutId",
                        component: RepositoryCheckoutsDetailPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Repository Checkout",
                            breadcrumbs: [
                                {
                                    title: "Repositories",
                                    url  : "/repositories/summary"
                                },
                                {
                                    title: "Repository Checkout",
                                    url  : "/repositories/item/:id"
                                }
                            ]
                        }
                    }
                ]
            },
            {
                path    : "images",
                children: [
                    {
                        path      : "",
                        redirectTo: "summary",
                        pathMatch : "full"
                    },
                    {
                        path     : "summary",
                        component: RegistryTaggedImagesSummaryPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Images",
                            breadcrumbs: []
                        }
                    },
                    {
                        path     : "item/:imageId",
                        component: RegistryTaggedImagesDetailPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Image Details",
                            breadcrumbs: [
                                {
                                    title: "Image",
                                    url  : "/images/summary"
                                }
                            ]
                        }
                    }
                ]
            },
            {
                path    : "customers",
                children: [
                    {
                        path      : "",
                        redirectTo: "summary",
                        pathMatch : "full"
                    },
                    {
                        path     : "summary",
                        component: CustomersSummaryPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Customers",
                            breadcrumbs: []
                        }
                    },
                    {
                        path     : "item/new",
                        component: CustomersDetailPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title           : "Customer Details",
                            ignoreAsPrevious: true,
                            breadcrumbs     : [
                                {
                                    title: "Customers",
                                    url  : "/customers/summary"
                                }
                            ]
                        }
                    },
                    {
                        path     : "item/:custId",
                        component: CustomersDetailPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Customer Details",
                            breadcrumbs: [
                                {
                                    title: "Customers",
                                    url  : "/customers/summary"
                                }
                            ]
                        }
                    },
                    {
                        path     : "item/:custId/service/new",
                        component: CustomerServicesDetailPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title           : "Service Details",
                            ignoreAsPrevious: true,
                            breadcrumbs     : [
                                {
                                    title: "Customers",
                                    url  : "/customers/summary"
                                },
                                {
                                    title: "Customer Details",
                                    url  : "/customers/item/:custId"
                                }
                            ]
                        }
                    },
                    {
                        path     : "item/:custId/service/:svcId",
                        component: CustomerServicesDetailPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Service Details",
                            breadcrumbs: [
                                {
                                    title: "Customers",
                                    url  : "/customers/summary"
                                },
                                {
                                    title: "Customer Details",
                                    url  : "/customers/item/:custId"
                                }
                            ]
                        }
                    },
                    {
                        path     : "item/:custId/service/:svcId/log",
                        component: CustomerServicesDetailPageLogComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Hosts",
                            breadcrumbs: [
                                {
                                    title: "Customers",
                                    url  : "/customers/summary"
                                },
                                {
                                    title: "Customer Details",
                                    url  : "/customers/item/:custId"
                                },
                                {
                                    title: "Customer Service Details",
                                    url  : "/customers/item/:custId/service/:svcId"
                                }
                            ]
                        }
                    },
                    {
                        path     : "item/:custId/service/:svcId/secret/new",
                        component: CustomerServiceSecretsCreationPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Service Details",
                            breadcrumbs: [
                                {
                                    title: "Customers",
                                    url  : "/customers/summary"
                                },
                                {
                                    title: "Customer Details",
                                    url  : "/customers/item/:custId"
                                },
                                {
                                    title: "Customer Service Details",
                                    url  : "/customers/item/:custId/service/:svcId"
                                }
                            ]
                        }
                    },
                    {
                        path     : "item/:custId/service/:svcId/secret/:secretId",
                        component: CustomerServiceSecretsDetailPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Service Details",
                            breadcrumbs: [
                                {
                                    title: "Customers",
                                    url  : "/customers/summary"
                                },
                                {
                                    title: "Customer Details",
                                    url  : "/customers/item/:custId"
                                },
                                {
                                    title: "Customer Service Details",
                                    url  : "/customers/item/:custId/service/:svcId"
                                }
                            ]
                        }
                    },
                    {
                        path     : "item/:custId/user/new",
                        component: CustomerSharedUsersCreationPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Service Details",
                            breadcrumbs: [
                                {
                                    title: "Customers",
                                    url  : "/customers/summary"
                                },
                                {
                                    title: "Customer Details",
                                    url  : "/customers/item/:custId"
                                }
                            ]
                        }
                    },
                    {
                        path     : "item/:custId/user/:userId",
                        component: CustomerSharedUsersDetailPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Service Details",
                            breadcrumbs: [
                                {
                                    title: "Customers",
                                    url  : "/customers/summary"
                                },
                                {
                                    title: "Customer Details",
                                    url  : "/customers/item/:custId"
                                }
                            ]
                        }
                    },
                    {
                        path     : "item/:custId/secret/new",
                        component: CustomerSharedSecretsCreationPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Service Details",
                            breadcrumbs: [
                                {
                                    title: "Customers",
                                    url  : "/customers/summary"
                                },
                                {
                                    title: "Customer Details",
                                    url  : "/customers/item/:custId"
                                }
                            ]
                        }
                    },
                    {
                        path     : "item/:custId/secret/:secretId",
                        component: CustomerSharedSecretsDetailPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Service Details",
                            breadcrumbs: [
                                {
                                    title: "Customers",
                                    url  : "/customers/summary"
                                },
                                {
                                    title: "Customer Details",
                                    url  : "/customers/item/:custId"
                                }
                            ]
                        }
                    },
                    {
                        path     : "item/:custId/service/:svcId/backup/:backupId",
                        component: ConsumerServiceBackupsDetailPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Backup Details",
                            breadcrumbs: [
                                {
                                    title: "Customers",
                                    url  : "/customers/summary"
                                },
                                {
                                    title: "Customer Details",
                                    url  : "/customers/item/:custId"
                                },
                                {
                                    title: "Service Details",
                                    url  : "/customers/item/:custId/service/:svcId"
                                }
                            ]
                        }
                    }
                ]
            },
            {
                path    : "deployments",
                children: [
                    {
                        path      : "",
                        redirectTo: "summary",
                        pathMatch : "full"
                    },
                    {
                        path     : "summary",
                        component: DeploymentHostsSummaryPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Deployments",
                            breadcrumbs: []
                        }
                    },
                    {
                        path     : "item/:id",
                        component: DeploymentHostsDetailPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Deployments",
                            breadcrumbs: [
                                {
                                    title: "Deployments",
                                    url  : "/deployments/summary"
                                }
                            ]
                        }
                    },
                    {
                        path     : "item/:id/log",
                        component: DeploymentHostsDetailPageLogComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Hosts",
                            breadcrumbs: [
                                {
                                    title: "Deployments",
                                    url  : "/deployments/summary"
                                },
                                {
                                    title: "Host Details",
                                    url  : "/deployments/item/:id"
                                }
                            ]
                        }
                    },
                    {
                        path     : "item/:depID/agent/:id",
                        component: DeploymentAgentsDetailPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Agent",
                            breadcrumbs: [
                                {
                                    title: "Deployments",
                                    url  : "/deployments/summary"
                                },
                                {
                                    title: "Agent Details",
                                    url  : "/deployments/item/:depID"
                                }
                            ]
                        }
                    },
                    {
                        path     : "item/:depID/task/:id",
                        component: DeploymentTasksDetailPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Task",
                            breadcrumbs: [
                                {
                                    title: "Deployments",
                                    url  : "/deployments/summary"
                                },
                                {
                                    title: "Task Details",
                                    url  : "/deployments/item/:depID"
                                }
                            ]
                        }
                    }
                ]
            },
            {
                path    : "background-activities",
                children: [
                    {
                        path      : "",
                        redirectTo: "summary",
                        pathMatch : "full"
                    },
                    {
                        path     : "summary",
                        component: BackgroundActivitiesSummaryPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Background Activities",
                            breadcrumbs: []
                        }
                    },
                    {
                        path     : "item/:id",
                        component: BackgroundActivityDetailPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Background Activities",
                            breadcrumbs: [
                                {
                                    title: "Background Activities",
                                    url  : "/background-activities/summary"
                                }
                            ]
                        }
                    }
                ]
            },
            {
                path    : "statistics",
                children: [
                    {
                        path     : "jobs-summary",
                        component: StatisticsJobsSummaryPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Statistics",
                            breadcrumbs: []
                        }
                    },
                    {
                        path     : "images-summary",
                        component: StatisticsImagesSummaryPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Statistics",
                            breadcrumbs: []
                        }
                    },
                    {
                        path     : "image-pulls-summary",
                        component: StatisticsImagePullsSummaryPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Statistics",
                            breadcrumbs: []
                        }
                    },
                    {
                        path     : "jobs-usage",
                        component: StatisticsJobsUsagePageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Statistics",
                            breadcrumbs: []
                        }
                    },
                    {
                        path     : "agents-usage",
                        component: StatisticsDeployersUsagePageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Statistics",
                            breadcrumbs: []
                        }
                    },
                    {
                        path     : "backups-usage",
                        component: StatisticsBackupsUsagePageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Statistics",
                            breadcrumbs: []
                        }
                    },
                    {
                        path     : "cellular-charges",
                        component: StatisticsCellularChargesPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Statistics",
                            breadcrumbs: []
                        }
                    },
                    {
                        path     : "datagram-sessions",
                        component: StatisticsDatagramSessionsPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Statistics",
                            breadcrumbs: []
                        }
                    },
                    {
                        path     : "upgrade-status",
                        component: StatisticsUpgradeSummaryPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Statistics",
                            breadcrumbs: []
                        }
                    }
                ]
            },
            {
                path    : "configuration",
                children: [
                    {
                        path      : "",
                        redirectTo: "users",
                        pathMatch : "full"
                    },
                    {
                        path     : "users",
                        component: UsersListPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Settings",
                            breadcrumbs: []
                        }
                    },
                    {
                        path     : "users/new",
                        component: UsersCreationPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "New User",
                            breadcrumbs: [
                                {
                                    title: "User Management",
                                    url  : "configuration/users"
                                }
                            ]
                        }
                    },
                    {
                        path     : "users/user/:id",
                        component: UsersDetailPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Settings",
                            breadcrumbs: [
                                {
                                    title: "User Management",
                                    url  : "configuration/users"
                                }
                            ]
                        }
                    },
                    {
                        path     : "loggers",
                        component: LoggersPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Settings",
                            breadcrumbs: []
                        }
                    },
                    {
                        path     : "threads",
                        component: ThreadsPageComponent,
                        resolve  : {breadcrumbs: AppNavigationResolver},
                        data     : {
                            title      : "Settings",
                            breadcrumbs: []
                        }
                    }
                ]
            },
            {
                path     : "provision",
                component: ProvisioningPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Provision Waypoint",
                    breadcrumbs: []
                }
            },
            {
                path     : "provision-labels",
                component: ProvisioningLabelsComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Label Waypoint",
                    breadcrumbs: []
                }
            }
        ]
    },

    // entrance pages
    {
        path            : "start",
        canActivateChild: [AuthGuard],
        children        : [
            {
                path      : "",
                redirectTo: "login",
                pathMatch : "full"
            },
            {
                path     : "login",
                component: LoginPageComponent
            },
            {
                path     : "forgotpassword",
                component: ForgotPasswordPageComponent
            },
            {
                path     : "resetpassword/:token",
                component: ResetPasswordPageComponent
            }
        ]
    },

    // error pages
    {
        path    : "error",
        children: [
            {
                path      : "",
                redirectTo: "general",
                pathMatch : "full"
            },
            {
                path     : "general",
                component: ErrorPageComponent
            },
            {
                path     : "not-found",
                component: NotFoundPageComponent
            }
        ]
    },
    {
        path      : "**",
        redirectTo: "error/not-found"
    }

    // Emergency loading, need to import component form file.
    //{
    //  path: 'dashboard',
    //  component: DashboardComponent
    //},
    // Lazy loading, you need to create a module file.
    //
    // 1. Find file dashboard.module.lazy at folder dashboard
    // 2. Rename file dashboard.module.lazy to dashboard.module.ts
    // 3. Modify this file
    //    change Line "component: DashboardComponent" to "loadChildren: './dashboard/dashboard.module#DashboardModule'"
    // 4. Modify file app.module.ts
    //    remove line "DashboardComponent," and "import { DashboardComponent } from './dashboard/dashboard.component';"
    //
    // {
    //   path: 'dashboard',
    //   loadChildren: './dashboard/dashboard.module#DashboardModule'
    // },
];
