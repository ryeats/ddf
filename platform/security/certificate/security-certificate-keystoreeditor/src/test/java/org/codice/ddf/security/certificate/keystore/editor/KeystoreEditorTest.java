/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.security.certificate.keystore.editor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.util.encoders.Base64;
import org.hamcrest.core.AnyOf;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class KeystoreEditorTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    File keyStoreFile = null;

    File trustStoreFile = null;

    File pkcs12StoreFile = null;

    File crtFile = null;

    File localhostCrtFile = null;

    File chainFile = null;

    File keyFile = null;

    File pemFile = null;

    File derFile = null;

    File localhostKeyFile = null;

    File jksFile = null;

    File p7bFile = null;

    File badFile = null;

    @Before
    public void setup() throws IOException {
        keyStoreFile = temporaryFolder.newFile("keystore.jks");
        trustStoreFile = temporaryFolder.newFile("truststore.jks");

        pkcs12StoreFile = temporaryFolder.newFile("asdf.p12");
        FileOutputStream p12OutStream = new FileOutputStream(pkcs12StoreFile);
        InputStream p12Stream = KeystoreEditor.class.getResourceAsStream("/asdf.p12");
        IOUtils.copy(p12Stream, p12OutStream);

        crtFile = temporaryFolder.newFile("asdf.crt");
        FileOutputStream crtOutStream = new FileOutputStream(crtFile);
        InputStream crtStream = KeystoreEditor.class.getResourceAsStream("/asdf.crt");
        IOUtils.copy(crtStream, crtOutStream);

        localhostCrtFile = temporaryFolder.newFile("localhost-cert.pem");
        FileOutputStream lCrtOutStream = new FileOutputStream(localhostCrtFile);
        InputStream lCrtStream = KeystoreEditor.class.getResourceAsStream("/localhost-cert.pem");
        IOUtils.copy(lCrtStream, lCrtOutStream);

        chainFile = temporaryFolder.newFile("chain.txt");
        FileOutputStream chainOutStream = new FileOutputStream(chainFile);
        InputStream chainStream = KeystoreEditor.class.getResourceAsStream("/chain.txt");
        IOUtils.copy(chainStream, chainOutStream);

        keyFile = temporaryFolder.newFile("asdf.key");
        FileOutputStream keyOutStream = new FileOutputStream(keyFile);
        InputStream keyStream = KeystoreEditor.class.getResourceAsStream("/asdf.key");
        IOUtils.copy(keyStream, keyOutStream);

        localhostKeyFile = temporaryFolder.newFile("localhost-key.pem");
        FileOutputStream lKeyOutStream = new FileOutputStream(localhostKeyFile);
        InputStream lKeyStream = KeystoreEditor.class.getResourceAsStream("/localhost-key.pem");
        IOUtils.copy(lKeyStream, lKeyOutStream);

        jksFile = temporaryFolder.newFile("asdf.jks");
        FileOutputStream jksOutStream = new FileOutputStream(jksFile);
        InputStream jksStream = KeystoreEditor.class.getResourceAsStream("/asdf.jks");
        IOUtils.copy(jksStream, jksOutStream);

        p7bFile = temporaryFolder.newFile("asdf.p7b");
        FileOutputStream p7bOutStream = new FileOutputStream(p7bFile);
        InputStream p7bStream = KeystoreEditor.class.getResourceAsStream("/asdf.p7b");
        IOUtils.copy(p7bStream, p7bOutStream);

        pemFile = temporaryFolder.newFile("asdf.pem");
        FileOutputStream pemOutStream = new FileOutputStream(pemFile);
        InputStream pemStream = KeystoreEditor.class.getResourceAsStream("/asdf.pem");
        IOUtils.copy(pemStream, pemOutStream);

        derFile = temporaryFolder.newFile("asdf.der");
        FileOutputStream derOutStream = new FileOutputStream(derFile);
        InputStream derStream = KeystoreEditor.class.getResourceAsStream("/asdf.der");
        IOUtils.copy(derStream, derOutStream);

        badFile = temporaryFolder.newFile("badfile.pem");
        FileOutputStream badOutStream = new FileOutputStream(badFile);
        InputStream badStream = KeystoreEditor.class.getResourceAsStream("/badfile.pem");
        IOUtils.copy(badStream, badOutStream);

        IOUtils.closeQuietly(p12OutStream);
        IOUtils.closeQuietly(p12Stream);
        IOUtils.closeQuietly(crtOutStream);
        IOUtils.closeQuietly(crtStream);
        IOUtils.closeQuietly(chainOutStream);
        IOUtils.closeQuietly(chainStream);
        IOUtils.closeQuietly(keyOutStream);
        IOUtils.closeQuietly(keyStream);
        IOUtils.closeQuietly(jksOutStream);
        IOUtils.closeQuietly(jksStream);
        IOUtils.closeQuietly(lKeyStream);
        IOUtils.closeQuietly(lKeyOutStream);
        IOUtils.closeQuietly(lCrtStream);
        IOUtils.closeQuietly(lCrtOutStream);
        IOUtils.closeQuietly(p7bStream);
        IOUtils.closeQuietly(p7bOutStream);
        IOUtils.closeQuietly(pemStream);
        IOUtils.closeQuietly(pemOutStream);
        IOUtils.closeQuietly(derStream);
        IOUtils.closeQuietly(derOutStream);
        IOUtils.closeQuietly(badStream);
        IOUtils.closeQuietly(badOutStream);

        System.setProperty("javax.net.ssl.keyStoreType", "jks");
        System.setProperty("ddf.home", "");
        System.setProperty("javax.net.ssl.keyStore", keyStoreFile.getAbsolutePath());
        System.setProperty("javax.net.ssl.trustStore", trustStoreFile.getAbsolutePath());
        System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
    }

    @Test
    public void testGetKeystoreInfo() {
        KeystoreEditor keystoreEditor = new KeystoreEditor();
        List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
        Assert.assertThat(keystore.size(), Is.is(0));
    }

    @Test
    public void testGetTruststoreInfo() {
        KeystoreEditor keystoreEditor = new KeystoreEditor();
        List<Map<String, Object>> truststore = keystoreEditor.getTruststore();
        Assert.assertThat(truststore.size(), Is.is(0));
    }

    @Test
    public void testAddKey() throws KeystoreEditor.KeystoreEditorException, IOException {
        KeystoreEditor keystoreEditor = new KeystoreEditor();
        addCertChain(keystoreEditor);
        List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
        Assert.assertThat(keystore.size(), Is.is(2));
        String alias1 = (String) keystore.get(0).get("alias");
        String alias2 = (String) keystore.get(1).get("alias");
        Assert.assertThat(alias1, AnyOf.anyOf(Is.is("asdf"), Is.is("ddf demo root ca")));
        Assert.assertThat(alias2, AnyOf.anyOf(Is.is("asdf"), Is.is("ddf demo root ca")));
        List<Map<String, Object>> truststore = keystoreEditor.getTruststore();
        Assert.assertThat(truststore.size(), Is.is(0));

        addPrivKey(keystoreEditor, keyFile, "");
        keystore = keystoreEditor.getKeystore();
        Assert.assertThat(keystore.size(), Is.is(2));
        alias1 = (String) keystore.get(0).get("alias");
        alias2 = (String) keystore.get(1).get("alias");
        Assert.assertThat(alias1, AnyOf.anyOf(Is.is("asdf"), Is.is("ddf demo root ca")));
        Assert.assertThat(alias2, AnyOf.anyOf(Is.is("asdf"), Is.is("ddf demo root ca")));
        truststore = keystoreEditor.getTruststore();
        Assert.assertThat(truststore.size(), Is.is(0));
    }

    @Test
    public void testAddPem() throws KeystoreEditor.KeystoreEditorException, IOException {
        KeystoreEditor keystoreEditor = new KeystoreEditor();
        addPrivKey(keystoreEditor, pemFile, KeystoreEditor.PEM_TYPE);
        List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
        Assert.assertThat(keystore.size(), Is.is(1));
        Assert.assertThat((String) keystore.get(0).get("alias"), Is.is("asdf"));
        List<Map<String, Object>> truststore = keystoreEditor.getTruststore();
        Assert.assertThat(truststore.size(), Is.is(0));
    }

    @Test
    public void testAddDer() throws KeystoreEditor.KeystoreEditorException, IOException {
        KeystoreEditor keystoreEditor = new KeystoreEditor();
        addPrivKey(keystoreEditor, derFile, KeystoreEditor.DER_TYPE);
        List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
        Assert.assertThat(keystore.size(), Is.is(1));
        Assert.assertThat((String) keystore.get(0).get("alias"), Is.is("asdf"));
        List<Map<String, Object>> truststore = keystoreEditor.getTruststore();
        Assert.assertThat(truststore.size(), Is.is(0));
    }

    private void addPrivKey(KeystoreEditor keystoreEditor, File keyFile, String type)
            throws KeystoreEditor.KeystoreEditorException, IOException {
        FileInputStream fileInputStream = new FileInputStream(keyFile);
        byte[] keyBytes = IOUtils.toByteArray(fileInputStream);
        IOUtils.closeQuietly(fileInputStream);
        keystoreEditor.addPrivateKey("asdf", "changeit", "", new String(Base64.encode(keyBytes)),
                type, keyFile.toString());
    }

    private void addCertChain(KeystoreEditor keystoreEditor)
            throws KeystoreEditor.KeystoreEditorException, IOException {
        FileInputStream fileInputStream = new FileInputStream(chainFile);
        byte[] crtBytes = IOUtils.toByteArray(fileInputStream);
        IOUtils.closeQuietly(fileInputStream);
        keystoreEditor.addPrivateKey("asdf", "changeit", "", new String(Base64.encode(crtBytes)),
                KeystoreEditor.PEM_TYPE, chainFile.toString());
    }

    @Test
    public void testAddKeyJks() throws KeystoreEditor.KeystoreEditorException, IOException {
        KeystoreEditor keystoreEditor = new KeystoreEditor();
        FileInputStream fileInputStream = new FileInputStream(jksFile);
        byte[] keyBytes = IOUtils.toByteArray(fileInputStream);
        IOUtils.closeQuietly(fileInputStream);
        keystoreEditor
                .addPrivateKey("asdf", "changeit", "changeit", new String(Base64.encode(keyBytes)),
                        "", jksFile.toString());
        List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
        Assert.assertThat(keystore.size(), Is.is(1));
        Assert.assertThat((String) keystore.get(0).get("alias"), Is.is("asdf"));

        List<Map<String, Object>> truststore = keystoreEditor.getTruststore();
        Assert.assertThat(truststore.size(), Is.is(0));
    }

    @Test
    public void testAddKeyP12() throws KeystoreEditor.KeystoreEditorException, IOException {
        KeystoreEditor keystoreEditor = new KeystoreEditor();
        FileInputStream fileInputStream = new FileInputStream(pkcs12StoreFile);
        byte[] keyBytes = IOUtils.toByteArray(fileInputStream);
        IOUtils.closeQuietly(fileInputStream);
        keystoreEditor
                .addPrivateKey("asdf", "changeit", "changeit", new String(Base64.encode(keyBytes)),
                        KeystoreEditor.PKCS12_TYPE, pkcs12StoreFile.toString());
        List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
        Assert.assertThat(keystore.size(), Is.is(1));
        Assert.assertThat((String) keystore.get(0).get("alias"), Is.is("asdf"));

        List<Map<String, Object>> truststore = keystoreEditor.getTruststore();
        Assert.assertThat(truststore.size(), Is.is(0));
    }

    @Test
    public void testAddKeyLocal() throws KeystoreEditor.KeystoreEditorException, IOException {
        KeystoreEditor keystoreEditor = new KeystoreEditor();
        FileInputStream fileInputStream = new FileInputStream(localhostCrtFile);
        byte[] crtBytes = IOUtils.toByteArray(fileInputStream);
        IOUtils.closeQuietly(fileInputStream);
        keystoreEditor
                .addPrivateKey("localhost", "changeit", "", new String(Base64.encode(crtBytes)),
                        KeystoreEditor.PEM_TYPE, localhostCrtFile.toString());
        List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
        Assert.assertThat(keystore.size(), Is.is(1));

        List<Map<String, Object>> truststore = keystoreEditor.getTruststore();
        Assert.assertThat(truststore.size(), Is.is(0));

        keystoreEditor = new KeystoreEditor();
        fileInputStream = new FileInputStream(localhostKeyFile);
        byte[] keyBytes = IOUtils.toByteArray(fileInputStream);
        IOUtils.closeQuietly(fileInputStream);
        keystoreEditor.addPrivateKey("localhost", "changeit", "changeit",
                new String(Base64.encode(keyBytes)), KeystoreEditor.PEM_TYPE,
                localhostKeyFile.toString());
        keystore = keystoreEditor.getKeystore();
        Assert.assertThat(keystore.size(), Is.is(1));
        Assert.assertThat((String) keystore.get(0).get("alias"), Is.is("localhost"));

        truststore = keystoreEditor.getTruststore();
        Assert.assertThat(truststore.size(), Is.is(0));
    }

    @Test
    public void testAddCert() throws KeystoreEditor.KeystoreEditorException, IOException {
        KeystoreEditor keystoreEditor = new KeystoreEditor();
        FileInputStream fileInputStream = new FileInputStream(crtFile);
        byte[] crtBytes = IOUtils.toByteArray(fileInputStream);
        IOUtils.closeQuietly(fileInputStream);
        keystoreEditor
                .addTrustedCertificate("asdf", "changeit", "", new String(Base64.encode(crtBytes)),
                        KeystoreEditor.PEM_TYPE, crtFile.toString());
        List<Map<String, Object>> truststore = keystoreEditor.getTruststore();
        Assert.assertThat(truststore.size(), Is.is(1));
        Assert.assertThat((String) truststore.get(0).get("alias"), Is.is("asdf"));

        List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
        Assert.assertThat(keystore.size(), Is.is(0));
    }

    @Test
    public void testDeleteCert() throws KeystoreEditor.KeystoreEditorException, IOException {
        KeystoreEditor keystoreEditor = new KeystoreEditor();
        addCertChain(keystoreEditor);
        List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
        Assert.assertThat(keystore.size(), Is.is(2));

        keystoreEditor.deletePrivateKey("asdf");
        keystore = keystoreEditor.getKeystore();
        Assert.assertThat(keystore.size(), Is.is(1));
    }

    @Test
    public void testDeleteTrustedCert() throws KeystoreEditor.KeystoreEditorException, IOException {
        KeystoreEditor keystoreEditor = new KeystoreEditor();
        FileInputStream fileInputStream = new FileInputStream(crtFile);
        byte[] crtBytes = IOUtils.toByteArray(fileInputStream);
        IOUtils.closeQuietly(fileInputStream);
        keystoreEditor
                .addTrustedCertificate("asdf", "changeit", "", new String(Base64.encode(crtBytes)),
                        KeystoreEditor.PEM_TYPE, crtFile.toString());
        List<Map<String, Object>> truststore = keystoreEditor.getTruststore();
        Assert.assertThat(truststore.size(), Is.is(1));

        List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
        Assert.assertThat(keystore.size(), Is.is(0));

        keystoreEditor.deleteTrustedCertificate("asdf");
        truststore = keystoreEditor.getTruststore();
        Assert.assertThat(truststore.size(), Is.is(0));

        keystore = keystoreEditor.getKeystore();
        Assert.assertThat(keystore.size(), Is.is(0));
    }

    @Test
    public void testDeleteKey() throws KeystoreEditor.KeystoreEditorException, IOException {
        KeystoreEditor keystoreEditor = new KeystoreEditor();
        addCertChain(keystoreEditor);
        List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
        Assert.assertThat(keystore.size(), Is.is(2));

        addPrivKey(keystoreEditor, keyFile, "");
        Assert.assertThat(keystore.size(), Is.is(2));

        keystoreEditor.deletePrivateKey("asdf");
        keystore = keystoreEditor.getKeystore();
        Assert.assertThat(keystore.size(), Is.is(1));
    }

    @Test
    public void testEncryptedData()
            throws KeystoreEditor.KeystoreEditorException, IOException {
        KeystoreEditor keystoreEditor = new KeystoreEditor();
        FileInputStream fileInputStream = new FileInputStream(p7bFile);
        byte[] crtBytes = IOUtils.toByteArray(fileInputStream);
        IOUtils.closeQuietly(fileInputStream);
        keystoreEditor
                .addTrustedCertificate("asdf", "changeit", "", new String(Base64.encode(crtBytes)),
                        KeystoreEditor.PEM_TYPE, p7bFile.toString());
        List<Map<String, Object>> truststore = keystoreEditor.getTruststore();
        Assert.assertThat(truststore.size(), Is.is(1));
        Assert.assertThat((String) truststore.get(0).get("alias"), Is.is("asdf"));

        List<Map<String, Object>> keystore = keystoreEditor.getKeystore();
        Assert.assertThat(keystore.size(), Is.is(0));
    }

    @Test(expected = KeystoreEditor.KeystoreEditorException.class)
    public void testBadData() throws KeystoreEditor.KeystoreEditorException {
        KeystoreEditor keystoreEditor = new KeystoreEditor();
        keystoreEditor.addPrivateKey("asdf", "changeit", "changeit", "", "", "file.pem");
    }

    @Test(expected = KeystoreEditor.KeystoreEditorException.class)
    public void testBadKeyPassword() throws KeystoreEditor.KeystoreEditorException, IOException {
        KeystoreEditor keystoreEditor = new KeystoreEditor();
        FileInputStream fileInputStream = new FileInputStream(jksFile);
        byte[] keyBytes = IOUtils.toByteArray(fileInputStream);
        IOUtils.closeQuietly(fileInputStream);
        keystoreEditor
                .addPrivateKey("asdf", "blah", "changeit", new String(Base64.encode(keyBytes)), "",
                        jksFile.toString());
    }

    @Test(expected = KeystoreEditor.KeystoreEditorException.class)
    public void testBadKeyPasswordP12() throws KeystoreEditor.KeystoreEditorException, IOException {
        KeystoreEditor keystoreEditor = new KeystoreEditor();
        FileInputStream fileInputStream = new FileInputStream(pkcs12StoreFile);
        byte[] keyBytes = IOUtils.toByteArray(fileInputStream);
        IOUtils.closeQuietly(fileInputStream);
        keystoreEditor
                .addPrivateKey("asdf", "blah", "changeit", new String(Base64.encode(keyBytes)), "",
                        pkcs12StoreFile.toString());
    }

    @Test(expected = KeystoreEditor.KeystoreEditorException.class)
    public void testBadStorePassword() throws KeystoreEditor.KeystoreEditorException, IOException {
        KeystoreEditor keystoreEditor = new KeystoreEditor();
        FileInputStream fileInputStream = new FileInputStream(jksFile);
        byte[] keyBytes = IOUtils.toByteArray(fileInputStream);
        IOUtils.closeQuietly(fileInputStream);
        keystoreEditor
                .addPrivateKey("asdf", "changeit", "blah", new String(Base64.encode(keyBytes)), "",
                        jksFile.toString());
    }

    @Test(expected = KeystoreEditor.KeystoreEditorException.class)
    public void testBadStorePasswordP12() throws KeystoreEditor.KeystoreEditorException, IOException {
        KeystoreEditor keystoreEditor = new KeystoreEditor();
        FileInputStream fileInputStream = new FileInputStream(pkcs12StoreFile);
        byte[] keyBytes = IOUtils.toByteArray(fileInputStream);
        IOUtils.closeQuietly(fileInputStream);
        keystoreEditor
                .addPrivateKey("asdf", "changeit", "blah", new String(Base64.encode(keyBytes)), "",
                        pkcs12StoreFile.toString());
    }

    @Test(expected = KeystoreEditor.KeystoreEditorException.class)
    public void testNullAlias() throws KeystoreEditor.KeystoreEditorException, IOException {
        KeystoreEditor keystoreEditor = new KeystoreEditor();
        FileInputStream fileInputStream = new FileInputStream(pkcs12StoreFile);
        byte[] keyBytes = IOUtils.toByteArray(fileInputStream);
        IOUtils.closeQuietly(fileInputStream);
        keystoreEditor
                .addPrivateKey(null, "changeit", "blah", new String(Base64.encode(keyBytes)), "",
                        pkcs12StoreFile.toString());
    }

    @Test(expected = KeystoreEditor.KeystoreEditorException.class)
    public void testBlankAlias() throws KeystoreEditor.KeystoreEditorException, IOException {
        KeystoreEditor keystoreEditor = new KeystoreEditor();
        FileInputStream fileInputStream = new FileInputStream(pkcs12StoreFile);
        byte[] keyBytes = IOUtils.toByteArray(fileInputStream);
        IOUtils.closeQuietly(fileInputStream);
        keystoreEditor
                .addPrivateKey("", "changeit", "blah", new String(Base64.encode(keyBytes)), "",
                        pkcs12StoreFile.toString());
    }

    @Test(expected = KeystoreEditor.KeystoreEditorException.class)
    public void testBadFile() throws KeystoreEditor.KeystoreEditorException, IOException {
        KeystoreEditor keystoreEditor = new KeystoreEditor();
        FileInputStream fileInputStream = new FileInputStream(badFile);
        byte[] keyBytes = IOUtils.toByteArray(fileInputStream);
        IOUtils.closeQuietly(fileInputStream);
        keystoreEditor
                .addPrivateKey("", "changeit", "blah", new String(Base64.encode(keyBytes)), "",
                        badFile.toString());
    }
}
