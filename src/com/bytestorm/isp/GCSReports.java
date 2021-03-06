package com.bytestorm.isp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.joda.time.DateTime;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.IOUtils;
import com.google.api.client.util.store.AbstractDataStore;
import com.google.api.client.util.store.AbstractDataStoreFactory;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.api.services.storage.model.Objects;
import com.google.api.services.storage.model.StorageObject;

public class GCSReports implements ReportsProvider {

    public GCSReports(Configuration cfg, Date date, boolean keepReports) throws IOException, IllegalArgumentException {
        if (null == cfg.getProperty("gcs.reports.bucket")) {
            if (DEFAULT_BUCKET.equals("<PLAY_BUCKET>")) {
                throw new IllegalArgumentException("GCS reports bucket id missing, and DEFAULT_BUCKET field is set");
            } else {
                cfg.put("gcs.reports.bucket", DEFAULT_BUCKET);
            }
        }        
        // Initialize the transport.
        HttpTransport httpTransport = null;
        try {
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException ex) {
            throw new IllegalArgumentException("Unable to create transprt", ex);
        }
        Credential credential  = authorize(httpTransport, cfg);        
        // storage client
        this.bucket = cfg.getProperty("gcs.reports.bucket");
        this.date = new DateTime(date);
        Storage client = new Storage.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APP_NAME)
                .build();        
        downloadAll(client, "earnings/earnings_" + DATE_FORMAT.format(this.date.toDate()), earningReports);
        if (earningReports.isEmpty()) {
            throw new IOException("Cannot find earnings report for specified date");
        }
        downloadAll(client, "sales/salesreport_" + DATE_FORMAT.format(this.date.toDate()), salesReports);
        downloadAll(client, "sales/salesreport_" + DATE_FORMAT.format(this.date.plusMonths(1).toDate()), salesReports);
        if (keepReports) {
            Log.v("Saving downloaded CSV files");
            // resolve collisions
            HashMap<String, ArrayList<File>> collisions = new HashMap<>();
            for (Map.Entry<File, String> entry : mapping.entrySet()) {
                ArrayList<File> filesWithName = collisions.get(entry.getValue()); 
                if (null == filesWithName) {
                    filesWithName = new ArrayList<>();
                    collisions.put(entry.getValue(), filesWithName);
                }
                filesWithName.add(entry.getKey());
            }
            for (Map.Entry<String, ArrayList<File>> entry : collisions.entrySet()) {
                if (entry.getValue().size() > 1) {
                    // colliding filename found
                    String name = Utils.splitFileName(entry.getKey())[0];
                    String ext = Utils.splitFileName(entry.getKey())[1];                    
                    int n = 1;
                    for (File f : entry.getValue()) {
                        mapping.put(f, name + " (" + (n++) + ")" + ext);
                    }
                }
            }
            for (Map.Entry<File, String> entry : mapping.entrySet()) {
                final File src = entry.getKey();
                final File dst = new File(entry.getValue());
                Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    @Override
    public File[] getEarningsReportsFiles() throws IOException, IllegalArgumentException {
        return earningReports.toArray(new File[earningReports.size()]);
    }

    @Override
    public File[] getSalesReportsFiles() throws IOException, IllegalArgumentException {
        return salesReports.toArray(new File[salesReports.size()]);
    }

    @Override
    public Date getDate() {
        return date.toDate();
    }
    
    private void downloadAll(Storage client, String prefix, ArrayList<File> out) throws IOException {
        Storage.Objects.List list = client.objects().list(bucket);
        list.setPrefix(prefix);
        Objects earningsReportsObjects = list.execute();        
        for (StorageObject obj : earningsReportsObjects.getItems()) {
            out.add(downloadAndUnpack(client, obj));
        }
    }
    
    private File downloadAndUnpack(Storage client, StorageObject file) throws IOException {
        Log.v("Downloading storage file " + file.getName());
        String fileName = file.getName();
        Storage.Objects.Get getObject = client.objects().get(bucket, fileName);
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        getObject.getMediaHttpDownloader().setDirectDownloadEnabled(true);
        getObject.executeMediaAndDownloadTo(data);
        File out = File.createTempFile("tmp", ".csv");
        String origName = Utils.unpack(new ByteArrayInputStream(data.toByteArray()), out);
        mapping.put(out, origName);
        return out;
    }
    
    private Credential authorize(HttpTransport http, Configuration config) throws IOException, IllegalArgumentException {
        if (null != config.getProperty("gcs.service.pk12.path")) {
            if (null != config.getProperty("gcs.service.email")) {
                Log.v("Connecting to GCS using service account credential");
                return authorizeService(http, new FileInputStream(config.getProperty("gcs.service.cert.pk12.path")), 
                        config.getProperty("gcs.service.email"));
            } else {
                Log.v("PK12 path configured but service e-mail address missing");
            }
        }
        if (null != config.getProperty("gcs.client.secret.json.path")) {
            Log.v("Connecting to GCS using user credential");
            return authorizeUser(http, new FileInputStream(config.getProperty("gcs.client.secret.json.path")));
        }
        
        if (null != getClass().getResource("/resources/service_key.p12") &&
                null != getClass().getResource("/resources/service_email")) {
            Log.v("Using build-in service credentials");
            String email = null;
            try (InputStream is = getClass().getResourceAsStream("/resources/service_email")) {
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                email = in.readLine();
                if (null == email || email.isEmpty()) {
                    throw new IOException("Cannot read service email from /resources/service_email resource");
                }
            }
            return authorizeService(http, getClass().getResourceAsStream("/resources/service_key.p12"), email);
        }
        if (null != getClass().getResource("/resources/client_secret.json")) {
            return authorizeUser(http, getClass().getResourceAsStream("/resources/client_secret.json"));
        }
        throw new IllegalArgumentException("Neither service nor user credentials are provided for GCS access");
    }
    
    private Credential authorizeUser(HttpTransport http, InputStream creds) throws IOException, IllegalArgumentException {
        try {
            System.setProperty("org.mortbay.log.class", SimpleJettyLogger.class.getName());
            GoogleClientSecrets secret = null;
            try {
                secret = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(creds));            
            } catch (Exception ex) {
                throw new IllegalArgumentException("Unable to load client id file", ex);
            }
            if (null == secret) {
                throw new IllegalArgumentException("Unable to load client id file");
            }
            if (null == secret.getDetails().getClientId() || null == secret.getDetails().getClientSecret()) {
                throw new IllegalArgumentException("Client id file is not well formed.");
            }
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(http, JSON_FACTORY, secret, GCS_SCOPES)
                    .setDataStoreFactory(DATA_STORE_FACTORY)
                    .build();
            return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        } finally {
            creds.close();
        }
    }
    
    private Credential authorizeService(HttpTransport http, InputStream key, String email) throws IOException {
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(key, "notasecret".toCharArray());
            PrivateKey pk = (PrivateKey) ks.getKey("privatekey", "notasecret".toCharArray());
            return new GoogleCredential.Builder()
                    .setTransport(http)
                    .setJsonFactory(JSON_FACTORY)
                    .setServiceAccountId(email)
                    .setServiceAccountPrivateKey(pk)
                    .setServiceAccountScopes(GCS_SCOPES)
                    .build();
        } catch (KeyStoreException e) {
            throw new Error("Cannot initialize PKCS12 key store");
        } catch (NoSuchAlgorithmException e) {
            throw new Error("Cannot load P12 key");
        } catch (CertificateException e) {
            throw new IllegalArgumentException("Invalid P12 key file");
        } catch (UnrecoverableKeyException e) {
            throw new IllegalArgumentException("Invalid P12 key file");
        } finally {
            key.close();
        }
    }
    
