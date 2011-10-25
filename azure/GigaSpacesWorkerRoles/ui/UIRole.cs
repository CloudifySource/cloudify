using System;
using Microsoft.WindowsAzure.ServiceRuntime;

namespace GigaSpaces
{
    public class UIRole : RoleCommonEntryPoint
    {
        protected override int WebuiMegabytesMemory
        {
            get { return GetInt32Config("GigaSpaces.XAP.WEBUI.MemoryInMB"); }
        }

        protected override int RestAdminMegabytesMemory
        {

            get { return GetInt32Config("GigaSpaces.XAP.RestAdmin.MemoryInMB"); }
        }

    }
}
