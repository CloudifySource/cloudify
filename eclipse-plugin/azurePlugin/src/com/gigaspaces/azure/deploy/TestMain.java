package com.gigaspaces.azure.deploy;

import com.gigaspaces.azure.deploy.commands.AzureDeployApplication;


public class TestMain {

    public static void main(String[] args) throws Exception {
        
        AzureDeployApplication deploy = new AzureDeployApplication();
        
        deploy.setAzureDeploymentName("gs-hosted-service");
        deploy.setAzureHostedServiceName("gs-deployment");
        deploy.setAzureRemoteDesktopPfxFilePassword("123456");
        
        deploy.deploy();

        
        // certificate generated from 'https://windows.azure.com/download/publishprofile.aspx' (empty password)
//        String cert = "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";
//        byte[] decodedCert = Base64.decode(cert);
//        ByteArrayInputStream bis = new ByteArrayInputStream(decodedCert);
//        FileOutputStream fis = new FileOutputStream("d:/home/temp/test.pfx");
//        
//        byte[] buffer = new byte[1024];
//        int bytesRead = 0;
//        while ((bytesRead = bis.read(buffer)) > 0) {
//            fis.write(buffer,0, bytesRead);
//        }
//        
//        fis.close();
//        bis.close();
        
    }
    
}
