package com.astraia.vmware

import com.adarshr.args.ArgsEngine
import com.astraia.vmware.InstanceTools.VmxFileNotFound
import groovy.util.slurpersupport.GPathResult
import groovy.util.slurpersupport.NodeChild
import groovy.xml.XmlUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Path
import java.text.DecimalFormat

/**
 * Created by Ruben on 18.05.2017.
 */
class GlobalInventory implements Inventory{
    final private static Logger logger = LoggerFactory.getLogger(GlobalInventory.class);

    private Set<VMwareInstance> globalInstances = new HashSet<>();
    private GPathResult inventoryParsed;

    public GlobalInventory() {
        inventoryParsed = new XmlSlurper().parse(Globals.inventoryXmlFile)
        inventoryParsed.ConfigEntry.each { entry ->
            File vmxFile=new File(entry.vmxCfgPath.text());
            if(vmxFile.isFile()) {
                VMwareInstance newInstance = VMwareInstance.fromVmxFile(vmxFile);
                int objId = Integer.parseInt((String)entry.objID);
                if(validateObjId(objId)) {
                    newInstance.setObjId(objId);
                } else {
                    throw new IllegalStateException("duplicate ID!");
                }
                globalInstances.add(newInstance);
            }
        }
        logger.debug("global inventory initialized with ${globalInstances.size()} instances")
    }

    public void addInstance(VMwareInstance instance) {
        if(this.isInstanceInInventory(instance.getVmxFile()))
            throw new IllegalStateException("Instance already in the GlobalInventory. Make sure to delete it beforehand");

        this.globalInstances.add(instance);
        if(instance.getObjId()<0) {
            instance.setObjId(generateId());
        }
    }

    private int generateId() {
        int newId = this.globalInstances.size() * 16;
        while(!validateObjId(newId)) {
            newId= VMwareInstance.convertObjId(newId+1);
        }
        return newId;
    }

    private boolean validateObjId(int objId) {
        return this.globalInstances.find {VMwareInstance inst ->
            inst.getObjId()==objId
        }==null;
    }

    public void write() {
        XmlSlurper xmlSlurper = new XmlSlurper();
        GPathResult newRoot = xmlSlurper.parseText("<ConfigRoot/>")

        DecimalFormat formatter = new DecimalFormat("####")
        this.globalInstances.eachWithIndex{ VMwareInstance entry, int i ->
            newRoot.appendNode{
                ConfigEntry(id: formatter.format(i)){
                    if(entry.getObjId()<0) {
                        objID(i*16) //ids should be multiple of 16 according to official vmware documentation
                    } else {
                        objID(entry.getObjId()) //ids should be multiple of 16 according to official vmware documentation
                    }
                    vmxCfgPath(entry.getVmxFile().getAbsolutePath());
                }
            }
        }

        Globals.inventoryXmlFile.write(XmlUtil.serialize(newRoot))
    }

    public VMwareInstance findInstance(File vmxFile) {
        Path vmxFilePath = vmxFile.toPath();
        return this.globalInstances.find { VMwareInstance instance ->
            return Files.isSameFile(instance.getVmxFile().toPath(), vmxFilePath);
        };
    }

    /**
     * Removes the specified instance from the global inventory.
     * If you want to persist the changes you need to write it later.
     *
     * @param inst
     * @return null if the instance could not be found. The instance removed if removed.
     */
    public VMwareInstance removeInstance(VMwareInstance inst) {
        if(this.globalInstances.remove(inst)!=null) {
            return inst;
        }
        return null;
    }

    public Set<VMwareInstance> getInventory() {
        return new HashSet<VMwareInstance>(this.globalInstances);
    }

    private boolean isInstanceInInventory(File instanceVmx) {
        def res = inventoryParsed.ConfigEntry.find( {
            File vmxIt=new File(it.vmxCfgPath.text())
            if(vmxIt.isFile()) {
                Files.isSameFile(vmxIt.toPath(), instanceVmx.toPath())
            }
        } );
        return res instanceof NodeChild
    }

}
