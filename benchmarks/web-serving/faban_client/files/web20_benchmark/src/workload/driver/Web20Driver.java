package workload.driver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPathExpressionException;

import org.json.JSONObject;

import com.sun.faban.driver.Background;
import com.sun.faban.driver.BenchmarkDefinition;
import com.sun.faban.driver.BenchmarkDriver;
import com.sun.faban.driver.BenchmarkOperation;
import com.sun.faban.driver.CustomMetrics;
import com.sun.faban.driver.CycleType;
import com.sun.faban.driver.DriverContext;
import com.sun.faban.driver.FixedTime;
import com.sun.faban.driver.HttpTransport;
import com.sun.faban.driver.MatrixMix;
import com.sun.faban.driver.OnceAfter;
import com.sun.faban.driver.Row;
import com.sun.faban.driver.Timing;

import workload.driver.RandomStringGenerator.Mode;
import workload.driver.Web20Client.ClientState;

@BenchmarkDefinition(name = "Elgg benchmark", version = "1.0")
@BenchmarkDriver(name = "ElggDriver", 
									/*
									 * Should be the same as the name attribute
									 * of driverConfig in run.xml
									 */
				 threadPerScale = 1,
				 percentiles = {  "95"})

/**
 * The mix of operations and their proabilities.
 */

@MatrixMix (operations = {
		"BrowsetoElgg",
		"DoLogin",  
		"PostSelfWall", 
		"AddFriend", 
		"Register", 
		"Logout"  },
		mix = { 
		@Row ({0, 90, 55, 10, 10, 1}),
		@Row ({10, 0, 60, 30, 0, 1}),
		@Row ({10, 0, 70, 10, 0, 1}),
		@Row ({10, 0, 30, 5, 0, 1}),
		@Row ({100, 0, 0, 0, 5, 10}),
		@Row ({90, 20, 0, 0, 0, 0})
		}
		)
/*
@MatrixMix (operations = {"AccessHomepage", 
		"DoLogin",  
	"Logout"  },
		mix = { 
		@Row ({0, 100, 0}),
		@Row ({0, 0, 100}),
		@Row ({100, 0, 0})
		}
		)
*/
@Background(operations = 
	{ "UpdateActivity"}, 
	timings = { 
		@FixedTime(cycleTime = 10000, cycleDeviation = 2)}
		
)


/*
@NegativeExponential(cycleDeviation = 2, 
						cycleMean = 4000, // 4 seconds
						cycleType = CycleType.THINKTIME)
*/

/*
@Uniform(cycleMax = 120000,
		 cycleMin = 40000,
		 cycleDeviation = 10,
		 cycleType = CycleType.THINKTIME)

*/
@FixedTime(cycleTime = 2000,
	cycleType = CycleType.THINKTIME, cycleDeviation = 10)
// cycle time or think time - count from the start of prev operation or end

/**
 * The main driver class.
 * 
 * Operations :-
 * 
 * Create new user (X)
 * Login existing user (X)
 * Logout logged in user
 * Activate user
 * Wall post (X)
 * New blog post 
 * Send friend request (X)
 * Update live feed (X)
 * Refresh security token
 * 
 * @author Tapti Palit
 *
 */
public class Web20Driver {

	private List<UserPasswordPair> userPasswordList;

	private DriverContext context;
	private Logger logger;
	private FileHandler fileTxt;

	private SimpleFormatter formatterTxt;

	private ElggDriverMetrics elggMetrics;

	private String hostUrl;

	private UserPasswordPair thisUserPasswordPair;

	private Web20Client thisClient;

	
	private Random random; 
	
	private boolean inited;
	
	/* Constants : URL */
	// ROOT_URL: root dir for localhost:8080. /usr/share/nginx/html/elgg 
	private final String ROOT_URL = "/";

	// jquery-migrate has been removed when updating from 1.2.1 to 2.1.4
	// Graphics have been moved to url(elgg_get_simplecache_url) 
        private final String[] ROOT_URLS = new String[] {
                        "/vendor/bower-asset/requirejs/require.js",
                        "/vendor/bower-asset/jquery/dist/jquery.min.js",
                        "/vendor/bower-asset/jquery-ui/jquery-ui.min.js" }; 

