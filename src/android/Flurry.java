package com.rjfun.cordova.plugin;

import com.flurry.android.FlurryAdType;
import com.flurry.android.FlurryAds;
import com.flurry.android.FlurryAdSize;
import com.flurry.android.FlurryAgent;
import com.flurry.android.FlurryAdListener;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.LinearLayoutSoftKeyboardDetect;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.os.Bundle;

import java.util.Iterator;
import java.util.Random;

/**
 * This class represents the native implementation for the Flurry Cordova plugin.
 * This plugin can be used to request Flurry ads natively via the Google Flurry SDK.
 * The Google Flurry SDK is a dependency for this plugin.
 */
public class Flurry extends CordovaPlugin {
  ViewGroup mBanner;
  private static final String adTopBanner = "TOP_BANNER";
  private static final String adBottomBanner = "BOTTOM_BANNER";
  private static final String adFull = "INTERSTITIAL_MAIN_VIEW";
  
  private String adSpace="MediatedBannerBottom";

  /** Whether or not the ad should be positioned at top or bottom of screen. */
  private String publisherId;
  private FlurryAdSize adSize;
  private boolean bannerAtTop;

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
  private static final int	ADSPACE_ARG_INDEX = 3;

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
    PluginResult result;
    if (ACTION_CREATE_BANNER_VIEW.equals(action)) {
      result = executeCreateBannerView(inputs);
    } else if (ACTION_CREATE_INTERSTITIAL_VIEW.equals(action)) {
      result = executeCreateInterstitialView(inputs);
    } else if (ACTION_DESTROY_BANNER_VIEW.equals(action)) {
      result = executeDestroyBannerView();
    } else if (ACTION_REQUEST_INTERSTITIAL_AD.equals(action)) {
      result = executeRequestInterstitialAd(inputs);
    } else if (ACTION_REQUEST_AD.equals(action)) {
      result = executeRequestAd(inputs);
    } else if (ACTION_SHOW_AD.equals(action)) {
      result = executeShowAd(inputs);
    } else {
      Log.d(LOGTAG, String.format("Invalid action passed: %s", action));
      result = new PluginResult(Status.INVALID_ACTION);
    }
    callbackContext.sendPluginResult( result );

