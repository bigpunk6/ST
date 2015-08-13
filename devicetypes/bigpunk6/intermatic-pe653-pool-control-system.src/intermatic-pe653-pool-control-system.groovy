/**
 *  Intermatic PE653 Pool Control System
 *
 *  Copyright 2014 bigpunk6
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "Intermatic PE653 Pool Control System", author: "bigpunk6", namespace:  "bigpunk6") {
        capability "Actuator"
		capability "Switch"
		capability "Polling"
		capability "Configuration"
		capability "Refresh"
		capability "Temperature Measurement"
		capability "Sensor"
                capability "Actuator"
		capability "Zw Multichannel"

		attribute "switch1", "string"
		attribute "switch2", "string"
		attribute "switch3", "string"
		attribute "switch4", "string"
		attribute "switch5", "string"

		command "onMulti"
		command "offMulti"
        command "on1"
		command "off1"
        command "on2"
		command "off2"
        command "on3"
		command "off3"
        command "on4"
		command "off4"
        command "on5"
		command "off5"
		
		fingerprint deviceId: "0x1001", inClusters: "0x91,0x73,0x72,0x86,0x81,0x60,0x70,0x85,0x25,0x27,0x43,0x31", outClusters: "0x82"
	}

	simulator {
		status "on":  "command: 2003, payload: FF"
		status "off": "command: 2003, payload: 00"
		reply "8E010101,delay 800,6007": "command: 6008, payload: 4004"
		reply "8505": "command: 8506, payload: 02"
		reply "59034002": "command: 5904, payload: 8102003101000000"
		reply "6007":  "command: 6008, payload: 0002"
		reply "600901": "command: 600A, payload: 10002532"
		reply "600902": "command: 600A, payload: 210031"
	}
    
	// tile definitions
	tiles {
		standardTile("switch1", "device.switch1",canChangeIcon: true) {
			state "on", label: "switch1", action: "off1", icon: "st.switches.switch.on", backgroundColor: "#79b821"
			state "off", label: "switch1", action: "on1", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
		}
        standardTile("switch2", "device.switch2",canChangeIcon: true) {
			state "on", label: "switch2", action: "off2", icon: "st.switches.switch.on", backgroundColor: "#79b821"
			state "off", label: "switch2", action: "on2", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
		}
        standardTile("switch3", "device.switch3",canChangeIcon: true) {
			state "on", label: "switch3", action: "off3", icon: "st.switches.switch.on", backgroundColor: "#79b821"
			state "off", label: "switch3", action:"on3", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
		}
        standardTile("switch4", "device.switch4",canChangeIcon: true) {
			state "on", label: "switch4", action: "off4", icon: "st.switches.switch.on", backgroundColor: "#79b821"
			state "off", label: "switch4", action:"on4", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
		}
        standardTile("switch5", "device.switch5",canChangeIcon: true) {
			state "on", label: "switch5", action: "off5", icon: "st.switches.switch.on", backgroundColor: "#79b821"
			state "off", label: "switch5", action:"on5", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
		}
        
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        
        standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}
        
        valueTile("temperature", "device.temperature") {
			state("temperature", label:'${currentValue}°', unit:"F",
				backgroundColors:[
					[value: 32, color: "#153591"],
					[value: 54, color: "#1e9cbb"],
					[value: 64, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 90, color: "#f1d801"],
					[value: 98, color: "#d04e00"],
					[value: 110, color: "#bc2323"]
				]
			)
		}
        
		main(["switch1","switch2","switch3","switch4","switch5","temperature"])
        details(["switch1","switch2","switch3","switch4","switch5","temperature","refresh"])
	}
}

import physicalgraph.zwave.commands.*

//Parse
def parse(String description) {
	def result = null
	if (description.startsWith("Err")) {
	    result = createEvent(descriptionText:description, isStateChange:true)
	} else if (description != "updated") {
		def cmd = zwave.parse(description, [0x20: 1, 0x70: 2, 0x86: 1, 0x60:3, 0x31:1, 0x25:1, 0x81:1])
		if (cmd) {
                    if( cmd.CMD == "6006" ) {
                        def map = [ name: "switch$cmd.instance" ]
                        if (cmd.commandClass == 37){
                            if (cmd.parameter == [0]) {
                                map.value = "off"
                            }
                            if (cmd.parameter == [255]) {
                               map.value = "on"
                            }
                        }
                        result = createEvent(map)
                    } else {
                    	result = createEvent(zwaveEvent(cmd))
                    }
        	}
	}
	log.debug("'$description' parsed to $result")
	return result
}

//Reports

    //Temperature
def zwaveEvent(sensormultilevelv1.SensorMultilevelReport cmd) {
	def map = [:]
	map.value = cmd.scaledSensorValue.toString()
	map.unit = cmd.scale == 1 ? "F" : "C"
	map.name = "temperature"
	map
}

def zwaveEvent(thermostatsetpointv2.ThermostatSetpointReport cmd) {
	def map = [:]
	map.value = cmd.scaledValue.toString()
	map.unit = cmd.scale == 1 ? "F" : "C"
	map.displayed = false
	switch (cmd.setpointType) {
		case 1:
			map.name = "poolSetpoint"
			break;
		case 7:
			map.name = "spaSetpoint"
			break;
		default:
			return [:]
	}
	// So we can respond with same format
	state.size = cmd.size
	state.scale = cmd.scale
	state.precision = cmd.precision
	map
}

def zwaveEvent(multichannelv3.MultiInstanceReport cmd) {
    log.debug "$cmd"
}

private List loadEndpointInfo() {
	if (state.endpointInfo) {
		state.endpointInfo
	} else if (device.currentValue("epInfo")) {
		fromJson(device.currentValue("epInfo"))
	} else {
		[]
	}
}

/def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelEndPointReport cmd) {
	updateDataValue("endpoints", cmd.endPoints.toString())
	if (!state.endpointInfo) {
		state.endpointInfo = loadEndpointInfo()
	}
	if (state.endpointInfo.size() > cmd.endPoints) {
		cmd.endpointInfo
	}
	state.endpointInfo = [null] * cmd.endPoints
	//response(zwave.associationV2.associationGroupingsGet())
	[ createEvent(name: "epInfo", value: util.toJson(state.endpointInfo), displayed: false, descriptionText:""),
	  response(zwave.multiChannelV3.multiChannelCapabilityGet(endPoint: 1)) ]
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCapabilityReport cmd) {
	def result = []
	def cmds = []
	if(!state.endpointInfo) state.endpointInfo = []
	state.endpointInfo[cmd.endPoint - 1] = cmd.format()[6..-1]
	if (cmd.endPoint < getDataValue("endpoints").toInteger()) {
		cmds = zwave.multiChannelV3.multiChannelCapabilityGet(endPoint: cmd.endPoint + 1).format()
	} else {
		log.debug "endpointInfo: ${state.endpointInfo.inspect()}"
	}
	result << createEvent(name: "epInfo", value: util.toJson(state.endpointInfo), displayed: false, descriptionText:"")
	if(cmds) result << response(cmds)
	result
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
	state.groups = cmd.supportedGroupings
	if (cmd.supportedGroupings > 1) {
		[response(zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier:2, listMode:1))]
	}
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupInfoReport cmd) {
	def cmds = []
	/*for (def i = 0; i < cmd.groupCount; i++) {
		def prof = cmd.payload[5 + (i * 7)]
		def num = cmd.payload[3 + (i * 7)]
		if (prof == 0x20 || prof == 0x31 || prof == 0x71) {
			updateDataValue("agi$num", String.format("%02X%02X", *(cmd.payload[(7*i+5)..(7*i+6)])))
			cmds << response(zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier:num, nodeId:zwaveHubNodeId))
		}
	}*/
	for (def i = 2; i <= state.groups; i++) {
		cmds << response(zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier:i, nodeId:zwaveHubNodeId))
	}
	cmds
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand([0x25: 1, 0x20: 1])
	if (encapsulatedCommand) {
		if (state.enabledEndpoints.find { it == cmd.sourceEndPoint }) {
			def formatCmd = ([cmd.commandClass, cmd.command] + cmd.parameter).collect{ String.format("%02X", it) }.join()
			createEvent(name: "epEvent", value: "$cmd.sourceEndPoint:$formatCmd", isStateChange: true, displayed: false, descriptionText: "(fwd to ep $cmd.sourceEndPoint)")
		} else {
			zwaveEvent(encapsulatedCommand, cmd.sourceEndPoint as Integer)
		}
	}
}

