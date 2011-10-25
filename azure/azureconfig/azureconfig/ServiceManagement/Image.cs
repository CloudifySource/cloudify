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
    /// The desired permission for generating a shared access signature. 
    /// This enum is used in the prepare image upload operation.
    /// </summary>
    [DataContract(Namespace = Constants.ServiceManagementNS)]
    public enum ImageSharedAccessSignaturePermission
    {
        [EnumMember]
        Read,
        [EnumMember]
        ReadWrite
    }

    /// <summary>
    /// The status of an image upload.
    /// </summary>
    [DataContract(Namespace = Constants.ServiceManagementNS)]
    public enum ImageStatus
    {
        [EnumMember]
        Pending,
        [EnumMember]
        Committed
    }

    /// <summary>
    /// Input for the prepare image upload operation.
    /// </summary>
    [DataContract(Name = "PrepareMachineImage", Namespace = Constants.ServiceManagementNS)]
    public class  PrepareImageUploadInput : IExtensibleDataObject
    {
        [DataMember(Order = 1)]
        public string Label { get; set; }

        [DataMember(Order = 2)]
        public string Description { get; set; }

        [DataMember(Order = 3)]
        public string Uuid { get; set; }

        [DataMember(Order = 4)]
        public string Timestamp { get; set; }

        [DataMember(Order = 5)]
        public long CompressedSizeInBytes { get; set; }

        [DataMember(Order = 6)]
        public long MountedSizeInBytes { get; set; }

        [DataMember(Order = 7, EmitDefaultValue = false)]
        public string Location { get; set; }

        [DataMember(Order = 8, EmitDefaultValue = false)]
        public string AffinityGroup { get; set; }

        [DataMember(Order = 9, EmitDefaultValue = false)]
        public string ParentUuid { get; set; }

        [DataMember(Order = 10, EmitDefaultValue = false)]
        public string ParentTimestamp { get; set; }

        public ExtensionDataObject ExtensionData { get; set; }
    }

    /// <summary>
    /// Input for the set image properties operation.
    /// </summary>
    [DataContract(Name="SetMachineImageProperties", Namespace = Constants.ServiceManagementNS)]
    public class SetMachineImagePropertiesInput : IExtensibleDataObject
    {
        [DataMember(Order = 1)]
        public string Label { get; set; }

        [DataMember(Order = 2)]
        public string Description { get; set; }

        public ExtensionDataObject ExtensionData { get; set; }
    }

    /// <summary>
    /// Input for the set parent image operation.
    /// </summary>
    [DataContract(Name="ParentMachineImage", Namespace = Constants.ServiceManagementNS)]
    public class SetParentImageInput : IExtensibleDataObject
    {
        [DataMember(Order = 1)]
        public string ParentImageName { get; set; }

        public ExtensionDataObject ExtensionData { get; set; }
    }


    /// <summary>
    /// Reference to an image that can be used for upload and download.
    /// </summary>
    [DataContract(Namespace = Constants.ServiceManagementNS)]
    public class MachineImageReference : IExtensibleDataObject
    {
        [DataMember(Order = 1)]
        public string SharedAccessSignatureUrl { get; set; }

        public ExtensionDataObject ExtensionData { get; set; }
    }

    /// <summary>
    /// List of images.
    /// </summary>
    [CollectionDataContract(Name="MachineImages",ItemName = "MachineImage",Namespace = Constants.ServiceManagementNS)]
    public class MachineImageList : List<MachineImage>
    {
    }

    /// <summary>
    /// Information associated with an image. 
    /// </summary>
    [DataContract(Namespace = Constants.ServiceManagementNS)]
    public class MachineImage : IExtensibleDataObject
    {
        [DataMember(Order = 1)]
        public string Name { get; set; }

        [DataMember(Order = 2)]
        public string Label { get; set; }

        [DataMember(Order = 3)]
        public string Description { get; set; }

        [DataMember(Order = 4, EmitDefaultValue = false)]
        public string Location { get; set; }

        [DataMember(Order = 5, EmitDefaultValue = false)]
        public string AffinityGroup { get; set; }

        [DataMember(Order = 6)]
        public string Status { get; set; }

        [DataMember(Order = 7)]
        public string ParentImageName { get; set; }

        [DataMember(Order = 8)]
        public string Uuid { get; set; }

        [DataMember(Order = 9)]
        public string Timestamp { get; set; }

        [DataMember(Order = 10)]
        public long MountedSizeInBytes { get; set; }

        [DataMember(Order = 11)]
        public long CompressedSizeInBytes { get; set; }

        [DataMember(Order = 12, EmitDefaultValue = false)]
        public string ParentUuid { get; set; }

        [DataMember(Order = 13, EmitDefaultValue = false)]
        public string ParentTimestamp { get; set; }

        [DataMember(Order = 15)]
        public bool InUse { get; set; }

        public ExtensionDataObject ExtensionData { get; set; }
    }

    /// <summary>
    /// The image-specific part of the service management service.
    /// </summary>
    public partial interface IServiceManagement
    {
        /// <summary>
        /// Prepare an image for upload.
        /// </summary>
        [OperationContract(AsyncPattern = true)]
        [WebInvoke(Method = "PUT", UriTemplate = @"{subscriptionID}/machineimages/{imageName}")]
        IAsyncResult BeginPrepareImageUpload(string subscriptionId, string imageName, PrepareImageUploadInput input, AsyncCallback callback, object state);
        void EndPrepareImageUpload(IAsyncResult asyncResult);

        /// <summary>
        /// Get image reference.
        /// </summary>
        [OperationContract(AsyncPattern = true)]
        [WebGet(UriTemplate = @"{subscriptionID}/machineimages/{imageName}?comp=reference&expiry={expiry}&permission={permission}")]
        IAsyncResult BeginGetImageReference(string subscriptionID, string imageName, string expiry, string permission, AsyncCallback callback, object state);
        MachineImageReference EndGetImageReference(IAsyncResult asyncResult);

        /// <summary>
        /// Commit the upload of an image.
        /// </summary>
        [OperationContract(AsyncPattern = true)]
        [WebInvoke(Method = "POST", UriTemplate = @"{subscriptionID}/machineimages/{imageName}?comp=commitmachineimage")]
        IAsyncResult BeginCommitImageUpload(string subscriptionID, string imageName, AsyncCallback callback, object state);
        void EndCommitImageUpload(IAsyncResult asyncResult);

        /// <summary>
        /// List all images associated with a subscription.
        /// </summary>
        [OperationContract(AsyncPattern = true)]
        [WebGet(UriTemplate = @"{subscriptionID}/machineimages")]
        IAsyncResult BeginListImages(string subscriptionID, AsyncCallback callback, object state);
        MachineImageList EndListImages(IAsyncResult asyncResult);

        /// <summary>
        /// Get information about an image.
        /// </summary>
        [OperationContract(AsyncPattern = true)]
        [WebGet(UriTemplate = @"{subscriptionID}/machineimages/{imageName}")]
        IAsyncResult BeginGetImageProperties(string subscriptionID, string imageName, AsyncCallback callback, object state);
        MachineImage EndGetImageProperties(IAsyncResult asyncResult);

        /// <summary>
        /// Set image properties.
        /// </summary>
        [OperationContract(AsyncPattern = true)]
        [WebInvoke(Method = "POST", UriTemplate = @"{subscriptionID}/machineimages/{imagename}?comp=properties")]
        IAsyncResult BeginSetImageProperties(string subscriptionID, string imageName, SetMachineImagePropertiesInput imageProperties, AsyncCallback callback, object state);
        void EndSetImageProperties(IAsyncResult asyncResult);

        /// <summary>
        /// Set parent image.
        /// </summary>
        [OperationContract(AsyncPattern = true)]
        [WebInvoke(Method = "POST", UriTemplate = @"{subscriptionID}/machineimages/{imagename}?comp=setparent")]
        IAsyncResult BeginSetParentImage(string subscriptionID, string imageName, SetParentImageInput parentImageInput, AsyncCallback callback, object state);
        void EndSetParentImage(IAsyncResult asyncResult);

        /// <summary>
        /// Delete an image.
        /// </summary>
        [OperationContract(AsyncPattern = true)]
        [WebInvoke(Method = "DELETE", UriTemplate = @"{subscriptionID}/machineimages/{imageName}")]
        IAsyncResult BeginDeleteImage(string subscriptionID, string imageName, AsyncCallback callback, object state);
        void EndDeleteImage(IAsyncResult asyncResult);
    }

    public static partial class ServiceManagementExtensionMethods
    {
        public static void PrepareImageUpload(this IServiceManagement proxy, string subscriptionID, string imageName, PrepareImageUploadInput input)
        {
            proxy.EndPrepareImageUpload(proxy.BeginPrepareImageUpload(subscriptionID, imageName, input, null, null));
        }

        public static MachineImageReference GetImageReference(this IServiceManagement proxy, string subscriptionId, string imageName, DateTime expiry, ImageSharedAccessSignaturePermission accessModifier)
        {
            return proxy.EndGetImageReference(proxy.BeginGetImageReference(subscriptionId, imageName, expiry.ToString("o"), accessModifier.ToString().ToLower(), null, null));
        }

        public static void CommitImageUpload(this IServiceManagement proxy, string subscriptionId, string imageName)
        {
            proxy.EndCommitImageUpload(proxy.BeginCommitImageUpload(subscriptionId, imageName, null, null));
        }

        public static MachineImageList ListImages(this IServiceManagement proxy, string subscriptionID)
        {
            return proxy.EndListImages(proxy.BeginListImages(subscriptionID, null, null));
        }

        public static MachineImage GetImageProperties(this IServiceManagement proxy, string subscriptionID, string imageName)
        {
            return proxy.EndGetImageProperties(proxy.BeginGetImageProperties(subscriptionID, imageName, null, null));
        }

        public static void SetImageProperties(this IServiceManagement proxy, string subscriptionID, string imageName, SetMachineImagePropertiesInput input)
        {
            proxy.EndSetImageProperties(proxy.BeginSetImageProperties(subscriptionID, imageName, input, null, null));
        }

        public static void SetParentImage(this IServiceManagement proxy, string subscriptionID, string imageName, SetParentImageInput input)
        {
            proxy.EndSetParentImage(proxy.BeginSetParentImage(subscriptionID, imageName, input, null, null));
        }

        public static void DeleteImage(this IServiceManagement proxy, string subscriptionID, string imageName)
        {
            proxy.EndDeleteImage(proxy.BeginDeleteImage(subscriptionID, imageName, null, null));
        }
    }
}