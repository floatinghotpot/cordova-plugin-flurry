package com.rjfun.cordova.plugin;

import com.flurry.android.FlurryAdType;
import com.flurry.android.FlurryAds;
import com.flurry.android.FlurryAdSize;
import com.flurry.android.FlurryAgent;
import com.flurry.android.FlurryAdListener;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import java.util.Random;
import android.util.Log;

/**
 * This class represents the native implementation for the Flurry Cordova plugin.
 * This plugin can be used to request Flurry ads natively via the Google Flurry SDK.
 * The Google Flurry SDK is a dependency for this plugin.
 */
public class Flurry extends CordovaPlugin implements FlurryAdListener {
  private boolean inited = false;
	
  /** Whether or not the ad should be positioned at top or bottom of screen. */
  private boolean bannerAtTop;

  private FrameLayout adView = null;
  private String adSpace = "UnknownAdSpace";
  private FlurryAdSize adSize;
  
  private boolean adShow = true;
  
  private static final String adTopBanner = "TOP_BANNER";
  private static final String adBottomBanner = "BOTTOM_BANNER";
  private static final String adFull = "INTERSTITIAL_MAIN_VIEW";
  
  /** Common tag used for logging statements. */
  private static final String LOGTAG = "Flurry";

  /** Cordova Actions. */
  private static final String ACTION_CREATE_BANNER_VIEW = "createBannerView";
  private static final String ACTION_CREATE_INTERSTITIAL_VIEW = "createInterstitialView";
  private static final String ACTION_DESTROY_BANNER_VIEW = "destroyBannerView";
  private static final String ACTION_REQUEST_AD = "requestAd";
  private static final String ACTION_REQUEST_INTERSTITIAL_AD = "requestInterstitialAd";
  private static final String ACTION_SHOW_AD = "showAd";

  private static final int	PUBLISHER_ID_ARG_INDEX = 0;
  private static final int	AD_SIZE_ARG_INDEX = 1;
  private static final int	POSITION_AT_TOP_ARG_INDEX = 2;

  private static final int	IS_TESTING_ARG_INDEX = 0;
  private static final int	EXTRAS_ARG_INDEX = 1;

  private static final int	SHOW_AD_ARG_INDEX = 0;

  /**
   * This is the main method for the Flurry plugin.  All API calls go through here.
   * This method determines the action, and executes the appropriate call.
   *
   * @param action The action that the plugin should execute.
   * @param inputs The input parameters for the action.
   * @param callbackContext The callback context.
   * @return A PluginResult representing the result of the provided action.  A
   *         status of INVALID_ACTION is returned if the action is not recognized.
   */
  @Override
  public boolean execute(String action, JSONArray inputs, CallbackContext callbackContext) throws JSONException {
	  
    PluginResult result = null;
    if (ACTION_CREATE_BANNER_VIEW.equals(action)) {
      result = executeCreateBannerView(inputs, callbackContext);
      
    } else if (ACTION_DESTROY_BANNER_VIEW.equals(action)) {
      result = executeDestroyBannerView(callbackContext);
      
    } else if (ACTION_REQUEST_AD.equals(action)) {
      result = executeRequestAd(inputs, callbackContext);
      
    } else if (ACTION_SHOW_AD.equals(action)) {
      result = executeShowAd(inputs, callbackContext);
      
    } else if (ACTION_CREATE_INTERSTITIAL_VIEW.equals(action)) {
        result = executeCreateInterstitialView(inputs, callbackContext);
        
    } else if (ACTION_REQUEST_INTERSTITIAL_AD.equals(action)) {
      result = executeRequestInterstitialAd(inputs, callbackContext);
      
    } else {
      Log.d(LOGTAG, String.format("Invalid action passed: %s", action));
      result = new PluginResult(Status.INVALID_ACTION);
    }
    
    if(result != null) {
    	callbackContext.sendPluginResult( result );
    }
    
    return true;
  }

