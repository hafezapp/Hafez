package org.hrana.hafez.net;

import okhttp3.RequestBody;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import rx.Observable;

/**
 * Interface for API requests
 */
public interface IApiEndpoint {

    @POST("api/v1/submit-attachment/")
    @Multipart
    Observable<String> postEncryptedAttachment(@Part("client_version") RequestBody clientVersion,
                                               @Part("submission_time") RequestBody time,
                                               @Part("encryption_key_id") RequestBody encryptionKeyId,
                                               @Part("security_token") RequestBody token,
                                               @Part("encrypted_blob") RequestBody blob,
                                               @Part("attachment_data\"; filename=\"attachment_data\"") RequestBody attachment);
    @FormUrlEncoded
    @POST("api/v1/submit-report/")
    Observable<String> postEncryptedReport(@Field("client_version") String clientVersion,
                                               @Field("submission_time") String time,
                                               @Field("encryption_key_id") String encryptionKeyId,
                                               @Field("security_token") String token,
                                               @Field("encrypted_blob") String blob);

}
