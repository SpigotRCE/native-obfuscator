package by.radioegor146.compiletime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class LoaderUnpack {
    public static native void registerNativesForClass(int index, Class<?> clazz);

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
            try (ZipInputStream zipInputStream = new ZipInputStream(resourceStream)) {
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