/*def zwaveEvent(multichannelv3.MultiChannelCmdEncap cmd) {
    log.debug "$cmd"
    def map = [ name: "switch$cmd.destinationEndPoint" ]
        if (cmd.commandClass == 37){
            if (cmd.parameter == [0]) {
                map.value = "off"
            }
            if (cmd.parameter == [255]) {
                map.value = "on"
            }
        }
    createEvent(map)
}*/

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	createEvent(descriptionText: "$device.displayName: $cmd", isStateChange: true)
}

//Commands

def configure() {
	commands([
		zwave.multiChannelV3.multiChannelEndPointGet()
	], 800)
}

def epCmd(Integer ep, String cmds) {
	def result
	if (cmds) {
		def header = state.sec ? "988100600D00" : "600D00"
		result = cmds.split(",").collect { cmd -> (cmd.startsWith("delay")) ? cmd : String.format("%s%02X%s", header, ep, cmd) }
	}
	result
}

def enableEpEvents(enabledEndpoints) {
	state.enabledEndpoints = enabledEndpoints.split(",").findAll()*.toInteger()
	null
}

private commands(commands, delay=200) {
	delayBetween(commands.collect{ command(it) }, delay)
}

private encap(cmd, endpoint) {
	if (endpoint) {
		command(zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint:endpoint).encapsulate(cmd))
	} else {
		command(cmd)
	}
}

