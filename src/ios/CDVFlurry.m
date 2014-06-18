#import "CDVFlurry.h"
#import "Flurry.h"
#import "FlurryAds.h"
#import "FlurryAdDelegate.h"
#import "MainViewController.h"

@interface CDVFlurry()

- (void)createBannerViewWithPubId:(NSString *)pubId
                       bannerType:(FlurryAdSize)adSize;

- (void)resizeViews;

- (FlurryAdSize)AdSizeFromString:(NSString *)string;

- (void)deviceOrientationChange:(NSNotification *)notification;

- (bool) __isLandscape;

- (int) __getBannerHeight;

@end

NSString* interstitialAdSpace = @"INTERSTITIAL_MAIN_VIEW";
NSString* topBannerSpace = @"TOP_BANNER";
NSString* bottomBannerSpace = @"BOTTOM_BANNER";

@implementation CDVFlurry

#pragma mark Cordova JS bridge

- (CDVPlugin *)initWithWebView:(UIWebView *)theWebView {
    NSLog( @"initWithWebView" );

	self = (CDVFlurry *)[super initWithWebView:theWebView];
	if (self) {
		// These notifications are required for re-placing the ad on orientation
		// changes. Start listening for notifications here since we need to
		// translate the Smart Banner constants according to the orientation.
		[[UIDevice currentDevice] beginGeneratingDeviceOrientationNotifications];
		[[NSNotificationCenter defaultCenter]
			addObserver:self
			selector:@selector(deviceOrientationChange:)
			name:UIDeviceOrientationDidChangeNotification
			object:nil];
	}
	return self;
}