	private final String LOGIN_URL = "/action/login";
	private final String[] LOGIN_URLS = new String[] { 
			"/mod/reportedcontent/graphics/icon_reportthis.gif" };
	private final String ACTIVITY_URL = "/activity";

	private final String[] ACTIVITY_URLS = new String[] {
			"/vendor/fortawesome/font-awesome/css/font-awesome.css" }; 
	
	private final String RIVER_UPDATE_URL = "/activity/proc/updateriver";
	private final String WALL_URL = "/action/wall/status";

	private final String REGISTER_PAGE_URL = "/register";

	private final String DO_REGISTER_URL = "/action/register";
	private final String DO_ADD_FRIEND = "/action/friends/add";

	// elgg chat feature has been deactivated 
	// https://elgg.org/plugins/1829094	
	
	private final String LOGOUT_URL = "/action/logout";
	
	public Web20Driver() throws SecurityException, IOException, XPathExpressionException {

		thisClient = new Web20Client();
		thisClient.setClientState(ClientState.LOGGED_OUT);
		
		context = DriverContext.getContext();
		userPasswordList = new ArrayList<UserPasswordPair>();

		logger = context.getLogger();
		logger.setLevel(Level.INFO);
	
		File usersFile = new File(System.getenv("FABAN_HOME")+"/users.list");
		BufferedReader bw = new BufferedReader(new FileReader(usersFile));
		String line;
		while ((line = bw.readLine()) != null) {
			String tokens[] = line.split(" ");
			UserPasswordPair pair = new UserPasswordPair(tokens[0], tokens[1], tokens[2]);
			userPasswordList.add(pair);
		}
		
		bw.close();
		
		thisUserPasswordPair = userPasswordList.get(context.getThreadId());

		elggMetrics = new ElggDriverMetrics();
		context.attachMetrics(elggMetrics);
	
		// http://localhost:8080	
		hostUrl = "http://"+context.getXPathValue("/webbenchmark/serverConfig/host")+":"+context.getXPathValue("/webbenchmark/serverConfig/port");
		random = new Random();
	}

	private String getRandomUserGUID() {
		int randomIndex = random.nextInt(userPasswordList.size());
		String randomGuid = userPasswordList.get(randomIndex).getGuid();
		while (randomGuid == thisClient.getGuid()) {
			randomIndex = random.nextInt(userPasswordList.size());
			randomGuid = userPasswordList.get(randomIndex).getGuid();
		}
		return randomGuid;
	}

	private UserPasswordPair getRandomUser() {
		int randomIndex = random.nextInt(userPasswordList.size());
		return userPasswordList.get(randomIndex);
	}
	
	Pattern pattern1 = Pattern.compile("input name=\"__elgg_token\" value=\"(.*?)\" type=\"hidden\"");
	Pattern pattern2 = Pattern.compile("input name=\"__elgg_ts\" value=\"(.*?)\" type=\"hidden\"");
	
	private void updateElggTokenAndTs(Web20Client client, StringBuilder sb, boolean updateGUID) {

		String elggToken = null;
		String elggTs = null;
		
	    Matcher matcherToken = pattern1.matcher(sb.toString());
	    while (matcherToken.find()) {
	    	elggToken = matcherToken.group(1);
	    }
	    
		Matcher matcherTs = pattern2.matcher(sb.toString());
		while (matcherTs.find()) {
			elggTs = matcherTs.group(1);
		}
	
		//zt TODO: pattern has changed. Fix it later. 
		//var elgg = {"config":{"lastcache":1554117145,"viewtype":"default","simplecache_enabled":1},"security":{"token":{"__elgg_ts":1555252434,"__elgg_token":"wyC-FNIuTj_65i-ZAEvy0g"}},"session":{"user":null,"token":"yZ4zHdtVu62MRRHJm2Dbo0"},"_data":{}}
		if (updateGUID) {
			// Get the Json
			int startIndex = sb.indexOf("var elgg = ");
			int endIndex = sb.indexOf(";", startIndex);
			String elggJson = sb.substring(startIndex + "var elgg = ".length(),
					endIndex);
	
			JSONObject elgg = new JSONObject(elggJson);
			if (!elgg.getJSONObject("session").isNull("user")) {
				JSONObject userSession = elgg.getJSONObject("session")
						.getJSONObject("user");
				Integer elggGuid = userSession.getInt("guid");
					client.setGuid(elggGuid.toString());
			}
		}
		
		logger.finer("Elgg Token = "+elggToken+" Elgg Ts = "+elggTs);
		
		if (null != elggToken) {
			client.setElggToken(elggToken);
		}
		
		if (null != elggTs) {
			client.setElggTs(elggTs);
		}
	}

