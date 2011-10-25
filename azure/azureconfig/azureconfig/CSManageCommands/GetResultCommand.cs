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
    using Microsoft.Samples.WindowsAzure.ServiceManagement.Tools.CSManageCommands;

    public partial class CSManageCommand
    {
        public static string OperationId { get; set; }
        public static void ValidateOperationId()
        {
            if (string.IsNullOrEmpty(OperationId))
            {
                throw new CSManageException("OperationId is null or empty.");
            }
        }
    }

    /// <summary>
    /// Implements GetResult Command for getting status of Asynchronous operation
    /// </summary>
    public class GetResultCommand : CSManageCommand
    {
        public override void Validate()
        {
            ValidateOperationId();
        }

        protected override void PerformOperation(IServiceManagement channel)
        {
            var operation = channel.GetOperationStatus(SubscriptionId, OperationId);
            Console.WriteLine("Requested Status={0}", operation.Status);
            if (operation.Error != null)
            {
                Console.WriteLine(operation.Error);
            }
        }
    }
}