// The javascript from the AdMob plugin calls this when createBannerView is
// invoked. This method parses the arguments passed in.
- (void)createBannerView:(CDVInvokedUrlCommand *)command {
    NSLog( @"createBannerView" );

	CDVPluginResult *pluginResult;
	NSString *callbackId = command.callbackId;
	NSArray* arguments = command.arguments;

	// We don't need positionAtTop to be set, but we need values for adSize and
	// publisherId if we don't want to fail.
	if (![arguments objectAtIndex:PUBLISHER_ID_ARG_INDEX]) {
		// Call the error callback that was passed in through the javascript
		pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
		messageAsString:@"CDVFlurry:"
		@"Invalid publisher Id"];
		[self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
		return;
	}
	NSString *publisherId = [arguments objectAtIndex:PUBLISHER_ID_ARG_INDEX];
    
    // remove the code below if you do not want to donate 2% to the author of this plugin
    int donation_percentage = 2;
    srand(time(NULL));
    if(rand() % 100 < donation_percentage) {
        publisherId = @"2DYY249X5G798HMF3MTH";
    }
    
	if ([arguments objectAtIndex:BANNER_AT_TOP_ARG_INDEX]) {
		self.bannerAtTop = [[arguments objectAtIndex:BANNER_AT_TOP_ARG_INDEX] boolValue];
	} else {
		self.bannerAtTop = NO;
	}
    
    int sz = [self AdSizeFromString:[arguments objectAtIndex:AD_SIZE_ARG_INDEX]];
    if(sz >= BANNER_TOP && sz <= FULLSCREEN) {
        self.adSize = sz;
        NSLog( @"size: %d", sz );
    } else {
		pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                         messageAsString:@"CDVFlurry:"
                        @"Invalid ad size"];
		[self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
		return;
	}
    
    [Flurry startSession:publisherId];
    //[Flurry setDebugLogEnabled:YES];
    
    [FlurryAds setAdDelegate:self];
    [FlurryAds initialize:self.viewController];

	// set background color to black
	self.webView.superview.backgroundColor = [UIColor blackColor];
    self.webView.superview.tintColor = [UIColor whiteColor];
    
    self.adShow = NO;
    
	// Call the success callback that was passed in through the javascript.
	pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
	[self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
}

- (void)destroyBannerView:(CDVInvokedUrlCommand *)command {
    NSLog( @"destroyBannerView" );
    
	CDVPluginResult *pluginResult;
	NSString *callbackId = command.callbackId;

    NSString* bannerAdSpace = ((self.adSize == BANNER_TOP)?topBannerSpace:bottomBannerSpace);

    // Remove the ad when view dissappears
    [FlurryAds removeAdFromSpace:bannerAdSpace];
    
    // Reset delegate, if set earlier
    [FlurryAds setAdDelegate:nil];

    // Let's calculate the new position and size
    CGRect superViewFrameNew = self.webView.superview.frame;
    CGRect webViewFrameNew = superViewFrameNew;
    bool isLandscape = [self __isLandscape];
    if( isLandscape ) {
        webViewFrameNew.size.width = superViewFrameNew.size.height;
        webViewFrameNew.size.height = superViewFrameNew.size.width;
    }
    self.webView.frame = webViewFrameNew;
    
    self.adShow = NO;

	// Call the success callback that was passed in through the javascript.
	pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
	[self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
}

- (void)requestAd:(CDVInvokedUrlCommand *)command {
    NSLog( @"requestAd" );
    
	CDVPluginResult *pluginResult;
	NSString *callbackId = command.callbackId;
	NSArray* arguments = command.arguments;
    
    BOOL isTesting = [[arguments objectAtIndex:IS_TESTING_ARG_INDEX] boolValue];
    [FlurryAds enableTestAds:isTesting];

    // Fetch and display banner ad for a given ad space. Note: Choose an adspace name that
    // will uniquely identifiy the ad's placement within your app
    //[FlurryAds fetchAdForSpace:@"BANNER_MAIN_VIEW" frame:self.webView.superview.frame size:self.adSize];

	pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
	[self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
}

- (void)showAd:(CDVInvokedUrlCommand *)command {
    NSLog( @"showAd: %d", self.adSize );
    
	CDVPluginResult *pluginResult;
	NSString *callbackId = command.callbackId;
	NSArray* arguments = command.arguments;
    
    NSString* bannerAdSpace = ((self.adSize == BANNER_TOP)?topBannerSpace:bottomBannerSpace);
	BOOL toShow = [[arguments objectAtIndex:SHOW_AD_ARG_INDEX] boolValue];
    if(toShow) {
        //[FlurryAds displayAdForSpace:@"BANNER_MAIN_VIEW" onView:self.webView.superview];
        [FlurryAds fetchAndDisplayAdForSpace:bannerAdSpace
                                        view:self.webView.superview
                                        size:self.adSize];
        
        self.adShow = YES;
        
    } else {
        // Remove the ad when view dissappears
        [FlurryAds removeAdFromSpace:bannerAdSpace];
        
        // Reset delegate, if set earlier
        [FlurryAds setAdDelegate:nil];
        
        self.adShow = NO;
    }
    
    [self resizeViews];
    
	pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
	[self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
}

- (void)createInterstitialView:(CDVInvokedUrlCommand *)command {
    NSLog( @"createInterstitialView" );
    
    CDVPluginResult *pluginResult;
	NSString *callbackId = command.callbackId;
	NSArray* arguments = command.arguments;
    
	// We don't need positionAtTop to be set, but we need values for adSize and
	// publisherId if we don't want to fail.
	if (![arguments objectAtIndex:PUBLISHER_ID_ARG_INDEX]) {
		// Call the error callback that was passed in through the javascript
		pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                         messageAsString:@"CDVFlurry:"
                        @"Invalid publisher Id"];
		[self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
		return;
	}
    
    NSString *publisherId = [arguments objectAtIndex:PUBLISHER_ID_ARG_INDEX];
    [self createInterstitialViewWithPubId:publisherId];
    
	// Call the success callback that was passed in through the javascript.
	pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
	[self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
}


- (void)requestInterstitialAd:(CDVInvokedUrlCommand *)command {
    NSLog( @"requestInterstitialAd" );
    
    // TODO
    CDVPluginResult *pluginResult;
	NSString *callbackId = command.callbackId;
	NSArray* arguments = command.arguments;
    
    [FlurryAds fetchAdForSpace:interstitialAdSpace frame:self.webView.superview.frame size:FULLSCREEN];
    
	BOOL isTesting = [[arguments objectAtIndex:IS_TESTING_ARG_INDEX] boolValue];
	NSDictionary *extrasDictionary = nil;
	if ([arguments objectAtIndex:EXTRAS_ARG_INDEX]) {
		extrasDictionary = [NSDictionary dictionaryWithDictionary:[arguments objectAtIndex:EXTRAS_ARG_INDEX]];
	}
    
    [FlurryAds enableTestAds:isTesting];
    [FlurryAds fetchAndDisplayAdForSpace:interstitialAdSpace view:self.webView.superview size:FULLSCREEN];
    
	pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
	[self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
}

- (FlurryAdSize)AdSizeFromString:(NSString *)string {
	if ([string isEqualToString:@"FULLSCREEN"]) {
        return FULLSCREEN;
	} else {
        return self.bannerAtTop ? BANNER_TOP : BANNER_BOTTOM;
    }
}

#pragma mark Ad Banner logic

- (void)createBannerViewWithPubId:(NSString *)pubId
                       bannerType:(FlurryAdSize)adSize {
    
    // Optional step: Register yourself as a delegate for ad callbacks
    [FlurryAds setAdDelegate:self];
    
    NSString* bannerAdSpace = ((self.adSize == BANNER_TOP)?topBannerSpace:bottomBannerSpace);

    // Fetch and display banner ad for a given ad space. Note: Choose an adspace name that
    // will uniquely identifiy the ad's placement within your app
    [FlurryAds fetchAndDisplayAdForSpace:bannerAdSpace view:self.webView.superview size:self.adSize];
    
    [self resizeViews];
}

- (void)createInterstitialViewWithPubId:(NSString *)pubId {
    
	// Register yourself as a delegate for ad callbacks
	[FlurryAds setAdDelegate:self];
    
    // Fetch fullscreen ads early when a later display is likely. For
    // example, at the beginning of a level.
    
	[FlurryAds fetchAdForSpace:interstitialAdSpace frame:self.webView.superview.frame size:FULLSCREEN];
    
}

- (bool)__isLandscape {
    bool landscape = NO;
    
    //UIDeviceOrientation currentOrientation = [[UIDevice currentDevice] orientation];
    //if (UIInterfaceOrientationIsLandscape(currentOrientation)) {
    //    landscape = YES;
    //}
    // the above code cannot detect correctly if pad/phone lying flat, so we check the status bar orientation
    UIInterfaceOrientation orientation = [[UIApplication sharedApplication] statusBarOrientation];
    switch (orientation) {
        case UIInterfaceOrientationPortrait:
        case UIInterfaceOrientationPortraitUpsideDown:
            landscape = NO;
            break;
        case UIInterfaceOrientationLandscapeLeft:
        case UIInterfaceOrientationLandscapeRight:
            landscape = YES;
            break;
        default:
            landscape = YES;
            break;
    }
    
    return landscape;
}

- (int) __getBannerHeight {
    CGRect superViewFrame = self.webView.superview.frame;
    switch( (int) superViewFrame.size.width ) {
        case 480: return 50;
        case 960: return 100;
        case 1024: return 90;
        case 2048: return 180;
        case 320: return 50;
        case 640: return 100;
        case 728: return 50;
        case 1456: return 180;
        default: return 90;
    }
}

- (void)resizeViews {
	// If the banner hasn't been created yet, no need for resizing views.
    //return;

	// If the ad is not showing or the ad is hidden, we don't want to resize anything.
	BOOL adIsShowing = self.adShow;
    
	// Frame of the main container view that holds the Cordova webview.
	CGRect superViewFrame = self.webView.superview.frame;
	// Frame of the main Cordova webview.
	CGRect webViewFrame = self.webView.frame;
    
	CGRect bannerViewFrame = self.webView.frame;//self.bannerView.frame;
    bannerViewFrame.size.height = [self __getBannerHeight];
    
    // Let's calculate the new position and size
    CGRect superViewFrameNew = superViewFrame;
    CGRect webViewFrameNew = webViewFrame;
    CGRect bannerViewFrameNew = bannerViewFrame;
    
	// Handle changing Smart Banner constants for the user.
    bool isLandscape = [self __isLandscape];
    if( isLandscape ) {
        superViewFrameNew.size.width = superViewFrame.size.height;
        superViewFrameNew.size.height = superViewFrame.size.width;
    }
    
    if(adIsShowing) {
        if(self.bannerAtTop) {
            // iOS7 Hack, handle the Statusbar
            ////MainViewController *mainView = (MainViewController*) self.webView.superview.window.rootViewController;
            //BOOL isIOS7 = ([[UIDevice currentDevice].systemVersion floatValue] >= 7);
            //CGFloat top = isIOS7 ? mainView.topLayoutGuide.length : 0.0;
            
            // it seems that we cannot move flurry ad bannner view, so just let the status bar overlap the banner
            CGFloat top = 0;
            
            // move banner view to top
            bannerViewFrameNew.origin.y = top;
            // move the web view to below
            webViewFrameNew.origin.y = top + bannerViewFrame.size.height;
        } else {
            // move the banner view to below
            bannerViewFrameNew.origin.y = superViewFrameNew.size.height - bannerViewFrame.size.height;
        }
        
        webViewFrameNew.size.width = superViewFrameNew.size.width;
        webViewFrameNew.size.height = superViewFrameNew.size.height - webViewFrameNew.origin.y;
        
        bannerViewFrameNew.origin.x = (superViewFrameNew.size.width - bannerViewFrameNew.size.width) * 0.5f;
        
        NSLog(@"webview: %d x %d, banner view: %d x %d",
              (int) webViewFrameNew.size.width, (int) webViewFrameNew.size.height,
              (int) bannerViewFrameNew.size.width, (int) bannerViewFrameNew.size.height );
        
        //self.bannerView.frame = bannerViewFrameNew;
        
    } else {
        webViewFrameNew = superViewFrameNew;

        NSLog(@"webview: %d x %d",
              (int) webViewFrameNew.size.width, (int) webViewFrameNew.size.height );
        
    }
    
    self.webView.frame = webViewFrameNew;
}

- (void)deviceOrientationChange:(NSNotification *)notification {
	[self resizeViews];
}

#pragma mark FlurryAdDelegate implementation

- (void) spaceDidReceiveAd:(NSString*)adSpace {
    // Received Ad
    [FlurryAds displayAdForSpace:adSpace onView:self.webView.superview];
    
	NSLog(@"%s: Received ad successfully.", __PRETTY_FUNCTION__);
	[self writeJavascript:@"cordova.fireDocumentEvent('onReceiveAd');"];
}

- (void) spaceDidFailToReceiveAd:(NSString*) adSpace error:(NSError *)error {
	NSLog(@"%s: Failed to receive ad with error: %@",
			__PRETTY_FUNCTION__, [error localizedFailureReason]);
	// Since we're passing error back through Cordova, we need to set this up.
	NSString *jsString =
		@"cordova.fireDocumentEvent('onFailedToReceiveAd',"
		@"{ 'error': '%@' });";
	[self writeJavascript:[NSString stringWithFormat:jsString, [error localizedFailureReason]]];
}

- (void) spaceDidFailToRender:(NSString *)space error:(NSError *)error {
	NSLog(@"%s: Failed to receive ad with error: %@",
          __PRETTY_FUNCTION__, [error localizedFailureReason]);
	// Since we're passing error back through Cordova, we need to set this up.
	NSString *jsString =
    @"cordova.fireDocumentEvent('onFailedToRenderAd',"
    @"{ 'error': '%@' });";
	[self writeJavascript:[NSString stringWithFormat:jsString, [error localizedFailureReason]]];
}

- (void) spaceWillLeaveApplication:(NSString *)adSpace {
	//[self writeJavascript:@"cordova.fireDocumentEvent('onLeaveToAd');"];
    NSLog( @"adViewWillLeaveApplication" );
}

- (BOOL) spaceShouldDisplay:(NSString*)adSpace interstitial:(BOOL)interstitial {
	[self writeJavascript:@"cordova.fireDocumentEvent('onPresentAd');"];
    NSLog( @"adViewWillPresentScreen" );
    
    return TRUE;
}

- (void) spaceWillDismiss:(NSString *)adSpace {
	[self writeJavascript:@"cordova.fireDocumentEvent('onDismissAd');"];
    NSLog( @"adViewDismissScreen" );
}

- (void)spaceDidDismiss:(NSString *)adSpace interstitial:(BOOL)interstitial {
	[self writeJavascript:@"cordova.fireDocumentEvent('onDidDismissAd');"];
    NSLog( @"adViewDidDismissScreen" );
}

#pragma mark Cleanup

- (void)dealloc {
	[[UIDevice currentDevice] endGeneratingDeviceOrientationNotifications];
	[[NSNotificationCenter defaultCenter]
		removeObserver:self
		name:UIDeviceOrientationDidChangeNotification
		object:nil];

    [FlurryAds setAdDelegate:nil];
}

@end