    private static class PreferencesDataStore<V extends Serializable> extends AbstractDataStore<V> {
        PreferencesDataStore(DataStoreFactory dataStore) {
            super(dataStore, "com.bytestorm.isp.gcs"); 
        }

        @Override
        public DataStore<V> clear() throws IOException {
            try {
                prefs.clear();
                return this;
            } catch (BackingStoreException e) {
                throw new IOException("Preferences exception", e);
            }            
        }

        @Override
        public DataStore<V> delete(String key) throws IOException {
            try {
                prefs.remove(key);
                prefs.sync();
                return this;
            } catch (BackingStoreException e) {
                throw new IOException("Preferences exception", e);
            }        
        }

        @Override
        public V get(String key) throws IOException {
            return IOUtils.deserialize(prefs.getByteArray(key, null));
        }

        @Override
        public Set<String> keySet() throws IOException {
            try {
                return new HashSet<String>(Arrays.asList(prefs.keys()));
            } catch (BackingStoreException e) {
                throw new IOException("Preferences exception", e);
            }             
        }

        @Override
        public DataStore<V> set(String key, V value) throws IOException {
            try {
                prefs.putByteArray(key, IOUtils.serialize(value));
                prefs.sync();
                return this;
            } catch (BackingStoreException e) {
                throw new IOException("Preferences exception", e);
            }            
        }

        @Override
        public Collection<V> values() throws IOException {
            try {
                ArrayList<V> retval = new ArrayList<>();
                for (String key : prefs.keys()) {
                    retval.add(IOUtils.deserialize(prefs.getByteArray(key, null)));
                }
                return retval;
            } catch (BackingStoreException e) {
                throw new IOException("Preferences exception", e);
            }                
        }
        
        private Preferences prefs = Preferences.userNodeForPackage(PreferencesDataStore.class);
    }
    
    private String bucket;
    private DateTime date;
    private ArrayList<File> earningReports = new ArrayList<>();
    private ArrayList<File> salesReports = new ArrayList<>();
    private HashMap<File, String> mapping = new HashMap<>();
    
    private static final String DEFAULT_BUCKET = "<PLAY_BUCKET>";
    
    private static final String APP_NAME = "Bytestorm-ISP/1.0";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMM");
    
    private static final Collection<String> GCS_SCOPES = Collections.singleton(StorageScopes.DEVSTORAGE_READ_ONLY);   
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();        
    private static final DataStoreFactory DATA_STORE_FACTORY = new AbstractDataStoreFactory() {        
        @Override
        protected <V extends Serializable> DataStore<V> createDataStore(String arg0) throws IOException {
            return new PreferencesDataStore<V>(this);
        }
    };
}
