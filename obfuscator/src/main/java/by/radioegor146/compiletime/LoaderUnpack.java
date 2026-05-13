package by.radioegor146.compiletime;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class LoaderUnpack {
    public static native void registerNativesForClass(int index, Class<?> clazz);

    private static byte[] hkdfExpand(byte[] key, String info, int length) throws Exception {
        String MAC_ALGO = "HmacSHA256";
        Mac mac = Mac.getInstance(MAC_ALGO);
        mac.init(new SecretKeySpec(key, MAC_ALGO));

        byte[] result = new byte[length];
        byte[] t = new byte[0];
        int pos = 0;
        int counter = 1;

        while (pos < length) {
            mac.reset();
            mac.update(t);
            mac.update(info.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) counter);

            t = mac.doFinal();

            int copy = Math.min(t.length, length - pos);
            System.arraycopy(t, 0, result, pos, copy);
            pos += copy;
            counter++;
        }

        return result;
    }

    private static InputStream decryptStream(InputStream encryptedStream) {
        try {
            byte[] encryptedBytes = encryptedStream.readAllBytes();

            byte[] masterKey = "%MASTER_KEY%".getBytes(StandardCharsets.ISO_8859_1);
            SecretKeySpec keySpec = new SecretKeySpec(hkdfExpand(masterKey, "aes-key", 16), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(hkdfExpand(masterKey, "aes-iv", 16));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new ByteArrayInputStream(decryptedBytes);
        } catch (Exception e) {
            throw new UnsatisfiedLinkError("Failed to initialize decryption: " + e.getMessage());
        }
    }

    static {
        String osName = System.getProperty("os.name").toLowerCase();
        String platform = System.getProperty("os.arch").toLowerCase();

        String platformTypeName;
        switch (platform) {
            case "x86_64":
            case "amd64":
                platformTypeName = "x64";
                break;
            case "aarch64":
                platformTypeName = "arm64";
                break;
            case "arm":
                platformTypeName = "arm32";
                break;
            case "x86":
                platformTypeName = "x86";
                break;
            default:
                platformTypeName = "raw" + platform;
                break;
        }

        String osTypeName;
        if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            osTypeName = "linux.so";
        } else if (osName.contains("win")) {
            osTypeName = "windows.dll";
        } else if (osName.contains("mac")) {
            osTypeName = "macos.dylib";
        } else {
            osTypeName = "raw" + osName;
        }

        String nativeDir = "/%NATIVE_DIR%/";
        String zipFileName = nativeDir + "%LIB_NAME%.zip";
        String entryName = platformTypeName + "-" + osTypeName;

        File libFile;
        try {
            libFile = File.createTempFile(UUID.randomUUID().toString(), null);
            libFile.deleteOnExit();
            if (!libFile.exists()) {
                throw new IOException();
            }
        } catch (IOException iOException) {
            throw new UnsatisfiedLinkError("Failed to create temp file");
        }

        try {
            InputStream resourceStream = LoaderUnpack.class.getResourceAsStream(zipFileName);
            if (resourceStream == null) {
                throw new UnsatisfiedLinkError(String.format("Failed to open zip file: %s", zipFileName));
            }
            try (ZipInputStream zipInputStream = new ZipInputStream(decryptStream(resourceStream))) {
                ZipEntry entry;
                boolean found = false;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    if (entry.getName().equals(entryName)) {
                        found = true;
                        byte[] buffer = new byte[2048];
                        try (FileOutputStream fileOutputStream = new FileOutputStream(libFile)) {
                            int size;
                            while ((size = zipInputStream.read(buffer)) != -1) {
                                fileOutputStream.write(buffer, 0, size);
                            }
                        }
                        zipInputStream.closeEntry();
                        break;
                    }
                    zipInputStream.closeEntry();
                }
                if (!found) {
                    throw new UnsatisfiedLinkError(String.format("Entry not found in zip: %s", entryName));
                }
            }
        } catch (IOException exception) {
            throw new UnsatisfiedLinkError(String.format("Failed to extract lib from zip: %s", exception.getMessage()));
        }
        System.load(libFile.getAbsolutePath());
    }
}
