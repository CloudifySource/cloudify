package org.cloudifysource.esc.installer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.Channel;
import org.apache.sshd.common.Cipher;
import org.apache.sshd.common.Compression;
import org.apache.sshd.common.ForwardingAcceptorFactory;
import org.apache.sshd.common.KeyExchange;
import org.apache.sshd.common.Mac;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.Signature;
import org.apache.sshd.common.cipher.AES128CBC;
import org.apache.sshd.common.cipher.AES128CTR;
import org.apache.sshd.common.cipher.AES192CBC;
import org.apache.sshd.common.cipher.AES256CBC;
import org.apache.sshd.common.cipher.AES256CTR;
import org.apache.sshd.common.cipher.ARCFOUR128;
import org.apache.sshd.common.cipher.ARCFOUR256;
import org.apache.sshd.common.cipher.BlowfishCBC;
import org.apache.sshd.common.cipher.TripleDESCBC;
import org.apache.sshd.common.compression.CompressionNone;
import org.apache.sshd.common.forward.DefaultForwardingAcceptorFactory;
import org.apache.sshd.common.mac.HMACMD5;
import org.apache.sshd.common.mac.HMACMD596;
import org.apache.sshd.common.mac.HMACSHA1;
import org.apache.sshd.common.mac.HMACSHA196;
import org.apache.sshd.common.random.BouncyCastleRandom;
import org.apache.sshd.common.random.JceRandom;
import org.apache.sshd.common.random.SingletonRandomFactory;
import org.apache.sshd.common.signature.SignatureDSA;
import org.apache.sshd.common.signature.SignatureRSA;
import org.apache.sshd.common.util.OsUtils;
import org.apache.sshd.common.util.SecurityUtils;
import org.apache.sshd.server.ForwardingFilter;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.channel.ChannelDirectTcpip;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.filesystem.NativeFileSystemFactory;
import org.apache.sshd.server.kex.DHG1;
import org.apache.sshd.server.kex.DHG14;
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.cloudifysource.dsl.cloud.FileTransferModes;
import org.cloudifysource.esc.installer.filetransfer.ScpFileTransfer;
import org.junit.Ignore;
import org.junit.Test;

public class FileTransferTest {

	public static SshServer setUpDefaultServer() {
		SshServer sshd = new SshServer();
		// DHG14 uses 2048 bits key which are not supported by the default JCE provider
		if (SecurityUtils.isBouncyCastleRegistered()) {
			sshd.setKeyExchangeFactories(Arrays.<NamedFactory<KeyExchange>> asList(
					new DHG14.Factory(),
					new DHG1.Factory()));
			sshd.setRandomFactory(new SingletonRandomFactory(new BouncyCastleRandom.Factory()));
		} else {
			sshd.setKeyExchangeFactories(Arrays.<NamedFactory<KeyExchange>> asList(
					new DHG1.Factory()));
			sshd.setRandomFactory(new SingletonRandomFactory(new JceRandom.Factory()));
		}
		setUpDefaultCiphers(sshd);
		// Compression is not enabled by default
		// sshd.setCompressionFactories(Arrays.<NamedFactory<Compression>>asList(
		// new CompressionNone.Factory(),
		// new CompressionZlib.Factory(),
		// new CompressionDelayedZlib.Factory()));
		sshd.setCompressionFactories(Arrays.<NamedFactory<Compression>> asList(
				new CompressionNone.Factory()));
		sshd.setMacFactories(Arrays.<NamedFactory<Mac>> asList(
				new HMACMD5.Factory(),
				new HMACSHA1.Factory(),
				new HMACMD596.Factory(),
				new HMACSHA196.Factory()));
		sshd.setChannelFactories(Arrays.<NamedFactory<Channel>> asList(
				new ChannelSession.Factory(),
				new ChannelDirectTcpip.Factory()));
		sshd.setSignatureFactories(Arrays.<NamedFactory<Signature>> asList(
				new SignatureDSA.Factory(),
				new SignatureRSA.Factory()));
		sshd.setFileSystemFactory(new NativeFileSystemFactory());

		ForwardingAcceptorFactory faf = new DefaultForwardingAcceptorFactory();
		sshd.setTcpipForwardNioSocketAcceptorFactory(faf);
		sshd.setX11ForwardNioSocketAcceptorFactory(faf);

		return sshd;
	}