	private void updateNumActivities(Web20Client client, StringBuilder sb) {
		int startIndex = sb.indexOf("var numactivities = ")+"var numactivities = ".length();
		int endIndex = sb.indexOf(";", startIndex);
		client.setNumActivities(sb.substring(startIndex, endIndex));
		
	}
	
	@BenchmarkOperation(name = "BrowsetoElgg", 
						percentileLimits= {30},
						timing = Timing.MANUAL)
	/**
	 * A new client accesses the home page. The "new client" is selected from a list maintained of possible users and their passwords.
	 * The details of the new client are stored in the 
	 * @throws Exception
	 */
	public void browseToElgg() throws Exception {
		boolean success = false;
		if (!inited) 
			logger.info("Inited thread" + context.getThreadId());
		inited = true;
		logger.info(context.getThreadId() +" : Doing operation: browsetoelgg");
		context.recordTime();
		
		if (thisClient.getClientState() == ClientState.LOGGED_OUT) {

			thisClient.setGuid(thisUserPasswordPair.getGuid());
			thisClient.setUsername(thisUserPasswordPair.getUserName());
			thisClient.setPassword(thisUserPasswordPair.getPassword());
			thisClient.setLoggedIn(false);

			logger.info("Logging in: "+thisClient.getUsername());
			
			HttpTransport http = HttpTransport.newInstance();
			http.addTextType("application/xhtml+xml");
			http.addTextType("application/xml");
			http.addTextType("q=0.9,*/*");
			http.addTextType("q=0.8");
			http.setFollowRedirects(true);
	
			thisClient.setHttp(http);
			StringBuilder sb = http.fetchURL(hostUrl + ROOT_URL);
			
			updateElggTokenAndTs(thisClient, sb, false);
			updateNumActivities(thisClient, sb);
			printErrorMessageIfAny(sb, null);

			for (String url : ROOT_URLS) {
				http.readURL(hostUrl + url);
			}

		}
		context.recordTime();
			elggMetrics.attemptHomePageCnt++;		

	}

@BenchmarkOperation(name = "AccessHomepage", 
						percentileLimits= {50},
						timing = Timing.MANUAL)
	/**
	 * A logged in client accesses the home page
	 * @throws Exception
	 */
	public void accessHomePage() throws Exception {
		boolean success = false;
		logger.fine(context.getThreadId()
				+ " : Doing operation: accessHomePage");
		context.recordTime();

		StringBuilder sb = thisClient.getHttp().fetchURL(hostUrl + ROOT_URL);

		updateElggTokenAndTs(thisClient, sb, false);
		updateNumActivities(thisClient, sb);

		printErrorMessageIfAny(sb, null);
		for (String url : ROOT_URLS) {
			thisClient.getHttp().readURL(hostUrl + url);
		}

		context.recordTime();
			elggMetrics.attemptHomePageCnt++;		

	}

