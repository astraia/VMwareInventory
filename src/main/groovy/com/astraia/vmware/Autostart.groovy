package com.astraia.vmware

import groovy.util.slurpersupport.GPathResult
import groovy.xml.XmlUtil

/**
 * Created by Ruben on 30.05.2017.
 */
class Autostart {
    private GPathResult xml;

    public Autostart() {
        xml = new XmlSlurper().parse(Globals.autoStartFile);
    }

    public boolean isMoidIn(int moid) {
        if(!isAutostartNodeThere())
            return false;

        def res = xml.AutoStartOrder.e.findAll {
            it.key.moid == "${moid}"
        }
        return res.size()>0;
    }



    public void removeInstance(int moid) {
        def res = xml.AutoStartOrder.e.findAll {
            it.key.moid == "${moid}"
        }
        res.replaceNode {}

        int length = Integer.parseInt(xml.AutoStartOrder._length.text());
        xml.AutoStartOrder._length.replaceBody length - 1;
    }


    private boolean isAutostartNodeThere() {
        return xml.AutoStartOrder.size() > 0;
    }

    private void initializeAutostartNode() {
        println("initializing autostart node")
        xml.appendNode {
            AutoStartOrder {
                _length("0")
                _type("vim.host.AutoStartManager.AutoPowerInfo[]")
            }
        }
        xml = new XmlSlurper().parseText(XmlUtil.serialize(xml));       //refresh the slurped structures!
    }

    public void addInstance(VMwareInstance instance) {
        if(!isAutostartNodeThere()) {
            initializeAutostartNode();
        }

        println xml.AutoStartOrder._length.text()
        int length = Integer.parseInt(xml.AutoStartOrder._length.text());

        xml.AutoStartOrder.appendNode{
            e(id: "${length}") {
                _type("vim.host.AutoStartManager.AutoPowerInfo")
                key {
                    _type("vim.VirtualMachine")
                    moid(instance.getObjId())
                }
                startAction("PowerOn")
                startDelay("-1")
                startOrder("-1")
                stopAction("GuestShutdown")
                stopDelay("120")
                waitForHeartbeat("systemDefault")
            }
        }

        xml.AutoStartOrder._length.replaceBody length + 1;

        println XmlUtil.serialize(xml)
    }

    public void write() {
        Globals.autoStartFile.write(XmlUtil.serialize(xml))
    }
}
