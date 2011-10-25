using System;
using Microsoft.WindowsAzure.ServiceRuntime;

namespace GigaSpaces
{
    public class ManagementRole : RoleCommonEntryPoint
    {

        protected override int GsmMegabytesMemory
        {
            get { return GetInt32Config("GigaSpaces.XAP.GSM.MemoryInMB"); }
        }

        protected override int LusMegabytesMemory
        {
            get { return GetInt32Config("GigaSpaces.XAP.LUS.MemoryInMB"); }
        }

        protected override int EsmMegabytesMemory
        {
            get { return GetInt32Config("GigaSpaces.XAP.ESM.MemoryInMB"); }
        }

        protected override int RestAdminMegabytesMemory
        {

            get { return GetInt32Config("GigaSpaces.XAP.RestAdmin.MemoryInMB"); }
        }
    }
}