	@BenchmarkOperation(name = "DoLogin", 
						percentileLimits= {50}, 
						timing = Timing.MANUAL)
	public void doLogin() throws Exception {
		boolean success = false;
		long loginStart = 0, loginEnd = 0;
		context.recordTime();
		logger.fine(context.getThreadId() + " : Doing operation: doLogin with"
				+ thisClient.getUsername());

		/*
		 * To do the login, To login, we need four parameters in the POST query
		 * 1. Elgg token 2. Elgg timestamp 3. user name 4. password
		 */
		String postRequest = "__elgg_token=" + thisClient.getElggToken()
				+ "&__elgg_ts=" + thisClient.getElggTs() + "&username="
				+ thisClient.getUsername() + "&password="
				+ thisClient.getPassword();

		for (String url : LOGIN_URLS) {
			thisClient.getHttp().readURL(hostUrl + url);
		}

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		headers.put("Accept-Language", "en-US,en;q=0.5");
		headers.put("Accept-Encoding", "gzip, deflate");
		// headers.put("Referer", hostUrl + "/");
		headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36"); 
		headers.put("Content-Type", "application/x-www-form-urlencoded");

		StringBuilder sb = thisClient.getHttp().fetchURL(hostUrl + LOGIN_URL,
				postRequest, headers);

		updateElggTokenAndTs(thisClient, sb, true);
		printErrorMessageIfAny(sb, postRequest);


		if (sb.toString().contains("You have been logged in")) {
			logger.fine("Successfully logged in: "+thisClient.getUsername());
      System.out.print("PRINT: Successfully logged in\n");
		} else {
			logger.fine("!!!!!!!!!!!!!!!!!! Failed to log in :"+thisClient.getUsername()+"!!!!!!!!!!!!!!!!!!!!!");
			System.out.print("PRINT: Failed to login!\n");
      throw new RuntimeException(sb.toString());
		}
		thisClient.setLoggedIn(true);
		thisClient.setClientState(ClientState.LOGGED_IN);
		success = true;

		context.recordTime();

		if (success)
			elggMetrics.attemptLoginCnt++;
	}

	@BenchmarkOperation(name = "UpdateActivity", percentileLimits= {50}, timing = Timing.MANUAL)
	public void updateActivity() throws Exception {
		boolean success = false;

		context.recordTime();
		if (thisClient.getClientState() == ClientState.LOGGED_IN) {
			logger.fine(context.getThreadId() +" : Doing operation: updateActivity");
			
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			headers.put("Accept-Language", "en-US,en;q=0.5");
			headers.put("Accept-Encoding", "gzip, deflate");
			// headers.put("Referer", hostUrl + "/activity");
			headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36"); 
			headers.put("Content-Type", "application/x-www-form-urlencoded");


			String postString = "options%5Bcount%5D=false&options%5Bpagination%5D=false&options%5Boffset%5D=0&options%5Blimit%5D=5&count="+thisClient.getNumActivities(); 
			// Note: the %5B %5D are [ and ] respectively.
			// #TODO: Fix the count value.
			StringBuilder sb = thisClient.getHttp().fetchURL(
					hostUrl + RIVER_UPDATE_URL, postString, headers);
			if (sb.toString().contains("Sorry, you cannot perform this action while logged out.")) {
				logger.fine("update activity: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!User logged out!!");
			}
			printErrorMessageIfAny(sb, postString);

			success = true;
		}
		context.recordTime();
		if (success) {
			elggMetrics.attemptUpdateActivityCnt++;
		}
	}

	/**
	 * Add friend
	 * 
	 * @throws Exception
	 */
	@BenchmarkOperation(name = "AddFriend", 
						percentileLimits= {50},
						timing = Timing.MANUAL)
	public void addFriend() throws Exception {
		boolean success = false;
		StringBuilder sb = null;
		context.recordTime();
		if (thisClient.getClientState() == ClientState.LOGGED_IN) {
			logger.fine(context.getThreadId() +" : Doing operation: addFriend");

			UserPasswordPair user = getRandomUser();
			String friendeeGuid = user.getGuid();
			String queryString = "friend=" + friendeeGuid + "&__elgg_ts="
					+ thisClient.getElggTs() + "&__elgg_token="
					+ thisClient.getElggToken();
			String postString = "__elgg_ts="
					+ thisClient.getElggTs() + "&__elgg_token="
					+ thisClient.getElggToken();
			
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			headers.put("Accept-Language", "en-US,en;q=0.5");
			headers.put("Accept-Encoding", "gzip, deflate");
			// headers.put("Referer", hostUrl + "/profile/"+user.getUserName());
			headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36"); 
			headers.put("Content-Type", "application/x-www-form-urlencoded");
			headers.put("X-Requested-With", "XMLHttpRequest");


			sb = thisClient.getHttp().fetchURL(
					hostUrl + DO_ADD_FRIEND + "?" + queryString, postString, headers);

			if (sb.toString().contains("Sorry, you cannot perform this action while logged out.")) {
				logger.fine("addFriend:!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!User logged out!!");
			}
			printErrorMessageIfAny(sb, postString);
			success = true;
		}
		context.recordTime();

		if (success) {
					elggMetrics.attemptAddFriendsCnt++;
		}/* else {
			if (thisClient.getClientState() == ClientState.AT_HOME_PAGE) {
				doLogin();
			} else if (thisClient.getClientState() == ClientState.LOGGED_OUT) {
				accessHomePage();
				doLogin();
			}
		}*/

	}

