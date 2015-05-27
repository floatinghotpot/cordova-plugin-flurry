package com.rjfun.cordova.flurry;

import java.util.HashMap;
import java.util.Iterator;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.flurry.android.FlurryAgent;
import com.flurry.android.FlurryAds;
import com.flurry.android.ads.FlurryAdBanner;
import com.flurry.android.ads.FlurryAdBannerListener;
import com.flurry.android.ads.FlurryAdErrorType;
import com.flurry.android.ads.FlurryAdInterstitial;
import com.flurry.android.ads.FlurryAdInterstitialListener;
import com.flurry.android.ads.FlurryAdNative;
import com.flurry.android.ads.FlurryAdNativeAsset;
import com.flurry.android.ads.FlurryAdNativeListener;
import com.rjfun.cordova.ad.GenericAdPlugin;

public class FlurryAdPlugin extends GenericAdPlugin {
    private static final String LOGTAG = "FlurryAdPlugin";

    private static final String AD_TOP_BANNER = "TOP_BANNER";
    private static final String AD_BOTTOM_BANNER = "BOTTOM_BANNER";
    private static final String AD_INTERSTITIAL = "INTERSTITIAL_MAIN_VIEW";
    private static final String AD_NATIVE = "NATIVE_AD";
    
    private static final String TEST_APIKEY = "G56KN4J49YT66CFRD5K6";

    private boolean inited = false;
    private String adSpace = AD_BOTTOM_BANNER;
    
	public static final String ACTION_CREATE_NATIVEAD = "createNativeAd";
	public static final String ACTION_REMOVE_NATIVEAD = "removeNativeAd";
	public static final String ACTION_SET_NATIVEAD_CLICKAREA = "setNativeAdClickArea";

	private RelativeLayout layout;
	
	public class FlexNativeAd {
		public String adId;
		public int x, y, w, h;
		public FlurryAdNative ad;
		public View view;
		public View tracking;
	};
	
