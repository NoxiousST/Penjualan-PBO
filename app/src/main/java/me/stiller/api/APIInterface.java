package me.stiller.api;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface APIInterface {

    @FormUrlEncoded
    @POST("/oauth2/v4/token")
    Call<JsonNode> getGoogleAccessToken(
            @Field("code") String code,
            @Field("client_id") String clientId,
            @Field("client_secret") String clientSecret,
            @Field("redirect_uri") String redirectUri,
            @Field("grant_type") String grantType
    );

    @GET("/oauth2/v3/userinfo")
    Call<JsonNode> getGoogleData(@Query("access_token") String accessToken);

    @FormUrlEncoded
    @POST("/v17.0/oauth/access_token")
    Call<JsonNode> getFacebookAccessToken(
            @Field("client_id") String clientId,
            @Field("client_secret") String clientSecret,
            @Field("redirect_uri") String redirectUri,
            @Field("code") String code
    );

    @GET("/v17.0/me")
    Call<JsonNode> getFacebookData(
            @Query("access_token") String accessToken,
            @Query("fields") String fields
    );

    @FormUrlEncoded
    @POST("/login/oauth/access_token")
    Call<ResponseBody> getGithubAccessToken(
            @Field("client_id") String clientId,
            @Field("client_secret") String clientSecret,
            @Field("redirect_uri") String redirectUri,
            @Field("code") String code,
            @Field("grant_type") String grantType
    );

    @GET("/user")
    Call<JsonNode> getGithubData();
}