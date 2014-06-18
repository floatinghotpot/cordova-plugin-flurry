#import <Cordova/CDV.h>
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

#import "Flurry.h"
#import "FlurryAds.h"
#import "FlurryAdDelegate.h"

#pragma mark - JS requestAd options

#define PUBLISHER_ID_ARG_INDEX    0
#define AD_SIZE_ARG_INDEX         1
#define BANNER_AT_TOP_ARG_INDEX   2

#define IS_TESTING_ARG_INDEX      0
#define EXTRAS_ARG_INDEX          1

#define SHOW_AD_ARG_INDEX    0

@class GADBannerView;
@class GADInterstitial;

#pragma mark Flurry Plugin

@interface CDVFlurry : CDVPlugin <FlurryAdDelegate> {
}

@property (assign) BOOL bannerAtTop;
@property (assign) int adSize;
@property (assign) BOOL adShow;

- (void)createBannerView:(CDVInvokedUrlCommand *)command;
- (void)destroyBannerView:(CDVInvokedUrlCommand *)command;
- (void)requestAd:(CDVInvokedUrlCommand *)command;
- (void)showAd:(CDVInvokedUrlCommand *)command;

- (void)createInterstitialView:(CDVInvokedUrlCommand *)command;
- (void)requestInterstitialAd:(CDVInvokedUrlCommand *)command;

@end
