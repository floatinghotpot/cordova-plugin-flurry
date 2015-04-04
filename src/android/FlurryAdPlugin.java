package com.rjfun.cordova.flurry;

import org.json.JSONObject;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import com.flurry.android.FlurryAgent;
import com.flurry.android.FlurryAds;

import com.flurry.android.ads.FlurryAdBanner;
import com.flurry.android.ads.FlurryAdBannerListener;
import com.flurry.android.ads.FlurryAdErrorType;
import com.flurry.android.ads.FlurryAdInterstitial;
import com.flurry.android.ads.FlurryAdInterstitialListener;
import com.rjfun.cordova.ad.GenericAdPlugin;

public class FlurryAdPlugin extends GenericAdPlugin {
    private static final String LOGTAG = "FlurryAdPlugin";

    private static final String AD_TOP_BANNER = "TOP_BANNER";
    private static final String AD_BOTTOM_BANNER = "BOTTOM_BANNER";
    private static final String AD_INTERSTITIAL = "INTERSTITIAL_MAIN_VIEW";
    
    private static final String TEST_APIKEY = "G56KN4J49YT66CFRD5K6";

    private boolean inited = false;
    private String adSpace = AD_BOTTOM_BANNER;

    @Override
    protected void pluginInitialize() {
    	super.pluginInitialize();
    	
    	// TODO: any init code
	}
    
    private void validateSession(String adId) {
    	if(! inited) {
    		FlurryAgent.init(getActivity(), adId);
    		FlurryAgent.onStartSession(getActivity());
    		inited = true;
    	}
    }
    
    @Override
    public void onDestroy() {
    	FlurryAgent.onEndSession(getActivity());
    	super.onDestroy();
    }

	@Override
	public void setOptions(JSONObject options) {
		super.setOptions(options);
		
		// for test only
		if(this.isTesting) {
			FlurryAds.enableTestAds( true );
			if(this.logVerbose) {
			    FlurryAgent.setLogEnabled(true);
			    FlurryAgent.setLogEvents(true);
			    FlurryAgent.setLogLevel(Log.VERBOSE);
			}
		}
		
		if(adPosition <= TOP_RIGHT) {
			adSpace = AD_TOP_BANNER;
		} else {
			adSpace = AD_BOTTOM_BANNER;
		}
	}

	@Override
	protected String __getProductShortName() {
		return "Flurry";
	}

	@Override
	protected String __getTestBannerId() {
		return TEST_APIKEY;
	}

	@Override
	protected String __getTestInterstitialId() {
		return TEST_APIKEY;
	}
	
	private FlurryAdBanner mFlurryAdBanner = null;
	private FlurryAdInterstitial mFlurryAdInterstitial = null;
	
	   /** Gets a string error reason from an error code. */
	   public String getErrorReason(FlurryAdErrorType t) {
	     String errorReason = "unknown";
	     switch(t) {
	     case FETCH:
	    	 errorReason = "error fetch ad";
	         break;
	     case RENDER:
	    	 errorReason = "error render ad";
	         break;
	     case CLICK:
	    	 errorReason = "error click ad";
	         break;
	     }
	     return errorReason;
	   }

	FlurryAdBannerListener bannerAdListener = new FlurryAdBannerListener() {

		@Override
		public void onAppExit(FlurryAdBanner arg0) {
	    	fireAdEvent(EVENT_AD_LEAVEAPP, ADTYPE_BANNER);
		}

		@Override
		public void onClicked(FlurryAdBanner arg0) {
	    	//fireAdEvent(EVENT_AD_LEAVEAPP, ADTYPE_BANNER);
		}

		@Override
		public void onCloseFullscreen(FlurryAdBanner arg0) {
        	fireAdEvent(EVENT_AD_DISMISS, ADTYPE_BANNER);
		}

		@Override
		public void onError(FlurryAdBanner arg0, FlurryAdErrorType adErrorType, int errorCode) {
        	fireAdErrorEvent(EVENT_AD_FAILLOAD, errorCode, getErrorReason(adErrorType), ADTYPE_BANNER);
		}

		@Override
		public void onFetched(FlurryAdBanner arg0) {
            if((! bannerVisible) && autoShowBanner) {
            	showBanner(adPosition, posX, posY);
            }
        	fireAdEvent(EVENT_AD_LOADED, ADTYPE_BANNER);
		}

		@Override
		public void onRendered(FlurryAdBanner arg0) {
        	fireAdEvent(EVENT_AD_PRESENT, ADTYPE_BANNER);
		}

		@Override
		public void onShowFullscreen(FlurryAdBanner arg0) {
        	fireAdEvent(EVENT_AD_PRESENT, ADTYPE_BANNER);
		}

		@Override
		public void onVideoCompleted(FlurryAdBanner arg0) {
        	fireAdEvent(EVENT_AD_DISMISS, ADTYPE_BANNER);
		}
		
	};

