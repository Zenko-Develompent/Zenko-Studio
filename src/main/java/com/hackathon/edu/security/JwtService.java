package com.hackathon.edu.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.edu.config.AppSecurityProperties;
import com.hackathon.edu.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Base64.Encoder B64U = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64UD = Base64.getUrlDecoder();

    private final byte[] key;
    private final long ttlSec;

    public JwtService(AppSecurityProperties props) {
        this.key = Base64.getDecoder().decode(props.getMasterKeyB64());
        if (key.length != 32) {
            throw new IllegalStateException("app.security.master-key-b64 must decode to 32 bytes");
        }
        this.ttlSec = props.getAccessTtlSec();
    }

    public String issue(UUID userId, UUID refreshId) {
        try {
            long iat = Instant.now().getEpochSecond();
            long exp = iat + ttlSec;
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload = Map.of(
                    "sub", userId.toString(),
                    "iat", iat,
                    "exp", exp,
                    "rid", refreshId.toString()
            );
            String h = B64U.encodeToString(MAPPER.writeValueAsBytes(header));
            String p = B64U.encodeToString(MAPPER.writeValueAsBytes(payload));
            String sig = sign(h + "." + p);
            return h + "." + p + "." + sig;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Claims verify(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length != 3) {
                throw unauthorized("invalid_access_token");
            }
            String expected = sign(parts[0] + "." + parts[1]);
            if (!constantEq(expected, parts[2])) {
                throw unauthorized("invalid_access_token");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = MAPPER.readValue(B64UD.decode(parts[1]), Map.class);
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= exp) {
                throw unauthorized("access_token_expired");
            }
            UUID userId = UUID.fromString((String) payload.get("sub"));
            UUID refreshId = UUID.fromString((String) payload.get("rid"));
            return new Claims(userId, refreshId, exp);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception e) {
            throw unauthorized("invalid_access_token");
        }
    }

    private ApiException unauthorized(String error) {
        return new ApiException(HttpStatus.UNAUTHORIZED, error);
    }

    private String sign(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return B64U.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private static boolean constantEq(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }

    public record Claims(UUID userId, UUID refreshId, long expEpochSec) {
    }
}
