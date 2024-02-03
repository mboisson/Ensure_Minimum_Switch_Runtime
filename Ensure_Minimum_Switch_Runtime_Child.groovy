/*
 *  Ensure Minimum Switch Runtime Child App
 *  Project URL: https://github.com/mboisson/Ensure_Minimum_Switch_Runtime
 *  Copyright 2024 Maxime Boissonneault
 *
 *  This app requires it's parent app to function, please go to the project page for more information.
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

definition(
	name: "Ensure Minimum Switch Runtime Child",
	namespace: "mboisson",
	author: "Maxime Boissonneault",
	description: "Ensures that a given switch runs at least for a minimum duration within a specific interval",
	category: "Green Living",
	iconUrl: "TODO",
	iconX2Url: "TODO",
	importUrl: "TODO",
    parent: "mboisson:Ensure Minimum Switch Runtime Manager"
)


preferences {
	page(name: "pageConfig") // Doing it this way elimiates the default app name/mode options.
}


def pageConfig() {          
    // Display all options for a new instance of the Advanced Broadlink Remote
	dynamicPage(name: "", title: "", install: true, uninstall: true, refreshInterval:0) {
		section() {
			label title: "Name of new Ensure Minimum Switch Runtime app:", required: true
		}
		
		section("Disable switch"){
			input "control_switch", "capability.switch", title: "Control of the monitor switch will only happen if switch is on", multiple: false, required: false
		}
		section("Switch to monitor and turn on"){
			input "monitor_switch", "capability.switch", title: "Switch", multiple: false, require: true
		}
        section("Runtime") {
			input (name: "minimum_runtime", type: "number", title: "Minimum runtime in minutes", required: true, defaultValue: 10)
		}
        section("Frequency") {
			input (name: "interval", type: "enum", title: "Every", required: true, options: [[0: 'Hour'], [1: 'Day']], defaultValue: 0)
		}
        section("Offset: start X time after beginning of day/hour...") {
            input (name: "hour_offset", type: "number", title:"Hours", required: true, defaultValue: 0)
            input (name: "minute_offset", type: "number", title:"Minute", required: true, defaultValue: 0)
        }

        section("Log Settings...") {
			input (name: "logLevel", type: "enum", title: "Live Logging Level: Messages with this level and higher will be logged", options: [[0: 'Disabled'], [1: 'Error'], [2: 'Warning'], [3: 'Info'], [4: 'Debug'], [5: 'Trace']], defaultValue: 3)
			input "logDropLevelTime", "decimal", title: "Drop down to Info Level Minutes", required: true, defaultValue: 5
		}
	}
}


def installed() {
    
	// Set log level as soon as it's installed to start logging what we do ASAP
	int loggingLevel
	if (settings.logLevel) {
		loggingLevel = settings.logLevel.toInteger()
	} else {
		loggingLevel = 3
	}
	
	logger("trace", "Installed Running Ensure Minimum Switch Runtime Child: $app.label")
}


def updated() {
	// Set log level to new value
	int loggingLevel
	if (settings.logLevel) {
		state.loggingLevel = settings.logLevel.toInteger()
	} else {
		state.loggingLevel = 3
	}
    initialize()
	
	logger("trace", "Updated Running Ensure Minimum Switch Runtime Child: $app.label")
}


def uninstalled() {
}


//************************************************************
// initialize
//     Set preferences in the associated device and subscribe to the selected sensors and remote device
//     Also set logging preferences
//
// Signature(s)
//     initialize(child)
//
// Parameters
//     child : deviceWrapper
//
// Returns
//     None
//
//************************************************************
def initialize(child) {
	logger("trace", "Initialize Running Ensure Minimum Switch Runtime Child: $app.label")

	// First we need tu unsubscribe and unschedule any previous settings we had
	unsubscribe()
	unschedule()

	// Recheck Log level in case it was changed in the child app
	if (settings.logLevel) {
		loggingLevel = settings.logLevel.toInteger()
	} else {
		loggingLevel = 3
	}
	
	// Log level was set to a higher level than 3, drop level to 3 in x number of minutes
	if (loggingLevel > 3) {
		logger("trace", "Initialize runIn $settings.logDropLevelTime")
		runIn(settings.logDropLevelTime.toInteger() * 60, logsDropLevel)
	}
	logger("warn", "App logging level set to $loggingLevel")
	logger("info", "Initialize LogDropLevelTime: $settings.logDropLevelTime")
    state.last_start = null
    state.cumulative_time_s = 0
    logger("debug", "Subscribing to ${monitor_switch}")
    subscribe(monitor_switch, "switch", monitorSwitchHandler)
    logger("debug", "Interval: ${settings.interval}")
    
    // every hour
    if (settings.interval.toInteger() == 0) {
        schedule("0 ${settings.minute_offset} * ? * *", checkAndTurnOn)
        logger("info","Scheduling checkAndTurnOn every hour, at ${settings.minute_offset} minute(s) past the hour")
    }
    // every day
    if (settings.interval.toInteger() == 1) {
        schedule("0 ${settings.minute_offset} ${settings.hour_offset} ? * *", checkAndTurnOn)
        logger("info","Scheduling checkAndTurnOn every day, at ${settings.hour_offset}:${settings.minute_offset}")
    }
}

def checkAndTurnOn() {
    logger("info", "running checkAndTurnOn")
    cumulative_runtime_m = state.cumulative_time_s
    logger("debug", "cumulative runtime is ${cumulative_runtime_m} minutes")
    if (control_switch == null || control_switch.currentValue("switch") == "on") {
        missing_runtime = settings.minimum_runtime - cumulative_runtime_m 
        if (missing_runtime > 0) {
            logger("debug", "missing ${missing_runtime} minutes, turning ${monitor_switch} on for that duration")
            turnOnMonitorSwitch()
            runIn(missing_runtime * 60, turnOffMonitorSwitch)
            runIn(missing_runtime * 60 + 1, resetCumulativeTime)
        }
        else {
            logger("debug", "cumulative runtime is large enough, not turning on ${monitor_switch}")
        }
    }
    else {
        logger("debug", "control switch ${control_switch} is off, not starting ${monitor_switch}")
    }
    resetCumulativeTime()
}
def resetCumulativeTime() {
    logger("debug", "resetting cumulative runtime")
    state.cumulative_time_s = 0 
}
def turnOnMonitorSwitch() {
    logger("debug", "turning ${monitor_switch} on")
    monitor_switch.on()
}
def turnOffMonitorSwitch() {
    logger("debug", "turning ${monitor_switch} off")
    monitor_switch.off()
}
def monitorSwitchHandler(evt) {
    logger("debug", "monitorSwitchHandler called: ${evt.value} ${state.last_start}")
    if (evt.value == "on") {
        state.last_start = now()
        logger("debug", "Last start is now: ${state.last_start}")
    }
    else if (state.last_start != null) {
        state.cumulative_time_s += (now() - state.last_start)/1000
        logger("debug", "Cumulative time is now: ${state.cumulative_time_s} seconds")
    }
}


//************************************************************
// logger
//     Wrapper function for all logging with level control via preferences
//
// Signature(s)
//     logger(String level, String msg)
//
// Parameters
//     level : Error level string
//     msg : Message to log
//
// Returns
//     None
//
//************************************************************
def logger(level, msg) {
	switch(level) {
		case "error":
			if (state.loggingLevel >= 1) log.error msg
			break

		case "warn":
			if (state.loggingLevel >= 2) log.warn msg
			break

		case "info":
			if (state.loggingLevel >= 3) log.info msg
			break

		case "debug":
			if (state.loggingLevel >= 4) log.debug msg
			break

		case "trace":
			if (state.loggingLevel >= 5) log.trace msg
			break

		default:
			log.debug msg
			break
	}
}


//************************************************************
// logsDropLevel
//     Turn down logLevel to 3 in this app/device and log the change
//
// Signature(s)
//     logsDropLevel()
//
// Parameters
//     None
//
// Returns
//     None
//
//************************************************************
def logsDropLevel() {
	app.updateSetting("logLevel",[type:"enum", value:"3"])
	state.loggingLevel = app.getSetting('logLevel').toInteger()
	logger("warn","App logging level set to $loggingLevel")
}

