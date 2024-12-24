/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2023-2024 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.authentication.oauth2;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.HttpClientManager;
import org.openbravo.cache.TimeInvalidatedCache;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

/**
 * Provides information about the user making an authentication request from the value of an OAuth
 * 2.0 token in the JSON Web Token (JWT) format.
 */
@ApplicationScoped
class JWTDataProvider {
  private static final Logger log = LogManager.getLogger();

  @Inject
  private HttpClientManager httpClientManager;

  private TimeInvalidatedCache<String, Map<String, RSAPublicKey>> rsaPublicKeys = TimeInvalidatedCache
      .newBuilder()
      .name("OAuth 2.0 RSA Public Keys")
      .expireAfterDuration(Duration.ofMinutes(10))
      .build(this::requestCertificateData);

  private Map<String, RSAPublicKey> requestCertificateData(String url) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Content-Type", "application/json")
          .timeout(Duration.ofSeconds(30))
          .GET()
          .build();
      HttpResponse<String> response = httpClientManager.send(request);
      return getRSAPublicKeys(response.body());
    } catch (Exception ex) {
      log.error("Error requesting keys to {}", url);
      return null;
    }
  }

  private Map<String, RSAPublicKey> getRSAPublicKeys(String certificateData)
      throws InvalidKeySpecException, NoSuchAlgorithmException, JSONException,
      CertificateException {
    JSONObject certificates = new JSONObject(certificateData);
    if (certificates.has("keys")) {
      // JSON Web Key (JWK) certificate
      JSONArray keys = certificates.getJSONArray("keys");
      Map<String, RSAPublicKey> publicKeys = new HashMap<>(keys.length());
      for (int i = 0; i < keys.length(); i += 1) {
        JSONObject key = keys.getJSONObject(i);
        String keyId = key.getString("kid");
        byte[] modulusBytes = Base64.decodeBase64(key.getString("n"));
        byte[] exponentBytes = Base64.decodeBase64(key.getString("e"));
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(new BigInteger(1, modulusBytes),
            new BigInteger(1, exponentBytes));
        RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
            .generatePublic(keySpec);
        publicKeys.put(keyId, publicKey);
      }
      return publicKeys;
    }
    // X.509 certificate in ASCII PEM format
    @SuppressWarnings("unchecked")
    Iterator<String> keysIterator = certificates.keys();
    Map<String, RSAPublicKey> publicKeys = new HashMap<>();
    while (keysIterator.hasNext()) {
      String keyId = keysIterator.next();
      String pem = certificates.getString(keyId);
      pem = pem.replace("-----BEGIN CERTIFICATE-----", "")
          .replace("-----END CERTIFICATE-----", "")
          .replaceAll("\\s", "");
      byte[] encoded = Base64.decodeBase64(pem);
      X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
          .generateCertificate(new ByteArrayInputStream(encoded));
      publicKeys.put(keyId, (RSAPublicKey) certificate.getPublicKey());
    }
    return publicKeys;
  }

  /**
   * Extracts the authentication information from the given OAuth2 2.0 token. For the moment the
   * returned map just contains the user identifier.
   *
   * @param token
   *          An OAuth2 2.0 token
   * @param certificateURL
   *          the URL to get the public keys required by the algorithm used for encrypting the token
   *          data.
   * @param userIdentifier
   *          property used to map the user to make the authentication
   *
   * @return the authentication information extracted from the given OAuth 2.0 token
   *
   * @throws OAuth2TokenVerificationException
   *           if it is not possible to verify the token or extract the authentication data
   */
  Map<String, Object> getData(String token, String certificateURL, String userIdentifier)
      throws OAuth2TokenVerificationException {
    try {
      DecodedJWT decodedJWT = JWT.decode(token);
      Algorithm algorithm = getAlgorithm(decodedJWT, certificateURL);
      JWTVerifier verifier = JWT.require(algorithm).build();
      DecodedJWT verifiedJWT = verifier.verify(token);
      Map<String, Claim> claims = verifiedJWT.getClaims();
      if (claims.containsKey(userIdentifier)) {
        return Map.of(userIdentifier, claims.get(userIdentifier).asString());
      }
      return Collections.emptyMap();
    } catch (NoSuchAlgorithmException | TokenExpiredException | JWTDecodeException ex) {
      throw new OAuth2TokenVerificationException("Could not retrieve data from OAuth2 token", ex);
    }
  }

  private Algorithm getAlgorithm(DecodedJWT decodedJWT, String certificateURL)
      throws NoSuchAlgorithmException, OAuth2TokenVerificationException {
    String algorithm = decodedJWT.getAlgorithm();
    String keyId = decodedJWT.getKeyId();
    if ("RS256".equals(algorithm)) {
      RSAPublicKey publicKey = rsaPublicKeys.get(certificateURL).get(keyId);
      if (publicKey == null) {
        throw new OAuth2TokenVerificationException(
            "Error getting the RSA public key from " + certificateURL);
      }
      return Algorithm.RSA256(publicKey);
    }
    throw new NoSuchAlgorithmException("Unsupported algorithm: " + algorithm);
  }

  /**
   * Invalidates the cache of public keys
   *
   * @param url
   *          The URL where the public keys to remove from cache are obtained
   */
  void invalidateCache(String url) {
    rsaPublicKeys.invalidate(url);
  }
}
