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

public class AuthorizeFacebook {

    private static final String CLIENT_ID = "1288537665089287";
    private static final String CLIENT_SECRET = "bf918d844197cc21322f4835fe8a47ad";
    private static final String REDIRECT = "https://oauth.pstmn.io/v1/callback";
    private final User user;
    RetrieveLogin listener;

    public AuthorizeFacebook(RetrieveLogin listener) {
        this.listener = listener;
        user = new User();
    }

    public WebView getWebView() {
        String url = "https://www.facebook.com/v17.0/dialog/oauth?client_id=" + CLIENT_ID + "&redirect_uri=" + REDIRECT;
        WebView root = new WebView();
        WebEngine engine = root.getEngine();
        engine.load(url);

        engine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            LogManager.getLogger().info(newValue);
            String location = engine.getLocation();
            if (newValue == Worker.State.SUCCEEDED) {
                String code = location.substring(location.indexOf("code=") + 5);
                getAccessToken(code);
            }
            LogManager.getLogger().info("Loc " + location);
        });

        engine.setUserStyleSheetLocation("data:,body{zoom:0.85}");
        return root;
    }

    private void getAccessToken(String code) {
        APIInterface apiInterface = APIClient.getFacebookServer().create(APIInterface.class);
        Call<JsonNode> call = apiInterface.getFacebookAccessToken(
                CLIENT_ID,
                CLIENT_SECRET,
                REDIRECT,
                code
        );

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NotNull Call<JsonNode> call, @NotNull Response<JsonNode> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonNode resp = response.body();
                    String accessToken = resp.get("access_token").asText();
                    LogManager.getLogger().info("Token " + accessToken);
                    getFacebookData(accessToken);
                }
            }

            @Override
            public void onFailure(@NotNull Call<JsonNode> call, @NotNull Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void getFacebookData(String accessToken) {
        LogManager.getLogger().info("Token " + accessToken);

        String fields = "id,name,email,picture.width(240).height(240)";
        APIInterface apiInterface = APIClient.getFacebookServer().create(APIInterface.class);
        Call<JsonNode> call = apiInterface.getFacebookData(accessToken, fields);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NotNull Call<JsonNode> call, @NotNull Response<JsonNode> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonNode resp = response.body();
                    String name = resp.get("name").asText();
                    String picture = resp.get("picture").get("data").get("url").asText();
                    user.setUsername(name);
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
