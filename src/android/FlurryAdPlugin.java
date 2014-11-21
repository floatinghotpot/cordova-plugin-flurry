package com.rjfun.cordova.flurry;

import org.json.JSONObject;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import com.flurry.android.FlurryAdType;
import com.flurry.android.FlurryAds;
import com.flurry.android.FlurryAdSize;
import com.flurry.android.FlurryAgent;
import com.flurry.android.FlurryAdListener;
import com.rjfun.cordova.ad.GenericAdPlugin;

public class FlurryAdPlugin extends GenericAdPlugin implements FlurryAdListener {
    private static final String LOGTAG = "FlurryAdPlugin";

    private static final String AD_TOP_BANNER = "TOP_BANNER";
    private static final String AD_BOTTOM_BANNER = "BOTTOM_BANNER";
    private static final String AD_INTERSTITIAL = "INTERSTITIAL_MAIN_VIEW";
    
    private static final String TEST_APIKEY = "G56KN4J49YT66CFRD5K6";

    private boolean inited = false;
    private FlurryAdSize adSize = FlurryAdSize.BANNER_BOTTOM;
    private String adSpace = AD_BOTTOM_BANNER;

    @Override
    protected void pluginInitialize() {
    	super.pluginInitialize();
    	
    	// TODO: any init code
	}
    
    private void validateSession(String adId) {
    	if(! inited) {
    		FlurryAds.setAdListener( this );
    		FlurryAgent.onStartSession(getActivity(), adId);
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
			adSize = FlurryAdSize.BANNER_TOP;
			adSpace = AD_TOP_BANNER;
		} else {
			adSize = FlurryAdSize.BANNER_BOTTOM;
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

	@Override
	protected View __createAdView(String adId) {
		if(isTesting) adId = TEST_APIKEY;
		
		validateSession(adId);
		
		FrameLayout ad = new FrameLayout(getActivity());
    	FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    	ad.setLayoutParams(params);
    	
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
		FrameLayout ad = (FrameLayout) view;
    	FlurryAds.fetchAd(getActivity(), adSpace, ad, adSize);
	}

	@Override
	protected void __pauseAdView(View view) {
	}

	@Override
	protected void __resumeAdView(View view) {
	}

	@Override
	protected void __destroyAdView(View view) {
		FrameLayout ad = (FrameLayout) view;
		FlurryAds.removeAd(getActivity(), adSpace, ad);
	}

	@Override
	protected Object __createInterstitial(String adId) {
		if(isTesting) adId = TEST_APIKEY;
		
		validateSession(adId);
		FrameLayout ad = new FrameLayout(getActivity());
    	FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
    			LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    	ad.setLayoutParams(params);
		return ad;
	}

	@Override
	protected void __loadInterstitial(Object interstitial) {
		FrameLayout ad = (FrameLayout) interstitial;
		FlurryAds.fetchAd(getActivity(), AD_INTERSTITIAL, ad, FlurryAdSize.FULLSCREEN);	
	}

	@Override
	protected void __showInterstitial(Object interstitial) {
		FrameLayout ad = (FrameLayout) interstitial;
    	FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		getActivity().addContentView(ad, params);
	}

	@Override
	protected void __destroyInterstitial(Object interstitial) {
		FrameLayout ad = (FrameLayout) interstitial;
		FlurryAds.removeAd(getActivity(), AD_INTERSTITIAL, ad);
		ViewGroup parentView = (ViewGroup) ad.getParent();
		if(parentView != null) {
			parentView.removeView(ad);
		}
	}

    /**
     * document.addEventListener('onAdLoaded', function(data));
     * document.addEventListener('onAdFailLoad', function(data));
     * document.addEventListener('onAdPresent', function(data));
     * document.addEventListener('onAdDismiss', function(data));
     * document.addEventListener('onAdLeaveApp', function(data));
     */
	@Override
	public void onAdClicked(String space) {
    	fireAdEvent(EVENT_AD_LEAVEAPP, AD_INTERSTITIAL.equals(space) ? ADTYPE_INTERSTITIAL : ADTYPE_BANNER);
	}

	@Override
	public void onAdClosed(String space) {
    	fireAdEvent(EVENT_AD_DISMISS, AD_INTERSTITIAL.equals(space) ? ADTYPE_INTERSTITIAL : ADTYPE_BANNER);
	}

	@Override
	public void onAdOpened(String space) {
    	fireAdEvent(EVENT_AD_PRESENT, AD_INTERSTITIAL.equals(space) ? ADTYPE_INTERSTITIAL : ADTYPE_BANNER);
	}

	@Override
	public void onApplicationExit(String space) {
    	fireAdEvent(EVENT_AD_DISMISS, AD_INTERSTITIAL.equals(space) ? ADTYPE_INTERSTITIAL : ADTYPE_BANNER);
	}

	@Override
	public void onRenderFailed(String space) {
    	fireAdEvent(EVENT_AD_FAILLOAD, AD_INTERSTITIAL.equals(space) ? ADTYPE_INTERSTITIAL : ADTYPE_BANNER);
	}

	@Override
	public void onRendered(String space) {
    	fireAdEvent(EVENT_AD_PRESENT, AD_INTERSTITIAL.equals(space) ? ADTYPE_INTERSTITIAL : ADTYPE_BANNER);
	}

	@Override
	public void onVideoCompleted(String space) {
    	fireAdEvent(EVENT_AD_DISMISS, AD_INTERSTITIAL.equals(space) ? ADTYPE_INTERSTITIAL : ADTYPE_BANNER);
	}

	@Override
	public boolean shouldDisplayAd(String space, FlurryAdType arg1) {
		return true;
	}

	@Override
	public void spaceDidFailToReceiveAd(String space) {
    	fireAdErrorEvent(EVENT_AD_FAILLOAD, -1, "Failed to receive Ad", AD_INTERSTITIAL.equals(space) ? ADTYPE_INTERSTITIAL : ADTYPE_BANNER);
	}

	@Override
	public void spaceDidReceiveAd(String space) {
		if(AD_INTERSTITIAL.equals(space)) {
			FlurryAds.displayAd(getActivity(), space, (ViewGroup) this.interstitialAd);
		} else {
			FlurryAds.displayAd(getActivity(), space, (ViewGroup) this.adView);
		}
    	fireAdEvent(EVENT_AD_LOADED, AD_INTERSTITIAL.equals(space) ? ADTYPE_INTERSTITIAL : ADTYPE_BANNER);
	}

}
