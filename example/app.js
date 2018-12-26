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
		myLatitude = "My Latitude is : " + loc.latitude;
		myLongitude = "My Longitude is : " + loc.longitude;
		Alloy.Globals.locationServices.removeEventListener('location', locationResults);
	};

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