	private static void setUpDefaultCiphers(SshServer sshd) {
		List<NamedFactory<Cipher>> avail = new LinkedList<NamedFactory<Cipher>>();
		avail.add(new AES128CTR.Factory());
		avail.add(new AES256CTR.Factory());
		avail.add(new ARCFOUR128.Factory());
		avail.add(new ARCFOUR256.Factory());
		avail.add(new AES128CBC.Factory());
		avail.add(new TripleDESCBC.Factory());
		avail.add(new BlowfishCBC.Factory());
		avail.add(new AES192CBC.Factory());
		avail.add(new AES256CBC.Factory());

		for (Iterator<NamedFactory<Cipher>> i = avail.iterator(); i.hasNext();) {
			final NamedFactory<Cipher> f = i.next();
			try {
				final Cipher c = f.create();
				final byte[] key = new byte[c.getBlockSize()];
				final byte[] iv = new byte[c.getIVSize()];
				c.init(Cipher.Mode.Encrypt, key, iv);
			} catch (InvalidKeyException e) {
				i.remove();
			} catch (Exception e) {
				i.remove();
			}
		}
		sshd.setCipherFactories(avail);
	}

	@Ignore
	@Test
	public void testScpFileTransfer() throws IOException, TimeoutException, InstallerException {
		ScpFileTransfer transfer = new ScpFileTransfer();
		InstallationDetails details = new InstallationDetails();

		details.setAdmin(null);
		details.setBindToPrivateIp(true);
		details.setConnectedToPrivateIp(true);
		details.setDeleteRemoteDirectoryContents(true);
		details.setFileTransferMode(FileTransferModes.SCP);
		details.setLocalDir("c:/temp/temp");
		details.setManagement(false);

		// TODO
		details.setPassword("reverse");
		details.setPrivateIp("192.168.0.6");
		details.setPublicIp("BLABLA");
		details.setRemoteDir("/home/ubuntu/transfer");
		details.setUsername("ubuntu");

		final long end = System.currentTimeMillis() + 1000 * 60;

		transfer.initialize(details, end);
		transfer.copyFiles(details, new HashSet<String>(Arrays.asList("2.txt", "subfolder1", "subfolder2/1.txt")),
				new ArrayList<File>(), end);
		// SshServer sshd = startSshServer(2222);
		// sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
		//
		// @Override
		// public boolean authenticate(String username, String password, ServerSession session) {
		// System.out.println(username);
		// System.out.println(password);
		// return true;
		// }
		// });

		// sshd.setPort(2222);

		// SshServer sshd = new SshServer();
		// sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
		//
		// @Override
		// public boolean authenticate(String username, String password, ServerSession session) {
		// System.out.println(username);
		// System.out.println(password);
		// return true;
		// }
		// });
		// sshd.setKeyExchangeFactories(new ArrayList<NamedFactory<KeyExchange>>());
		// sshd.start();
		// System.in.read();

	}

	private SshServer startSshServer(final int port) throws IOException {
		boolean error = false;

		System.err.println("Starting SSHD on port " + port);

		SshServer sshd = SshServer.setUpDefaultServer();
		sshd.setPort(port);
		if (SecurityUtils.isBouncyCastleRegistered()) {
			sshd.setKeyPairProvider(new PEMGeneratorHostKeyProvider("key.pem"));
		} else {
			sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("key.ser"));
		}
		if (OsUtils.isUNIX()) {
			sshd.setShellFactory(new ProcessShellFactory(new String[] { "/bin/sh", "-i", "-l" },
					EnumSet.of(ProcessShellFactory.TtyOptions.ONlCr)));
		} else {
			sshd.setShellFactory(new ProcessShellFactory(new String[] { "cmd.exe " },
					EnumSet.of(ProcessShellFactory.TtyOptions.Echo, ProcessShellFactory.TtyOptions.ICrNl,
							ProcessShellFactory.TtyOptions.ONlCr)));
		}
		sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
			@Override
			public boolean authenticate(String username, String password, ServerSession session) {
				return username != null && username.equals(password);
			}
		});
		sshd.setPublickeyAuthenticator(new PublickeyAuthenticator() {
			@Override
			public boolean authenticate(String username, PublicKey key, ServerSession session) {
				// File f = new File("/Users/" + username + "/.ssh/authorized_keys");
				return true;
			}
		});
		sshd.setForwardingFilter(new ForwardingFilter() {
			@Override
			public boolean canForwardAgent(ServerSession session) {
				return true;
			}

			@Override
			public boolean canForwardX11(ServerSession session) {
				return true;
			}

			@Override
			public boolean canListen(InetSocketAddress address, ServerSession session) {
				return true;
			}

			@Override
			public boolean canConnect(InetSocketAddress address, ServerSession session) {
				return true;
			}
		});
		sshd.start();
		return sshd;
	}

}
