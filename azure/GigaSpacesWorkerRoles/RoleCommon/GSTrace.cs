using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Diagnostics;

namespace GigaSpaces
{
    class GSTrace
    {
        public static void WriteLine(String message)
        {
            Trace.WriteLine(message, "[GigaSpaces]");
        }

        public static void Flush()
        {
            Trace.Flush();
        }
    }
}
