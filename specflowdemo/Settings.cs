using System;

namespace specflowdemo
{
    public class Settings
    {
        public string Browser
        {
            get { return Environment.GetEnvironmentVariable("SAUCELABS_BROWSER"); }
        }

        public string Platform
        {
            get { return Environment.GetEnvironmentVariable("SAUCELABS_PLATFORM"); }
        }

        public string Version
        {
            get { return Environment.GetEnvironmentVariable("SAUCELABS_VERSION"); }
        }

        public string Username
        {
            get { return Environment.GetEnvironmentVariable("SAUCELABS_USERNAME"); }
        }

        public string ApiKey
        {
            get { return Environment.GetEnvironmentVariable("SAUCELABS_APIKEY"); }
        }
    }
}
