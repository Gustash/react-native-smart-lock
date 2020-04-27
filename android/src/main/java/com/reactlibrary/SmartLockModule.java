package com.reactlibrary;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResponse;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.android.gms.auth.api.credentials.CredentialsOptions;
import com.google.android.gms.auth.api.credentials.IdToken;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.List;

import static android.app.Activity.RESULT_OK;

public class SmartLockModule extends ReactContextBaseJavaModule {
    private static final int RC_SAVE = 1;
    private static final int RC_REQUEST = 2;

    private final CredentialsClient mCredentialsClient;
    private final ReactApplicationContext mReactContext;

    SmartLockModule(ReactApplicationContext reactContext) {
        super(reactContext);

        CredentialsOptions options = new CredentialsOptions.Builder().build();

        this.mCredentialsClient = Credentials.getClient(reactContext, options);
        this.mReactContext = reactContext;
    }

    @NonNull
    @Override
    public String getName() {
        return "SmartLock";
    }

    @ReactMethod
    public void disableAutoSignIn() {
        mCredentialsClient.disableAutoSignIn();
    }

    @ReactMethod
    public void delete(String id, Promise promise) {
        mCredentialsClient.delete(new Credential.Builder(id).build())
            .addOnCompleteListener(getOnDeleteCompleteListener(promise));
    }

    @ReactMethod
    public void request(@Nullable String serverClientId, Promise promise) {
        final CredentialRequest request = new CredentialRequest.Builder()
                .setServerClientId(serverClientId)
                .setPasswordLoginSupported(true)
                .build();

        mCredentialsClient.request(request)
                .addOnCompleteListener(getOnRequestCompleteListener(promise));
    }

    @ReactMethod
    public void save(ReadableMap args, Promise promise) {
        mCredentialsClient.save(extractCredential(args))
            .addOnCompleteListener(getOnSaveCompleteListener(promise));
    }

    private OnCompleteListener<Void> getOnDeleteCompleteListener(final Promise promise) {
        return new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    promise.resolve(null);
                    return;
                }

                Exception e = task.getException();

                if (e == null) {
                    promise.resolve("Error: Couldn't delete credentials.");
                    return;
                }

