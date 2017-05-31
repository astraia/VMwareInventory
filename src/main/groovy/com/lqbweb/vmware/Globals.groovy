package com.lqbweb.vmware

/**
 * Created by Ruben on 23.05.2017.
 */
class Globals {
    public final static File allVmwareProfile = new File(System.getenv("ALLUSERSPROFILE"), "VMware")
    public final static File vmwareRoamingFile = new File(System.getenv("APPDATA"), "VMware")
    public final static File inventoryXmlFile = new File(allVmwareProfile, "hostd/vmInventory.xml")
    public final static File inventoryPropsFile = new File(vmwareRoamingFile, "inventory.vmls")
    public final static File autoStartFile = new File(allVmwareProfile, "hostd/vmAutoStart.xml")

}