    return true;
  }

  @Override
  public void onDestroy() {
	  Context ctx = cordova.getActivity();
	  FlurryAds.removeAd(ctx, adSpace, mBanner);
	  FlurryAgent.onEndSession(ctx);
	  
	  super.onDestroy();
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
  private PluginResult executeCreateBannerView(JSONArray inputs) {
    // Get the input data.
    try {
      this.publisherId = inputs.getString( PUBLISHER_ID_ARG_INDEX );
      this.adSize = adSizeFromString( inputs.getString( AD_SIZE_ARG_INDEX ));
      this.bannerAtTop = inputs.getBoolean( POSITION_AT_TOP_ARG_INDEX );
      //this.adSpace = inputs.getString( ADSPACE_ARG_INDEX );
      
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
    
    Context ctx = cordova.getActivity();
    FlurryAgent.onStartSession(ctx, this.publisherId);
    FlurryAds.enableTestAds( true );
    FlurryAds.setAdListener( new BasicListener() );
    
    mBanner = (ViewGroup) webView.getParent();
    
    adSpace = (this.adSize == FlurryAdSize.BANNER_TOP) ? adTopBanner : adBottomBanner;
    
    return new PluginResult(Status.OK);

    // Create the AdView on the UI thread.
    //return executeRunnable(new CreateBannerViewRunnable(this.publisherId, this.adSize));
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
  private PluginResult executeCreateInterstitialView(JSONArray inputs) {
    // Get the input data.
    try {
      this.publisherId = inputs.getString( PUBLISHER_ID_ARG_INDEX );
    } catch (JSONException exception) {
      Log.w(LOGTAG, String.format("Got JSON Exception: %s", exception.getMessage()));
      return new PluginResult(Status.JSON_EXCEPTION);
    }

    // Create the Interstitial View on the UI thread.
    return executeRunnable(new CreateInterstitialViewRunnable(this.publisherId));
  }

  private PluginResult executeDestroyBannerView() {
	FlurryAds.removeAd(cordova.getActivity(), adSpace, mBanner);  
    return new PluginResult(Status.OK);

    // Destroy the AdView on the UI thread.
    //return executeRunnable(new DestroyBannerViewRunnable());
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
  private PluginResult executeRequestAd(JSONArray inputs) {
    boolean isTesting;
    JSONObject inputExtras;

    // Get the input data.
    try {
      isTesting = inputs.getBoolean( IS_TESTING_ARG_INDEX );
      inputExtras = inputs.getJSONObject( EXTRAS_ARG_INDEX );

    } catch (JSONException exception) {
      Log.w(LOGTAG, String.format("Got JSON Exception: %s", exception.getMessage()));
      return new PluginResult(Status.JSON_EXCEPTION);
    }

    FlurryAds.enableTestAds( isTesting );
    FlurryAds.fetchAd(cordova.getActivity(), adSpace, mBanner, this.adSize );
    return new PluginResult(Status.OK);

    // Request an ad on the UI thread.
    //return executeRunnable(new RequestAdRunnable(isTesting, inputExtras));
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
  private PluginResult executeRequestInterstitialAd(JSONArray inputs) {
    boolean isTesting;
    JSONObject inputExtras;

    // Get the input data.
    try {
      isTesting = inputs.getBoolean( IS_TESTING_ARG_INDEX );
      inputExtras = inputs.getJSONObject( EXTRAS_ARG_INDEX );

    } catch (JSONException exception) {
      Log.w(LOGTAG, String.format("Got JSON Exception: %s", exception.getMessage()));
      return new PluginResult(Status.JSON_EXCEPTION);
    }

    FlurryAds.enableTestAds( isTesting );
    FlurryAds.fetchAd(cordova.getActivity(), adSpace, mBanner, FlurryAdSize.FULLSCREEN );
    return new PluginResult(Status.OK);

    // Request an ad on the UI thread.
    //return executeRunnable(new RequestInterstitialAdRunnable(isTesting, inputExtras));
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
  private PluginResult executeShowAd(JSONArray inputs) {
    boolean show;

    // Get the input data.
    try {
      show = inputs.getBoolean( SHOW_AD_ARG_INDEX );
    } catch (JSONException exception) {
      Log.w(LOGTAG, String.format("Got JSON Exception: %s", exception.getMessage()));
      return new PluginResult(Status.JSON_EXCEPTION);
    }

    if( show ) {
    	if(FlurryAds.isAdReady(adSpace)) {
    		FlurryAds.displayAd(cordova.getActivity(), adSpace, mBanner);
    	} else {
    		FlurryAds.fetchAd(cordova.getActivity(), adSpace, mBanner, this.adSize);
    	}
    } else {
    	FlurryAds.removeAd(cordova.getActivity(), adSpace, mBanner);
    }
    return new PluginResult(Status.OK);
    
    // Request an ad on the UI thread.
    //return executeRunnable( new ShowAdRunnable(show) );
  }

  /**
   * Executes the runnable on the activity from the plugin's context.  This
   * is a blocking call that waits for a notification from the runnable
   * before it continues.
   *
   * @param runnable The FlurryRunnable representing the command to run.
   * @return A PluginResult representing the result of the command.
   */
  private PluginResult executeRunnable(FlurryRunnable runnable) {
    synchronized (runnable) {
      cordova.getActivity().runOnUiThread(runnable);
      try {
        if (runnable.getPluginResult() == null) {
          runnable.wait();
        }
      } catch (InterruptedException exception) {
        Log.w(LOGTAG, String.format("Interrupted Exception: %s", exception.getMessage()));
        return new PluginResult(Status.ERROR, "Interruption occurred when running on UI thread");
      }
    }
    return runnable.getPluginResult();
  }

  /**
   * Represents a runnable for the Flurry plugin that will run on the UI thread.
   */
  private abstract class FlurryRunnable implements Runnable {
    protected PluginResult result = null;

    public PluginResult getPluginResult() {
      return result;
    }
  }

  /** Runnable for the createBannerView action. */
  private class CreateBannerViewRunnable extends FlurryRunnable {
    private String publisherId;
    private FlurryAdSize adSize;

    public CreateBannerViewRunnable(String publisherId, FlurryAdSize adSize) {
      this.publisherId = publisherId;
      this.adSize = adSize;
    }

    @Override
    public void run() {
      if (adSize == null) {
        result = new PluginResult(Status.ERROR, "AdSize is null. Did you use an AdSize constant?");
      } else {
    	mBanner = (ViewGroup) webView.getParent();
    	
    	FlurryAgent.onStartSession(cordova.getActivity(), this.publisherId);
    	FlurryAds.setAdListener(new BasicListener());
    	FlurryAds.fetchAd(cordova.getActivity(), adSpace, mBanner, this.adSize);

    	// Notify the plugin.
        result = new PluginResult(Status.OK);
      }
      synchronized (this) {
        this.notify();
      }
    }
  }

  /** Runnable for the createInterstitialView action. */
  private class CreateInterstitialViewRunnable extends FlurryRunnable {
    private String publisherId;

    public CreateInterstitialViewRunnable(String publisherId) {
      this.publisherId = publisherId;
      result = new PluginResult(Status.NO_RESULT);
    }

    @Override
    public void run() {
      // Create the interstitial Ad.
      // TODO: 	
    	
      result = new PluginResult(Status.OK);

      synchronized (this) {
        this.notify();
      }
    }
  }

  private class DestroyBannerViewRunnable extends FlurryRunnable {
    public DestroyBannerViewRunnable() {
      result = new PluginResult(Status.NO_RESULT);
    }

    @Override
    public void run() {
      FlurryAds.removeAd(cordova.getActivity(),adSpace, mBanner);
      
      // Notify the plugin.
      result = new PluginResult(Status.OK);
      synchronized (this) {
        this.notify();
      }
    }
  }

  /** Runnable for the basic requestAd action. */
	private class RequestAdBasicRunnable extends FlurryRunnable {
		private boolean isTesting;
		private JSONObject inputExtras;
		private String adType;

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			if (adType.isEmpty()) {
				result = new PluginResult(
						Status.ERROR,
						"AdView/InterstitialAd is null.  Did you call createBannerView/createInterstitialView?");
			} else {
				FlurryAds.enableTestAds( isTesting );
				
				FlurryAds.fetchAd(cordova.getActivity(), adSpace, mBanner, adSize);
				result = new PluginResult(Status.OK);
			}
			synchronized (this) {
				this.notify();
			}
		}

	}

  /** Runnable for the requestAd action for Banner. */
  private class RequestAdRunnable extends RequestAdBasicRunnable {
    public RequestAdRunnable(boolean isTesting, JSONObject inputExtras) {
      super.isTesting = isTesting;
      super.inputExtras = inputExtras;
      super.adType = "banner";
    }
  }

  /** Runnable for the requestAd action for Interstitial. */
  private class RequestInterstitialAdRunnable extends RequestAdBasicRunnable {
    public RequestInterstitialAdRunnable(boolean isTesting, JSONObject inputExtras) {
      super.isTesting = isTesting;
      super.inputExtras = inputExtras;
      super.adType = "interstitial";
      result = new PluginResult(Status.NO_RESULT);
    }
  }

  /** Runnable for the showAd action. This is only available for Banner View. */
  private class ShowAdRunnable extends FlurryRunnable {
    private boolean show;

    public ShowAdRunnable(boolean show) {
      this.show = show;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
      FlurryAds.displayAd(cordova.getActivity(), adSpace, mBanner);
      synchronized (this) {
        this.notify();
      }
    }
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
  public class BasicListener implements FlurryAdListener  {
	@Override
	public void onAdClicked(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAdClosed(String arg0) {
		// TODO Auto-generated method stub
		webView.loadUrl("javascript:cordova.fireDocumentEvent('onDismissAd');");
	}

	@Override
	public void onAdOpened(String arg0) {
		// TODO Auto-generated method stub
		webView.loadUrl("javascript:cordova.fireDocumentEvent('onPresentAd');");
	}

	@Override
	public void onApplicationExit(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRenderFailed(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRendered(String arg0) {
		// TODO Auto-generated method stub
	      Log.w("Flurry", "BannerAdLoaded");
	      webView.loadUrl("javascript:cordova.fireDocumentEvent('onReceiveAd');");
	}

	@Override
	public void onVideoCompleted(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean shouldDisplayAd(String arg0, FlurryAdType arg1) {
		// TODO Auto-generated method stub
		webView.loadUrl("javascript:cordova.fireDocumentEvent('onLeaveToAd');");
		return false;
	}

	@Override
	public void spaceDidFailToReceiveAd(String errorCode) {
		// TODO Auto-generated method stub
	      webView.loadUrl(String.format(
	              "javascript:cordova.fireDocumentEvent('onFailedToReceiveAd', { 'error': '%s' });",
	              errorCode));

	}

	@Override
	public void spaceDidReceiveAd(String arg0) {
		// TODO Auto-generated method stub
		
	}
  }

  /**
   * Gets an AdSize object from the string size passed in from JavaScript.
   * Returns null if an improper string is provided.
   *
   * @param size The string size representing an ad format constant.
   * @return An AdSize object used to create a banner.
   */
  public FlurryAdSize adSizeFromString(String size) {
    if ("FULLSCREEN".equals(size)) {
      return FlurryAdSize.FULLSCREEN;
    } else {
      return this.bannerAtTop ? FlurryAdSize.BANNER_TOP : FlurryAdSize.BANNER_BOTTOM;
    }
  }

}

