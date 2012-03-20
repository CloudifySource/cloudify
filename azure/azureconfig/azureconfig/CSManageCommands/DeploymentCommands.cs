//---------------------------------------------------------------------------------
// Microsoft (R) Windows Azure SDK
// Software Development Kit
// 
// Copyright (c) Microsoft Corporation. All rights reserved.  
//
// THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, 
// EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES 
// OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR PURPOSE. 
//---------------------------------------------------------------------------------

namespace Microsoft.Samples.WindowsAzure.ServiceManagement.Tools
{
    using System;

    using Microsoft.Samples.WindowsAzure.ServiceManagement;
    using System.Collections.Generic;
    using System.IO;
    using System.Net;
    using System.ServiceModel;
    using System.Globalization;
    using Microsoft.Samples.WindowsAzure.ServiceManagement.Tools.CSManageCommands;
    using System.Threading;

    public partial class CSManageCommand
    {
        public static string DeploymentName { get; set; }
        public static string DeploymentSlot { get; set; }

        public static void ValidateDeploymentName()
        {
            if (string.IsNullOrEmpty(DeploymentName))
            {
                throw new CSManageException("DeploymentName is null or empty.");
            }
        }

        public static void ValidateDeploymentSlot()
        {
            if (string.IsNullOrEmpty(DeploymentSlot))
            {
                throw new CSManageException("DeploymentSlot is null or empty.");
            }
        }

        public static void ValidateDeploymentNameOrSlot()
        {
            if (string.IsNullOrEmpty(DeploymentSlot) && string.IsNullOrEmpty(DeploymentName))
            {
                throw new CSManageException("Both DeploymentName and DeploymentSlot are null or empty.");
            }
        }

        public static string ConfigFileLocation { get; set; }
        public static void ValidateConfigFileLocation()
        {
            if (string.IsNullOrEmpty(ConfigFileLocation))
            {
                throw new CSManageException("ConfigFileLocation is null or empty.");
            }
            else
            {
                if (!File.Exists(ConfigFileLocation))
                {
                    throw new CSManageException(String.Format("The file {0} cannot be found or the caller does not have sufficient permissions to read it. ", ConfigFileLocation));
                }
            }
        }

        public static string DeploymentStatus { get; set; }
        public static void ValidateDeploymentStatus()
        {
            if (string.IsNullOrEmpty(DeploymentStatus))
            {
                throw new CSManageException("DeploymentStatus is null or empty.");
            }
        }

        public static string PackageLocation { get; set; }
        public static void ValidatePackageLocation()
        {
            if (string.IsNullOrEmpty(PackageLocation))
            {
                throw new CSManageException("PackageUrl is null or empty.");
            }

            if (!Uri.IsWellFormedUriString(PackageLocation, UriKind.Absolute) &&
                !File.Exists(PackageLocation))
            {
                throw new CSManageException("PackageUrl format error. It cannot be casted to Uri and is not an existing file path: '" + PackageLocation + "'");
            }

        }

        public static bool TreatWarningsAsError { get; set; }
    }

    class CreateDeploymentCommand : CSManageCommand
    {
        public override void Validate()
        {
            ValidateHostedServiceName();
            ValidateConfigFileLocation();
            ValidateDeploymentName();
            ValidateLabel(true);
            ValidateDeploymentSlot();
            ValidatePackageLocation();
            if (!Uri.IsWellFormedUriString(PackageLocation, UriKind.Absolute))
            {
                ValidateStorage();
            }
        }

