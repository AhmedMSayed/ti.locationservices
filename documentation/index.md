# Titanium Location Services Module for Andorid

Turn on GPS settings automatically and get your current or last location.

# Install

1. Get the module from the [releases page](https://github.com/AhmedMSayed/TiLocationServices/releases).
2. Unzip it, put it in your Titanium project `modules` folder.
3. Require the module in your application by adding it in your `tiapp.xml`.

```
<modules>
  <module version="2.0.0">ti.locationservices</module>
</modules>
```

# Example

```js
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

view.add(myLocation);
view.add(getMyCurrentLocation);
view.add(getMyLastLocation);
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
```

# Methods

## checkLocationSettings()

This method makes it easy for an app to ensure that the device's system settings are properly configured for the app's location needs. It will invoke a dialog that allows the user to enable the necessary location settings with a single tap.

```JS
locationservices.checkLocationSettings({
	onComplete: (check) => {
		if (!check.success) {
			console.error(check.message);
		}
	}
});
```

## getCurrentLocation()

Returns a single current location fix on the device.

```JS
locationservices.getCurrentLocation({
	onComplete: (location) => {
		if (location.success) {
			console.info(location)
		} else {
			console.error(location.message);
		}
	}
});
```

## getLastLocation()

Returns a single recent cached location currently available on the device.
> If a location is not available, which should happen very rarely, null will be returned

```JS
locationservices.getLastLocation({
	onComplete: (location) => {
		if (location.success) {
			console.info(location);
		} else {
			console.error(location.message);
		}
	}
});
```

## startLocationUpdates({ interval, fastestInterval, priority })

Requests location updates.
> This method will keep the Google Play services connection active, so make sure to call stopLocationUpdates when you no longer need it.

- At first, you want to register a `location` event listener to get the location updates.

```JS
locationservices.addEventListener('location', (location) => {
	console.info(location);
});
```

- Then you can start Location Updates 

```JS
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
```

- **interval** : Set the desired interval for active location updates, in milliseconds.

- **fastestInterval** : Explicitly set the fastest interval for location updates, in milliseconds.

- **priority** : Set the priority of the request.

## stopLocationUpdates()

Removes location updates if you no longer need updates.

```JS
locationservices.stopLocationUpdates({
	onComplete: (e) => {
		if (e.success) {
			console.info("stopLocationUpdates ::", "DONE");
		} else {
			console.info("stopLocationUpdates ::", e.message);
		}
	}
});
```

# Constant Summary

|             Constant             |                                                     Usage                                                      |
| :------------------------------: | :------------------------------------------------------------------------------------------------------------: |
| PRIORITY_BALANCED_POWER_ACCURACY |                      Used with `startLocationUpdates` to request "block" level accuracy.                       |
|      PRIORITY_HIGH_ACCURACY      |               Used with `startLocationUpdates` to request the most accurate locations available.               |
|        PRIORITY_LOW_POWER        |                       Used with `startLocationUpdates` to request "city" level accuracy.                       |
|        PRIORITY_NO_POWER         | Used with `startLocationUpdates` to request the best accuracy possible with zero additional power consumption. |


## License
MIT

## Author
Ahmed Eissa
