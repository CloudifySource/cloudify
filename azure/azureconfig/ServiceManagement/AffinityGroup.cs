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
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Diagnostics.CodeAnalysis;
using System.Runtime.Serialization;
using System.ServiceModel;
using System.ServiceModel.Web;


namespace Microsoft.Samples.WindowsAzure.ServiceManagement
{
    /// <summary>
    /// List of affinity groups.
    /// </summary>
    [CollectionDataContract(Name = "AffinityGroups", ItemName = "AffinityGroup", Namespace = Constants.ServiceManagementNS)]
    public class AffinityGroupList : List<AffinityGroup>
    {
        public AffinityGroupList()
        {
        }

        public AffinityGroupList(IEnumerable<AffinityGroup> affinityGroups)
            : base(affinityGroups)
        {
        }
    }

    /// <summary>
    /// Affinity Group data contract. 
    /// </summary>
    [DataContract(Namespace = Constants.ServiceManagementNS)]
    public class AffinityGroup : IExtensibleDataObject
    {
        [DataMember(Order = 1, EmitDefaultValue = false)]
        public string Name { get; set;}

        [DataMember(Order = 2)]
        public string Label { get; set; }

        [DataMember(Order = 3)]
        public string Description { get; set; }

        [DataMember(Order = 4)]
        public string Location { get; set; }

        [DataMember(Order = 5, EmitDefaultValue = false)]
        public HostedServiceList HostedServices { get; set; }

        [DataMember(Order = 6, EmitDefaultValue = false)]
        public StorageServiceList StorageServices { get; set; }

        public ExtensionDataObject ExtensionData { get; set; }
    }

    /// <summary>
    /// The affinity group related part of the external API
    /// </summary>
    public partial interface IServiceManagement
    {
        /// <summary>
        /// Lists the affinity groups associated with the specified subscription.
        /// </summary>
        [OperationContract(AsyncPattern = true)]
        [WebGet(UriTemplate = @"{subscriptionId}/affinitygroups")]
        IAsyncResult BeginListAffinityGroups(string subscriptionId, AsyncCallback callback, object state);
        AffinityGroupList EndListAffinityGroups(IAsyncResult asyncResult);

        /// <summary>
        /// Get properties for the specified affinity group. 
        /// </summary>
        [OperationContract(AsyncPattern = true)]
        [WebGet(UriTemplate = @"{subscriptionId}/affinitygroups/{affinityGroupName}")]
        IAsyncResult BeginGetAffinityGroup(string subscriptionId, string affinityGroupName, AsyncCallback callback, object state);
        AffinityGroup EndGetAffinityGroup(IAsyncResult asyncResult);
    }

    public static partial class ServiceManagementExtensionMethods
    {
        public static AffinityGroupList ListAffinityGroups(this IServiceManagement proxy, string subscriptionId)
        {
            return proxy.EndListAffinityGroups(proxy.BeginListAffinityGroups(subscriptionId, null, null));
        }

        public static AffinityGroup GetAffinityGroup(this IServiceManagement proxy, string subscriptionId, string affinityGroupName)
        {
            return proxy.EndGetAffinityGroup(proxy.BeginGetAffinityGroup(subscriptionId, affinityGroupName, null, null));
        }
    }
}
