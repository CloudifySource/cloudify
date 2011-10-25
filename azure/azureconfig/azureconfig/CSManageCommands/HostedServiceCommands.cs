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
    using System.Linq;
    using Microsoft.Samples.WindowsAzure.ServiceManagement;
    using Microsoft.Samples.WindowsAzure.ServiceManagement.Tools.CSManageCommands;
    using System.ServiceModel;
    using System.ServiceModel.Web;
    using System.Net;

    public partial class CSManageCommand
    {

        public static string HostedServiceName { get; set; }

        public static void ValidateHostedServiceName()
        {
            if (string.IsNullOrEmpty(HostedServiceName))
            {
                throw new CSManageException("hosted-service is null or empty.");
            }
        }

        public static string LocationConstraintName { get; set; }
        public static void ValidateLocationConstraintName()
        {
            if (String.IsNullOrEmpty(LocationConstraintName))
            {
                throw new CSManageException("location is null or empty.");
            }
        }
    }
    class CreateHostedServiceCommand : CSManageCommand
    {
        public override void Validate()
        {
            ValidateHostedServiceName();
            ValidateLabel(true);
            ValidateLocationConstraintName();
        }

        protected override void PerformOperation(IServiceManagement channel)
        {
            CreateHostedServiceInput input = new CreateHostedServiceInput()
            {
                ServiceName = HostedServiceName,
                Label = ServiceManagementHelper.EncodeToBase64String(CSManageCommand.Label),
                
            };

            if (CSManageCommand.Description != null)
            {
                Description = CSManageCommand.Description;
            }

            if (LocationConstraintName != null)
            {
                input.Location = LocationConstraintName;
            }
            try
            {
                channel.CreateHostedService(SubscriptionId, input);
            }
            catch (ProtocolException e)
            {
                ServiceManagementError error = null;
                System.Net.HttpStatusCode httpStatusCode = 0;
                string operationId;
                ServiceManagementHelper.TryGetExceptionDetails(e, out error, out httpStatusCode, out operationId);
                if (httpStatusCode != HttpStatusCode.Conflict)
                {
                    base.RethrowCommunicationError(e, error);
                }

                var myservices = channel.ListHostedServices(SubscriptionId);
                if ((from s in myservices select s.ServiceName).Contains(HostedServiceName))
                {
                    throw new CSManageException("The hosted service " + HostedServiceName + " already exists.");
                }
                else
                {
                    throw new CSManageException("A hosted service by the name " + HostedServiceName + " is already in use by another Subscription. Please choose a different name.", e);
                }
            }
        }
    }
    
    class ListHostedServicesCommand : CSManageCommand
    {
        public override void Validate()
        {
        }

        protected override void PerformOperation(IServiceManagement channel)
        {
            var services = channel.ListHostedServices(SubscriptionId);
            foreach (var service in services)
            {
                if (service.ServiceName.Length == 0)
                {
                    Console.WriteLine("...");
                }
                Console.WriteLine(service.ServiceName);
            }
        }
    }

    class ListLocationsCommand : CSManageCommand
    {
        public override void Validate()
        {
        }

        protected override void PerformOperation(IServiceManagement channel)
        {
            LocationList locations = channel.ListLocations(SubscriptionId);
            foreach (var location in locations)
            {
                Console.WriteLine(location.Name);
            }
        }
    }


}