	/**
	 * Post something on the Wall (actually on the Wire but from the Wall!).
	 * 
	 * @throws Exception
	 */
	@BenchmarkOperation(name = "PostSelfWall", 
						percentileLimits= {50},
						timing = Timing.MANUAL)
	public void postSelfWall() throws Exception {
		boolean success = false;

		context.recordTime();
		if (thisClient.getClientState() == ClientState.LOGGED_IN) {
			logger.fine(context.getThreadId()+context.getThreadId() +" : Doing operation: post wall");

			String status = "Hello world! "
					+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
							.format(new Date());
			String postRequest = "__elgg_token=" + thisClient.getElggToken()
					+ "&__elgg_ts=" + thisClient.getElggTs() + "&status=" + status
					+ "&address=&access_id=-2&origin=wall&container_guid="
					+ thisClient.getGuid()+"&X-Requested-With=XMLHttpRequest&river=true&widget=0";	
			//&X-Requested-With=XMLHttpRequest&container_guid=43&river=true&widget=0 
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			headers.put("Accept-Language", "en-US,en;q=0.5");
			headers.put("Accept-Encoding", "gzip, deflate");
			// headers.put("Referer", hostUrl + "/activity");
			headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36"); 
			headers.put("Content-Type", "application/x-www-form-urlencoded");

	
			StringBuilder sb = thisClient.getHttp().fetchURL(hostUrl + WALL_URL,
					postRequest, headers);
			printErrorMessageIfAny(sb, postRequest);
			updateElggTokenAndTs(thisClient, sb, false);
			success = true;
		}
		context.recordTime();

		if (success) {
			elggMetrics.attemptPostWallCnt++;
		} /*else {
			if (thisClient.getClientState() == ClientState.AT_HOME_PAGE) {
				doLogin();
			} else if (thisClient.getClientState() == ClientState.LOGGED_OUT) {
				accessHomePage();
				doLogin();
			}
		}	*/	

	}

