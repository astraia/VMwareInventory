package com.lqbweb.vmware

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Path
import java.text.DecimalFormat

/**
 * Created by Ruben on 18.05.2017.
 */
class VMwareInstance {
    final private static Logger logger = LoggerFactory.getLogger(VMwareInstance.class);

    private File instanceFile;
    private File vmxFile;
    private final Map<String, String> indexProperties = new LinkedHashMap<>();
    private final Map<String, String> vmlistProperties = new LinkedHashMap<>();
    private int objId=-1;
    private int vmlistIndex=-1;


    public VMwareInstance(File instanceFile) {
        this.instanceFile = instanceFile
        this.vmxFile = InstanceTools.getVmxFile(this.instanceFile);
    }

    boolean exists() {
        return this.instanceFile.isDirectory();
    }

    public File getInstanceFile() {
        return this.instanceFile;
    }

    File getVmxFile() {
        return this.vmxFile;
    }

    public void putIndexProperty(String propName, String value){
        indexProperties.put(propName, value);
    }

    public void putVmlistProperty(String propName, String value){
        vmlistProperties.put(propName, value);
    }

    public Map<String, String> getVmlistMap() {
        return new LinkedHashMap<>(this.vmlistProperties);
    }

    public Map<String, String> getIndexMap() {
        return new LinkedHashMap<>(this.indexProperties);
    }

    public void setObjId(int id) {
        this.objId = id;
    }

    public int getObjId() {
        return this.objId;
    }

    public void setVmlistIndex(int idx) {
        this.vmlistIndex=idx;
    }

    public int getVmlistIndex() {
        return this.vmlistIndex;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof VMwareInstance) {
            VMwareInstance objCasted = (VMwareInstance) obj;
            Path objVmxFilePath = objCasted.vmxFile.toPath();
            if(Files.isSameFile(objVmxFilePath, this.vmxFile.toPath())) {
                return objId == objCasted.getObjId();
            }
        }
        return false;
    }

    public int hashCode() {
        return objId.hashCode() + this.vmxFile.hashCode();
    }

    public static VMwareInstance fromVmxFile(File vmxFile) {
        return new VMwareInstance(vmxFile.getParentFile());
    }

    public static String convertObjId(int id) {
        DecimalFormat formatter = new DecimalFormat("####")
        return formatter.format(id);
    }

}
