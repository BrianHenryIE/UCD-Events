package ie.sortons.events.client;

import ie.sortons.events.shared.ClientPageData;
import ie.sortons.events.shared.ClientPageDataResponse;
import ie.sortons.events.shared.PageList;
import ie.sortons.events.shared.PagesListResponse;
import ie.sortons.events.shared.RecentPostsResponse;
import ie.sortons.events.shared.SourcePage;
import ie.sortons.events.shared.WallPost;
import ie.sortons.gwtfbplus.client.api.FBCore;
import ie.sortons.gwtfbplus.shared.domain.SignedRequest;
import ie.sortons.gwtfbplus.shared.domain.fql.FqlPage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Joiner;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.kfuntak.gwt.json.serialization.client.HashMapSerializer;
import com.kfuntak.gwt.json.serialization.client.Serializer;

public class RpcService {

	// When it comes time to refactor:
	// https://code.google.com/p/google-apis-client-generator/wiki/TableOfContents

	Serializer serializer = (Serializer) GWT.create(Serializer.class);

	// Must be https for cloud endpoints
	private String apiBase = "https://sortonsevents.appspot.com/_ah/api/";
//TODO
	
	// private String apiBase = "https://dev.sortons.ie/appengine/_ah/api/";

	
	private Long currentPageId;

	private FBCore fbCore;
	// private SimpleEventBus eventBus;

	private ClientPageData clientPageData;

	public ClientPageData getClientPageData() {
		return this.clientPageData;
	}

	public RpcService(SimpleEventBus eventBus) {
		// this.eventBus = eventBus;
	}

	public void setGwtFb(FBCore fbCore) {
		this.fbCore = fbCore;
	}

	{
		if (SignedRequest.getSignedRequestFromHTML() != null)
			currentPageId = Long.parseLong(((SignedRequest) serializer.deSerialize(
					new JSONObject(SignedRequest.getSignedRequestFromHTML()), "ie.sortons.gwtfbplus.shared.domain.SignedRequest"))
					.getPage().getId());
	}

	public void getEventsForPage(RequestCallback callback) {

		// Must be https for cloud endpoints
		String jsonUrl = apiBase + "upcomingEvents/v1/discoveredeventsresponse/";

		String url = jsonUrl + currentPageId;
		url = URL.encode(url);

		// Send request to server and catch any errors.
		RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);

