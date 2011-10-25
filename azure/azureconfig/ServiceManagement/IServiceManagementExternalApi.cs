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
using System.ServiceModel;

[assembly: CLSCompliant(true)]

namespace Microsoft.Samples.WindowsAzure.ServiceManagement
{
    /// <summary>
    /// Provides the Windows Azure Service Management Api. 
    /// </summary>
    [ServiceContract(Namespace = Constants.ServiceManagementNS)]
    public partial interface IServiceManagement
    {
    }
}
