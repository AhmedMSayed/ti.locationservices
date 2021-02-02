const win = Ti.UI.createWindow({
	backgroundColor: 'white'
});

const view = Ti.UI.createView({
	layout: 'vertical',
	height: Ti.UI.SIZE,
	width: Ti.UI.FILL
});

const myLocation = Ti.UI.createLabel({
	text: "My Location",
	color: "#000",
	font: {
		fontSize: '16sp'
	}
});

const getMyCurrentLocation = Ti.UI.createButton({
	title: "Get Current Location",
	color: "#fff",
	backgroundColor: "teal",
	font: {
		fontSize: '16sp'
	},
	top: 24
});

const getMyLastLocation = Ti.UI.createButton({
	title: "Get Last Location",
	color: "#fff",
	backgroundColor: "teal",
	font: {
		fontSize: '16sp'
	},
	top: 16
});

const startLocationUpdates = Ti.UI.createButton({
	title: "start Location Updates",
	color: "#fff",
	backgroundColor: "teal",
	font: {
		fontSize: '16sp'
	},
	top: 16
});

const stopLocationUpdates = Ti.UI.createButton({
	title: "stop Location Updates",
	color: "#fff",
	backgroundColor: "teal",
	font: {
		fontSize: '16sp'
	},
	top: 16
});

view.add(myLocation);
view.add(getMyCurrentLocation);
view.add(getMyLastLocation);
view.add(startLocationUpdates);
view.add(stopLocationUpdates);
win.add(view);

//Import Location Services Module
const locationservices = require('ti.locationservices');

getMyCurrentLocation.addEventListener("click", () => {
	locationservices.checkLocationSettings({
		onComplete: (check) => {
			if (check.success) {
				locationservices.getCurrentLocation({
					onComplete: (location) => {
						if (location.success) {
							myLocation.text = `Lat : ${location.coords.latitude}\nLong : ${location.coords.longitude}`;
						} else {
							console.error(location.message);
						}
					}
				});
			} else {
				console.error(check.message);
			}
		}
	});
});

getMyLastLocation.addEventListener("click", () => {
	locationservices.getLastLocation({
		onComplete: (location) => {
			if (location.success) {
				myLocation.text = `Lat : ${location.coords.latitude}\nLong : ${location.coords.longitude}`;
			} else {
				console.error(location.message);
			}
		}
	});
});

locationservices.addEventListener('location', (location) => {
	myLocation.text = `Lat : ${location.coords.latitude}\nLong : ${location.coords.longitude}`;
});

startLocationUpdates.addEventListener("click", () => {
	locationservices.startLocationUpdates({
		interval: 10000,
		fastestInterval: 5000,
		priority: locationservices.PRIORITY_HIGH_ACCURACY,
		onComplete: (e) => {
			if (e.success) {
				console.info("startLocationUpdates ::", "DONE");
			} else {
				console.info("startLocationUpdates ::", e.message);
			}
		}
	});
});

stopLocationUpdates.addEventListener("click", () => {
	locationservices.stopLocationUpdates({
		onComplete: (e) => {
			if (e.success) {
				console.info("stopLocationUpdates ::", "DONE");
			} else {
				console.info("stopLocationUpdates ::", e.message);
			}
		}
	});
});

win.addEventListener('open', () => {
	if (!Ti.Geolocation.hasLocationPermissions(Ti.Geolocation.AUTHORIZATION_WHEN_IN_USE)) {
		Ti.Geolocation.requestLocationPermissions(Ti.Geolocation.AUTHORIZATION_WHEN_IN_USE, (e) => {
			if (!e.success) {
				console.warn("Permissions is denied");
			}
		});
	}
});

win.open();