	/**
	 * Post something on the Wall (actually on the Wire but from the Wall!).
	 * 
	 * @throws Exception
	 */
	@BenchmarkOperation(name = "Logout", 
						percentileLimits= {50},
						timing = Timing.MANUAL)
	public void logout() throws Exception {
		boolean success = false;

		context.recordTime();
		if (thisClient.getClientState() == ClientState.LOGGED_IN) {
			logger.fine(context.getThreadId() +" : Doing operation: logout");

			Map<String, String> headers = new HashMap<String, String>();
			headers.put("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			headers.put("Accept-Language", "en-US,en;q=0.5");
			headers.put("Accept-Encoding", "gzip, deflate");
			// headers.put("Referer", hostUrl + "/activity");
			// headers.put("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:33.0) Gecko/20100101 Firefox/33.0");
			headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36"); 
	
			StringBuilder sb = thisClient.getHttp().fetchURL(hostUrl + LOGOUT_URL
					+"?__elgg_ts="+thisClient.getElggTs()+"&__elgg_token="+thisClient.getElggToken(), headers);
			printErrorMessageIfAny(sb, null);
			//System.out.println(sb);
			updateElggTokenAndTs(thisClient, sb, false);
			thisClient.setClientState(ClientState.LOGGED_OUT);
			thisClient.setLoggedIn(false);
			success = true;

		}
		context.recordTime();

		if (success) {
			elggMetrics.attemptLogoutCnt++;
		} /*else {
			if (thisClient.getClientState() == ClientState.AT_HOME_PAGE) {
				doLogin();
			} else if (thisClient.getClientState() == ClientState.LOGGED_OUT) {
				accessHomePage();
				doLogin();
			}
		}*/		

	}

	/**
	 * 
	 * Register a new user.
	 * 
	 */
	@BenchmarkOperation(name = "Register", 
						percentileLimits= {30},
						timing = Timing.MANUAL)
	public void register() throws Exception {
		boolean success = false;

		Web20Client tempClient = new Web20Client();
		HttpTransport http;
		
		context.recordTime();

		if (thisClient.getClientState() == ClientState.LOGGED_OUT) {
			http = HttpTransport.newInstance();
			tempClient.setHttp(http);

			logger.info(context.getThreadId() +" : Doing operation: register");

			// Navigate to the home page
	
			StringBuilder sb = tempClient.getHttp().fetchURL(hostUrl + ROOT_URL);
	
			updateElggTokenAndTs(tempClient, sb, false);
			for (String url : ROOT_URLS) {
				tempClient.getHttp().readURL(hostUrl + url);
			}
	
	
			// Click on Register link and generate user name and password
	
			tempClient.getHttp().fetchURL(hostUrl + REGISTER_PAGE_URL);
			String userName = RandomStringGenerator.generateRandomString(10,
					RandomStringGenerator.Mode.ALPHA);
			String password = RandomStringGenerator.generateRandomString(10,
					RandomStringGenerator.Mode.ALPHA);
			String email = RandomStringGenerator.generateRandomString(7,
					RandomStringGenerator.Mode.ALPHA)
					+ "@"
					+ RandomStringGenerator.generateRandomString(5,
							RandomStringGenerator.Mode.ALPHA) + ".co.in";
			tempClient.setUsername(userName);
			tempClient.setPassword(password);
			tempClient.setEmail(email);
	
			String postString = "__elgg_token=" + tempClient.getElggToken()
					+ "&__elgg_ts=" + tempClient.getElggTs() + "&name="
					+ tempClient.getUsername() + "&email=" + tempClient.getEmail()
					+ "&username=" + tempClient.getUsername() + "&password="
					+ tempClient.getPassword() + "&password2=" + tempClient.getPassword()
					+ "&friend_guid=0+&invitecode=&submit=Register";
			// __elgg_token=0c3a778d2b74a7e7faf63a6ba55d4832&__elgg_ts=1434992983&name=display_name&email=tapti.palit%40gmail.com&username=user_name&password=pass_word&password2=pass_word&friend_guid=0&invitecode=&submit=Register
	
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			headers.put("Accept-Language", "en-US,en;q=0.5");
			headers.put("Accept-Encoding", "gzip, deflate");
			// headers.put("Referer", hostUrl + "/register");
			// headers.put("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:33.0) Gecko/20100101 Firefox/33.0");
			headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36"); 
			headers.put("Content-Type", "application/x-www-form-urlencoded");
	
			sb = tempClient.getHttp().fetchURL(hostUrl + DO_REGISTER_URL, postString,
					headers);
			printErrorMessageIfAny(sb, postString);
			// System.out.println(sb);
		}
		context.recordTime();
		if (success) 
			elggMetrics.attemptRegisterCnt++;		

	}
	
	private void printErrorMessageIfAny(StringBuilder sb, String postRequest) {
		String htmlContent = sb.toString();
		//tem.out.println(htmlContent);
		String startTag = "<li class=\"elgg-message elgg-state-error\">";
		String endTag = "</li>";
		if (htmlContent.contains("elgg-system-messages")) {
			if (htmlContent.contains(startTag)) {
				int fromIndex = htmlContent.indexOf(startTag)+startTag.length();
				int toIndex = htmlContent.indexOf(endTag, fromIndex);
				String error = htmlContent.substring(fromIndex, toIndex);
				if (!error.trim().isEmpty()) {
					logger.info("Thread id: "+context.getThreadId()+" User: "+thisClient.getUsername()+" logged in status: "+thisClient.isLoggedIn()+"\nError: "+error+"Post request was: "+postRequest);
					throw new RuntimeException("Error happened");
				}
			}
		}
	}

	static class ElggDriverMetrics implements CustomMetrics {
		
		int attemptLoginCnt = 0;
		int attemptHomePageCnt = 0;
		int attemptPostWallCnt = 0;
		int attemptUpdateActivityCnt = 0;
		int attemptAddFriendsCnt = 0;
		int attemptLogoutCnt = 0;
		int attemptRegisterCnt = 0;

		@Override
		public void add(CustomMetrics arg0) {
			ElggDriverMetrics e = (ElggDriverMetrics) arg0;
			this.attemptHomePageCnt += e.attemptHomePageCnt;
			this.attemptLoginCnt += e.attemptLoginCnt;
			this.attemptPostWallCnt += e.attemptPostWallCnt;
			this.attemptUpdateActivityCnt += e.attemptUpdateActivityCnt;
			this.attemptAddFriendsCnt += e.attemptAddFriendsCnt;
			this.attemptLogoutCnt += e.attemptLogoutCnt;
			this.attemptRegisterCnt += e.attemptRegisterCnt;
		}

		@Override
		public Element[] getResults() {
			Element[] el = new Element[7];
			el[0] = new Element();
			el[0].description = "Number of times home page was actually attempted to be accessed.";
			el[0].passed = true;
			el[0].result = "" + this.attemptHomePageCnt;
			el[1] = new Element();
			el[1].description = "Number of times login was actually attempted.";
			el[1].passed = true;
			el[1].result = "" + this.attemptLoginCnt;
			el[2] = new Element();
			el[2].description = "Number of times posting on wall was actually attempted.";
			el[2].passed = true;
			el[2].result = "" + this.attemptPostWallCnt;
			el[3] = new Element();
			el[3].description = "Number of times update activity was actually attempted.";
			el[3].passed = true;
			el[3].result = "" + this.attemptUpdateActivityCnt;
			el[4] = new Element();
			el[4].description = "Number of times add friends was actually attempted.";
			el[4].passed = true;
			el[4].result = "" + this.attemptAddFriendsCnt;
			el[5] = new Element();
			el[5].description = "Number of times logout was actually attempted.";
			el[5].passed = true;
			el[5].result = "" + this.attemptLogoutCnt;
			el[6] = new Element();
			el[6].description = "Number of times register was actually attempted.";
			el[6].passed = true;
			el[6].result = "" + this.attemptRegisterCnt;
			return el;
		}

		public Object clone() {
			ElggDriverMetrics clone = new ElggDriverMetrics();
			clone.attemptHomePageCnt = this.attemptHomePageCnt;
			clone.attemptLoginCnt = this.attemptLoginCnt;
			clone.attemptPostWallCnt = this.attemptPostWallCnt;
			clone.attemptUpdateActivityCnt = this.attemptUpdateActivityCnt;
			clone.attemptAddFriendsCnt = this.attemptAddFriendsCnt;
			clone.attemptLogoutCnt = this.attemptLogoutCnt;
			clone.attemptRegisterCnt = this.attemptRegisterCnt;
			return clone;
		}
	}

	public static void main(String[] pp) throws Exception {
		Web20Driver driver = new Web20Driver();
		for (int i= 0; i<1; i++) {
			driver.browseToElgg();
			long start = System.currentTimeMillis();
			driver.doLogin();
			long end = System.currentTimeMillis();
			System.out.println("RUN\t"+i+"\t"+(end-start));
			System.out.println("Doing add friend ....................................");
			driver.addFriend();
			System.out.println("Doing post wall ............................");
			driver.postSelfWall();
			System.out.println("Doing logout ................................");
			driver.logout();
			System.out.println("Doing register ...........................");
			driver.register();
			
		}
	}
}