private encapWithDelay(commands, endpoint, delay=200) {
	delayBetween(commands.collect{ encap(it, endpoint) }, delay)
}

def setPoolSetpoint(degreesF) {
	setHeatingSetpoint(degreesF.toDouble())
}

def setPoolSetpoint(Double degreesF) {
	def p = (state.precision == null) ? 1 : state.precision
	delayBetween([
		zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 1, scale: 1, precision: p, scaledValue: degreesF).format(),
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 1).format()
	])
}

def setSpaSetpoint(degreesF) {
	setSpaSetpoint(degreesF.toDouble())
}

def setSpaSetpoint(Double degreesF) {
	def p = (state.precision == null) ? 1 : state.precision
	delayBetween([
		zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 7, scale: 1, precision: p, scaledValue: degreesF).format(),
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 7).format()
	])
}

def on() {
	delayBetween([
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint: 1, destinationEndPoint: 1, commandClass:37, command:1, parameter:[255]).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint: 1, destinationEndPoint: 1, commandClass:37, command:2).format()
	], 2300)
}

def off() {
	delayBetween([
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint: 1, destinationEndPoint: 1, commandClass:37, command:1, parameter:[0]).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint: 1, destinationEndPoint: 1, commandClass:37, command:2).format()
	], 2300)
}

//switch instance
def onMulti(value) {
	delayBetween([
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint: value, destinationEndPoint: value, commandClass:37, command:1, parameter:[255]).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint: value, destinationEndPoint: value, commandClass:37, command:2).format()
	], 2300)
}

def offMulti(value) {
	delayBetween([
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint: value, destinationEndPoint: value, commandClass:37, command:1, parameter:[0]).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint: value, destinationEndPoint: value, commandClass:37, command:2).format()
	], 2300)
}

//switch1
def on1() {
	onMulti(1)
}

def off1() {
	offMulti(1)
}

//switch2
def on2() {
	onMulti(2)
}

def off2() {
	offMulti(2)
}

//switch3
def on3() {
	onMulti(3)
}

def off3() {
	offMulti(3)
}

//switch4
def on4() {
	onMulti(4)
}

def off4() {
	offMulti(4)
}

//switch5
def on5() {
	onMulti(5)
}

def off5() {
	offMulti(5)
}

def poll() {
    zwave.sensorMultilevelV1.sensorMultilevelGet().format()
}

def refresh() {
	delayBetween([
    zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format(),
    zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:2).format(),
    zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:3, destinationEndPoint:3, commandClass:37, command:2).format(),
    zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:4, destinationEndPoint:4, commandClass:37, command:2).format(),
    zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:5, destinationEndPoint:5, commandClass:37, command:2).format(),
    zwave.sensorMultilevelV1.sensorMultilevelGet().format(),
    //zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 1).format(),
    //zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 7).format()
    ], 2500)
}
