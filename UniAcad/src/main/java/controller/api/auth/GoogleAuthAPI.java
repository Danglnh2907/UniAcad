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
import dao.StaffDAO;
import dao.StudentDAO;
import dao.TeacherDAO;
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
            Properties props = new Properties();
            try (InputStream input = getClass().getClassLoader().getResourceAsStream("oauth.properties")) {
                if (input == null) {
                    throw new IOException("Unable to find oauth.properties");
                }
                props.load(input);
            }

            String clientId = props.getProperty("oauth.client_id");
            String clientSecret = props.getProperty("oauth.client_secret");
            String scope = props.getProperty("oauth.google_scope");

            GoogleClientSecrets.Details web = new GoogleClientSecrets.Details();
            web.setClientId(clientId);
            web.setClientSecret(clientSecret);
            GoogleClientSecrets clientSecrets = new GoogleClientSecrets().setWeb(web);

            flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets,
                    Arrays.asList(scope.split(" "))
            ).setDataStoreFactory(new MemoryDataStoreFactory()).build();

            gson = new Gson();
        } catch (IOException e) {
            throw new ServletException("Failed to initialize Google OAuth", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String code = request.getParameter("code");

        if (code == null) {
            // Step 1: Redirect to Google Auth
            String dynamicRedirectUri = buildDynamicRedirectUri(request);
            response.sendRedirect(flow.newAuthorizationUrl().setRedirectUri(dynamicRedirectUri).build());
            return;
        }

        try {
            // Step 2: Process callback
            String dynamicRedirectUri = buildDynamicRedirectUri(request);

            TokenResponse tokenResponse = flow.newTokenRequest(code)
                    .setRedirectUri(dynamicRedirectUri)
                    .execute();

            String accessToken = tokenResponse.getAccessToken();

            com.google.api.client.http.HttpRequest userInfoRequest = HTTP_TRANSPORT.createRequestFactory()
                    .buildGetRequest(new GenericUrl(USERINFO_ENDPOINT));
            userInfoRequest.getHeaders().setAuthorization("Bearer " + accessToken);

            com.google.api.client.http.HttpResponse userInfoResponse = userInfoRequest.execute();
            String userInfoJson = userInfoResponse.parseAsString();
            com.google.gson.JsonObject userInfo = gson.fromJson(userInfoJson, com.google.gson.JsonObject.class);

            String email = userInfo.get("email").getAsString();
            String fullName = userInfo.has("name") ? userInfo.get("name").getAsString() : "Unknown";

            handleUserLogin(request.getSession(), email, fullName, response, request.getContextPath());

        } catch (Exception e) {
            logger.error("Google OAuth error", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(gson.toJson(Map.of("error", "Authentication failed")));
        } finally {
            out.flush();
        }
    }

    private String buildDynamicRedirectUri(HttpServletRequest request) {
        return request.getScheme() + "://" +
                request.getServerName() +
                ":" + request.getServerPort() +
                request.getContextPath() +
                "/api/google-auth";
    }

    private void handleUserLogin(HttpSession session, String email, String fullName, HttpServletResponse response, String contextPath) throws IOException {
        StudentDAO studentDAO = new StudentDAO();
        TeacherDAO teacherDAO = new TeacherDAO();
        StaffDAO staffDAO = new StaffDAO();

        if (staffDAO.getStaffByEmail(email) != null) {
            setupSession(session, email, fullName, "staff");
            response.sendRedirect(contextPath + "/staff/home");
        } else if (studentDAO.getStudentByEmail(email) != null) {
            setupSession(session, email, fullName, "student");
            response.sendRedirect(contextPath + "/student/home");
        } else if (teacherDAO.getTeacherByEmail(email) != null) {
            setupSession(session, email, fullName, "teacher");
            response.sendRedirect(contextPath + "/teacher/home");
        } else {
            logger.warn("Unauthorized email tried login: {}", email);
            response.sendRedirect(contextPath + "/");
        }
    }

    private void setupSession(HttpSession session, String email, String fullName, String role) {
        session.setAttribute("email", email);
        session.setAttribute("full_name", fullName);
        session.setAttribute("role", role);
        logger.info("Session initialized for email: {}, role: {}", email, role);
    }
}
