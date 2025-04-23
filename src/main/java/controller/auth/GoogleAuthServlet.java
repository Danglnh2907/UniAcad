package controller.auth;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Simplified GoogleAuthServlet for Google OAuth2 login to retrieve user email and full name.
 * Redirects to Google for authentication and stores email and full name in session.
 */
@WebServlet("/google-auth")
public class GoogleAuthServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(GoogleAuthServlet.class.getName());
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo";

    private AuthorizationCodeFlow flow;

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
        } catch (IOException e) {
            throw new ServletException("Failed to initialize Google OAuth flow", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String code = request.getParameter("code");
        if (code == null) {
            // Step 1: Redirect to Google OAuth
            AuthorizationCodeRequestUrl authUrl = flow.newAuthorizationUrl()
                    .setRedirectUri(getRedirectUri());
            LOGGER.info("Redirecting to: " + authUrl.build());
            response.sendRedirect(authUrl.build());
            return;
        }

        // Step 2: Process callback from Google
        try {
            TokenResponse tokenResponse = flow.newTokenRequest(code)
                    .setRedirectUri(getRedirectUri())
                    .execute();

            String accessToken = tokenResponse.getAccessToken();
            GenericUrl url = new GenericUrl(USERINFO_ENDPOINT);
            com.google.api.client.http.HttpRequest userInfoRequest =
                    HTTP_TRANSPORT.createRequestFactory().buildGetRequest(url);
            userInfoRequest.getHeaders().setAuthorization("Bearer " + accessToken);
            com.google.api.client.http.HttpResponse userInfoResponse = userInfoRequest.execute();

            String userInfoJson = userInfoResponse.parseAsString();
            com.google.gson.Gson gson = new com.google.gson.Gson();
            com.google.gson.JsonObject userInfo = gson.fromJson(userInfoJson, com.google.gson.JsonObject.class);
            Logger.getLogger("GoogleAuthServlet").info("User info: " + userInfo);
            String email = userInfo.get("email").getAsString();
            String fullName = userInfo.get("name") != null ? userInfo.get("name").getAsString() : "Unknown";

            // Store email and full name in session
            HttpSession session = request.getSession();
            session.setAttribute("email", email);
            session.setAttribute("full_name", fullName);
            LOGGER.info("User logged in with email: " + email + ", full name: " + fullName);

            response.sendRedirect("account.jsp");
        } catch (Exception e) {
            LOGGER.severe("Google OAuth error: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Google authentication failed");
        }
    }

    private String getRedirectUri() {
        Properties oauthProps = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("oauth.properties")) {
            oauthProps.load(input);
        } catch (IOException e) {
            LOGGER.severe("Failed to load oauth.properties: " + e.getMessage());
        }
        return oauthProps.getProperty("oauth.google_redirect_uri");
    }
}