	@Override
	protected View __createAdView(String adId) {
		if(isTesting) adId = TEST_APIKEY;
		
		validateSession(adId);
		
		FrameLayout ad = new FrameLayout(getActivity());
    	FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    	ad.setLayoutParams(params);
    	
    	mFlurryAdBanner = new FlurryAdBanner(getActivity(), ad, adSpace);
    	mFlurryAdBanner.setListener(bannerAdListener);
    	
		return ad;
	}
	
	@Override
	protected int __getAdViewWidth(View view) {
		return view.getWidth();
	}

	@Override
	protected int __getAdViewHeight(View view) {
		return view.getHeight();
	}

	@Override
	protected void __loadAdView(View view) {
		mFlurryAdBanner.fetchAndDisplayAd();
	}

	@Override
	protected void __pauseAdView(View view) {
	}

	@Override
	protected void __resumeAdView(View view) {
	}

	@Override
	protected void __destroyAdView(View view) {
		mFlurryAdBanner.destroy();
	}

	FlurryAdInterstitialListener interstitialAdListener = new FlurryAdInterstitialListener() {

		@Override
		public void onAppExit(FlurryAdInterstitial arg0) {
	    	fireAdEvent(EVENT_AD_LEAVEAPP, ADTYPE_INTERSTITIAL);
		}

		@Override
		public void onClicked(FlurryAdInterstitial arg0) {
	    	//fireAdEvent(EVENT_AD_LEAVEAPP, ADTYPE_INTERSTITIAL);
		}

		@Override
		public void onClose(FlurryAdInterstitial arg0) {
        	fireAdEvent(EVENT_AD_DISMISS, ADTYPE_INTERSTITIAL);

        	removeInterstitial();
		}

		@Override
		public void onDisplay(FlurryAdInterstitial arg0) {
        	fireAdEvent(EVENT_AD_PRESENT, ADTYPE_INTERSTITIAL);
		}

		@Override
		public void onError(FlurryAdInterstitial arg0, FlurryAdErrorType adErrorType, int errorCode) {
        	fireAdErrorEvent(EVENT_AD_FAILLOAD, errorCode, getErrorReason(adErrorType), ADTYPE_INTERSTITIAL);
		}

		@Override
		public void onFetched(FlurryAdInterstitial arg0) {
            if(autoShowInterstitial) {
            	showInterstitial();
            }
        	fireAdEvent(EVENT_AD_LOADED, ADTYPE_INTERSTITIAL);
		}

		@Override
		public void onRendered(FlurryAdInterstitial arg0) {
        	fireAdEvent(EVENT_AD_PRESENT, ADTYPE_INTERSTITIAL);
		}

		@Override
		public void onVideoCompleted(FlurryAdInterstitial arg0) {
	    	fireAdEvent(EVENT_AD_DISMISS, ADTYPE_INTERSTITIAL);
		}
		
	};
	
	@Override
	protected Object __createInterstitial(String adId) {
		if(isTesting) adId = TEST_APIKEY;
		
		validateSession(adId);
		
		mFlurryAdInterstitial = new FlurryAdInterstitial(getActivity(), AD_INTERSTITIAL);
		mFlurryAdInterstitial.setListener( interstitialAdListener );

		return mFlurryAdInterstitial;
	}

	@Override
	protected void __loadInterstitial(Object interstitial) {
		FlurryAdInterstitial ad = (FlurryAdInterstitial) interstitial;
		ad.fetchAd();
	}

	@Override
	protected void __showInterstitial(Object interstitial) {
		FlurryAdInterstitial ad = (FlurryAdInterstitial) interstitial;
		ad.displayAd();
	}

	@Override
	protected void __destroyInterstitial(Object interstitial) {
		FlurryAdInterstitial ad = (FlurryAdInterstitial) interstitial;
		ad.destroy();
	}

}
