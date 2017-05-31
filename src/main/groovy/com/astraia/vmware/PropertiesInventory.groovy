package com.astraia.vmware

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Path

/**
 * Created by Ruben on 23.05.2017.
 */
class PropertiesInventory implements Inventory{
    final private static Logger logger = LoggerFactory.getLogger(PropertiesInventory.class);

    private Map<Integer, VMwareInstance> idxInstancesMap = new LinkedHashMap<>();
    private Map<Integer, VMwareInstance> vmListInstancesMap = new LinkedHashMap<>();

    private static final String nl = System.getProperty("line.separator");


    public PropertiesInventory() {
        loadInstances();
    }

    public VMwareInstance getMachine(int index) {
        return idxInstancesMap.get(index);
    }

    public Integer findMachineIndex(File vmxFile) {
        Path path = vmxFile.toPath();
        Map.Entry<Integer, VMwareInstance> res = idxInstancesMap.find { Integer i, VMwareInstance inst ->
            return Files.isSameFile(inst.getVmxFile().toPath(), path)
        };
        if(res==null)
            return -1
        return res.key;
    }

    public boolean removeMachine(int id) {
        VMwareInstance inst = idxInstancesMap.remove(id);
        if(inst!=null) {
            return vmListInstancesMap.remove(inst.getVmlistIndex())!=null;
        }
        return false;
    }

    public Set<VMwareInstance> getInventory() {
        return new HashSet<VMwareInstance>(this.idxInstancesMap.values());
    }

    private void writeVmlist(StringBuilder builder) {
        vmListInstancesMap.forEach({ Integer vmlistKey, VMwareInstance m ->
            m.getVmlistMap().forEach({ String key, String value ->
                builder << "vmlist${vmlistKey}.${key} = ${value}${nl}"
            })
        })
    }

    private void writeIndexes(StringBuilder builder) {
        idxInstancesMap.forEach({ Integer idxKey, VMwareInstance m ->
            m.getIndexMap().forEach({ String key, String value ->
                builder << "index${idxKey}.${key} = ${value}${nl}"
            })
        })
        builder << "index.count = \"${idxInstancesMap.size()}\"${nl}"
    }

    public void write() {
        //write headers
        StringBuilder builder = StringBuilder.newInstance();
        //Globals.inventoryPropsFile
        builder << ".encoding = \"windows-1252\"${nl}"

        writeVmlist(builder);
        writeIndexes(builder);

        Globals.inventoryPropsFile.write(builder.toString());
        logger.debug(builder.toString());
    }

    /**
     * reads the Globals#inventoryPropsFile and instantiate the internal structures
     */
    private void loadInstances() {
        //Properties props = new Properties();
        ///props.load(new StringReader(Globals.inventoryPropsFile.text.replace("\\","\\\\")));
        VMLSProperties props = new VMLSProperties();
        props.loadProperties(Globals.inventoryPropsFile);

        //instantiate the VMwareInstances and fill up internal maps
        int vmCount = props.readNumber("index.count");
        for(int i = 0; i< vmCount; i++) {
            def indexIdKey ="index${i}.id";
            def configPath = props.readProperty(indexIdKey);
            VMwareInstance newInstance = VMwareInstance.fromVmxFile(new File(configPath));
            //find vmlist<<Index>>
            props.any { String key, String value ->
                def kMatcher = key =~ /vmlist(\d)[.]config$/
                if(kMatcher.matches()) {
                    def vMatcher = value =~ /"([\S\s]*)"$/
                    if(vMatcher.matches()) {
                        if(vMatcher.group(1).equals(configPath)) {
                            newInstance.setVmlistIndex(Integer.parseInt(kMatcher.group(1)));
                            return true;
                        }
                    } else {
                        throw new IllegalStateException("found vmlist but could not parse out the double quotes")
                    }
                }
            }
            idxInstancesMap.put(i, newInstance);
            vmListInstancesMap.put(newInstance.getVmlistIndex(), newInstance);
        }

        //fill up the properties for each VMwareinstance
        props.forEach({ String k, String v ->
            def dotIndex = k.indexOf(".");
            def indexPart = k.substring(0, dotIndex);
            def rest = k.substring(dotIndex+1);

            String res = indexPart.replaceAll('\\D*', "")
            if(!res.isEmpty()) {
                int index = Integer.parseInt(res);

                if(indexPart.startsWith("index")) {
                    VMwareInstance inst = idxInstancesMap.get(index);
                    if(inst==null)
                        throw new IllegalStateException("VMwareInstance not found. It should not happen")
                    inst.putIndexProperty(rest, v);
                } else if(indexPart.startsWith("vmlist")) {
                    VMwareInstance inst = vmListInstancesMap.get(index);
                    inst.putVmlistProperty(rest, v);
                }
            }
            //find first digit
        })

        logger.trace("properties inventory initialized! ${vmListInstancesMap.size()} instances");
        logger.debug(vmListInstancesMap.toMapString());
    }
}
