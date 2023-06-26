package me.stiller.api;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import me.stiller.data.models.User;
import me.stiller.interfaces.RetrieveLogin;
import okhttp3.ResponseBody;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class AuthorizeGithub {

    private static final String CLIENT_ID = "9e86da85dcda6a44fd1f";
    private static final String CLIENT_SECRET = "6bc07bbabe8eca419f6e8483c1b0d1fca7cabfd8";
    private static final String SCOPE = "user:email";
    private static final String REDIRECT = "https://oauth.pstmn.io/v1/callback";
    private final User user;
    private final RetrieveLogin listener;
    private boolean first = true;

    public AuthorizeGithub(RetrieveLogin listener) {
        this.listener = listener;
        user = new User();
    }

    public WebView getWebView() {
        String url = "https://github.com/login/oauth/authorize?client_id=" + CLIENT_ID + "&redirect_uri=" + REDIRECT + "&scope=" + SCOPE;
        WebView root = new WebView();
        WebEngine engine = root.getEngine();
        engine.load(url);

        engine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED) {
                String location = engine.getLocation();
                LogManager.getLogger().info(location);
                if (location.contains(REDIRECT)) {
                    int startIndex = location.indexOf("code=") + 5;
                    String code = location.substring(startIndex);
                    String decodedAuthCode = URLDecoder.decode(code, StandardCharsets.UTF_8);
                    LogManager.getLogger().info("AuthCode " + decodedAuthCode);
                    getAccessToken(decodedAuthCode);
                }
            }
        });

        engine.setUserStyleSheetLocation("data:,body{zoom:0.85}");
        return root;
    }

    private void getAccessToken(String code) {
        APIInterface apiInterface = APIClient.getGithubServer().create(APIInterface.class);
        Call<ResponseBody> call = apiInterface.getGithubAccessToken(
                CLIENT_ID,
                CLIENT_SECRET,
                REDIRECT,
                code,
                "authorization_code"
        );

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String resp = Objects.requireNonNull(response.body()).string();
                        int startIndex = resp.indexOf("=") + 1;
                        int endIndex = resp.indexOf("&");
                        String accessToken = resp.substring(startIndex, endIndex);
                        getGithubData(accessToken);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void getGithubData(String accessToken) {
        APIInterface apiInterface = APIClient.getGithubApiServer(accessToken).create(APIInterface.class);
        Call<JsonNode> call = apiInterface.getGithubData();

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NotNull Call<JsonNode> call, @NotNull Response<JsonNode> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonNode resp = response.body();
                    String name = resp.get("name").asText();
                    String email = resp.get("email").asText();
                    String picture = resp.get("avatar_url").asText();
                    user.setUsername(name);
                    if (!email.equals("null")) user.setEmail(email);
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
