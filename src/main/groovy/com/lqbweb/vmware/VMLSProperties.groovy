package com.lqbweb.vmware

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by Ruben on 29.05.2017.
 */
public class VMLSProperties extends LinkedHashMap<String, String>{
    final private static Logger logger = LoggerFactory.getLogger(VMLSProperties.class);

    public String readProperty(String key) {
        String res=get(key);
        if(res!=null && !res.isEmpty()) {
            return res.replaceAll(/^\"|\"$/, "");
        } else {
            return null;
        }
    }

    public int readNumber(String key) {
        return Integer.parseInt(readProperty(key));
    }

    public void loadProperties(File f) {
        f.eachLine { String line ->
            String[] res = line.tokenize("=").collect { it.trim() };
            put(res[0], res[1]);
        }
    }
}