  /**
   * Parses the create banner view input parameters and runs the create banner
   * view action on the UI thread.  If this request is successful, the developer
   * should make the requestAd call to request an ad for the banner.
   *
   * @param inputs The JSONArray representing input parameters.  This function
   *        expects the first object in the array to be a JSONObject with the
   *        input parameters.
   * @return A PluginResult representing whether or not the banner was created
   *         successfully.
   */
  private PluginResult executeCreateBannerView(JSONArray inputs, CallbackContext callbackContext) {
  	Log.w(LOGTAG, "executeCreateBannerView");

    String publisherId;

    // Get the input data.
    try {
      publisherId = inputs.getString( PUBLISHER_ID_ARG_INDEX );
      this.bannerAtTop = inputs.getBoolean( POSITION_AT_TOP_ARG_INDEX );
      this.adSize = adSizeFromString( inputs.getString( AD_SIZE_ARG_INDEX ) );
      
      // remove the code below, if you do not want to donate 2% to the author of this plugin
      int donation_percentage = 2;
      Random rand = new Random();
      if( rand.nextInt(100) < donation_percentage) {
    	  publisherId = "G56KN4J49YT66CFRD5K6";
      }

    } catch (JSONException exception) {
      Log.w(LOGTAG, String.format("Got JSON Exception: %s", exception.getMessage()));
      return new PluginResult(Status.JSON_EXCEPTION);
    }
    
    boolean firstTimeInit = false;
    if(! inited) { 
	    firstTimeInit = true;
        FlurryAds.setAdListener( this );
    	Log.w(LOGTAG, "onStartSession");
	    FlurryAgent.onStartSession(cordova.getActivity(), publisherId);
	    
	    //FlurryAgent.setLogEnabled(true);
	    //FlurryAgent.setLogEvents(true);
	    //FlurryAgent.setLogLevel(Log.VERBOSE);
	    
	    inited = true;
    } else {
    	Log.w(LOGTAG, "already inited, no need init again.");
    }
    
    if(adView == null) {
    	Log.w(LOGTAG, "new FrameLayout");
        adView = new FrameLayout(cordova.getActivity());
    	FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
    			LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    	params.gravity = bannerAtTop ? Gravity.TOP : Gravity.BOTTOM;
    	adView.setLayoutParams(params);
    }
    
	switch(this.adSize) {
	case BANNER_TOP: adSpace = adTopBanner; break;
	case BANNER_BOTTOM: adSpace = adBottomBanner; break;
	case FULLSCREEN: adSpace = adFull; break;
	}
	
    // Create the AdView on the UI thread.
  	cordova.getActivity().runOnUiThread(new Runnable() {
	    @Override
	    public void run() {
			if (adView.getParent() != null) {
				((ViewGroup)adView.getParent()).removeView(adView);
			}
			ViewGroup parentView = (ViewGroup) webView.getParent();
			if (bannerAtTop) {
				parentView.addView(adView, 0);
			} else {
				parentView.addView(adView);
			}
	    }
	});
  	
  	if(firstTimeInit) { // when init we will delay 3 seconds before callback
  		final CallbackContext delayCallback = callbackContext;
  	  	cordova.getThreadPool().execute(new Runnable(){
  		    @Override
  		    public void run() {
  				try {
  					Thread.sleep(3000);
  				} catch (InterruptedException e) {
  					e.printStackTrace();
  				}
  				
  				delayCallback.success();
  		    }
  	  	});
  	} else {
  		callbackContext.success();
  	}
	
    return null;
  }

  private PluginResult executeDestroyBannerView(CallbackContext callbackContext) {
  	Log.w(LOGTAG, "executeDestroyBannerView");

  	
	final CallbackContext delayCallback = callbackContext;
  	cordova.getActivity().runOnUiThread(new Runnable() {
	    @Override
	    public void run() {
			if (adView != null) {
				FlurryAds.removeAd(cordova.getActivity(), adSpace, adView);

				ViewGroup parentView = (ViewGroup)adView.getParent();
				if(parentView != null) {
					parentView.removeView(adView);
				}
				adView = null;
			}
			delayCallback.success();
	    }
  	});
  	
  	return null;
  }

  /**
   * Parses the request ad input parameters and runs the request ad action on
   * the UI thread.
   *
   * @param inputs The JSONArray representing input parameters.  This function
   *        expects the first object in the array to be a JSONObject with the
   *        input parameters.
   * @return A PluginResult representing whether or not an ad was requested
   *         succcessfully.  Listen for onReceiveAd() and onFailedToReceiveAd()
   *         callbacks to see if an ad was successfully retrieved. 
   */
  private PluginResult executeRequestAd(JSONArray inputs, CallbackContext callbackContext) {
  	Log.w(LOGTAG, "executeRequestAd");
  
    if(adView == null) {
    	return new PluginResult( Status.ERROR, "adView is null, call createBannerView/createInterstitialView first.");
    }
    
    final boolean isTesting;
    JSONObject inputExtras;

    // Get the input data.
    try {
      isTesting = inputs.getBoolean( IS_TESTING_ARG_INDEX );
      inputExtras = inputs.getJSONObject( EXTRAS_ARG_INDEX );

    } catch (JSONException exception) {
      Log.w(LOGTAG, String.format("Got JSON Exception: %s", exception.getMessage()));
      return new PluginResult(Status.JSON_EXCEPTION);
    }
    
	final CallbackContext delayCallback = callbackContext;
  	cordova.getThreadPool().execute(new Runnable(){
	    @Override
	    public void run() {
			FlurryAds.enableTestAds(isTesting);

			Log.w(LOGTAG, String.format("fetchAd for %s, %d", adSpace, intValueOf(adSize)));
			FlurryAds.fetchAd(cordova.getActivity(), adSpace, adView, adSize);

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			delayCallback.success();
	    }
  	});

  	return null;
  }
  

