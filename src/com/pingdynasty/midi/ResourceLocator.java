package com.pingdynasty.midi;

import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Utility class to find system resources, icons etc.
 */
public class ResourceLocator {

//     private static ClassLoader loader = null;

    public static URL getResourceURL(String systemId){
	try{
            URL url;
//             if(loader == null){
                // try for system resource
                url = ClassLoader.getSystemResource(systemId);
                if(url == null){
                    // not in system classpath, try context classloader
                    ClassLoader loader = 
                        Thread.currentThread().getContextClassLoader();
                    url = loader.getSystemResource(systemId);
                    if(url == null)
                        url = loader.getResource(systemId);
                }
                if(url == null)
                    url = ClassLoader.getSystemClassLoader().getResource(systemId);
//             }else{
//                 url = loader.getSystemResource(id);
//                 if(url == null)
//                     url = loader.getResource(id);
//             }
	    return url;
	}catch(Exception exc){
            return null;
        }
    }

    public static Icon getIcon(String systemId){
        URL url = getResourceURL(systemId);
        if(url == null)
            return null;
        return new ImageIcon(url);
    }

}