        protected override void PerformOperation(IServiceManagement channel)
        {
            CreateDeploymentInput input = new CreateDeploymentInput
            {
                Name = DeploymentName,
                StartDeployment = true,
                Configuration = Utility.GetSettings(ConfigFileLocation),
            };

            if (!string.IsNullOrEmpty(PackageLocation))
            {
                if (Uri.IsWellFormedUriString(PackageLocation, UriKind.Absolute))
                {
                    input.PackageUrl = new Uri(PackageLocation);
                }
                else
                {
                    //TODO: Propagate timeout parameter
                    input.PackageUrl = UploadFile(PackageLocation, TimeSpan.FromMinutes(60));
                }

            }

            if (!string.IsNullOrEmpty(Label))
            {
                input.Label = ServiceManagementHelper.EncodeToBase64String(Label);
            }

            if (TreatWarningsAsError)
            {
                input.TreatWarningsAsError = TreatWarningsAsError;
            }

            Console.WriteLine("Creating Deployment... Name: {0}, Label: {1}", DeploymentName, Label);
            channel.CreateOrUpdateDeployment(SubscriptionId, HostedServiceName, DeploymentSlot, input);
        }
    }

    class UpdateDeploymentStatusCommand : CSManageCommand
    {

        public override void Validate()
        {
            ValidateHostedServiceName();
            ValidateDeploymentNameOrSlot();
            ValidateDeploymentStatus();
        }

        protected override void PerformOperation(IServiceManagement channel)
        {
            var input = new UpdateDeploymentStatusInput()
            {
                Status = DeploymentStatus
            };
            Console.WriteLine("Updating DeploymentStatus");
            if (!string.IsNullOrEmpty(DeploymentName))
            {
                channel.UpdateDeploymentStatus(SubscriptionId, HostedServiceName, DeploymentName, input);
            }
            else if (!string.IsNullOrEmpty(DeploymentSlot))
            {
                channel.UpdateDeploymentStatusBySlot(SubscriptionId, HostedServiceName, DeploymentSlot, input);
            }
        }
    }

    class DeleteDeploymentCommand : CSManageCommand
    {
        public override void Validate()
        {
            ValidateHostedServiceName();
            ValidateDeploymentNameOrSlot();
        }

        protected override void PerformOperation(IServiceManagement channel)
        {
            Console.WriteLine("Deleting Deployment");
            if (!string.IsNullOrEmpty(DeploymentName))
            {
                channel.DeleteDeployment(SubscriptionId, HostedServiceName, DeploymentName);
            }
            else if (!string.IsNullOrEmpty(DeploymentSlot))
            {
                channel.DeleteDeploymentBySlot(SubscriptionId, HostedServiceName, DeploymentSlot);
            }
        }
    }

    class GetDeploymentConfigCommand : CSManageCommand
    {
        public override void Validate()
        {
            ValidateHostedServiceName();
            ValidateDeploymentNameOrSlot();
        }

        protected override void PerformOperation(IServiceManagement channel)
        {
            Deployment deployment = null;

            if (!string.IsNullOrEmpty(DeploymentName))
            {
                deployment = channel.GetDeployment(SubscriptionId, HostedServiceName, DeploymentName);
            }
            else if (!string.IsNullOrEmpty(DeploymentSlot))
            {
                deployment = channel.GetDeploymentBySlot(SubscriptionId, HostedServiceName, DeploymentSlot);

            }
            String xml = new System.Text.UTF8Encoding().GetString(Convert.FromBase64String(deployment.Configuration));
            xml = xml.Replace("xml version=\"1.0\" encoding=\"utf-16\"", "xml version=\"1.0\" encoding=\"utf-8\"");
            Console.WriteLine(xml);
        }
    }

    class GetDeploymentStatusCommand : CSManageCommand
    {
        public static int MaxRetries { get { return 5; } }

        public override void Validate()
        {
            ValidateHostedServiceName();
            ValidateDeploymentNameOrSlot();
        }