	private HashMap<String, FlexNativeAd> nativeAds = new HashMap<String, FlexNativeAd>();
	
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
			Log.d(LOGTAG,"isTesting");
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
    public boolean execute(String action, JSONArray inputs, CallbackContext callbackContext) throws JSONException {
        PluginResult result = null;
        
    	if (ACTION_CREATE_NATIVEAD.equals(action)) {
            String adid = inputs.optString(0);
            if(this.testTraffic) adid = this.__getTestNativeAdId();
            this.createNativeAd(adid);
            result = new PluginResult(Status.OK);
            
    	} else if (ACTION_REMOVE_NATIVEAD.equals(action)) {
            String adid = inputs.optString(0);
            this.removeNativeAd(adid);
            result = new PluginResult(Status.OK);
            
    	} else if (ACTION_SET_NATIVEAD_CLICKAREA.equals(action)) {
            String adid = inputs.optString(0);
            int x = inputs.optInt(1);
            int y = inputs.optInt(2);
            int w = inputs.optInt(3);
            int h = inputs.optInt(4);
            this.setNativeAdClickArea(adid, x, y, w, h);
            result = new PluginResult(Status.OK);
            
    	} else {
    		return super.execute(action, inputs, callbackContext);
    	}
    	
    	if(result != null) sendPluginResult(result, callbackContext);
    	
		return true;
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
	
	protected String __getTestNativeAdId() {
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
	
	FlurryAdNativeListener mFlurryAdNativeListener = new FlurryAdNativeListener() {

		@Override
		public void onAppExit(FlurryAdNative arg0) {
	    	fireAdEvent(EVENT_AD_LEAVEAPP, ADTYPE_NATIVE);
		}

		@Override
		public void onClicked(FlurryAdNative arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onCloseFullscreen(FlurryAdNative arg0) {
        	fireAdEvent(EVENT_AD_DISMISS, ADTYPE_NATIVE);
		}

		@Override
		public void onError(FlurryAdNative arg0, FlurryAdErrorType adErrorType, int errorCode) {
        	fireAdErrorEvent(EVENT_AD_FAILLOAD, errorCode, getErrorReason(adErrorType), ADTYPE_NATIVE);
		}

		@Override
		public void onFetched(FlurryAdNative ad) {
			fireNativeAdLoadEvent(ad);
		}

		@Override
		public void onImpressionLogged(FlurryAdNative arg0) {
        	fireAdEvent(EVENT_AD_PRESENT, ADTYPE_NATIVE);
		}

		@Override
		public void onShowFullscreen(FlurryAdNative arg0) {
        	fireAdEvent(EVENT_AD_PRESENT, ADTYPE_NATIVE);
		}
		
	};

	// nativeAdId = "apikey/index"
    public void createNativeAd(final String adId) {
    	String[] fields = adId.split("/");
    	String apikey = (fields.length >= 1) ? fields[0] : TEST_APIKEY;
    	String index = (fields.length >= 2) ? fields[1] : "0";
    	
		if(isTesting) apikey = TEST_APIKEY;
		
		validateSession(apikey);
		
	  	Log.d(LOGTAG, "createNativeAd: " + apikey + "/" + index);
	    final Activity activity = getActivity();
	    activity.runOnUiThread(new Runnable(){
            @Override
            public void run() {
            	if(nativeAds.containsKey(adId)) {
            		removeNativeAd(adId);
            	}
            	
            	if(layout == null) {
            		layout = new RelativeLayout(getActivity());
            		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.MATCH_PARENT);
            		ViewGroup parentView = (ViewGroup) getView().getRootView();
            		parentView.addView(layout, params);
            	}
            	
            	FlexNativeAd unit = new FlexNativeAd();
            	unit.adId = adId;
            	unit.x = unit.y = 0;
                unit.w = unit.h = 1;

            	unit.view = new View(getActivity());
            	unit.tracking = new View(getActivity());
            	layout.addView(unit.view, new RelativeLayout.LayoutParams(unit.w, unit.h));
            	layout.addView(unit.tracking, new RelativeLayout.LayoutParams(unit.w, unit.h));
            	if(isTesting) {
                	unit.view.setBackgroundColor(0x3000FF00);
            	}

            	// pass scroll event in tracking view to webview to improve UX
            	final View webV = getView();
            	final View trackingV = unit.tracking;
            	OnTouchListener t = new OnTouchListener(){
            		public float mTapX = 0, mTapY = 0;

					@Override
					public boolean onTouch(View v, MotionEvent evt) {
						switch(evt.getAction()) {
						case MotionEvent.ACTION_DOWN:
							mTapX = evt.getX();
							mTapY = evt.getY();
							break;

						case MotionEvent.ACTION_UP:
							if(Math.abs(evt.getX() - mTapX) + Math.abs(evt.getY() - mTapY) < 10) {
								mTapX = 0;
								mTapY = 0;
								evt.setAction(MotionEvent.ACTION_DOWN);
								trackingV.dispatchTouchEvent(evt);
								evt.setAction(MotionEvent.ACTION_UP);
								return trackingV.dispatchTouchEvent(evt);
							}
							break;
						}

						return webV.dispatchTouchEvent(evt);
					}
            	};
            	unit.view.setOnTouchListener(t);

            	unit.ad = new FlurryAdNative(getActivity(), AD_NATIVE);
            	unit.ad.setListener( mFlurryAdNativeListener );

            	nativeAds.put(adId, unit);

            	unit.ad.fetchAd();
            }
	    });
    }
    
    public void fireNativeAdLoadEvent(FlurryAdNative ad) {
        Iterator<String> it = nativeAds.keySet().iterator();
        while(it.hasNext()) {
        	String key = it.next();
        	FlexNativeAd unit = nativeAds.get(key);
        	if((unit != null) && (unit.ad == ad)){
				String jsonData = "{}";
				try {
					JSONObject json = new JSONObject();
					json.put("adNetwork", __getProductShortName());
					json.put("adEvent", EVENT_AD_LOADED);
					json.put("adType", ADTYPE_NATIVE);
					json.put("adId", unit.adId);
					
					JSONObject adRes = new JSONObject();

					FlurryAdNativeAsset asset = ad.getAsset("headline");
					if(asset != null) adRes.put("headline", asset.getValue());
					asset = ad.getAsset("summary");
					if(asset != null) adRes.put("summary", asset.getValue());
					asset = ad.getAsset("source");
					if(asset != null) adRes.put("source", asset.getValue());
					
					asset = ad.getAsset("secBrandingLogo");
					if(asset != null) {
						JSONObject img = new JSONObject();
						img.put("url", asset.getValue());
						img.put("width", 20);
						img.put("height", 20);
						adRes.put("secBrandingLogo", img);
					}
					
					asset = ad.getAsset("secHqBrandingLogo");
					if(asset != null) {
						JSONObject img = new JSONObject();
						img.put("url", asset.getValue());
						img.put("width", 40);
						img.put("height", 40);
						adRes.put("secHqBrandingLogo", img);
					}
					
					asset = ad.getAsset("secOrigImg");
					if(asset != null) {
						JSONObject img = new JSONObject();
						img.put("url", asset.getValue());
						img.put("width", 627);
						img.put("height", 627);
						adRes.put("secOrigImg", img);
					}

					asset = ad.getAsset("secHqImage");
					if(asset != null) {
						JSONObject img = new JSONObject();
						img.put("url", asset.getValue());
						img.put("width", 1200);
						img.put("height", 627);
						adRes.put("secHqImage", img);
					}

					asset = ad.getAsset("secImage");
					if(asset != null) {
						JSONObject img = new JSONObject();
						img.put("url", asset.getValue());
						img.put("width", 82);
						img.put("height", 82);
						adRes.put("secImage", img);
					}

					json.put("adRes", adRes);
					jsonData = json.toString();
				} catch(Exception e) {
				}
            	unit.ad.setTrackingView(unit.tracking);
				fireEvent(__getProductShortName(), EVENT_AD_LOADED, jsonData);
        		break;
        	}
        }
    }
    
    public void removeNativeAd(final String adId) {
	    final Activity activity = getActivity();
	    activity.runOnUiThread(new Runnable(){
            @Override
            public void run() {
            	if(nativeAds.containsKey(adId)) {
            		FlexNativeAd unit = nativeAds.remove(adId);
            		if(unit.view != null) {
            			ViewGroup parentView = (ViewGroup) unit.view.getParent();
            			if(parentView != null) {
            				parentView.removeView(unit.view);
            			}
            			unit.view = null;
            		}
            		if(unit.ad != null){
            			unit.ad.removeTrackingView();
            			unit.ad.destroy();
            			unit.ad = null;
            		}
            	}
            }
	    });
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void setNativeAdClickArea(final String adId, int x, int y, int w, int h) {
		final FlexNativeAd unit = nativeAds.get(adId);
		if(unit != null) {
	        DisplayMetrics metrics = cordova.getActivity().getResources().getDisplayMetrics();
			unit.x = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, x, metrics);
			unit.y = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, y, metrics);
			unit.w = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, w, metrics);
			unit.h = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, h, metrics);

        	View rootView = getView().getRootView();
        	int offsetRootView[] = {0,0}, offsetMainView[] = {0,0};
        	rootView.getLocationOnScreen( offsetRootView );
        	getView().getLocationOnScreen( offsetMainView );
        	unit.x += (offsetMainView[0] - offsetRootView[0]);
        	unit.y += (offsetMainView[1] - offsetRootView[1]);

		    final Activity activity = getActivity();
		    activity.runOnUiThread(new Runnable(){
	            @Override
	            public void run() {
	        		if(unit.view != null) {
	        			unit.view.setLeft(unit.x);
	        			unit.view.setTop(unit.y);
	        			unit.view.setRight(unit.x+unit.w);
	        			unit.view.setBottom(unit.y+unit.h);
	        		}
	            }
		    });
		}
    }
}
