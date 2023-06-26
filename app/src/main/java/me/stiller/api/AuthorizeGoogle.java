package me.stiller.api;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import me.stiller.data.models.User;
import me.stiller.interfaces.RetrieveLogin;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class AuthorizeGoogle {

    private static final String CLIENT_ID = "892558772478-8mp0f7rbpjimu91eivja1matoku508uu.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "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";
    private static final String SCOPE = "https://www.googleapis.com/auth/userinfo.email%20https://www.googleapis.com/auth/userinfo.profile";
    private static final String REDIRECT = "https://oauth.pstmn.io/v1/callback";
    private final User user;
    private final RetrieveLogin listener;
    private boolean first = true;

    public AuthorizeGoogle(RetrieveLogin listener) {
        this.listener = listener;
        user = new User();
    }

    public WebView getWebView() {

        String url = "https://accounts.google.com/o/oauth2/v2/auth?scope=" + SCOPE + "&redirect_uri=" + REDIRECT + "&response_type=code&client_id=" + CLIENT_ID + "&access_type=offline";
        WebView root = new WebView();
        WebEngine engine = root.getEngine();
        engine.load(url);

        engine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {

            if (newValue == Worker.State.SUCCEEDED) {
                String location = engine.getLocation();
                LogManager.getLogger().info(location);
                if (location.contains("code=") && first) {
                    int startIndex = location.indexOf("code=") + 5;
                    int endIndex = location.indexOf("&", startIndex);
                    String code = location.substring(startIndex, endIndex);
                    String decodedAuthCode = URLDecoder.decode(code, StandardCharsets.UTF_8);
                    LogManager.getLogger().info("Code " + decodedAuthCode);
                    getAccessToken(decodedAuthCode);
                }
            }
        });

        engine.setUserStyleSheetLocation("data:,body{zoom:0.85}");
        return root;
    }

    private void getAccessToken(String code) {
        APIInterface apiInterface = APIClient.getGoogleServer().create(APIInterface.class);
        Call<JsonNode> call = apiInterface.getGoogleAccessToken(
                code,
                CLIENT_ID,
                CLIENT_SECRET,
                REDIRECT,
                "authorization_code"
        );

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NotNull Call<JsonNode> call, @NotNull Response<JsonNode> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonNode resp = response.body();
                    String accessToken = resp.get("access_token").asText();
                    getGoogleData(accessToken);
                }
            }

            @Override
            public void onFailure(@NotNull Call<JsonNode> call, @NotNull Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void getGoogleData(String accessToken) {
        APIInterface apiInterface = APIClient.getGoogleServer().create(APIInterface.class);
        Call<JsonNode> call = apiInterface.getGoogleData(accessToken);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NotNull Call<JsonNode> call, @NotNull Response<JsonNode> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonNode resp = response.body();
                    String name = resp.get("name").asText();
                    String email = resp.get("email").asText();
                    String picture = resp.get("picture").asText();
                    user.setUsername(name);
                    user.setEmail(email);
                    user.setImage(picture);
                    Platform.runLater(() -> listener.retrieveLogin(user));
                }
            }

            @Override
            public void onFailure(@NotNull Call<JsonNode> call, @NotNull Throwable t) {
                t.printStackTrace();
            }
        });
    }

}
