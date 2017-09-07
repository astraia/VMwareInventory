package com.astraia.vmware

import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
            return inst.getVmxFile().toPath().equals(path);
        };
        if(res==null)
            return -1
        return res.key;
    }

    @Override
    public boolean removeMachine(File vmxFile) {
        Integer idxFile = findMachineIndex(vmxFile);
        if(idxFile>=0) {
            return removeMachine(idxFile);
        }
        return false;
    }

    public boolean removeMachine(int id) {
        VMwareInstance inst = idxInstancesMap.remove(id);
        if(inst!=null) {
            return vmListInstancesMap.remove(inst.getVmlistIndex())!=null;
        }
        return false;
    }

    @Override
    public Set<VMwareInstance> getInventory() {
        return new HashSet<VMwareInstance>(this.idxInstancesMap.values());
    }

    private void writeVmlist(StringBuilder builder) {
        int counter=1;
        vmListInstancesMap.forEach({ Integer vmlistKey, VMwareInstance m ->
            m.getVmlistMap().forEach({ String key, String value ->
                builder << "vmlist${counter}.${key} = ${value}${nl}"
            })
            counter++;
        })
    }

    private void writeIndexes(StringBuilder builder) {
        int counter=0;
        idxInstancesMap.forEach({ Integer idxKey, VMwareInstance m ->
            m.getIndexMap().forEach({ String key, String value ->
                builder << "index${counter}.${key} = ${value}${nl}"
            })
            counter++;
        })
        builder << "index.count = \"${idxInstancesMap.size()}\"${nl}"
    }

    @Override
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
        instantiateMachines(props)

        //fill up the properties for each VMwareinstance
        fillProperties(props)

        logger.trace("properties inventory initialized! ${vmListInstancesMap.size()} instances");
        logger.debug(vmListInstancesMap.toMapString());
    }

    private fillProperties(VMLSProperties props) {
        props.forEach({ String k, String v ->
            def dotIndex = k.indexOf(".");
            def indexPart = k.substring(0, dotIndex);
            def rest = k.substring(dotIndex + 1);
            def valueParsed = parseValue(v);

            if(!valueParsed.isEmpty()) {
                String res = indexPart.replaceAll('\\D*', "")
                if (!res.isEmpty()) {
                    int index = Integer.parseInt(res);

                    if (indexPart.startsWith("index")) {
                        VMwareInstance inst = idxInstancesMap.get(index);
                        if (inst == null)
                            throw new IllegalStateException("VMwareInstance not found. This should not happen")
                        inst.putIndexProperty(rest, v);
                    } else if (indexPart.startsWith("vmlist")) {
                        VMwareInstance inst = vmListInstancesMap.get(index);
                        if (inst == null) {
                            logger.warn("could not find VMware instance for index ${index} value ${v}. Ignoring vmlist${index}");
                        } else {
                            inst.putVmlistProperty(rest, v);
                        }
                    }
                }
            }
        })
    }

    private String parseValue(String withQuotes) {
        def vMatcher = withQuotes =~ /"([\S\s]*)"$/
        if (vMatcher.matches()) {
            return vMatcher.group(1);
        }
        throw IllegalArgumentException("invalid expression ${withQuotes}");
    }

    private void instantiateMachines(VMLSProperties props) {
        int vmCount = props.readNumber("index.count");
        for (int i = 0; i < vmCount; i++) {
            String indexIdKey = "index${i}.id";
            String configPath = props.readProperty(indexIdKey);
            if(configPath==null)
                throw new IllegalStateException("index.count is higher than the number of actual machines?. Inventory corrupted?");

            VMwareInstance newInstance = new VMwareInstance(new File(configPath));
            //find vmlist<<Index>>
            props.any { String key, String value ->
                def kMatcher = key =~ /vmlist(\d)[.]config$/
                if (kMatcher.matches()) {
                    String valueParsed = parseValue(value);
                    if (valueParsed.equals(configPath)) {
                        newInstance.setVmlistIndex(Integer.parseInt(kMatcher.group(1)));
                        return true;
                    }
                }
            }
            idxInstancesMap.put(i, newInstance);
            vmListInstancesMap.put(newInstance.getVmlistIndex(), newInstance);
        }
    }
}