                promise.resolve("Error: " + e.getMessage());
            }
        };
    }

    private OnCompleteListener<Void> getOnSaveCompleteListener(final Promise promise) {
        mReactContext.addActivityEventListener(new BaseActivityEventListener() {
            @Override
            public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
                if (requestCode == RC_SAVE) {
                    if (resultCode == RESULT_OK) {
                        promise.resolve(null);
                    } else {
                        promise.resolve("Error: Failed to save credentials.");
                    }

                    mReactContext.removeActivityEventListener(this);
                }
            }
        });

        return new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    promise.resolve(null);
                    return;
                }

                final Exception e = task.getException();
                if (e instanceof ResolvableApiException) {
                    // Try to resolve the save request. This will prompt the user if
                    // the credential is new.
                    ResolvableApiException rae = (ResolvableApiException) e;
                    try {
                        final Activity activity = getCurrentActivity();

                        if (activity == null) {
                            promise.resolve("Error: Not attached to an activity.");
                            return;
                        }

                        rae.startResolutionForResult(getCurrentActivity(), RC_SAVE);
                    } catch (IntentSender.SendIntentException exception) {
                        // Could not resolve the request
                        promise.resolve("Error: " + exception.getMessage());
                    }
                } else if (e instanceof ApiException) {
                    final ApiException apiException = (ApiException) e;

                    if (apiException.getStatusCode() == Status.RESULT_CANCELED.getStatusCode()) {
                        promise.resolve("canceled");
                        return;
                    }

                    promise.resolve("Error: " + e.getMessage());
                } else {
                    // Request has no resolution
                    if (e == null) {
                        promise.resolve("Error: Failed saving credentials.");
                        return;
                    }

                    promise.resolve("Error: " + e.getMessage());
                }
            }
        };
    }

    private OnCompleteListener<CredentialRequestResponse> getOnRequestCompleteListener(final Promise promise) {
        mReactContext.addActivityEventListener(new BaseActivityEventListener() {
            @Override
            public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
                if (requestCode == RC_REQUEST) {
                    if (resultCode == RESULT_OK) {
                        Credential credential = intent.getParcelableExtra(Credential.EXTRA_KEY);
                        promise.resolve(createCredentialMap(credential));
                    } else {
                        promise.resolve(createErrorMap("Error: Failed to request credentials."));
                    }

                    mReactContext.removeActivityEventListener(this);
                }
            }
        });

        return new OnCompleteListener<CredentialRequestResponse>() {
            @Override
            public void onComplete(@NonNull Task<CredentialRequestResponse> task) {
                if (task.isSuccessful()) {
                    final CredentialRequestResponse result = task.getResult();
                    if (result == null) {
                        promise.resolve(createErrorMap("Error: Failed to get results."));
                        return;
                    }

                    final Credential credential = result.getCredential();
                    promise.resolve(createCredentialMap(credential));
                    return;
                }

                final Exception e = task.getException();

                if (e instanceof ResolvableApiException) {
                    // Try to resolve the save request. This will prompt the user if
                    // the credential is new.
                    ResolvableApiException rae = (ResolvableApiException) e;
                    try {
                        final Activity activity = getCurrentActivity();

                        if (activity == null) {
                            promise.resolve(createErrorMap("Error: Not attached to an activity."));
                            return;
                        }

                        rae.startResolutionForResult(getCurrentActivity(), RC_REQUEST);
                    } catch (IntentSender.SendIntentException exception) {
                        // Could not resolve the request
                        promise.resolve(createErrorMap("Error:" + exception.getMessage()));
                    }
                } else {
                    // Request has no resolution
                    if (e == null) {
                        promise.resolve(createErrorMap("Error: Failed requesting credentials."));
                        return;
                    }

                    promise.resolve(createErrorMap("Error: " + e.getMessage()));
                }
            }
        };
    }

    private ReadableMap createCredentialMap(Credential credential) {
        if (credential == null) {
            return createErrorMap("Error: Could not parse credentials.");
        }

        WritableMap map = new WritableNativeMap();

        final String id = credential.getId();
        final String password = credential.getPassword();
        final String accountType = credential.getAccountType();
        final String familyName = credential.getFamilyName();
        final String givenName = credential.getGivenName();
        final List<IdToken> idTokens = credential.getIdTokens();
        final String name = credential.getName();
        final Uri profilePictureUri = credential.getProfilePictureUri();

        map.putBoolean("success", true);
        map.putString("id", id);
        map.putString("password", password);
        map.putString("accountType", accountType);
        map.putString("familyName", familyName);
        map.putString("givenName", givenName);
        map.putString("name", name);

        if (profilePictureUri != null) {
            map.putString("profilePictureUri", profilePictureUri.toString());
        }

        final WritableArray idTokensArray = new WritableNativeArray();
        for (IdToken token : idTokens) {
            WritableMap tokenMap = new WritableNativeMap();
            tokenMap.putString("accountType", token.getAccountType());
            tokenMap.putString("idToken", token.getIdToken());
            idTokensArray.pushMap(tokenMap);
        }

        map.putArray("idTokens", idTokensArray);
        return map;
    }

    @NonNull
    private Credential extractCredential(@NonNull ReadableMap args) {
        if (!args.hasKey("id")) {
            throw new Error("An id is required.");
        }

        if (!args.hasKey("password")) {
            throw new Error("A password is required.");
        }

        final String id = args.getString("id");
        final String password = args.getString("password");
        final String accountType = args.hasKey("accountType")
                ? args.getString("accountType")
                : null;
        final String name = args.hasKey("name")
                ? args.getString("name")
                : null;
        final String strProfilePictureUri = args.hasKey("profilePictureUri")
                ? args.getString("profilePictureUri")
                : null;
        final Uri profilePictureUri = strProfilePictureUri != null
                ? Uri.parse(strProfilePictureUri)
                : null;

        return new Credential.Builder(id)
                .setAccountType(accountType)
                .setName(name)
                .setProfilePictureUri(profilePictureUri)
                .setPassword(password)
                .build();
    }

    private ReadableMap createErrorMap(String message) {
        final WritableMap map = new WritableNativeMap();
        map.putString("error", message);
        return map;
    }
}
