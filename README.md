# TiLocationServices

### Turn on GPS automatically and get your current location

## install
- Get the module from the [releases page](https://github.com/AhmedMSayed/TiLocationServices/releases);
- Add the content to the `modules` folder of your project;
- Add `<module platform="android">com.locationservices</module>` under the `<modules>` tag of your `tiapp.xml` file.

## Usage
Require the module wherever you see fit (generally, in the `alloy.js` or `index.js` of your app) and set the callback function:

```js
//Import Location Services Module
const locationservices = require('com.locationservices');

const win = Ti.UI.createWindow({
	backgroundColor : 'white',
	layout : 'vertical'
});

var myLatitude = Ti.UI.createLabel({
	text : "My Latitude is : ",
	color : "#000",
	font : {
		fontSize : '16sp'
	},
	top : 72
});

var myLongitude = Ti.UI.createLabel({
	text : "My Longitude is : ",
	color : "#000",
	font : {
		fontSize : '16sp'
	},
	top : 24
});

win.add(myLatitude);
win.add(myLongitude);

win.addEventListener('open', function() {
	const locationResults = function(loc) {
		Ti.API.info("latitude : " + loc.latitude + ", longitude : " + loc.longitude);
		myLatitude.text = "My Latitude is : " + loc.latitude;
		myLongitude.text = "My Longitude is : " + loc.longitude;
		Alloy.Globals.locationServices.removeEventListener('location', locationResults);
	};

	//YOU MUST check Permissions before initialize();
	if (Ti.Geolocation.hasLocationPermissions(Ti.Geolocation.AUTHORIZATION_WHEN_IN_USE)) {
		locationservices.initialize();
		locationservices.addEventListener('location', locationResults);
	} else {
		Ti.Geolocation.requestLocationPermissions(Ti.Geolocation.AUTHORIZATION_WHEN_IN_USE, function(e) {
			if (e.success) {
				locationservices.initialize();
				locationservices.addEventListener('location', locationResults);
			} else {
				//Alert user to allow permissions from setting
			}
		});
	}
});

win.open();
```
## License

MIT