		try {
			@SuppressWarnings("unused")
			Request request = builder.sendRequest(null, callback);
		} catch (RequestException e) {
			System.out.println("Couldn't retrieve JSON : " + e.getMessage() + " :getEventsForPage()");
		}
	}

	public void getWallPostsForPage(final AsyncCallback<List<WallPost>> callback) {

		// Must be https for cloud endpoints
		String jsonUrl = apiBase + "recentPosts/v1/recentpostsresponse/";

		String url = jsonUrl + currentPageId;
		url = URL.encode(url);

		System.out.println(url);

		// Send request to server and catch any errors.
		RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);

		try {
			builder.sendRequest(null, new RequestCallback() {

				@Override
				public void onResponseReceived(Request request, Response response) {

					System.out.println("response: " + response.getText());

					RecentPostsResponse deResponse = (RecentPostsResponse) serializer.deSerialize(response.getText(),
							"ie.sortons.events.shared.RecentPostsResponse");
					callback.onSuccess(deResponse.getData());
				}

				@Override
				public void onError(Request request, Throwable exception) {
					// TODO Auto-generated method stub

				}
			});
		} catch (RequestException e) {
			System.out.println("Couldn't retrieve JSON : " + e.getMessage() + " :getEventsForPage()");
		}
	}

	public void refreshClientPageData(final AsyncCallback<ClientPageData> callback) {

		String jsonUrl = apiBase + "clientdata/v1/clientpagedata/";

		String url = URL.encode(jsonUrl + currentPageId);

		// Send request to server and catch any errors.
		RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);

		try {
			@SuppressWarnings("unused")
			Request request = builder.sendRequest(null, new RequestCallback() {
				public void onError(Request request, Throwable exception) {
					System.out.println("Couldn't retrieve JSON getClientPageData");
				}

				public void onResponseReceived(Request request, Response response) {
					if (200 == response.getStatusCode()) {

						clientPageData = serializer.deSerialize(response.getText(), ClientPageData.class);

						callback.onSuccess(clientPageData);

					} else {
						System.out.println("Couldn't retrieve JSON (" + response.getStatusText()
								+ ") rpcservice refreshClientPageData");
						// System.out.println("Couldn't retrieve JSON (" +
						// response.getStatusCode() +
						// ") getClientPageData");
						// System.out.println("Couldn't retrieve JSON (" +
						// response.getText() + ")" getClientPageData);
					}
				}
			});
		} catch (RequestException e) {
			System.out.println("catch (RequestException e) Couldn't retrieve JSON : " + e.getMessage() + " getClientPageData()");
		}

	}

	public void addPage(SourcePage newPage, RequestCallback callback) {

		String addPageAPI = apiBase + "clientdata/v1/addPage/" + currentPageId;

		RequestBuilder addPageBuilder = new RequestBuilder(RequestBuilder.POST, addPageAPI);

		addPageBuilder.setHeader("Content-Type", "application/json");

		clientPageData.getSuggestedPages().remove(newPage);

		try {
			@SuppressWarnings("unused")
			Request request = addPageBuilder.sendRequest(serializer.serialize(newPage), callback);
		} catch (RequestException e) {
			System.out.println("Couldn't retrieve JSON : " + e.getMessage() + " :addPage()");
		}

	}

	public void addPagesList(String pagesList, final AsyncCallback<List<SourcePage>> asyncCallback) {

		System.out.println(pagesList);
		String addPagesListAPI = apiBase + "clientdata/v1/addPagesList/" + currentPageId;

		RequestBuilder addPagesListBuilder = new RequestBuilder(RequestBuilder.POST, addPagesListAPI);

		addPagesListBuilder.setHeader("Content-Type", "application/json");

		try {
			@SuppressWarnings("unused")
			Request request = addPagesListBuilder.sendRequest(serializer.serialize(new PageList(pagesList)), new RequestCallback() {
				@Override
				public void onResponseReceived(Request request, Response response) {
					System.out.println(response.getText());
					PagesListResponse addedPages = (PagesListResponse) serializer.deSerialize(response.getText(),
							"ie.sortons.events.shared.PagesListResponse");
					System.out.println(addedPages.getPages());

					asyncCallback.onSuccess(addedPages.getPages());

				}

				@Override
				public void onError(Request request, Throwable exception) {
					// TODO Auto-generated method stub

					asyncCallback.onFailure(null);

				}
			});
		} catch (RequestException e) {
			System.out.println("Couldn't retrieve JSON : " + e.getMessage() + " :addPage()");
		}

	}

	public void removePage(SourcePage page, RequestCallback callback) {

		clientPageData.getSuggestedPages().remove(page);

		String removePageAPI = apiBase + "clientdata/v1/ignorePage/" + currentPageId;

		RequestBuilder ignorePageBuilder = new RequestBuilder(RequestBuilder.POST, removePageAPI);
		ignorePageBuilder.setHeader("Content-Type", "application/json");

		try {
			@SuppressWarnings("unused")
			Request request = ignorePageBuilder.sendRequest(serializer.serialize(page), callback);
		} catch (RequestException e) {
			System.out.println("Couldn't retrieve JSON : " + e.getMessage() + " :ignorePage()");
		}

	}

	public void graphCall(String graphPath, AsyncCallback<JavaScriptObject> callback) {
		fbCore.api(graphPath, callback);
	}

	private boolean alreadyFailed = false;

	/**
	 * Uses FQL
	 * 
	 * @param callback
	 */
	public void getSuggestions(final AsyncCallback<List<SourcePage>> callback) {

		// TODO shouldn't included existing included pages

		java.util.logging.Logger logger = Logger.getLogger(this.getClass().getSimpleName());
		logger.log(Level.INFO, "getSuggestions()!");
		
		
		// Uncaught com.google.gwt.event.shared.UmbrellaException: Exception caught: (TypeError) : Cannot read property 'ie_sortons_events_shared_ClientPageData_includedPages' of undefined
		// Have to wait for the clientpagedata here.
		List<Long> searchPagesList = new ArrayList<Long>();
		if(clientPageData==null)
			return;
		
		for (Long pageId : clientPageData.getIncludedPageIds())
			searchPagesList.add(pageId);

		System.out.println("searchPagesList " + searchPagesList.size());

		// http://blog.jonleonard.com/2012/10/gwt-collectionsshuffle-implementation.html
		for (int index = 0; index < searchPagesList.size(); index += 1) {
			Collections.swap(searchPagesList, index, Random.nextInt(searchPagesList.size()));
		}
		String searchPages = currentPageId + "," + Joiner.on(",").join(searchPagesList);

		System.out.println("searchPagesList shuffled");

		// TODO... someday (soon) this will get too big
		String fql = "SELECT page_id, name, page_url, location FROM page WHERE page_id IN (SELECT page_id FROM page_fan WHERE uid IN ("
				+ searchPages + ") ) AND NOT is_community_page = 'true'";
		// TODO Remove LIMIT 250 before deploying!

		System.out.println(fql);

		String method = "fql.query";
		JSONObject query = new JSONObject();
		query.put("method", new JSONString(method));
		query.put("query", new JSONString(fql));

		System.out.println("fire fql");

		fbCore.api(query.getJavaScriptObject(), new AsyncCallback<JavaScriptObject>() {
			@SuppressWarnings("unchecked")
			public void onSuccess(JavaScriptObject response) {

				HashMapSerializer hashMapSerializer = (HashMapSerializer) GWT.create(HashMapSerializer.class);

				System.out.println("deserialize");

				JSONObject responseJson = new JSONObject(response);

				// TODO check how slow this is compared to overlays
				HashMap<String, FqlPage> map = new HashMap<String, FqlPage>();
				try {
					// TODO stop using a string here
					map = (HashMap<String, FqlPage>) hashMapSerializer.deSerialize(responseJson,
							"ie.sortons.gwtfbplus.shared.domain.fql.FqlPage");
				} catch (Exception e) {

					System.out.println(responseJson);
					// If it's failed here, it's possibly because we've tried
					// this before the sdk has initialized with
					// its access token
					// JsFqlError error = (JsFqlError)
					// serializer.deSerialize(responseJson,
					// "ie.sortons.events.shared.JsFqlError");
					// System.out.println(error.getErrorMsg());
					if (!alreadyFailed) {
						alreadyFailed = true;
						Timer t = new Timer() {
							@Override
							public void run() {
								getSuggestions(callback);
							}
						};
						t.schedule(1000);
					}

				}
				System.out.println("process");
				ArrayList<SourcePage> pages = new ArrayList<SourcePage>();
				for (FqlPage page : map.values()) {
					if (!clientPageData.getIncludedPageIds().contains(page.getPageId()))
						pages.add(new SourcePage(page));
				}

				clientPageData.setSuggestedPages(pages);

				System.out.println("pages.size() " + pages.size());

				// Shuffle the list so it's not a bunch of similar suggestions
				// consecutively
				// http://blog.jonleonard.com/2012/10/gwt-collectionsshuffle-implementation.html
				for (int index = 0; index < clientPageData.getSuggestedPages().size(); index += 1) {
					Collections.swap(clientPageData.getSuggestedPages(), index,
							Random.nextInt(clientPageData.getSuggestedPages().size()));
				}

				callback.onSuccess(clientPageData.getSuggestedPages());

			}

			@Override
			public void onFailure(Throwable caught) {
				// TODO Auto-generated method stub
			}
		});

	}

	/**
	 * This is for the sortonsadmin view
	 * 
	 * 
	 * 
	 * @param asyncCallback
	 */
	public void getAllClients(final AsyncCallback<List<ClientPageData>> asyncCallback) {

		String getAllClientsAPI = apiBase + "clientdata/v1/clientpagedataresponse/";

		RequestBuilder getAllClientsBuilder = new RequestBuilder(RequestBuilder.GET, getAllClientsAPI);
		getAllClientsBuilder.setHeader("Content-Type", "application/json");

		try {
			getAllClientsBuilder.sendRequest(null, new RequestCallback() {

				@Override
				public void onResponseReceived(Request request, Response response) {
					System.out.println(response.getText());
					Serializer serializer = (Serializer) GWT.create(Serializer.class);

					List<ClientPageData> clients = ((ClientPageDataResponse) serializer.deSerialize(response.getText(),
							"ie.sortons.events.shared.ClientPageDataResponse")).getData();

					asyncCallback.onSuccess(clients);
				}

				@Override
				public void onError(Request request, Throwable exception) {
					// TODO Auto-generated method stub

				}
			});
		} catch (RequestException e) {
			System.out.println("Couldn't retrieve JSON : " + e.getMessage() + " :ignorePage()");
		}
	}

}