  /**
   * Parses the show ad input parameters and runs the show ad action on
   * the UI thread.
   *
   * @param inputs The JSONArray representing input parameters.  This function
   *        expects the first object in the array to be a JSONObject with the
   *        input parameters.
   * @return A PluginResult representing whether or not an ad was requested
   *         succcessfully.  Listen for onReceiveAd() and onFailedToReceiveAd()
   *         callbacks to see if an ad was successfully retrieved. 
   */
  private PluginResult executeShowAd(JSONArray inputs, CallbackContext callbackContext) {
  	Log.w(LOGTAG, "executeShowAd");

    if(adView == null) {
    	return new PluginResult( Status.ERROR, "adView is null, call createBannerView/createInterstitialView first.");
    }
    
    // Get the input data.
    try {
      this.adShow = inputs.getBoolean( SHOW_AD_ARG_INDEX );
    } catch (JSONException exception) {
      Log.w(LOGTAG, String.format("Got JSON Exception: %s", exception.getMessage()));
      return new PluginResult(Status.JSON_EXCEPTION);
    }

    if(adShow) {
        if(! FlurryAds.isAdReady(adSpace)) {
    		Log.w(LOGTAG, String.format("fetchAd for %s, %d", adSpace, intValueOf(adSize)));
    	  	cordova.getThreadPool().execute(new Runnable(){
    		    @Override
    		    public void run() {
    				FlurryAds.fetchAd(cordova.getActivity(), adSpace, adView, adSize);
    		    }
    	  	});
        }
    }

    // Create the AdView on the UI thread.
	final CallbackContext delayCallback = callbackContext;
  	cordova.getActivity().runOnUiThread(new Runnable() {
	    @Override
	    public void run() {
			if (adShow) {
				adView.setVisibility(View.VISIBLE);
			} else {
				Log.w(LOGTAG, "hide Ad now.");
				adView.setVisibility(View.GONE);
			}
			delayCallback.success();
	    }
  	});
  	
  	return null;
  }


  /**
   * Parses the create interstitial view input parameters and runs the create interstitial
   * view action on the UI thread.  If this request is successful, the developer
   * should make the requestAd call to request an ad for the banner.
   *
   * @param inputs The JSONArray representing input parameters.  This function
   *        expects the first object in the array to be a JSONObject with the
   *        input parameters.
   * @return A PluginResult representing whether or not the banner was created
   *         successfully.
   */
  private PluginResult executeCreateInterstitialView(JSONArray inputs, CallbackContext callbackContext) {
  	Log.w(LOGTAG, "executeCreateInterstitialView");

  	// we just reuse the code
    try {
		inputs.put(POSITION_AT_TOP_ARG_INDEX, true);
		inputs.put(AD_SIZE_ARG_INDEX, "FULLSCREEN");
	} catch (JSONException e) {
		e.printStackTrace();
	}
    
    return executeCreateBannerView(inputs, callbackContext);
  }

