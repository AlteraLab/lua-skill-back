package chatbot.api.user.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class KakaoAuthCodeInfo {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token-type")
    private String tokenType;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("expires_in")
    private int expiresIn;

    private String scope;
}