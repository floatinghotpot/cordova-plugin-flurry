# Cordova Plugin for Flurry Ads #

Present Flurry Ads in Mobile App/Games natively with single line of JavaScript. 

Highlights:
- [x] Easy-to-use APIs. Display Ad with single line of Js code.
- [x] Support Banner, Interstitial Ad, and Video Ad.
- [x] One plugin supports both Android and iOS platform.
- [x] Fixed and overlapped mode.
- [x] Auto fit on orientation change.
- [x] Same API with other RjFun Ad Plugins, easily to switch to other Ad.
- [x] Actively maintained, prompt support.

Compatible with:

* Cordova CLI, v3.5+
* Intel XDK and Crosswalk, r1095+
* IBM Worklight, v6.2+

## How to use? ##

If use with Cordova CLI:
```
cordova plugin add com.rjfun.cordova.flurryads
```

If use with Intel XDK:
Project -> CORDOVA 3.X HYBRID MOBILE APP SETTINGS -> PLUGINS AND PERMISSIONS -> Third-Party Plugins ->
Add a Third-Party Plugin -> Get Plugin from the Web, input:
```
Name: FlurryAdsPluginPro
Plugin ID: com.rjfun.cordova.flurryads
[x] Plugin is located in the Apache Cordova Plugins Registry
```

## Quick Start Example Code ##

Step 1: Prepare your Flurry Id for your app, create it in [Flurry Developers website](https://www.flurry.com/)

```javascript
var ad_units = {
	ios : { 
		banner:"2DYY249X5G798HMF3MTH",
		interstitial:"2DYY249X5G798HMF3MTH"
	},
	android : {
		banner:"G56KN4J49YT66CFRD5K6",
		interstitial:"G56KN4J49YT66CFRD5K6"
	}
};

// select the right Ad Id according to platform
var adid = (/(android)/i.test(navigator.userAgent)) ? ad_units.android : ad_units.ios;
```

Step 2: Create a banner with single line of javascript

```javascript
// it will display smart banner at top center, using the default options
if(FlurryAds) FlurryAds.createBanner( adid.banner );
```

Or, show the banner Ad in some other way:

```javascript
// or, show a default banner at bottom
if(FlurryAds) FlurryAds.createBanner( {
	adId: adid.banner, 
	position:FlurryAds.AD_POSITION.BOTTOM_CENTER, 
	autoShow:true} );
```

Step 3: Prepare an interstitial, and show it when needed

```javascript
// preppare and load ad resource in background, e.g. at begining of game level
if(FlurryAds) FlurryAds.prepareInterstitial( {adId:adid.interstitial, autoShow:false} );

// show the interstitial later, e.g. at end of game level
if(FlurryAds) FlurryAds.showInterstitial();
```

You can even embed the Ad into web content with Native Ad.

Check the example code [test/index.html] (https://github.com/floatinghotpot/cordova-plugin-flurry/blob/master/test/index.html)

## Javascript API Overview ##

Methods:
```javascript
// set default value for other methods
setOptions(options, success, fail);
// for banner
createBanner(adId/options, success, fail);
removeBanner();
showBanner(position);
showBannerAtXY(x, y);
hideBanner();
// for interstitial
prepareInterstitial(adId/options, success, fail);
showInterstitial();
// for native ad
createNativeAd(adId, success, fail);
removeNativeAd(adId);
setNativeAdClickArea(adId,x,y,w,h);
```

## Detailed Documentation ##

The APIs, Events and Options are detailed documented.

Read the detailed API Reference Documentation [English](https://github.com/floatinghotpot/cordova-plugin-flurry/wiki).

## FAQ ##

If encounter problem when using the plugin, please read the [FAQ](https://github.com/floatinghotpot/cordova-plugin-flurry/wiki/FAQ) first.

## Full Example Code ##

This FlurryAds Plugin Pro offers the most flexibility and many options.

Check the [test/index.html] (https://github.com/floatinghotpot/cordova-plugin-fllurry/blob/master/test/index.html).

## Screenshots ##

iPhone

![ScreenShot](docs/flurry-iphone.jpg)

iPad

![ScreenShot](docs/flurry-ipad.jpg)

Android

![ScreenShot](docs/flurry-android.jpg)

## Credits ##

This FlurryAds Plugin Pro is published in a win-win partnership model:
- It's FREE. 
- It's closed source.
- 2% Ad traffic will be shared, as return for the support and maintenance effort.
- You will get commercial-level support with high priority, prompt and professional.

If you hope to make the Ad 100% under your control and keep 100% Ad revenue, you can also consider spending $20 to [get a license key](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=C654HGZVC43T4) to remove the 2% Ad traffic sharing.
It's much cheaper than [the $50 iOS only AdMob plugin for unity](https://prime31.com/plugins). 

Then set the license key in options (either API setOptions, or createBanner, or prepareInterstitial):
```javascript
    license: 'username@gmail.com/xxxxxxxxxxxxxxx',
```

FlurryAds Plugin Pro is one of the best choice for HTML5/Cordova/PhoneGap/XDK/Construct2 app/games.

## See Also ##

Cordova/PhoneGap plugins for the world leading Mobile Ad services:

* [AdMob Plugin Pro](https://github.com/floatinghotpot/cordova-admob-pro), enhanced Google AdMob plugin, easy API and more features.
* [mMedia Plugin Pro](https://github.com/floatinghotpot/cordova-plugin-mmedia), enhanced mMedia plugin, support impressive video Ad.
* [iAd Plugin](https://github.com/floatinghotpot/cordova-plugin-iad), Apple iAd service. 
* [FlurryAds Plugin](https://github.com/floatinghotpot/cordova-plugin-flurry), Yahoo Flurry Ads service.
* [MobFox Plugin Pro](https://github.com/floatinghotpot/cordova-mobfox-pro), enhanced MobFox plugin, support video Ad and many other Ad network with server-side integration.
* [MoPub Plugin Pro](https://github.com/floatinghotpot/cordova-plugin-mopub), MobPub Ads service.
* [FacebookAds Plugin Pro](https://github.com/floatinghotpot/cordova-plugin-facebookads), Facebook Audience Network Ads service.

More Cordova/PhoneGap plugins by Raymond Xie, [click here](http://floatinghotpot.github.io/).

Project outsourcing and consulting service is also available. Please [contact us](http://floatinghotpot.github.io) if you have the business needs.

