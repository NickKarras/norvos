package de.norvos;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.whispersystems.libaxolotl.AxolotlAddress;
import org.whispersystems.libaxolotl.IdentityKey;
import org.whispersystems.libaxolotl.IdentityKeyPair;
import org.whispersystems.libaxolotl.InvalidKeyException;
import org.whispersystems.libaxolotl.InvalidKeyIdException;
import org.whispersystems.libaxolotl.state.PreKeyRecord;
import org.whispersystems.libaxolotl.state.SessionRecord;
import org.whispersystems.libaxolotl.state.SignedPreKeyRecord;
import org.whispersystems.libaxolotl.util.KeyHelper;

import de.norvos.axolotl.NorvosAxolotlStore;

public class AxolotlStoreTest {

	NorvosAxolotlStore store;
	NorvosAxolotlStore secondStore;

	int ANY_NUMBER;

	@Before
	public void setUp() throws Exception {
		store = new NorvosAxolotlStore();
		Thread.sleep(10);
		secondStore = new NorvosAxolotlStore();
		Random r = new Random();
		ANY_NUMBER = r.nextInt(10) + 1;
	}

	@Test
	public void getIdentityKeyPair() {
		assertEquals(store.getIdentityKeyPair(), store.getIdentityKeyPair());
		assertNotEquals(store.getIdentityKeyPair(), secondStore.getIdentityKeyPair());
	}

	@Test
	public void getLocalRegistrationId() {
		assertEquals(store.getLocalRegistrationId(), store.getLocalRegistrationId());
		assertNotEquals(store.getLocalRegistrationId(), secondStore.getLocalRegistrationId());
	}

	@Test
	public void isTrustedIdentity() {
		IdentityKey key = KeyHelper.generateIdentityKeyPair().getPublicKey();
		IdentityKey wrongKey = KeyHelper.generateIdentityKeyPair().getPublicKey();
		assertNotEquals(key, wrongKey);

		// Case: Correct Name and Key
		store.saveIdentity("TestName", key);
		assertTrue(store.isTrustedIdentity("TestName", key));

		// Case: Unknown Key (important: Trust on first use!)
		assertTrue(store.isTrustedIdentity("UnseenName", key));

		// Case: Wrong Key
		assertFalse(store.isTrustedIdentity("TestName", wrongKey));
	}

	@Test(expected = NullPointerException.class)
	public void saveIdentityBothNull() {
		store.saveIdentity(null, null);
	}

	@Test(expected = NullPointerException.class)
	public void saveIdentityNameNull() {
		store.saveIdentity(null, KeyHelper.generateIdentityKeyPair().getPublicKey());
	}

	@Test(expected = NullPointerException.class)
	public void saveIdentityIdentityNull() {
		store.saveIdentity("TestName", null);
	}

	@Test
	public void saveIdentity() {
		IdentityKey key = KeyHelper.generateIdentityKeyPair().getPublicKey();
		store.saveIdentity("TestName", key);
		assertTrue(store.isTrustedIdentity("TestName", key));
	}

	@Test
	public void contains_storePreKey() {
		PreKeyRecord preKey = KeyHelper.generatePreKeys(0, ANY_NUMBER).get(0);
		store.storePreKey(preKey.getId(), preKey);
		assertTrue(store.containsPreKey(preKey.getId()));
		assertFalse(store.containsPreKey(preKey.getId() + 1));
	}

	@Test
	public void removePreKey() {
		PreKeyRecord preKey = KeyHelper.generatePreKeys(0, ANY_NUMBER).get(0);
		store.storePreKey(42, preKey);
		store.removePreKey(preKey.getId());
		assertFalse(store.containsPreKey(preKey.getId()));
	}

	@Test
	public void loadPreKey() throws InvalidKeyIdException {
		PreKeyRecord preKey = KeyHelper.generatePreKeys(0, ANY_NUMBER).get(0);
		store.storePreKey(preKey.getId(), preKey);
		PreKeyRecord newKey = store.loadPreKey(preKey.getId());
		assertEquals(preKey, newKey);
	}

	@Test(expected = InvalidKeyIdException.class)
	public void loadPreKeyInvalid() throws InvalidKeyIdException {
		PreKeyRecord preKey = KeyHelper.generatePreKeys(0, ANY_NUMBER).get(0);
		store.storePreKey(42, preKey);
		store.loadPreKey(preKey.getId() + 1);
	}

	@Test
	public void sessions() {
		AxolotlAddress address = new AxolotlAddress("TestName", ANY_NUMBER);
		AxolotlAddress wrongAddress = new AxolotlAddress("WrongName", ANY_NUMBER);
		SessionRecord session = new SessionRecord();
		SessionRecord wrongSession = new SessionRecord();
		store.storeSession(address, session);

		assertTrue(store.containsSession(address));
		assertFalse(store.containsSession(wrongAddress));

		assertNotEquals(session, store.loadSession(address)); // returns a copy
		assertNotEquals(wrongSession, store.loadSession(address));

		store.deleteSession(address);
		assertFalse(store.containsSession(address));

		store.storeSession(address, new SessionRecord());
		store.deleteAllSessions(address.getName());
		assertFalse(store.containsSession(address));
	}

	@Test
	public void sessionsLoadInvalid() {
		AxolotlAddress address = new AxolotlAddress("TestName", ANY_NUMBER);
		assertFalse(store.containsSession(address));
		store.loadSession(address);
	}

	@Test
	public void signedPreKey() throws InvalidKeyException, InvalidKeyIdException{
		Random r = new Random();
		IdentityKeyPair identityKey = KeyHelper.generateIdentityKeyPair();
		SignedPreKeyRecord key = KeyHelper.generateSignedPreKey(identityKey, r.nextInt(Integer.MAX_VALUE));
		
		store.storeSignedPreKey(key.getId(), key);
		assertTrue(store.containsSignedPreKey(key.getId()));
		assertFalse(store.containsSignedPreKey(key.getId()+1));
		
		assertEquals(key, store.loadSignedPreKey(key.getId()));
		store.removeSignedPreKey(key.getId());
		
		assertFalse(store.containsSignedPreKey(key.getId()));
	}
}
