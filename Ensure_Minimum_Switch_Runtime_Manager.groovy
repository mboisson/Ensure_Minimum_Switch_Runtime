/*
 *  Ensure Minimum Switch Runtime Parent App
 *  Project URL: https://github.com/mboisson/Ensure_Minimum_Switch_Runtime
 *  Copyright 2024 Maxime Boissonneault
 *
 *  This app requires it's child app to function, please go to the project page for more information.
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

/*
Heavily inspired by Advanced vThermostat Manager by Nelson Clark.
*/

definition(
	name: "Ensure Minimum Switch Runtime Manager",
	namespace: "mboisson",
	author: "Maxime Boissonneault",
	description: "Ensures that a given switch runs at least for a minimum duration within a specific interval",
	category: "Green Living",
	iconUrl: "TODO",
	iconX2Url: "TODO",
	importUrl: "TODO",
	singleInstance: true
)

preferences {
	page(name: "Install", title: "Ensure Minimum Switch Runtime Manager", install: true, uninstall: true) {
		section("Devices") {
		}
		section {
			app(name: "ensure_minimum_switch_runtimes", appName: "Ensure Minimum Switch Runtime Child", namespace: "mboisson", title: "Add Ensure Minimum Switch Runtime", multiple: true)
        }
	}
}

def installed() {
	log.debug "Installed"
	initialize()
}

def updated() {
	log.debug "Updated"
	unsubscribe()
	initialize()
}

def initialize() {
	log.debug "Initializing; there are ${childApps.size()} child apps installed"
	childApps.each {child -> 
		log.debug "  child app: ${child.label}"
	}
}