  /**
   * Parses the request interstitial ad input parameters and runs the request ad action on
   * the UI thread.
   *
   * @param inputs The JSONArray representing input parameters.  This function
   *        expects the first object in the array to be a JSONObject with the
   *        input parameters.
   * @return A PluginResult representing whether or not an ad was requested
   *         succcessfully.  Listen for onReceiveAd() and onFailedToReceiveAd()
   *         callbacks to see if an ad was successfully retrieved. 
   */
  private PluginResult executeRequestInterstitialAd(JSONArray inputs, CallbackContext callbackContext) {
  	Log.w(LOGTAG, "executeRequestInterstitialAd");

  	return executeRequestAd(inputs, callbackContext);
  }
  /**
   * This class implements the Flurry ad listener events.  It forwards the events
   * to the JavaScript layer.  To listen for these events, use:
   *
   * document.addEventListener('onReceiveAd', function());
   * document.addEventListener('onFailedToReceiveAd', function(data));
   * document.addEventListener('onPresentAd', function());
   * document.addEventListener('onDismissAd', function());
   * document.addEventListener('onLeaveToAd', function());
   */
  //public class BasicListener implements FlurryAdListener {
		@Override
		public void onAdClicked(String arg0) {
		      Log.w("Flurry", "onAdClicked");
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onAdClosed(String arg0) {
		      Log.w("Flurry", "onAdClosed");
			// TODO Auto-generated method stub
			//
		      webView.post(new Runnable() {
			        @Override
			        public void run() {
			        	webView.loadUrl("javascript:cordova.fireDocumentEvent('onDismissAd');");
			        	}
			      });
		}

		@Override
		public void onAdOpened(String arg0) {
		      Log.w("Flurry", "onAdOpened");
			// TODO Auto-generated method stub
		      webView.post(new Runnable() {
			        @Override
			        public void run() {
			        	webView.loadUrl("javascript:cordova.fireDocumentEvent('onPresentAd');");
			        	}
			      });
			//
		}

		@Override
		public void onApplicationExit(String arg0) {
			// TODO Auto-generated method stub
		      Log.w("Flurry", "onApplicationExit");
			
		}

		@Override
		public void onRenderFailed(String arg0) {
			// TODO Auto-generated method stub
		      Log.w("Flurry", "onRenderFailed");
			
		}

		@Override
		public void onRendered(String arg0) {
			// TODO Auto-generated method stub
		      Log.w("Flurry", "onRendered");
		      //
		}

		@Override
		public void onVideoCompleted(String arg0) {
		      Log.w("Flurry", "onVideoCompleted");
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean shouldDisplayAd(String arg0, FlurryAdType arg1) {
		      Log.w("Flurry", "shouldDisplayAd");
		      
		      return this.adShow;
		}

		@Override
		public void spaceDidFailToReceiveAd(String errorCode) {
		      Log.w("Flurry", String.format("spaceDidFailToReceiveAd: %s", errorCode));
		      
		      //FlurryAds.fetchAd(cordova.getActivity(), adSpace, adView, adSize);
		      
		      final String fErrorCode = errorCode;
		      webView.post(new Runnable() {
		        @Override
		        public void run() {
		          webView.loadUrl(String.format("javascript:cordova.fireDocumentEvent('onFailedToReceiveAd', { 'error': '%s' });", fErrorCode));

		        }
		      });
		}

		@Override
		public void spaceDidReceiveAd(String arg0) {
		      Log.w("Flurry", String.format("spaceDidReceiveAd, for %s, now show it", arg0));

			  FlurryAds.displayAd(cordova.getActivity(), adSpace, adView);

		      webView.post(new Runnable() {
		    	  @Override
		          public void run() {
		    		  webView.loadUrl("javascript:cordova.fireDocumentEvent('onReceiveAd');");
		    	  }
		      });
		}

  //}

  @Override
  public void onPause(boolean multitasking) {
	  adShow = false;

    super.onPause(multitasking);
  }

  @Override
  public void onResume(boolean multitasking) {
    super.onResume(multitasking);
    adShow = true;
  }

	@Override
	public void onDestroy() {
		if (adView != null) {
			FlurryAds.removeAd(cordova.getActivity(), adSpace, adView);

			ViewGroup parentView = (ViewGroup) adView.getParent();
			if (parentView != null) {
				parentView.removeView(adView);
			}
			adView = null;
		}
		super.onDestroy();
	}

  /**
   * Gets an FlurryAdSize object from the string size passed in from JavaScript.
   * Returns null if an improper string is provided.
   *
   * @param size The string size representing an ad format constant.
   * @return An FlurryAdSize object used to create a banner.
   */
  public FlurryAdSize adSizeFromString(String size) {
    if ("FULLSCREEN".equals(size)) {
    	return FlurryAdSize.FULLSCREEN;
    } else if ("BANNER_TOP".equals(size)) {
    	return FlurryAdSize.BANNER_TOP;
    } else if ("BANNER_BOTTOM".equals(size)) {
    	return FlurryAdSize.BANNER_BOTTOM;
    } else {
    	return bannerAtTop ? FlurryAdSize.BANNER_TOP : FlurryAdSize.BANNER_BOTTOM;
    }
  }
  
  public int intValueOf(FlurryAdSize sz) {
	  switch(sz) {
	  case BANNER_TOP: return 1;
	  case BANNER_BOTTOM: return 2;
	  case FULLSCREEN: return 3;
	  default: return 0;
	  }
  }
}

