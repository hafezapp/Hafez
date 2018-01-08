package org.hrana.hafez.nacl;

import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Base64;
import android.util.Base64InputStream;

import org.hrana.hafez.di.module.TestCryptoModule;
import org.hrana.hafez.model.Attachment;
import org.hrana.hafez.model.Report;
import org.hrana.hafez.model.EncryptedReport;
import org.hrana.hafez.presenter.CryptoPresenter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.libsodium.jni.Sodium;
import org.libsodium.jni.keys.PrivateKey;
import org.libsodium.jni.keys.PublicKey;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;

import static org.hrana.hafez.Constants.SIMPLE_DATE_FORMAT;

/**
 * Test cryptography methods.
 *
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class NaclCryptoPresenterTest {

    @Inject CryptoPresenter cryptoPresenter;
    PublicKey publicKey = new PublicKey("5ccf6df7306718164450ab250e0c44f0a00ece70c45f3ceceb0d6f4e7f03fa50");
    PrivateKey privateKey = new PrivateKey("03c3aca1adc2c007f6f28c4e2cfa7fea06d404c110919abbf4c4436813a248fe");

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        DaggerITestCryptoComponent.builder()
                .testCryptoModule(new TestCryptoModule(publicKey))
                .build()
                .inject(this);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSanity() throws Exception { // just making sure crypto_box_seal works the way we want.
        String plaintext = "{test:plaintext sample}";
        byte[] plainbytes = plaintext.getBytes(StandardCharsets.UTF_8);
        byte[] cipher = new byte[plainbytes.length + Sodium.crypto_box_sealbytes()];
        Sodium.crypto_box_seal(cipher, plainbytes, plainbytes.length, publicKey.toBytes());
        byte[] decipher = new byte[cipher.length];
        Sodium.crypto_box_seal_open(decipher, cipher, cipher.length,
                publicKey.toBytes(),
                privateKey.toBytes());
        String recoveredText = new String(decipher, StandardCharsets.UTF_8).trim();
        Assert.assertTrue(recoveredText.equals(plaintext));
    }

    @Test
    public void encryptReport() throws Exception {
        Report report = fakeReport();
        EncryptedReport encryptedBody = cryptoPresenter.encryptReport(report); //base64'd binary
        byte[] decrypted = decryptLibsodiumBox(encryptedBody.getEncrypted_blob());
        String recoveredText = new String(decrypted, StandardCharsets.UTF_8).trim();
        Assert.assertTrue("Expected original report text but got " + recoveredText + ", "
                + cryptoPresenter.getJsonAdapter().toJson(report),
                recoveredText.equals(cryptoPresenter.getJsonAdapter().toJson(report)));
    }

    @Test
    public void testEncryptAES() throws Exception {
        SecretKey aesKey = cryptoPresenter.generateAttachmentKey();
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] iv = cipher.getIV();

        String sampleNonsense = "{\"title\":\"Sample nonsense report from me to you\"}";
        byte[] encrypted = cipher.doFinal(sampleNonsense.getBytes(StandardCharsets.UTF_8));

        Cipher decryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
        decryptCipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));
        byte[] decrypted = decryptCipher.doFinal(encrypted);
        String recoveredText = new String(decrypted).trim();
        Assert.assertTrue(sampleNonsense.equals(recoveredText));
    }

    @Test
    public void testEncryptAESBase64() throws Exception {
        SecretKey aesKey = cryptoPresenter.generateAttachmentKey();
        String sampleNonsense = "{\"title\":\"Sample nonsense report from me to you, this time p/b Base64.\"}";
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(sampleNonsense.getBytes(StandardCharsets.UTF_8));
        byte[] encrypted64 = Base64.encode(encrypted, Base64.NO_WRAP);

        Cipher decryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
        decryptCipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));
        byte[] decoded64 = Base64.decode(encrypted64, Base64.NO_WRAP);
        byte[] decrypted = decryptCipher.doFinal(decoded64);
        String recoveredText = new String(decrypted).trim();
        Assert.assertTrue(sampleNonsense.equals(recoveredText));
    }


    @Test
    public void testTwoPartEncryptionBundle() throws Exception {

        //*********** Encrypting ***************//

        // Create new AES SecretKey object and initialize cipher
        SecretKey aesKey = cryptoPresenter.generateAttachmentKey();
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new SecureRandom());
        byte[] iv = cipher.getIV();

        final String secret_message_file = "This is a secret message";

        // Fake file (could be text, image or multimedia)
        File file = folder.newFile("crryptotestfile");
        File outPutFile = folder.newFile("outputtestifle");

        FileOutputStream fos = new FileOutputStream(file);
        fos.write(secret_message_file.getBytes(Charset.forName("UTF-8")));
        fos.close();

        // Sample metadata stub
        Attachment metadata = Attachment.builder()
                .clientId("pretendId")
                .securityToken("")
                .iv(iv)
                .reportId("1234")
                .key(aesKey.getEncoded())
                .timestamp(new SimpleDateFormat(SIMPLE_DATE_FORMAT, Locale.US).format(new Date()))
                .build();

        // Encrypt media and write to stream.
        int consumed;
        byte[] buffer = new byte[1200]; // code has 3000-byte buffer.
        CipherInputStream cipherInputStream = new CipherInputStream(new FileInputStream(file), cipher);
        FileOutputStream outputStream = new FileOutputStream(outPutFile);

        while ((consumed = cipherInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, consumed);
        }
        outputStream.close();

        String json = cryptoPresenter.getMetadataAdapter().toJson(metadata);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        // Encrypt JSON with libsodium
        byte[] ciphered = new byte[jsonBytes.length + Sodium.crypto_box_sealbytes()];
                Sodium.crypto_box_seal(ciphered, jsonBytes, jsonBytes.length, publicKey.toBytes());

        String details = Base64.encodeToString(ciphered, Base64.NO_WRAP);


        //*********** Decrypting ***************//
        /*
         * 1. Base64#decode the blob. This is now a decoded (but still encrypted) byte[].
         * 2. Use Libsodium#Sealed_box_open with the server's public and private key credentials
         *    to decrypt this Json into a clean byte[].
         *    (Note that you will initially have to allocate more space in the byte[] than the decrypted
         *    JSON actually requires, because the encrypted object has a signature suffix).
         * 4. The decrypted Json contains metadata information (id, submission_time, mimeType),
         *    as well as the AES key and IV
         *    necessary to decrypt the latter half of the blob.
         * 5. Use the information above to decrypt the file (mediafile).
         */


        // Base64-Decoding remainder of blob
        byte[] encryptedJson = Base64.decode(details, Base64.NO_WRAP);

        // Open sealed_box using crypto_box_seal_open, store in new byte[]
        byte[] plainJson = new byte[encryptedJson.length];
        Sodium.crypto_box_seal_open(plainJson, encryptedJson, encryptedJson.length,
                publicKey.toBytes(), privateKey.toBytes());

        // The unencrypted byte[] is longer than the encrypted byte[], so use trim or similar
        // function if needed (language-dependent)
        String recoveredJsonString = new String(plainJson).trim();

        // Check sanity
        Assert.assertTrue("Expected equal string objects but found: "
                + json + ", " + recoveredJsonString,
                recoveredJsonString.equals(json));

        // Parse JSON into POJO
        Attachment decryptedMetaData = cryptoPresenter.getMetadataAdapter().fromJson(recoveredJsonString);

        // Read rest of privateDetails from [jsonSize : decodedfile.size], this is the media/attachment file.
        // Use information from JSON to initialize AES decrpytion cipher and decrypt this file

        byte[] aesEncryptedBinary = new byte[(int) outPutFile.length()]; // note: this is only because we KNOW this file is tiny and we're just making sure the AES key encrypts and decrypts. Can't do this in the real world!
        SecretKey aesDecryptKey = new SecretKeySpec(decryptedMetaData.getKey(),
                0, decryptedMetaData.getKey().length, "AES");
        Cipher decryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
        decryptCipher.init(Cipher.DECRYPT_MODE, aesDecryptKey, new IvParameterSpec(decryptedMetaData.getIv()));
        byte[] out = decryptCipher.doFinal(aesEncryptedBinary);

        //assertEquals(new String(out, Charset.forName("UTF-8")), secret_message_file);
    }

    @Test
    public void testDecryptAttachmentBlob() throws Exception {
        final SecretKey key = cryptoPresenter.generateAttachmentKey();
        File f = makeFile("");
        File encryptF = folder.newFile("encrypted");

        Attachment.builder()
                .uri(Uri.fromFile(f))
                .reportId("fakeId")
                .timestamp("2049-12-12T12:34:56.0000")
                .mimeType("application/json")
                .clientId("fakeId")
                .securityToken("")
                .attachmentId("1234567")
                .key(key.getEncoded())
                .build();

        cryptoPresenter.encryptAttachment(encryptF, key, Attachment.builder().build(), new FileInputStream(f));
    }

    private File makeFile(String prefix) throws Exception {
        InputStream realFileInputStream = InstrumentationRegistry.getContext().getAssets().open("picture.png");
        File tempAttachment = folder.newFile("picture" + prefix + ".png");
        FileOutputStream fos = new FileOutputStream(tempAttachment);

        byte[] buff = new byte[1024];
        int len;
        while ((len = realFileInputStream.read(buff)) != -1) {
            fos.write(buff, 0, len);
        }

        fos.close();
        return tempAttachment;
    }

    private Report fakeReport() {
        String fakeClientId = "1234";
        return Report.builder()
                .email("fake_email@pretend.org")
                .telegram("idontknow")
                .reportBody("pretend.")
                .timestamp(new SimpleDateFormat(SIMPLE_DATE_FORMAT, Locale.US).format(new Date()))
                .build()
                .assignId()
                .assignClientId(fakeClientId);
    }

    private byte[] decryptLibsodiumBox(String cipher) {
        byte[] message = Base64.decode(cipher, Base64.NO_WRAP);
        byte[] decipher = new byte[message.length];
        Sodium.crypto_box_seal_open(decipher, message, message.length, publicKey.toBytes(), privateKey.toBytes());
        return decipher;
    }


    private File decryptAES(Base64InputStream base64InputStream, SecretKey key, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, iv);

        InputStream inputStream = new CipherInputStream(base64InputStream, cipher);

        byte[] buffer = new byte[1024];
        File recoveredFile = folder.newFile("testrecoveredfile");

        FileOutputStream fos = new FileOutputStream(recoveredFile);

        int consumed = 0;
        while ((consumed = inputStream.read(buffer)) != -1) {
            fos.write(buffer, 0, consumed);
        }

        inputStream.close();
        fos.close();

        return recoveredFile;
    }
    
}