        protected override void PerformOperation(IServiceManagement channel)
        {
            for (int retries = 0; retries < MaxRetries; retries++)
            {
                try
                {
                    String status = PerformOperationOnce(channel);
                    Console.WriteLine(status);
                    break; // no need to retry
                }

                catch (TimeoutException)
                {
                    bool retry = (retries < MaxRetries - 1);
                    if (!retry)
                    {
                        //rethrow exception
                        throw;
                    }
                    // sleep before retry
                    Thread.Sleep(1000);
                }
                catch (CommunicationException ce)
                {
                    ServiceManagementError error = null;
                    HttpStatusCode httpStatusCode = 0;
                    string operationId;
                    ServiceManagementHelper.TryGetExceptionDetails(ce, out error, out httpStatusCode, out operationId);
                    if (error != null && httpStatusCode == HttpStatusCode.NotFound)
                    {
                        Console.WriteLine("NotFound");
                        //NotFound is a Legitimate return value, no more retries
                        break;
                    }
                    //cannot rethrow since stream already closed.
                    else
                    {
                        bool retry = (retries < MaxRetries-1) && shouldRetry(ce);
                        if (!retry)
                        {
                            //rethrow exception
                            base.RethrowCommunicationError(ce, error);
                            //unreachable code
                            break;
                        }
                        // sleep before retry
                        Thread.Sleep(1000);
                    }
                }
            }
        }

        private string PerformOperationOnce(IServiceManagement channel)
        {
            Deployment deployment = null;
                
            if (!string.IsNullOrEmpty(DeploymentName))
            {
                deployment = channel.GetDeployment(SubscriptionId, HostedServiceName, DeploymentName);
            }
            else if (!string.IsNullOrEmpty(DeploymentSlot))
            {
                deployment = channel.GetDeploymentBySlot(SubscriptionId, HostedServiceName, DeploymentSlot);
            }
            String status = deployment.Status;
            if (String.Equals(status, "Running"))
            {
                //downgrade running status if one of the instances is not ready yet
                //see http://msdn.microsoft.com/en-us/library/ee460804.aspx
                foreach (var instance in deployment.RoleInstanceList)
                {
                    if (!instance.InstanceStatus.Equals("Ready"))
                    {
                        status = "Starting";
                        break;
                    }
                }
            }
            return status;
        }

        private bool shouldRetry(CommunicationException ce)
        {
            for (Exception e = ce; e != null; e = e.InnerException)
            {
                // there was a temporary communication error
                System.Net.Sockets.SocketException se = e as System.Net.Sockets.SocketException;
                if (se != null)
                {
                    return true;
                }
                // continue to inner exception
            }

            // do not retry
            return false;
        }

    }

    class GetDeploymentUrlCommand : CSManageCommand
    {
        public override void Validate()
        {
            ValidateHostedServiceName();
            ValidateDeploymentNameOrSlot();
        }

        protected override void PerformOperation(IServiceManagement channel)
        {
            Deployment deployment = null;
            if (!string.IsNullOrEmpty(DeploymentName))
            {
                deployment = channel.GetDeployment(SubscriptionId, HostedServiceName, DeploymentName);
            }
            else if (!string.IsNullOrEmpty(DeploymentSlot))
            {
                deployment = channel.GetDeploymentBySlot(SubscriptionId, HostedServiceName, DeploymentSlot);
            }

            if (deployment.Url == null)
            {
                throw new CSManageException(deployment.Name + " has not started yet and does not have a load balancer URL");
            }

            Console.WriteLine(deployment.Url);
        }
    }

    class SetDeploymentConfigCommand : CSManageCommand
    {

        public override void Validate()
        {
            ValidateHostedServiceName();
            ValidateDeploymentNameOrSlot();
            ValidateConfigFileLocation();
        }

        protected override void PerformOperation(IServiceManagement channel)
        {
            var input = new ChangeConfigurationInput();
            input.Configuration = Utility.GetSettings(ConfigFileLocation);

            if (TreatWarningsAsError)
            {
                input.TreatWarningsAsError = TreatWarningsAsError;
            }

            if (!string.IsNullOrEmpty(DeploymentName))
            {
                channel.ChangeConfiguration(SubscriptionId, HostedServiceName, DeploymentName, input);
            }
            else if (!string.IsNullOrEmpty(DeploymentSlot))
            {
                channel.ChangeConfigurationBySlot(SubscriptionId, HostedServiceName, DeploymentSlot, input);
            }
        }
    }

}
