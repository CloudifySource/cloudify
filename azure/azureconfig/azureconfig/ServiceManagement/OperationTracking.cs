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

using System;
using System.Runtime.Serialization;
using System.ServiceModel;
using System.ServiceModel.Web;
using System.Net;

namespace Microsoft.Samples.WindowsAzure.ServiceManagement
{
    [DataContract(Namespace = Constants.ServiceManagementNS)]
    public class Operation : IExtensibleDataObject
    {
        [DataMember(Name = "ID", Order = 1)]
        public string OperationTrackingId { get; set; }
        /// <summary>
        /// The class OperationState defines its possible values. 
        /// </summary>
        [DataMember(Order = 2)]
        public string Status { get; set; }

        [DataMember(Order = 3, EmitDefaultValue = false)]
        public int HttpStatusCode { get; set; }

        [DataMember(Order = 4, EmitDefaultValue = false)]
        public ServiceManagementError Error { get; set; }

        public ExtensionDataObject ExtensionData { get; set; }
    }

    public partial interface IServiceManagement
    {

        /// <summary>
        /// Gets the result of an asynchronous operation.
        /// </summary>
        [OperationContract(AsyncPattern = true)]
        [WebGet(UriTemplate = @"{subscriptionId}/operations/{operationTrackingId}")]
        IAsyncResult BeginGetOperationStatus(string subscriptionId, string operationTrackingId, AsyncCallback callback, object state);
        Operation EndGetOperationStatus(IAsyncResult asyncResult);
    }

    public static partial class ServiceManagementExtensionMethods
    {
        public static Operation GetOperationStatus(this IServiceManagement proxy, string subscriptionId, string operationId)
        {
            return proxy.EndGetOperationStatus(proxy.BeginGetOperationStatus(subscriptionId, operationId, null, null));
        }
    }
}
