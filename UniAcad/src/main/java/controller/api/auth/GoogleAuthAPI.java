package controller.api.auth;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import com.google.gson.Gson;

/**
 * GoogleAuthServlet for Google OAuth2 login to retrieve user email and full name.
 * Redirects to Google for authentication and stores email and full name in session.
 * Returns JSON responses for compatibility with login.html.
 */
@WebServlet("/api/google-auth")
public class GoogleAuthAPI extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthAPI.class);
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo";
    private AuthorizationCodeFlow flow;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        try {
            Properties oauthProps = new Properties();
            try (InputStream input = getClass().getClassLoader().getResourceAsStream("oauth.properties")) {
                if (input == null) {
                    throw new IOException("Unable to find oauth.properties");
                }
                oauthProps.load(input);
            }

            String clientId = oauthProps.getProperty("oauth.client_id");
            String clientSecret = oauthProps.getProperty("oauth.client_secret");
            String scope = oauthProps.getProperty("oauth.google_scope");

            GoogleClientSecrets.Details web = new GoogleClientSecrets.Details();
            web.setClientId(clientId);
            web.setClientSecret(clientSecret);
            GoogleClientSecrets clientSecrets = new GoogleClientSecrets().setWeb(web);

            flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets,
                    Arrays.asList(scope.split(" ")))
                    .setDataStoreFactory(new MemoryDataStoreFactory())
                    .build();
            gson = new Gson();
        } catch (IOException e) {
            throw new ServletException("Failed to initialize Google OAuth flow", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String code = request.getParameter("code");
        if (code == null) {
            // Step 1: Redirect to Google OAuth
            try {
                AuthorizationCodeRequestUrl authUrl = flow.newAuthorizationUrl()
                        .setRedirectUri(getRedirectUri(request));
                logger.info("Redirecting to Google OAuth: {}", authUrl.build());
                response.sendRedirect(authUrl.build());
            } catch (Exception e) {
                logger.error("Error redirecting to Google OAuth: {}", e.getMessage(), e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println(gson.toJson(createErrorResponse("Failed to initiate Google authentication")));
            }
            return;
        }

        // Step 2: Process callback from Google
        try {
            TokenResponse tokenResponse = flow.newTokenRequest(code)
                    .setRedirectUri(getRedirectUri(request))
                    .execute();

            String accessToken = tokenResponse.getAccessToken();
            GenericUrl url = new GenericUrl(USERINFO_ENDPOINT);
            com.google.api.client.http.HttpRequest userInfoRequest =
                    HTTP_TRANSPORT.createRequestFactory().buildGetRequest(url);
            userInfoRequest.getHeaders().setAuthorization("Bearer " + accessToken);
            com.google.api.client.http.HttpResponse userInfoResponse = userInfoRequest.execute();

            String userInfoJson = userInfoResponse.parseAsString();
            com.google.gson.JsonObject userInfo = gson.fromJson(userInfoJson, com.google.gson.JsonObject.class);
            String email = userInfo.get("email").getAsString();
            String fullName = userInfo.get("name") != null ? userInfo.get("name").getAsString() : "Unknown";

            // Store email and full name in session
            HttpSession session = request.getSession();
            session.setAttribute("email", email);
            session.setAttribute("full_name", fullName);
            logger.info("User logged in with email: {}, full name: {}", email, fullName);

            response.sendRedirect("/student/homepage");

        } catch (Exception e) {
            logger.error("Google OAuth error: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println(gson.toJson(createErrorResponse("Google authentication failed: " + e.getMessage())));
        } finally {
            out.flush();
        }
    }

    private String getRedirectUri(HttpServletRequest request) {
        Properties oauthProps = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("oauth.properties")) {
            if (input == null) {
                throw new IOException("Unable to find oauth.properties");
            }
            oauthProps.load(input);
        } catch (IOException e) {
            logger.error("Failed to load oauth.properties: {}", e.getMessage(), e);
        }
        String redirectUri = oauthProps.getProperty("oauth.google_redirect_uri");
        // Ensure redirect URI matches the request context
        return redirectUri != null ? redirectUri : request.getScheme() + "://" +
                request.getServerName() + ":" + request.getServerPort() +
                request.getContextPath() + "/google-auth";
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("errorMessage", message);
        return error;
    }
}