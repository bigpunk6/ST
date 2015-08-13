/**
 *  Linksys/Cisco Internet Camera
 *
 *  Author: mkurtzjr@live.com
 *  Date: 2013-10-08
 *
 *  Modified example Foscam device type to support dynamic input of credentials
 *  and enable / disable motion to easily integrate into homemade
 *  security systems (when away, mark "motiondecetionStatus" as "on", when present, mark
 *  "motiondecetionStatus" as "off".  For use with email or FTP image uploading built
 *  into Linksys/Cisco cameras.
 *
 *  Capability: Image Capture, Polling
 *  Custom Attributes: motiondecetionStatus
 *  Custom Commands: motiondecetionOn, motiondecetionOff, toggleMotiondecetion
 */
 
preferences {
  input("username", "text", title: "Username", description: "Your Camera username")
  input("password", "password", title: "Password", description: "Your Camera password")
  input("ip", "text", title: "IP address", description: "The IP address of your Camera")
  input("port", "text", title: "Port", description: "The port of your Camera")
  input "size", "enum", title: "Image Resolution", metadata: [values: ["160x120","320x240","640x480"]]
  input "quality", "enum", title: "Image Quality", metadata: [values: ["Very high","High","Normal","Low","Very low"]]
}

// for the UI
metadata {
  definition (name: "Linksys/Cisco Internet Camera", author: "bigpunk6", namespace:  "bigpunk6") {
		capability "Actuator"
		capability "Sensor"
		capability "Image Capture"
    }
        
  tiles {
    carouselTile("cameraDetails", "device.image", width: 3, height: 2) { }

    standardTile("camera", "device.image", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
      state "default", label: "", action: "Image Capture.take", icon: "st.camera.dropcam-centered", backgroundColor: "#FFFFFF"
    }

    standardTile("take", "device.image", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false, decoration: "flat") {
      state "take", label: "", action: "Image Capture.take", icon: "st.secondary.take", nextState:"taking"
    }

    standardTile("motiondecetionStatus", "device.motiondecetionStatus", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
      state "off", label: "off", action: "toggleMotiondecetion", icon:"st.motion.motion.inactive", backgroundColor: "#bc2323"
      state "on", label: "on", action: "toggleMotiondecetion", icon:"st.motion.motion.active",  backgroundColor: "#79b821"
    }

    standardTile("refresh", "device.motiondecetionStatus", inactiveLabel: false, decoration: "flat") {
        state "default", action:"polling.poll", icon:"st.secondary.refresh"
    }

    main "camera"
      details(["cameraDetails", "take", "motiondecetionStatus", "refresh"])
  }
}

def parseCameraResponse(def response) {
  if (response.headers.'Content-Type'.contains("image/jpeg")) {
    def imageBytes = response.data
    if (imageBytes) {
      storeImage(getPictureName(), imageBytes)
    }
  } 
}

private getPictureName() {
  def pictureUuid = java.util.UUID.randomUUID().toString().replaceAll('-', '')
  "image" + "_$pictureUuid" + ".jpg"
}

private take() {
  log.debug("Take a photo")
  
  if(size == "160x120"){
  set usize = 1
  }
  api("snapshot", "${usize}&${uquality}") {
    log.debug("Image captured: ${size} ${quality}")
  }
  

  httpGet("http://${settings.username}:${settings.password}@${settings.ip}:${settings.port}/img/snapshot.cgi") {response ->        
    log.debug("Image captured")
    parseCameraResponse(response)
  }
}

def toggleMotiondecetion() {
  if(device.currentValue("motiondecetionStatus") == "on") {
    motiondecetionOff()
  }

  else {
    motiondecetionOn()
  }
}

private motiondecetionOn() {
  api("set_group", "md_mode=1") {
    log.debug("Motion Detection changed to: on")
    sendEvent(name: "motiondecetionStatus", value: "on");
  }
}

private motiondecetionOff() {
  api("set_group", "md_mode=0") {
    log.debug("Motion Detection changed to: off")
    sendEvent(name: "motiondecetionStatus", value: "off");
  }
}

def pause() {
  api("decoder_control", "command=1") {}
}

def api(method, args = [], success = {}) {
  def methods = [
    "snapshot": [uri: "http://${username}:${password}@${ip}:${port}/img/snapshot.cgi?${args}", type: "get"],
    "reboot": [uri: "http://${username}:${password}@${ip}:${port}/adm/reboot.cgi?", type: "get"],
    "set_group": [uri: "http://${username}:${password}@${ip}:${port}/adm/set_group.cgi?group=MOTION&${args}", type: "get"],
    "get_group": [uri: "http://${username}:${password}@${ip}:${port}/adm/get_group.cgi?group=MOTION", type: "get"],
  ]

  def request = methods.getAt(method)

  doRequest(request.uri, request.type, success)
}

private doRequest(uri, type, success) {
  log.debug(uri)

  if(type == "post") {
    httpPost(uri , "", success)
  }

  else if (type == "get") {
    httpGet(uri, success)
  }
}

def poll() {
  api("get_group", []) {
    def params = ""

    it.data.eachLine {
      if(it.startsWith("md_mode=0")) {
        log.info("Polled: Motion Detection off")
        sendEvent(name: "motiondecetionStatus", value: "off");
      }

      if(it.startsWith("md_mode=1")) {
        log.info("Polled: Motion Detection on")
        sendEvent(name: "motiondecetionStatus", value: "on");
      }
    }
  }
}